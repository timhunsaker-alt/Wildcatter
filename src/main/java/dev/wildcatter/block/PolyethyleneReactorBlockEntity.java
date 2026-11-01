package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A small gas-phase fluidized-bed polyethylene reactor and its cycle gas compressor (75 kW, 480 V) (docs/multiblocks/cracker.md).
 * Ethylene in on the side faces of its foot; 98 % of it becomes polyethylene, which the pelletizer touching it makes into plastic
 * sheets. It runs while ethylene waits and the pelletizer has room.
 */
public class PolyethyleneReactorBlockEntity extends UnitMotorBlockEntity {
	private String unit = "starting";

	public PolyethyleneReactorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.PE_REACTOR_ENTITY, pos, state);
	}

	private List<Lines.Face> inlet() {
		List<Lines.Face> out = new ArrayList<>();
		for (Direction side : Direction.Plane.HORIZONTAL) {
			out.addAll(faces(at -> at[1] == 0, side));
		}
		return out;
	}

	@Override
	protected void work(Level level) {
		PelletizerBlockEntity pelletizer = Touching.find(level, worldPosition, PelletizerBlockEntity.class, p -> true);
		double waiting = Lines.available(level, inlet(), Product.ETHYLENE, Pull.MACHINE, this);
		if (pelletizer == null) {
			unit = "NOT BUILT: set a PELLETIZER against the reactor";
		} else if (waiting < 0.001) {
			unit = "idle: no ethylene at its foot";
		} else if (pelletizer.full()) {
			unit = "STOPPED: the pelletizer's bin is full (take the plastic sheets, or put a hopper under its main cell)";
		} else {
			double strength = Math.min(drive(level), pelletizer.drive(level));
			if (strength <= 0) {
				unit = "starting: " + describe() + "; " + pelletizer.describe();
				return;
			}
			double m3 = Math.min(waiting, Tuning.PE_REACTOR_KGS / Tuning.ETHYLENE_KG_PER_M3 * seconds() * strength);
			double got = Lines.pull(level, inlet(), Product.ETHYLENE, m3, Pull.MACHINE, this);
			double polymer = got * Tuning.ETHYLENE_KG_PER_M3 * Tuning.PE_CONVERSION;
			pelletizer.addPolymer(polymer);
			unit = String.format("polymerizing %.3f m3/s of ethylene: %.2f kg/s of polyethylene", got / seconds(), polymer / seconds());
		}
	}

	@Override
	public Component inspect() {
		return Component.literal("Polyethylene reactor (gas phase): " + unit + "; " + describe());
	}
}
