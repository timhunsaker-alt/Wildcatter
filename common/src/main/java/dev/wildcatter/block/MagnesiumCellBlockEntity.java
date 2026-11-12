package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.Zirconium;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The magnesium cell (docs/multiblocks/zirconium.md, dc-bus.md): a monopolar MgCl2 cell at about 700 C, 14 kA at 5 V, a DC load on
 * its terminals (east face, z 3) or on the rectifier it touches, 90 % current efficiency. It splits the MgCl2 the Kroll reactor taps back to it (and the magnesia chlorinator's make-up)
 * into molten magnesium for the Kroll reactor and chlorine, out on the pipe at the east face of its front cells.
 */
public class MagnesiumCellBlockEntity extends PlantUnitBlockEntity implements dev.wildcatter.circuit.CircuitPart, Electrolysis.Part, HopperFed {
	/** 17c2: Pidgeon magnesium ingots by a hopper on top, make-up for the Kroll loop (zirconium-extensions.md item 4). */
	private final net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> slots = net.minecraft.core.NonNullList.withSize(1, net.minecraft.world.item.ItemStack.EMPTY);
	private final Electrolysis dc = new Electrolysis(new dev.wildcatter.circuit.DcCell(1, Tuning.MG_CELL_BACK_EMF, Tuning.MG_CELL_VOLTS, Tuning.MG_CELL_KA * 1000), 2, 3, Direction.EAST);
	private double mg;      // molten magnesium, kg
	private double mgCl2;   // kg
	private double share;

	public MagnesiumCellBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.MAGNESIUM_CELL_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 0 && at[1] == 0 && side == Direction.EAST ? Product.CHLORINE : null;
	}

	@Override
	protected double hold(Product product) {
		return 20;
	}

	public double mg() {
		return mg;
	}

	public double mgCl2() {
		return mgCl2;
	}

	public double takeMg(double kg) {
		double got = Math.max(0, Math.min(kg, mg));
		mg -= got;
		setChanged();
		return got;
	}

	/** Magnesium back from the vacuum distillation; returns what fitted. */
	public double addMg(double kg) {
		double fits = Math.max(0, Math.min(kg, Tuning.MG_CELL_HOLD_KG - mg));
		mg += fits;
		setChanged();
		return fits;
	}

	public double mgCl2Room() {
		return Math.max(0, Tuning.MG_CELL_HOLD_KG - mgCl2);
	}

	public void addMgCl2(double kg) {
		mgCl2 = Math.min(Tuning.MG_CELL_HOLD_KG, mgCl2 + kg);
		setChanged();
	}

	@Override
	public net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(net.minecraft.world.item.ItemStack stack) {
		return stack.is(dev.wildcatter.WildcatterItems.MAGNESIUM_INGOT);
	}

	/** 17c2: an ingot from its hopper melted into its magnesium when it has room for it. */
	private void meltIngot() {
		if (fed() > 0 && Tuning.MG_CELL_HOLD_KG - mg >= Tuning.MG_INGOT_KG) {
			useOne();
			mg += Tuning.MG_INGOT_KG;
			setChanged();
		}
	}

	public static void tick(Level level, BlockPos pos, BlockState state, MagnesiumCellBlockEntity c) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL == 0) {
			c.meltIngot();
		}
		if (c.plcHold(level)) {
			return;
		}
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		double clPerMg = Zirconium.CL2 / Zirconium.MG;
		boolean work = c.mgCl2 > 1e-6 && c.mg < Tuning.MG_CELL_HOLD_KG && c.room(Product.CHLORINE) > 1e-6;
		c.dc.setDrawing(level, work);
		c.share = c.dc.share();
		if (c.mgCl2 <= 1e-6) {
			c.status = "NO MgCl2: set the Kroll reactor or a magnesia chlorinator against it";
		} else if (!work) {
			c.status = c.mg >= Tuning.MG_CELL_HOLD_KG ? "FULL of magnesium: the Kroll reactor is not taking it" : "idle: its chlorine has nowhere to go (pipe it to a chlorinator)";
		} else if (c.dc.amps() < 1) {
			c.status = "NO DC: set a RECTIFIER against it, or run DC bus bars to its terminals (east face)";
		} else {
			double made = Math.min(Math.min(Zirconium.mgCellKgs() * seconds() * c.share, c.mgCl2 * Zirconium.magnesiumInMgCl2()),
					Math.min(Tuning.MG_CELL_HOLD_KG - c.mg, c.room(Product.CHLORINE) / clPerMg));
			c.mgCl2 -= made / Zirconium.magnesiumInMgCl2();
			c.mg += made;
			c.add(Product.CHLORINE, made * clPerMg);
			c.status = made > 1e-9 ? String.format("%.1f kA at %.1f V: %.1f kg/h of magnesium, %.1f kg/h of chlorine", c.dc.amps() / 1000, c.dc.volts(),
					made / seconds() * 3600, made * clPerMg / seconds() * 3600) : "starting: waiting for DC from the rectifier";
		}
		c.setChanged();
	}

	@Override
	public Electrolysis electrolysis() {
		return dc;
	}

	@Override
	public boolean hasPort(Direction face, dev.wildcatter.circuit.Conductor role, dev.wildcatter.energy.Voltage voltage) {
		return false;
	}

	@Override
	public void build(dev.wildcatter.circuit.Circuit circuit) {
		dc.build(circuit, this);
	}

	@Override
	public void apply(dev.wildcatter.circuit.Circuit circuit, int ticks) {
		dc.apply(circuit);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Magnesium cell: %s; magnesium %.1f kg, MgCl2 %.1f kg, chlorine %.1f kg held%s", status, mg, mgCl2, held(Product.CHLORINE),
				fed() > 0 ? String.format("; %d magnesium ingots in its hopper bin", fed()) : ""));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("mg", mg);
		output.putDouble("mgcl2", mgCl2);
		net.minecraft.world.ContainerHelper.saveAllItems(output, slots);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		mg = input.getDoubleOr("mg", 0);
		mgCl2 = input.getDoubleOr("mgcl2", 0);
		slots.clear();
		net.minecraft.world.ContainerHelper.loadAllItems(input, slots);
	}
}
