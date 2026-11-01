package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.Zirconium;
import dev.wildcatter.ore.Ore;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The magnesia chlorinator (docs/multiblocks/zirconium.md): an IG Farben shaft chlorinator, MgO + C + Cl2 -> MgCl2 + CO at 800-1,000
 * C, 30 kg of MgCl2 an hour. Magnesia and coal by a hopper on top, chlorine off the pipe at the north face of its front cells. It tops
 * up the magnesium cell it touches while the cell holds less than a quarter of its MgCl2: the magnesium loop's make-up.
 */
public class MagnesiaChlorinatorBlockEntity extends PlantUnitBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(2, ItemStack.EMPTY);
	private double mgo, carbon, made;

	public MagnesiaChlorinatorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.MAGNESIA_CHLORINATOR_ENTITY, pos, state);
	}

	private List<Lines.Face> chlorine() {
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

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.MAGNESIA) || stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
	}

	private boolean takes(int slot, ItemStack stack) {
		return slot == 0 && stack.is(WildcatterItems.MAGNESIA) || slot == 1 && (stack.is(Items.COAL) || stack.is(Items.CHARCOAL));
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {0, 1} : new int[0];
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return takes(slot, stack);
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side == Direction.UP && takes(slot, stack);
	}

	private boolean useFrom(int slot) {
		ItemStack stack = slots.get(slot);
		if (stack.isEmpty()) {
			return false;
		}
		stack.shrink(1);
		setChanged();
		return true;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, MagnesiaChlorinatorBlockEntity m) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		MagnesiumCellBlockEntity cell = Touching.find(level, pos, MagnesiumCellBlockEntity.class, c -> true);
		double rate = Tuning.MGCL2_CHLORINATOR_KGH / 3600 * seconds();
		if (m.mgo < rate * Zirconium.mgoPerMgCl2() && m.useFrom(0)) {
			m.mgo += Ore.MAGNESITE.kgPerBlock() * Tuning.MGO_IN_MGCO3;
		}
		if (m.carbon < rate * Zirconium.carbonPerMgCl2() && m.useFrom(1)) {
			m.carbon += Tuning.COAL_KG * Tuning.COAL_CARBON;
		}
		double cl2 = Lines.available(level, m.chlorine(), Product.CHLORINE, Pull.MACHINE, m);
		if (cell == null) {
			m.status = "NOT BUILT: set it against the MAGNESIUM CELL";
		} else if (cell.mgCl2() >= Tuning.MG_CELL_HOLD_KG / 4) {
			m.status = "standing by: the magnesium cell has its MgCl2";
		} else if (m.mgo <= 1e-9) {
			m.status = "NO MAGNESIA: put magnesia in its hopper";
		} else if (m.carbon <= 1e-9) {
			m.status = "NO CARBON: put coal or charcoal in its hopper";
		} else if (cl2 <= 1e-9) {
			m.status = "NO CHLORINE: pipe chlorine to the north face of its front";
		} else {
			double kg = Math.min(Math.min(rate, cell.mgCl2Room()), Math.min(Math.min(m.mgo / Zirconium.mgoPerMgCl2(), m.carbon / Zirconium.carbonPerMgCl2()),
					cl2 / Zirconium.chlorinePerMgCl2()));
			kg = Lines.pull(level, m.chlorine(), Product.CHLORINE, kg * Zirconium.chlorinePerMgCl2(), Pull.MACHINE, m) / Zirconium.chlorinePerMgCl2();
			m.mgo -= kg * Zirconium.mgoPerMgCl2();
			m.carbon -= kg * Zirconium.carbonPerMgCl2();
			cell.addMgCl2(kg);
			m.made += kg;
			m.status = String.format("chlorinating magnesia: %.1f kg/h of MgCl2 to the magnesium cell", kg / seconds() * 3600);
		}
		m.setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Magnesia chlorinator: %s; MgO %.0f kg, carbon %.0f kg on hand; %.0f kg of MgCl2 made", status, mgo, carbon, made));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("mgo", mgo);
		output.putDouble("carbon", carbon);
		output.putDouble("made", made);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		mgo = input.getDoubleOr("mgo", 0);
		carbon = input.getDoubleOr("carbon", 0);
		made = input.getDoubleOr("made", 0);
	}
}
