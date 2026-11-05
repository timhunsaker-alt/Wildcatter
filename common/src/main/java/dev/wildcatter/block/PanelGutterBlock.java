package dev.wildcatter.block;

import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.item.BranchBreakerItem;
import dev.wildcatter.item.PowerMeterItem;
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
 * A panel gutter (docs/multiblocks/panelboards.md): a wireway section beside a load centre or panelboard that holds one branch-breaker
 * space. Its branch circuit leaves its front face.
 * <ul>
 * <li>Right-clicking with a branch breaker clips it in, if it fits the panel and the panel has the spaces.</li>
 * <li>Right-click with an empty hand toggles the breaker, and resets it after a trip.</li>
 * <li>Sneak and right-click with an empty hand pulls an open breaker out.</li>
 * </ul>
 */
public class PanelGutterBlock extends BigPartBlock {
	/** What its cover shows: an empty space, or the breaker's handle on, off or tripped. */
	public enum Slot implements StringRepresentable {
		EMPTY("empty"), ON("on"), OFF("off"), TRIPPED("tripped");

		private final String name;

		Slot(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public static final EnumProperty<Slot> SLOT = EnumProperty.create("slot", Slot.class);
	public static final Footprint FOOTPRINT = Footprint.box(1, 1, 1, 0, 0);

	public PanelGutterBlock(Properties properties) {
		super(properties, FOOTPRINT, "panel_gutter");
		registerDefaultState(defaultBlockState().setValue(SLOT, Slot.EMPTY));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(SLOT);
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new PanelGutterBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, pos, player);
		if (read != null) {
			return read;
		}
		if (!(stack.getItem() instanceof BranchBreakerItem breaker)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PanelGutterBlockEntity gutter) {
			String refused = gutter.install(breaker);
			if (refused == null) {
				stack.consume(1, player);
				level.playSound(null, pos, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 0.8F, 0.7F);
				CircuitManager.markDirty(level);
				player.sendOverlayMessage(gutter.inspect());
			} else {
				player.sendOverlayMessage(Component.literal(refused));
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof PanelGutterBlockEntity gutter)) {
			return InteractionResult.SUCCESS;
		}
		if (player.isShiftKeyDown()) {
			ItemStack out = gutter.pull();
			if (out != null) {
				if (!player.getInventory().add(out)) {
					net.minecraft.world.Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), out);
				}
				level.playSound(null, pos, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 0.8F, 0.6F);
			}
		} else if (gutter.operate()) {
			level.playSound(null, pos, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 0.8F, gutter.closed() ? 1.0F : 0.85F);
		}
		player.sendOverlayMessage(gutter.inspect());
		CircuitManager.markDirty(level);
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
		CircuitManager.markDirty(level);
	}
}
