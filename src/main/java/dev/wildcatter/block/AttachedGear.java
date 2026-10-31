package dev.wildcatter.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Gear wired on every face (a receptacle, a plug, a lamp, a heater) mounted at true size on the face of its cell it was clicked
 * against: a wall, the floor or the ceiling (docs/multiblocks/building-gear.md). Its terminals do not move.
 */
public final class AttachedGear {
	/** The face of its cell it is mounted on; the floor (down) for gear set by a command or from an older world. */
	public static final EnumProperty<Direction> ATTACH = EnumProperty.create("attach", Direction.class);

	private AttachedGear() {
	}

	/** Mounted on whatever was clicked. */
	public static BlockState place(BlockState state, BlockPlaceContext context) {
		return state.setValue(ATTACH, context.getClickedFace().getOpposite());
	}

	/** A box {@code depth} sixteenths out from its face and {@code half} sixteenths either side of the face's middle. */
	public static VoxelShape shape(BlockState state, double depth, double half) {
		double a = 8 - half, b = 8 + half;
		return switch (state.getValue(ATTACH)) {
			case DOWN -> Block.box(a, 0, a, b, depth, b);
			case UP -> Block.box(a, 16 - depth, a, b, 16, b);
			case NORTH -> Block.box(a, a, 0, b, b, depth);
			case SOUTH -> Block.box(a, a, 16 - depth, b, b, 16);
			case WEST -> Block.box(0, a, a, depth, b, b);
			case EAST -> Block.box(16 - depth, a, a, 16, b, b);
		};
	}
}
