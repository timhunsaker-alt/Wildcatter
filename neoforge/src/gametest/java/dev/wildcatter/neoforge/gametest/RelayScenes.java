package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.abs;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.check;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.floor;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.genset;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.place;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.put;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.read;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.start;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.BreakerRelayCabinetBlockEntity;
import dev.wildcatter.block.MultiCableBlockEntity;
import dev.wildcatter.block.SwitchgearSectionBlockEntity;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.RelaySettings;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;

/**
 * NeoForge server scenes of the protection gap wave, group A (docs/questions/protection-gaps.md): the relays' settable curve, dial and
 * 50, the switchgear's racking time and remote racking, breaker failure (50BF), the 4.16 kV lineup's bus differential (87B), and the
 * breaker relay cabinet's reclosing (79).
 * <ul>
 * <li>{@code switchgear_settings}: a feeder set to EI, TD 0.5, 5 A trips on its load by 51; the cursor steps the settings; a rack takes
 * its 15 s, refusing a close meanwhile; remote racking from a PLC point brings it back to CONNECTED.</li>
 * <li>{@code switchgear_bf_87b}: a feeder with a stuck mechanism fails to clear a fault on its cable, and 50BF trips the main and the
 * other feeder (86BF); then grounds on the lineup's bus trip all three by 87B (86B), and a feeder refuses to close until the main is
 * reset.</li>
 * <li>{@code cabinet_reclose}: a 12.47 kV yard breaker and its relay cabinet: a temporary fault is reclosed by 79's first shot; a
 * permanent one trips through the remaining shots to LOCKED OUT; the hand reset closes it again.</li>
 * </ul>
 */
final class RelayScenes {
	private RelayScenes() {
	}

