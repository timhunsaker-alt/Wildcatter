package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.TurbineCycle;
import dev.wildcatter.multiblock.BigPart;
import dev.wildcatter.multiblock.BigPartEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The HP turbine, the turbine island's controller (docs/multiblocks/turbine-island.md). Each interval it finds its parts by touching,
 * takes the steam the steam generators offer above the 7.0 MPa header through the valve chest, works out the condenser's vacuum from
 * the circulating water, rolls up and drives the 60 MVA generator, and moves the water round: hotwell, condensate pumps, deaerator,
 * feed pumps, HP heaters. A turbine trip stops the steam and tells the steam generators (above P-9 the reactor trips); a sneak-click
 * resets it.
 */
public class HpTurbineBlockEntity extends BlockEntity implements Inspectable, BigPartEntity, dev.wildcatter.kinetic.Sounding {
	private final dev.wildcatter.kinetic.RunningSound running = new dev.wildcatter.kinetic.RunningSound();
	private double speed;
	private double hotwell;
	private double rejected;        // W the condenser last had to reject
	private double kpa = 101.3;
	private double steamKgs, dumpKgs, cwKgs, feedKgs, mw;
	private boolean tripped;
	private String trip = "";
	private String status = "starting";
	private boolean grown;

	public HpTurbineBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.HP_TURBINE_ENTITY, pos, state);
	}

	private static String kind(BlockEntity e) {
		return e.getBlockState().getBlock() instanceof BigPart part ? part.shapeName() : "";
	}

	private static @Nullable BlockEntity part(List<BlockEntity> around, String kind) {
		for (BlockEntity e : around) {
			if (kind(e).equals(kind)) {
				return e;
			}
		}
		return null;
	}

	private static List<UnitMotorBlockEntity> motors(List<BlockEntity> around, String kind) {
		List<UnitMotorBlockEntity> out = new ArrayList<>();
		for (BlockEntity e : around) {
			if (e instanceof UnitMotorBlockEntity m && m.kind().equals(kind)) {
				out.add(m);
			}
		}
		return out;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, HpTurbineBlockEntity t) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL == 0) {
			t.step(level);
			t.setChanged();
		}
	}

	private void step(Level level) {
		double dt = Tuning.WELL_INTERVAL / 20.0;
		List<BlockEntity> around = Touching.parts(level, worldPosition);
		BlockEntity chest = part(around, "turbine_valve_chest");
		BlockEntity msr = part(around, "moisture_separator_reheater");
		BlockEntity lp = part(around, "lp_turbine");
		List<BlockEntity> lpAround = lp == null ? List.of() : Touching.parts(level, lp.getBlockPos());
		GeneratorPartBlockEntity gen = null;
		CondenserBlockEntity cond = null;
		for (BlockEntity e : lpAround) {
			if (e instanceof GeneratorPartBlockEntity g && kind(g).equals("tg_generator_60")) {
				gen = g;
			} else if (e instanceof CondenserBlockEntity c) {
				cond = c;
			}
		}
		List<UnitMotorBlockEntity> condensate = cond == null ? List.of() : motors(Touching.parts(level, cond.getBlockPos()), "condensate_pump");
		DeaeratorBlockEntity da = null;
		for (UnitMotorBlockEntity p : condensate) {
			da = da != null ? da : Touching.find(level, p.getBlockPos(), DeaeratorBlockEntity.class, d -> true);
		}
		List<UnitMotorBlockEntity> feed = da == null ? List.of() : motors(Touching.parts(level, da.getBlockPos()), "main_feed_pump");
		HpHeaterBlockEntity heater = null;
		for (UnitMotorBlockEntity p : feed) {
			heater = heater != null ? heater : Touching.find(level, p.getBlockPos(), HpHeaterBlockEntity.class, h -> true);
		}
		String missing = chest == null ? "the TURBINE VALVE CHEST against it" : msr == null ? "the MOISTURE SEPARATOR REHEATER against it"
				: lp == null ? "the LP TURBINE against it" : gen == null ? "the 60 MVA GENERATOR against the LP turbine"
				: cond == null ? "the MAIN CONDENSER against the LP turbine" : condensate.isEmpty() ? "a CONDENSATE PUMP against the condenser"
				: da == null ? "the DEAERATOR against a condensate pump" : feed.isEmpty() ? "a MAIN FEED PUMP against the deaerator"
				: heater == null ? "the HP FEEDWATER HEATER against a feed pump" : "";

		// the water: hotwell to deaerator to the heaters
		feedKgs = 0;
		if (cond != null && da != null) {
			double move = 0;
			for (UnitMotorBlockEntity p : condensate) {
				move += Tuning.CONDENSATE_PUMP_KGS * dt * (hotwell > 1 ? p.drive(level) : 0);
			}
			hotwell -= da.fill(Math.min(move, hotwell));
			da.makeUp(Tuning.MAIN_FEED_PUMP_KGS * dt);
		}
		if (da != null && heater != null) {
			double move = 0;
			for (UnitMotorBlockEntity p : feed) {
				move += Tuning.MAIN_FEED_PUMP_KGS * dt * (heater.roomForFeed() > 1 ? p.drive(level) : 0);
			}
			double kg = da.draw(Math.min(move, heater.roomForFeed()));
			heater.feed(kg);
			feedKgs = kg / dt;
		}

		// the circulating water and the vacuum
		double design = TurbineCycle.cwNeeded(TurbineCycle.rejectedWatts(2 * dev.wildcatter.fluid.Reactor.steamPerGenerator(), Tuning.TG60_WATTS));
		double needed = Math.max(TurbineCycle.cwNeeded(rejected), design * 0.3);
		cwKgs = cond == null ? 0 : cond.pullCooling(level, needed * dt) / dt;
		kpa = cwKgs < 1 ? 101.3 : TurbineCycle.backPressureKpa(rejected, cwKgs);

		// the trips
		if (!tripped && speed > 0.05) {
			String why = !missing.isEmpty() ? "PART LOST: " + missing : kpa > Tuning.LOW_VACUUM_TRIP_KPA ? String.format("LOW VACUUM (%.0f kPa)", kpa)
					: da.stock() < Tuning.DEAERATOR_KG * Tuning.DEAERATOR_LOW ? "NO FEEDWATER (deaerator low)" : gen.tripped() ? "GENERATOR TRIP" : null;
			if (why != null) {
				tripped = true;
				trip = why;
			}
		}

		// the steam
		double canDeliver = 0;
		steamKgs = dumpKgs = 0;
		List<Lines.Face> inlet = chest == null ? List.of() : LeasePartBlock.faces(chest.getBlockPos(), chest.getBlockState(), at -> at[1] == 3, Direction.UP);
		if (tripped) {
			status = "TRIPPED: " + trip + " (sneak-click to reset)";
			notifySteamGenerators(level, inlet);
		} else if (!missing.isEmpty()) {
			status = "NOT BUILT: needs " + missing;
		} else if (kpa > Tuning.LOW_VACUUM_TRIP_KPA * 0.8) {
			status = String.format("NO VACUUM: the condenser needs circulating water (%.0f kPa)", kpa);
		} else if (da.stock() < Tuning.DEAERATOR_KG * Tuning.DEAERATOR_LOW * 2) {
			status = "NO FEEDWATER: the deaerator is low";
		} else {
			// the governor admits the steam its generator can use (on line, what it gave last and 2 MW more; rolling, a warming
			// flow); the condenser's steam dump takes what the steam generators still offer, up to 40 % of full steam
			double full = 2 * dev.wildcatter.fluid.Reactor.steamPerGenerator();
			double want = gen.breakerClosed() ? Math.min(Tuning.TG60_WATTS, gen.deliveredWatts() + 2e6) : 0.05 * Tuning.TG60_WATTS;
			double governed = want / Math.max(0.05, TurbineCycle.vacuumFactor(kpa)) / 1000 * Tuning.STEAM_KG_PER_KWH_PWR / 3600;
			double kg = Lines.pull(level, inlet, Product.STEAM, (Math.min(governed, full) + Tuning.STEAM_DUMP * full) * dt, FluidNode.Pull.MACHINE, this);
			steamKgs = kg / dt;
			hotwell = Math.min(Tuning.HOTWELL_KG_PWR, hotwell + kg);
			canDeliver = TurbineCycle.designWatts(Math.min(steamKgs, governed)) * TurbineCycle.vacuumFactor(kpa);
			status = steamKgs < 0.5 ? "waiting for steam over 7.0 MPa at the valve chest" : speed < 1 ? String.format("ROLLING: %.0f rpm", speed * rpm())
					: !gen.breakerClosed() ? "SYNCHRONIZING" : "RUNNING";
		}
		speed = canDeliver > 0 ? Math.min(1, speed + dt / Tuning.TURBINE_ROLL_SECONDS) : Math.max(0, speed - dt / 60.0);
		double delivered = 0;
		if (gen != null) {
			gen.drive(level, canDeliver > 0 && speed >= 1, canDeliver);
			delivered = gen.breakerClosed() ? gen.deliveredWatts() : 0;
		}
		mw = delivered / 1e6;
		dumpKgs = canDeliver > 0 ? steamKgs * Math.max(0, 1 - delivered / canDeliver) : 0;
		rejected = TurbineCycle.rejectedWatts(steamKgs, delivered);
		if (cond != null) {
			cond.status(String.format("%.1f kPa, circulating water %,.0f kg/s, steam dump %.1f kg/s, hotwell %.1f t", kpa, cwKgs, dumpKgs, hotwell / 1000));
		}
		running.update(this, speed > 0.05);
	}

	private double rpm() {
		return 120.0 * dev.wildcatter.energy.ElectricalStandard.active().hertz() / 2;
	}

	/** Tells the steam generators on its steam pipe that the turbine has tripped. */
	private void notifySteamGenerators(Level level, List<Lines.Face> inlet) {
		for (Lines.Line line : Lines.lines(level, inlet)) {
			for (Lines.Port port : Lines.sources(line, Product.STEAM, FluidNode.Pull.MACHINE, this)) {
				if (port.node() instanceof SteamGeneratorBlockEntity sg) {
					sg.markTurbineTrip(level.getGameTime());
				}
			}
		}
	}

	/** A sneak-click: resets a trip, and its generator's. */
	public String resetTrip(Level level) {
		if (!tripped) {
			return "HP turbine: not tripped";
		}
		tripped = false;
		trip = "";
		for (BlockEntity e : Touching.parts(level, worldPosition)) {
			if (kind(e).equals("lp_turbine")) {
				GeneratorPartBlockEntity gen = Touching.find(level, e.getBlockPos(), GeneratorPartBlockEntity.class, g -> true);
				if (gen != null && gen.tripped()) {
					gen.reset();
				}
			}
		}
		setChanged();
		return "HP turbine: trip reset";
	}

	/** For the game test: trips it as if a protection had. */
	public boolean tripped() {
		return tripped;
	}

	public double mw() {
		return mw;
	}

	public void setHotwellForTest(double kg) {
		hotwell = kg;
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Turbine (HP, MSR, double-flow LP; 51 MW): %s; %.0f rpm; steam %.1f kg/s, %.2f MW, vacuum %.1f kPa, "
				+ "circulating water %,.0f kg/s, steam dump %.1f kg/s, feed %.1f kg/s, hotwell %.1f t", status, speed * rpm(), steamKgs, mw, kpa, cwKgs, dumpKgs,
				feedKgs, hotwell / 1000));
	}

	@Override
	public net.minecraft.sounds.SoundEvent loop() {
		return running.on() ? dev.wildcatter.WildcatterSounds.TURBINE_HUM : null;
	}

	@Override
	public float soundPitch() {
		return 0.8F;
	}

	@Override
	public float soundVolume() {
		return running.on() ? 1.0F : 0;
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
		output.putDouble("hotwell", hotwell);
		output.putBoolean("tripped", tripped);
		output.putString("trip", trip);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		running.load(input);
		grown = input.getBooleanOr("grown", grown);
		speed = input.getDoubleOr("speed", 0);
		hotwell = input.getDoubleOr("hotwell", 0);
		tripped = input.getBooleanOr("tripped", false);
		trip = input.getStringOr("trip", "");
	}

	@Override
	public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}
}
