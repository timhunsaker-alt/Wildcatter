package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.safety.Bondable;
import dev.wildcatter.safety.Grounding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Two circuits at different voltages, joined only by magnetism: the power that goes in one side comes out the other,
 * with the volts and amps traded against each other (a 4.16 kV side carries 1/38th of the current of the 110 V side).
 * The windings have a little resistance, which is where the heat and the few percent of loss come from.
 * The neutral on each side is tied to ground, as it is at a real service entrance.
 */
public class TransformerBlockEntity extends BlockEntity implements CircuitPart, Inspectable, Bondable {
	/** Delta-wye only: the phase shift across the bank, as of the last solve. */
	private double shiftDegrees;
	private final int[] lowPort = new int[3];
	private final int[] highPort = new int[3];
	private final int[] highWinding = {-1, -1, -1};
	private final double[] lowVolts = new double[3];
	private final double[] highVolts = new double[3];
	private final double[] lowAmps = new double[3];
	private final double[] highAmps = new double[3];
	private int lowNeutral;
	private int highNeutral;
	private int phases = 1;
	private int lowBPort;
	private int lowBWinding = -1;
	private double lowBVolts;
	private double lowBAmps;
	private double legsVolts;
	private boolean split;
	private double lossWatts;
	/** Both windings' resistance seen from the high side, per phase. */
	private double seriesOhms;
	/** 0 to 1: how close an overloaded pole transformer is to failing. */
	private double heat;
	private boolean failed;
	/** The game test sets this to give a pole transformer a small rating. Null in play. */
	public static volatile Double testRatedVa;

	private double ratedVa() {
		return testRatedVa != null && block().poleMount() ? testRatedVa : block().ratedVa();
	}

	public TransformerBlockEntity(BlockPos pos, BlockState state) {
		this(WildcatterBlocks.TRANSFORMER_ENTITY, pos, state);
	}

	protected TransformerBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	private TransformerBlock block() {
		return (TransformerBlock) getBlockState().getBlock();
	}

	private Direction highFace() {
		return getBlockState().getValue(TransformerBlock.FACING);
	}

	public boolean isLive() {
		return Math.max(Math.max(lowVolts[0], highVolts[0]), Math.max(lowVolts[1], lowVolts[2])) > Tuning.SHOCK_MIN_VOLTS;
	}

	public Voltage topVoltage() {
		return block().high();
	}

	/** The high-voltage terminals are on the front and the top (where real bushings are); the low side is the other four faces, so a split-phase secondary has room for A, B and N. */
	private boolean isHighFace(Direction face) {
		return face == highFace() || face == Direction.UP;
	}

	private boolean isLowFace(Direction face) {
		return !isHighFace(face);
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		if (isHighFace(face)) {
			return voltage == block().high();
		}
		return isLowFace(face) && voltage == block().low();
	}

	/** Pole-mount and split-phase units are single-phase; the rest are three-phase banks, grounded Y to grounded Y. */
	private boolean singlePhase() {
		return split || block().poleMount();
	}

