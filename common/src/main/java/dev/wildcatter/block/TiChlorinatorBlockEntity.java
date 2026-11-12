package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.TitaniumPigment;
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
 * The titanium chlorinator (docs/multiblocks/titanium-pigment.md), the pigment plant's controller: a fluid-bed chlorinator at about
 * 1,000 C. Ti slag bags or rutile and coke by a hopper on top; chlorine on the south face of its back cells (the oxidiser's chlorine and
 * the make-up on one line). The other metals' chlorides go to its cyclone as waste, counted. It runs the TiCl4 purification against its
 * east side, the oxidiser against that, and the pigment finishing against the oxidiser: 0.25 t of TiO2 an hour. Its fluidizing blower is
 * 30 kW at the world's industrial voltage, the box on the west face of its main cell.
 */
public class TiChlorinatorBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(2, ItemStack.EMPTY);
	private double tio2;     // kg of TiO2 in the opened feed
	private double coke;     // kg of coke opened
	private double crude;    // kg of crude TiCl4 for the purification
	private double waste;    // kg of waste chlorides, counted
	private double making;   // kg/h of TiO2 pigment, the last interval
	private double chlorinating;   // kg/h of TiO2 chlorinated, the last interval
	private String unit = "starting";

	public TiChlorinatorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.TI_CHLORINATOR_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.TI_SLAG_BAG) || stack.is(WildcatterItems.RUTILE) || stack.is(WildcatterItems.MET_COKE);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {0, 1} : new int[0];
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot == 0 ? stack.is(WildcatterItems.TI_SLAG_BAG) || stack.is(WildcatterItems.RUTILE) : slot == 1 && stack.is(WildcatterItems.MET_COKE);
	}

	private List<Lines.Face> chlorineIn() {
		return faces(at -> at[2] == 3 && at[1] == 0, Direction.SOUTH);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	private double crudeHold() {
		return TitaniumPigment.tio2Kgs() * TitaniumPigment.ticl4PerTio2() * Tuning.PLANT_HOLD_SECONDS * 8;
	}

	/** kg/h of TiO2 pigment over the last interval: 250 at the rating. */
	public double making() {
		return making;
	}

	/** kg/h of TiO2 chlorinated over the last interval. */
	public double chlorinating() {
		return chlorinating;
	}

	private void open() {
		ItemStack feed = slots.get(0);
		while (tio2 < TitaniumPigment.tio2Kgs() * seconds() * 2 && !feed.isEmpty()) {
			boolean slag = feed.is(WildcatterItems.TI_SLAG_BAG);
			tio2 += slag ? Tuning.BULK_BAG_KG * Tuning.TI_SLAG_TIO2 : Tuning.TI_MINERAL_ITEM_KG * Tuning.RUTILE_TIO2;
			feed.shrink(1);
			setChanged();
		}
		ItemStack c = slots.get(1);
		while (coke < TitaniumPigment.tio2Kgs() * TitaniumPigment.cokePerTio2() * seconds() * 2 && !c.isEmpty()) {
			coke += Tuning.MET_COKE_KG;
			c.shrink(1);
			setChanged();
		}
	}

	@Override
	protected void work(Level level) {
		making = 0;
		chlorinating = 0;
		TiCl4PurificationBlockEntity column = Touching.find(level, worldPosition, TiCl4PurificationBlockEntity.class, p -> true);
		TiCl4OxidiserBlockEntity oxidiser = column == null ? null : Touching.find(level, column.getBlockPos(), TiCl4OxidiserBlockEntity.class, o -> true);
		PigmentFinishingBlockEntity finishing = oxidiser == null ? null : Touching.find(level, oxidiser.getBlockPos(), PigmentFinishingBlockEntity.class, f -> true);
		if (column == null || oxidiser == null || finishing == null) {
			unit = "NOT BUILT: set " + (column == null ? "the TiCl4 PURIFICATION against its east side" : oxidiser == null ? "the TiCl4 OXIDISER against the purification"
					: "the PIGMENT FINISHING against the oxidiser");
			return;
		}
		if (plcHold(level)) {
			unit = status;
			return;
		}
		open();
		double rate = TitaniumPigment.tio2Kgs() * seconds();
		// 1. the chlorinator: feed, coke and chlorine to crude TiCl4
		boolean loop = false;   // the oxidiser's chlorine on its line: it is taken first, the make-up after
		for (Lines.Line line : Lines.lines(level, chlorineIn())) {
			loop |= line.ports().stream().anyMatch(p -> p.node() == oxidiser);
		}
		double back = loop ? oxidiser.held(Product.CHLORINE) : 0;
		double chlorine = Lines.available(level, chlorineIn(), Product.CHLORINE, Pull.MACHINE, this);
		chlorine = Math.max(chlorine, back);
		double kg = Math.min(rate * 1.2, Math.min(tio2, coke / TitaniumPigment.cokePerTio2()));
		kg = Math.min(kg, chlorine / TitaniumPigment.cl2InPerTio2());
		kg = Math.min(kg, (crudeHold() - crude) / TitaniumPigment.ticl4PerTio2());
		if (tio2 <= 1e-6) {
			unit = "NO FEED: Ti slag bags or rutile in a hopper on its top";
		} else if (coke <= 1e-6) {
			unit = "NO COKE: coke in a hopper on its top";
		} else if (chlorine <= 1e-6) {
			unit = "NO CHLORINE: pipe chlorine to the south face of its back (the oxidiser's and the make-up)";
		} else if (crude >= crudeHold() - 1e-6) {
			unit = "FULL: its TiCl4 has nowhere to go (the purification and the oxidiser are not taking it)";
		} else {
			double strength = drive(level);
			if (strength <= 0) {
				unit = "starting: " + describe();
				kg = 0;
			} else {
				kg *= strength;
				double need = kg * TitaniumPigment.cl2InPerTio2();
				double first = loop ? oxidiser.takeChlorine(need) : 0;
				Lines.pull(level, chlorineIn(), Product.CHLORINE, need - first, Pull.MACHINE, this);
				tio2 -= kg;
				coke = Math.max(0, coke - kg * TitaniumPigment.cokePerTio2());
				crude += kg * TitaniumPigment.ticl4PerTio2();
				waste += kg * TitaniumPigment.wastePerTio2();
				chlorinating = kg / seconds() * 3600;
				unit = String.format("chlorinating %.0f kg/h of TiO2 at 1,000 C on %.0f kg/h of chlorine: %.0f kg/h of TiCl4", chlorinating,
						kg * TitaniumPigment.cl2InPerTio2() / seconds() * 3600, kg * TitaniumPigment.ticl4PerTio2() / seconds() * 3600);
			}
		}
		// 2. the purification: crude to pure TiCl4, SiCl4 off
		double move = Math.min(crude, rate * 1.2 * TitaniumPigment.ticl4PerTio2());
		move = Math.min(move, crudeHold() - column.pure);
		if (move > 1e-7) {
			double strength = column.drive(level);
			if (strength <= 0) {
				column.unit = "starting: " + column.describe();
			} else {
				move *= strength;
				crude -= move;
				column.sicl4 += move * Tuning.TICL4_SICL4_SHARE;
				column.pure += move;
				column.unit = String.format("distilling %.0f kg/h of TiCl4, SiCl4 and VOCl3 off", move / seconds() * 3600);
			}
		} else {
			column.unit = crude <= 1e-6 ? "idle: no crude TiCl4" : "FULL: the oxidiser is not taking its TiCl4";
		}
		// 3. the oxidiser: pure TiCl4 and oxygen to raw TiO2 for the finishing, chlorine back
		double burn = Math.min(column.pure / TitaniumPigment.ticl4PerTio2(), rate * 1.2);
		double oxygen = Lines.available(level, oxidiser.oxygenIn(), Product.OXYGEN, Pull.MACHINE, oxidiser);
		burn = Math.min(burn, oxygen / TitaniumPigment.o2M3PerTio2());
		burn = Math.min(burn, oxidiser.chlorineRoom() / TitaniumPigment.cl2BackPerTio2());
		burn = Math.min(burn, finishing.room());
		if (column.pure <= 1e-6) {
			oxidiser.unit = "idle: no pure TiCl4";
		} else if (oxygen <= 1e-6) {
			oxidiser.unit = "NO OXYGEN: pipe oxygen from a VPSA unit to the north face of its front";
		} else if (oxidiser.chlorineRoom() <= 1e-6) {
			oxidiser.unit = "FULL: its chlorine is not drawn back to the chlorinator";
		} else if (burn > 1e-7) {
			double need = burn * TitaniumPigment.preheatJPerTio2();
			double got = GasFiring.fire(level, oxidiser.gasIn(), need, oxidiser);
			burn *= Math.min(1, got / need);
			if (burn <= 1e-7) {
				oxidiser.unit = "NO GAS: pipe pipeline gas to the east face of its front for the preheaters";
			} else {
				Lines.pull(level, oxidiser.oxygenIn(), Product.OXYGEN, burn * TitaniumPigment.o2M3PerTio2(), Pull.MACHINE, oxidiser);
				column.pure -= burn * TitaniumPigment.ticl4PerTio2();
				oxidiser.giveChlorine(burn * TitaniumPigment.cl2BackPerTio2());
				oxidiser.burned += burn;
				finishing.raw += burn;
				oxidiser.unit = String.format("burning %.0f kg/h of TiCl4 in %.0f Nm3/h of oxygen at 1,500 C: %.0f kg/h of TiO2, %.0f kg/h of chlorine back",
						burn * TitaniumPigment.ticl4PerTio2() / seconds() * 3600, burn * TitaniumPigment.o2M3PerTio2() / seconds() * 3600, burn / seconds() * 3600,
						burn * TitaniumPigment.cl2BackPerTio2() / seconds() * 3600);
			}
		}
		// 4. the finishing: raw TiO2, hydrate and steam to pigment
		double fin = Math.min(finishing.raw, Math.min(rate * 1.2, finishing.room()));
		if (finishing.hydrate < fin * TitaniumPigment.hydratePerTio2() && finishing.fed() > 0) {
			finishing.useOne();
			finishing.hydrate += Tuning.BULK_BAG_KG;
		}
		double steam = Lines.available(level, finishing.steamIn(), Product.PROCESS_STEAM, Pull.MACHINE, finishing);
		fin = Math.min(fin, Math.min(finishing.hydrate / TitaniumPigment.hydratePerTio2(), steam / TitaniumPigment.steamPerTio2()));
		if (finishing.raw <= 1e-6) {
			finishing.unit = "idle: no TiO2 from the oxidiser";
		} else if (finishing.hydrate <= 1e-6) {
			finishing.unit = "NO HYDRATE: alumina hydrate bags in a hopper on its top";
		} else if (steam <= 1e-6) {
			finishing.unit = "NO STEAM: pipe process steam to the east face of its front for the micronizer";
		} else if (fin <= 1e-7) {
			finishing.unit = "FULL: set a bagging station against it";
		} else {
			double strength = finishing.drive(level);
			if (strength <= 0) {
				finishing.unit = "starting: " + finishing.describe();
			} else {
				fin *= strength;
				Lines.pull(level, finishing.steamIn(), Product.PROCESS_STEAM, fin * TitaniumPigment.steamPerTio2(), Pull.MACHINE, finishing);
				finishing.hydrate -= fin * TitaniumPigment.hydratePerTio2();
				finishing.raw -= fin;
				finishing.pigment += fin;
				making = fin / seconds() * 3600;
				finishing.unit = String.format("milling, coating, drying and micronizing %.0f kg/h of rutile pigment", making);
			}
		}
		setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Titanium chlorinator (blower %.0f kW, %s): ", Tuning.TI_CHLORINATOR_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit
				+ String.format("; TiO2 in its feed %,.0f kg, coke %.0f kg, crude TiCl4 %.0f kg; %,.0f kg of waste chlorides to the cyclone; ", tio2, coke, crude, waste) + describe());
	}

	@Override
	public List<Point> points() {
		return RatePoints.POINTS;
	}

	@Override
	public boolean readPoint(int point) {
		return RatePoints.read(point, making > 0, unitRemote.auto());
	}

	@Override
	public double readMa(int point) {
		return RatePoints.ma(making, TitaniumPigment.tio2Kgs() * 3600);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("tio2", tio2);
		output.putDouble("coke", coke);
		output.putDouble("crude", crude);
		output.putDouble("waste", waste);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		tio2 = input.getDoubleOr("tio2", 0);
		coke = input.getDoubleOr("coke", 0);
		crude = input.getDoubleOr("crude", 0);
		waste = input.getDoubleOr("waste", 0);
	}
}
