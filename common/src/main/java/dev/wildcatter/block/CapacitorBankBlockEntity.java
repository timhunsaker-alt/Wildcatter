package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.ShuntCapacitor;
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
 * A 3 Mvar shunt capacitor bank at the distribution tier (docs/multiblocks/ferroalloys.md), in its fenced rack with its breaker and its
 * inrush reactors; the terminal box on the west face of its main cell. It is a grid part: on any bus it gives vars back, and the
 * generators and lines carry that much less. On a submerged arc furnace's bus (8 MW at PF 0.80, 6 Mvar) it lifts the feeder's power
 * factor to about 0.94. Sneak and right-click opens or closes its breaker.
 */
public class CapacitorBankBlockEntity extends PlantUnitBlockEntity implements CircuitPart {
	private final ShuntCapacitor capacitor = new ShuntCapacitor(Voltage.DISTRIBUTION, Tuning.CAP_BANK_VAR);
	private boolean closed = true;

	public CapacitorBankBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CAPACITOR_BANK_ENTITY, pos, state);
	}

	public ShuntCapacitor capacitor() {
		return capacitor;
	}

	public boolean closed() {
		return closed;
	}

	public void setClosed(boolean closed) {
		if (this.closed != closed) {
			this.closed = closed;
			setChanged();
			if (level != null) {
				CircuitManager.markDirty(level);
			}
		}
	}

	/** Sneak and right-click: opens or closes its breaker. */
	public String toggle() {
		setClosed(!closed);
		return "Capacitor bank: breaker " + (closed ? "CLOSED" : "OPEN");
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	// ---- PLC points: CLOSE (a held command), CLOSED, Mvar 4-20 mA --------------------------------------------------------------------
	@Override
	public List<Point> points() {
		return List.of(new Point("CLOSE", true), new Point("CLOSED", false), new Point("AUTO", false), new Point("MVAR 4-20 mA", false, Signal.MA));
	}

	@Override
	public boolean readPoint(int point) {
		return point == 1 ? closed : point == 2 && unitRemote.auto();
	}

	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
		if (point == 0) {
			unitRemote.command(0, on, gameTime);
			if (unitRemote.auto()) {
				setClosed(on);
			}
		}
	}

	@Override
	public double readMa(int point) {
		return 4 + 16 * Math.min(1.03, Math.max(0, capacitor.vars() / Tuning.CAP_BANK_VAR));
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
		return cell.equals(worldPosition) && face == terminalFace() && voltage == Voltage.DISTRIBUTION && role != Conductor.N;
	}

	@Override
	public void build(Circuit circuit) {
		capacitor.build(circuit, worldPosition, List.of(terminalFace()), closed);
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		capacitor.apply(circuit);
	}

	@Override
	public Component inspect() {
		String state = !closed ? String.format("breaker OPEN; the bus at %.0f V", capacitor.volts())
				: capacitor.volts() < 1 ? "breaker CLOSED, NO SUPPLY: wire its west face to a " + Voltage.DISTRIBUTION.label() + " bus"
				: String.format("breaker CLOSED: giving %.2f Mvar at %.0f V, %.0f A", capacitor.vars() / 1e6, capacitor.volts(), capacitor.amps());
		return Component.literal(String.format("Capacitor bank (%.0f Mvar, %s): ", Tuning.CAP_BANK_VAR / 1e6, Voltage.DISTRIBUTION.label()) + state
				+ "; sneak and right-click opens or closes it");
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("closed", closed);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		closed = input.getBooleanOr("closed", true);
	}
}
