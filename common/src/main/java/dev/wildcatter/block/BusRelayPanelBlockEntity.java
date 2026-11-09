package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.BusProtection;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.multiblock.BigParts;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The generator bus's relay panel (docs/multiblocks/gen-bus-protection.md). Each time the circuit is built it finds its zone: the
 * 13.8 kV bus duct it stands against, up to the generator breakers and feeder breakers bounding it. Then on each solve:
 * <ul>
 * <li>87B: the current flowing into a fault inside the zone (by Kirchhoff, what enters the bus and does not leave it at a breaker)
 * against its percentage restraint trips every breaker on the bus, and 86B locks them out;</li>
 * <li>50 on each feeder breaker trips that breaker on a heavy fault past it;</li>
 * <li>after the solve, 51V on each generator breaker and 51 on each feeder breaker run their inverse-time curves.</li>
 * </ul>
 * It has no terminals: its CTs read the currents of the breakers it protects.
 */
public class BusRelayPanelBlockEntity extends BlockEntity implements CircuitPart, Inspectable, BigPartEntity {
	private boolean grown;
	private final List<BlockPos> ducts = new ArrayList<>();
	private final List<BlockPos> generators = new ArrayList<>();   // generator breakers' main cells
	private final List<BlockPos> feeders = new ArrayList<>();      // feeder breakers' main cells
	private final Map<BlockPos, BusProtection.TimeElement> elements = new HashMap<>();
	private final Set<BlockPos> openedByUs = new HashSet<>();
	private boolean lockedOut;
	private String lastTrip = "";
	private double difference;
	private final Map<BlockPos, Double> reading = new HashMap<>();

