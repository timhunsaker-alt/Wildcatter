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
	Block JAW_CRUSHER = millPart(dev.wildcatter.block.MillPartBlock.Part.JAW_CRUSHER);
	Block SAG_MILL = millPart(dev.wildcatter.block.MillPartBlock.Part.SAG_MILL);
	Block LEACH_TANK = millPart(dev.wildcatter.block.MillPartBlock.Part.LEACH_TANK);
	Block CCD_THICKENER = millPart(dev.wildcatter.block.MillPartBlock.Part.CCD_THICKENER);
	Block SX_CIRCUIT = millPart(dev.wildcatter.block.MillPartBlock.Part.SX_CIRCUIT);
	Block PRECIPITATION_TANK = millPart(dev.wildcatter.block.MillPartBlock.Part.PRECIPITATION_TANK);
	Block YELLOWCAKE_DRYER = millPart(dev.wildcatter.block.MillPartBlock.Part.YELLOWCAKE_DRYER);
	Block DRUM_FILLER = millPart(dev.wildcatter.block.MillPartBlock.Part.DRUM_FILLER);
	// 7c: the ISR wellfield (docs/multiblocks/isr-wellfield.md)
	Block HEADER_HOUSE = wellfieldPart(dev.wildcatter.block.WellfieldPartBlock.Part.HEADER_HOUSE);
	Block OXYGEN_GENERATOR = wellfieldPart(dev.wildcatter.block.WellfieldPartBlock.Part.OXYGEN_GENERATOR);
	Block IX_COLUMNS = wellfieldPart(dev.wildcatter.block.WellfieldPartBlock.Part.IX_COLUMNS);
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
	// 7e: the centrifuge cascade (docs/multiblocks/enrichment.md)
	Block CENTRIFUGE = enrichmentPart(dev.wildcatter.block.EnrichmentPartBlock.Part.CENTRIFUGE);
	Block FREQUENCY_CONVERTER = enrichmentPart(dev.wildcatter.block.EnrichmentPartBlock.Part.FREQUENCY_CONVERTER);
	Block FEED_STATION = enrichmentPart(dev.wildcatter.block.EnrichmentPartBlock.Part.FEED_STATION);
	Block PRODUCT_TAKEOFF = enrichmentPart(dev.wildcatter.block.EnrichmentPartBlock.Part.PRODUCT_TAKEOFF);
	Block TAILS_TAKEOFF = enrichmentPart(dev.wildcatter.block.EnrichmentPartBlock.Part.TAILS_TAKEOFF);
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
	/** A placed block of red mud filter cake, for a dry stack. */
	Block RED_MUD = block("red_mud", props -> new Block(props.mapColor(MapColor.TERRACOTTA_RED).strength(0.8F).sound(SoundType.MUD)));
	Block ISR_WELLHEAD = block("isr_wellhead", props -> new dev.wildcatter.block.IsrWellheadBlock(props.mapColor(MapColor.COLOR_LIGHT_GRAY)
			.strength(3.0F, 6.0F).sound(SoundType.METAL)));
}
