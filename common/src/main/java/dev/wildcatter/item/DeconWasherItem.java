package dev.wildcatter.item;

import dev.wildcatter.Tuning;
import dev.wildcatter.block.ReceptacleBlockEntity;
import dev.wildcatter.fluid.Radiation;
import dev.wildcatter.radiation.ContaminationData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A high-pressure decontamination washer (docs/multiblocks/reactor-extensions.md, 19b): its 2 kW pump runs plugged into a live
 * receptacle within its hose's reach, on water from a bucket you carry; a right-click cleans 90 % of a block's contamination.
 */
public class DeconWasherItem extends Item {
	public DeconWasherItem(Properties properties) {
		super(properties);
	}

	/** A live receptacle within reach of {@code pos}. */
	public static boolean plugged(Level level, BlockPos pos) {
		int r = (int) Math.ceil(Tuning.WASHER_REACH);
		for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, -r, -r), pos.offset(r, r, r))) {
			if (level.getBlockEntity(p) instanceof ReceptacleBlockEntity rec && rec.volts() > 0.75 * dev.wildcatter.energy.Voltage.LOW.nominal()) {
				return true;
			}
		}
		return false;
	}

	/** Washes the block at {@code pos} for {@code player}; returns what to tell them. */
	public static String wash(ServerLevel level, BlockPos pos, Player player) {
		if (!dev.wildcatter.Rules.reactorContamination()) {
			return "Washer: nothing to wash (no contamination is kept: the contamination rule is off)";
		}
		if (!plugged(level, pos)) {
			return String.format("Washer: plug it in: no live receptacle within %.0f m", Tuning.WASHER_REACH);
		}
		if (!player.getInventory().contains(new net.minecraft.world.item.ItemStack(Items.WATER_BUCKET))) {
			return "Washer: no water: carry a water bucket";
		}
		ContaminationData data = ContaminationData.get(level);
		long now = level.getGameTime();
		double before = data.at(pos, now);
		data.set(pos, before * (1 - Tuning.WASHER_CLEANS), now);
		double after = data.at(pos, now);
		return String.format("Washer: %s to %s at 1 m", Radiation.rateText(before * Tuning.CONTAMINATION_SV_H_AT_1M), Radiation.rateText(after * Tuning.CONTAMINATION_SV_H_AT_1M));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getLevel() instanceof ServerLevel level && context.getPlayer() != null) {
			context.getPlayer().sendOverlayMessage(Component.literal(wash(level, context.getClickedPos(), context.getPlayer())));
		}
		return InteractionResult.SUCCESS;
	}
}
