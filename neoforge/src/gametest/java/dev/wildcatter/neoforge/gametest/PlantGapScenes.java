package dev.wildcatter.neoforge.gametest;

import dev.wildcatter.Wildcatter;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.QuenchTowerBlockEntity;
import dev.wildcatter.block.StorageTankBlockEntity;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.multiblock.BigParts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The plant and oilfield gaps closed in the plant-crane-gaps wave (docs/questions/plant-crane-gaps.md), on the server. Each builds
 * its parts facing north at y 1 on a stone floor, as AmmoniaPlantScenes does.
 */
final class PlantGapScenes {
	private PlantGapScenes() {
	}

	static final int U = 1;

	static void check(GameTestHelper helper, boolean ok, String message) {
		helper.assertTrue(ok, Component.literal(message));
	}

	static BlockPos abs(GameTestHelper helper, int x, int y, int z) {
		return helper.absolutePos(new BlockPos(x, y, z));
	}

	static void place(GameTestHelper helper, Block block, int x, int z) {
		place(helper, block, x, U, z, Direction.NORTH);
	}

	static void place(GameTestHelper helper, Block block, int x, int y, int z, Direction facing) {
		BlockPos main = abs(helper, x, y, z);
		BlockState state = block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
		helper.getLevel().setBlock(main, state, 3);
		BigParts.grow(helper.getLevel(), main, state);
	}

	static void put(GameTestHelper helper, String id, int x, int y, int z) {
		helper.getLevel().setBlock(abs(helper, x, y, z), BuiltInRegistries.BLOCK.getValue(Wildcatter.id(id)).defaultBlockState(), 3);
	}

	@SuppressWarnings("unchecked")
	static <T> T entity(GameTestHelper helper, int x, int y, int z, Class<T> kind) {
		BlockEntity e = BigParts.entity(helper.getLevel(), abs(helper, x, y, z));
		check(helper, kind.isInstance(e), "expected a " + kind.getSimpleName() + " at " + x + ", " + y + ", " + z + ": " + e);
		return (T) e;
	}

	static String read(GameTestHelper helper, int x, int y, int z) {
		return BigParts.entity(helper.getLevel(), abs(helper, x, y, z)) instanceof Inspectable i ? i.inspect().getString() : "(no readout)";
	}

