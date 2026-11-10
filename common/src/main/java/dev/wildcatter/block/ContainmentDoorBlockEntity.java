package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.multiblock.BigParts;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The containment's passive parts (docs/multiblocks/reactor-extensions.md, 19a): the equipment hatch (a sneak-click opens or shuts
 * it; open, or failed by overpressure, the building is not closed), the personnel airlock (its two doors are interlocked, so the
 * building stays closed: a click takes you through to its other end) and a passive autocatalytic recombiner.
 */
public class ContainmentDoorBlockEntity extends BlockEntity implements BigPartEntity, Inspectable {
	private boolean grown;
	private boolean open;
	private boolean failed;

	public ContainmentDoorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CONTAINMENT_DOOR_ENTITY, pos, state);
	}

	private ContainmentPartBlock.Part part() {
		return getBlockState().getBlock() instanceof ContainmentPartBlock b ? b.part() : ContainmentPartBlock.Part.HYDROGEN_RECOMBINER;
	}

	/** Whether it closes the building: a hatch shut and not failed, or the airlock. */
	public boolean seals() {
		return switch (part()) {
			case EQUIPMENT_HATCH -> !open && !failed;
			case PERSONNEL_AIRLOCK -> true;
			default -> false;
		};
	}

	/** The building's pressure has failed the hatch's seals (2.5 times design): it stays open until it is shut again. */
	public void fail() {
		if (part() == ContainmentPartBlock.Part.EQUIPMENT_HATCH && !failed) {
			failed = true;
			setChanged();
		}
	}

	public String toggle() {
		if (failed) {
			failed = false;
			open = false;
			setChanged();
			return "Equipment hatch: new seals fitted; SHUT";
		}
		open = !open;
		setChanged();
		return "Equipment hatch: " + (open ? "OPEN: the containment is not closed" : "SHUT");
	}

	/** A click on the airlock: through its inner and outer doors in turn, to the cell beyond its other end. */
	public String passThrough(Level level, Player player) {
		List<BlockPos> cells = BigParts.cellsAt(level, worldPosition);
		Direction f = getBlockState().getValue(BigPartBlock.FACING);
		Direction.Axis axis = f.getAxis();
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
		double cx = 0, cz = 0;
		int floor = Integer.MAX_VALUE;
		for (BlockPos c : cells) {
			int v = c.get(axis);
			min = Math.min(min, v);
			max = Math.max(max, v);
			cx += c.getX() + 0.5;
			cz += c.getZ() + 0.5;
			floor = Math.min(floor, c.getY());
		}
		cx /= cells.size();
		cz /= cells.size();
		double mine = axis == Direction.Axis.X ? player.getX() : player.getZ();
		double to = mine < (min + max + 1) / 2.0 ? max + 1.5 : min - 0.5;
		double x = axis == Direction.Axis.X ? to : cx;
		double z = axis == Direction.Axis.Z ? to : cz;
		player.teleportTo(x, floor, z);
		return "Personnel airlock: through the inner and outer doors (interlocked: the building stays closed)";
	}

	@Override
	public Component inspect() {
		return Component.literal(switch (part()) {
			case EQUIPMENT_HATCH -> "Equipment hatch: " + (failed ? "FAILED by overpressure: sneak-click to fit new seals" : open ? "OPEN" : "SHUT") + "; sneak-click to open or shut";
			case PERSONNEL_AIRLOCK -> "Personnel airlock: two interlocked doors; click to pass through";
			default -> String.format("Hydrogen recombiner (passive, catalyst plates): starts itself above %.0f %% hydrogen, up to %.1f kg/h at 4 %%; needs no power",
					Tuning.PAR_START * 100, Tuning.PAR_KG_H);
		});
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

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("grown", grown);
		output.putBoolean("open", open);
		output.putBoolean("failed", failed);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		open = input.getBooleanOr("open", false);
		failed = input.getBooleanOr("failed", false);
	}
}
