package dev.wildcatter.block;

import dev.wildcatter.multiblock.BigPart;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Multiblock;
import dev.wildcatter.multiblock.MultiblockLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.Nullable;

/**
 * A machine built from true-size parts (docs/multiblocks/README.md, "Big parts"): where each part's main cell goes, measured
 * from the layout's origin part. The origin is the part a player puts down first (a genset's base, a tower's basin); a part
 * clicked on top of the machine snaps into its own slot by these layouts. The controller checks a machine against them.
 * One machine can have variants (the diesel and gas gensets): each its own layout and letters.
 */
public final class PartLayout {
	/** One variant: the layout (its controller letter is the origin part's) and what each letter is. */
	public record Variant(MultiblockLayout layout, Map<Character, Block> blocks) {
	}

	/** Where a part clicked at a position belongs, and which way it faces. */
	public record Slot(BlockPos main, Direction facing) {
	}

	/** What a controller finds: every part's main cell (null if the machine is not built), which variant, or why not. */
	public record Found(@Nullable List<BlockPos> parts, int variant, String missing) {
	}

	private final List<Variant> variants;
	private final Block controller;
	private final Map<Character, String> names;
	private final boolean snaps;

	/**
	 * @param controller the part that checks the machine
	 * @param names what each letter is called in the readout
	 * @param snaps whether a part clicked on top of the machine goes to its slot (a machine with no part to build on does not)
	 */
	public PartLayout(Block controller, Map<Character, String> names, boolean snaps, Variant... variants) {
		this.variants = List.of(variants);
		this.controller = controller;
		this.names = names;
		this.snaps = snaps;
	}

	private int[] slot(Block part, int variant, int turns) {
		Variant v = variants.get(variant);
		for (MultiblockLayout.Cell cell : v.layout().cells(turns)) {
			if (v.blocks().get(cell.part()) == part) {
				return new int[] {cell.dx(), cell.dy(), cell.dz()};
			}
		}
		return null;
	}

	/** The variant whose origin part this block is, or -1. */
	private int originOf(Block block) {
		for (int i = 0; i < variants.size(); i++) {
			if (variants.get(i).blocks().get(origin(i)) == block) {
				return i;
			}
		}
		return -1;
	}

	private char origin(int variant) {
		for (MultiblockLayout.Cell cell : variants.get(variant).layout().cells(0)) {
			if (cell.dx() == 0 && cell.dy() == 0 && cell.dz() == 0) {
				return cell.part();
			}
		}
		throw new IllegalStateException("a layout without its origin");
	}

	private boolean member(Block block) {
		for (Variant v : variants) {
			if (v.blocks().containsValue(block)) {
				return true;
			}
		}
		return false;
	}

	/** The origin part a part at {@code pos} would stand on: straight under it, or under the parts it would sit on. */
	private @Nullable BlockPos originUnder(BlockGetter level, BlockPos pos) {
		BlockPos at = pos.below();
		for (int i = 0; i < 6; i++) {
			BlockState state = level.getBlockState(at);
			if (!(state.getBlock() instanceof BigPart) || !member(state.getBlock())) {
				return null;
			}
			BlockPos main = BigParts.main(at, state);
			if (originOf(state.getBlock()) >= 0) {
				return main;
			}
			at = main.below();
		}
		return null;
	}

	/** Where a part clicked at {@code pos} belongs, and which way it faces: its slot on the machine it is clicked onto. */
	public @Nullable Slot slotFor(BlockGetter level, BlockPos pos, Block part) {
		if (!snaps) {
			return null;
		}
		BlockPos origin = originUnder(level, pos);
		if (origin == null) {
			return null;
		}
		BlockState state = level.getBlockState(origin);
		Direction facing = ((BigPart) state.getBlock()).facing(state);
		int[] at = slot(part, originOf(state.getBlock()), Multiblock.turns(facing));
		return at == null ? null : new Slot(origin.offset(at[0], at[1], at[2]), facing);
	}

