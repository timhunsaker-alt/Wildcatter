package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.multiblock.BigPart;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Multiblock;
import dev.wildcatter.multiblock.MultiblockLayout;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The 1,000 hp land rig's layout (docs/multiblocks/land-rig.md): where each part's main cell goes, drawn facing north as offsets
 * from the hole (the ground cell under the rotary table). The substructure is the origin and the controller. Every part snaps
 * into its place when clicked within {@link #REACH} blocks of a substructure: on its floor or on the ground beside it.
 */
public final class LandRig {
	/** How far from a substructure a part clicked on the ground still finds its place. */
	public static final int REACH = 16;

	// each part's main cell from the hole, facing north (x east, y up, z south)
	static final int[] SUBSTRUCTURE = {-4, 0, -4};
	static final int[] MAST = {-1, 6, -1};
	static final int[] DRAWWORKS = {-3, 6, 2};
	static final int[] MUD_TANKS = {6, 0, -5};
	static final int[] MUD_PUMP_1 = {10, 0, -4};
	static final int[] MUD_PUMP_2 = {10, 0, 0};
	static final int[] DRIVE_HOUSE = {-6, 0, 7};

	private LandRig() {
	}

	private static final class Holder {
		static final MultiblockLayout RIG = grid();
		static final PartLayout LAYOUT = new PartLayout(WildcatterBlocks.RIG_SUBSTRUCTURE,
				Map.of('S', "substructure", 'A', "mast (on the floor)", 'W', "drawworks (on the floor)", 'T', "mud tanks",
						'P', "first mud pump", 'Q', "second mud pump", 'D', "drive house"),
				false,
				new PartLayout.Variant(RIG, Map.of('S', WildcatterBlocks.RIG_SUBSTRUCTURE, 'A', WildcatterBlocks.RIG_MAST,
						'W', WildcatterBlocks.RIG_DRAWWORKS, 'T', WildcatterBlocks.MUD_TANKS, 'P', WildcatterBlocks.MUD_PUMP,
						'Q', WildcatterBlocks.MUD_PUMP, 'D', WildcatterBlocks.RIG_DRIVE_HOUSE)));

		/** The layout's letters in a grid from x -6, z -5 (the hole at column 6, row 5), the ground and the drill floor. */
		private static MultiblockLayout grid() {
			char[][][] layers = new char[7][16][21];
			for (char[][] layer : layers) {
				for (char[] row : layer) {
					java.util.Arrays.fill(row, '.');
				}
			}
			Map<Character, int[]> mains = Map.of('S', SUBSTRUCTURE, 'A', MAST, 'W', DRAWWORKS, 'T', MUD_TANKS, 'P', MUD_PUMP_1,
					'Q', MUD_PUMP_2, 'D', DRIVE_HOUSE);
			mains.forEach((letter, at) -> layers[at[1]][at[2] + 5][at[0] + 6] = letter);
			String[][] out = new String[7][16];
			for (int y = 0; y < 7; y++) {
				for (int z = 0; z < 16; z++) {
					out[y][z] = new String(layers[y][z]);
				}
			}
			return new MultiblockLayout('S', out);
		}
	}

	public static PartLayout layout() {
		return Holder.LAYOUT;
	}

	/** {dx, dy, dz} turned {@code facing}'s way, as the footprints and layouts turn. */
	static int[] turn(int[] d, Direction facing) {
		int dx = d[0], dz = d[2];
		for (int i = 0; i < Multiblock.turns(facing); i++) {
			int t = dx;
			dx = -dz;
			dz = t;
		}
		return new int[] {dx, d[1], dz};
	}

	private static BlockPos from(BlockPos hole, int[] d, Direction facing) {
		int[] t = turn(d, facing);
		return hole.offset(t[0], t[1], t[2]);
	}

	/** The hole of a rig whose substructure's main cell is at {@code origin}. */
	public static BlockPos hole(BlockPos origin, Direction facing) {
		int[] t = turn(SUBSTRUCTURE, facing);
		return origin.offset(-t[0], -t[1], -t[2]);
	}

	public static BlockPos driveHouse(BlockPos origin, Direction facing) {
		return from(hole(origin, facing), DRIVE_HOUSE, facing);
	}

	public static BlockPos mudTanks(BlockPos origin, Direction facing) {
		return from(hole(origin, facing), MUD_TANKS, facing);
	}

	/** Where a part of this kind goes on the rig with its substructure at {@code origin}: one main cell, or two for the pumps. */
	static BlockPos[] places(Block part, BlockPos origin, Direction facing) {
		BlockPos hole = hole(origin, facing);
		if (part == WildcatterBlocks.MUD_PUMP) {
			return new BlockPos[] {from(hole, MUD_PUMP_1, facing), from(hole, MUD_PUMP_2, facing)};
		}
		return new BlockPos[] {from(hole, placeOf(part), facing)};
	}

	/** The substructure (main cell) of the rig this part, with its main cell at {@code main}, would belong to, or null. */
	public static @Nullable BlockPos controllerOf(BlockGetter level, Block part, BlockPos main, Direction facing) {
		for (int[] d : part == WildcatterBlocks.MUD_PUMP ? new int[][] {MUD_PUMP_1, MUD_PUMP_2} : new int[][] {placeOf(part)}) {
			int[] t = turn(d, facing);
			BlockPos hole = main.offset(-t[0], -t[1], -t[2]);
			BlockPos origin = from(hole, SUBSTRUCTURE, facing);
			BlockState state = level.getBlockState(origin);
			if (state.is(WildcatterBlocks.RIG_SUBSTRUCTURE) && ((BigPart) state.getBlock()).facing(state) == facing
					&& state.getValue(((BigPart) state.getBlock()).piece()) == 0) {
				return origin;
			}
		}
		return null;
	}

	private static int[] placeOf(Block part) {
		return part == WildcatterBlocks.RIG_SUBSTRUCTURE ? SUBSTRUCTURE : part == WildcatterBlocks.RIG_MAST ? MAST
				: part == WildcatterBlocks.RIG_DRAWWORKS ? DRAWWORKS : part == WildcatterBlocks.MUD_TANKS ? MUD_TANKS : DRIVE_HOUSE;
	}

	/** A substructure's main cell within reach of {@code pos}: straight under it (a part on the floor), or nearby on the ground. */
	static @Nullable BlockPos substructureNear(BlockGetter level, BlockPos pos) {
		BlockState under = level.getBlockState(pos.below());
		if (under.is(WildcatterBlocks.RIG_SUBSTRUCTURE)) {
			return BigParts.main(pos.below(), under);
		}
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int dy = -1; dy <= 0; dy++) {
			for (int dx = -REACH; dx <= REACH; dx++) {
				for (int dz = -REACH; dz <= REACH; dz++) {
					at.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
					BlockState state = level.getBlockState(at);
					if (state.is(WildcatterBlocks.RIG_SUBSTRUCTURE)) {
						return BigParts.main(at, state);
					}
				}
			}
		}
		return null;
	}

	/** Where a part clicked at {@code pos} goes and which way it faces: its place on the rig nearby, or null. */
	static PartLayout.@Nullable Slot slotFor(BlockGetter level, BlockPos pos, Block part) {
		BlockPos origin = substructureNear(level, pos);
		if (origin == null) {
			return null;
		}
		BlockState state = level.getBlockState(origin);
		Direction facing = ((BigPart) state.getBlock()).facing(state);
		BlockPos[] options = places(part, origin, facing);
		for (BlockPos main : options) {
			if (main.equals(pos) || level.getBlockState(main).canBeReplaced()) {   // the first free place (the second pump's, once the first is in)
				return new PartLayout.Slot(main, facing);
			}
		}
		return null;
	}

	/** For {@link BigPart#snap}: a click near the rig moves to the part's place. */
	static BlockPlaceContext snap(BlockPlaceContext context, Block part) {
		PartLayout.Slot slot = slotFor(context.getLevel(), context.getClickedPos(), part);
		return slot == null || slot.main().equals(context.getClickedPos()) ? context : BlockPlaceContext.at(context, slot.main(), Direction.UP);
	}
}
