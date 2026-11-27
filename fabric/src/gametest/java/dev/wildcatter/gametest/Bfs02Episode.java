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
 * Built From Scratch, episode 2 "First light": the First power chapter played through in survival, in the series' world. The world is
 * new (seed {@link Bfs01Films#SEED}); episode 1's base is rebuilt first at the same block positions (PLAN.md "Episode 1's base": the table,
 * the two furnaces and the lit campfire; the two punched oaks; the stone and coal taken from the gully; the passages dug to the iron and
 * copper, lit, the ore mined; the grass cut), the Getting started chapter done with the same rewards claimed and the same pocket. Then
 * everything the player does goes through the client, as in episode 1 (Bfs01Episode): the destroy loop, useItemOn, container clicks with
 * a drawn cursor. What is gathered off camera is given by command and logged, so the edit can label it.
 * <p>
 * The plant goes against the campfire at -567, 66, 945: the boiler on it; a stone pad east of it (-566 and -565, 945; -566, 946) with the
 * turbine (facing east) and later the breaker and a ground rod on it; the hot wire from the turbine's east face across the pad, down and on
 * to a lamp at -562, 66, 945; the neutral from the turbine's north face down and along z 944 to the lamp. The receptacle's branch leaves the
 * hot wire's high cell through a fuse over the neutral to a receptacle at -563, 66, 943, a plug adapter against it.
 * <p>
 * Film only: the receptacle's and plug adapter's names are shown without the standard's name (a film-only label, as the series names no
 * standard): "Receptacle (" + the low tier's label + ", 15 A)". Films land in {@code build/scene-videos/bfs02__NN_<shot>.mov}; BFS_SHOTS
 * picks shots by number or name (the rest run unfilmed).
 */
final class Bfs02Episode {
	private final Bfs02Films t;
	private final ClientGameTestContext context;
	private final TestServerContext server;
	private final Set<String> shots;
	private int bx, by, bz;
	private BlockPos table, furnace, blast, campfire;
	private boolean filming;
	private String filmId = "";
	private long filmStart;
	/** The plant (see the class note). */
	private BlockPos boiler, turbine, padE, padS, rod1, lamp, rod2, recept, plug, fuseAt;
	private BlockPos[] hot, neutral, branch;

	Bfs02Episode(Bfs02Films t) {
		this.t = t;
		this.context = t.context;
		this.server = t.server;
		String pick = System.getenv("BFS_SHOTS");
		shots = pick == null || pick.isBlank() ? null : Set.of(pick.split(","));
	}

