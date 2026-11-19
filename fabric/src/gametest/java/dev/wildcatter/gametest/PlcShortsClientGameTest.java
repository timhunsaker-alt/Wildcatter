package dev.wildcatter.gametest;

import dev.wildcatter.control.PlcCabinetBlockEntity;
import dev.wildcatter.control.PlcModuleItem;
import dev.wildcatter.well.ReservoirData;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

/**
 * {@code plcShorts} (WILDCATTER_ONLY=plcShorts; never part of the full suite): the shots for three vertical PLC shorts, in the trailers'
 * real generated world. SHORTS picks them ({@code code}, {@code gensets}, {@code pid}; all by default). World shots are filmed in a portrait
 * window (486 x 864 points) with a 1080 x 1920 start still in TRAILER_OUT/&lt;short&gt;/; screen shots in the landscape window (1440 x 810).
 * Films land in {@code build/scene-videos/<short>__NN_<shot>.mov}. Every event a film shows is logged with its second from the film's start
 * ({@code event <film> <what> at s}), so a screen take and a world take of the same moment can be put side by side.
 * <ul>
 * <li>code: a ladder drawn, downloaded and run, the pumpjack starting as its rung goes live; then stop and start from the buttons.</li>
 * <li>gensets: feeder 3 put on from the HMI, the PLC starting set B, syncing and closing it, the load split; set B trips (a trip the
 * scene injects through the set's own frequency trip) and the program sheds feeder 3.</li>
 * <li>pid: a level loop on the HMI, its setpoint stepped from 50 to 70 % and its drain opened from 30 to 60 %, the loop recovering.</li>
 * </ul>
 */
public class PlcShortsClientGameTest extends PlcTrailerClientGameTest {
	/** Late golden hour, the sun low in the west. */
	private static final long WORLD_TIME = 12350;
	private Path root;
	private String shortName = "";
	private String filmId = "";
	private long filmStart;
	private java.util.Set<String> picked;

	@Override
	public void runTest(ClientGameTestContext context) {
		String only = System.getenv("WILDCATTER_ONLY");
		if (only == null || !java.util.Arrays.asList(only.split(",")).contains("plcShorts")) {
			return;
		}
		this.context = context;
		String dir = System.getenv("TRAILER_OUT");
		root = Path.of(dir == null || dir.isBlank() ? "plc-shorts" : dir).toAbsolutePath();
		String pick = System.getenv("SHORTS");
		picked = pick == null || pick.isBlank() ? java.util.Set.of("code", "gensets", "pid") : java.util.Set.of(pick.split(","));
		String films = System.getenv("TRAILER_SHOTS");
		shots = films == null || films.isBlank() ? null : java.util.Set.of(films.split(","));
		landscape();
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
				o.guiScale().set(6);   // the screens' text as big as the landscape window allows (it clamps lower in the portrait one)
				if (!client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
			String[] names = {"code", "gensets", "pid"};
			Runnable[] benches = {this::codeBench, this::gensetBench, this::levelBench};
			int[][][] boxes = {
					{{-10, 46, -16, 30}, {14}, {2, 34, -6, 16}, {4}},
					{{-10, 46, -24, 46}, {16}, {2, 34, -10, 32}, {5}},
					{{-10, 46, -18, 34}, {14}, {2, 34, -8, 22}, {4}}};
			int[][] bases = {{4000, 1500}, {1000, 0}, {500, 0}};
			for (int i = 0; i < benches.length; i++) {
				if (!picked.contains(names[i])) {
					continue;
				}
				shortName = names[i];
				try {
					Files.createDirectories(root.resolve(shortName));
				} catch (java.io.IOException ex) {
					throw new RuntimeException(ex);
				}
				int[][] b = boxes[i];
				site(bases[i][0], bases[i][1], b[0], b[1][0], b[2], b[3][0]);
				pad(b[2][0], b[2][2], b[2][1], b[2][3], "minecraft:grass_block");
				try {
					benches[i].run();
				} catch (Throwable t) {
					log("short " + shortName + " FAILED: " + t);
					t.printStackTrace();
				} finally {
					ReservoirData.testSeed = null;
					close();
					landscape();
				}
			}
		}
	}

	// ---- filming ----------------------------------------------------------------------------------------------------------

	private void landscape() {
		context.getInput().resizeWindow(1440, 810);
		context.waitTicks(10);
	}

