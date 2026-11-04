package dev.wildcatter;

import static dev.wildcatter.BlockFactory.entity;

import dev.wildcatter.block.BoilerBlockEntity;
import dev.wildcatter.block.PlugAdapterBlockEntity;
import dev.wildcatter.block.ReceptacleBlockEntity;
import dev.wildcatter.energy.PlugType;
import dev.wildcatter.block.InsulatorBlockEntity;
import dev.wildcatter.block.FuseBlockEntity;
import dev.wildcatter.block.BreakerBlockEntity;
import dev.wildcatter.block.CableBlock;
import dev.wildcatter.block.CoolingTowerBlockEntity;
import dev.wildcatter.block.DrillingRigBlockEntity;
import dev.wildcatter.block.GasWellheadBlockEntity;
import dev.wildcatter.block.PumpjackBlockEntity;
import dev.wildcatter.block.LoadBlockEntity;
import dev.wildcatter.block.WireBlockEntity;
import dev.wildcatter.block.FireboxBlockEntity;
import dev.wildcatter.block.PumpBlockEntity;
import dev.wildcatter.block.TransformerBlockEntity;
import dev.wildcatter.block.MultiCableBlock;
import dev.wildcatter.block.MultiCableBlockEntity;
import dev.wildcatter.block.MotorStarterBlockEntity;
import dev.wildcatter.block.MccIncomingBlockEntity;
import dev.wildcatter.block.ThreePhaseHeaterBlockEntity;
import dev.wildcatter.block.SteamTurbineBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import dev.wildcatter.energy.ConductorSize;
import dev.wildcatter.item.ConductorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Every block, its item, and its block entity. The blocks themselves live in one list per system. */
public final class WildcatterBlocks implements FuelCycleBlocks, GridBlocks, NuclearBlocks, OilGasBlocks, PowerPlantBlocks {
	public static final BlockEntityType<BoilerBlockEntity> BOILER_ENTITY = entity(BoilerBlockEntity::new, BOILER);
	public static final BlockEntityType<SteamTurbineBlockEntity> STEAM_TURBINE_ENTITY = entity(SteamTurbineBlockEntity::new, STEAM_TURBINE);
	public static final BlockEntityType<WireBlockEntity> WIRE_ENTITY = entity(WireBlockEntity::new, DIST_WIRE_HOT, DIST_WIRE_NEUTRAL, DIST_PLASTIC_WIRE_HOT, DIST_PLASTIC_WIRE_NEUTRAL, WIRE_HOT, WIRE_NEUTRAL, WOOL_WIRE_HOT,
			WOOL_WIRE_NEUTRAL, MV_WIRE_HOT, MV_WIRE_NEUTRAL, HV_WIRE_HOT, HV_WIRE_NEUTRAL,
			PLASTIC_WIRE_HOT, PLASTIC_WIRE_NEUTRAL, MV_PLASTIC_WIRE_HOT, MV_PLASTIC_WIRE_NEUTRAL, HV_PLASTIC_WIRE_HOT, HV_PLASTIC_WIRE_NEUTRAL,
			INDUSTRIAL_WIRE_HOT, INDUSTRIAL_WIRE_NEUTRAL, INDUSTRIAL_PLASTIC_WIRE_HOT, INDUSTRIAL_PLASTIC_WIRE_NEUTRAL);
	public static final BlockEntityType<MultiCableBlockEntity> MULTI_CABLE_ENTITY = entity(MultiCableBlockEntity::new, CABLE_LV, CABLE_INDUSTRIAL, CABLE_MV, CABLE_DIST, CABLE_HV, BUSWAY_LV, BUSWAY_INDUSTRIAL, BUS_DUCT_GEN);
	public static final BlockEntityType<MotorStarterBlockEntity> MOTOR_STARTER_ENTITY = entity(MotorStarterBlockEntity::new, MOTOR_STARTER, MCC_BUCKET);
	public static final BlockEntityType<MccIncomingBlockEntity> MCC_INCOMING_ENTITY = entity(MccIncomingBlockEntity::new, MCC_INCOMING);
	public static final BlockEntityType<dev.wildcatter.block.MccSectionBlockEntity> MCC_SECTION_ENTITY = entity(dev.wildcatter.block.MccSectionBlockEntity::new, MCC_SECTION);
	public static final BlockEntityType<dev.wildcatter.block.StarterCabinetBlockEntity> STARTER_CABINET_ENTITY = entity(dev.wildcatter.block.StarterCabinetBlockEntity::new,
			SOFT_STARTER, DRIVE_CABINET, SOFT_STARTER_MV, DRIVE_MV_CONTROL);
	public static final BlockEntityType<dev.wildcatter.block.DcBusBarBlockEntity> DC_BUS_ENTITY = entity(dev.wildcatter.block.DcBusBarBlockEntity::new, DC_BUS_4K, DC_BUS_16K, DC_BUS_180K, DC_BUS_180K_COPPER);
	public static final BlockEntityType<dev.wildcatter.block.DriveSectionBlock.Entity> DRIVE_SECTION_ENTITY = entity(dev.wildcatter.block.DriveSectionBlock.Entity::new,
			DRIVE_MV_TRANSFORMER, DRIVE_MV_CELLS);
	public static final BlockEntityType<dev.wildcatter.block.GensetPartBlockEntity> GENSET_PART_ENTITY = entity(dev.wildcatter.block.GensetPartBlockEntity::new,
			DIESEL_ENGINE_PART, GAS_ENGINE_PART, ALTERNATOR, RADIATOR, FUEL_TANK, EXHAUST_STACK, BASE_FRAME, EXHAUST_SILENCER_LARGE);
	public static final BlockEntityType<dev.wildcatter.block.SteamPlantPartBlockEntity> STEAM_PLANT_PART_ENTITY = entity(dev.wildcatter.block.SteamPlantPartBlockEntity::new,
			BOILER_BURNER, BOILER_DRUM, SUPERHEATER, TURBINE_CASING, CONDENSER, FEED_PUMP);
	public static final BlockEntityType<dev.wildcatter.block.RigSubstructureBlockEntity> RIG_SUBSTRUCTURE_ENTITY = entity(dev.wildcatter.block.RigSubstructureBlockEntity::new, RIG_SUBSTRUCTURE);
	public static final BlockEntityType<dev.wildcatter.block.RigDriveHouseBlockEntity> RIG_DRIVE_HOUSE_ENTITY = entity(dev.wildcatter.block.RigDriveHouseBlockEntity::new, RIG_DRIVE_HOUSE);
	public static final BlockEntityType<dev.wildcatter.block.RigPartBlockEntity> RIG_PART_ENTITY = entity(dev.wildcatter.block.RigPartBlockEntity::new,
			RIG_MAST, RIG_DRAWWORKS, MUD_PUMP, MUD_TANKS);
	public static final BlockEntityType<dev.wildcatter.block.SeparatorBlockEntity> SEPARATOR_ENTITY = entity(dev.wildcatter.block.SeparatorBlockEntity::new, SEPARATOR);
	public static final BlockEntityType<dev.wildcatter.block.StorageTankBlockEntity> STORAGE_TANK_ENTITY = entity(dev.wildcatter.block.StorageTankBlockEntity::new, STOCK_TANK, WATER_TANK, LPG_BULLET);
	public static final BlockEntityType<dev.wildcatter.block.GasCompressorBlockEntity> GAS_COMPRESSOR_ENTITY = entity(dev.wildcatter.block.GasCompressorBlockEntity::new, GAS_COMPRESSOR, REFRIGERATION_COMPRESSOR, CRACKED_GAS_COMPRESSOR);
	public static final BlockEntityType<dev.wildcatter.block.UnitMotorBlockEntity> UNIT_MOTOR_ENTITY = entity(dev.wildcatter.block.UnitMotorBlockEntity::new, TOPPING_SKID, DESALTER, CRUDE_CHARGE_PUMP, OVERHEAD_CONDENSER, AIR_COOLED_CONDENSER, HRSG_FEED_PUMP, MAIN_BLOWER, CRDM_HEAD, PRESSURIZER, CONDENSATE_PUMP, MAIN_FEED_PUMP, RHR_PUMP, PUSHER_MACHINE, FTC_FAN);
	public static final BlockEntityType<dev.wildcatter.block.GasTurbineBlockEntity> GAS_TURBINE_ENTITY = entity(dev.wildcatter.block.GasTurbineBlockEntity::new, GAS_TURBINE, AERO_TURBINE);
	public static final BlockEntityType<dev.wildcatter.block.GeneratorPartBlockEntity> GENERATOR_PART_ENTITY = entity(dev.wildcatter.block.GeneratorPartBlockEntity::new, GT_GENERATOR, ST_GENERATOR, AERO_GENERATOR, TG_GENERATOR_60);
	public static final BlockEntityType<dev.wildcatter.block.GenBreakerBlockEntity> GEN_BREAKER_ENTITY = entity(dev.wildcatter.block.GenBreakerBlockEntity::new, GEN_BREAKER, GEN_BREAKER_3000);
	public static final BlockEntityType<dev.wildcatter.block.HrsgBlockEntity> HRSG_ENTITY = entity(dev.wildcatter.block.HrsgBlockEntity::new, HRSG);
	public static final BlockEntityType<dev.wildcatter.block.SteamCycleBlockEntity> STEAM_CYCLE_ENTITY = entity(dev.wildcatter.block.SteamCycleBlockEntity::new, STEAM_TURBINE_5);
	public static final BlockEntityType<dev.wildcatter.block.LoadBankBlockEntity> LOAD_BANK_ENTITY = entity(dev.wildcatter.block.LoadBankBlockEntity::new, LOAD_BANK);
	public static final BlockEntityType<dev.wildcatter.block.TurbinePassiveBlockEntity> TURBINE_PASSIVE_ENTITY = entity(dev.wildcatter.block.TurbinePassiveBlockEntity::new, GT_INLET_HOUSE, GT_EXHAUST_STACK, AERO_INLET_HOUSE, AERO_EXHAUST_STACK);
	public static final BlockEntityType<dev.wildcatter.block.FiredHeaterBlockEntity> FIRED_HEATER_ENTITY = entity(dev.wildcatter.block.FiredHeaterBlockEntity::new, TOPPING_HEATER, CRUDE_HEATER);
	public static final BlockEntityType<dev.wildcatter.block.StripperBlockEntity> STRIPPER_ENTITY = entity(dev.wildcatter.block.StripperBlockEntity::new, SIDE_STRIPPERS);
	public static final BlockEntityType<dev.wildcatter.block.ToppingColumnBlockEntity> TOPPING_COLUMN_ENTITY = entity(dev.wildcatter.block.ToppingColumnBlockEntity::new, TOPPING_COLUMN);
	public static final BlockEntityType<dev.wildcatter.block.CrudeColumnBlockEntity> CRUDE_COLUMN_ENTITY = entity(dev.wildcatter.block.CrudeColumnBlockEntity::new, ATMOSPHERIC_COLUMN);
	public static final BlockEntityType<dev.wildcatter.block.CrackingFurnaceBlockEntity> CRACKING_FURNACE_ENTITY = entity(dev.wildcatter.block.CrackingFurnaceBlockEntity::new, CRACKING_FURNACE);
	public static final BlockEntityType<dev.wildcatter.block.QuenchTowerBlockEntity> QUENCH_TOWER_ENTITY = entity(dev.wildcatter.block.QuenchTowerBlockEntity::new, QUENCH_TOWER);
	public static final BlockEntityType<dev.wildcatter.block.ColdBoxBlockEntity> COLD_BOX_ENTITY = entity(dev.wildcatter.block.ColdBoxBlockEntity::new, COLD_BOX);
	public static final BlockEntityType<dev.wildcatter.block.PolyethyleneReactorBlockEntity> PE_REACTOR_ENTITY = entity(dev.wildcatter.block.PolyethyleneReactorBlockEntity::new, POLYETHYLENE_REACTOR);
	public static final BlockEntityType<dev.wildcatter.block.PelletizerBlockEntity> PELLETIZER_ENTITY = entity(dev.wildcatter.block.PelletizerBlockEntity::new, PELLETIZER);
	public static final BlockEntityType<dev.wildcatter.block.SulfurMelterBlockEntity> SULFUR_MELTER_ENTITY = entity(dev.wildcatter.block.SulfurMelterBlockEntity::new, SULFUR_MELTER);
	public static final BlockEntityType<dev.wildcatter.block.PassivePartBlockEntity> ACID_PASSIVE_ENTITY = entity(dev.wildcatter.block.PassivePartBlockEntity::new, DRYING_TOWER, INTERPASS_ABSORBER);
	public static final BlockEntityType<dev.wildcatter.block.SulfurFurnaceBlockEntity> SULFUR_FURNACE_ENTITY = entity(dev.wildcatter.block.SulfurFurnaceBlockEntity::new, SULFUR_FURNACE);
	public static final BlockEntityType<dev.wildcatter.block.WasteHeatBoilerBlockEntity> WASTE_HEAT_BOILER_ENTITY = entity(dev.wildcatter.block.WasteHeatBoilerBlockEntity::new, WASTE_HEAT_BOILER);
	public static final BlockEntityType<dev.wildcatter.block.ConverterBlockEntity> CONVERTER_ENTITY = entity(dev.wildcatter.block.ConverterBlockEntity::new, CONVERTER);
	public static final BlockEntityType<dev.wildcatter.block.FinalAbsorberBlockEntity> FINAL_ABSORBER_ENTITY = entity(dev.wildcatter.block.FinalAbsorberBlockEntity::new, FINAL_ABSORBER);
	public static final BlockEntityType<dev.wildcatter.block.JawCrusherBlockEntity> JAW_CRUSHER_ENTITY = entity(dev.wildcatter.block.JawCrusherBlockEntity::new, JAW_CRUSHER);
	public static final BlockEntityType<dev.wildcatter.block.SagMillBlockEntity> SAG_MILL_ENTITY = entity(dev.wildcatter.block.SagMillBlockEntity::new, SAG_MILL);
	public static final BlockEntityType<dev.wildcatter.block.LeachTankBlockEntity> LEACH_TANK_ENTITY = entity(dev.wildcatter.block.LeachTankBlockEntity::new, LEACH_TANK);
	public static final BlockEntityType<dev.wildcatter.block.CcdThickenerBlockEntity> CCD_THICKENER_ENTITY = entity(dev.wildcatter.block.CcdThickenerBlockEntity::new, CCD_THICKENER);
	public static final BlockEntityType<dev.wildcatter.block.SxCircuitBlockEntity> SX_CIRCUIT_ENTITY = entity(dev.wildcatter.block.SxCircuitBlockEntity::new, SX_CIRCUIT);
	public static final BlockEntityType<dev.wildcatter.block.PrecipitationTankBlockEntity> PRECIPITATION_TANK_ENTITY = entity(dev.wildcatter.block.PrecipitationTankBlockEntity::new, PRECIPITATION_TANK);
	public static final BlockEntityType<dev.wildcatter.block.YellowcakeDryerBlockEntity> YELLOWCAKE_DRYER_ENTITY = entity(dev.wildcatter.block.YellowcakeDryerBlockEntity::new, YELLOWCAKE_DRYER);
	public static final BlockEntityType<dev.wildcatter.block.DrumFillerBlockEntity> DRUM_FILLER_ENTITY = entity(dev.wildcatter.block.DrumFillerBlockEntity::new, DRUM_FILLER);
	public static final BlockEntityType<dev.wildcatter.block.HeaderHouseBlockEntity> HEADER_HOUSE_ENTITY = entity(dev.wildcatter.block.HeaderHouseBlockEntity::new, HEADER_HOUSE);
	public static final BlockEntityType<dev.wildcatter.block.OxygenGeneratorBlockEntity> OXYGEN_GENERATOR_ENTITY = entity(dev.wildcatter.block.OxygenGeneratorBlockEntity::new, OXYGEN_GENERATOR);
	public static final BlockEntityType<dev.wildcatter.block.IxColumnsBlockEntity> IX_COLUMNS_ENTITY = entity(dev.wildcatter.block.IxColumnsBlockEntity::new, IX_COLUMNS);
	public static final BlockEntityType<dev.wildcatter.block.IsrWellheadBlockEntity> ISR_WELLHEAD_ENTITY = entity(dev.wildcatter.block.IsrWellheadBlockEntity::new, ISR_WELLHEAD);
	public static final BlockEntityType<dev.wildcatter.block.ReductionReactorBlockEntity> REDUCTION_REACTOR_ENTITY = entity(dev.wildcatter.block.ReductionReactorBlockEntity::new, REDUCTION_REACTOR);
	public static final BlockEntityType<dev.wildcatter.block.ConversionStageBlockEntity> CONVERSION_STAGE_ENTITY = entity((pos, state) -> new dev.wildcatter.block.ConversionStageBlockEntity(WildcatterBlocks.CONVERSION_STAGE_ENTITY, pos, state), HYDROFLUORINATOR, FLUORINATOR);
	public static final BlockEntityType<dev.wildcatter.block.Uf6StageBlockEntity> UF6_STAGE_ENTITY = entity((pos, state) -> new dev.wildcatter.block.Uf6StageBlockEntity(WildcatterBlocks.UF6_STAGE_ENTITY, pos, state), COLD_TRAPS, DISTILLATION_COLUMNS);
	public static final BlockEntityType<dev.wildcatter.block.CylinderFillerBlockEntity> CYLINDER_FILLER_ENTITY = entity(dev.wildcatter.block.CylinderFillerBlockEntity::new, CYLINDER_FILLER);
	public static final BlockEntityType<dev.wildcatter.block.SteamReformerBlockEntity> STEAM_REFORMER_ENTITY = entity(dev.wildcatter.block.SteamReformerBlockEntity::new, STEAM_REFORMER);
	public static final BlockEntityType<dev.wildcatter.block.HfKilnBlockEntity> HF_KILN_ENTITY = entity(dev.wildcatter.block.HfKilnBlockEntity::new, HF_KILN);
	public static final BlockEntityType<dev.wildcatter.block.HfColumnBlockEntity> HF_COLUMN_ENTITY = entity(dev.wildcatter.block.HfColumnBlockEntity::new, HF_COLUMN);
	public static final BlockEntityType<dev.wildcatter.block.RectifierBlockEntity> RECTIFIER_ENTITY = entity(dev.wildcatter.block.RectifierBlockEntity::new, RECTIFIER);
	public static final BlockEntityType<dev.wildcatter.block.FluorineCellHouseBlockEntity> FLUORINE_CELL_HOUSE_ENTITY = entity(dev.wildcatter.block.FluorineCellHouseBlockEntity::new, FLUORINE_CELL_HOUSE);
	public static final BlockEntityType<dev.wildcatter.block.CentrifugeBlockEntity> CENTRIFUGE_ENTITY = entity(dev.wildcatter.block.CentrifugeBlockEntity::new, CENTRIFUGE);
	public static final BlockEntityType<dev.wildcatter.block.FrequencyConverterBlockEntity> FREQUENCY_CONVERTER_ENTITY = entity(dev.wildcatter.block.FrequencyConverterBlockEntity::new, FREQUENCY_CONVERTER);
	public static final BlockEntityType<dev.wildcatter.block.FeedStationBlockEntity> FEED_STATION_ENTITY = entity(dev.wildcatter.block.FeedStationBlockEntity::new, FEED_STATION);
	public static final BlockEntityType<dev.wildcatter.block.TakeoffBlockEntity> TAKEOFF_ENTITY = entity(dev.wildcatter.block.TakeoffBlockEntity::new, PRODUCT_TAKEOFF, TAILS_TAKEOFF);
	public static final BlockEntityType<dev.wildcatter.block.ZrLineBlockEntity> ZR_LINE_ENTITY = entity(dev.wildcatter.block.ZrLineBlockEntity::new, MINERAL_SEPARATOR, HAFNIUM_COLUMN, VACUUM_DISTILLATION, VAR_FURNACE, EXTRUSION_PRESS, VACUUM_ANNEAL_FURNACE);
	public static final BlockEntityType<dev.wildcatter.block.ChlorinatorBlockEntity> CHLORINATOR_ENTITY = entity(dev.wildcatter.block.ChlorinatorBlockEntity::new, CHLORINATOR);
	public static final BlockEntityType<dev.wildcatter.block.KrollReactorBlockEntity> KROLL_REACTOR_ENTITY = entity(dev.wildcatter.block.KrollReactorBlockEntity::new, KROLL_REACTOR);
	public static final BlockEntityType<dev.wildcatter.block.PilgerMillBlockEntity> PILGER_MILL_ENTITY = entity(dev.wildcatter.block.PilgerMillBlockEntity::new, PILGER_MILL);
	public static final BlockEntityType<dev.wildcatter.block.MagnesiumCellBlockEntity> MAGNESIUM_CELL_ENTITY = entity(dev.wildcatter.block.MagnesiumCellBlockEntity::new, MAGNESIUM_CELL);
	public static final BlockEntityType<dev.wildcatter.block.MagnesiaChlorinatorBlockEntity> MAGNESIA_CHLORINATOR_ENTITY = entity(dev.wildcatter.block.MagnesiaChlorinatorBlockEntity::new, MAGNESIA_CHLORINATOR);
	public static final BlockEntityType<dev.wildcatter.block.ChlorAlkaliCellBlockEntity> CHLOR_ALKALI_CELL_ENTITY = entity(dev.wildcatter.block.ChlorAlkaliCellBlockEntity::new, CHLOR_ALKALI_CELL);
	public static final BlockEntityType<dev.wildcatter.block.BayerStageBlockEntity> BAYER_STAGE_ENTITY = entity(dev.wildcatter.block.BayerStageBlockEntity::new, BALL_MILL, DIGESTER, FLASH_TANKS, SETTLER, MUD_WASHER, POLISHING_FILTER, PRECIPITATOR, EVAPORATOR, PACKAGE_BOILER);
	public static final BlockEntityType<dev.wildcatter.block.ResidueFilterPressBlockEntity> RESIDUE_FILTER_PRESS_ENTITY = entity(dev.wildcatter.block.ResidueFilterPressBlockEntity::new, RESIDUE_FILTER_PRESS);
	public static final BlockEntityType<dev.wildcatter.block.HydrateFilterBlockEntity> HYDRATE_FILTER_ENTITY = entity(dev.wildcatter.block.HydrateFilterBlockEntity::new, HYDRATE_FILTER);
	public static final BlockEntityType<dev.wildcatter.block.CalcinerBlockEntity> CALCINER_ENTITY = entity(dev.wildcatter.block.CalcinerBlockEntity::new, CALCINER);
	public static final BlockEntityType<dev.wildcatter.block.CausticizerBlockEntity> CAUSTICIZER_ENTITY = entity(dev.wildcatter.block.CausticizerBlockEntity::new, CAUSTICIZER);
	public static final BlockEntityType<dev.wildcatter.block.LimeKilnBlockEntity> LIME_KILN_ENTITY = entity(dev.wildcatter.block.LimeKilnBlockEntity::new, LIME_KILN);
	public static final BlockEntityType<dev.wildcatter.block.Alf3ReactorBlockEntity> ALF3_REACTOR_ENTITY = entity(dev.wildcatter.block.Alf3ReactorBlockEntity::new, ALF3_REACTOR);
	public static final BlockEntityType<dev.wildcatter.block.BaggingStationBlockEntity> BAGGING_STATION_ENTITY = entity(dev.wildcatter.block.BaggingStationBlockEntity::new, BAGGING_STATION);
	public static final BlockEntityType<dev.wildcatter.block.ChlorineFillerBlockEntity> CHLORINE_FILLER_ENTITY = entity(dev.wildcatter.block.ChlorineFillerBlockEntity::new, CHLORINE_FILLER);
	public static final BlockEntityType<dev.wildcatter.block.CokerFractionatorBlockEntity> COKER_FRACTIONATOR_ENTITY = entity(dev.wildcatter.block.CokerFractionatorBlockEntity::new, COKER_FRACTIONATOR);
	public static final BlockEntityType<dev.wildcatter.block.CokerHeaterBlockEntity> COKER_HEATER_ENTITY = entity(dev.wildcatter.block.CokerHeaterBlockEntity::new, COKER_HEATER);
	public static final BlockEntityType<dev.wildcatter.block.CokeDrumBlockEntity> COKE_DRUM_ENTITY = entity(dev.wildcatter.block.CokeDrumBlockEntity::new, COKE_DRUM);
	public static final BlockEntityType<dev.wildcatter.block.CokePitBlockEntity> COKE_PIT_ENTITY = entity(dev.wildcatter.block.CokePitBlockEntity::new, COKE_PIT);
	public static final BlockEntityType<dev.wildcatter.block.CokeKilnBlockEntity> COKE_KILN_ENTITY = entity(dev.wildcatter.block.CokeKilnBlockEntity::new, COKE_KILN);
	public static final BlockEntityType<dev.wildcatter.block.CokeCoolerBlockEntity> COKE_COOLER_ENTITY = entity(dev.wildcatter.block.CokeCoolerBlockEntity::new, COKE_COOLER);
	public static final BlockEntityType<dev.wildcatter.block.CoalTowerBlockEntity> COAL_TOWER_ENTITY = entity(dev.wildcatter.block.CoalTowerBlockEntity::new, COAL_TOWER);
	public static final BlockEntityType<dev.wildcatter.block.CokeOvenBatteryBlockEntity> COKE_OVEN_BATTERY_ENTITY = entity(dev.wildcatter.block.CokeOvenBatteryBlockEntity::new, COKE_OVEN_BATTERY);
	public static final BlockEntityType<dev.wildcatter.block.CokeQuenchTowerBlockEntity> COKE_QUENCH_TOWER_ENTITY = entity(dev.wildcatter.block.CokeQuenchTowerBlockEntity::new, COKE_QUENCH_TOWER);
	public static final BlockEntityType<dev.wildcatter.block.ByproductPlantBlockEntity> BYPRODUCT_PLANT_ENTITY = entity(dev.wildcatter.block.ByproductPlantBlockEntity::new, BYPRODUCT_PLANT);
	public static final BlockEntityType<dev.wildcatter.block.TarStillBlockEntity> TAR_STILL_ENTITY = entity(dev.wildcatter.block.TarStillBlockEntity::new, TAR_STILL);
	public static final BlockEntityType<dev.wildcatter.block.PastePlantBlockEntity> PASTE_PLANT_ENTITY = entity(dev.wildcatter.block.PastePlantBlockEntity::new, PASTE_PLANT);
	public static final BlockEntityType<dev.wildcatter.block.FormingPressBlockEntity> FORMING_PRESS_ENTITY = entity(dev.wildcatter.block.FormingPressBlockEntity::new, FORMING_PRESS);
	public static final BlockEntityType<dev.wildcatter.block.BakingFurnaceBlockEntity> BAKING_FURNACE_ENTITY = entity(dev.wildcatter.block.BakingFurnaceBlockEntity::new, BAKING_FURNACE);
	public static final BlockEntityType<dev.wildcatter.block.RectiformerBlockEntity> RECTIFORMER_ENTITY = entity(dev.wildcatter.block.RectiformerBlockEntity::new, RECTIFORMER);
	public static final BlockEntityType<dev.wildcatter.block.PotBlockEntity> POT_ENTITY = entity(dev.wildcatter.block.PotBlockEntity::new, POT);
	public static final BlockEntityType<dev.wildcatter.block.FumeTreatmentCentreBlockEntity> FUME_TREATMENT_CENTRE_ENTITY = entity(dev.wildcatter.block.FumeTreatmentCentreBlockEntity::new, FUME_TREATMENT_CENTRE);
	public static final BlockEntityType<dev.wildcatter.block.CastingMachineBlockEntity> CASTING_MACHINE_ENTITY = entity(dev.wildcatter.block.CastingMachineBlockEntity::new, CASTING_MACHINE);
	public static final BlockEntityType<dev.wildcatter.block.CraneBlockEntity> CRANE_ENTITY = entity(dev.wildcatter.block.CraneBlockEntity::new, TOWER_CRANE, CRAWLER_CRANE);
	public static final BlockEntityType<dev.wildcatter.block.SiteMarkerBlockEntity> SITE_MARKER_ENTITY = entity(dev.wildcatter.block.SiteMarkerBlockEntity::new, SITE_MARKER);
	public static final BlockEntityType<dev.wildcatter.block.RoddingShopBlockEntity> RODDING_SHOP_ENTITY = entity(dev.wildcatter.block.RoddingShopBlockEntity::new, RODDING_SHOP);
	public static final BlockEntityType<dev.wildcatter.block.FuelLineBlockEntity> FUEL_LINE_ENTITY = entity(dev.wildcatter.block.FuelLineBlockEntity::new, UF6_AUTOCLAVE, IDR_KILN, GRANULATOR, PELLET_PRESS, SINTERING_FURNACE, PELLET_GRINDER, ROD_LOADER);
	public static final BlockEntityType<dev.wildcatter.block.RodWelderBlockEntity> ROD_WELDER_ENTITY = entity(dev.wildcatter.block.RodWelderBlockEntity::new, ROD_WELDER);
	public static final BlockEntityType<dev.wildcatter.block.AssemblyFixtureBlockEntity> ASSEMBLY_FIXTURE_ENTITY = entity(dev.wildcatter.block.AssemblyFixtureBlockEntity::new, ASSEMBLY_FIXTURE);
	public static final BlockEntityType<dev.wildcatter.block.HeliumRecoveryBlockEntity> HELIUM_RECOVERY_ENTITY = entity(dev.wildcatter.block.HeliumRecoveryBlockEntity::new, HELIUM_RECOVERY_UNIT);
	public static final BlockEntityType<dev.wildcatter.block.ReactorVesselBlockEntity> REACTOR_VESSEL_ENTITY = entity(dev.wildcatter.block.ReactorVesselBlockEntity::new, REACTOR_VESSEL);
	public static final BlockEntityType<dev.wildcatter.block.ReactorConsoleBlockEntity> REACTOR_CONSOLE_ENTITY = entity(dev.wildcatter.block.ReactorConsoleBlockEntity::new, REACTOR_CONSOLE);
	public static final BlockEntityType<dev.wildcatter.block.SteamGeneratorBlockEntity> STEAM_GENERATOR_ENTITY = entity(dev.wildcatter.block.SteamGeneratorBlockEntity::new, STEAM_GENERATOR);
	public static final BlockEntityType<dev.wildcatter.block.ReactorCoolantPumpBlockEntity> REACTOR_COOLANT_PUMP_ENTITY = entity(dev.wildcatter.block.ReactorCoolantPumpBlockEntity::new, REACTOR_COOLANT_PUMP);
	public static final BlockEntityType<dev.wildcatter.block.CvcsBlockEntity> CVCS_ENTITY = entity(dev.wildcatter.block.CvcsBlockEntity::new, CVCS_SKID);
	public static final BlockEntityType<dev.wildcatter.block.AuxFeedPumpBlockEntity> AUX_FEED_PUMP_ENTITY = entity(dev.wildcatter.block.AuxFeedPumpBlockEntity::new, AUX_FEED_PUMP);
	public static final BlockEntityType<dev.wildcatter.block.BoronLineBlockEntity> BORON_LINE_ENTITY = entity(dev.wildcatter.block.BoronLineBlockEntity::new, BORAX_DISSOLVER, BORIC_ACID_CRYSTALLIZER, B4C_FURNACE, RCCA_BENCH);
	public static final BlockEntityType<dev.wildcatter.block.NaturalDraftTowerBlockEntity> NATURAL_DRAFT_TOWER_ENTITY = entity(dev.wildcatter.block.NaturalDraftTowerBlockEntity::new, NATURAL_DRAFT_TOWER);
	public static final BlockEntityType<dev.wildcatter.block.RefuellingMachineBlockEntity> REFUELLING_MACHINE_ENTITY = entity(dev.wildcatter.block.RefuellingMachineBlockEntity::new, REFUELLING_MACHINE);
	public static final BlockEntityType<dev.wildcatter.block.SpentFuelPoolBlockEntity> SPENT_FUEL_POOL_ENTITY = entity(dev.wildcatter.block.SpentFuelPoolBlockEntity::new, SPENT_FUEL_POOL);
	public static final BlockEntityType<dev.wildcatter.block.RhrHeatExchangerBlockEntity> RHR_HX_ENTITY = entity(dev.wildcatter.block.RhrHeatExchangerBlockEntity::new, RHR_HEAT_EXCHANGER);
	public static final BlockEntityType<dev.wildcatter.block.HpTurbineBlockEntity> HP_TURBINE_ENTITY = entity(dev.wildcatter.block.HpTurbineBlockEntity::new, HP_TURBINE);
	public static final BlockEntityType<dev.wildcatter.block.CondenserBlockEntity> CONDENSER_ENTITY = entity(dev.wildcatter.block.CondenserBlockEntity::new, MAIN_CONDENSER);
	public static final BlockEntityType<dev.wildcatter.block.DeaeratorBlockEntity> DEAERATOR_ENTITY = entity(dev.wildcatter.block.DeaeratorBlockEntity::new, DEAERATOR);
	public static final BlockEntityType<dev.wildcatter.block.HpHeaterBlockEntity> HP_HEATER_ENTITY = entity(dev.wildcatter.block.HpHeaterBlockEntity::new, HP_FEEDWATER_HEATER);
	public static final BlockEntityType<dev.wildcatter.block.CirculatingWaterPumpBlockEntity> CW_PUMP_ENTITY = entity(dev.wildcatter.block.CirculatingWaterPumpBlockEntity::new, CIRCULATING_WATER_PUMP);
	public static final BlockEntityType<dev.wildcatter.block.DehydratorBlockEntity> DEHYDRATOR_ENTITY = entity(dev.wildcatter.block.DehydratorBlockEntity::new, TEG_DEHYDRATOR);
	public static final BlockEntityType<dev.wildcatter.block.ChillerBlockEntity> CHILLER_ENTITY = entity(dev.wildcatter.block.ChillerBlockEntity::new, NGL_CHILLER);
	public static final BlockEntityType<dev.wildcatter.block.StabilizerBlockEntity> STABILIZER_ENTITY = entity(dev.wildcatter.block.StabilizerBlockEntity::new, STABILIZER);
	public static final BlockEntityType<dev.wildcatter.block.FlareStackBlockEntity> FLARE_STACK_ENTITY = entity(dev.wildcatter.block.FlareStackBlockEntity::new, FLARE_STACK);
	public static final BlockEntityType<dev.wildcatter.block.InjectionPumpBlockEntity> INJECTION_PUMP_ENTITY = entity(dev.wildcatter.block.InjectionPumpBlockEntity::new, INJECTION_PUMP);
	public static final BlockEntityType<dev.wildcatter.block.TurbineGeneratorBlockEntity> TURBINE_GENERATOR_ENTITY = entity(dev.wildcatter.block.TurbineGeneratorBlockEntity::new, TURBINE_GENERATOR);
	public static final BlockEntityType<dev.wildcatter.block.GensetControlPanelBlockEntity> GENSET_CONTROL_PANEL_ENTITY = entity(dev.wildcatter.block.GensetControlPanelBlockEntity::new, GENSET_CONTROL_PANEL);
	public static final BlockEntityType<ThreePhaseHeaterBlockEntity> THREE_PHASE_HEATER_ENTITY = entity(ThreePhaseHeaterBlockEntity::new, THREE_PHASE_HEATER);
	public static final BlockEntityType<LoadBlockEntity> LOAD_ENTITY = entity(LoadBlockEntity::new, ELECTRIC_LAMP, ELECTRIC_HEATER);
	public static final BlockEntityType<BreakerBlockEntity> BREAKER_ENTITY = entity(BreakerBlockEntity::new, BREAKER, MV_BREAKER, DIST_BREAKER, HV_BREAKER, SWITCH, UFLS_RELAY, UFLS_RELAY_DIST,
			BREAKER_3P_LV, BREAKER_3P_INDUSTRIAL, HV_BREAKER_3P, DIST_BREAKER_3P);
	public static final BlockEntityType<FuseBlockEntity> FUSE_ENTITY = entity(FuseBlockEntity::new, FUSE, FUSED_CUTOUT);
	public static final BlockEntityType<ReceptacleBlockEntity> RECEPTACLE_ENTITY = entity(ReceptacleBlockEntity::new, RECEPTACLE_NEMA_5_15, RECEPTACLE_NEMA_5_20, RECEPTACLE_NEMA_14_50, RECEPTACLE_CEE_7, RECEPTACLE_IEC_60309_BLUE,
			RECEPTACLE_NEMA_L21_30, RECEPTACLE_NEMA_L22_30, RECEPTACLE_IEC_60309_RED_32, RECEPTACLE_IEC_60309_RED_63);
	public static final BlockEntityType<PlugAdapterBlockEntity> PLUG_ADAPTER_ENTITY = entity(PlugAdapterBlockEntity::new, PLUG_NEMA_5_15, PLUG_NEMA_5_20, PLUG_NEMA_14_50, PLUG_CEE_7, PLUG_IEC_60309_BLUE,
			PLUG_NEMA_L21_30, PLUG_NEMA_L22_30, PLUG_IEC_60309_RED_32, PLUG_IEC_60309_RED_63);
	public static final BlockEntityType<InsulatorBlockEntity> INSULATOR_ENTITY = entity(InsulatorBlockEntity::new, INSULATOR_LV, INSULATOR_DIST, INSULATOR_HV);
	public static final BlockEntityType<dev.wildcatter.block.PowerTransformerBlockEntity> POWER_TRANSFORMER_ENTITY = entity(dev.wildcatter.block.PowerTransformerBlockEntity::new, POWER_TRANSFORMER);
	public static final BlockEntityType<dev.wildcatter.block.DisconnectSwitchBlockEntity> DISCONNECT_ENTITY = entity(dev.wildcatter.block.DisconnectSwitchBlockEntity::new, DISCONNECT_HV, DISCONNECT_DIST);
	public static final BlockEntityType<TransformerBlockEntity> TRANSFORMER_ENTITY = entity(TransformerBlockEntity::new, TRANSFORMER, SUBSTATION_TRANSFORMER, POLE_TRANSFORMER,
			PADMOUNT_208, PADMOUNT_480, UNIT_SUBSTATION, GSU_TRANSFORMER, GSU_25, GSU_60, SUBSTATION_TRANSFORMER_60, UNIT_AUX_TRANSFORMER);

