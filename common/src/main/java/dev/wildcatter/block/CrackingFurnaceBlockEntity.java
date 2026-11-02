package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Cracker;
import dev.wildcatter.fluid.Product;
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
 * A steam cracker's furnace (docs/multiblocks/cracker.md): LPG or gasoline, with dilution steam made in its convection section from
 * fresh water, through coils in a gas-fired radiant box. Feed in on the north face of its front cells, fuel gas on the east face of
 * its bottom cells, water off a water pipe on their west face; hot cracked gas out of the south face of its back cells.
 */
public class CrackingFurnaceBlockEntity extends PlantUnitBlockEntity implements WaterUser {
	private double water;
	private double cracked;
	private double firing;

	public CrackingFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CRACKING_FURNACE_ENTITY, pos, state);
	}

	private List<Lines.Face> feed() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	private List<Lines.Face> fuel() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 5 && at[1] == 0 && side == Direction.SOUTH ? Product.HOT_CRACKED_GAS : null;
	}

	@Override
	protected double hold(Product product) {
		return Tuning.QUENCH_KGS * Tuning.PLANT_HOLD_SECONDS;
	}

	// ---- its boiler feed water, from a pump on water pipe ---------------------------------------------------------------------
	private static final double WATER_HOLD = 200.0;

	@Override
	public double waterLevel() {
		return water / WATER_HOLD;
	}

	@Override
	public int addWater(int litres) {
		int fits = (int) Math.max(0, Math.min(litres, WATER_HOLD - water));
		water += fits;
		setChanged();
		return fits;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, CrackingFurnaceBlockEntity f) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		f.cracked = 0;
		f.firing = 0;
		Product feed = null;
		for (Lines.Line line : Lines.lines(level, f.feed())) {
			if (line.run().product() != null && Cracker.of(line.run().product()) != null) {
				feed = line.run().product();
			}
		}
		if (feed == null) {
			f.status = "idle: no LPG or gasoline at its feed (the north face of its front)";
			f.setChanged();
			return;
		}
		Cracker yields = Cracker.of(feed);
		double kgPerL = Cracker.kgPerLitre(feed);
		double seconds = seconds();
		double litres = Math.min(Tuning.CRACKER_FEED_LPS * seconds, f.room(Product.HOT_CRACKED_GAS) / kgPerL);
		litres = Math.min(litres, Lines.available(level, f.feed(), feed, Pull.MACHINE, f));
		double waterPerL = (feed == Product.LPG ? Tuning.CRACKER_WATER_LPG : Tuning.CRACKER_WATER_GASOLINE) / Tuning.CRACKER_FEED_LPS;
		if (f.water < litres * waterPerL) {
			litres = f.water / waterPerL;
		}
		if (litres < 0.001) {
			f.status = f.water < 1 ? "NO WATER: it makes its dilution steam from water: run water pipe from a pump to its west side"
					: "idle: its cracked gas has nowhere to go (pipe its back end to a quench tower)";
			f.setChanged();
			return;
		}
		double watts = feed == Product.LPG ? Tuning.CRACKER_FUEL_WATTS_LPG : Tuning.CRACKER_FUEL_WATTS_GASOLINE;
		double need = watts * litres / Tuning.CRACKER_FEED_LPS;
		double got = 0;
		List<Lines.Line> fuels = new java.util.ArrayList<>(Lines.lines(level, f.fuel()));
		fuels.sort(java.util.Comparator.comparing(l -> l.run().product() == Product.LPG));   // its own fuel gas first, LPG to make up
		for (Lines.Line line : fuels) {
			Product p = line.run().product();
			double per = p == Product.PIPELINE_GAS || p == Product.GAS || p == Product.DRIED_GAS || p == Product.COMPRESSED_GAS ? Tuning.GAS_JOULES_PER_M3
					: p == Product.LPG ? Tuning.LPG_JOULES_PER_L : 0;   // LPG fires it until its own fuel gas comes round
			if (per > 0 && got < need) {
				got += Lines.pull(line, p, (need - got) / per, Pull.MACHINE, f) * per;
			}
		}
		double fired = need > 0 ? Math.min(1, got / need) : 0;
		litres *= fired;
		double taken = litres > 0.001 ? Lines.pull(level, f.feed(), feed, litres, Pull.MACHINE, f) : 0;
		f.water = Math.max(0, f.water - taken * waterPerL);
		f.cracked = taken * kgPerL;
		f.addMixed(Product.HOT_CRACKED_GAS, f.cracked, new double[] {yields.ethylene(), yields.fuelGas(), yields.lpg(), yields.pyGas(), yields.residue()});
		f.firing = got / seconds;
		f.status = taken > 0 ? String.format("cracking %.2f L/s of %s (%.1f MW fired, %.2f L/s of water)%s", taken / seconds, feed.label(), f.firing / 1e6,
				taken * waterPerL / seconds, fired < 0.99 ? "; SHORT OF FUEL GAS (east side)" : "")
				: "NO FUEL: pipe fuel gas (pipeline gas, the cold box's fuel gas) or LPG to its east side";
		f.setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("Cracking furnace: " + status + String.format("; water %.0f/%.0f L", water, WATER_HOLD));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("water", water);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		water = input.getDoubleOr("water", 0);
	}
}
