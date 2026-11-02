package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.multiblock.BigPartEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A storage tank (docs/multiblocks/lease.md, gas-plant.md): the 400 bbl (64,000 L) steel stock tank holds one liquid product, set by
 * its first fill (crude, condensate, gasoline, kerosene, diesel, residue or natural gasoline); the fiberglass water tank holds produced water; the 18,000 gallon LPG bullet
 * holds 58,000 L of LPG. Pipe joins the side faces of its bottom layer. It fills from the process outlets on those runs (never from
 * another tank) and gives to whatever draws on them after the outlets are drawn.
 */
public class StorageTankBlockEntity extends BlockEntity implements Inspectable, BigPartEntity, FluidNode {
	private @Nullable Product product;
	private double amount;
	private boolean grown;

	public StorageTankBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.STORAGE_TANK_ENTITY, pos, state);
	}

	private boolean water() {
		return getBlockState().getBlock() instanceof LeasePartBlock part && part.part() == LeasePartBlock.Part.WATER_TANK;
	}

	private boolean bullet() {
		return getBlockState().getBlock() instanceof GasPlantPartBlock;
	}

	/** What it may hold. */
	public boolean accepts(Product p) {
		return water() ? p == Product.PRODUCED_WATER : bullet() ? p == Product.LPG
				: p.tanked() && p != Product.PRODUCED_WATER && p != Product.LPG && p != Product.WELL_FLUID && p != Product.Y_GRADE;
	}

	public double capacity() {
		return bullet() ? Tuning.LPG_BULLET_LITERS : Tuning.STOCK_TANK_LITERS;
	}

	/** Game test hook. */
	public void testFill(Product p, double litres) {
		add(p, litres);
	}

	public double amount() {
		return amount;
	}

	public @Nullable Product product() {
		return product;
	}

	private List<Lines.Face> ring() {
		List<Lines.Face> out = new java.util.ArrayList<>();
		for (Direction side : Direction.Plane.HORIZONTAL) {
			out.addAll(LeasePartBlock.faces(worldPosition, getBlockState(), at -> at[1] == 0, side));
		}
		return out;
	}

	private void add(Product p, double litres) {
		if (litres > 0) {
			product = p;
			amount = Math.min(capacity(), amount + litres);
			setChanged();
		}
	}

	private double remove(double litres) {
		double got = Math.min(litres, amount);
		amount -= got;
		if (amount < 0.5) {
			amount = 0;
			product = water() ? Product.PRODUCED_WATER : null;
		}
		setChanged();
		return got;
	}

	// ---- on its pipes -------------------------------------------------------------------------------------------------------
	@Override
	public boolean tank() {
		return true;
	}

	@Override
	public @Nullable Product offers(BlockPos cell, Direction side) {
		return amount >= 0.5 && side.getAxis().isHorizontal() && LeasePartBlock.local(worldPosition, getBlockState(), cell)[1] == 0 ? product : null;
	}

	@Override
	public double available(BlockPos cell, Direction side, Product p, Pull pull) {
		return p == product && pull == Pull.MACHINE ? amount : 0;
	}

	@Override
	public double take(BlockPos cell, Direction side, Product p, double max, Pull pull) {
		return available(cell, side, p, pull) > 0 ? remove(max) : 0;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, StorageTankBlockEntity tank) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0 || tank.amount >= tank.capacity()) {
			return;
		}
		for (Lines.Line line : Lines.lines(level, tank.ring())) {
			Product p = line.run().product();
			if (p != null && tank.accepts(p) && (tank.product == null || tank.product == p || tank.amount < 0.5)) {
				tank.add(p, Lines.pull(line, p, tank.capacity() - tank.amount, Pull.TANK, tank));
			}
		}
	}

	/** A bucket on the tank: an empty one fills with crude or diesel, a full one pours in. True if it was a bucket. */
	public boolean bucket(ItemStack stack, Player player, InteractionHand hand) {
		if (level == null) {
			return false;
		}
		ItemStack reward = null;
		if (stack.is(Items.BUCKET)) {
			if (level.isClientSide()) {
				return true;
			}
			if (product == Product.CRUDE && amount >= Tuning.CRUDE_BUCKET_UNITS) {
				remove(Tuning.CRUDE_BUCKET_UNITS);
				reward = new ItemStack(WildcatterItems.CRUDE_OIL_BUCKET);
			} else if (product == Product.DIESEL && amount >= Tuning.DIESEL_BUCKET_UNITS) {
				remove(Tuning.DIESEL_BUCKET_UNITS);
				reward = new ItemStack(WildcatterItems.DIESEL_BUCKET);
			} else {
				player.sendOverlayMessage(water() ? Component.literal("Produced water is brine: it goes down a disposal well, not into a bucket") : inspect());
				return true;
			}
		} else {
			Product poured = stack.is(WildcatterItems.CRUDE_OIL_BUCKET) ? Product.CRUDE : stack.is(WildcatterItems.DIESEL_BUCKET) ? Product.DIESEL : null;
			if (poured == null) {
				return false;
			}
			if (level.isClientSide()) {
				return true;
			}
			if (!accepts(poured) || (product != null && product != poured && amount >= 0.5) || amount + 1000 > capacity()) {
				player.sendOverlayMessage(inspect());
				return true;
			}
			add(poured, 1000);
			reward = new ItemStack(Items.BUCKET);
		}
		stack.shrink(1);
		if (stack.isEmpty()) {
			player.setItemInHand(hand, reward);
		} else if (!player.getInventory().add(reward)) {
			net.minecraft.world.Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), reward);
		}
		return true;
	}

	@Override
	public Component inspect() {
		String kind = water() ? "Water tank (400 bbl fiberglass, 64,000 L)" : bullet() ? "LPG bullet (18,000 gal, 250 psi, 58,000 L)" : "Stock tank (400 bbl API 12F, 64,000 L)";
		String holds = amount < 0.5 ? "empty" : String.format("%s %,.0f L (%.0f%%)", product == null ? "?" : product.label(), amount, amount / capacity() * 100);
		return Component.literal(kind + ": " + holds + (amount >= capacity() ? ", FULL" : ""));
	}

	@Override
	public boolean grown() {
		return grown;
	}

	@Override
	public void markGrown() {
		grown = true;
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putString("product", product == null ? "" : product.name());
		output.putDouble("amount", amount);
		output.putBoolean("grown", grown);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		String name = input.getStringOr("product", "");
		product = name.isEmpty() ? null : Product.valueOf(name);
		amount = input.getDoubleOr("amount", 0);
		grown = input.getBooleanOr("grown", grown);
	}
}
