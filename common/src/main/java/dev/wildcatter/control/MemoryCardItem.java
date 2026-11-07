package dev.wildcatter.control;

import dev.wildcatter.plc.lang.Project;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;

/** A PLC's memory card (docs/multiblocks/plc.md): holds a project, to copy it between cabinets. Seated in a cabinet's door. */
public class MemoryCardItem extends Item {
	/** What it holds at most: the bound is what an item's data can carry to every player who sees it, not what a real card holds. */
	public static final int CAPACITY = 256_000;
	/** A saved string's limit is 65,535 bytes, so the text goes in pieces. */
	private static final int PIECE = 20_000;

	public MemoryCardItem(Properties properties) {
		super(properties);
	}

	/** The project on the card; null if it is blank or damaged. */
	public static List<Project.Source> read(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return null;
		}
		ListTag pieces = data.copyTag().getListOrEmpty("project");
		if (pieces.isEmpty()) {
			return null;
		}
		StringBuilder packed = new StringBuilder();
		for (int i = 0; i < pieces.size(); i++) {
			packed.append(pieces.getStringOr(i, ""));
		}
		return Project.unpack(packed.toString());
	}

	/** Writes the project; why not, or null. */
	public static String write(ItemStack stack, List<Project.Source> project) {
		String packed = Project.pack(project);
		if (packed.length() > CAPACITY) {
			return "The project is " + packed.length() + " characters, and a card holds " + CAPACITY;
		}
		ListTag pieces = new ListTag();
		for (int at = 0; at < packed.length(); at += PIECE) {
			pieces.add(StringTag.valueOf(packed.substring(at, Math.min(packed.length(), at + PIECE))));
		}
		CompoundTag tag = new CompoundTag();
		tag.put("project", pieces);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		return null;
	}

	public static String describe(ItemStack stack) {
		List<Project.Source> project = read(stack);
		if (project == null) {
			return "blank";
		}
		List<String> names = new ArrayList<>();
		project.forEach(unit -> names.add(unit.name()));
		return project.size() + (project.size() == 1 ? " unit: " : " units: ") + String.join(", ", names);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.literal(describe(stack)));
	}
}
