package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * {@code recipes_unique}: no two crafting recipes give different things for the same grid. For every Wildcatter shaped and shapeless
 * recipe it lays the grids the recipe accepts (every key on its first item, then each key through every item its tag or list allows,
 * the others held), and asks every crafting recipe loaded (Wildcatter's and the game's) whether it matches. A shaped recipe matches its
 * mirror image too, so a mirrored grid is caught; a tag and an item in it overlap because the grid holds the item. Any recipe that
 * matches and gives something else is a collision: the table would craft only one of them (Built From Scratch ep 3: the stock tank's
 * grid gave a dry storage canister).
 */
final class RecipeScenes {
	private RecipeScenes() {
	}

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> out = new LinkedHashMap<>();
		out.put("recipes_unique", RecipeScenes::unique);
		return out;
	}

	/** The grids a recipe accepts, each as nine cells of a 3 x 3 table (row by row, empty where nothing goes). */
	private static List<List<ItemStack>> grids(List<Optional<Ingredient>> slots, int width) {
		List<Ingredient> distinct = new ArrayList<>();
		for (Optional<Ingredient> s : slots) {
			if (s.isPresent() && !distinct.contains(s.get())) {
				distinct.add(s.get());
			}
		}
		List<List<Holder<Item>>> choices = new ArrayList<>();
		for (Ingredient ing : distinct) {
			List<Holder<Item>> items = ing.items().toList();
			if (items.isEmpty()) {
				return List.of();   // an empty tag: nothing can be laid, nothing can collide
			}
			choices.add(items);
		}
		List<int[]> picks = new ArrayList<>();
		picks.add(new int[distinct.size()]);
		for (int k = 0; k < distinct.size(); k++) {
			for (int i = 1; i < choices.get(k).size(); i++) {
				int[] p = new int[distinct.size()];
				p[k] = i;
				picks.add(p);
			}
		}
		List<List<ItemStack>> out = new ArrayList<>();
		for (int[] p : picks) {
			List<ItemStack> cells = new ArrayList<>();
			for (int i = 0; i < 9; i++) {
				cells.add(ItemStack.EMPTY);
			}
			for (int i = 0; i < slots.size(); i++) {
				Optional<Ingredient> s = slots.get(i);
				if (s.isPresent()) {
					int k = distinct.indexOf(s.get());
					cells.set((i / width) * 3 + i % width, new ItemStack(choices.get(k).get(p[k])));
				}
			}
			out.add(cells);
		}
		return out;
	}

	private static String show(List<ItemStack> cells) {
		StringBuilder b = new StringBuilder("[");
		for (int i = 0; i < 9; i++) {
			b.append(i % 3 == 0 && i > 0 ? " / " : i > 0 ? ", " : "");
			b.append(cells.get(i).isEmpty() ? "-" : cells.get(i).getItem().toString());
		}
		return b.append(']').toString();
	}

	static void unique(GameTestHelper helper) {
		var server = helper.getLevel().getServer();
		List<RecipeHolder<CraftingRecipe>> crafting = new ArrayList<>();
		for (RecipeHolder<?> h : server.getRecipeManager().getRecipes()) {
			if (h.value() instanceof CraftingRecipe) {
				@SuppressWarnings("unchecked")
				RecipeHolder<CraftingRecipe> c = (RecipeHolder<CraftingRecipe>) h;
				crafting.add(c);
			}
		}
		Set<String> collisions = new LinkedHashSet<>();
		int checked = 0, grids = 0;
		for (RecipeHolder<CraftingRecipe> a : crafting) {
			if (!a.id().identifier().getNamespace().equals("wildcatter")) {
				continue;
			}
			List<List<ItemStack>> laid;
			if (a.value() instanceof ShapedRecipe shaped) {
				laid = grids(shaped.pattern.ingredients(), shaped.getWidth());
			} else if (a.value() instanceof ShapelessRecipe shapeless) {
				laid = grids(shapeless.placementInfo().ingredients().stream().map(Optional::of).toList(), 3);
			} else {
				continue;
			}
			checked++;
			for (List<ItemStack> cells : laid) {
				grids++;
				CraftingInput input = CraftingInput.ofPositioned(3, 3, cells).input();
				if (!a.value().matches(input, helper.getLevel())) {
					continue;   // a grid the recipe does not take (a shapeless recipe of more than nine): nothing to compare
				}
				ItemStack mine = a.value().assemble(input);
				for (RecipeHolder<CraftingRecipe> b : crafting) {
					if (b == a || !b.value().matches(input, helper.getLevel())) {
						continue;
					}
					ItemStack theirs = b.value().assemble(input);
					if (!ItemStack.matches(mine, theirs)) {
						String x = a.id().identifier().toString(), y = b.id().identifier().toString();
						String pair = x.compareTo(y) < 0 ? x + " / " + y : y + " / " + x;
						if (collisions.add(pair)) {
							log("recipes_unique", "collision: " + pair + " on " + show(cells));
						}
					}
				}
			}
		}
		log("recipes_unique", checked + " Wildcatter crafting recipes, " + grids + " grids against " + crafting.size() + " crafting recipes, "
				+ collisions.size() + " collisions");
		if (!collisions.isEmpty()) {
			helper.fail(Component.literal(collisions.size() + " grids craft two different things: " + String.join("; ", collisions)));
			return;
		}
		helper.succeed();
	}
}
