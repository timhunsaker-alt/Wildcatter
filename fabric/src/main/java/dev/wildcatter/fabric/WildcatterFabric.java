package dev.wildcatter.fabric;

import dev.wildcatter.Wildcatter;
import dev.wildcatter.circuit.CircuitManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/** The Fabric entry point: registers the mod and wires Fabric's events to the common handlers in {@link Wildcatter}. */
public class WildcatterFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		Wildcatter.register();
		OreGenFabric.register();
		// plugs hand power to Team Reborn Energy machines (bundled, so always loaded on Fabric)
		if (FabricLoader.getInstance().isModLoaded("team_reborn_energy")) {
			dev.wildcatter.energy.TeamRebornEnergy.register();
		}

		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Wildcatter.id("wildcatter"), FabricCreativeModeTab.builder()
				.title(Wildcatter.tabTitle())
				.icon(Wildcatter::tabIcon)
				.displayItems((parameters, output) -> Wildcatter.creativeTab(output::accept))
				.build());

		// Keep track of every circuit part and solve each connected group of them.
		ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register(CircuitManager::register);
		ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register(CircuitManager::unregister);
		ServerLifecycleEvents.SERVER_STARTED.register(Wildcatter::serverStarted);
		ServerLifecycleEvents.SERVER_STOPPED.register(Wildcatter::serverStopped);
		ServerTickEvents.END_LEVEL_TICK.register(CircuitManager::tick);
		ServerTickEvents.END_SERVER_TICK.register(Wildcatter::serverTick);
		ServerPlayerEvents.AFTER_RESPAWN.register((old, now, alive) -> Wildcatter.respawned(now));
	}
}
