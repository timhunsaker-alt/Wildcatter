package dev.wildcatter.gametest;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.block.BessCabinetBlockEntity;
import dev.wildcatter.block.BessContainerBlockEntity;
import dev.wildcatter.block.BessInverterBlockEntity.Preset;
import dev.wildcatter.block.BessPcsBlockEntity;
import dev.wildcatter.block.BessPlantControllerBlockEntity;
import dev.wildcatter.block.BrineWellheadBlockEntity;
import dev.wildcatter.block.CellLineBlockEntity;
import dev.wildcatter.block.ChokePanelBlockEntity;
import dev.wildcatter.block.FraschWaterHeaterBlockEntity;
import dev.wildcatter.block.FraschWellheadBlockEntity;
import dev.wildcatter.block.GasTurbineBlockEntity;
import dev.wildcatter.block.InjectionPumpBlockEntity;
import dev.wildcatter.block.LoadBankBlockEntity;
import dev.wildcatter.block.RigSubstructureBlockEntity;
import dev.wildcatter.block.RigSubstructureBlockEntity.Solid;
import dev.wildcatter.block.StorageCompressorBlockEntity;
import dev.wildcatter.block.StorageWellheadBlockEntity;
import dev.wildcatter.block.SulfurVatBlockEntity;
import dev.wildcatter.energy.ElectricalStandard;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import dev.wildcatter.well.Reservoir;
import dev.wildcatter.well.ReservoirData;
import dev.wildcatter.well.WellControl;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The 0.10 trailers' Wildcatter films (never part of the full suite): WILDCATTER_ONLY names the scenes, any of {@code chokeKill},
 * {@code lostCirculation}, {@code saltCavern}, {@code frasch} and {@code bess}. Each is built as its NeoForge scene builds it, in the
 * trailers' real generated world, and filmed in real time (no frame mode: a locked screen is reported and nothing is filmed). World shots
 * are filmed in a portrait window (486 x 864 points, 972 x 1728 pixels on the Mac's screen) with a readout card drawn large across the
 * top of the frame (the values are the blocks' own, read each tick); the remote choke panel's own screen is filmed in the landscape
 * window (1440 x 810) at a GUI scale that fills it. Films land in {@code build/scene-videos/<scene>__NN_<shot>.mov}; every beat is logged
 * as {@code event <film> <what> at <s>} from the film's start. TRAILER_SHOTS=1,3 films only those shots of each scene (the others still
 * run, unfilmed).
 */
public class Films010ClientGameTest extends PlcTrailerClientGameTest {
	static final List<String> SCENES = List.of("chokeKill", "lostCirculation", "saltCavern", "frasch", "bess", "fuelPool", "scram", "blowout", "arcflash", "turbine", "cascade");
	/** Late afternoon: the sun up, long light. */
	private static final long WORLD_TIME = 10500;

	private String scene = "";
	private String filmId = "";
	private long filmStart;
	private boolean filming;
	/** Ticks the camera holds still at each end of a film's move (the shorts' 3 s stills); 0 moves it over the whole film. */
	private int still;

	@Override
	public void runTest(ClientGameTestContext context) {
		String only = System.getenv("WILDCATTER_ONLY");
		if (only == null) {
			return;
		}
		List<String> picked = new ArrayList<>();
		for (String s : only.split(",")) {
			if (SCENES.contains(s)) {
				picked.add(s);
			}
		}
		if (picked.isEmpty()) {
			return;
		}
		if (System.getenv("TRAILER_FRAMES") != null) {
			log("these films are real time only: TRAILER_FRAMES is not supported; nothing filmed");
			return;
		}
		this.context = context;
		String films = System.getenv("TRAILER_SHOTS");
		shots = films == null || films.isBlank() ? null : java.util.Set.of(films.split(","));
		tall();
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
			server.runCommand("time set " + WORLD_TIME);
			context.runOnClient(client -> {
				var o = client.options;
				o.applyGraphicsPreset(net.minecraft.client.GraphicsPreset.FANCY);
				o.renderDistance().set(12);
				o.particles().set(net.minecraft.server.level.ParticleStatus.ALL);
				o.bobView().set(false);
				o.fov().set(70);
				o.cloudStatus().set(net.minecraft.client.CloudStatus.FANCY);
				if (!client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
			WildcatterClientGameTest.rules(server, true, "saltDomeGrid192", "mudGel", "wellControl", "chokeKill", "cavernStability");
			for (String name : picked) {
				scene = name;
				try {
					switch (name) {
						case "lostCirculation" -> lostCirculation();
						case "chokeKill" -> chokeKill();
						case "saltCavern" -> saltCavern();
						case "frasch" -> frasch();
						case "bess" -> bess();
						case "fuelPool" -> fuelPool();
						case "scram" -> scram();
						case "blowout" -> blowout();
						case "arcflash" -> arcflash();
						case "turbine" -> turbine();
						case "cascade" -> cascade();
						default -> {
						}
					}
				} catch (Throwable t) {
					log(name + " FAILED: " + t);
					t.printStackTrace();
				} finally {
					close();
					tall();
				}
			}
			for (ReservoirData.TestRegion r : regions) {
				ReservoirData.TEST_REGIONS.remove(r);
			}
		}
	}

	// ---- filming --------------------------------------------------------------------------------------------------------------------

	private void tall() {
		context.getInput().resizeWindow(486, 864);
		context.waitTicks(10);
	}

	private void wide() {
		context.getInput().resizeWindow(1440, 810);
		context.waitTicks(10);
	}

	/** The GUI scale that makes the window {@code units} GUI units wide (text as big as that allows). */
	private void guiWidth(int units) {
		context.runOnClient(client -> {
			int scale = Math.max(1, Math.round(client.getWindow().getWidth() / (float) units));
			client.options.guiScale().set(scale);
			client.resizeGui();
		});
		context.waitTicks(2);
	}

	/** No cursor in the films: the screens show one, so it is hidden on the window each time. */
	private void hideCursor() {
		context.runOnClient(client -> org.lwjgl.sdl.SDLMouse.SDL_HideCursor());
	}

	private void mark(String what) {
		if (filming) {
			log(String.format(Locale.ROOT, "event %s %s at %.2f s", filmId, what, (System.nanoTime() - filmStart) / 1e9));
		}
	}

	/**
	 * A film: from camera a to b (null: the camera stays where it is) over {@code ticks}, {@code beat} run each tick with the tick number;
	 * the beat returns false to end the film early. Shots not asked for (TRAILER_SHOTS) still run their beats, unfilmed.
	 */
	private void film(int n, String name, Cam a, Cam b, int ticks, Beat beat) {
		String id = String.format("%02d_%s", n, name);
		boolean want = wanted(n);
		if (a != null) {
			camera(a, true);
			context.waitTicks(want ? 40 : 2);
			camera(a, true);
			context.waitTicks(want ? 10 : 1);
		}
		hideCursor();
		Filming film = null;
		if (want) {
			filmId = scene + "__" + id;
			log("window for " + filmId + ": " + context.computeOnClient(client -> client.getWindow().getWidth() + "x" + client.getWindow().getHeight() + " gui scale "
					+ client.getWindow().getGuiScale()));
			film = context.computeOnClient(client -> Filming.start(client, filmId));
			filmStart = System.nanoTime();
			filming = true;
		}
		try {
			context.waitTicks(want ? 10 : 0);
			for (int t = 0; t <= ticks; t++) {
				if (a != null && b != null) {
					double move = Math.max(1, ticks - 2 * still);
					camera(a.lerp(b, Math.max(0.0, Math.min(1.0, (t - still) / move))), false);
				}
				int tick = t;
				if (!server.computeOnServer(srv -> beat.at(tick))) {   // the beats read and work the blocks: on the server thread
					break;
				}
				if (t % 10 == 0) {
					hideCursor();
				}
				context.waitTick();
			}
			context.waitTicks(want ? 10 : 0);
		} finally {
			filming = false;
			if (film != null) {
				film.stop();
				log("filmed " + filmId);
			}
		}
	}

	interface Beat {
		boolean at(int t);
	}

	static Beat until(IntConsumer each) {
		return t -> {
			each.accept(t);
			return true;
		};
	}

	// ---- the readout card -----------------------------------------------------------------------------------------------------------

	/**
	 * A transparent screen that draws the readout card across the top of the frame, its text as large as the frame's width allows
	 * (the top keeps clear of a phone app's buttons at the bottom and right). Lines starting {@code ^} are titles (amber), {@code !}
	 * alarms (red), {@code +} good news (green).
	 */
	static final class FilmCard extends Screen {
		static volatile String[] lines = new String[0];
		/** In a landscape film cut to a vertical short: the card kept inside the frame's middle 9:16. */
		static volatile boolean narrow;

		FilmCard() {
			super(Component.literal("readout"));
		}

		@Override
		public boolean isPauseScreen() {
			return false;
		}

		@Override
		public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
		}

		@Override
		public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
			String[] now = lines;
			if (now.length == 0) {
				return;
			}
			int widest = 1;
			for (String l : now) {
				widest = Math.max(widest, font.width(text(l)));
			}
			float room = narrow ? Math.min(width, height * 9f / 16f) : width;
			float s = Math.min(narrow ? 4f : 2.4f, (room * 0.90f) / widest);
			int line = 11;
			int boxW = (int) (widest * s) + 16;
			int boxH = (int) (now.length * line * s) + 10;
			int x0 = (width - boxW) / 2;
			int y0 = (int) (height * 0.07f);
			graphics.fill(x0, y0, x0 + boxW, y0 + boxH, 0xC8101418);
			graphics.pose().pushMatrix();
			graphics.pose().translate(x0 + 8, y0 + 6);
			graphics.pose().scale(s, s);
			for (int i = 0; i < now.length; i++) {
				String l = now[i];
				int colour = l.startsWith("^") ? 0xFFF0C040 : l.startsWith("!") ? 0xFFFF5A50 : l.startsWith("+") ? 0xFF70E070 : 0xFFF2F2F2;
				graphics.text(font, text(l), 0, i * line, colour, true);
			}
			graphics.pose().popMatrix();
		}

		private static String text(String l) {
			return l.startsWith("^") || l.startsWith("!") || l.startsWith("+") ? l.substring(1) : l;
		}
	}

	private void card(String... lines) {
		FilmCard.lines = lines;
	}

	private void showCard() {
		context.runOnClient(client -> {
			if (!(client.gui.screen() instanceof FilmCard)) {
				client.gui.setScreen(new FilmCard());
			}
		});
		context.waitTicks(2);
	}

	// ---- helpers ----------------------------------------------------------------------------------------------------------------------

	private net.minecraft.server.MinecraftServer mc;

	/** Runs {@code work} on the server thread: at once from a beat (already there), else through the test server. */
	private void onS(java.util.function.Consumer<net.minecraft.server.MinecraftServer> work) {
		if (mc == null) {
			mc = server.computeOnServer(srv -> srv);
		}
		if (mc.isSameThread()) {
			work.accept(mc);
		} else {
			server.runOnServer(work::accept);
		}
	}

	private <T> T getS(java.util.function.Function<net.minecraft.server.MinecraftServer, T> work) {
		if (mc == null) {
			mc = server.computeOnServer(srv -> srv);
		}
		return mc.isSameThread() ? work.apply(mc) : server.computeOnServer(work::apply);
	}

	private final List<ReservoirData.TestRegion> regions = new ArrayList<>();

	private <T> T at(BlockPos pos, Class<T> kind) {
		return getS(srv -> kind.cast(BigParts.entity(srv.overworld(), pos)));
	}

	@Override
	String read(BlockPos at) {
		return getS(srv -> BigParts.entity(srv.overworld(), at) instanceof dev.wildcatter.energy.Inspectable i ? i.inspect().getString() : "(no readout)");
	}

	private void place(Block block, int x, int k, int z, Direction facing) {
		placeBig(block, A(x, k, z), facing);
	}

	private void block(int x, int k, int z, net.minecraft.world.level.block.state.BlockState state) {
		on(level -> level.setBlock(A(x, k, z), state, 3));
	}

	private void run(String id, int k, int[]... corners) {
		for (int i = 0; i + 1 < corners.length; i++) {
			int[] a = corners[i], b = corners[i + 1];
			int dx = Integer.signum(b[0] - a[0]), dz = Integer.signum(b[1] - a[1]);
			for (int x = a[0], z = a[1];; x += dx, z += dz) {
				put(x, k, z, "wildcatter:" + id);
				if (x == b[0] && z == b[1]) {
					break;
				}
			}
		}
	}

	private TestGasSource.SourceEntity source(int x, int k, int z, Product product, double rate) {
		return server.computeOnServer(srv -> {
			srv.overworld().setBlock(A(x, k, z), TestGasSource.BLOCK.defaultBlockState(), 3);
			TestGasSource.SourceEntity s = (TestGasSource.SourceEntity) srv.overworld().getBlockEntity(A(x, k, z));
			s.product = product;
			s.rate = rate;
			return s;
		});
	}

	private boolean waitUntil(java.util.function.BooleanSupplier done, int max) {
		for (int t = 0; t < max; t += 10) {
			if (done.getAsBoolean()) {
				return true;
			}
			context.waitTicks(10);
		}
		return done.getAsBoolean();
	}

	private static String f0(double v) {
		return String.format(Locale.ROOT, "%,.0f", v);
	}

	private static String f1(double v) {
		return String.format(Locale.ROOT, "%,.1f", v);
	}

	private static String f2(double v) {
		return String.format(Locale.ROOT, "%,.2f", v);
	}

	// ---- the rig (lostCirculation and chokeKill share it) -------------------------------------------------------------------------------

	private RigSubstructureBlockEntity rig;
	private BlockPos hole;
	private Reservoir field;
	private int rigX, rigZ;

	private BlockPos rigPos() {
		return A(26, 1, -4);
	}

	private ChokePanelBlockEntity panel() {
		return at(A(33, 7, -3), ChokePanelBlockEntity.class);
	}

	/** Weights the mud to at least {@code ppg} by hook: fresh water, 25 lb/bbl of bentonite, then barite (OilfieldFollowOnScenes.weigh). */
	static void weigh(RigSubstructureBlockEntity rig, double ppg) {
		rig.testWater(0);
		rig.testWater(40_000);
		rig.testMix(Solid.BENTONITE, 71.3 * 40);
		while (rig.mudPpg() < ppg) {
			rig.testMix(Solid.BARITE, 200);
		}
	}

	/**
	 * The land rig as OilfieldFollowOnScenes lays it out (OilfieldScenes.landRig at y 1 over the pad, the BOP, accumulator, choke manifold,
	 * mud-gas separator, flare and remote choke panel), over a forced deep oil field; cased 8 blocks above the field, the LOT done.
	 */
	private void rigSite() {
		if (rig != null) {
			ox = rigX;
			oz = rigZ;
			return;
		}
		site(4000, 600, new int[] {-26, 60, -26, 28}, 12, new int[] {-16, 52, -16, 18}, 4);
		rigX = ox;
		rigZ = oz;
		pad(-16, -16, 52, 18, "minecraft:grass_block");
		hole = A(30, 1, 0);
		long seed = -1;
		for (long s = 1; s < 3_000_000 && seed < 0; s++) {
			Reservoir f = Reservoir.at(s, hole.getX(), hole.getZ());
			if (f != null && f.deep() && f.kind() == Reservoir.Kind.OIL) {
				seed = s;
			}
		}
		log("rig: deep field seed " + seed);
		ReservoirData.TestRegion region = new ReservoirData.TestRegion(ox - 30, oz - 30, ox + 70, oz + 30, seed);
		ReservoirData.TEST_REGIONS.add(region);
		regions.add(region);
		field = Reservoir.at(seed, hole.getX(), hole.getZ());
		place(WildcatterBlocks.RIG_SUBSTRUCTURE, 26, 1, -4, NORTH);
		place(WildcatterBlocks.RIG_MAST, 29, 7, -1, NORTH);
		place(WildcatterBlocks.RIG_DRAWWORKS, 27, 7, 2, NORTH);
		place(WildcatterBlocks.MUD_TANKS, 36, 1, -5, NORTH);
		place(WildcatterBlocks.MUD_PUMP, 40, 1, -4, NORTH);
		place(WildcatterBlocks.MUD_PUMP, 40, 1, 0, NORTH);
		place(WildcatterBlocks.RIG_DRIVE_HOUSE, 24, 1, 7, NORTH);
		place(WildcatterBlocks.BOP_ACCUMULATOR, 18, 1, -12, NORTH);
		place(WildcatterBlocks.BOP_STACK, 30, 2, 0, NORTH);
		BlockPos set = A(10, 1, 7);
		genset(set, NORTH, false);
		place(WildcatterBlocks.UNIT_SUBSTATION, 14, 1, 7, Direction.SOUTH);
		run("cable_mv", 1, new int[] {15, 7}, new int[] {23, 7});
		// the choke system (OilfieldFollowOnScenes.chokeSystem, all)
		run("gas_line", 2, new int[] {30, -5}, new int[] {30, -1});
		place(WildcatterBlocks.CHOKE_MANIFOLD, 30, 1, -9, NORTH);
		place(WildcatterBlocks.MUD_GAS_SEPARATOR, 32, 1, -8, NORTH);
		put(32, 7, -8, "wildcatter:gas_line");
		put(33, 7, -8, "wildcatter:gas_line");
		place(WildcatterBlocks.FLARE_STACK, 34, 1, -8, NORTH);
		place(WildcatterBlocks.REMOTE_CHOKE_PANEL, 33, 7, -3, NORTH);
		startGenset(set, NORTH, 3000);
		rig = at(rigPos(), RigSubstructureBlockEntity.class);
		int top = field.topY();
		on(level -> {
			log("rig formed " + rig.formed() + ", top drive " + rig.installTopDrive());
			weigh(rig, 12.8);
			rig.testDepth(hole.getY() - top - 8);
			rig.rackUp(new ItemStack(WildcatterItems.CASING_JOINT, rig.jointsNeeded()));
			rig.rackUp(new ItemStack(WildcatterItems.OILWELL_CEMENT, Tuning.CASING_CEMENT_BAGS));
			rig.consoleCasing(level);
			rig.testCasingDone();
		});
		waitUntil(() -> server.computeOnServer(srv -> rig.phase() == RigSubstructureBlockEntity.Phase.IDLE && rig.shoeDepth() > 0), 1200);
		on(level -> {
			rig.testDepth(rig.shoeDepth() + Tuning.LOT_NEW_HOLE_M);
			log("LOT: " + rig.consoleCasing(level).getString());
			panel().testHydraulics();
		});
		context.waitTicks(40);
		log("rig ready: " + read(rigPos()) + " | panel " + read(A(33, 7, -3)));
	}

	// ---- lostCirculation (23a) ----------------------------------------------------------------------------------------------------------
	/**
	 * Heavy mud at the shoe: weighted to 15.3 ppg and circulating off bottom, the pumps' ECD puts 15.6 on a shoe that holds 15.4, and it
	 * loses mud (LOST CIRCULATION: PARTIAL); an LCM bag through the hopper cures it within a minute, the shoe then holding 15.9.
	 */
	void lostCirculation() {
		rigSite();
		on(level -> {
			if (rig.phase() != RigSubstructureBlockEntity.Phase.IDLE) {
				rig.toggle();
			}
			weigh(rig, 15.3);
			rig.testHoldBit(true);
		});
		double[] lost0 = {0};
		Cam a = new Cam(47.5, 7.0, 15.0, 37.0, 3.0, 0.0), b = new Cam(45.0, 5.5, 11.5, 36.5, 3.0, 0.0);
		showCard();
		card("^MUD 15.3 ppg", "Circulating off bottom", "");
		boolean[] on = {false};
		int[] lossAt = {-1};
		film(1, "losses", a, b, 420, t -> {
			if (t == 20) {
				onS(srv -> rig.toggle());
				on[0] = true;
				lost0[0] = rig.lostMud();
				mark("pumps on");
			}
			if (on[0]) {
				boolean losing = rig.losses() != WellControl.Losses.NONE;
				if (losing && lossAt[0] < 0) {
					lossAt[0] = t;
					mark("LOST CIRCULATION: PARTIAL");
				}
				card("^MUD " + f1(rig.mudPpg()) + " ppg, PUMPING", "At the shoe: " + f1(rig.mudPpg() + Tuning.ECD_DRILLING_PPG) + " ppg", "Shoe holds:  " + f1(rig.weakFg()) + " ppg",
						losing ? "!LOST CIRCULATION: PARTIAL" : "+Returns full", "Mud lost: " + f1(rig.lostMud() - lost0[0]) + " m3");
			}
			return lossAt[0] < 0 || t < lossAt[0] + 240;
		});
		int[] cured = {-1};
		Cam c = new Cam(44.0, 5.0, 9.0, 37.0, 3.0, 0.0), d = new Cam(42.5, 4.5, 7.0, 37.0, 3.2, 0.0);
		film(2, "lcm", c, d, 1500, t -> {
			if (t == 20) {
				onS(srv -> rig.testMix(Solid.LCM, Tuning.BULK_BAG_KG));
				mark("LCM bag in");
			}
			boolean losing = rig.losses() != WellControl.Losses.NONE;
			if (!losing && cured[0] < 0 && t > 20) {
				cured[0] = t;
				mark("losses stopped");
			}
			String cure = t < 20 ? "!LOST CIRCULATION: PARTIAL" : losing ? "^LCM BAG IN: curing, " + Math.max(0, (Tuning.LCM_CURE_TICKS - (t - 20) + 19) / 20) + " s" : "+LOSSES STOPPED";
			card(cure, "At the shoe: " + f1(rig.mudPpg() + Tuning.ECD_DRILLING_PPG) + " ppg", (losing ? "Shoe holds:  " : "+Shoe now holds: ") + f1(rig.weakFg()) + " ppg",
					"Mud lost: " + f1(rig.lostMud() - lost0[0]) + " m3");
			return cured[0] < 0 || t < cured[0] + 120;
		});
		on(level -> {
			if (rig.phase() != RigSubstructureBlockEntity.Phase.IDLE) {
				rig.toggle();
			}
			rig.testHoldBit(false);
		});
		close();
		log("lostCirculation ok: " + read(rigPos()));
	}

	// ---- chokeKill (23b) -----------------------------------------------------------------------------------------------------------------
	/**
	 * A kick at 12.8 ppg drilling into the deep field: shut in, the kill sheet; the tanks weighted to the kill weight (wait and weight), the
	 * pump brought up on the remote choke panel and the choke held on the schedule (a scripted operator on the panel's PLC points, as
	 * OilfieldFollowOnScenes.operate; the model run 4 steps a tick); kill mud round, pump off and choke open, the flow check, WELL DEAD.
	 */
	void chokeKill() {
		rigSite();
		int top = field.topY();
		on(level -> {
			if (rig.phase() != RigSubstructureBlockEntity.Phase.IDLE) {
				rig.toggle();
			}
			rig.testHoldBit(false);
			weigh(rig, 12.8);
			rig.testDepth(hole.getY() - top - 3);   // three blocks to drill: a few seconds of drilling before the kick
		});
		context.waitTicks(40);
		// 1: drilling into the field: the kick, shut in
		showCard();
		int[] kickAt = {-1};
		int[] shutAt = {-1};
		Cam a = new Cam(20.5, 4.5, 4.5, 30.0, 3.5, 0.0), b = new Cam(23.0, 3.8, 2.5, 30.0, 3.5, 0.0);
		film(1, "kick", a, b, 1400, t -> {
			if (t == 20) {
				onS(srv -> rig.toggle());
				mark("drilling");
			}
			RigSubstructureBlockEntity.Phase phase = rig.phase();
			if (phase == RigSubstructureBlockEntity.Phase.KICK && kickAt[0] < 0) {
				kickAt[0] = t;
				mark("KICK");
			}
			if (kickAt[0] >= 0 && t == kickAt[0] + 100) {
				String shut = getS(srv -> rig.shutIn(srv.overworld()).getString());
				shutAt[0] = t;
				mark("shut in: " + shut);
			}
			if (shutAt[0] >= 0 && rig.killSheet() != null) {
				var sheet = rig.killSheet();
				card("^SHUT IN: BOP CLOSED", "Drill pipe (SIDPP): " + f0(sheet.sidpp()) + " psi", "Casing (SICP):     " + f0(sheet.sicp()) + " psi", "Pit gain: +" + f2(rig.shownGain()) + " m3");
			} else if (kickAt[0] >= 0) {
				card("!KICK!", "Pit gain: +" + f2(rig.shownGain()) + " m3", "Mud " + f1(rig.mudPpg()) + " ppg: under the pore pressure");
			} else {
				card("^DRILLING", "Mud " + f1(rig.mudPpg()) + " ppg", rig.depthNow() + " m down");
			}
			return shutAt[0] < 0 || t < shutAt[0] + 100;
		});
		var sheet = rig.killSheet();
		if (sheet == null) {
			throw new IllegalStateException("no kick: " + read(rigPos()));
		}
		log(String.format(Locale.ROOT, "kill sheet: SIDPP %.1f, SICP %.1f, KMW %.2f, ICP %.1f, FCP %.1f, strokes to bit %d, circulation %d, MAASP %.1f", sheet.sidpp(), sheet.sicp(),
				sheet.kmw(), sheet.icp(), sheet.fcp(), sheet.strokesToBit(), sheet.strokesCirculation(), rig.maasp()));
		on(level -> {
			weigh(rig, sheet.kmw());
			rig.testKillSteps(4);
		});
		close();
		// 2: the remote choke panel's screen: the kill sheet and gauges; the pump up, the choke held on the schedule
		wide();
		guiWidth(720);   // GUI scale 2 (720 units wide): the panel's layout needs 720; the game caps the scale at 3 on this window anyway
		stand(new Cam(33.5, 8.3, 0.5, 33.5, 7.5, -3.0));
		onS(srv -> panel().open(srv.getPlayerList().getPlayers().get(0)));
		context.waitTicks(30);
		boolean[] pumpUp = {true};
		int[] restarts = {0};
		Runnable operator = () -> onS(srv -> {
			if (rig.well() != null && rig.panel() != null && rig.panel().pumpOn() && rig.well().spm() >= Tuning.SCR_SPM - 1e-6) {
				pumpUp[0] = false;
			}
			operate(rig, pumpUp[0]);
		});
		film(2, "panel", null, null, 900, until(t -> {
			if (t == 30) {
				onS(srv -> panel().pump(true));
				mark("PUMP on: up to SCR, the choke opening");
			}
			if (t >= 30) {
				operator.run();
			}
		}));
		close();
		tall();
		guiWidth(486);
		// 3: the choke manifold, the separator and the flare, the gauges on the card
		showCard();
		Cam c = new Cam(41.0, 5.0, -17.0, 32.0, 4.0, -7.0), d = new Cam(38.0, 4.0, -14.5, 32.0, 4.0, -7.0);
		film(3, "choke", c, d, 400, until(t -> {
			operator.run();
			kill(sheet);
		}));
		// off camera: on to kill mud round, then pump off and choke open
		for (int t = 0; t < 12_000; t++) {
			operator.run();
			if (rig.well() == null) {
				break;
			}
			boolean round = getS(srv -> {
				var well = rig.well();
				var s = rig.killSheet();
				if (rig.panel() != null && !rig.panel().pumpOn() && well.strokes() < 1) {
					restarts[0]++;
					log("shut in again (second kick), starting again: " + read(rigPos()));
					pumpUp[0] = true;
					panel().pump(true);
					return false;
				}
				return well.strokes() > s.strokesCirculation() + 5 && !well.gasInHole() && well.lightestPpg() + 0.05 >= rig.killWeight() - 1e-6;
			});
			if (round) {
				log("kill mud round after " + t + " ticks: " + read(rigPos()));
				break;
			}
			if (t % 400 == 0) {
				log("killing: " + read(rigPos()));
			}
			context.waitTick();
		}
		// 4: kill mud round: pump off, choke open, the flow check, WELL DEAD
		int[] dead = {-1};
		Cam e = new Cam(42.0, 5.0, 12.0, 30.0, 7.0, 0.0), g = new Cam(39.5, 4.0, 9.5, 30.0, 7.5, 0.0);
		film(4, "well_dead", e, g, 900, t -> {
			if (t == 20) {
				onS(srv -> panel().pump(false));
				mark("pump stopped, choke opening for the flow check");
			}
			operator.run();
			if (rig.well() == null) {
				if (dead[0] < 0) {
					dead[0] = t;
					mark("WELL DEAD");
				}
				card("+WELL DEAD", "The flow check holds", "The BOP opens");
			} else {
				String note = rig.killNote();
				card(note.startsWith("FLOW CHECK") ? "^" + note : "^KILL MUD ALL ROUND", "Pump " + f0(rig.well().spm()) + " spm, choke " + f0(rig.well().choke() * 100) + " %",
						"Casing " + f0(rig.well().casingPsi()) + " psi", "Drill pipe " + f0(rig.well().dppGauge()) + " psi");
			}
			return dead[0] < 0 || t < dead[0] + 120;
		});
		close();
		log("chokeKill " + (dead[0] >= 0 ? "ok" : "NOT DEAD") + " (" + restarts[0] + " restarts): " + read(rigPos()));
	}

	private void kill(WellControl.KillSheet sheet) {
		var well = rig.well();
		if (well == null) {
			return;
		}
		card("^KILL: WAIT AND WEIGHT", "Drill pipe " + f0(well.dppGauge()) + " psi", "Schedule   " + f0(rig.schedulePsi()) + " psi", "Casing     " + f0(well.casingPsi()) + " psi",
				"Choke " + f0(well.choke() * 100) + " %   Pump " + f0(well.spm()) + " spm", "Strokes " + f0(well.strokes()) + " of " + f0(sheet.strokesCirculation()),
				"Kill mud " + f1(rig.killWeight()) + " ppg");
	}

	/** The scripted choke operator on the panel's PLC points (OilfieldFollowOnScenes.operate, no bias): hold SICP as the pump comes up, then the schedule. */
	private static void operate(RigSubstructureBlockEntity rig, boolean pumpUp) {
		var panel = rig.panel();
		var well = rig.well();
		if (panel == null || well == null) {
			return;
		}
		double spm = (panel.readMa(5) - 4) / 16 * 60;
		double choke = (panel.readMa(1) - 4) / 16;
		double next;
		if (!panel.pumpOn()) {
			next = rig.killNote().startsWith("kill mud all round") || rig.killNote().startsWith("FLOW CHECK") ? 1.0 : 0.0;
		} else if (pumpUp || spm < Tuning.SCR_SPM - 1e-6) {
			double r = Math.min(1, (spm + Tuning.PUMP_SPM_PER_S * 3 / 20) / Tuning.SCR_SPM);
			next = r * Math.sqrt(Tuning.CHOKE_K_PSI / Math.max(1, rig.killSheet().sicp()));
		} else {
			double dpp = (panel.readMa(2) - 4) / 16 * 1000;
			double err = dpp - rig.schedulePsi();
			next = choke * Math.max(1 / 1.03, Math.min(1.03, Math.exp(0.02 * err * 0.15)));
		}
		panel.commandMa(0, 4 + 16 * Math.max(panel.pumpOn() ? 0.001 : 0, Math.min(1, next)), rig.getLevel().getGameTime());
	}

	// ---- saltCavern (23c) ------------------------------------------------------------------------------------------------------------------
	/**
	 * A 9 x 9 column of rock salt (10 high) under a calcite cap below the pad, a hole cased into it and a brine wellhead, the injection pump on
	 * its 480 V set, the brine to a disposal well (OilfieldFollowOnScenes.gasStorage, set under the ground). Leaching (by hook, the cavern
	 * grown a block every two ticks, cavernStability off) seen from inside the rock (spectator, night vision); the sonar survey and its
	 * outline; the storage wellhead and storage compressor (driven by hook) debrining it, then working gas.
	 */
	void saltCavern() {
		site(5200, 600, new int[] {-76, 24, -56, 18}, 12, new int[] {-72, 20, -52, 14}, 4);
		pad(-72, -52, 20, 14, "minecraft:grass_block");
		var salt = OreBlocks.block(Ore.HALITE).defaultBlockState();
		on(level -> {
			for (int x = -4; x <= 4; x++) {
				for (int z = -4; z <= 4; z++) {
					for (int k = -13; k <= -4; k++) {
						level.setBlock(A(x, k, z), salt, 3);
					}
					for (int k = -3; k <= -2; k++) {
						level.setBlock(A(x, k, z), Blocks.CALCITE.defaultBlockState(), 3);
					}
				}
			}
		});
		for (int k = -5; k <= 1; k++) {
			put(0, k, 0, "wildcatter:casing");
		}
		block(0, 2, 0, WildcatterBlocks.BRINE_WELLHEAD.defaultBlockState().setValue(dev.wildcatter.block.BrineWellheadBlock.FACING, Direction.EAST));
		run("gas_line", 2, new int[] {1, 0}, new int[] {6, 0});
		put(7, 1, 0, "wildcatter:casing");
		block(7, 2, 0, WildcatterBlocks.DISPOSAL_WELLHEAD.defaultBlockState());
		place(WildcatterBlocks.INJECTION_PUMP, -6, 1, -4, NORTH);
		put(-6, 2, -1, "wildcatter:gas_line");
		run("gas_line", 2, new int[] {-6, 0}, new int[] {-1, 0});
		BlockPos set = A(-10, 1, -7);
		genset(set, NORTH, false);
		run("cable_industrial", 1, new int[] {-9, -7}, new int[] {-6, -7}, new int[] {-6, -5});
		startGenset(set, NORTH, 3000);
		InjectionPumpBlockEntity pump = at(A(-6, 1, -4), InjectionPumpBlockEntity.class);
		BrineWellheadBlockEntity brine = at(A(0, 2, 0), BrineWellheadBlockEntity.class);
		Runnable water = () -> onS(srv -> pump.addWater(2_000));
		for (int t = 0; t < 600 && !getS(srv -> pump.inspect().getString()).matches(".*injecting [0-9.]+ L/s.*"); t += 20) {
			water.run();
			context.waitTicks(20);
		}
		log("pump: " + getS(srv -> pump.inspect().getString() + " | head " + brine.inspect().getString()));
		server.runCommand("effect give @p minecraft:night_vision 99999 0 true");
		WildcatterClientGameTest.rules(server, false, "cavernStability");
		// 1: leaching, from inside the rock
		showCard();
		Cam a = new Cam(8.5, -2.0, 8.5, 0.0, -11.0, 0.0), b = new Cam(6.5, -3.5, 6.0, 0.0, -11.0, 0.0);
		boolean[] full = {false};
		int[] fullAt = {-1};
		film(1, "leaching", a, b, 520, t -> {
			if (t % 20 == 0) {
				water.run();
			}
			if (t >= 20 && t % 2 == 0 && !full[0]) {
				onS(srv -> {
					brine.testSalt(Ore.HALITE.kgPerBlock());
					brine.inject(srv.overworld(), 1.0);
					if (brine.inspect().getString().contains("CAVERN AT LIMIT")) {
						full[0] = true;
					}
				});
				if (full[0]) {
					fullAt[0] = t;
					mark("cavern at its limit: " + brine.blocks() + " m3");
				}
			}
			card("^SOLUTION MINING", "Fresh water down, brine up", "Cavern " + f0(brine.blocks()) + " m3", full[0] ? "+At its limit: salt left for the walls" : "Brine to the disposal well");
			return fullAt[0] < 0 || t < fullAt[0] + 80;
		});
		// 2: the sonar survey: 30 s of pings, then the outline and the record
		String[] record = {""};
		Cam c = new Cam(6.0, -3.5, 6.5, 0.0, -11.0, 0.0), d = new Cam(7.0, -4.0, 3.5, 0.0, -11.0, 0.0);
		int[] doneAt = {-1};
		film(2, "sonar", c, d, Tuning.SONAR_TICKS + 260, t -> {
			if (t == 20) {
				onS(srv -> {
					var player = srv.getPlayerList().getPlayers().get(0);
					brine.survey(player);
				});
				mark("sonar down");
			}
			if (t > 20 && doneAt[0] < 0 && t >= 20 + Tuning.SONAR_TICKS + 20) {
				doneAt[0] = t;
				record[0] = recordText(brine);
				mark("survey done: " + record[0]);
			}
			if (doneAt[0] >= 0 && (t - doneAt[0]) % 30 == 15) {
				outline(brine);   // the survey's outline lasts a moment: kept up for the film
			}
			if (doneAt[0] < 0) {
				int left = t < 20 ? Tuning.SONAR_TICKS / 20 : Math.max(0, (Tuning.SONAR_TICKS - (t - 20)) / 20);
				card("^SONAR SURVEY", "The sonde is down", "Surveying: " + left + " s");
			} else {
				card(recordCard(record[0]));
			}
			return doneAt[0] < 0 || t < doneAt[0] + 200;
		});
		// 3: the storage wellhead and storage compressor: debrining, then working gas
		block(0, 2, 0, WildcatterBlocks.STORAGE_WELLHEAD.defaultBlockState().setValue(dev.wildcatter.block.StorageWellheadBlock.FACING, Direction.EAST));
		put(0, 1, -6, "wildcatter:gas_line");
		put(0, 2, -6, "wildcatter:gas_line");
		run("gas_line", 2, new int[] {0, -5}, new int[] {0, -1});
		place(WildcatterBlocks.STORAGE_COMPRESSOR, -1, 1, -12, NORTH);
		// its 1 MW, 4.16 kV motor on a real supply (as gas_storage): the peaker's 12.47 kV feeder to a 1.5 MVA transformer back-fed beside it
		BlockPos station = peaker(-70, -40);
		place(WildcatterBlocks.GSU_TRANSFORMER, -3, 1, -12, Direction.WEST);
		put(-5, 3, -12, "wildcatter:cable_dist");
		put(-5, 2, -12, "wildcatter:cable_dist");
		run("cable_dist", 1, new int[] {-34, -45}, new int[] {-28, -45}, new int[] {-28, -16}, new int[] {-5, -16}, new int[] {-5, -12});
		put(-2, 1, -12, "wildcatter:cable_mv");
		startPeaker(-70, -40, station);
		put(0, 1, -13, "wildcatter:gas_line");
		put(0, 0, -13, "wildcatter:gas_line");
		source(0, -1, -13, Product.PIPELINE_GAS, 10.0);   // the pipeline, its stand-in source under the ground
		context.waitTicks(20);
		StorageWellheadBlockEntity head = at(A(0, 2, 0), StorageWellheadBlockEntity.class);
		StorageCompressorBlockEntity comp = at(A(-1, 1, -12), StorageCompressorBlockEntity.class);
		onS(srv -> head.setMode(StorageWellheadBlockEntity.Mode.INJECT));
		waitUntil(() -> getS(srv -> comp.motor().state() == dev.wildcatter.circuit.Motor.State.RUNNING), 600);
		log("compressor: " + read(A(-1, 1, -12)) + " | transformer " + read(A(-3, 1, -12)));
		server.runCommand("effect clear @p minecraft:night_vision");
		Cam e = new Cam(-9.0, 4.5, -18.0, -0.5, 2.0, -8.0), g = new Cam(-6.5, 3.5, -15.0, 0.0, 2.0, -6.0);
		int[] cushionAt = {-1};
		film(3, "compressor", e, g, 520, until(t -> {
			if (t == 260) {
				onS(srv -> head.testGas(dev.wildcatter.well.Cavern.cushion(head.volume()) - head.gas()));
				cushionAt[0] = t;
				mark("debrined (by hook to the cushion): working gas from here");
			}
			String h = head.inspect().getString();
			card("^STORAGE COMPRESSOR", h.contains("DEBRINING") ? "Gas in, brine out: DEBRINING" : head.dry() ? "+Cavern dry: working gas" : "Gas into the cavern",
					f0(head.bar()) + " bar in the cavern", "Working gas " + f0(head.working()) + " Sm3");
		}));
		close();
		WildcatterClientGameTest.rules(server, true, "cavernStability");   // off for the scene: the pump keeps running, and with it on it leaches on past the walls
		log("saltCavern ok: " + getS(srv -> head.inspect().getString()));
	}

	private static String recordText(BrineWellheadBlockEntity brine) {
		try {
			var hole = BrineWellheadBlockEntity.class.getDeclaredMethod("hole");
			hole.setAccessible(true);
			Object h = hole.invoke(brine);
			var text = Class.forName("dev.wildcatter.block.Caverns").getDeclaredMethod("recordText", net.minecraft.world.level.Level.class, h.getClass());
			text.setAccessible(true);
			return (String) text.invoke(null, brine.getLevel(), h);
		} catch (ReflectiveOperationException ex) {
			return "record unread: " + ex;
		}
	}

	private void outline(BrineWellheadBlockEntity brine) {
		onS(srv -> {
			try {
				var hole = BrineWellheadBlockEntity.class.getDeclaredMethod("hole");
				hole.setAccessible(true);
				Object h = hole.invoke(brine);
				var draw = Class.forName("dev.wildcatter.block.Caverns").getDeclaredMethod("outline", ServerLevel.class, h.getClass());
				draw.setAccessible(true);
				draw.invoke(null, srv.overworld(), h);
			} catch (ReflectiveOperationException ex) {
				log("outline: " + ex);
			}
		});
	}

	/** "cavern 176 m3, 8 m high, 5 m across, salt 2 m to the flank and 3 m over the roof" on the card's lines. */
	private static String[] recordCard(String record) {
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("cavern ([0-9,]+) m3, (\\d+) m high, (\\d+) m across, salt (\\d+) m to the flank and (\\d+) m over the roof(.*)").matcher(record);
		if (!m.find()) {
			return new String[] {"^SONAR SURVEY", record};
		}
		return new String[] {"^SONAR SURVEY", "Cavern " + m.group(1) + " m3", m.group(2) + " m high, " + m.group(3) + " m across", "Salt to the flank: " + m.group(4) + " m",
				"Salt over the roof: " + m.group(5) + " m", m.group(6).contains("THIN") ? "!WALL THIN" : "+Walls sound"};
	}

	// ---- frasch (23d) ------------------------------------------------------------------------------------------------------------------------
	/**
	 * A caprock of calcite (13 x 13 x 4) with native sulfur in a third of it under the pad, a hole cased into it and a Frasch wellhead: the
	 * Frasch water heater (water by hook, fuel gas off a test source) and the lift air compressor on a 480 V set on the pipe to its west
	 * side; molten sulfur out of its east side by pipe to a sulfur vat; a bleed water well into the caprock (OilfieldFollowOnScenes.frasch).
	 */
	void frasch() {
		site(6400, 600, new int[] {-36, 20, -14, 14}, 10, new int[] {-32, 16, -10, 10}, 3);
		pad(-32, -10, 16, 10, "minecraft:grass_block");
		on(level -> {
			for (int x = -6; x <= 6; x++) {
				for (int z = -6; z <= 6; z++) {
					for (int k = -4; k <= -1; k++) {
						boolean s = Math.floorMod(x + 2 * z + k, 3) == 0 && !(x == 0 && z == 0);
						level.setBlock(A(x, k, z), s ? OreBlocks.block(Ore.SULFUR).defaultBlockState() : Blocks.CALCITE.defaultBlockState(), 3);
					}
				}
			}
		});
		put(0, -1, 0, "wildcatter:casing");
		put(0, 0, 0, "wildcatter:casing");
		block(0, 1, 0, WildcatterBlocks.FRASCH_WELLHEAD.defaultBlockState().setValue(dev.wildcatter.block.FraschWellheadBlock.FACING, Direction.EAST));
		run("gas_line", 1, new int[] {1, 0}, new int[] {7, 0});
		place(WildcatterBlocks.SULFUR_VAT, 8, 1, -3, NORTH);
		run("gas_line", 1, new int[] {-12, 0}, new int[] {-1, 0});
		place(WildcatterBlocks.FRASCH_WATER_HEATER, -16, 1, -4, NORTH);
		place(WildcatterBlocks.LIFT_AIR_COMPRESSOR, -11, 1, -4, NORTH);
		put(-12, 1, 3, "wildcatter:gas_line");
		put(-12, 0, 3, "wildcatter:gas_line");
		source(-12, -1, 3, Product.PIPELINE_GAS, 2.0);   // the fuel gas, its stand-in source under the ground
		BlockPos set = A(-24, 1, -6);
		genset(set, NORTH, false);
		run("cable_industrial", 1, new int[] {-23, -6}, new int[] {-12, -6}, new int[] {-12, -4});
		run("cable_industrial", 1, new int[] {-17, -5}, new int[] {-17, -4});
		sizeCables(A(-23, 1, -6), A(-12, 1, -4), WildcatterBlocks.CABLE_INDUSTRIAL, dev.wildcatter.energy.ConductorSize.KCMIL_500);   // the compressor's start
		// the bleed water well
		put(4, -1, 4, "wildcatter:casing");
		put(4, 0, 4, "wildcatter:casing");
		block(4, 1, 4, WildcatterBlocks.DISPOSAL_WELLHEAD.defaultBlockState());
		FraschWaterHeaterBlockEntity heater = at(A(-16, 1, -4), FraschWaterHeaterBlockEntity.class);
		SulfurVatBlockEntity vat = at(A(8, 1, -3), SulfurVatBlockEntity.class);
		Runnable water = () -> onS(srv -> heater.addWater(20_000));
		water.run();
		startGenset(set, NORTH, 3000);
		FraschWellheadBlockEntity head = at(A(0, 1, 0), FraschWellheadBlockEntity.class);
		for (int t = 0; t < 3000 && head.rate() <= 0; t += 20) {
			water.run();
			context.waitTicks(20);
			if (t % 400 == 0) {
				log("frasch waiting: " + getS(srv -> head.inspect().getString()) + " | heater " + read(A(-16, 1, -4)) + " | air " + read(A(-11, 1, -4)));
			}
		}
		log("frasch lifting: " + getS(srv -> head.inspect().getString()) + " | heater " + read(A(-16, 1, -4)));
		if (head.rate() <= 0) {
			throw new IllegalStateException("the Frasch well is not lifting: " + read(A(0, 1, 0)));
		}
		showCard();
		// 1: hot water and air down: the heater and the compressor to the wellhead
		Cam a = new Cam(-5.0, 7.0, 11.0, -13.0, 2.0, -1.0), b = new Cam(-7.5, 5.5, 8.0, -12.0, 2.0, -1.0);
		film(1, "hot_water", a, b, 300, until(t -> {
			if (t % 20 == 0) {
				water.run();
			}
			card("^FRASCH SULFUR", "Water heated to " + f0(heater.temp()) + " C", "Hot water and air", "down the well");
		}));
		// 2: molten sulfur up, by pipe to the vat
		Cam c = new Cam(-3.0, 5.0, 8.0, 4.5, 1.2, -0.5), d = new Cam(0.5, 4.5, 8.0, 6.0, 1.2, -0.5);
		film(2, "molten_sulfur", c, d, 360, until(t -> {
			if (t % 20 == 0) {
				water.run();
			}
			card("^MOLTEN SULFUR UP", f1(head.rate() / 1000) + " t an hour", "In the vat: " + f0(vat.poured()) + " kg");
		}));
		close();
		log("frasch ok: " + read(A(0, 1, 0)) + " | vat " + read(A(8, 1, -3)));
	}

	/**
	 * The peaker (ChemicalScenes.peaker moved by px, pz): an aeroderivative gas turbine, its 13.8 kV bus duct, GSU and substation
	 * transformer, 12.47 kV (11 kV IEC) out of the substation transformer's throat at (px+36, pz-5); its station set and unit substation.
	 * Returns the station set's panel.
	 */
	private BlockPos peaker(int px, int pz) {
		place(WildcatterBlocks.AERO_TURBINE, px + 20, 1, pz, NORTH);
		place(WildcatterBlocks.AERO_INLET_HOUSE, px + 20, 1, pz - 5, NORTH);
		place(WildcatterBlocks.AERO_EXHAUST_STACK, px + 20, 1, pz + 14, NORTH);
		place(WildcatterBlocks.AERO_GENERATOR, px + 25, 1, pz + 1, NORTH);
		place(WildcatterBlocks.GEN_BREAKER_3000, px + 26, 1, pz + 8, NORTH);
		place(WildcatterBlocks.GSU_60, px + 36, 1, pz + 8, NORTH);
		place(WildcatterBlocks.SUBSTATION_TRANSFORMER_60, px + 36, 1, pz - 6, NORTH);
		place(WildcatterBlocks.STOCK_TANK, px + 14, 1, pz + 4, NORTH);
		put(px + 18, 1, pz + 6, "wildcatter:gas_line");
		put(px + 19, 1, pz + 6, "wildcatter:gas_line");
		run("bus_duct_gen", 1, new int[] {px + 26, pz + 10}, new int[] {px + 37, pz + 10});
		put(px + 36, 1, pz + 9, "wildcatter:bus_duct_gen");
		sizeCables(A(px + 26, 1, pz + 9), A(px + 37, 1, pz + 10), WildcatterBlocks.BUS_DUCT_GEN, dev.wildcatter.energy.ConductorSize.BUS_DUCT_3000);
		for (int z = -10; z <= 4; z++) {
			put(px + 36, 7, pz + z, "wildcatter:cable_hv");
		}
		BlockPos station = A(px + 10, 1, pz + 2);
		genset(station, NORTH, false);
		place(WildcatterBlocks.UNIT_SUBSTATION, px + 14, 1, pz + 2, Direction.SOUTH);
		run("cable_mv", 1, new int[] {px + 15, pz + 2}, new int[] {px + 19, pz + 2});
		fillTank(A(px + 14, 1, pz + 4), Product.KEROSENE, 40_000);
		return station;
	}

	/** Starts the peaker's station set, then the turbine, its start and ramp skipped (ChemicalScenes.startPeaker). */
	private void startPeaker(int px, int pz, BlockPos station) {
		startGenset(station, NORTH, 3000);
		GasTurbineBlockEntity gt = at(A(px + 20, 1, pz), GasTurbineBlockEntity.class);
		onS(srv -> gt.setWantRun(true));
		context.waitTicks(200);
		onS(srv -> gt.skipStartForTest(false));
		context.waitTicks(100);
		onS(srv -> gt.skipRampForTest());
		context.waitTicks(100);
		log("peaker: " + read(A(px + 20, 1, pz)) + " | station " + read(station));
	}

	// ---- bess (Phase 22) ------------------------------------------------------------------------------------------------------------------
	/**
	 * The cell line making a module (on its 480 V set by busway, HF and ethylene off test sources, the feeds and its progress to 97 % by
	 * hook); a utility block (PCS, two containers, MV skid, plant controller) on the peaker's 12.47 kV (11 kV IEC) feeder as BessScenes'
	 * bess_block lays it out: its containers filled with modules (by hook) and charging; idle on frequency response, a load bank step on the
	 * peaker's bus drops the frequency and the block answers; then a commercial cabinet set to GRID FORMING black-starts a dead 480 V island
	 * of heaters with its soft start.
	 */
	void bess() {
		site(7600, 600, new int[] {-48, 48, -36, 54}, 14, new int[] {-44, 44, -32, 50}, 5);
		pad(-44, -32, 44, 50, "minecraft:grass_block");
		// the peaker (ChemicalScenes.peaker at -40, -20) with two load banks on its 13.8 kV bus
		int px = -40, pz = -20;
		BlockPos station = peaker(px, pz);
		BlockPos bank1 = A(px + 29, 1, pz + 11), bank2 = A(px + 32, 1, pz + 11);
		placeBig(WildcatterBlocks.LOAD_BANK, bank1, NORTH);
		placeBig(WildcatterBlocks.LOAD_BANK, bank2, NORTH);
		// the utility block (bess_block)
		place(WildcatterBlocks.BESS_MV_SKID, 10, 1, 0, NORTH);
		place(WildcatterBlocks.BESS_PCS, 10, 1, -3, NORTH);
		place(WildcatterBlocks.BESS_CONTAINER, 10, 1, -9, NORTH);
		place(WildcatterBlocks.BESS_CONTAINER, 13, 1, -6, NORTH);
		place(WildcatterBlocks.BESS_PLANT_CONTROLLER, 2, 1, -6, NORTH);
		run("control_cable", 1, new int[] {5, -4}, new int[] {5, -3}, new int[] {9, -3});
		run("cable_dist", 1, new int[] {-4, -25}, new int[] {20, -25}, new int[] {20, 0}, new int[] {13, 0});
		// the cell line on its own 480 V set, busway to its box; HF and ethylene off test sources
		place(WildcatterBlocks.CELL_LINE, 30, 1, 20, NORTH);
		// three 500 kW sets in parallel on its busway, as BessScenes: one alone trips on frequency at the line's start
		BlockPos lineSet = A(28, 1, 38), lineSet2 = A(28, 1, 42), lineSet3 = A(28, 1, 46);
		genset(lineSet, NORTH, false);
		genset(lineSet2, NORTH, false);
		genset(lineSet3, NORTH, false);
		run("busway_industrial", 1, new int[] {29, 46}, new int[] {29, 20});
		for (int z : new int[] {20, 22}) {
			put(34, 1, z, "wildcatter:gas_line");
			put(34, 0, z, "wildcatter:gas_line");
		}
		source(34, -1, 20, Product.ANHYDROUS_HF, 1.0);   // stand-in sources under the ground
		source(34, -1, 22, Product.ETHYLENE, 1.0);
		// the dead island: a diesel set left stopped, 480 V cable to a commercial cabinet, four heaters (BessScenes.cabinetBus)
		BlockPos islandSet = A(0, 1, 12);
		genset(islandSet, NORTH, false);
		run("cable_industrial", 1, new int[] {1, 12}, new int[] {7, 12});
		sizeCables(A(1, 1, 12), A(7, 1, 12), WildcatterBlocks.CABLE_INDUSTRIAL, dev.wildcatter.energy.ConductorSize.KCMIL_500);
		for (int i = 0; i < 4; i++) {
			put(2 + i, 1, 13, "wildcatter:three_phase_heater");
		}
		place(WildcatterBlocks.BESS_CABINET, 8, 1, 12, NORTH);
		BessCabinetBlockEntity cab = at(A(8, 1, 12), BessCabinetBlockEntity.class);
		onS(srv -> {
			cab.setModules(Tuning.CABINET_MODULES);
			cab.cells().setSoc(0.8);
		});
		BessContainerBlockEntity boxA = at(A(10, 1, -9), BessContainerBlockEntity.class);
		BessContainerBlockEntity boxB = at(A(13, 1, -6), BessContainerBlockEntity.class);
		BessPcsBlockEntity pcs = at(A(10, 1, -3), BessPcsBlockEntity.class);
		BessPlantControllerBlockEntity plant = at(A(2, 1, -6), BessPlantControllerBlockEntity.class);
		CellLineBlockEntity cells = at(A(30, 1, 20), CellLineBlockEntity.class);
		// start the cell line's set and the peaker
		startGenset(lineSet, NORTH, 3000);
		startGenset(lineSet2, NORTH, 3000);
		startGenset(lineSet3, NORTH, 3000);
		startPeaker(px, pz, station);
		onS(srv -> {
			cells.slots().set(0, new ItemStack(WildcatterItems.LFP_CATHODE_BAG, 1));
			cells.slots().set(1, new ItemStack(WildcatterItems.PURIFIED_GRAPHITE_BAG, 1));
			cells.slots().set(2, new ItemStack(WildcatterItems.LI2CO3_BAG, 1));
			cells.slots().set(3, new ItemStack(Items.COPPER_INGOT, 2));
			cells.slots().set(4, new ItemStack(WildcatterItems.ALUMINIUM_INGOT, 2));
		});
		if (!waitUntil(() -> getS(srv -> cells.progress()) > 0, 600)) {
			log("cell line not started: " + read(A(30, 1, 20)) + " | busway " + read(A(29, 1, 20)) + " / " + read(A(29, 1, 38)) + " | set " + read(lineSet));
		}
		onS(srv -> cells.testProgress(0.97));
		log("cell line: " + read(A(30, 1, 20)) + " | busway at its box " + read(A(29, 1, 20)) + " | at the set " + read(A(29, 1, 38)) + " | set " + read(lineSet));
		showCard();
		// 1: the cell line makes a module
		int made0 = cells.made();
		int[] madeAt = {-1};
		Cam a = new Cam(40.0, 5.0, 14.0, 31.5, 2.0, 22.0), b = new Cam(38.0, 4.0, 17.0, 31.5, 2.0, 24.0);
		film(1, "cell_line", a, b, 900, t -> {
			if (cells.made() > made0 && madeAt[0] < 0) {
				madeAt[0] = t;
				mark("module made");
			}
			card("^CELL LINE", madeAt[0] < 0 ? "Module " + f0(cells.progress() * 100) + " %" : "+MODULE MADE", f1(Tuning.LFP_MODULE_KWH) + " kWh LFP module", "52 cells in series");
			return madeAt[0] < 0 || t < madeAt[0] + 100;
		});
		// 2: the containers filling with modules, then charging
		onS(srv -> {
			boxA.setModules(0);
			boxB.setModules(0);
			boxA.cells().setSoc(0.2);
			boxB.cells().setSoc(0.2);
			plant.setFrequencyResponse(false);
			plant.setPreset(Preset.IDLE);
		});
		Cam c = new Cam(22.0, 6.0, -16.0, 12.0, 2.0, -5.0), d = new Cam(19.0, 5.0, -13.5, 12.0, 2.0, -4.0);
		film(2, "containers", c, d, 520, until(t -> {
			if (t >= 20 && t <= 20 + Tuning.CONTAINER_MODULES * 2 && t % 2 == 0) {
				int n = Math.min(Tuning.CONTAINER_MODULES, (t - 20) / 2);
				onS(srv -> {
					boxA.setModules(n);
					boxB.setModules(n);
				});
			}
			if (t == 20 + Tuning.CONTAINER_MODULES * 2 + 20) {
				onS(srv -> plant.setPreset(Preset.CHARGE));
				mark("charging");
			}
			boolean charging = pcs.gridWatts() < -1000;
			card("^BATTERY CONTAINERS", "Modules " + boxA.modules() + " + " + boxB.modules() + " of " + (2 * Tuning.CONTAINER_MODULES),
					charging ? "+Charging " + f1(-pcs.gridWatts() / 1e6) + " MW" : "Not charging yet", "State of charge " + f1(pcs.soc(pcs.getLevel()) * 100) + " %");
		}));
		// 3: idle on frequency response; the load banks step on, the frequency drops, the battery answers
		onS(srv -> {
			plant.setPreset(Preset.IDLE);
			plant.setFrequencyResponse(true);
		});
		context.waitTicks(200);
		double nominal = ElectricalStandard.active().hertz();
		double[] low = {nominal};
		Cam e = new Cam(20.0, 5.0, -15.0, 11.0, 2.0, -5.0), g = new Cam(18.0, 4.5, -12.0, 11.0, 2.0, -4.0);
		film(3, "frequency", e, g, 400, until(t -> {
			if (t == 40) {
				onS(srv -> {
					((LoadBankBlockEntity) BigParts.entity(srv.overworld(), bank1)).setSteps(Tuning.LOAD_BANK_STEPS);
					((LoadBankBlockEntity) BigParts.entity(srv.overworld(), bank2)).setSteps(Tuning.LOAD_BANK_STEPS);
				});
				mark("load banks on: " + (2 * Tuning.LOAD_BANK_STEPS) + " MW");
			}
			double hz = pcs.measure(dev.wildcatter.control.Measurable.Quantity.HERTZ);
			low[0] = Math.min(low[0], hz);
			double mw = pcs.gridWatts() / 1e6;
			card(t < 40 ? "^GRID STEADY" : "!LOAD STEP: " + (2 * Tuning.LOAD_BANK_STEPS) + " MW", "Frequency " + String.format(Locale.ROOT, "%.2f", hz) + " Hz",
					mw > 0.05 ? "+Battery answers: " + f2(mw) + " MW" : "Battery idle, standing by", "Lowest " + String.format(Locale.ROOT, "%.2f", low[0]) + " Hz");
		}));
		log("frequency: lowest " + low[0] + " | pcs " + read(A(10, 1, -3)) + " | peaker " + read(A(px + 20, 1, pz)));
		// 4: the black start: the cabinet forms the dead island's bus, its voltage ramped up over the soft start
		Cam h = new Cam(8.0, 4.5, 20.0, 5.0, 1.5, 12.5), i = new Cam(6.5, 3.8, 18.0, 5.0, 1.5, 12.5);
		int[] closedAt = {-1};
		film(4, "black_start", h, i, 300, until(t -> {
			if (t == 40) {
				onS(srv -> cab.setPreset(Preset.FORMING));
				mark("GRID FORMING");
			}
			if (cab.breakerClosed() && closedAt[0] < 0) {
				closedAt[0] = t;
				mark("cabinet closes onto the dead bus: soft start");
			}
			boolean soft = closedAt[0] >= 0 && t - closedAt[0] < Tuning.BESS_SOFT_START_SECONDS * 20 + 10;
			card(t < 40 ? "!DEAD ISLAND: NO SET RUNNING" : soft ? "^BLACK START: SOFT START" : closedAt[0] >= 0 ? "+BUS LIVE" : "^BLACK START", "Bus " + f0(cab.volts()) + " V (" + Voltage.INDUSTRIAL.label() + ")",
					"Heaters " + f0(Math.max(0, cab.gridWatts()) / 1000) + " kW", "Battery state of charge " + f0(cab.soc(cab.getLevel()) * 100) + " %");
		}));
		close();
		log("bess ok: cabinet " + read(A(8, 1, 12)));
	}

	// ---- fuelPool (8e) ------------------------------------------------------------------------------------------------------------------------
	/**
	 * The spent fuel pool (docs/multiblocks/fuel-handling.md) in a dark fuel building: its controller digs, lines and fills the 6 x 6 m
	 * pool, 12 m deep, and the racks are loaded with irradiated assembly records (as the refuelling machine stores them): two rows from
	 * this outage, a week out of the core; a row cooled 60 to 110 days; a row cooled 150 to 365 days. Their Cherenkov glow is the
	 * renderer's, from each assembly's own decay heat. Filmed in the landscape window (2880 x 1620) for a vertical cut from its middle;
	 * FUEL_POOL_CHECK=1 takes stills only (day and night, with and without the building's roof) to judge the glow.
	 */
	void fuelPool() {
		wide();
		guiWidth(480);
		FilmCard.narrow = true;
		try {
			site(8800, 600, new int[] {-24, 24, -24, 24}, 12, new int[] {-12, 12, -14, 10}, 3);
			pad(-16, -16, 16, 12, "minecraft:grass_block");
			place(WildcatterBlocks.SPENT_FUEL_POOL, 0, 1, 0, NORTH);   // x 0-3, z 0-3; the pool x -1..4, z -6..-1, 12 deep
			BlockPos poolAt = A(0, 1, 0);
			dev.wildcatter.block.SpentFuelPoolBlockEntity pool = at(poolAt, dev.wildcatter.block.SpentFuelPoolBlockEntity.class);
			if (!waitUntil(() -> getS(srv -> srv.overworld().getBlockState(pool.cell(2, 2, 6)).is(Blocks.WATER)), 400)) {
				throw new IllegalStateException("the pool did not dig: " + read(poolAt));
			}
			// the fuel building: concrete walls and a roof, dim lights high on its walls
			String wall = "minecraft:light_gray_concrete";
			fill(-5, 1, -10, 8, 10, -10, wall);
			fill(-5, 1, 6, 8, 10, 6, wall);
			fill(-5, 1, -10, -5, 10, 6, wall);
			fill(8, 1, -10, 8, 10, 6, wall);
			fill(-5, 10, -10, 8, 10, 6, wall);
			fill(-4, 0, -9, 7, 0, 5, "minecraft:smooth_stone replace minecraft:grass_block");
			for (int[] l : new int[][] {{-4, -8}, {7, -8}, {-4, 4}, {7, 4}, {1, -9}, {-4, -2}, {7, -2}}) {
				put(l[0], 8, l[1], "minecraft:light[level=7]");
			}
			// the racks: index i is across i % 6 (x -1..4), front i / 6 (z -1 at the controller back to -6)
			double[] days = {7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 60, 70, 80, 90, 100, 110, 150, 180, 220, 260, 300};
			onS(srv -> {
				for (double d : days) {
					pool.store(srv.overworld(), assembly(d));
				}
			});
			context.waitTicks(40);
			log("fuelPool built: " + getS(srv -> pool.inspect().getString()));
			if (System.getenv("FUEL_POOL_CHECK") != null) {
				checkStills();
				return;
			}
			server.runCommand("time set 18000");
			showCard();
			card();
			// 1: looking down into the pool at the racks glowing, a slow push down toward the water
			film(1, "look_down", new Cam(1.5, 6.5, -1.2, 1.5, -10.0, -2.4), new Cam(1.5, 2.0, -1.9, 1.5, -10.0, -2.4), 300, until(t -> {}));
			// 2: under the water, a slow move along the racks on their west side
			film(2, "along_racks", new Cam(-0.6, -4.0, -0.5, 2.2, -9.5, -2.0), new Cam(-0.6, -4.0, -5.5, 2.2, -9.5, -4.5), 320, until(t -> {}));
			// 3: under the water: a fresh assembly racked at the end of a row of cooled ones
			film(3, "fresh_beside_cooled", new Cam(0.8, -5.2, -5.6, 4.0, -9.5, -3.5), new Cam(1.2, -5.6, -5.5, 4.0, -9.5, -3.5), 240, t -> {
				if (t == 60) {
					pool.store(pool.getLevel(), assembly(7));
					mark("a fresh assembly (7 days out of the core) racked at the row's near end, beside ones cooled 150-300 days");
				}
				return true;
			});
			// 4: the pool's readout on a large card over the whole pool
			film(4, "readout", new Cam(1.5, 3.4, -1.4, 1.5, -10.0, -2.6), new Cam(1.5, 3.1, -1.6, 1.5, -10.0, -2.6), 260, until(t -> {
				double heat = 0;
				double hottest = 0;
				for (dev.wildcatter.fluid.SpentFuel.Assembly a : pool.racks()) {
					heat += a.watts();
					hottest = Math.max(hottest, a.watts());
				}
				card("^SPENT FUEL POOL", "Racks " + pool.racks().size() + " of " + Tuning.POOL_RACKS, "Decay heat " + f0(heat / 1000) + " kW",
						"Hottest assembly " + f0(hottest / 1000) + " kW", "Water " + f1(pool.tempC()) + " °C", f1(pool.layers() - Tuning.POOL_FUEL_TOP_M) + " m of water over the fuel");
			}));
			card();
			close();
			log("fuelPool ok: " + read(poolAt));
		} finally {
			FilmCard.narrow = false;
		}
	}

	/** An irradiated assembly's record {@code days} out of the core after a long run at full power: each decay group at its share, decayed. */
	private static dev.wildcatter.fluid.SpentFuel.Assembly assembly(double days) {
		double[] d = new double[Tuning.DECAY_LAMBDA.length];
		for (int g = 0; g < d.length; g++) {
			d[g] = Tuning.DECAY_SHARE[g] * Math.exp(-Tuning.DECAY_LAMBDA[g] * days * 86400);
		}
		return new dev.wildcatter.fluid.SpentFuel.Assembly(40, d, false, days * 86400);
	}

	/** Stills at the film's size to judge the glow: day and night, with the building's roof and without. */
	private void checkStills() {
		java.nio.file.Path dir = java.nio.file.Path.of("../../scene-videos/fuelPool-check").toAbsolutePath().normalize();
		Cam down = new Cam(1.5, 6.0, -0.3, 1.5, -9.0, -4.0), row = new Cam(4.6, 2.2, -3.5, 0.5, -7.5, -3.5), under = new Cam(-0.5, -3.0, -0.5, 2.5, -9.0, -4.5);
		dev.wildcatter.block.SpentFuelPoolBlockEntity pool = at(A(0, 1, 0), dev.wildcatter.block.SpentFuelPoolBlockEntity.class);
		log("pool check: origin " + A(0, 0, 0).toShortString() + " racks " + pool.racks().size() + " cell(0,0,1) " + pool.cell(0, 0, 1).toShortString() + "="
				+ getS(srv -> srv.overworld().getBlockState(pool.cell(0, 0, 1)).toString()) + " cell(0,0,12) " + pool.cell(0, 0, 12).toShortString() + " rack0 "
				+ java.util.Arrays.toString(pool.rackPoint(0)) + " | " + getS(srv -> pool.inspect().getString()));
		fill(-5, 10, -10, 8, 10, 6, "minecraft:air");
		server.runCommand("time set 6000");
		camera(new Cam(1.5, 24.0, 16.0, 1.5, -2.0, -3.0), true);
		context.waitTicks(80);
		log("camera at " + context.computeOnClient(client -> client.player.position().toString() + " yaw " + client.player.getYRot() + " pitch " + client.player.getXRot()));
		context.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("far").withSize(2880, 1620).withDestinationDir(dir).disableCounterPrefix());
		fill(-5, 10, -10, 8, 10, 6, "minecraft:light_gray_concrete");
		String[][] looks = {{"hall_night", "18000", "roof"}, {"open_night", "18000", ""}, {"open_day", "6000", ""}};
		for (String[] look : looks) {
			server.runCommand("time set " + look[1]);
			if (look[2].isEmpty()) {
				fill(-5, 10, -10, 8, 10, 6, "minecraft:air");
			}
			for (Cam c : new Cam[] {down, row, under}) {
				camera(c, true);
				context.waitTicks(60);
				log("camera at " + context.computeOnClient(client -> client.player.position().toString()));
				String name = look[0] + (c == down ? "_down" : c == row ? "_row" : "_under");
				context.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of(name).withSize(2880, 1620).withDestinationDir(dir).disableCounterPrefix());
				log("still " + name);
			}
		}
	}

	// ---- shorts batch 3 (2026-11-21): scram, blowout, arcflash ------------------------------------------------------------------------
	// Filmed in the landscape window (2880 x 1620) with the readout card kept in the frame's middle 9:16 for a vertical cut, a 3 s still at
	// each end of every film.

	/**
	 * A reactor trip at power: the trailer's turbine island (TrailerClientGameTest.buildTurbineIsland) at 25 % with its turbine generator on
	 * line, then a manual trip on the console. The rods drop in 2 s on the
	 * console's rod position indicators, the trip breakers clunk and the annunciator horn sounds, the fission power falls on the delayed
	 * neutrons; the decay heat carries on and both reactor coolant pumps keep running.
	 */
	void scram() {
		wide();
		guiWidth(480);
		FilmCard.narrow = true;
		still = 60;
		try {
			site(9400, 600, new int[] {-70, 150, -90, 80}, 24, new int[] {-44, 130, -64, 46}, 9);
			int cx = ox;
			pad(-44, -64, 130, 46, "minecraft:grass_block");
			ox = cx - 30;
			buildTurbineIsland();
			BlockPos vessel = A(0, 1, 0), cvcs = A(-3, 1, 0), afw = A(12, 1, 0), da = A(29, 1, 13), console = A(3, 4, 1);
			BlockPos pumpA = A(0, 1, -5), pumpB = A(0, 1, 6);
			dev.wildcatter.fluid.Reactor r = getS(srv -> ((dev.wildcatter.block.ReactorVesselBlockEntity) srv.overworld().getBlockEntity(vessel)).reactor());
			Runnable keepWater = () -> onS(srv -> {   // from a beat: already on the server thread
				for (BlockPos at : new BlockPos[] {cvcs, afw, da}) {
					if (srv.overworld().getBlockEntity(at) instanceof dev.wildcatter.block.WaterUser user) {
						user.addWater(100_000);
					}
				}
			});
			java.util.function.DoubleSupplier flowA = () -> getS(srv -> ((dev.wildcatter.block.ReactorCoolantPumpBlockEntity) srv.overworld().getBlockEntity(pumpA)).flow());
			java.util.function.DoubleSupplier flowB = () -> getS(srv -> ((dev.wildcatter.block.ReactorCoolantPumpBlockEntity) srv.overworld().getBlockEntity(pumpB)).flow());
			double rated = dev.wildcatter.fluid.Reactor.ratedWatts();
			log("scram at power: " + read(vessel));
			showCard();
			// 1: the island at power
			film(1, "at_power", new Cam(9.5, 10.0, -5.0, 1.5, 6.0, 1.0), new Cam(8.0, 8.5, -3.5, 1.5, 6.0, 1.0), 260, until(t -> {
				if (t % 40 == 0) {
					keepWater.run();
				}
				card("^REACTOR AT POWER", "Power " + f1(r.power() * 100) + " %", "Rods: bank D " + f0(r.bankD()) + " of " + Tuning.ROD_STEPS + " steps",
						"Coolant pumps A " + f0(flowA.getAsDouble() * 100) + " %  B " + f0(flowB.getAsDouble() * 100) + " %");
			}));
			// 2: close on the console's rod position indicators: the trip, the rods drop
			int[] tripAt = {-1}, inAt = {-1};
			Cam rpi = new Cam(6.6, 6.6, -2.4, 4.4, 6.0, 1.6), rpi2 = new Cam(6.3, 6.5, -2.0, 4.4, 6.0, 1.6);
			film(2, "trip", rpi, rpi2, 400, until(t -> {
				if (t % 40 == 0) {
					keepWater.run();
				}
				if (t == 80) {
					r.setSetpoint(-1);
					tripAt[0] = t;
					mark("MANUAL TRIP");
				}
				boolean in = r.shutdownBank() <= 0 && r.bankD() <= 0;
				if (tripAt[0] >= 0 && in && inAt[0] < 0) {
					inAt[0] = t;
					mark(String.format(Locale.ROOT, "all rods in, %.2f s after the trip", (t - tripAt[0]) / 20.0));
				}
				String rods = "Rods: shutdown " + f0(r.shutdownBank()) + "  bank D " + f0(r.bankD());
				if (tripAt[0] < 0) {
					card("^REACTOR AT POWER", "Power " + f1(r.power() * 100) + " %", rods, "Coolant pumps running");
				} else {
					card("!REACTOR TRIP", inAt[0] >= 0 ? "+ALL RODS IN: " + String.format(Locale.ROOT, "%.1f", (inAt[0] - tripAt[0]) / 20.0) + " s" : rods,
							"Power " + f2(r.power() * 100) + " %", "Decay heat " + f1(r.decayHeat() * rated / 1e6) + " MW");
				}
			}));
			log("tripped: " + read(vessel));
			// 3: the island after the trip: fission power falling, the decay heat, the pumps still cooling
			film(3, "after", new Cam(8.0, 8.5, -3.5, 1.5, 6.0, 1.0), new Cam(9.5, 10.0, -5.0, 1.5, 6.0, 1.0), 360, until(t -> {
				if (t % 40 == 0) {
					keepWater.run();
				}
				card("!REACTOR TRIPPED", "Fission power " + f2(r.power() * 100) + " %", "+Decay heat " + f1(r.decayHeat() * rated / 1e6) + " MW, still coming",
						"+Pumps A " + f0(flowA.getAsDouble() * 100) + " %  B " + f0(flowB.getAsDouble() * 100) + " %: cooling", "Tavg " + f1(r.tavg()) + " °C");
			}));
			card();
			close();
			log("scram ok: " + read(vessel));
		} finally {
			FilmCard.narrow = false;
			still = 0;
		}
	}

	/**
	 * An uncontrolled well: the chokeKill rig drilling into the deep field at 12.8 ppg, the kick comes and the alarm sounds, and nobody shuts
	 * it in: after the 30 s window the well blows out (RigSubstructureBlockEntity's BLOWOUT), mud, oil and gas at the rig floor. No fire is
	 * near it, so it does not light.
	 */
	void blowout() {
		wide();
		guiWidth(480);
		FilmCard.narrow = true;
		still = 60;
		try {
			rigSite();
			int top = field.topY();
			on(level -> {
				if (rig.phase() != RigSubstructureBlockEntity.Phase.IDLE) {
					rig.toggle();
				}
				rig.testHoldBit(false);
				weigh(rig, 12.8);
				rig.testDepth(hole.getY() - top - 3);
			});
			context.waitTicks(40);
			showCard();
			int[] kickAt = {-1}, blowAt = {-1};
			java.util.regex.Pattern within = java.util.regex.Pattern.compile("within (\\d+) s");
			// 1: drilling, the kick, the alarm, the window running out: BLOWOUT
			film(1, "kick", new Cam(17.5, 9.0, 11.0, 30.0, 9.0, 0.0), new Cam(19.5, 9.0, 8.5, 30.0, 9.0, 0.0), 1500, t -> {
				if (t == 60) {
					onS(srv -> rig.toggle());
					mark("drilling");
				}
				RigSubstructureBlockEntity.Phase phase = rig.phase();
				String text = read(rigPos());
				var m = within.matcher(text);
				if (phase == RigSubstructureBlockEntity.Phase.KICK && m.find() && kickAt[0] < 0) {
					kickAt[0] = t;
					mark("KICK alarm");
				}
				if (phase == RigSubstructureBlockEntity.Phase.BLOWOUT && blowAt[0] < 0) {
					blowAt[0] = t;
					mark("BLOWOUT");
				}
				if (blowAt[0] >= 0) {
					card("!BLOWOUT", "Flowing wild at the rig floor", "Nobody shut it in", rig.onFire() ? "!ON FIRE" : "No fire near: not lit");
				} else if (kickAt[0] >= 0) {
					var left = within.matcher(text);
					card("!KICK!", "Pit gain +" + f2(rig.shownGain()) + " m3", left.find() ? "Shut in within " + left.group(1) + " s" : "Shut it in", "Mud " + f1(rig.mudPpg()) + " ppg");
				} else {
					card("^DRILLING", "Mud " + f1(rig.mudPpg()) + " ppg", rig.depthNow() + " m down");
				}
				return blowAt[0] < 0 || t < blowAt[0] + 220;
			});
			if (blowAt[0] < 0) {
				throw new IllegalStateException("no blowout: " + read(rigPos()));
			}
			// 2: close on the rig floor: the spray
			film(2, "floor", new Cam(22.0, 11.5, 7.5, 30.0, 10.0, 0.0), new Cam(23.0, 11.0, 6.5, 30.0, 10.5, 0.0), 300, until(t ->
					card("!BLOWOUT", "Mud, oil and gas", "out of the rotary table", rig.onFire() ? "!ON FIRE" : "Not lit")));
			// 3: the whole rig, the jet over the floor
			film(3, "wide", new Cam(8.0, 8.0, 22.0, 30.0, 12.0, 0.0), new Cam(11.0, 9.0, 19.0, 30.0, 12.0, 0.0), 300, until(t ->
					card("!BLOWOUT", "The field flowing wild", "Close the BOP if it stands,", "or drill a relief well")));
			card();
			close();
			log("blowout ok: " + read(rigPos()));
		} finally {
			FilmCard.narrow = false;
			still = 0;
		}
	}

	/**
	 * A fault that stays: the faults short's feeder made small, the yard breaker, its relay cabinet (50 at 40 A, 79 set to one shot, as a
	 * line's) and the line a few blocks long to a pole unit and its lamps. Grounding clamps closed onto the live cable at the breaker's
	 * terminal (a bolted fault, standing): the arc at the clamps, the breaker's flag green, 79 recloses after 2 s onto the fault (the flag red, the
	 * clamps arc again), it trips again and locks out. The lamps past the fault stay dark throughout: a bolted fault holds the line dead.
	 */
	void arcflash() {
		wide();
		guiWidth(480);
		FilmCard.narrow = true;
		still = 60;
		try {
			site(10400, 600, new int[] {-10, 40, -16, 20}, 12, new int[] {0, 30, -8, 10}, 3);
			pad(-2, -10, 32, 12, "minecraft:grass_block");
			BlockPos set = A(8, 1, 0);
			genset(set, NORTH, false);
			BlockPos out = inlineTransformer(WildcatterBlocks.PADMOUNT_480, set, Direction.EAST, true);
			for (int x = out.getX() - ox; x <= 14; x++) {
				put(x, 1, 0, "wildcatter:cable_dist");
			}
			BlockPos breaker = A(15, 1, -1), cabinet = A(15, 1, 2);
			placeBig(WildcatterBlocks.DIST_BREAKER_3P, breaker, NORTH);
			placeBig(WildcatterBlocks.BREAKER_RELAY_CABINET, cabinet, NORTH);
			// the line: east from the breaker's terminal to a pole unit and its lamps; the clamps go on it right beside the breaker
			put(17, 1, 0, "wildcatter:cable_dist");
			put(18, 1, 0, "wildcatter:cable_dist");
			put(19, 1, 0, "wildcatter:pole_transformer[facing=west]");
			BlockPos[] lamps = {A(20, 1, 0), A(19, 1, 1), A(19, 1, -1)};
			for (BlockPos l : lamps) {
				server.runCommand("setblock " + l.getX() + " " + l.getY() + " " + l.getZ() + " wildcatter:electric_lamp");
			}
			BlockPos fault = A(17, 1, 0);
			startGenset(set, NORTH, 3000);
			context.waitTicks(40);
			on(level -> {
				var cab = (dev.wildcatter.block.BreakerRelayCabinetBlockEntity) level.getBlockEntity(cabinet);
				cab.setPickup(5);
				cab.setShots(1);
			});
			context.waitTicks(100);
			server.runCommand("time set 6000");   // midday: everything reads; the arc is bright enough for it
			java.util.function.Supplier<String> state = () -> getS(srv -> {
				var level = srv.overworld();
				int lit = 0;
				for (BlockPos l : lamps) {
					var s = level.getBlockState(l);
					if (s.hasProperty(dev.wildcatter.block.LoadBlock.LIT) && s.getValue(dev.wildcatter.block.LoadBlock.LIT)) {
						lit++;
					}
				}
				var cab = (dev.wildcatter.block.BreakerRelayCabinetBlockEntity) level.getBlockEntity(cabinet);
				return level.getBlockState(breaker).getValue(dev.wildcatter.block.BreakerBlock.POSITION).getSerializedName() + "|" + lit + "|" + cab.shotsUsed() + "|"
						+ cab.lockedOut() + "|" + cab.hasTripped() + "|" + cab.lastTrip();
			});
			log("arcflash ready: " + read(cabinet) + " | " + state.get());
			showCard();
			String[] last = {""};
			java.util.regex.Pattern amps = java.util.regex.Pattern.compile("(\\d+) A");
			// the card's lines kept short, so it stays as large through the film as in its first second
			IntConsumer readout = t -> {
				String now = state.get();
				if (!now.equals(last[0])) {
					mark(now);
					last[0] = now;
				}
				String[] f = now.split("\\|", -1);
				var m = java.util.regex.Pattern.compile("recloses in (\\d+) s").matcher(read(cabinet));
				var a = amps.matcher(f[5]);
				String relay = f[3].equals("true") ? "!LOCKED OUT" : m.find() ? "^RECLOSE IN " + m.group(1) + " s" : f[4].equals("true") ? "!TRIPPED: 50" : "+Relay healthy";
				card("^FEEDER BREAKER", f[0].equals("on") ? "Breaker CLOSED" : "Breaker OPEN", relay, a.find() ? "!Fault " + a.group(1) + " A" : "No fault",
						"Lamps lit " + f[1] + " of " + lamps.length);
			};
			// 1: close, the clamps at the breaker's terminal in the middle of the frame, its flag above and left
			film(1, "reclose", new Cam(17.4, 1.9, -5.4, 17.3, 3.3, 0.5), new Cam(17.4, 1.85, -5.2, 17.3, 3.3, 0.5), 400, t -> {
				if (t == 70) {
					onS(srv -> ((dev.wildcatter.block.MultiCableBlockEntity) srv.overworld().getBlockEntity(fault)).setGrounded(true));
					mark("fault: grounding clamps on the live cable");
				}
				readout.accept(t);
				return true;
			});
			// 2: locked out, pulling back over the bench (for the end card)
			film(2, "locked_out", new Cam(17.4, 1.9, -5.4, 17.3, 3.3, 0.5), new Cam(19.0, 4.5, -11.0, 17.3, 4.2, 0.5), 240, until(readout));
			card();
			close();
			log("arcflash ok: " + read(cabinet) + " | " + state.get());
		} finally {
			FilmCard.narrow = false;
			still = 0;
		}
	}

	// ---- shorts batch 4 (2026-11-21): turbine, cascade -----------------------------------------------------------------------------

	/**
	 * A gas turbine start onto a live bus: two peakers (aeroderivatives, ChemicalScenes.peaker) whose 138 kV sides are tied. Unit B is
	 * already on line (its start skipped by hook) carrying two load banks; unit A starts for real: its starter cranks it to a quarter
	 * speed for the 30 s purge, it lights off (the light-off puff out of its stack), runs up to full speed in 60 s, and its generator's
	 * synchronizer slips it into phase with the live bus and closes its breaker; then a load bank on its own bus steps on and it picks
	 * up load at its ramp rate. Its synchronizer is put 150 degrees out of phase by hook as it reaches speed (the game starts the slip at
	 * 0 degrees, so it would close at once); from there the sweep and the close are the synchronizer's own.
	 */
	void turbine() {
		wide();
		guiWidth(480);
		FilmCard.narrow = true;
		still = 60;
		try {
			site(11200, 600, new int[] {-50, 56, -30, 28}, 14, new int[] {-44, 50, -22, 22}, 5);
			pad(-44, -22, 50, 22, "minecraft:grass_block");
			int px = -45, pz = -5, qx = px + 44;
			BlockPos stationA = peaker(px, pz);
			BlockPos stationB = peaker(qx, pz);
			for (int x = px + 36; x <= qx + 36; x++) {
				put(x, 7, pz - 10, "wildcatter:cable_hv");   // the two units' 138 kV tied
			}
			BlockPos bankA = A(px + 29, 1, pz + 11), bankB1 = A(qx + 29, 1, pz + 11), bankB2 = A(qx + 32, 1, pz + 11);
			placeBig(WildcatterBlocks.LOAD_BANK, bankA, NORTH);
			placeBig(WildcatterBlocks.LOAD_BANK, bankB1, NORTH);
			placeBig(WildcatterBlocks.LOAD_BANK, bankB2, NORTH);
			startPeaker(qx, pz, stationB);
			onS(srv -> {
				((LoadBankBlockEntity) BigParts.entity(srv.overworld(), bankB1)).setSteps(Tuning.LOAD_BANK_STEPS);
				((LoadBankBlockEntity) BigParts.entity(srv.overworld(), bankB2)).setSteps(Tuning.LOAD_BANK_STEPS);
			});
			startGenset(stationA, NORTH, 3000);
			context.waitTicks(200);
			GasTurbineBlockEntity gt = at(A(px + 20, 1, pz), GasTurbineBlockEntity.class);
			GasTurbineBlockEntity other = at(A(qx + 20, 1, pz), GasTurbineBlockEntity.class);
			var gen = getS(srv -> gt.generatorPart());
			var genB = getS(srv -> other.generatorPart());
			var sync = gen.synchronizer();
			double nominal = ElectricalStandard.active().hertz();
			int poles = 2;   // the aeroderivative's generator (docs/multiblocks/aeroderivative.md)
			log("turbine: unit B " + read(A(qx + 20, 1, pz)) + " | gen B " + read(A(qx + 25, 1, pz + 1)) + " | unit A " + read(A(px + 20, 1, pz)) + " | station A " + read(stationA));
			int[] closedAt = {-1}, litAt = {-1};
			boolean[] offset = {false};
			java.util.function.IntFunction<String[]> readout = t -> {
				double sp = gt.speed();
				String phase = switch (gt.phase()) {
					case STOPPED -> "!STOPPED";
					case PURGE -> "^CRANKING: PURGE";
					case RUNUP -> litAt[0] >= 0 && t >= litAt[0] && t - litAt[0] < 60 ? "!LIGHT-OFF" : "^RUN-UP";
					case RUNNING -> gen.breakerClosed() ? "+ON LINE" : "^SYNCHRONIZING";
				};
				double genHz = gt.phase() == GasTurbineBlockEntity.Phase.RUNNING ? (gen.breakerClosed() ? gen.hertz() : sync.ownHertz()) : sp * nominal;
				String hz = "Gen " + f2(genHz) + " Hz  Bus " + f2(genB.hertz()) + " Hz";
				String third = gt.phase() == GasTurbineBlockEntity.Phase.RUNNING && !gen.breakerClosed()
						? "Phase " + f0(Math.abs(sync.slipDegrees())) + "° " + (sync.slipDegrees() < 0 ? "behind" : "ahead")
						: gen.breakerClosed() ? "+Breaker CLOSED" : "Breaker open";
				return new String[] {phase, "Speed " + f0(sp * 100) + " %  " + f0(sp * 120 * nominal / poles) + " rpm", "Exhaust heat " + f1(gt.exhaustWatts() / 1e6) + " MW", hz, third,
						"Output " + f1(Math.max(0, gen.deliveredWatts()) / 1e6) + " MW"};
			};
			java.util.function.IntConsumer watch = t -> {
				if (gt.phase() == GasTurbineBlockEntity.Phase.RUNUP && litAt[0] == -1) {
					litAt[0] = t;
					mark("LIGHT-OFF");
				}
				if (gt.phase() == GasTurbineBlockEntity.Phase.RUNNING && !offset[0]) {
					offset[0] = true;
					sync.setSlipForTest(-150);
					mark("full speed: synchronizing, 150 degrees behind");
				}
				if (gen.breakerClosed() && closedAt[0] < 0) {
					closedAt[0] = t;
					mark("BREAKER CLOSED: " + sync.lastClose());
				}
				card(readout.apply(t));
			};
			showCard();
			long[] pressed = {0};
			// 1: the start button: the starter cranks it to a quarter speed for the purge
			Cam a = new Cam(px + 7.0, 5.0, pz + 25.0, px + 22.0, 5.0, pz + 9.0), b = new Cam(px + 8.5, 5.0, pz + 23.0, px + 22.0, 5.0, pz + 9.0);
			film(1, "crank", a, b, 380, until(t -> {
				if (t == 60) {
					gt.setWantRun(true);
					pressed[0] = gt.getLevel().getGameTime();
					mark("START: wantRun");
				}
				watch.accept(t);
			}));
			// to 5 s before the light-off
			long purge = (long) (Tuning.AERO_PURGE_SECONDS * 20);
			waitUntil(() -> getS(srv -> srv.overworld().getGameTime()) - pressed[0] >= purge - 160, 2000);
			// 2: light-off, the puff out of the stack; the run-up begins
			litAt[0] = -1;
			Cam c = new Cam(px + 10.0, 9.0, pz + 31.0, px + 22.0, 13.0, pz + 16.0), d = new Cam(px + 11.0, 9.5, pz + 29.5, px + 22.0, 13.5, pz + 16.0);
			film(2, "light_off", c, d, 420, t -> {
				watch.accept(t);
				return litAt[0] < 0 || t < litAt[0] + 200;
			});
			if (litAt[0] < 0) {
				throw new IllegalStateException("no light-off: " + read(A(px + 20, 1, pz)));
			}
			litAt[0] = Integer.MAX_VALUE / 2;   // the light-off is behind us: no more LIGHT-OFF on the card
			// to 92 % speed
			waitUntil(() -> getS(srv -> gt.speed()) >= 0.92, 2400);
			// 3: to full speed; the synchronizer brings it into phase and closes; a load bank steps on its bus, it picks up load
			closedAt[0] = -1;
			Cam e = new Cam(px + 36.0, 5.5, pz + 20.0, px + 26.0, 2.0, pz + 7.0), g = new Cam(px + 34.5, 5.0, pz + 18.0, px + 26.0, 2.0, pz + 7.0);
			film(3, "sync", e, g, 1200, t -> {
				watch.accept(t);
				if (closedAt[0] >= 0 && t == closedAt[0] + 40) {
					((LoadBankBlockEntity) BigParts.entity(gt.getLevel(), bankA)).setSteps(Tuning.LOAD_BANK_STEPS);
					mark("load bank on its bus: " + Tuning.LOAD_BANK_STEPS + " MW");
				}
				return closedAt[0] < 0 || t < closedAt[0] + 340;
			});
			if (closedAt[0] < 0) {
				throw new IllegalStateException("never closed: " + read(A(px + 25, 1, pz + 1)));
			}
			// 4: on line, pulling back over both units
			film(4, "on_line", new Cam(px + 14.0, 8.0, pz + 30.0, px + 30.0, 3.0, pz + 6.0), new Cam(px + 6.0, 16.0, pz + 42.0, px + 40.0, 3.0, pz + 4.0), 300, until(watch));
			card();
			close();
			log("turbine ok: " + read(A(px + 20, 1, pz)) + " | gen " + read(A(px + 25, 1, pz + 1)) + " | gen B " + read(A(qx + 25, 1, pz + 1)));
		} finally {
			FilmCard.narrow = false;
			still = 0;
		}
	}

	/**
	 * The centrifuge cascade (WildcatterClientGameTest.cascade's layout, docs/multiblocks/enrichment.md): the peaker's 12.47 kV to the
	 * frequency converter, eleven machines on the header, the feed station with a natural UF6 48Y, the product and tails take-offs.
	 * The stage readout is the cascade's own numbers: eleven stages, each raising the abundance ratio by the same factor from the tails'
	 * 0.25 % to the product's 4.95 %.
	 */
	void cascade() {
		wide();
		guiWidth(480);
		FilmCard.narrow = true;
		still = 60;
		try {
			site(12000, 600, new int[] {-36, 50, -46, 28}, 14, new int[] {-32, 46, -42, 24}, 5);
			pad(-32, -42, 46, 24, "minecraft:grass_block");
			int zg = -30;
			BlockPos station = peaker(0, 0);
			// 12.47 kV: the substation transformer's throat, east, north up x 40, west along -38 and down to the converter's box
			List<int[]> mv = new ArrayList<>();
			for (int x = 36; x <= 40; x++) {
				mv.add(new int[] {x, -5});
			}
			for (int z = -38; z <= -6; z++) {
				mv.add(new int[] {40, z});
			}
			for (int x = -17; x <= 39; x++) {
				mv.add(new int[] {x, -38});
			}
			mv.add(new int[] {-17, -37});
			mv.add(new int[] {-17, -36});
			for (int[] m : mv) {
				put(m[0], 1, m[1], "wildcatter:cable_dist");
			}
			sizeCables(A(-17, 1, -38), A(40, 1, -5), WildcatterBlocks.CABLE_DIST, dev.wildcatter.energy.ConductorSize.MV_500);
			BlockPos feed = A(-20, 1, zg - 5), converter = A(-16, 1, zg - 6), product = A(-12, 1, zg + 1), tails = A(-8, 1, zg + 1);
			placeBig(WildcatterBlocks.FEED_STATION, feed, NORTH);
			placeBig(WildcatterBlocks.FREQUENCY_CONVERTER, converter, NORTH);
			placeBig(WildcatterBlocks.PRODUCT_TAKEOFF, product, NORTH);
			placeBig(WildcatterBlocks.TAILS_TAKEOFF, tails, NORTH);
			int machines = 11;
			for (int x = 0; x < machines; x++) {
				placeBig(WildcatterBlocks.CENTRIFUGE, A(x, 1, zg), NORTH);
			}
			for (int x = -20; x <= -1; x++) {
				put(x, 1, zg, "wildcatter:gas_line");
			}
			for (int k = 2; k <= 7; k++) {
				put(-1, k, zg, "wildcatter:gas_line");
			}
			for (int x = 0; x <= machines; x++) {
				put(x, 7, zg, "wildcatter:gas_line");
			}
			put(-19, 4, zg - 3, "minecraft:hopper");
			put(-19, 0, zg - 3, "minecraft:hopper");
			put(-11, 4, zg + 2, "minecraft:hopper");
			put(-11, 0, zg + 2, "minecraft:hopper");
			put(-7, 4, zg + 2, "minecraft:hopper");
			put(-7, 0, zg + 2, "minecraft:hopper");
			BlockPos hop = A(-11, 4, zg + 2), hop2 = A(-7, 4, zg + 2);
			server.runCommand("item replace block " + hop.getX() + " " + hop.getY() + " " + hop.getZ() + " container.0 with wildcatter:empty_30b_cylinder 2");
			server.runCommand("item replace block " + hop2.getX() + " " + hop2.getY() + " " + hop2.getZ() + " container.0 with wildcatter:empty_48y_cylinder 2");
			// 480 V for the stations: a set south-west, up x -23 to the feed station's box, east along zg+6 to the take-offs' boxes
			BlockPos lvSet = A(-24, 1, zg + 4);
			genset(lvSet, NORTH, false);
			List<int[]> lv = new ArrayList<>();
			for (int z = zg - 5; z <= zg + 6; z++) {
				lv.add(new int[] {-23, z});
			}
			lv.add(new int[] {-22, zg - 5});
			lv.add(new int[] {-21, zg - 5});
			for (int x = -22; x <= -9; x++) {
				lv.add(new int[] {x, zg + 6});
			}
			for (int z = zg + 1; z <= zg + 5; z++) {
				lv.add(new int[] {-13, z});
				lv.add(new int[] {-9, z});
			}
			for (int[] m : lv) {
				put(m[0], 1, m[1], "wildcatter:cable_industrial");
			}
			startPeaker(0, 0, station);
			startGenset(lvSet, NORTH, 3000);
			context.waitTicks(200);
			log("cascade before feed: " + read(feed) + " | converter " + read(converter) + " | machine " + read(A(5, 1, zg)));
			dev.wildcatter.block.FeedStationBlockEntity fs = at(feed, dev.wildcatter.block.FeedStationBlockEntity.class);
			dev.wildcatter.block.TakeoffBlockEntity pt = at(product, dev.wildcatter.block.TakeoffBlockEntity.class);
			java.util.regex.Pattern feeding = java.util.regex.Pattern.compile("feeding ([0-9,]+) kg of UF6 an hour; ([0-9,]+) kg left");
			java.util.regex.Pattern made = java.util.regex.Pattern.compile("([0-9.]+) kg of ([0-9.]+) % product an hour");
			java.util.regex.Pattern hz = java.util.regex.Pattern.compile("machines at ([0-9,]+) Hz");
			java.util.regex.Pattern kw = java.util.regex.Pattern.compile("([0-9,]+) kW of");
			showCard();
			// 1: the feed goes in: a natural UF6 48Y into the hopper on the feed station; it loads and starts feeding
			BlockPos top = A(-19, 4, zg - 3);
			boolean[] fed = {false};
			film(1, "feed", new Cam(-26.0, 6.0, zg + 4.0, -18.5, 2.5, zg - 3.0), new Cam(-24.5, 5.5, zg + 2.5, -18.5, 2.5, zg - 3.0), 340, until(t -> {
				if (t == 60) {
					((net.minecraft.world.level.block.entity.HopperBlockEntity) fs.getLevel().getBlockEntity(top)).setItem(0, new ItemStack(WildcatterItems.UF6_CYLINDER_48Y, 1));
					mark("48Y of natural UF6 into the hopper");
				}
				String r = fs.inspect().getString();
				var m = feeding.matcher(r);
				if (m.find() && !fed[0]) {
					fed[0] = true;
					mark("feeding: " + r);
				}
				if (t < 60) {
					card("^FEED STATION", "Natural uranium as UF6", "0.71 % U-235", "Waiting for a cylinder");
				} else if (!fed[0]) {
					card("^FEED: A 48Y CYLINDER", "12,501 kg of natural UF6", "0.71 % U-235", "Heating it to sublime");
				} else {
					card("+FEEDING THE CASCADE", "UF6 " + m.group(1) + " kg an hour", "0.71 % U-235 in", "In the 48Y: " + m.group(2) + " kg");
				}
			}));
			log("cascade fed: " + read(feed) + " | converter " + read(converter));
			// 2: the hall: eleven machines on the header, driven at 1,225 Hz
			film(2, "hall", new Cam(15.0, 3.0, zg + 7.0, 4.0, 3.5, zg - 2.0), new Cam(13.0, 4.0, zg + 5.5, 3.0, 4.0, zg - 2.0), 300, until(t -> {
				String cv = read(converter);
				var h = hz.matcher(cv);
				var k = kw.matcher(cv);
				card("^CENTRIFUGE HALL", machines + " machines, " + (h.find() ? h.group(1) : f0(Tuning.CENTRIFUGE_HERTZ)) + " Hz", "Rotors at " + f0(Tuning.CENTRIFUGE_TIP_SPEED) + " m/s",
						"Drive " + (k.find() ? f1(Double.parseDouble(k.group(1).replace(",", "")) / 1000) : "?") + " MW");
			}));
			// 3: along the cascade, stage by stage: the U-235 share climbing from the tails' to the product's
			double rt = Tuning.ASSAY_TAILS / (1 - Tuning.ASSAY_TAILS), rp = Tuning.ASSAY_PRODUCT / (1 - Tuning.ASSAY_PRODUCT);
			int stages = dev.wildcatter.fluid.Enrichment.stages();
			int[] shown = {0};
			Cam s0 = new Cam(0.5, 3.4, zg + 4.6, 0.5, 3.2, zg + 0.5), s1 = new Cam(machines - 0.5, 3.4, zg + 4.6, machines - 0.5, 3.2, zg + 0.5);
			film(3, "stages", s0, s1, 520, until(t -> {
				double f = Math.max(0, Math.min(1, (t - 60) / 400.0));
				double eased = f * f * (3 - 2 * f);
				int stage = Math.max(1, Math.min(stages, (int) Math.round(eased * (stages - 1)) + 1));   // the machine mid-frame
				double r = rt * Math.pow(rp / rt, stage / (double) stages);
				double x = r / (1 + r);
				if (stage != shown[0]) {
					shown[0] = stage;
					mark("stage " + stage + ": " + f2(x * 100) + " %");
				}
				card("^STAGE " + stage + " OF " + stages, (stage == stages ? "+" : "") + "U-235: " + f2(x * 100) + " %", "Each stage enriches", "a little more");
			}));
			// 4: the product take-off: low-enriched uranium into a 30B, for reactor fuel
			film(4, "product", new Cam(-17.0, 4.5, zg + 10.0, -10.0, 2.0, zg + 4.5), new Cam(-15.5, 4.0, zg + 8.5, -10.0, 2.0, zg + 4.5), 300, until(t -> {
				var m = made.matcher(fs.inspect().getString());
				boolean ok = m.find();
				card("^PRODUCT: LOW-ENRICHED", "+" + (ok ? m.group(2) : f2(Tuning.ASSAY_PRODUCT * 100)) + " % U-235", "UF6 " + (ok ? m.group(1) : "?") + " kg an hour",
						"into a 30B cylinder", "Fuel for the reactor");
			}));
			card();
			close();
			log("cascade ok: " + read(feed) + " | converter " + read(converter) + " | product " + read(product) + " | tails " + read(tails));
		} finally {
			FilmCard.narrow = false;
			still = 0;
		}
	}
}
