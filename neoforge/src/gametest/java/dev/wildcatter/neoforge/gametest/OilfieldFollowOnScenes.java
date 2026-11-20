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
}
