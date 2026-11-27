package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.abs;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.check;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.put;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.BreakerBlock;
import dev.wildcatter.block.CableBlock;
import dev.wildcatter.block.LoadBlock;
import dev.wildcatter.block.MultiCableBlockEntity;
import dev.wildcatter.block.PipeBlock;
import dev.wildcatter.block.WireBlockEntity;
import dev.wildcatter.block.WireShape;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.ConductorSize;
import dev.wildcatter.item.ConductorItem;
import dev.wildcatter.quest.QuestData;
import dev.wildcatter.quest.QuestTracker;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Wires, cables and pipes that read as connected (2026-11-27):
 * <ul>
 * <li>{@code single_phase_cable_lamp}: single-phase cable, through a breaker, lights a lamp from a campfire turbine and finishes Light a lamp;</li>
 * <li>{@code single_phase_cable_one_conductor}: a single wire joins the cable on its own conductor only (A and N yes, B and C no);</li>
 * <li>{@code wire_undersized}: a 12 AWG wire feeding two heaters says it is undersized and names 10 AWG, and keeps its size;</li>
 * <li>{@code fittings_on_placement}: placed from sized stacks next to machines, wire and cable take their gauge and their arms (and a
 * terminal side), and a pipe a flange at the boiler's nozzle and a plain joint to the next pipe.</li>
 * </ul>
 */
