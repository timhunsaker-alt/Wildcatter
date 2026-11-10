package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Radiation;
import dev.wildcatter.radiation.Contamination;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The personnel contamination monitor and shower (docs/multiblocks/reactor-extensions.md, 19b), at the airlock or the site's exit, on
 * the residential tier (single-phase). Always on while powered; a click reads the player's contamination and washes it off.
 */
public class PersonnelMonitorBlockEntity extends UnitMotorBlockEntity {
	private int washed;

	public PersonnelMonitorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.PERSONNEL_MONITOR_ENTITY, pos, state);
	}

	@Override
	protected void work(Level level) {
		power(level);
	}

	public boolean powered() {
		return motor().output() > 0;
	}

	/** A click: read and wash. */
	public String wash(ServerPlayer player) {
		if (!powered()) {
			return "Personnel monitor: NO POWER (" + Voltage.LOW.label() + ", single-phase, at the west face of its base): " + motor().describe();
		}
		if (!dev.wildcatter.Rules.reactorContamination()) {
			return "Personnel monitor: CLEAN (no contamination is kept: the contamination rule is off)";
		}
		double was = Contamination.wash((ServerLevel) player.level(), player);
		if (was <= 0) {
			return "Personnel monitor: CLEAN";
		}
		washed++;
		setChanged();
		return "Personnel monitor: CONTAMINATED, " + Radiation.rateText(was) + " on you: washed off in the shower; now CLEAN";
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Personnel contamination monitor and shower: %s; %d washed; click it to be read", powered() ? "ready" : "NO POWER", washed)
				+ "; " + describe());
	}
}
