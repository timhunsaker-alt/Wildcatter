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
		int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
		long seed = ReservoirData.seed(level, bx, bz);
		Reservoir here = Reservoir.at(seed, bx, bz, ReservoirData.site(level));
		// a salt dome: salt is fast and light, so a refraction shot and a gravity low show it (docs/salt-dome-oil.md)
		dev.wildcatter.well.SaltDome dome = ReservoirData.dome(level, bx, bz);
		if (dome != null && dome.overMiddle(bx, bz)) {
			String cap = here != null ? " STRONG ECHO in the caprock: a pool of oil, its top at y=" + here.topY() + "." : " No echo from the caprock.";
			return "SALT DOME: a fast, light salt column about " + (dome.radius() * 2) + " blocks across under you, its crest at y=" + dome.crestY()
					+ " under " + Tuning.DOME_CAPROCK + " m of caprock. Oil gathers against its flanks." + cap;
		}
		if (here != null && here.source() == Reservoir.Source.FLANK) {
			return "STRONG ECHO: you are standing over a field on a salt dome's flank, " + Math.round(here.radius() - here.inner()) + " blocks across the band. Its top is at y="
					+ here.topY() + ". Worth a drill here.";
		}
		if (here != null && here.deep()) {
			return "DEEP ECHO: you are standing over a deep field about " + Math.round(here.radius() * 2) + " blocks across. Its top is at y="
					+ here.topY() + ", too deep for the small rig: it needs a land rig.";
		}
		if (here != null) {
			return "STRONG ECHO: you are standing over a field about " + Math.round(here.radius() * 2) + " blocks across. Its top is at y="
					+ here.topY() + ". Worth a drill here.";
		}
		Reservoir near = Reservoir.nearest(seed, x, z, Tuning.SURVEY_RANGE, ReservoirData.site(level));
		if (near == null) {
			return "No echo within " + Tuning.SURVEY_RANGE + " blocks: solid rock. Try somewhere else.";
		}
		double distance = Math.max(1, near.edgeDistance(x, z));
		double[] toward = near.source() == Reservoir.Source.FLANK ? nearestPoint(near, x, z) : new double[] {near.centerX(), near.centerZ()};
		double angle = Math.toDegrees(Math.atan2(toward[1] - z, toward[0] - x));
		String direction = COMPASS[(int) Math.floorMod(Math.round(angle / 45.0), 8L)];
		String strength = distance < 24 ? "Strong" : distance < 56 ? "Fair" : "Faint";
		return (near.deep() ? "Deep " + strength.toLowerCase() : strength) + " echo from the " + direction + ", about " + Math.round(distance) + " blocks to its edge. Walk that way and thump again.";
	}

	/** The middle of a flank field's band: where to walk toward (the dome's axis is not on the field). */
	private static double[] nearestPoint(Reservoir field, double x, double z) {
		int[] m = field.middle();
		return new double[] {m[0] + 0.5, m[1] + 0.5};
	}
}
