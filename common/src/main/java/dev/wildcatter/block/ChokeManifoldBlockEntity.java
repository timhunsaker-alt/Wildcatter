package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The choke manifold (docs/multiblocks/oilfield-follow-ons.md, 23b): a 5,000 psi manifold of gate valves, a hydraulic adjustable
 * drilling choke worked from the remote panel, a manual adjustable choke, gauges and a buffer chamber, joined to the BOP stack's choke
 * line by steel pipe within 12 blocks of the substructure. The rig finds it by that pipe; the mud-gas separator stands against it.
 */
public class ChokeManifoldBlockEntity extends ProcessPartBlockEntity {
	private @Nullable BlockPos rigAt;
	private long rigSeen = -1000;

	public ChokeManifoldBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CHOKE_MANIFOLD_ENTITY, pos, state);
	}

	void rigHere(BlockPos rig, long now) {
		rigAt = rig;
		rigSeen = now;
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	@Override
	protected void work(Level level) {
		status = rigAt != null && level.getGameTime() - rigSeen < 60 ? "on the rig's choke line" : "NOT ON A RIG: pipe it to a BOP stack's choke line within 12 blocks of the substructure";
	}

	@Override
	public Component inspect() {
		String rig = "";
		if (level != null && rigAt != null && level.getGameTime() - rigSeen < 60 && level.getBlockEntity(rigAt) instanceof RigSubstructureBlockEntity r && r.well() != null) {
			rig = String.format("; choke %.0f %% open, casing %,.0f psi, %.1f m3 of gas through it", r.well().choke() * 100, r.well().casingPsi(), r.gasVented());
		}
		return Component.literal("Choke manifold (5,000 psi, hydraulic and manual adjustable chokes): " + status + rig);
	}
}