	/** The tall window for world shots: 9:16, the largest that fits whole on the laptop's screen (1512 x 982 points, 944 below the menu bar). */
	private void portrait() {
		context.getInput().resizeWindow(486, 864);
		context.waitTicks(10);
	}

	/** Logs an event in the film being shot, with its second from the film's start. */
	private void mark(String what) {
		log(String.format(java.util.Locale.ROOT, "event %s %s at %.2f s", filmId, what, (System.nanoTime() - filmStart) / 1e9));
	}

	private Filming startFilm(String id) {
		filmId = id;
		Filming film = context.computeOnClient(client -> Filming.start(client, shortName + "__" + id));
		filmStart = System.nanoTime();
		return film;
	}

	/** A portrait world shot: the 1080 x 1920 start still, then the move from a to b filmed over {@code ticks}, {@code beat} run each tick. */
	private void tallShot(int n, String name, long time, Cam a, Cam b, int ticks, IntConsumer beat) {
		String id = String.format("%02d_%s", n, name);
		if (!wanted(n)) {
			for (int t = 0; t <= ticks; t++) {
				beat.accept(t);
				context.waitTick();
			}
			return;
		}
		close();
		portrait();
		server.runCommand("time set " + time);
		server.runCommand("weather clear");
		camera(a, true);
		context.waitTicks(80);
		camera(a, true);
		context.waitTicks(20);
		context.takeScreenshot(TestScreenshotOptions.of(id + "_a").withSize(1080, 1920).withDestinationDir(root.resolve(shortName)).disableCounterPrefix());
		camera(a, true);
		context.waitTicks(10);
		Filming film = startFilm(id);
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
		log("filmed " + shortName + " " + id);
	}

	/** A landscape screen shot: the screen is open already; {@code work} drives it while the window is filmed. */
	private void wideShot(int n, String name, Runnable work) {
		String id = String.format("%02d_%s", n, name);
		if (!wanted(n)) {
			work.run();
			return;
		}
		Filming film = startFilm(id);
		try {
			context.waitTicks(15);
			work.run();
			context.waitTicks(10);
		} finally {
			film.stop();
		}
		log("filmed " + shortName + " " + id);
	}

	/** Runs {@code ticks} ticks, marking each change of the tags watched as an event of the film. */
	private void watch(int ticks, java.util.function.Supplier<String> state, IntConsumer beat) {
		String last = state.get();
		for (int t = 0; t < ticks; t++) {
			beat.accept(t);
			context.waitTick();
			String now = state.get();
			if (!now.equals(last)) {
				mark(now);
				last = now;
			}
		}
	}

	private Object tag(BlockPos at, String name) {
		return server.computeOnServer(srv -> cabinet(srv, at).cpu().read(name));
	}

	private void write(BlockPos at, String name, Object value) {
		boolean ok = server.computeOnServer(srv -> cabinet(srv, at).cpu().write(name, value));
		if (!ok) {
			log("could not write " + name);
		}
	}

	/** An HMI's screen and alarms, saved as its editor saves them (laid out off camera, at set places). */
	private void layoutHmi(BlockPos hmi, List<String> rows, List<String> alarms) {
		server.runOnServer(srv -> {
			CompoundTag tag = new CompoundTag();
			tag.putString("action", "save");
			ListTag layout = new ListTag();
			rows.forEach(r -> layout.add(StringTag.valueOf(r)));
			ListTag alarmRows = new ListTag();
			alarms.forEach(r -> alarmRows.add(StringTag.valueOf(r)));
			tag.put("layout", layout);
			tag.put("alarms", alarmRows);
			((dev.wildcatter.control.HmiPanelBlockEntity) srv.overworld().getBlockEntity(hmi)).request(srv.getPlayerList().getPlayers().get(0), tag);
		});
	}

	private void openHmi(BlockPos hmi) {
		server.runOnServer(srv -> ((dev.wildcatter.control.HmiPanelBlockEntity) srv.overworld().getBlockEntity(hmi)).open(srv.getPlayerList().getPlayers().get(0)));
		context.waitTicks(20);
		hmiClick("RUN");
		context.waitTicks(10);
	}

	// ---- code: a rung written, downloaded, live, and the pumpjack obeying -------------------------------------------------------

	private static final String CODE_VARS = """
			VAR
				start_pb AT %IX0.0 : BOOL;
				stop_pb AT %IX0.1 : BOOL;
				estop_ok AT %IX0.2 : BOOL;
				running AT %IX0.3 : BOOL;
				tripped AT %IX0.4 : BOOL;
				pump AT %QX16.0 : BOOL;
				green AT %QX16.1 : BOOL;
				red AT %QX16.2 : BOOL;
				stopped : BOOL;
			END_VAR""";

