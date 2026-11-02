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
 * The sulfur furnace (docs/multiblocks/acid-plant.md): molten sulfur in on the east face of its front cells (the burner gun's side, the blower being at its front), burned at about 1,100 C
 * in dry air from the main blower touching it (with the drying tower against the blower), its gas handed to the waste heat boiler
 * touching it. It drives the blower; its rate follows the blower's output.
 */
public class SulfurFurnaceBlockEntity extends PlantUnitBlockEntity {
	private double burning;   // kg of sulfur a second, the last interval

	public SulfurFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.SULFUR_FURNACE_ENTITY, pos, state);
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, SulfurFurnaceBlockEntity f) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		f.burning = 0;
		UnitMotorBlockEntity blower = Touching.find(level, pos, UnitMotorBlockEntity.class, m -> m.kind().equals("main_blower"));
		WasteHeatBoilerBlockEntity boiler = Touching.find(level, pos, WasteHeatBoilerBlockEntity.class, b -> true);
		double waiting = Lines.available(level, f.inlet(), Product.MOLTEN_SULFUR, Pull.MACHINE, f) * Tuning.MOLTEN_SULFUR_KG_PER_L;
		if (blower == null) {
			f.status = "NO AIR: set a MAIN BLOWER against the furnace";
		} else if (Touching.find(level, blower.getBlockPos(), PassivePartBlockEntity.class, p -> p.kind().equals("drying_tower")) == null) {
			f.status = "WET AIR: set a DRYING TOWER against the blower (wet gas makes acid mist that eats the steel)";
		} else if (boiler == null) {
			f.status = "NO BOILER: set a WASTE HEAT BOILER against the furnace";
		} else if (boiler.dry()) {
			f.status = "BOILER DRY: pump water to the waste heat boiler";
		} else if (waiting < 0.001) {
			f.status = "idle: no molten sulfur at its burner (the east side of its front)";
		} else {
			double strength = blower.drive(level);
			if (strength <= 0) {
				f.status = "starting: " + blower.describe();
			} else {
				double kg = Math.min(Math.min(AcidPlant.ratedSulfurKgs() * seconds() * strength, waiting), boiler.canTake());
				double got = kg > 0.0001 ? Lines.pull(level, f.inlet(), Product.MOLTEN_SULFUR, kg / Tuning.MOLTEN_SULFUR_KG_PER_L, Pull.MACHINE, f)
						* Tuning.MOLTEN_SULFUR_KG_PER_L : 0;
				boiler.burned(level, got, seconds());
				f.burning = got / seconds();
				f.status = got > 0 ? String.format("burning %.2f kg/s of sulfur in %,.0f Nm3/h of air: %.1f kg/s of burner gas at %.0f %% SO2", f.burning,
						f.burning * AcidPlant.airM3PerSulfur() * 3600, f.burning * AcidPlant.gasPerSulfur(), Tuning.BURNER_SO2_VOLUME * 100)
						: "idle: the boiler's burner gas has nowhere to go (pipe its back end to a converter)";
			}
		}
		f.setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("Sulfur furnace: " + status);
	}
}
