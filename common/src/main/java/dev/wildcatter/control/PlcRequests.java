package dev.wildcatter.control;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** A block entity with a screen over {@link PlcNet}. */
public interface PlcRequests {
	void request(ServerPlayer player, CompoundTag tag);
}
