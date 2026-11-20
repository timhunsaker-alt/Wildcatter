package dev.wildcatter.client;

import dev.wildcatter.control.PlcNet;
import dev.wildcatter.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * The remote choke panel's screen (docs/multiblocks/oilfield-follow-ons.md, 23b), in the ISA-101 manner of the HMI: the kill sheet, the
 * gauges, the choke's position, the pump, and the drill pipe pressure against its schedule as a chart. The choke lever is held: press
 * and hold OPEN or CLOSE (or the keys O and C) and the hydraulic choke moves 10 % a second.
 */
public class ChokePanelScreen extends Screen {
	private static final int BACK = 0xF0303438;
	private static final int PANEL = 0xFF3C4147;
	private static final int TEXT = 0xFFE4E6E8;
	private static final int MUTED = 0xFFA0A6AC;
	private static final int ALARM = 0xFFE05050;
	private static final int WARN = 0xFFE0B040;
	private static final int TRACE = 0xFF7FD0FF;
	private static final int SCHED = 0xFFB0B0B0;
	private static final int CASING = 0xFFE0B040;

	private final int x;
	private final int y;
	private final int z;
	private CompoundTag state;
	private int jog;
	private int openX, openY, closeX, closeY;

	private ChokePanelScreen(CompoundTag state) {
		super(Component.literal("Remote choke panel"));
		this.state = state;
		x = state.getIntOr("x", 0);
		y = state.getIntOr("y", 0);
		z = state.getIntOr("z", 0);
	}

