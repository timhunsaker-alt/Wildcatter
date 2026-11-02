package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The motor-driven auxiliary feedwater pump (docs/multiblocks/reactor.md): 5 kg/s at about 8 MPa, 75 hp (56 kW) at 480 V. It takes
 * water off a pump and gives feedwater on a pipe from the south face of its back bottom cells to the steam generators, running while
 * they draw it.
 */
public class AuxFeedPumpBlockEntity extends UnitMotorBlockEntity implements WaterUser {
	private static final double WATER_HOLD = 500.0;
	private double water;
	private double pumped;
	private String unit = "starting";

	public AuxFeedPumpBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.AUX_FEED_PUMP_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 3 && at[1] == 0 && side == Direction.SOUTH ? Product.FEEDWATER : null;
	}

	@Override
	protected double hold(Product product) {
		return Tuning.AFW_KGS * 2;
	}

	@Override
	public double waterLevel() {
		return water / WATER_HOLD;
	}

	@Override
	public int addWater(int litres) {
		int fits = (int) Math.max(0, Math.min(litres, WATER_HOLD - water));
		water += fits;
		setChanged();
		return fits;
	}

	@Override
	protected void work(Level level) {
		double room = room(Product.FEEDWATER);
		if (room <= 1e-6) {
			unit = "idle: the steam generators are not drawing feed";
			return;
		}
		if (water <= 1e-6) {
			unit = "NO WATER: pump water to it";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double kg = Math.min(Math.min(Tuning.AFW_KGS * seconds() * strength, room), water);
		water -= kg;
		pumped += kg;
		add(Product.FEEDWATER, kg);
		unit = String.format("feeding %.1f kg/s", kg / seconds());
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Aux feed pump (75 hp, %s): %s; water %.0f L, %.0f t pumped; %s", motor().voltage().label(), unit, water, pumped / 1000,
				describe()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("water", water);
		output.putDouble("pumped", pumped);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		water = input.getDoubleOr("water", 0);
		pumped = input.getDoubleOr("pumped", 0);
	}
}
