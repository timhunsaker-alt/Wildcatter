package dev.wildcatter.block;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wildcatter.Tuning;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Flotation concentrate as items for the moly roaster's hopper (docs/multiblocks/ferroalloys.md, "Concentrate by hopper"), as
 * {@link ZirconFeed} is for the chlorinator, so Wildcatter depends on no other mod: an item tag names what counts as molybdenite or
 * scheelite concentrate ({@code #wildcatter:molybdenite_feed}, {@code #wildcatter:scheelite_feed}), and a kg file for each,
 * {@code data/<namespace>/wildcatter/<tag name>/<any name>.json}: {@code {"item": "tailings:scheelite_concentrate", "kg": 47.125}},
 * gives the kg of the mineral in one. An item in the tag with no kg file counts as the feed's default. The files are read from the
 * server's data packs when first wanted, and again after a reload.
 */
public final class ConcentrateFeed {
	public static final ConcentrateFeed MOLYBDENITE = new ConcentrateFeed("molybdenite_feed", Tuning.MOLYBDENITE_FEED_ITEM_KG);
	public static final ConcentrateFeed SCHEELITE = new ConcentrateFeed("scheelite_feed", Tuning.SCHEELITE_FEED_ITEM_KG);

	private final TagKey<Item> tag;
	private final String folder;
	private final double defaultKg;
	private ResourceManager readFrom;
	private Map<Item, Double> kg = Map.of();

	private ConcentrateFeed(String name, double defaultKg) {
		this.tag = TagKey.create(net.minecraft.core.registries.Registries.ITEM, dev.wildcatter.Wildcatter.id(name));
		this.folder = "wildcatter/" + name;
		this.defaultKg = defaultKg;
	}

	public boolean is(ItemStack stack) {
		return !stack.isEmpty() && stack.is(tag);
	}

	/** kg of the mineral in one of this item, or 0 if it is not this feed. */
	public double kg(MinecraftServer server, ItemStack stack) {
		if (!is(stack)) {
			return 0;
		}
		Double figure = table(server).get(stack.getItem());
		return figure != null ? figure : defaultKg;
	}

	private synchronized Map<Item, Double> table(MinecraftServer server) {
		ResourceManager manager = server == null ? null : server.getResourceManager();
		if (manager == null || manager == readFrom) {
			return kg;
		}
		Map<Item, Double> out = new HashMap<>();
		manager.listResources(folder, id -> id.getPath().endsWith(".json")).forEach((file, resource) -> {
			try (Reader reader = resource.openAsReader()) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				Identifier item = Identifier.tryParse(json.get("item").getAsString());
				double figure = json.get("kg").getAsDouble();
				if (item != null && figure > 0 && BuiltInRegistries.ITEM.containsKey(item)) {
					out.put(BuiltInRegistries.ITEM.getValue(item), figure);
				}
			} catch (Exception e) {
				dev.wildcatter.Wildcatter.LOGGER.warn("Concentrate feed file {} not read: {}", file, e.toString());
			}
		});
		readFrom = manager;
		kg = out;
		return kg;
	}
}
