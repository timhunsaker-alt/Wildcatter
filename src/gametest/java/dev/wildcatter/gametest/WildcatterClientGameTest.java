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
import dev.wildcatter.block.SteamTurbineBlockEntity;
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
		createWorldScreen(context);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			context.waitTicks(40);
			y = server.computeOnServer(srv -> srv.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 10, 10));
			up = y + 1;
			server.runCommand("fill 6 " + y + " 6 24 " + (y + 4) + " 20 minecraft:air");
			server.runCommand("difficulty normal");
			if (only("lowVoltageLoop")) {
				lowVoltageLoop(context, server);
			}
			if (only("transformerChain")) {
				transformerChain(context, server);
			}
			if (only("wells")) {
				wells(context, server);
			}
			if (only("steamLoop")) {
				steamLoop(context, server);
			}
			if (only("refining")) {
				refining(context, server);
			}
			if (only("frequency")) {
				frequency(context, server);
			}
			if (only("distribution")) {
				distribution(context, server);
			}
			if (only("spans")) {
				spans(context, server);
			}
			if (only("poleTransformer")) {
				poleTransformer(context, server);
			}
			if (only("serviceDrop")) {
				serviceDrop(context, server);
			}
			if (only("plugs")) {
				plugs(context, server);
			}
			if (only("feederRelay")) {
				feederRelay(context, server);
			}
			if (only("threePhase")) {
				threePhase(context, server);
			}
			if (only("solverBudget")) {
				solverBudget(context, server);
			}
			if (only("industrial")) {
				industrial(context, server);
			}
			if (only("motors")) {
				motors(context, server);
			}
			if (only("motorControlCenter")) {
				motorControlCenter(context, server);
			}
			if (only("motorStarter")) {
				motorStarter(context, server);
			}
			if (only("overheadThreePhase")) {
				overheadThreePhase(context, server);
			}
			if (only("statorLimit")) {
				statorLimit(context, server);
			}
			if (only("conductorSizes")) {
				conductorSizes(context, server);
			}
			if (only("gensets")) {
				gensets(context, server);
			}
			if (only("synchronizing")) {
				synchronizing(context, server);
			}
			if (only("steamPlant")) {
				steamPlant(context, server);
			}
			if (only("bigParts")) {
				bigParts(context, server);
			}
			if (only("gensetParts")) {
				gensetParts(context, server);
			}
			if (only("gensetFrameRate")) {
				gensetFrameRate(context, server);
			}
			if (only("towerParts")) {
				towerParts(context, server);
			}
			if (only("substationParts")) {
				substationParts(context, server);
			}
			if (only("lineParts")) {
				lineParts(context, server);
			}
			if (only("buildingGear")) {
				buildingGear(context, server);
			}
			fieldManual(server);
		}
	}

	/** WILDCATTER_ONLY=sceneA,sceneB runs just those scenes while working on them; unset, every scene runs. */
	private static boolean only(String scene) {
		String only = System.getenv("WILDCATTER_ONLY");
		return only == null || only.isBlank() || java.util.Arrays.asList(only.split(",")).contains(scene);
	}

	/** The Create World screen has the electrical standard selector, and clicking it changes the choice. */
	private void createWorldScreen(ClientGameTestContext context) {
		net.minecraft.client.gui.screens.Screen[] opened = new net.minecraft.client.gui.screens.Screen[1];
		net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof net.minecraft.client.gui.screens.worldselection.CreateWorldScreen) {
				opened[0] = screen;
			}
		});
		context.runOnClient(client -> net.minecraft.client.gui.screens.worldselection.CreateWorldScreen.openFresh(client, () -> {}, (screen, registries, data, rules, path) -> false));
		context.waitForScreen(net.minecraft.client.gui.screens.worldselection.CreateWorldScreen.class);
		context.waitTicks(20);
		int[] spot = context.computeOnClient(client -> {
			for (var widget : net.fabricmc.fabric.api.client.screen.v1.Screens.getWidgets(opened[0])) {
				if (widget instanceof net.minecraft.client.gui.components.CycleButton<?> && widget.getMessage().getString().startsWith("Grid")) {
					double scale = client.getWindow().getGuiScale();
					return new int[] {(int) ((widget.getX() + widget.getWidth() / 2.0) * scale), (int) ((widget.getY() + widget.getHeight() / 2.0) * scale)};
				}
			}
			return null;
		});
		check(spot != null, "the Create World screen should have the electrical standard button");
		check(dev.wildcatter.energy.ElectricalStandard.pending() == dev.wildcatter.energy.ElectricalStandard.NORTH_AMERICAN, "the selector should start on the config default");
		context.takeScreenshot("wildcatter-create-world");
		// The synthetic mouse of the test harness does not reach this screen's widgets, so press the button the way a key press does.
		context.runOnClient(client -> {
			for (var widget : net.fabricmc.fabric.api.client.screen.v1.Screens.getWidgets(opened[0])) {
				if (widget instanceof net.minecraft.client.gui.components.CycleButton<?> cycle && cycle.getMessage().getString().startsWith("Grid")) {
					cycle.onPress(new net.minecraft.client.input.KeyEvent(0, 0, 0));
				}
			}
		});
		context.waitTicks(10);
		System.out.println("[wildcatter-test] selector after click: " + context.computeOnClient(client -> {
			for (var widget : net.fabricmc.fabric.api.client.screen.v1.Screens.getWidgets(opened[0])) {
				if (widget.getMessage().getString().startsWith("Grid")) {
					return widget.getMessage().getString() + " children=" + opened[0].children().contains(widget) + " cursor=" + spot[0] + "," + spot[1] + " scale=" + client.getWindow().getGuiScale()
							+ " box=" + widget.getX() + "," + widget.getY() + "," + widget.getWidth() + "," + widget.getHeight();
				}
			}
			return "none";
		}));
		check(dev.wildcatter.energy.ElectricalStandard.pending() == dev.wildcatter.energy.ElectricalStandard.IEC, "clicking the selector should choose IEC, it is " + dev.wildcatter.energy.ElectricalStandard.pending());
		context.takeScreenshot("wildcatter-create-world-iec");
		System.out.println("[wildcatter-test] create world selector ok");
		dev.wildcatter.energy.ElectricalStandard.setPending(null);
		context.setScreen(() -> null);
		context.waitTicks(10);
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
		// any piece of a true-size part reads as the part
		return server.computeOnServer(srv -> dev.wildcatter.multiblock.BigParts.entity(srv.overworld(), new BlockPos(x, yy, z)) instanceof Inspectable i
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

	/**
	 * A true-size genset (docs/multiblocks/genset.md) with its control panel's main cell at {@code panel}, facing {@code facing}:
	 * the skid runs to the panel's left (alternator, engine, radiator), two cells deep away from the front, on its base one
	 * below and with the silencer two above. The output terminals are on the panel's right-hand (end) face. Prints any scene
	 * blocks it had to overwrite, so a set that does not fit its scene shows in the log.
	 */
	private void genset(TestServerContext server, BlockPos panel, net.minecraft.core.Direction facing, boolean gas) {
		java.util.List<BlockPos> overwrote = server.computeOnServer(srv -> dev.wildcatter.block.GensetSkid.build(srv.overworld(), panel, facing, gas));
		if (!overwrote.isEmpty()) {
			System.out.println("[wildcatter-test] genset at " + panel.toShortString() + " overwrote " + overwrote.size() + " blocks, e.g. " + overwrote.get(0).toShortString());
		}
	}

	private dev.wildcatter.block.GensetControlPanelBlockEntity panel(net.minecraft.server.MinecraftServer srv, BlockPos at) {
		return (dev.wildcatter.block.GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(at);
	}

	/** Lets the set form, fills a diesel set's tank, starts it, and waits for it to crank up and close its breaker. */
	private void startGenset(ClientGameTestContext context, TestServerContext server, BlockPos panel, net.minecraft.core.Direction facing, double liters) {
		context.waitTicks(30);   // the panel finds its parts
		server.computeOnServer(srv -> {
			BlockPos tank = dev.wildcatter.block.GensetSkid.base(panel, facing, false);
			if (liters > 0 && srv.overworld().getBlockEntity(tank) instanceof dev.wildcatter.block.GensetPartBlockEntity t) {
				t.setFuel(liters);
			}
			panel(srv, panel).setWantRun(true);
			return null;
		});
		context.waitTicks(240);   // cranks to rated speed in 10 s, then closes its breaker
		String state = read(server, panel.getX(), panel.getY(), panel.getZ());
		check(state.contains("RUNNING") && state.contains("breaker CLOSED"), "the genset should be running with its breaker closed: " + state);
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}

	// ---- the 3x3x3 machines ------------------------------------------------------------------------------------
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
		float[] rpm = context.computeOnClient(client -> new float[] {
				client.level.getBlockEntity(new BlockPos(51, up, 50)) instanceof dev.wildcatter.block.SteamTurbineBlockEntity t ? t.spin().rpm() : -1F,
				client.level.getBlockEntity(new BlockPos(53, up, 50)) instanceof dev.wildcatter.block.PumpBlockEntity p ? p.spin().rpm() : -1F});
		check(rpm[0] > 1000 && Math.abs(rpm[1] - Tuning.STARTER_PUMP_RPM) < 1, "the client should see the turbine's coupling and the pump's turning: " + rpm[0] + " / " + rpm[1] + " rpm");
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

		// The cooling tower is a true-size cell with a 7.5 kW 480 V fan (4.5c): the 120 V starter turbine cannot run one, so it reports
		// that it has none (the steam plant scene tests the tower)
		check(drain.contains("NO COOLING TOWER"), "a turbine without a tower should say so: " + drain);

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
	 * Oil and gas. The test picks a world seed that puts an oil field under the rig and a gas field somewhere near, then drills the
	 * oil well with the rig (on a genset's 480 V) and opens the gas well onto a firebox and a gas genset.
	 */
	private void wells(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("fill 30 " + (y - 1) + " 24 44 " + (y + 4) + " 40 minecraft:air");
		server.runCommand("fill 30 " + (y - 1) + " 24 44 " + (y - 1) + " 40 minecraft:stone");
		int rx = 37, rz = 31;
		int gx = 0, gz = 0;
		long found = -1;
		search:
		for (long seed = 1; seed < 2_000_000; seed++) {
			Reservoir oil = Reservoir.at(seed, rx + 7, rz);   // under the rig's hole
			if (oil == null || oil.kind() != Reservoir.Kind.OIL || oil.topY() > -42) {
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

			// ---- the oil well (docs/multiblocks/well-site.md): the rig and the pumpjack are 480 V three-phase motors. A diesel set feeds
			// a three-phase cable up a tall pole (the world is flat, so the rig stands high to have something to drill through) and
			// across to the rig's terminal box; the rig faces east, its hole 7 east of the pole so the casing misses the cable ----
			int top = up + 30;
			int hx = rx + 7;
			BlockPos hole = new BlockPos(hx, top + 1, rz);
			server.runCommand("kill @e[type=item]");
			BlockPos set = new BlockPos(rx, up, rz + 2);
			genset(server, set, net.minecraft.core.Direction.WEST, false);   // the skid runs south of the panel
			put(server, rx, up, rz + 1, "wildcatter:cable_industrial");
			for (int h = up; h <= top + 1; h++) {
				put(server, rx, h, rz, "wildcatter:cable_industrial");
			}
			put(server, rx + 1, top + 1, rz, "wildcatter:cable_industrial");
			startGenset(context, server, set, net.minecraft.core.Direction.WEST, 1000);
			// a pad of rock under the rig and the pumpjack, so what they drop lands there
			server.runCommand("fill " + (rx + 1) + " " + top + " " + (rz - 1) + " " + (hx + 1) + " " + top + " " + (rz + 6) + " minecraft:stone");
			placeBig(server, WildcatterBlocks.DRILLING_RIG, hole, net.minecraft.core.Direction.EAST);   // its terminal box at rx + 2
			// a ledge to watch from, level with the rig's deck
			server.runCommand("fill " + (hx - 6) + " " + (top + 1) + " " + (rz + 13) + " " + (hx - 2) + " " + (top + 1) + " " + (rz + 15) + " minecraft:stone");
			server.runCommand("tp @p " + (hx - 4) + " " + (top + 2) + " " + (rz + 14) + " 165 -8");
			context.waitTicks(100);   // the motor starts (2 s of inrush) and runs
			String rig = read(server, hx, top + 1, rz);
			check(rig.contains("drilling"), "a rig on 480 V three-phase should start drilling: " + rig + " | set " + read(server, rx, up, rz + 3)
					+ " | cable@top " + read(server, rx + 1, top + 1, rz));
			float rigRpm = context.computeOnClient(client -> client.level.getBlockEntity(hole) instanceof dev.wildcatter.block.DrillingRigBlockEntity r ? r.spin().rpm() : -1F);
			check(Math.abs(rigRpm - Tuning.RIG_ROTARY_RPM) < 1, "the client should see the top head turning at 80 rpm: " + rigRpm);
			context.takeScreenshot("wildcatter-drilling-rig");
			int topY = Reservoir.at(seed, hx, rz).topY();
			for (int waited = 0; waited < 3000; waited += 100) {
				if (server.computeOnServer(srv -> srv.overworld().getBlockState(hole).is(WildcatterBlocks.OIL_WELLHEAD))) {
					break;
				}
				context.waitTicks(100);
			}
			check(server.computeOnServer(srv -> srv.overworld().getBlockState(hole).is(WildcatterBlocks.OIL_WELLHEAD)),
					"the rig should have struck oil and rigged down, leaving an oil wellhead: " + read(server, hx, top + 1, rz));
			check(isAir(server, hx, top + 5, rz) && isAir(server, rx + 2, top + 1, rz), "the rig should have left the hole: mast and skid gone");
			check(itemsNear(server, hole, WildcatterBlocks.DRILLING_RIG) == 1, "the rig should drop as its item when it rigs down");
			check(server.computeOnServer(srv -> srv.overworld().getBlockState(new BlockPos(hx, topY, rz)).is(WildcatterBlocks.CASING)),
					"the hole should be cased down to the field");
			// a pumpjack clicked on the wellhead from the west snaps over it, facing east; the short cable gives way to its terminal
			// box, which the pole's cable then feeds directly
			put(server, rx + 1, top + 1, rz, "minecraft:air");
			check(placeAt(server, WildcatterBlocks.PUMPJACK, hole, net.minecraft.core.Direction.UP, rx - 0.5, rz + 0.5, -90.0F),
					"a pumpjack clicked on the oil wellhead should place");
			BlockPos jackMain = hole.west();
			check(stateAt(server, jackMain).is(WildcatterBlocks.PUMPJACK) && stateAt(server, jackMain).getValue(dev.wildcatter.block.PumpjackBlock.FACING) == net.minecraft.core.Direction.EAST
					&& stateAt(server, hole.above(2)).is(WildcatterBlocks.PUMPJACK), "the pumpjack should snap with its horsehead over the well: " + stateAt(server, jackMain));
			context.waitTicks(300);
			String jack = read(server, jackMain.getX(), jackMain.getY(), jackMain.getZ());
			check(jack.matches(".*tank [1-9]\\d*/.*"), "a wired pumpjack should fill its tank: " + jack);
			float jackRpm = context.computeOnClient(client -> client.level.getBlockEntity(jackMain) instanceof dev.wildcatter.block.PumpjackBlockEntity j ? j.spin().rpm() : -1F);
			check(Math.abs(jackRpm - Tuning.PUMPJACK_SPM) < 0.5, "the client should see the crank turning at 9 strokes a minute: " + jackRpm);
			context.takeScreenshot("wildcatter-pumpjack");
			// an older world's one-block pumpjack on its cased hole becomes the hole's wellhead and drops itself
			server.runCommand("kill @e[type=item]");
			BlockPos old = new BlockPos(hx, top + 1, rz + 6);
			put(server, old.getX(), old.getY() - 1, old.getZ(), "wildcatter:casing");
			put(server, old.getX(), old.getY(), old.getZ(), "wildcatter:pumpjack");
			context.waitTicks(10);
			check(stateAt(server, old).is(WildcatterBlocks.OIL_WELLHEAD) && itemsNear(server, old, WildcatterBlocks.PUMPJACK) == 1,
					"an old one-block pumpjack should become a wellhead and drop as an item: " + stateAt(server, old));

			// ---- the gas well: a wellhead against a firebox under a boiler, no fuel put in ----
			server.runCommand("tp @p " + gx + " " + up + " " + (gz - 6) + " 0 20");
			context.waitTicks(40);
			server.runCommand("fill " + (gx - 4) + " " + (up - 1) + " " + (gz - 1) + " " + (gx + 5) + " " + (up + 3) + " " + (gz + 3) + " minecraft:air");
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
			// a gas genset whose engine stands against the same well runs on what the firebox leaves; the wellhead touches the
			// engine's third cell along, not its main cell, so the gas finds the engine through any of its cells
			BlockPos gasSet = new BlockPos(gx + 3, up, gz + 1);
			genset(server, gasSet, net.minecraft.core.Direction.NORTH, true);   // the skid runs west: engine x gx-2 to gx+1, behind the wellhead
			startGenset(context, server, gasSet, net.minecraft.core.Direction.NORTH, 0);
			String turbine = read(server, gx + 3, up, gz + 1);
			check(turbine.contains("Gas genset") && turbine.contains("gas from a well"), "a gas genset on a gas well should run on its gas: " + turbine);
			String boiler = read(server, gx + 2, up + 1, gz) + " | " + turbine;
			System.out.println("[wildcatter-test] wells ok: " + echo + " | " + jack + " | " + head + " | " + fire + " | " + boiler);
		} finally {
			ReservoirData.testSeed = null;
		}
	}

	/**
	 * Refining and a genset: a 480 V diesel genset, a unit substation run backward to 4.16 kV and a transformer down to 120 V run a
	 * refinery tower (a base with two columns), and the refinery's diesel comes back down a gas line to the genset's fuel tank.
	 */
	private void refining(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("fill 66 " + (y - 1) + " 56 82 " + (y + 5) + " 66 minecraft:air");
		server.runCommand("fill 66 " + (y - 1) + " 56 82 " + (y - 1) + " 66 minecraft:stone");
		server.runCommand("kill @e[type=item]");
		server.runCommand("tp @p 74 " + up + " 70 180 20");
		BlockPos set = new BlockPos(72, up, 60);
		genset(server, set, net.minecraft.core.Direction.NORTH, false);   // skid to the west: alternator 71, engine 70, radiator 69
		// the unit substation (true size, x 73-76 and z 59-60) takes 480 V at its west end and gives 4.16 kV at its east end, through
		// a short MV wire to the old 110 V / 4.16 kV transformer and the refinery
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, new BlockPos(72, up, 60), net.minecraft.core.Direction.EAST, false);
		put(server, 77, up, 60, "wildcatter:mv_wire_hot");
		put(server, 78, up, 60, "wildcatter:transformer[facing=west]");
		put(server, 79, up, 60, "wildcatter:refinery");
		put(server, 79, up + 1, 60, "wildcatter:refinery_column");
		put(server, 79, up + 2, 60, "wildcatter:refinery_column");
		put(server, 79, up, 59, "wildcatter:gas_line");
		for (int x = 70; x <= 79; x++) {
			put(server, x, up, 58, "wildcatter:gas_line");
		}
		put(server, 70, up, 59, "wildcatter:gas_line");
		put(server, 70, y, 59, "wildcatter:gas_line");   // down beside the fuel tank under the engine
		startGenset(context, server, set, net.minecraft.core.Direction.NORTH, 100);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.RefineryBlockEntity) srv.overworld().getBlockEntity(new BlockPos(79, up, 60))).addCrude(3000);
			return null;
		});
		context.waitTicks(700);
		String gen = read(server, 72, up, 60);
		check(gen.contains("Diesel genset") && gen.contains("RUNNING"), "the diesel genset should be running: " + gen);
		String refinery = read(server, 79, up, 60);
		check(refinery.contains("Refinery tower"), "a base with two columns should be a full tower: " + refinery);
		check(refinery.contains("refining"), "a refinery on the engines' power should be refining: " + refinery);
		check(refinery.matches(".*plastic [1-9]\\d* sheets.*"), "a tower should have made plastic: " + refinery);
		check(refinery.matches(".*diesel [1-9]\\d*/.*") || read(server, 70, y, 60).matches(".*tank: [1-9]\\d* of.*"), "it should have made diesel: " + refinery + " | " + read(server, 70, y, 60));
		put(server, 70, up, 58, "wildcatter:plastic_wire_hot");
		String wire = read(server, 70, up, 58);
		check(wire.contains("plastic insulated"), "plastic wire should say so: " + wire);
		// A 4.16 kV ground fault off the unit substation's MV wire, with an MV breaker guarding it: it must trip
		put(server, 77, up, 61, "wildcatter:mv_breaker[facing=south]");
		put(server, 77, up, 62, "wildcatter:mv_wire_hot");
		put(server, 77, up, 63, "wildcatter:mv_wire_hot[conductor=g]");   // bare ground touching the bare phase: a fault to earth
		context.waitTicks(60);
		String breaker = read(server, 77, up, 61);
		check(breaker.contains("TRIPPED") && breaker.contains("4.16 kV"), "an MV breaker should trip on a short on the generator: " + breaker);
		context.takeScreenshot("wildcatter-refinery");
		// a pole line for the picture: three 40 ft poles with arms and pin insulators, strung 30 m apart, breakers at the foot
		server.runCommand("fill 84 " + (y - 1) + " 66 150 " + (y + 12) + " 76 minecraft:air");
		server.runCommand("fill 84 " + (y - 1) + " 66 150 " + (y - 1) + " 76 minecraft:grass_block");
		int[] poles = {86, 116, 146};
		for (int px : poles) {
			placeBig(server, WildcatterBlocks.WOOD_POLE_40, new BlockPos(px, up, 70), net.minecraft.core.Direction.NORTH);
			crossarm(server, new BlockPos(px, up + 9, 70));
			insulator(server, WildcatterBlocks.INSULATOR_DIST, new BlockPos(px, up + 10, 69), "a");
			insulator(server, WildcatterBlocks.INSULATOR_DIST, new BlockPos(px, up + 10, 70), "b");
			insulator(server, WildcatterBlocks.INSULATOR_DIST, new BlockPos(px, up + 10, 71), "c");
		}
		for (int k = 0; k + 1 < poles.length; k++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos from = new BlockPos(poles[k], up + 10, 70 + dz);
				BlockPos to = new BlockPos(poles[k + 1], up + 10, 70 + dz);
				server.computeOnServer(srv -> dev.wildcatter.item.SpoolItem.connect(srv.overworld(), from, to, dev.wildcatter.energy.Voltage.DISTRIBUTION, false, 1024));
			}
		}
		put(server, 88, up, 72, "wildcatter:mv_breaker[facing=east]");
		put(server, 90, up, 72, "wildcatter:hv_breaker[facing=east]");
		put(server, 92, up, 72, "wildcatter:breaker[facing=east]");
		put(server, 94, up, 72, "wildcatter:fuse[facing=east]");
		server.runCommand("tp @p 100 " + (up + 4) + " 84 160 -12");
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
		plant(server, 9);   // a second set beside the first: 4 kW between them, so a 3 kW overload trips the breaker and not the generators
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
		check(lampReading.matches(".*\\b(11[0-9]|120) V, 0\\.[45]\\d A.*"), "lamp volts and amps look wrong: " + lampReading);
		context.takeScreenshot("steam-plant-running");

		// The electrical standard is the world's choice: the same lamp on an IEC world runs on 230 V, and goes back on a North American one.
		dev.wildcatter.energy.ElectricalStandard.setActive(dev.wildcatter.energy.ElectricalStandard.IEC);
		context.waitTicks(80);
		String iec = read(server, 15, up, 10);
		check(iec.matches(".*\\b2[23]\\d V, 0\\.[23]\\d A.*") && iec.contains("230 V"), "on an IEC world the lamp should run on 230 V: " + iec);
		dev.wildcatter.energy.ElectricalStandard.setActive(dev.wildcatter.energy.ElectricalStandard.NORTH_AMERICAN);
		context.waitTicks(80);
		check(read(server, 15, up, 10).matches(".*\\b(11[0-9]|120) V, 0\\.[45]\\d A.*"), "back on a North American world the lamp should run on 120 V: " + read(server, 15, up, 10));
		System.out.println("[wildcatter-test] standards ok: " + iec);

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

	/** A survival player hits the block once (the way a hand touches it); returns their health right after. */
	private float touchCutout(TestServerContext server, BlockPos pos) {
		return server.computeOnServer(srv -> {
			ServerPlayer player = srv.getPlayerList().getPlayers().get(0);
			player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
			player.setHealth(20.0F);
			player.setInvulnerableTime(0);
			srv.overworld().getBlockState(pos).attack(srv.overworld(), pos, player);
			float health = player.getHealth();
			player.setHealth(20.0F);
			player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
			return health;
		});
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
		// It steps 120 V up to the 4.16 kV line's 2.40 kV to neutral (a little under, because of the winding resistance) at a tiny current.
		check(first.matches(".*\\b2[34]\\d\\d V 0\\.0\\d A on the 4.16 kV side.*"), "the first transformer should hold about 2.40 kV to neutral on its high side at a tiny current: " + first);

		// Split phase (North American): the same transformer's low side has a second leg, B, 180 degrees from A. A lamp on leg B lights,
		// and with equal lamps on A and B the shared neutral carries nothing.
		check(first.length() > 0 && read(server, 16, up, 14).contains("leg B"), "a North American transformer should report its second leg: " + read(server, 16, up, 14));
		put(server, 16, up, 13, "wildcatter:wool_wire_hot[conductor=b]");
		put(server, 16, up, 12, "wildcatter:electric_lamp");
		put(server, 17, up, 12, neutral);
		put(server, 17, y, 12, neutral);
		put(server, 17, y, 13, neutral);
		context.waitTicks(120);
		String legB = read(server, 16, up, 13);
		check(lit(server, 16, 12), "a lamp on leg B should light: " + legB);
		check(legB.contains("Phase B") && legB.matches(".*\\b1[12]\\d V,.*"), "the B wire should read about 120 V and say it is phase B: " + legB);
		String sharedNeutral = read(server, 16, y, 14);
		check(sharedNeutral.matches(".*, 0\\.[0-2] A of.*"), "equal lamps on A and B should leave the shared neutral nearly empty: " + sharedNeutral);
		put(server, 16, up, 12, "minecraft:air");
		context.waitTicks(60);
		String unbalanced = read(server, 16, y, 14);
		check(unbalanced.matches(".*, 0\\.[3-7] A of.*"), "with only the A lamp the neutral should carry its 0.5 A: " + unbalanced);
		System.out.println("[wildcatter-test] split phase ok: " + legB + " | balanced neutral: " + sharedNeutral + " | unbalanced: " + unbalanced);

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

	/**
	 * Frequency. A steam turbine rated 2 kW feeds a lamp and a bank of three 1.5 kW heaters (more than it can give). With an
	 * under-frequency relay in the heaters' feeder, the frequency dips, the relay sheds the heaters, and the turbine survives.
	 * Without one the frequency collapses and the turbine trips on under-frequency.
	 */
	private void frequency(ClientGameTestContext context, TestServerContext server) {
		for (int pass = 0; pass < 2; pass++) {
			boolean withRelay = pass == 0;
			int z = withRelay ? -30 : -44;
			server.runCommand("fill 8 " + y + " " + (z - 2) + " 18 " + (y + 5) + " " + (z + 3) + " minecraft:air");
			plant(server, z);
			String hot = "wildcatter:wool_wire_hot";
			String neutral = "wildcatter:wool_wire_neutral";
			put(server, 12, up, z, hot);
			put(server, 13, up, z, withRelay ? "wildcatter:ufls_relay[facing=east]" : hot);
			for (int x = 11; x <= 15; x++) {
				put(server, x, up, z + 1, neutral);
			}
			// a lamp on the bus ahead of the relay
			put(server, 12, up + 1, z, "wildcatter:electric_lamp");
			put(server, 12, up + 1, z + 1, neutral);
			server.runCommand("tp @p 14 " + (up + 2) + " " + (z + 6) + " 180 25");
			context.waitTicks(120);
			String quiet = read(server, 11, up, z);
			check(quiet.matches(".*\\b(59\\.9|60\\.0|60\\.1) Hz.*"), "a lightly loaded island should sit at 60 Hz: " + quiet);

			// a bank of three heaters, 4.5 kW on a 2 kW turbine
			for (int k = 0; k < 3; k++) {
				put(server, 14, up + k, z, hot);
				put(server, 15, up + k, z, "wildcatter:electric_heater");
				put(server, 15, up + k, z + 1, neutral);
			}
			if (withRelay) {
				context.waitTicks(60);
				String relay = read(server, 13, up, z);
				String turbine = read(server, 11, up, z);
				check(relay.contains("TRIPPED") && relay.contains("under-frequency"), "the relay should shed the heaters on under-frequency: " + relay);
				check(!turbine.contains("TRIPPED"), "with the heaters shed the turbine should stay on line: " + turbine);
				context.takeScreenshot("wildcatter-load-shedding");
				context.waitTicks(400);
				String recovered = read(server, 11, up, z);
				check(recovered.matches(".*\\b(59\\.[89]|60\\.[01]) Hz.*"), "after shedding the frequency should recover: " + recovered);
				System.out.println("[wildcatter-test] load shedding ok: " + relay + " | " + recovered);
			} else {
				context.waitTicks(100);
				String turbine = read(server, 11, up, z);
				check(turbine.contains("TRIPPED on frequency"), "without a relay the overloaded turbine should trip on under-frequency: " + turbine);
				// restart it (right-click does this), remove the heaters, and it runs again
				for (int k = 0; k < 3; k++) {
					put(server, 15, up + k, z, "minecraft:air");
				}
				server.computeOnServer(srv -> {
					((SteamTurbineBlockEntity) srv.overworld().getBlockEntity(new BlockPos(11, up, z))).resetTrip();
					return null;
				});
				context.waitTicks(120);
				String restarted = read(server, 11, up, z);
				check(!restarted.contains("TRIPPED"), "after the restart the turbine should be back on line: " + restarted);
				System.out.println("[wildcatter-test] generator trip ok: " + turbine + " | " + restarted);
			}
		}
	}

	/**
	 * The distribution tier: a turbine's 120 V is stepped up to 4.16 kV, up to 138 kV, and down to 12.47 kV, the single-phase tap there holding 7.2 kV.
	 * Row z=-60, facing east. The neutrals are tied to ground at each transformer, so only the hot line needs wire.
	 */
	private void distribution(ClientGameTestContext context, TestServerContext server) {
		int z = -60;
		server.runCommand("fill 8 " + y + " " + (z - 2) + " 22 " + (y + 5) + " " + (z + 3) + " minecraft:air");
		plant(server, z);
		stepUp(server, z);
		for (int x = 15; x <= 17; x++) {
			put(server, x, up, z, "wildcatter:dist_wire_hot");
		}
		put(server, 18, up, z, "wildcatter:dist_wire_hot");
		put(server, 19, up, z, "wildcatter:dist_wire_hot");
		server.runCommand("tp @p 17 " + (up + 1) + " " + (z + 5) + " 180 25");
		context.waitTicks(160);
		String line = read(server, 19, up, z);
		check(line.contains("12.47 kV") && line.matches(".*\\b7[12]\\d\\d V,.*"), "the distribution line should hold about 7.2 kV line to neutral: " + line
				+ " | turbine " + read(server, 11, up, z) + " | pad-mount " + read(server, 13, up, z));
		String transformer = read(server, 13, up, z);
		check(transformer.contains("12.47 kV") && transformer.contains("120 V"), "the pad-mount should name its two voltages: " + transformer);
		context.takeScreenshot("wildcatter-distribution");
		System.out.println("[wildcatter-test] distribution ok: " + line + " | " + transformer);
	}

	/** Overhead spans between insulators on poles: a good span carries 7.2 kV, rules refuse bad ones, and an under-rated insulator flashes over. */
	private void spans(ClientGameTestContext context, TestServerContext server) {
		int z = -76;
		server.runCommand("fill 8 " + y + " " + (z - 3) + " 70 " + (y + 12) + " " + (z + 4) + " minecraft:air");
		plant(server, z);
		stepUp(server, z);
		for (int x = 15; x <= 17; x++) {
			put(server, x, up, z, "wildcatter:dist_wire_hot");
		}
		put(server, 18, up, z, "wildcatter:dist_wire_hot");
		put(server, 19, up, z, "wildcatter:dist_wire_hot");
		put(server, 20, up, z, "wildcatter:dist_breaker[facing=east]");
		for (int x = 21; x <= 23; x++) {
			put(server, x, up, z, "wildcatter:dist_wire_hot");
		}
		// a riser up the first pole to its pole-top pin: 40 ft poles (10 m) with 8 ft arms, phase A on the pole tops
		for (int k = 1; k <= 10; k++) {
			put(server, 23, up + k, z, "wildcatter:dist_wire_hot");
		}
		for (int px : new int[] {24, 60}) {
			placeBig(server, WildcatterBlocks.WOOD_POLE_40, new BlockPos(px, up, z), net.minecraft.core.Direction.NORTH);
			crossarm(server, new BlockPos(px, up + 9, z));
			insulator(server, WildcatterBlocks.INSULATOR_DIST, new BlockPos(px, up + 10, z), "a");
			insulator(server, WildcatterBlocks.INSULATOR_DIST, new BlockPos(px, up + 10, z - 1), "b");
			insulator(server, WildcatterBlocks.INSULATOR_DIST, new BlockPos(px, up + 10, z + 1), "c");
		}
		placeBig(server, WildcatterBlocks.WOOD_POLE_40, new BlockPos(61, up, z + 3), net.minecraft.core.Direction.NORTH);
		insulator(server, WildcatterBlocks.INSULATOR_DIST, new BlockPos(61, up + 10, z + 3), "a");
		context.waitTicks(20);
		BlockPos a = new BlockPos(24, up + 10, z);
		BlockPos b = new BlockPos(60, up + 10, z);
		BlockPos c = new BlockPos(61, up + 10, z + 3);
		check(stateAt(server, a).getValue(dev.wildcatter.block.InsulatorBlock.MOUNT) == dev.wildcatter.block.InsulatorBlock.Mount.POLE_TOP
				&& stateAt(server, a.north()).getValue(dev.wildcatter.block.InsulatorBlock.MOUNT) == dev.wildcatter.block.InsulatorBlock.Mount.ARM,
				"the insulators should be mounted on the pole top and the arm: " + stateAt(server, a) + " | " + stateAt(server, a.north()));
		dev.wildcatter.item.SpoolItem.Result strung = server.computeOnServer(srv ->
				dev.wildcatter.item.SpoolItem.connect(srv.overworld(), a, b, dev.wildcatter.energy.Voltage.DISTRIBUTION, false, 1024));
		check(strung.ok() && strung.length() == 36 && strung.message().contains("(36 m)"), "a 36 m span of 4/0 ACSR (its longest) between two distribution insulators should be strung: " + strung);
		for (int dz : new int[] {-1, 1}) {   // B and C, dead, for the picture
			server.computeOnServer(srv -> dev.wildcatter.item.SpoolItem.connect(srv.overworld(), a.offset(0, 0, dz), b.offset(0, 0, dz), dev.wildcatter.energy.Voltage.DISTRIBUTION, false, 1024));
		}
		dev.wildcatter.item.SpoolItem.Result tooFar = server.computeOnServer(srv ->
				dev.wildcatter.item.SpoolItem.connect(srv.overworld(), a, c, dev.wildcatter.energy.Voltage.DISTRIBUTION, false, 1024));
		check(!tooFar.ok() && tooFar.message().contains("Too far"), "a 38 m span is over 4/0 ACSR's 36 m and should be refused: " + tooFar);
		insulator(server, WildcatterBlocks.INSULATOR_DIST, c, "b");
		dev.wildcatter.item.SpoolItem.Result wrongConductor = server.computeOnServer(srv ->
				dev.wildcatter.item.SpoolItem.connect(srv.overworld(), b, c, dev.wildcatter.energy.Voltage.DISTRIBUTION, false, 1024));
		check(!wrongConductor.ok() && wrongConductor.message().contains("different conductors"), "insulators on different conductors should not be joined: " + wrongConductor);
		server.runCommand("tp @p 42 " + (up + 7) + " " + (z + 28) + " 180 5");
		context.waitTicks(100);
		String far = read(server, 60, up + 10, z);
		check(far.contains("1 span") && far.matches(".*\\b7[12]\\d\\d V.*"), "the far end of the span should hold about 7.2 kV: " + far);
		context.takeScreenshot("wildcatter-span");
		System.out.println("[wildcatter-test] span ok: " + strung.message() + " | " + tooFar.message() + " | " + far);

		// an under-rated insulator: swap the far end for a low voltage one and the 12.47 kV line flashes over it
		server.runCommand("setblock 60 " + (up + 10) + " " + z + " minecraft:air");
		context.waitTicks(30);
		insulator(server, WildcatterBlocks.INSULATOR_LV, b, "a");
		dev.wildcatter.item.SpoolItem.Result flash = server.computeOnServer(srv ->
				dev.wildcatter.item.SpoolItem.connect(srv.overworld(), a, b, dev.wildcatter.energy.Voltage.DISTRIBUTION, false, 1024));
		check(flash.ok(), "a span can be strung to an under-rated insulator: " + flash);
		context.waitTicks(20);
		String lv = read(server, 60, up + 10, z);
		check(lv.contains("UNDER-RATED") && lv.contains("flashing over"), "the low voltage insulator on a 12.47 kV line should flash over: " + lv);
		System.out.println("[wildcatter-test] under-rated insulator ok: " + lv);
	}

	/** A pole-mount transformer on the 12.47 kV line lights lamps on both legs, and burns out when overloaded. */
	private void poleTransformer(ClientGameTestContext context, TestServerContext server) {
		int z = -92;
		server.runCommand("fill 8 " + y + " " + (z - 3) + " 26 " + (y + 6) + " " + (z + 4) + " minecraft:air");
		plant(server, z);
		stepUp(server, z);
		for (int x = 15; x <= 17; x++) {
			put(server, x, up, z, "wildcatter:dist_wire_hot");
		}
		put(server, 18, up, z, "wildcatter:dist_wire_hot");
		put(server, 19, up, z, "wildcatter:dist_wire_hot");
		placeBig(server, WildcatterBlocks.WOOD_POLE_40, new BlockPos(20, up, z - 1), net.minecraft.core.Direction.NORTH);
		put(server, 20, up, z, "wildcatter:pole_transformer[facing=west]");
		put(server, 21, up, z, "wildcatter:electric_lamp");        // leg A, on the back
		put(server, 20, up, z + 1, "wildcatter:electric_lamp");    // leg B, on a side
		server.runCommand("tp @p 22 " + (up + 1) + " " + (z + 5) + " 180 15");
		context.waitTicks(160);
		String unit = read(server, 20, up, z);
		check(unit.contains("25 kVA") && unit.contains("within its rating"), "the pole transformer should show its rating and be within it: " + unit);
		check(lit(server, 21, z) && lit(server, 20, z + 1), "lamps on both legs of the pole transformer should light: A " + read(server, 21, up, z) + " | B " + read(server, 20, up, z + 1));
		context.takeScreenshot("wildcatter-pole-transformer");
		// overload it: rate it at 30 VA for the test, with two lamps (120 VA) on it
		dev.wildcatter.block.TransformerBlockEntity.testRatedVa = 30.0;
		context.waitTicks(30);
		String heating = read(server, 20, up, z);
		check(heating.contains("OVERLOADED") || isAir(server, 20, up, z), "an overloaded pole transformer should say so or already have failed: " + heating);
		context.waitTicks(120);
		dev.wildcatter.block.TransformerBlockEntity.testRatedVa = null;
		check(isAir(server, 20, up, z), "a pole transformer at four times its rating should burn out");
		System.out.println("[wildcatter-test] pole transformer ok: " + unit + " | " + heating);
	}

	/**
	 * From a feeder through a fused cutout and a pole transformer, then a low voltage service drop (two spans, hot and neutral) to a house lamp.
	 * Blowing the cutout takes the house dark; a new link brings it back.
	 */
	private void serviceDrop(ClientGameTestContext context, TestServerContext server) {
		int z = -108;
		server.runCommand("fill 8 " + y + " " + (z - 3) + " 56 " + (y + 12) + " " + (z + 4) + " minecraft:air");
		plant(server, z);
		stepUp(server, z);
		for (int x = 15; x <= 17; x++) {
			put(server, x, up, z, "wildcatter:dist_wire_hot");
		}
		put(server, 18, up, z, "wildcatter:dist_wire_hot");
		put(server, 19, up, z, "wildcatter:fused_cutout[facing=east]");
		placeBig(server, WildcatterBlocks.WOOD_POLE_40, new BlockPos(20, up, z - 1), net.minecraft.core.Direction.NORTH);
		put(server, 20, up, z, "wildcatter:pole_transformer[facing=west]");
		String hot = "wildcatter:wool_wire_hot";
		String neutral = "wildcatter:wool_wire_neutral";
		// the transformer's low side: hot out of the back, round and up to the first service pole's arm (z-1); the neutral out
		// of the bottom and up to its other end (z+1)
		put(server, 21, up, z, hot);
		for (int k = 0; k <= 8; k++) {
			put(server, 21, up + k, z - 1, hot);
			put(server, 21, up + k, z + 1, neutral);
		}
		put(server, 20, y, z, neutral);
		put(server, 21, y, z, neutral);
		put(server, 21, y, z + 1, neutral);
		// two 30 ft poles 28 m apart, each with an arm across the drop and a spool insulator at each end of it
		for (int px : new int[] {22, 50}) {
			placeBig(server, WildcatterBlocks.WOOD_POLE_30, new BlockPos(px, up, z), net.minecraft.core.Direction.NORTH);
			crossarm(server, new BlockPos(px, up + 7, z));
			insulator(server, WildcatterBlocks.INSULATOR_LV, new BlockPos(px, up + 8, z - 1), "a");
			insulator(server, WildcatterBlocks.INSULATOR_LV, new BlockPos(px, up + 8, z + 1), "n");
		}
		// the house end
		for (int k = 0; k <= 8; k++) {
			put(server, 51, up + k, z - 1, hot);
			put(server, 51, up + k, z + 1, neutral);
		}
		put(server, 51, up, z, "wildcatter:electric_lamp");
		context.waitTicks(20);
		dev.wildcatter.item.SpoolItem.Result hotSpan = server.computeOnServer(srv -> dev.wildcatter.item.SpoolItem.connect(srv.overworld(),
				new BlockPos(22, up + 8, z - 1), new BlockPos(50, up + 8, z - 1), dev.wildcatter.energy.Voltage.LOW, true, 1024));
		dev.wildcatter.item.SpoolItem.Result neutralSpan = server.computeOnServer(srv -> dev.wildcatter.item.SpoolItem.connect(srv.overworld(),
				new BlockPos(22, up + 8, z + 1), new BlockPos(50, up + 8, z + 1), dev.wildcatter.energy.Voltage.LOW, true, 1024));
		check(hotSpan.ok() && neutralSpan.ok() && hotSpan.length() == 28, "the service drop's two 28 m spans should be strung: " + hotSpan + " " + neutralSpan);
		server.runCommand("tp @p 36 " + (up + 5) + " " + (z + 22) + " 180 -5");
		context.waitTicks(200);
		String cutout = read(server, 19, up, z);
		check(cutout.contains("Fused cutout") && lit(server, 51, z), "the house lamp should light through the cutout, transformer and service drop: " + cutout + " | " + read(server, 51, up, z));
		context.takeScreenshot("wildcatter-service-drop");
		// a fault takes out the cutout: shrink its rating so the lamp's small current blows it
		dev.wildcatter.block.FuseBlockEntity.testRatingScale = 0.0003;
		context.waitTicks(100);
		String blown = read(server, 19, up, z);
		check(blown.contains("BLOWN") && !lit(server, 51, z), "an overloaded cutout should blow and darken the house: " + blown);
		dev.wildcatter.block.FuseBlockEntity.testRatingScale = null;
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.FuseBlockEntity) srv.overworld().getBlockEntity(new BlockPos(19, up, z))).replaceElement();
			return null;
		});
		context.waitTicks(100);
		check(lit(server, 51, z), "with a new link the house should have power again: " + read(server, 19, up, z));
		// the cutout's contacts are bare: a hand on the live one is a shock; with the line open it is safe
		float live = touchCutout(server, new BlockPos(19, up, z));
		check(live < 20.0F, "touching a live fused cutout should shock: health " + live + " | " + read(server, 19, up, z));
		put(server, 18, up, z, "minecraft:air");
		context.waitTicks(40);
		float dead = touchCutout(server, new BlockPos(19, up, z));
		check(dead == 20.0F, "a de-energized cutout should be safe to touch: health " + dead + " | " + read(server, 19, up, z));
		System.out.println("[wildcatter-test] cutout shock ok: live touch left " + live + " health, dead touch " + dead);
		System.out.println("[wildcatter-test] service drop ok: " + hotSpan.message() + " | " + cutout + " | " + blown);
	}

	/** Receptacles and plugs: a matching plug delivers FE for the watts it draws, a wrong type does not fit, the wrong voltage delivers nothing, an overload burns it. */
	private void plugs(ClientGameTestContext context, TestServerContext server) {
		int z = -124;
		server.runCommand("fill 8 " + y + " " + (z - 3) + " 22 " + (y + 5) + " " + (z + 4) + " minecraft:air");
		plant(server, z);
		put(server, 12, up, z, "wildcatter:wool_wire_hot");
		for (int x = 11; x <= 13; x++) {
			put(server, x, up, z + 1, "wildcatter:wool_wire_neutral");
		}
		put(server, 13, up, z, "wildcatter:receptacle_nema_5_15");
		put(server, 14, up, z, "wildcatter:plug_nema_5_15");
		put(server, 15, up, z, "minecraft:iron_block");        // stands in for another mod's machine: it takes 1000 FE per tick
		java.util.concurrent.atomic.AtomicLong delivered = new java.util.concurrent.atomic.AtomicLong();
		dev.wildcatter.energy.EnergyBridge.clear();
		dev.wildcatter.energy.EnergyBridge.register(target -> target.level().getBlockState(target.pos()).is(net.minecraft.world.level.block.Blocks.IRON_BLOCK)
				? new dev.wildcatter.energy.HostEnergy() {
					@Override
					public long demand() {
						return 1000;
					}

					@Override
					public long insert(long fe) {
						delivered.addAndGet(fe);
						return fe;
					}
				} : null);
		server.runCommand("tp @p 14 " + (up + 1) + " " + (z + 5) + " 180 15");
		context.waitTicks(160);
		long before = delivered.get();
		context.waitTicks(100);
		long gained = delivered.get() - before;
		String plug = read(server, 14, up, z);
		check(plug.contains("delivering") && gained > 70_000 && gained < 120_000, "a matching plug should hand over about 1000 FE per tick (" + gained + " in 100 ticks): " + plug);
		context.takeScreenshot("wildcatter-plug");
		boolean mismatchFits = server.computeOnServer(srv -> dev.wildcatter.block.PlugAdapterBlock.fits(srv.overworld(), new BlockPos(14, up, z), dev.wildcatter.energy.PlugType.CEE_7));
		boolean matchFits = server.computeOnServer(srv -> dev.wildcatter.block.PlugAdapterBlock.fits(srv.overworld(), new BlockPos(14, up, z), dev.wildcatter.energy.PlugType.NEMA_5_15));
		check(!mismatchFits && matchFits, "a plug only fits a receptacle of its own type");

		// a 230 V plug on a 120 V circuit delivers nothing
		put(server, 14, up, z, "minecraft:air");
		put(server, 13, up, z, "wildcatter:receptacle_cee_7");
		put(server, 14, up, z, "wildcatter:plug_cee_7");
		context.waitTicks(60);
		long wrongBefore = delivered.get();
		context.waitTicks(60);
		String wrong = read(server, 14, up, z);
		check(wrong.contains("WRONG VOLTAGE") && delivered.get() - wrongBefore < 1000, "a 230 V plug on a 120 V circuit should deliver nothing: " + wrong);

		// an overload burns the plug and receptacle (the test shrinks the rating and the time)
		put(server, 14, up, z, "minecraft:air");
		put(server, 13, up, z, "wildcatter:receptacle_nema_5_15");
		put(server, 14, up, z, "wildcatter:plug_nema_5_15");
		context.waitTicks(40);
		dev.wildcatter.block.PlugAdapterBlockEntity.testRatingScale = 0.2;
		dev.wildcatter.block.PlugAdapterBlockEntity.testBurnSeconds = 1.5;
		context.waitTicks(120);
		dev.wildcatter.block.PlugAdapterBlockEntity.testRatingScale = null;
		dev.wildcatter.block.PlugAdapterBlockEntity.testBurnSeconds = null;
		dev.wildcatter.energy.EnergyBridge.clear();
		check(isAir(server, 14, up, z) && isAir(server, 13, up, z), "a plug held above its rating should burn out with its receptacle");
		System.out.println("[wildcatter-test] plugs ok: " + plug + " | " + wrong + " | FE gained " + gained);
	}

	/** An under-frequency relay on a 12.47 kV feeder sheds a pole transformer's overload and keeps the generator on line. */
	private void feederRelay(ClientGameTestContext context, TestServerContext server) {
		int z = -140;
		server.runCommand("fill 8 " + (y - 1) + " " + (z - 3) + " 24 " + (y + 5) + " " + (z + 4) + " minecraft:air");
		server.runCommand("fill 8 " + (y - 1) + " " + (z - 3) + " 24 " + (y - 1) + " " + (z + 4) + " minecraft:stone");
		plant(server, z);
		stepUp(server, z);
		for (int x = 15; x <= 17; x++) {
			put(server, x, up, z, "wildcatter:dist_wire_hot");
		}
		put(server, 18, up, z, "wildcatter:dist_wire_hot");
		put(server, 19, up, z, "wildcatter:ufls_relay_dist[facing=east]");
		placeBig(server, WildcatterBlocks.WOOD_POLE_40, new BlockPos(20, up, z - 1), net.minecraft.core.Direction.NORTH);
		put(server, 20, up, z, "wildcatter:pole_transformer[facing=west]");
		put(server, 21, up, z, "wildcatter:electric_lamp");
		context.waitTicks(120);
		String quiet = read(server, 11, up, z);
		check(quiet.matches(".*\\b(59\\.9|60\\.0|60\\.1) Hz.*"), "the feeder should sit at 60 Hz before the overload: " + quiet);
		// three heaters (4.5 kW) behind the pole transformer overload the 2 kW turbine
		put(server, 21, up, z, "wildcatter:electric_heater");
		put(server, 20, up, z + 1, "wildcatter:electric_heater");
		put(server, 20, y, z, "wildcatter:electric_heater");
		context.waitTicks(80);
		String relay = read(server, 19, up, z);
		String turbine = read(server, 11, up, z);
		check(relay.contains("TRIPPED") && relay.contains("under-frequency"), "the feeder relay should shed the overload: " + relay);
		check(!turbine.contains("TRIPPED"), "with the feeder shed the turbine should stay on line: " + turbine);
		System.out.println("[wildcatter-test] feeder relay ok: " + relay);
	}

	private double amps(String readout) {
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("([0-9.]+) A of").matcher(readout);
		check(m.find(), "no current in the readout: " + readout);
		return Double.parseDouble(m.group(1));
	}

	/**
	 * A three-phase turbine with one lamp on each phase (A east, B north, C south) and a shared neutral above:
	 * balanced, the neutral carries almost nothing; with one lamp gone it carries the imbalance.
	 */
	private void threePhase(ClientGameTestContext context, TestServerContext server) {
		int z = -156;
		server.runCommand("fill 8 " + y + " " + (z - 4) + " 20 " + (y + 5) + " " + (z + 4) + " minecraft:air");
		plant(server, z);
		put(server, 12, up, z, "wildcatter:wool_wire_hot[conductor=a]");
		put(server, 13, up, z, "wildcatter:electric_lamp");
		put(server, 11, up, z - 1, "wildcatter:wool_wire_hot[conductor=b]");
		put(server, 11, up, z - 2, "wildcatter:electric_lamp");
		put(server, 11, up, z + 1, "wildcatter:wool_wire_hot[conductor=c]");
		put(server, 11, up, z + 2, "wildcatter:electric_lamp");
		// the neutral: up from the turbine (the block it is read on), a junction above that, and down onto each lamp
		for (int[] at : new int[][] {{11, 1, z}, {11, 2, z}, {12, 2, z}, {13, 2, z}, {13, 1, z}, {11, 2, z - 1}, {11, 2, z - 2}, {11, 1, z - 2},
				{11, 2, z + 1}, {11, 2, z + 2}, {11, 1, z + 2}}) {
			put(server, at[0], up + at[1], at[2], "wildcatter:wool_wire_neutral");
		}
		server.runCommand("tp @p 16 " + (up + 3) + " " + (z + 5) + " 135 25");
		context.waitTicks(120);
		check(lit(server, 13, z) && lit(server, 11, z - 2) && lit(server, 11, z + 2),
				"a lamp on each phase should light: A " + read(server, 13, up, z) + " | B " + read(server, 11, up, z - 2) + " | C " + read(server, 11, up, z + 2));
		String lampB = read(server, 11, up, z - 2);
		double phase = amps(read(server, 12, up, z));
		double balanced = amps(read(server, 11, up + 1, z));
		check(phase > 0.3 && balanced < phase * 0.2, "a balanced load returns almost nothing on the neutral: phase " + phase + " A, neutral " + balanced + " A");
		context.takeScreenshot("wildcatter-three-phase");
		put(server, 11, up, z + 2, "minecraft:air");
		context.waitTicks(40);
		double unbalanced = amps(read(server, 11, up + 1, z));
		check(Math.abs(unbalanced - phase) < phase * 0.25, "with phase C's lamp gone the neutral carries one lamp's current: " + unbalanced + " A, expected about " + phase);
		System.out.println("[wildcatter-test] three phase ok: phase " + phase + " A, neutral balanced " + balanced + " A, unbalanced " + unbalanced + " A; " + lampB);
	}

	/** A tree of about a thousand wire blocks on a turbine: one solve must fit the 5 ms budget (ELECTRICAL_MODEL). */
	private void solverBudget(ClientGameTestContext context, TestServerContext server) {
		int z0 = -232;
		server.runCommand("fill 8 " + y + " " + z0 + " 44 " + (y + 3) + " " + (z0 + 66) + " minecraft:air");
		plant(server, z0);
		server.runCommand("fill 12 " + up + " " + z0 + " 12 " + up + " " + (z0 + 63) + " wildcatter:wool_wire_hot");
		for (int dz = 0; dz < 64; dz += 2) {
			server.runCommand("fill 13 " + up + " " + (z0 + dz) + " 42 " + up + " " + (z0 + dz) + " wildcatter:wool_wire_hot");
		}
		server.runCommand("tp @p 26 " + (up + 20) + " " + (z0 + 32) + " 0 90");
		context.waitTicks(60);
		dev.wildcatter.circuit.CircuitManager.resetSolveStats();
		context.waitTicks(40);
		int nodes = dev.wildcatter.circuit.CircuitManager.largestNodes;
		double ms = dev.wildcatter.circuit.CircuitManager.largestNanos / 1e6;
		System.out.println("[wildcatter-test] solver budget: " + nodes + " nodes solved in " + String.format("%.2f", ms) + " ms");
		check(nodes >= 1000, "the test network should have at least 1,000 nodes: " + nodes);
		check(ms < 5.0, "a 1,000-node network must solve in under 5 ms: " + ms + " ms for " + nodes + " nodes");
		server.runCommand("fill 8 " + y + " " + z0 + " 44 " + (y + 3) + " " + (z0 + 66) + " minecraft:air");
	}

	/**
	 * 480 V three-phase round the grid from a diesel genset: a unit substation run backward to 4.16 kV, a substation and a distribution
	 * transformer (face to face, so all three phases pass), 12.47 kV underground cable, a 480 V pad-mount, a three-pole breaker and
	 * cable to a 15 kW heater; and cable straight off the genset to a second heater. A phase-to-phase short trips all three poles.
	 */
	private void industrial(ClientGameTestContext context, TestServerContext server) {
		int z = -300;
		server.runCommand("tp @p 16 " + (up + 3) + " " + (z + 6) + " 160 30");
		context.waitTicks(20);
		server.runCommand("fill 4 " + (y - 1) + " " + (z - 6) + " 26 " + (y + 4) + " " + (z + 6) + " minecraft:air");
		server.runCommand("fill 4 " + (y - 1) + " " + (z - 6) + " 26 " + (y - 1) + " " + (z + 6) + " minecraft:stone");
		BlockPos set = new BlockPos(10, up, z);
		genset(server, set, net.minecraft.core.Direction.NORTH, false);   // the skid runs west of the panel; its terminals face east
		// the unit substation, true size x 11-14 and z-1 to z: 480 V in at its west end, 4.16 kV out of its east end
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, set, net.minecraft.core.Direction.EAST, false);
		// 4.16 kV delta to 12.47 kV wye, true size, face to face with it: 2 m deep (x 15-16), 3 across (z-1 to z+1), 3 high; the bushings on top at x 16
		placeBig(server, WildcatterBlocks.GSU_TRANSFORMER, new BlockPos(15, up, z), net.minecraft.core.Direction.EAST);
		put(server, 17, up + 2, z, "wildcatter:cable_dist");   // off the bushings' terminals and down to the pad-mount
		put(server, 17, up + 1, z, "wildcatter:cable_dist");
		put(server, 17, up, z, "wildcatter:cable_dist");
		inlineTransformer(server, WildcatterBlocks.PADMOUNT_480, new BlockPos(17, up, z), net.minecraft.core.Direction.EAST, false);   // x 18-19, 480 V out at x 20
		put(server, 20, up, z, "wildcatter:cable_industrial");
		put(server, 21, up, z, "wildcatter:breaker_3p_industrial[facing=east]");
		put(server, 22, up, z, "wildcatter:cable_industrial");
		put(server, 23, up, z, "wildcatter:three_phase_heater");
		// 480 V cable straight off the genset's other terminal cell (the back of the panel's end face)
		put(server, 11, up, z + 1, "wildcatter:cable_industrial");
		put(server, 11, up, z + 2, "wildcatter:cable_industrial");
		put(server, 11, up, z + 3, "wildcatter:three_phase_heater");
		startGenset(context, server, set, net.minecraft.core.Direction.NORTH, 1000);
		server.runCommand("tp @p 17 " + (up + 3) + " " + (z + 8) + " 180 15");
		context.waitTicks(200);
		String heater = read(server, 23, up, z);
		String cable = read(server, 22, up, z);
		String pad = read(server, 19, up, z);
		String unitHeater = read(server, 11, up, z + 3);
		check(heater.contains("running") && heater.matches(".*A-B 4[5-9]\\d V, B-C 4[5-9]\\d V, C-A 4[5-9]\\d V.*"),
				"the heater behind the pad-mount should run on 480 V between each pair of phases: " + heater + " | pad " + pad + " | cable " + cable + " | set " + read(server, 10, up, z));
		check(cable.matches(".*A 2[6-8]\\d V \\d+\\.\\d A, B 2[6-8]\\d V \\d+\\.\\d A, C 2[6-8]\\d V.*"), "each phase of the cable should be about 277 V to neutral: " + cable);
		check(pad.contains("Three-phase transformer") && pad.contains("300 kVA"), "the pad-mount should be a rated three-phase unit: " + pad);
		check(unitHeater.contains("running"), "the heater on the genset's cable should run: " + unitHeater + " | " + read(server, 11, up, z + 1));
		context.takeScreenshot("wildcatter-industrial");
		// a NEMA L22-30 three-phase receptacle and plug on the unit substation's cable, handing 10 kW to a stand-in machine
		put(server, 11, up, z + 3, "wildcatter:receptacle_nema_l22_30");
		put(server, 11, up, z + 4, "wildcatter:plug_nema_l22_30");
		put(server, 11, up, z + 5, "minecraft:iron_block");
		java.util.concurrent.atomic.AtomicLong delivered = new java.util.concurrent.atomic.AtomicLong();
		dev.wildcatter.energy.EnergyBridge.clear();
		dev.wildcatter.energy.EnergyBridge.register(target -> target.level().getBlockState(target.pos()).is(net.minecraft.world.level.block.Blocks.IRON_BLOCK)
				? new dev.wildcatter.energy.HostEnergy() {
					@Override
					public long demand() {
						return 10_000;
					}

					@Override
					public long insert(long fe) {
						delivered.addAndGet(fe);
						return fe;
					}
				} : null);
		String plug;
		long gained;
		try {
			context.waitTicks(80);
			long before = delivered.get();
			context.waitTicks(100);
			gained = delivered.get() - before;
			plug = read(server, 11, up, z + 4);
			boolean wrongFits = server.computeOnServer(srv -> dev.wildcatter.block.PlugAdapterBlock.fits(srv.overworld(), new BlockPos(11, up, z + 4), dev.wildcatter.energy.PlugType.NEMA_L21_30));
			check(!wrongFits, "an L21-30 plug must not fit an L22-30 receptacle");
		} finally {
			dev.wildcatter.energy.EnergyBridge.clear();
		}
		check(plug.contains("delivering") && gained > 700_000 && gained < 1_200_000, "an L22-30 plug should hand over about 10,000 FE per tick (" + gained + " in 100 ticks): " + plug);
		System.out.println("[wildcatter-test] three-phase plug ok: " + plug + " | FE gained " + gained);
		// a phase-to-phase short past the breaker: bare A and B wires touching
		put(server, 23, up, z, "wildcatter:industrial_wire_hot[conductor=a]");
		put(server, 22, up, z + 1, "wildcatter:industrial_wire_hot[conductor=b]");
		put(server, 23, up, z + 1, "wildcatter:industrial_wire_hot[conductor=b]");
		context.waitTicks(40);
		String breaker = read(server, 21, up, z);
		check(breaker.contains("Three-pole breaker") && breaker.contains("TRIPPED") && breaker.contains("short circuit"),
				"a phase-to-phase short should trip the three-pole breaker: " + breaker);
		System.out.println("[wildcatter-test] industrial ok: " + heater + " | " + cable + " | " + pad + " | " + unitHeater + " | " + breaker);
	}

	/**
	 * A pumpjack's 480 V motor fed by three separate plastic wires (A, B, C) from a unit substation: it starts and pumps, stops
	 * drawing when its tank is full, runs backward with two leads swapped, and stalls when one phase is cut.
	 */
	private void motors(ClientGameTestContext context, TestServerContext server) {
		int z = -330;
		int px = 15;
		server.runCommand("tp @p 14 " + (up + 2) + " " + (z + 6) + " 180 15");
		context.waitTicks(20);
		server.runCommand("fill 6 " + (y - 1) + " " + (z - 4) + " 22 " + (y + 4) + " " + (z + 4) + " minecraft:air");
		server.runCommand("fill 6 " + (y - 1) + " " + (z - 4) + " 22 " + (y - 1) + " " + (z + 4) + " minecraft:stone");
		long seed = -1;
		for (long s = 1; s < 2_000_000 && seed < 0; s++) {
			Reservoir oil = Reservoir.at(s, px + 6, z);   // under its wellhead
			if (oil != null && oil.kind() == Reservoir.Kind.OIL && oil.units() > 10_000) {
				seed = s;
			}
		}
		check(seed >= 0, "no seed puts an oil field under the test pumpjack");
		ReservoirData.testSeed = seed;
		try {
			BlockPos set = new BlockPos(11, up, z);
			genset(server, set, net.minecraft.core.Direction.NORTH, false);   // the skid runs west of the panel; its terminals face east
			put(server, 12, up, z, "wildcatter:cable_industrial");
			// phase A out of the cable's east face, B out of its north face, C out of its south face, each its own plastic wire to the pumpjack
			put(server, 13, up, z, "wildcatter:industrial_plastic_wire_hot[conductor=a]");
			put(server, 14, up, z, "wildcatter:industrial_plastic_wire_hot[conductor=a]");
			for (int x = 12; x <= px; x++) {
				put(server, x, up, z - 1, "wildcatter:industrial_plastic_wire_hot[conductor=b]");
				put(server, x, up, z + 1, "wildcatter:industrial_plastic_wire_hot[conductor=c]");
			}
			pumpjack(server, new BlockPos(px, up, z), net.minecraft.core.Direction.EAST);   // its terminal box at px, wired on three sides
			startGenset(context, server, set, net.minecraft.core.Direction.NORTH, 1000);
			context.waitTicks(100);   // the motor starts (2 s of inrush) and runs
			String jack = read(server, px, up, z);
			double running = amps(read(server, 14, up, z));
			check(jack.contains("pumping") && jack.contains("running,"), "a pumpjack on 480 V three-phase should start and pump: " + jack + " | sub " + read(server, 12, up, z));
			check(running > 8 && running < 15, "a 7.5 kW motor at 480 V should draw about 11 A: " + running + " A");
			context.takeScreenshot("wildcatter-motor");
			// a full tank: the motor stops and draws nothing
			server.computeOnServer(srv -> {
				jack(srv, new BlockPos(px, up, z)).testFill(Tuning.PUMPJACK_TANK);
				return null;
			});
			context.waitTicks(60);
			String full = read(server, px, up, z);
			double idle = amps(read(server, 14, up, z));
			check(full.contains("motor stopped") && idle < 0.5, "with its tank full the pumpjack should stop drawing: " + full + " | " + idle + " A");
			// emptied, it starts again; then two leads swapped at the terminal box run it backward
			server.computeOnServer(srv -> {
				dev.wildcatter.block.PumpjackBlockEntity j = jack(srv, new BlockPos(px, up, z));
				j.take(Tuning.PUMPJACK_TANK);
				j.swapLeads();
				return null;
			});
			context.waitTicks(140);
			String backward = read(server, px, up, z);
			check(backward.contains("RUNNING BACKWARD") && backward.contains("lifting nothing"), "with two leads swapped the motor should run backward and lift nothing: " + backward);
			server.computeOnServer(srv -> {
				jack(srv, new BlockPos(px, up, z)).swapLeads();
				return null;
			});
			context.waitTicks(80);
			// cut phase C: the running motor stalls and draws locked-rotor current on the other two
			put(server, 13, up, z + 1, "minecraft:air");
			context.waitTicks(40);
			String stalled = read(server, px, up, z);
			double stall = amps(read(server, 14, up, z));
			check(stalled.contains("STALLED") && stall > running * 3, "with a phase cut the motor should stall and draw locked-rotor current: " + stalled + " | " + stall + " A");
			System.out.println("[wildcatter-test] motors ok: " + jack + " | full " + full + " | " + backward + " | " + stalled + " | running " + running + " A, stalled " + stall + " A");
		} finally {
			ReservoirData.testSeed = null;
		}
	}

	private dev.wildcatter.block.MotorStarterBlockEntity starter(net.minecraft.server.MinecraftServer srv, int x, int yy, int z) {
		return (dev.wildcatter.block.MotorStarterBlockEntity) srv.overworld().getBlockEntity(new BlockPos(x, yy, z));
	}

	/**
	 * A motor control center at true size (4.5f, docs/multiblocks/motor-control-center.md): a tall incoming section fed on its top
	 * from a genset, and two starter sections to its right. Buckets are clicked into their spaces. Section 1's bottom bucket runs a
	 * pumpjack; section 2's middle bucket runs one wired with only two phases, which cannot start, stalls, and trips that bucket's
	 * overload relay while bucket 1 keeps running. Breaking section 1 drops it and its bucket and cuts the bus to section 2; an older
	 * world's one-block bucket drops as a bucket item.
	 */
	private void motorControlCenter(ClientGameTestContext context, TestServerContext server) {
		int z = -360;
		server.runCommand("tp @p 16 " + (up + 3) + " " + (z + 7) + " 180 15");
		context.waitTicks(20);
		server.runCommand("fill 6 " + (y - 1) + " " + (z - 10) + " 24 " + (y + 6) + " " + (z + 4) + " minecraft:air");
		server.runCommand("fill 6 " + (y - 1) + " " + (z - 10) + " 24 " + (y - 1) + " " + (z + 4) + " minecraft:stone");
		server.runCommand("kill @e[type=item]");
		long seed = -1;
		for (long s = 1; s < 3_000_000 && seed < 0; s++) {
			Reservoir a = Reservoir.at(s, 15, z - 8);   // under the two pumpjacks' wellheads
			Reservoir b = Reservoir.at(s, 18, z - 9);
			if (a != null && b != null && a.kind() == Reservoir.Kind.OIL && b.kind() == Reservoir.Kind.OIL && a.units() > 10_000 && b.units() > 10_000) {
				seed = s;
			}
		}
		check(seed >= 0, "no seed puts oil under both MCC pumpjacks");
		ReservoirData.testSeed = seed;
		try {
			net.minecraft.core.Direction south = net.minecraft.core.Direction.SOUTH;
			placeBig(server, WildcatterBlocks.MCC_INCOMING, new BlockPos(14, up, z), south);   // its front south; the sections go east
			placeBig(server, WildcatterBlocks.MCC_SECTION, new BlockPos(15, up, z), south);
			placeBig(server, WildcatterBlocks.MCC_SECTION, new BlockPos(16, up, z), south);
			BlockPos set = new BlockPos(13, up, z);
			genset(server, set, net.minecraft.core.Direction.NORTH, false);   // beside the incoming section; the skid runs west
			// off the back of the panel's end face, up the front of the incoming section, and onto its top
			for (int k = 0; k <= 3; k++) {
				put(server, 14, up + k, z + 1, "wildcatter:cable_industrial");
			}
			put(server, 14, up + 3, z, "wildcatter:cable_industrial");
			// buckets clicked into section 1's bottom space and section 2's middle space
			check(clickWith(server, new BlockPos(15, up, z), south, new ItemStack(WildcatterItems.MCC_BUCKET)), "a bucket should click into an empty space");
			check(clickWith(server, new BlockPos(16, up + 1, z), south, new ItemStack(WildcatterItems.MCC_BUCKET)), "a bucket should click into section 2's middle space");
			check(stateAt(server, new BlockPos(15, up, z)).getValue(dev.wildcatter.block.MccSectionBlock.BUCKET) == dev.wildcatter.block.MccSectionBlock.Bucket.STOPPED,
					"the space should show its bucket: " + stateAt(server, new BlockPos(15, up, z)));
			// bucket 1's motor leads leave the back of its cell to a pumpjack's terminal box; the pumpjack runs on north
			put(server, 15, up, z - 1, "wildcatter:cable_industrial");
			pumpjack(server, new BlockPos(15, up, z - 2), net.minecraft.core.Direction.NORTH);
			// section 2's bucket: phase A to one side of a second pumpjack's box and phase B to its back, no phase C
			put(server, 16, up + 1, z - 1, "wildcatter:cable_industrial");
			put(server, 17, up + 1, z - 1, "wildcatter:cable_industrial");
			put(server, 17, up, z - 1, "wildcatter:cable_industrial");
			put(server, 17, up, z - 2, "wildcatter:industrial_plastic_wire_hot[conductor=a]");
			put(server, 17, up, z - 3, "wildcatter:industrial_plastic_wire_hot[conductor=a]");
			put(server, 18, up, z - 1, "wildcatter:industrial_plastic_wire_hot[conductor=b]");
			put(server, 18, up, z - 2, "wildcatter:industrial_plastic_wire_hot[conductor=b]");
			pumpjack(server, new BlockPos(18, up, z - 3), net.minecraft.core.Direction.NORTH);
			startGenset(context, server, set, net.minecraft.core.Direction.NORTH, 1000);
			server.computeOnServer(srv -> {
				section(srv, 15, z).pushButton(0);
				section(srv, 16, z).pushButton(1);
				return null;
			});
			context.waitTicks(120);
			String mcc = read(server, 14, up, z);
			check(mcc.contains("2 bucket(s)") && mcc.contains("main 100 A ON"), "the incoming section and its two sections' buckets should form a lineup: " + mcc);
			String jack = read(server, 15, up, z - 2);
			check(jack.contains("pumping"), "the pumpjack on bucket 1 should run: " + jack + " | " + read(server, 15, up, z) + " | " + mcc);
			check(read(server, 18, up, z - 3).contains("STALLED"), "a motor with two phases cannot start: " + read(server, 18, up, z - 3));
			String tripped = "";
			for (int waited = 0; waited < 600; waited += 40) {
				tripped = read(server, 16, up, z);
				if (tripped.contains("TRIPPED")) {
					break;
				}
				context.waitTicks(40);
			}
			check(tripped.contains("TRIPPED") && tripped.contains("overload"), "the stalled motor should trip its bucket's overload relay: " + tripped);
			String first = read(server, 15, up, z);
			check(first.contains("1 running"), "bucket 1 should keep running when the other trips: " + first);
			check(stateAt(server, new BlockPos(16, up + 1, z)).getValue(dev.wildcatter.block.MccSectionBlock.BUCKET) == dev.wildcatter.block.MccSectionBlock.Bucket.TRIPPED,
					"the tripped bucket's space should show it");
			mcc = read(server, 14, up, z);
			context.takeScreenshot("wildcatter-mcc");
			// break section 1: it drops with its bucket, and section 2 is off the bus
			server.runCommand("kill @e[type=item]");
			server.computeOnServer(srv -> srv.overworld().destroyBlock(new BlockPos(15, up + 2, z), true));
			context.waitTicks(20);
			String cut = read(server, 16, up, z);
			check(cut.contains("NOT ON AN MCC BUS"), "with section 1 gone section 2 should be off the bus: " + cut);
			check(itemsNear(server, new BlockPos(15, up, z), WildcatterBlocks.MCC_SECTION) == 1 && itemsNear(server, new BlockPos(15, up, z), WildcatterItems.MCC_BUCKET) == 1,
					"a broken section should drop itself and its bucket");
			// an older world's one-block bucket drops as a bucket item
			server.runCommand("kill @e[type=item]");
			put(server, 21, up, z, "wildcatter:mcc_bucket[facing=south]");
			context.waitTicks(10);
			check(isAir(server, 21, up, z) && itemsNear(server, new BlockPos(21, up, z), WildcatterItems.MCC_BUCKET) == 1, "an older world's one-block bucket should drop as a bucket item");
			System.out.println("[wildcatter-test] mcc ok: " + mcc + " | " + first + " | " + tripped + " | " + cut);
		} finally {
			ReservoirData.testSeed = null;
		}
	}

	private dev.wildcatter.block.MccSectionBlockEntity section(net.minecraft.server.MinecraftServer srv, int x, int z) {
		return (dev.wildcatter.block.MccSectionBlockEntity) srv.overworld().getBlockEntity(new BlockPos(x, up, z));
	}

	/** A player's right-click with {@code stack} on {@code face} of the block at {@code at}: true if the block used it. */
	private boolean clickWith(TestServerContext server, BlockPos at, net.minecraft.core.Direction face, ItemStack stack) {
		return server.computeOnServer(srv -> {
			net.minecraft.server.level.ServerPlayer player = srv.getPlayerList().getPlayers().get(0);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
			net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
					net.minecraft.world.phys.Vec3.atCenterOf(at).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5), face, at, false);
			boolean used = srv.overworld().getBlockState(at).useItemOn(stack, srv.overworld(), player, net.minecraft.world.InteractionHand.MAIN_HAND, hit).consumesAction();
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			return used;
		});
	}

	private double starterAmps(String readout) {
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("RUNNING, ([0-9.]+) A").matcher(readout);
		check(m.find(), "no running current in the starter's readout: " + readout);
		return Double.parseDouble(m.group(1));
	}

	/**
	 * A standalone combination starter between a unit substation's cable and a pumpjack: stopped it passes nothing, the button
	 * and a redstone signal start and stop it, an overload dial set far below the motor's current trips it, and a tripped
	 * starter ignores redstone until it is reset by hand.
	 */
	private void motorStarter(ClientGameTestContext context, TestServerContext server) {
		int z = -385;
		int px = 17;
		server.runCommand("tp @p 14 " + (up + 2) + " " + (z + 6) + " 180 15");
		context.waitTicks(20);
		server.runCommand("fill 6 " + (y - 1) + " " + (z - 4) + " 24 " + (y + 5) + " " + (z + 4) + " minecraft:air");
		server.runCommand("fill 6 " + (y - 1) + " " + (z - 4) + " 24 " + (y - 1) + " " + (z + 4) + " minecraft:stone");
		long seed = -1;
		for (long s = 1; s < 2_000_000 && seed < 0; s++) {
			Reservoir oil = Reservoir.at(s, px + 6, z);   // under its wellhead
			if (oil != null && oil.kind() == Reservoir.Kind.OIL && oil.units() > 10_000) {
				seed = s;
			}
		}
		check(seed >= 0, "no seed puts an oil field under the starter's pumpjack");
		ReservoirData.testSeed = seed;
		try {
			BlockPos set = new BlockPos(12, up, z);
			genset(server, set, net.minecraft.core.Direction.NORTH, false);   // the skid runs west
			put(server, 13, up, z, "wildcatter:cable_industrial");
			put(server, 14, up, z, "wildcatter:motor_starter[facing=east]");   // line in at the back (west), motor out of the front (east)
			put(server, 15, up, z, "wildcatter:cable_industrial");
			put(server, 16, up, z, "wildcatter:cable_industrial");
			pumpjack(server, new BlockPos(px, up, z), net.minecraft.core.Direction.EAST);
			startGenset(context, server, set, net.minecraft.core.Direction.NORTH, 1000);
			String stopped = read(server, 14, up, z);
			String idleJack = read(server, px, up, z);
			check(stopped.contains("STOPPED") && !idleJack.contains("pumping"), "a stopped starter should pass nothing: " + stopped + " | " + idleJack);
			// the button starts it
			server.computeOnServer(srv -> {
				starter(srv, 14, up, z).pushButton();
				return null;
			});
			context.waitTicks(160);
			String running = read(server, 14, up, z);
			double amps = starterAmps(running);
			check(read(server, px, up, z).contains("pumping") && amps > 8 && amps < 15, "the started motor should pump at about 11 A: " + running + " | " + read(server, px, up, z));
			context.takeScreenshot("wildcatter-motor-starter");
			// the button stops it
			server.computeOnServer(srv -> {
				starter(srv, 14, up, z).pushButton();
				return null;
			});
			context.waitTicks(40);
			check(read(server, 14, up, z).contains("STOPPED"), "a second push should stop it: " + read(server, 14, up, z));
			// redstone on starts it, redstone off stops it
			put(server, 14, up + 1, z, "minecraft:redstone_block");
			context.waitTicks(120);
			String byRedstone = read(server, 14, up, z);
			check(byRedstone.contains("RUNNING"), "a redstone signal should start it: " + byRedstone);
			put(server, 14, up + 1, z, "minecraft:air");
			context.waitTicks(40);
			check(read(server, 14, up, z).contains("STOPPED"), "losing the signal should stop it: " + read(server, 14, up, z));
			// run it, then turn the overload dial down to 3 A: the 11 A motor heats the relay until it trips
			server.computeOnServer(srv -> {
				starter(srv, 14, up, z).pushButton();
				return null;
			});
			context.waitTicks(160);
			server.computeOnServer(srv -> {
				dev.wildcatter.block.MotorStarterBlockEntity s = starter(srv, 14, up, z);
				while (s.overloadAmps() != 3.0) {
					s.nextOverloadSetting();
				}
				return null;
			});
			String tripped = "";
			for (int waited = 0; waited < 1200; waited += 40) {
				tripped = read(server, 14, up, z);
				if (tripped.contains("TRIPPED")) {
					break;
				}
				context.waitTicks(40);
			}
			check(tripped.contains("TRIPPED") && tripped.contains("overload"), "an 11 A motor on a 3 A overload setting should trip the relay: " + tripped);
			// tripped, it ignores redstone; a push resets it to stopped
			put(server, 14, up + 1, z, "minecraft:redstone_block");
			context.waitTicks(40);
			check(read(server, 14, up, z).contains("TRIPPED"), "a tripped starter must not restart on redstone: " + read(server, 14, up, z));
			put(server, 14, up + 1, z, "minecraft:air");
			server.computeOnServer(srv -> {
				starter(srv, 14, up, z).pushButton();
				return null;
			});
			context.waitTicks(20);
			String reset = read(server, 14, up, z);
			check(reset.contains("STOPPED"), "a push after a trip should reset it to stopped: " + reset);
			System.out.println("[wildcatter-test] motor starter ok: " + running + " | " + byRedstone + " | " + tripped + " | " + reset);
		} finally {
			ReservoirData.testSeed = null;
		}
	}

	/**
	 * A 138 kV three-phase overhead line: a substation transformer's bushings feed high-voltage cable, which splits into
	 * three single-conductor risers (A, B, C) to three insulators; spans run to three more, and risers and cable bring the
	 * phases back together into a distribution transformer, a pad-mount and a 480 V heater.
	 */
	private void overheadThreePhase(ClientGameTestContext context, TestServerContext server) {
		int z = -410;
		server.runCommand("tp @p 20 " + (up + 6) + " " + (z + 12) + " 180 20");
		context.waitTicks(20);
		server.runCommand("fill 6 " + (y - 1) + " " + (z - 6) + " 50 " + (y + 22) + " " + (z + 6) + " minecraft:air");
		server.runCommand("fill 6 " + (y - 1) + " " + (z - 6) + " 50 " + (y - 1) + " " + (z + 6) + " minecraft:stone");
		server.runCommand("fill 27 " + y + " " + (z - 6) + " 50 " + y + " " + (z + 6) + " minecraft:stone");   // the yard's ground at y: it stands on it
		BlockPos set = new BlockPos(10, up, z);
		genset(server, set, net.minecraft.core.Direction.NORTH, false);   // the skid runs west
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, set, net.minecraft.core.Direction.EAST, false);   // 480 V up to 4.16 kV, x 11-14
		put(server, 15, up, z, "wildcatter:substation_transformer[facing=east]");
		// near end: cable off the 138 kV bushings, split into a riser per phase up an H-frame's strings (A on z, B 4 m north, C 4 m south)
		for (int cable : new int[] {16, 27}) {
			for (int dz = -1; dz <= 1; dz++) {
				put(server, cable, up, z + dz, "wildcatter:cable_hv");
			}
		}
		String[] conductor = {"a", "b", "c"};
		int[] row = {z, z - 4, z + 4};
		hFrame(server, 18, z);
		hFrame(server, 24, z);
		for (int k = 0; k < 3; k++) {
			String wire = "wildcatter:hv_wire_hot[conductor=" + conductor[k] + "]";
			for (int h = 0; h <= 18; h++) {
				put(server, 17, up + h, row[k], wire);
				put(server, 25, up + h, row[k], wire);
			}
			if (k > 0) {   // B and C from the cable's ends, out to their rows; at the far end round to the cable's ends
				int side = row[k] < z ? -1 : 1;
				put(server, 16, up, z + 2 * side, wire);
				put(server, 16, up, z + 3 * side, wire);
				put(server, 16, up, z + 4 * side, wire);
				put(server, 26, up, z + 4 * side, wire);
				put(server, 27, up, z + 4 * side, wire);
				put(server, 27, up, z + 3 * side, wire);
				put(server, 27, up, z + 2 * side, wire);
			} else {
				put(server, 26, up, z, wire);
			}
		}
		// far end: the substation yard at true size (docs/multiblocks/substation.md), 16 x 9 m from x 28, its centre line on z: the
		// 138 kV breaker x 28-29 takes the line at its west face; the feeder breaker x 42-43 sends 12.47 kV out of its east face to a
		// pad-mount and a heater. The transformer's main cell is at (34, z-2), the relay panel's at (36, z-4).
		BlockPos transformer = new BlockPos(34, up, z - 2);
		java.util.List<BlockPos> overwrote = server.computeOnServer(srv -> dev.wildcatter.block.SubstationLayout.layout().build(srv.overworld(), transformer, net.minecraft.core.Direction.NORTH, 0));
		check(overwrote.isEmpty(), "the yard should be built on clear ground: " + overwrote);
		put(server, 44, up, z, "wildcatter:cable_dist");
		inlineTransformer(server, WildcatterBlocks.PADMOUNT_480, new BlockPos(44, up, z), net.minecraft.core.Direction.EAST, false);   // x 45-46, 480 V out at x 47
		put(server, 47, up, z, "wildcatter:cable_industrial");
		put(server, 48, up, z, "wildcatter:three_phase_heater");
		// a stretch of the yard's fence along its south side: chain link with the barbed-wire top
		for (int x = 27; x <= 44; x++) {
			put(server, x, up, z + 6, "wildcatter:chain_link_fence");
			put(server, x, up + 1, z + 6, "wildcatter:barbed_wire_top");
		}
		context.waitTicks(40);
		for (int k = 0; k < 3; k++) {
			BlockPos near = new BlockPos(18, up + 18, row[k]);
			BlockPos far = new BlockPos(24, up + 18, row[k]);
			dev.wildcatter.item.SpoolItem.Result strung = server.computeOnServer(srv ->
					dev.wildcatter.item.SpoolItem.connect(srv.overworld(), near, far, dev.wildcatter.energy.Voltage.HIGH, false, 1024));
			check(strung.ok(), "a 138 kV span on phase " + conductor[k] + " should be strung: " + strung);
		}
		startGenset(context, server, set, net.minecraft.core.Direction.NORTH, 1000);
		StringBuilder line = new StringBuilder();
		for (int k = 0; k < 3; k++) {
			String far = read(server, 24, up + 18, row[k]);
			line.append(far).append(" | ");
			check(far.contains("1 span") && far.matches(".*\\b(7[5-9]|8[0-2])\\d{3} V.*"), "each phase at the far end of the 138 kV line should be about 79.7 kV to neutral: " + far);
		}
		String heater = read(server, 48, up, z);
		String sub = read(server, 34, up, z - 2);
		check(sub.contains("Delta-wye power transformer") && sub.matches(".*high side \\+30 degrees.*") && sub.contains("relays"),
				"the substation should form, with its 138 kV side 30 degrees ahead: " + sub);
		check(heater.contains("running") && heater.matches(".*A-B 4[5-9]\\d V, B-C 4[5-9]\\d V, C-A 4[5-9]\\d V.*"),
				"the heater past the substation should run on 480 V between each pair of phases: " + heater + " | sub " + sub + " | pad " + read(server, 45, up, z) + " | " + line);
		server.runCommand("tp @p 2 " + (up + 14) + " " + (z + 34) + " -145 12");
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-overhead-three-phase");
		server.runCommand("tp @p 50 " + (up + 8) + " " + (z - 10) + " 50 25");
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-substation-back");
		server.runCommand("tp @p 36 " + (up + 20) + " " + (z + 1) + " 90 75");
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-substation-top");
		System.out.println("[wildcatter-test] 138 kV three-phase overhead ok: " + line + heater + " | " + sub);
		// a fault on the feeder: bare A and B touching past the feeder breaker. The relay panel trips both breakers.
		dev.wildcatter.block.PowerTransformerBlockEntity.testRelayScale = 0.02;   // a 500 kW genset makes about 22 A of fault current at 138 kV: far below real substation settings
		try {
			put(server, 44, up + 1, z, "wildcatter:dist_wire_hot[conductor=a]");
			put(server, 44, up, z + 1, "wildcatter:dist_wire_hot[conductor=b]");
			put(server, 44, up + 1, z + 1, "wildcatter:dist_wire_hot[conductor=b]");
			context.waitTicks(40);
			String feeder = read(server, 42, up, z - 1);
			String relay = read(server, 34, up, z - 2);
			check(feeder.contains("TRIPPED by the relay panel") && relay.contains("RELAY TRIPPED"), "a feeder fault should trip the substation's breakers by relay: " + feeder + " | " + relay);
			check(!read(server, 48, up, z).contains("running"), "the heater should lose power with the feeder: " + read(server, 48, up, z));
			// clear the fault, reset at the relay panel, and the feeder is back
			put(server, 44, up + 1, z, "minecraft:air");
			put(server, 44, up + 1, z + 1, "minecraft:air");
			put(server, 44, up, z + 1, "minecraft:air");
			server.computeOnServer(srv -> {
				dev.wildcatter.block.RelayPanelBlock.transformer(srv.overworld(), new BlockPos(36, up, z - 4)).resetRelays();
				return null;
			});
			context.waitTicks(60);
			check(read(server, 48, up, z).contains("running"), "after a reset the heater should run again: " + read(server, 48, up, z) + " | " + read(server, 34, up, z - 2));
			// open the feeder breaker, then its disconnect: no load, no arc, and the blades swing open
			server.computeOnServer(srv -> {
				((dev.wildcatter.block.BreakerBlockEntity) srv.overworld().getBlockEntity(new BlockPos(42, up, z - 1))).tripByRelay("opened by hand for the test");
				((dev.wildcatter.block.DisconnectSwitchBlockEntity) srv.overworld().getBlockEntity(new BlockPos(41, up, z - 1))).setOpen(true);
				// and the 138 kV side: its breaker, then its disconnect, whose arms swing open
				((dev.wildcatter.block.BreakerBlockEntity) srv.overworld().getBlockEntity(new BlockPos(28, up, z - 4))).tripByRelay("opened by hand for the test");
				((dev.wildcatter.block.DisconnectSwitchBlockEntity) srv.overworld().getBlockEntity(new BlockPos(30, up, z - 4))).setOpen(true);
				return null;
			});
			context.waitTicks(60);
			String disc = read(server, 41, up, z - 1);
			String hvDisc = read(server, 30, up, z - 4);
			check(disc.contains("OPEN") && !disc.contains("an arc"), "a disconnect opened after its breaker should open without an arc: " + disc);
			check(hvDisc.contains("OPEN") && !hvDisc.contains("an arc"), "the 138 kV disconnect opened after its breaker should open without an arc: " + hvDisc);
			server.runCommand("tp @p 24 " + (up + 11) + " " + (z - 12) + " -50 35");
			context.waitTicks(20);
			context.takeScreenshot("wildcatter-substation");
			System.out.println("[wildcatter-test] substation ok: " + feeder + " | " + relay + " | " + disc);
		} finally {
			dev.wildcatter.block.PowerTransformerBlockEntity.testRelayScale = null;
		}
	}

	/**
	 * A 500 kW diesel genset steps up through a pad-mount to 12.47 kV; the cable splits into three overhead phases, each to a pole
	 * transformer running a 120 V heater. With all three hung on phase A, that stator phase carries three times its share and the
	 * set trips on stator overcurrent while the frequency is still in band. Moved to A, B and C, the same load runs cool.
	 * (The test shrinks the stator rating so three heaters are enough.)
	 */
	private void statorLimit(ClientGameTestContext context, TestServerContext server) {
		int z = -440;
		server.runCommand("tp @p 16 " + (up + 3) + " " + (z + 10) + " 180 25");
		context.waitTicks(20);
		server.runCommand("fill 6 " + (y - 1) + " " + (z - 6) + " 22 " + (y + 4) + " " + (z + 6) + " minecraft:air");
		server.runCommand("fill 6 " + (y - 1) + " " + (z - 6) + " 22 " + (y - 1) + " " + (z + 6) + " minecraft:stone");
		BlockPos set = new BlockPos(12, up, z);
		genset(server, set, net.minecraft.core.Direction.NORTH, false);   // the skid runs west
		inlineTransformer(server, WildcatterBlocks.PADMOUNT_480, set, net.minecraft.core.Direction.EAST, true);   // 480 V in at x 13, 12.47 kV out at x 15
		for (int dz = -2; dz <= 2; dz++) {
			put(server, 15, up, z + dz, "wildcatter:cable_dist");
		}
		int[] row = {z, z + 3, z - 3};
		java.util.function.BiConsumer<Integer, String> feeder = (k, conductor) -> {
			for (int x = k == 0 ? 16 : 15; x <= 17; x++) {
				put(server, x, up, row[k], "wildcatter:dist_plastic_wire_hot[conductor=" + conductor + "]");
			}
		};
		for (int k = 0; k < 3; k++) {
			feeder.accept(k, "a");
			placeBig(server, WildcatterBlocks.WOOD_POLE_40, new BlockPos(18, up, row[k] - 1), net.minecraft.core.Direction.NORTH);
			put(server, 18, up, row[k], "wildcatter:pole_transformer[facing=west]");
			put(server, 19, up, row[k], "wildcatter:wool_wire_hot");
			put(server, 20, up, row[k], "wildcatter:electric_heater");
			for (int x = 18; x <= 20; x++) {
				put(server, x, y, row[k], "wildcatter:wool_wire_neutral");
			}
		}
		dev.wildcatter.circuit.StatorHeating.testRatingScale = 0.012;
		try {
			startGenset(context, server, set, net.minecraft.core.Direction.NORTH, 1000);
			context.waitTicks(60);
			String heating = read(server, 12, up, z);
			check(lit(server, 20, z) && heating.contains("stator phase A at") && heating.matches(".*\\b(59|60)\\.\\d Hz.*"),
					"three heaters on phase A should run and heat the genset's phase A winding, frequency in band: " + heating);
			String tripped = "";
			for (int waited = 0; waited < 1200; waited += 40) {
				tripped = read(server, 12, up, z);
				if (tripped.contains("TRIPPED")) {
					break;
				}
				context.waitTicks(40);
			}
			check(tripped.contains("TRIPPED on stator overcurrent (phase A") && !tripped.contains("on frequency"),
					"the genset should trip on stator overcurrent on phase A, not on frequency: " + tripped);
			context.takeScreenshot("wildcatter-stator-trip");
			// spread the same heaters over A, B and C, reset, restart, and it runs cool
			feeder.accept(1, "b");
			feeder.accept(2, "c");
			server.computeOnServer(srv -> {
				panel(srv, set).pushButton();   // resets the trip
				panel(srv, set).setWantRun(true);
				return null;
			});
			context.waitTicks(600);
			String spread = read(server, 12, up, z);
			check(spread.contains("RUNNING") && !spread.contains("TRIPPED") && lit(server, 20, z) && lit(server, 20, z + 3) && lit(server, 20, z - 3),
					"spread over A, B and C the same heaters should run without tripping: " + spread);
			System.out.println("[wildcatter-test] stator limit ok: " + heating + " | " + tripped + " | spread " + spread);
		} finally {
			dev.wildcatter.circuit.StatorHeating.testRatingScale = null;
		}
	}

	private double loadVolts(String readout) {
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\): ([0-9.]+) V,").matcher(readout);
		check(m.find(), "no volts in the readout: " + readout);
		return Double.parseDouble(m.group(1));
	}

	/** Phase A current in a cable's readout ("A 277 V 90.2 A, ..."). */
	private double cableAmps(String readout) {
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\bA [0-9]+ V ([0-9.]+) A").matcher(readout);
		check(m.find(), "no phase A current in the cable readout: " + readout);
		return Double.parseDouble(m.group(1));
	}

	private void sizeWires(TestServerContext server, int x0, int x1, int z, dev.wildcatter.energy.ConductorSize size) {
		server.computeOnServer(srv -> {
			for (int x = x0; x <= x1; x++) {
				for (int dz = 0; dz <= 1; dz++) {
					if (srv.overworld().getBlockEntity(new BlockPos(x, up, z + dz)) instanceof dev.wildcatter.block.WireBlockEntity wire) {
						wire.setSize(size);
					}
				}
			}
			return null;
		});
	}

	/**
	 * Conductor sizes: a 40 m run of 12 AWG drops more voltage than the same run in 6 AWG; a 6 AWG cable feeding five 15 kW heaters
	 * (90 A) burns out where 2 AWG holds; a 4/0 ACSR span may not be as long as a 1/0 one; a wire placed without a size takes its
	 * tier's standard one; and a broken wire drops with its size.
	 */
	private void conductorSizes(ClientGameTestContext context, TestServerContext server) {
		int z = -470;
		server.runCommand("tp @p 30 " + (up + 6) + " " + (z + 12) + " 180 20");
		context.waitTicks(20);
		server.runCommand("fill 6 " + (y - 1) + " " + (z - 30) + " 56 " + (y + 6) + " " + (z + 4) + " minecraft:air");
		server.runCommand("fill 6 " + (y - 1) + " " + (z - 30) + " 56 " + (y - 1) + " " + (z + 4) + " minecraft:stone");
		// 1. voltage drop: a heater 40 blocks from a 120 V turbine
		plant(server, z);
		for (int x = 12; x <= 51; x++) {
			put(server, x, up, z, "wildcatter:wool_wire_hot");
		}
		for (int x = 11; x <= 52; x++) {
			put(server, x, up, z + 1, "wildcatter:wool_wire_neutral");
		}
		put(server, 52, up, z, "wildcatter:electric_heater");
		context.waitTicks(160);
		String thin = read(server, 52, up, z);
		check(read(server, 30, up, z).contains("12 AWG"), "a wire placed without a size should be the standard 12 AWG: " + read(server, 30, up, z));
		sizeWires(server, 11, 52, z, dev.wildcatter.energy.ConductorSize.AWG_6);
		context.waitTicks(80);
		String thick = read(server, 52, up, z);
		check(loadVolts(thick) - loadVolts(thin) >= 3, "the same heater 40 m away should see more voltage on 6 AWG than on 12 AWG: 12 AWG " + thin + " | 6 AWG " + thick);
		context.takeScreenshot("wildcatter-wire-sizes");
		System.out.println("[wildcatter-test] voltage drop ok: 12 AWG " + thin + " | 6 AWG " + thick);
		server.runCommand("fill 6 " + (y - 1) + " " + z + " 56 " + (y + 1) + " " + (z + 1) + " minecraft:air");

		// 2. ampacity: 90 A through one block of cable, 2 AWG (115 A) holds and 6 AWG (65 A) burns
		int zc = z - 8;
		BlockPos set = new BlockPos(12, up, zc);
		genset(server, set, net.minecraft.core.Direction.NORTH, false);   // the skid runs west
		put(server, 13, up, zc, "wildcatter:cable_industrial");
		for (int x = 14; x <= 16; x++) {
			put(server, x, up, zc, "wildcatter:cable_industrial");
		}
		for (int[] at : new int[][] {{14, zc - 1}, {14, zc + 1}, {15, zc - 1}, {15, zc + 1}, {16, zc + 1}}) {
			put(server, at[0], up, at[1], "wildcatter:three_phase_heater");
		}
		server.computeOnServer(srv -> {
			for (int x = 13; x <= 16; x++) {
				((dev.wildcatter.block.MultiCableBlockEntity) srv.overworld().getBlockEntity(new BlockPos(x, up, zc)))
						.setSize(x == 13 ? dev.wildcatter.energy.ConductorSize.AWG_2 : dev.wildcatter.energy.ConductorSize.KCMIL_500);
			}
			return null;
		});
		startGenset(context, server, set, net.minecraft.core.Direction.NORTH, 1000);
		context.waitTicks(60);
		String holds = read(server, 13, up, zc);
		check(holds.contains("2 AWG") && !isAir(server, 13, up, zc) && cableAmps(holds) > 70,
				"2 AWG should carry the five heaters' 90 A without burning: " + holds);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.MultiCableBlockEntity) srv.overworld().getBlockEntity(new BlockPos(13, up, zc))).setSize(dev.wildcatter.energy.ConductorSize.AWG_6);
			return null;
		});
		String burning = "";
		for (int waited = 0; waited < 1200 && !isAir(server, 13, up, zc); waited += 20) {
			burning = read(server, 13, up, zc);
			context.waitTicks(20);
		}
		check(isAir(server, 13, up, zc) && burning.contains("HEATING"), "6 AWG carrying 90 A should heat and burn out: " + burning);
		System.out.println("[wildcatter-test] ampacity ok: " + holds + " | " + burning);

		// 3. spans: 40 m is fine for 1/0 ACSR on distribution poles, too long for 4/0
		int zs = z - 20;
		server.runCommand("fill 6 " + (y - 1) + " " + (zs - 4) + " 64 " + (y + 12) + " " + (zs + 1) + " minecraft:air");
		for (int k = 0; k < 2; k++) {
			for (int px : new int[] {20, 60}) {
				placeBig(server, WildcatterBlocks.WOOD_POLE_40, new BlockPos(px, up, zs - 3 * k), net.minecraft.core.Direction.NORTH);
				insulator(server, WildcatterBlocks.INSULATOR_DIST, new BlockPos(px, up + 10, zs - 3 * k), "a");
			}
		}
		context.waitTicks(10);
		dev.wildcatter.item.SpoolItem.Result raven = server.computeOnServer(srv -> dev.wildcatter.item.SpoolItem.connect(srv.overworld(),
				new BlockPos(20, up + 10, zs), new BlockPos(60, up + 10, zs), dev.wildcatter.energy.Voltage.DISTRIBUTION, false, 1024, dev.wildcatter.energy.ConductorSize.ACSR_RAVEN));
		dev.wildcatter.item.SpoolItem.Result penguin = server.computeOnServer(srv -> dev.wildcatter.item.SpoolItem.connect(srv.overworld(),
				new BlockPos(20, up + 10, zs - 3), new BlockPos(60, up + 10, zs - 3), dev.wildcatter.energy.Voltage.DISTRIBUTION, false, 1024, dev.wildcatter.energy.ConductorSize.ACSR_PENGUIN));
		check(raven.ok() && raven.message().contains("Raven"), "a 40 m 1/0 ACSR span should string: " + raven);
		check(!penguin.ok() && penguin.message().contains("Too far"), "a 40 m 4/0 ACSR span should be refused (heavier, 36 m at most): " + penguin);
		check(read(server, 60, up + 10, zs).contains("Raven"), "the insulator should name its line's conductor: " + read(server, 60, up + 10, zs));

		// 4. a broken wire drops with its size
		put(server, 40, up, zs, "wildcatter:industrial_wire_hot");
		server.computeOnServer(srv -> {
			BlockPos at = new BlockPos(40, up, zs);
			dev.wildcatter.block.WireBlockEntity wire = (dev.wildcatter.block.WireBlockEntity) srv.overworld().getBlockEntity(at);
			wire.setSize(dev.wildcatter.energy.ConductorSize.AWG_4_0);
				srv.overworld().destroyBlock(at, true);
			return null;
		});
		context.waitTicks(5);
		String dropped = server.computeOnServer(srv -> srv.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
				new net.minecraft.world.phys.AABB(new BlockPos(40, up, zs)).inflate(2)).stream()
				.map(e -> String.valueOf(e.getItem().get(dev.wildcatter.WildcatterComponents.CONDUCTOR_SIZE))).filter(v -> !v.equals("null")).findFirst()
				.orElseGet(() -> "none sized among " + srv.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
						new net.minecraft.world.phys.AABB(new BlockPos(40, up, zs)).inflate(2)).stream().map(e -> e.getItem() + " " + e.getItem().getComponentsPatch()).toList()));
		check(dropped.equals("AWG_4_0"), "a broken 4/0 wire should drop as 4/0: " + dropped);
		System.out.println("[wildcatter-test] conductor sizes ok: " + raven.message() + " | " + penguin.message() + " | dropped " + dropped);
		server.runCommand("kill @e[type=item]");
	}

	/**
	 * Gensets (docs/multiblocks/genset.md): a diesel set forms, cranks to rated speed, closes its breaker and carries a heater; the
	 * client sees its shaft turning; it stops when its tank runs dry; taking its radiator away dissolves it. A gas set forms too.
	 */
	private void gensets(ClientGameTestContext context, TestServerContext server) {
		int z = -520;
		server.runCommand("tp @p 10 " + (up + 1) + " " + (z + 4) + " 200 15");
		context.waitTicks(20);   // let the chunks load before building there
		server.runCommand("fill 4 " + (y - 1) + " " + (z - 10) + " 22 " + (y + 4) + " " + (z + 4) + " minecraft:air");
		server.runCommand("fill 4 " + (y - 1) + " " + (z - 10) + " 22 " + (y - 1) + " " + (z + 4) + " minecraft:stone");
		BlockPos set = new BlockPos(12, up, z);
		genset(server, set, net.minecraft.core.Direction.NORTH, false);   // tank 8-12 under radiator 8, engine 9-10, alternator 11, panel 12
		put(server, 13, up, z, "wildcatter:cable_industrial");
		put(server, 14, up, z, "wildcatter:three_phase_heater");
		BlockPos gasSet = new BlockPos(12, up, z - 6);
		genset(server, gasSet, net.minecraft.core.Direction.NORTH, true);
		context.waitTicks(30);
		String idle = read(server, 12, up, z);
		check(idle.contains("Diesel genset (500 kW") && idle.contains("STOPPED"), "a complete diesel skid should form a stopped genset: " + idle);
		check(read(server, 12, up, z - 6).contains("Gas genset (1000 kW"), "a gas skid should form a gas genset: " + read(server, 12, up, z - 6));
		startGenset(context, server, set, net.minecraft.core.Direction.NORTH, 1000);
		context.waitTicks(40);
		String running = read(server, 12, up, z);
		double rated = 120.0 * dev.wildcatter.energy.ElectricalStandard.active().hertz() / Tuning.GENSET_POLES;
		check(running.matches(".*RUNNING (1[45]\\d\\d|1[78]\\d\\d) rpm.*") && running.matches(".*, 1[45] kW.*"), "the set should run at rated speed carrying the 15 kW heater: " + running);
		check(read(server, 14, up, z).contains("running"), "the heater on the genset should run: " + read(server, 14, up, z));
		float clientRpm = context.computeOnClient(client -> client.level.getBlockEntity(new BlockPos(9, up, z)) instanceof dev.wildcatter.block.GensetPartBlockEntity engine
				? engine.spin().rpm() : -1F);
		check(Math.abs(clientRpm - rated) < rated * 0.05, "the client should see the engine turning at about " + rated + " rpm: " + clientRpm);
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-genset");
		// a dry tank stops it
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.GensetPartBlockEntity) srv.overworld().getBlockEntity(new BlockPos(8, y, z))).setFuel(0.01);
			return null;
		});
		context.waitTicks(60);
		String dry = read(server, 12, up, z);
		check(dry.contains("OUT OF FUEL") && !dry.contains("RUNNING"), "the set should stop when its tank runs dry: " + dry);
		check(!read(server, 14, up, z).contains("running"), "the heater should lose power with the set: " + read(server, 14, up, z));
		// a missing radiator: no set
		put(server, 8, up, z, "minecraft:air");
		context.waitTicks(30);
		String broken = read(server, 12, up, z);
		check(broken.contains("NOT BUILT") && broken.contains("radiator"), "taking the radiator away should dissolve the set: " + broken);
		System.out.println("[wildcatter-test] gensets ok: " + running + " | client " + clientRpm + " rpm | " + dry + " | " + broken);
	}

	private double kilowatts(String readout) {
		java.util.regex.Matcher m = java.util.regex.Pattern.compile(", ([0-9]+) kW \\(").matcher(readout);
		check(m.find(), "no kW in the genset readout: " + readout);
		return Double.parseDouble(m.group(1));
	}

	/**
	 * Synchronizing (4c): set A closes onto a dead bus and carries a heater; set B (AUTO) synchronizes onto the live bus and the two
	 * share the load. Then B in MANUAL is closed by hand 120 degrees out: the breaker trips with a bang and the alternator is wrecked.
	 * With a new alternator, a hand close inside the window holds.
	 */
	private void synchronizing(ClientGameTestContext context, TestServerContext server) {
		int z = -560;
		server.runCommand("tp @p 15 " + (up + 3) + " " + (z + 8) + " 180 20");
		context.waitTicks(20);   // let the chunks load before building there
		server.runCommand("fill 4 " + (y - 1) + " " + (z - 4) + " 26 " + (y + 4) + " " + (z + 4) + " minecraft:air");
		server.runCommand("fill 4 " + (y - 1) + " " + (z - 4) + " 26 " + (y - 1) + " " + (z + 4) + " minecraft:stone");
		BlockPos a = new BlockPos(12, up, z);
		BlockPos b = new BlockPos(17, up, z);
		genset(server, a, net.minecraft.core.Direction.NORTH, false);   // skid west of 12
		genset(server, b, net.minecraft.core.Direction.SOUTH, false);   // skid east of 17
		for (int x = 13; x <= 16; x++) {
			put(server, x, up, z, "wildcatter:cable_industrial");
		}
		put(server, 14, up, z + 1, "wildcatter:three_phase_heater");
		put(server, 15, up, z + 1, "wildcatter:three_phase_heater");
		startGenset(context, server, a, net.minecraft.core.Direction.NORTH, 1000);
		check(read(server, 12, up, z).contains("dead bus"), "set A should close onto the dead bus: " + read(server, 12, up, z));
		startGenset(context, server, b, net.minecraft.core.Direction.SOUTH, 1000);
		context.waitTicks(60);
		String ra = read(server, 12, up, z);
		String rb = read(server, 17, up, z);
		check(rb.contains("in sync"), "set B should synchronize onto the live bus: " + rb);
		check(kilowatts(ra) >= 5 && kilowatts(rb) >= 5, "the two sets should share the 30 kW of heaters: A " + ra + " | B " + rb);
		context.takeScreenshot("wildcatter-sync");
		System.out.println("[wildcatter-test] auto sync ok: A " + ra + " | B " + rb);
		// B to MANUAL, closed by hand 120 degrees out of step
		server.computeOnServer(srv -> {
			panel(srv, b).setWantRun(false);
			return null;
		});
		context.waitTicks(240);
		server.computeOnServer(srv -> {
			panel(srv, b).setSync(dev.wildcatter.circuit.Synchronizer.Mode.MANUAL);
			panel(srv, b).setWantRun(true);
			return null;
		});
		context.waitTicks(260);
		String open = read(server, 17, up, z);
		check(open.contains("RUNNING") && open.contains("open") && open.contains("synchroscope"), "in MANUAL the set should run with its breaker open and show the synchroscope: " + open);
		server.computeOnServer(srv -> {
			panel(srv, b).setSlipForTest(120);
			panel(srv, b).manualClose();
			return null;
		});
		context.waitTicks(20);
		String bang = read(server, 17, up, z);
		check(bang.contains("OUT-OF-SYNC") && bang.contains("DAMAGED"), "closing 120 degrees out should trip and wreck the alternator: " + bang);
		check(read(server, 18, up, z).contains("DAMAGED"), "the alternator should say it is damaged: " + read(server, 18, up, z));
		check(read(server, 12, up, z).contains("RUNNING"), "set A should ride through B's bad close: " + read(server, 12, up, z));
		// a new alternator, and a hand close inside the window
		put(server, 18, up, z, "minecraft:air");
		server.computeOnServer(srv -> {   // a new alternator, placed and grown into its cells as a player would
			BlockPos at = new BlockPos(18, up, z);
			net.minecraft.world.level.block.state.BlockState alt = dev.wildcatter.WildcatterBlocks.ALTERNATOR.defaultBlockState()
					.setValue(dev.wildcatter.block.GensetPartBlock.FACING, net.minecraft.core.Direction.SOUTH);
			srv.overworld().setBlock(at, alt, 3);
			dev.wildcatter.multiblock.BigParts.grow(srv.overworld(), at, alt);
			return null;
		});
		context.waitTicks(30);
		server.computeOnServer(srv -> {
			panel(srv, b).pushButton();   // resets the trip
			panel(srv, b).setWantRun(true);
			return null;
		});
		context.waitTicks(260);
		server.computeOnServer(srv -> {
			panel(srv, b).setSlipForTest(3);
			panel(srv, b).manualClose();
			return null;
		});
		context.waitTicks(20);
		String good = read(server, 17, up, z);
		check(good.contains("RUNNING") && good.contains("breaker CLOSED") && good.contains("by hand, in sync"), "a hand close inside the window should hold: " + good);
		System.out.println("[wildcatter-test] manual sync ok: " + bang + " | " + good);
	}

	private dev.wildcatter.block.TurbineGeneratorBlockEntity plantAt(net.minecraft.server.MinecraftServer srv, BlockPos at) {
		return (dev.wildcatter.block.TurbineGeneratorBlockEntity) srv.overworld().getBlockEntity(at);
	}

	/**
	 * A true-size pumpjack over a new oil wellhead (docs/multiblocks/well-site.md): its motor's terminal box at {@code box}, its front
	 * and its well {@code facing} (the box is wired on its back and both sides). Returns the wellhead.
	 */
	private BlockPos pumpjack(TestServerContext server, BlockPos box, net.minecraft.core.Direction facing) {
		BlockPos main = box.relative(facing, 5);
		BlockPos well = main.relative(facing);
		put(server, well.getX(), well.getY(), well.getZ(), "wildcatter:oil_wellhead[facing=" + facing.getSerializedName() + "]");
		placeBig(server, WildcatterBlocks.PUMPJACK, main, facing);
		return well;
	}

	/** The pumpjack any of whose cells is at {@code at}. */
	private dev.wildcatter.block.PumpjackBlockEntity jack(net.minecraft.server.MinecraftServer srv, BlockPos at) {
		return (dev.wildcatter.block.PumpjackBlockEntity) dev.wildcatter.multiblock.BigParts.entity(srv.overworld(), at);
	}

	/** Places a true-size part's main cell and grows it into its cells, as a player's placing would. */
	private void placeBig(TestServerContext server, net.minecraft.world.level.block.Block block, BlockPos main, net.minecraft.core.Direction facing) {
		server.computeOnServer(srv -> {
			net.minecraft.world.level.block.state.BlockState state = block.defaultBlockState()
					.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, facing);
			srv.overworld().setBlock(main, state, 3);
			dev.wildcatter.multiblock.BigParts.grow(srv.overworld(), main, state);
			return null;
		});
	}

	/**
	 * A pad-mount or unit substation in a line (docs/multiblocks/padmount.md), fed from {@code from} going {@code d}: on its main
	 * cell's terminal if {@code feedMain} (a pad-mount's low side, a unit substation's 4.16 kV side), else on the far end's. It fills
	 * the cells after {@code from} (2 for a pad-mount, 4 for a unit substation) and one row behind them; returns the cell its other
	 * terminal feeds.
	 */
	private BlockPos inlineTransformer(TestServerContext server, net.minecraft.world.level.block.Block block, BlockPos from, net.minecraft.core.Direction d, boolean feedMain) {
		int across = block == WildcatterBlocks.UNIT_SUBSTATION ? 3 : 1;
		BlockPos first = from.relative(d);
		net.minecraft.core.Direction facing = feedMain ? d.getCounterClockWise() : d.getClockWise();
		placeBig(server, block, feedMain ? first : first.relative(d, across), facing);
		return from.relative(d, across + 2);
	}

	/** The distribution scenes' step-up: 120 V wool wire at x 12, a 75 kVA pad-mount at x 13-14 run backward, 12.47 kV out at x 15. */
	private void stepUp(TestServerContext server, int z) {
		put(server, 12, up, z, "wildcatter:wool_wire_hot");
		inlineTransformer(server, WildcatterBlocks.PADMOUNT_208, new BlockPos(12, up, z), net.minecraft.core.Direction.EAST, true);
	}

	/** An insulator put up as a lineman would: mounted by what holds it (an arm, a pole top, a crossbeam), on one conductor. */
	private void insulator(TestServerContext server, net.minecraft.world.level.block.Block block, BlockPos at, String conductor) {
		String why = server.computeOnServer(srv -> {
			dev.wildcatter.block.InsulatorBlock insulator = (dev.wildcatter.block.InsulatorBlock) block;
			dev.wildcatter.block.InsulatorBlock.Mount mount = insulator.mountAt(srv.overworld(), at);
			if (mount == null) {
				return "nothing holds an insulator at " + at.toShortString();
			}
			srv.overworld().setBlock(at, insulator.defaultBlockState().setValue(dev.wildcatter.block.InsulatorBlock.MOUNT, mount)
					.setValue(dev.wildcatter.block.CableBlock.CONDUCTOR, dev.wildcatter.circuit.Conductor.valueOf(conductor.toUpperCase())), 3);
			return "";
		});
		check(why.isEmpty(), why);
	}

	/** A crossarm on the pole whose top cell is {@code top}, square to a line running along x: its cells north and south of the pole. */
	private void crossarm(TestServerContext server, BlockPos top) {
		placeBig(server, WildcatterBlocks.CROSSARM_DIST, top.north(), net.minecraft.core.Direction.EAST);
	}

	/**
	 * A 138 kV H-frame across a line running along x (docs/multiblocks/poles.md): 75 ft poles at (px, z-2) and (px, z+2), the
	 * crossbeam across their tops, the X-brace, and the three strings under the beam: B at z-4, A at z and C at z+4, their clamps
	 * 18 m up.
	 */
	private void hFrame(TestServerContext server, int px, int z) {
		placeBig(server, WildcatterBlocks.WOOD_POLE_75, new BlockPos(px, up, z - 2), net.minecraft.core.Direction.NORTH);
		placeBig(server, WildcatterBlocks.WOOD_POLE_75, new BlockPos(px, up, z + 2), net.minecraft.core.Direction.NORTH);
		placeBig(server, WildcatterBlocks.CROSSBEAM_138, new BlockPos(px, up + 19, z), net.minecraft.core.Direction.EAST);
		placeBig(server, WildcatterBlocks.X_BRACE_138, new BlockPos(px, up + 15, z), net.minecraft.core.Direction.EAST);
		insulator(server, WildcatterBlocks.INSULATOR_HV, new BlockPos(px, up + 18, z - 4), "b");
		insulator(server, WildcatterBlocks.INSULATOR_HV, new BlockPos(px, up + 18, z), "a");
		insulator(server, WildcatterBlocks.INSULATOR_HV, new BlockPos(px, up + 18, z + 4), "c");
	}

	/**
	 * The 1 MW steam plant at true size (4.5c, docs/multiblocks/steam-plant.md): burner, boiler, superheater, generator, turbine,
	 * feed pump and condenser form a plant, 6 x 7 m. Lit, the drum rises toward pressure. A drum too low to run waits for its
	 * feed pump, which needs 480 V: a genset black-starts it. At pressure the turbine rolls up, the generator closes onto the dead
	 * bus, and through the GSU on the generator's end (4.16 kV delta to 12.47 kV wye, 30 degrees) and a pad-mount it runs a
	 * heater. A true-size cooling tower cell on the condenser's water, its 7.5 kW fan fed from the genset, cools it; without one
	 * the plant is capped at three quarters of rating. Taking the superheater out dissolves the plant.
	 */
	private void steamPlant(ClientGameTestContext context, TestServerContext server) {
		int z = -600;
		server.runCommand("tp @p 12 " + (up + 6) + " " + (z + 14) + " 180 25");
		context.waitTicks(20);   // let the chunks load before building there
		server.runCommand("fill 0 " + (y - 1) + " " + (z - 6) + " 24 " + (y + 8) + " " + (z + 9) + " minecraft:air");
		server.runCommand("fill 0 " + (y - 1) + " " + (z - 6) + " 24 " + y + " " + (z + 9) + " minecraft:stone");   // ground at y: the plant stands on it
		// the plant round its generator at (10, z): burner x 9, boiler x 10-13 and superheater x 14 at z-3 to z-1; generator x 10-12
		// and turbine x 13-14 at z to z+1; feed pump x 10-11 at z+2 and condenser x 12-14 at z+2 to z+3
		BlockPos gen = new BlockPos(10, up, z);
		java.util.List<BlockPos> overwrote = server.computeOnServer(srv -> dev.wildcatter.block.SteamPlantLayout.layout().build(srv.overworld(), gen, net.minecraft.core.Direction.NORTH, 0));
		check(overwrote.isEmpty(), "the plant should be built on clear ground: " + overwrote);
		// the GSU (true size) against the generator's west end, its bushings at the west, 12.47 kV cable down to a pad-mount and a heater
		placeBig(server, WildcatterBlocks.GSU_TRANSFORMER, new BlockPos(9, up, z + 1), net.minecraft.core.Direction.WEST);
		put(server, 7, up + 2, z + 1, "wildcatter:cable_dist");
		put(server, 7, up + 1, z + 1, "wildcatter:cable_dist");
		put(server, 7, up, z + 1, "wildcatter:cable_dist");
		inlineTransformer(server, WildcatterBlocks.PADMOUNT_480, new BlockPos(7, up, z + 1), net.minecraft.core.Direction.WEST, false);   // x 5-6, 480 V out at x 4
		put(server, 4, up, z + 1, "wildcatter:cable_industrial");
		put(server, 3, up, z + 1, "wildcatter:three_phase_heater");
		context.waitTicks(30);
		check(read(server, 10, up, z).contains("COLD"), "the parts should form a cold plant: " + read(server, 10, up, z));
		// light it with coal on the burner; the drum starts at 48 % water (below the half it needs to run) and a full hotwell
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.SteamPlantPartBlockEntity) srv.overworld().getBlockEntity(new BlockPos(9, up, z - 3))).insertFuel(new ItemStack(net.minecraft.world.item.Items.COAL, 4));
			plantAt(srv, gen).setWaterForTest(Tuning.DRUM_WATER_KG * 0.48, Tuning.HOTWELL_KG);
			plantAt(srv, gen).setWantRun(true);
			return null;
		});
		context.waitTicks(100);
		String firing = read(server, 10, up, z);
		check(firing.contains("FIRING") && !firing.contains("at 0% pressure"), "lit, the drum should start to rise: " + firing);
		// skip the five-minute warm-up: at pressure, but too little water and no power for the feed pump, it cannot roll
		server.computeOnServer(srv -> {
			plantAt(srv, gen).setHeatForTest(0.99);
			return null;
		});
		context.waitTicks(100);
		String dry = read(server, 10, up, z);
		check(dry.contains("FIRING") && read(server, 10, up, z + 2).contains("NO POWER"), "with the drum short and no station service the plant should wait: " + dry + " | " + read(server, 10, up, z + 2));
		// a genset black-starts the feed pump: 480 V cable from its panel's end to the terminal box on the pump motor's south side
		BlockPos set = new BlockPos(8, up + 1, z + 6);
		genset(server, set, net.minecraft.core.Direction.NORTH, false);   // its base on the ground; the skid runs west, x 4-8, z+6 to z+7
		put(server, 9, up + 1, z + 6, "wildcatter:cable_industrial");
		put(server, 10, up + 1, z + 6, "wildcatter:cable_industrial");
		put(server, 10, up, z + 6, "wildcatter:cable_industrial");
		put(server, 10, up, z + 5, "wildcatter:cable_industrial");
		put(server, 10, up, z + 4, "wildcatter:cable_industrial");
		put(server, 10, up, z + 3, "wildcatter:cable_industrial");
		startGenset(context, server, set, net.minecraft.core.Direction.NORTH, 1000);
		String rolling = "";
		for (int waited = 0; waited < 1600; waited += 40) {
			rolling = read(server, 10, up, z);
			if (rolling.contains("breaker CLOSED")) {
				break;
			}
			context.waitTicks(40);
		}
		check(rolling.contains("RUNNING") && rolling.contains("breaker CLOSED"), "fed and at pressure, the turbine should roll up and close onto the dead bus: " + rolling + " | pump " + read(server, 10, up, z + 2));
		context.waitTicks(60);
		String running = read(server, 10, up, z);
		String gsu = read(server, 9, up, z + 1);
		String heater = read(server, 3, up, z + 1);
		check(running.matches(".*RUNNING (3[45]\\d\\d|29\\d\\d|30\\d\\d) rpm.*"), "the turbine should run at rated speed: " + running);
		check(gsu.contains("line side +30 degrees") && heater.contains("running"), "through the GSU (30 degrees) and a pad-mount the heater should run: " + gsu + " | " + heater);
		check(running.contains("NOT COOLED"), "with no cooling tower the plant should say so: " + running);
		server.runCommand("tp @p 3 " + (up + 4) + " " + (z - 7) + " -45 22");
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-steam-plant");
		// a cooling tower cell east of the condenser, its basin on the condenser's water pipe; its fan fed from the genset's cable,
		// along z+4 and up to the disconnect at the fan deck's front
		BlockPos deck = new BlockPos(16, up + 4, z + 2);
		java.util.List<BlockPos> towerOver = server.computeOnServer(srv -> dev.wildcatter.block.TowerColumn.layout().build(srv.overworld(), deck, net.minecraft.core.Direction.NORTH, 0));
		check(towerOver.isEmpty(), "the tower should stand on clear ground: " + towerOver);
		put(server, 15, up, z + 2, "wildcatter:pipe");
		for (int x = 11; x <= 15; x++) {
			put(server, x, up, z + 4, "wildcatter:cable_industrial");
		}
		put(server, 15, up + 1, z + 4, "wildcatter:cable_industrial");
		for (int zz = z + 3; zz >= z + 1; zz--) {
			put(server, 15, up + 1, zz, "wildcatter:cable_industrial");
		}
		for (int h = up + 1; h <= up + 4; h++) {
			put(server, 16, h, z + 1, "wildcatter:cable_industrial");
		}
		context.waitTicks(200);
		String tower = read(server, 16, up + 4, z + 2);
		String cooled = read(server, 10, up, z);
		check(tower.contains("COOLING"), "a built tower with its fan powered should be cooling: " + tower);
		check(cooled.contains("condensate COOLED"), "with a cooling tower on the condenser's water the plant should be cooled: " + cooled + " | tower " + tower);
		server.runCommand("tp @p 22 " + (up + 6) + " " + (z + 12) + " 150 25");
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-steam-plant-cooled");
		server.runCommand("tp @p 23 " + (up + 9) + " " + (z - 4) + " 60 40");
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-cooling-tower");
		server.runCommand("tp @p 12 " + (up + 14) + " " + (z + 1) + " 90 80");
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-steam-plant-top");
		// take the superheater out: no plant
		put(server, 14, up, z - 3, "minecraft:air");
		context.waitTicks(30);
		String broken = read(server, 10, up, z);
		check(broken.contains("NOT BUILT") && broken.contains("superheater"), "without its superheater the plant should dissolve: " + broken);
		System.out.println("[wildcatter-test] steam plant ok: " + firing + " | " + running + " | " + gsu + " | " + tower + " | " + cooled + " | " + broken);
	}

	// ---- Phase 4.5: true-size parts ---------------------------------------------------------------------------------
	/** How many cells round {@code main} hold a GSU, and how many GSU items lie on the ground near it. */
	private int[] gsuCells(TestServerContext server, BlockPos main) {
		return server.computeOnServer(srv -> {
			int cells = 0;
			for (BlockPos at : BlockPos.betweenClosed(main.offset(-2, 0, -2), main.offset(2, 3, 2))) {
				if (srv.overworld().getBlockState(at).is(dev.wildcatter.WildcatterBlocks.GSU_TRANSFORMER)) {
					cells++;
				}
			}
			int items = 0;
			for (net.minecraft.world.entity.item.ItemEntity item : srv.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
					new net.minecraft.world.phys.AABB(main).inflate(4))) {
				if (item.getItem().is(dev.wildcatter.WildcatterBlocks.GSU_TRANSFORMER.asItem())) {
					items += item.getItem().getCount();
				}
			}
			return new int[] {cells, items};
		});
	}

	/** Places a GSU the way a player does: standing south of {@code main}, looking north, clicking the ground under it. */
	private boolean placeGsu(TestServerContext server, BlockPos main) {
		return server.computeOnServer(srv -> {
			net.minecraft.server.level.ServerPlayer player = srv.getPlayerList().getPlayers().get(0);
			player.snapTo(main.getX() + 0.5, main.getY(), main.getZ() + 4.5, 180.0F, 30.0F);
			ItemStack stack = new ItemStack(dev.wildcatter.WildcatterBlocks.GSU_TRANSFORMER);
			net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
					net.minecraft.world.phys.Vec3.atCenterOf(main.below()).add(0, 0.5, 0), net.minecraft.core.Direction.UP, main.below(), false);
			net.minecraft.world.InteractionResult result = ((net.minecraft.world.item.BlockItem) stack.getItem())
					.place(new net.minecraft.world.item.context.BlockPlaceContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, stack, hit));
			return result.consumesAction();
		});
	}

	/**
	 * A true-size part (docs/multiblocks/README.md, "Big parts"), on the GSU: placed like a player it fills its 3 x 2 x 3 m; a
	 * click on any piece reads the transformer; breaking one piece takes the whole part and drops one item; it is refused where
	 * a cell is blocked; and a lone main cell with no room (an older world's one-block GSU) drops as an item.
	 */
	private void bigParts(ClientGameTestContext context, TestServerContext server) {
		int z = -660;
		server.runCommand("tp @p 10 " + (up + 3) + " " + (z + 6) + " 180 20");
		context.waitTicks(20);
		server.runCommand("fill 4 " + (y - 1) + " " + (z - 8) + " 16 " + (y + 6) + " " + (z + 6) + " minecraft:air");
		server.runCommand("fill 4 " + (y - 1) + " " + (z - 8) + " 16 " + y + " " + (z + 6) + " minecraft:stone");   // ground at y, so the click lands on it
		BlockPos main = new BlockPos(10, up, z);
		// holding one, the ghost outline shows where it would go
		server.runCommand("item replace entity @p weapon.mainhand with wildcatter:gsu_transformer");
		server.runCommand("tp @p 10.5 " + up + " " + (z + 4.5) + " 180 40");
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-gsu-ghost");
		server.runCommand("item replace entity @p weapon.mainhand with minecraft:air");
		check(placeGsu(server, main), "a GSU should place on clear ground");
		context.waitTicks(5);
		int[] placed = gsuCells(server, main);
		check(placed[0] == 18, "placed, the GSU should fill its 3 x 2 x 3 cells: " + placed[0]);
		String viaPiece = read(server, 9, up + 2, z - 1);   // a top corner of the front row
		check(viaPiece.contains("generator step-up"), "a click on any piece should read the transformer: " + viaPiece);
		boolean bushingsJoin = server.computeOnServer(srv -> dev.wildcatter.circuit.CircuitPart.portAt(srv.overworld(), new BlockPos(10, up + 2, z - 1),
				net.minecraft.core.Direction.UP, dev.wildcatter.circuit.Conductor.A, dev.wildcatter.energy.Voltage.DISTRIBUTION));
		check(bushingsJoin, "the 12.47 kV terminals should be on top of the bushings");
		// screenshots from three sides
		server.runCommand("tp @p 7 " + (up + 1) + " " + (z - 4) + " -35 15");
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-gsu-front");
		server.runCommand("tp @p 14 " + (up + 2) + " " + (z + 4) + " 135 20");
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-gsu-back");
		server.runCommand("tp @p 13 " + (up + 5) + " " + (z - 3) + " 45 50");
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-gsu-top");
		// break one piece: the whole part goes and one GSU drops
		server.runCommand("kill @e[type=item]");
		server.computeOnServer(srv -> srv.overworld().destroyBlock(new BlockPos(11, up + 1, z - 1), true));
		context.waitTicks(10);
		int[] broken = gsuCells(server, main);
		check(broken[0] == 0 && broken[1] == 1, "breaking one piece should take the whole GSU and drop one item: " + broken[0] + " cells, " + broken[1] + " items");
		// a blocked cell refuses the placement
		server.runCommand("kill @e[type=item]");
		put(server, 11, up + 1, z - 1, "minecraft:stone");
		check(!placeGsu(server, main), "a GSU should be refused where a cell is blocked");
		context.waitTicks(5);
		check(gsuCells(server, main)[0] == 0, "a refused GSU should leave nothing behind");
		// a lone main cell with no room (as an older world's one-block GSU would load) drops as an item
		put(server, 10, up, z, "wildcatter:gsu_transformer[facing=north]");
		context.waitTicks(10);
		int[] lone = gsuCells(server, main);
		check(lone[0] == 0 && lone[1] == 1, "a lone GSU with no room should drop as one item: " + lone[0] + " cells, " + lone[1] + " items");
		// and with room it grows into its cells
		server.runCommand("kill @e[type=item]");
		put(server, 11, up + 1, z - 1, "minecraft:air");
		put(server, 10, up, z, "wildcatter:gsu_transformer[facing=north]");
		context.waitTicks(10);
		check(gsuCells(server, main)[0] == 18, "a lone GSU with room should grow to its full size");
		System.out.println("[wildcatter-test] big parts ok: GSU places, reads through a piece, breaks whole, refuses a blocked cell, migrates");
	}

	/** Places a true-size part the way a player does: standing at {@code (px, pz)} looking {@code yaw}, clicking the top of {@code clicked}. */
	private boolean placeOn(TestServerContext server, net.minecraft.world.level.block.Block block, BlockPos clicked, double px, double pz, float yaw) {
		return placeAt(server, block, clicked, net.minecraft.core.Direction.UP, px, pz, yaw);
	}

	/** The same, clicking {@code face} of {@code clicked} (the side of a pole, say). */
	private boolean placeAt(TestServerContext server, net.minecraft.world.level.block.Block block, BlockPos clicked, net.minecraft.core.Direction face, double px, double pz, float yaw) {
		return server.computeOnServer(srv -> {
			net.minecraft.server.level.ServerPlayer player = srv.getPlayerList().getPlayers().get(0);
			player.snapTo(px, clicked.getY() + 1, pz, yaw, 30.0F);
			ItemStack stack = new ItemStack(block);
			net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
					net.minecraft.world.phys.Vec3.atCenterOf(clicked).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5), face, clicked, false);
			return ((net.minecraft.world.item.BlockItem) stack.getItem())
					.place(new net.minecraft.world.item.context.BlockPlaceContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, stack, hit)).consumesAction();
		});
	}

	/** Items of one kind lying near {@code at}. */
	private int itemsNear(TestServerContext server, BlockPos at, net.minecraft.world.level.ItemLike block) {
		return server.computeOnServer(srv -> {
			int n = 0;
			for (net.minecraft.world.entity.item.ItemEntity item : srv.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
					new net.minecraft.world.phys.AABB(at).inflate(8))) {
				if (item.getItem().is(block.asItem())) {
					n += item.getItem().getCount();
				}
			}
			return n;
		});
	}

	/** The state at a position, for checks. */
	private net.minecraft.world.level.block.state.BlockState stateAt(TestServerContext server, BlockPos at) {
		return server.computeOnServer(srv -> srv.overworld().getBlockState(at));
	}

	/**
	 * True-size gensets (4.5b, docs/multiblocks/genset.md), built the way a player builds them: the sub-base tank first, then
	 * every part clicked anywhere on top of it goes to its own slot and turns to the tank's way, the engine included. The set
	 * forms; a diesel bucket on the tank's far corner fills it; an alternator turned the wrong way is named in the readout;
	 * both sets run (screenshots); breaking one engine piece drops one engine and dissolves the set; and a one-block radiator
	 * from an older world drops as an item.
	 */
	private void gensetParts(ClientGameTestContext context, TestServerContext server) {
		int z = -720;
		server.runCommand("tp @p 10 " + (up + 3) + " " + (z + 8) + " 180 20");
		context.waitTicks(20);
		server.runCommand("fill 0 " + (y - 1) + " " + (z - 14) + " 24 " + (y + 8) + " " + (z + 10) + " minecraft:air");
		server.runCommand("fill 0 " + (y - 1) + " " + (z - 14) + " 24 " + y + " " + (z + 10) + " minecraft:stone");   // ground at y
		server.runCommand("kill @e[type=item]");
		// the tank, placed by a player standing south and looking north: it faces the player
		BlockPos ground = new BlockPos(12, y, z);
		check(placeOn(server, WildcatterBlocks.FUEL_TANK, ground, 12.5, z + 4.5, 180.0F), "a sub-base tank should place on clear ground");
		BlockPos base = new BlockPos(12, up, z);
		net.minecraft.world.level.block.state.BlockState tank = stateAt(server, base);
		check(tank.is(WildcatterBlocks.FUEL_TANK), "the tank's main cell should be where it was clicked: " + tank);
		net.minecraft.core.Direction facing = tank.getValue(dev.wildcatter.block.GensetPartBlock.FACING);
		// every part clicked on the tank's own main cell, in no particular order: each goes to its slot
		net.minecraft.world.level.block.Block[] parts = {WildcatterBlocks.DIESEL_ENGINE_PART, WildcatterBlocks.RADIATOR, WildcatterBlocks.ALTERNATOR,
				WildcatterBlocks.GENSET_CONTROL_PANEL, WildcatterBlocks.EXHAUST_STACK};
		for (net.minecraft.world.level.block.Block part : parts) {
			check(placeOn(server, part, base, 12.5, z + 4.5, 180.0F), "a " + part.getName().getString() + " clicked on the tank should go to its slot");
		}
		int turns = dev.wildcatter.multiblock.Multiblock.turns(facing);
		java.util.function.Function<int[], BlockPos> slot = d -> {
			int dx = d[0], dz = d[2];
			for (int i = 0; i < turns; i++) {
				int t = dx;
				dx = -dz;
				dz = t;
			}
			return base.offset(dx, d[1], dz);
		};
		BlockPos engine = slot.apply(new int[] {1, 1, 0});
		BlockPos alternator = slot.apply(new int[] {3, 1, 0});
		BlockPos panelAt = slot.apply(new int[] {4, 1, 0});
		check(stateAt(server, engine).is(WildcatterBlocks.DIESEL_ENGINE_PART) && stateAt(server, engine).getValue(dev.wildcatter.block.GensetPartBlock.FACING) == facing,
				"the engine, clicked off its slot, should land in it facing the tank's way: " + stateAt(server, engine));
		context.waitTicks(30);
		String formed = read(server, panelAt.getX(), panelAt.getY(), panelAt.getZ());
		check(formed.contains("Diesel genset (500 kW") && formed.contains("STOPPED"), "a skid built by hand should form a stopped diesel genset: " + formed);
		// a diesel bucket on the tank's far corner fills it
		BlockPos far = slot.apply(new int[] {4, 0, 1});
		server.computeOnServer(srv -> {
			net.minecraft.server.level.ServerPlayer player = srv.getPlayerList().getPlayers().get(0);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(WildcatterItems.DIESEL_BUCKET));
			net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(far), facing, far, false);
			srv.overworld().getBlockState(far).useItemOn(player.getMainHandItem(), srv.overworld(), player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			return null;
		});
		String fuel = read(server, base.getX(), base.getY(), base.getZ());
		check(fuel.contains("1000 of 2000 L"), "a bucket on any cell of the tank should fill it: " + fuel);
		// an alternator turned the wrong way (facing the set's front, so it stands out in front) is named
		server.computeOnServer(srv -> {
			srv.overworld().setBlock(alternator, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
			net.minecraft.world.level.block.state.BlockState turned = WildcatterBlocks.ALTERNATOR.defaultBlockState()
					.setValue(dev.wildcatter.block.GensetPartBlock.FACING, facing.getOpposite());
			srv.overworld().setBlock(alternator, turned, 3);
			dev.wildcatter.multiblock.BigParts.grow(srv.overworld(), alternator, turned);
			return null;
		});
		context.waitTicks(30);
		String turned = read(server, panelAt.getX(), panelAt.getY(), panelAt.getZ());
		check(turned.contains("NOT BUILT") && turned.contains("alternator") && turned.contains("face the machine's way"), "a part facing the wrong way should be named: " + turned);
		server.computeOnServer(srv -> srv.overworld().setBlock(alternator, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3));
		context.waitTicks(2);
		check(placeOn(server, WildcatterBlocks.ALTERNATOR, base, 12.5, z + 4.5, 180.0F), "a new alternator clicked on the tank should go to its slot");
		context.waitTicks(30);
		check(read(server, panelAt.getX(), panelAt.getY(), panelAt.getZ()).contains("STOPPED"), "with the alternator back the set should form again: " + read(server, panelAt.getX(), panelAt.getY(), panelAt.getZ()));
		// a gas set beside it, with gas already in its line, and both run
		BlockPos gasPanel = new BlockPos(14, up + 1, z - 7);
		genset(server, gasPanel, net.minecraft.core.Direction.NORTH, true);
		context.waitTicks(30);
		server.computeOnServer(srv -> {
			panel(srv, gasPanel).setGasForTest(50);
			panel(srv, gasPanel).setWantRun(true);
			panel(srv, panelAt).setWantRun(true);
			return null;
		});
		context.waitTicks(260);
		String diesel = read(server, panelAt.getX(), panelAt.getY(), panelAt.getZ());
		String gas = read(server, gasPanel.getX(), gasPanel.getY(), gasPanel.getZ());
		check(diesel.contains("RUNNING") && gas.contains("RUNNING"), "both sets should run: " + diesel + " | " + gas);
		server.runCommand("tp @p 5 " + (up + 2) + " " + (z + 4) + " -140 15");
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-gensets-front");
		server.runCommand("tp @p 19 " + (up + 2) + " " + (z - 11) + " 40 12");
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-gensets-back");
		server.runCommand("tp @p 18 " + (up + 9) + " " + (z + 3) + " 150 50");
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-gensets-top");
		// break one piece of the engine: one engine drops and the set dissolves
		server.runCommand("kill @e[type=item]");
		BlockPos enginePiece = slot.apply(new int[] {2, 2, 1});
		check(stateAt(server, enginePiece).is(WildcatterBlocks.DIESEL_ENGINE_PART), "the engine should fill its far top cell: " + stateAt(server, enginePiece));
		server.computeOnServer(srv -> srv.overworld().destroyBlock(enginePiece, true));
		context.waitTicks(40);
		int engines = itemsNear(server, engine, WildcatterBlocks.DIESEL_ENGINE_PART);
		String gone = read(server, panelAt.getX(), panelAt.getY(), panelAt.getZ());
		check(engines == 1 && stateAt(server, engine).isAir(), "breaking one engine piece should take the whole engine and drop one: " + engines + " items");
		check(gone.contains("NOT BUILT") && gone.contains("diesel engine"), "without its engine the set should dissolve: " + gone);
		// an older world's one-block radiator: the layout changed, so it drops rather than grow
		server.runCommand("kill @e[type=item]");
		put(server, 4, up, z + 6, "wildcatter:radiator[facing=north]");
		context.waitTicks(10);
		check(stateAt(server, new BlockPos(4, up, z + 6)).isAir() && itemsNear(server, new BlockPos(4, up, z + 6), WildcatterBlocks.RADIATOR) == 1,
				"a one-block radiator from an older world should drop as one item");
		System.out.println("[wildcatter-test] genset parts ok: " + formed + " | " + fuel + " | " + turned + " | " + diesel + " | " + gas + " | " + gone);
	}

	/** The frame-rate check from the Phase 4.5 plan: ten running diesel sets in view, and the client's frames per second logged. */
	private void gensetFrameRate(ClientGameTestContext context, TestServerContext server) {
		int z = -780;
		server.runCommand("tp @p 10 " + (up + 6) + " " + (z + 14) + " 180 30");
		context.waitTicks(20);
		server.runCommand("fill 0 " + (y - 1) + " " + (z - 4) + " 20 " + (y + 6) + " " + (z + 32) + " minecraft:air");
		server.runCommand("fill 0 " + (y - 1) + " " + (z - 4) + " 20 " + (y - 1) + " " + (z + 32) + " minecraft:stone");
		context.waitTicks(40);
		int before = context.computeOnClient(client -> client.getFps());
		java.util.List<BlockPos> sets = new java.util.ArrayList<>();
		for (int k = 0; k < 10; k++) {
			BlockPos set = new BlockPos(k % 2 == 0 ? 8 : 16, up, z + 3 * (k / 2));
			sets.add(set);
			genset(server, set, net.minecraft.core.Direction.NORTH, false);
		}
		context.waitTicks(30);
		server.computeOnServer(srv -> {
			for (BlockPos set : sets) {
				((dev.wildcatter.block.GensetPartBlockEntity) srv.overworld().getBlockEntity(dev.wildcatter.block.GensetSkid.base(set, net.minecraft.core.Direction.NORTH, false))).setFuel(1000);
				panel(srv, set).setWantRun(true);
			}
			return null;
		});
		context.waitTicks(300);
		int running = 0;
		for (BlockPos set : sets) {
			if (read(server, set.getX(), set.getY(), set.getZ()).contains("RUNNING")) {
				running++;
			}
		}
		int after = context.computeOnClient(client -> client.getFps());
		context.takeScreenshot("wildcatter-gensets-ten");
		check(running == 10, "all ten sets should run: " + running);
		System.out.println("[wildcatter-test] frame rate: " + before + " fps empty, " + after + " fps with ten running gensets in view");
	}

	/**
	 * The cooling tower cell at true size (4.5c, docs/multiblocks/cooling-tower.md), built the way a player builds it: the basin
	 * first, then fill, drift eliminators and fan deck, each clicked on top of the basin, snap into the column. Without its
	 * eliminators the deck names them; breaking one piece of the fill drops one fill; and a one-block plant part from an older
	 * world drops as an item.
	 */
	private void towerParts(ClientGameTestContext context, TestServerContext server) {
		int z = -820;
		server.runCommand("tp @p 10 " + (up + 6) + " " + (z + 12) + " 180 25");
		context.waitTicks(20);
		server.runCommand("fill 0 " + (y - 1) + " " + (z - 8) + " 20 " + (y + 9) + " " + (z + 10) + " minecraft:air");
		server.runCommand("fill 0 " + (y - 1) + " " + (z - 8) + " 20 " + y + " " + (z + 10) + " minecraft:stone");   // ground at y
		server.runCommand("kill @e[type=item]");
		BlockPos basin = new BlockPos(10, up, z);
		check(placeOn(server, WildcatterBlocks.TOWER_BASIN, basin.below(), 10.5, z + 6.5, 180.0F), "a basin should place on clear ground");
		check(stateAt(server, basin).is(WildcatterBlocks.TOWER_BASIN), "the basin's main cell should be where it was clicked: " + stateAt(server, basin));
		for (net.minecraft.world.level.block.Block part : new net.minecraft.world.level.block.Block[] {WildcatterBlocks.COOLING_FILL, WildcatterBlocks.DRIFT_ELIMINATOR, WildcatterBlocks.COOLING_TOWER}) {
			check(placeOn(server, part, basin, 10.5, z + 6.5, 180.0F), "a " + part.getName().getString() + " clicked on the basin should go into the column");
		}
		BlockPos deck = basin.above(dev.wildcatter.block.TowerColumn.DECK_HEIGHT);
		check(stateAt(server, basin.above(1)).is(WildcatterBlocks.COOLING_FILL) && stateAt(server, basin.above(3)).is(WildcatterBlocks.DRIFT_ELIMINATOR)
				&& stateAt(server, deck).is(WildcatterBlocks.COOLING_TOWER), "fill, eliminators and deck should stack on the basin: " + stateAt(server, deck));
		context.waitTicks(40);
		String built = read(server, deck.getX(), deck.getY(), deck.getZ());
		check(built.contains("Cooling tower (7.5 kW") && !built.contains("NOT BUILT"), "a full column should be a built tower, waiting for power: " + built);
		String viaDeck = read(server, 8, up + 5, z - 2);   // a far top corner of the deck reads the tower
		check(viaDeck.contains("Cooling tower"), "any piece of the deck should read the tower: " + viaDeck);
		server.runCommand("tp @p 4 " + (up + 5) + " " + (z + 6) + " -140 25");
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-tower-built");
		// take the eliminators out: the deck names them
		server.computeOnServer(srv -> srv.overworld().destroyBlock(basin.above(3), false));
		context.waitTicks(40);
		String missing = read(server, deck.getX(), deck.getY(), deck.getZ());
		check(missing.contains("NOT BUILT") && missing.contains("drift eliminators"), "without its eliminators the tower should say so: " + missing);
		// break one piece of the fill: the whole fill goes and one drops
		server.runCommand("kill @e[type=item]");
		BlockPos fillPiece = server.computeOnServer(srv -> {
			for (BlockPos at : dev.wildcatter.multiblock.BigParts.cellsAt(srv.overworld(), basin.above(1))) {
				if (!at.equals(basin.above(1))) {
					return at.immutable();
				}
			}
			return basin.above(1);
		});
		server.computeOnServer(srv -> srv.overworld().destroyBlock(fillPiece, true));
		context.waitTicks(40);   // the pieces go at once; the main cell breaks and drops on its next tick
		check(stateAt(server, basin.above(1)).isAir() && itemsNear(server, basin, WildcatterBlocks.COOLING_FILL) == 1, "breaking one fill piece should take the whole fill and drop one: "
				+ fillPiece.toShortString() + " -> main " + stateAt(server, basin.above(1)) + ", " + itemsNear(server, basin, WildcatterBlocks.COOLING_FILL) + " fill items");
		// an older world's one-block condenser: the plant's layout changed, so it drops
		server.runCommand("kill @e[type=item]");
		put(server, 16, up, z + 6, "wildcatter:condenser[facing=north]");
		context.waitTicks(10);
		check(stateAt(server, new BlockPos(16, up, z + 6)).isAir() && itemsNear(server, new BlockPos(16, up, z + 6), WildcatterBlocks.CONDENSER) == 1,
				"a one-block condenser from an older world should drop as one item");
		System.out.println("[wildcatter-test] tower parts ok: " + built + " | " + missing);
	}

	/**
	 * The substation yard at true size (4.5d, docs/multiblocks/substation.md), placed the way a player places it: each part
	 * clicked on the ground where the yard needs it, from the north looking south, so it faces north. Without its 138 kV bus
	 * the transformer's readout names it; with every part the yard forms. Breaking one piece of a disconnect drops one
	 * disconnect; a one-block breaker from an older world drops as an item.
	 */
	private void substationParts(ClientGameTestContext context, TestServerContext server) {
		int z = -880;
		server.runCommand("tp @p 20 " + (up + 8) + " " + (z - 14) + " 0 25");
		context.waitTicks(20);
		server.runCommand("fill 0 " + (y - 1) + " " + (z - 16) + " 40 " + (y + 9) + " " + (z + 12) + " minecraft:air");
		server.runCommand("fill 0 " + (y - 1) + " " + (z - 16) + " 40 " + y + " " + (z + 12) + " minecraft:stone");   // ground at y
		server.runCommand("kill @e[type=item]");
		// the yard's parts at their main cells, x along the line from 10, z across from z
		Object[][] parts = {
				{WildcatterBlocks.HV_BREAKER_3P, 10, 0}, {WildcatterBlocks.DISCONNECT_HV, 12, 0}, {WildcatterBlocks.POWER_TRANSFORMER, 16, 2},
				{WildcatterBlocks.RELAY_PANEL, 18, 0}, {WildcatterBlocks.BUS_DIST, 22, 3}, {WildcatterBlocks.DISCONNECT_DIST, 23, 3},
				{WildcatterBlocks.DIST_BREAKER_3P, 24, 3}};
		for (Object[] part : parts) {
			net.minecraft.world.level.block.Block block = (net.minecraft.world.level.block.Block) part[0];
			BlockPos main = new BlockPos((Integer) part[1], up, z + (Integer) part[2]);
			check(placeOn(server, block, main.below(), main.getX() + 0.5, z - 6.5, 0.0F), "a " + block.getName().getString() + " should place on clear ground");
			check(stateAt(server, main).is(block) && stateAt(server, main).getValue(dev.wildcatter.block.BigPartBlock.FACING) == net.minecraft.core.Direction.NORTH,
					"placed from the north, the " + block.getName().getString() + " should face north at its main cell: " + stateAt(server, main));
		}
		String waiting = "";
		for (int waited = 0; waited < 100 && !waiting.contains("138 kV bus"); waited += 10) {
			context.waitTicks(10);
			waiting = read(server, 16, up, z + 2);   // the server can run a little behind the client's ticks: wait for its check
		}
		check(waiting.contains("138 kV bus") && waiting.contains("14 " + up + " " + z), "without its 138 kV bus the transformer should name it and where: " + waiting);
		check(placeOn(server, WildcatterBlocks.BUS_HV, new BlockPos(14, y, z), 14.5, z - 6.5, 0.0F), "the 138 kV bus should place");
		context.waitTicks(40);
		String formed = read(server, 16, up, z + 2);
		check(formed.contains("relays") && !formed.contains("needs"), "with every part in place the yard should form: " + formed);
		server.runCommand("tp @p 6 " + (up + 7) + " " + (z - 8) + " -45 25");
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-substation-built");
		// break one piece of the 138 kV disconnect: the whole disconnect goes, one drops, and the yard names it
		server.runCommand("kill @e[type=item]");
		BlockPos piece = new BlockPos(13, up + 3, z + 8);
		check(stateAt(server, piece).is(WildcatterBlocks.DISCONNECT_HV), "the disconnect should fill its far top corner: " + stateAt(server, piece));
		server.computeOnServer(srv -> srv.overworld().destroyBlock(piece, true));
		context.waitTicks(40);
		check(stateAt(server, new BlockPos(12, up, z)).isAir() && itemsNear(server, new BlockPos(12, up, z + 4), WildcatterBlocks.DISCONNECT_HV) == 1,
				"breaking one disconnect piece should take the whole disconnect and drop one");
		check(read(server, 16, up, z + 2).contains("138 kV disconnect"), "without its disconnect the yard should say so: " + read(server, 16, up, z + 2));
		// an older world's one-block breaker: the yard's layout changed, so it drops
		server.runCommand("kill @e[type=item]");
		put(server, 34, up, z + 10, "wildcatter:hv_breaker_3p[facing=north]");
		context.waitTicks(10);
		check(stateAt(server, new BlockPos(34, up, z + 10)).isAir() && itemsNear(server, new BlockPos(34, up, z + 10), WildcatterBlocks.HV_BREAKER_3P) == 1,
				"a one-block 138 kV breaker from an older world should drop as one item");
		System.out.println("[wildcatter-test] substation parts ok: " + waiting + " | " + formed);
	}

	/**
	 * The overhead line at true size (4.5e, docs/multiblocks/poles.md), put up the way a player does: a 40 ft pole placed by a
	 * click stands 10 m; an arm clicked on its side near the top goes to the standard place across the line; pin insulators on the
	 * arm and the pole top take those mounts; a pole transformer clicked on the pole hangs with its back to it (screenshot).
	 * Breaking the pole drops the pole, the arm and the insulators. Two H-frames with their strings (screenshot). An older world's
	 * stacked pole still carries its span to a lamp, and an older world's one-block pad-mount grows into its 2 x 2 x 2.
	 */
	private void lineParts(ClientGameTestContext context, TestServerContext server) {
		int z = -930;
		server.runCommand("tp @p 10 " + (up + 4) + " " + (z - 10) + " 0 -20");
		context.waitTicks(20);
		server.runCommand("fill 0 " + (y - 1) + " " + (z - 14) + " 48 " + (y + 24) + " " + (z + 14) + " minecraft:air");
		server.runCommand("fill 0 " + (y - 1) + " " + (z - 14) + " 48 " + y + " " + (z + 14) + " minecraft:grass_block");   // ground at y
		server.runCommand("kill @e[type=item]");
		BlockPos foot = new BlockPos(10, y, z);
		check(placeOn(server, WildcatterBlocks.WOOD_POLE_40, foot, 10.5, z - 5.5, 0.0F), "a 40 ft pole should place on clear ground");
		check(stateAt(server, new BlockPos(10, up + 9, z)).is(WildcatterBlocks.WOOD_POLE_40) && stateAt(server, new BlockPos(10, up + 10, z)).isAir(),
				"a 40 ft pole should stand 10 m: " + stateAt(server, new BlockPos(10, up + 9, z)));
		// standing south of it, looking north along the line, click the pole's side 2 m below its top: the arm goes to the top, across the line
		check(placeAt(server, WildcatterBlocks.CROSSARM_DIST, new BlockPos(10, up + 8, z), net.minecraft.core.Direction.NORTH, 10.5, z - 3.5, 0.0F),
				"a crossarm clicked on the pole should place");
		check(stateAt(server, new BlockPos(9, up + 9, z)).is(WildcatterBlocks.CROSSARM_DIST) && stateAt(server, new BlockPos(11, up + 9, z)).is(WildcatterBlocks.CROSSARM_DIST)
				&& stateAt(server, new BlockPos(10, up + 9, z)).is(WildcatterBlocks.WOOD_POLE_40),
				"the arm should go beside the pole's top cell, either side: " + stateAt(server, new BlockPos(9, up + 9, z)) + " | " + stateAt(server, new BlockPos(9, up + 8, z)));
		for (int x : new int[] {9, 10, 11}) {
			check(placeOn(server, WildcatterBlocks.INSULATOR_DIST, new BlockPos(x, up + 9, z), x + 0.5, z - 3.5, 0.0F), "a pin insulator should go on the arm or the pole top at x " + x);
		}
		check(stateAt(server, new BlockPos(9, up + 10, z)).getValue(dev.wildcatter.block.InsulatorBlock.MOUNT) == dev.wildcatter.block.InsulatorBlock.Mount.ARM
				&& stateAt(server, new BlockPos(10, up + 10, z)).getValue(dev.wildcatter.block.InsulatorBlock.MOUNT) == dev.wildcatter.block.InsulatorBlock.Mount.POLE_TOP,
				"the insulators should take the arm's and the pole top's mounts: " + stateAt(server, new BlockPos(9, up + 10, z)) + " | " + stateAt(server, new BlockPos(10, up + 10, z)));
		check(placeAt(server, WildcatterBlocks.POLE_TRANSFORMER, new BlockPos(10, up + 6, z), net.minecraft.core.Direction.SOUTH, 10.5, z + 3.5, 180.0F),
				"a pole transformer clicked on the pole should hang on it");
		check(stateAt(server, new BlockPos(10, up + 6, z + 1)).getValue(dev.wildcatter.block.TransformerBlock.FACING) == net.minecraft.core.Direction.SOUTH,
				"the pole transformer should hang with its back to the pole: " + stateAt(server, new BlockPos(10, up + 6, z + 1)));
		put(server, 9, up + 8, z + 1, "wildcatter:fused_cutout[facing=south]");
		check(!placeAt(server, WildcatterBlocks.CROSSARM_DIST, new BlockPos(20, y, z), net.minecraft.core.Direction.UP, 20.5, z - 3.5, 0.0F),
				"a crossarm with no pole should not place");
		server.runCommand("tp @p 15 " + (up + 9) + " " + (z + 7) + " 145 5");
		context.waitTicks(3);   // before the player falls far
		context.takeScreenshot("wildcatter-line-pole");
		// break the pole at its foot: it goes whole, and the arm and its insulators fall with it
		server.runCommand("kill @e[type=item]");
		put(server, 9, up + 8, z + 1, "minecraft:air");
		put(server, 10, up + 6, z + 1, "minecraft:air");
		server.computeOnServer(srv -> srv.overworld().destroyBlock(new BlockPos(10, up + 2, z), true));
		context.waitTicks(40);
		int poles = itemsNear(server, new BlockPos(10, up + 5, z), WildcatterBlocks.WOOD_POLE_40);
		int arms = itemsNear(server, new BlockPos(10, up + 5, z), WildcatterBlocks.CROSSARM_DIST);
		int pins = itemsNear(server, new BlockPos(10, up + 5, z), WildcatterBlocks.INSULATOR_DIST);
		check(poles == 1 && arms == 1 && pins == 3 && stateAt(server, new BlockPos(9, up + 9, z)).isAir(),
				"breaking the pole should drop one pole, its arm and its three insulators: " + poles + " " + arms + " " + pins
						+ " | left " + stateAt(server, new BlockPos(9, up + 10, z)) + " " + stateAt(server, new BlockPos(10, up + 10, z)) + " " + stateAt(server, new BlockPos(11, up + 10, z))
						+ " " + stateAt(server, new BlockPos(11, up + 9, z)));
		// a 138 kV H-frame pair: two 75 ft poles, the crossbeam, the X-brace and three hanging strings each, strung 20 m apart
		hFrame(server, 24, z);
		hFrame(server, 44, z);
		check(stateAt(server, new BlockPos(24, up + 18, z - 4)).getValue(dev.wildcatter.block.InsulatorBlock.MOUNT) == dev.wildcatter.block.InsulatorBlock.Mount.HANGING,
				"a 138 kV string under the crossbeam should hang: " + stateAt(server, new BlockPos(24, up + 18, z - 4)));
		for (int dz : new int[] {-4, 0, 4}) {
			dev.wildcatter.item.SpoolItem.Result hv = server.computeOnServer(srv -> dev.wildcatter.item.SpoolItem.connect(srv.overworld(),
					new BlockPos(24, up + 18, z + dz), new BlockPos(44, up + 18, z + dz), dev.wildcatter.energy.Voltage.HIGH, false, 1024));
			check(hv.ok(), "a 138 kV span between the H-frames should string: " + hv);
		}
		server.runCommand("tp @p 8 " + (up + 12) + " " + (z + 30) + " -140 -2");
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-h-frame");
		// an older world's stacked pole sections with an insulator on each still carry a 120 V span to a lamp
		int zz = z + 10;
		plant(server, zz);
		put(server, 12, up, zz, "wildcatter:wool_wire_hot");
		put(server, 12, up + 1, zz, "wildcatter:wool_wire_hot");
		put(server, 12, up + 2, zz, "wildcatter:wool_wire_hot");
		for (int px : new int[] {13, 20}) {
			put(server, px, up, zz, "wildcatter:pole");
			put(server, px, up + 1, zz, "wildcatter:pole");
			put(server, px, up + 2, zz, "wildcatter:insulator_lv");
		}
		for (int k = 0; k <= 2; k++) {
			put(server, 21, up + k, zz, "wildcatter:wool_wire_hot");
		}
		put(server, 22, up, zz, "wildcatter:electric_lamp");
		for (int x = 11; x <= 22; x++) {
			put(server, x, up, zz + 1, "wildcatter:wool_wire_neutral");
		}
		context.waitTicks(10);
		dev.wildcatter.item.SpoolItem.Result old = server.computeOnServer(srv -> dev.wildcatter.item.SpoolItem.connect(srv.overworld(),
				new BlockPos(13, up + 2, zz), new BlockPos(20, up + 2, zz), dev.wildcatter.energy.Voltage.LOW, false, 1024));
		check(old.ok(), "an older world's insulators should still take a span: " + old);
		context.waitTicks(160);
		check(lit(server, 22, zz), "the span on the older poles should light the lamp: " + read(server, 22, up, zz) + " | " + read(server, 20, up + 2, zz));
		// an older world's one-block pad-mount grows into its cells
		put(server, 30, up, z + 10, "wildcatter:padmount_208[facing=north]");
		context.waitTicks(10);
		check(stateAt(server, new BlockPos(31, up + 1, z + 11)).is(WildcatterBlocks.PADMOUNT_208), "a one-block pad-mount with room should grow to 2 x 2 x 2: " + stateAt(server, new BlockPos(31, up + 1, z + 11)));
		System.out.println("[wildcatter-test] line parts ok: pole, arm, insulators, transformer; break drops " + poles + "/" + arms + "/" + pins + "; H-frames strung; old span lit; pad-mount grew");
	}

	/**
	 * Building gear at true size (4.5f, docs/multiblocks/building-gear.md), put up the way a player does: a breaker clicked on a wall
	 * hangs on it with its line along the wall, a lamp and receptacles take the faces they are clicked on (a wall, the floor, the
	 * ceiling), and the starter turbine lights the lamp through the wall breaker. Screenshots of the wall of gear and the starter
	 * plant (the 1 x 2 x 1 boiler, the turbine, a firebox and a pump).
	 */
	private void buildingGear(ClientGameTestContext context, TestServerContext server) {
		int z = -960;
		net.minecraft.core.Direction south = net.minecraft.core.Direction.SOUTH;
		server.runCommand("tp @p 15 " + (up + 1) + " " + (z + 5) + " 180 10");
		context.waitTicks(20);
		server.runCommand("fill 4 " + (y - 1) + " " + (z - 4) + " 30 " + (y + 5) + " " + (z + 6) + " minecraft:air");
		server.runCommand("fill 4 " + (y - 1) + " " + (z - 4) + " 30 " + y + " " + (z + 6) + " minecraft:stone");   // ground at y
		server.runCommand("fill 12 " + up + " " + (z - 1) + " 26 " + (up + 2) + " " + (z - 1) + " minecraft:smooth_stone");   // a wall along the north
		server.runCommand("fill 18 " + (up + 2) + " " + z + " 22 " + (up + 2) + " " + z + " minecraft:smooth_stone");          // and a bit of ceiling
		plant(server, z);
		put(server, 12, up, z, "wildcatter:wool_wire_hot");
		// standing south of the wall, looking north: the breaker clicked on the wall hangs on it, its line running west to east
		check(placeAt(server, WildcatterBlocks.BREAKER, new BlockPos(13, up, z - 1), south, 13.5, z + 3.5, 180.0F), "a breaker should place on the wall");
		net.minecraft.world.level.block.state.BlockState breaker = stateAt(server, new BlockPos(13, up, z));
		check(breaker.getValue(dev.wildcatter.block.GearMount.MOUNT) == dev.wildcatter.block.GearMount.WALL && breaker.getValue(BreakerBlock.FACING) == net.minecraft.core.Direction.EAST,
				"a breaker on a north wall should hang on it, its line running east: " + breaker);
		put(server, 14, up, z, "wildcatter:wool_wire_hot");
		check(placeAt(server, WildcatterBlocks.ELECTRIC_LAMP, new BlockPos(15, up, z - 1), south, 15.5, z + 3.5, 180.0F), "a lamp should place on the wall");
		for (int x = 11; x <= 15; x++) {
			put(server, x, up, z + 1, "wildcatter:wool_wire_neutral");
		}
		check(placeAt(server, WildcatterBlocks.RECEPTACLE_NEMA_5_15, new BlockPos(17, up, z - 1), south, 17.5, z + 3.5, 180.0F), "a receptacle should place on the wall");
		check(placeOn(server, WildcatterBlocks.RECEPTACLE_NEMA_L22_30, new BlockPos(18, y, z), 18.5, z + 3.5, 180.0F), "a receptacle should place on the floor");
		check(placeAt(server, WildcatterBlocks.ELECTRIC_LAMP, new BlockPos(19, up + 2, z), net.minecraft.core.Direction.DOWN, 19.5, z + 3.5, 180.0F), "a lamp should hang from the ceiling");
		check(stateAt(server, new BlockPos(15, up, z)).getValue(dev.wildcatter.block.AttachedGear.ATTACH) == net.minecraft.core.Direction.NORTH
				&& stateAt(server, new BlockPos(18, up, z)).getValue(dev.wildcatter.block.AttachedGear.ATTACH) == net.minecraft.core.Direction.DOWN
				&& stateAt(server, new BlockPos(19, up + 1, z)).getValue(dev.wildcatter.block.AttachedGear.ATTACH) == net.minecraft.core.Direction.UP,
				"gear should mount on the face it was clicked on: " + stateAt(server, new BlockPos(15, up, z)) + " | " + stateAt(server, new BlockPos(18, up, z)) + " | " + stateAt(server, new BlockPos(19, up + 1, z)));
		for (Object[] g : new Object[][] {{WildcatterBlocks.ELECTRIC_HEATER, 21}, {WildcatterBlocks.MOTOR_STARTER, 22}, {WildcatterBlocks.BREAKER_3P_INDUSTRIAL, 23},
				{WildcatterBlocks.SWITCH, 24}, {WildcatterBlocks.THREE_PHASE_HEATER, 25}}) {
			int gx = (Integer) g[1];
			check(placeAt(server, (net.minecraft.world.level.block.Block) g[0], new BlockPos(gx, up, z - 1), south, gx + 0.5, z + 3.5, 180.0F), "gear should place on the wall at x " + gx);
		}
		put(server, 8, up, z, "wildcatter:firebox[facing=south]");
		put(server, 8, up, z + 2, "wildcatter:pump[facing=east]");
		context.waitTicks(160);
		check(lit(server, 15, z), "the lamp should light through the wall breaker: " + read(server, 15, up, z) + " | " + read(server, 13, up, z));
		check(stateAt(server, new BlockPos(10, up + 1, z)).is(WildcatterBlocks.BOILER), "the boiler should stand 2 m over its campfire");
		server.runCommand("tp @p 18 " + (up + 1) + " " + (z + 4) + " 180 12");
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-building-gear");
		server.runCommand("tp @p 6 " + (up + 1) + " " + (z + 4) + " -140 15");
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-starter-plant");
		System.out.println("[wildcatter-test] building gear ok: " + breaker + " | " + read(server, 15, up, z));
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
