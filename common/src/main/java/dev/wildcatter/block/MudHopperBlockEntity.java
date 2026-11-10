package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import net.minecraft.core.BlockPos;
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
 * The rig's jet mixing hopper (docs/multiblocks/drilling-mud.md): a hopper and venturi on a centrifugal mixing pump, 37 kW at 480 V,
 * set against a land rig's mud tanks. Bags of barite or bentonite, or rock salt, go in by a hopper on top; while its pump runs it
 * mixes a tonne a minute (a bag a minute; a block of rock salt is about two) into the tanks, and the rig's mud weight follows.
 */
public class MudHopperBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private RigSubstructureBlockEntity.Solid mixing;
	private double left;    // kg of the batch in hand still to mix
	private double mixed;   // kg mixed in all
	private String unit = "starting";

	public MudHopperBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.MUD_HOPPER_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return solid(stack) != null;
	}

	private static RigSubstructureBlockEntity.@Nullable Solid solid(ItemStack stack) {
		return stack.is(WildcatterItems.BARITE_BAG) ? RigSubstructureBlockEntity.Solid.BARITE : stack.is(WildcatterItems.BENTONITE_BAG) ? RigSubstructureBlockEntity.Solid.BENTONITE
				: stack.is(OreBlocks.MINERALS.get(Ore.HALITE)) ? RigSubstructureBlockEntity.Solid.SALT : null;
	}

	/** The land rig whose mud tanks it stands against, or null. */
	public @Nullable RigSubstructureBlockEntity rig() {
		if (level == null) {
			return null;
		}
		RigPartBlockEntity tanks = Touching.find(level, worldPosition, RigPartBlockEntity.class, p -> p.part() == RigPartBlock.Part.MUD_TANKS);
		return tanks == null ? null : tanks.rig();
	}

	public double mixed() {
		return mixed;
	}

	@Override
	protected void work(Level level) {
		RigSubstructureBlockEntity rig = rig();
		if (rig == null) {
			unit = "NOT AT A RIG: set it against a land rig's mud tanks";
			return;
		}
		if (left <= 1e-6 && fed() > 0) {
			ItemStack one = useOne();
			mixing = solid(one);
			left = mixing == RigSubstructureBlockEntity.Solid.SALT ? Ore.HALITE.kgPerBlock() : Tuning.BULK_BAG_KG;
		}
		if (left <= 1e-6 || mixing == null) {
			left = 0;
			unit = "NO BAGS: put a hopper of barite, bentonite or rock salt on top";
			return;
		}
		double room = rig.roomFor(mixing);
		if (room <= 1e-6) {
			unit = "TANKS FULL: drill or let the mud down before more " + mixing.label();
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double kg = Math.min(Math.min(left, room), Tuning.MUD_HOPPER_KGS * seconds() * strength);
		kg = rig.addSolid(mixing, kg);
		left -= kg;
		mixed += kg;
		unit = String.format("mixing %s: %,.0f kg of the batch left; mud %s", mixing.label(), left, rig.mudText());
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Mud hopper (jet mixing): %s; %d in its hopper, %.1f t mixed; ", unit, fed(), mixed / 1000) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putInt("mixing", mixing == null ? -1 : mixing.ordinal());
		output.putDouble("left", left);
		output.putDouble("mixed", mixed);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		int m = input.getIntOr("mixing", -1);
		mixing = m < 0 || m >= RigSubstructureBlockEntity.Solid.values().length ? null : RigSubstructureBlockEntity.Solid.values()[m];
		left = input.getDoubleOr("left", 0);
		mixed = input.getDoubleOr("mixed", 0);
	}
}
