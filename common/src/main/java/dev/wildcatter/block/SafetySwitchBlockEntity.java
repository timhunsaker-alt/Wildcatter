package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.item.CartridgeFuseItem;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.multiblock.BigParts;
import java.util.List;
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
 * The inside of a 480 V fused safety switch (docs/multiblocks/safety-switch.md): the line terminals on the top face of its top cell,
 * the load terminals on the bottom face of its main cell, and between them the handle's blades and a class J fuse in each phase. N and
 * G run through.
 */
public class SafetySwitchBlockEntity extends BlockEntity implements CircuitPart, Inspectable, BigPartEntity {
	private static final Conductor[] PHASES = {Conductor.A, Conductor.B, Conductor.C};

	private boolean grown;
	private final FuseSet fuses = new FuseSet(CartridgeFuseItem.Kind.CLASS_J);

	public SafetySwitchBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.SAFETY_SWITCH_ENTITY, pos, state);
	}

	private int frame() {
		return ((SafetySwitchBlock) getBlockState().getBlock()).frame();
	}

	public boolean on() {
		return getBlockState().getValue(SafetySwitchBlock.ON);
	}

	/** Works the handle. */
	public void setOn(boolean value) {
		if (level != null && on() != value) {
			for (BlockPos cell : BigParts.cells(worldPosition, getBlockState())) {
				BlockState state = level.getBlockState(cell);
				if (state.getBlock() instanceof SafetySwitchBlock) {
					level.setBlock(cell, state.setValue(SafetySwitchBlock.ON, value), 3);
				}
			}
			setChanged();
			CircuitManager.markDirty(level);
		}
	}

	/** Fits a fuse: null if it went in, otherwise why not. */
	public @Nullable String fit(CartridgeFuseItem fuse) {
		if (on()) {
			return "Handle OFF first: the door is interlocked with the handle";
		}
		if (fuse.kind() != CartridgeFuseItem.Kind.CLASS_J) {
			return "That fuse doesn't fit: the switch takes class J fuses";
		}
		if (fuse.amps() > frame()) {
			return "That fuse is too big for a " + frame() + " A switch";
		}
		if (!fuses.fit(fuse.amps())) {
			return "All three fuses are fitted and good";
		}
		setChanged();
		return null;
	}

	/** Pulls the fuses out, handle OFF: the good ones as items. */
	public List<ItemStack> pull() {
		if (on()) {
			return List.of();
		}
		List<ItemStack> out = fuses.pull();
		setChanged();
		CircuitManager.markDirty(level);
		return out;
	}

	public double amps() {
		return fuses.mostAmps();
	}

	/** The phases whose fuses are blown, as "B"; empty if none. */
	public String blownPhases() {
		return fuses.blownPhases();
	}

	private BlockPos topCell() {
		List<BlockPos> cells = BigParts.cells(worldPosition, getBlockState());
		return cells.get(cells.size() - 1);
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return hasPortAt(worldPosition, face, role, voltage);
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.INDUSTRIAL && (cell.equals(topCell()) && face == Direction.UP || cell.equals(worldPosition) && face == Direction.DOWN);
	}

	@Override
	public void build(Circuit circuit) {
		fuses.open();
		for (int k = 0; k < 3; k++) {
			int line = circuit.node();
			int load = circuit.node();
			circuit.port(topCell(), Direction.UP, PHASES[k], Voltage.INDUSTRIAL, line, Tuning.CONTACT_OHMS / 2, false);
			circuit.port(worldPosition, Direction.DOWN, PHASES[k], Voltage.INDUSTRIAL, load, Tuning.CONTACT_OHMS / 2, false);
			if (on()) {
				fuses.build(circuit, k, line, load);
			}
		}
		for (Conductor role : new Conductor[] {Conductor.N, Conductor.G}) {
			int node = circuit.node();
			circuit.port(topCell(), Direction.UP, role, Voltage.INDUSTRIAL, node, Tuning.CONTACT_OHMS / 2, false);
			circuit.port(worldPosition, Direction.DOWN, role, Voltage.INDUSTRIAL, node, Tuning.CONTACT_OHMS / 2, false);
		}
	}

	@Override
	public boolean protect(Circuit circuit, int ticks) {
		if (fuses.protect(circuit)) {
			blew();
			return true;
		}
		return false;
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		if (fuses.apply(circuit, ticks)) {
			blew();
		}
	}

	private void blew() {
		setChanged();
		if (level instanceof ServerLevel server) {
			server.playSound(null, worldPosition, WildcatterSounds.FUSE_POP, SoundSource.BLOCKS, 1.0F, 0.9F);
			CircuitManager.markDirty(server);
		}
	}

	@Override
	public Component inspect() {
		String state = on() ? String.format("ON, %,.0f A", fuses.mostAmps()) : "OFF";
		String blown = fuses.blownPhases();
		return Component.literal(String.format("Safety switch %s, %d A frame: %s; %s", Voltage.INDUSTRIAL.label(), frame(), state, fuses.describe())
				+ (blown.isEmpty() ? "" : "; phase " + blown + " open: fix the fault, then handle OFF and fit a new fuse")
				+ (fuses.empty() ? "; handle OFF, then right-click with class J fuses to fit them" : ""));
	}

	/** Breaking it drops its good fuses. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level != null) {
			for (ItemStack out : fuses.pull()) {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), out);
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
		fuses.save(output);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		fuses.load(input);
	}
}
