package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Generator;
import dev.wildcatter.circuit.StatorHeating;
import dev.wildcatter.circuit.Synchronizer;
import dev.wildcatter.circuit.ThreePhaseSource;
import dev.wildcatter.energy.ElectricalStandard;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.kinetic.Spin;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The 1 MW steam plant's turbine generator and controller (docs/multiblocks/steam-plant.md). It finds the burner, drum, superheater,
 * turbine, condenser and feed pump; fires the burner; raises the drum to pressure (5 minutes from cold at full fire); rolls the
 * turbine up to 3,600 rpm (3,000 at 50 Hz) once the drum is at pressure and fed; synchronizes; and makes 4.16 kV three-phase
 * (3.3 kV on IEC worlds) for the GSU. Without a cooling tower on the condenser's water the back pressure takes a third more steam,
 * so the plant makes at most three quarters of its rating. A low drum level trips it.
 */
public class TurbineGeneratorBlockEntity extends BlockEntity implements CircuitPart, Generator, Inspectable, dev.wildcatter.multiblock.BigPartEntity {
	public enum Run {
		COLD, FIRING, ROLLING, RUNNING
	}

	private static final double FULL_FIRE_WATTS = Tuning.PLANT_WATTS / Tuning.PLANT_EFFICIENCY;

	private final ThreePhaseSource phases = new ThreePhaseSource();
	/** Grown into its true-size cells (see BigParts). */
	private boolean grown;
	private final StatorHeating stator = new StatorHeating();
	private final Synchronizer sync = new Synchronizer();
	private final Spin spin = new Spin();
	private List<BlockPos> parts = List.of();
	private boolean formed;
	private String missing = "";
	private Run run = Run.COLD;
	private boolean wantRun;
	private boolean breakerClosed;
	private boolean tripped;
	private String tripReason = "";
	/** 0 to 1: drum pressure. */
	private double heat;
	/** 0 to 1: how hard the burner is firing, as of the last tick. */
	private double firing;
	private double drumKg;
	private double hotwellKg;
	private boolean cooled;
	private double volts;
	private double amps;
	private double outWatts;
	private double hertz = ElectricalStandard.active().hertz();
	private String fuelNote = "";

