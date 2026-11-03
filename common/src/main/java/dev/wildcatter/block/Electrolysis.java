package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.DcCell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The DC side of an electrolysis cell part (docs/multiblocks/dc-bus.md): its + and - terminals on one face, + two cells over -, and its
 * cells between them. Set against a rectifier it joins it directly, as if by short internal bus bars, and the rectifier takes its
 * rated current.
 */
public final class Electrolysis {
	/** A part that holds one: the rectifier touching it reads its rating. */
	public interface Part {
		Electrolysis electrolysis();
	}

	private final DcCell cells;
	private final int x;
	private final int z;
	private final Direction side;
	private int plus;
	private int minus;
	private boolean drawing;

	/** The terminals on {@code side} (the part's own frame) of its cells at {@code x, z}: + at y 2, - at y 0, a cell apart so their bars don't touch. */
	public Electrolysis(DcCell cells, int x, int z, Direction side) {
		this.cells = cells;
		this.x = x;
		this.z = z;
		this.side = side;
	}

	public DcCell cells() {
		return cells;
	}

	/** While it has work: an idle cell opens its circuit. */
	public void setDrawing(Level level, boolean value) {
		if (drawing != value) {
			drawing = value;
			CircuitManager.markDirty(level);
		}
	}

	public void build(Circuit circuit, BlockEntity self) {
		BlockPos main = self.getBlockPos();
		plus = circuit.node();
		minus = circuit.node();
		cells.build(circuit, plus, minus, drawing);
		Direction face = LeasePartBlock.world(self.getBlockState(), side);
		circuit.dcPort(LeasePartBlock.at(main, self.getBlockState(), x, 2, z), face, plus, Tuning.DC_JOINT_OHMS / 2, false);
		circuit.dcPort(LeasePartBlock.at(main, self.getBlockState(), x, 0, z), face, minus, Tuning.DC_JOINT_OHMS / 2, false);
		circuit.publish(main, plus);
		circuit.publish(main.above(), minus);
		RectifierBlockEntity rectifier = Touching.find(self.getLevel(), main, RectifierBlockEntity.class, r -> true);
		if (rectifier != null) {
			circuit.span(rectifier.getBlockPos(), main, Tuning.DC_JOINT_OHMS);
			circuit.span(rectifier.getBlockPos().above(), main.above(), Tuning.DC_JOINT_OHMS);
		}
	}

	public void apply(Circuit circuit) {
		cells.apply(circuit, plus, minus);
	}

	/** The share of its rated current it is getting: what it makes goes with it. */
	public double share() {
		return cells.share();
	}

	public double amps() {
		return cells.amps();
	}

	public double volts() {
		return cells.volts();
	}
}
