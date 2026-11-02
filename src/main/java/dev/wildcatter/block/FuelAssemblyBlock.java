package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.fluid.Fuel;
import dev.wildcatter.multiblock.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A finished 17x17 fuel assembly standing at true size (docs/multiblocks/fuel.md): 214 mm square and 4.06 m tall, its 264 rods, 25
 * thimbles, 8 grids and 2 nozzles drawn. Standing, it only waits for the reactor (Phase 8); its item carries it.
 */
public class FuelAssemblyBlock extends BigPartBlock {
	public static final Footprint FOOTPRINT = Footprint.box(1, 5, 1, 0, 0);

	public FuelAssemblyBlock(Properties properties) {
		super(properties, FOOTPRINT, "fuel_assembly");
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new BigPartBlockEntity(pos, state);
	}

	/** Its tooltip and readout. */
	public static String describe() {
		return String.format("17x17 fuel assembly: %d rods, %,.1f kg of uranium at %.2f %% U-235, about %,.0f kg", Tuning.ASSEMBLY_RODS, Fuel.assemblyU(),
				Tuning.ASSAY_PRODUCT * 100, Fuel.assemblyKg());
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			player.sendOverlayMessage(Component.literal(describe()));
		}
		return InteractionResult.SUCCESS;
	}
}
