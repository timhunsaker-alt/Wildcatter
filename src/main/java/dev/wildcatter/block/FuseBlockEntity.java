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
public class FuseBlockEntity extends BlockEntity implements CircuitPart, Inspectable {
	private int ratingIndex;
	private double heat;
	private int back;
	private int front;
	private int link = -1;
	private double amps;

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

	private boolean cutout() {
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
		return (role == Conductor.A || role == Conductor.B) && voltage == voltage() && (face == facing() || face == facing().getOpposite());
	}

	@Override
	public void build(Circuit circuit) {
		back = circuit.node();
		front = circuit.node();
		link = blown() ? -1 : circuit.resistor(back, front, Tuning.BREAKER_CLOSED_OHMS);
		circuit.port(worldPosition, facing().getOpposite(), Conductor.A, voltage(), back, Tuning.CONTACT_OHMS / 2, false);
		circuit.port(worldPosition, facing().getOpposite(), Conductor.B, voltage(), back, Tuning.CONTACT_OHMS / 2, false);
		circuit.port(worldPosition, facing(), Conductor.A, voltage(), front, Tuning.CONTACT_OHMS / 2, false);
		circuit.port(worldPosition, facing(), Conductor.B, voltage(), front, Tuning.CONTACT_OHMS / 2, false);
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

	@Override
	public void apply(Circuit circuit, int ticks) {
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
			server.playSound(null, worldPosition, WildcatterSounds.BREAKER_TRIP, SoundSource.BLOCKS, 1.0F, 1.6F);
			server.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 0.6,
					worldPosition.getZ() + 0.5, 8, 0.1, 0.1, 0.1, 0.02);
			CircuitManager.markDirty(server);
		}
	}

	@Override
	public Component inspect() {
		String status = blown() ? "BLOWN: fix the fault, then right-click with a copper ingot to fit a new element" : String.format("good, carrying %.1f A", amps)
				+ (heat > 0.05 ? ", getting hot " + Math.round(heat * 100) + "%" : "");
		return Component.literal((cutout() ? "Fused cutout, " + rating() + " A K link (" : "Fuse " + rating() + " A (") + voltage().label() + "): " + status + (cutout() ? "; the link drops out when it blows: fit a new one" : "; a fuse can't be reset, a breaker can"));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("rating", ratingIndex);
		output.putDouble("heat", heat);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		ratingIndex = Math.max(0, Math.min(Tuning.CUTOUT_K_LINKS.length - 1, input.getIntOr("rating", 0)));
		heat = input.getDoubleOr("heat", 0);
	}
}