	/**
	 * The plc scene's panel, close: a diesel set's 480 V through a combination starter to a pumpjack; the cabinet with a start and a stop
	 * button and an e-stop; a stack light on its own box beside the jack, on the starter's control cable.
	 */
	private void codeBench() {
		oilUnder(new int[] {23, 0});
		BlockPos set = A(12, 1, 0);
		genset(set, NORTH, false);
		put(13, 1, 0, "wildcatter:cable_industrial");
		put(14, 1, 0, "wildcatter:motor_starter[facing=east]");
		put(15, 1, 0, "wildcatter:cable_industrial");
		put(16, 1, 0, "wildcatter:cable_industrial");
		pumpjack(17, 0);
		BlockPos at = cabinetAt(6);
		for (int z = 1; z <= 5; z++) {
			put(14, 1, z, z == 3 ? "wildcatter:junction_box" : "wildcatter:control_cable");
		}
		for (int x = 15; x <= 17; x++) {
			put(x, 1, 3, "wildcatter:control_cable");
		}
		put(18, 1, 3, "wildcatter:junction_box");
		put(18, 2, 3, "wildcatter:stack_light[facing=south]");
		put(14, 2, 6, "wildcatter:junction_box");
		BlockPos startAt = A(14, 1, 7), stopAt = A(14, 2, 7), estop = A(13, 2, 6), stack = A(18, 2, 3), starter = A(14, 1, 0);
		put(14, 1, 7, "wildcatter:pushbutton[facing=south]");
		put(14, 2, 7, "wildcatter:pushbutton[facing=south]");
		put(13, 2, 6, "wildcatter:estop[facing=south]");
		startGenset(set, NORTH, 1000);
		context.waitTicks(30);
		String wired = server.computeOnServer(srv -> {
			PlcCabinetBlockEntity box = cabinet(srv, at);
			for (var type : List.of(PlcModuleItem.Type.CPU_SMALL, PlcModuleItem.Type.DI16, PlcModuleItem.Type.DO16)) {
				box.seat(type);
			}
			List<String> all = new ArrayList<>();
			all.add(box.land(startAt, 0, "%IX0.0"));
			all.add(box.land(stopAt, 0, "%IX0.1"));
			all.add(box.land(estop, 0, "%IX0.2"));
			all.add(box.land(starter, 1, "%IX0.3"));
			all.add(box.land(starter, 2, "%IX0.4"));
			all.add(box.land(starter, 0, "%QX16.0"));
			all.add(box.land(stack, 2, "%QX16.1"));
			all.add(box.land(stack, 0, "%QX16.2"));
			box.setTag(startAt, "PB-101");
			box.setTag(stopAt, "PB-102");
			box.setTag(estop, "ES-101");
			var control = dev.wildcatter.control.ControlNet.deviceAt(srv.overworld(), starter);
			control.cycleHoa(starter);
			control.cycleHoa(starter);
			return errors(all);
		});
		log("code wired: " + (wired.isEmpty() ? "ok" : wired) + " | " + read(at) + " | " + read(starter));
		emptyJacks(17, 0);
		String[] unit = {"POU2"};
		java.util.function.Supplier<String> state = () -> "pump " + tag(at, unit[0] + ".PUMP") + " running " + tag(at, unit[0] + ".RUNNING") + " stopped " + tag(at, unit[0] + ".STOPPED");
		Cam desk = new Cam(15.5, 2.4, 10.5, 15.5, 1.8, 6.5);
		// 2: the ladder drawn with the toolbar and the mouse
		server.runCommand("time set 12300");
		stand(desk);
		open(at);
		click("+LD");
		context.waitTicks(5);
		context.runOnClient(client -> plcScreen(client).selectUnitForTest("MAIN"));
		context.waitTicks(3);
		click("DELETE UNIT");
		context.waitTicks(5);
		String names = context.computeOnClient(client -> plcScreen(client).unitNamesForTest());
		unit[0] = names.split(",")[0].split(":")[0];
		log("units: " + names + ", the ladder " + unit[0]);
		click("VARS");
		context.runOnClient(client -> plcScreen(client).replaceForTest(CODE_VARS));
		click("DRAW");
		context.waitTicks(10);
		int pace = 5;
		wideShot(2, "draw", () -> {
			String[][][] rungs = {
					{{"NC", "stopped"}, {"NO", "estop_ok"}, {"NC", "tripped"}, {"COIL", "pump"}},
					{{"NO", "running"}, {"COIL", "green"}},
					{{"NC", "running"}, {"COIL", "red"}},
					{{"NO", "stop_pb"}, {"S", "stopped"}},
					{{"NO", "start_pb"}, {"R", "stopped"}}};
			for (String[][] rung : rungs) {
				click("RUNG");
				context.waitTicks(pace);
				for (String[] element : rung) {
					click(element[0]);
					field("variable", element[1]);
					context.waitTicks(pace);
				}
				context.runOnClient(client -> plcScreen(client).deselectForTest());
				context.waitTick();
			}
			context.waitTicks(10);
		});
		// 3: downloaded and run: the rungs go green and the pump starts
		wideShot(3, "go_live", () -> {
			click("DOWNLOAD");
			context.waitTicks(25);
			mark("RUN clicked");
			click("RUN");
			watch(100, () -> "live " + context.computeOnClient(client -> plcScreen(client).liveForTest()) + " rung1 " + context.computeOnClient(client -> plcScreen(client).rungPowerForTest(1))
					+ " | " + state.get(), t -> {});
		});
		log("ladder: " + context.computeOnClient(client -> plcScreen(client).errorsForTest().toString()) + " | units " + context.computeOnClient(client -> plcScreen(client).unitNamesForTest())
				+ " | " + read(at) + " | " + read(starter));
		close();
		Cam liveA = new Cam(19.3, 1.9, 8.0, 20.2, 3.4, 1.0), liveB = new Cam(19.0, 2.4, 7.0, 20.2, 3.6, 1.0);
		// 4: the same moment in the world: the CPU from STOP to RUN, the stack light from red to green, the jack starting
		server.runOnServer(srv -> cabinet(srv, at).cpu().setMode(dev.wildcatter.plc.Cpu.Mode.STOP));
		context.waitTicks(200);
		emptyJacks(17, 0);
		log("before live world: " + state.get() + " | " + read(starter));
		tallShot(4, "go_live_world", WORLD_TIME, liveA, liveB, 200, t -> {
			if (t == 40) {
				mark("CPU to RUN");
				server.runOnServer(srv -> cabinet(srv, at).cpu().setMode(dev.wildcatter.plc.Cpu.Mode.RUN));
			}
			if (t % 10 == 0 && t >= 40 && wanted(4)) {
				mark(state.get());
			}
		});
		context.waitTicks(60);
		// 5: online, stop and start from the buttons, rung 1 going dark and green again
		server.runCommand("time set 12300");
		landscape();
		stand(desk);
		open(at);
		context.waitTicks(10);
		context.runOnClient(client -> plcScreen(client).selectUnitForTest(unit[0]));
		context.waitTicks(10);
		log("reopened: live " + context.computeOnClient(client -> plcScreen(client).liveForTest()) + " rung1 " + context.computeOnClient(client -> plcScreen(client).rungPowerForTest(1)));
		wideShot(5, "stop_start", () -> {
			watch(200, () -> "rung1 " + context.computeOnClient(client -> plcScreen(client).rungPowerForTest(1)) + " | " + state.get(), t -> {
				if (t == 20) {
					mark("STOP pressed");
					press(stopAt);
				}
				if (t == 110) {
					mark("START pressed");
					press(startAt);
				}
			});
		});
		close();
		// 6: and in the world
		emptyJacks(17, 0);
		tallShot(6, "stop_start_world", WORLD_TIME, liveB, liveA, 220, t -> {
			if (t == 30) {
				mark("STOP pressed");
				press(stopAt);
			}
			if (t == 130) {
				mark("START pressed");
				press(startAt);
			}
			if (t % 10 == 0 && wanted(6)) {
				mark(state.get());
			}
		});
		// 1: the hook: the jack nodding, a low rise up past its head against the sky
		emptyJacks(17, 0);
		tallShot(1, "hook", WORLD_TIME, new Cam(27.5, 1.4, 5.0, 22.0, 4.0, 0.5), new Cam(26.0, 2.8, 3.4, 21.5, 5.0, 0.5), 180, t -> {});
		log("code end: " + state.get() + " | jack " + read(A(22, 1, 0)));
	}

