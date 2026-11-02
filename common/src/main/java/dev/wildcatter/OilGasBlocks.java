package dev.wildcatter;

import static dev.wildcatter.BlockFactory.*;

import dev.wildcatter.block.CasingBlock;
import dev.wildcatter.block.DrillingRigBlock;
import dev.wildcatter.block.GasWellheadBlock;
import dev.wildcatter.block.PumpjackBlock;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/** Drilling, wells, the lease, the gas plant, the refinery and the petrochemical plant. Reached through {@link WildcatterBlocks}, which registers them. */
public interface OilGasBlocks {
	/** Sinks a cased hole straight down. On a strike it rigs down and leaves an oil wellhead or a gas wellhead. */
	Block DRILLING_RIG = block("drilling_rig", props -> new DrillingRigBlock(props.mapColor(MapColor.COLOR_YELLOW)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	/** Steel casing cemented in the hole: it cannot be broken (like bedrock), so a miner who meets one knows it is a well. */
	Block CASING = block("casing", props -> new CasingBlock(props.mapColor(MapColor.METAL)
			.strength(-1.0F, 3_600_000.0F).noLootTable().sound(SoundType.NETHERITE_BLOCK)));
	Block PUMPJACK = block("pumpjack", props -> new PumpjackBlock(props.mapColor(MapColor.COLOR_BLACK)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	Block GAS_WELLHEAD = block("gas_wellhead", props -> new GasWellheadBlock(props.mapColor(MapColor.COLOR_ORANGE)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	/** The rod-pumping wellhead a strike leaves on an oil well; a pumpjack sets its horsehead over it (docs/multiblocks/well-site.md). */
	Block OIL_WELLHEAD = block("oil_wellhead", props -> new dev.wildcatter.block.OilWellheadBlock(props.mapColor(MapColor.COLOR_GREEN)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	// the 1,000 hp land rig (docs/multiblocks/land-rig.md)
	Block RIG_SUBSTRUCTURE = rigPart(dev.wildcatter.block.RigPartBlock.Part.SUBSTRUCTURE);
	Block RIG_MAST = rigPart(dev.wildcatter.block.RigPartBlock.Part.MAST);
	Block RIG_DRAWWORKS = rigPart(dev.wildcatter.block.RigPartBlock.Part.DRAWWORKS);
	Block MUD_PUMP = rigPart(dev.wildcatter.block.RigPartBlock.Part.MUD_PUMP);
	Block MUD_TANKS = rigPart(dev.wildcatter.block.RigPartBlock.Part.MUD_TANKS);
	Block RIG_DRIVE_HOUSE = rigPart(dev.wildcatter.block.RigPartBlock.Part.DRIVE_HOUSE);
	/** The lease (docs/multiblocks/lease.md): separator, stock and water tanks, flare stack, injection pump, disposal wellhead. */
	Block SEPARATOR = leasePart(dev.wildcatter.block.LeasePartBlock.Part.SEPARATOR);
	Block STOCK_TANK = leasePart(dev.wildcatter.block.LeasePartBlock.Part.STOCK_TANK);
	Block WATER_TANK = leasePart(dev.wildcatter.block.LeasePartBlock.Part.WATER_TANK);
	Block FLARE_STACK = leasePart(dev.wildcatter.block.LeasePartBlock.Part.FLARE_STACK);
	Block INJECTION_PUMP = leasePart(dev.wildcatter.block.LeasePartBlock.Part.INJECTION_PUMP);
	Block DISPOSAL_WELLHEAD = block("disposal_wellhead", props -> new dev.wildcatter.block.DisposalWellheadBlock(props.mapColor(MapColor.COLOR_GREEN)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	/** The gas plant (docs/multiblocks/gas-plant.md). */
	Block GAS_COMPRESSOR = gasPlantPart(dev.wildcatter.block.GasPlantPartBlock.Part.GAS_COMPRESSOR);
	Block TEG_DEHYDRATOR = gasPlantPart(dev.wildcatter.block.GasPlantPartBlock.Part.TEG_DEHYDRATOR);
	Block NGL_CHILLER = gasPlantPart(dev.wildcatter.block.GasPlantPartBlock.Part.NGL_CHILLER);
	Block REFRIGERATION_COMPRESSOR = gasPlantPart(dev.wildcatter.block.GasPlantPartBlock.Part.REFRIGERATION_COMPRESSOR);
	Block STABILIZER = gasPlantPart(dev.wildcatter.block.GasPlantPartBlock.Part.STABILIZER);
	Block LPG_BULLET = gasPlantPart(dev.wildcatter.block.GasPlantPartBlock.Part.LPG_BULLET);
	/** The crude units (docs/multiblocks/crude-units.md). */
	Block TOPPING_COLUMN = refineryPart(dev.wildcatter.block.RefineryPartBlock.Part.TOPPING_COLUMN);
	Block TOPPING_HEATER = refineryPart(dev.wildcatter.block.RefineryPartBlock.Part.TOPPING_HEATER);
	Block TOPPING_SKID = refineryPart(dev.wildcatter.block.RefineryPartBlock.Part.TOPPING_SKID);
	Block ATMOSPHERIC_COLUMN = refineryPart(dev.wildcatter.block.RefineryPartBlock.Part.ATMOSPHERIC_COLUMN);
	Block CRUDE_HEATER = refineryPart(dev.wildcatter.block.RefineryPartBlock.Part.CRUDE_HEATER);
	Block DESALTER = refineryPart(dev.wildcatter.block.RefineryPartBlock.Part.DESALTER);
	Block CRUDE_CHARGE_PUMP = refineryPart(dev.wildcatter.block.RefineryPartBlock.Part.CRUDE_CHARGE_PUMP);
	Block SIDE_STRIPPERS = refineryPart(dev.wildcatter.block.RefineryPartBlock.Part.SIDE_STRIPPERS);
	Block OVERHEAD_CONDENSER = refineryPart(dev.wildcatter.block.RefineryPartBlock.Part.OVERHEAD_CONDENSER);
	/** The cracker and polyethylene (docs/multiblocks/cracker.md). */
	Block CRACKING_FURNACE = petrochemPart(dev.wildcatter.block.PetrochemPartBlock.Part.CRACKING_FURNACE);
	Block QUENCH_TOWER = petrochemPart(dev.wildcatter.block.PetrochemPartBlock.Part.QUENCH_TOWER);
	Block CRACKED_GAS_COMPRESSOR = petrochemPart(dev.wildcatter.block.PetrochemPartBlock.Part.CRACKED_GAS_COMPRESSOR);
	Block COLD_BOX = petrochemPart(dev.wildcatter.block.PetrochemPartBlock.Part.COLD_BOX);
	Block POLYETHYLENE_REACTOR = petrochemPart(dev.wildcatter.block.PetrochemPartBlock.Part.POLYETHYLENE_REACTOR);
	Block PELLETIZER = petrochemPart(dev.wildcatter.block.PetrochemPartBlock.Part.PELLETIZER);
}
