package dev.wildcatter.item;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterComponents;
import dev.wildcatter.blueprint.Blueprint;
import dev.wildcatter.blueprint.BlueprintLibrary;
import dev.wildcatter.blueprint.Surveys;
import dev.wildcatter.block.SiteMarkerBlockEntity;
import dev.wildcatter.multiblock.Multiblock;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A blueprint (docs/blueprints.md): blank, a library plant, or a surveyed one carrying its data. Sneak-right-click in the air steps it
 * through the library; right-click a block sets its site marker on top, its origin there, turned the way you face. Held, the client
 * draws its ghost.
 */
public class BlueprintItem extends Item {
	public BlueprintItem(Properties properties) {
		super(properties);
	}

	/** "lib:<name>", a surveyed blueprint's compressed form, or "" when blank. */
	public static String data(ItemStack stack) {
		String data = stack.get(WildcatterComponents.BLUEPRINT);
		return data == null ? "" : data;
	}

	public static @Nullable Blueprint blueprint(ItemStack stack) {
		return BlueprintLibrary.of(data(stack));
	}

	public static void setData(ItemStack stack, String data) {
		stack.set(WildcatterComponents.BLUEPRINT, data);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}
		ItemStack stack = player.getItemInHand(hand);
		String data = data(stack);
		if (!data.isEmpty() && !data.startsWith("lib:")) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.literal("Blueprint: this one holds a survey; use a blank blueprint for the library"));
			}
			return InteractionResult.FAIL;
		}
		List<String> names = BlueprintLibrary.names();
		int at = data.isEmpty() ? -1 : names.indexOf(data.substring(4));
		String next = names.get((at + 1) % names.size());
		if (!level.isClientSide()) {
			setData(stack, "lib:" + next);
			player.sendOverlayMessage(Component.literal("Blueprint: " + BlueprintLibrary.title(next) + " (library)"));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null || player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}
		Level level = context.getLevel();
		ItemStack stack = context.getItemInHand();
		if (blueprint(stack) == null) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.literal("Blueprint: blank; survey a plant onto it, or sneak-right-click for the library"));
			}
			return InteractionResult.FAIL;
		}
		BlockPos origin = context.getClickedPos().relative(context.getClickedFace());
		if (!level.getBlockState(origin).canBeReplaced()) {
			return InteractionResult.FAIL;
		}
		if (!level.isClientSide()) {
			level.setBlock(origin, WildcatterBlocks.SITE_MARKER.defaultBlockState(), 3);
			if (level.getBlockEntity(origin) instanceof SiteMarkerBlockEntity site) {
				site.set(data(stack), turns(player));
				site.recount();
				player.sendOverlayMessage(site.inspect());
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** The quarter turns a blueprint takes from the way the player faces. */
	public static int turns(Player player) {
		return Multiblock.turns(player.getDirection());
	}

	@Override
	public Component getName(ItemStack stack) {
		String data = data(stack);
		if (data.startsWith("lib:")) {
			return Component.literal("Blueprint: " + BlueprintLibrary.title(data.substring(4)));
		}
		Blueprint b = data.isEmpty() ? null : blueprint(stack);
		return Component.literal(b == null ? "Blueprint (blank)" : "Blueprint: " + b.name());
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		Blueprint b = blueprint(stack);
		if (b == null) {
			tooltip.accept(Component.literal("Survey a plant onto it, or sneak-right-click for the library").withStyle(ChatFormatting.GRAY));
			return;
		}
		tooltip.accept(Component.literal(String.format("%d x %d x %d m: %d parts, %,d blocks", b.sizeX(), b.sizeY(), b.sizeZ(), b.parts().size(),
				b.blocks().size())).withStyle(ChatFormatting.GRAY));
		double[] parts = Surveys.partTonnes(b);
		int heaviest = -1;
		for (int i = 0; i < parts.length; i++) {
			if (heaviest < 0 || parts[i] > parts[heaviest]) {
				heaviest = i;
			}
		}
		if (heaviest >= 0) {
			var block = Surveys.part(b.parts().get(heaviest));
			tooltip.accept(Component.literal(String.format("Heaviest: %s, %.0f t", block == null ? b.parts().get(heaviest).id() : block.getName().getString(),
					parts[heaviest])).withStyle(ChatFormatting.GRAY));
		}
		tooltip.accept(Component.literal(Surveys.lifts(b).size() + " lifts").withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.literal("Right-click a block to set its site marker; it faces the way you face").withStyle(ChatFormatting.DARK_AQUA));
	}
}
