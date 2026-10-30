package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.safety.Shock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * One block of wire: a node in the circuit with a little resistance. It remembers how hot it has been and, if it is
 * wrapped in wool, how worn the wool is.
 */
public class WireBlockEntity extends BlockEntity implements CircuitPart, Inspectable {
	private int node;
	private double volts;
	private double amps;
	/** 0 to 1: how far the wool insulation is worn. */
	private double wear;
	/** 0 to 1: how close the copper is to burning out. */
	private double heat;

	public WireBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.WIRE_ENTITY, pos, state);
	}

	private CableBlock block() {
		return (CableBlock) getBlockState().getBlock();
	}

	private double ohms() {
		return switch (block().voltage()) {
			case LOW -> Tuning.WIRE_OHMS_LOW;
			case MEDIUM -> Tuning.WIRE_OHMS_MEDIUM;
			case HIGH -> Tuning.WIRE_OHMS_HIGH;
		};
	}

	private double ampacity() {
		return switch (block().voltage()) {
			case LOW -> Tuning.WIRE_AMPS_LOW;
			case MEDIUM -> Tuning.WIRE_AMPS_MEDIUM;
			case HIGH -> Tuning.WIRE_AMPS_HIGH;
		};
	}

	/** Bare copper, because there is no insulation or because it has worn through. */
	public boolean exposed() {
		return block().plastic() ? false : !block().wool() || wear >= 1.0;
	}

	/** Test hook and repair: sets how worn the insulation is. */
	public void setWear(double value) {
		wear = Math.max(0, Math.min(1, value));
		setChanged();
		syncStage();
	}

	public double wear() {
		return wear;
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return role == getBlockState().getValue(CableBlock.CONDUCTOR) && voltage == block().voltage();
	}

	@Override
	public void build(Circuit circuit) {
		node = circuit.node();
		CableBlock block = block();
		Conductor conductor = getBlockState().getValue(CableBlock.CONDUCTOR);
		if (conductor == Conductor.G) {
			circuit.ground(node);   // the ground conductor is the earth: tied to it at every piece
		}
		for (Direction face : Direction.values()) {
			circuit.port(worldPosition, face, conductor, block.voltage(), node, ohms() / 2.0, exposed());
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		if (level == null) {
			return;
		}
		volts = circuit.voltage(node);
		amps = circuit.through(node);
		double ratio = amps / ampacity();

		// Heat: a wire past its rating warms up, faster the harder it is pushed; under its rating it cools.
		if (ratio > 1.0) {
			heat += (ratio * ratio - 1.0) / 3.0 * ticks / (20.0 * Tuning.WIRE_BURN_SECONDS_AT_2X);
		} else {
			heat = Math.max(0, heat - ticks / (20.0 * Tuning.WIRE_COOL_SECONDS));
		}
		if (heat >= 1.0 && level instanceof ServerLevel server) {
			burnOut(server);
			return;
		}

		// Wool wears out with time, with current, and with weather.
		if (block().wool() && wear < 1.0) {
			boolean wet = level.isRainingAt(worldPosition.above()) || !level.getFluidState(worldPosition).isEmpty();
			wear += ticks / Tuning.WOOL_LIFE_TICKS * (1.0 + Tuning.WOOL_LOAD_WEAR * ratio * ratio) * (wet ? Tuning.WOOL_WET_WEAR : 1.0);
			wear = Math.min(1.0, wear);
			setChanged();
			syncStage();
		}
	}

	private void burnOut(ServerLevel server) {
		Vec3Helper.smoke(server, worldPosition);
		server.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.6F);
		server.destroyBlock(worldPosition, false);
		CircuitManager.markDirty(server);
	}

	/** Keeps the block's look (how frayed the wool is) in step with the wear. */
	private void syncStage() {
		if (level == null || !block().wool()) {
			return;
		}
		int stage = wear >= 1.0 ? Tuning.WEAR_STAGES : (int) (wear * Tuning.WEAR_STAGES);
		BlockState state = getBlockState();
		if (state.getValue(CableBlock.WEAR) != stage) {
			level.setBlock(worldPosition, state.setValue(CableBlock.WEAR, stage), 2);
			CircuitManager.markDirty(level);
		}
	}

	/** Runs every tick: bare wire with voltage on it shocks whatever is near it. */
	public static void tick(Level level, BlockPos pos, BlockState state, WireBlockEntity wire) {
		if (!(level instanceof ServerLevel server) || level.getGameTime() % Tuning.SHOCK_INTERVAL != 0
				|| !wire.exposed() || Math.abs(wire.volts) < Tuning.SHOCK_MIN_VOLTS) {
			return;
		}
		Voltage voltage = wire.block().voltage();
		// Measured from the wire itself (its thin core and arms), not from the whole block.
		double reach = Shock.reach(voltage);
		List<AABB> metal = WireShape.of(state).toAabbs().stream().map(box -> box.move(pos).inflate(reach)).toList();
		AABB search = new AABB(pos).inflate(reach);
		for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, search, e -> e.isAlive() && !e.isSpectator())) {
			if (metal.stream().anyMatch(zone -> zone.intersects(victim.getBoundingBox()))) {
				Shock.zap(server, victim, voltage, pos, wire.volts);
			}
		}
	}

	@Override
	public Component inspect() {
		CableBlock block = block();
		String insulation = block.plastic() ? "plastic insulated, safe to touch"
				: !block.wool() ? "bare copper, live to the touch"
				: wear >= 1.0 ? "wool worn OUT, bare copper" : "wool " + Math.round(wear * 100) + "% worn";
		return Component.literal(getBlockState().getValue(CableBlock.CONDUCTOR).label() + " wire (" + block.voltage().label() + ", " + insulation + "): "
				+ fmt(volts) + " V, " + fmt(amps) + " A of " + fmt(ampacity()) + " A allowed"
				+ (heat > 0.05 ? ", HEATING " + Math.round(heat * 100) + "%" : ""));
	}

	private static String fmt(double value) {
		return Math.abs(value) >= 100 ? String.valueOf(Math.round(value)) : String.format("%.1f", value);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("wear", wear);
		output.putDouble("heat", heat);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		wear = input.getDoubleOr("wear", 0);
		heat = input.getDoubleOr("heat", 0);
	}

	/** Smoke where a wire burned. */
	private static final class Vec3Helper {
		static void smoke(ServerLevel level, BlockPos pos) {
			level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.2, 0.2, 0.2, 0.02);
		}
	}
}
