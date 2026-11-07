package dev.wildcatter;

import static dev.wildcatter.BlockFactory.*;

import dev.wildcatter.block.BoilerBlock;
import dev.wildcatter.block.CoolingFillBlock;
import dev.wildcatter.block.CoolingTowerBlock;
import net.minecraft.world.level.material.MapColor;
import dev.wildcatter.block.FireboxBlock;
import dev.wildcatter.block.PipeBlock;
import dev.wildcatter.block.PumpBlock;
import dev.wildcatter.block.SteamTurbineBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/** Boilers, steam and gas turbines, gensets, cooling towers and the turbine island. Reached through {@link WildcatterBlocks}, which registers them. */
public interface PowerPlantBlocks {
	Block BOILER = block("boiler", props -> new BoilerBlock(props.mapColor(MapColor.METAL)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	Block STEAM_TURBINE = block("steam_turbine", props -> new SteamTurbineBlock(props.mapColor(MapColor.METAL)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

	/** Holds the fuel and heats the boiler above it. Nine in a 3x3 floor become one big firebox. */
	Block FIREBOX = block("firebox", props -> new FireboxBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(3.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()
			.lightLevel(state -> state.getValue(FireboxBlock.LIT) ? 13 : 0)));
	/** Water pipe: carries the pump's water to the boilers, and the turbines' condensate back. */
	Block PIPE = block("pipe", props -> new PipeBlock(props.mapColor(MapColor.METAL)
			.strength(1.5F, 4.0F).sound(SoundType.METAL).noOcclusion(), PipeBlock.Kind.WATER));
	Block CONTROL_VALVE = block("control_valve", props -> new dev.wildcatter.control.ValveBlock(props.mapColor(MapColor.METAL).strength(1.5F, 4.0F).sound(SoundType.METAL).noOcclusion(), PipeBlock.Kind.WATER, dev.wildcatter.control.ValveBlock.Type.CONTROL));
	Block ON_OFF_VALVE = block("on_off_valve", props -> new dev.wildcatter.control.ValveBlock(props.mapColor(MapColor.METAL).strength(1.5F, 4.0F).sound(SoundType.METAL).noOcclusion(), PipeBlock.Kind.WATER, dev.wildcatter.control.ValveBlock.Type.ON_OFF));
	Block MOTOR_OPERATED_VALVE = block("motor_operated_valve", props -> new dev.wildcatter.control.ValveBlock(props.mapColor(MapColor.METAL).strength(1.5F, 4.0F).sound(SoundType.METAL).noOcclusion(), PipeBlock.Kind.WATER, dev.wildcatter.control.ValveBlock.Type.MOTOR_OPERATED));
	Block CONTROL_VALVE_STEEL = block("control_valve_steel", props -> new dev.wildcatter.control.ValveBlock(props.mapColor(MapColor.COLOR_ORANGE).strength(1.5F, 4.0F).sound(SoundType.METAL).noOcclusion(), PipeBlock.Kind.GAS, dev.wildcatter.control.ValveBlock.Type.CONTROL));
	Block ON_OFF_VALVE_STEEL = block("on_off_valve_steel", props -> new dev.wildcatter.control.ValveBlock(props.mapColor(MapColor.COLOR_ORANGE).strength(1.5F, 4.0F).sound(SoundType.METAL).noOcclusion(), PipeBlock.Kind.GAS, dev.wildcatter.control.ValveBlock.Type.ON_OFF));
	Block MOTOR_OPERATED_VALVE_STEEL = block("motor_operated_valve_steel", props -> new dev.wildcatter.control.ValveBlock(props.mapColor(MapColor.COLOR_ORANGE).strength(1.5F, 4.0F).sound(SoundType.METAL).noOcclusion(), PipeBlock.Kind.GAS, dev.wildcatter.control.ValveBlock.Type.MOTOR_OPERATED));
	Block AIR_TUBING = block("air_tubing", props -> new PipeBlock(props.mapColor(MapColor.METAL).strength(0.5F).sound(SoundType.METAL).noOcclusion(), PipeBlock.Kind.AIR));
	Block AIR_COMPRESSOR = block("air_compressor", props -> new dev.wildcatter.control.AirCompressorBlock(props.mapColor(MapColor.COLOR_BLUE).strength(2.0F, 5.0F).sound(SoundType.METAL)));
	Block AIR_DRYER = block("air_dryer", props -> new dev.wildcatter.control.AirDryerBlock(props.mapColor(MapColor.COLOR_GRAY).strength(2.0F, 5.0F).sound(SoundType.METAL)));
	Block DAY_TANK = block("day_tank", props -> new dev.wildcatter.block.DayTankBlock(props.mapColor(MapColor.COLOR_LIGHT_GRAY).strength(2.0F, 5.0F).sound(SoundType.METAL)));
	Block TRANSMITTER = block("transmitter", props -> new dev.wildcatter.control.TransmitterBlock(props.mapColor(MapColor.COLOR_GRAY).strength(0.5F).sound(SoundType.METAL).noOcclusion()));
	/** A 110 V electric pump that lifts water from the water touching it. */
	Block PUMP = block("pump", props -> new PumpBlock(props.mapColor(MapColor.COLOR_BLUE)
			.strength(2.0F, 5.0F).sound(SoundType.METAL)));

	/** The water pump on a 37 kW, 480 V three-phase motor, for a plant with no 110 V supply (docs/multiblocks/tailings-cell.md). */
	Block PROCESS_WATER_PUMP = block("process_water_pump", props -> new PumpBlock(props.mapColor(MapColor.COLOR_BLUE)
			.strength(2.0F, 5.0F).sound(SoundType.METAL)));
	/** The lined tailings cell (docs/multiblocks/tailings-cell.md). */
	Block CELL_LINER = block("cell_liner", props -> new Block(props.mapColor(MapColor.COLOR_BLACK).strength(1.0F, 3.0F).sound(SoundType.WOOL)));
	Block SETTLED_TAILS = block("settled_tails", props -> new Block(props.mapColor(MapColor.SAND).strength(0.5F).sound(SoundType.SAND)));
	Block DECANT_TOWER = block("decant_tower", props -> new dev.wildcatter.block.DecantTowerBlock(props.mapColor(MapColor.STONE)
			.strength(2.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));

	/** Carries gas (and pumped crude) from a well to the fireboxes. Never joins a water pipe. */
	Block GAS_LINE = block("gas_line", props -> new PipeBlock(props.mapColor(MapColor.COLOR_ORANGE)
			.strength(1.5F, 4.0F).sound(SoundType.METAL).noOcclusion(), PipeBlock.Kind.GAS));

	/** A fan base plus two fill sections make a cooling tower: it cools the condensate of turbines on the same water pipes. */
	Block COOLING_TOWER = block("cooling_tower", props -> new CoolingTowerBlock(props.mapColor(MapColor.STONE)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	/** The foot and top of the cooling-tower column (docs/multiblocks/cooling-tower.md). */
	Block TOWER_BASIN = block("tower_basin", props -> new dev.wildcatter.block.TowerBasinBlock(props.mapColor(MapColor.STONE)
			.strength(2.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
	Block DRIFT_ELIMINATOR = block("drift_eliminator", props -> new CoolingFillBlock(props.mapColor(MapColor.STONE)
			.strength(1.5F, 3.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), dev.wildcatter.multiblock.Footprint.box(4, 1, 4, 0, 0), "drift_eliminator"));
	Block COOLING_FILL = block("cooling_fill", props -> new CoolingFillBlock(props.mapColor(MapColor.STONE)
			.strength(2.0F, 4.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), dev.wildcatter.multiblock.Footprint.box(4, 2, 4, 0, 0), "cooling_fill"));

	// ---- Gensets (docs/multiblocks/genset.md) ----
	Block GENSET_CONTROL_PANEL = block("genset_control_panel", props -> new dev.wildcatter.block.GensetControlPanelBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	Block DIESEL_ENGINE_PART = gensetPart("genset_diesel_engine", dev.wildcatter.block.GensetPartBlock.Part.DIESEL_ENGINE);
	Block GAS_ENGINE_PART = gensetPart("genset_gas_engine", dev.wildcatter.block.GensetPartBlock.Part.GAS_ENGINE);
	Block ALTERNATOR = gensetPart("alternator", dev.wildcatter.block.GensetPartBlock.Part.ALTERNATOR);
	Block RADIATOR = gensetPart("radiator", dev.wildcatter.block.GensetPartBlock.Part.RADIATOR);
	Block FUEL_TANK = gensetPart("fuel_tank", dev.wildcatter.block.GensetPartBlock.Part.FUEL_TANK);
	Block EXHAUST_STACK = gensetPart("exhaust_stack", dev.wildcatter.block.GensetPartBlock.Part.EXHAUST);
	Block BASE_FRAME = gensetPart("genset_base_frame", dev.wildcatter.block.GensetPartBlock.Part.BASE_FRAME);
	Block EXHAUST_SILENCER_LARGE = gensetPart("exhaust_silencer_large", dev.wildcatter.block.GensetPartBlock.Part.SILENCER_LARGE);
	// ---- The 1 MW steam plant (docs/multiblocks/steam-plant.md) ----
	Block TURBINE_GENERATOR = block("turbine_generator", props -> new dev.wildcatter.block.TurbineGeneratorBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	Block BOILER_BURNER = plantPart("boiler_burner", dev.wildcatter.block.SteamPlantPartBlock.Part.BURNER);
	Block BOILER_DRUM = plantPart("boiler_drum", dev.wildcatter.block.SteamPlantPartBlock.Part.DRUM);
	Block SUPERHEATER = plantPart("superheater", dev.wildcatter.block.SteamPlantPartBlock.Part.SUPERHEATER);
	Block TURBINE_CASING = plantPart("turbine_casing", dev.wildcatter.block.SteamPlantPartBlock.Part.TURBINE);
	Block CONDENSER = plantPart("condenser", dev.wildcatter.block.SteamPlantPartBlock.Part.CONDENSER);
	Block FEED_PUMP = plantPart("feed_pump", dev.wildcatter.block.SteamPlantPartBlock.Part.FEED_PUMP);
	/** The gas turbine and combined cycle (docs/multiblocks/gas-turbine.md, combined-cycle.md). */
	Block GAS_TURBINE = turbinePart(dev.wildcatter.block.TurbinePartBlock.Part.GAS_TURBINE);
	Block GT_INLET_HOUSE = turbinePart(dev.wildcatter.block.TurbinePartBlock.Part.GT_INLET_HOUSE);
	Block GT_GENERATOR = turbinePart(dev.wildcatter.block.TurbinePartBlock.Part.GT_GENERATOR);
	Block GEN_BREAKER = turbinePart(dev.wildcatter.block.TurbinePartBlock.Part.GEN_BREAKER);
	Block GT_EXHAUST_STACK = turbinePart(dev.wildcatter.block.TurbinePartBlock.Part.GT_EXHAUST_STACK);
	Block HRSG = turbinePart(dev.wildcatter.block.TurbinePartBlock.Part.HRSG);
	Block STEAM_TURBINE_5 = turbinePart(dev.wildcatter.block.TurbinePartBlock.Part.STEAM_TURBINE_5);
	Block ST_GENERATOR = turbinePart(dev.wildcatter.block.TurbinePartBlock.Part.ST_GENERATOR);
	Block AIR_COOLED_CONDENSER = turbinePart(dev.wildcatter.block.TurbinePartBlock.Part.AIR_COOLED_CONDENSER);
	Block HRSG_FEED_PUMP = turbinePart(dev.wildcatter.block.TurbinePartBlock.Part.HRSG_FEED_PUMP);
	Block LOAD_BANK = turbinePart(dev.wildcatter.block.TurbinePartBlock.Part.LOAD_BANK);
	Block AERO_TURBINE = turbinePart(dev.wildcatter.block.TurbinePartBlock.Part.AERO_TURBINE);
	Block AERO_INLET_HOUSE = turbinePart(dev.wildcatter.block.TurbinePartBlock.Part.AERO_INLET_HOUSE);
	Block AERO_GENERATOR = turbinePart(dev.wildcatter.block.TurbinePartBlock.Part.AERO_GENERATOR);
	Block AERO_EXHAUST_STACK = turbinePart(dev.wildcatter.block.TurbinePartBlock.Part.AERO_EXHAUST_STACK);
	Block GEN_BREAKER_3000 = turbinePart(dev.wildcatter.block.TurbinePartBlock.Part.GEN_BREAKER_3000);
	// 8b: the turbine island (docs/multiblocks/turbine-island.md)
	Block TURBINE_VALVE_CHEST = islandPart(dev.wildcatter.block.TurbineIslandPartBlock.Part.TURBINE_VALVE_CHEST);
	Block HP_TURBINE = islandPart(dev.wildcatter.block.TurbineIslandPartBlock.Part.HP_TURBINE);
	Block MOISTURE_SEPARATOR_REHEATER = islandPart(dev.wildcatter.block.TurbineIslandPartBlock.Part.MOISTURE_SEPARATOR_REHEATER);
	Block LP_TURBINE = islandPart(dev.wildcatter.block.TurbineIslandPartBlock.Part.LP_TURBINE);
	Block MAIN_CONDENSER = islandPart(dev.wildcatter.block.TurbineIslandPartBlock.Part.MAIN_CONDENSER);
	Block CONDENSATE_PUMP = islandPart(dev.wildcatter.block.TurbineIslandPartBlock.Part.CONDENSATE_PUMP);
	Block DEAERATOR = islandPart(dev.wildcatter.block.TurbineIslandPartBlock.Part.DEAERATOR);
	Block MAIN_FEED_PUMP = islandPart(dev.wildcatter.block.TurbineIslandPartBlock.Part.MAIN_FEED_PUMP);
	Block HP_FEEDWATER_HEATER = islandPart(dev.wildcatter.block.TurbineIslandPartBlock.Part.HP_FEEDWATER_HEATER);
	Block CIRCULATING_WATER_PUMP = islandPart(dev.wildcatter.block.TurbineIslandPartBlock.Part.CIRCULATING_WATER_PUMP);
	// 8c: the natural-draft cooling tower (docs/multiblocks/natural-draft-tower.md)
	Block NATURAL_DRAFT_TOWER = block("natural_draft_tower", props -> new dev.wildcatter.block.NaturalDraftTowerBlock(props.mapColor(MapColor.STONE)
			.strength(5.0F, 12.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()));
	Block TOWER_SHELL = block("tower_shell", props -> new dev.wildcatter.block.TowerShellBlock(props.mapColor(MapColor.STONE)
			.strength(-1.0F, 3_600_000.0F).noLootTable().noOcclusion()));
	Block TG_GENERATOR_60 = turbinePart(dev.wildcatter.block.TurbinePartBlock.Part.TG_GENERATOR_60);
	/** Never placed: its states carry the models of the turning parts (kinetic/RotorBlock). */
	Block ROTOR = block("rotor", props -> new dev.wildcatter.kinetic.RotorBlock(props.noOcclusion().noLootTable()));
}