	/** For {@link BigPart#snap}: a click on top of the machine moves to the part's slot. */
	public BlockPlaceContext snap(BlockPlaceContext context, Block part) {
		Slot slot = slotFor(context.getLevel(), context.getClickedPos(), part);
		if (slot == null || slot.main().equals(context.getClickedPos())) {
			return context;
		}
		return BlockPlaceContext.at(context, slot.main(), Direction.UP);
	}

	/** Facing the player, or the machine's way when it goes into a slot. */
	public BlockState placementState(BlockPlaceContext context, Block part) {
		Slot slot = slotFor(context.getLevel(), context.getClickedPos(), part);
		Direction facing = slot != null ? slot.facing() : context.getHorizontalDirection().getOpposite();
		return part.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
	}

	/** For {@code getStateForPlacement}: refused where the part's slot on this machine is taken, else placed as a big part. */
	public @Nullable BlockState placement(BlockPlaceContext context, Block part) {
		Slot slot = slotFor(context.getLevel(), context.getClickedPos(), part);
		if (slot != null && !slot.main().equals(context.getClickedPos())) {
			return null;
		}
		return BigParts.placement(context, placementState(context, part));
	}

	/** The origin part's main cell for a machine whose controller's main cell is at {@code controllerPos}. */
	public BlockPos origin(BlockPos controllerPos, Direction facing, int variant) {
		int[] k = slot(controller, variant, Multiblock.turns(facing));
		return controllerPos.offset(-k[0], -k[1], -k[2]);
	}

	/**
	 * Test hook: builds a whole machine round a controller at {@code controllerPos}, every part grown into its cells, as if
	 * placed by hand. Returns the cells it overwrote that were not air, so a test can tell when a machine does not fit its scene.
	 */
	public List<BlockPos> build(Level level, BlockPos controllerPos, Direction facing, int variant) {
		BlockPos origin = origin(controllerPos, facing, variant);
		Variant v = variants.get(variant);
		List<BlockPos> overwrote = new ArrayList<>();
		for (MultiblockLayout.Cell cell : v.layout().cells(Multiblock.turns(facing))) {
			Block part = v.blocks().get(cell.part());
			BlockPos main = origin.offset(cell.dx(), cell.dy(), cell.dz());
			BlockState state = part.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
			for (BlockPos at : BigParts.cells(main, state)) {
				if (!level.getBlockState(at).isAir()) {
					overwrote.add(at.immutable());
				}
			}
			level.setBlock(main, state, Block.UPDATE_ALL);
			BigParts.grow(level, main, state);
		}
		return overwrote;
	}

	/** What the controller at {@code controllerPos}, facing {@code facing}, finds. The first variant whose origin part is there is the one reported. */
	public Found check(Level level, BlockPos controllerPos, Direction facing) {
		int turns = Multiblock.turns(facing);
		String firstWhy = null;
		for (int variant = 0; variant < variants.size(); variant++) {
			Variant v = variants.get(variant);
			BlockPos origin = origin(controllerPos, facing, variant);
			boolean thisKind = level.getBlockState(origin).is(v.blocks().get(origin(variant)));
			List<BlockPos> parts = new ArrayList<>();
			String why = null;
			for (MultiblockLayout.Cell cell : v.layout().cells(turns)) {
				BlockPos at = origin.offset(cell.dx(), cell.dy(), cell.dz());
				BlockState state = level.getBlockState(at);
				Block want = v.blocks().get(cell.part());
				String where = names.getOrDefault(cell.part(), String.valueOf(cell.part())) + " at " + at.getX() + " " + at.getY() + " " + at.getZ();
				if (!level.isLoaded(at) || !state.is(want)) {
					why = where;
				} else if (state.getValue(((BigPart) want).piece()) != 0 || ((BigPart) want).facing(state) != facing) {
					why = where + " turned to face the machine's way (break it and place it again)";
				}
				if (why != null) {
					break;
				}
				parts.add(at.immutable());
			}
			if (why == null) {
				return new Found(parts, variant, "");
			}
			if (thisKind || firstWhy == null) {
				firstWhy = why;
			}
			if (thisKind) {
				break;
			}
		}
		return new Found(null, 0, firstWhy);
	}
}
