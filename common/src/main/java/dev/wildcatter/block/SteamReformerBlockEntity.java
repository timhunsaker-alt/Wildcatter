package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Conversion;
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
 * A packaged steam methane reformer with its shift and PSA (docs/multiblocks/conversion.md): 30 Nm3/h of hydrogen from 0.45 m3 of
 * gas a m3, feed and fuel together (or LPG at the same heat), on the east face of its bottom cells; its steam from water off a pump.
 * Hydrogen out of the south face of its back cells.
 */
public class SteamReformerBlockEntity extends PlantUnitBlockEntity implements WaterUser {
	private double water;

	public SteamReformerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.STEAM_REFORMER_ENTITY, pos, state);
	}

	private List<Lines.Face> fuel() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 3 && at[1] == 0 && side == Direction.SOUTH ? Product.HYDROGEN : null;
	}

	@Override
	protected double hold(Product product) {
		return Tuning.SMR_M3H / 3600 * Tuning.PLANT_HOLD_SECONDS * 4;
	}

	@Override
	public double waterLevel() {
		return water / 200.0;
	}

	@Override
	public int addWater(int litres) {
		int fits = (int) Math.max(0, Math.min(litres, 200.0 - water));
		water += fits;
		setChanged();
		return fits;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, SteamReformerBlockEntity r) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		double m3 = Math.min(Tuning.SMR_M3H / 3600 * seconds(), r.room(Product.HYDROGEN));
		m3 = Math.min(m3, r.water / Tuning.SMR_WATER_PER_H2);
		if (r.water < 0.001) {
			r.status = "NO WATER: pump water to it for its steam";
		} else if (m3 <= 1e-7) {
			r.status = "idle: its hydrogen has nowhere to go (pipe its back end to a reduction reactor)";
		} else {
			double need = m3 * Conversion.reformerJoulesPerM3();
			double got = GasFiring.fire(level, r.fuel(), need, r);
			m3 *= Math.min(1, got / need);
			r.water = Math.max(0, r.water - m3 * Tuning.SMR_WATER_PER_H2);
			r.add(Product.HYDROGEN, m3);
			r.status = m3 > 1e-7 ? String.format("reforming: %.1f Nm3/h of hydrogen on %.0f kW of fuel", m3 / seconds() * 3600, got / seconds() / 1000)
					: "NO FUEL: pipe gas or LPG to its east side";
		}
		r.setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("Steam reformer (30 Nm3/h): " + status + String.format("; water %.0f/200 L", water));
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
