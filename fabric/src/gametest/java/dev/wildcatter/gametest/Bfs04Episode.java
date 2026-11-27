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
 * Built From Scratch, episode 4 "Gas, motors and poles": the rest of Three-phase power (engine and generator parts, the gas genset, a motor on
 * a starter, the MCC, a PLC) and the first half of Distribution (poles and lines, a pole transformer, a pad-mount), played through in
 * survival in the world episode 3 left (Bfs04Films plays episode 3's film code first, nothing filmed). Everything the player does goes
 * through the client: the destroy loop, useItemOn, container clicks with a drawn cursor; what is gathered off camera is given by command
 * and logged, so the edit can label it. The layout at the gas well is the NeoForge proof scene's (Bfs04Scenes.gasGenset). Each take is
 * timed to the narration over it (cue(), from ep04/targets.json, in game time); readouts are the game's own (ReadoutOverlay). Films land in
 * {@code build/scene-videos/bfs04__NN_<shot>.mov}; BFS_SHOTS picks shots by number or name (the rest run unfilmed).
 */
final class Bfs04Episode {
	private final Bfs04Films t;
	private final ClientGameTestContext context;
	private final TestServerContext server;
	private final Set<String> shots;
	private int bx, by, bz;
	private BlockPos table;
	private boolean filming;
	private String filmId = "";

	Bfs04Episode(Bfs04Films t) {
		this(t, pick());
	}

	Bfs04Episode(Bfs04Films t, Set<String> shots) {
		this.t = t;
		this.context = t.context;
		this.server = t.server;
		this.shots = shots;
	}

	private static Set<String> pick() {
		String pick = System.getenv("BFS_SHOTS");
		return pick == null || pick.isBlank() ? null : Set.of(pick.split(","));
	}

	static void log(String s) {
		System.out.println("[bfs04] " + s);
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
		server.runCommand("weather clear");
		server.runCommand("difficulty peaceful");
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
		tickHook();
		pinHook();
		labels();
		loadTargets();
		bx = -568;
		bz = 940;
		by = 65;
		table = new BlockPos(bx, by + 1, bz);
		log("industrial tier " + Voltage.INDUSTRIAL.label() + ", medium " + Voltage.MEDIUM.label() + ", low " + Voltage.LOW.label());
		String[] steps = {"setup", "well", "parts", "engine", "frame", "wet", "separator", "start", "motor", "rig", "mcc", "poles", "dry", "pole1", "line", "base",
				"pad", "ends"};
		for (int i = 0; i < steps.length; i++) {
			int n = i + 1;
			log("step " + n + " " + steps[i]);
			try {
				switch (steps[i]) {
					case "setup" -> setup();
					case "well" -> well(n);
					case "parts" -> parts(n);
					case "engine" -> engine(n);
					case "frame" -> frame(n);
					case "wet" -> wet(n);
					case "separator" -> separator(n);
					case "start" -> start(n);
					case "motor" -> motor(n);
					case "rig" -> rig(n);
					case "mcc" -> mcc(n);
					case "poles" -> poles(n);
					case "dry" -> dry(n);
					case "pole1" -> pole1(n);
					case "line" -> line(n);
					case "base" -> baseEnd(n);
					case "pad" -> pad(n);
					case "ends" -> ends(n);
					default -> {
					}
				}
			} catch (Throwable e) {
				log("step " + steps[i] + " FAILED: " + e);
				e.printStackTrace();
				pin = null;
				stopFilm();
				context.setScreen(() -> null);
				break;
			}
		}
		log("quests done: " + questsDone());
		log("final inventory: " + inventory());
	}

	/** Film only: the receptacle and plug adapter named without their standard's name, the dry-type with its high side's label read from the standard (as episode 3). */
	private void labels() {
		double amps = dev.wildcatter.energy.PlugType.NEMA_5_15.amps();
		String tag = " (" + Voltage.LOW.label() + ", " + Math.round(amps) + " A)";
		Map<String, String> over = Map.of("block.wildcatter.receptacle_nema_5_15", "Receptacle" + tag, "block.wildcatter.plug_nema_5_15", "Plug Adapter" + tag,
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
	}

	// ---- film time -----------------------------------------------------------------------------------------------------------------------

	/** Client ticks since the game started. A take's time is its ticks over 20 (film60: three frames a tick, whatever the clock). */
	private static volatile long clientTicks;
	private static boolean ticking;
	private long filmTick0;

	private void tickHook() {
		if (!ticking) {
			ticking = true;
			net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(c -> clientTicks++);
		}
	}

	/** Seconds of the take filmed so far (game time). */
	private double filmTime() {
		return (clientTicks - filmTick0) / 20.0;
	}

	/**
	 * Waits until the take is {@code sec} seconds in (game time), so each beat lands where its paragraph of narration wants it (the
	 * episode's lengths, videos/longform/built-from-scratch/ep03); off camera it does not wait.
	 */
	private void until(double sec) {
		if (!filming) {
			return;
		}
		int left = (int) Math.ceil((sec - filmTime()) * 20 - 1e-6);
		if (left > 0) {
			context.waitTicks(left);
		} else if (left < -4) {
			log(String.format(Locale.ROOT, "late: %s wanted %.2f s, at %.2f s", filmId, sec, filmTime()));
		}
	}

	/** No readout carried over from the last take. */
	private void clearReadout() {
		context.runOnClient(c -> c.gui.hud.setOverlayMessage(net.minecraft.network.chat.Component.empty(), false));
		context.waitTick();
	}

	// ---- filming ------------------------------------------------------------------------------------------------------------------------

	private Filming film;

	private void startFilm(int n, String name) {
		if (!wanted(n) && !(shots != null && shots.contains(name))) {
			return;
		}
		filmId = String.format(Locale.ROOT, "bfs04__%02d_%s", n, name);
		clearReadout();
		context.waitTicks(2);
		film = context.computeOnClient(client -> Filming.start(client, filmId));
		filmTick0 = clientTicks;
		filming = true;
	}

	private void stopFilm() {
		if (film != null) {
			settle();
			context.waitTicks(10);
			film.stop();
			log(String.format(Locale.ROOT, "filmed %s (%.2f s)", filmId, filmTime()));
			film = null;
		}
		filming = false;
	}

	/** Logs a beat with its time from the film's start (the edit cuts to these). */
	private void mark(String what) {
		if (filming) {
			log(String.format(Locale.ROOT, "event %s %s at %.2f s", filmId, what, filmTime()));
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
		glide(to, glideTicks);
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

	/** The mouse's move to a slot and the pause after a click, in ticks (paceCraft sets them so a craft fills its time). */
	private int glideTicks = 7, clickWait = 5;

	private void click(int slot, int button, ContainerInput kind) {
		moveTo(slot);
		context.runOnClient(c -> {
			AbstractContainerMenu m = c.player.containerMenu;
			c.gameMode.handleContainerInput(m.containerId, slot, button, kind, c.player);
		});
		context.waitTicks(clickWait);
	}

	/** The clicks craft() makes: for each ingredient, the pick-up, a right click a cell a time, the rest put back; the takes and the drop. */
	private static int clicks(String[] pattern, Map<Character, Item> key, int times) {
		int n = 0;
		for (char k : key.keySet()) {
			n += 2;
			for (String row : pattern) {
				for (char ch : row.toCharArray()) {
					if (ch == k) {
						n += times;
					}
				}
			}
		}
		return n + times + 1;
	}

	/** The mouse slowed (never sped past its usual pace) so a craft of {@code clicks} clicks ends about {@code endSec} into the take. */
	private void paceCraft(int clicks, double endSec) {
		glideTicks = 7;
		clickWait = 5;
		if (filming) {
			int per = (int) Math.floor(((endSec - filmTime()) * 20 - 16) / clicks);
			per = Math.max(12, Math.min(30, per));
			glideTicks = Math.max(7, (int) Math.round(per * 0.6));
			clickWait = Math.max(5, per - glideTicks);
		}
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
							if (!carrying()) {
								// the stack ran out: the next stack of the same thing picked up, as a player would
								src = slotOf(e.getValue(), playerFrom);
								left(src);
							}
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
	private Bfs04Films.Cam cam(double x, double y, double z, double tx, double ty, double tz) {
		return new Bfs04Films.Cam(x, y, z, tx, ty, tz);
	}

	/** A spectator drone move between two cameras, HUD hidden. */
	private void drone(int n, String name, Bfs04Films.Cam a, Bfs04Films.Cam b, int ticks) {
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




	private void set(BlockPos p, BlockState s) {
		server.computeOnServer(srv -> srv.overworld().setBlock(p, s, 3));
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
		if (!filming) {
			context.waitTicks(10);
		}
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

	/** A table craft paced to end about {@code endSec} into the take (the mouse slower, every cell still laid one at a time). */
	private void tableCraftBy(double endSec, int width, String[] pattern, Map<Character, Item> key, int times, String what) {
		paceCraft(clicks(pattern, key, times), endSec);
		craft(width, pattern, key, times, 10, 37);
		glideTicks = 7;
		clickWait = 5;
		mark(what + " made");
		context.waitTicks(4);
	}

	private Map<Character, Item> keys(Object... kv) {
		LinkedHashMap<Character, Item> k = new LinkedHashMap<>();
		for (int i = 0; i < kv.length; i += 2) {
			k.put((Character) kv[i], (Item) kv[i + 1]);
		}
		return k;
	}




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
		claimIn(quest, 30, 30);
	}

	/** As claimIn(quest), the page held {@code before} ticks before the Claim button and {@code after} ticks after it. */
	private void claimIn(String quest, int before, int after) {
		hold(WildcatterItems.QUEST_BOOK);
		useHeld();
		context.waitTicks(6);
		context.runOnClient(c -> {
			if (c.gui.screen() instanceof QuestScreen q) {
				q.open(Identifier.parse("wildcatter:" + quest), true);
			}
		});
		context.waitTicks(before);
		claim(quest);
		context.waitTicks(after);
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
		walkGround(toX, toZ, 0);
	}

	/** As walkGround(toX, toZ), over {@code ticks} ticks (0: about four blocks a second). */
	private void walkGround(int toX, int toZ, int ticks) {
		Vec3 from = context.computeOnClient(c -> c.player.position());
		Vec3 end = new Vec3(toX + 0.5, from.y, toZ + 0.5);
		double dist = Math.hypot(end.x - from.x, end.z - from.z);
		if (dist < 0.3) {
			return;
		}
		face(new Vec3(end.x, from.y + 1.5, end.z), 10);
		int steps = ticks > 0 ? ticks : (int) Math.max(6, dist * 5);
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

	private void lookDown(double ahead, int ticks) {
		Vec3 eye = context.computeOnClient(c -> c.player.getEyePosition());
		float yaw = context.computeOnClient(c -> c.player.getYRot());
		double a = Math.toRadians(yaw);
		face(new Vec3(eye.x - Math.sin(a) * ahead, eye.y - 1.62, eye.z + Math.cos(a) * ahead), ticks);
	}

	/** The head turned {@code degrees} (positive: right) and level, over {@code ticks}. */
	private void lookAround(double degrees, int ticks) {
		Vec3 eye = context.computeOnClient(c -> c.player.getEyePosition());
		float yaw = context.computeOnClient(c -> c.player.getYRot());
		double a = Math.toRadians(yaw + degrees);
		face(new Vec3(eye.x - Math.sin(a) * 10, eye.y - 0.8, eye.z + Math.cos(a) * 10), ticks);
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
	private void droneAt(int n, String name, BlockPos origin, Bfs04Films.Cam a, Bfs04Films.Cam b, int ticks) {
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

	/** As sneakPut, at a working pace for a run of the same piece (the item already in hand stays; a short turn and wait). */
	private void quickSneakPut(Item what, BlockPos on, Direction face) {
		boolean inHand = context.computeOnClient(c -> c.player.getMainHandItem().is(what));
		if (!inHand) {
			hold(what);
		}
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(2);
		Vec3 at = Vec3.atCenterOf(on).add(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(0.5));
		face(at, 4);
		context.runOnClient(c -> {
			BlockHitResult hit = new BlockHitResult(at, face, on, false);
			if (c.gameMode.useItemOn(c.player, InteractionHand.MAIN_HAND, hit).consumesAction()) {
				c.player.swing(InteractionHand.MAIN_HAND, c.player.getMainHandItem().getAttackAnimation(), false);
			}
		});
		context.waitTicks(3);
		context.getInput().releaseKey(o -> o.keyShift);
		context.waitTicks(2);
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
		quickSneakPut(item("wildcatter:gas_line"), p.relative(from), from.getOpposite());
		if (state(p).isAir()) {
			throw new IllegalStateException("no pipe laid at " + p.toShortString() + " (against " + from + ")");
		}
	}

	private String cellsOf(BlockPos main) {
		return server.computeOnServer(srv -> {
			BlockState s = srv.overworld().getBlockState(main);
			return s.getBlock() instanceof dev.wildcatter.multiblock.BigPart ? dev.wildcatter.multiblock.BigParts.cells(main, s).stream().map(BlockPos::toShortString)
					.toList().toString() : "(not a big part)";
		});
	}

	// ---- paragraph cues ---------------------------------------------------------------------------------------------------------------
	//
	// Each stretch of a take is timed to the narration over it (REFILM_BRIEF.md: the narration voiced first, then every take timed to its
	// paragraph): the longer of the English and the Spanish paragraph, as build_ep.py lays it, and a second, times the speed it is shown at.
	// The lengths come from ep04/targets.json (BFS_TARGETS); cue() starts a stretch and logs it for the spec, the next cue (or the film's
	// end) first waits out what is left of it, and at() places a beat a share of the way through it.

	private final Map<String, Double> targets = new java.util.HashMap<>();
	private String cueName;
	private double cueAt, cueLen;

	private void loadTargets() {
		String path = System.getenv("BFS_TARGETS");
		if (path == null) {
			path = "/Users/timhunsaker/minecraft-mods/videos/longform/built-from-scratch/ep04/targets.json";
		}
		try {
			var json = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(java.nio.file.Path.of(path))).getAsJsonObject();
			for (var e : json.entrySet()) {
				targets.put(e.getKey(), e.getValue().getAsDouble());
			}
			log("targets from " + path + ": " + targets.size());
		} catch (Exception e) {
			log("no targets (" + e + "): every cue 6 s");
		}
	}

	private void cue(String name) {
		if (!filming) {
			return;
		}
		settle();
		cueName = name;
		cueAt = filmTime();
		cueLen = targets.getOrDefault(name, 6.0);
		log(String.format(Locale.ROOT, "cue %s %s at %.3f s target %.2f s", filmId, name, cueAt, cueLen));
	}

	/** Waits out what is left of the current cue (the action ran short), or logs how far it ran over. */
	private void settle() {
		if (!filming || cueName == null) {
			return;
		}
		double end = cueAt + cueLen, now = filmTime();
		if (now > end + 0.1) {
			log(String.format(Locale.ROOT, "OVERRUN %s %s by %.2f s (ran %.2f of %.2f)", filmId, cueName, now - end, now - cueAt, cueLen));
		} else {
			until(end);
		}
		cueName = null;
	}

	/** Waits until {@code share} of the current cue has gone by. */
	private void at(double share) {
		if (filming && cueName != null) {
			until(cueAt + share * cueLen);
		}
	}

	/** The film time {@code share} of the way through the current cue (for paced crafts). */
	private double by(double share) {
		return filming && cueName != null ? cueAt + share * cueLen : 0;
	}

	// ---- a pinned player: held still in the air while climbing a pole (the pole is climbable; the feet are inside its cell) ----------

	private static volatile Vec3 pin;
	private static boolean pinned;

	private void pinHook() {
		if (!pinned) {
			pinned = true;
			net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.START_CLIENT_TICK.register(c -> {
				Vec3 p = pin;
				if (p != null && c.player != null) {
					c.player.setPos(p.x, p.y, p.z);
					c.player.setDeltaMovement(Vec3.ZERO);
				}
			});
		}
	}

	/** Climbs the pole at {@code pole} (its ground cell) from its {@code side}, the feet to {@code feetY}, a block every six ticks. */
	private void climb(BlockPos pole, Direction side, double feetY) {
		Vec3 from = context.computeOnClient(c -> c.player.position());
		Vec3 at = new Vec3(pole.getX() + 0.5 + side.getStepX() * 0.43, from.y, pole.getZ() + 0.5 + side.getStepZ() * 0.43);
		// step into the pole's cell on its side, facing it
		for (int i = 1; i <= 8; i++) {
			Vec3 p = from.lerp(at, i / 8.0);
			pin = p;
			context.waitTick();
		}
		face(new Vec3(pole.getX() + 0.5, feetY + 1.4, pole.getZ() + 0.5), 8);
		int steps = (int) Math.max(1, Math.round((feetY - at.y) * 6));
		for (int i = 1; i <= steps; i++) {
			double s = (double) i / steps;
			pin = new Vec3(at.x, at.y + (feetY - at.y) * s, at.z);
			context.waitTick();
		}
		mark("climbed to " + feetY);
	}

	/** Back down the pole to the ground beside it, and the pin let go. */
	private void climbDown(BlockPos ground) {
		Vec3 p0 = pin != null ? pin : context.computeOnClient(c -> c.player.position());
		int steps = (int) Math.max(1, Math.round((p0.y - ground.getY()) * 4));
		for (int i = 1; i <= steps; i++) {
			double s = (double) i / steps;
			pin = new Vec3(p0.x, p0.y + (ground.getY() - p0.y) * s, p0.z);
			context.waitTick();
		}
		Vec3 out = Vec3.atBottomCenterOf(ground);
		for (int i = 1; i <= 6; i++) {
			pin = new Vec3(p0.x, ground.getY(), p0.z).lerp(out, i / 6.0);
			context.waitTick();
		}
		pin = null;
		context.waitTicks(2);
	}

	// ---- positions ---------------------------------------------------------------------------------------------------------------------
	// The gas well site (PLAN.md "Episode 3's base"), levelled at ground top 63: the player stands at y 64. The layout is the proof scene's
	// (neoforge Bfs04Scenes.gasGenset), relative to the tree at -478, 64, 980.

	static final int G = 63, S = 64;
	static final BlockPos TREE = new BlockPos(-478, S, 980);
	static final BlockPos TABLE = new BlockPos(-484, S, 985);
	static final BlockPos FRAME = new BlockPos(-480, G, 984);      // the base frame's main cell, a block into the ground
	static final BlockPos RADIATOR = new BlockPos(-480, S, 984), ENGINE = new BlockPos(-479, S, 984), ALTERNATOR = new BlockPos(-475, S, 984),
			PANEL = new BlockPos(-474, S, 984);
	static final BlockPos STARTER = new BlockPos(-473, S, 984);    // against the panel's north terminal
	static final BlockPos HOLE = new BlockPos(-467, S, 984);       // the rig's mast; its terminal box at -472, against the starter
	static final BlockPos DRY = new BlockPos(-473, S, 985);        // against the panel's south terminal, its high side west
	static final BlockPos SEP = new BlockPos(-477, S, 981);        // the separator's main cell (its front end, west): cells x -477..-474, z 980..981
	static final BlockPos[] WET = {new BlockPos(-478, S, 981), new BlockPos(-478, S, 982), new BlockPos(-478, S, 983)};
	static final BlockPos[] SEP_GAS = {new BlockPos(-474, 66, 981), new BlockPos(-474, 66, 982), new BlockPos(-474, 66, 983), new BlockPos(-475, 66, 983),
			new BlockPos(-476, 66, 983), new BlockPos(-476, 65, 983)};
	static final BlockPos[] CABLE1 = {new BlockPos(-473, S, 986), new BlockPos(-473, S, 987), new BlockPos(-473, 65, 987), new BlockPos(-473, 66, 987),
			new BlockPos(-473, 67, 987)};
	static final BlockPos P1 = new BlockPos(-473, S, 988);          // the first pole's ground cell (8 high: y 64-71); its unit at y 68 on the north side
	/** The rest of the line west to the base (planned; each set on the nearest dry, open ground, PLAN.md "Episode 4's base"). */
	static final int[][] ROUTE = {{-499, 972}, {-520, 966}, {-552, 954}};
	private final List<BlockPos> poles = new ArrayList<>();

	private BlockPos unitOf(BlockPos pole) {
		return pole.above(4).north();
	}

	private BlockPos insulatorOf(BlockPos pole) {
		return pole.above(8);
	}

	// ---- 1: off camera, to the gas well ------------------------------------------------------------------------------------------------

	private void setup() {
		server.runCommand("forceload add -580 930 -455 1000");
		if (System.getenv("BFS_QUICK3") != null) {
			quickThree();
		}
		server.runCommand("time set 1000");
		survival();
		standAt(new BlockPos(-484, S, 977), Vec3.atCenterOf(TREE));
		context.waitTicks(120);
		corridorGrid();
		// what this episode never uses put away at the base, off camera
		stashJunk(item("wildcatter:seismograph"), item("wildcatter:disposal_wellhead"), item("wildcatter:injection_pump"), item("wildcatter:crude_oil_bucket"),
				Items.BUCKET, Items.WATER_BUCKET, item("minecraft:leaf_litter"));
		// a crafting table carried out here; the ground under the base frame dug out, a block deep
		set(TABLE, Blocks.CRAFTING_TABLE.defaultBlockState());
		for (int x = -480; x <= -474; x++) {
			for (int z = 984; z <= 985; z++) {
				set(new BlockPos(x, G, z), Blocks.AIR.defaultBlockState());
			}
		}
		log("off camera: a crafting table at " + TABLE.toShortString() + "; the frame's trench dug x -480..-474, z 984..985 at y " + G);
		// the pole line's ground: each pole on the nearest dry, open column, the trees along the line cleared
		poles.add(P1);
		for (int[] r : ROUTE) {
			poles.add(poleSite(r[0], r[1]));
		}
		for (int i = 0; i + 1 < poles.size(); i++) {
			clearAlong(poles.get(i), poles.get(i + 1));
			log(String.format(Locale.ROOT, "pole %d %s to pole %d %s: span %.1f blocks", i + 1, poles.get(i).toShortString(), i + 2, poles.get(i + 1).toShortString(),
					Math.sqrt(insulatorOf(poles.get(i)).distSqr(insulatorOf(poles.get(i + 1))))));
		}
		log("the gas well: " + reading(TREE) + "; pocket " + inventory());
	}

	/**
	 * Working runs only (BFS_QUICK3, never a take): the little of episode 3's world this episode touches, set by command instead of
	 * playing episode 3 through: the gas well site levelled with its tree, the base's table, the chapters done, episode 3's pocket.
	 */
	private void quickThree() {
		server.runCommand("tp @p -480 80 978");
		context.waitTicks(200);
		levelSite(-486, 973, -462, 988, G, 8);
		set(TREE, block("wildcatter:gas_wellhead").defaultBlockState());
		set(table, Blocks.CRAFTING_TABLE.defaultBlockState());
		server.computeOnServer(srv -> {
			var p = dev.wildcatter.quest.QuestData.get(srv).of(sp(srv).getStringUUID());
			for (String q : new String[] {"wood", "stone", "campfire", "iron", "copper", "steel", "field_manual", "steam", "lamp", "protection", "firebox", "plugs",
					"step_up", "seismograph", "rig", "gas_well", "oil_well", "lease", "crude"}) {
				p.done.add("wildcatter:" + q);
			}
			dev.wildcatter.quest.QuestData.get(srv).changed();
			var inv = sp(srv).getInventory();
			inv.clearContent();
			Object[][] pocket = {{0, WildcatterItems.QUEST_BOOK, 1}, {1, WildcatterItems.FIELD_MANUAL, 1}, {3, WildcatterItems.POWER_METER, 1}, {4, item("wildcatter:ground_rod"), 1},
					{8, item("wildcatter:gas_line"), 12}, {9, WildcatterItems.STEEL_INGOT, 27}, {10, Items.COPPER_INGOT, 7}, {11, item("wildcatter:drilling_rig"), 1},
					{13, Items.STONE_PICKAXE, 1}, {32, item("wildcatter:piston"), 2}};
			for (Object[] o : pocket) {
				inv.setItem((Integer) o[0], new ItemStack((Item) o[1], (Integer) o[2]));
			}
			return null;
		});
		context.waitTicks(20);
		log("QUICK: episode 3's world set by command (a working run, not a take)");
	}

	/** A height map of the pole line's ground, for the plan (the last two digits of the ground's top; ~ water, T a log). */
	private void corridorGrid() {
		for (int z = 940; z <= 995; z += 1) {
			final int zz = z;
			String row = server.computeOnServer(srv -> {
				ServerLevel level = srv.overworld();
				StringBuilder b = new StringBuilder(String.format(Locale.ROOT, "%5d ", zz));
				for (int x = -575; x <= -460; x++) {
					int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, zz) - 1;
					BlockPos p = new BlockPos(x, top, zz);
					BlockState st = level.getBlockState(p);
					char c = !level.getFluidState(p).isEmpty() ? '~' : st.is(BlockTags.LOGS) ? 'T' : ' ';
					b.append(String.format(Locale.ROOT, "%02d%c", Math.floorMod(top, 100), c));
				}
				return b.toString();
			});
			log("grid " + row);
		}
	}

	/** The nearest column (within 5) to (x, z) whose top is dry, solid ground with no log or leaves over it: the pole's ground cell. */
	private BlockPos poleSite(int x, int z) {
		return server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (int r = 0; r <= 5; r++) {
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
							continue;
						}
						int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz) - 1;
						BlockPos g = new BlockPos(x + dx, top, z + dz);
						BlockState st = level.getBlockState(g);
						if (level.getFluidState(g).isEmpty() && level.getFluidState(g.above()).isEmpty() && !st.is(BlockTags.LOGS) && !st.is(BlockTags.LEAVES)
								&& st.isFaceSturdy(level, g, Direction.UP)) {
							return g.above();
						}
					}
				}
			}
			throw new IllegalStateException("no dry ground for a pole near " + x + ", " + z);
		});
	}

	/** Off camera: the trees and tall plants within three blocks of the line between two poles taken down (and round each pole). */
	private void clearAlong(BlockPos a, BlockPos b) {
		int n = server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			int k = 0;
			double len = Math.sqrt(a.distSqr(b));
			for (int i = 0; i <= len; i++) {
				double s = i / Math.max(1, len);
				int cx = (int) Math.round(a.getX() + (b.getX() - a.getX()) * s), cz = (int) Math.round(a.getZ() + (b.getZ() - a.getZ()) * s);
				int base = Math.min(a.getY(), b.getY()) - 1;
				for (BlockPos p : BlockPos.betweenClosed(cx - 3, base, cz - 3, cx + 3, base + 24, cz + 3)) {
					BlockState st = level.getBlockState(p);
					if (st.is(BlockTags.LOGS) || st.is(BlockTags.LEAVES) || p.getY() <= base + 3 && !st.isAir() && st.canBeReplaced() && st.getFluidState().isEmpty()
							|| st.is(BlockTags.FLOWERS)) {
						level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
						k++;
					}
				}
			}
			return k;
		});
		log("cleared off camera along " + a.toShortString() + " to " + b.toShortString() + ": " + n + " cells");
	}

	private void atTable(int n, String film, long time) {
		standAt(TABLE.south(2), Vec3.atCenterOf(TABLE));
		server.runCommand("time set " + time);
		context.waitTicks(wanted(n, film) ? 60 : 3);
		startFilm(n, film);
	}

	// ---- 2: the gas well as episode 3 left it (P3) -------------------------------------------------------------------------------------

	private void well(int n) {
		standAt(new BlockPos(-484, S, 977), Vec3.atCenterOf(TREE).add(0, 0.5, 0));
		server.runCommand("time set 1200");
		context.waitTicks(wanted(n, "well") ? 80 : 3);
		startFilm(n, "well");
		cue("P03");
		at(0.05);
		walk(new BlockPos(-481, S, 979));
		at(0.25);
		handRead(TREE);
		at(0.75);
		face(Vec3.atCenterOf(TREE).add(0, -0.3, 0.6), 30);
		settle();
		stopFilm();
	}

	// ---- 3: engine and generator parts (P4-P6) --------------------------------------------------------------------------------------------

	private void parts(int n) {
		give("minecraft:iron_block", 2, "iron mined and smelted, packed into blocks");
		give("minecraft:iron_ingot", 4, "iron mined and smelted");
		give("minecraft:copper_ingot", 24, "copper mined and smelted");
		give("minecraft:redstone", 2, "redstone");
		give("minecraft:comparator", 1, "a comparator (redstone torches, quartz, stone)");
		give("wildcatter:steel_ingot", 24, "steel from the blast furnace");
		// P4: the book's page, the six parts
		standAt(TABLE.south(2), Vec3.atCenterOf(TABLE));
		server.runCommand("time set 1400");
		context.waitTicks(wanted(n, "parts_page") ? 60 : 3);
		startFilm(n, "parts_page");
		cue("P04");
		at(0.08);
		bookOpen("engine_parts");
		at(0.97);
		context.setScreen(() -> null);
		settle();
		stopFilm();
		// P5 and P6 (3x): the six crafts, the quest
		atTable(n, "parts_craft", 1500);
		cue("P05");
		openBlock(TABLE);
		var s = WildcatterItems.STEEL_INGOT;
		tableCraftBy(by(0.36), 3, new String[] {"sss", "sbs", "sss"}, keys('s', s, 'b', Items.IRON_BLOCK), 1, "engine block casting");
		tableCraftBy(by(0.62), 3, new String[] {"s  ", " s ", "  s"}, keys('s', s), 1, "crankshaft");
		tableCraftBy(by(0.95), 3, new String[] {"sss", "i i"}, keys('s', s, 'i', Items.IRON_INGOT), 1, "cylinder head");
		cue("P06");
		tableCraftBy(by(0.30), 3, new String[] {"ccc", "csc", "ccc"}, keys('c', Items.COPPER_INGOT, 's', s), 1, "stator winding");
		tableCraftBy(by(0.58), 3, new String[] {"csc", "sbs", "csc"}, keys('c', Items.COPPER_INGOT, 's', s, 'b', Items.IRON_BLOCK), 1, "rotor field");
		tableCraftBy(by(0.82), 3, new String[] {"crc", "sds"}, keys('c', Items.COPPER_INGOT, 'r', Items.REDSTONE, 's', s, 'd', Items.COMPARATOR), 1, "exciter");
		closeScreen();
		waitQuest("engine_parts");
		settle();
		stopFilm();
		standAt(TABLE.south(2).east(), Vec3.atCenterOf(TABLE));
		context.waitTicks(wanted(n, "parts_claim") ? 40 : 3);
		startFilm(n, "parts_claim");
		cue("P06b");
		claimIn("engine_parts", 30, 50);   // four spark plugs
		settle();
		stopFilm();
	}

	// ---- 4: the engine and the alternator (P7, P8) -----------------------------------------------------------------------------------------

	private void engine(int n) {
		give("minecraft:iron_nugget", 1, "an iron ingot crafted into nuggets");
		atTable(n, "engine_craft", 1700);
		cue("P07");
		openBlock(TABLE);
		var s = WildcatterItems.STEEL_INGOT;
		tableCraftBy(by(0.32), 3, new String[] {"ssn"}, keys('s', s, 'n', Items.IRON_NUGGET), 1, "pistons and rods");
		tableCraftBy(by(0.93), 3, new String[] {"ihi", "pcp", "pbp"}, keys('i', item("wildcatter:spark_plug"), 'h', item("wildcatter:cylinder_head"),
				'p', item("wildcatter:piston"), 'c', item("wildcatter:crankshaft"), 'b', item("wildcatter:engine_block_casting")), 1, "gas engine");
		settle();
		cue("P08");
		tableCraftBy(by(0.28), 3, new String[] {"ccc", "csc", "ccc"}, keys('c', Items.COPPER_INGOT, 's', s), 1, "a second stator winding");
		tableCraftBy(by(0.52), 3, new String[] {"ccc", "csc", "ccc"}, keys('c', Items.COPPER_INGOT, 's', s), 1, "a third stator winding");
		tableCraftBy(by(0.92), 3, new String[] {"sws", "wrw", "sxs"}, keys('s', s, 'w', item("wildcatter:stator_winding"), 'r', item("wildcatter:rotor_field"),
				'x', item("wildcatter:exciter")), 1, "alternator");
		closeScreen();
		settle();
		stopFilm();
		give("wildcatter:genset_base_frame", 1, "the base frame: two steel over an iron block, twice");
		give("wildcatter:radiator", 1, "the radiator: steel, iron bars and a copper block");
		give("wildcatter:genset_control_panel", 1, "the control panel: steel, glass, redstone, a three-pole breaker and a comparator");
		give("wildcatter:exhaust_silencer_large", 1, "the large silencer: eight small silencers");
		log("after the parts: " + inventory());
	}

	// ---- 5: the set on its frame (P9, P10) ------------------------------------------------------------------------------------------------

	private void frame(int n) {
		// the frame faces the player (its front, the readout side, north): placed looking south, from the north
		BlockPos stand = new BlockPos(-480, S, 981);
		standAt(stand, Vec3.atCenterOf(FRAME));
		server.runCommand("time set 2000");
		context.waitTicks(wanted(n, "frame_place") ? 80 : 3);
		startFilm(n, "frame_place");
		cue("P09");
		at(0.06);
		hold(item("wildcatter:genset_base_frame"));
		at(0.16);
		useOn(FRAME.below(), Direction.UP);
		mark("base frame: " + state(FRAME) + " cells " + cellsOf(FRAME));
		at(0.32);
		walk(new BlockPos(-477, S, 982));
		placePart(0.50, "wildcatter:radiator", RADIATOR);
		placePart(0.64, "wildcatter:genset_gas_engine", ENGINE);
		placePart(0.80, "wildcatter:alternator", ALTERNATOR);
		placePart(0.92, "wildcatter:genset_control_panel", PANEL);
		settle();
		cue("P10");
		at(0.06);
		sneakPut(item("wildcatter:exhaust_silencer_large"), ENGINE.east(1).south().above(), Direction.UP);   // the top of the engine, under its slot
		mark("silencer: " + state(ENGINE.east(1).above(2)) + "; panel " + reading(PANEL));
		at(0.30);
		face(Vec3.atCenterOf(ENGINE).add(1.5, 2.2, 0.5), 40);
		at(0.55);
		walk(new BlockPos(-472, S, 982));
		face(Vec3.atCenterOf(PANEL).add(0.55, -0.1, 0.5), 30);
		mark("the panel's east end: its terminals");
		settle();
		stopFilm();
	}

	/** A genset part clicked on the frame's top where its slot is, at {@code share} of the cue. */
	private void placePart(double share, String id, BlockPos slot) {
		at(share - 0.07);
		hold(item(id));
		face(Vec3.atCenterOf(slot.below()).add(0, 0.5, 0), 8);
		at(share);
		sneakPut(item(id), slot.below(), Direction.UP);   // sneaking: a plain click on the frame reads it
		mark(id + ": " + state(slot) + " cells " + cellsOf(slot));
	}

	/** The action-bar text the player sees now (for the log). */
	private String overlay() {
		return context.computeOnClient(c -> {
			try {
				for (java.lang.reflect.Field f : c.gui.hud.getClass().getDeclaredFields()) {
					if (f.getType() == net.minecraft.network.chat.Component.class) {
						f.setAccessible(true);
						Object v = f.get(c.gui.hud);
						if (v != null && !((net.minecraft.network.chat.Component) v).getString().isEmpty()) {
							return ((net.minecraft.network.chat.Component) v).getString();
						}
					}
				}
			} catch (ReflectiveOperationException e) {
				return "(" + e + ")";
			}
			return "";
		});
	}

	// ---- 6: what goes wrong: wet gas (P11, P12) -------------------------------------------------------------------------------------------

	private void wet(int n) {
		standAt(new BlockPos(-476, S, 982), Vec3.atCenterOf(WET[1]));
		server.runCommand("time set 2300");
		context.waitTicks(wanted(n, "wet_gas") ? 80 : 3);
		startFilm(n, "wet_gas");
		cue("P11");
		at(0.30);
		pipeAt(WET[0]);
		at(0.45);
		pipeAt(WET[1]);
		at(0.60);
		pipeAt(WET[2]);
		mark("tree to engine: " + reading(ENGINE));
		at(0.72);
		face(Vec3.atCenterOf(WET[2]).add(0, -0.2, 0.5), 30);
		mark("the flange at the engine");
		settle();
		cue("P12");
		walk(new BlockPos(-474, S, 982));
		emptyHand();
		face(Vec3.atCenterOf(PANEL).add(0, 0.1, -0.5), 8);
		at(0.12);
		useOn(PANEL, Direction.NORTH);
		mark("start pressed: " + reading(PANEL));
		at(0.55);
		face(Vec3.atCenterOf(PANEL).add(0, 0.6, -0.5), 30);
		mark("the panel: " + reading(PANEL));
		settle();
		stopFilm();
	}

	// ---- 7: the fix: a separator between (P13) ---------------------------------------------------------------------------------------------

	private void separator(int n) {
		give("wildcatter:separator", 1, "a second separator, made as in episode three");
		standAt(new BlockPos(-480, S, 982), Vec3.atCenterOf(WET[2]));
		server.runCommand("time set 2600");
		context.waitTicks(wanted(n, "sep_place") ? 80 : 3);
		startFilm(n, "sep_place");
		cue("P13");
		hold(Items.STONE_PICKAXE);
		at(0.04);
		dig(WET[2], 60);
		dig(WET[1], 60);
		pickUpNear(WET[1], 3.0);
		mark("the straight run taken up");
		at(0.30);
		face(Vec3.atCenterOf(SEP.below()).add(0, 0.5, 0), 10);
		put(item("wildcatter:separator"), SEP.below(), Direction.UP);
		mark("separator: " + state(SEP) + " cells " + cellsOf(SEP));
		at(0.45);
		walk(new BlockPos(-475, S, 983));
		for (int i = 0; i < SEP_GAS.length; i++) {
			at(0.50 + 0.07 * i);
			pipeAt(SEP_GAS[i]);
		}
		mark("separator's gas to the engine: " + reading(SEP));
		settle();
		stopFilm();
	}

	// ---- 8: the set runs (P14, P15) ---------------------------------------------------------------------------------------------------------

	private void start(int n) {
		standAt(new BlockPos(-474, S, 982), Vec3.atCenterOf(PANEL).add(0, 0.1, -0.5));
		server.runCommand("time set 2800");
		context.waitTicks(wanted(n, "gas_start") ? 80 : 3);
		startFilm(n, "gas_start");
		cue("P14");
		emptyHand();
		at(0.08);
		useOn(PANEL, Direction.NORTH);
		mark("start pressed: " + reading(PANEL));
		for (int i = 0; i < 400 && !reading(PANEL).contains("RUNNING"); i++) {
			context.waitTick();
		}
		mark("running: " + reading(PANEL));
		at(0.40);
		face(Vec3.atCenterOf(ENGINE).add(1.5, 2.4, 0.5), 40);
		waitQuest("genset");
		settle();
		cue("P15");
		face(Vec3.atCenterOf(PANEL).add(0, 0.1, -0.5), 20);
		meter(PANEL);
		at(0.50);
		walk(new BlockPos(-479, S, 982));
		meter(TREE);
		settle();
		stopFilm();
	}

	// ---- 9: start a motor (P16-P20) -------------------------------------------------------------------------------------------------------

	private void motor(int n) {
		give("wildcatter:breaker", 6, "six breakers, made as in episode two");
		give("minecraft:redstone_block", 2, "redstone packed into blocks");
		give("minecraft:repeater", 1, "a repeater");
		give("minecraft:iron_ingot", 2, "iron");
		give("minecraft:red_dye", 2, "red dye (poppies)");
		give("minecraft:copper_ingot", 12, "copper mined and smelted");
		atTable(n, "motor_craft", 3000);
		cue("P16");   // 3x
		openBlock(TABLE);
		var s = WildcatterItems.STEEL_INGOT;
		var br = item("wildcatter:breaker");
		tableCraftBy(by(0.22), 3, new String[] {"bbb", "srs"}, keys('b', br, 's', s, 'r', Items.REDSTONE_BLOCK), 1, "three-pole breaker");
		tableCraftBy(by(0.40), 3, new String[] {"bbb", "srs"}, keys('b', br, 's', s, 'r', Items.REDSTONE_BLOCK), 1, "a second three-pole breaker");
		tableCraftBy(by(0.66), 3, new String[] {"sbs", "crc", "sss"}, keys('s', s, 'b', item("wildcatter:breaker_3p_industrial"), 'c', Items.COPPER_INGOT,
				'r', Items.REPEATER), 1, "motor starter");
		tableCraftBy(by(0.82), 3, new String[] {"ccc", "cid"}, keys('c', Items.COPPER_INGOT, 'i', Items.IRON_INGOT, 'd', item("minecraft:red_dye")), 1, "industrial wire");
		tableCraftBy(by(0.97), 3, new String[] {"ccc", "cid"}, keys('c', Items.COPPER_INGOT, 'i', Items.IRON_INGOT, 'd', item("minecraft:red_dye")), 1, "industrial wire");
		closeScreen();
		settle();
		stopFilm();
		log("motor parts: " + inventory());
	}

	private void rig(int n) {
		// P17: the starter against the panel's north terminal (placed from the east, so it faces east), then the rig over the field's far side
		standAt(new BlockPos(-470, S, 984), Vec3.atCenterOf(STARTER.below()).add(0, 0.5, 0));
		server.runCommand("time set 3200");
		context.waitTicks(wanted(n, "rig_place") ? 80 : 3);
		startFilm(n, "rig_place");
		cue("P17");
		at(0.06);
		put(item("wildcatter:motor_starter"), STARTER.below(), Direction.UP);
		mark("starter: " + state(STARTER));
		at(0.30);
		walk(new BlockPos(-463, S, 984));
		hold(item("wildcatter:drilling_rig"));
		face(Vec3.atCenterOf(HOLE.below()).add(0, 0.5, 0), 20);
		at(0.45);
		useOn(HOLE.below(), Direction.UP);
		mark("rig: " + state(HOLE) + "; box " + dev.wildcatter.block.DrillingRigBlock.terminalBox(HOLE, state(HOLE)).toShortString());
		at(0.62);
		face(Vec3.atCenterOf(HOLE).add(0, 6, 0), 40);
		at(0.80);
		face(Vec3.atCenterOf(STARTER), 40);
		settle();
		stopFilm();
		// P18: the overload dial stepped to the rig's 45 A (sneak and right-click)
		standAt(new BlockPos(-471, S, 982), Vec3.atCenterOf(STARTER));
		context.waitTicks(wanted(n, "dial") ? 60 : 3);
		startFilm(n, "dial");
		cue("P18");
		at(0.08);
		meter(STARTER);   // its readout: stopped, overload set to 11 A (a plain click would start it)
		emptyHand();
		for (int i = 0; i < 8 && !reading(STARTER).contains("set to 45.0 A"); i++) {
			at(0.30 + 0.09 * i);
			context.getInput().holdKey(o -> o.keyShift);
			context.waitTicks(3);
			useOn(STARTER, faceToward(STARTER));
			context.getInput().releaseKey(o -> o.keyShift);
			mark("dial stepped: " + reading(STARTER));
		}
		settle();
		// P19: started; the quest
		cue("P19");
		at(0.08);
		useOn(STARTER, faceToward(STARTER));
		mark("starter pressed: " + reading(STARTER));
		at(0.25);
		face(Vec3.atCenterOf(HOLE).add(0, 5, 0), 40);
		mark("rig: " + reading(HOLE));
		waitQuest("motor");
		mark("rig: " + reading(HOLE) + " | starter " + reading(STARTER));
		settle();
		stopFilm();
		standAt(new BlockPos(-471, S, 981), Vec3.atCenterOf(HOLE).add(0, 3, 0));
		context.waitTicks(wanted(n, "motor_claim") ? 30 : 3);
		startFilm(n, "motor_claim");
		cue("P19b");
		claimIn("motor", 30, 40);   // eight industrial wire
		settle();
		stopFilm();
		// P20 (3x): the rig drilling, from the south-east (spectator, the HUD hidden)
		spectator();
		t.ox = HOLE.getX();
		t.gy = G;
		t.oz = HOLE.getZ();
		Bfs04Films.Cam a = cam(6.0, 5.5, 9.0, -3.0, 3.0, 0.0), b = cam(2.0, 5.0, 10.0, -3.0, 3.0, 0.0);
		t.camera(a, true);
		context.waitTicks(wanted(n, "rig_drill") ? 60 : 3);
		startFilm(n, "rig_drill");
		cue("P20");
		double len = cueLen;
		int ticks = (int) Math.max(40, len * 20);
		for (int i = 0; i <= ticks; i++) {
			t.camera(a.lerp(b, (double) i / ticks), false);
			context.waitTick();
		}
		mark("drilling: " + reading(HOLE));
		settle();
		stopFilm();
		survival();
	}

	// ---- 10: the MCC and the PLC (P21, P22) -----------------------------------------------------------------------------------------------

	private void mcc(int n) {
		give("minecraft:copper_block", 3, "copper packed into blocks");
		give("minecraft:iron_bars", 2, "iron bars");
		give("minecraft:iron_block", 1, "iron packed into a block");
		give("minecraft:repeater", 1, "a repeater");
		give("minecraft:quartz", 4, "nether quartz");
		give("minecraft:comparator", 2, "comparators");
		give("minecraft:redstone", 2, "redstone");
		give("minecraft:gold_ingot", 1, "a gold ingot");
		give("wildcatter:steel_ingot", 20, "steel from the blast furnace");
		var s = WildcatterItems.STEEL_INGOT;
		atTable(n, "mcc_craft", 3600);
		cue("P21");   // 3x
		openBlock(TABLE);
		tableCraftBy(by(0.55), 3, new String[] {"sbs", "ccc", "sss"}, keys('s', s, 'b', item("wildcatter:breaker_3p_industrial"), 'c', item("minecraft:copper_block")), 1,
				"MCC incoming section");
		tableCraftBy(by(0.92), 3, new String[] {"sbs", "s s", "sbs"}, keys('s', s, 'b', Items.IRON_BARS), 1, "MCC starter section");
		closeScreen();
		waitQuest("mcc");
		settle();
		stopFilm();
		give("wildcatter:breaker_3p_industrial", 1, "a three-pole breaker for the quest, made again (the MCC's incoming section took the first)");
		atTable(n, "plc_craft", 3800);
		cue("P22");   // 3x
		openBlock(TABLE);
		tableCraftBy(by(0.45), 3, new String[] {"sss", "srs", "sis"}, keys('s', s, 'r', Items.REPEATER, 'i', Items.IRON_BLOCK), 1, "PLC cabinet");
		tableCraftBy(by(0.88), 3, new String[] {"qcq", "rgr", "qcq"}, keys('q', Items.QUARTZ, 'c', Items.COMPARATOR, 'r', Items.REDSTONE, 'g', Items.GOLD_INGOT), 1, "CPU");
		closeScreen();
		waitQuest("plc");
		settle();
		stopFilm();
		standAt(TABLE.south(2).east(), Vec3.atCenterOf(TABLE));
		context.waitTicks(wanted(n, "plc_claim") ? 30 : 3);
		startFilm(n, "plc_claim");
		cue("P22b");
		claimIn("plc", 30, 40);   // two pushbuttons
		settle();
		stopFilm();
	}

	// ---- 11: poles and lines (P23, P24) ---------------------------------------------------------------------------------------------------

	private void poles(int n) {
		give("minecraft:oak_log", 8, "oak logs");
		give("minecraft:coal", 2, "coal");
		give("minecraft:oak_planks", 3, "planks");
		give("minecraft:iron_nugget", 3, "iron nuggets");
		give("minecraft:brick", 2, "bricks (clay from the river, fired)");
		give("minecraft:iron_ingot", 18, "iron");
		give("minecraft:copper_ingot", 15, "copper");
		give("minecraft:red_dye", 3, "red dye");
		give("minecraft:stick", 1, "a stick");
		atTable(n, "poles_craft", 4200);
		cue("P23");   // 3x
		openBlock(TABLE);
		Item log = Items.OAK_LOG;
		tableCraftBy(by(0.30), 3, new String[] {"ll ", "ll ", "c  "}, keys('l', log, 'c', Items.COAL), 1, "a 30 ft wood pole");
		tableCraftBy(by(0.55), 3, new String[] {"ll ", "ll ", "c  "}, keys('l', log, 'c', Items.COAL), 1, "a second wood pole");
		tableCraftBy(by(0.78), 3, new String[] {"ppp", "n n"}, keys('p', Items.OAK_PLANKS, 'n', Items.IRON_NUGGET), 1, "crossarm");
		tableCraftBy(by(0.96), 3, new String[] {"b", "b", "n"}, keys('b', Items.BRICK, 'n', Items.IRON_NUGGET), 1, "two pin insulators");
		settle();
		cue("P24");   // 3x
		tableCraftBy(by(0.17), 3, new String[] {"iic", "ccd"}, keys('i', Items.IRON_INGOT, 'c', Items.COPPER_INGOT, 'd', item("minecraft:red_dye")), 1, "MV wire");
		tableCraftBy(by(0.30), 3, new String[] {"iic", "ccd"}, keys('i', Items.IRON_INGOT, 'c', Items.COPPER_INGOT, 'd', item("minecraft:red_dye")), 1, "MV wire");
		tableCraftBy(by(0.43), 3, new String[] {"iic", "ccd"}, keys('i', Items.IRON_INGOT, 'c', Items.COPPER_INGOT, 'd', item("minecraft:red_dye")), 1, "MV wire");
		tableCraftBy(by(0.62), 3, new String[] {"www", "wsw", "www"}, keys('w', item("wildcatter:mv_wire_hot"), 's', Items.STICK), 1, "MV conductor spool");
		tableCraftBy(by(0.80), 3, new String[] {"ici", "ici", "ici"}, keys('i', Items.IRON_INGOT, 'c', Items.COPPER_INGOT), 1, "pole transformer");
		tableCraftBy(by(0.97), 3, new String[] {"ici", "ici", "ici"}, keys('i', Items.IRON_INGOT, 'c', Items.COPPER_INGOT), 1, "a second pole transformer");
		closeScreen();
		mark("quests: " + questsDone());
		settle();
		stopFilm();
		standAt(TABLE.south(2).east(), Vec3.atCenterOf(TABLE));
		context.waitTicks(wanted(n, "poles_claim") ? 30 : 3);
		startFilm(n, "poles_claim");
		cue("P24b");
		claimIn("poles", 30, 40);   // four pin insulators
		settle();
		stopFilm();
		give("wildcatter:wood_pole_30", 2, "two more poles, made the same way");
		log("pole parts: " + inventory());
	}

	// ---- 12: the step to the line: the second dry-type (P25), the first pole and the cable (P26), the pole transformer run backward (P27) -

	private void dry(int n) {
		give("wildcatter:dry_type_transformer", 1, "a second dry-type, made as in episode three");
		standAt(new BlockPos(-470, S, 986), Vec3.atCenterOf(DRY.below()).add(0, 0.5, 0));
		server.runCommand("time set 4600");
		context.waitTicks(wanted(n, "dry2") ? 80 : 3);
		startFilm(n, "dry2");
		cue("P25");
		at(0.10);
		face(Vec3.atCenterOf(PANEL).add(0.5, 0, 0.5), 30);
		at(0.55);
		put(item("wildcatter:dry_type_transformer"), DRY.below(), Direction.UP);
		mark("dry-type: " + state(DRY) + " | " + reading(DRY));
		at(0.80);
		meter(DRY);
		settle();
		stopFilm();
	}

	private void pole1(int n) {
		give("wildcatter:single_phase_cable", 8, "single-phase cable (a hot, a neutral and a ground wire for each)");
		standAt(new BlockPos(-470, S, 988), Vec3.atCenterOf(CABLE1[0]));
		server.runCommand("time set 4800");
		context.waitTicks(wanted(n, "pole1") ? 80 : 3);
		startFilm(n, "pole1");
		cue("P26");
		at(0.10);
		put(item("wildcatter:single_phase_cable"), CABLE1[0].below(), Direction.UP);
		at(0.22);
		put(item("wildcatter:single_phase_cable"), CABLE1[1].below(), Direction.UP);
		mark("cable from the dry-type: " + reading(CABLE1[0]));
		at(0.40);
		face(Vec3.atCenterOf(P1.below()).add(0, 0.5, 0), 20);
		put(item("wildcatter:wood_pole_30"), P1.below(), Direction.UP);
		mark("pole 1: " + state(P1) + " top " + state(P1.above(7)));
		at(0.55);
		face(Vec3.atCenterOf(P1).add(0, 6, 0), 30);
		at(0.70);
		for (int i = 2; i < CABLE1.length; i++) {
			sneakPut(item("wildcatter:single_phase_cable"), CABLE1[i - 1], Direction.UP);
		}
		mark("cable up the pole: " + reading(CABLE1[CABLE1.length - 1]));
		settle();
		cue("P27");
		at(0.10);
		BlockPos unit = unitOf(P1);
		put(item("wildcatter:pole_transformer_mv"), unit.south(), Direction.NORTH);
		mark("pole transformer 1: " + state(unit) + " | " + reading(unit));
		at(0.45);
		waitQuest("pole_transformer");
		meter(unit);
		settle();
		stopFilm();
	}

	// ---- 13: the line (P28-P30) -------------------------------------------------------------------------------------------------------------

	private void line(int n) {
		BlockPos unit = unitOf(P1);
		// P28: up the first pole from its south side: the pin insulator on its top, the jumper, the span's first end
		standAt(P1.east(2), Vec3.atCenterOf(P1).add(0, 4, 0));
		server.runCommand("time set 5200");
		context.waitTicks(wanted(n, "ins1") ? 80 : 3);
		startFilm(n, "ins1");
		cue("P28");
		at(0.04);
		climb(P1, Direction.EAST, P1.getY() + 5.0);
		at(0.30);
		put(item("wildcatter:insulator_dist"), P1.above(7), Direction.UP);
		mark("insulator 1: " + state(insulatorOf(P1)));
		at(0.55);
		hold(item("wildcatter:conductor_spool_mv"));
		useOn(unit, Direction.EAST);
		mark("jumper's lower end on the bushing");
		at(0.72);
		useOn(insulatorOf(P1), Direction.EAST);
		mark("jumper: " + reading(insulatorOf(P1)));
		at(0.88);
		useOn(insulatorOf(P1), Direction.EAST);
		mark("the span's first end chosen");
		settle();
		stopFilm();
		climbDown(P1.east(2));
		// P29 and P30: the second pole, east of the river, then the span strung to it
		BlockPos p2 = poles.get(1);
		standAt(feet(p2.getX() + 3, p2.getZ() + 2), Vec3.atCenterOf(p2));
		server.runCommand("time set 5600");
		context.waitTicks(wanted(n, "pole2") ? 80 : 3);
		startFilm(n, "pole2");
		cue("P29");
		at(0.06);
		hold(item("wildcatter:wood_pole_30"));
		face(Vec3.atCenterOf(p2.below()).add(0, 0.5, 0), 20);
		at(0.18);
		put(item("wildcatter:wood_pole_30"), p2.below(), Direction.UP);
		mark("pole 2: " + state(p2));
		at(0.36);
		walk(feet(p2.getX(), p2.getZ() + 2));
		climb(p2, Direction.SOUTH, p2.getY() + 5.0);
		at(0.80);
		put(item("wildcatter:insulator_dist"), p2.above(7), Direction.UP);
		mark("insulator 2: " + state(insulatorOf(p2)));
		settle();
		cue("P30");
		at(0.08);
		hold(item("wildcatter:conductor_spool_mv"));
		useOn(insulatorOf(p2), Direction.SOUTH);
		mark("span 1: " + reading(insulatorOf(p2)));
		at(0.30);
		face(Vec3.atCenterOf(insulatorOf(P1)), 50);
		mark("looking back along the span to the first pole");
		settle();
		stopFilm();
		climbDown(feet(p2.getX(), p2.getZ() + 2));
		// off camera: the other poles and spans, the same way
		for (int i = 2; i < poles.size(); i++) {
			BlockPos p = poles.get(i), prev = poles.get(i - 1);
			// the span's first end at the pole before
			standAt(feet(prev.getX(), prev.getZ() + 2), Vec3.atCenterOf(prev));
			climb(prev, Direction.SOUTH, prev.getY() + 5.0);
			hold(item("wildcatter:conductor_spool_mv"));
			useOn(insulatorOf(prev), Direction.SOUTH);
			climbDown(feet(prev.getX(), prev.getZ() + 2));
			standAt(feet(p.getX() + 2, p.getZ() + 2), Vec3.atCenterOf(p));
			put(item("wildcatter:wood_pole_30"), p.below(), Direction.UP);
			standAt(feet(p.getX(), p.getZ() + 2), Vec3.atCenterOf(p));
			climb(p, Direction.SOUTH, p.getY() + 5.0);
			put(item("wildcatter:insulator_dist"), p.above(7), Direction.UP);
			hold(item("wildcatter:conductor_spool_mv"));
			useOn(insulatorOf(p), Direction.SOUTH);
			mark("off camera: pole " + (i + 1) + " " + p.toShortString() + " and its span: " + reading(insulatorOf(p)));
			climbDown(feet(p.getX(), p.getZ() + 2));
		}
	}

	// ---- 14: the base end: the second pole transformer (P31), the cable and the lamps (P32) ---------------------------------------------

	private BlockPos last, unit2;
	private final List<BlockPos> lamps = new ArrayList<>();

	private void baseEnd(int n) {
		last = poles.get(poles.size() - 1);
		unit2 = unitOf(last);
		give("wildcatter:electric_lamp", 4, "four lamps, made as in episode two");
		standAt(feet(last.getX(), last.getZ() + 2), Vec3.atCenterOf(last).add(0, 4, 0));
		server.runCommand("time set 6200");
		context.waitTicks(wanted(n, "unit2") ? 80 : 3);
		startFilm(n, "unit2");
		cue("P31");
		at(0.04);
		climb(last, Direction.SOUTH, last.getY() + 3.0);
		at(0.20);
		put(item("wildcatter:pole_transformer_mv"), unit2.south(), Direction.NORTH);
		mark("pole transformer 2: " + state(unit2));
		at(0.36);
		climb(last, Direction.SOUTH, last.getY() + 5.0);
		hold(item("wildcatter:conductor_spool_mv"));
		useOn(unit2, Direction.SOUTH);
		at(0.50);
		useOn(insulatorOf(last), Direction.SOUTH);
		mark("jumper 2: " + reading(insulatorOf(last)));
		at(0.62);
		meter(insulatorOf(last));
		settle();
		stopFilm();
		climbDown(feet(last.getX(), last.getZ() + 2));
		// P32: the cable down the pole's north side and west along the ground, a lamp beside it every few blocks
		List<BlockPos> run = new ArrayList<>();
		for (int y = unit2.getY() - 1; y >= last.getY(); y--) {
			run.add(new BlockPos(unit2.getX(), y, unit2.getZ()));
		}
		for (int k = 1; k <= 6; k++) {
			run.add(feet(unit2.getX() - k, unit2.getZ()));
		}
		for (int k : new int[] {2, 5}) {
			lamps.add(feet(unit2.getX() - k, unit2.getZ() + 1));
		}
		lamps.add(feet(unit2.getX() - 7, unit2.getZ()));
		standAt(feet(unit2.getX() - 1, unit2.getZ() + 2), Vec3.atCenterOf(unit2));
		context.waitTicks(wanted(n, "lamps") ? 60 : 3);
		startFilm(n, "lamps");
		cue("P32");
		// down the pole: each piece against the one above it (sneaking, as a click on a part would read it), then west along the ground
		at(0.04);
		quickSneakPut(item("wildcatter:single_phase_cable"), unit2, Direction.DOWN);
		int down = unit2.getY() - last.getY();
		for (int i = 1; i < run.size(); i++) {
			BlockPos prev = run.get(i - 1), here = run.get(i);
			if (i == down) {
				walk(feet(unit2.getX() - 4, unit2.getZ() + 2));
			}
			Direction d = Direction.getApproximateNearest(here.getX() - prev.getX(), here.getY() - prev.getY(), here.getZ() - prev.getZ());
			if (!prev.relative(d).equals(here)) {
				log("the ground steps between " + prev.toShortString() + " and " + here.toShortString());
			}
			quickSneakPut(item("wildcatter:single_phase_cable"), prev, d);
			if (state(here).isAir()) {
				log("no cable at " + here.toShortString());
			}
		}
		mark("cable laid: " + reading(run.get(run.size() - 1)));
		at(0.55);
		for (BlockPos l : lamps) {
			put(item("wildcatter:electric_lamp"), l.below(), Direction.UP);
		}
		mark("lamps: " + lamps.stream().map(l -> state(l).toString()).toList());
		at(0.80);
		face(Vec3.atCenterOf(lamps.get(0)), 30);
		settle();
		stopFilm();
	}

	// ---- 15: the pad-mount, at the base's table (P33) --------------------------------------------------------------------------------------

	private void pad(int n) {
		give("wildcatter:pole_transformer_mv", 2, "two more pole transformers, made the same way");
		give("minecraft:diamond", 1, "a diamond");
		give("minecraft:copper_block", 1, "copper packed into a block");
		give("wildcatter:steel_ingot", 5, "steel");
		standAt(new BlockPos(bx, by + 1, bz + 2), Vec3.atCenterOf(table));
		server.runCommand("time set 7000");
		context.waitTicks(wanted(n, "pad_craft") ? 60 : 3);
		startFilm(n, "pad_craft");
		cue("P33");
		openBlock(table);
		tableCraftBy(by(0.80), 3, new String[] {"sds", "tct", "sss"}, keys('s', WildcatterItems.STEEL_INGOT, 'd', Items.DIAMOND, 't', item("wildcatter:pole_transformer_mv"),
				'c', item("minecraft:copper_block")), 1, "pad-mount transformer");
		closeScreen();
		waitQuest("padmount");
		settle();
		stopFilm();
	}

	// ---- 16: dusk, the chapter, the hook and the title, the context shot, the outro (each its own take) ----------------------------------

	private void ends(int n) {
		BlockPos l0 = lamps.isEmpty() ? new BlockPos(bx, by + 1, bz) : lamps.get(0);
		// P36: the book
		standAt(feet(l0.getX() + 1, l0.getZ() + 4), Vec3.atCenterOf(l0));
		server.runCommand("time set 11800");
		context.waitTicks(wanted(n, "chapter") ? 60 : 3);
		startFilm(n, "chapter");
		cue("P35");
		at(0.04);
		bookOpen("plc");
		at(0.40);
		openQuest("padmount");
		at(0.72);
		openQuest("steam_plant");
		mark("next: steam_plant");
		settle();
		context.setScreen(() -> null);
		stopFilm();
		spectator();
		// P34: the line at dusk, from the gas well across the river to the lamps
		server.runCommand("time set 12600");
		BlockPos mid = poles.get(1);
		droneAt(n, "line_dusk", mid, cam(6.0, 16.0, 14.0, -20.0, 4.0, -10.0), cam(-6.0, 15.0, 16.0, -30.0, 3.0, -14.0), "P34");
		// P1, the hook: low past the lamps at dusk to the last pole
		server.runCommand("time set 12900");
		droneAt(n, "hook", last, cam(-14.0, 2.6, 4.0, -7.0, 1.0, 0.0), cam(-4.0, 3.0, 5.0, 0.0, 4.0, -1.0), "P01");
		// the title card: the gas well's set at dusk, the line leaving it
		droneAt(n, "title", PANEL, cam(8.0, 4.0, 8.0, -2.0, 1.5, 0.0), cam(6.0, 4.5, 10.0, -2.0, 1.5, 0.0), "title");
		// P2: high along the line, west from the gas well
		server.runCommand("time set 9000");
		droneAt(n, "context", poles.get(1), cam(20.0, 22.0, 18.0, -10.0, 2.0, -6.0), cam(6.0, 22.0, 20.0, -24.0, 2.0, -10.0), "P02");
		// the outro: low from the east of the river at sunset
		server.runCommand("time set 12700");
		droneAt(n, "outro", poles.get(1), cam(10.0, 3.5, -6.0, -20.0, 6.0, -8.0), cam(9.0, 4.0, -2.0, -20.0, 6.0, -8.0), "outro");
		survival();
		log("at the end: set " + reading(PANEL) + " | tree " + reading(TREE) + " | rig " + reading(HOLE) + " | separator " + reading(SEP) + " | unit 1 " + reading(unitOf(P1))
				+ " | unit 2 " + reading(unit2) + " | lamps " + lamps.stream().map(l -> state(l).toString()).toList());
		log("PLAN poles: " + poles + "; lamps " + lamps + "; unit2 " + unit2);
	}

	/** A spectator drone move between two cameras relative to {@code origin}, as long as cue {@code name}'s target. */
	private void droneAt(int n, String name, BlockPos origin, Bfs04Films.Cam a, Bfs04Films.Cam b, String cueId) {
		t.ox = origin.getX();
		t.gy = origin.getY() - 1;
		t.oz = origin.getZ();
		t.camera(a, true);
		context.waitTicks(wanted(n, name) ? 120 : 2);
		t.camera(a, true);
		context.waitTicks(10);
		startFilm(n, name);
		cue(cueId);
		int ticks = (int) Math.max(40, Math.round((filming ? cueLen : 2.0) * 20));
		for (int i = 0; i <= ticks; i++) {
			double s = (double) i / ticks;
			s = s * s * (3 - 2 * s);
			t.camera(a.lerp(b, s), false);
			context.waitTick();
		}
		settle();
		stopFilm();
	}

	/** Opens the quest book on {@code quest}'s page (and leaves it open). */
	private void bookOpen(String quest) {
		hold(WildcatterItems.QUEST_BOOK);
		context.runOnClient(c -> c.gui.toastManager().clear());
		useHeld();
		context.waitTicks(6);
		openQuest(quest);
	}

	private void openQuest(String quest) {
		context.runOnClient(c -> {
			if (c.gui.screen() instanceof QuestScreen q) {
				q.open(Identifier.parse("wildcatter:" + quest), true);
			}
		});
		mark("book on " + quest);
	}
}
