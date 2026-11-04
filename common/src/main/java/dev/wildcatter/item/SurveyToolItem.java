package dev.wildcatter.item;

import dev.wildcatter.WildcatterComponents;
import dev.wildcatter.blueprint.Blueprint;
import dev.wildcatter.blueprint.Surveys;
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

/**
 * The survey tool (docs/blueprints.md): right-click two corners of a plant, then right-click again with a blueprint in the off hand to
 * record everything between them onto it. Sneak-right-click clears the corners.
 */
public class SurveyToolItem extends Item {
	public SurveyToolItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		Level level = context.getLevel();
		if (player == null) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		ItemStack tool = context.getItemInHand();
		BlockPos clicked = context.getClickedPos();
		if (player.isShiftKeyDown()) {
			tool.remove(WildcatterComponents.SURVEY_A);
			tool.remove(WildcatterComponents.SURVEY_B);
			player.sendOverlayMessage(Component.literal("Survey tool: corners cleared"));
			return InteractionResult.SUCCESS;
		}
		BlockPos a = tool.get(WildcatterComponents.SURVEY_A);
		BlockPos b = tool.get(WildcatterComponents.SURVEY_B);
		if (a == null) {
			tool.set(WildcatterComponents.SURVEY_A, clicked.immutable());
			player.sendOverlayMessage(Component.literal(String.format("Survey tool: first corner at %d, %d, %d; now the second", clicked.getX(), clicked.getY(), clicked.getZ())));
			return InteractionResult.SUCCESS;
		}
		if (b == null) {
			int sx = Math.abs(clicked.getX() - a.getX()) + 1, sy = Math.abs(clicked.getY() - a.getY()) + 1, sz = Math.abs(clicked.getZ() - a.getZ()) + 1;
			if (sx > Surveys.MAX_X || sy > Surveys.MAX_Y || sz > Surveys.MAX_Z) {
				player.sendOverlayMessage(Component.literal(String.format("Survey tool: TOO BIG (%d x %d x %d; at most %d x %d x %d)", sx, sy, sz, Surveys.MAX_X,
						Surveys.MAX_Y, Surveys.MAX_Z)));
				return InteractionResult.FAIL;
			}
			tool.set(WildcatterComponents.SURVEY_B, clicked.immutable());
			player.sendOverlayMessage(Component.literal(String.format("Survey tool: box %d x %d x %d; hold a blueprint in your off hand and right-click to record", sx, sy, sz)));
			return InteractionResult.SUCCESS;
		}
		ItemStack paper = player.getItemInHand(InteractionHand.OFF_HAND);
		if (!(paper.getItem() instanceof BlueprintItem)) {
			player.sendOverlayMessage(Component.literal("Survey tool: hold a blueprint in your off hand to record onto"));
			return InteractionResult.FAIL;
		}
		Blueprint recorded = record(level, a, b);
		BlueprintItem.setData(paper, recorded.compressed());
		player.sendOverlayMessage(Component.literal(String.format("Survey tool: recorded %s: %d parts, %,d blocks", recorded.name(), recorded.parts().size(), recorded.blocks().size())));
		return InteractionResult.SUCCESS;
	}

	/** Records the box onto a new blueprint named for its size. */
	public static Blueprint record(Level level, BlockPos a, BlockPos b) {
		int sx = Math.abs(b.getX() - a.getX()) + 1, sy = Math.abs(b.getY() - a.getY()) + 1, sz = Math.abs(b.getZ() - a.getZ()) + 1;
		return Surveys.record(level, String.format("Surveyed %d x %d x %d", sx, sy, sz), a, b);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		BlockPos a = stack.get(WildcatterComponents.SURVEY_A);
		BlockPos b = stack.get(WildcatterComponents.SURVEY_B);
		tooltip.accept(Component.literal(a == null ? "Right-click the first corner" : b == null ? "Right-click the second corner"
				: "Right-click again with a blueprint in the off hand").withStyle(ChatFormatting.GRAY));
	}
}
