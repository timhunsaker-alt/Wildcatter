package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.multiblock.BigPartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The block entity of a true-size part that does nothing itself (a tower's fill or drift eliminators): it only remembers that
 * the part has grown into its cells, so a grown part with a piece gone is broken rather than taken for an old lone block.
 */
public class BigPartBlockEntity extends BlockEntity implements BigPartEntity {
	private boolean grown;
	// ---- a wood pole's (block/PoleLoad): its guys' anchors, how long it has been overloaded, whether it may lean ----------------------
	private final java.util.List<BlockPos> guyAnchors = new java.util.ArrayList<>();
	private double overloadSeconds;
	/** Set on a pole placed from 0.7.0 on; a pole from an older save only reads UNGUYED and never leans. */
	private boolean leans = true;
	private PoleLoad.@org.jspecify.annotations.Nullable Reading poleReading;

	public BigPartBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.BIG_PART_ENTITY, pos, state);
	}

	@Override
	public boolean grown() {
		return grown;
	}

	@Override
	public void markGrown() {
		grown = true;
		setChanged();
	}

	public java.util.List<BlockPos> guyAnchors() {
		return guyAnchors;
	}

	public void addGuyAnchor(BlockPos anchor) {
		guyAnchors.add(anchor.immutable());
		setChanged();
	}

	public double overloadSeconds() {
		return overloadSeconds;
	}

	void addOverloadSeconds(double seconds) {
		overloadSeconds += seconds;
		setChanged();
	}

	void clearOverload() {
		if (overloadSeconds > 0) {
			overloadSeconds = 0;
			setChanged();
		}
	}

	public boolean leans() {
		return leans;
	}

	/** Test hook: the pole as loaded from a save made before poles leaned. */
	public void asOlderSaveForTest() {
		leans = false;
	}

	void setPoleReading(PoleLoad.Reading reading) {
		poleReading = reading;
	}

	/** The pole's last check, or null before its first. */
	public PoleLoad.@org.jspecify.annotations.Nullable Reading poleReading() {
		return poleReading;
	}

	private boolean pole() {
		return getBlockState().getBlock() instanceof WoodPoleBlock;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("grown", grown);
		if (pole()) {
			output.putBoolean("leans", leans);
			output.putDouble("overload", overloadSeconds);
			output.putInt("guys", guyAnchors.size());
			for (int i = 0; i < guyAnchors.size(); i++) {
				output.putLong("guy" + i, guyAnchors.get(i).asLong());
			}
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		leans = input.getBooleanOr("leans", false);   // a pole from an older save never leans
		overloadSeconds = input.getDoubleOr("overload", 0);
		guyAnchors.clear();
		for (int i = 0; i < input.getIntOr("guys", 0); i++) {
			guyAnchors.add(BlockPos.of(input.getLongOr("guy" + i, 0)));
		}
	}
}