	// ---- gensets: two sets, one program -----------------------------------------------------------------------------------

	private static final String GRID_PROJECT = """
			PROGRAM GRID
			VAR
				a_running AT %IX0.0 : BOOL;
				a_closed AT %IX0.1 : BOOL;
				a_tripped AT %IX0.2 : BOOL;
				b_running AT %IX0.3 : BOOL;
				b_closed AT %IX0.4 : BOOL;
				f1_closed AT %IX0.5 : BOOL;
				f2_closed AT %IX0.6 : BOOL;
				f3_closed AT %IX0.7 : BOOL;
				b_tripped AT %IX1.0 : BOOL;
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
				f3_on : BOOL;
				one_set_kw : REAL := 100.0;
				a_kw, b_kw, total_kw : REAL;
				b_go : BOOL;
				shed : BOOL;
				settle : TON;
				need_b : TON;
				over_a : TON;
			END_VAR
			a_kw := SCALE(INT_TO_REAL(WORD_TO_INT(a_kw_raw)), 0.0, 32000.0, 0.0, 625.0);
			b_kw := SCALE(INT_TO_REAL(WORD_TO_INT(b_kw_raw)), 0.0, 32000.0, 0.0, 625.0);
			total_kw := a_kw + b_kw;
			a_run := TRUE;
			a_permit := TRUE;
			a_trim := INT_TO_WORD(16000);
			settle(IN := a_closed, PT := T#2s);
			f1_close := settle.Q;
			f2_close := settle.Q AND f1_closed;
			(* more load than one set may carry: start the second set *)
			need_b(IN := a_kw > one_set_kw AND NOT b_tripped, PT := T#2s);
			IF need_b.Q THEN
				b_go := TRUE;
			END_IF;
			IF b_tripped THEN
				b_go := FALSE;
			END_IF;
			b_run := b_go;
			b_permit := b_go AND b_running AND a_closed;
			(* no second set to come: shed feeder 3 *)
			over_a(IN := a_kw > one_set_kw AND NOT b_go, PT := T#3s);
			IF over_a.Q THEN
				shed := TRUE;
			END_IF;
			f3_close := f3_on AND f2_closed AND NOT shed;
			f3_open := shed OR NOT f3_on;
			END_PROGRAM""";

