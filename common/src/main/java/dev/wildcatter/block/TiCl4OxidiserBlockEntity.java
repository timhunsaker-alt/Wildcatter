package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.TitaniumPigment;
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
 * The TiCl4 oxidiser (docs/multiblocks/titanium-pigment.md): pure TiCl4 and oxygen, both preheated, burned at about 1,500 C and quenched
 * in a flue cooler. Oxygen on the north face of its front cells, pipeline gas for its preheaters (0.2 MW) on the east face of its front
 * cells; raw TiO2 to the pigment finishing it touches, chlorine back out of the west face of its back cell, for the chlorinator. The
 * chlorinator runs it; its spec gives it no motor.
 */
public class TiCl4OxidiserBlockEntity extends ProcessPartBlockEntity {
	double burned;    // kg of TiO2 made, all told
	double returned;  // kg of chlorine taken back by the chlorinator
	String unit = "idle: no pure TiCl4";

	public TiCl4OxidiserBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.TICL4_OXIDISER_ENTITY, pos, state);
	}

	List<Lines.Face> oxygenIn() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	List<Lines.Face> gasIn() {
		return faces(at -> at[0] == 2 && at[2] <= 1 && at[1] == 0, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[0] == 0 && at[2] == 9 && at[1] == 0 && side == Direction.WEST ? Product.CHLORINE : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.CHLORINE ? TitaniumPigment.tio2Kgs() * TitaniumPigment.cl2BackPerTio2() * Tuning.PLANT_HOLD_SECONDS * 8 : 0;
	}

	double chlorineRoom() {
		return room(Product.CHLORINE);
	}

	/** The chlorinator takes its chlorine back first: up to {@code kg}; returns what it got. */
	double takeChlorine(double kg) {
		double got = Math.min(kg, held(Product.CHLORINE));
		add(Product.CHLORINE, -got);
		returned += got;
		return got;
	}

	/** kg of chlorine the chlorinator has taken back, all told. */
	public double returned() {
		return returned;
	}

	void giveChlorine(double kg) {
		add(Product.CHLORINE, kg);
	}

	@Override
	protected void work(Level level) {
		// the chlorinator runs it
	}

	@Override
	public Component inspect() {
		return Component.literal("TiCl4 oxidiser (oxygen reactor and flue cooler): " + unit + String.format("; chlorine held for the chlorinator %.0f kg; %,.0f kg of TiO2 made",
				held(Product.CHLORINE), burned));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("burned", burned);
		output.putDouble("returned", returned);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		burned = input.getDoubleOr("burned", 0);
		returned = input.getDoubleOr("returned", 0);
	}
}
