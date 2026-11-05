package dev.wildcatter.block;

import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.item.CartridgeFuseItem;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A heavy-duty 480 V fusible safety switch on a wall (docs/multiblocks/safety-switch.md): 1 x 1 x 1 for the 100 A frame, 1 x 2 x 1 for
 * the 200 A. The line comes in on its top face, the load leaves its bottom face; three class J fuses behind the door.
 * <ul>
 * <li>Right-click works the handle.</li>
 * <li>Right-clicking with a class J fuse, handle OFF, fits it; sneak and right-click, handle OFF, pulls the fuses out.</li>
 * </ul>
 */
public class SafetySwitchBlock extends BigPartBlock {
	public static final BooleanProperty ON = BooleanProperty.create("on");

	private final int frame;

	public SafetySwitchBlock(Properties properties, int height, int frame, String shapeName) {
		super(properties, Footprint.box(1, height, 1, 0, 0), shapeName);
		this.frame = frame;
		registerDefaultState(defaultBlockState().setValue(ON, false));
	}

	/** The largest fuse it takes, in amps. */
	public int frame() {
		return frame;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(ON);
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new SafetySwitchBlockEntity(pos, state);
	}

	private static @Nullable SafetySwitchBlockEntity entity(Level level, BlockPos pos, BlockState state) {
		return level.getBlockEntity(BigParts.main(pos, state)) instanceof SafetySwitchBlockEntity entity ? entity : null;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, BigParts.main(pos, state), player);
		if (read != null) {
			return read;
		}
		if (!(stack.getItem() instanceof CartridgeFuseItem fuse)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		SafetySwitchBlockEntity entity = entity(level, pos, state);
		if (!level.isClientSide() && entity != null) {
			String refused = entity.fit(fuse);
			if (refused == null) {
				stack.consume(1, player);
				level.playSound(null, pos, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 0.7F, 1.4F);
				CircuitManager.markDirty(level);
				player.sendOverlayMessage(entity.inspect());
			} else {
				player.sendOverlayMessage(Component.literal(refused));
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		SafetySwitchBlockEntity entity = entity(level, pos, state);
		if (level.isClientSide() || entity == null) {
			return InteractionResult.SUCCESS;
		}
		if (player.isShiftKeyDown()) {
			for (ItemStack out : entity.pull()) {
				if (!player.getInventory().add(out)) {
					net.minecraft.world.Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), out);
				}
			}
		} else {
			entity.setOn(!entity.on());
			level.playSound(null, pos, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 1.0F, entity.on() ? 0.7F : 0.6F);
		}
		player.sendOverlayMessage(entity.inspect());
		CircuitManager.markDirty(level);
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
		CircuitManager.markDirty(level);
	}
}
