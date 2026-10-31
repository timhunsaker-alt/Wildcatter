package dev.wildcatter;

import dev.wildcatter.circuit.CircuitManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import dev.wildcatter.energy.ConductorSize;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.item.ConductorItem;
import dev.wildcatter.item.SpoolItem;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
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
		WildcatterComponents.register();
		WildcatterBlocks.register();
		WildcatterItems.register();

		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("wildcatter"), FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.wildcatter"))
				.icon(() -> new ItemStack(WildcatterItems.POWER_METER))
				.displayItems((parameters, output) -> creativeTab(output))
				.build());

		// Keep track of every circuit part and solve each connected group of them.
		ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register(CircuitManager::register);
		ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register(CircuitManager::unregister);
		ServerLifecycleEvents.SERVER_STARTED.register(dev.wildcatter.energy.StandardData::apply);
		ServerTickEvents.END_LEVEL_TICK.register(CircuitManager::tick);
	}

	/**
	 * The Wildcatter tab: machines and tools in the order they were added, then every conductor in every size, grouped by tier
	 * (wire, cable, busway, then spools) from 120 V up to 138 kV.
	 */
	private static void creativeTab(CreativeModeTab.Output output) {
		List<Item> conductors = new ArrayList<>();
		for (Item item : BuiltInRegistries.ITEM) {
			if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(MOD_ID)) {
				continue;
			}
			if (item instanceof ConductorItem || item instanceof SpoolItem) {
				conductors.add(item);
			} else {
				output.accept(item);
			}
		}
		for (Voltage tier : new Voltage[] {Voltage.LOW, Voltage.INDUSTRIAL, Voltage.MEDIUM, Voltage.DISTRIBUTION, Voltage.HIGH}) {
			for (ConductorSize.Kind kind : ConductorSize.Kind.values()) {
				for (Item item : conductors) {
					if (item instanceof ConductorItem conductor && conductor.tier() == tier && conductor.kind() == kind) {
						ConductorSize.sizes(tier, kind).forEach(size -> output.accept(conductor.stack(size)));
					}
				}
			}
			for (Item item : conductors) {
				if (item instanceof SpoolItem spool && spool.tier() == tier) {
					ConductorSize.sizes(tier, ConductorSize.Kind.WIRE).forEach(size -> output.accept(spool.stack(size)));
				}
			}
		}
	}
}
