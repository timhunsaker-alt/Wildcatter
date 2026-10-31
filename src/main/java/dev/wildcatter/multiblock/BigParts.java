package dev.wildcatter.multiblock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wildcatter.Wildcatter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * What every {@link BigPart} does the same way: it is placed only where all its cells are free and then fills them; breaking
 * any piece takes the whole part and drops one item (piece 0's loot); a click on any piece goes to the main cell; and each
 * piece's collision is the real shape the model tool exported. A part from an older world that was a single block grows into
 * its cells when it can, and otherwise drops as an item and says where.
 */
public final class BigParts {
	private BigParts() {
	}

	/** While a big part's block is being constructed, its footprint: {@code createBlockStateDefinition} runs before the constructor body. */
	public static final ThreadLocal<Footprint> CONSTRUCTING = new ThreadLocal<>();

	/** The piece property for a part of this many cells, made in {@code createBlockStateDefinition}. */
	public static net.minecraft.world.level.block.state.properties.IntegerProperty pieceProperty(Footprint footprint) {
		return net.minecraft.world.level.block.state.properties.IntegerProperty.create("piece", 0, Math.max(1, footprint.pieces() - 1));
	}

	private static BigPart part(BlockState state) {
		return (BigPart) state.getBlock();
	}

	public static int turns(BlockState state) {
		return Multiblock.turns(part(state).facing(state));
	}

	/** The main cell of the part this piece belongs to. */
	public static BlockPos main(BlockPos pos, BlockState state) {
		BigPart part = part(state);
		int[] at = part.footprint().offset(state.getValue(part.piece()), turns(state));
		return pos.offset(-at[0], -at[1], -at[2]);
	}

	/** Where piece {@code piece} of a part with its main cell at {@code main} sits. */
	public static BlockPos cell(BlockPos main, BlockState state, int piece) {
		int[] at = part(state).footprint().offset(piece, turns(state));
		return main.offset(at[0], at[1], at[2]);
	}

	/** Every cell of the part, main cell first. */
	public static List<BlockPos> cells(BlockPos main, BlockState state) {
		List<BlockPos> out = new ArrayList<>();
		for (int piece = 0; piece < part(state).footprint().pieces(); piece++) {
			out.add(cell(main, state, piece));
		}
		return out;
	}

	/** The cells of whatever stands at {@code pos}: all of a big part's cells, or just {@code pos} for anything else. */
	public static List<BlockPos> cellsAt(BlockGetter level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof BigPart) {
			return cells(main(pos, state), state);
		}
		return List.of(pos);
	}

	/** The main cell of whatever stands at {@code pos}: a big part's piece 0, or {@code pos} itself for anything else. */
	public static BlockPos owner(BlockGetter level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.getBlock() instanceof BigPart ? main(pos, state) : pos;
	}

	/** The block entity behind a click or a look at any piece. */
	public static BlockEntity entity(BlockGetter level, BlockPos pos) {
		return level.getBlockEntity(owner(level, pos));
	}

	private static BlockState pieceState(BlockState main, int piece) {
		return main.setValue(part(main).piece(), piece);
	}

	private static boolean free(LevelReader level, BlockPos at) {
		return level.isInsideBuildHeight(at) && level.getBlockState(at).canBeReplaced();
	}

	/** Every cell but the main one is free for a part placed with its main cell at {@code main}. */
	public static boolean roomFor(LevelReader level, BlockPos main, BlockState state) {
		for (int piece = 1; piece < part(state).footprint().pieces(); piece++) {
			if (!free(level, cell(main, state, piece))) {
				return false;
			}
		}
		return true;
	}

	/** For {@code getStateForPlacement}: the state, or null (the item is not used up) if the part does not fit. */
	public static BlockState placement(BlockPlaceContext context, BlockState state) {
		if (state == null || !free(context.getLevel(), context.getClickedPos()) || !roomFor(context.getLevel(), context.getClickedPos(), state)) {
			if (context.getPlayer() != null && !context.getLevel().isClientSide() && state != null) {
				context.getPlayer().sendOverlayMessage(Component.literal(context.getItemInHand().getHoverName().getString() + " needs "
						+ size(part(state).footprint()) + " of clear space"));
			}
			return null;
		}
		return state.setValue(part(state).piece(), 0);
	}

	/** "3 x 2 x 3 m" (across, deep, high) for messages. */
	public static String size(Footprint footprint) {
		int minX = 0, maxX = 0, minY = 0, maxY = 0, minZ = 0, maxZ = 0;
		for (int piece = 0; piece < footprint.pieces(); piece++) {
			Footprint.Cell c = footprint.cell(piece);
			minX = Math.min(minX, c.dx());
			maxX = Math.max(maxX, c.dx());
			minY = Math.min(minY, c.dy());
			maxY = Math.max(maxY, c.dy());
			minZ = Math.min(minZ, c.dz());
			maxZ = Math.max(maxZ, c.dz());
		}
		return (maxX - minX + 1) + " x " + (maxZ - minZ + 1) + " x " + (maxY - minY + 1) + " m";
	}

	/** Fills the other cells: after a player places it ({@code setPlacedBy}), and when a lone main cell is found. */
	public static void grow(Level level, BlockPos main, BlockState state) {
		if (level.getBlockEntity(main) instanceof BigPartEntity entity) {
			entity.markGrown();
		}
		for (int piece = 1; piece < part(state).footprint().pieces(); piece++) {
			level.setBlock(cell(main, state, piece), pieceState(state, piece), Block.UPDATE_ALL);
		}
	}

	/** For {@code setPlacedBy}: a player placed the part, so it fills its cells now. */
	public static void placed(Level level, BlockPos pos, BlockState state) {
		if (!level.isClientSide()) {
			grow(level, pos, state);
		}
	}

	/** For {@code onPlace}: a main cell set by a command, or found alone, is checked on the next tick. */
	public static void onPlace(Level level, BlockPos pos, BlockState state, BlockState oldState) {
		if (state.getValue(part(state).piece()) == 0 && !oldState.is(state.getBlock())) {
			level.scheduleTick(pos, state.getBlock(), 1);
		}
	}

	/** For a block entity's ticker: every so often the main cell checks itself, which is how an older world's lone cell is found. */
	public static void checkNow(Level level, BlockPos pos, BlockState state, int every) {
		if (level instanceof ServerLevel server && (level.getGameTime() + pos.hashCode()) % every == 0) {
			check(server, pos, state);
		}
	}

	/** Only the main cell drops the item: the other pieces of a broken part drop nothing. */
	public static java.util.List<net.minecraft.world.item.ItemStack> drops(BlockState state, java.util.List<net.minecraft.world.item.ItemStack> fromLoot) {
		return state.getValue(part(state).piece()) == 0 ? fromLoot : java.util.List.of();
	}

	private static boolean isPiece(BlockState there, BlockState state, int piece) {
		return there.is(state.getBlock()) && part(state).facing(there) == part(state).facing(state) && there.getValue(part(state).piece()) == piece;
	}

	/**
	 * For {@code updateShape}: a piece whose neighbouring piece has gone goes too, so breaking any cell (or an explosion, or a
	 * piston) takes the whole part. The main cell checks the whole part on its next tick instead, so a lone main cell from an
	 * older world is not lost before it can grow.
	 */
	public static BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, BlockPos neighborPos, BlockState neighborState) {
		BlockPos main = main(pos, state);
		int[] off = {neighborPos.getX() - main.getX(), neighborPos.getY() - main.getY(), neighborPos.getZ() - main.getZ()};
		int sibling = part(state).footprint().pieceAt(off[0], off[1], off[2], turns(state));
		if (sibling < 0 || isPiece(neighborState, state, sibling)) {
			return state;
		}
		if (state.getValue(part(state).piece()) == 0) {
			ticks.scheduleTick(pos, state.getBlock(), 1);
			return state;
		}
		return Blocks.AIR.defaultBlockState();
	}

	/**
	 * The main cell's check ({@code tick}, and every so often from its block entity): a whole part is left alone; a grown part
	 * with pieces gone is broken and drops; a main cell that never grew (an older world's one-block part) grows if there is room,
	 * or drops as an item and says where.
	 */
	public static void check(ServerLevel level, BlockPos main, BlockState state) {
		if (!state.is(state.getBlock()) || state.getValue(part(state).piece()) != 0) {
			return;
		}
		int present = 0;
		int pieces = part(state).footprint().pieces();
		for (int piece = 1; piece < pieces; piece++) {
			if (isPiece(level.getBlockState(cell(main, state, piece)), state, piece)) {
				present++;
			}
		}
		if (present == pieces - 1) {
			return;
		}
		boolean grown = level.getBlockEntity(main) instanceof BigPartEntity entity && entity.grown();
		boolean grows = part(state).growsOnLoad();
		if (!grown && present == 0 && grows && roomFor(level, main, state)) {
			grow(level, main, state);
			return;
		}
		if (!grown) {
			String where = state.getBlock().getName().getString() + " at " + main.getX() + " " + main.getY() + " " + main.getZ()
					+ " is now true size (" + size(part(state).footprint()) + ") and "
					+ (grows ? "had no room, so it dropped as an item" : "its machine's layout changed, so it dropped as an item: rebuild the machine at true size (anything it held was lost)");
			Wildcatter.LOGGER.info(where);
			for (ServerPlayer player : level.players()) {
				if (player.blockPosition().closerThan(main, 128)) {
					player.sendSystemMessage(Component.literal(where));
				}
			}
		}
		level.destroyBlock(main, true);
	}

	/**
	 * For {@code playerWillDestroy} on a piece that is not the main cell: the main cell goes with it, dropping the item if the
	 * player could have harvested it.
	 */
	public static void playerBreaks(Level level, BlockPos pos, BlockState state, Player player) {
		if (level.isClientSide() || state.getValue(part(state).piece()) == 0) {
			return;
		}
		BlockPos main = main(pos, state);
		BlockState mainState = level.getBlockState(main);
		if (!isPiece(mainState, state, 0)) {
			return;
		}
		if (!player.isCreative() && player.hasCorrectToolForDrops(mainState)) {
			Block.dropResources(mainState, level, main, level.getBlockEntity(main), player, player.getMainHandItem());
		}
		level.setBlock(main, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
	}

	// ---- collision: the real shape, piece by piece --------------------------------------------------------------------------
	private static final Map<String, VoxelShape[][]> SHAPES = new HashMap<>();

	/** Piece {@code piece}'s shape for the part facing {@code turns} quarter turns from north. A part with no shape file is solid. */
	public static VoxelShape shape(BlockState state) {
		BigPart part = part(state);
		VoxelShape[][] shapes;
		synchronized (SHAPES) {
			shapes = SHAPES.computeIfAbsent(part.shapeName(), name -> load(name, part.footprint().pieces()));
		}
		return shapes[Math.floorMod(turns(state), 4)][state.getValue(part.piece())];
	}

	/** Reads {@code assets/wildcatter/shapes/<name>.json}: per piece, a list of [x1, y1, z1, x2, y2, z2] boxes in sixteenths, facing north. */
	private static VoxelShape[][] load(String name, int pieces) {
		VoxelShape[][] out = new VoxelShape[4][pieces];
		List<List<double[]>> boxes = new ArrayList<>();
		String path = "/assets/" + Wildcatter.MOD_ID + "/shapes/" + name + ".json";
		try (InputStream in = BigParts.class.getResourceAsStream(path)) {
			if (in == null) {
				throw new IllegalStateException("missing " + path);
			}
			JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			JsonArray list = json.getAsJsonArray("pieces");
			for (int piece = 0; piece < pieces; piece++) {
				List<double[]> mine = new ArrayList<>();
				if (piece < list.size()) {
					for (JsonElement box : list.get(piece).getAsJsonArray()) {
						JsonArray b = box.getAsJsonArray();
						mine.add(new double[] {b.get(0).getAsDouble(), b.get(1).getAsDouble(), b.get(2).getAsDouble(), b.get(3).getAsDouble(), b.get(4).getAsDouble(), b.get(5).getAsDouble()});
					}
				}
				boxes.add(mine);
			}
		} catch (Exception e) {
			Wildcatter.LOGGER.warn("No collision shape for {} ({}); its pieces are solid", name, e.getMessage());
			for (int t = 0; t < 4; t++) {
				java.util.Arrays.fill(out[t], Shapes.block());
			}
			return out;
		}
		for (int t = 0; t < 4; t++) {
			for (int piece = 0; piece < pieces; piece++) {
				VoxelShape shape = Shapes.empty();
				for (double[] b : boxes.get(piece)) {
					double[] r = turn(b, t);
					shape = Shapes.or(shape, Block.box(r[0], r[1], r[2], r[3], r[4], r[5]));
				}
				out[t][piece] = shape.isEmpty() ? Shapes.empty() : shape.optimize();
			}
		}
		return out;
	}

	/** A box in sixteenths turned {@code turns} quarter turns clockwise (seen from above) about the cell's centre. */
	static double[] turn(double[] b, int turns) {
		double x1 = b[0], z1 = b[2], x2 = b[3], z2 = b[5];
		for (int i = 0; i < turns; i++) {
			double nx1 = 16 - z2, nx2 = 16 - z1, nz1 = x1, nz2 = x2;
			x1 = nx1;
			x2 = nx2;
			z1 = nz1;
			z2 = nz2;
		}
		return new double[] {x1, b[1], z1, x2, b[4], z2};
	}

}
