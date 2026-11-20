package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.abs;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.check;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.place;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.put;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.read;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.block.RigSubstructureBlockEntity;
import dev.wildcatter.block.RigSubstructureBlockEntity.Solid;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import dev.wildcatter.well.Reservoir;
import dev.wildcatter.well.ReservoirData;
import dev.wildcatter.well.WellControl;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Phase 23, the oilfield follow-ons (docs/multiblocks/oilfield-follow-ons.md), on the server, in a batch whose world has the oilfield's
 * rules on with {@code chokeKill} (and {@code cavernStability}):
 * <ul>
 * <li>{@code casing_lot} (23a): a land rig over a forced deep oil field, weighted to 13.8 ppg with only its surface casing, circulating
 * reads LOST CIRCULATION: PARTIAL (14.1 against 14.0); SET CASING 8 blocks above the field (joints and cement on the rack, the run by
 * hook), the LOT reads 15.4 ppg and a MAASP; at 13.8 it circulates without losses; at 16.0 it loses mud (TOTAL) and does not drill; at
 * 15.3 its partial losses are cured by an LCM bag, the shoe holding 0.5 ppg more after; at 13.8 again it drills in and strikes.</li>
 * <li>{@code oil_mud} (23a): the calcium chloride unit on a 480 V set, calcite by hopper and acid and steam off test sources, makes flake
 * and a bagging station against it fills a bag; the rig's empty tanks switched to OIL MUD take diesel off a pipe, water to 80/20, CaCl2
 * and the OBM package by hook: the readout reads OIL MUD 80/20 with ES ok; the rig drills through placed rock salt with no WASHOUT and no
 * LOW GEL, on into a forced deep field where the kick in light oil mud shows late: the alarm long after the influx began.</li>
 * </ul>
 */
