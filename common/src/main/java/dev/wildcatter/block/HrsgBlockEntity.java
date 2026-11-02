package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.multiblock.BigPartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A single-pressure unfired HRSG (docs/multiblocks/combined-cycle.md) against a gas turbine's exhaust end: it takes about 65 % of the
 * exhaust's heat above stack temperature and raises steam at 40 bar and 400 C for the steam turbine touching it. Its steam follows
 * the exhaust over a couple of minutes as its tubes and drum warm.
 */
public class HrsgBlockEntity extends BlockEntity implements Inspectable, BigPartEntity {
	/** Heat going into steam, watts. */
	private double heat;
	private boolean grown;

	public HrsgBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.HRSG_ENTITY, pos, state);
	}

	/** What its steam can make in a steam turbine, watts. */
	public double steamWatts() {
		return heat * Tuning.STEAM_CYCLE_EFFICIENCY;
	}

	/** The steam turbine's generator, through the steam turbine touching it. */
	public @Nullable GeneratorPartBlockEntity steamGenerator(Level level) {
		SteamCycleBlockEntity st = Touching.find(level, worldPosition, SteamCycleBlockEntity.class, s -> true);
		return st == null ? null : st.generator(level);
	}

	/** Game test hook: warm through at once (two minutes in play). */
	public void warmForTest(Level level) {
		GasTurbineBlockEntity gt = Touching.find(level, worldPosition, GasTurbineBlockEntity.class, g -> true);
		heat = gt == null ? 0 : gt.exhaustWatts() * Tuning.HRSG_RECOVERY;
		setChanged();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, HrsgBlockEntity h) {
		GasTurbineBlockEntity gt = Touching.find(level, pos, GasTurbineBlockEntity.class, g -> true);
		double target = gt == null ? 0 : gt.exhaustWatts() * Tuning.HRSG_RECOVERY;
		h.heat += (target - h.heat) * (1.0 / 20.0) / Tuning.HRSG_WARMUP_SECONDS;
		if (h.heat < 1000) {
			h.heat = target > h.heat ? h.heat : 0;
		}
		h.setChanged();
	}

	@Override
	public Component inspect() {
		double kgPerSecond = heat / 3.2e6;   // about 3.2 MJ a kg from feedwater to 40 bar and 400 C
		return Component.literal(String.format("HRSG (single pressure, unfired): taking %.1f MW from the exhaust, %.1f kg/s of steam at 40 bar, 400 C, good for %.1f MW",
				heat / 1e6, kgPerSecond, steamWatts() / 1e6));
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
		output.putDouble("heat", heat);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", grown);
		heat = input.getDoubleOr("heat", 0);
	}
}
