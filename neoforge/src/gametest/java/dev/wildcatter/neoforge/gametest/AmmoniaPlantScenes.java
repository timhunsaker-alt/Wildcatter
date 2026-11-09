package dev.wildcatter.neoforge.gametest;

import dev.wildcatter.Wildcatter;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.block.AmmoniaConverterBlockEntity;
import dev.wildcatter.block.AnNeutraliserBlockEntity;
import dev.wildcatter.block.EmulsionBlenderBlockEntity;
import dev.wildcatter.block.GensetControlPanelBlockEntity;
import dev.wildcatter.block.GensetPartBlockEntity;
import dev.wildcatter.block.GensetSkid;
import dev.wildcatter.block.NitricAcidUnitBlockEntity;
import dev.wildcatter.block.NitrogenGeneratorBlockEntity;
import dev.wildcatter.block.StorageTankBlockEntity;
import dev.wildcatter.block.SyngasCompressorBlockEntity;
import dev.wildcatter.block.WaterUser;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.fluid.AmmoniaPlant;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.multiblock.BigParts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The ammonia plant (docs/multiblocks/ammonia-plant.md), in three scenes as the spec lists them. Each builds its parts at true size
 * facing north at y 2 (main cell at the north-west bottom corner), lays steel pipe and 480 V cable, starts a diesel set, then checks
 * each stage makes its product at the spec's rate and stops without its feed or its power.
 * <ul>
 * <li>{@code ammonia_loop}: a steam reformer on LPG and a nitrogen generator feed the syngas compressor; the converter against it
 * fills a bullet with liquid ammonia at about 15.2 kg/h. Without hydrogen the compressor reads NO HYDROGEN; without power the
 * converter reads COLD and the generator NO AIR.</li>
 * <li>{@code nitric_acid}: ammonia from a bullet to the nitric acid unit, 60 % acid to a stock tank at 27 kg/h of HNO3; the gauze loses
 * platinum. Without power NO AIR; with the pack worn and none spare, NO GAUZE.</li>
 * <li>{@code an_emulsion}: acid from a stock tank and ammonia from a bullet to the neutraliser, 34.6 kg/h of AN; its solution, diesel
 * and emulsifier to the blender, about 46 kg/h of emulsion matrix into a stock tank. Without diesel NO DIESEL; without acid NO ACID.</li>
 * </ul>
 */
final class AmmoniaPlantScenes {
	private AmmoniaPlantScenes() {
	}

	private static final int U = 2;   // the plant's level
	private static final Direction NORTH = Direction.NORTH;

	// ---- helpers --------------------------------------------------------------------------------------------------------------------
	private static void check(GameTestHelper helper, boolean ok, String message) {
		helper.assertTrue(ok, Component.literal(message));
	}

	private static BlockPos abs(GameTestHelper helper, int x, int y, int z) {
		return helper.absolutePos(new BlockPos(x, y, z));
	}

