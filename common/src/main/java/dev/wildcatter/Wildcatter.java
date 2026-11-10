package dev.wildcatter;

import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.energy.ConductorSize;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.item.ConductorItem;
import dev.wildcatter.item.SpoolItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The mod, the same on every loader. Each loader's entry point (dev.wildcatter.fabric.WildcatterFabric,
 * dev.wildcatter.neoforge.WildcatterNeoForge) calls {@link #register()} while the registries are open, and wires its own events to
 * the handlers here.
 */
public final class Wildcatter {
	public static final String MOD_ID = "wildcatter";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private Wildcatter() {
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	/** Every block, item, sound, component and feature type, in this order. */
	public static void register() {
		WildcatterSounds.register();
		WildcatterComponents.register();
		WildcatterBlocks.register();
		WildcatterItems.register();
		dev.wildcatter.ore.OreBlocks.register();
		dev.wildcatter.ore.OreGen.register();
	}

	/** The creative tab's title and icon; each loader builds the tab, with {@link #creativeTab} as its contents. */
	public static Component tabTitle() {
		return Component.translatable("itemGroup.wildcatter");
	}

	public static ItemStack tabIcon() {
		return new ItemStack(WildcatterItems.POWER_METER);
	}

	public static void serverStarted(MinecraftServer server) {
		dev.wildcatter.energy.StandardData.apply(server);
	}

	/** A level has loaded: for the overworld, before any of its chunks generate, the world's rules (they decide where salt domes go). */
	public static void levelLoaded(net.minecraft.server.level.ServerLevel level) {
		if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
			RulesData.load(level);
		}
	}

	public static void registerCommands(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
		RulesCommand.register(dispatcher);
	}

	/** Static per-world state must not survive into the next world opened in the same session. */
	public static void serverStopped(MinecraftServer server) {
		CircuitManager.clearAll();
		Rules.clear();
		dev.wildcatter.block.SpentFuelPoolBlockEntity.clearAll();
		dev.wildcatter.block.NaturalDraftTowerBlockEntity.clearAll();
		dev.wildcatter.block.ReactorVesselBlockEntity.clearAll();
		dev.wildcatter.block.ContainmentMonitorBlockEntity.clearAll();
		dev.wildcatter.block.StorageModuleBlockEntity.clearAll();
		dev.wildcatter.radiation.RadiationTicker.clearAll();
		SpoolItem.clearAll();
		dev.wildcatter.safety.BodyContact.clearAll();
	}

	public static void serverTick(MinecraftServer server) {
		dev.wildcatter.radiation.RadiationTicker.tick(server);
	}

	public static void respawned(ServerPlayer player) {
		dev.wildcatter.radiation.RadiationTicker.respawned(player);
	}

	/**
	 * The Wildcatter tab: machines and tools in the order they were added, then every conductor in every size, grouped by tier
	 * (wire, cable, busway, then spools) from 120 V up to 138 kV.
	 */
	public static void creativeTab(java.util.function.Consumer<ItemStack> output) {
		List<Item> conductors = new ArrayList<>();
		for (Item item : BuiltInRegistries.ITEM) {
			if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(MOD_ID)) {
				continue;
			}
			if (item instanceof ConductorItem || item instanceof SpoolItem) {
				conductors.add(item);
			} else if (item == WildcatterBlocks.POLE.asItem() || item == WildcatterBlocks.CROSSARM.asItem()) {
				continue;   // an older world's one-block pole and crossarm: kept working, no longer offered (4.5e)
			} else {
				output.accept(new ItemStack(item));
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
