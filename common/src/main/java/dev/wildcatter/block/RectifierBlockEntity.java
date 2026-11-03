package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.DcSupply;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The cells' rectifier (docs/multiblocks/conversion.md, dc-bus.md): its rectifier transformer and 12-pulse diode bridge on 4.16 kV
 * three-phase (its box on the west face of its main cell), a constant-power AC load at 0.95 power factor. On the DC side it holds
 * its setpoint current through whatever is on its bus bars (+ at y 2 over - at y 0 on its east face, z 2), up to 150 V; set against a cell it
 * joins it directly and takes that cell's rated current. Its ground-fault detector alarms when a pole goes to earth.
 */
public class RectifierBlockEntity extends PlantUnitBlockEntity implements CircuitPart {
	private final DcSupply dc = new DcSupply(Voltage.MEDIUM);
	private int setting = Tuning.RECTIFIER_SETPOINT_DEFAULT;
	/** The rated amps of a cell set against it, or 0: then its dial. */
	private double cellAmps;

	public RectifierBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.RECTIFIER_ENTITY, pos, state);
	}

	public DcSupply dc() {
		return dc;
	}

	/** The current it holds: a touching cell's rating, else its dial. */
	public double setpoint() {
		return cellAmps > 0 ? cellAmps : Tuning.RECTIFIER_SETPOINTS_KA[setting] * 1000;
	}

	/** Sneak and right-click: the next setpoint on its dial. */
	public void nextSetpoint() {
		setting = (setting + 1) % Tuning.RECTIFIER_SETPOINTS_KA.length;
		setChanged();
	}

	/** Right-click: resets a DC overcurrent trip. */
	public void reset() {
		if (!dc.tripped().isEmpty()) {
			dc.reset();
			setChanged();
			CircuitManager.markDirty(level);
		}
	}

	public double dcAmps() {
		return dc.amps();
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	private Direction boxFace() {
		return LeasePartBlock.world(getBlockState(), Direction.WEST);
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return cell.equals(worldPosition) && face == boxFace() && voltage == Voltage.MEDIUM && role != Conductor.N;
	}

	@Override
	public void build(Circuit circuit) {
		Electrolysis.Part cell = level == null ? null : Touching.find(level, worldPosition, Electrolysis.Part.class, c -> true);
		cellAmps = cell == null ? 0 : cell.electrolysis().cells().ratedAmps();
		dc.build(circuit, worldPosition, List.of(boxFace()), true);
		Direction east = LeasePartBlock.world(getBlockState(), Direction.EAST);
		circuit.dcPort(LeasePartBlock.at(worldPosition, getBlockState(), 2, 2, 2), east, dc.plus(), Tuning.DC_JOINT_OHMS / 2, false);
		circuit.dcPort(LeasePartBlock.at(worldPosition, getBlockState(), 2, 0, 2), east, dc.minus(), Tuning.DC_JOINT_OHMS / 2, false);
		circuit.publish(worldPosition, dc.plus());
		circuit.publish(worldPosition.above(), dc.minus());
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		boolean was = dc.tripped().isEmpty();
		dc.apply(circuit, ticks, setpoint(), true);
		if (was && !dc.tripped().isEmpty() && level instanceof net.minecraft.server.level.ServerLevel server) {
			BlockPos at = LeasePartBlock.at(worldPosition, getBlockState(), 2, 1, 2);
			server.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.2);
			server.playSound(null, at, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.8F);
		}
		setChanged();
	}

	@Override
	public Component inspect() {
		String setpoint = String.format("setpoint %s kA%s", fmtKa(setpoint()), cellAmps > 0 ? ", the cell's" : "");
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
			state = String.format("DC %.0f V, %.2f kA (%s), %s", dc.volts(), dc.amps() / 1000, setpoint, ground);
		}
		return Component.literal(String.format("Rectifier (12-pulse, %s in): %s; AC %s; sneak and right-click steps the setpoint", Voltage.MEDIUM.label(), state,
				dc.ac().describe()));
	}

	private static String fmtKa(double amps) {
		double ka = amps / 1000;
		return ka == Math.rint(ka) ? String.valueOf((int) ka) : String.format("%.1f", ka);
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
		setting = Math.max(0, Math.min(Tuning.RECTIFIER_SETPOINTS_KA.length - 1, input.getIntOr("setting", Tuning.RECTIFIER_SETPOINT_DEFAULT)));
		dc.load(input);
	}
}
