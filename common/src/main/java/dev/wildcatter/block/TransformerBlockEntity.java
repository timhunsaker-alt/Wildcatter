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
public class TransformerBlockEntity extends BlockEntity implements CircuitPart, Inspectable, LeadEnd, Bondable, dev.wildcatter.multiblock.BigPartEntity {
	/** A true-size unit (the GSU) has grown into its cells: see BigParts. */
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
	private final dev.wildcatter.circuit.TransformerHeating heating = new dev.wildcatter.circuit.TransformerHeating();
	private boolean failed;
	// ---- 87T, the transformer differential (a transformer of 10 MVA and up that is a true-size part) ----
	private ProtectionZone lowZone;
	private ProtectionZone highZone;
	private boolean lockedOut;
	private String lastTrip = "";
	private double differencePu;
	private final java.util.Set<BlockPos> openedByUs = new java.util.LinkedHashSet<>();
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

	public Voltage bottomVoltage() {
		return block().low();
	}

	/** The terminals (TransformerBlock#terminals): on a one-block unit the front and top are high, the other four faces low. */
	private java.util.List<TransformerBlock.Terminal> terminals() {
		return block().terminals(worldPosition, getBlockState());
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return hasPortAt(worldPosition, face, role, voltage);
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		for (TransformerBlock.Terminal t : terminals()) {
			if (t.cell().equals(cell) && t.face() == face) {
				return voltage == (t.high() ? block().high() : block().low());
			}
		}
		return false;
	}

	/** Pole-mount and split-phase units are single-phase; the rest are three-phase banks, grounded Y to grounded Y. */
	private boolean singlePhase() {
		return split || block().poleMount();
	}

	/** Latched once it has hung on a pole: it then falls when its pole goes (a cutout or transformer set down elsewhere never does). */
	private boolean hung;
	/**
	 * Set once a new pole unit has looked for its pole (protection-gaps 21; Tim, 2026-11-10: "face the pole for new placements only").
	 * A unit from a save without this marker counts as set, so a built unit keeps its facing and its terminals.
	 */
	private boolean faced;

	/**
	 * A pole unit placed by command (the default facing, north) with a pole beside it but not behind it turns its back to that pole, as
	 * a player's click hangs it. A facing named in the command is kept, and a unit already hung right is left alone.
	 */
	private void faceItsPole(net.minecraft.world.level.Level level, BlockPos pos, BlockState state) {
		Direction facing = state.getValue(TransformerBlock.FACING);
		if (!block().poleMount() || facing != Direction.NORTH || WoodPoleBlock.isPole(level.getBlockState(pos.relative(facing.getOpposite())))) {
			return;
		}
		for (Direction side : Direction.Plane.HORIZONTAL) {
			if (WoodPoleBlock.isPole(level.getBlockState(pos.relative(side)))) {
				level.setBlock(pos, state.setValue(TransformerBlock.FACING, side.getOpposite()), 3);
				dev.wildcatter.circuit.CircuitManager.markDirty(level);
				return;
			}
		}
	}

	/** Test hook: the unit as loaded from a save made before units faced their poles. */
	public void asOlderSaveForTest() {
		faced = true;
	}

	/** True once it has looked for its pole (or it came from an older save). */
	public boolean faced() {
		return faced;
	}

	public boolean hung() {
		return hung;
	}

