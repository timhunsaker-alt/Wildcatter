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

	public static void tick(Level level, BlockPos pos, BlockState state, HfColumnBlockEntity c) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		HfKilnBlockEntity kiln = Touching.find(level, pos, HfKilnBlockEntity.class, k -> true);
		if (kiln == null) {
			c.status = "NOT BUILT: set it against an HF KILN";
		} else {
			double kg = kiln.takeHf(Math.min(Tuning.HF_KILN_KGS * 1.2 * seconds(), c.room(Product.ANHYDROUS_HF) * Tuning.AHF_KG_PER_L));
			c.add(Product.ANHYDROUS_HF, kg / Tuning.AHF_KG_PER_L);
			c.status = kg > 1e-7 ? String.format("condensing %.3f kg/s of anhydrous HF", kg / seconds()) : "idle: no HF from the kiln, or nowhere for it to go";
		}
		c.setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("HF column: " + status + String.format("; %.0f L of anhydrous HF held", held(Product.ANHYDROUS_HF)));
	}
}
