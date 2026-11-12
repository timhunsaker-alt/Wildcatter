package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.DcCell;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Potline;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.item.TappingCrucibleItem;
import dev.wildcatter.multiblock.BigParts;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A 180 kA prebake Hall–Héroult pot (docs/multiblocks/potline.md), its shell set across the line: a DC cell of 1.65 V back-EMF and
 * 4.0 V at 180 kA, its anode riser (+) on its west face and its cathode collector bars (−) on its east face at y 1, z 5, so a pot set
 * beside the next joins it through 1.67 µΩ of risers. Its hopper on top feeds reacted alumina bags (slot 0), fresh alumina bags (1),
 * AlF3 bags (2) and rodded anodes (3); spent anodes go out below. It changes its own anodes, goes to an anode effect with its
 * alumina gone, holds two days of metal for the tapping crucible, passes its gas along the row's duct to the row's end pot, and
 * freezes after 4 h without current.
 */
public class PotBlockEntity extends PlantUnitBlockEntity implements CircuitPart, HopperFed {
	private static final Item[] INS = {WildcatterItems.REACTED_ALUMINA_BAG, WildcatterItems.ALUMINA_BAG, WildcatterItems.ALF3_BAG, WildcatterItems.RODDED_ANODE};
	private static final int OUT = 4;
	/** 20c: lithium carbonate bags, a slot after the 17a ones so theirs keep their numbers. */
	private static final int LITHIUM_SLOT = INS.length + OUT;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(LITHIUM_SLOT + 1, ItemStack.EMPTY);
	private double lithium;          // kg of Li2CO3 in its bath (20c): it runs 0.1 V lower while it has some
	private final DcCell cell = new DcCell(1, Tuning.POT_BACK_EMF, Tuning.POT_CELL_VOLTS, Tuning.POT_AMPS);
	private int plus;
	private int minus;
	private double alumina = Tuning.POT_ALUMINA_BIN_KG;   // a new pot starts with its bins full
	private double alf3 = Tuning.POT_ALF3_BIN_KG;
	private double metal;
	private double burned;           // kg of carbon since the last anode change
	private int overdue;
	private boolean effect;
	private double clearing;         // seconds of alumina back, in an anode effect
	private double cf4;
	private double cold;             // seconds below the freezing current
	private boolean frozen;
	private boolean treated = true;  // its gas went to the duct last interval
	private double bath = Tuning.POT_BATH_ALUMINA_KG;   // alumina dissolved in its bath, kg: what it runs on while its feeders are stopped
	private boolean cutOut;          // shorted out of the line by its shorting bars
	private final dev.wildcatter.circuit.DriveLoad aux = new dev.wildcatter.circuit.DriveLoad(Voltage.INDUSTRIAL, Tuning.MOTOR_POWER_FACTOR, 0);
	private int changed;
	private double made;             // kg/s, for the readout

