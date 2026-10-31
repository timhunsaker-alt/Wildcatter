package dev.wildcatter.block;

import dev.wildcatter.energy.Voltage;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.safety.Shock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * An industrial three-phase heater: three elements in a delta across phases A, B and C, like a duct or unit heater
 * on a 480 V (400 V IEC) supply. It needs no neutral. With one phase missing it runs at half power.
 */
public class ThreePhaseHeaterBlock extends BaseEntityBlock {
	private final Voltage voltage;
	private final double watts;

	public ThreePhaseHeaterBlock(Properties properties, Voltage voltage, double watts) {
		super(properties);
		this.voltage = voltage;
		this.watts = watts;
		registerDefaultState(stateDefinition.any().setValue(LoadBlock.LIT, false));
	}

	public Voltage voltage() {
		return voltage;
	}

	public double watts() {
		return watts;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LoadBlock.LIT, AttachedGear.ATTACH);
	}

	@Override
	public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
		return AttachedGear.place(defaultBlockState(), context);
	}

	// a 15 kW unit heater, about 0.6 x 0.6 x 0.45 m
	@Override
	protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
			net.minecraft.world.phys.shapes.CollisionContext context) {
		return AttachedGear.shape(state, 8, 5);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, pos, player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ThreePhaseHeaterBlockEntity heater) {
			if (!Shock.touch((ServerLevel) level, pos, player, voltage, heater.isLive())) {
				player.sendOverlayMessage(heater.inspect());
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ThreePhaseHeaterBlockEntity(pos, state);
	}
}