	public static void fromServer(CompoundTag state) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.gui.screen() instanceof ChokePanelScreen open && open.x == state.getIntOr("x", 0) && open.y == state.getIntOr("y", 0) && open.z == state.getIntOr("z", 0)) {
			open.state = state;
		} else if (state.getBooleanOr("open", false)) {
			minecraft.gui.setScreen(new ChokePanelScreen(state));
		}
	}

	private void ask(String cmd, int value) {
		CompoundTag out = new CompoundTag();
		out.putInt("x", x);
		out.putInt("y", y);
		out.putInt("z", z);
		out.putString("chokeCmd", cmd);
		out.putInt("jog", value);
		Platform.INSTANCE.sendToServer(new PlcNet.Payload(out));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		if (jog != 0) {
			ask("jog", 0);
		}
		ask("close", 0);
		super.onClose();
	}

	@Override
	protected void init() {
		int bx = width - 132;
		addRenderableWidget(Button.builder(Component.literal("PUMP"), b -> ask("pump", 0)).bounds(bx, 30, 60, 18).build());
		addRenderableWidget(Button.builder(Component.literal("METHOD"), b -> ask("method", 0)).bounds(bx + 64, 30, 60, 18).build());
		openX = bx;
		openY = 56;
		closeX = bx + 64;
		closeY = 56;
	}

	private boolean in(double mx, double my, int bx, int by) {
		return mx >= bx && mx < bx + 60 && my >= by && my < by + 28;
	}

	private void setJog(int j) {
		if (j != jog) {
			jog = j;
			ask("jog", j);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
		if (in(event.x(), event.y(), openX, openY)) {
			setJog(1);
			return true;
		}
		if (in(event.x(), event.y(), closeX, closeY)) {
			setJog(-1);
			return true;
		}
		return super.mouseClicked(event, doubled);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		setJog(0);
		return super.mouseReleased(event);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		if (event.key() == 79) {
			setJog(1);
			return true;
		}
		if (event.key() == 67) {
			setJog(-1);
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean keyReleased(net.minecraft.client.input.KeyEvent event) {
		if (event.key() == 79 || event.key() == 67) {
			setJog(0);
			return true;
		}
		return super.keyReleased(event);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
		graphics.fill(0, 0, width, height, BACK);
	}

	private double d(String key) {
		return state.getDoubleOr(key, 0);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
		super.extractRenderState(graphics, mouseX, mouseY, partial);
		graphics.text(font, "REMOTE CHOKE PANEL", 8, 8, TEXT, false);
		String note = state.getStringOr("note", "");
		graphics.text(font, note, 8, 20, note.startsWith("GAS") || note.startsWith("NOT") ? ALARM : MUTED, false);
		boolean kill = state.getBooleanOr("kill", false);
		// the choke lever
		graphics.fill(openX, openY, openX + 60, openY + 28, jog > 0 ? 0xFF5A8A5A : PANEL);
		graphics.fill(closeX, closeY, closeX + 60, closeY + 28, jog < 0 ? 0xFF8A5A5A : PANEL);
		graphics.text(font, "OPEN (O)", openX + 8, openY + 10, TEXT, false);
		graphics.text(font, "CLOSE (C)", closeX + 6, closeY + 10, TEXT, false);
		int rx = width - 132;
		int ry = 92;
		graphics.text(font, state.getBooleanOr("powered", false) ? "HYDRAULICS OK" : "NO HYDRAULICS: manual choke", rx, ry, state.getBooleanOr("powered", false) ? MUTED : WARN, false);
		graphics.text(font, (state.getBooleanOr("choke", false) ? "manifold OK" : "NO CHOKE") + ", " + (state.getBooleanOr("separator", false) ? "separator OK" : "NO SEPARATOR"), rx, ry + 11,
				state.getBooleanOr("separator", false) ? MUTED : WARN, false);
		graphics.text(font, "METHOD: " + switch (state.getIntOr("method", 0)) {
			case 1 -> "DRILLER'S (1st)";
			case 2 -> "DRILLER'S (2nd)";
			default -> "WAIT AND WEIGHT";
		}, rx, ry + 22, MUTED, false);
		graphics.text(font, String.format("PUMP %s", state.getBooleanOr("pumpOn", false) ? "ON" : "OFF"), rx, ry + 33, MUTED, false);
		graphics.text(font, String.format("MUD %.2f ppg", d("mud")), rx, ry + 44, MUTED, false);
		graphics.text(font, String.format("PIT %+.2f m3", d("gain")), rx, ry + 55, MUTED, false);
		if (!kill) {
			graphics.text(font, "No kill under way. The panel works a shut-in well.", 8, 40, MUTED, false);
			return;
		}
		// the gauges
		int gx = 8, gy = 36;
		gauge(graphics, gx, gy, "DRILL PIPE", d("dpp"), "psi", TRACE);
		gauge(graphics, gx + 92, gy, "CASING", d("casing"), "psi", d("casing") > d("maasp") ? ALARM : CASING);
		gauge(graphics, gx + 184, gy, "CHOKE", d("chokePos") * 100, "%", TEXT);
		gauge(graphics, gx + 276, gy, "PUMP", d("spm"), "spm", TEXT);
		// the kill sheet
		int ky = gy + 44;
		graphics.text(font, String.format("KILL SHEET  SIDPP %.0f  SICP %.0f  KMW %.1f ppg  ICP %.0f  FCP %.0f psi  MAASP %.0f psi", d("sidpp"), d("sicp"), d("kmw"), d("icp"), d("fcp"), d("maasp")), 8, ky, TEXT, false);
		graphics.text(font, String.format("STROKES %.0f   to bit %d   to shoe %d   to surface %d   schedule %.0f psi   shoe %.1f / %.1f ppg", d("strokes"), state.getIntOr("toBit", 0),
				state.getIntOr("toShoe", 0), state.getIntOr("toSurface", 0), d("schedule"), d("shoeEmw"), d("fg")), 8, ky + 11, d("shoeEmw") > d("fg") ? ALARM : MUTED, false);
		// the chart: drill pipe pressure against its schedule, casing pressure
		int cx = 8, cy = ky + 28, cw = width - 152, ch = height - cy - 12;
		graphics.fill(cx, cy, cx + cw, cy + ch, PANEL);
		int[] dpp = state.getIntArray("hDpp").orElse(new int[0]);
		int[] sched = state.getIntArray("hSched").orElse(new int[0]);
		int[] cas = state.getIntArray("hCasing").orElse(new int[0]);
		double top = Math.max(50, Math.max(d("icp"), d("fcp")) * 1.3);
		for (int[] a : new int[][] {dpp, sched, cas}) {
			for (int v : a) {
				top = Math.max(top, v / 10.0 * 1.05);
			}
		}
		graphics.text(font, String.format("%.0f psi", top), cx + 2, cy + 2, MUTED, false);
		plot(graphics, sched, cx, cy, cw, ch, top, SCHED);
		plot(graphics, cas, cx, cy, cw, ch, top, CASING);
		plot(graphics, dpp, cx, cy, cw, ch, top, TRACE);
		graphics.text(font, "DRILL PIPE", cx + cw - 150, cy + 2, TRACE, false);
		graphics.text(font, "SCHEDULE", cx + cw - 96, cy + 2, SCHED, false);
		graphics.text(font, "CASING", cx + cw - 46, cy + 2, CASING, false);
	}

	private void gauge(GuiGraphicsExtractor graphics, int gx, int gy, String label, double value, String unit, int colour) {
		graphics.fill(gx, gy, gx + 88, gy + 38, PANEL);
		graphics.text(font, label, gx + 4, gy + 4, MUTED, false);
		graphics.text(font, String.format("%.0f %s", value, unit), gx + 4, gy + 20, colour, false);
	}

	private void plot(GuiGraphicsExtractor graphics, int[] values, int cx, int cy, int cw, int ch, double top, int colour) {
		int n = values.length;
		for (int i = 0; i < n; i++) {
			int px = cx + (int) ((double) i / Math.max(1, 239) * (cw - 2)) + 1;
			int py = cy + ch - 1 - (int) Math.min(ch - 2, values[i] / 10.0 / top * (ch - 2));
			graphics.fill(px, py, px + 2, py + 2, colour);
		}
	}
}
