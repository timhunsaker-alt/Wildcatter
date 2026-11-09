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

	/** A lead (jumper) from this insulator down to a cutout's line terminal or a pole transformer's H1 bushing. */
	public record Lead(BlockPos to, int tier, boolean covered, int length, ConductorSize size) {
		public Lead {
			size = ConductorSize.orStandard(size, Voltage.values()[Math.max(0, Math.min(Voltage.values().length - 1, tier))], ConductorSize.Kind.WIRE);
		}

		public Voltage voltage() {
			return Voltage.values()[Math.max(0, Math.min(Voltage.values().length - 1, tier))];
		}

		public double ohms() {
			return size.ohmsPerMeter() * length * Tuning.SPAN_METERS_PER_BLOCK;
		}
	}

	/** The game test shrinks every span's ampacity by this factor, so a small set can overload one; null in play. */
	public static Double testAmpacityScale;

	private static double rated(ConductorSize size) {
		return size.amps() * (testAmpacityScale != null ? testAmpacityScale : 1.0);
	}

	private final List<Span> spans = new ArrayList<>();
	private final List<Lead> leads = new ArrayList<>();
	private int node;
	private double volts;
	private double amps;
	private int flashTicks;
	/** The heat of each span this end owns (the lower position owns a span), as a multiple of its rated rise; not saved. */
	private final java.util.Map<BlockPos, Double> spanHeat = new java.util.HashMap<>();
	/** The amps in each span, read after the solve, for the readout. */
	private final java.util.Map<BlockPos, Double> spanAmps = new java.util.HashMap<>();
	/** Spans burnt down in the last solve: dropped on the next tick. */
	private final java.util.Set<BlockPos> burnt = new java.util.HashSet<>();
	/** The largest arc current seen by protect in this pass, and the arc's charge so far (amp-seconds). */
	private double arcAmps;
	private double arcCharge;

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

	/** The tier of the line on this insulator: its spans' (or leads') tier, or its own rating when it has none. */
	public Voltage lineTier() {
		return !spans.isEmpty() ? spans.get(0).voltage() : !leads.isEmpty() ? leads.get(0).voltage() : block().rating();
	}

	/** True if the line is a higher tier than the insulator can hold. */
	public boolean underRated() {
		return (!spans.isEmpty() || !leads.isEmpty()) && lineTier().above(block().rating());
	}

	public List<Lead> leads() {
		return leads;
	}

	public boolean hasLeadTo(BlockPos to) {
		return leads.stream().anyMatch(l -> l.to.equals(to));
	}

	public void addLead(Lead lead) {
		leads.add(lead);
		changed();
	}

	public void removeLeadTo(BlockPos to) {
		if (leads.removeIf(l -> l.to.equals(to))) {
			changed();
		}
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
		List<BlockPos> out = new ArrayList<>(spans.stream().map(Span::other).toList());
		leads.forEach(l -> out.add(l.to));
		return out;
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
		for (Lead lead : leads) {
			circuit.span(worldPosition, 0, lead.to, 0, lead.ohms());
		}
		if (underRated()) {
			circuit.ground(node);   // the arc: the line flashes over the insulator to earth
		}
	}

	/** The arc over an under-rated insulator: it is seen here, on the solve the fault appears in, even if a relay clears it at once. */
	@Override
	public boolean protect(Circuit circuit, int ticks) {
		if (underRated()) {
			arcAmps = Math.max(arcAmps, circuit.through(node));
		}
		return false;
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
		// the power-follow arc's charge: for the whole interval while it still burns, or for a relay's clearing time if one opened it
		if (underRated() && arcAmps > 1.0) {
			arcCharge += amps > 1.0 ? amps * ticks / 20.0 : arcAmps * Tuning.ARC_CLEARING_CYCLES / circuit.hertz();
		}
		arcAmps = 0;
		// each span heats on its conductor's rating; the lower end looks after it
		spanAmps.clear();
		for (Span span : spans) {
			boolean mine = worldPosition.asLong() < span.other.asLong();
			double i = mine ? circuit.spanCurrent(worldPosition, span.other) : circuit.spanCurrent(span.other, worldPosition);
			spanAmps.put(span.other, i);
			if (mine) {
				double rise = dev.wildcatter.energy.WireHeat.step(spanHeat.getOrDefault(span.other, 0.0), i / rated(span.size()), ticks / 20.0, Tuning.SPAN_HEAT_SECONDS);
				spanHeat.put(span.other, rise);
				if (rise >= Tuning.SPAN_BURN_RISE) {
					burnt.add(span.other);
				}
			}
		}
		for (Lead lead : leads) {
			double i = circuit.spanCurrent(worldPosition, 0, lead.to, 0);
			spanAmps.put(lead.to, i);
			double rise = dev.wildcatter.energy.WireHeat.step(spanHeat.getOrDefault(lead.to, 0.0), i / rated(lead.size()), ticks / 20.0, Tuning.SPAN_HEAT_SECONDS);
			spanHeat.put(lead.to, rise);
			if (rise >= Tuning.SPAN_BURN_RISE) {
				burnt.add(lead.to);
			}
		}
		spanHeat.keySet().retainAll(spanAmps.keySet());
	}

	/** The most loaded span's amps over its rating, 0 with none. */
	public double spanLoading() {
		double most = 0;
		for (Span span : spans) {
			most = Math.max(most, spanAmps.getOrDefault(span.other, 0.0) / span.size().amps());
		}
		return most;
	}

	/** The arc's charge so far, amp-seconds (the game test). */
	public double arcCharge() {
		return arcCharge;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, InsulatorBlockEntity insulator) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		long time = level.getGameTime();
		if (time % 20 == 0) {
			insulator.dropDeadSpans(server);
		}
		if (!insulator.burnt.isEmpty()) {
			for (BlockPos other : insulator.burnt) {
				// the span anneals, parts and falls: both ends lose it
				server.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.05);
				server.playSound(null, pos, dev.wildcatter.WildcatterSounds.FLASHOVER, net.minecraft.sounds.SoundSource.BLOCKS, 1.5F, 0.7F);
				if (server.getBlockEntity(other) instanceof InsulatorBlockEntity far) {
					far.removeSpanTo(pos);
				} else if (server.getBlockEntity(other) instanceof LeadEnd end) {
					end.leadsIn().remove(pos);
				}
				insulator.removeSpanTo(other);
				insulator.removeLeadTo(other);
				dev.wildcatter.Wildcatter.LOGGER.info("A span at {} burnt down (overloaded past its conductor's rating)", pos);
			}
			insulator.burnt.clear();
		}
		if (insulator.flashTicks >= Tuning.INSULATOR_FLASHOVER_TICKS || insulator.arcCharge >= Tuning.INSULATOR_SHATTER_AMP_SECONDS) {
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
			Vec3 from = Vec3.atBottomCenterOf(pos).add(0, InsulatorBlock.conductorY(state), 0);
			Vec3 to = Vec3.atBottomCenterOf(span.other).add(0, InsulatorBlock.conductorY(level.getBlockState(span.other)), 0);
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
		dropped |= leads.removeIf(lead -> server.isLoaded(lead.to)
				&& !(server.getBlockEntity(lead.to) instanceof LeadEnd end && end.leadVoltage() != null && end.leadsIn().contains(worldPosition)));
		if (dropped) {
			changed();
		}
	}

	@Override
	public Component inspect() {
		String hold = spans.isEmpty() && !leads.isEmpty() ? leads.size() + (leads.size() == 1 ? " lead" : " leads") + ", " + lineTier().label()
				: spans.isEmpty() ? "no spans" : spans.size() + (spans.size() == 1 ? " span" : " spans") + ", " + (covered() ? "covered" : "BARE") + " " + lineTier().label() + " line of " + spans.get(0).size().label();
		String warning = underRated() ? ", UNDER-RATED for this line: it is flashing over" + (arcCharge > 0 ? String.format(" (arc %.0f A·s)", arcCharge) : "") : "";
		StringBuilder load = new StringBuilder();
		double most = 0;
		Span worst = null;
		for (Span span : spans) {
			double i = spanAmps.getOrDefault(span.other, 0.0);
			if (worst == null || i / span.size().amps() > most) {
				most = i / span.size().amps();
				worst = span;
			}
		}
		if (worst != null) {
			load.append(String.format("; span %.1f A of %.0f A", spanAmps.getOrDefault(worst.other, 0.0), worst.size().amps()));
			double rise = Math.max(spanHeat.getOrDefault(worst.other, 0.0), 0);
			if (rise > 1.02) {
				load.append(String.format(", OVERLOADED, heating %d%%", Math.round(Math.min(1, (rise - 1) / (Tuning.SPAN_BURN_RISE - 1)) * 100)));
			}
		}
		return Component.literal(conductor().label() + " insulator (rated " + block().rating().label() + "): " + hold + warning
				+ String.format(", %.0f V, %.1f A", Math.abs(volts), amps) + load);
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
		ValueOutput.ValueOutputList leadList = output.childrenList("leads");
		for (Lead lead : leads) {
			ValueOutput child = leadList.addChild();
			child.putInt("x", lead.to.getX());
			child.putInt("y", lead.to.getY());
			child.putInt("z", lead.to.getZ());
			child.putInt("tier", lead.tier);
			child.putBoolean("covered", lead.covered);
			child.putInt("length", lead.length);
			child.putString("size", lead.size.getSerializedName());
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
		leads.clear();
		for (ValueInput child : input.childrenListOrEmpty("leads")) {
			leads.add(new Lead(new BlockPos(child.getIntOr("x", 0), child.getIntOr("y", 0), child.getIntOr("z", 0)),
					child.getIntOr("tier", 0), child.getBooleanOr("covered", false), child.getIntOr("length", 1),
					ConductorSize.byId(child.getStringOr("size", ""))));
		}
	}
}
