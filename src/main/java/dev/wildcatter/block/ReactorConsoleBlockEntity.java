package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The main control board (docs/multiblocks/reactor.md), against the vessel. A click reads the plant; a sneak-click steps the power
 * setpoint (TRIP, 0, 2, 5, 10, 25, 50, 75, 100 %); TRIP trips the reactor, and leaving it resets the trip once its cause has cleared.
 */
public class ReactorConsoleBlockEntity extends PlantUnitBlockEntity {
	public ReactorConsoleBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.REACTOR_CONSOLE_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	private @Nullable ReactorVesselBlockEntity vessel(Level level) {
		return Touching.find(level, worldPosition, ReactorVesselBlockEntity.class, v -> true);
	}

	public String nextSetpoint(Level level) {
		ReactorVesselBlockEntity vessel = vessel(level);
		if (vessel == null) {
			return "Reactor console: NOT BUILT: set it against the REACTOR VESSEL";
		}
		double set = vessel.reactor().nextSetpoint();
		vessel.setChanged();
		return set < 0 ? "Reactor console: TRIP (all rods in)" : String.format("Reactor console: setpoint %.0f %% (%s)", set * 100,
				vessel.reactor().tripped() ? "resetting the trip" : "rods under automatic control");
	}

	@Override
	public Component inspect() {
		ReactorVesselBlockEntity vessel = level == null ? null : vessel(level);
		return vessel == null ? Component.literal("Reactor console: NOT BUILT: set it against the REACTOR VESSEL") : vessel.inspect();
	}
}
