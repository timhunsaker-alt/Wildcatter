package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigPartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The inside of an MCC starter section (docs/multiblocks/motor-control-center.md): its share of the horizontal bus and a
 * {@link Starter} in each bucket space that has one. The bus joins its neighbours through the side faces of its bottom cell, but only
 * while the section stands in a lineup to the right of an incoming section; each bucket's motor leads are on the back of its cell.
 */
public class MccSectionBlockEntity extends BlockEntity implements CircuitPart, Inspectable, BigPartEntity {
	public static final int SPACES = 3;
	private static final Conductor[] PHASES = {Conductor.A, Conductor.B, Conductor.C};

	private final Starter[] buckets = new Starter[SPACES];
	private final int[] bus = new int[3];
	private boolean grown;

	public MccSectionBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.MCC_SECTION_ENTITY, pos, state);
	}

	private Direction facing() {
		return getBlockState().getValue(MccSectionBlock.FACING);
	}

	public @Nullable Starter bucket(int space) {
		return space >= 0 && space < SPACES ? buckets[space] : null;
	}

	/** Puts a bucket into an empty space; false if the space is taken. */
	public boolean install(int space) {
		if (space < 0 || space >= SPACES || buckets[space] != null) {
			return false;
		}
		buckets[space] = new Starter();
		show(space);
		return true;
	}

	public void pushButton(int space) {
		if (buckets[space] != null && buckets[space].pushButton()) {
			show(space);
		}
	}

	public void nextOverloadSetting(int space) {
		if (buckets[space] != null) {
			buckets[space].nextOverloadSetting();
			setChanged();
		}
	}

	public void redstone(int space, boolean signal) {
		if (space >= 0 && space < SPACES && buckets[space] != null && buckets[space].redstone(signal)) {
			show(space);
		}
	}

	/** Shows a space's bucket in its cell's state (its door and lights). */
	private void show(int space) {
		setChanged();
		BlockPos cell = worldPosition.above(space);
		if (level != null && level.getBlockState(cell).getBlock() instanceof MccSectionBlock) {
			MccSectionBlock.Bucket want = MccSectionBlock.Bucket.of(buckets[space]);
			if (level.getBlockState(cell).getValue(MccSectionBlock.BUCKET) != want) {
				level.setBlock(cell, level.getBlockState(cell).setValue(MccSectionBlock.BUCKET, want), 3);
			}
		}
	}

	/** On the bus: an incoming section to its left (as you face the front), through starter sections facing the same way. */
	public boolean onBus() {
		if (level == null) {
			return false;
		}
		Direction left = facing().getClockWise();
		BlockPos at = worldPosition;
		for (int step = 1; step <= Tuning.MCC_MAX_BUCKETS / SPACES; step++) {
			at = at.relative(left);
			BlockState state = level.getBlockState(at);
			if (state.getBlock() instanceof MccIncomingBlock && state.getValue(MccIncomingBlock.FACING) == facing() && state.getValue(((MccIncomingBlock) state.getBlock()).piece()) == 0) {
				return true;
			}
			if (!(level.getBlockEntity(at) instanceof MccSectionBlockEntity other && other.facing() == facing())) {
				return false;
			}
		}
		return false;
	}

	/** The buckets in their spaces, bottom to top (null for an empty space). */
	public Starter[] buckets() {
		return buckets;
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return hasPortAt(worldPosition, face, role, voltage);
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		int space = cell.getY() - worldPosition.getY();
		return voltage == Voltage.INDUSTRIAL && face == facing().getOpposite() && cell.getX() == worldPosition.getX() && cell.getZ() == worldPosition.getZ()
				&& bucket(space) != null && role != Conductor.N;
	}

	@Override
	public void build(Circuit circuit) {
		boolean fed = onBus();
		int ground = circuit.node();
		for (int k = 0; k < 3; k++) {
			bus[k] = circuit.node();
		}
		if (fed) {
			Direction left = facing().getClockWise();
			for (Direction side : new Direction[] {left, left.getOpposite()}) {
				for (int k = 0; k < 3; k++) {
					circuit.busPort(worldPosition, side, PHASES[k], bus[k]);
				}
				circuit.busPort(worldPosition, side, Conductor.G, ground);
			}
		}
		for (int space = 0; space < SPACES; space++) {
			if (buckets[space] != null) {
				buckets[space].build(circuit, bus, fed);
				buckets[space].motorPort(circuit, worldPosition.above(space), facing().getOpposite(), ground);
			}
		}
	}

	@Override
	public boolean protect(Circuit circuit, int ticks) {
		boolean tripped = false;
		for (int space = 0; space < SPACES; space++) {
			if (buckets[space] != null && buckets[space].protect(circuit)) {
				tripped(space);
				tripped = true;
			}
		}
		return tripped;
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		for (int space = 0; space < SPACES; space++) {
			if (buckets[space] != null && buckets[space].apply(circuit, ticks)) {
				tripped(space);
			}
		}
	}

	private void tripped(int space) {
		show(space);
		if (level instanceof ServerLevel server) {
			server.playSound(null, worldPosition.above(space), WildcatterSounds.BREAKER_TRIP, SoundSource.BLOCKS, 1.0F, 1.2F);
			CircuitManager.markDirty(server);
		}
	}

	/** One bucket's readout. */
	public Component inspect(int space) {
		Starter bucket = bucket(space);
		String bus = onBus() ? "" : "NOT ON AN MCC BUS: stand the section to the right of an incoming section; ";
		return Component.literal("MCC bucket " + (space + 1) + " (" + Voltage.INDUSTRIAL.label() + " three-phase): " + bus + (bucket == null ? "empty" : bucket.state()));
	}

	@Override
	public Component inspect() {
		StringBuilder text = new StringBuilder("MCC starter section (" + Voltage.INDUSTRIAL.label() + "): " + (onBus() ? "on the bus" : "NOT ON AN MCC BUS"));
		for (int space = 0; space < SPACES; space++) {
			text.append(space == 0 ? "; " : ", ").append(space + 1).append(" ").append(buckets[space] == null ? "empty" : buckets[space].brief());
		}
		return Component.literal(text.toString());
	}

	/** Breaking the section drops the buckets in it. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level != null) {
			for (Starter bucket : buckets) {
				if (bucket != null) {
					Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(WildcatterItems.MCC_BUCKET));
				}
			}
		}
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
		for (int space = 0; space < SPACES; space++) {
			output.putBoolean("has" + space, buckets[space] != null);
			if (buckets[space] != null) {
				buckets[space].save(output, "b" + space + "_");
			}
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		for (int space = 0; space < SPACES; space++) {
			buckets[space] = input.getBooleanOr("has" + space, false) ? new Starter() : null;
			if (buckets[space] != null) {
				buckets[space].load(input, "b" + space + "_");
			}
		}
	}
}
