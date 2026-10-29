package dev.wildcatter;

import dev.wildcatter.item.PowerMeterItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class WildcatterItems {
	public static final ResourceKey<Item> POWER_METER_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("power_meter"));
	public static final Item POWER_METER = new PowerMeterItem(new Item.Properties().stacksTo(1).setId(POWER_METER_KEY));

	private WildcatterItems() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, POWER_METER_KEY, POWER_METER);
	}
}