	@Override
	public void build(Circuit circuit) {
		if (!hung && level != null && block().poleMount() && WoodPoleBlock.poleBeside(level, worldPosition)) {
			hung = true;
			setChanged();
		}
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
		findZones();
		if (block.poleMount()) {
			pruneLeads();
			circuit.publish(worldPosition, highPort[0]);   // a lead from a line insulator lands on H1
		}

		Conductor[] names = {Conductor.A, Conductor.B, Conductor.C};
		for (TransformerBlock.Terminal t : terminals()) {
			BlockPos at = t.cell();
			Direction face = t.face();
			if (t.high()) {
				if (phases == 1) {
					// a single-phase unit is fed from whichever phase of the line it is hung on
					circuit.hotPort(at, face, block.high(), highPort[0], Tuning.CONTACT_OHMS / 2, false);
				} else {
					for (int k = 0; k < 3; k++) {
						circuit.port(at, face, names[k], block.high(), highPort[k], Tuning.CONTACT_OHMS / 2, false);
					}
				}
				if (!block.deltaHigh()) {   // a delta has no neutral
					circuit.port(at, face, Conductor.N, block.high(), highNeutral, Tuning.CONTACT_OHMS / 2, false);
				}
			} else {
				boolean side = face != highFace().getOpposite() && face != Direction.DOWN;
				if (phases == 3) {
					for (int k = 0; k < 3; k++) {
						circuit.port(at, face, names[k], block.low(), lowPort[k], Tuning.CONTACT_OHMS / 2, false);
					}
					if (!block.deltaLow()) {   // a delta has no neutral
						circuit.port(at, face, Conductor.N, block.low(), lowNeutral, Tuning.CONTACT_OHMS / 2, false);
					}
				} else if (!side) {
					// back and bottom: leg A and the neutral, as a single-phase low side always had
					circuit.port(at, face, Conductor.A, block.low(), lowPort[0], Tuning.CONTACT_OHMS / 2, false);
					circuit.port(at, face, Conductor.N, block.low(), lowNeutral, Tuning.CONTACT_OHMS / 2, false);
				} else if (split) {
					// the sides: leg B and the neutral, so a machine touching a side runs on leg B and never bridges the two legs
					circuit.port(at, face, Conductor.B, block.low(), lowBPort, Tuning.CONTACT_OHMS / 2, false);
					circuit.port(at, face, Conductor.N, block.low(), lowNeutral, Tuning.CONTACT_OHMS / 2, false);
				}
			}
		}
	}

	/** True if it carries 87T: a true-size, three-phase, rated unit of 10 MVA and up, not a substation yard's (its relay panel guards it). */
	public boolean differential() {
		TransformerBlock block = block();
		return block instanceof dev.wildcatter.multiblock.BigPart && block.threePhase() && block.ratedVa() >= Tuning.XFMR_87_MIN_VA
				&& !(this instanceof PowerTransformerBlockEntity);
	}

	/** The terminals on one side, as faces to walk the zone from. */
	private java.util.List<Lines.Face> sideFaces(boolean high) {
		java.util.List<Lines.Face> out = new java.util.ArrayList<>();
		for (TransformerBlock.Terminal t : terminals()) {
			if (t.high() == high) {
				out.add(new Lines.Face(t.cell(), t.face()));
			}
		}
		return out;
	}

	private void findZones() {
		if (level == null || !differential()) {
			lowZone = highZone = null;
			return;
		}
		lowZone = ProtectionZone.walk(level, sideFaces(false));
		highZone = ProtectionZone.walk(level, sideFaces(true));
	}

	/** The breakers bounding its high side (for the generator bus's 87B to transfer-trip a GSU's line side); empty if none. */
	public java.util.List<BlockPos> highSideBreakers() {
		if (level == null) {
			return java.util.List.of();
		}
		return ProtectionZone.walk(level, sideFaces(true)).breakers;
	}

	/** Rated line current on one side: its VA over three phases at that side's voltage to neutral. */
	private double ratedAmps(boolean high) {
		return block().ratedVa() / (3 * (high ? block().high() : block().low()).nominal());
	}

	private java.util.List<BlockPos> boundary() {
		java.util.List<BlockPos> all = new java.util.ArrayList<>();
		if (lowZone != null) {
			all.addAll(lowZone.breakers);
		}
		if (highZone != null) {
			all.addAll(highZone.breakers);
		}
		return all;
	}