	@Override
	public void build(Circuit circuit) {
		TransformerBlock block = block();
		double ratio = block.high().nominal() / block.low().nominal();
		// North American service: the low side is centre-tapped. Winding two runs the other way round, so leg B is 180 degrees from leg A.
		split = !block.threePhase() && block.low() == Voltage.LOW && dev.wildcatter.energy.ElectricalStandard.active().splitPhase();
		phases = singlePhase() ? 1 : 3;
		lowNeutral = circuit.node();
		highNeutral = circuit.node();
		double lowOhms = Tuning.TRANSFORMER_LOW_WINDING_OHMS;
		// leakage reactance from the nameplate impedance, seen from the high side: %Z x V^2 / VA for each phase
		double rated = block.ratedVa();   // the nameplate, not the game test's lowered rating
		double reactance = rated > 0 ? block.percentZ() / 100.0 * block.high().nominal() * block.high().nominal() / (rated / phases) : 0;   // %Z x V^2 / VA
		// both windings' resistance, seen from the high side, in series with the leakage reactance: a rated unit takes its resistance from the nameplate (X/R of about 5, typical of distribution units); the unrated stand-ins keep the old windings
		double zBase = block.high().nominal() * block.high().nominal() / Math.max(1.0, rated / phases);
		seriesOhms = rated > 0 ? block.percentZ() / 100.0 * zBase / Tuning.TRANSFORMER_X_OVER_R : 2.0 * lowOhms * ratio * ratio;
		for (int k = 0; k < phases; k++) {
			lowPort[k] = circuit.node();
			highPort[k] = circuit.node();
			highWinding[k] = -1;
		}
		for (int k = 0; k < phases; k++) {
			if (block.deltaHigh()) {
				// delta on the high side: each high winding between two lines (A-C, B-A, C-B), so the high side leads by 30 degrees
				highWinding[k] = circuit.transformer(highPort[k], highPort[(k + 2) % 3], lowPort[k], lowNeutral, ratio * Math.sqrt(3), seriesOhms * 3, reactance * 3);
			} else if (block.deltaLow()) {
				// delta: each low winding sits between two lines (A-B, B-C, C-A); its turns ratio is the high side's line-to-neutral
				// over the low side's line-to-line, and the high side comes out 30 degrees ahead
				highWinding[k] = circuit.transformer(highPort[k], highNeutral, lowPort[k], lowPort[(k + 1) % 3], ratio / Math.sqrt(3), seriesOhms, reactance);
			} else {
				highWinding[k] = circuit.transformer(highPort[k], highNeutral, lowPort[k], lowNeutral, ratio, seriesOhms, reactance);
			}
		}
		lowBPort = circuit.node();
		if (split) {
			// the second half of the centre-tapped secondary, wound the other way
			lowBWinding = circuit.transformer(highPort[0], highNeutral, lowNeutral, lowBPort, ratio, seriesOhms, reactance);
		} else {
			lowBWinding = -1;
		}
		circuit.ground(lowNeutral);
		circuit.ground(highNeutral);

		Conductor[] names = {Conductor.A, Conductor.B, Conductor.C};
		for (Direction face : Direction.values()) {
			if (isHighFace(face)) {
				if (phases == 1) {
					// a single-phase unit is fed from whichever phase of the line it is hung on
					circuit.hotPort(worldPosition, face, block.high(), highPort[0], Tuning.CONTACT_OHMS / 2, false);
				} else {
					for (int k = 0; k < 3; k++) {
						circuit.port(worldPosition, face, names[k], block.high(), highPort[k], Tuning.CONTACT_OHMS / 2, false);
					}
				}
				if (!block.deltaHigh()) {   // a delta has no neutral
					circuit.port(worldPosition, face, Conductor.N, block.high(), highNeutral, Tuning.CONTACT_OHMS / 2, false);
				}
			} else if (isLowFace(face)) {
				boolean side = face != highFace().getOpposite() && face != Direction.DOWN;
				if (phases == 3) {
					for (int k = 0; k < 3; k++) {
						circuit.port(worldPosition, face, names[k], block.low(), lowPort[k], Tuning.CONTACT_OHMS / 2, false);
					}
					if (!block.deltaLow()) {   // a delta has no neutral
						circuit.port(worldPosition, face, Conductor.N, block.low(), lowNeutral, Tuning.CONTACT_OHMS / 2, false);
					}
				} else if (!side) {
					// back and bottom: leg A and the neutral, as a single-phase low side always had
					circuit.port(worldPosition, face, Conductor.A, block.low(), lowPort[0], Tuning.CONTACT_OHMS / 2, false);
					circuit.port(worldPosition, face, Conductor.N, block.low(), lowNeutral, Tuning.CONTACT_OHMS / 2, false);
				} else if (split) {
					// the sides: leg B and the neutral, so a machine touching a side runs on leg B and never bridges the two legs
					circuit.port(worldPosition, face, Conductor.B, block.low(), lowBPort, Tuning.CONTACT_OHMS / 2, false);
					circuit.port(worldPosition, face, Conductor.N, block.low(), lowNeutral, Tuning.CONTACT_OHMS / 2, false);
				}
			}
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		double va = 0;
		lossWatts = 0;
		double ratio = block().high().nominal() / block().low().nominal();
		boolean delta = block().deltaLow();
		boolean deltaHigh = block().deltaHigh();
		for (int k = 0; k < phases; k++) {
			lowVolts[k] = delta ? circuit.across(lowPort[k], lowPort[(k + 1) % 3]) : circuit.across(lowPort[k], lowNeutral);
			highVolts[k] = deltaHigh ? circuit.across(highPort[k], highPort[(k + 2) % 3]) : circuit.across(highPort[k], highNeutral);
			highAmps[k] = circuit.transformerCurrent(highWinding[k]);
			lowAmps[k] = highAmps[k] * (delta ? ratio / Math.sqrt(3) : deltaHigh ? ratio * Math.sqrt(3) : ratio);   // in a delta: the winding current
			va += lowVolts[k] * lowAmps[k];
			lossWatts += highAmps[k] * highAmps[k] * seriesOhms;
		}
		if (delta || deltaHigh) {   // how far the high side's phase A leads the low side's (to earth): 30 degrees for a delta-wye either way
			double shift = circuit.voltagePhasor(highPort[0]).degrees() - circuit.voltagePhasor(lowPort[0]).degrees();
			shiftDegrees = ((shift + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
		}
		lowBVolts = split ? circuit.across(lowBPort, lowNeutral) : 0;
		lowBAmps = split ? circuit.transformerCurrent(lowBWinding) * ratio : 0;
		legsVolts = split ? circuit.across(lowPort[0], lowBPort) : 0;
		if (split) {
			va += lowBVolts * lowBAmps;
			highAmps[0] += lowBAmps / ratio;   // both halves of the secondary load the one primary
			lossWatts += lowBAmps / ratio * lowBAmps / ratio * seriesOhms;
		}
		double rated = ratedVa();
		if (rated > 0) {
			double loading = va / rated;
			double dt = ticks / 20.0;
			if (loading > 1.0) {
				heat += (loading - 1.0) * 2.0 * dt / Tuning.POLE_TRANSFORMER_FAIL_SECONDS;   // 150 % for the fail time takes it from cold to failed
			} else {
				heat = Math.max(0, heat - dt / Tuning.POLE_TRANSFORMER_COOL_SECONDS);
			}
			if (heat >= 1.0) {
				failed = true;
			}
		}
	}

	/** A pole transformer that has overheated burns out. */
	public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, TransformerBlockEntity transformer) {
		if (transformer.failed && level instanceof net.minecraft.server.level.ServerLevel server) {
			server.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.05);
			server.playSound(null, pos, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.3F);
			server.destroyBlock(pos, false);
			dev.wildcatter.circuit.CircuitManager.markDirty(server);
		}
	}

	/** The largest high-side winding current, as of the last solve (in a delta, the line current is 1.73 times it). */
	public double highWindingAmps() {
		double most = 0;
		for (int k = 0; k < phases; k++) {
			most = Math.max(most, highAmps[k]);
		}
		return most;
	}

	@Override
	public Component inspect() {
		String rating = ratedVa() > 0 ? String.format("rated %.0f kVA, %s; ", ratedVa() / 1000.0, heat > 0.05 ? "OVERLOADED, heating " + Math.round(heat * 100) + "%" : "within its rating") : "";
		String legB = split ? String.format("leg B %.0f V %.1f A (A to B %.0f V), ", lowBVolts, lowBAmps, legsVolts) : "";
		String bc = phases == 3 ? String.format("B %.0f V %.1f A, C %.0f V %.1f A, ", lowVolts[1], lowAmps[1], lowVolts[2], lowAmps[2]) : "";
		String kind = block().deltaLow() ? String.format("Delta-wye generator step-up (line side %+.0f degrees) ", shiftDegrees)
				: block().deltaHigh() ? String.format("Delta-wye power transformer (high side %+.0f degrees) ", shiftDegrees)
				: phases == 3 ? "Three-phase transformer " : "Transformer ";
		return Component.literal(kind + block().low().label() + (block().deltaLow() ? " (line to line)" : "") + " / " + block().high().label() + ": " + rating + legB + bc
				+ String.format("%s%.0f V %.1f A on the %s side, %.0f V %.2f A on the %s side, losing %.0f W as heat; ",
						phases == 3 ? "A " : "", lowVolts[0], lowAmps[0], block().low().label(), highVolts[0], highAmps[0], block().high().label(), lossWatts)
				+ (level != null && Grounding.isGrounded(level, worldPosition) ? "grounded" : "UNGROUNDED, touching it can shock you"));
	}
}
