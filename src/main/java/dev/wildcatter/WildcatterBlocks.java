package dev.wildcatter;

import dev.wildcatter.block.BoilerBlock;
import dev.wildcatter.block.BoilerBlockEntity;
import dev.wildcatter.block.CableBlock;
import dev.wildcatter.block.CableBlockEntity;
import dev.wildcatter.block.GroundRodBlock;
import dev.wildcatter.block.GroundWireBlock;
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
	// Cables come bare (cheap, shock you when live) or insulated (safe to touch). Higher voltage reaches further, so
	// bare lines at 4.16 kV and 138 kV have to be up on tall poles.
	public static final Block COPPER_CABLE = cable("copper_cable", Voltage.LOW, false);
	public static final Block INSULATED_CABLE = cable("insulated_cable", Voltage.LOW, true);
	public static final Block MEDIUM_LINE = cable("medium_line", Voltage.MEDIUM, false);
	public static final Block INSULATED_MEDIUM_CABLE = cable("insulated_medium_cable", Voltage.MEDIUM, true);
	public static final Block HIGH_LINE = cable("high_line", Voltage.HIGH, false);
	public static final Block INSULATED_HIGH_CABLE = cable("insulated_high_cable", Voltage.HIGH, true);

	/** A copper rod in the earth, for grounding machines. */
	public static final Block GROUND_ROD = block("ground_rod", props -> new GroundRodBlock(props.mapColor(net.minecraft.world.level.material.MapColor.COLOR_ORANGE)
			.strength(1.0F).sound(SoundType.COPPER).noOcclusion()));
	public static final Block GROUND_WIRE = block("ground_wire", props -> new GroundWireBlock(props.mapColor(net.minecraft.world.level.material.MapColor.COLOR_GREEN)
			.strength(0.3F).sound(SoundType.COPPER).noOcclusion()));

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
	public static final BlockEntityType<CableBlockEntity> CABLE_ENTITY = entity(CableBlockEntity::new, COPPER_CABLE, INSULATED_CABLE, MEDIUM_LINE, INSULATED_MEDIUM_CABLE, HIGH_LINE, INSULATED_HIGH_CABLE);
	public static final BlockEntityType<TransformerBlockEntity> TRANSFORMER_ENTITY = entity(TransformerBlockEntity::new, TRANSFORMER, SUBSTATION_TRANSFORMER);
	public static final BlockEntityType<LampBlockEntity> LAMP_ENTITY = entity(LampBlockEntity::new, ELECTRIC_LAMP);

	private WildcatterBlocks() {
	}

	private static Block cable(String name, Voltage voltage, boolean insulated) {
		return block(name, props -> new CableBlock(props.mapColor(net.minecraft.world.level.material.MapColor.COLOR_ORANGE)
				.strength(0.5F).sound(SoundType.COPPER).noOcclusion(), voltage, insulated));
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
		add("insulated_cable", INSULATED_CABLE);
		add("medium_line", MEDIUM_LINE);
		add("insulated_medium_cable", INSULATED_MEDIUM_CABLE);
		add("high_line", HIGH_LINE);
		add("insulated_high_cable", INSULATED_HIGH_CABLE);
		add("ground_rod", GROUND_ROD);
		add("ground_wire", GROUND_WIRE);
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
