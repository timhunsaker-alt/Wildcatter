package dev.wildcatter.neoforge.gametest;

import dev.wildcatter.Tuning;
import dev.wildcatter.Wildcatter;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.CrackingFurnaceBlockEntity;
import dev.wildcatter.block.GasTurbineBlockEntity;
import dev.wildcatter.block.GensetControlPanelBlockEntity;
import dev.wildcatter.block.GensetPartBlock;
import dev.wildcatter.block.GensetSkid;
import dev.wildcatter.block.LandRig;
import dev.wildcatter.block.LoadBankBlockEntity;
import dev.wildcatter.block.RigSubstructureBlockEntity;
import dev.wildcatter.block.SteamPlantLayout;
import dev.wildcatter.block.TurbineGeneratorBlockEntity;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.fluid.LeanGas;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.multiblock.BigParts;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The misc gaps wave (docs/questions/misc-gaps.md), on the server:
 * <ul>
 * <li>{@code cog_derate}: a 1 MW gas set on coke oven gas carrying a 1 MW load bank holds 864 kW, as a set on biogas holds 850 kW;</li>
 * <li>{@code aero_lean_gas}: the aeroderivative refuses coke oven gas by its Wobbe index and takes coker gas; the 16.5 MW turbine
 * takes coke oven gas;</li>
 * <li>{@code cracker_pump}: a process water pump on a diesel set's 480 V fills a cracking furnace's water, as play does (the cracker
 * scenes topped it up by hook);</li>
 * <li>{@code alternator_frames}: a gas set's alternator is drawn at the 1 MW frame, a diesel set's at the 500 kW one;</li>
 * <li>{@code drawn_runs}: a formed steam plant draws its feedwater line and a formed land rig its motor cables; broken, they stop.</li>
 * </ul>
 */