	/** 87T: grounds in the zone draw current that does not come out at the other side's breakers; it opens all of them, and 86T holds them. */
	@Override
	public boolean protect(Circuit circuit, int ticks) {
		if (level == null || lowZone == null || highZone == null) {
			return false;
		}
		if (lockedOut) {
			boolean any = false;
			for (BlockPos at : boundary()) {
				any |= ProtectionZone.open(level, at, "87T transformer differential (86T lockout)");
			}
			return any;
		}
		double through = 0;
		for (int k = 0; k < phases; k++) {
			double winding = Math.abs(circuit.transformerCurrent(highWinding[k]));
			through = Math.max(through, winding * (block().deltaHigh() ? Math.sqrt(3) : 1) / ratedAmps(true));
		}
		differencePu = lowZone.faultAmps(level, circuit) / ratedAmps(false) + highZone.faultAmps(level, circuit) / ratedAmps(true);
		if (!dev.wildcatter.circuit.Differential.transformer(differencePu, through)) {
			return false;
		}
		lockedOut = true;
		lastTrip = String.format("87T TRANSFORMER DIFFERENTIAL TRIP: %.1f per unit in the zone (pickup %.2f)", differencePu,
				dev.wildcatter.circuit.Differential.pickup(through, Tuning.XFMR_87_MIN_PU, Tuning.XFMR_87_SLOPE));
		for (BlockPos at : boundary()) {
			if (ProtectionZone.open(level, at, "87T transformer differential (86T lockout)")) {
				openedByUs.add(at);
			}
		}
		if (level instanceof net.minecraft.server.level.ServerLevel server) {
			server.playSound(null, worldPosition, dev.wildcatter.WildcatterSounds.HV_BREAKER, net.minecraft.sounds.SoundSource.BLOCKS, 1.2F, 0.9F);
		}
		setChanged();
		return true;
	}

	public boolean lockedOut() {
		return lockedOut;
	}

	public String lastTrip() {
		return lastTrip;
	}

	/** 86T reset by hand: the breakers it opened close again (a generator is reset to start and synchronize). */
	public void resetLockout() {
		if (level == null) {
			return;
		}
		lockedOut = false;
		for (BlockPos at : openedByUs) {
			ProtectionZone.close(level, at);
		}
		openedByUs.clear();
		dev.wildcatter.circuit.CircuitManager.markDirty(level);
		setChanged();
	}

	/** "87T ready (0.00 per unit), zone bounded by 2 breakers", or the trip. */
	private String differentialText() {
		if (!differential()) {
			return "";
		}
		if (lockedOut) {
			return lastTrip + ", LOCKED OUT (86T); right-click with an empty hand to reset once the fault is cleared; ";
		}
		if (level != null && !level.isClientSide()) {
			findZones();
		}
		int n = boundary().size();
		String text = String.format("87T ready (%.2f per unit difference), zone bounded by %d breaker%s", differencePu, n, n == 1 ? "" : "s");
		if (!lastTrip.isEmpty()) {
			text += "; last trip: " + lastTrip;
		}
		return text + "; ";
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
		if (rated > 0 && heating.step(perUnit(rated, delta, deltaHigh), ticks / 20.0)) {
			failed = true;   // IEEE C57.109: its windings' (I² - 1)·t has reached its limit
		}
	}

	/** The most loaded low-side winding's current over its rated current: a split-phase leg, a delta winding or a wye phase. */
	private double perUnit(double rated, boolean delta, boolean deltaHigh) {
		double volts = block().low().nominal();
		double windingAmps = split ? rated / (2 * volts) : phases == 1 ? rated / volts
				: delta ? rated / (3 * volts * Math.sqrt(3)) : rated / (3 * volts);
		double most = 0;
		for (int k = 0; k < phases; k++) {
			most = Math.max(most, lowAmps[k]);
		}
		if (split) {
			most = Math.max(most, lowBAmps);
		}
		return most / windingAmps;
	}

