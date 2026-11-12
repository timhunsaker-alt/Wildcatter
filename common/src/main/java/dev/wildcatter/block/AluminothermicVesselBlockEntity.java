package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Ferroalloys.Batch;
import dev.wildcatter.fluid.Product;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The aluminothermic vessel (docs/multiblocks/ferroalloys.md): a refractory-lined steel vessel in a sand bed, under a hood to a baghouse
 * (its fan 15 kW at LV, box west). A batch by a hopper on top: a bag of moly oxide, 4 iron ingots and 20 aluminium ingots for
 * ferromolybdenum, or a bag of scheelite concentrate, an iron ingot and 8 aluminium ingots for ferrotungsten. Sneak and right-click fires
 * it: the oxide and the aluminium react by their own heat, in 10 minutes, to a button of alloy and a block of alumina slag. Sparks and fume
 * the while; a player inside its hood takes fire damage. The alloy goes to the bagging station against it in 1 t bags.
 */
public class AluminothermicVesselBlockEntity extends UnitMotorBlockEntity implements HopperFed, Bagged {
	private static final int MOLY = 0, SCHEELITE = 1, IRON = 2, ALUMINIUM = 3, CR2O3 = 4, CHROMIUM_OUT = 5;   // 17c2: chromic oxide in, chromium out below
	private final NonNullList<ItemStack> slots = NonNullList.withSize(6, ItemStack.EMPTY);
	private double chromium;   // 17c2: kg toward the next piece
	private final double[] alloy = new double[Batch.values().length];   // kg waiting for bags
	private @Nullable Batch firing;
	private double fireLeft;   // s
	private int batches;
	private double slag;       // t
	private String unit = "idle: load a batch by a hopper on top, then sneak and right-click to fire it";

	public AluminothermicVesselBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.ALUMINOTHERMIC_VESSEL_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	private static Item itemOf(int slot) {
		return switch (slot) {
			case MOLY -> WildcatterItems.MOLY_OXIDE_BAG;
			case SCHEELITE -> WildcatterItems.SCHEELITE_CONCENTRATE_BAG;
			case IRON -> Items.IRON_INGOT;
			case CR2O3 -> WildcatterItems.CHROMIC_OXIDE;
			case CHROMIUM_OUT -> Items.AIR;
			default -> WildcatterItems.ALUMINIUM_INGOT;
		};
	}

