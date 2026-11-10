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
 * The acid evaporator (docs/multiblocks/fertilizer.md): a forced-circulation vacuum evaporator concentrating filter acid (or the
 * DEPA-TOPO circuit's treated acid) from 28 % to 54 % P2O5 merchant acid on process steam, about 2 t of steam a tonne of P2O5. Acid in
 * on the north face of its bottom cells, steam on the east; merchant acid out of the south. Its fluorine off-gas (SiF4) is counted.
 * The circulation pump is 75 kW on the world's industrial voltage, the box on the west face of its main cell.
 */
public class AcidEvaporatorBlockEntity extends UnitMotorBlockEntity {
	private double making;     // L/s of merchant acid, the last interval
	private double fluorine;   // kg of F off as SiF4
	private String unit = "starting";

	public AcidEvaporatorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.ACID_EVAPORATOR_ENTITY, pos, state);
	}

	private List<Lines.Face> acidIn() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	private List<Lines.Face> steamIn() {
		return faces(at -> at[0] == 2 && at[1] == 0, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 2 && at[1] == 0 && side == Direction.SOUTH ? Product.MERCHANT_ACID : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.MERCHANT_ACID ? Phosphate.merchantLps() * Tuning.PLANT_HOLD_SECONDS * 4 : 0;
	}

	/** L/s of merchant acid over the last interval: about 0.26 at the rating. */
	public double making() {
		return making;
	}

	@Override
	protected void work(Level level) {
		making = 0;
		double filter = Lines.available(level, acidIn(), Product.FILTER_ACID, Pull.MACHINE, this);
		double treated = Lines.available(level, acidIn(), Product.TREATED_ACID, Pull.MACHINE, this);
		if (filter + treated < 1e-6) {
			unit = "NO ACID: pipe filter acid to its north side";
			return;
		}
		double steam = Lines.available(level, steamIn(), Product.PROCESS_STEAM, Pull.MACHINE, this);
		if (steam < 1e-6) {
			unit = "NO STEAM: pipe process steam from a package boiler to its east side";
			return;
		}
		double perKg = Phosphate.steamPerFilterAcid();
		double kg = Math.min(Phosphate.filterAcidLps() * Tuning.FILTER_ACID_KG_PER_L * seconds(), (filter + treated) * Tuning.FILTER_ACID_KG_PER_L);
		kg = Math.min(kg, steam / perKg);
		kg = Math.min(kg, room(Product.MERCHANT_ACID) * Tuning.MERCHANT_ACID_KG_PER_L / Phosphate.merchantPerFilterAcid());
		if (kg <= 1e-7) {
			unit = "idle: FULL, its merchant acid has nowhere to go (pipe its south side to the granulator or a stock tank)";
			return;
		}
		if (plcHold(level)) {
			unit = status;
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		kg *= strength;
		double l = kg / Tuning.FILTER_ACID_KG_PER_L;
		double got = Lines.pull(level, acidIn(), Product.TREATED_ACID, l, Pull.MACHINE, this);
		got += Lines.pull(level, acidIn(), Product.FILTER_ACID, l - got, Pull.MACHINE, this);
		kg = got * Tuning.FILTER_ACID_KG_PER_L;
		Lines.pull(level, steamIn(), Product.PROCESS_STEAM, kg * perKg, Pull.MACHINE, this);
		double merchant = kg * Phosphate.merchantPerFilterAcid() / Tuning.MERCHANT_ACID_KG_PER_L;
		add(Product.MERCHANT_ACID, merchant);
		fluorine += kg * Phosphate.fluorinePerFilterAcid();
		making = merchant / seconds();
		unit = String.format("concentrating %.2f L/s of acid to %.0f %% P2O5 under vacuum on %.2f kg/s of steam: %.2f L/s of merchant acid", got / seconds(),
				Tuning.MERCHANT_ACID_P2O5 * 100, kg * perKg / seconds(), making);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Acid evaporator (circulation pump %.0f kW, %s): ", Tuning.ACID_EVAPORATOR_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit
				+ String.format("; merchant acid held %.0f L; %.0f kg of fluorine off as SiF4; ", held(Product.MERCHANT_ACID), fluorine) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("fluorine", fluorine);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		fluorine = input.getDoubleOr("fluorine", 0);
	}
}
