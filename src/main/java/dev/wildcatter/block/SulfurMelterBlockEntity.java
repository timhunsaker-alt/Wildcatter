package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.AcidPlant;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The sulfur melting pit (docs/multiblocks/acid-plant.md): native sulfur in by a hopper on top, held molten at 135-145 C by steam
 * coils off a waste heat boiler touching it, or by gas on its east side while the plant is cold. Molten sulfur out of the south face
 * of its back cells.
 */
public class SulfurMelterBlockEntity extends PlantUnitBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double sulfur;   // kg broken out of the items, not yet melted

	public SulfurMelterBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.SULFUR_MELTER_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(OreBlocks.MINERALS.get(Ore.SULFUR));
	}

	private List<Lines.Face> gas() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 7 && at[1] == 0 && side == Direction.SOUTH ? Product.MOLTEN_SULFUR : null;
	}

	@Override
	protected double hold(Product product) {
		return AcidPlant.ratedSulfurKgs() * Tuning.PLANT_HOLD_SECONDS / Tuning.MOLTEN_SULFUR_KG_PER_L;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, SulfurMelterBlockEntity m) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		double max = Math.min(AcidPlant.ratedSulfurKgs() * seconds(), m.room(Product.MOLTEN_SULFUR) * Tuning.MOLTEN_SULFUR_KG_PER_L);
		if (m.sulfur < max && m.fed() > 0) {
			m.useOne();
			m.sulfur += Ore.SULFUR.kgPerBlock();
		}
		double kg = Math.min(max, m.sulfur);
		if (kg < 0.001) {
			m.status = m.sulfur < 0.001 ? "idle: no sulfur (put a hopper of sulfur on top)" : "idle: its molten sulfur has nowhere to go (pipe its back end to a sulfur furnace)";
			m.setChanged();
			return;
		}
		WasteHeatBoilerBlockEntity boiler = Touching.find(level, pos, WasteHeatBoilerBlockEntity.class, b -> b.steaming(level));
		String heat;
		if (boiler != null) {
			heat = "on the boiler's steam";
		} else {
			double need = Tuning.MELTER_START_WATTS * seconds() * kg / (AcidPlant.ratedSulfurKgs() * seconds());
			double got = GasFiring.fire(level, m.gas(), need, m);
			kg *= need > 0 ? Math.min(1, got / need) : 0;
			heat = String.format("on start-up gas (%.0f kW)", got / seconds() / 1000);
			if (kg < 0.001) {
				m.status = "COLD: no steam from a waste heat boiler touching it, and no gas at its east side to start it";
				m.setChanged();
				return;
			}
		}
		m.sulfur -= kg;
		m.add(Product.MOLTEN_SULFUR, kg / Tuning.MOLTEN_SULFUR_KG_PER_L);
		m.status = String.format("melting %.2f kg/s of sulfur %s", kg / seconds(), heat);
		m.setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("Sulfur melter: " + status + String.format("; %,.0f kg of sulfur waiting, %d blocks in its hopper bin", sulfur, fed()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("sulfur", sulfur);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		sulfur = input.getDoubleOr("sulfur", 0);
	}
}
