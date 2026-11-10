package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
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
 * The CO2 storage tank (docs/multiblocks/isr-operations.md, 16d1): a 30 t insulated liquid CO2 tank with its vaporiser, the only tank
 * that holds CO2. It fills from the pipe at the north face of its front cells (a CO2 liquefier's line) and doses the ISR header house it
 * stands against, which takes it by touching.
 */
public class Co2StorageTankBlockEntity extends PlantUnitBlockEntity {
	private double co2;

	public Co2StorageTankBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CO2_STORAGE_TANK_ENTITY, pos, state);
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	public double co2() {
		return co2;
	}

	/** The header house's vaporiser draw: up to {@code kg}; returns what it got. */
	public double take(double kg) {
		double got = Math.min(kg, co2);
		co2 -= got;
		setChanged();
		return got;
	}

	/** For the game test: liquid CO2 put straight in. */
	public void testFill(double kg) {
		co2 = Math.min(Tuning.CO2_TANK_KG, co2 + kg);
		setChanged();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, Co2StorageTankBlockEntity t) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		double room = Tuning.CO2_TANK_KG - t.co2;
		if (room > 1e-6) {
			t.co2 += Lines.pull(level, t.inlet(), Product.CO2, room, Pull.MACHINE, t);
		}
		t.status = t.co2 < 1e-3 ? "EMPTY: pipe a CO2 liquefier to the north face of its front" : String.format("%,.0f of %,.0f kg of liquid CO2 (%.0f %%)", t.co2, Tuning.CO2_TANK_KG,
				t.co2 / Tuning.CO2_TANK_KG * 100);
		t.setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("CO2 storage tank (30 t, insulated, with vaporiser): " + status);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("co2", co2);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		co2 = input.getDoubleOr("co2", 0);
	}
}
