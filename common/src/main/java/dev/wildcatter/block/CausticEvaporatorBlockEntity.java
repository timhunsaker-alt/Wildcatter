package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.ChlorAlkaliPlant;
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
 * The caustic evaporator (docs/multiblocks/chlor-alkali-plant.md): a double-effect falling-film evaporator taking the cell room's 32 %
 * caustic to 50 % on process steam, about 0.7 t of steam a tonne of NaOH. 32 % in on the north face of its bottom cells, steam on the
 * east; out of the south face. With no steam, the 32 % passes through to its outlet. Its spec gives it no motor.
 */
public class CausticEvaporatorBlockEntity extends ProcessPartBlockEntity {
	private double making;   // kg/h of 50 % caustic, the last interval
	private double total;

	public CausticEvaporatorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CAUSTIC_EVAPORATOR_ENTITY, pos, state);
	}

	private List<Lines.Face> causticIn() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	private List<Lines.Face> steamIn() {
		return faces(at -> at[0] == 2 && at[1] == 0, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		if (at[2] != 2 || at[1] != 0 || side != Direction.SOUTH) {
			return null;
		}
		return held(Product.CAUSTIC_SODA) > 1e-6 && held(Product.CAUSTIC_SODA_50) <= 1e-6 ? Product.CAUSTIC_SODA : Product.CAUSTIC_SODA_50;
	}

	@Override
	protected double hold(Product product) {
		double rate = ChlorAlkaliPlant.cl2Kgs() * ChlorAlkaliPlant.caustic32PerCl2() * Tuning.PLANT_HOLD_SECONDS * 4;
		return product == Product.CAUSTIC_SODA || product == Product.CAUSTIC_SODA_50 ? rate : 0;
	}

	/** kg/h of 50 % caustic over the last interval: about 2,250 at the plant's rating. */
	public double making() {
		return making;
	}

	@Override
	protected void work(Level level) {
		making = 0;
		double in = Lines.available(level, causticIn(), Product.CAUSTIC_SODA, Pull.MACHINE, this);
		if (in < 1e-6) {
			status = "idle: no 32 % caustic at its north side";
			return;
		}
		double steam = Lines.available(level, steamIn(), Product.PROCESS_STEAM, Pull.MACHINE, this);
		double max = ChlorAlkaliPlant.cl2Kgs() * ChlorAlkaliPlant.caustic32PerCl2() * seconds() * 1.2;
		if (steam < 1e-6 || held(Product.CAUSTIC_SODA) > 1e-6) {
			// off: the 32 % passes through (once the 50 % in its outlet is drawn away, the outlet carries 32 %)
			if (held(Product.CAUSTIC_SODA_50) > 1e-6 && steam < 1e-6) {
				status = "NO STEAM: its 50 % still to be drawn from its outlet before the 32 % passes through";
				return;
			}
			if (steam < 1e-6) {
				double kg = Lines.pull(level, causticIn(), Product.CAUSTIC_SODA, Math.min(max, room(Product.CAUSTIC_SODA)), Pull.MACHINE, this);
				add(Product.CAUSTIC_SODA, kg);
				status = String.format("NO STEAM: passing %,.0f kg/h of 32 %% caustic through (pipe process steam to its east side)", kg / seconds() * 3600);
				return;
			}
			status = "starting: its 32 % still to be drawn from its outlet";
			return;
		}
		double kg = Math.min(Math.min(max, in), steam / ChlorAlkaliPlant.steamPer32());
		kg = Math.min(kg, room(Product.CAUSTIC_SODA_50) / ChlorAlkaliPlant.caustic50Per32());
		if (kg <= 1e-7) {
			status = "FULL: its 50 % caustic has nowhere to go (pipe its south side to a tank or a user)";
			return;
		}
		kg = Lines.pull(level, causticIn(), Product.CAUSTIC_SODA, kg, Pull.MACHINE, this);
		Lines.pull(level, steamIn(), Product.PROCESS_STEAM, kg * ChlorAlkaliPlant.steamPer32(), Pull.MACHINE, this);
		double out = kg * ChlorAlkaliPlant.caustic50Per32();
		add(Product.CAUSTIC_SODA_50, out);
		total += out;
		making = out / seconds() * 3600;
		status = String.format("concentrating %,.0f kg/h of 32 %% caustic to %,.0f kg/h of 50 %% on %,.0f kg/h of steam", kg / seconds() * 3600, making,
				kg * ChlorAlkaliPlant.steamPer32() / seconds() * 3600);
	}

	@Override
	public Component inspect() {
		return Component.literal("Caustic evaporator (double-effect falling film): " + status + String.format("; 50 %% held %.0f kg, 32 %% held %.0f kg; %,.0f kg of 50 %% made",
				held(Product.CAUSTIC_SODA_50), held(Product.CAUSTIC_SODA), total));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("total", total);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		total = input.getDoubleOr("total", 0);
	}
}
