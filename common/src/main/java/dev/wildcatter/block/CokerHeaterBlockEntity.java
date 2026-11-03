package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The coker heater (docs/multiblocks/delayed-coker.md): a two-pass charge heater taking the fractionator's bottoms to 495 C on
 * 1.1 MJ a kg of gas, coker gas or LPG at the east face of its bottom cells; charge pump 75 kW at 480 V. It must touch the
 * fractionator and the coke drums. The residue cracks in the drum it is filling: the coke stays, the vapour goes back to the
 * fractionator. The filling drum switches once it holds a tonne and the other is cut out.
 */
public class CokerHeaterBlockEntity extends UnitMotorBlockEntity {
	private long fillingAt = Long.MIN_VALUE;   // the filling drum's main cell
	private double cracked;   // t of residue
	private String unit = "starting";

	public CokerHeaterBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.COKER_HEATER_ENTITY, pos, state);
	}

	private List<Lines.Face> east() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	private List<CokeDrumBlockEntity> drums(Level level) {
		List<CokeDrumBlockEntity> out = new ArrayList<>();
		for (BlockEntity e : Touching.parts(level, worldPosition)) {
			if (e instanceof CokeDrumBlockEntity drum) {
				out.add(drum);
			}
		}
		return out;
	}

	/** The drum to fill now, switching once it holds a tonne and the other is empty. */
	private CokeDrumBlockEntity filling(List<CokeDrumBlockEntity> drums) {
		CokeDrumBlockEntity now = drums.get(0);
		for (CokeDrumBlockEntity d : drums) {
			if (d.getBlockPos().asLong() == fillingAt) {
				now = d;
			}
		}
		for (CokeDrumBlockEntity d : drums) {
			if (d != now && d.coke() < 0.5 && now.coke() >= Tuning.COKE_DRUM_SWITCH_KG) {
				now = d;
				break;
			}
		}
		fillingAt = now.getBlockPos().asLong();
		for (CokeDrumBlockEntity d : drums) {
			d.setFilling(d == now);
		}
		return now;
	}

	@Override
	protected void work(Level level) {
		CokerFractionatorBlockEntity column = Touching.find(level, worldPosition, CokerFractionatorBlockEntity.class, c -> true);
		if (column == null) {
			unit = "NOT BUILT: set it against the coker fractionator";
			return;
		}
		List<CokeDrumBlockEntity> drums = drums(level);
		if (drums.isEmpty()) {
			unit = "NOT BUILT: set it against the coke drums";
			return;
		}
		CokeDrumBlockEntity drum = filling(drums);
		if (column.bottoms() < 0.01) {
			unit = "idle: no residue in the fractionator's bottoms";
			return;
		}
		double max = Math.min(Math.min(Tuning.COKER_RESIDUE_KGS * Tuning.CARBON_CATCH_UP * seconds(), column.bottoms()),
				Math.min(column.room(), drum.room() / Tuning.COKER_COKE_YIELD));
		if (drum.room() < 1) {
			unit = drums.size() < 2 ? "ONE DRUM: it is full; set a second drum on the pit against the heater" : "FULL: the drums are full (is the pit cutting?)";
			return;
		}
		if (max <= 1e-7) {
			unit = "idle: the fractionator's products have nowhere to go";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double kg = max * strength;
		double need = kg * Tuning.COKER_HEATER_J_PER_KG;
		double got = GasFiring.fire(level, east(), need, this);
		kg *= Math.min(1, got / need);
		if (kg <= 1e-7) {
			unit = "NO FUEL: pipe gas, coker gas or LPG to its east side";
			return;
		}
		column.takeFeed(kg);
		for (Product p : new Product[] {Product.DIESEL, Product.GASOLINE, Product.COKER_GAS}) {
			column.give(p, kg * CokerFractionatorBlockEntity.perResidue(p));
		}
		drum.addCoke(kg * Tuning.COKER_COKE_YIELD);
		cracked += kg / 1000;
		StringBuilder each = new StringBuilder();
		for (int i = 0; i < drums.size(); i++) {
			each.append(i == 0 ? "" : ", ").append(String.format("%s %,.0f kg", drums.get(i) == drum ? "filling" : "cutting", drums.get(i).coke()));
		}
		unit = String.format("coking at 495 C: %.2f kg/s of residue, %.1f MW of fuel; drums: %s", kg / seconds(), got / seconds() / 1e6, each);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Coker heater: %s; %.1f t of residue coked; ", unit, cracked) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong("fillingAt", fillingAt);
		output.putDouble("cracked", cracked);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		fillingAt = input.getLongOr("fillingAt", Long.MIN_VALUE);
		cracked = input.getDoubleOr("cracked", 0);
	}
}
