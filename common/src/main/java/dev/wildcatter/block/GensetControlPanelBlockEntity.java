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
 * The genset's control panel and controller (docs/multiblocks/genset.md): it finds the skid's parts, holds the governor, the
 * output breaker and the readout, burns the fuel, and makes 480Y/277 V three-phase (400Y/230 V on IEC worlds) at its terminals
 * (every face but the front). Right-click starts and stops the set: it cranks to rated speed in 10 s, then closes its breaker.
 */
public class GensetControlPanelBlockEntity extends BlockEntity implements CircuitPart, Generator, Inspectable, dev.wildcatter.multiblock.BigPartEntity {
	public enum Run {
		STOPPED, CRANKING, RUNNING
	}


	private final ThreePhaseSource phases = new ThreePhaseSource();
	private final StatorHeating stator = new StatorHeating();
	private final Spin spin = new Spin();
	private List<BlockPos> parts = List.of();
	private boolean formed;
	/** Grown into its true-size cells (see BigParts). */
	private boolean grown;
	private boolean gas;
	private Run run = Run.STOPPED;
	private boolean wantRun;
	private boolean breakerClosed;
	private boolean tripped;
	private String tripReason = "";
	private String missing = "";
	private String supply = "";
	private double gasBuffer;
	private double volts;
	private double amps;
	private double outWatts;
	private double hertz = ElectricalStandard.active().hertz();
	/** With the breaker open it reads the bus and tracks the set's slip against it (circuit/Synchronizer). */
	private final Synchronizer sync = new Synchronizer();

