package dev.wildcatter.gametest;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.well.Reservoir;
import dev.wildcatter.well.ReservoirData;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * {@code trailer} (WILDCATTER_ONLY=trailer; never part of the full suite): the modpack trailer's Wildcatter shots, staged in a real
 * generated world (a fixed seed) at golden hour, each plant built as its game test builds it and left running. Each shot is taken twice
 * from the two ends of a short camera move ({@code NN_<shot>_a.png} and {@code _b.png}, 1920 x 1080, HUD hidden, spectator so no hand)
 * and filmed along that move ({@code build/scene-videos/NN_<shot>.mov}). TRAILER_SHOTS=1,4 runs only those shots; TRAILER_OUT is the
 * folder for the stills (default {@code trailer} in the run folder).
 */
public class TrailerClientGameTest implements FabricClientGameTest {
	static final String SEED = "8675309";
	static final Direction NORTH = Direction.NORTH;

	ClientGameTestContext context;
	TestServerContext server;
	Path out;
	/** The site being built: its origin in x and z, and its ground's top block's y. */
	int ox, gy, oz;
	/** The shots asked for (TRAILER_SHOTS), or null for all. */
	java.util.Set<String> shots;

	@Override
	public void runTest(ClientGameTestContext context) {
		String only = System.getenv("WILDCATTER_ONLY");
		if (only == null || !java.util.Arrays.asList(only.split(",")).contains("trailer")) {
			return;
		}
		this.context = context;
		String dir = System.getenv("TRAILER_OUT");
		out = Path.of(dir == null || dir.isBlank() ? "trailer" : dir).toAbsolutePath();
		try {
			Files.createDirectories(out);
		} catch (java.io.IOException ex) {
			throw new RuntimeException(ex);
		}
		// the stills render off screen at 1920 x 1080 (TestScreenshotOptions), but the films record the window as it shows on screen:
		// a 16:9 window that fits a laptop's screen (1440 x 810 points, 2880 x 1620 pixels on a Retina display), or the recording
		// keeps only the part of the window on the screen, its top left
		context.getInput().resizeWindow(1440, 810);
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
			int[] base = {0, 0};
			String pick = System.getenv("TRAILER_SHOTS");
			shots = pick == null || pick.isBlank() ? null : java.util.Set.of(pick.split(","));
			Runnable[] all = {this::fireToSteam, this::oilfield, this::refinery, this::powerLines, this::gensets, this::reactor};
			// each shot's land: {box x1, x2, z1, z2}, its range, {the plant's box}, how flat that must be
			int[][][] boxes = {
					{{-40, 60, -30, 40}, {14}, {-14, 24, -14, 16}, {3}},
					{{-50, 40, -40, 110}, {22}, {-34, 26, -18, 82}, {7}},
					{{-40, 60, -80, 60}, {22}, {-20, 36, -58, 30}, {7}},
					{{-220, 60, -40, 40}, {34}, {-4, 44, -20, 18}, {5}},
					{{-40, 40, -30, 40}, {14}, {-15, 17, -14, 16}, {4}},
					{{-70, 150, -90, 80}, {24}, {-44, 130, -64, 46}, {9}}};
			for (int i = 0; i < all.length; i++) {
				// 7 (the flare) is shot on the refinery's site, 8 (the pumpjack's crank, close, to check it) on the oilfield's
				if (!wanted(i + 1) && !(i == 2 && wanted(7)) && !(i == 1 && wanted(8))) {
					continue;
				}
				int[][] b = boxes[i];
				site(base[0] + i * 500, base[1], b[0], b[1][0], b[2], b[3][0]);
				try {
					all[i].run();
				} catch (Throwable t) {
					log("shot " + (i + 1) + " FAILED: " + t);
					t.printStackTrace();
				} finally {
					ReservoirData.testSeed = null;
				}
			}
		}
	}

	boolean wanted(int shot) {
		return shots == null || shots.contains(String.valueOf(shot));
	}

	// ---- sites -----------------------------------------------------------------------------------------------------

	final List<int[]> taken = new ArrayList<>();

	/**
	 * Finds dry land near (x, z) from the world's own terrain noise (no chunks generated): every point of the box x1..x2, z1..z2 round it
	 * over the sea, the box no more than {@code range} high to low, its inner box ix1..ix2, iz1..iz2 (where the plant goes) within
	 * {@code flat}; far from the sites already taken. The camera goes there so its chunks load, and the ground's top is the inner box's median.
	 */
	void site(int x, int z, int[] box, int range, int[] inner, int flat) {
		int[] at = server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			var gen = level.getChunkSource().getGenerator();
			var rs = level.getChunkSource().randomState();
			java.util.function.IntBinaryOperator h = (px, pz) -> gen.getBaseHeight(px, pz, Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
			int[] best = {0, 0, 0, Integer.MAX_VALUE};
			for (int r = 0; r <= 30; r++) {
				for (int i = -r; i <= r; i++) {
					for (int j = -r; j <= r; j++) {
						if (Math.max(Math.abs(i), Math.abs(j)) != r) {
							continue;
						}
						int cx = x + i * 48, cz = z + j * 48;
						boolean far = true;
						for (int[] t : taken) {
							far &= Math.hypot(t[0] - cx, t[1] - cz) > 420;
						}
						if (!far) {
							continue;
						}
						int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
						for (int px = box[0]; px <= box[1] && lo > 65; px += 8) {
							for (int pz = box[2]; pz <= box[3]; pz += 8) {
								int v = h.applyAsInt(cx + px, cz + pz);
								lo = Math.min(lo, v);
								hi = Math.max(hi, v);
							}
						}
						if (lo <= 65) {
							continue;
						}
						List<Integer> hs = new ArrayList<>();
						for (int px = inner[0]; px <= inner[1]; px += 4) {
							for (int pz = inner[2]; pz <= inner[3]; pz += 4) {
								hs.add(h.applyAsInt(cx + px, cz + pz));
							}
						}
						java.util.Collections.sort(hs);
						int innerRange = hs.getLast() - hs.getFirst();
						if (hi - lo <= range && innerRange <= flat) {
							return new int[] {cx, cz, hs.get(hs.size() / 2) - 1};
						}
						int score = Math.max(0, hi - lo - range) + 3 * Math.max(0, innerRange - flat);
						if (score < best[3]) {
							best[0] = cx;
							best[1] = cz;
							best[2] = hs.get(hs.size() / 2) - 1;
							best[3] = score;
						}
					}
				}
			}
			return best[3] < Integer.MAX_VALUE ? new int[] {best[0], best[1], best[2]} : new int[] {x, z, 70};
		});
		ox = at[0];
		oz = at[1];
		gy = at[2];
		taken.add(new int[] {ox, oz});
		server.runCommand("tp @p " + ox + " " + (gy + 60) + " " + oz + " 0 60");
		context.waitTicks(160);
		log("site at " + ox + " " + gy + " " + oz);
	}

	/** Flattens a pad round the site (relative x1..x2, z1..z2): air above the ground's top at gy, grass at gy, dirt under; trees near it go. */
	void pad(int x1, int z1, int x2, int z2, String top) {
		fillAbs(ox + x1 - 6, gy - 2, oz + z1 - 6, ox + x2 + 6, gy + 60, oz + z2 + 6, "minecraft:air replace #minecraft:logs");
		fillAbs(ox + x1 - 6, gy - 2, oz + z1 - 6, ox + x2 + 6, gy + 60, oz + z2 + 6, "minecraft:air replace #minecraft:leaves");
		fillAbs(ox + x1, gy + 1, oz + z1, ox + x2, gy + 46, oz + z2, "minecraft:air");
		fillAbs(ox + x1, gy - 4, oz + z1, ox + x2, gy - 1, oz + z2, "minecraft:dirt");
		fillAbs(ox + x1, gy, oz + z1, ox + x2, gy, oz + z2, top);
		// a skirt of slopes, one block a step, so the pad meets the land round it: cut what stands over, fill what falls under
		for (int d = 1; d <= 14; d++) {
			int[][] strips = {{x1 - d, z1 - d, x2 + d, z1 - d}, {x1 - d, z2 + d, x2 + d, z2 + d}, {x1 - d, z1 - d + 1, x1 - d, z2 + d - 1}, {x2 + d, z1 - d + 1, x2 + d, z2 + d - 1}};
			for (int[] st : strips) {
				fillAbs(ox + st[0], gy + d + 1, oz + st[1], ox + st[2], gy + 46, oz + st[3], "minecraft:air");
				fillAbs(ox + st[0], gy - 16, oz + st[1], ox + st[2], gy - d - 1, oz + st[3], "minecraft:dirt replace #minecraft:replaceable");
				fillAbs(ox + st[0], gy - 16, oz + st[1], ox + st[2], gy - d - 1, oz + st[3], "minecraft:dirt replace minecraft:water");
				fillAbs(ox + st[0], gy - d, oz + st[1], ox + st[2], gy - d, oz + st[3], "minecraft:grass_block replace #minecraft:replaceable");
				fillAbs(ox + st[0], gy - d, oz + st[1], ox + st[2], gy - d, oz + st[3], "minecraft:grass_block replace minecraft:water");
			}
		}
		fillAbs(ox + x1, gy - 16, oz + z1, ox + x2, gy - 5, oz + z2, "minecraft:dirt replace #minecraft:replaceable");
		server.runCommand("kill @e[type=item]");
	}

	/** A fill of any size, cut into fills the command allows (32,768 blocks). */
	void fillAbs(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
		int h = y2 - y1 + 1;
		int zs = Math.max(1, Math.min(z2 - z1 + 1, 32768 / h));
		int xs = Math.max(1, 32768 / (h * zs));
		for (int x = x1; x <= x2; x += xs) {
			for (int z = z1; z <= z2; z += zs) {
				server.runCommand("fill " + x + " " + y1 + " " + z + " " + Math.min(x2, x + xs - 1) + " " + y2 + " " + Math.min(z2, z + zs - 1) + " " + block);
			}
		}
	}

	void fill(int x1, int k1, int z1, int x2, int k2, int z2, String block) {
		fillAbs(ox + x1, gy + k1, oz + z1, ox + x2, gy + k2, oz + z2, block);
	}

	/** The site position x, z with k above the ground's top block (k 0 is the ground, 1 the first block over it: the scenes' "up"). */
	BlockPos A(int x, int k, int z) {
		return new BlockPos(ox + x, gy + k, oz + z);
	}

	/** The first air over the ground at site x, z (natural terrain, off the pad). */
	int surface(int x, int z) {
		return server.computeOnServer(srv -> srv.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ox + x, oz + z));
	}

	// ---- the builders' helpers, as WildcatterClientGameTest has them --------------------------------------------------

	void on(Consumer<ServerLevel> work) {
		server.computeOnServer(srv -> {
			work.accept(srv.overworld());
			return null;
		});
	}

	void put(int x, int k, int z, String block) {
		BlockPos p = A(x, k, z);
		server.runCommand("setblock " + p.getX() + " " + p.getY() + " " + p.getZ() + " " + block);
	}

	void placeBig(Block block, BlockPos main, Direction facing) {
		on(level -> {
			BlockState state = block.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, facing);
			level.setBlock(main, state, 3);
			BigParts.grow(level, main, state);
		});
	}

	boolean placeOn(Block block, BlockPos clicked, double px, double pz, float yaw) {
		return placeAt(block, clicked, Direction.UP, px, pz, yaw);
	}

	boolean placeAt(Block block, BlockPos clicked, Direction face, double px, double pz, float yaw) {
		return server.computeOnServer(srv -> {
			net.minecraft.server.level.ServerPlayer player = srv.getPlayerList().getPlayers().get(0);
			player.snapTo(px, clicked.getY() + 1, pz, yaw, 30.0F);
			ItemStack stack = new ItemStack(block);
			net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
					net.minecraft.world.phys.Vec3.atCenterOf(clicked).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5), face, clicked, false);
			return ((net.minecraft.world.item.BlockItem) stack.getItem())
					.place(new net.minecraft.world.item.context.BlockPlaceContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, stack, hit)).consumesAction();
		});
	}

	boolean clickWith(BlockPos at, Direction face, ItemStack stack) {
		return server.computeOnServer(srv -> {
			net.minecraft.server.level.ServerPlayer player = srv.getPlayerList().getPlayers().get(0);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
			net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
					net.minecraft.world.phys.Vec3.atCenterOf(at).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5), face, at, false);
			boolean used = srv.overworld().getBlockState(at).useItemOn(stack, srv.overworld(), player, net.minecraft.world.InteractionHand.MAIN_HAND, hit).consumesAction();
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			return used;
		});
	}

	String read(BlockPos at) {
		return server.computeOnServer(srv -> BigParts.entity(srv.overworld(), at) instanceof Inspectable i ? i.inspect().getString() : "(no readout)");
	}

	void genset(BlockPos panel, Direction facing, boolean gas) {
		List<BlockPos> overwrote = server.computeOnServer(srv -> dev.wildcatter.block.GensetSkid.build(srv.overworld(), panel, facing, gas));
		if (!overwrote.isEmpty()) {
			log("genset at " + panel.toShortString() + " overwrote " + overwrote.size() + " blocks");
		}
	}

	dev.wildcatter.block.GensetControlPanelBlockEntity panel(MinecraftServer srv, BlockPos at) {
		return (dev.wildcatter.block.GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(at);
	}

	void startGenset(BlockPos panel, Direction facing, double liters) {
		context.waitTicks(30);
		server.computeOnServer(srv -> {
			BlockPos tank = dev.wildcatter.block.GensetSkid.base(panel, facing, false);
			if (liters > 0 && srv.overworld().getBlockEntity(tank) instanceof dev.wildcatter.block.GensetPartBlockEntity t) {
				t.setFuel(liters);
			}
			panel(srv, panel).setWantRun(true);
			return null;
		});
		context.waitTicks(240);
		log("genset " + panel.toShortString() + ": " + read(panel));
	}

	BlockPos inlineTransformer(Block block, BlockPos from, Direction d, boolean feedMain) {
		int across = block == WildcatterBlocks.UNIT_SUBSTATION ? 3 : 1;
		BlockPos first = from.relative(d);
		Direction facing = feedMain ? d.getCounterClockWise() : d.getClockWise();
		placeBig(block, feedMain ? first : first.relative(d, across), facing);
		return from.relative(d, across + 2);
	}

	void sizeCables(BlockPos from, BlockPos to, Block kind, dev.wildcatter.energy.ConductorSize size) {
		on(level -> {
			for (BlockPos at : BlockPos.betweenClosed(from, to)) {
				if (level.getBlockEntity(at) instanceof dev.wildcatter.block.MultiCableBlockEntity cable && cable.getBlockState().is(kind)) {
					cable.setSize(size);
				}
			}
		});
	}

	void fillTank(BlockPos tank, dev.wildcatter.fluid.Product product, double litres) {
		on(level -> ((dev.wildcatter.block.StorageTankBlockEntity) level.getBlockEntity(tank)).testFill(product, litres));
	}

	void water(BlockPos... mains) {
		on(level -> {
			for (BlockPos at : mains) {
				if (level.getBlockEntity(at) instanceof dev.wildcatter.block.WaterUser user) {
					user.addWater(100_000);
				}
			}
		});
	}

	/** Steel pipe through cells {x, k over the ground, z}. */
	void pipes(int[][] cells) {
		for (int[] c : cells) {
			put(c[0], c[1], c[2], "wildcatter:gas_line");
		}
	}

	void insulator(Block block, BlockPos at, String conductor) {
		String why = server.computeOnServer(srv -> {
			dev.wildcatter.block.InsulatorBlock insulator = (dev.wildcatter.block.InsulatorBlock) block;
			dev.wildcatter.block.InsulatorBlock.Mount mount = insulator.mountAt(srv.overworld(), at);
			if (mount == null) {
				return "nothing holds an insulator at " + at.toShortString();
			}
			srv.overworld().setBlock(at, insulator.defaultBlockState().setValue(dev.wildcatter.block.InsulatorBlock.MOUNT, mount)
					.setValue(dev.wildcatter.block.CableBlock.CONDUCTOR, dev.wildcatter.circuit.Conductor.valueOf(conductor.toUpperCase())), 3);
			return "";
		});
		if (!why.isEmpty()) {
			log(why);
		}
	}

	void span(BlockPos a, BlockPos b, dev.wildcatter.energy.Voltage tier) {
		var result = server.computeOnServer(srv -> dev.wildcatter.item.SpoolItem.connect(srv.overworld(), a, b, tier, false, 1024));
		if (!result.ok()) {
			log("span " + a.toShortString() + " to " + b.toShortString() + ": " + result);
		}
	}

	static void weighted(dev.wildcatter.block.RigSubstructureBlockEntity rig) {
		rig.testWater(0);
		rig.testWater(45_000);
		rig.testMix(dev.wildcatter.block.RigSubstructureBlockEntity.Solid.BENTONITE, 71.3 * 45);
		while (rig.mudPpg() < 13.9) {
			rig.testMix(dev.wildcatter.block.RigSubstructureBlockEntity.Solid.BARITE, Tuning.BULK_BAG_KG);
		}
	}

	static void log(String text) {
		System.out.println("[wildcatter-trailer] " + text);
	}

	// ---- the camera ------------------------------------------------------------------------------------------------

	/** A camera position (site coordinates, y over the ground's top) looking at a site point. */
	record Cam(double x, double y, double z, double tx, double ty, double tz) {
		float yaw() {
			return (float) Math.toDegrees(Math.atan2(-(tx - x), tz - z));
		}

		float pitch() {
			return (float) -Math.toDegrees(Math.atan2(ty - y, Math.hypot(tx - x, tz - z)));
		}

		Cam lerp(Cam b, double t) {
			double s = t * t * (3 - 2 * t);   // eased
			return new Cam(x + (b.x - x) * s, y + (b.y - y) * s, z + (b.z - z) * s, tx + (b.tx - tx) * s, ty + (b.ty - ty) * s, tz + (b.tz - tz) * s);
		}
	}

	void camera(Cam c, boolean snap) {
		double x = ox + c.x, y = gy + c.y - 1.62, z = oz + c.z;   // the eye at c
		float yaw = c.yaw(), pitch = c.pitch();
		if (snap) {
			server.runCommand(String.format(java.util.Locale.ROOT, "tp @p %.3f %.3f %.3f %.2f %.2f", x, y, z, yaw, pitch));
		}
		context.runOnClient(client -> {
			var p = client.player;
			if (snap) {
				p.snapTo(x, y, z, yaw, pitch);
			} else {
				p.setPos(x, y, z);
				p.setYRot(yaw);
				p.setXRot(pitch);
			}
			p.setYHeadRot(yaw);
			p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		});
	}

	/** Run every second while a shot is taken, to keep what the scene's hooks set (a tower's heat) as it was. */
	Runnable keep = () -> {};

	void hold(int ticks) {
		for (int t = 0; t < ticks; t += 20) {
			keep.run();
			context.waitTicks(Math.min(20, ticks - t));
		}
	}

	/** The shot: the start frame, the end frame, then the move between them filmed over {@code ticks}. */
	void shoot(int n, String name, long time, Cam a, Cam b, int ticks) {
		server.runCommand("time set " + time);
		server.runCommand("weather clear");
		String id = String.format("%02d_%s", n, name);
		camera(a, true);
		hold(120);
		camera(a, true);
		hold(20);
		screenshot(id + "_a");
		camera(b, true);
		hold(40);
		screenshot(id + "_b");
		camera(a, true);
		hold(40);
		if (System.getenv("TRAILER_FRAMES") != null) {
			frames(id, a, b, ticks);
			return;
		}
		Filming film = context.computeOnClient(client -> Filming.start(client, id));
		try {
			context.waitTicks(10);
			for (int t = 0; t <= ticks; t++) {
				camera(a.lerp(b, (double) t / ticks), false);
				if (t % 20 == 0) {
					keep.run();
				}
				context.waitTick();
			}
			context.waitTicks(10);
		} finally {
			film.stop();
			keep = () -> {};
		}
	}

	/**
	 * The move filmed off screen (TRAILER_FRAMES set): a 1920 x 1080 frame each tick into {@code frames/<shot>/}, rendered as the stills
	 * are, so it needs no window on screen (a locked screen or a hidden window records nothing). 20 frames a second; join them with
	 * {@code ffmpeg -framerate 20 -i %04d.png}.
	 */
	void frames(String id, Cam a, Cam b, int ticks) {
		Path dir = out.resolve("frames").resolve(id);
		try {
			Files.createDirectories(dir);
		} catch (java.io.IOException ex) {
			throw new RuntimeException(ex);
		}
		try {
			for (int t = 0; t <= ticks; t++) {
				camera(a.lerp(b, (double) t / ticks), false);
				if (t % 20 == 0) {
					keep.run();
				}
				context.waitTick();
				context.takeScreenshot(TestScreenshotOptions.of(String.format("%04d", t)).withSize(1920, 1080).withDestinationDir(dir).disableCounterPrefix());
			}
		} finally {
			keep = () -> {};
		}
		log("frames " + id + ": " + (ticks + 1));
	}

	void screenshot(String name) {
		context.takeScreenshot(TestScreenshotOptions.of(name).withSize(1920, 1080).withDestinationDir(out).disableCounterPrefix());
		log("shot " + name);
	}

	// ---- 1: fire to steam ------------------------------------------------------------------------------------------

	/**
	 * The starter plant as the dryType scene builds it (QuestClientScenes): steam turbines beside boilers on coal-fired fireboxes, a
	 * dry-type transformer on their low side, its 480 V front against the drilling rig, drilling; a 40 ft pole line marching off east.
	 */
	void fireToSteam() {
		pad(-14, -14, 24, 16, "minecraft:grass_block");
		int n = 3, x0 = 0, z0 = 0, xt = x0 + n, hx = xt + 6;
		on(level -> {
			BlockState stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
			for (int x = xt; x <= hx + 1; x++) {
				for (int z = z0 - 1; z <= z0 + 1; z++) {
					level.setBlock(A(x, 1, z), stone, 3);
				}
			}
			for (int i = 0; i < n; i++) {
				level.setBlock(A(x0 + i, 1, z0), stone, 3);
			}
			BlockState rig = WildcatterBlocks.DRILLING_RIG.defaultBlockState()
					.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
			level.setBlock(A(hx, 2, z0), rig, 3);
			BigParts.grow(level, A(hx, 2, z0), rig);
		});
		for (int i = 0; i < n; i++) {
			BlockPos fb = A(x0 + i, 1, z0 - 1);
			put(x0 + i, 1, z0 - 1, "wildcatter:firebox");
			server.runCommand("item replace block " + fb.getX() + " " + fb.getY() + " " + fb.getZ() + " container.0 with minecraft:coal_block 64");
			put(x0 + i, 2, z0 - 1, "wildcatter:boiler");
			put(x0 + i, 2, z0, "wildcatter:steam_turbine[facing=east]");   // its exhaust pipe out the open south side
		}
		on(level -> {
			for (int i = 0; i < n; i++) {
				((dev.wildcatter.block.BoilerBlockEntity) BigParts.entity(level, A(x0 + i, 2, z0 - 1))).addWater(4000);
			}
		});
		context.waitTicks(220);
		on(level -> level.setBlock(A(xt, 2, z0), WildcatterBlocks.DRY_TYPE_TRANSFORMER.defaultBlockState()
				.setValue(dev.wildcatter.block.TransformerBlock.FACING, Direction.EAST), 3));
		// the line out: 40 ft poles every 30 m east along z 7, crossarm and three pins each, strung at 12.47 kV
		List<BlockPos[]> tops = new ArrayList<>();
		for (int k = 0; k < 6; k++) {
			int px = 4 + 30 * k, pz = 11;
			int base = surface(px, pz);
			fillAbs(ox + px - 2, base, oz + pz - 2, ox + px + 2, base + 14, oz + pz + 2, "minecraft:air");
			BlockPos main = new BlockPos(ox + px, base, oz + pz);
			placeBig(WildcatterBlocks.WOOD_POLE_40, main, NORTH);
			BlockPos top = main.above(9);
			placeBig(WildcatterBlocks.CROSSARM_DIST, top.north(), Direction.EAST);
			BlockPos[] pins = {top.above().north(), top.above(), top.above().south()};
			String[] phase = {"b", "a", "c"};
			for (int i = 0; i < 3; i++) {
				insulator(WildcatterBlocks.INSULATOR_DIST, pins[i], phase[i]);
			}
			tops.add(pins);
		}
		for (int k = 1; k < tops.size(); k++) {
			for (int i = 0; i < 3; i++) {
				span(tops.get(k - 1)[i], tops.get(k)[i], dev.wildcatter.energy.Voltage.DISTRIBUTION);
			}
		}
		context.waitTicks(200);
		log("fire: rig " + read(A(hx, 2, z0)) + " | dry-type " + read(A(xt, 2, z0)) + " | turbine " + read(A(0, 2, 0)) + " | boiler " + read(A(0, 2, -1)));
		if (wanted(1)) {
			// from the west, out of the steam's way (it blows south and billows up), the rig behind the plant
			shoot(1, "fire_to_steam", 12450, new Cam(-3.2, 3.6, 5.0, 1.2, 3.4, 0.3), new Cam(-1.8, 4.0, 3.8, 1.3, 3.6, 0.2), 180);
		}
	}

	// ---- 2: the oilfield -------------------------------------------------------------------------------------------

	/**
	 * The land rig as the landRig scene builds it (substructure, mast, drawworks, mud tanks, two mud pumps, drive house, top drive, BOP),
	 * powered by a diesel set through a unit substation, drilling; in front of it the lease as the lease scene builds it (pumpjack,
	 * separator, stock and water tanks, H-pump, flare, gas genset), and two more pumpjacks on their own sets, started apart so they nod
	 * out of step.
	 */
	void oilfield() {
		int cx = ox, cz = oz;
		pad(-34, -18, 26, 82, "minecraft:grass_block");
		// the lease's wellheads (lease x 21 at z 0, and the two more at 21, 24 and 33, 32), lease origin at (cx-26, cz+40)
		int[][] wells = {{cx - 5, cz + 40}, {cx - 5, cz + 64}, {cx + 7, cz + 72}};
		long seed = -1;
		for (long s = 1; s < 4_000_000 && seed < 0; s++) {
			boolean all = true;
			for (int[] w : wells) {
				Reservoir oil = WildcatterClientGameTest.fieldAnyRule(s, w[0], w[1]);
				if (oil == null || oil.deep() || oil.kind() != Reservoir.Kind.OIL || oil.units() < 30_000) {
					all = false;
					break;
				}
			}
			if (all) {
				seed = s;
			}
		}
		log("oilfield seed " + seed);
		if (seed >= 0) {
			ReservoirData.testSeed = seed;
		}
		WildcatterClientGameTest.rules(server, true, "saltDomeGrid192", "mudGel", "wellControl");
		try {
			buildRig(cx, cz);
			buildLease(cx, cz);
		} finally {
			WildcatterClientGameTest.rules(server, false, "saltDomeGrid192", "mudGel", "wellControl");
		}
		ox = cx;
		oz = cz;
		// close on the second jack (main cell at -6, 64, facing east: its crank at x -9.7, the horsehead over the well at -4.5), the rig behind
		if (wanted(2)) {
			shoot(2, "oilfield", 12450, new Cam(-2.5, 2.2, 71, -7.5, 4.0, 64), new Cam(-4.2, 2.8, 68.6, -8.0, 4.2, 64), 180);
		}
		// the crank and counterweights close, from three sides, to check they draw steadily as they turn
		if (wanted(8)) {
			shoot(8, "crank_south", 12450, new Cam(-8.5, 2.6, 67.5, -9.7, 2.5, 64), new Cam(-12, 2.8, 67, -9.7, 2.5, 64), 100);
			shoot(8, "crank_north", 12450, new Cam(-8, 3.0, 60.5, -9.7, 2.5, 64), new Cam(-11.5, 2.4, 60.8, -9.7, 2.5, 64), 100);
			shoot(8, "crank_above", 12450, new Cam(-12.5, 5.5, 66.5, -9.7, 2.5, 64), new Cam(-12.5, 5.5, 61.5, -9.7, 2.5, 64), 100);
		}
		// the lease's flare (lease 36, 2: site 10, 42), the other candidate for the flare shot
		if (wanted(7)) {
			shoot(7, "flare_lease", 12850, new Cam(4, 2.0, 49, 10.5, 7.5, 42.5), new Cam(7.5, 10.0, 45.5, 10.5, 11.3, 42.5), 180);
		}
	}

	/** landRig, with the pad on the ground: the scene's x and z (hole at 30, -440) shifted onto the site. */
	void buildRig(int cx, int cz) {
		ox = cx - 30;
		oz = cz + 440;
		int hx = 30, hz = -440;
		fill(20, 0, -448, 47, 0, -426, "minecraft:stone");
		BlockPos origin = A(hx - 4, 1, hz - 4);
		placeOn(WildcatterBlocks.RIG_SUBSTRUCTURE, origin.below(), ox + hx - 4 + 0.5, oz + hz - 7 + 0.5, 0.0F);
		context.waitTicks(25);
		BlockPos floor = A(hx + 3, 6, hz - 3);
		placeOn(WildcatterBlocks.RIG_MAST, floor, ox + hx + 3.5, oz + hz - 2.5, 0.0F);
		placeOn(WildcatterBlocks.RIG_DRAWWORKS, floor, ox + hx + 3.5, oz + hz - 2.5, 0.0F);
		BlockPos ground = A(hx + 2, 0, hz + 12);
		for (Block part : new Block[] {WildcatterBlocks.MUD_TANKS, WildcatterBlocks.MUD_PUMP, WildcatterBlocks.MUD_PUMP, WildcatterBlocks.RIG_DRIVE_HOUSE}) {
			placeOn(part, ground, ox + hx + 2.5, oz + hz + 12.5, 180.0F);
		}
		context.waitTicks(25);
		clickWith(A(hx - 1, 20, hz - 1), Direction.WEST, new ItemStack(WildcatterItems.TOP_DRIVE));
		placeBig(WildcatterBlocks.BOP_STACK, A(hx, 2, hz), NORTH);
		placeBig(WildcatterBlocks.BOP_ACCUMULATOR, A(hx - 10, 1, hz - 10), NORTH);
		// water: a 120 V starter plant runs a pump from a pond, its pipe to the mud tanks
		put(42, 0, hz + 9, "minecraft:campfire[lit=true]");
		put(42, 1, hz + 9, "wildcatter:boiler");
		put(41, 1, hz + 9, "wildcatter:steam_turbine");
		put(40, 1, hz + 9, "wildcatter:wool_wire_hot");
		put(39, 1, hz + 9, "wildcatter:pump");
		for (int x = 39; x <= 41; x++) {
			put(x, 1, hz + 10, "wildcatter:wool_wire_neutral");
		}
		put(37, 1, hz + 9, "minecraft:stone");
		put(38, 1, hz + 8, "minecraft:stone");
		put(38, 1, hz + 10, "minecraft:stone");
		put(38, 2, hz + 9, "minecraft:stone");
		put(38, 1, hz + 9, "minecraft:water");
		for (int z = hz + 8; z >= hz + 6; z--) {
			put(39, 1, z, "wildcatter:pipe");
		}
		on(level -> ((dev.wildcatter.block.BoilerBlockEntity) level.getBlockEntity(A(42, 1, hz + 9))).addWater(4000));
		// power: a 500 kW diesel set, a unit substation run backward to 4.16 kV, MV cable to the drive house's west end
		BlockPos set = A(10, 1, hz + 7);
		genset(set, NORTH, false);
		inlineTransformer(WildcatterBlocks.UNIT_SUBSTATION, set, Direction.EAST, false);
		for (int x = 15; x <= hx - 7; x++) {
			put(x, 1, hz + 7, "wildcatter:cable_mv");
		}
		startGenset(set, NORTH, 1000);
		context.waitTicks(100);
		on(level -> {
			var rig = (dev.wildcatter.block.RigSubstructureBlockEntity) level.getBlockEntity(origin);
			weighted(rig);
			rig.toggle();
		});
		context.waitTicks(200);
		log("rig: " + read(origin) + " | set " + read(set));
	}

	/** The lease scene's build, its origin at (cx-26, cz+40), and two more pumpjacks south of it on sets of their own. */
	void buildLease(int cx, int cz) {
		ox = cx - 26;
		oz = cz + 40;
		BlockPos set = A(11, 1, 0);
		genset(set, NORTH, false);
		BlockPos box = A(15, 1, 0);
		put(21, 1, 0, "wildcatter:oil_wellhead[facing=east]");
		placeBig(WildcatterBlocks.PUMPJACK, box.relative(Direction.EAST, 5), Direction.EAST);
		placeBig(WildcatterBlocks.SEPARATOR, A(23, 1, 0), Direction.WEST);
		placeBig(WildcatterBlocks.STOCK_TANK, A(28, 1, -7), NORTH);
		placeBig(WildcatterBlocks.WATER_TANK, A(28, 1, 3), NORTH);
		placeBig(WildcatterBlocks.INJECTION_PUMP, A(22, 1, 10), Direction.WEST);
		placeBig(WildcatterBlocks.FLARE_STACK, A(36, 1, 2), NORTH);
		put(25, 0, 13, "wildcatter:casing");
		put(25, 1, 13, "wildcatter:disposal_wellhead");
		BlockPos gasSet = A(44, 1, -4);
		genset(gasSet, NORTH, true);
		for (int x = 12; x <= 14; x++) {
			put(x, 1, 0, "wildcatter:cable_industrial");
		}
		for (int zz = 1; zz <= 10; zz++) {
			put(13, 1, zz, "wildcatter:cable_industrial");
		}
		for (int x = 14; x <= 21; x++) {
			put(x, 1, 10, "wildcatter:cable_industrial");
		}
		sizeCables(A(12, 1, 0), A(21, 1, 10), WildcatterBlocks.CABLE_INDUSTRIAL, dev.wildcatter.energy.ConductorSize.AWG_4_0);
		put(22, 1, 0, "wildcatter:gas_line");
		pipes(new int[][] {{27, 2, -1}, {27, 2, -2}, {27, 2, -3}, {27, 1, -3}, {27, 1, -4}});
		for (int zz = 0; zz <= 3; zz++) {
			put(27, 1, zz, "wildcatter:gas_line");
		}
		pipes(new int[][] {{29, 1, 7}, {29, 1, 8}, {29, 1, 9}, {28, 1, 9}});
		for (int zz = 10; zz <= 13; zz++) {
			put(25, 2, zz, "wildcatter:gas_line");
		}
		for (int x = 26; x <= 33; x++) {
			put(x, 3, 0, "wildcatter:gas_line");
		}
		put(33, 2, 0, "wildcatter:gas_line");
		for (int x = 33; x <= 40; x++) {
			put(x, 1, 0, "wildcatter:gas_line");
		}
		put(40, 1, -1, "wildcatter:gas_line");
		put(40, 1, -2, "wildcatter:gas_line");
		put(36, 1, 1, "wildcatter:gas_line");
		// two more pumpjacks to the south, each on a set of its own like the lease's
		int[][] more = {{15, 24}, {27, 32}};
		List<BlockPos> sets = new ArrayList<>();
		for (int[] m : more) {
			BlockPos b = A(m[0], 1, m[1]);
			put(m[0] + 6, 1, m[1], "wildcatter:oil_wellhead[facing=east]");
			placeBig(WildcatterBlocks.PUMPJACK, b.relative(Direction.EAST, 5), Direction.EAST);
			BlockPos s = A(m[0] - 4, 1, m[1]);
			genset(s, NORTH, false);
			for (int x = m[0] - 3; x <= m[0] - 1; x++) {
				put(x, 1, m[1], "wildcatter:cable_industrial");
			}
			sizeCables(A(m[0] - 3, 1, m[1]), A(m[0] - 1, 1, m[1]), WildcatterBlocks.CABLE_INDUSTRIAL, dev.wildcatter.energy.ConductorSize.AWG_4_0);
			sets.add(s);
		}
		startGenset(set, NORTH, 1000);
		on(level -> ((dev.wildcatter.block.StorageTankBlockEntity) level.getBlockEntity(A(28, 1, 3))).testFill(dev.wildcatter.fluid.Product.PRODUCED_WATER, 20_000));
		startGenset(sets.get(0), NORTH, 1000);   // each set cranks for a few seconds: the jacks start apart, out of step
		context.waitTicks(37);
		startGenset(sets.get(1), NORTH, 1000);
		on(level -> panel(level.getServer(), gasSet).setWantRun(true));
		context.waitTicks(260);
		on(level -> {
			for (int[] m : more) {
				if (BigParts.entity(level, A(m[0] + 5, 1, m[1])) instanceof dev.wildcatter.block.PumpjackBlockEntity jack) {
					jack.testFill(0);   // no flowline on these two: their own tanks emptied, as a truck would
				}
			}
		});
		log("lease: jack " + read(A(20, 1, 0)) + " | sep " + read(A(24, 1, 0)) + " | flare " + read(A(36, 6, 2)) + " | gas set " + read(gasSet)
				+ " | jack2 " + read(A(20, 1, 24)) + " | jack3 " + read(A(32, 1, 32)));
	}

	// ---- 3: the refinery -------------------------------------------------------------------------------------------

	/** The crude unit (crudeUnit scene) and, north of it, the gas plant (gasPlant scene) with its flare, both running. */
	void refinery() {
		int cx = ox, cz = oz;
		pad(-20, -58, 36, 30, "minecraft:grass_block");
		fill(-16, 0, -54, 34, 0, 10, "minecraft:gravel");
		long seed = -1;
		for (long s = 1; s < 3_000_000 && seed < 0; s++) {
			Reservoir gas = WildcatterClientGameTest.fieldAnyRule(s, cx - 10, cz - 40);
			if (gas != null && !gas.deep() && gas.kind() == Reservoir.Kind.GAS && gas.units() > 100_000) {
				seed = s;
			}
		}
		log("refinery gas seed " + seed);
		if (seed >= 0) {
			ReservoirData.testSeed = seed;
		}
		ox = cx - 18;
		oz = cz - 40;
		buildGasPlant();
		ox = cx - 18;
		oz = cz;
		buildCrudeUnit();
		ox = cx - 18;
		oz = cz;
		if (wanted(3)) {
			shoot(3, "refinery", 12650, new Cam(46, 5, 30, 21, 13, -4), new Cam(40, 8, 36, 20, 14, -8), 180);
		}
		// the gas plant's flare (gas plant 41, -9: site 23, -49, its tip 10.1 m up), at dusk: a crane up the stack to the flame
		if (wanted(7)) {
			ox = cx;
			oz = cz;
			shoot(7, "flare", 12850, new Cam(26, 2.0, -56, 23.5, 7.5, -49), new Cam(24.6, 10.4, -52.2, 23.5, 11.4, -48.5), 180);
		}
	}

	void buildGasPlant() {
		placeBig(WildcatterBlocks.GAS_WELLHEAD, A(8, 1, 0), NORTH);
		placeBig(WildcatterBlocks.SEPARATOR, A(10, 1, 0), Direction.WEST);
		placeBig(WildcatterBlocks.GAS_COMPRESSOR, A(18, 1, 0), Direction.WEST);
		placeBig(WildcatterBlocks.TEG_DEHYDRATOR, A(28, 1, -1), Direction.WEST);
		placeBig(WildcatterBlocks.NGL_CHILLER, A(34, 1, -1), Direction.WEST);
		placeBig(WildcatterBlocks.REFRIGERATION_COMPRESSOR, A(34, 1, 2), Direction.WEST);
		placeBig(WildcatterBlocks.STABILIZER, A(43, 1, -3), NORTH);
		placeBig(WildcatterBlocks.LPG_BULLET, A(46, 1, -8), NORTH);
		placeBig(WildcatterBlocks.STOCK_TANK, A(42, 1, 2), NORTH);
		BlockPos power = A(16, 1, 3);
		genset(power, NORTH, true);
		inlineTransformer(WildcatterBlocks.UNIT_SUBSTATION, power, Direction.EAST, false);
		for (int[] p : new int[][] {{21, 3}, {21, 2}, {21, 1}, {22, 1}}) {
			put(p[0], 1, p[1], "wildcatter:cable_mv");
		}
		put(17, 1, 4, "wildcatter:cable_industrial");
		for (int x = 17; x <= 34; x++) {
			put(x, 1, 5, "wildcatter:cable_industrial");
		}
		put(34, 1, 4, "wildcatter:cable_industrial");
		put(34, 1, 3, "wildcatter:cable_industrial");
		sizeCables(A(17, 1, 3), A(34, 1, 5), WildcatterBlocks.CABLE_INDUSTRIAL, dev.wildcatter.energy.ConductorSize.AWG_4_0);
		put(9, 1, 0, "wildcatter:gas_line");
		for (int x = 13; x <= 16; x++) {
			put(x, 3, 0, "wildcatter:gas_line");
		}
		put(16, 2, 0, "wildcatter:gas_line");
		put(16, 1, 0, "wildcatter:gas_line");
		put(17, 1, 0, "wildcatter:gas_line");
		pipes(new int[][] {{14, 3, 1}, {14, 3, 2}, {14, 2, 2}, {14, 1, 2}});
		put(27, 1, -1, "wildcatter:gas_line");
		put(33, 1, -1, "wildcatter:gas_line");
		pipes(new int[][] {{41, 1, -2}, {41, 1, -3}, {41, 1, -4}, {42, 1, -4}, {43, 1, -4}});
		put(45, 1, -3, "wildcatter:gas_line");
		for (int zz = -1; zz <= 1; zz++) {
			put(43, 1, zz, "wildcatter:gas_line");
		}
		BlockPos sales = A(40, 1, -10);
		genset(sales, NORTH, true);
		put(41, 2, -1, "wildcatter:gas_line");
		for (int zz = -8; zz <= -1; zz++) {
			put(41, 3, zz, "wildcatter:gas_line");
		}
		put(41, 2, -8, "wildcatter:gas_line");
		for (int x = 37; x <= 41; x++) {
			put(x, 1, -8, "wildcatter:gas_line");
		}
		placeBig(WildcatterBlocks.FLARE_STACK, A(41, 1, -9), NORTH);
		put(14, 2, -1, "wildcatter:gas_line");
		startGenset(power, NORTH, 0);
		context.waitTicks(200);
		on(level -> panel(level.getServer(), sales).setWantRun(true));
		context.waitTicks(300);
		log("gas plant: flare " + read(A(41, 1, -9)) + " | comp " + read(A(20, 1, 0)) + " | chill " + read(A(36, 1, -1)) + " | stab " + read(A(43, 1, -3)) + " | sales set " + read(sales));
	}

	void buildCrudeUnit() {
		placeBig(WildcatterBlocks.ATMOSPHERIC_COLUMN, A(20, 1, 0), NORTH);
		placeBig(WildcatterBlocks.CRUDE_HEATER, A(23, 1, 0), NORTH);
		placeBig(WildcatterBlocks.SIDE_STRIPPERS, A(20, 1, -1), NORTH);
		placeBig(WildcatterBlocks.OVERHEAD_CONDENSER, A(16, 1, 0), NORTH);
		context.waitTicks(25);
		placeBig(WildcatterBlocks.CRUDE_CHARGE_PUMP, A(24, 1, 8), NORTH);
		placeBig(WildcatterBlocks.DESALTER, A(28, 1, 18), Direction.SOUTH);
		BlockPos crude = A(26, 1, 20), fuel = A(24, 1, -6);
		placeBig(WildcatterBlocks.STOCK_TANK, crude, NORTH);
		placeBig(WildcatterBlocks.STOCK_TANK, fuel, NORTH);
		pipes(new int[][] {{27, 1, 19}});
		pipes(new int[][] {{24, 1, -2}, {24, 1, -1}});
		BlockPos set = A(10, 1, 12);
		genset(set, NORTH, false);
		inlineTransformer(WildcatterBlocks.UNIT_SUBSTATION, set, Direction.EAST, false);
		for (int x = 15; x <= 22; x++) {
			put(x, 1, 12, "wildcatter:cable_mv");
		}
		for (int zz = 8; zz <= 11; zz++) {
			put(22, 1, zz, "wildcatter:cable_mv");
		}
		put(23, 1, 8, "wildcatter:cable_mv");
		put(11, 1, 13, "wildcatter:cable_industrial");
		for (int x = 4; x <= 11; x++) {
			put(x, 1, 14, "wildcatter:cable_industrial");
		}
		for (int zz = 15; zz <= 24; zz++) {
			put(11, 1, zz, "wildcatter:cable_industrial");
		}
		for (int x = 12; x <= 30; x++) {
			put(x, 1, 24, "wildcatter:cable_industrial");
		}
		for (int zz = 18; zz <= 23; zz++) {
			put(30, 1, zz, "wildcatter:cable_industrial");
		}
		put(29, 1, 18, "wildcatter:cable_industrial");
		for (int zz = 0; zz <= 13; zz++) {
			put(4, 1, zz, "wildcatter:cable_industrial");
		}
		for (int x = 5; x <= 15; x++) {
			put(x, 1, 0, "wildcatter:cable_industrial");
		}
		BlockPos set2 = A(10, 1, 16);
		genset(set2, NORTH, false);
		sizeCables(A(4, 1, 0), A(30, 1, 24), WildcatterBlocks.CABLE_INDUSTRIAL, dev.wildcatter.energy.ConductorSize.AWG_4_0);
		fillTank(fuel, dev.wildcatter.fluid.Product.RESIDUE, 30_000);
		startGenset(set, NORTH, 2000);
		startGenset(set2, NORTH, 2000);
		fillTank(crude, dev.wildcatter.fluid.Product.CRUDE, 60_000);
		context.waitTicks(240);
		log("crude unit: " + read(A(20, 1, 0)) + " | heater " + read(A(25, 1, 3)) + " | pump " + read(A(24, 1, 8)));
	}

	// ---- 4: power lines --------------------------------------------------------------------------------------------

	/** The substation yard (substationParts scene), fenced, and a 138 kV H-frame line (lineParts' H-frames) marching to it over the land. */
	void powerLines() {
		pad(-4, -20, 44, 18, "minecraft:grass_block");
		fill(6, 0, -8, 34, 0, 14, "minecraft:gravel");
		Object[][] parts = {
				{WildcatterBlocks.HV_BREAKER_3P, 10, 0}, {WildcatterBlocks.DISCONNECT_HV, 12, 0}, {WildcatterBlocks.POWER_TRANSFORMER, 16, 2},
				{WildcatterBlocks.RELAY_PANEL, 18, 0}, {WildcatterBlocks.BUS_DIST, 22, 3}, {WildcatterBlocks.DISCONNECT_DIST, 23, 3},
				{WildcatterBlocks.DIST_BREAKER_3P, 24, 3}};
		for (Object[] part : parts) {
			BlockPos main = A((Integer) part[1], 1, (Integer) part[2]);
			placeOn((Block) part[0], main.below(), main.getX() + 0.5, oz - 6.5, 0.0F);
		}
		placeOn(WildcatterBlocks.BUS_HV, A(14, 0, 0), ox + 14.5, oz - 6.5, 0.0F);
		context.waitTicks(40);
		log("yard: " + read(A(16, 1, 2)));
		// the fence round the yard, chain link with barbed wire on top
		for (int x = 6; x <= 34; x++) {
			for (int z : new int[] {-8, 14}) {
				fill(x, 1, z, x, 1, z, "wildcatter:chain_link_fence replace air");
				fill(x, 2, z, x, 2, z, "wildcatter:barbed_wire_top replace air");
			}
		}
		for (int z = -8; z <= 14; z++) {
			for (int x : new int[] {6, 34}) {
				fill(x, 1, z, x, 1, z, "wildcatter:chain_link_fence replace air");
				fill(x, 2, z, x, 2, z, "wildcatter:barbed_wire_top replace air");
			}
		}
		// the line: H-frames every 40 m west from the yard, each on the land where it stands
		List<BlockPos[]> strings = new ArrayList<>();
		for (int k = 0; k < 6; k++) {
			int px = 1 - 40 * k;
			int base = k == 0 ? gy + 1 : Math.max(surface(px, -2), surface(px, 2));
			fillAbs(ox + px - 2, base, oz - 6, ox + px + 2, base + 22, oz + 6, "minecraft:air");
			fillAbs(ox + px, base - 6, oz - 2, ox + px, base - 1, oz - 2, "minecraft:dirt replace air");
			fillAbs(ox + px, base - 6, oz + 2, ox + px, base - 1, oz + 2, "minecraft:dirt replace air");
			int b = base;
			placeBig(WildcatterBlocks.WOOD_POLE_75, new BlockPos(ox + px, b, oz - 2), NORTH);
			placeBig(WildcatterBlocks.WOOD_POLE_75, new BlockPos(ox + px, b, oz + 2), NORTH);
			placeBig(WildcatterBlocks.CROSSBEAM_138, new BlockPos(ox + px, b + 19, oz), Direction.EAST);
			placeBig(WildcatterBlocks.X_BRACE_138, new BlockPos(ox + px, b + 15, oz), Direction.EAST);
			BlockPos[] s = {new BlockPos(ox + px, b + 18, oz - 4), new BlockPos(ox + px, b + 18, oz), new BlockPos(ox + px, b + 18, oz + 4)};
			String[] phase = {"b", "a", "c"};
			for (int i = 0; i < 3; i++) {
				insulator(WildcatterBlocks.INSULATOR_HV, s[i], phase[i]);
			}
			strings.add(s);
		}
		for (int k = 1; k < strings.size(); k++) {
			for (int i = 0; i < 3; i++) {
				span(strings.get(k)[i], strings.get(k - 1)[i], dev.wildcatter.energy.Voltage.HIGH);
			}
		}
		context.waitTicks(40);
		shoot(4, "power_lines", 12400, new Cam(40, 5, 16, -60, 16, 0), new Cam(37, 11, 19, -60, 17, 0), 180);
	}

	// ---- 5: gensets ------------------------------------------------------------------------------------------------

	/** Three pairs of diesel sets as the synchronizing scene builds them: A closes onto the dead bus, B synchronizes on, heaters share. */
	void gensets() {
		ox -= 15;
		oz -= 6;
		pad(0, -8, 32, 22, "minecraft:grass_block");
		List<BlockPos[]> pairs = new ArrayList<>();
		for (int d : new int[] {0, 7, 14}) {
			BlockPos a = A(12, 1, d), b = A(17, 1, d);
			genset(a, NORTH, false);
			genset(b, Direction.SOUTH, false);
			for (int x = 13; x <= 16; x++) {
				put(x, 1, d, "wildcatter:cable_industrial");
			}
			put(14, 1, d + 1, "wildcatter:three_phase_heater");
			put(15, 1, d + 1, "wildcatter:three_phase_heater");
			pairs.add(new BlockPos[] {a, b});
		}
		for (BlockPos[] p : pairs) {
			startGenset(p[0], NORTH, 1000);
		}
		for (BlockPos[] p : pairs) {
			startGenset(p[1], Direction.SOUTH, 1000);
		}
		context.waitTicks(60);
		shoot(5, "genset_or_motors", 12450, new Cam(27, 1.7, -3, 13, 2.6, 9), new Cam(28, 2.5, 3, 12, 2.6, 11), 160);
	}

	// ---- 6: the reactor ----------------------------------------------------------------------------------------------

	/**
	 * The turbine island scene's build (the PWR island, the turbine hall, the gas turbine for offsite power, its load banks), brought to
	 * 25 % with the turbine generator on line; the natural draft cooling tower east of it (coolingTower scene) with its plume up.
	 */
	void reactor() {
		int cx = ox, cz = oz;
		pad(-44, -64, 130, 46, "minecraft:grass_block");
		ox = cx - 30;
		oz = cz;
		try {
			buildTurbineIsland();
		} catch (Throwable t) {
			log("turbine island: " + t);
		}
		ox = cx - 30;
		oz = cz;
		try {
			buildTower(115, 10);
		} catch (Throwable t) {
			log("tower: " + t);
		}
		ox = cx - 30;
		oz = cz;
		// tilted up enough to keep the tower's top and its plume in the frame
		shoot(6, "reactor", 12550, new Cam(34, 7, 56, 70, 38, -12), new Cam(46, 13, 58, 76, 44, -12), 180);
	}

	void buildTurbineIsland() {
		Direction north = NORTH;
		fill(32, -3, -13, 43, 0, -4, "minecraft:stone");
		fill(33, -2, -12, 42, 0, -5, "minecraft:water");
		BlockPos cw1 = A(34, -2, -11), cw2 = A(38, -2, -11);
		placeBig(WildcatterBlocks.CIRCULATING_WATER_PUMP, cw1, north);
		placeBig(WildcatterBlocks.CIRCULATING_WATER_PUMP, cw2, north);
		BlockPos vessel = A(0, 1, 0);
		placeBig(WildcatterBlocks.REACTOR_VESSEL, vessel, north);
		placeBig(WildcatterBlocks.CRDM_HEAD, A(0, 11, 0), north);
		placeBig(WildcatterBlocks.STEAM_GENERATOR, A(0, 1, -3), north);
		BlockPos pumpA = A(0, 1, -5), pumpB = A(0, 1, 6);
		placeBig(WildcatterBlocks.REACTOR_COOLANT_PUMP, pumpA, north);
		placeBig(WildcatterBlocks.PRESSURIZER, A(-2, 1, -3), north);
		placeBig(WildcatterBlocks.STEAM_GENERATOR, A(0, 1, 3), north);
		placeBig(WildcatterBlocks.REACTOR_COOLANT_PUMP, pumpB, north);
		BlockPos cvcs = A(-3, 1, 0);
		placeBig(WildcatterBlocks.CVCS_SKID, cvcs, north);
		placeBig(WildcatterBlocks.REACTOR_CONSOLE, A(3, 4, 1), north);
		BlockPos afw = A(12, 1, 0);
		placeBig(WildcatterBlocks.AUX_FEED_PUMP, afw, north);
		placeBig(WildcatterBlocks.TURBINE_VALVE_CHEST, A(20, 1, -6), north);
		BlockPos hp = A(20, 1, -3);
		placeBig(WildcatterBlocks.HP_TURBINE, hp, north);
		placeBig(WildcatterBlocks.MOISTURE_SEPARATOR_REHEATER, A(24, 1, -7), north);
		placeBig(WildcatterBlocks.LP_TURBINE, A(20, 1, 3), north);
		placeBig(WildcatterBlocks.MAIN_CONDENSER, A(26, 1, 3), north);
		placeBig(WildcatterBlocks.CONDENSATE_PUMP, A(26, 1, 11), north);
		placeBig(WildcatterBlocks.CONDENSATE_PUMP, A(29, 1, 11), north);
		BlockPos tg = A(21, 1, 11);
		placeBig(WildcatterBlocks.TG_GENERATOR_60, tg, north);
		placeBig(WildcatterBlocks.GEN_BREAKER_3000, A(22, 1, 19), north);
		BlockPos da = A(29, 1, 13);
		placeBig(WildcatterBlocks.DEAERATOR, da, north);
		placeBig(WildcatterBlocks.MAIN_FEED_PUMP, A(27, 1, 13), north);
		placeBig(WildcatterBlocks.MAIN_FEED_PUMP, A(27, 1, 18), north);
		placeBig(WildcatterBlocks.HP_FEEDWATER_HEATER, A(27, 1, 23), north);
		for (int zz = -2; zz <= 4; zz++) {
			put(1, 14, zz, "wildcatter:gas_line");
		}
		put(1, 13, 4, "wildcatter:gas_line");
		for (int x = 1; x <= 21; x++) {
			put(x, 13, -2, "wildcatter:gas_line");
		}
		for (int zz = -5; zz <= -3; zz++) {
			put(21, 13, zz, "wildcatter:gas_line");
		}
		for (int k = 5; k <= 12; k++) {
			put(21, k, -5, "wildcatter:gas_line");
		}
		for (int x = 3; x <= 12; x++) {
			put(x, 1, 4, "wildcatter:gas_line");
		}
		for (int zz = -1; zz <= 3; zz++) {
			put(3, 1, zz, "wildcatter:gas_line");
		}
		for (int k = 1; k <= 4; k++) {
			put(27, k, 31, "wildcatter:gas_line");
		}
		for (int x = 12; x <= 26; x++) {
			put(x, 4, 31, "wildcatter:gas_line");
		}
		for (int zz = 4; zz <= 30; zz++) {
			put(12, 4, zz, "wildcatter:gas_line");
		}
		put(12, 3, 4, "wildcatter:gas_line");
		put(12, 2, 4, "wildcatter:gas_line");
		for (int zz = -9; zz <= 2; zz++) {
			put(37, 1, zz, "wildcatter:gas_line");
		}
		for (int x = 38; x <= 41; x++) {
			put(x, 1, -8, "wildcatter:gas_line");
		}
		put(41, 1, -9, "wildcatter:gas_line");
		for (int x = 27; x <= 36; x++) {
			put(x, 1, 2, "wildcatter:gas_line");
		}
		BlockPos gt = A(60, 1, 0);
		placeBig(WildcatterBlocks.GAS_TURBINE, gt, north);
		placeBig(WildcatterBlocks.GT_INLET_HOUSE, A(60, 1, -4), north);
		placeBig(WildcatterBlocks.GT_EXHAUST_STACK, A(60, 1, 10), north);
		placeBig(WildcatterBlocks.GT_GENERATOR, A(64, 1, 1), north);
		placeBig(WildcatterBlocks.GEN_BREAKER, A(65, 1, 7), north);
		BlockPos bank1 = A(65, 1, 10), bank2 = A(68, 1, 10);
		placeBig(WildcatterBlocks.LOAD_BANK, bank1, north);
		placeBig(WildcatterBlocks.LOAD_BANK, bank2, north);
		BlockPos fuelTank = A(54, 1, 4);
		placeBig(WildcatterBlocks.STOCK_TANK, fuelTank, north);
		pipes(new int[][] {{58, 1, 6}, {59, 1, 6}});
		fillTank(fuelTank, dev.wildcatter.fluid.Product.KEROSENE, 40_000);
		BlockPos starterSet = A(50, 1, 2);
		genset(starterSet, north, false);
		inlineTransformer(WildcatterBlocks.UNIT_SUBSTATION, starterSet, Direction.EAST, false);
		for (int x = 55; x <= 59; x++) {
			put(x, 1, 2, "wildcatter:cable_mv");
		}
		BlockPos uat = A(72, 1, 3);
		placeBig(WildcatterBlocks.UNIT_AUX_TRANSFORMER, uat, north);
		List<int[]> bus = new ArrayList<>();
		for (int x = 65; x <= 70; x++) {
			bus.add(new int[] {x, 0, 9});
		}
		for (int k = 1; k <= 16; k++) {
			bus.add(new int[] {70, k, 9});
		}
		bus.add(new int[] {71, 16, 9});
		for (int zz = 0; zz <= 21; zz++) {
			bus.add(new int[] {72, 16, zz});
		}
		for (int k = 4; k <= 15; k++) {
			bus.add(new int[] {72, k, 0});
		}
		for (int x = 22; x <= 71; x++) {
			bus.add(new int[] {x, 16, 21});
		}
		for (int k = 0; k <= 15; k++) {
			bus.add(new int[] {22, k, 21});
		}
		for (int[] b : bus) {
			put(b[0], 1 + b[1], b[2], "wildcatter:bus_duct_gen");
		}
		for (int[] b : bus) {
			sizeCables(A(b[0], 1 + b[1], b[2]), A(b[0], 1 + b[1], b[2]), WildcatterBlocks.BUS_DUCT_GEN, dev.wildcatter.energy.ConductorSize.BUS_DUCT_3000);
		}
		List<int[]> mvTray = new ArrayList<>();
		for (int k = 0; k <= 14; k++) {
			mvTray.add(new int[] {72, k, 4});
		}
		for (int x = -1; x <= 71; x++) {
			mvTray.add(new int[] {x, 14, 4});
		}
		for (int zz = -5; zz <= 6; zz++) {
			mvTray.add(new int[] {-1, 14, zz});
		}
		for (int zz = 5; zz <= 18; zz++) {
			mvTray.add(new int[] {26, 14, zz});
		}
		for (int zz = -11; zz <= 3; zz++) {
			mvTray.add(new int[] {33, 14, zz});
		}
		for (int x = 34; x <= 37; x++) {
			mvTray.add(new int[] {x, 14, -11});
		}
		for (int zz = -8; zz <= 3; zz++) {
			mvTray.add(new int[] {7, 14, zz});
		}
		for (int k = 0; k <= 13; k++) {
			mvTray.add(new int[] {7, k, -8});
		}
		for (int[] c : mvTray) {
			put(c[0], 1 + c[1], c[2], "wildcatter:cable_mv");
		}
		BlockPos lvOut = inlineTransformer(WildcatterBlocks.UNIT_SUBSTATION, A(7, 1, -8), Direction.EAST, true);
		int lx = lvOut.getX() - ox, lz = lvOut.getZ() - oz;
		List<int[]> lvTray = new ArrayList<>();
		for (int k = 0; k <= 17; k++) {
			lvTray.add(new int[] {lx, k, lz});
		}
		for (int x = -4; x <= 25; x++) {
			lvTray.add(new int[] {x, 17, -8});
		}
		for (int zz = -7; zz <= 0; zz++) {
			lvTray.add(new int[] {-4, 17, zz});
			lvTray.add(new int[] {11, 17, zz});
		}
		for (int zz = -7; zz <= 11; zz++) {
			lvTray.add(new int[] {25, 17, zz});
		}
		for (int x = 26; x <= 28; x++) {
			lvTray.add(new int[] {x, 17, 11});
		}
		for (int[] c : lvTray) {
			put(c[0], 1 + c[1], c[2], "wildcatter:busway_industrial");
		}
		on(level -> {
			((dev.wildcatter.block.ReactorVesselBlockEntity) level.getBlockEntity(vessel)).setHotStandbyForTest(1500);
			((dev.wildcatter.block.CvcsBlockEntity) level.getBlockEntity(cvcs)).setAcidForTest(500);
		});
		water(cvcs, afw, da);
		startGenset(starterSet, north, 2000);
		on(level -> ((dev.wildcatter.block.GasTurbineBlockEntity) level.getBlockEntity(gt)).setWantRun(true));
		context.waitTicks(200);
		on(level -> ((dev.wildcatter.block.GasTurbineBlockEntity) level.getBlockEntity(gt)).skipStartForTest());
		context.waitTicks(100);
		log("GT: " + read(gt));
		int[][] mvDrops = {{-1, -5}, {-1, 6}, {26, 13}, {26, 18}, {33, -11}, {37, -11}};
		for (int[] d : mvDrops) {
			for (int k = 1; k <= 14; k++) {
				put(d[0], k, d[1], "wildcatter:cable_mv");
			}
			context.waitTicks(60);
		}
		int[][] lvDrops = {{-4, -3}, {-4, 0}, {11, 0}, {25, 11}, {28, 11}};
		for (int[] d : lvDrops) {
			for (int k = 1; k <= 17; k++) {
				put(d[0], k, d[1], "wildcatter:cable_industrial");
			}
			sizeCables(A(d[0], 1, d[1]), A(d[0], 17, d[1]), WildcatterBlocks.CABLE_INDUSTRIAL, dev.wildcatter.energy.ConductorSize.KCMIL_500);
			context.waitTicks(40);
		}
		put(-3, 1, -3, "wildcatter:cable_industrial");
		for (int x = -3; x <= -1; x++) {
			put(x, 11, 0, "wildcatter:cable_industrial");
		}
		sizeCables(A(-3, 1, -3), A(-1, 11, 0), WildcatterBlocks.CABLE_INDUSTRIAL, dev.wildcatter.energy.ConductorSize.KCMIL_500);
		for (int i = 0; i < 6; i++) {
			context.waitTicks(50);
			water(cvcs, afw, da);
		}
		dev.wildcatter.fluid.Reactor[] r = new dev.wildcatter.fluid.Reactor[1];
		on(level -> {
			r[0] = ((dev.wildcatter.block.ReactorVesselBlockEntity) level.getBlockEntity(vessel)).reactor();
			r[0].setSetpoint(0.25);
			r[0].setRodsForTest(Tuning.ROD_STEPS, 150);
			r[0].holdPowerForTest(0.2);
			((dev.wildcatter.block.LoadBankBlockEntity) level.getBlockEntity(bank1)).setSteps(4);
			((dev.wildcatter.block.LoadBankBlockEntity) level.getBlockEntity(bank2)).setSteps(4);
		});
		boolean synced = false;
		for (int i = 0; i < 90 && !synced; i++) {
			context.waitTicks(40);
			water(cvcs, afw, da);
			synced = server.computeOnServer(srv -> ((dev.wildcatter.block.GeneratorPartBlockEntity) srv.overworld().getBlockEntity(tg)).breakerClosed());
		}
		on(level -> {
			((dev.wildcatter.block.LoadBankBlockEntity) level.getBlockEntity(bank1)).setSteps(10);
			((dev.wildcatter.block.LoadBankBlockEntity) level.getBlockEntity(bank2)).setSteps(10);
		});
		for (int i = 0; i < 20; i++) {
			context.waitTicks(40);
			water(cvcs, afw, da);
		}
		log("turbine island: synced " + synced + " | vessel " + read(vessel) + " | turbine " + read(hp) + " | generator " + read(tg) + " | GT " + read(gt));
	}

	/** The natural draft tower (coolingTower scene), its controller at site x, z; built from its hopper, the rest by hook; 104 MW on it. */
	void buildTower(int cx, int cz) {
		fill(cx - 35, -4, cz - 70, cx + 40, -1, cz + 5, "minecraft:stone");
		BlockPos main = A(cx, 1, cz);
		placeBig(WildcatterBlocks.NATURAL_DRAFT_TOWER, main, NORTH);
		BlockPos hopper = A(cx + 1, 4, cz + 1);
		put(cx + 1, 4, cz + 1, "minecraft:hopper");
		String at = hopper.getX() + " " + hopper.getY() + " " + hopper.getZ();
		server.runCommand("item replace block " + at + " container.0 with minecraft:gray_concrete 64");
		server.runCommand("item replace block " + at + " container.1 with minecraft:gray_concrete 40");
		server.runCommand("item replace block " + at + " container.2 with wildcatter:steel_ingot 12");
		int stage = 0;
		for (int i = 0; i < 70 && stage == 0; i++) {
			context.waitTicks(40);
			stage = server.computeOnServer(srv -> ((dev.wildcatter.block.NaturalDraftTowerBlockEntity) srv.overworld().getBlockEntity(main)).stage());
		}
		on(level -> ((dev.wildcatter.block.NaturalDraftTowerBlockEntity) level.getBlockEntity(main)).buildForTest(level, dev.wildcatter.fluid.CoolingTower.lifts()));
		context.waitTicks(10);
		water(main);
		on(level -> ((dev.wildcatter.block.NaturalDraftTowerBlockEntity) level.getBlockEntity(main)).setHeatForTest(level, 104e6));
		keep = () -> on(level -> {   // the coolingTower scene's 104 MW, held while the shot is taken
			if (level.getBlockEntity(main) instanceof dev.wildcatter.block.NaturalDraftTowerBlockEntity t) {
				t.setHeatForTest(level, 104e6);
				if (t instanceof dev.wildcatter.block.WaterUser user) {
					user.addWater(100_000);
				}
			}
		});
		context.waitTicks(40);
		log("tower: stage " + stage + " | " + read(main));
	}
}
