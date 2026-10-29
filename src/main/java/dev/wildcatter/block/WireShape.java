package dev.wildcatter.block;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The thin wire look shared by cables and ground wire: a 2-pixel core with an arm toward each joined neighbour. */
public final class WireShape {
	public static final Map<Direction, BooleanProperty> ARMS = new EnumMap<>(Direction.class);
	private static final VoxelShape CORE = Block.box(7, 7, 7, 9, 9, 9);
	private static final Map<Direction, VoxelShape> ARM_SHAPES = new EnumMap<>(Direction.class);

	static {
		for (Direction side : Direction.values()) {
			ARMS.put(side, BooleanProperty.create(side.getName()));
		}
		ARM_SHAPES.put(Direction.NORTH, Block.box(7, 7, 0, 9, 9, 7));
		ARM_SHAPES.put(Direction.SOUTH, Block.box(7, 7, 9, 9, 9, 16));
		ARM_SHAPES.put(Direction.WEST, Block.box(0, 7, 7, 7, 9, 9));
		ARM_SHAPES.put(Direction.EAST, Block.box(9, 7, 7, 16, 9, 9));
		ARM_SHAPES.put(Direction.DOWN, Block.box(7, 0, 7, 9, 7, 9));
		ARM_SHAPES.put(Direction.UP, Block.box(7, 9, 7, 9, 16, 9));
	}

	private WireShape() {
	}

	public static BlockState noArms(BlockState state) {
		for (BooleanProperty arm : ARMS.values()) {
			state = state.setValue(arm, false);
		}
		return state;
	}

	public static VoxelShape of(BlockState state) {
		VoxelShape shape = CORE;
		for (Direction side : Direction.values()) {
			if (state.getValue(ARMS.get(side))) {
				shape = Shapes.or(shape, ARM_SHAPES.get(side));
			}
		}
		return shape;
	}
}
