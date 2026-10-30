package dev.wildcatter.safety;

import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.Tuning;
import dev.wildcatter.energy.Voltage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Electric shocks: how hard, and what it looks and sounds like. */
public final class Shock {
	private Shock() {
	}

	public static float damage(Voltage voltage) {
		return switch (voltage) {
			case LOW -> Tuning.SHOCK_DAMAGE_LOW;
			case MEDIUM -> Tuning.SHOCK_DAMAGE_MEDIUM;
			case DISTRIBUTION -> Tuning.SHOCK_DAMAGE_DISTRIBUTION;
			case HIGH -> Tuning.SHOCK_DAMAGE_HIGH;
		};
	}

	/** How far a bare live wire at this voltage reaches. */
	public static double reach(Voltage voltage) {
		return switch (voltage) {
			case LOW -> Tuning.BARE_REACH_LOW;
			case MEDIUM -> Tuning.BARE_REACH_MEDIUM;
			case DISTRIBUTION -> Tuning.BARE_REACH_DISTRIBUTION;
			case HIGH -> Tuning.BARE_REACH_HIGH;
		};
	}

	public static double touchChance(Voltage voltage) {
		return switch (voltage) {
			case LOW -> Tuning.TOUCH_SHOCK_CHANCE_LOW;
			case MEDIUM -> Tuning.TOUCH_SHOCK_CHANCE_MEDIUM;
			case DISTRIBUTION -> Tuning.TOUCH_SHOCK_CHANCE_DISTRIBUTION;
			case HIGH -> Tuning.TOUCH_SHOCK_CHANCE_HIGH;
		};
	}

	/** Shocks with whatever fraction of the full voltage is actually present. */
	public static void zap(ServerLevel level, LivingEntity victim, Voltage voltage, BlockPos source, double volts) {
		zapScaled(level, victim, voltage, source, (float) Math.min(1.0, Math.abs(volts) / voltage.nominal()));
	}

	/** Shocks {@code victim} at full strength. */
	public static void zap(ServerLevel level, LivingEntity victim, Voltage voltage, BlockPos source) {
		zapScaled(level, victim, voltage, source, 1.0F);
	}

	/** Shocks {@code victim}: damage (doubled when wet), a crack of sparks and a sting of sound. Creative players are skipped. */
	private static void zapScaled(ServerLevel level, LivingEntity victim, Voltage voltage, BlockPos source, float fraction) {
		if (victim instanceof Player player && (player.isCreative() || player.isSpectator())) {
			return;
		}
		float amount = damage(voltage) * fraction * (victim.isInWaterOrRain() ? Tuning.WET_SHOCK_MULTIPLIER : 1.0F);
		victim.hurtServer(level, level.damageSources().lightningBolt(), amount);
		Vec3 at = Vec3.atCenterOf(source);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 12, 0.3, 0.3, 0.3, 0.2);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, victim.getX(), victim.getY() + 1.0, victim.getZ(), 8, 0.3, 0.5, 0.3, 0.2);
		level.playSound(null, source, WildcatterSounds.ZAP, SoundSource.BLOCKS, 0.9F, 1.0F);
	}

	/**
	 * A player touches a machine. If it is live and has no ground connection, it may bite.
	 * Returns true if it did.
	 */
	public static boolean touch(ServerLevel level, BlockPos machine, Player player, Voltage voltage, boolean live) {
		if (!live || Grounding.isGrounded(level, machine)) {
			return false;
		}
		if (level.getRandom().nextDouble() >= touchChance(voltage)) {
			return false;
		}
		zap(level, player, voltage, machine);
		return true;
	}
}
