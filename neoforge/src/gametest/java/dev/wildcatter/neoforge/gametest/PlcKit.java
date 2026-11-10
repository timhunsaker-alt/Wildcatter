package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.abs;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.check;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.put;

import dev.wildcatter.block.BoilerBlockEntity;
import dev.wildcatter.control.ControlDevice;
import dev.wildcatter.control.ControlNet;
import dev.wildcatter.control.PlcCabinetBlockEntity;
import dev.wildcatter.control.PlcModuleItem;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.plc.Cpu;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * A PLC on a server scene, built as a player builds one (docs/multiblocks/plc.md): a 120 V starter plant (or the world's standard) feeding a
 * 24 V control supply, a cabinet with its modules, control cable and junction boxes, signals landed, selectors to AUTO, a real program.
 * Coordinates are relative to the test's origin, things standing at {@code y} with a floor at {@code y - 2}.
 */
final class PlcKit {
	private PlcKit() {
	}

	/**
	 * The starter plant and supply along x from (x, y, z): a campfire under a boiler at x, its turbine at x + 1, the hot wire at x + 2, the supply
	 * at x + 3, the neutral along z + 1. The supply's 24 V leaves by its east face, at x + 4.
	 */
	static void supply(GameTestHelper helper, int x, int y, int z) {
		helper.getLevel().setBlock(abs(helper, x, y - 1, z), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 3);
		put(helper, "boiler", x, y, z);
		put(helper, "steam_turbine", x + 1, y, z);
		put(helper, "plastic_wire_hot", x + 2, y, z);
		put(helper, "control_supply", x + 3, y, z);
		for (int xx = x + 1; xx <= x + 3; xx++) {
			put(helper, "plastic_wire_neutral", xx, y, z + 1);
		}
		if (helper.getLevel().getBlockEntity(abs(helper, x, y, z)) instanceof BoilerBlockEntity boiler) {
			boiler.addWater(4000);
		}
	}

	/** A cabinet at (x, y, z) facing south, its door cell at y + 1, with these modules seated in order. */
	static PlcCabinetBlockEntity cabinet(GameTestHelper helper, int x, int y, int z, PlcModuleItem.Type... modules) {
		BlockPos at = abs(helper, x, y, z);
		BlockState state = BuiltInRegistries.BLOCK.getValue(dev.wildcatter.Wildcatter.id("plc_cabinet")).defaultBlockState();
		if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
		}
		helper.getLevel().setBlock(at, state, 3);
		BigParts.grow(helper.getLevel(), at, state);
		PlcCabinetBlockEntity cabinet = (PlcCabinetBlockEntity) helper.getLevel().getBlockEntity(at);
		for (PlcModuleItem.Type type : modules) {
			String refused = cabinet.seat(type);
			check(helper, refused == null, "the module " + type + " should seat: " + refused);
		}
		return cabinet;
	}

	/** A straight line of {@code id} from one cell to another, both included. */
	static void line(GameTestHelper helper, String id, int x1, int y1, int z1, int x2, int y2, int z2) {
		int n = Math.max(Math.abs(x2 - x1), Math.max(Math.abs(y2 - y1), Math.abs(z2 - z1)));
		for (int i = 0; i <= n; i++) {
			put(helper, id, x1 + Integer.signum(x2 - x1) * i, y1 + Integer.signum(y2 - y1) * i, z1 + Integer.signum(z2 - z1) * i);
		}
	}

	/**
	 * Lays a control cable from the box or cabinet cell at {@code from} (relative) to any cell of the device at {@code to} (relative), the
	 * shortest way through air, never past anything else a cable would join (so no bad joint): as a player routes one.
	 */
	static void cable(GameTestHelper helper, int fx, int fy, int fz, int tx, int ty, int tz) {
		net.minecraft.server.level.ServerLevel level = helper.getLevel();
		BlockPos from = abs(helper, fx, fy, fz);
		BlockPos target = ControlNet.mainOf(level, abs(helper, tx, ty, tz));
		java.util.Set<BlockPos> device = new java.util.HashSet<>(level.getBlockState(target).getBlock() instanceof dev.wildcatter.multiblock.BigPart
				? BigParts.cells(target, level.getBlockState(target)) : java.util.List.of(target));
		device.add(abs(helper, tx, ty, tz));
		java.util.Map<BlockPos, BlockPos> back = new java.util.HashMap<>();
		java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
		for (Direction side : Direction.values()) {
			BlockPos first = from.relative(side);
			if (free(level, first, from, device, false)) {
				back.put(first, from);
				queue.add(first);
			}
		}
		BlockPos end = null;
		int minX = Math.min(from.getX(), target.getX()) - 8, maxX = Math.max(from.getX(), target.getX()) + 8;
		int minY = Math.min(from.getY(), target.getY()) - 2, maxY = Math.max(from.getY(), target.getY()) + 8;
		int minZ = Math.min(from.getZ(), target.getZ()) - 8, maxZ = Math.max(from.getZ(), target.getZ()) + 8;
		while (!queue.isEmpty() && end == null) {
			BlockPos at = queue.poll();
			for (Direction side : Direction.values()) {
				if (device.contains(at.relative(side))) {
					end = at;
					break;
				}
			}
			if (end != null) {
				break;
			}
			for (Direction side : Direction.values()) {
				BlockPos next = at.relative(side);
				if (back.containsKey(next) || next.getX() < minX || next.getX() > maxX || next.getY() < minY || next.getY() > maxY || next.getZ() < minZ || next.getZ() > maxZ) {
					continue;
				}
				if (free(level, next, at, device, true)) {
					back.put(next, at);
					queue.add(next);
				}
			}
		}
		check(helper, end != null, "no way for a control cable from " + fx + ", " + fy + ", " + fz + " to " + tx + ", " + ty + ", " + tz);
		java.util.List<BlockPos> path = new java.util.ArrayList<>();
		for (BlockPos at = end; !at.equals(from); at = back.get(at)) {
			path.add(at);
		}
		BlockState cableState = BuiltInRegistries.BLOCK.getValue(dev.wildcatter.Wildcatter.id("control_cable")).defaultBlockState();
		for (BlockPos at : path) {
			level.setBlock(at, cableState, 3);
		}
		for (BlockPos at : path) {
			int touching = 0;
			for (Direction side : Direction.values()) {
				touching += ControlNet.joins(level, at.relative(side)) ? 1 : 0;
			}
			check(helper, touching <= 2, "the routed cable makes a bad joint at " + at.toShortString());
		}
	}

	/** Air, with nothing a cable joins beside it but where it came from and, at the end, the device. */
	private static boolean free(net.minecraft.server.level.ServerLevel level, BlockPos cell, BlockPos cameFrom, java.util.Set<BlockPos> device, boolean deviceOk) {
		if (!level.getBlockState(cell).isAir()) {
			return false;
		}
		for (Direction side : Direction.values()) {
			BlockPos next = cell.relative(side);
			if (next.equals(cameFrom) || device.contains(next)) {
				continue;
			}
			if (ControlNet.joins(level, next) || level.getBlockState(next).getBlock() instanceof dev.wildcatter.control.ControlCableBlock) {
				return false;
			}
		}
		return true;
	}

	static ControlDevice device(GameTestHelper helper, int x, int y, int z) {
		ControlDevice device = ControlNet.deviceAt(helper.getLevel(), abs(helper, x, y, z));
		check(helper, device != null, "no control device at " + x + ", " + y + ", " + z + ": " + helper.getLevel().getBlockState(abs(helper, x, y, z)));
		return device;
	}

	/** The device's selector stepped to AUTO (REMOTE on switchgear), as the control key does; nothing on one with no selector. */
	static void auto(GameTestHelper helper, int x, int y, int z) {
		BlockPos at = abs(helper, x, y, z);
		ControlDevice device = device(helper, x, y, z);
		for (int i = 0; i < 3; i++) {
			String now = device.cycleHoa(at);
			if (now == null || "AUTO".equals(now) || "REMOTE".equals(now)) {   // an operator device has no selector: it is always the PLC's
				return;
			}
		}
		check(helper, false, device.deviceName() + " would not go to AUTO");
	}

	/** Lands a device's point on an address, as the TERMINALS tab does; the device is found from any of its cells. */
	static void land(GameTestHelper helper, PlcCabinetBlockEntity cabinet, int x, int y, int z, int point, String address) {
		BlockPos main = ControlNet.mainOf(helper.getLevel(), abs(helper, x, y, z));
		String refused = cabinet.land(main, point, address);
		check(helper, refused == null, "point " + point + " of " + device(helper, x, y, z).deviceName() + " on " + address + ": " + refused);
	}

	/** Downloads a program of structured text and puts the CPU in RUN. */
	static void run(GameTestHelper helper, PlcCabinetBlockEntity cabinet, String program) {
		List<?> errors = cabinet.download(program);
		check(helper, errors.isEmpty(), "the program should compile: " + errors);
		cabinet.cpu().setMode(Cpu.Mode.RUN);
	}

	static Object tag(PlcCabinetBlockEntity cabinet, String name) {
		return cabinet.cpu() == null ? null : cabinet.cpu().read(name);
	}

	static boolean on(PlcCabinetBlockEntity cabinet, String name) {
		return Boolean.TRUE.equals(tag(cabinet, name));
	}

	static double number(PlcCabinetBlockEntity cabinet, String name) {
		return tag(cabinet, name) instanceof Number n ? n.doubleValue() : Double.NaN;
	}
}
