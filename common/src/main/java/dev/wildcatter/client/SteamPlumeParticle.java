package dev.wildcatter.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * A puff of a cooling tower's plume ({@link dev.wildcatter.WildcatterParticles#STEAM_PLUME}): 8 to 13 m across as it leaves the tower's
 * top, rising at the speed it is given and slowing (some 40 to 60 m up), spreading to about twice that and thinning out over 10 to 14 s.
 */
public class SteamPlumeParticle extends SingleQuadParticle {
	private final SpriteSet sprites;
	private final float startSize;

	SteamPlumeParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
		super(level, x, y, z, sprites.first());
		this.sprites = sprites;
		this.xd = xd;
		this.yd = yd;
		this.zd = zd;
		this.hasPhysics = false;
		this.gravity = 0;
		this.friction = 0.992F;
		this.lifetime = 200 + random.nextInt(80);
		this.startSize = 4.0F + random.nextFloat() * 2.5F;
		this.quadSize = startSize;
		this.roll = random.nextFloat() * (float) Math.PI * 2;
		this.oRoll = roll;
		float grey = 0.9F + random.nextFloat() * 0.08F;
		setColor(grey, grey, grey + 0.02F);
		setAlpha(0.8F);
		setSpriteFromAge(sprites);
	}

	@Override
	public void tick() {
		super.tick();
		if (removed) {
			return;
		}
		float t = (float) age / lifetime;
		quadSize = startSize * (1 + t);
		setAlpha(0.8F * (1 - t * t));
		setSpriteFromAge(sprites);
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}

	public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
				RandomSource random) {
			return new SteamPlumeParticle(level, x, y, z, xd, yd, zd, sprites);
		}
	}
}
