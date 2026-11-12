package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Motor;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.ChlorAlkaliPlant;
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
 * The chlorine train (docs/multiblocks/chlor-alkali-plant.md): the chlorine cooler, two sulfuric acid drying towers, the compressor and
 * a refrigerated liquefier. Wet chlorine in on the north face of its front cells, 98 % sulfuric acid on the east face of its front cells
 * (spent 75 % acid counted); dry chlorine out of the south face of its back cells, and liquid chlorine into a chlorine filler it touches.
 * Two supplies: the compressor at the world's medium voltage (box on the west face of its main cell), the liquefier at the industrial
 * voltage (box on the north face of its main cell).
 */
public class ChlorineTrainBlockEntity extends UnitMotorBlockEntity {
	private final Motor liquefier = new Motor(true, Voltage.INDUSTRIAL, Tuning.CL2_LIQUEFIER_WATTS);
	private boolean liquefierWanted;
	private long liquefierCalled = -1000;
	private double making;    // kg/h of chlorine dried, the last interval
	private double liquid;    // kg/h liquefied, the last interval
	private double spentAcid;
	private String unit = "starting";

	public ChlorineTrainBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CHLORINE_TRAIN_ENTITY, pos, state);
	}

	private List<Lines.Face> wetIn() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	private List<Lines.Face> acidIn() {
		return faces(at -> at[0] == 3 && at[2] == 1 && at[1] == 0, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 9 && at[1] == 0 && side == Direction.SOUTH ? Product.CHLORINE : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.CHLORINE ? ChlorAlkaliPlant.cl2Kgs() * Tuning.PLANT_HOLD_SECONDS * 4 : 0;
	}

	/** kg/h of chlorine dried over the last interval. */
	public double making() {
		return making;
	}

	/** kg/h of chlorine liquefied into the filler over the last interval. */
	public double liquefying() {
		return liquid;
	}

	@Override
	protected void work(Level level) {
		making = 0;
		liquid = 0;
		double wet = Lines.available(level, wetIn(), Product.WET_CHLORINE, Pull.MACHINE, this);
		if (wet < 1e-6) {
			unit = "idle: no wet chlorine at its front (north face)";
			return;
		}
		double acid = Lines.available(level, acidIn(), Product.SULFURIC_ACID, Pull.MACHINE, this);
		if (acid < 1e-6) {
			unit = "NO ACID: pipe sulfuric acid to the east face of its front for the drying towers";
			return;
		}
		double kg = Math.min(wet, ChlorAlkaliPlant.cl2Kgs() * seconds() * 1.2);
		kg = Math.min(kg, acid * Tuning.ACID_KG_PER_L / Tuning.DRYING_ACID_PER_CL2);
		kg = Math.min(kg, room(Product.CHLORINE));
		ChlorineFillerBlockEntity filler = Touching.find(level, worldPosition, ChlorineFillerBlockEntity.class, f -> true);
		if (kg <= 1e-7 && filler == null) {
			unit = "FULL: its dry chlorine has nowhere to go (pipe its back end to a user, or set a chlorine filler against it)";
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
		if (kg > 1e-7) {
			kg = Lines.pull(level, wetIn(), Product.WET_CHLORINE, kg, Pull.MACHINE, this);
			Lines.pull(level, acidIn(), Product.SULFURIC_ACID, kg * Tuning.DRYING_ACID_PER_CL2 / Tuning.ACID_KG_PER_L, Pull.MACHINE, this);
			spentAcid += kg * ChlorAlkaliPlant.spentAcidPerCl2();
			add(Product.CHLORINE, kg);
			making = kg / seconds() * 3600;
		}
		// the liquefier takes what the pipe's users leave above two intervals' make
		String liq = "no chlorine filler against it";
		if (filler != null) {
			liquefierCalled = level.getGameTime();
			double spare = held(Product.CHLORINE) - ChlorAlkaliPlant.cl2Kgs() * seconds() * 2;
			double cap = Tuning.CL2_LIQUEFIER_KGS * seconds() * (liquefier.reversed() ? 0 : liquefier.output());
			double go = Math.max(0, Math.min(Math.min(spare, cap), filler.liquidRoom()));
			if (go > 1e-7) {
				add(Product.CHLORINE, -go);
				filler.addChlorine(go);
				liquid = go / seconds() * 3600;
			}
			liq = liquefier.output() <= 0 ? "liquefier " + liquefier.describe() : String.format("liquefying %,.0f kg/h into the filler", liquid);
		}
		unit = String.format("drying %,.0f kg/h of chlorine on %.1f kg/h of acid, compressing it; %s", making, making * Tuning.DRYING_ACID_PER_CL2, liq);
	}

	// ---- the second supply: the liquefier -----------------------------------------------------------------------------------------
	private Direction liquefierFace() {
		return LeasePartBlock.world(getBlockState(), Direction.NORTH);
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return super.hasPortAt(cell, face, role, voltage)
				|| cell.equals(worldPosition) && face == liquefierFace() && voltage == Voltage.INDUSTRIAL && role != Conductor.N;
	}

	@Override
	public void build(Circuit circuit) {
		super.build(circuit);
		liquefier.build(circuit, worldPosition, List.of(liquefierFace()));
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		super.apply(circuit, ticks);
		liquefierWanted = level != null && level.getGameTime() - liquefierCalled < 40;
		liquefier.apply(circuit, ticks, liquefierWanted);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Chlorine train (compressor %.0f kW, %s; liquefier %.0f kW, %s): ", Tuning.CL2_COMPRESSOR_WATTS / 1000, Voltage.MEDIUM.label(),
				Tuning.CL2_LIQUEFIER_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit + String.format("; dry chlorine held %.0f kg; %.0f kg of spent 75 %% acid; ",
						held(Product.CHLORINE), spentAcid) + describe() + "; liquefier: " + (liquefierWanted ? liquefier.describe() : "idle"));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("spentAcid", spentAcid);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		spentAcid = input.getDoubleOr("spentAcid", 0);
	}
}
