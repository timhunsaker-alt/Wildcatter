package dev.wildcatter;

import static dev.wildcatter.BlockFactory.*;

import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/** The nuclear island and the boron plant. Reached through {@link WildcatterBlocks}, which registers them. */
public interface NuclearBlocks {
	// 8a: the nuclear island (docs/multiblocks/reactor.md) and the boron plant (boron.md)
	Block REACTOR_VESSEL = reactorPart(dev.wildcatter.block.ReactorPartBlock.Part.REACTOR_VESSEL);
	Block CRDM_HEAD = reactorPart(dev.wildcatter.block.ReactorPartBlock.Part.CRDM_HEAD);
	Block REACTOR_CONSOLE = reactorPart(dev.wildcatter.block.ReactorPartBlock.Part.REACTOR_CONSOLE);
	Block STEAM_GENERATOR = reactorPart(dev.wildcatter.block.ReactorPartBlock.Part.STEAM_GENERATOR);
	Block REACTOR_COOLANT_PUMP = reactorPart(dev.wildcatter.block.ReactorPartBlock.Part.REACTOR_COOLANT_PUMP);
	Block PRESSURIZER = reactorPart(dev.wildcatter.block.ReactorPartBlock.Part.PRESSURIZER);
	Block CVCS_SKID = reactorPart(dev.wildcatter.block.ReactorPartBlock.Part.CVCS_SKID);
	Block AUX_FEED_PUMP = reactorPart(dev.wildcatter.block.ReactorPartBlock.Part.AUX_FEED_PUMP);
	Block REFUELLING_MACHINE = handlingPart(dev.wildcatter.block.FuelHandlingPartBlock.Part.REFUELLING_MACHINE);
	Block FUEL_TRANSFER_SYSTEM = handlingPart(dev.wildcatter.block.FuelHandlingPartBlock.Part.FUEL_TRANSFER_SYSTEM);
	Block SPENT_FUEL_POOL = handlingPart(dev.wildcatter.block.FuelHandlingPartBlock.Part.SPENT_FUEL_POOL);
	Block RHR_PUMP = reactorPart(dev.wildcatter.block.ReactorPartBlock.Part.RHR_PUMP);
	Block RHR_HEAT_EXCHANGER = reactorPart(dev.wildcatter.block.ReactorPartBlock.Part.RHR_HEAT_EXCHANGER);
	Block BORAX_DISSOLVER = boronPart(dev.wildcatter.block.BoronPartBlock.Part.BORAX_DISSOLVER);
	Block BORIC_ACID_CRYSTALLIZER = boronPart(dev.wildcatter.block.BoronPartBlock.Part.BORIC_ACID_CRYSTALLIZER);
	Block B4C_FURNACE = boronPart(dev.wildcatter.block.BoronPartBlock.Part.B4C_FURNACE);
	Block RCCA_BENCH = boronPart(dev.wildcatter.block.BoronPartBlock.Part.RCCA_BENCH);
	// 19a: containment and protection (docs/multiblocks/reactor-extensions.md)
	Block CONTAINMENT_CONCRETE = block("containment_concrete", props -> new Block(props.mapColor(MapColor.STONE).strength(50.0F, 1200.0F)
			.sound(SoundType.STONE).requiresCorrectToolForDrops()));
	Block HYDROGEN_IGNITER = block("hydrogen_igniter", props -> new dev.wildcatter.block.LoadBlock(props.mapColor(MapColor.METAL)
			.strength(1.0F).sound(SoundType.METAL).lightLevel(state -> state.getValue(dev.wildcatter.block.LoadBlock.LIT) ? 5 : 0), dev.wildcatter.Tuning.IGNITER_WATTS));
	Block EQUIPMENT_HATCH = containmentPart(dev.wildcatter.block.ContainmentPartBlock.Part.EQUIPMENT_HATCH);
	Block PERSONNEL_AIRLOCK = containmentPart(dev.wildcatter.block.ContainmentPartBlock.Part.PERSONNEL_AIRLOCK);
	Block HYDROGEN_RECOMBINER = containmentPart(dev.wildcatter.block.ContainmentPartBlock.Part.HYDROGEN_RECOMBINER);
	Block CONTAINMENT_MONITOR = containmentPart(dev.wildcatter.block.ContainmentPartBlock.Part.CONTAINMENT_MONITOR);
	Block PRESSURIZER_RELIEF_TANK = containmentPart(dev.wildcatter.block.ContainmentPartBlock.Part.PRESSURIZER_RELIEF_TANK);
	// 19b: dry casks, contamination, fresh-fuel inspection
	Block CASK_LOADING_STATION = fuelStoragePart(dev.wildcatter.block.FuelStoragePartBlock.Part.CASK_LOADING_STATION);
	Block STORAGE_MODULE = fuelStoragePart(dev.wildcatter.block.FuelStoragePartBlock.Part.STORAGE_MODULE);
	Block NEW_FUEL_INSPECTION_STAND = fuelStoragePart(dev.wildcatter.block.FuelStoragePartBlock.Part.NEW_FUEL_INSPECTION_STAND);
	Block PERSONNEL_MONITOR = fuelStoragePart(dev.wildcatter.block.FuelStoragePartBlock.Part.PERSONNEL_MONITOR);
	Block FUEL_ASSEMBLY = block("fuel_assembly", props -> new dev.wildcatter.block.FuelAssemblyBlock(props.mapColor(MapColor.METAL)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).noOcclusion()));
}
