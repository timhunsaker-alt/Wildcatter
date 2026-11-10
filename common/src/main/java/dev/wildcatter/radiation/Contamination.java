package dev.wildcatter.radiation;

import dev.wildcatter.Tuning;
import dev.wildcatter.fluid.Radiation;
import dev.wildcatter.fluid.Reactor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Contamination where primary water gets out (docs/multiblocks/reactor-extensions.md, 19b), with the contamination rule on: a release
 * puts it on the surfaces round it; a contaminated block is a dose source (up to 1 mSv/h at 1 m); a player walking on one picks some up
 * and carries it until a personnel monitor washes it off; a washer cleans a block. With the rule off nothing is put down or read.
 */
public final class Contamination {
	private Contamination() {
	}

	/** How strongly coolant contaminates what it reaches: its activity over the limit, at least a twentieth. */
	public static double strength(Reactor r) {
		return Math.max(0.05, Math.min(1, r.activity() / Tuning.ACTIVITY_LIMIT));
	}

	/**
	 * A release at {@code centre}: the top surface of each column within {@code radius} takes contamination falling to half at the edge.
	 * Returns the blocks contaminated.
	 */
	public static int splash(ServerLevel level, BlockPos centre, int radius, double strength) {
		if (!dev.wildcatter.Rules.reactorContamination() || strength <= 0) {
			return 0;
		}
		ContaminationData data = ContaminationData.get(level);
		long now = level.getGameTime();
		int count = 0;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > radius) {
					continue;
				}
				BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(centre.getX() + dx, centre.getY() + Math.min(radius, 12), centre.getZ() + dz);
				for (int i = 0; i <= Math.min(radius, 12) * 2; i++, p.move(0, -1, 0)) {
					if (!level.isLoaded(p)) {
						break;
					}
					BlockState s = level.getBlockState(p);
					if (!s.isAir() && s.isCollisionShapeFullBlock(level, p) && !level.getBlockState(p.above()).isCollisionShapeFullBlock(level, p.above())) {
						data.add(p.immutable(), strength * (1 - 0.5 * d / Math.max(1, radius)), now);
						count++;
						break;
					}
				}
			}
		}
		return count;
	}

	/** Sv/h at {@code at} from the contaminated blocks within 16 m (surface sources: no shielding counted). */
	public static double rate(ServerLevel level, Vec3 at) {
		if (!dev.wildcatter.Rules.reactorContamination()) {
			return 0;
		}
		ContaminationData data = ContaminationData.get(level);
		if (data.size() == 0) {
			return 0;
		}
		long now = level.getGameTime();
		double rate = 0;
		for (BlockPos p : data.near(at.x, at.z, 16)) {
			double d2 = at.distanceToSqr(p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5);
			if (d2 <= 256) {
				rate += data.at(p, now) * Tuning.CONTAMINATION_SV_H_AT_1M / Math.max(1, d2);
			}
		}
		return rate;
	}

	/** A second on a contaminated block: the player picks up some of it. */
	public static void pickUp(ServerLevel level, ServerPlayer player) {
		if (!dev.wildcatter.Rules.reactorContamination()) {
			return;
		}
		ContaminationData data = ContaminationData.get(level);
		if (data.size() == 0) {
			return;
		}
		long now = level.getGameTime();
		BlockPos under = player.blockPosition().below();
		double on = data.at(under, now);
		if (on > 1e-4) {
			double taken = Tuning.CONTAMINATION_PICKUP * on;
			data.set(under, on - taken, now);
			data.setSkin(player.getStringUUID(), data.skin(player.getStringUUID(), now) + taken, now);
		}
	}

	/** Sv/h a player's own contamination gives them. */
	public static double skinRate(ServerLevel level, ServerPlayer player) {
		return dev.wildcatter.Rules.reactorContamination() ? ContaminationData.get(level).skin(player.getStringUUID(), level.getGameTime()) * Tuning.SKIN_SV_H : 0;
	}

	/** Washes a player clean; returns the Sv/h they carried. */
	public static double wash(ServerLevel level, ServerPlayer player) {
		double was = skinRate(level, player);
		ContaminationData.get(level).setSkin(player.getStringUUID(), 0, level.getGameTime());
		return was;
	}

	/** A block's contamination for a readout. */
	public static String reading(ServerLevel level, BlockPos pos) {
		double share = ContaminationData.get(level).at(pos, level.getGameTime());
		return share <= 0 ? "Contamination: none" : "Contamination: " + Radiation.rateText(share * Tuning.CONTAMINATION_SV_H_AT_1M) + " at 1 m";
	}
}