	public static final BlockEntityType<FireboxBlockEntity> FIREBOX_ENTITY = entity(FireboxBlockEntity::new, FIREBOX);
	public static final BlockEntityType<PumpBlockEntity> PUMP_ENTITY = entity(PumpBlockEntity::new, PUMP);

	public static final BlockEntityType<DrillingRigBlockEntity> DRILLING_RIG_ENTITY = entity(DrillingRigBlockEntity::new, DRILLING_RIG);
	public static final BlockEntityType<PumpjackBlockEntity> PUMPJACK_ENTITY = entity(PumpjackBlockEntity::new, PUMPJACK);
	public static final BlockEntityType<GasWellheadBlockEntity> GAS_WELLHEAD_ENTITY = entity(GasWellheadBlockEntity::new, GAS_WELLHEAD);

	public static final BlockEntityType<dev.wildcatter.block.TowerBasinBlockEntity> TOWER_BASIN_ENTITY = entity(dev.wildcatter.block.TowerBasinBlockEntity::new, TOWER_BASIN);
	public static final BlockEntityType<CoolingTowerBlockEntity> COOLING_TOWER_ENTITY = entity(CoolingTowerBlockEntity::new, COOLING_TOWER);
	/** The plain true-size parts that only remember they have grown (block/BigPartBlockEntity). */
	public static final BlockEntityType<dev.wildcatter.block.BigPartBlockEntity> BIG_PART_ENTITY = entity(dev.wildcatter.block.BigPartBlockEntity::new, COOLING_FILL, DRIFT_ELIMINATOR, RELAY_PANEL, FUEL_TRANSFER_SYSTEM, TURBINE_VALVE_CHEST, MOISTURE_SEPARATOR_REHEATER, LP_TURBINE,
			WOOD_POLE_30, WOOD_POLE_40, WOOD_POLE_75, CROSSARM_DIST, CROSSBEAM_138, X_BRACE_138, FUEL_ASSEMBLY);
	public static final BlockEntityType<dev.wildcatter.block.StationBusBlockEntity> STATION_BUS_ENTITY = entity(dev.wildcatter.block.StationBusBlockEntity::new, BUS_HV, BUS_DIST);

