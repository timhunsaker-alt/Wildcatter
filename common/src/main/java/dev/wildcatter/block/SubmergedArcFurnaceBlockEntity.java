package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.fluid.Ferroalloys;
import dev.wildcatter.fluid.Ferroalloys.Charge;
import dev.wildcatter.fluid.Ferroalloys.Recipe;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The submerged arc furnace (docs/multiblocks/ferroalloys.md): a round shell about 7 m across, three Soderberg electrodes buried in the
 * charge, the charging chutes and a fume hood. It forms with a furnace transformer against its west face, which feeds it and is its load
 * on the grid: 8 MW at PF 0.80 at full tap. Ore, coke and flux by a hopper on top, each to its bin; Soderberg paste (green anodes from
 * the anode plant) to a bin of its own, which the electrodes burn at 20 kg a tonne. It smelts its recipe (HC ferrochrome, HC
 * ferromanganese or ferrosilicon 75) at the recipe's MWh a tonne into its hearth, and taps every 2 hours, or when the hearth is full,
 * at the taphole on its east face into the bagging station there; slag is counted. Sneak and right-click changes the recipe: the hearth is
 * run out first, an hour at power. Four hours without power and the hearth freezes; a frozen furnace is dug out (broken and placed again).
 */
public class SubmergedArcFurnaceBlockEntity extends PlantUnitBlockEntity implements HopperFed, Bagged {
	/** Its bins: one for each charge, then the paste. */
	private static final int PASTE = Charge.values().length;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(PASTE + 1, ItemStack.EMPTY);
	private final double[] stock = new double[Charge.values().length];   // kg of each charge in its bins' chutes
	private final double[] tapped = new double[Recipe.values().length];  // kg of each alloy tapped, waiting for bags
	private Recipe recipe = Recipe.FECR;
	private @Nullable Recipe changingTo;
	private double changeLeft;      // s of running left in a recipe change
	private double hearth;          // kg of alloy in the hearth
	private double paste = Tuning.SAF_ELECTRODE_PASTE_KG;   // kg of paste in the electrode columns
	private double sinceTap;        // s
	private double lastTapKg;
	private double unpowered;       // s without power since it last ran
	private boolean everRan;
	private boolean frozen;
	private boolean want;           // asks its transformer for power
	private double made;            // t of alloy, all told
	private double slag;            // t of slag, all told
	private double tapShow;         // s of the tap's fume and sparks left
	private double watts;
	private double vars;
	private double making;          // kg/h, the last interval
	private boolean tapWasCommanded;
	private boolean tapRequested;
	private double setpointMa;
	private long setpointTick = -1000;

