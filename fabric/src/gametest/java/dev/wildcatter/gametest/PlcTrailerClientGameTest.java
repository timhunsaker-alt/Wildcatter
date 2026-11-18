package dev.wildcatter.gametest;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.control.PlcCabinetBlockEntity;
import dev.wildcatter.control.PlcModuleItem;
import dev.wildcatter.well.Reservoir;
import dev.wildcatter.well.ReservoirData;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;

/**
 * {@code plcTrailer} (WILDCATTER_ONLY=plcTrailer; never part of the full suite): the PLC trailer's Wildcatter shots, in the first
 * trailer's real generated world at golden hour. Each bench is built as its PLC scene builds it (plc, plcSfc, plcAnalog, plcPower),
 * on land found by {@link TrailerClientGameTest#site}, and the real screens are worked with the real mouse and keys while the game
 * window is filmed ({@code build/scene-videos/NN_<shot>.mov}). World shots also leave a 1920 x 1080 start still {@code NN_<shot>_a.png}
 * in TRAILER_OUT. TRAILER_SHOTS picks benches by shot number: 1-4 the panel, 5-6 the chart, 7 the PID, 8 the gensets' HMI.
 */
public class PlcTrailerClientGameTest extends TrailerClientGameTest {

	@Override
	public void runTest(ClientGameTestContext context) {
		String only = System.getenv("WILDCATTER_ONLY");
		if (only == null || !java.util.Arrays.asList(only.split(",")).contains("plcTrailer")) {
			return;
		}
		this.context = context;
		String dir = System.getenv("TRAILER_OUT");
		out = Path.of(dir == null || dir.isBlank() ? "plc-trailer" : dir).toAbsolutePath();
		try {
			Files.createDirectories(out);
		} catch (java.io.IOException ex) {
			throw new RuntimeException(ex);
		}
		context.getInput().resizeWindow(1440, 810);   // as the first trailer: a window that fits the laptop's screen
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(s -> {
			s.setSeed(SEED);
			s.setGameMode(net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode.CREATIVE);
			s.setAllowCommands(true);
		}).create()) {
			server = world.getServer();
			context.waitTicks(40);
			server.computeOnServer(srv -> {
				var rules = srv.overworld().getGameRules();
				rules.set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME, false, srv);
				rules.set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_WEATHER, false, srv);
				rules.set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS, false, srv);
				return null;
			});
			server.runCommand("weather clear");
			server.runCommand("difficulty peaceful");
			server.runCommand("gamemode spectator @p");
			context.runOnClient(client -> {
				var o = client.options;
				o.applyGraphicsPreset(net.minecraft.client.GraphicsPreset.FANCY);
				o.renderDistance().set(16);
				o.particles().set(net.minecraft.server.level.ParticleStatus.ALL);
				o.bobView().set(false);
				o.fov().set(70);
				o.cloudStatus().set(net.minecraft.client.CloudStatus.FANCY);
				if (!client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
			String pick = System.getenv("TRAILER_SHOTS");
			shots = pick == null || pick.isBlank() ? null : java.util.Set.of(pick.split(","));
			Runnable[] benches = {this::panelBench, this::chartBench, this::pidBench, this::powerBench};
			int[][] numbers = {{1, 2, 3, 4}, {5, 6}, {7}, {8}};
			int[][][] boxes = {
					{{-10, 46, -16, 26}, {14}, {2, 34, -6, 12}, {4}},
					{{-10, 46, -18, 38}, {14}, {2, 34, -8, 26}, {4}},
					{{-10, 46, -18, 30}, {14}, {2, 34, -8, 18}, {4}},
					{{-10, 46, -24, 46}, {16}, {2, 34, -14, 36}, {5}}};
			for (int i = 0; i < benches.length; i++) {
				boolean any = false;
				for (int n : numbers[i]) {
					any |= wanted(n);
				}
				if (!any) {
					continue;
				}
				int[][] b = boxes[i];
				int[][] bases = {{4000, 1500}, {4500, 1500}, {500, 0}, {1000, 0}};   // the last two on the first trailer's grassland, not the taiga east of 4500
				site(bases[i][0], bases[i][1], b[0], b[1][0], b[2], b[3][0]);
				pad(b[2][0], b[2][2], b[2][1], b[2][3], "minecraft:grass_block");
				try {
					benches[i].run();
				} catch (Throwable t) {
					log("bench " + (i + 1) + " FAILED: " + t);
					t.printStackTrace();
				} finally {
					ReservoirData.testSeed = null;
					context.runOnClient(client -> {
						if (client.gui.screen() != null) {
							client.gui.screen().onClose();
						}
					});
					context.waitTicks(5);
				}
			}
		}
	}

	// ---- filming ---------------------------------------------------------------------------------------------------

	/** A world shot: the start still, then the move from a to b filmed over {@code ticks}, {@code beat} run each tick of it. */
	private void worldShot(int n, String name, long time, Cam a, Cam b, int ticks, IntConsumer beat) {
		server.runCommand("time set " + time);
		server.runCommand("weather clear");
		String id = String.format("%02d_%s", n, name);
		camera(a, true);
		context.waitTicks(100);
		camera(a, true);
		context.waitTicks(20);
		screenshot(id + "_a");
		camera(a, true);
		context.waitTicks(10);
		Filming film = context.computeOnClient(client -> Filming.start(client, id));
		try {
			context.waitTicks(10);
			for (int t = 0; t <= ticks; t++) {
				camera(a.lerp(b, (double) t / ticks), false);
				beat.accept(t);
				context.waitTick();
			}
			context.waitTicks(10);
		} finally {
			film.stop();
		}
		log("filmed " + id);
	}

	/** A screen shot: the screen is open already; {@code work} drives it while the window is filmed. */
	private void screenShot(int n, String name, Runnable work) {
		String id = String.format("%02d_%s", n, name);
		Filming film = context.computeOnClient(client -> Filming.start(client, id));
		try {
			context.waitTicks(15);
			work.run();
			context.waitTicks(10);
		} finally {
			film.stop();
		}
		log("filmed " + id);
	}

	private PlcCabinetBlockEntity cabinet(MinecraftServer srv, BlockPos at) {
		return (PlcCabinetBlockEntity) srv.overworld().getBlockEntity(at);
	}

	private dev.wildcatter.client.PlcScreen plcScreen(net.minecraft.client.Minecraft client) {
		return (dev.wildcatter.client.PlcScreen) client.gui.screen();
	}

	private dev.wildcatter.client.HmiScreen hmiScreen(net.minecraft.client.Minecraft client) {
		return (dev.wildcatter.client.HmiScreen) client.gui.screen();
	}

	/** Clicks GUI pixels with the real mouse. */
	private void mouse(int[] at) {
		if (at == null) {
			log("nothing to click");
			return;
		}
		double toWindow = context.computeOnClient(client -> (double) client.getWindow().getScreenWidth() / client.getWindow().getGuiScaledWidth());
		context.getInput().setCursorPos(at[0] * toWindow, at[1] * toWindow);
		context.waitTick();
		context.getInput().pressMouse(1);
		context.waitTick();
	}

	private void click(String label) {
		int[] at = context.computeOnClient(client -> plcScreen(client).buttonForTest(label));
		if (at == null) {
			log("no " + label + " button");
		}
		mouse(at);
	}

	private void field(String label, String value) {
		if (!context.computeOnClient(client -> plcScreen(client).setFieldForTest(label, value))) {
			log("no " + label + " field");
		}
	}

	private void hmiClick(String label) {
		mouse(context.computeOnClient(client -> hmiScreen(client).buttonForTest(label)));
	}

	private void hmiField(String label, String value) {
		context.computeOnClient(client -> hmiScreen(client).setFieldForTest(label, value));
	}

	/** Types into the code editor as a player does, {@code perTick} keys a tick; the editor keeps a line's indent, so a new line's tabs are fixed by hand. */
	private void type(String text, int perTick) {
		int indent = 0;
		int keys = 0;
		for (String line : text.split("\n", -1)) {
			if (keys > 0) {
				context.getInput().pressKey(257);
				int want = 0;
				while (want < line.length() && line.charAt(want) == '\t') {
					want++;
				}
				for (int i = want; i < indent; i++) {
					context.getInput().pressKey(259);
				}
				for (int i = indent; i < want; i++) {
					context.getInput().pressKey(258);
				}
				indent = want;
				line = line.substring(want);
				context.waitTicks(2);
			} else {
				while (indent < line.length() && line.charAt(indent) == '\t') {
					context.getInput().pressKey(258);
					indent++;
				}
				line = line.substring(indent);
			}
			for (int i = 0; i < line.length(); i++) {
				context.getInput().typeChar(line.charAt(i));
				if (++keys % perTick == 0) {
					context.waitTick();
				}
			}
			keys++;
		}
	}

	private void press(BlockPos device) {
		server.runOnServer(srv -> ((dev.wildcatter.control.OperatorDeviceBlockEntity) srv.overworld().getBlockEntity(device)).operate());
	}

	private void open(BlockPos cabinet) {
		server.runOnServer(srv -> cabinet(srv, cabinet).open(srv.getPlayerList().getPlayers().get(0)));
		context.waitTicks(20);
	}

	private void close() {
		context.runOnClient(client -> {
			if (client.gui.screen() != null) {
				client.gui.screen().onClose();
			}
		});
		context.waitTicks(5);
	}

	/** The player near a cabinet, as a screen's requests need (8 blocks), looking at it: the screen dims this behind it. */
	private void stand(Cam at) {
		camera(at, true);
		context.waitTicks(5);
	}

	/** A small steam set making 120 V for a control supply, as the PLC scenes' plant: boiler on a campfire, turbine east of it. */
	private void plant(int z) {
		put(10, 0, z, "minecraft:campfire[lit=true]");
		put(10, 1, z, "wildcatter:boiler");
		put(11, 1, z, "wildcatter:steam_turbine");
		on(level -> ((dev.wildcatter.block.BoilerBlockEntity) level.getBlockEntity(A(10, 1, z))).addWater(4000));
	}

	/** The plant, its 120 V into a 24 V supply, a box and the cabinet at x 15, grown. */
	private BlockPos cabinetAt(int z) {
		plant(z);
		put(12, 1, z, "wildcatter:plastic_wire_hot");
		put(13, 1, z, "wildcatter:control_supply");
		for (int x = 11; x <= 13; x++) {
			put(x, 1, z + 1, "wildcatter:plastic_wire_neutral");
		}
		put(14, 1, z, "wildcatter:junction_box");
		BlockPos at = A(15, 1, z);
		put(15, 1, z, "wildcatter:plc_cabinet[facing=south]");
		on(level -> dev.wildcatter.multiblock.BigParts.grow(level, at, level.getBlockState(at)));
		return at;
	}

	/** A true-size pumpjack over a new oil wellhead, its motor's box at {@code box}, facing east. */
	private void pumpjack(int bx, int z) {
		put(bx + 6, 1, z, "wildcatter:oil_wellhead[facing=east]");
		placeBig(WildcatterBlocks.PUMPJACK, A(bx + 5, 1, z), Direction.EAST);
	}

	/** A seed with oil under every one of these site columns, the way the PLC scenes pick theirs. */
	private void oilUnder(int[]... columns) {
		long seed = -1;
		for (long s = 1; s < 4_000_000 && seed < 0; s++) {
			boolean all = true;
			for (int i = 0; i < columns.length && all; i++) {
				Reservoir oil = WildcatterClientGameTest.fieldAnyRule(s, ox + columns[i][0], oz + columns[i][1]);
				all = oil != null && oil.kind() == Reservoir.Kind.OIL && oil.units() > 10_000;
			}
			if (all) {
				seed = s;
			}
		}
		log("oil seed " + seed);
		ReservoirData.testSeed = seed < 0 ? null : seed;
	}

	private void emptyJacks(int bx, int... zs) {
		on(level -> {
			for (int z : zs) {
				if (dev.wildcatter.multiblock.BigParts.entity(level, A(bx + 5, 1, z)) instanceof dev.wildcatter.block.PumpjackBlockEntity jack) {
					jack.testFill(0);
				}
			}
		});
	}

	private static String errors(List<String> all) {
		StringBuilder errors = new StringBuilder();
		all.stream().filter(java.util.Objects::nonNull).forEach(e -> errors.append(e).append("; "));
		return errors.toString();
	}

	// ---- 1-4: the panel ------------------------------------------------------------------------------------------------

	private static final String ST_HEAD = """
			(* P-101 transfer pump: start, e-stop, run lamp *)
			PROGRAM MAIN
			VAR
				start AT %IX0.0 : BOOL;      (* PB-101 *)
				estop_ok AT %IX0.1 : BOOL;   (* ES-101, NC *)
				running AT %IX0.2 : BOOL;
				tripped AT %IX0.3 : BOOL;
				run_cmd AT %QX16.0 : BOOL;
				lamp AT %QX16.1 : BOOL;
				green AT %QX16.2 : BOOL;
				red AT %QX16.3 : BOOL;
			END_VAR
			""";

	private static final String ST_BODY = """
			IF start AND estop_ok THEN
				run_cmd := TRUE;
			ELSIF NOT estop_ok OR tripped THEN
				run_cmd := FALSE;
			END_IF;
			lamp := running;
			green := running;
			red := NOT estop_ok;
			END_PROGRAM""";

	/**
	 * The plc scene's panel: a diesel set's 480 V through a combination starter to a pumpjack; a cabinet on its own 24 V, a box with
	 * a pushbutton, an e-stop, a pilot light and a stack light, a control cable north to the starter.
	 */
	private void panelBench() {
		oilUnder(new int[] {23, 0});
		BlockPos set = A(12, 1, 0);
		genset(set, NORTH, false);
		put(13, 1, 0, "wildcatter:cable_industrial");
		put(14, 1, 0, "wildcatter:motor_starter[facing=east]");
		put(15, 1, 0, "wildcatter:cable_industrial");
		put(16, 1, 0, "wildcatter:cable_industrial");
		pumpjack(17, 0);
		BlockPos at = cabinetAt(5);
		put(14, 2, 5, "wildcatter:junction_box");
		for (int z = 1; z <= 4; z++) {
			put(14, 1, z, "wildcatter:control_cable");
		}
		BlockPos button = A(14, 1, 6), estop = A(13, 2, 5), lamp = A(14, 2, 6), stack = A(14, 3, 5), starter = A(14, 1, 0);
		put(14, 1, 6, "wildcatter:pushbutton[facing=south]");
		put(13, 2, 5, "wildcatter:estop[facing=south]");
		put(14, 2, 6, "wildcatter:pilot_light[facing=south]");
		put(14, 3, 5, "wildcatter:stack_light[facing=south]");
		startGenset(set, NORTH, 1000);
		context.waitTicks(30);
		String wired = server.computeOnServer(srv -> {
			PlcCabinetBlockEntity box = cabinet(srv, at);
			for (var type : List.of(PlcModuleItem.Type.CPU_SMALL, PlcModuleItem.Type.DI16, PlcModuleItem.Type.DO16, PlcModuleItem.Type.AI8, PlcModuleItem.Type.AO4, PlcModuleItem.Type.COMM)) {
				box.seat(type);
			}
			List<String> all = new ArrayList<>(List.of(String.valueOf(box.land(button, 0, "%IX0.0")), String.valueOf(box.land(estop, 0, "%IX0.1")), String.valueOf(box.land(starter, 1, "%IX0.2")),
					String.valueOf(box.land(starter, 2, "%IX0.3")), String.valueOf(box.land(starter, 0, "%QX16.0")), String.valueOf(box.land(lamp, 0, "%QX16.1")),
					String.valueOf(box.land(stack, 2, "%QX16.2")), String.valueOf(box.land(stack, 0, "%QX16.3"))));
			all.removeIf("null"::equals);
			box.setTag(button, "PB-101");
			box.setTag(estop, "ES-101");
			var control = dev.wildcatter.control.ControlNet.deviceAt(srv.overworld(), starter);
			control.cycleHoa(starter);
			control.cycleHoa(starter);
			return errors(all);
		});
		log("panel wired: " + (wired.isEmpty() ? "ok" : wired) + " | " + read(at) + " | " + read(starter));
		emptyJacks(17, 0);
		// 2: the structured text, typed, checked, downloaded and run
		Cam desk = new Cam(15.5, 2.4, 9.5, 15.5, 1.8, 5.5);
		if (wanted(2) || wanted(3) || wanted(4) || wanted(1)) {
			server.runCommand("time set 12300");
			stand(desk);
			open(at);
			context.runOnClient(client -> plcScreen(client).replaceForTest(ST_HEAD));
			context.waitTick();
			context.getInput().pressKey(267);
			context.getInput().pressKey(267);
			context.getInput().pressKey(267);
			context.waitTicks(2);
			Runnable work = () -> {
				context.waitTicks(10);
				type(ST_BODY, 2);
				context.waitTicks(20);
				click("CHECK");
				context.waitTicks(30);
				click("DOWNLOAD");
				context.waitTicks(30);
				click("RUN");
				context.waitTicks(50);
			};
			if (wanted(2)) {
				screenShot(2, "code_st", work);
			} else {
				work.run();
			}
			log("st: " + context.computeOnClient(client -> plcScreen(client).errorsForTest().toString()) + " | " + read(at));
			close();
		}
		// 4: the button pressed, close to the station; the pumpjack starts, its pilot light on
		if (wanted(4)) {
			emptyJacks(17, 0);
			worldShot(4, "button_start", 12350, new Cam(15.3, 1.85, 7.7, 14.5, 1.45, 6.4), new Cam(17.8, 3.0, 11.0, 19.5, 2.4, 1.0), 220, t -> {
				if (t == 30) {
					press(button);
				}
			});
			log("after start: " + read(starter) + " | jack " + read(A(22, 1, 0)));
		} else {
			press(button);
		}
		context.waitTicks(40);
		// 1: the cabinet, close, its modules seated and its lights on, the jack nodding past it; a slow push-in
		if (wanted(1)) {
			emptyJacks(17, 0);
			worldShot(1, "cabinet", 12250, new Cam(18.2, 3.0, 11.0, 14.6, 1.9, 5.0), new Cam(15.75, 2.0, 6.8, 15.4, 1.8, 5.4), 220, t -> {});
		}
		// 3: the ladder: drawn with the toolbar, downloaded, run, and the button lights the rungs
		if (wanted(3)) {
			press(estop);   // the motor stopped first, so the ladder starts it from rest
			context.waitTicks(40);
			press(estop);
			context.waitTicks(20);
			server.runCommand("time set 12300");
			stand(desk);
			open(at);
			click("+LD");
			context.waitTicks(5);
			context.runOnClient(client -> plcScreen(client).selectUnitForTest("MAIN"));
			context.waitTicks(3);
			click("DELETE UNIT");
			context.waitTicks(5);
			click("VARS");
			context.runOnClient(client -> plcScreen(client).replaceForTest("""
					VAR
						start AT %IX0.0 : BOOL;
						healthy AT %IX0.1 : BOOL;
						running AT %IX0.2 : BOOL;
						tripped AT %IX0.3 : BOOL;
						coil AT %QX16.0 : BOOL;
						lamp AT %QX16.1 : BOOL;
						green AT %QX16.2 : BOOL;
					END_VAR"""));
			click("DRAW");
			context.waitTicks(5);
			screenShot(3, "ladder", () -> {
				int pace = 7;
				click("RUNG");
				context.waitTicks(pace);
				click("NO");
				field("variable", "start");
				context.waitTicks(pace);
				click("BRANCH");
				field("variable", "coil");
				context.waitTicks(pace);
				context.runOnClient(client -> plcScreen(client).deselectForTest());
				context.waitTick();
				click("NO");
				field("variable", "healthy");
				context.waitTicks(pace);
				click("NC");
				field("variable", "tripped");
				context.waitTicks(pace);
				click("COIL");
				field("variable", "coil");
				context.waitTicks(pace);
				click("RUNG");
				context.waitTicks(pace);
				click("NO");
				field("variable", "running");
				context.waitTicks(pace);
				click("COIL");
				field("variable", "lamp");
				context.waitTicks(pace);
				click("RUNG");
				context.waitTicks(pace);
				click("NO");
				field("variable", "running");
				context.waitTicks(pace);
				click("COIL");
				field("variable", "green");
				context.waitTicks(pace);
				context.runOnClient(client -> plcScreen(client).deselectForTest());
				context.waitTicks(10);
				click("DOWNLOAD");
				context.waitTicks(25);
				click("RUN");
				context.waitTicks(40);
				press(button);
				context.waitTicks(80);
			});
			log("ladder: " + context.computeOnClient(client -> plcScreen(client).errorsForTest().toString()) + " | live " + context.computeOnClient(client -> plcScreen(client).liveForTest())
					+ " | " + read(at) + " | " + read(starter));
			close();
		}
	}

	// ---- 5-6: the chart ------------------------------------------------------------------------------------------------

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

	private void step(String name) {
		click("STEP");
		field("step name", name);
	}

	private void trans(String condition) {
		click("TRANS");
		field("condition", condition);
	}

	private void action(String qualifier, String action, String time) {
		click("ACTION");
		field("qualifier", qualifier);
		field("action", action);
		field("time", time);
	}

	/** The plcSfc scene's line: three starters and pumpjacks standing in for belts, a cabinet with start, stop, e-stop, horn and stack light. */
	private void chartBench() {
		int bay = 14;
		oilUnder(new int[] {23, 0}, new int[] {23, 6}, new int[] {23, 12});
		BlockPos set = A(12, 1, 0);
		genset(set, NORTH, false);
		for (int z = 0; z <= 12; z++) {
			put(13, 1, z, "wildcatter:cable_industrial");
		}
		BlockPos[] starters = new BlockPos[3];
		for (int i = 0; i < 3; i++) {
			int line = i * 6;
			starters[i] = A(14, 1, line);
			put(14, 1, line, "wildcatter:motor_starter[facing=east]");
			put(15, 1, line, "wildcatter:cable_industrial");
			put(16, 1, line, "wildcatter:cable_industrial");
			pumpjack(17, line);
			put(14, 2, line, "wildcatter:junction_box");
		}
		for (int z = 0; z <= bay; z++) {
			if (z % 6 != 0 || z > 12) {
				put(14, 2, z, "wildcatter:control_cable");
			}
		}
		BlockPos at = cabinetAt(bay);
		put(14, 2, bay, "wildcatter:junction_box");
		put(14, 2, bay - 1, "wildcatter:junction_box");
		BlockPos startAt = A(14, 1, bay + 1), stopAt = A(14, 1, bay - 1), estop = A(13, 2, bay), hornAt = A(14, 2, bay + 1), stack = A(14, 3, bay);
		put(14, 1, bay + 1, "wildcatter:pushbutton[facing=south]");
		put(14, 1, bay - 1, "wildcatter:pushbutton[facing=north]");
		put(13, 2, bay, "wildcatter:estop[facing=south]");
		put(14, 2, bay + 1, "wildcatter:horn[facing=south]");
		put(14, 3, bay, "wildcatter:stack_light[facing=south]");
		startGenset(set, NORTH, 1000);
		context.waitTicks(30);
		String wired = server.computeOnServer(srv -> {
			PlcCabinetBlockEntity box = cabinet(srv, at);
			for (var type : List.of(PlcModuleItem.Type.CPU_SMALL, PlcModuleItem.Type.DI16, PlcModuleItem.Type.DO16)) {
				box.seat(type);
			}
			String[][] inputs = {{"%IX0.3", "%IX0.4", "%IX0.5"}, {"%IX0.6", "%IX0.7", "%IX1.0"}, {"%IX1.1", "%IX1.2", "%IX1.3"}};
			List<String> all = new ArrayList<>();
			all.add(box.land(startAt, 0, "%IX0.0"));
			all.add(box.land(stopAt, 0, "%IX0.1"));
			all.add(box.land(estop, 0, "%IX0.2"));
			for (int i = 0; i < 3; i++) {
				all.add(box.land(starters[i], 0, "%QX16." + i));
				for (int point = 0; point < 3; point++) {
					all.add(box.land(starters[i], point + 1, inputs[i][point]));
				}
				var control = dev.wildcatter.control.ControlNet.deviceAt(srv.overworld(), starters[i]);
				control.cycleHoa(starters[i]);
				control.cycleHoa(starters[i]);
			}
			all.add(box.land(hornAt, 0, "%QX16.3"));
			all.add(box.land(stack, 1, "%QX16.4"));
			all.add(box.land(stack, 2, "%QX16.5"));
			all.add(box.land(stack, 0, "%QX16.6"));
			return errors(all);
		});
		context.waitTicks(30);
		log("chart wired: " + (wired.isEmpty() ? "ok" : wired) + " | " + read(at));
		server.runCommand("time set 12300");
		stand(new Cam(15.5, 2.4, bay + 4.5, 15.5, 1.8, bay + 0.5));
		open(at);
		// drawn as the plcSfc scene draws it, with the toolbar and the mouse, off camera
		click("+SFC");
		context.waitTicks(3);
		field("name", "BELTS");
		context.runOnClient(client -> plcScreen(client).selectUnitForTest("MAIN"));
		context.waitTick();
		click("DELETE UNIT");
		context.waitTicks(3);
		click("VARS");
		context.runOnClient(client -> plcScreen(client).replaceForTest(BELT_VARS));
		click("DRAW");
		step("IDLE");
		trans("start_pb AND estop_ok");
		step("WARN1");
		action("N", "horn", "");
		context.waitTick();
		context.runOnClient(client -> plcScreen(client).sfcScrollForTest(0));
		mouse(context.computeOnClient(client -> plcScreen(client).sfcTransitionPinForTest(0, true)));
		step("WARN2");
		action("N", "amber", "");
		mouse(context.computeOnClient(client -> plcScreen(client).sfcStepPinForTest("WARN1", true)));
		trans("WARN1.T >= T#3s");
		mouse(context.computeOnClient(client -> plcScreen(client).sfcStepPinForTest("WARN2", true)));
		mouse(context.computeOnClient(client -> plcScreen(client).sfcTransitionPinForTest(1, false)));
		step("B3");
		action("S", "want3", "");
		trans("B3.T >= T#2s AND m3.STATE = 2");
		step("B2");
		action("S", "want2", "");
		trans("B2.T >= T#2s AND m2.STATE = 2");
		step("B1");
		action("S", "want1", "");
		trans("B1.T >= T#2s AND m1.STATE = 2");
		step("LINE_UP");
		action("N", "green", "");
		trans("stop_pb OR NOT estop_ok");
		step("STOP1");
		action("R", "want1", "");
		trans("NOT run1 AND STOP1.T >= T#1s");
		step("STOP2");
		action("R", "want2", "");
		trans("NOT run2 AND STOP2.T >= T#1s");
		step("STOP3");
		action("R", "want3", "");
		trans("NOT run3");
		context.waitTick();
		context.runOnClient(client -> plcScreen(client).sfcScrollForTest(300));
		context.waitTick();
		mouse(context.computeOnClient(client -> plcScreen(client).sfcTransitionPinForTest(8, true)));
		context.runOnClient(client -> plcScreen(client).sfcScrollForTest(0));
		context.waitTick();
		mouse(context.computeOnClient(client -> plcScreen(client).sfcStepPinForTest("IDLE", false)));
		context.runOnClient(client -> plcScreen(client).deselectForTest());
		context.waitTick();
		step("DEVICES");
		click("INITIAL");
		click("ACTION");
		field("action", "DRIVE");
		click("BODY");
		context.waitTick();
		context.runOnClient(client -> plcScreen(client).replaceForTest(BELT_DRIVE));
		click("DRAW");
		context.waitTick();
		click("DOWNLOAD");
		context.waitTicks(20);
		click("RUN");
		context.waitTicks(30);
		context.runOnClient(client -> plcScreen(client).deselectForTest());
		context.runOnClient(client -> plcScreen(client).sfcScrollForTest(0));
		context.waitTicks(10);
		log("chart: " + context.computeOnClient(client -> plcScreen(client).errorsForTest().toString()) + " | live " + context.computeOnClient(client -> plcScreen(client).liveForTest()) + " | " + read(at));
		java.util.function.Function<String, Object> tag = name -> server.computeOnServer(srv -> cabinet(srv, at).cpu().read("BELTS." + name));
		emptyJacks(17, 0, 6, 12);
		// 5: the chart running, its steps lighting in turn, the view following them down
		if (wanted(5)) {
			screenShot(5, "sfc", () -> {
				context.waitTicks(15);
				press(startAt);
				for (int t = 0; t < 300; t++) {
					int scroll = t < 90 ? 0 : Math.min(150, (t - 90) * 150 / 120);
					int s = scroll;
					context.runOnClient(client -> plcScreen(client).sfcScrollForTest(s));
					context.waitTick();
				}
			});
		} else {
			press(startAt);
			context.waitTicks(300);
		}
		log("chart after start: line up " + tag.apply("LINE_UP.X") + " | " + read(starters[0]) + " | " + read(starters[2]));
		// stopped again for the world shot, back to IDLE
		press(stopAt);
		context.waitTicks(200);
		close();
		log("chart after stop: idle " + tag.apply("IDLE.X"));
		// 6: in the world: horn and amber, then the three jacks in turn, then green
		if (wanted(6)) {
			emptyJacks(17, 0, 6, 12);
			worldShot(6, "sequence", 12350, new Cam(15.6, 2.9, bay + 4.6, 14.3, 2.7, bay + 0.5), new Cam(19.5, 4.6, bay + 7.5, 19.0, 2.0, 6.0), 320, t -> {
				if (t == 20) {
					press(startAt);
				}
			});
			log("sequence: line up " + tag.apply("LINE_UP.X") + " | " + read(starters[0]) + " | " + read(starters[1]) + " | " + read(starters[2]));
		}
	}

	// ---- 7: the PID ----------------------------------------------------------------------------------------------------

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

	/** The plcAnalog scene's loop: a drive-fed pump filling a day tank through a motor-operated valve, a level transmitter, an air-operated drain valve. */
	private void pidBench() {
		int bay = 10;
		BlockPos set = A(12, 1, 0);
		genset(set, NORTH, false);
		put(13, 1, 0, "wildcatter:cable_industrial");
		put(14, 1, 0, "wildcatter:cable_industrial");
		put(14, 2, 0, "wildcatter:cable_industrial");
		put(14, 3, 0, "wildcatter:cable_industrial");
		for (int x = 15; x <= 18; x++) {
			put(x, 3, 0, "wildcatter:cable_industrial");
		}
		for (int z = 1; z <= 3; z++) {
			put(18, 3, z, "wildcatter:cable_industrial");
		}
		put(18, 2, 3, "wildcatter:cable_industrial");
		BlockPos drive = A(15, 1, 0);
		placeBig(WildcatterBlocks.DRIVE_CABINET, drive, NORTH);
		put(15, 1, 1, "wildcatter:cable_industrial");
		put(15, 1, 2, "wildcatter:cable_industrial");
		on(level -> {
			var box = ((dev.wildcatter.block.StarterCabinetBlockEntity) level.getBlockEntity(drive)).starterForTest();
			while (box.overloadAmps() < 52) {
				box.nextOverloadSetting();
			}
		});
		fill(13, 0, 2, 17, 0, 4, "minecraft:stone");
		put(15, 0, 3, "minecraft:water");
		put(15, 1, 3, "wildcatter:process_water_pump[facing=north]");
		put(16, 1, 3, "wildcatter:pipe");
		put(17, 1, 3, "wildcatter:pipe");
		BlockPos mov = A(18, 1, 3);
		put(18, 1, 3, "wildcatter:motor_operated_valve");
		put(19, 1, 3, "wildcatter:pipe");
		BlockPos tank = A(20, 1, 3);
		put(20, 1, 3, "wildcatter:day_tank");
		BlockPos fcv = A(21, 1, 3);
		put(21, 1, 3, "wildcatter:control_valve");
		put(19, 3, 3, "wildcatter:air_compressor");
		put(20, 3, 3, "wildcatter:air_dryer");
		put(21, 3, 3, "wildcatter:air_tubing");
		put(21, 2, 3, "wildcatter:air_tubing");
		BlockPos lt = A(20, 1, 4), ft = A(15, 1, 4);
		put(20, 1, 4, "wildcatter:transmitter[facing=south]");
		put(15, 1, 4, "wildcatter:transmitter[facing=south]");
		for (int x = 14; x <= 21; x++) {
			boolean box = x == 15 || x == 16 || x == 18 || x == 20 || x == 21;
			put(x, 1, 5, box ? "wildcatter:junction_box" : "wildcatter:control_cable");
		}
		put(18, 1, 4, "wildcatter:control_cable");
		put(21, 1, 4, "wildcatter:junction_box");
		for (int z = 1; z <= 5; z++) {
			put(16, 2, z, "wildcatter:control_cable");
		}
		put(16, 2, 0, "wildcatter:control_cable");
		for (int z = 6; z < bay; z++) {
			put(14, 1, z, "wildcatter:control_cable");
		}
		BlockPos at = cabinetAt(bay);
		startGenset(set, NORTH, 2000);
		context.waitTicks(30);
		String wired = server.computeOnServer(srv -> {
			PlcCabinetBlockEntity box = cabinet(srv, at);
			for (var type : List.of(PlcModuleItem.Type.CPU_SMALL, PlcModuleItem.Type.DI16, PlcModuleItem.Type.DO16, PlcModuleItem.Type.AI8, PlcModuleItem.Type.AO4, PlcModuleItem.Type.HSC2)) {
				box.seat(type);
			}
			List<String> all = new ArrayList<>();
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
			var flow = (dev.wildcatter.control.TransmitterBlockEntity) srv.overworld().getBlockEntity(ft);
			flow.setQuantity(dev.wildcatter.control.Measurable.Quantity.FLOW);
			flow.setRange(0, 800);
			for (BlockPos device : new BlockPos[] {drive, fcv, mov}) {
				var control = dev.wildcatter.control.ControlNet.deviceAt(srv.overworld(), device);
				control.cycleHoa(device);
				control.cycleHoa(device);
			}
			return errors(all);
		});
		context.waitTicks(30);
		List<?> errors = server.computeOnServer(srv -> cabinet(srv, at).download(LEVEL_PROJECT));
		server.runOnServer(srv -> cabinet(srv, at).cpu().setMode(dev.wildcatter.plc.Cpu.Mode.RUN));
		log("pid wired: " + (wired.isEmpty() ? "ok" : wired) + " | compile " + errors + " | " + read(at));
		context.waitTicks(1800);
		java.util.function.Function<String, Object> tag = name -> server.computeOnServer(srv -> cabinet(srv, at).cpu().read("LEVEL." + name));
		log("pid holding: pv " + tag.apply("PV") + " | drive " + read(drive) + " | tank " + read(tank));
		// the screen: the loop's values online, the setpoint stepped up, the output answering
		server.runCommand("time set 12300");
		stand(new Cam(16.5, 2.4, bay + 4.5, 15.5, 1.8, bay + 0.5));
		open(at);
		click("ONLINE");
		context.waitTicks(3);
		for (String name : new String[] {"LEVEL.SP", "LEVEL.PV", "LEVEL.LOOP.OUT", "LEVEL.DRAIN", "LEVEL.LITRES"}) {
			field("tag or address", name);
			click("WATCH");
			context.waitTicks(2);
		}
		field("tag or address", "LEVEL.SP");
		field("value", "70");
		context.waitTicks(20);
		screenShot(7, "pid", () -> {
			context.waitTicks(30);
			click("WRITE");
			context.waitTicks(200);
		});
		close();
		log("pid after step: pv " + tag.apply("PV") + " | out " + tag.apply("LOOP.OUT"));
		// and the world: tank, pump, valve, transmitter, as the level climbs to the new setpoint
		worldShot(7, "pid_world", 12200, new Cam(20.5, 3.4, -3.5, 18.5, 1.6, 3.0), new Cam(18.5, 2.7, -1.6, 18.0, 1.5, 3.2), 180, t -> {});
	}

	// ---- 8: the gensets' HMI -----------------------------------------------------------------------

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

	/** The plcPower scene's site: two sets, three feeders of heaters, a cabinet and its HMI. */
	private void powerBench() {
		int bay = 22;
		BlockPos setA = A(12, 1, 0), setB = A(12, 1, 16);
		genset(setA, NORTH, false);
		genset(setB, NORTH, false);
		for (int z = 0; z <= 16; z++) {
			put(13, 1, z, "wildcatter:cable_industrial");
		}
		BlockPos[] feeders = {A(14, 1, 3), A(14, 1, 7), A(14, 1, 11)};
		for (int f = 0; f < 3; f++) {
			int z = 3 + f * 4;
			put(14, 1, z, "wildcatter:breaker_3p_industrial[facing=east,position=off]");
			for (int d = -1; d <= 1; d++) {
				put(15, 1, z + d, "wildcatter:cable_industrial");
				put(16, 1, z + d, "wildcatter:three_phase_heater");
			}
		}
		on(level -> {
			for (BlockPos feeder : feeders) {
				var breaker = (dev.wildcatter.block.BreakerBlockEntity) level.getBlockEntity(feeder);
				while (breaker.rating() < 100) {
					breaker.nextRating();
				}
			}
		});
		for (int z = 1; z < bay; z++) {
			boolean box = z == 1 || z == 3 || z == 7 || z == 11 || z == 16;
			put(14, 2, z, box ? "wildcatter:junction_box" : "wildcatter:control_cable");
		}
		put(14, 2, 0, "wildcatter:control_cable");
		for (int line : new int[] {0, 16}) {
			put(12, 3, line, "wildcatter:control_cable");
			put(13, 3, line, "wildcatter:control_cable");
			put(14, 3, line, "wildcatter:control_cable");
		}
		BlockPos at = cabinetAt(bay);
		put(14, 2, bay, "wildcatter:junction_box");
		put(16, 1, bay, "wildcatter:network_cable");
		put(17, 1, bay, "wildcatter:network_cable");
		BlockPos hmiAt = A(18, 1, bay);
		put(18, 1, bay, "wildcatter:hmi_panel[facing=south]");
		context.waitTicks(30);
		on(level -> {
			for (BlockPos set : new BlockPos[] {setA, setB}) {
				BlockPos tank = dev.wildcatter.block.GensetSkid.base(set, NORTH, false);
				if (level.getBlockEntity(tank) instanceof dev.wildcatter.block.GensetPartBlockEntity t) {
					t.setFuel(1500);
				}
			}
		});
		server.runOnServer(srv -> {
			PlcCabinetBlockEntity box = cabinet(srv, at);
			for (var type : List.of(PlcModuleItem.Type.CPU_SMALL, PlcModuleItem.Type.DI16, PlcModuleItem.Type.DO16, PlcModuleItem.Type.AI8, PlcModuleItem.Type.AO4, PlcModuleItem.Type.COMM)) {
				box.seat(type);
			}
			box.setStation("PLC-1");
		});
		context.waitTicks(25);
		String lands = server.computeOnServer(srv -> {
			PlcCabinetBlockEntity box = cabinet(srv, at);
			List<String> all = new ArrayList<>();
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
			for (BlockPos device : new BlockPos[] {setA, setB, feeders[0], feeders[1], feeders[2]}) {
				var control = dev.wildcatter.control.ControlNet.deviceAt(srv.overworld(), device);
				if (control != null) {
					control.cycleHoa(device);
					control.cycleHoa(device);
				}
			}
			return errors(all);
		});
		context.waitTicks(25);
		List<?> errors = server.computeOnServer(srv -> cabinet(srv, at).download(POWER_PROJECT));
		server.runOnServer(srv -> cabinet(srv, at).cpu().setMode(dev.wildcatter.plc.Cpu.Mode.RUN));
		log("power wired: " + (lands.isEmpty() ? "ok" : lands) + " | compile " + errors + " | " + read(at));
		java.util.function.Function<String, Object> tag = name -> server.computeOnServer(srv -> cabinet(srv, at).cpu().read("SITE." + name));
		// the operator's screen, laid out through its real editor, off camera
		server.runCommand("time set 12300");
		stand(new Cam(18.5, 2.4, bay + 3.5, 18.5, 1.6, bay + 0.5));
		server.runOnServer(srv -> ((dev.wildcatter.control.HmiPanelBlockEntity) srv.overworld().getBlockEntity(hmiAt)).open(srv.getPlayerList().getPlayers().get(0)));
		context.waitTicks(20);
		hmiClick("EDIT");
		context.waitTicks(2);
		String[][] widgets = {{"BUTTON", "SITE START", "SITE.GO", "0", "1"}, {"VALUE", "TOTAL KW", "SITE.TOTAL_KW", "0", "100"}, {"LAMP", "SHEDDING", "SITE.SHED", "0", "1"},
				{"BAR", "SET A KW", "SITE.A_KW", "0", "100"}, {"BAR", "SET B KW", "SITE.B_KW", "0", "100"}, {"LAMP", "FEEDER 3", "SITE.F3_CLOSED", "0", "1"},
				{"LAMP", "SET A CLOSED", "SITE.A_CLOSED", "0", "1"}, {"LAMP", "SET B CLOSED", "SITE.B_CLOSED", "0", "1"}, {"TREND", "TOTAL KW", "SITE.TOTAL_KW", "0", "150"}};
		for (String[] w : widgets) {
			context.runOnClient(client -> hmiScreen(client).kindForTest(w[0]));
			context.waitTick();
			hmiField("label", w[1]);
			hmiField("tag", w[2]);
			hmiField("low", w[3]);
			hmiField("high", w[4]);
			hmiClick("ADD");
			context.waitTick();
		}
		hmiClick("ALARMS");
		context.waitTicks(2);
		hmiField("alarm tag", "SITE.SHED");
		hmiField("op", "=");
		hmiField("limit", "1");
		hmiField("pri", "1");
		hmiField("message", "Load shed: feeder 3 open");
		hmiClick("ADD ALARM");
		context.waitTick();
		hmiClick("RUN");
		context.waitTicks(10);
		log("hmi widgets: " + context.computeOnClient(client -> hmiScreen(client).layoutSizeForTest()));
		// 8: start pressed, the sets start, close and load; the shed raises its alarm; ACK; the trend. Filmed whole, cut in the edit
		Runnable work = () -> {
			context.waitTicks(20);
			mouse(context.computeOnClient(client -> hmiScreen(client).widgetForTest(0)));
			context.waitTicks(1500);
			hmiClick("ALARMS");
			context.waitTicks(80);
			hmiClick("ACK");
			context.waitTicks(60);
			hmiClick("RUN");
			context.waitTicks(80);
		};
		if (wanted(8)) {
			screenShot(8, "hmi", work);
		} else {
			work.run();
		}
		log("hmi: A " + tag.apply("A_CLOSED") + " B " + tag.apply("B_CLOSED") + " shed " + tag.apply("SHED") + " total " + tag.apply("TOTAL_KW"));
		close();
		// 8b: the two sets running, loaded
		if (wanted(8)) {
			worldShot(8, "gensets_world", 12400, new Cam(25.0, 5.0, 9.0, 9.0, 1.6, 8.0), new Cam(21.5, 3.4, 7.0, 9.0, 2.0, 8.0), 180, t -> {});
		}
	}
}
