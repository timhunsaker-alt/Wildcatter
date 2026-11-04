package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.Reactor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The reactor vessel, the nuclear island's controller (docs/multiblocks/reactor.md): the 9 assemblies and 5 RCCAs, the RCS and the
 * plant model (fluid/Reactor.java). Each interval it finds its parts by touching (the CRDM head on top, the console, the CVCS, two steam
 * generators with their pumps, the pressurizer against it or a steam generator), runs them, and steps the plant with what they give.
 */
public class ReactorVesselBlockEntity extends PlantUnitBlockEntity {
	private final Reactor reactor = new Reactor();
	private final Reactor.Inputs in = new Reactor.Inputs();
	private String parts = "NOT BUILT";
	private boolean pumpsStopped = true;

	public ReactorVesselBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.REACTOR_VESSEL_ENTITY, pos, state);
	}

	public Reactor reactor() {
		return reactor;
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	private static final java.util.Map<net.minecraft.resources.ResourceKey<Level>, java.util.Set<BlockPos>> VESSELS = new java.util.HashMap<>();

	/** Forgets every vessel; called when the server stops so a new world starts clean. */
	public static void clearAll() {
		VESSELS.clear();
	}

	/** The vessels in a level, for the dose (8e). */
	public static java.util.Set<BlockPos> vessels(Level level) {
		return VESSELS.getOrDefault(level.dimension(), java.util.Set.of());
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (level != null && !level.isClientSide()) {
			java.util.Optional.ofNullable(VESSELS.get(level.dimension())).ifPresent(set -> set.remove(worldPosition));
		}
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ReactorVesselBlockEntity vessel) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL == 0) {
			VESSELS.computeIfAbsent(level.dimension(), k -> new java.util.HashSet<>()).add(pos.immutable());
			vessel.step(level);
			vessel.setChanged();
		}
	}

	private static @Nullable UnitMotorBlockEntity motor(List<BlockEntity> around, String kind) {
		for (BlockEntity e : around) {
			if (e instanceof UnitMotorBlockEntity m && m.kind().equals(kind)) {
				return m;
			}
		}
		return null;
	}

	private void step(Level level) {
		double dt = seconds();
		List<BlockEntity> around = Touching.parts(level, worldPosition);
		UnitMotorBlockEntity head = motor(around, "crdm_head");
		CvcsBlockEntity cvcs = null;
		List<SteamGeneratorBlockEntity> sgs = new ArrayList<>();
		for (BlockEntity e : around) {
			if (e instanceof CvcsBlockEntity c) {
				cvcs = c;
			} else if (e instanceof SteamGeneratorBlockEntity sg && sgs.size() < 2) {
				sgs.add(sg);
			}
		}
		sgs.sort(Comparator.comparingInt((SteamGeneratorBlockEntity s) -> s.getBlockPos().getZ()).thenComparingInt(s -> s.getBlockPos().getX()));
		UnitMotorBlockEntity pzr = motor(around, "pressurizer");
		UnitMotorBlockEntity rhrPump = motor(around, "rhr_pump");
		RhrHeatExchangerBlockEntity rhrHx = rhrPump == null ? null : Touching.find(level, rhrPump.getBlockPos(), RhrHeatExchangerBlockEntity.class, h -> true);
		ReactorCoolantPumpBlockEntity[] pumps = new ReactorCoolantPumpBlockEntity[2];
		for (int l = 0; l < sgs.size(); l++) {
			List<BlockEntity> sgAround = Touching.parts(level, sgs.get(l).getBlockPos());
			if (pzr == null) {
				pzr = motor(sgAround, "pressurizer");
			}
			for (BlockEntity e : sgAround) {
				if (e instanceof ReactorCoolantPumpBlockEntity p) {
					pumps[l] = p;
				}
			}
		}
		parts = head == null ? "NOT BUILT: set the CRDM HEAD on top of it" : sgs.isEmpty() ? "NOT BUILT: set a STEAM GENERATOR against it"
				: cvcs == null ? "NOT BUILT: set the CVCS SKID against it" : pzr == null ? "NOT BUILT: set the PRESSURIZER against it or a steam generator"
				: pumps[0] == null ? "NOT BUILT: set a REACTOR COOLANT PUMP against each steam generator" : "";

		in.crdm = head != null && head.drive(level) > 0;
		in.pressurizer = pzr != null;
		in.heaters = pzr != null && reactor.full() ? pzr.drive(level) * (reactor.wantsHeaters() ? 1 : 0) : 0;
		in.cvcs = cvcs != null;
		in.charging = cvcs != null ? cvcs.drive(level) : 0;
		in.boricAcid = cvcs != null ? cvcs.boricAcid() : 0;
		in.water = cvcs != null ? cvcs.water() : 0;
		boolean permit = reactor.full() && reactor.pressure() >= Tuning.RCP_SEAL_MPA;
		String why = !reactor.full() ? "the RCS is not full" : String.format("its seals need %.1f MPa", Tuning.RCP_SEAL_MPA);
		pumpsStopped = true;
		for (int l = 0; l < 2; l++) {
			SteamGeneratorBlockEntity sg = l < sgs.size() ? sgs.get(l) : null;
			ReactorCoolantPumpBlockEntity pump = sg != null ? pumps[l] : null;
			in.loop[l] = sg != null;
			if (pump != null) {
				pump.operate(level, permit, why, dt);
				pumpsStopped &= pump.flow() < 0.01;
			}
			in.pump[l] = pump != null ? pump.flow() : 0;
			in.pumpPower[l] = pump != null ? pump.power() : 0;
			in.steamTaken[l] = sg != null ? sg.taken() : 0;
			in.feed[l] = sg != null ? sg.pullFeed(level, reactor.feedWanted(l, dt)) : 0;
		}

		// the RHR: its pump runs while the plant is under 177 C and 2.9 MPa, its exchanger on the circulating water
		in.rhrPump = rhrPump != null && rhrHx != null && reactor.rhrPermit() && reactor.tavg() > Tuning.RHR_HOLD_C ? rhrPump.drive(level) : 0;
		double rhrKg = in.rhrPump > 0 ? rhrHx.pull(level, Tuning.RHR_CW_KGS * dt) : 0;
		in.rhrWaterKgs = rhrKg / dt;
		in.rhrWaterC = rhrHx != null ? rhrHx.inC() : Tuning.LAKE_C;
		in.turbineTripped = false;
		for (SteamGeneratorBlockEntity sg : sgs) {
			in.turbineTripped |= sg.turbineTripped(level.getGameTime());
		}
		reactor.step(in, dt);

		if (cvcs != null) {
			cvcs.use(reactor.boricAcidUsed(), reactor.waterUsed());
		}
		if (rhrHx != null) {
			double warmed = in.rhrWaterKgs > 0 ? in.rhrWaterC + reactor.rhrHeat() / (in.rhrWaterKgs * 4186) : in.rhrWaterC;
			rhrHx.giveBack(rhrKg, warmed, !reactor.rhrPermit() ? String.format("idle: the RHR cuts in under %.0f C and %.1f MPa", Tuning.RHR_MAX_C, Tuning.RHR_MAX_MPA)
					: String.format("taking %.2f MW into %.0f kg/s of circulating water at %.1f C", reactor.rhrHeat() / 1e6, in.rhrWaterKgs, in.rhrWaterC));
		}
		if (reactor.takeBlast()) {
			// the hydrogen in the island's building air ignites at the pressurizer's top (or the vessel's, without one)
			BlockPos at = pzr != null ? pzr.getBlockPos().above(8) : worldPosition.above(12);
			level.explode(null, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, Tuning.H2_BLAST_POWER, Level.ExplosionInteraction.BLOCK);
		}
		for (int l = 0; l < sgs.size(); l++) {
			String status = String.format("loop %s: level %.0f %%, %.2f MPa, feed %.1f kg/s, dump valve %.1f kg/s, to its pipe %.1f kg/s; pump %s", l == 0 ? "A" : "B",
					reactor.sgLevel(l) * 100, reactor.sgPressure(l), in.feed[l] / dt, reactor.vented(l) / dt, in.steamTaken[l] / dt,
					pumps[l] == null ? "NONE" : String.format("%.0f %%", pumps[l].flow() * 100));
			sgs.get(l).refill(reactor.steaming(l), reactor.headerShare(l), status);
		}
	}

	// ---- fuel ----------------------------------------------------------------------------------------------------------------------
	/** Cold, depressurized, its pumps stopped: the refuelling machine may work (8e). */
	public boolean openForRefuelling() {
		return reactor.loadable(pumpsStopped);
	}

	/** A right-click with an assembly or an RCCA: loads it if the vessel is cold, depressurized and its pumps stopped. */
	public String load(ItemStack stack) {
		if (!reactor.loadable(pumpsStopped)) {
			return String.format("Reactor vessel: it opens only cold (under %.0f C), depressurized and with its pumps stopped (%.0f C, %.1f MPa)", Tuning.LOADING_MAX_C,
					reactor.tavg(), reactor.pressure());
		}
		boolean rcca = stack.is(WildcatterItems.RCCA);
		boolean ok = rcca ? reactor.loadRcca() : reactor.loadAssembly(0);
		if (!ok) {
			return rcca ? "Reactor vessel: all 5 RCCAs are in" : "Reactor vessel: the core is full (9 assemblies)";
		}
		stack.shrink(1);
		setChanged();
		return String.format("Reactor vessel: loaded; %d of %d assemblies, %d of %d RCCAs", reactor.assemblies(), Tuning.CORE_ASSEMBLIES, reactor.rccas(), Tuning.RCCAS);
	}

	/** A sneak-click with an empty hand: the last RCCA out, then the last assembly if it is fresh. */
	public String unload(Player player) {
		if (!reactor.loadable(pumpsStopped)) {
			return "Reactor vessel: it opens only cold, depressurized and with its pumps stopped";
		}
		ItemStack out;
		if (reactor.unloadRcca()) {
			out = new ItemStack(WildcatterItems.RCCA);
		} else if (reactor.unloadFreshAssembly()) {
			out = new ItemStack(WildcatterBlocks.FUEL_ASSEMBLY);
		} else {
			return reactor.assemblies() > 0 ? "Reactor vessel: irradiated fuel waits for the refuelling machine (8c)" : "Reactor vessel: empty";
		}
		if (!player.getInventory().add(out.copy())) {
			net.minecraft.world.Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), out);
		}
		setChanged();
		return String.format("Reactor vessel: unloaded; %d assemblies, %d RCCAs", reactor.assemblies(), reactor.rccas());
	}

	/** For the game test: the core loaded with fresh fuel and the plant at hot standby at {@code ppm}. */
	public void setHotStandbyForTest(double ppm) {
		while (reactor.loadAssembly(0)) {
		}
		while (reactor.loadRcca()) {
		}
		reactor.setHotStandbyForTest(ppm);
		setChanged();
	}

	@Override
	public Component inspect() {
		Reactor r = reactor;
		String state = r.exploded() ? "HYDROGEN EXPLOSION; CORE DAMAGE" : r.coreDamaged() ? "CORE DAMAGE" : !parts.isEmpty() ? parts
				: r.tripped() ? "TRIPPED: " + r.trip() : r.power() < 1e-4 ? "SUBCRITICAL" : "CRITICAL";
		String power = r.power() < 1e-4 ? String.format("source range %.0f cps", r.power() * 1e10) : String.format("power %.2f %% (%.1f MW)", r.power() * 100,
				r.power() * Reactor.ratedWatts() / 1e6);
		String setpoint = r.setpoint() == Reactor.COOLDOWN ? "COOLDOWN" : r.setpoint() < 0 ? "TRIP" : String.format("%.0f %%", r.setpoint() * 100);
		return Component.literal(String.format("Reactor: %s; %s, startup rate %.2f dpm; setpoint %s%s; rods: shutdown bank %.0f, bank D %.0f of %d steps; "
				+ "boron %.0f ppm, xenon %.0f pcm; decay heat %.2f %% (%.1f MW); Tavg %.1f C (Thot %.1f, Tcold %.1f), fuel %.0f C; pressurizer %.2f MPa; RCS %.1f t; "
				+ "RHR %.2f MW; H2 %.1f kg; core %d of %d assemblies, %d of %d RCCAs, burnup %.2f GWd/t",
				state, power, r.startupRate(), setpoint, r.held().isEmpty() ? "" : ", HELD: " + r.held(), r.shutdownBank(), r.bankD(), Tuning.ROD_STEPS, r.ppm(),
				r.xenonPcm(), r.decayHeat() * 100, r.decayHeat() * Reactor.ratedWatts() / 1e6, r.tavg(), r.thot(), r.tcold(), r.fuelC(), r.pressure(), r.waterKg() / 1000,
				r.rhrHeat() / 1e6, r.h2(), r.assemblies(), Tuning.CORE_ASSEMBLIES, r.rccas(), Tuning.RCCAS,
				r.assemblies() > 0 ? r.burnup(0) : 0));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		reactor.save(new Reactor.Store() {
			@Override
			public void put(String key, double value) {
				output.putDouble(key, value);
			}

			@Override
			public double get(String key, double fallback) {
				return fallback;
			}
		});
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		reactor.load(new Reactor.Store() {
			@Override
			public void put(String key, double value) {
			}

			@Override
			public double get(String key, double fallback) {
				return input.getDoubleOr(key, fallback);
			}
		});
	}
}
