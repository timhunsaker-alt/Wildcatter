package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Enrichment;
import dev.wildcatter.fluid.Product;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A TC-21 class gas centrifuge (docs/multiblocks/enrichment.md): a 20 cm rotor over 5 m in its vacuum casing, on the cascade header
 * by a pipe at its top cell. It spins while a frequency converter on its header drives it; its separative work follows its speed.
 * One per stage at least: the feed station counts them.
 * <p>16c1, with {@code cascadeSpinUp=true} in the config ({@link dev.wildcatter.Rules}; off by default, so cascades run as before):
 * it takes 10 minutes from rest to full speed on its converter's ramp and coasts for an hour in its vacuum when its drive goes,
 * separating at its speed all the while.
 * <p>16c2 (enrichment-operations.md item 4), with the rule {@code centrifugeCrashes}: no random crashes. A machine crashes held between
 * 30 % and 60 % of its speed for 2 minutes, overspeed past 1,300 Hz for 15 minutes, its converter closing onto it while it coasts over
 * 30 %, or (the feed station's count) a hot casing past 15 minutes. A crash bangs, lets out 0.1 kg of UF6 that harms whoever is within
 * 3 m, leaves a crashed centrifuge in its place, and shakes each machine within a block of it: 20 % each crashes too.
 */
public class CentrifugeBlockEntity extends PlantUnitBlockEntity {
	/** Ticks a machine keeps its speed after its converter last drove it. */
	private static final int HOLD_TICKS = 40;
	private @Nullable BlockPos driver;
	private long drivenAt = -1000;
	private double share;
	private double spin;          // 16c1: its speed with spin-up and coast-down, 0 to 1
	private long spinAt = -1;     // game time of that speed
	private double critical;      // 16c2: s held at a critical speed
	private double overspeed;     // s over 1,300 Hz
	private @Nullable String closedOnto;   // its converter closed onto it while it coasted
	/** The chance a crash shakes a neighbour into one: the real 20 %, or set by a game test. */
	private static double shake = Tuning.CRASH_SHAKE_CHANCE;

	public CentrifugeBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CENTRIFUGE_ENTITY, pos, state);
	}

	/** True if {@code cell} is its top cell, where the header joins it. */
	public boolean header(BlockPos cell) {
		return LeasePartBlock.local(worldPosition, getBlockState(), cell)[1] == 5;
	}

	/** True if the converter at {@code converter} may drive it: it is free, or already that converter's. */
	public boolean free(BlockPos converter, long now) {
		return driver == null || driver.equals(converter) || now - drivenAt > HOLD_TICKS;
	}

	/** Its converter feeds it {@code share} of its power. */
	public void drive(BlockPos converter, double share, long now) {
		// 16c2: a converter closing onto it while it coasts (driven before, in this session, and not now)
		if (drivenAt > 0 && now - drivenAt > HOLD_TICKS && dev.wildcatter.Rules.centrifugeCrashes() && dev.wildcatter.Rules.cascadeSpinUp()
				&& spin > Tuning.CLOSE_ONTO_SPEED && share > 0) {
			closedOnto = String.format("its converter closed onto it coasting at %.0f %% speed", spin * 100);
		}
		driver = converter;
		drivenAt = now;
		this.share = Math.max(0, Math.min(1, share));
	}

	/** Its speed as a share of full, 0 when no converter has driven it lately. */
	public double speed() {
		double driven = level == null || level.getGameTime() - drivenAt > HOLD_TICKS ? 0 : share;
		if (level == null || !dev.wildcatter.Rules.cascadeSpinUp()) {
			return driven;
		}
		// 16c1: spin-up on the converter's ramp, coast-down in the casing's vacuum
		long now = level.getGameTime();
		double dt = spinAt < 0 ? 0 : (now - spinAt) / 20.0;
		spinAt = now;
		if (spin < driven) {
			spin = Math.min(driven, spin + dt / Tuning.SPIN_UP_SECONDS);
		} else if (spin > driven) {
			spin = Math.max(driven, spin - dt / Tuning.COAST_DOWN_SECONDS);
		}
		return spin;
	}

	/** True while its drive is on: a coasting machine still turns but no converter drives it. */
	public boolean driven() {
		return level != null && level.getGameTime() - drivenAt <= HOLD_TICKS && share > 0;
	}

	/** For the game test: its speed set straight (the spin-up or coast-down skipped). */
	public void setSpinForTest(double s) {
		spin = s;
		spinAt = level == null ? -1 : level.getGameTime();
	}

	// ---- 16c2: crashes ---------------------------------------------------------------------------------------------------------------
	/** For the game test: the chance a crash shakes a neighbour into one (negative for the real 20 %). */
	public static void setShakeChanceForTest(double chance) {
		shake = chance < 0 ? Tuning.CRASH_SHAKE_CHANCE : chance;
	}

	/** For the game test: held this long at a critical speed already. */
	public void setCriticalForTest(double seconds) {
		critical = seconds;
	}

	/** The feed station's look at it each interval (the rule centrifugeCrashes): why it crashes now, or null. */
	public @Nullable String crashCheck(double dt) {
		double s = speed();
		critical = s >= Tuning.CRITICAL_SPEED_LOW && s <= Tuning.CRITICAL_SPEED_HIGH ? critical + dt : 0;
		overspeed = Tuning.CENTRIFUGE_HERTZ * s > Tuning.OVERSPEED_HZ ? overspeed + dt : 0;
		if (closedOnto != null) {
			return closedOnto;
		}
		if (critical >= Tuning.CRITICAL_SECONDS) {
			return String.format("held at a critical speed (%.0f %%) for %.0f s", s * 100, critical);
		}
		if (overspeed >= Tuning.OVERSPEED_SECONDS) {
			return String.format("overspeed, %.0f Hz for %.0f minutes", Tuning.CENTRIFUGE_HERTZ * s, overspeed / 60);
		}
		return null;
	}

	/**
	 * The rotor breaks up in its casing: a bang and a puff of UF6 that harms whoever is within 3 m, a crashed centrifuge in its place, and
	 * (if {@code shakes}) each machine within a block of it crashes too at the shake chance. Returns the machines crashed.
	 */
	public int crash(net.minecraft.server.level.ServerLevel level, String reason, boolean shakes) {
		BlockPos main = worldPosition;
		BlockState state = getBlockState();
		net.minecraft.core.Direction facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
		java.util.List<CentrifugeBlockEntity> near = new java.util.ArrayList<>();
		if (shakes) {
			for (BlockPos p : BlockPos.betweenClosed(main.offset(-2, 0, -2), main.offset(2, 0, 2))) {
				if (!p.equals(main) && level.getBlockEntity(p) instanceof CentrifugeBlockEntity c) {
					near.add(c);
				}
			}
		}
		for (BlockPos cell : dev.wildcatter.multiblock.BigParts.cells(main, state)) {
			level.setBlock(cell, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
		}
		BlockState crashed = dev.wildcatter.WildcatterBlocks.CRASHED_CENTRIFUGE.defaultBlockState()
				.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, facing);
		level.setBlock(main, crashed, 3);
		dev.wildcatter.multiblock.BigParts.grow(level, main, crashed);
		if (level.getBlockEntity(main) instanceof CrashedCentrifugeBlockEntity wreck) {
			wreck.setReason(reason);
		}
		double x = main.getX() + 0.5, y = main.getY() + 3, z = main.getZ() + 0.5;
		level.playSound(null, main, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.BLOCKS, 2.0F, 0.8F);
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, x, y, z, 40, 0.8, 2.0, 0.8, 0.02);   // UF6 meeting moist air: HF and UO2F2
		net.minecraft.world.phys.AABB puff = new net.minecraft.world.phys.AABB(main).inflate(Tuning.CRASH_HARM_RADIUS, 6, Tuning.CRASH_HARM_RADIUS);
		for (net.minecraft.world.entity.LivingEntity e : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, puff)) {
			e.hurtServer(level, level.damageSources().magic(), 4.0F);
			e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 200, 0));
		}
		int n = 1;
		for (CentrifugeBlockEntity c : near) {
			if (!c.isRemoved() && level.getRandom().nextDouble() < shake) {
				n += c.crash(level, "shaken apart by the crash beside it", false);
			}
		}
		return n;
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	@Override
	protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("spin", spin);
	}

	@Override
	protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
		super.loadAdditional(input);
		spin = input.getDoubleOr("spin", 0);
	}

	@Override
	public Component inspect() {
		double s = speed();
		if (s > 0 && !driven()) {
			return Component.literal(String.format("Gas centrifuge (TC-21 class): COASTING with no drive, %.0f Hz (%.0f %% speed), %.1f SWU/h", Tuning.CENTRIFUGE_HERTZ * s, s * 100,
					Enrichment.machineSwuPerSecond() * 3600 * s));
		}
		if (s <= 0) {
			return Component.literal("Gas centrifuge (TC-21 class): NO DRIVE, 0 Hz: set a FREQUENCY CONVERTER on its header (a steel pipe at its top)");
		}
		return Component.literal(String.format("Gas centrifuge (TC-21 class): %.0f Hz, rotor %.0f m/s (%,.0f rev/s), %.1f SWU/h", Tuning.CENTRIFUGE_HERTZ * s,
				Tuning.CENTRIFUGE_TIP_SPEED * s, Tuning.CENTRIFUGE_HERTZ * 2 / Tuning.CENTRIFUGE_MOTOR_POLES * s, Enrichment.machineSwuPerSecond() * 3600 * s));
	}
}
