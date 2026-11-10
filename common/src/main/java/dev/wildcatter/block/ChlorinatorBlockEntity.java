package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
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
 * The fluid-bed carbochlorinator (docs/multiblocks/zirconium.md): zircon from the mineral separator it touches, or zircon concentrate
 * ({@code #wildcatter:zircon_feed} with its kg file, see {@link ZirconFeed}: Tailings' zircon concentrate) by a hopper on top; carbon from coal or charcoal by a
 * hopper on top, chlorine off the pipe at the east face of its bottom cells. ZrSiO4 + 4C + 4Cl2 -> ZrCl4 + SiCl4 + 4CO
 * at about 1,200 C, the reaction keeping it hot; its fluidizing blower is 15 kW at 480 V. SiCl4 and CO are counted.
 */
public class ChlorinatorBlockEntity extends UnitMotorBlockEntity implements ZrStage, HopperFed {
	/** Slot 0 carbon, slot 1 zircon feed. */
	private final NonNullList<ItemStack> slots = NonNullList.withSize(2, ItemStack.EMPTY);
	private double metal;    // ZrCl4, as kg of metal
	private double zircon;   // kg of ZrSiO4 from zircon feed, not yet chlorinated
	private double carbon;   // kg
	private double siCl4, co;
	private String unit = "starting";

	public ChlorinatorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CHLORINATOR_ENTITY, pos, state);
	}

	private List<Lines.Face> chlorine() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	@Override
	public double zirconium() {
		return metal;
	}

	@Override
	public double takeZirconium(double kg) {
		double got = Math.max(0, Math.min(kg, metal));
		metal -= got;
		setChanged();
		return got;
	}

	@Override
	public void addZirconium(double kg) {
		metal = Math.min(Tuning.ZR_STAGE_HOLD_KG, metal + kg);
		setChanged();
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return SolidCarbon.is(stack);
	}

	/** True for zircon feed: anything in {@code #wildcatter:zircon_feed} (block/ZirconFeed), as Tailings' zircon concentrate. */
	public static boolean zirconFeed(ItemStack stack) {
		return ZirconFeed.is(stack);
	}

	private double itemKg(ItemStack stack) {
		return ZirconFeed.kg(level == null ? null : level.getServer(), stack);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {0, 1} : new int[0];
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot == 0 ? takes(stack) : slot == 1 && zirconFeed(stack);
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side == Direction.UP && canPlaceItem(slot, stack);
	}

	/** kg of ZrSiO4 from zircon feed it holds, burning or in its hopper slot. */
	public double zirconFeedKg() {
		return zircon + slots.get(1).getCount() * itemKg(slots.get(1));
	}

	/** Zircon feed as kg of zirconium: what is left of the last item, topped up from the slot. */
	private double feedZirconium(double wanted) {
		while (zircon * Tuning.ZR_IN_ZIRCON < wanted && zirconFeed(slots.get(1))) {
			zircon += itemKg(slots.get(1));
			slots.get(1).shrink(1);
			setChanged();
		}
		return zircon * Tuning.ZR_IN_ZIRCON;
	}

	@Override
	protected void work(Level level) {
		ZrStage separator = Touching.find(level, worldPosition, ZrLineBlockEntity.class, z -> z.kind().equals("mineral_separator"));
		boolean fed = zirconFeedKg() > 1e-9;
		if (separator == null && !fed) {
			unit = "NOT BUILT: set it against the MINERAL SEPARATOR, or feed it zircon concentrate by a hopper on top";
			return;
		}
		double rate = Tuning.ZR_LINE_KGS * 1.2 * seconds();
		ZrStage up = fed ? feedStage() : separator;   // zircon concentrate in its hopper first, then the separator's
		if (carbon < rate * Zirconium.carbonPerZr() && fed() > 0) {
			carbon += SolidCarbon.carbonKg(useOne());
		}
		double cl2 = Lines.available(level, chlorine(), Product.CHLORINE, Pull.MACHINE, this);
		if (up.zirconium() < 1e-9) {
			unit = separator != null ? "waiting for zircon from the mineral separator" : "waiting for zircon concentrate in its hopper";
			return;
		}
		if (carbon <= 1e-9) {
			unit = "NO CARBON: put coal, charcoal or coke in its hopper";
			return;
		}
		if (cl2 <= 1e-9) {
			unit = "NO CHLORINE: pipe chlorine to the east face of its bottom cells";
			return;
		}
		double room = Tuning.ZR_STAGE_HOLD_KG - metal;
		if (room <= 1e-9) {
			unit = "FULL: the hafnium column is not taking its ZrCl4";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting the blower: " + describe();
			return;
		}
		double zr = Math.min(Math.min(rate * strength, room), Math.min(up.zirconium(), Math.min(carbon / Zirconium.carbonPerZr(), cl2 / Zirconium.chlorinePerZr())));
		zr = Lines.pull(level, chlorine(), Product.CHLORINE, zr * Zirconium.chlorinePerZr(), Pull.MACHINE, this) / Zirconium.chlorinePerZr();
		zr = up.takeZirconium(zr);
		carbon -= zr * Zirconium.carbonPerZr();
		metal += zr;
		siCl4 += zr * Zirconium.siCl4PerZr();
		co += zr * 4 * 28.01 / Zirconium.ZR;
		unit = String.format("chlorinating at about 1,200 C: %.1f kg/h of zircon, %.1f kg/h of chlorine; carbon %.0f kg", zr * Zirconium.zirconPerZr() / seconds() * 3600,
				zr * Zirconium.chlorinePerZr() / seconds() * 3600, carbon);
	}

	/** The zircon feed as a stage the chlorination draws on: kg of zirconium in it, taken as ZrSiO4. */
	private ZrStage feedStage() {
		return new ZrStage() {
			@Override
			public double zirconium() {
				return feedZirconium(Tuning.ZR_LINE_KGS * 1.2 * seconds());
			}

			@Override
			public double takeZirconium(double kg) {
				double got = Math.max(0, Math.min(kg, feedZirconium(kg)));
				zircon = Math.max(0, zircon - got / Tuning.ZR_IN_ZIRCON);
				setChanged();
				return got;
			}

			@Override
			public void addZirconium(double kg) {
			}
		};
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Chlorinator (fluid bed): %s; ZrCl4 %.2f kg of metal held; SiCl4 %.0f kg, CO %.0f kg to the scrubber; %s", unit, metal,
				siCl4, co, describe()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("metal", metal);
		output.putDouble("carbon", carbon);
		output.putDouble("sicl4", siCl4);
		output.putDouble("co", co);
		if (zircon > 0) {
			output.putDouble("zircon", zircon);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		metal = input.getDoubleOr("metal", 0);
		carbon = input.getDoubleOr("carbon", 0);
		siCl4 = input.getDoubleOr("sicl4", 0);
		co = input.getDoubleOr("co", 0);
		zircon = input.getDoubleOr("zircon", 0);
	}
}