	private static void place(GameTestHelper helper, Block block, int x, int z) {
		BlockPos main = abs(helper, x, U, z);
		BlockState state = block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, NORTH);
		helper.getLevel().setBlock(main, state, 3);
		BigParts.grow(helper.getLevel(), main, state);
	}

	private static void put(GameTestHelper helper, String id, int x, int y, int z) {
		helper.getLevel().setBlock(abs(helper, x, y, z), BuiltInRegistries.BLOCK.getValue(Wildcatter.id(id)).defaultBlockState(), 3);
	}

	private static void pipe(GameTestHelper helper, int[]... cells) {
		for (int[] c : cells) {
			put(helper, "gas_line", c[0], U + c[1], c[2]);
		}
	}

	private static void cable(GameTestHelper helper, int x, int y, int z) {
		put(helper, "cable_industrial", x, y, z);
	}

	private static void air(GameTestHelper helper, int x, int z) {
		helper.getLevel().setBlock(abs(helper, x, U, z), Blocks.AIR.defaultBlockState(), 3);
	}

	@SuppressWarnings("unchecked")
	private static <T> T entity(GameTestHelper helper, int x, int z, Class<T> kind) {
		BlockEntity e = BigParts.entity(helper.getLevel(), abs(helper, x, U, z));
		check(helper, kind.isInstance(e), "expected a " + kind.getSimpleName() + " at " + x + ", " + z + ": " + e);
		return (T) e;
	}

	private static String read(GameTestHelper helper, int x, int z) {
		return BigParts.entity(helper.getLevel(), abs(helper, x, U, z)) instanceof Inspectable i ? i.inspect().getString() : "(no readout)";
	}

	/**
	 * Lays a stone floor under the scene and force-loads its chunks: the framework forces only the chunks of the test's own (one-block)
	 * structure, and a block entity in a chunk that is not ticking does nothing. The runner releases every forced chunk after the batch.
	 */
	private static void floor(GameTestHelper helper, int x1, int z1, int x2, int z2) {
		BlockPos a = abs(helper, x1, 0, z1);
		BlockPos b = abs(helper, x2, 0, z2);
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

	private static void fill(GameTestHelper helper, int x, int z, Product product, double amount) {
		entity(helper, x, z, StorageTankBlockEntity.class).testFill(product, amount);
	}

	private static void water(GameTestHelper helper, int x, int z) {
		entity(helper, x, z, WaterUser.class).addWater(100_000);
	}

	/** A 500 kW diesel set with its panel at (x, U, z) facing north: the skid to its west, its 480 V terminal the cell east of it. */
	private static void genset(GameTestHelper helper, int x, int z) {
		GensetSkid.build(helper.getLevel(), abs(helper, x, U, z), NORTH, false);
	}

	private static GensetControlPanelBlockEntity panel(GameTestHelper helper, int x, int z) {
		return (GensetControlPanelBlockEntity) helper.getLevel().getBlockEntity(abs(helper, x, U, z));
	}

	/** Fuels and starts the set once its panel has found its parts; {@code then} runs once it is up with its breaker closed. */
	private static void start(GameTestHelper helper, int x, int z, Runnable then) {
		helper.runAfterDelay(30, () -> {
			if (helper.getLevel().getBlockEntity(GensetSkid.base(abs(helper, x, U, z), NORTH, false)) instanceof GensetPartBlockEntity tank) {
				tank.setFuel(2000);
			}
			panel(helper, x, z).setWantRun(true);
			helper.runAfterDelay(260, () -> {
				String state = read(helper, x, z);
				check(helper, state.contains("RUNNING") && state.contains("breaker CLOSED"), "the genset should be running with its breaker closed: " + state);
				then.run();
			});
		});
	}

	private static boolean near(double got, double want, double share) {
		return Math.abs(got - want) <= want * share;
	}

	private static void log(String scene, String text) {
		System.out.println("[wildcatter-neoforge-test] " + scene + " " + text);
	}

	// ---- ammonia_loop -----------------------------------------------------------------------------------------------------------------
	static void ammoniaLoop(GameTestHelper helper) {
		floor(helper, -2, -14, 33, 28);
		place(helper, WildcatterBlocks.STEAM_REFORMER, 13, 0);       // x 13-15, z 0-3
		place(helper, WildcatterBlocks.LPG_BULLET, 17, -10);         // x 17-19, z -10..1: its LPG
		place(helper, WildcatterBlocks.NITROGEN_GENERATOR, 13, 7);   // x 13-14, z 7-9
		place(helper, WildcatterBlocks.SYNGAS_COMPRESSOR, 8, 6);     // x 8-10, z 6-10
		place(helper, WildcatterBlocks.AMMONIA_CONVERTER, 8, 11);    // x 8-10, z 11-13, against the compressor
		place(helper, WildcatterBlocks.LPG_BULLET, 8, 15);           // x 8-10, z 15-26: the ammonia bullet
		pipe(helper, new int[] {16, 0, 0});                                                              // LPG to the reformer's east side
		pipe(helper, new int[] {14, 0, 4}, new int[] {14, 0, 5}, new int[] {13, 0, 5}, new int[] {12, 0, 5}, new int[] {11, 0, 5}, new int[] {11, 0, 6});   // hydrogen
		pipe(helper, new int[] {13, 0, 10}, new int[] {12, 0, 10}, new int[] {11, 0, 10});               // nitrogen
		pipe(helper, new int[] {9, 0, 14});                                                              // liquid ammonia to the bullet
		genset(helper, 30, -3);                                                                          // skid x 25-30, z -3..-2
		for (int z = -12; z <= -3; z++) {
			cable(helper, 31, U, z);
		}
		for (int x = 7; x <= 30; x++) {
			cable(helper, x, U, -12);
		}
		for (int z = -11; z <= 11; z++) {
			cable(helper, 7, U, z);                                                                      // the spine past the compressor's and converter's boxes
		}
		for (int x = 8; x <= 12; x++) {
			cable(helper, x, U, -1);
		}
		for (int z = 0; z <= 4; z++) {
			cable(helper, 12, U, z);
		}
		cable(helper, 12, U + 1, 4);
		cable(helper, 12, U + 1, 5);                                                                     // over the hydrogen pipe
		cable(helper, 12, U + 1, 6);
		cable(helper, 12, U, 6);
		cable(helper, 12, U, 7);                                                                         // the nitrogen generator's box
		fill(helper, 17, -10, Product.LPG, 20_000);
		water(helper, 13, 0);
		start(helper, 30, -3, () -> helper.runAfterDelay(700, () -> {
			NitrogenGeneratorBlockEntity n2 = entity(helper, 13, 7, NitrogenGeneratorBlockEntity.class);
			SyngasCompressorBlockEntity compressor = entity(helper, 8, 6, SyngasCompressorBlockEntity.class);
			AmmoniaConverterBlockEntity converter = entity(helper, 8, 11, AmmoniaConverterBlockEntity.class);
			StorageTankBlockEntity bullet = entity(helper, 8, 15, StorageTankBlockEntity.class);
			String chain = " | reformer " + read(helper, 13, 0) + " | n2 " + read(helper, 13, 7) + " | compressor " + read(helper, 8, 6) + " | converter " + read(helper, 8, 11)
					+ " | bullet " + read(helper, 8, 15);
			check(helper, near(n2.making(), 10, 0.05), "the nitrogen generator should make 10 Nm3/h: " + n2.making() + chain);
			check(helper, near(compressor.making(), SyngasCompressorBlockEntity.ratedNm3h(), 0.10), "the compressor should take 40 Nm3/h of syngas: " + compressor.making() + chain);
			check(helper, near(converter.making(), AmmoniaPlant.ammoniaKgh(), 0.10), "the converter should make about 15.2 kg/h of ammonia: " + converter.making() + chain);
			check(helper, bullet.product() == Product.AMMONIA && bullet.amount() > 0.05, "liquid ammonia should be in the bullet: " + chain);
			log("ammonia_loop", "running:" + chain);
			air(helper, 14, 4);                                                                          // the hydrogen pipe cut
			helper.runAfterDelay(40, () -> {
				String noH2 = read(helper, 8, 6);
				check(helper, noH2.contains("NO HYDROGEN"), "without hydrogen the compressor should stop: " + noH2);
				panel(helper, 30, -3).setWantRun(false);                                                 // and the power off
				helper.runAfterDelay(200, () -> {
					String cold = read(helper, 8, 11);
					String noAir = read(helper, 13, 7);
					check(helper, cold.contains("COLD") && converter.making() == 0, "without power the converter should stop: " + cold);
					check(helper, noAir.contains("NO AIR") || noAir.contains("FULL"), "without power the generator should stop: " + noAir);
					log("ammonia_loop", "ok: stops on " + noH2 + " | " + cold);
					helper.succeed();
				});
			});
		}));
	}

	// ---- nitric_acid -------------------------------------------------------------------------------------------------------------------
	static void nitricAcid(GameTestHelper helper) {
		floor(helper, -10, -14, 10, 12);
		place(helper, WildcatterBlocks.NITRIC_ACID_UNIT, 0, 0);       // x 0-3, z 0-3
		place(helper, WildcatterBlocks.LPG_BULLET, 5, -12);           // x 5-7, z -12..-1: the ammonia
		place(helper, WildcatterBlocks.STOCK_TANK, 0, 6);             // x 0-3, z 6-9: the acid
		pipe(helper, new int[] {4, 0, 0}, new int[] {4, 0, -1});       // ammonia to the unit's east side
		pipe(helper, new int[] {1, 0, 4}, new int[] {1, 0, 5});        // acid to the tank
		genset(helper, -3, -6);                                        // skid x -8..-3, z -6..-5
		cable(helper, -2, U, -6);
		for (int z = -6; z <= 0; z++) {
			cable(helper, -1, U, z);
		}
		fill(helper, 5, -12, Product.AMMONIA, 5_000);
		water(helper, 0, 0);
		NitricAcidUnitBlockEntity unit = entity(helper, 0, 0, NitricAcidUnitBlockEntity.class);
		unit.slots().set(0, new ItemStack(WildcatterItems.GAUZE_PACK, 1));
		start(helper, -3, -6, () -> helper.runAfterDelay(400, () -> {
			StorageTankBlockEntity tank = entity(helper, 0, 6, StorageTankBlockEntity.class);
			String chain = " | unit " + read(helper, 0, 0) + " | tank " + read(helper, 0, 6);
			check(helper, near(unit.making(), AmmoniaPlant.hno3Kgh(), 0.10), "the unit should make about 27 kg/h of HNO3 (45 kg/h of 60 % acid): " + unit.making() + chain);
			check(helper, tank.product() == Product.NITRIC_ACID && tank.amount() > 0.05, "60 % acid should be in the tank: " + chain);
			check(helper, unit.gauze() < 20 && unit.gauze() > 19.99, "the gauze should lose a little platinum: " + unit.gauze() + chain);
			log("nitric_acid", "running:" + chain);
			panel(helper, -3, -6).setWantRun(false);
			helper.runAfterDelay(200, () -> {
				String noAir = read(helper, 0, 0);
				check(helper, noAir.contains("NO AIR") && unit.making() == 0, "without power the unit should stop: " + noAir);
				unit.testWear(0.25);                                       // the pack worn, none spare
				helper.runAfterDelay(20, () -> {
					String noGauze = read(helper, 0, 0);
					check(helper, noGauze.contains("NO GAUZE"), "with its pack worn and none spare the unit should stop: " + noGauze);
					log("nitric_acid", "ok: stops on " + noAir + " | " + noGauze);
					helper.succeed();
				});
			});
		}));
	}

	// ---- an_emulsion -------------------------------------------------------------------------------------------------------------------
	static void anEmulsion(GameTestHelper helper) {
		floor(helper, -16, -14, 9, 17);
		place(helper, WildcatterBlocks.AN_NEUTRALISER, 0, 0);         // x 0-2, z 0-2
		place(helper, WildcatterBlocks.LPG_BULLET, 4, -12);           // x 4-6, z -12..-1: the ammonia
		place(helper, WildcatterBlocks.STOCK_TANK, 4, 3);             // x 4-7, z 3-6: the acid
		place(helper, WildcatterBlocks.EMULSION_BLENDER, -6, 6);      // x -6..-4, z 6-9
		place(helper, WildcatterBlocks.STOCK_TANK, -2, 9);            // x -2..1, z 9-12: the diesel
		place(helper, WildcatterBlocks.STOCK_TANK, -7, 12);           // x -7..-4, z 12-15: the matrix
		pipe(helper, new int[] {3, 0, 0}, new int[] {3, 0, -1});                                          // ammonia
		pipe(helper, new int[] {3, 0, 2}, new int[] {3, 0, 3});                                           // acid
		pipe(helper, new int[] {1, 0, 3}, new int[] {1, 0, 4}, new int[] {1, 0, 5}, new int[] {0, 0, 5}, new int[] {-1, 0, 5}, new int[] {-2, 0, 5},
				new int[] {-3, 0, 5}, new int[] {-3, 0, 6});                                                     // AN solution to the blender
		pipe(helper, new int[] {-3, 0, 9});                                                               // diesel
		pipe(helper, new int[] {-5, 0, 10}, new int[] {-5, 0, 11});                                       // matrix to its tank
		genset(helper, -10, -6);                                                                          // skid x -15..-10, z -6..-5
		cable(helper, -9, U, -6);
		cable(helper, -8, U, -6);
		for (int z = -6; z <= 6; z++) {
			cable(helper, -7, U, z);
		}
		fill(helper, 4, -12, Product.AMMONIA, 5_000);
		fill(helper, 4, 3, Product.NITRIC_ACID, 5_000);
		fill(helper, -2, 9, Product.DIESEL, 5_000);
		water(helper, -6, 6);
		EmulsionBlenderBlockEntity blender = entity(helper, -6, 6, EmulsionBlenderBlockEntity.class);
		blender.slots().set(0, new ItemStack(WildcatterItems.EMULSIFIER_PAIL, 2));
		AnNeutraliserBlockEntity neutraliser = entity(helper, 0, 0, AnNeutraliserBlockEntity.class);
		helper.runAfterDelay(100, () -> {   // before the blender takes any: the neutraliser runs at its rating into its own hold
			check(helper, near(neutraliser.making(), AmmoniaPlant.anKgh(), 0.05), "the neutraliser should make about 34.6 kg/h of AN: " + neutraliser.making() + " | " + read(helper, 0, 0));
		});
		start(helper, -10, -6, () -> helper.runAfterDelay(500, () -> {
			StorageTankBlockEntity matrix = entity(helper, -7, 12, StorageTankBlockEntity.class);
			String chain = " | neutraliser " + read(helper, 0, 0) + " | blender " + read(helper, -6, 6) + " | matrix tank " + read(helper, -7, 12);
			check(helper, near(blender.making(), AmmoniaPlant.matrixKgh(), 0.10), "the blender should make about 46 kg/h of matrix: " + blender.making() + chain);
			check(helper, near(neutraliser.making(), AmmoniaPlant.anKgh(), 0.15), "the neutraliser should keep up with the blender: " + neutraliser.making() + chain);
			check(helper, matrix.product() == Product.EMULSION_MATRIX && matrix.amount() > 0.05, "emulsion matrix should be in its tank: " + chain);
			log("an_emulsion", "running:" + chain);
			air(helper, -3, 9);                                       // the diesel cut
			air(helper, 3, 2);                                        // and the acid
			helper.runAfterDelay(20, () -> {
				String noDiesel = read(helper, -6, 6);
				String noAcid = read(helper, 0, 0);
				check(helper, noDiesel.contains("NO DIESEL"), "without diesel the blender should stop: " + noDiesel);
				check(helper, noAcid.contains("NO ACID"), "without acid the neutraliser should stop: " + noAcid);
				log("an_emulsion", "ok: stops on " + noDiesel + " | " + noAcid);
				helper.succeed();
			});
		}));
	}
}
