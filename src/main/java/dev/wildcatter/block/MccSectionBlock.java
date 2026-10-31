package dev.wildcatter.block;

import dev.wildcatter.WildcatterItems;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.CircuitManager;
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
 * A starter section of a motor control center at true size (docs/multiblocks/motor-control-center.md): a NEMA section 1 x 3 x 1
 * with three bucket spaces, one per cell, beside its vertical wireway. It stands to the right of the incoming section (or another
 * starter section) as you face the front. A bucket item clicked on an empty space goes in; each cell's state shows its bucket.
 * Right-click a bucket to start or stop it (or reset a trip), sneak and right-click to step its overload dial; redstone on a cell
 * runs that cell's bucket. Its motor leads leave the back of its cell.
 */
public class MccSectionBlock extends BigPartBlock {
	/** What a cell shows: an empty space, or its bucket stopped, running or tripped. */
	public enum Bucket implements StringRepresentable {
		EMPTY("empty"), STOPPED("stopped"), RUNNING("running"), TRIPPED("tripped");

		private final String name;

		Bucket(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}

		static Bucket of(@Nullable Starter starter) {
			return starter == null ? EMPTY : switch (starter.run()) {
				case STOPPED -> STOPPED;
				case RUNNING -> RUNNING;
				case TRIPPED -> TRIPPED;
			};
		}
	}

	public static final EnumProperty<Bucket> BUCKET = EnumProperty.create("bucket", Bucket.class);
	public static final Footprint FOOTPRINT = Footprint.box(1, 3, 1, 0, 0);

	public MccSectionBlock(Properties properties) {
		super(properties, FOOTPRINT, "mcc_section");
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(BUCKET);
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new MccSectionBlockEntity(pos, state);
	}

	private static @Nullable MccSectionBlockEntity section(Level level, BlockPos pos, BlockState state) {
		return level.getBlockEntity(BigParts.main(pos, state)) instanceof MccSectionBlockEntity section ? section : null;
	}

	private static int space(BlockPos pos, BlockState state) {
		return pos.getY() - BigParts.main(pos, state).getY();
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, BigParts.main(pos, state), player);
		if (read != null) {
			return read;
		}
		if (!stack.is(WildcatterItems.MCC_BUCKET)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		MccSectionBlockEntity section = section(level, pos, state);
		if (!level.isClientSide() && section != null) {
			if (section.install(space(pos, state))) {
				stack.consume(1, player);
				level.playSound(null, pos, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 0.8F, 0.7F);
				CircuitManager.markDirty(level);
			} else {
				player.sendOverlayMessage(Component.literal("That space already has a bucket"));
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		MccSectionBlockEntity section = section(level, pos, state);
		if (level.isClientSide() || section == null) {
			return InteractionResult.SUCCESS;
		}
		int k = space(pos, state);
		if (section.bucket(k) == null) {
			player.sendOverlayMessage(Component.literal("An empty bucket space: click an MCC bucket into it. ").append(section.inspect()));
			return InteractionResult.SUCCESS;
		}
		if (player.isShiftKeyDown()) {
			section.nextOverloadSetting(k);
		} else {
			section.pushButton(k);
			level.playSound(null, pos, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 0.8F, 1.2F);
		}
		player.sendOverlayMessage(section.inspect(k));
		CircuitManager.markDirty(level);
		return InteractionResult.SUCCESS;
	}

	/** Redstone on a cell runs that cell's bucket. */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
		MccSectionBlockEntity section = section(level, pos, state);
		if (!level.isClientSide() && section != null) {
			section.redstone(space(pos, state), level.hasNeighborSignal(pos));
		}
		CircuitManager.markDirty(level);
	}
}
