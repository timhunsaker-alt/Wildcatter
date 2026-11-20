package dev.wildcatter.client;

import dev.wildcatter.NewWorldSetup;
import dev.wildcatter.TimeScale;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The World Rules screen, opened from the Create World screen (docs/time-scale.md): the time scale as five preset buttons, and every world
 * rule of Wildcatter and of the mods that register theirs ({@link NewWorldSetup}) as an on/off button, each starting at its new-world
 * default. The choices go to {@link NewWorldSetup} and into the world when it is made; Done goes back to the Create World screen.
 */
public class WorldRulesScreen extends Screen {
	private static final int TEXT = 0xFFFFFFFF;
	private static final int MUTED = 0xFFA0A0A0;
	private final Screen parent;
	private final List<Button> scaleButtons = new ArrayList<>();
	private int rulesTop;
	/** The rule buttons and the rows they stand in: the list scrolls when the rules of every mod are more than the window holds. */
	private final List<Button> ruleButtons = new ArrayList<>();
	private final List<Integer> ruleRows = new ArrayList<>();
	private int rows;
	private int step = 22;
	private int visibleRows;
	private int scroll;

	public WorldRulesScreen(Screen parent) {
		super(Component.literal("World Rules"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		scaleButtons.clear();
		int w = 44;
		int left = width / 2 - (TimeScale.PRESETS.length * (w + 4) - 4) / 2;
		for (int i = 0; i < TimeScale.PRESETS.length; i++) {
			int preset = TimeScale.PRESETS[i];
			Button b = addRenderableWidget(Button.builder(Component.literal(TimeScale.label(preset)), x -> {
				NewWorldSetup.chooseScale(preset);
				refreshScale();
			}).bounds(left + i * (w + 4), 42, w, 20).tooltip(Tooltip.create(Component.literal(preset == 1
					? "Real time: every wait takes as long as in a real plant."
					: "The long waits (reactor heat-up and cooldown, residence holds, cask cooling, paste cure) take 1/" + preset
							+ " of their real time. Rates, fuel and electrics stay real."))).build());
			scaleButtons.add(b);
		}
		refreshScale();

		List<NewWorldSetup.Entry> entries = NewWorldSetup.entries();
		rulesTop = 84;
		ruleButtons.clear();
		ruleRows.clear();
		rows = (entries.size() + 1) / 2;
		int room = height - rulesTop - 36;   // down to the Done button, with a gap
		step = Math.max(18, Math.min(22, room / Math.max(1, rows)));
		visibleRows = Math.max(1, Math.min(rows, room / step));
		scroll = Math.max(0, Math.min(scroll, rows - visibleRows));
		for (int i = 0; i < entries.size(); i++) {
			NewWorldSetup.Entry e = entries.get(i);
			int col = i < rows ? 0 : 1;
			int row = i < rows ? i : i - rows;
			int x = col == 0 ? width / 2 - 156 : width / 2 + 4;
			Button[] holder = new Button[1];
			holder[0] = addRenderableWidget(Button.builder(label(e), b -> {
				NewWorldSetup.choose(e.mod(), e.key(), !NewWorldSetup.rule(e.mod(), e.key(), e.newWorld()));
				holder[0].setMessage(label(e));
			}).bounds(x, rulesTop + row * step, 152, Math.min(20, step - 2)).tooltip(Tooltip.create(Component.literal(e.what()
					+ " (" + e.mod() + "; new worlds: " + (e.newWorld() ? "on" : "off") + ")"))).build());
			ruleButtons.add(holder[0]);
			ruleRows.add(row);
		}
		layoutRules();
		addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(width / 2 - 75, height - 26, 150, 20).build());
	}

	/** Places the rule buttons for the scroll: rows above or below the window are hidden. */
	private void layoutRules() {
		for (int i = 0; i < ruleButtons.size(); i++) {
			int row = ruleRows.get(i) - scroll;
			Button b = ruleButtons.get(i);
			b.visible = row >= 0 && row < visibleRows;
			b.setY(rulesTop + Math.max(0, row) * step);
		}
	}

	/** Scrolls the rule list by {@code by} rows; true if it moved (the client test calls it as the wheel does). */
	public boolean scrollRules(int by) {
		int next = Math.max(0, Math.min(rows - visibleRows, scroll + by));
		if (next == scroll) {
			return false;
		}
		scroll = next;
		layoutRules();
		return true;
	}

	/** Which rule rows show now, first and last (for the client test). */
	public int[] shownRows() {
		return new int[] {scroll, scroll + visibleRows - 1, rows};
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (scrollY != 0 && scrollRules(scrollY > 0 ? -1 : 1)) {
			return true;
		}
		return super.mouseScrolled(x, y, scrollX, scrollY);
	}

	private static Component label(NewWorldSetup.Entry e) {
		return Component.literal(e.key() + ": " + (NewWorldSetup.rule(e.mod(), e.key(), e.newWorld()) ? "ON" : "OFF"));
	}

	private void refreshScale() {
		int chosen = NewWorldSetup.scale();
		for (int i = 0; i < scaleButtons.size(); i++) {
			scaleButtons.get(i).active = TimeScale.PRESETS[i] != chosen;   // the chosen preset shows pressed
		}
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
		super.extractRenderState(graphics, mouseX, mouseY, partial);
		center(graphics, "World Rules", 12, TEXT);
		center(graphics, "Time scale: " + TimeScale.label(NewWorldSetup.scale()) + " (the long waits only)", 30, MUTED);
		center(graphics, "Rules (hover for what each does; change later with /wildcatter rule)", rulesTop - 12, MUTED);
		if (visibleRows < rows) {
			String more = (scroll > 0 ? "\u25B2 " : "  ") + "scroll for more: rows " + (scroll + 1) + "-" + (scroll + visibleRows) + " of " + rows
					+ (scroll + visibleRows < rows ? " \u25BC" : "  ");
			center(graphics, more, rulesTop + visibleRows * step + 2, MUTED);
		}
	}

	private void center(GuiGraphicsExtractor graphics, String text, int y, int colour) {
		graphics.text(font, text, (width - font.width(text)) / 2, y, colour, false);
	}
}