	private WildcatterBlocks() {
	}

	public static Block receptacleFor(PlugType type) {
		return switch (type) {
			case NEMA_5_15 -> RECEPTACLE_NEMA_5_15;
			case NEMA_5_20 -> RECEPTACLE_NEMA_5_20;
			case NEMA_14_50 -> RECEPTACLE_NEMA_14_50;
			case CEE_7 -> RECEPTACLE_CEE_7;
			case IEC_60309_BLUE -> RECEPTACLE_IEC_60309_BLUE;
			case NEMA_L21_30 -> RECEPTACLE_NEMA_L21_30;
			case NEMA_L22_30 -> RECEPTACLE_NEMA_L22_30;
			case IEC_60309_RED_32 -> RECEPTACLE_IEC_60309_RED_32;
			case IEC_60309_RED_63 -> RECEPTACLE_IEC_60309_RED_63;
		};
	}

	public static Block plugFor(PlugType type) {
		return switch (type) {
			case NEMA_5_15 -> PLUG_NEMA_5_15;
			case NEMA_5_20 -> PLUG_NEMA_5_20;
			case NEMA_14_50 -> PLUG_NEMA_14_50;
			case CEE_7 -> PLUG_CEE_7;
			case IEC_60309_BLUE -> PLUG_IEC_60309_BLUE;
			case NEMA_L21_30 -> PLUG_NEMA_L21_30;
			case NEMA_L22_30 -> PLUG_NEMA_L22_30;
			case IEC_60309_RED_32 -> PLUG_IEC_60309_RED_32;
			case IEC_60309_RED_63 -> PLUG_IEC_60309_RED_63;
		};
	}

