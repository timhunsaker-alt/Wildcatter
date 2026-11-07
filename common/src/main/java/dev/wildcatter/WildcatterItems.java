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

	/** 7c: a 4 in submersible pump (7.5 hp, 480 V): clicked into an ISR well, it makes the well a producer. */
	public static final ResourceKey<Item> SUBMERSIBLE_PUMP_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("submersible_pump"));
	public static final Item SUBMERSIBLE_PUMP = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(16).setId(SUBMERSIBLE_PUMP_KEY),
			"4 in submersible, 7.5 hp, 480 V", String.format("%.1f L/s; click it into an ISR well to make a producer", Tuning.ISR_PRODUCER_LPS));

	/** 7d: a 48Y cylinder, empty and full of natural UF6 (docs/multiblocks/conversion.md). */
	public static final ResourceKey<Item> EMPTY_48Y_CYLINDER_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("empty_48y_cylinder"));
	public static final Item EMPTY_48Y_CYLINDER = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(4).setId(EMPTY_48Y_CYLINDER_KEY),
			String.format("48 in steel UF6 cylinder (ANSI N14.1), %,.0f kg tare", Tuning.CYLINDER_48Y_TARE));
	public static final ResourceKey<Item> UF6_CYLINDER_48Y_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("uf6_cylinder_48y"));
	public static final Item UF6_CYLINDER_48Y = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(1).setId(UF6_CYLINDER_48Y_KEY),
			String.format("%,.0f kg of natural UF6, %,.0f kg of uranium", Tuning.CYLINDER_48Y_KG, Tuning.CYLINDER_48Y_KG / Tuning.UF6_PER_U),
			"0.711 % U-235, as mined");

	/** 7e: the cascade's cylinders (docs/multiblocks/enrichment.md): a 30B, empty and full of LEU, and a 48Y of tails. */
	public static final ResourceKey<Item> EMPTY_30B_CYLINDER_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("empty_30b_cylinder"));
	public static final Item EMPTY_30B_CYLINDER = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(4).setId(EMPTY_30B_CYLINDER_KEY),
			String.format("30 in steel UF6 cylinder (ANSI N14.1), %,.0f kg tare", Tuning.CYLINDER_30B_TARE));
	public static final ResourceKey<Item> LEU_CYLINDER_30B_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("leu_cylinder_30b"));
	public static final Item LEU_CYLINDER_30B = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(1).setId(LEU_CYLINDER_30B_KEY),
			String.format("%,.0f kg of enriched UF6, %,.0f kg of uranium", Tuning.CYLINDER_30B_KG, Tuning.CYLINDER_30B_KG / Tuning.UF6_PER_U),
			String.format("%.2f %% U-235: low-enriched, for fuel", Tuning.ASSAY_PRODUCT * 100));
	public static final ResourceKey<Item> TAILS_CYLINDER_48Y_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("tails_cylinder_48y"));
	public static final Item TAILS_CYLINDER_48Y = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(1).setId(TAILS_CYLINDER_48Y_KEY),
			String.format("%,.0f kg of depleted UF6, %,.0f kg of uranium", Tuning.CYLINDER_48Y_KG, Tuning.CYLINDER_48Y_KG / Tuning.UF6_PER_U),
			String.format("%.2f %% U-235: the cascade's tails", Tuning.ASSAY_TAILS * 100));

	/** 7f: the zirconium plant's tubes (docs/multiblocks/zirconium.md), for the 17x17 assembly. */
	public static final ResourceKey<Item> ZIRCALOY_TUBE_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("zircaloy_tube"));
	public static final Item ZIRCALOY_TUBE = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(64).setId(ZIRCALOY_TUBE_KEY),
			String.format("Zircaloy-4 fuel cladding, %.2f mm OD, %.2f mm wall, %.2f m", Tuning.CLAD_OD_MM, Tuning.CLAD_WALL_MM, Tuning.CLAD_LENGTH_M),
			String.format("%.0f g; 264 make a 17x17 assembly's rods", dev.wildcatter.fluid.Zirconium.claddingKg() * 1000));
	public static final ResourceKey<Item> GUIDE_THIMBLE_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("guide_thimble"));
	public static final Item GUIDE_THIMBLE = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(64).setId(GUIDE_THIMBLE_KEY),
			String.format("Zircaloy-4 guide thimble, %.2f mm OD, %.2f mm wall, %.1f m", Tuning.THIMBLE_OD_MM, Tuning.THIMBLE_WALL_MM, Tuning.THIMBLE_LENGTH_M),
			"24 for the control rods and 1 instrument tube in a 17x17 assembly");
	public static final ResourceKey<Item> SPACER_GRID_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("spacer_grid"));
	public static final Item SPACER_GRID = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(64).setId(SPACER_GRID_KEY),
			String.format("Zircaloy-4 mid grid for a 17x17 assembly, %.1f kg", Tuning.SPACER_GRID_KG), "6 to an assembly, with 2 end grids");

	/** 7g: helium cylinders (docs/multiblocks/fuel.md), from the gas plant's helium recovery unit to the rod welder. */
	public static final ResourceKey<Item> EMPTY_HELIUM_CYLINDER_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("empty_helium_cylinder"));
	public static final Item EMPTY_HELIUM_CYLINDER = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(16).setId(EMPTY_HELIUM_CYLINDER_KEY),
			"Steel gas cylinder, 300 cf at 2,400 psi");
	public static final ResourceKey<Item> HELIUM_CYLINDER_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("helium_cylinder"));
	public static final Item HELIUM_CYLINDER = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(16).setId(HELIUM_CYLINDER_KEY),
			String.format("%.1f m3 of helium, 99.995 %%", Tuning.HELIUM_CYLINDER_M3), "fills about 21,000 fuel rods at 2 MPa");
	// 8e: the dosimeter (docs/multiblocks/fuel-handling.md)
	public static final ResourceKey<Item> DOSIMETER_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("dosimeter"));
	public static final Item DOSIMETER = new dev.wildcatter.item.DosimeterItem(new Item.Properties().stacksTo(1).setId(DOSIMETER_KEY));
	// 8a: the boron plant (docs/multiblocks/boron.md)
	public static final ResourceKey<Item> BORIC_ACID_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("boric_acid"));
	public static final Item BORIC_ACID = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(64).setId(BORIC_ACID_KEY),
			String.format("%.0f kg bag of boric acid (H3BO3), %.2f kg of boron", Tuning.BORIC_ACID_BAG_KG, Tuning.BORIC_ACID_BAG_KG * Tuning.B_IN_BORIC_ACID),
			"for the CVCS's tank, or the B4C furnace");
	public static final ResourceKey<Item> B4C_PELLETS_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("b4c_pellets"));
	public static final Item B4C_PELLETS = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(64).setId(B4C_PELLETS_KEY),
			"1 kg tray of boron carbide pellets, 70 % dense", "9 trays to an RCCA's 24 rodlets");
	public static final ResourceKey<Item> RCCA_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("rcca"));
	public static final Item RCCA = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(1).setId(RCCA_KEY),
			"Rod cluster control assembly: 24 B4C rodlets on a spider", "5 to the reactor's core; load it by right-click on the cold vessel");

	// 10b2: bulk bags and chlorine ton containers (docs/multiblocks/alumina-plant.md)
	public static final ResourceKey<Item> EMPTY_BULK_BAG_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("empty_bulk_bag"));
	public static final Item EMPTY_BULK_BAG = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(16).setId(EMPTY_BULK_BAG_KEY),
			String.format("Woven polypropylene FIBC, %,.0f kg", Tuning.BULK_BAG_KG));
	public static final ResourceKey<Item> ALUMINA_BAG_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("alumina_bag"));
	public static final Item ALUMINA_BAG = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(16).setId(ALUMINA_BAG_KEY),
			String.format("%,.0f kg of smelter-grade alumina (Al2O3)", Tuning.BULK_BAG_KG), "for the potline (10b3)");
	public static final ResourceKey<Item> HYDRATE_BAG_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("hydrate_bag"));
	public static final Item HYDRATE_BAG = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(16).setId(HYDRATE_BAG_KEY),
			String.format("%,.0f kg of aluminium hydroxide (gibbsite hydrate)", Tuning.BULK_BAG_KG), "for the AlF3 reactor");
	public static final ResourceKey<Item> ALF3_BAG_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("alf3_bag"));
	public static final Item ALF3_BAG = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(16).setId(ALF3_BAG_KEY),
			String.format("%,.0f kg of aluminium fluoride (AlF3)", Tuning.BULK_BAG_KG), "the potline's bath additive (10b3)");
	public static final ResourceKey<Item> QUICKLIME_BAG_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("quicklime_bag"));
	public static final Item QUICKLIME_BAG = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(16).setId(QUICKLIME_BAG_KEY),
			String.format("%,.0f kg of quicklime (CaO)", Tuning.BULK_BAG_KG), "for the refinery's causticizer");
	public static final ResourceKey<Item> EMPTY_TON_CONTAINER_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("empty_ton_container"));
	public static final Item EMPTY_TON_CONTAINER = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(4).setId(EMPTY_TON_CONTAINER_KEY),
			String.format("Welded steel chlorine ton container, %,.0f kg tare", Tuning.CL2_TON_TARE));
	public static final ResourceKey<Item> CHLORINE_TON_CONTAINER_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("chlorine_ton_container"));
	public static final Item CHLORINE_TON_CONTAINER = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(1).setId(CHLORINE_TON_CONTAINER_KEY),
			String.format("%,.0f kg (2,000 lb) of liquid chlorine", Tuning.CL2_TON_KG));

	// 10b3a: coke and anodes (docs/multiblocks/delayed-coker.md, coke-ovens.md, anode-plant.md)
	public static final ResourceKey<Item> GREEN_COKE_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("green_petroleum_coke"));
	public static final Item GREEN_COKE = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(16).setId(GREEN_COKE_KEY),
			String.format("%,.0f kg of green (raw) sponge coke", Tuning.GREEN_COKE_KG), "for the coke calciner");
	public static final ResourceKey<Item> CALCINED_COKE_BAG_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("calcined_coke_bag"));
	public static final Item CALCINED_COKE_BAG = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(16).setId(CALCINED_COKE_BAG_KEY),
			String.format("%,.0f kg of calcined petroleum coke", Tuning.BULK_BAG_KG), "for the anode paste plant");
	public static final ResourceKey<Item> MET_COKE_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("metallurgical_coke"));
	public static final Item MET_COKE = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(64).setId(MET_COKE_KEY),
			String.format("%.1f kg of blast-furnace coke, %.0f %% carbon", Tuning.MET_COKE_KG, Tuning.MET_COKE_CARBON * 100), "works wherever coal does");
	public static final ResourceKey<Item> GREEN_ANODE_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("green_anode"));
	public static final Item GREEN_ANODE = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(16).setId(GREEN_ANODE_KEY),
			String.format("Green prebake anode, %,.0f kg, 1.6 x 0.8 x 0.6 m", Tuning.GREEN_ANODE_KG), "for the baking furnace");
	public static final ResourceKey<Item> BAKED_ANODE_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("baked_anode"));
	public static final Item BAKED_ANODE = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(16).setId(BAKED_ANODE_KEY),
			String.format("Baked prebake anode, %,.0f kg", Tuning.BAKED_ANODE_KG), "for the rodding shop");
	public static final ResourceKey<Item> RODDED_ANODE_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("rodded_anode"));
	public static final Item RODDED_ANODE = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(16).setId(RODDED_ANODE_KEY),
			String.format("Rodded anode: a %,.0f kg baked block cast onto its rod", Tuning.BAKED_ANODE_KG), "for the potline");
	public static final ResourceKey<Item> SPENT_ANODE_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("spent_anode"));
	public static final Item SPENT_ANODE = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(16).setId(SPENT_ANODE_KEY),
			String.format("Spent anode: a %,.0f kg butt on its rod", Tuning.BUTT_KG), "strip it in the rodding shop");
	public static final ResourceKey<Item> CRUSHED_BUTTS_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("crushed_butts"));
	public static final Item CRUSHED_BUTTS = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(16).setId(CRUSHED_BUTTS_KEY),
			String.format("%,.0f kg of cleaned, crushed anode butts", Tuning.BUTT_KG), "for the anode paste plant");
	// 10b3b: the potline (docs/multiblocks/potline.md)
	public static final ResourceKey<Item> ALUMINIUM_INGOT_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("aluminium_ingot"));
	public static final Item ALUMINIUM_INGOT = new dev.wildcatter.item.LabelledItem(new Item.Properties().setId(ALUMINIUM_INGOT_KEY),
			String.format("A %.1f kg (50 lb) primary aluminium ingot", Tuning.AL_INGOT_KG));
	public static final ResourceKey<Item> REACTED_ALUMINA_BAG_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("reacted_alumina_bag"));
	public static final Item REACTED_ALUMINA_BAG = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(16).setId(REACTED_ALUMINA_BAG_KEY),
			String.format("%,.0f kg of reacted alumina, carrying the fluoride the fume treatment centre caught", Tuning.BULK_BAG_KG), "the pots take it first");
	public static final ResourceKey<Item> SPENT_POT_LINING_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("spent_pot_lining"));
	public static final Item SPENT_POT_LINING = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(16).setId(SPENT_POT_LINING_KEY),
			"Spent pot lining: a frozen pot's carbon cathode and refractory, with its fluoride", "no use yet");
	public static final ResourceKey<Item> TAPPING_CRUCIBLE_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("tapping_crucible"));
	public static final Item TAPPING_CRUCIBLE = new dev.wildcatter.item.TappingCrucibleItem(new Item.Properties().stacksTo(1).setId(TAPPING_CRUCIBLE_KEY));
	// 10c1: blueprints (docs/blueprints.md)
	public static final ResourceKey<Item> BLUEPRINT_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("blueprint"));
	public static final Item BLUEPRINT = new dev.wildcatter.item.BlueprintItem(new Item.Properties().stacksTo(1).setId(BLUEPRINT_KEY));
	public static final ResourceKey<Item> SURVEY_TOOL_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("survey_tool"));
	public static final Item SURVEY_TOOL = new dev.wildcatter.item.SurveyToolItem(new Item.Properties().stacksTo(1).setId(SURVEY_TOOL_KEY));
	// 10d1: the grounding clamp set (docs/multiblocks/gen-bus-protection.md)
	public static final ResourceKey<Item> GROUNDING_SET_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("grounding_set"));
	public static final Item GROUNDING_SET = new dev.wildcatter.item.GroundingSetItem(new Item.Properties().stacksTo(1).setId(GROUNDING_SET_KEY));
	public static final ResourceKey<Item> ANODE_ROD_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("anode_rod"));
	public static final Item ANODE_ROD = new dev.wildcatter.item.LabelledItem(new Item.Properties().stacksTo(16).setId(ANODE_ROD_KEY),
			"Anode rod: a stem with its steel yoke and stubs", "reused round and round");

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
	/** A soft-starter bucket for a motor control center (docs/multiblocks/soft-starter.md). */
	public static final ResourceKey<Item> MCC_SOFT_STARTER_BUCKET_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("mcc_soft_starter_bucket"));
	public static final Item MCC_SOFT_STARTER_BUCKET = new Item(new Item.Properties().stacksTo(16).setId(MCC_SOFT_STARTER_BUCKET_KEY));
	/** A drive bucket for a motor control center, up to 100 hp (docs/multiblocks/vfd.md). */
	public static final ResourceKey<Item> MCC_DRIVE_BUCKET_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("mcc_drive_bucket"));
	public static final Item MCC_DRIVE_BUCKET = new Item(new Item.Properties().stacksTo(16).setId(MCC_DRIVE_BUCKET_KEY));
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

	// ---- 10d3: branch breakers and cartridge fuses (docs/multiblocks/panelboards.md, safety-switch.md, load-interrupter.md) ----
	/** The branch breakers, by item name: one per rating. Load-centre ones are the LOW tier, panelboard ones the INDUSTRIAL tier. */
	public static final java.util.Map<String, dev.wildcatter.item.BranchBreakerItem> BRANCH_BREAKERS = new java.util.LinkedHashMap<>();
	/** The cartridge fuses, by item name: class J for the safety switch, E-rated for the fused load-interrupter. */
	public static final java.util.Map<String, dev.wildcatter.item.CartridgeFuseItem> CARTRIDGE_FUSES = new java.util.LinkedHashMap<>();

	static {
		branch("branch_breaker_1p_15", Voltage.LOW, 15, 1, false);
		branch("branch_breaker_1p_20", Voltage.LOW, 20, 1, false);
		branch("branch_breaker_2p_30", Voltage.LOW, 30, 2, false);
		branch("branch_breaker_2p_50", Voltage.LOW, 50, 2, false);
		branch("gfci_breaker_1p_20", Voltage.LOW, 20, 1, true);
		branch("branch_breaker_277_1p_20", Voltage.INDUSTRIAL, 20, 1, false);
		branch("branch_breaker_480_3p_20", Voltage.INDUSTRIAL, 20, 3, false);
		branch("branch_breaker_480_3p_30", Voltage.INDUSTRIAL, 30, 3, false);
		branch("branch_breaker_480_3p_60", Voltage.INDUSTRIAL, 60, 3, false);
		branch("branch_breaker_480_3p_100", Voltage.INDUSTRIAL, 100, 3, false);
		for (int amps : new int[] {30, 60, 100, 200}) {
			fuse("fuse_class_j_" + amps, dev.wildcatter.item.CartridgeFuseItem.Kind.CLASS_J, amps);
		}
		for (int amps : new int[] {10, 25, 50, 100, 200}) {
			fuse("fuse_e_" + amps, dev.wildcatter.item.CartridgeFuseItem.Kind.E_RATED, amps);
		}
	}

	private static void branch(String name, Voltage tier, int amps, int poles, boolean gfci) {
		BRANCH_BREAKERS.put(name, new dev.wildcatter.item.BranchBreakerItem(new Item.Properties().stacksTo(16)
				.setId(ResourceKey.create(Registries.ITEM, Wildcatter.id(name))), tier, amps, poles, gfci));
	}

	private static void fuse(String name, dev.wildcatter.item.CartridgeFuseItem.Kind kind, int amps) {
		CARTRIDGE_FUSES.put(name, new dev.wildcatter.item.CartridgeFuseItem(new Item.Properties().stacksTo(16)
				.setId(ResourceKey.create(Registries.ITEM, Wildcatter.id(name))), kind, amps));
	}

	/** The branch-breaker item of this kind, or null if there is none. */
	public static dev.wildcatter.item.BranchBreakerItem branchBreaker(Voltage tier, int amps, int poles, boolean gfci) {
		for (dev.wildcatter.item.BranchBreakerItem item : BRANCH_BREAKERS.values()) {
			if (item.tier() == tier && item.amps() == amps && item.poles() == poles && item.gfci() == gfci) {
				return item;
			}
		}
		return null;
	}

	/** The cartridge-fuse item of this kind and rating, or null if there is none. */
	public static dev.wildcatter.item.CartridgeFuseItem cartridgeFuse(dev.wildcatter.item.CartridgeFuseItem.Kind kind, int amps) {
		for (dev.wildcatter.item.CartridgeFuseItem item : CARTRIDGE_FUSES.values()) {
			if (item.kind() == kind && item.amps() == amps) {
				return item;
			}
		}
		return null;
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

	public static final ResourceKey<Item> CPU_SMALL_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("cpu_small"));
	public static final Item CPU_SMALL = new dev.wildcatter.control.PlcModuleItem(new Item.Properties().stacksTo(16).setId(CPU_SMALL_KEY), dev.wildcatter.control.PlcModuleItem.Type.CPU_SMALL);
	public static final ResourceKey<Item> CPU_LARGE_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("cpu_large"));
	public static final Item CPU_LARGE = new dev.wildcatter.control.PlcModuleItem(new Item.Properties().stacksTo(16).setId(CPU_LARGE_KEY), dev.wildcatter.control.PlcModuleItem.Type.CPU_LARGE);
	public static final ResourceKey<Item> DI16_MODULE_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("di16_module"));
	public static final Item DI16_MODULE = new dev.wildcatter.control.PlcModuleItem(new Item.Properties().stacksTo(16).setId(DI16_MODULE_KEY), dev.wildcatter.control.PlcModuleItem.Type.DI16);
	public static final ResourceKey<Item> DO16_MODULE_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("do16_module"));
	public static final Item DO16_MODULE = new dev.wildcatter.control.PlcModuleItem(new Item.Properties().stacksTo(16).setId(DO16_MODULE_KEY), dev.wildcatter.control.PlcModuleItem.Type.DO16);
	public static final ResourceKey<Item> AI8_MODULE_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("ai8_module"));
	public static final Item AI8_MODULE = new dev.wildcatter.control.PlcModuleItem(new Item.Properties().stacksTo(16).setId(AI8_MODULE_KEY), dev.wildcatter.control.PlcModuleItem.Type.AI8);
	public static final ResourceKey<Item> AO4_MODULE_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("ao4_module"));
	public static final Item AO4_MODULE = new dev.wildcatter.control.PlcModuleItem(new Item.Properties().stacksTo(16).setId(AO4_MODULE_KEY), dev.wildcatter.control.PlcModuleItem.Type.AO4);
	public static final ResourceKey<Item> RTD4_MODULE_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("rtd4_module"));
	public static final Item RTD4_MODULE = new dev.wildcatter.control.PlcModuleItem(new Item.Properties().stacksTo(16).setId(RTD4_MODULE_KEY), dev.wildcatter.control.PlcModuleItem.Type.RTD4);
	public static final ResourceKey<Item> HSC2_MODULE_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("hsc2_module"));
	public static final Item HSC2_MODULE = new dev.wildcatter.control.PlcModuleItem(new Item.Properties().stacksTo(16).setId(HSC2_MODULE_KEY), dev.wildcatter.control.PlcModuleItem.Type.HSC2);
	public static final ResourceKey<Item> CONTROL_KEY_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("control_key"));
	public static final Item CONTROL_KEY = new dev.wildcatter.control.ControlKeyItem(new Item.Properties().stacksTo(1).setId(CONTROL_KEY_KEY));

	public static final ResourceKey<Item> MEMORY_CARD_KEY = ResourceKey.create(Registries.ITEM, Wildcatter.id("memory_card"));
	public static final Item MEMORY_CARD = new dev.wildcatter.control.MemoryCardItem(new Item.Properties().stacksTo(1).setId(MEMORY_CARD_KEY));

	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, POWER_METER_KEY, POWER_METER);
		Registry.register(BuiltInRegistries.ITEM, MEMORY_CARD_KEY, MEMORY_CARD);
		Registry.register(BuiltInRegistries.ITEM, CPU_SMALL_KEY, CPU_SMALL);
		Registry.register(BuiltInRegistries.ITEM, CPU_LARGE_KEY, CPU_LARGE);
		Registry.register(BuiltInRegistries.ITEM, DI16_MODULE_KEY, DI16_MODULE);
		Registry.register(BuiltInRegistries.ITEM, DO16_MODULE_KEY, DO16_MODULE);
		Registry.register(BuiltInRegistries.ITEM, CONTROL_KEY_KEY, CONTROL_KEY);
		Registry.register(BuiltInRegistries.ITEM, AI8_MODULE_KEY, AI8_MODULE);
		Registry.register(BuiltInRegistries.ITEM, AO4_MODULE_KEY, AO4_MODULE);
		Registry.register(BuiltInRegistries.ITEM, RTD4_MODULE_KEY, RTD4_MODULE);
		Registry.register(BuiltInRegistries.ITEM, HSC2_MODULE_KEY, HSC2_MODULE);
		Registry.register(BuiltInRegistries.ITEM, FIELD_MANUAL_KEY, FIELD_MANUAL);
		Registry.register(BuiltInRegistries.ITEM, SEISMOGRAPH_KEY, SEISMOGRAPH);
		Registry.register(BuiltInRegistries.ITEM, SCINTILLOMETER_KEY, SCINTILLOMETER);
		Registry.register(BuiltInRegistries.ITEM, MAGNESIA_KEY, MAGNESIA);
		Registry.register(BuiltInRegistries.ITEM, EMPTY_DRUM_KEY, EMPTY_DRUM);
		Registry.register(BuiltInRegistries.ITEM, YELLOWCAKE_DRUM_KEY, YELLOWCAKE_DRUM);
		Registry.register(BuiltInRegistries.ITEM, SUBMERSIBLE_PUMP_KEY, SUBMERSIBLE_PUMP);
		Registry.register(BuiltInRegistries.ITEM, EMPTY_48Y_CYLINDER_KEY, EMPTY_48Y_CYLINDER);
		Registry.register(BuiltInRegistries.ITEM, UF6_CYLINDER_48Y_KEY, UF6_CYLINDER_48Y);
		Registry.register(BuiltInRegistries.ITEM, EMPTY_30B_CYLINDER_KEY, EMPTY_30B_CYLINDER);
		Registry.register(BuiltInRegistries.ITEM, LEU_CYLINDER_30B_KEY, LEU_CYLINDER_30B);
		Registry.register(BuiltInRegistries.ITEM, TAILS_CYLINDER_48Y_KEY, TAILS_CYLINDER_48Y);
		Registry.register(BuiltInRegistries.ITEM, ZIRCALOY_TUBE_KEY, ZIRCALOY_TUBE);
		Registry.register(BuiltInRegistries.ITEM, GUIDE_THIMBLE_KEY, GUIDE_THIMBLE);
		Registry.register(BuiltInRegistries.ITEM, SPACER_GRID_KEY, SPACER_GRID);
		Registry.register(BuiltInRegistries.ITEM, EMPTY_HELIUM_CYLINDER_KEY, EMPTY_HELIUM_CYLINDER);
		Registry.register(BuiltInRegistries.ITEM, HELIUM_CYLINDER_KEY, HELIUM_CYLINDER);
		Registry.register(BuiltInRegistries.ITEM, BORIC_ACID_KEY, BORIC_ACID);
		Registry.register(BuiltInRegistries.ITEM, B4C_PELLETS_KEY, B4C_PELLETS);
		Registry.register(BuiltInRegistries.ITEM, RCCA_KEY, RCCA);
		Registry.register(BuiltInRegistries.ITEM, DOSIMETER_KEY, DOSIMETER);
		Registry.register(BuiltInRegistries.ITEM, EMPTY_BULK_BAG_KEY, EMPTY_BULK_BAG);
		Registry.register(BuiltInRegistries.ITEM, ALUMINA_BAG_KEY, ALUMINA_BAG);
		Registry.register(BuiltInRegistries.ITEM, HYDRATE_BAG_KEY, HYDRATE_BAG);
		Registry.register(BuiltInRegistries.ITEM, ALF3_BAG_KEY, ALF3_BAG);
		Registry.register(BuiltInRegistries.ITEM, QUICKLIME_BAG_KEY, QUICKLIME_BAG);
		Registry.register(BuiltInRegistries.ITEM, EMPTY_TON_CONTAINER_KEY, EMPTY_TON_CONTAINER);
		Registry.register(BuiltInRegistries.ITEM, CHLORINE_TON_CONTAINER_KEY, CHLORINE_TON_CONTAINER);
		Registry.register(BuiltInRegistries.ITEM, GREEN_COKE_KEY, GREEN_COKE);
		Registry.register(BuiltInRegistries.ITEM, CALCINED_COKE_BAG_KEY, CALCINED_COKE_BAG);
		Registry.register(BuiltInRegistries.ITEM, MET_COKE_KEY, MET_COKE);
		Registry.register(BuiltInRegistries.ITEM, GREEN_ANODE_KEY, GREEN_ANODE);
		Registry.register(BuiltInRegistries.ITEM, BAKED_ANODE_KEY, BAKED_ANODE);
		Registry.register(BuiltInRegistries.ITEM, RODDED_ANODE_KEY, RODDED_ANODE);
		Registry.register(BuiltInRegistries.ITEM, SPENT_ANODE_KEY, SPENT_ANODE);
		Registry.register(BuiltInRegistries.ITEM, CRUSHED_BUTTS_KEY, CRUSHED_BUTTS);
		Registry.register(BuiltInRegistries.ITEM, ANODE_ROD_KEY, ANODE_ROD);
		Registry.register(BuiltInRegistries.ITEM, ALUMINIUM_INGOT_KEY, ALUMINIUM_INGOT);
		Registry.register(BuiltInRegistries.ITEM, REACTED_ALUMINA_BAG_KEY, REACTED_ALUMINA_BAG);
		Registry.register(BuiltInRegistries.ITEM, SPENT_POT_LINING_KEY, SPENT_POT_LINING);
		Registry.register(BuiltInRegistries.ITEM, TAPPING_CRUCIBLE_KEY, TAPPING_CRUCIBLE);
		Registry.register(BuiltInRegistries.ITEM, BLUEPRINT_KEY, BLUEPRINT);
		Registry.register(BuiltInRegistries.ITEM, SURVEY_TOOL_KEY, SURVEY_TOOL);
		Registry.register(BuiltInRegistries.ITEM, GROUNDING_SET_KEY, GROUNDING_SET);
		Registry.register(BuiltInRegistries.ITEM, CRUDE_OIL_BUCKET_KEY, CRUDE_OIL_BUCKET);
		Registry.register(BuiltInRegistries.ITEM, PLASTIC_SHEET_KEY, PLASTIC_SHEET);
		Registry.register(BuiltInRegistries.ITEM, STEEL_INGOT_KEY, STEEL_INGOT);
		Registry.register(BuiltInRegistries.ITEM, DIESEL_BUCKET_KEY, DIESEL_BUCKET);
		Registry.register(BuiltInRegistries.ITEM, MCC_BUCKET_KEY, MCC_BUCKET);
		Registry.register(BuiltInRegistries.ITEM, MCC_SOFT_STARTER_BUCKET_KEY, MCC_SOFT_STARTER_BUCKET);
		Registry.register(BuiltInRegistries.ITEM, MCC_DRIVE_BUCKET_KEY, MCC_DRIVE_BUCKET);
		Registry.register(BuiltInRegistries.ITEM, TOP_DRIVE_KEY, TOP_DRIVE);
		PARTS.forEach((name, item) -> Registry.register(BuiltInRegistries.ITEM, ResourceKey.create(Registries.ITEM, Wildcatter.id(name)), item));
		BRANCH_BREAKERS.forEach((name, item) -> Registry.register(BuiltInRegistries.ITEM, ResourceKey.create(Registries.ITEM, Wildcatter.id(name)), item));
		CARTRIDGE_FUSES.forEach((name, item) -> Registry.register(BuiltInRegistries.ITEM, ResourceKey.create(Registries.ITEM, Wildcatter.id(name)), item));
		registerSpool("conductor_spool_lv", SPOOL_LV);
		registerSpool("conductor_spool_dist", SPOOL_DIST);
		registerSpool("conductor_spool_hv", SPOOL_HV);
		registerSpool("covered_conductor_spool_lv", COVERED_SPOOL_LV);
		registerSpool("covered_conductor_spool_dist", COVERED_SPOOL_DIST);
		registerSpool("covered_conductor_spool_hv", COVERED_SPOOL_HV);
	}
}
