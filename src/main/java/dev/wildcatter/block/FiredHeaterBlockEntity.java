package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.multiblock.BigPart;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A crude unit's fired heater (docs/multiblocks/crude-units.md): the topping unit's vertical cylindrical heater (3 MMBtu/h) or the
 * crude unit's box heater (60 MMBtu/h). Its column asks it for heat; it burns gas or residue piped to its fuel nozzle (the topping
 * heater burns the unit's own LPG first). Gas at 38 MJ/m3, residue at 38 MJ/L.
 */
public class FiredHeaterBlockEntity extends PlantUnitBlockEntity {
	private double firing;
	private String fuel = "";

	public FiredHeaterBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.FIRED_HEATER_ENTITY, pos, state);
	}

	public boolean big() {
		return getBlockState().getBlock() instanceof BigPart part && part.shapeName().equals("crude_heater");
	}

	public double ratedWatts() {
		return big() ? Tuning.CRUDE_HEATER_WATTS : Tuning.TOPPING_HEATER_WATTS;
	}

	private List<Lines.Face> fuelFaces() {
		return big() ? faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH) : horizontalBottom();
	}

	private List<Lines.Face> horizontalBottom() {
		List<Lines.Face> out = new java.util.ArrayList<>();
		for (Direction side : Direction.Plane.HORIZONTAL) {
			out.addAll(faces(at -> at[1] == 0, side));
		}
		return out;
	}

	/** Joules in a unit of a fuel, or 0 if the heater does not burn it. */
	private static double joules(Product p) {
		return switch (p) {
			case GAS, PIPELINE_GAS, DRIED_GAS, COMPRESSED_GAS -> Tuning.GAS_JOULES_PER_M3;
			case RESIDUE -> Tuning.FUEL_OIL_JOULES_PER_L;
			case LPG -> Tuning.LPG_JOULES_PER_L;
			default -> 0;
		};
	}

	/** Burns fuel off its nozzle for up to {@code wanted} joules; returns the joules it got. */
	public double burn(Level level, double wanted, double seconds) {
		double got = 0;
		fuel = "";
		for (Lines.Line line : Lines.lines(level, fuelFaces())) {
			Product p = line.run().product();
			double per = p == null ? 0 : joules(p);
			if (per <= 0 || got >= wanted) {
				continue;
			}
			got += Lines.pull(line, p, (wanted - got) / per, Pull.MACHINE, this) * per;
			fuel = p.label();
		}
		firing = seconds > 0 ? got / seconds / ratedWatts() : 0;
		setChanged();
		return got;
	}

	/** Its own LPG burned first (the topping unit's offgas): what it is credited with this interval. */
	public void credit(double joules, double wantedJoules, double seconds) {
		firing = seconds > 0 ? Math.min(1, (joules + firing * ratedWatts() * seconds) / seconds / ratedWatts()) : 0;
		if (joules > 0) {
			fuel = fuel.isEmpty() ? "its own LPG" : "its own LPG and " + fuel;
		}
	}

	public void idle() {
		firing = 0;
		fuel = "";
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
		String name = big() ? "Crude heater (box, 60 MMBtu/h)" : "Topping heater (vertical cylindrical, 3 MMBtu/h)";
		return Component.literal(name + ": " + (firing > 0.001 ? String.format("firing at %.0f%%, %.2f MW, on %s", firing * 100, firing * ratedWatts() / 1e6, fuel)
				: "cold (fuel gas or residue to its " + (big() ? "front" : "bottom") + ")"));
	}
}
