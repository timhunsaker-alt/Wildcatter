package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.multiblock.BigPartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The combined cycle's 5 MW condensing steam turbine and the steam side's controller (docs/multiblocks/combined-cycle.md). It takes
 * the steam of the HRSG it touches, exhausts into the air-cooled condenser touching it (whose fans it runs) and needs the HRSG's feed
 * pump running. It rolls up once there is steam and its generator synchronizes; its power is what the steam gives.
 *
 * <p>Its second steam source (docs/multiblocks/acid-plant-steam.md): an acid plant's superheater touching it, its feed pump touching
 * the acid plant's waste heat boiler (to which it returns the condensate). It makes 0.87 MW a kg/s less its throttle's part-load loss,
 * and trips on LOW STEAM TEMPERATURE if the superheater passes saturated steam.
 */
public class SteamCycleBlockEntity extends BlockEntity implements Inspectable, BigPartEntity, dev.wildcatter.kinetic.Sounding {
	private final dev.wildcatter.kinetic.RunningSound running = new dev.wildcatter.kinetic.RunningSound();
	private double speed;
	private String status = "starting";
	private boolean grown;
	/** Tripped on low steam temperature: it coasts down, and resets once stopped with superheat back. */
	private boolean lowTemperature;

	public SteamCycleBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.STEAM_CYCLE_ENTITY, pos, state);
	}

	public @Nullable GeneratorPartBlockEntity generator(Level level) {
		return Touching.find(level, worldPosition, GeneratorPartBlockEntity.class, g -> g.getBlockState().is(WildcatterBlocks.ST_GENERATOR));
	}

	public static void tick(Level level, BlockPos pos, BlockState state, SteamCycleBlockEntity st) {
		double dt = 1.0 / 20.0;
		HrsgBlockEntity hrsg = Touching.find(level, pos, HrsgBlockEntity.class, h -> true);
		AcidSuperheaterBlockEntity sh = hrsg != null ? null : Touching.find(level, pos, AcidSuperheaterBlockEntity.class, h -> true);
		WasteHeatBoilerBlockEntity boiler = sh == null ? null : sh.boiler(level);
		GeneratorPartBlockEntity gen = st.generator(level);
		UnitMotorBlockEntity acc = Touching.find(level, pos, UnitMotorBlockEntity.class, m -> m.kind().equals("air_cooled_condenser"));
		BlockPos feedFrom = hrsg != null ? hrsg.getBlockPos() : boiler != null ? boiler.getBlockPos() : null;
		UnitMotorBlockEntity pump = feedFrom == null ? null : Touching.find(level, feedFrom, UnitMotorBlockEntity.class, m -> m.kind().equals("hrsg_feed_pump"));
		double steam = hrsg != null ? hrsg.steamWatts() : sh != null ? dev.wildcatter.fluid.AcidSteam.turbineWatts(sh.steamKgs()) : 0;
		double available = 0;
		if (st.lowTemperature && st.speed <= 0 && (sh == null || sh.superheated() || sh.steamKgs() <= 0)) {
			st.lowTemperature = false;   // stopped, and the steam is hot again (or gone): it may roll up again
		}
		if (hrsg == null && sh == null) {
			st.status = "NO STEAM: needs an HRSG or an ACID SUPERHEATER against it";
		} else if (sh != null && sh.steamKgs() <= 0) {
			st.status = "NO STEAM: the acid superheater has none (" + sh.inspect().getString().replace("Acid plant superheater: ", "") + ")";
		} else if (gen == null || acc == null || pump == null) {
			st.status = "NOT BUILT: needs " + (gen == null ? "its GENERATOR against it, " : "") + (acc == null ? "an AIR-COOLED CONDENSER against it, " : "")
					+ (pump == null ? (sh != null ? "a FEED PUMP against the waste heat boiler, " : "a FEED PUMP against the HRSG, ") : "");
			st.status = st.status.substring(0, st.status.length() - 2);
		} else if (st.lowTemperature) {
			st.status = "TRIPPED on LOW STEAM TEMPERATURE: the superheater passed saturated steam (is the converter running?); it resets once stopped";
		} else if (sh != null && sh.steamKgs() > 0 && !sh.superheated()) {
			st.lowTemperature = true;   // wet steam would erode the last stages' blades
			if (gen.live()) {
				gen.trip("on LOW STEAM TEMPERATURE");
			}
			st.status = "TRIPPED on LOW STEAM TEMPERATURE: the superheater passed saturated steam (is the converter running?)";
		} else if (steam < Tuning.ST_WATTS * 0.05) {
			st.status = hrsg != null ? String.format("waiting for steam (the HRSG makes %.2f MW worth)", steam / 1e6)
					: String.format("NO STEAM: the superheater passes %.2f kg/s", sh.steamKgs());
		} else {
			double fans = acc.drive(level), feed = pump.drive(level);
			if (fans <= 0) {
				st.status = "NO CONDENSER: its fans are not running (" + acc.describe() + ")";
			} else if (feed <= 0) {
				st.status = "NO FEEDWATER: the feed pump is not running (" + pump.describe() + ")";
			} else {
				available = Math.min(steam, Tuning.ST_WATTS) * Math.min(fans, feed);
				st.status = sh == null ? String.format("steam for %.2f MW", available / 1e6)
						: String.format("steam for %.2f MW (%.2f kg/s from the acid plant)", available / 1e6, sh.steamKgs());
				if (boiler != null && feed > 0) {
					boiler.returnCondensate(level, sh.steamKgs() * dt * Tuning.ACID_CONDENSATE_RETURN, dt);
				}
			}
		}
		// it rolls up over a minute on steam, and coasts down without it
		st.speed = available > 0 ? Math.min(1, st.speed + dt / Tuning.TURBINE_RUNUP_SECONDS) : Math.max(0, st.speed - dt / 60.0);
		if (gen != null) {
			gen.drive(level, available > 0 && st.speed >= 1, available);
		}
		st.running.update(st, st.speed > 0.05);
		st.setChanged();
	}

	/** Game test hook: at speed at once (a minute in play). */
	public void skipRunupForTest() {
		speed = 1;
		setChanged();
	}

	@Override
	public Component inspect() {
		GeneratorPartBlockEntity gen = level == null ? null : generator(level);
		return Component.literal(String.format("Steam turbine (5 MW condensing): %s, %.0f%% speed; %s", status, speed * 100, gen == null ? "no generator" : gen.status()));
	}

	@Override
	public net.minecraft.sounds.SoundEvent loop() {
		return running.on() ? dev.wildcatter.WildcatterSounds.TURBINE_HUM : null;
	}

	@Override
	public float soundPitch() {
		return 0.9F;
	}

	@Override
	public float soundVolume() {
		return running.on() ? 0.8F : 0;
	}

	@Override
	public boolean grown() {
		return grown;
	}

	@Override
	public void markGrown() {
		grown = true;
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("grown", grown);
		output.putDouble("speed", speed);
		output.putBoolean("lowTemperature", lowTemperature);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		running.load(input);
		grown = input.getBooleanOr("grown", grown);
		speed = input.getDoubleOr("speed", 0);
		lowTemperature = input.getBooleanOr("lowTemperature", false);
	}

	@Override
	public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
		return running.tag();
	}
}
