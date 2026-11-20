package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.BessCells;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The utility block's power conversion system (docs/multiblocks/grid-batteries.md, 22b): a 2.5 MW, 2.75 MVA three-level inverter skid on the
 * world's industrial LV tier. One or two battery containers touch it (their DC strings are modelled here, not on the solver), and its MV skid
 * touches it: the PCS's AC side is joined to the skid's low-voltage bushings by the short bus between them, so its terminals are the skid's.
 */
public class BessPcsBlockEntity extends BessInverterBlockEntity {
	public BessPcsBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.BESS_PCS_ENTITY, pos, state);
	}

	@Override
	public double ratedWatts() {
		return Tuning.PCS_WATTS;
	}

	@Override
	protected double ratedVa() {
		return Tuning.PCS_VA;
	}

	@Override
	protected String partName() {
		return "Battery PCS";
	}

	/** The battery containers touching it, at most two. */
	public List<BessContainerBlockEntity> containers(Level level) {
		List<BessContainerBlockEntity> out = new ArrayList<>();
		for (BlockEntity e : Touching.parts(level, worldPosition)) {
			if (e instanceof BessContainerBlockEntity box && out.size() < 2) {
				out.add(box);
			}
		}
		return out;
	}

	/** The MV skid touching it, or null. */
	public TransformerBlockEntity skid(Level level) {
		for (BlockEntity e : Touching.parts(level, worldPosition)) {
			if (e instanceof TransformerBlockEntity t && t.getBlockState().is(WildcatterBlocks.BESS_MV_SKID)) {
				return t;
			}
		}
		return null;
	}

	@Override
	protected List<BessCells> cells(Level level) {
		List<BessCells> out = new ArrayList<>();
		for (BessContainerBlockEntity box : containers(level)) {
			if (!box.burning() && box.modules() > 0) {
				out.add(box.cells());
			}
		}
		return out;
	}

	@Override
	protected void cellsChanged() {
		super.cellsChanged();
		if (level != null) {
			for (BessContainerBlockEntity box : containers(level)) {
				box.setChanged();
			}
		}
	}

	@Override
	protected List<Lines.Face> acPorts(Level level) {
		TransformerBlockEntity skid = skid(level);
		if (skid == null || !(skid.getBlockState().getBlock() instanceof BigTransformerBlock block)) {
			return List.of();
		}
		List<TransformerBlock.Terminal> low = new ArrayList<>();
		for (TransformerBlock.Terminal t : block.terminals(skid.getBlockPos(), skid.getBlockState())) {
			if (!t.high()) {
				low.add(t);
			}
		}
		return outside(low);
	}

	@Override
	public java.util.Collection<BlockPos> links() {
		return level != null && skid(level) != null ? List.of(skid(level).getBlockPos()) : List.of();
	}

	@Override
	protected String unformed(Level level) {
		if (skid(level) == null) {
			return "NO MV SKID: set the MV skid against it";
		}
		if (containers(level).isEmpty()) {
			return "NO CONTAINER: set one or two battery containers against it";
		}
		if (cells(level).isEmpty()) {
			return "NO MODULES: load its containers with LFP modules";
		}
		return null;
	}

	@Override
	protected boolean onFire(Level level) {
		for (BessContainerBlockEntity box : containers(level)) {
			if (box.burning()) {
				return true;
			}
		}
		return false;
	}
}
