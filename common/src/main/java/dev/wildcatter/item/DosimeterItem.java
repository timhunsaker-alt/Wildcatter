package dev.wildcatter.item;

import dev.wildcatter.fluid.Radiation;
import dev.wildcatter.radiation.DoseData;
import dev.wildcatter.radiation.RadiationTicker;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * An electronic personal dosimeter (docs/multiblocks/fuel-handling.md): a click reads the dose rate and the dose. In the hotbar it alarms
 * over 1 mSv/h, and over 20 mSv (the occupational year).
 */
public class DosimeterItem extends Item {
	public DosimeterItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
			double rate = RadiationTicker.rateAt(server, sp.getEyePosition(), sp);
			double[] dose = DoseData.get(server).of(sp.getStringUUID());
			int stage = Radiation.stage(dose[1]);
			String sick = switch (stage) {
				case 0 -> "";
				case 1 -> "; RADIATION SICKNESS (mild)";
				case 2 -> "; RADIATION SICKNESS";
				default -> "; SEVERE RADIATION SICKNESS";
			};
			sp.sendOverlayMessage(Component.literal(String.format("Dosimeter: %s; dose %s (lifetime), %s (recent)%s", Radiation.rateText(rate), Radiation.doseText(dose[0]),
					Radiation.doseText(dose[1]), sick)));
		}
		return InteractionResult.SUCCESS;
	}
}
