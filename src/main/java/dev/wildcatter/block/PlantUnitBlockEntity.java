package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.multiblock.BigPartEntity;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A process part of the gas plant (docs/multiblocks/gas-plant.md): it pulls from its inlet run, makes its products and holds a few
 * seconds of each at its outlet nozzles for whatever draws on them. Its nozzles are written in the part's own frame (drawn facing
 * north) and turned with it.
 */
public abstract class PlantUnitBlockEntity extends BlockEntity implements Inspectable, BigPartEntity, FluidNode {
	protected final Map<Product, Double> held = new EnumMap<>(Product.class);
	protected String status = "starting";
	private boolean grown;

	protected PlantUnitBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** The product a nozzle gives, for a cell at {@code at} in the part's own frame and a side in that frame; null if none. */
	protected abstract @Nullable Product nozzle(int[] at, Direction localSide);

	/** How much of {@code product} it holds at most: a few seconds of its rated flow. */
	protected abstract double hold(Product product);

	/** The inlet faces: in the part's own frame, the cells {@code where} and their {@code side}. */
	protected List<Lines.Face> faces(java.util.function.Predicate<int[]> where, Direction side) {
		return LeasePartBlock.faces(worldPosition, getBlockState(), where, side);
	}

	protected double held(Product product) {
		return held.getOrDefault(product, 0.0);
	}

	protected double room(Product product) {
		return Math.max(0, hold(product) - held(product));
	}

	protected void add(Product product, double amount) {
		held.put(product, Math.min(hold(product), held(product) + amount));
		setChanged();
	}

	/** The side {@code side} (in the world) seen in the part's own frame. */
	private Direction local(Direction side) {
		Direction out = side;
		for (int i = 0; side.getAxis().isHorizontal() && i < dev.wildcatter.multiblock.BigParts.turns(getBlockState()); i++) {
			out = out.getCounterClockWise();
		}
		return out;
	}

	@Override
	public @Nullable Product offers(BlockPos cell, Direction side) {
		return nozzle(LeasePartBlock.local(worldPosition, getBlockState(), cell), local(side));
	}

	/** What a puller may have: a flare takes only a gas held above three quarters full (its back-pressure valve opens), never a liquid. */
	@Override
	public double available(BlockPos cell, Direction side, Product product, Pull pull) {
		if (offers(cell, side) != product) {
			return 0;
		}
		if (pull == Pull.FLARE) {
			return product.gas() ? Math.max(0, held(product) - hold(product) * Tuning.SEPARATOR_FLARE_ABOVE) : 0;
		}
		return held(product);
	}

	@Override
	public double take(BlockPos cell, Direction side, Product product, double max, Pull pull) {
		double got = Math.min(max, available(cell, side, product, pull));
		if (got > 0) {
			held.put(product, held(product) - got);
			setChanged();
		}
		return got;
	}

	/** Seconds per interval of a process part's work. */
	protected static double seconds() {
		return Tuning.WELL_INTERVAL / 20.0;
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
		for (Map.Entry<Product, Double> e : held.entrySet()) {
			output.putDouble("held_" + e.getKey().name(), e.getValue());
		}
		output.putBoolean("grown", grown);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		for (Product p : Product.values()) {
			double v = input.getDoubleOr("held_" + p.name(), 0);
			if (v > 0) {
				held.put(p, v);
			}
		}
		grown = input.getBooleanOr("grown", grown);
	}
}
