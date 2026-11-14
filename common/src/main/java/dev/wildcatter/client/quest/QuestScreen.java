package dev.wildcatter.client.quest;

import dev.wildcatter.quest.QuestBook;
import dev.wildcatter.quest.QuestBook.Chapter;
import dev.wildcatter.quest.QuestBook.Quest;
import dev.wildcatter.quest.QuestBook.Task;
import dev.wildcatter.quest.QuestClient;
import dev.wildcatter.quest.QuestClient.State;
import dev.wildcatter.quest.QuestNet;
import dev.wildcatter.quest.QuestTracker;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The quest screen (docs/quests.md, "The book"), ported from Farmings' manual screen: chapters and their quests (or search hits) on the
 * left page, the chosen quest on the right, split into sheets. Drawn as filled shapes, no texture.
 */
public class QuestScreen extends Screen {
	private int bookW = 300;
	private int bookH = 196;
	private int pageW = 128;
	static final int MARGIN = 10;
	static final int TEXT_TOP = 30;
	static final int TEXT_BOTTOM = 26;
	static final int ROW = 12;

	static final int PAPER = 0xFFF3E9D2;
	static final int EDGE = 0xFF3F5A3A;
	static final int SPINE = 0xFF6B4F2A;
	static final int INK = 0xFF3A2E1E;
	static final int FAINT = 0xFF9A8B6E;
	static final int LINK = 0xFF2457A6;
	static final int HEAD = 0xFF2F5D2A;
	static final int DONE = 0xFF2F7D2A;
	static final int MARK = 0xFFE2D3AE;
	static final int BUTTON = 0xFFB87333;
	static final int BUTTON_HOVER = 0xFFD08A48;

	/** Where the reader was, kept for the game session only. */
	private static Identifier lastQuest;
	private static final Set<Identifier> OPEN = new HashSet<>();

	private EditBox search;
	private Identifier questId;
	private int sheet;
	private int listSheet;
	private int seenVersion = -1;
	private final Deque<Identifier> back = new ArrayDeque<>();
	private List<List<Line>> sheets = List.of();
	private final List<Hit> hits = new ArrayList<>();
	private int left;
	private int top;

	/** Draws one laid-out line of the right page at (x, y). */
	interface Drawer {
		void draw(GuiGraphicsExtractor g, int x, int y, int mx, int my);
	}

	record Line(int height, Drawer drawer) {
	}

