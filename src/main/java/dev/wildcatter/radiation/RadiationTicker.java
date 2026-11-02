package dev.wildcatter.radiation;

import dev.wildcatter.Tuning;
import dev.wildcatter.Wildcatter;
import dev.wildcatter.block.ReactorVesselBlockEntity;
import dev.wildcatter.block.SpentFuelPoolBlockEntity;
import dev.wildcatter.fluid.Radiation;
import dev.wildcatter.fluid.SpentFuel;
import dev.wildcatter.multiblock.BigParts;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Each second, each player's dose (docs/multiblocks/fuel-handling.md): the vessels and the spent fuel pools' assemblies within 64 m, each
 * through the blocks between it and the player (water and other full blocks shield), and the ore round them as the scintillometer
 * counts it; the acute dose's repair; its effects, and death by radiation at 8 Sv. A dosimeter in the hotbar alarms.
 */
public final class RadiationTicker {
	private RadiationTicker() {
	}

	public static final ResourceKey<DamageType> RADIATION = ResourceKey.create(Registries.DAMAGE_TYPE, Wildcatter.id("radiation"));
	private static final Map<UUID, Double> RATES = new HashMap<>();
	private static final Map<UUID, Double> ORE = new HashMap<>();

	/** The dose rate a player had last second, Sv/h. */
	public static double rate(ServerPlayer player) {
		return RATES.getOrDefault(player.getUUID(), 0.0);
	}

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.isSpectator()) {
				continue;
			}
			ServerLevel level = player.level();
			double rate = rateAt(level, player.getEyePosition(), server.getTickCount() % 200 == 0 ? player : null);
			RATES.put(player.getUUID(), rate);
			DoseData data = DoseData.get(level);
			double[] dose = data.of(player.getStringUUID());
			dose[0] += rate / 3600;
			dose[1] = Radiation.repaired(dose[1], 1) + rate / 3600;
			if (rate > 0) {
				data.changed();
			}
			effects(level, player, dose[1]);
			boolean dosimeter = false;
			for (int i = 0; i < 9; i++) {
				dosimeter |= player.getInventory().getItem(i).is(dev.wildcatter.WildcatterItems.DOSIMETER);
			}
			if (dosimeter && (rate > Tuning.ALARM_RATE_SV_H || dose[0] > Tuning.ALARM_DOSE_SV && server.getTickCount() % 1200 == 0)) {
				player.sendOverlayMessage(Component.literal("DOSIMETER ALARM: " + Radiation.rateText(rate) + ", dose " + Radiation.doseText(dose[0])));
			}
		}
	}

	/** Sv/h at {@code at}. With {@code player}, the ore round it is counted afresh (every 10 s); otherwise the last count is reused. */
	public static double rateAt(ServerLevel level, Vec3 at, ServerPlayer player) {
		double rate = 0;
		for (BlockPos p : Set.copyOf(ReactorVesselBlockEntity.vessels(level))) {
			if (!(level.getBlockEntity(p) instanceof ReactorVesselBlockEntity vessel)) {
				continue;
			}
			var r = vessel.reactor();
			double at1m = r.coreDamaged() ? Tuning.DAMAGED_CORE_SV_H_AT_1M : Tuning.VESSEL_SV_H_AT_1M * r.power();
			if (at1m > 0) {
				rate += through(level, new Vec3(p.getX() + 1.5, p.getY() + 5, p.getZ() + 1.5), at, at1m, Set.copyOf(BigParts.cellsAt(level, p)));
			}
		}
		for (BlockPos p : Set.copyOf(SpentFuelPoolBlockEntity.pools(level))) {
			if (!(level.getBlockEntity(p) instanceof SpentFuelPoolBlockEntity pool)) {
				continue;
			}
			var racks = pool.racks();
			for (int i = 0; i < racks.size(); i++) {
				SpentFuel.Assembly a = racks.get(i);
				double[] c = pool.rackPoint(i);
				rate += through(level, new Vec3(c[0], c[1], c[2]), at, Radiation.assemblyAt1m(a.watts()), Set.of());
			}
		}
		if (player != null) {
			double cps = dev.wildcatter.item.ScintillometerItem.read(level, at.x, at.y, at.z).cps();
			ORE.put(player.getUUID(), cps * Tuning.ORE_USV_H_PER_CPS * 1e-6);
		}
		return rate + (player != null ? ORE.getOrDefault(player.getUUID(), 0.0) : 0);
	}

	/** Sv/h from a source of {@code at1m} at {@code from} to {@code to}, through the blocks between (the source's own skipped). */
	private static double through(ServerLevel level, Vec3 from, Vec3 to, double at1m, Set<BlockPos> own) {
		double d = from.distanceTo(to);
		if (d > Tuning.DOSE_RANGE) {
			return 0;
		}
		int water = 0, solid = 0;
		Set<BlockPos> seen = new HashSet<>();
		seen.add(BlockPos.containing(from));   // the source's own cell
		int steps = (int) Math.ceil(d / 0.4);
		for (int i = 1; i < steps; i++) {
			Vec3 v = from.lerp(to, i / (double) steps);
			BlockPos b = BlockPos.containing(v);
			if (own.contains(b) || !seen.add(b)) {
				continue;
			}
			BlockState s = level.getBlockState(b);
			if (level.getFluidState(b).is(FluidTags.WATER)) {
				water++;
			} else if (s.isCollisionShapeFullBlock(level, b)) {
				solid++;
			}
			if (water > 12 || solid > 6) {
				return 0;
			}
		}
		return Radiation.rate(at1m, d, water, solid);
	}

	private static void effects(ServerLevel level, ServerPlayer player, double acute) {
		int stage = Radiation.stage(acute);
		if (stage >= 4) {
			DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(RADIATION));
			player.hurtServer(level, source, Float.MAX_VALUE);
			return;
		}
		if (stage >= 1) {
			player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, stage - 1, true, false));
			if (level.getRandom().nextFloat() < 0.03F) {
				player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0, true, false));
			}
		}
		if (stage >= 2) {
			player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 0, true, false));
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0, true, false));
		}
		if (stage >= 3) {
			player.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0, true, false));
		}
	}

	/** Dying clears the acute dose. */
	public static void respawned(ServerPlayer player) {
		DoseData data = DoseData.get(player.level());
		data.of(player.getStringUUID())[1] = 0;
		data.changed();
	}
}
