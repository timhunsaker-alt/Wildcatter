package dev.wildcatter.gametest;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.BoilerBlockEntity;
import dev.wildcatter.block.CableBlock;
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

	/** Row 10: campfire, boiler, turbine, two cables, lamp, all 110 V. Row 14: the same but through two transformers and a medium-voltage line. */
	private void steamPlant(ClientGameTestContext context, TestServerContext server) {
		int y = server.computeOnServer(srv -> srv.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 10, 10));
		server.runCommand("fill 6 " + y + " 6 20 " + (y + 4) + " 18 minecraft:air");
		int[] rows = {10, 14};
		for (int z : rows) {
			server.runCommand("setblock 10 " + y + " " + z + " minecraft:campfire[lit=true]");
			server.runCommand("setblock 10 " + (y + 1) + " " + z + " wildcatter:boiler");
			server.runCommand("setblock 11 " + (y + 1) + " " + z + " wildcatter:steam_turbine");
			server.runCommand("setblock 12 " + (y + 1) + " " + z + " wildcatter:copper_cable");
		}
		// Row 10 stays at 110 V.
		server.runCommand("setblock 13 " + (y + 1) + " 10 wildcatter:copper_cable");
		server.runCommand("setblock 14 " + (y + 1) + " 10 wildcatter:electric_lamp");
		// Row 14 steps up to 4.16 kV, runs two pieces of line, steps back down, and ends at a lamp.
		server.runCommand("setblock 13 " + (y + 1) + " 14 wildcatter:transformer[facing=east]");
		server.runCommand("setblock 14 " + (y + 1) + " 14 wildcatter:medium_cable");
		server.runCommand("setblock 15 " + (y + 1) + " 14 wildcatter:medium_cable");
		server.runCommand("setblock 16 " + (y + 1) + " 14 wildcatter:transformer[facing=west]");
		server.runCommand("setblock 17 " + (y + 1) + " 14 wildcatter:electric_lamp");
		server.runCommand("tp @p 13 " + (y + 1) + " 7 0 20");
		context.waitTicks(10);

		BlockPos lamp110 = new BlockPos(14, y + 1, 10);
		BlockPos lampFar = new BlockPos(17, y + 1, 14);
		BlockPos cableA = new BlockPos(12, y + 1, 10);
		BlockPos cableB = new BlockPos(13, y + 1, 10);
		BlockPos cableMed = new BlockPos(14, y + 1, 14);
		BlockPos transformer = new BlockPos(13, y + 1, 14);

		check(!lit(server, lamp110) && !lit(server, lampFar), "the lamps should be dark while the boilers are empty");

		// Cables only grow arms toward things at their own voltage.
		String arms = server.computeOnServer(srv -> {
			var a = srv.overworld().getBlockState(cableA);
			var b = srv.overworld().getBlockState(cableB);
			var m = srv.overworld().getBlockState(cableMed);
			return "A west=" + a.getValue(CableBlock.ARMS.get(Direction.WEST)) + " east=" + a.getValue(CableBlock.ARMS.get(Direction.EAST))
					+ " B east=" + b.getValue(CableBlock.ARMS.get(Direction.EAST)) + " north=" + b.getValue(CableBlock.ARMS.get(Direction.NORTH))
					+ " M west=" + m.getValue(CableBlock.ARMS.get(Direction.WEST)) + " east=" + m.getValue(CableBlock.ARMS.get(Direction.EAST));
		});
		check(arms.equals("A west=true east=true B east=true north=false M west=true east=true"), "cable arms are wrong: " + arms);

		// A 110 V cable and a medium-voltage line touching each other must not join.
		server.runCommand("setblock 14 " + (y + 1) + " 12 wildcatter:medium_cable");
		server.runCommand("setblock 13 " + (y + 1) + " 12 wildcatter:copper_cable");
		context.waitTicks(5);
		String mismatch = server.computeOnServer(srv -> {
			var low = srv.overworld().getBlockState(new BlockPos(13, y + 1, 12));
			var med = srv.overworld().getBlockState(new BlockPos(14, y + 1, 12));
			return "low east=" + low.getValue(CableBlock.ARMS.get(Direction.EAST)) + " med west=" + med.getValue(CableBlock.ARMS.get(Direction.WEST));
		});
		check(mismatch.equals("low east=false med west=false"), "110 V and medium voltage must not join: " + mismatch);

		for (int z : rows) {
			int got = server.computeOnServer(srv -> ((BoilerBlockEntity) srv.overworld().getBlockEntity(new BlockPos(10, y + 1, z))).addWater(Tuning.WATER_PER_BUCKET));
			check(got == Tuning.WATER_PER_BUCKET, "boiler took " + got + " water, not a bucket");
		}

		context.waitTicks(200);
		context.takeScreenshot("steam-plant-running");

		check(lit(server, lamp110), "the 110 V lamp should be lit");
		check(lit(server, lampFar), "the lamp past two transformers should be lit");
		String t = server.computeOnServer(srv -> ((TransformerBlockEntity) srv.overworld().getBlockEntity(transformer)).inspect().getString());
		System.out.println("[wildcatter-test] " + t);
		check(!t.contains("losing 0 RF"), "the transformer should lose a little as heat: " + t);
		String turbine = server.computeOnServer(srv -> ((SteamTurbineBlockEntity) srv.overworld().getBlockEntity(new BlockPos(11, y + 1, 10))).inspect().getString());
		System.out.println("[wildcatter-test] " + turbine);
		String boilerState = server.computeOnServer(srv -> ((BoilerBlockEntity) srv.overworld().getBlockEntity(new BlockPos(10, y + 1, 10))).inspect().getString());
		check(!boilerState.contains("water 1000/"), "the boiler should have used some water: " + boilerState);
		System.out.println("[wildcatter-test] " + boilerState);

		// Fires out and everything stored drained: both lamps must go dark.
		for (int z : rows) {
			server.runCommand("setblock 10 " + y + " " + z + " minecraft:campfire[lit=false]");
		}
		server.computeOnServer(srv -> {
			for (int z : rows) {
				for (int x = 11; x <= 17; x++) {
					if (srv.overworld().getBlockEntity(new BlockPos(x, y + 1, z)) instanceof EnergyNode node) {
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
		System.out.println("[wildcatter-test] steam plant: both lamps lit with fire and water (one through two transformers), dark without");
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
