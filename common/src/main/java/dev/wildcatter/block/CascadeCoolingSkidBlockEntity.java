package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Enrichment;
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
 * The cascade cooling skid (docs/multiblocks/enrichment-operations.md, 16c1): plate exchangers and pumps on the hall's closed cooling
 * loop. Its cells against the cascade header, it cools the machines the feed station counts there: each one's 571 kW of electrical
 * input as heat, 13.6 kg/s of circulating water a machine at a 10 C rise. Circulating water in on the north face of its front cells (from
 * a CW pump), back warm on the south face of its back cells; what nothing takes back is discharged, as the RHR exchanger does. Its pumps:
 * 55 kW on the industrial supply, box on the west.
 */
public class CascadeCoolingSkidBlockEntity extends UnitMotorBlockEntity {
	private int machines;        // the machines the feed station last asked it to cool
	private long cooledAt = -1000;
	private double discharged;
	private String unit = "starting";

	public CascadeCoolingSkidBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CASCADE_COOLING_SKID_ENTITY, pos, state);
	}

	/** kg of circulating water a second one machine takes at full speed. */
	public static double waterPerMachine() {
		return Enrichment.machineWatts() / (Tuning.WATER_SPECIFIC_HEAT * Tuning.CASING_RISE_C);
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 5 && at[1] == 0 && side == Direction.SOUTH ? Product.COOLING_WATER : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.COOLING_WATER ? waterPerMachine() * 12 * Tuning.PLANT_HOLD_SECONDS : 0;
	}

	/** The feed station: cool {@code n} machines. */
	public void serve(int n) {
		machines = n;
	}

	/** True if it cooled its machines lately. */
	public boolean cooling(Level level) {
		return level.getGameTime() - cooledAt < 40;
	}

	@Override
	protected void work(Level level) {
		if (machines <= 0) {
			unit = "idle: no machines running on its header";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting the pumps: " + describe();
			return;
		}
		double need = machines * waterPerMachine() * seconds();
		double[] mix = new double[5];
		double got = pullMixed(level, inlet(), Product.COOLING_WATER, need, mix);
		double inC = mix[0] > 0.1 ? mix[0] : Tuning.LAKE_C;
		double fits = Math.min(got, room(Product.COOLING_WATER));
		if (fits > 0) {
			addMixed(Product.COOLING_WATER, fits, new double[] {inC + Tuning.CASING_RISE_C, 0, 0, 0, 0});
		}
		discharged += got - fits;
		if (got >= need * 0.99) {
			cooledAt = level.getGameTime();
			unit = String.format("cooling %d machines: %.0f kg/s of circulating water, %.0f C in, %.0f C out", machines, got / seconds(), inC, inC + Tuning.CASING_RISE_C);
		} else {
			unit = String.format("NO WATER: %.0f of %.0f kg/s of circulating water for %d machines (pipe a CW pump to its north face)", got / seconds(), need / seconds(), machines);
		}
	}

	@Override
	public Component inspect() {
		return Component.literal("Cascade cooling skid (" + Math.round(Tuning.CASCADE_SKID_WATTS / 1000) + " kW, " + Voltage.INDUSTRIAL.label() + "): " + unit
				+ String.format("; %,.0f t of warm water discharged; ", discharged / 1000) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("discharged", discharged);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		discharged = input.getDoubleOr("discharged", 0);
	}
}