	public SubmergedArcFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.SUBMERGED_ARC_FURNACE_ENTITY, pos, state);
	}

	// ---- bins -----------------------------------------------------------------------------------------------------------------------
	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	private static Item itemOf(int slot) {
		if (slot == PASTE) {
			return WildcatterItems.GREEN_ANODE;
		}
		return switch (Charge.values()[slot]) {
			case CHROMITE -> OreBlocks.MINERALS.get(Ore.CHROMITE);
			case PYROLUSITE -> OreBlocks.MINERALS.get(Ore.PYROLUSITE);
			case QUARTZ -> Items.QUARTZ;
			case LIMESTONE -> OreBlocks.MINERALS.get(Ore.LIMESTONE);
			case COKE -> WildcatterItems.MET_COKE;
			case IRON -> Items.IRON_INGOT;
		};
	}

	@Override
	public boolean takes(ItemStack stack) {
		for (int i = 0; i < slots.size(); i++) {
			if (stack.is(itemOf(i))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		if (side != Direction.UP) {
			return new int[0];
		}
		int[] all = new int[slots.size()];
		for (int i = 0; i < all.length; i++) {
			all[i] = i;
		}
		return all;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot >= 0 && slot < slots.size() && stack.is(itemOf(slot));
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side == Direction.UP && canPlaceItem(slot, stack);
	}

	/** kg of {@code charge} it has, in its bin's chute and its bin. */
	private double have(Charge charge) {
		return stock[charge.ordinal()] + slots.get(charge.ordinal()).getCount() * charge.itemKg();
	}

	/** Takes {@code kg} of {@code charge}, opening items from its bin as it needs them. */
	private void use(Charge charge, double kg) {
		int i = charge.ordinal();
		while (stock[i] < kg && !slots.get(i).isEmpty()) {
			slots.get(i).shrink(1);
			stock[i] += charge.itemKg();
		}
		stock[i] = Math.max(0, stock[i] - kg);
		setChanged();
	}

	// ---- the transformer and the grid ----------------------------------------------------------------------------------------------
	public @Nullable FurnaceTransformerBlockEntity transformer() {
		return level == null ? null : Touching.find(level, worldPosition, FurnaceTransformerBlockEntity.class, t -> true);
	}

	/** What it asks its transformer for, given the tap's power: nothing while it is held or has no charge; half with its electrodes short. */
	public double wantedWatts(double tapWatts) {
		if (!want || frozen) {
			return 0;
		}
		return tapWatts * (shortElectrodes() ? Tuning.SAF_SHORT_POWER : 1.0);
	}

	public boolean shortElectrodes() {
		return paste <= 0;
	}

	public Recipe recipe() {
		return recipe;
	}

	public double watts() {
		return watts;
	}

	public double vars() {
		return vars;
	}

	public double hearth() {
		return hearth;
	}

	public double tapped(Recipe r) {
		return tapped[r.ordinal()];
	}

	public boolean frozen() {
		return frozen;
	}

	/** kg/h of alloy the last interval. */
	public double making() {
		return making;
	}

	/** Sneak and right-click: the next recipe. The hearth is run out first. */
	public String nextRecipe() {
		Recipe next = (changingTo == null ? recipe : changingTo).next();
		if (next == recipe) {
			changingTo = null;
			changeLeft = 0;
		} else {
			changingTo = next;
			changeLeft = Tuning.SAF_CHANGEOVER_SECONDS;
		}
		setChanged();
		return changingTo == null ? "Submerged arc furnace: staying on " + recipe.label
				: String.format("Submerged arc furnace: changing to %s; the hearth is run out first, %.0f minutes at power", next.label, changeLeft / 60);
	}

	// ---- game test hooks ----------------------------------------------------------------------------------------------------------
	/** The hearth brought to {@code kg} of the recipe's alloy, as if smelted. */
	public void testFillHearth(double kg) {
		hearth = kg;
		setChanged();
	}

	/** A recipe change's hour skipped: the next interval finishes it. */
	public void testSkipChangeover() {
		if (changingTo != null) {
			changeLeft = 0.001;
		}
	}

	/** Sets the recipe at once, the hearth emptied (for a scene that starts on one). */
	public void testRecipe(Recipe r) {
		recipe = r;
		changingTo = null;
		changeLeft = 0;
		hearth = 0;
		setChanged();
	}

	/** The paste in the electrode columns burned away, as after a long run with an empty paste bin. */
	public void testBurnElectrodes() {
		paste = 0;
		setChanged();
	}

	/** {@code seconds} without power, as if time had passed. */
	public void testUnpowered(double seconds) {
		unpowered += seconds;
		setChanged();
	}

	// ---- work ----------------------------------------------------------------------------------------------------------------------
	public static void tick(Level level, BlockPos pos, BlockState state, SubmergedArcFurnaceBlockEntity f) {
		if (level.isClientSide()) {
			return;
		}
		if (f.tapShow > 0 && level instanceof ServerLevel server && level.getGameTime() % 2 == 0) {
			f.showTap(server);
		}
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		f.work(level);
		f.setChanged();
	}

	private void work(Level level) {
		double dt = seconds();
		making = 0;
		tapShow = Math.max(0, tapShow - dt);
		FurnaceTransformerBlockEntity tx = transformer();
		watts = tx == null ? 0 : tx.load().watts();
		vars = tx == null ? 0 : tx.load().vars();
		if (watts > Tuning.SAF_WATTS * 0.02) {
			everRan = true;
			unpowered = 0;
		} else if (everRan && hearth > 0) {
			unpowered += dt;
		}
		if (everRan && hearth > 0 && unpowered >= Tuning.SAF_FREEZE_SECONDS) {
			frozen = true;
		}
		if (frozen) {
			want = false;
			status = "FROZEN: its hearth froze without power; dig it out (break it and place it again)";
			return;
		}
		if (tx == null) {
			want = false;
			status = "NOT BUILT: NO TRANSFORMER: set a furnace transformer against its west face";
			return;
		}
		// the PLC: a power set point steps the tap; TAP taps at once
		if (unitRemote.auto() && level.getGameTime() - setpointTick <= dev.wildcatter.block.Starter.COMMAND_STALE_TICKS && setpointMa >= 3.6) {
			tx.setTap(Ferroalloys.tapFor(Math.max(0, Math.min(1, (setpointMa - 4) / 16)) * Tuning.SAF_WATTS));
		}
		sinceTap += dt;
		if (plcHold(level)) {
			want = false;
			return;
		}
		if (changingTo != null) {
			want = true;
			if (watts > 0) {
				changeLeft -= dt * Math.min(1, watts / Ferroalloys.tapWatts(tx.tap()));
			}
			if (changeLeft <= 0) {
				tap(true);
				recipe = changingTo;
				changingTo = null;
				changeLeft = 0;
			} else {
				status = String.format("CHANGING TO %s: running the hearth out, %.0f minutes at power left", changingTo.label, changeLeft / 60);
				return;
			}
		}
		// its charge: enough of each for an interval at its tap's power
		double nominalKg = Ferroalloys.tapWatts(tx.tap()) * dt / recipe.joulesPerKg();
		for (Charge c : Charge.values()) {
			double need = nominalKg * recipe.kgPerTonne(c) / 1000;
			if (need > 0 && have(c) < need) {
				want = false;
				status = recipe.lack(c);
				return;
			}
		}
		if (hearth >= Tuning.SAF_HEARTH_KG && !roomToTap()) {
			want = false;
			status = "HEARTH FULL: no room to tap; set a bagging station against its east face (taphole) and give it bags";
			return;
		}
		want = true;
		// it smelts what its power melts
		double kg = Math.max(0, watts) * dt / recipe.joulesPerKg();
		if (kg > 0) {
			for (Charge c : Charge.values()) {
				double need = kg * recipe.kgPerTonne(c) / 1000;
				if (need > 0) {
					use(c, need);
				}
			}
			hearth += kg;
			made += kg / 1000;
			slag += kg / 1000 * recipe.slag;
			paste = Math.max(0, paste - kg / 1000 * Tuning.SAF_PASTE_KG_PER_T);
			making = kg / dt * 3600;
		}
		// paste: a green anode broken into the columns whenever there is room for one
		while (paste <= Tuning.SAF_ELECTRODE_PASTE_KG - Tuning.GREEN_ANODE_KG && !slots.get(PASTE).isEmpty()) {
			slots.get(PASTE).shrink(1);
			paste += Tuning.GREEN_ANODE_KG;
		}
		if ((sinceTap >= Tuning.SAF_TAP_SECONDS || hearth >= Tuning.SAF_HEARTH_KG || tapRequested) && hearth > 0 && roomToTap()) {
			tap(false);
		}
		tapRequested = false;
		String electrodes = shortElectrodes() ? "; ELECTRODES SHORT: put green anodes (Soderberg paste) in a hopper on its top" : "";
		status = watts < 1000 ? "starting: waiting for power from its transformer" + electrodes
				: String.format("smelting %s: %.2f t/h, the hearth %.1f t, the next tap in %.0f min", recipe.label, making / 1000, hearth / 1000,
						Math.max(0, Tuning.SAF_TAP_SECONDS - sinceTap) / 60) + electrodes;
	}

	private boolean roomToTap() {
		double waiting = 0;
		for (double t : tapped) {
			waiting += t;
		}
		return waiting < Tuning.SAF_TAPPED_HOLD_KG;
	}

	/** Opens the taphole: the hearth's alloy runs into the ladles for the bagging station. */
	private void tap(boolean runOut) {
		if (hearth <= 0) {
			sinceTap = 0;
			return;
		}
		tapped[recipe.ordinal()] += hearth;
		lastTapKg = hearth;
		hearth = 0;
		sinceTap = 0;
		tapShow = Tuning.SAF_TAP_SHOW_SECONDS;
		if (level instanceof ServerLevel server) {
			BlockPos hole = taphole();
			server.playSound(null, hole, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 1.5F, 0.7F);
			server.playSound(null, hole, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.6F);
		}
		setChanged();
	}

	/** The taphole: the east face of the shell, at its middle. */
	private BlockPos taphole() {
		return LeasePartBlock.at(worldPosition, getBlockState(), 6, 0, 3);
	}

	private void showTap(ServerLevel server) {
		BlockPos hole = taphole();
		Direction east = LeasePartBlock.world(getBlockState(), Direction.EAST);
		double x = hole.getX() + 0.5 + east.getStepX() * 0.6, y = hole.getY() + 0.6, z = hole.getZ() + 0.5 + east.getStepZ() * 0.6;
		server.sendParticles(ParticleTypes.LAVA, x, y, z, 4, 0.15, 0.1, 0.15, 0.0);
		server.sendParticles(ParticleTypes.FLAME, x, y, z, 6, 0.2, 0.15, 0.2, 0.02);
		server.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.6, z, 3, 0.3, 0.3, 0.3, 0.02);
		if (server.getGameTime() % 10 == 0) {
			server.playSound(null, hole, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 0.8F, 0.8F);
		}
	}

	// ---- the bagging station at its taphole ------------------------------------------------------------------------------------
	private Recipe bagRecipe() {
		Recipe best = recipe;
		for (Recipe r : Recipe.values()) {
			if (tapped[r.ordinal()] > tapped[best.ordinal()]) {
				best = r;
			}
		}
		return best;
	}

	public static Item bagFor(Recipe r) {
		return switch (r) {
			case FECR -> WildcatterItems.FERROCHROME_BAG;
			case FEMN -> WildcatterItems.FERROMANGANESE_BAG;
			case FESI -> WildcatterItems.FERROSILICON_BAG;
		};
	}

	@Override
	public Item bagItem() {
		return bagFor(bagRecipe());
	}

	@Override
	public double bagReady() {
		return tapped[bagRecipe().ordinal()];
	}

	@Override
	public double takeForBag(double kg) {
		int i = bagRecipe().ordinal();
		double got = Math.min(kg, tapped[i]);
		tapped[i] -= got;
		setChanged();
		return got;
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
	public double measure(Quantity quantity) {
		return quantity == Quantity.LEVEL ? hearth / Tuning.SAF_HEARTH_KG * 100 : Double.NaN;
	}

	// ---- PLC points: RUN, TAP, the power set point; MW, Mvar ------------------------------------------------------------------------
	@Override
	public List<Point> points() {
		return List.of(new Point("RUN", true), new Point("TAP", true), new Point("POWER SP 4-20 mA", true, Signal.MA), new Point("MW 4-20 mA", false, Signal.MA),
				new Point("MVAR 4-20 mA", false, Signal.MA), new Point("RUNNING", false), new Point("AUTO", false));
	}

	@Override
	public boolean readPoint(int point) {
		return point == 5 ? watts > 1000 : point == 6 && unitRemote.auto();
	}

	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
		if (point == 0) {
			unitRemote.command(0, on, gameTime);
		} else if (point == 1) {
			if (on && !tapWasCommanded && unitRemote.auto()) {
				tapRequested = true;
			}
			tapWasCommanded = on;
		}
	}

	@Override
	public void commandMa(int point, double milliamps, long gameTime) {
		if (point == 2) {
			setpointMa = milliamps;
			setpointTick = gameTime;
		}
	}

	@Override
	public double readMa(int point) {
		return switch (point) {
			case 3 -> 4 + 16 * Math.min(1.03, Math.max(0, watts / Tuning.SAF_WATTS));
			case 4 -> 4 + 16 * Math.min(1.03, Math.max(0, vars / Ferroalloys.vars(Tuning.SAF_WATTS, Tuning.SAF_POWER_FACTOR)));
			default -> 0;
		};
	}

	// ---- readout ------------------------------------------------------------------------------------------------------------------
	@Override
	public Component inspect() {
		FurnaceTransformerBlockEntity tx = transformer();
		StringBuilder bins = new StringBuilder();
		for (Charge c : Charge.values()) {
			if (recipe.kgPerTonne(c) > 0) {
				bins.append(String.format(", %s %.1f t", c.label, have(c) / 1000));
			}
		}
		String power = tx == null ? "" : String.format("; tap %d of %d, %s", tx.tap(), Tuning.FURNACE_TAPS, tx.metering());
		String last = lastTapKg > 0 ? String.format("; last tap %.1f t", lastTapKg / 1000) : "";
		String waiting = "";
		for (Recipe r : Recipe.values()) {
			if (tapped[r.ordinal()] >= 1) {
				waiting += String.format(", %.1f t of %s for bags", tapped[r.ordinal()] / 1000, r.shortName);
			}
		}
		return Component.literal(String.format("Submerged arc furnace (%s, %s): ", recipe.label, recipe.grade) + status + power
				+ String.format("; hearth %.1f of %.0f t%s%s; paste %.0f kg in the electrodes, %d green anodes%s; %.1f t made, %.1f t of slag",
						hearth / 1000, Tuning.SAF_HEARTH_KG / 1000, last, waiting, paste, slots.get(PASTE).getCount(), bins, made, slag)
				+ "; sneak and right-click changes its recipe");
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		for (Charge c : Charge.values()) {
			output.putDouble("stock_" + c.name(), stock[c.ordinal()]);
		}
		for (Recipe r : Recipe.values()) {
			output.putDouble("tapped_" + r.name(), tapped[r.ordinal()]);
		}
		output.putString("recipe", recipe.name());
		output.putString("changingTo", changingTo == null ? "" : changingTo.name());
		output.putDouble("changeLeft", changeLeft);
		output.putDouble("hearth", hearth);
		output.putDouble("paste", paste);
		output.putDouble("sinceTap", sinceTap);
		output.putDouble("lastTap", lastTapKg);
		output.putDouble("unpowered", unpowered);
		output.putBoolean("everRan", everRan);
		output.putBoolean("frozen", frozen);
		output.putDouble("made", made);
		output.putDouble("slag", slag);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		for (Charge c : Charge.values()) {
			stock[c.ordinal()] = input.getDoubleOr("stock_" + c.name(), 0);
		}
		for (Recipe r : Recipe.values()) {
			tapped[r.ordinal()] = input.getDoubleOr("tapped_" + r.name(), 0);
		}
		recipe = parse(input.getStringOr("recipe", Recipe.FECR.name()), Recipe.FECR);
		String to = input.getStringOr("changingTo", "");
		changingTo = to.isEmpty() ? null : parse(to, null);
		changeLeft = input.getDoubleOr("changeLeft", 0);
		hearth = input.getDoubleOr("hearth", 0);
		paste = input.getDoubleOr("paste", Tuning.SAF_ELECTRODE_PASTE_KG);
		sinceTap = input.getDoubleOr("sinceTap", 0);
		lastTapKg = input.getDoubleOr("lastTap", 0);
		unpowered = input.getDoubleOr("unpowered", 0);
		everRan = input.getBooleanOr("everRan", false);
		frozen = input.getBooleanOr("frozen", false);
		made = input.getDoubleOr("made", 0);
		slag = input.getDoubleOr("slag", 0);
	}

	private static @Nullable Recipe parse(String name, @Nullable Recipe fallback) {
		for (Recipe r : Recipe.values()) {
			if (r.name().equals(name)) {
				return r;
			}
		}
		return fallback;
	}
}
