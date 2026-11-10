package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Bayer;
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
 * A pipe-to-pipe part of the Bayer refinery (docs/multiblocks/bayer-refinery.md): the ball mill, digester, flash tanks, settler, mud
 * washer, polishing filters, precipitator, evaporator and package boiler. Each takes what its inlet run (the north face of its front
 * cells) offers, up to the plant rate with a fifth more to catch up, while its motor runs, and holds its output at the south face of
 * its back cells (the settler's and washer's mud at the east face). Steam, gas and caustic come in on the east face of its bottom
 * cells. Every stream carries its alumina in a fixed share ({@link Bayer}), so the sums are in kg of alumina's worth.
 */
public class BayerStageBlockEntity extends UnitMotorBlockEntity implements HopperFed, WaterUser {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double bauxite;   // kg the ball mill has tipped in
	private double water;     // litres the washer or the boiler holds
	private double reserve;   // kg of strong liquor in the evaporator's storage
	private boolean charged;
	/** With the world rule realResidenceTimes: kg of alumina's worth in process (the digesters 1 h in minute buckets, the precipitators 36 h in half hours). */
	private final dev.wildcatter.fluid.Holdup holdup;
	private String unit = "starting";

	public BayerStageBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.BAYER_STAGE_ENTITY, pos, state);
		holdup = new dev.wildcatter.fluid.Holdup(state.getBlock() instanceof AluminaPartBlock b && b.part() == AluminaPartBlock.Part.DIGESTER ? 60 : 1800);
	}

	public AluminaPartBlock.Part part() {
		return getBlockState().getBlock() instanceof AluminaPartBlock block ? block.part() : AluminaPartBlock.Part.BALL_MILL;
	}

	private static double rate() {
		return Tuning.BAYER_ALUMINA_KGS * Tuning.BAYER_CATCH_UP;
	}

	/** kg of a stream a kg of alumina: what it holds is about 5 s of that at the full rate. */
	static double perAlumina(Product product) {
		return switch (product) {
			case BAUXITE_SLURRY, DIGESTED_SLURRY -> Bayer.bauxiteSlurry();
			case GREEN_LIQUOR, POLISHED_LIQUOR, HYDRATE_SLURRY -> Bayer.polishedLiquor();
			case MUD_SLURRY -> Bayer.mudSlurry();
			case SPENT_LIQUOR -> Bayer.spentLiquor();
			case STRONG_LIQUOR -> Bayer.strongLiquor();
			case PROCESS_STEAM -> Bayer.steam();
			default -> 1;
		};
	}

	private @Nullable Product out() {
		return switch (part()) {
			case BALL_MILL -> Product.BAUXITE_SLURRY;
			case FLASH_TANKS -> Product.DIGESTED_SLURRY;
			case SETTLER, MUD_WASHER -> Product.GREEN_LIQUOR;
			case POLISHING_FILTER -> Product.POLISHED_LIQUOR;
			case PRECIPITATOR -> Product.HYDRATE_SLURRY;
			case EVAPORATOR -> Product.STRONG_LIQUOR;
			case PACKAGE_BOILER -> Product.PROCESS_STEAM;
			default -> null;
		};
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		AluminaPartBlock.Part p = part();
		if (at[1] != 0) {
			return null;
		}
		if (side == Direction.SOUTH && at[2] == p.l - 1) {
			return out();
		}
		if (side == Direction.EAST && at[0] == p.w - 1 && (p == AluminaPartBlock.Part.SETTLER || p == AluminaPartBlock.Part.MUD_WASHER)) {
			return Product.MUD_SLURRY;
		}
		return null;
	}

	@Override
	protected double hold(Product product) {
		if (part() == AluminaPartBlock.Part.PACKAGE_BOILER) {
			return Tuning.PROCESS_BOILER_KGS * Tuning.PLANT_HOLD_SECONDS;
		}
		return perAlumina(product) * rate() * Tuning.PLANT_HOLD_SECONDS;
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	private List<Lines.Face> east() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	// ---- bauxite and water --------------------------------------------------------------------------------------------------------
	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return part() == AluminaPartBlock.Part.BALL_MILL && stack.is(OreBlocks.MINERALS.get(Ore.BAUXITE));
	}

	private boolean watered() {
		return part() == AluminaPartBlock.Part.MUD_WASHER || part() == AluminaPartBlock.Part.PACKAGE_BOILER;
	}

	@Override
	public double waterLevel() {
		return watered() ? water / Tuning.BAYER_WATER_HOLD : 1;
	}

	@Override
	public int addWater(int litres) {
		int fits = watered() ? (int) Math.max(0, Math.min(litres, Tuning.BAYER_WATER_HOLD - water)) : 0;
		water += fits;
		setChanged();
		return fits;
	}

	/** The evaporator's storage, for a readout and the game test. */
	public double reserve() {
		return reserve;
	}

	// ---- the work ---------------------------------------------------------------------------------------------------------------
	@Override
	protected void work(Level level) {
		switch (part()) {
			case BALL_MILL -> mill(level);
			case DIGESTER -> {
				if (dev.wildcatter.Rules.realResidenceTimes() || !holdup.isEmpty()) {
					heldDigester(level);
				} else {
					digester(level);
				}
			}
			case FLASH_TANKS -> unit = held(Product.DIGESTED_SLURRY) > 0.5 ? String.format("flashing to atmospheric: %.0f kg held", held(Product.DIGESTED_SLURRY))
					: "idle: no slurry from the digester it touches";
			case SETTLER -> pass(level, Product.DIGESTED_SLURRY, Bayer.bauxiteSlurry(), 0, new Product[] {Product.GREEN_LIQUOR, Product.MUD_SLURRY},
					new double[] {Bayer.greenLiquor(), Bayer.mudSlurry()}, "settling");
			case MUD_WASHER -> pass(level, Product.MUD_SLURRY, Bayer.mudSlurry(), Bayer.washWater(), new Product[] {Product.GREEN_LIQUOR, Product.MUD_SLURRY},
					new double[] {Bayer.washWater(), Bayer.mudSlurry()}, "washing");
			case POLISHING_FILTER -> pass(level, Product.GREEN_LIQUOR, Bayer.polishedLiquor(), 0, new Product[] {Product.POLISHED_LIQUOR},
					new double[] {Bayer.polishedLiquor()}, "filtering");
			case PRECIPITATOR -> {
				if (dev.wildcatter.Rules.realResidenceTimes() || !holdup.isEmpty()) {
					heldPrecipitator(level);
				} else {
					pass(level, Product.POLISHED_LIQUOR, Bayer.polishedLiquor(), 0, new Product[] {Product.HYDRATE_SLURRY},
							new double[] {Bayer.polishedLiquor()}, "precipitating");
				}
			}
			case EVAPORATOR -> evaporator(level);
			case PACKAGE_BOILER -> boiler(level);
			default -> {
			}
		}
	}

	private void mill(Level level) {
		double max = rate() * seconds();
		if (bauxite < max * Bayer.bauxite() && fed() > 0) {
			useOne();
			bauxite += Ore.BAUXITE.blockKg();
		}
		double liquor = Lines.available(level, inlet(), Product.STRONG_LIQUOR, Pull.MACHINE, this);
		if (bauxite < 0.01) {
			unit = "NO BAUXITE: put a hopper of bauxite on it";
			return;
		}
		if (liquor < 0.01) {
			unit = "NO LIQUOR: pipe strong liquor from the evaporator to its front (north face)";
			return;
		}
		max = Math.min(Math.min(max, bauxite / Bayer.bauxite()), Math.min(liquor / Bayer.strongLiquor(), room(Product.BAUXITE_SLURRY) / Bayer.bauxiteSlurry()));
		if (max <= 1e-7) {
			unit = "idle: its slurry has nowhere to go (pipe its back end to the digester)";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double a = Lines.pull(level, inlet(), Product.STRONG_LIQUOR, max * strength * Bayer.strongLiquor(), Pull.MACHINE, this) / Bayer.strongLiquor();
		bauxite = Math.max(0, bauxite - a * Bayer.bauxite());
		add(Product.BAUXITE_SLURRY, a * Bayer.bauxiteSlurry());
		unit = String.format("grinding %.2f kg/s of bauxite in %.1f kg/s of strong liquor", a * Bayer.bauxite() / seconds(), a * Bayer.strongLiquor() / seconds());
	}

	private void digester(Level level) {
		BayerStageBlockEntity flash = Touching.find(level, worldPosition, BayerStageBlockEntity.class, b -> b.part() == AluminaPartBlock.Part.FLASH_TANKS);
		if (flash == null) {
			unit = "NOT BUILT: set it against the flash tanks";
			return;
		}
		double slurry = Lines.available(level, inlet(), Product.BAUXITE_SLURRY, Pull.MACHINE, this);
		if (slurry < 0.01) {
			unit = "idle: no bauxite slurry at its front (north face)";
			return;
		}
		double steam = Lines.available(level, east(), Product.PROCESS_STEAM, Pull.MACHINE, this);
		if (steam < 0.001) {
			unit = "NO STEAM: pipe process steam from a package boiler to its east side";
			return;
		}
		double max = Math.min(Math.min(rate() * seconds(), slurry / Bayer.bauxiteSlurry()),
				Math.min(steam / Bayer.digesterSteam(), flash.roomFor(Product.DIGESTED_SLURRY) / Bayer.bauxiteSlurry()));
		if (max <= 1e-7) {
			unit = "idle: the flash tanks are full";
			return;
		}
		double strength = Math.min(drive(level), flash.drive(level));
		if (strength <= 0) {
			unit = "starting: " + describe() + "; flash pump " + flash.motor().describe();
			return;
		}
		double a = Lines.pull(level, inlet(), Product.BAUXITE_SLURRY, max * strength * Bayer.bauxiteSlurry(), Pull.MACHINE, this) / Bayer.bauxiteSlurry();
		Lines.pull(level, east(), Product.PROCESS_STEAM, a * Bayer.digesterSteam(), Pull.MACHINE, this);
		flash.give(Product.DIGESTED_SLURRY, a * Bayer.bauxiteSlurry());
		unit = String.format("digesting at 145 C: %.1f kg/s of slurry on %.2f kg/s of steam", a * Bayer.bauxiteSlurry() / seconds(), a * Bayer.digesterSteam() / seconds());
	}

	/** kg of a stream it holds to send on (for the game test). */
	public double holding(Product product) {
		return held(product);
	}

	/** kg of alumina's worth in process (the rule's hold). */
	public double inProcess() {
		return holdup.inProcess();
	}

	/** Game test hook: as if {@code seconds} had passed for what is in process. */
	public void ageForTest(double seconds) {
		holdup.age(seconds);
		setChanged();
	}

	private static String nextOut(dev.wildcatter.fluid.Holdup h, double now) {
		double next = h.untilNext(now);
		return next > 0 ? String.format("; the next out in %.1f h", next / 3600) : "";
	}

	/**
	 * The digester with the world rule realResidenceTimes (plant-crane-gaps #11): slurry and its steam go in at the rate, and each
	 * charge is digested for its real hour at 145 C before it goes to the flash tanks; it holds up to an hour's feed. With the rule
	 * turned off again, what it holds goes on as the flash tanks take it.
	 */
	private void heldDigester(Level level) {
		boolean hold = dev.wildcatter.Rules.realResidenceTimes();
		double now = level.getGameTime() / 20.0;
		BayerStageBlockEntity flash = Touching.find(level, worldPosition, BayerStageBlockEntity.class, b -> b.part() == AluminaPartBlock.Part.FLASH_TANKS);
		if (flash == null) {
			unit = "NOT BUILT: set it against the flash tanks";
			return;
		}
		double strength = Math.min(drive(level), flash.drive(level));
		if (strength <= 0) {
			unit = "starting: " + describe() + "; flash pump " + flash.motor().describe();
			return;
		}
		double room = flash.roomFor(Product.DIGESTED_SLURRY) / Bayer.bauxiteSlurry();
		double out = hold ? holdup.take(now, room) : holdup.takeAny(room);
		flash.give(Product.DIGESTED_SLURRY, out * Bayer.bauxiteSlurry());
		double a = 0;
		if (hold) {
			double slurry = Lines.available(level, inlet(), Product.BAUXITE_SLURRY, Pull.MACHINE, this);
			double steam = Lines.available(level, east(), Product.PROCESS_STEAM, Pull.MACHINE, this);
			double max = Math.min(Math.min(rate() * seconds(), slurry / Bayer.bauxiteSlurry()),
					Math.min(steam / Bayer.digesterSteam(), Tuning.BAYER_ALUMINA_KGS * Tuning.DIGESTION_HOLD_S - holdup.inProcess()));
			if (max > 1e-7) {
				a = Lines.pull(level, inlet(), Product.BAUXITE_SLURRY, max * strength * Bayer.bauxiteSlurry(), Pull.MACHINE, this) / Bayer.bauxiteSlurry();
				Lines.pull(level, east(), Product.PROCESS_STEAM, a * Bayer.digesterSteam(), Pull.MACHINE, this);
				holdup.add(now, Tuning.DIGESTION_HOLD_S, a);
			} else if (steam < 0.001 && slurry >= 0.01) {
				unit = "NO STEAM: pipe process steam from a package boiler to its east side; " + String.format("%,.0f kg of slurry digesting", holdup.inProcess() * Bayer.bauxiteSlurry());
				setChanged();
				return;
			}
		}
		unit = String.format("digesting at 145 C (1 h a charge): %,.0f kg of slurry in the digesters, %.1f kg/s in, %.1f kg/s on to the flash tanks",
				holdup.inProcess() * Bayer.bauxiteSlurry(), a * Bayer.bauxiteSlurry() / seconds(), out * Bayer.bauxiteSlurry() / seconds()) + nextOut(holdup, now);
		setChanged();
	}

	/**
	 * The precipitator train with the world rule realResidenceTimes: polished liquor in at the rate, its hydrate slurry out after the
	 * real 36 h of seeded precipitation; it holds up to 36 h of feed. With the rule turned off again, what it holds comes out.
	 */
	private void heldPrecipitator(Level level) {
		boolean hold = dev.wildcatter.Rules.realResidenceTimes();
		double now = level.getGameTime() / 20.0;
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double room = room(Product.HYDRATE_SLURRY) / Bayer.polishedLiquor();
		double out = hold ? holdup.take(now, room) : holdup.takeAny(room);
		add(Product.HYDRATE_SLURRY, out * Bayer.polishedLiquor());
		double a = 0;
		if (hold) {
			double waiting = Lines.available(level, inlet(), Product.POLISHED_LIQUOR, Pull.MACHINE, this) / Bayer.polishedLiquor();
			double max = Math.min(Math.min(rate() * seconds(), waiting), Tuning.BAYER_ALUMINA_KGS * Tuning.PRECIPITATION_HOLD_S - holdup.inProcess());
			if (max > 1e-7) {
				a = Lines.pull(level, inlet(), Product.POLISHED_LIQUOR, max * strength * Bayer.polishedLiquor(), Pull.MACHINE, this) / Bayer.polishedLiquor();
				holdup.add(now, Tuning.PRECIPITATION_HOLD_S, a);
			}
		}
		unit = String.format("precipitating (36 h): %,.0f kg of liquor in the train, %.1f kg/s in, %.1f kg/s of hydrate slurry out", holdup.inProcess() * Bayer.polishedLiquor(),
				a * Bayer.polishedLiquor() / seconds(), out * Bayer.polishedLiquor() / seconds()) + nextOut(holdup, now);
		setChanged();
	}

	/** One stream in, its products out by fixed shares; {@code waterPerA} litres of pumped water a kg of alumina's worth. */
	private void pass(Level level, Product in, double inPerA, double waterPerA, Product[] outs, double[] perA, String verb) {
		double waiting = Lines.available(level, inlet(), in, Pull.MACHINE, this) / inPerA;
		if (waiting < 1e-6) {
			unit = "idle: no " + in.label() + " at its front (north face)";
			return;
		}
		if (waterPerA > 0 && water < 1) {
			unit = "NO WATER: pump wash water to it";
			return;
		}
		double max = Math.min(rate() * seconds(), waiting);
		if (waterPerA > 0) {
			max = Math.min(max, water / waterPerA);
		}
		for (int i = 0; i < outs.length; i++) {
			max = Math.min(max, room(outs[i]) / perA[i]);
		}
		if (max <= 1e-7) {
			unit = "idle: its output has nowhere to go";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double a = Lines.pull(level, inlet(), in, max * strength * inPerA, Pull.MACHINE, this) / inPerA;
		water = Math.max(0, water - a * waterPerA);
		for (int i = 0; i < outs.length; i++) {
			add(outs[i], a * perA[i]);
		}
		unit = String.format("%s %.1f kg/s of %s", verb, a * inPerA / seconds(), in.label()) + (waterPerA > 0 ? String.format(" with %.1f L/s of water", a * waterPerA / seconds()) : "");
	}

	private void evaporator(Level level) {
		double charge = Bayer.strongLiquor() * Tuning.BAYER_ALUMINA_KGS * Tuning.BAYER_LIQUOR_CHARGE_SECONDS;
		if (!charged) {
			reserve = charge;   // the first fill, waived (docs/multiblocks/bayer-refinery.md)
			charged = true;
		}
		unit = evaporate(level, charge);
		double top = Math.min(reserve, room(Product.STRONG_LIQUOR));   // its storage tops up the mill while the loop fills
		if (top > 0) {
			add(Product.STRONG_LIQUOR, top);
			reserve -= top;
		}
	}

	private String evaporate(Level level, double charge) {
		double spent = Lines.available(level, inlet(), Product.SPENT_LIQUOR, Pull.MACHINE, this);
		if (spent < 0.01) {
			return "idle: no spent liquor at its front (north face)";
		}
		CausticizerBlockEntity causticizer = Touching.find(level, worldPosition, CausticizerBlockEntity.class, c -> true);
		if (causticizer == null) {
			return "NOT BUILT: set it against the causticizer";
		}
		double steam = Lines.available(level, east(), Product.PROCESS_STEAM, Pull.MACHINE, this);
		double caustic = Lines.available(level, east(), Product.CAUSTIC_SODA, Pull.MACHINE, this);
		if (steam < 0.001) {
			return "NO STEAM: pipe process steam from a package boiler to its east side";
		}
		if (caustic < 0.001) {
			return "NO CAUSTIC: pipe 32 % caustic soda to its east side, on its own run";
		}
		if (causticizer.lime() < 0.001) {
			return "NO LIME: put quicklime bags in the causticizer's hopper";
		}
		double spentPerA = Bayer.spentLiquor();
		double room = room(Product.STRONG_LIQUOR) + Math.max(0, charge - reserve);
		double max = Math.min(Math.min(rate() * seconds(), spent / spentPerA), room / Bayer.strongLiquor());
		max = Math.min(max, Math.min(steam / Bayer.evaporatorSteam(), Math.min(caustic / Bayer.caustic(), causticizer.lime() / Bayer.lime())));
		if (max <= 1e-7) {
			return "idle: its storage is full";
		}
		double strength = Math.min(drive(level), causticizer.drive(level));
		if (strength <= 0) {
			return "starting: " + describe() + "; causticizer " + causticizer.motor().describe();
		}
		double a = Lines.pull(level, inlet(), Product.SPENT_LIQUOR, max * strength * spentPerA, Pull.MACHINE, this) / spentPerA;
		Lines.pull(level, east(), Product.PROCESS_STEAM, a * Bayer.evaporatorSteam(), Pull.MACHINE, this);
		Lines.pull(level, east(), Product.CAUSTIC_SODA, a * Bayer.caustic(), Pull.MACHINE, this);
		causticizer.takeLime(a * Bayer.lime());
		double strong = a * Bayer.strongLiquor();
		double out = Math.min(strong, room(Product.STRONG_LIQUOR));
		add(Product.STRONG_LIQUOR, out);
		reserve += strong - out;   // what comes back over is banked
		double capA = rate() * seconds();
		String why = caustic / Bayer.caustic() < capA && caustic / Bayer.caustic() <= max * 1.001 ? " (short of caustic)"
				: causticizer.lime() / Bayer.lime() < capA && causticizer.lime() / Bayer.lime() <= max * 1.001 ? " (short of lime)"
				: steam / Bayer.evaporatorSteam() < capA && steam / Bayer.evaporatorSteam() <= max * 1.001 ? " (short of steam)" : "";
		return String.format("evaporating %.2f kg/s of water: %.1f kg/s of strong liquor, %.0f kg/h of caustic, %.0f kg/h of lime%s", a * Bayer.evaporated() / seconds(),
				strong / seconds(), a * Bayer.caustic() / seconds() * 3600, a * Bayer.lime() / seconds() * 3600, why);
	}

	private void boiler(Level level) {
		if (water < 1) {
			unit = "NO WATER: pump water to it";
			return;
		}
		double max = Math.min(Math.min(Tuning.PROCESS_BOILER_KGS * seconds(), room(Product.PROCESS_STEAM)), water);
		if (max <= 1e-7) {
			unit = "idle: nothing is drawing its steam";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double kg = max * strength;
		double need = kg * Bayer.boilerJoulesPerSteam();
		double got = GasFiring.fire(level, east(), need, this);
		kg *= Math.min(1, got / need);
		if (kg <= 1e-7) {
			unit = "NO FUEL: pipe gas or LPG to its east side";
			return;
		}
		water -= kg;
		add(Product.PROCESS_STEAM, kg);
		unit = String.format("firing: %.2f kg/s of 10 bar steam, %.0f kW of gas", kg / seconds(), got / seconds() / 1000);
	}

	private String label() {
		return switch (part()) {
			case BALL_MILL -> "Ball mill";
			case DIGESTER -> "Digester";
			case FLASH_TANKS -> "Flash tanks";
			case SETTLER -> "Settler";
			case MUD_WASHER -> "Mud washer";
			case POLISHING_FILTER -> "Polishing filters";
			case PRECIPITATOR -> "Precipitator";
			case EVAPORATOR -> "Evaporator";
			case PACKAGE_BOILER -> "Package boiler";
			default -> kind();
		};
	}

	@Override
	public Component inspect() {
		String extra = switch (part()) {
			case BALL_MILL -> String.format("; bauxite %,.0f kg and %d blocks", bauxite, fed());
			case MUD_WASHER, PACKAGE_BOILER -> String.format("; water %.0f/%.0f L", water, Tuning.BAYER_WATER_HOLD);
			case EVAPORATOR -> String.format("; storage %,.0f kg of strong liquor", reserve);
			default -> "";
		};
		return Component.literal(label() + ": " + unit + extra + heldReadout() + "; " + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("bauxite", bauxite);
		output.putDouble("water", water);
		output.putDouble("reserve", reserve);
		output.putBoolean("charged", charged);
		output.putString("holdup", holdup.save());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		bauxite = input.getDoubleOr("bauxite", 0);
		water = input.getDoubleOr("water", 0);
		reserve = input.getDoubleOr("reserve", 0);
		charged = input.getBooleanOr("charged", false);
		holdup.load(input.getStringOr("holdup", ""));
	}
}
