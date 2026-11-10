package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.abs;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.check;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.floor;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.place;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.put;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.read;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.BigPartBlockEntity;
import dev.wildcatter.block.BoilerBlockEntity;
import dev.wildcatter.block.ChlorinatorBlockEntity;
import dev.wildcatter.block.DecantTowerBlockEntity;
import dev.wildcatter.block.FireboxBlock;
import dev.wildcatter.block.GuyAnchorBlockEntity;
import dev.wildcatter.block.InsulatorBlock;
import dev.wildcatter.block.Lines;
import dev.wildcatter.block.PipeBlock;
import dev.wildcatter.block.Plumbing;
import dev.wildcatter.block.PoleLoad;
import dev.wildcatter.block.TransformerBlock;
import dev.wildcatter.block.TransformerBlockEntity;
import dev.wildcatter.block.WireShape;
import dev.wildcatter.block.WoodPoleBlock;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.ConductorSize;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.FireboxFuel;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.item.GuyStrandItem;
import dev.wildcatter.item.SpoolItem;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * NeoForge server scenes for Tim's answers of 2026-11-10 built on branch {@code w-grid} (docs/questions/answers-grid.md). Helpers in
 * {@link GridSceneKit}; the scenes build at y {@link #U} on a stone floor.
 * <ul>
 * <li>{@code firebox_heat_rate}: fireboxes on pipeline gas and on biogas stay lit and burn at their one 0.36 MW heat rate (answer 17); a bucket of crude or diesel burns one hour.</li>
 * <li>{@code guy_wire}: a dead-end 40 ft pole reads UNGUYED and leans; a tangent pole holds; a pole from an older save reads UNGUYED and
 * stays plumb; a guy to an anchor holds the pull and draws the leaning pole plumb (answer 16).</li>
 * <li>{@code pole_unit_facing}: a pole unit set by command in its default facing turns its back to its pole; a named facing and a unit
 * from an older save keep theirs (answer 18).</li>
 * <li>{@code decant_rate}: a small cell takes today's 20 kg a tick; a 32 x 32 cell 80; a 32 x 32 cell from an older save 20 (answer 12).</li>
 * <li>{@code slurry_pipe}: the 600 mm slurry pipe passes its rated 1 m3/s (75 kg a tick of slurry) to a cell that would take 80, and
 * carries no gas (answer 12).</li>
 * <li>{@code air_header}: a run of 600 mm air header is its own network, apart from instrument air tubing (answer 12).</li>
 * <li>{@code zircon_feed}: a chlorinator takes a tagged zircon concentrate by a hopper at its kg file's figure, with no separator
 * (answer 9).</li>
 * </ul>
 */
final class GridAnswerScenes {
	private GridAnswerScenes() {
	}

	private static final int U = 1;

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> scenes = new LinkedHashMap<>();
		scenes.put("firebox_heat_rate", GridAnswerScenes::fireboxHeatRate);
		scenes.put("guy_wire", GridAnswerScenes::guyWire);
		scenes.put("pole_unit_facing", GridAnswerScenes::poleUnitFacing);
		scenes.put("decant_rate", GridAnswerScenes::decantRate);
		scenes.put("slurry_pipe", GridAnswerScenes::slurryPipe);
		scenes.put("air_header", GridAnswerScenes::airHeader);
		scenes.put("zircon_feed", GridAnswerScenes::zirconFeed);
		return scenes;
	}

	// ---- helpers ------------------------------------------------------------------------------------------------------------------------
	private static void set(GameTestHelper helper, BlockState state, int x, int y, int z) {
		helper.getLevel().setBlock(abs(helper, x, y, z), state, 3);
	}

	private static TestGasSource.SourceEntity source(GameTestHelper helper, int x, int y, int z, Product product, double rate) {
		set(helper, BuiltInRegistries.BLOCK.getValue(WildcatterNeoForgeGameTests.id("gas_source")).defaultBlockState(), x, y, z);
		TestGasSource.SourceEntity s = (TestGasSource.SourceEntity) helper.getLevel().getBlockEntity(abs(helper, x, y, z));
		s.product = product;
		s.rate = rate;
		return s;
	}

	private static BlockState state(GameTestHelper helper, int x, int y, int z) {
		return helper.getLevel().getBlockState(abs(helper, x, y, z));
	}

	// ---- firebox_heat_rate (answer 17) -------------------------------------------------------------------------------------------------
	static void fireboxHeatRate(GameTestHelper helper) {
		floor(helper, -2, -2, 10, 4);
		Product[] fuels = {Product.PIPELINE_GAS, Product.BIOGAS};
		TestGasSource.SourceEntity[] sources = new TestGasSource.SourceEntity[2];
		for (int i = 0; i < 2; i++) {
			int x = i * 5;
			sources[i] = source(helper, x, U, 2, fuels[i], 1.0);   // a digester's (or a meter run's) 1 m3/s
			put(helper, "gas_line", x, U, 1);
			put(helper, "firebox", x, U, 0);
			put(helper, "boiler", x, U + 1, 0);
			((BoilerBlockEntity) helper.getLevel().getBlockEntity(abs(helper, x, U + 1, 0))).addWater(4000);
		}
		boolean[] dark = new boolean[2];
		for (int t = 20; t <= 140; t += 5) {
			helper.runAfterDelay(t, () -> {
				for (int i = 0; i < 2; i++) {
					dark[i] |= !state(helper, i * 5, U, 0).getValue(FireboxBlock.LIT);
				}
			});
		}
		helper.runAfterDelay(141, () -> {
			for (int i = 0; i < 2; i++) {
				double taken = sources[i].taken;
				// about 140 ticks of fire and its 200-tick feed, at 2,111 ticks a m3 of gas and 1,278 of biogas
				double want = 340 / FireboxFuel.ticksPerUnit(fuels[i]);
				log("firebox_heat_rate", fuels[i] + ": " + String.format("%.3f m3 taken (about %.3f), dark %s", taken, want, dark[i]));
				check(helper, !dark[i], "a firebox on " + fuels[i].label() + " should stay lit at its one heat rate");
				check(helper, taken > want * 0.5 && taken < want * 1.6, "a firebox on " + fuels[i].label() + " should burn about " + want + " m3, not " + taken);
			}
			check(helper, Math.abs(FireboxFuel.ticksPerUnit(Product.PIPELINE_GAS) - 2111.1) < 0.1, "a m3 of natural gas is 2,111 ticks of fire");
			for (net.minecraft.world.item.Item bucket : new net.minecraft.world.item.Item[] {dev.wildcatter.WildcatterItems.CRUDE_OIL_BUCKET, dev.wildcatter.WildcatterItems.DIESEL_BUCKET}) {
				int ticks = dev.wildcatter.block.FireboxBlockEntity.burnTicks(helper.getLevel(), new net.minecraft.world.item.ItemStack(bucket));
				check(helper, ticks == 72_000, "a bucket of " + bucket + " should burn one hour (72,000 ticks) in a firebox, not " + ticks);
			}
			log("firebox_heat_rate", "ok");
			helper.succeed();
		});
	}

	// ---- guy_wire (answer 16) -------------------------------------------------------------------------------------------------------
	private static void insulator(GameTestHelper helper, int x, int y, int z) {
		InsulatorBlock insulator = (InsulatorBlock) WildcatterBlocks.INSULATOR_DIST;
		InsulatorBlock.Mount mount = insulator.mountAt(helper.getLevel(), abs(helper, x, y, z));
		check(helper, mount != null, "nothing holds an insulator at " + x + ", " + y + ", " + z);
		set(helper, insulator.defaultBlockState().setValue(InsulatorBlock.MOUNT, mount).setValue(dev.wildcatter.block.CableBlock.CONDUCTOR, Conductor.A), x, y, z);
	}

	private static int lean(GameTestHelper helper, int x) {
		return state(helper, x, U, 0).getValue(WoodPoleBlock.LEAN);
	}

	static void guyWire(GameTestHelper helper) {
		floor(helper, -12, -3, 64, 3);
		for (int x : new int[] {0, 30, 60}) {
			place(helper, WildcatterBlocks.WOOD_POLE_40, x, U, 0, Direction.NORTH);
			insulator(helper, x, U + 10, 0);
		}
		((BigPartBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 60, U, 0))).asOlderSaveForTest();
		for (int x : new int[] {0, 30}) {
			SpoolItem.Result span = SpoolItem.connect(helper.getLevel(), abs(helper, x, U + 10, 0), abs(helper, x + 30, U + 10, 0), Voltage.DISTRIBUTION, false, 1024,
					ConductorSize.ACSR_LINNET);
			check(helper, span.ok(), "a 30 m span of Linnet should string: " + span);
		}
		PoleLoad.testLeanSeconds = 2.0;
		helper.runAfterDelay(200, () -> {
			String dead = read0(helper, 0);
			String tangent = read0(helper, 30);
			String older = read0(helper, 60);
			log("guy_wire", "unguyed | " + dead + " | " + tangent + " | " + older);
			check(helper, dead.contains("UNGUYED") && dead.contains("class 4") && dead.contains("2,400 lbf"), "the dead-end pole should read UNGUYED: " + dead);
			check(helper, lean(helper, 0) == WoodPoleBlock.MAX_LEAN && dead.contains("LEANING 5.0"), "the dead-end pole should have leant its three steps: " + dead);
			check(helper, !tangent.contains("UNGUYED") && lean(helper, 30) == 0, "the tangent pole's pulls cancel: " + tangent);
			check(helper, older.contains("UNGUYED") && lean(helper, 60) == 0, "a pole from an older save reads UNGUYED and stays plumb: " + older);
			check(helper, state(helper, 0, U + 9, 0).getValue(WoodPoleBlock.LEAN) == WoodPoleBlock.MAX_LEAN, "every cell of the pole leans");
			check(helper, state(helper, 0, U + 9, 0).getBlock() == WildcatterBlocks.WOOD_POLE_40 && !state(helper, 0, U + 10, 0).isAir(), "the pole and its insulator still stand: it never breaks");
			// the guy: an anchor 9 m out on the far side from the line, clamped at the top cell
			put(helper, "guy_anchor", -9, U, 0);
			put(helper, "guy_anchor", -1, U, 0);
			GuyStrandItem.Result close = GuyStrandItem.connect(helper.getLevel(), abs(helper, 0, U + 9, 0), abs(helper, -1, U, 0), 60);
			check(helper, !close.ok(), "an anchor 1 m out should be refused: " + close);
			GuyStrandItem.Result guy = GuyStrandItem.connect(helper.getLevel(), abs(helper, 0, U + 9, 0), abs(helper, -9, U, 0), 60);
			check(helper, guy.ok() && guy.length() == 13, "a 13 m guy should string: " + guy);
			helper.runAfterDelay(90, () -> {
				PoleLoad.testLeanSeconds = null;
				String guyed = read0(helper, 0);
				String anchor = read(helper, -9, U, 0);
				log("guy_wire", "guyed | " + guyed + " | " + anchor);
				check(helper, guyed.contains("holding") && guyed.contains("1 guy") && lean(helper, 0) == 0, "the guy should hold the pull and stand the pole plumb: " + guyed);
				check(helper, anchor.contains("3/8 in EHS"), "the anchor reads its guy: " + anchor);
				check(helper, ((GuyAnchorBlockEntity) helper.getLevel().getBlockEntity(abs(helper, -9, U, 0))).attach() != null, "the anchor holds its guy");
				// breaking the pole lets the guy go
				helper.getLevel().destroyBlock(abs(helper, 0, U + 3, 0), true);
				helper.runAfterDelay(50, () -> {
					check(helper, ((GuyAnchorBlockEntity) helper.getLevel().getBlockEntity(abs(helper, -9, U, 0))).attach() == null, "with its pole gone the anchor lets its guy go");
					log("guy_wire", "ok: " + guy.message());
					helper.succeed();
				});
			});
		});
	}

	/** The Power Meter's reading of the pole at (x, U, 0). */
	private static String read0(GameTestHelper helper, int x) {
		BlockPos foot = abs(helper, x, U, 0);
		BlockState state = helper.getLevel().getBlockState(foot);
		return PoleLoad.describe(helper.getLevel(), foot, state, (WoodPoleBlock) state.getBlock(), (BigPartBlockEntity) helper.getLevel().getBlockEntity(foot));
	}

	// ---- pole_unit_facing (answer 18) -----------------------------------------------------------------------------------------------
	static void poleUnitFacing(GameTestHelper helper) {
		floor(helper, -3, -3, 3, 3);
		place(helper, WildcatterBlocks.WOOD_POLE_40, 0, U, 0, Direction.NORTH);
		// set by command in its default facing (north), its pole to its west: it should turn its back (the bracket) to the pole
		put(helper, "pole_transformer", 1, U + 6, 0);
		// a facing named in the command, its pole to its south: kept
		set(helper, WildcatterBlocks.POLE_TRANSFORMER.defaultBlockState().setValue(TransformerBlock.FACING, Direction.EAST), 0, U + 6, -1);
		// a unit from an older save, in the default facing beside the pole: kept, so its terminals stay where they were
		put(helper, "pole_transformer", -1, U + 6, 0);
		((TransformerBlockEntity) helper.getLevel().getBlockEntity(abs(helper, -1, U + 6, 0))).asOlderSaveForTest();
		// a default-facing unit north of the pole already has its back (south) to it: left alone
		put(helper, "pole_transformer", 0, U + 5, -1);
		helper.runAfterDelay(10, () -> {
			Direction turned = state(helper, 1, U + 6, 0).getValue(TransformerBlock.FACING);
			Direction named = state(helper, 0, U + 6, -1).getValue(TransformerBlock.FACING);
			Direction older = state(helper, -1, U + 6, 0).getValue(TransformerBlock.FACING);
			Direction behind = state(helper, 0, U + 5, -1).getValue(TransformerBlock.FACING);
			log("pole_unit_facing", "turned " + turned + ", named " + named + ", older " + older + ", behind " + behind);
			check(helper, turned == Direction.EAST, "a default-facing unit with its pole to the west should face east (its back to the pole): " + turned);
			check(helper, named == Direction.EAST, "a named facing should be kept: " + named);
			check(helper, older == Direction.NORTH, "a unit from an older save should keep its facing: " + older);
			check(helper, behind == Direction.NORTH, "a unit with its pole behind it should be left: " + behind);
			check(helper, ((TransformerBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 1, U + 6, 0))).faced(), "the new unit is marked as faced");
			log("pole_unit_facing", "ok");
			helper.succeed();
		});
	}

	// ---- decant_rate and slurry_pipe (answer 12) --------------------------------------------------------------------------------------
	/**
	 * A lined cell with {@code inside} x {@code inside} cells of open floor at x0..: liner floor at y U, walls U+1..U+3, the tower in the
	 * middle at U+1 fed from above by {@code pipe} from a tails source at U+6. Returns the tower's position (relative).
	 */
	private static BlockPos cell(GameTestHelper helper, int x0, int inside, String pipe, double rate) {
		Block liner = WildcatterBlocks.CELL_LINER;
		for (int x = x0; x <= x0 + inside + 1; x++) {
			for (int z = 0; z <= inside + 1; z++) {
				set(helper, liner.defaultBlockState(), x, U, z);
				boolean wall = x == x0 || x == x0 + inside + 1 || z == 0 || z == inside + 1;
				for (int y = U + 1; y <= U + 3 && wall; y++) {
					set(helper, liner.defaultBlockState(), x, y, z);
				}
			}
		}
		int cx = x0 + 1 + inside / 2, cz = 1 + inside / 2;
		put(helper, "decant_tower", cx, U + 1, cz);
		for (int y = U + 2; y <= U + 5; y++) {
			put(helper, pipe, cx, y, cz);
		}
		source(helper, cx, U + 6, cz, Product.TAILS_SLURRY, rate);
		return new BlockPos(cx, U + 1, cz);
	}

	private static DecantTowerBlockEntity tower(GameTestHelper helper, BlockPos at) {
		return (DecantTowerBlockEntity) helper.getLevel().getBlockEntity(abs(helper, at.getX(), at.getY(), at.getZ()));
	}

	/** Measures each tower's slurry intake, kg a tick, over 100 ticks from tick 20. */
	private static void measure(GameTestHelper helper, BlockPos[] towers, Consumer<double[]> then) {
		double[] start = new double[towers.length];
		helper.runAfterDelay(20, () -> {
			for (int i = 0; i < towers.length; i++) {
				start[i] = tower(helper, towers[i]).settledKg();
			}
		});
		helper.runAfterDelay(120, () -> {
			double[] rate = new double[towers.length];
			for (int i = 0; i < towers.length; i++) {
				rate[i] = (tower(helper, towers[i]).settledKg() - start[i]) / Tuning.CELL_SLURRY_SOLIDS / 100.0;
			}
			then.accept(rate);
		});
	}

	static void decantRate(GameTestHelper helper) {
		floor(helper, -1, -1, 112, 36);
		BlockPos small = cell(helper, 0, 3, "gas_line", 100_000);
		BlockPos big = cell(helper, 8, 32, "gas_line", 100_000);
		BlockPos older = cell(helper, 44, 32, "gas_line", 100_000);
		tower(helper, older).asOlderSaveForTest();
		measure(helper, new BlockPos[] {small, big, older}, rate -> {
			String bigRead = read(helper, big.getX(), big.getY(), big.getZ());
			log("decant_rate", String.format("small %.1f, 32 x 32 %.1f, older %.1f kg a tick | %s", rate[0], rate[1], rate[2], bigRead));
			check(helper, Math.abs(rate[0] - 20) < 0.5, "a small cell's tower keeps today's 20 kg a tick: " + rate[0]);
			check(helper, Math.abs(rate[1] - 80) < 1, "a 32 x 32 cell's tower takes 1,024 / 256 x 20 = 80 kg a tick: " + rate[1]);
			check(helper, Math.abs(rate[2] - 20) < 0.5, "a tower from an older save keeps 20 kg a tick: " + rate[2]);
			check(helper, bigRead.contains("5,760 t/h"), "the tower reads its rate: " + bigRead);
			log("decant_rate", "ok");
			helper.succeed();
		});
	}

	static void slurryPipe(GameTestHelper helper) {
		floor(helper, -1, -1, 40, 36);
		BlockPos big = cell(helper, 0, 32, "slurry_pipe", 100_000);
		measure(helper, new BlockPos[] {big}, rate -> {
			BlockPos pipe = abs(helper, big.getX(), big.getY() + 1, big.getZ());
			Lines.Line line = Lines.line(helper.getLevel(), pipe);
			double cap = Lines.capacityPerTick(line, Product.TAILS_SLURRY);
			log("slurry_pipe", String.format("%.1f kg a tick through the slurry pipe (rated %.1f) | %d lengths, %s", rate[0], cap, line.pipes().size(), line.run().describe()));
			check(helper, line.slurry() && line.pipes().size() == 4, "the four lengths of slurry pipe are one rated run: " + line.pipes().size());
			check(helper, Math.abs(cap - 75) < 1e-6, "1 m3/s at 1.5 t/m3 is 75 kg a tick: " + cap);
			check(helper, Math.abs(rate[0] - 75) < 1, "the pipe should pass its rated 75 kg a tick to a cell that would take 80: " + rate[0]);
			check(helper, Lines.capacityPerTick(line, Product.BIOGAS) == 0, "a slurry pipe carries no gas");
			check(helper, helper.getLevel().getBlockState(pipe).getValue(WireShape.ARMS.get(Direction.DOWN)), "the slurry pipe draws its arm into the tower");
			log("slurry_pipe", "ok");
			helper.succeed();
		});
	}

	// ---- air_header (answer 12) -----------------------------------------------------------------------------------------------------
	static void airHeader(GameTestHelper helper) {
		floor(helper, -1, -2, 14, 2);
		put(helper, "cell_liner", 0, U, 0);                 // stands in for a tank cell
		for (int x = 1; x <= 10; x++) {
			put(helper, "air_header", x, U, 0);
		}
		put(helper, "air_tubing", 11, U, 0);
		put(helper, "air_header", 5, U, 1);                 // a drop to the next cell
		helper.runAfterDelay(5, () -> {
			int header = Plumbing.network(helper.getLevel(), List.of(abs(helper, 0, U, 0)), PipeBlock.Kind.AIR_HEADER).size();
			int tubing = Plumbing.network(helper.getLevel(), List.of(abs(helper, 0, U, 0)), PipeBlock.Kind.AIR).size();
			BlockState end = state(helper, 10, U, 0);
			log("air_header", header + " lengths of header, " + tubing + " of tubing on the cell");
			check(helper, header == 11, "the cell's air header is its 11 lengths: " + header);
			check(helper, tubing == 0, "instrument air tubing is not on the header");
			check(helper, end.getValue(WireShape.ARMS.get(Direction.WEST)) && !end.getValue(WireShape.ARMS.get(Direction.EAST)), "the header draws no arm to the tubing");
			check(helper, state(helper, 5, U, 1).getValue(WireShape.ARMS.get(Direction.NORTH)), "the drop draws its arm to the header");
			log("air_header", "ok");
			helper.succeed();
		});
	}

	// ---- zircon_feed (answer 9) -------------------------------------------------------------------------------------------------------
	static void zirconFeed(GameTestHelper helper) {
		floor(helper, -4, -4, 8, 8);
		place(helper, WildcatterBlocks.CHLORINATOR, 0, U, 0, Direction.NORTH);
		ChlorinatorBlockEntity chlorinator = (ChlorinatorBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 0, U, 0));
		ItemStack feed = new ItemStack(WildcatterNeoForgeGameTests.ZIRCON_CONCENTRATE, 2);
		check(helper, ChlorinatorBlockEntity.zirconFeed(feed), "the stand-in zircon concentrate is in #wildcatter:zircon_feed");
		check(helper, !chlorinator.canPlaceItemThroughFace(0, feed, Direction.UP) && chlorinator.canPlaceItemThroughFace(1, feed, Direction.UP),
				"a hopper on top puts zircon concentrate in its second slot");
		check(helper, !chlorinator.canPlaceItemThroughFace(1, new ItemStack(Items.QUARTZ), Direction.UP), "it takes nothing else as zircon");
		chlorinator.setItem(1, feed);
		helper.runAfterDelay(20, () -> {
			double kg = chlorinator.zirconFeedKg();
			String noCarbon = chlorinator.inspect().getString();
			log("zircon_feed", String.format("%.2f kg of zircon | %s", kg, noCarbon));
			check(helper, Math.abs(kg - 100.0) < 1e-6, "two items at the kg file's 50 kg are 100 kg of zircon: " + kg);
			check(helper, noCarbon.contains("NO CARBON") && !noCarbon.contains("NOT BUILT"), "fed zircon, it needs no separator; it wants its carbon: " + noCarbon);
			chlorinator.setItem(0, new ItemStack(Items.COAL, 4));
			helper.runAfterDelay(20, () -> {
				String noChlorine = chlorinator.inspect().getString();
				log("zircon_feed", "ok | " + noChlorine);
				check(helper, noChlorine.contains("NO CHLORINE"), "with carbon it wants its chlorine: " + noChlorine);
				helper.succeed();
			});
		});
	}
}
