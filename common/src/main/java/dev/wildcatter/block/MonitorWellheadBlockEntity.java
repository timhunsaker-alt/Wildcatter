package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.fluid.Product;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 16d2: a monitor well (MonitorWellheadBlock). On a header house's well pipe it is a port that offers nothing; the header house reads it
 * (its distance from the patterns, whether the front has reached it) and runs its sampling pump off its drives.
 */
public class MonitorWellheadBlockEntity extends BlockEntity implements FluidNode, Inspectable {
	private String reading = "NOT ON A WELLFIELD: run its line to a header house's well pipe";
	private long readAt = -1000;

	public MonitorWellheadBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.MONITOR_WELLHEAD_ENTITY, pos, state);
	}

	/** The header house's sample: what it reads now. */
	public void read(long now, String what) {
		readAt = now;
		reading = what;
	}

	@Override
	public @Nullable Product offers(BlockPos cell, Direction side) {
		return null;
	}

	@Override
	public double available(BlockPos cell, Direction side, Product product, Pull pull) {
		return 0;
	}

	@Override
	public double take(BlockPos cell, Direction side, Product product, double max, Pull pull) {
		return 0;
	}

	@Override
	public Component inspect() {
		boolean fresh = level != null && level.getGameTime() - readAt < 100;
		return Component.literal("Monitor well (sampling pump 1.5 kW): " + (fresh ? reading : "NOT ON A WELLFIELD: run its line to a header house's well pipe"));
	}
}
