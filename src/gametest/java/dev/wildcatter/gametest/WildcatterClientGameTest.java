package dev.wildcatter.gametest;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.item.SeismographItem;
import dev.wildcatter.well.Reservoir;
import dev.wildcatter.well.ReservoirData;
import dev.wildcatter.block.BoilerBlock;
import dev.wildcatter.block.BoilerBlockEntity;
import dev.wildcatter.block.BreakerBlock;
import dev.wildcatter.block.BreakerBlockEntity;
import dev.wildcatter.block.LoadBlock;
import dev.wildcatter.block.SteamTurbineBlock;
import dev.wildcatter.block.WireBlockEntity;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.safety.Shock;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Builds real circuits in a real world and checks they behave like real circuits: a loop is needed, amps add up on a
 * breaker, a short circuit trips it (or burns the wire if there is none), worn wool shocks and shorts, and a transformer
 * chain steps the voltage up and back down.
 * Run with: ./gradlew runClientGameTest
 */
public class WildcatterClientGameTest implements FabricClientGameTest {
	private int y;
	private int up;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			context.waitTicks(40);
			y = server.computeOnServer(srv -> srv.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 10, 10));
			up = y + 1;
			server.runCommand("fill 6 " + y + " 6 24 " + (y + 4) + " 20 minecraft:air");
			server.runCommand("difficulty normal");
			lowVoltageLoop(context, server);
			transformerChain(context, server);
			bigMachines(context, server);
			steamLoop(context, server);
			refining(context, server);
			fieldManual(server);
		}
	}

	// ---- helpers ---------------------------------------------------------------------------------------------
	private void put(TestServerContext server, int x, int yy, int z, String block) {
		server.runCommand("setblock " + x + " " + yy + " " + z + " " + block);
	}

	private void plant(TestServerContext server, int z) {
		put(server, 10, y, z, "minecraft:campfire[lit=true]");
		put(server, 10, up, z, "wildcatter:boiler");
		put(server, 11, up, z, "wildcatter:steam_turbine");
		server.computeOnServer(srv -> ((BoilerBlockEntity) srv.overworld().getBlockEntity(new BlockPos(10, up, z))).addWater(4000));
	}

	private boolean lit(TestServerContext server, int x, int z) {
		return server.computeOnServer(srv -> srv.overworld().getBlockState(new BlockPos(x, up, z)).getValue(LoadBlock.LIT));
	}

	private BreakerBlock.Position breaker(TestServerContext server, int x, int z) {
		return server.computeOnServer(srv -> srv.overworld().getBlockState(new BlockPos(x, up, z)).getValue(BreakerBlock.POSITION));
	}

	private String read(TestServerContext server, int x, int yy, int z) {
		return server.computeOnServer(srv -> srv.overworld().getBlockEntity(new BlockPos(x, yy, z)) instanceof Inspectable i
				? i.inspect().getString() : "(no readout)");
	}

	private boolean isAir(TestServerContext server, int x, int yy, int z) {
		return server.computeOnServer(srv -> srv.overworld().getBlockState(new BlockPos(x, yy, z)).isAir());
	}

	private void resetBreaker(TestServerContext server, int x, int z) {
		server.computeOnServer(srv -> {
			((BreakerBlockEntity) srv.overworld().getBlockEntity(new BlockPos(x, up, z))).setSwitch(true);
			return null;
		});
	}

	/** The wires of the 110 V loop: hot along z=10, neutral along z=11, in wool so they can run side by side. */
	private void hotLine(TestServerContext server, String wire) {
		put(server, 12, up, 10, wire);
		put(server, 14, up, 10, wire);
	}

	private void neutralLine(TestServerContext server, String wire) {
		for (int x = 11; x <= 15; x++) {
			put(server, x, up, 11, wire);
		}
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}

	// ---- the 3x3x3 machines ------------------------------------------------------------------------------------
	private void bigMachines(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("fill 27 " + (y - 1) + " 26 42 " + (y + 6) + " 36 minecraft:air");
		server.runCommand("fill 27 " + (y - 1) + " 26 42 " + (y - 1) + " 36 minecraft:stone");
		server.runCommand("tp @p 34 " + up + " 24 0 20");
		// 9 fireboxes, with 27 boilers on top of them
		for (int x = 30; x <= 32; x++) {
			for (int z = 30; z <= 32; z++) {
				put(server, x, y, z, "wildcatter:firebox");
				for (int h = 0; h < 3; h++) {
					put(server, x, up + h, z, "wildcatter:boiler");
				}
			}
		}
		// 27 steam turbines right next to them, and a transformer + lamp on the generator's far side
		for (int x = 33; x <= 35; x++) {
			for (int z = 30; z <= 32; z++) {
				for (int h = 0; h < 3; h++) {
					put(server, x, up + h, z, "wildcatter:steam_turbine");
				}
			}
		}
		put(server, 36, up, 31, "wildcatter:transformer[facing=west]");
		put(server, 37, up, 31, "wildcatter:electric_lamp");
		context.waitTicks(60);   // they find each other
		server.computeOnServer(srv -> {
			BoilerBlockEntity boiler = (BoilerBlockEntity) srv.overworld().getBlockEntity(new BlockPos(31, up + 1, 31));
			for (int i = 0; i < 30; i++) {
				boiler.addWater(1000);
			}
			return null;
		});
		// Fuel: a hopper feeds the side of one firebox, and a bit is put straight into another (they share one store)
		put(server, 29, y, 31, "minecraft:hopper[facing=east]");
		server.computeOnServer(srv -> {
			((Container) srv.overworld().getBlockEntity(new BlockPos(29, y, 31))).setItem(0, new ItemStack(Items.COAL, 8));
			((Container) srv.overworld().getBlockEntity(new BlockPos(32, y, 32))).setItem(0, new ItemStack(Items.COAL, 4));
			return null;
		});
		context.waitTicks(100);
		check(server.computeOnServer(srv -> ((Container) srv.overworld().getBlockEntity(new BlockPos(29, y, 31))).isEmpty()),
				"the hopper should have fed its coal into the firebox");
		String fire = read(server, 31, y, 30);
		check(fire.contains("Big firebox") && fire.contains("burning"), "9 fireboxes with coal should form one big burning firebox: " + fire);

		String coil = read(server, 31, up + 1, 31);   // any boiler of the cube answers for the whole thing
		check(coil.contains("Coil boiler") && coil.contains("9 of 9"), "27 boilers over 9 burning fireboxes should form a coil boiler: " + coil);
		check(coil.contains("/108000"), "the coil boiler should hold 27 boilers of water: " + coil);
		check(server.computeOnServer(srv -> srv.overworld().getBlockState(new BlockPos(32, up + 2, 32)).getValue(BoilerBlock.FORMED)),
				"the boilers of a coil boiler should switch to the coil look");
		String gen = read(server, 34, up + 1, 31);
		check(gen.contains("Medium voltage generator"), "27 turbines in a cube should form a medium voltage generator: " + gen);
		check(server.computeOnServer(srv -> srv.overworld().getBlockState(new BlockPos(35, up + 2, 30)).getValue(SteamTurbineBlock.FORMED)),
				"the turbines of the generator should switch to the generator look");
		check(read(server, 36, up, 31).contains("4"), "the transformer should see the generator's medium voltage: " + read(server, 36, up, 31));
		check(lit(server, 37, 31), "a lamp behind a transformer on the medium voltage generator should light: " + read(server, 37, up, 31));
		String lamp = read(server, 37, up, 31);
		context.takeScreenshot("wildcatter-big-machines");

		wells(context, server);   // the generator is running: its transformer feeds the rig

		// Pull out a boiler from the middle layer: the coil boiler falls apart, and the blocks go back to normal.
		server.runCommand("setblock 31 " + (up + 1) + " 31 minecraft:air");
		context.waitTicks(60);
		String broken = read(server, 31, up, 31);
		check(!broken.contains("Coil boiler"), "a coil boiler with a boiler missing must fall apart: " + broken);
		check(!server.computeOnServer(srv -> srv.overworld().getBlockState(new BlockPos(32, up + 2, 32)).getValue(BoilerBlock.FORMED)),
				"the leftover boilers should go back to the plain look");
		System.out.println("[wildcatter-test] big machines ok: " + fire + " | " + coil + " | " + gen + " | " + lamp);
	}

	/** Pump, pipes and drain: a pump feeds a second boiler through pipes, and the turbine's drain brings its water home. */
	private void steamLoop(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("fill 46 " + (y - 1) + " 44 58 " + (y + 4) + " 54 minecraft:air");
		server.runCommand("fill 46 " + (y - 1) + " 44 58 " + (y - 1) + " 54 minecraft:stone");
		server.runCommand("tp @p 52 " + up + " 56 180 20");
		put(server, 50, y, 50, "minecraft:campfire[lit=true]");
		put(server, 50, up, 50, "wildcatter:boiler");
		put(server, 51, up, 50, "wildcatter:steam_turbine");
		put(server, 52, up, 50, "wildcatter:wool_wire_hot");
		put(server, 53, up, 50, "wildcatter:pump");
		for (int x = 51; x <= 53; x++) {
			put(server, x, up, 51, "wildcatter:wool_wire_neutral");
		}
		// the pump drinks from a pond that is walled in so it cannot run away
		put(server, 55, up, 50, "minecraft:stone");
		put(server, 54, up, 49, "minecraft:stone");
		put(server, 54, up, 51, "minecraft:stone");
		put(server, 54, up + 1, 50, "minecraft:stone");
		put(server, 54, up, 50, "minecraft:water");
		// its pipes run to a second boiler
		put(server, 53, up, 49, "wildcatter:pipe");
		put(server, 53, up, 48, "wildcatter:pipe");
		put(server, 53, up, 47, "wildcatter:boiler");
		server.computeOnServer(srv -> ((BoilerBlockEntity) srv.overworld().getBlockEntity(new BlockPos(50, up, 50))).addWater(4000));
		context.waitTicks(120);

		String pump = read(server, 53, up, 50);
		check(pump.contains("pumping") || pump.contains("boilers are full"), "a wired pump touching water with a pipe to a boiler should pump: " + pump);
		String tank = read(server, 53, up, 47);
		check(tank.matches(".*water [1-9]\\d*/.*"), "the pipe should have carried water into the second boiler: " + tank);
		String noDrain = read(server, 51, up, 50);
		check(noDrain.contains("no drain pipe"), "a turbine with no pipe should say it has no drain: " + noDrain);

		// The drain: a pipe touching the turbine that reaches the boiler it takes steam from
		put(server, 51, up, 49, "wildcatter:pipe");
		put(server, 50, up, 49, "wildcatter:pipe");
		context.waitTicks(60);
		String drain = read(server, 51, up, 50);
		check(drain.contains("drain connected"), "a pipe on the turbine should connect its drain: " + drain);
		int before = waterIn(server, 50, up, 50);
		context.waitTicks(400);
		int after = waterIn(server, 50, up, 50);
		check(before - after <= 4, "with the drain, the boiler should get its condensate back: " + before + " -> " + after);
		context.takeScreenshot("wildcatter-pump-and-pipes");
		System.out.println("[wildcatter-test] steam loop ok: " + pump + " | " + tank + " | " + drain + " | boiler water " + before + " -> " + after);

		// The cooling tower, built from small parts: a fan on top of the turbine (powered through it), two fill blocks on the fan,
		// and a pipe taking the fan to the drain.
		check(drain.contains("NO COOLING TOWER"), "a turbine without a tower should say so: " + drain);
		put(server, 51, up + 1, 50, "wildcatter:cooling_tower");
		put(server, 51, up + 1, 49, "wildcatter:pipe");
		put(server, 51, up + 2, 50, "wildcatter:cooling_fill");
		context.waitTicks(60);
		check(read(server, 51, up + 1, 50).contains("needs 2 fill"), "a fan with one fill section is not a tower: " + read(server, 51, up + 1, 50));
		put(server, 51, up + 3, 50, "wildcatter:cooling_fill");
		context.waitTicks(80);
		String fan = read(server, 51, up + 1, 50);
		String cooled = read(server, 51, up, 50);
		check(fan.contains("COOLING"), "a powered fan with two fill sections should be cooling: " + fan);
		check(cooled.contains("condensate cooled"), "the turbine should know its condensate is cooled: " + cooled);
		server.runCommand("tp @p 53 " + (up + 3) + " 56 180 20");
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-cooling-tower");
		System.out.println("[wildcatter-test] cooling ok: " + fan + " | " + cooled);

		// A fuse in the hot wire between turbine and pump: a short blows it, and it stays blown until it gets a copper ingot.
		put(server, 52, up, 50, "wildcatter:fuse[facing=east]");
		context.waitTicks(40);
		check(read(server, 52, up, 50).contains("good"), "a fuse with a healthy load should be good: " + read(server, 52, up, 50));
		put(server, 53, up + 1, 50, "wildcatter:wire_hot");
		put(server, 53, up + 1, 51, "wildcatter:wire_neutral");
		context.waitTicks(40);
		String blown = read(server, 52, up, 50);
		check(blown.contains("BLOWN"), "a short circuit should blow the fuse: " + blown);
		put(server, 53, up + 1, 50, "minecraft:air");
		put(server, 53, up + 1, 51, "minecraft:air");
		server.runCommand("item replace entity @p weapon.mainhand with minecraft:copper_ingot");
		server.runCommand("tp @p 52.5 " + (up + 3) + " 52.4 180 60");
		context.waitTicks(10);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.FuseBlockEntity) srv.overworld().getBlockEntity(new BlockPos(52, up, 50))).replaceElement();
			return null;
		});
		context.waitTicks(40);
		check(read(server, 52, up, 50).contains("good"), "a new element should make the fuse good again: " + read(server, 52, up, 50));
		System.out.println("[wildcatter-test] fuse ok: " + blown);

		// A plain switch in the same place: closed it carries current, open it cuts the pump off, and it never trips.
		put(server, 52, up, 50, "wildcatter:switch[facing=east]");
		context.waitTicks(40);
		String closed = read(server, 52, up, 50);
		check(closed.contains("CLOSED"), "a switch should start closed: " + closed);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.BreakerBlockEntity) srv.overworld().getBlockEntity(new BlockPos(52, up, 50))).setSwitch(false);
			return null;
		});
		context.waitTicks(40);
		String open = read(server, 52, up, 50);
		check(open.contains("OPEN") && open.contains("NO protection"), "an open switch should say so: " + open);
		check(read(server, 53, up, 50).contains("NO POWER"), "an open switch should cut the pump off: " + read(server, 53, up, 50));
		System.out.println("[wildcatter-test] switch ok: " + closed + " | " + open);
	}

	/**
	 * Oil and gas. The test picks a world seed that puts an oil field under the lamp of the big generator (so the
	 * rig has 110 V) and a gas field somewhere near, then drills the oil well with the rig and opens the gas well onto a firebox.
	 */
	private void wells(ClientGameTestContext context, TestServerContext server) {
		// bigMachines ended with the generator still running: its transformer feeds whatever sits where the lamp was
		int rx = 37, rz = 31;
		int gx = 0, gz = 0;
		long found = -1;
		search:
		for (long seed = 1; seed < 2_000_000; seed++) {
			Reservoir oil = Reservoir.at(seed, rx, rz);
			if (oil == null || Reservoir.at(seed, rx + 1, rz) == null || oil.kind() != Reservoir.Kind.OIL || oil.topY() > -42) {
				continue;
			}
			for (int x = 20; x <= 60; x += 2) {
				for (int z = 40; z <= 70; z += 2) {
					Reservoir gas = Reservoir.at(seed, x, z);
					if (gas != null && gas.kind() == Reservoir.Kind.GAS && gas.units() > 0 && !gas.key().equals(oil.key())) {
						found = seed;
						gx = x;
						gz = z;
						break search;
					}
				}
			}
		}
		check(found >= 0, "no seed puts an oil field under the rig and a gas field nearby");
		ReservoirData.testSeed = found;
		final int fgx = gx, fgz = gz;
		final long seed = found;
		try {
			String echo = server.computeOnServer(srv -> SeismographItem.report(srv.overworld(), rx + 0.5, rz + 0.5));
			check(echo.contains("STRONG ECHO"), "a seismograph over a field should hear it: " + echo);
			String far = server.computeOnServer(srv -> SeismographItem.report(srv.overworld(), fgx + 300.5, fgz + 300.5));
			check(far.contains("echo") || far.contains("solid rock"), "a seismograph should say something anywhere: " + far);

			// ---- the oil well: the world is flat, so the rig stands on a tall pole of wool wire (hot up the back of the
			// transformer, neutral up from its underside) to have some rock to drill through ----
			int top = up + 30;
			int dx = rx + 1;   // the rig stands beside the hot pole so its hole does not eat the wire
			server.runCommand("tp @p 36 " + up + " 24 0 20");
			for (int h = up; h <= top + 1; h++) {
				put(server, rx, h, rz, "wildcatter:wool_wire_hot");
				put(server, rx, h, rz + 2, "wildcatter:wool_wire_neutral");
			}
			put(server, rx - 1, up - 1, rz, "wildcatter:wool_wire_neutral");
			put(server, rx, up - 1, rz, "wildcatter:wool_wire_neutral");
			put(server, rx, up - 1, rz + 1, "wildcatter:wool_wire_neutral");
			put(server, rx, up - 1, rz + 2, "wildcatter:wool_wire_neutral");
			put(server, dx, top + 1, rz + 2, "wildcatter:wool_wire_neutral");
			put(server, dx, top + 1, rz + 1, "wildcatter:wool_wire_neutral");
			put(server, dx, top + 1, rz, "wildcatter:drilling_rig");
			context.waitTicks(40);
			String rig = read(server, dx, top + 1, rz);
			check(rig.contains("drilling"), "a wired rig should start drilling: " + rig + " | xfmr " + read(server, 36, up, 31)
					+ " | hot@top " + read(server, rx, top + 1, rz) + " | neutral@top " + read(server, dx, top + 1, rz + 1));
			int topY = Reservoir.at(seed, dx, rz).topY();
			for (int waited = 0; waited < 3000; waited += 100) {
				if (server.computeOnServer(srv -> srv.overworld().getBlockState(new BlockPos(dx, top + 1, rz)).is(WildcatterBlocks.PUMPJACK))) {
					break;
				}
				context.waitTicks(100);
			}
			check(server.computeOnServer(srv -> srv.overworld().getBlockState(new BlockPos(dx, top + 1, rz)).is(WildcatterBlocks.PUMPJACK)),
					"the rig should have struck oil and become a pumpjack: " + read(server, dx, top + 1, rz));
			check(server.computeOnServer(srv -> srv.overworld().getBlockState(new BlockPos(dx, topY, rz)).is(WildcatterBlocks.CASING)),
					"the hole should be cased down to the field");
			context.waitTicks(200);
			String jack = read(server, dx, top + 1, rz);
			check(jack.matches(".*tank [1-9]\\d*/.*"), "a wired pumpjack should fill its tank: " + jack);
			context.takeScreenshot("wildcatter-pumpjack");

			// ---- the gas well: a wellhead against a firebox under a boiler, no fuel put in ----
			server.runCommand("tp @p " + gx + " " + up + " " + (gz - 6) + " 0 20");
			context.waitTicks(40);
			server.runCommand("fill " + (gx - 1) + " " + (up - 1) + " " + (gz - 1) + " " + (gx + 3) + " " + (up + 3) + " " + (gz + 1) + " minecraft:air");
			put(server, gx, up, gz, "wildcatter:gas_wellhead");
			put(server, gx + 1, up, gz, "wildcatter:gas_line");
			put(server, gx + 2, up, gz, "wildcatter:firebox");
			put(server, gx + 2, up + 1, gz, "wildcatter:boiler");
			server.computeOnServer(srv -> ((BoilerBlockEntity) srv.overworld().getBlockEntity(new BlockPos(fgx + 2, up + 1, fgz))).addWater(4000));
			context.waitTicks(200);
			String fire = read(server, gx + 2, up, gz);
			check(fire.contains("burning"), "a gas well on a gas line should light a firebox with no fuel in it: " + fire);
			String head = read(server, gx, up, gz);
			check(head.contains("feeding"), "the wellhead should be feeding the firebox: " + head);
			// a gas turbine on the same well draws what the firebox leaves
			put(server, gx, up + 1, gz, "wildcatter:gas_turbine");
			context.waitTicks(100);
			String turbine = read(server, gx, up + 1, gz);
			check(turbine.matches(".*fuel [1-9]\\d*(\\.\\d+)?/.*") || turbine.contains("fuel 1"), "a gas turbine touching a gas well should fill its tank: " + turbine);
			String boiler = read(server, gx + 2, up + 1, gz) + " | " + turbine;
			System.out.println("[wildcatter-test] wells ok: " + echo + " | " + jack + " | " + head + " | " + fire + " | " + boiler);
		} finally {
			ReservoirData.testSeed = null;
		}
	}

	/**
	 * Refining and engines: eight diesel engines join into a generator, a transformer steps it down to run a refinery
	 * tower (a base with two columns), and the refinery's diesel comes back down a gas line to fuel the engines.
	 */
	private void refining(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("fill 66 " + (y - 1) + " 56 82 " + (y + 5) + " 66 minecraft:air");
		server.runCommand("fill 66 " + (y - 1) + " 56 82 " + (y - 1) + " 66 minecraft:stone");
		server.runCommand("tp @p 74 " + up + " 70 180 20");
		for (int x = 72; x <= 73; x++) {
			for (int h = 0; h < 2; h++) {
				for (int z = 60; z <= 61; z++) {
					put(server, x, up + h, z, "wildcatter:diesel_engine");
				}
			}
		}
		put(server, 74, up, 60, "wildcatter:transformer[facing=west]");
		put(server, 75, up, 60, "wildcatter:refinery");
		put(server, 75, up + 1, 60, "wildcatter:refinery_column");
		put(server, 75, up + 2, 60, "wildcatter:refinery_column");
		put(server, 75, up, 59, "wildcatter:gas_line");
		put(server, 74, up, 59, "wildcatter:gas_line");
		put(server, 73, up, 59, "wildcatter:gas_line");
		context.waitTicks(60);   // the engines find each other
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.EngineBlockEntity) srv.overworld().getBlockEntity(new BlockPos(72, up, 60))).addFuel(100);
			((dev.wildcatter.block.RefineryBlockEntity) srv.overworld().getBlockEntity(new BlockPos(75, up, 60))).addCrude(3000);
			return null;
		});
		context.waitTicks(700);
		String gen = read(server, 72, up, 60);
		check(gen.contains("generator (8 engines"), "eight diesel engines in a cube should form a generator: " + gen);
		String refinery = read(server, 75, up, 60);
		check(refinery.contains("Refinery tower"), "a base with two columns should be a full tower: " + refinery);
		check(refinery.contains("refining"), "a refinery on the engines' power should be refining: " + refinery);
		check(refinery.matches(".*plastic [1-9]\\d* sheets.*"), "a tower should have made plastic: " + refinery);
		check(refinery.matches(".*diesel [1-9]\\d*/.*") || gen.matches(".*fuel \\d+/.*"), "it should have made diesel: " + refinery);
		put(server, 70, up, 58, "wildcatter:plastic_wire_hot");
		String wire = read(server, 70, up, 58);
		check(wire.contains("plastic insulated"), "plastic wire should say so: " + wire);
		// A 4.16 kV short on the generator's west face, with an MV breaker guarding the hot side: it must trip
		put(server, 71, up, 60, "wildcatter:mv_breaker[facing=west]");
		put(server, 70, up, 60, "wildcatter:mv_wire_hot");
		put(server, 71, up, 61, "wildcatter:mv_wire_neutral");
		put(server, 70, up, 61, "wildcatter:mv_wire_neutral");   // touches the hot wire: a short
		context.waitTicks(60);
		String breaker = read(server, 71, up, 60);
		check(breaker.contains("TRIPPED") && breaker.contains("4.16 kV"), "an MV breaker should trip on a short on the generator: " + breaker);
		context.takeScreenshot("wildcatter-refinery");
		// a pole line for the picture: two poles with crossarms, an MV breaker at the foot of one
		server.runCommand("fill 84 " + (y - 1) + " 66 100 " + (y + 8) + " 76 minecraft:air");
		server.runCommand("fill 84 " + (y - 1) + " 66 100 " + (y - 1) + " 76 minecraft:grass_block");
		for (int px : new int[] {86, 96}) {
			for (int h = 0; h < 6; h++) {
				put(server, px, up + h, 70, "wildcatter:pole");
			}
			put(server, px, up + 6, 70, "wildcatter:crossarm[axis=z]");
			put(server, px, up + 6, 69, "wildcatter:crossarm[axis=z]");
			put(server, px, up + 6, 71, "wildcatter:crossarm[axis=z]");
		}
		for (int wx = 87; wx <= 95; wx++) {
			put(server, wx, up + 7, 69, "wildcatter:hv_plastic_wire_hot");
			put(server, wx, up + 7, 71, "wildcatter:hv_plastic_wire_neutral");
		}
		put(server, 88, up, 72, "wildcatter:mv_breaker[facing=east]");
		put(server, 90, up, 72, "wildcatter:hv_breaker[facing=east]");
		put(server, 92, up, 72, "wildcatter:breaker[facing=east]");
		put(server, 94, up, 72, "wildcatter:fuse[facing=east]");
		server.runCommand("tp @p 91 " + (up + 2) + " 79 180 -5");
		context.waitTicks(20);
		context.takeScreenshot("wildcatter-pole-line");
		System.out.println("[wildcatter-test] refining ok: " + gen + " | " + refinery + " | " + wire + " | " + breaker);
	}

	private int waterIn(TestServerContext server, int x, int yy, int z) {
		// water plus the water the steam in the tank came from, so a boiler filling its steam tank does not look like a leak
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("water (\\d+)/\\d+, steam (\\d+)/").matcher(read(server, x, yy, z));
		return m.find() ? Integer.parseInt(m.group(1)) + Integer.parseInt(m.group(2)) / Tuning.STEAM_PER_WATER : -1;
	}

	// ---- the 110 V loop --------------------------------------------------------------------------------------
	private void lowVoltageLoop(ClientGameTestContext context, TestServerContext server) {
		plant(server, 10);
		hotLine(server, "wildcatter:wool_wire_hot");
		put(server, 13, up, 10, "wildcatter:breaker[facing=east]");
		put(server, 15, up, 10, "wildcatter:electric_lamp");
		server.runCommand("tp @p 13 " + up + " 7 0 20");
		context.waitTicks(80);

		// 1. One wire is a dead end: the lamp must stay dark.
		check(!lit(server, 15, 10), "a lamp with a hot wire but no neutral must stay dark");
		check(read(server, 15, up, 10).contains("NO POWER"), "the meter should say there is no power: " + read(server, 15, up, 10));

		// 2. Close the loop and it lights, at about the right volts and amps (60 W at 110 V is 0.55 A).
		neutralLine(server, "wildcatter:wool_wire_neutral");
		context.waitTicks(60);
		check(lit(server, 15, 10), "the lamp should light once the neutral wire completes the loop");
		String lampReading = read(server, 15, up, 10);
		System.out.println("[wildcatter-test] lamp: " + lampReading);
		check(lampReading.matches(".*\\b(10[5-9]|110) V, 0\\.[45]\\d A.*"), "lamp volts and amps look wrong: " + lampReading);
		context.takeScreenshot("steam-plant-running");

		// 3. The boiler and turbine must not flicker while they run.
		boolean boilerLit = server.computeOnServer(srv -> srv.overworld().getBlockState(new BlockPos(10, up, 10)).getValue(BoilerBlock.LIT));
		boolean running = server.computeOnServer(srv -> srv.overworld().getBlockState(new BlockPos(11, up, 10)).getValue(SteamTurbineBlock.RUNNING));
		int flips = 0;
		for (int i = 0; i < 80; i++) {
			context.waitTicks(1);
			boolean b = server.computeOnServer(srv -> srv.overworld().getBlockState(new BlockPos(10, up, 10)).getValue(BoilerBlock.LIT));
			boolean t = server.computeOnServer(srv -> srv.overworld().getBlockState(new BlockPos(11, up, 10)).getValue(SteamTurbineBlock.RUNNING));
			if (b != boilerLit || t != running) {
				flips++;
			}
			boilerLit = b;
			running = t;
		}
		check(flips == 0, "the boiler or turbine changed state " + flips + " times while running steadily");

		// 4. Overload: two 1500 W heaters want about 27 A. The 15 A breaker must trip before the 20 A wire is hurt.
		put(server, 15, up, 10, "wildcatter:electric_heater");
		put(server, 16, up, 10, "wildcatter:electric_heater");
		context.waitTicks(400);
		check(breaker(server, 13, 10) == BreakerBlock.Position.TRIPPED, "two heaters on a 15 A breaker should trip it, it is " + breaker(server, 13, 10));
		check(!isAir(server, 12, up, 10) && !isAir(server, 14, up, 10), "the breaker should trip before the wire burns");
		check(!lit(server, 15, 10), "the heaters must be dark once the breaker has tripped");
		System.out.println("[wildcatter-test] overload: " + read(server, 13, up, 10));

		// Fix the cause (take one heater away), reset, and it holds: one heater is 13.6 A.
		put(server, 16, up, 10, "minecraft:air");
		resetBreaker(server, 13, 10);
		context.waitTicks(300);
		check(breaker(server, 13, 10) == BreakerBlock.Position.ON, "one heater is under 15 A; the breaker should stay on");
		check(lit(server, 15, 10), "the heater should be running");
		put(server, 15, up, 10, "wildcatter:electric_lamp");
		context.waitTicks(20);

		// 5. Short circuit with a breaker: bare hot touching bare neutral. It must trip at once and the wire must survive.
		put(server, 14, up, 10, "wildcatter:wire_hot");
		put(server, 14, up, 11, "wildcatter:wire_neutral");
		context.waitTicks(30);
		check(breaker(server, 13, 10) == BreakerBlock.Position.TRIPPED, "a short circuit should trip the breaker");
		check(read(server, 13, up, 10).contains("short circuit"), "the breaker should say why: " + read(server, 13, up, 10));
		check(!isAir(server, 12, up, 10) && !isAir(server, 14, up, 10), "the breaker should save the wire from a short");
		System.out.println("[wildcatter-test] short: " + read(server, 13, up, 10));
		put(server, 14, up, 10, "wildcatter:wool_wire_hot");
		put(server, 14, up, 11, "wildcatter:wool_wire_neutral");
		resetBreaker(server, 13, 10);
		context.waitTicks(40);
		check(breaker(server, 13, 10) == BreakerBlock.Position.ON && lit(server, 15, 10), "after the fault is fixed the lamp should run again");

		// 6. Wool wears. Fresh wool is safe; worn-out wool is bare copper: it shocks, and it shorts against a worn neighbour.
		server.computeOnServer(srv -> {
			((WireBlockEntity) srv.overworld().getBlockEntity(new BlockPos(12, up, 10))).setWear(0.0);
			return null;
		});
		context.waitTicks(200);
		double wornBy = server.computeOnServer(srv -> ((WireBlockEntity) srv.overworld().getBlockEntity(new BlockPos(12, up, 10))).wear());
		check(wornBy > 0.001 && wornBy < 0.5, "wool should have worn a little by itself, wear=" + wornBy);
		System.out.println("[wildcatter-test] wool wear after 10 s: " + wornBy);

		server.runCommand("gamemode survival @p");
		float freshHealth = shockOver(context, server, new BlockPos(12, up, 10));
		check(freshHealth >= 20.0F, "fresh wool must be safe to stand against, health fell to " + freshHealth);
		server.computeOnServer(srv -> {
			((WireBlockEntity) srv.overworld().getBlockEntity(new BlockPos(12, up, 10))).setWear(1.0);
			return null;
		});
		context.waitTicks(20);
		float wornHealth = shockOver(context, server, new BlockPos(12, up, 10));
		check(wornHealth < 20.0F, "worn-out wool is bare live wire and should shock, health stayed " + wornHealth);
		System.out.println("[wildcatter-test] shocks: fresh wool left " + freshHealth + " hp, worn-out wool left " + wornHealth + " hp");

		// Two worn-out wires side by side AFTER the breaker short out, and the breaker protects the wire.
		server.computeOnServer(srv -> {
			((WireBlockEntity) srv.overworld().getBlockEntity(new BlockPos(12, up, 10))).setWear(0.0);
			((WireBlockEntity) srv.overworld().getBlockEntity(new BlockPos(14, up, 10))).setWear(1.0);
			((WireBlockEntity) srv.overworld().getBlockEntity(new BlockPos(14, up, 11))).setWear(1.0);
			return null;
		});
		context.waitTicks(30);
		check(breaker(server, 13, 10) == BreakerBlock.Position.TRIPPED, "two worn-out wires side by side after the breaker should short and trip it");
		server.computeOnServer(srv -> {
			((WireBlockEntity) srv.overworld().getBlockEntity(new BlockPos(14, up, 10))).setWear(0.0);
			((WireBlockEntity) srv.overworld().getBlockEntity(new BlockPos(14, up, 11))).setWear(0.0);
			return null;
		});
		resetBreaker(server, 13, 10);
		context.waitTicks(40);
		check(lit(server, 15, 10), "after replacing the worn wool the lamp should run again");

		// A breaker only protects what comes after it: the same fault BEFORE the breaker never trips it, and the wire burns.
		server.computeOnServer(srv -> {
			((WireBlockEntity) srv.overworld().getBlockEntity(new BlockPos(12, up, 10))).setWear(1.0);
			((WireBlockEntity) srv.overworld().getBlockEntity(new BlockPos(12, up, 11))).setWear(1.0);
			return null;
		});
		context.waitTicks(100);
		check(breaker(server, 13, 10) == BreakerBlock.Position.ON, "a short before the breaker must not trip it; the breaker only guards what is after it");
		check(isAir(server, 12, up, 10) || isAir(server, 12, up, 11), "a short before the breaker should burn out the wire");
		System.out.println("[wildcatter-test] short upstream of the breaker: breaker stayed on, the wire burned");
		hotLine(server, "wildcatter:wool_wire_hot");
		neutralLine(server, "wildcatter:wool_wire_neutral"); // the burnt neutral next to the turbine too
		context.waitTicks(60);
		check(lit(server, 15, 10), "after rebuilding the burnt wire the lamp should run again");

		// 7. An ungrounded machine bites when touched; a ground rod, or ground wire to a rod, stops it.
		int bites = touches(server, new BlockPos(15, up, 10));
		check(bites > 0, "an ungrounded live lamp never shocked in 200 touches");
		put(server, 15, y, 9, "minecraft:stone");
		put(server, 15, up, 9, "wildcatter:ground_rod");
		check(touches(server, new BlockPos(15, up, 10)) == 0, "a lamp with a ground rod against it should never shock");
		put(server, 15, up, 9, "wildcatter:ground_wire");
		put(server, 16, y, 9, "minecraft:stone");
		put(server, 16, up, 9, "wildcatter:ground_rod");
		check(touches(server, new BlockPos(15, up, 10)) == 0, "a lamp tied to a ground rod by ground wire should never shock");
		put(server, 16, up, 9, "minecraft:air");
		check(touches(server, new BlockPos(15, up, 10)) > 0, "ground wire that leads nowhere should not ground the lamp");
		System.out.println("[wildcatter-test] touch: " + bites + " bites in 200 ungrounded, none when grounded");
		server.runCommand("gamemode creative @p");

		// 8. Short circuit with NO breaker: nothing stops the current, so the wire burns out.
		put(server, 13, up, 10, "wildcatter:wool_wire_hot");
		context.waitTicks(20);
		put(server, 14, up, 10, "wildcatter:wire_hot");
		put(server, 14, up, 11, "wildcatter:wire_neutral");
		context.waitTicks(200);
		boolean burnt = isAir(server, 12, up, 10) || isAir(server, 13, up, 10) || isAir(server, 14, up, 10) || isAir(server, 14, up, 11)
				|| isAir(server, 11, up, 11) || isAir(server, 12, up, 11) || isAir(server, 13, up, 11);
		check(burnt, "a dead short with no breaker should burn out a wire");
		System.out.println("[wildcatter-test] unprotected short: a wire burned out");
	}

	/** Puts a survival player right beside a wire for a while and returns their lowest health. */
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
		server.runCommand("setblock " + wire.getX() + " " + (wire.getY() - 1) + " " + wire.getZ() + " minecraft:air");
		server.runCommand("tp @p 13 " + wire.getY() + " 7 0 20");
		context.waitTicks(10);
		return lowest;
	}

	/** Touches the machine 200 times as a survival player and counts how often it bit. */
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

	// ---- 110 V -> 4.16 kV -> 110 V ------------------------------------------------------------------------------
	/**
	 * Row z=14. Low-voltage wire in and out of the transformers goes on their back and bottom faces; the 4.16 kV line
	 * leaves their front and top faces. The two bare medium-voltage lines are kept apart so they do not short.
	 */
	private void transformerChain(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("fill 6 " + y + " 12 24 " + (y + 4) + " 18 minecraft:air");
		plant(server, 14);
		String hot = "wildcatter:wool_wire_hot";
		String neutral = "wildcatter:wool_wire_neutral";
		// Generator -> first transformer (facing east: back is west, bottom is down).
		put(server, 12, up, 14, hot);
		put(server, 11, y, 14, neutral);
		put(server, 12, y, 14, neutral);
		put(server, 13, y, 14, neutral);
		put(server, 13, up, 14, "wildcatter:transformer[facing=east]");
		// 4.16 kV: hot leaves the front, neutral leaves the top, routed around so they never touch.
		put(server, 14, up, 14, "wildcatter:mv_wire_hot");
		put(server, 15, up, 14, "wildcatter:mv_wire_hot");
		put(server, 13, up + 1, 14, "wildcatter:mv_wire_neutral");
		put(server, 13, up + 1, 15, "wildcatter:mv_wire_neutral");
		put(server, 14, up + 1, 15, "wildcatter:mv_wire_neutral");
		put(server, 15, up + 1, 15, "wildcatter:mv_wire_neutral");
		put(server, 16, up + 1, 15, "wildcatter:mv_wire_neutral");
		put(server, 16, up + 1, 14, "wildcatter:mv_wire_neutral");
		// Second transformer (facing west: front is west, back is east, bottom is down) and a lamp on its low side.
		put(server, 16, up, 14, "wildcatter:transformer[facing=west]");
		put(server, 17, up, 14, hot);
		put(server, 16, y, 14, neutral);
		put(server, 17, y, 14, neutral);
		put(server, 18, y, 14, neutral);
		put(server, 18, up, 14, "wildcatter:electric_lamp");
		server.runCommand("tp @p 15 " + up + " 10 0 20");
		context.waitTicks(200);

		String first = read(server, 13, up, 14);
		System.out.println("[wildcatter-test] " + first);
		System.out.println("[wildcatter-test] lamp through two transformers: " + read(server, 18, up, 14));
		check(lit(server, 18, 14), "the lamp past two transformers should be lit: " + read(server, 18, up, 14));
		// It steps 110 V up to about 4.16 kV (a little under, because of the winding resistance) at a tiny current.
		check(first.matches(".*\\b4[01]\\d\\d V 0\\.0\\d A on the 4.16 kV side.*"), "the first transformer should hold about 4.16 kV on its high side at a tiny current: " + first);

		// A 110 V wire and a 4.16 kV line touching each other must not join.
		put(server, 14, up, 12, "wildcatter:wire_hot");
		put(server, 15, up, 12, "wildcatter:mv_wire_hot");
		context.waitTicks(10);
		boolean joined = server.computeOnServer(srv -> {
			var low = srv.overworld().getBlockState(new BlockPos(14, up, 12));
			var med = srv.overworld().getBlockState(new BlockPos(15, up, 12));
			return low.getValue(dev.wildcatter.block.WireShape.ARMS.get(net.minecraft.core.Direction.EAST))
					|| med.getValue(dev.wildcatter.block.WireShape.ARMS.get(net.minecraft.core.Direction.WEST));
		});
		check(!joined, "110 V and medium voltage wire must not join");
		System.out.println("[wildcatter-test] transformer chain: 110 V up to 4.16 kV and back down, lamp lit");
	}

	private void fieldManual(TestServerContext server) {
		boolean ok = server.computeOnServer(srv -> {
			ItemStack manual = new ItemStack(WildcatterItems.FIELD_MANUAL);
			return manual.has(DataComponents.WRITTEN_BOOK_CONTENT);
		});
		check(ok, "the Field Manual should carry its book pages");
		System.out.println("[wildcatter-test] Field Manual carries its pages");
	}
}
