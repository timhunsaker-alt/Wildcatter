package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.control.Measurable;
import dev.wildcatter.control.ValveBlockEntity;
import dev.wildcatter.energy.Inspectable;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Water in, through a pump's pipes; water out through each valve touching it, with the valve's opening and the square root of the head.
 * Its level goes to the client, which draws it in the tank's sight glass (DayTankRenderer).
 */
public class DayTankBlockEntity extends BlockEntity implements WaterUser, Inspectable, Measurable {
	/** The client is told when the level has moved this far (a 2 L step of the 1,000 L tank) since it was last told. */
	private static final double SYNC_STEP = 0.002;
	private double litres;
	private double outflow;
	private double sentLevel = -1;
	/** Client side: the level drawn, easing toward the level last received, and when it was last drawn (DayTankRenderer). */
	public double shownLevel = -1;
	public long shownAt;

	public DayTankBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.DAY_TANK_ENTITY, pos, state);
	}

	public void setLitres(double to) {
		litres = to;
		setChanged();
	}

	@Override
	public double waterLevel() {
		return litres / Tuning.DAY_TANK_LITRES;
	}

	@Override
	public int addWater(int amount) {
		int fitted = (int) Math.min(amount, Tuning.DAY_TANK_LITRES - litres);
		litres += fitted;
		setChanged();
		return Math.max(0, fitted);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, DayTankBlockEntity tank) {
		double rate = 0;
		for (Direction side : Direction.values()) {
			if (level.getBlockEntity(pos.relative(side)) instanceof ValveBlockEntity valve && level.getBlockState(pos.relative(side)).getBlock() instanceof PipeBlock pipe && pipe.kind() == PipeBlock.Kind.WATER) {
				rate += Tuning.DAY_TANK_OUTLET_LITRES_PER_SECOND * valve.position() * Math.sqrt(Math.max(0, tank.waterLevel()));
			}
		}
		tank.outflow = Math.min(rate, tank.litres * 20);
		if (tank.outflow > 0) {
			tank.litres = Math.max(0, tank.litres - tank.outflow / 20);
			tank.setChanged();
		}
		double now = tank.waterLevel();
		if (Math.abs(now - tank.sentLevel) >= SYNC_STEP || (now != tank.sentLevel && level.getGameTime() % 20 == 0)) {
			tank.sentLevel = now;
			level.sendBlockUpdated(pos, state, state, 3);
		}
	}

	@Override
	public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}

	/** Only what the client draws: the water in it. */
	@Override
	public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
		net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
		tag.putDouble("litres", litres);
		return tag;
	}

	@Override
	public double measure(Quantity quantity) {
		return switch (quantity) {
			case LEVEL -> waterLevel() * 100;
			case FLOW -> outflow;
			default -> Double.NaN;
		};
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format(Locale.ROOT, "Water day tank: %.0f of %.0f L (%.1f %%), %.1f L/s out", litres, Tuning.DAY_TANK_LITRES, waterLevel() * 100, outflow));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("litres", litres);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		litres = input.getDoubleOr("litres", 0);
	}
}