	record Hit(int x, int y, int w, int h, Runnable action) {
		boolean in(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	public QuestScreen() {
		super(Component.translatable("item.wildcatter.quest_book"));
	}

	/** The quest shown, for the client scene. */
	public Identifier questId() {
		return questId;
	}

	@Override
	protected void init() {
		bookW = Math.max(300, Math.min(460, width - 40));
		bookH = Math.max(196, Math.min(270, height - 30));
		pageW = bookW / 2 - 2 * MARGIN - 2;
		left = (width - bookW) / 2;
		top = (height - bookH) / 2;
		String query = search == null ? "" : search.getValue();
		search = new EditBox(font, left + MARGIN, top + MARGIN, pageW, 14, Component.translatable("gui.wildcatter.quest.search"));
		search.setHint(Component.translatable("gui.wildcatter.quest.search"));
		search.setMaxLength(40);
		search.setValue(query);
		search.setResponder(q -> listSheet = 0);
		addRenderableWidget(search);
		if (questId == null) {
			pickStart();
		}
		layout();
	}

	/** The last quest read, or the first quest marked next, or the first quest. */
	private void pickStart() {
		QuestBook book = QuestClient.book();
		if (lastQuest != null && book.quest(lastQuest) != null) {
			questId = lastQuest;
		} else {
			questId = null;
			for (Chapter c : book.chapters()) {
				for (Quest q : c.quests()) {
					if (questId == null && QuestClient.state(q) == State.NEXT) {
						questId = q.id();
					}
				}
			}
			if (questId == null && !book.isEmpty()) {
				questId = book.quests().keySet().iterator().next();
			}
		}
		if (questId != null) {
			OPEN.add(book.quest(questId).chapter());
		}
	}

	/** Opens a quest; {@code remember} pushes the current one on the Back list. */
	public boolean open(Identifier id, boolean remember) {
		Quest q = id == null ? null : QuestClient.book().quest(id);
		if (q == null) {
			return false;
		}
		if (remember && questId != null && !questId.equals(id)) {
			back.push(questId);
			while (back.size() > 32) {
				back.removeLast();
			}
		}
		questId = id;
		lastQuest = id;
		sheet = 0;
		OPEN.add(q.chapter());
		if (font != null) {
			layout();
		}
		return true;
	}

	public void search(String query) {
		search.setValue(query);
	}

	public void turn(int by) {
		sheet = Math.max(0, Math.min(sheets.size() - 1, sheet + by));
	}

	// ---- layout of the right page ----

	private void layout() {
		seenVersion = QuestClient.version();
		Quest q = questId == null ? null : QuestClient.book().quest(questId);
		List<Line> lines = new ArrayList<>();
		if (q != null) {
			for (String para : q.text().split("\n")) {
				wrap(lines, Component.literal(para), INK);
				lines.add(new Line(3, (g, x, y, mx, my) -> {
				}));
			}
			heading(lines, Component.translatable("gui.wildcatter.quest.tasks"));
			for (int i = 0; i < q.tasks().size(); i++) {
				task(lines, q, i);
			}
			if (!q.requires().isEmpty()) {
				heading(lines, Component.translatable("gui.wildcatter.quest.needs"));
				for (Identifier need : q.requires()) {
					Quest n = QuestClient.book().quest(need);
					Component label = Component.literal(n == null ? need.toString() : n.title());
					State s = n == null ? State.LATER : QuestClient.state(n);
					lines.add(new Line(ROW, (g, x, y, mx, my) -> {
						Component c = fit(Component.literal(mark(s) + " ").append(label).withStyle(Style.EMPTY.withUnderlined(n != null)), pageW);
						boolean hover = n != null && mx >= x && mx < x + font.width(c) && my >= y && my < y + 10;
						g.text(font, c, x, y + 1, hover ? LINK : color(s), false);
						if (n != null) {
							hits.add(new Hit(x, y, font.width(c), 10, () -> open(need, true)));
						}
					}));
				}
			}
			if (!q.rewards().isEmpty()) {
				heading(lines, Component.translatable("gui.wildcatter.quest.rewards"));
				lines.add(new Line(20, (g, x, y, mx, my) -> {
					int ix = x;
					for (QuestBook.Reward r : q.rewards()) {
						ItemStack st = stack(r.item(), r.count());
						g.item(st, ix, y);
						g.itemDecorations(font, st, ix, y);
						if (mx >= ix && mx < ix + 16 && my >= y && my < y + 16) {
							g.setTooltipForNextFrame(font, st, mx, my);
						}
						ix += 18;
					}
				}));
				lines.add(new Line(16, (g, x, y, mx, my) -> claimButton(g, q, x, y, mx, my)));
			}
		}
		int room = bookH - TEXT_TOP - TEXT_BOTTOM;
		List<List<Line>> out = new ArrayList<>();
		List<Line> cur = new ArrayList<>();
		int used = 0;
		for (Line l : lines) {
			if (used + l.height() > room && !cur.isEmpty()) {
				out.add(cur);
				cur = new ArrayList<>();
				used = 0;
			}
			cur.add(l);
			used += l.height();
		}
		if (!cur.isEmpty() || out.isEmpty()) {
			out.add(cur);
		}
		sheets = out;
		sheet = Math.max(0, Math.min(sheet, sheets.size() - 1));
	}

	private void wrap(List<Line> lines, Component text, int color) {
		for (FormattedCharSequence seq : font.split(FormattedText.of(text.getString()), pageW)) {
			lines.add(new Line(10, (g, x, y, mx, my) -> g.text(font, seq, x, y, color, false)));
		}
	}

	private void heading(List<Line> lines, Component text) {
		lines.add(new Line(4, (g, x, y, mx, my) -> {
		}));
		lines.add(new Line(11, (g, x, y, mx, my) -> g.text(font, text.copy().withStyle(Style.EMPTY.withBold(true)), x, y, HEAD, false)));
	}

	private void task(List<Line> lines, Quest q, int i) {
		Task t = q.tasks().get(i);
		ItemStack icon = switch (t.kind()) {
			case ITEM -> t.isTag() ? tagStack(t.target()) : stack(t.target(), 1);
			default -> stack(q.icon(), 1);
		};
		lines.add(new Line(18, (g, x, y, mx, my) -> {
			boolean met = QuestClient.met(q.id(), i);
			g.item(icon, x, y);
			if (mx >= x && mx < x + 16 && my >= y && my < y + 16 && t.kind() == QuestBook.Kind.ITEM) {
				g.setTooltipForNextFrame(font, icon, mx, my);
			}
			Component label = fit(taskLabel(t, icon), pageW - 32);
			g.text(font, label, x + 19, y + 4, met ? DONE : INK, false);
			g.text(font, met ? "✔" : "✘", x + pageW - 8, y + 4, met ? DONE : FAINT, false);
		}));
	}

	private Component taskLabel(Task t, ItemStack icon) {
		if (!t.text().isBlank()) {
			return Component.literal(t.text());
		}
		return switch (t.kind()) {
			case ITEM -> {
				int have = minecraft == null || minecraft.player == null ? 0 : QuestTracker.count(minecraft.player.getInventory(), t.target());
				Component name = t.isTag() ? Component.literal(t.target().substring(t.target().indexOf(':') + 1).replace('_', ' ')) : icon.getHoverName();
				Component base = Component.translatable(t.isTag() ? "gui.wildcatter.quest.have_tag" : "gui.wildcatter.quest.have", t.count(), name);
				yield t.count() > 1 ? base.copy().append(" (" + Math.min(have, t.count()) + "/" + t.count() + ")") : base;
			}
			case ADVANCEMENT -> Component.translatable("gui.wildcatter.quest.advancement", QuestClient.advancementTitle(t.target()));
			case MILESTONE -> Component.translatable("gui.wildcatter.quest.milestone", t.target());
		};
	}

	private void claimButton(GuiGraphicsExtractor g, Quest q, int x, int y, int mx, int my) {
		if (QuestClient.claimed(q.id())) {
			g.text(font, Component.translatable("gui.wildcatter.quest.claimed"), x, y + 3, FAINT, false);
			return;
		}
		if (!QuestClient.done(q.id())) {
			return;
		}
		Component label = Component.translatable("gui.wildcatter.quest.claim");
		int w = font.width(label) + 12;
		boolean hover = mx >= x && mx < x + w && my >= y && my < y + 14;
		g.fill(x, y, x + w, y + 14, hover ? BUTTON_HOVER : BUTTON);
		g.text(font, label, x + 6, y + 3, 0xFFFFFFFF, false);
		hits.add(new Hit(x, y, w, 14, () -> QuestNet.claim(q.id())));
	}

	static ItemStack stack(String id, int count) {
		Identifier key = Identifier.tryParse(id);
		Item item = key == null ? Items.BARRIER : BuiltInRegistries.ITEM.getValue(key);
		return new ItemStack(item == Items.AIR ? Items.BARRIER : item, count);
	}

	static ItemStack tagStack(String tag) {
		Identifier key = Identifier.tryParse(tag.substring(1));
		if (key == null) {
			return new ItemStack(Items.BARRIER);
		}
		return BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, key))
				.flatMap(set -> set.stream().findFirst())
				.map(h -> new ItemStack(h.value()))
				.orElse(new ItemStack(Items.BARRIER));
	}