final class MiscGapScenes {
	private MiscGapScenes() {
	}

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> scenes = new LinkedHashMap<>();
		scenes.put("cog_derate", MiscGapScenes::cogDerate);
		scenes.put("aero_lean_gas", MiscGapScenes::aeroLeanGas);
		scenes.put("cracker_pump", MiscGapScenes::crackerPump);
		scenes.put("alternator_frames", MiscGapScenes::alternatorFrames);
		scenes.put("drawn_runs", MiscGapScenes::drawnRuns);
		return scenes;
	}

	private static final int U = 2;
	private static final Direction NORTH = Direction.NORTH;

	// ---- helpers (as GasPowerScenes') ---------------------------------------------------------------------------------------------
	private static void check(GameTestHelper helper, boolean ok, String message) {
		helper.assertTrue(ok, Component.literal(message));
	}

	private static BlockPos abs(GameTestHelper helper, int x, int y, int z) {
		return helper.absolutePos(new BlockPos(x, y, z));
	}

	private static ServerLevel level(GameTestHelper helper) {
		return helper.getLevel();
	}

	private static void place(GameTestHelper helper, Block block, int x, int z, Direction facing) {
		BlockPos main = abs(helper, x, U, z);
		BlockState state = block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
		level(helper).setBlock(main, state, 3);
		BigParts.grow(level(helper), main, state);
	}

	private static void put(GameTestHelper helper, String id, int x, int y, int z) {
		level(helper).setBlock(abs(helper, x, y, z), BuiltInRegistries.BLOCK.getValue(Wildcatter.id(id)).defaultBlockState(), 3);
	}

	private static BlockEntity at(GameTestHelper helper, int x, int y, int z) {
		return BigParts.entity(level(helper), abs(helper, x, y, z));
	}

	private static String read(GameTestHelper helper, int x, int z) {
		return at(helper, x, U, z) instanceof Inspectable i ? i.inspect().getString() : "(no readout)";
	}

	/** A stone floor at y 0 and y 1, the air above cleared, its chunks forced (see AmmoniaPlantScenes.floor). */
	private static void floor(GameTestHelper helper, int x1, int z1, int x2, int z2) {
		BlockPos a = abs(helper, x1, 0, z1);
		BlockPos b = abs(helper, x2, 0, z2);
		for (int cx = Math.min(a.getX(), b.getX()) >> 4; cx <= Math.max(a.getX(), b.getX()) >> 4; cx++) {
			for (int cz = Math.min(a.getZ(), b.getZ()) >> 4; cz <= Math.max(a.getZ(), b.getZ()) >> 4; cz++) {
				level(helper).setChunkForced(cx, cz, true);
			}
		}
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				level(helper).setBlock(abs(helper, x, 0, z), Blocks.STONE.defaultBlockState(), 3);
				level(helper).setBlock(abs(helper, x, 1, z), Blocks.STONE.defaultBlockState(), 3);
			}
		}
	}

	private static TestGasSource.SourceEntity source(GameTestHelper helper, int x, int z, Product product, double rate) {
		level(helper).setBlock(abs(helper, x, U, z), BuiltInRegistries.BLOCK.getValue(WildcatterNeoForgeGameTests.id("gas_source")).defaultBlockState(), 3);
		TestGasSource.SourceEntity s = (TestGasSource.SourceEntity) level(helper).getBlockEntity(abs(helper, x, U, z));
		s.product = product;
		s.rate = rate;
		return s;
	}

	private static GensetControlPanelBlockEntity panel(GameTestHelper helper, int x, int z) {
		return (GensetControlPanelBlockEntity) level(helper).getBlockEntity(abs(helper, x, U, z));
	}

	private static BlockPos inline(GameTestHelper helper, Block block, int x, int z, Direction d, boolean feedMain) {
		int across = block == WildcatterBlocks.UNIT_SUBSTATION ? 3 : 1;
		BlockPos from = new BlockPos(x, U, z);
		BlockPos first = from.relative(d);
		Direction facing = feedMain ? d.getCounterClockWise() : d.getClockWise();
		BlockPos main = feedMain ? first : first.relative(d, across);
		place(helper, block, main.getX(), main.getZ(), facing);
		return from.relative(d, across + 2);
	}

	private static void log(String scene, String text) {
		System.out.println("[wildcatter-neoforge-test] " + scene + " " + text);
	}

	// ---- cog_derate -------------------------------------------------------------------------------------------------------------------
	/** The biogas scene's set and bank (GasPowerScenes.biogas), on coke oven gas: held at 86 % of 1 MW. */
	static void cogDerate(GameTestHelper helper) {
		floor(helper, -2, -6, 24, 9);
		TestGasSource.SourceEntity ovens = source(helper, 3, -3, Product.COKE_OVEN_GAS, 1.0);
		put(helper, "gas_line", 3, U, -2);
		put(helper, "gas_line", 3, U, -1);                                          // to the engine's north side
		GensetSkid.build(level(helper), abs(helper, 8, U, 0), NORTH, true);        // radiator x 2, engine x 3-6, panel x 8, z 0-1
		BlockPos mv = inline(helper, WildcatterBlocks.UNIT_SUBSTATION, 8, 0, Direction.EAST, false);
		for (int z = 0; z <= 5; z++) {
			put(helper, "cable_mv", mv.getX(), U, z);
		}
		put(helper, "cable_mv", 14, U, 5);
		put(helper, "cable_mv", 15, U, 5);
		put(helper, "cable_mv", 16, U, 5);
		place(helper, WildcatterBlocks.UNIT_AUX_TRANSFORMER, 16, 4, NORTH);
		place(helper, WildcatterBlocks.LOAD_BANK, 20, 2, NORTH);
		put(helper, "bus_duct_gen", 16, U + 4, 1);
		for (int x = 17; x <= 19; x++) {
			put(helper, "bus_duct_gen", x, U + 4, 1);
		}
		for (int y = 3; y >= 0; y--) {
			put(helper, "bus_duct_gen", 19, U + y, 1);
		}
		for (int x = 20; x <= 22; x++) {
			put(helper, "bus_duct_gen", x, U, 1);
		}
		helper.runAfterDelay(20, () -> ((LoadBankBlockEntity) at(helper, 20, U, 2)).setSteps(1));
		helper.runAfterDelay(30, () -> panel(helper, 8, 0).setWantRun(true));
		helper.runAfterDelay(320, () -> {
			String set = read(helper, 8, 0);
			check(helper, set.contains("RUNNING") && set.contains("breaker CLOSED"), "the gas set should start on coke oven gas: " + set);
			helper.runAfterDelay(400, () -> {
				GensetControlPanelBlockEntity p = panel(helper, 8, 0);
				String held = read(helper, 8, 0);
				double kw = Tuning.GENSET_GAS_WATTS * Tuning.GENSET_COKE_OVEN_GAS_DERATE;
				check(helper, p.onCokeOvenGas() && held.contains("COKE OVEN GAS: 864 kW MAX"), "the set should be on coke oven gas, derated: " + held);
				check(helper, Math.abs(p.ratedWatts() - kw) < 1, "its rating should be 864 kW: " + p.ratedWatts());
				check(helper, p.watts() > kw * 0.95 && p.watts() < kw * 1.01, "the set should hold about 864 kW against the 1 MW bank: " + p.watts() + " | " + held
						+ " | bank " + read(helper, 20, 2));
				check(helper, ovens.taken > 1, "it should burn the ovens' gas: " + ovens.taken);
				log("cog_derate", String.format("ok: %.0f kW, %.0f m3 taken | %s", p.watts() / 1000, ovens.taken, held));
				helper.succeed();
			});
		});
	}

	// ---- aero_lean_gas ------------------------------------------------------------------------------------------------------------------
	/** Three turbines, each with a gas on its fuel nozzle (the west face of its cell at x 0, z 6). */
	static void aeroLeanGas(GameTestHelper helper) {
		floor(helper, -3, -1, 26, 15);
		place(helper, WildcatterBlocks.AERO_TURBINE, 0, 0, NORTH);                 // x 0-4, z 0-13
		source(helper, -2, 6, Product.COKE_OVEN_GAS, 5.0);
		put(helper, "gas_line", -1, U, 6);
		place(helper, WildcatterBlocks.GAS_TURBINE, 10, 0, NORTH);                 // x 10-13, z 0-9
		source(helper, 8, 6, Product.COKE_OVEN_GAS, 5.0);
		put(helper, "gas_line", 9, U, 6);
		place(helper, WildcatterBlocks.AERO_TURBINE, 20, 0, NORTH);                // x 20-24, z 0-13
		source(helper, 18, 6, Product.COKER_GAS, 5.0);
		put(helper, "gas_line", 19, U, 6);
		helper.runAfterDelay(120, () -> {
			String aero = ((GasTurbineBlockEntity) at(helper, 0, U, 0)).fuelName();
			String frame = ((GasTurbineBlockEntity) at(helper, 10, U, 0)).fuelName();
			String coker = ((GasTurbineBlockEntity) at(helper, 20, U, 0)).fuelName();
			log("aero_lean_gas", "aero on coke oven gas | " + aero + " | frame | " + frame + " | aero on coker gas | " + coker);
			check(helper, aero.startsWith("WRONG GAS") && aero.contains("Wobbe index is 30 MJ/m3, 40 % off"), "the aeroderivative should refuse coke oven gas by its Wobbe index: " + aero);
			check(helper, frame.equals(Product.COKE_OVEN_GAS.label()), "the 16.5 MW turbine should take coke oven gas: " + frame);
			check(helper, coker.equals(Product.COKER_GAS.label()), "the aeroderivative should take coker gas: " + coker);
			check(helper, LeanGas.aeroTakes(Product.PIPELINE_GAS), "pipeline gas is its design gas");
			log("aero_lean_gas", "ok");
			helper.succeed();
		});
	}

	// ---- cracker_pump -------------------------------------------------------------------------------------------------------------------
	/** A cracking furnace's water from a process water pump over a sump, on a diesel set's 480 V (no hook). */
	static void crackerPump(GameTestHelper helper) {
		floor(helper, -16, -2, 6, 10);
		place(helper, WildcatterBlocks.CRACKING_FURNACE, 0, 0, NORTH);             // x 0-3, z 0-5, 14 high
		CrackingFurnaceBlockEntity furnace = (CrackingFurnaceBlockEntity) at(helper, 0, U, 0);
		level(helper).setBlock(abs(helper, -3, 1, 2), Blocks.WATER.defaultBlockState(), 3);   // the sump, in the floor under the pump
		BlockState pump = BuiltInRegistries.BLOCK.getValue(Wildcatter.id("process_water_pump")).defaultBlockState();
		if (pump.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			pump = pump.setValue(BlockStateProperties.HORIZONTAL_FACING, NORTH);
		}
		level(helper).setBlock(abs(helper, -3, U, 2), pump, 3);
		put(helper, "pipe", -2, U, 2);
		put(helper, "pipe", -1, U, 2);                                              // against the furnace's west side
		GensetSkid.build(level(helper), abs(helper, -10, U, 6), NORTH, false);     // a 500 kW diesel set, its panel at x -10
		for (int z = 6; z >= 2; z--) {
			put(helper, "cable_industrial", -9, U, z);
		}
		for (int x = -8; x <= -4; x++) {
			put(helper, "cable_industrial", x, U, 2);                              // to the pump's west side
		}
		helper.runAfterDelay(20, () -> {
			check(helper, furnace.waterLevel() < 0.01, "the furnace should start dry: " + furnace.inspect().getString());
			if (level(helper).getBlockEntity(GensetSkid.base(abs(helper, -10, U, 6), NORTH, false)) instanceof dev.wildcatter.block.GensetPartBlockEntity tank) {
				tank.setFuel(2000);
			}
			panel(helper, -10, 6).setWantRun(true);
		});
		helper.runAfterDelay(500, () -> {
			String f = furnace.inspect().getString();
			String p = read(helper, -3, 2);
			log("cracker_pump", "furnace | " + f + " | pump | " + p + " | set | " + read(helper, -10, 6));
			check(helper, furnace.waterLevel() > 0.95, "the pump should fill the furnace's water: " + f + " | pump " + p + " | set " + read(helper, -10, 6));
			log("cracker_pump", "ok");
			helper.succeed();
		});
	}

	// ---- alternator_frames ---------------------------------------------------------------------------------------------------------------
	static void alternatorFrames(GameTestHelper helper) {
		floor(helper, -2, -2, 10, 10);
		GensetSkid.build(level(helper), abs(helper, 8, U, 0), NORTH, true);        // gas: alternator at x 7
		GensetSkid.build(level(helper), abs(helper, 8, U, 6), NORTH, false);       // diesel: alternator at x 7
		helper.runAfterDelay(60, () -> {
			GensetPartBlock.Alternator.Frame gas = frame(helper, 7, 0);
			GensetPartBlock.Alternator.Frame diesel = frame(helper, 7, 6);
			log("alternator_frames", "gas " + gas + " | diesel " + diesel);
			check(helper, gas == GensetPartBlock.Alternator.Frame.MW1, "the gas set's alternator should be drawn at the 1 MW frame: " + gas);
			check(helper, diesel == GensetPartBlock.Alternator.Frame.KW500, "the diesel set's alternator should be drawn at the 500 kW frame: " + diesel);
			log("alternator_frames", "ok");
			helper.succeed();
		});
	}

	/** The frame of the alternator whose cells include (x, U, z) or (x, U + 1, z), or null. */
	private static GensetPartBlock.Alternator.Frame frame(GameTestHelper helper, int x, int z) {
		for (int y = U; y <= U + 2; y++) {
			for (int dz = 0; dz <= 1; dz++) {
				BlockState s = level(helper).getBlockState(abs(helper, x, y, z + dz));
				if (s.getBlock() instanceof GensetPartBlock.Alternator) {
					return s.getValue(GensetPartBlock.Alternator.FRAME);
				}
			}
		}
		return null;
	}

	// ---- drawn_runs ---------------------------------------------------------------------------------------------------------------------
	static void drawnRuns(GameTestHelper helper) {
		floor(helper, -3, -5, 54, 18);
		BlockPos gen = abs(helper, 2, U, 0);
		SteamPlantLayout.layout().build(level(helper), gen, NORTH, 0);
		BlockPos sub = abs(helper, 30, U, 0);
		LandRig.layout().build(level(helper), sub, NORTH, 0);
		helper.runAfterDelay(60, () -> {
			TurbineGeneratorBlockEntity plant = (TurbineGeneratorBlockEntity) level(helper).getBlockEntity(gen);
			RigSubstructureBlockEntity rig = (RigSubstructureBlockEntity) level(helper).getBlockEntity(sub);
			check(helper, plant.formed() && plant.drawnRuns().size() == 2, "a formed plant should draw its feedwater line and its posts: " + plant.formed() + ", "
					+ plant.drawnRuns().size() + " | " + plant.inspect().getString());
			check(helper, rig.formed() && rig.drawnRuns().size() == 5, "a formed rig should draw its two trays and three cable drops: " + rig.formed() + ", "
					+ rig.drawnRuns().size() + " | " + rig.inspect().getString());
			level(helper).destroyBlock(gen.offset(0, 0, 2), false);                // the feed pump
			level(helper).destroyBlock(sub.offset(14, 0, 4), false);               // the second mud pump
			helper.runAfterDelay(60, () -> {
				check(helper, !plant.formed() && plant.drawnRuns().isEmpty(), "without its feed pump the plant should draw no line: " + plant.drawnRuns().size());
				check(helper, !rig.formed() && rig.drawnRuns().isEmpty(), "without a mud pump the rig should draw no cables: " + rig.drawnRuns().size());
				log("drawn_runs", "ok");
				helper.succeed();
			});
		});
	}
}
