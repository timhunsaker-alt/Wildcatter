package dev.wildcatter.block;

import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The incoming section of a motor control center at true size (docs/multiblocks/motor-control-center.md): a NEMA section 0.51 m
 * wide, 2.29 m tall and 0.51 m deep, 1 x 3 x 1, holding the main breaker; the controller. The feed comes in on the top of its top
 * cell or at the back of its bottom cell; its bus runs out of the bottom cell's right-hand side (as you face the front) into the
 * starter sections. Right-click switches the main breaker, sneak and right-click steps its rating.
 */
public class MccIncomingBlock extends BigPartBlock {
	public static final EnumProperty<BreakerBlock.Position> POSITION = BreakerBlock.POSITION;
	public static final Footprint FOOTPRINT = Footprint.box(1, 3, 1, 0, 0);

	public MccIncomingBlock(Properties properties) {
		super(properties, FOOTPRINT, "mcc_incoming");
		registerDefaultState(defaultBlockState().setValue(POSITION, BreakerBlock.Position.ON));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(POSITION);
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new MccIncomingBlockEntity(pos, state);
	}

	/** An older world's one-block incoming section grows into its cells where it has room. */
	@Override
	public boolean growsOnLoad() {
		return true;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, BigParts.main(pos, state), player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		BlockPos main = BigParts.main(pos, state);
		if (level.isClientSide() || !(level.getBlockEntity(main) instanceof MccIncomingBlockEntity mcc)) {
			return InteractionResult.SUCCESS;
		}
		if (player.isShiftKeyDown()) {
			mcc.nextRating();
		} else {
			mcc.setSwitch(level.getBlockState(main).getValue(POSITION) != BreakerBlock.Position.ON);
			level.playSound(null, main, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 0.8F, 0.9F);
		}
		player.sendOverlayMessage(mcc.inspect());
		CircuitManager.markDirty(level);
		return InteractionResult.SUCCESS;
	}
}
