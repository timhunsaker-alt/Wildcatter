package dev.wildcatter;

import dev.wildcatter.block.PlugAdapterBlock;
import dev.wildcatter.block.ReceptacleBlock;
import dev.wildcatter.energy.PlugType;
import dev.wildcatter.block.CableBlock;
import dev.wildcatter.circuit.Conductor;
import net.minecraft.world.level.material.MapColor;
import dev.wildcatter.block.MultiCableBlock;
import dev.wildcatter.energy.Voltage;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** The shared constructors behind the per-system block lists. */
final class BlockFactory {
	private BlockFactory() {
	}

	static Block receptacle(String name, PlugType type) {
		return block(name, props -> new ReceptacleBlock(props.mapColor(MapColor.COLOR_LIGHT_GRAY).strength(1.0F).sound(SoundType.METAL).noOcclusion(), type));
	}

	static Block plug(String name, PlugType type) {
		return block(name, props -> new PlugAdapterBlock(props.mapColor(MapColor.COLOR_BLACK).strength(1.0F).sound(SoundType.METAL).noOcclusion(), type));
	}

	static Block multiCable(String name, Voltage voltage) {
		return block(name, props -> new MultiCableBlock(props.mapColor(MapColor.COLOR_BLACK).strength(0.8F).sound(SoundType.WOOL).noOcclusion(), voltage, false));
	}

