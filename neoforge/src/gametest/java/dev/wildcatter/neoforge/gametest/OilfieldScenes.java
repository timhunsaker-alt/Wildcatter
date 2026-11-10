package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.abs;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.check;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.place;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.put;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.read;

import dev.wildcatter.Tuning;
import dev.wildcatter.Wildcatter;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.Drilling;
import dev.wildcatter.block.DrillingRigBlockEntity;
import dev.wildcatter.block.PumpjackBlockEntity;
import dev.wildcatter.item.SeismographItem;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import dev.wildcatter.well.Reservoir;
import dev.wildcatter.well.ReservoirData;
import dev.wildcatter.well.SaltDome;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/**
 * Phase 15, the oilfield (docs/salt-dome-oil.md, docs/multiblocks/drilling-mud.md), on the server:
 * <ul>
 * <li>{@code salt_dome}: a forced seed with a grid dome under the scene. The seismograph over it reads SALT DOME and its crest; the
 * dome's feature placed in a box of stone puts its salt and caprock where {@link SaltDome} says; the small rig over a flank field's
 * middle strikes oil and a pumpjack lifts it; a hole down the dome's middle finds only the caprock pool, whose well flows at full rate
 * and falls under half of it once its first 10 % is gone (drained by hook).</li>
 * <li>{@code mud_plant}: the barite mill and the bentonite dryer on a 480 V diesel set, their minerals by hopper, the dryer on gas; each
 * grinds, and a bagging station against each fills bulk bags of barite and of bentonite.</li>
 * <li>{@code rig_mud}: a land rig with a mud hopper against its tanks: the hopper on its own 480 V set mixes a bag of barite a minute;
 * weighted by hook the mud goes from 8.34 to over 13.8 ppg, gelled with bentonite it is not LOW GEL; with fresh mud the rig drilling
 * into a placed rock-salt layer reads WASHOUT and stops; salt-saturated, it drills on through the salt.</li>
 * <li>{@code well_control} (15b2): a land rig with a BOP stack and its accumulator over a forced deep oil field, drilling at 8.6 ppg,
 * takes a KICK; shut in within 30 s it holds and reads its SIDPP and kill weight; weighted to the kill weight by hook, the circulation
 * (sped by hook) kills it and the rig strikes oil. Without its BOP the rig stops short of the deep field: NO BOP.</li>
 * <li>{@code blowout} (15b2): the same, the shut-in left: BLOWOUT, the field's units fall fast, a lit campfire within 16 blocks ignites
 * it; the BOP that still stands, closed from the accumulator's last closes, shuts it in.</li>
 * </ul>
 */
