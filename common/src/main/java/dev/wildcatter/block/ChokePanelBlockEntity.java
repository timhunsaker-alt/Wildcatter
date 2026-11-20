package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.control.PlcNet;
import dev.wildcatter.control.PlcRequests;
import dev.wildcatter.platform.Platform;
import dev.wildcatter.well.KillModel;
import dev.wildcatter.well.WellControl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The remote choke panel (docs/multiblocks/oilfield-follow-ons.md, 23b): the choke console on the rig floor, with the choke's position,
 * the drill pipe and casing gauges, the stroke counter and pump rate, and a 0.2 kW hydraulic pump on a residential supply that moves the
 * hydraulic choke 10 % a second (without power the manual choke's handwheel moves 2 % a second). Its screen is where the kill is worked:
 * the kill sheet, the gauges and the drill pipe schedule as a chart. A PLC may hold the choke on CHOKE POSITION (4-20 mA), as a real
 * automatic choke does, reading DRILL PIPE PRESSURE and CASING PRESSURE (4-20 mA, 0-1,000 psi), STROKES and PUMP RATE.
 */
public class ChokePanelBlockEntity extends UnitMotorBlockEntity implements PlcRequests {
	private static final int HISTORY = 240;
	private boolean pumpOn;
	/** -1 closing, 0 still, 1 opening: the panel's choke lever, held. */
	private int jog;
	private double plcTarget = -1;
	private long plcAt = -1000;
	private @Nullable BlockPos rigAt;
	private final float[] dppHist = new float[HISTORY];
	private final float[] schedHist = new float[HISTORY];
	private final float[] casingHist = new float[HISTORY];
	private int histAt;
	private int histCount;
	private final Map<UUID, Long> viewers = new HashMap<>();
	private String note = "";

	public ChokePanelBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.REMOTE_CHOKE_PANEL_ENTITY, pos, state);
	}

	public boolean pumpOn() {
		return pumpOn;
	}

	/** The hydraulic pump has its supply: the hydraulic choke moves at its rate. */
	public boolean powered() {
		return testHydraulics || motor().output() > 0.5;
	}

	private boolean testHydraulics;

	/** Game test hook: the hydraulic pump as if on its supply. */
	public void testHydraulics() {
		testHydraulics = true;
	}

	/** Where the choke should go now: the PLC's command while it keeps coming, else the lever held. */
	public double chokeTarget(long now, double current) {
		if (now - plcAt < 40 && plcTarget >= 0) {
			return plcTarget;
		}
		return jog > 0 ? 1.0 : jog < 0 ? 0.0 : current;
	}

	void stopForSecondKick() {
		pumpOn = false;
		jog = 0;
		setChanged();
	}

	public @Nullable RigSubstructureBlockEntity rig() {
		return level != null && rigAt != null && level.getBlockEntity(rigAt) instanceof RigSubstructureBlockEntity r ? r : null;
	}

	// ---- the panel's controls, for the screen and game tests ----------------------------------------------------------------------
	/** PUMP: brings the rig's pump up to SCR, or stops it. */
	public String pump(boolean on) {
		RigSubstructureBlockEntity rig = rig();
		if (rig == null || rig.well() == null) {
			return "No kill under way: the panel works a shut-in well";
		}
		pumpOn = on;
		setChanged();
		return on ? "PUMP: up to " + Math.round(Tuning.SCR_SPM) + " spm (SCR): open the choke to hold casing pressure at SICP" : "PUMP: stopping";
	}

	/** The choke lever: -1 close, 0 hold, 1 open. */
	public void jog(int direction) {
		jog = Integer.signum(direction);
		setChanged();
	}

	public String method() {
		RigSubstructureBlockEntity rig = rig();
		return rig == null ? "No rig" : rig.toggleMethod();
	}

	// ---- PLC points ------------------------------------------------------------------------------------------------------------------
	@Override
	public String deviceName() {
		return "Remote choke panel";
	}

	@Override
	public List<Point> points() {
		return List.of(new Point("CHOKE POSITION command 4-20 mA (0-100 %)", true, Signal.MA), new Point("CHOKE POSITION 4-20 mA (0-100 %)", false, Signal.MA),
				new Point("DRILL PIPE PRESSURE 4-20 mA (0-1,000 psi)", false, Signal.MA), new Point("CASING PRESSURE 4-20 mA (0-1,000 psi)", false, Signal.MA),
				new Point("STROKES", false, Signal.PULSE), new Point("PUMP RATE 4-20 mA (0-60 spm)", false, Signal.MA), new Point("KILL UNDER WAY", false));
	}

	@Override
	public boolean readPoint(int point) {
		RigSubstructureBlockEntity rig = rig();
		return point == 6 && rig != null && rig.well() != null;
	}

	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
	}

	@Override
	public void commandMa(int point, double milliamps, long gameTime) {
		if (point == 0) {
			plcTarget = Math.max(0, Math.min(1, (milliamps - 4) / 16));
			plcAt = gameTime;
		}
	}

	@Override
	public double readMa(int point) {
		RigSubstructureBlockEntity rig = rig();
		KillModel well = rig == null ? null : rig.well();
		if (well == null) {
			return point >= 1 && point <= 5 ? 4 : 0;
		}
		double v = switch (point) {
			case 1 -> well.choke();
			case 2 -> well.dppGauge() / 1000;
			case 3 -> well.casingPsi() / 1000;
			case 5 -> well.spm() / 60;
			default -> 0;
		};
		return 4 + 16 * Math.max(0, Math.min(1.03, v));
	}

	@Override
	public long readPulses(int point) {
		RigSubstructureBlockEntity rig = rig();
		return point == 4 && rig != null && rig.well() != null ? (long) rig.well().strokes() : 0;
	}

	// ---- each interval: tell the rig, keep the chart, feed the screens ------------------------------------------------------------
	@Override
	protected void work(Level level) {
		power(level);
		BlockPos origin = LandRig.substructureNear(level, worldPosition);
		rigAt = origin != null && origin.closerThan(worldPosition, Tuning.CHOKE_REACH + 9) ? origin : null;
		RigSubstructureBlockEntity rig = rig();
		if (rig != null) {
			rig.panelHere(worldPosition, level.getGameTime());
		}
		KillModel well = rig == null ? null : rig.well();
		if (well != null && level.getGameTime() % 20 < Tuning.WELL_INTERVAL) {
			dppHist[histAt] = (float) well.dppGauge();
			schedHist[histAt] = (float) rig.schedulePsi();
			casingHist[histAt] = (float) well.casingPsi();
			histAt = (histAt + 1) % HISTORY;
			histCount = Math.min(HISTORY, histCount + 1);
		} else if (well == null) {
			histCount = 0;
			pumpOn = false;
		}
		note = rig == null ? "NOT ON A RIG FLOOR: set it on a land rig's floor" : well == null ? "no kill: " + rig.phase() : rig.killNote();
		if (level instanceof ServerLevel server && !viewers.isEmpty()) {
			CompoundTag tag = state(true);
			List<UUID> gone = new ArrayList<>();
			for (Map.Entry<UUID, Long> v : viewers.entrySet()) {
				ServerPlayer player = server.getServer().getPlayerList().getPlayer(v.getKey());
				if (player == null || player.blockPosition().distSqr(worldPosition) > 64 || level.getGameTime() - v.getValue() > 20 * 600) {
					gone.add(v.getKey());
				} else {
					Platform.INSTANCE.sendToPlayer(player, new PlcNet.Payload(tag));
				}
			}
			gone.forEach(viewers::remove);
		}
	}

	/** Opens the screen for a player (a click). */
	public void open(ServerPlayer player) {
		viewers.put(player.getUUID(), player.level().getGameTime());
		CompoundTag tag = state(true);
		tag.putBoolean("open", true);
		Platform.INSTANCE.sendToPlayer(player, new PlcNet.Payload(tag));
	}

	@Override
	public void request(ServerPlayer player, CompoundTag tag) {
		String cmd = tag.getStringOr("chokeCmd", "");
		String reply = switch (cmd) {
			case "pump" -> pump(!pumpOn);
			case "method" -> method();
			case "jog" -> {
				jog(tag.getIntOr("jog", 0));
				yield "";
			}
			case "close" -> {
				viewers.remove(player.getUUID());
				yield "";
			}
			default -> "";
		};
		if (!reply.isEmpty()) {
			player.sendOverlayMessage(Component.literal(reply));
		}
	}

	/** What the screen shows. */
	public CompoundTag state(boolean history) {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("chokePanel", true);
		tag.putInt("x", worldPosition.getX());
		tag.putInt("y", worldPosition.getY());
		tag.putInt("z", worldPosition.getZ());
		tag.putString("note", note);
		tag.putBoolean("powered", powered());
		tag.putBoolean("pumpOn", pumpOn);
		RigSubstructureBlockEntity rig = rig();
		KillModel well = rig == null ? null : rig.well();
		if (rig != null) {
			tag.putString("status", rig.inspect().getString());
			tag.putDouble("mud", rig.mudPpg());
			tag.putDouble("gain", rig.shownGain());
			tag.putBoolean("choke", rig.hasChoke());
			tag.putBoolean("separator", rig.hasSeparator());
			tag.putInt("method", rig.drillersMethod());
		}
		WellControl.KillSheet sheet = rig == null ? null : rig.killSheet();
		if (well != null && sheet != null) {
			tag.putBoolean("kill", true);
			tag.putDouble("chokePos", well.choke());
			tag.putDouble("spm", well.spm());
			tag.putDouble("strokes", well.strokes());
			tag.putDouble("dpp", well.dppGauge());
			tag.putDouble("casing", well.casingPsi());
			tag.putDouble("schedule", rig.schedulePsi());
			tag.putDouble("sidpp", sheet.sidpp());
			tag.putDouble("sicp", sheet.sicp());
			tag.putDouble("kmw", sheet.kmw());
			tag.putDouble("icp", sheet.icp());
			tag.putDouble("fcp", sheet.fcp());
			tag.putDouble("maasp", rig.maasp());
			tag.putInt("toBit", sheet.strokesToBit());
			tag.putInt("toShoe", sheet.strokesToBit() + sheet.strokesBitToShoe());
			tag.putInt("toSurface", sheet.strokesCirculation());
			tag.putDouble("shoeEmw", well.shoeEmw());
			tag.putDouble("fg", well.fgPpg);
			if (history) {
				int n = histCount;
				int[] d = new int[n], s = new int[n], c = new int[n];
				for (int i = 0; i < n; i++) {
					int k = (histAt - n + i + HISTORY * 2) % HISTORY;
					d[i] = Math.round(dppHist[k] * 10);
					s[i] = Math.round(schedHist[k] * 10);
					c[i] = Math.round(casingHist[k] * 10);
				}
				tag.putIntArray("hDpp", d);
				tag.putIntArray("hSched", s);
				tag.putIntArray("hCasing", c);
			}
		}
		return tag;
	}

	@Override
	public Component inspect() {
		RigSubstructureBlockEntity rig = rig();
		KillModel well = rig == null ? null : rig.well();
		String gauges = well == null ? "" : String.format("; choke %.0f %%, DPP %,.0f psi (schedule %,.0f), casing %,.0f psi, %.0f spm, %,.0f strokes",
				well.choke() * 100, well.dppGauge(), rig.schedulePsi(), well.casingPsi(), well.spm(), well.strokes());
		return Component.literal("Remote choke panel (hydraulic pump " + Math.round(Tuning.CHOKE_PANEL_WATTS) + " W): " + note + gauges + "; " + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("pumpOn", pumpOn);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		pumpOn = input.getBooleanOr("pumpOn", false);
	}
}
