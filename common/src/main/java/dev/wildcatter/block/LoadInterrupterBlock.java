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
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A 5 kV metal-enclosed load-interrupter switch, 1 x 3 x 2 (docs/multiblocks/load-interrupter.md), plain or with three E-rated power
 * fuses. The line cable lands on the top face of its top back cell, the load leaves the back face of its bottom back cell.
 * <ul>
 * <li>Right-click opens or closes it. It breaks load up to 600 A; opened over that it fails, with an arc, and is wrecked.</li>
 * <li>On the fused one, right-clicking with an E fuse, the switch open, fits it; sneak and right-click, open, pulls the fuses out.</li>
 * </ul>
 */
public class LoadInterrupterBlock extends BigPartBlock {
	public enum State implements StringRepresentable {
		CLOSED("closed"), OPEN("open"), WRECKED("wrecked");

		private final String name;

		State(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public static final EnumProperty<State> STATE = EnumProperty.create("state", State.class);
	/** 1 wide, 3 tall, 2 deep; the main cell is the front bottom. */
	public static final Footprint FOOTPRINT = Footprint.box(1, 3, 2, 0, 0);

	private final boolean fused;

	public LoadInterrupterBlock(Properties properties, boolean fused, String shapeName) {
		super(properties, FOOTPRINT, shapeName);
		this.fused = fused;
		registerDefaultState(defaultBlockState().setValue(STATE, State.OPEN));
	}

	public boolean fused() {
		return fused;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(STATE);
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new LoadInterrupterBlockEntity(pos, state);
	}

	private static @Nullable LoadInterrupterBlockEntity entity(Level level, BlockPos pos, BlockState state) {
		return level.getBlockEntity(BigParts.main(pos, state)) instanceof LoadInterrupterBlockEntity entity ? entity : null;
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
		LoadInterrupterBlockEntity entity = entity(level, pos, state);
		if (!level.isClientSide() && entity != null) {
			String refused = entity.fit(fuse);
			if (refused == null) {
				stack.consume(1, player);
				level.playSound(null, pos, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 0.8F, 1.3F);
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
		LoadInterrupterBlockEntity entity = entity(level, pos, state);
		if (level.isClientSide() || entity == null) {
			return InteractionResult.SUCCESS;
		}
		if (player.isShiftKeyDown()) {
			for (ItemStack out : entity.pull()) {
				if (!player.getInventory().add(out)) {
					net.minecraft.world.Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), out);
				}
			}
		} else if (entity.operate()) {
			level.playSound(null, pos, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 1.2F, 0.5F);
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
