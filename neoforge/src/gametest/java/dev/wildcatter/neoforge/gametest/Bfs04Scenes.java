package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.check;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.put;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.read;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.block.GensetControlPanelBlockEntity;
import dev.wildcatter.block.InsulatorBlock;
import dev.wildcatter.block.LoadBlock;
import dev.wildcatter.block.MotorStarterBlockEntity;
import dev.wildcatter.block.TransformerBlock;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.ConductorSize;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.item.SpoolItem;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.quest.Milestones;
import dev.wildcatter.quest.QuestBook;
import dev.wildcatter.quest.QuestData;
import dev.wildcatter.quest.QuestTracker;
import dev.wildcatter.well.Reservoir;
import dev.wildcatter.well.ReservoirData;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Built From Scratch, episode 4 "Gas, motors and poles" (Three-phase power after the step-up, and the first half of Distribution): the
 * proof that what the episode films works in normal play.
 * <ul>
 * <li>{@code bfs04_gas_genset}: the episode's gas well site, relative to its Christmas tree at (0, 1, 0) (the film's tree at -478, 64, 980).
 * The gas set's base frame is set into the ground, so its control panel's terminals are at the height of everything it feeds. Piped
 * straight from the tree, the engine gets wet gas and will not run: its readout says to put a separator between them (the episode's
 * "what goes wrong"). With a separator between the tree and the engine (well stream in at its front end, gas off the top of its back
 * end to the engine), the set starts, comes up to speed, closes its breaker and fires {@code gas_genset_running}: The gas genset done on
 * its two parts carried. Then a three-pole breaker and a motor starter set in a row against the panel's terminals, the drilling rig's
 * terminal box against the starter: the starter's overload stepped to the rig's 45 A, the starter's button pressed, the rig's motor
 * comes up to speed and the starter fires {@code motor_started} (Start a motor). A dry-type against the panel's other terminal steps the
 * industrial tier down to the low tier; a single-phase cable from its low side up a pole into a pole transformer's low side runs that
 * unit backward to the medium-voltage tier on its H1; a lead to the pole-top insulator, a span to a second pole, a lead down to a second
 * pole transformer, and its low side lights the lamps along a cable at the pole's foot. The far unit fires
 * {@code pole_transformer_energised} (A pole transformer); Poles and lines, The MCC, A PLC and Pad-mounts finish on their parts.</li>
 * <li>{@code bfs04_rig_start}: the Field Manual's drilling page told the player to feed the rig through "a 60 A three-pole breaker". A 60 A
 * three-pole breaker in line ahead of the rig's starter trips on the rig's start (six times its 45 A for two seconds, which the breaker's
 * thermal element counts as an overload), so the advice left the rig unable to start. The page now says to start the rig from a motor
 * starter against its terminal box with the overload set to its current, as the episode does (bfs04_gas_genset proves that runs); this
 * scene shows the trip and checks the page.</li>
 * <li>{@code bfs04_recipes}: every grid the episode lays out crafts what the episode makes, as the crafting table asks the server.</li>
 * </ul>
 */
final class Bfs04Scenes {
	private Bfs04Scenes() {
	}

