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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The element melts: a current over the rating heats it, faster the more it is over, and a short circuit melts it
 * at once. Unlike a breaker it cannot be reset, and it is a 110 V part.
 */
public class FuseBlockEntity extends BlockEntity implements CircuitPart, Protective, Inspectable, LeadEnd {
	private int ratingIndex;
	private double heat;
	private int back;
	private int front;
	private int link = -1;
	private double amps;
	private double volts;

	public FuseBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.FUSE_ENTITY, pos, state);
	}

	/** The game test shrinks every rating by this factor so a small fault can blow a big fuse. Null in play. */
	public static volatile Double testRatingScale;

	private Voltage voltage() {
		return ((FuseBlock) getBlockState().getBlock()).voltage();
	}

	private int[] ratings() {
		return voltage() == Voltage.LOW ? Tuning.BREAKER_AMPS : Tuning.CUTOUT_K_LINKS;
	}

	public int rating() {
		return ratings()[Math.min(ratingIndex, ratings().length - 1)];
	}

	private double effectiveRating() {
		return rating() * (testRatingScale != null ? testRatingScale : 1.0);
	}

	public boolean cutout() {
		return voltage() != Voltage.LOW;
	}

	private boolean blown() {
		return getBlockState().getValue(FuseBlock.BLOWN);
	}

	private Direction facing() {
		return getBlockState().getValue(FuseBlock.FACING);
	}

	public void nextRating() {
		ratingIndex = (ratingIndex + 1) % ratings().length;
		setChanged();
	}

	/** A new element: the fuse works again. */
	public void replaceElement() {
		heat = 0;
		setBlown(false);
		CircuitManager.markDirty(level);
	}

	private void setBlown(boolean blown) {
		if (level != null && blown() != blown) {
			level.setBlock(worldPosition, getBlockState().setValue(FuseBlock.BLOWN, blown), 3);
			setChanged();
		}
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return (role == Conductor.A || role == Conductor.B || role == Conductor.C) && voltage == voltage() && (face == facing() || face == facing().getOpposite());
	}

	/** Latched once it has hung on a pole: it then falls when its pole goes (a cutout or transformer set down elsewhere never does). */
	private boolean hung;

	public boolean hung() {
		return hung;
	}

	@Override
	public void build(Circuit circuit) {
		if (!hung && level != null && cutout() && WoodPoleBlock.poleBeside(level, worldPosition)) {
			hung = true;
			setChanged();
		}
		back = circuit.node();
		front = circuit.node();
		link = blown() ? -1 : circuit.resistor(back, front, Tuning.BREAKER_CLOSED_OHMS);
		circuit.hotPort(worldPosition, facing().getOpposite(), voltage(), back, Tuning.CONTACT_OHMS / 2, false);
		if (cutout()) {
			pruneLeads();
			circuit.publish(worldPosition, back);   // a lead from a line insulator lands on the line (top) contact
		}
		circuit.hotPort(worldPosition, facing(), voltage(), front, Tuning.CONTACT_OHMS / 2, false);
	}

	@Override
	public boolean protect(Circuit circuit, int ticks) {
		if (link < 0) {
			return false;
		}
		double now = Math.abs(circuit.current(link));
		if (now > effectiveRating() * Tuning.FUSE_INSTANT_MULTIPLE) {
			blow();
			return true;
		}
		return false;
	}

	// ---- the protection race and the coordination study (neither a plug fuse nor an expulsion link limits current) ------------------
	@Override
	public double clearingSeconds(Circuit circuit) {
		return link >= 0 && Math.abs(circuit.current(link)) > effectiveRating() * Tuning.FUSE_INSTANT_MULTIPLE
				? Tuning.MAGNETIC_CLEAR_CYCLES / circuit.hertz() : Double.POSITIVE_INFINITY;
	}

	@Override
	public double instantPickupAmps() {
		return effectiveRating() * Tuning.FUSE_INSTANT_MULTIPLE;
	}

	@Override
	public String curveName() {
		return cutout() ? rating() + " A K link" : rating() + " A fuse";
	}

	@Override
	public double clearingSeconds(double amps) {
		return dev.wildcatter.circuit.TimeCurrent.thermalMagnetic(curveName(), effectiveRating(), Tuning.FUSE_SECONDS_AT_2X, Tuning.FUSE_INSTANT_MULTIPLE,
				dev.wildcatter.energy.ElectricalStandard.active().hertz()).clearingSeconds(amps);
	}

	@Override
	public double ratingAmps() {
		return effectiveRating();
	}

	@Override
	public double interruptingAmps() {
		return Tuning.INTERRUPT_FUSE;
	}

	@Override
	public java.util.List<Terminal> lineSide() {
		return java.util.List.of(new Terminal(worldPosition, facing().getOpposite()));
	}

	/** True if either end is live: a blown cutout's top contact is still on the line. */
	public boolean isLive() {
		return volts > Tuning.SHOCK_MIN_VOLTS;
	}

	public double volts() {
		return volts;
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		volts = Math.max(circuit.voltage(back), circuit.voltage(front));
		amps = link >= 0 ? Math.abs(circuit.current(link)) : 0;
		double ratio = amps / effectiveRating();
		if (ratio > 1.0) {
			heat += (ratio * ratio - 1.0) / 3.0 * ticks / (20.0 * Tuning.FUSE_SECONDS_AT_2X);
		} else {
			heat = Math.max(0, heat - ticks / (20.0 * Tuning.BREAKER_COOL_SECONDS));
		}
		if (heat >= 1.0 && !blown()) {
			blow();
		}
	}

	private void blow() {
		heat = 0;
		amps = 0;
		setBlown(true);
		if (level instanceof ServerLevel server) {
			server.playSound(null, worldPosition, WildcatterSounds.FUSE_POP, SoundSource.BLOCKS, 1.0F, 1.0F);
			server.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 0.6,
					worldPosition.getZ() + 0.5, 8, 0.1, 0.1, 0.1, 0.02);
			CircuitManager.markDirty(server);
		}
	}

	@Override
	public Component inspect() {
		String status = blown() ? "BLOWN: fix the fault, then right-click with a copper ingot to fit a new element" : String.format("good, carrying %.1f A", amps)
				+ (heat > 0.05 ? ", getting hot " + Math.round(heat * 100) + "%" : "");
		return Component.literal((cutout() ? "Fused cutout, " + rating() + " A K link (" : "Fuse " + rating() + " A (") + voltage().label() + "): " + status + (cutout() ? "; the link drops out when it blows: fit a new one" + (isLive() ? ". LIVE: its contacts are bare, open the line before you touch it" : "") : "; a fuse can't be reset, a breaker can") + Coordination.readout(level, this));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("hung", hung);
		saveLeads(output);
		output.putInt("rating", ratingIndex);
		output.putDouble("heat", heat);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		hung = input.getBooleanOr("hung", false);
		loadLeads(input);
		ratingIndex = Math.max(0, Math.min(Tuning.CUTOUT_K_LINKS.length - 1, input.getIntOr("rating", 0)));
		heat = input.getDoubleOr("heat", 0);
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

	private void saveLeads(ValueOutput output) {
		output.putInt("leadsIn", leadsIn.size());
		int k = 0;
		for (BlockPos at : leadsIn) {
			output.putLong("leadIn" + k++, at.asLong());
		}
	}

	private void loadLeads(ValueInput input) {
		leadsIn.clear();
		for (int k = 0; k < input.getIntOr("leadsIn", 0); k++) {
			leadsIn.add(BlockPos.of(input.getLongOr("leadIn" + k, 0)));
		}
	}

	@Override
	public dev.wildcatter.energy.Voltage leadVoltage() {
		return cutout() ? voltage() : null;
	}

	@Override
	public double leadY() {
		return 0.85;
	}
}
