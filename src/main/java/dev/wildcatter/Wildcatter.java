package dev.wildcatter;

import dev.wildcatter.circuit.CircuitManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTabs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point. Power from steam today; oil and gas wells next.
 * The energy code in {@code dev.wildcatter.energy} has no Fabric or Minecraft dependencies beyond block entities,
 * so it can move to NeoForge (or bridge to Mekanism's energy) without a rewrite.
 */
public class Wildcatter implements ModInitializer {
	public static final String MOD_ID = "wildcatter";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		WildcatterBlocks.register();
		WildcatterItems.register();

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
			for (var block : new net.minecraft.world.level.block.Block[] {
					WildcatterBlocks.BOILER, WildcatterBlocks.STEAM_TURBINE,
					WildcatterBlocks.WIRE_HOT, WildcatterBlocks.WIRE_NEUTRAL, WildcatterBlocks.WOOL_WIRE_HOT, WildcatterBlocks.WOOL_WIRE_NEUTRAL,
					WildcatterBlocks.MV_WIRE_HOT, WildcatterBlocks.MV_WIRE_NEUTRAL, WildcatterBlocks.HV_WIRE_HOT, WildcatterBlocks.HV_WIRE_NEUTRAL,
					WildcatterBlocks.GROUND_ROD, WildcatterBlocks.GROUND_WIRE, WildcatterBlocks.BREAKER,
					WildcatterBlocks.TRANSFORMER, WildcatterBlocks.SUBSTATION_TRANSFORMER,
					WildcatterBlocks.ELECTRIC_LAMP, WildcatterBlocks.ELECTRIC_HEATER}) {
				entries.accept(block);
			}
			entries.accept(WildcatterItems.POWER_METER);
			entries.accept(WildcatterItems.FIELD_MANUAL);
		});

		// Keep track of every circuit part and solve each connected group of them.
		ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register(CircuitManager::register);
		ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register(CircuitManager::unregister);
		ServerTickEvents.END_LEVEL_TICK.register(CircuitManager::tick);
	}
}
