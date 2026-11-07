package dev.wildcatter.neoforge;

import dev.wildcatter.platform.Platform;
import java.nio.file.Path;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

public final class NeoForgePlatform implements Platform {
	@Override
	public void sendToPlayer(net.minecraft.server.level.ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
		net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
	}

	@Override
	public void sendToServer(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
		net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload);
	}

	@Override
	public String loader() {
		return "NeoForge";
	}

	@Override
	public Path configDir() {
		return FMLPaths.CONFIGDIR.get();
	}

	@Override
	public boolean isModLoaded(String modId) {
		return ModList.get().isLoaded(modId);
	}
}
