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
 * Built From Scratch, episode 5 "Medium voltage": the second half of Distribution (the steam plant, the unit substation and GSU, the MV
 * switchgear), played through in survival in the world episode 4 left (Bfs05Films plays episodes 3 and 4's film code first, nothing
 * filmed). The land rig half moved to episode 6 (Tim, 2026-11-27). Everything the player does goes through the client: the destroy loop,
 * useItemOn, container clicks with a drawn cursor; what is gathered off camera is given by command and logged, so the edit can label it.
 * The layouts are the NeoForge proof scenes' (Bfs05Scenes). Each take is timed to its narration (cue(), from ep05/targets.json, in game
 * time); readouts are the game's own (ReadoutOverlay). Films land in {@code build/scene-videos/bfs05__NN_<shot>.mov}; BFS_SHOTS picks
 * shots by number or name (the rest run unfilmed). The helpers are episode 4's (Bfs04Episode).
 */
final class Bfs05Episode {
	private final Bfs05Films t;
	private final ClientGameTestContext context;
	private final TestServerContext server;
	private final Set<String> shots;
	private int bx, by, bz;
	private BlockPos table;
	private boolean filming;
	private String filmId = "";

	Bfs05Episode(Bfs05Films t) {
		this(t, pick());
	}

	Bfs05Episode(Bfs05Films t, Set<String> shots) {
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
		System.out.println("[bfs05] " + s);
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
		String[] steps = {"setup", "well", "unitsub", "stop", "strip", "place", "riser", "start", "lamps", "plantpage", "plantcraft", "plantplace", "mistake",
				"startset", "raise", "gsu", "switchgear", "ends"};
		for (int i = 0; i < steps.length; i++) {
			int n = i + 1;
			log("step " + n + " " + steps[i]);
			try {
				switch (steps[i]) {
					case "setup" -> setup();
					case "well" -> well(n);
					case "unitsub" -> unitSub(n);
					case "stop" -> stopSet(n);
					case "strip" -> strip(n);
					case "place" -> placeUnit(n);
					case "riser" -> riser(n);
					case "start" -> startSet(n);
					case "lamps" -> lamps(n);
					case "plantpage" -> plantPage(n);
					case "plantcraft" -> plantCraft(n);
					case "plantplace" -> plantPlace(n);
					case "mistake" -> mistake(n);
					case "startset" -> startingSet(n);
					case "raise" -> raise(n);
					case "gsu" -> gsu(n);
					case "switchgear" -> switchgear(n);
					case "ends" -> ends(n);
					default -> {
					}
				}
			} catch (Throwable e) {
				log("step " + steps[i] + " FAILED: " + e);
				e.printStackTrace();
				climbing = false;
				pin = null;
				context.getInput().releaseKey(o -> o.keyShift);
				context.getInput().releaseKey(o -> o.keyJump);
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
		filmId = String.format(Locale.ROOT, "bfs05__%02d_%s", n, name);
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
	private Bfs05Films.Cam cam(double x, double y, double z, double tx, double ty, double tz) {
		return new Bfs05Films.Cam(x, y, z, tx, ty, tz);
	}

	/** A spectator drone move between two cameras, HUD hidden. */
	private void drone(int n, String name, Bfs05Films.Cam a, Bfs05Films.Cam b, int ticks) {
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
		if (!climbing) {
			context.getInput().releaseKey(o -> o.keyShift);
		}
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
	private void droneAt(int n, String name, BlockPos origin, Bfs05Films.Cam a, Bfs05Films.Cam b, int ticks) {
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
		if (!climbing) {
			context.getInput().releaseKey(o -> o.keyShift);
		}
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
			path = "/Users/timhunsaker/minecraft-mods/videos/longform/built-from-scratch/ep05/targets.json";
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

	/** True while the player is on a pole: the sneak key stays down (holding on), so every click there is a sneaking one. */
	private boolean climbing;

	/**
	 * Climbs the pole at {@code pole} (its ground cell) from its {@code side}, as a player does: a step into the pole's cell beside the wood
	 * (the pole is climbable), the jump key held until the feet are at {@code feetY}, then the sneak key held to hang there.
	 */
	private void climb(BlockPos pole, Direction side, double feetY) {
		Vec3 from = context.computeOnClient(c -> c.player.position());
		if (!climbing) {
			Vec3 at = new Vec3(pole.getX() + 0.5 + side.getStepX() * 0.47, from.y, pole.getZ() + 0.5 + side.getStepZ() * 0.47);
			for (int i = 1; i <= 8; i++) {
				Vec3 p = from.lerp(at, i / 8.0);
				context.runOnClient(c -> c.player.setPos(p.x, p.y, p.z));
				context.waitTick();
			}
			face(new Vec3(pole.getX() + 0.5, feetY + 1.4, pole.getZ() + 0.5), 8);
		}
		context.getInput().releaseKey(o -> o.keyShift);
		context.getInput().holdKey(o -> o.keyJump);
		for (int i = 0; i < 240 && context.computeOnClient(c -> c.player.getY()) < feetY - 0.05; i++) {
			context.waitTick();
		}
		context.getInput().holdKey(o -> o.keyShift);
		context.getInput().releaseKey(o -> o.keyJump);
		climbing = true;
		context.waitTicks(4);
		mark(String.format(Locale.ROOT, "climbed to %.2f (server %.2f)", context.computeOnClient(c -> c.player.getY()),
				server.computeOnServer(srv -> sp(srv).getY())));
	}

	/** Back down the pole (the sneak key let go, sliding down its steps) and a step out to {@code ground}. */
	private void climbDown(BlockPos ground) {
		context.getInput().releaseKey(o -> o.keyShift);
		climbing = false;
		for (int i = 0; i < 200 && !context.computeOnClient(c -> c.player.onGround()); i++) {
			context.waitTick();
		}
		Vec3 p0 = context.computeOnClient(c -> c.player.position());
		Vec3 out = Vec3.atBottomCenterOf(ground);
		for (int i = 1; i <= 8; i++) {
			Vec3 p = p0.lerp(out, i / 8.0);
			context.runOnClient(c -> c.player.setPos(p.x, p.y, p.z));
			context.waitTick();
		}
		context.waitTicks(2);
	}


	// ---- positions ---------------------------------------------------------------------------------------------------------------------
	// The gas well site (PLAN.md "Episode 4's base"), levelled at ground top 63: the player stands at y 64. The layouts are the proof
	// scenes' (neoforge Bfs05Scenes), relative to the tree at -478, 64, 980.

	static final int G = Bfs04Episode.G, S = Bfs04Episode.S;
	static final BlockPos TREE = Bfs04Episode.TREE, TABLE = Bfs04Episode.TABLE, PANEL = Bfs04Episode.PANEL, STARTER = Bfs04Episode.STARTER,
			DRY = Bfs04Episode.DRY, P1 = Bfs04Episode.P1, SEP = Bfs04Episode.SEP;
	static final BlockPos[] CABLE1 = Bfs04Episode.CABLE1;
	static final BlockPos UNIT1 = new BlockPos(-473, 68, 987);       // episode 4's backfed pole transformer, on pole 1's north side
	static final BlockPos INS1 = new BlockPos(-473, 72, 988);        // pole 1's pin insulator
	/** The unit substation facing south: its main cell (the medium-voltage end) at x -470, its 480 V end at x -473 against the panel; z 984-985. */
	static final BlockPos UNIT = new BlockPos(-470, S, 985);
	/** The bare medium-voltage wire: off the unit substation's medium-voltage end, along the ground, and up pole 1's north side to its insulator. */
	static final BlockPos[] RISER = {new BlockPos(-469, S, 985), new BlockPos(-469, S, 986), new BlockPos(-469, S, 987), new BlockPos(-470, S, 987),
			new BlockPos(-471, S, 987), new BlockPos(-472, S, 987), new BlockPos(-473, S, 987), new BlockPos(-473, 65, 987), new BlockPos(-473, 66, 987),
			new BlockPos(-473, 67, 987), new BlockPos(-473, 68, 987), new BlockPos(-473, 69, 987), new BlockPos(-473, 70, 987), new BlockPos(-473, 71, 987),
			new BlockPos(-473, 72, 987)};
	/** The base's lamps at the line's far end (PLAN.md "Episode 4's base"), and its last pole. */
	static final BlockPos[] LAMPS = {new BlockPos(-554, 66, 954), new BlockPos(-557, 66, 954), new BlockPos(-559, 66, 953)};
	static final BlockPos LAST = new BlockPos(-552, 66, 954);
	// the steam plant, facing north round its generator (SteamPlantLayout), on ground levelled north of the well
	static final BlockPos GEN = new BlockPos(-480, S, 968);
	static final BlockPos BURNER = GEN.offset(-1, 0, -3), DRUM = GEN.offset(0, 0, -3), SUPER = GEN.offset(4, 0, -3), TURBINE = GEN.offset(3, 0, 0),
			FEED = GEN.offset(0, 0, 2), COND = GEN.offset(2, 0, 2);
	// the starting set south of the feed pump: the dry-type (front, 480 V, north to the pump's box), the turbine against its back, the
	// boiler on a firebox set in the ground west of the turbine, the water pump east of it in a hole of water, piped to the condenser
	static final BlockPos DRY2 = FEED.south(), SMALL = FEED.south(2), BOILER = SMALL.west(), FIREBOX = BOILER.below(), PUMP = SMALL.east(), WATER = PUMP.below(),
			PIPE = PUMP.north();
	static final BlockPos GSU = GEN.offset(-1, 0, 1);                // facing west, its main cell's back against the generator's other terminal cell
	static final BlockPos ROD = GEN.offset(-2, 0, 3);                // a ground rod against the GSU's front row
	static final BlockPos FEEDER = new BlockPos(-474, S, 968), MAIN = new BlockPos(-473, S, 968);   // the lineup, facing north, the main to the east

	// ---- 1: off camera, to the gas well ------------------------------------------------------------------------------------------------

	private void setup() {
		server.runCommand("forceload add -590 930 -455 1000");
		if (System.getenv("BFS_QUICK4") != null) {
			quickFour();
		}
		server.runCommand("time set 1000");
		survival();
		standAt(new BlockPos(-484, S, 977), Vec3.atCenterOf(PANEL));
		context.waitTicks(120);
		stashJunk(item("wildcatter:seismograph"), item("wildcatter:disposal_wellhead"), item("wildcatter:injection_pump"), item("wildcatter:crude_oil_bucket"),
				Items.BUCKET, Items.WATER_BUCKET, item("minecraft:leaf_litter"), item("wildcatter:mcc_incoming"), item("wildcatter:mcc_section"),
				item("wildcatter:plc_cabinet"), item("wildcatter:cpu_small"), item("wildcatter:pushbutton"), item("wildcatter:padmount_480"),
				item("wildcatter:industrial_wire_hot"), item("wildcatter:breaker_3p_industrial"), item("wildcatter:spark_plug"), item("wildcatter:electric_lamp"),
				item("wildcatter:crossarm_dist"), item("wildcatter:insulator_dist"), item("wildcatter:conductor_spool_mv"));
		// the plant's ground north of the well, levelled at the site's height, the trees near it taken down
		levelSite(-488, 960, -468, 977, G, 5);
		log("the gas well: set " + reading(PANEL) + " | dry-type " + reading(DRY) + " | unit 1 " + reading(UNIT1) + " | insulator " + reading(INS1)
				+ " | lamps " + java.util.Arrays.stream(LAMPS).map(l -> state(l).toString()).toList() + "; pocket " + inventory());
	}

	/**
	 * Working runs only (BFS_QUICK4, never a take): the little of episode 4's world this episode touches, set by command instead of
	 * playing episodes 3 and 4 through: the gas well's set on its separator, the starter, the stand-in, pole 1 and its insulator.
	 */
	private void quickFour() {
		server.runCommand("tp @p -480 80 978");
		context.waitTicks(200);
		levelSite(-486, 973, -462, 988, G, 8);
		set(TREE, block("wildcatter:gas_wellhead").defaultBlockState());
		server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			dev.wildcatter.block.GensetSkid.build(level, PANEL, Direction.NORTH, true);
			BlockState sep = block("wildcatter:separator").defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
			level.setBlock(SEP, sep, 3);
			dev.wildcatter.multiblock.BigParts.grow(level, SEP, sep);
			level.setBlock(STARTER, block("wildcatter:motor_starter").defaultBlockState().setValue(dev.wildcatter.block.MotorStarterBlock.FACING, Direction.EAST), 3);
			level.setBlock(DRY, block("wildcatter:dry_type_transformer").defaultBlockState().setValue(dev.wildcatter.block.TransformerBlock.FACING, Direction.WEST), 3);
			BlockState pole = block("wildcatter:wood_pole_30").defaultBlockState();
			level.setBlock(P1, pole, 3);
			dev.wildcatter.multiblock.BigParts.grow(level, P1, pole);
			var ins = (dev.wildcatter.block.InsulatorBlock) block("wildcatter:insulator_dist");
			level.setBlock(INS1, ins.defaultBlockState().setValue(dev.wildcatter.block.InsulatorBlock.MOUNT, ins.mountAt(level, INS1))
					.setValue(dev.wildcatter.block.CableBlock.CONDUCTOR, dev.wildcatter.circuit.Conductor.A), 3);
			level.setBlock(UNIT1, block("wildcatter:pole_transformer_mv").defaultBlockState().setValue(dev.wildcatter.block.TransformerBlock.FACING, Direction.NORTH), 3);
			dev.wildcatter.item.SpoolItem.connectLead(level, INS1, UNIT1, Voltage.MEDIUM, false, 64, dev.wildcatter.energy.ConductorSize.ACSR_SPARROW);
			var p = dev.wildcatter.quest.QuestData.get(srv).of(sp(srv).getStringUUID());
			for (String q : new String[] {"wood", "stone", "campfire", "iron", "copper", "steel", "field_manual", "steam", "lamp", "protection", "firebox", "plugs",
					"step_up", "seismograph", "rig", "gas_well", "oil_well", "lease", "crude", "engine_parts", "genset", "motor", "mcc", "plc", "poles", "pole_transformer", "padmount"}) {
				p.done.add("wildcatter:" + q);
			}
			dev.wildcatter.quest.QuestData.get(srv).changed();
			var inv = sp(srv).getInventory();
			inv.clearContent();
			Object[][] pocket = {{0, WildcatterItems.QUEST_BOOK, 1}, {1, WildcatterItems.FIELD_MANUAL, 1}, {2, item("wildcatter:gas_line"), 5}, {3, WildcatterItems.POWER_METER, 1},
					{4, item("wildcatter:ground_rod"), 1}, {8, item("wildcatter:single_phase_cable"), 5}, {9, WildcatterItems.STEEL_INGOT, 15}, {10, Items.COPPER_INGOT, 3},
					{12, Items.STONE_PICKAXE, 1}, {25, item("wildcatter:mv_wire_hot"), 4}};
			for (Object[] o : pocket) {
				inv.setItem((Integer) o[0], new ItemStack((Item) o[1], (Integer) o[2]));
			}
			return null;
		});
		for (BlockPos c : CABLE1) {
			set(c, block("wildcatter:single_phase_cable").defaultBlockState());
		}
		List<BlockPos> gas = new ArrayList<>(List.of(Bfs04Episode.WET[0]));
		gas.addAll(List.of(Bfs04Episode.SEP_GAS));
		for (BlockPos c : gas) {
			set(c, block("wildcatter:gas_line").defaultBlockState());
		}
		set(TABLE, Blocks.CRAFTING_TABLE.defaultBlockState());
		joinShapes(List.of(CABLE1));
		joinShapes(gas);
		context.waitTicks(40);
		server.computeOnServer(srv -> {
			if (dev.wildcatter.multiblock.BigParts.entity(srv.overworld(), PANEL) instanceof dev.wildcatter.block.GensetControlPanelBlockEntity panel) {
				panel.setWantRun(true);
			}
			return null;
		});
		context.waitTicks(300);
		log("QUICK: episode 4's gas well set by command (a working run, not a take): " + reading(PANEL));
	}

	// ---- 2: the gas well as episode 4 left it (P3) -------------------------------------------------------------------------------------

	private void well(int n) {
		standAt(new BlockPos(-478, S, 990), Vec3.atCenterOf(PANEL).add(0, 0.5, 0));
		server.runCommand("time set 1200");
		context.waitTicks(wanted(n, "well") ? 80 : 3);
		startFilm(n, "well");
		cue("P03");
		at(0.06);
		face(Vec3.atCenterOf(PANEL).add(0.5, 0, 0), 30);
		at(0.40);
		face(Vec3.atCenterOf(DRY), 30);
		mark("the dry-type: " + reading(DRY));
		at(0.60);
		face(Vec3.atCenterOf(CABLE1[3]), 30);
		at(0.75);
		face(Vec3.atCenterOf(UNIT1), 30);
		mark("the backfed pole transformer: " + reading(UNIT1));
		settle();
		stopFilm();
	}

	// ---- 3: the unit substation crafted (P4) -------------------------------------------------------------------------------------------

	private void unitSub(int n) {
		give("wildcatter:pole_transformer_mv", 2, "two pole transformers, made as in episode four");
		give("minecraft:diamond", 2, "diamonds");
		give("minecraft:copper_block", 1, "copper packed into a block");
		give("wildcatter:steel_ingot", 4, "steel from the blast furnace");
		standAt(TABLE.south(2), Vec3.atCenterOf(TABLE));
		server.runCommand("time set 1400");
		context.waitTicks(wanted(n, "unit_craft") ? 60 : 3);
		startFilm(n, "unit_craft");
		cue("P04");
		openBlock(TABLE);
		tableCraftBy(by(0.90), 3, new String[] {"sds", "tct", "sds"}, keys('s', WildcatterItems.STEEL_INGOT, 'd', Items.DIAMOND, 't', item("wildcatter:pole_transformer_mv"),
				'c', item("minecraft:copper_block")), 1, "unit substation");
		closeScreen();
		settle();
		stopFilm();
	}

	// ---- 4: the set stopped, the line dead (P5) ----------------------------------------------------------------------------------------

	private void stopSet(int n) {
		standAt(new BlockPos(-476, S, 981), Vec3.atCenterOf(PANEL).add(0, 0.1, -0.5));
		server.runCommand("time set 1700");
		context.waitTicks(wanted(n, "stop") ? 60 : 3);
		startFilm(n, "stop");
		cue("P05");
		at(0.10);
		walk(new BlockPos(-474, S, 982));
		emptyHand();
		face(Vec3.atCenterOf(PANEL).add(0, 0.1, -0.5), 10);
		at(0.45);
		useOn(PANEL, Direction.NORTH);
		mark("stop pressed: " + reading(PANEL));
		at(0.70);
		face(Vec3.atCenterOf(P1).add(0, 6, 0), 40);
		mark("the set: " + reading(PANEL));
		settle();
		stopFilm();
		for (int i = 0; i < 400 && reading(PANEL).contains("RUNNING"); i++) {
			context.waitTick();
		}
		log("the set stopped: " + reading(PANEL) + " | insulator " + reading(INS1));
	}

	// ---- 5: the stand-in out (P6) ----------------------------------------------------------------------------------------------------------

	private void strip(int n) {
		give("minecraft:iron_pickaxe", 1, "an iron pickaxe, made off camera");
		standAt(new BlockPos(-471, S, 983), Vec3.atCenterOf(STARTER));
		server.runCommand("time set 1900");
		context.waitTicks(wanted(n, "strip") ? 60 : 3);
		startFilm(n, "strip");
		cue("P06");
		hold(Items.IRON_PICKAXE);
		at(0.04);
		dig(STARTER, 80);
		mark("starter off");
		at(0.30);
		walk(new BlockPos(-471, S, 986));
		dig(DRY, 80);
		mark("dry-type off");
		at(0.50);
		dig(UNIT1, 80);
		pickUpNear(UNIT1.below(4), 4.0);
		mark("pole transformer off: insulator " + reading(INS1));
		for (int i = CABLE1.length - 1; i >= 0; i--) {
			dig(CABLE1[i], 40);
		}
		pickUpNear(CABLE1[1], 4.0);
		mark("cable off; pocket " + inventory());
		settle();
		stopFilm();
		pickUpNear(CABLE1[3], 5.0);
		stashJunk(item("minecraft:leaf_litter"), item("wildcatter:single_phase_cable"), item("wildcatter:motor_starter"), Items.STONE_PICKAXE);
	}

	// ---- 6: the unit substation in their place (P7) ------------------------------------------------------------------------------------

	private void placeUnit(int n) {
		standAt(new BlockPos(-470, S, 988), Vec3.atCenterOf(UNIT.below()).add(0, 0.5, 0));
		server.runCommand("time set 2100");
		context.waitTicks(wanted(n, "unit_place") ? 60 : 3);
		startFilm(n, "unit_place");
		cue("P07");
		at(0.08);
		hold(item("wildcatter:unit_substation"));
		at(0.20);
		useOn(UNIT.below(), Direction.UP);
		mark("unit substation: " + state(UNIT) + " cells " + cellsOf(UNIT));
		at(0.45);
		face(Vec3.atCenterOf(PANEL).add(0.5, 0, 0.5), 30);
		at(0.70);
		face(Vec3.atCenterOf(UNIT).add(0.5, 0, 0), 30);
		settle();
		stopFilm();
	}

	// ---- 7: the bare wire to the line (P8) -----------------------------------------------------------------------------------------------

	private void riser(int n) {
		give("wildcatter:mv_wire_hot", 12, "medium-voltage wire, made as in episode four");
		standAt(new BlockPos(-467, S, 986), Vec3.atCenterOf(RISER[0]));
		server.runCommand("time set 2300");
		context.waitTicks(wanted(n, "riser") ? 60 : 3);
		startFilm(n, "riser");
		cue("P08");
		hold(item("wildcatter:mv_wire_hot"));
		for (int i = 0; i < 7; i++) {
			at(0.03 + 0.045 * i);
			if (i == 3) {
				walk(new BlockPos(-471, S, 989));
			}
			quickPut(item("wildcatter:mv_wire_hot"), RISER[i].below(), Direction.UP);
		}
		for (int i = 7; i <= 9; i++) {
			quickSneakPut(item("wildcatter:mv_wire_hot"), RISER[i - 1], Direction.UP);
		}
		at(0.52);
		walk(new BlockPos(-472, S, 989));
		climb(P1, Direction.EAST, P1.getY() + 4.6);
		for (int i = 10; i < RISER.length; i++) {
			quickSneakPut(item("wildcatter:mv_wire_hot"), RISER[i - 1], Direction.UP);
		}
		mark("the wire at the insulator: " + reading(RISER[RISER.length - 1]) + " | insulator " + reading(INS1));
		at(0.90);
		face(Vec3.atCenterOf(INS1), 20);
		settle();
		stopFilm();
		climbDown(new BlockPos(-471, S, 989));
		for (BlockPos r : RISER) {
			if (state(r).isAir()) {
				log("NO WIRE at " + r.toShortString());
			}
		}
	}

	/** As put(), at a working pace for a run of the same piece (the item kept in hand). */
	private void quickPut(Item what, BlockPos on, Direction side) {
		if (!context.computeOnClient(c -> c.player.getMainHandItem().is(what))) {
			hold(what);
		}
		Vec3 at = Vec3.atCenterOf(on).add(Vec3.atLowerCornerOf(side.getUnitVec3i()).scale(0.5));
		face(at, 5);
		context.runOnClient(c -> {
			BlockHitResult hit = new BlockHitResult(at, side, on, false);
			if (c.gameMode.useItemOn(c.player, InteractionHand.MAIN_HAND, hit).consumesAction()) {
				c.player.swing(InteractionHand.MAIN_HAND, c.player.getMainHandItem().getAttackAnimation(), false);
			}
		});
		context.waitTicks(4);
	}

	// ---- 8: the set started, the line fed through the unit substation (P9) ----------------------------------------------------------------

	private void startSet(int n) {
		standAt(new BlockPos(-476, S, 981), Vec3.atCenterOf(PANEL).add(0, 0.1, -0.5));
		server.runCommand("time set 2600");
		context.waitTicks(wanted(n, "start") ? 60 : 3);
		startFilm(n, "start");
		cue("P09");
		at(0.04);
		walk(new BlockPos(-474, S, 982));
		emptyHand();
		face(Vec3.atCenterOf(PANEL).add(0, 0.1, -0.5), 8);
		useOn(PANEL, Direction.NORTH);
		mark("start pressed: " + reading(PANEL));
		at(0.25);
		face(Vec3.atCenterOf(UNIT).add(0.5, 0.5, 0), 30);
		for (int i = 0; i < 300 && !reading(PANEL).contains("RUNNING"); i++) {
			context.waitTick();
		}
		mark("running: " + reading(PANEL) + " | unit " + reading(UNIT));
		at(0.45);
		walkGround(-466, 982);
		walkGround(-466, 990);
		at(0.75);
		meter(RISER[2]);   // read from three blocks away: a bare medium-voltage wire shocks within two
		settle();
		stopFilm();
		log("the line fed: insulator " + reading(INS1) + " | lamps " + java.util.Arrays.stream(LAMPS).map(l -> state(l).toString()).toList());
	}

	// ---- 9: the lamps at the base (P10) ---------------------------------------------------------------------------------------------------

	private void lamps(int n) {
		standAt(feet(LAMPS[0].getX() - 3, LAMPS[0].getZ() + 4), Vec3.atCenterOf(LAMPS[0]));
		server.runCommand("time set 11600");
		context.waitTicks(wanted(n, "lamps") ? 80 : 3);
		startFilm(n, "lamps");
		cue("P10");
		mark("lamps: " + java.util.Arrays.stream(LAMPS).map(l -> state(l).toString()).toList());
		at(0.30);
		face(Vec3.atCenterOf(LAST).add(0, 5, 0), 50);
		settle();
		stopFilm();
	}

	// ---- 10: the steam plant's page (P11) -------------------------------------------------------------------------------------------------

	private void plantPage(int n) {
		standAt(new BlockPos(-478, S, 962), Vec3.atCenterOf(GEN));
		server.runCommand("time set 2900");
		context.waitTicks(wanted(n, "plant_page") ? 60 : 3);
		startFilm(n, "plant_page");
		cue("P11");
		at(0.06);
		bookOpen("steam_plant");
		at(0.97);
		context.setScreen(() -> null);
		settle();
		stopFilm();
	}

	// ---- 11: the seven parts crafted (P12-P14, 3x) -----------------------------------------------------------------------------------------

	private void plantCraft(int n) {
		give("wildcatter:stator_winding", 3, "three stators, made as in episode four");
		give("wildcatter:rotor_field", 1, "a rotor field, made as in episode four");
		give("wildcatter:exciter", 1, "an exciter, made as in episode four");
		give("wildcatter:crankshaft", 1, "a crankshaft, made as in episode four");
		give("wildcatter:steel_ingot", 24, "steel from the blast furnace");
		give("minecraft:iron_bars", 2, "iron bars");
		give("minecraft:bricks", 4, "bricks");
		give("minecraft:blast_furnace", 1, "a blast furnace, made as in episode one");
		give("minecraft:water_bucket", 1, "a bucket of water");
		give("minecraft:copper_ingot", 4, "copper");
		give("minecraft:copper_block", 1, "copper packed into a block");
		give("wildcatter:pipe", 4, "water pipe");
		give("wildcatter:pump", 1, "a water pump, made as in episode two");
		give("minecraft:piston", 2, "pistons");
		var s = WildcatterItems.STEEL_INGOT;
		standAt(TABLE.south(2), Vec3.atCenterOf(TABLE));
		server.runCommand("time set 3100");
		context.waitTicks(wanted(n, "plant_craft") ? 60 : 3);
		startFilm(n, "plant_craft");
		cue("P12");   // 3x
		openBlock(TABLE);
		tableCraftBy(by(0.52), 3, new String[] {"sws", "wrw", "pxp"}, keys('s', s, 'w', item("wildcatter:stator_winding"), 'r', item("wildcatter:rotor_field"),
				'p', item("wildcatter:gas_line"), 'x', item("wildcatter:exciter")), 1, "turbine generator");
		tableCraftBy(by(0.95), 3, new String[] {"sss", "bcb", "sss"}, keys('s', s, 'b', Items.IRON_BARS, 'c', item("wildcatter:crankshaft")), 1, "turbine");
		cue("P13");   // 3x
		tableCraftBy(by(0.32), 3, new String[] {"sbs", "bfb", "sbs"}, keys('s', s, 'b', Items.BRICKS, 'f', Items.BLAST_FURNACE), 1, "burner");
		tableCraftBy(by(0.62), 3, new String[] {"sss", "sws", "sss"}, keys('s', s, 'w', Items.WATER_BUCKET), 1, "boiler drum");
		shift(5);   // the empty bucket the water bucket leaves in the grid, back to the pocket
		tableCraftBy(by(0.95), 3, new String[] {"scs", "csc", "scs"}, keys('s', s, 'c', Items.COPPER_INGOT), 1, "superheater");
		cue("P14");   // 3x
		tableCraftBy(by(0.48), 3, new String[] {"sps", "pcp", "sps"}, keys('s', s, 'p', item("wildcatter:pipe"), 'c', item("minecraft:copper_block")), 1, "condenser");
		tableCraftBy(by(0.92), 3, new String[] {"sps", "mcm"}, keys('s', s, 'p', item("wildcatter:pump"), 'm', Items.PISTON, 'c', Items.COPPER_INGOT), 1, "feed pump");
		closeScreen();
		settle();
		stopFilm();
		log("the plant's parts: " + inventory());
	}

	// ---- 12: the plant placed and formed (P15-P17) -------------------------------------------------------------------------------------------

	private void plantPlace(int n) {
		standAt(new BlockPos(-480, S, 964), Vec3.atCenterOf(GEN.below()).add(0, 0.5, 0));
		server.runCommand("time set 3300");
		context.waitTicks(wanted(n, "plant_place") ? 80 : 3);
		startFilm(n, "plant_place");
		cue("P15");
		at(0.08);
		hold(item("wildcatter:turbine_generator"));
		at(0.20);
		useOn(GEN.below(), Direction.UP);
		mark("generator: " + state(GEN) + " cells " + cellsOf(GEN));
		at(0.50);
		meter(GEN);   // the readout names the next part and where
		settle();
		cue("P16");
		// every part faces the way the player looks when it is set (the plant's parts do not snap): each placed looking south, from the
		// north or north-west, so the plant faces north; the front row first, over the ground the back row will take
		walkGround(-482, 964);
		placePart(0.10, "wildcatter:feed_pump", FEED, new BlockPos(-482, S, 967), new Vec3(-479.95, 64.0, 970.05));
		walkGround(-479, 963);
		placePart(0.30, "wildcatter:condenser", COND, new BlockPos(-476, S, 966), new Vec3(-477.05, 64.0, 970.05));
		placePart(0.46, "wildcatter:turbine_casing", TURBINE, new BlockPos(-477, S, 966), null);
		walkGround(-477, 963);
		placePart(0.62, "wildcatter:superheater", SUPER, new BlockPos(-476, S, 963), null);
		placePart(0.76, "wildcatter:boiler_drum", DRUM, new BlockPos(-479, S, 963), null);
		placePart(0.90, "wildcatter:boiler_burner", BURNER, new BlockPos(-481, S, 963), null);
		settle();
		cue("P17");
		at(0.05);
		face(Vec3.atCenterOf(GEN).add(0, 1, -2), 30);
		waitQuest("steam_plant");
		mark("formed: " + reading(GEN));
		settle();
		stopFilm();
	}

	/** A plant part placed on the ground at its main cell's place, from {@code stand}, at {@code share} of the cue (it takes the plant's facing). */
	private void placePart(double share, String id, BlockPos main, BlockPos stand, Vec3 aim) {
		at(share - 0.08);
		walkGround(stand.getX(), stand.getZ());
		hold(item(id));
		Vec3 hitAt = aim != null ? aim : Vec3.atCenterOf(main.below()).add(0, 0.5, 0);
		face(hitAt, 8);
		at(share);
		context.runOnClient(c -> {
			BlockHitResult hit = new BlockHitResult(hitAt, Direction.UP, main.below(), false);
			if (c.gameMode.useItemOn(c.player, InteractionHand.MAIN_HAND, hit).consumesAction()) {
				c.player.swing(InteractionHand.MAIN_HAND, c.player.getMainHandItem().getAttackAnimation(), false);
			}
		});
		context.waitTicks(6);
		mark(id + ": " + state(main) + " cells " + cellsOf(main) + " | " + overlay());
	}

	// ---- 13: what goes wrong: lit with nothing on the feed pump (P18, P19) ------------------------------------------------------------------

	private void mistake(int n) {
		give("minecraft:coal", 24, "coal from the base's store");
		standAt(new BlockPos(-482, S, 964), Vec3.atCenterOf(BURNER));
		server.runCommand("time set 3600");
		context.waitTicks(wanted(n, "mistake") ? 60 : 3);
		startFilm(n, "mistake");
		cue("P18");
		at(0.06);
		put(Items.COAL, BURNER, Direction.WEST);
		mark("coal in the burner: " + reading(BURNER));
		at(0.30);
		walkGround(-482, 968);
		emptyHand();
		face(Vec3.atCenterOf(GEN), 10);
		useOn(GEN, Direction.WEST);
		mark("lit: " + reading(GEN));
		at(0.70);
		meter(GEN);
		settle();
		cue("P19");
		at(0.06);
		walkGround(-482, 971);
		meter(FEED);
		at(0.65);
		face(Vec3.atCenterOf(FEED).add(0, 0, 0.5), 20);
		settle();
		stopFilm();
	}

	// ---- 14: the starting set (P20-P23) ----------------------------------------------------------------------------------------------------

	private void startingSet(int n) {
		give("wildcatter:firebox", 1, "a firebox, made as in episode two");
		give("wildcatter:boiler", 1, "a boiler, made as in episode two");
		give("wildcatter:steam_turbine", 1, "a steam turbine, made as in episode two");
		give("minecraft:water_bucket", 5, "water carried from the river");
		give("wildcatter:pipe", 1, "water pipe");
		give("wildcatter:pump", 1, "a second water pump, made as in episode two (the first went into the feed pump)");
		give("minecraft:coal", 8, "coal for the small set's firebox");
		standAt(new BlockPos(-482, S, 973), Vec3.atCenterOf(PANEL));
		server.runCommand("time set 3800");
		context.waitTicks(wanted(n, "start_set") ? 60 : 3);
		startFilm(n, "start_set");
		cue("P20");
		at(0.05);
		face(Vec3.atCenterOf(PANEL).add(0, 1, 0), 40);
		at(0.55);
		face(Vec3.atCenterOf(FEED), 40);
		settle();
		cue("P21");
		at(0.04);
		walkGround(DRY2.getX(), DRY2.getZ() + 3);
		hold(item("wildcatter:dry_type_transformer"));
		face(Vec3.atCenterOf(DRY2.below()).add(0, 0.5, 0), 10);
		at(0.12);
		useOn(DRY2.below(), Direction.UP);
		mark("dry-type: " + state(DRY2));
		at(0.30);
		put(item("wildcatter:steam_turbine"), SMALL.below(), Direction.UP);
		mark("turbine: " + state(SMALL));
		at(0.45);
		hold(Items.IRON_PICKAXE);
		dig(FIREBOX, 40);
		put(item("wildcatter:firebox"), FIREBOX.below(), Direction.UP);
		mark("firebox: " + state(FIREBOX));
		at(0.62);
		emptyHand();
		openBlock(FIREBOX);
		shift(slotOf(Items.COAL, 1));
		closeScreen();
		mark("coal in the firebox: " + reading(FIREBOX));
		at(0.82);
		sneakPut(item("wildcatter:boiler"), FIREBOX, Direction.UP);
		mark("boiler: " + state(BOILER));
		settle();
		cue("P22");
		at(0.04);
		hold(Items.IRON_PICKAXE);
		dig(WATER, 40);
		at(0.20);
		hold(Items.WATER_BUCKET);
		face(Vec3.atCenterOf(WATER).add(0, -0.45, 0), 10);
		useHeld();   // a bucket pours where the player looks
		mark("water: " + state(WATER));
		at(0.36);
		sneakPut(item("wildcatter:pump"), SMALL, Direction.EAST);
		mark("water pump: " + state(PUMP));
		at(0.50);
		put(item("wildcatter:pipe"), PIPE.below(), Direction.UP);
		mark("pipe: " + state(PIPE));
		for (int i = 0; i < 4; i++) {
			at(0.62 + 0.08 * i);
			put(Items.WATER_BUCKET, BOILER, Direction.SOUTH);
		}
		mark("boiler: " + reading(BOILER));
		settle();
		for (int i = 0; i < 1200 && !reading(FEED).contains("running"); i++) {
			context.waitTick();
		}
		mark("the feed pump: " + reading(FEED) + " | turbine " + reading(SMALL) + " | water pump " + reading(PUMP));
		cue("P23");
		at(0.06);
		meter(FEED);
		at(0.50);
		meter(GEN);   // the hotwell filling
		settle();
		stopFilm();
	}

	// ---- 15: raising steam (P24 at 6x), the roll (P25), running (P26) ----------------------------------------------------------------------

	private void raise(int n) {
		standAt(new BlockPos(-472, S, 963), Vec3.atCenterOf(DRUM).add(0, 1, 0));
		server.runCommand("time set 4000");
		context.waitTicks(wanted(n, "raise") ? 40 : 3);
		startFilm(n, "raise");
		cue("P24");   // 6x
		at(0.05);
		meter(GEN);
		at(0.40);
		face(Vec3.atCenterOf(BURNER).add(0, 1, 0), 60);
		at(0.75);
		meter(GEN);
		settle();
		stopFilm();
		long t0 = clientTicks;
		String at0 = reading(GEN);
		for (int i = 0; i < 24000 && !reading(GEN).contains("rolling") && !reading(GEN).contains("RUNNING"); i += 20) {
			context.waitTicks(20);
			if (drum() >= 0.495 * dev.wildcatter.Tuning.DRUM_WATER_KG) {
				break;
			}
		}
		log(String.format(Locale.ROOT, "off camera: %.0f s of filling (%s -> %s)", (clientTicks - t0) / 20.0, at0, reading(GEN)));
		standAt(new BlockPos(-474, S, 966), Vec3.atCenterOf(TURBINE).add(0, 1, 0));
		startFilm(n, "roll");
		cue("P25");
		mark("the plant: " + reading(GEN));
		for (int i = 0; i < 2400 && !reading(GEN).contains("RUNNING"); i++) {
			context.waitTick();
			if (i % 200 == 0) {
				mark("rolling: " + reading(GEN));
			}
		}
		mark("running: " + reading(GEN));
		cue("P26");
		at(0.05);
		meter(GEN);
		at(0.55);
		face(Vec3.atCenterOf(GEN).add(0, 1, 0), 30);
		settle();
		stopFilm();
	}

	/** The action-bar text the player sees now (for the log): every text field of the HUD, named. */
	private String overlay() {
		return context.computeOnClient(c -> {
			StringBuilder b = new StringBuilder();
			try {
				for (java.lang.reflect.Field f : c.gui.hud.getClass().getDeclaredFields()) {
					if (net.minecraft.network.chat.Component.class.isAssignableFrom(f.getType()) && !java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
						f.setAccessible(true);
						Object v = f.get(c.gui.hud);
						if (v != null) {
							b.append(f.getName()).append('=').append(((net.minecraft.network.chat.Component) v).getString()).append("; ");
						}
					}
				}
			} catch (ReflectiveOperationException e) {
				b.append(e);
			}
			return b.toString();
		});
	}

	private double drum() {
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("drum water (\\d+) kg").matcher(reading(GEN));
		return m.find() ? Double.parseDouble(m.group(1)) : 0.0;
	}

	// ---- 16: the GSU (P27 at 3x, P28) -------------------------------------------------------------------------------------------------------

	private void gsu(int n) {
		give("wildcatter:unit_substation", 3, "three more unit substations, made the same way (two go into the GSU)");
		give("minecraft:diamond", 2, "diamonds");
		give("minecraft:copper_block", 1, "copper packed into a block");
		give("wildcatter:steel_ingot", 4, "steel");
		atTable(n, "gsu_craft", 4600);
		cue("P27");   // 3x
		openBlock(TABLE);
		tableCraftBy(by(0.75), 3, new String[] {"sds", "tct", "sds"}, keys('s', WildcatterItems.STEEL_INGOT, 'd', Items.DIAMOND, 't', item("wildcatter:unit_substation"),
				'c', item("minecraft:copper_block")), 1, "GSU");
		closeScreen();
		waitQuest("substation");
		settle();
		stopFilm();
		// P28: up on the generator's top (the only place to face west over its terminal), the GSU set facing west
		standAt(new BlockPos(-479, 66, 969), Vec3.atCenterOf(GSU.below()).add(0, 0.5, 0));
		server.runCommand("time set 4800");
		context.waitTicks(wanted(n, "gsu_place") ? 60 : 3);
		startFilm(n, "gsu_place");
		cue("P28");
		at(0.06);
		hold(item("wildcatter:gsu_transformer"));
		face(Vec3.atCenterOf(GSU.below()).add(0.3, 0.5, 0), 10);
		at(0.22);
		useOn(GSU.below(), Direction.UP);
		mark("GSU: " + state(GSU) + " cells " + cellsOf(GSU) + " | " + overlay() + " | at " + context.computeOnClient(c -> c.player.position() + " yaw " + c.player.getYRot()));
		at(0.38);
		walkGround(-479, 971);
		walkGround(-482, 973);
		put(item("wildcatter:ground_rod"), ROD.below(), Direction.UP);
		mark("ground rod: " + state(ROD));
		at(0.62);
		meter(GSU);
		settle();
		stopFilm();
	}

	// ---- 17: the switchgear (P29 at 3x, P30) ----------------------------------------------------------------------------------------------

	private void switchgear(int n) {
		give("wildcatter:steel_ingot", 12, "steel");
		give("minecraft:glass_pane", 2, "glass panes");
		give("minecraft:comparator", 2, "comparators");
		give("minecraft:copper_block", 1, "copper packed into a block");
		give("minecraft:copper_ingot", 1, "copper");
		atTable(n, "swg_craft", 5200);
		cue("P29");   // 3x
		openBlock(TABLE);
		var s = WildcatterItems.STEEL_INGOT;
		tableCraftBy(by(0.48), 3, new String[] {"sgs", "srs", "scs"}, keys('s', s, 'g', Items.GLASS_PANE, 'r', Items.COMPARATOR, 'c', item("minecraft:copper_block")), 1, "switchgear main");
		tableCraftBy(by(0.88), 3, new String[] {"sgs", "srs", "scs"}, keys('s', s, 'g', Items.GLASS_PANE, 'r', Items.COMPARATOR, 'c', Items.COPPER_INGOT), 1, "switchgear feeder");
		closeScreen();
		waitQuest("switchgear");
		settle();
		stopFilm();
		standAt(new BlockPos(-474, S, 964), Vec3.atCenterOf(FEEDER.below()).add(0, 0.5, 0));
		server.runCommand("time set 5400");
		context.waitTicks(wanted(n, "swg_place") ? 60 : 3);
		startFilm(n, "swg_place");
		cue("P30");
		at(0.04);
		walkGround(-474, 965);
		hold(item("wildcatter:mv_switchgear_feeder"));
		useOn(FEEDER.below(), Direction.UP);
		mark("feeder: " + state(FEEDER) + " cells " + cellsOf(FEEDER));
		at(0.20);
		walkGround(-473, 965);
		hold(item("wildcatter:mv_switchgear_main"));
		useOn(MAIN.below(), Direction.UP);
		mark("main: " + state(MAIN) + " cells " + cellsOf(MAIN));
		at(0.45);
		meter(MAIN);
		settle();
		stopFilm();
	}

	// ---- 18: the gas well now, the chapter, the hook and the title, the context shot, the outro (each its own take) -------------------------

	private void ends(int n) {
		// P32: the book
		standAt(new BlockPos(-472, S, 962), Vec3.atCenterOf(GEN));
		server.runCommand("time set 11000");
		context.waitTicks(wanted(n, "chapter") ? 60 : 3);
		startFilm(n, "chapter");
		cue("P32");
		at(0.04);
		bookOpen("steam_plant");
		at(0.35);
		openQuest("switchgear");
		at(0.70);
		openQuest("crude_unit");
		mark("next: crude_unit");
		settle();
		context.setScreen(() -> null);
		stopFilm();
		spectator();
		// P31: the gas well now, high from the north-east, the plant, the set and the line
		server.runCommand("time set 9000");
		droneAt(n, "well_now", GEN, cam(16.0, 18.0, -6.0, 2.0, 1.0, 8.0), cam(18.0, 16.0, 6.0, 0.0, 1.0, 12.0), "P31");
		// P1, the hook: low past the plant at dusk
		server.runCommand("time set 12300");
		droneAt(n, "hook", GEN, cam(-9.0, 3.0, -7.0, 2.0, 2.0, 0.0), cam(-8.0, 3.5, 4.0, 2.0, 2.0, 0.0), "P01");
		// the title card: the generator and its GSU at dusk
		droneAt(n, "title", GEN, cam(-6.0, 5.0, 8.0, 0.0, 1.5, 0.0), cam(-4.0, 5.5, 10.0, 0.0, 1.5, 0.0), "title");
		// P2: high over the gas well, the plant north of the set
		server.runCommand("time set 8000");
		droneAt(n, "context", GEN, cam(24.0, 26.0, 22.0, 2.0, 0.0, 8.0), cam(8.0, 26.0, 28.0, 2.0, 0.0, 8.0), "P02");
		// the outro: low from the south-west at sunset
		server.runCommand("time set 12500");
		droneAt(n, "outro", GEN, cam(-10.0, 4.0, 14.0, 2.0, 2.0, 0.0), cam(-12.0, 4.5, 9.0, 2.0, 2.0, 0.0), "outro");
		survival();
		log("at the end: plant " + reading(GEN) + " | GSU " + reading(GSU) + " | set " + reading(PANEL) + " | unit " + reading(UNIT) + " | main " + reading(MAIN)
				+ " | small set " + reading(SMALL) + " | feed pump " + reading(FEED) + " | lamps " + java.util.Arrays.stream(LAMPS).map(l -> state(l).toString()).toList());
	}

	private void atTable(int n, String film, long time) {
		standAt(TABLE.south(2), Vec3.atCenterOf(TABLE));
		server.runCommand("time set " + time);
		context.waitTicks(wanted(n, film) ? 60 : 3);
		startFilm(n, film);
	}

	/** A spectator drone move between two cameras relative to {@code origin}, as long as cue {@code name}'s target. */
	private void droneAt(int n, String name, BlockPos origin, Bfs05Films.Cam a, Bfs05Films.Cam b, String cueId) {
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
