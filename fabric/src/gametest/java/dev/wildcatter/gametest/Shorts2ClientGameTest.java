package dev.wildcatter.gametest;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.BreakerBlock;
import dev.wildcatter.block.BreakerRelayCabinetBlockEntity;
import dev.wildcatter.block.LoadBlock;
import dev.wildcatter.block.MultiCableBlockEntity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.IntConsumer;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * {@code shorts2} (WILDCATTER_ONLY=shorts2; never part of the full suite): the Wildcatter shots of the second set of vertical shorts, in
 * the trailers' real generated world. SHORTS picks them ({@code faults}; all by default). World shots are filmed in a portrait window
 * (486 x 864 points) with a 1080 x 1920 start still in TRAILER_OUT/&lt;short&gt;/; screen shots in the landscape window (1440 x 810).
 * Films land in {@code build/scene-videos/<short>__NN_<shot>.mov}; every event is logged with its second from the film's start
 * ({@code event <film> <what> at s}). With TRAILER_FRAMES set (a locked screen records nothing) each film is saved a frame a tick into
 * TRAILER_OUT/frames/&lt;short&gt;/&lt;film&gt;/ instead, the world frozen and stepped a tick a frame, and events are timed in film time.
 * <ul>
 * <li>faults: the cabinet_reclose scene's feeder carried overhead. A diesel set, a 480 V pad-mount run backward to 12.47 kV, a yard breaker
 * worked by its relay cabinet (51 pickup 5 A, so 50 at 40 A; 79 three shots), cable up a 40 ft pole, a span of 4/0 to a second pole, cable
 * down it to a 25 kVA pole unit and three lamps. The fault is a grounding set clamped on the live riser near the second pole's top (a
 * bolted fault: it flashes). A temporary fault (the clamps off again before the first shot) is reclosed by shot 1 and the lamps come
 * back; a permanent one runs 79 through its three shots to lockout. The relay's own readout (the Power Meter's, on the action bar) is the
 * screen take.</li>
 * </ul>
 */
public class Shorts2ClientGameTest extends PlcTrailerClientGameTest {
	/** Sunset: the sun on the horizon, the lamps showing. */
	private static final long WORLD_TIME = 12700;
	private Path root;
	private String shortName = "";
	private String filmId = "";
	private long filmStart;

	@Override
	public void runTest(ClientGameTestContext context) {
		String only = System.getenv("WILDCATTER_ONLY");
		if (only == null || !java.util.Arrays.asList(only.split(",")).contains("shorts2")) {
			return;
		}
		this.context = context;
		String dir = System.getenv("TRAILER_OUT");
		root = Path.of(dir == null || dir.isBlank() ? "shorts2" : dir).toAbsolutePath();
		String pick = System.getenv("SHORTS");
		java.util.Set<String> picked = pick == null || pick.isBlank() ? java.util.Set.of("faults") : java.util.Set.of(pick.split(","));
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
				rules.set(net.minecraft.world.level.gamerules.GameRules.SEND_COMMAND_FEEDBACK, false, srv);
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
				o.guiScale().set(4);   // the relay's readout on the action bar: big, its static ends clipped
				if (!client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
			if (picked.contains("faults")) {
				shortName = "faults";
				try {
					Files.createDirectories(root.resolve(shortName));
				} catch (java.io.IOException ex) {
					throw new RuntimeException(ex);
				}
				site(1500, 500, new int[] {-10, 56, -16, 30}, 14, new int[] {2, 48, -8, 14}, 4);
				pad(2, -8, 48, 14, "minecraft:grass_block");
				try {
					faultBench();
				} catch (Throwable t) {
					log("short faults FAILED: " + t);
					t.printStackTrace();
				} finally {
					hud(false);
					close();
					landscape();
				}
			}
		}
	}

	// ---- filming (as PlcShortsClientGameTest) ---------------------------------------------------------------------------

	private void landscape() {
		context.getInput().resizeWindow(1440, 810);
		context.waitTicks(10);
	}

