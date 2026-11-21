package dev.wildcatter.safety;

import dev.wildcatter.WildcatterParticles;
import dev.wildcatter.WildcatterSounds;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;

/**
 * A power arc where fault current starts (grounding clamps closing onto a live line): its crack, a blue-white glare a few metres across,
 * a burst of sparks, a puff of the vaporised metal's smoke, and for a few ticks the light of it on everything round (a light block in the
 * nearest air, taken away again). Lasts the few cycles a bolted fault runs before it is cleared.
 */
public final class ArcFlash {
	/** Ticks the arc's light stays on the surroundings. */
	private static final int LIGHT_TICKS = 5;
	private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> LIGHTS = new HashMap<>();

	private ArcFlash() {
	}

	public static void at(ServerLevel level, BlockPos pos) {
		double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
		level.playSound(null, pos, WildcatterSounds.FLASHOVER, SoundSource.BLOCKS, 2.5F, 1.0F);
		level.sendParticles(WildcatterParticles.ARC_FLASH, x, y + 0.2, z, 6, 0.2, 0.2, 0.2, 0);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 90, 0.3, 0.3, 0.3, 0.6);
		level.sendParticles(ParticleTypes.LAVA, x, y, z, 3, 0.2, 0.2, 0.2, 0.1);
		level.sendParticles(ParticleTypes.SMOKE, x, y + 0.3, z, 16, 0.2, 0.2, 0.2, 0.04);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.4, z, 3, 0.15, 0.15, 0.15, 0.02);
		for (BlockPos at : new BlockPos[] {pos.above(), pos.relative(Direction.NORTH), pos.relative(Direction.SOUTH), pos.relative(Direction.EAST),
				pos.relative(Direction.WEST)}) {
			if (level.getBlockState(at).isAir()) {
				level.setBlock(at, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), 3);
				LIGHTS.computeIfAbsent(level.dimension(), k -> new HashMap<>()).put(at.immutable(), level.getGameTime() + LIGHT_TICKS);
				break;
			}
		}
	}

	/** Each server tick: the arcs' light goes out once they are over. */
	public static void tick(MinecraftServer server) {
		for (ServerLevel level : server.getAllLevels()) {
			Map<BlockPos, Long> lights = LIGHTS.get(level.dimension());
			if (lights == null || lights.isEmpty()) {
				continue;
			}
			for (Iterator<Map.Entry<BlockPos, Long>> it = lights.entrySet().iterator(); it.hasNext();) {
				Map.Entry<BlockPos, Long> e = it.next();
				if (level.getGameTime() >= e.getValue()) {
					if (level.getBlockState(e.getKey()).is(Blocks.LIGHT)) {
						level.setBlock(e.getKey(), Blocks.AIR.defaultBlockState(), 3);
					}
					it.remove();
				}
			}
		}
	}

	/** A new world starts with no arcs. */
	public static void clearAll() {
		LIGHTS.clear();
	}
}
