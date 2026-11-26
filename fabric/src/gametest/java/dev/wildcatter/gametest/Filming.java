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
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.impl.client.gametest.util.WindowHooks;
import net.minecraft.client.Minecraft;

/**
 * Films a scene into {@code build/scene-videos/<scene>.mov} (the Gradle run folder's parent).
 * <p>
 * By default it reads the game's own frames, not the screen, so another window over the game is never filmed (the recorder of Tailings' fix3-hi04, ported): at the
 * start of each client tick (a client game test renders one frame a tick) the main render target, which still holds the frame just
 * drawn with its HUD and card, is copied on the GPU into one of a few reused read-back buffers ({@code copyTextureToBuffer}, as
 * {@code Screenshot} does); when the copy is done the buffer's bytes go to a writer thread, which pipes them as raw RGBA to ffmpeg
 * ({@code -f rawvideo -pix_fmt rgba -s WxH -r 20 -i -}, flipped upright and written at 60 fps in x264). So the film runs at the
 * game's ticks: one second of film is 20 ticks, whatever the clock. A frame is dropped (and counted) only if all the read-back buffers
 * are still busy or the writer is behind; the frames written, dropped and the real rate are logged at the end.
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
final class Filming {
	/** Frames read a second of game time, frames a second written, the film's size over the scene's window. */
	private static final int TICK_FPS = 20, OUT_FPS = 60, SCALE = 2, BUFFERS = 4, QUEUE = 6;
	private static final String TAG = "[wildcatter-test] ";
	private static volatile Filming active;
	private static boolean hooked;
	/** The window size and GUI scale a native take changed, put back on the client thread after the take (or by the next take's start). */
	private static final AtomicReference<Runnable> RESTORE = new AtomicReference<>();

	private final Process process;
	private Minecraft client;
	private final String scene;
	// framebuffer read-back (null when filming the screen)
	private final int width, height;
	private final GpuBuffer[] buffers;
	private final boolean[] busy;
	private final BlockingQueue<byte[]> queue;
	private final ArrayDeque<byte[]> spare = new ArrayDeque<>();
	private final Thread writer;
	private volatile boolean closed;
	private long grabbed, written, dropped, resizing, startNanos;
	/** Of the dropped: the frame not the film's size, every read-back buffer busy, the writer behind. */
	private long droppedSize, droppedBusy, droppedBehind;

	private Filming(Process process, String scene) {
		this(process, scene, 0, 0, null);
	}

	private Filming(Process process, String scene, int width, int height, OutputStream sink) {
		this.process = process;
		this.scene = scene;
		this.width = width;
		this.height = height;
		this.buffers = sink == null ? null : new GpuBuffer[BUFFERS];
		this.busy = new boolean[BUFFERS];
		this.queue = sink == null ? null : new ArrayBlockingQueue<>(QUEUE);
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
			RenderTarget target = client.gameRenderer.mainRenderTarget();
			int w = target.width, h = target.height;
			if (!doubled) {
				w *= SCALE;
				h *= SCALE;
				drawAt(client, w, h);
			}
			String filter = doubled ? "vflip,scale=" + SCALE + "*iw:" + SCALE + "*ih:flags=neighbor,fps=" + OUT_FPS : "vflip,fps=" + OUT_FPS;
			Process process = new ProcessBuilder(ffmpeg(), "-hide_banner", "-loglevel", "error", "-y",
					"-f", "rawvideo", "-pix_fmt", "rgba", "-s", w + "x" + h, "-r", Integer.toString(TICK_FPS), "-i", "-",
					"-vf", filter, "-c:v", "libx264", "-preset", "veryfast", "-crf", "14", "-pix_fmt", "yuv420p",
					out.getPath()).redirectErrorStream(true).redirectOutput(new File(folder, scene + ".log")).start();
			Filming film = new Filming(process, scene, w, h, new BufferedOutputStream(process.getOutputStream(), 1 << 20));
			film.client = client;
			if (!hooked) {
				hooked = true;
				ClientTickEvents.START_CLIENT_TICK.register(Filming::tick);
			}
			film.startNanos = System.nanoTime();
			film.writer.start();
			active = film;
			System.out.println(TAG + "filming the game's frames: " + scene + " drawn at " + w + "x" + h + (doubled ? ", doubled" : "") + ", "
					+ TICK_FPS + " frames a second of game time, to " + out);
			return film;
		} catch (IOException ex) {
			restore();
			System.out.println(TAG + "cannot film " + scene + ": " + ex.getMessage());
			return new Filming(null, scene);
		}
	}

	/**
	 * On the client thread: the (virtual) window set to {@code w x h} for the take, the GUI scale doubled with it; what to put back is
	 * kept in {@link #RESTORE}. Each is put back only if the scene has not changed it since.
	 */
	private static void drawAt(Minecraft client, int w, int h) {
		var window = client.getWindow();
		int oldW = window.getWidth(), oldH = window.getHeight();
		var option = client.options.guiScale();
		int oldGui = option.get();
		((WindowHooks) (Object) window).fabric_resize(w, h);
		int gui = oldGui == 0 ? 0 : oldGui * SCALE;
		option.set(gui);
		client.resizeGui();
		RESTORE.set(() -> {
			if (option.get() == gui) {
				option.set(oldGui);
			}
			if (window.getWidth() == w && window.getHeight() == h) {
				((WindowHooks) (Object) window).fabric_resize(oldW, oldH);
			}
			client.resizeGui();
		});
	}

	/** On the client thread: the window and GUI scale of the last native take put back, if not done yet. */
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

	/** On the client thread, at the start of each tick: the frame just drawn, copied on the GPU into a free read-back buffer. */
	private static void tick(Minecraft client) {
		Filming film = active;
		if (film != null && !film.closed) {
			film.grab(client);
		}
	}

	private void grab(Minecraft client) {
		RenderTarget target = client.gameRenderer.mainRenderTarget();
		GpuTexture texture = target.getColorTexture();
		if (texture == null || target.width != width || target.height != height) {
			if (written == 0 && grabbed == 0) {
				resizing++;   // the first tick, still the frame drawn before the window was set to the film's size
			} else {
				dropped++;
				droppedSize++;
			}
			return;
		}
		int free = -1;
		for (int i = 0; i < BUFFERS; i++) {
			if (!busy[i]) {
				free = i;
				break;
			}
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
					if (!queue.offer(frame)) {
						dropped++;   // the writer is behind
						droppedBehind++;
					}
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
					if (spare.size() < QUEUE) {
						spare.addLast(frame);
					}
				}
			}
		} catch (IOException | InterruptedException ex) {
			System.out.println(TAG + "filming " + scene + ": the encoder stopped: " + ex.getMessage());
		}
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
		closed = true;
		active = null;
		try {
			// any copy still on the GPU is let go (the films end on a still); its buffer is closed when it comes back
			queue.put(END);
			writer.join(60_000);
			if (!process.waitFor(120, TimeUnit.SECONDS)) {
				process.destroy();
			}
			double clock = (System.nanoTime() - startNanos) / 1e9;
			System.out.println(String.format(Locale.ROOT,
					"%sfilmed %s at %dx%d: %d frames written (%.1f s of game time), %d dropped (%d wrong size, %d read-backs busy, %d writer behind), %d skipped while the window was set, over %.1f s by the clock: %.1f frames a second",
					TAG, scene, width, height, written, written / (double) TICK_FPS, dropped, droppedSize, droppedBusy, droppedBehind, resizing, clock, written / clock));
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
