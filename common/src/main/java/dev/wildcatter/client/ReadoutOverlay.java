package dev.wildcatter.client;

import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;

/**
 * The action bar's message (a machine's readout, a meter's reading), kept on the screen when it is long. The game draws it as one line
 * centred over the hotbar for three seconds, so a long readout runs off both edges and is gone before it is read (Built From Scratch ep 3).
 * A message wider than the screen is drawn here instead: wrapped onto as many centred lines as it needs, the last where the game's line
 * sits, over a dark band, and held long enough to read (two seconds and a fortieth of a second a character, at most twelve seconds), then
 * faded. A message that fits, or one the game animates (a record playing), is left to the game. Both loaders wrap the game's own overlay
 * message layer with {@link #draw}.
 */
public final class ReadoutOverlay {
	private static final int MARGIN = 12;
	private static final long BASE_MS = 2000, PER_CHAR_MS = 40, MAX_MS = 12000, FADE_MS = 500;
	private static Field message, time, animate;

	private static Component seen;
	private static int lastTime;
	private static Component held;
	private static long heldAt, holdMs;

	private ReadoutOverlay() {
	}

	private static Object field(Hud hud, String name) throws ReflectiveOperationException {
		Field f = switch (name) {
			case "overlayMessageString" -> message;
			case "overlayMessageTime" -> time;
			default -> animate;
		};
		if (f == null) {
			f = Hud.class.getDeclaredField(name);
			f.setAccessible(true);
			switch (name) {
				case "overlayMessageString" -> message = f;
				case "overlayMessageTime" -> time = f;
				default -> animate = f;
			}
		}
		return f.get(hud);
	}

	/** How long a message of {@code chars} characters stays up. */
	static long holdMs(int chars) {
		return Math.min(MAX_MS, BASE_MS + PER_CHAR_MS * chars);
	}

	/** The width a long message wraps at on a screen {@code guiWidth} wide. */
	static int wrapWidth(int guiWidth) {
		return Math.max(100, guiWidth - 2 * MARGIN);
	}

	/** Draws the overlay message: a long one wrapped and held here, anything else by the game ({@code vanilla}). */
	public static void draw(GuiGraphicsExtractor g, Runnable vanilla) {
		Minecraft mc = Minecraft.getInstance();
		Component cur;
		int t;
		boolean animated;
		try {
			cur = (Component) field(mc.gui.hud, "overlayMessageString");
			t = (Integer) field(mc.gui.hud, "overlayMessageTime");
			animated = (Boolean) field(mc.gui.hud, "animateOverlayMessageColor");
		} catch (ReflectiveOperationException | RuntimeException e) {
			vanilla.run();   // a game whose action bar is laid out differently: leave it alone
			return;
		}
		long now = Util.getMillis();
		if (cur != null && t > 0 && (cur != seen || t > lastTime)) {   // a new message (or the same one sent again)
			seen = cur;
			String text = cur.getString();
			if (!animated && mc.font.width(cur) > g.guiWidth() - 2 * MARGIN) {
				held = cur;
				heldAt = now;
				holdMs = holdMs(text.length());
			} else {
				held = null;
			}
		}
		lastTime = t;
		if (held == null || now - heldAt > holdMs) {
			held = null;
			vanilla.run();
			return;
		}
		long left = holdMs - (now - heldAt);
		int alpha = left < FADE_MS ? (int) (255 * left / FADE_MS) : 255;
		if (alpha < 8) {
			return;
		}
		Font font = mc.font;
		List<FormattedCharSequence> lines = font.split(held, wrapWidth(g.guiWidth()));
		int lh = font.lineHeight + 1;
		int bottom = g.guiHeight() - 68 + 5;   // the game's line: centred 68 above the bottom, 4 above its middle
		int top = bottom - lines.size() * lh;
		int w = 0;
		for (FormattedCharSequence l : lines) {
			w = Math.max(w, font.width(l));
		}
		int x0 = (g.guiWidth() - w) / 2;
		g.nextStratum();
		g.fill(x0 - 4, top - 2, x0 + w + 4, bottom + 1, ARGB.color((int) (alpha * 0.55), 0, 0, 0));
		for (int i = 0; i < lines.size(); i++) {
			g.text(font, lines.get(i), (g.guiWidth() - font.width(lines.get(i))) / 2, top + i * lh, ARGB.white(alpha), true);
		}
	}

	/** The lines a long message is drawn in now (empty when the game draws it, or nothing is up), for the client test. */
	public static List<String> shown(int guiWidth) {
		if (held == null || Util.getMillis() - heldAt > holdMs) {
			return List.of();
		}
		return Minecraft.getInstance().font.split(held, wrapWidth(guiWidth)).stream().map(l -> {
			StringBuilder b = new StringBuilder();
			l.accept((i, style, cp) -> {
				b.appendCodePoint(cp);
				return true;
			});
			return b.toString();
		}).toList();
	}
}
