package dev.wildcatter.gametest;

import dev.wildcatter.WildcatterItems;
import dev.wildcatter.client.quest.QuestScreen;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Built From Scratch, episode 3 "Strike oil": the step-up quest (Three-phase power) and the Oil and gas chapter played through in survival,
 * in the series' world. The world is new (seed {@link Bfs01Films#SEED}); episode 1's base is rebuilt as episode 2 rebuilt it, then episode
 * 2's plant is set at the same block positions (PLAN.md "Episode 2's base"), both chapters done with the same rewards claimed and the same
 * pocket. Then everything the player does goes through the client: the destroy loop, useItemOn, container clicks with a drawn cursor, the
 * seismograph used as a right-click. What is gathered off camera is given by command and logged, so the edit can label it.
 * <p>
 * The survey is the game's own: each reading is the seismograph's report at the spot the player stands (logged with it). The fields were
 * found off camera first (videos/footage/films-bfs03/logs/scout*.log): a deep gas field under the base's south-west (too deep for the small
 * rig), a shallow gas field across the river to the east (-471, 978) and a shallow oil field in the plains to the north-west (-677.5, 865).
 * The rig at each field is powered by a second steam set and the dry-type, against its terminal box; the ground at each site is levelled
 * and its trees cleared off camera.
 * <p>
 * Film only: names that carry a standard's body or a voltage not read from the standard are shown without them (labels()); the game's
 * action-bar readouts, which run off the screen's edges when long, are drawn wrapped and held for a few seconds (readoutHook()), except in
 * the GAME_READOUT films, re-shot once the game wrapped them itself. Films land
 * in {@code build/scene-videos/bfs03__NN_<shot>.mov}; BFS_SHOTS picks shots by number or name (the rest run unfilmed).
 */
final class Bfs03Episode {
	private final Bfs03Films t;
	private final ClientGameTestContext context;
	private final TestServerContext server;
	private final Set<String> shots;
	private int bx, by, bz;
	private BlockPos table, furnace, blast, campfire;
	private boolean filming;
	private String filmId = "";
	private long filmStart;
	/** Episode 2's plant (PLAN.md "Episode 2's base"). */
	private BlockPos boiler, turbine, lamp;

	Bfs03Episode(Bfs03Films t) {
		this.t = t;
		this.context = t.context;
		this.server = t.server;
		String pick = System.getenv("BFS_SHOTS");
		shots = pick == null || pick.isBlank() ? null : Set.of(pick.split(","));
	}

	static void log(String s) {
		System.out.println("[bfs03] " + s);
	}

	private boolean wanted(int n) {
		return shots == null || shots.contains(String.valueOf(n));
	}

	private boolean wanted(int n, String name) {
		return wanted(n) || shots != null && shots.contains(name);
	}

	private static Item item(String id) {
		return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
	}

	// ---- the run ----------------------------------------------------------------------------------------------------------------------

