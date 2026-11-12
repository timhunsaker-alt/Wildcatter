package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.fluid.ChlorAlkaliPlant;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The brine wellhead's state (docs/multiblocks/chlor-alkali-plant.md): fresh water from an injection pump dissolves the salt at the
 * bottom of its hole into saturated brine (26 % NaCl), which it offers on the pipe at the side it faces. Each 2,117 kg of salt it brings
 * up turns one rock salt block into water, from the bottom of the hole upward and outward: the cavern grows.
 */
public class BrineWellheadBlockEntity extends BlockEntity implements FluidNode, Inspectable {
	private static final double BRINE_HOLD = 2_000.0;
	private double brine;      // kg held for the pipe
	private double salt;       // kg of salt dissolved toward the next block
	private double brought;    // kg of salt brought up since placed
	private int blocks;        // rock salt blocks dissolved
	private String status = "idle: no water from an injection pump";

	public BrineWellheadBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.BRINE_WELLHEAD_ENTITY, pos, state);
	}

	/** The block under the hole's casing, where the cavern starts; null if the hole has no casing under the head. */
	private @Nullable BlockPos bottom(Level level) {
		BlockPos at = worldPosition.below();
		if (!level.getBlockState(at).is(WildcatterBlocks.CASING)) {
			return null;
		}
		while (level.getBlockState(at).is(WildcatterBlocks.CASING) && at.getY() > level.getMinY()) {
			at = at.below();
		}
		return at;
	}

	private static boolean salt(BlockState state) {
		return state.is(OreBlocks.block(Ore.HALITE));
	}

	/** The next rock salt block the water reaches: the bottom of the hole, then upward and outward through the cavern. */
	@Nullable BlockPos nextSalt(Level level) {
		BlockPos start = bottom(level);
		if (start == null) {
			return null;
		}
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> seen = new HashSet<>();
		queue.add(start);
		seen.add(start);
		while (!queue.isEmpty()) {
			BlockPos at = queue.poll();
			BlockState state = level.getBlockState(at);
			if (salt(state)) {
				return at;
			}
			if (!at.equals(start) && !state.is(Blocks.WATER)) {
				continue;
			}
			if (at.equals(start) && !state.is(Blocks.WATER)) {
				return null;   // the hole does not end in salt
			}
			for (Direction d : new Direction[] {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
				BlockPos next = at.relative(d);
				if (next.distManhattan(start) <= Tuning.BRINE_CAVERN_REACH && seen.add(next)) {
					queue.add(next);
				}
			}
		}
		return null;
	}

	/** The injection pump pushes {@code litres} of fresh water down; returns what the well took. */
	public double inject(Level level, double litres) {
		BlockPos next = nextSalt(level);
		if (next == null) {
			status = bottom(level) == null ? "NO HOLE: it must stand on a cased hole" : "NO SALT: the hole does not end in rock salt, or the cavern has dissolved all within reach";
			return 0;
		}
		double perLitre = Tuning.BRINE_NACL / (1 - Tuning.BRINE_NACL);   // kg of salt a litre of water carries away saturated
		double water = Math.min(litres, (BRINE_HOLD - brine) * (1 - Tuning.BRINE_NACL));
		if (water <= 1e-6) {
			status = "FULL: its brine has nowhere to go (pipe the side it faces to a brine saturator)";
			return 0;
		}
		double kg = water * perLitre;
		salt += kg;
		brought += kg;
		brine += water + kg;
		while (salt >= Ore.HALITE.kgPerBlock() && next != null) {
			salt -= Ore.HALITE.kgPerBlock();
			level.setBlock(next, Blocks.WATER.defaultBlockState(), 3);
			blocks++;
			next = nextSalt(level);
		}
		status = String.format("solution mining: %.2f L/s of water down, brine up at %.0f %% NaCl; the cavern %d blocks", water / seconds(), Tuning.BRINE_NACL * 100, blocks);
		setChanged();
		return water;
	}

	private static double seconds() {
		return Tuning.WELL_INTERVAL / 20.0;
	}

	/** Rock salt blocks dissolved into the cavern. */
	public int blocks() {
		return blocks;
	}

	/** For the game test: salt as if dissolved, toward the next block. */
	public void testSalt(double kg) {
		salt += kg;
	}

	private Direction out() {
		return getBlockState().getValue(BrineWellheadBlock.FACING);
	}

	@Override
	public @Nullable Product offers(BlockPos cell, Direction side) {
		return side == out() && brine > 1e-6 ? Product.BRINE : null;
	}

	@Override
	public double available(BlockPos cell, Direction side, Product product, Pull pull) {
		return side == out() && product == Product.BRINE ? brine : 0;
	}

	@Override
	public double take(BlockPos cell, Direction side, Product product, double max, Pull pull) {
		if (side != out() || product != Product.BRINE) {
			return 0;
		}
		double got = Math.min(max, brine);
		brine -= got;
		setChanged();
		return got;
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Brine wellhead (solution mining): %s; brine held %.0f kg; %,.0f kg of salt brought up (%.1f brine a kg), %d rock salt blocks dissolved",
				status, brine, brought, ChlorAlkaliPlant.brinePerSalt(), blocks));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("brine", brine);
		output.putDouble("salt", salt);
		output.putDouble("brought", brought);
		output.putInt("blocks", blocks);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		brine = input.getDoubleOr("brine", 0);
		salt = input.getDoubleOr("salt", 0);
		brought = input.getDoubleOr("brought", 0);
		blocks = input.getIntOr("blocks", 0);
	}
}
