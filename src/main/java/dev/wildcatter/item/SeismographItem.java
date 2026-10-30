package dev.wildcatter.item;

import dev.wildcatter.Tuning;
import dev.wildcatter.well.Reservoir;
import dev.wildcatter.well.ReservoirData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Thumps the ground and listens for the echo. A field of oil or gas reflects sound differently from solid rock, so it
 * can find where one lies and how deep, but not what is in it: only a drill finds that out.
 */
public class SeismographItem extends Item {
	private static final String[] COMPASS = {"east", "south-east", "south", "south-west", "west", "north-west", "north", "north-east"};

	public SeismographItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel server) {
			level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6F, 0.4F);
			player.sendOverlayMessage(Component.literal(report(server, player.getX(), player.getZ())));
		}
		return InteractionResult.SUCCESS;
	}

	public static String report(ServerLevel level, double x, double z) {
		long seed = ReservoirData.seed(level);
		Reservoir here = Reservoir.at(seed, (int) Math.floor(x), (int) Math.floor(z));
		if (here != null) {
			return "STRONG ECHO: you are standing over a field about " + Math.round(here.radius() * 2) + " blocks across. Its top is at y="
					+ here.topY() + ". Worth a drill here.";
		}
		Reservoir near = Reservoir.nearest(seed, x, z, Tuning.SURVEY_RANGE);
		if (near == null) {
			return "No echo within " + Tuning.SURVEY_RANGE + " blocks: solid rock. Try somewhere else.";
		}
		double distance = Math.max(1, near.edgeDistance(x, z));
		double angle = Math.toDegrees(Math.atan2(near.centerZ() - z, near.centerX() - x));
		String direction = COMPASS[(int) Math.floorMod(Math.round(angle / 45.0), 8L)];
		String strength = distance < 24 ? "Strong" : distance < 56 ? "Fair" : "Faint";
		return strength + " echo from the " + direction + ", about " + Math.round(distance) + " blocks to its edge. Walk that way and thump again.";
	}
}
