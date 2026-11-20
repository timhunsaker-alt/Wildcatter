package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.fluid.ChlorAlkaliPlant;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.well.CavernData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The brine wellhead's state (docs/multiblocks/chlor-alkali-plant.md; 23c, oilfield-follow-ons.md): fresh water from an injection pump
 * dissolves the salt at the bottom of its hole into saturated brine (26 % NaCl), which it offers on the pipe at the side it faces, or
 * sends straight to a disposal wellhead on that pipe (then the pump runs at its full 10 L/s). Each 2,117 kg of salt it brings up turns one
 * rock salt block into water: the cavern grows, upward without a blanket, outward first with one (diesel or nitrogen on a pipe to any
 * other side of it), and stops at its walls (2 blocks of salt to the flank, 3 under the caprock: CAVERN AT LIMIT). With the rule
 * {@code cavernStability} it leaches on past them and breaches. The cavern is saved per hole ({@link CavernData}).
 */
public class BrineWellheadBlockEntity extends BlockEntity implements FluidNode, Inspectable {
	private static final double BRINE_HOLD = 2_000.0;
	private double brine;      // kg held for the pipe
	private double salt;       // kg of salt dissolved toward the next block
	private double brought;    // kg of salt brought up since placed
	private double disposed;   // kg of brine sent to disposal
	private boolean toDisposal;
	private boolean blanket;
	private int surveyLeft;
	private java.util.@Nullable UUID surveyor;
	private String status = "idle: no water from an injection pump";

