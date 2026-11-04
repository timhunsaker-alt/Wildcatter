package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.DcSupply;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Potline;
import dev.wildcatter.fluid.Product;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A potline rectiformer (docs/multiblocks/potline.md, dc-bus.md): a regulating transformer with its on-load taps and saturable
 * reactors, and its diode cubicles, 60 kA and up to 220 V, on the 138 kV grid (its terminal on the west face of its main cell). Set
 * side by side, rectiformers form a yard: their DC terminals (+ at y 2 over - at y 0 on the east face, z 2) are joined inside, they
 * hold one line setpoint, and each unit online carries its share of it, at most 60 kA.
 */
public class RectiformerBlockEntity extends PlantUnitBlockEntity implements CircuitPart {
	private final DcSupply dc = new DcSupply(Voltage.HIGH, Tuning.RECTIFORMER_MAX_DC_VOLTS, Tuning.RECTIFORMER_DC_OHMS, Tuning.RECTIFORMER_RAMP_VOLTS_PER_S,
			Tuning.RECTIFORMER_EFFICIENCY);
	private int setting = Tuning.POTLINE_SETPOINT_DEFAULT;
	private int online = 1;
	private int units = 1;

	public RectiformerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.RECTIFORMER_ENTITY, pos, state);
	}

	public DcSupply dc() {
		return dc;
	}

	/** The yard's line setpoint, amps. */
	public double lineSetpoint() {
		return Tuning.POTLINE_SETPOINTS_KA[setting] * 1000;
	}

	/** This unit's share of it. */
	public double share() {
		return Potline.unitShare(lineSetpoint(), online);
	}

	public boolean online() {
		return dc.tripped().isEmpty() && !dc.ac().undervoltage();
	}

	/** The rectiformers of its yard, joined side by side, with this one. */
	public Set<RectiformerBlockEntity> yard() {
		Set<RectiformerBlockEntity> seen = new LinkedHashSet<>();
		if (level == null) {
			seen.add(this);
			return seen;
		}
		ArrayDeque<RectiformerBlockEntity> todo = new ArrayDeque<>(List.of(this));
		while (!todo.isEmpty() && seen.size() < 16) {
			RectiformerBlockEntity unit = todo.poll();
			if (seen.add(unit)) {
				for (BlockEntity next : Touching.parts(level, unit.getBlockPos())) {
					if (next instanceof RectiformerBlockEntity other && !seen.contains(other)) {
						todo.add(other);
					}
				}
			}
		}
		return seen;
	}

	/** Sneak and right-click: the next line setpoint, for the whole yard. */
	public void nextSetpoint() {
		int next = (setting + 1) % Tuning.POTLINE_SETPOINTS_KA.length;
		for (RectiformerBlockEntity unit : yard()) {
			unit.setting = next;
			unit.setChanged();
		}
	}

	/** Right-click: resets a DC overcurrent trip. */
	public void reset() {
		if (!dc.tripped().isEmpty()) {
			dc.reset();
			setChanged();
			CircuitManager.markDirty(level);
		}
	}

	/** For the game test and the operator: opens its breaker. */
	public void trip() {
		dc.trip("opened by hand");
		setChanged();
		CircuitManager.markDirty(level);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	private Direction terminalFace() {
		return LeasePartBlock.world(getBlockState(), Direction.WEST);
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return cell.equals(worldPosition) && face == terminalFace() && voltage == Voltage.HIGH && role != Conductor.N;
	}

	@Override
	public void build(Circuit circuit) {
		Set<RectiformerBlockEntity> yard = yard();
		units = yard.size();
		online = (int) yard.stream().filter(RectiformerBlockEntity::online).count();
		dc.build(circuit, worldPosition, List.of(terminalFace()), true);
		Direction east = LeasePartBlock.world(getBlockState(), Direction.EAST);
		circuit.dcPort(LeasePartBlock.at(worldPosition, getBlockState(), 7, 2, 2), east, dc.plus(), Tuning.POTLINE_JOINT_OHMS / 2, false);
		circuit.dcPort(LeasePartBlock.at(worldPosition, getBlockState(), 7, 0, 2), east, dc.minus(), Tuning.POTLINE_JOINT_OHMS / 2, false);
		circuit.publish(worldPosition, dc.plus());
		circuit.publish(worldPosition.above(), dc.minus());
		if (level != null) {
			for (BlockEntity next : Touching.parts(level, worldPosition)) {
				if (next instanceof RectiformerBlockEntity other && worldPosition.compareTo(other.getBlockPos()) < 0) {
					circuit.span(worldPosition, other.getBlockPos(), Tuning.POTLINE_JOINT_OHMS);   // the yard's DC header
					circuit.span(worldPosition.above(), other.getBlockPos().above(), Tuning.POTLINE_JOINT_OHMS);
				}
			}
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		boolean was = dc.tripped().isEmpty();
		dc.apply(circuit, ticks, share(), true);
		if (was && !dc.tripped().isEmpty() && level instanceof net.minecraft.server.level.ServerLevel server) {
			BlockPos at = LeasePartBlock.at(worldPosition, getBlockState(), 7, 1, 2);
			server.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.2);
			server.playSound(null, at, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.8F);
			CircuitManager.markDirty(level);   // the others take up its share
		}
		setChanged();
	}

	@Override
	public Component inspect() {
		String state;
		if (!dc.tripped().isEmpty()) {
			state = "TRIPPED (" + dc.tripped() + "): right-click to reset";
		} else if (dc.ac().undervoltage()) {
			state = "NO AC: " + dc.ac().describe();
		} else {
			String fault = dc.groundFault();
			String ground = fault.isEmpty() ? "ground OK"
					: String.format("GROUND FAULT on %s: %s at %.0f V to ground", fault, fault.equals("+") ? "-" : "+",
							Math.abs(fault.equals("+") ? dc.minusToEarth() : dc.plusToEarth()));
			state = String.format("DC %.0f V, %.1f kA of the line's %.0f kA (%d of %d online), %s", dc.volts(), dc.amps() / 1000, Math.min(lineSetpoint(),
					online * Tuning.RECTIFORMER_AMPS) / 1000, online, units, ground);
		}
		return Component.literal(String.format("Rectiformer (60 kA, %s in): %s; AC %s; sneak and right-click steps the line setpoint", Voltage.HIGH.label(), state,
				dc.ac().describe()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("setting", setting);
		dc.save(output);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		setting = Math.max(0, Math.min(Tuning.POTLINE_SETPOINTS_KA.length - 1, input.getIntOr("setting", Tuning.POTLINE_SETPOINT_DEFAULT)));
		dc.load(input);
	}
}
