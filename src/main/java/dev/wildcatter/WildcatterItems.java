package dev.wildcatter;

import dev.wildcatter.item.FieldManualItem;
import dev.wildcatter.item.Manual;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.item.SeismographItem;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import dev.wildcatter.Tuning;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.item.SpoolItem;
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

	/** 7a: a handheld gamma scintillometer that finds uranium ore (docs/ores.md). */
	public static final ResourceKey<Item> SCINTILLOMETER_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("scintillometer"));
	public static final Item SCINTILLOMETER = new dev.wildcatter.item.ScintillometerItem(new Item.Properties().stacksTo(1).setId(SCINTILLOMETER_KEY));

	/** 7b: caustic-calcined magnesia, the uranium mill's precipitant: one magnesite out of a furnace (docs/multiblocks/uranium-mill.md). */
	public static final ResourceKey<Item> MAGNESIA_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("magnesia"));
	public static final Item MAGNESIA = new dev.wildcatter.item.LabelledItem(new Item.Properties().setId(MAGNESIA_KEY),
			"MgO, caustic-calcined", String.format("%,.0f kg, from one magnesite", dev.wildcatter.ore.Ore.MAGNESITE.kgPerBlock() * Tuning.MGO_IN_MGCO3));
	public static final ResourceKey<Item> EMPTY_DRUM_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("empty_drum"));
	public static final Item EMPTY_DRUM = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(16).setId(EMPTY_DRUM_KEY),
			"208 L (55 gallon) open-head steel drum");
	/** A drum of yellowcake from the mill's drum filler. */
	public static final ResourceKey<Item> YELLOWCAKE_DRUM_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("yellowcake_drum"));
	public static final Item YELLOWCAKE_DRUM = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(1).setId(YELLOWCAKE_DRUM_KEY),
			String.format("%,.0f kg of uranium ore concentrate (magnesium diuranate)", Tuning.DRUM_NET_KG),
			String.format("%.0f %% U3O8: %,.0f kg of U3O8, %,.0f kg of uranium", Tuning.YELLOWCAKE_U3O8 * 100, Tuning.DRUM_NET_KG * Tuning.YELLOWCAKE_U3O8,
					Tuning.DRUM_NET_KG * Tuning.YELLOWCAKE_U3O8 * dev.wildcatter.ore.Ore.U_IN_U3O8));

	/** A bucket of crude oil. It burns in a firebox (as long as lava) and leaves the bucket behind. */
	public static final ResourceKey<Item> CRUDE_OIL_BUCKET_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("crude_oil_bucket"));
	public static final Item CRUDE_OIL_BUCKET = new Item(new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET).setId(CRUDE_OIL_BUCKET_KEY));

	public static final ResourceKey<Item> STEEL_INGOT_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("steel_ingot"));
	public static final Item STEEL_INGOT = new Item(new Item.Properties().setId(STEEL_INGOT_KEY));

	public static final ResourceKey<Item> PLASTIC_SHEET_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("plastic_sheet"));
	public static final Item PLASTIC_SHEET = new Item(new Item.Properties().setId(PLASTIC_SHEET_KEY));

	/** A combination starter bucket for a motor control center: it clicks into an empty space of a starter section (docs/multiblocks/motor-control-center.md). */
	public static final ResourceKey<Item> MCC_BUCKET_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("mcc_bucket"));
	public static final Item MCC_BUCKET = new Item(new Item.Properties().stacksTo(16).setId(MCC_BUCKET_KEY));
	/** The land rig's 250-ton AC top drive: clicked into the rig's mast (docs/multiblocks/land-rig.md). */
	public static final ResourceKey<Item> TOP_DRIVE_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("top_drive"));
	public static final Item TOP_DRIVE = new Item(new Item.Properties().stacksTo(1).setId(TOP_DRIVE_KEY));

	/** A bucket of diesel. It burns in a firebox, or fills a diesel engine by hand. */
	public static final ResourceKey<Item> DIESEL_BUCKET_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("diesel_bucket"));
	public static final Item DIESEL_BUCKET = new Item(new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET).setId(DIESEL_BUCKET_KEY));

	/** Conductor spools: a span of a tier, bare or plastic covered. A spool holds {@link Tuning#SPOOL_SPAN_BLOCKS} blocks of span. */
	public static final Item SPOOL_LV = spool("conductor_spool_lv", Voltage.LOW, false);
	public static final Item SPOOL_DIST = spool("conductor_spool_dist", Voltage.DISTRIBUTION, false);
	public static final Item SPOOL_HV = spool("conductor_spool_hv", Voltage.HIGH, false);
	public static final Item COVERED_SPOOL_LV = spool("covered_conductor_spool_lv", Voltage.LOW, true);
	public static final Item COVERED_SPOOL_DIST = spool("covered_conductor_spool_dist", Voltage.DISTRIBUTION, true);
	public static final Item COVERED_SPOOL_HV = spool("covered_conductor_spool_hv", Voltage.HIGH, true);

	/** Genset parts, crafted and then built into the engine and the alternator (docs/multiblocks/genset.md). */
	public static final String[] GENSET_PARTS = {"engine_block_casting", "piston", "crankshaft", "cylinder_head", "fuel_injector", "spark_plug",
			"stator_winding", "rotor_field", "exciter"};
	public static final java.util.Map<String, Item> PARTS = new java.util.LinkedHashMap<>();

	static {
		for (String name : GENSET_PARTS) {
			PARTS.put(name, new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Wildcatter.id(name)))));
		}
	}

	private static Item spool(String name, Voltage tier, boolean covered) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Wildcatter.id(name));
		return new SpoolItem(new Item.Properties().durability(Tuning.SPOOL_SPAN_BLOCKS).setId(key), tier, covered);
	}

	private static void registerSpool(String name, Item item) {
		Registry.register(BuiltInRegistries.ITEM, ResourceKey.create(Registries.ITEM, Wildcatter.id(name)), item);
	}

	private WildcatterItems() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, POWER_METER_KEY, POWER_METER);
		Registry.register(BuiltInRegistries.ITEM, FIELD_MANUAL_KEY, FIELD_MANUAL);
		Registry.register(BuiltInRegistries.ITEM, SEISMOGRAPH_KEY, SEISMOGRAPH);
		Registry.register(BuiltInRegistries.ITEM, SCINTILLOMETER_KEY, SCINTILLOMETER);
		Registry.register(BuiltInRegistries.ITEM, MAGNESIA_KEY, MAGNESIA);
		Registry.register(BuiltInRegistries.ITEM, EMPTY_DRUM_KEY, EMPTY_DRUM);
		Registry.register(BuiltInRegistries.ITEM, YELLOWCAKE_DRUM_KEY, YELLOWCAKE_DRUM);
		Registry.register(BuiltInRegistries.ITEM, CRUDE_OIL_BUCKET_KEY, CRUDE_OIL_BUCKET);
		Registry.register(BuiltInRegistries.ITEM, PLASTIC_SHEET_KEY, PLASTIC_SHEET);
		Registry.register(BuiltInRegistries.ITEM, STEEL_INGOT_KEY, STEEL_INGOT);
		Registry.register(BuiltInRegistries.ITEM, DIESEL_BUCKET_KEY, DIESEL_BUCKET);
		Registry.register(BuiltInRegistries.ITEM, MCC_BUCKET_KEY, MCC_BUCKET);
		Registry.register(BuiltInRegistries.ITEM, TOP_DRIVE_KEY, TOP_DRIVE);
		PARTS.forEach((name, item) -> Registry.register(BuiltInRegistries.ITEM, ResourceKey.create(Registries.ITEM, Wildcatter.id(name)), item));
		registerSpool("conductor_spool_lv", SPOOL_LV);
		registerSpool("conductor_spool_dist", SPOOL_DIST);
		registerSpool("conductor_spool_hv", SPOOL_HV);
		registerSpool("covered_conductor_spool_lv", COVERED_SPOOL_LV);
		registerSpool("covered_conductor_spool_dist", COVERED_SPOOL_DIST);
		registerSpool("covered_conductor_spool_hv", COVERED_SPOOL_HV);
	}
}
