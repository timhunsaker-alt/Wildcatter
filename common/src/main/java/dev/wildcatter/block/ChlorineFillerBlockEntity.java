package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The chlorine filler (docs/multiblocks/alumina-plant.md): a chlorine liquefier (refrigeration 30 kW at 480 V) and ton-container
 * filling, up to 360 kg an hour. Chlorine in at the north face of its front cells; empty ton containers by a hopper on top; full ones
 * (907 kg) out to a hopper under it, or to an empty hand.
 */
public class ChlorineFillerBlockEntity extends FillerBlockEntity {
	private double chlorine;
	private int filled;
	private String unit = "starting";

	public ChlorineFillerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CHLORINE_FILLER_ENTITY, pos, state);
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.EMPTY_TON_CONTAINER);
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	/** For the game test: liquid chlorine as if liquefied. */
	public void addChlorine(double kg) {
		chlorine += kg;
		setChanged();
	}

	@Override
	protected void work(Level level) {
		ItemStack full = new ItemStack(WildcatterItems.CHLORINE_TON_CONTAINER);
		if (chlorine >= Tuning.CL2_TON_KG && fed() > 0 && roomOut(full)) {
			useOne();
			chlorine -= Tuning.CL2_TON_KG;
			putOut(full);
			filled++;
		}
		double waiting = Lines.available(level, inlet(), Product.CHLORINE, Pull.MACHINE, this);
		double max = Math.min(Math.min(Tuning.CL2_FILLER_KGS * seconds(), waiting), 2 * Tuning.CL2_TON_KG - chlorine);
		if (waiting < 1e-4) {
			unit = String.format("idle: no chlorine at its front (north face); %.0f/%.0f kg liquid", chlorine, Tuning.CL2_TON_KG);
			return;
		}
		if (max <= 1e-7) {
			unit = fed() == 0 ? "NO CONTAINERS: put empty ton containers in its hopper" : "FULL: take the containers (empty hand, or a hopper under it)";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double got = Lines.pull(level, inlet(), Product.CHLORINE, max * strength, Pull.MACHINE, this);
		chlorine += got;
		unit = String.format("liquefying %.0f kg/h of chlorine; %.0f/%.0f kg toward a container%s", got / seconds() * 3600, chlorine, Tuning.CL2_TON_KG,
				fed() == 0 ? " (NO CONTAINERS in its hopper)" : "");
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Chlorine filler: %s; %d ton containers filled, %d waiting, %d empty; ", unit, filled, outCount(), fed()) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("chlorine", chlorine);
		output.putInt("filled", filled);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		chlorine = input.getDoubleOr("chlorine", 0);
		filled = input.getIntOr("filled", 0);
	}
}
