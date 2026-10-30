package dev.wildcatter;

import dev.wildcatter.circuit.CircuitManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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
		WildcatterSounds.register();
		WildcatterBlocks.register();
		WildcatterItems.register();

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
			for (var block : new net.minecraft.world.level.block.Block[] {
					WildcatterBlocks.BOILER, WildcatterBlocks.STEAM_TURBINE,
					WildcatterBlocks.WIRE_HOT, WildcatterBlocks.WIRE_NEUTRAL, WildcatterBlocks.WOOL_WIRE_HOT, WildcatterBlocks.WOOL_WIRE_NEUTRAL,
					WildcatterBlocks.MV_WIRE_HOT, WildcatterBlocks.MV_WIRE_NEUTRAL, WildcatterBlocks.HV_WIRE_HOT, WildcatterBlocks.HV_WIRE_NEUTRAL,
					WildcatterBlocks.GROUND_ROD, WildcatterBlocks.GROUND_WIRE, WildcatterBlocks.BREAKER, WildcatterBlocks.FUSE, WildcatterBlocks.SWITCH, WildcatterBlocks.UFLS_RELAY, WildcatterBlocks.MV_BREAKER, WildcatterBlocks.HV_BREAKER, WildcatterBlocks.POLE, WildcatterBlocks.CROSSARM, WildcatterBlocks.INSULATOR_LV, WildcatterBlocks.INSULATOR_DIST, WildcatterBlocks.INSULATOR_HV,
					WildcatterBlocks.TRANSFORMER, WildcatterBlocks.SUBSTATION_TRANSFORMER, WildcatterBlocks.DISTRIBUTION_TRANSFORMER,
					WildcatterBlocks.DIST_WIRE_HOT, WildcatterBlocks.DIST_WIRE_NEUTRAL, WildcatterBlocks.DIST_PLASTIC_WIRE_HOT, WildcatterBlocks.DIST_PLASTIC_WIRE_NEUTRAL, WildcatterBlocks.DIST_BREAKER,
					WildcatterBlocks.ELECTRIC_LAMP, WildcatterBlocks.ELECTRIC_HEATER,
					WildcatterBlocks.FIREBOX, WildcatterBlocks.PIPE, WildcatterBlocks.PUMP, WildcatterBlocks.GAS_LINE,
					WildcatterBlocks.DRILLING_RIG, WildcatterBlocks.CASING, WildcatterBlocks.PUMPJACK, WildcatterBlocks.GAS_WELLHEAD,
					WildcatterBlocks.REFINERY, WildcatterBlocks.REFINERY_COLUMN, WildcatterBlocks.COOLING_TOWER, WildcatterBlocks.COOLING_FILL, WildcatterBlocks.DIESEL_ENGINE, WildcatterBlocks.GAS_TURBINE,
					WildcatterBlocks.PLASTIC_WIRE_HOT, WildcatterBlocks.PLASTIC_WIRE_NEUTRAL, WildcatterBlocks.MV_PLASTIC_WIRE_HOT,
					WildcatterBlocks.MV_PLASTIC_WIRE_NEUTRAL, WildcatterBlocks.HV_PLASTIC_WIRE_HOT, WildcatterBlocks.HV_PLASTIC_WIRE_NEUTRAL}) {
				entries.accept(block);
			}
			entries.accept(WildcatterItems.POWER_METER);
			entries.accept(WildcatterItems.SEISMOGRAPH);
			entries.accept(WildcatterItems.CRUDE_OIL_BUCKET);
			entries.accept(WildcatterItems.DIESEL_BUCKET);
			entries.accept(WildcatterItems.PLASTIC_SHEET);
			entries.accept(WildcatterItems.SPOOL_LV);
			entries.accept(WildcatterItems.SPOOL_DIST);
			entries.accept(WildcatterItems.SPOOL_HV);
			entries.accept(WildcatterItems.COVERED_SPOOL_LV);
			entries.accept(WildcatterItems.COVERED_SPOOL_DIST);
			entries.accept(WildcatterItems.COVERED_SPOOL_HV);
			entries.accept(WildcatterItems.STEEL_INGOT);
			entries.accept(WildcatterItems.FIELD_MANUAL);
		});

		// Keep track of every circuit part and solve each connected group of them.
		ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register(CircuitManager::register);
		ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register(CircuitManager::unregister);
		ServerLifecycleEvents.SERVER_STARTED.register(dev.wildcatter.energy.StandardData::apply);
		ServerTickEvents.END_LEVEL_TICK.register(CircuitManager::tick);
	}
}
