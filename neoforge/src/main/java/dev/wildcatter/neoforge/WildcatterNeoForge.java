package dev.wildcatter.neoforge;

import dev.wildcatter.Wildcatter;
import dev.wildcatter.circuit.CircuitManager;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * The NeoForge entry point: registers the mod while NeoForge has the registries open, and wires NeoForge's events to the common
 * handlers in {@link Wildcatter}. Block entities reach the circuit manager through {@code mixin.LevelChunkMixin}.
 */
@Mod(Wildcatter.MOD_ID)
public class WildcatterNeoForge {
	private static boolean registered;

	public WildcatterNeoForge(IEventBus modBus) {
		modBus.addListener(RegisterEvent.class, event -> {
			if (!registered) {   // the first registry event: every registry is open while they fire
				registered = true;
				Wildcatter.register();
				NeoForgeEnergy.register();   // plugs hand power to FE machines (Mekanism and others)
				Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Wildcatter.id("wildcatter"), CreativeModeTab.builder()
						.title(Wildcatter.tabTitle())
						.icon(Wildcatter::tabIcon)
						.displayItems((parameters, output) -> Wildcatter.creativeTab(output::accept))
						.build());
			}
		});
		NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, event -> Wildcatter.serverStarted(event.getServer()));
		NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> Wildcatter.serverStopped(event.getServer()));
		NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> Wildcatter.serverTick(event.getServer()));
		NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
			if (event.getLevel() instanceof ServerLevel level) {
				CircuitManager.tick(level);
			}
		});
		NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, event -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				Wildcatter.respawned(player);
			}
		});
		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			dev.wildcatter.neoforge.client.WildcatterNeoForgeClient.init(modBus);
		}
	}
}
