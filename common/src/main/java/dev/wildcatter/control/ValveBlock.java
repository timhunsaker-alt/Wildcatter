package dev.wildcatter.control;

import dev.wildcatter.block.PipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** A valve in a run of pipe: it joins as a pipe does, and a run stops at one that is shut (docs/multiblocks/plc.md). */
public class ValveBlock extends PipeBlock implements EntityBlock {
	public enum Type {
		/** Air to move, a spring when air is lost; a positioner on 4-20 mA. */
		CONTROL,
		/** Air to open, a spring to close; a solenoid on a relay. */
		ON_OFF,
		/** A 480 V actuator: stays put on lost power. */
		MOTOR_OPERATED
	}

	private final Type type;

	public ValveBlock(Properties properties, Kind kind, Type type) {
		super(properties, kind);
		this.type = type;
	}

	public Type type() {
		return type;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ValveBlockEntity valve) {
			if (player.isShiftKeyDown()) {
				valve.toggleFailMode();
			} else {
				valve.hand();
			}
			player.sendOverlayMessage(valve.inspect());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ValveBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() || type != dev.wildcatter.WildcatterBlocks.VALVE_ENTITY ? null : (l, p, s, e) -> ValveBlockEntity.tick(l, p, s, (ValveBlockEntity) e);
	}
}
