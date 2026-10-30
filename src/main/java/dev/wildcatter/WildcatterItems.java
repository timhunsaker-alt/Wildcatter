package dev.wildcatter;

import dev.wildcatter.item.FieldManualItem;
import dev.wildcatter.item.Manual;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.item.SeismographItem;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class WildcatterItems {
	public static final ResourceKey<Item> POWER_METER_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("power_meter"));
	public static final Item POWER_METER = new PowerMeterItem(new Item.Properties().stacksTo(1).setId(POWER_METER_KEY));

	public static final ResourceKey<Item> FIELD_MANUAL_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("field_manual"));
	public static final Item FIELD_MANUAL = new FieldManualItem(new Item.Properties().stacksTo(1)
			.component(DataComponents.WRITTEN_BOOK_CONTENT, Manual.content()).setId(FIELD_MANUAL_KEY));

	public static final ResourceKey<Item> SEISMOGRAPH_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("seismograph"));
	public static final Item SEISMOGRAPH = new SeismographItem(new Item.Properties().stacksTo(1).setId(SEISMOGRAPH_KEY));

	/** A bucket of crude oil. It burns in a firebox (as long as lava) and leaves the bucket behind. */
	public static final ResourceKey<Item> CRUDE_OIL_BUCKET_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("crude_oil_bucket"));
	public static final Item CRUDE_OIL_BUCKET = new Item(new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET).setId(CRUDE_OIL_BUCKET_KEY));

	private WildcatterItems() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, POWER_METER_KEY, POWER_METER);
		Registry.register(BuiltInRegistries.ITEM, FIELD_MANUAL_KEY, FIELD_MANUAL);
		Registry.register(BuiltInRegistries.ITEM, SEISMOGRAPH_KEY, SEISMOGRAPH);
		Registry.register(BuiltInRegistries.ITEM, CRUDE_OIL_BUCKET_KEY, CRUDE_OIL_BUCKET);
	}
}
