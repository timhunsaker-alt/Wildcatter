package dev.wildcatter.control;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.Starter;
import dev.wildcatter.energy.Inspectable;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The contacts and lamps of an {@link OperatorDeviceBlock}. Lamps and horns go dark when the PLC stops commanding them. */
public class OperatorDeviceBlockEntity extends BlockEntity implements ControlDevice, Inspectable {
	/** A click holds a pushbutton in for this long, as a hand does. */
	private static final int PRESS_TICKS = 10;
	private static final int HORN_EVERY = 20;
	private boolean latched;
	private int pressed;
	private final boolean[] outputs = new boolean[3];
	private final long[] commandTick = {-100, -100, -100};

	public OperatorDeviceBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.OPERATOR_DEVICE_ENTITY, pos, state);
	}

	private OperatorDeviceBlock.Kind kind() {
		return ((OperatorDeviceBlock) getBlockState().getBlock()).kind();
	}

	/** A hand on it. True if it moved. */
	public boolean operate() {
		switch (kind()) {
			case PUSHBUTTON -> pressed = PRESS_TICKS;
			case SELECTOR, ESTOP, PULL_CORD -> latched = !latched;
			default -> {
				return false;
			}
		}
		setChanged();
		return true;
	}

	/** A cut wire reads the same as a press on the e-stop, whose contact is normally closed. */
	private boolean contact() {
		return switch (kind()) {
			case PUSHBUTTON -> pressed > 0;
			case SELECTOR -> latched;
			case ESTOP, PULL_CORD -> !latched;
			default -> false;
		};
	}

	@Override
	public String deviceName() {
		return switch (kind()) {
			case PUSHBUTTON -> "Pushbutton";
			case SELECTOR -> "Selector switch";
			case ESTOP -> "E-stop";
			case PULL_CORD -> "Pull cord switch";
			case PILOT_LIGHT -> "Pilot light";
			case STACK_LIGHT -> "Stack light";
			case HORN -> "Horn";
		};
	}

	@Override
	public List<Point> points() {
		return switch (kind()) {
			case PUSHBUTTON -> List.of(new Point("PRESSED", false));
			case SELECTOR -> List.of(new Point("ON", false));
			case ESTOP, PULL_CORD -> List.of(new Point("HEALTHY (NC)", false));
			case PILOT_LIGHT -> List.of(new Point("LAMP", true));
			case STACK_LIGHT -> List.of(new Point("RED", true), new Point("AMBER", true), new Point("GREEN", true));
			case HORN -> List.of(new Point("SOUND", true));
		};
	}

	/** A 24 V LED pilot light 0.02 A, a stack light's LED tier 0.1 A, a 100 dB horn 0.25 A (docs/questions/plc-gaps.md). */
	@Override
	public double outputAmps(int point) {
		return switch (kind()) {
			case PILOT_LIGHT -> 0.02;
			case STACK_LIGHT -> 0.1;
			case HORN -> 0.25;
			default -> 0;
		};
	}

	@Override
	public boolean readPoint(int point) {
		return contact();
	}

	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
		if (point >= 0 && point < outputs.length) {
			outputs[point] = on;
			commandTick[point] = gameTime;
		}
	}

	private boolean output(int point, long now) {
		return outputs[point] && now - commandTick[point] <= Starter.COMMAND_STALE_TICKS;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, OperatorDeviceBlockEntity device) {
		long now = level.getGameTime();
		int lamps = switch (device.kind()) {
			case PUSHBUTTON -> device.pressed > 0 ? 1 : 0;
			case SELECTOR, ESTOP, PULL_CORD -> device.latched ? 1 : 0;
			case STACK_LIGHT -> (device.output(0, now) ? 1 : 0) | (device.output(1, now) ? 2 : 0) | (device.output(2, now) ? 4 : 0);
			default -> device.output(0, now) ? 1 : 0;
		};
		if (device.pressed > 0) {
			device.pressed--;
		}
		if (device.kind() == OperatorDeviceBlock.Kind.HORN && device.output(0, now) && now % HORN_EVERY == 0) {
			level.playSound(null, pos, SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), SoundSource.BLOCKS, 1.5F, 1.0F);
		}
		if (state.getValue(OperatorDeviceBlock.LAMPS) != lamps) {
			level.setBlock(pos, state.setValue(OperatorDeviceBlock.LAMPS, lamps), 3);
		}
	}

	@Override
	public Component inspect() {
		return Component.literal(deviceName() + " (24 V): " + switch (kind()) {
			case PUSHBUTTON -> pressed > 0 ? "pressed" : "released";
			case SELECTOR -> latched ? "ON" : "OFF";
			case ESTOP -> latched ? "PRESSED: click to release" : "healthy";
			case PULL_CORD -> latched ? "PULLED: click to reset it" : "healthy";
			default -> getBlockState().getValue(OperatorDeviceBlock.LAMPS) != 0 ? "on" : "off";
		});
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("latched", latched);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		latched = input.getBooleanOr("latched", false);
	}
}
