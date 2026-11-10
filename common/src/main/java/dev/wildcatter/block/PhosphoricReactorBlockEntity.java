package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Phosphate;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The phosphoric reactor (docs/multiblocks/fertilizer.md), the plant's controller: a single-tank dihydrate attack reactor with its
 * agitators and flash cooler at 75 C. Rock slurry on the north face of its front cells, sulfuric acid on the south face of its back
 * cells; it hands its slurry to the tilting pan filter against its east side and runs it. 20 t of P2O5 a day as 28 % filter acid and
 * 90 t of phosphogypsum; 150 kW on the world's industrial voltage, the box on the west face of its main cell.
 */
public class PhosphoricReactorBlockEntity extends UnitMotorBlockEntity {
	private double making;   // kg/h of P2O5 into the filter acid, the last interval
	private double gypsumT;  // t of phosphogypsum to the stack
	private double lostT;    // t of P2O5 lost in it
	private String unit = "starting";

	public PhosphoricReactorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.PHOSPHORIC_REACTOR_ENTITY, pos, state);
	}

	private List<Lines.Face> slurryIn() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	private List<Lines.Face> acidIn() {
		return faces(at -> at[2] == 7 && at[1] == 0, Direction.SOUTH);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	/** kg/h of P2O5 into the filter acid over the last interval: 833 at the rating. */
	public double making() {
		return making;
	}

	@Override
	protected void work(Level level) {
		making = 0;
		PanFilterBlockEntity filter = Touching.find(level, worldPosition, PanFilterBlockEntity.class, f -> true);
		if (filter == null) {
			unit = "NO FILTER: set the tilting pan filter against its east side";
			return;
		}
		double slurry = Lines.available(level, slurryIn(), Product.ROCK_SLURRY, Pull.MACHINE, this);
		if (slurry < 1e-6) {
			unit = "NO ROCK SLURRY: pipe the phosphate mill's slurry to its north side";
			filter.unit = "idle: no slurry from its reactor";
			return;
		}
		double acid = Lines.available(level, acidIn(), Product.SULFURIC_ACID, Pull.MACHINE, this);
		if (acid < 1e-6) {
			unit = "NO ACID: pipe sulfuric acid to its south side";
			filter.unit = "idle: no slurry from its reactor";
			return;
		}
		double rock = Math.min(Phosphate.reactorRockKgs() * seconds(), slurry / Phosphate.slurryPerRock());
		rock = Math.min(rock, acid * Tuning.ACID_KG_PER_L / Phosphate.acidPerRock());
		double gypsumPerRock = Phosphate.gypsumPerRock();
		if (filter.roomFor(Product.TAILS_SLURRY) < Math.min(rock, Phosphate.reactorRockKgs() * seconds() / 4) * gypsumPerRock * Phosphate.tailsPerGypsum()) {
			filter.unit = "NO GYPSUM STACK: pipe its west side to a lined tailings cell's decant tower (full, breached or missing)";
			unit = "HELD: the filter has nowhere to put its gypsum (NO GYPSUM STACK)";
			return;
		}
		rock = Math.min(rock, filter.roomFor(Product.TAILS_SLURRY) / (gypsumPerRock * Phosphate.tailsPerGypsum()));
		if (filter.water() < 1e-6) {
			filter.unit = "NO WATER: pump water to it for its washes";
			unit = "HELD: the filter has no wash water";
			return;
		}
		rock = Math.min(rock, filter.water() / (gypsumPerRock * Phosphate.filterWaterPerGypsum()));
		rock = Math.min(rock, filter.roomFor(Product.FILTER_ACID) * Tuning.FILTER_ACID_KG_PER_L / Phosphate.filterAcidPerRock());
		if (rock <= 1e-7) {
			filter.unit = "idle: FULL, its filter acid has nowhere to go (pipe its south side to the evaporator or a stock tank)";
			unit = "idle: the filter's acid is not drawn";
			return;
		}
		if (plcHold(level)) {
			unit = status;
			return;
		}
		double strength = Math.min(drive(level), filter.drive(level));
		if (strength <= 0) {
			unit = "starting: " + describe() + "; filter " + filter.motor().describe();
			filter.unit = "starting";
			return;
		}
		rock *= strength;
		Lines.pull(level, slurryIn(), Product.ROCK_SLURRY, rock * Phosphate.slurryPerRock(), Pull.MACHINE, this);
		Lines.pull(level, acidIn(), Product.SULFURIC_ACID, rock * Phosphate.acidPerRock() / Tuning.ACID_KG_PER_L, Pull.MACHINE, this);
		double gypsum = rock * gypsumPerRock;
		filter.useWater(gypsum * Phosphate.filterWaterPerGypsum());
		filter.give(Product.FILTER_ACID, rock * Phosphate.filterAcidPerRock() / Tuning.FILTER_ACID_KG_PER_L);
		filter.give(Product.TAILS_SLURRY, gypsum * Phosphate.tailsPerGypsum());
		gypsumT += gypsum / 1000;
		lostT += rock * Phosphate.p2o5LostPerRock() / 1000;
		making = rock * Phosphate.rockP2o5() * Tuning.PHOSPHORIC_RECOVERY / seconds() * 3600;
		double lps = rock * Phosphate.filterAcidPerRock() / Tuning.FILTER_ACID_KG_PER_L / seconds();
		unit = String.format("attacking %.2f kg/s of rock at 75 C on %.2f L/s of sulfuric acid: %.0f kg/h of P2O5 (%.1f t a day)", rock / seconds(),
				rock * Phosphate.acidPerRock() / Tuning.ACID_KG_PER_L / seconds(), making, making * 24 / 1000);
		filter.unit = String.format("filtering: %.2f L/s of filter acid at %.0f %% P2O5, %.2f kg/s of gypsum to the stack", lps, Tuning.FILTER_ACID_P2O5 * 100, gypsum / seconds());
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Phosphoric reactor (agitators and flash cooler %.0f kW, %s): ", Tuning.PHOSPHORIC_REACTOR_WATTS / 1000, Voltage.INDUSTRIAL.label())
				+ unit + String.format("; %.1f t of phosphogypsum sent to the stack, %.2f t of P2O5 lost in it; ", gypsumT, lostT) + describe());
	}

	@Override
	public List<Point> points() {
		return RatePoints.POINTS;
	}

	@Override
	public boolean readPoint(int point) {
		return RatePoints.read(point, making > 0, unitRemote.auto());
	}

	@Override
	public double readMa(int point) {
		return RatePoints.ma(making, Phosphate.p2o5Kgs() * 3600);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("gypsumT", gypsumT);
		output.putDouble("lostT", lostT);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		gypsumT = input.getDoubleOr("gypsumT", 0);
		lostT = input.getDoubleOr("lostT", 0);
	}
}
