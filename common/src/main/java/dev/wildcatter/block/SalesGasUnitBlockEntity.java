package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SalesGas;
import dev.wildcatter.multiblock.BigPart;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A meter station's unpowered parts (docs/multiblocks/sales-gas.md), worked by the meter building touching their meter run:
 * <ul>
 * <li>the filter separator: pipeline gas in on the north face of its bottom cells; its sump catches oil, glycol and debris, counted, and
 * drains itself;</li>
 * <li>the ultrasonic meter run: a 4 in, 4-path meter with its flow conditioner and straight runs, its transducers on the building's power;</li>
 * <li>the regulator skid: worker and monitor regulators and the line heater; pipeline gas out of the south face of its back cells;</li>
 * <li>the pipeline tap: the hot-tap tee, the mainline block valve and its operator, open while the station runs.</li>
 * </ul>
 */
public class SalesGasUnitBlockEntity extends PlantUnitBlockEntity {
	private double sump;            // litres caught, all told
	private String reading = "";
	private long readAt = -1000;

	public SalesGasUnitBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.SALES_GAS_UNIT_ENTITY, pos, state);
	}

	public String kind() {
		return getBlockState().getBlock() instanceof BigPart part ? part.shapeName() : "";
	}

	/** The filter separator's inlet: the north face of its bottom cells. */
	public List<Lines.Face> inlet() {
		return faces(at -> at[1] == 0 && at[2] == 0, Direction.NORTH);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return kind().equals("regulator_skid") && at[2] == 4 && at[1] == 0 && side == Direction.SOUTH ? Product.PIPELINE_GAS : null;
	}

	/** The regulator skid holds one interval of the station's rating: the gas in its own piping. */
	@Override
	protected double hold(Product product) {
		return kind().equals("regulator_skid") && product == Product.PIPELINE_GAS ? SalesGas.rating(seconds()) : 0;
	}

	public double roomFor(Product product) {
		return room(product);
	}

	public void give(Product product, double amount) {
		add(product, amount);
	}

	/** The filter separator's sump: what it has caught. */
	public void caught(double litres) {
		sump += litres;
		setChanged();
	}

	public double sump() {
		return sump;
	}

	/** The building's word on this part, each interval. */
	public void read(String text, long now) {
		reading = text;
		readAt = now;
		setChanged();
	}

	private boolean fresh() {
		return level != null && level.getGameTime() - readAt < 40;
	}

	@Override
	public Component inspect() {
		String name = switch (kind()) {
			case "filter_separator" -> "Filter separator (coalescing elements, liquid sump)";
			case "ultrasonic_meter_run" -> "Ultrasonic meter run (4 in, 4-path, AGA 9)";
			case "regulator_skid" -> "Regulator skid (worker and monitor regulators, line heater)";
			case "pipeline_tap" -> "Pipeline tap (hot-tap tee, mainline block valve, check valve)";
			default -> kind();
		};
		String text = fresh() ? reading : switch (kind()) {
			case "pipeline_tap" -> "block valve CLOSED: no meter building works this station";
			default -> "idle: no meter building works this station";
		};
		if (kind().equals("filter_separator")) {
			text += String.format("; sump has caught %.2f L, drained", sump);
		}
		return Component.literal(name + ": " + text);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("sump", sump);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		sump = input.getDoubleOr("sump", 0);
	}
}
