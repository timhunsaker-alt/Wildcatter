package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterComponents;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.ConductorSize;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * One block of three-phase cable: five conductors (A, B, C, N, G), each a node with the tier's wire resistance. The ground
 * conductor is tied to earth at every piece, as a wire's G is. Each conductor heats on its own current; the hottest one decides
 * when the cable burns.
 */
public class MultiCableBlockEntity extends BlockEntity implements CircuitPart, Inspectable {
	private static final Conductor[] CONDUCTORS = Conductor.values();
	private final int[] node = new int[CONDUCTORS.length];
	private final double[] volts = new double[CONDUCTORS.length];
	private final double[] amps = new double[CONDUCTORS.length];
	/** 0 to 1: how close the hottest conductor is to burning out. */
	private double heat;
	private double lineVolts;
	private double hertz = dev.wildcatter.energy.ElectricalStandard.active().hertz();
	/** Null for a cable placed before sizes: it takes its tier's standard size. */
	private ConductorSize size;

	public MultiCableBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.MULTI_CABLE_ENTITY, pos, state);
	}

	private MultiCableBlock block() {
		return (MultiCableBlock) getBlockState().getBlock();
	}

	private Voltage voltage() {
		return block().voltage();
	}

	private ConductorSize.Kind kind() {
		return block().busway() ? ConductorSize.Kind.BUSWAY : ConductorSize.Kind.CABLE;
	}

	public ConductorSize size() {
		return ConductorSize.orStandard(size, voltage(), kind());
	}

	/** Test hook: sets the conductor size. */
	public void setSize(ConductorSize value) {
		size = value;
		setChanged();
		CircuitManager.markDirty(level);
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == voltage();
	}

	@Override
	public void build(Circuit circuit) {
		double half = size().ohmsPerMeter() / 2.0;   // one block is a metre
		for (Conductor conductor : CONDUCTORS) {
			int k = conductor.ordinal();
			node[k] = circuit.node();
			if (conductor == Conductor.G) {
				circuit.ground(node[k]);
			}
			for (Direction face : Direction.values()) {
				circuit.port(worldPosition, face, conductor, voltage(), node[k], half, false);
			}
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		hertz = circuit.hertz();
		double worst = 0;
		for (int k = 0; k < CONDUCTORS.length; k++) {
			volts[k] = circuit.voltage(node[k]);
			amps[k] = circuit.through(node[k]);
			worst = Math.max(worst, amps[k]);
		}
		lineVolts = circuit.across(node[Conductor.A.ordinal()], node[Conductor.B.ordinal()]);
		double ratio = worst / size().amps();
		heat = dev.wildcatter.energy.WireHeat.step(heat, ratio, ticks / 20.0);
		if (dev.wildcatter.energy.WireHeat.burnt(heat) && level instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 10, 0.2, 0.2, 0.2, 0.02);
			server.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.6F);
			server.destroyBlock(worldPosition, false);
			CircuitManager.markDirty(server);
		}
	}

	/** Current on one conductor, for the game test and the meter. */
	public double amps(Conductor conductor) {
		return amps[conductor.ordinal()];
	}

	@Override
	public Component inspect() {
		StringBuilder text = new StringBuilder((block().busway() ? "Busway (" : "Three-phase cable (") + voltage().label() + ", " + size().label() + ", insulated): ");
		for (Conductor conductor : new Conductor[] {Conductor.A, Conductor.B, Conductor.C}) {
			int k = conductor.ordinal();
			text.append(String.format("%s %.0f V %.1f A, ", conductor.name(), volts[k], amps[k]));
		}
		text.append(String.format("N %.1f A, A to B %.0f V; %.0f A per conductor allowed%s, %.1f Hz", amps[Conductor.N.ordinal()],
				lineVolts, size().amps(), dev.wildcatter.energy.WireHeat.readout(heat), hertz));
		return Component.literal(text.toString());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("heat", heat);
		if (size != null) {
			output.putString("size", size.getSerializedName());
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		heat = input.getDoubleOr("heat", 0);
		size = ConductorSize.byId(input.getStringOr("size", ""));
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		ConductorSize carried = components.get(WildcatterComponents.CONDUCTOR_SIZE);
		if (carried != null) {
			size = carried;
		}
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		components.set(WildcatterComponents.CONDUCTOR_SIZE, size());
	}
}
