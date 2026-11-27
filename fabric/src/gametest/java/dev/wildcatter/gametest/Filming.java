package dev.wildcatter.gametest;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.textures.GpuTexture;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.impl.client.gametest.threading.ThreadingImpl;
import net.fabricmc.fabric.impl.client.gametest.util.WindowHooks;
import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Films a scene into {@code build/scene-videos/<scene>.mov} (the Gradle run folder's parent).
 * <p>
 * By default it reads the game's own frames, not the screen, so another window over the game is never filmed (the recorder of Tailings' fix3-hi04, ported): each
 * frame the game has drawn, with its HUD and card, is copied on the GPU from the main render target into one of a few reused
 * read-back buffers ({@code copyTextureToBuffer}, as {@code Screenshot} does); when the copy is done the buffer's bytes go to a writer
 * thread, which pipes them as raw RGBA to ffmpeg ({@code -f rawvideo -pix_fmt rgba -s WxH -r 60 -i -}, flipped upright, x264).
 * <p>
 * <b>60 frames a second of game time</b> (film60, 2026-11-27, the default): while a take films, the game's frame timer runs on game
 * time, not the clock ({@code mixin.FilmClockMixin}): the client draws exactly three frames a tick, at partial ticks 0, 1/3 and 2/3,
 * with the game's own interpolation of entities, the camera and the player between ticks, and every frame is filmed. One second of
 * film is 60 frames and 20 ticks, whatever the clock: if the Mac draws or encodes slower than that, the take runs slower than real time
 * (the game waits for a free read-back buffer or for the writer) and drops nothing. The camera moves the scenes make from the test thread
 * (setPos / setYRot before a tick, which the tick would copy into the previous-tick fields and so show as a jump every third frame)
 * are blended: after each tick the camera entity's previous-tick position and rotation are set back to where it was at the end of the
 * tick before, unless it was snapped (snapTo, a teleport: the previous-tick fields set with it), which stays a cut. The frame-rate cap
 * (and the AFK cap, 30 then 10 fps after a minute without input) is lifted for the take.
 * <p>
 * WILDCATTER_FILM_TICKRATE=1 films as before film60: one frame a tick (the frame last drawn at the start of each client tick, on the
 * game's own clock), 20 a second of game time, each shown three times in the 60 fps film, a frame dropped (and counted) if the
 * read-backs or the writer are behind.
 * <p>
 * The film is twice the scene's window each way (2880 x 1620 for the usual 1440 x 810 window), the size of the old screen recordings
 * (the window doubled on a Retina screen). While filming, the game draws at that size itself (framebuffer-film, 2026-11-26): the
 * client test's window is virtual, so it is set to twice the scene's size for the take and the GUI scale doubled with it (the HUD and
 * cards stay the same share of the frame), both put back after. WILDCATTER_FILM_DOUBLED=1 instead films the scene's own window size,
 * each pixel doubled (nearest neighbour), as before.
 * <p>
 * WILDCATTER_FILM_SCREEN=1 films the screen as before, with macOS's screencapture of the game window's rectangle. Off the Mac or
 * with no ffmpeg, the scene still runs, unfilmed.
 */
public final class Filming {
	/** The game's frame timer, which {@code mixin.FilmClockMixin} makes run on game time while a 60 fps take films. */
	public interface Clock {
		/** The frame being drawn now is drawn at partial tick 0 and is the first of its tick's three. */
		void wildcatterFilmStart();
	}

	/** Ticks a second of game time, frames a second of film, the film's size over the scene's window. */
	private static final int TICK_FPS = 20, OUT_FPS = 60, FRAMES_PER_TICK = OUT_FPS / TICK_FPS, SCALE = 2, BUFFERS = 6;
	/**
	 * Frames waiting for the writer: about 256 MB of them, 6 to 60 (13, 0.2 s of game time at 60 fps, at 2880 x 1620), so a stall of
	 * the encoder (seen when another app is brought in front of the game) is waited out rather than a frame dropped.
	 */
	private static final long QUEUE_BYTES = 256L << 20;
	/** Longest a 60 fps take waits for a read-back or the writer before it gives the frame up (counted as dropped). */
	private static final long WAIT_LIMIT_NANOS = TimeUnit.SECONDS.toNanos(30);
	/** A camera move longer than this in a tick is a cut, never blended (a teleport the scene made without snapping). */
	private static final double CUT_BLOCKS = 16;
	private static final float CUT_DEGREES = 120;
	private final int queueSize;
	private static final String TAG = "[wildcatter-test] ";
	private static volatile Filming active;
	private static boolean hooked;
	/** What a native or 60 fps take changed (window size, GUI scale, frame cap), put back on the client thread after the take (or by the next take's start). */
	private static final AtomicReference<Runnable> RESTORE = new AtomicReference<>();

	private final Process process;
	private Minecraft client;
	private final String scene;
	/** 60 frames a second of game time (false: one frame a tick, WILDCATTER_FILM_TICKRATE). */
	private final boolean steady;
	private final int inFps;
	// framebuffer read-back (null when filming the screen)
	private final int width, height;
	private final GpuBuffer[] buffers;
	private final boolean[] busy;
	private final BlockingQueue<byte[]> queue;
	private final ArrayDeque<byte[]> spare = new ArrayDeque<>();
	private final Thread writer;
	private volatile boolean stopping, closed;
	private long grabbed, written, dropped, resizing, startNanos;
	/** Of the dropped: the frame not the film's size, every read-back buffer busy, the writer behind. */
	private long droppedSize, droppedBusy, droppedBehind;
	/** 60 fps: the times the game waited for a read-back buffer and for the writer, and the clock time spent waiting. */
	private long waitedBusy, waitedBehind, waitNanos;
	/** 60 fps: ticks in which the camera's move was blended, and cuts kept. */
	private long blended, cuts;
	private final Pose pose = new Pose();

	private Filming(Process process, String scene) {
		this(process, scene, 0, 0, null, false);
	}

	private Filming(Process process, String scene, int width, int height, OutputStream sink, boolean steady) {
		this.process = process;
		this.scene = scene;
		this.width = width;
		this.height = height;
		this.steady = steady;
		this.inFps = steady ? OUT_FPS : TICK_FPS;
		this.buffers = sink == null ? null : new GpuBuffer[BUFFERS];
		this.busy = new boolean[BUFFERS];
		this.queueSize = (int) Math.max(6, Math.min(60, QUEUE_BYTES / Math.max(1L, (long) width * height * 4)));
		this.queue = sink == null ? null : new ArrayBlockingQueue<>(queueSize);
		this.writer = sink == null ? null : new Thread(() -> write(sink), "wildcatter-filming-" + scene);
	}

	private static boolean set(String name) {
		String value = System.getenv(name);
		return value != null && !value.isBlank() && !value.equals("0");
	}

	static Filming start(Minecraft client, String scene) {
		if (!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac")) {
			return new Filming(null, scene);
		}
		restore();
		try {
			File folder = new File("../../scene-videos").getCanonicalFile();
			folder.mkdirs();
			File out = new File(folder, scene + ".mov");
			out.delete();
			if (set("WILDCATTER_FILM_SCREEN")) {
				var window = client.getWindow();
				String rect = window.getX() + "," + window.getY() + "," + window.getScreenWidth() + "," + window.getScreenHeight();
				Process process = new ProcessBuilder("screencapture", "-x", "-v", "-R", rect, out.getPath()).redirectErrorStream(true)
						.redirectOutput(new File(folder, scene + ".log")).start();
				System.out.println(TAG + "filming the screen: " + scene + " at " + rect + " to " + out);
				return new Filming(process, scene);
			}
			boolean doubled = set("WILDCATTER_FILM_DOUBLED");
			boolean steady = !set("WILDCATTER_FILM_TICKRATE") && client.getDeltaTracker() instanceof Clock;
			RenderTarget target = client.gameRenderer.mainRenderTarget();
			int w = target.width, h = target.height;
			Runnable back = () -> {};
			if (!doubled) {
				w *= SCALE;
				h *= SCALE;
				back = drawAt(client, w, h);
			}
			if (steady) {
				Runnable size = back, cap = uncapped(client);
				back = () -> {
					cap.run();
					size.run();
				};
			}
			RESTORE.set(back);
			int inFps = steady ? OUT_FPS : TICK_FPS;
			String scale = doubled ? "vflip,scale=" + SCALE + "*iw:" + SCALE + "*ih:flags=neighbor" : "vflip";
			String filter = steady ? scale : scale + ",fps=" + OUT_FPS;
			Process process = new ProcessBuilder(ffmpeg(), "-hide_banner", "-loglevel", "error", "-y",
					"-f", "rawvideo", "-pix_fmt", "rgba", "-s", w + "x" + h, "-r", Integer.toString(inFps), "-i", "-",
					"-vf", filter, "-c:v", "libx264", "-preset", "veryfast", "-crf", "14", "-pix_fmt", "yuv420p",
					out.getPath()).redirectErrorStream(true).redirectOutput(new File(folder, scene + ".log")).start();
			Filming film = new Filming(process, scene, w, h, new BufferedOutputStream(process.getOutputStream(), 1 << 20), steady);
			film.client = client;
			if (!hooked) {
				hooked = true;
				ClientTickEvents.START_CLIENT_TICK.register(Filming::tickStart);
				ClientTickEvents.END_CLIENT_TICK.register(Filming::tickEnd);
			}
			film.startNanos = System.nanoTime();
			film.writer.start();
			if (steady) {
				((Clock) client.getDeltaTracker()).wildcatterFilmStart();
			}
			active = film;
			System.out.println(TAG + "filming the game's frames: " + scene + " drawn at " + w + "x" + h + (doubled ? ", doubled" : "") + ", "
					+ (steady ? OUT_FPS + " frames a second of game time (" + FRAMES_PER_TICK + " a tick)" : TICK_FPS + " frames a second of game time (one a tick)")
					+ ", to " + out);
			return film;
		} catch (IOException ex) {
			restore();
			System.out.println(TAG + "cannot film " + scene + ": " + ex.getMessage());
			return new Filming(null, scene);
		}
	}

	/**
	 * On the client thread: the (virtual) window set to {@code w x h} for the take, the GUI scale doubled with it; returns what puts them
	 * back, each only if the scene has not changed it since.
	 */
	private static Runnable drawAt(Minecraft client, int w, int h) {
		var window = client.getWindow();
		int oldW = window.getWidth(), oldH = window.getHeight();
		var option = client.options.guiScale();
		int oldGui = option.get();
		((WindowHooks) (Object) window).fabric_resize(w, h);
		int gui = oldGui == 0 ? 0 : oldGui * SCALE;
		option.set(gui);
		client.resizeGui();
		return () -> {
			if (option.get() == gui) {
				option.set(oldGui);
			}
			if (window.getWidth() == w && window.getHeight() == h) {
				((WindowHooks) (Object) window).fabric_resize(oldW, oldH);
			}
			client.resizeGui();
		};
	}

	/** On the client thread: no frame-rate cap for a 60 fps take (nor the AFK one); returns what puts the options back. */
	private static Runnable uncapped(Minecraft client) {
		var limit = client.options.framerateLimit();
		var inactive = client.options.inactivityFpsLimit();
		int oldLimit = limit.get();
		InactivityFpsLimit oldInactive = inactive.get();
		limit.set(Options.UNLIMITED_FRAMERATE_CUTOFF);
		inactive.set(InactivityFpsLimit.MINIMIZED);
		return () -> {
			limit.set(oldLimit);
			inactive.set(oldInactive);
		};
	}

	/** On the client thread: the window, GUI scale and frame cap of the last take put back, if not done yet. */
	private static void restore() {
		Runnable back = RESTORE.getAndSet(null);
		if (back != null) {
			back.run();
		}
	}

	private static String ffmpeg() {
		for (String path : new String[] {"/opt/homebrew/bin/ffmpeg", "/usr/local/bin/ffmpeg"}) {
			if (new File(path).canExecute()) {
				return path;
			}
		}
		return "ffmpeg";
	}

	/**
	 * On the client thread, from {@code mixin.FilmClockMixin} at the start of every frame the client draws: during a 60 fps take the
	 * frame just drawn is filmed, and the frames to draw a tick are returned (the timer then runs on game time); 0 otherwise.
	 */
	public static int frameDrawn(Clock clock) {
		Filming film = active;
		if (film == null || !film.steady || film.closed) {
			return 0;
		}
		if (!film.stopping) {
			film.grab(film.client);
		}
		return FRAMES_PER_TICK;
	}

	/** On the client thread, at the start of each tick: one frame a tick filmed (WILDCATTER_FILM_TICKRATE), or the camera's cuts noted. */
	private static void tickStart(Minecraft client) {
		Filming film = active;
		if (film == null || film.closed || film.stopping) {
			return;
		}
		if (film.steady) {
			film.pose.before(client.getCameraEntity());
		} else {
			film.grab(client);
		}
	}

	/** On the client thread, at the end of each tick (60 fps): the camera's move this tick blended over the tick's three frames. */
	private static void tickEnd(Minecraft client) {
		Filming film = active;
		if (film != null && film.steady && !film.closed && !film.stopping) {
			film.pose.after(film, client.getCameraEntity());
		}
	}

	/**
	 * The camera entity at the end of the last tick. A tick copies an entity's position and rotation into its previous-tick fields
	 * before it moves, so a move a scene makes from the test thread (before the tick) would be drawn at its end from partial tick 0 on:
	 * a jump each tick. After the tick, the previous-tick fields are set back to where the camera was at the end of the tick before, so
	 * the three frames blend from there. A snap (snapTo, absSnapTo, a teleport packet: the previous-tick fields set with the new place,
	 * seen before the tick) or a move too long for a tick is a cut and stays one.
	 */
	private static final class Pose {
		private Entity entity;
		private double x, y, z;
		private float yRot, xRot, head;
		private boolean posCut, rotCut;

		void before(Entity e) {
			if (e == null || e != entity) {
				entity = null;
				return;
			}
			boolean moved = e.getX() != x || e.getY() != y || e.getZ() != z;
			posCut = moved && e.xo == e.getX() && e.yo == e.getY() && e.zo == e.getZ();
			boolean turned = e.getYRot() != yRot || e.getXRot() != xRot;
			rotCut = turned && e.yRotO == e.getYRot() && e.xRotO == e.getXRot();
		}

		void after(Filming film, Entity e) {
			if (e == null) {
				entity = null;
				return;
			}
			if (e == entity) {
				boolean blend = false;
				if (!posCut && e.position().distanceToSqr(x, y, z) < CUT_BLOCKS * CUT_BLOCKS) {
					blend |= e.xo != x || e.yo != y || e.zo != z;
					e.xo = x;
					e.yo = y;
					e.zo = z;
				} else if (posCut) {
					film.cuts++;
				}
				if (!rotCut && Math.abs(Mth.wrapDegrees(e.getYRot() - yRot)) < CUT_DEGREES && Math.abs(e.getXRot() - xRot) < CUT_DEGREES) {
					blend |= e.yRotO != yRot || e.xRotO != xRot;
					e.yRotO = yRot;
					e.xRotO = xRot;
					if (e instanceof LivingEntity living) {
						living.yHeadRotO = head;
					}
				} else if (rotCut) {
					film.cuts++;
				}
				if (blend) {
					film.blended++;
				}
			}
			entity = e;
			x = e.getX();
			y = e.getY();
			z = e.getZ();
			yRot = e.getYRot();
			xRot = e.getXRot();
			head = e instanceof LivingEntity living ? living.yHeadRot : yRot;
			posCut = rotCut = false;
		}
	}

	private int freeBuffer() {
		for (int i = 0; i < BUFFERS; i++) {
			if (!busy[i]) {
				return i;
			}
		}
		return -1;
	}

	/** On the client thread: the GPU's finished copies handed on (their callbacks run) until {@code done}, or the wait limit. */
	private void pump(java.util.function.BooleanSupplier done) {
		long t0 = System.nanoTime();
		// the copies' fences signal only once their commands are submitted (renderFrame submits at its end; a wait between frames must)
		RenderSystem.getDevice().createCommandEncoder().submit();
		while (!done.getAsBoolean() && System.nanoTime() - t0 < WAIT_LIMIT_NANOS) {
			RenderSystem.executePendingTasks();
			if (!done.getAsBoolean()) {
				LockSupport.parkNanos(200_000);
			}
		}
		waitNanos += System.nanoTime() - t0;
	}

	private void grab(Minecraft client) {
		RenderTarget target = client.gameRenderer.mainRenderTarget();
		GpuTexture texture = target.getColorTexture();
		if (texture == null || target.width != width || target.height != height) {
			if (written == 0 && grabbed == 0) {
				resizing++;   // the first frame, still drawn before the window was set to the film's size
			} else {
				dropped++;
				droppedSize++;
			}
			return;
		}
		int free = freeBuffer();
		if (free < 0 && steady) {
			waitedBusy++;   // the game waits for a read-back to come back rather than drop the frame
			pump(() -> freeBuffer() >= 0);
			free = freeBuffer();
		}
		if (free < 0) {
			dropped++;
			droppedBusy++;
			return;
		}
		int i = free;
		if (buffers[i] == null) {
			// MAP_READ | COPY_DST, as Screenshot's
			buffers[i] = RenderSystem.getDevice().createBuffer(() -> "Wildcatter filming buffer", GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST,
					(long) width * height * texture.getFormat().blockSize());
		}
		busy[i] = true;
		grabbed++;
		GpuBuffer buffer = buffers[i];
		RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(texture, buffer, 0L, () -> {
			try {
				if (!closed) {
					byte[] frame;
					synchronized (spare) {
						frame = spare.pollFirst();
					}
					if (frame == null) {
						frame = new byte[width * height * 4];
					}
					try (GpuBufferSlice.MappedView view = buffer.map(true, false)) {
						view.data().get(0, frame, 0, frame.length);
					}
					enqueue(frame);
				}
			} finally {
				busy[i] = false;
				if (closed) {
					buffer.close();
					buffers[i] = null;
				}
			}
		}, 0);
	}

	private void enqueue(byte[] frame) {
		if (queue.offer(frame)) {
			return;
		}
		if (steady) {
			waitedBehind++;   // the game waits for the writer rather than drop the frame
			long t0 = System.nanoTime();
			try {
				if (queue.offer(frame, WAIT_LIMIT_NANOS, TimeUnit.NANOSECONDS)) {
					waitNanos += System.nanoTime() - t0;
					return;
				}
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			waitNanos += System.nanoTime() - t0;
		}
		dropped++;   // the writer is behind
		droppedBehind++;
	}

	private static final byte[] END = new byte[0];

	private void write(OutputStream sink) {
		try (sink) {
			while (true) {
				byte[] frame = queue.take();
				if (frame == END) {
					break;
				}
				sink.write(frame);
				written++;
				synchronized (spare) {
					if (spare.size() < queueSize) {
						spare.addLast(frame);
					}
				}
			}
		} catch (IOException | InterruptedException ex) {
			System.out.println(TAG + "filming " + scene + ": the encoder stopped: " + ex.getMessage());
		}
	}

	/** The copies still on the GPU waited for, so the take's last frames are filmed too (on the client thread, from either thread). */
	private void flush() {
		try {
			if (client.isSameThread()) {
				pump(() -> !anyBusy());
			} else if (Thread.currentThread() == ThreadingImpl.testThread && ThreadingImpl.clientCanAcceptTasks) {
				ThreadingImpl.runOnClient(() -> pump(() -> !anyBusy()));
			}
		} catch (RuntimeException ex) {
			System.out.println(TAG + "filming " + scene + ": the last frames were not waited for: " + ex.getMessage());
		}
	}

	private boolean anyBusy() {
		for (boolean b : busy) {
			if (b) {
				return true;
			}
		}
		return false;
	}

	/** Stops the recording and writes the file. */
	void stop() {
		if (process == null) {
			return;
		}
		if (writer == null) {
			stopScreen();
			return;
		}
		stopping = true;
		double clock = (System.nanoTime() - startNanos) / 1e9;   // the take, start to stop: what the game's speed is measured on
		long tail0 = System.nanoTime();
		long waitedBefore = waitNanos;
		flush();
		waitNanos = waitedBefore;   // the end's wait is not the take's
		closed = true;
		active = null;
		try {
			queue.put(END);
			writer.join(120_000);
			if (!process.waitFor(300, TimeUnit.SECONDS)) {
				process.destroy();
			}
			double tail = (System.nanoTime() - tail0) / 1e9;
			double game = (written + dropped) / (double) inFps;
			System.out.println(String.format(Locale.ROOT,
					"%sfilmed %s at %dx%d, %d frames a second of game time: %d frames written (%.1f s of game time), %d dropped (%d wrong size, %d read-backs busy, %d writer behind), %d skipped while the window was set, over %.1f s by the clock: %.1f frames a second, game time at %.2fx the clock (then %.1f s for the encoder to finish)",
					TAG, scene, width, height, inFps, written, game, dropped, droppedSize, droppedBusy, droppedBehind, resizing, clock, written / clock, game / clock, tail)
					+ (steady ? String.format(Locale.ROOT, "; waited %d times for a read-back, %d for the writer (%.1f s); camera blended in %d ticks, %d cuts kept",
							waitedBusy, waitedBehind, waitNanos / 1e9, blended, cuts) : ""));
		} catch (InterruptedException ex) {
			System.out.println(TAG + "filming did not stop cleanly: " + ex.getMessage());
		}
		client.execute(() -> {
			for (int i = 0; i < BUFFERS; i++) {
				if (buffers[i] != null && !busy[i]) {
					buffers[i].close();
					buffers[i] = null;
				}
			}
			restore();
		});
	}

	/** Any character on screencapture's input stops the recording and writes the file. */
	private void stopScreen() {
		try {
			OutputStream in = process.getOutputStream();
			in.write('\n');
			in.flush();
			if (!process.waitFor(15, TimeUnit.SECONDS)) {
				new ProcessBuilder("kill", "-INT", Long.toString(process.pid())).start().waitFor();
				process.waitFor(15, TimeUnit.SECONDS);
			}
		} catch (IOException | InterruptedException ex) {
			System.out.println(TAG + "filming did not stop cleanly: " + ex.getMessage());
		}
	}
}
