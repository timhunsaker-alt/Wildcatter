package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.fluid.Product;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The lined tailings cell (docs/multiblocks/tailings-cell.md): finds its basin from where it stands, takes tails slurry off any steel
 * pipe touching it until the basin is full or breached, fills the basin with settled tails, and returns the water on a water pipe.
 */
public class DecantTowerBlockEntity extends BlockEntity implements Inspectable {
	private static final int SCAN_TICKS = 40;

	private double settledKg;
	private double reclaimed;
	private double gypsumKg;   // of the settled tails, phosphogypsum from a pan filter (Phase 18a)
	private double saltKg;     // and salt tails from a potash dissolver
	private int placed;
	private List<BlockPos> floor = List.of();
	private int rim;
	private @Nullable BlockPos breach;
	private String status = "finding its basin";
	/**
	 * Set on a tower placed from 0.7.0 on: its rate scales with its cell's floor ({@link #ratePerTick}). A tower from an older save keeps
	 * the old 20 kg a tick whatever its cell (protection of built worlds).
	 */
	private boolean scales = true;

	public DecantTowerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.DECANT_TOWER_ENTITY, pos, state);
	}

	public double settledKg() {
		return settledKg;
	}

	public double capacityKg() {
		return (floor.size() * rim - 1) * Tuning.CELL_KG_PER_BLOCK;
	}

	/**
	 * kg of slurry it takes a tick at most: 20 kg (400 kg/s) on a cell of up to 256 m2 of floor, and that much again for each 256 m2 more
	 * (Tuning.CELL_FLOOR_PER_RATE), so a 4,096-cell basin takes 16 times as much. A tower from an older save takes 20 kg.
	 */
	public double ratePerTick() {
		double rate = Tuning.CELL_SLURRY_KG_PER_TICK;
		return scales ? rate * Math.max(1.0, floor.size() / Tuning.CELL_FLOOR_PER_RATE) : rate;
	}

	/** Test hook: the tower as loaded from a save made before its rate scaled. */
	public void asOlderSaveForTest() {
		scales = false;
	}

	public @Nullable BlockPos breach() {
		return breach;
	}

	public String status() {
		return status;
	}

	/** Test hook: tails in the cell without a pipe. */
	public void addSolidsForTest(double kg) {
		settledKg += kg;
		setChanged();
	}

	private static boolean liner(Level level, BlockPos pos) {
		return level.getBlockState(pos).getBlock() == WildcatterBlocks.CELL_LINER;
	}

	private static boolean open(Level level, BlockPos pos) {
		Block block = level.getBlockState(pos).getBlock();
		return block == WildcatterBlocks.SETTLED_TAILS || block instanceof DecantTowerBlock || level.getBlockState(pos).canBeReplaced() || block instanceof PipeBlock;
	}

	/** The tower's layer flooded out to the walls, the walls' lowest point, and the first hole found. */
	private void scan(Level level) {
		breach = null;
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		seen.add(worldPosition);
		queue.add(worldPosition);
		int low = 16;
		while (!queue.isEmpty()) {
			BlockPos at = queue.poll();
			if (!liner(level, at.below())) {
				breach = at.below();
				break;
			}
			for (Direction side : Direction.Plane.HORIZONTAL) {
				BlockPos next = at.relative(side);
				if (liner(level, next)) {
					int height = 1;
					while (height < low && liner(level, next.above(height))) {
						height++;
					}
					low = Math.min(low, height);
				} else if (seen.size() >= Tuning.CELL_MAX_AREA || !open(level, next)) {
					breach = next;
					queue.clear();
					break;
				} else if (seen.add(next)) {
					queue.add(next);
				}
			}
		}
		rim = low;
		floor = new ArrayList<>(seen);
		floor.sort(Comparator.comparingInt((BlockPos p) -> p.distManhattan(worldPosition)).thenComparingLong(BlockPos::asLong));
		if (breach == null && settledKg > capacityKg() + Tuning.CELL_KG_PER_BLOCK) {
			breach = worldPosition.above(rim);   // a wall broken below what the cell holds
		}
	}

	/** Shows the fill: a block of settled tails for each 2.2 t, layer by layer. */
	private void show(Level level) {
		int want = (int) Math.floor(settledKg / Tuning.CELL_KG_PER_BLOCK);
		int cells = floor.size();
		while (placed < want && placed < cells * rim - 1) {
			// the tower's own column is the first of the floor cells: it is skipped, and its upper cells are filled last
			int index = placed + 1;
			BlockPos at = floor.get(index % cells).above(index / cells);
			if (level.getBlockState(at).canBeReplaced()) {
				level.setBlock(at, WildcatterBlocks.SETTLED_TAILS.defaultBlockState(), Block.UPDATE_ALL);
			}
			placed++;
		}
	}

	private void decant(Level level) {
		if (reclaimed < 1) {
			return;
		}
		List<BlockPos> cell = List.of(worldPosition);
		Set<BlockPos> pipes = Plumbing.network(level, cell);
		int moved = 0;
		int budget = (int) reclaimed;
		for (BlockEntity user : Plumbing.machines(level, cell, pipes, true, BlockEntity.class)) {
			if (user instanceof WaterUser water && moved < budget) {
				moved += water.addWater(budget - moved);
			}
		}
		reclaimed = moved > 0 ? reclaimed - moved : Math.min(reclaimed, 200);   // with nowhere to go, the rest is lost
	}

	public static void tick(Level level, BlockPos pos, BlockState state, DecantTowerBlockEntity tower) {
		long now = level.getGameTime() + pos.hashCode();
		if (now % SCAN_TICKS == 0 || tower.floor.isEmpty()) {
			tower.scan(level);
			tower.show(level);
			if (tower.breach != null && level instanceof ServerLevel server) {
				BlockPos b = tower.breach;
				server.sendParticles(ParticleTypes.DRIPPING_WATER, b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5, 8, 0.4, 0.2, 0.4, 0);
			}
		}
		if (tower.breach != null) {
			tower.status = "BREACHED: seeping at " + tower.breach.toShortString() + ": put the liner back";
			return;
		}
		double room = tower.capacityKg() - tower.settledKg;
		if (room <= 0) {
			tower.status = String.format("FULL: %.0f t of tails", tower.settledKg / 1000);
			return;
		}
		Slurry.Taken got = Slurry.take(level, Lines.faces(List.of(pos)), Product.TAILS_SLURRY, Math.min(tower.ratePerTick(), room / Tuning.CELL_SLURRY_SOLIDS), tower);
		if (got.kg() > 0) {
			tower.settledKg += got.kg() * Tuning.CELL_SLURRY_SOLIDS;
			tower.gypsumKg += got.assay().kg(PanFilterBlockEntity.GYPSUM, got.kg());
			tower.saltKg += got.assay().kg(PotashDissolverBlockEntity.SALT, got.kg());
			tower.reclaimed += got.kg() * (1 - Tuning.CELL_SLURRY_SOLIDS) * Tuning.CELL_RECLAIM;
			tower.setChanged();
		}
		if (now % Tuning.PLUMBING_INTERVAL == 0) {
			tower.decant(level);
		}
		tower.status = String.format("%.1f of %.0f t of tails (%d x %d cells); takes up to %,.0f t/h of slurry", tower.settledKg / 1000, tower.capacityKg() / 1000,
				tower.floor.size(), tower.rim, tower.ratePerTick() * 20 * 3.6) + tower.stack();
	}

	/** What the stack is made of, when a fertilizer plant sent it: "", or "; of which 12.0 t phosphogypsum". */
	private String stack() {
		String out = gypsumKg >= 1 ? String.format("%.1f t phosphogypsum", gypsumKg / 1000) : "";
		if (saltKg >= 1) {
			out += (out.isEmpty() ? "" : " and ") + String.format("%.1f t salt", saltKg / 1000);
		}
		return out.isEmpty() ? "" : "; of which " + out;
	}

	/** kg of phosphogypsum it has taken. */
	public double gypsumKg() {
		return gypsumKg;
	}

	@Override
	public Component inspect() {
		return Component.literal("Decant tower: " + status);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("settled", settledKg);
		output.putDouble("reclaimed", reclaimed);
		output.putInt("placed", placed);
		output.putBoolean("scales", scales);
		if (gypsumKg > 0) {
			output.putDouble("gypsum", gypsumKg);
		}
		if (saltKg > 0) {
			output.putDouble("salt", saltKg);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		settledKg = input.getDoubleOr("settled", 0);
		reclaimed = input.getDoubleOr("reclaimed", 0);
		placed = input.getIntOr("placed", 0);
		scales = input.getBooleanOr("scales", false);   // an older save keeps 20 kg a tick
		gypsumKg = input.getDoubleOr("gypsum", 0);
		saltKg = input.getDoubleOr("salt", 0);
	}
}
