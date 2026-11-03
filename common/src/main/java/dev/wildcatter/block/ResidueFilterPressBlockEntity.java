package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Bayer;
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
 * The residue filter press (docs/multiblocks/bayer-refinery.md): a plate-and-frame press, hydraulics 30 kW at 480 V. Washed red mud
 * slurry in at the north face of its front cells is pressed to 70 % solids cake; each 1,800 kg goes out as a block of red mud, to a
 * hopper under it or an empty hand, for a dry stack.
 */
public class ResidueFilterPressBlockEntity extends FillerBlockEntity {
	private double cake;     // kg of cake waiting for a block
	private double stacked;  // blocks made
	private String unit = "starting";

	public ResidueFilterPressBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.RESIDUE_FILTER_PRESS_ENTITY, pos, state);
	}

	@Override
	public boolean takes(ItemStack stack) {
		return false;
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	/** kg of cake a kg of mud slurry: the solids stay, the liquor is pressed out. */
	private static double cakePerSlurry() {
		return Bayer.cake() / Bayer.mudSlurry();
	}

	@Override
	protected void work(Level level) {
		while (cake >= Tuning.RED_MUD_BLOCK_KG && putOut(new ItemStack(WildcatterBlocks.RED_MUD))) {
			cake -= Tuning.RED_MUD_BLOCK_KG;
			stacked++;
		}
		if (cake >= Tuning.RED_MUD_BLOCK_KG) {
			unit = "FULL: take the red mud (empty hand, or a hopper under it)";
			return;
		}
		double waiting = Lines.available(level, inlet(), Product.MUD_SLURRY, Pull.MACHINE, this);
		if (waiting < 0.01) {
			unit = String.format("idle: no red mud slurry at its front (north face); %.0f/%.0f kg of cake", cake, Tuning.RED_MUD_BLOCK_KG);
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double max = Math.min(Tuning.BAYER_ALUMINA_KGS * Tuning.BAYER_CATCH_UP * Bayer.mudSlurry() * seconds(), waiting) * strength;
		double got = Lines.pull(level, inlet(), Product.MUD_SLURRY, max, Pull.MACHINE, this);
		cake += got * cakePerSlurry();
		unit = String.format("pressing %.2f kg/s of mud to %.2f kg/s of cake; %.0f/%.0f kg toward a block", got / seconds(), got * cakePerSlurry() / seconds(), cake,
				Tuning.RED_MUD_BLOCK_KG);
	}

	/** For the game test: cake as if pressed. */
	public void addCake(double kg) {
		cake += kg;
		setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Residue filter press: %s; %.0f blocks of red mud made, %d waiting; ", unit, stacked, outCount()) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("cake", cake);
		output.putDouble("stacked", stacked);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		cake = input.getDoubleOr("cake", 0);
		stacked = input.getDoubleOr("stacked", 0);
	}
}