final class WireLookScenes {
	private WireLookScenes() {
	}

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> out = new LinkedHashMap<>();
		out.put("single_phase_cable_lamp", WireLookScenes::lamp);
		out.put("single_phase_cable_one_conductor", WireLookScenes::oneConductor);
		out.put("wire_undersized", WireLookScenes::undersized);
		out.put("fittings_on_placement", WireLookScenes::fittings);
		return out;
	}

	private static void ground(GameTestHelper helper) {
		for (int x = -1; x <= 7; x++) {
			for (int z = -1; z <= 6; z++) {
				helper.setBlock(x, 0, z, Blocks.STONE);
			}
		}
	}

	private static ItemStack sized(String id, ConductorSize size, int count) {
		ItemStack stack = ((ConductorItem) net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(dev.wildcatter.Wildcatter.id(id))).stack(size);
		stack.setCount(count);
		return stack;
	}

	private static void plant(GameTestHelper helper, boolean firebox) {
		if (firebox) {
			helper.setBlock(0, 1, 0, WildcatterBlocks.FIREBOX);
			helper.getBlockEntity(new BlockPos(0, 1, 0), dev.wildcatter.block.FireboxBlockEntity.class).setItem(0, new ItemStack(Items.COAL, 64));
		} else {
			helper.setBlock(0, 1, 0, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
		}
		helper.setBlock(0, 2, 0, WildcatterBlocks.BOILER);
		helper.setBlock(1, 2, 0, WildcatterBlocks.STEAM_TURBINE);
		helper.getBlockEntity(new BlockPos(0, 2, 0), dev.wildcatter.block.BoilerBlockEntity.class).addWater(4000);
	}

	/** Turbine, cable, a breaker in the run, cable, lamp: the cable's neutral passes the breaker, and the lamp lights on its hot and neutral. */
	static void lamp(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ground(helper);
		plant(helper, false);
		put(helper, "single_phase_cable", 2, 2, 0);
		helper.setBlock(3, 2, 0, WildcatterBlocks.BREAKER.defaultBlockState().setValue(BreakerBlock.FACING, Direction.EAST));
		put(helper, "single_phase_cable", 4, 2, 0);
		helper.setBlock(5, 2, 0, WildcatterBlocks.ELECTRIC_LAMP);
		ServerPlayer player = QuestScenes.player(helper, 3, 1, 3);
		player.getInventory().add(new ItemStack(WildcatterBlocks.BOILER));
		player.getInventory().add(new ItemStack(WildcatterBlocks.STEAM_TURBINE));
		player.getInventory().add(new ItemStack(WildcatterBlocks.ELECTRIC_LAMP));
		player.getInventory().add(sized("single_phase_cable", ConductorSize.AWG_12, 4));
		helper.succeedWhen(() -> {
			check(helper, helper.getBlockState(new BlockPos(5, 2, 0)).getValue(LoadBlock.LIT), "the lamp should light on single-phase cable: " + GridSceneKit.read(helper, 4, 2, 0));
			MultiCableBlockEntity cable = helper.getBlockEntity(new BlockPos(4, 2, 0), MultiCableBlockEntity.class);
			check(helper, cable.amps(Conductor.A) > 0.2 && Math.abs(cable.amps(Conductor.N) - cable.amps(Conductor.A)) < 0.05,
					"the lamp's current goes out on the hot and back on the neutral: " + cable.inspect().getString());
			BlockState first = helper.getBlockState(new BlockPos(2, 2, 0));
			check(helper, first.getValue(WireShape.ARMS.get(Direction.WEST)) && first.getValue(WireShape.ARMS.get(Direction.EAST)), "the cable joins the turbine and the breaker");
			int terminals = WireShape.terminals(helper.getLevel(), abs(helper, 2, 2, 0), first);
			check(helper, terminals == (1 << Direction.WEST.ordinal() | 1 << Direction.EAST.ordinal()), "the cable lands on terminals at the turbine and the breaker: " + terminals);
			QuestTracker.check(player);
			check(helper, QuestData.get(server).of(player.getStringUUID()).done.contains("wildcatter:lamp"), "carrying single-phase cable, Light a lamp should be done");
			QuestScenes.leave(helper, player);
			log("single_phase_cable_lamp", "ok: " + cable.inspect().getString());
		});
	}

	static void oneConductor(GameTestHelper helper) {
		ground(helper);
		put(helper, "single_phase_cable", 2, 1, 2);
		put(helper, "wire_hot", 3, 1, 2);                                     // A
		put(helper, "wire_neutral", 2, 1, 1);                                 // N
		helper.setBlock(1, 1, 2, WildcatterBlocks.WIRE_HOT.defaultBlockState().setValue(CableBlock.CONDUCTOR, Conductor.B));
		helper.setBlock(2, 1, 3, WildcatterBlocks.WIRE_HOT.defaultBlockState().setValue(CableBlock.CONDUCTOR, Conductor.C));
		helper.runAfterDelay(5, () -> {
			BlockState cable = helper.getBlockState(new BlockPos(2, 1, 2));
			check(helper, cable.getValue(WireShape.ARMS.get(Direction.EAST)), "a hot (A) wire joins the cable");
			check(helper, cable.getValue(WireShape.ARMS.get(Direction.NORTH)), "a neutral wire joins the cable");
			check(helper, !cable.getValue(WireShape.ARMS.get(Direction.WEST)), "a phase B wire does not join single-phase cable");
			check(helper, !cable.getValue(WireShape.ARMS.get(Direction.SOUTH)), "a phase C wire does not join single-phase cable");
			check(helper, helper.getBlockState(new BlockPos(3, 1, 2)).getValue(WireShape.ARMS.get(Direction.WEST)), "the A wire reaches into the cable");
			check(helper, !helper.getBlockState(new BlockPos(1, 1, 2)).getValue(WireShape.ARMS.get(Direction.EAST)), "the B wire draws no arm to it");
			MultiCableBlockEntity entity = helper.getBlockEntity(new BlockPos(2, 1, 2), MultiCableBlockEntity.class);
			check(helper, entity.hasPort(Direction.EAST, Conductor.A, dev.wildcatter.energy.Voltage.LOW) && entity.hasPort(Direction.EAST, Conductor.G, dev.wildcatter.energy.Voltage.LOW)
					&& !entity.hasPort(Direction.EAST, Conductor.B, dev.wildcatter.energy.Voltage.LOW), "single-phase cable has terminals for A, N and G only");
			check(helper, WireShape.terminals(helper.getLevel(), abs(helper, 2, 1, 2), cable) == 0, "wire to cable is a run, not a terminal");
			log("single_phase_cable_one_conductor", "ok");
			helper.succeed();
		});
	}

	/** Two 1,500 W heaters on a 12 AWG (20 A) hot wire: about 25 A. */
	static void undersized(GameTestHelper helper) {
		ground(helper);
		plant(helper, true);
		for (int x = 2; x <= 4; x++) {
			put(helper, "wire_hot", x, 2, 0);
			put(helper, "wire_neutral", x, 2, 2);
		}
		put(helper, "wire_neutral", 1, 2, 1);
		put(helper, "wire_neutral", 1, 2, 2);
		put(helper, "electric_heater", 3, 2, 1);
		put(helper, "electric_heater", 4, 2, 1);
		String plain = ConductorSize.AWG_12.undersized(48);
		check(helper, plain.equals("UNDERSIZED: carrying 48 A; needs 6 AWG or bigger"), "48 A on 12 AWG needs 6 AWG: " + plain);
		check(helper, ConductorSize.AWG_12.undersized(19).isEmpty(), "19 A on 12 AWG is within its rating");
		check(helper, ConductorSize.KCMIL_500.undersized(500).contains("more than any one 500 kcmil"), "nothing single carries 500 A of building wire");
		helper.runAfterDelay(400, () -> {
			WireBlockEntity wire = helper.getBlockEntity(new BlockPos(2, 2, 0), WireBlockEntity.class);
			String reading = wire.inspect().getString();
			double amps = Double.parseDouble(reading.replaceAll("(?s).*?([0-9.]+) A of 20.*", "$1"));
			check(helper, amps > 20 && amps <= 30, "two heaters should put 20 to 30 A on the hot wire: " + reading);
			check(helper, reading.startsWith("UNDERSIZED: carrying ") && reading.contains(" A; needs 10 AWG or bigger."), "the readout names 10 AWG: " + reading);
			check(helper, wire.size() == ConductorSize.AWG_12, "a wire never resizes itself: " + wire.size().label());
			log("wire_undersized", "ok: " + reading);
			helper.succeed();
		});
	}

	private static void placeFrom(GameTestHelper helper, net.minecraft.world.entity.player.Player player, ItemStack stack, int x, int z) {
		BlockPos floorAt = abs(helper, x, 0, z);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
		var result = player.getMainHandItem().useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(),
				new BlockHitResult(Vec3.atCenterOf(floorAt).add(0, 0.5, 0), Direction.UP, floorAt, false)));
		check(helper, result.consumesAction(), "placing " + stack + " at " + x + ", " + z + ": " + result);
	}

	static void fittings(GameTestHelper helper) {
		ground(helper);
		var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
		helper.setBlock(1, 1, 1, WildcatterBlocks.ELECTRIC_LAMP);
		placeFrom(helper, player, sized("single_phase_cable", ConductorSize.AWG_4_0, 1), 2, 1);
		helper.setBlock(1, 1, 3, WildcatterBlocks.STEAM_TURBINE);
		placeFrom(helper, player, sized("wire_hot", ConductorSize.AWG_2, 1), 2, 3);
		placeFrom(helper, player, sized("wire_hot", ConductorSize.KCMIL_500, 1), 3, 3);
		helper.setBlock(1, 1, 5, WildcatterBlocks.BOILER);
		placeFrom(helper, player, new ItemStack(WildcatterBlocks.PIPE), 2, 5);
		placeFrom(helper, player, new ItemStack(WildcatterBlocks.PIPE), 3, 5);
		helper.runAfterDelay(5, () -> {
			BlockState cable = helper.getBlockState(new BlockPos(2, 1, 1));
			check(helper, cable.getValue(WireShape.GAUGE) == ConductorSize.Gauge.LARGE, "4/0 cable is drawn large: " + cable);
			check(helper, cable.getValue(WireShape.ARMS.get(Direction.WEST)), "the cable reaches the lamp");
			check(helper, WireShape.terminals(helper.getLevel(), abs(helper, 2, 1, 1), cable) == 1 << Direction.WEST.ordinal(), "and lands on its terminal");
			check(helper, helper.getBlockEntity(new BlockPos(2, 1, 1), MultiCableBlockEntity.class).size() == ConductorSize.AWG_4_0, "the cable keeps the stack's size");
			BlockState wire = helper.getBlockState(new BlockPos(2, 1, 3));
			check(helper, wire.getValue(WireShape.GAUGE) == ConductorSize.Gauge.MEDIUM && wire.getValue(WireShape.ARMS.get(Direction.WEST)), "2 AWG wire is medium and reaches the turbine: " + wire);
			check(helper, WireShape.terminals(helper.getLevel(), abs(helper, 2, 1, 3), wire) == 1 << Direction.WEST.ordinal(), "a terminal at the turbine, none at the next wire");
			check(helper, helper.getBlockState(new BlockPos(3, 1, 3)).getValue(WireShape.GAUGE) == ConductorSize.Gauge.VERY_LARGE, "500 kcmil is drawn very large");
			BlockState pipe = helper.getBlockState(new BlockPos(2, 1, 5));
			check(helper, PipeBlock.end(pipe, Direction.WEST) == PipeBlock.End.FLANGE, "the pipe is flanged to the boiler's nozzle: " + pipe);
			check(helper, PipeBlock.end(pipe, Direction.EAST) == PipeBlock.End.PIPE, "and joined plain to the next pipe: " + pipe);
			check(helper, PipeBlock.end(helper.getBlockState(new BlockPos(3, 1, 5)), Direction.WEST) == PipeBlock.End.PIPE, "the next pipe joins back");
			log("fittings_on_placement", "ok");
			helper.succeed();
		});
	}
}