	public static void register() {
		add("boiler", BOILER);
		add("steam_turbine", STEAM_TURBINE);
		add("firebox", FIREBOX);
		add("pipe", PIPE);
		add("pump", PUMP);
		add("gas_line", GAS_LINE);
		add("drilling_rig", DRILLING_RIG);
		add("casing", CASING);
		add("pumpjack", PUMPJACK);
		add("gas_wellhead", GAS_WELLHEAD);
		add("oil_wellhead", OIL_WELLHEAD);
		add("cooling_tower", COOLING_TOWER);
		add("cooling_fill", COOLING_FILL);
		add("tower_basin", TOWER_BASIN);
		add("drift_eliminator", DRIFT_ELIMINATOR);
		add("plastic_wire_hot", PLASTIC_WIRE_HOT);
		add("plastic_wire_neutral", PLASTIC_WIRE_NEUTRAL);
		add("mv_plastic_wire_hot", MV_PLASTIC_WIRE_HOT);
		add("mv_plastic_wire_neutral", MV_PLASTIC_WIRE_NEUTRAL);
		add("hv_plastic_wire_hot", HV_PLASTIC_WIRE_HOT);
		add("hv_plastic_wire_neutral", HV_PLASTIC_WIRE_NEUTRAL);
		add("wire_hot", WIRE_HOT);
		add("wire_neutral", WIRE_NEUTRAL);
		add("wool_wire_hot", WOOL_WIRE_HOT);
		add("wool_wire_neutral", WOOL_WIRE_NEUTRAL);
		add("mv_wire_hot", MV_WIRE_HOT);
		add("mv_wire_neutral", MV_WIRE_NEUTRAL);
		add("dist_wire_hot", DIST_WIRE_HOT);
		add("dist_wire_neutral", DIST_WIRE_NEUTRAL);
		add("dist_plastic_wire_hot", DIST_PLASTIC_WIRE_HOT);
		add("dist_plastic_wire_neutral", DIST_PLASTIC_WIRE_NEUTRAL);
		add("hv_wire_hot", HV_WIRE_HOT);
		add("hv_wire_neutral", HV_WIRE_NEUTRAL);
		add("ground_rod", GROUND_ROD);
		add("ground_wire", GROUND_WIRE);
		add("pole", POLE);
		add("crossarm", CROSSARM);
		add("wood_pole_30", WOOD_POLE_30);
		add("wood_pole_40", WOOD_POLE_40);
		add("wood_pole_75", WOOD_POLE_75);
		add("crossarm_dist", CROSSARM_DIST);
		add("crossbeam_138", CROSSBEAM_138);
		add("x_brace_138", X_BRACE_138);
		add("breaker", BREAKER);
		for (PlugType type : PlugType.values()) {
			add("receptacle_" + type.id(), receptacleFor(type));
			add("plug_" + type.id(), plugFor(type));
		}
		add("fuse", FUSE);
		add("fused_cutout", FUSED_CUTOUT);
		add("insulator_lv", INSULATOR_LV);
		add("insulator_dist", INSULATOR_DIST);
		add("insulator_hv", INSULATOR_HV);
		add("switch", SWITCH);
		add("ufls_relay", UFLS_RELAY);
		add("ufls_relay_dist", UFLS_RELAY_DIST);
		add("mv_breaker", MV_BREAKER);
		add("dist_breaker", DIST_BREAKER);
		add("hv_breaker", HV_BREAKER);
		add("transformer", TRANSFORMER);
		add("substation_transformer", SUBSTATION_TRANSFORMER);
		add("power_transformer", POWER_TRANSFORMER);
		add("hv_breaker_3p", HV_BREAKER_3P);
		add("dist_breaker_3p", DIST_BREAKER_3P);
		add("disconnect_hv", DISCONNECT_HV);
		add("disconnect_dist", DISCONNECT_DIST);
		add("bus_hv", BUS_HV);
		add("bus_dist", BUS_DIST);
		add("relay_panel", RELAY_PANEL);
		add("chain_link_fence", CHAIN_LINK_FENCE);
		add("barbed_wire_top", BARBED_WIRE_TOP);
		add("pole_transformer", POLE_TRANSFORMER);
		add("electric_lamp", ELECTRIC_LAMP);
		add("electric_heater", ELECTRIC_HEATER);
		add("industrial_wire_hot", INDUSTRIAL_WIRE_HOT);
		add("industrial_wire_neutral", INDUSTRIAL_WIRE_NEUTRAL);
		add("industrial_plastic_wire_hot", INDUSTRIAL_PLASTIC_WIRE_HOT);
		add("industrial_plastic_wire_neutral", INDUSTRIAL_PLASTIC_WIRE_NEUTRAL);
		add("cable_lv", CABLE_LV);
		add("cable_industrial", CABLE_INDUSTRIAL);
		add("cable_mv", CABLE_MV);
		add("cable_dist", CABLE_DIST);
		add("cable_hv", CABLE_HV);
		add("busway_lv", BUSWAY_LV);
		add("busway_industrial", BUSWAY_INDUSTRIAL);
		add("breaker_3p_lv", BREAKER_3P_LV);
		add("breaker_3p_industrial", BREAKER_3P_INDUSTRIAL);
		add("three_phase_heater", THREE_PHASE_HEATER);
		add("motor_starter", MOTOR_STARTER);
		add("mcc_incoming", MCC_INCOMING);
		add("genset_control_panel", GENSET_CONTROL_PANEL);
		add("genset_diesel_engine", DIESEL_ENGINE_PART);
		add("genset_gas_engine", GAS_ENGINE_PART);
		add("alternator", ALTERNATOR);
		add("radiator", RADIATOR);
		add("fuel_tank", FUEL_TANK);
		add("exhaust_stack", EXHAUST_STACK);
		add("genset_base_frame", BASE_FRAME);
		add("exhaust_silencer_large", EXHAUST_SILENCER_LARGE);
		add("turbine_generator", TURBINE_GENERATOR);
		add("boiler_burner", BOILER_BURNER);
		add("boiler_drum", BOILER_DRUM);
		add("superheater", SUPERHEATER);
		add("turbine_casing", TURBINE_CASING);
		add("condenser", CONDENSER);
		add("feed_pump", FEED_PUMP);
		for (Block part : new Block[] {RIG_SUBSTRUCTURE, RIG_MAST, RIG_DRAWWORKS, MUD_PUMP, MUD_TANKS, RIG_DRIVE_HOUSE}) {
			add(((dev.wildcatter.block.RigPartBlock) part).part().id(), part);
		}
		for (Block part : new Block[] {SEPARATOR, STOCK_TANK, WATER_TANK, FLARE_STACK, INJECTION_PUMP}) {
			add(((dev.wildcatter.block.LeasePartBlock) part).part().id(), part);
		}
		add("disposal_wellhead", DISPOSAL_WELLHEAD);
		for (Block part : new Block[] {GAS_COMPRESSOR, TEG_DEHYDRATOR, NGL_CHILLER, REFRIGERATION_COMPRESSOR, STABILIZER, LPG_BULLET}) {
			add(((dev.wildcatter.block.GasPlantPartBlock) part).part().id(), part);
		}
		for (Block part : new Block[] {TOPPING_COLUMN, TOPPING_HEATER, TOPPING_SKID, ATMOSPHERIC_COLUMN, CRUDE_HEATER, DESALTER, CRUDE_CHARGE_PUMP, SIDE_STRIPPERS, OVERHEAD_CONDENSER}) {
			add(((dev.wildcatter.block.RefineryPartBlock) part).part().id(), part);
		}
		for (Block part : new Block[] {GAS_TURBINE, GT_INLET_HOUSE, GT_GENERATOR, GEN_BREAKER, GT_EXHAUST_STACK, HRSG, STEAM_TURBINE_5, ST_GENERATOR, AIR_COOLED_CONDENSER, HRSG_FEED_PUMP, LOAD_BANK}) {
			add(((dev.wildcatter.block.TurbinePartBlock) part).part().id(), part);
		}
		for (Block part : new Block[] {AERO_TURBINE, AERO_INLET_HOUSE, AERO_GENERATOR, AERO_EXHAUST_STACK, GEN_BREAKER_3000}) {
			add(((dev.wildcatter.block.TurbinePartBlock) part).part().id(), part);
		}
		add("gsu_60", GSU_60);
		add("substation_transformer_60", SUBSTATION_TRANSFORMER_60);
		add("gsu_25", GSU_25);
		add("bus_duct_gen", BUS_DUCT_GEN);
		for (Block part : new Block[] {CRACKING_FURNACE, QUENCH_TOWER, CRACKED_GAS_COMPRESSOR, COLD_BOX, POLYETHYLENE_REACTOR, PELLETIZER}) {
			add(((dev.wildcatter.block.PetrochemPartBlock) part).part().id(), part);
		}
		for (Block part : new Block[] {SULFUR_MELTER, DRYING_TOWER, MAIN_BLOWER, SULFUR_FURNACE, WASTE_HEAT_BOILER, CONVERTER, INTERPASS_ABSORBER, FINAL_ABSORBER}) {
			add(((dev.wildcatter.block.AcidPlantPartBlock) part).part().id(), part);
		}
		for (Block part : new Block[] {JAW_CRUSHER, SAG_MILL, LEACH_TANK, CCD_THICKENER, SX_CIRCUIT, PRECIPITATION_TANK, YELLOWCAKE_DRYER, DRUM_FILLER}) {
			add(((dev.wildcatter.block.MillPartBlock) part).part().id(), part);
		}
		for (Block part : new Block[] {HEADER_HOUSE, OXYGEN_GENERATOR, IX_COLUMNS}) {
			add(((dev.wildcatter.block.WellfieldPartBlock) part).part().id(), part);
		}
		add("isr_wellhead", ISR_WELLHEAD);
		for (Block part : new Block[] {REDUCTION_REACTOR, HYDROFLUORINATOR, FLUORINATOR, COLD_TRAPS, DISTILLATION_COLUMNS, CYLINDER_FILLER, STEAM_REFORMER, HF_KILN, HF_COLUMN, RECTIFIER, FLUORINE_CELL_HOUSE}) {
			add(((dev.wildcatter.block.ConversionPartBlock) part).part().id(), part);
		}
		for (Block part : new Block[] {CENTRIFUGE, FREQUENCY_CONVERTER, FEED_STATION, PRODUCT_TAKEOFF, TAILS_TAKEOFF}) {
			add(((dev.wildcatter.block.EnrichmentPartBlock) part).part().id(), part);
		}
		for (Block part : new Block[] {MINERAL_SEPARATOR, CHLORINATOR, HAFNIUM_COLUMN, KROLL_REACTOR, VACUUM_DISTILLATION, VAR_FURNACE, EXTRUSION_PRESS, PILGER_MILL, VACUUM_ANNEAL_FURNACE, MAGNESIUM_CELL, MAGNESIA_CHLORINATOR, CHLOR_ALKALI_CELL}) {
			add(((dev.wildcatter.block.ZirconiumPartBlock) part).part().id(), part);
		}
		for (Block part : new Block[] {BALL_MILL, DIGESTER, FLASH_TANKS, SETTLER, MUD_WASHER, RESIDUE_FILTER_PRESS, POLISHING_FILTER, PRECIPITATOR, HYDRATE_FILTER, CALCINER, EVAPORATOR, CAUSTICIZER, PACKAGE_BOILER, LIME_KILN, ALF3_REACTOR, BAGGING_STATION, CHLORINE_FILLER}) {
			add(((dev.wildcatter.block.AluminaPartBlock) part).part().id(), part);
		}
		add("red_mud", RED_MUD);
		for (Block part : new Block[] {COKER_FRACTIONATOR, COKER_HEATER, COKE_DRUM, COKE_PIT, COKE_KILN, COKE_COOLER, COAL_TOWER, COKE_OVEN_BATTERY, PUSHER_MACHINE, COKE_QUENCH_TOWER, BYPRODUCT_PLANT, TAR_STILL, PASTE_PLANT, FORMING_PRESS, BAKING_FURNACE, RODDING_SHOP}) {
			add(((dev.wildcatter.block.CarbonPartBlock) part).part().id(), part);
		}
		for (Block part : new Block[] {RECTIFORMER, POT, FUME_TREATMENT_CENTRE, FTC_FAN, CASTING_MACHINE}) {
			add(((dev.wildcatter.block.PotlinePartBlock) part).part().id(), part);
		}
		add("aluminium_block", ALUMINIUM_BLOCK);
		add("site_marker", SITE_MARKER);
		add("tower_crane", TOWER_CRANE);
		add("crawler_crane", CRAWLER_CRANE);
		for (Block part : new Block[] {UF6_AUTOCLAVE, IDR_KILN, GRANULATOR, PELLET_PRESS, SINTERING_FURNACE, PELLET_GRINDER, ROD_LOADER, ROD_WELDER, ASSEMBLY_FIXTURE, HELIUM_RECOVERY_UNIT}) {
			add(((dev.wildcatter.block.FuelPartBlock) part).part().id(), part);
		}
		add("fuel_assembly", FUEL_ASSEMBLY);
		for (Block part : new Block[] {REACTOR_VESSEL, CRDM_HEAD, REACTOR_CONSOLE, STEAM_GENERATOR, REACTOR_COOLANT_PUMP, PRESSURIZER, CVCS_SKID, AUX_FEED_PUMP, RHR_PUMP, RHR_HEAT_EXCHANGER}) {
			add(((dev.wildcatter.block.ReactorPartBlock) part).part().id(), part);
		}
		for (Block part : new Block[] {TURBINE_VALVE_CHEST, HP_TURBINE, MOISTURE_SEPARATOR_REHEATER, LP_TURBINE, MAIN_CONDENSER, CONDENSATE_PUMP, DEAERATOR, MAIN_FEED_PUMP, HP_FEEDWATER_HEATER, CIRCULATING_WATER_PUMP}) {
			add(((dev.wildcatter.block.TurbineIslandPartBlock) part).part().id(), part);
		}
		add("tg_generator_60", TG_GENERATOR_60);
		add("natural_draft_tower", NATURAL_DRAFT_TOWER);
		for (Block part : new Block[] {REFUELLING_MACHINE, FUEL_TRANSFER_SYSTEM, SPENT_FUEL_POOL}) {
			add(((dev.wildcatter.block.FuelHandlingPartBlock) part).part().id(), part);
		}
		Registry.register(BuiltInRegistries.BLOCK, Wildcatter.id("tower_shell"), TOWER_SHELL);   // no item: the tower places it
		add("unit_auxiliary_transformer", UNIT_AUX_TRANSFORMER);
		for (Block part : new Block[] {BORAX_DISSOLVER, BORIC_ACID_CRYSTALLIZER, B4C_FURNACE, RCCA_BENCH}) {
			add(((dev.wildcatter.block.BoronPartBlock) part).part().id(), part);
		}
		Registry.register(BuiltInRegistries.BLOCK, Wildcatter.id("rotor"), ROTOR);   // no item: it is never placed
		Registry.register(BuiltInRegistries.BLOCK, Wildcatter.id("mcc_bucket"), MCC_BUCKET);   // its item is WildcatterItems.MCC_BUCKET
		add("mcc_section", MCC_SECTION);
		add("soft_starter", SOFT_STARTER);
		add("drive_cabinet", DRIVE_CABINET);
		add("soft_starter_mv", SOFT_STARTER_MV);
		add("drive_mv_transformer", DRIVE_MV_TRANSFORMER);
		add("drive_mv_cells", DRIVE_MV_CELLS);
		add("drive_mv_control", DRIVE_MV_CONTROL);
		add("dc_bus_4k", DC_BUS_4K);
		add("dc_bus_16k", DC_BUS_16K);
		add("dc_bus_180k", DC_BUS_180K);
		add("dc_bus_180k_copper", DC_BUS_180K_COPPER);
		add("padmount_208", PADMOUNT_208);
		add("padmount_480", PADMOUNT_480);
		add("unit_substation", UNIT_SUBSTATION);
		add("gsu_transformer", GSU_TRANSFORMER);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("boiler"), BOILER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("steam_turbine"), STEAM_TURBINE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("firebox"), FIREBOX_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("pump"), PUMP_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("drilling_rig"), DRILLING_RIG_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("pumpjack"), PUMPJACK_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("gas_wellhead"), GAS_WELLHEAD_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("cooling_tower"), COOLING_TOWER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("big_part"), BIG_PART_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("station_bus"), STATION_BUS_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("wire"), WIRE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("load"), LOAD_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("breaker"), BREAKER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("fuse"), FUSE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("receptacle"), RECEPTACLE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("plug_adapter"), PLUG_ADAPTER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("insulator"), INSULATOR_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("transformer"), TRANSFORMER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("multi_cable"), MULTI_CABLE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("three_phase_heater"), THREE_PHASE_HEATER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("motor_starter"), MOTOR_STARTER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("mcc_incoming"), MCC_INCOMING_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("mcc_section"), MCC_SECTION_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("starter_cabinet"), STARTER_CABINET_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("drive_section"), DRIVE_SECTION_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("dc_bus"), DC_BUS_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("genset_part"), GENSET_PART_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("tower_basin"), TOWER_BASIN_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("power_transformer"), POWER_TRANSFORMER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("disconnect_switch"), DISCONNECT_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("steam_plant_part"), STEAM_PLANT_PART_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("rig_substructure"), RIG_SUBSTRUCTURE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("rig_drive_house"), RIG_DRIVE_HOUSE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("rig_part"), RIG_PART_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("separator"), SEPARATOR_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("storage_tank"), STORAGE_TANK_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("flare_stack"), FLARE_STACK_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("injection_pump"), INJECTION_PUMP_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("gas_compressor"), GAS_COMPRESSOR_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("dehydrator"), DEHYDRATOR_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("chiller"), CHILLER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("stabilizer"), STABILIZER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("unit_motor"), UNIT_MOTOR_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("gas_turbine"), GAS_TURBINE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("generator_part"), GENERATOR_PART_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("gen_breaker"), GEN_BREAKER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("hrsg"), HRSG_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("steam_cycle"), STEAM_CYCLE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("load_bank"), LOAD_BANK_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("turbine_part"), TURBINE_PASSIVE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("fired_heater"), FIRED_HEATER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("side_strippers"), STRIPPER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("topping_column"), TOPPING_COLUMN_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("crude_column"), CRUDE_COLUMN_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("cracking_furnace"), CRACKING_FURNACE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("quench_tower"), QUENCH_TOWER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("cold_box"), COLD_BOX_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("polyethylene_reactor"), PE_REACTOR_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("pelletizer"), PELLETIZER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("sulfur_melter"), SULFUR_MELTER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("acid_passive"), ACID_PASSIVE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("sulfur_furnace"), SULFUR_FURNACE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("waste_heat_boiler"), WASTE_HEAT_BOILER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("converter"), CONVERTER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("final_absorber"), FINAL_ABSORBER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("jaw_crusher"), JAW_CRUSHER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("sag_mill"), SAG_MILL_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("leach_tank"), LEACH_TANK_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("ccd_thickener"), CCD_THICKENER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("sx_circuit"), SX_CIRCUIT_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("precipitation_tank"), PRECIPITATION_TANK_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("yellowcake_dryer"), YELLOWCAKE_DRYER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("drum_filler"), DRUM_FILLER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("header_house"), HEADER_HOUSE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("oxygen_generator"), OXYGEN_GENERATOR_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("ix_columns"), IX_COLUMNS_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("isr_wellhead"), ISR_WELLHEAD_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("reduction_reactor"), REDUCTION_REACTOR_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("conversion_stage"), CONVERSION_STAGE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("uf6_stage"), UF6_STAGE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("cylinder_filler"), CYLINDER_FILLER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("steam_reformer"), STEAM_REFORMER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("hf_kiln"), HF_KILN_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("hf_column"), HF_COLUMN_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("rectifier"), RECTIFIER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("fluorine_cell_house"), FLUORINE_CELL_HOUSE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("centrifuge"), CENTRIFUGE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("frequency_converter"), FREQUENCY_CONVERTER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("feed_station"), FEED_STATION_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("takeoff"), TAKEOFF_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("zr_line"), ZR_LINE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("chlorinator"), CHLORINATOR_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("kroll_reactor"), KROLL_REACTOR_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("pilger_mill"), PILGER_MILL_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("magnesium_cell"), MAGNESIUM_CELL_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("magnesia_chlorinator"), MAGNESIA_CHLORINATOR_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("chlor_alkali_cell"), CHLOR_ALKALI_CELL_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("bayer_stage"), BAYER_STAGE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("residue_filter_press"), RESIDUE_FILTER_PRESS_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("hydrate_filter"), HYDRATE_FILTER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("calciner"), CALCINER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("causticizer"), CAUSTICIZER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("lime_kiln"), LIME_KILN_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("alf3_reactor"), ALF3_REACTOR_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("bagging_station"), BAGGING_STATION_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("chlorine_filler"), CHLORINE_FILLER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("coker_fractionator"), COKER_FRACTIONATOR_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("coker_heater"), COKER_HEATER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("coke_drum"), COKE_DRUM_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("coke_pit"), COKE_PIT_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("coke_kiln"), COKE_KILN_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("coke_cooler"), COKE_COOLER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("coal_tower"), COAL_TOWER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("coke_oven_battery"), COKE_OVEN_BATTERY_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("coke_quench_tower"), COKE_QUENCH_TOWER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("byproduct_plant"), BYPRODUCT_PLANT_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("tar_still"), TAR_STILL_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("paste_plant"), PASTE_PLANT_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("forming_press"), FORMING_PRESS_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("baking_furnace"), BAKING_FURNACE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("rodding_shop"), RODDING_SHOP_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("rectiformer"), RECTIFORMER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("pot"), POT_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("fume_treatment_centre"), FUME_TREATMENT_CENTRE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("casting_machine"), CASTING_MACHINE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("site_marker"), SITE_MARKER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("crane"), CRANE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("fuel_line"), FUEL_LINE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("rod_welder"), ROD_WELDER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("assembly_fixture"), ASSEMBLY_FIXTURE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("helium_recovery_unit"), HELIUM_RECOVERY_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("reactor_vessel"), REACTOR_VESSEL_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("reactor_console"), REACTOR_CONSOLE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("steam_generator"), STEAM_GENERATOR_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("reactor_coolant_pump"), REACTOR_COOLANT_PUMP_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("cvcs_skid"), CVCS_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("aux_feed_pump"), AUX_FEED_PUMP_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("boron_line"), BORON_LINE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("hp_turbine"), HP_TURBINE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("rhr_heat_exchanger"), RHR_HX_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("refuelling_machine"), REFUELLING_MACHINE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("spent_fuel_pool"), SPENT_FUEL_POOL_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("natural_draft_tower"), NATURAL_DRAFT_TOWER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("main_condenser"), CONDENSER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("deaerator"), DEAERATOR_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("hp_feedwater_heater"), HP_HEATER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("circulating_water_pump"), CW_PUMP_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("turbine_generator"), TURBINE_GENERATOR_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("genset_control_panel"), GENSET_CONTROL_PANEL_ENTITY);
	}

	/** Registers the block and its item. */
	private static void add(String name, Block block) {
		Registry.register(BuiltInRegistries.BLOCK, Wildcatter.id(name), block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Wildcatter.id(name));
		Item.Properties props = new Item.Properties().setId(itemKey).useBlockDescriptionPrefix();
		Item item = block instanceof CableBlock wire ? new ConductorItem(block, props, wire.voltage(), ConductorSize.Kind.WIRE)
				: block instanceof MultiCableBlock cable ? new ConductorItem(block, props, cable.voltage(), cable.busway() ? ConductorSize.Kind.BUSWAY : ConductorSize.Kind.CABLE)
				: block instanceof dev.wildcatter.block.FuelAssemblyBlock ? new dev.wildcatter.item.FuelAssemblyItem(block, props.stacksTo(1))
				: block instanceof dev.wildcatter.multiblock.BigPart ? new dev.wildcatter.item.BigPartItem(block, props)
				: new BlockItem(block, props);
		Registry.register(BuiltInRegistries.ITEM, itemKey, item);
	}
}
