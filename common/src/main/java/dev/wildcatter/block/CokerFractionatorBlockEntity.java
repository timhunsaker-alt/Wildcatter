package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Carbon;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The coker fractionator (docs/multiblocks/delayed-coker.md): pumps 45 kW at 480 V. Residue in at the north face of its front cells
 * goes to its bottoms for the heater it touches; the drum vapour comes back and is split into coker gas oil (the diesel line, out of
 * the south face of its back cells at x 0), naphtha (the gasoline line, at x 2) and coker gas (the east face of its bottom cells).
 */
public class CokerFractionatorBlockEntity extends UnitMotorBlockEntity {
	private double feed;   // kg of residue in its bottoms for the heater
	private String unit = "starting";

	public CokerFractionatorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.COKER_FRACTIONATOR_ENTITY, pos, state);
	}

	private static double rate() {
		return Tuning.COKER_RESIDUE_KGS * Tuning.CARBON_CATCH_UP;
	}

	/** What it makes a kg of residue, in its pipe unit. */
	static double perResidue(Product product) {
		return switch (product) {
			case DIESEL -> Carbon.gasOilLitres();
			case GASOLINE -> Carbon.naphthaLitres();
			case COKER_GAS -> Carbon.cokerGas();
			default -> 0;
		};
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		if (at[1] != 0) {
			return null;
		}
		if (side == Direction.SOUTH && at[2] == 2) {
			return at[0] == 0 ? Product.DIESEL : at[0] == 2 ? Product.GASOLINE : null;
		}
		return side == Direction.EAST && at[0] == 2 ? Product.COKER_GAS : null;
	}

	@Override
	protected double hold(Product product) {
		return perResidue(product) * rate() * Tuning.PLANT_HOLD_SECONDS;
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	/** kg of residue in its bottoms, for the heater. */
	public double bottoms() {
		return feed;
	}

	public void takeFeed(double kg) {
		feed = Math.max(0, feed - kg);
		setChanged();
	}

	/** kg of residue whose products would fit at its nozzles. */
	public double room() {
		double max = Double.MAX_VALUE;
		for (Product p : new Product[] {Product.DIESEL, Product.GASOLINE, Product.COKER_GAS}) {
			max = Math.min(max, roomFor(p) / perResidue(p));
		}
		return max;
	}

	@Override
	protected void work(Level level) {
		double room = rate() * Tuning.PLANT_HOLD_SECONDS - feed;
		double offered = Lines.available(level, inlet(), Product.RESIDUE, Pull.MACHINE, this) * Tuning.RESIDUE_KG_PER_L;
		if (offered < 0.01) {
			unit = feed > 0.5 ? String.format("%.0f kg of residue in its bottoms; none more at its front", feed) : "NO RESIDUE: pipe residue to its front (north face)";
			return;
		}
		if (room <= 0.01) {
			unit = "idle: its bottoms are full (is the heater running?)";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double kg = Lines.pull(level, inlet(), Product.RESIDUE, Math.min(Math.min(rate() * seconds(), room), offered) * strength / Tuning.RESIDUE_KG_PER_L, Pull.MACHINE, this)
				* Tuning.RESIDUE_KG_PER_L;
		feed += kg;
		unit = String.format("fractionating: %.2f kg/s of residue in", kg / seconds());
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Coker fractionator: %s; %.0f kg of residue in its bottoms", unit, feed) + heldReadout() + "; " + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("feed", feed);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		feed = input.getDoubleOr("feed", 0);
	}
}