	public BusRelayPanelBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.BUS_RELAY_PANEL_ENTITY, pos, state);
	}

	public boolean lockedOut() {
		return lockedOut;
	}

	/** True if it has opened something its reset would close again: after a lockout, or a feeder's own 50/51 trip. */
	public boolean hasTripped() {
		return lockedOut || !openedByUs.isEmpty();
	}

	public String lastTrip() {
		return lastTrip;
	}

	/** Its zone, found again: the generator breakers and feeder breakers bounding the bus duct it stands against. */
	public int[] zone() {
		findZone();
		return new int[] {generators.size(), feeders.size(), ducts.size()};
	}

	private void findZone() {
		ducts.clear();
		generators.clear();
		feeders.clear();
		if (level == null) {
			return;
		}
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> seen = new HashSet<>();
		for (BlockPos cell : BigParts.cells(worldPosition, getBlockState())) {
			for (Direction d : Direction.values()) {
				BlockPos n = cell.relative(d);
				if (level.getBlockState(n).is(WildcatterBlocks.BUS_DUCT_GEN) && seen.add(n)) {
					queue.add(n);
				}
			}
		}
		while (!queue.isEmpty() && ducts.size() < 512) {
			BlockPos here = queue.poll();
			ducts.add(here);
			for (Direction d : Direction.values()) {
				BlockPos n = here.relative(d);
				if (!seen.add(n)) {
					continue;
				}
				if (level.getBlockState(n).is(WildcatterBlocks.BUS_DUCT_GEN)) {
					queue.add(n);
				} else if (BigParts.entity(level, n) instanceof GenBreakerBlockEntity g && !generators.contains(g.getBlockPos())) {
					generators.add(g.getBlockPos());
				} else if (BigParts.entity(level, n) instanceof BreakerBlockEntity b && b.getBlockState().is(WildcatterBlocks.GEN_FEEDER_BREAKER)
						&& !feeders.contains(b.getBlockPos())) {
					feeders.add(b.getBlockPos());
				}
			}
		}
		Set<BlockPos> watched = new HashSet<>(generators);
		watched.addAll(feeders);
		elements.keySet().retainAll(watched);
		feederElements.keySet().retainAll(watched);
	}

	private final Map<BlockPos, dev.wildcatter.circuit.RelaySettings.Element> feederElements = new HashMap<>();
	/** 50BF: how long each feeder that failed to open has been carrying since. */
	private final Map<BlockPos, Double> failedFor = new HashMap<>();

	private dev.wildcatter.circuit.RelaySettings.Element feederElement(BlockPos at) {
		return feederElements.computeIfAbsent(at, k -> new dev.wildcatter.circuit.RelaySettings.Element());
	}

	private BusProtection.TimeElement element(BlockPos at, boolean veryInverse) {
		return elements.computeIfAbsent(at, k -> new BusProtection.TimeElement(veryInverse));
	}

	private static double genBreakerRating(GenBreakerBlockEntity g) {
		return g.getBlockState().is(WildcatterBlocks.GEN_BREAKER_3000) ? 3000 : 1200;
	}

	private GenBreakerBlockEntity genBreaker(BlockPos at) {
		return level != null && level.getBlockEntity(at) instanceof GenBreakerBlockEntity g ? g : null;
	}

	private BreakerBlockEntity feeder(BlockPos at) {
		return level != null && level.getBlockEntity(at) instanceof BreakerBlockEntity b ? b : null;
	}

	// ---- the circuit ----------------------------------------------------------------------------------------------------------------
	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	@Override
	public void build(Circuit circuit) {
		findZone();
	}

	/** 87B and the feeders' 50: they act on the solve in which the fault appears. */
	@Override
	public boolean protect(Circuit circuit, int ticks) {
		if (lockedOut || ducts.isEmpty() || level == null) {
			return false;
		}
		double fault = 0;
		for (BlockPos at : ducts) {
			if (level.getBlockEntity(at) instanceof MultiCableBlockEntity duct && duct.grounded()) {
				fault += duct.groundAmps(circuit);
			}
		}
		double boundary = 0;
		double largest = 0;
		for (BlockPos at : generators) {
			GenBreakerBlockEntity g = genBreaker(at);
			if (g != null) {
				boundary += g.phaseAmps(circuit);
				largest = Math.max(largest, genBreakerRating(g));
			}
		}
		for (BlockPos at : feeders) {
			BreakerBlockEntity b = feeder(at);
			if (b != null) {
				boundary += b.closed() ? b.mostAmps(circuit) : 0;
				largest = Math.max(largest, b.rating());
			}
		}
		difference = fault;
		if (BusProtection.differentialOperates(fault, boundary, largest)) {
			tripBus(String.format("87B BUS DIFFERENTIAL TRIP: %,.0f A in the bus (pickup %,.0f A)", fault, BusProtection.differentialPickup(boundary, largest)));
			return true;
		}
		boolean tripped = false;
		for (BlockPos at : feeders) {
			BreakerBlockEntity b = feeder(at);
			if (b != null && b.closed()) {
				double amps = b.mostAmps(circuit);
				if (b.failedToOpen()) {
					// 50BF: the feeder breaker was told to open and is still carrying after the failure timer: the whole bus opens (86BF)
					if (amps > Tuning.BF_DETECTOR_AMPS && failedFor.getOrDefault(at, 0.0) >= Tuning.BF_SECONDS) {
						tripBus(String.format("50BF BREAKER FAILURE: the feeder at %s failed to open on %,.0f A", at.toShortString(), amps));
						failedFor.remove(at);
						return true;
					}
					continue;
				}
				var relay = b.feederRelay();
				if (relay.instantaneous() > 0 && amps > relay.instantaneous() * b.feederPickup()) {
					b.seeFault(amps);
					b.tripByRelay(String.format("50 instantaneous, %,.0f A", amps));
					openedByUs.add(at);
					lastTrip = String.format("feeder at %d, %d, %d: 50 instantaneous, %,.0f A", at.getX(), at.getY(), at.getZ(), amps);
					tripped |= !b.closed();
				}
			}
		}
		if (tripped) {
			setChanged();
		}
		return tripped;
	}

	/** 51V on each generator breaker and 51 on each feeder breaker, after the solve. */
	@Override
	public void apply(Circuit circuit, int ticks) {
		if (lockedOut || level == null) {
			return;
		}
		double dt = ticks / 20.0;
		for (BlockPos at : generators) {
			GenBreakerBlockEntity g = genBreaker(at);
			GeneratorPartBlockEntity gen = g == null ? null : g.generator();
			if (gen == null || !gen.breakerClosed()) {
				element(at, true).reset();
				continue;
			}
			double pickup = BusProtection.restrainedPickup(gen.fullLoadAmps(), gen.voltsPerUnit());
			reading.put(at, gen.phaseAmps());
			if (element(at, true).step(gen.phaseAmps(), pickup, Tuning.GEN_51V_TIME_DIAL, dt)) {
				gen.trip(String.format("by the bus relay panel (51V: %,.0f A, pickup %,.0f A at %.0f%% voltage)", gen.phaseAmps(), pickup, gen.voltsPerUnit() * 100));
				lastTrip = "51V on the generator breaker at " + at.toShortString();
				element(at, true).reset();
				setChanged();
			}
		}
		for (BlockPos at : feeders) {
			BreakerBlockEntity b = feeder(at);
			if (b == null || !b.closed()) {
				feederElement(at).reset();
				failedFor.remove(at);
				continue;
			}
			double amps = b.amps();
			reading.put(at, amps);
			if (b.failedToOpen()) {
				failedFor.merge(at, dt, Double::sum);
				if (amps <= Tuning.BF_DETECTOR_AMPS) {
					failedFor.put(at, 0.0);
				}
				continue;
			}
			var relay = b.feederRelay();
			if (feederElement(at).step(amps, b.feederPickup(), relay.curve(), relay.dial(), dt)) {
				b.tripByRelay(String.format("51 time overcurrent, %,.0f A on a %,d A pickup", amps, b.feederPickup()));
				openedByUs.add(at);
				lastTrip = "51 on the feeder at " + at.toShortString();
				feederElement(at).reset();
				CircuitManager.markDirty(level);
				setChanged();
			}
		}
	}

	/** 87B: every breaker on the bus opens, and 86B holds them. */
	private void tripBus(String why) {
		for (BlockPos at : generators) {
			GenBreakerBlockEntity g = genBreaker(at);
			GeneratorPartBlockEntity gen = g == null ? null : g.generator();
			if (gen != null && !gen.tripped()) {
				gen.trip("by the bus relay panel (87B bus differential, 86B lockout)");
				openedByUs.add(gen.getBlockPos());
			}
		}
		for (BlockPos at : feeders) {
			BreakerBlockEntity b = feeder(at);
			if (b != null && b.closed()) {
				b.tripByRelay("87B bus differential, 86B lockout");
				openedByUs.add(at);
			}
		}
		lockedOut = true;
		lastTrip = why;
		setChanged();
	}

	/** The panel's reset: 86B released, the feeder breakers it opened closed again, the generators it tripped reset to resynchronize. */
	public void reset() {
		if (level == null) {
			return;
		}
		lockedOut = false;
		for (BlockPos at : openedByUs) {
			if (level.getBlockEntity(at) instanceof BreakerBlockEntity b) {
				b.closeByRelay();
			} else if (level.getBlockEntity(at) instanceof GeneratorPartBlockEntity gen) {
				gen.reset();
			}
		}
		openedByUs.clear();
		elements.values().forEach(BusProtection.TimeElement::reset);
		feederElements.values().forEach(dev.wildcatter.circuit.RelaySettings.Element::reset);
		failedFor.clear();
		CircuitManager.markDirty(level);
		setChanged();
	}

	// ---- the readout --------------------------------------------------------------------------------------------------------------
	@Override
	public Component inspect() {
		if (level != null && !level.isClientSide()) {
			findZone();
		}
		if (ducts.isEmpty()) {
			return Component.literal("Bus relay panel: NO BUS: set it against the " + Voltage.GENERATOR.label() + " bus duct");
		}
		String zone = String.format("Bus relay panel: %s bus, %d generator breaker%s, %d feeder%s", Voltage.GENERATOR.label(), generators.size(),
				generators.size() == 1 ? "" : "s", feeders.size(), feeders.size() == 1 ? "" : "s");
		if (lockedOut) {
			return Component.literal(zone + "; " + lastTrip + ", LOCKED OUT (86B); right-click to reset once the fault is cleared");
		}
		StringBuilder text = new StringBuilder(zone).append(String.format("; 87B ready (%,.0f A difference)", difference));
		for (BlockPos at : generators) {
			GenBreakerBlockEntity g = genBreaker(at);
			GeneratorPartBlockEntity gen = g == null ? null : g.generator();
			if (gen != null) {
				text.append(String.format("; 51V generator %,.0f A of pickup %,.0f A", gen.phaseAmps(), BusProtection.restrainedPickup(gen.fullLoadAmps(), gen.voltsPerUnit())));
			}
		}
		int n = 1;
		for (BlockPos at : feeders) {
			BreakerBlockEntity b = feeder(at);
			if (b != null) {
				text.append(String.format("; feeder %d (%,d A, %s) %s", n++, b.rating(), b.feederRelay().describe(b.rating(), false),
						b.failedToOpen() ? "FAILED TO OPEN" : b.closed() ? String.format("%,.0f A", b.amps()) : "OPEN"));
			}
		}
		if (!lastTrip.isEmpty()) {
			text.append("; last trip: ").append(lastTrip);
		}
		return Component.literal(text.toString());
	}

	// ---- saving ------------------------------------------------------------------------------------------------------------------
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
		output.putBoolean("lockedOut", lockedOut);
		output.putString("lastTrip", lastTrip);
		output.putInt("opened", openedByUs.size());
		int i = 0;
		for (BlockPos at : openedByUs) {
			output.putLong("opened" + i++, at.asLong());
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", grown);
		lockedOut = input.getBooleanOr("lockedOut", false);
		lastTrip = input.getStringOr("lastTrip", "");
		openedByUs.clear();
		for (int i = 0; i < input.getIntOr("opened", 0); i++) {
			openedByUs.add(BlockPos.of(input.getLongOr("opened" + i, 0)));
		}
	}
}
