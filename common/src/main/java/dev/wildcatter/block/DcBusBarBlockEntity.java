package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.safety.Shock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * One metre of DC bus bar (docs/multiblocks/dc-bus.md): a node with half its resistance on each face. Bare: set against bare
 * ground (a ground wire) it is a ground fault there, and at 120 V DC or more to earth it shocks whoever is within reach. It heats
 * with the square of its current over its ampacity, and burns open as a wire does.
 */
public class DcBusBarBlockEntity extends BlockEntity implements CircuitPart, Inspectable {
	private int node;
	private double volts;
	private double amps;
	private double heat;
	private boolean faulted;

	public DcBusBarBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.DC_BUS_ENTITY, pos, state);
	}

	private DcBusBarBlock block() {
		return (DcBusBarBlock) getBlockState().getBlock();
	}

	public double volts() {
		return volts;
	}

	public double amps() {
		return amps;
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	/** Bare ground it touches: a ground wire, or the bare green conductor of a wire. */
	private boolean touchesGround() {
		if (level == null) {
			return false;
		}
		for (Direction side : Direction.values()) {
			BlockPos next = worldPosition.relative(side);
			BlockState there = level.getBlockState(next);
			if (there.getBlock() instanceof GroundWireBlock) {
				return true;
			}
			if (there.getBlock() instanceof CableBlock && CableBlock.conductorOf(there) == Conductor.G
					&& level.getBlockEntity(next) instanceof WireBlockEntity wire && wire.exposed()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void build(Circuit circuit) {
		node = circuit.node();
		for (Direction face : Direction.values()) {
			circuit.dcPort(worldPosition, face, node, block().ohmsPerMeter() / 2.0, true);
		}
		faulted = touchesGround();
		if (faulted) {
			circuit.resistor(node, 0, Tuning.SHORT_CONTACT_OHMS);
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		if (level == null) {
			return;
		}
		volts = circuit.dcVolts(node);
		amps = circuit.through(node);
		heat = dev.wildcatter.energy.WireHeat.step(heat, amps / block().amps(), ticks / 20.0);
		if (dev.wildcatter.energy.WireHeat.burnt(heat) && level instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 10, 0.2, 0.2, 0.2, 0.02);
			server.sendParticles(ParticleTypes.ELECTRIC_SPARK, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.2);
			server.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.6F);
			server.destroyBlock(worldPosition, false);
			CircuitManager.markDirty(server);
			return;
		}
		setChanged();
	}

	/** Runs every tick: a bar at 120 V DC or more to earth shocks whatever is within the 480 V tier's reach. */
	public static void tick(Level level, BlockPos pos, BlockState state, DcBusBarBlockEntity bar) {
		if (!(level instanceof ServerLevel server) || level.getGameTime() % Tuning.SHOCK_INTERVAL != 0 || Math.abs(bar.volts) < Tuning.DC_SHOCK_MIN_VOLTS) {
			return;
		}
		AABB zone = bar.block().getShape(state, level, pos, net.minecraft.world.phys.shapes.CollisionContext.empty()).bounds().move(pos).inflate(Shock.reach(Voltage.INDUSTRIAL));
		for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, zone, e -> e.isAlive() && !e.isSpectator())) {
			Shock.zapDc(server, victim, pos, bar.volts);
		}
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("DC bus bar (%s copper): %+.0f V to ground, %.2f kA of %.0f kA allowed%s%s", block().label(), volts, amps / 1000,
				block().amps() / 1000, dev.wildcatter.energy.WireHeat.readout(heat), faulted ? "; touching GROUND" : ""));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("heat", heat);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		heat = input.getDoubleOr("heat", 0);
	}
}
