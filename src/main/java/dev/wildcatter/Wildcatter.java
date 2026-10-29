package dev.wildcatter;

import net.fabricmc.api.ModInitializer;
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
			entries.accept(WildcatterBlocks.BOILER);
			entries.accept(WildcatterBlocks.STEAM_TURBINE);
			entries.accept(WildcatterBlocks.COPPER_CABLE);
			entries.accept(WildcatterBlocks.INSULATED_CABLE);
			entries.accept(WildcatterBlocks.MEDIUM_LINE);
			entries.accept(WildcatterBlocks.INSULATED_MEDIUM_CABLE);
			entries.accept(WildcatterBlocks.HIGH_LINE);
			entries.accept(WildcatterBlocks.INSULATED_HIGH_CABLE);
			entries.accept(WildcatterBlocks.GROUND_ROD);
			entries.accept(WildcatterBlocks.GROUND_WIRE);
			entries.accept(WildcatterBlocks.TRANSFORMER);
			entries.accept(WildcatterBlocks.SUBSTATION_TRANSFORMER);
			entries.accept(WildcatterBlocks.ELECTRIC_LAMP);
			entries.accept(WildcatterItems.POWER_METER);
			entries.accept(WildcatterItems.FIELD_MANUAL);
		});
	}
}
