package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The storage compressor (docs/multiblocks/oilfield-follow-ons.md, 23c): an electric-driven two-stage reciprocating compressor, 5 Sm3/s,
 * 1.0 MW at the industrial medium voltage. Pipeline gas in off steel pipe at the north face of its front bottom cells; out of the south
 * face of its back bottom cells by pipe to a gas storage wellhead, which it finds by that pipe and runs only while the wellhead is on
 * INJECT and takes the gas.
 */
public class StorageCompressorBlockEntity extends UnitMotorBlockEntity {
	private double made;
	private double rate;
	private String unit = "starting";

	public StorageCompressorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.STORAGE_COMPRESSOR_ENTITY, pos, state);
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	private List<Lines.Face> outlet() {
		return faces(at -> at[2] == 5 && at[1] == 0, Direction.SOUTH);
	}

	static @Nullable StorageWellheadBlockEntity wellheadOn(Level level, List<Lines.Face> faces) {
		for (Lines.Line line : Lines.lines(level, faces)) {
			for (BlockPos pipe : line.pipes()) {
				for (Direction side : Direction.values()) {
					if (level.getBlockEntity(pipe.relative(side)) instanceof StorageWellheadBlockEntity head) {
						return head;
					}
				}
			}
		}
		return null;
	}

	private boolean testDriven;

	/** Game test hook: run as if its 1 MW motor had its medium-voltage supply (a scene's 500 kW set cannot start it). */
	public void testDriven() {
		testDriven = true;
	}

	public double made() {
		return made;
	}

	@Override
	protected void work(Level level) {
		rate = 0;
		StorageWellheadBlockEntity head = wellheadOn(level, outlet());
		if (head == null) {
			unit = "NO WELLHEAD: pipe the south side of its back to a gas storage wellhead";
			return;
		}
		if (head.mode() != StorageWellheadBlockEntity.Mode.INJECT) {
			unit = "idle: the wellhead is not on INJECT";
			return;
		}
		if (head.highPressure()) {
			unit = "HIGH PRESSURE: the cavern is full; stopped";
			return;
		}
		double gas = Lines.available(level, inlet(), Product.PIPELINE_GAS, Pull.MACHINE, this);
		if (gas < 1e-3) {
			unit = "NO GAS: pipe pipeline gas to the north side of its front";
			return;
		}
		double strength = testDriven ? 1.0 : drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double want = Math.min(gas, Tuning.STORAGE_RATE_SM3S * seconds() * strength);
		double took = head.inject(level, want);
		Lines.pull(level, inlet(), Product.PIPELINE_GAS, took, Pull.MACHINE, this);
		made += took;
		rate = took / seconds();
		unit = took > 0 ? String.format("compressing %.1f Sm3/s into the cavern", rate) : "the wellhead takes nothing: " + head.inspect().getString();
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Storage compressor (two-stage reciprocating, %.0f Sm3/s, %.1f MW): %s; %,.0f Sm3 put in; ", Tuning.STORAGE_RATE_SM3S,
				Tuning.STORAGE_COMPRESSOR_WATTS / 1e6, unit, made) + describe());
	}
}
