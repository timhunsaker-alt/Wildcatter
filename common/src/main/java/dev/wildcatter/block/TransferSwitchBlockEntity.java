package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.ElectricalStandard;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.multiblock.BigParts;
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
 * The inside of a 480 V open-transition automatic transfer switch (docs/multiblocks/ats.md): two sets of three-pole contacts, NORMAL to
 * LOAD and EMERGENCY to LOAD, never closed together, and the controller that runs them.
 * <ul>
 * <li>Normal fails below 85 % of nominal on any phase (or under a test); after TDES it calls for the genset (redstone).</li>
 * <li>Once emergency is at 90 % of voltage and 95 % of frequency, after TDNE, it opens normal, waits in the neutral position, and closes
 * emergency.</li>
 * <li>Normal restored at 90 %: after TDEN it opens emergency, waits, closes normal, and after TDEC it drops the call.</li>
 * </ul>
 * The neutral is solid and the ground runs through.
 */
public class TransferSwitchBlockEntity extends BlockEntity implements CircuitPart, Inspectable, BigPartEntity {
	public enum Stage {
		NORMAL, TDES, START, TDNE, TO_EMERGENCY, EMERGENCY, TDEN, TO_NORMAL, TDEC;

		boolean normalClosed() {
			return this == NORMAL || this == TDES || this == START || this == TDNE || this == TDEC;
		}

		boolean emergencyClosed() {
			return this == EMERGENCY || this == TDEN;
		}

		boolean engineStart() {
			return this != NORMAL && this != TDES;
		}
	}

	private static final Conductor[] PHASES = {Conductor.A, Conductor.B, Conductor.C};

	private boolean grown;
	private Stage stage = Stage.NORMAL;
	private double timer;
	private boolean test;
	private final int[] normal = new int[3];
	private final int[] emergency = new int[3];
	private int[] links = new int[0];
	private double amps;
	private double normalVolts;
	private double emergencyVolts;
	private double hertz;
	/** Set if the two sets of contacts were ever closed together: never, by construction; the game test reads it. */
	private boolean paralleled;

