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
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.block.InsulatorBlock;
import dev.wildcatter.block.InsulatorBlockEntity;
import dev.wildcatter.block.LoadBlock;
import dev.wildcatter.block.PanelBlockEntity;
import dev.wildcatter.block.PanelGutterBlockEntity;
import dev.wildcatter.block.TransformerBlock;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.ConductorSize;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.item.SpoolItem;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * NeoForge server scenes of the protection gap wave, group D: spans, 4.16 kV spans, leads, pole hardware, the house's earth and the
 * flashover. Helpers in {@link GridSceneKit}. The scenes build at y {@link #U} on a stone floor and start a 500 kW diesel set.
 * <ul>
 * <li>{@code span_burns}: a 12.47 kV span and a lead to a pole transformer feed two heaters; with the span's ampacity shrunk (a test
 * hook) the span heats, burns down and the heaters go dark; breaking the far pole brings its pole transformer down with it.</li>
 * <li>{@code mv_span_flashover}: a 4.16 kV span strung from an MV spool carries 2.4 kV to neutral and, through a lead, lights a 4.16 kV
 * pole unit's heater; a second span to a 1 kV insulator flashes over, and its arc shatters it.</li>
 * <li>{@code house_earth}: a service with a ground rod at its load centre reads SERVICE (bonded and earthed); with the service
 * neutral cut the lamp stays lit through the earth; with the rod gone too it goes dark.</li>
 * </ul>
 */
final class DistributionScenes {
	private DistributionScenes() {
	}

