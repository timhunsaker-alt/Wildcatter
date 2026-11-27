package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.check;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.put;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.BoilerBlockEntity;
import dev.wildcatter.block.FireboxBlockEntity;
import dev.wildcatter.block.SeparatorBlockEntity;
import dev.wildcatter.block.StorageTankBlockEntity;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.item.SeismographItem;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Built From Scratch, episode 3 "Strike oil" (the step-up quest and the Oil and gas chapter, {@code wildcatter_quests/oil_and_gas.json}):
 * the proof that what the episode films works in normal play. The step-up, the oil well and the strike have their scenes already
 * (dry_type_runs_rig, quest_pumpjack_milestone, quest_well_struck_milestone); these add the rest:
 * <ul>
 * <li>{@code bfs03_seismograph}: the survey the episode films, read the way the episode reads it. Away from any field it hears the nearest
 * one and says which way it lies and how far to its edge, strong under 24 blocks, fair under 56, faint beyond; over a shallow field, a
 * strong echo with its size and the top's height; over a deep field, a deep echo that says it needs a land rig. Carrying the seismograph
 * finishes Find a field.</li>
 * <li>{@code bfs03_lease}: the episode's oil lease on the episode's power: a turbine on a firebox boiler into the dry-type's low side, its
 * high side against the pumpjack's terminal box, the pumpjack over an oil wellhead. Piped straight to a stock tank, the well fills nothing:
 * the tank takes no raw well fluid. Through a separator with no flare, the separator fills with gas and shuts in (the episode's "what goes
 * wrong"); a flare on its gas side, it separates again and crude reaches the tank. The lease finishes on its four parts carried, and A
 * stock tank of crude on the tank and a bucket of crude filled at it.</li>
 * <li>{@code bfs03_pumpjack_on_wellhead}: a player holding a pumpjack right-clicks an oil wellhead, as the strike's message says to (no
 * sneaking): the pumpjack is set over it, its horsehead over the well and its front the way the player faces. Before the fix the wellhead
 * took the click for its readout ("no pumpjack over it. Click it with a pumpjack") and nothing was placed.</li>
 * <li>{@code bfs03_recipes}: every grid the episode lays out crafts what the episode makes, as the crafting table asks the server: the
 * dry-type, the seismograph, the pistons and the rig, steel pipe, the pumpjack, the lease's parts and the stock tank. The stock tank's
 * old grid (eight steel round an empty middle) was also the dry storage canister's, the fuel tank's and the 48Y cylinder's, and gave a
 * canister.</li>
 * <li>{@code bfs03_big_part_grounded}: a ground rod against a cell of the rig, and of the pumpjack, that is not its main cell (and not
 * beside it) grounds the machine: its readout says grounded, where before the fix it said UNGROUNDED (the search began only at the main
 * cell, as the episode's rod against the rig's skid and the pumpjack's terminal box showed).</li>
 * </ul>
 */
final class Bfs03Scenes {
	private Bfs03Scenes() {
	}

	/** Quick scenes (QUEST_SCENES). */
	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> out = new LinkedHashMap<>();
		out.put("bfs03_seismograph", Bfs03Scenes::seismograph);
		out.put("bfs03_pumpjack_on_wellhead", Bfs03Scenes::pumpjackOnWellhead);
		out.put("bfs03_recipes", Bfs03Scenes::recipes);
		out.put("bfs03_big_part_grounded", Bfs03Scenes::bigPartGrounded);
		return out;
	}

	/** Scenes with a running plant (QUEST_MACHINE_SCENES). */
	static Map<String, Consumer<GameTestHelper>> machineScenes() {
		Map<String, Consumer<GameTestHelper>> out = new LinkedHashMap<>();
		out.put("bfs03_lease", Bfs03Scenes::lease);
		return out;
	}

	private static boolean done(MinecraftServer server, ServerPlayer player, String quest) {
		return QuestData.get(server).of(player.getStringUUID()).done.contains("wildcatter:" + quest);
	}

	/** Marks quests done as the earlier episodes left them, so the ones under test are open. */
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

	// ---- the seismograph --------------------------------------------------------------------------------------------------------------

	/**
	 * A seed whose shallow field in the cell at (x, z) is the nearest field 20 blocks west of its edge, and whose deep field in the deep
	 * cell there has a column no shallow field covers. The points are far from every other scene, so its seed region overlaps none.
	 */
	private static long surveySeed(int x, int z, int[] out) {
		for (long s = 1; s < 400_000; s++) {
			Reservoir f = Reservoir.inCell(s, Math.floorDiv(x, Tuning.FIELD_CELL), Math.floorDiv(z, Tuning.FIELD_CELL));
			Reservoir d = Reservoir.deepInCell(s, Math.floorDiv(x, Tuning.DEEP_FIELD_CELL), Math.floorDiv(z, Tuning.DEEP_FIELD_CELL));
			if (f == null || d == null) {
				continue;
			}
			int ox = (int) Math.floor(f.centerX() - f.radius() - 20), oz = (int) Math.floor(f.centerZ());
			Reservoir near = Reservoir.nearest(s, ox + 0.5, oz + 0.5, Tuning.SURVEY_RANGE, dome -> false);
			if (near == null || !near.key().equals(f.key()) || Reservoir.at(s, ox, oz, dome -> false) != null) {
				continue;
			}
			int dx = (int) Math.floor(d.centerX()), dz = (int) Math.floor(d.centerZ());
			Reservoir under = Reservoir.at(s, dx, dz, dome -> false);
			if (under == null || !under.deep()) {
				continue;
			}
			out[0] = (int) Math.floor(f.centerX());
			out[1] = (int) Math.floor(f.centerZ());
			out[2] = ox;
			out[3] = oz;
			out[4] = dx;
			out[5] = dz;
			return s;
		}
		return -1;
	}

	static void seismograph(GameTestHelper helper) {
		String scene = "bfs03_seismograph";
		MinecraftServer server = helper.getLevel().getServer();
		BlockPos o = helper.absolutePos(BlockPos.ZERO);
		// a survey far out along x (no chunk is needed: the report is the seed's arithmetic), clear of every other scene's region
		int x = o.getX() + 60_000, z = o.getZ() + 60_000;
		int[] p = new int[6];
		long seed = surveySeed(x, z, p);
		check(helper, seed > 0, "no seed puts a shallow field and a deep field where the survey needs them");
		ReservoirData.TestRegion region = new ReservoirData.TestRegion(x - 400, z - 400, x + 400, z + 400, seed);
		ReservoirData.TEST_REGIONS.add(region);
		try {
			Reservoir field = ReservoirData.field(helper.getLevel(), p[0], p[1]);
			check(helper, field != null && !field.deep(), "the shallow field should be under its centre");
			String over = SeismographItem.report(helper.getLevel(), p[0] + 0.5, p[1] + 0.5);
			check(helper, over.startsWith("STRONG ECHO: you are standing over a field about " + Math.round(field.radius() * 2) + " blocks across")
					&& over.contains("Its top is at y=" + field.topY()) && over.contains("Worth a drill here"), "over a shallow field: a strong echo, its size and its top: " + over);
			String away = SeismographItem.report(helper.getLevel(), p[2] + 0.5, p[3] + 0.5);
			double edge = field.edgeDistance(p[2] + 0.5, p[3] + 0.5);
			String strength = edge < 24 ? "Strong" : edge < 56 ? "Fair" : "Faint";
			check(helper, away.startsWith(strength + " echo from the east, about " + Math.round(edge) + " blocks to its edge. Walk that way and thump again."),
					"20 blocks west of its edge: the field lies east, " + strength.toLowerCase() + " (" + Math.round(edge) + " blocks): " + away);
			String deep = SeismographItem.report(helper.getLevel(), p[4] + 0.5, p[5] + 0.5);
			check(helper, deep.startsWith("DEEP ECHO: you are standing over a deep field") && deep.contains("too deep for the small rig: it needs a land rig"),
					"over a deep field: a deep echo that needs a land rig: " + deep);
			// the bands: strong under 24 blocks, fair under 56, faint beyond, read along a line walked west from the field
			StringBuilder walked = new StringBuilder();
			for (int k = 5; k <= 85; k += 20) {
				int wx = (int) Math.floor(field.centerX() - field.radius() - k);
				if (!field.key().equals(java.util.Optional.ofNullable(Reservoir.nearest(seed, wx + 0.5, p[1] + 0.5, Tuning.SURVEY_RANGE, d -> false)).map(Reservoir::key).orElse(""))) {
					continue;   // another field nearer this far out: its own reading
				}
				String r = SeismographItem.report(helper.getLevel(), wx + 0.5, p[1] + 0.5);
				double e = field.edgeDistance(wx + 0.5, p[1] + 0.5);
				String band = e < 24 ? "Strong" : e < 56 ? "Fair" : "Faint";
				check(helper, r.startsWith(band + " echo from the east"), "at " + Math.round(e) + " blocks the echo should be " + band.toLowerCase() + ": " + r);
				walked.append(Math.round(e)).append(" blocks: ").append(band).append("; ");
			}
			// Find a field: carrying a seismograph (steel and Protect the circuit done before it)
			ServerPlayer player = QuestScenes.player(helper, 1, 1, 1);
			earlier(server, player, "wood", "stone", "campfire", "iron", "copper", "steel", "field_manual", "steam", "lamp", "protection", "firebox", "plugs");
			QuestTracker.check(player);
			check(helper, !done(server, player, "seismograph"), "Find a field should wait for a seismograph");
			player.getInventory().add(new ItemStack(item("wildcatter:seismograph")));
			QuestTracker.check(player);
			check(helper, done(server, player, "seismograph"), "carrying a seismograph should finish Find a field");
			check(helper, !done(server, player, "rig"), "The drilling rig should wait for its pistons, the rig and Step up");
			log(scene, "ok: seed " + seed + " | over: " + over + " | away: " + away + " | deep: " + deep + " | walked " + walked);
			QuestScenes.leave(helper, player);
		} finally {
			ReservoirData.TEST_REGIONS.remove(region);
		}
		helper.succeed();
	}

	// ---- the episode's recipes -----------------------------------------------------------------------------------------------------------

	/** A 3 x 3 grid from rows of keys ({@code ' '} empty). */
	private static net.minecraft.world.item.crafting.CraftingInput grid(String[] rows, Object... kv) {
		Map<Character, Item> key = new java.util.HashMap<>();
		for (int i = 0; i < kv.length; i += 2) {
			key.put((Character) kv[i], kv[i + 1] instanceof Item it ? it : item((String) kv[i + 1]));
		}
		List<ItemStack> cells = new java.util.ArrayList<>();
		for (String r : rows) {
			for (char c : String.format("%-3s", r).toCharArray()) {
				cells.add(c == ' ' ? ItemStack.EMPTY : new ItemStack(key.get(c)));
			}
		}
		while (cells.size() < 9) {
			cells.add(ItemStack.EMPTY);
		}
		return net.minecraft.world.item.crafting.CraftingInput.of(3, 3, cells);
	}

	static void recipes(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		String st = "wildcatter:steel_ingot", pipe = "wildcatter:gas_line";
		Object[][] cases = {
				{"wildcatter:dry_type_transformer", grid(new String[] {"sws", "cic", "sws"}, 's', st, 'w', "wildcatter:wool_wire_hot", 'c', "minecraft:copper_block", 'i', Items.IRON_BLOCK)},
				{"wildcatter:seismograph", grid(new String[] {"ipi", "rcr", "iii"}, 'i', Items.IRON_INGOT, 'p', Items.PAPER, 'r', Items.REDSTONE, 'c', Items.COMPASS)},
				{"wildcatter:piston", grid(new String[] {"ssn"}, 's', st, 'n', Items.IRON_NUGGET)},
				{"wildcatter:drilling_rig", grid(new String[] {"ipi", "cmc", "ibi"}, 'i', Items.IRON_INGOT, 'p', Items.IRON_PICKAXE, 'c', Items.COPPER_INGOT, 'm', Items.PISTON, 'b', Items.IRON_BLOCK)},
				{"wildcatter:gas_line", grid(new String[] {"sss", "   ", "sss"}, 's', st)},
				{"wildcatter:pumpjack", grid(new String[] {"iii", "cpc", "iri"}, 'i', Items.IRON_INGOT, 'c', Items.COPPER_INGOT, 'p', Items.PISTON, 'r', Items.REDSTONE)},
				{"wildcatter:separator", grid(new String[] {"sss", "pgp", "sss"}, 's', st, 'p', pipe, 'g', Items.GLASS)},
				{"wildcatter:flare_stack", grid(new String[] {" f ", " p ", " p "}, 'f', Items.FLINT_AND_STEEL, 'p', pipe)},
				{"wildcatter:crankshaft", grid(new String[] {"s  ", " s ", "  s"}, 's', st)},
				{"wildcatter:injection_pump", grid(new String[] {"sps", "cxc", "sss"}, 's', st, 'p', pipe, 'c', Items.COPPER_INGOT, 'x', "wildcatter:crankshaft")},
				{"wildcatter:disposal_wellhead", grid(new String[] {"sps", " s ", "sbs"}, 's', st, 'p', pipe, 'b', Items.IRON_BLOCK)},
				{"wildcatter:stock_tank", grid(new String[] {"sss", "sls", "sss"}, 's', st, 'l', Items.LADDER)},
		};
		StringBuilder ok = new StringBuilder();
		for (Object[] c : cases) {
			var input = (net.minecraft.world.item.crafting.CraftingInput) c[1];
			var recipe = server.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, helper.getLevel());
			ItemStack out = recipe.map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
			check(helper, out.is(item((String) c[0])), "the episode's grid for " + c[0] + " should craft it, not " + out);
			ok.append(c[0].toString().substring(11)).append(' ');
		}
		// the old stock-tank grid, eight steel round an empty middle, is not a stock tank any more
		var old = grid(new String[] {"sss", "s s", "sss"}, 's', st);
		var oldRecipe = server.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, old, helper.getLevel());
		check(helper, oldRecipe.map(r -> !r.value().assemble(old).is(item("wildcatter:stock_tank"))).orElse(true), "eight steel round an empty middle should not give a stock tank");
		log("bfs03_recipes", "ok: " + ok);
		helper.succeed();
	}

	// ---- a rod against any cell of a big part grounds it ---------------------------------------------------------------------------------

	static void bigPartGrounded(GameTestHelper helper) {
		String scene = "bfs03_big_part_grounded";
		GridSceneKit.floor(helper, -18, -10, 18, 10);
		for (int x = -18; x <= 18; x++) {
			for (int z = -10; z <= 10; z++) {
				for (int y = 1; y <= 14; y++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
				}
			}
		}
		StringBuilder ok = new StringBuilder();
		for (Block machine : List.of(WildcatterBlocks.DRILLING_RIG, WildcatterBlocks.PUMPJACK)) {
			int mx = machine == WildcatterBlocks.DRILLING_RIG ? -10 : 4;
			GridSceneKit.place(helper, machine, mx, 1, 0, Direction.WEST);
			BlockPos main = helper.absolutePos(new BlockPos(mx, 1, 0));
			List<BlockPos> cells = dev.wildcatter.multiblock.BigParts.cells(main, helper.getLevel().getBlockState(main));
			String before = GridSceneKit.read(helper, mx, 1, 0);
			check(helper, before.contains("UNGROUNDED"), machine + " with no rod should read UNGROUNDED: " + before);
			// a free spot on the floor against a ground-level cell, as far from the main cell as the part goes
			BlockPos rod = null, against = null;
			for (BlockPos cell : cells) {
				if (cell.getY() != main.getY() || cell.equals(main)) {
					continue;
				}
				for (Direction side : Direction.Plane.HORIZONTAL) {
					BlockPos spot = cell.relative(side);
					if (cells.contains(spot) || spot.distManhattan(main) <= 1 || !helper.getLevel().getBlockState(spot).isAir()) {
						continue;
					}
					if (rod == null || spot.distManhattan(main) > rod.distManhattan(main)) {
						rod = spot;
						against = cell;
					}
				}
			}
			check(helper, rod != null, machine + " should have a free spot against a cell other than its main one");
			helper.getLevel().setBlock(rod, WildcatterBlocks.GROUND_ROD.defaultBlockState(), 3);
			String after = GridSceneKit.read(helper, mx, 1, 0);
			check(helper, after.endsWith("grounded") && !after.contains("UNGROUNDED"),
					machine + " with a rod at " + helper.relativePos(rod) + " against its cell " + helper.relativePos(against) + " should read grounded: " + after);
			check(helper, dev.wildcatter.safety.Grounding.isGrounded(helper.getLevel(), against), "asked at the cell the rod touches, it is grounded too");
			ok.append(machine.getDescriptionId()).append(" rod ").append(helper.relativePos(rod)).append(" against ").append(helper.relativePos(against))
					.append(" (main ").append(helper.relativePos(main)).append(", ").append(cells.size()).append(" cells); ");
		}
		log(scene, "ok: " + ok);
		helper.succeed();
	}

	// ---- the pumpjack clicked on its wellhead ----------------------------------------------------------------------------------------------

	static void pumpjackOnWellhead(GameTestHelper helper) {
		String scene = "bfs03_pumpjack_on_wellhead";
		GridSceneKit.floor(helper, -2, -3, 9, 3);
		for (int x = -2; x <= 9; x++) {
			for (int z = -3; z <= 3; z++) {
				for (int y = 1; y <= 7; y++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
				}
			}
		}
		put(helper, "oil_wellhead", 0, 1, 0);
		ServerPlayer player = QuestScenes.player(helper, 4, 1, 1);
		BlockPos well = helper.absolutePos(new BlockPos(0, 1, 0));
		BlockPos eye = helper.absolutePos(new BlockPos(4, 1, 1));
		// standing east of it, looking west at its top, a pumpjack in hand
		player.snapTo(eye.getX() + 0.5, eye.getY(), eye.getZ() + 0.5, 90F, 30F);
		ItemStack jack = new ItemStack(item("wildcatter:pumpjack"));
		player.setItemInHand(InteractionHand.MAIN_HAND, jack);
		net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(well).add(0, 0.5, 0),
				Direction.UP, well, false);
		net.minecraft.world.InteractionResult r = player.gameMode.useItemOn(player, helper.getLevel(), jack, InteractionHand.MAIN_HAND, hit);
		net.minecraft.world.level.block.state.BlockState main = helper.getLevel().getBlockState(well.east());
		check(helper, main.getBlock() == WildcatterBlocks.PUMPJACK, "a pumpjack clicked on an oil wellhead should be set over it: " + r + ", east of it " + main
				+ "; the wellhead says " + GridSceneKit.read(helper, 0, 1, 0));
		check(helper, main.getValue(dev.wildcatter.block.PumpjackBlock.FACING) == Direction.WEST, "its front the way the player faces (west): " + main);
		String read = GridSceneKit.read(helper, 1, 1, 0);
		check(helper, !read.contains("NOT OVER A WELL"), "its horsehead should be over the well: " + read);
		log(scene, "ok: " + r + " | " + read);
		QuestScenes.leave(helper, player);
		helper.succeed();
	}

	// ---- the lease -----------------------------------------------------------------------------------------------------------------------

	/** A seed with an oil field under this absolute column. */
	private static long oilSeed(BlockPos well) {
		for (long s = 1; s < 3_000_000; s++) {
			Reservoir f = Reservoir.at(s, well.getX(), well.getZ(), d -> false);
			if (f != null && f.units() > 0 && f.kind() == Reservoir.Kind.OIL && !f.deep()) {
				return s;
			}
		}
		return -1;
	}

	/**
	 * The episode's lease, relative to the wellhead at (0, 1, 0) (as the film's oil well at -677, 74, 864): the pumpjack facing west over it,
	 * its terminal box at (6, 1, 0); the dry-type at (7, 1, 0) with its high side (front) west against the box; the turbine at (8, 1, 0) on
	 * its low side; the boiler at (9, 1, 0) on a firebox set into the floor. West of the well: the separator facing east, its cells x -6..-3,
	 * z -1..0; the stock tank's cells x -12..-9, z 2..5; the flare at (-6, 1, -4).
	 */
	static void lease(GameTestHelper helper) {
		String scene = "bfs03_lease";
		MinecraftServer server = helper.getLevel().getServer();
		QuestBook book = QuestTracker.book(server);
		QuestBook.Chapter chapter = book.chapter(Identifier.parse("wildcatter:oil_and_gas"));
		List<String> ids = book.quests().values().stream().filter(q -> q.chapter().equals(chapter.id())).map(q -> q.id().getPath()).toList();
		check(helper, ids.containsAll(List.of("seismograph", "rig", "oil_well", "gas_well", "lease", "crude")), "the chapter should hold the episode's six quests: " + ids);
		BlockPos wellAbs = helper.absolutePos(new BlockPos(0, 1, 0));
		// Fields keep clear of their cell's edges, so no seed puts one under a column in a cell's corner (about one test place in seven).
		// The region reads its fields over the middle of the well's cell instead, wherever the test lands.
		int dx = Math.floorDiv(wellAbs.getX(), Tuning.FIELD_CELL) * Tuning.FIELD_CELL + Tuning.FIELD_CELL / 2 - wellAbs.getX();
		int dz = Math.floorDiv(wellAbs.getZ(), Tuning.FIELD_CELL) * Tuning.FIELD_CELL + Tuning.FIELD_CELL / 2 - wellAbs.getZ();
		long seed = oilSeed(wellAbs.offset(dx, 0, dz));
		check(helper, seed > 0, "no seed puts an oil field under the well");
		ReservoirData.TestRegion region = new ReservoirData.TestRegion(wellAbs.getX() - 16, wellAbs.getZ() - 10, wellAbs.getX() + 16, wellAbs.getZ() + 10, seed, dx, dz);
		ReservoirData.TEST_REGIONS.add(region);
		GridSceneKit.floor(helper, -14, -7, 12, 7);
		put(helper, "oil_wellhead", 0, 1, 0);
		GridSceneKit.place(helper, WildcatterBlocks.PUMPJACK, 1, 1, 0, Direction.WEST);
		helper.setBlock(new BlockPos(7, 1, 0), WildcatterBlocks.DRY_TYPE_TRANSFORMER.defaultBlockState().setValue(dev.wildcatter.block.TransformerBlock.FACING, Direction.WEST));
		helper.setBlock(new BlockPos(8, 1, 0), WildcatterBlocks.STEAM_TURBINE);
		helper.setBlock(new BlockPos(9, 0, 0), WildcatterBlocks.FIREBOX);
		GridSceneKit.entity(helper, 9, 0, 0, FireboxBlockEntity.class).setItem(0, new ItemStack(Items.COAL, 32));
		GridSceneKit.place(helper, WildcatterBlocks.BOILER, 9, 1, 0, Direction.NORTH);
		BoilerBlockEntity boiler = GridSceneKit.entity(helper, 9, 1, 0, BoilerBlockEntity.class);
		for (int i = 0; i < 4; i++) {
			boiler.addWater(Tuning.WATER_PER_BUCKET);
		}
		// first, the well piped straight to a stock tank
		for (int x = -1; x >= -8; x--) {
			put(helper, "gas_line", x, 1, 0);
		}
		put(helper, "gas_line", -8, 1, 1);
		put(helper, "gas_line", -8, 1, 2);
		GridSceneKit.place(helper, block("wildcatter:stock_tank"), -9, 1, 2, Direction.EAST);
		ServerPlayer player = QuestScenes.player(helper, 3, 1, 4);
		earlier(server, player, "wood", "stone", "campfire", "iron", "copper", "steel", "field_manual", "steam", "lamp", "protection", "firebox", "plugs",
				"seismograph", "step_up", "rig", "gas_well");
		helper.runAfterDelay(320, () -> {
			StorageTankBlockEntity tank = GridSceneKit.entity(helper, -9, 1, 2, StorageTankBlockEntity.class);
			String jack = GridSceneKit.read(helper, 1, 1, 0);
			String dry = GridSceneKit.read(helper, 7, 1, 0);
			check(helper, jack.contains("pumping at") || jack.contains("flowline"), "the pumpjack should run on the dry-type: " + jack + " | " + dry);
			QuestTracker.check(player);
			check(helper, QuestData.get(server).of(player.getStringUUID()).milestones.contains(Milestones.PUMPJACK_RUNNING.toString()), "the pumpjack should lift: " + jack);
			check(helper, done(server, player, "oil_well"), "lifting oil with a pumpjack should finish An oil well");
			check(helper, tank.amount() < 0.5, "piped straight from the well, the stock tank should take nothing (no raw well fluid): " + tank.inspect().getString());
			log(scene, "well straight to the tank: " + tank.inspect().getString() + " | " + jack + " | " + dry);
			// the separator between them: the straight run out, the separator in its place, its oil side on to the tank
			for (int x = -3; x >= -7; x--) {
				helper.setBlock(new BlockPos(x, 1, 0), Blocks.AIR);
			}
			GridSceneKit.place(helper, block("wildcatter:separator"), -3, 1, -1, Direction.EAST);
			put(helper, "gas_line", -7, 2, 0);
			put(helper, "gas_line", -8, 2, 0);
		});
		int[] shutAt = {-1};
		for (int t = 340; t <= 760; t += 20) {
			final int at = t;
			helper.runAfterDelay(t, () -> {
				if (shutAt[0] >= 0) {
					return;
				}
				String sep = GridSceneKit.read(helper, -3, 1, -1);
				if (sep.contains("SHUT IN: gas has nowhere to go")) {
					shutAt[0] = at;
					log(scene, "no flare, at " + at + ": " + sep + " | " + GridSceneKit.read(helper, 1, 1, 0));
					// the fix: a flare on its gas side
					GridSceneKit.place(helper, block("wildcatter:flare_stack"), -6, 1, -4, Direction.NORTH);
					for (int[] c : new int[][] {{-6, 3, -1}, {-6, 3, -2}, {-6, 3, -3}, {-6, 2, -3}, {-6, 1, -3}}) {
						put(helper, "gas_line", c[0], c[1], c[2]);
					}
				} else if (at == 760) {
					check(helper, false, "with no flare the separator should shut in on its gas: " + sep);
				}
			});
		}
		helper.runAfterDelay(1000, () -> {
			SeparatorBlockEntity sep = GridSceneKit.entity(helper, -3, 1, -1, SeparatorBlockEntity.class);
			StorageTankBlockEntity tank = GridSceneKit.entity(helper, -9, 1, 2, StorageTankBlockEntity.class);
			String s = sep.inspect().getString(), flare = GridSceneKit.read(helper, -6, 1, -4);
			check(helper, !s.contains("SHUT IN"), "with a flare on its gas side the separator should separate again: " + s);
			check(helper, tank.amount() > 0.5 && tank.product() == dev.wildcatter.fluid.Product.CRUDE, "crude should reach the stock tank through the separator: " + tank.inspect().getString());
			log(scene, "with the flare: " + s + " | " + flare + " | " + tank.inspect().getString());
			// The lease: its four parts carried
			QuestTracker.check(player);
			check(helper, !done(server, player, "lease"), "The lease should wait for its parts");
			player.getInventory().add(new ItemStack(item("wildcatter:separator")));
			player.getInventory().add(new ItemStack(item("wildcatter:flare_stack")));
			player.getInventory().add(new ItemStack(item("wildcatter:injection_pump")));
			QuestTracker.check(player);
			check(helper, !done(server, player, "lease"), "The lease should wait for the disposal wellhead");
			player.getInventory().add(new ItemStack(item("wildcatter:disposal_wellhead")));
			QuestTracker.check(player);
			check(helper, done(server, player, "lease"), "separator, flare, injection pump and disposal wellhead should finish The lease");
			// A stock tank of crude: the tank carried, then a bucket filled at the tank once it holds a bucket's worth
			player.getInventory().add(new ItemStack(item("wildcatter:stock_tank")));
			QuestTracker.check(player);
			check(helper, !done(server, player, "crude"), "A stock tank of crude should wait for a bucket of crude");
			double need = Tuning.CRUDE_BUCKET_UNITS - tank.amount();
			if (need > 0) {
				tank.testFill(dev.wildcatter.fluid.Product.CRUDE, need);   // the rest of a bucket's worth at once: the flow is shown above
			}
			ItemStack bucket = new ItemStack(Items.BUCKET);
			player.setItemInHand(InteractionHand.MAIN_HAND, bucket);
			check(helper, tank.bucket(bucket, player, InteractionHand.MAIN_HAND), "a bucket on the stock tank should be taken");
			QuestTracker.check(player);
			int crude = QuestScenes.count(player, item("wildcatter:crude_oil_bucket"));
			check(helper, crude >= 1, "an empty bucket at the tank should fill with crude: " + tank.inspect().getString());
			check(helper, done(server, player, "crude"), "a stock tank and a bucket of crude should finish A stock tank of crude");
			check(helper, Voltage.INDUSTRIAL.nominal() > 0, "the industrial tier read from the standard");
			log(scene, String.format("ok: shut in at %d without a flare; %s; quests %s", shutAt[0], tank.inspect().getString(),
					QuestData.get(server).of(player.getStringUUID()).done));
			ReservoirData.TEST_REGIONS.remove(region);
			QuestScenes.leave(helper, player);
			helper.succeed();
		});
	}
}