	private static final int U = 2;
	private static final Direction NORTH = Direction.NORTH;

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> scenes = new LinkedHashMap<>();
		scenes.put("switchgear_settings", RelayScenes::switchgearSettings);
		scenes.put("switchgear_bf_87b", RelayScenes::switchgearBreakerFailure);
		scenes.put("cabinet_reclose", RelayScenes::cabinetReclose);
		return scenes;
	}

	// ---- the lineup ----------------------------------------------------------------------------------------------------------------
	/**
	 * A pad-mount or unit substation in a line, fed from (x, U, z) going east: on its main cell's terminal if {@code feedMain}, else
	 * on the far end's (the client test's inlineTransformer). Returns the x of the cell its other terminal feeds.
	 */
	private static int inline(GameTestHelper helper, Block block, int x, int z, boolean feedMain) {
		int across = block == WildcatterBlocks.UNIT_SUBSTATION ? 3 : 1;
		Direction facing = feedMain ? Direction.EAST.getCounterClockWise() : Direction.EAST.getClockWise();
		place(helper, block, feedMain ? x + 1 : x + 1 + across, U, z, facing);
		return x + across + 2;
	}

	/**
	 * A genset at x 10 (skid x 5-10, z 0..1), a unit substation run backward to 4.16 kV (x 11-14), cable to the lineup facing north:
	 * feeder 3 at x 21, the main at x 22, feeder 1 at x 23 (z -3..-1). Feeder 1 serves a unit substation and four 15 kW heaters,
	 * feeder 3 a dead-end cable at (19..21, U+3, -1).
	 */
	static void lineup(GameTestHelper helper) {
		floor(helper, -2, -8, 40, 8);
		genset(helper, 10, U, 0);
		inline(helper, WildcatterBlocks.UNIT_SUBSTATION, 10, 0, false);
		for (int x = 15; x <= 22; x++) {
			put(helper, "cable_mv", x, U, 0);
		}
		place(helper, WildcatterBlocks.MV_SWITCHGEAR_FEEDER, 21, U, -3, NORTH);
		place(helper, WildcatterBlocks.MV_SWITCHGEAR_MAIN, 22, U, -3, NORTH);
		place(helper, WildcatterBlocks.MV_SWITCHGEAR_FEEDER, 23, U, -3, NORTH);
		for (int x = 23; x <= 26; x++) {
			put(helper, "cable_mv", x, U + 3, -1);
		}
		for (int y = U; y <= U + 2; y++) {
			put(helper, "cable_mv", 26, y, -1);
		}
		int out = inline(helper, WildcatterBlocks.UNIT_SUBSTATION, 26, -1, true);
		for (int x = out; x <= out + 2; x++) {
			put(helper, "cable_industrial", x, U, -1);
		}
		for (int[] at : new int[][] {{out + 1, -2}, {out + 2, -2}, {out + 1, 0}, {out + 2, 0}}) {
			put(helper, "three_phase_heater", at[0], U, at[1]);
		}
		for (int x = 19; x <= 21; x++) {
			put(helper, "cable_mv", x, U + 3, -1);
		}
	}

	static SwitchgearSectionBlockEntity section(GameTestHelper helper, int x) {
		return GridSceneKit.entity(helper, x, U, -3, SwitchgearSectionBlockEntity.class);
	}

	static void closeAll(GameTestHelper helper, int mainPickup) {
		for (int x = 21; x <= 23; x++) {
			SwitchgearSectionBlockEntity s = section(helper, x);
			s.setPickup(x == 22 ? mainPickup : 25);
			s.operate();
		}
		CircuitManager.markDirty(helper.getLevel());
	}

	private static void grounds(GameTestHelper helper, int x, int y, int z, boolean on) {
		GridSceneKit.entity(helper, x, y, z, MultiCableBlockEntity.class).setGrounded(on);
	}

	// ---- switchgear_settings -------------------------------------------------------------------------------------------------------
	static void switchgearSettings(GameTestHelper helper) {
		lineup(helper);
		closeAll(helper, 600);
		start(helper, 10, U, 0, () -> helper.runAfterDelay(120, () -> {
			String f1 = read(helper, 23, U, -3);
			log("switchgear_settings", "running | " + read(helper, 22, U, -3) + " | " + f1);
			check(helper, f1.contains("CLOSED, CONNECTED") && !f1.contains("CONNECTED, 0 A"), "feeder 1 should carry its heaters: " + f1);
			SwitchgearSectionBlockEntity feeder1 = section(helper, 23);
			// the cursor: pickup, curve, dial, 50 in turn; the selected one in brackets
			feeder1.nextSetting();
			check(helper, read(helper, 23, U, -3).contains("[MI]"), "the cursor should be on the curve: " + read(helper, 23, U, -3));
			feeder1.nextPickup();                                   // MI -> VI
			feeder1.nextPickup();                                   // VI -> EI
			feeder1.nextSetting();
			check(helper, read(helper, 23, U, -3).contains("[TD 1]"), "the cursor should be on the dial: " + read(helper, 23, U, -3));
			feeder1.settings().setDial(0.5);
			feeder1.nextSetting();
			check(helper, read(helper, 23, U, -3).contains("[50 at"), "the cursor should be on 50: " + read(helper, 23, U, -3));
			feeder1.nextSetting();                                  // back to the pickup
			double amps = feeder1.pickup() * 7;
			double ei = feeder1.clearingSeconds(amps);
			feeder1.settings().setCurve(RelaySettings.Curve.MI);
			double mi = feeder1.clearingSeconds(amps);
			feeder1.settings().setCurve(RelaySettings.Curve.EI);
			log("switchgear_settings", String.format("at %.0f A the EI TD 0.5 relay clears in %.2f s, MI in %.2f s: %s", amps, ei, mi, feeder1.curveName()));
			check(helper, ei > 0 && ei < 100 && mi > ei, "at 7 x pickup the EI curve at TD 0.5 should clear sooner than MI: EI " + ei + " MI " + mi);
			feeder1.setPickup(5);                                   // under its load: 51 runs on EI, TD 0.5
			helper.runAfterDelay(400, () -> {
				String tripped = read(helper, 23, U, -3);
				log("switchgear_settings", "51 | " + tripped + " | " + read(helper, 22, U, -3));
				check(helper, feeder1.tripped() && tripped.contains("51 time overcurrent") && tripped.contains("(EI, TD 0.5)"), "feeder 1's 51 should trip it on EI, TD 0.5: " + tripped);
				check(helper, read(helper, 22, U, -3).contains("CLOSED, CONNECTED"), "the main should stay closed: " + read(helper, 22, U, -3));
				// racking takes its time: TEST is reached after 15 s, and a close is refused meanwhile
				check(helper, feeder1.rack() && feeder1.racking() && !feeder1.operate(), "a rack should start and refuse a close while it runs");
				String racking = read(helper, 23, U, -3);
				check(helper, racking.contains("RACKING from CONNECTED to TEST"), "the readout should say it is racking: " + racking);
				helper.runAfterDelay(100, () -> {
					check(helper, feeder1.racking() && feeder1.racked() == SwitchgearSectionBlockEntity.Rack.CONNECTED, "after 5 s the truck should still be moving");
					helper.runAfterDelay(260, () -> {
						check(helper, !feeder1.racking() && feeder1.racked() == SwitchgearSectionBlockEntity.Rack.TEST, "after 15 s it should stand in TEST: " + read(helper, 23, U, -3));
						// remote racking: the selector to REMOTE, then a RACK IN command from the PLC
						feeder1.cycleHoa(feeder1.getBlockPos());
						feeder1.cycleHoa(feeder1.getBlockPos());
						check(helper, feeder1.remote(), "the selector should be on REMOTE");
						feeder1.commandPoint(6, true, helper.getLevel().getGameTime());
						check(helper, feeder1.racking() && feeder1.readPoint(9), "RACK IN should start the remote racking motor");
						helper.runAfterDelay(330, () -> {
							feeder1.commandPoint(6, false, helper.getLevel().getGameTime());
							check(helper, feeder1.racked() == SwitchgearSectionBlockEntity.Rack.CONNECTED && feeder1.readPoint(8), "remote racking should bring it to CONNECTED: " + read(helper, 23, U, -3));
							log("switchgear_settings", "ok | " + read(helper, 23, U, -3));
							helper.succeed();
						});
					});
				});
			});
		}));
	}

	// ---- switchgear_bf_87b ---------------------------------------------------------------------------------------------------------
	static void switchgearBreakerFailure(GameTestHelper helper) {
		lineup(helper);
		closeAll(helper, 200);
		start(helper, 10, U, 0, () -> helper.runAfterDelay(120, () -> {
			SwitchgearSectionBlockEntity main = section(helper, 22);
			SwitchgearSectionBlockEntity feeder1 = section(helper, 23);
			SwitchgearSectionBlockEntity feeder3 = section(helper, 21);
			check(helper, read(helper, 22, U, -3).contains("87B ready"), "the main should carry the lineup's 87B: " + read(helper, 22, U, -3));
			// ---- 50BF: feeder 1's mechanism sticks; its pickup set under its load, its 51 trips it and it fails to open; 50BF opens the rest
			feeder1.stickMechanism(true);
			feeder1.setPickup(5);
			helper.runAfterDelay(400, () -> {
				String f1 = read(helper, 23, U, -3);
				String m = read(helper, 22, U, -3);
				String f3 = read(helper, 21, U, -3);
				log("switchgear_bf_87b", "50BF | " + f1 + " | " + m + " | " + f3);
				check(helper, f1.contains("FAILED TO OPEN") && feeder1.closed(), "feeder 1 should have failed to open: " + f1);
				check(helper, !main.closed() && m.contains("50BF BREAKER FAILURE") && m.contains("LOCKED OUT"), "50BF should trip the main and lock out: " + m);
				check(helper, !feeder3.closed() && f3.contains("50BF"), "50BF should trip feeder 3: " + f3);
				check(helper, !feeder3.operate(), "feeder 3 should refuse to close while the main is locked out");
				// feeder 1 opened by hand (its manual trip), its pickup back, the main reset, both feeders closed
				feeder1.operate();
				check(helper, !feeder1.closed(), "a hand opens the stuck breaker");
				feeder1.setPickup(25);
				check(helper, main.operate() && main.closed(), "the main's right-click should reset 86BF and close it");
				check(helper, feeder1.operate() && feeder3.operate(), "the feeders should close once the main is reset");
				CircuitManager.markDirty(helper.getLevel());
				helper.runAfterDelay(60, () -> {
					String back = read(helper, 23, U, -3);
					check(helper, back.contains("CLOSED, CONNECTED") && !back.contains("CONNECTED, 0 A"), "feeder 1 should carry its load again: " + back);
					// ---- 87B: grounds on the lineup's bus, in feeder 1's bus compartment
					feeder1.setGrounded(true);
					helper.runAfterDelay(40, () -> {
						String m2 = read(helper, 22, U, -3);
						String f12 = read(helper, 23, U, -3);
						log("switchgear_bf_87b", "87B | " + m2 + " | " + f12);
						check(helper, m2.contains("87B BUS DIFFERENTIAL TRIP") && m2.contains("LOCKED OUT") && !main.closed(), "grounds on the bus should trip 87B: " + m2);
						check(helper, !feeder1.closed() && f12.contains("87B") && f12.contains("bus GROUNDED"), "87B should open feeder 1 too: " + f12);
						check(helper, !feeder1.operate(), "feeder 1 should refuse to close while 86B holds");
						feeder1.setGrounded(false);
						main.operate();
						feeder1.operate();
						CircuitManager.markDirty(helper.getLevel());
						helper.runAfterDelay(60, () -> {
							String again = read(helper, 23, U, -3);
							check(helper, again.contains("CLOSED, CONNECTED") && !again.contains("CONNECTED, 0 A"), "after the reset feeder 1 should carry again: " + again);
							log("switchgear_bf_87b", "ok | " + again);
							helper.succeed();
						});
					});
				});
			});
		}));
	}

	// ---- cabinet_reclose ---------------------------------------------------------------------------------------------------------------
	/**
	 * A genset at x 10, a 300 kVA pad-mount run backward (480 V to 12.47 kV, x 11), 12.47 kV cable to a yard breaker at x 15-16 (z -1..1,
	 * terminals on z 0) worked by a relay cabinet north of it (z -2), then cable east to x 20 where the grounds go.
	 */
	static void cabinetReclose(GameTestHelper helper) {
		floor(helper, -2, -6, 30, 6);
		genset(helper, 10, U, 0);
		int out = inline(helper, WildcatterBlocks.PADMOUNT_480, 10, 0, true);
		for (int x = out; x <= 14; x++) {
			put(helper, "cable_dist", x, U, 0);
		}
		place(helper, WildcatterBlocks.DIST_BREAKER_3P, 15, U, -1, NORTH);
		place(helper, WildcatterBlocks.BREAKER_RELAY_CABINET, 15, U, -2, NORTH);
		for (int x = 17; x <= 20; x++) {
			put(helper, "cable_dist", x, U, 0);
		}
		start(helper, 10, U, 0, () -> helper.runAfterDelay(60, () -> {
			BreakerRelayCabinetBlockEntity cabinet = GridSceneKit.entity(helper, 15, U, -2, BreakerRelayCabinetBlockEntity.class);
			cabinet.setPickup(5);                                   // 50 at 40 A
			String ready = read(helper, 15, U, -2);
			log("cabinet_reclose", "ready | " + ready);
			check(helper, ready.contains("CLOSED") && ready.contains("79 3 shots"), "a 12.47 kV cabinet should start with three shots: " + ready);
			// ---- a temporary fault: tripped, then reclosed by the first shot once the grounds are gone
			grounds(helper, 20, U, 0, true);
			helper.runAfterDelay(10, () -> {
				String open = read(helper, 15, U, -2);
				log("cabinet_reclose", "temporary | " + open);
				check(helper, open.contains("TRIPPED") && open.contains("79 recloses in"), "the fault should trip it and 79 time its first shot: " + open);
				grounds(helper, 20, U, 0, false);
				helper.runAfterDelay(80, () -> {
					String reclosed = read(helper, 15, U, -2);
					log("cabinet_reclose", "shot 1 | " + reclosed);
					check(helper, reclosed.contains("CLOSED") && reclosed.contains("shot 1 of 3") && cabinet.shotsUsed() == 1, "79's first shot should reclose it: " + reclosed);
					// ---- a permanent fault: shots 2 and 3 (15 s and 30 s) close onto it; the fourth trip locks out
					grounds(helper, 20, U, 0, true);
					helper.runAfterDelay(1100, () -> {
						String locked = read(helper, 15, U, -2);
						log("cabinet_reclose", "permanent | " + locked + " | " + read(helper, 15, U, -1));
						check(helper, cabinet.lockedOut() && locked.contains("79 LOCKED OUT after 3 shots"), "a permanent fault should run 79 to lockout: " + locked);
						check(helper, read(helper, 15, U, -1).contains("TRIPPED"), "the breaker should stay open: " + read(helper, 15, U, -1));
						grounds(helper, 20, U, 0, false);
						cabinet.reset();
						helper.runAfterDelay(40, () -> {
							String back = read(helper, 15, U, -2);
							check(helper, back.contains("CLOSED") && !cabinet.lockedOut(), "the hand reset should close it again: " + back);
							log("cabinet_reclose", "ok | " + back);
							helper.succeed();
						});
					});
				});
			});
		}));
	}

	static net.minecraft.core.BlockPos at(GameTestHelper helper, int x, int y, int z) {
		return abs(helper, x, y, z);
	}
}
