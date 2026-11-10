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
 * Zircon from outside Wildcatter's own mineral separator (Tim, 2026-11-10, answer 9: "Wildcatter's chlorinator takes Tailings' zircon
 * concentrate"), so Wildcatter depends on no other mod:
 * <ul>
 * <li>the item tag {@code #wildcatter:zircon_feed} ({@code data/wildcatter/tags/item/zircon_feed.json}) names what a chlorinator's hopper
 * takes as zircon (ZrSiO4), skipping the separator's ilmenite, rutile and monazite split;</li>
 * <li>a kg file for each, {@code data/<namespace>/wildcatter/zircon_feed/<any name>.json}:
 * {@code {"item": "tailings:zircon_concentrate", "zircon_kg": 56.25}}, gives its kg of ZrSiO4. An item in the tag with no kg file counts
 * as {@link Tuning#ZIRCON_FEED_ITEM_KG}.</li>
 * </ul>
 * The kg files are read from the server's data packs when first wanted, and again after a reload.
 */
public final class ZirconFeed {
	private ZirconFeed() {
	}

	public static final TagKey<Item> TAG = TagKey.create(net.minecraft.core.registries.Registries.ITEM, dev.wildcatter.Wildcatter.id("zircon_feed"));
	/** The folder of the kg files under each namespace's data. */
	public static final String FOLDER = "wildcatter/zircon_feed";

	private static ResourceManager readFrom;
	private static Map<Item, Double> kg = Map.of();

	/** True for an item a chlorinator takes as zircon. */
	public static boolean is(ItemStack stack) {
		return !stack.isEmpty() && stack.is(TAG);
	}

	/** kg of ZrSiO4 in one of this item, or 0 if it is not zircon feed. */
	public static double kg(MinecraftServer server, ItemStack stack) {
		if (!is(stack)) {
			return 0;
		}
		Double figure = table(server).get(stack.getItem());
		return figure != null ? figure : Tuning.ZIRCON_FEED_ITEM_KG;
	}

	private static synchronized Map<Item, Double> table(MinecraftServer server) {
		ResourceManager manager = server == null ? null : server.getResourceManager();
		if (manager == null || manager == readFrom) {
			return kg;
		}
		Map<Item, Double> out = new HashMap<>();
		manager.listResources(FOLDER, id -> id.getPath().endsWith(".json")).forEach((file, resource) -> {
			try (Reader reader = resource.openAsReader()) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				Identifier item = Identifier.tryParse(json.get("item").getAsString());
				double figure = json.get("zircon_kg").getAsDouble();
				if (item != null && figure > 0 && BuiltInRegistries.ITEM.containsKey(item)) {
					out.put(BuiltInRegistries.ITEM.getValue(item), figure);
				}
			} catch (Exception e) {
				dev.wildcatter.Wildcatter.LOGGER.warn("Zircon feed file {} not read: {}", file, e.toString());
			}
		});
		readFrom = manager;
		kg = out;
		return kg;
	}
}
