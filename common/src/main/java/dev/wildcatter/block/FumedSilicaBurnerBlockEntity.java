package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.Zirconium;
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
 * The fumed silica burner (docs/multiblocks/zirconium-extensions.md item 1, 17c2): a flame-hydrolysis burner, coagulation pipes, a
 * cyclone and a baghouse, 2 x 6 x 3. Against the chlorinator it takes the SiCl4 the chlorinator keeps for it and burns it in a
 * hydrogen-oxygen flame, SiCl4 + 2 H2 + O2 -> SiO2 + 4 HCl: hydrogen off the pipe at the east face of its bottom cells, oxygen at the
 * east face one up; fumed silica in 25 kg sacks out to a hopper under it; the HCl absorbed to 32 % hydrochloric acid out of the south
 * face of its back cells. Its fans and baghouse: 15 kW on the industrial supply, box on the north face of its main cell (its west is against the chlorinator).
 */
public class FumedSilicaBurnerBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double silica;   // kg toward the next sack
	private double burned;   // kg of SiCl4
	private String unit = "starting";

	public FumedSilicaBurnerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.FUMED_SILICA_BURNER_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return false;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.DOWN ? new int[] {0} : new int[0];
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return side == Direction.DOWN;
	}

	private List<Lines.Face> hydrogen() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	private List<Lines.Face> oxygen() {
		return faces(at -> at[1] == 1, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 2 && at[1] == 0 && side == Direction.SOUTH ? Product.HYDROCHLORIC_ACID : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.HYDROCHLORIC_ACID ? 50.0 : 0;
	}

	/** Its box is on the north face of its main cell: its west is against the chlorinator. */
	@Override
	protected Direction boxSide() {
		return Direction.NORTH;
	}

	public int sacks() {
		return slots.get(0).getCount();
	}

	/** For the game test: silica as if burned. */
	public void addSilicaForTest(double kg) {
		silica += kg;
		setChanged();
	}

	@Override
	protected void work(Level level) {
		ChlorinatorBlockEntity chlorinator = Touching.find(level, worldPosition, ChlorinatorBlockEntity.class, c -> true);
		if (silica >= Tuning.FUMED_SILICA_SACK_KG - 1e-6) {
			ItemStack out = slots.get(0);
			if (out.isEmpty()) {
				slots.set(0, new ItemStack(WildcatterItems.FUMED_SILICA));
			} else if (out.getCount() < out.getMaxStackSize()) {
				out.grow(1);
			} else {
				unit = "FULL: take the sacks of fumed silica (a hopper under it)";
				return;
			}
			silica -= Tuning.FUMED_SILICA_SACK_KG;
			setChanged();
		}
		if (chlorinator == null) {
			unit = "NOT BUILT: set it against the CHLORINATOR, whose SiCl4 it burns";
			return;
		}
		if (chlorinator.siCl4Held() < 1e-9) {
			unit = "waiting for SiCl4 from the chlorinator";
			return;
		}
		double h2 = Lines.available(level, hydrogen(), Product.HYDROGEN, Pull.MACHINE, this);
		double o2 = Lines.available(level, oxygen(), Product.OXYGEN, Pull.MACHINE, this);
		if (h2 <= 1e-9) {
			unit = "NO HYDROGEN: pipe hydrogen to the east face of its bottom cells";
			return;
		}
		if (o2 <= 1e-9) {
			unit = "NO OXYGEN: pipe oxygen to the east face of its cells one up";
			return;
		}
		double room = room(Product.HYDROCHLORIC_ACID);
		if (room <= 1e-6) {
			unit = "FULL: its hydrochloric acid has nowhere to go (pipe its back to a tank)";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting the fans: " + describe();
			return;
		}
		double want = Zirconium.siCl4Kgh() / 3600 * 1.2 * seconds() * strength;
		double kg = Math.min(Math.min(want, chlorinator.siCl4Held()), Math.min(h2 / Zirconium.hydrogenPerSiCl4(), o2 / Zirconium.oxygenPerSiCl4()));
		kg = Math.min(kg, room * Tuning.HCL_STRENGTH / Zirconium.hclPerSiCl4());
		kg = chlorinator.takeSiCl4(kg);
		Lines.pull(level, hydrogen(), Product.HYDROGEN, kg * Zirconium.hydrogenPerSiCl4(), Pull.MACHINE, this);
		Lines.pull(level, oxygen(), Product.OXYGEN, kg * Zirconium.oxygenPerSiCl4(), Pull.MACHINE, this);
		add(Product.HYDROCHLORIC_ACID, kg * Zirconium.hclPerSiCl4() / Tuning.HCL_STRENGTH);
		silica += kg * Zirconium.silicaPerSiCl4();
		burned += kg;
		double h = 3600 / seconds();
		unit = String.format("burning %.1f kg/h of SiCl4 in %.1f Nm3/h of hydrogen and %.1f of oxygen: %.1f kg/h of fumed silica, %.1f kg/h of HCl as 32 %% acid", kg * h,
				kg * Zirconium.hydrogenPerSiCl4() * h, kg * Zirconium.oxygenPerSiCl4() * h, kg * Zirconium.silicaPerSiCl4() * h, kg * Zirconium.hclPerSiCl4() * h);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Fumed silica burner (%.0f kW, %s): %s; sack %.1f of %.0f kg, %d sacks for a hopper; %,.0f kg of SiCl4 burned; HCl acid %.0f kg held; ",
				Tuning.FUMED_SILICA_WATTS / 1000, Voltage.INDUSTRIAL.label(), unit, silica, Tuning.FUMED_SILICA_SACK_KG, sacks(), burned, held(Product.HYDROCHLORIC_ACID)) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("silica", silica);
		output.putDouble("burned", burned);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		silica = input.getDoubleOr("silica", 0);
		burned = input.getDoubleOr("burned", 0);
	}
}
