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
		// the test world starts as an existing world would, every world rule off; the scenes switch on the rules they show
		dev.wildcatter.RulesData.testWorldsAsExisting = true;
		createWorldScreen(context);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			context.waitTicks(40);
			checkAssets(context);
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
			if (only("landRig")) {
				landRig(context, server);
			}
			if (only("lease")) {
				lease(context, server);
			}
			if (only("gasPlant")) {
				gasPlant(context, server);
			}
			if (only("toppingUnit")) {
				toppingUnit(context, server);
			}
			if (only("crudeUnit")) {
				crudeUnit(context, server);
			}
			if (only("cracker")) {
				cracker(context, server);
			}
			if (only("gasTurbine")) {
				gasTurbine(context, server);
			}
			if (only("peaker")) {
				peaker(context, server);
			}
			if (only("ores")) {
				ores(context, server);
			}
			if (only("saltDomeFilm")) {
				SaltDomeFilmScene.run(context, server, y);
			}
			if (only("acidPlant")) {
				acidPlant(context, server);
			}
			if (only("tailingsCell")) {
				tailingsCell(context, server);
			}
			if (only("uraniumMill")) {
				uraniumMill(context, server);
			}
			if (only("isrWellfield")) {
				isrWellfield(context, server);
			}
			if (only("conversion")) {
				conversion(context, server);
			}
			if (only("ammoniaPlant")) {
				ammoniaPlant(context, server);
			}
			if (only("aduYellowcake")) {
				aduYellowcake(context, server);
			}
			if (only("phosphatePlant")) {
				phosphatePlant(context, server);
			}
			if (only("salesGas")) {
				new GasPowerClientScenes(context, server, y, up, -8200).salesGas();
			}
			if (only("acidSteam")) {
				new GasPowerClientScenes(context, server, y, up, -8400).acidSteam();
			}
			if (only("biogas")) {
				new GasPowerClientScenes(context, server, y, up, -8600).biogas();
			}
			if (only("ferroalloys")) {
				new FerroalloyClientScenes(context, server, y, up, -9400).ferroalloys();
			}
			if (only("cascade")) {
				cascade(context, server);
			}
			if (only("zirconium")) {
				zirconium(context, server);
			}
			if (only("fuel")) {
				fuel(context, server);
			}
			if (only("plc")) {
				plc(context, server);
			}
			if (only("plcSfc")) {
				plcSfc(context, server);
			}
			if (only("plcAnalog")) {
				plcAnalog(context, server);
			}
			if (only("plcPower")) {
				plcPower(context, server);
			}
			if (only("plcNetwork")) {
				plcNetwork(context, server);
			}
			if (only("boron")) {
				boron(context, server);
			}
			if (only("reactor")) {
				reactor(context, server);
			}
			if (only("turbineIsland")) {
				turbineIsland(context, server);
			}
			if (only("coolingTower")) {
				coolingTower(context, server);
			}
			if (only("reactorSafety")) {
				reactorSafety(context, server);
			}
			if (only("fuelHandling")) {
				fuelHandling(context, server);
			}
			if (only("teamRebornEnergy")) {
				teamRebornEnergy(context, server);
			}
			if (only("drives")) {
				drives(context, server);
			}
			if (only("dcTier")) {
				dcTier(context, server);
			}
			if (only("bayer")) {
				bayer(context, server);
			}
			if (only("alf3Lime")) {
				alf3Lime(context, server);
			}
			if (only("coker")) {
				coker(context, server);
			}
			if (only("cokeOvens")) {
				cokeOvens(context, server);
			}
			if (only("anodes")) {
				anodes(context, server);
			}
			if (only("potline")) {
				potline(context, server);
			}
			if (only("blueprints")) {
				blueprints(context, server);
			}
			if (only("cranes")) {
				cranes(context, server);
			}
			if (only("heavyLift")) {
				heavyLift(context, server);
			}
			if (only("busProtection")) {
				busProtection(context, server);
			}
			if (only("switchgear")) {
				switchgear(context, server);
			}
			if (only("panels")) {
				panels(context, server);
			}
			if (only("ats")) {
				ats(context, server);
			}
			if (only("ringTravel")) {
				ringTravel(context, server);
			}
			if (only("reactorContainment")) {
				new ReactorClientScenes(context, server, y, -9600).containment();
			}
			fieldManual(server);
		}
	}

	/**
	 * The ring crane's rails (docs/questions/plant-crane-gaps.md, 4): a ring crane on a row of crane rail under its slewing centre, its
	 * site 150 m down the rails and out of its 120 m reach, travels along them (sped up) until the site is 50 m out, and stops there.
	 */
	private void ringTravel(ClientGameTestContext context, TestServerContext server) {
		final int z = -9000;   // clear of salesGas (-8200), acidSteam (-8400) and biogas (-8600)
		server.runCommand("weather clear");
		server.runCommand("effect give @p minecraft:slow_falling 600 0 true");
		server.runCommand("tp @p 100 " + (up + 70) + " " + (z + 90) + " 90 35");
		context.waitTicks(60);
		for (int zz = z - 2; zz <= z + 200; zz += 40) {
			int z2 = Math.min(zz + 39, z + 200);
			server.runCommand("fill 34 " + y + " " + zz + " 86 " + (y + 12) + " " + z2 + " minecraft:air");
			server.runCommand("fill 34 " + (y - 1) + " " + zz + " 86 " + (y - 1) + " " + z2 + " minecraft:stone");
		}
		for (int zz = z + 20; zz <= z + 190; zz++) {
			put(server, 60, y - 1, zz, "wildcatter:crane_rail[axis=z]");
		}
		BlockPos ring = new BlockPos(60, y, z);
		placeBig(server, WildcatterBlocks.RING_CRANE, ring, net.minecraft.core.Direction.NORTH);   // x 36-84, z..z+48, its centre 60.5, z+24.5
		BlockPos origin = new BlockPos(70, y, z + 174);
		for (int x = 71; x <= 72; x++) {
			put(server, x, y, z + 175, "minecraft:stone_bricks");
		}
		String data = server.computeOnServer(srv -> dev.wildcatter.item.SurveyToolItem.record(srv.overworld(), origin, origin.offset(3, 1, 2)).compressed());
		server.runCommand("fill 70 " + y + " " + (z + 174) + " 73 " + (y + 1) + " " + (z + 176) + " minecraft:air");
		server.computeOnServer(srv -> {
			srv.overworld().setBlock(origin, WildcatterBlocks.SITE_MARKER.defaultBlockState(), 3);
			((dev.wildcatter.block.SiteMarkerBlockEntity) srv.overworld().getBlockEntity(origin)).set(data, 0);
			dev.wildcatter.block.CraneBlockEntity c = (dev.wildcatter.block.CraneBlockEntity) srv.overworld().getBlockEntity(ring);
			c.testFuel(6_000);
			c.testSpeedup(720);   // 2 m/s: a metre every 10 ticks
			return null;
		});
		context.waitTicks(400);
		String moving = read(server, 60, y, z + 30) + read(server, 60, y, z + 40);
		System.out.println("[wildcatter-test] ringTravel: on its way | " + moving);
		context.takeScreenshot("wildcatter-ring-crane-rails");
		BlockPos stop = new BlockPos(60, y, z + 101);
		String arrived = waitFor(context, server, () -> server.computeOnServer(srv -> srv.overworld().getBlockEntity(stop) instanceof dev.wildcatter.block.CraneBlockEntity c
				&& !c.travelling()), 2400);
		String there = read(server, 60, y, z + 101);
		System.out.println("[wildcatter-test] ringTravel: arrived " + arrived + " | " + there);
		check(arrived.startsWith("yes") && !there.contains("NO SITE") && !there.contains("TRAVEL"), "the ring crane should travel 101 m along its rails and find its site in reach: " + there);
		context.takeScreenshot("wildcatter-ring-crane-arrived");
		System.out.println("[wildcatter-test] ringTravel ok: " + there);
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
			if (oil == null || oil.deep() || oil.kind() != Reservoir.Kind.OIL || oil.topY() > -42) {
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
			String echo = server.computeOnServer(srv -> SeismographItem.report(srv.overworld(), rx + 7.5, rz + 0.5));   // over the hole
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
			// the small rig reaches 110 m: one already that deep stops TOO DEEP (a second rig south of the pole, its box on the pole's cable)
			BlockPos deepRig = new BlockPos(rx, top + 1, rz + 6);
			placeBig(server, WildcatterBlocks.DRILLING_RIG, deepRig, net.minecraft.core.Direction.SOUTH);
			server.computeOnServer(srv -> {
				((dev.wildcatter.block.DrillingRigBlockEntity) srv.overworld().getBlockEntity(deepRig)).testDepth(Tuning.SMALL_RIG_MAX_DEPTH);
				return null;
			});
			context.waitTicks(120);
			String tooDeep = read(server, deepRig.getX(), deepRig.getY(), deepRig.getZ());
			check(tooDeep.contains("TOO DEEP"), "a small rig " + Tuning.SMALL_RIG_MAX_DEPTH + " m down should stop: " + tooDeep);
			server.computeOnServer(srv -> srv.overworld().destroyBlock(deepRig, false));
			context.waitTicks(5);
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
			check(jack.matches(".*flowline [1-9]\\d*/.*") && jack.contains("water cut 1"), "a wired pumpjack should fill its flowline with a new field's stream: " + jack);
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
			check(head.contains("flowing"), "the wellhead should be flowing to the firebox: " + head);
			// a gas genset whose engine stands against the same well will not take its wet gas (docs/multiblocks/lease.md): the
			// wellhead touches the engine's third cell along, not its main cell, so the line finds the engine through any of its cells
			BlockPos gasSet = new BlockPos(gx + 3, up, gz + 1);
			genset(server, gasSet, net.minecraft.core.Direction.NORTH, true);   // the skid runs west: engine x gx-2 to gx+1, behind the wellhead
			context.waitTicks(30);
			server.computeOnServer(srv -> {
				panel(srv, gasSet).setWantRun(true);
				return null;
			});
			context.waitTicks(80);
			String turbine = read(server, gx + 3, up, gz + 1);
			check(turbine.contains("Gas genset") && turbine.contains("WET GAS") && !turbine.contains("RUNNING"), "a gas genset straight on a gas well should refuse its wet gas: " + turbine);
			String boiler = read(server, gx + 2, up + 1, gz) + " | " + turbine;
			System.out.println("[wildcatter-test] wells ok: " + echo + " | " + jack + " | " + head + " | " + fire + " | " + boiler);
		} finally {
			ReservoirData.testSeed = null;
		}
	}

	/**
	 * A genset and the 4.16 kV tier (the refinery that stood here is gone, docs/multiblocks/crude-units.md: see toppingUnit): a 480 V
	 * diesel genset and a unit substation run backward to 4.16 kV; plastic wire reads safe; an MV breaker trips on a ground fault.
	 */
	private void refining(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("fill 66 " + (y - 1) + " 56 82 " + (y + 5) + " 66 minecraft:air");
		server.runCommand("fill 66 " + (y - 1) + " 56 82 " + (y - 1) + " 66 minecraft:stone");
		server.runCommand("kill @e[type=item]");
		server.runCommand("tp @p 74 " + up + " 70 180 20");
		BlockPos set = new BlockPos(72, up, 60);
		genset(server, set, net.minecraft.core.Direction.NORTH, false);   // skid to the west: alternator 71, engine 70, radiator 69
		// the unit substation (true size, x 73-76 and z 59-60) takes 480 V at its west end and gives 4.16 kV at its east end
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, new BlockPos(72, up, 60), net.minecraft.core.Direction.EAST, false);
		// 10d2: its 4.16 kV into a switchgear lineup, MAIN at x 78 (cable in low on its back at z 60) and a FEEDER at x 79, both facing north
		put(server, 77, up, 60, "wildcatter:cable_mv");
		put(server, 78, up, 60, "wildcatter:cable_mv");
		placeBig(server, WildcatterBlocks.MV_SWITCHGEAR_MAIN, new BlockPos(78, up, 57), net.minecraft.core.Direction.NORTH);
		placeBig(server, WildcatterBlocks.MV_SWITCHGEAR_FEEDER, new BlockPos(79, up, 57), net.minecraft.core.Direction.NORTH);
		put(server, 79, up + 3, 59, "wildcatter:cable_mv");
		put(server, 80, up + 3, 59, "wildcatter:cable_mv");
		server.computeOnServer(srv -> {
			for (int x = 78; x <= 79; x++) {
				dev.wildcatter.block.SwitchgearSectionBlockEntity s = (dev.wildcatter.block.SwitchgearSectionBlockEntity) srv.overworld().getBlockEntity(new BlockPos(x, up, 57));
				s.setPickup(x == 78 ? 600 : 25);
				s.operate();
			}
			return null;
		});
		startGenset(context, server, set, net.minecraft.core.Direction.NORTH, 100);
		context.waitTicks(100);
		String gen = read(server, 72, up, 60);
		check(gen.contains("Diesel genset") && gen.contains("RUNNING"), "the diesel genset should be running: " + gen);
		String refinery = "";
		put(server, 70, up, 58, "wildcatter:plastic_wire_hot");
		String wire = read(server, 70, up, 58);
		check(wire.contains("plastic insulated"), "plastic wire should say so: " + wire);
		// A 4.16 kV fault on the feeder's cable (the grounding set): the feeder's 50 must trip it, and the main stay closed
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.MultiCableBlockEntity) srv.overworld().getBlockEntity(new BlockPos(80, up + 3, 59))).setGrounded(true);
			return null;
		});
		context.waitTicks(60);
		String breaker = read(server, 79, up, 57);
		check(breaker.contains("TRIPPED") && breaker.contains("4.16 kV") && read(server, 78, up, 57).contains("CLOSED"),
				"a 4.16 kV feeder should trip on a fault on its cable, the main staying closed: " + breaker + " | " + read(server, 78, up, 57));
		context.takeScreenshot("wildcatter-mv-fault");
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
		for (int waited = 0; waited < 1200 && !(isAir(server, 12, up, 10) || isAir(server, 12, up, 11)); waited += 20) {
			context.waitTicks(20);   // the wire heats over seconds (energy/WireHeat)
		}
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
		boolean burnt = false;
		for (int waited = 0; waited < 1200 && !burnt; waited += 20) {
			context.waitTicks(20);
			burnt = isAir(server, 12, up, 10) || isAir(server, 13, up, 10) || isAir(server, 14, up, 10) || isAir(server, 14, up, 11)
					|| isAir(server, 11, up, 11) || isAir(server, 12, up, 11) || isAir(server, 13, up, 11);
		}
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
		put(server, 13, up, 13, "wildcatter:pole");   // a 4.16 kV pole transformer hangs on a pole
		put(server, 13, up, 14, "wildcatter:pole_transformer_mv[facing=east]");
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
		put(server, 16, up, 15, "wildcatter:pole");
		put(server, 16, up, 14, "wildcatter:pole_transformer_mv[facing=west]");
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
		// 10d2: the line's breaker is a yard breaker (x 20-21, z-1..z+1, terminals on z) worked by a relay cabinet north of it
		placeBig(server, WildcatterBlocks.DIST_BREAKER_3P, new BlockPos(20, up, z - 1), net.minecraft.core.Direction.NORTH);
		placeBig(server, WildcatterBlocks.BREAKER_RELAY_CABINET, new BlockPos(20, up, z - 2), net.minecraft.core.Direction.NORTH);
		for (int x = 22; x <= 23; x++) {
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
		// overload it: rate it at 30 VA for the test with two lamps (120 VA) on it, 4 x on each leg. Under IEEE C57.109 that lasts
		// (I² - 1)·t = 1250 pu²·s: about 83 s, so it reads OVERLOADED with its time left
		dev.wildcatter.block.TransformerBlockEntity.testRatedVa = 30.0;
		context.waitTicks(30);
		String heating = read(server, 20, up, z);
		check(heating.matches(".*OVERLOADED at (39\\d|40\\d)%.*") && heating.contains("fails in"), "a pole transformer at 4 x should read OVERLOADED with its time left: " + heating);
		// at 25 x it lasts 2 s
		dev.wildcatter.block.TransformerBlockEntity.testRatedVa = 4.8;
		context.waitTicks(100);
		dev.wildcatter.block.TransformerBlockEntity.testRatedVa = null;
		check(isAir(server, 20, up, z), "a pole transformer at 25 times its rating should burn out in about 2 s: " + read(server, 20, up, z));
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
		server.runCommand("tp @p 14 " + (up + 1) + " " + (z + 5) + " 180 15");   // load the scene's chunks first
		context.waitTicks(20);
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
		dev.wildcatter.energy.EnergyBridge.clearTests();
		dev.wildcatter.energy.EnergyBridge.registerForTest(target -> target.level().getBlockState(target.pos()).is(net.minecraft.world.level.block.Blocks.IRON_BLOCK)
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
		dev.wildcatter.energy.EnergyBridge.clearTests();
		check(isAir(server, 14, up, z) && isAir(server, 13, up, z), "a plug held above its rating should burn out with its receptacle");
		System.out.println("[wildcatter-test] plugs ok: " + plug + " | " + wrong + " | FE gained " + gained);
	}

	/**
	 * Soft starters and drives (Phase 10a, docs/multiblocks/soft-starter.md, vfd.md). The 250 hp 4.16 kV charge pump on one 500 kW
	 * set (a diesel set, a backward unit substation to 4.16 kV): direct on line it cannot start; behind the 4.16 kV soft starter it
	 * starts with the current held at the dial and bypasses; behind the 4.16 kV drive lineup it starts at rated current and a 0.95
	 * line power factor, and breaking the cell section stops it. Two 500 kW coolant pumps on one three-set island, each on its own
	 * drive, both run. A 480 V MCC's soft-starter and drive buckets each run a 60 hp pump.
	 */
	private void drives(ClientGameTestContext context, TestServerContext server) {
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		java.util.function.IntConsumer clear = z -> {
			server.runCommand("fill 0 " + (y - 1) + " " + (z - 8) + " 30 " + (y + 8) + " " + (z + 16) + " minecraft:air");
			server.runCommand("fill 0 " + (y - 1) + " " + (z - 8) + " 30 " + (y - 1) + " " + (z + 16) + " minecraft:stone");
		};
		server.runCommand("effect give @p minecraft:slow_falling 600 0 true");
		// ---- direct on line: the pump starts at locked-rotor current
		int z = -4600;
		server.runCommand("tp @p 16 " + (up + 12) + " " + (z + 14) + " 180 35");
		context.waitTicks(20);
		clear.accept(z);
		BlockPos set = new BlockPos(10, up, z);
		genset(server, set, north, false);
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, set, net.minecraft.core.Direction.EAST, false);   // 4.16 kV out at x 15
		for (int x = 15; x <= 22; x++) {
			put(server, x, up, z, "wildcatter:cable_mv");
		}
		BlockPos pump = new BlockPos(23, up, z);
		placeBig(server, WildcatterBlocks.CRUDE_CHARGE_PUMP, pump, north);   // x 23-24, z..z+4; its box on the west of its main cell
		startGenset(context, server, set, north, 2000);
		double dolPeak = 0;
		for (int t = 0; t < 200; t += 10) {
			runMotors(context, server, java.util.List.of(pump), 10);
			BlockPos p0 = pump;
			dolPeak = Math.max(dolPeak, server.computeOnServer(srv -> ((dev.wildcatter.block.UnitMotorBlockEntity) srv.overworld().getBlockEntity(p0)).motor().amps()));
		}
		String dol = read(server, pump.getX(), up, pump.getZ());
		System.out.println("[wildcatter-test] drives: direct on line: peak " + dolPeak + " A; " + dol + " | " + read(server, set.getX(), up, set.getZ()));
		check(dolPeak > 4.5 * 32.4, "direct on line the pump should draw about 6 x its full-load amps: " + dolPeak);

		// ---- the 4.16 kV soft starter: its line on its top, its motor out of the back of its bottom cell
		z = -4640;
		server.runCommand("tp @p 16 " + (up + 12) + " " + (z + 14) + " 180 35");
		context.waitTicks(20);
		clear.accept(z);
		set = new BlockPos(10, up, z);
		genset(server, set, north, false);
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, set, net.minecraft.core.Direction.EAST, false);
		for (int k = 0; k <= 3; k++) {
			put(server, 15, up + k, z, "wildcatter:cable_mv");
		}
		for (int x = 16; x <= 21; x++) {
			put(server, x, up + 3, z, "wildcatter:cable_mv");
		}
		BlockPos soft = new BlockPos(21, up, z);
		placeBig(server, WildcatterBlocks.SOFT_STARTER_MV, soft, north);   // its back to the south
		put(server, 21, up, z + 1, "wildcatter:cable_mv");
		put(server, 22, up, z + 1, "wildcatter:cable_mv");
		pump = new BlockPos(23, up, z + 1);
		placeBig(server, WildcatterBlocks.CRUDE_CHARGE_PUMP, pump, north);
		startGenset(context, server, set, north, 2000);
		press(server, soft);
		double softPeak = 0;
		String ramping = "";
		for (int t = 0; t < 600; t += 10) {
			runMotors(context, server, java.util.List.of(pump), 10);
			double amps = server.computeOnServer(srv -> ((dev.wildcatter.block.StarterCabinetBlockEntity) srv.overworld().getBlockEntity(soft)).starter().lineAmps());
			softPeak = Math.max(softPeak, amps);
			String now = read(server, soft.getX(), up, soft.getZ());
			if (now.contains("RAMPING") && ramping.isEmpty()) {
				ramping = now;
			}
		}
		String softRead = read(server, soft.getX(), up, soft.getZ());
		String softPump = read(server, pump.getX(), up, pump.getZ());
		System.out.println("[wildcatter-test] drives: soft starter: peak " + softPeak + " A; " + ramping + " | " + softRead + " | " + softPump + " | " + read(server, set.getX(), up, set.getZ()));
		check(ramping.contains("RAMPING, limit 3.5 x 32 A"), "the soft starter should ramp at its 3.5 x limit: " + ramping);
		check(softPeak < 3.5 * 32 * 1.1 && softPeak < dolPeak * 0.75, "the line current should stay at the limit, below the direct start's " + dolPeak + ": " + softPeak);
		check(softRead.contains("BYPASSED"), "the soft starter should bypass once the pump is up to speed: " + softRead);
		check(softPump.contains("running,"), "the pump should run behind the soft starter: " + softPump);
		server.runCommand("tp @p 24 " + (up + 4) + " " + (z - 7) + " 32 20");   // aimed just before the shot: the player falls between
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-soft-starter");

		// ---- the 4.16 kV drive lineup: line in at the transformer section's west end, motor out at the control section's east end
		z = -4680;
		server.runCommand("tp @p 18 " + (up + 9) + " " + (z + 12) + " 180 30");
		context.waitTicks(20);
		clear.accept(z);
		set = new BlockPos(10, up, z);
		genset(server, set, north, false);
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, set, net.minecraft.core.Direction.EAST, false);
		put(server, 15, up, z, "wildcatter:cable_mv");
		put(server, 16, up, z, "wildcatter:cable_mv");
		placeBig(server, WildcatterBlocks.DRIVE_MV_TRANSFORMER, new BlockPos(17, up, z), north);   // x 17-18
		placeBig(server, WildcatterBlocks.DRIVE_MV_CELLS, new BlockPos(19, up, z), north);         // x 19-20
		BlockPos control = new BlockPos(21, up, z);
		placeBig(server, WildcatterBlocks.DRIVE_MV_CONTROL, control, north);
		put(server, 22, up, z, "wildcatter:cable_mv");
		pump = new BlockPos(23, up, z);
		placeBig(server, WildcatterBlocks.CRUDE_CHARGE_PUMP, pump, north);
		startGenset(context, server, set, north, 2000);
		press(server, control);
		double drivePeak = 0;
		String driveRamp = "";
		for (int t = 0; t < 300; t += 10) {
			runMotors(context, server, java.util.List.of(pump), 10);
			drivePeak = Math.max(drivePeak, server.computeOnServer(srv -> ((dev.wildcatter.block.StarterCabinetBlockEntity) srv.overworld().getBlockEntity(control)).starter().lineAmps()));
			String now = read(server, control.getX(), up, control.getZ());
			if (now.contains("RAMPING") && driveRamp.isEmpty() && t > 60) {
				driveRamp = now;
			}
		}
		String driveRead = read(server, control.getX(), up, control.getZ());
		String drivePump = read(server, pump.getX(), up, pump.getZ());
		double pf = server.computeOnServer(srv -> ((dev.wildcatter.block.StarterCabinetBlockEntity) srv.overworld().getBlockEntity(control)).starter().linePowerFactor());
		System.out.println("[wildcatter-test] drives: drive: peak " + drivePeak + " A, PF " + pf + "; " + driveRamp + " | " + driveRead + " | " + drivePump);
		check(driveRamp.contains("RAMPING") && driveRamp.contains("Hz"), "the drive should ramp its frequency: " + driveRamp);
		check(drivePeak <= 1.5 * 32.4, "the line current should stay within 1.5 x full-load amps: " + drivePeak);
		check(pf >= 0.94, "the drive's line power factor should be about 0.95: " + pf);
		check(driveRead.contains("RUNNING 60.0 Hz"), "the drive should reach full frequency: " + driveRead);
		check(drivePump.contains("running,"), "the pump should run on the drive: " + drivePump);
		server.runCommand("tp @p 20 " + (up + 3) + " " + (z - 7) + " 10 15");   // aimed just before the shot: the player falls between
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-drive-lineup");
		BlockPos cellPiece = new BlockPos(20, up + 1, z);
		server.computeOnServer(srv -> srv.overworld().destroyBlock(cellPiece, true));   // a piece of the cell section
		runMotors(context, server, java.util.List.of(pump), 60);
		String broken = read(server, control.getX(), up, control.getZ());
		check(broken.contains("NOT BUILT") && broken.contains("power cell section"), "the drive should name its missing cell section: " + broken);
		check(!read(server, pump.getX(), up, pump.getZ()).contains("running,"), "the pump should stop with the drive broken: " + read(server, pump.getX(), up, pump.getZ()));

		// ---- two 500 kW coolant pumps on one three-set island, each on its own drive
		z = -4740;
		server.runCommand("tp @p 18 " + (up + 14) + " " + (z + 20) + " 180 35");
		context.waitTicks(20);
		clear.accept(z);
		BlockPos[] sets = {new BlockPos(10, up, z), new BlockPos(10, up, z + 4), new BlockPos(10, up, z + 8)};
		for (BlockPos s : sets) {
			genset(server, s, north, false);
			inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, s, net.minecraft.core.Direction.EAST, false);
		}
		for (int zz = z; zz <= z + 8; zz++) {
			put(server, 15, up, zz, "wildcatter:cable_mv");   // the 4.16 kV bus joining the three unit substations
		}
		BlockPos[] controls = {new BlockPos(21, up, z), new BlockPos(21, up, z + 4)};
		BlockPos[] pumps = {new BlockPos(23, up, z), new BlockPos(23, up, z + 4)};
		for (int i = 0; i < 2; i++) {
			int zz = controls[i].getZ();
			put(server, 16, up, zz, "wildcatter:cable_mv");
			placeBig(server, WildcatterBlocks.DRIVE_MV_TRANSFORMER, new BlockPos(17, up, zz), north);
			placeBig(server, WildcatterBlocks.DRIVE_MV_CELLS, new BlockPos(19, up, zz), north);
			placeBig(server, WildcatterBlocks.DRIVE_MV_CONTROL, controls[i], north);
			put(server, 22, up, zz, "wildcatter:cable_mv");
			placeBig(server, WildcatterBlocks.REACTOR_COOLANT_PUMP, pumps[i], north);   // x 23-24, two deep, five high
		}
		sizeCables(server, new BlockPos(15, up, z), new BlockPos(22, up, z + 8), WildcatterBlocks.CABLE_MV, dev.wildcatter.energy.ConductorSize.MV_4_0);
		for (BlockPos s : sets) {
			startGenset(context, server, s, north, 2000);
		}
		server.computeOnServer(srv -> {
			for (BlockPos c : controls) {
				for (int k = 0; k < 4; k++) {
					((dev.wildcatter.block.StarterCabinetBlockEntity) srv.overworld().getBlockEntity(c)).nextOverloadSetting();   // 32 A to 80 A: a 500 kW pump is 87 A
				}
			}
			return null;
		});
		press(server, controls[0]);
		runMotors(context, server, java.util.List.of(pumps), 300);
		press(server, controls[1]);
		runMotors(context, server, java.util.List.of(pumps), 300);
		for (int i = 0; i < 2; i++) {
			String p = read(server, pumps[i].getX(), up, pumps[i].getZ());
			check(p.contains("running,"), "coolant pump " + (i + 1) + " should run on its drive: " + p + " | " + read(server, controls[i].getX(), up, controls[i].getZ()));
		}
		for (BlockPos s : sets) {
			String st = read(server, s.getX(), up, s.getZ());
			check(st.contains("RUNNING") && st.contains("breaker CLOSED"), "the island should stay up with both pumps on drives: " + st);
		}
		System.out.println("[wildcatter-test] drives: two coolant pumps: " + read(server, pumps[0].getX(), up, pumps[0].getZ()) + " | " + read(server, pumps[1].getX(), up, pumps[1].getZ()));
		server.runCommand("tp @p 19 " + (up + 8) + " " + (z - 8) + " 10 35");   // aimed just before the shot: the player falls between
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-drives-coolant-pumps");

		// ---- a 480 V MCC: a soft-starter bucket and a drive bucket, each running a 60 hp pump
		z = -4800;
		server.runCommand("tp @p 12 " + (up + 4) + " " + (z + 7) + " 200 20");
		context.waitTicks(20);
		clear.accept(z);
		net.minecraft.core.Direction south = net.minecraft.core.Direction.SOUTH;
		placeBig(server, WildcatterBlocks.MCC_INCOMING, new BlockPos(14, up, z), south);
		placeBig(server, WildcatterBlocks.MCC_SECTION, new BlockPos(15, up, z), south);
		set = new BlockPos(13, up, z);
		genset(server, set, north, false);
		for (int k = 0; k <= 3; k++) {
			put(server, 14, up + k, z + 1, "wildcatter:cable_industrial");
		}
		put(server, 14, up + 3, z, "wildcatter:cable_industrial");
		check(clickWith(server, new BlockPos(15, up, z), south, new ItemStack(WildcatterItems.MCC_SOFT_STARTER_BUCKET)), "a soft-starter bucket should click into an empty space");
		check(clickWith(server, new BlockPos(15, up + 2, z), south, new ItemStack(WildcatterItems.MCC_DRIVE_BUCKET)), "a drive bucket should click into an empty space");
		// the soft starter's pump, off the back of the bottom bucket
		put(server, 15, up, z - 1, "wildcatter:cable_industrial");
		put(server, 15, up, z - 2, "wildcatter:cable_industrial");
		BlockPos pumpA = new BlockPos(16, up, z - 2);
		placeBig(server, WildcatterBlocks.HRSG_FEED_PUMP, pumpA, north);   // x 16-17, z-2..z+1
		// the drive's pump, off the back of the top bucket (clear of the bottom one's cable), round the incoming section
		put(server, 15, up + 2, z - 1, "wildcatter:cable_industrial");
		put(server, 14, up + 2, z - 1, "wildcatter:cable_industrial");
		put(server, 14, up + 1, z - 1, "wildcatter:cable_industrial");
		for (int zz = z - 2; zz >= z - 6; zz--) {
			put(server, 14, up + 1, zz, "wildcatter:cable_industrial");
		}
		put(server, 14, up, z - 6, "wildcatter:cable_industrial");
		put(server, 15, up, z - 6, "wildcatter:cable_industrial");
		BlockPos pumpB = new BlockPos(16, up, z - 6);
		placeBig(server, WildcatterBlocks.HRSG_FEED_PUMP, pumpB, north);   // x 16-17, z-6..z-3
		sizeCables(server, new BlockPos(14, up, z - 6), new BlockPos(15, up + 3, z + 1), WildcatterBlocks.CABLE_INDUSTRIAL, dev.wildcatter.energy.ConductorSize.AWG_4_0);
		startGenset(context, server, set, north, 1000);
		final int zm = z;
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.MccIncomingBlockEntity) srv.overworld().getBlockEntity(new BlockPos(14, up, zm))).nextRating();   // 225 A for two 60 hp pumps
			dev.wildcatter.block.MccSectionBlockEntity section = section(srv, 15, zm);
			for (int k = 0; k < 5; k++) {
				section.nextOverloadSetting(0);   // 11 A to 63 A on the soft starter...
				section.nextOverloadSetting(2);   // ...and the drive
			}
			section.nextLimitSetting(0);          // 3.5 x to 4 x
			section.pushButton(0);
			section.pushButton(2);
			return null;
		});
		runMotors(context, server, java.util.List.of(pumpA, pumpB), 300);
		String bucketSoft = read(server, 15, up, z);
		String bucketA = server.computeOnServer(srv -> section(srv, 15, zm).inspect(0).getString());
		String bucketB = server.computeOnServer(srv -> section(srv, 15, zm).inspect(2).getString());
		System.out.println("[wildcatter-test] drives: MCC: " + bucketSoft + " | " + bucketA + " | " + bucketB + " | " + read(server, 14, up, z));
		check(bucketA.contains("BYPASSED") && bucketA.contains("limit 4.0 x"), "the soft-starter bucket should bypass, its dial at 4 x: " + bucketA);
		check(bucketB.contains("RUNNING 60.0 Hz"), "the drive bucket should run at full frequency: " + bucketB);
		check(read(server, pumpA.getX(), up, pumpA.getZ()).contains("running,"), "the soft starter's pump should run: " + read(server, pumpA.getX(), up, pumpA.getZ()));
		check(read(server, pumpB.getX(), up, pumpB.getZ()).contains("running,"), "the drive's pump should run: " + read(server, pumpB.getX(), up, pumpB.getZ()));
		server.runCommand("tp @p 15 " + (up + 2) + " " + (z + 5) + " 180 12");   // aimed just before the shot: the player falls between
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-drives-mcc");
		System.out.println("[wildcatter-test] drives ok");
	}

	/**
	 * The DC tier (docs/multiblocks/dc-bus.md). A rectifier on a diesel set through a backward unit substation to 4.16 kV drives a
	 * magnesium cell 8 m away over 16 kA bus bars at its 14 kA setpoint, its poles split about earth. A ground wire against the - bus
	 * is a ground fault; a 4 kA bar in the run burns open. Open, the rectifier rises to 150 V; with its - pole faulted the + bus
	 * shocks, and a bar across the runs is a short that trips it. A second magnesium cell set against its rectifier, with no bus, takes
	 * its 14 kA as before.
	 */
	/**
	 * The Bayer refinery (10b2, docs/multiblocks/bayer-refinery.md). The line runs south down x 0: ball mill, digester with the flash
	 * tanks at its back, settler, polishing filters, precipitator, hydrate filter and the calciner at its back with a bagging station;
	 * the mud washer, filter press, causticizer, evaporator and package boiler stand east of a second busway at x 14. Pipes cross the
	 * busway one block up. Strong liquor runs back round the east side to the mill. LPG bullets fire the boiler and the calciner; a
	 * stock tank of 32 % caustic feeds the evaporator. Three diesel sets carry the 480 V parts; a fourth, through a backward unit
	 * substation, the calciner's 4.16 kV fan.
	 */
	private void bayer(ClientGameTestContext context, TestServerContext server) {
		final int z = -5100;
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		server.runCommand("effect give @p minecraft:slow_falling 1200 0 true");
		server.runCommand("tp @p 20 " + (up + 40) + " " + (z + 40) + " 90 60");
		context.waitTicks(20);
		for (int zz = z - 16; zz <= z + 80; zz += 17) {   // /fill takes at most 32,768 blocks
			server.runCommand("fill -27 " + (y - 1) + " " + zz + " 44 " + (y + 24) + " " + Math.min(zz + 16, z + 80) + " minecraft:air");
		}
		for (int zz = z - 16; zz <= z + 80; zz += 40) {
			server.runCommand("fill -27 " + (y - 1) + " " + zz + " 44 " + (y - 1) + " " + Math.min(zz + 39, z + 80) + " minecraft:stone");
		}
		// the west line
		BlockPos mill = new BlockPos(0, up, z);
		placeBig(server, WildcatterBlocks.BALL_MILL, mill, north);                         // x 0-3, z..z+7
		BlockPos digester = new BlockPos(0, up, z + 9);
		placeBig(server, WildcatterBlocks.DIGESTER, digester, north);                      // x 0-4, z+9..z+13
		BlockPos flash = new BlockPos(0, up, z + 14);
		placeBig(server, WildcatterBlocks.FLASH_TANKS, flash, north);                      // x 0-2, z+14..z+19
		BlockPos settler = new BlockPos(0, up, z + 21);
		placeBig(server, WildcatterBlocks.SETTLER, settler, north);                        // x 0-9, z+21..z+30
		BlockPos polishing = new BlockPos(0, up, z + 32);
		placeBig(server, WildcatterBlocks.POLISHING_FILTER, polishing, north);             // x 0-2, z+32..z+37
		BlockPos precipitator = new BlockPos(0, up, z + 39);
		placeBig(server, WildcatterBlocks.PRECIPITATOR, precipitator, north);              // x 0-11, z+39..z+50
		BlockPos hydrate = new BlockPos(0, up, z + 52);
		placeBig(server, WildcatterBlocks.HYDRATE_FILTER, hydrate, north);                 // x 0-3, z+52..z+61
		BlockPos calciner = new BlockPos(0, up, z + 62);
		placeBig(server, WildcatterBlocks.CALCINER, calciner, north);                      // x 0-5, z+62..z+67
		BlockPos bagger = new BlockPos(2, up, z + 68);
		placeBig(server, WildcatterBlocks.BAGGING_STATION, bagger, north);                 // x 2-3, z+68..z+69
		// the east side
		BlockPos boiler = new BlockPos(16, up, z + 2);
		placeBig(server, WildcatterBlocks.PACKAGE_BOILER, boiler, north);                  // x 16-19, z+2..z+10
		BlockPos washer = new BlockPos(16, up, z + 21);
		placeBig(server, WildcatterBlocks.MUD_WASHER, washer, north);                      // x 16-25, z+21..z+30
		BlockPos press = new BlockPos(16, up, z + 33);
		placeBig(server, WildcatterBlocks.RESIDUE_FILTER_PRESS, press, north);             // x 16-19, z+33..z+40
		BlockPos causticizer = new BlockPos(18, up, z + 41);
		placeBig(server, WildcatterBlocks.CAUSTICIZER, causticizer, north);                // x 18-20, z+41..z+43
		BlockPos evaporator = new BlockPos(16, up, z + 44);
		placeBig(server, WildcatterBlocks.EVAPORATOR, evaporator, north);                  // x 16-19, z+44..z+51
		BlockPos causticTank = new BlockPos(22, up, z + 47);
		placeBig(server, WildcatterBlocks.STOCK_TANK, causticTank, north);                 // x 22-25, z+47..z+50
		BlockPos bullet = new BlockPos(21, up, z + 1);
		placeBig(server, WildcatterBlocks.LPG_BULLET, bullet, north);                      // x 21-23, z+1..z+12
		BlockPos calcinerBullet = new BlockPos(7, up, z + 56);
		placeBig(server, WildcatterBlocks.LPG_BULLET, calcinerBullet, north);              // x 7-9, z+56..z+67
		// the pipes: {x, y offset, z offset}
		java.util.List<int[]> runs = new java.util.ArrayList<>();
		java.util.function.BiConsumer<int[], int[]> run = (from, to) -> {   // a straight run between two cells, both ends included
			int n = Math.max(Math.max(Math.abs(to[0] - from[0]), Math.abs(to[1] - from[1])), Math.abs(to[2] - from[2]));
			for (int i = 0; i <= n; i++) {
				runs.add(new int[] {from[0] + Integer.signum(to[0] - from[0]) * i, from[1] + Integer.signum(to[1] - from[1]) * i, from[2] + Integer.signum(to[2] - from[2]) * i});
			}
		};
		java.util.function.BiConsumer<Integer, Integer> hop = (xx, zz) -> {   // over the x 14 busway, one block up, from x 13 to x 15
			run.accept(new int[] {13, 1, zz}, new int[] {15, 1, zz});
		};
		runs.add(new int[] {1, 0, 8});                                          // slurry: mill to digester
		runs.add(new int[] {1, 0, 20});                                         // digested slurry: flash tanks to settler
		run.accept(new int[] {10, 0, 25}, new int[] {10, 0, 20});              // red mud: the settler's east side to the washer
		run.accept(new int[] {11, 0, 20}, new int[] {13, 0, 20});
		hop.accept(14, 20);
		run.accept(new int[] {16, 1, 20}, new int[] {16, 0, 20});
		run.accept(new int[] {1, 0, 31}, new int[] {13, 0, 31});               // green liquor: settler and washer to the polishing filters
		hop.accept(14, 31);
		run.accept(new int[] {16, 1, 31}, new int[] {16, 0, 31});
		run.accept(new int[] {26, 0, 24}, new int[] {26, 0, 32});              // washed mud: the washer's east side to the press
		run.accept(new int[] {25, 0, 32}, new int[] {17, 0, 32});
		runs.add(new int[] {1, 0, 38});                                         // polished liquor to the precipitator
		runs.add(new int[] {1, 0, 51});                                         // hydrate slurry to the hydrate filter
		run.accept(new int[] {4, 0, 53}, new int[] {12, 0, 53});               // spent liquor: the hydrate filter's east side to the evaporator
		run.accept(new int[] {12, 0, 52}, new int[] {12, 0, 43});
		runs.add(new int[] {13, 0, 43});
		hop.accept(14, 43);
		run.accept(new int[] {16, 1, 43}, new int[] {17, 1, 43});
		runs.add(new int[] {17, 0, 43});
		run.accept(new int[] {17, 0, 52}, new int[] {32, 0, 52});              // strong liquor: back round the east side to the mill
		run.accept(new int[] {32, 0, 51}, new int[] {32, 0, -1});
		run.accept(new int[] {31, 0, -1}, new int[] {15, 0, -1});
		run.accept(new int[] {15, 1, -1}, new int[] {13, 1, -1});
		run.accept(new int[] {13, 0, -1}, new int[] {1, 0, -1});
		run.accept(new int[] {17, 0, 11}, new int[] {17, 0, 14});              // process steam: the boiler's back to the digester and evaporator
		run.accept(new int[] {16, 0, 14}, new int[] {16, 0, 14});
		run.accept(new int[] {15, 0, 14}, new int[] {15, 1, 14});
		run.accept(new int[] {14, 1, 14}, new int[] {13, 1, 14});
		run.accept(new int[] {13, 0, 14}, new int[] {5, 0, 14});
		runs.add(new int[] {5, 0, 13});
		run.accept(new int[] {18, 0, 14}, new int[] {29, 0, 14});
		run.accept(new int[] {29, 0, 15}, new int[] {29, 0, 45});
		run.accept(new int[] {28, 0, 45}, new int[] {20, 0, 45});
		runs.add(new int[] {20, 0, 5});                                         // LPG: the bullet to the boiler
		runs.add(new int[] {6, 0, 64});                                         // LPG: the second bullet to the calciner
		run.accept(new int[] {21, 0, 49}, new int[] {20, 0, 49});              // caustic: the tank to the evaporator
		pipes(server, z, runs.toArray(new int[0][]));
		// hoppers
		put(server, 1, up + 4, z + 2, "minecraft:hopper");
		server.runCommand("item replace block 1 " + (up + 4) + " " + (z + 2) + " container.0 with wildcatter:bauxite 3");
		put(server, 19, up + 5, z + 42, "minecraft:hopper");
		server.runCommand("item replace block 19 " + (up + 5) + " " + (z + 42) + " container.0 with wildcatter:quicklime_bag 2");
		put(server, 2, up + 4, z + 68, "minecraft:hopper");
		server.runCommand("item replace block 2 " + (up + 4) + " " + (z + 68) + " container.0 with wildcatter:empty_bulk_bag 4");
		BlockPos bagsOut = new BlockPos(2, y, z + 68);
		put(server, 2, y, z + 68, "minecraft:hopper");
		BlockPos mudOut = new BlockPos(17, y, z + 36);
		put(server, 17, y, z + 36, "minecraft:hopper");
		// 480 V, the west line: two sets north-west, a busway down x -2 with a 500 kcmil stub to each box at x -1
		BlockPos[] westSets = {new BlockPos(-12, up, z - 8), new BlockPos(-4, up, z - 8)};
		for (BlockPos set : westSets) {
			genset(server, set, north, false);                                               // x -17..-12 and -9..-4, z-8..z-7
			put(server, set.getX() + 1, up, z - 8, "wildcatter:busway_industrial");
		}
		for (int x = -11; x <= -3; x++) {
			put(server, x, up, z - 9, "wildcatter:busway_industrial");
		}
		for (int zz = z - 9; zz <= z + 52; zz++) {
			put(server, -2, up, zz, "wildcatter:busway_industrial");
		}
		for (int zz : new int[] {0, 9, 14, 21, 32, 39, 52}) {
			put(server, -1, up, z + zz, "wildcatter:cable_industrial");
		}
		sizeCables(server, new BlockPos(-1, up, z), new BlockPos(-1, up, z + 52), WildcatterBlocks.CABLE_INDUSTRIAL, dev.wildcatter.energy.ConductorSize.KCMIL_500);
		// 480 V, the east side: one set north, a busway down x 14, stubs at x 15; the bagging station by cable round the south
		BlockPos eastSet = new BlockPos(11, up, z - 8);
		genset(server, eastSet, north, false);                                               // x 6-11, z-8..z-7
		for (int x = 12; x <= 14; x++) {
			put(server, x, up, z - 8, "wildcatter:busway_industrial");
		}
		for (int zz = z - 8; zz <= z + 72; zz++) {
			put(server, 14, up, zz, "wildcatter:busway_industrial");
		}
		for (int zz : new int[] {2, 21, 33, 41, 44}) {
			put(server, 15, up, z + zz, "wildcatter:cable_industrial");
		}
		put(server, 16, up, z + 41, "wildcatter:cable_industrial");
		put(server, 17, up, z + 41, "wildcatter:cable_industrial");
		for (int zz = z + 68; zz <= z + 72; zz++) {
			put(server, 1, up, zz, "wildcatter:cable_industrial");
		}
		for (int x = 2; x <= 13; x++) {
			put(server, x, up, z + 72, "wildcatter:cable_industrial");
		}
		// 4.16 kV: a set south-west through a backward unit substation, north up x -15 and east along z+62 to the calciner's box
		BlockPos mvSet = new BlockPos(-20, up, z + 76);
		genset(server, mvSet, north, false);                                                 // x -25..-20, z+76..z+77
		BlockPos mvOut = inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, mvSet, net.minecraft.core.Direction.EAST, false);
		for (int zz = z + 62; zz <= mvOut.getZ(); zz++) {
			put(server, mvOut.getX(), up, zz, "wildcatter:cable_mv");
		}
		for (int x = mvOut.getX() + 1; x <= -1; x++) {
			put(server, x, up, z + 62, "wildcatter:cable_mv");
		}
		exportBlueprint(server, "bayer_refinery", new BlockPos(-27, y, z - 16), new BlockPos(44, y + 24, z + 80));   // the library (docs/blueprints.md)
		fillTank(server, bullet, dev.wildcatter.fluid.Product.LPG, 20_000);
		fillTank(server, calcinerBullet, dev.wildcatter.fluid.Product.LPG, 20_000);
		fillTank(server, causticTank, dev.wildcatter.fluid.Product.CAUSTIC_SODA, 20_000);
		water(server, washer, boiler);
		for (BlockPos set : westSets) {
			startGenset(context, server, set, north, 2000);
		}
		startGenset(context, server, eastSet, north, 2000);
		startGenset(context, server, mvSet, north, 2000);
		BlockPos[][] parts = {{mill}, {digester}, {flash}, {settler}, {washer}, {press}, {polishing}, {precipitator}, {hydrate}, {calciner}, {evaporator}, {boiler}};
		String[] doing = {"grinding", "digesting", "flashing", "settling", "washing", "pressing", "filtering", "precipitating", "filtering", "calcining", "evaporating", "firing"};
		java.util.function.Supplier<String> all = () -> {
			StringBuilder out = new StringBuilder();
			for (BlockPos[] p : parts) {
				out.append(" | ").append(read(server, p[0].getX(), up, p[0].getZ()));
			}
			return out + " | causticizer " + read(server, causticizer.getX(), up, causticizer.getZ()) + " | bagger " + read(server, bagger.getX(), up, bagger.getZ());
		};
		boolean running = false;
		for (int i = 0; i < 40 && !running; i++) {
			context.waitTicks(40);
			water(server, washer, boiler);
			running = true;
			for (int k = 0; k < parts.length; k++) {
				running &= read(server, parts[k][0].getX(), up, parts[k][0].getZ()).contains(doing[k]);
			}
		}
		String state = all.get();
		System.out.println("[wildcatter-test] bayer: running" + state);
		for (int k = 0; k < parts.length; k++) {
			check(read(server, parts[k][0].getX(), up, parts[k][0].getZ()).contains(doing[k]), "every part should run, in flow order (" + doing[k] + "):" + state);
		}
		// almost a bag of alumina at the calciner, almost a block of cake at the press: the bag and the red mud drop out under them
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.CalcinerBlockEntity) srv.overworld().getBlockEntity(calciner)).addAlumina(Tuning.BULK_BAG_KG - 5);
			((dev.wildcatter.block.ResidueFilterPressBlockEntity) srv.overworld().getBlockEntity(press)).addCake(Tuning.RED_MUD_BLOCK_KG - 5);
			return null;
		});
		int bags = 0;
		int mud = 0;
		for (int i = 0; i < 30 && (bags == 0 || mud == 0); i++) {
			context.waitTicks(20);
			water(server, washer, boiler);
			bags = countIn(server, bagsOut, WildcatterItems.ALUMINA_BAG);
			mud = countIn(server, mudOut, WildcatterBlocks.RED_MUD.asItem());
		}
		check(bags > 0, "an alumina bag should drop under the bagging station: " + read(server, bagger.getX(), up, bagger.getZ()) + " | " + read(server, 0, up, z + 62));
		check(mud > 0, "a block of red mud should drop under the filter press: " + read(server, press.getX(), up, press.getZ()));
		put(server, 30, up, z + 70, "wildcatter:red_mud");
		check(!isAir(server, 30, up, z + 70), "red mud should place as a block");
		server.runCommand("tp @p 36 " + (up + 26) + " " + (z + 78) + " 135 30");   // aimed just before the shot: the player falls between
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-bayer-refinery");
		// the caustic shut off: the evaporator says so
		put(server, 21, up, z + 49, "minecraft:air");
		String evap = "";
		for (int i = 0; i < 10 && !evap.contains("NO CAUSTIC"); i++) {
			context.waitTicks(20);
			evap = read(server, evaporator.getX(), up, evaporator.getZ());
		}
		check(evap.contains("NO CAUSTIC"), "with its caustic shut off the evaporator should read NO CAUSTIC: " + evap);
		System.out.println("[wildcatter-test] bayer ok: " + bags + " alumina bag, " + mud + " red mud; " + evap);
	}

	/**
	 * Lime, AlF3 and chlorine (10b2, docs/multiblocks/alumina-plant.md): a lime kiln with a bagging station against it, an AlF3 reactor
	 * with another (HF from a seeded HF column, LPG from one bullet for both), and a chlor-alkali cell on its rectifier piping caustic
	 * to a stock tank and chlorine to a chlorine filler.
	 */
	private void alf3Lime(ClientGameTestContext context, TestServerContext server) {
		final int z = -5300;
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		net.minecraft.core.Direction south = net.minecraft.core.Direction.SOUTH;
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand("tp @p 10 " + (up + 20) + " " + (z + 30) + " 180 40");
		context.waitTicks(20);
		for (int zz = z - 16; zz <= z + 24; zz += 20) {
			server.runCommand("fill -20 " + (y - 1) + " " + zz + " 40 " + (y + 16) + " " + Math.min(zz + 19, z + 24) + " minecraft:air");
		}
		server.runCommand("fill -20 " + (y - 1) + " " + (z - 16) + " 40 " + (y - 1) + " " + (z + 24) + " minecraft:stone");
		BlockPos kiln = new BlockPos(0, up, z);
		placeBig(server, WildcatterBlocks.LIME_KILN, kiln, north);                         // x 0-2, z..z+2
		BlockPos limeBagger = new BlockPos(0, up, z + 3);
		placeBig(server, WildcatterBlocks.BAGGING_STATION, limeBagger, north);             // x 0-1, z+3..z+4
		BlockPos reactor = new BlockPos(0, up, z + 10);
		placeBig(server, WildcatterBlocks.ALF3_REACTOR, reactor, north);                   // x 0-2, z+10..z+12
		BlockPos alf3Bagger = new BlockPos(0, up, z + 13);
		placeBig(server, WildcatterBlocks.BAGGING_STATION, alf3Bagger, north);             // x 0-1, z+13..z+14
		BlockPos filler = new BlockPos(0, up, z + 16);
		placeBig(server, WildcatterBlocks.CHLORINE_FILLER, filler, north);                 // x 0-2, z+16..z+18
		BlockPos bullet = new BlockPos(4, up, z - 12);
		placeBig(server, WildcatterBlocks.LPG_BULLET, bullet, north);                      // x 4-6, z-12..z-1
		BlockPos column = new BlockPos(6, up, z + 10);
		placeBig(server, WildcatterBlocks.HF_COLUMN, column, north);                       // x 6-7, z+10..z+11
		BlockPos cell = new BlockPos(12, up, z);
		placeBig(server, WildcatterBlocks.CHLOR_ALKALI_CELL, cell, north);                 // x 12-14, z..z+5
		BlockPos rectifier = new BlockPos(17, up, z + 4);
		placeBig(server, WildcatterBlocks.RECTIFIER, rectifier, south);                    // x 15-17, z+1..z+4, its box east at x 18
		BlockPos tank = new BlockPos(7, up, z + 6);
		placeBig(server, WildcatterBlocks.STOCK_TANK, tank, north);                        // x 7-10, z+6..z+9
		java.util.List<int[]> runs = new java.util.ArrayList<>();
		for (int zz = -1; zz <= 10; zz++) {
			runs.add(new int[] {3, 0, zz});                                     // LPG: the bullet down x 3, past the kiln to the reactor
		}
		for (int x = 3; x <= 6; x++) {
			runs.add(new int[] {x, 0, 12});                                     // HF: the column's front to the reactor's east side
		}
		runs.add(new int[] {11, 0, 5});                                         // caustic: the cell's west side at its back, to the tank
		runs.add(new int[] {11, 0, 6});
		for (int zz = 6; zz <= 15; zz++) {
			runs.add(new int[] {13, 0, zz});                                    // chlorine: the cell's back, south and west to the filler
		}
		for (int x = 1; x <= 12; x++) {
			runs.add(new int[] {x, 0, 15});
		}
		pipes(server, z, runs.toArray(new int[0][]));
		put(server, 1, up + 14, z + 1, "minecraft:hopper");
		server.runCommand("item replace block 1 " + (up + 14) + " " + (z + 1) + " container.0 with wildcatter:limestone 2");
		put(server, 1, up + 10, z + 11, "minecraft:hopper");
		server.runCommand("item replace block 1 " + (up + 10) + " " + (z + 11) + " container.0 with wildcatter:hydrate_bag 1");
		put(server, 0, up + 4, z + 3, "minecraft:hopper");
		server.runCommand("item replace block 0 " + (up + 4) + " " + (z + 3) + " container.0 with wildcatter:empty_bulk_bag 2");
		put(server, 0, up + 4, z + 13, "minecraft:hopper");
		server.runCommand("item replace block 0 " + (up + 4) + " " + (z + 13) + " container.0 with wildcatter:empty_bulk_bag 2");
		put(server, 1, up + 4, z + 17, "minecraft:hopper");
		server.runCommand("item replace block 1 " + (up + 4) + " " + (z + 17) + " container.0 with wildcatter:empty_ton_container 2");
		put(server, 13, up + 3, z + 2, "minecraft:hopper");
		server.runCommand("item replace block 13 " + (up + 3) + " " + (z + 2) + " container.0 with wildcatter:halite 1");
		BlockPos limeOut = new BlockPos(0, y, z + 3);
		BlockPos alf3Out = new BlockPos(0, y, z + 13);
		BlockPos tonOut = new BlockPos(1, y, z + 17);
		for (BlockPos h : new BlockPos[] {limeOut, alf3Out, tonOut}) {
			put(server, h.getX(), h.getY(), h.getZ(), "minecraft:hopper");
		}
		// 480 V: a set north-west, a busway down x -2, a stub to each box at x -1
		BlockPos lvSet = new BlockPos(-12, up, z - 8);
		genset(server, lvSet, north, false);                                                 // x -17..-12, z-8..z-7
		put(server, -11, up, z - 8, "wildcatter:busway_industrial");
		for (int x = -11; x <= -3; x++) {
			put(server, x, up, z - 9, "wildcatter:busway_industrial");
		}
		for (int zz = z - 9; zz <= z + 16; zz++) {
			put(server, -2, up, zz, "wildcatter:busway_industrial");
		}
		for (int zz : new int[] {0, 3, 10, 13, 16}) {
			put(server, -1, up, z + zz, "wildcatter:cable_industrial");
		}
		// 4.16 kV for the rectifier: a set north-east through a backward unit substation, south and west along z+4 to its box
		BlockPos mvSet = new BlockPos(24, up, z - 6);
		genset(server, mvSet, north, false);                                                 // x 19-24, z-6..z-5
		BlockPos mvOut = inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, mvSet, net.minecraft.core.Direction.EAST, false);
		for (int zz = mvOut.getZ(); zz <= z + 4; zz++) {
			put(server, mvOut.getX(), up, zz, "wildcatter:cable_mv");
		}
		for (int x = 18; x < mvOut.getX(); x++) {
			put(server, x, up, z + 4, "wildcatter:cable_mv");
		}
		exportBlueprint(server, "alumina_plant", new BlockPos(-20, y, z - 16), new BlockPos(40, y + 16, z + 24));   // the library (docs/blueprints.md)
		fillTank(server, bullet, dev.wildcatter.fluid.Product.LPG, 20_000);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.HfColumnBlockEntity) srv.overworld().getBlockEntity(column)).addHf(10);
			return null;
		});
		water(server, cell);
		startGenset(context, server, lvSet, north, 2000);
		startGenset(context, server, mvSet, north, 2000);
		water(server, cell);
		context.waitTicks(200);
		String state = " | kiln " + read(server, 0, up, z) + " | reactor " + read(server, 0, up, z + 10) + " | cell " + read(server, 12, up, z) + " | rectifier "
				+ read(server, 17, up, z + 4) + " | filler " + read(server, 0, up, z + 16) + " | tank " + read(server, 7, up, z + 6);
		System.out.println("[wildcatter-test] alf3Lime: running" + state);
		check(read(server, 0, up, z).contains("burning"), "the lime kiln should burn limestone: " + state);
		check(read(server, 0, up, z + 10).contains("fluorinating"), "the AlF3 reactor should make AlF3: " + state);
		check(read(server, 12, up, z).contains("kg/h of chlorine"), "the chlor-alkali cell should run: " + state);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.LimeKilnBlockEntity) srv.overworld().getBlockEntity(kiln)).addQuicklime(Tuning.BULK_BAG_KG - 1);
			((dev.wildcatter.block.Alf3ReactorBlockEntity) srv.overworld().getBlockEntity(reactor)).addAlf3(Tuning.BULK_BAG_KG - 0.05);
			((dev.wildcatter.block.ChlorineFillerBlockEntity) srv.overworld().getBlockEntity(filler)).addChlorine(Tuning.CL2_TON_KG - 0.1);
			((dev.wildcatter.block.HfColumnBlockEntity) srv.overworld().getBlockEntity(column)).addHf(10);
			return null;
		});
		int lime = 0, alf3 = 0, tons = 0;
		for (int i = 0; i < 60 && (lime == 0 || alf3 == 0 || tons == 0); i++) {
			context.waitTicks(20);
			water(server, cell);
			lime = countIn(server, limeOut, WildcatterItems.QUICKLIME_BAG);
			alf3 = countIn(server, alf3Out, WildcatterItems.ALF3_BAG);
			tons = countIn(server, tonOut, WildcatterItems.CHLORINE_TON_CONTAINER);
		}
		state = " | kiln " + read(server, 0, up, z) + " | lime bagger " + read(server, 0, up, z + 3) + " | reactor " + read(server, 0, up, z + 10) + " | AlF3 bagger "
				+ read(server, 0, up, z + 13) + " | cell " + read(server, 12, up, z) + " | filler " + read(server, 0, up, z + 16) + " | tank " + read(server, 7, up, z + 6);
		check(lime > 0, "a quicklime bag should drop under the kiln's bagging station: " + state);
		check(alf3 > 0, "an AlF3 bag should drop under the reactor's bagging station: " + state);
		check(tons > 0, "a chlorine ton container should drop under the filler: " + state);
		double caustic = server.computeOnServer(srv -> srv.overworld().getBlockEntity(tank) instanceof dev.wildcatter.block.StorageTankBlockEntity t
				&& t.product() == dev.wildcatter.fluid.Product.CAUSTIC_SODA ? t.amount() : 0.0);
		check(caustic > 0, "the cell's caustic should come into the tank (a few kg an hour of a 75 kg/h rate): " + caustic + state);
		server.runCommand("tp @p 20 " + (up + 14) + " " + (z + 26) + " 145 30");   // aimed just before the shot: the player falls between
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-lime-alf3");
		System.out.println("[wildcatter-test] alf3Lime ok: " + lime + " quicklime bag, " + alf3 + " AlF3 bag, " + tons + " ton container, " + String.format("%.2f", caustic) + " kg of caustic in the tank" + state);
	}

	/**
	 * The delayed coker and calciner (10b3a, docs/multiblocks/delayed-coker.md). The coke pit at x 0-5 with two drums on its east half;
	 * the heater east of the pit, turned to face east so its box is on its north side, against both drums; the fractionator east of
	 * the heater, a stock tank of residue at its front, tanks on its gas oil and naphtha and a flare on its coker gas. South-east, the
	 * calcining kiln, its cooler and a bagging station in a line. LPG bullets fire the heater and the kiln. A diesel set's 480 V on a
	 * busway down x 18; a second, through a backward unit substation, the pit's 4.16 kV jet pump.
	 */
	private void coker(ClientGameTestContext context, TestServerContext server) {
		final int z = -5500;
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		server.runCommand("effect give @p minecraft:slow_falling 1200 0 true");
		server.runCommand("tp @p 10 " + (up + 30) + " " + (z + 40) + " 160 40");
		context.waitTicks(20);
		for (int zz = z - 16; zz <= z + 50; zz += 13) {   // /fill takes at most 32,768 blocks
			server.runCommand("fill -27 " + (y - 1) + " " + zz + " 30 " + (y + 24) + " " + Math.min(zz + 12, z + 50) + " minecraft:air");
		}
		for (int zz = z - 16; zz <= z + 50; zz += 40) {
			server.runCommand("fill -27 " + (y - 1) + " " + zz + " 30 " + (y - 1) + " " + Math.min(zz + 39, z + 50) + " minecraft:stone");
		}
		BlockPos pit = new BlockPos(0, up, z);
		placeBig(server, WildcatterBlocks.COKE_PIT, pit, north);                            // x 0-5, z..z+7
		BlockPos drumA = new BlockPos(3, up + 4, z);
		placeBig(server, WildcatterBlocks.COKE_DRUM, drumA, north);                         // x 3-5, z..z+2, on the pit
		BlockPos drumB = new BlockPos(3, up + 4, z + 3);
		placeBig(server, WildcatterBlocks.COKE_DRUM, drumB, north);                         // x 3-5, z+3..z+5
		BlockPos heater = new BlockPos(11, up, z + 1);
		placeBig(server, WildcatterBlocks.COKER_HEATER, heater, net.minecraft.core.Direction.EAST);   // x 6-11, z+1..z+4; box north, fuel south
		BlockPos column = new BlockPos(12, up, z - 1);
		placeBig(server, WildcatterBlocks.COKER_FRACTIONATOR, column, north);               // x 12-14, z-1..z+1
		BlockPos residue = new BlockPos(12, up, z - 6);
		placeBig(server, WildcatterBlocks.STOCK_TANK, residue, north);                      // x 12-15, z-6..z-3
		BlockPos dieselTank = new BlockPos(10, up, z + 6);
		placeBig(server, WildcatterBlocks.STOCK_TANK, dieselTank, north);                   // x 10-13, z+6..z+9
		BlockPos gasolineTank = new BlockPos(14, up, z + 6);
		placeBig(server, WildcatterBlocks.STOCK_TANK, gasolineTank, north);                 // x 14-17, z+6..z+9
		BlockPos bullet = new BlockPos(6, up, z + 6);
		placeBig(server, WildcatterBlocks.LPG_BULLET, bullet, north);                       // x 6-8, z+6..z+17
		placeBig(server, WildcatterBlocks.FLARE_STACK, new BlockPos(16, up, z), north);     // the coker gas no one takes
		BlockPos kiln = new BlockPos(20, up, z);
		placeBig(server, WildcatterBlocks.COKE_KILN, kiln, north);                          // x 20-22, z..z+29
		BlockPos cooler = new BlockPos(20, up, z + 30);
		placeBig(server, WildcatterBlocks.COKE_COOLER, cooler, north);                      // x 20-21, z+30..z+41
		BlockPos bagger = new BlockPos(20, up, z + 42);
		placeBig(server, WildcatterBlocks.BAGGING_STATION, bagger, north);                  // x 20-21, z+42..z+43
		BlockPos kilnBullet = new BlockPos(24, up, z + 10);
		placeBig(server, WildcatterBlocks.LPG_BULLET, kilnBullet, north);                   // x 24-26, z+10..z+21
		java.util.List<int[]> runs = new java.util.ArrayList<>();
		runs.add(new int[] {13, 0, -2});                                        // residue: the tank to the fractionator's front
		for (int zz = 2; zz <= 5; zz++) {
			runs.add(new int[] {12, 0, zz});                                    // gas oil: x 12 south to its tank
			runs.add(new int[] {14, 0, zz});                                    // naphtha: x 14 south to its tank
		}
		runs.add(new int[] {15, 0, 0});                                         // coker gas: the fractionator's east side to the flare
		runs.add(new int[] {8, 0, 5});                                          // LPG: the bullet to the heater's fuel side
		runs.add(new int[] {23, 0, 12});                                        // LPG: the second bullet to the kiln
		pipes(server, z, runs.toArray(new int[0][]));
		put(server, 21, up + 3, z + 1, "minecraft:hopper");
		server.runCommand("item replace block 21 " + (up + 3) + " " + (z + 1) + " container.0 with wildcatter:green_petroleum_coke 1");
		put(server, 20, up + 4, z + 42, "minecraft:hopper");
		server.runCommand("item replace block 20 " + (up + 4) + " " + (z + 42) + " container.0 with wildcatter:empty_bulk_bag 2");
		BlockPos cokeOut = new BlockPos(1, y, z + 4);
		put(server, 1, y, z + 4, "minecraft:hopper");
		BlockPos bagsOut = new BlockPos(20, y, z + 42);
		put(server, 20, y, z + 42, "minecraft:hopper");
		// 480 V: a set north-west, a busway along z-9 and down x 18
		BlockPos lvSet = new BlockPos(-4, up, z - 8);
		genset(server, lvSet, north, false);                                                 // x -9..-4, z-8..z-7
		put(server, -3, up, z - 8, "wildcatter:busway_industrial");
		for (int x = -3; x <= 18; x++) {
			put(server, x, up, z - 9, "wildcatter:busway_industrial");
		}
		for (int zz = z - 9; zz <= z + 42; zz++) {
			put(server, 18, up, zz, "wildcatter:busway_industrial");
		}
		for (int zz : new int[] {0, 30, 42}) {
			put(server, 19, up, z + zz, "wildcatter:cable_industrial");         // the kiln, cooler and bagging station
		}
		for (int zz = z - 8; zz <= z; zz++) {
			put(server, 11, up, zz, "wildcatter:cable_industrial");             // the fractionator (at z-1) and the heater (at z)
		}
		for (int zz = z - 8; zz <= z - 1; zz++) {
			put(server, 2, up, zz, "wildcatter:cable_industrial");              // up to the drums on the pit
		}
		for (int yy = up + 1; yy <= up + 4; yy++) {
			put(server, 2, yy, z - 1, "wildcatter:cable_industrial");
		}
		for (int zz = z; zz <= z + 3; zz++) {
			put(server, 2, up + 4, zz, "wildcatter:cable_industrial");
		}
		// 4.16 kV: a 1 MW gas set south-west through a backward unit substation, north and east to the pit's box at x -1. The 400 kW
		// jet pump starts direct on line, and since every start goes with V² (10d1) a 500 kW diesel set sags and trips on it.
		BlockPos mvSet = new BlockPos(-20, up, z + 20);
		genset(server, mvSet, north, true);                                                  // x -27..-20, z+20..z+21
		BlockPos mvOut = inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, mvSet, net.minecraft.core.Direction.EAST, false);
		for (int zz = z; zz <= mvOut.getZ(); zz++) {
			put(server, mvOut.getX(), up, zz, "wildcatter:cable_mv");
		}
		for (int x = mvOut.getX() + 1; x <= -1; x++) {
			put(server, x, up, z, "wildcatter:cable_mv");
		}
		exportBlueprint(server, "coker", new BlockPos(-27, y, z - 16), new BlockPos(30, y + 24, z + 50));   // the library (docs/blueprints.md)
		fillTank(server, residue, dev.wildcatter.fluid.Product.RESIDUE, 40_000);
		fillTank(server, bullet, dev.wildcatter.fluid.Product.LPG, 20_000);
		fillTank(server, kilnBullet, dev.wildcatter.fluid.Product.LPG, 20_000);
		water(server, pit);
		startGenset(context, server, lvSet, north, 2000);
		context.waitTicks(30);
		server.computeOnServer(srv -> {
			panel(srv, mvSet).setGasForTest(200);   // gas already in its line: no gas plant in this scene
			panel(srv, mvSet).setWantRun(true);
			return null;
		});
		context.waitTicks(240);
		check(read(server, mvSet.getX(), up, mvSet.getZ()).contains("breaker CLOSED"), "the gas set should be running: " + read(server, mvSet.getX(), up, mvSet.getZ()));
		BlockPos[] parts = {column, heater, kiln};
		String[] doing = {"fractionating", "coking", "calcining"};
		java.util.function.Supplier<String> all = () -> " | fractionator " + read(server, column.getX(), up, column.getZ()) + " | heater " + read(server, heater.getX(), up, heater.getZ())
				+ " | drum A " + read(server, drumA.getX(), drumA.getY(), drumA.getZ()) + " | drum B " + read(server, drumB.getX(), drumB.getY(), drumB.getZ()) + " | pit "
				+ read(server, 0, up, z) + " | kiln " + read(server, kiln.getX(), up, kiln.getZ()) + " | cooler " + read(server, cooler.getX(), up, cooler.getZ());
		boolean running = false;
		for (int i = 0; i < 30 && !running; i++) {
			context.waitTicks(40);
			running = true;
			for (int k = 0; k < parts.length; k++) {
				running &= read(server, parts[k].getX(), up, parts[k].getZ()).contains(doing[k]);
			}
		}
		String state = all.get();
		System.out.println("[wildcatter-test] coker: running" + state);
		for (int k = 0; k < parts.length; k++) {
			check(read(server, parts[k].getX(), up, parts[k].getZ()).contains(doing[k]), "the coker should run (" + doing[k] + "):" + state);
		}
		check(state.contains("FILLING"), "one drum should be filling:" + state);
		// a little coke in the drum that is not filling and almost a piece in the pit; almost a bag in the cooler
		server.computeOnServer(srv -> {
			for (BlockPos d : new BlockPos[] {drumA, drumB}) {
				if (srv.overworld().getBlockEntity(d) instanceof dev.wildcatter.block.CokeDrumBlockEntity drum && !drum.filling()) {
					drum.addCoke(20);
				}
			}
			((dev.wildcatter.block.CokePitBlockEntity) srv.overworld().getBlockEntity(pit)).addCoke(Tuning.GREEN_COKE_KG - 5);
			((dev.wildcatter.block.CokeCoolerBlockEntity) srv.overworld().getBlockEntity(cooler)).addCoke(Tuning.BULK_BAG_KG - 5);
			return null;
		});
		int coke = 0;
		int bags = 0;
		for (int i = 0; i < 30 && (coke == 0 || bags == 0); i++) {
			context.waitTicks(20);
			water(server, pit);
			coke = countIn(server, cokeOut, WildcatterItems.GREEN_COKE);
			bags = countIn(server, bagsOut, WildcatterItems.CALCINED_COKE_BAG);
		}
		state = all.get();
		check(coke > 0, "a piece of green coke should drop under the pit:" + state);
		check(bags > 0, "a bag of calcined coke should drop under the bagging station:" + state + " | bagger " + read(server, bagger.getX(), up, bagger.getZ()));
		java.util.function.Function<BlockPos, Double> tank = at -> server.computeOnServer(srv -> srv.overworld().getBlockEntity(at) instanceof dev.wildcatter.block.StorageTankBlockEntity t
				? t.amount() : 0.0);
		double diesel = tank.apply(dieselTank);
		double gasoline = tank.apply(gasolineTank);
		check(diesel > 0 && gasoline > 0, "gas oil and naphtha should come into their tanks: " + diesel + " L, " + gasoline + " L" + state);
		server.runCommand("tp @p 34 " + (up + 26) + " " + (z + 46) + " 135 30");   // aimed just before the shot: the player falls between
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-coker");
		System.out.println("[wildcatter-test] coker ok: " + coke + " green coke, " + bags + " calcined coke bag, " + String.format("%.0f L of gas oil, %.0f L of naphtha", diesel, gasoline) + state);
	}

	/**
	 * The coke ovens and tar still (10b3a, docs/multiblocks/coke-ovens.md). The battery at x 0-9, the coal tower at its north end, the
	 * pusher machine on its west side and the quench tower (turned to face south, so its box is on its east side) on its east. Its raw
	 * gas goes south to the by-product plant, which flares its coke oven gas and pipes its tar east to the tar still; the still's pitch
	 * and tar oils go to stock tanks. A diesel set's 480 V on a busway along z-9 and down x 20 for the battery's parts, a second set for
	 * the still, and a third through a backward unit substation for the exhauster's 4.16 kV.
	 */
	private void cokeOvens(ClientGameTestContext context, TestServerContext server) {
		final int z = -5700;
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		server.runCommand("effect give @p minecraft:slow_falling 1200 0 true");
		server.runCommand("tp @p 10 " + (up + 30) + " " + (z + 45) + " 170 40");
		context.waitTicks(20);
		for (int zz = z - 16; zz <= z + 40; zz += 13) {
			server.runCommand("fill -34 " + (y - 1) + " " + zz + " 30 " + (y + 24) + " " + Math.min(zz + 12, z + 40) + " minecraft:air");
		}
		for (int zz = z - 16; zz <= z + 40; zz += 40) {
			server.runCommand("fill -34 " + (y - 1) + " " + zz + " 30 " + (y - 1) + " " + Math.min(zz + 39, z + 40) + " minecraft:stone");
		}
		BlockPos battery = new BlockPos(0, up, z);
		placeBig(server, WildcatterBlocks.COKE_OVEN_BATTERY, battery, north);               // x 0-9, z..z+15
		BlockPos tower = new BlockPos(3, up, z - 4);
		placeBig(server, WildcatterBlocks.COAL_TOWER, tower, north);                        // x 3-6, z-4..z-1
		BlockPos pusher = new BlockPos(-4, up, z + 1);
		placeBig(server, WildcatterBlocks.PUSHER_MACHINE, pusher, north);                   // x -4..-1, z+1..z+16
		BlockPos quench = new BlockPos(14, up, z + 9);
		placeBig(server, WildcatterBlocks.COKE_QUENCH_TOWER, quench, net.minecraft.core.Direction.SOUTH);   // x 10-14, z+5..z+9; box east
		BlockPos plant = new BlockPos(2, up, z + 18);
		placeBig(server, WildcatterBlocks.BYPRODUCT_PLANT, plant, north);                   // x 2-6, z+18..z+25
		placeBig(server, WildcatterBlocks.FLARE_STACK, new BlockPos(4, up, z + 27), north); // the coke oven gas no one takes
		BlockPos still = new BlockPos(12, up, z + 25);
		placeBig(server, WildcatterBlocks.TAR_STILL, still, north);                         // x 12-14, z+25..z+27
		BlockPos bullet = new BlockPos(16, up, z + 14);
		placeBig(server, WildcatterBlocks.LPG_BULLET, bullet, north);                       // x 16-18, z+14..z+25
		BlockPos pitchTank = new BlockPos(9, up, z + 29);
		placeBig(server, WildcatterBlocks.STOCK_TANK, pitchTank, north);                    // x 9-12, z+29..z+32
		BlockPos oilTank = new BlockPos(14, up, z + 29);
		placeBig(server, WildcatterBlocks.STOCK_TANK, oilTank, north);                      // x 14-17, z+29..z+32
		java.util.List<int[]> runs = new java.util.ArrayList<>();
		runs.add(new int[] {3, 0, 16});                                         // raw gas: the battery's back to the by-product plant
		runs.add(new int[] {3, 0, 17});
		runs.add(new int[] {4, 0, 26});                                         // coke oven gas to the flare
		for (int x = 7; x <= 12; x++) {
			runs.add(new int[] {x, 0, 24});                                     // tar: the plant's east side to the still's front
		}
		runs.add(new int[] {15, 0, 25});                                        // LPG: the bullet to the still
		runs.add(new int[] {12, 0, 28});                                        // pitch to its tank
		runs.add(new int[] {14, 0, 28});                                        // tar oils to theirs
		pipes(server, z, runs.toArray(new int[0][]));
		put(server, 4, up + 12, z - 3, "minecraft:hopper");
		server.runCommand("item replace block 4 " + (up + 12) + " " + (z - 3) + " container.0 with minecraft:coal_block 6");
		BlockPos cokeOut = new BlockPos(12, y, z + 7);
		put(server, 12, y, z + 7, "minecraft:hopper");
		// 480 V for the battery's parts: a set north-west, a busway along z-9 and down x 20
		BlockPos lvSet = new BlockPos(-12, up, z - 8);
		BlockPos lvSet2 = new BlockPos(-20, up, z - 8);
		for (BlockPos set : new BlockPos[] {lvSet, lvSet2}) {
			genset(server, set, north, false);                                               // x -17..-12 and -25..-20, z-8..z-7
			put(server, set.getX() + 1, up, z - 8, "wildcatter:busway_industrial");
		}
		for (int x = -19; x <= 20; x++) {
			put(server, x, up, z - 9, "wildcatter:busway_industrial");
		}
		for (int zz = z - 9; zz <= z + 9; zz++) {
			put(server, 20, up, zz, "wildcatter:busway_industrial");
		}
		for (int x = 15; x <= 19; x++) {
			put(server, x, up, z + 9, "wildcatter:cable_industrial");           // the quench tower's box
		}
		for (int zz = z - 8; zz <= z; zz++) {
			put(server, -1, up, zz, "wildcatter:cable_industrial");             // the battery's box
		}
		for (int zz = z - 8; zz <= z - 4; zz++) {
			put(server, 2, up, zz, "wildcatter:cable_industrial");              // the coal tower's
		}
		for (int zz = z - 8; zz <= z + 1; zz++) {
			put(server, -5, up, zz, "wildcatter:cable_industrial");             // the pusher's
		}
		// 480 V for the still: a set south, a cable north up x 8 and east along z+25 to its box at x 11
		BlockPos stillSet = new BlockPos(7, up, z + 35);
		genset(server, stillSet, north, false);                                              // x 2-7, z+35..z+36
		put(server, 8, up, z + 35, "wildcatter:busway_industrial");
		for (int zz = z + 25; zz <= z + 34; zz++) {
			put(server, 8, up, zz, "wildcatter:cable_industrial");
		}
		for (int x = 9; x <= 11; x++) {
			put(server, x, up, z + 25, "wildcatter:cable_industrial");
		}
		// 4.16 kV for the exhauster: two sets west, paralleled on a short busway, through a backward unit substation, east along z+18
		BlockPos mvSet = new BlockPos(-28, up, z + 18);
		BlockPos mvSet2 = new BlockPos(-28, up, z + 22);
		for (BlockPos set : new BlockPos[] {mvSet, mvSet2}) {
			genset(server, set, north, false);                                               // x -33..-28, z+18..z+19 and z+22..z+23
		}
		for (int zz = z + 18; zz <= z + 22; zz++) {
			put(server, -27, up, zz, "wildcatter:busway_industrial");
		}
		BlockPos mvOut = inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, new BlockPos(-27, up, z + 18), net.minecraft.core.Direction.EAST, false);
		for (int x = mvOut.getX(); x <= 1; x++) {
			put(server, x, up, z + 18, "wildcatter:cable_mv");
		}
		exportBlueprint(server, "coke_ovens", new BlockPos(-34, y, z - 16), new BlockPos(30, y + 24, z + 40));   // the library (docs/blueprints.md)
		fillTank(server, bullet, dev.wildcatter.fluid.Product.LPG, 20_000);
		water(server, quench);
		startGenset(context, server, mvSet, north, 2000);       // the exhauster's sets first, so they are up before there is gas
		startGenset(context, server, mvSet2, north, 2000);
		startGenset(context, server, stillSet, north, 2000);
		startGenset(context, server, lvSet, north, 2000);
		startGenset(context, server, lvSet2, north, 2000);
		java.util.function.Supplier<String> all = () -> " | battery " + read(server, 0, up, z) + " | tower " + read(server, tower.getX(), up, tower.getZ()) + " | quench "
				+ read(server, quench.getX(), up, quench.getZ()) + " | by-product plant " + read(server, plant.getX(), up, plant.getZ()) + " | still "
				+ read(server, still.getX(), up, still.getZ());
		boolean running = false;
		for (int i = 0; i < 30 && !running; i++) {
			context.waitTicks(40);
			water(server, quench);
			running = read(server, 0, up, z).contains("coking") && read(server, plant.getX(), up, plant.getZ()).contains("cleaning")
					&& read(server, still.getX(), up, still.getZ()).contains("distilling");
		}
		String state = all.get();
		System.out.println("[wildcatter-test] cokeOvens: running" + state);
		check(read(server, 0, up, z).contains("coking"), "the battery should coke coal:" + state);
		check(read(server, plant.getX(), up, plant.getZ()).contains("cleaning") && state.contains("coke oven gas"), "the by-product plant should clean the gas and hold coke oven gas:" + state);
		check(read(server, still.getX(), up, still.getZ()).contains("distilling"), "the tar still should distil tar:" + state);
		int coke = 0;
		for (int i = 0; i < 60 && coke == 0; i++) {
			context.waitTicks(20);
			water(server, quench);
			coke = countIn(server, cokeOut, WildcatterItems.MET_COKE);
		}
		check(coke > 0, "metallurgical coke should drop under the quench tower:" + all.get());
		double pitch = server.computeOnServer(srv -> srv.overworld().getBlockEntity(pitchTank) instanceof dev.wildcatter.block.StorageTankBlockEntity t
				&& t.product() == dev.wildcatter.fluid.Product.PITCH ? t.amount() : 0.0);
		check(pitch > 0, "pitch should come into its tank: " + pitch + all.get());
		server.runCommand("tp @p 30 " + (up + 24) + " " + (z + 40) + " 135 30");
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-coke-ovens");
		// the exhauster unpowered: the battery's gas has nowhere to go
		put(server, 0, up, z + 18, "minecraft:air");
		String battery0 = "";
		for (int i = 0; i < 20 && !battery0.contains("nowhere to go"); i++) {
			context.waitTicks(20);
			water(server, quench);
			battery0 = read(server, 0, up, z);
		}
		check(battery0.contains("nowhere to go"), "with the exhauster unpowered the battery should stop, its gas with nowhere to go: " + battery0);
		System.out.println("[wildcatter-test] cokeOvens ok: " + coke + " coke, " + String.format("%.1f kg of pitch", pitch) + "; " + battery0);
	}

	/**
	 * The anode plant (10b3a, docs/multiblocks/anode-plant.md). The paste plant with a stock tank of pitch on its east side and the
	 * forming press against its back; the ring baking furnace east, an LPG bullet on its fuel side; the rodding shop south of the
	 * press. A set's 480 V for the press and the rodding shop from the west, another for the furnace from the north, and a third through
	 * a backward unit substation for the paste plant's 4.16 kV.
	 */
	private void anodes(ClientGameTestContext context, TestServerContext server) {
		final int z = -5900;
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		server.runCommand("effect give @p minecraft:slow_falling 1200 0 true");
		server.runCommand("tp @p 10 " + (up + 26) + " " + (z + 40) + " 170 40");
		context.waitTicks(20);
		for (int zz = z - 22; zz <= z + 32; zz += 13) {
			server.runCommand("fill -27 " + (y - 1) + " " + zz + " 30 " + (y + 16) + " " + Math.min(zz + 12, z + 32) + " minecraft:air");
		}
		for (int zz = z - 22; zz <= z + 32; zz += 40) {
			server.runCommand("fill -27 " + (y - 1) + " " + zz + " 30 " + (y - 1) + " " + Math.min(zz + 39, z + 32) + " minecraft:stone");
		}
		BlockPos paste = new BlockPos(0, up, z);
		placeBig(server, WildcatterBlocks.PASTE_PLANT, paste, north);                       // x 0-4, z..z+5
		BlockPos press = new BlockPos(0, up, z + 6);
		placeBig(server, WildcatterBlocks.FORMING_PRESS, press, north);                     // x 0-3, z+6..z+9
		BlockPos pitchTank = new BlockPos(6, up, z + 1);
		placeBig(server, WildcatterBlocks.STOCK_TANK, pitchTank, north);                    // x 6-9, z+1..z+4
		BlockPos furnace = new BlockPos(14, up, z);
		placeBig(server, WildcatterBlocks.BAKING_FURNACE, furnace, north);                  // x 14-23, z..z+23
		BlockPos bullet = new BlockPos(25, up, z + 2);
		placeBig(server, WildcatterBlocks.LPG_BULLET, bullet, north);                       // x 25-27, z+2..z+13
		BlockPos shop = new BlockPos(0, up, z + 14);
		placeBig(server, WildcatterBlocks.RODDING_SHOP, shop, north);                       // x 0-3, z+14..z+25
		pipes(server, z, new int[][] {{5, 0, 2}, {24, 0, 3}});                // pitch to the paste plant; LPG to the furnace
		put(server, 2, up + 10, z + 2, "minecraft:hopper");
		server.runCommand("item replace block 2 " + (up + 10) + " " + (z + 2) + " container.0 with wildcatter:calcined_coke_bag 2");
		server.runCommand("item replace block 2 " + (up + 10) + " " + (z + 2) + " container.1 with wildcatter:crushed_butts 2");
		put(server, 16, up + 5, z + 2, "minecraft:hopper");
		server.runCommand("item replace block 16 " + (up + 5) + " " + (z + 2) + " container.0 with wildcatter:green_anode 2");
		put(server, 1, up + 5, z + 15, "minecraft:hopper");
		server.runCommand("item replace block 1 " + (up + 5) + " " + (z + 15) + " container.0 with wildcatter:spent_anode 1");
		server.runCommand("item replace block 1 " + (up + 5) + " " + (z + 15) + " container.1 with minecraft:iron_ingot 2");
		BlockPos greenOut = new BlockPos(1, y, z + 7);
		BlockPos bakedOut = new BlockPos(16, y, z + 5);
		BlockPos shopOut = new BlockPos(1, y, z + 16);
		for (BlockPos h : new BlockPos[] {greenOut, bakedOut, shopOut}) {
			put(server, h.getX(), h.getY(), h.getZ(), "minecraft:hopper");
		}
		// 480 V for the press and the rodding shop: a set west, a busway along z+10 and down x -4, stubs to their boxes at x -1
		BlockPos lvSet = new BlockPos(-12, up, z + 10);
		genset(server, lvSet, north, false);                                                 // x -17..-12, z+10..z+11
		for (int x = -11; x <= -4; x++) {
			put(server, x, up, z + 10, "wildcatter:busway_industrial");
		}
		for (int zz = z + 6; zz <= z + 14; zz++) {
			put(server, -4, up, zz, "wildcatter:busway_industrial");
		}
		for (int zz : new int[] {6, 14}) {
			for (int x = -3; x <= -1; x++) {
				put(server, x, up, z + zz, "wildcatter:cable_industrial");
			}
		}
		sizeCables(server, new BlockPos(-3, up, z + 6), new BlockPos(-1, up, z + 14), WildcatterBlocks.CABLE_INDUSTRIAL, dev.wildcatter.energy.ConductorSize.KCMIL_500);
		// 480 V for the furnace: a set north, a cable down x 13 to its box
		BlockPos furnaceSet = new BlockPos(12, up, z - 8);
		genset(server, furnaceSet, north, false);                                            // x 7-12, z-8..z-7
		put(server, 13, up, z - 8, "wildcatter:busway_industrial");
		for (int zz = z - 7; zz <= z; zz++) {
			put(server, 13, up, zz, "wildcatter:cable_industrial");
		}
		// 4.16 kV for the paste plant: a set north-west through a backward unit substation, east along z-16 and down x -1
		BlockPos mvSet = new BlockPos(-20, up, z - 16);
		genset(server, mvSet, north, false);                                                 // x -25..-20, z-16..z-15
		BlockPos mvOut = inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, mvSet, net.minecraft.core.Direction.EAST, false);
		for (int x = mvOut.getX(); x <= -1; x++) {
			put(server, x, up, z - 16, "wildcatter:cable_mv");
		}
		for (int zz = z - 15; zz <= z; zz++) {
			put(server, -1, up, zz, "wildcatter:cable_mv");
		}
		exportBlueprint(server, "anode_plant", new BlockPos(-27, y, z - 22), new BlockPos(30, y + 16, z + 32));   // the library (docs/blueprints.md)
		fillTank(server, pitchTank, dev.wildcatter.fluid.Product.PITCH, 5_000);
		fillTank(server, bullet, dev.wildcatter.fluid.Product.LPG, 20_000);
		startGenset(context, server, lvSet, north, 2000);
		startGenset(context, server, furnaceSet, north, 2000);
		startGenset(context, server, mvSet, north, 2000);
		java.util.function.Supplier<String> all = () -> " | paste plant " + read(server, 0, up, z) + " | press " + read(server, 0, up, z + 6) + " | furnace "
				+ read(server, furnace.getX(), up, furnace.getZ()) + " | rodding shop " + read(server, 0, up, z + 14);
		boolean running = false;
		for (int i = 0; i < 30 && !running; i++) {
			context.waitTicks(40);
			running = read(server, 0, up, z).contains("mixing") && read(server, furnace.getX(), up, furnace.getZ()).contains("firing");
		}
		String state = all.get();
		System.out.println("[wildcatter-test] anodes: running" + state);
		check(read(server, 0, up, z).contains("mixing"), "the paste plant should mix paste:" + state);
		check(read(server, furnace.getX(), up, furnace.getZ()).contains("firing"), "the furnace should fire:" + state);
		// almost an anode's paste, almost an anode fired, and the rodding shop's minute mostly done
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.PastePlantBlockEntity) srv.overworld().getBlockEntity(paste)).addPaste(Tuning.GREEN_ANODE_KG);
			((dev.wildcatter.block.BakingFurnaceBlockEntity) srv.overworld().getBlockEntity(furnace)).addBaking(0.995);
			((dev.wildcatter.block.RoddingShopBlockEntity) srv.overworld().getBlockEntity(shop)).addWork(Tuning.RODDING_SECONDS - 2);
			return null;
		});
		int green = 0, baked = 0, butts = 0;
		for (int i = 0; i < 30 && (green == 0 || baked == 0 || butts == 0); i++) {
			context.waitTicks(20);
			green = countIn(server, greenOut, WildcatterItems.GREEN_ANODE);
			baked = countIn(server, bakedOut, WildcatterItems.BAKED_ANODE);
			butts = countIn(server, shopOut, WildcatterItems.CRUSHED_BUTTS);
		}
		state = all.get();
		check(green > 0, "the forming press should drop a green anode:" + state);
		check(baked > 0, "the furnace should drop a baked anode:" + state);
		check(butts > 0, "the rodding shop should strip the spent anode into crushed butts:" + state);
		// the baked anode carried to the rodding shop's hopper, which rods it on the stripped rod
		server.runCommand("item replace block 1 " + (up + 5) + " " + (z + 15) + " container.2 with wildcatter:baked_anode 1");
		int rodded = 0;
		for (int i = 0; i < 30 && rodded == 0; i++) {
			context.waitTicks(20);
			if (i == 2) {
				server.computeOnServer(srv -> {
					((dev.wildcatter.block.RoddingShopBlockEntity) srv.overworld().getBlockEntity(shop)).addWork(Tuning.RODDING_SECONDS - 2);
					return null;
				});
			}
			rodded = countIn(server, shopOut, WildcatterItems.RODDED_ANODE);
		}
		state = all.get();
		check(rodded > 0, "the rodding shop should drop a rodded anode:" + state);
		server.runCommand("tp @p 32 " + (up + 20) + " " + (z + 36) + " 140 30");
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-anode-plant");
		System.out.println("[wildcatter-test] anodes ok: " + green + " green, " + baked + " baked, " + butts + " crushed butts, " + rodded + " rodded" + state);
	}

	/**
	 * The potline (10b3b, docs/multiblocks/potline.md). South-west: the peaker's chain, an aeroderivative through its GSU 60, its 138 kV
	 * run north and east to a yard of 4 rectiformers along x 44. East of them two rows of 5 pots: row 1 faces north (current east) at
	 * z-13..z-2, row 2 faces south (current west) at z+2..z+13, a crossover bus along x 90 and the return along z+8 and x 53. Row 1's
	 * duct is piped from its west end to the fume treatment centre north of it, with a main fan on a diesel set through a backward unit
	 * substation; the casting machine on a diesel set's 480 V. The line must hold 180 kA at about 45 V, the pots make metal, the centre
	 * fill a reacted bag, a pot change an anode, a full pot be tapped and cast into ingots; with a unit tripped the line must hold on
	 * three at 60 kA each, a pot with no alumina must go to an anode effect at about 30 V and clear, and a pot left without current
	 * past 4 h must freeze and break into spent pot lining.
	 */
	private void potline(ClientGameTestContext context, TestServerContext server) {
		final int z = -6300;
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		net.minecraft.core.Direction south = net.minecraft.core.Direction.SOUTH;
		server.runCommand("effect give @p minecraft:slow_falling 1200 0 true");
		server.runCommand("tp @p 70 " + (up + 30) + " " + (z + 40) + " 180 40");
		context.waitTicks(20);
		for (int zz = z - 50; zz <= z + 34; zz += 14) {   // /fill takes at most 32,768 blocks
			server.runCommand("fill 0 " + (y - 1) + " " + zz + " 110 " + (y + 14) + " " + Math.min(zz + 13, z + 34) + " minecraft:air");
		}
		for (int zz = z - 50; zz <= z + 34; zz += 40) {
			server.runCommand("fill 0 " + (y - 1) + " " + zz + " 110 " + (y - 1) + " " + Math.min(zz + 39, z + 34) + " minecraft:stone");
		}
		// the power: the peaker's turbine and GSU, as its scene builds them
		BlockPos gt = new BlockPos(20, up, z);
		placeBig(server, WildcatterBlocks.AERO_TURBINE, gt, north);                                 // x 20-24, z..z+13
		placeBig(server, WildcatterBlocks.AERO_INLET_HOUSE, new BlockPos(20, up, z - 5), north);    // x 20-25, z-5..z-1
		placeBig(server, WildcatterBlocks.AERO_EXHAUST_STACK, new BlockPos(20, up, z + 14), north); // x 20-23, z+14..z+17
		placeBig(server, WildcatterBlocks.AERO_GENERATOR, new BlockPos(25, up, z + 1), north);      // x 25-28, z+1..z+7
		placeBig(server, WildcatterBlocks.GEN_BREAKER_3000, new BlockPos(26, up, z + 8), north);    // x 26-27, z+8..z+9
		placeBig(server, WildcatterBlocks.GSU_60, new BlockPos(36, up, z + 8), north);              // x 33-38, z+4..z+8
		BlockPos fuelTank = new BlockPos(14, up, z + 4);
		placeBig(server, WildcatterBlocks.STOCK_TANK, fuelTank, north);                              // x 14-17, z+4..z+7
		pipes(server, z, new int[][] {{18, 0, 6}, {19, 0, 6}});
		for (int x = 26; x <= 37; x++) {
			put(server, x, up, z + 10, "wildcatter:bus_duct_gen");
		}
		put(server, 36, up, z + 9, "wildcatter:bus_duct_gen");
		sizeCables(server, new BlockPos(26, up, z + 9), new BlockPos(37, up, z + 10), WildcatterBlocks.BUS_DUCT_GEN, dev.wildcatter.energy.ConductorSize.BUS_DUCT_3000);
		BlockPos starter = new BlockPos(10, up, z + 2);
		genset(server, starter, north, false);                                                    // x 5-10, z+2..z+3
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, starter, net.minecraft.core.Direction.EAST, false);   // x 11-14, z+1..z+2
		for (int x = 15; x <= 19; x++) {
			put(server, x, up, z + 2, "wildcatter:cable_mv");
		}
		// 138 kV: off the GSU's bushings north along x 36, down, east along z-4 and north up x 44 past the four rectiformers' terminals
		for (int zz = z - 4; zz <= z + 4; zz++) {
			put(server, 36, up + 6, zz, "wildcatter:cable_hv");
		}
		for (int yy = up; yy <= up + 5; yy++) {
			put(server, 36, yy, z - 4, "wildcatter:cable_hv");
		}
		for (int x = 37; x <= 44; x++) {
			put(server, x, up, z - 4, "wildcatter:cable_hv");
		}
		for (int zz = z - 28; zz <= z - 5; zz++) {
			put(server, 44, up, zz, "wildcatter:cable_hv");
		}
		BlockPos[] units = new BlockPos[4];
		for (int u = 0; u < 4; u++) {
			units[u] = new BlockPos(45, up, z - 28 + 6 * u);
			placeBig(server, WildcatterBlocks.RECTIFORMER, units[u], north);                     // x 45-52, six each along z-28..z-5
		}
		// the pots: row 1 facing north at x 60-89, row 2 facing south back from x 89
		BlockPos[] row1 = new BlockPos[5];
		BlockPos[] row2 = new BlockPos[5];
		for (int p = 0; p < 5; p++) {
			row1[p] = new BlockPos(60 + 6 * p, up, z - 13);
			placeBig(server, WildcatterBlocks.POT, row1[p], north);                               // x 60+6p..65+6p, z-13..z-2
			row2[p] = new BlockPos(89 - 6 * p, up, z + 13);
			placeBig(server, WildcatterBlocks.POT, row2[p], south);                               // x 84-6p..89-6p, z+2..z+13
		}
		// the 180 kA bus: the yard's + (x 52, y 2, z-8) to row 1's first riser (x 60, y 1, z-8); row 1's last collector (x 89, y 1)
		// across x 90 to row 2's first riser at z+8; row 2's last collector (x 60) back along z+8 and x 53 to the yard's - (x 52, y 0)
		for (int x = 53; x <= 59; x++) {
			put(server, x, up + 2, z - 8, "wildcatter:dc_bus_180k[axis=x]");
		}
		put(server, 59, up + 1, z - 8, "wildcatter:dc_bus_180k[axis=y]");
		for (int zz = z - 8; zz <= z + 8; zz++) {
			put(server, 90, up + 1, zz, "wildcatter:dc_bus_180k[axis=z]");
		}
		put(server, 59, up + 1, z + 8, "wildcatter:dc_bus_180k[axis=y]");
		for (int x = 53; x <= 59; x++) {
			put(server, x, up, z + 8, "wildcatter:dc_bus_180k[axis=x]");
		}
		for (int zz = z - 8; zz <= z + 7; zz++) {
			put(server, 53, up, zz, "wildcatter:dc_bus_180k[axis=z]");
		}
		// the fume treatment centre north of row 1, its fan against its north side; row 1's duct from its west end (x 59, y 5, z-8)
		BlockPos ftc = new BlockPos(60, up, z - 40);
		placeBig(server, WildcatterBlocks.FUME_TREATMENT_CENTRE, ftc, north);                    // x 60-67, z-40..z-25
		BlockPos fan = new BlockPos(60, up, z - 46);
		placeBig(server, WildcatterBlocks.FTC_FAN, fan, north);                                  // x 60-63, z-46..z-41
		for (int zz = z - 40; zz <= z - 8; zz++) {
			put(server, 59, up + 5, zz, "wildcatter:gas_line");
		}
		for (int yy = up; yy <= up + 4; yy++) {
			put(server, 59, yy, z - 40, "wildcatter:gas_line");
		}
		put(server, 62, up + 12, z - 38, "minecraft:hopper");   // its fresh alumina once the fan's set runs, so the fan starts on a live set
		BlockPos reactedOut = new BlockPos(62, y, z - 38);
		put(server, reactedOut.getX(), reactedOut.getY(), reactedOut.getZ(), "minecraft:hopper");
		// the fan's 4.16 kV off the yard's 138 kV: a step-down at the run's north end (its high side south), cable north and over to a
		// soft starter, out of its back to the fan's box
		// 10d2: the 10 MVA unit (x 42-46, z-34..z-31, its 138 kV side south): cable up a riser at z-30 to its bushings' front face
		for (int zz = z - 30; zz <= z - 29; zz++) {
			put(server, 44, up, zz, "wildcatter:cable_hv");
		}
		for (int yy = up + 1; yy <= up + 4; yy++) {
			put(server, 44, yy, z - 30, "wildcatter:cable_hv");
		}
		placeBig(server, WildcatterBlocks.SUBSTATION_TRANSFORMER_10, new BlockPos(44, up, z - 34), net.minecraft.core.Direction.SOUTH);
		for (int zz = z - 48; zz <= z - 35; zz++) {
			put(server, 44, up, zz, "wildcatter:cable_mv");
		}
		for (int yy = up + 1; yy <= up + 3; yy++) {
			put(server, 44, yy, z - 48, "wildcatter:cable_mv");
		}
		for (int x = 45; x <= 57; x++) {
			put(server, x, up + 3, z - 48, "wildcatter:cable_mv");
		}
		BlockPos fanStarter = new BlockPos(57, up, z - 48);
		placeBig(server, WildcatterBlocks.SOFT_STARTER_MV, fanStarter, north);                   // x 57, y up..up+2; its back to the south
		for (int[] c : new int[][] {{57, z - 47}, {58, z - 47}, {59, z - 47}, {59, z - 46}}) {
			put(server, c[0], up, c[1], "wildcatter:cable_mv");
		}
		// the casting machine south-west, on a diesel set's 480 V
		BlockPos caster = new BlockPos(45, up, z + 20);
		placeBig(server, WildcatterBlocks.CASTING_MACHINE, caster, north);                       // x 45-47, z+20..z+31
		BlockPos ingotsOut = new BlockPos(46, y, z + 22);
		put(server, ingotsOut.getX(), ingotsOut.getY(), ingotsOut.getZ(), "minecraft:hopper");
		BlockPos lvSet = new BlockPos(40, up, z + 20);
		genset(server, lvSet, north, false);                                                     // x 35-40, z+20..z+21
		put(server, 41, up, z + 20, "wildcatter:busway_industrial");
		for (int x = 42; x <= 44; x++) {
			put(server, x, up, z + 20, "wildcatter:cable_industrial");
		}
		// the pots' auxiliaries (pot controllers, feeders, crust breakers, beam jacks): a diesel set's 480 V along the front of each row,
		// past each pot's main cell; north of row 1 along z-14, east along x 91 and back along z+14 in front of row 2
		BlockPos auxSet = new BlockPos(75, up, z - 20);
		genset(server, auxSet, north, false);                                                    // x 70-75, z-20..z-19
		for (int zz = z - 20; zz <= z - 14; zz++) {
			put(server, 76, up, zz, "wildcatter:cable_industrial");
		}
		for (int x = 60; x <= 91; x++) {
			put(server, x, up, z - 14, "wildcatter:cable_industrial");
			put(server, x, up, z + 14, "wildcatter:cable_industrial");
		}
		for (int zz = z - 13; zz <= z + 13; zz++) {
			put(server, 91, up, zz, "wildcatter:cable_industrial");
		}
		// pot row 1's middle pot: rodded anodes on top, a hopper under it for the spent ones
		BlockPos changer = row1[2];
		put(server, changer.getX() + 1, up + 6, changer.getZ() + 2, "minecraft:hopper");
		server.runCommand("item replace block " + (changer.getX() + 1) + " " + (up + 6) + " " + (changer.getZ() + 2) + " container.0 with wildcatter:rodded_anode 2");
		BlockPos spentOut = new BlockPos(changer.getX() + 1, y, changer.getZ() + 2);
		put(server, spentOut.getX(), spentOut.getY(), spentOut.getZ(), "minecraft:hopper");
		// a pot off the line, to freeze
		BlockPos lone = new BlockPos(100, up, z + 20);
		placeBig(server, WildcatterBlocks.POT, lone, north);                                     // x 100-105, z+20..z+31
		exportBlueprint(server, "potline", new BlockPos(0, y, z - 50), new BlockPos(110, y + 14, z + 34));   // the library (docs/blueprints.md)
		// start: the starter set, the turbine, then the sets for the fan and the caster
		fillTank(server, fuelTank, dev.wildcatter.fluid.Product.KEROSENE, 40_000);
		startGenset(context, server, starter, north, 2000);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.GasTurbineBlockEntity) srv.overworld().getBlockEntity(gt)).setWantRun(true);
			return null;
		});
		context.waitTicks(200);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.GasTurbineBlockEntity) srv.overworld().getBlockEntity(gt)).skipStartForTest(false);
			return null;
		});
		context.waitTicks(100);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.GasTurbineBlockEntity) srv.overworld().getBlockEntity(gt)).skipRampForTest();
			return null;
		});
		press(server, fanStarter);
		startGenset(context, server, lvSet, north, 2000);
		startGenset(context, server, auxSet, north, 2000);
		server.runCommand("item replace block 62 " + (up + 12) + " " + (z - 38) + " container.0 with wildcatter:alumina_bag 4");
		java.util.function.ToDoubleFunction<BlockPos> potAmps = pos -> server.computeOnServer(srv ->
				((dev.wildcatter.block.PotBlockEntity) srv.overworld().getBlockEntity(pos)).amps());
		java.util.function.Supplier<String> all = () -> " | unit 1 " + read(server, 45, up, z - 28) + " | unit 4 " + read(server, 45, up, z - 10) + " | pot 1 "
				+ read(server, 60, up, z - 13) + " | pot 10 " + read(server, 65, up, z + 13) + " | turbine " + read(server, 20, up, z) + " | bus " + read(server, 56, up + 2, z - 8);
		double amps = 0;
		for (int i = 0; i < 40 && Math.abs(amps - 180_000) > 3_000; i++) {
			context.waitTicks(20);
			amps = potAmps.applyAsDouble(row1[0]);
		}
		String state = all.get();
		System.out.println("[wildcatter-test] potline: running" + state);
		check(Math.abs(amps - 180_000) < 3_000, "the yard should hold 180 kA through the pots:" + state);
		java.util.function.ToDoubleFunction<BlockPos> unitAmps = pos -> server.computeOnServer(srv ->
				((dev.wildcatter.block.RectiformerBlockEntity) srv.overworld().getBlockEntity(pos)).dc().amps());
		check(read(server, 45, up, z - 28).contains("4 of 4 online") && Math.abs(unitAmps.applyAsDouble(units[0]) - 45_000) < 1_500, "each of four units should carry 45 kA:" + state);
		double volts = server.computeOnServer(srv -> ((dev.wildcatter.block.RectiformerBlockEntity) srv.overworld().getBlockEntity(units[3])).dc().volts());
		check(volts > 38 && volts < 55, "ten pots should take about 45 V: " + volts + state);
		context.waitTicks(Tuning.WELL_INTERVAL * 2);
		double made = server.computeOnServer(srv -> ((dev.wildcatter.block.PotBlockEntity) srv.overworld().getBlockEntity(row2[4])).metal());
		check(made > 0, "the pots should make metal:" + state);
		// the fume treatment centre: almost a bag's worth of gas treated
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.FumeTreatmentCentreBlockEntity) srv.overworld().getBlockEntity(ftc)).addTreated(Tuning.FTC_GAS_PER_BAG - 5);
			((dev.wildcatter.block.PotBlockEntity) srv.overworld().getBlockEntity(changer)).burnCarbon(dev.wildcatter.fluid.Potline.carbonPerAnode() - 0.05);
			((dev.wildcatter.block.PotBlockEntity) srv.overworld().getBlockEntity(row2[0])).addMetal(Tuning.POT_METAL_HOLD_KG);
			return null;
		});
		int reacted = 0, spent = 0;
		for (int i = 0; i < 30 && (reacted == 0 || spent == 0); i++) {
			context.waitTicks(20);
			reacted = countIn(server, reactedOut, WildcatterItems.REACTED_ALUMINA_BAG);
			spent = countIn(server, spentOut, WildcatterItems.SPENT_ANODE);
		}
		String ftcRead = read(server, 60, up, z - 40);
		check(reacted > 0, "the fume treatment centre should fill a reacted alumina bag: " + ftcRead + " | fan " + read(server, 60, up, z - 46) + " | soft starter " + read(server, fanStarter.getX(), up, fanStarter.getZ()) + state);
		check(spent > 0, "the pot should change an anode and drop the spent one: " + read(server, changer.getX(), up, changer.getZ()));
		check(read(server, row2[0].getX(), up, row2[0].getZ()).contains("FULL: TAP IT"), "a full pot should ask to be tapped: " + read(server, row2[0].getX(), up, row2[0].getZ()));
		// tap it and pour the crucible into the casting machine
		String tapped = server.computeOnServer(srv -> {
			ItemStack crucible = new ItemStack(WildcatterItems.TAPPING_CRUCIBLE);
			String t = ((dev.wildcatter.block.PotBlockEntity) srv.overworld().getBlockEntity(row2[0])).tap(crucible).getString();
			dev.wildcatter.block.CastingMachineBlockEntity c = (dev.wildcatter.block.CastingMachineBlockEntity) srv.overworld().getBlockEntity(caster);
			return t + " | " + c.pour(crucible).getString();
		});
		System.out.println("[wildcatter-test] potline: " + tapped);
		check(tapped.contains("Tapped 2,726 kg") && tapped.contains("Poured 2,726 kg"), "the crucible should tap the pot and pour into the caster: " + tapped);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.CastingMachineBlockEntity) srv.overworld().getBlockEntity(caster)).addPoured(Tuning.AL_INGOT_KG * 3);
			return null;
		});
		int ingots = 0;
		for (int i = 0; i < 30 && ingots < 3; i++) {
			context.waitTicks(20);
			ingots = countIn(server, ingotsOut, WildcatterItems.ALUMINIUM_INGOT);
		}
		check(ingots >= 3, "the casting machine should drop ingots: " + ingots + " | " + read(server, 45, up, z + 20));
		server.runCommand("tp @p 74 " + (up + 26) + " " + (z + 40) + " 200 38");
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-potroom");
		// a unit trips: the other three carry 60 kA each and the line holds
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.RectiformerBlockEntity) srv.overworld().getBlockEntity(units[0])).trip();
			return null;
		});
		context.waitTicks(100);
		amps = potAmps.applyAsDouble(row1[0]);
		state = all.get();
		check(Math.abs(amps - 180_000) < 3_000, "three units should hold 180 kA: " + amps + state);
		check(read(server, 45, up, z - 10).contains("3 of 4 online") && Math.abs(unitAmps.applyAsDouble(units[3]) - 60_000) < 1_500, "each of three units should carry 60 kA:" + state);
		// an anode effect: a pot's alumina taken out
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.PotBlockEntity) srv.overworld().getBlockEntity(row1[1])).emptyAlumina();
			return null;
		});
		String effect = "";
		for (int i = 0; i < 20 && !effect.contains("ANODE EFFECT"); i++) {
			context.waitTicks(20);
			effect = read(server, row1[1].getX(), up, row1[1].getZ());
		}
		context.waitTicks(80);   // the rectiformers climb to hold the line
		double aeVolts = server.computeOnServer(srv -> ((dev.wildcatter.block.PotBlockEntity) srv.overworld().getBlockEntity(row1[1])).volts());
		amps = potAmps.applyAsDouble(row1[0]);
		state = all.get();
		check(effect.contains("ANODE EFFECT"), "a pot with no alumina should go to an anode effect: " + effect);
		check(aeVolts > 26 && aeVolts < 34, "an anode effect should read about 30 V: " + aeVolts + state);
		check(Math.abs(amps - 180_000) < 5_000, "the line should still hold 180 kA through an anode effect: " + amps + state);
		server.computeOnServer(srv -> {
			dev.wildcatter.block.PotBlockEntity pot = (dev.wildcatter.block.PotBlockEntity) srv.overworld().getBlockEntity(row1[1]);
			pot.setItem(1, new ItemStack(WildcatterItems.ALUMINA_BAG, 2));
			pot.addClearing(Tuning.POT_AE_CLEAR_SECONDS - 2);
			return null;
		});
		boolean cleared = false;
		for (int i = 0; i < 20 && !cleared; i++) {
			context.waitTicks(20);
			cleared = !server.computeOnServer(srv -> ((dev.wildcatter.block.PotBlockEntity) srv.overworld().getBlockEntity(row1[1])).effect());
		}
		check(cleared, "alumina back, the anode effect should clear: " + read(server, row1[1].getX(), up, row1[1].getZ()));
		check(!read(server, row1[1].getX(), up, row1[1].getZ()).contains("NO AUX POWER") && !read(server, row2[2].getX(), up, row2[2].getZ()).contains("NO AUX POWER"),
				"the pots' auxiliaries should have their 480 V: " + read(server, row1[1].getX(), up, row1[1].getZ()) + " | " + read(server, row2[2].getX(), up, row2[2].getZ()));
		// a pot cut out live: its shorting bars carry the line past it, and the line holds
		String cut = server.computeOnServer(srv -> ((dev.wildcatter.block.PotBlockEntity) srv.overworld().getBlockEntity(row1[3])).toggleCutOut().getString());
		context.waitTicks(100);
		amps = potAmps.applyAsDouble(row1[0]);
		String cutRead = read(server, row1[3].getX(), up, row1[3].getZ());
		System.out.println("[wildcatter-test] potline: cut out " + cut + " | " + cutRead + " | line " + Math.round(amps) + " A");
		check(cutRead.contains("CUT OUT") && Math.abs(amps - 180_000) < 5_000, "a pot cut out live should leave the line at 180 kA: " + amps + " | " + cutRead);
		server.computeOnServer(srv -> ((dev.wildcatter.block.PotBlockEntity) srv.overworld().getBlockEntity(row1[3])).toggleCutOut().getString());
		// a freeze: the lone pot past four hours without current, then broken
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.PotBlockEntity) srv.overworld().getBlockEntity(lone)).addCold(Tuning.POT_FREEZE_SECONDS - 5);
			return null;
		});
		String frozen = "";
		for (int i = 0; i < 20 && !frozen.contains("FROZEN"); i++) {
			context.waitTicks(20);
			frozen = read(server, lone.getX(), up, lone.getZ());
		}
		check(frozen.contains("FROZEN"), "a pot past 4 h without current should freeze: " + frozen);
		int lining = server.computeOnServer(srv -> {
			srv.overworld().destroyBlock(lone, true);
			return 0;
		});
		context.waitTicks(10);
		lining = server.computeOnServer(srv -> srv.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
				new net.minecraft.world.phys.AABB(lone).inflate(8), e -> e.getItem().is(WildcatterItems.SPENT_POT_LINING)).size());
		check(lining > 0, "a frozen pot should break into spent pot lining");
		System.out.println("[wildcatter-test] potline ok: " + Math.round(made * 1000) / 1000.0 + " kg made, " + reacted + " reacted bag, " + spent + " spent anode, "
				+ ingots + " ingots, AE " + Math.round(aeVolts) + " V | " + tapped + state);
	}

	/**
	 * Blueprints (10c1, docs/blueprints.md). A diesel set, a unit substation and its 4.16 kV cable with a stone-brick apron are surveyed
	 * onto a blueprint; it must hold each block and part with the cable's size. Its site marker is set 50 m east, turned to face east,
	 * and its lift list must put the parts before the blocks of their layer in pallets of at most 64. The site is then built lift by
	 * lift as a hand would, and its marker must read it built. Every library blueprint must load with every block and part known. The
	 * potline's ghost is drawn at a marker for the screenshot.
	 */
	private void blueprints(ClientGameTestContext context, TestServerContext server) {
		final int z = -6500;
		server.runCommand("effect give @p minecraft:slow_falling 600 0 true");
		server.runCommand("tp @p 30 " + (up + 18) + " " + (z + 24) + " 180 40");
		context.waitTicks(20);
		for (int zz = z - 10; zz <= z + 130; zz += 20) {
			server.runCommand("fill -70 " + (y - 1) + " " + zz + " 80 " + (y + 10) + " " + Math.min(zz + 19, z + 130) + " minecraft:air");
			server.runCommand("fill -70 " + (y - 1) + " " + zz + " 80 " + (y - 1) + " " + Math.min(zz + 19, z + 130) + " minecraft:stone");
		}
		BlockPos set = new BlockPos(12, up, z);
		genset(server, set, net.minecraft.core.Direction.NORTH, false);                       // x 7-12, z..z+1
		BlockPos mvOut = inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, set, net.minecraft.core.Direction.EAST, false);   // x 13-16
		for (int x = mvOut.getX(); x <= mvOut.getX() + 3; x++) {
			put(server, x, up, z, "wildcatter:cable_mv");
		}
		sizeCables(server, new BlockPos(mvOut.getX(), up, z), new BlockPos(mvOut.getX() + 3, up, z), WildcatterBlocks.CABLE_MV, dev.wildcatter.energy.ConductorSize.MV_500);
		for (int x = 6; x <= 20; x++) {
			put(server, x, y, z + 3, "minecraft:stone_bricks");
		}
		BlockPos lo = new BlockPos(5, y, z - 3);
		BlockPos hi = new BlockPos(21, y + 5, z + 3);
		String data = server.computeOnServer(srv -> dev.wildcatter.item.SurveyToolItem.record(srv.overworld(), lo, hi).compressed());
		dev.wildcatter.blueprint.Blueprint surveyed = dev.wildcatter.blueprint.Blueprint.decompress(data);
		String summary = surveyed.name() + ": " + surveyed.parts().size() + " parts, " + surveyed.blocks().size() + " blocks, palette " + surveyed.palette();
		System.out.println("[wildcatter-test] blueprints: surveyed " + summary);
		check(surveyed.parts().stream().anyMatch(p -> p.id().equals("wildcatter:unit_substation")), "the survey should hold the unit substation as a part: " + summary);
		check(surveyed.palette().stream().filter(st -> st.startsWith("minecraft:stone_bricks")).count() == 1
				&& surveyed.blocks().stream().filter(k -> surveyed.palette().get(k.state()).startsWith("minecraft:stone_bricks")).count() == 15, "the survey should hold the 15 stone bricks: " + summary);
		check(surveyed.sizes().containsValue(dev.wildcatter.energy.ConductorSize.MV_500.getSerializedName()), "the survey should hold the cable's size: " + surveyed.sizes());
		// its site 50 m east, turned to face east
		BlockPos site = new BlockPos(60, y, z);
		server.computeOnServer(srv -> {
			srv.overworld().setBlock(site, WildcatterBlocks.SITE_MARKER.defaultBlockState(), 3);
			((dev.wildcatter.block.SiteMarkerBlockEntity) srv.overworld().getBlockEntity(site)).set(data, 1);
			return null;
		});
		String plan = server.computeOnServer(srv -> {
			dev.wildcatter.block.SiteMarkerBlockEntity marker = (dev.wildcatter.block.SiteMarkerBlockEntity) srv.overworld().getBlockEntity(site);
			java.util.List<dev.wildcatter.blueprint.Blueprint.Lift> lifts = marker.lifts();
			StringBuilder out = new StringBuilder();
			int lastY = -1;
			boolean palletOnLayer = false;
			for (dev.wildcatter.blueprint.Blueprint.Lift lift : lifts) {
				if (lift.y() != lastY) {
					lastY = lift.y();
					palletOnLayer = false;
				}
				if (lift.isPart() && palletOnLayer) {
					out.append(" PART AFTER A PALLET ON LAYER ").append(lift.y());
				}
				if (!lift.isPart()) {
					palletOnLayer = true;
					if (lift.blocks().size() > dev.wildcatter.blueprint.Blueprint.PALLET_BLOCKS) {
						out.append(" PALLET OF ").append(lift.blocks().size());
					}
				}
			}
			return lifts.size() + " lifts" + out;
		});
		System.out.println("[wildcatter-test] blueprints: site " + plan + " | " + read(server, site.getX(), site.getY(), site.getZ()));
		check(!plan.contains("PART AFTER") && !plan.contains("PALLET OF"), "the lift list should put parts first on each layer, in pallets of at most 64: " + plan);
		// built lift by lift, as a hand would
		boolean set0 = server.computeOnServer(srv -> {
			dev.wildcatter.block.SiteMarkerBlockEntity marker = (dev.wildcatter.block.SiteMarkerBlockEntity) srv.overworld().getBlockEntity(site);
			boolean ok = true;
			for (dev.wildcatter.blueprint.Blueprint.Lift lift : marker.lifts()) {
				ok &= dev.wildcatter.blueprint.Surveys.setLift(srv.overworld(), site, marker.blueprint(), marker.states(), lift);
			}
			marker.recount();
			return ok;
		});
		String built = read(server, site.getX(), site.getY(), site.getZ());
		System.out.println("[wildcatter-test] blueprints: built " + built);
		check(set0 && built.contains("built"), "the site, built lift by lift, should read built: " + built);
		// every library blueprint loads, with every block and part known
		String library = server.computeOnServer(srv -> {
			StringBuilder bad = new StringBuilder();
			for (String name : dev.wildcatter.blueprint.BlueprintLibrary.names()) {
				dev.wildcatter.blueprint.Blueprint b = dev.wildcatter.blueprint.BlueprintLibrary.load(name);
				if (b == null) {
					bad.append(" MISSING ").append(name);
					continue;
				}
				net.minecraft.world.level.block.state.BlockState[] states = dev.wildcatter.blueprint.Surveys.states(b);
				for (int i = 0; i < states.length; i++) {
					if (states[i] == null) {
						bad.append(" ").append(name).append(": unknown ").append(b.palette().get(i));
					}
				}
				for (dev.wildcatter.blueprint.Blueprint.Part p : b.parts()) {
					if (dev.wildcatter.blueprint.Surveys.partState(p) == null) {
						bad.append(" ").append(name).append(": unknown part ").append(p.id());
					}
				}
			}
			return bad.toString();
		});
		check(library.isEmpty(), "every library blueprint should load:" + library);
		// the potline's ghost at a marker, for the screenshot
		BlockPos ghost = new BlockPos(-60, y, z + 40);
		server.computeOnServer(srv -> {
			srv.overworld().setBlock(ghost, WildcatterBlocks.SITE_MARKER.defaultBlockState(), 3);
			((dev.wildcatter.block.SiteMarkerBlockEntity) srv.overworld().getBlockEntity(ghost)).set("lib:potline", 0);
			return null;
		});
		server.runCommand("tp @p -76 " + (up + 24) + " " + (z + 26) + " -45 25");   // beside its marker, so the marker's chunk is loaded
		context.waitTicks(60);
		boolean seen = context.computeOnClient(client -> dev.wildcatter.block.SiteMarkerBlockEntity.CLIENT.stream()
				.anyMatch(s -> s.getBlockPos().equals(ghost) && "lib:potline".equals(s.data()) && s.blueprint() != null));
		String clientSide = context.computeOnClient(client -> {
			var be = client.level.getBlockEntity(ghost);
			return "markers " + dev.wildcatter.block.SiteMarkerBlockEntity.CLIENT.size() + ", block " + client.level.getBlockState(ghost) + ", entity "
					+ (be instanceof dev.wildcatter.block.SiteMarkerBlockEntity m ? "data " + m.data().length() + " chars, blueprint " + (m.blueprint() != null) : String.valueOf(be))
					+ ", player " + client.player.blockPosition();
		});
		check(seen, "the client should hold the potline's site marker with its blueprint, for the ghost: " + clientSide);
		context.takeScreenshot("wildcatter-blueprint-ghost");
		System.out.println("[wildcatter-test] blueprints ok: " + summary + " | " + plan + " | " + built);
	}

	/**
	 * Cranes (10c2, docs/multiblocks/tower-crane.md, crawler-crane.md). A tower crane on a diesel set's 480 V finishes a surveyed site
	 * (a stone-brick pad with a 480 V pad-mount on it, seeded with all but its last two lifts) from its chest, at its real speeds under
	 * /tick sprint; its marker then goes to DISMANTLE and the crane must lift the pad-mount back into its chest. A crawler crane fed
	 * diesel from a stock tank must read TOO HEAVY for a GSU 60 (90 t) at 9 m on its 86 m boom, and set it on its 32 m boom.
	 */
	private void cranes(ClientGameTestContext context, TestServerContext server) {
		final int z = -6800;
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		server.runCommand("effect give @p minecraft:slow_falling 600 0 true");
		server.runCommand("weather clear");   // rain is over the crawler's 9 m/s at its boom head
		server.runCommand("tp @p 40 " + (up + 30) + " " + (z + 45) + " 180 30");
		context.waitTicks(20);
		for (int zz = z - 20; zz <= z + 40; zz += 20) {
			server.runCommand("fill 0 " + (y - 1) + " " + zz + " 80 " + (y + 12) + " " + Math.min(zz + 19, z + 40) + " minecraft:air");
			server.runCommand("fill 0 " + (y - 1) + " " + zz + " 80 " + (y - 1) + " " + Math.min(zz + 19, z + 40) + " minecraft:stone");
		}
		// ---- the tower crane, fed from a diesel set's 480 V terminals by cable to its switch cabinet
		BlockPos tower = new BlockPos(20, y, z);
		placeBig(server, WildcatterBlocks.TOWER_CRANE, tower, north);                    // x 20-25, z..z+5, its cabinet's terminal on the west face
		BlockPos set = new BlockPos(12, up, z);
		genset(server, set, north, false);                                               // its terminals on the panel's east face
		put(server, 13, up, z, "wildcatter:cable_industrial");
		for (int x = 13; x <= 19; x++) {
			put(server, x, y, z, "wildcatter:cable_industrial");
		}
		// its site: surveyed in place, cleared, its marker set at the box's empty corner
		BlockPos origin = new BlockPos(30, y, z + 10);
		for (int x = 31; x <= 35; x++) {
			for (int zz = z + 11; zz <= z + 15; zz++) {
				put(server, x, y, zz, "minecraft:stone_bricks");
			}
		}
		placeBig(server, WildcatterBlocks.PADMOUNT_480, new BlockPos(33, y + 1, z + 13), north);
		String towerData = server.computeOnServer(srv -> dev.wildcatter.item.SurveyToolItem.record(srv.overworld(), origin, origin.offset(6, 4, 6)).compressed());
		server.runCommand("fill " + origin.getX() + " " + y + " " + origin.getZ() + " " + (origin.getX() + 6) + " " + (y + 4) + " " + (origin.getZ() + 6) + " minecraft:air");
		String seeded = server.computeOnServer(srv -> {
			srv.overworld().setBlock(origin, WildcatterBlocks.SITE_MARKER.defaultBlockState(), 3);
			dev.wildcatter.block.SiteMarkerBlockEntity marker = (dev.wildcatter.block.SiteMarkerBlockEntity) srv.overworld().getBlockEntity(origin);
			marker.set(towerData, 0);
			java.util.List<dev.wildcatter.blueprint.Blueprint.Lift> lifts = marker.lifts();
			for (int i = 0; i < lifts.size() - 2; i++) {
				dev.wildcatter.blueprint.Surveys.setLift(srv.overworld(), origin, marker.blueprint(), marker.states(), lifts.get(i));
			}
			marker.recount();
			return lifts.size() + " lifts, the last " + marker.describe(lifts.get(lifts.size() - 1));
		});
		put(server, 26, y, z + 2, "minecraft:chest");
		server.runCommand("item replace block 26 " + y + " " + (z + 2) + " container.0 with minecraft:stone_bricks 64");
		server.runCommand("item replace block 26 " + y + " " + (z + 2) + " container.1 with wildcatter:padmount_480 1");
		startGenset(context, server, set, north, 2000);
		System.out.println("[wildcatter-test] cranes: tower site " + seeded + " | " + read(server, tower.getX(), tower.getY(), tower.getZ()));
		// ---- the crawler crane, fed diesel from a stock tank by pipe to its west crawler
		BlockPos crawler = new BlockPos(60, y, z);
		placeBig(server, WildcatterBlocks.CRAWLER_CRANE, crawler, north);                // x 60-68, z..z+8, its centre at 64.5, z+4.5
		BlockPos tank = new BlockPos(52, y, z + 2);
		placeBig(server, WildcatterBlocks.STOCK_TANK, tank, north);                      // x 52-55, z+2..z+5
		fillTank(server, tank, dev.wildcatter.fluid.Product.DIESEL, 5_000);   // first: a run carries what its sources offer when it is laid
		for (int x = 56; x <= 59; x++) {
			put(server, x, y, z + 3, "wildcatter:gas_line");   // the steel process line
		}
		// its site: a GSU 60 surveyed in place 9 m north of its centre, then taken away
		BlockPos gsuOrigin = new BlockPos(61, y, z - 8);
		placeBig(server, WildcatterBlocks.GSU_60, new BlockPos(65, y, z - 3), north);    // x 62-67, z-7..z-3
		String gsuData = server.computeOnServer(srv -> dev.wildcatter.item.SurveyToolItem.record(srv.overworld(), gsuOrigin, gsuOrigin.offset(7, 9, 6)).compressed());
		server.runCommand("fill 61 " + y + " " + (z - 8) + " 68 " + (y + 9) + " " + (z - 2) + " minecraft:air");
		server.computeOnServer(srv -> {
			srv.overworld().setBlock(gsuOrigin, WildcatterBlocks.SITE_MARKER.defaultBlockState(), 3);
			((dev.wildcatter.block.SiteMarkerBlockEntity) srv.overworld().getBlockEntity(gsuOrigin)).set(gsuData, 0);
			return null;
		});
		put(server, 69, y, z + 4, "minecraft:chest");   // its GSU goes in once the tower crane is done, so each is seen at work
		// on the 86 m boom the GSU is too heavy at 9 m
		server.computeOnServer(srv -> {
			dev.wildcatter.block.CraneBlockEntity c = (dev.wildcatter.block.CraneBlockEntity) srv.overworld().getBlockEntity(crawler);
			while (c.boom() != 12) {
				c.nextBoom();
			}
			return null;
		});
		context.waitTicks(60);
		String heavy = read(server, crawler.getX(), crawler.getY(), crawler.getZ());
		System.out.println("[wildcatter-test] cranes: crawler on its 86 m boom | " + heavy);
		check(heavy.contains("TOO HEAVY") && heavy.contains("86 m boom"), "the GSU 60 should be too heavy for the 86 m boom at 9 m: " + heavy);
		check(!heavy.contains("diesel 0 of"), "the crawler should have drawn diesel from the stock tank: " + heavy);
		server.computeOnServer(srv -> {
			dev.wildcatter.block.CraneBlockEntity c = (dev.wildcatter.block.CraneBlockEntity) srv.overworld().getBlockEntity(crawler);
			while (c.boom() != 3) {
				c.nextBoom();
			}
			return null;
		});
		// both cranes work at their real speeds, sped up; each is caught mid-lift for a screenshot
		server.runCommand("tick sprint 40000");
		String towerLift = waitFor(context, server, () -> server.computeOnServer(srv -> ((dev.wildcatter.block.CraneBlockEntity) srv.overworld().getBlockEntity(tower)).carrying()), 2400);
		server.runCommand("tick sprint stop");
		context.waitTicks(10);
		server.runCommand("tp @p -25 " + (y + 30) + " " + (z + 55) + " -137 0");
		context.waitTicks(30);
		System.out.println("[wildcatter-test] cranes: tower mid-lift " + towerLift + " | " + read(server, tower.getX(), tower.getY(), tower.getZ()));
		context.takeScreenshot("wildcatter-tower-crane-lift");
		server.runCommand("tick sprint 40000");
		String built = waitFor(context, server, () -> read(server, origin.getX(), origin.getY(), origin.getZ()).contains("built"), 3000);
		String towerRead = read(server, tower.getX(), tower.getY(), tower.getZ());
		String site = read(server, origin.getX(), origin.getY(), origin.getZ());
		System.out.println("[wildcatter-test] cranes: tower built " + built + " | " + site + " | " + towerRead);
		check(site.contains("built"), "the tower crane should finish its site: " + site + " | " + towerRead);
		server.runCommand("item replace block 69 " + y + " " + (z + 4) + " container.0 with wildcatter:gsu_60 1");
		String crawlerLift = waitFor(context, server, () -> server.computeOnServer(srv -> ((dev.wildcatter.block.CraneBlockEntity) srv.overworld().getBlockEntity(crawler)).carrying()), 3000);
		server.runCommand("tick sprint stop");
		context.waitTicks(10);
		server.runCommand("tp @p 95 " + (y + 20) + " " + (z + 30) + " 130 7");
		context.waitTicks(30);
		System.out.println("[wildcatter-test] cranes: crawler mid-lift " + crawlerLift + " | " + read(server, crawler.getX(), crawler.getY(), crawler.getZ()));
		context.takeScreenshot("wildcatter-crawler-crane-lift");
		server.runCommand("tick sprint 40000");
		String gsuSet = waitFor(context, server, () -> read(server, gsuOrigin.getX(), gsuOrigin.getY(), gsuOrigin.getZ()).contains("built"), 3000);
		String gsuSite = read(server, gsuOrigin.getX(), gsuOrigin.getY(), gsuOrigin.getZ());
		String crawlerRead = read(server, crawler.getX(), crawler.getY(), crawler.getZ());
		System.out.println("[wildcatter-test] cranes: crawler set " + gsuSet + " | " + gsuSite + " | " + crawlerRead);
		check(gsuSite.contains("built"), "the crawler crane on its 32 m boom should set the GSU 60: " + gsuSite + " | " + crawlerRead);
		// the tower site to DISMANTLE: the pad-mount, its last lift, comes out first into the chest
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.SiteMarkerBlockEntity) srv.overworld().getBlockEntity(origin)).toggleMode();
			return null;
		});
		String out = waitFor(context, server, () -> server.computeOnServer(srv -> srv.overworld().getBlockEntity(new BlockPos(26, y, z + 2)) instanceof net.minecraft.world.Container chest
				&& java.util.stream.IntStream.range(0, chest.getContainerSize()).anyMatch(i -> chest.getItem(i).is(WildcatterBlocks.PADMOUNT_480.asItem()))), 3000);
		server.runCommand("tick sprint stop");
		String down = read(server, origin.getX(), origin.getY(), origin.getZ());
		System.out.println("[wildcatter-test] cranes: dismantle " + out + " | " + down + " | " + read(server, tower.getX(), tower.getY(), tower.getZ()));
		check(out.startsWith("yes"), "dismantling should lift the pad-mount back into the tower crane's chest: " + down);
		check(down.contains("DISMANTLE"), "the marker should read DISMANTLE: " + down);
		System.out.println("[wildcatter-test] cranes ok: " + heavy + " | " + site + " | " + gsuSite + " | " + down);
	}

	/**
	 * 10c3 (docs/multiblocks/ring-crane.md, docs/blueprints.md "Sharing and tandem lifts"): the ring crane sets a reactor vessel from
	 * its laydown chest 45 m out; then two crawlers share one site, setting its pallets between them and its GSU 60 in tandem.
	 */
	private void heavyLift(ClientGameTestContext context, TestServerContext server) {
		final int z = -7200;
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand("weather clear");   // rain is over the crawler's 9 m/s at its boom head
		server.runCommand("tp @p 100 " + (up + 40) + " " + (z + 5) + " 180 30");   // between both areas, so all their chunks load
		context.waitTicks(60);
		for (int zz = z - 50; zz <= z + 60; zz += 20) {
			int z2 = Math.min(zz + 19, z + 60);
			for (int[] xs : new int[][] {{28, 92}, {122, 166}}) {
				server.runCommand("fill " + xs[0] + " " + (y - 1) + " " + zz + " " + xs[1] + " " + (y + 12) + " " + z2 + " minecraft:air");
				server.runCommand("fill " + xs[0] + " " + (y - 1) + " " + zz + " " + xs[1] + " " + (y - 1) + " " + z2 + " minecraft:stone");
			}
		}
		// ---- the ring crane: its ring x 36-84, z..z+48, its centre at 60.5, z+24.5, its boom north at rest
		BlockPos ring = new BlockPos(60, y, z);
		placeBig(server, WildcatterBlocks.RING_CRANE, ring, north);
		BlockPos tank = new BlockPos(57, y, z + 23);
		placeBig(server, WildcatterBlocks.STOCK_TANK, tank, north);                      // x 57-60, z+23..z+26, inside the ring
		fillTank(server, tank, dev.wildcatter.fluid.Product.DIESEL, 8_000);
		for (int x = 61; x <= 79; x++) {
			put(server, x, y, z + 24, "wildcatter:gas_line");                             // to the ring's inner edge at x 80
		}
		// its site: a reactor vessel surveyed 60 m north of the centre, then taken away
		BlockPos vesselOrigin = new BlockPos(58, y, z - 38);
		placeBig(server, WildcatterBlocks.REACTOR_VESSEL, new BlockPos(59, y, z - 37), north);   // x 59-61, z-37..z-35: its centre 60 m out
		String vesselData = server.computeOnServer(srv -> dev.wildcatter.item.SurveyToolItem.record(srv.overworld(), vesselOrigin, vesselOrigin.offset(4, 9, 4)).compressed());
		server.runCommand("fill 58 " + y + " " + (z - 38) + " 62 " + (y + 9) + " " + (z - 34) + " minecraft:air");
		server.computeOnServer(srv -> {
			srv.overworld().setBlock(vesselOrigin, WildcatterBlocks.SITE_MARKER.defaultBlockState(), 3);
			((dev.wildcatter.block.SiteMarkerBlockEntity) srv.overworld().getBlockEntity(vesselOrigin)).set(vesselData, 0);
			return null;
		});
		// its laydown: a chest 45 m out, 10 degrees west of north
		put(server, 52, y, z - 20, "minecraft:chest");
		// the 160.5 m boom reads less at 60 m; back to the 118 m boom, capped at its hook
		String charts = server.computeOnServer(srv -> {
			dev.wildcatter.block.CraneBlockEntity c = (dev.wildcatter.block.CraneBlockEntity) srv.overworld().getBlockEntity(ring);
			c.nextBoom();
			String longBoom = c.boom() + ":" + c.chart().capacity(60);
			c.nextBoom();
			return longBoom + " | " + c.boom() + ":" + c.chart().chartFigure(60) + ":" + c.chart().capacity(60);
		});
		System.out.println("[wildcatter-test] heavyLift: ring charts at 60 m " + charts);
		check(charts.equals("1:2850.0 | 0:3831.0:3200.0"), "the ring crane's charts at 60 m should read 2,850 t on 160.5 m, and 3,831 t capped at 3,200 t on 118 m: " + charts);
		context.waitTicks(60);
		String waiting = read(server, ring.getX(), ring.getY(), ring.getZ());
		System.out.println("[wildcatter-test] heavyLift: ring waiting | " + waiting);
		check(waiting.contains("118 m boom") && waiting.contains("WAITING") && !waiting.contains("diesel 0 of"),
				"the ring crane should have diesel and wait for the vessel in its laydown chest: " + waiting);
		server.runCommand("item replace block 52 " + y + " " + (z - 20) + " container.0 with wildcatter:reactor_vessel 1");
		server.runCommand("tick sprint 80000");
		String ringLift = waitFor(context, server, () -> server.computeOnServer(srv -> ((dev.wildcatter.block.CraneBlockEntity) srv.overworld().getBlockEntity(ring)).carrying()), 4000);
		server.runCommand("tick sprint stop");
		context.waitTicks(10);
		String lifting = read(server, ring.getX(), ring.getY(), ring.getZ());
		server.runCommand("tp @p 115 " + (y + 35) + " " + (z + 30) + " 129 -8");   // near enough that its chunks keep ticking
		context.waitTicks(40);
		System.out.println("[wildcatter-test] heavyLift: ring mid-lift " + ringLift + " | " + lifting);
		check(lifting.contains("288.2 t on the hook") && lifting.contains("3200.0 t at 60 m"),
				"the ring crane should carry the vessel and its 104.6 t hook block against its capped chart at 60 m: " + lifting);
		context.takeScreenshot("wildcatter-ring-crane-lift");
		server.runCommand("tp @p 100 " + (up + 40) + " " + (z + 5) + " 180 30");
		server.runCommand("tick sprint 80000");
		String vesselSet = waitFor(context, server, () -> read(server, vesselOrigin.getX(), vesselOrigin.getY(), vesselOrigin.getZ()).contains("built"), 8000);
		server.runCommand("tick sprint stop");
		String vesselSite = read(server, vesselOrigin.getX(), vesselOrigin.getY(), vesselOrigin.getZ());
		System.out.println("[wildcatter-test] heavyLift: vessel set " + vesselSet + " | " + vesselSite + " | " + read(server, ring.getX(), ring.getY(), ring.getZ()));
		check(vesselSite.contains("built"), "the ring crane should set the reactor vessel: " + vesselSite + " | " + read(server, ring.getX(), ring.getY(), ring.getZ()));
		// ---- two crawlers, 20 m apart, share one site: a GSU 60 between them and three pallets of stone bricks
		BlockPos lead = new BlockPos(130, y, z + 10);                                    // x 130-138, z+10..z+18, centre 134.5, z+14.5
		BlockPos second = new BlockPos(150, y, z + 10);                                  // x 150-158, centre 154.5, z+14.5
		placeBig(server, WildcatterBlocks.CRAWLER_CRANE, lead, north);
		placeBig(server, WildcatterBlocks.CRAWLER_CRANE, second, north);
		BlockPos tank2 = new BlockPos(143, y, z + 16);                                   // x 143-146, z+16..z+19, between them
		placeBig(server, WildcatterBlocks.STOCK_TANK, tank2, north);
		fillTank(server, tank2, dev.wildcatter.fluid.Product.DIESEL, 3_000);
		for (int x : new int[] {139, 140, 141, 142, 147, 148, 149}) {
			put(server, x, y, z + 17, "wildcatter:gas_line");
		}
		BlockPos siteOrigin = new BlockPos(140, y, z - 3);                               // the box x 140-154, z-3..z+6
		placeBig(server, WildcatterBlocks.GSU_60, new BlockPos(145, y, z + 4), north);   // x 142-147, z..z+4: 16 m from each crane's centre
		for (int x : new int[] {140, 141, 148, 149, 150, 151, 152, 153, 154}) {
			put(server, x, y, z + 6, "minecraft:stone_bricks");                          // three pallets: x 140-141, 148-149 and 150-154
		}
		String siteData = server.computeOnServer(srv -> dev.wildcatter.item.SurveyToolItem.record(srv.overworld(), siteOrigin, siteOrigin.offset(14, 9, 9)).compressed());
		server.runCommand("fill 140 " + y + " " + (z - 3) + " 154 " + (y + 9) + " " + (z + 6) + " minecraft:air");
		String lifts = server.computeOnServer(srv -> {
			srv.overworld().setBlock(siteOrigin, WildcatterBlocks.SITE_MARKER.defaultBlockState(), 3);
			dev.wildcatter.block.SiteMarkerBlockEntity m = (dev.wildcatter.block.SiteMarkerBlockEntity) srv.overworld().getBlockEntity(siteOrigin);
			m.set(siteData, 0);
			return m.lifts().size() + " lifts, first " + m.describe(m.lifts().get(0));
		});
		put(server, 139, y, z + 14, "minecraft:chest");                                  // the lead's, against its east crawler
		put(server, 159, y, z + 14, "minecraft:chest");                                  // the second's, against its east crawler
		context.waitTicks(80);
		String alone = read(server, lead.getX(), lead.getY(), lead.getZ());
		System.out.println("[wildcatter-test] heavyLift: shared site " + lifts + " | lead alone " + alone);
		check(alone.contains("TOO HEAVY") && alone.contains("89.5 t at 16 m"), "the GSU 60 should be too heavy for one crawler at 16 m: " + alone);
		check(!alone.contains("diesel 0 of"), "the lead crawler should have drawn diesel: " + alone);
		server.runCommand("item replace block 139 " + y + " " + (z + 14) + " container.0 with wildcatter:gsu_60 1");
		server.runCommand("item replace block 139 " + y + " " + (z + 14) + " container.1 with minecraft:stone_bricks 32");
		server.runCommand("item replace block 159 " + y + " " + (z + 14) + " container.0 with minecraft:stone_bricks 64");
		server.runCommand("tp @p 144 " + (y + 30) + " " + (z + 50) + " 180 25");
		server.runCommand("tick sprint 40000");
		String[] tandemRead = new String[1];
		String tandem = waitFor(context, server, () -> {
			String r = read(server, lead.getX(), lead.getY(), lead.getZ());
			boolean on = r.contains("TANDEM") && server.computeOnServer(srv -> ((dev.wildcatter.block.CraneBlockEntity) srv.overworld().getBlockEntity(lead)).carrying());
			if (on) {
				tandemRead[0] = r;
			}
			return on;
		}, 4000);
		server.runCommand("tick sprint stop");
		server.runCommand("tp @p 144 " + (y + 20) + " " + (z + 42) + " 180 5");
		context.waitTicks(30);
		System.out.println("[wildcatter-test] heavyLift: tandem " + tandem + " | " + tandemRead[0] + " | " + read(server, second.getX(), second.getY(), second.getZ()));
		check(tandemRead[0] != null && tandemRead[0].contains("45.9 of 91.8 t") && tandemRead[0].contains("80 % 71.6 t"),
				"the two crawlers should lift the GSU 60 in tandem, 45.9 t each against 80 % of 89.5 t: " + tandemRead[0]);
		context.takeScreenshot("wildcatter-tandem-lift");
		server.runCommand("tick sprint 40000");
		String built = waitFor(context, server, () -> read(server, siteOrigin.getX(), siteOrigin.getY(), siteOrigin.getZ()).contains("built"), 6000);
		server.runCommand("tick sprint stop");
		String shared = read(server, siteOrigin.getX(), siteOrigin.getY(), siteOrigin.getZ());
		String twice = server.computeOnServer(srv -> {
			dev.wildcatter.block.SiteMarkerBlockEntity m = (dev.wildcatter.block.SiteMarkerBlockEntity) srv.overworld().getBlockEntity(siteOrigin);
			return m.workers() + " cranes, " + m.setTwice() + " set twice";
		});
		System.out.println("[wildcatter-test] heavyLift: shared site built " + built + " | " + shared + " | " + twice);
		check(shared.contains("built") && shared.contains("worked by 2 cranes"), "both crawlers should build the shared site: " + shared + " | " + twice);
		check(twice.endsWith(" 0 set twice"), "no lift should be set twice: " + twice);
		System.out.println("[wildcatter-test] heavyLift ok: " + charts + " | " + vesselSite + " | " + tandemRead[0] + " | " + shared);
	}

	/**
	 * 10d1 (docs/multiblocks/gen-bus-protection.md): a gas turbine's 13.8 kV bus with two feeder breakers to load banks and its relay
	 * panel. Grounds on the bus trip everything by 87B and lock out; grounds past a feeder trip that feeder alone by its 50; grounds on
	 * a dead section trip nothing.
	 */
	private void busProtection(ClientGameTestContext context, TestServerContext server) {
		final int z = -7500;
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand("tp @p 30 " + (up + 18) + " " + (z + 34) + " 180 30");
		context.waitTicks(20);
		server.runCommand("fill 2 " + (y - 1) + " " + (z - 8) + " 40 " + (y + 12) + " " + (z + 24) + " minecraft:air");
		server.runCommand("fill 2 " + (y - 1) + " " + (z - 8) + " 40 " + (y - 1) + " " + (z + 24) + " minecraft:stone");
		BlockPos gt = new BlockPos(20, up, z);
		placeBig(server, WildcatterBlocks.GAS_TURBINE, gt, north);                                   // x 20-23, z..z+9
		placeBig(server, WildcatterBlocks.GT_INLET_HOUSE, new BlockPos(20, up, z - 4), north);       // x 20-23, z-4..z-1
		placeBig(server, WildcatterBlocks.GT_GENERATOR, new BlockPos(24, up, z + 1), north);         // x 24-27, z+1..z+6
		placeBig(server, WildcatterBlocks.GEN_BREAKER, new BlockPos(25, up, z + 7), north);          // x 25-26, z+7..z+8
		placeBig(server, WildcatterBlocks.HRSG, new BlockPos(20, up, z + 10), north);                // x 20-24, z+10..z+21, its exhaust
		BlockPos fuelTank = new BlockPos(14, up, z + 4);
		placeBig(server, WildcatterBlocks.STOCK_TANK, fuelTank, north);                              // x 14-17, z+4..z+7
		pipes(server, z, new int[][] {{18, 0, 6}, {19, 0, 6}});                                     // kerosene to the fuel nozzle
		// the bus: from the generator breaker east to x 28, and south to z+12; the panel against it
		for (int x = 25; x <= 28; x++) {
			put(server, x, up, z + 9, "wildcatter:bus_duct_gen");
		}
		for (int zz = z + 10; zz <= z + 12; zz++) {
			put(server, 28, up, zz, "wildcatter:bus_duct_gen");
		}
		placeBig(server, WildcatterBlocks.BUS_RELAY_PANEL, new BlockPos(27, up, z + 10), north);
		// feeder 1 at z+9 and feeder 2 at z+12, each 3 along the line (x 29-31); past each, duct to its load bank
		placeBig(server, WildcatterBlocks.GEN_FEEDER_BREAKER, new BlockPos(29, up, z + 9), north);
		placeBig(server, WildcatterBlocks.GEN_FEEDER_BREAKER, new BlockPos(29, up, z + 12), north);
		for (int x = 32; x <= 37; x++) {
			put(server, x, up, z + 9, "wildcatter:bus_duct_gen");
		}
		for (int x = 32; x <= 34; x++) {
			put(server, x, up, z + 12, "wildcatter:bus_duct_gen");
		}
		placeBig(server, WildcatterBlocks.LOAD_BANK, new BlockPos(35, up, z + 10), north);           // x 35-37, z+10..z+15, fed at z+9
		placeBig(server, WildcatterBlocks.LOAD_BANK, new BlockPos(32, up, z + 13), north);           // x 32-34, z+13..z+18, fed at z+12
		// station power for the starter: a diesel set and a backward unit substation, 4.16 kV to the turbine's west face
		BlockPos set = new BlockPos(10, up, z + 2);
		genset(server, set, north, false);
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, set, net.minecraft.core.Direction.EAST, false);
		for (int x = 15; x <= 19; x++) {
			put(server, x, up, z + 2, "wildcatter:cable_mv");
		}
		fillTank(server, fuelTank, dev.wildcatter.fluid.Product.KEROSENE, 40_000);
		startGenset(context, server, set, north, 2000);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.GasTurbineBlockEntity) srv.overworld().getBlockEntity(gt)).setWantRun(true);
			return null;
		});
		context.waitTicks(200);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.GasTurbineBlockEntity) srv.overworld().getBlockEntity(gt)).skipStartForTest();
			for (BlockPos bank : new BlockPos[] {new BlockPos(35, up, z + 10), new BlockPos(32, up, z + 13)}) {
				((dev.wildcatter.block.LoadBankBlockEntity) srv.overworld().getBlockEntity(bank)).setSteps(4);
			}
			return null;
		});
		context.waitTicks(200);
		String running = read(server, 20, up, z);
		String panel = read(server, 27, up, z + 10);
		System.out.println("[wildcatter-test] busProtection: " + running + " | " + panel);
		check(running.contains("breaker CLOSED"), "the gas turbine should be on its bus: " + running);
		check(panel.contains("1 generator breaker, 2 feeders") && panel.contains("87B ready"), "the panel should find its zone: " + panel);
		check(read(server, 35, up, z + 10).matches(".*taking [1-9].*") && read(server, 32, up, z + 13).matches(".*taking [1-9].*"), "both load banks should run through their feeders: "
				+ read(server, 35, up, z + 10) + " | " + read(server, 32, up, z + 13));
		server.runCommand("tp @p 39 " + (up + 7) + " " + (z + 3) + " 55 32");   // from the north-east, over the feeder breakers to the panel
		context.waitTicks(20);
		context.takeScreenshot("wildcatter-bus-protection");
		java.util.function.Consumer<BlockPos> grounds = at -> server.computeOnServer(srv -> {
			dev.wildcatter.block.MultiCableBlockEntity duct = (dev.wildcatter.block.MultiCableBlockEntity) srv.overworld().getBlockEntity(at);
			duct.setGrounded(!duct.grounded());
			return null;
		});
		// ---- a bus fault: grounds on the bus between the breakers
		BlockPos onBus = new BlockPos(28, up, z + 10);
		grounds.accept(onBus);
		context.waitTicks(20);
		String busTrip = read(server, 27, up, z + 10);
		String gen = read(server, 24, up, z + 1);
		String f1 = read(server, 29, up, z + 9);
		String f2 = read(server, 29, up, z + 12);
		System.out.println("[wildcatter-test] busProtection: bus fault | " + busTrip + " | " + gen + " | " + f1 + " | " + f2);
		check(busTrip.contains("87B BUS DIFFERENTIAL TRIP") && busTrip.contains("LOCKED OUT"), "grounds on the bus should trip 87B and lock out: " + busTrip);
		check(gen.contains("TRIPPED") && gen.contains("87B"), "87B should trip the generator: " + gen);
		check(f1.contains("TRIPPED") && f2.contains("TRIPPED"), "87B should trip both feeders: " + f1 + " | " + f2);
		// grounds off, the panel reset: the feeders close, the generator is reset, and its turbine (which ran down with the unit trip)
		// starts again, synchronizes onto the dead bus and closes
		grounds.accept(onBus);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.BusRelayPanelBlockEntity) srv.overworld().getBlockEntity(new BlockPos(27, up, z + 10))).reset();
			return null;
		});
		context.waitTicks(100);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.GasTurbineBlockEntity) srv.overworld().getBlockEntity(gt)).skipStartForTest();
			return null;
		});
		context.waitTicks(200);
		String back = read(server, 35, up, z + 10);
		System.out.println("[wildcatter-test] busProtection: reset | " + read(server, 27, up, z + 10) + " | " + read(server, 20, up, z) + " | " + back);
		check(back.matches(".*taking [1-9].*"), "after the reset and a restart the load bank should run again: " + back + " | " + read(server, 20, up, z));
		// ---- a feeder fault: grounds past feeder 1 trip it alone by its 50
		BlockPos pastFeeder = new BlockPos(33, up, z + 9);
		grounds.accept(pastFeeder);
		context.waitTicks(20);
		String feederTrip = read(server, 29, up, z + 9);
		String stillOn = read(server, 24, up, z + 1);
		String notLocked = read(server, 27, up, z + 10);
		System.out.println("[wildcatter-test] busProtection: feeder fault | " + feederTrip + " | " + stillOn + " | " + notLocked + " | " + read(server, 29, up, z + 12));
		check(feederTrip.contains("TRIPPED") && feederTrip.contains("50"), "grounds past feeder 1 should trip it by its 50: " + feederTrip);
		check(stillOn.contains("CLOSED") && !notLocked.contains("LOCKED OUT") && read(server, 29, up, z + 12).contains("CLOSED"),
				"a feeder fault should leave the generator, the other feeder and 87B alone: " + stillOn + " | " + notLocked);
		// ---- the same grounds on the section now dead: GROUNDED, and nothing more trips
		context.waitTicks(60);
		String dead = read(server, 33, up, z + 9);
		System.out.println("[wildcatter-test] busProtection: dead section | " + dead + " | " + read(server, 24, up, z + 1));
		check(dead.contains("GROUNDED") && read(server, 24, up, z + 1).contains("CLOSED") && read(server, 32, up, z + 13).matches(".*taking [1-9].*"),
				"grounds on a dead section should read GROUNDED and trip nothing: " + dead);
		// grounds off and the panel reset: feeder 1, opened by its own 50, closes again
		grounds.accept(pastFeeder);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.BusRelayPanelBlockEntity) srv.overworld().getBlockEntity(new BlockPos(27, up, z + 10))).reset();
			return null;
		});
		context.waitTicks(40);
		String reclosed = read(server, 29, up, z + 9);
		check(reclosed.contains("CLOSED") && read(server, 35, up, z + 10).matches(".*taking [1-9].*"), "the panel's reset should close feeder 1 again: " + reclosed);
		// ---- gap wave: feeder 1's relay set from its relay compartment (curve VI), then a stuck mechanism: 50BF opens the whole bus
		String relaySet = server.computeOnServer(srv -> {
			dev.wildcatter.block.BreakerBlockEntity f = (dev.wildcatter.block.BreakerBlockEntity) srv.overworld().getBlockEntity(new BlockPos(29, up, z + 9));
			f.nextFeederSetting();   // the cursor to the curve
			f.stepFeederRelay();     // MI -> VI
			f.stickMechanism(true);
			return f.inspect().getString();
		});
		check(relaySet.contains("[VI]"), "the feeder relay's curve should step to VI: " + relaySet);
		grounds.accept(pastFeeder);
		context.waitTicks(40);
		String bf = read(server, 27, up, z + 10);
		String stuckFeeder = read(server, 29, up, z + 9);
		System.out.println("[wildcatter-test] busProtection: 50BF | " + bf + " | " + stuckFeeder + " | " + read(server, 24, up, z + 1));
		check(bf.contains("50BF BREAKER FAILURE") && bf.contains("LOCKED OUT"), "a feeder that fails to open should trip the bus by 50BF: " + bf);
		check(read(server, 24, up, z + 1).contains("TRIPPED") && read(server, 29, up, z + 12).contains("TRIPPED"), "50BF should open the generator and feeder 2");
		grounds.accept(pastFeeder);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.BreakerBlockEntity) srv.overworld().getBlockEntity(new BlockPos(29, up, z + 9))).setSwitch(false);   // opened by hand
			((dev.wildcatter.block.BusRelayPanelBlockEntity) srv.overworld().getBlockEntity(new BlockPos(27, up, z + 10))).reset();
			return null;
		});
		context.waitTicks(40);
		System.out.println("[wildcatter-test] busProtection ok: " + busTrip + " | " + feederTrip + " | " + reclosed + " | " + bf);
	}

	/**
	 * 10d2: a 4.16 kV switchgear lineup (docs/multiblocks/mv-switchgear.md). A genset and a backward unit substation feed the MAIN at
	 * x 22 (cable in low on its back); FEEDER 1 at x 23 serves a unit substation with four 15 kW heaters, FEEDER 3 at x 21 a dead-end
	 * cable for the fault. The sections face north, z-3..z-1.
	 */
	private void switchgear(ClientGameTestContext context, TestServerContext server) {
		final int z = -7580;
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		server.runCommand("tp @p 20 " + (up + 4) + " " + (z + 10) + " 180 20");
		context.waitTicks(20);
		server.runCommand("fill 2 " + (y - 1) + " " + (z - 10) + " 40 " + (y + 8) + " " + (z + 6) + " minecraft:air");
		server.runCommand("fill 2 " + (y - 1) + " " + (z - 10) + " 40 " + (y - 1) + " " + (z + 6) + " minecraft:stone");
		BlockPos set = new BlockPos(10, up, z);
		genset(server, set, north, false);
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, set, net.minecraft.core.Direction.EAST, false);   // 480 V up to 4.16 kV, x 11-14
		for (int x = 15; x <= 22; x++) {
			put(server, x, up, z, "wildcatter:cable_mv");
		}
		BlockPos feeder3 = new BlockPos(21, up, z - 3);
		BlockPos main = new BlockPos(22, up, z - 3);
		BlockPos feeder1 = new BlockPos(23, up, z - 3);
		placeBig(server, WildcatterBlocks.MV_SWITCHGEAR_FEEDER, feeder3, north);
		placeBig(server, WildcatterBlocks.MV_SWITCHGEAR_MAIN, main, north);
		placeBig(server, WildcatterBlocks.MV_SWITCHGEAR_FEEDER, feeder1, north);
		// feeder 1: out of its roof at the back, east and down to a unit substation (4.16 kV in at x 27), 480 V out at x 31 to heaters
		for (int x = 23; x <= 26; x++) {
			put(server, x, up + 3, z - 1, "wildcatter:cable_mv");
		}
		for (int yy = up; yy <= up + 2; yy++) {
			put(server, 26, yy, z - 1, "wildcatter:cable_mv");
		}
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, new BlockPos(26, up, z - 1), net.minecraft.core.Direction.EAST, true);   // x 27-30
		for (int x = 31; x <= 33; x++) {
			put(server, x, up, z - 1, "wildcatter:cable_industrial");
		}
		for (int[] at : new int[][] {{32, z - 2}, {33, z - 2}, {32, z}, {33, z}}) {
			put(server, at[0], up, at[1], "wildcatter:three_phase_heater");
		}
		// feeder 3: out of its roof and west, a dead-end cable
		for (int x = 19; x <= 21; x++) {
			put(server, x, up + 3, z - 1, "wildcatter:cable_mv");
		}
		java.util.function.Function<BlockPos, dev.wildcatter.block.SwitchgearSectionBlockEntity> section = at -> server.computeOnServer(srv ->
				(dev.wildcatter.block.SwitchgearSectionBlockEntity) srv.overworld().getBlockEntity(at));
		server.computeOnServer(srv -> {
			for (BlockPos at : new BlockPos[] {feeder3, main, feeder1}) {
				dev.wildcatter.block.SwitchgearSectionBlockEntity s = (dev.wildcatter.block.SwitchgearSectionBlockEntity) srv.overworld().getBlockEntity(at);
				s.setPickup(at.equals(main) ? 600 : at.equals(feeder1) ? 25 : 25);
				s.operate();
			}
			dev.wildcatter.circuit.CircuitManager.markDirty(srv.overworld());
			return null;
		});
		startGenset(context, server, set, north, 1000);
		context.waitTicks(120);
		String lineup = read(server, 22, up, z - 3);
		String f1 = read(server, 23, up, z - 3);
		System.out.println("[wildcatter-test] switchgear: " + lineup + " | " + f1 + " | " + read(server, 21, up, z - 3) + " | " + read(server, 10, up, z));
		check(lineup.contains("MAIN 2 of 3") && lineup.contains("1 main, 2 feeders") && lineup.contains("CLOSED, CONNECTED"), "the main should see its lineup: " + lineup);
		check(f1.contains("FEEDER 1 of 3") && f1.matches(".*CONNECTED, [5-9] A.*|.*CONNECTED, 1\\d A.*"), "feeder 1 should carry its heaters: " + f1);
		server.runCommand("tp @p 29 " + (up + 4) + " " + (z - 9) + " 40 18");   // from the north-east, onto the fronts of the lineup
		context.waitTicks(20);
		context.takeScreenshot("wildcatter-mv-switchgear");
		// ---- an overload: feeder 1's pickup set to 5 A, under its load; 51 trips it after its moderately inverse time
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.SwitchgearSectionBlockEntity) srv.overworld().getBlockEntity(feeder1)).setPickup(5);
			return null;
		});
		String tripped51 = waitFor(context, server, () -> section.apply(feeder1).tripped(), 600);
		String f1Trip = read(server, 23, up, z - 3);
		String mainOn = read(server, 22, up, z - 3);
		System.out.println("[wildcatter-test] switchgear: overload " + tripped51 + " | " + f1Trip + " | " + mainOn);
		check(tripped51.startsWith("yes") && f1Trip.contains("TRIPPED") && f1Trip.contains("51 time overcurrent"), "feeder 1's 51 should trip it on the overload: " + f1Trip);
		check(mainOn.contains("CLOSED, CONNECTED"), "the main should stay closed: " + mainOn);
		// right-click resets and closes it (pickup back to 25 A)
		server.computeOnServer(srv -> {
			dev.wildcatter.block.SwitchgearSectionBlockEntity s = (dev.wildcatter.block.SwitchgearSectionBlockEntity) srv.overworld().getBlockEntity(feeder1);
			s.setPickup(25);
			s.operate();
			dev.wildcatter.circuit.CircuitManager.markDirty(srv.overworld());
			return null;
		});
		context.waitTicks(60);
		String reclosed = read(server, 23, up, z - 3);
		check(reclosed.contains("CLOSED, CONNECTED") && reclosed.contains("last trip"), "a right-click should reset and close feeder 1: " + reclosed);
		// ---- a fault: the grounding set on feeder 3's cable; its 50 trips it alone
		BlockPos onCable = new BlockPos(19, up + 3, z - 1);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.MultiCableBlockEntity) srv.overworld().getBlockEntity(onCable)).setGrounded(true);
			return null;
		});
		context.waitTicks(20);
		String f3Trip = read(server, 21, up, z - 3);
		String mainStill = read(server, 22, up, z - 3);
		String f1Still = read(server, 23, up, z - 3);
		System.out.println("[wildcatter-test] switchgear: fault | " + f3Trip + " | " + mainStill + " | " + f1Still);
		check(f3Trip.contains("TRIPPED") && f3Trip.contains("50 instantaneous"), "grounds on feeder 3's cable should trip it by its 50: " + f3Trip);
		check(mainStill.contains("CLOSED, CONNECTED") && f1Still.contains("CLOSED, CONNECTED"), "a feeder fault should leave the main and feeder 1 closed: " + mainStill + " | " + f1Still);
		// ---- racking: feeder 3 racked out to DISCONNECTED cannot close
		boolean refused = server.computeOnServer(srv -> {
			dev.wildcatter.block.SwitchgearSectionBlockEntity s = (dev.wildcatter.block.SwitchgearSectionBlockEntity) srv.overworld().getBlockEntity(feeder3);
			s.rack();             // racking takes 15 s a position (gap wave): the test finishes each move at once
			s.finishRacking();
			s.rack();
			s.finishRacking();
			return !s.operate();
		});
		String racked = read(server, 21, up, z - 3);
		System.out.println("[wildcatter-test] switchgear: racked | " + racked);
		check(refused && racked.contains("DISCONNECTED") && !racked.contains("CLOSED"), "a breaker racked to DISCONNECTED should refuse to close: " + racked);
		System.out.println("[wildcatter-test] switchgear ok: " + lineup + " | " + f1Trip + " | " + f3Trip + " | " + racked);
	}

	/**
	 * 10d3: panels and switches (docs/multiblocks/panelboards.md, safety-switch.md, load-interrupter.md). A load centre on a pole unit
	 * with four gutters; a panelboard and a fused safety switch on a genset's 480 V; a fused and a plain 5 kV load-interrupter in a
	 * 4.16 kV run between two unit substations.
	 */
	private void panels(ClientGameTestContext context, TestServerContext server) {
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		java.util.function.Function<BlockPos, dev.wildcatter.block.PanelGutterBlockEntity> gutter = at -> server.computeOnServer(srv ->
				(dev.wildcatter.block.PanelGutterBlockEntity) srv.overworld().getBlockEntity(at));
		java.util.function.BiConsumer<BlockPos, String> clip = (at, item) -> server.computeOnServer(srv -> {
			dev.wildcatter.block.PanelGutterBlockEntity g = (dev.wildcatter.block.PanelGutterBlockEntity) srv.overworld().getBlockEntity(at);
			String refused = g.install(WildcatterItems.BRANCH_BREAKERS.get(item));
			check(refused == null, item + " should clip into the gutter at " + at.toShortString() + ": " + refused);
			g.setClosed(true);
			dev.wildcatter.circuit.CircuitManager.markDirty(srv.overworld());
			return null;
		});
		// ---- the load centre: a genset, a 480 V pad-mount run backward to 12.47 kV, a 25 kVA pole unit, 120/240 V to the panel
		final int z = -7700;
		server.runCommand("tp @p 22 " + (up + 3) + " " + (z - 8) + " 0 20");
		context.waitTicks(20);
		server.runCommand("fill 2 " + (y - 1) + " " + (z - 8) + " 34 " + (y + 8) + " " + (z + 8) + " minecraft:air");
		server.runCommand("fill 2 " + (y - 1) + " " + (z - 8) + " 34 " + (y - 1) + " " + (z + 8) + " minecraft:stone");
		BlockPos set = new BlockPos(10, up, z);
		genset(server, set, north, false);
		inlineTransformer(server, WildcatterBlocks.PADMOUNT_480, set, net.minecraft.core.Direction.EAST, true);   // 12.47 kV out at x 13
		for (int x = 13; x <= 15; x++) {
			put(server, x, up, z, "wildcatter:dist_wire_hot");
		}
		put(server, 16, up, z - 1, "wildcatter:pole");
		put(server, 16, up, z, "wildcatter:pole_transformer[facing=west]");
		// leg A and the neutral off its back, leg B and the neutral off its south side, joined in one cable up and over to the panel's top
		put(server, 17, up, z, "wildcatter:cable_lv");
		put(server, 16, up, z + 1, "wildcatter:cable_lv");
		put(server, 17, up, z + 1, "wildcatter:cable_lv");
		put(server, 17, up + 1, z + 1, "wildcatter:cable_lv");
		for (int x = 17; x <= 22; x++) {
			put(server, x, up + 2, z + 1, "wildcatter:cable_lv");
		}
		put(server, 22, up + 2, z + 2, "wildcatter:cable_lv");
		put(server, 22, up + 2, z + 3, "wildcatter:cable_lv");
		final int zp = z + 3;
		server.runCommand("fill 19 " + up + " " + (zp + 1) + " 25 " + (up + 3) + " " + (zp + 1) + " minecraft:bricks");   // the wall
		BlockPos centre = new BlockPos(22, up + 1, zp);
		placeBig(server, WildcatterBlocks.LOAD_CENTRE, centre, north);
		BlockPos g1 = new BlockPos(22, up, zp);        // space 1, below the panel: phase A
		BlockPos g2 = new BlockPos(23, up + 1, zp);    // space 2, left of it as you face it: phase B
		BlockPos g3 = new BlockPos(21, up + 1, zp);    // space 3, right of it: phase A (a 2-pole: A-B)
		BlockPos g4 = new BlockPos(23, up, zp);        // space 4, below space 2: phase B
		for (BlockPos g : new BlockPos[] {g1, g2, g3, g4}) {
			placeBig(server, WildcatterBlocks.PANEL_GUTTER, g, north);
		}
		// branch cable straight out of each gutter's front, north
		for (int zz = z + 1; zz <= z + 2; zz++) {
			put(server, 22, up, zz, "wildcatter:branch_cable[axis=z]");
			put(server, 23, up + 1, zz, "wildcatter:branch_cable[axis=z]");
		}
		put(server, 21, up + 1, z + 2, "wildcatter:branch_cable[axis=z]");
		for (int zz = z - 1; zz <= z + 2; zz++) {
			put(server, 23, up, zz, "wildcatter:branch_cable[axis=z]");
		}
		put(server, 22, up, z, "wildcatter:electric_heater");          // space 1: one 1,500 W heater
		put(server, 23, up + 1, z, "wildcatter:electric_lamp");        // space 2, the GFCI: a lamp
		put(server, 23, up, z - 2, "wildcatter:cable_lv");             // space 4: two heaters off a box at the end of its run
		put(server, 24, up, z - 2, "wildcatter:electric_heater");
		put(server, 23, up, z - 3, "wildcatter:electric_heater");
		clip.accept(g1, "branch_breaker_1p_20");
		clip.accept(g2, "gfci_breaker_1p_20");
		clip.accept(g3, "branch_breaker_2p_30");
		startGenset(context, server, set, north, 1000);
		context.waitTicks(80);
		String panel = read(server, centre.getX(), centre.getY(), centre.getZ());
		String s1 = read(server, g1.getX(), g1.getY(), g1.getZ());
		String s2 = read(server, g2.getX(), g2.getY(), g2.getZ());
		String s3 = read(server, g3.getX(), g3.getY(), g3.getZ());
		System.out.println("[wildcatter-test] panels: " + panel + " | " + s1 + " | " + s2 + " | " + s3 + " | " + read(server, 16, up, z));
		check(panel.contains("Load centre") && panel.contains("ON") && panel.contains("4 spaces of 40 used (3 breakers)"), "the load centre should count its breakers' poles: " + panel);
		check(s1.contains("Space 1 (phase A)") && s1.matches(".*ON, 1[0-3]\\.\\d A.*"), "space 1 should carry its heater on phase A: " + s1);
		check(s2.contains("Space 2 (phase B)") && s2.contains("GFCI") && s2.contains("imbalance 0.0 mA"), "space 2's GFCI should carry its lamp, balanced: " + s2);
		check(s3.contains("Space 3 (phase A-B)") && s3.matches(".*\\b2[2-4]\\d V across its poles.*"), "the 2-pole breaker should read about 240 V across its poles: " + s3);
		check(server.computeOnServer(srv -> srv.overworld().getBlockState(new BlockPos(23, up + 1, z)).getValue(LoadBlock.LIT)), "the GFCI's lamp should be lit");
		// a closed breaker won't come out
		boolean held = server.computeOnServer(srv -> ((dev.wildcatter.block.PanelGutterBlockEntity) srv.overworld().getBlockEntity(g1)).pull() == null);
		check(held, "a closed breaker should not come out of its gutter");
		server.runCommand("tp @p 21 " + (up + 2) + " " + z + " -15 12");   // from the north, onto the gutters' fronts and the panel
		context.waitTicks(20);
		context.takeScreenshot("wildcatter-panels");
		// ---- an overload: a 15 A breaker in space 4 with two 1,500 W heaters (25 A) trips on its thermal element; the main holds
		clip.accept(g4, "branch_breaker_1p_15");
		String trip4 = waitFor(context, server, () -> gutter.apply(g4).tripped(), 600);
		String s4 = read(server, g4.getX(), g4.getY(), g4.getZ());
		String mainOn = read(server, centre.getX(), centre.getY(), centre.getZ());
		System.out.println("[wildcatter-test] panels: overload " + trip4 + " | " + s4 + " | " + mainOn);
		check(s4.contains("Space 4 (phase B)") && s4.contains("TRIPPED (overload"), "the 15 A breaker should trip on two heaters: " + s4);
		check(mainOn.contains(": ON") && mainOn.contains("5 spaces of 40 used (4 breakers)"), "the main should stay on: " + mainOn);
		// ---- a ground fault: the grounding set on the GFCI's branch cable
		BlockPos gfciRun = new BlockPos(23, up + 1, z + 1);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.MultiCableBlockEntity) srv.overworld().getBlockEntity(gfciRun)).setGrounded(true);
			return null;
		});
		context.waitTicks(20);
		String gfci = read(server, g2.getX(), g2.getY(), g2.getZ());
		String s1Still = read(server, g1.getX(), g1.getY(), g1.getZ());
		System.out.println("[wildcatter-test] panels: ground fault | " + gfci + " | " + s1Still);
		check(gfci.contains("TRIPPED (ground fault"), "grounds on the GFCI's branch should trip it on ground fault: " + gfci);
		check(s1Still.contains("ON,"), "a ground fault on space 2 should leave space 1 on: " + s1Still);

		// ---- the panelboard and the safety switch on a genset's 480Y/277 V
		final int z2 = z - 30;
		server.runCommand("fill 2 " + (y - 1) + " " + (z2 - 8) + " 34 " + (y + 8) + " " + (z2 + 8) + " minecraft:air");
		server.runCommand("fill 2 " + (y - 1) + " " + (z2 - 8) + " 34 " + (y - 1) + " " + (z2 + 8) + " minecraft:stone");
		server.runCommand("tp @p 16 " + (up + 3) + " " + (z2 - 8) + " 0 20");
		context.waitTicks(20);
		BlockPos set2 = new BlockPos(10, up, z2);
		genset(server, set2, north, false);
		for (int x = 11; x <= 13; x++) {
			put(server, x, up, z2, "wildcatter:cable_industrial");
		}
		for (int zz = z2 + 1; zz <= z2 + 4; zz++) {
			put(server, 13, up, zz, "wildcatter:cable_industrial");
		}
		put(server, 14, up, z2 + 4, "wildcatter:cable_industrial");
		put(server, 15, up, z2 + 4, "wildcatter:cable_industrial");
		put(server, 16, up, z2 + 4, "wildcatter:cable_industrial");     // into the panelboard's back
		BlockPos board = new BlockPos(16, up, z2 + 3);
		placeBig(server, WildcatterBlocks.PANELBOARD_480, board, north);
		BlockPos b1 = new BlockPos(17, up, z2 + 3);   // space 1: a 3-pole 30 A, phases A-B-C
		BlockPos b2 = new BlockPos(15, up, z2 + 3);   // space 2: a 1-pole 277 V 20 A, phase B
		placeBig(server, WildcatterBlocks.PANEL_GUTTER, b1, north);
		placeBig(server, WildcatterBlocks.PANEL_GUTTER, b2, north);
		put(server, 17, up, z2 + 2, "wildcatter:branch_cable_industrial[axis=z]");
		put(server, 17, up, z2 + 1, "wildcatter:branch_cable_industrial[axis=z]");
		put(server, 17, up, z2, "wildcatter:cable_industrial");
		put(server, 18, up, z2, "wildcatter:three_phase_heater");
		put(server, 16, up, z2, "wildcatter:three_phase_heater");
		put(server, 17, up, z2 - 1, "wildcatter:three_phase_heater");
		put(server, 15, up, z2 + 2, "wildcatter:branch_cable_industrial[axis=z]");
		clip.accept(b2, "branch_breaker_277_1p_20");
		// the safety switch: teed off the feed at x 13, up and over into its top; its load out of its bottom to a heater
		for (int zz = z2 - 3; zz <= z2 - 1; zz++) {
			put(server, 13, up, zz, "wildcatter:cable_industrial");
		}
		put(server, 13, up + 1, z2 - 3, "wildcatter:cable_industrial");
		put(server, 13, up + 2, z2 - 3, "wildcatter:cable_industrial");
		put(server, 14, up + 2, z2 - 3, "wildcatter:cable_industrial");
		put(server, 15, up + 2, z2 - 3, "wildcatter:cable_industrial");
		BlockPos sw = new BlockPos(15, up + 1, z2 - 3);
		placeBig(server, WildcatterBlocks.SAFETY_SWITCH_100, sw, north);
		put(server, 15, up, z2 - 3, "wildcatter:cable_industrial");
		put(server, 16, up, z2 - 3, "wildcatter:three_phase_heater");
		java.util.function.Function<String, String> fitFuse = item -> server.computeOnServer(srv -> {
			String refused = ((dev.wildcatter.block.SafetySwitchBlockEntity) srv.overworld().getBlockEntity(sw)).fit(WildcatterItems.CARTRIDGE_FUSES.get(item));
			return refused == null ? "" : refused;
		});
		for (int k = 0; k < 3; k++) {
			check(fitFuse.apply("fuse_class_j_30").isEmpty(), "a 30 A class J fuse should fit the switch with its handle OFF");
		}
		check(!fitFuse.apply("fuse_class_j_30").isEmpty(), "a fourth fuse should not fit");
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.SafetySwitchBlockEntity) srv.overworld().getBlockEntity(sw)).setOn(true);
			return null;
		});
		startGenset(context, server, set2, north, 1000);
		context.waitTicks(60);
		String boardRead = read(server, board.getX(), board.getY(), board.getZ());
		String swRead = read(server, sw.getX(), sw.getY(), sw.getZ());
		System.out.println("[wildcatter-test] panels: " + boardRead + " | " + read(server, b2.getX(), b2.getY(), b2.getZ()) + " | " + swRead);
		check(boardRead.contains("Panelboard") && boardRead.contains("ON"), "the panelboard should be on: " + boardRead);
		check(swRead.contains("Safety switch") && swRead.matches(".*ON, 1[5-9] A.*") && swRead.contains("A good, B good, C good"), "the safety switch should carry its 15 kW heater: " + swRead);
		// an overload: a 3-pole 30 A breaker with three 15 kW heaters (about 54 A)
		clip.accept(b1, "branch_breaker_480_3p_30");
		String tripB1 = waitFor(context, server, () -> gutter.apply(b1).tripped(), 600);
		String b1Read = read(server, b1.getX(), b1.getY(), b1.getZ());
		System.out.println("[wildcatter-test] panels: panelboard overload " + tripB1 + " | " + b1Read);
		check(b1Read.contains("phase A-B-C") && b1Read.contains("TRIPPED (overload"), "the 3-pole 30 A breaker should trip on three heaters: " + b1Read);
		// a fault on the 277 V branch
		BlockPos run277 = new BlockPos(15, up, z2 + 2);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.MultiCableBlockEntity) srv.overworld().getBlockEntity(run277)).setGrounded(true);
			return null;
		});
		context.waitTicks(20);
		String b2Read = read(server, b2.getX(), b2.getY(), b2.getZ());
		System.out.println("[wildcatter-test] panels: 277 V fault | " + b2Read);
		check(b2Read.contains("phase B") && b2Read.contains("TRIPPED (short circuit"), "grounds on the 277 V branch should trip it on its magnetic element: " + b2Read);
		// a fault on the switch's load: its fuses blow and it names the phases
		BlockPos swLoad = new BlockPos(15, up, z2 - 3);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.MultiCableBlockEntity) srv.overworld().getBlockEntity(swLoad)).setGrounded(true);
			return null;
		});
		context.waitTicks(20);
		String blown = read(server, sw.getX(), sw.getY(), sw.getZ());
		System.out.println("[wildcatter-test] panels: switch fault | " + blown + " | " + read(server, set2.getX(), set2.getY(), set2.getZ()));
		check(blown.contains("BLOWN") && blown.contains("open: fix the fault"), "a fault on the switch's load should blow its fuses: " + blown);
		check(fitFuse.apply("fuse_class_j_30").startsWith("Handle OFF"), "a fuse should not fit with the handle ON");
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.MultiCableBlockEntity) srv.overworld().getBlockEntity(swLoad)).setGrounded(false);
			dev.wildcatter.block.SafetySwitchBlockEntity s = (dev.wildcatter.block.SafetySwitchBlockEntity) srv.overworld().getBlockEntity(sw);
			s.setOn(false);
			return null;
		});
		for (int k = 0; k < 3; k++) {
			fitFuse.apply("fuse_class_j_30");
		}
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.SafetySwitchBlockEntity) srv.overworld().getBlockEntity(sw)).setOn(true);
			return null;
		});
		context.waitTicks(40);
		String refitted = read(server, sw.getX(), sw.getY(), sw.getZ());
		System.out.println("[wildcatter-test] panels: switch refitted | " + refitted);
		check(refitted.matches(".*ON, 1[5-9] A.*") && !refitted.contains("BLOWN"), "new fuses should carry the heater again: " + refitted);

		// ---- the load-interrupters: 4.16 kV from a unit substation, through the fused LIS, then the plain one, to a unit substation
		final int z3 = z - 60;
		server.runCommand("fill 2 " + (y - 1) + " " + (z3 - 8) + " 40 " + (y + 8) + " " + (z3 + 8) + " minecraft:air");
		server.runCommand("fill 2 " + (y - 1) + " " + (z3 - 8) + " 40 " + (y - 1) + " " + (z3 + 8) + " minecraft:stone");
		server.runCommand("tp @p 22 " + (up + 4) + " " + (z3 - 10) + " 0 20");
		context.waitTicks(20);
		BlockPos set3 = new BlockPos(10, up, z3);
		genset(server, set3, north, false);
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, set3, net.minecraft.core.Direction.EAST, false);   // 4.16 kV out at x 15
		put(server, 15, up, z3, "wildcatter:cable_mv");
		for (int yy = up; yy <= up + 3; yy++) {
			put(server, 16, yy, z3, "wildcatter:cable_mv");
		}
		put(server, 16, up + 3, z3 - 1, "wildcatter:cable_mv");
		put(server, 16, up + 3, z3 - 2, "wildcatter:cable_mv");
		put(server, 17, up + 3, z3 - 2, "wildcatter:cable_mv");
		put(server, 18, up + 3, z3 - 2, "wildcatter:cable_mv");       // into the fused LIS's roof, at its back
		BlockPos fused = new BlockPos(18, up, z3 - 3);
		placeBig(server, WildcatterBlocks.LIS_5KV_FUSED, fused, north);
		put(server, 18, up, z3 - 1, "wildcatter:cable_mv");           // out of its back, low
		put(server, 19, up, z3 - 1, "wildcatter:cable_mv");
		for (int yy = up; yy <= up + 3; yy++) {
			put(server, 20, yy, z3 - 1, "wildcatter:cable_mv");
		}
		put(server, 20, up + 3, z3 - 2, "wildcatter:cable_mv");
		put(server, 21, up + 3, z3 - 2, "wildcatter:cable_mv");
		put(server, 22, up + 3, z3 - 2, "wildcatter:cable_mv");       // into the plain LIS's roof
		BlockPos plain = new BlockPos(22, up, z3 - 3);
		placeBig(server, WildcatterBlocks.LIS_5KV, plain, north);
		put(server, 22, up, z3 - 1, "wildcatter:cable_mv");
		put(server, 23, up, z3 - 1, "wildcatter:cable_mv");
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, new BlockPos(23, up, z3 - 1), net.minecraft.core.Direction.EAST, true);   // x 24-27
		for (int x = 28; x <= 30; x++) {
			put(server, x, up, z3 - 1, "wildcatter:cable_industrial");
		}
		put(server, 29, up, z3 - 2, "wildcatter:three_phase_heater");
		put(server, 30, up, z3, "wildcatter:three_phase_heater");
		java.util.function.Function<BlockPos, dev.wildcatter.block.LoadInterrupterBlockEntity> lis = at -> server.computeOnServer(srv ->
				(dev.wildcatter.block.LoadInterrupterBlockEntity) srv.overworld().getBlockEntity(at));
		String fusedRefused = server.computeOnServer(srv -> {
			dev.wildcatter.block.LoadInterrupterBlockEntity f = (dev.wildcatter.block.LoadInterrupterBlockEntity) srv.overworld().getBlockEntity(fused);
			for (int k = 0; k < 3; k++) {
				check(f.fit(WildcatterItems.CARTRIDGE_FUSES.get("fuse_e_10")) == null, "a 10E fuse should fit the open LIS");
			}
			f.setClosed(true);
			String refused = f.fit(WildcatterItems.CARTRIDGE_FUSES.get("fuse_e_10"));
			((dev.wildcatter.block.LoadInterrupterBlockEntity) srv.overworld().getBlockEntity(plain)).setClosed(true);
			return refused == null ? "" : refused;
		});
		check(fusedRefused.startsWith("Open the switch first"), "a fuse should not fit while the LIS is closed: " + fusedRefused);
		startGenset(context, server, set3, north, 1000);
		context.waitTicks(60);
		String fusedRead = read(server, fused.getX(), fused.getY(), fused.getZ());
		String plainRead = read(server, plain.getX(), plain.getY(), plain.getZ());
		System.out.println("[wildcatter-test] panels: LIS | " + fusedRead + " | " + plainRead);
		check(fusedRead.contains("fused 10E") && fusedRead.matches(".*CLOSED, [1-9] A.*") && fusedRead.contains("A good, B good, C good"), "the fused LIS should carry the load: " + fusedRead);
		check(plainRead.matches(".*CLOSED, [1-9] A.*"), "the plain LIS should carry the load: " + plainRead);
		server.runCommand("tp @p 24 " + (up + 3) + " " + (z3 - 8) + " 25 12");   // from the north-east, onto the two switches' fronts
		context.waitTicks(20);
		context.takeScreenshot("wildcatter-load-interrupter");
		// the plain LIS opens its load and closes again
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.LoadInterrupterBlockEntity) srv.overworld().getBlockEntity(plain)).setClosed(false);
			return null;
		});
		context.waitTicks(20);
		String opened = read(server, plain.getX(), plain.getY(), plain.getZ());
		check(opened.contains(": OPEN") && !lis.apply(plain).wrecked(), "the plain LIS should open its normal load: " + opened);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.LoadInterrupterBlockEntity) srv.overworld().getBlockEntity(plain)).setClosed(true);
			return null;
		});
		context.waitTicks(40);
		// with its load-break rating lowered under the load for the test, opening it wrecks it
		dev.wildcatter.block.LoadInterrupterBlockEntity.testLoadBreakAmps = 1.0;
		context.waitTicks(10);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.LoadInterrupterBlockEntity) srv.overworld().getBlockEntity(plain)).setClosed(false);
			return null;
		});
		dev.wildcatter.block.LoadInterrupterBlockEntity.testLoadBreakAmps = null;
		context.waitTicks(20);
		String wrecked = read(server, plain.getX(), plain.getY(), plain.getZ());
		System.out.println("[wildcatter-test] panels: LIS opened over its rating | " + wrecked);
		check(lis.apply(plain).wrecked() && wrecked.contains("WRECKED"), "a LIS opened over its load-break rating should be wrecked: " + wrecked);
		// a fault between the two: the fused LIS's fuses clear it
		BlockPos between = new BlockPos(19, up, z3 - 1);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.MultiCableBlockEntity) srv.overworld().getBlockEntity(between)).setGrounded(true);
			return null;
		});
		context.waitTicks(20);
		String cleared = read(server, fused.getX(), fused.getY(), fused.getZ());
		System.out.println("[wildcatter-test] panels: LIS fault | " + cleared + " | " + read(server, set3.getX(), set3.getY(), set3.getZ()));
		check(cleared.contains("BLOWN") && cleared.contains("open: fix the fault"), "the fused LIS's fuses should clear a fault on its load side: " + cleared);
		System.out.println("[wildcatter-test] panels ok: " + panel + " | " + s4 + " | " + gfci + " | " + b1Read + " | " + blown + " | " + wrecked + " | " + cleared);
	}

	/**
	 * 10d3: the transfer switch (docs/multiblocks/ats.md). Genset 1 is the normal source, genset 2 the emergency one, started by the
	 * ATS's redstone; the load is two 15 kW heaters.
	 */
	private void ats(ClientGameTestContext context, TestServerContext server) {
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		final int zn = -7820;
		final int ze = zn + 8;
		server.runCommand("fill 2 " + (y - 1) + " " + (zn - 6) + " 30 " + (y + 8) + " " + (ze + 8) + " minecraft:air");
		server.runCommand("fill 2 " + (y - 1) + " " + (zn - 6) + " 30 " + (y - 1) + " " + (ze + 8) + " minecraft:stone");
		server.runCommand("tp @p 20 " + (up + 3) + " " + (ze + 8) + " 150 20");
		context.waitTicks(20);
		BlockPos normalSet = new BlockPos(10, up, zn);
		BlockPos standby = new BlockPos(10, up, ze);
		genset(server, normalSet, north, false);
		genset(server, standby, north, false);
		// normal: out of genset 1 east, up and north into the ATS's top
		for (int x = 11; x <= 13; x++) {
			put(server, x, up, zn, "wildcatter:cable_industrial");
		}
		for (int yy = up + 1; yy <= up + 3; yy++) {
			put(server, 13, yy, zn, "wildcatter:cable_industrial");
		}
		for (int zz = zn + 1; zz <= ze + 1; zz++) {
			put(server, 13, up + 3, zz, "wildcatter:cable_industrial");
		}
		put(server, 12, up + 3, ze + 1, "wildcatter:cable_industrial");
		// emergency: out of genset 2 east, up and into the ATS's back (it faces east, so its back is west)
		put(server, 11, up, ze, "wildcatter:cable_industrial");
		put(server, 11, up + 1, ze, "wildcatter:cable_industrial");
		put(server, 11, up + 2, ze, "wildcatter:cable_industrial");
		put(server, 11, up + 2, ze + 1, "wildcatter:cable_industrial");
		// the load: out of the ATS's bottom to two heaters
		put(server, 12, up, ze + 1, "wildcatter:cable_industrial");
		put(server, 13, up, ze + 1, "wildcatter:cable_industrial");
		put(server, 14, up, ze + 1, "wildcatter:three_phase_heater");
		put(server, 13, up, ze + 2, "wildcatter:three_phase_heater");
		server.computeOnServer(srv -> {
			BlockPos tank = dev.wildcatter.block.GensetSkid.base(standby, north, false);
			((dev.wildcatter.block.GensetPartBlockEntity) srv.overworld().getBlockEntity(tank)).setFuel(1000);
			return null;
		});
		startGenset(context, server, normalSet, north, 1000);
		// the ATS goes in with normal live; its engine-start contact to genset 2's panel by a dot of redstone
		BlockPos ats = new BlockPos(12, up + 1, ze + 1);
		placeBig(server, WildcatterBlocks.ATS_480, ats, net.minecraft.core.Direction.EAST);
		put(server, 11, up, ze + 1, "minecraft:stone");
		put(server, 11, up + 1, ze + 1, "minecraft:redstone_wire");
		java.util.function.Supplier<dev.wildcatter.block.TransferSwitchBlockEntity> atsBe = () -> server.computeOnServer(srv ->
				(dev.wildcatter.block.TransferSwitchBlockEntity) srv.overworld().getBlockEntity(ats));
		context.waitTicks(5);
		// a new ATS runs the factory timers (TDEN 30 min, TDEC 5 min); this scene runs the short set (protection gap wave)
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.TransferSwitchBlockEntity) srv.overworld().getBlockEntity(ats)).setTimers(dev.wildcatter.block.TransferSwitchBlockEntity.Timers.SHORT);
			return null;
		});
		context.waitTicks(95);
		String onNormal = read(server, ats.getX(), ats.getY(), ats.getZ());
		System.out.println("[wildcatter-test] ats: " + onNormal + " | " + read(server, standby.getX(), up, standby.getZ()));
		check(onNormal.contains("on NORMAL") && onNormal.contains("engine start off") && onNormal.matches(".*NORMAL, [2-4]\\d A.*"), "the ATS should feed its load from normal: " + onNormal);
		check(!read(server, standby.getX(), up, standby.getZ()).contains("RUNNING"), "the standby set should be stopped");
		server.runCommand("tp @p 18 " + (up + 3) + " " + (ze + 6) + " 120 20");
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-ats");
		// normal fails: genset 1 stops
		server.computeOnServer(srv -> {
			panel(srv, normalSet).setWantRun(false);
			return null;
		});
		String toEmergency = waitFor(context, server, () -> atsBe.get().stage() == dev.wildcatter.block.TransferSwitchBlockEntity.Stage.EMERGENCY, 800);
		context.waitTicks(20);
		String onEmergency = read(server, ats.getX(), ats.getY(), ats.getZ());
		String standbyRun = read(server, standby.getX(), up, standby.getZ());
		System.out.println("[wildcatter-test] ats: normal lost, " + toEmergency + " | " + onEmergency + " | " + standbyRun);
		check(toEmergency.startsWith("yes") && onEmergency.contains("on EMERGENCY") && onEmergency.contains("engine start ON") && onEmergency.matches(".*EMERGENCY, [2-4]\\d A.*"),
				"the ATS should start genset 2 and transfer the load: " + onEmergency);
		check(standbyRun.contains("RUNNING"), "the standby set should run on the ATS's redstone: " + standbyRun);
		// normal returns: after TDEN it retransfers, after TDEC it drops the call and genset 2 stops
		server.computeOnServer(srv -> {
			panel(srv, normalSet).setWantRun(true);
			return null;
		});
		String back = waitFor(context, server, () -> atsBe.get().stage() == dev.wildcatter.block.TransferSwitchBlockEntity.Stage.TDEC, 1400);
		String cooling = read(server, ats.getX(), ats.getY(), ats.getZ());
		System.out.println("[wildcatter-test] ats: normal back, " + back + " | " + cooling);
		check(back.startsWith("yes") && cooling.contains("on NORMAL") && cooling.contains("engine cooldown"), "the ATS should retransfer after TDEN: " + cooling);
		String done = waitFor(context, server, () -> atsBe.get().stage() == dev.wildcatter.block.TransferSwitchBlockEntity.Stage.NORMAL, 900);
		context.waitTicks(40);
		String settled = read(server, ats.getX(), ats.getY(), ats.getZ());
		String standbyStopped = read(server, standby.getX(), up, standby.getZ());
		System.out.println("[wildcatter-test] ats: cooled down, " + done + " | " + settled + " | " + standbyStopped);
		check(done.startsWith("yes") && settled.contains("engine start off"), "after TDEC the ATS should drop its call: " + settled);
		check(!standbyStopped.contains("RUNNING"), "genset 2 should stop when the call drops: " + standbyStopped);
		check(!atsBe.get().paralleled(), "the two sources should never be closed together");
		System.out.println("[wildcatter-test] ats ok: " + onNormal + " | " + onEmergency + " | " + settled);
	}

	/** Waits (in client ticks of 10) until {@code done} holds or {@code limit} ticks pass; "yes after N ticks" or "NO after N ticks". */
	private String waitFor(ClientGameTestContext context, TestServerContext server, java.util.function.BooleanSupplier done, int limit) {
		for (int t = 0; t < limit; t += 10) {
			if (done.getAsBoolean()) {
				return "yes after " + t + " ticks";
			}
			context.waitTicks(10);
		}
		return done.getAsBoolean() ? "yes after " + limit + " ticks" : "NO after " + limit + " ticks";
	}

	private void dcTier(ClientGameTestContext context, TestServerContext server) {
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		net.minecraft.core.Direction south = net.minecraft.core.Direction.SOUTH;
		java.util.function.IntConsumer clear = z -> {
			server.runCommand("fill 0 " + (y - 1) + " " + (z - 8) + " 36 " + (y + 8) + " " + (z + 16) + " minecraft:air");
			server.runCommand("fill 0 " + (y - 1) + " " + (z - 8) + " 36 " + (y - 1) + " " + (z + 16) + " minecraft:stone");
		};
		server.runCommand("effect give @p minecraft:slow_falling 600 0 true");
		// ---- a magnesium cell 8 m from its rectifier, on 16 kA bus
		final int z = -4900;
		server.runCommand("tp @p 24 " + (up + 10) + " " + (z + 14) + " 180 35");
		context.waitTicks(20);
		clear.accept(z);
		BlockPos set = new BlockPos(10, up, z);
		genset(server, set, north, false);
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, set, net.minecraft.core.Direction.EAST, false);   // 4.16 kV out at x 15
		for (int x = 15; x <= 17; x++) {
			put(server, x, up, z, "wildcatter:cable_mv");
		}
		BlockPos rectifier = new BlockPos(18, up, z);
		placeBig(server, WildcatterBlocks.RECTIFIER, rectifier, north);   // x 18-20, z..z+3, its box west at x 17, + and - east at z+2
		for (int x = 21; x <= 28; x++) {
			put(server, x, up + 2, z + 2, "wildcatter:dc_bus_16k[axis=x]");   // +
			put(server, x, up, z + 2, "wildcatter:dc_bus_16k[axis=x]");       // -
		}
		BlockPos cell = new BlockPos(31, up, z + 5);
		placeBig(server, WildcatterBlocks.MAGNESIUM_CELL, cell, south);   // x 29-31, z..z+5, its terminals west at z+2
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.MagnesiumCellBlockEntity) srv.overworld().getBlockEntity(cell)).addMgCl2(Tuning.MG_CELL_HOLD_KG);
			dev.wildcatter.block.RectifierBlockEntity r = rectifier(srv, rectifier);
			for (int k = 0; k < 4; k++) {
				r.nextSetpoint();   // 6 kA to 14 kA, the magnesium cell's
			}
			return null;
		});
		startGenset(context, server, set, north, 2000);
		context.waitTicks(400);
		String run = read(server, rectifier.getX(), up, rectifier.getZ()) + " | " + read(server, cell.getX(), up, cell.getZ()) + " | + bus " + read(server, 24, up + 2, z + 2)
				+ " | - bus " + read(server, 24, up, z + 2) + " | " + read(server, set.getX(), up, set.getZ());
		System.out.println("[wildcatter-test] dcTier: bus run: " + run);
		double amps = server.computeOnServer(srv -> rectifier(srv, rectifier).dcAmps());
		check(Math.abs(amps - 14_000) < 14_000 * 0.02, "the rectifier should hold its 14 kA setpoint: " + amps + " | " + run);
		check(read(server, cell.getX(), up, cell.getZ()).contains("kg/h of magnesium"), "the cell should make magnesium on the bus: " + run);
		check(read(server, rectifier.getX(), up, rectifier.getZ()).contains("ground OK"), "the detector should read healthy: " + run);
		double plus = server.computeOnServer(srv -> rectifier(srv, rectifier).dc().plusToEarth());
		double line = server.computeOnServer(srv -> rectifier(srv, rectifier).dc().volts());
		check(line > 4 && line < 7 && Math.abs(plus - line / 2) < 0.5, "about 5 V, the + pole at half of it to earth: " + plus + " of " + line);
		server.runCommand("tp @p 25 " + (up + 5) + " " + (z + 9) + " 160 30");   // aimed just before the shot: the player falls between
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-dc-bus");

		// a ground wire against the - bus: a ground fault on -
		put(server, 24, up, z + 3, "wildcatter:ground_wire");
		context.waitTicks(40);
		String fault = read(server, rectifier.getX(), up, rectifier.getZ());
		check(fault.contains("GROUND FAULT on -"), "the detector should name the - pole: " + fault);
		check(!fault.contains("TRIPPED"), "a ground fault alarms but does not trip: " + fault);
		put(server, 24, up, z + 3, "minecraft:air");

		// a 4 kA bar in the - run carries the cell's 14 kA, and burns open
		put(server, 26, up, z + 2, "wildcatter:dc_bus_4k[axis=x]");
		boolean burnt = false;
		for (int i = 0; i < 60 && !burnt; i++) {
			context.waitTicks(20);
			burnt = isAir(server, 26, up, z + 2);
		}
		check(burnt, "a 4 kA bar carrying 14 kA should burn open: " + read(server, 26, up, z + 2) + " | " + read(server, rectifier.getX(), up, rectifier.getZ()));

		// open, it rises to 150 V; with - faulted to earth the + bus is at 150 V, and it shocks
		put(server, 22, up, z + 3, "wildcatter:ground_wire");
		context.waitTicks(300);
		String open = read(server, rectifier.getX(), up, rectifier.getZ());
		System.out.println("[wildcatter-test] dcTier: open and faulted: " + open + " | + bus " + read(server, 23, up + 2, z + 2));
		check(open.contains("DC 150 V") && open.contains("GROUND FAULT on -"), "open, it should stand at 150 V with - faulted: " + open);
		put(server, 23, up + 1, z + 1, "minecraft:stone");
		server.runCommand("summon minecraft:pig 23.5 " + (up + 2) + " " + (z + 1.5) + " {NoAI:1b,Tags:[\"dcshock\"]}");
		context.waitTicks(40);
		float health = server.computeOnServer(srv -> srv.overworld().getEntitiesOfClass(net.minecraft.world.entity.animal.pig.Pig.class,
				new net.minecraft.world.phys.AABB(new BlockPos(23, up + 2, z + 1)).inflate(2), e -> e.entityTags().contains("dcshock")).stream()
				.findFirst().map(net.minecraft.world.entity.LivingEntity::getHealth).orElse(-1F));
		check(health >= 0 && health < 10F, "the + bus at 150 V to earth should shock the pig beside it: " + health);
		server.runCommand("kill @e[tag=dcshock]");
		// a bar across the + and - runs is a short through its 0.1 mOhm, far past its setpoint with its terminals collapsed: it trips
		put(server, 24, up + 1, z + 2, "wildcatter:dc_bus_16k[axis=y]");
		context.waitTicks(40);
		String shorted = read(server, rectifier.getX(), up, rectifier.getZ());
		System.out.println("[wildcatter-test] dcTier: short: " + shorted);
		check(shorted.contains("TRIPPED (DC overcurrent"), "a short across the 150 V bus should trip the rectifier: " + shorted);

		// ---- a magnesium cell set against its rectifier, as in 7f: no bus, its 14 kA taken from the cell
		final int z2 = -4960;
		server.runCommand("tp @p 22 " + (up + 10) + " " + (z2 + 14) + " 180 35");
		context.waitTicks(20);
		clear.accept(z2);
		BlockPos set2 = new BlockPos(10, up, z2);
		genset(server, set2, north, false);
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, set2, net.minecraft.core.Direction.EAST, false);
		for (int x = 15; x <= 17; x++) {
			put(server, x, up, z2, "wildcatter:cable_mv");
		}
		BlockPos rectifier2 = new BlockPos(18, up, z2);
		placeBig(server, WildcatterBlocks.RECTIFIER, rectifier2, north);   // x 18-20
		BlockPos cell2 = new BlockPos(21, up, z2);
		placeBig(server, WildcatterBlocks.MAGNESIUM_CELL, cell2, north);   // x 21-23, against the rectifier's east side
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.MagnesiumCellBlockEntity) srv.overworld().getBlockEntity(cell2)).addMgCl2(Tuning.MG_CELL_HOLD_KG);
			return null;
		});
		startGenset(context, server, set2, north, 2000);
		context.waitTicks(400);
		String touching = read(server, rectifier2.getX(), up, rectifier2.getZ()) + " | " + read(server, cell2.getX(), up, cell2.getZ());
		System.out.println("[wildcatter-test] dcTier: touching: " + touching);
		double amps2 = server.computeOnServer(srv -> rectifier(srv, rectifier2).dcAmps());
		check(touching.contains("setpoint 14 kA, the cell's") && Math.abs(amps2 - 14_000) < 14_000 * 0.02, "set against its cell, the rectifier should take its 14 kA: " + amps2 + " | " + touching);
		check(read(server, cell2.getX(), up, cell2.getZ()).contains("kg/h of magnesium"), "the touching cell should make magnesium: " + touching);
		System.out.println("[wildcatter-test] dcTier ok");
	}

	private dev.wildcatter.block.RectifierBlockEntity rectifier(net.minecraft.server.MinecraftServer srv, BlockPos at) {
		return (dev.wildcatter.block.RectifierBlockEntity) srv.overworld().getBlockEntity(at);
	}

	/** Presses a starter cabinet's start button, on the server thread. */
	private void press(TestServerContext server, BlockPos main) {
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.StarterCabinetBlockEntity) srv.overworld().getBlockEntity(main)).pushButton();
			return null;
		});
	}

	/** Waits {@code ticks}, calling on the motors every 10 ticks as their units would, so they want to run. */
	private void runMotors(ClientGameTestContext context, TestServerContext server, java.util.List<BlockPos> motors, int ticks) {
		for (int t = 0; t < ticks; t += 10) {
			server.computeOnServer(srv -> {
				for (BlockPos m : motors) {
					if (srv.overworld().getBlockEntity(m) instanceof dev.wildcatter.block.UnitMotorBlockEntity motor) {
						motor.drive(srv.overworld());
					}
				}
				return null;
			});
			context.waitTicks(10);
		}
	}

	/** A real Team Reborn Energy storage beside a plug: found through the API, filled tick by tick at its own rate, and drawing nothing once full. */
	private static final team.reborn.energy.api.base.SimpleEnergyStorage TR_STORAGE = new team.reborn.energy.api.base.SimpleEnergyStorage(1_000_000, 1000, 0);

	private void teamRebornEnergy(ClientGameTestContext context, TestServerContext server) {
		int z = -4300;
		server.runCommand("tp @p 14 " + (up + 1) + " " + (z + 5) + " 180 15");   // load the scene's chunks first
		context.waitTicks(20);
		// a gold block stands in for a Tech Reborn battery: a 1,000,000 E storage that takes 1,000 E a tick
		team.reborn.energy.api.EnergyStorage.SIDED.registerForBlocks((level, pos, state, entity, side) -> TR_STORAGE, net.minecraft.world.level.block.Blocks.GOLD_BLOCK);
		server.runCommand("fill 8 " + y + " " + (z - 3) + " 22 " + (y + 5) + " " + (z + 4) + " minecraft:air");
		plant(server, z);
		put(server, 12, up, z, "wildcatter:wool_wire_hot");
		for (int x = 11; x <= 13; x++) {
			put(server, x, up, z + 1, "wildcatter:wool_wire_neutral");
		}
		server.runCommand("fill 13 " + (up - 1) + " " + z + " 15 " + (up - 1) + " " + z + " minecraft:smooth_stone");   // a bench to stand them on
		put(server, 13, up, z, "wildcatter:receptacle_nema_5_15[attach=down]");
		put(server, 14, up, z, "wildcatter:plug_nema_5_15[attach=down]");
		put(server, 15, up, z, "minecraft:gold_block");
		server.runCommand("tp @p 14 " + (up + 1) + " " + (z + 4) + " 180 25");
		context.waitTicks(160);
		long before = TR_STORAGE.amount;
		context.waitTicks(100);
		long gained = TR_STORAGE.amount - before;
		String plug = read(server, 14, up, z);
		check(plug.contains("E/t") && gained > 70_000 && gained < 120_000, "a plug should fill a Team Reborn storage at its 1,000 E a tick (" + gained + " E in 100 ticks): " + plug);
		check(plug.contains(" 8.") || plug.contains(" 7.") || plug.contains(" 9."), "1,000 W at 120 V is about 8.3 A: " + plug);
		server.runCommand("tp @p 14.5 " + (up + 0.3) + " " + (z + 2.6) + " 180 35");
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-team-reborn-energy");

		// full, it asks for nothing and the plug draws nothing
		server.runOnServer(srv -> TR_STORAGE.amount = TR_STORAGE.getCapacity());
		context.waitTicks(80);
		String full = read(server, 14, up, z);
		check(full.contains("delivering 0 W") && full.contains(", 0.0 A"), "a full storage should draw nothing: " + full);
		System.out.println("[wildcatter-test] team reborn energy ok: " + plug + " | " + full + " | E gained " + gained);
	}

	/** An under-frequency relay on a 12.47 kV feeder sheds a pole transformer's overload and keeps the generator on line. */
	private void feederRelay(ClientGameTestContext context, TestServerContext server) {
		int z = -140;
		server.runCommand("fill 8 " + (y - 1) + " " + (z - 3) + " 24 " + (y + 5) + " " + (z + 4) + " minecraft:air");
		server.runCommand("fill 8 " + (y - 1) + " " + (z - 3) + " 24 " + (y - 1) + " " + (z + 4) + " minecraft:stone");
		plant(server, z);
		stepUp(server, z);
		for (int x = 15; x <= 16; x++) {
			put(server, x, up, z, "wildcatter:dist_wire_hot");
		}
		// 10d2: a yard breaker (x 17-18, z-1..z+1, terminals on z) worked by a relay cabinet north of it, with 81U
		placeBig(server, WildcatterBlocks.DIST_BREAKER_3P, new BlockPos(17, up, z - 1), net.minecraft.core.Direction.NORTH);
		placeBig(server, WildcatterBlocks.BREAKER_RELAY_CABINET, new BlockPos(17, up, z - 2), net.minecraft.core.Direction.NORTH);
		put(server, 19, up, z, "wildcatter:dist_wire_hot");
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
		String relay = read(server, 17, up, z - 2);
		String turbine = read(server, 11, up, z);
		check(relay.contains("TRIPPED") && relay.contains("81U under-frequency"), "the cabinet's 81U should shed the overload: " + relay);
		check(read(server, 17, up, z - 1).contains("TRIPPED"), "81U should open the breaker: " + read(server, 17, up, z - 1));
		check(!turbine.contains("TRIPPED"), "with the feeder shed the turbine should stay on line: " + turbine);
		// the heaters gone and the frequency back, a right-click at the cabinet recloses the breaker
		put(server, 21, up, z, "wildcatter:electric_lamp");
		put(server, 20, up, z + 1, "minecraft:air");
		put(server, 20, y, z, "minecraft:air");
		context.waitTicks(200);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.BreakerRelayCabinetBlockEntity) srv.overworld().getBlockEntity(new BlockPos(17, up, z - 2))).reset();
			return null;
		});
		context.waitTicks(60);
		String reclosed = read(server, 17, up, z - 2);
		check(reclosed.contains("CLOSED") && reclosed.contains("last trip"), "the cabinet's reset should reclose the breaker: " + reclosed + " | " + read(server, 11, up, z));
		System.out.println("[wildcatter-test] feeder relay ok: " + relay + " | " + reclosed);
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
		server.runCommand("tp @p 26 " + (up + 20) + " " + (z0 + 32) + " 0 90");   // load the scene's chunks first
		context.waitTicks(20);
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
		dev.wildcatter.energy.EnergyBridge.clearTests();
		dev.wildcatter.energy.EnergyBridge.registerForTest(target -> target.level().getBlockState(target.pos()).is(net.minecraft.world.level.block.Blocks.IRON_BLOCK)
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
			dev.wildcatter.energy.EnergyBridge.clearTests();
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
	private static final String PLC_PROJECT = """
			(* Start and stop a motor from a panel, with a latched e-stop. *)
			PROGRAM MAIN
			VAR
				start AT %IX0.0 : BOOL;     (* PB-101 *)
				healthy AT %IX0.1 : BOOL;   (* ES-101, normally closed *)
				running AT %IX0.2 : BOOL;   (* the starter's feedback *)
				tripped AT %IX0.3 : BOOL;
				coil AT %QX16.0 : BOOL;     (* the starter's RUN *)
				lamp AT %QX16.1 : BOOL;     (* the pilot light *)
				latch : BOOL;
			END_VAR
			IF start AND healthy THEN
				latch := TRUE;
			END_IF;
			IF NOT healthy OR tripped THEN
				latch := FALSE;
			END_IF;
			coil := latch;
			lamp := running;
			END_PROGRAM
			""";

	/** Phase 13b (docs/multiblocks/plc.md): a real pushbutton, through a real structured text program and real wiring, starts a real motor. */
	private void plc(ClientGameTestContext context, TestServerContext server) {
		int z = -3000;
		int px = 17;
		server.runCommand("tp @p 15 " + (up + 2) + " " + (z + 11) + " 180 15");
		context.waitTicks(20);
		server.runCommand("fill 2 " + (y - 1) + " " + (z - 6) + " 34 " + (y + 6) + " " + (z + 12) + " minecraft:air");
		server.runCommand("fill 2 " + (y - 1) + " " + (z - 6) + " 34 " + (y - 1) + " " + (z + 12) + " minecraft:stone");
		long seed = -1;
		for (long s = 1; s < 2_000_000 && seed < 0; s++) {
			Reservoir oil = Reservoir.at(s, px + 6, z);
			if (oil != null && oil.kind() == Reservoir.Kind.OIL && oil.units() > 10_000) {
				seed = s;
			}
		}
		check(seed >= 0, "no seed puts an oil field under the PLC scene's pumpjack");
		ReservoirData.testSeed = seed;
		Filming film = null;
		try {
			// the 480 V line: as the starter's own scene
			BlockPos set = new BlockPos(12, up, z);
			genset(server, set, net.minecraft.core.Direction.NORTH, false);
			put(server, 13, up, z, "wildcatter:cable_industrial");
			put(server, 14, up, z, "wildcatter:motor_starter[facing=east]");
			put(server, 15, up, z, "wildcatter:cable_industrial");
			put(server, 16, up, z, "wildcatter:cable_industrial");
			pumpjack(server, new BlockPos(px, up, z), net.minecraft.core.Direction.EAST);
			// 120 V for the control supply, from a small steam set
			plant(server, z + 5);
			put(server, 12, up, z + 5, "wildcatter:plastic_wire_hot");
			put(server, 13, up, z + 5, "wildcatter:control_supply");
			for (int x = 11; x <= 13; x++) {
				put(server, x, up, z + 6, "wildcatter:plastic_wire_neutral");
			}
			// the panel: a box beside the supply and cabinet with the operator devices on it, and a run north to the starter
			put(server, 14, up, z + 5, "wildcatter:junction_box");
			put(server, 14, up + 1, z + 5, "wildcatter:junction_box");
			put(server, 15, up, z + 5, "wildcatter:plc_cabinet[facing=south]");
			server.runOnServer(srv -> dev.wildcatter.multiblock.BigParts.grow(srv.overworld(), new BlockPos(15, up, z + 5), srv.overworld().getBlockState(new BlockPos(15, up, z + 5))));
			for (int zz = z + 1; zz <= z + 4; zz++) {
				put(server, 14, up, zz, "wildcatter:control_cable");
			}
			put(server, 14, up, z + 6, "wildcatter:pushbutton[facing=south]");
			put(server, 13, up + 1, z + 5, "wildcatter:estop[facing=south]");
			put(server, 14, up + 1, z + 6, "wildcatter:pilot_light[facing=south]");
			put(server, 14, up + 2, z + 5, "wildcatter:stack_light[facing=south]");
			startGenset(context, server, set, net.minecraft.core.Direction.NORTH, 1000);
			context.waitTicks(40);
			BlockPos at = new BlockPos(15, up, z + 5);
			BlockPos button = new BlockPos(14, up, z + 6);
			BlockPos estop = new BlockPos(13, up + 1, z + 5);
			BlockPos starterAt = new BlockPos(14, up, z);
			BlockPos lamp = new BlockPos(14, up + 1, z + 6);
			java.util.function.Function<net.minecraft.server.MinecraftServer, dev.wildcatter.control.PlcCabinetBlockEntity> cabinet = srv -> (dev.wildcatter.control.PlcCabinetBlockEntity) srv.overworld().getBlockEntity(at);
			java.util.function.Supplier<String> status = () -> read(server, 15, up, z + 5);
			check(status.get().contains("NO CPU"), "a new cabinet has no CPU: " + status.get());
			String refused = server.computeOnServer(srv -> cabinet.apply(srv).land(button, 0, "%IX0.0"));
			check(refused != null && refused.contains("no DI16"), "a signal should not land on a module that is not there: " + refused);
			String wired = server.computeOnServer(srv -> {
				dev.wildcatter.control.PlcCabinetBlockEntity box = cabinet.apply(srv);
				box.seat(dev.wildcatter.control.PlcModuleItem.Type.CPU_SMALL);
				box.seat(dev.wildcatter.control.PlcModuleItem.Type.DI16);
				box.seat(dev.wildcatter.control.PlcModuleItem.Type.DO16);
				StringBuilder errors = new StringBuilder();
				for (String err : new String[] {box.land(button, 0, "%IX0.0"), box.land(estop, 0, "%IX0.1"), box.land(starterAt, 1, "%IX0.2"), box.land(starterAt, 2, "%IX0.3"), box.land(starterAt, 0, "%QX16.0"), box.land(lamp, 0, "%QX16.1")}) {
					if (err != null) {
						errors.append(err).append("; ");
					}
				}
				box.setTag(button, "PB-101");
				return errors.toString();
			});
			check(wired.isEmpty(), "every signal should land: " + wired);
			String wrong = server.computeOnServer(srv -> cabinet.apply(srv).land(button, 0, "%QX16.5"));
			check(wrong != null && wrong.contains("input bit"), "an input cannot land on an output: " + wrong);
			server.runOnServer(srv -> cabinet.apply(srv).land(button, 0, "%IX0.0"));
			context.waitTicks(30);
			check(status.get().contains("devices reached") && !status.get().contains("NO 24 V"), "the supply should reach the cabinet: " + status.get());
			film = context.computeOnClient(client -> Filming.start(client, "plc"));
			context.takeScreenshot("wildcatter-plc-panel");
			// the modules seated, drawn in the door's window (plc-gaps): the client has the rail, and a close look shows it
			check(context.computeOnClient(client -> client.level.getBlockEntity(at) instanceof dev.wildcatter.control.PlcCabinetBlockEntity c
					&& c.cpuModule() == dev.wildcatter.control.PlcModuleItem.Type.CPU_SMALL && c.module(0) == dev.wildcatter.control.PlcModuleItem.Type.DI16
					&& c.module(1) == dev.wildcatter.control.PlcModuleItem.Type.DO16 && c.module(2) == null), "the client should know the modules seated");
			server.runCommand("tp @p 15.5 " + up + " " + (z + 7.3) + " 180 -2");
			context.waitTicks(10);
			context.takeScreenshot("wildcatter-plc-rack");
			server.runCommand("tp @p 15 " + (up + 2) + " " + (z + 11) + " 180 15");
			context.waitTicks(5);
			// the engineering station: typed into the real editor and downloaded over the real packet
			server.runOnServer(srv -> cabinet.apply(srv).open(srv.getPlayerList().getPlayers().get(0)));
			context.waitTicks(20);
			check(context.computeOnClient(client -> client.gui.screen() instanceof dev.wildcatter.client.PlcScreen), "the cabinet's screen should open");
			context.getInput().typeChars(PLC_PROJECT.substring(0, 60).replace("\n", " "));
			context.waitTicks(10);
			context.runOnClient(client -> ((dev.wildcatter.client.PlcScreen) client.gui.screen()).replaceForTest(PLC_PROJECT + "\nthis is not structured text"));
			context.runOnClient(client -> ((dev.wildcatter.client.PlcScreen) client.gui.screen()).check());
			context.waitTicks(10);
			check(context.computeOnClient(client -> !((dev.wildcatter.client.PlcScreen) client.gui.screen()).errorsForTest().isEmpty()), "CHECK should list the error");
			context.takeScreenshot("wildcatter-plc-error");
			context.runOnClient(client -> ((dev.wildcatter.client.PlcScreen) client.gui.screen()).replaceForTest(PLC_PROJECT));
			// the real button with the real mouse, down the game's whole input path (the left button is number 1 in this version). A click finds a button whether or not it can be seen, so the screenshots are what show one is not hidden
			int[] downloadAt = context.computeOnClient(client -> ((dev.wildcatter.client.PlcScreen) client.gui.screen()).buttonForTest("DOWNLOAD"));
			check(downloadAt != null, "the DOWNLOAD button should be there");
			double toWindow = context.computeOnClient(client -> (double) client.getWindow().getScreenWidth() / client.getWindow().getGuiScaledWidth());
			context.getInput().setCursorPos(downloadAt[0] * toWindow, downloadAt[1] * toWindow);
			context.waitTick();
			context.getInput().pressMouse(1);
			context.waitTicks(20);
			check(server.computeOnServer(srv -> cabinet.apply(srv).project().getFirst().text().equals(PLC_PROJECT)), "the download should reach the cabinet");
			context.takeScreenshot("wildcatter-plc-editor");
			context.runOnClient(client -> ((dev.wildcatter.client.PlcScreen) client.gui.screen()).showTabForTest("TERMINALS"));
			context.waitTicks(15);
			context.takeScreenshot("wildcatter-plc-terminals");
			context.runOnClient(client -> client.gui.screen().onClose());
			context.waitTicks(10);
			check(status.get().contains("STOP"), "a download leaves the CPU in STOP: " + status.get());
			// AUTO: the selector; RUN: the key
			java.util.function.Supplier<String> run = () -> read(server, 14, up, z);
			server.runOnServer(srv -> {
				starter(srv, 14, up, z).pushButton();
			});
			context.waitTicks(20);
			check(run.get().contains("RUNNING"), "in HAND the starter's own button still works: " + run.get());
			server.runOnServer(srv -> {
				starter(srv, 14, up, z).cycleHoa(starterAt);
			});
			context.waitTicks(10);
			check(run.get().contains("[OFF]") && run.get().contains("STOPPED"), "OFF stops it: " + run.get());
			server.runOnServer(srv -> {
				starter(srv, 14, up, z).cycleHoa(starterAt);
			});
			server.runOnServer(srv -> cabinet.apply(srv).cpu().setMode(dev.wildcatter.plc.Cpu.Mode.RUN));
			context.waitTicks(20);
			check(run.get().contains("[AUTO]") && run.get().contains("STOPPED"), "AUTO with nothing commanded stands still: " + run.get());
			server.runOnServer(srv -> {
				starter(srv, 14, up, z).pushButton();
			});
			context.waitTicks(10);
			check(run.get().contains("STOPPED"), "in AUTO the starter's own button does nothing: " + run.get());
			// the operator's button
			server.runOnServer(srv -> ((dev.wildcatter.control.OperatorDeviceBlockEntity) srv.overworld().getBlockEntity(button)).operate());
			context.waitTicks(160);
			check(run.get().contains("RUNNING") && read(server, px, up, z).contains("pumping"), "the pushbutton should start the motor through the program: " + run.get() + " | " + read(server, px, up, z));
			check(server.computeOnServer(srv -> srv.overworld().getBlockState(lamp).getValue(dev.wildcatter.control.OperatorDeviceBlock.LAMPS)) == 1, "the pilot light should show it running");
			context.takeScreenshot("wildcatter-plc-running");
			// the e-stop drops it and holds it down
			server.runOnServer(srv -> ((dev.wildcatter.control.OperatorDeviceBlockEntity) srv.overworld().getBlockEntity(estop)).operate());
			context.waitTicks(30);
			check(run.get().contains("STOPPED"), "the e-stop should stop it: " + run.get());
			server.runOnServer(srv -> ((dev.wildcatter.control.OperatorDeviceBlockEntity) srv.overworld().getBlockEntity(button)).operate());
			context.waitTicks(30);
			check(run.get().contains("STOPPED"), "a start should do nothing with the e-stop in: " + run.get());
			server.runOnServer(srv -> ((dev.wildcatter.control.OperatorDeviceBlockEntity) srv.overworld().getBlockEntity(estop)).operate());
			context.waitTicks(10);
			server.runOnServer(srv -> ((dev.wildcatter.control.OperatorDeviceBlockEntity) srv.overworld().getBlockEntity(button)).operate());
			context.waitTicks(60);
			check(run.get().contains("RUNNING"), "released, a start should work again: " + run.get());
			// a cut cable to the starter: it stops at once, as it has heard no command, and the latch is still set, so mending the cable starts it again
			put(server, 14, up, z + 3, "minecraft:air");
			context.waitTicks(40);
			check(run.get().contains("STOPPED"), "a cut cable should stop the motor: " + run.get());
			context.takeScreenshot("wildcatter-plc-cut");
			put(server, 14, up, z + 3, "wildcatter:control_cable");
			context.waitTicks(40);
			check(run.get().contains("RUNNING"), "the panel's cable was not the one cut, so its latch still commands the motor: " + run.get());
			// a T in a cable is a bad joint, named at the cabinet
			put(server, 13, up, z + 3, "wildcatter:control_cable");
			context.waitTicks(40);
			check(status.get().contains("BAD JOINT") && run.get().contains("STOPPED"), "a T in a cable should be a bad joint: " + status.get() + " | " + run.get());
			put(server, 13, up, z + 3, "minecraft:air");
			context.waitTicks(40);
			// a CPU fault drops everything, and names the line
			server.runOnServer(srv -> {
				dev.wildcatter.control.PlcCabinetBlockEntity box = cabinet.apply(srv);
				box.download("PROGRAM MAIN\nVAR coil AT %QX16.0 : BOOL; i : DINT; END_VAR\ncoil := TRUE;\nWHILE TRUE DO i := i + 1; END_WHILE;\nEND_PROGRAM");
				box.cpu().setMode(dev.wildcatter.plc.Cpu.Mode.RUN);
			});
			context.waitTicks(30);
			check(status.get().contains("CPU FAULT") && run.get().contains("STOPPED"), "a runaway program should fault and drop the starter: " + status.get() + " | " + run.get());
			check(server.computeOnServer(srv -> cabinet.apply(srv).cpu().diagnostics().getLast().toString()).contains("watchdog"), "the buffer should say why");
			// so many direct-on-line starts heat the overload relay for real: let it cool, and reset it by hand if it tripped, which even AUTO allows
			context.waitTicks(1200);
			if (run.get().contains("TRIPPED")) {
				server.runOnServer(srv -> {
					starter(srv, 14, up, z).pushButton();
				});
				check(run.get().contains("[AUTO]") && !run.get().contains("TRIPPED"), "a trip's hand reset should work in AUTO: " + run.get());
			}
			// back on the real project, then lose the supply
			server.runOnServer(srv -> {
				dev.wildcatter.control.PlcCabinetBlockEntity box = cabinet.apply(srv);
				box.download(PLC_PROJECT);
				box.cpu().setMode(dev.wildcatter.plc.Cpu.Mode.RUN);
			});
			context.waitTicks(10);
			server.runOnServer(srv -> ((dev.wildcatter.control.OperatorDeviceBlockEntity) srv.overworld().getBlockEntity(button)).operate());
			context.waitTicks(60);
			check(run.get().contains("RUNNING"), "the real project should run it again: " + run.get());
			plcLadder(context, server, cabinet, status, run, button, estop, new BlockPos(14, up + 2, z + 5));
			// a save and load keeps the project, the terminals and the latch
			boolean kept = server.computeOnServer(srv -> {
				dev.wildcatter.control.PlcCabinetBlockEntity box = cabinet.apply(srv);
				var registries = srv.registryAccess();
				net.minecraft.nbt.CompoundTag saved = box.saveCustomOnly(registries);
				dev.wildcatter.control.PlcCabinetBlockEntity copy = new dev.wildcatter.control.PlcCabinetBlockEntity(at, box.getBlockState());
				copy.setLevel(srv.overworld());
				copy.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, registries, saved));
				dev.wildcatter.control.PlcCabinetBlockEntity.tick(srv.overworld(), at, box.getBlockState(), copy);
				return copy.project().equals(box.project()) && copy.cpu() != null && Boolean.TRUE.equals(copy.cpu().read("POU2.COIL")) && copy.cpu().mode() == dev.wildcatter.plc.Cpu.Mode.RUN;
			});
			check(kept, "a save and load should keep the project and the latch");
			put(server, 13, up, z + 5, "minecraft:air");
			context.waitTicks(40);
			check(status.get().contains("NO 24 V") && run.get().contains("STOPPED"), "no 24 V should stop the motor: " + status.get() + " | " + run.get());
			put(server, 13, up, z + 5, "wildcatter:control_supply");
			context.waitTicks(60);
			check(status.get().contains("RUN"), "the supply back, the CPU restarts: " + status.get());
			check(run.get().contains("STOPPED"), "and the latch was not RETAIN, so the motor stays stopped until an operator starts it: " + run.get());
			System.out.println("[wildcatter-test] plc ok: " + status.get());
		} finally {
			ReservoirData.testSeed = null;
			if (film != null) {
				film.stop();
			}
		}
	}

	/** Clicks the sheet at a place in GUI pixels, with the real mouse. */
	private void plcMouse(ClientGameTestContext context, int[] at) {
		double toWindow = context.computeOnClient(client -> (double) client.getWindow().getScreenWidth() / client.getWindow().getGuiScaledWidth());
		context.getInput().setCursorPos(at[0] * toWindow, at[1] * toWindow);
		context.waitTick();
		context.getInput().pressMouse(1);
		context.waitTick();
	}

	private static final String BELT_VARS = """
			VAR
				start_pb AT %IX0.0 : BOOL;
				stop_pb AT %IX0.1 : BOOL;
				estop_ok AT %IX0.2 : BOOL;
				run3 AT %IX0.3 : BOOL;
				trip3 AT %IX0.4 : BOOL;
				auto3 AT %IX0.5 : BOOL;
				run2 AT %IX0.6 : BOOL;
				trip2 AT %IX0.7 : BOOL;
				auto2 AT %IX1.0 : BOOL;
				run1 AT %IX1.1 : BOOL;
				trip1 AT %IX1.2 : BOOL;
				auto1 AT %IX1.3 : BOOL;
				coil3 AT %QX16.0 : BOOL;
				coil2 AT %QX16.1 : BOOL;
				coil1 AT %QX16.2 : BOOL;
				horn AT %QX16.3 : BOOL;
				amber AT %QX16.4 : BOOL;
				green AT %QX16.5 : BOOL;
				red AT %QX16.6 : BOOL;
				want3, want2, want1 : BOOL;
				m3, m2, m1 : MOTOR_CTRL;
			END_VAR""";

	private static final String BELT_DRIVE = """
			m3(START := want3, RUNNING := run3, TRIPPED := trip3, AUTO := auto3, RESET := start_pb, RUN => coil3);
			m2(START := want2, RUNNING := run2, TRIPPED := trip2, AUTO := auto2, RESET := start_pb, RUN => coil2);
			m1(START := want1, RUNNING := run1, TRIPPED := trip1, AUTO := auto1, RESET := start_pb, RUN => coil1);""";

	private void sfcStep(ClientGameTestContext context, String name) {
		plcClick(context, "STEP");
		plcField(context, "step name", name);
	}

	private void sfcTrans(ClientGameTestContext context, String condition) {
		plcClick(context, "TRANS");
		plcField(context, "condition", condition);
	}

	private void sfcAction(ClientGameTestContext context, String qualifier, String action, String time) {
		plcClick(context, "ACTION");
		plcField(context, "qualifier", qualifier);
		plcField(context, "action", action);
		plcField(context, "time", time);
	}

	/** Waits for three starters' run state to reach {@code on}, noting the tick each did. */
	private long[] plcBelts(ClientGameTestContext context, java.util.function.IntFunction<String> belt, boolean on, int ticks) {
		long[] when = {-1, -1, -1};
		for (int t = 0; t < ticks && (when[0] < 0 || when[1] < 0 || when[2] < 0); t++) {
			for (int i = 0; i < 3; i++) {
				if (when[i] < 0 && belt.apply(i).contains("RUNNING") == on) {
					when[i] = t;
				}
			}
			context.waitTick();
		}
		return when;
	}

	/** Phase 13d (docs/multiblocks/plc.md): a conveyor start-up, drawn as a chart with the real mouse, on three real starters and motors standing in for belts. */
	private void plcSfc(ClientGameTestContext context, TestServerContext server) {
		int z = -3200;
		int bay = z + 14;
		int px = 17;
		server.runCommand("tp @p 15 " + (up + 2) + " " + (bay + 5) + " 180 15");
		context.waitTicks(20);
		server.runCommand("fill 2 " + (y - 1) + " " + (z - 8) + " 34 " + (y + 8) + " " + (bay + 12) + " minecraft:air");
		server.runCommand("fill 2 " + (y - 1) + " " + (z - 8) + " 34 " + (y - 1) + " " + (bay + 12) + " minecraft:stone");
		long seed = -1;
		for (long s = 1; s < 4_000_000 && seed < 0; s++) {
			boolean all = true;
			for (int i = 0; i < 3 && all; i++) {
				Reservoir oil = Reservoir.at(s, px + 6, z + i * 6);
				all = oil != null && oil.kind() == Reservoir.Kind.OIL && oil.units() > 10_000;
			}
			if (all) {
				seed = s;
			}
		}
		check(seed >= 0, "no seed puts oil under all three pumpjacks");
		ReservoirData.testSeed = seed;
		Filming film = null;
		try {
			BlockPos set = new BlockPos(12, up, z);
			genset(server, set, net.minecraft.core.Direction.NORTH, false);
			for (int zz = z; zz <= z + 12; zz++) {
				put(server, 13, up, zz, "wildcatter:cable_industrial");
			}
			BlockPos[] starters = new BlockPos[3];
			for (int i = 0; i < 3; i++) {
				int line = z + i * 6;
				starters[i] = new BlockPos(14, up, line);
				put(server, 14, up, line, "wildcatter:motor_starter[facing=east]");
				put(server, 15, up, line, "wildcatter:cable_industrial");
				put(server, 16, up, line, "wildcatter:cable_industrial");
				pumpjack(server, new BlockPos(px, up, line), net.minecraft.core.Direction.EAST);
				put(server, 14, up + 1, line, "wildcatter:junction_box");
			}
			for (int zz = z; zz <= bay; zz++) {
				if ((zz - z) % 6 != 0 || zz > z + 12) {
					put(server, 14, up + 1, zz, "wildcatter:control_cable");
				}
			}
			plant(server, bay);
			put(server, 12, up, bay, "wildcatter:plastic_wire_hot");
			put(server, 13, up, bay, "wildcatter:control_supply");
			for (int x = 11; x <= 13; x++) {
				put(server, x, up, bay + 1, "wildcatter:plastic_wire_neutral");
			}
			put(server, 14, up, bay, "wildcatter:junction_box");
			put(server, 14, up + 1, bay, "wildcatter:junction_box");
			put(server, 14, up + 1, bay - 1, "wildcatter:junction_box");   // the stop button hangs under here, and a cable cell there would touch three things
			put(server, 15, up, bay, "wildcatter:plc_cabinet[facing=south]");
			server.runOnServer(srv -> dev.wildcatter.multiblock.BigParts.grow(srv.overworld(), new BlockPos(15, up, bay), srv.overworld().getBlockState(new BlockPos(15, up, bay))));
			BlockPos startAt = new BlockPos(14, up, bay + 1);
			BlockPos stopAt = new BlockPos(14, up, bay - 1);
			BlockPos estop = new BlockPos(13, up + 1, bay);
			BlockPos hornAt = new BlockPos(14, up + 1, bay + 1);
			BlockPos stack = new BlockPos(14, up + 2, bay);
			put(server, 14, up, bay + 1, "wildcatter:pushbutton[facing=south]");
			put(server, 14, up, bay - 1, "wildcatter:pushbutton[facing=north]");
			put(server, 13, up + 1, bay, "wildcatter:estop[facing=south]");
			put(server, 14, up + 1, bay + 1, "wildcatter:horn[facing=south]");
			put(server, 14, up + 2, bay, "wildcatter:stack_light[facing=south]");
			startGenset(context, server, set, net.minecraft.core.Direction.NORTH, 1000);
			BlockPos at = new BlockPos(15, up, bay);
			java.util.function.Function<net.minecraft.server.MinecraftServer, dev.wildcatter.control.PlcCabinetBlockEntity> cabinet = srv -> (dev.wildcatter.control.PlcCabinetBlockEntity) srv.overworld().getBlockEntity(at);
			java.util.function.Supplier<String> status = () -> read(server, 15, up, bay);
			java.util.function.IntFunction<String> belt = i -> read(server, 14, up, starters[i].getZ());
			context.waitTicks(30);
			String wired = server.computeOnServer(srv -> {
				dev.wildcatter.control.PlcCabinetBlockEntity box = cabinet.apply(srv);
				box.seat(dev.wildcatter.control.PlcModuleItem.Type.CPU_SMALL);
				box.seat(dev.wildcatter.control.PlcModuleItem.Type.DI16);
				box.seat(dev.wildcatter.control.PlcModuleItem.Type.DO16);
				StringBuilder errors = new StringBuilder();
				String[][] inputs = {{"%IX0.3", "%IX0.4", "%IX0.5"}, {"%IX0.6", "%IX0.7", "%IX1.0"}, {"%IX1.1", "%IX1.2", "%IX1.3"}};
				java.util.List<String> all = new java.util.ArrayList<>();
				all.add(box.land(startAt, 0, "%IX0.0"));
				all.add(box.land(stopAt, 0, "%IX0.1"));
				all.add(box.land(estop, 0, "%IX0.2"));
				// belt 3 is the last in the line and starts first: starters[0] here
				for (int i = 0; i < 3; i++) {
					all.add(box.land(starters[i], 0, "%QX16." + i));
					for (int point = 0; point < 3; point++) {
						all.add(box.land(starters[i], point + 1, inputs[i][point]));
					}
				}
				all.add(box.land(hornAt, 0, "%QX16.3"));
				all.add(box.land(stack, 1, "%QX16.4"));
				all.add(box.land(stack, 2, "%QX16.5"));
				all.add(box.land(stack, 0, "%QX16.6"));
				all.stream().filter(java.util.Objects::nonNull).forEach(e -> errors.append(e).append("; "));
				return errors.toString();
			});
			check(wired.isEmpty(), "every signal should land: " + wired);
			server.runOnServer(srv -> {
				for (BlockPos starter : starters) {
					starter(srv, starter.getX(), starter.getY(), starter.getZ()).cycleHoa(starter);
					starter(srv, starter.getX(), starter.getY(), starter.getZ()).cycleHoa(starter);
				}
			});
			context.waitTicks(30);
			check(status.get().contains("devices reached") && !status.get().contains("NO 24 V") && !status.get().contains("BAD JOINT"), "the panel should be wired: " + status.get());
			film = context.computeOnClient(client -> Filming.start(client, "plcSfc"));
			server.runOnServer(srv -> cabinet.apply(srv).open(srv.getPlayerList().getPlayers().get(0)));
			context.waitTicks(20);
			// draw it: the toolbar chains a step and a transition, and the mouse wires what it does not
			plcClick(context, "+SFC");
			context.waitTicks(3);
			plcField(context, "name", "BELTS");
			context.runOnClient(client -> plcScreen(client).selectUnitForTest("MAIN"));
			context.waitTick();
			plcClick(context, "DELETE UNIT");
			context.waitTicks(3);
			plcClick(context, "VARS");
			context.runOnClient(client -> plcScreen(client).replaceForTest(BELT_VARS));
			plcClick(context, "DRAW");
			sfcStep(context, "IDLE");
			sfcTrans(context, "start_pb AND estop_ok");
			sfcStep(context, "WARN1");
			sfcAction(context, "N", "horn", "");
			context.waitTick();
			context.runOnClient(client -> plcScreen(client).sfcScrollForTest(0));
			plcMouse(context, context.computeOnClient(client -> plcScreen(client).sfcTransitionPinForTest(0, true)));   // the transition's foot: the second branch comes out of the same one
			sfcStep(context, "WARN2");
			sfcAction(context, "N", "amber", "");
			plcMouse(context, context.computeOnClient(client -> plcScreen(client).sfcStepPinForTest("WARN1", true)));
			sfcTrans(context, "WARN1.T >= T#3s");
			// the join: WARN2's foot to this transition's head, as the double bar
			plcMouse(context, context.computeOnClient(client -> plcScreen(client).sfcStepPinForTest("WARN2", true)));
			plcMouse(context, context.computeOnClient(client -> plcScreen(client).sfcTransitionPinForTest(1, false)));
			sfcStep(context, "B3");
			sfcAction(context, "S", "want3", "");
			sfcTrans(context, "B3.T >= T#2s AND m3.STATE = 2");
			sfcStep(context, "B2");
			sfcAction(context, "S", "want2", "");
			sfcTrans(context, "B2.T >= T#2s AND m2.STATE = 2");
			sfcStep(context, "B1");
			sfcAction(context, "S", "want1", "");
			sfcTrans(context, "B1.T >= T#2s AND m1.STATE = 2");
			sfcStep(context, "LINE_UP");
			sfcAction(context, "N", "green", "");
			sfcTrans(context, "stop_pb OR NOT estop_ok");
			sfcStep(context, "STOP1");
			sfcAction(context, "R", "want1", "");
			sfcTrans(context, "NOT run1 AND STOP1.T >= T#1s");
			sfcStep(context, "STOP2");
			sfcAction(context, "R", "want2", "");
			sfcTrans(context, "NOT run2 AND STOP2.T >= T#1s");
			sfcStep(context, "STOP3");
			sfcAction(context, "R", "want3", "");
			sfcTrans(context, "NOT run3");
			// the way back: the last transition's foot to IDLE's head, a jump round the side
			context.waitTick();
			int lastTransition = 8;
			context.runOnClient(client -> plcScreen(client).sfcScrollForTest(300));
			context.waitTick();
			plcMouse(context, context.computeOnClient(client -> plcScreen(client).sfcTransitionPinForTest(lastTransition, true)));
			context.runOnClient(client -> plcScreen(client).sfcScrollForTest(0));
			context.waitTick();
			plcMouse(context, context.computeOnClient(client -> plcScreen(client).sfcStepPinForTest("IDLE", false)));
			// the second way out of LINE_UP: a fault stops everything and shows red
			context.runOnClient(client -> plcScreen(client).sfcScrollForTest(150));
			context.waitTick();
			plcMouse(context, context.computeOnClient(client -> plcScreen(client).sfcStepPinForTest("LINE_UP", true)));
			sfcTrans(context, "m3.FAULT OR m2.FAULT OR m1.FAULT");
			sfcStep(context, "FAULTED");
			sfcAction(context, "N", "red", "");
			sfcAction(context, "R", "want1", "");
			sfcAction(context, "R", "want2", "");
			sfcAction(context, "R", "want3", "");
			sfcTrans(context, "start_pb");
			context.waitTick();
			plcMouse(context, context.computeOnClient(client -> plcScreen(client).sfcTransitionPinForTest(10, true)));
			context.runOnClient(client -> plcScreen(client).sfcScrollForTest(0));
			context.waitTick();
			plcMouse(context, context.computeOnClient(client -> plcScreen(client).sfcStepPinForTest("IDLE", false)));
			// a second network, as the standard allows: one step always active, its action running the three device blocks
			context.runOnClient(client -> plcScreen(client).deselectForTest());
			context.waitTick();
			sfcStep(context, "DEVICES");
			plcClick(context, "INITIAL");
			plcClick(context, "ACTION");
			plcField(context, "action", "DRIVE");
			plcClick(context, "BODY");
			context.waitTick();
			context.runOnClient(client -> plcScreen(client).replaceForTest(BELT_DRIVE));
			plcClick(context, "DRAW");
			context.waitTick();
			context.runOnClient(client -> System.out.println("[wildcatter-test] plc chart saved: " + plcScreen(client).savedForTest().replace("\n", " / ")));
			plcClick(context, "DOWNLOAD");
			context.waitTicks(20);
			check(context.computeOnClient(client -> plcScreen(client).errorsForTest().isEmpty()), "the drawn chart should compile: " + context.computeOnClient(client -> plcScreen(client).errorsForTest().toString()));
			check(server.computeOnServer(srv -> cabinet.apply(srv).project().getFirst().language() == dev.wildcatter.plc.lang.Project.Language.SFC), "the chart should reach the cabinet");
			plcClick(context, "RUN");
			context.waitTicks(30);
			check(status.get().contains("RUN"), "RUN, from the screen: " + status.get());
			check(context.computeOnClient(client -> plcScreen(client).liveForTest()), "the chart should go live");
			context.runOnClient(client -> plcScreen(client).sfcScrollForTest(0));
			context.takeScreenshot("wildcatter-plc-sfc-idle");
			java.util.function.Supplier<Integer> lamps = () -> server.computeOnServer(srv -> srv.overworld().getBlockState(stack).getValue(dev.wildcatter.control.OperatorDeviceBlock.LAMPS));
			java.util.function.Consumer<BlockPos> press = device -> server.runOnServer(srv -> ((dev.wildcatter.control.OperatorDeviceBlockEntity) srv.overworld().getBlockEntity(device)).operate());
			java.util.function.Function<String, Object> tag = name -> server.computeOnServer(srv -> cabinet.apply(srv).cpu().read("BELTS." + name));
			// the start: horn and amber together, in the parallel branch, and nothing moving yet
			press.accept(startAt);
			context.waitTicks(20);
			check(Boolean.TRUE.equals(tag.apply("WARN1.X")) && Boolean.TRUE.equals(tag.apply("WARN2.X")), "both branches should run: " + tag.apply("WARN1.X") + " " + tag.apply("WARN2.X"));
			check(Boolean.TRUE.equals(tag.apply("HORN")) && (lamps.get() & 2) != 0, "the horn and amber light warn first: lamps " + lamps.get());
			check(!belt.apply(0).contains("RUNNING"), "nothing moves during the warning: " + belt.apply(0));
			context.takeScreenshot("wildcatter-plc-sfc-warning");
			long[] up3 = plcBelts(context, belt, true, 600);
			String order = up3[0] + ", " + up3[1] + ", " + up3[2];
			check(up3[0] >= 0 && up3[1] > up3[0] + 30 && up3[2] > up3[1] + 30, "belt 3, then 2, then 1, at least 2 s apart: " + order);
			context.waitTicks(60);
			check(Boolean.TRUE.equals(tag.apply("LINE_UP.X")) && (lamps.get() & 4) != 0 && (lamps.get() & 2) == 0, "the line is up, green and no amber: lamps " + lamps.get());
			check(read(server, px, up, starters[0].getZ()).contains("pumping"), "the real motor is doing real work: " + read(server, px, up, starters[0].getZ()));
			context.runOnClient(client -> plcScreen(client).sfcScrollForTest(150));
			context.waitTicks(10);
			context.takeScreenshot("wildcatter-plc-sfc-running");
			System.out.println("[wildcatter-test] plc belts started at ticks " + order);
			// the stop: the reverse order
			press.accept(stopAt);
			long[] down = plcBelts(context, belt, false, 600);
			String stopOrder = down[0] + ", " + down[1] + ", " + down[2];
			check(down[2] >= 0 && down[1] > down[2] && down[0] > down[1], "belt 1 first, then 2, then 3: " + stopOrder);
			context.waitTicks(30);
			check(Boolean.TRUE.equals(tag.apply("IDLE.X")), "and back to IDLE, by the jump");
			// a belt's feedback lost, as a contactor dropping out: the belts feeding it stop, and red
			press.accept(startAt);
			long[] again = plcBelts(context, belt, true, 600);
			check(again[2] >= 0, "it should start again: " + again[2]);
			context.waitTicks(60);
			check(Boolean.TRUE.equals(tag.apply("LINE_UP.X")), "up again");
			server.runOnServer(srv -> cabinet.apply(srv).cpu().force("%IX0.3", "FALSE"));
			context.waitTicks(30);
			check(Boolean.TRUE.equals(tag.apply("M3.FAIL_RUN")), "MOTOR_CTRL should see its feedback gone: " + tag.apply("M3.STATE"));
			check(Boolean.TRUE.equals(tag.apply("FAULTED.X")) && (lamps.get() & 1) != 0, "the chart goes to its fault step, red: lamps " + lamps.get());
			check(!belt.apply(1).contains("RUNNING") && !belt.apply(2).contains("RUNNING"), "the belts feeding it stop: " + belt.apply(1) + " | " + belt.apply(2));
			context.runOnClient(client -> plcScreen(client).sfcScrollForTest(230));
			context.waitTicks(5);
			context.takeScreenshot("wildcatter-plc-sfc-fault");
			context.runOnClient(client -> plcScreen(client).sfcScrollForTest(370));
			context.waitTicks(5);
			context.takeScreenshot("wildcatter-plc-sfc-jump");
			server.runOnServer(srv -> cabinet.apply(srv).cpu().release("%IX0.3"));
			context.waitTicks(40);
			press.accept(startAt);
			context.waitTicks(15);
			check(Boolean.TRUE.equals(tag.apply("IDLE.X")) || Boolean.TRUE.equals(tag.apply("WARN1.X")), "start clears it, back through IDLE: " + tag.apply("FAULTED.X"));
			check(!Boolean.TRUE.equals(tag.apply("M3.FAULT")), "and RESET cleared the block's latch");
			// a selector out of AUTO is no failure: the block asks for nothing and raises nothing
			server.runOnServer(srv -> starter(srv, 14, up, starters[1].getZ()).cycleHoa(starters[1]));
			context.waitTicks(10);
			check(((Number) tag.apply("M2.STATE")).intValue() == 5 && !Boolean.TRUE.equals(tag.apply("M2.FAULT")), "out of AUTO: state " + tag.apply("M2.STATE") + ", fault " + tag.apply("M2.FAULT"));
			server.runOnServer(srv -> {
				starter(srv, 14, up, starters[1].getZ()).cycleHoa(starters[1]);
				starter(srv, 14, up, starters[1].getZ()).cycleHoa(starters[1]);
			});
			context.waitTicks(1200);   // the overload relays cool from these starts, and the warning finishes and belts come up
			plcCardAndFiles(context, server, cabinet, status, belt, bay, press, startAt, stopAt);
			System.out.println("[wildcatter-test] plcSfc ok: " + status.get());
		} finally {
			ReservoirData.testSeed = null;
			if (film != null) {
				film.stop();
			}
		}
	}

	/** The memory card to a second cabinet, then the project out to PLCopen XML and back in, running; then the mouse work 13c left untested. */
	private void plcCardAndFiles(ClientGameTestContext context, TestServerContext server, java.util.function.Function<net.minecraft.server.MinecraftServer, dev.wildcatter.control.PlcCabinetBlockEntity> cabinet, java.util.function.Supplier<String> status,
			java.util.function.IntFunction<String> belt, int bay, java.util.function.Consumer<BlockPos> press, BlockPos startAt, BlockPos stopAt) {
		plcClick(context, "PROJECT");
		context.waitTicks(3);
		plcClick(context, "TO CARD");
		context.waitTicks(10);
		check(context.computeOnClient(client -> plcScreen(client).notesForTest().toString()).contains("no card"), "TO CARD with no card says so: " + context.computeOnClient(client -> plcScreen(client).notesForTest().toString()));
		server.runOnServer(srv -> cabinet.apply(srv).seatCard(new net.minecraft.world.item.ItemStack(dev.wildcatter.WildcatterItems.MEMORY_CARD)));
		context.waitTicks(10);
		plcClick(context, "FROM CARD");
		context.waitTicks(10);
		check(context.computeOnClient(client -> plcScreen(client).notesForTest().toString()).contains("blank"), "a blank card says so");
		plcClick(context, "TO CARD");
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-plc-project");
		BlockPos second = new BlockPos(17, up, bay);
		put(server, 17, up, bay, "wildcatter:plc_cabinet[facing=south]");
		server.runOnServer(srv -> dev.wildcatter.multiblock.BigParts.grow(srv.overworld(), second, srv.overworld().getBlockState(second)));
		boolean copied = server.computeOnServer(srv -> {
			var box = (dev.wildcatter.control.PlcCabinetBlockEntity) srv.overworld().getBlockEntity(second);
			box.seat(dev.wildcatter.control.PlcModuleItem.Type.CPU_SMALL);
			net.minecraft.world.item.ItemStack card = cabinet.apply(srv).unseatLast();   // the card comes out before any module
			if (!(card.getItem() instanceof dev.wildcatter.control.MemoryCardItem) || !dev.wildcatter.control.MemoryCardItem.describe(card).contains("BELTS")) {
				return false;
			}
			box.seatCard(card);
			var registries = srv.registryAccess();
			net.minecraft.nbt.CompoundTag saved = box.saveCustomOnly(registries);
			var copy = new dev.wildcatter.control.PlcCabinetBlockEntity(second, box.getBlockState());
			copy.setLevel(srv.overworld());
			copy.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, registries, saved));
			return dev.wildcatter.control.MemoryCardItem.describe(copy.card()).contains("BELTS") && box.fromCard().isEmpty() && box.project().equals(cabinet.apply(srv).project());
		});
		check(copied, "the card should carry the project to another cabinet, surviving a save");
		// out to a real file and back
		plcField(context, "file name", "../escape");
		plcClick(context, "EXPORT XML");
		context.waitTick();
		check(context.computeOnClient(client -> plcScreen(client).notesForTest().toString()).contains("letters, numbers"), "a file name cannot leave the folder");
		plcField(context, "file name", "belts");
		plcClick(context, "EXPORT XML");
		context.waitTicks(3);
		plcClick(context, "EXPORT ST");
		context.waitTicks(3);
		check(context.computeOnClient(client -> plcScreen(client).filesForTest().contains("belts.xml")), "the file should be listed: " + context.computeOnClient(client -> plcScreen(client).filesForTest().toString()));
		check(context.computeOnClient(client -> plcScreen(client).notesForTest().toString()).contains("no structured text"), "an all-chart project has no ST to export");
		String savedBefore = context.computeOnClient(client -> plcScreen(client).sourcesForTest().getFirst().text());
		plcClick(context, "CODE");
		context.waitTicks(2);
		plcClick(context, "+ST");
		context.waitTicks(2);
		context.runOnClient(client -> plcScreen(client).selectUnitForTest("BELTS"));
		context.waitTick();
		plcClick(context, "DELETE UNIT");
		context.waitTicks(2);
		check(context.computeOnClient(client -> !plcScreen(client).unitNamesForTest().contains("BELTS")), "the chart is gone from the editor");
		plcClick(context, "PROJECT");
		context.waitTicks(2);
		context.runOnClient(client -> plcScreen(client).selectFileForTest("belts.xml"));
		plcClick(context, "IMPORT");
		context.waitTicks(3);
		check(context.computeOnClient(client -> plcScreen(client).unitNamesForTest().contains("BELTS:SFC")), "the chart is back, as a chart: " + context.computeOnClient(client -> plcScreen(client).unitNamesForTest()));
		String imported = context.computeOnClient(client -> plcScreen(client).sourcesForTest().stream().filter(u -> u.name().equals("BELTS")).findFirst().get().text());
		check(imported.contains("\"WARN1.T >= T#3s\"") && imported.contains("\"DEVICES\"") && imported.contains("m3(START := want3"), "with its conditions and its action's body: " + imported);
		System.out.println("[wildcatter-test] plc chart was " + savedBefore.length() + " characters, reimported " + imported.length());
		plcClick(context, "CODE");
		context.waitTicks(2);
		context.runOnClient(client -> plcScreen(client).selectUnitForTest("BELTS"));
		context.waitTick();
		context.runOnClient(client -> plcScreen(client).sourcesForTest().forEach(u -> {
			if (u.language() == dev.wildcatter.plc.lang.Project.Language.ST) {
				plcScreen(client).deleteUnitForTest(u.name());
			}
		}));
		context.waitTick();
		plcClick(context, "DOWNLOAD");
		context.waitTicks(15);
		check(context.computeOnClient(client -> plcScreen(client).errorsForTest().isEmpty()), "the imported chart should compile: " + context.computeOnClient(client -> plcScreen(client).errorsForTest().toString()));
		plcClick(context, "RUN");
		context.waitTicks(20);
		press.accept(startAt);
		long[] up = plcBelts(context, belt, true, 600);
		check(up[0] >= 0 && up[1] > up[0] + 30 && up[2] > up[1] + 30, "the imported chart runs the same sequence: " + up[0] + ", " + up[1] + ", " + up[2]);
		context.waitTicks(40);
		press.accept(stopAt);
		long[] down = plcBelts(context, belt, false, 600);
		check(down[2] >= 0 && down[0] > down[1] && down[1] > down[2], "and the same stop: " + down[0] + ", " + down[1] + ", " + down[2]);
		// FBD by mouse: blocks, a wire from pin to pin, a drag and a pan, none of which 13c's scene touched
		plcClick(context, "+FBD");
		context.waitTicks(2);
		plcClick(context, "IN");
		plcField(context, "variable or value", "run3");
		plcClick(context, "BLOCK");
		context.waitTick();
		plcClick(context, "OUT");
		plcField(context, "variable or value", "other");
		context.waitTick();
		plcMouse(context, context.computeOnClient(client -> plcScreen(client).fbdPinForTest(1, "OUT", true)));
		plcMouse(context, context.computeOnClient(client -> plcScreen(client).fbdPinForTest(2, "IN", false)));
		plcMouse(context, context.computeOnClient(client -> plcScreen(client).fbdPinForTest(2, "Q", true)));
		plcMouse(context, context.computeOnClient(client -> plcScreen(client).fbdPinForTest(3, "IN", false)));
		context.waitTick();
		String wiredFbd = context.computeOnClient(client -> plcScreen(client).savedForTest());
		check(wiredFbd.contains("@1.OUT") && wiredFbd.contains("@2.Q"), "the mouse should have wired both: " + wiredFbd);
		int[] body = context.computeOnClient(client -> plcScreen(client).fbdPinForTest(2, "IN", false));
		double toWindow = context.computeOnClient(client -> (double) client.getWindow().getScreenWidth() / client.getWindow().getGuiScaledWidth());
		int blockXBefore = context.computeOnClient(client -> plcScreen(client).fbdBlockXForTest(2));
		context.getInput().setCursorPos((body[0] + 30) * toWindow, (body[1] - 18) * toWindow);
		context.waitTick();
		context.getInput().holdMouse(1);
		context.waitTick();
		context.getInput().setCursorPos((body[0] + 70) * toWindow, (body[1] - 18) * toWindow);
		context.waitTicks(2);
		context.getInput().releaseMouse(1);
		context.waitTick();
		int blockXAfter = context.computeOnClient(client -> plcScreen(client).fbdBlockXForTest(2));
		check(blockXAfter == blockXBefore + 40, "a drag should move the block: " + blockXBefore + " to " + blockXAfter);
		context.takeScreenshot("wildcatter-plc-fbd-mouse");
		context.runOnClient(client -> plcScreen(client).undoForTest());
		context.waitTick();
		check(context.computeOnClient(client -> plcScreen(client).fbdBlockXForTest(2)) == blockXBefore, "undo should put it back");
		context.runOnClient(client -> client.gui.screen().onClose());
		context.waitTicks(5);
	}

	private static final String LEVEL_PROJECT = """
			PROGRAM LEVEL
			VAR
				running AT %IX0.0 : BOOL;
				tripped AT %IX0.1 : BOOL;
				drive_auto AT %IX0.2 : BOOL;
				mov_open AT %IX0.5 : BOOL;
				mov_closed AT %IX0.6 : BOOL;
				mov_auto AT %IX0.7 : BOOL;
				coil AT %QX16.0 : BOOL;
				mov_open_cmd AT %QX16.1 : BOOL;
				mov_close_cmd AT %QX16.2 : BOOL;
				lt_raw AT %IW34 : WORD;
				zt_raw AT %IW36 : WORD;
				speed_ref AT %QW48 : WORD;
				fcv_signal AT %QW50 : WORD;
				pulses AT %ID64 : DWORD;
				sp : REAL := 50.0;
				drain : REAL := 30.0;
				inlet : BOOL := TRUE;
				pv : REAL;
				lt_fault : BOOL;
				lt_counts : INT;
				litres : DINT;
				loop : PID;
				pump : MOTOR_CTRL;
				mov : VALVE_CTRL;
			END_VAR
			lt_counts := WORD_TO_INT(lt_raw);
			lt_fault := lt_counts = 32767;
			pv := SCALE(INT_TO_REAL(lt_counts), 0.0, 32000.0, 0.0, 100.0);
			loop(AUTO := NOT lt_fault, PV := pv, SP := sp, MAN := 0.0, KP := 2.0, TI := T#30s);
			speed_ref := INT_TO_WORD(REAL_TO_INT(loop.OUT * 320.0));
			pump(START := NOT lt_fault, RUNNING := running, TRIPPED := tripped, AUTO := drive_auto, RUN => coil);
			fcv_signal := INT_TO_WORD(REAL_TO_INT(drain * 320.0));
			mov(OPEN := inlet, OPENED := mov_open, CLOSED := mov_closed, AUTO := mov_auto, TRAVEL_T := T#35s, CMD_OPEN => mov_open_cmd, CMD_CLOSE => mov_close_cmd);
			litres := DWORD_TO_DINT(pulses);
			END_PROGRAM""";

	/** Phase 13e (docs/multiblocks/plc.md): a PID holding a real tank's level on a real drive-fed pump, through real 4-20 mA loops; air and motor-operated valves; a failed loop. */
	private void plcAnalog(ClientGameTestContext context, TestServerContext server) {
		int z = -3400;
		int bay = z + 10;
		server.runCommand("tp @p 18 " + (up + 3) + " " + (z + 9) + " 180 25");
		context.waitTicks(20);
		server.runCommand("fill 2 " + (y - 1) + " " + (z - 8) + " 34 " + (y + 8) + " " + (bay + 8) + " minecraft:air");
		server.runCommand("fill 2 " + (y - 1) + " " + (z - 8) + " 34 " + (y - 1) + " " + (bay + 8) + " minecraft:stone");
		Filming film = null;
		try {
			BlockPos set = new BlockPos(12, up, z);
			genset(server, set, net.minecraft.core.Direction.NORTH, false);
			// 480 V to the drive's top, along the top to the compressor and the valve's actuator
			put(server, 13, up, z, "wildcatter:cable_industrial");
			put(server, 14, up, z, "wildcatter:cable_industrial");
			put(server, 14, up + 1, z, "wildcatter:cable_industrial");
			put(server, 14, up + 2, z, "wildcatter:cable_industrial");
			for (int x = 15; x <= 18; x++) {
				put(server, x, up + 2, z, "wildcatter:cable_industrial");
			}
			for (int zz = z + 1; zz <= z + 3; zz++) {
				put(server, 18, up + 2, zz, "wildcatter:cable_industrial");
			}
			put(server, 18, up + 1, z + 3, "wildcatter:cable_industrial");
			BlockPos drive = new BlockPos(15, up, z);
			placeBig(server, WildcatterBlocks.DRIVE_CABINET, drive, net.minecraft.core.Direction.NORTH);
			put(server, 15, up, z + 1, "wildcatter:cable_industrial");
			put(server, 15, up, z + 2, "wildcatter:cable_industrial");
			server.runOnServer(srv -> {   // the dial set for a 37 kW motor's full-load amps, as a player sets it
				var box = ((dev.wildcatter.block.StarterCabinetBlockEntity) srv.overworld().getBlockEntity(drive)).starterForTest();
				while (box.overloadAmps() < 52) {
					box.nextOverloadSetting();
				}
			});
			// the pump, over its sump
			server.runCommand("fill 13 " + y + " " + (z + 2) + " 17 " + y + " " + (z + 4) + " minecraft:stone");
			put(server, 15, y, z + 3, "minecraft:water");
			put(server, 15, up, z + 3, "wildcatter:process_water_pump[facing=north]");
			put(server, 16, up, z + 3, "wildcatter:pipe");
			put(server, 17, up, z + 3, "wildcatter:pipe");
			BlockPos mov = new BlockPos(18, up, z + 3);
			put(server, 18, up, z + 3, "wildcatter:motor_operated_valve");
			put(server, 19, up, z + 3, "wildcatter:pipe");
			BlockPos tank = new BlockPos(20, up, z + 3);
			put(server, 20, up, z + 3, "wildcatter:day_tank");
			BlockPos fcv = new BlockPos(21, up, z + 3);
			put(server, 21, up, z + 3, "wildcatter:control_valve");
			// instrument air: compressor, dryer, tubing down to the valve's top
			put(server, 19, up + 2, z + 3, "wildcatter:air_compressor");
			put(server, 20, up + 2, z + 3, "wildcatter:air_dryer");
			BlockPos tubing = new BlockPos(21, up + 2, z + 3);
			put(server, 21, up + 2, z + 3, "wildcatter:air_tubing");
			put(server, 21, up + 1, z + 3, "wildcatter:air_tubing");
			// the transmitters, clamped on
			BlockPos lt = new BlockPos(20, up, z + 4);
			BlockPos ft = new BlockPos(15, up, z + 4);
			put(server, 20, up, z + 4, "wildcatter:transmitter[facing=south]");
			put(server, 15, up, z + 4, "wildcatter:transmitter[facing=south]");
			// the trunk: boxes where anything touches, cable between
			for (int x = 14; x <= 21; x++) {
				boolean box = x == 15 || x == 16 || x == 18 || x == 20 || x == 21;
				put(server, x, up, z + 5, box ? "wildcatter:junction_box" : "wildcatter:control_cable");
			}
			put(server, 18, up, z + 4, "wildcatter:control_cable");
			put(server, 21, up, z + 4, "wildcatter:junction_box");
			for (int zz = z + 1; zz <= z + 5; zz++) {
				put(server, 16, up + 1, zz, "wildcatter:control_cable");
			}
			put(server, 16, up + 1, z, "wildcatter:control_cable");
			for (int zz = z + 6; zz < bay; zz++) {
				put(server, 14, up, zz, "wildcatter:control_cable");
			}
			plant(server, bay);
			put(server, 12, up, bay, "wildcatter:plastic_wire_hot");
			put(server, 13, up, bay, "wildcatter:control_supply");
			for (int x = 11; x <= 13; x++) {
				put(server, x, up, bay + 1, "wildcatter:plastic_wire_neutral");
			}
			put(server, 14, up, bay, "wildcatter:junction_box");
			BlockPos at = new BlockPos(15, up, bay);
			put(server, 15, up, bay, "wildcatter:plc_cabinet[facing=south]");
			server.runOnServer(srv -> dev.wildcatter.multiblock.BigParts.grow(srv.overworld(), at, srv.overworld().getBlockState(at)));
			// the new blocks in a row, for the contact sheet
			String[] row = {"control_valve", "on_off_valve", "motor_operated_valve", "control_valve_steel", "on_off_valve_steel", "motor_operated_valve_steel", "air_compressor", "air_dryer", "day_tank"};
			for (int i = 0; i < row.length; i++) {
				put(server, 24 + i, up, z + 7, "wildcatter:" + row[i]);
			}
			put(server, 24, up + 2, z + 7, "wildcatter:air_tubing");
			put(server, 25, up + 2, z + 7, "wildcatter:air_tubing");
			put(server, 26, up + 2, z + 7, "wildcatter:transmitter[facing=south]");
			put(server, 26, up + 1, z + 7, "wildcatter:day_tank");
			startGenset(context, server, set, net.minecraft.core.Direction.NORTH, 2000);
			java.util.function.Function<net.minecraft.server.MinecraftServer, dev.wildcatter.control.PlcCabinetBlockEntity> cabinet = srv -> (dev.wildcatter.control.PlcCabinetBlockEntity) srv.overworld().getBlockEntity(at);
			java.util.function.Supplier<String> status = () -> read(server, 15, up, bay);
			context.waitTicks(30);
			String refused = server.computeOnServer(srv -> cabinet.apply(srv).land(lt, 0, "%IW32"));
			check(refused != null && refused.contains("no AI8"), "an analog signal needs an analog module: " + refused);
			String wired = server.computeOnServer(srv -> {
				dev.wildcatter.control.PlcCabinetBlockEntity box = cabinet.apply(srv);
				for (var type : java.util.List.of(dev.wildcatter.control.PlcModuleItem.Type.CPU_SMALL, dev.wildcatter.control.PlcModuleItem.Type.DI16, dev.wildcatter.control.PlcModuleItem.Type.DO16, dev.wildcatter.control.PlcModuleItem.Type.AI8, dev.wildcatter.control.PlcModuleItem.Type.AO4,
						dev.wildcatter.control.PlcModuleItem.Type.HSC2)) {
					box.seat(type);
				}
				java.util.List<String> all = new java.util.ArrayList<>();
				all.add(box.land(drive, 0, "%QX16.0"));
				all.add(box.land(drive, 1, "%IX0.0"));
				all.add(box.land(drive, 2, "%IX0.1"));
				all.add(box.land(drive, 3, "%IX0.2"));
				all.add(box.land(drive, 4, "%QW48"));
				all.add(box.land(drive, 5, "%IW32"));
				all.add(box.land(lt, 0, "%IW34"));
				all.add(box.land(ft, 1, "%ID64"));
				all.add(box.land(fcv, 0, "%QW50"));
				all.add(box.land(fcv, 1, "%IW36"));
				all.add(box.land(mov, 0, "%QX16.1"));
				all.add(box.land(mov, 1, "%QX16.2"));
				all.add(box.land(mov, 2, "%IX0.5"));
				all.add(box.land(mov, 3, "%IX0.6"));
				all.add(box.land(mov, 4, "%IX0.7"));
				box.setTag(lt, "LT-101");
				box.setTag(ft, "FT-101");
				box.setTag(fcv, "FV-101");
				box.setTag(mov, "XV-101");
				StringBuilder errors = new StringBuilder();
				all.stream().filter(java.util.Objects::nonNull).forEach(e -> errors.append(e).append("; "));
				return errors.toString();
			});
			check(wired.isEmpty(), "every signal should land: " + wired);
			String badWord = server.computeOnServer(srv -> cabinet.apply(srv).land(lt, 0, "%IX0.3"));
			check(badWord != null && badWord.contains("input word"), "a loop cannot land on a bit: " + badWord);
			server.runOnServer(srv -> {
				cabinet.apply(srv).land(lt, 0, "%IW34");
				var flow = (dev.wildcatter.control.TransmitterBlockEntity) srv.overworld().getBlockEntity(ft);
				flow.setQuantity(dev.wildcatter.control.Measurable.Quantity.FLOW);
				flow.setRange(0, 800);
				for (BlockPos device : new BlockPos[] {drive, fcv, mov}) {
					var control = dev.wildcatter.control.ControlNet.deviceAt(srv.overworld(), device);
					control.cycleHoa(device);
					control.cycleHoa(device);
				}
			});
			context.waitTicks(30);
			check(status.get().contains("devices reached") && !status.get().contains("NO 24 V") && !status.get().contains("BAD JOINT"), "the panel should be wired: " + status.get());
			film = context.computeOnClient(client -> Filming.start(client, "plcAnalog"));
			context.takeScreenshot("wildcatter-plc-analog-site");
			java.util.List<?> errors = server.computeOnServer(srv -> cabinet.apply(srv).download(LEVEL_PROJECT));
			check(errors.isEmpty(), "the program should compile: " + errors);
			server.runOnServer(srv -> cabinet.apply(srv).cpu().setMode(dev.wildcatter.plc.Cpu.Mode.RUN));
			java.util.function.Supplier<Double> level = () -> server.computeOnServer(srv -> ((dev.wildcatter.block.DayTankBlockEntity) srv.overworld().getBlockEntity(tank)).waterLevel() * 100);
			java.util.function.Supplier<dev.wildcatter.control.ValveBlockEntity> none = null;
			java.util.function.Function<BlockPos, Double> valveAt = pos -> server.computeOnServer(srv -> ((dev.wildcatter.control.ValveBlockEntity) srv.overworld().getBlockEntity(pos)).position());
			java.util.function.Function<String, Object> tag = name -> server.computeOnServer(srv -> cabinet.apply(srv).cpu().read("LEVEL." + name));
			java.util.function.Supplier<String> driveText = () -> read(server, 15, up, z);
			// the inlet valve opens on its 480 V actuator; air charges; the drain valve goes to 30 %; the pump fills it to the setpoint and holds it
			context.waitTicks(400);
			check(valveAt.apply(mov) > 0.5, "the motor-operated valve should be opening under power: " + valveAt.apply(mov) + " | " + read(server, 18, up, z + 3));
			check(Math.abs(valveAt.apply(fcv) - 0.3) < 0.02, "the control valve should follow its 4-20 mA to 30 %: " + valveAt.apply(fcv) + " | " + read(server, 21, up, z + 3));
			context.waitTicks(1400);
			double first = level.get();
			System.out.println("[wildcatter-test] plc analog: level " + first + " % at setpoint 50 | " + driveText.get() + " | " + read(server, 15, up, z + 3));
			check(Math.abs(first - 50) < 4, "the PID should hold the level near 50 %: " + first + " | " + driveText.get());
			check(Math.abs(((Number) tag.apply("PV")).doubleValue() - first) < 1.5, "what the CPU reads should be the tank: " + tag.apply("PV") + " against " + first);
			double speed = server.computeOnServer(srv -> ((dev.wildcatter.block.StarterCabinetBlockEntity) srv.overworld().getBlockEntity(drive)).measure(dev.wildcatter.control.Measurable.Quantity.SPEED));
			double watts = server.computeOnServer(srv -> ((dev.wildcatter.block.StarterCabinetBlockEntity) srv.overworld().getBlockEntity(drive)).measure(dev.wildcatter.control.Measurable.Quantity.WATTS));
			System.out.println("[wildcatter-test] plc analog: drive at " + speed + " %, " + watts + " W, of a 37,000 W motor");
			check(speed > 22 && speed < 60, "holding a small draw, the drive should run well below full speed: " + speed);
			check(watts < 37_000 * 0.25, "power should fall with the cube of speed: " + watts + " W at " + speed + " %");
			check(((Number) tag.apply("PUMP.STATE")).intValue() == 2, "the device block should read the drive running: " + tag.apply("PUMP.STATE") + " | " + driveText.get());
			check(((Number) tag.apply("LITRES")).longValue() > 100, "the counter should total the flow: " + tag.apply("LITRES"));
			context.takeScreenshot("wildcatter-plc-analog-holding");
			// a bigger draw: it holds the level, by speeding up
			server.runOnServer(srv -> cabinet.apply(srv).cpu().write("LEVEL.DRAIN", 80.0));
			context.waitTicks(800);
			double second = level.get();
			double faster = server.computeOnServer(srv -> ((dev.wildcatter.block.StarterCabinetBlockEntity) srv.overworld().getBlockEntity(drive)).measure(dev.wildcatter.control.Measurable.Quantity.SPEED));
			System.out.println("[wildcatter-test] plc analog: drain 80 %: level " + second + " %, drive " + faster + " %");
			check(Math.abs(second - 50) < 4, "the level should hold through a bigger draw: " + second);
			check(faster > speed, "by running faster: " + faster + " against " + speed);
			// a new setpoint
			server.runOnServer(srv -> cabinet.apply(srv).cpu().write("LEVEL.SP", 70.0));
			context.waitTicks(800);
			double third = level.get();
			System.out.println("[wildcatter-test] plc analog: setpoint 70: level " + third + " %");
			check(Math.abs(third - 70) < 4, "the level should follow a new setpoint: " + third);
			// a failed loop: the transmitter gone reads as an open loop, so the program stops the pump rather than filling blind
			put(server, 20, up, z + 4, "minecraft:air");
			context.waitTicks(40);
			check(Boolean.TRUE.equals(tag.apply("LT_FAULT")) && ((Number) tag.apply("LT_COUNTS")).intValue() == 32767, "an open loop should read 32767: " + tag.apply("LT_COUNTS"));
			check(driveText.get().contains("STOPPED"), "and the program stops the pump: " + driveText.get());
			put(server, 20, up, z + 4, "wildcatter:transmitter[facing=south]");
			context.waitTicks(400);
			check(!Boolean.TRUE.equals(tag.apply("LT_FAULT")) && driveText.get().contains("RUN"), "back, it runs again: " + driveText.get());
			// air lost: the spring takes the control valve shut, whatever its signal says
			put(server, 21, up + 2, z + 3, "minecraft:air");
			context.waitTicks(160);
			check(valveAt.apply(fcv) < 0.02, "with no air it should fail shut: " + valveAt.apply(fcv) + " | " + read(server, 21, up, z + 3));
			put(server, 21, up + 2, z + 3, "wildcatter:air_tubing");
			context.waitTicks(200);
			check(valveAt.apply(fcv) > 0.5, "air back, it goes back to its signal: " + valveAt.apply(fcv));
			// power lost to the motor-operated valve: told to close, it stays put, and VALVE_CTRL says so
			put(server, 18, up + 1, z + 3, "minecraft:air");
			context.waitTicks(20);
			server.runOnServer(srv -> cabinet.apply(srv).cpu().write("LEVEL.INLET", false));
			context.waitTicks(780);
			check(valveAt.apply(mov) > 0.95, "with no power it should stay put: " + valveAt.apply(mov));
			check(Boolean.TRUE.equals(tag.apply("MOV.FAIL_CLOSE")), "and VALVE_CTRL should report it would not close: state " + tag.apply("MOV.STATE"));
			put(server, 18, up + 1, z + 3, "wildcatter:cable_industrial");
			server.runOnServer(srv -> cabinet.apply(srv).cpu().write("LEVEL.MOV.RESET", true));
			context.waitTicks(10);
			server.runOnServer(srv -> cabinet.apply(srv).cpu().write("LEVEL.MOV.RESET", false));
			context.waitTicks(700);
			check(valveAt.apply(mov) < 0.05 && !Boolean.TRUE.equals(tag.apply("MOV.FAULT")), "power back and reset, it closes: " + valveAt.apply(mov));
			// a CPU in STOP writes no signal: the drive stops, and the air valve goes to its fail position
			server.runOnServer(srv -> cabinet.apply(srv).cpu().setMode(dev.wildcatter.plc.Cpu.Mode.STOP));
			context.waitTicks(160);
			check(driveText.get().contains("STOPPED"), "STOP stops the drive: " + driveText.get());
			check(valveAt.apply(fcv) < 0.02, "and shuts the air valve: " + valveAt.apply(fcv));
			// the terminal screen's range, by the real mouse
			server.runOnServer(srv -> cabinet.apply(srv).open(srv.getPlayerList().getPlayers().get(0)));
			context.waitTicks(20);
			plcClick(context, "TERMINALS");
			context.waitTicks(5);
			check(context.computeOnClient(client -> plcScreen(client).selectTransmitterForTest()), "the transmitters should be listed");
			plcField(context, "range", "0 500");
			plcClick(context, "SET RANGE");
			context.waitTicks(10);
			check(context.computeOnClient(client -> plcScreen(client).notesForTest().toString()).contains("Range set"), "the range should set: " + context.computeOnClient(client -> plcScreen(client).notesForTest().toString()));
			context.takeScreenshot("wildcatter-plc-analog-terminals");
			context.runOnClient(client -> client.gui.screen().onClose());
			context.waitTicks(5);
			server.runCommand("tp @p 26 " + (up + 1) + " " + (z + 10) + " 180 12");
			context.waitTicks(30);
			context.takeScreenshot("wildcatter-plc-analog-contact-valves");
			server.runCommand("tp @p 31 " + (up + 1) + " " + (z + 10) + " 180 12");
			context.waitTicks(30);
			context.takeScreenshot("wildcatter-plc-analog-contact-air");
			System.out.println("[wildcatter-test] plcAnalog ok: " + status.get());
		} finally {
			if (film != null) {
				film.stop();
			}
		}
	}

	private static final String POWER_PROJECT = """
			PROGRAM SITE
			VAR
				a_running AT %IX0.0 : BOOL;
				a_closed AT %IX0.1 : BOOL;
				a_tripped AT %IX0.2 : BOOL;
				b_running AT %IX0.3 : BOOL;
				b_closed AT %IX0.4 : BOOL;
				f1_closed AT %IX0.5 : BOOL;
				f2_closed AT %IX0.6 : BOOL;
				f3_closed AT %IX0.7 : BOOL;
				f3_tripped AT %IX1.0 : BOOL;
				a_run AT %QX16.0 : BOOL;
				a_permit AT %QX16.1 : BOOL;
				b_run AT %QX16.2 : BOOL;
				b_permit AT %QX16.3 : BOOL;
				f1_close AT %QX16.4 : BOOL;
				f2_close AT %QX16.5 : BOOL;
				f3_close AT %QX16.6 : BOOL;
				f3_open AT %QX16.7 : BOOL;
				a_kw_raw AT %IW32 : WORD;
				b_kw_raw AT %IW34 : WORD;
				a_trim AT %QW48 : WORD;
				go : BOOL;
				a_kw, b_kw, total_kw : REAL;
				shed_kw : REAL := 100.0;
				trim_a : REAL := 50.0;
				shed : BOOL;
				over : TON;
				clear : TON;
				settle : TON;
			END_VAR
			a_kw := SCALE(INT_TO_REAL(WORD_TO_INT(a_kw_raw)), 0.0, 32000.0, 0.0, 625.0);
			b_kw := SCALE(INT_TO_REAL(WORD_TO_INT(b_kw_raw)), 0.0, 32000.0, 0.0, 625.0);
			total_kw := a_kw + b_kw;
			a_run := go;
			a_permit := go;
			b_run := go AND a_closed;
			b_permit := b_running AND a_closed;
			a_trim := INT_TO_WORD(REAL_TO_INT(trim_a * 320.0));
			settle(IN := a_closed AND b_closed, PT := T#2s);
			f1_close := settle.Q;
			f2_close := settle.Q AND f1_closed;
			over(IN := total_kw > shed_kw, PT := T#3s);
			clear(IN := total_kw < shed_kw - 20.0, PT := T#10s);
			IF over.Q THEN
				shed := TRUE;
			ELSIF clear.Q THEN
				shed := FALSE;
			END_IF;
			f3_close := settle.Q AND f2_closed AND NOT shed;
			f3_open := shed;
			END_PROGRAM""";

	/** Phase 13f (docs/multiblocks/plc.md): a PLC starts two sets, closes them, loads them, sheds load past a limit and shifts their shares, all watched and worked from an HMI. */
	private void plcPower(ClientGameTestContext context, TestServerContext server) {
		int z = -3600;
		int bay = z + 22;
		server.runCommand("tp @p 19 " + (up + 2) + " " + (bay + 5) + " 180 10");
		context.waitTicks(20);
		server.runCommand("fill 2 " + (y - 1) + " " + (z - 14) + " 34 " + (y + 8) + " " + (bay + 10) + " minecraft:air");
		server.runCommand("fill 2 " + (y - 1) + " " + (z - 14) + " 34 " + (y - 1) + " " + (bay + 10) + " minecraft:stone");
		Filming film = null;
		try {
			BlockPos setA = new BlockPos(12, up, z);
			BlockPos setB = new BlockPos(12, up, z + 16);
			genset(server, setA, net.minecraft.core.Direction.NORTH, false);
			genset(server, setB, net.minecraft.core.Direction.NORTH, false);
			for (int zz = z; zz <= z + 16; zz++) {
				put(server, 13, up, zz, "wildcatter:cable_industrial");
			}
			BlockPos[] feeders = {new BlockPos(14, up, z + 3), new BlockPos(14, up, z + 7), new BlockPos(14, up, z + 11)};
			for (BlockPos feeder : feeders) {
				put(server, 14, up, feeder.getZ(), "wildcatter:breaker_3p_industrial[facing=east,position=off]");
				put(server, 15, up, feeder.getZ(), "wildcatter:cable_industrial");
				put(server, 16, up, feeder.getZ(), "wildcatter:three_phase_heater");
				put(server, 15, up, feeder.getZ() + 1, "wildcatter:cable_industrial");
				put(server, 16, up, feeder.getZ() + 1, "wildcatter:three_phase_heater");
				put(server, 15, up, feeder.getZ() - 1, "wildcatter:cable_industrial");
				put(server, 16, up, feeder.getZ() - 1, "wildcatter:three_phase_heater");
			}
			server.runOnServer(srv -> {   // each feeder's dial set for its 54 A, as a player sets it
				for (BlockPos feeder : feeders) {
					var breaker = (dev.wildcatter.block.BreakerBlockEntity) srv.overworld().getBlockEntity(feeder);
					while (breaker.rating() < 100) {
						breaker.nextRating();
					}
				}
			});
			// the control trunk, boxes where anything touches
			for (int zz = z + 1; zz < bay; zz++) {
				boolean box = zz == z + 1 || zz == z + 3 || zz == z + 7 || zz == z + 11 || zz == z + 16;
				put(server, 14, up + 1, zz, box ? "wildcatter:junction_box" : "wildcatter:control_cable");
			}
			// over each set's panel, which is two cells deep and two high, and down to the trunk
			put(server, 14, up + 1, z, "wildcatter:control_cable");
			for (int line : new int[] {z, z + 16}) {
				put(server, 12, up + 2, line, "wildcatter:control_cable");
				put(server, 13, up + 2, line, "wildcatter:control_cable");
				put(server, 14, up + 2, line, "wildcatter:control_cable");
			}
			plant(server, bay);
			put(server, 12, up, bay, "wildcatter:plastic_wire_hot");
			put(server, 13, up, bay, "wildcatter:control_supply");
			for (int x = 11; x <= 13; x++) {
				put(server, x, up, bay + 1, "wildcatter:plastic_wire_neutral");
			}
			put(server, 14, up, bay, "wildcatter:junction_box");
			put(server, 14, up + 1, bay, "wildcatter:junction_box");
			BlockPos at = new BlockPos(15, up, bay);
			put(server, 15, up, bay, "wildcatter:plc_cabinet[facing=south]");
			server.runOnServer(srv -> dev.wildcatter.multiblock.BigParts.grow(srv.overworld(), at, srv.overworld().getBlockState(at)));
			put(server, 16, up, bay, "wildcatter:network_cable");
			put(server, 17, up, bay, "wildcatter:network_cable");
			BlockPos hmiAt = new BlockPos(18, up, bay);
			put(server, 18, up, bay, "wildcatter:hmi_panel[facing=south]");
			// fuel the sets by hand, as a start needs: the PLC starts them, not a panel click
			context.waitTicks(30);
			server.computeOnServer(srv -> {
				for (BlockPos set : new BlockPos[] {setA, setB}) {
					BlockPos tank = dev.wildcatter.block.GensetSkid.base(set, net.minecraft.core.Direction.NORTH, false);
					if (srv.overworld().getBlockEntity(tank) instanceof dev.wildcatter.block.GensetPartBlockEntity t) {
						t.setFuel(1500);
					}
				}
				return null;
			});
			java.util.function.Function<net.minecraft.server.MinecraftServer, dev.wildcatter.control.PlcCabinetBlockEntity> cabinet = srv -> (dev.wildcatter.control.PlcCabinetBlockEntity) srv.overworld().getBlockEntity(at);
			java.util.function.Supplier<String> status = () -> read(server, 15, up, bay);
			String wired = server.computeOnServer(srv -> {
				dev.wildcatter.control.PlcCabinetBlockEntity box = cabinet.apply(srv);
				for (var type : java.util.List.of(dev.wildcatter.control.PlcModuleItem.Type.CPU_SMALL, dev.wildcatter.control.PlcModuleItem.Type.DI16, dev.wildcatter.control.PlcModuleItem.Type.DO16, dev.wildcatter.control.PlcModuleItem.Type.AI8, dev.wildcatter.control.PlcModuleItem.Type.AO4,
						dev.wildcatter.control.PlcModuleItem.Type.COMM)) {
					box.seat(type);
				}
				box.setStation("PLC-1");
				java.util.List<String> all = new java.util.ArrayList<>();
				return String.join(";", all);
			});
			context.waitTicks(25);
			check(wired.isEmpty(), "modules: " + wired);
			String lands = server.computeOnServer(srv -> {
				dev.wildcatter.control.PlcCabinetBlockEntity box = cabinet.apply(srv);
				java.util.List<String> all = new java.util.ArrayList<>();
				all.add(box.land(setA, 0, "%QX16.0"));
				all.add(box.land(setA, 1, "%QX16.1"));
				all.add(box.land(setA, 2, "%QW48"));
				all.add(box.land(setA, 4, "%IX0.0"));
				all.add(box.land(setA, 5, "%IX0.1"));
				all.add(box.land(setA, 6, "%IX0.2"));
				all.add(box.land(setA, 9, "%IW32"));
				all.add(box.land(setB, 0, "%QX16.2"));
				all.add(box.land(setB, 1, "%QX16.3"));
				all.add(box.land(setB, 4, "%IX0.3"));
				all.add(box.land(setB, 5, "%IX0.4"));
				all.add(box.land(setB, 9, "%IW34"));
				all.add(box.land(feeders[0], 0, "%QX16.4"));
				all.add(box.land(feeders[1], 0, "%QX16.5"));
				all.add(box.land(feeders[2], 0, "%QX16.6"));
				all.add(box.land(feeders[2], 1, "%QX16.7"));
				all.add(box.land(feeders[0], 2, "%IX0.5"));
				all.add(box.land(feeders[1], 2, "%IX0.6"));
				all.add(box.land(feeders[2], 2, "%IX0.7"));
				all.add(box.land(feeders[2], 3, "%IX1.0"));
				StringBuilder errors = new StringBuilder();
				all.stream().filter(java.util.Objects::nonNull).forEach(e -> errors.append(e).append("; "));
				for (BlockPos device : new BlockPos[] {setA, setB, feeders[0], feeders[1], feeders[2]}) {
					var control = dev.wildcatter.control.ControlNet.deviceAt(srv.overworld(), device);
					if (control == null) {
						errors.append("no device at ").append(device.toShortString()).append(": ").append(srv.overworld().getBlockState(device)).append(" ").append(srv.overworld().getBlockEntity(device)).append("; ");
						continue;
					}
					control.cycleHoa(device);
					control.cycleHoa(device);
				}
				return errors.toString();
			});
			check(lands.isEmpty(), "every signal should land: " + lands);
			context.waitTicks(25);
			check(status.get().contains("devices reached") && !status.get().contains("NO 24 V") && !status.get().contains("BAD JOINT"), "the panel should be wired: " + status.get());
			film = context.computeOnClient(client -> Filming.start(client, "plcPower"));
			java.util.List<?> errors = server.computeOnServer(srv -> cabinet.apply(srv).download(POWER_PROJECT));
			check(errors.isEmpty(), "the program should compile: " + errors);
			server.runOnServer(srv -> cabinet.apply(srv).cpu().setMode(dev.wildcatter.plc.Cpu.Mode.RUN));
			context.waitTicks(10);
			java.util.function.Function<String, Object> tag = name -> server.computeOnServer(srv -> cabinet.apply(srv).cpu().read("SITE." + name));
			// the operator's screen, laid out through its real editor
			server.runOnServer(srv -> ((dev.wildcatter.control.HmiPanelBlockEntity) srv.overworld().getBlockEntity(hmiAt)).open(srv.getPlayerList().getPlayers().get(0)));
			context.waitTicks(20);
			check(context.computeOnClient(client -> client.gui.screen() instanceof dev.wildcatter.client.HmiScreen), "the HMI should open");
			java.util.function.BiConsumer<String, String[]> add = (kind, v) -> {
				context.runOnClient(client -> ((dev.wildcatter.client.HmiScreen) client.gui.screen()).kindForTest(kind));
				context.waitTick();
				hmiField(context, "label", v[0]);
				hmiField(context, "tag", v[1]);
				hmiField(context, "low", v[2]);
				hmiField(context, "high", v[3]);
				hmiClick(context, "ADD");
				context.waitTick();
			};
			hmiClick(context, "EDIT");
			context.waitTicks(2);
			add.accept("BUTTON", new String[] {"SITE START", "SITE.GO", "0", "1"});
			add.accept("VALUE", new String[] {"TOTAL KW", "SITE.TOTAL_KW", "0", "100"});
			add.accept("LAMP", new String[] {"SHEDDING", "SITE.SHED", "0", "1"});
			add.accept("BAR", new String[] {"SET A KW", "SITE.A_KW", "0", "100"});
			add.accept("BAR", new String[] {"SET B KW", "SITE.B_KW", "0", "100"});
			add.accept("LAMP", new String[] {"FEEDER 3", "SITE.F3_CLOSED", "0", "1"});
			add.accept("TREND", new String[] {"TOTAL KW", "SITE.TOTAL_KW", "0", "150"});
			check(context.computeOnClient(client -> ((dev.wildcatter.client.HmiScreen) client.gui.screen()).layoutSizeForTest()) == 7, "the screen should hold what was added");
			hmiClick(context, "ALARMS");
			context.waitTicks(2);
			hmiField(context, "alarm tag", "SITE.SHED");
			hmiField(context, "op", "=");
			hmiField(context, "limit", "1");
			hmiField(context, "pri", "1");
			hmiField(context, "message", "Load shed: feeder 3 open");
			hmiClick(context, "ADD ALARM");
			context.waitTick();
			hmiClick(context, "RUN");
			context.waitTicks(5);
			context.takeScreenshot("wildcatter-plc-hmi-stopped");
			// the operator's start button, by the real mouse
			plcMouseAt(context, context.computeOnClient(client -> ((dev.wildcatter.client.HmiScreen) client.gui.screen()).widgetForTest(0)));
			context.waitTicks(40);
			check(Boolean.TRUE.equals(tag.apply("GO")), "the HMI's button should write the tag");
			String a = read(server, 12, up, z);
			System.out.println("[wildcatter-test] plc power: after start: " + a);
			context.waitTicks(1200);
			String stateA = read(server, 12, up, z);
			String stateB = read(server, 12, up, z + 16);
			System.out.println("[wildcatter-test] plc power: A " + stateA + " | B " + stateB);
			check(Boolean.TRUE.equals(tag.apply("A_CLOSED")) && Boolean.TRUE.equals(tag.apply("B_CLOSED")), "the PLC should have run and closed both sets: " + stateA + " | " + stateB);
			check(Boolean.TRUE.equals(tag.apply("F1_CLOSED")) && Boolean.TRUE.equals(tag.apply("F2_CLOSED")), "and closed the feeders: " + read(server, 14, up, feeders[0].getZ()));
			// feeder 3 took the load over the limit, so it was shed
			context.waitTicks(200);
			double total = ((Number) tag.apply("TOTAL_KW")).doubleValue();
			System.out.println("[wildcatter-test] plc power: total " + total + " kW, shed " + tag.apply("SHED") + ", feeder 3 " + read(server, 14, up, feeders[2].getZ()));
			check(Boolean.TRUE.equals(tag.apply("SHED")), "over its limit the PLC should have shed: " + total + " kW");
			check(!Boolean.TRUE.equals(tag.apply("F3_CLOSED")), "feeder 3 should be open: " + read(server, 14, up, feeders[2].getZ()));
			check(total > 80 && total < 100, "with feeder 3 off, two feeders of 45 kW each should be the load: " + total);
			context.takeScreenshot("wildcatter-plc-hmi-running");
			// a shift of share: A's speed trim up takes load from B
			double aBefore = ((Number) tag.apply("A_KW")).doubleValue();
			double bBefore = ((Number) tag.apply("B_KW")).doubleValue();
			server.runOnServer(srv -> cabinet.apply(srv).cpu().write("SITE.TRIM_A", 65.0));
			context.waitTicks(40);
			double aAfter = ((Number) tag.apply("A_KW")).doubleValue();
			double bAfter = ((Number) tag.apply("B_KW")).doubleValue();
			System.out.println("[wildcatter-test] plc power: share A " + aBefore + " B " + bBefore + " kW, trimmed A " + aAfter + " B " + bAfter);
			check(aAfter > aBefore && bAfter < bBefore, "a trim should shift the load between the sets: " + aBefore + "/" + bBefore + " to " + aAfter + "/" + bAfter);
			server.runOnServer(srv -> cabinet.apply(srv).cpu().write("SITE.TRIM_A", 50.0));
			// the alarm: active and unacknowledged, then acknowledged, by the real mouse
			hmiClick(context, "ALARMS");
			context.waitTicks(30);
			var hmi = (java.util.function.Supplier<dev.wildcatter.control.HmiPanelBlockEntity.AlarmState>) () -> server.computeOnServer(srv -> ((dev.wildcatter.control.HmiPanelBlockEntity) srv.overworld().getBlockEntity(hmiAt)).alarmState(0));
			check(hmi.get() == dev.wildcatter.control.HmiPanelBlockEntity.AlarmState.UNACKED, "the shed should raise an alarm: " + hmi.get());
			context.takeScreenshot("wildcatter-plc-hmi-alarm");
			hmiClick(context, "ACK");
			context.waitTicks(15);
			check(hmi.get() == dev.wildcatter.control.HmiPanelBlockEntity.AlarmState.ACKED, "ACK should acknowledge it: " + hmi.get());
			check(server.computeOnServer(srv -> ((dev.wildcatter.control.HmiPanelBlockEntity) srv.overworld().getBlockEntity(hmiAt)).trendSamples("SITE.TOTAL_KW")) > 30, "the trend should have samples");
			hmiClick(context, "SHELVE");
			context.waitTicks(15);
			check(hmi.get() == dev.wildcatter.control.HmiPanelBlockEntity.AlarmState.SHELVED, "SHELVE should shelve it: " + hmi.get());
			hmiClick(context, "RUN");
			context.waitTicks(5);
			// a protective trip beats the PLC: feeder 1 tripped by hand of its protection stays out, its close coil does nothing, until a hand resets it
			server.runOnServer(srv -> ((dev.wildcatter.block.BreakerBlockEntity) srv.overworld().getBlockEntity(feeders[0])).tripByRelay("test"));
			context.waitTicks(40);
			check(read(server, 14, up, feeders[0].getZ()).toUpperCase(java.util.Locale.ROOT).contains("TRIP"), "a trip should stand against the PLC's close: " + read(server, 14, up, feeders[0].getZ()));
			// the operator stops it: sets stop and open
			plcMouseAt(context, context.computeOnClient(client -> ((dev.wildcatter.client.HmiScreen) client.gui.screen()).widgetForTest(0)));
			context.waitTicks(60);
			check(!Boolean.TRUE.equals(tag.apply("A_RUNNING")) && !Boolean.TRUE.equals(tag.apply("B_RUNNING")), "the HMI's button should stop both sets");
			context.runOnClient(client -> client.gui.screen().onClose());
			context.waitTicks(5);
			// a cable lost: the sets drop with the commands, as a dead PLC is a stopped plant
			System.out.println("[wildcatter-test] plcPower ok: " + status.get());
		} finally {
			if (film != null) {
				film.stop();
			}
		}
	}

	/** Phase 13h (docs/multiblocks/plc.md): a starter and a button on a remote drop 60 blocks from their PLC; a second PLC taking its tags; an HMI on either; a cut network. */
	private void plcNetwork(ClientGameTestContext context, TestServerContext server) {
		int z = -3800;
		int mid = z + 30;
		int far = z + 60;
		server.runCommand("tp @p 18 " + (up + 2) + " " + (mid + 4) + " 180 10");
		context.waitTicks(20);
		for (int zz = z - 4; zz <= far + 6; zz += 20) {
			server.runCommand("fill 8 " + (y - 1) + " " + zz + " 24 " + (y + 6) + " " + Math.min(zz + 19, far + 6) + " minecraft:air");
			server.runCommand("fill 8 " + (y - 1) + " " + zz + " 24 " + (y - 1) + " " + Math.min(zz + 19, far + 6) + " minecraft:stone");
		}
		Filming film = null;
		try {
			film = context.computeOnClient(client -> Filming.start(client, "plcNetwork"));
			BlockPos master = plcBench(context, server, z, dev.wildcatter.control.PlcModuleItem.Type.COMM);
			BlockPos second = plcBench(context, server, mid, dev.wildcatter.control.PlcModuleItem.Type.COMM, dev.wildcatter.control.PlcModuleItem.Type.DO16);
			// the drop: its own 24 V and control cables, no CPU, on the far end of the network
			plant(server, far);
			put(server, 12, up, far, "wildcatter:plastic_wire_hot");
			put(server, 13, up, far, "wildcatter:control_supply");
			for (int x = 11; x <= 13; x++) {
				put(server, x, up, far + 1, "wildcatter:plastic_wire_neutral");
			}
			put(server, 14, up, far, "wildcatter:junction_box");
			BlockPos dropAt = new BlockPos(15, up, far);
			put(server, 15, up, far, "wildcatter:remote_io_drop[facing=south]");
			server.runOnServer(srv -> dev.wildcatter.multiblock.BigParts.grow(srv.overworld(), dropAt, srv.overworld().getBlockState(dropAt)));
			BlockPos starterAt = new BlockPos(14, up, far - 1);
			BlockPos button = new BlockPos(14, up, far + 1);
			put(server, 14, up, far - 1, "wildcatter:motor_starter[facing=east]");
			put(server, 14, up, far + 1, "wildcatter:pushbutton[facing=south]");
			BlockPos lamp = new BlockPos(14, up + 1, mid);
			put(server, 14, up + 1, mid, "wildcatter:pilot_light[facing=south]");
			// the network: each cabinet's east side out to a run down x 20; the HMI on it, by the second PLC
			for (int line : new int[] {z, mid, far}) {
				for (int x = 16; x <= 19; x++) {
					put(server, x, up, line, "wildcatter:network_cable");
				}
			}
			for (int zz = z; zz <= far; zz++) {
				put(server, 20, up, zz, "wildcatter:network_cable");
			}
			BlockPos hmiAt = new BlockPos(21, up, mid);
			put(server, 21, up, mid, "wildcatter:hmi_panel[facing=east]");
			java.util.function.Function<net.minecraft.server.MinecraftServer, dev.wildcatter.control.PlcCabinetBlockEntity> plc1 = srv -> (dev.wildcatter.control.PlcCabinetBlockEntity) srv.overworld().getBlockEntity(master);
			java.util.function.Function<net.minecraft.server.MinecraftServer, dev.wildcatter.control.PlcCabinetBlockEntity> plc2 = srv -> (dev.wildcatter.control.PlcCabinetBlockEntity) srv.overworld().getBlockEntity(second);
			java.util.function.Function<net.minecraft.server.MinecraftServer, dev.wildcatter.control.RemoteIoDropBlockEntity> drop = srv -> (dev.wildcatter.control.RemoteIoDropBlockEntity) srv.overworld().getBlockEntity(dropAt);
			server.runOnServer(srv -> {
				drop.apply(srv).seat(dev.wildcatter.control.PlcModuleItem.Type.DI16);
				drop.apply(srv).seat(dev.wildcatter.control.PlcModuleItem.Type.DO16);
				String nope = drop.apply(srv).seat(dev.wildcatter.control.PlcModuleItem.Type.CPU_SMALL);
				check(nope != null, "a drop takes no CPU");
				plc1.apply(srv).setStation("PLC-1");
				plc2.apply(srv).setStation("PLC-2");
			});
			context.waitTicks(30);
			String landed = server.computeOnServer(srv -> {
				var box = plc1.apply(srv);
				String out = "";
				for (String one : new String[] {box.land(button, 0, "%IX192.0"), box.land(starterAt, 1, "%IX192.1"), box.land(starterAt, 2, "%IX192.2"), box.land(starterAt, 0, "%QX208.0")}) {
					if (one != null) {
						out += one + "; ";
					}
				}
				String lampOut = plc2.apply(srv).land(lamp, 0, "%QX16.0");
				out += lampOut == null ? "" : lampOut;
				dev.wildcatter.control.ControlNet.deviceAt(srv.overworld(), starterAt).cycleHoa(starterAt);
				dev.wildcatter.control.ControlNet.deviceAt(srv.overworld(), starterAt).cycleHoa(starterAt);
				box.download("PROGRAM MAIN\nVAR\n btn AT %IX192.0 : BOOL;\n fed AT %IX192.1 : BOOL;\n bad AT %IX192.2 : BOOL;\n coil AT %QX208.0 : BOOL;\n scans : DINT;\nEND_VAR\ncoil := (btn OR coil) AND NOT bad;\nscans := scans + 1;\nEND_PROGRAM");
				box.cpu().setMode(dev.wildcatter.plc.Cpu.Mode.RUN);
				plc2.apply(srv).download("PROGRAM LINK\nVAR\n seen : BOOL;\n light AT %QX16.0 : BOOL;\n count : DINT;\nEND_VAR\nlight := seen;\nEND_PROGRAM");
				plc2.apply(srv).cpu().setMode(dev.wildcatter.plc.Cpu.Mode.RUN);
				return out;
			});
			check(landed.isEmpty(), "the drop's signals should land on the PLC, by rack: " + landed + " | " + read(server, 15, up, z) + " | " + read(server, 15, up, far));
			context.waitTicks(30);
			check(read(server, 15, up, z).contains("rack") || server.computeOnServer(srv -> plc1.apply(srv).drops().size()) == 1, "the PLC should see its drop");
			java.util.function.Supplier<Boolean> running = () -> server.computeOnServer(srv -> ((dev.wildcatter.block.MotorStarterBlockEntity) srv.overworld().getBlockEntity(starterAt)).readPoint(1));
			// PLC-2's screen: its tag taken over the network, by real clicks
			server.runOnServer(srv -> plc2.apply(srv).open(srv.getPlayerList().getPlayers().get(0)));
			context.waitTicks(20);
			plcClick(context, "NET");
			context.waitTicks(5);
			plcField(context, "exchange", "LINK.SEEN = PLC-1/MAIN.COIL");
			plcClick(context, "ADD ROW");
			context.waitTicks(5);
			plcField(context, "exchange", "LINK.COUNT = PLC-1/MAIN.SCANS");
			plcClick(context, "ADD ROW");
			context.waitTicks(30);
			check(server.computeOnServer(srv -> plc2.apply(srv).exchange().size()) == 2, "the rows should reach the cabinet");
			check(server.computeOnServer(srv -> plc2.apply(srv).exchangeFailed().isEmpty()), "both should read: " + server.computeOnServer(srv -> plc2.apply(srv).exchangeFailed().toString()));
			context.takeScreenshot("wildcatter-plc-network-tab");
			context.runOnClient(client -> client.gui.screen().onClose());
			context.waitTicks(5);
			// the button on the drop, across the network, runs the starter on the drop
			server.runOnServer(srv -> ((dev.wildcatter.control.OperatorDeviceBlockEntity) srv.overworld().getBlockEntity(button)).operate());
			context.waitTicks(40);
			check(running.get(), "a button on a remote drop should run a starter on it: " + read(server, 14, up, far - 1) + " | " + read(server, 15, up, z));
			check(Boolean.TRUE.equals(server.computeOnServer(srv -> plc2.apply(srv).cpu().read("LINK.SEEN"))), "the second PLC should take its tag");
			check(server.computeOnServer(srv -> srv.overworld().getBlockState(lamp).getValue(dev.wildcatter.control.OperatorDeviceBlock.LAMPS)) == 1, "and light its own lamp from it");
			check(((Number) server.computeOnServer(srv -> plc2.apply(srv).cpu().read("LINK.COUNT"))).longValue() > 10, "and a number comes across too");
			// an HMI reads and writes either PLC by station, and a marker as a register
			java.util.function.Function<net.minecraft.server.MinecraftServer, java.util.List<dev.wildcatter.control.PlcCabinetBlockEntity>> netPlcs = srv -> dev.wildcatter.control.NetworkNet.plcs(srv.overworld(), java.util.List.of(hmiAt));
			check(server.computeOnServer(srv -> netPlcs.apply(srv).size()) == 2, "an HMI should see both PLCs: " + read(server, 21, up, mid));
			check(Boolean.TRUE.equals(server.computeOnServer(srv -> dev.wildcatter.control.NetworkNet.read(netPlcs.apply(srv), "PLC-1/MAIN.COIL"))), "by station");
			check(server.computeOnServer(srv -> dev.wildcatter.control.NetworkNet.write(netPlcs.apply(srv), "PLC-2/%MW10", 1234L)), "a marker takes a register's write");
			check(((Number) server.computeOnServer(srv -> dev.wildcatter.control.NetworkNet.read(netPlcs.apply(srv), "PLC-2/%MW10"))).longValue() == 1234, "and reads it back");
			// the network cut between them: the drop's outputs stop arriving, so the starter falls back, and the exchange says FAILED
			put(server, 20, up, mid + 10, "minecraft:air");
			context.waitTicks(40);
			check(!running.get(), "a cut network should stop a remote starter: " + read(server, 14, up, far - 1));
			put(server, 20, up, mid - 10, "minecraft:air");
			context.waitTicks(30);
			check(server.computeOnServer(srv -> plc2.apply(srv).exchangeFailed().size()) == 2, "and a tag exchange should say it failed");
			check(Boolean.TRUE.equals(server.computeOnServer(srv -> plc2.apply(srv).cpu().read("LINK.SEEN"))), "holding its last value");
			put(server, 20, up, mid + 10, "wildcatter:network_cable");
			put(server, 20, up, mid - 10, "wildcatter:network_cable");
			context.waitTicks(40);
			// a save and load keeps the rack and the exchange
			boolean kept = server.computeOnServer(srv -> {
				var registries = srv.registryAccess();
				var box = plc2.apply(srv);
				net.minecraft.nbt.CompoundTag saved = box.saveCustomOnly(registries);
				var copy = new dev.wildcatter.control.PlcCabinetBlockEntity(second, box.getBlockState());
				copy.setLevel(srv.overworld());
				copy.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, registries, saved));
				return copy.exchange().size() == 2 && copy.station().equals("PLC-2");
			});
			check(kept, "a save and load should keep the exchange and the name");
			// a long trunk: about 2,400 cells of control cable, rows two apart in four layers, with a lamp at its far end
			int trunk = server.computeOnServer(srv -> {
				var level = srv.overworld();
				var cable = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse("wildcatter:control_cable")).defaultBlockState();
				java.util.List<BlockPos> laid = new java.util.ArrayList<>();
				int z0 = z - 2;
				int z1 = far + 4;
				BlockPos at = null;
				int row = 0;
				for (int layer = 0; layer < 4; layer++) {
					int yb = up + 8 + 2 * layer;
					for (int r = 0; r < 9; r++, row++) {
						int x = 8 + 2 * (layer % 2 == 0 ? r : 8 - r);
						BlockPos from = new BlockPos(x, yb, row % 2 == 0 ? z0 : z1);
						BlockPos to = new BlockPos(x, yb, row % 2 == 0 ? z1 : z0);
						for (BlockPos step : java.util.List.of(from, to)) {
							while (at != null && !at.equals(step)) {
								at = at.offset(Integer.signum(step.getX() - at.getX()), Integer.signum(step.getY() - at.getY()), Integer.signum(step.getZ() - at.getZ()));
								level.setBlock(at, cable, 2);
								laid.add(at);
							}
							if (at == null) {
								at = step;
								level.setBlock(at, cable, 2);
								laid.add(at);
							}
						}
					}
				}
				BlockPos end = at.relative(row % 2 == 0 ? net.minecraft.core.Direction.NORTH : net.minecraft.core.Direction.SOUTH);
				level.setBlock(end, net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse("wildcatter:pilot_light")).defaultBlockState(), 2);
				var found = dev.wildcatter.control.ControlNet.walk(level, java.util.List.of(laid.get(0).north()));
				boolean reached = found.devices().contains(end) && found.badJoint() == null;
				for (BlockPos one : laid) {
					level.setBlock(one, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
				}
				level.setBlock(end, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
				return reached ? laid.size() : -laid.size();
			});
			check(trunk > 2000, "a cabinet should reach a device at the end of a long trunk (" + Math.abs(trunk) + " cells)");
			System.out.println("[wildcatter-test] plcNetwork ok: " + read(server, 15, up, z) + " | " + read(server, 15, up, far));
		} finally {
			if (film != null) {
				film.stop();
			}
		}
	}

	/** A control cable along these cells, refusing to overwrite anything: a true-size part it replaced would come apart. */
	private void controlRun(TestServerContext server, String block, int[]... cells) {
		for (int[] c : cells) {
			boolean clear = server.computeOnServer(srv -> srv.overworld().getBlockState(new BlockPos(c[0], c[1], c[2])).isAir());
			check(clear, "a control run should not overwrite " + server.computeOnServer(srv -> srv.overworld().getBlockState(new BlockPos(c[0], c[1], c[2])).toString()) + " at " + c[0] + " " + c[1] + " " + c[2]);
			put(server, c[0], c[1], c[2], block);
		}
	}

	private int[][] straight(int x0, int y0, int z0, int x1, int y1, int z1) {
		java.util.List<int[]> out = new java.util.ArrayList<>();
		int n = Math.max(Math.abs(x1 - x0), Math.max(Math.abs(y1 - y0), Math.abs(z1 - z0)));
		for (int i = 0; i <= n; i++) {
			out.add(new int[] {x0 + Integer.signum(x1 - x0) * i, y0 + Integer.signum(y1 - y0) * i, z0 + Integer.signum(z1 - z0) * i});
		}
		return out.toArray(new int[0][]);
	}

	/** A cabinet with a CPU and modules, its own 24 V from a small steam set at x 10-11, a box at x 14: the trunk starts on that box's top. */
	private BlockPos plcBench(ClientGameTestContext context, TestServerContext server, int z, dev.wildcatter.control.PlcModuleItem.Type... modules) {
		plant(server, z);
		put(server, 12, up, z, "wildcatter:plastic_wire_hot");
		put(server, 13, up, z, "wildcatter:control_supply");
		for (int x = 11; x <= 13; x++) {
			put(server, x, up, z + 1, "wildcatter:plastic_wire_neutral");
		}
		put(server, 14, up, z, "wildcatter:junction_box");
		BlockPos at = new BlockPos(15, up, z);
		put(server, 15, up, z, "wildcatter:plc_cabinet[facing=south]");
		server.runOnServer(srv -> dev.wildcatter.multiblock.BigParts.grow(srv.overworld(), at, srv.overworld().getBlockState(at)));
		context.waitTicks(25);
		server.runOnServer(srv -> {
			var box = (dev.wildcatter.control.PlcCabinetBlockEntity) srv.overworld().getBlockEntity(at);
			box.seat(dev.wildcatter.control.PlcModuleItem.Type.CPU_SMALL);
			for (var type : modules) {
				box.seat(type);
			}
		});
		return at;
	}

	private dev.wildcatter.client.HmiScreen hmiScreen(net.minecraft.client.Minecraft client) {
		return (dev.wildcatter.client.HmiScreen) client.gui.screen();
	}

	private void hmiClick(ClientGameTestContext context, String label) {
		int[] at = context.computeOnClient(client -> hmiScreen(client).buttonForTest(label));
		check(at != null, "the HMI's " + label + " button should be there");
		plcMouseAt(context, at);
	}

	private void hmiField(ClientGameTestContext context, String label, String value) {
		check(context.computeOnClient(client -> hmiScreen(client).setFieldForTest(label, value)), "the HMI's " + label + " field should be there");
	}

	private void plcMouseAt(ClientGameTestContext context, int[] at) {
		plcMouse(context, at);
	}

	/** Every block and item of the mod has its files. The asset generator clears what it does not know about, so a hand-made file can vanish without an error anywhere else. */
	private void checkAssets(ClientGameTestContext context) {
		java.util.List<String> missing = context.computeOnClient(client -> {
			java.util.List<String> out = new java.util.ArrayList<>();
			var resources = client.getResourceManager();
			for (var block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
				var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
				if (id.getNamespace().equals("wildcatter") && resources.getResource(net.minecraft.resources.Identifier.fromNamespaceAndPath("wildcatter", "blockstates/" + id.getPath() + ".json")).isEmpty()) {
					out.add("blockstate " + id.getPath());
				}
			}
			for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
				var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
				if (id.getNamespace().equals("wildcatter") && resources.getResource(net.minecraft.resources.Identifier.fromNamespaceAndPath("wildcatter", "items/" + id.getPath() + ".json")).isEmpty()) {
					out.add("item " + id.getPath());
				}
			}
			return out;
		});
		check(missing.isEmpty(), "these have no assets: " + missing);
	}

	private dev.wildcatter.client.PlcScreen plcScreen(net.minecraft.client.Minecraft client) {
		return (dev.wildcatter.client.PlcScreen) client.gui.screen();
	}

	/** Presses a labelled button in the PLC screen with the real mouse. */
	private void plcClick(ClientGameTestContext context, String label) {
		int[] at = context.computeOnClient(client -> plcScreen(client).buttonForTest(label));
		check(at != null, "the " + label + " button should be there");
		double toWindow = context.computeOnClient(client -> (double) client.getWindow().getScreenWidth() / client.getWindow().getGuiScaledWidth());
		context.getInput().setCursorPos(at[0] * toWindow, at[1] * toWindow);
		context.waitTick();
		context.getInput().pressMouse(1);
		context.waitTick();
	}

	private void plcField(ClientGameTestContext context, String label, String value) {
		check(context.computeOnClient(client -> plcScreen(client).setFieldForTest(label, value)), "the " + label + " field should be there");
	}

	/** Phase 13c: the same motor from a ladder drawn with the real toolbar, a live view, forcing, an online change, and an FBD beside it. */
	private void plcLadder(ClientGameTestContext context, TestServerContext server, java.util.function.Function<net.minecraft.server.MinecraftServer, dev.wildcatter.control.PlcCabinetBlockEntity> cabinet,
			java.util.function.Supplier<String> status, java.util.function.Supplier<String> run, BlockPos button, BlockPos estop, BlockPos stack) {
		context.waitTicks(1200);   // the overload relay cools from the last round of starts
		server.runOnServer(srv -> cabinet.apply(srv).open(srv.getPlayerList().getPlayers().get(0)));
		context.waitTicks(20);
		// a project far past a vanilla client packet's 32 KB
		context.runOnClient(client -> {
			StringBuilder big = new StringBuilder("FUNCTION_BLOCK BIG\n(*");
			for (int i = 0; i < 4000; i++) {
				big.append("A long comment, to make a big project. ");
			}
			plcScreen(client).loadUnitForTest(dev.wildcatter.plc.lang.Project.Language.ST, "BIG", big + "*)\nEND_FUNCTION_BLOCK");
		});
		plcClick(context, "DOWNLOAD");
		context.waitTicks(40);
		check(server.computeOnServer(srv -> cabinet.apply(srv).project().size()) == 2, "a 150 KB project should reach the cabinet: " + server.computeOnServer(srv -> cabinet.apply(srv).project().size()));
		// and it comes back: a packet's string holds 65,535 bytes, so a screen opening on this project has to take it in pieces
		context.runOnClient(client -> client.gui.screen().onClose());
		context.waitTicks(5);
		server.runOnServer(srv -> cabinet.apply(srv).open(srv.getPlayerList().getPlayers().get(0)));
		context.waitTicks(20);
		check(context.computeOnClient(client -> client.gui.screen() instanceof dev.wildcatter.client.PlcScreen && plcScreen(client).unitCountForTest() == 2), "a screen should open on a 150 KB project and have both its units");
		context.runOnClient(client -> plcScreen(client).selectUnitForTest("BIG"));
		context.waitTick();
		// and it saves: a saved string holds 65,535 bytes, and this unit is one line of 156,000
		String saved = server.computeOnServer(srv -> {
			try {
				var out = new java.io.ByteArrayOutputStream();
				net.minecraft.nbt.NbtIo.write(cabinet.apply(srv).saveCustomOnly(srv.registryAccess()), new java.io.DataOutputStream(out));
				return "ok " + out.size();
			} catch (java.io.IOException | RuntimeException e) {
				return e.toString();
			}
		});
		check(saved.startsWith("ok"), "a cabinet holding a 150 KB unit should write to disk: " + saved);
		plcClick(context, "DELETE UNIT");   // BIG is not first, so it selects MAIN and we delete below
		// draw the ladder with the toolbar
		plcClick(context, "+LD");
		context.waitTicks(5);
		context.runOnClient(client -> plcScreen(client).selectUnitForTest("MAIN"));
		context.waitTicks(5);
		plcClick(context, "DELETE UNIT");
		context.waitTicks(5);
		plcClick(context, "VARS");
		context.runOnClient(client -> plcScreen(client).replaceForTest("""
				VAR
					start AT %IX0.0 : BOOL;
					healthy AT %IX0.1 : BOOL;
					running AT %IX0.2 : BOOL;
					tripped AT %IX0.3 : BOOL;
					coil AT %QX16.0 : BOOL;
					lamp AT %QX16.1 : BOOL;
				END_VAR"""));
		plcClick(context, "DRAW");
		plcClick(context, "RUNG");
		plcClick(context, "NO");
		plcField(context, "variable", "start");
		plcClick(context, "BRANCH");
		plcField(context, "variable", "coil");
		context.runOnClient(client -> plcScreen(client).deselectForTest());
		context.waitTick();
		plcClick(context, "NO");
		plcField(context, "variable", "healthy");
		plcClick(context, "NC");
		plcField(context, "variable", "tripped");
		plcClick(context, "COIL");
		plcField(context, "variable", "coil");
		plcClick(context, "RUNG");
		plcClick(context, "NO");
		plcField(context, "variable", "running");
		plcClick(context, "COIL");
		plcField(context, "variable", "lamp");
		context.waitTick();
		context.runOnClient(client -> System.out.println("[wildcatter-test] plc ladder saved: " + plcScreen(client).savedForTest().replace("\n", " / ")));
		plcClick(context, "DOWNLOAD");
		context.waitTicks(20);
		check(context.computeOnClient(client -> plcScreen(client).errorsForTest().isEmpty()), "the drawn ladder should compile: " + context.computeOnClient(client -> plcScreen(client).errorsForTest().toString()));
		check(server.computeOnServer(srv -> cabinet.apply(srv).project().size() == 1 && cabinet.apply(srv).project().getFirst().language() == dev.wildcatter.plc.lang.Project.Language.LD), "the ladder should reach the cabinet");
		check(status.get().contains("STOP"), "a download stops the CPU: " + status.get());
		plcClick(context, "RUN");
		context.waitTicks(30);
		check(status.get().contains("RUN"), "RUN, from the screen: " + status.get());
		check(context.computeOnClient(client -> plcScreen(client).liveForTest()), "the ladder should go live");
		context.takeScreenshot("wildcatter-plc-ladder-idle");
		server.runOnServer(srv -> ((dev.wildcatter.control.OperatorDeviceBlockEntity) srv.overworld().getBlockEntity(button)).operate());
		context.waitTicks(120);
		check(run.get().contains("RUNNING"), "the drawn ladder should start the motor: " + run.get());
		context.takeScreenshot("wildcatter-plc-ladder-running");
		// the power it draws is what the CPU computed: the seal branch has power, and the start branch does not, now the button is out
		java.util.Map<String, String> shown = context.computeOnClient(client -> new java.util.HashMap<>(plcScreen(client).valuesForTest()));
		check(shown.values().stream().filter("true"::equals).count() >= 3 && shown.containsValue("false"), "the live view should show power and no power: " + shown);
		// an online change: a rung added with the motor running
		plcClick(context, "RUNG");
		plcClick(context, "NO");
		plcField(context, "variable", "running");
		plcClick(context, "ONLINE CHG");
		context.waitTicks(20);
		check(status.get().contains("RUN") && run.get().contains("RUNNING"), "an online change should not stop anything: " + status.get() + " | " + run.get());
		// a jump: while the motor runs, rung 4 jumps over rung 5 to the rung labelled DONE, and the rung passed over shows no power
		plcClick(context, "RUNG");
		plcClick(context, "NO");
		plcField(context, "variable", "running");
		plcClick(context, ">>");
		plcField(context, "jump to", "DONE");
		plcClick(context, "RUNG");
		plcClick(context, "NO");
		plcField(context, "variable", "running");
		plcClick(context, "RUNG");
		plcField(context, "label", "DONE");
		plcClick(context, "NO");
		plcField(context, "variable", "running");
		plcClick(context, "ONLINE CHG");
		context.waitTicks(40);
		check(context.computeOnClient(client -> plcScreen(client).errorsForTest().isEmpty()), "the jump should compile: " + context.computeOnClient(client -> plcScreen(client).errorsForTest().toString()));
		check(status.get().contains("RUN") && run.get().contains("RUNNING"), "adding a jump online should not stop anything: " + status.get() + " | " + run.get());
		String jumped = context.computeOnClient(client -> plcScreen(client).rungPowerForTest(5) + " " + plcScreen(client).rungPowerForTest(6));
		check(jumped.equals("false true"), "the rung jumped over should show no power and the labelled rung its own: " + jumped);
		context.takeScreenshot("wildcatter-plc-ladder-jump");
		// an FBD beside the ladder, in one project: run a green lamp a second after the motor starts
		String fbd = """
				PROGRAM GREEN
				VAR
					running AT %IX0.2 : BOOL;
					green AT %QX16.2 : BOOL;
				END_VAR
				---
				("B" "1" "IN_VAR" "running" "4" "8")
				("B" "2" "TON" "SETTLE" "110" "8" ("IN" "@1.OUT") ("PT" "T#1s"))
				("B" "3" "OUT_VAR" "green" "240" "8" ("IN" "@2.Q"))
				""";
		context.runOnClient(client -> plcScreen(client).loadUnitForTest(dev.wildcatter.plc.lang.Project.Language.FBD, "GREEN", fbd));
		context.waitTick();
		BlockPos stackAt = stack;
		server.runOnServer(srv -> cabinet.apply(srv).land(stackAt, 2, "%QX16.2"));
		plcClick(context, "ONLINE CHG");
		context.waitTicks(60);
		check(context.computeOnClient(client -> plcScreen(client).errorsForTest().isEmpty()), "the FBD should compile: " + context.computeOnClient(client -> plcScreen(client).errorsForTest().toString()));
		check(server.computeOnServer(srv -> srv.overworld().getBlockState(stackAt).getValue(dev.wildcatter.control.OperatorDeviceBlock.LAMPS)) == 4, "the FBD's timer should light the stack's green lamp");
		context.takeScreenshot("wildcatter-plc-fbd");
		// the e-stop, then a force: the ONLINE tab starts the motor with nobody at the button
		server.runOnServer(srv -> ((dev.wildcatter.control.OperatorDeviceBlockEntity) srv.overworld().getBlockEntity(estop)).operate());
		context.waitTicks(20);
		check(run.get().contains("STOPPED"), "the e-stop still stops it: " + run.get());
		server.runOnServer(srv -> ((dev.wildcatter.control.OperatorDeviceBlockEntity) srv.overworld().getBlockEntity(estop)).operate());
		context.waitTicks(10);
		plcClick(context, "ONLINE");
		plcField(context, "tag or address", "%IX0.0");
		plcField(context, "value", "TRUE");
		plcClick(context, "WATCH");
		plcClick(context, "FORCE");
		context.waitTicks(120);
		check(status.get().contains("FORCING") && run.get().contains("RUNNING"), "a forced input should start it, with the light on: " + status.get() + " | " + run.get());
		context.takeScreenshot("wildcatter-plc-online");
		plcClick(context, "RELEASE");
		context.waitTicks(10);
		check(!status.get().contains("FORCING") && run.get().contains("RUNNING"), "released, the seal holds it: " + status.get() + " | " + run.get());
		context.runOnClient(client -> client.gui.screen().onClose());
		context.waitTicks(5);
	}

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
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");   // its camera spots are high up: the player must not die falling
		server.runCommand("tp @p 20 " + (up + 6) + " " + (z + 12) + " 180 20");
		context.waitTicks(20);
		server.runCommand("fill 0 " + (y - 1) + " " + (z - 6) + " 50 " + (y + 22) + " " + (z + 6) + " minecraft:air");
		server.runCommand("fill 0 " + (y - 1) + " " + (z - 6) + " 50 " + (y - 1) + " " + (z + 6) + " minecraft:stone");
		server.runCommand("fill 27 " + y + " " + (z - 6) + " 50 " + y + " " + (z + 6) + " minecraft:stone");   // the yard's ground at y: it stands on it
		BlockPos set = new BlockPos(6, up, z);
		genset(server, set, net.minecraft.core.Direction.NORTH, false);   // the skid runs west
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, set, net.minecraft.core.Direction.EAST, false);   // 480 V up to 4.16 kV, x 7-10
		// 10d2: the 10 MVA unit (x 12-15, z-2..z+2, its 138 kV side east): 4.16 kV cable in at x 11, its bushings' front face at x 16,
		// a cable riser down to the cable at x 16
		put(server, 11, up, z, "wildcatter:cable_mv");
		placeBig(server, WildcatterBlocks.SUBSTATION_TRANSFORMER_10, new BlockPos(12, up, z), net.minecraft.core.Direction.EAST);
		for (int yy = up + 1; yy <= up + 4; yy++) {
			put(server, 16, yy, z, "wildcatter:cable_hv");
		}
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
	 * Conductor sizes: a 40 m run of 12 AWG drops more voltage than the same run in 6 AWG; a 10 AWG cable feeding five 15 kW heaters
	 * (90 A, three times its rating) burns out where 2 AWG holds; a 4/0 ACSR span may not be as long as a 1/0 one; a wire placed without a size takes its
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

		// 2. ampacity: 90 A through one block of cable, 2 AWG (115 A) holds and 10 AWG (30 A, three times over) burns
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
			((dev.wildcatter.block.MultiCableBlockEntity) srv.overworld().getBlockEntity(new BlockPos(13, up, zc))).setSize(dev.wildcatter.energy.ConductorSize.AWG_10);
			return null;
		});
		String burning = "";
		for (int waited = 0; waited < 1200 && !isAir(server, 13, up, zc); waited += 20) {
			burning = read(server, 13, up, zc);
			context.waitTicks(20);
		}
		check(isAir(server, 13, up, zc) && burning.contains("HEATING"), "10 AWG carrying 90 A should heat and burn out: " + burning);
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
		exportBlueprint(server, "genset", new BlockPos(4, y, z - 10), new BlockPos(22, y + 4, z + 4));   // the library (docs/blueprints.md)
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
	 * The lease (5a, docs/multiblocks/lease.md) over a new oil field: a pumpjack on 480 V from a diesel set, its wellhead piped to a
	 * separator; the separator's oil to a stock tank, its gas to a gas genset and a flare, its water to a water tank, then an H-pump
	 * and a disposal wellhead on a dry hole's casing. The water cut reads 10 %, the tank fills, the genset runs on separator gas, the
	 * flare burns what is left, the H-pump injects, buckets go in and out of the stock tank, a joint between the oil and water runs
	 * reads BLOCKED, and with the flare and the genset gone the separator shuts in on gas, as does a pumpjack whose flowline is full.
	 */
	private void lease(ClientGameTestContext context, TestServerContext server) {
		int z = -1020;
		server.runCommand("effect give @p minecraft:slow_falling 600 0 true");   // the camera stands high: no fall, no lost chunks
		server.runCommand("tp @p 20 " + (up + 8) + " " + (z + 22) + " 160 25");
		context.waitTicks(20);   // let the chunks load before building there
		server.runCommand("fill 4 " + (y - 1) + " " + (z - 10) + " 46 " + (y + 12) + " " + (z + 16) + " minecraft:air");
		server.runCommand("fill 4 " + (y - 1) + " " + (z - 10) + " 46 " + (y - 1) + " " + (z + 16) + " minecraft:stone");
		long seed = -1;
		for (long s = 1; s < 2_000_000 && seed < 0; s++) {
			Reservoir oil = Reservoir.at(s, 21, z);   // under the wellhead
			if (oil != null && !oil.deep() && oil.kind() == Reservoir.Kind.OIL && oil.units() > 30_000) {
				seed = s;
			}
		}
		check(seed >= 0, "no seed puts an oil field under the lease's wellhead");
		ReservoirData.testSeed = seed;
		try {
			// power: a diesel set, 480 V cable east to the pumpjack's terminal box and south then east to the H-pump's
			BlockPos set = new BlockPos(11, up, z);
			genset(server, set, net.minecraft.core.Direction.NORTH, false);
			BlockPos well = pumpjack(server, new BlockPos(15, up, z), net.minecraft.core.Direction.EAST);   // box at x 15, wellhead at x 21
			placeBig(server, WildcatterBlocks.SEPARATOR, new BlockPos(23, up, z), net.minecraft.core.Direction.WEST);   // x 23-26, z-1..z, inlet west
			placeBig(server, WildcatterBlocks.STOCK_TANK, new BlockPos(28, up, z - 7), net.minecraft.core.Direction.NORTH);   // x 28-31, z-7..z-4
			placeBig(server, WildcatterBlocks.WATER_TANK, new BlockPos(28, up, z + 3), net.minecraft.core.Direction.NORTH);   // x 28-31, z+3..z+6
			placeBig(server, WildcatterBlocks.INJECTION_PUMP, new BlockPos(22, up, z + 10), net.minecraft.core.Direction.WEST);   // x 22-27, z+9..z+10
			placeBig(server, WildcatterBlocks.FLARE_STACK, new BlockPos(36, up, z + 2), net.minecraft.core.Direction.NORTH);
			put(server, 25, y, z + 13, "wildcatter:casing");   // a dry hole's top casing
			put(server, 25, up, z + 13, "wildcatter:disposal_wellhead");
			BlockPos gasSet = new BlockPos(44, up, z - 4);
			genset(server, gasSet, net.minecraft.core.Direction.NORTH, true);   // engine x 39-42, z-4..z-3
			for (int x = 12; x <= 14; x++) {
				put(server, x, up, z, "wildcatter:cable_industrial");
			}
			for (int zz = z + 1; zz <= z + 10; zz++) {
				put(server, 13, up, zz, "wildcatter:cable_industrial");
			}
			for (int x = 14; x <= 21; x++) {
				put(server, x, up, z + 10, "wildcatter:cable_industrial");   // to the H-pump's terminal box, the west face of its main cell
			}
			server.computeOnServer(srv -> {   // a 100 hp motor draws about 112 A: 4/0 cable for it, not the 115 A default at its limit
				for (BlockPos at : BlockPos.betweenClosed(12, up, z, 21, up, z + 10)) {
					if (srv.overworld().getBlockEntity(at) instanceof dev.wildcatter.block.MultiCableBlockEntity cable) {
						cable.setSize(dev.wildcatter.energy.ConductorSize.AWG_4_0);
					}
				}
				return null;
			});
			// the pipes
			put(server, 22, up, z, "wildcatter:gas_line");   // wellhead to the separator's inlet
			for (int[] p : new int[][] {{27, 1, -1}, {27, 1, -2}, {27, 1, -3}, {27, 0, -3}, {27, 0, -4}}) {
				put(server, p[0], up + p[1], z + p[2], "wildcatter:gas_line");   // oil, out of the back upper cell to the stock tank
			}
			for (int zz = z; zz <= z + 3; zz++) {
				put(server, 27, up, zz, "wildcatter:gas_line");   // water, out of the back lower cell to the water tank
			}
			for (int[] p : new int[][] {{29, 7}, {29, 8}, {29, 9}, {28, 9}}) {
				put(server, p[0], up, z + p[1], "wildcatter:gas_line");   // the water tank to the H-pump's suction
			}
			for (int zz = z + 10; zz <= z + 13; zz++) {
				put(server, 25, up + 1, zz, "wildcatter:gas_line");   // its discharge over to the disposal wellhead
			}
			for (int x = 26; x <= 33; x++) {
				put(server, x, up + 2, z, "wildcatter:gas_line");   // gas off the top
			}
			put(server, 33, up + 1, z, "wildcatter:gas_line");
			for (int x = 33; x <= 40; x++) {
				put(server, x, up, z, "wildcatter:gas_line");
			}
			put(server, 40, up, z - 1, "wildcatter:gas_line");
			put(server, 40, up, z - 2, "wildcatter:gas_line");   // against the gas engine's back
			put(server, 36, up, z + 1, "wildcatter:gas_line");   // the flare's branch

			exportBlueprint(server, "lease", new BlockPos(4, y, z - 10), new BlockPos(46, y + 12, z + 16));   // the library (docs/blueprints.md)
			startGenset(context, server, set, net.minecraft.core.Direction.NORTH, 1000);
			server.computeOnServer(srv -> {   // a day's water already in the tank, so the H-pump's level switch calls it
				((dev.wildcatter.block.StorageTankBlockEntity) srv.overworld().getBlockEntity(new BlockPos(28, up, z + 3))).testFill(dev.wildcatter.fluid.Product.PRODUCED_WATER, 20_000);
				return null;
			});
			context.waitTicks(300);
			String jack = read(server, 20, up, z);
			check(jack.contains("pumping") && jack.contains("water cut 10%"), "the pumpjack should pump a new field's stream: " + jack + " | set " + read(server, 11, up, z)
					+ " | cable14 " + read(server, 14, up, z) + " | cable12 " + read(server, 12, up, z) + " | cable13s " + read(server, 13, up, z + 5)
					+ " | cable21 " + read(server, 21, up, z + 10) + " | pump " + read(server, 24, up, z + 10));
			String sep = read(server, 24, up, z);
			check(sep.contains("separating") && sep.contains("water cut 10%"), "the separator should split the stream, 10 % water: " + sep);
			String oilTank = read(server, 29, up, z - 5);
			check(oilTank.contains("Stock tank") && oilTank.contains("crude") && !oilTank.contains("empty"), "the stock tank should fill with crude: " + oilTank);
			String flare = read(server, 36, up + 5, z + 2);
			check(flare.contains("BURNING"), "the flare should burn the gas nothing takes: " + flare + " | " + sep);
			String pump = read(server, 24, up, z + 10);
			check(pump.contains("injecting") && pump.contains("running"), "the H-pump should inject the produced water: " + pump + " | " + read(server, 29, up, z + 4));
			float pumpRpm = context.computeOnClient(client -> client.level.getBlockEntity(new BlockPos(22, up, z + 10)) instanceof dev.wildcatter.block.InjectionPumpBlockEntity p ? p.spin().rpm() : -1F);
			double twoPole = 120.0 * dev.wildcatter.energy.ElectricalStandard.active().hertz() / 2 * (1 - Tuning.INJECTION_PUMP_SLIP);
			check(Math.abs(pumpRpm - twoPole) < 5, "the client should see the H-pump's coupling at about " + twoPole + " rpm: " + pumpRpm);
			// the gas genset runs on the separator's gas
			server.computeOnServer(srv -> {
				panel(srv, gasSet).setWantRun(true);
				return null;
			});
			context.waitTicks(260);
			String gasGen = read(server, 44, up, z - 4);
			check(gasGen.contains("RUNNING") && gasGen.contains("gas from a separator"), "a gas genset on separator gas should run: " + gasGen);
			String run = server.computeOnServer(srv -> dev.wildcatter.block.Lines.line(srv.overworld(), new BlockPos(30, up + 2, z)).run().describe());
			check(run.equals("carrying gas"), "the gas run should carry gas: " + run);
			server.runCommand("tp @p 18 " + (up + 7) + " " + (z + 16) + " 210 20");   // aimed just before the shot: the player falls between
			context.waitTicks(3);
			context.takeScreenshot("wildcatter-lease");
			// buckets: crude poured in, and taken back out
			BlockPos tankCell = new BlockPos(29, up + 2, z - 4);
			double before = tankAmount(server, tankCell);
			check(clickWith(server, tankCell, net.minecraft.core.Direction.SOUTH, new ItemStack(WildcatterItems.CRUDE_OIL_BUCKET)), "a crude bucket should pour into the stock tank");
			double poured = tankAmount(server, tankCell);
			check(clickWith(server, tankCell, net.minecraft.core.Direction.SOUTH, new ItemStack(net.minecraft.world.item.Items.BUCKET)), "an empty bucket should fill at the stock tank");
			double taken = tankAmount(server, tankCell);
			check(poured - before > 900 && poured - taken > 900, "a bucket is 1,000 L in and out: " + before + " -> " + poured + " -> " + taken);
			// a joint between the oil run and the water run blocks both
			put(server, 27, up, z - 1, "wildcatter:gas_line");
			context.waitTicks(2);
			String blocked = pipeReading(server, new BlockPos(27, up, z + 2));
			check(blocked.contains("BLOCKED") && blocked.contains("crude") && blocked.contains("produced water"), "oil meeting water should block the run: " + blocked);
			put(server, 27, up, z - 1, "minecraft:air");
			// no flare and no engine: the gas has nowhere to go and the separator shuts in; a full flowline shuts the pumpjack in
			server.computeOnServer(srv -> {
				srv.overworld().destroyBlock(new BlockPos(36, up, z + 2), false);
				panel(srv, gasSet).setWantRun(false);
				return null;
			});
			context.waitTicks(200);
			String shut = read(server, 24, up, z);
			check(shut.contains("SHUT IN: gas"), "with nowhere for its gas the separator should shut in: " + shut);
			server.computeOnServer(srv -> {
				jack(srv, new BlockPos(20, up, z)).testFill(Tuning.PUMPJACK_TANK);
				return null;
			});
			context.waitTicks(20);
			String jackShut = read(server, 20, up, z);
			check(jackShut.contains("SHUT IN"), "a pumpjack with its flowline backed up should stop: " + jackShut);
			System.out.println("[wildcatter-test] lease ok: " + jack + " | " + sep + " | " + oilTank + " | " + flare + " | " + pump + " | " + gasGen + " | " + blocked + " | " + shut);
		} finally {
			ReservoirData.testSeed = null;
		}
	}

	/**
	 * The gas plant (5b, docs/multiblocks/gas-plant.md): a gas well and separator; a 1 MW gas set on the separator's gas powers the
	 * plant (480 V to the refrigeration compressor, a backward unit substation to 4.16 kV for the gas compressor). The train runs west
	 * to east: compressor, dehydrator, chiller (the refrigeration compressor against its south side), the stabilizer, an LPG bullet
	 * and a stock tank of natural gasoline; a second gas set runs on the pipeline gas. Without refrigeration the chiller stops, and
	 * fed wet gas it reads HYDRATES.
	 */
	private void gasPlant(ClientGameTestContext context, TestServerContext server) {
		int z = -1080;
		server.runCommand("effect give @p minecraft:slow_falling 600 0 true");   // the camera stands high: no fall, no lost chunks
		server.runCommand("tp @p 28 " + (up + 14) + " " + (z + 20) + " 180 35");
		context.waitTicks(20);   // let the chunks load before building there
		server.runCommand("fill 4 " + (y - 1) + " " + (z - 12) + " 50 " + (y + 13) + " " + (z + 8) + " minecraft:air");
		server.runCommand("fill 4 " + (y - 1) + " " + (z - 12) + " 50 " + (y - 1) + " " + (z + 8) + " minecraft:stone");
		long seed = -1;
		for (long s = 1; s < 2_000_000 && seed < 0; s++) {
			Reservoir gas = Reservoir.at(s, 8, z);   // under the Christmas tree
			if (gas != null && !gas.deep() && gas.kind() == Reservoir.Kind.GAS && gas.units() > 100_000) {
				seed = s;
			}
		}
		check(seed >= 0, "no seed puts a gas field under the gas plant's well");
		ReservoirData.testSeed = seed;
		try {
			// the lease: the tree, a separator (x 10-13, z-1..z, inlet west)
			placeBig(server, WildcatterBlocks.GAS_WELLHEAD, new BlockPos(8, up, z), net.minecraft.core.Direction.NORTH);
			placeBig(server, WildcatterBlocks.SEPARATOR, new BlockPos(10, up, z), net.minecraft.core.Direction.WEST);
			// the plant, flowing east: compressor x 18-26, dehydrator x 28-32, chiller x 34-40 (all z-3..z, facing west, inlets west)
			placeBig(server, WildcatterBlocks.GAS_COMPRESSOR, new BlockPos(18, up, z), net.minecraft.core.Direction.WEST);
			placeBig(server, WildcatterBlocks.TEG_DEHYDRATOR, new BlockPos(28, up, z - 1), net.minecraft.core.Direction.WEST);
			placeBig(server, WildcatterBlocks.NGL_CHILLER, new BlockPos(34, up, z - 1), net.minecraft.core.Direction.WEST);
			BlockPos refrigeration = new BlockPos(34, up, z + 2);   // x 34-39, z..z+2: against the chiller's south side
			placeBig(server, WildcatterBlocks.REFRIGERATION_COMPRESSOR, refrigeration, net.minecraft.core.Direction.WEST);
			placeBig(server, WildcatterBlocks.STABILIZER, new BlockPos(43, up, z - 3), net.minecraft.core.Direction.NORTH);   // x 43-44, z-3..z-2
			placeBig(server, WildcatterBlocks.LPG_BULLET, new BlockPos(46, up, z - 8), net.minecraft.core.Direction.NORTH);   // x 46-48, z-8..z+3
			placeBig(server, WildcatterBlocks.STOCK_TANK, new BlockPos(42, up, z + 2), net.minecraft.core.Direction.NORTH);   // x 42-45, z+2..z+5
			// power: a gas set south of the separator; a unit substation off its panel to 4.16 kV, its second terminal at 480 V
			BlockPos power = new BlockPos(16, up, z + 3);
			genset(server, power, net.minecraft.core.Direction.NORTH, true);   // engine x 11-14, z+3..z+4
			inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, power, net.minecraft.core.Direction.EAST, false);   // x 17-20, z+2..z+3
			for (int[] p : new int[][] {{21, 3}, {21, 2}, {21, 1}, {22, 1}}) {
				put(server, p[0], up, z + p[1], "wildcatter:cable_mv");   // to the compressor motor's box, its south face at x 22
			}
			put(server, 17, up, z + 4, "wildcatter:cable_industrial");
			for (int x = 17; x <= 34; x++) {
				put(server, x, up, z + 5, "wildcatter:cable_industrial");
			}
			put(server, 34, up, z + 4, "wildcatter:cable_industrial");
			put(server, 34, up, z + 3, "wildcatter:cable_industrial");   // the refrigeration compressor's box, its main cell's south face
			server.computeOnServer(srv -> {   // 150 hp draws about 170 A: 4/0 (230 A), which carries its start too
				for (BlockPos at : BlockPos.betweenClosed(17, up, z + 3, 34, up, z + 5)) {
					if (srv.overworld().getBlockEntity(at) instanceof dev.wildcatter.block.MultiCableBlockEntity cable && cable.getBlockState().is(WildcatterBlocks.CABLE_INDUSTRIAL)) {
						cable.setSize(dev.wildcatter.energy.ConductorSize.AWG_4_0);
					}
				}
				return null;
			});
			// the pipes
			put(server, 9, up, z, "wildcatter:gas_line");   // the tree to the separator
			for (int x = 13; x <= 16; x++) {
				put(server, x, up + 2, z, "wildcatter:gas_line");   // separator gas off the top...
			}
			put(server, 16, up + 1, z, "wildcatter:gas_line");
			put(server, 16, up, z, "wildcatter:gas_line");
			put(server, 17, up, z, "wildcatter:gas_line");   // ...to the compressor's suction
			for (int[] p : new int[][] {{14, 2, 1}, {14, 2, 2}, {14, 1, 2}, {14, 0, 2}}) {
				put(server, p[0], up + p[1], z + p[2], "wildcatter:gas_line");   // and to the power set's engine
			}
			put(server, 27, up, z - 1, "wildcatter:gas_line");   // compressor to dehydrator
			put(server, 33, up, z - 1, "wildcatter:gas_line");   // dehydrator to chiller
			for (int[] p : new int[][] {{41, 0, -2}, {41, 0, -3}, {41, 0, -4}, {42, 0, -4}, {43, 0, -4}}) {
				put(server, p[0], up + p[1], z + p[2], "wildcatter:gas_line");   // Y-grade to the stabilizer
			}
			put(server, 45, up, z - 3, "wildcatter:gas_line");   // LPG to the bullet
			for (int zz = z - 1; zz <= z + 1; zz++) {
				put(server, 43, up, zz, "wildcatter:gas_line");   // natural gasoline to the stock tank
			}
			BlockPos sales = new BlockPos(40, up, z - 10);   // the second gas set, on pipeline gas: engine x 35-38, z-10..z-9
			genset(server, sales, net.minecraft.core.Direction.NORTH, true);
			put(server, 41, up + 1, z - 1, "wildcatter:gas_line");   // pipeline gas, out of the chiller's back upper layer
			for (int zz = z - 8; zz <= z - 1; zz++) {
				put(server, 41, up + 2, zz, "wildcatter:gas_line");
			}
			put(server, 41, up + 1, z - 8, "wildcatter:gas_line");
			for (int x = 37; x <= 41; x++) {
				put(server, x, up, z - 8, "wildcatter:gas_line");
			}
			placeBig(server, WildcatterBlocks.FLARE_STACK, new BlockPos(41, up, z - 9), net.minecraft.core.Direction.NORTH);   // the residue gas no one takes
			// 7g: a helium recovery unit on the pipeline gas, its front against the run at z-8; its own 480 V set west of it
			BlockPos helium = new BlockPos(37, up, z - 7);
			placeBig(server, WildcatterBlocks.HELIUM_RECOVERY_UNIT, helium, net.minecraft.core.Direction.NORTH);   // x 37-39, z-7..z-4
			put(server, 38, up + 6, z - 5, "minecraft:hopper");
			server.runCommand("item replace block 38 " + (up + 6) + " " + (z - 5) + " container.0 with wildcatter:empty_helium_cylinder 1");
			BlockPos heliumOut = new BlockPos(38, y, z - 5);
			put(server, 38, y, z - 5, "minecraft:hopper");
			BlockPos heliumSet = new BlockPos(34, up, z - 6);
			genset(server, heliumSet, net.minecraft.core.Direction.NORTH, false);                     // x 29-34, z-6..z-5
			for (int[] p : new int[][] {{35, -6}, {35, -7}, {36, -7}}) {
				put(server, p[0], up, z + p[1], "wildcatter:cable_industrial");
			}

			put(server, 14, up + 1, z - 1, "wildcatter:gas_line");   // on the separator's oil nozzle: a gas well's liquid is condensate
			exportBlueprint(server, "gas_plant", new BlockPos(4, y, z - 12), new BlockPos(50, y + 13, z + 8));   // the library (docs/blueprints.md)
			startGenset(context, server, power, net.minecraft.core.Direction.NORTH, 0);
			context.waitTicks(200);   // the plant fills with pipeline gas before the second set cranks on it
			String condensate = pipeReading(server, new BlockPos(14, up + 1, z - 1));
			check(condensate.contains("carrying condensate"), "a separator fed only by a gas well should give condensate: " + condensate);
			server.computeOnServer(srv -> {
				panel(srv, sales).setWantRun(true);
				return null;
			});
			context.waitTicks(400);
			String comp = read(server, 20, up, z);
			check(comp.contains("compressing") && comp.contains("running"), "the gas compressor should compress separator gas: " + comp + " | sep " + read(server, 11, up, z)
					+ " | set " + read(server, 16, up, z + 3) + " | dehy " + read(server, 30, up, z - 1) + " | chill " + read(server, 36, up, z - 1)
					+ " | refr " + read(server, 35, up, z + 1) + " | cable " + read(server, 34, up, z + 3) + " | stab " + read(server, 43, up, z - 3));
			float rpm = context.computeOnClient(client -> client.level.getBlockEntity(new BlockPos(18, up, z)) instanceof dev.wildcatter.block.GasCompressorBlockEntity c ? c.spin().rpm() : -1F);
			double sixPole = 120.0 * dev.wildcatter.energy.ElectricalStandard.active().hertz() / Tuning.GAS_COMPRESSOR_POLES * (1 - Tuning.GAS_COMPRESSOR_SLIP);
			check(Math.abs(rpm - sixPole) < 5, "the client should see the compressor's coupling at about " + sixPole + " rpm: " + rpm);
			String dehy = read(server, 30, up, z - 1);
			check(dehy.contains("drying"), "the dehydrator should dry the compressed gas: " + dehy);
			String chill = read(server, 36, up, z - 1);
			check(chill.contains("chilling"), "the chiller should make pipeline gas and NGL: " + chill + " | " + read(server, 35, up, z + 1));
			String stab = read(server, 43, up, z - 3);
			check(stab.contains("splitting"), "the stabilizer should split the NGL: " + stab);
			String bullet = read(server, 47, up, z - 3);
			check(bullet.contains("LPG bullet") && bullet.contains("LPG ") && !bullet.contains("empty"), "the bullet should fill with LPG: " + bullet);
			String tank = read(server, 43, up, z + 3);
			check(tank.contains("natural gasoline"), "the stock tank should fill with natural gasoline: " + tank);
			String salesSet = read(server, 40, up, z - 10);
			check(salesSet.contains("RUNNING") && salesSet.contains("pipeline gas"), "a gas set on the plant's pipeline gas should run: " + salesSet);
			// the helium recovery unit: on its own set, it treats the pipeline gas and fills a cylinder a hook has nearly filled
			startGenset(context, server, heliumSet, net.minecraft.core.Direction.NORTH, 2000);
			context.waitTicks(200);
			String he = read(server, 37, up, z - 7);
			check(he.contains("treating") && he.contains("m3/h of helium"), "the helium recovery unit should take helium out of the pipeline gas: " + he + " | set " + read(server, 34, up, z - 6));
			server.computeOnServer(srv -> {
				((dev.wildcatter.block.HeliumRecoveryBlockEntity) srv.overworld().getBlockEntity(helium)).setHeliumForTest(Tuning.HELIUM_CYLINDER_M3 - 0.002);
				return null;
			});
			int cylinders = 0;
			for (int i = 0; i < 20 && cylinders == 0; i++) {
				context.waitTicks(20);
				cylinders = countIn(server, heliumOut, WildcatterItems.HELIUM_CYLINDER);
			}
			check(cylinders > 0, "a full helium cylinder should drop into the hopper under the unit: " + read(server, 37, up, z - 7));
			check(read(server, 40, up, z - 10).contains("RUNNING"), "the gas set should still run on the treated gas: " + read(server, 40, up, z - 10));
			server.runCommand("tp @p 30 " + (up + 9) + " " + (z + 15) + " 200 22");   // aimed just before the shot: the player falls between
			context.waitTicks(3);
			context.takeScreenshot("wildcatter-gas-plant");
			// no refrigeration: the chiller stops
			server.computeOnServer(srv -> srv.overworld().destroyBlock(refrigeration, false));
			String warm = "";
			for (int waited = 0; waited < 200 && !warm.contains("NO REFRIGERATION"); waited += 20) {
				context.waitTicks(20);
				warm = read(server, 36, up, z - 1);
			}
			check(warm.contains("NO REFRIGERATION"), "without its refrigeration compressor the chiller should stop: " + warm);
			// the dehydrator bypassed: wet compressed gas reaches the chiller
			server.computeOnServer(srv -> srv.overworld().destroyBlock(new BlockPos(28, up, z - 1), false));
			for (int x = 28; x <= 32; x++) {
				put(server, x, up, z - 1, "wildcatter:gas_line");
			}
			String hydrates = "";
			for (int waited = 0; waited < 200 && !hydrates.contains("HYDRATES"); waited += 20) {
				context.waitTicks(20);
				hydrates = read(server, 36, up, z - 1);
			}
			check(hydrates.contains("HYDRATES"), "wet gas in the chiller should freeze: " + hydrates);
			System.out.println("[wildcatter-test] gas plant ok: " + comp + " | " + dehy + " | " + chill + " | " + stab + " | " + bullet + " | " + tank + " | " + salesSet + " | " + hydrates);
		} finally {
			ReservoirData.testSeed = null;
		}
	}

	/** Puts down a run of steel pipe through the listed cells (x, y offset from up, z offset from {@code z}). */
	private void pipes(TestServerContext server, int z, int[][] cells) {
		for (int[] c : cells) {
			put(server, c[0], up + c[1], z + c[2], "wildcatter:gas_line");
		}
	}

	private void fillTank(TestServerContext server, BlockPos tank, dev.wildcatter.fluid.Product product, double litres) {
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.StorageTankBlockEntity) srv.overworld().getBlockEntity(tank)).testFill(product, litres);
			return null;
		});
	}

	/** Sets every cable of one kind in a box to a size. */
	private void sizeCables(TestServerContext server, BlockPos from, BlockPos to, net.minecraft.world.level.block.Block kind, dev.wildcatter.energy.ConductorSize size) {
		server.computeOnServer(srv -> {
			for (BlockPos at : BlockPos.betweenClosed(from, to)) {
				if (srv.overworld().getBlockEntity(at) instanceof dev.wildcatter.block.MultiCableBlockEntity cable && cable.getBlockState().is(kind)) {
					cable.setSize(size);
				}
			}
			return null;
		});
	}

	/**
	 * The early crude unit (5c, docs/multiblocks/crude-units.md): a 1,000 bbl/day topping unit fed crude from a stock tank, its
	 * skid run straight off a diesel set's terminals, its heater on its own LPG. Gasoline, kerosene, diesel and residue each fill
	 * their own stock tank.
	 */
	private void toppingUnit(ClientGameTestContext context, TestServerContext server) {
		int z = -1140;
		server.runCommand("effect give @p minecraft:slow_falling 600 0 true");
		server.runCommand("tp @p 14 " + (up + 10) + " " + (z + 24) + " 180 30");
		context.waitTicks(20);
		server.runCommand("fill 2 " + (y - 1) + " " + (z - 8) + " 26 " + (y + 13) + " " + (z + 16) + " minecraft:air");
		server.runCommand("fill 2 " + (y - 1) + " " + (z - 8) + " 26 " + (y - 1) + " " + (z + 16) + " minecraft:stone");
		BlockPos crude = new BlockPos(12, up, z - 5);
		placeBig(server, WildcatterBlocks.STOCK_TANK, crude, net.minecraft.core.Direction.NORTH);                       // x 12-15, z-5..z-2
		placeBig(server, WildcatterBlocks.TOPPING_SKID, new BlockPos(12, up, z + 1), net.minecraft.core.Direction.NORTH);   // x 12-14, z+1..z+4
		BlockPos column = new BlockPos(14, up, z + 5);
		placeBig(server, WildcatterBlocks.TOPPING_COLUMN, column, net.minecraft.core.Direction.NORTH);                  // x 14-15, z+5..z+6
		context.waitTicks(10);
		String unbuilt = read(server, 14, up, z + 5);
		check(unbuilt.contains("NOT BUILT") && unbuilt.contains("TOPPING HEATER"), "a column with no heater should say so: " + unbuilt);
		placeBig(server, WildcatterBlocks.TOPPING_HEATER, new BlockPos(16, up, z + 6), net.minecraft.core.Direction.NORTH); // x 16-17, z+6..z+7
		BlockPos gasolineTank = new BlockPos(6, up, z + 5), keroseneTank = new BlockPos(19, up, z + 2);
		BlockPos dieselTank = new BlockPos(14, up, z + 10), residueTank = new BlockPos(10, up, z + 10);
		for (BlockPos t : new BlockPos[] {gasolineTank, keroseneTank, dieselTank, residueTank}) {
			placeBig(server, WildcatterBlocks.STOCK_TANK, t, net.minecraft.core.Direction.NORTH);
		}
		pipes(server, z, new int[][] {{13, 0, -1}, {13, 0, 0}});                                   // crude to the skid's suction
		pipes(server, z, new int[][] {{12, 0, 5}, {11, 0, 5}, {10, 0, 5}});                        // gasoline off the skid
		pipes(server, z, new int[][] {{16, 0, 5}, {17, 0, 5}, {18, 0, 5}});                        // kerosene off the column's east
		pipes(server, z, new int[][] {{15, 0, 7}, {15, 0, 8}, {15, 0, 9}});                        // diesel off its south
		pipes(server, z, new int[][] {{13, 0, 6}, {13, 0, 7}, {13, 0, 8}, {13, 0, 9}});            // residue off its west
		BlockPos set = new BlockPos(11, up, z + 1);   // its terminals face the skid's terminal box, face to face
		genset(server, set, net.minecraft.core.Direction.NORTH, false);
		fillTank(server, crude, dev.wildcatter.fluid.Product.CRUDE, 30_000);
		startGenset(context, server, set, net.minecraft.core.Direction.NORTH, 1000);
		context.waitTicks(500);
		String unit = read(server, 14, up, z + 5);
		check(unit.contains("refining 1.8") && unit.contains("crude"), "the topping unit should refine about 1.8 L/s of crude: " + unit + " | skid "
				+ read(server, 12, up, z + 1) + " | heater " + read(server, 16, up, z + 6));
		String heater = read(server, 16, up, z + 6);
		check(heater.contains("firing") && heater.contains("its own LPG"), "the heater should fire on the unit's own LPG: " + heater);
		String[] names = {"gasoline", "kerosene", "diesel", "residue"};
		BlockPos[] tanks = {gasolineTank, keroseneTank, dieselTank, residueTank};
		StringBuilder all = new StringBuilder();
		for (int i = 0; i < 4; i++) {
			String t = read(server, tanks[i].getX(), tanks[i].getY(), tanks[i].getZ());
			check(t.contains(names[i]) && !t.contains("empty"), "the " + names[i] + " tank should fill: " + t);
			all.append(" | ").append(t);
		}
		server.runCommand("tp @p 14 " + (up + 8) + " " + (z + 20) + " 180 28");
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-topping-unit");
		System.out.println("[wildcatter-test] topping unit ok: " + unit + " | " + heater + all);
		// 13g (docs/multiblocks/plc.md): the same column enabled by a real program; off, it holds; a cut cable holds it too
		BlockPos bench = plcBench(context, server, z + 15, dev.wildcatter.control.PlcModuleItem.Type.DI16, dev.wildcatter.control.PlcModuleItem.Type.DO16, dev.wildcatter.control.PlcModuleItem.Type.AI8);
		controlRun(server, "wildcatter:junction_box", new int[] {13, up + 1, z + 5});
		controlRun(server, "wildcatter:junction_box", new int[] {12, up + 1, z + 5});   // the skid is beside it, and is a device too
		controlRun(server, "wildcatter:control_cable", straight(11, up + 1, z + 5, 11, up + 7, z + 5));
		controlRun(server, "wildcatter:control_cable", straight(11, up + 7, z + 6, 11, up + 7, z + 15));
		controlRun(server, "wildcatter:control_cable", straight(12, up + 7, z + 15, 13, up + 7, z + 15));
		controlRun(server, "wildcatter:control_cable", new int[] {14, up + 7, z + 15});
		controlRun(server, "wildcatter:control_cable", straight(14, up + 6, z + 15, 14, up + 2, z + 15));
		controlRun(server, "wildcatter:junction_box", new int[] {14, up + 1, z + 15});   // beside the cabinet's top, where a cable cell would touch three things
		context.waitTicks(25);
		java.util.function.Function<net.minecraft.server.MinecraftServer, dev.wildcatter.control.PlcCabinetBlockEntity> plc = srv -> (dev.wildcatter.control.PlcCabinetBlockEntity) srv.overworld().getBlockEntity(bench);
		String landed = server.computeOnServer(srv -> {
			var box = plc.apply(srv);
			String out = String.valueOf(box.land(column, 0, "%QX16.0")) + box.land(column, 1, "%IX0.0") + box.land(column, 2, "%IX0.1") + box.land(column, 3, "%IW32");
			dev.wildcatter.control.ControlNet.deviceAt(srv.overworld(), column).cycleHoa(column);
			dev.wildcatter.control.ControlNet.deviceAt(srv.overworld(), column).cycleHoa(column);
			box.download("PROGRAM UNIT\nVAR\n go : BOOL := TRUE;\n enable AT %QX16.0 : BOOL;\n auto AT %IX0.0 : BOOL;\n held AT %IX0.1 : BOOL;\n level AT %IW32 : WORD;\nEND_VAR\nenable := go;\nEND_PROGRAM");
			box.cpu().setMode(dev.wildcatter.plc.Cpu.Mode.RUN);
			return out;
		});
		check(landed.equals("nullnullnullnull"), "the column's signals should land: " + landed);
		context.waitTicks(60);
		String enabled = read(server, 14, up, z + 5);
		check(enabled.contains("refining"), "enabled by its PLC it should refine: " + enabled + " | " + read(server, 15, up, z + 15));
		server.runOnServer(srv -> plc.apply(srv).cpu().write("UNIT.GO", false));
		context.waitTicks(30);
		String heldBy = read(server, 14, up, z + 5);
		check(heldBy.contains("HELD"), "with its enable off it should hold, and say so: " + heldBy);
		check(Boolean.TRUE.equals(server.computeOnServer(srv -> plc.apply(srv).cpu().read("UNIT.HELD"))), "and tell its PLC");
		server.runOnServer(srv -> plc.apply(srv).cpu().write("UNIT.GO", true));
		context.waitTicks(30);
		check(read(server, 14, up, z + 5).contains("refining"), "enabled again it should go on: " + read(server, 14, up, z + 5));
		put(server, 11, up + 7, z + 10, "minecraft:air");
		context.waitTicks(40);
		check(read(server, 14, up, z + 5).contains("HELD"), "a cut cable should hold it: " + read(server, 14, up, z + 5));
		System.out.println("[wildcatter-test] topping unit under a PLC ok: " + heldBy);
	}

	/**
	 * The late crude unit (5c): a 20,000 bbl/day unit built part by part, the column naming what is missing; a 500 kW diesel set's
	 * 480 V to the desalter and the condenser's fans and, through a backward unit substation, 4.16 kV to the charge pump; the heater on
	 * residue from a tank. It refines at full rate; taking the condenser away stops it.
	 */
	private void crudeUnit(ClientGameTestContext context, TestServerContext server) {
		int z = -1200;
		server.runCommand("effect give @p minecraft:slow_falling 600 0 true");
		server.runCommand("tp @p 16 " + (up + 14) + " " + (z + 34) + " 180 25");
		context.waitTicks(20);
		server.runCommand("fill 2 " + (y - 1) + " " + (z - 10) + " 34 " + (y + 14) + " " + (z + 26) + " minecraft:air");
		server.runCommand("fill 2 " + (y + 15) + " " + (z - 10) + " 34 " + (y + 32) + " " + (z + 26) + " minecraft:air");
		server.runCommand("fill 2 " + (y - 1) + " " + (z - 10) + " 34 " + (y - 1) + " " + (z + 26) + " minecraft:stone");
		placeBig(server, WildcatterBlocks.ATMOSPHERIC_COLUMN, new BlockPos(20, up, z), net.minecraft.core.Direction.NORTH);   // x 20-22, z..z+2
		context.waitTicks(10);
		String unbuilt = read(server, 20, up, z);
		check(unbuilt.contains("NOT BUILT") && unbuilt.contains("CRUDE HEATER") && unbuilt.contains("OVERHEAD CONDENSER"), "a lone column should name its parts: " + unbuilt);
		placeBig(server, WildcatterBlocks.CRUDE_HEATER, new BlockPos(23, up, z), net.minecraft.core.Direction.NORTH);         // x 23-28, z..z+7
		placeBig(server, WildcatterBlocks.SIDE_STRIPPERS, new BlockPos(20, up, z - 1), net.minecraft.core.Direction.NORTH);   // x 20-21, z-1
		placeBig(server, WildcatterBlocks.OVERHEAD_CONDENSER, new BlockPos(16, up, z), net.minecraft.core.Direction.NORTH);   // x 16-19, z..z+7
		context.waitTicks(25);
		String half = read(server, 20, up, z);
		check(half.contains("NOT BUILT") && half.contains("CHARGE PUMP") && !half.contains("CRUDE HEATER"), "with the heater in it should ask for the charge pump: " + half);
		placeBig(server, WildcatterBlocks.CRUDE_CHARGE_PUMP, new BlockPos(24, up, z + 8), net.minecraft.core.Direction.NORTH);  // x 24-25, z+8..z+12
		placeBig(server, WildcatterBlocks.DESALTER, new BlockPos(28, up, z + 18), net.minecraft.core.Direction.SOUTH);         // x 26-28, z+9..z+18
		BlockPos crude = new BlockPos(26, up, z + 20), fuel = new BlockPos(24, up, z - 6);
		placeBig(server, WildcatterBlocks.STOCK_TANK, crude, net.minecraft.core.Direction.NORTH);   // x 26-29, z+20..z+23
		placeBig(server, WildcatterBlocks.STOCK_TANK, fuel, net.minecraft.core.Direction.NORTH);    // x 24-27, z-6..z-3
		pipes(server, z, new int[][] {{27, 0, 19}});                    // crude to the desalter's inlet (its south end, facing south)
		pipes(server, z, new int[][] {{24, 0, -2}, {24, 0, -1}});       // residue fuel to the heater's front
		// power: a diesel set; 4.16 kV off a backward unit substation to the pump, 480 V off its second terminal to the rest
		BlockPos set = new BlockPos(10, up, z + 12);
		genset(server, set, net.minecraft.core.Direction.NORTH, false);   // x 5-10, z+12..z+13
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, set, net.minecraft.core.Direction.EAST, false);   // x 11-14, z+11..z+12
		for (int x = 15; x <= 22; x++) {
			put(server, x, up, z + 12, "wildcatter:cable_mv");
		}
		for (int zz = z + 8; zz <= z + 11; zz++) {
			put(server, 22, up, zz, "wildcatter:cable_mv");
		}
		put(server, 23, up, z + 8, "wildcatter:cable_mv");   // the charge pump's box, the west face of its main cell
		put(server, 11, up, z + 13, "wildcatter:cable_industrial");
		for (int x = 4; x <= 11; x++) {
			put(server, x, up, z + 14, "wildcatter:cable_industrial");
		}
		for (int zz = z + 15; zz <= z + 24; zz++) {
			put(server, 11, up, zz, "wildcatter:cable_industrial");   // south, round the crude tank...
		}
		for (int x = 12; x <= 30; x++) {
			put(server, x, up, z + 24, "wildcatter:cable_industrial");
		}
		for (int zz = z + 18; zz <= z + 23; zz++) {
			put(server, 30, up, zz, "wildcatter:cable_industrial");
		}
		put(server, 29, up, z + 18, "wildcatter:cable_industrial");   // ...to the desalter's box, the east face of its main cell
		for (int zz = z; zz <= z + 13; zz++) {
			put(server, 4, up, zz, "wildcatter:cable_industrial");
		}
		for (int x = 5; x <= 15; x++) {
			put(server, x, up, z, "wildcatter:cable_industrial");   // to the condenser's box, the west face of its main cell
		}
		sizeCables(server, new BlockPos(4, up, z), new BlockPos(30, up, z + 24), WildcatterBlocks.CABLE_INDUSTRIAL, dev.wildcatter.energy.ConductorSize.AWG_4_0);
		// a second set beside the first, its terminals on the same 480 V cable: it synchronizes on (the charge pump starts at six
		// times its 187 kW, more than one 500 kW set carries)
		BlockPos set2 = new BlockPos(10, up, z + 16);
		genset(server, set2, net.minecraft.core.Direction.NORTH, false);   // x 5-10, z+16..z+17, its terminals against the cable at x 11
		sizeCables(server, new BlockPos(4, up, z), new BlockPos(30, up, z + 24), WildcatterBlocks.CABLE_INDUSTRIAL, dev.wildcatter.energy.ConductorSize.AWG_4_0);
		exportBlueprint(server, "crude_unit", new BlockPos(2, y, z - 10), new BlockPos(34, y + 14, z + 26));   // the library (docs/blueprints.md)
		fillTank(server, fuel, dev.wildcatter.fluid.Product.RESIDUE, 30_000);
		startGenset(context, server, set, net.minecraft.core.Direction.NORTH, 2000);
		startGenset(context, server, set2, net.minecraft.core.Direction.NORTH, 2000);
		fillTank(server, crude, dev.wildcatter.fluid.Product.CRUDE, 60_000);
		context.waitTicks(240);
		String unit = read(server, 20, up, z);
		check(unit.contains("refining 36") || unit.contains("refining 37") || unit.contains("refining 35"), "the crude unit should refine about 36.8 L/s: " + unit
				+ " | pump " + read(server, 24, up, z + 8) + " | desalter " + read(server, 27, up, z + 15) + " | condenser " + read(server, 17, up, z + 2)
				+ " | set " + read(server, 10, up, z + 12));
		String heater = read(server, 25, up, z + 3);
		check(heater.contains("firing") && heater.contains("residue"), "the heater should fire on residue: " + heater);
		float pumpRpm = context.computeOnClient(client -> client.level.getBlockEntity(new BlockPos(24, up, z + 8)) instanceof dev.wildcatter.block.UnitMotorBlockEntity p ? p.spin().rpm() : -1F);
		check(pumpRpm > 3000, "the client should see the charge pump turning at about 3,500 rpm: " + pumpRpm);
		String strippers = read(server, 20, up, z - 1);
		check(strippers.matches(".*kerosene [1-9].*") && strippers.matches(".*diesel [1-9].*"), "the strippers should hold kerosene and diesel: " + strippers);
		server.runCommand("tp @p 12 " + (up + 18) + " " + (z + 30) + " 205 25");
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-crude-unit");
		server.computeOnServer(srv -> srv.overworld().destroyBlock(new BlockPos(16, up, z), false));
		context.waitTicks(30);
		String broken = read(server, 20, up, z);
		check(broken.contains("NOT BUILT") && broken.contains("OVERHEAD CONDENSER"), "without its condenser the unit should stop: " + broken);
		System.out.println("[wildcatter-test] crude unit ok: " + unit + " | " + heater + " | " + strippers + " | " + broken);
	}

	/**
	 * The cracker and polyethylene (5d, docs/multiblocks/cracker.md), west to east: an LPG bullet feeds the furnace (and fires it at
	 * first), then the quench tower, the cracked gas compressor, the cold box with its refrigeration compressor, the polyethylene
	 * reactor and its pelletizer. The cold box's LPG goes back to the bullet and its fuel gas back to the furnace. Plastic sheets
	 * fill the pelletizer's bin and a hopper under it takes them.
	 */
	private void cracker(ClientGameTestContext context, TestServerContext server) {
		int z = -1260;
		server.runCommand("effect give @p minecraft:slow_falling 600 0 true");
		server.runCommand("tp @p 22 " + (up + 12) + " " + (z + 22) + " 180 30");
		context.waitTicks(20);
		server.runCommand("fill 0 " + (y - 1) + " " + (z - 10) + " 46 " + (y + 16) + " " + (z + 10) + " minecraft:air");
		server.runCommand("fill 0 " + (y - 1) + " " + (z - 10) + " 46 " + (y - 1) + " " + (z + 10) + " minecraft:stone");
		net.minecraft.core.Direction west = net.minecraft.core.Direction.WEST;
		BlockPos bullet = new BlockPos(2, up, z - 6);
		placeBig(server, WildcatterBlocks.LPG_BULLET, bullet, west);                                           // x 2-13, z-8..z-6
		BlockPos furnace = new BlockPos(6, up, z);
		placeBig(server, WildcatterBlocks.CRACKING_FURNACE, furnace, west);                                    // x 6-11, z-3..z
		placeBig(server, WildcatterBlocks.QUENCH_TOWER, new BlockPos(13, up, z), west);                        // x 13-14, z-1..z
		placeBig(server, WildcatterBlocks.CRACKED_GAS_COMPRESSOR, new BlockPos(16, up, z), west);              // x 16-23, z-3..z
		placeBig(server, WildcatterBlocks.COLD_BOX, new BlockPos(25, up, z), west);                            // x 25-27, z-2..z
		BlockPos refrigeration = new BlockPos(25, up, z + 3);
		placeBig(server, WildcatterBlocks.REFRIGERATION_COMPRESSOR, refrigeration, west);                      // x 25-30, z+1..z+3
		placeBig(server, WildcatterBlocks.POLYETHYLENE_REACTOR, new BlockPos(31, up, z), west);                // x 31-33, z-2..z
		BlockPos pelletizer = new BlockPos(34, up, z);
		placeBig(server, WildcatterBlocks.PELLETIZER, pelletizer, west);                                       // x 34-39, z-1..z
		put(server, 34, y, z, "minecraft:hopper");   // under the pelletizer's main cell, its bin
		// the pipes
		pipes(server, z, new int[][] {{2, 0, -5}, {3, 0, -5}, {4, 0, -5}, {5, 0, -5}, {5, 0, -4}, {5, 0, -3}, {6, 0, -4}});   // LPG: feed, and fuel at first
		pipes(server, z, new int[][] {{12, 0, 0}});                                                            // furnace to quench
		pipes(server, z, new int[][] {{15, 0, 0}});                                                            // quench to compressor
		pipes(server, z, new int[][] {{24, 0, 0}});                                                            // compressor to cold box
		pipes(server, z, new int[][] {{28, 0, -2}, {28, 0, -3}, {28, 0, -4}, {28, 0, -5}, {28, 0, -6}});      // the cold box's LPG back to the bullet...
		for (int x = 14; x <= 27; x++) {
			put(server, x, up, z - 6, "wildcatter:gas_line");                                                  // ...into its east end
		}
		pipes(server, z, new int[][] {{28, 1, -1}, {29, 1, -1}, {30, 1, -1}, {30, 0, -1}});                    // ethylene to the reactor
		pipes(server, z, new int[][] {{28, 2, 0}, {28, 3, 0}, {28, 3, -1}, {28, 3, -2}, {28, 3, -3}, {28, 3, -4}});   // fuel gas back over...
		for (int x = 10; x <= 27; x++) {
			put(server, x, up + 3, z - 4, "wildcatter:gas_line");
		}
		pipes(server, z, new int[][] {{10, 2, -4}, {10, 1, -4}, {10, 0, -4}});                                 // ...to the furnace's north side
		// power: two diesel sets. One through a backward unit substation to 4.16 kV for the compressor and the pelletizer...
		BlockPos mvSet = new BlockPos(18, up, z + 6);
		genset(server, mvSet, net.minecraft.core.Direction.NORTH, false);                                      // x 13-18, z+6..z+7
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, mvSet, net.minecraft.core.Direction.EAST, false);   // x 19-22, z+5..z+6
		for (int zz = z + 1; zz <= z + 6; zz++) {
			put(server, 23, up, zz, "wildcatter:cable_mv");   // from the substation's 4.16 kV end, in line with the set's panel
		}
		for (int x = 20; x <= 22; x++) {
			put(server, x, up, z + 1, "wildcatter:cable_mv");   // the compressor's box, south of its motor
		}
		for (int x = 23; x <= 34; x++) {
			put(server, x, up, z + 5, "wildcatter:cable_mv");
		}
		for (int zz = z + 1; zz <= z + 4; zz++) {
			put(server, 34, up, zz, "wildcatter:cable_mv");   // the pelletizer's box, south of its main cell
		}
		// ...and one at 480 V for the refrigeration compressor and the reactor's cycle gas compressor, its line overhead at the crossing
		BlockPos lvSet = new BlockPos(40, up, z + 6);
		genset(server, lvSet, net.minecraft.core.Direction.NORTH, false);                                      // x 35-40, z+6..z+7
		put(server, 41, up, z + 6, "wildcatter:cable_industrial");
		put(server, 41, up, z + 5, "wildcatter:cable_industrial");
		put(server, 41, up, z + 4, "wildcatter:cable_industrial");
		for (int x = 25; x <= 41; x++) {
			put(server, x, up + 1, z + 4, "wildcatter:cable_industrial");
		}
		put(server, 25, up, z + 4, "wildcatter:cable_industrial");   // the refrigeration compressor's box
		for (int zz = z + 1; zz <= z + 4; zz++) {
			put(server, 31, up, zz, "wildcatter:cable_industrial");   // the reactor's box
		}
		sizeCables(server, new BlockPos(25, up, z + 1), new BlockPos(41, up + 1, z + 6), WildcatterBlocks.CABLE_INDUSTRIAL, dev.wildcatter.energy.ConductorSize.AWG_4_0);
		exportBlueprint(server, "cracker", new BlockPos(0, y, z - 10), new BlockPos(46, y + 16, z + 10));   // the library (docs/blueprints.md)
		fillTank(server, bullet, dev.wildcatter.fluid.Product.LPG, 20_000);
		server.computeOnServer(srv -> {   // a day's boiler feed water in the furnace (a pump on water pipe tops it up in play)
			((dev.wildcatter.block.CrackingFurnaceBlockEntity) srv.overworld().getBlockEntity(furnace)).addWater(200);
			return null;
		});
		startGenset(context, server, mvSet, net.minecraft.core.Direction.NORTH, 2000);
		startGenset(context, server, lvSet, net.minecraft.core.Direction.NORTH, 2000);
		context.waitTicks(600);
		String f = read(server, 6, up, z);
		String chain = " | quench " + read(server, 13, up, z) + " | compressor " + read(server, 18, up, z) + " | cold box " + read(server, 25, up, z)
				+ " | refrigeration " + read(server, 26, up, z + 2) + " | reactor " + read(server, 31, up, z) + " | pelletizer " + read(server, 34, up, z)
				+ " | mv set " + read(server, 18, up, z + 6) + " | lv set " + read(server, 40, up, z + 6);
		check(f.contains("cracking") && f.contains("LPG"), "the furnace should crack LPG: " + f + chain);
		String quench = read(server, 13, up, z);
		check(quench.contains("quenching"), "the quench tower should cool the cracked gas: " + quench);
		String cgc = read(server, 18, up, z);
		check(cgc.contains("compressing") || cgc.contains("UNLOADED"), "the cracked gas compressor should run: " + cgc);
		String cold = read(server, 25, up, z);
		check(cold.contains("separating") || cold.contains("idle"), "the cold box should run: " + cold + " | " + read(server, 26, up, z + 2));
		String reactor = read(server, 31, up, z);
		String pel = read(server, 34, up, z);
		int sheetsInHopper = server.computeOnServer(srv -> srv.overworld().getBlockEntity(new BlockPos(34, y, z)) instanceof net.minecraft.world.level.block.entity.HopperBlockEntity h
				? java.util.stream.IntStream.range(0, h.getContainerSize()).map(i -> h.getItem(i).is(WildcatterItems.PLASTIC_SHEET) ? h.getItem(i).getCount() : 0).sum() : -1);
		check(sheetsInHopper > 0 || pel.matches(".*: [1-9]\\d*/256.*"), "the pelletizer should make plastic sheets: " + pel + " | reactor " + reactor + " | hopper " + sheetsInHopper
				+ " | cold box " + cold + " | compressor " + cgc + " | quench " + quench + " | furnace " + f);
		check(sheetsInHopper > 0, "a hopper under the pelletizer should take its sheets: " + sheetsInHopper + " | " + pel);
		server.runCommand("tp @p 20 " + (up + 10) + " " + (z + 18) + " 200 25");
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-cracker");
		System.out.println("[wildcatter-test] cracker ok: " + f + " | " + quench + " | " + cgc + " | " + cold + " | " + reactor + " | " + pel + " | hopper " + sheetsInHopper);
	}

	/**
	 * The 16 MW gas turbine in combined cycle (6a, docs/multiblocks/gas-turbine.md, combined-cycle.md). A diesel set and a backward
	 * unit substation give the starter 4.16 kV; kerosene from a stock tank. With no 4.16 kV it will not start; with it, it purges and
	 * (skipped by a hook) runs up, closes its generator breaker onto the dead 13.8 kV bus duct and energizes the GSU's 138 kV side.
	 * A load bank takes 10 MW; the HRSG (warmed by a hook) raises steam, and the 5 MW steam turbine synchronizes and takes its share
	 * with its condenser fans and feed pump running.
	 */
	private void gasTurbine(ClientGameTestContext context, TestServerContext server) {
		int z = -1320;
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand("tp @p 24 " + (up + 16) + " " + (z + 34) + " 180 30");
		context.waitTicks(20);
		server.runCommand("fill 2 " + (y - 1) + " " + (z - 8) + " 36 " + (y + 12) + " " + (z + 27) + " minecraft:air");
		server.runCommand("fill 2 " + (y - 1) + " " + (z - 8) + " 36 " + (y - 1) + " " + (z + 27) + " minecraft:stone");
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		BlockPos gt = new BlockPos(20, up, z);
		placeBig(server, WildcatterBlocks.GAS_TURBINE, gt, north);                              // x 20-23, z..z+9
		context.waitTicks(25);
		String unbuilt = read(server, 20, up, z);
		check(unbuilt.contains("NOT BUILT") && unbuilt.contains("INLET HOUSE") && unbuilt.contains("GENERATOR"), "a lone turbine should name its parts: " + unbuilt);
		placeBig(server, WildcatterBlocks.GT_INLET_HOUSE, new BlockPos(20, up, z - 4), north);       // x 20-23, z-4..z-1
		placeBig(server, WildcatterBlocks.GT_GENERATOR, new BlockPos(24, up, z + 1), north);         // x 24-27, z+1..z+6
		placeBig(server, WildcatterBlocks.GEN_BREAKER, new BlockPos(25, up, z + 7), north);          // x 25-26, z+7..z+8
		BlockPos hrsg = new BlockPos(20, up, z + 10);
		placeBig(server, WildcatterBlocks.HRSG, hrsg, north);                                        // x 20-24, z+10..z+21
		placeBig(server, WildcatterBlocks.STEAM_TURBINE_5, new BlockPos(17, up, z + 15), north);     // x 17-19, z+15..z+20
		placeBig(server, WildcatterBlocks.ST_GENERATOR, new BlockPos(14, up, z + 15), north);        // x 14-16, z+15..z+18
		placeBig(server, WildcatterBlocks.GEN_BREAKER, new BlockPos(14, up, z + 19), north);         // x 14-15, z+19..z+20
		placeBig(server, WildcatterBlocks.AIR_COOLED_CONDENSER, new BlockPos(12, up, z + 9), north); // x 12-17, z+9..z+14
		placeBig(server, WildcatterBlocks.HRSG_FEED_PUMP, new BlockPos(21, up, z + 22), north);      // x 21-22, z+22..z+25
		placeBig(server, WildcatterBlocks.GSU_25, new BlockPos(31, up, z + 7), north);               // x 29-33, z+4..z+7
		placeBig(server, WildcatterBlocks.LOAD_BANK, new BlockPos(31, up, z + 10), north);           // x 31-33, z+10..z+15
		placeBig(server, WildcatterBlocks.LOAD_BANK, new BlockPos(28, up, z + 10), north);           // a second, x 28-30: 20 MW for full load
		BlockPos fuelTank = new BlockPos(14, up, z + 4);
		placeBig(server, WildcatterBlocks.STOCK_TANK, fuelTank, north);                              // x 14-17, z+4..z+7
		pipes(server, z, new int[][] {{18, 0, 6}, {19, 0, 6}});                                     // kerosene to the fuel nozzle
		// the 13.8 kV bus: the gas turbine's breaker, the GSU, the load bank; the steam turbine's breaker over the top to it
		for (int x = 25; x <= 33; x++) {
			put(server, x, up, z + 9, "wildcatter:bus_duct_gen");
		}
		put(server, 31, up, z + 8, "wildcatter:bus_duct_gen");   // up to the GSU's 13.8 kV throat
		for (int h = 0; h <= 5; h++) {
			put(server, 14, up + h, z + 21, "wildcatter:bus_duct_gen");
		}
		for (int x = 14; x <= 34; x++) {
			put(server, x, up + 5, z + 22, "wildcatter:bus_duct_gen");
		}
		for (int h = 0; h <= 4; h++) {
			put(server, 34, up + h, z + 22, "wildcatter:bus_duct_gen");
		}
		for (int zz = z + 9; zz <= z + 21; zz++) {
			put(server, 34, up, zz, "wildcatter:bus_duct_gen");
		}
		put(server, 31, up + 4, z + 3, "wildcatter:cable_hv");   // on a 138 kV bushing, to read the line side
		// station power: a diesel set; 4.16 kV through a backward unit substation to the starter, 480 V to the fans and the feed pump
		BlockPos set = new BlockPos(10, up, z + 2);
		genset(server, set, north, false);                                                        // x 5-10, z+2..z+3
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, set, net.minecraft.core.Direction.EAST, false);   // x 11-14, z+1..z+2
		for (int x = 15; x <= 19; x++) {
			put(server, x, up, z + 2, "wildcatter:cable_mv");   // to the starter's box, the west face of the turbine at z+2
		}
		for (int zz = z + 3; zz <= z + 22; zz++) {
			put(server, 11, up, zz, "wildcatter:cable_industrial");
		}
		put(server, 12, up, z + 3, "wildcatter:cable_industrial");
		for (int x = 12; x <= 20; x++) {
			put(server, x, up, z + 22, "wildcatter:cable_industrial");   // to the feed pump's box, the west face of its main cell
		}
		sizeCables(server, new BlockPos(11, up, z + 3), new BlockPos(20, up, z + 22), WildcatterBlocks.CABLE_INDUSTRIAL, dev.wildcatter.energy.ConductorSize.AWG_4_0);
		exportBlueprint(server, "combined_cycle", new BlockPos(2, y, z - 8), new BlockPos(36, y + 12, z + 27));   // the library (docs/blueprints.md)
		fillTank(server, fuelTank, dev.wildcatter.fluid.Product.KEROSENE, 40_000);
		context.waitTicks(30);
		// no 4.16 kV yet: the starter cannot turn it
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.GasTurbineBlockEntity) srv.overworld().getBlockEntity(gt)).setWantRun(true);
			return null;
		});
		context.waitTicks(120);
		String dead = read(server, 20, up, z);
		check(dead.contains("STARTER: no 4.16 kV"), "with no 4.16 kV the start should fail: " + dead);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.GasTurbineBlockEntity) srv.overworld().getBlockEntity(gt)).pushButton();   // reset
			return null;
		});
		startGenset(context, server, set, north, 2000);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.GasTurbineBlockEntity) srv.overworld().getBlockEntity(gt)).setWantRun(true);
			return null;
		});
		context.waitTicks(200);
		String purge = read(server, 20, up, z);
		check(purge.contains("PURGE") && purge.contains("25% speed"), "the starter should purge it at a quarter speed: " + purge + " | set " + read(server, 10, up, z + 2));
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.GasTurbineBlockEntity) srv.overworld().getBlockEntity(gt)).skipStartForTest();
			return null;
		});
		context.waitTicks(100);
		String running = read(server, 20, up, z);
		check(running.contains("RUNNING") && running.contains("breaker CLOSED"), "at speed it should close its breaker onto the dead bus: " + running);
		String line = read(server, 31, up + 4, z + 3);
		check(line.contains("138 kV") && !line.contains(" 0 V"), "the GSU should energize the 138 kV side: " + line);
		// 10 MW on the load bank
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.LoadBankBlockEntity) srv.overworld().getBlockEntity(new BlockPos(31, up, z + 10))).setSteps(10);
			return null;
		});
		context.waitTicks(200);
		String loaded = read(server, 20, up, z);
		String bank = read(server, 31, up, z + 10);
		check(bank.contains("taking 9.") || bank.contains("taking 10."), "the load bank should take about 10 MW: " + bank + " | " + loaded);
		check(loaded.contains("burning") && loaded.contains("kerosene"), "it should burn kerosene at its heat rate: " + loaded);
		// the HRSG raises steam and the steam turbine joins in
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.HrsgBlockEntity) srv.overworld().getBlockEntity(hrsg)).warmForTest(srv.overworld());
			return null;
		});
		context.waitTicks(1000);   // it rolls up in 30 s, synchronizes and takes its share
		String steam = read(server, 17, up, z + 15);
		check(steam.contains("breaker CLOSED"), "the steam turbine should synchronize on the HRSG's steam: " + steam + " | hrsg " + read(server, 20, up, z + 10)
				+ " | acc " + read(server, 12, up, z + 9) + " | pump " + read(server, 21, up, z + 22));
		// near full load (the second bank on too) the combined cycle is over 40 % efficient (at 10 MW it was part load)
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.LoadBankBlockEntity) srv.overworld().getBlockEntity(new BlockPos(28, up, z + 10))).setSteps(10);
			return null;
		});
		context.waitTicks(400);
		String plant = read(server, 20, up, z);
		java.util.regex.Matcher eff = java.util.regex.Pattern.compile("(\\d+)% efficient").matcher(plant);
		check(eff.find() && Integer.parseInt(eff.group(1)) > 40, "the combined cycle should be over 40 % efficient near full load: " + plant + " | " + read(server, 17, up, z + 15));
		String acc = read(server, 12, up, z + 9);
		check(acc.contains("running"), "the condenser fans should run: " + acc);
		server.runCommand("tp @p 2 " + (up + 14) + " " + (z + 32) + " 215 25");
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-gas-turbine");
		System.out.println("[wildcatter-test] gas turbine ok: " + plant + " | " + steam + " | " + bank + " | " + line);
	}

	/**
	 * The 45 MW aeroderivative peaker (6b, docs/multiblocks/aeroderivative.md): a diesel set's 4.16 kV turns its starter and it
	 * purges; it closes onto a dead 13.8 kV bus over 3,000 A bus duct and holds at its ramp limit; four load banks take 40 MW; its
	 * 60 MVA GSU puts 138 kV on a line to a 60 MVA substation transformer, whose 12.47 kV side is live.
	 */
	private void peaker(ClientGameTestContext context, TestServerContext server) {
		int z = -1390;
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand("tp @p 30 " + (up + 18) + " " + (z + 34) + " 180 30");
		context.waitTicks(20);
		server.runCommand("fill 2 " + (y - 1) + " " + (z - 14) + " 44 " + (y + 16) + " " + (z + 22) + " minecraft:air");
		server.runCommand("fill 2 " + (y - 1) + " " + (z - 14) + " 44 " + (y - 1) + " " + (z + 22) + " minecraft:stone");
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		BlockPos gt = new BlockPos(20, up, z);
		placeBig(server, WildcatterBlocks.AERO_TURBINE, gt, north);                                 // x 20-24, z..z+13
		context.waitTicks(25);
		String unbuilt = read(server, 20, up, z);
		check(unbuilt.contains("NOT BUILT") && unbuilt.contains("INLET HOUSE") && unbuilt.contains("EXHAUST STACK"), "a lone package should name its parts: " + unbuilt);
		placeBig(server, WildcatterBlocks.AERO_INLET_HOUSE, new BlockPos(20, up, z - 5), north);    // x 20-25, z-5..z-1
		placeBig(server, WildcatterBlocks.AERO_EXHAUST_STACK, new BlockPos(20, up, z + 14), north); // x 20-23, z+14..z+17
		placeBig(server, WildcatterBlocks.AERO_GENERATOR, new BlockPos(25, up, z + 1), north);      // x 25-28, z+1..z+7
		placeBig(server, WildcatterBlocks.GEN_BREAKER_3000, new BlockPos(26, up, z + 8), north);    // x 26-27, z+8..z+9
		placeBig(server, WildcatterBlocks.GSU_60, new BlockPos(36, up, z + 8), north);              // x 33-38, z+4..z+8
		placeBig(server, WildcatterBlocks.SUBSTATION_TRANSFORMER_60, new BlockPos(36, up, z - 6), north);   // x 33-38, z-10..z-6
		int[] banks = {26, 29, 32, 35};
		for (int x : banks) {
			placeBig(server, WildcatterBlocks.LOAD_BANK, new BlockPos(x, up, z + 11), north);       // x..x+2, z+11..z+16
		}
		BlockPos fuelTank = new BlockPos(14, up, z + 4);
		placeBig(server, WildcatterBlocks.STOCK_TANK, fuelTank, north);                              // x 14-17, z+4..z+7
		pipes(server, z, new int[][] {{18, 0, 6}, {19, 0, 6}});                                     // kerosene to the fuel nozzle
		// the 13.8 kV bus, 3,000 A: the breaker's back, the four banks, up to the GSU's throat
		for (int x = 26; x <= 37; x++) {
			put(server, x, up, z + 10, "wildcatter:bus_duct_gen");
		}
		put(server, 36, up, z + 9, "wildcatter:bus_duct_gen");
		sizeCables(server, new BlockPos(26, up, z + 9), new BlockPos(37, up, z + 10), WildcatterBlocks.BUS_DUCT_GEN, dev.wildcatter.energy.ConductorSize.BUS_DUCT_3000);
		// the 138 kV line, bushing top to bushing top, over the yard
		for (int zz = z - 10; zz <= z + 4; zz++) {
			put(server, 36, up + 6, zz, "wildcatter:cable_hv");
		}
		put(server, 36, up, z - 5, "wildcatter:cable_dist");   // on the substation transformer's 12.47 kV throat
		// station power: a diesel set, 4.16 kV through a backward unit substation to the starter
		BlockPos set = new BlockPos(10, up, z + 2);
		genset(server, set, north, false);                                                        // x 5-10, z+2..z+3
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, set, net.minecraft.core.Direction.EAST, false);   // x 11-14, z+1..z+2
		for (int x = 15; x <= 19; x++) {
			put(server, x, up, z + 2, "wildcatter:cable_mv");   // to the starter's box, the west face of the package at z+2
		}
		exportBlueprint(server, "aeroderivative", new BlockPos(2, y, z - 14), new BlockPos(44, y + 16, z + 22));   // the library (docs/blueprints.md)
		fillTank(server, fuelTank, dev.wildcatter.fluid.Product.KEROSENE, 40_000);
		startGenset(context, server, set, north, 2000);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.GasTurbineBlockEntity) srv.overworld().getBlockEntity(gt)).setWantRun(true);
			return null;
		});
		context.waitTicks(200);
		String purge = read(server, 20, up, z);
		check(purge.contains("PURGE") && purge.contains("25% speed"), "the starter should purge it at a quarter speed: " + purge + " | set " + read(server, 10, up, z + 2));
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.GasTurbineBlockEntity) srv.overworld().getBlockEntity(gt)).skipStartForTest(false);
			return null;
		});
		context.waitTicks(100);
		String running = read(server, 20, up, z);
		check(running.contains("RUNNING") && running.contains("breaker CLOSED"), "at speed it should close its breaker onto the dead bus: " + running);
		check(running.contains("ramping"), "just on line it should be held to its ramp: " + running);
		String line = read(server, 36, up + 6, z - 3);
		check(line.contains("138 kV") && !line.contains(" 0 V"), "the GSU should energize the 138 kV line: " + line);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.GasTurbineBlockEntity) srv.overworld().getBlockEntity(gt)).skipRampForTest();
			for (int x : banks) {
				((dev.wildcatter.block.LoadBankBlockEntity) srv.overworld().getBlockEntity(new BlockPos(x, up, z + 11))).setSteps(10);
			}
			return null;
		});
		context.waitTicks(300);
		String loaded = read(server, 20, up, z);
		String gen = read(server, 25, up, z + 1);
		double total = 0;
		for (int x : banks) {
			java.util.regex.Matcher taking = java.util.regex.Pattern.compile("taking ([0-9.]+) MW").matcher(read(server, x, up, z + 11));
			check(taking.find(), "each bank should take its load: " + read(server, x, up, z + 11) + " | " + loaded);
			total += Double.parseDouble(taking.group(1));
		}
		check(total > 36, "the four banks should take about 40 MW: " + total + " | " + loaded + " | " + gen);
		java.util.regex.Matcher hz = java.util.regex.Pattern.compile("([0-9.]+) Hz").matcher(gen);
		check(hz.find() && Double.parseDouble(hz.group(1)) > 57.5, "it should hold the frequency up: " + gen);
		check(loaded.contains("burning") && loaded.contains("kerosene"), "it should burn kerosene: " + loaded);
		String low = read(server, 36, up, z - 5);
		java.util.regex.Matcher phase = java.util.regex.Pattern.compile("A (\\d+) V").matcher(low);
		check(phase.find() && Math.abs(Integer.parseInt(phase.group(1)) - 7200) < 500, "the substation transformer's 12.47 kV side should read about 7.2 kV a phase: " + low);
		server.runCommand("tp @p 46 " + (up + 16) + " " + (z + 30) + " 145 25");
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-peaker");
		System.out.println("[wildcatter-test] peaker ok: " + String.format("%.1f MW", total) + " | " + loaded + " | " + gen + " | " + line + " | " + low);
	}

	private double tankAmount(TestServerContext server, BlockPos cell) {
		return server.computeOnServer(srv -> dev.wildcatter.multiblock.BigParts.entity(srv.overworld(), cell) instanceof dev.wildcatter.block.StorageTankBlockEntity t ? t.amount() : -1.0);
	}

	/** What the Power Meter says on a pipe. */
	private String pipeReading(TestServerContext server, BlockPos pipe) {
		return server.computeOnServer(srv -> "Steel pipe: " + dev.wildcatter.block.Lines.line(srv.overworld(), pipe).run().describe());
	}

	/**
	 * The 1,000 hp land rig (docs/multiblocks/land-rig.md) on a pad 45 m up, over a deep oil field: built part by part by clicks,
	 * the readout naming what is missing; the top drive clicked into the mast; a 120 V starter plant's pump filling the mud tanks;
	 * a 500 kW diesel set through a unit substation run backward to 4.16 kV, up an MV cable to the drive house. Far short of the
	 * rig's 1.9 MW, the drives power-limit and the set keeps running; the rig drills, makes a connection, strikes and sets an oil
	 * wellhead in its cellar. Taking a mud pump away unbuilds it, and with no water it stops for mud.
	 */
	private void landRig(ClientGameTestContext context, TestServerContext server) {
		int hx = 30, hz = -440;
		int pad = up + 45;
		rules(server, true, "saltDomeGrid192", "mudGel", "wellControl");   // Phase 15's rig, as in a new world
		BlockPos hole = new BlockPos(hx, pad + 1, hz);
		long seed = -1;
		for (long s = 1; s < 3_000_000 && seed < 0; s++) {
			Reservoir field = Reservoir.at(s, hx, hz);
			if (field != null && field.deep() && field.kind() == Reservoir.Kind.OIL && hole.getY() - field.topY() > Tuning.LAND_RIG_STAND_BLOCKS + 3) {
				seed = s;
			}
		}
		check(seed >= 0, "no seed puts a deep oil field under the land rig");
		ReservoirData.testSeed = seed;
		final long found = seed;
		try {
			server.runCommand("effect give @p minecraft:slow_falling 600 0 true");   // the pad is 45 m up: a fall would kill the player and unload the genset
			server.runCommand("tp @p " + hx + " " + (up + 2) + " " + (hz + 14) + " 180 0");   // so the scene's chunks are loaded before it is built
			context.waitTicks(40);
			server.runCommand("kill @e[type=item]");
			server.runCommand("fill 0 " + (y - 1) + " -452 47 " + (y - 1) + " -424 minecraft:stone");
			server.runCommand("fill 0 " + y + " -452 47 " + (y + 6) + " -424 minecraft:air");
			server.runCommand("fill 20 " + pad + " -448 47 " + pad + " -426 minecraft:stone");
			String echo = server.computeOnServer(srv -> SeismographItem.report(srv.overworld(), hx + 0.5, hz + 0.5));
			check(echo.contains("DEEP ECHO"), "a seismograph over a deep field should say so: " + echo);
			// the substructure first: clicked on the pad from the north, looking south, it faces north over the hole
			BlockPos origin = new BlockPos(hx - 4, pad + 1, hz - 4);
			check(placeOn(server, WildcatterBlocks.RIG_SUBSTRUCTURE, origin.below(), hx - 4 + 0.5, hz - 7 + 0.5, 0.0F), "the substructure should place");
			check(stateAt(server, origin).is(WildcatterBlocks.RIG_SUBSTRUCTURE) && stateAt(server, origin).getValue(dev.wildcatter.block.RigPartBlock.FACING) == net.minecraft.core.Direction.NORTH,
					"the substructure should face north: " + stateAt(server, origin));
			context.waitTicks(25);
			check(read(server, origin.getX(), origin.getY(), origin.getZ()).contains("NOT BUILT: needs the"), "alone it should name what it needs: " + read(server, origin.getX(), origin.getY(), origin.getZ()));
			// each part clicked somewhere on the floor or the pad snaps into its place
			BlockPos floor = new BlockPos(hx + 3, pad + 6, hz - 3);
			check(placeOn(server, WildcatterBlocks.RIG_MAST, floor, hx + 3.5, hz - 2.5, 0.0F), "the mast should place on the floor");
			check(placeOn(server, WildcatterBlocks.RIG_DRAWWORKS, floor, hx + 3.5, hz - 2.5, 0.0F), "the drawworks should place on the floor");
			BlockPos ground = new BlockPos(hx + 2, pad, hz + 12);
			for (net.minecraft.world.level.block.Block part : new net.minecraft.world.level.block.Block[] {WildcatterBlocks.MUD_TANKS, WildcatterBlocks.MUD_PUMP,
					WildcatterBlocks.MUD_PUMP, WildcatterBlocks.RIG_DRIVE_HOUSE}) {
				check(placeOn(server, part, ground, hx + 2.5, hz + 12.5, 180.0F), part.getName().getString() + " should place near the rig");
			}
			check(stateAt(server, new BlockPos(hx - 1, pad + 7, hz - 1)).is(WildcatterBlocks.RIG_MAST), "the mast should have snapped over the hole");
			check(stateAt(server, new BlockPos(hx + 10, pad + 1, hz)).is(WildcatterBlocks.MUD_PUMP), "the second pump should have gone to its own place");
			check(stateAt(server, new BlockPos(hx - 6, pad + 1, hz + 7)).is(WildcatterBlocks.RIG_DRIVE_HOUSE), "the drive house should be behind the rig");
			context.waitTicks(25);
			String noDrive = read(server, origin.getX(), origin.getY(), origin.getZ());
			check(noDrive.contains("NO TOP DRIVE"), "built, it should ask for its top drive: " + noDrive);
			check(clickWith(server, new BlockPos(hx - 1, pad + 20, hz - 1), net.minecraft.core.Direction.WEST, new ItemStack(WildcatterItems.TOP_DRIVE)), "the top drive should go into the mast");
			// 15b2: a deep field is geopressured: a BOP stack on the wellhead cell under the rotary table and its accumulator nearby
			placeBig(server, WildcatterBlocks.BOP_STACK, new BlockPos(hx, pad + 2, hz), net.minecraft.core.Direction.NORTH);
			placeBig(server, WildcatterBlocks.BOP_ACCUMULATOR, new BlockPos(hx - 10, pad + 1, hz - 10), net.minecraft.core.Direction.NORTH);
			// a ledge to watch from, south-west of the rig (the player must not fall: the genset far below only runs near a player)
			// (near enough that the rig, its pumps and the genset all keep ticking)
			server.runCommand("fill " + (hx - 12) + " " + (pad + 14) + " " + (hz + 18) + " " + (hx - 8) + " " + (pad + 14) + " " + (hz + 22) + " minecraft:stone");
			server.runCommand("tp @p " + (hx - 10) + " " + (pad + 15) + " " + (hz + 20) + " -150 -25");
			// water: a 120 V starter plant runs a pump from a pond, its pipe to the mud tanks' east side
			put(server, 42, pad, hz + 9, "minecraft:campfire[lit=true]");
			put(server, 42, pad + 1, hz + 9, "wildcatter:boiler");
			put(server, 41, pad + 1, hz + 9, "wildcatter:steam_turbine");
			put(server, 40, pad + 1, hz + 9, "wildcatter:wool_wire_hot");
			put(server, 39, pad + 1, hz + 9, "wildcatter:pump");
			for (int x = 39; x <= 41; x++) {
				put(server, x, pad + 1, hz + 10, "wildcatter:wool_wire_neutral");
			}
			put(server, 37, pad + 1, hz + 9, "minecraft:stone");
			put(server, 38, pad + 1, hz + 8, "minecraft:stone");
			put(server, 38, pad + 1, hz + 10, "minecraft:stone");
			put(server, 38, pad + 2, hz + 9, "minecraft:stone");
			put(server, 38, pad + 1, hz + 9, "minecraft:water");
			for (int z = hz + 8; z >= hz + 6; z--) {
				put(server, 39, pad + 1, z, "wildcatter:pipe");
			}
			server.computeOnServer(srv -> ((BoilerBlockEntity) srv.overworld().getBlockEntity(new BlockPos(42, pad + 1, hz + 9))).addWater(4000));
			// power: a 500 kW diesel set, a unit substation run backward to 4.16 kV, and MV cable up to the drive house's west end
			BlockPos set = new BlockPos(10, up, hz + 7);
			genset(server, set, net.minecraft.core.Direction.NORTH, false);
			inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, set, net.minecraft.core.Direction.EAST, false);
			for (int x = 15; x <= hx - 7; x++) {
				put(server, x, up, hz + 7, "wildcatter:cable_mv");
			}
			for (int h = up + 1; h <= pad + 1; h++) {
				put(server, hx - 7, h, hz + 7, "wildcatter:cable_mv");
			}
			exportBlueprint(server, "land_rig", new BlockPos(0, y, -452), new BlockPos(47, y + 6, -424));   // the library (docs/blueprints.md)
			startGenset(context, server, set, net.minecraft.core.Direction.NORTH, 1000);
			context.waitTicks(100);
			String filling = read(server, origin.getX(), origin.getY(), origin.getZ());
			check(filling.matches(".*mud water [1-9]\\d* of.*"), "the pump should be filling the mud tanks: " + filling + " | " + stateAt(server, origin) + " | pump " + read(server, 39, pad + 1, hz + 9));
			server.computeOnServer(srv -> {
				weighted(rig(srv, origin));   // topped up and weighted past 13.8 ppg (15b2) so the test need not wait for the hopper
				rig(srv, origin).toggle();   // the driller's console: drill
				return null;
			});
			context.waitTicks(300);
			String drilling = read(server, origin.getX(), origin.getY(), origin.getZ());
			String gen = read(server, set.getX(), set.getY(), set.getZ());
			check(drilling.contains("drilling at") && drilling.contains("POWER LIMITED"), "on 500 kW the rig should drill power-limited: " + drilling);
			check(gen.contains("RUNNING") && gen.contains("breaker CLOSED"), "the power limit should keep the genset on line: " + gen);
			java.util.regex.Matcher hz1 = java.util.regex.Pattern.compile("([0-9.]+) Hz").matcher(drilling);
			check(hz1.find() && Double.parseDouble(hz1.group(1)) > 59.0, "the frequency should be held up: " + drilling);
			float topRpm = context.computeOnClient(client -> client.level.getBlockEntity(origin) instanceof dev.wildcatter.block.RigSubstructureBlockEntity r ? r.spin().rpm() : -1F);
			check(Math.abs(topRpm - Tuning.TOP_DRIVE_RPM) < 1, "the client should see the top drive turning at 120 rpm: " + topRpm);
			context.takeScreenshot("wildcatter-land-rig");
			// with no water it stops for mud; with a mud pump gone it is not built
			server.computeOnServer(srv -> {
				rig(srv, origin).testWater(0);
				return null;
			});
			context.waitTicks(5);
			String noMud = read(server, origin.getX(), origin.getY(), origin.getZ());
			check(noMud.contains("NO MUD"), "with no water it should stop for mud: " + noMud);
			server.computeOnServer(srv -> {
				weighted(rig(srv, origin));
				srv.overworld().destroyBlock(new BlockPos(hx + 10, pad + 1, hz), false);
				return null;
			});
			context.waitTicks(30);
			String unbuilt = read(server, origin.getX(), origin.getY(), origin.getZ());
			check(unbuilt.contains("NOT BUILT") && unbuilt.contains("mud pump"), "with a mud pump gone it should stop: " + unbuilt);
			check(placeOn(server, WildcatterBlocks.MUD_PUMP, ground, hx + 2.5, hz + 12.5, 180.0F), "the pump should go back in its place");
			// on to the field: a connection at 27 m, then the strike
			boolean connected = false;
			for (int waited = 0; waited < 4000; waited += 20) {
				server.computeOnServer(srv -> {   // the mud engineer keeps the weight up as the pump's water thins it
					while (rig(srv, origin).mudPpg() < 13.9 && rig(srv, origin).roomFor(dev.wildcatter.block.RigSubstructureBlockEntity.Solid.BARITE) > 1000) {
						rig(srv, origin).testMix(dev.wildcatter.block.RigSubstructureBlockEntity.Solid.BARITE, Tuning.BULK_BAG_KG);
					}
					return null;
				});
				String now = read(server, origin.getX(), origin.getY(), origin.getZ());
				connected |= now.contains("making a connection");
				if (now.contains("WELL COMPLETE")) {
					break;
				}
				context.waitTicks(20);
			}
			String done = read(server, origin.getX(), origin.getY(), origin.getZ());
			check(connected, "the rig should have made a connection after its first 27 m stand");
			check(done.contains("WELL COMPLETE") && stateAt(server, hole).is(WildcatterBlocks.OIL_WELLHEAD), "the rig should strike the deep oil and set a wellhead in its cellar: " + done);
			int topY = Reservoir.at(found, hx, hz).topY();
			check(stateAt(server, new BlockPos(hx, topY, hz)).is(WildcatterBlocks.CASING), "the hole should be cased down to the deep field");
			System.out.println("[wildcatter-test] land rig ok: " + echo + " | " + drilling + " | " + done + " | " + gen);
		} finally {
			server.runCommand("effect clear @p minecraft:slow_falling");
			ReservoirData.testSeed = null;
			rules(server, false, "saltDomeGrid192", "mudGel", "wellControl");
		}
	}

	/** Sets world rules as an operator would: {@code /wildcatter rule <key> <on>}. */
	static void rules(TestServerContext server, boolean on, String... keys) {
		for (String key : keys) {
			server.runCommand("wildcatter rule " + key + " " + on);
		}
	}

	/** 45 m3 of water, gelled with bentonite and weighted with barite past 13.8 ppg: enough to drill into a deep field (15b2). */
	private static void weighted(dev.wildcatter.block.RigSubstructureBlockEntity rig) {
		rig.testWater(0);
		rig.testWater(45_000);
		rig.testMix(dev.wildcatter.block.RigSubstructureBlockEntity.Solid.BENTONITE, 71.3 * 45);
		while (rig.mudPpg() < 13.9) {
			rig.testMix(dev.wildcatter.block.RigSubstructureBlockEntity.Solid.BARITE, Tuning.BULK_BAG_KG);
		}
	}

	private dev.wildcatter.block.RigSubstructureBlockEntity rig(net.minecraft.server.MinecraftServer srv, BlockPos origin) {
		net.minecraft.world.level.block.entity.BlockEntity entity = srv.overworld().getBlockEntity(origin);
		if (!(entity instanceof dev.wildcatter.block.RigSubstructureBlockEntity rig)) {
			throw new AssertionError("no rig at " + origin + ": " + srv.overworld().getBlockState(origin) + " owner " + dev.wildcatter.multiblock.BigParts.owner(srv.overworld(), origin)
					+ " entity " + entity + " via owner " + dev.wildcatter.multiblock.BigParts.entity(srv.overworld(), origin));
		}
		return rig;
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
		exportBlueprint(server, "steam_plant", new BlockPos(0, y, z - 6), new BlockPos(24, y + 8, z + 9));   // the library (docs/blueprints.md)
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
		exportBlueprint(server, "substation", new BlockPos(0, y, z - 16), new BlockPos(40, y + 9, z + 12));   // the library (docs/blueprints.md)
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

	// ---- Phase 7a: the ores (docs/ores.md) ------------------------------------------------------------------------------

	/** Tops up the water of each water user (a boiler, an absorber, a SAG mill, a thickener) as a pump would, as the cracker test does. */
	private void water(TestServerContext server, BlockPos... mains) {
		server.computeOnServer(srv -> {
			for (BlockPos at : mains) {
				if (srv.overworld().getBlockEntity(at) instanceof dev.wildcatter.block.WaterUser user) {
					user.addWater(100_000);
				}
			}
			return null;
		});
	}

	private int countIn(TestServerContext server, BlockPos hopper, net.minecraft.world.item.Item item) {
		return server.computeOnServer(srv -> srv.overworld().getBlockEntity(hopper) instanceof net.minecraft.world.Container h
				? java.util.stream.IntStream.range(0, h.getContainerSize()).map(i -> h.getItem(i).is(item) ? h.getItem(i).getCount() : 0).sum() : -1);
	}

	/**
	 * The sulfuric acid plant (7b, docs/multiblocks/acid-plant.md). Every part faces west, so the gas runs east: the drying tower, the
	 * main blower and the sulfur furnace in a line, the waste heat boiler at the furnace's end with the melter against its north side,
	 * the converter beyond with the interpass absorber east of it and the final absorber north. Sulfur in a hopper on the melter, start-up
	 * LPG from a bullet, a diesel set through a backward unit substation for the blower's 4.16 kV. Acid fills a stock tank; with the
	 * interpass absorber gone the converter runs single absorption and the stack's SO2 rises at least tenfold.
	 */
	private void acidPlant(ClientGameTestContext context, TestServerContext server) {
		int z = -1600;
		server.runCommand("effect give @p minecraft:slow_falling 600 0 true");
		server.runCommand("tp @p 18 " + (up + 14) + " " + (z + 22) + " 180 30");
		context.waitTicks(20);
		server.runCommand("fill -2 " + (y - 1) + " " + (z - 12) + " 40 " + (y + 22) + " " + (z + 10) + " minecraft:air");
		server.runCommand("fill -2 " + (y - 1) + " " + (z - 12) + " 40 " + (y - 1) + " " + (z + 10) + " minecraft:stone");
		net.minecraft.core.Direction west = net.minecraft.core.Direction.WEST;
		placeBig(server, WildcatterBlocks.DRYING_TOWER, new BlockPos(0, up, z), west);              // x 0-2, z-2..z
		placeBig(server, WildcatterBlocks.MAIN_BLOWER, new BlockPos(3, up, z), west);               // x 3-7, z-2..z
		BlockPos furnace = new BlockPos(8, up, z);
		placeBig(server, WildcatterBlocks.SULFUR_FURNACE, furnace, west);                          // x 8-17, z-2..z
		BlockPos boiler = new BlockPos(18, up, z);
		placeBig(server, WildcatterBlocks.WASTE_HEAT_BOILER, boiler, west);                        // x 18-25, z-2..z
		BlockPos melter = new BlockPos(18, up, z - 3);
		placeBig(server, WildcatterBlocks.SULFUR_MELTER, melter, west);                            // x 18-25, z-6..z-3
		BlockPos converter = new BlockPos(27, up, z);
		placeBig(server, WildcatterBlocks.CONVERTER, converter, west);                             // x 27-31, z-4..z
		placeBig(server, WildcatterBlocks.INTERPASS_ABSORBER, new BlockPos(32, up, z), west);      // x 32-34, z-2..z
		BlockPos absorber = new BlockPos(27, up, z - 5);
		placeBig(server, WildcatterBlocks.FINAL_ABSORBER, absorber, west);                         // x 27-29, z-7..z-5
		BlockPos tank = new BlockPos(31, up, z - 6);
		placeBig(server, WildcatterBlocks.STOCK_TANK, tank, west);                                 // x 31-34, z-9..z-6
		BlockPos bullet = new BlockPos(6, up, z - 8);
		placeBig(server, WildcatterBlocks.LPG_BULLET, bullet, west);                               // x 6-17, z-10..z-8
		// molten sulfur: out of the melter's east end, up and over, down to the burner on the furnace's north side
		pipes(server, z, new int[][] {{26, 0, -5}, {26, 1, -5}, {26, 2, -5}, {26, 3, -5}});
		for (int x = 8; x <= 25; x++) {
			put(server, x, up + 3, z - 5, "wildcatter:gas_line");
		}
		pipes(server, z, new int[][] {{8, 3, -4}, {8, 3, -3}, {8, 2, -3}, {8, 1, -3}, {8, 0, -3}});
		pipes(server, z, new int[][] {{26, 0, -1}});                                                  // burner gas, boiler to converter
		pipes(server, z, new int[][] {{30, 0, -6}});                                                  // acid, final absorber to the stock tank
		for (int x = 17; x <= 25; x++) {
			put(server, x, up, z - 7, "wildcatter:gas_line");                                         // start-up LPG along the melter's north side
		}
		put(server, 20, up + 3, z - 3, "minecraft:hopper");
		// power: two diesel sets in parallel on a busway, a backward unit substation to 4.16 kV, the line to the blower's box south of
		// its main cell. A 250 kW motor started across the line takes one 500 kW set down on frequency
		BlockPos[] sets = {new BlockPos(4, up, z + 9), new BlockPos(12, up, z + 9)};
		for (BlockPos set : sets) {
			genset(server, set, net.minecraft.core.Direction.NORTH, false);                         // x -1..4 and 7-12, z+9..z+10
			put(server, set.getX() + 1, up, z + 9, "wildcatter:busway_industrial");
		}
		for (int x = 5; x <= 14; x++) {
			put(server, x, up, z + 8, "wildcatter:busway_industrial");
		}
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, new BlockPos(14, up, z + 8), net.minecraft.core.Direction.EAST, false);   // x 15-18, z+7..z+8
		for (int zz = z + 2; zz <= z + 8; zz++) {
			put(server, 19, up, zz, "wildcatter:cable_mv");
		}
		for (int x = 3; x <= 18; x++) {
			put(server, x, up, z + 2, "wildcatter:cable_mv");
		}
		put(server, 3, up, z + 1, "wildcatter:cable_mv");
		exportBlueprint(server, "acid_plant", new BlockPos(-2, y, z - 12), new BlockPos(40, y + 22, z + 10));   // the library (docs/blueprints.md)
		fillTank(server, bullet, dev.wildcatter.fluid.Product.LPG, 20_000);
		water(server, boiler, absorber);
		for (BlockPos set : sets) {
			startGenset(context, server, set, net.minecraft.core.Direction.NORTH, 2000);   // the second syncs onto the first
		}
		// the sets first, then the plant: sulfur into the melter's hopper once the blower has a live bus to start on
		server.runCommand("item replace block 20 " + (up + 3) + " " + (z - 3) + " container.0 with wildcatter:sulfur 8");
		for (int i = 0; i < 6; i++) {
			context.waitTicks(100);
			water(server, boiler, absorber);
		}
		String m = read(server, 18, up, z - 3), f = read(server, 8, up, z), c = read(server, 27, up, z), t = read(server, 31, up, z - 6);
		String chain = " | melter " + m + " | furnace " + f + " | boiler " + read(server, 18, up, z) + " | converter " + c + " | absorber " + read(server, 27, up, z - 5)
				+ " | tank " + t + " | sets " + read(server, 4, up, z + 9) + " / " + read(server, 12, up, z + 9) + " | blower " + read(server, 3, up, z);
		check(m.contains("melting"), "the melter should melt sulfur: " + chain);
		check(f.contains("burning"), "the furnace should burn sulfur: " + chain);
		check(c.contains("double absorption"), "the converter should run double absorption: " + chain);
		double acid = server.computeOnServer(srv -> ((dev.wildcatter.block.StorageTankBlockEntity) srv.overworld().getBlockEntity(tank)).amount());
		check(acid > 1, "the stock tank should fill with acid: " + acid + chain);
		double doubleSo2 = server.computeOnServer(srv -> ((dev.wildcatter.block.ConverterBlockEntity) srv.overworld().getBlockEntity(converter)).stackSo2());
		server.runCommand("tp @p 16 " + (up + 12) + " " + (z + 22) + " 200 25");
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-acid-plant");
		// take the interpass absorber away: single absorption, and the stack shows it
		server.runCommand("fill 32 " + up + " " + (z - 2) + " 34 " + (up + 11) + " " + z + " minecraft:air");
		water(server, boiler, absorber);
		context.waitTicks(100);
		String single = read(server, 27, up, z);
		double singleSo2 = server.computeOnServer(srv -> ((dev.wildcatter.block.ConverterBlockEntity) srv.overworld().getBlockEntity(converter)).stackSo2());
		check(single.contains("SINGLE ABSORPTION") && singleSo2 >= doubleSo2 * 10, "without the interpass absorber the stack SO2 should rise tenfold: "
				+ doubleSo2 + " -> " + singleSo2 + " kg/h | " + single);
		System.out.println("[wildcatter-test] acid plant ok: " + f + " | " + c + " | tank " + t + " | single: " + singleSo2 + " kg/h vs " + doubleSo2);
	}

	/**
	 * The uranium mill (7b, docs/multiblocks/uranium-mill.md). Every part faces west, so the ore goes east: crusher and SAG mill, leach
	 * tank, CCD thickener, SX circuit, precipitation tank, with the yellowcake dryer and the drum filler against it. Acid from a stock
	 * tank, LPG for the dryer from a bullet, water and the SX's kerosene by hooks. A diesel set at 480 V for the small motors and a
	 * second through a backward unit substation for the SAG mill's 4.16 kV. Pitchblende is held to SX's limit; uranium ore then runs at
	 * the full rate. The filler is given 380 kg by hook, and the mill's own yellowcake must finish the drum into the hopper under it.
	 */
	/** The lined cell (docs/multiblocks/tailings-cell.md): finds its basin, fills, reads a broken floor as a breach, and goes on when mended. */
	private void tailingsCell(ClientGameTestContext context, TestServerContext server) {
		int z = 420;
		server.computeOnServer(srv -> {
			net.minecraft.server.level.ServerLevel level = srv.overworld();
			for (int x = 0; x <= 9; x++) {
				for (int zz = z; zz <= z + 9; zz++) {
					for (int yy = 0; yy <= 3; yy++) {
						boolean wall = x == 0 || x == 9 || zz == z || zz == z + 9;
						level.setBlock(new BlockPos(x, up + yy, zz), yy == 0 || wall ? WildcatterBlocks.CELL_LINER.defaultBlockState() : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
					}
				}
			}
			level.setBlock(new BlockPos(4, up + 1, z + 4), WildcatterBlocks.DECANT_TOWER.defaultBlockState(), 3);
			return null;
		});
		BlockPos tower = new BlockPos(4, up + 1, z + 4);
		server.runCommand("gamemode spectator @p");
		server.runCommand("tp @p 15 " + (up + 9) + " " + (z - 6) + " 45 30");
		context.waitTicks(60);
		check(cell(server, tower).status().contains("of 420 t"), "an 8 x 8 cell, 3 high, should hold 191 blocks at 2.2 t: " + cell(server, tower).status());
		server.computeOnServer(srv -> {
			cell(srv, tower).addSolidsForTest(40 * Tuning.CELL_KG_PER_BLOCK);
			return null;
		});
		context.waitTicks(60);
		int shown = server.computeOnServer(srv -> {
			int n = 0;
			for (BlockPos at : BlockPos.betweenClosed(new BlockPos(1, up + 1, z + 1), new BlockPos(8, up + 3, z + 8))) {
				n += srv.overworld().getBlockState(at).getBlock() == WildcatterBlocks.SETTLED_TAILS ? 1 : 0;
			}
			return n;
		});
		check(shown == 40, "forty blocks of settled tails should show, got " + shown);
		context.takeScreenshot("wildcatter-tailings-cell");
		server.computeOnServer(srv -> srv.overworld().setBlock(new BlockPos(8, up, z + 8), net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(), 3));
		context.waitTicks(60);
		check(cell(server, tower).status().contains("BREACHED"), "a broken floor should read as a breach: " + cell(server, tower).status());
		server.computeOnServer(srv -> srv.overworld().setBlock(new BlockPos(8, up, z + 8), WildcatterBlocks.CELL_LINER.defaultBlockState(), 3));
		context.waitTicks(60);
		check(!cell(server, tower).status().contains("BREACHED"), "mended, it should go on: " + cell(server, tower).status());
		server.computeOnServer(srv -> {
			cell(srv, tower).addSolidsForTest(1000 * Tuning.CELL_KG_PER_BLOCK);
			return null;
		});
		context.waitTicks(60);
		check(cell(server, tower).status().contains("BREACHED"), "more than it can hold reads as over its walls: " + cell(server, tower).status());
		server.runCommand("gamemode survival @p");
		System.out.println("[wildcatter-test] tailingsCell ok: " + cell(server, tower).status());
	}

	private dev.wildcatter.block.DecantTowerBlockEntity cell(TestServerContext server, BlockPos tower) {
		return server.computeOnServer(srv -> cell(srv, tower));
	}

	private static dev.wildcatter.block.DecantTowerBlockEntity cell(net.minecraft.server.MinecraftServer srv, BlockPos tower) {
		return (dev.wildcatter.block.DecantTowerBlockEntity) srv.overworld().getBlockEntity(tower);
	}

	private void uraniumMill(ClientGameTestContext context, TestServerContext server) {
		int z = -1700;
		server.runCommand("effect give @p minecraft:slow_falling 600 0 true");
		server.runCommand("tp @p 28 " + (up + 18) + " " + (z + 26) + " 180 35");
		context.waitTicks(20);
		server.runCommand("fill -20 " + (y - 1) + " " + (z - 14) + " 62 " + (y + 11) + " " + (z + 13) + " minecraft:air");
		server.runCommand("fill -20 " + (y - 1) + " " + (z - 14) + " 62 " + (y - 1) + " " + (z + 13) + " minecraft:stone");
		net.minecraft.core.Direction west = net.minecraft.core.Direction.WEST;
		BlockPos crusher = new BlockPos(0, up, z);
		placeBig(server, WildcatterBlocks.JAW_CRUSHER, crusher, west);                             // x 0-3, z-2..z
		BlockPos sag = new BlockPos(4, up, z);
		placeBig(server, WildcatterBlocks.SAG_MILL, sag, west);                                    // x 4-8, z-6..z
		placeBig(server, WildcatterBlocks.LEACH_TANK, new BlockPos(10, up, z), west);              // x 10-17, z-7..z
		BlockPos ccd = new BlockPos(19, up, z);
		placeBig(server, WildcatterBlocks.CCD_THICKENER, ccd, west);                               // x 19-30, z-11..z
		BlockPos sx = new BlockPos(32, up, z);
		placeBig(server, WildcatterBlocks.SX_CIRCUIT, sx, west);                                   // x 32-47, z-5..z
		placeBig(server, WildcatterBlocks.PRECIPITATION_TANK, new BlockPos(49, up, z), west);      // x 49-52, z-3..z
		placeBig(server, WildcatterBlocks.YELLOWCAKE_DRYER, new BlockPos(53, up, z), west);        // x 53-55, z-2..z
		BlockPos filler = new BlockPos(56, up, z);
		placeBig(server, WildcatterBlocks.DRUM_FILLER, filler, west);                              // x 56-57, z-1..z
		BlockPos acidTank = new BlockPos(10, up, z - 9);
		placeBig(server, WildcatterBlocks.STOCK_TANK, acidTank, west);                             // x 10-13, z-12..z-9
		BlockPos bullet = new BlockPos(48, up, z - 5 - 1);
		placeBig(server, WildcatterBlocks.LPG_BULLET, bullet, west);                               // x 48-59, z-8..z-6
		pipes(server, z, new int[][] {{9, 0, 0}, {18, 0, 0}, {31, 0, 0}, {48, 0, 0}});              // slurry, slurry, liquor, strip liquor
		for (int x = 10; x <= 13; x++) {
			put(server, x, up, z - 8, "wildcatter:gas_line");                                        // acid along the leach tank's north side
		}
		pipes(server, z, new int[][] {{53, 0, -3}, {53, 0, -4}, {53, 0, -5}});                       // LPG to the dryer's north side
		BlockPos under = new BlockPos(56, y, z);
		put(server, 56, y, z, "minecraft:hopper");
		// power: 480 V along z+3 with a stub to each small motor's box, south of its main cell...
		BlockPos lvSet = new BlockPos(20, up, z + 6);
		genset(server, lvSet, net.minecraft.core.Direction.NORTH, false);                           // x 15-20, z+6..z+7
		for (int zz = z + 3; zz <= z + 6; zz++) {
			put(server, 21, up, zz, "wildcatter:cable_industrial");
		}
		for (int x = 0; x <= 56; x++) {
			put(server, x, up, z + 3, "wildcatter:cable_industrial");
		}
		for (int x : new int[] {0, 10, 19, 32, 49, 56}) {
			put(server, x, up, z + 2, "wildcatter:cable_industrial");
			put(server, x, up, z + 1, "wildcatter:cable_industrial");
		}
		sizeCables(server, new BlockPos(0, up, z + 1), new BlockPos(56, up, z + 6), WildcatterBlocks.CABLE_INDUSTRIAL, dev.wildcatter.energy.ConductorSize.AWG_4_0);
		// ...and 4.16 kV for the SAG mill: three sets in parallel on an 800 A busway, a backward unit substation, and the 4.16 kV line
		// overhead to the mill's box. A 522 kW motor started across the line draws about six times its current: one 500 kW set cannot
		// start it (soft starters and drives are in ROADMAP Ideas)
		BlockPos[] sagSets = {new BlockPos(-12, up, z + 11), new BlockPos(-4, up, z + 11), new BlockPos(4, up, z + 11)};
		for (BlockPos set : sagSets) {
			genset(server, set, net.minecraft.core.Direction.NORTH, false);                         // x -17..-12, -9..-4, -1..4; z+11..z+12
			put(server, set.getX() + 1, up, z + 11, "wildcatter:busway_industrial");
		}
		for (int x = -11; x <= 6; x++) {
			put(server, x, up, z + 10, "wildcatter:busway_industrial");
		}
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, new BlockPos(6, up, z + 10), net.minecraft.core.Direction.EAST, false);   // x 7-10, z+9..z+10
		put(server, 11, up, z + 10, "wildcatter:cable_mv");
		put(server, 11, up, z + 9, "wildcatter:cable_mv");
		put(server, 11, up, z + 8, "wildcatter:cable_mv");
		put(server, 11, up + 1, z + 8, "wildcatter:cable_mv");
		for (int x = 4; x <= 11; x++) {
			put(server, x, up + 2, z + 8, "wildcatter:cable_mv");
		}
		for (int zz = z + 1; zz <= z + 7; zz++) {
			put(server, 4, up + 2, zz, "wildcatter:cable_mv");
		}
		put(server, 4, up + 1, z + 1, "wildcatter:cable_mv");
		put(server, 4, up, z + 1, "wildcatter:cable_mv");
		fillTank(server, acidTank, dev.wildcatter.fluid.Product.SULFURIC_ACID, 20_000);
		fillTank(server, bullet, dev.wildcatter.fluid.Product.LPG, 20_000);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.SxCircuitBlockEntity) srv.overworld().getBlockEntity(sx)).testCharge(Tuning.SX_KEROSENE_CHARGE);
			return null;
		});
		water(server, sag, ccd);
		exportBlueprint(server, "uranium_mill", new BlockPos(-20, y, z - 14), new BlockPos(62, y + 11, z + 13));   // the library (docs/blueprints.md)
		startGenset(context, server, lvSet, net.minecraft.core.Direction.NORTH, 2000);
		for (BlockPos set : sagSets) {
			startGenset(context, server, set, net.minecraft.core.Direction.NORTH, 2000);   // the second and third sync onto the first
		}
		// the sets first, then the mill: ore, reagents and drums into the hoppers
		put(server, 1, up + 4, z - 1, "minecraft:hopper");
		server.runCommand("item replace block 1 " + (up + 4) + " " + (z - 1) + " container.0 with wildcatter:pitchblende 1");
		server.runCommand("item replace block 1 " + (up + 4) + " " + (z - 1) + " container.1 with wildcatter:uraninite 8");
		put(server, 12, up + 9, z - 2, "minecraft:hopper");
		server.runCommand("item replace block 12 " + (up + 9) + " " + (z - 2) + " container.0 with wildcatter:pyrolusite 2");
		put(server, 34, up + 3, z - 2, "minecraft:hopper");
		server.runCommand("item replace block 34 " + (up + 3) + " " + (z - 2) + " container.0 with wildcatter:halite 2");
		put(server, 50, up + 5, z - 1, "minecraft:hopper");
		server.runCommand("item replace block 50 " + (up + 5) + " " + (z - 1) + " container.0 with wildcatter:magnesia 2");
		put(server, 56, up + 4, z, "minecraft:hopper");
		server.runCommand("item replace block 56 " + (up + 4) + " " + z + " container.0 with wildcatter:empty_drum 4");
		for (int i = 0; i < 4; i++) {
			context.waitTicks(50);
			water(server, sag, ccd);
		}
		String held = read(server, 4, up, z);
		check(held.contains("60.0 % U3O8") && held.contains("held to SX"), "pitchblende should be held to SX's limit: " + held + " | crusher " + read(server, 0, up, z)
				+ " | sag sets " + read(server, -12, up, z + 11) + " / " + read(server, 4, up, z + 11));
		server.computeOnServer(srv -> {   // the rest of the pitchblende block goes; the uranium ore in the hopper follows
			((dev.wildcatter.block.JawCrusherBlockEntity) srv.overworld().getBlockEntity(crusher)).testClear();
			return null;
		});
		for (int i = 0; i < 8; i++) {
			context.waitTicks(50);
			water(server, sag, ccd);
		}
		String grind = read(server, 4, up, z), leach = read(server, 10, up, z), thick = read(server, 19, up, z), sxr = read(server, 32, up, z),
				pt = read(server, 49, up, z), dry = read(server, 53, up, z), fill = read(server, 56, up, z);
		String chain = " | SAG " + grind + " | leach " + leach + " | CCD " + thick + " | SX " + sxr + " | precipitation " + pt + " | dryer " + dry + " | filler " + fill
				+ " | lv set " + read(server, 20, up, z + 6) + " | sag sets " + read(server, -12, up, z + 11) + " / " + read(server, 4, up, z + 11);
		check(grind.contains("grinding") && grind.contains("2.0 % U3O8") && !grind.contains("held to SX"), "uranium ore should grind at the full rate: " + chain);
		check(leach.contains("leaching"), "the leach tank should leach: " + chain);
		check(thick.contains("washing") && thick.contains("pregnant liquor"), "the thickener should make pregnant liquor: " + chain);
		check(sxr.contains("strip liquor"), "the SX circuit should make strip liquor: " + chain);
		check(pt.contains("precipitating"), "the precipitation tank should precipitate: " + chain);
		check(dry.contains("drying"), "the dryer should dry yellowcake: " + chain);
		server.computeOnServer(srv -> {   // 380 kg already in the filler: the mill's own yellowcake finishes the drum
			((dev.wildcatter.block.DrumFillerBlockEntity) srv.overworld().getBlockEntity(filler)).addYellowcake(380);
			return null;
		});
		int drums = 0;
		for (int i = 0; i < 30 && drums == 0; i++) {
			context.waitTicks(50);
			water(server, sag, ccd);
			drums = countIn(server, under, WildcatterItems.YELLOWCAKE_DRUM);
		}
		check(drums > 0, "a full yellowcake drum should drop into the hopper under the filler: " + read(server, 56, up, z) + " | dryer " + read(server, 53, up, z)
				+ " | precipitation " + read(server, 49, up, z));
		server.runCommand("tp @p 26 " + (up + 16) + " " + (z + 24) + " 195 30");
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-uranium-mill");
		System.out.println("[wildcatter-test] uranium mill ok: " + held + " | " + grind + " | " + thick + " | " + sxr + " | " + dry + " | drums " + drums);
	}


	/**
	 * The ISR wellfield (7c, docs/multiblocks/isr-wellfield.md), far out in a cell of its own on a seed that puts a roll-front
	 * there, the ground turned to desert. The small rig drills the producer's hole on the front's middle line and must log the ore
	 * and leave an ISR well; its set and cable then go. Four injectors stand at the corners of a 5-spot round it, on an injection ring
	 * from the header house; the producer's line runs to the header house's west side. The oxygen generator stands against the
	 * header house's east side, the IX columns south of it, acid from a stock tank, the eluate to a precipitation tank with its
	 * dryer and filler. The producer must read four injectors and a head grade near 500 mg/L; the IX columns must elute and the tank
	 * precipitate the eluate; with the oxygen generator gone the head grade must fall.
	 */
	private void isrWellfield(ClientGameTestContext context, TestServerContext server) {
		int y0 = up + 31;   // the working level: high enough that the front lies under a hole drilled from it
		long found = -1;
		dev.wildcatter.well.RollFront front = null;
		for (long seed = 1; seed < 2_000_000 && found < 0; seed++) {
			dev.wildcatter.well.RollFront f = dev.wildcatter.well.RollFront.inCell(seed, 4, -10);
			if (f == null || f.topY() > y0 - 8 || f.bottomY() < -60) {
				continue;
			}
			int[] m = f.middle();
			boolean all = true;
			for (int[] c : new int[][] {{0, 0}, {-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) {
				all &= f.covers(m[0] + c[0], m[1] + c[1]);
				Reservoir field = Reservoir.at(seed, m[0] + c[0], m[1] + c[1]);
				all &= field == null || field.topY() < f.bottomY();
			}
			if (all) {
				found = seed;
				front = f;
			}
		}
		check(found >= 0, "no seed puts a roll-front under the wellfield");
		ReservoirData.testSeed = found;
		final int px = front.middle()[0], pz = front.middle()[1];
		try {
			server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
			server.runCommand("tp @p " + (px + 10) + " " + (y0 + 20) + " " + (pz + 30) + " 180 40");
			context.waitTicks(40);   // the chunks load
			server.runCommand("fill " + (px - 12) + " " + (y0 - 1) + " " + (pz - 11) + " " + (px + 34) + " " + (y0 + 8) + " " + (pz + 28) + " minecraft:air");
			server.runCommand("fill " + (px - 12) + " " + (y0 - 1) + " " + (pz - 11) + " " + (px + 34) + " " + (y0 - 1) + " " + (pz + 28) + " minecraft:stone");
			// desert under the 5-spot (fillbiome's limit is 32,768 blocks: the wells' columns are enough)
			server.runCommand("fillbiome " + (px - 6) + " -60 " + (pz - 6) + " " + (px + 6) + " " + (y0 + 2) + " " + (pz + 6) + " minecraft:desert");
			check(server.computeOnServer(srv -> srv.overworld().getBiome(new BlockPos(px, y0, pz)).is(net.minecraft.world.level.biome.Biomes.DESERT)),
					"fillbiome should have turned the 5-spot to desert");
			net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
			// ---- the producer's hole: the small rig on its own set, as in the wells scene ----
			BlockPos hole = new BlockPos(px, y0, pz);
			BlockPos rigSet = new BlockPos(px - 6, y0, pz + 2);
			genset(server, rigSet, net.minecraft.core.Direction.WEST, false);
			put(server, px - 6, y0, pz + 1, "wildcatter:cable_industrial");
			put(server, px - 6, y0, pz, "wildcatter:cable_industrial");
			placeBig(server, WildcatterBlocks.DRILLING_RIG, hole, net.minecraft.core.Direction.EAST);
			startGenset(context, server, rigSet, net.minecraft.core.Direction.WEST, 1000);
			for (int waited = 0; waited < 3000; waited += 100) {
				if (server.computeOnServer(srv -> srv.overworld().getBlockState(hole).is(WildcatterBlocks.ISR_WELLHEAD))) {
					break;
				}
				context.waitTicks(100);
			}
			String log = read(server, px, y0, pz);
			check(log.contains("GAMMA LOG") && log.contains("ORE"), "the rig should complete an ISR well through the front and log its ore: " + log + " | rig "
					+ read(server, px, y0, pz));
			// the rig's set and cable go; the floor goes back
			server.runCommand("kill @e[type=item]");
			server.runCommand("fill " + (px - 9) + " " + (y0 - 1) + " " + (pz) + " " + (px - 3) + " " + (y0 + 4) + " " + (pz + 10) + " minecraft:air");
			server.runCommand("fill " + (px - 9) + " " + (y0 - 1) + " " + (pz) + " " + (px - 3) + " " + (y0 - 1) + " " + (pz + 10) + " minecraft:stone");
			// ---- the 5-spot: four injectors at the corners, cased; a pump into the producer ----
			for (int[] c : new int[][] {{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) {
				put(server, px + c[0], y0 - 1, pz + c[1], "wildcatter:casing");
				put(server, px + c[0], y0, pz + c[1], "wildcatter:isr_wellhead");
			}
			server.computeOnServer(srv -> {
				net.minecraft.world.level.block.state.BlockState st = srv.overworld().getBlockState(hole);
				srv.overworld().setBlock(hole, st.setValue(dev.wildcatter.block.IsrWellheadBlock.PUMP, true), 3);
				return null;
			});
			// ---- the surface plant ----
			placeBig(server, WildcatterBlocks.HEADER_HOUSE, new BlockPos(px + 12, y0, pz - 5), north);          // x+12..15, z-5..z+4
			BlockPos oxygen = new BlockPos(px + 21, y0, pz - 2);
			placeBig(server, WildcatterBlocks.OXYGEN_GENERATOR, oxygen, net.minecraft.core.Direction.EAST);    // x+16..21, z-2..z
			placeBig(server, WildcatterBlocks.IX_COLUMNS, new BlockPos(px + 10, y0, pz + 7), north);            // x+10..17, z+7..z+10
			BlockPos acidTank = new BlockPos(px + 19, y0, pz + 7);
			placeBig(server, WildcatterBlocks.STOCK_TANK, acidTank, north);                                     // x+19..22, z+7..z+10
			placeBig(server, WildcatterBlocks.PRECIPITATION_TANK, new BlockPos(px + 10, y0, pz + 12), north);   // x+10..13, z+12..z+15
			placeBig(server, WildcatterBlocks.YELLOWCAKE_DRYER, new BlockPos(px + 10, y0, pz + 16), north);     // x+10..12, z+16..z+18
			placeBig(server, WildcatterBlocks.DRUM_FILLER, new BlockPos(px + 8, y0, pz + 16), north);           // x+8..9, z+16..z+17
			BlockPos bullet = new BlockPos(px + 14, y0, pz + 16);
			placeBig(server, WildcatterBlocks.LPG_BULLET, bullet, north);                                       // x+14..16, z+16..z+27
			// the producer's line east to the header house's west side
			for (int x = px + 1; x <= px + 11; x++) {
				put(server, x, y0, pz, "wildcatter:gas_line");
			}
			// the injection ring: out of the header house's east side, round the pattern, a stub to each injector
			java.util.List<int[]> ring = new java.util.ArrayList<>();
			for (int z = pz - 7; z <= pz - 5; z++) {
				ring.add(new int[] {px + 16, z});
			}
			for (int x = px - 9; x <= px + 15; x++) {
				ring.add(new int[] {x, pz - 7});
			}
			for (int z = pz - 6; z <= pz + 7; z++) {
				ring.add(new int[] {px - 9, z});
			}
			for (int x = px - 8; x <= px + 5; x++) {
				ring.add(new int[] {x, pz + 7});
			}
			for (int[] c : new int[][] {{px - 5, pz - 6}, {px + 5, pz - 6}, {px - 5, pz + 6}, {px + 5, pz + 6}}) {
				ring.add(c);
			}
			for (int[] c : ring) {
				put(server, c[0], y0, c[1], "wildcatter:gas_line");
			}
			// to and from the IX columns
			put(server, px + 12, y0, pz + 5, "wildcatter:gas_line");
			put(server, px + 12, y0, pz + 6, "wildcatter:gas_line");
			int[][] back = {{px + 16, pz + 11}, {px + 16, pz + 12}};
			for (int[] c : back) {
				put(server, c[0], y0, c[1], "wildcatter:gas_line");
			}
			for (int x = px + 17; x <= px + 24; x++) {
				put(server, x, y0, pz + 12, "wildcatter:gas_line");
			}
			for (int z = pz + 5; z <= pz + 11; z++) {
				put(server, px + 24, y0, z, "wildcatter:gas_line");
			}
			for (int x = px + 15; x <= px + 23; x++) {
				put(server, x, y0, pz + 5, "wildcatter:gas_line");
			}
			put(server, px + 18, y0, pz + 8, "wildcatter:gas_line");   // acid, the stock tank to the IX columns' east side
			put(server, px + 11, y0, pz + 11, "wildcatter:gas_line");  // eluate to the precipitation tank
			put(server, px + 13, y0, pz + 17, "wildcatter:gas_line");  // LPG to the dryer
			// hoppers: salt on the IX columns, magnesia on the precipitation tank, drums on the filler and one under it
			put(server, px + 12, y0 + 6, pz + 8, "minecraft:hopper");
			server.runCommand("item replace block " + (px + 12) + " " + (y0 + 6) + " " + (pz + 8) + " container.0 with wildcatter:halite 2");
			put(server, px + 11, y0 + 5, pz + 13, "minecraft:hopper");
			server.runCommand("item replace block " + (px + 11) + " " + (y0 + 5) + " " + (pz + 13) + " container.0 with wildcatter:magnesia 2");
			put(server, px + 8, y0 + 4, pz + 16, "minecraft:hopper");
			server.runCommand("item replace block " + (px + 8) + " " + (y0 + 4) + " " + (pz + 16) + " container.0 with wildcatter:empty_drum 2");
			put(server, px + 8, y0 - 1, pz + 16, "minecraft:hopper");
			fillTank(server, acidTank, dev.wildcatter.fluid.Product.SULFURIC_ACID, 20_000);
			fillTank(server, bullet, dev.wildcatter.fluid.Product.LPG, 20_000);
			// ---- 480 V: a set north-east; its line along z-10, over the injection ring to the header house's box, down to the oxygen
			// generator's box, and round the west to the precipitation tank and the filler ----
			BlockPos set = new BlockPos(px + 32, y0, pz - 9);
			genset(server, set, north, false);                                                                  // x+27..32, z-9..z-8
			java.util.List<int[]> cable = new java.util.ArrayList<>();
			cable.add(new int[] {px + 33, 0, pz - 9});
			for (int x = px - 11; x <= px + 33; x++) {
				cable.add(new int[] {x, 0, pz - 10});
			}
			for (int[] c : new int[][] {{px + 12, 0, pz - 9}, {px + 12, 0, pz - 8}, {px + 12, 1, pz - 8}, {px + 12, 1, pz - 7}, {px + 12, 1, pz - 6}, {px + 12, 0, pz - 6}}) {
				cable.add(c);
			}
			for (int z = pz - 9; z <= pz - 3; z++) {
				cable.add(new int[] {px + 22, 0, z});
			}
			cable.add(new int[] {px + 21, 0, pz - 3});
			for (int z = pz - 9; z <= pz + 12; z++) {
				cable.add(new int[] {px - 11, 0, z});
			}
			for (int x = px - 10; x <= px + 9; x++) {
				cable.add(new int[] {x, 0, pz + 12});
			}
			for (int z = pz + 13; z <= pz + 16; z++) {
				cable.add(new int[] {px + 7, 0, z});
			}
			for (int[] c : cable) {
				put(server, c[0], y0 + c[1], c[2], "wildcatter:cable_industrial");
			}
			startGenset(context, server, set, north, 2000);
			context.waitTicks(400);
			String producer = read(server, px, y0, pz);
			String chain = " | header house " + read(server, px + 12, y0, pz - 5) + " | oxygen " + read(server, px + 21, y0, pz - 2) + " | IX " + read(server, px + 10, y0, pz + 7)
					+ " | injector " + read(server, px - 5, y0, pz - 5) + " | precipitation " + read(server, px + 10, y0, pz + 12) + " | set " + read(server, px + 32, y0, pz - 9);
			double grade = server.computeOnServer(srv -> ((dev.wildcatter.block.IsrWellheadBlockEntity) srv.overworld().getBlockEntity(hole)).headGrade());
			int injectors = server.computeOnServer(srv -> ((dev.wildcatter.block.IsrWellheadBlockEntity) srv.overworld().getBlockEntity(hole)).injectors());
			check(injectors == 4 && grade > 400, "the producer should see four injectors and about 500 mg/L: " + producer + chain);
			String ix = read(server, px + 10, y0, pz + 7), pt = read(server, px + 10, y0, pz + 12);
			check(ix.contains("eluting"), "the IX columns should elute: " + ix + chain);
			check(pt.contains("precipitating") && pt.contains("eluate"), "the precipitation tank should precipitate the eluate: " + pt + chain);
			server.runCommand("tp @p " + (px + 6) + " " + (y0 + 18) + " " + (pz + 34) + " 190 35");
			context.waitTicks(3);
			context.takeScreenshot("wildcatter-isr-wellfield");
			// no oxygen: the lixiviant reaches the injectors without it, and the head grade falls away
			server.runCommand("fill " + (px + 16) + " " + y0 + " " + (pz - 2) + " " + (px + 21) + " " + (y0 + 2) + " " + pz + " minecraft:air");
			context.waitTicks(400);
			double without = server.computeOnServer(srv -> ((dev.wildcatter.block.IsrWellheadBlockEntity) srv.overworld().getBlockEntity(hole)).headGrade());
			String hh = read(server, px + 12, y0, pz - 5);
			check(without < grade / 4 && hh.contains("NO OXYGEN"), "without oxygen the head grade should fall: " + grade + " -> " + without + " | " + hh);
			System.out.println("[wildcatter-test] isr wellfield ok: " + log + " | " + producer + " | " + ix + " | " + pt + " | no oxygen: " + Math.round(grade) + " -> "
					+ Math.round(without) + " mg/L");
		} finally {
			ReservoirData.testSeed = null;
		}
	}


	/**
	 * The ammonia plant (Phase 14, docs/multiblocks/ammonia-plant.md), the whole chain at true size, every part facing north. North
	 * end: a steam reformer on LPG from a bullet and a nitrogen generator feed the syngas compressor, with the ammonia converter against
	 * its south end; the converter's liquid ammonia runs into a pressure bullet. Off that bullet's west side: the nitric acid unit (its
	 * gauze packs in a hopper on top) and the AN neutraliser, the acid running from the one to the other; the neutraliser's solution to
	 * the emulsion blender (diesel from a stock tank, emulsifier pails in a hopper on top) and the matrix into its own stock tank. One
	 * 500 kW diesel set on the industrial cable to every part's west box. The ammonia bullet starts with 2,000 L, as if the loop had
	 * been running a few days (the loop's 25 L/h would take an hour to show), and the diesel tank with 5,000 L. Each stage must run at
	 * the spec's rate; then the hydrogen pipe is cut and the compressor reads NO HYDROGEN, and once its gas is used the converter
	 * NO SYNGAS. Screenshots of each readout and of the plant.
	 */
	private void ammoniaPlant(ClientGameTestContext context, TestServerContext server) {
		int z = -8000;
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand("tp @p 8 " + (up + 30) + " " + (z + 50) + " 180 40");
		context.waitTicks(20);
		for (int zz = z - 16; zz <= z + 42; zz += 20) {   // /fill takes at most 32,768 blocks
			server.runCommand("fill -16 " + (y - 1) + " " + zz + " 34 " + (y + 15) + " " + Math.min(zz + 19, z + 42) + " minecraft:air");
		}
		server.runCommand("fill -16 " + (y - 1) + " " + (z - 16) + " 34 " + (y - 1) + " " + (z + 42) + " minecraft:stone");
		Filming film = null;
		try {
			film = context.computeOnClient(client -> Filming.start(client, "ammoniaPlant"));
			// the ammonia loop
			placeBig(server, WildcatterBlocks.STEAM_REFORMER, new BlockPos(13, up, z), north);           // x 13-15, z..z+3
			BlockPos lpg = new BlockPos(17, up, z - 10);
			placeBig(server, WildcatterBlocks.LPG_BULLET, lpg, north);                                   // x 17-19, z-10..z+1
			placeBig(server, WildcatterBlocks.NITROGEN_GENERATOR, new BlockPos(13, up, z + 7), north);   // x 13-14, z+7..z+9
			placeBig(server, WildcatterBlocks.SYNGAS_COMPRESSOR, new BlockPos(8, up, z + 6), north);     // x 8-10, z+6..z+10
			placeBig(server, WildcatterBlocks.AMMONIA_CONVERTER, new BlockPos(8, up, z + 11), north);    // x 8-10, z+11..z+13, against it
			BlockPos nh3 = new BlockPos(8, up, z + 15);
			placeBig(server, WildcatterBlocks.LPG_BULLET, nh3, north);                                   // x 8-10, z+15..z+26: the ammonia
			// acid, AN and matrix
			placeBig(server, WildcatterBlocks.NITRIC_ACID_UNIT, new BlockPos(3, up, z + 16), north);     // x 3-6, z+16..z+19
			placeBig(server, WildcatterBlocks.AN_NEUTRALISER, new BlockPos(0, up, z + 22), north);       // x 0-2, z+22..z+24
			placeBig(server, WildcatterBlocks.EMULSION_BLENDER, new BlockPos(-6, up, z + 28), north);    // x -6..-4, z+28..z+31
			BlockPos diesel = new BlockPos(-2, up, z + 31);
			placeBig(server, WildcatterBlocks.STOCK_TANK, diesel, north);                                // x -2..1, z+31..z+34
			BlockPos matrixTank = new BlockPos(-7, up, z + 34);
			placeBig(server, WildcatterBlocks.STOCK_TANK, matrixTank, north);                            // x -7..-4, z+34..z+37
			// steel pipe
			pipes(server, z, new int[][] {{16, 0, 0}});                                                  // LPG to the reformer's east side
			pipes(server, z, new int[][] {{14, 0, 4}, {14, 0, 5}, {13, 0, 5}, {12, 0, 5}, {11, 0, 5}, {11, 0, 6}});   // hydrogen
			pipes(server, z, new int[][] {{13, 0, 10}, {12, 0, 10}, {11, 0, 10}});                        // nitrogen
			pipes(server, z, new int[][] {{9, 0, 14}});                                                  // liquid ammonia to its bullet
			pipes(server, z, new int[][] {{7, 0, 16}});                                                  // ammonia to the acid unit's east side
			pipes(server, z, new int[][] {{7, 0, 24}, {6, 0, 24}, {5, 0, 24}, {4, 0, 24}, {3, 0, 24}});  // ammonia to the neutraliser
			pipes(server, z, new int[][] {{4, 0, 20}, {4, 0, 21}, {4, 0, 22}, {3, 0, 22}});              // 60 % acid to the neutraliser
			pipes(server, z, new int[][] {{1, 0, 25}, {1, 0, 26}, {1, 0, 27}, {0, 0, 27}, {-1, 0, 27}, {-2, 0, 27}, {-3, 0, 27}, {-3, 0, 28}});   // AN solution
			pipes(server, z, new int[][] {{-3, 0, 31}});                                                 // diesel
			pipes(server, z, new int[][] {{-5, 0, 32}, {-5, 0, 33}});                                    // matrix to its tank
			// hoppers: gauze packs on the acid unit, emulsifier pails on the blender
			put(server, 4, up + 12, z + 17, "minecraft:hopper");
			server.runCommand("item replace block 4 " + (up + 12) + " " + (z + 17) + " container.0 with wildcatter:gauze_pack 2");
			put(server, -5, up + 3, z + 29, "minecraft:hopper");
			server.runCommand("item replace block -5 " + (up + 3) + " " + (z + 29) + " container.0 with wildcatter:emulsifier_pail 4");
			// the industrial cable: a diesel set east, north round the LPG bullet, a spine down x 7 past the compressor's and
			// converter's boxes, a branch over the hydrogen pipe to the nitrogen generator, and one west and south to the acid unit and the blender
			BlockPos set = new BlockPos(30, up, z - 3);
			genset(server, set, north, false);                                                           // x 25-30, z-3..z-2
			java.util.List<int[]> cable = new java.util.ArrayList<>();
			for (int zz = -12; zz <= -3; zz++) {
				cable.add(new int[] {31, 0, zz});
			}
			for (int x = 7; x <= 30; x++) {
				cable.add(new int[] {x, 0, -12});
			}
			for (int zz = -11; zz <= 11; zz++) {
				cable.add(new int[] {7, 0, zz});
			}
			for (int x = 8; x <= 12; x++) {
				cable.add(new int[] {x, 0, -1});
			}
			for (int zz = 0; zz <= 4; zz++) {
				cable.add(new int[] {12, 0, zz});
			}
			for (int[] c : new int[][] {{12, 1, 4}, {12, 1, 5}, {12, 1, 6}, {12, 0, 6}, {12, 0, 7}}) {    // over the hydrogen pipe
				cable.add(c);
			}
			for (int x = 2; x <= 6; x++) {
				cable.add(new int[] {x, 0, 11});
			}
			for (int zz = 12; zz <= 16; zz++) {
				cable.add(new int[] {2, 0, zz});                                                         // the acid unit's box
			}
			for (int x = -7; x <= 1; x++) {
				cable.add(new int[] {x, 0, 12});
			}
			for (int zz = 13; zz <= 28; zz++) {
				cable.add(new int[] {-7, 0, zz});                                                        // the blender's box
			}
			for (int[] c : cable) {
				put(server, c[0], up + c[1], z + c[2], "wildcatter:cable_industrial");
			}
			fillTank(server, lpg, dev.wildcatter.fluid.Product.LPG, 20_000);
			fillTank(server, nh3, dev.wildcatter.fluid.Product.AMMONIA, 2_000);
			fillTank(server, diesel, dev.wildcatter.fluid.Product.DIESEL, 5_000);
			BlockPos reformer = new BlockPos(13, up, z);
			BlockPos acidUnit = new BlockPos(3, up, z + 16);
			BlockPos blender = new BlockPos(-6, up, z + 28);
			water(server, reformer, acidUnit, blender);
			startGenset(context, server, set, north, 2000);
			for (int i = 0; i < 24; i++) {   // 60 s: every stage up to its rate, and matrix in its tank
				context.waitTicks(50);
				water(server, reformer, acidUnit, blender);
			}
			String chain = " | reformer " + read(server, 13, up, z) + " | n2 " + read(server, 13, up, z + 7) + " | compressor " + read(server, 8, up, z + 6)
					+ " | converter " + read(server, 8, up, z + 11) + " | bullet " + read(server, 8, up, z + 15) + " | acid unit " + read(server, 3, up, z + 16)
					+ " | neutraliser " + read(server, 0, up, z + 22) + " | blender " + read(server, -6, up, z + 28) + " | matrix " + read(server, -7, up, z + 34)
					+ " | set " + read(server, 30, up, z - 3);
			double[] rates = server.computeOnServer(srv -> new double[] {
					((dev.wildcatter.block.AmmoniaConverterBlockEntity) srv.overworld().getBlockEntity(new BlockPos(8, up, z + 11))).making(),
					((dev.wildcatter.block.NitricAcidUnitBlockEntity) srv.overworld().getBlockEntity(acidUnit)).making(),
					((dev.wildcatter.block.AnNeutraliserBlockEntity) srv.overworld().getBlockEntity(new BlockPos(0, up, z + 22))).making(),
					((dev.wildcatter.block.EmulsionBlenderBlockEntity) srv.overworld().getBlockEntity(blender)).making(),
					((dev.wildcatter.block.StorageTankBlockEntity) srv.overworld().getBlockEntity(matrixTank)).amount()});
			check(read(server, 13, up, z).contains("reforming"), "the reformer should make hydrogen: " + chain);
			check(Math.abs(rates[0] - dev.wildcatter.fluid.AmmoniaPlant.ammoniaKgh()) < 1.5, "the converter should make about 15.2 kg/h of ammonia: " + rates[0] + chain);
			check(Math.abs(rates[1] - dev.wildcatter.fluid.AmmoniaPlant.hno3Kgh()) < 2.7, "the acid unit should make about 27 kg/h of HNO3: " + rates[1] + chain);
			check(Math.abs(rates[2] - dev.wildcatter.fluid.AmmoniaPlant.anKgh()) < 5, "the neutraliser should make about 34.6 kg/h of AN: " + rates[2] + chain);
			check(Math.abs(rates[3] - dev.wildcatter.fluid.AmmoniaPlant.matrixKgh()) < 7, "the blender should make about 46 kg/h of matrix: " + rates[3] + chain);
			check(rates[4] > 0.05, "emulsion matrix should be in its tank: " + rates[4] + chain);
			System.out.println("[wildcatter-test] ammoniaPlant running" + chain);
			server.runCommand("tp @p 22 " + (up + 22) + " " + (z + 46) + " 155 35");
			context.waitTicks(5);
			context.takeScreenshot("wildcatter-ammonia-plant");
			showReadout(context, server, new BlockPos(8, up, z + 11), "tp @p 4 " + (up + 3) + " " + (z + 12) + " -90 10", "wildcatter-ammonia-converter");
			showReadout(context, server, acidUnit, "tp @p 0 " + (up + 3) + " " + (z + 13) + " -135 10", "wildcatter-ammonia-acid-unit");
			showReadout(context, server, new BlockPos(0, up, z + 22), "tp @p -3 " + (up + 3) + " " + (z + 20) + " -120 15", "wildcatter-ammonia-neutraliser");
			showReadout(context, server, blender, "tp @p -10 " + (up + 3) + " " + (z + 26) + " -135 15", "wildcatter-ammonia-blender");
			showReadout(context, server, matrixTank, "tp @p -10 " + (up + 3) + " " + (z + 40) + " -135 15", "wildcatter-ammonia-matrix-tank");
			// the hydrogen pipe cut: the compressor stops at once, the converter when its gas is used up
			put(server, 14, up, z + 4, "minecraft:air");
			context.waitTicks(40);
			String noH2 = read(server, 8, up, z + 6);
			check(noH2.contains("NO HYDROGEN"), "without hydrogen the compressor should stop: " + noH2);
			showReadout(context, server, new BlockPos(8, up, z + 6), "tp @p 4 " + (up + 3) + " " + (z + 4) + " -120 15", "wildcatter-ammonia-no-hydrogen");
			String starved = "";
			for (int i = 0; i < 16 && !starved.contains("NO SYNGAS"); i++) {   // the compressor holds 20 s of gas
				context.waitTicks(50);
				water(server, reformer, acidUnit, blender);
				starved = read(server, 8, up, z + 11);
			}
			check(starved.contains("NO SYNGAS"), "once the compressor's gas is used the converter should stop: " + starved);
			showReadout(context, server, new BlockPos(8, up, z + 11), "tp @p 4 " + (up + 3) + " " + (z + 12) + " -90 10", "wildcatter-ammonia-no-syngas");
			System.out.println("[wildcatter-test] ammoniaPlant ok: " + noH2 + " | " + starved);
		} finally {
			if (film != null) {
				film.stop();
			}
		}
	}

	/**
	 * Phase 16a, ADU yellowcake (docs/multiblocks/adu-yellowcake.md), filmed. Strip liquor (a stand-in source, as the mill's SX would
	 * make it) and liquid ammonia from a pressure bullet to the precipitation tank; the uranium calciner against it fires up on LPG from
	 * a second bullet and calcines the ADU into the drum filler, which fills a U3O8 drum (96 %). The filler starts near full (a hook), and
	 * the calciner's five minutes of firing up are cut short by hook once FIRING UP has shown. Then the ammonia is cut: NO AMMONIA.
	 * One 500 kW diesel set; the parts' boxes: the tank and filler west, the calciner north.
	 */
	private void aduYellowcake(ClientGameTestContext context, TestServerContext server) {
		int z = -9300;
		int x0 = 0;
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand("tp @p 18 " + (up + 14) + " " + (z + 22) + " 150 30");
		context.waitTicks(20);
		server.runCommand("fill -12 " + (y - 1) + " " + (z - 10) + " 26 " + (y + 14) + " " + (z + 20) + " minecraft:air");
		server.runCommand("fill -12 " + (y - 1) + " " + (z - 10) + " 26 " + (y - 1) + " " + (z + 20) + " minecraft:stone");
		Filming film = null;
		try {
			film = context.computeOnClient(client -> Filming.start(client, "aduYellowcake"));
			BlockPos tank = new BlockPos(x0, up, z);
			placeBig(server, WildcatterBlocks.PRECIPITATION_TANK, tank, north);                         // x 0-3, z..z+3
			BlockPos calciner = new BlockPos(x0 + 4, up, z);
			placeBig(server, WildcatterBlocks.URANIUM_CALCINER, calciner, north);                       // x 4-6, z..z+2, against the tank
			BlockPos filler = new BlockPos(x0 + 7, up, z - 1);
			placeBig(server, WildcatterBlocks.DRUM_FILLER, filler, north);                              // x 7-8, z-1..z, against the calciner
			BlockPos nh3 = new BlockPos(x0 + 3, up, z + 6);
			placeBig(server, WildcatterBlocks.LPG_BULLET, nh3, north);                                  // x 3-5, z+6..z+17: the ammonia
			BlockPos lpg = new BlockPos(x0 + 7, up, z + 6);
			placeBig(server, WildcatterBlocks.LPG_BULLET, lpg, north);                                  // x 7-9, z+6..z+17: the calciner's LPG
			pipes(server, z, new int[][] {{1, 0, -1}, {1, -1, -1}});                                     // strip liquor in, the tank's front, from the floor
			pipes(server, z, new int[][] {{4, 0, 3}, {4, 0, 4}, {4, 0, 5}});                             // ammonia to the tank's east side
			pipes(server, z, new int[][] {{7, 0, 2}, {7, 0, 3}, {7, 0, 4}, {7, 0, 5}});                  // LPG to the calciner's burners
			server.computeOnServer(srv -> {
				srv.overworld().setBlock(new BlockPos(1, up - 2, z - 1), TestGasSource.BLOCK.defaultBlockState(), 3);   // in the floor, out of sight
				TestGasSource.SourceEntity s = (TestGasSource.SourceEntity) srv.overworld().getBlockEntity(new BlockPos(1, up - 2, z - 1));
				s.product = dev.wildcatter.fluid.Product.STRIP_LIQUOR;
				s.rate = 20.0;
				s.mix[0] = Tuning.SX_STRIP_U3O8_G_PER_L / 1000;
				return null;
			});
			fillTank(server, nh3, dev.wildcatter.fluid.Product.AMMONIA, 5_000);
			fillTank(server, lpg, dev.wildcatter.fluid.Product.LPG, 20_000);
			BlockPos set = new BlockPos(-4, up, z - 6);
			genset(server, set, north, false);                                                          // x -9..-4, z-6..z-5
			java.util.List<int[]> cable = new java.util.ArrayList<>();
			for (int x = -3; x <= 6; x++) {
				cable.add(new int[] {x, 0, -6});
			}
			for (int zz = -5; zz <= 0; zz++) {
				cable.add(new int[] {-1, 0, zz});                                                        // the tank's box
			}
			for (int zz = -5; zz <= -1; zz++) {
				cable.add(new int[] {4, 0, zz});                                                         // the calciner's box, north
				cable.add(new int[] {6, 0, zz});                                                         // the filler's box
			}
			for (int[] c : cable) {
				put(server, c[0], up + c[1], z + c[2], "wildcatter:cable_industrial");
			}
			put(server, 7, up + 4, z - 1, "minecraft:hopper");                                          // empty drums on the filler
			server.runCommand("item replace block 7 " + (up + 4) + " " + (z - 1) + " container.0 with wildcatter:empty_drum 4");
			server.computeOnServer(srv -> {
				((dev.wildcatter.block.DrumFillerBlockEntity) srv.overworld().getBlockEntity(filler)).addU3o8(Tuning.DRUM_NET_KG - 2);   // near full
				return null;
			});
			server.runCommand("tp @p 18 " + (up + 14) + " " + (z + 22) + " 150 30");
			context.waitTicks(20);
			context.takeScreenshot("wildcatter-adu-line");
			startGenset(context, server, set, north, 2000);
			context.waitTicks(100);
			String firing = read(server, 4, up, z);
			String precip = read(server, 0, up, z);
			check(precip.contains("precipitating ADU"), "strip liquor and ammonia should make ADU: " + precip);
			check(firing.contains("firing up"), "the calciner should be firing up: " + firing);
			showReadout(context, server, tank, "tp @p -4 " + (up + 4) + " " + (z + 6) + " -140 20", "wildcatter-adu-tank");
			showReadout(context, server, calciner, "tp @p 9 " + (up + 6) + " " + (z - 10) + " 25 25", "wildcatter-adu-calciner-firing");
			server.computeOnServer(srv -> {
				((dev.wildcatter.block.UraniumCalcinerBlockEntity) srv.overworld().getBlockEntity(calciner)).setWarmForTest();   // its five minutes
				return null;
			});
			String filled = "";
			for (int i = 0; i < 10 && !filled.contains("1 full drums"); i++) {
				context.waitTicks(40);
				filled = read(server, 7, up, z - 1);
			}
			String calcining = read(server, 4, up, z);
			check(calcining.contains("calcining"), "at temperature the calciner should calcine: " + calcining);
			check(filled.contains("1 full drums"), "the filler should fill a U3O8 drum: " + filled);
			showReadout(context, server, calciner, "tp @p 9 " + (up + 6) + " " + (z - 10) + " 25 25", "wildcatter-adu-calcining");
			showReadout(context, server, filler, "tp @p 11 " + (up + 3) + " " + (z - 4) + " 45 15", "wildcatter-adu-drum");
			server.runCommand("tp @p 16 " + (up + 9) + " " + (z + 12) + " 140 25");
			context.waitTicks(10);
			context.takeScreenshot("wildcatter-adu-calciner");
			System.out.println("[wildcatter-test] aduYellowcake running | " + precip + " | " + calcining + " | " + filled);
			put(server, 4, up, z + 4, "minecraft:air");                                                  // the ammonia cut
			String noNh3 = "";
			for (int i = 0; i < 10 && !noNh3.contains("NO AMMONIA"); i++) {
				context.waitTicks(20);
				noNh3 = read(server, 0, up, z);
			}
			check(noNh3.contains("NO AMMONIA"), "without ammonia the tank should read NO AMMONIA: " + noNh3);
			showReadout(context, server, tank, "tp @p -4 " + (up + 4) + " " + (z + 6) + " -140 20", "wildcatter-adu-no-ammonia");
			System.out.println("[wildcatter-test] aduYellowcake ok: " + noNh3);
		} finally {
			if (film != null) {
				film.stop();
			}
		}
	}

	/**
	 * The phosphoric acid and fertilizer plant (Phase 18a, docs/multiblocks/fertilizer.md), filmed, every part facing north. A hopper of
	 * phosphate rock on the phosphate mill (4.16 kV), its rock slurry to the phosphoric reactor; sulfuric acid from a stock tank; the
	 * tilting pan filter against the reactor's east side (4.16 kV) sends its gypsum over the wall of a lined tailings cell and its filter
	 * acid to the evaporator, on process steam from a package boiler fired on LPG. The merchant acid and ammonia from a bullet go to the
	 * granulation plant on DAP, its dryer on LPG, a bagging station against its south end. Five diesel sets: two on 480 V for the
	 * plant, one for the boiler, two through backward unit substations for the mill and filter; the big motors started one at a time.
	 * The reactor must run near its rating, gypsum must reach the cell, merchant acid the granulator, and a DAP bag (brought near full by
	 * hook) must drop. Screenshots of the plant and each readout.
	 */
	private void phosphatePlant(ClientGameTestContext context, TestServerContext server) {
		int z = -10000;
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand("tp @p 6 " + (up + 34) + " " + (z + 48) + " 180 45");
		context.waitTicks(20);
		for (int zz = z - 26; zz <= z + 44; zz += 12) {   // /fill takes at most 32,768 blocks
			server.runCommand("fill -27 " + (y - 1) + " " + zz + " 37 " + (y + 14) + " " + Math.min(zz + 11, z + 44) + " minecraft:air");
		}
		server.runCommand("fill -27 " + (y - 1) + " " + (z - 26) + " 37 " + (y - 1) + " " + (z + 44) + " minecraft:stone");
		Filming film = null;
		try {
			BlockPos mill = new BlockPos(-6, up, z - 8);
			BlockPos reactor = new BlockPos(0, up, z);
			BlockPos filter = new BlockPos(8, up, z);
			BlockPos acidTank = new BlockPos(2, up, z + 9);
			BlockPos evaporator = new BlockPos(9, up, z + 11);
			BlockPos boiler = new BlockPos(15, up, z + 1);
			BlockPos lpg = new BlockPos(20, up, z + 1);
			BlockPos granulator = new BlockPos(9, up, z + 16);
			BlockPos bagger = new BlockPos(10, up, z + 28);
			BlockPos nh3 = new BlockPos(15, up, z + 18);
			BlockPos lpg2 = new BlockPos(15, up, z + 30);
			placeBig(server, WildcatterBlocks.PHOSPHATE_MILL, mill, north);              // x -6..-4, z-8..z-3
			placeBig(server, WildcatterBlocks.PHOSPHORIC_REACTOR, reactor, north);        // x 0-7, z..z+7
			placeBig(server, WildcatterBlocks.TILTING_PAN_FILTER, filter, north);         // x 8-13, z..z+9, against its east side
			placeBig(server, WildcatterBlocks.STOCK_TANK, acidTank, north);               // x 2-5, z+9..z+12
			placeBig(server, WildcatterBlocks.ACID_EVAPORATOR, evaporator, north);        // x 9-11, z+11..z+13
			placeBig(server, WildcatterBlocks.PACKAGE_BOILER, boiler, north);             // x 15-18, z+1..z+9
			placeBig(server, WildcatterBlocks.LPG_BULLET, lpg, north);                    // x 20-22, z+1..z+12
			placeBig(server, WildcatterBlocks.GRANULATION_PLANT, granulator, north);      // x 9-12, z+16..z+27
			placeBig(server, WildcatterBlocks.BAGGING_STATION, bagger, north);            // x 10-11, z+28..z+29
			placeBig(server, WildcatterBlocks.LPG_BULLET, nh3, north);                    // x 15-17, z+18..z+29: the ammonia
			placeBig(server, WildcatterBlocks.LPG_BULLET, lpg2, north);                   // x 15-17, z+30..z+41: the dryer's LPG
			// the lined tailings cell, x 0-6, z+16..z+22, its tower inside the north wall
			for (int x = 0; x <= 6; x++) {
				for (int zz = 16; zz <= 22; zz++) {
					put(server, x, up - 1, z + zz, "wildcatter:cell_liner");
					if (x == 0 || x == 6 || zz == 16 || zz == 22) {
						for (int yy = up; yy <= up + 2; yy++) {
							put(server, x, yy, z + zz, "wildcatter:cell_liner");
						}
					}
				}
			}
			put(server, 3, up, z + 17, "wildcatter:decant_tower");
			// steel pipe
			pipes(server, z, new int[][] {{-5, 0, -2}, {-4, 0, -2}, {-3, 0, -2}, {-2, 0, -2}, {-1, 0, -2}, {0, 0, -2}, {1, 0, -2}, {2, 0, -2}, {2, 0, -1}});   // rock slurry
			pipes(server, z, new int[][] {{4, 0, 8}});                                                                     // sulfuric acid
			pipes(server, z, new int[][] {{7, 0, 9}, {7, 0, 10}, {7, 0, 11}, {7, 0, 12}, {7, 0, 13}, {7, 0, 14}, {7, 0, 15}, {6, 0, 15}, {5, 0, 15}, {4, 0, 15},
					{3, 0, 15}, {3, 1, 15}, {3, 2, 15}, {3, 3, 15}, {3, 3, 16}, {3, 3, 17}, {3, 2, 17}, {3, 1, 17}});         // gypsum over the wall to the tower
			pipes(server, z, new int[][] {{10, 0, 10}});                                                                    // filter acid
			pipes(server, z, new int[][] {{12, 0, 12}, {13, 0, 12}, {14, 0, 12}, {15, 0, 12}, {16, 0, 12}, {16, 0, 11}, {16, 0, 10}});   // process steam
			pipes(server, z, new int[][] {{19, 0, 5}});                                                                     // LPG to the boiler
			pipes(server, z, new int[][] {{10, 0, 14}, {11, 0, 14}, {12, 0, 14}, {13, 0, 14}, {13, 0, 15}, {13, 0, 16}});   // merchant acid
			pipes(server, z, new int[][] {{13, 0, 18}, {14, 0, 18}});                                                       // ammonia
			pipes(server, z, new int[][] {{13, 0, 27}, {13, 0, 28}, {13, 0, 29}, {13, 0, 30}, {14, 0, 30}});                // LPG to the dryer
			put(server, 10, up + 4, z + 28, "minecraft:hopper");                                                           // empty bags on the bagging station
			server.runCommand("item replace block 10 " + (up + 4) + " " + (z + 28) + " container.0 with wildcatter:empty_bulk_bag 8");
			put(server, -5, up + 4, z - 7, "minecraft:hopper");                                                            // rock on the mill
			server.runCommand("item replace block -5 " + (up + 4) + " " + (z - 7) + " container.0 with wildcatter:phosphate 16");
			// power. 480 V: set A the reactor, set B the evaporator, granulator and bagging station, set E the boiler
			java.util.List<int[]> lv = new java.util.ArrayList<>();
			BlockPos setA = new BlockPos(-20, up, z - 4);
			genset(server, setA, north, false);
			for (int zz = -4; zz <= 0; zz++) {
				lv.add(new int[] {-19, zz});
			}
			for (int x = -18; x <= -1; x++) {
				lv.add(new int[] {x, 0});
			}
			BlockPos setB = new BlockPos(5, up, z + 34);
			genset(server, setB, north, false);
			for (int zz = 30; zz <= 34; zz++) {
				lv.add(new int[] {6, zz});
			}
			lv.add(new int[] {7, 30});
			for (int zz = 11; zz <= 30; zz++) {
				lv.add(new int[] {8, zz});
			}
			lv.add(new int[] {9, 30});
			lv.add(new int[] {9, 29});
			lv.add(new int[] {9, 28});
			BlockPos setE = new BlockPos(24, up, z - 6);
			genset(server, setE, north, false);
			for (int zz = -6; zz <= -2; zz++) {
				lv.add(new int[] {25, zz});
			}
			for (int x = 14; x <= 24; x++) {
				lv.add(new int[] {x, -2});
			}
			lv.add(new int[] {14, -1});
			lv.add(new int[] {14, 0});
			lv.add(new int[] {14, 1});
			for (int[] c : lv) {
				put(server, c[0], up, z + c[1], "wildcatter:cable_industrial");
			}
			sizeCables(server, new BlockPos(-27, up, z - 26), new BlockPos(37, up, z + 44), WildcatterBlocks.CABLE_INDUSTRIAL, dev.wildcatter.energy.ConductorSize.KCMIL_500);
			// 4.16 kV: set C the mill, set D the filter, each through a backward unit substation
			BlockPos setC = new BlockPos(-20, up, z - 14);
			genset(server, setC, north, false);
			BlockPos mvC = inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, setC, net.minecraft.core.Direction.EAST, false);
			for (int x = mvC.getX(); x <= -7; x++) {
				put(server, x, up, z - 14, "wildcatter:cable_mv");
			}
			for (int zz = -13; zz <= -8; zz++) {
				put(server, -7, up, z + zz, "wildcatter:cable_mv");
			}
			BlockPos setD = new BlockPos(-20, up, z - 24);
			genset(server, setD, north, false);
			BlockPos mvD = inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, setD, net.minecraft.core.Direction.EAST, false);
			for (int x = mvD.getX(); x <= 8; x++) {
				put(server, x, up, z - 24, "wildcatter:cable_mv");
			}
			for (int zz = -23; zz <= -1; zz++) {
				put(server, 8, up, z + zz, "wildcatter:cable_mv");
			}
			fillTank(server, acidTank, dev.wildcatter.fluid.Product.SULFURIC_ACID, 40_000);
			fillTank(server, lpg, dev.wildcatter.fluid.Product.LPG, 20_000);
			fillTank(server, lpg2, dev.wildcatter.fluid.Product.LPG, 20_000);
			fillTank(server, nh3, dev.wildcatter.fluid.Product.AMMONIA, 5_000);
			water(server, mill, filter, boiler);
			BlockPos[] staged = {evaporator, granulator, bagger};
			server.computeOnServer(srv -> {
				((dev.wildcatter.block.GranulationPlantBlockEntity) srv.overworld().getBlockEntity(granulator)).switchProduct();   // DAP
				for (BlockPos at : staged) {
					((dev.wildcatter.block.PlantUnitBlockEntity) srv.overworld().getBlockEntity(at)).cycleHoa(null);   // OFF until the sets are up
				}
				return null;
			});
			String overview = "tp @p 24 " + (up + 17) + " " + (z + 42) + " 140 28";
			server.runCommand("gamemode spectator @p");   // a still camera for the film: no falling
			server.runCommand(overview);
			context.waitTicks(20);
			film = context.computeOnClient(client -> Filming.start(client, "phosphatePlant"));   // filmed once built: the sets starting, then the plant
			for (BlockPos set : new BlockPos[] {setA, setB, setC, setD, setE}) {
				server.runCommand(overview);   // slow falling still sinks the camera: put it back for the film
				startGenset(context, server, set, north, 2000);
			}
			for (BlockPos at : staged) {   // one motor at a time on set B
				server.computeOnServer(srv -> {
					dev.wildcatter.block.PlantUnitBlockEntity unit = (dev.wildcatter.block.PlantUnitBlockEntity) srv.overworld().getBlockEntity(at);
					unit.cycleHoa(null);
					unit.cycleHoa(null);
					return null;
				});
				server.runCommand(overview);
				context.waitTicks(60);
				water(server, mill, filter, boiler);
			}
			for (int i = 0; i < 16; i++) {   // 40 s: every stage up to its rate; the camera put back each time, as slow falling sinks it
				server.runCommand(overview);
				context.waitTicks(50);
				water(server, mill, filter, boiler);
			}
			server.runCommand(overview);
			context.waitTicks(5);
			String chain = " | mill " + read(server, -6, up, z - 8) + " | reactor " + read(server, 0, up, z) + " | filter " + read(server, 8, up, z) + " | evaporator "
					+ read(server, 9, up, z + 11) + " | boiler " + read(server, 15, up, z + 1) + " | granulator " + read(server, 9, up, z + 16) + " | tower "
					+ server.computeOnServer(srv -> ((Inspectable) srv.overworld().getBlockEntity(new BlockPos(3, up, z + 17))).inspect().getString());
			double[] rates = server.computeOnServer(srv -> new double[] {
					((dev.wildcatter.block.PhosphoricReactorBlockEntity) srv.overworld().getBlockEntity(reactor)).making(),
					((dev.wildcatter.block.AcidEvaporatorBlockEntity) srv.overworld().getBlockEntity(evaporator)).making(),
					((dev.wildcatter.block.GranulationPlantBlockEntity) srv.overworld().getBlockEntity(granulator)).making(),
					((dev.wildcatter.block.DecantTowerBlockEntity) srv.overworld().getBlockEntity(new BlockPos(3, up, z + 17))).gypsumKg()});
			check(Math.abs(rates[0] - dev.wildcatter.fluid.Phosphate.p2o5Kgs() * 3600) < 0.15 * dev.wildcatter.fluid.Phosphate.p2o5Kgs() * 3600,
					"the reactor should make about 833 kg/h of P2O5: " + rates[0] + chain);
			check(rates[1] > 0, "the evaporator should make merchant acid: " + chain);
			check(rates[2] > 0, "the granulator should make DAP: " + chain);
			check(rates[3] > 0, "phosphogypsum should reach the cell: " + chain);
			System.out.println("[wildcatter-test] phosphatePlant running" + chain);
			context.takeScreenshot("wildcatter-phosphate-plant");
			server.computeOnServer(srv -> {
				((dev.wildcatter.block.GranulationPlantBlockEntity) srv.overworld().getBlockEntity(granulator)).addGranules(Tuning.BULK_BAG_KG - 5);
				return null;
			});
			server.runCommand("gamemode survival @p");
			showReadout(context, server, reactor, "tp @p -5 " + (up + 9) + " " + (z - 5) + " -45 35", "wildcatter-phosphate-reactor");
			showReadout(context, server, filter, "tp @p 18 " + (up + 8) + " " + (z - 4) + " 45 35", "wildcatter-phosphate-filter");
			showReadout(context, server, evaporator, "tp @p 13 " + (up + 3) + " " + (z + 15) + " 150 10", "wildcatter-phosphate-evaporator");
			showReadout(context, server, granulator, "tp @p 3 " + (up + 10) + " " + (z + 27) + " -110 35", "wildcatter-phosphate-granulator");
			String bags = "";
			for (int i = 0; i < 10 && !bags.contains("1 bags filled"); i++) {
				context.waitTicks(20);
				bags = read(server, 10, up, z + 28);
			}
			check(bags.contains("1 bags filled"), "a DAP bag should be filled: " + bags);
			showReadout(context, server, bagger, "tp @p 7 " + (up + 5) + " " + (z + 34) + " -135 25", "wildcatter-phosphate-bagging");
			server.runCommand("tp @p -4 " + (up + 9) + " " + (z + 26) + " -150 40");
			context.waitTicks(10);
			server.computeOnServer(srv -> {
				Inspectable tower = (Inspectable) srv.overworld().getBlockEntity(new BlockPos(3, up, z + 17));
				for (ServerPlayer player : srv.getPlayerList().getPlayers()) {
					player.sendOverlayMessage(tower.inspect());
				}
				return null;
			});
			context.waitTicks(10);
			context.takeScreenshot("wildcatter-phosphate-gypsum-cell");
			context.waitTicks(30);
			server.runCommand("tp @p 30 " + (up + 18) + " " + (z - 18) + " 45 30");
			context.waitTicks(60);
			context.takeScreenshot("wildcatter-phosphate-plant-north");
			System.out.println("[wildcatter-test] phosphatePlant ok: " + bags);
		} finally {
			if (film != null) {
				film.stop();
			}
		}
	}

	/** Puts a part's readout in the player's action bar, as clicking it would, from where {@code tp} stands; then a screenshot. */
	private void showReadout(ClientGameTestContext context, TestServerContext server, BlockPos part, String tp, String shot) {
		server.runCommand(tp);
		context.waitTicks(3);
		server.computeOnServer(srv -> {
			if (dev.wildcatter.multiblock.BigParts.entity(srv.overworld(), part) instanceof Inspectable i) {
				for (ServerPlayer player : srv.getPlayerList().getPlayers()) {
					player.sendOverlayMessage(i.inspect());
				}
			}
			return null;
		});
		context.waitTicks(10);
		context.takeScreenshot(shot);
		context.waitTicks(30);   // a moment to read it on film
	}

	/**
	 * Conversion to UF6 (7d, docs/multiblocks/conversion.md). The main line runs south in a column, every part facing north and
	 * touching the next: reduction reactor, hydrofluorinator, fluorinator, cold traps, distillation columns, cylinder filler; their
	 * reagents on the east faces, their 480 V boxes on the west. North: the steam reformer. East: the HF kiln and its column, an LPG
	 * bullet and an acid tank. Beside the line: the fluorine cell house with its rectifier against its east side, on 4.16 kV from two
	 * paralleled diesel sets and a backward unit substation. Hydrogen, HF and fluorine must flow; the rectifier must drive about
	 * 6 kA; the line must carry uranium to the filler; a cylinder the hook has filled to just under full must be finished and drop
	 * into the hopper under the filler; empty drums must come out under the reducer.
	 */
	private void conversion(ClientGameTestContext context, TestServerContext server) {
		int z = -1900;
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand("tp @p 12 " + (up + 24) + " " + (z + 40) + " 180 40");
		context.waitTicks(20);
		server.runCommand("fill -4 " + (y - 1) + " " + (z - 11) + " 42 " + (y + 12) + " " + (z + 27) + " minecraft:air");
		server.runCommand("fill -4 " + (y - 1) + " " + (z - 11) + " 42 " + (y - 1) + " " + (z + 27) + " minecraft:stone");
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		// the main line, south down x 0-2
		BlockPos reducer = new BlockPos(0, up, z);
		placeBig(server, WildcatterBlocks.REDUCTION_REACTOR, reducer, north);                  // z..z+2
		placeBig(server, WildcatterBlocks.HYDROFLUORINATOR, new BlockPos(0, up, z + 3), north);  // z+3..z+8
		placeBig(server, WildcatterBlocks.FLUORINATOR, new BlockPos(0, up, z + 9), north);       // z+9..z+11
		placeBig(server, WildcatterBlocks.COLD_TRAPS, new BlockPos(0, up, z + 12), north);       // z+12..z+17
		placeBig(server, WildcatterBlocks.DISTILLATION_COLUMNS, new BlockPos(0, up, z + 18), north);   // z+18..z+20
		BlockPos filler = new BlockPos(0, up, z + 21);
		placeBig(server, WildcatterBlocks.CYLINDER_FILLER, filler, north);                       // z+21..z+26
		// the supply plants
		BlockPos reformer = new BlockPos(8, up, z - 6);
		placeBig(server, WildcatterBlocks.STEAM_REFORMER, reformer, north);                       // x 8-10, z-6..z-3
		placeBig(server, WildcatterBlocks.HF_KILN, new BlockPos(14, up, z), north);              // x 14-16, z..z+9
		placeBig(server, WildcatterBlocks.HF_COLUMN, new BlockPos(12, up, z + 2), north);         // x 12-13, z+2..z+3
		BlockPos bullet = new BlockPos(18, up, z - 6);
		placeBig(server, WildcatterBlocks.LPG_BULLET, bullet, north);                             // x 18-20, z-6..z+5
		BlockPos acidTank = new BlockPos(18, up, z + 7);
		placeBig(server, WildcatterBlocks.STOCK_TANK, acidTank, north);                           // x 18-21, z+7..z+10
		BlockPos cells = new BlockPos(5, up, z + 12);
		placeBig(server, WildcatterBlocks.FLUORINE_CELL_HOUSE, cells, north);                     // x 5-10, z+12..z+23
		BlockPos rectifier = new BlockPos(13, up, z + 17);
		placeBig(server, WildcatterBlocks.RECTIFIER, rectifier, net.minecraft.core.Direction.SOUTH);   // x 11-13, z+14..z+17, its box east
		// hydrogen: the reformer's back, down and west to the reducer's east side
		pipes(server, z, new int[][] {{9, 0, -2}, {9, 0, -1}, {9, 0, 0}, {9, 0, 1}, {8, 0, 1}, {7, 0, 1}, {6, 0, 1}, {5, 0, 1}, {4, 0, 1}, {3, 0, 1}});
		// anhydrous HF: the column's south side west to the hydrofluorinator, a branch south to the cell house's front
		pipes(server, z, new int[][] {{12, 0, 4}, {12, 0, 5}});
		for (int x = 3; x <= 11; x++) {
			put(server, x, up, z + 5, "wildcatter:gas_line");
		}
		for (int zz = z + 6; zz <= z + 11; zz++) {
			put(server, 6, up, zz, "wildcatter:gas_line");
		}
		// fluorine: the cell house's back, west and up x 4 to the fluorinator's east side
		pipes(server, z, new int[][] {{7, 0, 24}, {6, 0, 24}, {5, 0, 24}, {4, 0, 24}});
		for (int zz = z + 10; zz <= z + 23; zz++) {
			put(server, 4, up, zz, "wildcatter:gas_line");
		}
		put(server, 3, up, z + 10, "wildcatter:gas_line");
		// LPG to the kiln and the reformer; acid to the kiln
		put(server, 17, up, z + 1, "wildcatter:gas_line");
		for (int x = 11; x <= 17; x++) {
			put(server, x, up, z - 5, "wildcatter:gas_line");
		}
		put(server, 17, up, z + 8, "wildcatter:gas_line");
		// hoppers: drums on the reducer and the empties under it, fluorspar on the kiln, cylinders on the filler and under it
		put(server, 1, up + 10, z + 1, "minecraft:hopper");
		server.runCommand("item replace block 1 " + (up + 10) + " " + (z + 1) + " container.0 with wildcatter:yellowcake_drum 1");
		server.runCommand("item replace block 1 " + (up + 10) + " " + (z + 1) + " container.1 with wildcatter:yellowcake_drum 1");
		BlockPos drumsOut = new BlockPos(1, y, z + 1);
		put(server, 1, y, z + 1, "minecraft:hopper");
		put(server, 15, up + 4, z + 4, "minecraft:hopper");
		server.runCommand("item replace block 15 " + (up + 4) + " " + (z + 4) + " container.0 with wildcatter:fluorite 2");
		put(server, 1, up + 3, z + 23, "minecraft:hopper");
		server.runCommand("item replace block 1 " + (up + 3) + " " + (z + 23) + " container.0 with wildcatter:empty_48y_cylinder 2");
		BlockPos cylindersOut = new BlockPos(1, y, z + 23);
		put(server, 1, y, z + 23, "minecraft:hopper");
		fillTank(server, bullet, dev.wildcatter.fluid.Product.LPG, 20_000);
		fillTank(server, acidTank, dev.wildcatter.fluid.Product.SULFURIC_ACID, 20_000);
		water(server, reformer);
		// 480 V: a set north, its line along z-10, west to x -2 and down the line's boxes; a branch to the kiln's box over the LPG pipe
		BlockPos lvSet = new BlockPos(26, up, z - 9);
		genset(server, lvSet, north, false);                                                    // x 21-26, z-9..z-8
		java.util.List<int[]> cable = new java.util.ArrayList<>();
		cable.add(new int[] {27, 0, -9});
		for (int x = -2; x <= 27; x++) {
			cable.add(new int[] {x, 0, -10});
		}
		for (int zz = -9; zz <= 21; zz++) {
			cable.add(new int[] {-2, 0, zz});
		}
		for (int zz : new int[] {12, 18, 21}) {
			cable.add(new int[] {-1, 0, zz});
		}
		for (int[] c : new int[][] {{14, 0, -9}, {14, 0, -8}, {14, 0, -7}, {14, 0, -6}, {14, 1, -6}, {14, 1, -5}, {14, 1, -4}, {14, 0, -4}, {14, 0, -3}, {14, 0, -2},
				{14, 0, -1}, {13, 0, -1}, {13, 0, 0}}) {
			cable.add(c);
		}
		for (int[] c : cable) {
			put(server, c[0], up + c[1], z + c[2], "wildcatter:cable_industrial");
		}
		// 4.16 kV: two sets on a busway, a backward unit substation, the line north and west to the rectifier's box
		BlockPos[] mvSets = {new BlockPos(26, up, z + 20), new BlockPos(34, up, z + 20)};
		for (BlockPos set : mvSets) {
			genset(server, set, north, false);                                                   // x 21-26 and 29-34, z+20..z+21
			put(server, set.getX() + 1, up, z + 20, "wildcatter:busway_industrial");
		}
		for (int x = 27; x <= 36; x++) {
			put(server, x, up, z + 19, "wildcatter:busway_industrial");
		}
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, new BlockPos(36, up, z + 19), net.minecraft.core.Direction.EAST, false);   // x 37-40, z+18..z+19
		for (int zz = z + 16; zz <= z + 19; zz++) {
			put(server, 41, up, zz, "wildcatter:cable_mv");
		}
		for (int x = 14; x <= 40; x++) {
			put(server, x, up, z + 16, "wildcatter:cable_mv");
		}
		put(server, 14, up, z + 17, "wildcatter:cable_mv");
		exportBlueprint(server, "conversion", new BlockPos(-4, y, z - 11), new BlockPos(42, y + 12, z + 27));   // the library (docs/blueprints.md)
		startGenset(context, server, lvSet, north, 2000);
		for (BlockPos set : mvSets) {
			startGenset(context, server, set, north, 2000);
		}
		// both sets on the bus before the 0.67 MW of cells: their cooling water now, and they start
		water(server, reformer, cells);
		for (int i = 0; i < 12; i++) {
			context.waitTicks(50);
			water(server, reformer, cells);
		}
		String chain = " | reformer " + read(server, 8, up, z - 6) + " | kiln " + read(server, 14, up, z) + " | column " + read(server, 12, up, z + 2)
				+ " | cells " + read(server, 5, up, z + 12) + " | rectifier " + read(server, 13, up, z + 17) + " | reducer " + read(server, 0, up, z)
				+ " | hydrofluorinator " + read(server, 0, up, z + 3) + " | fluorinator " + read(server, 0, up, z + 9) + " | traps " + read(server, 0, up, z + 12)
				+ " | columns " + read(server, 0, up, z + 18) + " | filler " + read(server, 0, up, z + 21) + " | lv " + read(server, 26, up, z - 9)
				+ " | mv " + read(server, 26, up, z + 20);
		check(read(server, 8, up, z - 6).contains("reforming"), "the reformer should make hydrogen: " + chain);
		check(read(server, 12, up, z + 2).contains("condensing"), "the HF column should make anhydrous HF: " + chain);
		check(read(server, 5, up, z + 12).contains("kg/h of fluorine"), "the cell house should make fluorine: " + chain);
		double amps = server.computeOnServer(srv -> ((dev.wildcatter.block.RectifierBlockEntity) srv.overworld().getBlockEntity(rectifier)).dcAmps());
		check(amps > 5_000, "the rectifier should drive about 6 kA: " + amps + chain);
		check(read(server, 0, up, z).contains("reducing") && read(server, 0, up, z + 3).contains("hydrofluorinating")
				&& read(server, 0, up, z + 9).contains("fluorinating"), "the line should reduce, hydrofluorinate and fluorinate: " + chain);
		int empties = countIn(server, drumsOut, WildcatterItems.EMPTY_DRUM);
		check(empties > 0, "the empty drum should come out under the reducer: " + empties + chain);
		server.computeOnServer(srv -> {   // a cylinder all but full: the plant's own UF6 finishes it
			((dev.wildcatter.block.CylinderFillerBlockEntity) srv.overworld().getBlockEntity(filler)).addUf6(Tuning.CYLINDER_48Y_KG - 3);
			return null;
		});
		int full = 0;
		for (int i = 0; i < 40 && full == 0; i++) {
			context.waitTicks(50);
			water(server, reformer, cells);
			full = countIn(server, cylindersOut, WildcatterItems.UF6_CYLINDER_48Y);
		}
		check(full > 0, "a full 48Y cylinder should drop into the hopper under the filler: " + read(server, 0, up, z + 21) + " | columns " + read(server, 0, up, z + 18)
				+ " | traps " + read(server, 0, up, z + 12) + " | fluorinator " + read(server, 0, up, z + 9));
		server.runCommand("tp @p 18 " + (up + 18) + " " + (z + 38) + " 165 30");
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-conversion");
		System.out.println("[wildcatter-test] conversion ok: " + read(server, 13, up, z + 17) + " | " + read(server, 5, up, z + 12) + " | " + read(server, 0, up, z)
				+ " | full cylinders " + full);
	}

	/**
	 * The centrifuge cascade (7e, docs/multiblocks/enrichment.md). South: the peaker's chain, an aeroderivative through its 60 MVA
	 * GSU to 138 kV and a 60 MVA substation transformer down to 12.47 kV, run north and west to the frequency converter. North: the
	 * feed station, the converter and the product and tails take-offs on a ground header, which climbs to run over the tops of a row
	 * of centrifuges; a diesel set's 480 V for the stations. Ten machines must read too few stages; the 11th must start the cascade
	 * at about 6.3 MW and 1,225 Hz; both take-offs must fill; nearly full cylinders must finish and drop into their hoppers; a nearly
	 * empty feed 48Y must come out empty; without the product take-off it must idle on total reflux, and without the converter the
	 * machines must lose their drive.
	 */
	private void cascade(ClientGameTestContext context, TestServerContext server) {
		int z = -2100;
		int zg = z - 30;   // the ground header
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand("tp @p 10 " + (up + 20) + " " + (z + 20) + " 180 30");
		context.waitTicks(20);
		for (int zz = z - 40; zz <= z + 22; zz += 21) {   // /fill takes at most 32,768 blocks
			server.runCommand("fill -30 " + (y - 1) + " " + zz + " 44 " + (y + 16) + " " + Math.min(zz + 20, z + 22) + " minecraft:air");
		}
		server.runCommand("fill -30 " + (y - 1) + " " + (z - 40) + " 44 " + (y - 1) + " " + (z + 22) + " minecraft:stone");
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		// the power: the peaker's turbine, GSU and substation transformer, as its scene builds them
		BlockPos gt = new BlockPos(20, up, z);
		placeBig(server, WildcatterBlocks.AERO_TURBINE, gt, north);                                 // x 20-24, z..z+13
		placeBig(server, WildcatterBlocks.AERO_INLET_HOUSE, new BlockPos(20, up, z - 5), north);    // x 20-25, z-5..z-1
		placeBig(server, WildcatterBlocks.AERO_EXHAUST_STACK, new BlockPos(20, up, z + 14), north); // x 20-23, z+14..z+17
		placeBig(server, WildcatterBlocks.AERO_GENERATOR, new BlockPos(25, up, z + 1), north);      // x 25-28, z+1..z+7
		placeBig(server, WildcatterBlocks.GEN_BREAKER_3000, new BlockPos(26, up, z + 8), north);    // x 26-27, z+8..z+9
		placeBig(server, WildcatterBlocks.GSU_60, new BlockPos(36, up, z + 8), north);              // x 33-38, z+4..z+8
		placeBig(server, WildcatterBlocks.SUBSTATION_TRANSFORMER_60, new BlockPos(36, up, z - 6), north);   // x 33-38, z-10..z-6
		BlockPos fuelTank = new BlockPos(14, up, z + 4);
		placeBig(server, WildcatterBlocks.STOCK_TANK, fuelTank, north);                              // x 14-17, z+4..z+7
		pipes(server, z, new int[][] {{18, 0, 6}, {19, 0, 6}});
		for (int x = 26; x <= 37; x++) {
			put(server, x, up, z + 10, "wildcatter:bus_duct_gen");
		}
		put(server, 36, up, z + 9, "wildcatter:bus_duct_gen");
		sizeCables(server, new BlockPos(26, up, z + 9), new BlockPos(37, up, z + 10), WildcatterBlocks.BUS_DUCT_GEN, dev.wildcatter.energy.ConductorSize.BUS_DUCT_3000);
		for (int zz = z - 10; zz <= z + 4; zz++) {
			put(server, 36, up + 6, zz, "wildcatter:cable_hv");
		}
		BlockPos starter = new BlockPos(10, up, z + 2);
		genset(server, starter, north, false);                                                    // x 5-10, z+2..z+3
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, starter, net.minecraft.core.Direction.EAST, false);   // x 11-14, z+1..z+2
		for (int x = 15; x <= 19; x++) {
			put(server, x, up, z + 2, "wildcatter:cable_mv");
		}
		// 12.47 kV: the substation transformer's throat, east, north up x 40, west along z-38 and down to the converter's box
		java.util.List<int[]> mv = new java.util.ArrayList<>();
		for (int x = 36; x <= 40; x++) {
			mv.add(new int[] {x, z - 5});
		}
		for (int zz = z - 38; zz <= z - 6; zz++) {
			mv.add(new int[] {40, zz});
		}
		for (int x = -17; x <= 39; x++) {
			mv.add(new int[] {x, z - 38});
		}
		mv.add(new int[] {-17, z - 37});
		mv.add(new int[] {-17, z - 36});
		for (int[] c : mv) {
			put(server, c[0], up, c[1], "wildcatter:cable_dist");
		}
		sizeCables(server, new BlockPos(-17, up, z - 38), new BlockPos(40, up, z - 5), WildcatterBlocks.CABLE_DIST, dev.wildcatter.energy.ConductorSize.MV_500);
		// the cascade: feed station, converter and take-offs on the ground header at zg, which climbs at x -1 over ten machines
		BlockPos feed = new BlockPos(-20, up, zg - 5);
		placeBig(server, WildcatterBlocks.FEED_STATION, feed, north);                              // x -20..-18, zg-5..zg-1
		BlockPos converter = new BlockPos(-16, up, zg - 6);
		placeBig(server, WildcatterBlocks.FREQUENCY_CONVERTER, converter, north);                  // x -16..-14, zg-6..zg-1, its box at x -17
		BlockPos product = new BlockPos(-12, up, zg + 1);
		placeBig(server, WildcatterBlocks.PRODUCT_TAKEOFF, product, north);                        // x -12..-10, zg+1..zg+4
		BlockPos tails = new BlockPos(-8, up, zg + 1);
		placeBig(server, WildcatterBlocks.TAILS_TAKEOFF, tails, north);                            // x -8..-6, zg+1..zg+5
		for (int x = 0; x < 10; x++) {
			placeBig(server, WildcatterBlocks.CENTRIFUGE, new BlockPos(x, up, zg), north);
		}
		for (int x = -20; x <= -1; x++) {
			put(server, x, up, zg, "wildcatter:gas_line");
		}
		for (int yy = up + 1; yy <= up + 6; yy++) {
			put(server, -1, yy, zg, "wildcatter:gas_line");
		}
		for (int x = 0; x <= 10; x++) {
			put(server, x, up + 6, zg, "wildcatter:gas_line");
		}
		// hoppers: a natural 48Y on the feed station, empties on the take-offs, and one under each
		put(server, -19, up + 3, zg - 3, "minecraft:hopper");
		server.runCommand("item replace block -19 " + (up + 3) + " " + (zg - 3) + " container.0 with wildcatter:uf6_cylinder_48y 1");
		BlockPos feedOut = new BlockPos(-19, y, zg - 3);
		put(server, -19, y, zg - 3, "minecraft:hopper");
		put(server, -11, up + 3, zg + 2, "minecraft:hopper");
		server.runCommand("item replace block -11 " + (up + 3) + " " + (zg + 2) + " container.0 with wildcatter:empty_30b_cylinder 2");
		BlockPos productOut = new BlockPos(-11, y, zg + 2);
		put(server, -11, y, zg + 2, "minecraft:hopper");
		put(server, -7, up + 3, zg + 2, "minecraft:hopper");
		server.runCommand("item replace block -7 " + (up + 3) + " " + (zg + 2) + " container.0 with wildcatter:empty_48y_cylinder 2");
		BlockPos tailsOut = new BlockPos(-7, y, zg + 2);
		put(server, -7, y, zg + 2, "minecraft:hopper");
		// 480 V for the stations: a set south-west, up x -23 to the feed station's box, east along zg+6 to the take-offs' boxes
		BlockPos lvSet = new BlockPos(-24, up, zg + 4);
		genset(server, lvSet, north, false);                                                    // x -29..-24, zg+4..zg+5
		java.util.List<int[]> lv = new java.util.ArrayList<>();
		for (int zz = zg - 5; zz <= zg + 6; zz++) {
			lv.add(new int[] {-23, zz});
		}
		lv.add(new int[] {-22, zg - 5});
		lv.add(new int[] {-21, zg - 5});
		for (int x = -22; x <= -9; x++) {
			lv.add(new int[] {x, zg + 6});
		}
		for (int zz = zg + 1; zz <= zg + 5; zz++) {
			lv.add(new int[] {-13, zz});
			lv.add(new int[] {-9, zz});
		}
		for (int[] c : lv) {
			put(server, c[0], up, c[1], "wildcatter:cable_industrial");
		}
		exportBlueprint(server, "enrichment", new BlockPos(-30, y, z - 40), new BlockPos(44, y + 16, z + 22));   // the library (docs/blueprints.md)
		// start: the starter set, the turbine, then the stations' set
		fillTank(server, fuelTank, dev.wildcatter.fluid.Product.KEROSENE, 40_000);
		startGenset(context, server, starter, north, 2000);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.GasTurbineBlockEntity) srv.overworld().getBlockEntity(gt)).setWantRun(true);
			return null;
		});
		context.waitTicks(200);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.GasTurbineBlockEntity) srv.overworld().getBlockEntity(gt)).skipStartForTest(false);
			return null;
		});
		context.waitTicks(100);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.GasTurbineBlockEntity) srv.overworld().getBlockEntity(gt)).skipRampForTest();
			return null;
		});
		startGenset(context, server, lvSet, north, 2000);
		context.waitTicks(300);
		String few = read(server, -20, up, zg - 5);
		String power = " | converter " + read(server, -16, up, zg - 6) + " | turbine " + read(server, 20, up, z) + " | throat " + read(server, 36, up, z - 5);
		check(read(server, -16, up, zg - 6).contains("driving 10 machines"), "the converter should drive the ten machines: " + few + power);
		check(few.contains("TOO FEW STAGES (10 of 11)"), "ten machines are too few stages: " + few + power);
		// the 11th machine starts the cascade
		placeBig(server, WildcatterBlocks.CENTRIFUGE, new BlockPos(10, up, zg), north);
		context.waitTicks(300);
		String running = read(server, -20, up, zg - 5);
		String conv = read(server, -16, up, zg - 6);
		power = " | converter " + conv + " | turbine " + read(server, 20, up, z) + " | machine " + read(server, 5, up, zg);
		check(running.contains("feeding") && running.contains("11 machines driven"), "eleven machines should run the cascade: " + running + power);
		java.util.regex.Matcher kw = java.util.regex.Pattern.compile("([0-9]+) kW of ([0-9]+) kW wanted").matcher(conv);
		check(kw.find() && Double.parseDouble(kw.group(1)) > 5_500, "the converter should take about 6.3 MW: " + conv);
		java.util.regex.Matcher hz = java.util.regex.Pattern.compile("machines at ([0-9]+) Hz").matcher(conv);
		check(conv.contains("1,225 Hz out") && hz.find() && Integer.parseInt(hz.group(1)) > 1_150, "the machines should run near 1,225 Hz: " + conv + power);
		double[] filled = server.computeOnServer(srv -> new double[] {
				((dev.wildcatter.block.TakeoffBlockEntity) srv.overworld().getBlockEntity(product)).uf6(),
				((dev.wildcatter.block.TakeoffBlockEntity) srv.overworld().getBlockEntity(tails)).uf6()});
		check(filled[0] > 0 && filled[1] > filled[0] * 8, "both take-offs should fill, the tails about nine times faster: " + filled[0] + " / " + filled[1]
				+ " | " + read(server, -12, up, zg + 1) + " | " + read(server, -8, up, zg + 1));
		// cylinders all but full: the cascade finishes them
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.TakeoffBlockEntity) srv.overworld().getBlockEntity(product)).addUf6(Tuning.CYLINDER_30B_KG);
			((dev.wildcatter.block.TakeoffBlockEntity) srv.overworld().getBlockEntity(product)).addUf6(-0.05);
			((dev.wildcatter.block.TakeoffBlockEntity) srv.overworld().getBlockEntity(tails)).addUf6(Tuning.CYLINDER_48Y_KG);
			((dev.wildcatter.block.TakeoffBlockEntity) srv.overworld().getBlockEntity(tails)).addUf6(-0.5);
			return null;
		});
		int leu = 0, depleted = 0;
		for (int i = 0; i < 30 && (leu == 0 || depleted == 0); i++) {
			context.waitTicks(40);
			leu = countIn(server, productOut, WildcatterItems.LEU_CYLINDER_30B);
			depleted = countIn(server, tailsOut, WildcatterItems.TAILS_CYLINDER_48Y);
		}
		check(leu > 0, "a full 30B of LEU should drop into the hopper under the product take-off: " + read(server, -12, up, zg + 1) + " | " + read(server, -20, up, zg - 5));
		check(depleted > 0, "a full 48Y of tails should drop into the hopper under the tails take-off: " + read(server, -8, up, zg + 1));
		// a feed cylinder all but empty: it comes out empty
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.FeedStationBlockEntity) srv.overworld().getBlockEntity(feed)).setUf6ForTest(1.0);
			return null;
		});
		int empty = 0;
		for (int i = 0; i < 30 && empty == 0; i++) {
			context.waitTicks(40);
			empty = countIn(server, feedOut, WildcatterItems.EMPTY_48Y_CYLINDER);
		}
		check(empty > 0, "the emptied 48Y should drop into the hopper under the feed station: " + read(server, -20, up, zg - 5));
		check(read(server, -20, up, zg - 5).contains("NO FEED"), "with its only cylinder empty it should read NO FEED: " + read(server, -20, up, zg - 5));
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.FeedStationBlockEntity) srv.overworld().getBlockEntity(feed)).setUf6ForTest(Tuning.CYLINDER_48Y_KG);
			return null;
		});
		context.waitTicks(40);
		check(read(server, -20, up, zg - 5).contains("feeding"), "a fresh cylinder should feed again: " + read(server, -20, up, zg - 5));
		server.runCommand("tp @p -6 " + (up + 14) + " " + (zg + 20) + " 180 32");
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-cascade");
		String ok = read(server, -20, up, zg - 5) + " | " + conv + " | " + read(server, 5, up, zg);
		// without the product take-off: total reflux; without the converter: no drive
		server.computeOnServer(srv -> srv.overworld().destroyBlock(product, false));
		context.waitTicks(40);
		check(read(server, -20, up, zg - 5).contains("TOTAL REFLUX"), "without its product take-off the cascade should idle on total reflux: " + read(server, -20, up, zg - 5));
		server.computeOnServer(srv -> srv.overworld().destroyBlock(converter, false));
		context.waitTicks(80);
		check(read(server, -20, up, zg - 5).contains("NO DRIVE") && read(server, 5, up, zg).contains("NO DRIVE"),
				"without the converter the machines should lose their drive: " + read(server, -20, up, zg - 5) + " | " + read(server, 5, up, zg));
		System.out.println("[wildcatter-test] cascade ok: " + ok + " | 30B " + leu + ", tails " + depleted + ", emptied " + empty);
	}

	/**
	 * The zirconium plant (7f, docs/multiblocks/zirconium.md). The line runs south down x 0-2, each part against the next; the
	 * magnesium cell against the Kroll reactor and the vacuum distillation, with the magnesia chlorinator north of it and a rectifier
	 * east; the chlor-alkali cell north-east with its own rectifier. One chlorine run joins both cells to both chlorinators. Two
	 * paralleled sets give 480 V down x -2; two more through a backward unit substation give 4.16 kV to both rectifiers and to the
	 * extrusion press, which faces south so its box is on the east. Both cells must make chlorine, the magnesia chlorinator must start
	 * the magnesium loop, each stage must run once the one before it holds metal, a nearly rolled tube and then a guide thimble must
	 * drop into the hopper under the anneal furnace, and without the hafnium column the Kroll reactor must say so.
	 */
	private void zirconium(ClientGameTestContext context, TestServerContext server) {
		int z = -2300;
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand("tp @p 10 " + (up + 24) + " " + (z + 30) + " 180 40");
		context.waitTicks(20);
		for (int zz = z - 12; zz <= z + 52; zz += 21) {   // /fill takes at most 32,768 blocks
			server.runCommand("fill -20 " + (y - 1) + " " + zz + " 47 " + (y + 14) + " " + Math.min(zz + 20, z + 52) + " minecraft:air");
		}
		server.runCommand("fill -20 " + (y - 1) + " " + (z - 12) + " 47 " + (y - 1) + " " + (z + 52) + " minecraft:stone");
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		net.minecraft.core.Direction south = net.minecraft.core.Direction.SOUTH;
		// the line, south down x 0-2
		placeBig(server, WildcatterBlocks.MINERAL_SEPARATOR, new BlockPos(0, up, z), north);          // z..z+5
		BlockPos chlorinator = new BlockPos(0, up, z + 6);
		placeBig(server, WildcatterBlocks.CHLORINATOR, chlorinator, north);                           // z+6..z+8
		BlockPos hafnium = new BlockPos(0, up, z + 9);
		placeBig(server, WildcatterBlocks.HAFNIUM_COLUMN, hafnium, north);                            // z+9..z+11
		BlockPos kroll = new BlockPos(0, up, z + 12);
		placeBig(server, WildcatterBlocks.KROLL_REACTOR, kroll, north);                               // z+12..z+14
		BlockPos distillation = new BlockPos(0, up, z + 15);
		placeBig(server, WildcatterBlocks.VACUUM_DISTILLATION, distillation, north);                  // z+15..z+17
		BlockPos var = new BlockPos(0, up, z + 18);
		placeBig(server, WildcatterBlocks.VAR_FURNACE, var, north);                                   // z+18..z+20
		BlockPos press = new BlockPos(2, up, z + 28);
		placeBig(server, WildcatterBlocks.EXTRUSION_PRESS, press, south);                             // z+21..z+28, its box east at x 3
		BlockPos pilger = new BlockPos(0, up, z + 29);
		placeBig(server, WildcatterBlocks.PILGER_MILL, pilger, north);                                // z+29..z+38
		placeBig(server, WildcatterBlocks.VACUUM_ANNEAL_FURNACE, new BlockPos(0, up, z + 39), north); // z+39..z+46
		// the magnesium loop and the chlor-alkali cell
		BlockPos cell = new BlockPos(3, up, z + 12);
		placeBig(server, WildcatterBlocks.MAGNESIUM_CELL, cell, north);                              // x 3-5, z+12..z+17
		BlockPos magnesia = new BlockPos(3, up, z + 9);
		placeBig(server, WildcatterBlocks.MAGNESIA_CHLORINATOR, magnesia, north);                    // x 3-5, z+9..z+11
		BlockPos mgRectifier = new BlockPos(8, up, z + 16);
		placeBig(server, WildcatterBlocks.RECTIFIER, mgRectifier, south);                            // x 6-8, z+13..z+16, its box east at x 9
		BlockPos chlorAlkali = new BlockPos(12, up, z);
		placeBig(server, WildcatterBlocks.CHLOR_ALKALI_CELL, chlorAlkali, north);                    // x 12-14, z..z+5
		BlockPos caRectifier = new BlockPos(17, up, z + 4);
		placeBig(server, WildcatterBlocks.RECTIFIER, caRectifier, south);                            // x 15-17, z+1..z+4, its box east at x 18
		// chlorine: the chlor-alkali cell's back and the magnesium cell's front east, to the chlorinator's east side and the magnesia
		// chlorinator's front
		pipes(server, z, new int[][] {{13, 0, 6}, {13, 0, 7}, {13, 0, 8}, {6, 0, 12}, {6, 0, 11}, {6, 0, 10}, {6, 0, 9}});
		for (int x = 3; x <= 12; x++) {
			put(server, x, up, z + 8, "wildcatter:gas_line");
		}
		// hoppers
		put(server, 1, up + 4, z + 2, "minecraft:hopper");
		server.runCommand("item replace block 1 " + (up + 4) + " " + (z + 2) + " container.0 with wildcatter:heavy_mineral 2");
		put(server, 1, up + 10, z + 7, "minecraft:hopper");
		server.runCommand("item replace block 1 " + (up + 10) + " " + (z + 7) + " container.0 with minecraft:coal 4");
		put(server, 4, up + 6, z + 10, "minecraft:hopper");
		server.runCommand("item replace block 4 " + (up + 6) + " " + (z + 10) + " container.0 with wildcatter:magnesia 1");
		server.runCommand("item replace block 4 " + (up + 6) + " " + (z + 10) + " container.1 with minecraft:coal 2");
		put(server, 1, up + 8, z + 19, "minecraft:hopper");
		server.runCommand("item replace block 1 " + (up + 8) + " " + (z + 19) + " container.0 with wildcatter:tin_ingot 1");
		server.runCommand("item replace block 1 " + (up + 8) + " " + (z + 19) + " container.1 with minecraft:iron_ingot 1");
		server.runCommand("item replace block 1 " + (up + 8) + " " + (z + 19) + " container.2 with wildcatter:chromite 1");
		put(server, 13, up + 3, z + 2, "minecraft:hopper");
		server.runCommand("item replace block 13 " + (up + 3) + " " + (z + 2) + " container.0 with wildcatter:halite 1");
		BlockPos tubesOut = new BlockPos(1, y, z + 42);
		put(server, 1, y, z + 42, "minecraft:hopper");
		// 480 V: two sets on a busway north-west, down x -2 with a stub to each box at x -1
		BlockPos[] lvSets = {new BlockPos(-12, up, z - 8), new BlockPos(-4, up, z - 8)};
		for (BlockPos set : lvSets) {
			genset(server, set, north, false);                                                     // x -17..-12 and -9..-4, z-8..z-7
			put(server, set.getX() + 1, up, z - 8, "wildcatter:busway_industrial");
		}
		for (int x = -11; x <= -3; x++) {
			put(server, x, up, z - 9, "wildcatter:busway_industrial");
		}
		for (int zz = z - 9; zz <= z + 39; zz++) {   // the plant's 0.5 MW is about 600 A: an 800 A busway down the line
			put(server, -2, up, zz, "wildcatter:busway_industrial");
		}
		for (int zz : new int[] {0, 6, 9, 12, 15, 18, 29, 39}) {
			put(server, -1, up, z + zz, "wildcatter:cable_industrial");
		}
		sizeCables(server, new BlockPos(-1, up, z), new BlockPos(-1, up, z + 39), WildcatterBlocks.CABLE_INDUSTRIAL, dev.wildcatter.energy.ConductorSize.KCMIL_500);
		// 4.16 kV: two sets on a busway east, a backward unit substation, west along z+16 to the magnesium cell's rectifier, north up
		// x 18 to the chlor-alkali cell's, and south down x 45 and west along z+28 to the press
		BlockPos[] mvSets = {new BlockPos(30, up, z + 20), new BlockPos(38, up, z + 20)};
		for (BlockPos set : mvSets) {
			genset(server, set, north, false);                                                     // x 25-30 and 33-38, z+20..z+21
			put(server, set.getX() + 1, up, z + 20, "wildcatter:busway_industrial");
		}
		for (int x = 31; x <= 40; x++) {
			put(server, x, up, z + 19, "wildcatter:busway_industrial");
		}
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, new BlockPos(40, up, z + 19), net.minecraft.core.Direction.EAST, false);   // x 41-44, z+18..z+19
		java.util.List<int[]> mv = new java.util.ArrayList<>();
		for (int zz = z + 16; zz <= z + 28; zz++) {
			mv.add(new int[] {45, zz});
		}
		for (int x = 9; x <= 44; x++) {
			mv.add(new int[] {x, z + 16});
		}
		for (int zz = z + 4; zz <= z + 15; zz++) {
			mv.add(new int[] {18, zz});
		}
		for (int x = 3; x <= 44; x++) {
			mv.add(new int[] {x, z + 28});
		}
		for (int[] c : mv) {
			put(server, c[0], up, c[1], "wildcatter:cable_mv");
		}
		exportBlueprint(server, "zirconium", new BlockPos(-20, y, z - 12), new BlockPos(47, y + 14, z + 52));   // the library (docs/blueprints.md)
		for (BlockPos set : lvSets) {
			startGenset(context, server, set, north, 2000);
		}
		for (BlockPos set : mvSets) {
			startGenset(context, server, set, north, 2000);
		}
		water(server, chlorAlkali);
		for (int i = 0; i < 12; i++) {
			context.waitTicks(50);
			water(server, chlorAlkali);
		}
		String cells = " | chlor-alkali " + read(server, 12, up, z) + " | its rectifier " + read(server, 17, up, z + 4) + " | magnesia chlorinator " + read(server, 3, up, z + 9)
				+ " | magnesium cell " + read(server, 3, up, z + 12) + " | its rectifier " + read(server, 8, up, z + 16) + " | mv " + read(server, 30, up, z + 20);
		check(read(server, 12, up, z).contains("kg/h of chlorine") && read(server, 12, up, z).contains("m3/h of hydrogen"), "the chlor-alkali cell should make chlorine and hydrogen: " + cells);
		check(read(server, 3, up, z + 12).contains("kg/h of magnesium"), "the magnesia chlorinator should start the magnesium cell: " + cells);
		check(read(server, 17, up, z + 4).contains("kA") && read(server, 8, up, z + 16).contains("kA"), "both rectifiers should drive their cells: " + cells);
		check(read(server, 0, up, z).contains("separating"), "the separator should separate the heavy minerals: " + read(server, 0, up, z) + " | lv set " + read(server, -12, up, z - 8)
				+ " | second " + read(server, -4, up, z - 8) + " | busway " + read(server, -5, up, z - 9) + " | column " + read(server, -2, up, z - 5) + " / " + read(server, -2, up, z) + " | stub " + read(server, -1, up, z));
		check(read(server, 0, up, z + 6).contains("chlorinating"), "the chlorinator should chlorinate the zircon: " + read(server, 0, up, z + 6) + cells);
		// metal in each stage: the next one runs
		server.computeOnServer(srv -> {
			for (BlockPos at : new BlockPos[] {chlorinator, hafnium, kroll, distillation, var, press}) {
				((dev.wildcatter.block.ZrStage) srv.overworld().getBlockEntity(at)).addZirconium(1.0);
			}
			return null;
		});
		context.waitTicks(200);
		String line = " | hafnium " + read(server, 0, up, z + 9) + " | kroll " + read(server, 0, up, z + 12) + " | distillation " + read(server, 0, up, z + 15)
				+ " | var " + read(server, 0, up, z + 18) + " | press " + read(server, 2, up, z + 28) + " | pilger " + read(server, 0, up, z + 29) + cells;
		check(read(server, 0, up, z + 9).contains("distilling"), "the hafnium column should run: " + line);
		check(read(server, 0, up, z + 12).contains("reducing"), "the Kroll reactor should reduce: " + line);
		check(read(server, 0, up, z + 15).contains("boiling off"), "the vacuum distillation should run: " + line);
		check(read(server, 0, up, z + 18).contains("remelting"), "the VAR furnace should melt Zircaloy-4: " + line);
		check(read(server, 2, up, z + 28).contains("extruding"), "the extrusion press should extrude: " + line);
		check(read(server, 0, up, z + 29).contains("rolling a cladding tube"), "the pilger mill should roll cladding: " + line);
		// a tube all but rolled: it drops into the hopper under the anneal furnace
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.PilgerMillBlockEntity) srv.overworld().getBlockEntity(pilger)).addProgressForTest(dev.wildcatter.fluid.Zirconium.claddingKg() - 0.002);
			return null;
		});
		int tubes = 0;
		for (int i = 0; i < 20 && tubes == 0; i++) {
			context.waitTicks(20);
			tubes = countIn(server, tubesOut, WildcatterItems.ZIRCALOY_TUBE);
		}
		check(tubes > 0, "a cladding tube should drop into the hopper under the anneal furnace: " + read(server, 0, up, z + 29) + " | " + read(server, 0, up, z + 39));
		// new dies: a guide thimble
		server.computeOnServer(srv -> {
			dev.wildcatter.block.PilgerMillBlockEntity mill = (dev.wildcatter.block.PilgerMillBlockEntity) srv.overworld().getBlockEntity(pilger);
			mill.changeDies();
			((dev.wildcatter.block.ZrStage) srv.overworld().getBlockEntity(press)).addZirconium(1.0);
			mill.addProgressForTest(dev.wildcatter.fluid.Zirconium.thimbleKg() - 0.002);
			return null;
		});
		int thimbles = 0;
		for (int i = 0; i < 20 && thimbles == 0; i++) {
			context.waitTicks(20);
			thimbles = countIn(server, tubesOut, WildcatterItems.GUIDE_THIMBLE);
		}
		check(thimbles > 0, "a guide thimble should drop into the hopper after the die change: " + read(server, 0, up, z + 29));
		server.runCommand("tp @p 22 " + (up + 18) + " " + (z + 44) + " 145 30");
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-zirconium");
		String ok = read(server, 12, up, z) + " | " + read(server, 3, up, z + 12) + " | " + read(server, 0, up, z + 12) + " | " + read(server, 0, up, z + 18);
		// without the hafnium column the Kroll reactor says so
		server.computeOnServer(srv -> srv.overworld().destroyBlock(hafnium, false));
		context.waitTicks(40);
		check(read(server, 0, up, z + 12).contains("HAFNIUM COLUMN"), "without its hafnium column the Kroll reactor should say so: " + read(server, 0, up, z + 12));
		System.out.println("[wildcatter-test] zirconium ok: " + ok + " | tubes " + tubes + ", thimbles " + thimbles);
	}

	/**
	 * The fuel plant (7g, docs/multiblocks/fuel.md). The line runs south down x 0-2, each part against the next, from the UF6
	 * autoclave to the assembly fixture. North-east, the conversion plant's steam reformer on LPG from a bullet makes the hydrogen,
	 * piped to the east sides of the kiln and the sintering furnace. Two paralleled sets give 480 V down an 800 A busway at x -2.
	 * Each stage must run once the one before it holds uranium; the fixture must build its skeleton from its hopper and, with 263
	 * rods in it, drop a finished assembly into the hopper under it once the welder racks the 264th; an emptied 30B must drop under
	 * the autoclave. An assembly stands beside the line for the screenshot.
	 */
	private void fuel(ClientGameTestContext context, TestServerContext server) {
		int z = -2500;
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand("tp @p 10 " + (up + 24) + " " + (z + 40) + " 180 40");
		context.waitTicks(20);
		for (int zz = z - 12; zz <= z + 66; zz += 21) {   // /fill takes at most 32,768 blocks
			server.runCommand("fill -20 " + (y - 1) + " " + zz + " 24 " + (y + 14) + " " + Math.min(zz + 20, z + 66) + " minecraft:air");
		}
		server.runCommand("fill -20 " + (y - 1) + " " + (z - 12) + " 24 " + (y - 1) + " " + (z + 66) + " minecraft:stone");
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		BlockPos autoclave = new BlockPos(0, up, z);
		placeBig(server, WildcatterBlocks.UF6_AUTOCLAVE, autoclave, north);                     // z..z+3
		BlockPos kiln = new BlockPos(0, up, z + 4);
		placeBig(server, WildcatterBlocks.IDR_KILN, kiln, north);                               // z+4..z+13
		BlockPos granulator = new BlockPos(0, up, z + 14);
		placeBig(server, WildcatterBlocks.GRANULATOR, granulator, north);                       // z+14..z+17
		BlockPos press = new BlockPos(0, up, z + 18);
		placeBig(server, WildcatterBlocks.PELLET_PRESS, press, north);                          // z+18..z+21
		BlockPos furnace = new BlockPos(0, up, z + 22);
		placeBig(server, WildcatterBlocks.SINTERING_FURNACE, furnace, north);                   // z+22..z+33
		BlockPos grinder = new BlockPos(0, up, z + 34);
		placeBig(server, WildcatterBlocks.PELLET_GRINDER, grinder, north);                      // z+34..z+37
		BlockPos loader = new BlockPos(0, up, z + 38);
		placeBig(server, WildcatterBlocks.ROD_LOADER, loader, north);                           // z+38..z+47
		BlockPos welder = new BlockPos(0, up, z + 48);
		placeBig(server, WildcatterBlocks.ROD_WELDER, welder, north);                           // z+48..z+53
		BlockPos fixture = new BlockPos(0, up, z + 54);
		placeBig(server, WildcatterBlocks.ASSEMBLY_FIXTURE, fixture, north);                    // z+54..z+63
		// hydrogen: a steam reformer on LPG, its back to a pipe west along z+6 and down x 3 past the kiln to the furnace
		BlockPos reformer = new BlockPos(8, up, z + 2);
		placeBig(server, WildcatterBlocks.STEAM_REFORMER, reformer, north);                     // x 8-10, z+2..z+5
		BlockPos bullet = new BlockPos(14, up, z - 2);
		placeBig(server, WildcatterBlocks.LPG_BULLET, bullet, north);                           // x 14-16, z-2..z+9
		for (int x = 11; x <= 13; x++) {
			put(server, x, up, z + 3, "wildcatter:gas_line");
		}
		for (int x = 3; x <= 9; x++) {
			put(server, x, up, z + 6, "wildcatter:gas_line");
		}
		for (int zz = z + 7; zz <= z + 22; zz++) {
			put(server, 3, up, zz, "wildcatter:gas_line");
		}
		// hoppers
		put(server, 1, up + 3, z + 1, "minecraft:hopper");
		server.runCommand("item replace block 1 " + (up + 3) + " " + (z + 1) + " container.0 with wildcatter:leu_cylinder_30b 1");
		BlockPos emptiesOut = new BlockPos(1, y, z + 1);
		put(server, 1, y, z + 1, "minecraft:hopper");
		put(server, 1, up + 3, z + 40, "minecraft:hopper");
		server.runCommand("item replace block 1 " + (up + 3) + " " + (z + 40) + " container.0 with wildcatter:zircaloy_tube 16");
		server.runCommand("item replace block 1 " + (up + 3) + " " + (z + 40) + " container.1 with wildcatter:steel_ingot 1");
		put(server, 1, up + 3, z + 50, "minecraft:hopper");
		server.runCommand("item replace block 1 " + (up + 3) + " " + (z + 50) + " container.0 with wildcatter:helium_cylinder 1");
		put(server, 1, up + 6, z + 58, "minecraft:hopper");
		server.runCommand("item replace block 1 " + (up + 6) + " " + (z + 58) + " container.0 with wildcatter:guide_thimble 25");
		server.runCommand("item replace block 1 " + (up + 6) + " " + (z + 58) + " container.1 with wildcatter:spacer_grid 6");
		server.runCommand("item replace block 1 " + (up + 6) + " " + (z + 58) + " container.2 with wildcatter:steel_ingot 1");
		BlockPos assemblyOut = new BlockPos(1, y, z + 58);
		put(server, 1, y, z + 58, "minecraft:hopper");
		fillTank(server, bullet, dev.wildcatter.fluid.Product.LPG, 20_000);
		water(server, reformer, kiln);
		// 480 V: two sets on a busway north-west, an 800 A busway down x -2, a 500 kcmil stub to each box at x -1
		BlockPos[] sets = {new BlockPos(-12, up, z - 8), new BlockPos(-4, up, z - 8)};
		for (BlockPos set : sets) {
			genset(server, set, north, false);                                                   // x -17..-12 and -9..-4, z-8..z-7
			put(server, set.getX() + 1, up, z - 8, "wildcatter:busway_industrial");
		}
		for (int x = -11; x <= -3; x++) {
			put(server, x, up, z - 9, "wildcatter:busway_industrial");
		}
		for (int zz = z - 9; zz <= z + 54; zz++) {
			put(server, -2, up, zz, "wildcatter:busway_industrial");
		}
		exportBlueprint(server, "fuel_plant", new BlockPos(-20, y, z - 12), new BlockPos(24, y + 14, z + 66));   // the library (docs/blueprints.md)
		for (BlockPos set : sets) {
			startGenset(context, server, set, north, 2000);
		}
		// the sets on the bus first, then each part's stub
		for (int zz : new int[] {0, 4, 14, 18, 22, 34, 38, 48, 54}) {
			put(server, -1, up, z + zz, "wildcatter:cable_industrial");
			sizeCables(server, new BlockPos(-1, up, z + zz), new BlockPos(-1, up, z + zz), WildcatterBlocks.CABLE_INDUSTRIAL, dev.wildcatter.energy.ConductorSize.KCMIL_500);
			context.waitTicks(40);
			String set = read(server, -12, up, z - 8);
			check(set.contains("RUNNING") && set.contains("breaker CLOSED"), "the sets should carry the part at z+" + zz + ": " + set + " | " + read(server, 0, up, z + zz)
					+ " | stub " + read(server, -1, up, z + zz));
		}
		for (int i = 0; i < 12; i++) {
			context.waitTicks(50);
			water(server, reformer, kiln);
		}
		String head = " | autoclave " + read(server, 0, up, z) + " | kiln " + read(server, 0, up, z + 4) + " | reformer " + read(server, 8, up, z + 2) + " | set " + read(server, -12, up, z - 8);
		check(read(server, 0, up, z).contains("vaporizing"), "the autoclave should vaporize the 30B's UF6: " + head);
		check(read(server, 0, up, z + 4).contains("converting"), "the kiln should turn the UF6 into UO2 on the reformer's hydrogen: " + head);
		check(read(server, 0, up, z + 54).contains("pulling rods"), "the fixture should build its skeleton from its hopper: " + read(server, 0, up, z + 54));
		// uranium in each stage: the next one runs
		server.computeOnServer(srv -> {
			for (BlockPos at : new BlockPos[] {kiln, granulator, press, furnace, grinder}) {
				((dev.wildcatter.block.FuelStage) srv.overworld().getBlockEntity(at)).addFuelU(1.0);
			}
			((dev.wildcatter.block.FuelStage) srv.overworld().getBlockEntity(loader)).addFuelU(dev.wildcatter.fluid.Fuel.rodU());
			return null;
		});
		for (int i = 0; i < 4; i++) {
			context.waitTicks(50);
			water(server, reformer, kiln);
		}
		String line = " | granulator " + read(server, 0, up, z + 14) + " | press " + read(server, 0, up, z + 18) + " | furnace " + read(server, 0, up, z + 22)
				+ " | grinder " + read(server, 0, up, z + 34) + " | loader " + read(server, 0, up, z + 38) + " | welder " + read(server, 0, up, z + 48);
		check(read(server, 0, up, z + 14).contains("granulating"), "the granulator should run: " + line);
		check(read(server, 0, up, z + 18).contains("pressing"), "the press should run: " + line);
		check(read(server, 0, up, z + 22).contains("sintering"), "the sintering furnace should run on hydrogen: " + line);
		check(read(server, 0, up, z + 34).contains("grinding"), "the grinder should run: " + line);
		check(read(server, 0, up, z + 38).contains("loading"), "the rod loader should load a tube: " + line);
		check(read(server, 0, up, z + 48).contains("welding"), "the welder should fill and weld a rod: " + line);
		// 263 rods in the skeleton and one in the rack: the assembly is finished
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.AssemblyFixtureBlockEntity) srv.overworld().getBlockEntity(fixture)).setRodsForTest(Tuning.ASSEMBLY_RODS - 1);
			((dev.wildcatter.block.RodWelderBlockEntity) srv.overworld().getBlockEntity(welder)).setRackForTest(1);
			((dev.wildcatter.block.FuelLineBlockEntity) srv.overworld().getBlockEntity(autoclave)).setUf6ForTest(0.05);
			return null;
		});
		int assemblies = 0, empties = 0;
		for (int i = 0; i < 20 && (assemblies == 0 || empties == 0); i++) {
			context.waitTicks(20);
			assemblies = countIn(server, assemblyOut, WildcatterBlocks.FUEL_ASSEMBLY.asItem());
			empties = countIn(server, emptiesOut, WildcatterItems.EMPTY_30B_CYLINDER);
		}
		check(assemblies > 0, "a finished fuel assembly should drop into the hopper under the fixture: " + read(server, 0, up, z + 54) + " | " + read(server, 0, up, z + 48));
		check(empties > 0, "the emptied 30B should drop under the autoclave: " + read(server, 0, up, z));
		placeBig(server, WildcatterBlocks.FUEL_ASSEMBLY, new BlockPos(5, up, z + 58), north);
		check(stateAt(server, new BlockPos(5, up + 4, z + 58)).is(WildcatterBlocks.FUEL_ASSEMBLY), "a fuel assembly should stand five blocks tall");
		server.runCommand("tp @p 14 " + (up + 9) + " " + (z + 66) + " 140 25");
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-fuel");
		System.out.println("[wildcatter-test] fuel ok: " + read(server, 0, up, z + 4) + " | " + read(server, 0, up, z + 22) + " | " + read(server, 0, up, z + 48)
				+ " | " + read(server, 0, up, z + 54) + " | assemblies " + assemblies + ", empty 30Bs " + empties);
	}

	// ---- Phase 8a: the boron plant and the nuclear island (docs/multiblocks/boron.md, reactor.md) -----------------------------

	/**
	 * The boron plant down x 0-2, every part facing north: the dissolver with the crystallizer against it, the B4C furnace and the
	 * RCCA bench. Borax, boric acid, coal, pellets and steel by hopper; sulfuric acid from a stock tank; a diesel set on a busway.
	 */
	private void boron(ClientGameTestContext context, TestServerContext server) {
		int z = -2700;
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand("tp @p 10 " + (up + 14) + " " + (z + 28) + " 180 40");
		context.waitTicks(20);
		for (int zz = z - 12; zz <= z + 24; zz += 21) {
			server.runCommand("fill -20 " + (y - 1) + " " + zz + " 16 " + (y + 10) + " " + Math.min(zz + 20, z + 24) + " minecraft:air");
		}
		server.runCommand("fill -20 " + (y - 1) + " " + (z - 12) + " 16 " + (y - 1) + " " + (z + 24) + " minecraft:stone");
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		BlockPos dissolver = new BlockPos(0, up, z);
		placeBig(server, WildcatterBlocks.BORAX_DISSOLVER, dissolver, north);                     // z..z+2
		BlockPos crystallizer = new BlockPos(0, up, z + 3);
		placeBig(server, WildcatterBlocks.BORIC_ACID_CRYSTALLIZER, crystallizer, north);          // z+3..z+5
		BlockPos furnace = new BlockPos(0, up, z + 6);
		placeBig(server, WildcatterBlocks.B4C_FURNACE, furnace, north);                           // z+6..z+11
		BlockPos bench = new BlockPos(0, up, z + 12);
		placeBig(server, WildcatterBlocks.RCCA_BENCH, bench, north);                              // z+12..z+19
		BlockPos acidTank = new BlockPos(6, up, z);
		placeBig(server, WildcatterBlocks.STOCK_TANK, acidTank, north);
		for (int x = 3; x <= 5; x++) {
			put(server, x, up, z + 1, "wildcatter:gas_line");
		}
		fillTank(server, acidTank, dev.wildcatter.fluid.Product.SULFURIC_ACID, 20_000);
		put(server, 1, up + 4, z + 1, "minecraft:hopper");
		server.runCommand("item replace block 1 " + (up + 4) + " " + (z + 1) + " container.0 with wildcatter:borax 1");
		BlockPos bagsOut = new BlockPos(1, y, z + 4);
		put(server, 1, y, z + 4, "minecraft:hopper");
		put(server, 1, up + 4, z + 8, "minecraft:hopper");
		server.runCommand("item replace block 1 " + (up + 4) + " " + (z + 8) + " container.0 with wildcatter:boric_acid 4");
		server.runCommand("item replace block 1 " + (up + 4) + " " + (z + 8) + " container.1 with minecraft:coal 2");
		BlockPos traysOut = new BlockPos(1, y, z + 8);
		put(server, 1, y, z + 8, "minecraft:hopper");
		put(server, 1, up + 3, z + 14, "minecraft:hopper");
		server.runCommand("item replace block 1 " + (up + 3) + " " + (z + 14) + " container.0 with wildcatter:b4c_pellets 9");
		server.runCommand("item replace block 1 " + (up + 3) + " " + (z + 14) + " container.1 with wildcatter:steel_ingot 1");
		BlockPos rccaOut = new BlockPos(1, y, z + 14);
		put(server, 1, y, z + 14, "minecraft:hopper");
		water(server, dissolver);
		// 480 V: two sets north-west on a busway, an 800 A busway down x -2, a stub to each box at x -1 (as the fuel scene)
		BlockPos[] sets = {new BlockPos(-12, up, z - 8), new BlockPos(-4, up, z - 8)};
		for (BlockPos set : sets) {
			genset(server, set, north, false);                                                     // x -17..-12 and -9..-4, z-8..z-7
			put(server, set.getX() + 1, up, z - 8, "wildcatter:busway_industrial");
		}
		for (int x = -11; x <= -3; x++) {
			put(server, x, up, z - 9, "wildcatter:busway_industrial");
		}
		for (int zz = z - 9; zz <= z + 12; zz++) {
			put(server, -2, up, zz, "wildcatter:busway_industrial");
		}
		exportBlueprint(server, "boron", new BlockPos(-20, y, z - 12), new BlockPos(16, y + 10, z + 24));   // the library (docs/blueprints.md)
		for (BlockPos set : sets) {
			startGenset(context, server, set, north, 2000);
		}
		for (int zz : new int[] {0, 3, 6, 12}) {
			put(server, -1, up, z + zz, "wildcatter:cable_industrial");
			sizeCables(server, new BlockPos(-1, up, z + zz), new BlockPos(-1, up, z + zz), WildcatterBlocks.CABLE_INDUSTRIAL, dev.wildcatter.energy.ConductorSize.KCMIL_500);
			context.waitTicks(40);
		}
		for (int i = 0; i < 6; i++) {
			context.waitTicks(50);
			water(server, dissolver);
		}
		String all = " | dissolver " + read(server, 0, up, z) + " | crystallizer " + read(server, 0, up, z + 3) + " | furnace " + read(server, 0, up, z + 6)
				+ " | bench " + read(server, 0, up, z + 12) + " | set " + read(server, -12, up, z - 8);
		check(read(server, 0, up, z).contains("dissolving"), "the dissolver should dissolve borax in sulfuric acid: " + all);
		check(read(server, 0, up, z + 3).contains("crystallizing"), "the crystallizer should take the liquor: " + all);
		check(read(server, 0, up, z + 6).contains("reducing"), "the furnace should make B4C: " + all);
		check(read(server, 0, up, z + 12).contains("rodlets"), "the bench should start an RCCA: " + all);
		server.computeOnServer(srv -> {
			for (BlockPos at : new BlockPos[] {crystallizer, furnace, bench}) {
				((dev.wildcatter.block.BoronLineBlockEntity) srv.overworld().getBlockEntity(at)).setNearlyDoneForTest();
			}
			return null;
		});
		int bags = 0, trays = 0, rccas = 0;
		for (int i = 0; i < 20 && (bags == 0 || trays == 0 || rccas == 0); i++) {
			context.waitTicks(20);
			water(server, dissolver);
			bags = countIn(server, bagsOut, WildcatterItems.BORIC_ACID);
			trays = countIn(server, traysOut, WildcatterItems.B4C_PELLETS);
			rccas = countIn(server, rccaOut, WildcatterItems.RCCA);
		}
		check(bags > 0, "a bag of boric acid should drop under the crystallizer: " + read(server, 0, up, z + 3));
		check(trays > 0, "a tray of B4C pellets should drop under the furnace: " + read(server, 0, up, z + 6));
		check(rccas > 0, "an RCCA should drop under the bench: " + read(server, 0, up, z + 12));
		server.runCommand("tp @p 12 " + (up + 7) + " " + (z + 24) + " 135 25");
		context.waitTicks(3);
		context.takeScreenshot("wildcatter-boron");
		System.out.println("[wildcatter-test] boron ok: " + read(server, 0, up, z) + " | " + read(server, 0, up, z + 6) + " | bags " + bags + ", trays " + trays
				+ ", RCCAs " + rccas);
	}

	/**
	 * The nuclear island (docs/multiblocks/reactor.md), laid out as the spec with the vessel's main cell at 0, up, z: loop A north,
	 * loop B south, the pressurizer against A, the CVCS west, the console against the vessel's east face (raised, so the feedwater
	 * pipe runs under it). Power: three diesel sets through a backward unit substation for the pumps' 4.16 kV, two sets on a busway
	 * for 480 V, and a set of its own for the aux feed pump.
	 */
	private void reactor(ClientGameTestContext context, TestServerContext server) {
		int z = -2900;
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand("tp @p 12 " + (up + 26) + " " + (z + 24) + " 160 45");
		context.waitTicks(20);
		for (int zz = z - 20; zz <= z + 20; zz += 20) {
			server.runCommand("fill -26 " + (y - 1) + " " + zz + " 20 " + (y + 15) + " " + Math.min(zz + 19, z + 20) + " minecraft:air");
		}
		server.runCommand("fill -26 " + (y - 1) + " " + (z - 20) + " 20 " + (y - 1) + " " + (z + 20) + " minecraft:stone");
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		BlockPos vessel = new BlockPos(0, up, z);
		placeBig(server, WildcatterBlocks.REACTOR_VESSEL, vessel, north);                         // x 0-2, z..z+2, y up..up+9
		placeBig(server, WildcatterBlocks.CRDM_HEAD, new BlockPos(0, up + 10, z), north);
		placeBig(server, WildcatterBlocks.STEAM_GENERATOR, new BlockPos(0, up, z - 3), north);    // loop A, z-3..z-1
		BlockPos pumpA = new BlockPos(0, up, z - 5);
		placeBig(server, WildcatterBlocks.REACTOR_COOLANT_PUMP, pumpA, north);                    // z-5..z-4
		placeBig(server, WildcatterBlocks.PRESSURIZER, new BlockPos(-2, up, z - 3), north);       // x -2..-1, z-3..z-2
		placeBig(server, WildcatterBlocks.STEAM_GENERATOR, new BlockPos(0, up, z + 3), north);    // loop B, z+3..z+5
		BlockPos pumpB = new BlockPos(0, up, z + 6);
		placeBig(server, WildcatterBlocks.REACTOR_COOLANT_PUMP, pumpB, north);                    // z+6..z+7
		BlockPos cvcs = new BlockPos(-3, up, z);
		placeBig(server, WildcatterBlocks.CVCS_SKID, cvcs, north);                                // x -3..-1, z..z+5
		placeBig(server, WildcatterBlocks.REACTOR_CONSOLE, new BlockPos(3, up + 3, z + 1), north);   // x 3-5, raised
		BlockPos afw = new BlockPos(12, up, z);
		placeBig(server, WildcatterBlocks.AUX_FEED_PUMP, afw, north);                             // x 12-13, z..z+3
		// feedwater: south of the aux feed pump, west along z+4 past loop B's steam generator, north up x 3 to loop A's
		for (int x = 3; x <= 12; x++) {
			put(server, x, up, z + 4, "wildcatter:gas_line");
		}
		for (int zz = z - 1; zz <= z + 3; zz++) {
			put(server, 3, up, zz, "wildcatter:gas_line");
		}
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.ReactorVesselBlockEntity) srv.overworld().getBlockEntity(vessel)).setHotStandbyForTest(1500);
			((dev.wildcatter.block.CvcsBlockEntity) srv.overworld().getBlockEntity(cvcs)).setAcidForTest(500);
			((dev.wildcatter.block.ReactorCoolantPumpBlockEntity) srv.overworld().getBlockEntity(pumpB)).setOnForTest(false);
			return null;
		});
		water(server, cvcs, afw);
		// 480 V: two sets south-west on a busway up x -5, stubs to the CVCS, the pressurizer and (up a riser) the CRDM head
		BlockPos[] lvSets = {new BlockPos(-14, up, z + 14), new BlockPos(-6, up, z + 14)};
		for (BlockPos set : lvSets) {
			genset(server, set, north, false);                                                     // x -19..-14 and -11..-6, z+14..z+15
			put(server, set.getX() + 1, up, z + 14, "wildcatter:busway_industrial");
		}
		for (int x = -13; x <= -5; x++) {
			put(server, x, up, z + 13, "wildcatter:busway_industrial");
		}
		for (int zz = z - 3; zz <= z + 13; zz++) {
			put(server, -5, up, zz, "wildcatter:busway_industrial");
		}
		// 4.16 kV: three sets north-west, a backward unit substation, a cable round the north to pump A and down x 14 to pump B
		BlockPos[] mvSets = {new BlockPos(-22, up, z - 13), new BlockPos(-14, up, z - 13), new BlockPos(-6, up, z - 13)};
		for (BlockPos set : mvSets) {
			genset(server, set, north, false);                                                     // z-13..z-12
			put(server, set.getX() + 1, up, z - 13, "wildcatter:busway_industrial");
		}
		for (int x = -21; x <= 2; x++) {
			put(server, x, up, z - 14, "wildcatter:busway_industrial");
		}
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, new BlockPos(2, up, z - 14), net.minecraft.core.Direction.EAST, false);   // x 3-6, z-15..z-14
		BlockPos[] mvSetsB = {new BlockPos(2, up, z + 14), new BlockPos(10, up, z + 14)};
		for (BlockPos set : mvSetsB) {
			genset(server, set, north, false);                                                     // x -3..2 and 5..10, z+14..z+15
			put(server, set.getX() + 1, up, z + 14, "wildcatter:busway_industrial");
		}
		for (int x = 3; x <= 11; x++) {
			put(server, x, up, z + 13, "wildcatter:busway_industrial");
		}
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, new BlockPos(11, up, z + 13), net.minecraft.core.Direction.EAST, false);   // x 12-15, z+12..z+13
		// the aux feed pump's own set, its terminal against the pump's box
		BlockPos afwSet = new BlockPos(10, up, z);
		genset(server, afwSet, north, false);                                                     // x 5-10, z..z+1
		for (BlockPos set : lvSets) {
			startGenset(context, server, set, north, 2000);
		}
		for (BlockPos set : mvSets) {
			startGenset(context, server, set, north, 2000);
		}
		for (BlockPos set : mvSetsB) {
			startGenset(context, server, set, north, 2000);
		}
		put(server, 11, up, z, "wildcatter:cable_industrial");   // a lone set is solved only with a load on its bus
		sizeCables(server, new BlockPos(11, up, z), new BlockPos(11, up, z), WildcatterBlocks.CABLE_INDUSTRIAL, dev.wildcatter.energy.ConductorSize.KCMIL_500);
		exportBlueprint(server, "pwr", new BlockPos(-26, y, z - 20), new BlockPos(20, y + 15, z + 20));   // the library (docs/blueprints.md)
		startGenset(context, server, afwSet, north, 2000);
		// 480 V stubs, one at a time: the CVCS, the pressurizer, the CRDM head's riser
		put(server, -4, up, z, "wildcatter:cable_industrial");
		context.waitTicks(40);
		put(server, -4, up, z - 3, "wildcatter:cable_industrial");
		put(server, -3, up, z - 3, "wildcatter:cable_industrial");
		sizeCables(server, new BlockPos(-4, up, z - 3), new BlockPos(-3, up, z - 3), WildcatterBlocks.CABLE_INDUSTRIAL, dev.wildcatter.energy.ConductorSize.KCMIL_500);
		context.waitTicks(40);
		for (int yy = up; yy <= up + 10; yy++) {
			put(server, -4, yy, z - 1, "wildcatter:cable_industrial");
		}
		for (int x = -3; x <= -1; x++) {
			put(server, x, up + 10, z - 1, "wildcatter:cable_industrial");
		}
		put(server, -1, up + 10, z, "wildcatter:cable_industrial");
		context.waitTicks(40);
		String lv = read(server, -14, up, z + 14);
		check(lv.contains("RUNNING") && lv.contains("breaker CLOSED"), "the 480 V sets should carry the island's 480 V loads: " + lv);
		// 4.16 kV to the pumps
		java.util.List<int[]> mv = new java.util.ArrayList<>();
		for (int zz = z - 14; zz <= z - 12; zz++) {
			mv.add(new int[] {7, zz});
		}
		for (int x = -1; x <= 14; x++) {
			mv.add(new int[] {x, z - 12});
		}
		for (int zz = z - 11; zz <= z - 5; zz++) {
			mv.add(new int[] {-1, zz});
		}
		// pump B on an island of its own: two sets south, a second backward unit substation, west along z+10 and up x -1
		for (int zz = z + 10; zz <= z + 13; zz++) {
			mv.add(new int[] {16, zz});
		}
		for (int x = -1; x <= 15; x++) {
			mv.add(new int[] {x, z + 10});
		}
		for (int zz = z + 6; zz <= z + 9; zz++) {
			mv.add(new int[] {-1, zz});
		}
		for (int[] c : mv) {
			put(server, c[0], up, c[1], "wildcatter:cable_mv");
		}
		context.waitTicks(300);
		String island = " | vessel " + read(server, 0, up, z) + " | pump A " + read(server, 0, up, z - 5) + " | MV set " + read(server, -6, up, z - 13)
				+ " | CRDM " + read(server, 0, up + 10, z);
		check(read(server, 0, up, z - 5).contains("flow 100"), "pump A should run on 4.16 kV: " + island);
		check(read(server, 0, up, z).contains("shut down") && read(server, 0, up, z).contains("pressurizer 15."), "the reactor should stand shut down at hot standby, 15.5 MPa: " + island);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.ReactorCoolantPumpBlockEntity) srv.overworld().getBlockEntity(pumpB)).setOnForTest(true);
			return null;
		});
		context.waitTicks(300);
		check(read(server, 0, up, z + 6).contains("flow 100"), "pump B should start too: " + read(server, 0, up, z + 6) + " | " + read(server, 2, up, z + 14));
		// the startup: the shutdown bank out and the core just short of critical (the hooks shorten the waits), the setpoint 2 %
		dev.wildcatter.fluid.Reactor[] r = new dev.wildcatter.fluid.Reactor[1];
		server.computeOnServer(srv -> {
			r[0] = ((dev.wildcatter.block.ReactorVesselBlockEntity) srv.overworld().getBlockEntity(vessel)).reactor();
			r[0].setSetpoint(0.02);
			r[0].setRodsForTest(Tuning.ROD_STEPS, 100);
			r[0].holdPowerForTest(2e-3);
			return null;
		});
		boolean critical = false;
		double peak = 0;
		for (int i = 0; i < 150; i++) {
			context.waitTicks(40);
			water(server, cvcs, afw);
			double rate = server.computeOnServer(srv -> r[0].startupRate());
			double p = server.computeOnServer(srv -> r[0].power());
			critical |= rate > 0.2;
			peak = Math.max(peak, p);
			if (critical && p > 0.019) {
				break;
			}
		}
		String now = " | vessel " + read(server, 0, up, z) + " | SG A " + read(server, 0, up, z - 3);
		check(critical, "the reactor should go critical with a positive startup rate: " + now);
		for (int i = 0; i < 15; i++) {
			context.waitTicks(40);
			water(server, cvcs, afw);
		}
		double held = server.computeOnServer(srv -> r[0].power());
		now = " | vessel " + read(server, 0, up, z) + " | SG A " + read(server, 0, up, z - 3) + " | SG B " + read(server, 0, up, z + 3) + " | AFW " + read(server, 12, up, z);
		check(!r[0].tripped() && Math.abs(held - 0.02) < 0.005, "it should hold about 2 % on bank D: " + held + now);
		double vented = server.computeOnServer(srv -> r[0].vented(0) + r[0].vented(1));
		check(vented > 0, "the dump valves should vent the steam: " + now);
		check(server.computeOnServer(srv -> r[0].sgLevel(0)) > 0.9, "the aux feed pump should hold the levels: " + now);
		// pump B stopped at 2 %: it runs on one loop
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.ReactorCoolantPumpBlockEntity) srv.overworld().getBlockEntity(pumpB)).setOnForTest(false);
			return null;
		});
		context.waitTicks(400);
		check(!r[0].tripped(), "one loop below P-8 should run on: " + read(server, 0, up, z));
		server.runCommand("tp @p 15 " + (up + 17) + " " + (z + 17) + " 140 32");
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-reactor");
		String running = read(server, 0, up, z);
		// 13g (docs/multiblocks/plc.md): rods and boron from a real program, the temperature on an RTD input, and the trips beating it all
		BlockPos bench = plcBench(context, server, z + 17, dev.wildcatter.control.PlcModuleItem.Type.DI16, dev.wildcatter.control.PlcModuleItem.Type.DO16, dev.wildcatter.control.PlcModuleItem.Type.AI8, dev.wildcatter.control.PlcModuleItem.Type.AO4,
				dev.wildcatter.control.PlcModuleItem.Type.RTD4);
		controlRun(server, "wildcatter:junction_box", new int[] {14, up + 1, z + 17});   // beside the cabinet's top, where a cable cell would touch three things
		controlRun(server, "wildcatter:control_cable", straight(14, up + 2, z + 17, 14, up + 6, z + 17));
		controlRun(server, "wildcatter:control_cable", straight(15, up + 6, z + 17, 16, up + 6, z + 17));
		controlRun(server, "wildcatter:control_cable", straight(16, up + 6, z + 16, 16, up + 6, z + 2));
		controlRun(server, "wildcatter:control_cable", straight(15, up + 6, z + 2, 3, up + 6, z + 2));
		context.waitTicks(25);
		java.util.function.Function<net.minecraft.server.MinecraftServer, dev.wildcatter.control.PlcCabinetBlockEntity> plc = srv -> (dev.wildcatter.control.PlcCabinetBlockEntity) srv.overworld().getBlockEntity(bench);
		String signals = server.computeOnServer(srv -> {
			var box = plc.apply(srv);
			String out = "";
			for (String one : new String[] {box.land(vessel, 0, "%QW48"), box.land(vessel, 1, "%QW50"), box.land(vessel, 4, "%IW32"), box.land(vessel, 7, "%IW34"), box.land(vessel, 8, "%IW36"), box.land(vessel, 5, "%IW64"), box.land(vessel, 11, "%IX0.0"), box.land(vessel, 3, "%QX16.0")}) {
				if (one != null) {
					out += one + "; ";
				}
			}
			var control = dev.wildcatter.control.ControlNet.deviceAt(srv.overworld(), vessel);
			control.cycleHoa(vessel);
			control.cycleHoa(vessel);
			java.util.List<?> errs = box.download("""
					PROGRAM CORE
					VAR
						rod_out AT %QW48 : WORD;
						boron_out AT %QW50 : WORD;
						power_raw AT %IW32 : WORD;
						ppm_raw AT %IW34 : WORD;
						bank_raw AT %IW36 : WORD;
						tavg_tenths AT %IW64 : WORD;
						tripped AT %IX0.0 : BOOL;
						trip AT %QX16.0 : BOOL;
						ready : BOOL;
						rods : REAL;
						ppm : REAL;
						want_trip : BOOL;
						tavg : REAL;
						bank : REAL;
					END_VAR
					bank := INT_TO_REAL(WORD_TO_INT(bank_raw)) / 320.0;
					tavg := INT_TO_REAL(WORD_TO_INT(tavg_tenths)) / 10.0;
					IF NOT ready THEN
						rods := bank;
						ppm := INT_TO_REAL(WORD_TO_INT(ppm_raw)) / 32000.0 * 2500.0;
						ready := WORD_TO_INT(bank_raw) > 0;
					END_IF;
					rod_out := INT_TO_WORD(REAL_TO_INT(rods * 320.0));
					boron_out := INT_TO_WORD(REAL_TO_INT(ppm / 2500.0 * 32000.0));
					trip := want_trip;
					END_PROGRAM""");
			if (!errs.isEmpty()) {
				out += errs.toString();
			}
			box.cpu().setMode(dev.wildcatter.plc.Cpu.Mode.RUN);
			return out;
		});
		check(signals.isEmpty(), "the reactor's signals should land and its program compile: " + signals + " | " + read(server, 15, up, z + 17));
		context.waitTicks(60);
		check(server.computeOnServer(srv -> plc.apply(srv).net().devices().size()) >= 1, "the cabinet should reach the vessel: " + read(server, 15, up, z + 17));
		double bank0 = server.computeOnServer(srv -> r[0].bankD());
		double powerNow = server.computeOnServer(srv -> ((Number) plc.apply(srv).cpu().read("CORE.TAVG")).doubleValue());
		check(Math.abs(powerNow - server.computeOnServer(srv -> r[0].tavg())) < 2, "an RTD input should read Tavg in tenths: " + powerNow + " against " + server.computeOnServer(srv -> r[0].tavg()));
		check(server.computeOnServer(srv -> r[0].rodDemand()) >= 0, "the PLC should hold the rods' demand");
		check(Math.abs(bank0 - server.computeOnServer(srv -> r[0].rodDemand())) < 8, "bumpless: its demand starts where the rods are: " + bank0 + " against " + server.computeOnServer(srv -> r[0].rodDemand()));
		// rods in by the PLC's demand, and power falls; boron target by the PLC's
		double downTo = bank0 / Tuning.ROD_STEPS * 100 - 10;
		server.runOnServer(srv -> plc.apply(srv).cpu().write("CORE.RODS", downTo));
		server.runOnServer(srv -> plc.apply(srv).cpu().write("CORE.PPM", 1450.0));
		context.waitTicks(300);
		double bank1 = server.computeOnServer(srv -> r[0].bankD());
		System.out.println("[wildcatter-test] plc reactor: bank D " + bank0 + " to " + bank1 + ", target " + server.computeOnServer(srv -> r[0].targetPpm()) + " | " + read(server, 0, up, z));
		check(Math.abs(bank1 / Tuning.ROD_STEPS * 100 - downTo) < 2.5, "bank D should go to the PLC's demand: " + bank1 + " for " + downTo + " %");
		check(Math.abs(server.computeOnServer(srv -> r[0].targetPpm()) - 1450) < 3, "the boron target should be the PLC's: " + server.computeOnServer(srv -> r[0].targetPpm()));
		context.takeScreenshot("wildcatter-plc-reactor");
		// the trips beat it: the PLC demands every rod out, and high flux drops them all the same
		server.runOnServer(srv -> plc.apply(srv).cpu().write("CORE.RODS", 100.0));
		server.computeOnServer(srv -> {
			r[0].holdPowerForTest(0.3);
			return null;
		});
		context.waitTicks(200);
		String beaten = read(server, 0, up, z);
		System.out.println("[wildcatter-test] plc reactor: the PLC demanding all out, " + beaten);
		check(r[0].tripped() && r[0].bankD() == 0 && r[0].shutdownBank() == 0, "a trip should drop the rods against the PLC's demand: " + beaten);
		check(server.computeOnServer(srv -> r[0].rodDemand()) > 0, "with its demand still standing");
		check(Boolean.TRUE.equals(server.computeOnServer(srv -> plc.apply(srv).cpu().read("CORE.TRIPPED"))), "and the PLC reads the trip");
		// a hand's reset, as ever; a PLC's own manual trip works too
		server.computeOnServer(srv -> {
			r[0].setSetpoint(0);
			return null;
		});
		context.waitTicks(160);
		check(!r[0].tripped(), "the console's reset should clear it: " + read(server, 0, up, z));
		server.runOnServer(srv -> plc.apply(srv).cpu().write("CORE.WANT_TRIP", true));
		context.waitTicks(60);
		check(r[0].tripped() && read(server, 0, up, z).contains("MANUAL TRIP"), "a PLC may always add a trip: " + read(server, 0, up, z));
		server.runOnServer(srv -> plc.apply(srv).cpu().write("CORE.WANT_TRIP", false));
		// its selector back to HAND, for what follows
		server.runOnServer(srv -> {
			var control = dev.wildcatter.control.ControlNet.deviceAt(srv.overworld(), vessel);
			control.cycleHoa(vessel);
		});
		context.waitTicks(40);
		check(server.computeOnServer(srv -> r[0].rodDemand()) < 0, "out of AUTO the reactor's own control has the rods back");
		server.computeOnServer(srv -> {
			r[0].setSetpoint(0);
			return null;
		});
		context.waitTicks(100);
		// a manual trip
		server.computeOnServer(srv -> {
			r[0].setSetpoint(-1);
			return null;
		});
		context.waitTicks(400);
		for (int i = 0; i < 40 && server.computeOnServer(srv -> r[0].power()) >= 0.001; i++) {
			context.waitTicks(100);   // the excursion above left delayed neutrons to die away
		}
		String trip = read(server, 0, up, z);
		check(r[0].tripped() && trip.contains("MANUAL TRIP") && r[0].power() < 0.001 && r[0].bankD() == 0 && r[0].shutdownBank() == 0,
				"a manual trip should drop all rods and the power: " + trip);
		// reset, and cut the CRDM head's power while the shutdown bank comes out
		server.computeOnServer(srv -> {
			r[0].setSetpoint(0);
			return null;
		});
		context.waitTicks(100);
		put(server, -1, up + 10, z, "minecraft:air");
		context.waitTicks(100);
		String cut = read(server, 0, up, z);
		check(cut.contains("CRDM POWER LOST"), "losing the CRDM head's power should trip it: " + cut);
		System.out.println("[wildcatter-test] reactor ok: " + running + " | " + trip + " | " + cut);
	}

	/**
	 * The turbine island (8b, docs/multiblocks/turbine-island.md) on the 8a reactor island. The reactor island as in the reactor scene,
	 * vessel at 0, up, z; the turbine island east of it, valve chest at 20, z-6, laid out as the spec; two circulating water pumps in a
	 * dug pool north-east. Offsite power: the 16.5 MW gas turbine east on a 13.8 kV bus (its starter on a diesel set), with load banks,
	 * the new generator's breaker and the unit auxiliary transformer on that bus. Elevated trays keep the networks apart: 4.16 kV at
	 * up+14, 480 V busway at up+17, the 13.8 kV bus duct at up+16; steam at up+12/13, the main feedwater at up+3.
	 */
	private void turbineIsland(ClientGameTestContext context, TestServerContext server) {
		int z = -3100;
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand("tp @p 30 " + (up + 40) + " " + (z + 45) + " 180 45");
		context.waitTicks(20);
		for (int zz = z - 20; zz <= z + 40; zz += 14) {
			server.runCommand("fill -10 " + (y - 1) + " " + zz + " 80 " + (y + 20) + " " + Math.min(zz + 13, z + 40) + " minecraft:air");   // 91 x 22 x 14
		}
		for (int zz = z - 20; zz <= z + 40; zz += 300) {
			server.runCommand("fill -10 " + (y - 1) + " " + zz + " 80 " + (y - 1) + " " + (z + 40) + " minecraft:stone");
		}
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		// the intake: a pool x 33-42, z-12..z-5, three deep, in a stone box
		server.runCommand("fill 32 " + (up - 4) + " " + (z - 13) + " 43 " + (up - 1) + " " + (z - 4) + " minecraft:stone");
		server.runCommand("fill 33 " + (up - 3) + " " + (z - 12) + " 42 " + (up - 1) + " " + (z - 5) + " minecraft:water");
		BlockPos cw1 = new BlockPos(34, up - 3, z - 11), cw2 = new BlockPos(38, up - 3, z - 11);
		placeBig(server, WildcatterBlocks.CIRCULATING_WATER_PUMP, cw1, north);                    // x 34-36, z-11..z-9
		placeBig(server, WildcatterBlocks.CIRCULATING_WATER_PUMP, cw2, north);                    // x 38-40
		// the reactor island
		BlockPos vessel = new BlockPos(0, up, z);
		placeBig(server, WildcatterBlocks.REACTOR_VESSEL, vessel, north);
		placeBig(server, WildcatterBlocks.CRDM_HEAD, new BlockPos(0, up + 10, z), north);
		placeBig(server, WildcatterBlocks.STEAM_GENERATOR, new BlockPos(0, up, z - 3), north);
		BlockPos pumpA = new BlockPos(0, up, z - 5), pumpB = new BlockPos(0, up, z + 6);
		placeBig(server, WildcatterBlocks.REACTOR_COOLANT_PUMP, pumpA, north);
		placeBig(server, WildcatterBlocks.PRESSURIZER, new BlockPos(-2, up, z - 3), north);
		placeBig(server, WildcatterBlocks.STEAM_GENERATOR, new BlockPos(0, up, z + 3), north);
		placeBig(server, WildcatterBlocks.REACTOR_COOLANT_PUMP, pumpB, north);
		BlockPos cvcs = new BlockPos(-3, up, z);
		placeBig(server, WildcatterBlocks.CVCS_SKID, cvcs, north);
		placeBig(server, WildcatterBlocks.REACTOR_CONSOLE, new BlockPos(3, up + 3, z + 1), north);
		BlockPos afw = new BlockPos(12, up, z);
		placeBig(server, WildcatterBlocks.AUX_FEED_PUMP, afw, north);
		// the turbine island
		placeBig(server, WildcatterBlocks.TURBINE_VALVE_CHEST, new BlockPos(20, up, z - 6), north);
		BlockPos hp = new BlockPos(20, up, z - 3);
		placeBig(server, WildcatterBlocks.HP_TURBINE, hp, north);
		placeBig(server, WildcatterBlocks.MOISTURE_SEPARATOR_REHEATER, new BlockPos(24, up, z - 7), north);
		placeBig(server, WildcatterBlocks.LP_TURBINE, new BlockPos(20, up, z + 3), north);
		placeBig(server, WildcatterBlocks.MAIN_CONDENSER, new BlockPos(26, up, z + 3), north);
		placeBig(server, WildcatterBlocks.CONDENSATE_PUMP, new BlockPos(26, up, z + 11), north);
		placeBig(server, WildcatterBlocks.CONDENSATE_PUMP, new BlockPos(29, up, z + 11), north);
		BlockPos tg = new BlockPos(21, up, z + 11);
		placeBig(server, WildcatterBlocks.TG_GENERATOR_60, tg, north);                            // x 21-24, z+11..z+18
		placeBig(server, WildcatterBlocks.GEN_BREAKER_3000, new BlockPos(22, up, z + 19), north);   // x 22-23, z+19..z+20
		BlockPos da = new BlockPos(29, up, z + 13);
		placeBig(server, WildcatterBlocks.DEAERATOR, da, north);                                  // x 29-31, z+13..z+20
		placeBig(server, WildcatterBlocks.MAIN_FEED_PUMP, new BlockPos(27, up, z + 13), north);   // x 27-28, z+13..z+17
		placeBig(server, WildcatterBlocks.MAIN_FEED_PUMP, new BlockPos(27, up, z + 18), north);   // z+18..z+22
		placeBig(server, WildcatterBlocks.HP_FEEDWATER_HEATER, new BlockPos(27, up, z + 23), north);   // z+23..z+30
		// steam: up off both steam generators' tops, east at up+12 to the valve chest, down onto its top
		for (int zz = z - 2; zz <= z + 4; zz++) {
			put(server, 1, up + 13, zz, "wildcatter:gas_line");
		}
		put(server, 1, up + 12, z + 4, "wildcatter:gas_line");
		for (int x = 1; x <= 21; x++) {
			put(server, x, up + 12, z - 2, "wildcatter:gas_line");
		}
		for (int zz = z - 5; zz <= z - 3; zz++) {
			put(server, 21, up + 12, zz, "wildcatter:gas_line");
		}
		for (int yy = up + 4; yy <= up + 11; yy++) {
			put(server, 21, yy, z - 5, "wildcatter:gas_line");
		}
		// feedwater: the aux feed's header (as the reactor scene), and the main feed from the HP heaters over at up+3
		for (int x = 3; x <= 12; x++) {
			put(server, x, up, z + 4, "wildcatter:gas_line");
		}
		for (int zz = z - 1; zz <= z + 3; zz++) {
			put(server, 3, up, zz, "wildcatter:gas_line");
		}
		for (int yy = up; yy <= up + 3; yy++) {
			put(server, 27, yy, z + 31, "wildcatter:gas_line");
		}
		for (int x = 12; x <= 26; x++) {
			put(server, x, up + 3, z + 31, "wildcatter:gas_line");
		}
		for (int zz = z + 4; zz <= z + 30; zz++) {
			put(server, 12, up + 3, zz, "wildcatter:gas_line");
		}
		put(server, 12, up + 2, z + 4, "wildcatter:gas_line");
		put(server, 12, up + 1, z + 4, "wildcatter:gas_line");
		// circulating water: both pumps' heads, down x 37 and west along z+2 to the condenser's north face
		for (int zz = z - 9; zz <= z + 2; zz++) {
			put(server, 37, up, zz, "wildcatter:gas_line");
		}
		for (int x = 38; x <= 41; x++) {
			put(server, x, up, z - 8, "wildcatter:gas_line");
		}
		put(server, 41, up, z - 9, "wildcatter:gas_line");
		for (int x = 27; x <= 36; x++) {
			put(server, x, up, z + 2, "wildcatter:gas_line");
		}
		// offsite power: the gas turbine east, its starter on a diesel set, the 13.8 kV bus with two load banks
		BlockPos gt = new BlockPos(60, up, z);
		placeBig(server, WildcatterBlocks.GAS_TURBINE, gt, north);                                // x 60-63, z..z+9
		placeBig(server, WildcatterBlocks.GT_INLET_HOUSE, new BlockPos(60, up, z - 4), north);
		placeBig(server, WildcatterBlocks.GT_EXHAUST_STACK, new BlockPos(60, up, z + 10), north);   // x 60-62, z+10..z+12
		placeBig(server, WildcatterBlocks.GT_GENERATOR, new BlockPos(64, up, z + 1), north);      // x 64-67, z+1..z+6
		placeBig(server, WildcatterBlocks.GEN_BREAKER, new BlockPos(65, up, z + 7), north);       // x 65-66, z+7..z+8
		BlockPos bank1 = new BlockPos(65, up, z + 10), bank2 = new BlockPos(68, up, z + 10);
		placeBig(server, WildcatterBlocks.LOAD_BANK, bank1, north);
		placeBig(server, WildcatterBlocks.LOAD_BANK, bank2, north);
		BlockPos fuelTank = new BlockPos(54, up, z + 4);
		placeBig(server, WildcatterBlocks.STOCK_TANK, fuelTank, north);                           // x 54-57, z+4..z+7
		pipes(server, z, new int[][] {{58, 0, 6}, {59, 0, 6}});
		fillTank(server, fuelTank, dev.wildcatter.fluid.Product.KEROSENE, 40_000);
		BlockPos starterSet = new BlockPos(50, up, z + 2);
		genset(server, starterSet, north, false);                                                // x 45-50, z+2..z+3
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, starterSet, net.minecraft.core.Direction.EAST, false);   // x 51-54, z+1..z+2
		for (int x = 55; x <= 59; x++) {
			put(server, x, up, z + 2, "wildcatter:cable_mv");
		}
		BlockPos uat = new BlockPos(72, up, z + 3);
		placeBig(server, WildcatterBlocks.UNIT_AUX_TRANSFORMER, uat, north);                      // x 71-74, z..z+3
		java.util.List<int[]> bus = new java.util.ArrayList<>();   // {x, y offset, z}
		for (int x = 65; x <= 70; x++) {
			bus.add(new int[] {x, 0, z + 9});
		}
		for (int yy = 1; yy <= 16; yy++) {
			bus.add(new int[] {70, yy, z + 9});
		}
		bus.add(new int[] {71, 16, z + 9});
		for (int zz = z; zz <= z + 21; zz++) {
			bus.add(new int[] {72, 16, zz});
		}
		for (int yy = 4; yy <= 15; yy++) {
			bus.add(new int[] {72, yy, z});
		}
		for (int x = 22; x <= 71; x++) {
			bus.add(new int[] {x, 16, z + 21});
		}
		for (int yy = 0; yy <= 15; yy++) {
			bus.add(new int[] {22, yy, z + 21});
		}
		for (int[] b : bus) {
			put(server, b[0], up + b[1], b[2], "wildcatter:bus_duct_gen");
		}
		for (int[] b : bus) {
			sizeCables(server, new BlockPos(b[0], up + b[1], b[2]), new BlockPos(b[0], up + b[1], b[2]), WildcatterBlocks.BUS_DUCT_GEN, dev.wildcatter.energy.ConductorSize.BUS_DUCT_3000);
		}
		// the 4.16 kV tray at up+14 from the UAT's back, and the forward unit substation for 480 V
		java.util.List<int[]> mvTray = new java.util.ArrayList<>();
		for (int yy = 0; yy <= 14; yy++) {
			mvTray.add(new int[] {72, yy, z + 4});
		}
		for (int x = -1; x <= 71; x++) {
			mvTray.add(new int[] {x, 14, z + 4});
		}
		for (int zz = z - 5; zz <= z + 6; zz++) {
			mvTray.add(new int[] {-1, 14, zz});
		}
		for (int zz = z + 5; zz <= z + 18; zz++) {
			mvTray.add(new int[] {26, 14, zz});
		}
		for (int zz = z - 11; zz <= z + 3; zz++) {
			mvTray.add(new int[] {33, 14, zz});
		}
		for (int x = 34; x <= 37; x++) {
			mvTray.add(new int[] {x, 14, z - 11});
		}
		for (int zz = z - 8; zz <= z + 3; zz++) {
			mvTray.add(new int[] {7, 14, zz});
		}
		for (int yy = 0; yy <= 13; yy++) {
			mvTray.add(new int[] {7, yy, z - 8});   // down to the forward unit substation's 4.16 kV side
		}
		for (int[] c : mvTray) {
			put(server, c[0], up + c[1], c[2], "wildcatter:cable_mv");
		}
		BlockPos lvOut = inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, new BlockPos(7, up, z - 8), net.minecraft.core.Direction.EAST, true);   // x 8-11
		// the 480 V busway tray at up+17
		java.util.List<int[]> lvTray = new java.util.ArrayList<>();
		for (int yy = 0; yy <= 17; yy++) {
			lvTray.add(new int[] {lvOut.getX(), yy, lvOut.getZ()});
		}
		for (int x = -4; x <= 25; x++) {
			lvTray.add(new int[] {x, 17, z - 8});
		}
		for (int zz = z - 7; zz <= z; zz++) {
			lvTray.add(new int[] {-4, 17, zz});
			lvTray.add(new int[] {11, 17, zz});
		}
		for (int zz = z - 7; zz <= z + 11; zz++) {
			lvTray.add(new int[] {25, 17, zz});
		}
		for (int x = 26; x <= 28; x++) {
			lvTray.add(new int[] {x, 17, z + 11});
		}
		for (int[] c : lvTray) {
			put(server, c[0], up + c[1], c[2], "wildcatter:busway_industrial");
		}
		// hooks: hot standby at 1,500 ppm, boric acid in the CVCS, pumped water
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.ReactorVesselBlockEntity) srv.overworld().getBlockEntity(vessel)).setHotStandbyForTest(1500);
			((dev.wildcatter.block.CvcsBlockEntity) srv.overworld().getBlockEntity(cvcs)).setAcidForTest(500);
			return null;
		});
		water(server, cvcs, afw, da);
		// the gas turbine on: starter set, purge skipped, onto the dead 13.8 kV bus
		exportBlueprint(server, "turbine_island", new BlockPos(-10, y, z - 20), new BlockPos(80, y + 20, z + 40));   // the library (docs/blueprints.md)
		startGenset(context, server, starterSet, north, 2000);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.GasTurbineBlockEntity) srv.overworld().getBlockEntity(gt)).setWantRun(true);
			return null;
		});
		context.waitTicks(200);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.GasTurbineBlockEntity) srv.overworld().getBlockEntity(gt)).skipStartForTest();
			return null;
		});
		context.waitTicks(100);
		String gtState = read(server, 60, up, z);
		check(gtState.contains("RUNNING") && gtState.contains("breaker CLOSED"), "the gas turbine should carry the 13.8 kV bus: " + gtState);
		// the drops, one motor at a time: 4.16 kV to the pumps, 500 kcmil 480 V stubs
		int[][] mvDrops = {{-1, z - 5}, {-1, z + 6}, {26, z + 13}, {26, z + 18}, {33, z - 11}, {37, z - 11}};
		for (int[] d : mvDrops) {
			for (int yy = up; yy <= up + 13; yy++) {
				put(server, d[0], yy, d[1], "wildcatter:cable_mv");
			}
			context.waitTicks(60);
		}
		int[][] lvDrops = {{-4, z - 3}, {-4, z}, {11, z}, {25, z + 11}, {28, z + 11}};
		for (int[] d : lvDrops) {
			for (int yy = up; yy <= up + 16; yy++) {
				put(server, d[0], yy, d[1], "wildcatter:cable_industrial");
			}
			sizeCables(server, new BlockPos(d[0], up, d[1]), new BlockPos(d[0], up + 16, d[1]), WildcatterBlocks.CABLE_INDUSTRIAL, dev.wildcatter.energy.ConductorSize.KCMIL_500);
			context.waitTicks(40);
		}
		put(server, -3, up, z - 3, "wildcatter:cable_industrial");   // the pressurizer's box
		for (int x = -3; x <= -1; x++) {
			put(server, x, up + 10, z, "wildcatter:cable_industrial");   // over the CVCS to the CRDM head's box
		}
		sizeCables(server, new BlockPos(-3, up, z - 3), new BlockPos(-1, up + 10, z), WildcatterBlocks.CABLE_INDUSTRIAL, dev.wildcatter.energy.ConductorSize.KCMIL_500);
		for (int i = 0; i < 6; i++) {
			context.waitTicks(50);
			water(server, cvcs, afw, da);
		}
		String pumps = " | RCP A " + read(server, 0, up, z - 5) + " | CW " + read(server, 34, up - 3, z - 11) + " | feed " + read(server, 27, up, z + 13)
				+ " | condensate " + read(server, 26, up, z + 11) + " | GT " + read(server, 60, up, z) + " | turbine " + read(server, 20, up, z - 3);
		check(read(server, 0, up, z - 5).contains("flow 100") && read(server, 0, up, z + 6).contains("flow 100"), "both coolant pumps should run off the UAT: " + pumps);
		String cwB = read(server, 38, up - 3, z - 11);
		check(read(server, 34, up - 3, z - 11).contains("pumping") && (cwB.contains("pumping") || cwB.contains("idle: the condenser")),
				"the CW pumps should stand in the pool and pump what the condenser draws: " + cwB + pumps);
		// the reactor to 25 %; the turbine rolls and its generator synchronizes
		dev.wildcatter.fluid.Reactor[] r = new dev.wildcatter.fluid.Reactor[1];
		server.computeOnServer(srv -> {
			r[0] = ((dev.wildcatter.block.ReactorVesselBlockEntity) srv.overworld().getBlockEntity(vessel)).reactor();
			r[0].setSetpoint(0.25);
			r[0].setRodsForTest(Tuning.ROD_STEPS, 150);
			r[0].holdPowerForTest(0.2);
			((dev.wildcatter.block.LoadBankBlockEntity) srv.overworld().getBlockEntity(bank1)).setSteps(4);   // 8 MW: the gas turbine alone carries it
			((dev.wildcatter.block.LoadBankBlockEntity) srv.overworld().getBlockEntity(bank2)).setSteps(4);
			return null;
		});
		boolean synced = false;
		for (int i = 0; i < 90 && !synced; i++) {
			context.waitTicks(40);
			water(server, cvcs, afw, da);
			synced = server.computeOnServer(srv -> ((dev.wildcatter.block.GeneratorPartBlockEntity) srv.overworld().getBlockEntity(tg)).breakerClosed());
		}
		check(synced, "the turbine should roll up and its generator synchronize: " + read(server, 20, up, z - 3) + " | " + read(server, 21, up, z + 11)
				+ " | " + read(server, 0, up, z) + " | GT " + read(server, 60, up, z));
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.LoadBankBlockEntity) srv.overworld().getBlockEntity(bank1)).setSteps(10);   // 20 MW with both on line
			((dev.wildcatter.block.LoadBankBlockEntity) srv.overworld().getBlockEntity(bank2)).setSteps(10);
			return null;
		});
		double mw = 0;
		for (int i = 0; i < 30 && mw < 8; i++) {
			context.waitTicks(40);
			water(server, cvcs, afw, da);
			mw = server.computeOnServer(srv -> ((dev.wildcatter.block.HpTurbineBlockEntity) srv.overworld().getBlockEntity(hp)).mw());
		}
		String plant = " | vessel " + read(server, 0, up, z) + " | turbine " + read(server, 20, up, z - 3) + " | generator " + read(server, 21, up, z + 11)
				+ " | condenser " + read(server, 26, up, z + 3) + " | SG A " + read(server, 0, up, z - 3) + " | GT " + read(server, 60, up, z);
		check(mw > 8, "the turbine generator should synchronize and carry about 12 MW at 25 %: " + mw + plant);
		check(!r[0].tripped(), "the reactor should hold 25 %: " + plant);
		check(read(server, 20, up, z - 3).contains("RUNNING"), "the turbine should run: " + plant);
		server.runCommand("tp @p 38 " + (up + 26) + " " + (z + 40) + " 150 38");
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-turbine-island");
		String running = read(server, 20, up, z - 3);
		// both CW pumps lose their power: low vacuum trips the turbine; at 25 % (below P-9) the reactor stays up. The banks come back
		// to 8 MW first, so the gas turbine alone can carry the bus when the turbine generator drops off.
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.LoadBankBlockEntity) srv.overworld().getBlockEntity(bank1)).setSteps(4);
			((dev.wildcatter.block.LoadBankBlockEntity) srv.overworld().getBlockEntity(bank2)).setSteps(4);
			return null;
		});
		context.waitTicks(100);
		put(server, 33, up + 14, z + 3, "minecraft:air");
		context.waitTicks(200);
		String tripped = read(server, 20, up, z - 3);
		check(tripped.contains("TRIPPED") && tripped.contains("VACUUM"), "losing the circulating water should trip the turbine on low vacuum: " + tripped);
		check(!r[0].tripped(), "below P-9 the reactor should stay up on the dump valves: " + read(server, 0, up, z));
		System.out.println("[wildcatter-test] turbineIsland ok: " + running + " | " + tripped + " | " + read(server, 0, up, z));
	}

	/**
	 * The hyperbolic natural-draft cooling tower (8c, docs/multiblocks/natural-draft-tower.md). Its controller at 0, up, z faces north,
	 * so the tower's centre is at 2, z-32. The basin is built from concrete and steel fed by hopper; a hook builds the rest. The pond fills,
	 * the shell's blocks stand on the hyperbola, a pump in the basin sees the tower's water, and with 104 MW of heat the cold water
	 * settles 8 C over the wet bulb and the plume rises.
	 */
	private void coolingTower(ClientGameTestContext context, TestServerContext server) {
		int z = -3700;
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand("tp @p 30 " + (up + 20) + " " + (z + 30) + " 150 30");
		context.waitTicks(20);
		server.runCommand("fill -35 " + (y - 4) + " " + (z - 70) + " 40 " + (y - 1) + " " + (z + 5) + " minecraft:stone");
		for (int zz = z - 70; zz <= z + 5; zz += 38) {
			server.runCommand("fill -35 " + y + " " + zz + " 40 " + (y + 10) + " " + Math.min(zz + 37, z + 5) + " minecraft:air");
		}
		BlockPos main = new BlockPos(0, up, z);
		placeBig(server, WildcatterBlocks.NATURAL_DRAFT_TOWER, main, net.minecraft.core.Direction.NORTH);   // x 0-3, z..z+3
		put(server, 1, up + 3, z + 1, "minecraft:hopper");
		server.runCommand("item replace block 1 " + (up + 3) + " " + (z + 1) + " container.0 with minecraft:gray_concrete 64");
		server.runCommand("item replace block 1 " + (up + 3) + " " + (z + 1) + " container.1 with minecraft:gray_concrete 40");
		server.runCommand("item replace block 1 " + (up + 3) + " " + (z + 1) + " container.2 with wildcatter:steel_ingot 12");
		context.waitTicks(100);
		String pouring = read(server, 0, up, z);
		check(pouring.contains("BASIN"), "the controller should start on the basin: " + pouring);
		int stage = 0;
		for (int i = 0; i < 60 && stage == 0; i++) {
			context.waitTicks(40);
			stage = server.computeOnServer(srv -> ((dev.wildcatter.block.NaturalDraftTowerBlockEntity) srv.overworld().getBlockEntity(main)).stage());
		}
		check(stage == dev.wildcatter.block.NaturalDraftTowerBlockEntity.COLUMNS, "the basin should be poured from the hopper's concrete and steel: " + read(server, 0, up, z));
		double cx = 2.0, cz = z - 32.0;
		check(stateAt(server, BlockPos.containing(cx, up - 2, cz)).is(net.minecraft.world.level.block.Blocks.WATER), "the pond should be dug and filled");
		check(stateAt(server, BlockPos.containing(cx + 31.5, up, cz)).is(WildcatterBlocks.TOWER_SHELL), "the rim should stand at 31-32 m");
		// the rest by hook: the raker columns, 54 lifts and the fill
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.NaturalDraftTowerBlockEntity) srv.overworld().getBlockEntity(main)).buildForTest(srv.overworld(), dev.wildcatter.fluid.CoolingTower.lifts());
			return null;
		});
		for (double h : new double[] {8.5, 62.5, 87.5}) {
			double r = dev.wildcatter.fluid.CoolingTower.radius(h);
			int hy = up + (int) Math.floor(h);
			int found = server.computeOnServer(srv -> {
				for (int dx = 0; dx <= 34; dx++) {
					if (srv.overworld().getBlockState(BlockPos.containing(cx + dx, hy, cz)).is(WildcatterBlocks.TOWER_SHELL)) {
						return dx;
					}
				}
				return -1;
			});
			check(Math.abs(found - r) <= 1.5, "the shell should stand at " + String.format("%.1f", r) + " m at " + h + " m: found " + found);
		}
		context.waitTicks(10);   // the readout catches up on its next interval
		String built = read(server, 0, up, z);
		check(built.contains("BUILT"), "the tower should be built: " + built);
		// a pump standing in the basin sees the tower's water
		boolean inBasin = server.computeOnServer(srv -> dev.wildcatter.block.NaturalDraftTowerBlockEntity.basinAt(srv.overworld(), BlockPos.containing(cx + 10, up - 3, cz)) != null);
		check(inBasin, "a pump in the basin should draw from the tower");
		// 104 MW of heat: the cold water settles 8 C over the wet bulb, it evaporates and the plume rises
		water(server, main);
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.NaturalDraftTowerBlockEntity) srv.overworld().getBlockEntity(main)).setHeatForTest(srv.overworld(), 104e6);
			return null;
		});
		context.waitTicks(8);
		double[] t = server.computeOnServer(srv -> {
			var tower = (dev.wildcatter.block.NaturalDraftTowerBlockEntity) srv.overworld().getBlockEntity(main);
			return new double[] {tower.coldC(), tower.wetBulbC(), tower.plume() ? 1 : 0};
		});
		String hot = read(server, 0, up, z);
		check(Math.abs(t[0] - t[1] - 8.0) < 0.5 || t[0] <= Tuning.TOWER_MIN_COLD_C + 0.01, "the cold water should settle 8 C over the wet bulb: " + hot);
		check(t[2] > 0 && hot.contains("evaporation 4"), "it should evaporate about 42 kg/s and raise its plume: " + hot);
		server.runCommand("tp @p " + (cx + 36) + " " + (up + 4) + " " + (cz + 36) + " 135 -28");   // inside the test client's 5-chunk fog
		context.waitTicks(80);
		exportBlueprint(server, "cooling_tower", new BlockPos(-35, y, z - 70), new BlockPos(40, y + 10, z + 5));   // the library (docs/blueprints.md)
		context.takeScreenshot("wildcatter-cooling-tower");
		System.out.println("[wildcatter-test] coolingTower ok: " + hot);
	}

	/**
	 * After the trip (8d, docs/multiblocks/reactor.md): a vessel with its CRDM head, an RHR pump against its north face and the RHR
	 * exchanger against the pump, a CW pump in a pool north-east piping water to the exchanger, and three diesel sets through a backward
	 * unit substation for their 4.16 kV. Hooks give the core a full-power history and trip it: decay heat and xenon; then put it at
	 * 170 C and 2.5 MPa for the RHR; then drain it with the fuel at 1,150 C: core damage, hydrogen and the blast. The vessel stands.
	 */
	private void reactorSafety(ClientGameTestContext context, TestServerContext server) {
		int z = -3900;
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand("tp @p 14 " + (up + 18) + " " + (z + 16) + " 150 40");
		context.waitTicks(20);
		for (int zz = z - 20; zz <= z + 10; zz += 15) {
			server.runCommand("fill -26 " + (y - 1) + " " + zz + " 16 " + (y + 15) + " " + Math.min(zz + 14, z + 10) + " minecraft:air");
		}
		server.runCommand("fill -26 " + (y - 4) + " " + (z - 20) + " 16 " + (y - 1) + " " + (z + 10) + " minecraft:stone");
		server.runCommand("fill 7 " + (up - 3) + " " + (z - 14) + " 13 " + (up - 1) + " " + (z - 6) + " minecraft:water");
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		BlockPos vessel = new BlockPos(0, up, z);
		placeBig(server, WildcatterBlocks.REACTOR_VESSEL, vessel, north);
		placeBig(server, WildcatterBlocks.CRDM_HEAD, new BlockPos(0, up + 10, z), north);
		placeBig(server, WildcatterBlocks.RHR_PUMP, new BlockPos(0, up, z - 2), north);              // x 0-1, z-2..z-1, against the vessel
		placeBig(server, WildcatterBlocks.RHR_HEAT_EXCHANGER, new BlockPos(2, up, z - 2), north);    // x 2-3, against the pump
		placeBig(server, WildcatterBlocks.CIRCULATING_WATER_PUMP, new BlockPos(8, up - 3, z - 12), north);   // x 8-10, z-12..z-10, in the pool
		for (int zz = z - 11; zz <= z - 4; zz++) {
			put(server, 11, up, zz, "wildcatter:gas_line");
		}
		for (int x = 2; x <= 11; x++) {
			put(server, x, up, z - 3, "wildcatter:gas_line");
		}
		// 4.16 kV: three sets, a backward unit substation, north to the CW pump's box and round to the RHR pump's
		BlockPos[] mvSets = {new BlockPos(-22, up, z - 13), new BlockPos(-14, up, z - 13), new BlockPos(-6, up, z - 13)};
		for (BlockPos set : mvSets) {
			genset(server, set, north, false);
			put(server, set.getX() + 1, up, z - 13, "wildcatter:busway_industrial");
		}
		for (int x = -21; x <= 2; x++) {
			put(server, x, up, z - 14, "wildcatter:busway_industrial");
		}
		inlineTransformer(server, WildcatterBlocks.UNIT_SUBSTATION, new BlockPos(2, up, z - 14), net.minecraft.core.Direction.EAST, false);   // x 3-6
		for (BlockPos set : mvSets) {
			startGenset(context, server, set, north, 2000);
		}
		for (int zz = z - 14; zz <= z - 12; zz++) {
			put(server, 7, up, zz, "wildcatter:cable_mv");
		}
		context.waitTicks(100);
		for (int x = -1; x <= 6; x++) {
			put(server, x, up, z - 13, "wildcatter:cable_mv");
		}
		for (int zz = z - 12; zz <= z - 2; zz++) {
			put(server, -1, up, zz, "wildcatter:cable_mv");
		}
		// a long run at full power, then a trip: decay heat and xenon
		dev.wildcatter.fluid.Reactor[] r = new dev.wildcatter.fluid.Reactor[1];
		server.computeOnServer(srv -> {
			r[0] = ((dev.wildcatter.block.ReactorVesselBlockEntity) srv.overworld().getBlockEntity(vessel)).reactor();
			((dev.wildcatter.block.ReactorVesselBlockEntity) srv.overworld().getBlockEntity(vessel)).setHotStandbyForTest(1500);
			r[0].setHistoryForTest(1.0);
			r[0].setSetpoint(-1);
			return null;
		});
		context.waitTicks(40);
		double[] after = server.computeOnServer(srv -> new double[] {r[0].decayHeat(), r[0].xenonPcm()});
		String trip = read(server, 0, up, z);
		check(after[0] > 0.04 && after[0] < 0.10, "a few seconds after the trip the decay heat should be about 6 %: " + trip);
		check(after[1] < -2500, "the xenon should be at its full-power level and building: " + trip);
		// cooled to 170 C and 2.5 MPa: the RHR cuts in and takes the heat into the circulating water
		server.computeOnServer(srv -> {
			r[0].setCooldownForTest(170, 2.5);
			return null;
		});
		double rhr = 0;
		for (int i = 0; i < 30 && rhr < 3e5; i++) {
			context.waitTicks(20);
			rhr = server.computeOnServer(srv -> r[0].rhrHeat());
		}
		String rhrRead = read(server, 0, up, z) + " | RHR " + read(server, 2, up, z - 2) + " | pump " + read(server, 0, up, z - 2) + " | CW " + read(server, 8, up - 3, z - 12);
		check(rhr > 3e5, "the RHR should take the decay heat into the circulating water: " + rhrRead);
		// loss of all cooling: the RHR's power cut, the RCS drained to a quarter, the fuel at 1,150 C
		put(server, -1, up, z - 2, "minecraft:air");
		server.computeOnServer(srv -> {
			r[0].setInventoryForTest(0.25 * Tuning.RCS_WATER_KG);
			r[0].setFuelForTest(1150);
			return null;
		});
		boolean exploded = false;
		for (int i = 0; i < 60 && !exploded; i++) {
			context.waitTicks(20);
			exploded = server.computeOnServer(srv -> r[0].exploded());
		}
		String damage = read(server, 0, up, z);
		check(server.computeOnServer(srv -> r[0].coreDamaged()), "the cladding should fail over 1,200 C: " + damage);
		check(exploded && damage.contains("HYDROGEN EXPLOSION"), "the hydrogen should ignite: " + damage);
		check(stateAt(server, vessel).is(WildcatterBlocks.REACTOR_VESSEL) && stateAt(server, vessel.above(10)).is(WildcatterBlocks.CRDM_HEAD),
				"the vessel and its head should stand through the blast");
		server.runCommand("tp @p 14 " + (up + 8) + " " + (z + 14) + " 135 20");
		context.waitTicks(20);
		context.takeScreenshot("wildcatter-reactor-safety");
		System.out.println("[wildcatter-test] reactorSafety ok: " + trip + " | " + rhrRead + " | " + damage);
	}

	/**
	 * Fuel handling (8e, docs/multiblocks/fuel-handling.md): a vessel at cold shutdown with a burned core; the refuelling machine against
	 * its north face, the fuel transfer system north of the machine, the spent fuel pool's controller north of that with its pool dug
	 * in front; two diesel sets on a busway. REFUEL (moves sped up) sends the 3 most-burned to the pool and loads 3 fresh; the pool
	 * shields them; drained, the dose over it is lethal; an acute dose brings its effects; DEFUEL clears a damaged core.
	 */
	private void fuelHandling(ClientGameTestContext context, TestServerContext server) {
		int z = -4100;
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand("tp @p 10 " + (up + 14) + " " + (z + 6) + " 160 40");
		context.waitTicks(20);
		// the test world is superflat a few blocks over its floor: the scene stands on a 16 m stone platform so the pool can be dug
		for (int zz = z - 30; zz <= z + 6; zz += 18) {
			server.runCommand("fill -20 " + up + " " + zz + " 12 " + (up + 15) + " " + Math.min(zz + 17, z + 6) + " minecraft:stone");
			server.runCommand("fill -20 " + (up + 16) + " " + zz + " 12 " + (up + 28) + " " + Math.min(zz + 17, z + 6) + " minecraft:air");
		}
		int up = this.up + 16;   // the platform's top
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		BlockPos vessel = new BlockPos(0, up, z);
		placeBig(server, WildcatterBlocks.REACTOR_VESSEL, vessel, north);
		BlockPos machine = new BlockPos(0, up, z - 4);
		placeBig(server, WildcatterBlocks.REFUELLING_MACHINE, machine, north);                       // x 0-3, z-4..z-1
		placeBig(server, WildcatterBlocks.FUEL_TRANSFER_SYSTEM, new BlockPos(0, up, z - 10), north);  // x 0-1, z-10..z-5
		BlockPos pool = new BlockPos(0, up, z - 14);
		placeBig(server, WildcatterBlocks.SPENT_FUEL_POOL, pool, north);                             // x 0-3, z-14..z-11; the pool z-20..z-15
		put(server, 1, up + 4, z - 3, "minecraft:hopper");
		server.runCommand("item replace block 1 " + (up + 4) + " " + (z - 3) + " container.0 with wildcatter:fuel_assembly 1");
		server.runCommand("item replace block 1 " + (up + 4) + " " + (z - 3) + " container.1 with wildcatter:fuel_assembly 1");
		server.runCommand("item replace block 1 " + (up + 4) + " " + (z - 3) + " container.2 with wildcatter:fuel_assembly 1");
		BlockPos[] sets = {new BlockPos(-12, up, z - 26), new BlockPos(-4, up, z - 26)};
		for (BlockPos set : sets) {
			genset(server, set, north, false);
			put(server, set.getX() + 1, up, z - 26, "wildcatter:busway_industrial");
		}
		for (int x = -11; x <= -3; x++) {
			put(server, x, up, z - 27, "wildcatter:busway_industrial");
		}
		for (int zz = z - 27; zz <= z - 3; zz++) {
			put(server, -2, up, zz, "wildcatter:busway_industrial");
		}
		for (BlockPos set : sets) {
			startGenset(context, server, set, north, 2000);
		}
		for (int zz : new int[] {-4, -14}) {
			put(server, -1, up, z + zz, "wildcatter:cable_industrial");
			context.waitTicks(40);
		}
		// a burned core at cold shutdown, a long run behind it
		dev.wildcatter.fluid.Reactor[] r = new dev.wildcatter.fluid.Reactor[1];
		server.computeOnServer(srv -> {
			var v = (dev.wildcatter.block.ReactorVesselBlockEntity) srv.overworld().getBlockEntity(vessel);
			r[0] = v.reactor();
			v.setHotStandbyForTest(1500);
			r[0].setColdForTest();
			r[0].setBurnupsForTest(33, 33, 33, 17, 17, 17, 8, 8, 8);
			r[0].setHistoryForTest(1.0, 7 * 86400);   // shut down a week ago, as an outage is
			r[0].setSetpoint(-1);
			((dev.wildcatter.block.RefuellingMachineBlockEntity) srv.overworld().getBlockEntity(machine)).setMoveSecondsForTest(1);
			return null;
		});
		context.waitTicks(60);
		String probe = server.computeOnServer(srv -> {
			var p = (dev.wildcatter.block.SpentFuelPoolBlockEntity) srv.overworld().getBlockEntity(pool);
			return p.cell(2, 2, 6).toShortString() + "=" + srv.overworld().getBlockState(p.cell(2, 2, 6)) + ", " + p.cell(2, 2, 1).toShortString() + "="
					+ srv.overworld().getBlockState(p.cell(2, 2, 1));
		});
		check(stateAt(server, new BlockPos(1, up - 6, z - 17)).is(net.minecraft.world.level.block.Blocks.WATER), "the pool should be dug and full: " + probe + " | "
				+ read(server, 0, up, z - 14));
		String began = server.computeOnServer(srv -> ((dev.wildcatter.block.RefuellingMachineBlockEntity) srv.overworld().getBlockEntity(machine)).nextMode(srv.overworld()));
		check(began.contains("REFUEL"), "a sneak-click should start REFUEL: " + began);
		int racks = 0;
		for (int i = 0; i < 40 && racks < 3; i++) {
			context.waitTicks(20);
			racks = server.computeOnServer(srv -> ((dev.wildcatter.block.SpentFuelPoolBlockEntity) srv.overworld().getBlockEntity(pool)).racks().size());
		}
		context.waitTicks(200);
		double[] core = server.computeOnServer(srv -> {
			double fresh = 0, max = 0;
			for (int i = 0; i < r[0].assemblies(); i++) {
				fresh += r[0].burnup(i) < 0.01 ? 1 : 0;
				max = Math.max(max, r[0].burnup(i));
			}
			return new double[] {r[0].assemblies(), fresh, max};
		});
		String outage = read(server, 0, up, z - 4) + " | " + read(server, 0, up, z - 14);
		check(racks == 3, "REFUEL should send 3 assemblies to the pool: " + outage);
		check(core[0] == 9 && core[1] == 3 && core[2] < 20, "the core should hold 3 fresh and the 6 less burned: " + java.util.Arrays.toString(core) + " | " + outage);
		// shielded by the pool's water, then drained
		net.minecraft.world.phys.Vec3 over = new net.minecraft.world.phys.Vec3(2.5, up + 0.5, z - 17.5);
		double shielded = server.computeOnServer(srv -> dev.wildcatter.radiation.RadiationTicker.rateAt(srv.overworld(), over, null));
		check(shielded < 1e-4, "the pool's water should shield the fuel: " + dev.wildcatter.fluid.Radiation.rateText(shielded));
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.SpentFuelPoolBlockEntity) srv.overworld().getBlockEntity(pool)).setWaterForTest(srv.overworld(), 2 * 36_000);
			return null;
		});
		double bare = server.computeOnServer(srv -> dev.wildcatter.radiation.RadiationTicker.rateAt(srv.overworld(), over, null));
		check(bare > 1, "with the pool drained the dose over it should be lethal: " + dev.wildcatter.fluid.Radiation.rateText(bare));
		server.computeOnServer(srv -> {
			((dev.wildcatter.block.SpentFuelPoolBlockEntity) srv.overworld().getBlockEntity(pool)).setWaterForTest(srv.overworld(), Tuning.POOL_WATER_KG);
			return null;
		});
		// an acute dose brings its effects
		server.computeOnServer(srv -> {
			var player = srv.getPlayerList().getPlayers().get(0);
			dev.wildcatter.radiation.DoseData.get(srv.overworld()).of(player.getStringUUID())[1] = 3.5;
			return null;
		});
		context.waitTicks(40);
		boolean sick = server.computeOnServer(srv -> {
			var player = srv.getPlayerList().getPlayers().get(0);
			return player.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS) && player.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS);
		});
		server.computeOnServer(srv -> {
			var player = srv.getPlayerList().getPlayers().get(0);
			dev.wildcatter.radiation.DoseData.get(srv.overworld()).of(player.getStringUUID())[1] = 0;
			player.removeAllEffects();
			return null;
		});
		check(sick, "3.5 Sv should bring radiation sickness");
		// a damaged core: DEFUEL takes it all out and clears it
		server.computeOnServer(srv -> {
			r[0].setDamagedForTest();
			var m = (dev.wildcatter.block.RefuellingMachineBlockEntity) srv.overworld().getBlockEntity(machine);
			m.nextMode(srv.overworld());   // REFUEL
			m.nextMode(srv.overworld());   // DEFUEL
			return null;
		});
		for (int i = 0; i < 40 && server.computeOnServer(srv -> r[0].assemblies()) > 0; i++) {
			context.waitTicks(20);
		}
		check(server.computeOnServer(srv -> r[0].assemblies() == 0 && !r[0].coreDamaged()), "DEFUEL should empty the core and clear its damage: " + read(server, 0, up, z));
		int stored = server.computeOnServer(srv -> ((dev.wildcatter.block.SpentFuelPoolBlockEntity) srv.overworld().getBlockEntity(pool)).racks().size());
		check(stored == 12, "the pool should hold 12: " + stored);
		server.runCommand("tp @p 2.0 " + (up + 6) + " " + (z - 24.5) + " 0 38");   // over the pool's far end, looking down at its racks
		context.waitTicks(20);
		context.takeScreenshot("wildcatter-spent-fuel-pool");
		System.out.println("[wildcatter-test] fuelHandling ok: " + outage + " | shielded " + dev.wildcatter.fluid.Radiation.rateText(shielded) + ", drained "
				+ dev.wildcatter.fluid.Radiation.rateText(bare) + " | " + read(server, 0, up, z - 14));
	}

	/**
	 * Every vein, the evaporite beds, the Bushveld reef and a salt dome are placed into rock with /place feature and must
	 * leave their ore; every ore drops its mineral; a furnace smelts galena; the scintillometer hears a pitchblende pod.
	 */
	private void ores(ClientGameTestContext context, TestServerContext server) {
		int z = -1520;
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand("tp @p 40 " + (up + 4) + " " + (z + 20) + " 180 10");
		context.waitTicks(20);
		server.runCommand("fill 0 " + (y - 1) + " " + (z - 30) + " 100 " + (y - 1) + " " + (z + 24) + " minecraft:stone");

		// the scintillometer, first, before any uranium is near: background over barren stone, then a pod 6 blocks east
		double barren = server.computeOnServer(srv -> dev.wildcatter.item.ScintillometerItem.read(srv.overworld(), 60.5, up + 1.5, z + 10.5).cps());
		check(Math.abs(barren - Tuning.SCINT_BACKGROUND_CPS) < 1, "over barren rock the scintillometer should read background: " + barren);
		server.runCommand("fill 66 " + up + " " + (z + 9) + " 68 " + (up + 2) + " " + (z + 11) + " wildcatter:pitchblende_ore");
		var pod = server.computeOnServer(srv -> dev.wildcatter.item.ScintillometerItem.read(srv.overworld(), 60.5, up + 1.5, z + 10.5));
		String scint = server.computeOnServer(srv -> dev.wildcatter.item.ScintillometerItem.report(srv.overworld(), 60.5, up + 1.5, z + 10.5));
		check(pod.cps() > 1000 && pod.dx() > 0 && Math.abs(pod.dz()) < pod.dx() * 0.2, "a pitchblende pod to the east should read high from the east: " + scint);
		check(scint.contains("east"), "the meter should say the count rises to the east: " + scint);
		server.runCommand("fill 66 " + up + " " + (z + 9) + " 68 " + (up + 2) + " " + (z + 11) + " minecraft:air");

		// each ore drops its mineral
		for (dev.wildcatter.ore.Ore ore : dev.wildcatter.ore.Ore.values()) {
			boolean drops = server.computeOnServer(srv -> net.minecraft.world.level.block.Block.getDrops(dev.wildcatter.ore.OreBlocks.block(ore).defaultBlockState(),
					srv.overworld(), new BlockPos(60, up, z), null).stream().anyMatch(stack -> stack.is(dev.wildcatter.ore.OreBlocks.MINERALS.get(ore))));
			check(drops, ore.blockName + " should drop " + ore.mineralName);
		}

		// a furnace smelts galena into lead
		put(server, 60, up, z + 4, "minecraft:furnace");
		server.runCommand("item replace block 60 " + up + " " + (z + 4) + " container.0 with wildcatter:galena");
		server.runCommand("item replace block 60 " + up + " " + (z + 4) + " container.1 with minecraft:coal");
		context.waitTicks(230);
		boolean lead = server.computeOnServer(srv -> ((Container) srv.overworld().getBlockEntity(new BlockPos(60, up, z + 4))).getItem(2)
				.is(dev.wildcatter.ore.OreBlocks.INGOTS.get("lead")));
		check(lead, "a furnace should smelt galena into a lead ingot");

		// every feature, into a box of its host rock
		String[][] veins = {{"uraninite", "stone"}, {"pitchblende", "deepslate"}, {"fluorite", "stone"}, {"heavy_mineral", "sand"}, {"bauxite", "stone"},
				{"galena", "stone"}, {"sphalerite", "stone"}, {"cassiterite", "stone"}, {"pentlandite", "deepslate"}, {"molybdenite", "stone"},
				{"magnesite", "stone"}, {"dolomite", "stone"}, {"graphite", "stone"}, {"borax", "stone"}, {"spodumene", "stone"}, {"barite", "stone"},
				{"bentonite", "stone"}, {"acanthite", "stone"}, {"scheelite", "stone"}, {"cobaltite", "stone"}, {"pyrolusite", "stone"}, {"phosphate", "stone"}};
		int bx = 70, by = up, bz = z - 28;
		StringBuilder found = new StringBuilder();
		for (String[] vein : veins) {
			dev.wildcatter.ore.Ore ore = dev.wildcatter.ore.Ore.valueOf(vein[0].toUpperCase());
			int n = 0;
			for (int tries = 0; tries < 4 && n == 0; tries++) {
				server.runCommand("fill " + bx + " " + by + " " + bz + " " + (bx + 10) + " " + (by + 10) + " " + (bz + 10) + " minecraft:" + vein[1]);
				server.runCommand("place feature wildcatter:ore_" + vein[0] + " " + (bx + 5) + " " + (by + 5) + " " + (bz + 5));
				n = count(server, dev.wildcatter.ore.OreBlocks.block(ore), new BlockPos(bx, by, bz), new BlockPos(bx + 10, by + 10, bz + 10));
			}
			check(n > 0, "the " + vein[0] + " vein should leave " + ore.blockName + " in " + vein[1]);
			found.append(vein[0]).append(' ').append(n).append(", ");
		}
		server.runCommand("fill " + bx + " " + by + " " + bz + " " + (bx + 10) + " " + (by + 10) + " " + (bz + 10) + " minecraft:air");
		// the layered features: a wide flat box of stone
		for (String[] layered : new String[][] {{"evaporite_beds", "HALITE", "SYLVITE"}, {"bushveld_reef", "CHROMITE", "PLATINUM"}}) {
			server.runCommand("fill 30 " + by + " " + (z - 30) + " 60 " + (by + 7) + " " + z + " minecraft:stone");
			server.runCommand("place feature wildcatter:" + layered[0] + " 45 " + (by + 1) + " " + (z - 15));
			int a = count(server, dev.wildcatter.ore.OreBlocks.block(dev.wildcatter.ore.Ore.valueOf(layered[1])), new BlockPos(30, by, z - 30), new BlockPos(60, by + 7, z));
			int b = count(server, dev.wildcatter.ore.OreBlocks.block(dev.wildcatter.ore.Ore.valueOf(layered[2])), new BlockPos(30, by, z - 30), new BlockPos(60, by + 7, z));
			check(a > 100 && b > 50, layered[0] + " should lay down " + layered[1] + " and " + layered[2] + ": " + a + ", " + b);
			found.append(layered[0]).append(' ').append(a).append('/').append(b).append(", ");
		}
		server.runCommand("fill 30 " + by + " " + (z - 30) + " 60 " + (by + 7) + " " + z + " minecraft:air");

		// a salt dome in a column of stone from bedrock up, cut open on its south side for the picture. Domes stand on a seed grid
		// (docs/salt-dome-oil.md): a forced seed puts one near, and the feature placed by hand draws the part of it in its chunk
		long domeSeed = -1;
		dev.wildcatter.well.SaltDome dome = null;
		for (long s = 1; s < 2_000_000 && domeSeed < 0; s++) {
			dev.wildcatter.well.SaltDome d = dev.wildcatter.well.SaltDome.inCell(s, Math.floorDiv(20, Tuning.DOME_CELL), Math.floorDiv(z - 60, Tuning.DOME_CELL));
			if (d != null && Math.abs(d.centerX() - 20) < 40 && Math.abs(d.centerZ() - (z - 60)) < 40 && (d.centerX() & 15) >= 7 && (d.centerX() & 15) <= 8
					&& (d.centerZ() & 15) >= 7 && (d.centerZ() & 15) <= 8) {
				domeSeed = s;
				dome = d;
			}
		}
		check(domeSeed >= 0, "no seed puts a salt dome near the ores scene");
		ReservoirData.testSeed = domeSeed;
		ReservoirData.testDomeBiome = true;
		int cx = dome.centerX(), cz = dome.centerZ();
		server.runCommand("tp @p " + cx + " " + (up + 4) + " " + (cz + 26) + " 180 10");
		context.waitTicks(40);
		server.runCommand("fill " + (cx - 7) + " -63 " + (cz - 7) + " " + (cx + 8) + " 25 " + (cz + 8) + " minecraft:stone");
		rules(server, true, "saltDomeGrid192");   // the grid dome, as a new world has it
		server.runCommand("place feature wildcatter:salt_dome " + cx + " 0 " + cz);
		rules(server, false, "saltDomeGrid192");
		ReservoirData.testSeed = null;
		ReservoirData.testDomeBiome = null;
		int salt = count(server, dev.wildcatter.ore.OreBlocks.block(dev.wildcatter.ore.Ore.HALITE), new BlockPos(cx - 7, -40, cz - 7), new BlockPos(cx + 8, -30, cz + 8));
		int sulfur = count(server, dev.wildcatter.ore.OreBlocks.block(dev.wildcatter.ore.Ore.SULFUR), new BlockPos(cx - 7, 0, cz - 7), new BlockPos(cx + 8, 25, cz + 8));
		check(salt > 300 && sulfur > 5, "a salt dome should have rock salt in its column and sulfur in its cap: " + salt + " salt, " + sulfur + " sulfur");
		server.runCommand("fill " + (cx - 7) + " " + up + " " + (cz + 1) + " " + (cx + 8) + " 25 " + (cz + 8) + " minecraft:air");
		server.runCommand("tp @p " + cx + " 4 " + (cz + 26) + " 180 -8");
		context.waitTicks(40);
		context.takeScreenshot("wildcatter-salt-dome");

		// the wall of every ore block, nine across and three high
		dev.wildcatter.ore.Ore[] all = dev.wildcatter.ore.Ore.values();
		for (int i = 0; i < all.length; i++) {
			put(server, 40 + i % 9, up + 2 - i / 9, z + 10, "wildcatter:" + all[i].blockId());
		}
		server.runCommand("tp @p 44.5 " + (up + 1) + " " + (z + 15.5) + " 180 0");
		context.waitTicks(40);
		context.takeScreenshot("wildcatter-ores");

		// the tooltips say where each ore is found, what mines it and what uses it (OreUses)
		String saltTip = tooltip(context, new ItemStack(dev.wildcatter.ore.OreBlocks.MINERALS.get(dev.wildcatter.ore.Ore.HALITE)));
		String galenaTip = tooltip(context, new ItemStack(dev.wildcatter.ore.OreBlocks.block(dev.wildcatter.ore.Ore.GALENA)));
		String bariteTip = tooltip(context, new ItemStack(dev.wildcatter.ore.OreBlocks.MINERALS.get(dev.wildcatter.ore.Ore.BARITE)));
		check(saltTip.contains("Used in chlor-alkali cells") && saltTip.contains("Found: salt domes"), "rock salt's tooltip should say where it is found and what uses it: " + saltTip);
		check(galenaTip.contains("Used in a furnace (lead ingot)") && galenaTip.contains("Mined with a stone pickaxe"), "galena ore's tooltip should say it smelts to lead: " + galenaTip);
		check(bariteTip.contains("Used in the barite mill"), "barite's tooltip should say the mud plant uses it: " + bariteTip);
		// the picture: rock salt in the first hotbar slot, the inventory open and the cursor over it
		server.runCommand("clear @p");
		server.runCommand("item replace entity @p hotbar.0 with wildcatter:halite");
		context.waitTicks(5);
		context.setScreen(() -> new net.minecraft.client.gui.screens.inventory.InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
		context.waitTicks(10);
		double[] at = context.computeOnClient(client -> {
			double scale = client.getWindow().getGuiScale();
			int left = (client.getWindow().getGuiScaledWidth() - 176) / 2, top = (client.getWindow().getGuiScaledHeight() - 166) / 2;
			return new double[] {(left + 8 + 8) * scale, (top + 142 + 8) * scale};
		});
		context.getInput().setCursorPos(at[0], at[1]);
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-ore-tooltip");
		// with JEI in the run (fabric.addMods), U over the slot opens Wildcatter's information page for rock salt
		if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jei")) {
			context.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_U);
			context.waitTicks(20);
			String page = context.computeOnClient(client -> client.gui.screen() == null ? "none" : client.gui.screen().getClass().getName());
			check(page.contains("jei"), "U over rock salt should open JEI's page: " + page);
			context.takeScreenshot("wildcatter-ore-jei");
		}
		context.setScreen(() -> null);
		System.out.println("[wildcatter-test] ores ok: " + scint + " | " + found + "salt dome " + salt + " salt, " + sulfur + " sulfur | rock salt: " + saltTip);
	}

	/** An item's tooltip as the player sees it, one line after another. */
	private static String tooltip(ClientGameTestContext context, ItemStack stack) {
		return context.computeOnClient(client -> String.join(" / ", stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(client.level), client.player,
				net.minecraft.world.item.TooltipFlag.Default.NORMAL).stream().map(net.minecraft.network.chat.Component::getString).toList()));
	}

	private static int count(TestServerContext server, net.minecraft.world.level.block.Block block, BlockPos from, BlockPos to) {
		return server.computeOnServer(srv -> {
			int n = 0;
			for (BlockPos at : BlockPos.betweenClosed(from, to)) {
				if (srv.overworld().getBlockState(at).is(block)) {
					n++;
				}
			}
			return n;
		});
	}

	/** With WILDCATTER_EXPORT_BLUEPRINTS set, records a scene's built plant into the library folder (docs/blueprints.md, "The library"). */
	private void exportBlueprint(TestServerContext server, String name, BlockPos a, BlockPos b) {
		String dir = System.getenv("WILDCATTER_EXPORT_BLUEPRINTS");
		if (dir == null || dir.isBlank()) {
			return;
		}
		String json = server.computeOnServer(srv -> dev.wildcatter.blueprint.Surveys.record(srv.overworld(), dev.wildcatter.blueprint.BlueprintLibrary.title(name), a, b).toJson());
		try {
			java.nio.file.Files.createDirectories(java.nio.file.Path.of(dir));
			java.nio.file.Files.writeString(java.nio.file.Path.of(dir, name + ".json"), json);
		} catch (java.io.IOException e) {
			throw new IllegalStateException(e);
		}
		System.out.println("[wildcatter-test] blueprint " + name + " exported: " + json.length() + " chars");
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
