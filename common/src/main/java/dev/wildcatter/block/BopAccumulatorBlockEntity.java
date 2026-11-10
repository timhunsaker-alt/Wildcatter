package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The BOP's accumulator unit (docs/multiblocks/drilling-mud.md, 15b2): nitrogen-charged bottles, a 15 kW triplex pump at 480 V and the
 * remote panel, within 20 blocks of a land rig. Its bottles hold pressure for three closes of the stack without power; its pump
 * brings back one close each 30 s while it has power. It tells the rig it is there.
 */
public class BopAccumulatorBlockEntity extends UnitMotorBlockEntity {
	private int closes = Tuning.BOP_CLOSES;
	private double charging;   // seconds of pumping toward the next close
	private String unit = "starting";
	private long lookedAt = -1000;
	private @Nullable BlockPos rigAt;

	public BopAccumulatorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.BOP_ACCUMULATOR_ENTITY, pos, state);
	}

	public int closes() {
		return closes;
	}

	/** Uses one close of the stack's preventers; false if its bottles are spent. */
	public boolean takeClose() {
		if (closes <= 0) {
			return false;
		}
		closes--;
		setChanged();
		return true;
	}

	/** Game test hook. */
	public void testCloses(int n) {
		closes = n;
		setChanged();
	}

	@Override
	protected void work(Level level) {
		if (level.getGameTime() - lookedAt >= 20) {
			lookedAt = level.getGameTime();
			rigAt = findRig(level);
		}
		if (rigAt != null && level.getBlockEntity(rigAt) instanceof RigSubstructureBlockEntity rig) {
			rig.accumulatorHere(worldPosition, level.getGameTime());
		}
		if (closes >= Tuning.BOP_CLOSES) {
			charging = 0;
			unit = "charged: " + closes + " closes in its bottles";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = closes + " closes left, pump " + describe();
			return;
		}
		charging += seconds() * strength;
		if (charging >= Tuning.ACCUMULATOR_RECHARGE_S) {
			charging = 0;
			closes++;
		}
		unit = String.format("charging: %d closes, the next in %.0f s", closes, Tuning.ACCUMULATOR_RECHARGE_S - charging);
	}

	/** A land rig's substructure within reach on this level (or one down), or null. */
	private @Nullable BlockPos findRig(Level level) {
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		int r = Tuning.ACCUMULATOR_REACH;
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					at.set(worldPosition.getX() + dx, worldPosition.getY() + dy, worldPosition.getZ() + dz);
					BlockState state = level.getBlockState(at);
					if (state.is(WildcatterBlocks.RIG_SUBSTRUCTURE)) {
						return dev.wildcatter.multiblock.BigParts.main(at, state);
					}
				}
			}
		}
		return null;
	}

	@Override
	public Component inspect() {
		return Component.literal("BOP accumulator (" + Tuning.BOP_CLOSES + " closes, " + Math.round(Tuning.ACCUMULATOR_WATTS / 1000) + " kW pump): " + unit + "; "
				+ (rigAt != null ? "works the rig's BOP" : "NO RIG within " + Tuning.ACCUMULATOR_REACH + " blocks") + "; " + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("closes", closes);
		output.putDouble("charging", charging);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		closes = input.getIntOr("closes", Tuning.BOP_CLOSES);
		charging = input.getDoubleOr("charging", 0);
	}
}
