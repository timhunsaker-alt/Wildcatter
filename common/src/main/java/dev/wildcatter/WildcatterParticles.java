package dev.wildcatter;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * The mod's own particles (docs/kinetics-and-sound.md). Vanilla's are about half a block across, too small to read over an 88 m
 * cooling tower, so the tower's plume has its own: a puff of steam metres across that rises, spreads and thins out
 * ({@code client/SteamPlumeParticle}). Client-side only; the server never sends it.
 */
public final class WildcatterParticles {
	public static final SimpleParticleType STEAM_PLUME = Registry.register(BuiltInRegistries.PARTICLE_TYPE, Wildcatter.id("steam_plume"),
			new SimpleParticleType(true) {
			});

	private WildcatterParticles() {
	}

	/** Touch this class early so the particles are registered. */
	public static void register() {
	}
}
