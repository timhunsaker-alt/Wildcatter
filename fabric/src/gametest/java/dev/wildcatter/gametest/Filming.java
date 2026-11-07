package dev.wildcatter.gametest;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import net.minecraft.client.Minecraft;

/**
 * Films a scene with macOS's screencapture, only the game window's rectangle, into {@code build/scene-videos/<scene>.mov} (the
 * Gradle run folder's parent). Off the Mac, or with no permission to record, the scene still runs, unfilmed.
 */
final class Filming {
	private final Process process;

	private Filming(Process process) {
		this.process = process;
	}

	static Filming start(Minecraft client, String scene) {
		if (!System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("mac")) {
			return new Filming(null);
		}
		try {
			File folder = new File("../../scene-videos").getCanonicalFile();
			folder.mkdirs();
			File out = new File(folder, scene + ".mov");
			out.delete();
			var window = client.getWindow();
			String rect = window.getX() + "," + window.getY() + "," + window.getScreenWidth() + "," + window.getScreenHeight();
			Process process = new ProcessBuilder("screencapture", "-x", "-v", "-R", rect, out.getPath()).redirectErrorStream(true)
					.redirectOutput(new File(folder, scene + ".log")).start();
			System.out.println("[wildcatter-test] filming " + scene + " at " + rect + " to " + out);
			return new Filming(process);
		} catch (IOException ex) {
			System.out.println("[wildcatter-test] cannot film " + scene + ": " + ex.getMessage());
			return new Filming(null);
		}
	}

	/** Any character on its input stops the recording and writes the file. */
	void stop() {
		if (process == null) {
			return;
		}
		try {
			OutputStream in = process.getOutputStream();
			in.write('\n');
			in.flush();
			if (!process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS)) {
				new ProcessBuilder("kill", "-INT", Long.toString(process.pid())).start().waitFor();
				process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);
			}
		} catch (IOException | InterruptedException ex) {
			System.out.println("[wildcatter-test] filming did not stop cleanly: " + ex.getMessage());
		}
	}
}
