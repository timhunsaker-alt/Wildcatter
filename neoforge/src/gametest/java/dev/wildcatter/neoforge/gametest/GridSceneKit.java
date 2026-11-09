package dev.wildcatter.neoforge.gametest;

import dev.wildcatter.Wildcatter;
import dev.wildcatter.block.GensetControlPanelBlockEntity;
import dev.wildcatter.block.GensetPartBlockEntity;
import dev.wildcatter.block.GensetSkid;
import dev.wildcatter.energy.Inspectable;
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
 * Helpers shared by the protection gap wave's server scenes (the same ones {@link AmmoniaPlantScenes} uses, made public to the
 * package). Coordinates are relative to the test's origin; {@link #floor} force-loads the chunks a scene spreads over, because the
 * framework forces only its own one-block structure's chunk.
 */
final class GridSceneKit {
	private GridSceneKit() {
	}

	static void check(GameTestHelper helper, boolean ok, String message) {
		helper.assertTrue(ok, Component.literal(message));
	}

	static BlockPos abs(GameTestHelper helper, int x, int y, int z) {
		return helper.absolutePos(new BlockPos(x, y, z));
	}

	/** Places a big part (or any horizontal-facing block) with its main cell at (x, y, z), facing {@code facing}, and grows it. */
	static void place(GameTestHelper helper, Block block, int x, int y, int z, Direction facing) {
		BlockPos main = abs(helper, x, y, z);
		BlockState state = block.defaultBlockState();
		if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
		}
		helper.getLevel().setBlock(main, state, 3);
		BigParts.grow(helper.getLevel(), main, state);
	}

	/** Places the block {@code wildcatter:<id>} in its default state. */
	static void put(GameTestHelper helper, String id, int x, int y, int z) {
		helper.getLevel().setBlock(abs(helper, x, y, z), BuiltInRegistries.BLOCK.getValue(Wildcatter.id(id)).defaultBlockState(), 3);
	}

	static void air(GameTestHelper helper, int x, int y, int z) {
		helper.getLevel().setBlock(abs(helper, x, y, z), Blocks.AIR.defaultBlockState(), 3);
	}

	/** The block entity at (x, y, z), or the main cell's for a piece of a big part. */
	@SuppressWarnings("unchecked")
	static <T> T entity(GameTestHelper helper, int x, int y, int z, Class<T> kind) {
		BlockEntity e = BigParts.entity(helper.getLevel(), abs(helper, x, y, z));
		check(helper, kind.isInstance(e), "expected a " + kind.getSimpleName() + " at " + x + ", " + y + ", " + z + ": " + e);
		return (T) e;
	}

	static String read(GameTestHelper helper, int x, int y, int z) {
		return BigParts.entity(helper.getLevel(), abs(helper, x, y, z)) instanceof Inspectable i ? i.inspect().getString() : "(no readout)";
	}

	/** Lays a stone floor at y 0 over (x1, z1)..(x2, z2) and force-loads its chunks (the runner releases them after the batch). */
	static void floor(GameTestHelper helper, int x1, int z1, int x2, int z2) {
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

	/** A 500 kW diesel set with its panel at (x, y, z) facing north: the skid to its west, its 480 V terminal the cell east of it. */
	static void genset(GameTestHelper helper, int x, int y, int z) {
		GensetSkid.build(helper.getLevel(), abs(helper, x, y, z), Direction.NORTH, false);
	}

	static GensetControlPanelBlockEntity gensetPanel(GameTestHelper helper, int x, int y, int z) {
		return (GensetControlPanelBlockEntity) helper.getLevel().getBlockEntity(abs(helper, x, y, z));
	}

	/** Fuels and starts the set once its panel has found its parts; {@code then} runs once it is up with its breaker closed. */
	static void start(GameTestHelper helper, int x, int y, int z, Runnable then) {
		helper.runAfterDelay(30, () -> {
			if (helper.getLevel().getBlockEntity(GensetSkid.base(abs(helper, x, y, z), Direction.NORTH, false)) instanceof GensetPartBlockEntity tank) {
				tank.setFuel(2000);
			}
			gensetPanel(helper, x, y, z).setWantRun(true);
			helper.runAfterDelay(260, () -> {
				String state = read(helper, x, y, z);
				check(helper, state.contains("RUNNING") && state.contains("breaker CLOSED"), "the genset should be running with its breaker closed: " + state);
				then.run();
			});
		});
	}

	static boolean near(double got, double want, double share) {
		return Math.abs(got - want) <= Math.abs(want) * share;
	}

	static void log(String scene, String text) {
		System.out.println("[wildcatter-neoforge-test] " + scene + " " + text);
	}
}
