package dev.wildcatter.ore;

import dev.wildcatter.Wildcatter;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** The ore blocks, their mineral items and the four ingots the furnace makes from them (docs/ores.md). */
public final class OreBlocks {
	public static final Map<Ore, Block> BLOCKS = new EnumMap<>(Ore.class);
	public static final Map<Ore, Item> MINERALS = new EnumMap<>(Ore.class);
	/** Galena, cassiterite, sphalerite and acanthite smelt in a furnace, as they really smelt simply. */
	public static final Map<String, Item> INGOTS = new LinkedHashMap<>();

	private OreBlocks() {
	}

	public static Block block(Ore ore) {
		return BLOCKS.get(ore);
	}

	public static void register() {
		for (Ore ore : Ore.values()) {
			ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Wildcatter.id(ore.blockId()));
			boolean deep = ore.where == Ore.Where.DEEP || ore == Ore.PENTLANDITE || ore == Ore.CHROMITE || ore == Ore.PLATINUM;
			BlockBehaviour.Properties props = BlockBehaviour.Properties.of().setId(key);
			props = switch (ore.tool) {
				case SHOVEL -> props.strength(0.6F).sound(ore == Ore.HEAVY_MINERAL ? SoundType.SAND : SoundType.GRAVEL);
				default -> props.strength(deep ? 4.5F : 3.0F, 3.0F).requiresCorrectToolForDrops().sound(deep ? SoundType.DEEPSLATE : SoundType.STONE);
			};
			Block block = new Block(props);
			Registry.register(BuiltInRegistries.BLOCK, key, block);
			ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Wildcatter.id(ore.blockId()));
			Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
			BLOCKS.put(ore, block);

			ResourceKey<Item> mineralKey = ResourceKey.create(Registries.ITEM, Wildcatter.id(ore.key));
			Item mineral = new MineralItem(new Item.Properties().setId(mineralKey), ore);
			Registry.register(BuiltInRegistries.ITEM, mineralKey, mineral);
			MINERALS.put(ore, mineral);
		}
		for (String metal : new String[] {"lead", "tin", "zinc", "silver"}) {
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Wildcatter.id(metal + "_ingot"));
			Item ingot = new Item(new Item.Properties().setId(key));
			Registry.register(BuiltInRegistries.ITEM, key, ingot);
			INGOTS.put(metal, ingot);
		}
	}
}
