package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.safety.Bondable;
import dev.wildcatter.safety.Grounding;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Splits crude into diesel and plastic. It is a 110 V load that draws its power whether or not there is crude to
 * refine; with low voltage it works slower. It pulls crude from a run of steel pipe on the base's sides or bottom (a separator's
 * oil side or a stock tank) when its tank has room, and its diesel comes off the top: any face of a column, or the base's top face
 * on a pot still (docs/multiblocks/lease.md: one product per run).
 */
public class RefineryBlockEntity extends BlockEntity implements CircuitPart, Inspectable, Bondable, dev.wildcatter.kinetic.Sounding, FluidNode {
	/** Its induction motor (single-phase); it turns only while there is work. */
	private final dev.wildcatter.circuit.Motor motor = new dev.wildcatter.circuit.Motor(false, Voltage.LOW, Tuning.REFINERY_WATTS);
	private final dev.wildcatter.kinetic.RunningSound running = new dev.wildcatter.kinetic.RunningSound();
	/** Set by the machine's own logic: whether it has work for the motor. */
	private boolean wanted;
	private double volts;
	private double amps;
	private double crude;
	private double diesel;
	private double plasticProgress;
	private int plastic;
	private String status = "starting";
	/** Two columns stacked on the base make it a full tower. */
	private boolean tower;