	/** The plcPower scene's site, closer: two diesel sets four blocks apart, three feeders of heaters, a cabinet and its HMI. */
	private void gensetBench() {
		int bay = 24;
		BlockPos setA = A(12, 1, 0), setB = A(12, 1, 4);
		genset(setA, NORTH, false);
		genset(setB, NORTH, false);
		for (int z = 0; z <= 18; z++) {
			put(13, 1, z, "wildcatter:cable_industrial");
		}
		BlockPos[] feeders = {A(14, 1, 10), A(14, 1, 14), A(14, 1, 18)};
		for (BlockPos feeder : feeders) {
			int z = feeder.getZ() - oz;
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
			boolean box = z == 1 || z == 4 || z == 10 || z == 14 || z == 18;
			put(14, 2, z, box ? "wildcatter:junction_box" : "wildcatter:control_cable");
		}
		put(14, 2, 0, "wildcatter:control_cable");
		for (int line : new int[] {0, 4}) {
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
			all.add(box.land(setB, 6, "%IX1.0"));
			all.add(box.land(setB, 9, "%IW34"));
			all.add(box.land(feeders[0], 0, "%QX16.4"));
			all.add(box.land(feeders[1], 0, "%QX16.5"));
			all.add(box.land(feeders[2], 0, "%QX16.6"));
			all.add(box.land(feeders[2], 1, "%QX16.7"));
			all.add(box.land(feeders[0], 2, "%IX0.5"));
			all.add(box.land(feeders[1], 2, "%IX0.6"));
			all.add(box.land(feeders[2], 2, "%IX0.7"));
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
		List<?> errors = server.computeOnServer(srv -> cabinet(srv, at).download(GRID_PROJECT));
		server.runOnServer(srv -> cabinet(srv, at).cpu().setMode(dev.wildcatter.plc.Cpu.Mode.RUN));
		log("grid wired: " + (lands.isEmpty() ? "ok" : lands) + " | compile " + errors + " | " + read(at));
		layoutHmi(hmiAt, List.of(
				"BUTTON|FEEDER 3|GRID.F3_ON|0|1|8|24",
				"LAMP|SET B RUNNING|GRID.B_RUNNING|0|1|102|24",
				"LAMP|SET B CLOSED|GRID.B_CLOSED|0|1|196|24",
				"LAMP|SHEDDING|GRID.SHED|0|1|290|24",
				"VALUE|TOTAL KW|GRID.TOTAL_KW|0|150|384|24",
				"BAR|SET A KW|GRID.A_KW|0|150|8|66",
				"BAR|SET B KW|GRID.B_KW|0|150|102|66",
				"LAMP|F3 CLOSED|GRID.F3_CLOSED|0|1|196|66",
				"TREND|SET A KW|GRID.A_KW|0|150|8|108",
				"TREND|SET B KW|GRID.B_KW|0|150|196|108"),
				List.of("GRID.SHED|=|1|1|Load shed: feeder 3 open"));
		java.util.function.Supplier<String> state = () -> String.format(java.util.Locale.ROOT, "A %.0f kW B %.0f kW | B running %s closed %s tripped %s | f3 %s shed %s",
				((Number) tag(at, "GRID.A_KW")).doubleValue(), ((Number) tag(at, "GRID.B_KW")).doubleValue(), tag(at, "GRID.B_RUNNING"), tag(at, "GRID.B_CLOSED"), tag(at, "GRID.B_TRIPPED"),
				tag(at, "GRID.F3_CLOSED"), tag(at, "GRID.SHED"));
		java.util.function.Supplier<String> steps = () -> "B running " + tag(at, "GRID.B_RUNNING") + " closed " + tag(at, "GRID.B_CLOSED") + " tripped " + tag(at, "GRID.B_TRIPPED") + " | f3 "
				+ tag(at, "GRID.F3_CLOSED") + " shed " + tag(at, "GRID.SHED");
		context.waitTicks(1600);   // set A started, closed, feeders 1 and 2 on; a minute and more of trend
		log("grid holding: " + state.get() + " | " + read(setA));
		Cam hmiStand = new Cam(18.5, 2.4, bay + 3.5, 18.5, 1.6, bay + 0.5);
		// 2: FEEDER 3 pressed on the HMI: set A over its share, the PLC starts set B, syncs it, closes it, the load splits
		server.runCommand("time set 12300");
		stand(hmiStand);
		openHmi(hmiAt);
		wideShot(2, "load_sync", () -> {
			context.waitTicks(20);
			mark("FEEDER 3 pressed");
			mouse(context.computeOnClient(client -> hmiScreen(client).widgetForTest(0)));
			watch(560, steps, t -> {});
			mark(state.get());
		});
		log("after sync: " + state.get() + " | " + read(setB));
		close();
		// 3: the same, in the world, on set B: feeder 3 put on as the HMI's button does, set B cranking, running and closing
		write(at, "GRID.F3_ON", false);
		write(at, "GRID.B_GO", false);
		context.waitTicks(500);
		log("reset for the world take: " + state.get());
		Cam bA = new Cam(10.5, 1.8, 9.0, 10.0, 3.2, 4.0), bB = new Cam(10.2, 2.4, 8.0, 10.0, 3.6, 4.0);
		tallShot(3, "sync_world", WORLD_TIME, bA, bB, 380, t -> {
			if (t == 20) {
				mark("FEEDER 3 on");
				write(at, "GRID.F3_ON", true);
			}
			if (t % 20 == 0 && wanted(3)) {
				mark(state.get());
			}
		});
		context.waitTicks(200);
		log("both on: " + state.get());
		// 1: the hook: both sets running and loaded, a low rise between their stacks
		tallShot(1, "hook", WORLD_TIME, new Cam(10.6, 1.6, -4.6, 10.0, 3.4, 0.0), new Cam(10.1, 2.6, -3.7, 10.0, 3.9, 0.0), 180, t -> {});
		// 4: set B trips; the program sheds feeder 3 to keep set A within its share
		landscape();
		server.runCommand("time set 12300");
		stand(hmiStand);
		openHmi(hmiAt);
		wideShot(4, "trip_shed", () -> {
			context.waitTicks(30);
			mark("set B tripped");
			server.runOnServer(srv -> panel(srv, setB).tripOnFrequency(dev.wildcatter.energy.ElectricalStandard.active().hertz() * 0.9));
			watch(200, steps, t -> {});
			mark(state.get());
			hmiClick("ALARMS");
			context.waitTicks(60);
			hmiClick("ACK");
			context.waitTicks(40);
		});
		log("after trip: " + state.get() + " | " + read(setB));
		close();
		// 5: again, in the world: reset, put back on, tripped on camera
		server.runOnServer(srv -> panel(srv, setB).pushButton());
		write(at, "GRID.SHED", false);
		write(at, "GRID.F3_ON", false);
		context.waitTicks(100);
		write(at, "GRID.F3_ON", true);
		context.waitTicks(600);
		log("before the trip in the world: " + state.get());
		tallShot(5, "trip_world", WORLD_TIME, new Cam(6.0, 4.4, 7.6, 10.2, 2.0, 2.0), new Cam(6.6, 4.0, 7.0, 10.2, 2.2, 2.0), 200, t -> {
			if (t == 40) {
				mark("set B tripped");
				server.runOnServer(srv -> panel(srv, setB).tripOnFrequency(dev.wildcatter.energy.ElectricalStandard.active().hertz() * 0.9));
			}
			if (t % 10 == 0 && wanted(5)) {
				mark(state.get());
			}
		});
		log("gensets end: " + state.get());
	}

	// ---- pid: hold the level -----------------------------------------------------------------------------------------------

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
				high_sp : BOOL;
				more_draw : BOOL;
				sp : REAL := 50.0;
				drain : REAL := 30.0;
				valve : REAL;
				inlet : BOOL := TRUE;
				pv : REAL;
				lt_fault : BOOL;
				lt_counts : INT;
				loop : PID;
				pump : MOTOR_CTRL;
				mov : VALVE_CTRL;
			END_VAR
			IF high_sp THEN
				sp := 70.0;
			ELSE
				sp := 50.0;
			END_IF;
			IF more_draw THEN
				drain := 80.0;
			ELSE
				drain := 30.0;
			END_IF;
			lt_counts := WORD_TO_INT(lt_raw);
			lt_fault := lt_counts = 32767;
			pv := SCALE(INT_TO_REAL(lt_counts), 0.0, 32000.0, 0.0, 100.0);
			valve := SCALE(INT_TO_REAL(WORD_TO_INT(zt_raw)), 0.0, 32000.0, 0.0, 100.0);
			loop(AUTO := NOT lt_fault, PV := pv, SP := sp, MAN := 0.0, KP := 2.0, TI := T#10s);
			speed_ref := INT_TO_WORD(REAL_TO_INT(loop.OUT * 320.0));
			pump(START := NOT lt_fault, RUNNING := running, TRIPPED := tripped, AUTO := drive_auto, RUN => coil);
			fcv_signal := INT_TO_WORD(REAL_TO_INT(drain * 320.0));
			mov(OPEN := inlet, OPENED := mov_open, CLOSED := mov_closed, AUTO := mov_auto, TRAVEL_T := T#35s, CMD_OPEN => mov_open_cmd, CMD_CLOSE => mov_close_cmd);
			END_PROGRAM""";

	/** The plcAnalog scene's loop: a drive-fed pump filling a day tank through a motor-operated valve, a level transmitter, an air-operated drain valve; and an HMI. */
	private void levelBench() {
		int bay = 14;
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
		placeBig(dev.wildcatter.WildcatterBlocks.DRIVE_CABINET, drive, NORTH);
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
		BlockPos lt = A(20, 1, 4);
		put(20, 1, 4, "wildcatter:transmitter[facing=south]");
		for (int x = 14; x <= 21; x++) {
			boolean box = x == 16 || x == 18 || x == 20 || x == 21;
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
		put(16, 1, bay, "wildcatter:network_cable");
		put(17, 1, bay, "wildcatter:network_cable");
		BlockPos hmiAt = A(18, 1, bay);
		put(18, 1, bay, "wildcatter:hmi_panel[facing=south]");
		startGenset(set, NORTH, 2000);
		context.waitTicks(30);
		String wired = server.computeOnServer(srv -> {
			PlcCabinetBlockEntity box = cabinet(srv, at);
			for (var type : List.of(PlcModuleItem.Type.CPU_SMALL, PlcModuleItem.Type.DI16, PlcModuleItem.Type.DO16, PlcModuleItem.Type.AI8, PlcModuleItem.Type.AO4, PlcModuleItem.Type.COMM)) {
				box.seat(type);
			}
			box.setStation("PLC-1");
			List<String> all = new ArrayList<>();
			all.add(box.land(drive, 0, "%QX16.0"));
			all.add(box.land(drive, 1, "%IX0.0"));
			all.add(box.land(drive, 2, "%IX0.1"));
			all.add(box.land(drive, 3, "%IX0.2"));
			all.add(box.land(drive, 4, "%QW48"));
			all.add(box.land(lt, 0, "%IW34"));
			all.add(box.land(fcv, 0, "%QW50"));
			all.add(box.land(fcv, 1, "%IW36"));
			all.add(box.land(mov, 0, "%QX16.1"));
			all.add(box.land(mov, 1, "%QX16.2"));
			all.add(box.land(mov, 2, "%IX0.5"));
			all.add(box.land(mov, 3, "%IX0.6"));
			all.add(box.land(mov, 4, "%IX0.7"));
			box.setTag(lt, "LT-101");
			box.setTag(fcv, "FV-101");
			box.setTag(mov, "XV-101");
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
		log("level wired: " + (wired.isEmpty() ? "ok" : wired) + " | compile " + errors + " | " + read(at));
		layoutHmi(hmiAt, List.of(
				"VALUE|SETPOINT %|LEVEL.SP|0|100|8|24",
				"VALUE|LEVEL %|LEVEL.PV|0|100|102|24",
				"BAR|PUMP SPEED %|LEVEL.LOOP.OUT|0|100|196|24",
				"BAR|DRAIN VALVE %|LEVEL.VALVE|0|100|290|24",
				"BUTTON|SETPOINT 70|LEVEL.HIGH_SP|0|1|384|24",
				"TREND|LEVEL %|LEVEL.PV|20|90|8|66",
				"TREND|PUMP SPEED %|LEVEL.LOOP.OUT|0|100|196|66",
				"BUTTON|DRAW MORE|LEVEL.MORE_DRAW|0|1|384|66",
				"TREND|DRAIN VALVE %|LEVEL.VALVE|0|100|8|150"),
				List.of());
		java.util.function.Supplier<String> state = () -> String.format(java.util.Locale.ROOT, "sp %s pv %.1f out %.1f valve %.1f", tag(at, "LEVEL.SP"), ((Number) tag(at, "LEVEL.PV")).doubleValue(),
				((Number) tag(at, "LEVEL.LOOP.OUT")).doubleValue(), ((Number) tag(at, "LEVEL.VALVE")).doubleValue());
		context.waitTicks(2400);
		log("level holding: " + state.get() + " | drive " + read(drive) + " | tank " + read(tank));
		Cam hmiStand = new Cam(18.5, 2.4, bay + 3.5, 18.5, 1.6, bay + 0.5);
		IntConsumer every = t -> {
			if (t % 20 == 0) {
				mark(state.get());
			}
		};
		// 2: the setpoint stepped to 70 % from the HMI, the pump speeding up, the level climbing and settling (long, for a time-lapse)
		server.runCommand("time set 12300");
		stand(hmiStand);
		openHmi(hmiAt);
		wideShot(2, "setpoint", () -> {
			context.waitTicks(20);
			mark("SETPOINT 70 pressed");
			mouse(context.computeOnClient(client -> hmiScreen(client).widgetForTest(4)));
			for (int t = 0; t < 700; t++) {
				every.accept(t);
				context.waitTick();
			}
		});
		log("after the step: " + state.get());
		// 3: the drain opened to 60 % from the HMI, the level dipping, the pump catching it (long, for a time-lapse)
		wideShot(3, "disturbance", () -> {
			context.waitTicks(20);
			mark("DRAW MORE pressed");
			mouse(context.computeOnClient(client -> hmiScreen(client).widgetForTest(7)));
			for (int t = 0; t < 700; t++) {
				every.accept(t);
				context.waitTick();
			}
		});
		log("after the draw: " + state.get() + " | drive " + read(drive) + " | tank " + read(tank));
		close();
		// 4: in the world: the pump, its drive and the tank, the setpoint stepped back down to 50 and up again
		Cam wA = new Cam(21.0, 5.6, 9.0, 17.5, 1.6, 2.0), wB = new Cam(20.4, 4.8, 8.0, 17.5, 1.7, 2.0);
		tallShot(4, "pump_world", WORLD_TIME, wA, wB, 220, t -> {
			if (t == 20) {
				mark("setpoint 50");
				write(at, "LEVEL.HIGH_SP", false);
			}
			if (t % 20 == 0 && wanted(4)) {
				mark(state.get());
			}
		});
		// 1: the hook: low across the tank, the air set over it, the pump running behind
		tallShot(1, "hook", WORLD_TIME, new Cam(19.5, 2.4, 7.0, 18.5, 2.6, 2.5), new Cam(21.5, 6.2, 9.2, 17.5, 1.6, 2.0), 180, t -> {});
		log("pid end: " + state.get());
	}
}