	/** A stone floor at y 0, the air above it cleared, its chunks force-loaded (AmmoniaPlantScenes.floor). */
	static void floor(GameTestHelper helper, int x1, int z1, int x2, int z2) {
		BlockPos a = abs(helper, x1, 0, z1), b = abs(helper, x2, 0, z2);
		for (int cx = Math.min(a.getX(), b.getX()) >> 4; cx <= Math.max(a.getX(), b.getX()) >> 4; cx++) {
			for (int cz = Math.min(a.getZ(), b.getZ()) >> 4; cz <= Math.max(a.getZ(), b.getZ()) >> 4; cz++) {
				helper.getLevel().setChunkForced(cx, cz, true);
			}
		}
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				helper.getLevel().setBlock(abs(helper, x, 0, z), Blocks.STONE.defaultBlockState(), 3);
			}
		}
	}

	static void log(String scene, String text) {
		System.out.println("[wildcatter-neoforge-test] " + scene + " " + text);
	}

	// ---- quench_sump --------------------------------------------------------------------------------------------------------------
	/**
	 * The cracker's quench tower with its sump full reads FULL; piped away, its pyrolysis gasoline (east face) and fuel oil (west
	 * face) run into two stock tanks and it is free to quench again.
	 */
	static void quenchSump(GameTestHelper helper) {
		floor(helper, -8, -4, 9, 6);
		place(helper, WildcatterBlocks.QUENCH_TOWER, 0, 0);   // x 0-1, z 0-1
		QuenchTowerBlockEntity quench = entity(helper, 0, U, 0, QuenchTowerBlockEntity.class);
		quench.testFillSump();
		place(helper, WildcatterBlocks.STOCK_TANK, 3, -1);    // x 3-6, z -1..2
		place(helper, WildcatterBlocks.STOCK_TANK, -5, -1);   // x -5..-2
		helper.runAfterDelay(40, () -> {
			String full = read(helper, 0, U, 0);
			log("quench_sump", "full | " + full);
			check(helper, full.contains("FULL") && full.contains("2000.0 L"), "a full sump should stop the quench: " + full);
			put(helper, "gas_line", 2, U, 0);
			put(helper, "gas_line", -1, U, 0);
			helper.runAfterDelay(400, () -> {
				StorageTankBlockEntity east = entity(helper, 3, U, -1, StorageTankBlockEntity.class);
				StorageTankBlockEntity west = entity(helper, -5, U, -1, StorageTankBlockEntity.class);
				String after = read(helper, 0, U, 0);
				log("quench_sump", "piped | " + after + " | east " + east.inspect().getString() + " | west " + west.inspect().getString());
				check(helper, east.product() == Product.GASOLINE && east.amount() > 10, "pyrolysis gasoline should run east into a stock tank: " + east.inspect().getString());
				check(helper, west.product() == Product.RESIDUE && west.amount() > 10, "fuel oil should run west into a stock tank: " + west.inspect().getString());
				check(helper, !after.contains("FULL"), "with its sump drawn down it should quench again: " + after);
				log("quench_sump", "ok");
				helper.succeed();
			});
		});
	}

	// ---- stabilizer_fuel ------------------------------------------------------------------------------------------------------------
	/**
	 * The gas plant's stabilizer fires its reboiler: on its own LPG with no fuel piped, then on LPG piped from a bullet to the west face
	 * of its base.
	 */
	static void stabilizerFuel(GameTestHelper helper) {
		floor(helper, -6, -8, 4, 6);
		place(helper, WildcatterBlocks.STABILIZER, 0, 0);    // x 0-1, z 0-1
		place(helper, WildcatterBlocks.LPG_BULLET, -4, -6);  // x -4..-2, z -6..5
		entity(helper, -4, U, -6, StorageTankBlockEntity.class).testFill(Product.LPG, 5_000);
		dev.wildcatter.block.StabilizerBlockEntity st = entity(helper, 0, U, 0, dev.wildcatter.block.StabilizerBlockEntity.class);
		st.testFeed(20);
		helper.runAfterDelay(30, () -> {
			String own = read(helper, 0, U, 0);
			log("stabilizer_fuel", "no fuel piped | " + own);
			check(helper, own.contains("splitting") && own.contains("its own LPG"), "with no fuel piped its reboiler should burn its own LPG: " + own);
			put(helper, "gas_line", -1, U, 0);
			st.testFeed(20);
			helper.runAfterDelay(30, () -> {
				String piped = read(helper, 0, U, 0);
				log("stabilizer_fuel", "fuel piped | " + piped);
				check(helper, piped.contains("splitting") && piped.contains("piped fuel"), "with LPG on its west face its reboiler should burn that: " + piped);
				log("stabilizer_fuel", "ok");
				helper.succeed();
			});
		});
	}

	// ---- cog_engine -------------------------------------------------------------------------------------------------------------------
	/**
	 * A gas engine set runs on coke oven gas from a by-product plant's outlet, counted by its heat (18 MJ/m3 against natural gas's 38):
	 * it starts, closes its breaker and names its fuel.
	 */
	static void cogEngine(GameTestHelper helper) {
		floor(helper, -14, -16, 6, 8);
		BlockPos panelAt = abs(helper, 0, U, 0);
		java.util.List<BlockPos> set = dev.wildcatter.block.GensetSkid.build(helper.getLevel(), panelAt, Direction.NORTH, true);
		BlockPos engine = null;
		StringBuilder seen = new StringBuilder();
		for (BlockPos cell : BlockPos.betweenClosed(panelAt.offset(-10, 0, -3), panelAt.offset(2, 0, 4))) {
			if (BigParts.entity(helper.getLevel(), cell) instanceof dev.wildcatter.block.GensetPartBlockEntity part) {
				if (seen.indexOf(part.part().name()) < 0) {
					seen.append(part.part().name()).append(' ');
				}
				if (part.part() == dev.wildcatter.block.GensetPartBlock.Part.GAS_ENGINE && (engine == null || cell.getZ() < engine.getZ())) {
					engine = cell.immutable();
				}
			}
		}
		check(helper, engine != null, "the gas set should have a gas engine: " + set.size() + " parts, " + seen);
		BlockPos pipe = engine.north();
		BlockPos plantMain = pipe.offset(-2, 0, -8);   // x pipe-2..pipe+2, z pipe-8..pipe-1: its south row against the pipe
		BlockState plantState = WildcatterBlocks.BYPRODUCT_PLANT.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
		helper.getLevel().setBlock(plantMain, plantState, 3);
		BigParts.grow(helper.getLevel(), plantMain, plantState);
		dev.wildcatter.block.ByproductPlantBlockEntity plant = (dev.wildcatter.block.ByproductPlantBlockEntity) helper.getLevel().getBlockEntity(plantMain);
		plant.testAdd(Product.COKE_OVEN_GAS, 1_000);
		helper.getLevel().setBlock(pipe, net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(Wildcatter.id("gas_line")).defaultBlockState(), 3);
		helper.runAfterDelay(30, () -> {
			dev.wildcatter.block.GensetControlPanelBlockEntity panel = (dev.wildcatter.block.GensetControlPanelBlockEntity) helper.getLevel().getBlockEntity(panelAt);
			panel.setWantRun(true);
			helper.runAfterDelay(300, () -> {
				String state = panel.inspect().getString();
				log("cog_engine", "running | " + state + " | plant " + plant.inspect().getString());
				check(helper, state.contains("RUNNING") && state.contains("coke oven gas"), "the gas set should run on coke oven gas: " + state);
				log("cog_engine", "ok");
				helper.succeed();
			});
		});
	}

	// ---- pot_aux ------------------------------------------------------------------------------------------------------------------------
	/** Fuels and starts a 500 kW diesel set with its panel at (x, U, z) facing north; {@code then} runs once its breaker is closed. */
	static void genset(GameTestHelper helper, int x, int z, Runnable then) {
		BlockPos panelAt = abs(helper, x, U, z);
		dev.wildcatter.block.GensetSkid.build(helper.getLevel(), panelAt, Direction.NORTH, false);
		helper.runAfterDelay(30, () -> {
			if (helper.getLevel().getBlockEntity(dev.wildcatter.block.GensetSkid.base(panelAt, Direction.NORTH, false)) instanceof dev.wildcatter.block.GensetPartBlockEntity tank) {
				tank.setFuel(2000);
			}
			dev.wildcatter.block.GensetControlPanelBlockEntity panel = (dev.wildcatter.block.GensetControlPanelBlockEntity) helper.getLevel().getBlockEntity(panelAt);
			panel.setWantRun(true);
			helper.runAfterDelay(260, () -> {
				String state = panel.inspect().getString();
				check(helper, state.contains("RUNNING") && state.contains("breaker CLOSED"), "the genset should be running with its breaker closed: " + state);
				then.run();
			});
		});
	}

	/**
	 * A pot's auxiliaries take 480 V at the front of its main cell: unfed they read so, fed from a diesel set they run. Frozen, a pot is
	 * cut out of the line with its shorting bars (sneak-right-click), and stays cut out.
	 */
	static void potAux(GameTestHelper helper) {
		floor(helper, -9, -5, 7, 13);
		place(helper, WildcatterBlocks.POT, 0, 0);   // x 0-5, z 0-11
		dev.wildcatter.block.PotBlockEntity pot = entity(helper, 0, U, 0, dev.wildcatter.block.PotBlockEntity.class);
		helper.runAfterDelay(20, () -> {
			boolean unfed = pot.auxPowered();
			log("pot_aux", "unfed | " + pot.inspect().getString());
			check(helper, !unfed, "a pot with no 480 V should not read its auxiliaries powered: " + pot.inspect().getString());
			for (int[] c : new int[][] {{-1, -3}, {-1, -2}, {-1, -1}, {0, -1}}) {
				put(helper, "cable_industrial", c[0], U, c[1]);
			}
			genset(helper, -2, -3, () -> helper.runAfterDelay(40, () -> {
				String fed = pot.inspect().getString();
				log("pot_aux", "fed | " + fed);
				check(helper, pot.auxPowered(), "fed 480 V at its front its auxiliaries should run: " + fed);
				pot.addCold(dev.wildcatter.Tuning.POT_FREEZE_SECONDS);
				helper.runAfterDelay(60, () -> {
					String frozen = pot.inspect().getString();
					log("pot_aux", "frozen | " + frozen);
					check(helper, pot.frozen() && frozen.contains("short it out"), "past 4 h without current it should freeze: " + frozen);
					String cut = pot.toggleCutOut().getString();
					helper.runAfterDelay(40, () -> {
						String out = pot.inspect().getString();
						String again = pot.toggleCutOut().getString();
						log("pot_aux", "cut out | " + cut + " | " + out + " | " + again);
						check(helper, pot.cutOut() && out.contains("CUT OUT, FROZEN") && again.contains("stays cut out"), "a frozen pot should be cut out and stay so: " + out + " | " + again);
						log("pot_aux", "ok");
						helper.succeed();
					});
				});
			}));
		});
	}

	// ---- burner_hopper ----------------------------------------------------------------------------------------------------------------
	/** A hopper over the far end of the steam plant's burner, not its main cell, feeds it coal. */
	static void burnerHopper(GameTestHelper helper) {
		floor(helper, -2, -2, 3, 4);
		place(helper, WildcatterBlocks.BOILER_BURNER, 0, 0);   // x 0, y 1-3, z 0-2
		BlockPos hopper = abs(helper, 0, U + 3, 2);
		helper.getLevel().setBlock(hopper, Blocks.HOPPER.defaultBlockState(), 3);
		((net.minecraft.world.Container) helper.getLevel().getBlockEntity(hopper)).setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL, 8));
		helper.runAfterDelay(100, () -> {
			String burner = read(helper, 0, U, 0);
			log("burner_hopper", burner);
			check(helper, burner.contains("8 x Coal"), "a hopper over the burner's far end should feed it: " + burner);
			log("burner_hopper", "ok");
			helper.succeed();
		});
	}
}
