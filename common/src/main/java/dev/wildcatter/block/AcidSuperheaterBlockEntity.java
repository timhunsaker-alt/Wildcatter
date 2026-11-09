package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.AcidSteam;
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
 * The acid plant's steam superheater (docs/multiblocks/acid-plant-steam.md), in the gas between the converter's first and second beds
 * as real DCDA plants have it. It stands against the converter, takes the waste heat boiler's saturated 40 bar steam on a steam pipe at
 * the west face of its bottom cells, and brings it to about 400 C with the converter's heat (about 0.83 MW at 2 kg/s). The 5 MW steam
 * turbine touching it takes the steam. With the converter stopped it has no heat and passes saturated steam. No power.
 */
public class AcidSuperheaterBlockEntity extends PlantUnitBlockEntity {
	private double steamKgs;   // kg/s through it, the last interval
	private boolean hot;
	private @Nullable BlockPos boiler;

	public AcidSuperheaterBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.ACID_SUPERHEATER_ENTITY, pos, state);
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[1] == 0, Direction.WEST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	/** kg/s of steam through it, the last interval. */
	public double steamKgs() {
		return steamKgs;
	}

	/** True while the converter's gas heats it: its steam is at 400 C, not saturated. */
	public boolean superheated() {
		return hot;
	}

	/** The waste heat boiler its steam came from, the last interval: the feed pump returns the condensate to it. */
	public @Nullable WasteHeatBoilerBlockEntity boiler(Level level) {
		return boiler != null && level.getBlockEntity(boiler) instanceof WasteHeatBoilerBlockEntity b ? b : null;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, AcidSuperheaterBlockEntity s) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		ConverterBlockEntity converter = Touching.find(level, pos, ConverterBlockEntity.class, c -> true);
		s.hot = converter != null && converter.converting(level);
		double max = AcidSteam.boilerKgs() * 1.5 * seconds();
		double got = 0;
		BlockPos from = null;
		for (Lines.Line line : Lines.lines(level, s.inlet())) {
			for (Lines.Port port : Lines.sources(line, Product.STEAM, Pull.MACHINE, s)) {
				if (got >= max) {
					break;
				}
				double took = port.node().take(port.cell(), port.side(), Product.STEAM, (max - got) * line.opening(), Pull.MACHINE);
				if (took > 0 && port.node() instanceof WasteHeatBoilerBlockEntity b) {
					from = b.getBlockPos();
				}
				got += took;
			}
		}
		s.boiler = from != null ? from : got > 0 ? null : s.boiler;
		s.steamKgs = got / seconds();
		if (converter == null) {
			s.status = "NO HEAT: set it against the CONVERTER";
		} else if (got <= 0) {
			s.status = "NO STEAM: pipe the waste heat boiler's top to the west side of its bottom";
		} else if (!s.hot) {
			s.status = String.format("passing %.2f kg/s of SATURATED steam: the converter is not running, so it has no heat", s.steamKgs);
		} else {
			s.status = String.format("superheating %.2f kg/s of 40 bar steam to 400 C, taking %.2f MW from the converter's gas", s.steamKgs,
					AcidSteam.superheatWatts(s.steamKgs) / 1e6);
		}
		s.setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("Acid plant superheater: " + status);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("steamKgs", steamKgs);
		output.putBoolean("hot", hot);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		steamKgs = input.getDoubleOr("steamKgs", 0);
		hot = input.getBooleanOr("hot", false);
	}
}