	static void log(String s) {
		System.out.println("[bfs02] " + s);
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
		int y = by + 1;
		boiler = campfire.above();
		turbine = new BlockPos(-566, y + 1, 945);
		padE = new BlockPos(-565, y, 945);
		padS = new BlockPos(-566, y, 946);
		rod1 = padS.above();
		lamp = new BlockPos(-562, y, 945);
		rod2 = new BlockPos(-562, y, 946);
		hot = new BlockPos[] {new BlockPos(-565, y + 1, 945), new BlockPos(-564, y, 945), new BlockPos(-564, y + 1, 945), new BlockPos(-563, y, 945)};
		neutral = new BlockPos[] {new BlockPos(-566, y, 944), new BlockPos(-566, y + 1, 944), new BlockPos(-565, y, 944), new BlockPos(-564, y, 944),
				new BlockPos(-563, y, 944), new BlockPos(-562, y, 944)};
		branch = new BlockPos[] {new BlockPos(-564, y, 943), new BlockPos(-564, y + 1, 943)};
		fuseAt = new BlockPos(-564, y + 1, 944);
		recept = new BlockPos(-563, y, 943);
		plug = new BlockPos(-562, y, 943);
		log("base at " + bx + " " + by + " " + bz + "; ground " + state(new BlockPos(bx, by, bz)) + "; low tier " + Voltage.LOW.label());
		rebuildDayOne();
		String[] steps = {"base", "boiler", "turbine", "wires", "hot", "neutral", "hook", "protection", "firebox", "plugs", "done"};
		for (int i = 0; i < steps.length; i++) {
			int n = i + 1;
			log("step " + n + " " + steps[i]);
			try {
				switch (steps[i]) {
					case "base" -> base(n);
					case "boiler" -> boiler(n);
					case "turbine" -> turbine(n);
					case "wires" -> wires(n);
					case "hot" -> hotRun(n);
					case "neutral" -> neutralRun(n);
					case "hook" -> hook(n);
					case "protection" -> protection(n);
					case "firebox" -> firebox(n);
					case "plugs" -> plugs(n);
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

	/** Film only: the receptacle and plug adapter named without the standard's name, their tier's label read from the standard. */
	private void labels() {
		double amps = dev.wildcatter.energy.PlugType.NEMA_5_15.amps();
		String tag = " (" + Voltage.LOW.label() + ", " + Math.round(amps) + " A)";
		Map<String, String> over = Map.of("block.wildcatter.receptacle_nema_5_15", "Receptacle" + tag, "block.wildcatter.plug_nema_5_15", "Plug Adapter" + tag);
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

	// ---- filming ------------------------------------------------------------------------------------------------------------------------

	private Filming film;

	private void startFilm(int n, String name) {
		if (!wanted(n) && !(shots != null && shots.contains(name))) {
			return;
		}
		filmId = String.format(Locale.ROOT, "bfs02__%02d_%s", n, name);
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
	private Bfs02Films.Cam cam(double x, double y, double z, double tx, double ty, double tz) {
		return new Bfs02Films.Cam(x, y, z, tx, ty, tz);
	}

	/** A spectator drone move between two cameras, HUD hidden. */
	private void drone(int n, String name, Bfs02Films.Cam a, Bfs02Films.Cam b, int ticks) {
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

	// ---- 1: where day one left off ------------------------------------------------------------------------------------------------------

	private void base(int n) {
		spectator();
		server.runCommand("time set 1000");
		drone(n, "base", cam(6, 4.5, 12, 1.5, 0.6, 3.0), cam(-5, 4.5, 10, 1.5, 0.6, 3.0), 300);   // from the open south, clear of the forest to the east
		survival();
		// the rewards day one left in the book: copper's eight copper, steel's eight iron (and the apples and the paper)
		standAt(new BlockPos(bx + 1, by + 1, bz + 3), Vec3.atCenterOf(furnace));
		context.waitTicks(wanted(n, "claims") ? 40 : 3);
		startFilm(n, "claims");
		claimIn("copper");
		claimIn("steel");
		claim("wood");
		claim("field_manual");
		stopFilm();
		log("after the claims: " + inventory());
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

	// ---- 2: the boiler ------------------------------------------------------------------------------------------------------------------

	private void boiler(int n) {
		give("minecraft:water_bucket", 1, "a bucket filled at the pond");
		atTable(n, "boiler_craft", 2000);
		tableCraft(3, new String[] {"iii", "iwi", "iii"}, keys('i', Items.IRON_INGOT, 'w', Items.WATER_BUCKET), 1, "boiler");
		context.waitTicks(10);
		closeScreen();
		stopFilm();
		give("minecraft:water_bucket", 4, "four buckets filled at the pond");
		// on the campfire, from the south-west: the campfire, then the boiler on it, four buckets in
		standAt(new BlockPos(bx - 1, by + 1, bz + 8), Vec3.atCenterOf(campfire));
		server.runCommand("time set 2500");
		context.waitTicks(wanted(n, "boiler_place") ? 60 : 3);
		startFilm(n, "boiler_place");
		context.waitTicks(10);
		put(item("wildcatter:boiler"), campfire, Direction.UP);
		mark("boiler placed: " + state(boiler));
		context.waitTicks(20);
		for (int i = 0; i < 4; i++) {
			hold(Items.WATER_BUCKET);
			useOn(boiler, faceToward(boiler));
			mark("bucket " + (i + 1) + ": " + reading(boiler));
			context.waitTicks(14);
		}
		context.waitTicks(20);
		handRead(boiler);
		// steam builds; with nothing drawing it the safety valve lifts
		for (int i = 0; i < 16 && !lifting(); i++) {
			context.waitTicks(20);
		}
		mark("safety valve lifting " + lifting() + ": " + reading(boiler));
		context.waitTicks(80);
		stopFilm();
	}

	private boolean lifting() {
		return server.computeOnServer(srv -> dev.wildcatter.multiblock.BigParts.entity(srv.overworld(), boiler) instanceof dev.wildcatter.block.BoilerBlockEntity b && b.lifting());
	}

	// ---- 3: the turbine --------------------------------------------------------------------------------------------------------------------

	private void turbine(int n) {
		give("minecraft:iron_ingot", 20, "iron from the passage, mined and smelted");
		give("minecraft:copper_ingot", 8, "copper from the passage, mined and smelted");
		give("minecraft:redstone", 5, "redstone from deeper in the passage");
		atTable(n, "turbine_craft", 3500);
		tableCraft(3, new String[] {"ici", "crc", "ici"}, keys('i', Items.IRON_INGOT, 'c', Items.COPPER_INGOT, 'r', Items.REDSTONE), 1, "steam turbine");
		context.waitTicks(10);
		closeScreen();
		stopFilm();
		give("minecraft:smooth_stone", 3, "smooth stone, smelted from the gully's cobblestone");
		// the pad and the turbine on it, from the east, looking west (so it faces east, its exhaust to the south)
		standAt(new BlockPos(-562, by + 1, 946), Vec3.atCenterOf(turbine));
		server.runCommand("time set 4000");
		context.waitTicks(wanted(n, "turbine_place") ? 60 : 3);
		startFilm(n, "turbine_place");
		context.waitTicks(10);
		for (BlockPos p : new BlockPos[] {turbine.below(), padE, padS}) {
			put(Items.SMOOTH_STONE, p.below(), Direction.UP);
		}
		mark("pad laid");
		face(Vec3.atCenterOf(turbine), 8);
		put(item("wildcatter:steam_turbine"), turbine.below(), Direction.UP);
		mark("turbine placed: " + state(turbine));
		context.waitTicks(30);
		handRead(turbine);
		mark("steam quest done: " + questDone("steam"));
		context.waitTicks(20);
		stopFilm();
	}

	// ---- 4: wire, the lamp, the meter --------------------------------------------------------------------------------------------------------

	private void wires(int n) {
		give("minecraft:red_dye", 2, "red dye from poppies");
		give("minecraft:black_dye", 2, "black dye from ink sacs");
		give("minecraft:white_wool", 4, "wool from sheep");
		give("minecraft:glass_pane", 3, "glass from the river's sand");
		Item bareHot = item("wildcatter:wire_hot"), bareNeutral = item("wildcatter:wire_neutral");
		atTable(n, "wire_craft", 5500);
		// bare wire: three copper and a dye make six; twice over
		tableCraft(3, new String[] {"ccc", "d  "}, keys('c', Items.COPPER_INGOT, 'd', item("minecraft:red_dye")), 2, "bare red wire x2");
		// wool wire: four bare wires and a wool make four; twice over
		tableCraft(3, new String[] {"bb ", "bb ", "w  "}, keys('b', bareHot, 'w', item("minecraft:white_wool")), 2, "wool red wire x2");
		tableCraft(3, new String[] {"ccc", "d  "}, keys('c', Items.COPPER_INGOT, 'd', item("minecraft:black_dye")), 2, "bare black wire x2");
		tableCraft(3, new String[] {"bb ", "bb ", "w  "}, keys('b', bareNeutral, 'w', item("minecraft:white_wool")), 2, "wool black wire x2");
		context.waitTicks(10);
		closeScreen();
		stopFilm();
		atTable(n, "lamp_craft", 6000);
		tableCraft(3, new String[] {" g ", "grg", " c "}, keys('g', Items.GLASS_PANE, 'r', Items.REDSTONE, 'c', Items.COPPER_INGOT), 1, "lamp");
		tableCraft(3, new String[] {" c ", "iri", " i "}, keys('c', Items.COPPER_INGOT, 'i', Items.IRON_INGOT, 'r', Items.REDSTONE), 1, "power meter");
		context.waitTicks(10);
		closeScreen();
		stopFilm();
		log("wires: hot " + count(item("wildcatter:wool_wire_hot")) + ", neutral " + count(item("wildcatter:wool_wire_neutral")) + "; " + inventory());
	}

	// ---- 5: the hot wire and the lamp, and what goes wrong -------------------------------------------------------------------------------

	private void hotRun(int n) {
		Item w = item("wildcatter:wool_wire_hot");
		standAt(new BlockPos(-563, by + 1, 947), Vec3.atCenterOf(hot[0]));
		server.runCommand("time set 8000");
		context.waitTicks(wanted(n, "hot") ? 60 : 3);
		startFilm(n, "hot");
		context.waitTicks(10);
		put(w, padE, Direction.UP);                  // out of the turbine's east face, on the pad
		put(w, hot[1].below(), Direction.UP);        // on the ground below the next cell
		put(w, hot[1], Direction.UP);                // the drop: up from it to meet the first
		put(w, hot[3].below(), Direction.UP);        // and on towards the lamp
		mark("hot wire laid: " + state(hot[0]) + " / " + state(hot[2]));
		face(Vec3.atCenterOf(lamp), 8);
		put(item("wildcatter:electric_lamp"), lamp.below(), Direction.UP);
		mark("lamp placed: " + state(lamp));
		context.waitTicks(40);
		mark("lamp lit " + lampLit());
		stopFilm();
		// what goes wrong: one wire is a dead end
		standAt(new BlockPos(-561, by + 1, 947), Vec3.atCenterOf(lamp));
		context.waitTicks(wanted(n, "mistake") ? 40 : 3);
		startFilm(n, "mistake");
		context.waitTicks(20);
		meter(lamp);
		face(Vec3.atCenterOf(turbine), 14);
		meter(turbine);
		mark("lamp lit " + lampLit() + ", quests steam " + questDone("steam") + " lamp " + questDone("lamp") + ", valve lifting " + lifting());
		context.waitTicks(20);
		stopFilm();
	}

	// ---- 6: the neutral back, the lamp lit -----------------------------------------------------------------------------------------------------

	private void neutralRun(int n) {
		Item w = item("wildcatter:wool_wire_neutral");
		// round the east end to the north side of the run
		standAt(new BlockPos(-563, by + 1, 942), Vec3.atCenterOf(neutral[1]));
		server.runCommand("time set 8500");
		context.waitTicks(wanted(n, "neutral") ? 60 : 3);
		startFilm(n, "neutral");
		context.waitTicks(10);
		put(w, neutral[0].below(), Direction.UP);    // under the turbine's north face
		put(w, neutral[0], Direction.UP);            // up to the turbine
		for (int i = 2; i < neutral.length; i++) {
			put(w, neutral[i].below(), Direction.UP);
			if (lampLit()) {
				mark("lamp lit at neutral " + i);
			}
		}
		mark("neutral laid; lamp lit " + lampLit());
		face(Vec3.atCenterOf(lamp), 10);
		for (int i = 0; i < 40 && !(questDone("steam") && questDone("lamp")); i++) {
			context.waitTicks(5);
		}
		mark("quests steam " + questDone("steam") + " lamp " + questDone("lamp"));
		context.waitTicks(40);
		meter(lamp);
		face(Vec3.atCenterOf(turbine), 14);
		meter(turbine);
		stopFilm();
		startFilm(n, "lamp_book");
		claimIn("steam");
		claimIn("lamp");
		stopFilm();
	}

	// ---- 7: the hook: the lamp lit at dusk beside the campfire ---------------------------------------------------------------------------

	private void hook(int n) {
		server.runCommand("time set 12700");
		spectator();
		// low, just east of the lamp, looking west past it to the boiler on its campfire and the sun going down behind
		drone(n, "hook_lamp", cam(7.9, 1.55, 6.3, 2.0, 1.7, 5.2), cam(7.4, 1.5, 5.6, 2.0, 1.7, 5.0), 240);
		spectator();
		drone(n, "hook_orbit", cam(6.0, 2.6, 9.5, 4.0, 1.0, 5.0), cam(-0.5, 2.8, 10.0, 4.0, 1.0, 5.0), 280);
		survival();
	}

	// ---- 8: protect the circuit ------------------------------------------------------------------------------------------------------------

	private void protection(int n) {
		give("minecraft:glass_pane", 6, "more glass from the river's sand");
		give("minecraft:iron_nugget", 2, "an iron ingot crafted into nuggets");
		give("minecraft:iron_ingot", 1, "an iron ingot for the ground rods' core");   // the rod is copper, iron, copper (master 486c7446)
		atTable(n, "protect_craft", 9000);
		tableCraft(3, new String[] {"ici", "iri", "ici"}, keys('i', Items.IRON_INGOT, 'c', Items.COPPER_INGOT, 'r', Items.REDSTONE), 1, "breaker");
		tableCraft(3, new String[] {"ggg", "ncn", "ggg"}, keys('g', Items.GLASS_PANE, 'n', Items.IRON_NUGGET, 'c', Items.COPPER_INGOT), 1, "fuses");
		tableCraft(3, new String[] {" c ", " i ", " c "}, keys('c', Items.COPPER_INGOT, 'i', Items.IRON_INGOT), 1, "ground rods");
		context.waitTicks(10);
		closeScreen();
		waitQuest("protection");
		context.waitTicks(20);
		stopFilm();
		// the breaker in the hot wire right at the turbine: the first hot cell out, the breaker in its place on the pad
		standAt(new BlockPos(-568, by + 1, 947), Vec3.atCenterOf(hot[0]));   // south-west of it, clear of the boiler, still facing east (34 degrees off)
		server.runCommand("time set 9500");
		context.waitTicks(wanted(n, "breaker") ? 60 : 3);
		startFilm(n, "breaker");
		context.waitTicks(10);
		emptyHand();
		dig(hot[0], 80);
		mark("first hot cell out: lamp lit " + lampLit());
		context.waitTicks(10);
		hold(item("wildcatter:breaker"));
		face(Vec3.atCenterOf(hot[0]).add(0, -0.5, 0), 8);
		useOn(padE, Direction.UP);
		mark("breaker placed: " + state(hot[0]) + "; lamp lit " + lampLit());
		context.waitTicks(40);
		meter(hot[0]);
		stopFilm();
		// a ground rod against the turbine (on its pad), one in the earth beside the lamp
		standAt(new BlockPos(-565, by + 1, 948), Vec3.atCenterOf(rod1));
		context.waitTicks(wanted(n, "rods") ? 40 : 3);
		startFilm(n, "rods");
		context.waitTicks(10);
		put(item("wildcatter:ground_rod"), padS, Direction.UP);
		mark("rod against the turbine: " + state(rod1));
		context.waitTicks(10);
		meter(turbine);
		walk(new BlockPos(-562, by + 1, 948));
		face(Vec3.atCenterOf(rod2), 10);
		put(item("wildcatter:ground_rod"), rod2.below(), Direction.UP);
		mark("rod beside the lamp: " + state(rod2));
		meter(lamp);
		stopFilm();
		spectator();
		drone(n, "pad_view", cam(6.5, 3.0, 9.0, 2.8, 1.9, 5.4), cam(4.6, 2.9, 9.6, 2.8, 1.9, 5.4), 200);
		survival();
		startFilm(n, "protect_book");
		claimIn("protection");
		stopFilm();
	}

	// ---- 9: a firebox --------------------------------------------------------------------------------------------------------------------------

	private void firebox(int n) {
		give("minecraft:bricks", 4, "bricks fired from the river's clay");
		give("minecraft:cobblestone", 8, "cobblestone from the gully");
		atTable(n, "firebox_craft", 10000);
		tableCraft(3, new String[] {"ccc", "c c", "ccc"}, keys('c', Items.COBBLESTONE), 1, "furnace");
		tableCraft(3, new String[] {"ibi", "bfb", "ibi"}, keys('i', Items.IRON_INGOT, 'b', Items.BRICKS, 'f', Items.FURNACE), 1, "firebox");
		context.waitTicks(10);
		closeScreen();
		waitQuest("firebox");
		context.waitTicks(10);
		stopFilm();
		startFilm(n, "firebox_book");
		claimIn("firebox");   // 16 coal
		stopFilm();
		// the swap: the campfire out from under the boiler, the firebox in, the coal in it
		standAt(new BlockPos(-569, by + 1, 947), Vec3.atCenterOf(campfire));
		server.runCommand("time set 10500");
		context.waitTicks(wanted(n, "swap") ? 60 : 3);
		startFilm(n, "swap");
		context.waitTicks(10);
		emptyHand();
		dig(campfire, 160);
		mark("campfire out: " + state(campfire) + "; boiler " + reading(boiler) + "; lamp lit " + lampLit());
		context.waitTicks(10);
		face(Vec3.atCenterOf(campfire), 6);
		put(item("wildcatter:firebox"), campfire.below(), Direction.UP);
		mark("firebox placed: " + state(campfire) + "; lamp lit " + lampLit());
		context.waitTicks(10);
		openBlock(campfire);
		shift(slotOf(Items.COAL, 9));
		mark("coal in: " + reading(campfire));
		context.waitTicks(30);
		closeScreen();
		context.waitTicks(60);
		mark("boiler " + reading(boiler) + "; lamp lit " + lampLit());
		face(Vec3.atCenterOf(turbine), 12);
		meter(turbine);
		stopFilm();
	}

	// ---- 10: receptacle and plug -----------------------------------------------------------------------------------------------------------

	private void plugs(int n) {
		give("minecraft:white_dye", 1, "white dye from bone meal");
		atTable(n, "plugs_craft", 11000);
		tableCraft(3, new String[] {"icd"}, keys('i', Items.IRON_INGOT, 'c', Items.COPPER_INGOT, 'd', item("minecraft:white_dye")), 1, "receptacles");
		tableCraft(3, new String[] {"pri"}, keys('p', item("wildcatter:receptacle_nema_5_15"), 'r', Items.REDSTONE, 'i', Items.IRON_INGOT), 1, "plug adapter");
		context.waitTicks(10);
		closeScreen();
		waitQuest("plugs");
		context.waitTicks(10);
		stopFilm();
		Item w = item("wildcatter:wool_wire_hot");
		// the branch from the north side: a hot wire up and over the neutral, the receptacle on the neutral's corner, the plug against it
		standAt(new BlockPos(-563, by + 1, 941), Vec3.atCenterOf(branch[0]));
		server.runCommand("time set 11300");
		context.waitTicks(wanted(n, "branch") ? 60 : 3);
		startFilm(n, "branch");
		context.waitTicks(10);
		put(w, branch[0].below(), Direction.UP);
		put(w, branch[0], Direction.UP);
		face(Vec3.atCenterOf(recept), 8);
		put(item("wildcatter:receptacle_nema_5_15"), recept.below(), Direction.UP);
		mark("receptacle placed: " + state(recept));
		face(Vec3.atCenterOf(plug), 8);
		put(item("wildcatter:plug_nema_5_15"), plug.below(), Direction.UP);
		mark("plug placed: " + state(plug));
		context.waitTicks(20);
		stopFilm();
		// from the south, the fuse in the gap over the neutral, facing north: in from the hot wire at its back, out to the branch at its front
		standAt(new BlockPos(-564, by + 1, 947), Vec3.atCenterOf(fuseAt));
		context.waitTicks(wanted(n, "fuse") ? 40 : 3);
		startFilm(n, "fuse");
		context.waitTicks(10);
		face(Vec3.atCenterOf(fuseAt), 10);
		put(item("wildcatter:fuse"), fuseAt.below(), Direction.UP);
		mark("fuse placed: " + state(fuseAt) + "; receptacle " + reading(recept));
		context.waitTicks(30);
		face(Vec3.atCenterOf(hot[0]), 10);
		meter(hot[0]);
		stopFilm();
		spectator();
		drone(n, "branch_view", cam(4.3, 3.0, 0.6, 5.0, 1.3, 3.6), cam(6.2, 2.8, 0.9, 5.0, 1.3, 3.6), 200);
		survival();
		startFilm(n, "plugs_book");
		book("plugs", 60);
		stopFilm();
	}

	// ---- 11: the chapter, done; the base at nightfall ------------------------------------------------------------------------------------

	private void done(int n) {
		startFilm(n, "chapter");
		book("plugs", 70);
		hold(WildcatterItems.QUEST_BOOK);
		useHeld();
		context.waitTicks(6);
		context.runOnClient(c -> {
			if (c.gui.screen() instanceof QuestScreen q) {
				q.open(Identifier.parse("wildcatter:seismograph"), true);
			}
		});
		mark("next: seismograph");
		context.waitTicks(90);
		context.setScreen(() -> null);
		stopFilm();
		server.runCommand("time set 11800");
		spectator();
		drone(n, "top", cam(-0.5, 4.0, 10.5, 3.2, 1.0, 4.6), cam(5.5, 3.6, 10.5, 3.2, 1.0, 4.6), 260);
		survival();
		log("plant at the end: lamp " + reading(lamp) + " | turbine " + reading(turbine) + " | breaker " + reading(hot[0]) + " | fuse " + reading(fuseAt)
				+ " | boiler " + reading(boiler) + " | firebox " + reading(campfire));
	}
}
