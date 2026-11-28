package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.check;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.put;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.read;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.InsulatorBlock;
import dev.wildcatter.block.LoadBlock;
import dev.wildcatter.block.TransformerBlock;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.ConductorSize;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.item.SpoolItem;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.quest.QuestData;
import dev.wildcatter.quest.QuestTracker;
import java.util.LinkedHashMap;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Built From Scratch, episode 5 "Medium voltage" (Distribution, second half: the steam plant, the unit substation and GSU, the MV
 * switchgear): the proof that what the episode films works in normal play. The land-rig half moved to episode 6 (Tim, 2026-11-27).
 * <ul>
 * <li>{@code bfs05_unit_sub_line}: episode 4's gas well end of the pole line (relative to its Christmas tree at (0, 1, 0), as
 * Bfs04Scenes): the set's panel, the rig's old starter on its north terminal, the stand-in on its south terminal (a dry-type down to the
 * low tier, a single-phase cable up the first pole into a pole transformer run backward), a span to a far pole whose unit lights two
 * lamps. The film's swap: the set stopped, the starter, the dry-type, the cable and the backfed unit taken off; a unit substation's
 * 480 V end against the panel's south terminal, so it steps the industrial tier straight up to medium voltage (fed from its 480 V side
 * it runs backward, as the land rig's scenes use it); a bare medium-voltage wire from its medium-voltage end along the ground and up
 * the first pole beside the insulator; the set started again. The far lamps light again on the unit substation alone.</li>
 * <li>{@code bfs05_steam_plant} (a long scene): the steam plant from cold, built from its parts, with the episode's starting set: a
 * small steam set (firebox, boiler and turbine, as episode two's) and the dry-type from the stand-in against the feed pump's terminal
 * box, its high side (front) to the box; a 120 V water pump against the turbine, in water, piped to the condenser. Lit with the
 * starting set cold (the episode's "what goes wrong"), the burner fires but the drum stays dry; with the starting set running, the
 * makeup pump fills the hotwell and the feed pump fills the drum, the plant rolls, closes its breaker and runs, The steam plant done.
 * A GSU against the generator's other terminal cell comes up at the distribution tier; the unit substations and the GSU carried finish
 * Unit substation and GSU, the two switchgear sections set down unconnected finish MV switchgear.</li>
 * <li>{@code bfs05_recipes}: every grid the episode lays out crafts what the episode makes.</li>
 * </ul>
 */
final class Bfs05Scenes {
	private Bfs05Scenes() {
	}

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> out = new LinkedHashMap<>();
		out.put("bfs05_recipes", Bfs05Scenes::recipes);
		return out;
	}

	static Map<String, Consumer<GameTestHelper>> machineScenes() {
		Map<String, Consumer<GameTestHelper>> out = new LinkedHashMap<>();
		out.put("bfs05_unit_sub_line", Bfs05Scenes::unitSubLine);
		return out;
	}

	static Map<String, Consumer<GameTestHelper>> longScenes() {
		Map<String, Consumer<GameTestHelper>> out = new LinkedHashMap<>();
		out.put("bfs05_steam_plant", Bfs05Scenes::steamPlant);
		return out;
	}

	private static Item item(String id) {
		return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
	}

	private static boolean done(MinecraftServer server, ServerPlayer player, String quest) {
		return QuestData.get(server).of(player.getStringUUID()).done.contains("wildcatter:" + quest);
	}

	private static void earlier(MinecraftServer server, ServerPlayer player, String... quests) {
		var p = QuestData.get(server).of(player.getStringUUID());
		for (String q : quests) {
			p.done.add("wildcatter:" + q);
		}
		QuestData.get(server).changed();
	}

	private static void carry(ServerPlayer player, String id, int n) {
		player.getInventory().add(new ItemStack(item(id), n));
		QuestTracker.check(player);
	}

	private static boolean lit(GameTestHelper helper, int x, int y, int z) {
		BlockState state = helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(x, y, z)));
		return state.hasProperty(LoadBlock.LIT) && state.getValue(LoadBlock.LIT);
	}

	static final String[] EARLIER = {"wood", "stone", "campfire", "iron", "copper", "steel", "field_manual", "steam", "lamp", "protection", "firebox", "plugs",
			"seismograph", "step_up", "rig", "gas_well", "oil_well", "lease", "crude", "engine_parts", "genset", "motor", "mcc", "plc", "poles", "pole_transformer", "padmount"};

	// ---- the unit substation in the stand-in's place --------------------------------------------------------------------------------

	static final int[] PANEL = Bfs04Scenes.PANEL;      // (4, 1, 4)
	static final int[] STARTER = Bfs04Scenes.STARTER;  // (5, 1, 4)
	static final int[] DRY = Bfs04Scenes.DRY;          // (5, 1, 5)
	static final int POLE_X = Bfs04Scenes.POLE_X, POLE_Z = Bfs04Scenes.POLE_Z, FAR_X = Bfs04Scenes.FAR_X;
	/** The unit substation facing south: its main cell (the medium-voltage end) at x 8, its 480 V end at x 5 against the panel; z 4-5. */
	static final int[] UNIT = {8, 1, 5};
	/** The bare medium-voltage wire: off the unit substation's medium-voltage end, along the ground to the first pole and up its north side. */
	static final int[][] RISER = {{9, 1, 5}, {9, 1, 6}, {9, 1, 7}, {8, 1, 7}, {7, 1, 7}, {6, 1, 7}, {5, 1, 7}, {5, 2, 7}, {5, 3, 7}, {5, 4, 7}, {5, 5, 7}, {5, 6, 7},
			{5, 7, 7}, {5, 8, 7}, {5, 9, 7}};

	static String lamps(GameTestHelper helper) {
		StringBuilder b = new StringBuilder();
		for (int[] l : Bfs04Scenes.LAMPS) {
			b.append(lit(helper, l[0], l[1], l[2]) ? "lit " : "dark ");
		}
		return b.toString().trim();
	}

	static void unitSubLine(GameTestHelper helper) {
		String scene = "bfs05_unit_sub_line";
		MinecraftServer server = helper.getLevel().getServer();
		GridSceneKit.floor(helper, -24, -4, 20, 14);
		for (int x = -24; x <= 20; x++) {
			for (int z = -4; z <= 14; z++) {
				for (int y = 1; y <= 12; y++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
				}
			}
		}
		// episode 4's end state: the set on gas by hook (a test source on its engine), the starter, the stand-in and the line
		dev.wildcatter.block.GensetSkid.build(helper.getLevel(), GridSceneKit.abs(helper, PANEL[0], PANEL[1], PANEL[2]), Direction.NORTH, true);
		var src = OilfieldFollowOnScenes.source(helper, 0, 1, 3, dev.wildcatter.fluid.Product.GAS, 5.0);
		put(helper, "gas_line", 0, 2, 3);
		put(helper, "gas_line", 1, 2, 3);
		put(helper, "gas_line", 2, 2, 3);
		var panel = GridSceneKit.gensetPanel(helper, PANEL[0], PANEL[1], PANEL[2]);
		helper.setBlock(new BlockPos(STARTER[0], STARTER[1], STARTER[2]), WildcatterBlocks.MOTOR_STARTER.defaultBlockState()
				.setValue(dev.wildcatter.block.MotorStarterBlock.FACING, Direction.EAST));
		helper.setBlock(new BlockPos(DRY[0], DRY[1], DRY[2]), WildcatterBlocks.DRY_TYPE_TRANSFORMER.defaultBlockState().setValue(TransformerBlock.FACING, Direction.WEST));
		int[][] cable = {{5, 1, 6}, {5, 1, 7}, {5, 2, 7}, {5, 3, 7}, {5, 4, 7}};
		for (int[] c : cable) {
			put(helper, "single_phase_cable", c[0], c[1], c[2]);
		}
		for (int px : new int[] {POLE_X, FAR_X}) {
			GridSceneKit.place(helper, WildcatterBlocks.WOOD_POLE_30, px, 1, POLE_Z, Direction.NORTH);
			BlockPos top = GridSceneKit.abs(helper, px, 9, POLE_Z);
			InsulatorBlock ins = (InsulatorBlock) WildcatterBlocks.INSULATOR_DIST;
			helper.getLevel().setBlock(top, ins.defaultBlockState().setValue(InsulatorBlock.MOUNT, ins.mountAt(helper.getLevel(), top))
					.setValue(dev.wildcatter.block.CableBlock.CONDUCTOR, Conductor.A), 3);
			helper.getLevel().setBlock(GridSceneKit.abs(helper, px, 5, POLE_Z - 1), WildcatterBlocks.POLE_TRANSFORMER_MV.defaultBlockState().setValue(TransformerBlock.FACING, Direction.NORTH), 3);
		}
		int[][] down = {{FAR_X, 4, 7}, {FAR_X, 3, 7}, {FAR_X, 2, 7}, {FAR_X, 1, 7}, {FAR_X + 1, 1, 7}, {FAR_X + 2, 1, 7}, {FAR_X + 3, 1, 7}, {FAR_X + 4, 1, 7}};
		for (int[] c : down) {
			put(helper, "single_phase_cable", c[0], c[1], c[2]);
		}
		for (int[] l : Bfs04Scenes.LAMPS) {
			put(helper, "electric_lamp", l[0], l[1], l[2]);
		}
		BlockPos near = GridSceneKit.abs(helper, POLE_X, 9, POLE_Z), far = GridSceneKit.abs(helper, FAR_X, 9, POLE_Z);
		check(helper, SpoolItem.connect(helper.getLevel(), near, far, Voltage.MEDIUM, false, 1024, ConductorSize.ACSR_SPARROW).ok(), "the span should string");
		check(helper, SpoolItem.connectLead(helper.getLevel(), near, GridSceneKit.abs(helper, POLE_X, 5, POLE_Z - 1), Voltage.MEDIUM, false, 64, ConductorSize.ACSR_SPARROW).ok()
				&& SpoolItem.connectLead(helper.getLevel(), far, GridSceneKit.abs(helper, FAR_X, 5, POLE_Z - 1), Voltage.MEDIUM, false, 64, ConductorSize.ACSR_SPARROW).ok(), "the leads should string");
		ServerPlayer player = QuestScenes.player(helper, -4, 1, 12);
		earlier(server, player, EARLIER);
		helper.runAfterDelay(30, () -> panel.setWantRun(true));
		helper.runAfterDelay(330, () -> {
			String before = lamps(helper);
			log(scene, "episode 4's stand-in | set " + read(helper, PANEL[0], PANEL[1], PANEL[2]) + " | dry-type " + read(helper, DRY[0], DRY[1], DRY[2]) + " | step-up unit "
					+ read(helper, POLE_X, 5, POLE_Z - 1) + " | lamps " + before);
			check(helper, before.equals("lit lit"), "episode 4's line should light the far lamps: " + before + " | gas " + src.taken);
			// the swap, the set stopped first (no one works on a line while it carries power)
			panel.setWantRun(false);
		});
		helper.runAfterDelay(400, () -> {
			check(helper, lamps(helper).equals("dark dark"), "the set stopped, the line should be dead: " + lamps(helper));
			for (int[] c : new int[][] {STARTER, DRY}) {
				helper.getLevel().destroyBlock(GridSceneKit.abs(helper, c[0], c[1], c[2]), false);
			}
			for (int[] c : cable) {
				helper.getLevel().destroyBlock(GridSceneKit.abs(helper, c[0], c[1], c[2]), false);
			}
			helper.getLevel().destroyBlock(GridSceneKit.abs(helper, POLE_X, 5, POLE_Z - 1), false);
			GridSceneKit.place(helper, WildcatterBlocks.UNIT_SUBSTATION, UNIT[0], UNIT[1], UNIT[2], Direction.SOUTH);
			StringBuilder cells = new StringBuilder();
			for (BlockPos c : BigParts.cellsAt(helper.getLevel(), GridSceneKit.abs(helper, UNIT[0], UNIT[1], UNIT[2]))) {
				cells.append(helper.relativePos(c).toShortString()).append("; ");
			}
			log(scene, "unit substation cells " + cells);
			for (int[] w : RISER) {
				put(helper, "mv_wire_hot", w[0], w[1], w[2]);
			}
			log(scene, "near insulator after the swap: " + read(helper, POLE_X, 9, POLE_Z));
			CircuitManager.markDirty(helper.getLevel());
		});
		helper.runAfterDelay(440, () -> panel.setWantRun(true));
		helper.runAfterDelay(760, () -> {
			String after = lamps(helper);
			String unit = read(helper, UNIT[0], UNIT[1], UNIT[2]);
			String riser = read(helper, 5, 9, 7);
			String ins = read(helper, POLE_X, 9, POLE_Z);
			String farIns = read(helper, FAR_X, 9, POLE_Z);
			log(scene, "the unit substation | set " + read(helper, PANEL[0], PANEL[1], PANEL[2]) + " | unit " + unit + " | riser " + riser + " | near insulator " + ins
					+ " | far insulator " + farIns + " | far unit " + read(helper, FAR_X, 5, POLE_Z - 1) + " | lamps " + after);
			check(helper, after.equals("lit lit"), "the unit substation alone should light the far lamps again: " + after);
			check(helper, farIns.contains(Voltage.MEDIUM.label()), "the far insulator should carry medium voltage: " + farIns);
			log(scene, "ok: the unit substation steps the set straight up to the line | " + unit);
			QuestScenes.leave(helper, player);
			helper.succeed();
		});
	}

	// ---- the steam plant from cold on its starting set ---------------------------------------------------------------------------------

	/** The generator (the plant's controller); the plant faces north, its parts at the layout's places round it (SteamPlantLayout). */
	static final int GX = 8, GZ = 4;

	static void steamPlant(GameTestHelper helper) {
		String scene = "bfs05_steam_plant";
		MinecraftServer server = helper.getLevel().getServer();
		GridSceneKit.floor(helper, -6, -4, 20, 14);
		for (int x = -6; x <= 20; x++) {
			for (int z = -4; z <= 14; z++) {
				for (int y = 1; y <= 8; y++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
				}
			}
		}
		BlockPos gen = GridSceneKit.abs(helper, GX, 1, GZ);
		dev.wildcatter.block.SteamPlantLayout.layout().build(helper.getLevel(), gen, Direction.NORTH, 0);
		var plant = (dev.wildcatter.block.TurbineGeneratorBlockEntity) helper.getLevel().getBlockEntity(gen);
		// the starting set south of the feed pump (its main cell at GX, GZ + 2): the dry-type, front (480 V) north to the box; the turbine
		// against its back; the boiler on a firebox set in the ground west of the turbine; the water pump east of it, in water, piped on
		int fz = GZ + 3;
		helper.setBlock(new BlockPos(GX, 1, fz), WildcatterBlocks.DRY_TYPE_TRANSFORMER.defaultBlockState().setValue(TransformerBlock.FACING, Direction.NORTH));
		helper.setBlock(new BlockPos(GX - 1, 0, fz + 1), WildcatterBlocks.FIREBOX);
		helper.setBlock(new BlockPos(GX - 1, 1, fz + 1), WildcatterBlocks.BOILER);
		helper.setBlock(new BlockPos(GX + 1, 0, fz + 1), Blocks.WATER);
		helper.setBlock(new BlockPos(GX + 1, 1, fz + 1), WildcatterBlocks.PUMP);
		put(helper, "pipe", GX + 1, 1, fz);
		// the GSU against the generator's other terminal cell (west face, z GZ + 1), facing west: its line side away from the plant
		GridSceneKit.place(helper, WildcatterBlocks.GSU_TRANSFORMER, GX - 1, 1, GZ + 1, Direction.WEST);
		// the switchgear lineup set down unconnected west of it, facing north: the main, then a feeder to its east
		GridSceneKit.place(helper, WildcatterBlocks.MV_SWITCHGEAR_MAIN, GX - 6, 1, GZ + 6, Direction.NORTH);
		GridSceneKit.place(helper, WildcatterBlocks.MV_SWITCHGEAR_FEEDER, GX - 5, 1, GZ + 6, Direction.NORTH);
		ServerPlayer player = QuestScenes.player(helper, GX + 6, 1, GZ + 8);
		earlier(server, player, EARLIER);
		carry(player, "wildcatter:turbine_generator", 1);
		player.getInventory().clearContent();
		long[] litAt = {-1};
		int[] step = {0};
		helper.runAfterDelay(40, () -> {
			StringBuilder cells = new StringBuilder();
			for (BlockPos c : BigParts.cellsAt(helper.getLevel(), GridSceneKit.abs(helper, GX - 1, 1, GZ + 1))) {
				cells.append(helper.relativePos(c).toShortString()).append("; ");
			}
			log(scene, "formed " + plant.formed() + " | " + read(helper, GX, 1, GZ) + " | GSU cells " + cells);
			check(helper, plant.formed(), "the plant should form: " + read(helper, GX, 1, GZ));
			check(helper, done(server, player, "steam_plant"), "the generator carried and the plant formed should finish The steam plant: "
					+ QuestData.get(server).of(player.getStringUUID()).done);
			// coal in the burner (its main cell at GX - 1, GZ - 3) and lit, the starting set still cold: the mistake
			((dev.wildcatter.block.SteamPlantPartBlockEntity) helper.getLevel().getBlockEntity(GridSceneKit.abs(helper, GX - 1, 1, GZ - 3))).insertFuel(new ItemStack(Items.COAL, 32));
			plant.pushButton();
		});
		for (int t = 100; t <= 23000; t += 100) {
			final int at = t;
			helper.runAfterDelay(t, () -> {
				String g = read(helper, GX, 1, GZ);
				if (at % 1000 == 0 || step[0] == 0 && at == 600) {
					log(scene, at + " | plant " + g + " | feed pump " + read(helper, GX, 1, GZ + 2) + " | dry-type " + read(helper, GX, 1, fz) + " | turbine " + read(helper, GX, 1, fz + 1)
							+ " | water pump " + read(helper, GX + 1, 1, fz + 1));
				}
				switch (step[0]) {
					case 0 -> {
						if (at >= 600) {
							check(helper, g.contains("FIRING") && g.contains("drum water 0 kg"), "lit with the starting set cold, the burner should fire with the drum dry: " + g);
							log(scene, "the mistake | " + g + " | feed pump " + read(helper, GX, 1, GZ + 2));
							// the fix: the starting set lit (coal in the firebox, four buckets in the boiler)
							GridSceneKit.entity(helper, GX - 1, 0, fz + 1, dev.wildcatter.block.FireboxBlockEntity.class).setItem(0, new ItemStack(Items.COAL, 32));
							GridSceneKit.entity(helper, GX - 1, 1, fz + 1, dev.wildcatter.block.BoilerBlockEntity.class).addWater(4000);
							helper.setBlock(new BlockPos(GX, 1, fz + 1), WildcatterBlocks.STEAM_TURBINE);
							CircuitManager.markDirty(helper.getLevel());
							litAt[0] = at;
							step[0] = 1;
						}
					}
					case 1 -> {
						if (g.contains("RUNNING")) {
							log(scene, "running after " + (at - litAt[0]) + " ticks from the starting set | " + g + " | GSU " + read(helper, GX - 1, 1, GZ + 1));
							check(helper, g.contains("breaker CLOSED") || g.contains("CLOSED"), "the plant should close its breaker: " + g);
							step[0] = 2;
						}
					}
					case 2 -> {
						String gsu = read(helper, GX - 1, 1, GZ + 1);
						String main = read(helper, GX - 6, 1, GZ + 6);
						log(scene, "GSU " + gsu + " | switchgear main " + main + " | feeder " + read(helper, GX - 5, 1, GZ + 6));
						carry(player, "wildcatter:unit_substation", 1);
						carry(player, "wildcatter:gsu_transformer", 1);
						check(helper, done(server, player, "substation"), "a GSU and a unit substation carried should finish Unit substation and GSU");
						carry(player, "wildcatter:mv_switchgear_main", 1);
						carry(player, "wildcatter:mv_switchgear_feeder", 1);
						check(helper, done(server, player, "switchgear"), "a main and a feeder section should finish MV switchgear");
						log(scene, "ok: quests " + QuestData.get(server).of(player.getStringUUID()).done);
						step[0] = 99;
						QuestScenes.leave(helper, player);
						helper.succeed();
					}
					default -> {
					}
				}
				if (at == 23000 && step[0] < 99) {
					check(helper, false, "the plant should be running by now: " + g);
				}
			});
		}
	}

	// ---- recipes ------------------------------------------------------------------------------------------------------------------------

	static void recipes(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		String st = "wildcatter:steel_ingot";
		Object[][] cases = {
				{"wildcatter:unit_substation", Bfs04Scenes.grid(new String[] {"sds", "tct", "sds"}, 's', st, 'd', Items.DIAMOND, 't', "wildcatter:pole_transformer_mv", 'c', "minecraft:copper_block")},
				{"wildcatter:gsu_transformer", Bfs04Scenes.grid(new String[] {"sds", "tct", "sds"}, 's', st, 'd', Items.DIAMOND, 't', "wildcatter:unit_substation", 'c', "minecraft:copper_block")},
				{"wildcatter:mv_switchgear_main", Bfs04Scenes.grid(new String[] {"sgs", "srs", "scs"}, 's', st, 'g', Items.GLASS_PANE, 'r', Items.COMPARATOR, 'c', "minecraft:copper_block")},
				{"wildcatter:mv_switchgear_feeder", Bfs04Scenes.grid(new String[] {"sgs", "srs", "scs"}, 's', st, 'g', Items.GLASS_PANE, 'r', Items.COMPARATOR, 'c', Items.COPPER_INGOT)},
				{"wildcatter:turbine_generator", Bfs04Scenes.grid(new String[] {"sws", "wrw", "pxp"}, 's', st, 'w', "wildcatter:stator_winding", 'r', "wildcatter:rotor_field",
						'x', "wildcatter:exciter", 'p', "wildcatter:gas_line")},
				{"wildcatter:boiler_burner", Bfs04Scenes.grid(new String[] {"sbs", "bfb", "sbs"}, 's', st, 'b', Items.BRICKS, 'f', Items.BLAST_FURNACE)},
				{"wildcatter:boiler_drum", Bfs04Scenes.grid(new String[] {"sss", "sws", "sss"}, 's', st, 'w', Items.WATER_BUCKET)},
				{"wildcatter:superheater", Bfs04Scenes.grid(new String[] {"scs", "csc", "scs"}, 's', st, 'c', Items.COPPER_INGOT)},
				{"wildcatter:turbine_casing", Bfs04Scenes.grid(new String[] {"sss", "bcb", "sss"}, 's', st, 'b', Items.IRON_BARS, 'c', "wildcatter:crankshaft")},
				{"wildcatter:condenser", Bfs04Scenes.grid(new String[] {"sps", "pcp", "sps"}, 's', st, 'p', "wildcatter:pipe", 'c', "minecraft:copper_block")},
				{"wildcatter:feed_pump", Bfs04Scenes.grid(new String[] {"sps", "mcm"}, 's', st, 'p', "wildcatter:pump", 'm', Items.PISTON, 'c', Items.COPPER_INGOT)},
				{"wildcatter:stator_winding", Bfs04Scenes.grid(new String[] {"ccc", "csc", "ccc"}, 'c', Items.COPPER_INGOT, 's', st)},
				{"wildcatter:pole_transformer_mv", Bfs04Scenes.grid(new String[] {"ici", "ici", "ici"}, 'i', Items.IRON_INGOT, 'c', Items.COPPER_INGOT)},
		};
		StringBuilder ok = new StringBuilder();
		for (Object[] c : cases) {
			var input = (net.minecraft.world.item.crafting.CraftingInput) c[1];
			var recipe = server.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, helper.getLevel());
			ItemStack out = recipe.map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
			check(helper, out.is(item((String) c[0])), "the episode's grid for " + c[0] + " should craft it, not " + out);
			ok.append(c[0].toString().substring(11)).append(' ');
		}
		log("bfs05_recipes", "ok: " + ok);
		helper.succeed();
	}
}