final class OilfieldScenes {
	private OilfieldScenes() {
	}

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> scenes = new LinkedHashMap<>();
		scenes.put("salt_dome", OilfieldScenes::saltDome);
		scenes.put("mud_plant", OilfieldScenes::mudPlant);
		scenes.put("rig_mud", OilfieldScenes::rigMud);
		scenes.put("well_control", h -> wellControl(h, false));
		scenes.put("blowout", h -> wellControl(h, true));
		return scenes;
	}

	/** The height of the scenes' floor in the world: low enough that the small rig reaches a dome's flank fields. */
	private static final int FLOOR_Y = 40;

	/** A stone floor at relative height {@code y} over (x1, z1)..(x2, z2), its chunks force-loaded. */
	private static void floor(GameTestHelper helper, int x1, int z1, int x2, int z2, int y) {
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

	/**
	 * Takes the scene down before it succeeds: everything between the corners goes back to air. A scene left standing (a rig still
	 * drilling, its sets running) stopped the next batch's test from ever finishing.
	 */
	private static void clear(GameTestHelper helper, int x1, int z1, int x2, int z2, int y1, int y2) {
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				for (int y = y1; y <= y2; y++) {
					at.set(abs(helper, x, y, z));
					if (!level(helper).getBlockState(at).isAir()) {
						level(helper).removeBlockEntity(at);
						level(helper).setBlock(at, Blocks.AIR.defaultBlockState(), 2 | 16);
					}
				}
			}
		}
	}

	private static ServerLevel level(GameTestHelper helper) {
		return helper.getLevel();
	}

	/** A run of {@code id} cable on the floor from (x1, z1) to (x2, z2) at height y: along x first, then along z. */
	private static void cableL(GameTestHelper helper, String id, int x1, int z1, int x2, int z2, int y) {
		for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
			put(helper, id, x, y, z1);
		}
		for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
			put(helper, id, x2, y, z);
		}
	}

	private static int pumpPercent(String jack) {
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("pumping at (\\d+)% of full rate").matcher(jack);
		return m.find() ? Integer.parseInt(m.group(1)) : -1;
	}

	// ---- salt_dome ------------------------------------------------------------------------------------------------------------------
	static void saltDome(GameTestHelper helper) {
		String scene = "salt_dome";
		BlockPos o = abs(helper, 0, 0, 0);
		// the scene stands on a floor at y 40 whatever the runner's padding puts the origin at, so the small rig reaches the flanks
		int y = FLOOR_Y - o.getY() + 1;
		int holeY = FLOOR_Y + 1;
		// a seed whose dome stands 10 to 70 blocks south-east of the scene's origin (a 60-block window always meets the part of a dome
		// cell a dome can stand in), with a caprock pool and an oil flank field south of it that the small rig reaches, laid out so the
		// cables to the two wells cross nothing
		long seed = -1;
		SaltDome dome = null;
		Reservoir flank = null;
		for (long s = 1; s < 20_000_000 && seed < 0; s++) {
			for (int[] corner : new int[][] {{10, 10}, {70, 10}, {10, 70}, {70, 70}}) {
				SaltDome d = SaltDome.inCell(s, Math.floorDiv(o.getX() + corner[0], Tuning.DOME_CELL), Math.floorDiv(o.getZ() + corner[1], Tuning.DOME_CELL));
				int dx = d == null ? -1 : d.centerX() - o.getX(), dz = d == null ? -1 : d.centerZ() - o.getZ();
				if (d == null || d.caprockPool() == null || dx < 10 || dx > 70 || dz < 10 || dz > 70) {
					continue;
				}
				for (Reservoir f : d.flanks()) {
					int[] m = f.middle();
					int bx = m[0] - 6;   // the column its cables come down
					if (f.kind() == Reservoir.Kind.OIL && holeY - f.topY() < Tuning.SMALL_RIG_MAX_DEPTH - 5 && m[1] > d.centerZ() + 3
							&& (bx < d.centerX() - 8 || bx > d.centerX() + 3) && m[0] - o.getX() > 8 && m[0] - o.getX() < 88 && m[1] - o.getZ() < 88) {
						seed = s;
						dome = d;
						flank = f;
						break;
					}
				}
				if (seed >= 0) {
					break;
				}
			}
		}
		log(scene, "origin " + o + ", seed " + seed);
		check(helper, seed >= 0, "no seed puts a salt dome with a caprock pool and an oil flank under the scene");
		ReservoirData.TestRegion region = new ReservoirData.TestRegion(o.getX() - 40, o.getZ() - 40, o.getX() + 130, o.getZ() + 130, seed);
		ReservoirData.TEST_REGIONS.add(region);
		ReservoirData.testDomeBiome = true;
		final SaltDome fd = dome;
		final Reservoir ff = flank;
		int cx = dome.centerX() - o.getX(), cz = dome.centerZ() - o.getZ();
		int[] m = flank.middle();
		int hx = m[0] - o.getX(), hz = m[1] - o.getZ();
		log(scene, "seed " + seed + ": dome at " + cx + ", " + cz + " (radius " + dome.radius() + ", crest y " + dome.crestY() + "), flank " + flank.key() + " top y "
				+ flank.topY() + " middle " + hx + ", " + hz + "; caprock pool " + dome.caprockPool().units() + " units");
		floor(helper, -22, -8, 92, 92, y - 1);

		// the seismograph over the dome's middle
		String echo = SeismographItem.report(level(helper), dome.centerX() + 0.5, dome.centerZ() + 0.5);
		check(helper, echo.contains("SALT DOME") && echo.contains("crest at y=" + dome.crestY()), "the seismograph over the dome should read SALT DOME and its crest: " + echo);
		String overFlank = SeismographItem.report(level(helper), m[0] + 0.5, m[1] + 0.5);
		check(helper, overFlank.contains("salt dome's flank") && overFlank.contains("y=" + flank.topY()), "over the flank it should hear the field: " + overFlank);
		// a hole down the middle finds the caprock pool and nothing under it
		Reservoir middle = Drilling.field(level(helper), new BlockPos(dome.centerX(), holeY, dome.centerZ()));
		check(helper, middle != null && middle.source() == Reservoir.Source.CAPROCK && middle.topY() > dome.crestY(), "a hole down the middle should find only the caprock pool: " + middle);
		check(helper, Drilling.field(level(helper), new BlockPos(m[0], holeY, m[1])).equals(flank), "the hole over the flank's middle should reach the flank field");

		// the dome's feature, placed by hand in a box of stone in its chunk round the crest, puts its salt and caprock where SaltDome says
		int x0 = dome.centerX() & ~15, z0 = dome.centerZ() & ~15;
		int ylo = dome.crestY() - 6, yhi = dome.capTop();
		for (int x = x0; x < x0 + 16; x++) {
			for (int z = z0; z < z0 + 16; z++) {
				for (int yy = ylo; yy <= yhi; yy++) {
					level(helper).setBlock(new BlockPos(x, yy, z), Blocks.STONE.defaultBlockState(), 2);
				}
			}
		}
		net.minecraft.world.level.levelgen.feature.Feature feature = level(helper).registryAccess().lookupOrThrow(Registries.FEATURE)
				.getValueOrThrow(ResourceKey.create(Registries.FEATURE, Wildcatter.id("salt_dome")));
		feature.place(level(helper), level(helper).getChunkSource().getGenerator(), level(helper).getRandom(), new BlockPos(dome.centerX(), 0, dome.centerZ()));
		int salt = 0, wantSalt = 0, cap = 0, wantCap = 0, wrong = 0;
		for (int x = x0; x < x0 + 16; x++) {
			for (int z = z0; z < z0 + 16; z++) {
				for (int yy = ylo; yy <= yhi; yy++) {
					var state = level(helper).getBlockState(new BlockPos(x, yy, z));
					boolean isSalt = state.is(OreBlocks.block(Ore.HALITE));
					boolean isCap = state.is(Blocks.CALCITE) || state.is(OreBlocks.block(Ore.SULFUR));
					wantSalt += dome.inSalt(x, yy, z) ? 1 : 0;
					wantCap += dome.inCap(x, yy, z) ? 1 : 0;
					salt += isSalt ? 1 : 0;
					cap += isCap ? 1 : 0;
					wrong += isSalt != dome.inSalt(x, yy, z) || isCap != dome.inCap(x, yy, z) ? 1 : 0;
				}
			}
		}
		check(helper, wantSalt > 50 && wrong == 0 && salt == wantSalt && cap == wantCap, "the placed dome should put its salt and caprock where SaltDome says: salt " + salt + "/" + wantSalt
				+ ", cap " + cap + "/" + wantCap + ", " + wrong + " blocks wrong");
		log(scene, "feature: " + salt + " salt, " + cap + " caprock blocks in the chunk, as SaltDome says");

		// power: a 500 kW diesel set to the west, a trunk of 480 V cable along z -6 and a drop to each well's terminal box
		GridSceneKit.genset(helper, -14, y, -6);                                    // its terminal at (-13, y, -6)
		put(helper, "cable_industrial", -13, y, -6);
		cableL(helper, "cable_industrial", -13, -6, hx - 6, hz, y);                 // to the rig's box at hx - 5 (its back)
		cableL(helper, "cable_industrial", -13, -6, cx - 7, cz, y);                 // to the crest pumpjack's box at cx - 6 (its back)
		// the small rig over the flank's middle, facing east, already drilled to 3 blocks above the field
		BlockPos hole = abs(helper, hx, y, hz);
		place(helper, WildcatterBlocks.DRILLING_RIG, hx, y, hz, Direction.EAST);
		DrillingRigBlockEntity rig = (DrillingRigBlockEntity) level(helper).getBlockEntity(hole);
		rig.testDepth(holeY - flank.topY() - 3);
		// the crest well: an oil wellhead on the dome's middle and a pumpjack over it, facing east
		put(helper, "oil_wellhead", cx, y, cz);
		place(helper, WildcatterBlocks.PUMPJACK, cx - 1, y, cz, Direction.EAST);
		GridSceneKit.start(helper, -14, y, -6, () -> {});
		// polled from the top: callbacks scheduled by the dozen inside another upset the runner
		boolean[] struck = {false};
		for (int t = 320; t <= 1700; t += 20) {
			final int at = t;
			helper.runAfterDelay(t, () -> {
				if (struck[0] || !level(helper).getBlockState(hole).is(WildcatterBlocks.OIL_WELLHEAD)) {
					return;
				}
				struck[0] = true;
				log(scene, "struck after " + at + " ticks");
				// the rig rigged down: a pumpjack over the new wellhead, its box where the rig's cable ended
				GridSceneKit.air(helper, hx - 6, y, hz);
				place(helper, WildcatterBlocks.PUMPJACK, hx - 1, y, hz, Direction.EAST);
				helper.runAfterDelay(300, () -> flankAndCrest(helper, scene, fd, ff, hx, hz, cx, cz, y, region));
			});
		}
	}

	private static void flankAndCrest(GameTestHelper helper, String scene, SaltDome dome, Reservoir flank, int hx, int hz, int cx, int cz, int y, ReservoirData.TestRegion region) {
		String jack = read(helper, hx - 1, y, hz);
		log(scene, "flank pumpjack | " + jack);
		check(helper, jack.matches(".*flowline [1-9]\\d*/.*"), "the pumpjack on the flank well should lift its oil: " + jack);
		ReservoirData data = ReservoirData.get(level(helper));
		check(helper, data.remaining(flank) < flank.units(), "the oil should come out of the flank field: " + data.remaining(flank) + " of " + flank.units());
		String crest = read(helper, cx - 1, y, cz);
		int full = pumpPercent(crest);
		log(scene, "crest pumpjack | " + crest);
		check(helper, full >= 95, "the caprock well should flow at its full rate at first: " + crest);
		Reservoir pool = dome.caprockPool();
		data.testTake(pool, (int) Math.ceil(pool.units() * Tuning.CAPROCK_FLUSH) + 1);
		helper.runAfterDelay(40, () -> {
			String after = read(helper, cx - 1, y, cz);
			int now = pumpPercent(after);
			log(scene, "crest pumpjack after its first 10 % | " + after);
			check(helper, now >= 0 && now < full / 2.0, "after its first 10 % the caprock well should fall under half its rate: " + now + " % from " + full + " %: " + after);
			ReservoirData.TEST_REGIONS.remove(region);
			ReservoirData.testDomeBiome = null;
			log(scene, "ok: flank " + jack.replaceAll(".*flowline ", "flowline ") + "; crest " + full + " % then " + now + " %");
			clear(helper, -22, -8, 92, 92, y - 1, y + 12);
			helper.succeed();
		});
	}

	// ---- mud_plant -------------------------------------------------------------------------------------------------------------------
	/** A vanilla hopper at (x, y, z) holding {@code count} of {@code item}. */
	private static void hopper(GameTestHelper helper, int x, int y, int z, net.minecraft.world.item.Item item, int count) {
		level(helper).setBlock(abs(helper, x, y, z), Blocks.HOPPER.defaultBlockState(), 3);
		((net.minecraft.world.Container) level(helper).getBlockEntity(abs(helper, x, y, z))).setItem(0, new net.minecraft.world.item.ItemStack(item, count));
	}

	static void mudPlant(GameTestHelper helper) {
		String scene = "mud_plant";
		BlockPos o = abs(helper, 0, 0, 0);
		int y = FLOOR_Y - o.getY() + 1;
		floor(helper, -10, -4, 14, 22, y - 1);
		GridSceneKit.genset(helper, 0, y, 0);                                       // its terminal at (1, y, 0)
		put(helper, "cable_industrial", 1, y, 0);
		cableL(helper, "cable_industrial", 1, 0, 3, 16, y);                         // down x 3: the boxes on the west of each main cell
		for (int z = 0; z <= 16; z++) {                                             // 500 kcmil: the mill's 150 kW takes about 200 A
			size(helper, z == 0 ? 1 : 3, y, z);
			size(helper, z == 0 ? 2 : 3, y, z);
			size(helper, 3, y, z);
		}
		place(helper, WildcatterBlocks.BARITE_MILL, 4, y, 0, Direction.NORTH);      // x 4-6, z 0-3, 6 high
		place(helper, WildcatterBlocks.BAGGING_STATION, 4, y, 4, Direction.NORTH);  // against the mill's south side
		place(helper, WildcatterBlocks.BENTONITE_DRYER, 4, y, 8, Direction.NORTH);  // x 4-6, z 8-15, 4 high
		place(helper, WildcatterBlocks.BAGGING_STATION, 4, y, 16, Direction.NORTH); // against the dryer's south end
		TestGasSource.SourceEntity gas = gasSource(helper, 8, y, 10, dev.wildcatter.fluid.Product.PIPELINE_GAS, 1.0);
		put(helper, "gas_line", 7, y, 10);                                          // to the dryer's burner, the east of its bottom cells
		hopper(helper, 5, y + 6, 1, OreBlocks.MINERALS.get(Ore.BARITE), 4);
		helper.runAfterDelay(500, () -> hopper(helper, 5, y + 4, 10, OreBlocks.MINERALS.get(Ore.BENTONITE), 4));   // the dryer started after the mill
		hopper(helper, 4, y + 4, 4, dev.wildcatter.WildcatterItems.EMPTY_BULK_BAG, 4);
		hopper(helper, 4, y + 4, 16, dev.wildcatter.WildcatterItems.EMPTY_BULK_BAG, 4);
		dev.wildcatter.block.MudMillBlockEntity mill = GridSceneKit.entity(helper, 4, y, 0, dev.wildcatter.block.MudMillBlockEntity.class);
		dev.wildcatter.block.MudMillBlockEntity dryer = GridSceneKit.entity(helper, 4, y, 8, dev.wildcatter.block.MudMillBlockEntity.class);
		GridSceneKit.start(helper, 0, y, 0, () -> helper.runAfterDelay(600, () -> {
			String m = read(helper, 4, y, 0), d = read(helper, 4, y, 8);
			log(scene, "mill | " + m + " | dryer | " + d + " | set | " + read(helper, 0, y, 0));
			check(helper, mill.made() > 10 && m.contains("grinding"), "the barite mill should grind its barite on 480 V: " + m);
			check(helper, dryer.made() > 5 && d.contains("drying") && gas.taken > 0, "the bentonite dryer should dry and mill its bentonite on gas: " + d + " | gas " + gas.taken);
			// a bag's worth each by hook (a tonne is 12 minutes of the mill), then the stations fill them
			mill.testGround(dev.wildcatter.Tuning.BULK_BAG_KG);
			dryer.testGround(dev.wildcatter.Tuning.BULK_BAG_KG);
			helper.runAfterDelay(120, () -> {
				var barite = (net.minecraft.world.Container) GridSceneKit.entity(helper, 4, y, 4, dev.wildcatter.block.BaggingStationBlockEntity.class);
				var bentonite = (net.minecraft.world.Container) GridSceneKit.entity(helper, 4, y, 16, dev.wildcatter.block.BaggingStationBlockEntity.class);
				String b1 = read(helper, 4, y, 4), b2 = read(helper, 4, y, 16);
				log(scene, "barite station | " + b1 + " | bentonite station | " + b2);
				check(helper, holds(barite, dev.wildcatter.WildcatterItems.BARITE_BAG), "the station against the mill should fill a bag of barite: " + b1);
				check(helper, holds(bentonite, dev.wildcatter.WildcatterItems.BENTONITE_BAG), "the station against the dryer should fill a bag of bentonite: " + b2);
				log(scene, "ok");
				clear(helper, -10, -4, 14, 22, y - 1, y + 8);
				helper.succeed();
			});
		}));
	}

	private static void size(GameTestHelper helper, int x, int y, int z) {
		if (level(helper).getBlockEntity(abs(helper, x, y, z)) instanceof dev.wildcatter.block.MultiCableBlockEntity cable) {
			cable.setSize(dev.wildcatter.energy.ConductorSize.KCMIL_500);
		}
	}

	private static boolean holds(net.minecraft.world.Container c, net.minecraft.world.item.Item item) {
		for (int i = 0; i < c.getContainerSize(); i++) {
			if (c.getItem(i).is(item)) {
				return true;
			}
		}
		return false;
	}

	private static TestGasSource.SourceEntity gasSource(GameTestHelper helper, int x, int y, int z, dev.wildcatter.fluid.Product product, double rate) {
		level(helper).setBlock(abs(helper, x, y, z), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(WildcatterNeoForgeGameTests.id("gas_source")).defaultBlockState(), 3);
		TestGasSource.SourceEntity s = (TestGasSource.SourceEntity) level(helper).getBlockEntity(abs(helper, x, y, z));
		s.product = product;
		s.rate = rate;
		return s;
	}

	// ---- rig_mud --------------------------------------------------------------------------------------------------------------------
	/** The 1,000 hp land rig (as PlcPlantScenes.rig), its hole at (30, y, 0), on a 500 kW set through a unit substation at 4.16 kV. */
	static dev.wildcatter.block.RigSubstructureBlockEntity landRig(GameTestHelper helper, int y) {
		place(helper, WildcatterBlocks.RIG_SUBSTRUCTURE, 26, y, -4, Direction.NORTH);
		place(helper, WildcatterBlocks.RIG_MAST, 29, y + 6, -1, Direction.NORTH);
		place(helper, WildcatterBlocks.RIG_DRAWWORKS, 27, y + 6, 2, Direction.NORTH);
		place(helper, WildcatterBlocks.MUD_TANKS, 36, y, -5, Direction.NORTH);      // x 36-38, z -5..6
		place(helper, WildcatterBlocks.MUD_PUMP, 40, y, -4, Direction.NORTH);
		place(helper, WildcatterBlocks.MUD_PUMP, 40, y, 0, Direction.NORTH);
		place(helper, WildcatterBlocks.RIG_DRIVE_HOUSE, 24, y, 7, Direction.NORTH);
		GridSceneKit.genset(helper, 10, y, 7);
		place(helper, WildcatterBlocks.UNIT_SUBSTATION, 14, y, 7, Direction.SOUTH);
		for (int x = 15; x <= 23; x++) {
			put(helper, "cable_mv", x, y, 7);
		}
		return GridSceneKit.entity(helper, 26, y, -4, dev.wildcatter.block.RigSubstructureBlockEntity.class);
	}

	private static java.util.regex.Matcher depthOf(String rig) {
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+) m down").matcher(rig);
		m.find();
		return m;
	}

	static void rigMud(GameTestHelper helper) {
		String scene = "rig_mud";
		BlockPos o = abs(helper, 0, 0, 0);
		int y = FLOOR_Y - o.getY() + 1;
		floor(helper, 0, -14, 52, 18, y - 1);
		dev.wildcatter.block.RigSubstructureBlockEntity rig = landRig(helper, y);
		// the mud hopper against the tanks' east face, facing south so its motor's box (its west) faces away, on its own 480 V set
		place(helper, WildcatterBlocks.MUD_HOPPER, 40, y, 5, Direction.SOUTH);      // x 39-40, z 3-5
		GridSceneKit.genset(helper, 46, y, 8);                                      // its skid x 40-46, z 8-9; terminal (47, y, 8)
		put(helper, "cable_industrial", 47, y, 8);
		put(helper, "cable_industrial", 47, y, 9);
		cableL(helper, "cable_industrial", 47, 10, 41, 5, y);
		dev.wildcatter.block.MudHopperBlockEntity hopper = GridSceneKit.entity(helper, 40, y, 5, dev.wildcatter.block.MudHopperBlockEntity.class);
		BlockPos hole = abs(helper, 30, y, 0);
		int[] drillFrom = {0};
		int[] stuck = {-1};
		helper.runAfterDelay(40, () -> {
			check(helper, rig.formed() && rig.installTopDrive(), "the rig should be built: " + read(helper, 26, y, -4));
			check(helper, hopper.rig() == rig, "the mud hopper should find the rig through its mud tanks: " + read(helper, 40, y, 5));
			rig.testWater(40_000);
			String fresh = read(helper, 26, y, -4);
			log(scene, "fresh | " + fresh);
			check(helper, Math.abs(rig.mudPpg() - 8.34) < 0.02 && fresh.contains("8.34 ppg") && rig.lowGel(), "water alone should weigh 8.34 ppg and be LOW GEL: " + fresh);
			hopper.setItem(0, new net.minecraft.world.item.ItemStack(dev.wildcatter.WildcatterItems.BARITE_BAG, 2));
		});
		GridSceneKit.start(helper, 46, y, 8, () -> {});
		GridSceneKit.start(helper, 10, y, 7, () -> {});
		helper.runAfterDelay(300 + 1260, () -> {
			String h = read(helper, 40, y, 5);
			log(scene, "a minute of mixing | " + h + " | rig " + read(helper, 26, y, -4));
			check(helper, hopper.mixed() > 900 && hopper.mixed() < 2000 && rig.mudPpg() > 8.45, "the hopper should mix about a bag of barite a minute: " + hopper.mixed() + " kg | " + h);
			// weighted by hook: bentonite to 25 lb/bbl, then barite until it passes 13.8 ppg
			rig.testMix(dev.wildcatter.block.RigSubstructureBlockEntity.Solid.BENTONITE, 71.3 * 40);
			int bags = 0;
			while (rig.mudPpg() < 13.85 && bags < 200) {
				rig.testMix(dev.wildcatter.block.RigSubstructureBlockEntity.Solid.BARITE, dev.wildcatter.Tuning.BULK_BAG_KG);
				bags++;
			}
			String heavy = read(helper, 26, y, -4);
			log(scene, "weighted with " + bags + " more bags | " + heavy);
			check(helper, rig.mudPpg() > 13.8 && !rig.lowGel() && !heavy.contains("LOW GEL"), "weighted, the mud should pass 13.8 ppg and carry its gel: " + heavy);
			// fresh mud again, a rock-salt layer three to five blocks under the hole, and drill
			rig.testWater(0);
			rig.testWater(40_000);
			rig.testMix(dev.wildcatter.block.RigSubstructureBlockEntity.Solid.BENTONITE, 71.3 * 40);
			for (int k = 3; k <= 5; k++) {
				level(helper).setBlock(hole.below(k), OreBlocks.block(Ore.HALITE).defaultBlockState(), 3);
			}
			rig.toggle();
			drillFrom[0] = 1;
		});
		for (int t = 1600; t <= 3600; t += 20) {
			helper.runAfterDelay(t, () -> {
				if (drillFrom[0] == 0) {
					return;
				}
				String now = read(helper, 26, y, -4);
				int depth = Integer.parseInt(depthOf(now).group(1));
				if (stuck[0] < 0 && now.contains("WASHOUT")) {
					stuck[0] = depth;
					log(scene, "washout | " + now);
					check(helper, depth == 2, "the rig should stop for the salt at 2 m: " + now);
					helper.runAfterDelay(100, () -> {
						String still = read(helper, 26, y, -4);
						check(helper, still.contains("WASHOUT") && Integer.parseInt(depthOf(still).group(1)) == 2, "with fresh mud it should not drill the salt: " + still);
						rig.testMix(dev.wildcatter.block.RigSubstructureBlockEntity.Solid.SALT, 40_000 * dev.wildcatter.Tuning.SALT_SATURATION_KG_PER_L);
						check(helper, rig.saltSaturated(), "salt by hook should saturate the mud: " + read(helper, 26, y, -4));
					});
				} else if (stuck[0] >= 0 && depth >= 6 && rig.saltSaturated()) {
					stuck[0] = Integer.MIN_VALUE / 2;
					String through = read(helper, 26, y, -4);
					check(helper, level(helper).getBlockState(hole.below(4)).is(WildcatterBlocks.CASING), "salt-saturated, it should drill and case through the salt: " + through);
					log(scene, "ok: through the salt | " + through);
					clear(helper, -2, -14, 52, 18, y - 1, y + 50);
					helper.succeed();
				}
			});
		}
	}

	// ---- well_control and blowout (15b2) ----------------------------------------------------------------------------------------------
	static void wellControl(GameTestHelper helper, boolean blowout) {
		String scene = blowout ? "blowout" : "well_control";
		BlockPos o = abs(helper, 0, 0, 0);
		int y = FLOOR_Y - o.getY() + 1;
		BlockPos hole = abs(helper, 30, y, 0);
		long seed = -1;
		for (long s = 1; s < 3_000_000 && seed < 0; s++) {
			Reservoir f = Reservoir.at(s, hole.getX(), hole.getZ());
			if (f != null && f.deep() && f.kind() == Reservoir.Kind.OIL) {
				seed = s;
			}
		}
		check(helper, seed >= 0, "no seed puts a deep oil field under the rig");
		ReservoirData.TestRegion region = new ReservoirData.TestRegion(o.getX() - 20, o.getZ() - 30, o.getX() + 70, o.getZ() + 30, seed);
		ReservoirData.TEST_REGIONS.add(region);
		Reservoir field = Reservoir.at(seed, hole.getX(), hole.getZ());
		floor(helper, 0, -14, 52, 18, y - 1);
		dev.wildcatter.block.RigSubstructureBlockEntity rig = landRig(helper, y);
		place(helper, WildcatterBlocks.BOP_ACCUMULATOR, 18, y, -12, Direction.NORTH);    // x 18-19, z -12..-9: within 20 blocks
		int[] state = {0};
		long[] before = {0};
		int[] blewAt = {0};
		helper.runAfterDelay(40, () -> {
			check(helper, rig.formed() && rig.installTopDrive(), "the rig should be built: " + read(helper, 26, y, -4));
			rig.testWater(40_000);
			rig.testMix(dev.wildcatter.block.RigSubstructureBlockEntity.Solid.BENTONITE, 71.3 * 40);   // 25 lb/bbl: about 8.6 ppg
			rig.testDepth(hole.getY() - field.topY() - 4);
			rig.toggle();
		});
		GridSceneKit.start(helper, 10, y, 7, () -> {});
		for (int t = 300; t <= 5800; t += 20) {
			final int at = t;
			helper.runAfterDelay(t, () -> {
				String now = read(helper, 26, y, -4);
				switch (state[0]) {
					case 0 -> {   // drilling with no BOP: it stops short of the geopressured field
						if (now.contains("NO BOP:")) {
							log(scene, "no BOP | " + now);
							place(helper, WildcatterBlocks.BOP_STACK, 30, y + 1, 0, Direction.NORTH);   // on the wellhead cell under the rotary table
							state[0] = 1;
						}
					}
					case 1 -> {
						if (now.contains("KICK:")) {
							log(scene, "kick after " + at + " ticks | " + now);
							check(helper, rig.mudPpg() < 9 && rig.readPoint(5), "at about 8.6 ppg the rig should take a KICK and read it on its PLC point: " + now);
							if (blowout) {
								rig.testKickTimeout();
								state[0] = 10;
							} else {
								String shut = rig.shutIn(level(helper)).getString();
								log(scene, "shut in | " + shut);
								check(helper, rig.phase() == dev.wildcatter.block.RigSubstructureBlockEntity.Phase.SHUT_IN && shut.contains("SIDPP") && shut.contains("kill weight 13.8"),
										"shut in within 30 s it should hold and read its SIDPP and kill weight: " + shut);
								state[0] = 2;
							}
						}
					}
					case 2 -> {   // held a while, then weighted to the kill weight
						check(helper, rig.phase() == dev.wildcatter.block.RigSubstructureBlockEntity.Phase.SHUT_IN, "shut in, it should stay shut in: " + now);
						while (rig.mudPpg() < 13.82) {
							rig.testMix(dev.wildcatter.block.RigSubstructureBlockEntity.Solid.BARITE, dev.wildcatter.Tuning.BULK_BAG_KG);
						}
						state[0] = 3;
					}
					case 3 -> {
						if (now.contains("CIRCULATING KILL MUD")) {
							log(scene, "circulating | " + now);
							rig.testCirculated(dev.wildcatter.Tuning.KILL_CIRCULATION_TICKS);   // the minute's circulation, by hook
							state[0] = 4;
						}
					}
					case 4 -> {
						if (now.contains("WELL COMPLETE")) {
							check(helper, level(helper).getBlockState(hole).is(WildcatterBlocks.OIL_WELLHEAD), "killed, the rig should strike the oil and set its wellhead: " + now);
							state[0] = 99;
							finish(helper, scene, "ok: killed and struck | " + now, y, region);
						}
					}
					case 10 -> {
						if (now.contains("BLOWOUT")) {
							before[0] = ReservoirData.get(level(helper)).remaining(field);
							blewAt[0] = at;
							log(scene, "blowout | " + now);
							state[0] = 11;
						}
					}
					case 11 -> {   // 100 ticks of it, then fire within reach
						if (at >= blewAt[0] + 100) {
							long gone = before[0] - ReservoirData.get(level(helper)).remaining(field);
							log(scene, "units gone in a while | " + gone);
							check(helper, gone > 300, "a blowout should drain the field fast: " + gone + " units");
							level(helper).setBlock(abs(helper, 30, y, -12), Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, true), 3);
							state[0] = 12;
						}
					}
					case 12 -> {
						if (rig.onFire()) {
							log(scene, "on fire | " + now);
							check(helper, now.contains("ON FIRE"), "the rig should read BLOWOUT ON FIRE: " + now);
							String shut = rig.shutIn(level(helper)).getString();
							check(helper, rig.phase() == dev.wildcatter.block.RigSubstructureBlockEntity.Phase.SHUT_IN, "the BOP that still stands should shut the wild well in: " + shut);
							state[0] = 99;
							finish(helper, scene, "ok: blew out, caught fire, shut in by its BOP | " + shut, y, region);
						}
					}
					default -> {
					}
				}
			});
		}
	}

	private static void finish(GameTestHelper helper, String scene, String text, int y, ReservoirData.TestRegion region) {
		log(scene, text);
		ReservoirData.TEST_REGIONS.remove(region);
		clear(helper, -2, -14, 52, 18, y - 1, y + 50);
		helper.succeed();
	}

	static PumpjackBlockEntity jack(GameTestHelper helper, int x, int y, int z) {
		return (PumpjackBlockEntity) BigParts.entity(level(helper), abs(helper, x, y, z));
	}
}
