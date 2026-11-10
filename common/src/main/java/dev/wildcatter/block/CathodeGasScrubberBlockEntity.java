package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Conversion;
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
 * The cathode gas scrubber (docs/multiblocks/conversion-extensions.md, 16b1): twin sodium fluoride beds, one adsorbing the HF the
 * fluorine cells' cathode hydrogen carries while the other is regenerated at about 300 C, and a KOH polishing column. It stands against
 * the fluorine cell house, takes the hydrogen the cells make (one H2 for each F2, about 28.6 Nm3/h at full current) and puts it on the
 * pipe at the south face of its back cells, for the reduction reactor. What the pipe cannot take is vented, as the cell house vents it
 * with no scrubber. Its heaters and pump: 10 kW on the industrial supply, the box on the north face of its main cell.
 */
public class CathodeGasScrubberBlockEntity extends UnitMotorBlockEntity {
	private double scrubbed;   // m3 of hydrogen taken from the cells
	private double vented;     // m3 of it the pipe had no room for
	private double rate;       // m3 a second, last interval
	private String unit = "starting";

	public CathodeGasScrubberBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CATHODE_GAS_SCRUBBER_ENTITY, pos, state);
	}

	@Override
	protected List<Direction> boxFaces() {
		return List.of(LeasePartBlock.world(getBlockState(), Direction.NORTH));
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 1 && at[1] == 0 && side == Direction.SOUTH ? Product.HYDROGEN : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.HYDROGEN ? Conversion.fluorineKgs() * FluorineCellHouseBlockEntity.cathodeM3PerF2() * Tuning.PLANT_HOLD_SECONDS * 4 : 0;
	}

	/** Nm3 an hour of hydrogen it scrubbed over its last interval. */
	public double nm3h() {
		return rate * 3600;
	}

	public double scrubbed() {
		return scrubbed;
	}

	@Override
	protected void work(Level level) {
		FluorineCellHouseBlockEntity cells = Touching.find(level, worldPosition, FluorineCellHouseBlockEntity.class, c -> true);
		if (cells == null) {
			unit = "NOT BUILT: set it against a FLUORINE CELL HOUSE";
			rate = 0;
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			rate = 0;
			return;
		}
		double got = cells.takeCathodeGas(Double.MAX_VALUE);
		double fits = Math.min(got, room(Product.HYDROGEN));
		add(Product.HYDROGEN, fits);
		scrubbed += got;
		vented += got - fits;
		rate = got / seconds();
		unit = got > 1e-9 ? String.format("scrubbing %.1f Nm3/h of cathode hydrogen%s", rate * 3600, got - fits > 1e-9 ? String.format(", venting %.1f the pipe cannot take", (got - fits) / seconds() * 3600) : "")
				: "idle: the cells make no hydrogen (no fluorine)";
	}

	@Override
	public Component inspect() {
		return Component.literal("Cathode gas scrubber (NaF beds, " + Math.round(Tuning.CATHODE_SCRUBBER_WATTS / 1000) + " kW, " + Voltage.INDUSTRIAL.label() + "): " + unit
				+ String.format("; %,.0f Nm3 scrubbed, %,.0f vented; hydrogen %.1f Nm3 held; ", scrubbed, vented, held(Product.HYDROGEN)) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("scrubbed", scrubbed);
		output.putDouble("vented", vented);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		scrubbed = input.getDoubleOr("scrubbed", 0);
		vented = input.getDoubleOr("vented", 0);
	}
}
