package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.safety.Shock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** A diesel generator or a gas turbine: a 4.16 kV generator that burns fuel in step with the power drawn from it. */
public class EngineBlock extends BaseEntityBlock {
	public static final BooleanProperty RUNNING = BlockStateProperties.LIT;

	public enum Fuel {
		DIESEL, GAS
	}

	private final Fuel fuel;

	public EngineBlock(Properties properties, Fuel fuel) {
		super(properties);
		this.fuel = fuel;
		registerDefaultState(stateDefinition.any().setValue(RUNNING, false));
	}

	public Fuel fuel() {
		return fuel;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
		builder.add(RUNNING);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new EngineBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, WildcatterBlocks.ENGINE_ENTITY, EngineBlockEntity::tick);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, pos, player);
		if (read != null) {
			return read;
		}
		if (fuel == Fuel.DIESEL && stack.is(WildcatterItems.DIESEL_BUCKET) && level.getBlockEntity(pos) instanceof EngineBlockEntity engine) {
			if (!level.isClientSide() && engine.addFuel(Tuning.DIESEL_BUCKET_UNITS)) {
				stack.shrink(1);
				ItemStack empty = new ItemStack(Items.BUCKET);
				if (stack.isEmpty()) {
					player.setItemInHand(hand, empty);
				} else if (!player.getInventory().add(empty)) {
					net.minecraft.world.Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), empty);
				}
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof EngineBlockEntity engine) {
			if (engine.isTripped()) {
				engine.resetTrip();
			}
			if (!Shock.touch((ServerLevel) level, pos, player, engine.voltage(), engine.isLive())) {
				player.sendOverlayMessage(engine.inspect());
			}
		}
		return InteractionResult.SUCCESS;
	}
}
