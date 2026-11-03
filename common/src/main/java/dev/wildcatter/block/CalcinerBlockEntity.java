package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The gas-suspension calciner (docs/multiblocks/bayer-refinery.md): 2 Al(OH)3 -> Al2O3 + 3 H2O at about 1,000 C on 3.0 MJ a kg of
 * alumina, gas or LPG at the east face of its bottom cells; ID fan 250 kW at 4.16 kV. Hydrate from the hydrate filter it touches,
 * alumina to the bagging station it touches.
 */
public class CalcinerBlockEntity extends UnitMotorBlockEntity implements Bagged {
	private double alumina;
	private double made;   // t
	private String unit = "starting";

	public CalcinerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CALCINER_ENTITY, pos, state);
	}

	private List<Lines.Face> east() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	@Override
	public Item bagItem() {
		return WildcatterItems.ALUMINA_BAG;
	}

	@Override
	public double bagReady() {
		return alumina;
	}

	@Override
	public double takeForBag(double kg) {
		double got = Math.min(kg, alumina);
		alumina -= got;
		setChanged();
		return got;
	}

	/** For the game test: alumina as if calcined. */
	public void addAlumina(double kg) {
		alumina += kg;
		setChanged();
	}

	@Override
	protected void work(Level level) {
		HydrateFilterBlockEntity filter = Touching.find(level, worldPosition, HydrateFilterBlockEntity.class, f -> true);
		if (filter == null) {
			unit = "NOT BUILT: set it against the hydrate filter";
			return;
		}
		double hydrate = filter.hydrate();
		if (hydrate < 0.001) {
			unit = "idle: no hydrate from the filter";
			return;
		}
		double max = Math.min(Math.min(Tuning.BAYER_ALUMINA_KGS * Tuning.BAYER_CATCH_UP * seconds(), hydrate * Tuning.AL2O3_IN_GIBBSITE),
				2 * Tuning.BULK_BAG_KG - alumina);
		if (max <= 1e-7) {
			unit = "FULL: set a bagging station against it";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double a = max * strength;
		double need = a * Tuning.CALCINER_J_PER_KG;
		double got = GasFiring.fire(level, east(), need, this);
		a *= Math.min(1, got / need);
		if (a <= 1e-7) {
			unit = "NO FUEL: pipe gas or LPG to its east side";
			return;
		}
		filter.takeHydrate(a / Tuning.AL2O3_IN_GIBBSITE);
		alumina += a;
		made += a / 1000;
		unit = String.format("calcining at 1,000 C: %.2f kg/s of alumina, %.1f MW of gas", a / seconds(), got / seconds() / 1e6);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Calciner (gas suspension): %s; %.0f kg of alumina held, %.1f t made; ", unit, alumina, made) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("alumina", alumina);
		output.putDouble("made", made);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		alumina = input.getDoubleOr("alumina", 0);
		made = input.getDoubleOr("made", 0);
	}
}
