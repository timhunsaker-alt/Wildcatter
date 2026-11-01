package dev.wildcatter.item;

import dev.wildcatter.Tuning;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * A handheld gamma scintillometer: counts per second from the uranium around you, over a steady background. Each block with
 * uranium in it adds its U3O8 grade over the square of its distance. Real gamma gets through only about 0.3 m of rock; this
 * one hears {@link Tuning#SCINT_RANGE} blocks, as the seismograph hears further than real (docs/ores.md).
 */
public class ScintillometerItem extends Item {
	private static final String[] COMPASS = {"east", "south-east", "south", "south-west", "west", "north-west", "north", "north-east"};

	public ScintillometerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel server) {
			level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.4F, 1.6F);
			player.sendOverlayMessage(Component.literal(report(server, player.getX(), player.getEyeY(), player.getZ())));
		}
		return InteractionResult.SUCCESS;
	}

	/** The count rate and which way it rises, as the meter shows it. */
	public static String report(ServerLevel level, double x, double y, double z) {
		Reading r = read(level, x, y, z);
		long cps = Math.round(r.cps());
		if (r.cps() < Tuning.SCINT_BACKGROUND_CPS * 1.2) {
			return cps + " cps: background. No uranium within " + Tuning.SCINT_RANGE + " blocks.";
		}
		double horizontal = Math.hypot(r.dx(), r.dz());
		String way;
		if (Math.abs(r.dy()) > horizontal * 2) {
			way = r.dy() < 0 ? "below you" : "above you";
		} else {
			double angle = Math.toDegrees(Math.atan2(r.dz(), r.dx()));
			way = "to the " + COMPASS[(int) Math.floorMod(Math.round(angle / 45.0), 8L)] + (r.dy() < -horizontal * 0.5 ? " and below" : "");
		}
		String strength = r.cps() > 5000 ? "Very high" : r.cps() > 500 ? "High" : "Above background";
		return cps + " cps. " + strength + ": the count rises " + way + ".";
	}

	/** Counts per second, and the direction the count comes from (a sum of each source's pull, not normalised). */
	public record Reading(double cps, double dx, double dy, double dz) {
	}

	public static Reading read(ServerLevel level, double x, double y, double z) {
		Map<Block, Double> grade = new java.util.HashMap<>();
		for (Ore ore : Ore.values()) {
			if (ore.u3o8() > 0) {
				grade.put(OreBlocks.block(ore), ore.u3o8());
			}
		}
		int range = Tuning.SCINT_RANGE;
		BlockPos centre = BlockPos.containing(x, y, z);
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		double cps = Tuning.SCINT_BACKGROUND_CPS, vx = 0, vy = 0, vz = 0;
		for (int dx = -range; dx <= range; dx++) {
			for (int dy = -range; dy <= range; dy++) {
				for (int dz = -range; dz <= range; dz++) {
					double d2 = dx * dx + dy * dy + dz * dz;
					if (d2 > range * range) {
						continue;
					}
					at.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
					Double g = grade.get(level.getBlockState(at).getBlock());
					if (g == null) {
						continue;
					}
					double ex = at.getX() + 0.5 - x, ey = at.getY() + 0.5 - y, ez = at.getZ() + 0.5 - z;
					double r2 = Math.max(1, ex * ex + ey * ey + ez * ez);
					double c = Tuning.SCINT_CPS_PER_U3O8 * g / r2;
					cps += c;
					double r = Math.sqrt(r2);
					vx += c * ex / r;
					vy += c * ey / r;
					vz += c * ez / r;
				}
			}
		}
		return new Reading(cps, vx, vy, vz);
	}
}
