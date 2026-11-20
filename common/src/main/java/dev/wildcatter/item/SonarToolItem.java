package dev.wildcatter.item;

import dev.wildcatter.Tuning;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The sonar survey tool (docs/multiblocks/oilfield-follow-ons.md, 23c): used on a brine or storage wellhead, it lowers its sonde and
 * surveys the cavern for 30 s, then outlines it in particles and reads its volume and walls (WALL THIN under 3 blocks of salt).
 */
public class SonarToolItem extends Item {
	public SonarToolItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		BlockEntity entity = context.getLevel().getBlockEntity(context.getClickedPos());
		if (context.getLevel().isClientSide() || context.getPlayer() == null) {
			return entity instanceof dev.wildcatter.block.BrineWellheadBlockEntity || entity instanceof dev.wildcatter.block.StorageWellheadBlockEntity ? InteractionResult.SUCCESS : InteractionResult.PASS;
		}
		if (entity instanceof dev.wildcatter.block.BrineWellheadBlockEntity brine) {
			brine.survey(context.getPlayer());
		} else if (entity instanceof dev.wildcatter.block.StorageWellheadBlockEntity storage) {
			storage.survey(context.getPlayer());
		} else {
			return InteractionResult.PASS;
		}
		context.getPlayer().sendOverlayMessage(Component.literal("SONAR: the sonde is down; surveying the cavern for " + Tuning.SONAR_TICKS / 20 + " s"));
		return InteractionResult.SUCCESS;
	}
}
