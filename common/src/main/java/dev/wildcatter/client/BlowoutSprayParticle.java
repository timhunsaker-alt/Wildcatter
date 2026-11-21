package dev.wildcatter.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * A drop of a blowout's spray ({@link dev.wildcatter.WildcatterParticles#BLOWOUT_SPRAY}): mud and oil thrown up off the rig floor at the
 * speed it is given, slowing under gravity and falling back, a dark brown (mud) or near black (oil), lasting 2 to 3 s.
 */
public class BlowoutSprayParticle extends SingleQuadParticle {
	private final SpriteSet sprites;

	BlowoutSprayParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
		super(level, x, y, z, sprites.first());
		this.sprites = sprites;
		this.xd = xd;
		this.yd = yd;
		this.zd = zd;
		this.hasPhysics = true;
		this.gravity = 1.0F;
		this.friction = 0.985F;
		this.lifetime = 40 + random.nextInt(25);
		this.quadSize = 0.3F + random.nextFloat() * 0.35F;
		this.roll = random.nextFloat() * (float) Math.PI * 2;
		this.oRoll = roll;
		if (random.nextBoolean()) {
			float k = 0.85F + random.nextFloat() * 0.3F;
			setColor(0.36F * k, 0.26F * k, 0.16F * k);   // mud
		} else {
			float k = 0.08F + random.nextFloat() * 0.06F;
			setColor(k, k * 0.92F, k * 0.8F);            // oil
		}
		setSpriteFromAge(sprites);
	}

	@Override
	public void tick() {
		super.tick();
		if (!removed) {
			setSpriteFromAge(sprites);
		}
	}

	@Override
	protected Layer getLayer() {
		return Layer.OPAQUE;
	}

	public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
				RandomSource random) {
			return new BlowoutSprayParticle(level, x, y, z, xd, yd, zd, sprites);
		}
	}
}
