package dev.wildcatter.gametest;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.BoilerBlockEntity;
import dev.wildcatter.block.BoilerBlock;
import dev.wildcatter.block.SteamTurbineBlock;
import dev.wildcatter.block.WireShape;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.safety.Shock;
import net.minecraft.server.level.ServerPlayer;
import dev.wildcatter.block.LampBlock;
import dev.wildcatter.block.SteamTurbineBlockEntity;
import dev.wildcatter.block.TransformerBlockEntity;
import dev.wildcatter.energy.EnergyNode;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Builds a campfire -> boiler -> turbine -> two cables -> lamp, and checks the lamp lights, the water is used,
 * and that the whole chain goes quiet when the fire goes out.
 * Run with: ./gradlew runClientGameTest
 */
public class WildcatterClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			context.waitTicks(40);
			steamPlant(context, server);
		}
	}

	/**
	 * Row 10: campfire, boiler, turbine, bare cable, lamp, all 110 V. Row 14: the same through two transformers and a
	 * medium-voltage line. Then: the lamp needs two cables, the boiler must not flicker, bare wire shocks and insulated
	 * wire does not, and an ungrounded machine bites until it is grounded.
	 */
	private void steamPlant(ClientGameTestContext context, TestServerContext server) {
		int y = server.computeOnServer(srv -> srv.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 10, 10));
		int up = y + 1;
		server.runCommand("fill 6 " + y + " 6 22 " + (y + 4) + " 18 minecraft:air");
		int[] rows = {10, 14};
		for (int z : rows) {
			server.runCommand("setblock 10 " + y + " " + z + " minecraft:campfire[lit=true]");
			server.runCommand("setblock 10 " + up + " " + z + " wildcatter:boiler");
			server.runCommand("setblock 11 " + up + " " + z + " wildcatter:steam_turbine");
			server.runCommand("setblock 12 " + up + " " + z + " wildcatter:copper_cable");
		}
		// Row 10 stays at 110 V: bare wire into a lamp that only has ONE cable for now.
		server.runCommand("setblock 13 " + up + " 10 wildcatter:copper_cable");
		server.runCommand("setblock 14 " + up + " 10 wildcatter:electric_lamp");
		// Row 14 steps up to 4.16 kV, runs two pieces of line, steps back down, and ends at a lamp with two cables.
		server.runCommand("setblock 13 " + up + " 14 wildcatter:transformer[facing=east]");
		server.runCommand("setblock 14 " + up + " 14 wildcatter:medium_line");
		server.runCommand("setblock 15 " + up + " 14 wildcatter:medium_line");
		server.runCommand("setblock 16 " + up + " 14 wildcatter:transformer[facing=west]");
		server.runCommand("setblock 17 " + up + " 14 wildcatter:electric_lamp");
		server.runCommand("setblock 18 " + up + " 14 wildcatter:copper_cable");
		server.runCommand("setblock 17 " + up + " 15 wildcatter:copper_cable");
		server.runCommand("tp @p 13 " + up + " 7 0 20");
		context.waitTicks(10);

		BlockPos lamp110 = new BlockPos(14, up, 10);
		BlockPos lampFar = new BlockPos(17, up, 14);
		BlockPos cableA = new BlockPos(12, up, 10);
		BlockPos cableB = new BlockPos(13, up, 10);
		BlockPos cableMed = new BlockPos(14, up, 14);
		BlockPos transformer = new BlockPos(13, up, 14);
		BlockPos boiler = new BlockPos(10, up, 10);

		check(!lit(server, lamp110) && !lit(server, lampFar), "the lamps should be dark while the boilers are empty");

		// Cables only grow arms toward things at their own voltage.
		String arms = server.computeOnServer(srv -> {
			var a = srv.overworld().getBlockState(cableA);
			var b = srv.overworld().getBlockState(cableB);
			var m = srv.overworld().getBlockState(cableMed);
			return "A west=" + a.getValue(WireShape.ARMS.get(Direction.WEST)) + " east=" + a.getValue(WireShape.ARMS.get(Direction.EAST))
					+ " B east=" + b.getValue(WireShape.ARMS.get(Direction.EAST)) + " north=" + b.getValue(WireShape.ARMS.get(Direction.NORTH))
					+ " M west=" + m.getValue(WireShape.ARMS.get(Direction.WEST)) + " east=" + m.getValue(WireShape.ARMS.get(Direction.EAST));
		});
		check(arms.equals("A west=true east=true B east=true north=false M west=true east=true"), "cable arms are wrong: " + arms);

		// A 110 V cable and a medium-voltage line touching each other must not join.
		server.runCommand("setblock 14 " + up + " 12 wildcatter:medium_line");
		server.runCommand("setblock 13 " + up + " 12 wildcatter:copper_cable");
		context.waitTicks(5);
		String mismatch = server.computeOnServer(srv -> {
			var low = srv.overworld().getBlockState(new BlockPos(13, up, 12));
			var med = srv.overworld().getBlockState(new BlockPos(14, up, 12));
			return "low east=" + low.getValue(WireShape.ARMS.get(Direction.EAST)) + " med west=" + med.getValue(WireShape.ARMS.get(Direction.WEST));
		});
		check(mismatch.equals("low east=false med west=false"), "110 V and medium voltage must not join: " + mismatch);
		server.runCommand("setblock 14 " + up + " 12 minecraft:air");
		server.runCommand("setblock 13 " + up + " 12 minecraft:air");

		for (int z : rows) {
			int got = server.computeOnServer(srv -> ((BoilerBlockEntity) srv.overworld().getBlockEntity(new BlockPos(10, up, z))).addWater(Tuning.WATER_PER_BUCKET));
			check(got == Tuning.WATER_PER_BUCKET, "boiler took " + got + " water, not a bucket");
		}

		context.waitTicks(200);
		// One cable is a dead end: the 110 V lamp has power waiting at it but must stay dark.
		check(!lit(server, lamp110), "a lamp with only one cable must not light");
		check(lit(server, lampFar), "the lamp past two transformers has two cables and should be lit");
		server.runCommand("setblock 15 " + up + " 10 wildcatter:copper_cable");
		context.waitTicks(40);
		check(lit(server, lamp110), "the 110 V lamp should light once it has a second cable");
		context.takeScreenshot("steam-plant-running");

		// The boiler and turbine must not flicker while they run.
		int flips = server.computeOnServer(srv -> 0);
		boolean lastBoiler = server.computeOnServer(srv -> srv.overworld().getBlockState(boiler).getValue(BoilerBlock.LIT));
		boolean lastTurbine = server.computeOnServer(srv -> srv.overworld().getBlockState(new BlockPos(11, up, 10)).getValue(SteamTurbineBlock.RUNNING));
		for (int i = 0; i < 80; i++) {
			context.waitTicks(1);
			boolean b = server.computeOnServer(srv -> srv.overworld().getBlockState(boiler).getValue(BoilerBlock.LIT));
			boolean t = server.computeOnServer(srv -> srv.overworld().getBlockState(new BlockPos(11, up, 10)).getValue(SteamTurbineBlock.RUNNING));
			if (b != lastBoiler || t != lastTurbine) {
				flips++;
			}
			lastBoiler = b;
			lastTurbine = t;
		}
		check(flips == 0, "the boiler or turbine changed state " + flips + " times while running steadily");

		String t = server.computeOnServer(srv -> ((TransformerBlockEntity) srv.overworld().getBlockEntity(transformer)).inspect().getString());
		System.out.println("[wildcatter-test] " + t);
		check(!t.contains("losing 0 RF"), "the transformer should lose a little as heat: " + t);

		// Bare live wire shocks; insulated does not.
		server.runCommand("difficulty normal");
		server.runCommand("gamemode survival @p");
		float bare = shockOver(context, server, cableA);
		check(bare < 20.0F, "standing in a live bare wire should hurt, health stayed " + bare);
		server.runCommand("setblock 12 " + up + " 10 wildcatter:insulated_cable");
		server.runCommand("setblock 13 " + up + " 10 wildcatter:insulated_cable");
		context.waitTicks(60);
		float insulated = shockOver(context, server, cableA);
		check(insulated >= 20.0F, "standing in live insulated cable must be safe, health fell to " + insulated);
		check(lit(server, lamp110), "the lamp should still be lit through insulated cable");
		System.out.println("[wildcatter-test] shocks: bare wire left " + bare + " hp, insulated left " + insulated + " hp");

		// An ungrounded live machine bites when touched; a ground rod, or a ground wire to a rod, stops it.
		int bites = touches(server, lamp110);
		check(bites > 0, "an ungrounded live lamp never shocked in 200 touches");
		server.runCommand("setblock 14 " + y + " 11 minecraft:stone");
		server.runCommand("setblock 14 " + up + " 11 wildcatter:ground_rod");
		check(touches(server, lamp110) == 0, "a lamp with a ground rod against it should never shock");
		server.runCommand("setblock 14 " + up + " 11 wildcatter:ground_wire");
		server.runCommand("setblock 15 " + y + " 11 minecraft:stone");
		server.runCommand("setblock 15 " + up + " 11 wildcatter:ground_rod");
		check(touches(server, lamp110) == 0, "a lamp tied to a ground rod by ground wire should never shock");
		server.runCommand("setblock 15 " + up + " 11 minecraft:air");
		check(touches(server, lamp110) > 0, "ground wire that leads nowhere should not ground the lamp");
		System.out.println("[wildcatter-test] touch: " + bites + " bites in 200 ungrounded, none when grounded");

		// Fires out and everything stored drained: both lamps must go dark.
		server.runCommand("gamemode creative @p");
		for (int z : rows) {
			server.runCommand("setblock 10 " + y + " " + z + " minecraft:campfire[lit=false]");
		}
		server.computeOnServer(srv -> {
			for (int z : rows) {
				for (int x = 11; x <= 18; x++) {
					if (srv.overworld().getBlockEntity(new BlockPos(x, up, z)) instanceof EnergyNode node) {
						for (Direction side : Direction.values()) {
							node.energyAt(side).set(0);
						}
					}
				}
			}
			return null;
		});
		context.waitTicks(100);
		check(!lit(server, lamp110) && !lit(server, lampFar), "the lamps should go dark once the fires are out and the stored power is gone");
		System.out.println("[wildcatter-test] steam plant: lit with fire, water and two cables; dark without");
	}

	/** Puts a survival player inside a wire for a while and returns their health afterwards. */
	private float shockOver(ClientGameTestContext context, TestServerContext server, BlockPos wire) {
		server.computeOnServer(srv -> {
			ServerPlayer player = srv.getPlayerList().getPlayers().get(0);
			player.setHealth(20.0F);
			player.setInvulnerableTime(0);
			srv.overworld().setBlockAndUpdate(wire.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
			player.teleportTo(wire.getX() + 0.5, wire.getY(), wire.getZ() + 0.9); // standing right beside the wire
			return null;
		});
		float lowest = 20.0F;
		for (int i = 0; i < 40; i++) { // peaceful worlds heal almost at once, so watch the lowest health, not the last
			context.waitTicks(1);
			lowest = Math.min(lowest, server.computeOnServer(srv -> srv.getPlayerList().getPlayers().get(0).getHealth()));
		}
		float health = lowest;
		server.runCommand("setblock " + wire.getX() + " " + (wire.getY() - 1) + " " + wire.getZ() + " minecraft:air");
		server.runCommand("tp @p 13 " + (wire.getY()) + " 7 0 20");
		context.waitTicks(10);
		return health;
	}

	/** Touches the lamp 200 times as a survival player and counts how often it bit. */
	private int touches(TestServerContext server, BlockPos machine) {
		return server.computeOnServer(srv -> {
			ServerPlayer player = srv.getPlayerList().getPlayers().get(0);
			int bites = 0;
			for (int i = 0; i < 200; i++) {
				player.setHealth(20.0F);
				player.setInvulnerableTime(0);
				if (Shock.touch(srv.overworld(), machine, player, Voltage.LOW, true)) {
					bites++;
				}
			}
			player.setHealth(20.0F);
			return bites;
		});
	}

	private static boolean lit(TestServerContext server, BlockPos lamp) {
		return server.computeOnServer(srv -> srv.overworld().getBlockState(lamp).getValue(LampBlock.LIT));
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}
}
