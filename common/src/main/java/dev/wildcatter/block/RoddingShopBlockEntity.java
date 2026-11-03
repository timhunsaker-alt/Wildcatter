package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The rodding shop (docs/multiblocks/anode-plant.md): the butt stripper and cleaning station, the butt crusher, the rod line and an
 * induction furnace for the cast iron, 200 kW at 480 V. By a hopper on top: baked anodes (slot 0), anode rods (1), iron ingots (2)
 * and spent anodes (3). An anode a minute, it strips a spent anode into its rod (back on the rod line) and crushed butts, or casts a
 * baked anode onto a rod with an iron ingot's thimbles. Rodded anodes and crushed butts go out to a hopper under it, or to an empty
 * hand.
 */
public class RoddingShopBlockEntity extends FillerBlockEntity {
	private static final Item[] INS = {WildcatterItems.BAKED_ANODE, WildcatterItems.ANODE_ROD, Items.IRON_INGOT, WildcatterItems.SPENT_ANODE};
	private double timer;   // seconds toward the next anode
	private int rodded;
	private int stripped;
	private String unit = "starting";

	public RoddingShopBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.RODDING_SHOP_ENTITY, pos, state, INS.length);
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(INS[0]);
	}

	@Override
	protected boolean takesIn(int slot, ItemStack stack) {
		return slot < INS.length && stack.is(INS[slot]);
	}

	private ItemStack in(int slot) {
		return slots().get(slot);
	}

	@Override
	protected void work(Level level) {
		boolean strip = !in(3).isEmpty() && roomOut(new ItemStack(WildcatterItems.CRUSHED_BUTTS));
		boolean rod = !in(0).isEmpty() && !in(1).isEmpty() && !in(2).isEmpty() && roomOut(new ItemStack(WildcatterItems.RODDED_ANODE));
		if (!strip && !rod) {
			timer = 0;
			unit = in(3).isEmpty() && in(0).isEmpty() ? "idle: no anodes in its hopper"
					: !in(3).isEmpty() || in(0).isEmpty() ? "FULL: take the crushed butts (empty hand, or a hopper under it)"
					: in(1).isEmpty() ? "NO RODS: put anode rods (or spent anodes) in its hopper"
					: in(2).isEmpty() ? "NO IRON: put iron ingots in its hopper, one a rodding" : "FULL: take the anodes (empty hand, or a hopper under it)";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		timer += seconds() * strength;
		if (timer < Tuning.RODDING_SECONDS) {
			unit = String.format("%s: %.0f/%.0f s", strip ? "stripping a spent anode" : "rodding an anode", timer, Tuning.RODDING_SECONDS);
			return;
		}
		timer = 0;
		if (strip) {
			in(3).shrink(1);
			putOut(new ItemStack(WildcatterItems.CRUSHED_BUTTS));
			ItemStack rods = in(1);
			if (rods.isEmpty()) {
				slots().set(1, new ItemStack(WildcatterItems.ANODE_ROD));
			} else if (rods.getCount() < rods.getMaxStackSize()) {
				rods.grow(1);
			} else {
				putOut(new ItemStack(WildcatterItems.ANODE_ROD));
			}
			stripped++;
			unit = "stripped a spent anode: its rod back on the line, its butt crushed";
		} else {
			in(0).shrink(1);
			in(1).shrink(1);
			in(2).shrink(1);
			putOut(new ItemStack(WildcatterItems.RODDED_ANODE));
			rodded++;
			unit = "rodded an anode";
		}
		setChanged();
	}

	/** For the game test: the minute an anode takes, mostly done. */
	public void addWork(double s) {
		timer += s;
		setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Rodding shop: %s; %d baked anodes, %d rods, %d iron, %d spent anodes in; %d rodded, %d stripped, %d waiting; ", unit,
				in(0).getCount(), in(1).getCount(), in(2).getCount(), in(3).getCount(), rodded, stripped, outCount()) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("timer", timer);
		output.putInt("rodded", rodded);
		output.putInt("stripped", stripped);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		timer = input.getDoubleOr("timer", 0);
		rodded = input.getIntOr("rodded", 0);
		stripped = input.getIntOr("stripped", 0);
	}
}
