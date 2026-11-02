package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The deaerator on its storage tank, with the LP heaters (docs/multiblocks/turbine-island.md): it holds the feed train's water,
 * 30 t, filled at the start, and takes make-up water off a pump. The HP turbine moves the water through it.
 */
public class DeaeratorBlockEntity extends PlantUnitBlockEntity implements WaterUser {
	private static final double MAKEUP_HOLD = 500.0;
	private double stock = Tuning.DEAERATOR_KG;
	private double makeup;

	public DeaeratorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.DEAERATOR_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	@Override
	public double waterLevel() {
		return makeup / MAKEUP_HOLD;
	}

	@Override
	public int addWater(int litres) {
		int fits = (int) Math.max(0, Math.min(litres, MAKEUP_HOLD - makeup));
		makeup += fits;
		setChanged();
		return fits;
	}

	public double stock() {
		return stock;
	}

	public double room() {
		return Math.max(0, Tuning.DEAERATOR_KG - stock);
	}

	/** Adds condensate (or make-up); returns what fitted. */
	public double fill(double kg) {
		double fits = Math.max(0, Math.min(kg, room()));
		stock += fits;
		setChanged();
		return fits;
	}

	public double draw(double kg) {
		double got = Math.max(0, Math.min(kg, stock));
		stock -= got;
		setChanged();
		return got;
	}

	/** Tops it up from its make-up water, up to {@code kg}. */
	public void makeUp(double kg) {
		double got = Math.min(Math.min(kg, makeup), room());
		makeup -= got;
		stock += got;
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Deaerator and LP heaters: storage %.1f of %.0f t (%.0f %%); make-up water %.0f L", stock / 1000,
				Tuning.DEAERATOR_KG / 1000, stock / Tuning.DEAERATOR_KG * 100, makeup));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("stock", stock);
		output.putDouble("makeup", makeup);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		stock = input.getDoubleOr("stock", Tuning.DEAERATOR_KG);
		makeup = input.getDoubleOr("makeup", 0);
	}
}