	public RefineryBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.REFINERY_ENTITY, pos, state);
	}

	public boolean isLive() {
		return Math.abs(volts) > Tuning.SHOCK_MIN_VOLTS;
	}

	public boolean addCrude(double units) {
		if (crude + units > Tuning.REFINERY_TANK) {
			return false;
		}
		crude += units;
		setChanged();
		return true;
	}

	public boolean takeDiesel(double units) {
		if (diesel < units) {
			return false;
		}
		diesel -= units;
		setChanged();
		return true;
	}

	/** A diesel engine on the gas lines draws from the tank: returns how much it got. */
	public double drawDiesel(double max) {
		double got = Math.min(max, diesel);
		if (got > 0) {
			diesel -= got;
			setChanged();
		}
		return got;
	}

	private int columnsAbove(Level level) {
		int n = 0;
		while (n < Tuning.REFINERY_COLUMNS && level.getBlockState(worldPosition.above(n + 1)).is(WildcatterBlocks.REFINERY_COLUMN)) {
			n++;
		}
		return n;
	}

	public int takePlastic() {
		int sheets = plastic;
		plastic = 0;
		if (sheets > 0) {
			setChanged();
		}
		return sheets;
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == motor.voltage() && (motor.threePhase() ? role != Conductor.N : true);
	}

	@Override
	public void build(Circuit circuit) {
		motor.build(circuit, worldPosition);
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		motor.apply(circuit, ticks, wanted);
		volts = motor.volts();
		amps = motor.amps();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, RefineryBlockEntity refinery) {
		refinery.running.update(refinery, refinery.motor.output() > 0);   // the client plays its loop while it runs
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		if ((level.getGameTime() / Tuning.WELL_INTERVAL) % 5 == 0) {
			boolean was = refinery.tower;
			refinery.tower = refinery.columnsAbove(level) >= Tuning.REFINERY_COLUMNS;
			if (refinery.tower && !was) {
				level.playSound(null, pos.above(), WildcatterSounds.STRUCTURE_FORMED, SoundSource.BLOCKS, 1.0F, 1.3F);
			}
		}
		if (refinery.crude < Tuning.REFINERY_TANK) {
			List<Lines.Face> inlet = Lines.faces(List.of(pos)).stream().filter(f -> f.side() != Direction.UP).toList();
			refinery.crude += Lines.pull(level, inlet, Product.CRUDE, Tuning.REFINERY_TANK - refinery.crude, Pull.MACHINE, refinery);
		}
		refinery.wanted = refinery.crude >= 1 && refinery.plastic < Tuning.PLASTIC_BIN && refinery.diesel < Tuning.REFINERY_TANK;
		double strength = refinery.motor.output();
		if (refinery.crude < 1) {
			refinery.status = "idle: no crude (pour in a bucket, or pipe it from a separator's oil side or a stock tank)";
		} else if (refinery.plastic >= Tuning.PLASTIC_BIN) {
			refinery.status = "STOPPED: plastic bin full, collect it with an empty hand";
		} else if (refinery.diesel >= Tuning.REFINERY_TANK) {
			refinery.status = "STOPPED: diesel tank full";
		} else if (strength == 0) {
			refinery.status = refinery.motor.describe();
		} else {
			double yield = refinery.tower ? Tuning.DIESEL_YIELD : Tuning.POT_STILL_YIELD;
			double room = (Tuning.REFINERY_TANK - refinery.diesel) / yield;
			double speed = refinery.tower ? 1.0 : Tuning.POT_STILL_SPEED;
			double amount = Math.min(Math.min(refinery.crude, room), Tuning.REFINERY_CRUDE_PER_TICK * Tuning.WELL_INTERVAL * strength * speed);
			refinery.crude -= amount;
			refinery.diesel += amount * yield;
			if (refinery.tower) {
				refinery.plasticProgress += amount;
				while (refinery.plasticProgress >= Tuning.CRUDE_PER_PLASTIC) {
					refinery.plasticProgress -= Tuning.CRUDE_PER_PLASTIC;
					refinery.plastic++;
				}
			}
			refinery.status = "refining " + Math.round(amount * 20.0 / Tuning.WELL_INTERVAL) + " crude/s";
		}
		refinery.setChanged();
	}

	// ---- its diesel on steel pipe off the top ---------------------------------------------------------------------------------
	@Override
	public @Nullable Product offers(BlockPos cell, Direction side) {
		boolean top = cell.equals(worldPosition) ? side == Direction.UP && level != null && !level.getBlockState(worldPosition.above()).is(WildcatterBlocks.REFINERY_COLUMN)
				: true;   // a column's cell
		return top ? Product.DIESEL : null;
	}

	@Override
	public double available(BlockPos cell, Direction side, Product product, Pull pull) {
		return product == Product.DIESEL && pull != Pull.FLARE ? diesel : 0;
	}

	@Override
	public double take(BlockPos cell, Direction side, Product product, double max, Pull pull) {
		return available(cell, side, product, pull) > 0 ? drawDiesel(max) : 0;
	}

	@Override
	public Component inspect() {
		String kind = tower ? "Refinery tower" : "Pot still (stack " + Tuning.REFINERY_COLUMNS + " columns on top for a full tower: more diesel, faster, and plastic)";
		return Component.literal(kind + " (rated " + Math.round(Tuning.REFINERY_WATTS) + " W at " + Voltage.LOW.label() + "): " + String.format("%.0f V, %.2f A", volts, amps)
				+ ", crude " + (int) crude + "/" + Tuning.REFINERY_TANK + ", diesel " + (int) diesel + "/" + Tuning.REFINERY_TANK
				+ ", plastic " + plastic + " sheets, " + status + "; "
				+ (level != null && Grounding.isGrounded(level, worldPosition) ? "grounded" : "UNGROUNDED, touching it can shock you"));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("crude", crude);
		output.putDouble("diesel", diesel);
		output.putDouble("plastic_progress", plasticProgress);
		output.putInt("plastic", plastic);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		running.load(input);
		crude = input.getDoubleOr("crude", 0);
		diesel = input.getDoubleOr("diesel", 0);
		plasticProgress = input.getDoubleOr("plastic_progress", 0);
		plastic = input.getIntOr("plastic", 0);
	}

	@Override
	public net.minecraft.sounds.SoundEvent loop() {
		return running.on() ? WildcatterSounds.FAN : null;
	}

	@Override
	public float soundPitch() {
		return 0.9F;
	}

	@Override
	public float soundVolume() {
		return running.on() ? 0.5F : 0;
	}

	@Override
	public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
		return running.tag();
	}
}
