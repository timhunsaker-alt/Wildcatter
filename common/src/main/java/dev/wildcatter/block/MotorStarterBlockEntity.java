package dev.wildcatter.block;

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
 * The inside of a standalone {@link MotorStarterBlock}: one {@link Starter}, its line on the back face and its motor on the front.
 * Its run state shows in the block state. (An older world's one-block MCC bucket shares this entity until it drops; it does
 * nothing in the meantime.)
 */
public class MotorStarterBlockEntity extends BlockEntity implements CircuitPart, Inspectable, dev.wildcatter.control.ControlDevice, dev.wildcatter.control.Measurable {
	private final Starter starter = new Starter();
	private final int[] line = new int[3];
	private boolean loadedRun;

	public MotorStarterBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.MOTOR_STARTER_ENTITY, pos, state);
	}

	private MotorStarterBlock block() {
		return (MotorStarterBlock) getBlockState().getBlock();
	}

	private Direction facing() {
		return getBlockState().getValue(MotorStarterBlock.FACING);
	}

	@Override
	public double measure(Quantity quantity) {
		return starter.measure(quantity);
	}

	@Override
	public String deviceName() {
		return "Motor starter";
	}

	@Override
	public java.util.List<Point> points() {
		return block().bucket() ? java.util.List.of() : Starter.POINTS;
	}

	@Override
	public boolean readPoint(int point) {
		return starter.readPoint(point);
	}

	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
		if (point == 0) {
			starter.command(on, gameTime);
			follow(gameTime);
		}
	}

	private void follow(long now) {
		if (starter.followCommand(now)) {
			show();
			if (level instanceof ServerLevel server) {
				CircuitManager.markDirty(server);
			}
		}
	}

	@Override
	public String cycleHoa(BlockPos cell) {
		if (block().bucket()) {
			return null;
		}
		if (starter.cycleHoa()) {
			show();
			if (level instanceof ServerLevel server) {
				CircuitManager.markDirty(server);
			}
		}
		setChanged();
		return starter.hoa().name();
	}

	public MotorStarterBlock.Run run() {
		return starter.run();
	}

	public double overloadAmps() {
		return starter.overloadAmps();
	}

	public void nextOverloadSetting() {
		starter.nextOverloadSetting();
		setChanged();
	}

	public void pushButton() {
		if (starter.pushButton()) {
			show();
		}
	}

	public void redstone(boolean signal) {
		if (starter.redstone(signal)) {
			show();
		}
		setChanged();
	}

	/** Puts the starter's run state into the block state, for its lights. */
	private void show() {
		setChanged();
		if (level != null && getBlockState().getValue(MotorStarterBlock.RUN) != starter.run()) {
			level.setBlock(worldPosition, getBlockState().setValue(MotorStarterBlock.RUN, starter.run()), 3);
		}
	}

	@Override
	public void setLevel(net.minecraft.world.level.Level level) {
		super.setLevel(level);
		if (!loadedRun) {   // an older save kept the run state only in the block state
			starter.setRun(getBlockState().getValue(MotorStarterBlock.RUN));
			loadedRun = true;
		}
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return !block().bucket() && voltage == Voltage.INDUSTRIAL && (face == facing().getOpposite() || face == facing());
	}

	@Override
	public void build(Circuit circuit) {
		if (block().bucket()) {
			return;
		}
		int ground = circuit.node();
		for (int k = 0; k < 3; k++) {
			line[k] = circuit.node();
		}
		starter.build(circuit, line, true);
		Starter.linePort(circuit, worldPosition, facing().getOpposite(), line, ground);
		starter.motorPort(circuit, worldPosition, facing(), ground);
	}

	@Override
	public boolean protect(Circuit circuit, int ticks) {
		if (block().bucket() || !starter.protect(circuit)) {
			return false;
		}
		tripped();
		return true;
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		if (!block().bucket() && starter.apply(circuit, ticks)) {
			tripped();
		}
		if (!block().bucket() && level != null) {
			follow(level.getGameTime());
		}
	}

	private void tripped() {
		show();
		if (level instanceof ServerLevel server) {
			server.playSound(null, worldPosition, WildcatterSounds.BREAKER_TRIP, SoundSource.BLOCKS, 1.0F, 1.2F);
			CircuitManager.markDirty(server);
		}
	}

	@Override
	public Component inspect() {
		return Component.literal("Motor starter (" + Voltage.INDUSTRIAL.label() + " three-phase): " + starter.state());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		starter.save(output, "");
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		starter.load(input, "");
		loadedRun = input.getStringOr("run", "").length() > 0;
	}
}