	public TurbineGeneratorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.TURBINE_GENERATOR_ENTITY, pos, state);
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

	private Direction facing() {
		return getBlockState().getValue(TurbineGeneratorBlock.FACING);
	}

	public Spin spin() {
		return spin;
	}

	public double ratedRpm() {
		return 120.0 * ElectricalStandard.active().hertz() / Tuning.TURBINE_POLES;
	}

	public double firing() {
		return firing;
	}

	public double loadShare() {
		return Math.max(0, Math.min(1, outWatts / Tuning.PLANT_WATTS));
	}

	public boolean formed() {
		return formed;
	}

	public Run run() {
		return run;
	}

	/** Start (light the burner) and stop; a trip is reset first. */
	public void pushButton() {
		if (tripped) {
			tripped = false;
			tripReason = "";
			stator.reset();
		} else {
			wantRun = !wantRun;
		}
		setChanged();
	}

	public void setWantRun(boolean value) {
		wantRun = value;
		setChanged();
	}

	/** Test hooks: water in the drum and hotwell, and drum pressure. */
	public void setWaterForTest(double drum, double hotwell) {
		drumKg = drum;
		hotwellKg = hotwell;
	}

	public void setHeatForTest(double value) {
		heat = value;
	}

	/** Makeup water from a pump on the condenser's pipe, in kg; returns what it took. */
	public double addMakeup(double kg) {
		double took = Math.min(kg, Tuning.HOTWELL_KG - hotwellKg);
		hotwellKg += Math.max(0, took);
		return Math.max(0, took);
	}

	public double hotwellLevel() {
		return hotwellKg / Tuning.HOTWELL_KG;
	}

	// ---- every tick -----------------------------------------------------------------------------------------------
	public static void tick(Level level, BlockPos pos, BlockState state, TurbineGeneratorBlockEntity g) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		long phase = level.getGameTime() + pos.hashCode();
		if (phase % Tuning.STRUCTURE_CHECK_TICKS == 0 || (g.formed && g.parts.isEmpty())) {
			g.checkStructure(server);
		}
		if (phase % Tuning.PLUMBING_INTERVAL == 0 && g.formed) {
			g.checkCooling(server);
		}
		g.step(server);
	}

	private void checkStructure(ServerLevel level) {
		PartLayout.Found found = SteamPlantLayout.layout().check(level, worldPosition, facing());
		if (found.parts() != null) {
			if (!formed) {
				level.playSound(null, worldPosition, WildcatterSounds.STRUCTURE_FORMED, SoundSource.BLOCKS, 1.5F, 0.8F);
			}
			formed = true;
			missing = "";
			for (BlockPos at : found.parts()) {
				if (level.getBlockEntity(at) instanceof SteamPlantPartBlockEntity part) {
					part.bind(worldPosition);
				}
			}
			parts = found.parts();
		} else {
			missing = found.missing();
			if (formed) {
				for (BlockPos at : parts) {
					if (level.getBlockEntity(at) instanceof SteamPlantPartBlockEntity part) {
						part.unbind();
					}
				}
				parts = List.of();
				formed = false;
				breakerClosed = false;
				run = Run.COLD;
				CircuitManager.markDirty(level);
			}
		}
		setChanged();
	}

	private SteamPlantPartBlockEntity part(Level level, SteamPlantPartBlock.Part kind) {
		for (BlockPos at : parts) {
			if (level.getBlockEntity(at) instanceof SteamPlantPartBlockEntity p && p.part() == kind) {
				return p;
			}
		}
		return null;
	}

	/** The condenser is cooled while a cooling tower's basin on its water pipe is cooling. */
	private void checkCooling(ServerLevel level) {
		SteamPlantPartBlockEntity condenser = part(level, SteamPlantPartBlock.Part.CONDENSER);
		cooled = false;
		if (condenser == null) {
			return;
		}
		List<BlockPos> cells = dev.wildcatter.multiblock.BigParts.cellsAt(level, condenser.getBlockPos());   // the water pipe on any cell
		Set<BlockPos> pipes = Plumbing.network(level, cells);
		for (TowerBasinBlockEntity basin : Plumbing.machines(level, cells, pipes, false, TowerBasinBlockEntity.class)) {
			cooled |= basin.cooling();
		}
	}

	/** Steam per unit of output: a third more without cooling (back pressure). */
	private double steamFactor() {
		return cooled ? 1.0 : 1.0 / Tuning.UNCOOLED_EFFICIENCY;
	}

	private void step(ServerLevel level) {
		double dt = 1.0 / 20.0;
		boolean wasLive = live();
		if (!formed || tripped) {
			run = formed && heat > 0 ? Run.COLD : Run.COLD;
			breakerClosed = false;
		}
		SteamPlantPartBlockEntity burner = formed ? part(level, SteamPlantPartBlock.Part.BURNER) : null;
		SteamPlantPartBlockEntity pump = formed ? part(level, SteamPlantPartBlock.Part.FEED_PUMP) : null;
		// steam the turbine is taking, as a share of the drum's full output
		double steamShare = live() ? loadShare() * steamFactor() : 0;
		// the burner: full fire to raise pressure, then as much as the steam takes, never below a low fire
		double fireWanted = !wantRun || !formed || tripped ? 0 : heat < 1.0 ? 1.0 : Math.max(Tuning.PLANT_MIN_FIRE, Math.min(1.0, steamShare + 0.02));
		double joulesWanted = fireWanted * FULL_FIRE_WATTS * dt;
		double got = 0;
		if (burner != null && joulesWanted > 0) {
			got = burner.burn(joulesWanted);
			if (got < joulesWanted) {
				got += drawGas(level, burner, joulesWanted - got);
			}
		}
		firing = got / (FULL_FIRE_WATTS * dt);
		fuelNote = burner == null ? "" : firing < fireWanted * 0.99 && fireWanted > 0 ? (wrongGas(level, burner) ? "OUT OF FUEL: its gas line carries unprocessed gas; the burner needs pipeline gas: process it in a gas plant"
				: "OUT OF FUEL: put coal in the burner or pipe pipeline gas or residue to it") : "";
		// drum pressure: the fire raises it, the steam drawn and the losses lower it
		heat += (firing - steamShare - 0.02) * dt / Tuning.PLANT_HEATUP_SECONDS;
		heat = Math.max(0, Math.min(1, heat));
		// water: steam leaves the drum, condensate comes back to the hotwell, the feed pump returns it to the drum
		double steamKg = outWatts / 1000.0 * Tuning.STEAM_KG_PER_KWH / 3600.0 * steamFactor() * dt;
		drumKg = Math.max(0, drumKg - steamKg);
		hotwellKg = Math.min(Tuning.HOTWELL_KG, hotwellKg + steamKg * Tuning.CONDENSATE_RETURN);
		if (pump != null) {
			pump.setWanted(formed && drumKg < Tuning.DRUM_WATER_KG * 0.95 && hotwellKg > 0);
			double moved = Math.min(Math.min(hotwellKg, Tuning.DRUM_WATER_KG - drumKg), Tuning.FEED_PUMP_KG_PER_S * pump.pumpOutput() * dt);
			hotwellKg -= moved;
			drumKg += moved;
		}
		if (live() && drumKg < Tuning.DRUM_WATER_KG * 0.1) {
			trip("on LOW DRUM LEVEL: the feed pump needs 480 V power and the condenser makeup water");
		}
		// the turbine: it rolls once the drum is at pressure and has water, then synchronizes
		double rated = ratedRpm();
		float rpm = spin.rpm();
		boolean steamReady = formed && !tripped && wantRun && heat >= 0.95 && drumKg >= Tuning.DRUM_WATER_KG * 0.5;
		switch (run) {
			case COLD, FIRING -> {
				run = !formed || tripped ? Run.COLD : wantRun ? (steamReady ? Run.ROLLING : Run.FIRING) : Run.COLD;
				rpm = (float) Math.max(0, rpm - rated / (Tuning.TURBINE_RUNUP_SECONDS * 20));
			}
			case ROLLING -> {
				if (!steamReady) {
					run = Run.FIRING;
				} else {
					rpm = (float) Math.min(rated, rpm + rated / (Tuning.TURBINE_RUNUP_SECONDS * 20));
					if (rpm >= rated) {
						run = Run.RUNNING;
						sync.reset(ElectricalStandard.active().hertz());
						CircuitManager.markDirty(level);
					}
				}
			}
			case RUNNING -> {
				if (!steamReady && !(heat >= 0.9 && wantRun)) {
					run = Run.FIRING;
					breakerClosed = false;
				} else if (!breakerClosed) {
					String why = sync.step(ElectricalStandard.active().hertz(), Voltage.MEDIUM);
					if (why != null) {
						breakerClosed = true;
						sync.closed(why);
						level.playSound(null, worldPosition, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 1.0F, 0.8F);
						CircuitManager.markDirty(level);
					}
				}
				rpm = (float) (120.0 * (breakerClosed ? hertz : sync.ownHertz()) / Tuning.TURBINE_POLES);
			}
		}
		spin.setRpm(rpm);
		if (spin.needsSync()) {
			spin.markSynced();
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
		if (live() != wasLive) {
			CircuitManager.markDirty(level);
		}
		setChanged();
	}

	/** Pipeline gas or residue at the burner (docs/multiblocks/gas-plant.md, crude-units.md): up to the heat it still needs, off any of its cells. */
	private double drawGas(ServerLevel level, SteamPlantPartBlockEntity burner, double joules) {
		List<BlockPos> cells = dev.wildcatter.multiblock.BigParts.cellsAt(level, burner.getBlockPos());   // the gas line on any cell
		double got = Lines.pull(level, Lines.faces(cells), Product.PIPELINE_GAS, joules / Tuning.GAS_JOULES_PER_M3, FluidNode.Pull.MACHINE, null)
				* Tuning.GAS_JOULES_PER_M3;
		// or residue, as fuel oil (docs/multiblocks/crude-units.md)
		return got + Lines.pull(level, Lines.faces(cells), Product.RESIDUE, (joules - got) / Tuning.FUEL_OIL_JOULES_PER_L, FluidNode.Pull.MACHINE, null)
				* Tuning.FUEL_OIL_JOULES_PER_L;
	}

	/** True if the burner's gas line carries a gas it will not burn. */
	private boolean wrongGas(ServerLevel level, SteamPlantPartBlockEntity burner) {
		dev.wildcatter.fluid.Run wrong = Lines.wrong(level, Lines.faces(dev.wildcatter.multiblock.BigParts.cellsAt(level, burner.getBlockPos())), Product.PIPELINE_GAS);
		return wrong != null && wrong.product() != null && wrong.product().gas();
	}

	private boolean live() {
		return formed && run == Run.RUNNING && breakerClosed && !tripped;
	}

	private void trip(String why) {
		tripped = true;
		tripReason = why;
		breakerClosed = false;
		run = Run.COLD;
		wantRun = false;
		if (level instanceof ServerLevel server) {
			server.playSound(null, worldPosition, WildcatterSounds.BREAKER_TRIP, SoundSource.BLOCKS, 1.2F, 0.7F);
			CircuitManager.markDirty(server);
		}
		setChanged();
	}

	// ---- the circuit ----------------------------------------------------------------------------------------------
	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return hasPortAt(worldPosition, face, role, voltage);
	}

	/** The 4.16 kV terminals: the generator's non-drive end, away from the turbine, on both bottom cells. */
	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.MEDIUM && face == TurbineGeneratorBlock.terminalFace(getBlockState())
				&& TurbineGeneratorBlock.terminalCells(worldPosition, getBlockState()).contains(cell);
	}

	@Override
	public void build(Circuit circuit) {
		sync.beginBuild();
		if (!live()) {
			if (formed && run == Run.RUNNING && !breakerClosed && !tripped) {
				sync.buildTransformers(circuit, TurbineGeneratorBlock.terminalCells(worldPosition, getBlockState()),
						List.of(TurbineGeneratorBlock.terminalFace(getBlockState())), Voltage.MEDIUM);
			}
			return;
		}
		double nominal = Voltage.MEDIUM.nominal();
		double ratedAmps = Tuning.PLANT_WATTS / Tuning.GENERATOR_RATED_PF / (3 * nominal);
		double ohms = nominal * Tuning.ENGINE_SAG / ratedAmps;
		phases.build(circuit, TurbineGeneratorBlock.terminalCells(worldPosition, getBlockState()),
				List.of(TurbineGeneratorBlock.terminalFace(getBlockState())), Voltage.MEDIUM, nominal, ohms);
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		if (!live()) {
			volts = amps = outWatts = 0;
			sync.read(circuit);
			return;
		}
		volts = phases.volts(circuit);
		amps = phases.amps(circuit);
		outWatts = phases.watts(circuit);
		hertz = circuit.hertz();
		if (stator.step(phases.phaseAmps(circuit), StatorHeating.ratedPhaseAmps(Tuning.PLANT_WATTS, Voltage.MEDIUM.nominal()), ticks / 20.0)) {
			trip(String.format("on stator overcurrent (phase %s at %.1fx its rating)", "ABC".charAt(stator.hottestPhase()), stator.worstRatio()));
		}
	}

	// ---- Generator ----------------------------------------------------------------------------------------------------
	@Override
	public double ratedWatts() {
		return Tuning.PLANT_WATTS * (cooled ? 1.0 : Tuning.UNCOOLED_EFFICIENCY);   // back pressure caps it at three quarters
	}

	@Override
	public double inertiaSeconds() {
		return Tuning.INERTIA_STEAM;
	}

	@Override
	public double deliveredWatts() {
		return outWatts;
	}

	@Override
	public boolean online() {
		return live();
	}

	@Override
	public void tripOnFrequency(double frequency) {
		trip(String.format("on frequency (%.1f Hz)", frequency));
	}

	@Override
	public Component inspect() {
		String name = "Steam plant (" + Math.round(Tuning.PLANT_WATTS / 1000) + " kW, " + Voltage.MEDIUM.label() + ")";
		if (!formed) {
			return Component.literal(name + ": NOT BUILT" + (missing.isEmpty() ? "" : ", needs a " + missing));
		}
		String state = tripped ? "TRIPPED " + tripReason + ": right-click to reset"
				: switch (run) {
					case COLD -> wantRun ? "lighting the burner" : spin.rpm() > 0 ? String.format("stopping, %.0f rpm", spin.rpm()) : "COLD: right-click to light the burner";
					case FIRING -> String.format("FIRING, drum at %d%% pressure", Math.round(heat * 100));
					case ROLLING -> String.format("turbine rolling, %.0f of %.0f rpm", spin.rpm(), ratedRpm());
					case RUNNING -> String.format("RUNNING %.0f rpm, %.1f Hz, %.0f V, %.0f kW (%d%%)", spin.rpm(), hertz, volts * Math.sqrt(3), outWatts / 1000, Math.round(loadShare() * 100));
				};
		String water = String.format(", drum water %.0f kg, hotwell %.0f kg, burner at %d%%", drumKg, hotwellKg, Math.round(firing * 100));
		String cool = cooled ? ", condensate COOLED" : ", NOT COOLED (no cooling tower on the condenser's water: 3/4 of rating at most)";
		String breaker = run == Run.RUNNING ? ", output breaker " + (breakerClosed ? "CLOSED" : "open") + (!breakerClosed && sync.busLive(Voltage.MEDIUM) ? "; " + sync.scope() : "") : "";
		String heating = stator.level() > 0.05 ? String.format(", stator phase %s heating %d%%", "ABC".charAt(stator.hottestPhase()), Math.round(stator.level() * 100)) : "";
		return Component.literal(name + ": " + state + water + cool + breaker + heating + (fuelNote.isEmpty() ? "" : "; " + fuelNote));
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.putFloat("rpm", spin.rpm());
		tag.putDouble("firing", firing);
		tag.putDouble("watts", outWatts);
		return tag;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("formed", formed);
		output.putBoolean("grown", grown);
		output.putString("run", run.name());
		output.putBoolean("wantRun", wantRun);
		output.putBoolean("breaker", breakerClosed);
		output.putBoolean("tripped", tripped);
		output.putString("tripReason", tripReason);
		output.putDouble("heat", heat);
		output.putDouble("drum", drumKg);
		output.putDouble("hotwell", hotwellKg);
		sync.save(output);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		formed = input.getBooleanOr("formed", formed);
		grown = input.getBooleanOr("grown", grown);
		try {
			run = Run.valueOf(input.getStringOr("run", run.name()));
		} catch (IllegalArgumentException e) {
			run = Run.COLD;
		}
		wantRun = input.getBooleanOr("wantRun", wantRun);
		breakerClosed = input.getBooleanOr("breaker", breakerClosed);
		tripped = input.getBooleanOr("tripped", tripped);
		tripReason = input.getStringOr("tripReason", tripReason);
		heat = input.getDoubleOr("heat", heat);
		drumKg = input.getDoubleOr("drum", drumKg);
		hotwellKg = input.getDoubleOr("hotwell", hotwellKg);
		spin.setRpm(input.getFloatOr("rpm", spin.rpm()));
		firing = input.getDoubleOr("firing", firing);
		outWatts = input.getDoubleOr("watts", outWatts);
		sync.load(input);
	}
}
