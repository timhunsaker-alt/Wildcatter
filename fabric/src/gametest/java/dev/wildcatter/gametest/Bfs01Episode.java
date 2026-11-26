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
		String[] steps = {"site", "tree", "planks", "pickaxe", "stone", "coal", "campfire", "furnace", "iron", "smelt", "copper", "blastFurnace", "mistake", "steel",
				"manual", "done", "hook"};
		for (int i = 0; i < steps.length; i++) {
			int n = i + 1;
			log("step " + n + " " + steps[i]);
			try {
				switch (steps[i]) {
					case "site" -> site(n);
					case "tree" -> tree(n);
					case "planks" -> planks(n);
					case "pickaxe" -> pickaxe(n);
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

	private void startFilm(int n, String name) {
		if (!wanted(n) && !(shots != null && shots.contains(name))) {
			return;
		}
		filmId = String.format(Locale.ROOT, "bfs01__%02d_%s", n, name);
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

	// ---- 1: the site ------------------------------------------------------------------------------------------------------------------------

	/** The untouched site from the air, early morning: the grass flat, the forest's edge, the gully and the river beyond. */
	private void site(int n) {
		spectator();
		// high, from the south-west, across to the south: the flat, the forest's edge, the gully and the river
		drone(n, "site", cam(-40, 40, 40, 0, 0, 0), cam(15, 34, 50, 0, 0, 0), 520);
		// low over the base's own ground, from the plain in the west towards the forest, the gully below on the right
		drone(n, "site2", cam(-36, 12, 8, 4, 2, 2), cam(-8, 9, 10, 10, 3, 0), 440);
	}

	/** A camera relative to the base corner (bx, by, bz). */
	private Bfs01Films.Cam cam(double x, double y, double z, double tx, double ty, double tz) {
		return new Bfs01Films.Cam(x, y, z, tx, ty, tz);
	}

	/** A spectator drone move between two cameras, HUD hidden. */
	private void drone(int n, String name, Bfs01Films.Cam a, Bfs01Films.Cam b, int ticks) {
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

	private void tree(int n) {
		// the nearest oak or birch trunk to the base standing on the ground, at least five logs tall, with a cell beside it facing the base
		BlockPos base = new BlockPos(bx, by + 1, bz);
		trunk = server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos best = null;
			double bestD = 1e9;
			for (BlockPos p : BlockPos.betweenClosed(base.offset(-30, -4, -30), base.offset(30, 6, 30))) {
				BlockState st = level.getBlockState(p);
				if (!st.is(BlockTags.LOGS) || !level.getBlockState(p.below()).is(BlockTags.DIRT)) {
					continue;
				}
				int h = 0;
				while (level.getBlockState(p.above(h)).is(BlockTags.LOGS)) {
					h++;
				}
				if (h < 5 || p.distSqr(base) >= bestD || p.distSqr(base) < 64) {   // at the forest's edge, clear of the base
					continue;
				}
				best = p.immutable();
				bestD = p.distSqr(base);
			}
			return best;
		});
		if (trunk == null) {
			throw new IllegalStateException("no tree near the base");
		}
		int logs = server.computeOnServer(srv -> {
			int h = 0;
			while (srv.overworld().getBlockState(trunk.above(h)).is(BlockTags.LOGS)) {
				h++;
			}
			return h;
		});
		log("trunk at " + trunk.toShortString() + ", " + logs + " logs, " + state(trunk));
		// stand beside it, on the side towards the base
		Direction toward = Direction.getApproximateNearest(bx - trunk.getX(), 0, bz - trunk.getZ());
		BlockPos feet = null;
		for (Direction d : new Direction[] {toward, toward.getClockWise(), toward.getCounterClockWise(), toward.getOpposite()}) {
			BlockPos f = trunk.relative(d);
			if (clear(f) && clear(f.above()) && solid(f.below())) {
				feet = f;
				break;
			}
		}
		if (feet == null) {
			throw new IllegalStateException("no room beside the trunk at " + trunk.toShortString());
		}
		survival();
		// a few steps in, as a player walks up to the tree (from the farthest of up to three cells back that is clear and level)
		Direction back = Direction.getApproximateNearest(feet.getX() - trunk.getX(), 0, feet.getZ() - trunk.getZ());
		BlockPos start = feet;
		for (int k = 1; k <= 3; k++) {
			BlockPos c = feet.relative(back, k);
			if (!clear(c) || !clear(c.above()) || !solid(c.below())) {
				break;
			}
			start = c;
		}
		standAt(start, Vec3.atCenterOf(trunk.above(2)));
		context.waitTicks(wanted(n) ? 100 : 5);
		startFilm(n, "tree");
		Vec3 from = Vec3.atBottomCenterOf(start), to = Vec3.atBottomCenterOf(feet);
		int steps = (int) Math.max(1, from.distanceTo(to) * 9);
		context.waitTicks(20);
		for (int i = 1; i <= steps; i++) {
			Vec3 p = from.lerp(to, (double) i / steps);
			context.runOnClient(c -> c.player.setPos(p.x, p.y, p.z));
			context.waitTick();
		}
		mark("at the tree");
		int got = 0;
		for (int k = 0; k < Math.min(logs, 5); k++) {
			BlockPos log = trunk.above(k);
			if (dig(log, 140)) {
				got++;
				mark("log " + got);
			}
			if (got == 4) {
				context.waitTicks(10);
			}
		}
		context.waitTicks(20);
		mark("logs in hand " + count(Items.OAK_LOG) + " oak " + count(Items.BIRCH_LOG) + " birch");
		waitQuest("wood");
		context.waitTicks(40);
		stopFilm();
		// the quest book: Punch a tree ticked
		startFilm(n, "tree_book");
		book("wood", 90);
		stopFilm();
	}

	private Item logItem() {
		return count(Items.OAK_LOG) >= count(Items.BIRCH_LOG) ? Items.OAK_LOG : Items.BIRCH_LOG;
	}

	private Item plankItem() {
		return logItem() == Items.OAK_LOG ? Items.OAK_PLANKS : Items.BIRCH_PLANKS;
	}

	// ---- 3: planks and the crafting table -----------------------------------------------------------------------------------------------

	private void planks(int n) {
		Item log = logItem();
		int have = count(log);
		if (have < 6) {
			give(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(log).toString(), 6 - have, "another log or two from the same trees");
		}
		// stand at the base, looking at its ground
		standAt(new BlockPos(bx, by + 1, bz + 3), Vec3.atCenterOf(table.below()));
		context.waitTicks(wanted(n) ? 60 : 3);
		startFilm(n, "planks");
		openInventory();
		mark("inventory open");
		// three logs into planks: the inventory's own 2x2 grid (slots 1-4, player 9-44, hotbar 36-44)
		int src = slotOf(log, 9);
		left(src);
		right(1);
		right(1);
		right(1);
		left(src);
		context.waitTicks(6);
		mark("logs in the grid");
		shift(0);
		mark("planks made: " + count(plankItem()));
		context.waitTicks(10);
		// the crafting table: four planks, one in each cell
		craft(2, new String[] {"pp", "pp"}, Map.of('p', plankItem()), 1, 9, 36);
		mark("crafting table made");
		context.waitTicks(16);
		closeScreen();
		hold(Items.CRAFTING_TABLE);
		useOn(table.below(), Direction.UP);
		mark("table placed");
		context.waitTicks(30);
		stopFilm();
		log("table: " + state(table));
	}

	// ---- 4: sticks and the wooden pickaxe ------------------------------------------------------------------------------------------------

	private void pickaxe(int n) {
		startFilm(n, "pickaxe");
		openBlock(table);
		mark("table open");
		// crafting menu: result 0, grid 1-9, player 10-45, hotbar 37-45
		craft(3, new String[] {" p ", " p "}, Map.of('p', plankItem()), 2, 10, 37);
		mark("sticks made: " + count(Items.STICK));
		LinkedHashMap<Character, Item> k = new LinkedHashMap<>();
		k.put('p', plankItem());
		k.put('s', Items.STICK);
		craft(3, new String[] {"ppp", " s ", " s "}, k, 1, 10, 37);
		mark("wooden pickaxe made");
		context.waitTicks(20);
		closeScreen();
		context.waitTicks(10);
		stopFilm();
	}

	// ---- 5: stone, the stone pickaxe ----------------------------------------------------------------------------------------------------

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
		standAt(gullyFeet, Vec3.atCenterOf(stones.get(0)));
		server.runCommand("time set 2500");
		context.waitTicks(wanted(n) ? 100 : 5);
		startFilm(n, "stone");
		int got = 0;
		for (BlockPos s : stones) {
			if (got >= 3) {
				break;
			}
			if (dig(s, 80)) {
				got++;
				mark("stone " + got);
			}
		}
		context.waitTicks(30);
		mark("cobblestone " + count(Items.COBBLESTONE));
		stopFilm();
		// the stone pickaxe at the table, and enough cobblestone for a furnace and smooth stone, mined off camera
		int cob = count(Items.COBBLESTONE);
		give("minecraft:cobblestone", Math.max(0, 3 + 8 + 3 - cob + 3), "more stone from the gully");
		standAt(new BlockPos(bx, by + 1, bz + 2), Vec3.atCenterOf(table));
		context.waitTicks(wanted(n) ? 60 : 3);
		startFilm(n, "stone_pick");
		openBlock(table);
		LinkedHashMap<Character, Item> k = new LinkedHashMap<>();
		k.put('c', Items.COBBLESTONE);
		k.put('s', Items.STICK);
		craft(3, new String[] {"ccc", " s ", " s "}, k, 1, 10, 37);
		mark("stone pickaxe made");
		context.waitTicks(10);
		closeScreen();
		waitQuest("stone");
		context.waitTicks(30);
		stopFilm();
		startFilm(n, "stone_book");
		book("stone", 80);
		stopFilm();
		claim("stone");   // 16 torches
	}

	// ---- 6: coal ----------------------------------------------------------------------------------------------------------------------------

	private void coal(int n) {
		hold(Items.STONE_PICKAXE);
		standAt(gullyFeet, Vec3.atCenterOf(coalOre));
		context.waitTicks(wanted(n) ? 80 : 5);
		startFilm(n, "coal");
		dig(coalOre, 80);
		context.waitTicks(30);
		mark("coal " + count(Items.COAL));
		stopFilm();
		if (count(Items.COAL) < 1) {
			give("minecraft:coal", 1, "the coal ore's drop missed");
		}
	}

	// ---- 7: the campfire -------------------------------------------------------------------------------------------------------------------

	private void campfire(int n) {
		standAt(new BlockPos(bx, by + 1, bz + 2), Vec3.atCenterOf(table));
		context.waitTicks(wanted(n) ? 60 : 3);
		startFilm(n, "campfire");
		openBlock(table);
		LinkedHashMap<Character, Item> k = new LinkedHashMap<>();
		k.put('s', Items.STICK);
		k.put('c', Items.COAL);
		k.put('l', logItem());
		craft(3, new String[] {" s ", "scs", "lll"}, k, 1, 10, 37);
		mark("campfire made");
		context.waitTicks(10);
		closeScreen();
		waitQuest("campfire");
		hold(Items.CAMPFIRE);
		face(Vec3.atCenterOf(campfire), 12);
		useOn(campfire.below(), Direction.UP);
		mark("campfire placed: " + state(campfire));
		context.waitTicks(60);
		stopFilm();
		startFilm(n, "campfire_book");
		book("campfire", 60);
		claim("campfire");   // 8 coal
		book("campfire", 50);
		stopFilm();
		log("coal after the reward: " + count(Items.COAL));
	}

	// ---- 8: the furnace ---------------------------------------------------------------------------------------------------------------------

	private void furnace(int n) {
		standAt(new BlockPos(bx + 1, by + 1, bz + 2), Vec3.atCenterOf(table));
		context.waitTicks(wanted(n) ? 60 : 3);
		startFilm(n, "furnace");
		openBlock(table);
		craft(3, new String[] {"ccc", "c c", "ccc"}, Map.of('c', Items.COBBLESTONE), 1, 10, 37);
		mark("furnace made");
		context.waitTicks(10);
		closeScreen();
		hold(Items.FURNACE);
		useOn(furnace.below(), Direction.UP);
		mark("furnace placed: " + state(furnace));
		context.waitTicks(40);
		stopFilm();
	}

	// ---- 9: iron (and copper) ore, below the gully ------------------------------------------------------------------------------------------

	/**
	 * The nearest iron or copper ore to the gully's floor; a passage one wide and two high is dug to it off camera (from the gully, in steps
	 * down), lit with the stone quest's torches, and the player stands at its end facing the ore.
	 */
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
			if (i % 4 == 2) {
				BlockPos torch = at.above();
				server.computeOnServer(srv -> srv.overworld().setBlock(torch, Blocks.TORCH.defaultBlockState(), 3));
			}
		}
		// a step back from the face on the same level, so the ore shows with the rock round it, and a torch in a niche either side
		Direction dir = Direction.getApproximateNearest(target.getX() - at.getX(), 0, target.getZ() - at.getZ());
		BlockPos back = at.relative(dir.getOpposite());
		BlockPos[] cut = {back, back.above(), at.relative(dir.getClockWise()), back.relative(dir.getCounterClockWise())};
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
		standAt(back, Vec3.atCenterOf(target));
		return target;
	}

	private void iron(int n) {
		hold(Items.STONE_PICKAXE);
		server.runCommand("time set 4000");
		BlockPos ore = oreFace(Blocks.IRON_ORE);
		context.waitTicks(wanted(n) ? 120 : 5);
		startFilm(n, "iron");
		dig(ore, 80);
		context.waitTicks(30);
		mark("raw iron " + count(Items.RAW_IRON));
		stopFilm();
		give("minecraft:raw_iron", 13 - count(Items.RAW_IRON), "the rest of the iron ore, mined off camera");
	}

	// ---- 10: smelting -------------------------------------------------------------------------------------------------------------------------

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
		for (int i = 0; i < 20 * 12 * items + 100 && furnaceOut(at) < items; i += 20) {
			if (i % 200 == 0) {
				mark("out " + furnaceOut(at));
			}
			context.waitTicks(20);
		}
		mark("out " + furnaceOut(at));
	}

	private void smelt(int n) {
		standAt(new BlockPos(bx + 2, by + 1, bz + 2), Vec3.atCenterOf(furnace));
		server.runCommand("time set 5000");
		context.waitTicks(wanted(n) ? 60 : 3);
		startFilm(n, "smelt");
		openBlock(furnace);
		load(Items.RAW_IRON, 13, 2);
		mark("loaded 13 raw iron, 2 coal");
		cook(furnace, 13, n, wanted(n));
		shift(2);
		mark("iron ingots " + count(Items.IRON_INGOT));
		context.waitTicks(20);
		closeScreen();
		waitQuest("iron");
		context.waitTicks(20);
		stopFilm();
		startFilm(n, "smelt_book");
		book("iron", 70);
		stopFilm();
		claim("iron");
	}

	// ---- 11: copper ---------------------------------------------------------------------------------------------------------------------------

	private void copper(int n) {
		hold(Items.STONE_PICKAXE);
		BlockPos ore = oreFace(Blocks.COPPER_ORE);
		context.waitTicks(wanted(n) || shots != null && shots.contains("copper") ? 120 : 5);
		startFilm(n, "copper");
		dig(ore, 80);
		context.waitTicks(30);
		mark("raw copper " + count(Items.RAW_COPPER));
		stopFilm();
		give("minecraft:raw_copper", Math.max(0, 17 - count(Items.RAW_COPPER)), "the rest of the copper ore, mined off camera");
		// copper and then the smooth stone in the furnace
		standAt(new BlockPos(bx + 2, by + 1, bz + 2), Vec3.atCenterOf(furnace));
		server.runCommand("time set 6500");
		context.waitTicks(wanted(n) ? 60 : 3);
		startFilm(n, "copper_smelt");
		openBlock(furnace);
		load(Items.RAW_COPPER, 17, 3);
		mark("loaded 17 raw copper, 3 coal");
		cook(furnace, 17, n, wanted(n));
		shift(2);
		mark("copper ingots " + count(Items.COPPER_INGOT));
		context.waitTicks(10);
		closeScreen();
		waitQuest("copper");
		stopFilm();
		// smooth stone: cobblestone to stone to smooth stone, three of each (the furnace's own recipes); filmed but quickly cut
		startFilm(n, "smooth");
		openBlock(furnace);
		load(Items.COBBLESTONE, 3, lit(furnace) ? 0 : 1);
		cook(furnace, 3, n, wanted(n));
		shift(2);
		load(Items.STONE, 3, lit(furnace) ? 0 : 1);
		cook(furnace, 3, n, wanted(n));
		shift(2);
		mark("smooth stone " + count(Items.SMOOTH_STONE));
		closeScreen();
		stopFilm();
		startFilm(n, "copper_book");
		book("copper", 70);
		stopFilm();
	}

	// ---- 12: the blast furnace ----------------------------------------------------------------------------------------------------------------

	private void blastFurnace(int n) {
		standAt(new BlockPos(bx + 2, by + 1, bz + 2), Vec3.atCenterOf(table));   // from here the blast furnace goes down facing south, as the furnace
		server.runCommand("time set 8000");
		context.waitTicks(wanted(n) ? 60 : 3);
		startFilm(n, "blast");
		openBlock(table);
		LinkedHashMap<Character, Item> k = new LinkedHashMap<>();
		k.put('i', Items.IRON_INGOT);
		k.put('f', Items.FURNACE);
		k.put('s', Items.SMOOTH_STONE);
		if (count(Items.FURNACE) < 1) {
			// the recipe takes a furnace: a second one from cobblestone first
			give("minecraft:cobblestone", Math.max(0, 8 - count(Items.COBBLESTONE)), "stone for a second furnace");
			craft(3, new String[] {"ccc", "c c", "ccc"}, Map.of('c', Items.COBBLESTONE), 1, 10, 37);
			mark("second furnace made");
		}
		craft(3, new String[] {"iii", "ifi", "sss"}, k, 1, 10, 37);
		mark("blast furnace made");
		context.waitTicks(10);
		closeScreen();
		hold(Items.BLAST_FURNACE);
		useOn(blast.below(), Direction.UP);
		mark("blast furnace placed: " + state(blast));
		context.waitTicks(40);
		stopFilm();
	}

	// ---- 13: what goes wrong: iron in the plain furnace -----------------------------------------------------------------------------------------

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
		openBlock(furnace);
		load(Items.IRON_INGOT, 8, 1);
		mark("8 iron ingots and a coal in the furnace");
		context.waitTicks(160);
		mark("nothing: lit " + lit(furnace) + ", out " + furnaceOut(furnace));
		// take them back out
		shift(0);
		shift(1);
		mark("taken back out");
		context.waitTicks(10);
		closeScreen();
		face(Vec3.atCenterOf(furnace), 6);
		context.waitTicks(30);
		mark("the furnace, cold: lit " + lit(furnace));
		context.waitTicks(30);
		stopFilm();
	}

	// ---- 14: steel ------------------------------------------------------------------------------------------------------------------------------

	private void steel(int n) {
		standAt(new BlockPos(bx + 3, by + 1, bz + 2), Vec3.atCenterOf(blast));
		server.runCommand("time set 9000");
		context.waitTicks(wanted(n) ? 60 : 3);
		startFilm(n, "steel");
		openBlock(blast);
		load(Items.IRON_INGOT, 8, 1);
		mark("8 iron ingots and a coal in the blast furnace");
		cook(blast, 8, n, wanted(n));
		shift(2);
		mark("steel " + count(WildcatterItems.STEEL_INGOT));
		context.waitTicks(10);
		closeScreen();
		waitQuest("steel");
		face(Vec3.atCenterOf(blast), 6);
		context.waitTicks(30);
		stopFilm();
		startFilm(n, "steel_book");
		book("steel", 70);
		stopFilm();
	}

	// ---- 15: the Field Manual -----------------------------------------------------------------------------------------------------------------

	private void manual(int n) {
		give("minecraft:paper", 3, "paper from sugar cane by the river");
		give("minecraft:leather", 1, "leather from a cow");
		standAt(new BlockPos(bx, by + 1, bz + 2), Vec3.atCenterOf(table));
		context.waitTicks(wanted(n) ? 60 : 3);
		startFilm(n, "manual");
		openBlock(table);
		LinkedHashMap<Character, Item> k = new LinkedHashMap<>();
		k.put('p', Items.PAPER);
		k.put('l', Items.LEATHER);
		craft(3, new String[] {"pp ", "pl "}, k, 1, 10, 37);
		mark("book made");
		LinkedHashMap<Character, Item> m = new LinkedHashMap<>();
		m.put('b', Items.BOOK);
		m.put('c', Items.COPPER_INGOT);
		craft(3, new String[] {"bc"}, m, 1, 10, 37);
		mark("field manual made");
		context.waitTicks(10);
		closeScreen();
		waitQuest("field_manual");
		stopFilm();
		startFilm(n, "manual_read");
		hold(WildcatterItems.FIELD_MANUAL);
		useHeld();
		context.waitTicks(10);
		mark("manual open: " + context.computeOnClient(c -> String.valueOf(c.gui.screen())));
		context.waitTicks(80);
		for (int page : new int[] {1, 9, 10}) {
			context.runOnClient(c -> {
				if (c.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.BookViewScreen b) {
					b.setPage(page);
				}
			});
			mark("manual page " + page);
			context.waitTicks(100);
		}
		context.setScreen(() -> null);
		context.waitTicks(10);
		stopFilm();
	}

	// ---- 16: the chapter complete ---------------------------------------------------------------------------------------------------------------

	private void done(int n) {
		startFilm(n, "chapter");
		book("field_manual", 60);
		hold(WildcatterItems.QUEST_BOOK);
		useHeld();
		context.waitTicks(6);
		context.runOnClient(c -> {
			if (c.gui.screen() instanceof QuestScreen q) {
				q.open(Identifier.parse("wildcatter:steam"), true);
			}
		});
		mark("next: steam");
		context.waitTicks(90);
		context.setScreen(() -> null);
		stopFilm();
	}

	// ---- 17: the hook: steel in hand at the finished base ---------------------------------------------------------------------------------------

	private void hook(int n) {
		server.runCommand("time set 11200");
		// steel in hand, looking over the campfire and the furnaces
		hold(WildcatterItems.STEEL_INGOT);
		standAt(new BlockPos(bx + 1, by + 1, bz + 4), Vec3.atCenterOf(blast).add(-1.5, -0.3, 0));
		context.waitTicks(wanted(n) ? 100 : 3);
		startFilm(n, "hook_hand");
		face(Vec3.atCenterOf(campfire).add(0, 0.2, 0), 60);
		face(Vec3.atCenterOf(furnace).add(0, 0.2, 0), 60);
		context.waitTicks(40);
		stopFilm();
		spectator();
		drone(n, "hook_orbit", cam(8, 4.5, 8, 1.5, 0.6, 2.5), cam(-5, 4, 9, 1.5, 0.6, 2.5), 240);
		drone(n, "top", cam(1.5, 10, 6.5, 1.5, 0, 2.6), cam(1.5, 8.5, 5.0, 1.5, 0, 2.6), 160);
		survival();
		log("final inventory: " + inventory());
	}
}
