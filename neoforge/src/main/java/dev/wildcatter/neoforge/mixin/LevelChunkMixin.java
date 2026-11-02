package dev.wildcatter.neoforge.mixin;

import dev.wildcatter.circuit.CircuitManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells the circuit manager when a block entity joins or leaves a server chunk: placed, loaded, removed or unloaded. On Fabric the
 * API's block entity events do this (from the same places).
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
	@Shadow
	public abstract Level getLevel();

	@Inject(method = "setBlockEntity", at = @At("TAIL"))
	private void wildcatter$loaded(BlockEntity entity, CallbackInfo ci) {
		if (getLevel() instanceof ServerLevel level && ((LevelChunk) (Object) this).getBlockEntities().get(entity.getBlockPos()) == entity) {
			CircuitManager.register(entity, level);
		}
	}

	@Inject(method = "removeBlockEntity", at = @At("HEAD"))
	private void wildcatter$removed(BlockPos pos, CallbackInfo ci) {
		BlockEntity entity = ((LevelChunk) (Object) this).getBlockEntities().get(pos);
		if (entity != null && getLevel() instanceof ServerLevel level) {
			CircuitManager.unregister(entity, level);
		}
	}

	@Inject(method = "clearAllBlockEntities", at = @At("HEAD"))
	private void wildcatter$unloaded(CallbackInfo ci) {
		if (getLevel() instanceof ServerLevel level) {
			for (BlockEntity entity : ((LevelChunk) (Object) this).getBlockEntities().values()) {
				CircuitManager.unregister(entity, level);
			}
		}
	}
}
