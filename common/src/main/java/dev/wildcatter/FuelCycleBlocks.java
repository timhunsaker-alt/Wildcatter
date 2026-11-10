package dev.wildcatter;

import static dev.wildcatter.BlockFactory.*;

import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/** The acid plant, uranium mill, ISR wellfield, conversion, enrichment, zirconium and fuel plants. Reached through {@link WildcatterBlocks}, which registers them. */
public interface FuelCycleBlocks {
	// 7b: the sulfuric acid plant and the uranium mill (docs/multiblocks/acid-plant.md, uranium-mill.md)
	Block SULFUR_MELTER = acidPart(dev.wildcatter.block.AcidPlantPartBlock.Part.SULFUR_MELTER);
	Block DRYING_TOWER = acidPart(dev.wildcatter.block.AcidPlantPartBlock.Part.DRYING_TOWER);
	Block MAIN_BLOWER = acidPart(dev.wildcatter.block.AcidPlantPartBlock.Part.MAIN_BLOWER);
	Block SULFUR_FURNACE = acidPart(dev.wildcatter.block.AcidPlantPartBlock.Part.SULFUR_FURNACE);
	Block WASTE_HEAT_BOILER = acidPart(dev.wildcatter.block.AcidPlantPartBlock.Part.WASTE_HEAT_BOILER);
	Block CONVERTER = acidPart(dev.wildcatter.block.AcidPlantPartBlock.Part.CONVERTER);
	Block INTERPASS_ABSORBER = acidPart(dev.wildcatter.block.AcidPlantPartBlock.Part.INTERPASS_ABSORBER);
	Block FINAL_ABSORBER = acidPart(dev.wildcatter.block.AcidPlantPartBlock.Part.FINAL_ABSORBER);
	// 14c: the acid plant's steam to the 5 MW turbine (docs/multiblocks/acid-plant-steam.md)
	Block ACID_SUPERHEATER = acidPart(dev.wildcatter.block.AcidPlantPartBlock.Part.ACID_SUPERHEATER);
	Block JAW_CRUSHER = millPart(dev.wildcatter.block.MillPartBlock.Part.JAW_CRUSHER);
	Block SAG_MILL = millPart(dev.wildcatter.block.MillPartBlock.Part.SAG_MILL);
	Block LEACH_TANK = millPart(dev.wildcatter.block.MillPartBlock.Part.LEACH_TANK);
	Block CCD_THICKENER = millPart(dev.wildcatter.block.MillPartBlock.Part.CCD_THICKENER);
	Block SX_CIRCUIT = millPart(dev.wildcatter.block.MillPartBlock.Part.SX_CIRCUIT);
	Block PRECIPITATION_TANK = millPart(dev.wildcatter.block.MillPartBlock.Part.PRECIPITATION_TANK);
	Block YELLOWCAKE_DRYER = millPart(dev.wildcatter.block.MillPartBlock.Part.YELLOWCAKE_DRYER);
	Block DRUM_FILLER = millPart(dev.wildcatter.block.MillPartBlock.Part.DRUM_FILLER);
	// 16a: the ADU route's calciner (docs/multiblocks/adu-yellowcake.md)
	Block URANIUM_CALCINER = millPart(dev.wildcatter.block.MillPartBlock.Part.URANIUM_CALCINER);
	// 7c: the ISR wellfield (docs/multiblocks/isr-wellfield.md)
	Block HEADER_HOUSE = wellfieldPart(dev.wildcatter.block.WellfieldPartBlock.Part.HEADER_HOUSE);
	Block OXYGEN_GENERATOR = wellfieldPart(dev.wildcatter.block.WellfieldPartBlock.Part.OXYGEN_GENERATOR);
	Block IX_COLUMNS = wellfieldPart(dev.wildcatter.block.WellfieldPartBlock.Part.IX_COLUMNS);
	// 16d1 (docs/multiblocks/isr-operations.md)
	Block CO2_LIQUEFIER = wellfieldPart(dev.wildcatter.block.WellfieldPartBlock.Part.CO2_LIQUEFIER);
	Block CO2_STORAGE_TANK = wellfieldPart(dev.wildcatter.block.WellfieldPartBlock.Part.CO2_STORAGE_TANK);
	Block RESIN_TRAILER = block("resin_trailer", props -> new dev.wildcatter.block.ResinTrailerBlock(props.mapColor(net.minecraft.world.level.material.MapColor.COLOR_LIGHT_GRAY)
			.strength(3.0F, 6.0F).sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion()));
	// 7d: conversion to UF6 (docs/multiblocks/conversion.md)
	Block REDUCTION_REACTOR = conversionPart(dev.wildcatter.block.ConversionPartBlock.Part.REDUCTION_REACTOR);
	Block HYDROFLUORINATOR = conversionPart(dev.wildcatter.block.ConversionPartBlock.Part.HYDROFLUORINATOR);
	Block FLUORINATOR = conversionPart(dev.wildcatter.block.ConversionPartBlock.Part.FLUORINATOR);
	Block COLD_TRAPS = conversionPart(dev.wildcatter.block.ConversionPartBlock.Part.COLD_TRAPS);
	Block DISTILLATION_COLUMNS = conversionPart(dev.wildcatter.block.ConversionPartBlock.Part.DISTILLATION_COLUMNS);
	Block CYLINDER_FILLER = conversionPart(dev.wildcatter.block.ConversionPartBlock.Part.CYLINDER_FILLER);
	Block STEAM_REFORMER = conversionPart(dev.wildcatter.block.ConversionPartBlock.Part.STEAM_REFORMER);
	Block HF_KILN = conversionPart(dev.wildcatter.block.ConversionPartBlock.Part.HF_KILN);
	Block HF_COLUMN = conversionPart(dev.wildcatter.block.ConversionPartBlock.Part.HF_COLUMN);
	Block RECTIFIER = conversionPart(dev.wildcatter.block.ConversionPartBlock.Part.RECTIFIER);
	Block FLUORINE_CELL_HOUSE = conversionPart(dev.wildcatter.block.ConversionPartBlock.Part.FLUORINE_CELL_HOUSE);
	// 16b1 (docs/multiblocks/conversion-extensions.md)
	Block CATHODE_GAS_SCRUBBER = conversionPart(dev.wildcatter.block.ConversionPartBlock.Part.CATHODE_GAS_SCRUBBER);
	Block KOH_SCRUBBER = conversionPart(dev.wildcatter.block.ConversionPartBlock.Part.KOH_SCRUBBER);
	// 7e: the centrifuge cascade (docs/multiblocks/enrichment.md)
	Block CENTRIFUGE = enrichmentPart(dev.wildcatter.block.EnrichmentPartBlock.Part.CENTRIFUGE);
	Block FREQUENCY_CONVERTER = enrichmentPart(dev.wildcatter.block.EnrichmentPartBlock.Part.FREQUENCY_CONVERTER);
	Block FEED_STATION = enrichmentPart(dev.wildcatter.block.EnrichmentPartBlock.Part.FEED_STATION);
	Block PRODUCT_TAKEOFF = enrichmentPart(dev.wildcatter.block.EnrichmentPartBlock.Part.PRODUCT_TAKEOFF);
	Block TAILS_TAKEOFF = enrichmentPart(dev.wildcatter.block.EnrichmentPartBlock.Part.TAILS_TAKEOFF);
	// 16c1 (docs/multiblocks/enrichment-operations.md)
	Block CASCADE_COOLING_SKID = enrichmentPart(dev.wildcatter.block.EnrichmentPartBlock.Part.CASCADE_COOLING_SKID);
	// 7f: the zirconium plant (docs/multiblocks/zirconium.md)
	Block MINERAL_SEPARATOR = zirconiumPart(dev.wildcatter.block.ZirconiumPartBlock.Part.MINERAL_SEPARATOR);
	Block CHLORINATOR = zirconiumPart(dev.wildcatter.block.ZirconiumPartBlock.Part.CHLORINATOR);
	Block HAFNIUM_COLUMN = zirconiumPart(dev.wildcatter.block.ZirconiumPartBlock.Part.HAFNIUM_COLUMN);
	Block KROLL_REACTOR = zirconiumPart(dev.wildcatter.block.ZirconiumPartBlock.Part.KROLL_REACTOR);
	Block VACUUM_DISTILLATION = zirconiumPart(dev.wildcatter.block.ZirconiumPartBlock.Part.VACUUM_DISTILLATION);
	Block VAR_FURNACE = zirconiumPart(dev.wildcatter.block.ZirconiumPartBlock.Part.VAR_FURNACE);
	Block EXTRUSION_PRESS = zirconiumPart(dev.wildcatter.block.ZirconiumPartBlock.Part.EXTRUSION_PRESS);
	Block PILGER_MILL = zirconiumPart(dev.wildcatter.block.ZirconiumPartBlock.Part.PILGER_MILL);
	Block VACUUM_ANNEAL_FURNACE = zirconiumPart(dev.wildcatter.block.ZirconiumPartBlock.Part.VACUUM_ANNEAL_FURNACE);
	Block MAGNESIUM_CELL = zirconiumPart(dev.wildcatter.block.ZirconiumPartBlock.Part.MAGNESIUM_CELL);
	Block MAGNESIA_CHLORINATOR = zirconiumPart(dev.wildcatter.block.ZirconiumPartBlock.Part.MAGNESIA_CHLORINATOR);
	Block CHLOR_ALKALI_CELL = zirconiumPart(dev.wildcatter.block.ZirconiumPartBlock.Part.CHLOR_ALKALI_CELL);
	// 7g: the fuel plant (docs/multiblocks/fuel.md), and the gas plant's helium recovery unit
	Block UF6_AUTOCLAVE = fuelPart(dev.wildcatter.block.FuelPartBlock.Part.UF6_AUTOCLAVE);
	Block IDR_KILN = fuelPart(dev.wildcatter.block.FuelPartBlock.Part.IDR_KILN);
	Block GRANULATOR = fuelPart(dev.wildcatter.block.FuelPartBlock.Part.GRANULATOR);
	Block PELLET_PRESS = fuelPart(dev.wildcatter.block.FuelPartBlock.Part.PELLET_PRESS);
	Block SINTERING_FURNACE = fuelPart(dev.wildcatter.block.FuelPartBlock.Part.SINTERING_FURNACE);
	Block PELLET_GRINDER = fuelPart(dev.wildcatter.block.FuelPartBlock.Part.PELLET_GRINDER);
	Block ROD_LOADER = fuelPart(dev.wildcatter.block.FuelPartBlock.Part.ROD_LOADER);
	Block ROD_WELDER = fuelPart(dev.wildcatter.block.FuelPartBlock.Part.ROD_WELDER);
	Block ASSEMBLY_FIXTURE = fuelPart(dev.wildcatter.block.FuelPartBlock.Part.ASSEMBLY_FIXTURE);
	Block HELIUM_RECOVERY_UNIT = fuelPart(dev.wildcatter.block.FuelPartBlock.Part.HELIUM_RECOVERY_UNIT);
	// 10b2: the Bayer refinery, lime kiln, AlF3 reactor, bagging station and chlorine filler (docs/multiblocks/bayer-refinery.md, alumina-plant.md)
	Block BALL_MILL = aluminaPart(dev.wildcatter.block.AluminaPartBlock.Part.BALL_MILL);
	Block DIGESTER = aluminaPart(dev.wildcatter.block.AluminaPartBlock.Part.DIGESTER);
	Block FLASH_TANKS = aluminaPart(dev.wildcatter.block.AluminaPartBlock.Part.FLASH_TANKS);
	Block SETTLER = aluminaPart(dev.wildcatter.block.AluminaPartBlock.Part.SETTLER);
	Block MUD_WASHER = aluminaPart(dev.wildcatter.block.AluminaPartBlock.Part.MUD_WASHER);
	Block RESIDUE_FILTER_PRESS = aluminaPart(dev.wildcatter.block.AluminaPartBlock.Part.RESIDUE_FILTER_PRESS);
	Block POLISHING_FILTER = aluminaPart(dev.wildcatter.block.AluminaPartBlock.Part.POLISHING_FILTER);
	Block PRECIPITATOR = aluminaPart(dev.wildcatter.block.AluminaPartBlock.Part.PRECIPITATOR);
	Block HYDRATE_FILTER = aluminaPart(dev.wildcatter.block.AluminaPartBlock.Part.HYDRATE_FILTER);
	Block CALCINER = aluminaPart(dev.wildcatter.block.AluminaPartBlock.Part.CALCINER);
	Block EVAPORATOR = aluminaPart(dev.wildcatter.block.AluminaPartBlock.Part.EVAPORATOR);
	Block CAUSTICIZER = aluminaPart(dev.wildcatter.block.AluminaPartBlock.Part.CAUSTICIZER);
	Block PACKAGE_BOILER = aluminaPart(dev.wildcatter.block.AluminaPartBlock.Part.PACKAGE_BOILER);
	Block LIME_KILN = aluminaPart(dev.wildcatter.block.AluminaPartBlock.Part.LIME_KILN);
	Block ALF3_REACTOR = aluminaPart(dev.wildcatter.block.AluminaPartBlock.Part.ALF3_REACTOR);
	Block BAGGING_STATION = aluminaPart(dev.wildcatter.block.AluminaPartBlock.Part.BAGGING_STATION);
	Block CHLORINE_FILLER = aluminaPart(dev.wildcatter.block.AluminaPartBlock.Part.CHLORINE_FILLER);
	// 10b3a: the delayed coker and calciner, the coke ovens and tar still, the anode plant (docs/multiblocks/delayed-coker.md, coke-ovens.md, anode-plant.md)
	Block COKER_FRACTIONATOR = carbonPart(dev.wildcatter.block.CarbonPartBlock.Part.COKER_FRACTIONATOR);
	Block COKER_HEATER = carbonPart(dev.wildcatter.block.CarbonPartBlock.Part.COKER_HEATER);
	Block COKE_DRUM = carbonPart(dev.wildcatter.block.CarbonPartBlock.Part.COKE_DRUM);
	Block COKE_PIT = carbonPart(dev.wildcatter.block.CarbonPartBlock.Part.COKE_PIT);
	Block COKE_KILN = carbonPart(dev.wildcatter.block.CarbonPartBlock.Part.COKE_KILN);
	Block COKE_COOLER = carbonPart(dev.wildcatter.block.CarbonPartBlock.Part.COKE_COOLER);
	Block COAL_TOWER = carbonPart(dev.wildcatter.block.CarbonPartBlock.Part.COAL_TOWER);
	Block COKE_OVEN_BATTERY = carbonPart(dev.wildcatter.block.CarbonPartBlock.Part.COKE_OVEN_BATTERY);
	Block PUSHER_MACHINE = carbonPart(dev.wildcatter.block.CarbonPartBlock.Part.PUSHER_MACHINE);
	Block COKE_QUENCH_TOWER = carbonPart(dev.wildcatter.block.CarbonPartBlock.Part.COKE_QUENCH_TOWER);
	Block BYPRODUCT_PLANT = carbonPart(dev.wildcatter.block.CarbonPartBlock.Part.BYPRODUCT_PLANT);
	Block TAR_STILL = carbonPart(dev.wildcatter.block.CarbonPartBlock.Part.TAR_STILL);
	Block PASTE_PLANT = carbonPart(dev.wildcatter.block.CarbonPartBlock.Part.PASTE_PLANT);
	Block FORMING_PRESS = carbonPart(dev.wildcatter.block.CarbonPartBlock.Part.FORMING_PRESS);
	Block BAKING_FURNACE = carbonPart(dev.wildcatter.block.CarbonPartBlock.Part.BAKING_FURNACE);
	Block RODDING_SHOP = carbonPart(dev.wildcatter.block.CarbonPartBlock.Part.RODDING_SHOP);
	// 10b3b: the potline (docs/multiblocks/potline.md)
	Block RECTIFORMER = potlinePart(dev.wildcatter.block.PotlinePartBlock.Part.RECTIFORMER);
	Block POT = potlinePart(dev.wildcatter.block.PotlinePartBlock.Part.POT);
	Block FUME_TREATMENT_CENTRE = potlinePart(dev.wildcatter.block.PotlinePartBlock.Part.FUME_TREATMENT_CENTRE);
	Block FTC_FAN = potlinePart(dev.wildcatter.block.PotlinePartBlock.Part.FTC_FAN);
	Block CASTING_MACHINE = potlinePart(dev.wildcatter.block.PotlinePartBlock.Part.CASTING_MACHINE);
	// The ammonia plant (docs/multiblocks/ammonia-plant.md)
	Block NITROGEN_GENERATOR = ammoniaPart(dev.wildcatter.block.AmmoniaPartBlock.Part.NITROGEN_GENERATOR);
	Block SYNGAS_COMPRESSOR = ammoniaPart(dev.wildcatter.block.AmmoniaPartBlock.Part.SYNGAS_COMPRESSOR);
	Block AMMONIA_CONVERTER = ammoniaPart(dev.wildcatter.block.AmmoniaPartBlock.Part.AMMONIA_CONVERTER);
	Block NITRIC_ACID_UNIT = ammoniaPart(dev.wildcatter.block.AmmoniaPartBlock.Part.NITRIC_ACID_UNIT);
	Block AN_NEUTRALISER = ammoniaPart(dev.wildcatter.block.AmmoniaPartBlock.Part.AN_NEUTRALISER);
	Block EMULSION_BLENDER = ammoniaPart(dev.wildcatter.block.AmmoniaPartBlock.Part.EMULSION_BLENDER);
	// Phase 17a: ferroalloys (docs/multiblocks/ferroalloys.md)
	Block FURNACE_TRANSFORMER = ferroalloyPart(dev.wildcatter.block.FerroalloyPartBlock.Part.FURNACE_TRANSFORMER);
	Block SUBMERGED_ARC_FURNACE = ferroalloyPart(dev.wildcatter.block.FerroalloyPartBlock.Part.SUBMERGED_ARC_FURNACE);
	Block CAPACITOR_BANK = ferroalloyPart(dev.wildcatter.block.FerroalloyPartBlock.Part.CAPACITOR_BANK);
	Block MOLY_ROASTER = ferroalloyPart(dev.wildcatter.block.FerroalloyPartBlock.Part.MOLY_ROASTER);
	Block ALUMINOTHERMIC_VESSEL = ferroalloyPart(dev.wildcatter.block.FerroalloyPartBlock.Part.ALUMINOTHERMIC_VESSEL);
	// 10c2: the cranes (docs/multiblocks/tower-crane.md, crawler-crane.md)
	Block TOWER_CRANE = cranePart(dev.wildcatter.block.CranePartBlock.Part.TOWER_CRANE);
	Block CRAWLER_CRANE = cranePart(dev.wildcatter.block.CranePartBlock.Part.CRAWLER_CRANE);
	Block RING_CRANE = cranePart(dev.wildcatter.block.CranePartBlock.Part.RING_CRANE);
	/** The ring crane's track (docs/multiblocks/ring-crane.md, "Relocation"). */
	Block CRANE_RAIL = block("crane_rail", props -> new dev.wildcatter.block.CraneRailBlock(props.mapColor(MapColor.METAL).strength(5.0F, 6.0F)
			.sound(SoundType.METAL).requiresCorrectToolForDrops()));
	/** A blueprint's site (docs/blueprints.md). */
	Block SITE_MARKER = block("site_marker", props -> new dev.wildcatter.block.SiteMarkerBlock(props.mapColor(MapColor.COLOR_ORANGE)
			.strength(0.5F).sound(SoundType.WOOD).noOcclusion()));
	/** Nine aluminium ingots. */
	Block ALUMINIUM_BLOCK = block("aluminium_block", props -> new Block(props.mapColor(MapColor.METAL).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	/** A placed block of red mud filter cake, for a dry stack. */
	Block RED_MUD = block("red_mud", props -> new Block(props.mapColor(MapColor.TERRACOTTA_RED).strength(0.8F).sound(SoundType.MUD)));
	Block ISR_WELLHEAD = block("isr_wellhead", props -> new dev.wildcatter.block.IsrWellheadBlock(props.mapColor(MapColor.COLOR_LIGHT_GRAY)
			.strength(3.0F, 6.0F).sound(SoundType.METAL)));
}