	public PotBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.POT_ENTITY, pos, state);
	}

	// ---- the hopper ---------------------------------------------------------------------------------------------------------
	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(INS[0]) || stack.is(INS[1]);
	}

	private static boolean takesIn(int slot, ItemStack stack) {
		return slot < INS.length && stack.is(INS[slot]) || slot == LITHIUM_SLOT && stack.is(WildcatterItems.LI2CO3_BAG);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {0, 1, 2, 3, LITHIUM_SLOT} : side == Direction.DOWN ? new int[] {4, 5, 6, 7} : new int[0];
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return takesIn(slot, stack);
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side == Direction.UP && takesIn(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot >= INS.length && slot < LITHIUM_SLOT && side == Direction.DOWN;
	}

	/** Volts across the pot, the last solve. */
	public double cellVolts() {
		return cell.volts();
	}

	/** kg of lithium carbonate in its bath (20c). */
	public double lithium() {
		return lithium;
	}

	public int outCount() {
		int n = 0;
		for (int i = INS.length; i < LITHIUM_SLOT; i++) {
			n += slots.get(i).getCount();
		}
		return n;
	}

	public void giveAll(Player player) {
		for (int i = INS.length; i < LITHIUM_SLOT; i++) {
			ItemStack stack = slots.get(i);
			if (!stack.isEmpty()) {
				if (!player.getInventory().add(stack.copy())) {
					net.minecraft.world.Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), stack.copy());
				}
				slots.set(i, ItemStack.EMPTY);
			}
		}
		setChanged();
	}

	private boolean putOut(ItemStack made) {
		for (int i = INS.length; i < LITHIUM_SLOT; i++) {
			ItemStack in = slots.get(i);
			if (in.isEmpty()) {
				slots.set(i, made);
				return true;
			}
			if (ItemStack.isSameItemSameComponents(in, made) && in.getCount() + made.getCount() <= in.getMaxStackSize()) {
				in.grow(made.getCount());
				return true;
			}
		}
		return false;
	}

	// ---- the row and its duct -----------------------------------------------------------------------------------------------
	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[1] == 5 && at[2] == 5 && (at[0] == 0 && side == Direction.WEST || at[0] == 5 && side == Direction.EAST) ? Product.POT_GAS : null;
	}

	@Override
	protected double hold(Product product) {
		return Tuning.POT_GAS_M3H / 3600 * Tuning.POT_GAS_HOLD_S * 40;
	}

	/** The pot touching this one's {@code local} terminal face (west or east), or null. */
	private @Nullable PotBlockEntity beside(Direction local) {
		if (level == null) {
			return null;
		}
		BlockPos cell = LeasePartBlock.at(worldPosition, getBlockState(), local == Direction.EAST ? 5 : 0, 1, 5).relative(LeasePartBlock.world(getBlockState(), local));
		return level.getBlockEntity(BigParts.owner(level, cell)) instanceof PotBlockEntity pot && pot != this ? pot : null;
	}

	/** The pots of its row, joined side by side, with this one. */
	private Set<PotBlockEntity> row() {
		Set<PotBlockEntity> seen = new LinkedHashSet<>();
		ArrayDeque<PotBlockEntity> todo = new ArrayDeque<>(List.of(this));
		while (!todo.isEmpty() && seen.size() < 64) {
			PotBlockEntity pot = todo.poll();
			if (seen.add(pot)) {
				for (Direction side : new Direction[] {Direction.WEST, Direction.EAST}) {
					PotBlockEntity next = pot.beside(side);
					if (next != null && !seen.contains(next)) {
						todo.add(next);
					}
				}
			}
		}
		return seen;
	}

	/** Gas from a pot of its row; returns what fitted. Only a row's end pot, with a free duct end, takes it. */
	private double takeGas(double m3) {
		double fits = Math.min(m3, room(Product.POT_GAS));
		add(Product.POT_GAS, fits);
		return fits;
	}

	private boolean isEnd() {
		return beside(Direction.WEST) == null || beside(Direction.EAST) == null;
	}

	/** Passes {@code m3} of its gas to the row's end pot with the most room; true if it all went. */
	private boolean passGas(double m3) {
		PotBlockEntity end = null;
		for (PotBlockEntity pot : row()) {
			if (pot.isEnd() && (end == null || pot.room(Product.POT_GAS) > end.room(Product.POT_GAS))) {
				end = pot;
			}
		}
		return end == null || end.takeGas(m3) >= m3 * 0.999;
	}

	// ---- the circuit --------------------------------------------------------------------------------------------------------
	/** Its auxiliaries' 480 V: the pot control cabinet on its front face, at its main cell. */
	private Direction auxFace() {
		return LeasePartBlock.world(getBlockState(), Direction.NORTH);
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return face == auxFace() && voltage == Voltage.INDUSTRIAL && role != Conductor.N;
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return cell.equals(worldPosition) && hasPort(face, role, voltage);
	}

	/** True while its auxiliaries have their 480 V. */
	public boolean auxPowered() {
		return !aux.undervoltage() && aux.volts() >= Voltage.INDUSTRIAL.lineToLine() * Tuning.LAND_RIG_UNDERVOLTAGE;
	}

	@Override
	public void build(Circuit circuit) {
		aux.setDemand(Tuning.POT_AUX_WATTS);
		aux.build(circuit, worldPosition, List.of(auxFace()));
		plus = circuit.node();
		minus = circuit.node();
		cell.setBackEmf(effect ? Potline.anodeEffectBackEmf() : Tuning.POT_BACK_EMF - (lithium > 1e-6 ? Tuning.POT_LITHIUM_VOLTS : 0));
		cell.build(circuit, plus, minus, !frozen && !cutOut);   // a pot in series is never opened, but a frozen bath is solid
		if (cutOut) {
			circuit.resistor(plus, minus, Tuning.POT_SHORTING_OHMS);   // its shorting bars carry the line past it
		}
		BlockState state = getBlockState();
		circuit.dcPort(LeasePartBlock.at(worldPosition, state, 0, 1, 5), LeasePartBlock.world(state, Direction.WEST), plus, Tuning.POT_RISER_OHMS / 2, false);
		circuit.dcPort(LeasePartBlock.at(worldPosition, state, 5, 1, 5), LeasePartBlock.world(state, Direction.EAST), minus, Tuning.POT_RISER_OHMS / 2, false);
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		aux.apply(circuit, ticks);
		cell.apply(circuit, plus, minus);
	}

	public double amps() {
		return cell.amps();
	}

	public double volts() {
		return cell.volts();
	}

	// ---- each interval ------------------------------------------------------------------------------------------------------
	public static void tick(Level level, BlockPos pos, BlockState state, PotBlockEntity pot) {
		if (pot.plcHold(level)) {
			return;
		}
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		pot.work(level);
		pot.setChanged();
	}

	private void work(Level level) {
		double s = seconds();
		double amps = frozen || cutOut ? 0 : cell.amps();
		made = 0;
		if (cutOut) {
			status = frozen ? "CUT OUT, FROZEN: its shorting bars carry the line past it; break it for its spent pot lining"
					: "CUT OUT: its shorting bars carry the line past it; sneak and right-click to cut it back in";
			return;
		}
		if (frozen) {
			status = "FROZEN: its bath is solid and its circuit open; sneak and right-click to short it out of the line, or break it (it leaves spent pot lining) and bus around it";
			return;
		}
		// the freeze clock
		if (amps < Tuning.POT_FREEZE_AMPS) {
			cold += s;
			if (cold >= Tuning.POT_FREEZE_SECONDS) {
				frozen = true;
				CircuitManager.markDirty(level);
				status = "FROZEN: its bath is solid and its circuit open; sneak and right-click to short it out of the line, or break it (it leaves spent pot lining) and bus around it";
				return;
			}
		} else {
			cold = 0;
		}
		// the bins: reacted alumina first
		for (int slot = 0; slot < 2; slot++) {
			if (alumina <= Tuning.POT_ALUMINA_BIN_KG - Tuning.BULK_BAG_KG && !slots.get(slot).isEmpty()) {
				slots.get(slot).shrink(1);
				alumina += Tuning.BULK_BAG_KG;
			}
		}
		if (alf3 <= Tuning.POT_ALF3_BIN_KG - Tuning.BULK_BAG_KG && !slots.get(2).isEmpty()) {
			slots.get(2).shrink(1);
			alf3 += Tuning.BULK_BAG_KG;
		}
		if (lithium <= 1e-6 && !slots.get(LITHIUM_SLOT).isEmpty()) {   // 20c: a bag of lithium carbonate into its bath
			slots.get(LITHIUM_SLOT).shrink(1);
			lithium += Tuning.BULK_BAG_KG;
			dev.wildcatter.circuit.CircuitManager.markDirty(level);
		}
		// its point feeders keep its bath's alumina up while its auxiliaries have power; without, it runs on what is dissolved
		boolean fed = auxPowered();
		if (fed) {
			double top = Math.min(alumina, Tuning.POT_BATH_ALUMINA_KG - bath);
			bath += top;
			alumina -= top;
		}
		// the anode effect: its alumina gone, back two minutes after it returns
		boolean was = effect;
		if (bath <= 1e-6) {
			effect = true;
			clearing = 0;
		} else if (effect) {
			clearing += s;
			if (clearing >= Tuning.POT_AE_CLEAR_SECONDS) {
				effect = false;
			}
		}
		if (effect != was) {
			CircuitManager.markDirty(level);
		}
		// anodes: change a due or overdue one from its hopper
		while (overdue > 0 && !slots.get(3).isEmpty() && putOut(new ItemStack(WildcatterItems.SPENT_ANODE))) {
			slots.get(3).shrink(1);
			overdue--;
			changed++;
		}
		if (amps < 1) {
			status = cold > 0 ? String.format("NO CURRENT: FREEZES IN %s; bus it to a rectiformer yard (its + riser west, its - collector east, y 1)",
					hours(Tuning.POT_FREEZE_SECONDS - cold)) : "NO CURRENT";
			return;
		}
		double gas = Potline.gasM3s(amps) * s;
		treated = passGas(gas);
		if (effect) {
			cf4 += Tuning.POT_AE_CF4_KG_PER_MIN * s / 60 * Math.min(1, amps / Tuning.POT_AMPS);
			status = String.format("ANODE EFFECT (%.0f V, CF4 %.1f kg): NO ALUMINA, put alumina bags in its hopper%s", cell.volts(), cf4,
					alumina > 1e-6 ? String.format("; clearing in %.0f s", Tuning.POT_AE_CLEAR_SECONDS - clearing) : "");
			return;
		}
		if (metal >= Tuning.POT_METAL_HOLD_KG - 1e-6) {
			status = String.format("FULL: TAP IT (%,.0f kg of metal; right-click it with a tapping crucible)", metal);
			return;
		}
		boolean noAlf3 = alf3 <= 1e-6;
		double ce = Potline.currentEfficiency(overdue, noAlf3);
		double al = Potline.aluminiumKgs(amps, ce) * s;
		al = Math.min(al, Math.min((bath + (fed ? alumina : 0)) / Tuning.POT_ALUMINA_PER_AL, Tuning.POT_METAL_HOLD_KG - metal));
		double use = al * Tuning.POT_ALUMINA_PER_AL;
		double fromBath = fed ? Math.min(bath, use) : use;
		bath = Math.max(0, bath - fromBath);
		if (fed) {
			alumina = Math.max(0, alumina - (use - fromBath));
			double top = Math.min(alumina, Tuning.POT_BATH_ALUMINA_KG - bath);   // the feeders top the bath back up
			bath += top;
			alumina -= top;
		}
		if (!noAlf3) {
			alf3 = Math.max(0, alf3 - al * Potline.alf3PerAl(treated));
		}
		metal += al;
		made = al / s;
		if (lithium > 1e-6) {   // 20c: its bath's lithium is used with the metal
			lithium = Math.max(0, lithium - al / 1000 * Tuning.POT_LI2CO3_PER_T_AL);
			if (lithium <= 1e-6) {
				dev.wildcatter.circuit.CircuitManager.markDirty(level);
			}
		}
		burned += al * Tuning.POT_CARBON_PER_AL;
		while (burned >= Potline.carbonPerAnode()) {
			burned -= Potline.carbonPerAnode();
			if (!slots.get(3).isEmpty() && putOut(new ItemStack(WildcatterItems.SPENT_ANODE))) {
				slots.get(3).shrink(1);
				changed++;
			} else {
				overdue = Math.min(Tuning.POT_ANODES, overdue + 1);
			}
		}
		StringBuilder notes = new StringBuilder();
		if (overdue > 0) {
			notes.append(String.format("; ANODE CHANGE OVERDUE (%d): put rodded anodes in its hopper", overdue));
		}
		if (noAlf3) {
			notes.append("; NO AlF3: its bath is drifting, put AlF3 bags in its hopper");
		}
		if (!treated) {
			notes.append("; VENTING (no duct): pipe its row's duct end to a fume treatment centre");
		}
		if (!fed) {
			notes.append(String.format("; NO AUX POWER (%s at the front of its main cell): its point feeders and crust breakers are stopped, %.0f kg of alumina left in its bath",
					Voltage.INDUSTRIAL.label(), bath));
		}
		status = String.format("%.2f V, %.0f kA, %.0f %%: %.1f kg/h of aluminium%s", cell.volts(), amps / 1000, ce * 100, made * 3600, notes);
	}

	private double anodeHours() {
		double carbonKgs = Potline.aluminiumKgs(Math.max(1, cell.amps()), Tuning.POT_CURRENT_EFFICIENCY) * Tuning.POT_CARBON_PER_AL;
		return (Potline.carbonPerAnode() - burned) / carbonKgs / 3600;
	}

	private static String hours(double seconds) {
		int min = (int) Math.ceil(seconds / 60);
		return min >= 60 ? String.format("%d h %d min", min / 60, min % 60) : min + " min";
	}

	// ---- the tapping crucible and the game test -----------------------------------------------------------------------------
	/** Siphons its metal into the crucible, as far as it holds. */
	public Component tap(ItemStack crucible) {
		double room = Tuning.CRUCIBLE_KG - TappingCrucibleItem.kg(crucible);
		double kg = Math.max(0, Math.min(metal, room));
		if (kg <= 0) {
			return Component.literal(metal <= 0 ? "Pot: no metal to tap yet" : "Tapping crucible: FULL; pour it into a casting machine");
		}
		metal -= kg;
		TappingCrucibleItem.setKg(crucible, TappingCrucibleItem.kg(crucible) + kg);
		setChanged();
		return Component.literal(String.format("Tapped %,.0f kg of aluminium: the crucible holds %,.0f of %,.0f kg", kg, TappingCrucibleItem.kg(crucible), Tuning.CRUCIBLE_KG));
	}

	public double metal() {
		return metal;
	}

	public boolean frozen() {
		return frozen;
	}

	public boolean cutOut() {
		return cutOut;
	}

	/**
	 * Sneak-right-click: its shorting bars in or out. Cut out, the line's current goes past it through the bars, live, as a frozen or
	 * relined pot is taken out of a running line; a frozen pot cannot be cut back in.
	 */
	public Component toggleCutOut() {
		if (cutOut && frozen) {
			return Component.literal("Pot: FROZEN: it stays cut out; break it for its spent pot lining");
		}
		cutOut = !cutOut;
		setChanged();
		if (level != null) {
			CircuitManager.markDirty(level);
		}
		return Component.literal(cutOut ? "Pot: CUT OUT: its shorting bars carry the line past it" : "Pot: CUT IN: its shorting bars are out, the line runs through it");
	}

	public double bath() {
		return bath;
	}

	public boolean effect() {
		return effect;
	}

	public int changed() {
		return changed;
	}

	/** For the game test: metal as if made. */
	public void addMetal(double kg) {
		metal = Math.min(Tuning.POT_METAL_HOLD_KG, metal + kg);
		setChanged();
	}

	/** For the game test: an anode nearly due. */
	public void burnCarbon(double kg) {
		burned += kg;
		setChanged();
	}

	/** For the game test: its alumina taken out, as if its feeders had run the bin dry. */
	public void emptyAlumina() {
		alumina = 0;
		bath = 0;
		slots.get(0).setCount(0);
		slots.get(1).setCount(0);
		setChanged();
	}

	/** For the game test: most of the two minutes an anode effect takes to clear. */
	public void addClearing(double seconds) {
		clearing += seconds;
		setChanged();
	}

	/** For the game test: hours without current, as if the line had been down. */
	public void addCold(double seconds) {
		cold += seconds;
		setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Pot (180 kA prebake): %s; metal %,.0f of %,.0f kg; alumina %,.0f kg and %d bags, AlF3 %,.0f kg and %d bags; %d anodes, next change in %.0f h, %d rodded anodes waiting, %d changed; auxiliaries %s",
				status, metal, Tuning.POT_METAL_HOLD_KG, alumina, slots.get(0).getCount() + slots.get(1).getCount(), alf3, slots.get(2).getCount(), Tuning.POT_ANODES,
				anodeHours(), slots.get(3).getCount(), changed, aux.describe()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("alumina", alumina);
		output.putDouble("alf3", alf3);
		output.putDouble("lithium", lithium);
		output.putDouble("metal", metal);
		output.putDouble("burned", burned);
		output.putInt("overdue", overdue);
		output.putBoolean("effect", effect);
		output.putDouble("clearing", clearing);
		output.putDouble("cf4", cf4);
		output.putDouble("cold", cold);
		output.putBoolean("frozen", frozen);
		output.putDouble("bath", bath);
		output.putBoolean("cutOut", cutOut);
		output.putInt("changed", changed);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		alumina = input.getDoubleOr("alumina", Tuning.POT_ALUMINA_BIN_KG);
		alf3 = input.getDoubleOr("alf3", Tuning.POT_ALF3_BIN_KG);
		lithium = input.getDoubleOr("lithium", 0);
		metal = input.getDoubleOr("metal", 0);
		burned = input.getDoubleOr("burned", 0);
		overdue = input.getIntOr("overdue", 0);
		effect = input.getBooleanOr("effect", false);
		clearing = input.getDoubleOr("clearing", 0);
		cf4 = input.getDoubleOr("cf4", 0);
		cold = input.getDoubleOr("cold", 0);
		frozen = input.getBooleanOr("frozen", false);
		bath = input.getDoubleOr("bath", Tuning.POT_BATH_ALUMINA_KG);
		cutOut = input.getBooleanOr("cutOut", false);
		changed = input.getIntOr("changed", 0);
	}
}
