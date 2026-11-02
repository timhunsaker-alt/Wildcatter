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
import dev.wildcatter.kinetic.Kinetic;
import dev.wildcatter.kinetic.RotorBlock;
import dev.wildcatter.kinetic.Spin;
import dev.wildcatter.safety.Shock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

/**
 * The inside of a disconnect switch: three blades that close the phases straight through, with neutral and ground passing
 * unswitched. Opened while carrying load it draws an arc: a flash and a crack, and anyone close is burned. The blades swing.
 */
public class DisconnectSwitchBlockEntity extends BlockEntity implements CircuitPart, Inspectable, Kinetic, dev.wildcatter.multiblock.BigPartEntity {
	/** A true-size part has grown into its cells (see BigParts). */
	private boolean grown;

	@Override
	public boolean grown() {
		return grown;
	}

	@Override
	public void markGrown() {
		grown = true;
		setChanged();
	}

	private static final Conductor[] PHASES = {Conductor.A, Conductor.B, Conductor.C};
	/** Blades closed lie flat; open, they stand up. */
	private static final float OPEN_DEGREES = -80.0F;

	private final int[] links = {-1, -1, -1};
	private double amps;
	private String lastArc = "";
	private final Spin blade = new Spin();

	public DisconnectSwitchBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.DISCONNECT_ENTITY, pos, state);
	}

	private Voltage voltage() {
		return ((DisconnectSwitchBlock) getBlockState().getBlock()).voltage();
	}

	public boolean open() {
		return getBlockState().getValue(DisconnectSwitchBlock.OPEN);
	}

	/** The way the part faces, for its turning blades: the yard's facing for a true-size disconnect. */
	@Override
	public Direction facing() {
		return getBlockState().getValue(DisconnectSwitchBlock.FACING);
	}

	/** The way the line runs through it: in at the back, out at the front. */
	private Direction lineFacing() {
		return ((DisconnectSwitchBlock) getBlockState().getBlock()).lineFacing(getBlockState());
	}

	/** The cell that carries the terminal on {@code face}: this one, or for a true-size disconnect the end of the yard's centre line. */
	private BlockPos at(Direction face) {
		return getBlockState().getBlock() instanceof dev.wildcatter.multiblock.BigPart part ? part.terminalCell(worldPosition, getBlockState(), face) : worldPosition;
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return hasPort(face, role, voltage) && cell.equals(at(face));
	}

	/** Right-click: swing the blades. Opening under load draws an arc. */
	public void toggle() {
		if (level == null) {
			return;
		}
		boolean opening = !open();
		if (opening && amps > Tuning.DISCONNECT_ARC_AMPS && level instanceof ServerLevel server) {
			arc(server);
		}
		level.setBlock(worldPosition, getBlockState().setValue(DisconnectSwitchBlock.OPEN, opening), 3);
		blade.setTarget(opening ? OPEN_DEGREES : 0);
		setChanged();
		level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		CircuitManager.markDirty(level);
	}

	/** Test hook: open or close without anyone standing at it. */
	public void setOpen(boolean value) {
		if (open() != value) {
			toggle();
		}
	}

	private void arc(ServerLevel server) {
		lastArc = String.format("opened under load at %.0f A: an arc", amps);
		server.playSound(null, worldPosition, WildcatterSounds.DISCONNECT_ARC, SoundSource.BLOCKS, 2.0F, 1.0F);
		server.sendParticles(ParticleTypes.ELECTRIC_SPARK, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, 40, 0.4, 0.4, 0.4, 0.3);
		server.sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.02);
		AABB zone = new AABB(worldPosition).inflate(Tuning.ARC_FLASH_REACH);
		for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, zone, e -> e.isAlive() && !e.isSpectator())) {
			Shock.zap(server, victim, voltage(), worldPosition);
		}
	}

	public double amps() {
		return amps;
	}

	public String lastArc() {
		return lastArc;
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == voltage() && (face == lineFacing() || face == lineFacing().getOpposite());
	}

	@Override
	public void build(Circuit circuit) {
		for (int k = 0; k < 3; k++) {
			int back = circuit.node();
			int front = circuit.node();
			links[k] = open() ? -1 : circuit.resistor(back, front, Tuning.BREAKER_CLOSED_OHMS);
			circuit.port(at(lineFacing().getOpposite()), lineFacing().getOpposite(), PHASES[k], voltage(), back, Tuning.CONTACT_OHMS / 2, false);
			circuit.port(at(lineFacing()), lineFacing(), PHASES[k], voltage(), front, Tuning.CONTACT_OHMS / 2, false);
		}
		for (Conductor passing : new Conductor[] {Conductor.N, Conductor.G}) {
			int node = circuit.node();
			circuit.port(at(lineFacing().getOpposite()), lineFacing().getOpposite(), passing, voltage(), node, Tuning.CONTACT_OHMS / 2, false);
			circuit.port(at(lineFacing()), lineFacing(), passing, voltage(), node, Tuning.CONTACT_OHMS / 2, false);
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		double most = 0;
		for (int link : links) {
			if (link >= 0) {
				most = Math.max(most, circuit.current(link));
			}
		}
		amps = most;
	}

	// ---- the blades swing -----------------------------------------------------------------------------------------
	@Override
	public List<Rotor> rotors() {
		// drawn for a switch facing north (the line runs south to north): the blades hinge at the south end about the X axis
		if (getBlockState().getBlock() instanceof BigDisconnectBlock big) {
			return big.rotors();
		}
		return List.of(new Rotor(((RotorBlock) WildcatterBlocks.ROTOR).part(RotorBlock.Part.DISCONNECT_BLADE), new Vector3f(0.5F, 0.8F, 0.85F), Direction.Axis.X, 1.0F));
	}

	@Override
	public Spin spin() {
		blade.setTarget(open() ? OPEN_DEGREES : 0);   // the client sees the block state change and swings toward it
		return blade;
	}

	@Override
	public Component inspect() {
		return Component.literal("Disconnect switch (" + voltage().label() + ", three-pole): " + (open() ? "OPEN: a visible break" : String.format("CLOSED, carrying %.0f A", amps))
				+ (lastArc.isEmpty() ? "" : "; last opened " + lastArc) + "; right-click to swing it. Open the breaker first: it is not built to break load");
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return new CompoundTag();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("grown", grown);
		output.putString("lastArc", lastArc);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		lastArc = input.getStringOr("lastArc", "");
	}
}
