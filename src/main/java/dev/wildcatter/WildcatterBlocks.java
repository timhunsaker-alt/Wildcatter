package dev.wildcatter;

import dev.wildcatter.block.BoilerBlock;
import dev.wildcatter.block.BoilerBlockEntity;
import dev.wildcatter.block.CableBlock;
import dev.wildcatter.block.CableBlockEntity;
import dev.wildcatter.block.LampBlock;
import dev.wildcatter.block.LampBlockEntity;
import dev.wildcatter.block.SteamTurbineBlock;
import dev.wildcatter.block.TransformerBlock;
import dev.wildcatter.block.TransformerBlockEntity;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.block.SteamTurbineBlockEntity;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Every block, its item, and its block entity. */
public final class WildcatterBlocks {
	public static final Block BOILER = block("boiler", props -> new BoilerBlock(props.mapColor(net.minecraft.world.level.material.MapColor.METAL)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	public static final Block STEAM_TURBINE = block("steam_turbine", props -> new SteamTurbineBlock(props.mapColor(net.minecraft.world.level.material.MapColor.METAL)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	public static final Block COPPER_CABLE = block("copper_cable", props -> new CableBlock(props.mapColor(net.minecraft.world.level.material.MapColor.COLOR_ORANGE)
			.strength(0.5F).sound(SoundType.COPPER).noOcclusion(), Voltage.LOW));
	/** Insulated line for medium voltage (4.16 kV). */
	public static final Block MEDIUM_CABLE = block("medium_cable", props -> new CableBlock(props.mapColor(net.minecraft.world.level.material.MapColor.COLOR_GRAY)
			.strength(0.8F).sound(SoundType.COPPER).noOcclusion(), Voltage.MEDIUM));
	/** Steps 110 V up to 4.16 kV and back. */
	public static final Block TRANSFORMER = block("transformer", props -> new TransformerBlock(props.mapColor(net.minecraft.world.level.material.MapColor.COLOR_GRAY)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.LOW, Voltage.MEDIUM));
	/** Steps 4.16 kV up to 138 kV and back. */
	public static final Block SUBSTATION_TRANSFORMER = block("substation_transformer", props -> new TransformerBlock(props.mapColor(net.minecraft.world.level.material.MapColor.COLOR_BLUE)
			.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.MEDIUM, Voltage.HIGH));
	public static final Block ELECTRIC_LAMP = block("electric_lamp", props -> new LampBlock(props.mapColor(net.minecraft.world.level.material.MapColor.COLOR_YELLOW)
			.strength(0.5F).sound(SoundType.GLASS).lightLevel(state -> state.getValue(LampBlock.LIT) ? 15 : 0)));

	public static final BlockEntityType<BoilerBlockEntity> BOILER_ENTITY = entity(BoilerBlockEntity::new, BOILER);
	public static final BlockEntityType<SteamTurbineBlockEntity> STEAM_TURBINE_ENTITY = entity(SteamTurbineBlockEntity::new, STEAM_TURBINE);
	public static final BlockEntityType<CableBlockEntity> CABLE_ENTITY = entity(CableBlockEntity::new, COPPER_CABLE, MEDIUM_CABLE);
	public static final BlockEntityType<TransformerBlockEntity> TRANSFORMER_ENTITY = entity(TransformerBlockEntity::new, TRANSFORMER, SUBSTATION_TRANSFORMER);
	public static final BlockEntityType<LampBlockEntity> LAMP_ENTITY = entity(LampBlockEntity::new, ELECTRIC_LAMP);

	private WildcatterBlocks() {
	}

	private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Wildcatter.id(name));
		return factory.apply(BlockBehaviour.Properties.of().setId(key));
	}

	private static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> entity(
			BlockEntityType.BlockEntitySupplier<T> factory, Block... blocks) {
		return new BlockEntityType<>(factory, Set.of(blocks));
	}

	public static void register() {
		add("boiler", BOILER);
		add("steam_turbine", STEAM_TURBINE);
		add("copper_cable", COPPER_CABLE);
		add("medium_cable", MEDIUM_CABLE);
		add("transformer", TRANSFORMER);
		add("substation_transformer", SUBSTATION_TRANSFORMER);
		add("electric_lamp", ELECTRIC_LAMP);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("boiler"), BOILER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("steam_turbine"), STEAM_TURBINE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("copper_cable"), CABLE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("transformer"), TRANSFORMER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("electric_lamp"), LAMP_ENTITY);
	}

	/** Registers the block and its item. */
	private static void add(String name, Block block) {
		Registry.register(BuiltInRegistries.BLOCK, Wildcatter.id(name), block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Wildcatter.id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
	}
}
