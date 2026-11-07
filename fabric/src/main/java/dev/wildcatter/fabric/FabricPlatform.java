package dev.wildcatter.fabric;

import dev.wildcatter.platform.Platform;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class FabricPlatform implements Platform {
	@Override
	public String loader() {
		return "Fabric";
	}

	@Override
	public Path configDir() {
		return FabricLoader.getInstance().getConfigDir();
	}

	@Override
	public void sendToPlayer(net.minecraft.server.level.ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
	}

	@Override
	public void sendToServer(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
	}

	@Override
	public boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}
}