	static String mark(State s) {
		return switch (s) {
			case DONE -> "✔";
			case NEXT -> "►";
			case LATER -> "·";
		};
	}

	static int color(State s) {
		return switch (s) {
			case DONE -> DONE;
			case NEXT -> LINK;
			case LATER -> FAINT;
		};
	}

	// ---- drawing ----

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
		if (seenVersion != QuestClient.version()) {
			if (questId == null || QuestClient.book().quest(questId) == null) {
				questId = null;
				pickStart();
			}
			layout();
		}
		hits.clear();
		g.fill(left - 4, top - 4, left + bookW + 4, top + bookH + 4, EDGE);
		g.fill(left, top, left + bookW, top + bookH, PAPER);
		g.fill(left + bookW / 2 - 2, top, left + bookW / 2 + 2, top + bookH, SPINE);
		contents(g, mx, my);
		rightPage(g, mx, my);
		super.extractRenderState(g, mx, my, partial);
	}

	/** A row of the left page: a chapter header or a quest. */
	private record Entry(Chapter chapter, Quest quest, Component label) {
	}

	private void contents(GuiGraphicsExtractor g, int mx, int my) {
		int x = left + MARGIN;
		int y0 = top + TEXT_TOP;
		int rows = (bookH - TEXT_TOP - TEXT_BOTTOM) / ROW;
		QuestBook book = QuestClient.book();
		List<Entry> entries = new ArrayList<>();
		String query = search.getValue().trim().toLowerCase(Locale.ROOT);
		if (book.isEmpty()) {
			entries.add(new Entry(null, null, Component.translatable("gui.wildcatter.quest.empty").withStyle(Style.EMPTY.withItalic(true))));
		} else if (!query.isEmpty()) {
			for (Quest q : book.quests().values()) {
				if (q.matches(query)) {
					entries.add(new Entry(null, q, Component.literal(q.title())));
				}
			}
			if (entries.isEmpty()) {
				entries.add(new Entry(null, null, Component.translatable("gui.wildcatter.quest.no_results").withStyle(Style.EMPTY.withItalic(true))));
			}
		} else {
			for (Chapter c : book.chapters()) {
				boolean open = OPEN.contains(c.id());
				long done = c.quests().stream().filter(q -> QuestClient.done(q.id())).count();
				entries.add(new Entry(c, null, Component.literal((open ? "▾ " : "▸ ") + c.title() + "  " + done + "/" + c.quests().size())
						.withStyle(Style.EMPTY.withBold(true))));
				if (open) {
					for (Quest q : c.quests()) {
						entries.add(new Entry(null, q, Component.literal(q.title())));
					}
				}
			}
		}
		int pages = Math.max(1, (entries.size() + rows - 1) / rows);
		listSheet = Math.min(listSheet, pages - 1);
		for (int i = 0; i < rows && listSheet * rows + i < entries.size(); i++) {
			Entry e = entries.get(listSheet * rows + i);
			int y = y0 + i * ROW;
			if (e.quest() != null) {
				Quest q = e.quest();
				State s = QuestClient.state(q);
				int indent = query.isEmpty() ? 6 : 0;
				if (q.id().equals(questId)) {
					g.fill(x - 2, y - 1, x + pageW + 2, y + ROW - 1, MARK);
				}
				ItemStack icon = stack(q.icon(), 1);
				g.pose().pushMatrix();
				g.pose().translate(x + indent, y);
				g.pose().scale(10 / 16f, 10 / 16f);
				g.item(icon, 0, 0);
				g.pose().popMatrix();
				Component label = fit(e.label().copy().withStyle(Style.EMPTY.withBold(s == State.NEXT)), pageW - indent - 22);
				boolean hover = mx >= x && mx < x + pageW && my >= y - 1 && my < y + ROW - 1;
				g.text(font, label, x + indent + 12, y + 1, hover ? LINK : color(s), false);
				g.text(font, mark(s), x + pageW - 6, y + 1, color(s), false);
				hits.add(new Hit(x, y - 1, pageW, ROW, () -> open(q.id(), true)));
			} else if (e.chapter() != null) {
				Chapter c = e.chapter();
				boolean hover = mx >= x && mx < x + pageW && my >= y - 1 && my < y + ROW - 1;
				g.text(font, fit(e.label(), pageW), x, y + 1, hover ? LINK : INK, false);
				hits.add(new Hit(x, y - 1, pageW, ROW, () -> {
					if (!OPEN.remove(c.id())) {
						OPEN.add(c.id());
					}
				}));
			} else {
				g.text(font, fit(e.label(), pageW), x, y + 1, FAINT, false);
			}
		}
		if (pages > 1) {
			int by = top + bookH - 18;
			arrow(g, mx, my, x, by, "‹", listSheet > 0, () -> listSheet--);
			g.centeredText(font, (listSheet + 1) + " / " + pages, x + pageW / 2, by, FAINT);
			arrow(g, mx, my, x + pageW - 8, by, "›", listSheet < pages - 1, () -> listSheet++);
		}
	}

	private void rightPage(GuiGraphicsExtractor g, int mx, int my) {
		int x0 = left + bookW / 2 + MARGIN;
		Quest q = questId == null ? null : QuestClient.book().quest(questId);
		if (q == null) {
			g.text(font, Component.translatable(QuestClient.book().isEmpty() ? "gui.wildcatter.quest.empty" : "gui.wildcatter.quest.pick"), x0, top + MARGIN + 4, FAINT, false);
			return;
		}
		State s = QuestClient.state(q);
		g.item(stack(q.icon(), 1), x0, top + MARGIN);
		g.text(font, fit(Component.literal(q.title()).withStyle(Style.EMPTY.withBold(true)), pageW - 20), x0 + 19, top + MARGIN + 1, HEAD, false);
		Component state = Component.translatable("gui.wildcatter.quest." + s.name().toLowerCase(Locale.ROOT));
		Chapter c = QuestClient.book().chapter(q.chapter());
		Component sub = c == null ? state : state.copy().append(" · " + c.title());
		g.text(font, fit(sub, pageW - 20), x0 + 19, top + MARGIN + 11, color(s), false);
		int y = top + TEXT_TOP;
		for (Line line : sheets.get(sheet)) {
			line.drawer().draw(g, x0, y, mx, my);
			y += line.height();
		}
		int by = top + bookH - 18;
		if (sheets.size() > 1) {
			arrow(g, mx, my, x0 + pageW - 58, by, "‹", sheet > 0, () -> turn(-1));
			g.centeredText(font, (sheet + 1) + " / " + sheets.size(), x0 + pageW - 29, by, FAINT);
			arrow(g, mx, my, x0 + pageW - 6, by, "›", sheet < sheets.size() - 1, () -> turn(1));
		}
		if (!back.isEmpty()) {
			Component label = Component.literal("‹ ").append(Component.translatable("gui.wildcatter.quest.back"));
			boolean hover = mx >= x0 && mx < x0 + font.width(label) && my >= by - 1 && my < by + 9;
			g.text(font, label, x0, by, hover ? LINK : FAINT, false);
			hits.add(new Hit(x0, by - 1, font.width(label), 10, () -> open(back.pop(), false)));
		}
	}

	/** The label, cut with an ellipsis to fit {@code w}. */
	private Component fit(Component label, int w) {
		if (font.width(label) <= w) {
			return label;
		}
		String text = label.getString();
		for (int n = text.length() - 1; n > 0; n--) {
			Component c = Component.literal(text.substring(0, n).stripTrailing() + "…").withStyle(label.getStyle());
			if (font.width(c) <= w) {
				return c;
			}
		}
		return label;
	}

	private void arrow(GuiGraphicsExtractor g, int mx, int my, int x, int y, String glyph, boolean active, Runnable action) {
		if (!active) {
			return;
		}
		boolean hover = mx >= x - 2 && mx < x + 10 && my >= y - 2 && my < y + 10;
		g.text(font, glyph, x, y, hover ? LINK : INK, false);
		hits.add(new Hit(x - 2, y - 2, 12, 12, action));
	}

	// ---- input ----

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		for (Hit h : List.copyOf(hits)) {
			if (h.in(event.x(), event.y())) {
				search.setFocused(false);
				h.action().run();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (!search.isFocused()) {
			if (minecraft.options.keyInventory.matches(event)) {
				onClose();
				return true;
			}
			if (event.key() == 262) {
				turn(1);
				return true;
			}
			if (event.key() == 263) {
				turn(-1);
				return true;
			}
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double dx, double dy) {
		if (mx > left + bookW / 2) {
			turn(dy < 0 ? 1 : -1);
		} else {
			listSheet = Math.max(0, listSheet + (dy < 0 ? 1 : -1));
		}
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
