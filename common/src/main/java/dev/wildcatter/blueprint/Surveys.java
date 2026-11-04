package dev.wildcatter.blueprint;

import dev.wildcatter.block.MultiCableBlockEntity;
import dev.wildcatter.block.WireBlockEntity;
import dev.wildcatter.multiblock.BigPart;
import dev.wildcatter.multiblock.BigParts;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.Nullable;

/**
 * The Minecraft side of blueprints (docs/blueprints.md): recording a box of the world, reading a blueprint's states back, and
 * comparing a site with what stands.
 */
public final class Surveys {
	private Surveys() {
	}

	public static final int MAX_X = 128, MAX_Y = 96, MAX_Z = 128;

	/** Records everything between {@code a} and {@code b}: ordinary blocks, and each true-size part by its main cell. */
	public static Blueprint record(Level level, String name, BlockPos a, BlockPos b) {
		BlockPos lo = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
		BlockPos hi = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
		int sx = hi.getX() - lo.getX() + 1, sy = hi.getY() - lo.getY() + 1, sz = hi.getZ() - lo.getZ() + 1;
		List<String> palette = new ArrayList<>();
		Map<String, Integer> index = new HashMap<>();
		List<Blueprint.Block> blocks = new ArrayList<>();
		List<Blueprint.Part> parts = new ArrayList<>();
		Map<String, String> sizes = new LinkedHashMap<>();
		for (BlockPos pos : BlockPos.betweenClosed(lo, hi)) {
			BlockState state = level.getBlockState(pos);
			int x = pos.getX() - lo.getX(), y = pos.getY() - lo.getY(), z = pos.getZ() - lo.getZ();
			if (state.getBlock() instanceof BigPart part) {
				if (state.getValue(part.piece()) != 0) {
					continue;   // its main cell stands for it
				}
				int x0 = x, y0 = y, z0 = z, x1 = x, y1 = y, z1 = z;
				for (BlockPos cell : BigParts.cells(pos, state)) {
					x0 = Math.min(x0, cell.getX() - lo.getX());
					y0 = Math.min(y0, cell.getY() - lo.getY());
					z0 = Math.min(z0, cell.getZ() - lo.getZ());
					x1 = Math.max(x1, cell.getX() - lo.getX());
					y1 = Math.max(y1, cell.getY() - lo.getY());
					z1 = Math.max(z1, cell.getZ() - lo.getZ());
				}
				parts.add(new Blueprint.Part(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), BigParts.turns(state), x, y, z, x0, y0, z0, x1, y1, z1));
				continue;
			}
			if (!recorded(state)) {
				continue;
			}
			String s = BlockStateParser.serialize(state);
			Integer i = index.get(s);
			if (i == null) {
				i = palette.size();
				palette.add(s);
				index.put(s, i);
			}
			blocks.add(new Blueprint.Block(x, y, z, i));
			String size = size(level, pos);
			if (size != null) {
				sizes.put(x + "," + y + "," + z, size);
			}
		}
		return new Blueprint(name, sx, sy, sz, palette, blocks, parts, sizes);
	}

	/** Air, fluids, fire and blocks with no item are left out. */
	public static boolean recorded(BlockState state) {
		return !state.isAir() && !(state.getBlock() instanceof LiquidBlock) && state.getBlock().asItem() != Items.AIR;
	}

	private static @Nullable String size(BlockGetter level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof WireBlockEntity wire ? wire.size().getSerializedName()
				: level.getBlockEntity(pos) instanceof MultiCableBlockEntity cable ? cable.size().getSerializedName() : null;
	}

	// ---- reading a blueprint back --------------------------------------------------------------------------------------------
	/** Each palette entry as a state, null where the block is unknown (a mod that is gone). */
	public static BlockState[] states(Blueprint b) {
		BlockState[] out = new BlockState[b.palette().size()];
		for (int i = 0; i < out.length; i++) {
			try {
				out[i] = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, b.palette().get(i), false).blockState();
			} catch (Exception e) {
				out[i] = null;
			}
		}
		return out;
	}

	public static @Nullable Block part(Blueprint.Part p) {
		var id = net.minecraft.resources.Identifier.tryParse(p.id());
		return id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
	}

	/** A part's state for setting it: facing as written, piece 0. */
	public static @Nullable BlockState partState(Blueprint.Part p) {
		Block block = part(p);
		if (!(block instanceof BigPart big)) {
			return null;
		}
		BlockState state = block.defaultBlockState();
		Direction facing = Direction.NORTH;
		for (int i = 0; i < p.turns(); i++) {
			facing = facing.getClockWise();
		}
		if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
		}
		return state.setValue(big.piece(), 0);
	}

	/** A palette state turned {@code turns} quarter turns, as its blueprint was. */
	public static BlockState turned(BlockState state, int turns) {
		return switch (Math.floorMod(turns, 4)) {
			case 1 -> state.rotate(Rotation.CLOCKWISE_90);
			case 2 -> state.rotate(Rotation.CLOCKWISE_180);
			case 3 -> state.rotate(Rotation.COUNTERCLOCKWISE_90);
			default -> state;
		};
	}

	/** What an ordinary block is made of, by its sound type and its namespace. */
	public static Masses.Material material(BlockState state) {
		var key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		if (key.getNamespace().equals("wildcatter")) {
			return Masses.Material.WILDCATTER;
		}
		SoundType sound = state.getSoundType();
		if (sound == SoundType.STONE || sound == SoundType.DEEPSLATE || sound == SoundType.DEEPSLATE_BRICKS || sound == SoundType.POLISHED_DEEPSLATE
				|| sound == SoundType.TUFF || sound == SoundType.TUFF_BRICKS || sound == SoundType.SAND
				|| sound == SoundType.GRAVEL || sound == SoundType.NETHER_BRICKS || sound == SoundType.MUD_BRICKS || sound == SoundType.CALCITE
				|| sound == SoundType.BASALT || sound == SoundType.DRIPSTONE_BLOCK) {
			return Masses.Material.STONE;
		}
		if (sound == SoundType.METAL || sound == SoundType.ANVIL || sound == SoundType.NETHERITE_BLOCK || sound == SoundType.CHAIN || sound == SoundType.IRON) {
			return Masses.Material.METAL;
		}
		if (sound == SoundType.COPPER || sound == SoundType.COPPER_BULB || sound == SoundType.COPPER_GRATE) {
			return Masses.Material.COPPER;
		}
		if (sound == SoundType.GLASS) {
			return Masses.Material.GLASS;
		}
		if (sound == SoundType.WOOD || sound == SoundType.BAMBOO_WOOD || sound == SoundType.CHERRY_WOOD || sound == SoundType.NETHER_WOOD) {
			return Masses.Material.WOOD;
		}
		if (sound == SoundType.WOOL || sound == SoundType.GRASS || sound == SoundType.SNOW || sound == SoundType.AZALEA_LEAVES || sound == SoundType.CHERRY_LEAVES
				|| sound == SoundType.MOSS_CARPET || sound == SoundType.CROP) {
			return Masses.Material.LIGHT;
		}
		return Masses.Material.OTHER;
	}

	/** Each palette entry's tonnes. */
	public static double[] stateTonnes(Blueprint b, BlockState[] states) {
		double[] out = new double[states.length];
		for (int i = 0; i < out.length; i++) {
			out[i] = states[i] == null ? Masses.Material.OTHER.tonnes : material(states[i]).tonnes;
		}
		return out;
	}

	/** Each part's tonnes. */
	public static double[] partTonnes(Blueprint b) {
		double[] out = new double[b.parts().size()];
		for (int i = 0; i < out.length; i++) {
			Blueprint.Part p = b.parts().get(i);
			String id = p.id().contains(":") ? p.id().substring(p.id().indexOf(':') + 1) : p.id();
			out[i] = Masses.part(id, p.width(), p.height(), p.length());   // the box's volume is the same however it is turned
		}
		return out;
	}

	/** The lift list with the tower crane's 16 t as the pallets' cap (docs/blueprints.md). */
	public static List<Blueprint.Lift> lifts(Blueprint b) {
		return b.lifts(stateTonnes(b, states(b)), partTonnes(b), dev.wildcatter.Tuning.PALLET_CAP_TONNES);
	}

	// ---- a site ------------------------------------------------------------------------------------------------------------
	/** True if a block of the blueprint ({@code turned} already) stands at its place from {@code origin}. */
	public static boolean blockStands(Level level, BlockPos origin, Blueprint.Block k, BlockState want) {
		return want != null && standsAs(level.getBlockState(origin.offset(k.x(), k.y(), k.z())), want);
	}

	/**
	 * True if {@code there} is the block {@code want} is, set the same way. The six sides a cable, wire, fence or pane joins to are
	 * left out: the block redraws them from its new neighbours, so they need not match the survey.
	 */
	public static boolean standsAs(BlockState there, BlockState want) {
		if (there.getBlock() != want.getBlock()) {
			return false;
		}
		for (var property : want.getProperties()) {
			if (SIDES.contains(property.getName())) {
				continue;
			}
			if (!there.getValue(property).equals(want.getValue(property))) {
				return false;
			}
		}
		return true;
	}

	private static final java.util.Set<String> SIDES = java.util.Set.of("north", "south", "east", "west", "up", "down");

	/**
	 * Sets one lift of a site whose blueprint ({@code turned} already, with its turned {@code states}) has its origin at {@code origin}:
	 * a part placed and grown as a hand placement is, or a pallet's blocks with their conductor sizes. What stands already is left.
	 * Returns false if something else is in the way.
	 */
	public static boolean setLift(Level level, BlockPos origin, Blueprint b, BlockState[] states, Blueprint.Lift lift) {
		if (lift.isPart()) {
			Blueprint.Part p = b.parts().get(lift.part());
			if (partStands(level, origin, p)) {
				return true;
			}
			BlockState state = partState(p);
			BlockPos main = origin.offset(p.x(), p.y(), p.z());
			if (state == null || !level.getBlockState(main).canBeReplaced() || !BigParts.roomFor(level, main, state)) {
				return false;
			}
			level.setBlock(main, state, Block.UPDATE_ALL);
			BigParts.grow(level, main, state);
			return true;
		}
		boolean all = true;
		for (int i : lift.blocks()) {
			Blueprint.Block k = b.blocks().get(i);
			BlockState want = states[k.state()];
			BlockPos at = origin.offset(k.x(), k.y(), k.z());
			if (want == null || standsAs(level.getBlockState(at), want)) {
				continue;
			}
			if (!level.getBlockState(at).canBeReplaced()) {
				all = false;
				continue;
			}
			level.setBlock(at, want, Block.UPDATE_ALL);
			String size = b.size(k.x(), k.y(), k.z());
			var conductor = size == null ? null : dev.wildcatter.energy.ConductorSize.byId(size);
			if (conductor != null) {
				if (level.getBlockEntity(at) instanceof WireBlockEntity wire) {
					wire.setSize(conductor);
				} else if (level.getBlockEntity(at) instanceof MultiCableBlockEntity cable) {
					cable.setSize(conductor);
				}
			}
		}
		return all;
	}

	/** True if a part of the blueprint stands with its main cell at its place. */
	public static boolean partStands(Level level, BlockPos origin, Blueprint.Part p) {
		BlockState want = partState(p);
		if (want == null) {
			return false;
		}
		BlockState there = level.getBlockState(origin.offset(p.x(), p.y(), p.z()));
		return there.getBlock() == want.getBlock() && there.getValue(((BigPart) want.getBlock()).piece()) == 0
				&& BigParts.turns(there) == p.turns();
	}
}
