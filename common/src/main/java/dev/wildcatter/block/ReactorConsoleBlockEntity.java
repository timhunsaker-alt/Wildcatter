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
		if (set == dev.wildcatter.fluid.Reactor.COOLDOWN) {
			return "Reactor console: COOLDOWN (rods in; the dump valves lower the temperature 55 C an hour, the RHR takes it under 177 C)";
		}
		return set < 0 ? "Reactor console: TRIP (all rods in)" : String.format("Reactor console: setpoint %.0f %% (%s)", set * 100,
				vessel.reactor().tripped() ? "resetting the trip" : "rods under automatic control");
	}

	/** A sneak-click on its lower face, with the LTOP rule: LTOP ARMED (19a). */
	public String toggleLtop(Level level) {
		ReactorVesselBlockEntity vessel = vessel(level);
		if (vessel == null) {
			return "Reactor console: NOT BUILT: set it against the REACTOR VESSEL";
		}
		String said = vessel.toggleLtop();
		return said != null ? said : nextSetpoint(level);
	}

	// ---- the rod position indication and the trip alarm (drawn by client/ReactorConsoleRenderer) -----------------------------------
	/** The rods as the board shows them: the shutdown bank (the 4 edge RCCAs) and bank D (the centre), steps withdrawn. */
	private double shownShutdown, shownBankD;
	private boolean shownTripped;
	/** Client side: what the renderer last drew, eased toward what the server sent. */
	public double easedShutdown = -1, easedBankD = -1;
	public long easedAt;
	/** Game time of the last trip, for the alarm's blasts; -1 when there has been none. */
	private long trippedAt = -1;
	private boolean seenTrip = true;   // a plant tripped when it was loaded has already sounded

	public double shownShutdown() {
		return shownShutdown;
	}

	public double shownBankD() {
		return shownBankD;
	}

	public boolean shownTripped() {
		return shownTripped;
	}

	/**
	 * Each step of the vessel: what the rod position indicators show, sent to the client when a rod moves a step or the trip changes.
	 * A new trip opens the reactor trip breakers (their clunk) and sounds the annunciator horn, in blasts for {@link #ALARM_TICKS}.
	 */
	public void show(Level level, double shutdown, double bankD, boolean tripped) {
		boolean changed = Math.abs(shutdown - shownShutdown) >= 1 || Math.abs(bankD - shownBankD) >= 1 || tripped != shownTripped
				|| (shutdown == 0) != (shownShutdown == 0) || (bankD == 0) != (shownBankD == 0);
		if (tripped && !seenTrip && level instanceof net.minecraft.server.level.ServerLevel server) {
			trippedAt = level.getGameTime();
			server.playSound(null, worldPosition, dev.wildcatter.WildcatterSounds.BREAKER_TRIP, net.minecraft.sounds.SoundSource.BLOCKS, 2.0F, 0.7F);
		}
		seenTrip = tripped;
		shownShutdown = shutdown;
		shownBankD = bankD;
		shownTripped = tripped;
		if (changed) {
			setChanged();
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	/** The annunciator's horn after a trip: a blast every 1.5 s for 9 s. */
	static final int ALARM_TICKS = 180;

	public static void tick(Level level, BlockPos pos, BlockState state, ReactorConsoleBlockEntity console) {
		if (console.trippedAt >= 0 && level instanceof net.minecraft.server.level.ServerLevel server) {
			long since = level.getGameTime() - console.trippedAt;
			if (since > ALARM_TICKS || !console.shownTripped) {
				console.trippedAt = -1;
			} else if (since % 30 == 2) {
				server.playSound(null, pos, dev.wildcatter.WildcatterSounds.HORN, net.minecraft.sounds.SoundSource.BLOCKS, 2.0F, 1.0F);
			}
		}
	}

	@Override
	public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
		net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
		tag.putDouble("rpiShutdown", shownShutdown);
		tag.putDouble("rpiBankD", shownBankD);
		tag.putBoolean("rpiTripped", shownTripped);
		return tag;
	}

	@Override
	protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
		super.loadAdditional(input);
		shownShutdown = input.getDoubleOr("rpiShutdown", shownShutdown);
		shownBankD = input.getDoubleOr("rpiBankD", shownBankD);
		shownTripped = input.getBooleanOr("rpiTripped", shownTripped);
	}

	@Override
	public Component inspect() {
		ReactorVesselBlockEntity vessel = level == null ? null : vessel(level);
		return vessel == null ? Component.literal("Reactor console: NOT BUILT: set it against the REACTOR VESSEL") : vessel.inspect();
	}
}