final class OilfieldFollowOnScenes {
	private OilfieldFollowOnScenes() {
	}

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> scenes = new LinkedHashMap<>();
		scenes.put("casing_lot", OilfieldFollowOnScenes::casingLot);
		scenes.put("oil_mud", OilfieldFollowOnScenes::oilMud);
		scenes.put("choke_kill", OilfieldFollowOnScenes::chokeKill);
		scenes.put("gas_storage", OilfieldFollowOnScenes::gasStorage);
		scenes.put("frasch", OilfieldFollowOnScenes::frasch);
		return scenes;
	}

	/** The rules the batch turns on: a new world's oilfield. */
	static final java.util.List<String> RULES = java.util.List.of("saltDomeGrid192", "mudGel", "wellControl", "chokeKill", "cavernStability");

	private static final int FLOOR_Y = 40;

	// ---- helpers --------------------------------------------------------------------------------------------------------------------
	static void floor(GameTestHelper helper, int x1, int z1, int x2, int z2, int y) {
		BlockPos a = abs(helper, x1, y, z1);
		BlockPos b = abs(helper, x2, y, z2);
		for (int cx = Math.min(a.getX(), b.getX()) >> 4; cx <= Math.max(a.getX(), b.getX()) >> 4; cx++) {
			for (int cz = Math.min(a.getZ(), b.getZ()) >> 4; cz <= Math.max(a.getZ(), b.getZ()) >> 4; cz++) {
				helper.getLevel().setChunkForced(cx, cz, true);
			}
		}
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				helper.getLevel().setBlock(abs(helper, x, y, z), Blocks.STONE.defaultBlockState(), 3);
			}
		}
	}

	static void clear(GameTestHelper helper, int x1, int z1, int x2, int z2, int y1, int y2) {
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				for (int y = y1; y <= y2; y++) {
					at.set(abs(helper, x, y, z));
					if (!helper.getLevel().getBlockState(at).isAir()) {
						helper.getLevel().removeBlockEntity(at);
						helper.getLevel().setBlock(at, Blocks.AIR.defaultBlockState(), 2 | 16);
					}
				}
			}
		}
	}

	static TestGasSource.SourceEntity source(GameTestHelper helper, int x, int y, int z, Product product, double rate) {
		helper.getLevel().setBlock(abs(helper, x, y, z), BuiltInRegistries.BLOCK.getValue(WildcatterNeoForgeGameTests.id("gas_source")).defaultBlockState(), 3);
		TestGasSource.SourceEntity s = (TestGasSource.SourceEntity) helper.getLevel().getBlockEntity(abs(helper, x, y, z));
		s.product = product;
		s.rate = rate;
		return s;
	}

	/** A seed that puts a deep oil field under the hole, and its test region; null region if none. */
	static ReservoirData.TestRegion deepField(GameTestHelper helper, BlockPos hole, Reservoir[] field) {
		BlockPos o = abs(helper, 0, 0, 0);
		long seed = -1;
		for (long s = 1; s < 3_000_000 && seed < 0; s++) {
			Reservoir f = Reservoir.at(s, hole.getX(), hole.getZ());
			if (f != null && f.deep() && f.kind() == Reservoir.Kind.OIL) {
				seed = s;
			}
		}
		check(helper, seed >= 0, "no seed puts a deep oil field under the rig at " + hole + " (origin " + o + ")");
		ReservoirData.TestRegion region = new ReservoirData.TestRegion(o.getX() - 20, o.getZ() - 30, o.getX() + 70, o.getZ() + 30, seed);
		ReservoirData.TEST_REGIONS.add(region);
		field[0] = Reservoir.at(seed, hole.getX(), hole.getZ());
		return region;
	}

	/** Weights the rig's water mud to at least {@code ppg} by hook: fresh water, 25 lb/bbl of bentonite, then barite. */
	static void weigh(RigSubstructureBlockEntity rig, double ppg) {
		rig.testWater(0);
		rig.testWater(40_000);
		rig.testMix(Solid.BENTONITE, 71.3 * 40);
		while (rig.mudPpg() < ppg) {
			rig.testMix(Solid.BARITE, 200);
		}
	}

	/**
	 * The choke manifold (x 30-31, z -9..-6) piped from the BOP stack's north side down the substructure's BOP space; with {@code all}, the
	 * mud-gas separator against it (x 32, z -8), its vent to a flare stack (x 34), and the remote choke panel on the rig floor.
	 */
	static void chokeSystem(GameTestHelper helper, int y, boolean all) {
		for (int z = -5; z <= -1; z++) {
			put(helper, "gas_line", 30, y + 1, z);
		}
		place(helper, WildcatterBlocks.CHOKE_MANIFOLD, 30, y, -9, Direction.NORTH);
		if (all) {
			place(helper, WildcatterBlocks.MUD_GAS_SEPARATOR, 32, y, -8, Direction.NORTH);
			put(helper, "gas_line", 32, y + 6, -8);
			put(helper, "gas_line", 33, y + 6, -8);
			place(helper, WildcatterBlocks.FLARE_STACK, 34, y, -8, Direction.NORTH);
			place(helper, WildcatterBlocks.REMOTE_CHOKE_PANEL, 33, y + 6, -3, Direction.NORTH);
		}
	}

	/** The remote choke panel on the rig floor ({@link #chokeSystem}). */
	static dev.wildcatter.block.ChokePanelBlockEntity panel(GameTestHelper helper) {
		return GridSceneKit.entity(helper, 33, FLOOR_Y - abs(helper, 0, 0, 0).getY() + 1 + 6, -3, dev.wildcatter.block.ChokePanelBlockEntity.class);
	}

	private static void finish(GameTestHelper helper, String scene, String text, int y, ReservoirData.TestRegion region) {
		log(scene, text);
		if (region != null) {
			ReservoirData.TEST_REGIONS.remove(region);
		}
		clear(helper, -16, -16, 52, 18, y - 1, y + 50);
		helper.succeed();
	}

	// ---- casing_lot (23a) ----------------------------------------------------------------------------------------------------------
	static void casingLot(GameTestHelper helper) {
		String scene = "casing_lot";
		BlockPos o = abs(helper, 0, 0, 0);
		int y = FLOOR_Y - o.getY() + 1;
		BlockPos hole = abs(helper, 30, y, 0);
		Reservoir[] field = new Reservoir[1];
		ReservoirData.TestRegion region = deepField(helper, hole, field);
		floor(helper, 0, -14, 52, 18, y - 1);
		RigSubstructureBlockEntity rig = OilfieldScenes.landRig(helper, y);
		place(helper, WildcatterBlocks.BOP_ACCUMULATOR, 18, y, -12, Direction.NORTH);
		place(helper, WildcatterBlocks.BOP_STACK, 30, y + 1, 0, Direction.NORTH);
		chokeSystem(helper, y, false);   // chokeKill: no deep field without a choke manifold
		int above = 8;
		int[] step = {0};
		int[] mark = {0};
		helper.runAfterDelay(40, () -> {
			check(helper, rig.formed() && rig.installTopDrive(), "the rig should be built: " + read(helper, 26, y, -4));
			weigh(rig, 13.8);
			rig.testDepth(hole.getY() - field[0].topY() - above);
			rig.testHoldBit(true);   // circulating off bottom: the pumps' ECD on the open hole, no new hole
			rig.toggle();
		});
		GridSceneKit.start(helper, 10, y, 7, () -> {});
		for (int t = 400; t <= 5800; t += 20) {
			final int at = t;
			helper.runAfterDelay(t, () -> {
				String now = read(helper, 26, y, -4);
				switch (step[0]) {
					case 0 -> {   // only surface casing: 13.8 + 0.3 = 14.1 against 14.0
						if (now.contains("LOST CIRCULATION")) {
							log(scene, "surface casing only | " + now);
							check(helper, now.contains("LOST CIRCULATION: PARTIAL") && rig.lostMud() > 0 && Math.abs(rig.weakFg() - 14.0) < 1e-6,
									"kill-weight mud with the drilling ECD should break down the open hole under surface casing: " + now);
							rig.toggle();
							rig.rackUp(new ItemStack(WildcatterItems.CASING_JOINT, rig.jointsNeeded()));
							rig.rackUp(new ItemStack(WildcatterItems.OILWELL_CEMENT, Tuning.CASING_CEMENT_BAGS));
							step[0] = 1;
						}
					}
					case 1 -> {
						String set = rig.consoleCasing(helper.getLevel()).getString();
						log(scene, "set casing | " + set);
						check(helper, rig.phase() == RigSubstructureBlockEntity.Phase.CASING && set.contains("SET CASING"), "SET CASING should start the run: " + set);
						check(helper, rig.joints() == 0 && rig.cementBags() == 0, "the run should use the racked joints and cement: " + now);
						rig.testCasingDone();
						step[0] = 2;
					}
					case 2 -> {
						if (rig.phase() == RigSubstructureBlockEntity.Phase.IDLE) {
							log(scene, "cased | " + now);
							check(helper, rig.shoeDepth() == rig.depthNow() && now.contains("FG NOT TESTED"), "the shoe at the bit, FG NOT TESTED: " + now);
							String early = rig.consoleCasing(helper.getLevel()).getString();
							check(helper, early.contains("drill 3 m more"), "the LOT wants 3 m of new hole first: " + early);
							rig.testDepth(rig.shoeDepth() + Tuning.LOT_NEW_HOLE_M);
							String lot = rig.consoleCasing(helper.getLevel()).getString();
							log(scene, "LOT | " + lot);
							double maasp = WellControl.maasp(15.4, rig.mudPpg(), rig.shoeDepth() * Tuning.FEET_PER_M);
							check(helper, rig.lotDone() && lot.contains("holds 15.4 ppg") && lot.contains(String.format("MAASP %,.0f psi", maasp)),
									"the LOT should read 15.4 ppg and the MAASP for the mud: " + lot);
							rig.toggle();
							mark[0] = at;
							step[0] = 3;
						}
					}
					case 3 -> {   // 13.8 under the 15.4 shoe: no losses
						if (at >= mark[0] + 60) {
							log(scene, "13.8 under the shoe | " + now);
							check(helper, rig.losses() == WellControl.Losses.NONE && !now.contains("LOST") && now.contains("drilling at"), "13.8 ppg should circulate under the 15.4 shoe: " + now);
							rig.toggle();
							weigh(rig, 16.0);
							rig.toggle();
							mark[0] = at;
							step[0] = 4;
						}
					}
					case 4 -> {
						if (at >= mark[0] + 60) {
							log(scene, "16.0 | " + now);
							check(helper, now.contains("LOST CIRCULATION: TOTAL") && now.contains("no returns"), "16.0 ppg (16.3 circulating) should lose all returns at the 15.4 shoe: " + now);
							rig.toggle();
							weigh(rig, 15.3);
							rig.toggle();
							mark[0] = at;
							step[0] = 5;
						}
					}
					case 5 -> {
						if (at >= mark[0] + 60) {
							log(scene, "15.3 | " + now);
							check(helper, now.contains("LOST CIRCULATION: PARTIAL"), "15.3 ppg (15.6 circulating) should take partial losses: " + now);
							rig.testMix(Solid.LCM, Tuning.BULK_BAG_KG);
							mark[0] = at;
							step[0] = 6;
						}
					}
					case 6 -> {
						if (rig.losses() == WellControl.Losses.NONE && at > mark[0] + 40) {
							log(scene, "LCM | " + now);
							check(helper, Math.abs(rig.weakFg() - 15.9) < 1e-6 && at - mark[0] <= Tuning.LCM_CURE_TICKS + 100, "an LCM bag should cure the losses within a minute and leave the shoe at 15.9: " + now);
							rig.toggle();
							weigh(rig, 13.8);
							rig.testHoldBit(false);
							rig.toggle();
							step[0] = 7;
						}
					}
					case 7 -> {
						if (now.contains("WELL COMPLETE")) {
							check(helper, helper.getLevel().getBlockState(hole).is(WildcatterBlocks.OIL_WELLHEAD), "cased, at 13.8 it should drill in and strike: " + now);
							step[0] = 99;
							finish(helper, scene, "ok: cased, tested, lost and cured, struck | " + now, y, region);
						}
					}
					default -> {
					}
				}
			});
		}
	}

	// ---- oil_mud (23a) -------------------------------------------------------------------------------------------------------------
	static void oilMud(GameTestHelper helper) {
		String scene = "oil_mud";
		BlockPos o = abs(helper, 0, 0, 0);
		int y = FLOOR_Y - o.getY() + 1;
		BlockPos hole = abs(helper, 30, y, 0);
		Reservoir[] field = new Reservoir[1];
		ReservoirData.TestRegion region = deepField(helper, hole, field);
		floor(helper, -16, -16, 52, 18, y - 1);
		// the calcium chloride unit on its own 480 V set, its box west of the main cell; calcite on top, acid north, steam east
		place(helper, WildcatterBlocks.CALCIUM_CHLORIDE_UNIT, 2, y, -12, Direction.NORTH);   // x 2-3, z -12..-10
		GridSceneKit.genset(helper, -8, y, -12);
		for (int x = -7; x <= 1; x++) {
			put(helper, "cable_industrial", x, y, -12);
		}
		for (int z = -11; z <= -9; z++) {
			put(helper, "cable_industrial", 1, y, z);   // on to the bagging station's box
		}
		source(helper, 2, y, -14, Product.HYDROCHLORIC_ACID, 5.0);
		put(helper, "gas_line", 2, y, -13);
		source(helper, 5, y, -12, Product.PROCESS_STEAM, 5.0);
		put(helper, "gas_line", 4, y, -12);
		helper.getLevel().setBlock(abs(helper, 2, y + 4, -12), Blocks.HOPPER.defaultBlockState(), 3);
		((net.minecraft.world.Container) helper.getLevel().getBlockEntity(abs(helper, 2, y + 4, -12))).setItem(0, new ItemStack(net.minecraft.world.item.Items.CALCITE, 8));
		place(helper, WildcatterBlocks.BAGGING_STATION, 2, y, -9, Direction.NORTH);         // against the unit's back
		helper.getLevel().setBlock(abs(helper, 2, y + 4, -9), Blocks.HOPPER.defaultBlockState(), 3);
		((net.minecraft.world.Container) helper.getLevel().getBlockEntity(abs(helper, 2, y + 4, -9))).setItem(0, new ItemStack(WildcatterItems.EMPTY_BULK_BAG, 2));
		dev.wildcatter.block.CalciumChlorideUnitBlockEntity unit = GridSceneKit.entity(helper, 2, y, -12, dev.wildcatter.block.CalciumChlorideUnitBlockEntity.class);
		// the rig, a BOP, rock salt three to five blocks down, diesel off a pipe at the tanks' north side
		RigSubstructureBlockEntity rig = OilfieldScenes.landRig(helper, y);
		place(helper, WildcatterBlocks.BOP_ACCUMULATOR, 18, y, -12, Direction.NORTH);
		place(helper, WildcatterBlocks.BOP_STACK, 30, y + 1, 0, Direction.NORTH);
		chokeSystem(helper, y, false);
		for (int k = 3; k <= 5; k++) {
			helper.getLevel().setBlock(hole.below(k), OreBlocks.block(Ore.HALITE).defaultBlockState(), 3);
		}
		TestGasSource.SourceEntity diesel = source(helper, 37, y, -7, Product.DIESEL, 50.0);
		put(helper, "gas_line", 37, y, -6);
		int[] step = {0};
		int[] mark = {0};
		int[] kickAt = {-1};
		GridSceneKit.start(helper, -8, y, -12, () -> {});
		GridSceneKit.start(helper, 10, y, 7, () -> {});
		helper.runAfterDelay(40, () -> {
			check(helper, rig.formed() && rig.installTopDrive(), "the rig should be built: " + read(helper, 26, y, -4));
			rig.testWater(0);
			String sw = rig.tanksSwitch(helper.getLevel().getGameTime()).getString();
			check(helper, rig.oilMud() && sw.contains("OIL MUD"), "the empty tanks should switch to OIL MUD: " + sw);
		});
		for (int t = 400; t <= 5800; t += 20) {
			final int at = t;
			helper.runAfterDelay(t, () -> {
				String now = read(helper, 26, y, -4);
				switch (step[0]) {
					case 0 -> {   // the unit making flake; diesel coming in off the pipe
						if (unit.made() > 1 && rig.diesel() > 100) {
							log(scene, "unit | " + read(helper, 2, y, -12) + " | rig " + now);
							check(helper, diesel.taken > 0, "the tanks should take diesel off the pipe: " + diesel.taken);
							unit.testFlake(Tuning.BULK_BAG_KG);
							// the rest by hook: diesel to 36 m3, water to 80/20, CaCl2 to 28 %, the package to 20 lb/bbl
							rig.testDiesel(36_000 - rig.diesel());
							rig.addWater((int) (36_000 * 0.25 * 0.885));   // the flake brings the rest of the water phase
							while (rig.cacl2Strength() < Tuning.OIL_MUD_CACL2) {
								rig.testMix(Solid.CACL2, 100);
							}
							rig.testMix(Solid.OBM, 0.057 * rig.mudVolume());
							mark[0] = at;
							step[0] = 1;
						}
					}
					case 1 -> {
						if (at >= mark[0] + 120) {
							var station = (net.minecraft.world.Container) GridSceneKit.entity(helper, 2, y, -9, dev.wildcatter.block.BaggingStationBlockEntity.class);
							boolean bag = false;
							for (int i = 0; i < station.getContainerSize(); i++) {
								bag |= station.getItem(i).is(WildcatterItems.CACL2_BAG);
							}
							check(helper, bag, "the bagging station against the unit should fill a bag of CaCl2 flake: " + read(helper, 2, y, -9));
							log(scene, "oil mud | " + now);
							check(helper, now.contains("OIL MUD 80/20") && now.contains("ES ok") && rig.oilMudWorking(), "the readout should read OIL MUD 80/20 with ES ok: " + now);
							check(helper, Math.abs(rig.cacl2Strength() - 0.28) < 0.01, "28 % CaCl2 in the water phase: " + rig.cacl2Strength());
							check(helper, rig.mudPpg() > 7.5 && rig.mudPpg() < 8.2, "light oil mud about 7.7-7.9 ppg before barite: " + rig.mudPpg());
							rig.toggle();
							step[0] = 2;
						}
					}
					case 2 -> {   // through the salt with no washout, no LOW GEL
						check(helper, !now.contains("WASHOUT") && !now.contains("LOW GEL"), "oil mud should not wash out the salt nor read LOW GEL: " + now);
						if (rig.depthNow() >= 6) {
							check(helper, helper.getLevel().getBlockState(hole.below(4)).is(WildcatterBlocks.CASING), "it should case through the salt: " + now);
							log(scene, "through the salt | " + now);
							step[0] = 3;
						}
					}
					case 3 -> {   // on into the deep field at about 7.8 ppg: the kick, hidden in the oil
						if (rig.phase() == RigSubstructureBlockEntity.Phase.KICK && kickAt[0] < 0) {
							kickAt[0] = at;
							log(scene, "the influx begins | " + now);
						}
						if (kickAt[0] >= 0) {
							if (!rig.readPoint(5)) {
								check(helper, !now.contains("KICK"), "the kick in oil mud should not alarm at once: " + now);
							} else {
								log(scene, "alarm after " + (at - kickAt[0]) + " ticks | " + now);
								check(helper, at - kickAt[0] >= 200, "the kick in oil mud should alarm late (water mud alarms at once): " + (at - kickAt[0]) + " ticks");
								check(helper, rig.dieselLost() > 0 && rig.cuttings() > 0, "the diesel lost with the mud and the cuttings should be counted: " + now);
								step[0] = 99;
								finish(helper, scene, "ok: flake bagged, OIL MUD 80/20 ES ok, through salt, the kick late | " + now, y, region);
							}
						}
					}
					default -> {
					}
				}
			});
		}
	}

	// ---- choke_kill (23b) ----------------------------------------------------------------------------------------------------------
	/**
	 * A land rig cased 8 blocks above a forced deep field, its choke manifold left off: NO CHOKE stops it short of the field. With the
	 * manifold, separator, flare and panel set it drills in at 12.8 ppg and kicks; shut in, the kill sheet's SIDPP, kill weight, ICP and
	 * FCP match the hand figures. Weighted to the kill weight, the pump brought up on the panel and the choke held on the schedule by a
	 * scripted operator on the panel's PLC points (its CHOKE POSITION command, reading DRILL PIPE PRESSURE and PUMP RATE, as a PLC's PID
	 * would): held too far closed the shoe takes LOST CIRCULATION; held too far open the well takes a SECOND KICK and is shut in again;
	 * then held on schedule it kills the well in under 12 minutes (the model run 3 steps a tick, by hook): kill mud round, the pump
	 * stopped and the choke open, the flow check, WELL DEAD, and the rig strikes.
	 */
	static void chokeKill(GameTestHelper helper) {
		String scene = "choke_kill";
		BlockPos o = abs(helper, 0, 0, 0);
		int y = FLOOR_Y - o.getY() + 1;
		BlockPos hole = abs(helper, 30, y, 0);
		Reservoir[] field = new Reservoir[1];
		ReservoirData.TestRegion region = deepField(helper, hole, field);
		floor(helper, -16, -16, 52, 18, y - 1);
		RigSubstructureBlockEntity rig = OilfieldScenes.landRig(helper, y);
		place(helper, WildcatterBlocks.BOP_ACCUMULATOR, 18, y, -12, Direction.NORTH);
		place(helper, WildcatterBlocks.BOP_STACK, 30, y + 1, 0, Direction.NORTH);
		int[] step = {0};
		int[] mark = {0};
		double[] bias = {0};
		double[] kill = {0, 0};   // the tick the kill started, restarts after another second kick
		GridSceneKit.start(helper, 10, y, 7, () -> {});
		helper.runAfterDelay(40, () -> {
			check(helper, rig.formed() && rig.installTopDrive(), "the rig should be built: " + read(helper, 26, y, -4));
			weigh(rig, 12.8);
			rig.testDepth(hole.getY() - field[0].topY() - 8);
			rig.rackUp(new ItemStack(WildcatterItems.CASING_JOINT, rig.jointsNeeded()));
			rig.rackUp(new ItemStack(WildcatterItems.OILWELL_CEMENT, Tuning.CASING_CEMENT_BAGS));
			rig.consoleCasing(helper.getLevel());
			rig.testCasingDone();
		});
		for (int t = 400; t <= 11900; t++) {
			final int at = t;
			helper.runAfterDelay(t, () -> {
				if (step[0] >= 3 && step[0] < 99) {
					operate(rig, bias[0], step[0] == 3);
				}
				if (at % 10 != 0) {
					return;
				}
				String now = read(helper, 26, y, -4);
				switch (step[0]) {
					case 0 -> {
						if (rig.phase() == RigSubstructureBlockEntity.Phase.IDLE && rig.shoeDepth() > 0) {
							check(helper, Math.abs(rig.weakFg() - 15.4) < 1e-6, "the shoe 8 blocks above the field should hold 15.4: " + now);
							rig.toggle();
							step[0] = 1;
						}
					}
					case 1 -> {
						if (now.contains("NO CHOKE")) {
							log(scene, "no choke | " + now);
							int d = rig.depthNow();
							check(helper, hole.getY() - (d + 1) <= field[0].topY(), "NO CHOKE should stop it just short of the deep field: " + now);
							chokeSystem(helper, y, true);
							panel(helper).testHydraulics();
							step[0] = 2;
						}
					}
					case 2 -> {
						if (rig.phase() == RigSubstructureBlockEntity.Phase.KICK) {
							String shut = rig.shutIn(helper.getLevel()).getString();
							log(scene, "shut in | " + shut);
							WellControl.KillSheet sheet = rig.killSheet();
							check(helper, rig.phase() == RigSubstructureBlockEntity.Phase.SHUT_IN && rig.well() != null && sheet != null, "shut in, the well should be in the kill model: " + shut);
							double tvd = rig.depthNow() * Tuning.FEET_PER_M;
							double sidpp = 0.052 * (Tuning.DEEP_PORE_PPG - 12.8) * tvd;
							log(scene, String.format("kill sheet: SIDPP %.1f (hand %.1f), SICP %.1f, KMW %.2f, ICP %.1f, FCP %.1f, strokes to bit %d, circulation %d, MAASP %.1f",
									sheet.sidpp(), sidpp, sheet.sicp(), sheet.kmw(), sheet.icp(), sheet.fcp(), sheet.strokesToBit(), sheet.strokesCirculation(), rig.maasp()));
							check(helper, Math.abs(sheet.sidpp() - sidpp) < 2.0, "SIDPP should be 0.052 x (13.5 - 12.8) x TVD: " + sheet.sidpp() + " against " + sidpp);
							check(helper, Math.abs(sheet.kmwExact() - Tuning.DEEP_PORE_PPG) < 0.1, "the kill weight should be the pore pressure's 13.5: " + sheet.kmwExact());
							check(helper, Math.abs(sheet.icp() - (sheet.sidpp() + Tuning.SCR_PSI)) < 1e-6 && Math.abs(sheet.fcp() - Tuning.SCR_PSI * sheet.kmw() / 12.8) < 0.5,
									"ICP = SIDPP + SCR pressure, FCP = SCR x KMW / OMW: " + sheet.icp() + ", " + sheet.fcp());
							weigh(rig, sheet.kmw());
							rig.testKillSteps(3);
							panel(helper).pump(true);
							mark[0] = at;
							step[0] = 3;
						}
					}
					case 3 -> {   // the pump coming up, the choke opened to hold SICP
						if (rig.well().spm() >= Tuning.SCR_SPM - 1e-6) {
							mark[0] = at;
							step[0] = 4;
						}
					}
					case 4 -> {   // on schedule a while, then too far closed
						if (at >= mark[0] + 60) {
							var well = rig.well();
							double margin = (well.fgPpg - well.shoeEmw()) * 0.052 * well.shoeM * Tuning.FEET_PER_M;
							bias[0] = margin + 2;
							log(scene, String.format("on schedule: DPP %.1f against %.1f | too far closed by %.1f psi", well.dppGauge(), rig.schedulePsi(), bias[0]));
							check(helper, Math.abs(well.dppGauge() - rig.schedulePsi()) < 8, "the operator should hold the schedule: " + now);
							mark[0] = at;
							step[0] = 5;
						}
					}
					case 5 -> {
						if (rig.losses() != WellControl.Losses.NONE) {
							log(scene, "too far closed | " + now);
							check(helper, now.contains("LOST CIRCULATION"), "held too far closed the shoe should take losses: " + now);
							bias[0] = -15;
							mark[0] = at;
							step[0] = 6;
						} else {
							check(helper, rig.phase() == RigSubstructureBlockEntity.Phase.SHUT_IN && at < mark[0] + 400, "held too far closed it should lose mud at the shoe: " + now);
						}
					}
					case 6 -> {   // too far open: a second kick
						if (rig.well() != null && rig.well().strokes() < 1 && !rig.panel().pumpOn() && at > mark[0] + 10) {
							log(scene, "too far open | " + now);
							bias[0] = 0;
							panel(helper).pump(true);
							kill[0] = at;
							step[0] = 7;
						} else {
							check(helper, at < mark[0] + 1600, "held too far open the well should take a second kick: " + now);
						}
					}
					case 7 -> {   // the kill on schedule, to kill mud round
						var well = rig.well();
						var sheet = rig.killSheet();
						if (!rig.panel().pumpOn() && well.strokes() < 1) {   // shut in again after another second kick: start again
							check(helper, ++kill[1] <= 3, "the kill should not keep taking second kicks: " + now);
							log(scene, "shut in again, starting again | " + now);
							panel(helper).pump(true);
						}
						if (at % 400 == 0) {
							log(scene, String.format("killing: gas %.1f L, top %.0f m, lightest %.2f, annulus %.2f | %s", well.gasL(), well.gasTopM(), well.lightestPpg(), well.annulusPpg(), now));
						}
						if (well.strokes() > sheet.strokesCirculation() + 5 && !well.gasInHole() && well.lightestPpg() + 0.05 >= rig.killWeight() - 1e-6) {
							double minutes = well.strokes() / Tuning.SCR_SPM;   // minutes of pumping at SCR since the last shut-in
							log(scene, String.format("kill mud round after %.1f minutes | %s", minutes, now));
							check(helper, minutes < 12, "the kill should take under 12 minutes: " + minutes);
							check(helper, rig.losses() == WellControl.Losses.NONE, "on schedule no losses: " + now);
							panel(helper).pump(false);
							step[0] = 8;
						}
					}
					case 8 -> {
						if (at % 200 == 0) {
							log(scene, "flow check | " + now);
						}
						if (now.contains("WELL COMPLETE")) {
							check(helper, helper.getLevel().getBlockState(hole).is(WildcatterBlocks.OIL_WELLHEAD), "WELL DEAD, it should strike: " + now);
							step[0] = 99;
							finish(helper, scene, "ok: NO CHOKE, kill sheet, losses closed, second kick open, killed on schedule, dead and struck | " + now, y, region);
						}
					}
					default -> {
					}
				}
			});
		}
	}

	/** The scripted choke operator on the panel's PLC points: open the choke as the pump comes up holding SICP, then hold the schedule (+ bias). */
	private static void operate(RigSubstructureBlockEntity rig, double bias, boolean pumpUp) {
		var panel = rig.panel();
		var well = rig.well();
		if (panel == null || well == null) {
			return;
		}
		double spm = (panel.readMa(5) - 4) / 16 * 60;
		double choke = (panel.readMa(1) - 4) / 16;
		double next;
		if (!panel.pumpOn()) {
			next = rig.killNote().startsWith("kill mud all round") || rig.killNote().startsWith("FLOW CHECK") ? 1.0 : 0.0;   // the pump stopped: closed, or open for the flow check
		} else if (pumpUp || spm < Tuning.SCR_SPM - 1e-6) {
			double r = Math.min(1, (spm + Tuning.PUMP_SPM_PER_S * 3 / 20) / Tuning.SCR_SPM);
			next = r * Math.sqrt(Tuning.CHOKE_K_PSI / Math.max(1, rig.killSheet().sicp()));
		} else {
			double dpp = (panel.readMa(2) - 4) / 16 * 1000;
			double err = dpp - (rig.schedulePsi() + bias);
			next = choke * Math.max(1 / 1.03, Math.min(1.03, Math.exp(0.02 * err * 0.15)));   // a PID's output rate limit
		}
		panel.commandMa(0, 4 + 16 * Math.max(panel.pumpOn() ? 0.001 : 0, Math.min(1, next)), rig.getLevel().getGameTime());
	}

	// ---- gas_storage (23c) ---------------------------------------------------------------------------------------------------------
	/**
	 * A 9 x 9 column of rock salt (y 1-10) under a calcite cap, a hole cased into it and a brine wellhead whose brine goes by pipe to a
	 * disposal wellhead: the injection pump on its 480 V set runs at its full 10 L/s; leached by hook (with cavernStability off) the cavern
	 * stops at its walls, 5 x 5 by 7 high plus the hole's bottom (176 m3), its record reading 2 m of salt to the flank and 3 over the roof.
	 * The brine wellhead swapped for a storage wellhead on INJECT, the storage compressor (driven by hook: its 1 MW motor wants more than
	 * the scene's set) on pipeline gas debrines it to the disposal well, its cushion then by hook: CAVERN DRY, and working gas raises the
	 * pressure. On WITHDRAW with no dehydrator it reads WET GAS; with the glycol dehydrator (on its own 480 V set, fuel gas to its reboiler)
	 * the gas comes out dry at its outlet. A 3 x 3 salt column beside it, cavernStability on, leaches past its flank wall and breaches.
	 */
	static void gasStorage(GameTestHelper helper) {
		String scene = "gas_storage";
		int u = 2;
		floor(helper, -18, -16, 20, 14, 1);
		for (int x = -4; x <= 4; x++) {
			for (int z = -4; z <= 4; z++) {
				for (int y = 2; y <= 11; y++) {
					helper.getLevel().setBlock(abs(helper, x, y, z), OreBlocks.block(Ore.HALITE).defaultBlockState(), 3);
				}
				for (int y = 12; y <= 13; y++) {
					helper.getLevel().setBlock(abs(helper, x, y, z), Blocks.CALCITE.defaultBlockState(), 3);
				}
			}
		}
		for (int y = 10; y <= 14; y++) {
			put(helper, "casing", 0, y, 0);
		}
		BlockPos headPos = abs(helper, 0, 15, 0);
		helper.getLevel().setBlock(headPos, WildcatterBlocks.BRINE_WELLHEAD.defaultBlockState().setValue(dev.wildcatter.block.BrineWellheadBlock.FACING, Direction.EAST), 3);
		for (int x = 1; x <= 6; x++) {
			put(helper, "gas_line", x, 15, 0);
		}
		helper.getLevel().setBlock(abs(helper, 7, 15, 0), WildcatterBlocks.DISPOSAL_WELLHEAD.defaultBlockState(), 3);
		// fresh water: the injection pump's discharge to the wellhead's west, as the brine_well scene
		place(helper, WildcatterBlocks.INJECTION_PUMP, -6, u, -4, Direction.NORTH);
		for (int y = u + 1; y <= 15; y++) {
			put(helper, "gas_line", -6, y, -1);
		}
		put(helper, "gas_line", -6, 15, 0);
		for (int x = -5; x <= -1; x++) {
			put(helper, "gas_line", x, 15, 0);
		}
		GridSceneKit.genset(helper, -10, u, -7);
		for (int x = -9; x <= -6; x++) {
			put(helper, "cable_industrial", x, u, -7);
		}
		put(helper, "cable_industrial", -6, u, -6);
		put(helper, "cable_industrial", -6, u, -5);
		dev.wildcatter.block.InjectionPumpBlockEntity pump = GridSceneKit.entity(helper, -6, u, -4, dev.wildcatter.block.InjectionPumpBlockEntity.class);
		dev.wildcatter.block.BrineWellheadBlockEntity[] brine = {(dev.wildcatter.block.BrineWellheadBlockEntity) helper.getLevel().getBlockEntity(headPos)};
		// the second, thin column for the breach: 3 x 3, y 2-11, at x 14
		for (int x = 13; x <= 15; x++) {
			for (int z = -1; z <= 1; z++) {
				for (int y = 2; y <= 11; y++) {
					helper.getLevel().setBlock(abs(helper, x, y, z), OreBlocks.block(Ore.HALITE).defaultBlockState(), 3);
				}
			}
		}
		for (int y = 10; y <= 14; y++) {
			put(helper, "casing", 14, y, 0);
		}
		BlockPos thinPos = abs(helper, 14, 15, 0);
		helper.getLevel().setBlock(thinPos, WildcatterBlocks.BRINE_WELLHEAD.defaultBlockState().setValue(dev.wildcatter.block.BrineWellheadBlock.FACING, Direction.NORTH), 3);
		int[] step = {0};
		int[] mark = {0};
		GridSceneKit.start(helper, -10, u, -7, () -> {});
		for (int t = 300; t <= 11900; t += 20) {
			final int at = t;
			helper.runAfterDelay(t, () -> {
				switch (step[0]) {
					case 0 -> {   // the pump on fresh water, the brine to disposal at the full rate
						pump.addWater(2_000);
						String p = pump.inspect().getString();
						java.util.regex.Matcher lps = java.util.regex.Pattern.compile("injecting ([0-9.]+) L/s").matcher(p);
						if (at % 400 == 0) {
							log(scene, "waiting | " + p + " | head " + brine[0].inspect().getString());
						}
						if (lps.find() && Double.parseDouble(lps.group(1)) > 9.0) {
							log(scene, "pump | " + p + " | head " + brine[0].inspect().getString());
							check(helper, brine[0].toDisposal(), "with its brine to disposal the well should leach at the pump's full rate: " + brine[0].inspect().getString());
							boolean[] rules = dev.wildcatter.Rules.snapshot();
							dev.wildcatter.Rules.set(dev.wildcatter.Rules.Rule.CAVERN_STABILITY, false);
							for (int k = 0; k < 400 && !brine[0].inspect().getString().contains("CAVERN AT LIMIT"); k++) {
								brine[0].testSalt(Ore.HALITE.kgPerBlock());
								brine[0].inject(helper.getLevel(), 1.0);
							}
							brine[0].inject(helper.getLevel(), 0.01);
							dev.wildcatter.Rules.restore(rules);
							String h = brine[0].inspect().getString();
							log(scene, "leached | " + h);
							check(helper, h.contains("CAVERN AT LIMIT") && brine[0].blocks() == 176, "the cavern should stop at its walls, 176 m3: " + brine[0].blocks() + " | " + h);
							check(helper, helper.getLevel().getBlockState(abs(helper, 2, 2, 2)).is(Blocks.WATER) && helper.getLevel().getBlockState(abs(helper, 3, 2, 3)).is(OreBlocks.block(Ore.HALITE)),
									"the cavern should keep 2 blocks of salt to the flank");
							check(helper, helper.getLevel().getBlockState(abs(helper, 1, 8, 1)).is(Blocks.WATER) && helper.getLevel().getBlockState(abs(helper, 1, 9, 1)).is(OreBlocks.block(Ore.HALITE)),
									"the cavern should keep 3 blocks of salt under the caprock");
							// the storage plant on the hole
							helper.getLevel().setBlock(headPos, WildcatterBlocks.STORAGE_WELLHEAD.defaultBlockState().setValue(dev.wildcatter.block.StorageWellheadBlock.FACING, Direction.EAST), 3);
							for (int y = u; y <= 15; y++) {
								put(helper, "gas_line", 0, y, -6);   // the compressor's discharge up outside the salt and over to the wellhead's north side
							}
							for (int z = -5; z <= -1; z++) {
								put(helper, "gas_line", 0, 15, z);
							}
							place(helper, WildcatterBlocks.STORAGE_COMPRESSOR, -1, u, -12, Direction.NORTH);   // x -1..1, z -12..-7: out of its back at z -6
							source(helper, 0, u, -14, Product.PIPELINE_GAS, 10.0);
							put(helper, "gas_line", 0, u, -13);
							mark[0] = at;
							step[0] = 1;
						}
					}
					case 1 -> {
						var head = GridSceneKit.entity(helper, 0, 15, 0, dev.wildcatter.block.StorageWellheadBlockEntity.class);
						var comp = GridSceneKit.entity(helper, -1, u, -12, dev.wildcatter.block.StorageCompressorBlockEntity.class);
						comp.testDriven();
						head.setMode(dev.wildcatter.block.StorageWellheadBlockEntity.Mode.INJECT);
						mark[0] = at;
						step[0] = 2;
					}
					case 2 -> {
						var head = GridSceneKit.entity(helper, 0, 15, 0, dev.wildcatter.block.StorageWellheadBlockEntity.class);
						if (at >= mark[0] + 200) {
							String h = head.inspect().getString();
							log(scene, "debrining | " + h + " | compressor " + read(helper, -1, u, -12));
							check(helper, h.contains("DEBRINING") && head.gas() > 20, "the compressor's gas should debrine the cavern to the disposal well: " + h);
							head.testGas(dev.wildcatter.well.Cavern.cushion(head.volume()) - head.gas());
							mark[0] = at;
							step[0] = 3;
						}
					}
					case 3 -> {
						var head = GridSceneKit.entity(helper, 0, 15, 0, dev.wildcatter.block.StorageWellheadBlockEntity.class);
						if (at >= mark[0] + 200) {
							String h = head.inspect().getString();
							log(scene, "working gas | " + h);
							check(helper, head.dry() && head.bar() > dev.wildcatter.well.Cavern.minBar() && head.working() > 20, "dry, the compressor should put working gas in over the cushion: " + h);
							double v = head.volume();
							head.testGas((dev.wildcatter.well.Cavern.capacity(v) - dev.wildcatter.well.Cavern.cushion(v)) / 2);   // half full of working gas, by hook
							head.setMode(dev.wildcatter.block.StorageWellheadBlockEntity.Mode.WITHDRAW);
							mark[0] = at;
							step[0] = 4;
						}
					}
					case 4 -> {
						var head = GridSceneKit.entity(helper, 0, 15, 0, dev.wildcatter.block.StorageWellheadBlockEntity.class);
						if (at >= mark[0] + 100) {
							check(helper, head.wetGas() && head.readPoint(7) && head.inspect().getString().contains("WET GAS"), "without a dehydrator it should read WET GAS: " + head.inspect().getString());
							// the glycol dehydrator off the wellhead's south side, on its own set, fuel gas to its reboiler
							for (int z = 1; z <= 6; z++) {
								put(helper, "gas_line", 0, 15, z);
							}
							for (int y = u; y <= 14; y++) {
								put(helper, "gas_line", 0, y, 6);
							}
							put(helper, "gas_line", 0, u, 7);
							place(helper, WildcatterBlocks.GLYCOL_DEHYDRATOR, 0, u, 8, Direction.NORTH);   // x 0-1, z 8-10: in at its north (z 7)
							GridSceneKit.genset(helper, -8, u, 9);
							put(helper, "cable_industrial", -7, u, 9);
							for (int x = -7; x <= -1; x++) {
								put(helper, "cable_industrial", x, u, 8);
							}
							source(helper, 3, u, 8, Product.PIPELINE_GAS, 1.0);
							put(helper, "gas_line", 2, u, 8);
							GridSceneKit.start(helper, -8, u, 9, () -> {});
							mark[0] = at;
							step[0] = 5;
						}
					}
					case 5 -> {
						var head = GridSceneKit.entity(helper, 0, 15, 0, dev.wildcatter.block.StorageWellheadBlockEntity.class);
						var dehy = GridSceneKit.entity(helper, 0, u, 8, dev.wildcatter.block.GlycolDehydratorBlockEntity.class);
						if (dehy.dried() > 10) {
							log(scene, "withdrawing | " + head.inspect().getString() + " | dehydrator " + read(helper, 0, u, 8));
							check(helper, !head.wetGas(), "through the dehydrator it should not read WET GAS: " + head.inspect().getString());
							// the breach: the thin column leached with cavernStability on (the batch's rule)
							var thin = (dev.wildcatter.block.BrineWellheadBlockEntity) helper.getLevel().getBlockEntity(thinPos);
							for (int k = 0; k < 40 && !thin.inspect().getString().contains("BREACHED"); k++) {
								thin.testSalt(Ore.HALITE.kgPerBlock());
								thin.inject(helper.getLevel(), 1.0);
							}
							String th = thin.inspect().getString();
							log(scene, "thin column | " + th);
							check(helper, th.contains("BREACHED"), "with cavernStability on, leaching past the flank wall should breach the cavern: " + th);
							step[0] = 99;
							log(scene, "ok");
							clear(helper, -18, -16, 20, 14, 1, 20);
							helper.succeed();
						} else {
							check(helper, at < mark[0] + 1500, "the dehydrator should dry withdrawn gas: " + read(helper, 0, u, 8) + " | " + head.inspect().getString());
						}
					}
					default -> {
					}
				}
			});
		}
	}

	// ---- frasch (23d) --------------------------------------------------------------------------------------------------------------
	/**
	 * A placed caprock of calcite (13 x 13 x 4) with native sulfur in a third of it, a hole cased into it and a Frasch wellhead: the Frasch
	 * water heater (water by hook, fuel gas off a test source) and the lift air compressor on a 480 V set, both on the steel pipe to its west
	 * side; the molten sulfur out of its east side by pipe to a sulfur vat. It lifts molten sulfur at 2 t/h; the sulfur block in hand, melted
	 * by hook, turns to calcite; with no bleed water well its rate falls to a third (NO BLEED, by hook for the 10 minutes); a disposal
	 * wellhead on another hole into the caprock within 16 blocks brings it back up; the vat takes the molten sulfur.
	 */
	static void frasch(GameTestHelper helper) {
		String scene = "frasch";
		int u = 2;
		floor(helper, -32, -10, 16, 10, 1);
		int sulfur = 0;
		for (int x = -6; x <= 6; x++) {
			for (int z = -6; z <= 6; z++) {
				for (int y = 2; y <= 5; y++) {
					boolean s = Math.floorMod(x + 2 * z + y, 3) == 0 && !(x == 0 && z == 0);
					sulfur += s ? 1 : 0;
					helper.getLevel().setBlock(abs(helper, x, y, z), s ? OreBlocks.block(Ore.SULFUR).defaultBlockState() : Blocks.CALCITE.defaultBlockState(), 3);
				}
			}
		}
		put(helper, "casing", 0, 5, 0);
		put(helper, "casing", 0, 6, 0);
		BlockPos headPos = abs(helper, 0, 7, 0);
		helper.getLevel().setBlock(headPos, WildcatterBlocks.FRASCH_WELLHEAD.defaultBlockState().setValue(dev.wildcatter.block.FraschWellheadBlock.FACING, Direction.EAST), 3);
		// molten sulfur east and down to the vat (x 8-13, z -3..2)
		for (int x = 1; x <= 7; x++) {
			put(helper, "gas_line", x, 7, 0);
		}
		for (int y = u; y <= 6; y++) {
			put(helper, "gas_line", 7, y, 0);
		}
		place(helper, WildcatterBlocks.SULFUR_VAT, 8, u, -3, Direction.NORTH);
		// hot water and air: one pipe off its west side, down outside the caprock and west past the compressor to the heater
		for (int x = -8; x <= -1; x++) {
			put(helper, "gas_line", x, 7, 0);
		}
		for (int y = u; y <= 6; y++) {
			put(helper, "gas_line", -8, y, 0);
		}
		for (int x = -12; x <= -9; x++) {
			put(helper, "gas_line", x, u, 0);
		}
		place(helper, WildcatterBlocks.FRASCH_WATER_HEATER, -16, u, -4, Direction.NORTH);   // x -16..-13, z -4..3; box (-17, -4)
		place(helper, WildcatterBlocks.LIFT_AIR_COMPRESSOR, -11, u, -4, Direction.NORTH);   // x -11..-10, z -4..-1; box (-12, -4)
		source(helper, -11, u, 3, Product.PIPELINE_GAS, 2.0);
		put(helper, "gas_line", -12, u, 3);
		GridSceneKit.genset(helper, -24, u, -6);
		ChemicalScenes.run(helper, "cable_industrial", new int[] {-23, -6}, new int[] {-12, -6}, new int[] {-12, -4});   // 500 kcmil: the compressor's start
		ChemicalScenes.run(helper, "cable_industrial", new int[] {-17, -5}, new int[] {-17, -4});
		var heater = GridSceneKit.entity(helper, -16, u, -4, dev.wildcatter.block.FraschWaterHeaterBlockEntity.class);
		var vat = GridSceneKit.entity(helper, 8, u, -3, dev.wildcatter.block.SulfurVatBlockEntity.class);
		int sulfurBlocks = sulfur;
		int[] step = {0};
		int[] mark = {0};
		GridSceneKit.start(helper, -24, u, -6, () -> {});
		for (int t = 300; t <= 5900; t += 20) {
			final int at = t;
			helper.runAfterDelay(t, () -> {
				var head = (dev.wildcatter.block.FraschWellheadBlockEntity) helper.getLevel().getBlockEntity(headPos);
				heater.addWater(20_000);
				String h = head.inspect().getString();
				switch (step[0]) {
					case 0 -> {
						if (at % 400 == 0) {
							log(scene, "waiting | " + h + " | heater " + read(helper, -16, u, -4) + " | air " + read(helper, -11, u, -4) + " | set " + read(helper, -24, u, -6) + " | cable " + read(helper, -17, u, -4));
						}
						if (head.rate() > 0) {
							log(scene, "lifting | " + h + " | heater " + read(helper, -16, u, -4) + " | air " + read(helper, -11, u, -4));
							check(helper, h.contains("lifting") && head.rate() > 1700 && head.rate() < 2100 && heater.temp() >= 150, "the well should lift about 2 t/h of molten sulfur on 165 C water: " + h);
							check(helper, h.contains("NO BLEED"), "with no bleed water well it should say so: " + h);
							long before = count(helper, Ore.SULFUR);
							head.testMelt(Ore.SULFUR.kgPerBlock() * Tuning.FRASCH_YIELD);
							long after = count(helper, Ore.SULFUR);
							check(helper, after == before - 1, "the sulfur block in hand, melted out, should turn to calcite: " + before + " -> " + after + " of " + sulfurBlocks);
							head.testBleedSpent();
							mark[0] = at;
							step[0] = 1;
						}
					}
					case 1 -> {
						if (at >= mark[0] + 40) {
							log(scene, "no bleed | " + h);
							check(helper, head.rate() < 2000 * 0.4 && h.contains("NO BLEED"), "without a bleed well the rate should be down to a third: " + h);
							put(helper, "casing", 4, 5, 4);
							put(helper, "casing", 4, 6, 4);
							helper.getLevel().setBlock(abs(helper, 4, 7, 4), WildcatterBlocks.DISPOSAL_WELLHEAD.defaultBlockState(), 3);
							mark[0] = at;
							step[0] = 2;
						}
					}
					case 2 -> {
						if (at >= mark[0] + 200) {
							log(scene, "bleed well | " + h + " | vat " + read(helper, 8, u, -3));
							check(helper, head.bleed() > Tuning.BLEED_FLOOR + 0.01 && !h.contains("NO BLEED"), "a bleed water well into the caprock should bring the rate back up: " + h);
							check(helper, vat.poured() > 0, "the vat should take the molten sulfur: " + read(helper, 8, u, -3));
							step[0] = 99;
							log(scene, "ok");
							clear(helper, -32, -10, 16, 10, 1, 12);
							helper.succeed();
						}
					}
					default -> {
					}
				}
			});
		}
	}

	private static long count(GameTestHelper helper, Ore ore) {
		long n = 0;
		for (int x = -6; x <= 6; x++) {
			for (int z = -6; z <= 6; z++) {
				for (int y = 2; y <= 5; y++) {
					n += helper.getLevel().getBlockState(abs(helper, x, y, z)).is(OreBlocks.block(ore)) ? 1 : 0;
				}
			}
		}
		return n;
	}
}