	static Block gensetPart(String name, dev.wildcatter.block.GensetPartBlock.Part part) {
		if (part == dev.wildcatter.block.GensetPartBlock.Part.ALTERNATOR) {   // drawn at its set's frame
			return block(name, props -> new dev.wildcatter.block.GensetPartBlock.Alternator(props.mapColor(MapColor.COLOR_GRAY)
					.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
		}
		return block(name, props -> new dev.wildcatter.block.GensetPartBlock(props.mapColor(MapColor.COLOR_GRAY)
				.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block turbinePart(dev.wildcatter.block.TurbinePartBlock.Part part) {
		return block(part.id(), props -> new dev.wildcatter.block.TurbinePartBlock(props.mapColor(MapColor.COLOR_GRAY)
				.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block refineryPart(dev.wildcatter.block.RefineryPartBlock.Part part) {
		return block(part.id(), props -> new dev.wildcatter.block.RefineryPartBlock(props.mapColor(MapColor.COLOR_GRAY)
				.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block petrochemPart(dev.wildcatter.block.PetrochemPartBlock.Part part) {
		return block(part.id(), props -> new dev.wildcatter.block.PetrochemPartBlock(props.mapColor(MapColor.COLOR_GRAY)
				.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block acidPart(dev.wildcatter.block.AcidPlantPartBlock.Part part) {
		return block(part.id(), props -> new dev.wildcatter.block.AcidPlantPartBlock(props.mapColor(MapColor.COLOR_GRAY)
				.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block conversionPart(dev.wildcatter.block.ConversionPartBlock.Part part) {
		return block(part.id(), props -> new dev.wildcatter.block.ConversionPartBlock(props.mapColor(MapColor.COLOR_GRAY)
				.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block handlingPart(dev.wildcatter.block.FuelHandlingPartBlock.Part part) {
		return block(part.id(), props -> new dev.wildcatter.block.FuelHandlingPartBlock(props.mapColor(MapColor.COLOR_LIGHT_GRAY)
				.strength(5.0F, 12.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block islandPart(dev.wildcatter.block.TurbineIslandPartBlock.Part part) {
		return block(part.id(), props -> new dev.wildcatter.block.TurbineIslandPartBlock(props.mapColor(MapColor.COLOR_LIGHT_GRAY)
				.strength(5.0F, 12.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block reactorPart(dev.wildcatter.block.ReactorPartBlock.Part part) {
		// the vessel and its head are thick forged steel: they stand through a hydrogen blast (8d)
		float blast = part == dev.wildcatter.block.ReactorPartBlock.Part.REACTOR_VESSEL || part == dev.wildcatter.block.ReactorPartBlock.Part.CRDM_HEAD ? 1200.0F : 12.0F;
		return block(part.id(), props -> new dev.wildcatter.block.ReactorPartBlock(props.mapColor(MapColor.COLOR_LIGHT_GRAY)
				.strength(5.0F, blast).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block boronPart(dev.wildcatter.block.BoronPartBlock.Part part) {
		return block(part.id(), props -> new dev.wildcatter.block.BoronPartBlock(props.mapColor(MapColor.COLOR_LIGHT_GRAY)
				.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block fuelPart(dev.wildcatter.block.FuelPartBlock.Part part) {
		return block(part.id(), props -> new dev.wildcatter.block.FuelPartBlock(props.mapColor(MapColor.COLOR_LIGHT_GRAY)
				.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block zirconiumPart(dev.wildcatter.block.ZirconiumPartBlock.Part part) {
		return block(part.id(), props -> new dev.wildcatter.block.ZirconiumPartBlock(props.mapColor(MapColor.COLOR_GRAY)
				.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block aluminaPart(dev.wildcatter.block.AluminaPartBlock.Part part) {
		return block(part.id(), props -> new dev.wildcatter.block.AluminaPartBlock(props.mapColor(MapColor.COLOR_LIGHT_GRAY)
				.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block mudPart(dev.wildcatter.block.MudPartBlock.Part part) {
		return block(part.id(), props -> new dev.wildcatter.block.MudPartBlock(props.mapColor(MapColor.COLOR_LIGHT_GRAY)
				.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block salesGasPart(dev.wildcatter.block.SalesGasPartBlock.Part part) {
		return block(part.id(), props -> new dev.wildcatter.block.SalesGasPartBlock(props.mapColor(MapColor.COLOR_LIGHT_GRAY)
				.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block ammoniaPart(dev.wildcatter.block.AmmoniaPartBlock.Part part) {
		return block(part.id(), props -> new dev.wildcatter.block.AmmoniaPartBlock(props.mapColor(MapColor.COLOR_LIGHT_GRAY)
				.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block carbonPart(dev.wildcatter.block.CarbonPartBlock.Part part) {
		return block(part.id(), props -> new dev.wildcatter.block.CarbonPartBlock(props.mapColor(MapColor.COLOR_GRAY)
				.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block potlinePart(dev.wildcatter.block.PotlinePartBlock.Part part) {
		return block(part.id(), props -> new dev.wildcatter.block.PotlinePartBlock(props.mapColor(MapColor.COLOR_GRAY)
				.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block cranePart(dev.wildcatter.block.CranePartBlock.Part part) {
		return block(part.id(), props -> new dev.wildcatter.block.CranePartBlock(props.mapColor(MapColor.COLOR_YELLOW)
				.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block enrichmentPart(dev.wildcatter.block.EnrichmentPartBlock.Part part) {
		return block(part.id(), props -> new dev.wildcatter.block.EnrichmentPartBlock(props.mapColor(MapColor.COLOR_LIGHT_GRAY)
				.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block wellfieldPart(dev.wildcatter.block.WellfieldPartBlock.Part part) {
		return block(part.id(), props -> new dev.wildcatter.block.WellfieldPartBlock(props.mapColor(MapColor.COLOR_GRAY)
				.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block millPart(dev.wildcatter.block.MillPartBlock.Part part) {
		return block(part.id(), props -> new dev.wildcatter.block.MillPartBlock(props.mapColor(MapColor.COLOR_GRAY)
				.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block gasPlantPart(dev.wildcatter.block.GasPlantPartBlock.Part part) {
		return block(part.id(), props -> new dev.wildcatter.block.GasPlantPartBlock(props.mapColor(MapColor.COLOR_GRAY)
				.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block leasePart(dev.wildcatter.block.LeasePartBlock.Part part) {
		return block(part.id(), props -> new dev.wildcatter.block.LeasePartBlock(props.mapColor(MapColor.COLOR_GRAY)
				.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block rigPart(dev.wildcatter.block.RigPartBlock.Part part) {
		return block(part.id(), props -> new dev.wildcatter.block.RigPartBlock(props.mapColor(MapColor.COLOR_YELLOW)
				.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block plantPart(String name, dev.wildcatter.block.SteamPlantPartBlock.Part part) {
		return block(name, props -> new dev.wildcatter.block.SteamPlantPartBlock(props.mapColor(MapColor.COLOR_GRAY)
				.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	static Block busway(String name, Voltage voltage) {
		return block(name, props -> new MultiCableBlock(props.mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops(), voltage, true));
	}

	static Block cable(String name, Voltage voltage, Conductor role, boolean wool) {
		return block(name, props -> new CableBlock(props.mapColor(role == Conductor.A ? MapColor.COLOR_RED : MapColor.COLOR_BLACK)
				.strength(0.5F).sound(wool ? SoundType.WOOL : SoundType.COPPER).noOcclusion(), voltage, role, wool));
	}

	static Block plasticCable(String name, Voltage voltage, Conductor role) {
		return block(name, props -> new CableBlock(props.mapColor(role == Conductor.A ? MapColor.COLOR_RED : MapColor.COLOR_BLACK)
				.strength(0.5F).sound(SoundType.COPPER).noOcclusion(), voltage, role, false, true));
	}

	static Block block(String name, Function<BlockBehaviour.Properties, Block> factory) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Wildcatter.id(name));
		return factory.apply(BlockBehaviour.Properties.of().setId(key));
	}

	static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> entity(
			BlockEntityType.BlockEntitySupplier<T> factory, Block... blocks) {
		return new BlockEntityType<>(factory, Set.of(blocks));
	}
}
