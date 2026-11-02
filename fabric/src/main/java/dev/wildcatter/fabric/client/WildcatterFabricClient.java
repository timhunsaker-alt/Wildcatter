package dev.wildcatter.fabric.client;

import dev.wildcatter.client.WildcatterClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

/** The Fabric client entry point: wires Fabric's client events to {@link WildcatterClient}. */
public class WildcatterFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		WildcatterClient.init();
		WildcatterClient.renderers(BlockEntityRenderers::register);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> WildcatterClient.disconnected());
		LevelRenderEvents.BEFORE_GIZMOS.register(context -> WildcatterClient.drawGizmos());
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			Runnable tick = WildcatterClient.screenOpened(screen, Screens.getWidgets(screen), Screens.getWidgets(screen)::add);
			if (tick != null) {
				ScreenEvents.afterTick(screen).register(s -> tick.run());
			}
		});
	}
}
