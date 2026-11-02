package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.AcidPlant;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The converter (docs/multiblocks/acid-plant.md): four beds of vanadium catalyst that turn SO2 to SO3. Burner gas in on the north face
 * of its front cells. With an interpass absorber touching it, it converts 99.8 %; without, 95 %. The final absorber touching it makes
 * the acid; what is not converted goes up the stack.
 */
public class ConverterBlockEntity extends PlantUnitBlockEntity {
	private double stackSo2;   // kg/h
	private boolean interpass;

	public ConverterBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CONVERTER_ENTITY, pos, state);
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

	/** kg of SO2 a hour up the stack, the last interval. */
	public double stackSo2() {
		return stackSo2;
	}

	public boolean interpass() {
		return interpass;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ConverterBlockEntity c) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		c.stackSo2 = 0;
		FinalAbsorberBlockEntity absorber = Touching.find(level, pos, FinalAbsorberBlockEntity.class, a -> true);
		c.interpass = Touching.find(level, pos, PassivePartBlockEntity.class, p -> p.kind().equals("interpass_absorber")) != null;
		double waiting = Lines.available(level, c.inlet(), Product.BURNER_GAS, Pull.MACHINE, c);
		if (absorber == null) {
			c.status = "NO ABSORBER: set a FINAL ABSORBER against the converter";
		} else if (absorber.dry()) {
			c.status = "ABSORBER DRY: pump water to the final absorber";
		} else if (waiting < 0.001) {
			c.status = "idle: no burner gas at its front (north face)";
		} else {
			double conversion = AcidPlant.conversion(c.interpass);
			double productPerGas = AcidPlant.so2InGas() * conversion * AcidPlant.acidPerSo2() * AcidPlant.productPerAcid();
			double max = Math.min(AcidPlant.ratedSulfurKgs() * AcidPlant.gasPerSulfur() * seconds() * 1.2, absorber.canMake() / productPerGas);
			double gas = max > 0.001 ? Lines.pull(level, c.inlet(), Product.BURNER_GAS, Math.min(max, waiting), Pull.MACHINE, c) : 0;
			absorber.absorb(gas * productPerGas);
			c.stackSo2 = gas * AcidPlant.so2InGas() * (1 - conversion) / seconds() * 3600;
			double acid = gas * productPerGas / seconds();
			c.status = gas > 0 ? String.format("%s: %.1f %% converted, %.2f kg/s of acid (%.0f t/day), %.1f kg/h of SO2 up the stack",
					c.interpass ? "double absorption" : "SINGLE ABSORPTION (no interpass absorber against it)", conversion * 100, acid,
					acid * 86.4, c.stackSo2)
					: "idle: the final absorber's acid has nowhere to go (pipe its south face to a storage tank)";
		}
		c.setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("Converter (four V2O5 beds): " + status);
	}
}
