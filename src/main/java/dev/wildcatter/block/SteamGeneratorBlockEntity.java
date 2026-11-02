package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.Reactor;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A U-tube steam generator (docs/multiblocks/reactor.md), against the vessel on its hot leg, its pump against it. Feedwater comes in
 * on the east face of its bottom cells; saturated steam leaves its top face for whatever pipe draws it (8b's turbine); its atmospheric
 * dump valve, on top, vents the rest. The vessel works out its secondary side; this holds the steam it offers and pulls its feed.
 */
public class SteamGeneratorBlockEntity extends PlantUnitBlockEntity {
	private double offered;   // steam it held after its last refill, kg
	private long turbineTrip = -1000;
	private String unit = "NOT BUILT: set it against the REACTOR VESSEL";

	public SteamGeneratorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.STEAM_GENERATOR_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[1] == 11 && side == Direction.UP ? Product.STEAM : null;
	}

	@Override
	protected double hold(Product product) {
		return Reactor.steamPerGenerator() * Tuning.SG_STEAM_HOLD_S;
	}

	private List<Lines.Face> feed() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	/** Steam its pipe drew since the last refill, kg. */
	public double taken() {
		return Math.max(0, offered - held(Product.STEAM));
	}

	/** Pulls up to {@code kg} of feedwater from its feed pipe; returns what came. */
	public double pullFeed(Level level, double kg) {
		return kg <= 1e-9 ? 0 : Lines.pull(level, feed(), Product.FEEDWATER, kg, Pull.MACHINE, this);
	}

	/** The turbine on its steam pipe has tripped (turbine-island.md). */
	public void markTurbineTrip(long now) {
		turbineTrip = now;
	}

	public boolean turbineTripped(long now) {
		return now - turbineTrip < 20;
	}

	/**
	 * Tops up the steam it offers its pipe ({@code share} of a full top-up: the turbine's governor valves hold the header), or empties
	 * it when the steam generator has no pressure.
	 */
	public void refill(boolean steaming, double share, String status) {
		if (steaming) {
			add(Product.STEAM, room(Product.STEAM) * share);
		} else {
			held.put(Product.STEAM, 0.0);
		}
		offered = held(Product.STEAM);
		unit = status;
		setChanged();
	}

	public void notBuilt(String why) {
		unit = why;
	}

	@Override
	public Component inspect() {
		return Component.literal("Steam generator (U-tube): " + unit);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("offered", offered);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		offered = input.getDoubleOr("offered", 0);
	}
}
