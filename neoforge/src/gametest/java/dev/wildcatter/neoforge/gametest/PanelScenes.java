package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.abs;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.check;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.entity;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.floor;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.genset;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.gensetPanel;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.place;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.put;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.read;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.start;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.block.BreakerBlockEntity;
import dev.wildcatter.block.GensetPartBlockEntity;
import dev.wildcatter.block.GensetSkid;
import dev.wildcatter.block.MultiCableBlockEntity;
import dev.wildcatter.block.PanelBlockEntity;
import dev.wildcatter.block.PanelGutterBlock;
import dev.wildcatter.block.PanelGutterBlockEntity;
import dev.wildcatter.block.SafetySwitchBlockEntity;
import dev.wildcatter.block.TransferSwitchBlockEntity;
import dev.wildcatter.circuit.CircuitManager;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Mob;

/**
 * NeoForge server scenes of the protection gap wave, group C (docs/questions/protection-gaps.md, C-entries):
 * <ul>
 * <li>{@code panel_gfci_shock}: a load centre on a pole transformer. A pig against a bare wire on a plain branch is shocked and nothing
 * trips; against a bare wire on the GFCI's branch its body's current to earth trips the GFCI "ground fault". The main's handle is drawn
 * ON, OFF and TRIPPED, and a gutter's coordination readout names the main.</li>
 * <li>{@code fuse_let_through}: a 400 A three-pole breaker feeding a safety switch with 30 A class J fuses. A fault past the switch is
 * limited by the fuses and the breaker, whose magnetic pickup is under the prospective fault, stays closed.</li>
 * <li>{@code ats_timers}: the ATS on its factory timers: normal lost, it starts the standby set and transfers; normal back, it waits
 * the real 30 min retransfer delay. On the short timers it retransfers in 30 s.</li>
 * <li>{@code ats_closed_in_phase}: a test transfer by closed transition (make before break, under 100 ms, never paralleled outside
 * it), then a retransfer by the in-phase monitor while the sources slip apart at 0.1 Hz.</li>
 * </ul>
 */
final class PanelScenes {
	private PanelScenes() {
	}

