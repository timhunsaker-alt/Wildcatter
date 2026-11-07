package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigPartEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The inside of a {@link StarterCabinetBlock}: one {@link Starter} of the cabinet's kind.
 * <ul>
 * <li>A standalone cabinet takes its line on the top and the back of its top cell, and sends its motor's supply out of the back
 * of its bottom cell.</li>
 * <li>The 4.16 kV drive's control section takes its line at the west end of its lineup's transformer section (its two bottom
 * cells) or on that section's top, and sends the motor's supply out of its own east end (its two bottom cells). Until the lineup
 * is complete it has no terminals.</li>
 * </ul>
 */
public class StarterCabinetBlockEntity extends BlockEntity implements CircuitPart, Inspectable, BigPartEntity, dev.wildcatter.control.ControlDevice, dev.wildcatter.control.Measurable {
	private final Starter starter;
	private final int[] line = new int[3];
	private boolean grown;

	public StarterCabinetBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.STARTER_CABINET_ENTITY, pos, state);
		StarterCabinetBlock block = (StarterCabinetBlock) state.getBlock();
		starter = new Starter(block.kind(), block.voltage(), block.dial());
	}

	private StarterCabinetBlock block() {
		return (StarterCabinetBlock) getBlockState().getBlock();
	}

	private Direction facing() {
		return getBlockState().getValue(StarterCabinetBlock.FACING);
	}

	private Voltage voltage() {
		return block().voltage();
	}

	public Starter starter() {
		return starter;
	}

	/** What the lineup check finds (the 4.16 kV drive only). */
	private PartLayout.Found lineupCheck() {
		return DriveLineup.layout().check(level, worldPosition, facing());
	}

	/** True for a standalone cabinet, or a drive lineup with all its sections. */
	public boolean formed() {
		return !block().lineup() || (level != null && lineupCheck().parts() != null);
	}

	/** The transformer section's main cell, where the lineup's line comes in. */
	private BlockPos transformerSection() {
		return DriveLineup.layout().origin(worldPosition, facing(), 0);
	}

	/** The line's terminals, as {cell, face} pairs. */
	private List<Object[]> lineTerminals() {
		List<Object[]> out = new ArrayList<>();
		Direction back = facing().getOpposite();
		if (!block().lineup()) {
			BlockPos top = worldPosition.above(block().footprint().pieces() - 1);
			out.add(new Object[] {top, Direction.UP});
			out.add(new Object[] {top, back});
			return out;
		}
		if (!formed()) {
			return out;
		}
		BlockPos t = transformerSection();
		Direction west = facing().getCounterClockWise();
		Direction east = facing().getClockWise();
		out.add(new Object[] {t, west});
		out.add(new Object[] {t.relative(back), west});
		for (BlockPos cell : new BlockPos[] {t.above(2), t.above(2).relative(east), t.above(2).relative(back), t.above(2).relative(back).relative(east)}) {
			out.add(new Object[] {cell, Direction.UP});
		}
		return out;
	}

	/** The motor's terminals, as {cell, face} pairs. */
	private List<Object[]> motorTerminals() {
		List<Object[]> out = new ArrayList<>();
		Direction back = facing().getOpposite();
		if (!block().lineup()) {
			out.add(new Object[] {worldPosition, back});
		} else if (formed()) {
			Direction east = facing().getClockWise();
			out.add(new Object[] {worldPosition, east});
			out.add(new Object[] {worldPosition.relative(back), east});
		}
		return out;
	}

	private static boolean has(List<Object[]> terminals, BlockPos cell, Direction face) {
		for (Object[] t : terminals) {
			if (t[0].equals(cell) && t[1] == face) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String deviceName() {
		return starter.kind() == Starter.Kind.DRIVE ? "Variable-frequency drive" : "Soft starter";
	}

	@Override
	public java.util.List<Point> points() {
		return starter.points();
	}

	@Override
	public double readMa(int point) {
		return starter.readMa(point);
	}

	@Override
	public void commandMa(int point, double milliamps, long gameTime) {
		if (point == 4) {
			starter.commandSpeed(milliamps, gameTime);
		}
	}

	public Starter starterForTest() {
		return starter;
	}

	@Override
	public double measure(Quantity quantity) {
		return starter.measure(quantity);
	}

	@Override
	public boolean readPoint(int point) {
		return starter.readPoint(point);
	}

	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
		if (point == 0) {
			starter.noteTime(gameTime);
			starter.command(on, gameTime);
			follow(gameTime);
		}
	}

	private void follow(long now) {
		if (starter.followCommand(now)) {
			setChanged();
			if (level instanceof ServerLevel server) {
				CircuitManager.markDirty(server);
			}
		}
	}

	@Override
	public String cycleHoa(BlockPos cell) {
		if (starter.cycleHoa() && level instanceof ServerLevel server) {
			CircuitManager.markDirty(server);
		}
		setChanged();
		return starter.hoa().name();
	}

	public void pushButton() {
		if (starter.pushButton()) {
			setChanged();
		}
	}

	public void nextOverloadSetting() {
		starter.nextOverloadSetting();
		setChanged();
	}

	public void nextLimitSetting() {
		starter.nextLimitSetting();
		setChanged();
	}

	public void redstone(boolean signal) {
		if (starter.redstone(signal)) {
			setChanged();
		}
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return hasPortAt(worldPosition, face, role, voltage);
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return voltage == voltage() && role != Conductor.N && (has(lineTerminals(), cell, face) || has(motorTerminals(), cell, face));
	}

	/** The lineup's transformer section is solved with it: the line comes in there. */
	@Override
	public java.util.Collection<BlockPos> links() {
		return block().lineup() && formed() ? List.of(transformerSection()) : List.of();
	}

	@Override
	public void build(Circuit circuit) {
		int ground = circuit.node();
		for (int k = 0; k < 3; k++) {
			line[k] = circuit.node();
		}
		starter.build(circuit, line, formed());
		for (Object[] t : lineTerminals()) {
			Starter.linePort(circuit, (BlockPos) t[0], (Direction) t[1], voltage(), line, ground);
		}
		for (Object[] t : motorTerminals()) {
			starter.motorPort(circuit, (BlockPos) t[0], (Direction) t[1], ground);
		}
	}

	@Override
	public boolean protect(Circuit circuit, int ticks) {
		if (!starter.protect(circuit)) {
			return false;
		}
		tripped();
		return true;
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		if (starter.apply(circuit, ticks)) {
			tripped();
		}
		if (level != null) {
			follow(level.getGameTime());
		}
		setChanged();
	}

	private void tripped() {
		if (level instanceof ServerLevel server) {
			server.playSound(null, worldPosition, WildcatterSounds.BREAKER_TRIP, SoundSource.BLOCKS, 1.0F, 1.2F);
			CircuitManager.markDirty(server);
		}
	}

	@Override
	public Component inspect() {
		String name = switch (starter.kind()) {
			case DIRECT -> "Motor starter";
			case SOFT -> "Soft starter";
			case DRIVE -> "Drive";
		};
		String head = name + " (" + voltage().label() + " three-phase): ";
		if (block().lineup() && level != null) {
			PartLayout.Found found = lineupCheck();
			if (found.parts() == null) {
				return Component.literal(head + "NOT BUILT: " + found.missing());
			}
		}
		return Component.literal(head + starter.state());
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
		starter.save(output, "");
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		starter.load(input, "");
	}

	/** For a section of its lineup: the control section of the lineup a section at {@code main} would belong to, if it stands. */
	public static @Nullable StarterCabinetBlockEntity controlFor(net.minecraft.world.level.Level level, BlockState section, BlockPos main) {
		Direction facing = section.getValue(BigPartBlock.FACING);
		BlockPos control = DriveLineup.layout().controllerFrom(section.getBlock(), main, facing, 0);
		return level.getBlockEntity(control) instanceof StarterCabinetBlockEntity cabinet && cabinet.block().lineup() ? cabinet : null;
	}
}