	private void portrait() {
		context.getInput().resizeWindow(486, 864);
		context.waitTicks(10);
	}

	private void hud(boolean shown) {
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() == shown) {
				client.gui.hud.toggle();
			}
		});
	}

	/** TRAILER_FRAMES: a frame saved each tick (for a locked screen) into TRAILER_OUT/frames/&lt;short&gt;/&lt;film&gt;/; events timed by it at 20 a second. */
	private static final boolean FRAMES = System.getenv("TRAILER_FRAMES") != null;
	private int frame = -1;
	/** In frame mode the world is frozen once the bench is built and stepped a tick at a time, waits included. */
	private boolean frozen;
	private Path frameDir;
	private int frameW, frameH;

	/** One tick of a shot: in frame mode, the frame saved. */
	private void tick() {
		if (frozen) {
			// the world frozen in frame mode: exactly one world tick a frame, so its timers run in film time
			long before = server.computeOnServer(srv -> srv.overworld().getGameTime());
			server.runCommand("tick step 1");
			for (int i = 0; i < 20 && server.computeOnServer(srv -> srv.overworld().getGameTime()) == before; i++) {
				context.waitTick();
			}
		} else {
			context.waitTick();
		}
		if (frame >= 0) {
			var options = TestScreenshotOptions.of(String.format("%04d", frame)).withDestinationDir(frameDir).disableCounterPrefix();
			context.takeScreenshot(frameW > 0 ? options.withSize(frameW, frameH) : options);
			frame++;
		}
	}

	private void ticks(int n) {
		for (int i = 0; i < n; i++) {
			tick();
		}
	}

	private boolean filming;

	private void mark(String what) {
		if (!filming) {
			return;   // a film not wanted this run: its beats still run, unlogged
		}
		double at = frame >= 0 ? frame / 20.0 : (System.nanoTime() - filmStart) / 1e9;
		log(String.format(java.util.Locale.ROOT, "event %s %s at %.2f s", filmId, what, at));
	}

	private Filming startFilm(String id, int w, int h) {
		filmId = id;
		filming = true;
		if (FRAMES) {
			frameDir = root.resolve("frames").resolve(shortName).resolve(id);
			try {
				Files.createDirectories(frameDir);
			} catch (java.io.IOException ex) {
				throw new RuntimeException(ex);
			}
			frameW = w;
			frameH = h;
			frame = 0;
			return null;
		}
		Filming film = context.computeOnClient(client -> Filming.start(client, shortName + "__" + id));
		filmStart = System.nanoTime();
		return film;
	}

	private void tallShot(int n, String name, long time, Cam a, Cam b, int ticks, IntConsumer beat) {
		String id = String.format("%02d_%s", n, name);
		if (!wanted(n)) {
			for (int t = 0; t <= ticks; t++) {
				beat.accept(t);
				tick();
			}
			return;
		}
		close();
		hud(false);
		portrait();
		server.runCommand("time set " + time);
		server.runCommand("weather clear");
		camera(a, true);
		context.waitTicks(FRAMES ? 20 : 80);   // in frame mode the world keeps ticking under these waits: keep them short
		camera(a, true);
		context.waitTicks(FRAMES ? 5 : 20);
		context.takeScreenshot(TestScreenshotOptions.of(id + "_a").withSize(1080, 1920).withDestinationDir(root.resolve(shortName)).disableCounterPrefix());
		camera(a, true);
		context.waitTicks(10);
		Filming film = startFilm(id, 1080, 1920);
		try {
			ticks(10);
			for (int t = 0; t <= ticks; t++) {
				camera(a.lerp(b, (double) t / ticks), false);
				beat.accept(t);
				tick();
			}
			ticks(10);
		} finally {
			stopFilm(film);
		}
		log("filmed " + shortName + " " + id);
	}

	private void stopFilm(Filming film) {
		filming = false;
		if (film != null) {
			film.stop();
		}
		if (frame >= 0) {
			log("frames " + shortName + " " + filmId + ": " + frame);
		}
		frame = -1;
	}

	private void wideShot(int n, String name, Runnable work) {
		String id = String.format("%02d_%s", n, name);
		if (!wanted(n)) {
			work.run();
			return;
		}
		log("window for " + id + ": " + context.computeOnClient(client -> client.getWindow().getWidth() + "x" + client.getWindow().getHeight() + " gui " + client.getWindow().getGuiScaledWidth() + "x"
				+ client.getWindow().getGuiScaledHeight() + " scale " + client.getWindow().getGuiScale()));
		Filming film = startFilm(id, 0, 0);
		try {
			ticks(15);
			work.run();
			ticks(10);
		} finally {
			stopFilm(film);
		}
		log("filmed " + shortName + " " + id);
	}

	// ---- faults: a fault, and the protection clears it ------------------------------------------------------------------

	private BlockPos cabinetPos, breakerPos, faultPos;
	private BlockPos[] lamps;

	private BreakerRelayCabinetBlockEntity relay(net.minecraft.server.level.ServerLevel level) {
		return (BreakerRelayCabinetBlockEntity) level.getBlockEntity(cabinetPos);
	}

	private void grounds(boolean on) {
		on(level -> ((MultiCableBlockEntity) level.getBlockEntity(faultPos)).setGrounded(on));
	}

	/** The breaker's position and how many lamps are lit: what the world shows. */
	private String worldState() {
		return server.computeOnServer(srv -> {
			var level = srv.overworld();
			int lit = 0;
			for (BlockPos lamp : lamps) {
				var s = level.getBlockState(lamp);
				if (s.hasProperty(LoadBlock.LIT) && s.getValue(LoadBlock.LIT)) {
					lit++;
				}
			}
			var cab = relay(level);
			return "breaker " + level.getBlockState(breakerPos).getValue(BreakerBlock.POSITION).getSerializedName() + ", lamps lit " + lit + "/" + lamps.length
					+ ", shots used " + cab.shotsUsed() + (cab.lockedOut() ? ", LOCKED OUT" : "");
		});
	}

	/** The relay's readout without its settings tail, as the action bar shows it. */
	private String relayState() {
		String r = read(cabinetPos);
		int cut = r.indexOf("51 pickup");
		cut = cut > 0 ? r.lastIndexOf(';', cut) : -1;
		return cut > 0 ? r.substring(0, cut) : r;
	}

	/** The Power Meter's readout of the cabinet on the action bar (what a right-click with the meter shows), kept fresh. */
	private void meter() {
		server.runOnServer(srv -> srv.getPlayerList().getPlayers().get(0).sendOverlayMessage(relay(srv.overworld()).inspect()));
	}

	/** A beat that marks each change of {@code state}. */
	private IntConsumer watching(java.util.function.Supplier<String> state, IntConsumer then) {
		String[] last = {null};
		return t -> {
			then.accept(t);
			String now = state.get();
			if (!now.equals(last[0])) {
				if (last[0] != null) {
					mark(now);
				}
				last[0] = now;
			}
		};
	}

	/** Waits until the relay's readout shows {@code text}, at most {@code ticks}. */
	private void waitFor(String text, int ticks) {
		for (int t = 0; t < ticks; t++) {
			if (read(cabinetPos).contains(text)) {
				return;
			}
			tick();
		}
		log("waited " + ticks + " ticks for '" + text + "': " + relayState());
	}

	private void faultBench() {
		// the source: a diesel set and a 480 V pad-mount run backward, 12.47 kV out at x 13
		BlockPos set = A(10, 1, 0);
		genset(set, NORTH, false);
		BlockPos out = inlineTransformer(WildcatterBlocks.PADMOUNT_480, set, Direction.EAST, true);
		for (int x = out.getX() - ox; x <= 14; x++) {
			put(x, 1, 0, "wildcatter:cable_dist");
		}
		// the yard breaker (x 15-16, z -1..1, terminals on z 0) and its relay cabinet north of it
		breakerPos = A(15, 1, -1);
		cabinetPos = A(15, 1, -2);
		placeBig(WildcatterBlocks.DIST_BREAKER_3P, breakerPos, NORTH);
		placeBig(WildcatterBlocks.BREAKER_RELAY_CABINET, cabinetPos, NORTH);
		// cable east to pole 1, up it as a riser to the pole-top pin; a span of 4/0 to pole 2 (B and C strung dead, for the picture)
		for (int x = 17; x <= 21; x++) {
			put(x, 1, 0, "wildcatter:cable_dist");
		}
		int p1 = 23, p2 = 39;
		// the risers are line wire up the poles; on pole 2 a piece of cable near the top, where the grounding clamps go
		for (int k = 1; k <= 11; k++) {
			put(p1 - 1, k, 0, "wildcatter:dist_wire_hot");
			put(p2 + 1, k, 0, k == 10 ? "wildcatter:cable_dist" : "wildcatter:dist_wire_hot");
		}
		for (int px : new int[] {p1, p2}) {
			placeBig(WildcatterBlocks.WOOD_POLE_40, A(px, 1, 0), NORTH);
			placeBig(WildcatterBlocks.CROSSARM_DIST, A(px, 10, -1), Direction.EAST);
			insulator(WildcatterBlocks.INSULATOR_DIST, A(px, 11, 0), "a");
			insulator(WildcatterBlocks.INSULATOR_DIST, A(px, 11, -1), "b");
			insulator(WildcatterBlocks.INSULATOR_DIST, A(px, 11, 1), "c");
		}
		context.waitTicks(20);
		for (int dz = -1; dz <= 1; dz++) {
			span(A(p1, 11, dz), A(p2, 11, dz), dev.wildcatter.energy.Voltage.DISTRIBUTION);
		}
		// down pole 2 to a 25 kVA pole unit at its foot (front west to the cable), and its lamps: leg A on its back, leg B on its sides
		put(p2 + 2, 1, 0, "wildcatter:pole_transformer[facing=west]");
		lamps = new BlockPos[] {A(p2 + 3, 1, 0), A(p2 + 2, 1, 1), A(p2 + 2, 1, -1)};
		for (BlockPos lamp : lamps) {
			server.runCommand("setblock " + lamp.getX() + " " + lamp.getY() + " " + lamp.getZ() + " wildcatter:electric_lamp");
		}
		faultPos = A(p2 + 1, 10, 0);
		startGenset(set, NORTH, 3000);
		context.waitTicks(40);
		on(level -> relay(level).setPickup(5));   // 51 at 5 A, 50 at 40 A: a small feeder's settings
		context.waitTicks(100);
		log("faults ready: " + read(cabinetPos) + " | breaker " + read(breakerPos) + " | unit " + read(A(p2 + 2, 1, 0)) + " | riser " + read(faultPos)
				+ " | " + worldState() + " | set " + read(set));

		if (FRAMES) {
			server.runCommand("tick freeze");
			frozen = true;
		}
		// 1: the hook, a temporary fault: the clamps flash on the riser, the lamps drop out, the clamps come off, shot 1 (2 s) brings them back
		Cam hookA = new Cam(p2 + 7.0, 2.0, 7.0, p2 + 1, 6.4, 0), hookB = new Cam(p2 + 5.6, 2.8, 5.6, p2 + 1, 6.8, 0);
		tallShot(1, "hook", WORLD_TIME, hookA, hookB, 170, watching(this::worldState, t -> {
			if (t == 20) {
				mark("fault: grounds on the live riser (flash)");
				grounds(true);
			}
			if (t == 34) {
				mark("fault gone: grounds off (temporary fault)");
				grounds(false);
			}
		}));
		log("after the temporary fault: " + relayState() + " | " + worldState());
		ticks(1260);   // held closed 60 s: the 79 sequence resets
		log("79 reset: " + relayState() + " | " + worldState());

		// 2: the same temporary fault on the relay's readout: 50 picks up, 79 times shot 1, recloses
		landscape();
		hud(true);
		server.runCommand("time set " + WORLD_TIME);
		stand(new Cam(15.5, 2.3, -4.4, 15.5, 2.0, -1.5));
		ticks(10);
		IntConsumer fresh = t -> {
			if (t % 4 == 0) {
				meter();
			}
		};
		wideShot(2, "relay_trip", () -> {
			IntConsumer beat = watching(this::relayState, fresh);
			for (int t = 0; t < 150; t++) {
				if (t == 20) {
					mark("fault: grounds on (temporary)");
					grounds(true);
				}
				if (t == 34) {
					mark("grounds off");
					grounds(false);
				}
				beat.accept(t);
				tick();
			}
		});
		hud(false);
		log("after the temporary fault 2: " + relayState() + " | " + worldState());
		ticks(1260);
		log("79 reset: " + relayState() + " | " + worldState());

		// 3: a permanent fault in the world: the flash, the lamps out, shot 1 closes onto it and it trips again; the lamps stay dark
		tallShot(3, "fault_world", WORLD_TIME, new Cam(p2 + 8.0, 3.4, -4.0, p2 + 1, 5.0, 0), new Cam(p2 + 6.8, 3.8, -3.2, p2 + 1, 5.4, 0), 160,
				watching(this::worldState, t -> {
					if (t == 20) {
						mark("fault: grounds on the live riser (permanent)");
						grounds(true);
					}
				}));
		log("permanent fault, world: " + relayState() + " | " + worldState());
		// 4: the feeder from the yard: breaker and cabinet in front, the line to the dark lamps; shot 2 closes onto the fault
		waitFor("recloses in 5 s (shot 2", 400);
		tallShot(4, "shot2_world", WORLD_TIME, new Cam(11.0, 2.4, -5.0, 22.0, 4.0, 1.0), new Cam(11.5, 3.2, -4.0, 22.0, 4.6, 1.0), 160, watching(this::worldState, t -> {}));
		// 5: lockout: shot 3 closes onto it, trips, and 79 locks out; the clamps come off and the hand reset at the cabinet (a right-click) brings the lamps back
		waitFor("recloses in 5 s (shot 3", 700);
		tallShot(5, "lockout_world", WORLD_TIME, new Cam(p2 + 7.5, 4.2, 6.5, p2 + 1.5, 3.0, 0), new Cam(p2 + 6.0, 3.6, 5.6, p2 + 1.5, 3.4, 0), 220,
				watching(this::worldState, t -> {
					if (t == 120) {
						mark("grounds off");
						grounds(false);
					}
					if (t == 150) {
						mark("hand reset at the cabinet");
						on(level -> relay(level).reset());
					}
				}));
		ticks(100);
		log("reset by hand: " + read(cabinetPos) + " | " + worldState());
		landscape();
		hud(true);
		stand(new Cam(15.5, 2.3, -4.4, 15.5, 2.0, -1.5));
		ticks(10);
		// 6: the permanent fault on the readout: trip, shot 1 onto the fault, 79 timing shot 2
		wideShot(6, "relay_shots", () -> {
			IntConsumer beat = watching(this::relayState, fresh);
			for (int t = 0; t < 160; t++) {
				if (t == 20) {
					mark("fault: grounds on (permanent)");
					grounds(true);
				}
				beat.accept(t);
				tick();
			}
		});
		// 7: shot 3 onto the fault, and LOCKED OUT
		waitFor("recloses in 3 s (shot 3", 900);
		wideShot(7, "relay_lockout", () -> {
			IntConsumer beat = watching(this::relayState, fresh);
			for (int t = 0; t < 170; t++) {
				beat.accept(t);
				tick();
			}
		});
		hud(false);
		log("faults end: " + read(cabinetPos) + " | " + worldState());
		grounds(false);
		if (frozen) {
			server.runCommand("tick unfreeze");
			frozen = false;
		}
	}
}
