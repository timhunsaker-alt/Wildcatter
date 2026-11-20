package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The mud-gas separator (docs/multiblocks/oilfield-follow-ons.md, 23b): a vertical atmospheric separator ("poor boy degasser"), about
 * 0.9 m across and 6 m tall, with a liquid seal, set against the choke manifold. Mud goes back to the tanks; the gas goes up its vent
 * line, steel pipe from the top of it, to a flare stack (which burns it). With no flare on the vent the gas is vented cold at its top.
 */
public class MudGasSeparatorBlockEntity extends ProcessPartBlockEntity {
	private static final double HOLD_M3 = 20.0;
	private double through;   // m3 of gas through it
	private double ventedCold;

	public MudGasSeparatorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.MUD_GAS_SEPARATOR_ENTITY, pos, state);
	}

	/** Gas off the choke: into the vent line for the flare. */
	public void gasThrough(ServerLevel level, double m3) {
		through += m3;
		double fit = Math.min(m3, room(Product.GAS));
		add(Product.GAS, fit);
		ventedCold += m3 - fit;
		if (level.getGameTime() % 5 == 0) {
			BlockPos top = worldPosition.above(5);
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, top.getX() + 0.5, top.getY() + 1.1, top.getZ() + 0.5, 3, 0.1, 0.2, 0.1, 0.01);
		}
		setChanged();
	}

	public double through() {
		return through;
	}

	/** True if its vent line (steel pipe off its top) reaches a flare stack. */
	public boolean ventsToFlare(Level level) {
		for (Lines.Line line : Lines.lines(level, java.util.List.of(new Lines.Face(worldPosition.above(5), Direction.UP)))) {
			for (BlockPos pipe : line.pipes()) {
				for (Direction side : Direction.values()) {
					if (dev.wildcatter.multiblock.BigParts.entity(level, pipe.relative(side)) instanceof FlareStackBlockEntity) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[1] == 5 && side == Direction.UP ? Product.GAS : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.GAS ? HOLD_M3 : 0;
	}

	@Override
	protected void work(Level level) {
		status = held(Product.GAS) > 0.01 ? (ventsToFlare(level) ? "gas up the vent to the flare" : "gas venting cold at its top: pipe its vent to a flare stack") : "liquid seal holding";
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Mud-gas separator (vertical, 0.9 m x 6 m, liquid seal): %s; %.1f m3 of gas through it, %.1f m3 vented cold", status, through, ventedCold));
	}
}