	@Override
	public boolean takes(ItemStack stack) {
		for (int i = 0; i < CHROMIUM_OUT; i++) {
			if (stack.is(itemOf(i))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {MOLY, SCHEELITE, IRON, ALUMINIUM, CR2O3} : side == Direction.DOWN ? new int[] {CHROMIUM_OUT} : new int[0];
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot >= 0 && slot < CHROMIUM_OUT && stack.is(itemOf(slot));
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == CHROMIUM_OUT && side == Direction.DOWN;
	}

	public int chromiumPieces() {
		return slots.get(CHROMIUM_OUT).getCount();
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side == Direction.UP && canPlaceItem(slot, stack);
	}

	/** The batch its bins hold a full charge for, ferromolybdenum first; null if neither. */
	private @Nullable Batch ready() {
		if (slots.get(MOLY).getCount() >= 1 && slots.get(IRON).getCount() >= Batch.FERROMOLY.iron && slots.get(ALUMINIUM).getCount() >= Batch.FERROMOLY.aluminium) {
			return Batch.FERROMOLY;
		}
		if (slots.get(SCHEELITE).getCount() >= 1 && slots.get(IRON).getCount() >= Batch.FERROTUNGSTEN.iron && slots.get(ALUMINIUM).getCount() >= Batch.FERROTUNGSTEN.aluminium) {
			return Batch.FERROTUNGSTEN;
		}
		if (slots.get(CR2O3).getCount() >= Tuning.VESSEL_CR_SACKS && slots.get(ALUMINIUM).getCount() >= Batch.CHROMIUM.aluminium) {
			return Batch.CHROMIUM;
		}
		return null;
	}

	public boolean firing() {
		return firing != null;
	}

	public double alloy(Batch b) {
		return alloy[b.ordinal()];
	}

	/** Sneak and right-click: fires a full batch. */
	public String fire(Level level) {
		if (firing != null) {
			return String.format("Aluminothermic vessel: already firing %s, %.0f s left", firing.label, fireLeft);
		}
		Batch batch = ready();
		if (batch == null) {
			return "Aluminothermic vessel: NO BATCH: a bag of moly oxide, " + Batch.FERROMOLY.iron + " iron and " + Batch.FERROMOLY.aluminium
					+ " aluminium ingots; or a bag of scheelite concentrate, " + Batch.FERROTUNGSTEN.iron + " iron and " + Batch.FERROTUNGSTEN.aluminium + " aluminium ingots; or "
					+ Tuning.VESSEL_CR_SACKS + " sacks of chromic oxide and " + Batch.CHROMIUM.aluminium + " aluminium ingots";
		}
		slots.get(batch == Batch.FERROMOLY ? MOLY : batch == Batch.CHROMIUM ? CR2O3 : SCHEELITE)
				.shrink(batch == Batch.CHROMIUM ? Tuning.VESSEL_CR_SACKS : 1);
		slots.get(IRON).shrink(batch.iron);
		slots.get(ALUMINIUM).shrink(batch.aluminium);
		firing = batch;
		fireLeft = Tuning.VESSEL_FIRE_SECONDS;
		setChanged();
		if (level instanceof ServerLevel server) {
			server.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.5F, 0.6F);
		}
		return "Aluminothermic vessel: fired " + batch.label;
	}

	/** For the game test: the firing's 10 minutes skipped; the next interval finishes it. */
	public void testFinishFiring() {
		if (firing != null) {
			fireLeft = 0.001;
		}
	}

	@Override
	protected void work(Level level) {
		if (firing == null) {
			Batch ready = ready();
			unit = ready == null ? "idle: load a batch by a hopper on top, then sneak and right-click to fire it"
					: "READY: a batch of " + ready.label + " loaded; sneak and right-click to fire it";
			return;
		}
		double strength = drive(level);   // the baghouse fan runs the while
		if (level instanceof ServerLevel server) {
			show(server);
		}
		fireLeft -= seconds();
		if (fireLeft > 0) {
			unit = String.format("FIRING %s: %.0f s left%s", firing.label, fireLeft, strength <= 0 ? "; NO FAN: its fume goes nowhere (its " + describe() + ")" : "");
			return;
		}
		if (firing == Batch.CHROMIUM) {
			chromium += firing.alloyKg();   // broken from its button into 10 kg pieces, out to a hopper under it
			int n = (int) Math.floor(chromium / Tuning.CHROMIUM_PIECE_KG + 1e-9);
			ItemStack out = slots.get(CHROMIUM_OUT);
			n = Math.min(n, out.isEmpty() ? 64 : out.getMaxStackSize() - out.getCount());
			if (n > 0) {
				if (out.isEmpty()) {
					slots.set(CHROMIUM_OUT, new ItemStack(WildcatterItems.CHROMIUM_METAL, n));
				} else {
					out.grow(n);
				}
				chromium -= n * Tuning.CHROMIUM_PIECE_KG;
			}
		} else {
			alloy[firing.ordinal()] += firing.alloyKg();
		}
		slag += firing.slagKg() / 1000;
		batches++;
		unit = String.format("fired: %.0f kg of %s and %.0f kg of alumina slag", firing.alloyKg(), firing.label, firing.slagKg());
		firing = null;
		fireLeft = 0;
		setChanged();
	}

	/** Sparks and fume out of the vessel, and fire inside its hood. */
	private void show(ServerLevel server) {
		BlockPos centre = LeasePartBlock.at(worldPosition, getBlockState(), 1, 1, 1);
		double x = centre.getX() + 0.5, y = centre.getY() + 0.2, z = centre.getZ() + 0.5;
		server.sendParticles(ParticleTypes.LAVA, x, y, z, 3, 0.4, 0.2, 0.4, 0.0);
		server.sendParticles(ParticleTypes.FLAME, x, y, z, 8, 0.5, 0.3, 0.5, 0.05);
		server.sendParticles(ParticleTypes.FIREWORK, x, y + 0.3, z, 6, 0.3, 0.3, 0.3, 0.15);
		server.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 1.2, z, 3, 0.4, 0.3, 0.4, 0.02);
		if (server.getGameTime() % 20 == 0) {
			server.playSound(null, centre, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1.2F, 0.6F);
		}
		AABB hood = AABB.encapsulatingFullBlocks(LeasePartBlock.at(worldPosition, getBlockState(), 0, 0, 0), LeasePartBlock.at(worldPosition, getBlockState(), 2, 2, 2));
		for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, hood)) {
			e.igniteForSeconds(4);
			e.hurtServer(server, server.damageSources().inFire(), 2.0F);
		}
	}

	@Override
	public Item bagItem() {
		return alloy[Batch.FERROTUNGSTEN.ordinal()] > alloy[Batch.FERROMOLY.ordinal()] ? WildcatterItems.FERROTUNGSTEN_BAG : WildcatterItems.FERROMOLYBDENUM_BAG;
	}

	private int bagIndex() {
		return alloy[Batch.FERROTUNGSTEN.ordinal()] > alloy[Batch.FERROMOLY.ordinal()] ? Batch.FERROTUNGSTEN.ordinal() : Batch.FERROMOLY.ordinal();
	}

	@Override
	public double bagReady() {
		return alloy[bagIndex()];
	}

	@Override
	public double takeForBag(double kg) {
		int i = bagIndex();
		double got = Math.min(kg, alloy[i]);
		alloy[i] -= got;
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
	public Component inspect() {
		return Component.literal(String.format("Aluminothermic vessel (baghouse fan %.0f kW, %s): ", Tuning.VESSEL_FAN_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit
				+ String.format("; bins: %d moly oxide, %d scheelite concentrate, %d iron, %d aluminium, %d chromic oxide; %.0f kg of ferromolybdenum and %.0f kg of ferrotungsten for bags; %d pieces of chromium for a hopper; %d batches, %.1f t of slag; ",
						slots.get(MOLY).getCount(), slots.get(SCHEELITE).getCount(), slots.get(IRON).getCount(), slots.get(ALUMINIUM).getCount(), slots.get(CR2O3).getCount(),
						alloy[Batch.FERROMOLY.ordinal()], alloy[Batch.FERROTUNGSTEN.ordinal()], chromiumPieces(), batches, slag)
				+ describe());
	}

	@Override
	public List<Point> points() {
		return RatePoints.POINTS;
	}

	@Override
	public boolean readPoint(int point) {
		return RatePoints.read(point, firing != null, unitRemote.auto());
	}

	@Override
	public double readMa(int point) {
		return firing == null ? 4 : 4 + 16 * Math.max(0, Math.min(1, 1 - fireLeft / Tuning.VESSEL_FIRE_SECONDS));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		for (Batch b : Batch.values()) {
			output.putDouble("alloy_" + b.name(), alloy[b.ordinal()]);
		}
		output.putString("firing", firing == null ? "" : firing.name());
		output.putDouble("fireLeft", fireLeft);
		output.putInt("batches", batches);
		output.putDouble("slag", slag);
		output.putDouble("chromium", chromium);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		for (Batch b : Batch.values()) {
			alloy[b.ordinal()] = input.getDoubleOr("alloy_" + b.name(), 0);
		}
		chromium = input.getDoubleOr("chromium", 0);
		String f = input.getStringOr("firing", "");
		firing = null;
		for (Batch b : Batch.values()) {
			if (b.name().equals(f)) {
				firing = b;
			}
		}
		fireLeft = input.getDoubleOr("fireLeft", 0);
		batches = input.getIntOr("batches", 0);
		slag = input.getDoubleOr("slag", 0);
	}
}