	void run() {
		server.runCommand("gamerule advance_time false");
		server.runCommand("gamerule advance_weather false");
		server.runCommand("gamerule spawn_mobs false");
		server.runCommand("gamerule log_admin_commands false");
		server.runCommand("gamerule show_death_messages false");
		server.runCommand("weather clear");
		server.runCommand("difficulty peaceful");
		server.runCommand("time set 1000");
		context.runOnClient(client -> {
			var o = client.options;
			o.applyGraphicsPreset(net.minecraft.client.GraphicsPreset.FANCY);
			o.renderDistance().set(12);
			o.particles().set(net.minecraft.server.level.ParticleStatus.ALL);
			o.bobView().set(false);
			o.fov().set(70);
			o.cloudStatus().set(net.minecraft.client.CloudStatus.FANCY);
		});
		cursorHook();
		readoutHook();
		labels();
		bx = -568;
		bz = 940;
		server.runCommand("gamemode spectator @p");
		server.runCommand("forceload add " + (bx - 80) + " " + (bz - 60) + " " + (bx + 60) + " " + (bz + 60));
		server.runCommand("tp @p " + bx + " 120 " + bz);
		for (int i = 0; i < 60 && server.computeOnServer(srv -> srv.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz)) < 0; i++) {
			context.waitTicks(10);
		}
		context.waitTicks(60);
		by = server.computeOnServer(srv -> srv.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz)) - 1;
		table = new BlockPos(bx, by + 1, bz);
		furnace = new BlockPos(bx + 2, by + 1, bz);
		blast = new BlockPos(bx + 3, by + 1, bz);
		campfire = new BlockPos(bx + 1, by + 1, bz + 5);
		boiler = campfire.above();
		turbine = new BlockPos(-566, by + 2, 945);
		lamp = new BlockPos(-562, by + 1, 945);
		log("base at " + bx + " " + by + " " + bz + "; ground " + state(new BlockPos(bx, by, bz)) + "; low tier " + Voltage.LOW.label() + ", industrial tier "
				+ Voltage.INDUSTRIAL.label());
		rebuildDayOne();
		rebuildEpisodeTwo();
		String[] steps = {"base", "stepup", "seismograph", "survey", "rig", "gasfield", "oilsurvey", "oilfield", "pumpjack", "lease", "crude", "done"};
		for (int i = 0; i < steps.length; i++) {
			int n = i + 1;
			log("step " + n + " " + steps[i]);
			try {
				switch (steps[i]) {
					case "base" -> base(n);
					case "stepup" -> stepUp(n);
					case "seismograph" -> seismograph(n);
					case "survey" -> survey(n);
					case "rig" -> rig(n);
					case "gasfield" -> gasField(n);
					case "oilsurvey" -> oilSurvey(n);
					case "oilfield" -> oilField(n);
					case "pumpjack" -> pumpjack(n);
					case "lease" -> lease(n);
					case "crude" -> crude(n);
					case "done" -> done(n);
					default -> {
					}
				}
			} catch (Throwable e) {
				log("step " + steps[i] + " FAILED: " + e);
				e.printStackTrace();
				stopFilm();
				context.setScreen(() -> null);
				break;
			}
		}
		log("quests done: " + questsDone());
		log("final inventory: " + inventory());
	}

	/**
	 * Film only. The receptacle and plug adapter named without their standard's name (as episode 2); the pumpjack without its API size
	 * (a standard body's name); the dry-type with its high side's label read from the standard (the name written in the game says the
	 * North American figure whatever the standard).
	 */
	private void labels() {
		double amps = dev.wildcatter.energy.PlugType.NEMA_5_15.amps();
		String tag = " (" + Voltage.LOW.label() + ", " + Math.round(amps) + " A)";
		Map<String, String> over = Map.of("block.wildcatter.receptacle_nema_5_15", "Receptacle" + tag, "block.wildcatter.plug_nema_5_15", "Plug Adapter" + tag,
				"block.wildcatter.pumpjack", "Pumpjack (beam pumping unit, 10 hp)",
				"block.wildcatter.dry_type_transformer", "Dry-Type Transformer (LV / " + Voltage.INDUSTRIAL.label() + ", 75 kVA)");
		context.runOnClient(c -> {
			net.minecraft.locale.Language base = net.minecraft.locale.Language.getInstance();
			net.minecraft.locale.Language.inject(new net.minecraft.locale.Language() {
				@Override
				public String getOrDefault(String key, String fallback) {
					String v = over.get(key);
					return v != null ? v : base.getOrDefault(key, fallback);
				}

				@Override
				public boolean has(String key) {
					return over.containsKey(key) || base.has(key);
				}

				@Override
				public boolean isDefaultRightToLeft() {
					return base.isDefaultRightToLeft();
				}

				@Override
				public net.minecraft.util.FormattedCharSequence getVisualOrder(net.minecraft.network.chat.FormattedText text) {
					return base.getVisualOrder(text);
				}
			});
		});
		log("film labels: " + over);
	}

	// ---- the readout, wrapped and held (film only) ----------------------------------------------------------------------------------

	private static boolean readoutHooked;
	private static volatile net.minecraft.network.chat.Component heldReadout;
	private static volatile Object lastReadout;
	private static volatile long heldAt;
	/** How long a readout stays up: the game's own fades after three seconds, too short to read a long line on a film. */
	static final int READOUT_HOLD = 170;
	/**
	 * Films re-shot after the game learnt to wrap long readouts itself (master 486c7446, ReadoutOverlay): in these the readout is the
	 * game's own, not this film-only wrap. They are the shots whose rig or pumpjack readout changed to grounded (a rod against any cell of
	 * a big part grounds it), filmed again for that (BFS patch, take 3).
	 */
	static final Set<String> GAME_READOUT = Set.of("bfs03__06_power_place", "bfs03__09_jack_place", "bfs03__10_mistake");
	private static volatile boolean gameReadout;

	private static Object hudField(Object hud, String name) {
		try {
			java.lang.reflect.Field f = hud.getClass().getDeclaredField(name);
			f.setAccessible(true);
			return f.get(hud);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * The action bar's readout drawn in up to four centred lines, wrapped at 80 % of the screen's width, over a dark band, where the game
	 * draws it, and held READOUT_HOLD ticks. The words are the game's own, unchanged.
	 */
	private void readoutHook() {
		if (readoutHooked) {
			return;
		}
		readoutHooked = true;
		net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.replaceElement(
				net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.OVERLAY_MESSAGE, old -> (g, delta) -> {
					var mc = net.minecraft.client.Minecraft.getInstance();
					if (gameReadout) {
						old.extractRenderState(g, delta);   // the game's own readout (ReadoutOverlay)
						return;
					}
					if (mc.level == null) {
						return;
					}
					Object hud = mc.gui.hud;
					net.minecraft.network.chat.Component cur = (net.minecraft.network.chat.Component) hudField(hud, "overlayMessageString");
					int time = (Integer) hudField(hud, "overlayMessageTime");
					long now = mc.level.getGameTime();
					if (cur != null && time > 0 && cur != lastReadout) {
						lastReadout = cur;
						heldReadout = cur;
						heldAt = now;
					}
					net.minecraft.network.chat.Component msg = heldReadout;
					if (msg != null && msg.getString().contains(" API ")) {
						// film only, as the names: a standard body's name left out of a readout (the stock tank's "API 12F")
						msg = net.minecraft.network.chat.Component.literal(msg.getString().replaceAll(" API [0-9A-Z-]+", ""));
					}
					if (msg == null || msg.getString().isEmpty() || now - heldAt > READOUT_HOLD) {
						return;
					}
					long left = READOUT_HOLD - (now - heldAt);
					int alpha = left < 10 ? (int) (255 * left / 10.0) : 255;
					var font = mc.font;
					List<net.minecraft.util.FormattedCharSequence> lines = font.split(msg, (int) (g.guiWidth() * 0.8));
					int lh = font.lineHeight + 1;
					int bottom = g.guiHeight() - 64;
					int top = bottom - lines.size() * lh;
					int w = 0;
					for (var l : lines) {
						w = Math.max(w, font.width(l));
					}
					int x0 = (g.guiWidth() - w) / 2;
					g.nextStratum();
					g.fill(x0 - 4, top - 3, x0 + w + 4, bottom + 1, net.minecraft.util.ARGB.color((int) (alpha * 0.6), 0, 0, 0));
					for (int i = 0; i < lines.size(); i++) {
						g.text(font, lines.get(i), (g.guiWidth() - font.width(lines.get(i))) / 2, top + i * lh, net.minecraft.util.ARGB.white(alpha), true);
					}
				});
		log("readout hook: action-bar readouts wrapped and held " + READOUT_HOLD + " ticks (film only)");
	}

	/** No readout carried over from the last film. */
	private void clearReadout() {
		heldReadout = null;
		context.runOnClient(c -> c.gui.hud.setOverlayMessage(net.minecraft.network.chat.Component.empty(), false));
		context.waitTick();
		heldReadout = null;
	}

	// ---- filming ------------------------------------------------------------------------------------------------------------------------

	private Filming film;

	private void startFilm(int n, String name) {
		if (!wanted(n) && !(shots != null && shots.contains(name))) {
			return;
		}
		filmId = String.format(Locale.ROOT, "bfs03__%02d_%s", n, name);
		gameReadout = GAME_READOUT.contains(filmId);
		clearReadout();
		context.waitTicks(2);
		film = context.computeOnClient(client -> Filming.start(client, filmId));
		filmStart = System.nanoTime();
		filming = true;
		context.waitTicks(10);
	}

	private void stopFilm() {
		if (film != null) {
			context.waitTicks(10);
			film.stop();
			log("filmed " + filmId);
			film = null;
		}
		filming = false;
	}

	/** Logs a beat with its time from the film's start (the edit cuts to these). */
	private void mark(String what) {
		if (filming) {
			log(String.format(Locale.ROOT, "event %s %s at %.2f s", filmId, what, (System.nanoTime() - filmStart) / 1e9));
		} else {
			log("beat (unfilmed) " + what);
		}
	}

	private void hud(boolean shown) {
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() == shown) {
				client.gui.hud.toggle();
			}
		});
	}

	private void survival() {
		server.runCommand("gamemode survival @p");
		hud(true);
		context.waitTicks(5);
	}

	private void spectator() {
		server.runCommand("gamemode spectator @p");
		hud(false);
		context.waitTicks(5);
	}

	// ---- the drawn cursor ---------------------------------------------------------------------------------------------------------------

	static volatile boolean cursorOn;
	private static boolean hooked;

	/** A small arrow drawn over every screen at the mouse, as the film has no system cursor. */
	private void cursorHook() {
		if (hooked) {
			return;
		}
		hooked = true;
		net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) ->
				net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.afterExtract(screen).register((s, g, mx, my, partial) -> {
					if (!cursorOn) {
						return;
					}
					// an arrow 10 GUI pixels tall: black outline, white fill
					int[] w = {1, 2, 3, 4, 5, 6, 7, 4, 2, 1};
					for (int row = 0; row < w.length; row++) {
						g.fill(mx - 1, my + row - 1, mx + w[row] + 1, my + row + 2, 0xFF000000);
					}
					for (int row = 0; row < w.length; row++) {
						g.fill(mx, my + row, mx + w[row], my + row + 1, 0xFFFFFFFF);
					}
				}));
	}

	// ---- the player's hands -------------------------------------------------------------------------------------------------------------

	private ServerPlayer sp(net.minecraft.server.MinecraftServer srv) {
		return srv.getPlayerList().getPlayers().get(0);
	}

	/** Stands the player at a block's top centre (feet at y), looking at yaw/pitch. */
	private void stand(double x, double y, double z, float yaw, float pitch) {
		server.runCommand(String.format(Locale.ROOT, "tp @p %.3f %.3f %.3f %.2f %.2f", x, y, z, yaw, pitch));
		context.runOnClient(client -> {
			client.player.snapTo(x, y, z, yaw, pitch);
			client.player.setDeltaMovement(Vec3.ZERO);
		});
		context.waitTicks(3);
	}

	private void standAt(BlockPos feet, Vec3 lookAt) {
		Vec3 eye = Vec3.atBottomCenterOf(feet).add(0, 1.62, 0);
		float[] yp = yawPitch(eye, lookAt);
		stand(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, yp[0], yp[1]);
	}

	private static float[] yawPitch(Vec3 eye, Vec3 at) {
		double dx = at.x - eye.x, dy = at.y - eye.y, dz = at.z - eye.z;
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
		return new float[] {yaw, pitch};
	}

	/** Turns the player's head smoothly to look at a point over {@code ticks}. */
	private void face(Vec3 at, int ticks) {
		float[] from = context.computeOnClient(c -> new float[] {c.player.getYRot(), c.player.getXRot()});
		Vec3 eye = context.computeOnClient(c -> c.player.getEyePosition());
		float[] to = yawPitch(eye, at);
		float dyaw = to[0] - from[0];
		while (dyaw > 180) {
			dyaw -= 360;
		}
		while (dyaw < -180) {
			dyaw += 360;
		}
		// no turn faster than about 150 degrees a second, eased in and out
		ticks = Math.max(ticks, (int) Math.ceil(Math.max(Math.abs(dyaw), Math.abs(to[1] - from[1])) / 7.5));
		for (int i = 1; i <= ticks; i++) {
			double s = (double) i / ticks;
			s = s * s * (3 - 2 * s);
			float yaw = (float) (from[0] + dyaw * s), pitch = (float) (from[1] + (to[1] - from[1]) * s);
			context.runOnClient(c -> {
				c.player.setYRot(yaw);
				c.player.setXRot(pitch);
				c.player.setYHeadRot(yaw);
			});
			context.waitTick();
		}
	}

	private Direction faceToward(BlockPos pos) {
		Vec3 eye = context.computeOnClient(c -> c.player.getEyePosition());
		Vec3 d = eye.subtract(Vec3.atCenterOf(pos));
		return Direction.getApproximateNearest(d.x, d.y, d.z);
	}

	private boolean air(BlockPos p) {
		return context.computeOnClient(c -> c.level.getBlockState(p).isAir());
	}

	/** Breaks a block as holding the attack button does: the client's destroy loop each tick, the arm swinging. */
	private boolean dig(BlockPos pos, int max) {
		Direction side = faceToward(pos);
		face(Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(side.getUnitVec3i()).scale(0.45)), 8);
		context.runOnClient(c -> {
			c.gameMode.startDestroyBlock(pos, side);
			c.player.swing(InteractionHand.MAIN_HAND, c.player.getMainHandItem().getAttackAnimation(), false);
		});
		for (int i = 0; i < max; i++) {
			context.waitTick();
			if (air(pos)) {
				break;
			}
			context.runOnClient(c -> {
				if (c.gameMode.continueDestroyBlock(pos, side)) {
					c.player.swing(InteractionHand.MAIN_HAND, c.player.getMainHandItem().getAttackAnimation(), false);
				}
			});
		}
		boolean broke = air(pos);
		context.runOnClient(c -> c.gameMode.stopDestroyBlock());
		context.waitTicks(14);
		if (broke) {
			collect();
		}
		return broke;
	}

	/** A drop left lying within a few blocks: the player steps over to it (on the same level, if the ground allows) and back. */
	private void collect() {
		Vec3 drop = context.computeOnClient(c -> {
			var items = c.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, c.player.getBoundingBox().inflate(4, 3, 4));
			return items.isEmpty() ? null : items.get(0).position();
		});
		if (drop == null) {
			return;
		}
		Vec3 home = context.computeOnClient(c -> c.player.position());
		BlockPos cell = BlockPos.containing(drop.x, home.y + 0.1, drop.z);
		if (!clear(cell) || !clear(cell.above()) || !solid(cell.below())) {
			log("a drop at " + drop + " out of reach");
			return;
		}
		Vec3 to = new Vec3(drop.x, home.y, drop.z);
		for (int i = 1; i <= 8; i++) {
			Vec3 p = home.lerp(to, i / 8.0);
			context.runOnClient(c -> c.player.setPos(p.x, p.y, p.z));
			context.waitTick();
		}
		context.waitTicks(6);
		for (int i = 1; i <= 8; i++) {
			Vec3 p = to.lerp(home, i / 8.0);
			context.runOnClient(c -> c.player.setPos(p.x, p.y, p.z));
			context.waitTick();
		}
		mark("stepped over for a drop");
	}

	/** Uses the held item on a block's face (places a block, opens a table or furnace), as a right-click does. */
	private void useOn(BlockPos pos, Direction side) {
		face(Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(side.getUnitVec3i()).scale(0.5)), 8);
		context.runOnClient(c -> {
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(side.getUnitVec3i()).scale(0.5)), side, pos, false);
			if (c.gameMode.useItemOn(c.player, InteractionHand.MAIN_HAND, hit).consumesAction()) {
				c.player.swing(InteractionHand.MAIN_HAND, c.player.getMainHandItem().getAttackAnimation(), false);
			}
		});
		context.waitTicks(6);
	}

	private void useHeld() {
		context.runOnClient(c -> c.gameMode.useItem(c.player, InteractionHand.MAIN_HAND));
		context.waitTicks(6);
	}

	private int hotbarOf(Item item) {
		return context.computeOnClient(c -> {
			for (int i = 0; i < 9; i++) {
				if (c.player.getInventory().getItem(i).is(item)) {
					return i;
				}
			}
			return -1;
		});
	}

	/** Picks the hotbar slot holding {@code item} with its number key. */
	private void hold(Item item) {
		int i = hotbarOf(item);
		if (i < 0) {
			// not in the hotbar: moved there from the pack (an empty hotbar slot, or the last one), as a player drags it down
			i = server.computeOnServer(srv -> {
				var inv = sp(srv).getInventory();
				int from = -1;
				for (int k = 9; k < 36; k++) {
					if (inv.getItem(k).is(item)) {
						from = k;
						break;
					}
				}
				if (from < 0) {
					return -1;
				}
				int to = 8;
				for (int k = 2; k < 9; k++) {
					if (inv.getItem(k).isEmpty()) {
						to = k;
						break;
					}
				}
				ItemStack a = inv.getItem(from).copy(), b = inv.getItem(to).copy();
				inv.setItem(to, a);
				inv.setItem(from, b);
				return to;
			});
			context.waitTicks(3);
		}
		if (i < 0) {
			throw new IllegalStateException("no " + item + " in the inventory: " + inventory());
		}
		final int slot = i;
		context.getInput().pressKey(o -> o.keyHotbarSlots[slot]);
		context.waitTicks(4);
	}

	private String inventory() {
		return context.computeOnClient(c -> {
			StringBuilder b = new StringBuilder();
			for (int i = 0; i < 36; i++) {
				ItemStack s = c.player.getInventory().getItem(i);
				if (!s.isEmpty()) {
					b.append(i).append(':').append(s.getCount()).append(' ').append(s.getItem()).append(", ");
				}
			}
			return b.toString();
		});
	}

	private int count(Item item) {
		return context.computeOnClient(c -> {
			int n = 0;
			for (int i = 0; i < 36; i++) {
				ItemStack s = c.player.getInventory().getItem(i);
				if (s.is(item)) {
					n += s.getCount();
				}
			}
			return n;
		});
	}

	/** What was gathered off camera, given by command, logged for the edit's label. */
	private void give(String item, int n, String why) {
		server.runCommand("give @p " + item + " " + n);
		log("given off camera: " + n + " " + item + " (" + why + ")");
		context.waitTicks(2);
	}

	// ---- screens --------------------------------------------------------------------------------------------------------------------------

	private static int field(AbstractContainerScreen<?> s, String name) {
		try {
			java.lang.reflect.Field f = AbstractContainerScreen.class.getDeclaredField(name);
			f.setAccessible(true);
			return f.getInt(s);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private double[] mouse = {0, 0};

	/** Moves the mouse (window coordinates) smoothly to a slot's centre. */
	private void moveTo(int slot) {
		double[] to = context.computeOnClient(c -> {
			AbstractContainerScreen<?> s = (AbstractContainerScreen<?>) c.gui.screen();
			Slot sl = s.getMenu().slots.get(slot);
			double gx = field(s, "leftPos") + sl.x + 8, gy = field(s, "topPos") + sl.y + 8;
			double k = (double) c.getWindow().getScreenWidth() / c.getWindow().getGuiScaledWidth();
			return new double[] {gx * k, gy * k};
		});
		glide(to, 7);
	}

	private void glide(double[] to, int ticks) {
		double[] from = mouse.clone();
		for (int i = 1; i <= ticks; i++) {
			double s = (double) i / ticks;
			s = s * s * (3 - 2 * s);
			context.getInput().setCursorPos(from[0] + (to[0] - from[0]) * s, from[1] + (to[1] - from[1]) * s);
			context.waitTick();
		}
		mouse = to;
	}

	private void centreMouse() {
		double[] mid = context.computeOnClient(c -> new double[] {c.getWindow().getScreenWidth() / 2.0, c.getWindow().getScreenHeight() / 2.0});
		context.getInput().setCursorPos(mid[0], mid[1]);
		mouse = mid;
	}

	private void click(int slot, int button, ContainerInput kind) {
		moveTo(slot);
		context.runOnClient(c -> {
			AbstractContainerMenu m = c.player.containerMenu;
			c.gameMode.handleContainerInput(m.containerId, slot, button, kind, c.player);
		});
		context.waitTicks(5);
	}

	private void left(int slot) {
		click(slot, 0, ContainerInput.PICKUP);
	}

	private void right(int slot) {
		click(slot, 1, ContainerInput.PICKUP);
	}

	private void shift(int slot) {
		click(slot, 0, ContainerInput.QUICK_MOVE);
	}

	/** The menu slot (from {@code from} on: the player's own slots) holding the most of {@code item}. */
	private int slotOf(Item item, int from) {
		return context.computeOnClient(c -> {
			AbstractContainerMenu m = c.player.containerMenu;
			int best = -1, most = 0;
			for (int i = from; i < m.slots.size(); i++) {
				ItemStack s = m.slots.get(i).getItem();
				if (s.is(item) && s.getCount() > most) {
					best = i;
					most = s.getCount();
				}
			}
			if (best < 0) {
				throw new IllegalStateException("no " + item + " in the open menu from slot " + from);
			}
			return best;
		});
	}

	/** An empty slot in the menu's hotbar row (its last nine slots before any offhand), or else any empty player slot. */
	private int emptySlot(int from, int hotbarStart) {
		return context.computeOnClient(c -> {
			AbstractContainerMenu m = c.player.containerMenu;
			for (int i = hotbarStart; i < hotbarStart + 9; i++) {
				if (m.slots.get(i).getItem().isEmpty()) {
					return i;
				}
			}
			for (int i = from; i < m.slots.size(); i++) {
				if (m.slots.get(i).getItem().isEmpty()) {
					return i;
				}
			}
			throw new IllegalStateException("no empty slot");
		});
	}

	private boolean carrying() {
		return context.computeOnClient(c -> !c.player.containerMenu.getCarried().isEmpty());
	}

	/**
	 * Lays a recipe in the open grid (slot 1 on, {@code width} wide) a cell at a time with right clicks, then takes the result {@code times}
	 * and drops it into the hotbar. {@code pattern} rows use the keys of {@code key}; spaces are empty cells. Returns where the result went.
	 */
	private int craft(int width, String[] pattern, Map<Character, Item> key, int times, int playerFrom, int hotbarStart) {
		for (Map.Entry<Character, Item> e : key.entrySet()) {
			int src = slotOf(e.getValue(), playerFrom);
			left(src);
			for (int r = 0; r < pattern.length; r++) {
				for (int col = 0; col < pattern[r].length(); col++) {
					if (pattern[r].charAt(col) == e.getKey()) {
						for (int k = 0; k < times; k++) {
							right(1 + r * width + col);
						}
					}
				}
			}
			if (carrying()) {
				left(src);
			}
		}
		context.waitTicks(4);
		for (int i = 0; i < times; i++) {
			left(0);
		}
		ItemStack made = context.computeOnClient(c -> c.player.containerMenu.getCarried().copy());
		int dest = emptySlot(playerFrom, hotbarStart);
		context.waitTicks(4);
		left(dest);
		log("crafted " + made.getCount() + " " + made.getItem() + " into slot " + dest);
		return dest;
	}

	private void closeScreen() {
		context.runOnClient(c -> {
			if (c.player.containerMenu != c.player.inventoryMenu) {
				c.player.closeContainer();
			} else {
				c.gui.setScreen(null);
			}
		});
		cursorOn = false;
		context.waitTicks(5);
	}

	private void openInventory() {
		context.runOnClient(c -> c.setScreenAndShow(new InventoryScreen(c.player)));
		context.waitTicks(4);
		centreMouse();
		cursorOn = true;
		context.waitTicks(6);
	}

	private void openBlock(BlockPos pos) {
		useOn(pos, faceToward(pos));
		context.waitTicks(4);
		boolean open = context.computeOnClient(c -> c.gui.screen() instanceof AbstractContainerScreen);
		if (!open) {
			throw new IllegalStateException("no screen opened at " + pos.toShortString());
		}
		centreMouse();
		cursorOn = true;
		context.waitTicks(6);
	}

	// ---- the quest book -------------------------------------------------------------------------------------------------------------------

	private String questsDone() {
		return server.computeOnServer(srv -> String.join(", ", dev.wildcatter.quest.QuestData.get(srv).of(sp(srv).getStringUUID()).done));
	}

	private boolean questDone(String id) {
		return server.computeOnServer(srv -> dev.wildcatter.quest.QuestData.get(srv).of(sp(srv).getStringUUID()).done.contains("wildcatter:" + id));
	}

	private void waitQuest(String id) {
		for (int i = 0; i < 60 && !questDone(id); i++) {
			context.waitTicks(5);
		}
		if (!questDone(id)) {
			throw new IllegalStateException("quest " + id + " did not tick: " + questsDone() + " | " + inventory());
		}
		mark("quest " + id + " done");
		context.waitTicks(25);   // the client's copy and the chat line
	}

	/** Opens the quest book from the hotbar on {@code quest}'s page and holds it. */
	private void book(String quest, int holdTicks) {
		hold(WildcatterItems.QUEST_BOOK);
		context.runOnClient(c -> c.gui.toastManager().clear());
		useHeld();
		context.waitTicks(6);
		context.runOnClient(c -> {
			if (c.gui.screen() instanceof QuestScreen q) {
				q.open(Identifier.parse("wildcatter:" + quest), true);
			}
		});
		mark("book on " + quest);
		context.waitTicks(holdTicks);
		context.setScreen(() -> null);
		context.waitTicks(4);
	}

	private void claim(String quest) {
		context.runOnClient(c -> dev.wildcatter.quest.QuestNet.claim(Identifier.parse("wildcatter:" + quest)));
		mark("claimed " + quest);
		context.waitTicks(20);
	}

	// ---- the world ------------------------------------------------------------------------------------------------------------------------

	private BlockState state(BlockPos p) {
		return server.computeOnServer(srv -> srv.overworld().getBlockState(p));
	}

	private boolean solid(BlockPos p) {
		return server.computeOnServer(srv -> srv.overworld().getBlockState(p).isFaceSturdy(srv.overworld(), p, Direction.UP));
	}

	private boolean clear(BlockPos p) {
		return server.computeOnServer(srv -> srv.overworld().getBlockState(p).getCollisionShape(srv.overworld(), p).isEmpty()
				&& srv.overworld().getFluidState(p).isEmpty());
	}

	/** A cell the player can stand in (solid under, two clear) within reach of {@code target} with a clear line to it, nearest to {@code near}. */
	private BlockPos standFor(BlockPos target, BlockPos near, double reach) {
		return server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos best = null;
			double bestD = 1e9;
			for (BlockPos p : BlockPos.betweenClosed(target.offset(-3, -3, -3), target.offset(3, 1, 3))) {
				if (!level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)
						|| !level.getBlockState(p).getCollisionShape(level, p).isEmpty() || !level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
						|| !level.getFluidState(p).isEmpty() || !level.getFluidState(p.above()).isEmpty()) {
					continue;
				}
				Vec3 eye = Vec3.atBottomCenterOf(p).add(0, 1.62, 0);
				Vec3 c = Vec3.atCenterOf(target);
				if (eye.distanceTo(c) > reach || eye.distanceTo(c) < 1.2) {
					continue;
				}
				var hit = level.clip(new net.minecraft.world.level.ClipContext(eye, c, net.minecraft.world.level.ClipContext.Block.OUTLINE,
						net.minecraft.world.level.ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
				if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK || !hit.getBlockPos().equals(target)) {
					continue;
				}
				if (p.equals(target.above())) {
					continue;   // never on top of it, looking down
				}
				// the same level as the block, so its drop falls where the player picks it up
				double d = p.distSqr(near) + Math.abs(p.getY() - target.getY()) * 40;
				if (d < bestD) {
					bestD = d;
					best = p.immutable();
				}
			}
			return best;
		});
	}

	/** A camera relative to the base corner (bx, by, bz). */
	private Bfs03Films.Cam cam(double x, double y, double z, double tx, double ty, double tz) {
		return new Bfs03Films.Cam(x, y, z, tx, ty, tz);
	}

	/** A spectator drone move between two cameras, HUD hidden. */
	private void drone(int n, String name, Bfs03Films.Cam a, Bfs03Films.Cam b, int ticks) {
		t.ox = bx;
		t.gy = by;
		t.oz = bz;
		t.camera(a, true);
		context.waitTicks(wanted(n) || shots != null && shots.contains(name) ? 120 : 2);
		t.camera(a, true);
		context.waitTicks(10);
		startFilm(n, name);
		for (int i = 0; i <= ticks; i++) {
			t.camera(a.lerp(b, (double) i / ticks), false);
			context.waitTick();
		}
		stopFilm();
	}

	// ---- 2: punch a tree ------------------------------------------------------------------------------------------------------------------

	private BlockPos trunk;

	/** A cell beside {@code trunk} to stand in (clear, two high, on solid ground), the side towards {@code toward} first. */
	private BlockPos besideTrunk(BlockPos trunk, BlockPos toward) {
		Direction d0 = Direction.getApproximateNearest(toward.getX() - trunk.getX(), 0, toward.getZ() - trunk.getZ());
		for (Direction d : new Direction[] {d0, d0.getClockWise(), d0.getCounterClockWise(), d0.getOpposite()}) {
			BlockPos f = trunk.relative(d);
			if (clear(f) && clear(f.above()) && solid(f.below())) {
				return f;
			}
		}
		return null;
	}

	/** Walks the player in a straight line to {@code to} (feet), a step a block, as a player walks on level ground. */
	private void walk(BlockPos to) {
		Vec3 from = context.computeOnClient(c -> c.player.position()), end = Vec3.atBottomCenterOf(to);
		int steps = (int) Math.max(1, from.distanceTo(end) * 5);
		for (int i = 1; i <= steps; i++) {
			Vec3 p = from.lerp(end, (double) i / steps);
			context.runOnClient(c -> c.player.setPos(p.x, p.y, p.z));
			context.waitTick();
		}
	}

	private boolean pathClear(BlockPos a, BlockPos b) {
		Vec3 va = Vec3.atBottomCenterOf(a), vb = Vec3.atBottomCenterOf(b);
		int n = (int) Math.ceil(va.distanceTo(vb) * 2);
		for (int i = 0; i <= n; i++) {
			Vec3 v = va.lerp(vb, (double) i / Math.max(1, n));
			BlockPos c = BlockPos.containing(v.x, a.getY() + 0.1, v.z);
			if (!clear(c) || !clear(c.above()) || !solid(c.below())) {
				return false;
			}
		}
		return true;
	}

	private List<BlockPos> stoneNear(BlockPos feet, int max) {
		return server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			Vec3 eye = Vec3.atBottomCenterOf(feet).add(0, 1.62, 0);
			List<BlockPos> out = new ArrayList<>();
			for (BlockPos p : BlockPos.betweenClosed(feet.offset(-3, -2, -3), feet.offset(3, 3, 3))) {
				if (!level.getBlockState(p).is(Blocks.STONE) || eye.distanceTo(Vec3.atCenterOf(p)) > 4.2 || p.getY() < feet.getY() || p.getY() > feet.getY() + 1) {
					continue;
				}
				var hit = level.clip(new net.minecraft.world.level.ClipContext(eye, Vec3.atCenterOf(p), net.minecraft.world.level.ClipContext.Block.OUTLINE,
						net.minecraft.world.level.ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
				if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK && hit.getBlockPos().equals(p)) {
					out.add(p.immutable());
				}
			}
			// nearest the feet first: a block beside them drops its cobblestone where the player picks it up
			Vec3 foot = Vec3.atBottomCenterOf(feet);
			out.sort((a, b) -> Double.compare(a.distToCenterSqr(foot), b.distToCenterSqr(foot)));
			return out.subList(0, Math.min(max, out.size()));
		});
	}

	private final List<BlockPos> passage = new ArrayList<>();

	/** Walks down the passage from where oreFace left the player to the face: the cells in order, a few ticks a cell. */
	private void downPassage(BlockPos target) {
		int from = Math.max(0, passage.size() - 5);
		for (int i = from + 1; i < passage.size(); i++) {
			Vec3 a = Vec3.atBottomCenterOf(passage.get(i - 1)), b = Vec3.atBottomCenterOf(passage.get(i));
			for (int k = 1; k <= 6; k++) {
				Vec3 p = a.lerp(b, k / 6.0);
				float[] yp = yawPitch(p.add(0, 1.62, 0), Vec3.atCenterOf(target));
				context.runOnClient(c -> {
					c.player.setPos(p.x, p.y, p.z);
					c.player.setYRot(yp[0]);
					c.player.setXRot(yp[1]);
					c.player.setYHeadRot(yp[0]);
					c.player.setDeltaMovement(Vec3.ZERO);
				});
				context.waitTick();
			}
		}
		context.waitTicks(10);
	}

	private BlockPos oreFace(Block ore) {
		BlockPos from = gullyFeet;
		BlockPos target = server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos best = null;
			for (BlockPos p : BlockPos.betweenClosed(from.offset(-20, -20, -20), from.offset(20, 2, 20))) {
				if (level.getBlockState(p).is(ore) && (best == null || p.distSqr(from) < best.distSqr(from))) {
					best = p.immutable();
				}
			}
			return best;
		});
		if (target == null) {
			throw new IllegalStateException("no " + ore + " near the gully");
		}
		// dig: step from the gully toward the ore, down one a step while above it, until the next step is the ore
		BlockPos at = from;
		List<BlockPos> dug = new ArrayList<>();
		passage.clear();
		passage.add(from);
		for (int i = 0; i < 60; i++) {
			int hx = target.getX() - at.getX(), hz = target.getZ() - at.getZ();
			int dy = at.getY() > target.getY() ? -1 : 0;
			if (Math.abs(hx) + Math.abs(hz) == 1 && dy == 0) {
				break;   // beside it, on its level
			}
			BlockPos next;
			if (Math.abs(hx) + Math.abs(hz) <= 1) {
				// over it or beside it but higher: step to a side cell and down
				Direction side = Direction.getApproximateNearest(-(hz == 0 ? 1 : 0), 0, hz == 0 ? 0 : -Integer.signum(hz));
				next = Math.abs(hx) + Math.abs(hz) == 0 ? at.relative(side).below() : at.below();
			} else {
				next = Math.abs(hx) >= Math.abs(hz) ? at.offset(Integer.signum(hx), dy, 0) : at.offset(0, dy, Integer.signum(hz));
			}
			if (next.equals(target)) {
				break;
			}
			for (BlockPos c : new BlockPos[] {next, next.above(), next.above(2)}) {
				if (!c.equals(target) && !clear(c)) {
					server.computeOnServer(srv -> srv.overworld().setBlock(c, Blocks.AIR.defaultBlockState(), 3));
					dug.add(c);
				}
			}
			if (!solid(next.below())) {
				server.computeOnServer(srv -> srv.overworld().setBlock(next.below(), Blocks.STONE.defaultBlockState(), 3));
			}
			at = next;
			passage.add(at);
			if (i % 4 == 2) {
				BlockPos torch = at.above();
				server.computeOnServer(srv -> srv.overworld().setBlock(torch, Blocks.TORCH.defaultBlockState(), 3));
			}
		}
		// a step back from the face on the same level, so the ore shows with the rock round it, and a torch in a niche either side
		Direction dir = Direction.getApproximateNearest(target.getX() - at.getX(), 0, target.getZ() - at.getZ());
		BlockPos back = at.relative(dir.getOpposite());
		// the cell over the ore too, so its drop lies where the player can step to it
		BlockPos[] cut = {back, back.above(), at.relative(dir.getClockWise()), back.relative(dir.getCounterClockWise()), target.above()};
		for (BlockPos c : cut) {
			if (!c.equals(target) && !clear(c)) {
				server.computeOnServer(srv -> srv.overworld().setBlock(c, Blocks.AIR.defaultBlockState(), 3));
				dug.add(c);
			}
		}
		if (!solid(back.below())) {
			server.computeOnServer(srv -> srv.overworld().setBlock(back.below(), Blocks.STONE.defaultBlockState(), 3));
		}
		for (BlockPos c : new BlockPos[] {at.relative(dir.getClockWise()), back.relative(dir.getCounterClockWise())}) {
			if (solid(c.below())) {
				server.computeOnServer(srv -> srv.overworld().setBlock(c, Blocks.TORCH.defaultBlockState(), 3));
			}
		}
		log("dug " + dug.size() + " blocks off camera to " + ore + " at " + target.toShortString() + "; stand at " + back.toShortString());
		passage.add(back);
		// start four cells up the passage, looking down it
		BlockPos start = passage.get(Math.max(0, passage.size() - 5));
		standAt(start, Vec3.atCenterOf(target));
		return target;
	}
	// ---- episode 1's base, rebuilt at the same positions (off camera) --------------------------------------------------------------------

	private static final BlockPos GULLY = new BlockPos(-584, 60, 955);
	private BlockPos gullyFeet;

	private void set(BlockPos p, BlockState s) {
		server.computeOnServer(srv -> srv.overworld().setBlock(p, s, 3));
	}

	private void rebuildDayOne() {
		// the grass and flowers round the base, as episode 1 cut them
		server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (BlockPos p : BlockPos.betweenClosed(bx - 10, by + 1, bz - 8, bx + 10, by + 3, bz + 12)) {
				BlockState st = level.getBlockState(p);
				if (st.canBeReplaced() && !st.isAir() && st.getFluidState().isEmpty() || st.is(BlockTags.FLOWERS)) {
					level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
				}
			}
			return null;
		});
		// the two oaks it punched: the bottom two logs of each, found as episode 1 found them
		BlockPos base = new BlockPos(bx, by + 1, bz);
		List<BlockPos> trunks = server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			List<BlockPos> out = new ArrayList<>();
			for (BlockPos p : BlockPos.betweenClosed(base.offset(-30, -4, -30), base.offset(30, 6, 30))) {
				if (!level.getBlockState(p).is(Blocks.OAK_LOG) || !level.getBlockState(p.below()).is(BlockTags.DIRT) || !level.getBlockState(p.above()).is(Blocks.OAK_LOG)
						|| p.distSqr(base) < 64) {
					continue;
				}
				out.add(p.immutable());
			}
			out.sort((x, y) -> Double.compare(x.distSqr(base), y.distSqr(base)));
			return out;
		});
		BlockPos a = null, b = null;
		// PLAN.md "Episode 1's base": these two, as episode 1's take found them (the search below depends on which chunks have loaded)
		BlockPos p1 = new BlockPos(-563, 67, 930), p2 = new BlockPos(-557, 67, 930);
		if (state(p1).is(Blocks.OAK_LOG) && state(p1.above()).is(Blocks.OAK_LOG) && state(p2).is(Blocks.OAK_LOG) && state(p2.above()).is(Blocks.OAK_LOG)) {
			a = p1;
			b = p2;
			trunks.clear();
		}
		search:
		for (BlockPos t1 : trunks) {
			BlockPos s1 = besideTrunk(t1, base);
			if (s1 == null) {
				continue;
			}
			for (BlockPos t2 : trunks) {
				double d = t1.distSqr(t2);
				if (t2.equals(t1) || d < 9 || d > 81 || Math.abs(t2.getY() - t1.getY()) > 0) {
					continue;
				}
				BlockPos s2 = besideTrunk(t2, t1);
				if (s2 != null && pathClear(s1, s2)) {
					a = t1;
					b = t2;
					break search;
				}
			}
		}
		for (BlockPos p : new BlockPos[] {a, a.above(), b, b.above()}) {
			set(p, Blocks.AIR.defaultBlockState());
		}
		log("oaks punched (bottom two logs) at " + a.toShortString() + " and " + b.toShortString());
		// the table, the furnace and the blast furnace in a row (both facing south), the campfire lit five blocks south
		set(table, Blocks.CRAFTING_TABLE.defaultBlockState());
		set(furnace, Blocks.FURNACE.defaultBlockState().setValue(net.minecraft.world.level.block.FurnaceBlock.FACING, Direction.SOUTH));
		set(blast, Blocks.BLAST_FURNACE.defaultBlockState().setValue(net.minecraft.world.level.block.BlastFurnaceBlock.FACING, Direction.SOUTH));
		set(campfire, Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, true)
				.setValue(net.minecraft.world.level.block.CampfireBlock.FACING, Direction.SOUTH));
		// the gully: three stones and the coal ore taken, as episode 1 took them
		BlockPos coalOre = server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos best = null;
			for (BlockPos p : BlockPos.betweenClosed(GULLY.offset(-16, -8, -16), GULLY.offset(16, 8, 24))) {
				if (level.getBlockState(p).is(Blocks.COAL_ORE) && (best == null || p.distSqr(GULLY) < best.distSqr(GULLY))) {
					boolean open = false;
					for (Direction d : Direction.values()) {
						open |= level.getBlockState(p.relative(d)).isAir() && level.canSeeSky(p.relative(d));
					}
					if (open) {
						best = p.immutable();
					}
				}
			}
			return best;
		});
		gullyFeet = standFor(coalOre, coalOre.above(2), 4.2);
		List<BlockPos> stones = stoneNear(gullyFeet, 6);
		for (int i = 0; i < 3 && i < stones.size(); i++) {
			set(stones.get(i), Blocks.AIR.defaultBlockState());
		}
		set(coalOre, Blocks.AIR.defaultBlockState());
		// the passages to the iron and the copper, dug and lit as episode 1 dug them, the ore mined
		BlockPos iron = oreFace(Blocks.IRON_ORE);
		set(iron, Blocks.AIR.defaultBlockState());
		BlockPos copper = oreFace(Blocks.COPPER_ORE);
		set(copper, Blocks.AIR.defaultBlockState());
		log("day one rebuilt: coal " + coalOre.toShortString() + ", gully stand " + gullyFeet.toShortString() + ", stones " + stones.subList(0, Math.min(3, stones.size()))
				+ ", iron " + iron.toShortString() + ", copper " + copper.toShortString() + ", campfire " + state(campfire));
		// the Getting started chapter, done as episode 1 did it: the items carried and the advancements earned; the stone, campfire and
		// iron rewards claimed; then the pocket as episode 1 left it
		survival();
		server.runCommand("recipe give @p *");
		for (String adv : new String[] {"story/mine_stone", "story/upgrade_tools", "story/smelt_iron"}) {
			server.runCommand("advancement grant @p only minecraft:" + adv);
		}
		for (String g : new String[] {"minecraft:oak_log 4", "minecraft:campfire 1", "minecraft:iron_ingot 8", "minecraft:copper_ingot 16", "wildcatter:steel_ingot 8",
				"wildcatter:field_manual 1"}) {
			server.runCommand("give @p " + g);
		}
		for (int i = 0; i < 40 && !questDone("field_manual"); i++) {
			context.waitTicks(10);
		}
		for (String q : new String[] {"stone", "campfire", "iron"}) {
			server.computeOnServer(srv -> dev.wildcatter.quest.QuestTracker.claim(sp(srv), Identifier.parse("wildcatter:" + q)));
		}
		context.waitTicks(10);
		server.computeOnServer(srv -> {
			var inv = sp(srv).getInventory();
			inv.clearContent();
			inv.setItem(0, new ItemStack(WildcatterItems.QUEST_BOOK));
			inv.setItem(1, new ItemStack(WildcatterItems.FIELD_MANUAL));
			inv.setItem(9, new ItemStack(WildcatterItems.STEEL_INGOT, 8));
			inv.setItem(10, new ItemStack(Items.COPPER_INGOT, 16));
			inv.setItem(11, new ItemStack(Items.COAL, 2));
			inv.setItem(12, new ItemStack(Items.WOODEN_PICKAXE));
			inv.setItem(13, new ItemStack(Items.STONE_PICKAXE));
			inv.setItem(14, new ItemStack(Items.TORCH, 16));
			inv.setItem(15, new ItemStack(Items.BREAD, 6));
			return null;
		});
		context.waitTicks(10);
		context.runOnClient(c -> {
			c.gui.hud.getChat().clearMessages(false);
			c.gui.toastManager().clear();
		});
		log("day one's quests: " + questsDone() + "; pocket " + inventory());
	}

	// ---- small hands ----------------------------------------------------------------------------------------------------------------------

	/** The held item used on a block's face: places a block or wire, fills, reads (useOn, as a right-click). */
	private void put(Item what, BlockPos on, Direction face) {
		hold(what);
		useOn(on, face);
		context.waitTicks(6);
	}

	/** As put(), sneaking: setting a part on a machine (a click on a machine otherwise reads it or opens it, as in play). */
	private void sneakPut(Item what, BlockPos on, Direction face) {
		hold(what);
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(4);
		useOn(on, face);
		context.getInput().releaseKey(o -> o.keyShift);
		context.waitTicks(6);
	}

	/** Reads a block with the power meter (a right-click with it), and logs the reading the player sees. */
	private void meter(BlockPos at) {
		hold(WildcatterItems.POWER_METER);
		context.waitTicks(4);
		useOn(at, faceToward(at));
		mark("meter on " + at.toShortString() + ": " + reading(at));
		context.waitTicks(70);
	}

	private String reading(BlockPos at) {
		return server.computeOnServer(srv -> {
			var be = dev.wildcatter.multiblock.BigParts.entity(srv.overworld(), at);
			return be instanceof Inspectable i ? i.inspect().getString() : String.valueOf(be);
		});
	}

	/** An empty hand (the last hotbar slot), for reading a boiler or a turbine by hand. */
	private void emptyHand() {
		server.computeOnServer(srv -> {
			var inv = sp(srv).getInventory();
			if (!inv.getItem(8).isEmpty()) {
				for (int k = 9; k < 36; k++) {
					if (inv.getItem(k).isEmpty()) {
						inv.setItem(k, inv.getItem(8).copy());
						inv.setItem(8, ItemStack.EMPTY);
						break;
					}
				}
			}
			return null;
		});
		context.waitTicks(2);
		context.getInput().pressKey(o -> o.keyHotbarSlots[8]);
		context.waitTicks(4);
	}

	private void handRead(BlockPos at) {
		emptyHand();
		useOn(at, faceToward(at));
		mark("read by hand " + at.toShortString() + ": " + reading(at));
		context.waitTicks(70);
	}

	private void tableCraft(int width, String[] pattern, Map<Character, Item> key, int times, String what) {
		craft(width, pattern, key, times, 10, 37);
		mark(what + " made");
		context.waitTicks(8);
	}

	private Map<Character, Item> keys(Object... kv) {
		LinkedHashMap<Character, Item> k = new LinkedHashMap<>();
		for (int i = 0; i < kv.length; i += 2) {
			k.put((Character) kv[i], (Item) kv[i + 1]);
		}
		return k;
	}

	private void atTable(int n, String film, long time) {
		standAt(new BlockPos(bx, by + 1, bz + 2), Vec3.atCenterOf(table));
		server.runCommand("time set " + time);
		context.waitTicks(wanted(n, film) ? 60 : 3);
		startFilm(n, film);
		openBlock(table);
	}

	private boolean lampLit() {
		return server.computeOnServer(srv -> {
			BlockState s = srv.overworld().getBlockState(lamp);
			return s.hasProperty(dev.wildcatter.block.LoadBlock.LIT) && s.getValue(dev.wildcatter.block.LoadBlock.LIT);
		});
	}


	// ---- episode 2's plant, rebuilt at the same positions (off camera) -------------------------------------------------------------

	private static Block block(String id) {
		return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
	}

	/** A big part's main cell set and its other cells grown, facing {@code facing} (as GridSceneKit.place). */
	private void bigPart(BlockPos main, BlockState state) {
		server.computeOnServer(srv -> {
			srv.overworld().setBlock(main, state, 3);
			dev.wildcatter.multiblock.BigParts.grow(srv.overworld(), main, state);
			return null;
		});
	}

	/** Every wire's drawn joins worked out from its neighbours, as a player laying them in order leaves them. */
	private void joinShapes(List<BlockPos> cells) {
		server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (BlockPos p : cells) {
				BlockState s = level.getBlockState(p);
				BlockState u = Block.updateFromNeighbourShapes(s, level, p);
				if (u != s) {
					level.setBlock(p, u, 2);
				}
			}
			return null;
		});
	}

	/**
	 * PLAN.md "Episode 2's base": the campfire became the firebox (16 coal), the boiler on it (water as the take left it), the smooth stone pad,
	 * the turbine facing east, the breaker on the pad (facing east), the hot wire to the lamp, the neutral back along z 944, the ground rods,
	 * the fuse over the neutral (facing north) and the branch to the receptacle with the plug adapter against it. Then the First power
	 * chapter finished as episode 2 finished it (the plant's own milestones, the items carried), every reward claimed (Receptacle and plug
	 * has none), and episode 2's pocket as its take log left it.
	 */
	private void rebuildEpisodeTwo() {
		int y = by + 1;
		var fb = block("wildcatter:firebox");
		set(campfire, fb.defaultBlockState());
		server.computeOnServer(srv -> {
			if (srv.overworld().getBlockEntity(campfire) instanceof dev.wildcatter.block.FireboxBlockEntity f) {
				f.setItem(0, new ItemStack(Items.COAL, 16));
			}
			return null;
		});
		bigPart(boiler, block("wildcatter:boiler").defaultBlockState());
		server.computeOnServer(srv -> {
			if (dev.wildcatter.multiblock.BigParts.entity(srv.overworld(), boiler) instanceof dev.wildcatter.block.BoilerBlockEntity b) {
				b.addWater(3700);
			}
			return null;
		});
		for (BlockPos p : new BlockPos[] {new BlockPos(-566, y, 945), new BlockPos(-565, y, 945), new BlockPos(-566, y, 946)}) {
			set(p, Blocks.SMOOTH_STONE.defaultBlockState());
		}
		set(turbine, block("wildcatter:steam_turbine").defaultBlockState().setValue(dev.wildcatter.block.SteamTurbineBlock.FACING, Direction.EAST));
		set(new BlockPos(-565, y + 1, 945), block("wildcatter:breaker").defaultBlockState().setValue(dev.wildcatter.block.BreakerBlock.FACING, Direction.EAST)
				.setValue(dev.wildcatter.block.GearMount.MOUNT, dev.wildcatter.block.GearMount.FLOOR));
		List<BlockPos> wires = new ArrayList<>();
		for (BlockPos p : new BlockPos[] {new BlockPos(-564, y + 1, 945), new BlockPos(-564, y, 945), new BlockPos(-563, y, 945), new BlockPos(-564, y + 1, 943),
				new BlockPos(-564, y, 943)}) {
			set(p, block("wildcatter:wool_wire_hot").defaultBlockState());
			wires.add(p);
		}
		for (BlockPos p : new BlockPos[] {new BlockPos(-566, y + 1, 944), new BlockPos(-566, y, 944), new BlockPos(-565, y, 944), new BlockPos(-564, y, 944),
				new BlockPos(-563, y, 944), new BlockPos(-562, y, 944)}) {
			set(p, block("wildcatter:wool_wire_neutral").defaultBlockState());
			wires.add(p);
		}
		set(lamp, block("wildcatter:electric_lamp").defaultBlockState());
		set(new BlockPos(-566, y + 1, 946), block("wildcatter:ground_rod").defaultBlockState());
		set(new BlockPos(-562, y, 946), block("wildcatter:ground_rod").defaultBlockState());
		set(new BlockPos(-564, y + 1, 944), block("wildcatter:fuse").defaultBlockState().setValue(dev.wildcatter.block.FuseBlock.FACING, Direction.NORTH)
				.setValue(dev.wildcatter.block.GearMount.MOUNT, dev.wildcatter.block.GearMount.FLOOR));
		set(new BlockPos(-563, y, 943), block("wildcatter:receptacle_nema_5_15").defaultBlockState());
		set(new BlockPos(-562, y, 943), block("wildcatter:plug_nema_5_15").defaultBlockState().setValue(dev.wildcatter.block.AttachedGear.ATTACH, Direction.WEST));
		joinShapes(wires);
		server.runCommand("tp @p -560 " + (y + 1) + " 948");
		context.waitTicks(100);
		// the chapter as episode 2 finished it: the plant's milestones (the turbine generating, the lamp lit) and the items carried
		for (String g : new String[] {"wildcatter:boiler 1", "wildcatter:steam_turbine 1", "wildcatter:wool_wire_hot 4", "wildcatter:wool_wire_neutral 4",
				"wildcatter:electric_lamp 1", "wildcatter:breaker 1", "wildcatter:fuse 1", "wildcatter:ground_rod 1", "wildcatter:power_meter 1", "wildcatter:firebox 1",
				"wildcatter:receptacle_nema_5_15 1", "wildcatter:plug_nema_5_15 1"}) {
			server.runCommand("give @p " + g);
		}
		for (int i = 0; i < 40 && !questDone("plugs"); i++) {
			context.waitTicks(10);
		}
		for (String q : new String[] {"wood", "field_manual", "copper", "steel", "steam", "lamp", "protection", "firebox"}) {
			server.computeOnServer(srv -> dev.wildcatter.quest.QuestTracker.claim(sp(srv), Identifier.parse("wildcatter:" + q)));
		}
		context.waitTicks(10);
		// episode 2's pocket, slot for slot (films-bfs02 take 2's final inventory)
		server.computeOnServer(srv -> {
			var inv = sp(srv).getInventory();
			inv.clearContent();
			Object[][] pocket = {{0, WildcatterItems.QUEST_BOOK, 1}, {1, WildcatterItems.FIELD_MANUAL, 1}, {2, Items.BUCKET, 1}, {3, Items.APPLE, 3}, {4, Items.PAPER, 8},
					{5, Items.BUCKET, 1}, {6, Items.BUCKET, 1}, {7, Items.BUCKET, 1}, {8, WildcatterItems.POWER_METER, 1}, {9, WildcatterItems.STEEL_INGOT, 8},
					{10, Items.COPPER_INGOT, 7}, {11, item("wildcatter:wool_wire_hot"), 2}, {12, Items.WOODEN_PICKAXE, 1}, {13, Items.STONE_PICKAXE, 1}, {14, Items.TORCH, 16},
					{15, Items.BREAD, 6}, {16, Items.BUCKET, 1}, {18, item("wildcatter:wire_neutral"), 4}, {19, Items.IRON_INGOT, 1},
					{20, item("wildcatter:wool_wire_neutral"), 2}, {21, item("wildcatter:fuse"), 3}, {22, item("wildcatter:wire_hot"), 4}, {23, Items.WATER_BUCKET, 1},
					{24, item("wildcatter:wool_wire_hot"), 8}, {25, item("wildcatter:wool_wire_neutral"), 8}, {26, item("wildcatter:ground_rod"), 2}};
			for (Object[] p : pocket) {
				inv.setItem((Integer) p[0], new ItemStack((Item) p[1], (Integer) p[2]));
			}
			return null;
		});
		context.waitTicks(10);
		context.runOnClient(c -> {
			c.gui.hud.getChat().clearMessages(false);
			c.gui.toastManager().clear();
		});
		log("episode 2 rebuilt: lamp " + reading(lamp) + " | turbine " + reading(turbine) + " | boiler " + reading(boiler) + " | firebox " + reading(campfire));
		log("episode 2's quests: " + questsDone() + "; pocket " + inventory());
	}

	/** Puts away (off camera, into a chest at the base) what this episode never uses, so the pocket has room. */
	private void stash() {
		server.computeOnServer(srv -> {
			var inv = sp(srv).getInventory();
			Set<Item> away = Set.of(Items.APPLE, Items.WOODEN_PICKAXE, Items.BREAD, item("wildcatter:wire_neutral"), item("wildcatter:wire_hot"),
					item("wildcatter:wool_wire_neutral"), item("wildcatter:fuse"), Items.PAPER);
			for (int i = 0; i < 36; i++) {
				if (away.contains(inv.getItem(i).getItem())) {
					inv.setItem(i, ItemStack.EMPTY);
				}
			}
			return null;
		});
		context.waitTicks(4);
		log("stashed at the base (off camera): apples, bread, the wooden pickaxe, paper, bare wire, the neutral wire, the fuses; pocket " + inventory());
	}

	/** Off camera: what the player picked up on the way (dirt from the pits, seeds, mushrooms) and what this episode no longer needs put away. */
	private void stashJunk(Item... also) {
		Set<Item> away = new java.util.HashSet<>(List.of(Items.DIRT, Items.WHEAT_SEEDS, Items.RED_MUSHROOM, Items.BROWN_MUSHROOM, Items.STICK, Items.APPLE,
				Items.OAK_SAPLING, Items.BIRCH_SAPLING, Items.SHORT_GRASS, Items.COBBLESTONE, Items.PAPER, Items.BREAD, Items.WOODEN_PICKAXE));
		away.addAll(List.of(also));
		String gone = server.computeOnServer(srv -> {
			var inv = sp(srv).getInventory();
			StringBuilder b = new StringBuilder();
			for (int i = 0; i < 36; i++) {
				ItemStack st = inv.getItem(i);
				if (!st.isEmpty() && (away.contains(st.getItem()) || st.is(net.minecraft.tags.ItemTags.SAPLINGS))) {
					b.append(st.getCount()).append(' ').append(st.getItem()).append(", ");
					inv.setItem(i, ItemStack.EMPTY);
				}
			}
			return b.toString();
		});
		log("put away off camera: " + gone + "pocket " + inventory());
	}

	/** Takes {@code n} of an item out of the pocket (off camera: it was placed or used where the film does not show). */
	private void take(Item what, int n) {
		server.computeOnServer(srv -> {
			var inv = sp(srv).getInventory();
			int left = n;
			for (int i = 0; i < 36 && left > 0; i++) {
				ItemStack s = inv.getItem(i);
				if (s.is(what)) {
					int k = Math.min(left, s.getCount());
					s.shrink(k);
					left -= k;
				}
			}
			return null;
		});
	}

	/** Claims a quest's reward with its page open (the Claim button), and holds the page a moment. */
	private void claimIn(String quest) {
		hold(WildcatterItems.QUEST_BOOK);
		useHeld();
		context.waitTicks(6);
		context.runOnClient(c -> {
			if (c.gui.screen() instanceof QuestScreen q) {
				q.open(Identifier.parse("wildcatter:" + quest), true);
			}
		});
		context.waitTicks(30);
		claim(quest);
		context.waitTicks(30);
		context.setScreen(() -> null);
		context.waitTicks(4);
	}

	// ---- the ground --------------------------------------------------------------------------------------------------------------------

	/** The cell the player stands in at this column: over the ground's top, logs and leaves ignored. */
	private BlockPos feet(int x, int z) {
		return server.computeOnServer(srv -> new BlockPos(x, srv.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z));
	}

	/**
	 * Off camera: the trees round a site taken down (logs and leaves within {@code margin} of the box) and the box's ground made level at
	 * {@code top} (cut down or filled with dirt, grass on top), short plants cleared.
	 */
	private void levelSite(int x1, int z1, int x2, int z2, int top, int margin) {
		int cells = server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			int n = 0;
			for (BlockPos p : BlockPos.betweenClosed(x1 - margin, top - 4, z1 - margin, x2 + margin, top + 32, z2 + margin)) {
				BlockState s = level.getBlockState(p);
				if (s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES) || p.getY() > top && p.getY() <= top + 2 && (s.canBeReplaced() && !s.isAir() && s.getFluidState().isEmpty()
						|| s.is(BlockTags.FLOWERS))) {
					level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
					n++;
				}
			}
			for (int x = x1; x <= x2; x++) {
				for (int z = z1; z <= z2; z++) {
					for (int yy = top + 1; yy <= top + 32; yy++) {
						BlockPos p = new BlockPos(x, yy, z);
						if (!level.getBlockState(p).isAir()) {
							level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
							n++;
						}
					}
					for (int yy = top - 4; yy < top; yy++) {
						BlockPos p = new BlockPos(x, yy, z);
						BlockState s = level.getBlockState(p);
						if (s.isAir() || !s.getFluidState().isEmpty() || s.canBeReplaced()) {
							level.setBlock(p, Blocks.DIRT.defaultBlockState(), 2);
							n++;
						}
					}
					BlockPos topAt = new BlockPos(x, top, z);
					if (!level.getBlockState(topAt).is(Blocks.GRASS_BLOCK)) {
						level.setBlock(topAt, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
						n++;
					}
				}
			}
			return n;
		});
		log("site levelled off camera: x " + x1 + ".." + x2 + ", z " + z1 + ".." + z2 + " at ground top " + top + ", trees within " + margin + " cleared (" + cells + " cells)");
	}

	// ---- walking the survey --------------------------------------------------------------------------------------------------------------

	/** Walks over the ground to {@code to} (feet), about four blocks a second, easing in and out, the head turned the way it goes. */
	private void walkGround(int toX, int toZ) {
		Vec3 from = context.computeOnClient(c -> c.player.position());
		Vec3 end = new Vec3(toX + 0.5, from.y, toZ + 0.5);
		double dist = Math.hypot(end.x - from.x, end.z - from.z);
		if (dist < 0.3) {
			return;
		}
		face(new Vec3(end.x, from.y + 1.5, end.z), 10);
		int steps = (int) Math.max(6, dist * 5);
		double y = from.y;
		for (int i = 1; i <= steps; i++) {
			double s = (double) i / steps;
			s = s * s * (3 - 2 * s);
			double x = from.x + (end.x - from.x) * s, z = from.z + (end.z - from.z) * s;
			int ground = feet((int) Math.floor(x), (int) Math.floor(z)).getY();
			y += Math.max(-0.25, Math.min(0.25, ground - y));
			final double fx = x, fy = y, fz = z;
			context.runOnClient(c -> {
				c.player.setPos(fx, fy, fz);
				c.player.setDeltaMovement(Vec3.ZERO);
			});
			context.waitTick();
		}
	}

	private static final String[] COMPASS = {"east", "south-east", "south", "south-west", "west", "north-west", "north", "north-east"};

	/** The seismograph thumped where the player stands: its report logged with the spot (the edit's evidence). */
	private String thump() {
		hold(item("wildcatter:seismograph"));
		context.waitTicks(10);
		String report = server.computeOnServer(srv -> {
			ServerPlayer p = sp(srv);
			return dev.wildcatter.item.SeismographItem.report(srv.overworld(), p.getX(), p.getZ());
		});
		useHeld();
		Vec3 at = context.computeOnClient(c -> c.player.position());
		mark(String.format(Locale.ROOT, "seismograph at %.1f %.1f %.1f: %s", at.x, at.y, at.z, report));
		return report;
	}

	/** The way the report says the field lies, as a point ten blocks off at eye height; null when it says the player is over one. */
	private Vec3 lies(String report) {
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("echo from the ([a-z-]+),").matcher(report);
		if (!m.find()) {
			return null;
		}
		int k = java.util.Arrays.asList(COMPASS).indexOf(m.group(1));
		double a = Math.toRadians(k * 45.0);
		Vec3 eye = context.computeOnClient(c -> c.player.getEyePosition());
		return eye.add(Math.cos(a) * 10, -1.5, Math.sin(a) * 10);
	}

	/**
	 * One reading of the survey: the player a few blocks short of the spot, walking in along {@code fromX, fromZ}, then the seismograph; the
	 * readout held; then the head turned the way the report says the field lies.
	 */
	private String reading(int n, String name, int fromX, int fromZ, int atX, int atZ, long time) {
		BlockPos at0 = open(atX, atZ);
		atX = at0.getX();
		atZ = at0.getZ();
		BlockPos from = open(fromX, fromZ);
		trample(from, at0);
		standAt(from, new Vec3(atX + 0.5, from.getY() + 1.4, atZ + 0.5));
		server.runCommand("time set " + time);
		context.waitTicks(wanted(n, name) ? 80 : 3);
		startFilm(n, name);
		context.waitTicks(6);
		walkGround(atX, atZ);
		context.waitTicks(10);
		String report = thump();
		context.waitTicks(80);
		Vec3 way = lies(report);
		if (way != null) {
			face(way, 30);
			mark("turned to the " + way);
		}
		context.waitTicks(70);
		stopFilm();
		return report;
	}

	/** Drops lying within {@code r} of {@code at} picked up: drawn in to the player, as walking over them does. */
	private void pickUpNear(BlockPos at, double r) {
		int n = server.computeOnServer(srv -> {
			ServerPlayer p = sp(srv);
			var items = srv.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(at).inflate(r));
			for (var e : items) {
				e.setPos(p.getX(), p.getY() + 0.3, p.getZ());
				e.setNoPickUpDelay();
			}
			return items.size();
		});
		context.waitTicks(6);
		mark("picked up " + n + " drop(s) near " + at.toShortString());
	}

	/** The nearest column (within 6) where the player stands in the open: no leaves or logs within four blocks over the head. */
	private BlockPos open(int x, int z) {
		return server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (int r = 0; r <= 6; r++) {
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
							continue;
						}
						int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz);
						BlockPos f = new BlockPos(x + dx, y, z + dz);
						boolean clear = level.getFluidState(f.below()).isEmpty() && !level.getBlockState(f.below()).is(BlockTags.LOGS);
						for (int k = 0; k < 6 && clear; k++) {
							BlockState st = level.getBlockState(f.above(k));
							clear = !st.is(BlockTags.LEAVES) && !st.is(BlockTags.LOGS);
						}
						if (clear) {
							return f;
						}
					}
				}
			}
			return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
		});
	}

	/** Off camera: the tall grass and flowers along the few steps of a reading's walk trodden down (cut), so they do not fill the view. */
	private void trample(BlockPos a, BlockPos b) {
		int n = server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			int k = 0;
			for (BlockPos p : BlockPos.betweenClosed(Math.min(a.getX(), b.getX()) - 3, Math.min(a.getY(), b.getY()) - 1, Math.min(a.getZ(), b.getZ()) - 3,
					Math.max(a.getX(), b.getX()) + 3, Math.max(a.getY(), b.getY()) + 3, Math.max(a.getZ(), b.getZ()) + 3)) {
				BlockState st = level.getBlockState(p);
				if (!st.isAir() && st.canBeReplaced() && st.getFluidState().isEmpty() || st.is(BlockTags.FLOWERS) || st.is(Blocks.SUGAR_CANE)) {
					level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
					k++;
				}
			}
			return k;
		});
		log("trodden off camera: " + n + " plants round " + a.toShortString() + " to " + b.toShortString());
	}

	/** A survey stake: a fence post with a torch on it, where the reading said to drill. */
	private void stake(BlockPos ground) {
		put(Items.OAK_FENCE, ground, Direction.UP);
		put(Items.TORCH, ground.above(), Direction.UP);
		mark("stake at " + ground.above().toShortString());
	}

	// ---- 1: where episode two left off ---------------------------------------------------------------------------------------------------

	private void base(int n) {
		spectator();
		server.runCommand("time set 1500");
		// from the south-east, low, along the plant: the lamp at the front, the boiler on its firebox behind
		drone(n, "base", cam(8.5, 3.6, 10.5, 2.0, 1.4, 4.8), cam(1.5, 3.8, 12.0, 2.0, 1.4, 4.8), 300);
		survival();
	}

	// ---- 2: step up ----------------------------------------------------------------------------------------------------------------------

	private BlockPos dryAtBase;

	private void stepUp(int n) {
		give("minecraft:copper_block", 2, "copper mined and smelted, packed into blocks");
		give("minecraft:iron_block", 1, "iron mined and smelted, packed into a block");
		atTable(n, "drytype_craft", 2000);
		tableCraft(3, new String[] {"sws", "cic", "sws"}, keys('s', WildcatterItems.STEEL_INGOT, 'w', item("wildcatter:wool_wire_hot"), 'c', item("minecraft:copper_block"),
				'i', Items.IRON_BLOCK), 1, "dry-type transformer");
		context.waitTicks(10);
		closeScreen();
		stopFilm();
		// on top of episode 2's turbine: its bottom face is a low side; the high side (front and top) left open
		dryAtBase = turbine.above();
		standAt(new BlockPos(-563, by + 1, 947), Vec3.atCenterOf(dryAtBase));
		server.runCommand("time set 2300");
		context.waitTicks(wanted(n, "drytype_place") ? 60 : 3);
		startFilm(n, "drytype_place");
		context.waitTicks(10);
		face(Vec3.atCenterOf(dryAtBase), 10);
		sneakPut(item("wildcatter:dry_type_transformer"), turbine, Direction.UP);
		mark("dry-type placed: " + state(dryAtBase));
		context.waitTicks(30);
		meter(dryAtBase);
		waitQuest("step_up");
		context.waitTicks(20);
		stopFilm();
		startFilm(n, "stepup_book");
		claimIn("step_up");
		stopFilm();
		// it goes to the field with the rig: taken down again
		standAt(new BlockPos(-563, by + 1, 947), Vec3.atCenterOf(dryAtBase));
		context.waitTicks(wanted(n, "drytype_off") ? 40 : 3);
		startFilm(n, "drytype_off");
		context.waitTicks(8);
		hold(Items.STONE_PICKAXE);
		dig(dryAtBase, 200);
		mark("dry-type taken down: " + state(dryAtBase) + "; lamp lit " + lampLit());
		context.waitTicks(20);
		stopFilm();
		pickUpNear(dryAtBase, 2.5);
		log("dry-type carried: " + count(item("wildcatter:dry_type_transformer")));
	}

	// ---- 3: the seismograph --------------------------------------------------------------------------------------------------------------

	private void seismograph(int n) {
		give("minecraft:iron_ingot", 4, "iron mined and smelted");
		give("minecraft:redstone", 2, "redstone from the passage");
		give("minecraft:compass", 1, "a compass (four iron and a redstone)");
		atTable(n, "seismo_craft", 3000);
		tableCraft(3, new String[] {"ipi", "rcr", "iii"}, keys('i', Items.IRON_INGOT, 'p', Items.PAPER, 'r', Items.REDSTONE, 'c', Items.COMPASS), 1, "seismograph");
		context.waitTicks(10);
		closeScreen();
		waitQuest("seismograph");
		context.waitTicks(10);
		stopFilm();
		startFilm(n, "seismo_book");
		claimIn("seismograph");   // a compass
		stopFilm();
	}

	// ---- 4: the survey: the deep field under the base, then east across the river to a field the small rig reaches ---------------------

	static final int GAS_HOLE_X = -478, GAS_HOLE_Z = 980, GAS_GROUND = 63;
	static final int OIL_HOLE_X = -677, OIL_HOLE_Z = 864, OIL_GROUND = 73;

	private void survey(int n) {
		server.runCommand("forceload add -600 930 -460 1000");
		give("minecraft:oak_fence", 2, "two fence posts made from planks");
		// at the base: the table's corner, walking in from the furnaces
		reading(n, "survey_base", -570, 944, -569, 941, 3600);
		// a few blocks south-west, as it said: over the deep field
		reading(n, "survey_deep", -571, 943, -576, 947, 3800);
		// forty blocks east, through the trees: still the deep field, now behind
		reading(n, "survey_east", -545, 945, -540, 945, 4100);
		// over the river, on the far bank: a field the small rig reaches
		reading(n, "survey_river", -494, 987, -489, 986, 4400);
		// on to the field: over it
		reading(n, "survey_field", -486, 980, -481, 980, 4600);
		BlockPos stakeAt = feet(-480, GAS_HOLE_Z).below();
		standAt(feet(-482, GAS_HOLE_Z + 1), Vec3.atCenterOf(stakeAt.above()));
		context.waitTicks(wanted(n, "survey_stake") ? 60 : 3);
		startFilm(n, "survey_stake");
		context.waitTicks(10);
		stake(stakeAt);
		context.waitTicks(40);
		stopFilm();
	}

	// ---- 5: the rig and the gas lines, at the base ------------------------------------------------------------------------------------------

	private void rig(int n) {
		give("minecraft:iron_ingot", 4, "iron mined and smelted");
		give("minecraft:iron_block", 1, "iron mined and smelted, packed into a block");
		give("minecraft:iron_pickaxe", 1, "an iron pickaxe");
		give("minecraft:piston", 1, "a piston (planks, cobblestone, iron, redstone)");
		give("minecraft:iron_nugget", 1, "an iron ingot crafted into nuggets");
		give("wildcatter:steel_ingot", 14, "steel from the blast furnace");
		give("minecraft:copper_ingot", 2, "copper mined and smelted");
		atTable(n, "rig_craft", 5200);
		tableCraft(3, new String[] {"ssn"}, keys('s', WildcatterItems.STEEL_INGOT, 'n', Items.IRON_NUGGET), 1, "pistons");
		tableCraft(3, new String[] {"ipi", "cmc", "ibi"}, keys('i', Items.IRON_INGOT, 'p', Items.IRON_PICKAXE, 'c', Items.COPPER_INGOT, 'm', Items.PISTON,
				'b', Items.IRON_BLOCK), 1, "drilling rig");
		context.waitTicks(10);
		closeScreen();
		waitQuest("rig");
		context.waitTicks(10);
		stopFilm();
		startFilm(n, "rig_book");
		claimIn("rig");   // 16 steel
		stopFilm();
		atTable(n, "lines_craft", 5400);
		tableCraft(3, new String[] {"sss", "   ", "sss"}, keys('s', WildcatterItems.STEEL_INGOT), 3, "steel pipe x3");
		context.waitTicks(10);
		closeScreen();
		stopFilm();
		log("after the rig: " + inventory());
	}

	// ---- 6: the gas field: the rig on its own steam set, drilled, struck -------------------------------------------------------------------

	/** The field plant: the rig facing west over {@code hole}, its terminal box 5 east; the dry-type against it, the turbine, the boiler on a firebox. */
	private record Site(BlockPos hole, int s) {
		BlockPos box() {
			return hole.east(5);
		}

		BlockPos dry() {
			return hole.east(6);
		}

		BlockPos rod() {
			return hole.east(6).south();
		}

		BlockPos turbine() {
			return hole.east(7);
		}

		BlockPos firebox() {
			return hole.east(8).below();
		}

		BlockPos boiler() {
			return hole.east(8);
		}
	}

	private Site gas;

	private void gasField(int n) {
		stash();
		levelSite(-486, 973, -462, 988, GAS_GROUND, 8);
		gas = new Site(new BlockPos(GAS_HOLE_X, GAS_GROUND + 1, GAS_HOLE_Z), GAS_GROUND + 1);
		give("wildcatter:boiler", 1, "a second boiler, crafted as in episode 2");
		give("wildcatter:firebox", 1, "a second firebox, crafted as in episode 2");
		give("wildcatter:steam_turbine", 1, "a second steam turbine, crafted as in episode 2");
		give("minecraft:coal", 16, "coal mined off camera");
		take(Items.BUCKET, 4);
		give("minecraft:water_bucket", 4, "buckets filled at the river");
		// the rig, from the stake: looking east, so it faces west with its power unit at the far end
		BlockPos hole = gas.hole();
		standAt(new BlockPos(hole.getX() - 3, gas.s(), hole.getZ() + 1), Vec3.atCenterOf(hole.below()));
		server.runCommand("time set 6000");
		context.waitTicks(wanted(n, "rig_place") ? 80 : 3);
		startFilm(n, "rig_place");
		context.waitTicks(10);
		put(item("wildcatter:drilling_rig"), hole.below(), Direction.UP);
		mark("rig placed: " + state(hole) + "; box " + gas.box().toShortString());
		face(Vec3.atCenterOf(hole).add(0, 6, 0), 30);
		context.waitTicks(30);
		face(Vec3.atCenterOf(gas.box()), 30);
		context.waitTicks(30);
		stopFilm();
		powerUp(n, gas, "power_place", 6300, true, true);
		drill(n, gas, "gas", 6600);
		startFilm(n, "gas_book");
		book("gas_well", 70);
		stopFilm();
	}

	/**
	 * The field plant by hand, from the east: the dry-type against the rig's terminal box (placed looking west, so its high side faces the
	 * box), a ground rod touching both, the turbine against its low side, a firebox dug into the ground with coal in it, the boiler on it and
	 * four buckets. Filmed when {@code film}.
	 */
	private void powerUp(int n, Site site, String name, long time, boolean film, boolean rig) {
		BlockPos stand = new BlockPos(site.hole().getX() + 9, site.s(), site.hole().getZ());
		standAt(stand, Vec3.atCenterOf(site.dry().below()));
		server.runCommand("time set " + time);
		if (film) {
			context.waitTicks(wanted(n, name) ? 80 : 3);
			startFilm(n, name);
		}
		context.waitTicks(10);
		put(item("wildcatter:dry_type_transformer"), site.dry().below(), Direction.UP);
		mark("dry-type placed: " + state(site.dry()));
		put(item("wildcatter:ground_rod"), site.rod().below(), Direction.UP);
		mark("ground rod placed");
		put(item("wildcatter:steam_turbine"), site.turbine().below(), Direction.UP);
		mark("turbine placed: " + state(site.turbine()));
		hold(Items.STONE_PICKAXE);
		dig(site.firebox(), 120);
		pickUpNear(site.firebox(), 1.5);
		face(Vec3.atCenterOf(site.firebox()), 6);
		put(item("wildcatter:firebox"), site.firebox().below(), Direction.UP);
		mark("firebox placed: " + state(site.firebox()));
		openBlock(site.firebox());
		shift(slotOf(Items.COAL, 9));
		mark("coal in: " + reading(site.firebox()));
		context.waitTicks(16);
		closeScreen();
		sneakPut(item("wildcatter:boiler"), site.firebox(), Direction.UP);
		mark("boiler placed: " + state(site.boiler()));
		for (int i = 0; i < 4; i++) {
			hold(Items.WATER_BUCKET);
			useOn(site.boiler(), faceToward(site.boiler()));
			mark("bucket " + (i + 1) + ": " + reading(site.boiler()));
			context.waitTicks(8);
		}
		// step aside, clear of the boiler, to read the rig as it starts
		walk(new BlockPos(site.hole().getX() + 8, site.s(), site.hole().getZ() + 2));
		if (!rig) {
			context.waitTicks(60);
			face(Vec3.atCenterOf(site.dry()), 10);
			meter(site.dry());
			return;
		}
		for (int i = 0; i < 20 && !reading(site.hole()).contains("drilling,"); i++) {
			context.waitTicks(10);
		}
		mark("rig: " + reading(site.hole()));
		meter(site.box());
		face(Vec3.atCenterOf(site.dry()), 10);
		meter(site.dry());
		if (film) {
			stopFilm();
		}
	}

	private int depth(BlockPos hole) {
		String r = reading(hole);
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+) blocks down").matcher(r);
		return m.find() ? Integer.parseInt(m.group(1)) : -1;
	}

	private int goal(BlockPos hole) {
		return server.computeOnServer(srv -> {
			var f = dev.wildcatter.well.ReservoirData.field(srv.overworld(), hole.getX(), hole.getZ());
			return f == null ? -1 : hole.getY() - f.topY();
		});
	}

	/**
	 * The drilling, filmed from a fixed camera (spectator, the HUD hidden) for the edit to speed up; the boiler topped up by hand if it runs
	 * low (logged); then the player beside the rig for the strike: the rig rigs down and drops, the wellhead stands on the hole.
	 */
	private void drill(int n, Site site, String tag, long time) {
		BlockPos hole = site.hole();
		int goal = goal(hole);
		log(tag + " hole " + hole.toShortString() + ": the field's top " + goal + " blocks down; rig " + reading(hole) + "; boiler " + reading(site.boiler()));
		server.runCommand("time set " + time);
		// from the south-west, the mast and the whole plant in frame
		spectator();
		t.ox = hole.getX();
		t.gy = site.s() - 1;
		t.oz = hole.getZ();
		Bfs03Films.Cam a = cam(-2.5, 4.0, 10.5, 3.5, 4.0, 0.0), b = cam(-0.5, 4.2, 11.0, 3.5, 4.0, 0.0);
		t.camera(a, true);
		context.waitTicks(10);
		startFilm(n, tag + "_drill");
		int ticks = 0, total = Math.max(200, (goal - depth(hole) - 8) * 20 + 60);
		while (depth(hole) >= 0 && depth(hole) < goal - 8 && ticks < 4000) {
			t.camera(a.lerp(b, Math.min(1.0, ticks / (double) total)), false);
			context.waitTick();
			ticks++;
			if (ticks % 200 == 0) {
				mark("drilling: " + reading(hole) + " | boiler " + reading(site.boiler()));
				if (boilerWater(site.boiler()) < 1200) {
					topUp(site);
				}
			}
		}
		mark("drilled to " + depth(hole) + " of " + goal + " in " + ticks + " ticks");
		stopFilm();
		survival();
		// beside the rig, south of the mast, for the strike
		standAt(new BlockPos(hole.getX() - 3, site.s(), hole.getZ() - 3), Vec3.atCenterOf(hole).add(0, 2.5, 0));
		context.waitTicks(4);
		startFilm(n, tag + "_strike");
		for (int i = 0; i < 400 && !state(hole).is(block("wildcatter:oil_wellhead")) && !state(hole).is(block("wildcatter:gas_wellhead")); i++) {
			context.waitTick();
		}
		mark("struck: " + state(hole) + "; quests " + questsDone());
		context.waitTicks(40);
		face(Vec3.atCenterOf(hole), 14);
		context.waitTicks(30);
		walk(new BlockPos(hole.getX(), site.s(), hole.getZ() - 2));
		pickUpNear(hole.east().above(), 3.0);
		mark("rig carried: " + count(item("wildcatter:drilling_rig")));
		face(Vec3.atCenterOf(hole), 10);
		meter(hole);
		stopFilm();
	}

	private int boilerWater(BlockPos boiler) {
		String r = reading(boiler);
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("water (\\d+)/").matcher(r);
		return m.find() ? Integer.parseInt(m.group(1)) : 4000;
	}

	/** Off camera: the boiler topped up from the river (a bucket at a time, as a player would), logged for the edit. */
	private void topUp(Site site) {
		server.computeOnServer(srv -> {
			if (dev.wildcatter.multiblock.BigParts.entity(srv.overworld(), site.boiler()) instanceof dev.wildcatter.block.BoilerBlockEntity b) {
				for (int i = 0; i < 2; i++) {
					b.addWater(dev.wildcatter.Tuning.WATER_PER_BUCKET);
				}
			}
			return null;
		});
		mark("boiler topped up with two buckets (off camera): " + reading(site.boiler()));
	}

	// ---- 7: the second survey: north-west from the base, to a field in the plains ----------------------------------------------------------

	private void oilSurvey(int n) {
		server.runCommand("forceload add -690 850 -590 930");
		reading(n, "oil_base", -594, 925, -600, 920, 7200);
		reading(n, "oil_fair", -621, 904, -626, 900, 7400);
		reading(n, "oil_strong", -649, 884, -654, 880, 7600);
		levelSite(-692, 852, -662, 873, OIL_GROUND, 6);
		reading(n, "oil_field", -674, 869, -679, 867, 7800);
		BlockPos stakeAt = feet(-679, 867).below();
		standAt(feet(-681, 868), Vec3.atCenterOf(stakeAt.above()));
		context.waitTicks(wanted(n, "oil_stake") ? 60 : 3);
		startFilm(n, "oil_stake");
		context.waitTicks(10);
		stake(stakeAt);
		context.waitTicks(40);
		stopFilm();
	}

	// ---- 8: the oil field: the same plant, moved (off camera), drilled, struck ---------------------------------------------------------------

	private Site oil;

	private void oilField(int n) {
		stashJunk();
		// the gas field's plant taken down and carried here (off camera): the dry-type, the turbine, the boiler and firebox, the rod
		server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (BlockPos p : new BlockPos[] {gas.boiler(), gas.turbine(), gas.dry(), gas.rod()}) {
				level.removeBlockEntity(p);
				level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
			}
			level.removeBlockEntity(gas.boiler().above());
			level.setBlock(gas.boiler().above(), Blocks.AIR.defaultBlockState(), 3);
			level.removeBlockEntity(gas.firebox());
			level.setBlock(gas.firebox(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
			return null;
		});
		give("wildcatter:dry_type_transformer", 1, "carried from the gas field");
		give("wildcatter:ground_rod", 1, "carried from the gas field");
		give("wildcatter:steam_turbine", 1, "carried from the gas field");
		give("wildcatter:firebox", 1, "carried from the gas field");
		give("wildcatter:boiler", 1, "carried from the gas field");
		give("minecraft:coal", 16, "coal mined off camera");
		take(Items.BUCKET, 4);
		give("minecraft:water_bucket", 4, "buckets filled at the pond");
		oil = new Site(new BlockPos(OIL_HOLE_X, OIL_GROUND + 1, OIL_HOLE_Z), OIL_GROUND + 1);
		BlockPos hole = oil.hole();
		// set up off camera, as at the gas field
		standAt(new BlockPos(hole.getX() - 3, oil.s(), hole.getZ() + 1), Vec3.atCenterOf(hole.below()));
		context.waitTicks(20);
		put(item("wildcatter:drilling_rig"), hole.below(), Direction.UP);
		log("oil rig placed (off camera): " + state(hole));
		powerUp(n, oil, "oil_power", 8200, false, true);
		log("oil field set up off camera: rig " + reading(hole));
		// on camera: the plant as it stands, then the drilling and the strike
		spectator();
		droneAt(n, "oil_site", hole, cam(-6.0, 6.0, -9.0, 4.5, 3.0, 0.5), cam(-8.5, 6.0, -5.0, 4.5, 3.0, 0.5), 200);
		survival();
		drill(n, oil, "oil", 8400);
		startFilm(n, "oil_book");
		book("oil_well", 50);
		stopFilm();
	}

	// ---- 9: the pumpjack -------------------------------------------------------------------------------------------------------------------

	private BlockPos fieldTable;

	private void pumpjack(int n) {
		BlockPos hole = oil.hole();
		// off camera: the dry-type, turbine and boiler moved one block back (the pumpjack's motor box is one block further from the well
		// than the rig's), and a crafting table carried out from the base
		server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (BlockPos p : new BlockPos[] {oil.boiler(), oil.boiler().above(), oil.turbine(), oil.dry(), oil.rod()}) {
				level.removeBlockEntity(p);
				level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
			}
			level.removeBlockEntity(oil.firebox());
			level.setBlock(oil.firebox(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
			return null;
		});
		Site moved = new Site(hole.east(), oil.s());
		stashJunk(item("wildcatter:wool_wire_hot"), Items.TORCH, Items.OAK_FENCE);
		give("wildcatter:dry_type_transformer", 1, "moved one block back");
		give("wildcatter:ground_rod", 2, "moved");
		give("wildcatter:steam_turbine", 1, "moved one block back");
		give("wildcatter:firebox", 1, "moved one block back");
		give("wildcatter:boiler", 1, "moved one block back");
		give("minecraft:coal", 16, "coal mined off camera");
		take(Items.BUCKET, 4);
		give("minecraft:water_bucket", 4, "buckets filled at the pond");
		powerUp(n, moved, "move_power", 8800, false, false);
		put(item("wildcatter:ground_rod"), hole.east(6).south().below(), Direction.UP);   // against the pumpjack's terminal box
		oil = moved;
		fieldTable = new BlockPos(hole.getX() + 3, oil.s(), hole.getZ() + 5);
		give("minecraft:crafting_table", 1, "a crafting table carried out from the base");
		standAt(new BlockPos(fieldTable.getX(), oil.s(), fieldTable.getZ() + 2), Vec3.atCenterOf(fieldTable.below()));
		put(Items.CRAFTING_TABLE, fieldTable.below(), Direction.UP);
		log("moved one block back (off camera): dry-type " + state(oil.dry()) + "; table " + state(fieldTable));
		// the pumpjack, crafted at the field
		give("minecraft:iron_ingot", 5, "iron mined and smelted");
		give("minecraft:piston", 1, "a piston");
		give("minecraft:redstone", 1, "redstone");
		give("minecraft:copper_ingot", 2, "copper");
		fieldCraft(n, "jack_craft", 9000);
		tableCraft(3, new String[] {"iii", "cpc", "iri"}, keys('i', Items.IRON_INGOT, 'c', Items.COPPER_INGOT, 'p', Items.PISTON, 'r', Items.REDSTONE), 1, "pumpjack");
		context.waitTicks(10);
		closeScreen();
		stopFilm();
		// set over the wellhead from behind: from the east, looking west at it
		Vec3 eye = new Vec3(hole.getX() + 3.5, oil.s() + 1.62, hole.getZ() + 2.35);
		float[] yp = yawPitch(eye, Vec3.atCenterOf(hole).add(0, 0.5, 0));
		stand(eye.x, oil.s(), eye.z, yp[0], yp[1]);
		server.runCommand("time set 9300");
		context.waitTicks(wanted(n, "jack_place") ? 80 : 3);
		startFilm(n, "jack_place");
		context.waitTicks(10);
		hold(item("wildcatter:pumpjack"));
		useOn(hole, Direction.UP);
		mark("pumpjack placed: " + state(hole.east()) + "; box " + hole.east(6).toShortString());
		context.waitTicks(20);
		walk(new BlockPos(hole.getX() + 2, oil.s(), hole.getZ() + 6));
		face(Vec3.atCenterOf(hole.east(3)).add(0, 1.5, 0), 20);
		waitQuest("oil_well");
		context.waitTicks(40);
		meter(hole.east());
		stopFilm();
		startFilm(n, "jack_book");
		claimIn("oil_well");   // 12 steel pipe
		stopFilm();
	}

	/** A spectator drone move round {@code origin} (its ground at origin.y - 1), HUD hidden. */
	private void droneAt(int n, String name, BlockPos origin, Bfs03Films.Cam a, Bfs03Films.Cam b, int ticks) {
		t.ox = origin.getX();
		t.gy = origin.getY() - 1;
		t.oz = origin.getZ();
		t.camera(a, true);
		context.waitTicks(wanted(n, name) ? 120 : 2);
		t.camera(a, true);
		context.waitTicks(10);
		startFilm(n, name);
		for (int i = 0; i <= ticks; i++) {
			t.camera(a.lerp(b, (double) i / ticks), false);
			context.waitTick();
		}
		stopFilm();
	}

	/** Lays a pipe in {@code p} by clicking the face of a block beside it that is already there (the ground, a part or the last pipe). */
	private void pipeAt(BlockPos p) {
		Direction from = server.computeOnServer(srv -> {
			for (Direction d : new Direction[] {Direction.DOWN, Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH, Direction.UP}) {
				if (!srv.overworld().getBlockState(p.relative(d)).isAir()) {
					return d;
				}
			}
			return null;
		});
		if (from == null) {
			throw new IllegalStateException("nothing beside " + p.toShortString() + " to lay a pipe against");
		}
		sneakPut(item("wildcatter:gas_line"), p.relative(from), from.getOpposite());
		if (state(p).isAir()) {
			throw new IllegalStateException("no pipe laid at " + p.toShortString() + " (against " + from + ")");
		}
	}

	private void fieldCraft(int n, String film, long time) {
		standAt(new BlockPos(fieldTable.getX(), oil.s(), fieldTable.getZ() + 2), Vec3.atCenterOf(fieldTable));
		server.runCommand("time set " + time);
		context.waitTicks(wanted(n, film) ? 60 : 3);
		startFilm(n, film);
		openBlock(fieldTable);
	}

	// ---- 10: the lease ----------------------------------------------------------------------------------------------------------------------

	private BlockPos sepMain, tankMain, flareAt;

	private void lease(int n) {
		stashJunk(Items.COMPASS);
		give("wildcatter:steel_ingot", 40, "steel from the blast furnace");
		give("minecraft:glass", 1, "glass from the river's sand");
		give("minecraft:flint_and_steel", 1, "flint and steel");
		give("minecraft:copper_ingot", 2, "copper");
		give("minecraft:iron_block", 1, "iron packed into a block");
		give("minecraft:ladder", 1, "a ladder (sticks)");
		fieldCraft(n, "lease_craft", 9600);
		tableCraft(3, new String[] {"sss", "pgp", "sss"}, keys('s', WildcatterItems.STEEL_INGOT, 'p', item("wildcatter:gas_line"), 'g', Items.GLASS), 1, "separator");
		tableCraft(3, new String[] {" f ", " p ", " p "}, keys('f', Items.FLINT_AND_STEEL, 'p', item("wildcatter:gas_line")), 1, "flare stack");
		tableCraft(3, new String[] {"s  ", " s ", "  s"}, keys('s', WildcatterItems.STEEL_INGOT), 1, "crankshaft");
		tableCraft(3, new String[] {"sps", "cxc", "sss"}, keys('s', WildcatterItems.STEEL_INGOT, 'p', item("wildcatter:gas_line"), 'c', Items.COPPER_INGOT,
				'x', item("wildcatter:crankshaft")), 1, "injection pump");
		tableCraft(3, new String[] {"sps", " s ", "sbs"}, keys('s', WildcatterItems.STEEL_INGOT, 'p', item("wildcatter:gas_line"), 'b', Items.IRON_BLOCK), 1, "disposal wellhead");
		context.waitTicks(10);
		closeScreen();
		waitQuest("lease");
		context.waitTicks(10);
		stopFilm();
		fieldCraft(n, "tank_craft", 9800);
		tableCraft(3, new String[] {"sss", "sls", "sss"}, keys('s', WildcatterItems.STEEL_INGOT, 'l', Items.LADDER), 1, "stock tank");
		context.waitTicks(10);
		closeScreen();
		stopFilm();
		startFilm(n, "lease_book");
		book("lease", 50);
		stopFilm();
		// the separator west of the well, facing east (its inlet end towards the well): placed by a player east of it, looking west
		BlockPos hole = new BlockPos(OIL_HOLE_X, oil.s(), OIL_HOLE_Z);
		sepMain = hole.west(3).north();          // cells x -683..-680, z 863..864
		tankMain = hole.west(9).south(2);        // cells x -689..-686, z 866..869
		flareAt = hole.west(6).north(4);
		standAt(hole.north(), new Vec3(sepMain.getX() + 0.5, sepMain.getY(), sepMain.getZ() + 0.5));
		server.runCommand("time set 10000");
		context.waitTicks(wanted(n, "sep_place") ? 80 : 3);
		startFilm(n, "sep_place");
		context.waitTicks(10);
		put(item("wildcatter:separator"), sepMain.below(), Direction.UP);
		mark("separator placed: " + state(sepMain) + " cells " + cellsOf(sepMain));
		// the well's flowline to its front end
		pipeAt(hole.west());
		pipeAt(hole.west(2));
		mark("well to separator: " + reading(hole.west()) + " | separator " + reading(sepMain));
		context.waitTicks(20);
		stopFilm();
		// its oil side (the back end's upper nozzle) to a stock tank south-west of it
		standAt(new BlockPos(tankMain.getX() + 3, oil.s(), tankMain.getZ()), new Vec3(tankMain.getX() + 0.5, tankMain.getY(), tankMain.getZ() + 0.5));
		context.waitTicks(wanted(n, "tank_place") ? 60 : 3);
		startFilm(n, "tank_place");
		context.waitTicks(10);
		put(item("wildcatter:stock_tank"), tankMain.below(), Direction.UP);
		mark("stock tank placed: " + state(tankMain) + " cells " + cellsOf(tankMain));
		BlockPos o1 = hole.west(7).above();
		for (BlockPos p : new BlockPos[] {o1, o1.west(), o1.west().below(), o1.west().below().south(), o1.west().below().south(2)}) {
			pipeAt(p);
		}
		mark("oil side piped: " + reading(o1) + " | separator " + reading(sepMain));
		context.waitTicks(20);
		stopFilm();
		// what goes wrong: no flare. The separator fills with gas and shuts in, and the pumpjack backs up behind it
		standAt(new BlockPos(hole.getX() - 2, oil.s(), hole.getZ() + 2), Vec3.atCenterOf(sepMain));
		context.waitTicks(3);
		for (int i = 0; i < 160 && !reading(sepMain).contains("SHUT IN"); i++) {
			context.waitTicks(10);
		}
		mark("separator: " + reading(sepMain));
		startFilm(n, "mistake");
		context.waitTicks(20);
		meter(sepMain);
		face(Vec3.atCenterOf(hole.east(2)).add(0, 2, 0), 20);
		meter(hole.east());
		stopFilm();
		// the fix: a flare on the gas side (the separator's top, back end), north of it, away from the tank
		standAt(new BlockPos(hole.getX() - 4, oil.s(), hole.getZ() - 3), Vec3.atCenterOf(flareAt));
		context.waitTicks(wanted(n, "flare") ? 60 : 3);
		startFilm(n, "flare");
		context.waitTicks(10);
		put(item("wildcatter:flare_stack"), flareAt.below(), Direction.UP);
		mark("flare placed: " + state(flareAt));
		BlockPos g0 = sepMain.west(3).above(2);   // over the back cell: its top face is the gas nozzle
		for (BlockPos p : new BlockPos[] {g0, g0.north(), g0.north(2), g0.north(2).below(), g0.north(2).below(2)}) {
			pipeAt(p);
		}
		mark("gas side piped: " + reading(g0));
		for (int i = 0; i < 80 && !reading(sepMain).contains("separating"); i++) {
			context.waitTicks(5);
		}
		mark("flare: " + reading(flareAt) + " | separator " + reading(sepMain));
		face(Vec3.atCenterOf(flareAt).add(0, 9, 0), 30);
		context.waitTicks(60);
		face(Vec3.atCenterOf(sepMain), 20);
		meter(sepMain);
		stopFilm();
	}

	private String cellsOf(BlockPos main) {
		return server.computeOnServer(srv -> {
			BlockState s = srv.overworld().getBlockState(main);
			return s.getBlock() instanceof dev.wildcatter.multiblock.BigPart ? dev.wildcatter.multiblock.BigParts.cells(main, s).stream().map(BlockPos::toShortString)
					.toList().toString() : "(not a big part)";
		});
	}

	// ---- 11: crude in the stock tank -------------------------------------------------------------------------------------------------------

	private void crude(int n) {
		BlockPos hole = new BlockPos(OIL_HOLE_X, oil.s(), OIL_HOLE_Z);
		// the tank filling: a fixed camera over the lease for the edit to speed up
		spectator();
		t.ox = hole.getX();
		t.gy = oil.s() - 1;
		t.oz = hole.getZ();
		server.runCommand("time set 10400");
		Bfs03Films.Cam a = cam(-2.0, 9.0, 12.0, -6.0, 2.0, -1.0), b = cam(-3.5, 9.0, 12.5, -6.0, 2.0, -1.0);
		t.camera(a, true);
		context.waitTicks(20);
		startFilm(n, "tank_fill");
		int ticks = 0;
		while (tankLitres() < 1100 && ticks < 4000) {
			t.camera(a.lerp(b, Math.min(1.0, ticks / 2400.0)), false);
			context.waitTick();
			ticks++;
			if (ticks % 200 == 0) {
				mark("tank: " + reading(tankMain) + " | separator " + reading(sepMain) + " | boiler " + reading(oil.boiler()));
				if (boilerWater(oil.boiler()) < 1200) {
					topUp(oil);
				}
			}
		}
		mark("tank at " + tankLitres() + " L after " + ticks + " ticks");
		stopFilm();
		survival();
		standAt(new BlockPos(tankMain.getX() + 1, oil.s(), tankMain.getZ() + 5), Vec3.atCenterOf(tankMain.west().south(2)));
		context.waitTicks(wanted(n, "bucket") ? 60 : 3);
		startFilm(n, "bucket");
		context.waitTicks(10);
		meter(tankMain.west().south(3));
		hold(Items.BUCKET);
		useOn(tankMain.west().south(3), Direction.SOUTH);
		mark("bucket: " + count(item("wildcatter:crude_oil_bucket")) + " crude; tank " + reading(tankMain));
		waitQuest("crude");
		context.waitTicks(30);
		stopFilm();
		startFilm(n, "crude_book");
		claimIn("crude");   // two buckets
		stopFilm();
	}

	private double tankLitres() {
		return server.computeOnServer(srv -> dev.wildcatter.multiblock.BigParts.entity(srv.overworld(), tankMain) instanceof dev.wildcatter.block.StorageTankBlockEntity s
				? s.amount() : 0.0);
	}

	// ---- 12: the hook, the chapter, the lease in late light ----------------------------------------------------------------------------------

	private void done(int n) {
		BlockPos hole = new BlockPos(OIL_HOLE_X, oil.s(), OIL_HOLE_Z);
		t.ox = hole.getX();
		t.gy = oil.s() - 1;
		t.oz = hole.getZ();
		server.runCommand("time set 11200");
		spectator();
		// the hook: low past the pumpjack's horsehead nodding over the well, the stock tank behind
		droneAt(n, "hook_jack", hole, cam(1.5, 2.2, 5.5, -1.0, 2.5, 0.0), cam(-1.0, 2.6, 6.0, -1.0, 2.5, 0.0), 260);
		droneAt(n, "hook_lease", hole, cam(6.0, 7.0, 11.0, -4.0, 2.0, -1.0), cam(-1.0, 7.5, 12.0, -4.0, 2.0, -1.0), 280);
		survival();
		standAt(new BlockPos(hole.getX() - 2, oil.s(), hole.getZ() + 4), Vec3.atCenterOf(hole));
		context.waitTicks(10);
		startFilm(n, "chapter");
		book("crude", 80);
		book("step_up", 60);
		hold(WildcatterItems.QUEST_BOOK);
		useHeld();
		context.waitTicks(6);
		context.runOnClient(c -> {
			if (c.gui.screen() instanceof QuestScreen q) {
				q.open(Identifier.parse("wildcatter:genset"), true);
			}
		});
		mark("next: genset");
		context.waitTicks(80);
		context.setScreen(() -> null);
		stopFilm();
		server.runCommand("time set 12000");
		spectator();
		droneAt(n, "top", hole, cam(14.0, 14.0, 10.0, -3.0, 1.0, -1.0), cam(10.0, 15.0, 15.0, -3.0, 1.0, -1.0), 300);
		survival();
		log("lease at the end: pumpjack " + reading(hole.east()) + " | separator " + reading(sepMain) + " | tank " + reading(tankMain) + " | flare " + reading(flareAt)
				+ " | boiler " + reading(oil.boiler()) + " | gas well " + reading(gas.hole()));
	}
}