	public GensetControlPanelBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.GENSET_CONTROL_PANEL_ENTITY, pos, state);
	}

	private Direction facing() {
		return getBlockState().getValue(GensetControlPanelBlock.FACING);
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

	public boolean formed() {
		return formed;
	}

	public boolean gas() {
		return gas;
	}

	public Run run() {
		return run;
	}

	public boolean breakerClosed() {
		return breakerClosed;
	}

	public Spin spin() {
		return spin;
	}

	public double ratedRpm() {
		return 120.0 * ElectricalStandard.active().hertz() / Tuning.GENSET_POLES;
	}

	/** 0 to 1: the share of its rating the set is delivering. */
	public double loadShare() {
		return Math.max(0, Math.min(1, outWatts / ratedWatts()));
	}

	public double watts() {
		return outWatts;
	}

	/** Start and stop: right-click on the panel. A trip is reset first. */
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

	public Synchronizer.Mode syncMode() {
		return sync.mode();
	}

	/** Sneak and right-click: stopped, it switches AUTO and MANUAL synchronizing; running open in MANUAL, it steps the speed trim. */
	public void sneakButton() {
		if (run == Run.RUNNING && !breakerClosed && sync.mode() == Synchronizer.Mode.MANUAL) {
			sync.stepTrim();
		} else if (run == Run.STOPPED) {
			sync.toggleMode();
		}
		setChanged();
	}

	/** In MANUAL, running with the breaker open: right-click closes it, in sync or not. */
	public boolean wantsManualClose() {
		return sync.mode() == Synchronizer.Mode.MANUAL && run == Run.RUNNING && !breakerClosed && !tripped;
	}

	/** Test hooks for the synchronizer. */
	public void setSync(Synchronizer.Mode value) {
		sync.setMode(value);
		setChanged();
	}

	public void setSlipForTest(double degrees) {
		sync.setSlipForTest(degrees);
	}

	/** Test hook: gas already in the engine's line, in cubic metres, for a set with no well near it. */
	public void setGasForTest(double m3) {
		gasBuffer = m3;
		setChanged();
	}

	/** Test hook and remote start: run or stop. */
	public void setWantRun(boolean value) {
		wantRun = value;
		setChanged();
	}

	// ---- every tick -----------------------------------------------------------------------------------------------
	public static void tick(Level level, BlockPos pos, BlockState state, GensetControlPanelBlockEntity panel) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		long phase = level.getGameTime() + pos.hashCode();
		if (phase % 100 == 0) {
			dev.wildcatter.multiblock.BigParts.check(server, pos, state);   // an older world's one-block panel drops
			if (!(level.getBlockEntity(pos) instanceof GensetControlPanelBlockEntity)) {
				return;
			}
		}
		if (phase % Tuning.STRUCTURE_CHECK_TICKS == 0) {
			panel.checkStructure(server);
		}
		if (phase % Tuning.WELL_INTERVAL == 0 && panel.formed && panel.gas) {
			panel.drawGas(server);
		}
		panel.step(server);
	}

	private void checkStructure(ServerLevel level) {
		PartLayout.Found found = GensetSkid.layout().check(level, worldPosition, facing());
		if (found.parts() != null) {
			if (!formed || (found.variant() == 1) != gas) {
				level.playSound(null, worldPosition, WildcatterSounds.STRUCTURE_FORMED, SoundSource.BLOCKS, 1.2F, 0.9F);
			}
			formed = true;
			gas = (found.variant() == 1);
			missing = "";
			for (BlockPos at : found.parts()) {
				if (level.getBlockEntity(at) instanceof GensetPartBlockEntity part) {
					part.bind(worldPosition);
				}
			}
			parts = found.parts();
		} else {
			missing = found.missing();
			if (formed) {
				dissolve(level);
			}
		}
		setChanged();
	}

	private void dissolve(ServerLevel level) {
		for (BlockPos at : parts) {
			if (level.getBlockEntity(at) instanceof GensetPartBlockEntity part) {
				part.unbind();
			}
		}
		parts = List.of();
		formed = false;
		breakerClosed = false;
		run = Run.STOPPED;
		CircuitManager.markDirty(level);
	}

	private GensetPartBlockEntity tank(Level level) {
		for (BlockPos at : parts) {
			if (level.getBlockEntity(at) instanceof GensetPartBlockEntity part && part.part() == GensetPartBlock.Part.FUEL_TANK) {
				return part;
			}
		}
		return null;
	}

	private BlockPos engine() {
		for (BlockPos at : parts) {
			if (level != null && level.getBlockEntity(at) instanceof GensetPartBlockEntity part
					&& (part.part() == GensetPartBlock.Part.DIESEL_ENGINE || part.part() == GensetPartBlock.Part.GAS_ENGINE)) {
				return at;
			}
		}
		return worldPosition;
	}

	/** Separated gas: the engine draws from the gas run on any of its cells into a few seconds' buffer. Wet gas would wreck it. */
	private void drawGas(ServerLevel level) {
		double want = Tuning.GAS_FULL_LOAD_M3H / 3600.0 * 10 - gasBuffer;   // ten seconds at full load
		if (want <= 0) {
			return;
		}
		List<Lines.Face> faces = Lines.faces(dev.wildcatter.multiblock.BigParts.cellsAt(level, engine()));   // a gas line to any cell of the engine
		double got = Lines.pull(level, faces, Product.GAS, want, FluidNode.Pull.MACHINE, null);
		double piped = Lines.pull(level, faces, Product.PIPELINE_GAS, want - got, FluidNode.Pull.MACHINE, null);   // a gas plant's gas does as well
		got += piped;
		gasBuffer += got;
		dev.wildcatter.fluid.Run wrong = got > 0 ? null : Lines.wrong(level, faces, Product.GAS);
		if (wrong != null && wrong.carries(Product.PIPELINE_GAS)) {
			wrong = null;
		}
		supply = piped > 0 ? "pipeline gas" : got > 0 || gasBuffer > 0 ? "gas from a separator"
				: wrong != null && wrong.carries(Product.WET_GAS) ? "WET GAS: put a separator between the well and the engine"
				: wrong != null ? "NO GAS: its gas line is " + wrong.describe()
				: "NO GAS: run a gas line from a separator's gas side to the engine";
	}

	/** Diesel: the tank is topped up from a stock tank of diesel or a crude unit's diesel nozzle on its steel pipe. */
	private void refillDiesel(ServerLevel level, GensetPartBlockEntity tank) {
		double room = Tuning.GENSET_TANK_LITERS - tank.liters();
		if (room < 1) {
			return;
		}
		List<BlockPos> cells = dev.wildcatter.multiblock.BigParts.cellsAt(level, tank.getBlockPos());   // a line to any cell of the tank
		tank.addFuel(Lines.pull(level, Lines.faces(cells), Product.DIESEL, room, FluidNode.Pull.MACHINE, null));
	}

	/** Fuel for one tick at the present load: 15 % of full-load fuel at no load, rising in a straight line to full. */
	private double fuelPerTick() {
		double fullPerSecond = (gas ? Tuning.GAS_FULL_LOAD_M3H : Tuning.DIESEL_FULL_LOAD_LPH) / 3600.0;
		return fullPerSecond * (Tuning.GENSET_NO_LOAD_FUEL + (1 - Tuning.GENSET_NO_LOAD_FUEL) * loadShare()) / 20.0;
	}

	private void step(ServerLevel level) {
		if (formed && parts.isEmpty()) {
			checkStructure(level);   // just loaded: find the parts again before burning fuel from the tank
		}
		boolean wasLive = live();
		if (formed && !gas && level.getGameTime() % Tuning.WELL_INTERVAL == 0) {
			GensetPartBlockEntity tank = tank(level);
			if (tank != null) {
				refillDiesel(level, tank);
				supply = tank.liters() < 0.5 ? "OUT OF FUEL: the tank is dry, pour in diesel or pipe it from a diesel tanky"
						: String.format("%.0f of %.0f L of diesel", tank.liters(), Tuning.GENSET_TANK_LITERS);
			}
		}
		if (formed && alternatorDamaged(level)) {
			wantRun = false;
			supply = "ALTERNATOR DAMAGED by an out-of-sync close: break it and fit a new one";
		}
		if (!formed || tripped) {
			wantRun = wantRun && formed;
			run = Run.STOPPED;
		}
		// fuel
		if (run != Run.STOPPED) {
			double need = fuelPerTick();
			double got;
			if (gas) {
				if (gasBuffer < need) {
					drawGas(level);
				}
				got = Math.min(gasBuffer, need);
				gasBuffer -= got;
			} else {
				GensetPartBlockEntity tank = tank(level);
				got = tank == null ? 0 : tank.take(need);
			}
			if (got < need * 0.999) {
				run = Run.STOPPED;
				wantRun = false;
				tripReason = "";
				supply = (gas ? "OUT OF GAS" : "OUT OF FUEL") + ": the set stopped";
			}
		}
		// governor and starter
		double rated = ratedRpm();
		float rpm = spin.rpm();
		if (wantRun && formed && !tripped && run == Run.STOPPED) {
			run = Run.CRANKING;
			level.playSound(null, engine(), WildcatterSounds.ENGINE_CRANK, SoundSource.BLOCKS, 1.5F, gas ? 1.2F : 1.0F);
		} else if (!wantRun && run != Run.STOPPED) {
			run = Run.STOPPED;
		}
		switch (run) {
			case CRANKING -> {
				rpm = (float) Math.min(rated, rpm + rated / (Tuning.GENSET_START_SECONDS * 20));
				if (rpm >= rated) {
					run = Run.RUNNING;
					sync.reset(ElectricalStandard.active().hertz());
					CircuitManager.markDirty(level);   // its potential transformers go on the bus
				}
			}
			case RUNNING -> {
				if (!breakerClosed) {
					String why = sync.step(ElectricalStandard.active().hertz(), Voltage.INDUSTRIAL);
					if (why != null) {
						close(level, why);
					}
				}
				rpm = (float) (120.0 * (breakerClosed ? hertz : sync.ownHertz()) / Tuning.GENSET_POLES);
			}
			case STOPPED -> {
				breakerClosed = false;
				sync.reset(ElectricalStandard.active().hertz());
				rpm = (float) Math.max(0, rpm - rated / (Tuning.GENSET_STOP_SECONDS * 20));
			}
		}
		spin.setRpm(rpm);
		if (spin.needsSync()) {
			spin.markSynced();
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
			setChanged();
		}
		if (live() != wasLive) {
			CircuitManager.markDirty(level);
		}
	}

	private GensetPartBlockEntity alternator(Level level) {
		for (BlockPos at : parts) {
			if (level.getBlockEntity(at) instanceof GensetPartBlockEntity part && part.part() == GensetPartBlock.Part.ALTERNATOR) {
				return part;
			}
		}
		return null;
	}

	private boolean alternatorDamaged(Level level) {
		GensetPartBlockEntity a = alternator(level);
		return a != null && a.damaged();
	}

	/** Right-click in MANUAL: close now. Outside the window the set is slammed into step: a fault, a trip, maybe a wrecked alternator. */
	public void manualClose() {
		if (!(level instanceof ServerLevel server) || !wantsManualClose() || !sync.busRead()) {
			return;
		}
		boolean live = sync.busLive(Voltage.INDUSTRIAL);
		if (!live || sync.inWindow(Voltage.INDUSTRIAL)) {
			close(server, live ? String.format("by hand, in sync (%.0f degrees)", sync.slipDegrees()) : "by hand onto a dead bus");
			return;
		}
		double slipDegrees = sync.slipDegrees();
		double pu = sync.closingFaultPu();
		boolean wrecked = pu > Tuning.SYNC_DAMAGE_PU;
		server.playSound(null, worldPosition, WildcatterSounds.SYNC_BANG, SoundSource.BLOCKS, 2.0F, 0.8F);
		server.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, 10, 0.3, 0.2, 0.3, 0.02);
		if (wrecked) {
			GensetPartBlockEntity a = alternator(server);
			if (a != null) {
				a.damage();
			}
		}
		sync.closed(String.format("out of sync, %.0f degrees apart: %.1f per unit fault", slipDegrees, pu));
		trip(String.format("on an OUT-OF-SYNC CLOSE (%.0f degrees apart, %.1f per unit)%s", slipDegrees, pu, wrecked ? "; the alternator is DAMAGED" : ""));
		wantRun = false;
	}

	private void close(ServerLevel level, String how) {
		breakerClosed = true;
		sync.closed(how);
		level.playSound(null, worldPosition, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 1.0F, 0.9F);
		CircuitManager.markDirty(level);
		setChanged();
	}

	/** Making power at its terminals. */
	private boolean live() {
		return formed && run == Run.RUNNING && breakerClosed && !tripped;
	}

	// ---- the circuit ----------------------------------------------------------------------------------------------
	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return hasPortAt(worldPosition, face, role, voltage);
	}

	/** The output terminals: the panel's east end face (for a set facing north), on both of its bottom cells. */
	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.INDUSTRIAL && face == GensetControlPanelBlock.terminalFace(getBlockState())
				&& GensetControlPanelBlock.terminalCells(worldPosition, getBlockState()).contains(cell);
	}

	@Override
	public void build(Circuit circuit) {
		sync.beginBuild();
		if (!live()) {
			if (formed && run == Run.RUNNING && !breakerClosed && !tripped) {
				sync.buildTransformers(circuit, GensetControlPanelBlock.terminalCells(worldPosition, getBlockState()),
						List.of(GensetControlPanelBlock.terminalFace(getBlockState())), Voltage.INDUSTRIAL);
			}
			return;
		}
		double nominal = Voltage.INDUSTRIAL.nominal();
		double ratedAmps = ratedWatts() / Tuning.GENERATOR_RATED_PF / (3 * nominal);
		double ohms = nominal * Tuning.ENGINE_SAG / ratedAmps;   // sags ENGINE_SAG at full current
		phases.build(circuit, GensetControlPanelBlock.terminalCells(worldPosition, getBlockState()),
				List.of(GensetControlPanelBlock.terminalFace(getBlockState())), Voltage.INDUSTRIAL, nominal, ohms);
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
		if (stator.step(phases.phaseAmps(circuit), StatorHeating.ratedPhaseAmps(ratedWatts(), Voltage.INDUSTRIAL.nominal()), ticks / 20.0)) {
			trip(String.format("on stator overcurrent (phase %s at %.1fx its rating)", "ABC".charAt(stator.hottestPhase()), stator.worstRatio()));
		}
	}

	private void trip(String why) {
		tripped = true;
		tripReason = why;
		breakerClosed = false;
		run = Run.STOPPED;
		setChanged();
		if (level instanceof ServerLevel server) {
			server.playSound(null, worldPosition, WildcatterSounds.BREAKER_TRIP, SoundSource.BLOCKS, 1.0F, 0.8F);
			CircuitManager.markDirty(server);
		}
	}

	// ---- Generator: the island's frequency ----------------------------------------------------------------------------
	@Override
	public double ratedWatts() {
		return gas ? Tuning.GENSET_GAS_WATTS : Tuning.GENSET_DIESEL_WATTS;
	}

	@Override
	public double inertiaSeconds() {
		return Tuning.INERTIA_GENSET;
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

	public boolean isTripped() {
		return tripped;
	}

	@Override
	public Component inspect() {
		String name = (gas ? "Gas genset (" + Math.round(Tuning.GENSET_GAS_WATTS / 1000) : "Diesel genset (" + Math.round(Tuning.GENSET_DIESEL_WATTS / 1000))
				+ " kW, " + Voltage.INDUSTRIAL.label() + ")";
		if (!formed) {
			return Component.literal(name + ": NOT BUILT" + (missing.isEmpty() ? "" : ", needs the " + missing));
		}
		String state = tripped ? "TRIPPED " + tripReason + ": fix the cause, then right-click to reset"
				: switch (run) {
					case STOPPED -> spin.rpm() > 0 ? String.format("stopping, %.0f rpm", spin.rpm()) : "STOPPED: right-click to start";
					case CRANKING -> String.format("cranking, %.0f of %.0f rpm", spin.rpm(), ratedRpm());
					case RUNNING -> String.format("RUNNING %.0f rpm, %.1f Hz, %.0f V, %.0f kW (%d%%)", spin.rpm(), hertz, volts * Math.sqrt(3), outWatts / 1000, Math.round(loadShare() * 100));
				};
		String breaker = ", output breaker " + (breakerClosed ? "CLOSED" : "open") + ", " + sync.mode() + " sync";
		if (run == Run.RUNNING && !breakerClosed && sync.busLive(Voltage.INDUSTRIAL)) {
			breaker += "; " + sync.scope();
		}
		if (!sync.lastClose().isEmpty()) {
			breaker += "; last close " + sync.lastClose();
		}
		String heating = stator.level() > 0.05 ? String.format(", stator phase %s at %.0f%% of rating, heating %d%%", "ABC".charAt(stator.hottestPhase()), stator.worstRatio() * 100, Math.round(stator.level() * 100)) : "";
		return Component.literal(name + ": " + state + breaker + heating + (supply.isEmpty() ? "" : "; " + supply));
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("formed", formed);
		output.putBoolean("grown", grown);
		output.putBoolean("gas", gas);
		output.putString("run", run.name());
		output.putBoolean("wantRun", wantRun);
		output.putBoolean("breaker", breakerClosed);
		output.putBoolean("tripped", tripped);
		output.putString("tripReason", tripReason);
		output.putDouble("gasBuffer", gasBuffer);
		output.putFloat("rpm", spin.rpm());
		output.putDouble("watts", outWatts);
		sync.save(output);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		formed = input.getBooleanOr("formed", false);
		grown = input.getBooleanOr("grown", false);
		gas = input.getBooleanOr("gas", false);
		try {
			run = Run.valueOf(input.getStringOr("run", "STOPPED"));
		} catch (IllegalArgumentException e) {
			run = Run.STOPPED;
		}
		wantRun = input.getBooleanOr("wantRun", false);
		breakerClosed = input.getBooleanOr("breaker", false);
		tripped = input.getBooleanOr("tripped", false);
		tripReason = input.getStringOr("tripReason", "");
		gasBuffer = input.getDoubleOr("gasBuffer", 0);
		spin.setRpm(input.getFloatOr("rpm", 0));
		outWatts = input.getDoubleOr("watts", 0);
		sync.load(input);
	}
}
