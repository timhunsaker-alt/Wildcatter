package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.DriveLoad;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigPartEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The land rig's drive house (docs/multiblocks/land-rig.md): a 2,500 kVA 4.16 kV / 600 V rig transformer and the drives for
 * every motor on the rig, seen from the line as one {@link DriveLoad}. Its 4.16 kV terminal is the west end of its main cell
 * (facing north); it draws what the substructure asks for, within its power limit.
 */
public class RigDriveHouseBlockEntity extends BlockEntity implements CircuitPart, Inspectable, BigPartEntity {
	private final DriveLoad load = new DriveLoad(Voltage.MEDIUM);
	private boolean grown;

	public RigDriveHouseBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.RIG_DRIVE_HOUSE_ENTITY, pos, state);
	}

	public DriveLoad load() {
		return load;
	}

	public double delivered() {
		return load.delivered();
	}

	public boolean undervoltage() {
		return load.undervoltage();
	}

	private Direction terminalFace() {
		return getBlockState().getValue(RigPartBlock.FACING).getCounterClockWise();
	}

	private RigSubstructureBlockEntity rig() {
		return level == null ? null : RigPartBlock.controller(level, worldPosition, getBlockState());
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return face == terminalFace() && voltage == Voltage.MEDIUM && role != Conductor.N;
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return cell.equals(worldPosition) && hasPort(face, role, voltage);
	}

	@Override
	public void build(Circuit circuit) {
		RigSubstructureBlockEntity rig = rig();
		load.setDemand(rig != null ? rig.demand() : 0);
		load.build(circuit, worldPosition, List.of(terminalFace()));
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		load.apply(circuit, ticks);
		setChanged();
	}

	@Override
	public Component inspect() {
		RigSubstructureBlockEntity rig = rig();
		return Component.literal("Rig drive house (2,500 kVA, " + Voltage.MEDIUM.label() + " three-phase in, its terminal on this end): "
				+ load.describe() + (rig == null ? "; NOT PART OF A RIG: set it in its place by a substructure" : ""));
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
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", grown);
	}
}
