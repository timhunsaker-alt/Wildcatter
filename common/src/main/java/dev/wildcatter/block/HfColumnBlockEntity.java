package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The HF column against the kiln (docs/multiblocks/conversion.md): it scrubs, condenses and purifies the kiln's gas to anhydrous HF,
 * 99.98 %, out of the south face of its bottom cells.
 */
public class HfColumnBlockEntity extends PlantUnitBlockEntity {
	public HfColumnBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.HF_COLUMN_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 1 && at[1] == 0 && side == Direction.SOUTH ? Product.ANHYDROUS_HF : null;
	}

	@Override
	protected double hold(Product product) {
		return Tuning.HF_KILN_KGS / Tuning.AHF_KG_PER_L * Tuning.PLANT_HOLD_SECONDS * 4;
	}

	/** For the game test: anhydrous HF as if condensed. */
	public void addHf(double litres) {
		add(Product.ANHYDROUS_HF, litres);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, HfColumnBlockEntity c) {
		if (c.plcHold(level)) {
			return;
		}
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		HfKilnBlockEntity kiln = Touching.find(level, pos, HfKilnBlockEntity.class, k -> true);
		if (kiln == null) {
			c.status = "NOT BUILT: set it against an HF KILN";
		} else {
			c.recover(level, kiln);   // 19c2: aqueous HF from the fuel plant first: the kiln then roasts that much less fluorspar
			double kg = kiln.takeHf(Math.min(Tuning.HF_KILN_KGS * 1.2 * seconds(), c.room(Product.ANHYDROUS_HF) * Tuning.AHF_KG_PER_L));
			c.add(Product.ANHYDROUS_HF, kg / Tuning.AHF_KG_PER_L);
			c.status = (kg > 1e-7 ? String.format("condensing %.3f kg/s of anhydrous HF", kg / seconds()) : "idle: no HF from the kiln, or nowhere for it to go") + c.recovering;
		}
		c.setChanged();
	}

	private double recovered;   // kg of HF from aqueous acid, all told
	private String recovering = "";

	public double recovered() {
		return recovered;
	}

	/** Its north face: 70 % aqueous HF from a fuel plant's recovery condenser, distilled over the kiln's sulfuric acid (0.5 kg a kg of HF). */
	private void recover(Level level, HfKilnBlockEntity kiln) {
		recovering = "";
		java.util.List<Lines.Face> north = faces(at -> at[1] == 0 && at[2] == 0, Direction.NORTH);
		double waiting = Lines.available(level, north, Product.AQUEOUS_HF, Pull.MACHINE, this);
		if (waiting <= 1e-9) {
			return;
		}
		double litresPerHf = Tuning.HF_RECOVERY_ACID_PER_HF / Tuning.ACID_STRENGTH / Tuning.ACID_KG_PER_L;
		double hf = Math.min(Math.min(waiting * Tuning.AQUEOUS_HF_STRENGTH, room(Product.ANHYDROUS_HF) * Tuning.AHF_KG_PER_L), Tuning.HF_KILN_KGS * seconds());
		hf = Math.min(hf, kiln.acidAvailable(level) / litresPerHf);
		if (hf <= 1e-9) {
			recovering = "; aqueous HF waiting: NO ACID on the kiln's acid pipe, or no room";
			return;
		}
		double aq = Lines.pull(level, north, Product.AQUEOUS_HF, hf / Tuning.AQUEOUS_HF_STRENGTH, Pull.MACHINE, this);
		hf = aq * Tuning.AQUEOUS_HF_STRENGTH;
		kiln.takeAcid(level, hf * litresPerHf);
		add(Product.ANHYDROUS_HF, hf / Tuning.AHF_KG_PER_L);
		recovered += hf;
		recovering = String.format("; %.1f kg/h of HF recovered from the fuel plant's aqueous acid", hf / seconds() * 3600);
	}

	@Override
	public Component inspect() {
		return Component.literal("HF column: " + status + String.format("; %.0f L of anhydrous HF held; %.1f kg recovered from aqueous HF", held(Product.ANHYDROUS_HF), recovered));
	}

	@Override
	protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("recovered", recovered);
	}

	@Override
	protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
		super.loadAdditional(input);
		recovered = input.getDoubleOr("recovered", 0);
	}
}
