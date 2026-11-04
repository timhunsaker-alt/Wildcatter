package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.item.TappingCrucibleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The cast house's ingot casting machine (docs/multiblocks/potline.md): a tilting pour box over a chain of moulds, a water spray and
 * a stacker, 15 kW at 480 V. It holds what the tapping crucible pours, up to 8 t, and casts it at 10 t/h into 22.7 kg ingots, out to
 * a hopper under it or to an empty hand.
 */
public class CastingMachineBlockEntity extends FillerBlockEntity {
	private double metal;
	private double poured;   // kg toward the next ingot
	private int cast;
	private String unit = "starting";

	public CastingMachineBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CASTING_MACHINE_ENTITY, pos, state, 0);
	}

	@Override
	public boolean takes(ItemStack stack) {
		return false;
	}

	public double metal() {
		return metal;
	}

	public int cast() {
		return cast;
	}

	/** Pours the crucible's metal in, as far as it has room. */
	public Component pour(ItemStack crucible) {
		double kg = Math.max(0, Math.min(TappingCrucibleItem.kg(crucible), Tuning.CASTER_HOLD_KG - metal));
		if (kg <= 0) {
			return Component.literal(TappingCrucibleItem.kg(crucible) <= 0 ? "Tapping crucible: empty; tap a pot with it" : "Casting machine: FULL of metal");
		}
		metal += kg;
		TappingCrucibleItem.setKg(crucible, TappingCrucibleItem.kg(crucible) - kg);
		setChanged();
		return Component.literal(String.format("Poured %,.0f kg into the casting machine: it holds %,.0f kg", kg, metal));
	}

	/** For the game test: an ingot's worth all but poured. */
	public void addPoured(double kg) {
		poured += kg;
		setChanged();
	}

	@Override
	protected void work(Level level) {
		ItemStack ingot = new ItemStack(WildcatterItems.ALUMINIUM_INGOT);
		if (metal <= 1e-6) {
			unit = "NO METAL: pour a tapping crucible into it (right-click with it)";
			return;
		}
		if (!roomOut(ingot)) {
			unit = "FULL: take the ingots (empty hand, or a hopper under it)";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double kg = Math.min(metal, Tuning.CASTER_KGS * seconds() * strength);
		metal -= kg;
		poured += kg;
		while (poured >= Tuning.AL_INGOT_KG && roomOut(ingot)) {
			poured -= Tuning.AL_INGOT_KG;
			putOut(new ItemStack(WildcatterItems.ALUMINIUM_INGOT));
			cast++;
		}
		unit = String.format("casting %,.0f kg/h", kg / seconds() * 3600);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Casting machine: %s; %,.0f kg of metal; %d ingots cast, %d waiting; ", unit, metal, cast, outCount()) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("metal", metal);
		output.putDouble("poured", poured);
		output.putInt("cast", cast);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		metal = input.getDoubleOr("metal", 0);
		poured = input.getDoubleOr("poured", 0);
		cast = input.getIntOr("cast", 0);
	}
}
