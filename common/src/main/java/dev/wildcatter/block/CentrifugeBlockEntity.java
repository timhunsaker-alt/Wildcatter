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
 */
public class CentrifugeBlockEntity extends PlantUnitBlockEntity {
	/** Ticks a machine keeps its speed after its converter last drove it. */
	private static final int HOLD_TICKS = 40;
	private @Nullable BlockPos driver;
	private long drivenAt = -1000;
	private double share;
	private double spin;          // 16c1: its speed with spin-up and coast-down, 0 to 1
	private long spinAt = -1;     // game time of that speed

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
