package dev.wildcatter.block;

import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The 4.16 kV drive's input transformer and power cell sections (docs/multiblocks/vfd.md). They have no work of their own: the
 * control section does the drive's work and puts the line's terminals on the transformer section. A click reads the drive.
 */
public class DriveSectionBlock extends BigPartBlock {
	public DriveSectionBlock(Properties properties, Footprint footprint, String shapeName) {
		super(properties, footprint, shapeName);
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new Entity(pos, state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, BigParts.main(pos, state), player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(BigParts.main(pos, state)) instanceof Entity section) {
			player.sendOverlayMessage(section.inspect());
		}
		return InteractionResult.SUCCESS;
	}

	/** A section: it answers for the terminals its control section puts on it, and is solved with it. */
	public static class Entity extends BlockEntity implements CircuitPart, Inspectable, BigPartEntity {
		private boolean grown;

		public Entity(BlockPos pos, BlockState state) {
			super(dev.wildcatter.WildcatterBlocks.DRIVE_SECTION_ENTITY, pos, state);
		}

		private @Nullable StarterCabinetBlockEntity control() {
			return level == null ? null : StarterCabinetBlockEntity.controlFor(level, getBlockState(), worldPosition);
		}

		@Override
		public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
			return hasPortAt(worldPosition, face, role, voltage);
		}

		@Override
		public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
			StarterCabinetBlockEntity control = control();
			return control != null && control.formed() && control.hasPortAt(cell, face, role, voltage);
		}

		@Override
		public java.util.Collection<BlockPos> links() {
			StarterCabinetBlockEntity control = control();
			return control != null ? List.of(control.getBlockPos()) : List.of();
		}

		@Override
		public void build(Circuit circuit) {
		}

		@Override
		public void apply(Circuit circuit, int ticks) {
		}

		@Override
		public Component inspect() {
			StarterCabinetBlockEntity control = control();
			String name = getBlockState().getBlock() instanceof DriveSectionBlock block && block.shapeName().equals("drive_mv_transformer")
					? "Drive input transformer section" : "Drive power cell section";
			return control != null ? Component.literal(name + ". ").append(control.inspect())
					: Component.literal(name + ": no output and control section in its lineup");
		}

		@Override
		public boolean grown() {
			return grown;
		}

		@Override
		public void markGrown() {
			grown = true;
			setChanged();
		}

		@Override
		protected void saveAdditional(ValueOutput output) {
			super.saveAdditional(output);
			output.putBoolean("grown", grown);
		}

		@Override
		protected void loadAdditional(ValueInput input) {
			super.loadAdditional(input);
			grown = input.getBooleanOr("grown", false);
		}
	}
}
