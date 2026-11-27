package dev.wildcatter.gametest;

import dev.wildcatter.WildcatterItems;
import dev.wildcatter.client.quest.QuestScreen;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * Built From Scratch, episode 1 "Day one to steel": the Getting started chapter played through in survival, in the series' world (seed
 * {@link Bfs01Films#SEED}, base site {@link Bfs01Films#SX}, {@link Bfs01Films#SZ}). Everything the player does goes through the client's own
 * paths, as a player's mouse would drive them: blocks broken with the client's destroy loop (the cracks and the arm swinging), blocks placed
 * and used with useItemOn, items moved in the crafting, inventory and furnace screens with container clicks while the cursor (drawn, as the
 * film has no system cursor) moves to each slot. The quests tick from what the player carries and earns. What is gathered off camera is given by
 * command and each film that follows it is logged with what was given, so the edit can label it.
 * <p>
 * Films land in {@code build/scene-videos/bfs01__NN_<shot>.mov}; BFS_SHOTS=3,4 films only those (the rest still run, unfilmed). BFS_FAST=1
 * skips the smelting waits of unfilmed shots (the outputs given instead), for working on the later shots.
 * <p>
 * Re-film r2 (2026-11-27, film60): each stretch of a take is as long as the narration over it ({@link #TARGET}, the longer of the
 * English and Spanish paragraph), the action paced to fill it once, in story order; the log's "cue" lines give the edit its cuts. The
 * hook and the ending are takes of their own. The drawn cursor glides a frame at a time.
 */
final class Bfs01Episode {
	private final Bfs01Films t;
	private final ClientGameTestContext context;
	private final TestServerContext server;
	private final Set<String> shots;
	private final boolean fast;
	/** The base: its corner (the crafting table), on the ground's top block at y. */
	private int bx, by, bz;
	private BlockPos table, furnace, blast, campfire, chest;
	private boolean filming;
	private String filmId = "";
	private long filmStart;

	Bfs01Episode(Bfs01Films t) {
		this.t = t;
		this.context = t.context;
		this.server = t.server;
		String pick = System.getenv("BFS_SHOTS");
		shots = pick == null || pick.isBlank() ? null : Set.of(pick.split(","));
		fast = System.getenv("BFS_FAST") != null;
	}

	static void log(String s) {
		System.out.println("[bfs01] " + s);
	}

	private boolean wanted(int n) {
		return shots == null || shots.contains(String.valueOf(n));
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
		// the base: on the flat by the forest's edge, north of the stony gully
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
		chest = new BlockPos(bx - 1, by + 1, bz);
		campfire = new BlockPos(bx + 1, by + 1, bz + 5);
		log("base at " + bx + " " + by + " " + bz + "; ground " + server.computeOnServer(srv -> srv.overworld().getBlockState(new BlockPos(bx, by, bz)).toString()));
		// the grass and flowers round the base cut down off camera (a player knocks them down walking about; standing in tall grass hides
		// the view), and every recipe known, so the recipe book's "new recipe" notes do not cover the top right of each film
		server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			int cut = 0;
			for (BlockPos p : BlockPos.betweenClosed(bx - 10, by + 1, bz - 8, bx + 10, by + 3, bz + 12)) {
				BlockState st = level.getBlockState(p);
				if (st.canBeReplaced() && !st.isAir() && st.getFluidState().isEmpty() || st.is(BlockTags.FLOWERS)) {
					level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
					cut++;
				}
			}
			log("grass and flowers cut round the base: " + cut);
			return null;
		});
		server.runCommand("recipe give @p *");
		String[] steps = {"site", "tree", "craft", "stone", "coal", "campfire", "furnace", "iron", "smelt", "copper", "blastFurnace", "mistake", "steel",
				"manual", "done", "ending", "hook"};
		for (int i = 0; i < steps.length; i++) {
			int n = i + 1;
			log("step " + n + " " + steps[i]);
			try {
				switch (steps[i]) {
					case "site" -> site(n);
					case "tree" -> tree(n);
					case "craft" -> craft(n);
					case "stone" -> stone(n);
					case "coal" -> coal(n);
					case "campfire" -> campfire(n);
					case "furnace" -> furnace(n);
					case "iron" -> iron(n);
					case "smelt" -> smelt(n);
					case "copper" -> copper(n);
					case "blastFurnace" -> blastFurnace(n);
					case "mistake" -> mistake(n);
					case "steel" -> steel(n);
					case "manual" -> manual(n);
					case "done" -> done(n);
					case "ending" -> ending(n);
					case "hook" -> hook(n);
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
	}

	// ---- filming ------------------------------------------------------------------------------------------------------------------------

	private Filming film;

	/**
	 * Each stretch of a take is timed to the narration over it (re-film r2, 2026-11-27): the longer of the English and Spanish paragraph
	 * (lead, sentences, pauses and pad, as build_ep.py lays them) plus a margin, in seconds of film (a shot shown sped up runs its speed
	 * times as long). {@link #cue} starts one; the next cue (or the film's end) first waits out what is left of it, so the footage under a
	 * paragraph is never shorter than its voice. A paragraph shown in two shots has two cues (P07, P07b).
	 */
	private static final Map<String, Double> TARGET = Map.ofEntries(
			Map.entry("title", 5.8), Map.entry("P02", 20.4), Map.entry("P03", 15.0), Map.entry("P04", 20.0),
			Map.entry("P05", 11.3), Map.entry("P06", 12.1), Map.entry("P07", 5.0), Map.entry("P07b", 17.6),
			Map.entry("P08", 14.0), Map.entry("P09", 11.3), Map.entry("P10", 13.2),
			Map.entry("P11", 12.8), Map.entry("P12", 7.2), Map.entry("P12b", 6.0), Map.entry("P13", 11.0), Map.entry("P13b", 9.5),
			Map.entry("P14", 11.2), Map.entry("P15", 9.6), Map.entry("P16", 5.5), Map.entry("P16b", 14.6), Map.entry("P17", 7.7),
			Map.entry("P18", 14.5), Map.entry("P19", 10.5), Map.entry("P20", 16.2), Map.entry("P21", 17.3 * 8), Map.entry("P22", 8.6),
			Map.entry("P23", 9.2), Map.entry("P24", 5.0), Map.entry("P24b", 17.4 * 12), Map.entry("P25", 13.1 * 6),
			Map.entry("P26", 12.8), Map.entry("P27", 8.9), Map.entry("P28", 9.9), Map.entry("P29", 18.8), Map.entry("P30", 6.3),
			Map.entry("P31", 4.8), Map.entry("P31b", 11.8 * 4), Map.entry("P32", 5.0), Map.entry("P32b", 5.6),
			Map.entry("P33", 13.8), Map.entry("P34", 7.6), Map.entry("P35", 17.3), Map.entry("P36", 19.3), Map.entry("P37", 12.5),
			Map.entry("P37b", 6.5), Map.entry("outro", 8.6), Map.entry("P01", 8.5), Map.entry("P01b", 4.6));
	private String cueName;
	private double cueAt, cueLen;

	private void startFilm(int n, String name) {
		if (!wanted(n) && !(shots != null && shots.contains(name))) {
			return;
		}
		filmId = String.format(Locale.ROOT, "bfs01__%02d_%s", n, name);
		context.waitTicks(2);
		film = context.computeOnClient(client -> Filming.start(client, filmId));
		filmStart = System.nanoTime();
		filming = true;
		cueName = null;
		context.waitTicks(10);
	}

	private void stopFilm() {
		if (film != null) {
			settle();
			context.waitTicks(10);
			film.stop();
			log("filmed " + filmId);
			film = null;
		}
		filming = false;
		cueName = null;
	}

	/** The film's time now, in seconds of film (frames grabbed over 60), or 0 when not filming. */
	private double filmSec() {
		return film == null ? 0 : film.frames() / 60.0;
	}

	/** The footage for paragraph {@code name} starts here (logged for the spec); the cue before it is waited out first. */
	private void cue(String name) {
		if (film == null) {
			return;
		}
		settle();
		Double len = TARGET.get(name);
		if (len == null) {
			throw new IllegalStateException("no target for cue " + name);
		}
		cueName = name;
		cueAt = filmSec();
		cueLen = len;
		log(String.format(Locale.ROOT, "cue %s %s at %.3f s target %.2f s", filmId, name, cueAt, cueLen));
	}

	/** Waits out what is left of the current cue's target (the action ran short), or logs how far it ran over. */
	private void settle() {
		if (film == null || cueName == null) {
			return;
		}
		double end = cueAt + cueLen, now = filmSec();
		if (now > end + 0.05) {
			log(String.format(Locale.ROOT, "OVERRUN %s %s by %.2f s (ran %.2f of %.2f)", filmId, cueName, now - end, now - cueAt, cueLen));
		} else if (end - now > 0.05) {
			log(String.format(Locale.ROOT, "fill %s %s %.2f s", filmId, cueName, end - now));
			idle((int) Math.ceil((end - now) * 20));
		}
		cueName = null;
	}

	/** Seconds of the current cue left, or 0. */
	private double left() {
		return film == null || cueName == null ? 0 : Math.max(0, cueAt + cueLen - filmSec());
	}

	/**
	 * A pause in the action: in the world the head drifts a degree or two and back (a still player is never quite still); in a screen
	 * the cursor rests. Waits {@code ticks} either way.
	 */
	private void idle(int ticks) {
		if (ticks <= 0) {
			return;
		}
		boolean still = context.computeOnClient(c -> c.gui.screen() != null || c.player.isSpectator());
		if (still || ticks < 16) {
			context.waitTicks(ticks);
			return;
		}
		float[] from = context.computeOnClient(c -> new float[] {c.player.getYRot(), c.player.getXRot()});
		for (int i = 1; i <= ticks; i++) {
			double s = Math.sin(Math.PI * i / ticks);
			float yaw = (float) (from[0] + 1.6 * s), pitch = (float) (from[1] - 0.6 * s);
			context.runOnClient(c -> {
				c.player.setYRot(yaw);
				c.player.setXRot(pitch);
				c.player.setYHeadRot(yaw);
			});
			context.waitTick();
		}
	}

	/** Logs a beat with its time from the film's start (the edit cuts to these). */
	private void mark(String what) {
		if (filming) {
			log(String.format(Locale.ROOT, "event %s %s at %.2f s", filmId, what, filmSec()));
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
	/** A glide under way while a 60 fps take films: from x, y to x, y (window pixels), from frame, over frames. */
	private static volatile double[] glidePath;

	/**
	 * A small arrow drawn over every screen at the mouse, as the film has no system cursor. While a 60 fps take films, a glide moves the
	 * mouse itself a frame at a time ({@link Filming#perFrame}), so the arrow and any stack carried on it move as smoothly as the camera
	 * (moved a tick at a time, they stepped every third frame).
	 */
	private void cursorHook() {
		if (hooked) {
			return;
		}
		hooked = true;
		Filming.perFrame = frame -> {
			double[] p = glidePath;
			if (p == null) {
				return;
			}
			Minecraft mc = Minecraft.getInstance();
			if (mc.gui.screen() == null) {
				return;
			}
			double s = Math.max(0, Math.min(1, (frame - p[4]) / p[5]));
			s = s * s * (3 - 2 * s);
			double x = p[0] + (p[2] - p[0]) * s, y = p[1] + (p[3] - p[1]) * s;
			((net.fabricmc.fabric.mixin.client.gametest.input.MouseHandlerAccessor) mc.mouseHandler).invokeOnMove(mc.getWindow().handle(), x, y,
					x - mc.mouseHandler.xpos(), y - mc.mouseHandler.ypos());
		};
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
			throw new IllegalStateException("no " + item + " in the hotbar: " + inventory());
		}
		context.getInput().pressKey(o -> o.keyHotbarSlots[i]);
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
	/** The hand's pace in screens: ticks a glide to a slot takes, and ticks after each click. {@link #pace} sets both. */
	private int glideTicks = 7, clickTicks = 5;

	private void pace(int glide, int after) {
		glideTicks = glide;
		clickTicks = after;
	}

	/** Window pixels over GUI pixels. */
	private double guiScale() {
		return context.computeOnClient(c -> (double) c.getWindow().getScreenWidth() / c.getWindow().getGuiScaledWidth());
	}

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

	/** Rests the cursor over a slot for {@code ticks} (as a player looks at what is there), at a slower glide. */
	private void hover(int slot, int glide, int ticks) {
		int was = glideTicks;
		glideTicks = glide;
		moveTo(slot);
		glideTicks = was;
		context.waitTicks(ticks);
	}

	private void glide(double[] to, int ticks) {
		double[] from = mouse.clone();
		if (film != null) {
			// a frame at a time, on the client (cursorHook); the test thread waits it out
			glidePath = new double[] {from[0], from[1], to[0], to[1], film.frames(), ticks * 3.0};
			context.waitTicks(ticks);
			context.waitTick();
			glidePath = null;
			context.getInput().setCursorPos(to[0], to[1]);
		} else {
			for (int i = 1; i <= ticks; i++) {
				double s = (double) i / ticks;
				s = s * s * (3 - 2 * s);
				context.getInput().setCursorPos(from[0] + (to[0] - from[0]) * s, from[1] + (to[1] - from[1]) * s);
				context.waitTick();
			}
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
		context.waitTicks(clickTicks);
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

	/** Waits (up to 15 s) for {@code id} to tick, then {@code after} ticks more (the chat line comes up). */
	private void waitQuest(String id, int after) {
		for (int i = 0; i < 60 && !questDone(id); i++) {
			context.waitTicks(5);
		}
		if (!questDone(id)) {
			throw new IllegalStateException("quest " + id + " did not tick: " + questsDone() + " | " + inventory());
		}
		mark("quest " + id + " done");
		context.waitTicks(after);
	}

	private void waitQuest(String id) {
		waitQuest(id, 25);
	}

	/** Opens the quest book from the hotbar on {@code quest}'s page (left open). */
	private void openBook(String quest) {
		hold(WildcatterItems.QUEST_BOOK);
		context.runOnClient(c -> c.gui.toastManager().clear());
		useHeld();
		context.waitTicks(6);
		pageTo(quest);
	}

	private void pageTo(String quest) {
		context.runOnClient(c -> {
			if (c.gui.screen() instanceof QuestScreen q) {
				q.open(Identifier.parse("wildcatter:" + quest), true);
			}
		});
		mark("book on " + quest);
	}

	private void closeBook() {
		cursorOn = false;
		context.setScreen(() -> null);
		context.waitTicks(4);
	}

	/** The open quest page's Claim button (GUI pixels: x, y, w, h), or null: the one 14-pixel-high hit the page lays out. */
	private int[] claimButton() {
		return context.computeOnClient(c -> {
			if (!(c.gui.screen() instanceof QuestScreen q)) {
				return null;
			}
			try {
				java.lang.reflect.Field f = QuestScreen.class.getDeclaredField("hits");
				f.setAccessible(true);
				for (Object h : (List<?>) f.get(q)) {
					int[] r = new int[4];
					String[] names = {"x", "y", "w", "h"};
					for (int i = 0; i < 4; i++) {
						java.lang.reflect.Method m = h.getClass().getDeclaredMethod(names[i]);
						m.setAccessible(true);
						r[i] = (int) m.invoke(h);
					}
					if (r[3] == 14) {
						return r;
					}
				}
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException(e);
			}
			return null;
		});
	}

	/** Moves the drawn cursor onto the open page's Claim button over {@code ticks}; with {@code press}, clicks it as a mouse does. */
	private boolean toClaim(int ticks, boolean press) {
		int[] b = claimButton();
		if (b == null) {
			log("no Claim button on the page");
			return false;
		}
		double k = guiScale();
		if (!cursorOn) {
			centreMouse();
			cursorOn = true;
			context.waitTicks(4);
		}
		glide(new double[] {(b[0] + b[2] * 0.45) * k, (b[1] + b[3] * 0.55) * k}, ticks);
		if (press) {
			context.waitTicks(8);
			context.getInput().pressMouse(0);
			mark("Claim pressed");
		}
		return true;
	}

	/** Opens the quest book from the hotbar on {@code quest}'s page, holds it and closes it (unfilmed books, the step's own logic). */
	private void book(String quest, int holdTicks) {
		openBook(quest);
		context.waitTicks(holdTicks);
		closeBook();
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

	// ---- 1: the site ------------------------------------------------------------------------------------------------------------------------

	/** The untouched site from the air, early morning: the grass flat, the forest's edge, the gully and the river beyond. */
	private void site(int n) {
		spectator();
		// the title card's ground: in from the river side, east of the base, low over the grass towards the forest's edge
		drone(n, "title", new String[] {"title"}, cam(42, 20, -6, 0, 2, 4), cam(36, 17, 14, 0, 2, 4));
		// P02: low over the base's own ground, from the plain in the west towards the forest, the gully below on the right
		drone(n, "site2", new String[] {"P02"}, cam(-38, 12, 8, 4, 2, 2), cam(-6, 9, 10, 10, 3, 0));
		// P03: high, from the south-west, across to the south: the flat, the forest's edge, the gully and the river;
		// P04: on round, turning to look back west over the level ground for the plant
		drone(n, "site", new String[] {"P03", "P04"}, cam(-40, 40, 40, 0, 0, 0), cam(15, 34, 50, 0, 0, 0), cam(36, 30, 18, -30, 0, 4));
	}

	/** A camera relative to the base corner (bx, by, bz). */
	private Bfs01Films.Cam cam(double x, double y, double z, double tx, double ty, double tz) {
		return new Bfs01Films.Cam(x, y, z, tx, ty, tz);
	}

	/**
	 * A spectator drone move through the cameras, HUD hidden: one leg a cue, each leg as long as its cue's target (eased in and out), so
	 * the move fills the narration over it.
	 */
	private void drone(int n, String name, String[] cues, Bfs01Films.Cam... cams) {
		t.ox = bx;
		t.gy = by;
		t.oz = bz;
		boolean on = wanted(n) || shots != null && shots.contains(name);
		t.camera(cams[0], true);
		context.waitTicks(on ? 120 : 2);
		t.camera(cams[0], true);
		context.waitTicks(10);
		startFilm(n, name);
		for (int leg = 0; leg < cues.length; leg++) {
			cue(cues[leg]);
			int ticks = on ? (int) Math.ceil(TARGET.get(cues[leg]) * 20) + 2 : 2;
			Bfs01Films.Cam a = cams[Math.min(leg, cams.length - 1)], b = cams[Math.min(leg + 1, cams.length - 1)];
			for (int i = 0; i <= ticks; i++) {
				t.camera(a.lerp(b, (double) i / ticks), false);
				context.waitTick();
			}
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

	/** Walks the player in a straight line to {@code to} (feet), {@code perBlock} ticks a block, as a player walks on level ground. */
	private void walk(BlockPos to, int perBlock) {
		Vec3 from = context.computeOnClient(c -> c.player.position()), end = Vec3.atBottomCenterOf(to);
		int steps = (int) Math.max(1, from.distanceTo(end) * perBlock);
		for (int i = 1; i <= steps; i++) {
			double s = (double) i / steps;
			s = s * s * (3 - 2 * s);
			Vec3 p = from.lerp(end, s);
			context.runOnClient(c -> c.player.setPos(p.x, p.y, p.z));
			context.waitTick();
		}
	}

	private void walk(BlockPos to) {
		walk(to, 5);
	}

	/** Walks to {@code to} looking at {@code at} all the way (the head turns as the body moves). */
	private void walkLooking(BlockPos to, Vec3 at, int perBlock) {
		Vec3 from = context.computeOnClient(c -> c.player.position()), end = Vec3.atBottomCenterOf(to);
		float[] f0 = context.computeOnClient(c -> new float[] {c.player.getYRot(), c.player.getXRot()});
		int steps = (int) Math.max(1, from.distanceTo(end) * perBlock);
		for (int i = 1; i <= steps; i++) {
			double s = (double) i / steps;
			s = s * s * (3 - 2 * s);
			Vec3 p = from.lerp(end, s);
			float[] yp = yawPitch(p.add(0, 1.62, 0), at);
			double w = Math.min(1, i / 8.0);   // the head comes round over the first steps
			float dy = yp[0] - f0[0];
			while (dy > 180) {
				dy -= 360;
			}
			while (dy < -180) {
				dy += 360;
			}
			float yaw = (float) (f0[0] + dy * w), pitch = (float) (f0[1] + (yp[1] - f0[1]) * w);
			context.runOnClient(c -> {
				c.player.setPos(p.x, p.y, p.z);
				c.player.setYRot(yaw);
				c.player.setXRot(pitch);
				c.player.setYHeadRot(yaw);
			});
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

	/** The farthest of up to {@code max} cells back from {@code to} along {@code dir} that the player can walk in from. */
	private BlockPos backFrom(BlockPos to, Direction dir, int max) {
		BlockPos start = to;
		for (int k = 1; k <= max; k++) {
			BlockPos c = to.relative(dir, k);
			if (!clear(c) || !clear(c.above()) || !solid(c.below())) {
				break;
			}
			start = c;
		}
		return start;
	}

	/**
	 * Punch a tree: two oaks at the forest's edge, the bottom two logs of each, so the camera stays level on the trunk (a log higher up
	 * means looking up into the leaves). Four logs finish the quest.
	 */
	private void tree(int n) {
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
		BlockPos a = null, b = null, fa = null, fb = null;
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
					fa = s1;
					fb = s2;
					break search;
				}
			}
		}
		if (a == null) {
			throw new IllegalStateException("no two oaks near the base: " + trunks.size() + " trunks");
		}
		trunk = a;
		log("trees at " + a.toShortString() + " and " + b.toShortString() + "; stand " + fa.toShortString() + ", " + fb.toShortString());
		survival();
		// a few steps in, as a player walks up to the tree, the quest book in hand (slot 1) as a new player has it
		Direction back = Direction.getApproximateNearest(fa.getX() - a.getX(), 0, fa.getZ() - a.getZ());
		BlockPos start = backFrom(fa, back, 3);
		standAt(start, Vec3.atCenterOf(a.above()));
		context.getInput().pressKey(o -> o.keyHotbarSlots[0]);
		context.waitTicks(wanted(n) ? 100 : 5);
		startFilm(n, "tree");
		cue("P05");
		context.waitTicks(24);
		context.getInput().pressKey(o -> o.keyHotbarSlots[8]);   // an empty hand (the logs fill from slot 2)
		mark("empty hand");
		context.waitTicks(8);
		walk(fa, 7);
		mark("at the first tree");
		int got = 0;
		for (BlockPos log : new BlockPos[] {a, a.above()}) {
			if (dig(log, 140)) {
				got++;
				mark("log " + got);
			}
		}
		cue("P06");
		face(Vec3.atCenterOf(b.above()), 12);
		walk(fb, 5);
		mark("at the second tree");
		for (BlockPos log : new BlockPos[] {b, b.above()}) {
			if (dig(log, 140)) {
				got++;
				mark("log " + got);
			}
		}
		mark("logs in hand " + count(Items.OAK_LOG));
		waitQuest("wood", 0);
		cue("P07");   // "With four logs in the inventory, the chat says the quest is done"
		stopFilm();
		// the quest book: Punch a tree ticked; the Claim button waits for its three apples
		startFilm(n, "tree_book");
		openBook("wood");
		cue("P07b");
		context.waitTicks(20 * 11);
		toClaim(30, false);
		mark("over Claim");
		settle();
		closeBook();
		stopFilm();
	}

	private Item logItem() {
		return count(Items.OAK_LOG) >= count(Items.BIRCH_LOG) ? Items.OAK_LOG : Items.BIRCH_LOG;
	}

	private Item plankItem() {
		return logItem() == Items.OAK_LOG ? Items.OAK_PLANKS : Items.BIRCH_PLANKS;
	}

	// ---- 3: planks, the crafting table, sticks and the wooden pickaxe: one take ------------------------------------------------------------

	private void craft(int n) {
		Item log = logItem();
		Item plank = plankItem();
		// stand at the base, looking at its ground
		standAt(new BlockPos(bx, by + 1, bz + 3), Vec3.atCenterOf(table.below()));
		context.waitTicks(wanted(n) ? 60 : 3);
		startFilm(n, "craft");
		// P08: three logs into planks in the inventory's own 2x2 grid (slots 1-4, player 9-44, hotbar 36-44)
		cue("P08");
		openInventory();
		mark("inventory open");
		pace(12, 8);
		for (int cell = 1; cell <= 4; cell++) {
			hover(cell, 8, 3);   // the four cells of the small grid
		}
		int src = slotOf(log, 9);
		hover(src, 12, 10);
		left(src);
		for (int i = 0; i < 3; i++) {
			right(1);
			context.waitTicks(6);
		}
		left(src);
		mark("logs in the grid");
		hover(0, 12, 14);   // the result: four planks
		shift(0);
		mark("planks made: " + count(plank));
		// P09: the crafting table: four planks, one in each cell; set down and used, a 3x3 grid
		cue("P09");
		pace(9, 6);
		craft(2, new String[] {"pp", "pp"}, Map.of('p', plank), 1, 9, 36);
		mark("crafting table made");
		context.waitTicks(8);
		closeScreen();
		hold(Items.CRAFTING_TABLE);
		useOn(table.below(), Direction.UP);
		mark("table placed");
		context.waitTicks(12);
		openBlock(table);
		mark("table open");
		context.waitTicks(Math.max(0, (int) (left() * 20) - 2));
		// P10: sticks (two planks, one above the other, twice over), then the wooden pickaxe
		cue("P10");
		pace(7, 5);
		// crafting menu: result 0, grid 1-9, player 10-45, hotbar 37-45
		craft(3, new String[] {" p ", " p "}, Map.of('p', plank), 2, 10, 37);
		mark("sticks made: " + count(Items.STICK));
		LinkedHashMap<Character, Item> k = new LinkedHashMap<>();
		k.put('p', plank);
		k.put('s', Items.STICK);
		craft(3, new String[] {"ppp", " s ", " s "}, k, 1, 10, 37);
		mark("wooden pickaxe made");
		settle();
		closeScreen();
		stopFilm();
		log("table: " + state(table));
	}

	// ---- 4: stone, the stone pickaxe ----------------------------------------------------------------------------------------------------

	/** The gully south of the base: stone in its walls, coal in its floor. */
	private static final BlockPos GULLY = new BlockPos(-584, 60, 955);

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

	private BlockPos coalOre;
	private BlockPos gullyFeet;

	private void stone(int n) {
		coalOre = server.computeOnServer(srv -> {
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
		log("coal ore at " + (coalOre == null ? "none" : coalOre.toShortString()));
		gullyFeet = standFor(coalOre, coalOre.above(2), 4.2);
		log("gully stand at " + gullyFeet);
		List<BlockPos> stones = stoneNear(gullyFeet, 6);
		log("stone in reach: " + stones);
		hold(Items.WOODEN_PICKAXE);
		// first a look along the gully (away from the wall), then round to the stone
		Vec3 wall = Vec3.atCenterOf(stones.get(0));
		Vec3 eye = Vec3.atBottomCenterOf(gullyFeet).add(0, 1.62, 0);
		Vec3 along = eye.add(eye.subtract(wall).normalize().scale(6)).add(0, -1.2, 0);
		standAt(gullyFeet, eye.add(new Vec3(wall.z - eye.z, 0, eye.x - wall.x).normalize().scale(8)).add(0, -2, 0));
		server.runCommand("time set 2500");
		context.waitTicks(wanted(n) ? 100 : 5);
		startFilm(n, "stone");
		cue("P11");
		context.waitTicks(20);
		face(along, 70);
		context.waitTicks(10);
		face(wall, 70);
		int got = 0;
		for (BlockPos s : stones) {
			if (got >= 3) {
				break;
			}
			if (got == 1) {
				cue("P12");   // the first cobblestone picked up: the Stone Age toast
			}
			if (dig(s, 80)) {
				got++;
				mark("stone " + got);
			}
		}
		mark("cobblestone " + count(Items.COBBLESTONE));
		stopFilm();
		// the stone pickaxe at the table, and enough cobblestone for a furnace and smooth stone, mined off camera
		int cob = count(Items.COBBLESTONE);
		give("minecraft:cobblestone", Math.max(0, 3 + 8 + 3 - cob + 3), "more stone from the gully");
		BlockPos at = new BlockPos(bx, by + 1, bz + 2);
		BlockPos from = backFrom(at, Direction.SOUTH, 3);
		standAt(from, Vec3.atCenterOf(table));
		context.waitTicks(wanted(n) ? 60 : 3);
		startFilm(n, "stone_pick");
		cue("P12b");
		context.waitTicks(10);
		walkLooking(at, Vec3.atCenterOf(table), 9);
		openBlock(table);
		pace(9, 6);
		hover(slotOf(Items.COBBLESTONE, 10), 14, 16);   // the stone gathered off camera
		context.waitTicks(Math.max(0, (int) (left() * 20) - 2));
		cue("P13");
		LinkedHashMap<Character, Item> k = new LinkedHashMap<>();
		k.put('c', Items.COBBLESTONE);
		k.put('s', Items.STICK);
		craft(3, new String[] {"ccc", " s ", " s "}, k, 1, 10, 37);
		mark("stone pickaxe made");
		context.waitTicks(6);
		closeScreen();
		waitQuest("stone", 10);
		stopFilm();
		startFilm(n, "stone_book");
		openBook("stone");
		cue("P13b");
		settle();
		closeBook();
		stopFilm();
		claim("stone");   // 16 torches
	}

	// ---- 5: coal ----------------------------------------------------------------------------------------------------------------------------

	private void coal(int n) {
		hold(Items.WOODEN_PICKAXE);
		// from the gully's wall round to its floor, where the coal shows
		Vec3 eye = Vec3.atBottomCenterOf(gullyFeet).add(0, 1.62, 0);
		Vec3 ore = Vec3.atCenterOf(coalOre);
		Vec3 side = eye.add(new Vec3(ore.z - eye.z, 0, eye.x - ore.x).normalize().scale(5)).add(0, -1.5, 0);
		standAt(gullyFeet, side);
		context.waitTicks(wanted(n) ? 80 : 5);
		startFilm(n, "coal");
		cue("P14");
		context.waitTicks(16);
		face(ore, 50);
		context.waitTicks(10);
		hold(Items.STONE_PICKAXE);
		context.waitTicks(10);
		dig(coalOre, 80);
		mark("coal " + count(Items.COAL));
		stopFilm();
		if (count(Items.COAL) < 1) {
			give("minecraft:coal", 1, "the coal ore's drop missed");
		}
	}

	// ---- 6: the campfire -------------------------------------------------------------------------------------------------------------------

	private void campfire(int n) {
		// the campfire's three logs: one left from the tree, two more gathered off camera (labelled)
		int have = count(logItem());
		if (have < 3) {
			give(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(logItem()).toString(), 3 - have, "two more logs from the same trees");
		}
		standAt(new BlockPos(bx, by + 1, bz + 2), Vec3.atCenterOf(table));
		context.waitTicks(wanted(n) ? 60 : 3);
		startFilm(n, "campfire");
		openBlock(table);
		cue("P15");
		pace(5, 4);
		LinkedHashMap<Character, Item> k = new LinkedHashMap<>();
		k.put('s', Items.STICK);
		k.put('c', Items.COAL);
		k.put('l', logItem());
		craft(3, new String[] {" s ", "scs", "lll"}, k, 1, 10, 37);
		mark("campfire made");
		context.waitTicks(4);
		closeScreen();
		hold(Items.CAMPFIRE);
		face(Vec3.atCenterOf(campfire), 12);
		useOn(campfire.below(), Direction.UP);
		mark("campfire placed: " + state(campfire));
		context.waitTicks(Math.max(0, (int) (left() * 20) - 2));
		cue("P16");   // "The quest text says why it matters": the fire lit, its smoke going up
		waitQuest("campfire", 0);
		face(Vec3.atCenterOf(campfire).add(0, 1.2, 0), 60);
		stopFilm();
		startFilm(n, "campfire_book");
		openBook("campfire");
		cue("P16b");
		context.waitTicks(20 * 5);
		toClaim(26, true);   // 8 coal
		context.waitTicks(30);
		closeBook();
		if (hotbarOf(Items.COAL) >= 0) {
			hold(Items.COAL);   // the hotbar: the coal there
		}
		mark("coal after the reward: " + count(Items.COAL));
		stopFilm();
		log("coal after the reward: " + count(Items.COAL));
	}

	// ---- 7: the furnace ---------------------------------------------------------------------------------------------------------------------

	private void furnace(int n) {
		standAt(new BlockPos(bx + 1, by + 1, bz + 2), Vec3.atCenterOf(table));
		context.waitTicks(wanted(n) ? 60 : 3);
		startFilm(n, "furnace");
		openBlock(table);
		cue("P17");
		pace(5, 3);
		craft(3, new String[] {"ccc", "c c", "ccc"}, Map.of('c', Items.COBBLESTONE), 1, 10, 37);
		mark("furnace made");
		context.waitTicks(4);
		closeScreen();
		hold(Items.FURNACE);
		useOn(furnace.below(), Direction.UP);
		mark("furnace placed: " + state(furnace));
		context.waitTicks(16);
		stopFilm();
	}

	// ---- 8: iron (and copper) ore, below the gully ------------------------------------------------------------------------------------------

	/**
	 * The nearest iron or copper ore to the gully's floor; a passage one wide and two high is dug to it off camera (from the gully, in steps
	 * down), lit with the stone quest's torches, and the player stands at its top facing down it.
	 */
	private final List<BlockPos> passage = new ArrayList<>();

	/** Walks down the passage from cell {@code from} to the face, {@code perCell} ticks a cell, looking at the ore. */
	private void downPassage(BlockPos target, int from, int perCell) {
		for (int i = from + 1; i < passage.size(); i++) {
			Vec3 a = Vec3.atBottomCenterOf(passage.get(i - 1)), b = Vec3.atBottomCenterOf(passage.get(i));
			for (int k = 1; k <= perCell; k++) {
				Vec3 p = a.lerp(b, (double) k / perCell);
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
		context.waitTicks(8);
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
		log("dug " + dug.size() + " blocks off camera to " + ore + " at " + target.toShortString() + "; stand at " + back.toShortString()
				+ "; passage " + passage.size() + " cells");
		passage.add(back);
		return target;
	}

	/** Mines a passage's ore on camera: from {@code fromCell} cells down the passage (looking down it), to the face, the ore, its drop. */
	private void mineOre(int n, String name, String cueName, Block block, Item drop, int cellsBack, int perCell) {
		hold(Items.STONE_PICKAXE);
		BlockPos ore = oreFace(block);
		int from = Math.max(0, passage.size() - 1 - cellsBack);
		standAt(passage.get(from), Vec3.atCenterOf(ore));
		context.waitTicks(wanted(n) || shots != null && shots.contains(name) ? 120 : 5);
		startFilm(n, name);
		cue(cueName);
		context.waitTicks(14);
		downPassage(ore, from, perCell);
		mark("at the face");
		context.waitTicks(10);
		dig(ore, 80);
		mark(name + " drop " + count(drop));
		stopFilm();
	}

	private void iron(int n) {
		server.runCommand("time set 4000");
		mineOre(n, "iron", "P18", Blocks.IRON_ORE, Items.RAW_IRON, 40, 9);
		give("minecraft:raw_iron", 13 - count(Items.RAW_IRON), "the rest of the iron ore, mined off camera");
	}

	// ---- 9: smelting -------------------------------------------------------------------------------------------------------------------------

	/** Puts {@code n} of {@code item} in the open furnace's top slot and {@code fuel} coal under it (furnace menu: 0 in, 1 fuel, 2 out, player 3-38, hotbar 30-38). */
	private void load(Item item, int n, int fuel) {
		int src = slotOf(item, 3);
		left(src);
		if (n >= context.computeOnClient(c -> c.player.containerMenu.getCarried().getCount())) {
			left(0);
		} else {
			for (int i = 0; i < n; i++) {
				right(0);
			}
			left(src);
		}
		if (fuel > 0) {
			int coal = slotOf(Items.COAL, 3);
			left(coal);
			for (int i = 0; i < fuel; i++) {
				right(1);
			}
			if (carrying()) {
				left(coal);
			}
		}
	}

	private int furnaceOut(BlockPos at) {
		return server.computeOnServer(srv -> ((net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity) srv.overworld().getBlockEntity(at)).getItem(2).getCount());
	}

	private void cook(BlockPos at, int items, int n, boolean filmed) {
		if (!filmed && fast) {
			server.computeOnServer(srv -> {
				var f = (net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity) srv.overworld().getBlockEntity(at);
				ItemStack in = f.getItem(0);
				var input = new net.minecraft.world.item.crafting.SingleRecipeInput(in);
				var type = f.getBlockState().is(Blocks.BLAST_FURNACE) ? net.minecraft.world.item.crafting.RecipeType.BLASTING : net.minecraft.world.item.crafting.RecipeType.SMELTING;
				var r = srv.getRecipeManager().getRecipeFor(type, input, srv.overworld()).orElseThrow();
				ItemStack out = r.value().assemble(input);
				out.setCount(in.getCount() + f.getItem(2).getCount());
				f.setItem(2, out);
				f.setItem(0, ItemStack.EMPTY);
				return null;
			});
			return;
		}
		for (int i = 0; i < 20 * 12 * items + 100 && furnaceOut(at) < items; i += 10) {
			if (i % 200 == 0) {
				mark("out " + furnaceOut(at));
			}
			context.waitTicks(10);
		}
		mark("out " + furnaceOut(at));
	}

	/** Waits (filmed) until the furnace's output first holds {@code k}. */
	private void cookUntil(BlockPos at, int k) {
		for (int i = 0; i < 20 * 15 * k && furnaceOut(at) < k; i += 5) {
			context.waitTicks(5);
		}
		mark("out " + furnaceOut(at));
	}

	private void smelt(int n) {
		// from beside where the campfire stands, a few steps up to the furnace
		BlockPos at = new BlockPos(bx + 2, by + 1, bz + 2);
		BlockPos from = backFrom(at, Direction.SOUTH, 2);
		standAt(from, Vec3.atCenterOf(furnace));
		server.runCommand("time set 5000");
		context.waitTicks(wanted(n) ? 60 : 3);
		startFilm(n, "smelt");
		cue("P19");
		context.waitTicks(10);
		walkLooking(at, Vec3.atCenterOf(furnace), 10);
		openBlock(furnace);
		pace(9, 6);
		hover(slotOf(Items.RAW_IRON, 3), 14, 30);   // thirteen raw iron: the stack's count
		hover(0, 14, 10);   // the top slot
		hover(1, 12, 10);   // the fuel slot
		context.waitTicks(Math.max(0, (int) (left() * 20) - 2));
		cue("P20");
		load(Items.RAW_IRON, 13, 2);
		mark("loaded 13 raw iron, 2 coal");
		hover(2, 16, 10);   // the arrow and the output
		cookUntil(furnace, 1);
		mark("first ingot");
		cue("P21");   // shown at 8x
		cook(furnace, 13, n, wanted(n));
		shift(2);
		mark("iron ingots " + count(Items.IRON_INGOT));
		context.waitTicks(20);
		closeScreen();
		waitQuest("iron", 10);
		stopFilm();
		startFilm(n, "smelt_book");
		openBook("iron");
		cue("P22");
		settle();
		closeBook();
		stopFilm();
		claim("iron");
	}

	// ---- 10: copper, and smooth stone ---------------------------------------------------------------------------------------------------------

	private void copper(int n) {
		mineOre(n, "copper", "P23", Blocks.COPPER_ORE, Items.RAW_COPPER, 6, 8);
		give("minecraft:raw_copper", Math.max(0, 17 - count(Items.RAW_COPPER)), "the rest of the copper ore, mined off camera");
		// copper and then the smooth stone in the furnace
		standAt(new BlockPos(bx + 2, by + 1, bz + 2), Vec3.atCenterOf(furnace));
		server.runCommand("time set 6500");
		context.waitTicks(wanted(n) ? 60 : 3);
		startFilm(n, "copper_smelt");
		cue("P24");
		openBlock(furnace);
		pace(6, 4);
		load(Items.RAW_COPPER, 17, 3);
		mark("loaded 17 raw copper, 3 coal");
		context.waitTicks(Math.max(0, (int) (left() * 20) - 2));
		cue("P24b");   // shown at 12x
		cook(furnace, 17, n, wanted(n));
		shift(2);
		mark("copper ingots " + count(Items.COPPER_INGOT));
		context.waitTicks(10);
		closeScreen();
		waitQuest("copper", 10);
		stopFilm();
		// smooth stone: cobblestone to stone to smooth stone, three of each (the furnace's own recipes), on the copper's last coal
		startFilm(n, "smooth");
		cue("P25");   // shown at 6x
		openBlock(furnace);
		pace(7, 5);
		load(Items.COBBLESTONE, 3, 1);   // a coal under it: the copper's last burns out before the sixth piece
		cook(furnace, 3, n, wanted(n));
		shift(2);
		load(Items.STONE, 3, lit(furnace) ? 0 : 1);
		cook(furnace, 3, n, wanted(n));
		shift(2);
		mark("smooth stone " + count(Items.SMOOTH_STONE));
		settle();
		closeScreen();
		stopFilm();
	}

	// ---- 11: the blast furnace ----------------------------------------------------------------------------------------------------------------

	private void blastFurnace(int n) {
		standAt(new BlockPos(bx + 2, by + 1, bz + 2), Vec3.atCenterOf(table));   // from here the blast furnace goes down facing south, as the furnace
		server.runCommand("time set 8000");
		LinkedHashMap<Character, Item> k = new LinkedHashMap<>();
		k.put('i', Items.IRON_INGOT);
		k.put('f', Items.FURNACE);
		k.put('s', Items.SMOOTH_STONE);
		// the recipe takes a furnace: a second one from cobblestone first (more stone gathered off camera)
		give("minecraft:cobblestone", Math.max(0, 8 - count(Items.COBBLESTONE)), "stone for a second furnace");
		context.waitTicks(wanted(n) ? 60 : 3);
		startFilm(n, "blast");
		cue("P26");
		openBlock(table);
		pace(4, 3);
		craft(3, new String[] {"ccc", "c c", "ccc"}, Map.of('c', Items.COBBLESTONE), 1, 10, 37);
		mark("second furnace made");
		craft(3, new String[] {"iii", "ifi", "sss"}, k, 1, 10, 37);
		mark("blast furnace made");
		cue("P27");
		context.waitTicks(6);
		closeScreen();
		hold(Items.BLAST_FURNACE);
		useOn(blast.below(), Direction.UP);
		mark("blast furnace placed: " + state(blast));
		context.waitTicks(20);
		// eight iron ingots left
		openInventory();
		pace(12, 6);
		hover(slotOf(Items.IRON_INGOT, 9), 14, 30);
		mark("iron left " + count(Items.IRON_INGOT));
		settle();
		closeScreen();
		stopFilm();
	}

	// ---- 12: what goes wrong: iron in the plain furnace -----------------------------------------------------------------------------------------

	private boolean lit(BlockPos at) {
		return server.computeOnServer(srv -> srv.overworld().getBlockState(at).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT));
	}

	private void mistake(int n) {
		for (int i = 0; i < 120 && lit(furnace); i++) {
			context.waitTicks(20);   // the last coal burns out
		}
		log("furnace lit before the mistake: " + lit(furnace));
		standAt(new BlockPos(bx + 2, by + 1, bz + 2), Vec3.atCenterOf(furnace));
		context.waitTicks(wanted(n) ? 60 : 3);
		startFilm(n, "mistake");
		cue("P28");
		openBlock(furnace);
		pace(10, 8);
		load(Items.IRON_INGOT, 8, 1);
		mark("8 iron ingots and a coal in the furnace");
		hover(2, 16, 10);
		cue("P29");   // nothing happens: the cursor goes over the flame, the arrow and the empty output
		for (int s : new int[] {1, 0, 2, 1, 2}) {
			hover(s, 30, 40);
		}
		mark("nothing: lit " + lit(furnace) + ", out " + furnaceOut(furnace));
		cue("P30");   // take them back out
		pace(9, 6);
		shift(0);
		shift(1);
		mark("taken back out");
		context.waitTicks(10);
		closeScreen();
		face(Vec3.atCenterOf(blast), 24);
		mark("the furnace, cold: lit " + lit(furnace));
		stopFilm();
	}

	// ---- 13: steel ------------------------------------------------------------------------------------------------------------------------------

	private void steel(int n) {
		standAt(new BlockPos(bx + 3, by + 1, bz + 2), Vec3.atCenterOf(blast));
		server.runCommand("time set 9000");
		context.waitTicks(wanted(n) ? 60 : 3);
		startFilm(n, "steel");
		cue("P31");
		openBlock(blast);
		pace(6, 4);
		load(Items.IRON_INGOT, 8, 1);
		mark("8 iron ingots and a coal in the blast furnace");
		context.waitTicks(Math.max(0, (int) (left() * 20) - 2));
		cue("P31b");   // shown at 4x
		cook(blast, 8, n, wanted(n));
		cue("P32");
		pace(8, 6);
		shift(2);
		mark("steel " + count(WildcatterItems.STEEL_INGOT));
		context.waitTicks(8);
		closeScreen();
		waitQuest("steel", 10);
		stopFilm();
		startFilm(n, "steel_book");
		openBook("steel");
		cue("P32b");
		settle();
		closeBook();
		stopFilm();
	}

	// ---- 14: the Field Manual -----------------------------------------------------------------------------------------------------------------

	private void manual(int n) {
		give("minecraft:paper", 3, "paper from sugar cane by the river");
		give("minecraft:leather", 1, "leather from a cow");
		standAt(new BlockPos(bx, by + 1, bz + 2), Vec3.atCenterOf(table));
		context.waitTicks(wanted(n) ? 60 : 3);
		startFilm(n, "manual");
		cue("P33");
		openBlock(table);
		pace(11, 8);
		hover(slotOf(Items.PAPER, 10), 14, 14);
		hover(slotOf(Items.LEATHER, 10), 12, 14);
		LinkedHashMap<Character, Item> k = new LinkedHashMap<>();
		k.put('p', Items.PAPER);
		k.put('l', Items.LEATHER);
		craft(3, new String[] {"pp ", "pl "}, k, 1, 10, 37);
		mark("book made");
		cue("P34");
		pace(7, 5);
		LinkedHashMap<Character, Item> m = new LinkedHashMap<>();
		m.put('b', Items.BOOK);
		m.put('c', Items.COPPER_INGOT);
		craft(3, new String[] {"bc"}, m, 1, 10, 37);
		mark("field manual made");
		context.waitTicks(6);
		closeScreen();
		waitQuest("field_manual", 10);
		stopFilm();
		// the cover, then page 1 (steam); the safety pages print the volts, so they are not filmed
		startFilm(n, "manual_read");
		cue("P35");
		hold(WildcatterItems.FIELD_MANUAL);
		useHeld();
		mark("manual open: " + context.computeOnClient(c -> String.valueOf(c.gui.screen())));
		context.waitTicks(150);
		context.runOnClient(c -> {
			if (c.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.BookViewScreen b) {
				b.setPage(1);
			}
		});
		mark("manual page 1 (steam)");
		settle();
		context.setScreen(() -> null);
		stopFilm();
	}

	// ---- 15: the chapter complete ---------------------------------------------------------------------------------------------------------------

	private void done(int n) {
		startFilm(n, "chapter");
		openBook("field_manual");
		cue("P37");
		context.waitTicks(120);
		pageTo("steam");
		mark("next: steam");
		settle();
		closeBook();
		stopFilm();
	}

	// ---- 16: the end of the day: the base, the campfire at dusk, the outro ------------------------------------------------------------------------

	private void ending(int n) {
		server.runCommand("time set 10500");
		spectator();
		// P36: three-quarter from the south-east, the row of table and furnaces with the campfire in front, its smoke off to the left
		drone(n, "top", new String[] {"P36"}, cam(8.5, 5.2, 9.0, 1.6, 0.3, 2.2), cam(5.8, 4.4, 9.6, 1.6, 0.3, 2.2));
		// P37b: the campfire at sundown, where the boiler goes next time
		server.runCommand("time set 12100");
		survival();
		hold(WildcatterItems.QUEST_BOOK);
		BlockPos near = new BlockPos(bx + 1, by + 1, bz + 8);
		standAt(near, Vec3.atCenterOf(campfire));
		context.waitTicks(wanted(n) ? 80 : 3);
		startFilm(n, "campfire_end");
		cue("P37b");
		walkLooking(new BlockPos(bx + 1, by + 1, bz + 7), Vec3.atCenterOf(campfire).add(0, 0.3, 0), 60);
		stopFilm();
		// the outro: up and away from the base at dusk
		server.runCommand("time set 12500");
		spectator();
		drone(n, "outro", new String[] {"outro"}, cam(3, 4, 12, 1.5, 0.5, 2.5), cam(10, 16, 26, 1.5, 0.5, 2.5));
		survival();
	}

	// ---- 17: the hook: steel in hand at the finished base (its own take, filmed last) ------------------------------------------------------------

	private void hook(int n) {
		server.runCommand("time set 11200");
		// steel in hand, looking over the campfire and the furnaces
		hold(WildcatterItems.STEEL_INGOT);
		standAt(new BlockPos(bx + 1, by + 1, bz + 4), Vec3.atCenterOf(blast).add(-1.5, -0.3, 0));
		context.waitTicks(wanted(n) ? 100 : 3);
		startFilm(n, "hook_hand");
		cue("P01");
		face(Vec3.atCenterOf(campfire).add(0, 0.2, 0), 70);
		face(Vec3.atCenterOf(furnace).add(0, 0.2, 0), 70);
		stopFilm();
		spectator();
		// a low pass across the north side of the row, the campfire beyond it: not the ending's angle
		drone(n, "hook_drone", new String[] {"P01b"}, cam(-4, 3.2, -5, 1.5, 0.6, 2.0), cam(6, 3.6, -5.5, 1.5, 0.6, 2.0));
		survival();
		log("final inventory: " + inventory());
	}
}
