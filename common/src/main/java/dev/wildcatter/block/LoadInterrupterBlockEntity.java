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
import dev.wildcatter.safety.Shock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The inside of a 5 kV load-interrupter switch (docs/multiblocks/load-interrupter.md): the line terminal on the top face of its top
 * back cell, the load terminal on the back face of its bottom back cell, the switch's three blades between them and, on the fused
 * one, an E-rated fuse in each phase. N and G run through. It opens load but not a fault: opened over its load-break rating, it is
 * wrecked.
 */
public class LoadInterrupterBlockEntity extends BlockEntity implements CircuitPart, Inspectable, BigPartEntity {
	private static final Conductor[] PHASES = {Conductor.A, Conductor.B, Conductor.C};

	/** The game test lowers the load-break rating to this so a small load can wreck it. Null in play. */
	public static volatile Double testLoadBreakAmps;

	private boolean grown;
	private final FuseSet fuses = new FuseSet(CartridgeFuseItem.Kind.E_RATED);
	private int[] links = new int[0];
	private double amps;
	private String wreck = "";

	public LoadInterrupterBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.LOAD_INTERRUPTER_ENTITY, pos, state);
	}

	private boolean fused() {
		return ((LoadInterrupterBlock) getBlockState().getBlock()).fused();
	}

	private Direction facing() {
		return getBlockState().getValue(LoadInterrupterBlock.FACING);
	}

	public LoadInterrupterBlock.State state() {
		return getBlockState().getValue(LoadInterrupterBlock.STATE);
	}

	public boolean closed() {
		return state() == LoadInterrupterBlock.State.CLOSED;
	}

	public boolean wrecked() {
		return state() == LoadInterrupterBlock.State.WRECKED;
	}

	public double amps() {
		return amps;
	}

	public String blownPhases() {
		return fuses.blownPhases();
	}

	private double loadBreak() {
		return testLoadBreakAmps != null ? testLoadBreakAmps : Tuning.LIS_LOAD_BREAK_AMPS;
	}

	private void setState(LoadInterrupterBlock.State value) {
		if (level != null && state() != value) {
			for (BlockPos cell : BigParts.cells(worldPosition, getBlockState())) {
				BlockState state = level.getBlockState(cell);
				if (state.getBlock() instanceof LoadInterrupterBlock) {
					level.setBlock(cell, state.setValue(LoadInterrupterBlock.STATE, value), 3);
				}
			}
			setChanged();
			CircuitManager.markDirty(level);
		}
	}

	/** Right-click: open or close. Opening over its load-break rating wrecks it. False if it is wrecked. */
	public boolean operate() {
		if (wrecked()) {
			return false;
		}
		if (closed() && amps > loadBreak() && level instanceof ServerLevel server) {
			arc(server);
			return true;
		}
		setState(closed() ? LoadInterrupterBlock.State.OPEN : LoadInterrupterBlock.State.CLOSED);
		return true;
	}

	/** Test hook: open or close without anyone standing at it. */
	public void setClosed(boolean value) {
		if (closed() != value) {
			operate();
		}
	}

	private void arc(ServerLevel server) {
		wreck = String.format("opened under %,.0f A, over its %,.0f A load-break rating", amps, loadBreak());
		server.playSound(null, worldPosition, WildcatterSounds.DISCONNECT_ARC, SoundSource.BLOCKS, 2.0F, 0.8F);
		server.sendParticles(ParticleTypes.ELECTRIC_SPARK, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, 60, 0.4, 0.6, 0.4, 0.3);
		server.sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5, 12, 0.3, 0.4, 0.3, 0.02);
		AABB zone = new AABB(worldPosition).inflate(Tuning.ARC_FLASH_REACH);
		for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, zone, e -> e.isAlive() && !e.isSpectator())) {
			Shock.zap(server, victim, Voltage.MEDIUM, worldPosition);
		}
		amps = 0;
		setState(LoadInterrupterBlock.State.WRECKED);
	}

	/** Fits an E fuse, the switch open: null if it went in, otherwise why not. */
	public @Nullable String fit(CartridgeFuseItem fuse) {
		if (!fused()) {
			return "This load-interrupter has no fuses";
		}
		if (closed()) {
			return "Open the switch first: the fuse door is interlocked with it";
		}
		if (fuse.kind() != CartridgeFuseItem.Kind.E_RATED) {
			return "That fuse doesn't fit: the switch takes E-rated power fuses";
		}
		if (!fuses.fit(fuse.amps())) {
			return "All three fuses are fitted and good";
		}
		setChanged();
		return null;
	}

	/** Pulls the fuses out, the switch open: the good ones as items. */
	public List<ItemStack> pull() {
		if (closed() || !fused()) {
			return List.of();
		}
		List<ItemStack> out = fuses.pull();
		setChanged();
		CircuitManager.markDirty(level);
		return out;
	}

	private BlockPos cell(int dy, int dz) {
		return BigParts.cell(worldPosition, getBlockState(), LoadInterrupterBlock.FOOTPRINT.pieceAt(0, dy, dz, 0));
	}

	private BlockPos lineCell() {
		return cell(2, 1);
	}

	private BlockPos loadCell() {
		return cell(0, 1);
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return hasPortAt(worldPosition, face, role, voltage);
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.MEDIUM && (cell.equals(lineCell()) && face == Direction.UP || cell.equals(loadCell()) && face == facing().getOpposite());
	}

	@Override
	public void build(Circuit circuit) {
		fuses.open();
		boolean closed = closed();
		links = closed && !fused() ? new int[3] : new int[0];
		for (int k = 0; k < 3; k++) {
			int line = circuit.node();
			int load = circuit.node();
			circuit.port(lineCell(), Direction.UP, PHASES[k], Voltage.MEDIUM, line, Tuning.CONTACT_OHMS / 2, false);
			circuit.port(loadCell(), facing().getOpposite(), PHASES[k], Voltage.MEDIUM, load, Tuning.CONTACT_OHMS / 2, false);
			if (closed) {
				if (fused()) {
					fuses.build(circuit, k, line, load);
				} else {
					links[k] = circuit.resistor(line, load, Tuning.BREAKER_CLOSED_OHMS);
				}
			}
		}
		for (Conductor role : new Conductor[] {Conductor.N, Conductor.G}) {
			int node = circuit.node();
			circuit.port(lineCell(), Direction.UP, role, Voltage.MEDIUM, node, Tuning.CONTACT_OHMS / 2, false);
			circuit.port(loadCell(), facing().getOpposite(), role, Voltage.MEDIUM, node, Tuning.CONTACT_OHMS / 2, false);
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
		double most = fuses.mostAmps();
		for (int link : links) {
			most = Math.max(most, circuit.current(link));
		}
		amps = most;
	}

	private void blew() {
		setChanged();
		if (level instanceof ServerLevel server) {
			server.playSound(null, worldPosition, WildcatterSounds.FUSE_POP, SoundSource.BLOCKS, 1.5F, 0.6F);
			server.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5, 10, 0.2, 0.3, 0.2, 0.02);
			CircuitManager.markDirty(server);
		}
	}

	@Override
	public Component inspect() {
		String head = String.format("Load-interrupter switch %s (%,.0f A load-break%s): ", Voltage.MEDIUM.label(), loadBreak(),
				fused() ? fuses.largest() > 0 ? ", fused " + CartridgeFuseItem.label(CartridgeFuseItem.Kind.E_RATED, fuses.largest()) : ", fused, NO FUSES" : "");
		if (wrecked()) {
			return Component.literal(head + "WRECKED: " + wreck + "; break it and fit a new one");
		}
		StringBuilder text = new StringBuilder(head).append(closed() ? String.format("CLOSED, %,.0f A", amps) : "OPEN");
		if (closed() && amps > loadBreak()) {
			text.append("; OVER ITS LOAD-BREAK RATING: do not open it, open the breaker upstream");
		}
		if (fused()) {
			text.append("; ").append(fuses.describe());
			String blown = fuses.blownPhases();
			if (!blown.isEmpty()) {
				text.append("; phase ").append(blown).append(" open: fix the fault, then open the switch and fit a new fuse");
			}
		}
		return Component.literal(text.toString());
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
		output.putString("wreck", wreck);
		fuses.save(output);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		wreck = input.getStringOr("wreck", "");
		fuses.load(input);
	}
}
