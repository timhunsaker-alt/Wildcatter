package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A wood utility pole at true size (docs/multiblocks/poles.md): one item that stands its real height above ground, 8 m for a
 * 30 ft pole, 10 m for a 40 ft and 20 m for a 75 ft. Its top cell carries a pole-top pin; a crossarm or an H-frame crossbeam is
 * clicked onto it. Climbable, like the one-block sections it replaces.
 */
public class WoodPoleBlock extends BigPartBlock {
	/** Steps of lean (protection-gaps 20): 0 plumb; see {@link dev.wildcatter.Tuning#POLE_LEAN_DEGREES}. */
	public static final int MAX_LEAN = 3;
	public static final net.minecraft.world.level.block.state.properties.IntegerProperty LEAN =
			net.minecraft.world.level.block.state.properties.IntegerProperty.create("lean", 0, MAX_LEAN);

	private final int height;
	private final String label;
	private final int poleClass;
	private final double classLoadLbf;

	/**
	 * {@code label} the pole's length ("40 ft class 4"); {@code classLoadLbf} its ANSI O5.1 class's horizontal load 2 ft below the top
	 * (class 5: 1,900 lbf; class 4: 2,400; class 2: 3,700).
	 */
	public WoodPoleBlock(Properties properties, int height, String shapeName, String label, int poleClass, double classLoadLbf) {
		super(properties, Footprint.box(1, height, 1, 0, 0), shapeName);
		this.height = height;
		this.label = label;
		this.poleClass = poleClass;
		this.classLoadLbf = classLoadLbf;
	}

	public int height() {
		return height;
	}

	public String label() {
		return label;
	}

	public int poleClass() {
		return poleClass;
	}

	public double classLoadLbf() {
		return classLoadLbf;
	}

	@Override
	protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(LEAN);
	}

	/** Its main cell weighs up its line pull and its guys (block/PoleLoad). */
	@Override
	@SuppressWarnings("unchecked")
	protected <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> mainTicker(net.minecraft.world.level.Level level, BlockState state,
			net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
		if (level.isClientSide() || type != WildcatterBlocks.BIG_PART_ENTITY) {
			return null;
		}
		return (l, p, s, entity) -> PoleLoad.tick(l, p, s, (BigPartBlockEntity) entity);
	}

	/** The Power Meter on any of its cells reads its line pull, its guys and its lean. */
	@Override
	protected net.minecraft.world.InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
			net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
		if (!stack.is(dev.wildcatter.WildcatterItems.POWER_METER)) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		BlockPos foot = BigParts.main(pos, state);
		if (!level.isClientSide() && level.getBlockEntity(foot) instanceof BigPartBlockEntity entity) {
			player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(PoleLoad.describe(level, foot, level.getBlockState(foot), this, entity)));
		}
		return net.minecraft.world.InteractionResult.SUCCESS;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new BigPartBlockEntity(pos, state);
	}

	/** A new pole, unlike the parts of a machine whose layout changed: one set by a command grows into its cells. */
	@Override
	public boolean growsOnLoad() {
		return true;
	}

	/** Any pole cell: a true-size pole's, or an older world's one-block section. */
	public static boolean isPole(BlockState state) {
		return state.is(WildcatterBlocks.POLE) || state.getBlock() instanceof WoodPoleBlock;
	}

	/** The top cell of a true-size pole: a pole-top pin goes on top of it, and a crossarm beside it. */
	public static boolean isTop(BlockState state) {
		return state.getBlock() instanceof WoodPoleBlock pole && state.getValue(pole.piece()) == pole.height - 1;
	}

	/** The top cell of the pole that has a cell at {@code pos} (an older one-block section is its own top). */
	public static BlockPos top(BlockPos pos, BlockState state) {
		return state.getBlock() instanceof WoodPoleBlock pole ? BigParts.main(pos, state).above(pole.height - 1) : pos;
	}

	/** True if a pole cell stands beside {@code pos} (north, south, east or west): what pole hardware hangs on. */
	public static boolean poleBeside(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
		for (net.minecraft.core.Direction side : net.minecraft.core.Direction.Plane.HORIZONTAL) {
			if (isPole(level.getBlockState(pos.relative(side)))) {
				return true;
			}
		}
		return false;
	}
}