	private static final int U = 1;

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> scenes = new LinkedHashMap<>();
		scenes.put("panel_gfci_shock", PanelScenes::panelGfciShock);
		scenes.put("fuse_let_through", PanelScenes::fuseLetThrough);
		scenes.put("ats_timers", PanelScenes::atsTimers);
		scenes.put("ats_closed_in_phase", PanelScenes::atsClosedInPhase);
		return scenes;
	}

	// ---- helpers ---------------------------------------------------------------------------------------------------------------------
	/** Places a block from its state string, e.g. "wildcatter:pole_transformer[facing=west]". */
	private static void state(GameTestHelper helper, String text, int x, int y, int z) {
		try {
			helper.getLevel().setBlock(abs(helper, x, y, z), BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, text, false).blockState(), 3);
		} catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
			throw new IllegalArgumentException(text, e);
		}
	}

	/** Polls {@code done} every 10 ticks up to {@code limit}; then runs {@code then} with "yes after N" or "NO after N". */
	private static void waitFor(GameTestHelper helper, BooleanSupplier done, int limit, Consumer<String> then) {
		waitFor(helper, done, limit, 0, then);
	}

	private static void waitFor(GameTestHelper helper, BooleanSupplier done, int limit, int waited, Consumer<String> then) {
		if (done.getAsBoolean() || waited >= limit) {
			then.accept((done.getAsBoolean() ? "yes after " : "NO after ") + waited + " ticks");
			return;
		}
		helper.runAfterDelay(10, () -> waitFor(helper, done, limit, waited + 10, then));
	}

	private static PanelGutterBlockEntity gutter(GameTestHelper helper, int x, int y, int z, String item) {
		PanelGutterBlockEntity g = entity(helper, x, y, z, PanelGutterBlockEntity.class);
		String refused = g.install(WildcatterItems.BRANCH_BREAKERS.get(item));
		check(helper, refused == null, item + " should clip in at " + x + ", " + y + ", " + z + ": " + refused);
		g.setClosed(true);
		return g;
	}

	private static Mob pig(GameTestHelper helper, int x, int y, int z) {
		Mob pig = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(x, y, z));
		pig.setPermanentlyInvulnerable(true);   // shocked, never killed: its body stays across the wire
		pig.setNoAi(true);
		return pig;
	}

	// ---- panel_gfci_shock -------------------------------------------------------------------------------------------------------------
	static void panelGfciShock(GameTestHelper helper) {
		floor(helper, 0, -8, 32, 10);
		// a genset, a 480 V pad-mount run backward to 12.47 kV, a 25 kVA pole unit, 120/240 V into the load centre's top
		genset(helper, 10, U, 0);
		place(helper, WildcatterBlocks.PADMOUNT_480, 11, U, 0, Direction.NORTH);
		for (int x = 13; x <= 15; x++) {
			put(helper, "dist_wire_hot", x, U, 0);
		}
		put(helper, "pole", 16, U, -1);
		state(helper, "wildcatter:pole_transformer[facing=west]", 16, U, 0);
		put(helper, "cable_lv", 17, U, 0);
		put(helper, "cable_lv", 16, U, 1);
		put(helper, "cable_lv", 17, U, 1);
		put(helper, "cable_lv", 17, U + 1, 1);
		for (int x = 17; x <= 22; x++) {
			put(helper, "cable_lv", x, U + 2, 1);
		}
		put(helper, "cable_lv", 22, U + 2, 2);
		put(helper, "cable_lv", 22, U + 2, 3);
		for (int x = 19; x <= 25; x++) {
			for (int y = U; y <= U + 3; y++) {
				state(helper, "minecraft:bricks", x, y, 4);
			}
		}
		place(helper, WildcatterBlocks.LOAD_CENTRE, 22, U + 1, 3, Direction.NORTH);
		place(helper, WildcatterBlocks.PANEL_GUTTER, 22, U, 3, Direction.NORTH);       // space 1: the GFCI, its bare wire at floor level
		place(helper, WildcatterBlocks.PANEL_GUTTER, 23, U + 1, 3, Direction.NORTH);   // space 2: a plain 20 A, its bare wire a block up
		place(helper, WildcatterBlocks.PANEL_GUTTER, 21, U + 1, 3, Direction.NORTH);   // space 3: a 2-pole 30 A, for the fault
		state(helper, "wildcatter:branch_cable[axis=z]", 22, U, 2);
		state(helper, "wildcatter:branch_cable[axis=z]", 22, U, 1);
		put(helper, "cable_lv", 22, U, 0);
		put(helper, "wire_hot", 22, U, -1);
		put(helper, "wire_hot", 22, U, -2);
		state(helper, "wildcatter:branch_cable[axis=z]", 23, U + 1, 2);
		state(helper, "wildcatter:branch_cable[axis=z]", 23, U + 1, 1);
		put(helper, "cable_lv", 23, U + 1, 0);
		put(helper, "wire_hot", 23, U + 1, -1);
		put(helper, "wire_hot", 23, U + 1, -2);
		state(helper, "minecraft:stone", 23, U, -1);
		state(helper, "minecraft:stone", 23, U, -2);
		state(helper, "wildcatter:branch_cable[axis=z]", 21, U + 1, 2);
		state(helper, "wildcatter:branch_cable[axis=z]", 21, U + 1, 1);
		put(helper, "electric_heater", 21, U + 1, 0);
		helper.runAfterDelay(5, () -> {
			gutter(helper, 22, U, 3, "gfci_breaker_1p_20");
			gutter(helper, 23, U + 1, 3, "branch_breaker_1p_20");
			gutter(helper, 21, U + 1, 3, "branch_breaker_2p_30");
			CircuitManager.markDirty(helper.getLevel());
		});
		start(helper, 10, U, 0, () -> helper.runAfterDelay(60, () -> {
			PanelBlockEntity panel = entity(helper, 22, U + 1, 3, PanelBlockEntity.class);
			String gfci = read(helper, 22, U, 3);
			String plain = read(helper, 23, U + 1, 3);
			log("panel_gfci_shock", "before | " + read(helper, 22, U + 1, 3) + " | " + gfci + " | " + plain + " | wire " + read(helper, 22, U, -2) + " | " + read(helper, 23, U + 1, -2));
			check(helper, panel.handle() == PanelGutterBlock.Slot.ON && panel.closed(), "the main's handle should be drawn ON: " + panel.handle());
			check(helper, gfci.contains("GFCI: ON") && gfci.contains("imbalance 0.0 mA"), "the GFCI should be on and balanced: " + gfci);
			check(helper, gfci.contains("coordination: under the 200 A main") && gfci.contains("selective only up to"),
					"the branch's coordination readout should name the main and its limit: " + gfci);
			check(helper, read(helper, 22, U, -2).matches(".*: 1[0-2]\\d V.*") && read(helper, 23, U + 1, -2).matches(".*: 1[0-2]\\d V.*"),
					"both bare wires should be live: " + read(helper, 22, U, -2) + " | " + read(helper, 23, U + 1, -2));
			// the main by hand: OFF, then ON again
			panel.setMain(false);
			check(helper, panel.handle() == PanelGutterBlock.Slot.OFF, "the main's handle should be drawn OFF: " + panel.handle());
			panel.setMain(true);
			CircuitManager.markDirty(helper.getLevel());
			// a pig against the plain branch's bare wire: shocked, and nothing trips
			Mob one = pig(helper, 23, U + 1, -2);
			helper.runAfterDelay(60, () -> {
				String plainAfter = read(helper, 23, U + 1, 3);
				log("panel_gfci_shock", "plain branch shock | " + plainAfter + " | pig hurt " + one.hurtTime);
				check(helper, plainAfter.contains(": ON"), "a shock on a plain branch should trip nothing: " + plainAfter);
				one.discard();
				// a pig against the GFCI's bare wire: its body's current to earth trips the GFCI
				pig(helper, 22, U, -2);
				waitFor(helper, () -> entity(helper, 22, U, 3, PanelGutterBlockEntity.class).tripped(), 200, how -> {
					String tripped = read(helper, 22, U, 3);
					log("panel_gfci_shock", "GFCI shock " + how + " | " + tripped);
					check(helper, tripped.contains("TRIPPED (ground fault"), "a body across the GFCI's bare wire should trip it on ground fault: " + tripped);
					// a fault on the 2-pole branch: under the main's magnetic pickup, so the branch opens alone, as its readout said
					entity(helper, 21, U + 1, 2, MultiCableBlockEntity.class).setGrounded(true);
					helper.runAfterDelay(20, () -> {
						PanelBlockEntity main = entity(helper, 22, U + 1, 3, PanelBlockEntity.class);
						String m = read(helper, 22, U + 1, 3);
						String s3 = read(helper, 21, U + 1, 3);
						log("panel_gfci_shock", "fault | " + m + " | " + s3);
						check(helper, s3.contains("TRIPPED"), "grounds on the 2-pole branch should trip it: " + s3);
						check(helper, main.handle() == (main.tripped() ? PanelGutterBlock.Slot.TRIPPED : main.closed() ? PanelGutterBlock.Slot.ON : PanelGutterBlock.Slot.OFF),
								"the main's handle should follow its state: " + main.handle() + " | " + m);
						java.util.regex.Matcher limit = java.util.regex.Pattern.compile("selective only up to ([\\d,]+) A").matcher(s3);
						java.util.regex.Matcher fault = java.util.regex.Pattern.compile("short circuit: ([\\d,]+) A").matcher(s3);
						check(helper, limit.find() && fault.find(), "the branch should read its fault and its selectivity: " + s3);
						boolean under = Double.parseDouble(fault.group(1).replace(",", "")) < Double.parseDouble(limit.group(1).replace(",", ""));
						check(helper, under == !main.tripped(), "the main should hold exactly when the fault is under the readout's selective limit: " + m + " | " + s3);
						// the main's own trip, drawn TRIPPED
						main.tripForTest();
						check(helper, main.handle() == PanelGutterBlock.Slot.TRIPPED, "a tripped main's handle should be drawn TRIPPED: " + main.handle());
						main.setMain(true);
						check(helper, main.handle() == PanelGutterBlock.Slot.ON, "reset, it should be drawn ON again: " + main.handle());
						helper.succeed();
					});
				});
			});
		}));
	}

	// ---- fuse_let_through ------------------------------------------------------------------------------------------------------------
	static void fuseLetThrough(GameTestHelper helper) {
		floor(helper, 0, -6, 24, 6);
		genset(helper, 10, U, 0);
		put(helper, "cable_industrial", 11, U, 0);
		put(helper, "cable_industrial", 12, U, 0);
		state(helper, "wildcatter:breaker_3p_industrial[facing=east]", 13, U, 0);
		put(helper, "cable_industrial", 14, U, 0);
		put(helper, "cable_industrial", 15, U, 0);
		put(helper, "cable_industrial", 15, U + 1, 0);
		put(helper, "cable_industrial", 15, U + 2, 0);
		put(helper, "cable_industrial", 16, U + 2, 0);
		put(helper, "cable_industrial", 17, U + 2, 0);
		place(helper, WildcatterBlocks.SAFETY_SWITCH_100, 17, U + 1, 0, Direction.NORTH);
		put(helper, "cable_industrial", 17, U, 0);
		put(helper, "three_phase_heater", 18, U, 0);
		helper.runAfterDelay(5, () -> {
			BreakerBlockEntity breaker = entity(helper, 13, U, 0, BreakerBlockEntity.class);
			while (breaker.rating() != 400) {
				breaker.nextRating();
			}
			SafetySwitchBlockEntity sw = entity(helper, 17, U + 1, 0, SafetySwitchBlockEntity.class);
			for (int k = 0; k < 3; k++) {
				check(helper, sw.fit(WildcatterItems.CARTRIDGE_FUSES.get("fuse_class_j_30")) == null, "a 30 A class J should fit");
			}
			sw.setOn(true);
		});
		start(helper, 10, U, 0, () -> helper.runAfterDelay(60, () -> {
			String sw = read(helper, 17, U + 1, 0);
			String br = read(helper, 13, U, 0);
			log("fuse_let_through", "before | " + br + " | " + sw);
			check(helper, sw.matches(".*ON, 1[5-9] A.*"), "the switch should carry its heater: " + sw);
			check(helper, sw.contains("coordination: under the 400 A three-pole breaker"), "the switch's coordination readout should name the breaker: " + sw);
			entity(helper, 17, U, 0, MultiCableBlockEntity.class).setGrounded(true);
			helper.runAfterDelay(20, () -> {
				String blown = read(helper, 17, U + 1, 0);
				String held = read(helper, 13, U, 0);
				log("fuse_let_through", "fault | " + held + " | " + blown);
				check(helper, blown.contains("BLOWN") && blown.contains("was limited to"), "the class J fuses should clear the fault and say what they let through: " + blown);
				java.util.regex.Matcher m = java.util.regex.Pattern.compile("([\\d,]+) A prospective, was limited to ([\\d,]+) A peak").matcher(blown);
				check(helper, m.find(), "the let-through readout: " + blown);
				double prospective = Double.parseDouble(m.group(1).replace(",", ""));
				double peak = Double.parseDouble(m.group(2).replace(",", ""));
				check(helper, peak < Math.sqrt(2) * prospective, "the peak let through should be under the fault's own peak: " + blown);
				check(helper, held.contains("ON, carrying"), "the breaker upstream should hold: the fuse cleared the fault inside half a cycle: " + held);
				if (prospective <= 3200) {
					log("fuse_let_through", "note: the prospective fault, " + prospective + " A, is under the breaker's 3,200 A magnetic pickup");
				}
				helper.succeed();
			});
		}));
	}

	// ---- the ATS ----------------------------------------------------------------------------------------------------------------------
	/** Two sets: normal at (10, U, 0), standby at (10, U, 8); the ATS at (12, U+1, 9) facing east, its redstone to the standby panel. */
	private static void atsPlant(GameTestHelper helper) {
		floor(helper, 0, -6, 24, 14);
		genset(helper, 10, U, 0);
		genset(helper, 10, U, 8);
		for (int x = 11; x <= 13; x++) {
			put(helper, "cable_industrial", x, U, 0);
		}
		for (int y = U + 1; y <= U + 3; y++) {
			put(helper, "cable_industrial", 13, y, 0);
		}
		for (int z = 1; z <= 9; z++) {
			put(helper, "cable_industrial", 13, U + 3, z);
		}
		put(helper, "cable_industrial", 12, U + 3, 9);
		put(helper, "cable_industrial", 11, U, 8);
		put(helper, "cable_industrial", 11, U + 1, 8);
		put(helper, "cable_industrial", 11, U + 2, 8);
		put(helper, "cable_industrial", 11, U + 2, 9);
		put(helper, "cable_industrial", 12, U, 9);
		put(helper, "cable_industrial", 13, U, 9);
		put(helper, "three_phase_heater", 14, U, 9);
		put(helper, "three_phase_heater", 13, U, 10);
		helper.runAfterDelay(30, () -> {
			if (helper.getLevel().getBlockEntity(GensetSkid.base(abs(helper, 10, U, 8), Direction.NORTH, false)) instanceof GensetPartBlockEntity tank) {
				tank.setFuel(2000);
			}
		});
	}

	private static TransferSwitchBlockEntity ats(GameTestHelper helper) {
		return entity(helper, 12, U + 1, 9, TransferSwitchBlockEntity.class);
	}

	private static void placeAts(GameTestHelper helper) {
		place(helper, WildcatterBlocks.ATS_480, 12, U + 1, 9, Direction.EAST);
		state(helper, "minecraft:stone", 11, U, 9);
		state(helper, "minecraft:redstone_wire", 11, U + 1, 9);
	}

	static void atsTimers(GameTestHelper helper) {
		atsPlant(helper);
		start(helper, 10, U, 0, () -> {
			placeAts(helper);
			helper.runAfterDelay(60, () -> {
				String onNormal = read(helper, 12, U + 1, 9);
				log("ats_timers", "normal | " + onNormal);
				check(helper, onNormal.contains("factory timers") && onNormal.contains("on NORMAL"), "a new ATS should be on its factory timers, on normal: " + onNormal);
				gensetPanel(helper, 10, U, 0).setWantRun(false);
				waitFor(helper, () -> ats(helper).stage() == TransferSwitchBlockEntity.Stage.EMERGENCY, 800, how -> {
					String onEmergency = read(helper, 12, U + 1, 9);
					log("ats_timers", "normal lost, " + how + " | " + onEmergency + " | " + read(helper, 10, U, 8));
					check(helper, how.startsWith("yes") && onEmergency.contains("on EMERGENCY") && read(helper, 10, U, 8).contains("RUNNING"),
							"the ATS should start the standby set and transfer: " + onEmergency);
					gensetPanel(helper, 10, U, 0).setWantRun(true);
					waitFor(helper, () -> ats(helper).stage() == TransferSwitchBlockEntity.Stage.TDEN, 600, back -> {
						helper.runAfterDelay(40, () -> {
							String waiting = read(helper, 12, U + 1, 9);
							log("ats_timers", "normal back, " + back + " | " + waiting);
							check(helper, waiting.contains("on EMERGENCY") && waiting.matches(".*retransfer in 29 min.*"), "on factory timers the retransfer should wait 30 min: " + waiting);
							ats(helper).setTimers(TransferSwitchBlockEntity.Timers.SHORT);
							waitFor(helper, () -> ats(helper).stage() == TransferSwitchBlockEntity.Stage.TDEC, 800, done -> {
								String cooling = read(helper, 12, U + 1, 9);
								log("ats_timers", "short timers, " + done + " | " + cooling);
								check(helper, done.startsWith("yes") && cooling.contains("on NORMAL") && cooling.contains("short timers"), "on the short timers it should retransfer in 30 s: " + cooling);
								check(helper, !ats(helper).paralleled(), "an open transition should never parallel the sources");
								helper.succeed();
							});
						});
					});
				});
			});
		});
	}

	static void atsClosedInPhase(GameTestHelper helper) {
		atsPlant(helper);
		start(helper, 10, U, 0, () -> {
			placeAts(helper);
			helper.runAfterDelay(20, () -> {
				TransferSwitchBlockEntity ats = ats(helper);
				ats.setTimers(TransferSwitchBlockEntity.Timers.SHORT);
				ats.setTransition(TransferSwitchBlockEntity.Transition.CLOSED);
				ats.setTest(true);   // a test transfer: normal stays live
				waitFor(helper, () -> ats(helper).stage() == TransferSwitchBlockEntity.Stage.EMERGENCY, 900, how -> {
					TransferSwitchBlockEntity a = ats(helper);
					String closed = read(helper, 12, U + 1, 9);
					log("ats_closed_in_phase", "closed transition, " + how + " | " + closed + " | overlap " + a.longestOverlap());
					check(helper, how.startsWith("yes") && a.lastTransfer().contains("closed transition"), "the test transfer should be a closed transition: " + closed);
					check(helper, a.longestOverlap() > 0 && a.longestOverlap() <= 0.1 + 1e-9, "make before break for at most 100 ms: " + a.longestOverlap());
					check(helper, !a.paralleled(), "the sources should be closed together only inside the transition");
					// the retransfer by the in-phase monitor, the two sources slipping apart at 0.1 Hz
					a.setTransition(TransferSwitchBlockEntity.Transition.IN_PHASE);
					a.setSlipForTest(0.1);
					a.setTest(false);
					waitFor(helper, () -> ats(helper).stage() == TransferSwitchBlockEntity.Stage.TDEC, 1200, back -> {
						TransferSwitchBlockEntity b = ats(helper);
						String inPhase = read(helper, 12, U + 1, 9);
						log("ats_closed_in_phase", "in-phase retransfer, " + back + " | " + inPhase);
						check(helper, back.startsWith("yes") && b.lastTransfer().contains("in phase"), "the retransfer should wait for the in-phase window: " + inPhase);
						java.util.regex.Matcher m = java.util.regex.Pattern.compile("in phase \\((-?\\d+) degrees apart\\)").matcher(b.lastTransfer());
						check(helper, m.find() && Math.abs(Integer.parseInt(m.group(1))) <= 15, "it should transfer within 15 degrees: " + b.lastTransfer());
						helper.succeed();
					});
				});
			});
		});
	}
}
