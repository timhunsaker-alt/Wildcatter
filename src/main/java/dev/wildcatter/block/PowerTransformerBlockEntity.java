package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.Multiblock;
import dev.wildcatter.multiblock.MultiblockLayout;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The substation (docs/multiblocks/substation.md), run from its power transformer: HV breaker, disconnect, bus, transformer, bus,
 * disconnect, feeder breaker in a line, and the relay panel in front of the transformer. The transformer is energized only while
 * the lineup stands. The relay panel watches the transformer's current: a time-overcurrent element (51, IEEE moderately inverse,
 * pickup 150 % of full load) and an instantaneous one (50, 800 %) open both breakers, which stay open until the panel is reset.
 */
public class PowerTransformerBlockEntity extends TransformerBlockEntity {
	/** As built, drawn for the line running west (138 kV) to east (12.47 kV): the relay panel stands south of the transformer. */
	private static final MultiblockLayout LAYOUT = new MultiblockLayout('T',
			new String[] {"HDBTbdL", "...K..."});
	private static final Map<Character, Predicate<BlockState>> LEGEND = Map.of(
			'T', s -> s.is(WildcatterBlocks.POWER_TRANSFORMER),
			'H', s -> s.is(WildcatterBlocks.HV_BREAKER_3P),
			'D', s -> s.is(WildcatterBlocks.DISCONNECT_HV),
			'B', s -> s.is(WildcatterBlocks.BUS_HV),
			'b', s -> s.is(WildcatterBlocks.BUS_DIST),
			'd', s -> s.is(WildcatterBlocks.DISCONNECT_DIST),
			'L', s -> s.is(WildcatterBlocks.DIST_BREAKER_3P),
			'K', s -> s.is(WildcatterBlocks.RELAY_PANEL));
	private static final Map<Character, String> NAMES = Map.of('H', "138 kV breaker", 'D', "138 kV disconnect", 'B', "138 kV bus", 'b', "12.47 kV bus",
			'd', "12.47 kV disconnect", 'L', "12.47 kV feeder breaker", 'K', "relay panel (in front of the transformer)");

	private boolean formed;
	private String missing = "";
	private List<BlockPos> parts = List.of();
	/** 0 to 1: how far the time-overcurrent element has run toward a trip. */
	private double timer;
	private boolean lockedOut;
	private String lastTrip = "";
	private double lineAmps;

