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
		// 17c1: the zirconium line's scrap and hafnium (docs/multiblocks/zirconium-extensions.md)
		info(registration, WildcatterItems.ZIRCALOY_SCRAP, "Crop ends, turnings and rejected tubes from the extrusion press and pilger mill (the rule zirconiumBatches), "
				+ "binned by the pilger mill into a hopper under it. The VAR furnace takes it by hopper, up to 30 % of an ingot, saving sponge.");
		info(registration, WildcatterItems.HFCL4_DRUM, "The hafnium the hafnium column takes out of the zirconium, 50 kg of Hf a drum, into a hopper under the column. "
				+ "A Kroll reactor on a hafnium campaign (sneak-click it) takes it by a hopper on top.");
		info(registration, WildcatterItems.HAFNIUM_RODLET, "Solid hafnium, 9.7 mm across and 3.66 m long, rolled by the pilger mill's rodlet dies on a hafnium campaign. "
				+ "24 make a hafnium RCCA at the RCCA bench.");
		// 17c2: fumed silica, Pidgeon magnesium, chromium (docs/multiblocks/zirconium-extensions.md)
		info(registration, WildcatterItems.FUMED_SILICA, "SiCl4 from the chlorinator burned in a hydrogen-oxygen flame by the fumed silica burner. No use in Wildcatter; "
				+ "tagged c:fumed_silica for other mods.");
		info(registration, WildcatterItems.DOLIME_BAG, "Calcined dolomite from a lime kiln fed dolomite, bagged by a bagging station against it. For the Pidgeon furnace.");
		info(registration, WildcatterItems.MAGNESIUM_INGOT, "Pidgeon magnesium, cast from the furnace's crowns at the end of each cycle. A hopper on the magnesium cell takes it as "
				+ "make-up for the Kroll loop.");
		info(registration, WildcatterItems.CHROMIC_OXIDE, "From the chromate kiln: chromite roasted with caustic soda, leached and reduced with sulfur. Four sacks and two "
				+ "aluminium ingots fire in the aluminothermic vessel to chromium metal.");
		info(registration, WildcatterItems.CHROMIUM_METAL, "Aluminothermic chromium. The VAR furnace takes it by hopper in place of chromite for Zircaloy-4's 0.10 % Cr.");
		// 19d: rod materials and the sodium sulfate (docs/multiblocks/reactor-extensions.md)
		info(registration, WildcatterItems.HAFNIUM_RCCA, "24 hafnium rodlets on a spider, from the RCCA bench (the rodlets in its tray hopper, with steel). "
				+ "85 % of a B4C RCCA's worth; it takes no helium and does not wear. Load it into the cold vessel as a B4C RCCA.");
		info(registration, WildcatterItems.SODIUM_SULFATE_BAG, "Salt cake from the borax dissolver, bagged by a bagging station set against it. "
				+ "Wildcatter has no use for it (tagged c:sodium_sulfate for other mods): dump it in a lined tailings cell through its decant tower.");
	}

	private static void info(IRecipeRegistration registration, net.minecraft.world.item.Item item, String text) {
		registration.addItemStackInfo(new ItemStack(item), Component.literal(text));
	}

	private static void ingot(IRecipeRegistration registration, String metal, String text) {
		registration.addItemStackInfo(new ItemStack(OreBlocks.INGOTS.get(metal)), Component.literal(text));
	}
}