	/** Quick scenes (QUEST_SCENES). */
	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> out = new LinkedHashMap<>();
		out.put("bfs04_recipes", Bfs04Scenes::recipes);
		return out;
	}

	/** Scenes with a running plant (QUEST_MACHINE_SCENES). */
	static Map<String, Consumer<GameTestHelper>> machineScenes() {
		Map<String, Consumer<GameTestHelper>> out = new LinkedHashMap<>();
		out.put("bfs04_gas_genset", Bfs04Scenes::gasGenset);
		out.put("bfs04_rig_start", Bfs04Scenes::rigStart);
		return out;
	}

	private static boolean done(MinecraftServer server, ServerPlayer player, String quest) {
		return QuestData.get(server).of(player.getStringUUID()).done.contains("wildcatter:" + quest);
	}

	private static boolean reached(MinecraftServer server, ServerPlayer player, Identifier key) {
		return QuestData.get(server).of(player.getStringUUID()).milestones.contains(key.toString());
	}

	private static void earlier(MinecraftServer server, ServerPlayer player, String... quests) {
		var p = QuestData.get(server).of(player.getStringUUID());
		for (String q : quests) {
			p.done.add("wildcatter:" + q);
		}
		QuestData.get(server).changed();
	}

	private static Item item(String id) {
		return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
	}

	private static Block block(String id) {
		return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
	}

	private static boolean lit(GameTestHelper helper, int x, int y, int z) {
		BlockState state = helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(x, y, z)));
		return state.hasProperty(LoadBlock.LIT) && state.getValue(LoadBlock.LIT);
	}

	private static void carry(ServerPlayer player, String id, int n) {
		player.getInventory().add(new ItemStack(item(id), n));
		QuestTracker.check(player);
	}

	/** A seed whose shallow gas field lies under {@code at}. */
	private static long gasSeed(BlockPos at) {
		for (long s = 1; s < 3_000_000; s++) {
			Reservoir f = Reservoir.at(s, at.getX(), at.getZ(), d -> false);
			if (f != null && f.units() > 0 && f.kind() == Reservoir.Kind.GAS && !f.deep()) {
				return s;
			}
		}
		return -1;
	}

	// ---- the gas well site --------------------------------------------------------------------------------------------------------------

	// relative to the tree at (0, 1, 0); y 1 is the ground's top cell's air (the film's y 64), the floor at y 0
	static final int[] PANEL = {4, 1, 4};          // the control panel's main cell (north row); the set facing north, the shaft running west
	static final int[] ENGINE = {-1, 1, 4};        // the gas engine's main cell: cells x -1..2, z 4..5, y 1..2
	static final int[] STARTER = {5, 1, 4};        // against the panel's north terminal: a combination starter (its own breaker and overload)
	static final int[] HOLE = {11, 1, 4};          // the rig's mast over the hole; its terminal box at x 6, against the starter
	static final int[] DRY = {5, 1, 5};            // against the panel's south terminal, its high side (front) west
	static final int POLE_X = 5, POLE_Z = 8;       // the first pole; the pole transformer on its north side at y 5
	static final int FAR_X = -18;                  // the second pole, 24 m west

	static void gasGenset(GameTestHelper helper) {
		String scene = "bfs04_gas_genset";
		MinecraftServer server = helper.getLevel().getServer();
		QuestBook book = QuestTracker.book(server);
		for (String ch : new String[] {"three_phase", "distribution"}) {
			check(helper, book.chapter(Identifier.parse("wildcatter:" + ch)) != null, "no chapter " + ch);
		}
		BlockPos treeAbs = helper.absolutePos(new BlockPos(0, 1, 0));
		int dx = Math.floorDiv(treeAbs.getX(), Tuning.FIELD_CELL) * Tuning.FIELD_CELL + Tuning.FIELD_CELL / 2 - treeAbs.getX();
		int dz = Math.floorDiv(treeAbs.getZ(), Tuning.FIELD_CELL) * Tuning.FIELD_CELL + Tuning.FIELD_CELL / 2 - treeAbs.getZ();
		long seed = gasSeed(treeAbs.offset(dx, 0, dz));
		check(helper, seed > 0, "no seed puts a gas field under the tree");
		ReservoirData.TestRegion region = new ReservoirData.TestRegion(treeAbs.getX() - 2, treeAbs.getZ() - 2, treeAbs.getX() + 2, treeAbs.getZ() + 2, seed, dx, dz);
		ReservoirData.TEST_REGIONS.add(region);
		GridSceneKit.floor(helper, -24, -4, 20, 14);
		for (int x = -24; x <= 20; x++) {
			for (int z = -4; z <= 14; z++) {
				for (int y = 1; y <= 12; y++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
				}
			}
		}
		put(helper, "gas_wellhead", 0, 1, 0);
		// the gas set, its base frame in the ground (y 0), the parts on it at y 1-2
		dev.wildcatter.block.GensetSkid.build(helper.getLevel(), GridSceneKit.abs(helper, PANEL[0], PANEL[1], PANEL[2]), Direction.NORTH, true);
		GensetControlPanelBlockEntity panel = GridSceneKit.gensetPanel(helper, PANEL[0], PANEL[1], PANEL[2]);
		// first, the mistake: steel pipe straight from the tree to the engine's north face
		for (int z = 1; z <= 3; z++) {
			put(helper, "gas_line", 0, 1, z);
		}
		ServerPlayer player = QuestScenes.player(helper, -4, 1, 8);
		earlier(server, player, "wood", "stone", "campfire", "iron", "copper", "steel", "field_manual", "steam", "lamp", "protection", "firebox", "plugs",
				"seismograph", "step_up", "rig", "gas_well", "oil_well", "lease", "crude");
		carry(player, "wildcatter:engine_block_casting", 1);
		carry(player, "wildcatter:crankshaft", 1);
		carry(player, "wildcatter:cylinder_head", 1);
		carry(player, "wildcatter:stator_winding", 1);
		carry(player, "wildcatter:rotor_field", 1);
		carry(player, "wildcatter:exciter", 1);
		check(helper, done(server, player, "engine_parts"), "the six parts should finish Engine and generator parts");
		carry(player, "wildcatter:genset_gas_engine", 1);
		carry(player, "wildcatter:genset_control_panel", 1);
		player.getInventory().clearContent();   // the parts set down
		helper.runAfterDelay(30, () -> {
			check(helper, panel.formed(), "the gas set should form with its base frame in the ground: " + read(helper, PANEL[0], PANEL[1], PANEL[2]));
			panel.setWantRun(true);
		});
		helper.runAfterDelay(200, () -> {
			String wet = read(helper, PANEL[0], PANEL[1], PANEL[2]);
			log(scene, "straight from the tree | " + wet + " | " + read(helper, 0, 1, 0));
			check(helper, wet.contains("WET GAS"), "wet gas straight from the tree should not run the set: " + wet);
			check(helper, !reached(server, player, Milestones.GAS_GENSET_RUNNING), "a set on wet gas should not fire gas_genset_running");
			QuestTracker.check(player);
			check(helper, !done(server, player, "genset"), "The gas genset should wait for the set to run");
			// the fix: the straight run out, a separator between (its front end west, by the tree), its gas to the engine
			helper.setBlock(new BlockPos(0, 1, 2), Blocks.AIR);
			helper.setBlock(new BlockPos(0, 1, 3), Blocks.AIR);
			GridSceneKit.place(helper, block("wildcatter:separator"), 1, 1, 1, Direction.WEST);
			List<BlockPos> cells = BigParts.cellsAt(helper.getLevel(), GridSceneKit.abs(helper, 1, 1, 1));
			StringBuilder b = new StringBuilder();
			for (BlockPos c : cells) {
				b.append(helper.relativePos(c).toShortString()).append("; ");
			}
			log(scene, "separator cells " + b);
			for (int[] c : SEP_GAS) {
				put(helper, "gas_line", c[0], c[1], c[2]);
			}
			CircuitManager.markDirty(helper.getLevel());
		});
		// start again (a set refused for wet gas stays stopped until started again)
		helper.runAfterDelay(230, () -> panel.setWantRun(true));
		helper.runAfterDelay(520, () -> {
			String run = read(helper, PANEL[0], PANEL[1], PANEL[2]);
			String sep = read(helper, 1, 1, 1);
			log(scene, "through the separator | " + run + " | " + sep + " | " + read(helper, 0, 1, 0));
			check(helper, run.contains("RUNNING") && run.contains("breaker CLOSED"), "through a separator the set should run with its breaker closed: " + run + " | " + sep);
			check(helper, reached(server, player, Milestones.GAS_GENSET_RUNNING), "the running gas set should fire gas_genset_running: " + run);
			QuestTracker.check(player);
			check(helper, done(server, player, "genset"), "the gas set running, with its engine and panel carried, should finish The gas genset");
			// Start a motor: the combination starter against the panel's north terminal, the rig's terminal box against the starter
			helper.setBlock(new BlockPos(STARTER[0], STARTER[1], STARTER[2]), WildcatterBlocks.MOTOR_STARTER.defaultBlockState()
					.setValue(dev.wildcatter.block.MotorStarterBlock.FACING, Direction.EAST));
			GridSceneKit.place(helper, WildcatterBlocks.DRILLING_RIG, HOLE[0], HOLE[1], HOLE[2], Direction.EAST);
			BlockPos box = dev.wildcatter.block.DrillingRigBlock.terminalBox(GridSceneKit.abs(helper, HOLE[0], HOLE[1], HOLE[2]),
					helper.getLevel().getBlockState(GridSceneKit.abs(helper, HOLE[0], HOLE[1], HOLE[2])));
			log(scene, "rig's terminal box at " + helper.relativePos(box).toShortString());
			check(helper, helper.relativePos(box).equals(new BlockPos(STARTER[0] + 1, STARTER[1], STARTER[2])), "the rig's box should be against the starter's front");
			carry(player, "wildcatter:breaker_3p_industrial", 1);
			carry(player, "wildcatter:industrial_wire_hot", 6);
			carry(player, "wildcatter:motor_starter", 1);
			player.getInventory().clearContent();
			CircuitManager.markDirty(helper.getLevel());
		});
		helper.runAfterDelay(560, () -> {
			MotorStarterBlockEntity starter = GridSceneKit.entity(helper, STARTER[0], STARTER[1], STARTER[2], MotorStarterBlockEntity.class);
			String before = read(helper, STARTER[0], STARTER[1], STARTER[2]);
			for (int i = 0; i < 12 && !read(helper, STARTER[0], STARTER[1], STARTER[2]).contains("set to 45.0 A"); i++) {
				starter.nextOverloadSetting();
			}
			log(scene, "starter before " + before + " | stepped " + read(helper, STARTER[0], STARTER[1], STARTER[2]) + " | rig " + read(helper, HOLE[0], HOLE[1], HOLE[2]));
			check(helper, !reached(server, player, Milestones.MOTOR_STARTED), "a starter never started should not fire motor_started");
			starter.pushButton();
			CircuitManager.markDirty(helper.getLevel());
		});
		helper.runAfterDelay(820, () -> {
			String starter = read(helper, STARTER[0], STARTER[1], STARTER[2]);
			String rig = read(helper, HOLE[0], HOLE[1], HOLE[2]);
			log(scene, "rig on the starter | " + starter + " | " + rig + " | set " + read(helper, PANEL[0], PANEL[1], PANEL[2]));
			check(helper, rig.contains("drilling"), "started at the starter the rig should drill: " + rig + " | " + starter);
			check(helper, reached(server, player, Milestones.MOTOR_STARTED), "the starter should fire motor_started: " + starter);
			QuestTracker.check(player);
			check(helper, done(server, player, "motor"), "Start a motor should be done");
			carry(player, "wildcatter:mcc_incoming", 1);
			carry(player, "wildcatter:mcc_section", 1);
			check(helper, done(server, player, "mcc"), "an incoming section and a starter section should finish The MCC");
			carry(player, "wildcatter:plc_cabinet", 1);
			carry(player, "wildcatter:cpu_small", 1);
			check(helper, done(server, player, "plc"), "a cabinet and a CPU should finish A PLC");
			// the pole line: dry-type off the panel's south terminal, the cable up the first pole to a pole transformer run backward
			helper.setBlock(new BlockPos(DRY[0], DRY[1], DRY[2]), WildcatterBlocks.DRY_TYPE_TRANSFORMER.defaultBlockState().setValue(TransformerBlock.FACING, Direction.WEST));
			int[][] cable = {{5, 1, 6}, {5, 1, 7}, {5, 2, 7}, {5, 3, 7}, {5, 4, 7}};
			for (int[] c : cable) {
				put(helper, "single_phase_cable", c[0], c[1], c[2]);
			}
			for (int px : new int[] {POLE_X, FAR_X}) {
				GridSceneKit.place(helper, WildcatterBlocks.WOOD_POLE_30, px, 1, POLE_Z, Direction.NORTH);
				BlockPos top = GridSceneKit.abs(helper, px, 9, POLE_Z);
				InsulatorBlock ins = (InsulatorBlock) WildcatterBlocks.INSULATOR_DIST;
				InsulatorBlock.Mount mount = ins.mountAt(helper.getLevel(), top);
				check(helper, mount != null, "a pin insulator should stand on the 30 ft pole's top at y 9");
				helper.getLevel().setBlock(top, ins.defaultBlockState().setValue(InsulatorBlock.MOUNT, mount).setValue(dev.wildcatter.block.CableBlock.CONDUCTOR, Conductor.A), 3);
				helper.getLevel().setBlock(GridSceneKit.abs(helper, px, 5, POLE_Z - 1), WildcatterBlocks.POLE_TRANSFORMER_MV.defaultBlockState().setValue(TransformerBlock.FACING, Direction.NORTH), 3);
			}
			// the far unit's low side: a cable from its bottom down the pole, along the ground, lamps beside it
			int[][] down = {{FAR_X, 4, 7}, {FAR_X, 3, 7}, {FAR_X, 2, 7}, {FAR_X, 1, 7}, {FAR_X + 1, 1, 7}, {FAR_X + 2, 1, 7}, {FAR_X + 3, 1, 7}, {FAR_X + 4, 1, 7}};
			for (int[] c : down) {
				put(helper, "single_phase_cable", c[0], c[1], c[2]);
			}
			for (int[] l : LAMPS) {
				put(helper, "electric_lamp", l[0], l[1], l[2]);
			}
			BlockPos near = GridSceneKit.abs(helper, POLE_X, 9, POLE_Z), far = GridSceneKit.abs(helper, FAR_X, 9, POLE_Z);
			SpoolItem.Result span = SpoolItem.connect(helper.getLevel(), near, far, Voltage.MEDIUM, false, 1024, ConductorSize.ACSR_SPARROW);
			check(helper, span.ok(), "a span of the MV spool should string 24 m: " + span);
			SpoolItem.Result lead1 = SpoolItem.connectLead(helper.getLevel(), near, GridSceneKit.abs(helper, POLE_X, 5, POLE_Z - 1), Voltage.MEDIUM, false, 64, ConductorSize.ACSR_SPARROW);
			SpoolItem.Result lead2 = SpoolItem.connectLead(helper.getLevel(), far, GridSceneKit.abs(helper, FAR_X, 5, POLE_Z - 1), Voltage.MEDIUM, false, 64, ConductorSize.ACSR_SPARROW);
			check(helper, lead1.ok() && lead2.ok(), "leads down to both pole units should string: " + lead1 + " | " + lead2);
			log(scene, "line " + span.message() + " | " + lead1.message() + " | " + lead2.message());
			carry(player, "wildcatter:wood_pole_30", 2);
			carry(player, "wildcatter:crossarm_dist", 1);
			carry(player, "wildcatter:insulator_dist", 2);
			carry(player, "wildcatter:mv_wire_hot", 4);
			check(helper, done(server, player, "poles"), "two poles, a crossarm, two insulators and four MV wires should finish Poles and lines");
			carry(player, "wildcatter:pole_transformer_mv", 1);
			check(helper, !done(server, player, "pole_transformer"), "A pole transformer should wait for a pole unit's high side to be live");
			CircuitManager.markDirty(helper.getLevel());
		});
		helper.runAfterDelay(1000, () -> {
			String dry = read(helper, DRY[0], DRY[1], DRY[2]);
			String up = read(helper, POLE_X, 5, POLE_Z - 1);
			String downUnit = read(helper, FAR_X, 5, POLE_Z - 1);
			String farIns = read(helper, FAR_X, 9, POLE_Z);
			StringBuilder lamps = new StringBuilder();
			int litCount = 0;
			for (int[] l : LAMPS) {
				boolean on = lit(helper, l[0], l[1], l[2]);
				litCount += on ? 1 : 0;
				lamps.append(on ? "lit " : "dark ");
			}
			log(scene, "line | dry-type " + dry + " | step-up unit " + up + " | far insulator " + farIns + " | far unit " + downUnit + " | lamps " + lamps
					+ " | cable " + read(helper, FAR_X + 2, 1, 7) + " | set " + read(helper, PANEL[0], PANEL[1], PANEL[2]) + " | rig " + read(helper, HOLE[0], HOLE[1], HOLE[2]));
			check(helper, farIns.contains("4.16 kV") || farIns.contains(Voltage.MEDIUM.label()), "the far insulator should carry the medium-voltage tier: " + farIns);
			check(helper, litCount == LAMPS.length, "every lamp along the cable at the far pole should light: " + lamps + " | " + downUnit);
			check(helper, reached(server, player, Milestones.POLE_TRANSFORMER_ENERGISED), "the far pole unit should fire pole_transformer_energised: " + downUnit);
			QuestTracker.check(player);
			check(helper, done(server, player, "pole_transformer"), "A pole transformer should be done");
			carry(player, "wildcatter:padmount_480", 1);
			check(helper, done(server, player, "padmount"), "a pad-mount carried should finish Pad-mounts");
			log(scene, "ok: quests " + QuestData.get(server).of(player.getStringUUID()).done);
			ReservoirData.TEST_REGIONS.remove(region);
			QuestScenes.leave(helper, player);
			helper.succeed();
		});
	}

	static void rigStart(GameTestHelper helper) {
		String scene = "bfs04_rig_start";
		String pages = dev.wildcatter.item.Manual.content().pages().stream().map(f -> f.raw().getString()).reduce("", String::concat);
		check(helper, !pages.contains("use a 60 A three-pole breaker"), "the Field Manual should no longer tell the player to feed the rig through a 60 A breaker");
		check(helper, pages.contains("Start it from a MOTOR STARTER against the box"), "the Field Manual's drilling page should say to start the rig from a motor starter");
		GridSceneKit.floor(helper, -8, -4, 14, 4);
		for (int x = -8; x <= 14; x++) {
			for (int z = -4; z <= 4; z++) {
				for (int y = 1; y <= 11; y++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
				}
			}
		}
		GridSceneKit.genset(helper, 0, 1, 0);   // its 480 V terminal the cell east of the panel
		helper.setBlock(new BlockPos(1, 1, 0), WildcatterBlocks.BREAKER_3P_INDUSTRIAL.defaultBlockState()
				.setValue(dev.wildcatter.block.BreakerBlock.FACING, Direction.EAST).setValue(dev.wildcatter.block.GearMount.MOUNT, dev.wildcatter.block.GearMount.FLOOR));
		helper.setBlock(new BlockPos(2, 1, 0), WildcatterBlocks.MOTOR_STARTER.defaultBlockState().setValue(dev.wildcatter.block.MotorStarterBlock.FACING, Direction.EAST));
		GridSceneKit.place(helper, WildcatterBlocks.DRILLING_RIG, 8, 1, 0, Direction.EAST);   // its terminal box at x 3, against the starter
		var breaker = GridSceneKit.entity(helper, 1, 1, 0, dev.wildcatter.block.BreakerBlockEntity.class);
		for (int i = 0; i < 8 && breaker.rating() != 60; i++) {
			breaker.nextRating();
		}
		breaker.setSwitch(true);
		MotorStarterBlockEntity starter = GridSceneKit.entity(helper, 2, 1, 0, MotorStarterBlockEntity.class);
		for (int i = 0; i < 12 && Math.abs(starter.overloadAmps() - 45) > 0.1; i++) {
			starter.nextOverloadSetting();
		}
		GridSceneKit.start(helper, 0, 1, 0, () -> {
			starter.pushButton();
			CircuitManager.markDirty(helper.getLevel());
			helper.runAfterDelay(100, () -> {
				String b = read(helper, 1, 1, 0), st = read(helper, 2, 1, 0), rig = read(helper, 8, 1, 0);
				log(scene, "60 A breaker ahead of the starter | " + b + " | " + st + " | " + rig);
				check(helper, b.contains("TRIPPED"), "a 60 A breaker ahead of the rig's starter should trip on the rig's start: " + b);
				log(scene, "ok: " + b);
				helper.succeed();
			});
		});
	}

	/** The separator's gas off the top of its back end (x 4) to the engine's north face at (2, 2, 4): along the top, then down. */
	static final int[][] SEP_GAS = {{4, 3, 1}, {4, 3, 2}, {4, 3, 3}, {3, 3, 3}, {2, 3, 3}, {2, 2, 3}};
	/** Lamps beside the cable at the far pole's foot: one beside its run, one at its end. */
	static final int[][] LAMPS = {{FAR_X + 2, 1, 6}, {FAR_X + 5, 1, 7}};

	// ---- recipes ------------------------------------------------------------------------------------------------------------------------

	static net.minecraft.world.item.crafting.CraftingInput grid(String[] pattern, Object... kv) {
		Map<Character, Item> key = new java.util.HashMap<>();
		for (int i = 0; i < kv.length; i += 2) {
			key.put((Character) kv[i], kv[i + 1] instanceof Item it ? it : item((String) kv[i + 1]));
		}
		int w = 0;
		for (String r : pattern) {
			w = Math.max(w, r.length());
		}
		List<ItemStack> stacks = new java.util.ArrayList<>();
		for (String r : pattern) {
			for (int c = 0; c < w; c++) {
				char ch = c < r.length() ? r.charAt(c) : ' ';
				stacks.add(ch == ' ' ? ItemStack.EMPTY : new ItemStack(key.get(ch)));
			}
		}
		return net.minecraft.world.item.crafting.CraftingInput.of(w, pattern.length, stacks);
	}

	static void recipes(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		String st = "wildcatter:steel_ingot";
		Object[][] cases = {
				{"wildcatter:engine_block_casting", grid(new String[] {"sss", "sbs", "sss"}, 's', st, 'b', Items.IRON_BLOCK)},
				{"wildcatter:crankshaft", grid(new String[] {"s  ", " s ", "  s"}, 's', st)},
				{"wildcatter:cylinder_head", grid(new String[] {"sss", "i i"}, 's', st, 'i', Items.IRON_INGOT)},
				{"wildcatter:stator_winding", grid(new String[] {"ccc", "csc", "ccc"}, 'c', Items.COPPER_INGOT, 's', st)},
				{"wildcatter:rotor_field", grid(new String[] {"csc", "sbs", "csc"}, 'c', Items.COPPER_INGOT, 's', st, 'b', Items.IRON_BLOCK)},
				{"wildcatter:exciter", grid(new String[] {"crc", "sds"}, 'c', Items.COPPER_INGOT, 'r', Items.REDSTONE, 's', st, 'd', Items.COMPARATOR)},
				{"wildcatter:piston", grid(new String[] {"ssn"}, 's', st, 'n', Items.IRON_NUGGET)},
				{"wildcatter:genset_gas_engine", grid(new String[] {"ihi", "pcp", "pbp"}, 'i', "wildcatter:spark_plug", 'h', "wildcatter:cylinder_head", 'p', "wildcatter:piston",
						'c', "wildcatter:crankshaft", 'b', "wildcatter:engine_block_casting")},
				{"wildcatter:alternator", grid(new String[] {"sws", "wrw", "sxs"}, 's', st, 'w', "wildcatter:stator_winding", 'r', "wildcatter:rotor_field", 'x', "wildcatter:exciter")},
				{"wildcatter:breaker_3p_industrial", grid(new String[] {"bbb", "srs"}, 'b', "wildcatter:breaker", 's', st, 'r', Items.REDSTONE_BLOCK)},
				{"wildcatter:motor_starter", grid(new String[] {"sbs", "crc", "sss"}, 's', st, 'b', "wildcatter:breaker_3p_industrial", 'c', Items.COPPER_INGOT, 'r', Items.REPEATER)},
				{"wildcatter:mcc_incoming", grid(new String[] {"sbs", "ccc", "sss"}, 's', st, 'b', "wildcatter:breaker_3p_industrial", 'c', "minecraft:copper_block")},
				{"wildcatter:mcc_section", grid(new String[] {"sbs", "s s", "sbs"}, 's', st, 'b', Items.IRON_BARS)},
				{"wildcatter:plc_cabinet", grid(new String[] {"sss", "srs", "sis"}, 's', st, 'r', Items.REPEATER, 'i', Items.IRON_BLOCK)},
				{"wildcatter:cpu_small", grid(new String[] {"qcq", "rgr", "qcq"}, 'q', Items.QUARTZ, 'c', Items.COMPARATOR, 'r', Items.REDSTONE, 'g', Items.GOLD_INGOT)},
				{"wildcatter:wood_pole_30", grid(new String[] {"ll", "ll", "c "}, 'l', Items.OAK_LOG, 'c', Items.COAL)},
				{"wildcatter:crossarm_dist", grid(new String[] {"ppp", "n n"}, 'p', Items.OAK_PLANKS, 'n', Items.IRON_NUGGET)},
				{"wildcatter:insulator_dist", grid(new String[] {"b", "b", "n"}, 'b', Items.BRICK, 'n', Items.IRON_NUGGET)},
				{"wildcatter:pole_transformer_mv", grid(new String[] {"ici", "ici", "ici"}, 'i', Items.IRON_INGOT, 'c', Items.COPPER_INGOT)},
				{"wildcatter:padmount_480", grid(new String[] {"sds", "tct", "sss"}, 's', st, 'd', Items.DIAMOND, 't', "wildcatter:pole_transformer_mv", 'c', "minecraft:copper_block")},
				{"wildcatter:separator", grid(new String[] {"sss", "pgp", "sss"}, 's', st, 'p', "wildcatter:gas_line", 'g', Items.GLASS)},
		};
		StringBuilder ok = new StringBuilder();
		for (Object[] c : cases) {
			var input = (net.minecraft.world.item.crafting.CraftingInput) c[1];
			var recipe = server.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, helper.getLevel());
			ItemStack out = recipe.map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
			check(helper, out.is(item((String) c[0])), "the episode's grid for " + c[0] + " should craft it, not " + out);
			ok.append(c[0].toString().substring(11)).append(' ');
		}
		check(helper, WildcatterItems.SPOOL_MV != null, "an MV spool");
		log("bfs04_recipes", "ok: " + ok);
		helper.succeed();
	}
}