	public PowerTransformerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.POWER_TRANSFORMER_ENTITY, pos, state);
	}

	private Direction lineFacing() {
		// the transformer's front faces the 138 kV side; the layout is drawn with the line running west to east
		return getBlockState().getValue(TransformerBlock.FACING).getClockWise();
	}

	public boolean formed() {
		return formed;
	}

	/** The game test shrinks the relay settings by this factor, so a genset's small fault current can reach them. Null in play. */
	public static volatile Double testRelayScale;

	/** Full-load line current on the 138 kV side (the relay settings are multiples of it). */
	private double fullLoadAmps() {
		return Tuning.SUBSTATION_VA / (3 * Voltage.HIGH.nominal()) * (testRelayScale != null ? testRelayScale : 1.0);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, PowerTransformerBlockEntity t) {
		TransformerBlockEntity.tick(level, pos, state, t);
		if (level instanceof ServerLevel server && (level.getGameTime() + pos.hashCode()) % Tuning.STRUCTURE_CHECK_TICKS == 0) {
			t.checkStructure(server);
		}
	}

	private void checkStructure(ServerLevel level) {
		List<BlockPos> found = Multiblock.match(level, worldPosition, lineFacing(), LAYOUT, LEGEND);
		boolean was = formed;
		formed = found != null;
		missing = formed ? "" : String.valueOf(Multiblock.missing(level, worldPosition, lineFacing(), LAYOUT, LEGEND, NAMES));
		if (formed) {
			parts = found;
			Direction along = getBlockState().getValue(TransformerBlock.FACING).getOpposite();   // from the 138 kV side toward the 12.47 kV side
			for (BlockPos at : found) {
				BlockState s = level.getBlockState(at);
				if (s.getBlock() instanceof BreakerBlock && s.getValue(BreakerBlock.FACING) != along) {
					level.setBlock(at, s.setValue(BreakerBlock.FACING, along), 3);
				} else if (s.getBlock() instanceof DisconnectSwitchBlock && s.getValue(DisconnectSwitchBlock.FACING) != along) {
					level.setBlock(at, s.setValue(DisconnectSwitchBlock.FACING, along), 3);
				}
			}
		} else {
			parts = List.of();
		}
		if (formed != was) {
			if (formed) {
				level.playSound(null, worldPosition, WildcatterSounds.STRUCTURE_FORMED, SoundSource.BLOCKS, 1.5F, 0.7F);
			}
			CircuitManager.markDirty(level);
		}
		setChanged();
	}

	private BreakerBlockEntity breaker(Level level, net.minecraft.world.level.block.Block kind) {
		for (BlockPos at : parts) {
			if (level.getBlockState(at).is(kind) && level.getBlockEntity(at) instanceof BreakerBlockEntity b) {
				return b;
			}
		}
		return null;
	}

	private void tripBoth(String why) {
		if (level == null) {
			return;
		}
		for (net.minecraft.world.level.block.Block kind : new net.minecraft.world.level.block.Block[] {WildcatterBlocks.HV_BREAKER_3P, WildcatterBlocks.DIST_BREAKER_3P}) {
			BreakerBlockEntity b = breaker(level, kind);
			if (b != null) {
				b.tripByRelay(why);
			}
		}
		lockedOut = true;
		lastTrip = why;
		timer = 0;
		setChanged();
	}

	/** The relay panel's reset: clear the lockout and close both breakers again. */
	public void resetRelays() {
		if (level == null) {
			return;
		}
		lockedOut = false;
		timer = 0;
		for (net.minecraft.world.level.block.Block kind : new net.minecraft.world.level.block.Block[] {WildcatterBlocks.HV_BREAKER_3P, WildcatterBlocks.DIST_BREAKER_3P}) {
			BreakerBlockEntity b = breaker(level, kind);
			if (b != null) {
				b.closeByRelay();
			}
		}
		setChanged();
	}

	@Override
	public void build(Circuit circuit) {
		if (formed) {
			super.build(circuit);   // not formed: the transformer is not energized and connects nothing
		}
	}

	/** 50: an instantaneous trip on a heavy fault, before anything else sees it. */
	@Override
	public boolean protect(Circuit circuit, int ticks) {
		if (!formed || lockedOut) {
			return false;
		}
		super.apply(circuit, 0);
		double now = highWindingAmps() * Math.sqrt(3);   // line current of the delta
		if (now > fullLoadAmps() * Tuning.RELAY_50_MULTIPLE) {
			tripBoth(String.format("instantaneous overcurrent (50): %.0f A, set at %.0f A", now, fullLoadAmps() * Tuning.RELAY_50_MULTIPLE));
			return true;
		}
		return false;
	}

	/** 51: a time-overcurrent trip, sooner the heavier the overload (IEEE moderately inverse). */
	@Override
	public void apply(Circuit circuit, int ticks) {
		if (!formed) {
			return;
		}
		super.apply(circuit, ticks);
		lineAmps = highWindingAmps() * Math.sqrt(3);
		double m = lineAmps / (fullLoadAmps() * Tuning.RELAY_51_PICKUP);
		if (m > 1.0 && !lockedOut) {
			double seconds = Tuning.RELAY_TIME_DIAL * (0.0515 / (Math.pow(m, 0.02) - 1.0) + 0.114);
			timer += ticks / 20.0 / seconds;
			if (timer >= 1.0) {
				tripBoth(String.format("time overcurrent (51): %.0f A, %.1fx the pickup", lineAmps, m));
			}
		} else {
			timer = 0;
		}
	}

	@Override
	public Component inspect() {
		if (!formed) {
			return Component.literal("Substation power transformer (" + Voltage.HIGH.label() + " delta / " + Voltage.DISTRIBUTION.label() + " wye, "
					+ Math.round(Tuning.SUBSTATION_VA / 1e6) + " MVA): NOT BUILT, needs a " + missing + "; it is not energized until the lineup is complete");
		}
		String relay = lockedOut ? "; RELAY TRIPPED (" + lastTrip + "): right-click the relay panel to reset"
				: String.format("; relays: 51 picks up at %.0f A, 50 at %.0f A (now %.0f A)", fullLoadAmps() * Tuning.RELAY_51_PICKUP, fullLoadAmps() * Tuning.RELAY_50_MULTIPLE, lineAmps);
		return Component.literal("Substation: ").append(super.inspect()).append(relay);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("lockedOut", lockedOut);
		output.putString("lastTrip", lastTrip);
		output.putBoolean("formed", formed);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		lockedOut = input.getBooleanOr("lockedOut", false);
		lastTrip = input.getStringOr("lastTrip", "");
		formed = input.getBooleanOr("formed", false);
	}
}