	private static final int U = 2;
	private static final Direction NORTH = Direction.NORTH;

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> scenes = new LinkedHashMap<>();
		scenes.put("span_burns", DistributionScenes::spanBurns);
		scenes.put("mv_span_flashover", DistributionScenes::mvSpanFlashover);
		scenes.put("house_earth", DistributionScenes::houseEarth);
		return scenes;
	}

	// ---- helpers ------------------------------------------------------------------------------------------------------------------------
	private static void set(GameTestHelper helper, BlockState state, int x, int y, int z) {
		helper.getLevel().setBlock(abs(helper, x, y, z), state, 3);
	}

	private static void insulator(GameTestHelper helper, Block block, int x, int y, int z, Conductor conductor) {
		InsulatorBlock insulator = (InsulatorBlock) block;
		InsulatorBlock.Mount mount = insulator.mountAt(helper.getLevel(), abs(helper, x, y, z));
		check(helper, mount != null, "nothing holds an insulator at " + x + ", " + y + ", " + z);
		set(helper, insulator.defaultBlockState().setValue(InsulatorBlock.MOUNT, mount).setValue(dev.wildcatter.block.CableBlock.CONDUCTOR, conductor), x, y, z);
	}

	private static void poleUnit(GameTestHelper helper, Block block, int x, int y, int z, Direction facing) {
		set(helper, block.defaultBlockState().setValue(TransformerBlock.FACING, facing), x, y, z);
	}

	private static boolean lit(GameTestHelper helper, int x, int y, int z) {
		BlockState state = helper.getLevel().getBlockState(abs(helper, x, y, z));
		return state.hasProperty(LoadBlock.LIT) && state.getValue(LoadBlock.LIT);
	}

	private static boolean air(GameTestHelper helper, int x, int y, int z) {
		return helper.getLevel().getBlockState(abs(helper, x, y, z)).isAir();
	}

	/** The 12.47 kV supply: the set at (10, U, 0), a 480 V pad-mount at x 11-12 run backward, 12.47 kV out at x 13. */
	private static void distributionSupply(GameTestHelper helper) {
		genset(helper, 10, U, 0);
		place(helper, WildcatterBlocks.PADMOUNT_480, 11, U, 0, NORTH);
	}

	// ---- span_burns -----------------------------------------------------------------------------------------------------------------
	static void spanBurns(GameTestHelper helper) {
		floor(helper, -2, -6, 40, 8);
		distributionSupply(helper);
		put(helper, "dist_wire_hot", 13, U, 0);
		for (int y = U; y <= U + 10; y++) {
			put(helper, "dist_wire_hot", 14, y, 0);    // the riser up the near pole to its pole-top pin
		}
		place(helper, WildcatterBlocks.WOOD_POLE_40, 15, U, 0, NORTH);
		place(helper, WildcatterBlocks.WOOD_POLE_40, 35, U, 0, NORTH);
		insulator(helper, WildcatterBlocks.INSULATOR_DIST, 15, U + 10, 0, Conductor.A);
		insulator(helper, WildcatterBlocks.INSULATOR_DIST, 35, U + 10, 0, Conductor.A);
		// the pole transformer hangs on the far pole's south side, its back to the pole; two heaters on its low side
		poleUnit(helper, WildcatterBlocks.POLE_TRANSFORMER, 35, U + 6, 1, Direction.SOUTH);
		put(helper, "electric_heater", 35, U + 5, 1);   // its bottom: leg A
		put(helper, "electric_heater", 34, U + 6, 1);   // its west side: leg B
		BlockPos near = abs(helper, 15, U + 10, 0);
		BlockPos far = abs(helper, 35, U + 10, 0);
		BlockPos unit = abs(helper, 35, U + 6, 1);
		SpoolItem.Result span = SpoolItem.connect(helper.getLevel(), near, far, Voltage.DISTRIBUTION, false, 1024, ConductorSize.ACSR_SPARROW);
		check(helper, span.ok() && span.length() == 20, "a 20 m span of #2 ACSR should string: " + span);
		SpoolItem.Result lead = SpoolItem.connectLead(helper.getLevel(), far, unit, Voltage.DISTRIBUTION, false, 64, ConductorSize.ACSR_SPARROW);
		check(helper, lead.ok(), "a lead from the far insulator down to the pole transformer's H1 should string: " + lead);
		SpoolItem.Result tooLong = SpoolItem.connectLead(helper.getLevel(), near, unit, Voltage.DISTRIBUTION, false, 64, ConductorSize.ACSR_SPARROW);
		check(helper, !tooLong.ok(), "a second lead, 20 m long, to a unit that has its lead should be refused: " + tooLong);
		start(helper, 10, U, 0, () -> helper.runAfterDelay(80, () -> {
			String farRead = read(helper, 35, U + 10, 0);
			String unitRead = read(helper, 35, U + 6, 1);
			log("span_burns", "carrying | " + farRead + " | " + unitRead);
			check(helper, farRead.contains("1 span") && farRead.matches(".*\\b7[0-2]\\d\\d V.*"), "the far insulator should hold about 7.2 kV: " + farRead);
			check(helper, lit(helper, 35, U + 5, 1) && lit(helper, 34, U + 6, 1), "the heaters should run through the span, the lead and the pole unit: " + unitRead);
			// the span's #2 ACSR is rated 180 A; shrunk to 0.06 A its 3 kW load is about 7 x: it heats and parts in about half a minute
			InsulatorBlockEntity.testAmpacityScale = 0.06 / 180.0;
			helper.runAfterDelay(900, () -> {
				InsulatorBlockEntity.testAmpacityScale = null;
				String burnt = read(helper, 15, U + 10, 0);
				log("span_burns", "burnt | " + burnt + " | " + read(helper, 35, U + 10, 0));
				check(helper, burnt.contains("no spans"), "the overloaded span should have burnt down: " + burnt);
				check(helper, !lit(helper, 35, U + 5, 1) && !lit(helper, 34, U + 6, 1), "with the span down the heaters should be dark");
				// breaking the far pole: its insulator and the pole transformer hung on it come down
				helper.getLevel().destroyBlock(abs(helper, 35, U + 2, 0), true);
				helper.runAfterDelay(20, () -> {
					check(helper, air(helper, 35, U + 6, 1), "the pole transformer should fall with its pole");
					check(helper, air(helper, 35, U + 10, 0), "the insulator should fall with its pole");
					log("span_burns", "ok: " + span.message() + " | " + lead.message() + " | " + burnt);
					helper.succeed();
				});
			});
		}));
	}

	// ---- mv_span_flashover --------------------------------------------------------------------------------------------------------
	static void mvSpanFlashover(GameTestHelper helper) {
		floor(helper, -2, -6, 42, 24);
		genset(helper, 10, U, 0);
		// a unit substation run backward: 480 V in on its side, 4.16 kV out at x 15 (as the client scenes' inlineTransformer)
		place(helper, WildcatterBlocks.UNIT_SUBSTATION, 14, U, 0, Direction.SOUTH);
		put(helper, "cable_mv", 15, U, 0);
		for (int y = U; y <= U + 10; y++) {
			put(helper, "cable_mv", 16, y, 0);         // the riser: its A joins the pole-top insulator's A
		}
		place(helper, WildcatterBlocks.WOOD_POLE_40, 17, U, 0, NORTH);
		place(helper, WildcatterBlocks.WOOD_POLE_40, 37, U, 0, NORTH);
		place(helper, WildcatterBlocks.WOOD_POLE_40, 17, U, 18, NORTH);
		insulator(helper, WildcatterBlocks.INSULATOR_DIST, 17, U + 10, 0, Conductor.A);
		insulator(helper, WildcatterBlocks.INSULATOR_DIST, 37, U + 10, 0, Conductor.A);
		poleUnit(helper, WildcatterBlocks.POLE_TRANSFORMER_MV, 37, U + 6, 1, Direction.SOUTH);
		put(helper, "electric_lamp", 37, U + 5, 1);
		BlockPos near = abs(helper, 17, U + 10, 0);
		BlockPos far = abs(helper, 37, U + 10, 0);
		check(helper, WildcatterItems.SPOOL_MV instanceof SpoolItem s && s.tier() == Voltage.MEDIUM, "there should be a 4.16 kV conductor spool");
		SpoolItem.Result span = SpoolItem.connect(helper.getLevel(), near, far, Voltage.MEDIUM, false, 1024, ConductorSize.ACSR_RAVEN);
		check(helper, span.ok(), "a 4.16 kV span of 1/0 ACSR should string: " + span);
		SpoolItem.Result lead = SpoolItem.connectLead(helper.getLevel(), far, abs(helper, 37, U + 6, 1), Voltage.MEDIUM, false, 64, ConductorSize.ACSR_RAVEN);
		check(helper, lead.ok(), "a 4.16 kV lead down to the pole unit should string: " + lead);
		SpoolItem.Result wrong = SpoolItem.connectLead(helper.getLevel(), far, abs(helper, 37, U + 6, 1), Voltage.DISTRIBUTION, false, 64, ConductorSize.ACSR_RAVEN);
		check(helper, !wrong.ok(), "a 12.47 kV lead should not land on a 4.16 kV unit: " + wrong);
		start(helper, 10, U, 0, () -> helper.runAfterDelay(80, () -> {
			String farRead = read(helper, 37, U + 10, 0);
			log("mv_span_flashover", "carrying | " + farRead + " | " + read(helper, 37, U + 6, 1));
			check(helper, farRead.contains("4.16 kV") && farRead.matches(".*\\b2[34]\\d\\d V.*"), "the far insulator should hold about 2.4 kV to neutral: " + farRead);
			check(helper, lit(helper, 37, U + 5, 1), "the lamp should light through the 4.16 kV span, the lead and the pole unit: " + read(helper, 37, U + 6, 1));
			// a span on to a 1 kV insulator: the 4.16 kV line flashes over it to earth, and its arc shatters the porcelain
			insulator(helper, WildcatterBlocks.INSULATOR_LV, 17, U + 10, 18, Conductor.A);
			SpoolItem.Result under = SpoolItem.connect(helper.getLevel(), near, abs(helper, 17, U + 10, 18), Voltage.MEDIUM, false, 1024, ConductorSize.ACSR_RAVEN);
			check(helper, under.ok(), "a span can be strung to an under-rated insulator: " + under);
			CircuitManager.markDirty(helper.getLevel());
			String[] flashing = {""};
			helper.runAfterDelay(2, () -> flashing[0] = read(helper, 17, U + 10, 18));
			helper.runAfterDelay(200, () -> {
				log("mv_span_flashover", "flashover | " + flashing[0] + " | now " + read(helper, 17, U + 10, 18) + " | " + read(helper, 10, U, 0));
				check(helper, air(helper, 17, U + 10, 18), "the under-rated insulator's arc should have shattered it: " + read(helper, 17, U + 10, 18));
				log("mv_span_flashover", "ok: " + span.message() + " | " + lead.message() + " | " + farRead);
				helper.succeed();
			});
		}));
	}

	// ---- house_earth ---------------------------------------------------------------------------------------------------------------
	static void houseEarth(GameTestHelper helper) {
		floor(helper, -2, -6, 30, 10);
		distributionSupply(helper);
		for (int x = 13; x <= 15; x++) {
			put(helper, "dist_wire_hot", x, U, 0);
		}
		place(helper, WildcatterBlocks.WOOD_POLE_40, 16, U, -1, NORTH);
		poleUnit(helper, WildcatterBlocks.POLE_TRANSFORMER, 16, U, 0, Direction.WEST);   // the feeder into its front (west)
		// the service: leg A off its back (east), the neutral off its bottom, into a cable that takes both
		for (int x = 17; x <= 20; x++) {
			put(helper, "wool_wire_hot", x, U, 0);
		}
		for (int x = 16; x <= 21; x++) {
			put(helper, "wool_wire_neutral", x, U - 1, 0);
		}
		int[][] feed = {{21, U, 0}, {22, U, 0}, {23, U, 0}, {24, U, 0}, {24, U, 1}, {24, U, 2}, {24, U, 3}, {24, U, 4}, {24, U, 5}, {24, U, 6}, {24, U + 1, 6}, {23, U + 1, 6}, {22, U + 1, 6}};
		for (int[] c : feed) {
			put(helper, "cable_lv", c[0], c[1], c[2]);
		}
		place(helper, WildcatterBlocks.LOAD_CENTRE, 22, U, 6, NORTH);        // fed on its top face
		place(helper, WildcatterBlocks.PANEL_GUTTER, 21, U, 6, NORTH);       // space on its west: phase A
		set(helper, dev.wildcatter.WildcatterBlocks.BRANCH_CABLE.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Z), 21, U, 5);
		set(helper, dev.wildcatter.WildcatterBlocks.BRANCH_CABLE.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Z), 21, U, 4);
		put(helper, "electric_lamp", 21, U, 3);
		put(helper, "ground_rod", 22, U - 1, 6);                            // the grounding electrode under the panel, in the earth
		PanelGutterBlockEntity gutter = (PanelGutterBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 21, U, 6));
		String refused = gutter.install(WildcatterItems.BRANCH_BREAKERS.get("branch_breaker_1p_20"));
		check(helper, refused == null, "a 20 A breaker should clip in: " + refused);
		gutter.setClosed(true);
		start(helper, 10, U, 0, () -> helper.runAfterDelay(60, () -> {
			PanelBlockEntity panel = (PanelBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 22, U, 6));
			String bonded = panel.inspect().getString();
			log("house_earth", "bonded | " + bonded + " | " + read(helper, 21, U, 3));
			check(helper, bonded.contains("SERVICE: neutral bonded"), "the load centre with a rod should be the bonded, earthed service: " + bonded);
			check(helper, lit(helper, 21, U, 3), "the house lamp should light: " + read(helper, 21, U, 3));
			// an open neutral on the service: the lamp's current comes home through the rod and the earth
			helper.getLevel().setBlock(abs(helper, 18, U - 1, 0), Blocks.AIR.defaultBlockState(), 3);
			CircuitManager.markDirty(helper.getLevel());
			helper.runAfterDelay(40, () -> {
				String open = panel.inspect().getString();
				log("house_earth", "open neutral | " + open + " | " + read(helper, 21, U, 3));
				check(helper, lit(helper, 21, U, 3) && panel.electrodeAmps() > 0.2, "with the neutral cut the lamp should stay lit through the earth: " + open + " | " + read(helper, 21, U, 3));
				helper.getLevel().setBlock(abs(helper, 22, U - 1, 6), Blocks.AIR.defaultBlockState(), 3);
				CircuitManager.markDirty(helper.getLevel());
				helper.runAfterDelay(40, () -> {
					String none = panel.inspect().getString();
					log("house_earth", "no rod | " + none + " | " + read(helper, 21, U, 3));
					check(helper, !lit(helper, 21, U, 3) && none.contains("not earthed"), "with no neutral and no rod the lamp should go dark: " + none + " | " + read(helper, 21, U, 3));
					log("house_earth", "ok: " + bonded + " | " + open);
					helper.succeed();
				});
			});
		}));
	}
}
