package dev.wildcatter.neoforge.client;

import dev.wildcatter.client.WildcatterClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/** The NeoForge client side: wires NeoForge's client events to {@link WildcatterClient}. */
public final class WildcatterNeoForgeClient {
	private static Screen tickScreen;
	private static Runnable tick;

	private WildcatterNeoForgeClient() {
	}

	public static void init(IEventBus modBus) {
		modBus.addListener(FMLClientSetupEvent.class, event -> WildcatterClient.init());
		modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> WildcatterClient.renderers(event::registerBlockEntityRenderer));
		NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> WildcatterClient.disconnected());
		NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.AfterTranslucentBlocks.class, event -> WildcatterClient.drawGizmos());
		NeoForge.EVENT_BUS.addListener(ScreenEvent.Init.Post.class, event -> {
			Runnable each = WildcatterClient.screenOpened(event.getScreen(), event.getListenersList(), event::addListener);
			tickScreen = each != null ? event.getScreen() : null;
			tick = each;
		});
		NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> {
			if (tick != null && Minecraft.getInstance().gui.screen() == tickScreen) {
				tick.run();
			}
		});
	}
}
