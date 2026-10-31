package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.ConductorSize;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.safety.Shock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * One end of overhead spans. It is a node of the circuit; each span joins it to another insulator through a resistance
 * proportional to its length. Spans are stored on both ends, drawn by the client, and dropped if the other end disappears.
 */
public class InsulatorBlockEntity extends BlockEntity implements CircuitPart, Inspectable {
	/** A span to another insulator. */
	public record Span(BlockPos other, int tier, boolean covered, int length, ConductorSize size) {
		public Span {
			size = ConductorSize.orStandard(size, Voltage.values()[Math.max(0, Math.min(Voltage.values().length - 1, tier))], ConductorSize.Kind.WIRE);
		}

		public Voltage voltage() {
			return Voltage.values()[Math.max(0, Math.min(Voltage.values().length - 1, tier))];
		}

		/** Resistance of the span: its conductor's ohms per metre over the span's metres. */
		public double ohms() {
			return size.ohmsPerMeter() * length * Tuning.SPAN_METERS_PER_BLOCK;
		}
	}

	private final List<Span> spans = new ArrayList<>();
	private int node;
	private double volts;
	private double amps;
	private int flashTicks;

	public InsulatorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.INSULATOR_ENTITY, pos, state);
	}

	private InsulatorBlock block() {
		return (InsulatorBlock) getBlockState().getBlock();
	}

	public Conductor conductor() {
		return getBlockState().getValue(CableBlock.CONDUCTOR);
	}

	public List<Span> spans() {
		return spans;
	}

	public int spanCount() {
		return spans.size();
	}

	/** The tier of the line on this insulator: its spans' tier, or its own rating when it has none. */
	public Voltage lineTier() {
		return spans.isEmpty() ? block().rating() : spans.get(0).voltage();
	}

	/** True if the line is a higher tier than the insulator can hold. */
	public boolean underRated() {
		return !spans.isEmpty() && lineTier().above(block().rating());
	}

	private boolean covered() {
		return !spans.isEmpty() && spans.get(0).covered();
	}

	/** True for bare conductor that can be touched. */
	public boolean exposed() {
		return !spans.isEmpty() && !covered();
	}

	public boolean hasSpanTo(BlockPos other) {
		return spans.stream().anyMatch(s -> s.other.equals(other));
	}

	public void addSpan(Span span) {
		spans.add(span);
		changed();
	}

	public void removeSpanTo(BlockPos other) {
		if (spans.removeIf(s -> s.other.equals(other))) {
			changed();
		}
	}

	private void changed() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
			CircuitManager.markDirty(level);
		}
	}

	@Override
	public List<BlockPos> links() {
		return spans.stream().map(Span::other).toList();
	}

	@Override
	public boolean hasPort(Direction face, Conductor conductor, Voltage voltage) {
		return conductor == conductor() && voltage == lineTier();
	}

	@Override
	public void build(Circuit circuit) {
		node = circuit.node();
		circuit.publish(worldPosition, node);
		for (Direction face : Direction.values()) {
			circuit.port(worldPosition, face, conductor(), lineTier(), node, Tuning.CONTACT_OHMS / 2, exposed());
		}
		for (Span span : spans) {
			if (worldPosition.asLong() < span.other.asLong()) {
				circuit.span(worldPosition, span.other, span.ohms());
			}
		}
		if (underRated()) {
			circuit.ground(node);   // the arc: the line flashes over the insulator to earth
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		volts = circuit.voltage(node);
		amps = circuit.through(node);
		if (underRated() && amps > 1.0) {
			flashTicks += ticks;
		} else if (flashTicks > 0) {
			flashTicks = Math.max(0, flashTicks - ticks);
		}
	}

	public static void tick(Level level, BlockPos pos, BlockState state, InsulatorBlockEntity insulator) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		long time = level.getGameTime();
		if (time % 20 == 0) {
			insulator.dropDeadSpans(server);
		}
		if (insulator.flashTicks >= Tuning.INSULATOR_FLASHOVER_TICKS) {
			server.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.05);
			server.playSound(null, pos, dev.wildcatter.WildcatterSounds.FLASHOVER, net.minecraft.sounds.SoundSource.BLOCKS, 2.0F, 1.0F);
			server.destroyBlock(pos, false);   // the insulator shatters; its spans fall with it
			return;
		}
		if (time % Tuning.SHOCK_INTERVAL != 0 || !insulator.exposed() || Math.abs(insulator.volts) < Tuning.SHOCK_MIN_VOLTS) {
			return;
		}
		// bare line shocks whatever is near it: the lower end of each span looks after the span
		double reach = Shock.reach(insulator.lineTier());
		for (Span span : insulator.spans) {
			if (pos.asLong() >= span.other.asLong()) {
				continue;
			}
			Vec3 from = Vec3.atCenterOf(pos).add(0, 0.4, 0);
			Vec3 to = Vec3.atCenterOf(span.other).add(0, 0.4, 0);
			int steps = Math.max(2, (int) Math.ceil(from.distanceTo(to)));
			for (int i = 0; i <= steps; i++) {
				Vec3 at = from.lerp(to, i / (double) steps);
				AABB zone = new AABB(at, at).inflate(reach);
				for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, zone, e -> e.isAlive() && !e.isSpectator())) {
					Shock.zap(server, victim, insulator.lineTier(), pos, insulator.volts);
				}
			}
		}
	}

	/** A span whose other end is gone (broken, or never there) is dropped; ends in unloaded chunks are left alone. */
	private void dropDeadSpans(ServerLevel server) {
		boolean dropped = spans.removeIf(span -> server.isLoaded(span.other)
				&& !(server.getBlockEntity(span.other) instanceof InsulatorBlockEntity o && o.hasSpanTo(worldPosition)));
		if (dropped) {
			changed();
		}
	}

	@Override
	public Component inspect() {
		String hold = spans.isEmpty() ? "no spans" : spans.size() + (spans.size() == 1 ? " span" : " spans") + ", " + (covered() ? "covered" : "BARE") + " " + lineTier().label() + " line of " + spans.get(0).size().label();
		String warning = underRated() ? ", UNDER-RATED for this line: it is flashing over" : "";
		return Component.literal(conductor().label() + " insulator (rated " + block().rating().label() + "): " + hold + warning
				+ String.format(", %.0f V, %.1f A", Math.abs(volts), amps));
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ValueOutput.ValueOutputList list = output.childrenList("spans");
		for (Span span : spans) {
			ValueOutput child = list.addChild();
			child.putInt("x", span.other.getX());
			child.putInt("y", span.other.getY());
			child.putInt("z", span.other.getZ());
			child.putInt("tier", span.tier);
			child.putBoolean("covered", span.covered);
			child.putInt("length", span.length);
			child.putString("size", span.size.getSerializedName());
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		spans.clear();
		for (ValueInput child : input.childrenListOrEmpty("spans")) {
			spans.add(new Span(new BlockPos(child.getIntOr("x", 0), child.getIntOr("y", 0), child.getIntOr("z", 0)),
					child.getIntOr("tier", 0), child.getBooleanOr("covered", false), child.getIntOr("length", 1),
					ConductorSize.byId(child.getStringOr("size", ""))));
		}
	}
}
