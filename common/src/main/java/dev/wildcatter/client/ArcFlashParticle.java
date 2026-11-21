package dev.wildcatter.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * The light of a power arc ({@link dev.wildcatter.WildcatterParticles#ARC_FLASH}): a blue-white glare two to four metres across, at full
 * brightness whatever the light round it, gone within a few ticks (a fault's arc lasts the few cycles until it is cleared).
 */
public class ArcFlashParticle extends SingleQuadParticle {
	ArcFlashParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
		super(level, x, y, z, sprites.get(level.getRandom()));
		this.xd = 0;
		this.yd = 0;
		this.zd = 0;
		this.hasPhysics = false;
		this.gravity = 0;
		this.lifetime = 4 + random.nextInt(3);
		this.quadSize = 2.2F + random.nextFloat() * 1.6F;
		this.roll = random.nextFloat() * (float) Math.PI * 2;
		this.oRoll = roll;
		setColor(0.88F, 0.94F, 1.0F);
		setAlpha(0.95F);
	}

	@Override
	public void tick() {
		super.tick();
		setAlpha(0.95F * (1 - (float) age / lifetime));
	}

	@Override
	protected int getLightCoords(float partialTick) {
		return 0xF000F0;
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}

	public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
				RandomSource random) {
			return new ArcFlashParticle(level, x, y, z, sprites);
		}
	}
}