	public BrineWellheadBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.BRINE_WELLHEAD_ENTITY, pos, state);
	}

	private CavernData.@Nullable Hole hole() {
		return level instanceof ServerLevel server ? CavernData.get(server).at(worldPosition) : null;
	}

	/** The next rock salt block the water reaches, or null. */
	@Nullable BlockPos nextSalt(Level level) {
		BlockPos start = Caverns.bottom(level, worldPosition);
		CavernData.Hole hole = hole();
		if (start == null || hole == null) {
			return null;
		}
		if (hole.blocks.isEmpty() && level.getBlockState(start).is(Blocks.WATER)) {
			adopt(level, hole, start);   // a cavern leached before 23c: its water becomes the cavern's record
		}
		return Caverns.next(level, hole, start, blanket);
	}

	/** The water a brine well leached before the caverns were recorded, joined to its hole's bottom within 12 blocks. */
	private static void adopt(Level level, CavernData.Hole hole, BlockPos start) {
		java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>(List.of(start));
		java.util.Set<BlockPos> seen = new java.util.HashSet<>(List.of(start));
		while (!queue.isEmpty() && hole.blocks.size() < 4000) {
			BlockPos at = queue.poll();
			if (!level.getBlockState(at).is(Blocks.WATER)) {
				continue;
			}
			hole.blocks.add(at.asLong());
			for (Direction d : Direction.values()) {
				BlockPos n = at.relative(d);
				if (n.distManhattan(start) <= Tuning.BRINE_CAVERN_REACH && seen.add(n)) {
					queue.add(n);
				}
			}
		}
	}

	/** The brine goes to a disposal wellhead on its outlet's pipe: the injection pump may run at its full rate. */
	public boolean toDisposal() {
		return toDisposal;
	}

	/** The injection pump pushes {@code litres} of fresh water down; returns what the well took. */
	public double inject(Level level, double litres) {
		CavernData.Hole hole = hole();
		toDisposal = level != null && disposalOnOutlet(level);
		blanket = takeBlanket(level, litres / (Tuning.BRINE_WELL_INJECT_LPS));
		BlockPos next = nextSalt(level);
		if (next == null || hole == null) {
			status = Caverns.bottom(level, worldPosition) == null ? "NO HOLE: it must stand on a cased hole"
					: hole != null && hole.breached ? "BREACHED: the cavern is lost" : hole != null && hole.volume() > 0 ? "CAVERN AT LIMIT: its walls are as thin as they may be"
					: "NO SALT: the hole does not end in rock salt";
			return 0;
		}
		if (toDisposal) {
			disposed += brine;
			brine = 0;   // the disposal well takes it as it comes
		}
		double perLitre = Tuning.BRINE_NACL / (1 - Tuning.BRINE_NACL);   // kg of salt a litre of water carries away saturated
		double water = Math.min(litres, (BRINE_HOLD - brine) * (1 - Tuning.BRINE_NACL));
		if (water <= 1e-6) {
			status = "FULL: its brine has nowhere to go (pipe the side it faces to a brine saturator or a disposal wellhead)";
			return 0;
		}
		double kg = water * perLitre;
		salt += kg;
		brought += kg;
		brine += water + kg;
		while (salt >= Ore.HALITE.kgPerBlock() && next != null && !hole.breached) {
			salt -= Ore.HALITE.kgPerBlock();
			leach(level, hole, next);
			next = nextSalt(level);
		}
		if (toDisposal) {
			disposed += brine;
			brine = 0;
		}
		status = String.format("solution mining%s: %.2f L/s of water down, brine up at %.0f %% NaCl%s; %s", blanket ? " under a blanket" : "", water / seconds(), Tuning.BRINE_NACL * 100,
				toDisposal ? " to disposal" : "", Caverns.recordText(level, hole));
		setChanged();
		return water;
	}

	/** One rock salt block into the cavern; past its walls with {@code cavernStability}, the breach. */
	private void leach(Level level, CavernData.Hole hole, BlockPos at) {
		boolean flank = Caverns.flankOk(level, hole, at), roof = Caverns.roofOk(level, hole, at);
		boolean first = hole.blocks.isEmpty();
		level.setBlock(at, Blocks.WATER.defaultBlockState(), 3);
		hole.blocks.add(at.asLong());
		if (!first && level instanceof ServerLevel server && dev.wildcatter.Rules.cavernStability()) {
			if (!flank) {
				Caverns.floodBreach(server, hole, worldPosition);
			} else if (!roof) {
				Caverns.heave(server, hole, worldPosition);
			}
		}
	}

	/** True if the pipe on the side it faces reaches a disposal wellhead. */
	private boolean disposalOnOutlet(Level level) {
		for (Lines.Line line : Lines.lines(level, List.of(new Lines.Face(worldPosition, out())))) {
			if (line.touches(level, DisposalWellheadBlock.class)) {
				return true;
			}
		}
		return false;
	}

	/** Diesel or nitrogen on a pipe at any other side keeps a blanket on the roof. */
	private boolean takeBlanket(Level level, double seconds) {
		List<Lines.Face> faces = new ArrayList<>();
		for (Direction d : Direction.Plane.HORIZONTAL) {
			if (d != out()) {
				faces.add(new Lines.Face(worldPosition, d));
			}
		}
		double want = Tuning.BLANKET_LPS * Math.max(0.05, seconds);
		double got = Lines.pull(level, faces, Product.DIESEL, want, Pull.MACHINE, this);
		if (got < want * 0.5) {
			got += Lines.pull(level, faces, Product.NITROGEN, want, Pull.MACHINE, this);
		}
		return got > 0;
	}

	private static double seconds() {
		return Tuning.WELL_INTERVAL / 20.0;
	}

	/** Rock salt blocks dissolved into the cavern. */
	public int blocks() {
		CavernData.Hole hole = hole();
		return hole == null ? 0 : hole.volume();
	}

	public boolean blanket() {
		return blanket;
	}

	/** For the game test: salt as if dissolved, toward the next block. */
	public void testSalt(double kg) {
		salt += kg;
	}

	/** The sonar tool's survey: 30 s of pings, then the outline and the record for the surveyor. */
	public void survey(net.minecraft.world.entity.player.Player player) {
		surveyLeft = Tuning.SONAR_TICKS;
		surveyor = player.getUUID();
		setChanged();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, BrineWellheadBlockEntity head) {
		if (!(level instanceof ServerLevel server) || level.getGameTime() % 20 != 0) {
			return;
		}
		CavernData.Hole hole = head.hole();
		if (hole != null) {
			Caverns.tickHeave(server, hole, pos);
			head.surveyLeft = Sonar.tick(server, pos, hole, head.surveyLeft, head.surveyor, "Brine wellhead");
		}
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
		return Component.literal(String.format("Brine wellhead (solution mining): %s; brine held %.0f kg; %,.0f kg of salt brought up (%.1f brine a kg), %,.0f kg of brine to disposal",
				status, brine, brought, ChlorAlkaliPlant.brinePerSalt(), disposed));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("brine", brine);
		output.putDouble("salt", salt);
		output.putDouble("brought", brought);
		output.putDouble("disposed", disposed);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		brine = input.getDoubleOr("brine", 0);
		salt = input.getDoubleOr("salt", 0);
		brought = input.getDoubleOr("brought", 0);
		disposed = input.getDoubleOr("disposed", 0);
	}
}
