package dev.wildcatter.compat;

import dev.wildcatter.Wildcatter;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import dev.wildcatter.ore.OreUses;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * JEI information pages (the U key) for the ores, their minerals, the furnace ingots and magnesia: what each is, where it is
 * found and which plant takes it (OreUses). JEI is optional: NeoForge finds this by @JeiPlugin, Fabric by the jei_mod_plugin
 * entrypoint, and without JEI the class is never loaded. It uses only JEI's loader-neutral API.
 */
@JeiPlugin
public class WildcatterJeiPlugin implements IModPlugin {
	@Override
	public Identifier getPluginUid() {
		return Wildcatter.id("jei_plugin");
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		for (Ore ore : Ore.values()) {
			List<Component> page = new ArrayList<>();
			page.add(Component.literal(ore.mineralName + " (" + ore.formula + "), in " + ore.blockName + ": " + ore.gradeText() + "."));
			page.add(Component.literal("Real ore: " + ore.realGrade + "."));
			page.add(Component.literal("Found: " + OreUses.found(ore) + ". Mined with " + OreUses.tool(ore) + "."));
			List<String> uses = OreUses.uses(ore);
			if (uses.isEmpty()) {
				page.add(Component.literal("No use yet."));
			}
			for (String use : uses) {
				page.add(Component.literal("Used in " + use + "."));
			}
			registration.addItemStackInfo(List.of(new ItemStack(OreBlocks.block(ore)), new ItemStack(OreBlocks.MINERALS.get(ore))),
					page.toArray(Component[]::new));
		}
		ingot(registration, "tin", "Smelted from cassiterite. The zirconium plant's VAR furnace alloys it into Zircaloy.");
		ingot(registration, "lead", "Smelted from galena. No use yet.");
		ingot(registration, "zinc", "Smelted from sphalerite. No use yet.");
		ingot(registration, "silver", "Smelted from acanthite. No use yet.");
		registration.addItemStackInfo(new ItemStack(WildcatterItems.MAGNESIA), Component.literal("Caustic-calcined magnesia (MgO), "
				+ "from magnesite in a furnace. The uranium mill's precipitation tank uses it to precipitate the uranium, and the "
				+ "magnesia chlorinator turns it into MgCl2 for the magnesium cell."));
	}

	private static void ingot(IRecipeRegistration registration, String metal, String text) {
		registration.addItemStackInfo(new ItemStack(OreBlocks.INGOTS.get(metal)), Component.literal(text));
	}
}
