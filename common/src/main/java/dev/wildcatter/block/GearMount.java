package dev.wildcatter.block;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * How a piece of in-line gear (a breaker, switch, fuse or starter: in at the back, out at the front) is mounted at true size
 * (docs/multiblocks/building-gear.md). Clicked against the side of a block it hangs on that wall, its line running along the wall,
 * in from the left and out to the right as you face it: the wall is on its facing's counter-clockwise side. Otherwise it stands on
 * a Unistrut stand on the floor, facing as it always has.
 */
public enum GearMount implements StringRepresentable {
	FLOOR("floor"), WALL("wall");

	public static final EnumProperty<GearMount> MOUNT = EnumProperty.create("mount", GearMount.class);

	private static final VoxelShape FLOOR_SHAPE = Block.box(4, 0, 4, 12, 13, 12);
	private static final VoxelShape[] WALL_SHAPES = {   // by the wall's side: north, south, west, east
			Block.box(3, 2, 0, 13, 14, 4), Block.box(3, 2, 12, 13, 14, 16), Block.box(0, 2, 3, 4, 14, 13), Block.box(12, 2, 3, 16, 14, 13)};

	private final String name;

	GearMount(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	/**
	 * The placed state: on a wall when the click was on the side of a block, with {@code facing} set along the wall; otherwise on
	 * the floor facing {@code floorFacing}.
	 */
	public static BlockState place(BlockState state, BlockPlaceContext context, EnumProperty<Direction> facing, Direction floorFacing) {
		Direction face = context.getClickedFace();
		if (face.getAxis().isHorizontal()) {
			return state.setValue(facing, face.getCounterClockWise()).setValue(MOUNT, WALL);
		}
		return state.setValue(facing, floorFacing).setValue(MOUNT, FLOOR);
	}

	/** Its outline: the enclosure against its wall, or on its stand. */
	public static VoxelShape shape(BlockState state, Direction facing) {
		if (!state.hasProperty(MOUNT) || state.getValue(MOUNT) == FLOOR) {
			return FLOOR_SHAPE;
		}
		return WALL_SHAPES[facing.getCounterClockWise().get3DDataValue() - 2];
	}
}
