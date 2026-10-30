package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.EnergyBridge;
import dev.wildcatter.energy.HostEnergy;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.PlugType;
import dev.wildcatter.energy.Voltage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The plug draws real current from the grid: as many watts as the machines beside it would take (1 W per FE/t by default), no
 * more than the circuit gives. It has no protection of its own, so the breaker or fuse upstream trips on an overload. Held above its
 * rating it heats the receptacle until both burn out. On the wrong voltage it delivers nothing.
 */
public class PlugAdapterBlockEntity extends BlockEntity implements CircuitPart, Inspectable {
	private static final int INTERVAL = 4;

	/** The game test shrinks the rating and the time to burn. Null in play. */
	public static volatile Double testRatingScale;
	public static volatile Double testBurnSeconds;

	private int hotA;
	private int hotB;
	private int neutral;
	private int load = -1;
	private double volts;
	private double amps;
	private double demandWatts;
	private boolean wrongVoltage;
	private int burnTicks;
	private long delivered;
	private String status = "starting";

	public PlugAdapterBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.PLUG_ADAPTER_ENTITY, pos, state);
	}

	public PlugType type() {
		return ((PlugAdapterBlock) getBlockState().getBlock()).type();
	}

	/** The faces that touch a matching receptacle: the plug is wired to the grid only there. */
	private boolean matchingFace(Direction face) {
		return level != null && level.getBlockState(worldPosition.relative(face)).getBlock() instanceof ReceptacleBlock receptacle && receptacle.type() == type();
	}

	@Override
	public boolean hasPort(Direction face, Conductor conductor, Voltage voltage) {
		return false;   // a plug is not a place to run wire to
	}

	@Override
	public void build(Circuit circuit) {
		hotA = circuit.node();
		hotB = type().betweenLegs() ? circuit.node() : hotA;
		neutral = circuit.node();
		double watts = wrongVoltage ? 0 : demandWatts;
		double volts = type().volts();
		if (type().betweenLegs()) {
			load = circuit.resistor(hotA, hotB, watts > 0 ? volts * volts / watts : 1e9);
		} else {
			load = circuit.resistor(hotA, neutral, watts > 0 ? volts * volts / watts : 1e9);
		}
		for (Direction face : Direction.values()) {
			if (matchingFace(face)) {
				circuit.port(worldPosition, face, Conductor.A, Voltage.LOW, hotA, Tuning.CONTACT_OHMS / 2, false);
				circuit.port(worldPosition, face, Conductor.B, Voltage.LOW, hotB, Tuning.CONTACT_OHMS / 2, false);
				circuit.port(worldPosition, face, Conductor.N, Voltage.LOW, neutral, Tuning.CONTACT_OHMS / 2, false);
			}
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		volts = type().betweenLegs() ? Math.abs(circuit.across(hotA, hotB)) : Math.abs(circuit.across(hotA, neutral));
		amps = load >= 0 ? Math.abs(circuit.current(load)) : 0;
		boolean wrong = Math.abs(volts - type().volts()) > type().volts() * 0.15;
		if (wrong != wrongVoltage) {
			wrongVoltage = wrong;
		}
		double rating = type().amps() * (testRatingScale != null ? testRatingScale : 1.0);
		if (amps > rating * 1.2) {
			burnTicks += ticks;
		} else {
			burnTicks = Math.max(0, burnTicks - ticks);
		}
	}

	private List<HostEnergy> sinks() {
		List<HostEnergy> found = new ArrayList<>();
		if (level == null) {
			return found;
		}
		for (Direction face : Direction.values()) {
			if (!matchingFace(face)) {
				HostEnergy sink = EnergyBridge.find(level, worldPosition.relative(face));
				if (sink != null) {
					found.add(sink);
				}
			}
		}
		return found;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, PlugAdapterBlockEntity plug) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		double burnSeconds = testBurnSeconds != null ? testBurnSeconds : Tuning.RECEPTACLE_BURN_SECONDS;
		if (plug.burnTicks >= burnSeconds * 20) {
			server.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 14, 0.2, 0.2, 0.2, 0.05);
			server.playSound(null, pos, net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.2F);
			for (Direction face : Direction.values()) {
				if (plug.matchingFace(face)) {
					server.destroyBlock(pos.relative(face), false);   // the receptacle burns with it
				}
			}
			server.destroyBlock(pos, false);
			dev.wildcatter.circuit.CircuitManager.markDirty(server);
			return;
		}
		if ((level.getGameTime() + pos.hashCode()) % INTERVAL != 0) {
			return;
		}
		List<HostEnergy> sinks = plug.sinks();
		long demand = 0;
		for (HostEnergy sink : sinks) {
			demand += Math.max(0, sink.demand());
		}
		double wattsPerFe = EnergyBridge.wattsPerFe();
		double wanted = demand * wattsPerFe;
		if (Math.abs(wanted - plug.demandWatts) > 0.5) {
			plug.demandWatts = wanted;
			dev.wildcatter.circuit.CircuitManager.markDirty(server);
		}
		// hand over what the grid actually delivered since the last visit, as FE per tick for the interval
		double watts = plug.wrongVoltage ? 0 : plug.volts * plug.amps;
		long fe = Math.round(watts / wattsPerFe * INTERVAL);
		for (HostEnergy sink : sinks) {
			long share = demand > 0 ? Math.round(fe * (double) Math.max(0, sink.demand()) / demand) : 0;
			plug.delivered += sink.insert(share);
		}
		plug.status = sinks.isEmpty() ? "nothing beside it takes power"
				: plug.wrongVoltage ? "WRONG VOLTAGE: this plug needs " + Math.round(plug.type().volts()) + " V"
				: String.format("delivering %.0f W (%.0f FE/t)", watts, watts / wattsPerFe);
		plug.setChanged();
	}

	@Override
	public Component inspect() {
		String warning = burnTicks > 20 ? String.format(", OVERHEATING %d%%", Math.min(99, Math.round(burnTicks / ((testBurnSeconds != null ? testBurnSeconds : Tuning.RECEPTACLE_BURN_SECONDS) * 20) * 100))) : "";
		return Component.literal(type().label() + " plug adapter (" + Math.round(type().volts()) + " V, " + Math.round(type().amps()) + " A): "
				+ String.format("%.0f V, %.1f A, ", volts, amps) + status + warning + "; it has no protection of its own, so the breaker upstream must trip on an overload");
	}
}
