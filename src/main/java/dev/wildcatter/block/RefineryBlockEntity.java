package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Role;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.safety.Bondable;
import dev.wildcatter.safety.Grounding;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Splits crude into diesel and plastic. It is a 110 V load that draws its power whether or not there is crude to
 * refine; with low voltage it works slower. It pulls crude from pumpjacks on its gas lines when its tank has room.
 */
public class RefineryBlockEntity extends BlockEntity implements CircuitPart, Inspectable, Bondable {
	private int hot;
	private int neutral;
	private int resistor;
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
	public boolean hasPort(Direction face, Role role, Voltage voltage) {
		return voltage == Voltage.LOW;
	}

	@Override
	public void build(Circuit circuit) {
		hot = circuit.node();
		neutral = circuit.node();
		double nominal = Voltage.LOW.nominal();
		resistor = circuit.resistor(hot, neutral, nominal * nominal / Tuning.REFINERY_WATTS);
		for (Direction face : Direction.values()) {
			circuit.port(worldPosition, face, Role.HOT, Voltage.LOW, hot, Tuning.CONTACT_OHMS / 2, false);
			circuit.port(worldPosition, face, Role.NEUTRAL, Voltage.LOW, neutral, Tuning.CONTACT_OHMS / 2, false);
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		volts = circuit.across(hot, neutral);
		amps = circuit.current(resistor);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, RefineryBlockEntity refinery) {
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
		double nominal = Voltage.LOW.nominal();
		double strength = refinery.volts < nominal * Tuning.BRIGHT_ABOVE ? 0 : Math.min(1.0, Math.pow(refinery.volts / nominal, 2));
		List<BlockPos> cell = List.of(pos);
		if (refinery.crude < Tuning.REFINERY_TANK) {
			Set<BlockPos> lines = Plumbing.network(level, cell, PipeBlock.Kind.GAS);
			for (PumpjackBlockEntity jack : Plumbing.machines(level, cell, lines, true, PumpjackBlockEntity.class)) {
				refinery.crude += jack.take(Tuning.REFINERY_TANK - refinery.crude);
			}
		}
		if (strength == 0) {
			refinery.status = refinery.volts < 1.0 ? "NO POWER: wire a hot and a neutral to it" : "too little voltage to run";
		} else if (refinery.crude < 1) {
			refinery.status = "idle: no crude (pour in a bucket, or put it on a gas line with a pumpjack)";
		} else if (refinery.plastic >= Tuning.PLASTIC_BIN) {
			refinery.status = "STOPPED: plastic bin full, collect it with an empty hand";
		} else if (refinery.diesel >= Tuning.REFINERY_TANK) {
			refinery.status = "STOPPED: diesel tank full";
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
			if ((level.getGameTime() / Tuning.WELL_INTERVAL) % 12 == 0) {
				level.playSound(null, pos, WildcatterSounds.BOILER_HISS, SoundSource.BLOCKS, 0.4F, 0.8F);
			}
		}
		refinery.setChanged();
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
		crude = input.getDoubleOr("crude", 0);
		diesel = input.getDoubleOr("diesel", 0);
		plasticProgress = input.getDoubleOr("plastic_progress", 0);
		plastic = input.getIntOr("plastic", 0);
	}
}