	/** A pole transformer that has overheated burns out. */
	public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, TransformerBlockEntity transformer) {
		if (!transformer.faced) {
			transformer.faced = true;
			transformer.setChanged();
			transformer.faceItsPole(level, pos, state);
		}
		if (transformer.failed && level instanceof net.minecraft.server.level.ServerLevel server) {
			server.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.05);
			server.playSound(null, pos, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.3F);
			server.destroyBlock(pos, false);
			dev.wildcatter.circuit.CircuitManager.markDirty(server);
		}
		if (state.getBlock() instanceof dev.wildcatter.multiblock.BigPart && level instanceof net.minecraft.server.level.ServerLevel server
				&& (level.getGameTime() + pos.hashCode()) % 100 == 0) {
			dev.wildcatter.multiblock.BigParts.check(server, pos, state);   // an older world's one-block unit grows to its real size
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

	/** "within its rating", or how hot it is and how long it lasts at this load (IEEE C57.109). */
	private String heatText() {
		double left = heating.secondsToFail();
		if (heating.ratio() > 1.0) {
			String when = left >= 120 ? String.format("%.0f min", left / 60) : String.format("%.0f s", left);
			return String.format("OVERLOADED at %.0f%%, heating %d%%, fails in %s at this load", heating.ratio() * 100, Math.round(heating.level() * 100), when);
		}
		return heating.level() > 0.05 ? String.format("cooling, %d%% heated", Math.round(heating.level() * 100)) : "within its rating";
	}

	@Override
	public Component inspect() {
		String rating = ratedVa() > 0 ? String.format("rated %.0f kVA, %s; ", ratedVa() / 1000.0, heatText()) : "";
		String legB = split ? String.format("leg B %.0f V %.1f A (A to B %.0f V), ", lowBVolts, lowBAmps, legsVolts) : "";
		String bc = phases == 3 ? String.format("B %.0f V %.1f A, C %.0f V %.1f A, ", lowVolts[1], lowAmps[1], lowVolts[2], lowAmps[2]) : "";
		String kind = block().deltaLow() ? String.format("Delta-wye generator step-up (line side %+.0f degrees) ", shiftDegrees)
				: block().deltaHigh() ? String.format("Delta-wye power transformer (high side %+.0f degrees) ", shiftDegrees)
				: phases == 3 ? "Three-phase transformer " : "Transformer ";
		return Component.literal(kind + block().low().label() + (block().deltaLow() ? " (line to line)" : "") + " / " + block().high().label() + ": " + differentialText() + rating + legB + bc
				+ String.format("%s%.0f V %.1f A on the %s side, %.0f V %.2f A on the %s side, losing %.0f W as heat; ",
						phases == 3 ? "A " : "", lowVolts[0], lowAmps[0], block().low().label(), highVolts[0], highAmps[0], block().high().label(), lossWatts)
				+ (level != null && Grounding.isGrounded(level, worldPosition) ? "grounded" : "UNGROUNDED, touching it can shock you"));
	}

	@Override
	protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("hung", hung);
		output.putBoolean("faced", faced);
		saveLeads(output);
		output.putBoolean("grown", grown);
		output.putBoolean("lockedOut", lockedOut);
		output.putString("lastTrip", lastTrip);
		output.putInt("opened", openedByUs.size());
		int i = 0;
		for (BlockPos at : openedByUs) {
			output.putLong("opened" + i++, at.asLong());
		}
	}

	@Override
	protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
		super.loadAdditional(input);
		hung = input.getBooleanOr("hung", false);
		faced = input.getBooleanOr("faced", true);   // an older save: built units keep their terminals
		loadLeads(input);
		grown = input.getBooleanOr("grown", false);
		lockedOut = input.getBooleanOr("lockedOut", false);
		lastTrip = input.getStringOr("lastTrip", "");
		openedByUs.clear();
		for (int i = 0; i < input.getIntOr("opened", 0); i++) {
			openedByUs.add(BlockPos.of(input.getLongOr("opened" + i, 0)));
		}
	}

	// ---- leads from a line insulator (LeadEnd) ----
	private final java.util.Set<BlockPos> leadsIn = new java.util.HashSet<>();

	@Override
	public java.util.Set<BlockPos> leadsIn() {
		return leadsIn;
	}

	@Override
	public java.util.Collection<BlockPos> links() {
		return leadsIn;
	}

	/** Forgets a lead whose insulator is gone or no longer holds it. */
	private void pruneLeads() {
		if (level != null && leadsIn.removeIf(at -> level.isLoaded(at) && !(level.getBlockEntity(at) instanceof InsulatorBlockEntity i && i.hasLeadTo(worldPosition)))) {
			setChanged();
		}
	}

	private void saveLeads(net.minecraft.world.level.storage.ValueOutput output) {
		output.putInt("leadsIn", leadsIn.size());
		int k = 0;
		for (BlockPos at : leadsIn) {
			output.putLong("leadIn" + k++, at.asLong());
		}
	}

	private void loadLeads(net.minecraft.world.level.storage.ValueInput input) {
		leadsIn.clear();
		for (int k = 0; k < input.getIntOr("leadsIn", 0); k++) {
			leadsIn.add(BlockPos.of(input.getLongOr("leadIn" + k, 0)));
		}
	}

	@Override
	public dev.wildcatter.energy.Voltage leadVoltage() {
		return block().poleMount() ? block().high() : null;
	}

	@Override
	public double leadY() {
		return 0.95;
	}
}