	public TransferSwitchBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.TRANSFER_SWITCH_ENTITY, pos, state);
	}

	private Direction facing() {
		return getBlockState().getValue(TransferSwitchBlock.FACING);
	}

	public Stage stage() {
		return stage;
	}

	public boolean engineStart() {
		return stage.engineStart();
	}

	public boolean test() {
		return test;
	}

	public void setTest(boolean value) {
		test = value;
		setChanged();
	}

	public boolean paralleled() {
		return paralleled;
	}

	public double amps() {
		return amps;
	}

	private BlockPos topCell() {
		return BigParts.cell(worldPosition, getBlockState(), 1);
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return hasPortAt(worldPosition, face, role, voltage);
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.INDUSTRIAL && (cell.equals(topCell()) && (face == Direction.UP || face == facing().getOpposite())
				|| cell.equals(worldPosition) && face == Direction.DOWN);
	}

	@Override
	public void build(Circuit circuit) {
		boolean n = stage.normalClosed();
		boolean e = stage.emergencyClosed();
		paralleled |= n && e;
		links = new int[(n ? 3 : 0) + (e ? 3 : 0)];
		int used = 0;
		for (int k = 0; k < 3; k++) {
			normal[k] = circuit.node();
			emergency[k] = circuit.node();
			int load = circuit.node();
			circuit.port(topCell(), Direction.UP, PHASES[k], Voltage.INDUSTRIAL, normal[k], Tuning.CONTACT_OHMS / 2, false);
			circuit.port(topCell(), facing().getOpposite(), PHASES[k], Voltage.INDUSTRIAL, emergency[k], Tuning.CONTACT_OHMS / 2, false);
			circuit.port(worldPosition, Direction.DOWN, PHASES[k], Voltage.INDUSTRIAL, load, Tuning.CONTACT_OHMS / 2, false);
			if (n) {
				links[used++] = circuit.resistor(normal[k], load, Tuning.BREAKER_CLOSED_OHMS);
			}
			if (e) {
				links[used++] = circuit.resistor(emergency[k], load, Tuning.BREAKER_CLOSED_OHMS);
			}
		}
		// a solid neutral: normal, emergency and load neutrals are one; the ground runs through
		for (Conductor role : new Conductor[] {Conductor.N, Conductor.G}) {
			int node = circuit.node();
			circuit.port(topCell(), Direction.UP, role, Voltage.INDUSTRIAL, node, Tuning.CONTACT_OHMS / 2, false);
			circuit.port(topCell(), facing().getOpposite(), role, Voltage.INDUSTRIAL, node, Tuning.CONTACT_OHMS / 2, false);
			circuit.port(worldPosition, Direction.DOWN, role, Voltage.INDUSTRIAL, node, Tuning.CONTACT_OHMS / 2, false);
		}
	}

	/** The lowest phase voltage of a source, as a share of nominal. */
	private static double lowest(Circuit circuit, int[] nodes) {
		double low = Double.MAX_VALUE;
		for (int node : nodes) {
			low = Math.min(low, circuit.voltage(node));
		}
		return low / Voltage.INDUSTRIAL.nominal();
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		double most = 0;
		for (int link : links) {
			most = Math.max(most, circuit.current(link));
		}
		amps = most;
		double n = lowest(circuit, normal);
		double e = lowest(circuit, emergency);
		normalVolts = n * Voltage.INDUSTRIAL.lineToLine();
		emergencyVolts = e * Voltage.INDUSTRIAL.lineToLine();
		hertz = circuit.hertz();
		boolean normalFailed = test || n < Tuning.ATS_DROPOUT;
		boolean normalRestored = !test && n >= Tuning.ATS_PICKUP;
		boolean emergencyOk = e >= Tuning.ATS_PICKUP && hertz >= Tuning.ATS_EMERGENCY_HERTZ * ElectricalStandard.active().hertz();
		timer += ticks / 20.0;
		Stage was = stage;
		switch (stage) {
			case NORMAL -> {
				if (normalFailed) {
					go(Stage.TDES);
				}
			}
			case TDES -> {
				if (!normalFailed) {
					go(Stage.NORMAL);
				} else if (timer >= Tuning.ATS_TDES_SECONDS) {
					go(Stage.START);
				}
			}
			case START -> {
				if (!normalFailed) {
					go(Stage.TDEC);   // normal came back before the transfer: cool the engine down
				} else if (emergencyOk) {
					go(Stage.TDNE);
				}
			}
			case TDNE -> {
				if (!emergencyOk) {
					go(Stage.START);
				} else if (timer >= Tuning.ATS_TDNE_SECONDS) {
					go(Stage.TO_EMERGENCY);
				}
			}
			case TO_EMERGENCY -> {
				if (timer >= Tuning.ATS_NEUTRAL_SECONDS) {
					go(Stage.EMERGENCY);
				}
			}
			case EMERGENCY -> {
				if (!emergencyOk && normalRestored) {
					go(Stage.TO_NORMAL);   // the genset failed and normal is there: back at once
				} else if (normalRestored) {
					go(Stage.TDEN);
				}
			}
			case TDEN -> {
				if (!emergencyOk) {
					go(Stage.TO_NORMAL);
				} else if (!normalRestored) {
					go(Stage.EMERGENCY);
				} else if (timer >= Tuning.ATS_TDEN_SECONDS) {
					go(Stage.TO_NORMAL);
				}
			}
			case TO_NORMAL -> {
				if (timer >= Tuning.ATS_NEUTRAL_SECONDS) {
					go(Stage.TDEC);
				}
			}
			case TDEC -> {
				if (normalFailed) {
					go(Stage.START);   // normal failed again while the engine ran on: it is still running
				} else if (timer >= Tuning.ATS_TDEC_SECONDS) {
					go(Stage.NORMAL);
				}
			}
		}
		if (stage != was && level instanceof ServerLevel server) {
			if (stage.normalClosed() != was.normalClosed() || stage.emergencyClosed() != was.emergencyClosed()) {
				server.playSound(null, worldPosition, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 1.2F, 0.6F);
				CircuitManager.markDirty(server);
			}
			if (stage.engineStart() != was.engineStart()) {
				for (BlockPos cell : BigParts.cells(worldPosition, getBlockState())) {
					server.updateNeighborsAt(cell, getBlockState().getBlock());
				}
			}
		}
	}

	private void go(Stage next) {
		stage = next;
		timer = 0;
		setChanged();
	}

	@Override
	public Component inspect() {
		String on = switch (stage) {
			case NORMAL, TDES, START, TDNE, TDEC -> String.format("on NORMAL, %,.0f A", amps);
			case EMERGENCY, TDEN -> String.format("on EMERGENCY, %,.0f A", amps);
			case TO_EMERGENCY, TO_NORMAL -> "in the NEUTRAL position (open transition)";
		};
		String timing = switch (stage) {
			case TDES -> String.format("; normal failed: engine start in %.0f s", Math.max(0, Tuning.ATS_TDES_SECONDS - timer));
			case START -> "; waiting for emergency to be acceptable";
			case TDNE -> String.format("; transfer to emergency in %.0f s", Math.max(0, Tuning.ATS_TDNE_SECONDS - timer));
			case TDEN -> String.format("; normal restored: retransfer in %.0f s", Math.max(0, Tuning.ATS_TDEN_SECONDS - timer));
			case TDEC -> String.format("; engine cooldown, %.0f s left", Math.max(0, Tuning.ATS_TDEC_SECONDS - timer));
			default -> "";
		};
		return Component.literal(String.format("Transfer switch %s, %d A, open transition: %s%s; normal %,.0f V, emergency %,.0f V, %.1f Hz; engine start %s%s",
				Voltage.INDUSTRIAL.label(), Tuning.ATS_AMPS, on, timing, normalVolts, emergencyVolts, hertz, engineStart() ? "ON" : "off",
				test ? "; TEST: normal treated as failed (sneak and right-click to end it)" : ""));
	}

	@Override
	public boolean grown() {
		return grown;
	}

	@Override
	public void markGrown() {
		grown = true;
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("grown", grown);
		output.putString("stage", stage.name());
		output.putDouble("timer", timer);
		output.putBoolean("test", test);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		stage = Stage.NORMAL;
		for (Stage s : Stage.values()) {
			if (s.name().equals(input.getStringOr("stage", ""))) {
				stage = s;
			}
		}
		timer = input.getDoubleOr("timer", 0);
		test = input.getBooleanOr("test", false);
	}
}
