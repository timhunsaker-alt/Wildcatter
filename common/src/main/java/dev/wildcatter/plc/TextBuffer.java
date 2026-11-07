package dev.wildcatter.plc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** The code editor's text, caret, selection and undo, in plain Java so it can be tested without a screen. */
public final class TextBuffer {
	private static final int MOST_UNDO = 200;
	private static final String INDENT = "\t";

	private record Snapshot(String text, int line, int column) {
	}

	private final List<StringBuilder> lines = new ArrayList<>();
	private final Deque<Snapshot> undo = new ArrayDeque<>();
	private final Deque<Snapshot> redo = new ArrayDeque<>();
	private int line;
	private int column;
	private int anchorLine = -1;
	private int anchorColumn;

	public TextBuffer(String text) {
		setText(text);
	}

	public void setText(String text) {
		lines.clear();
		for (String row : text.split("\n", -1)) {
			lines.add(new StringBuilder(row));
		}
		line = 0;
		column = 0;
		anchorLine = -1;
	}

	public String text() {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < lines.size(); i++) {
			out.append(i > 0 ? "\n" : "").append(lines.get(i));
		}
		return out.toString();
	}

	public int lineCount() {
		return lines.size();
	}

	public String line(int at) {
		return lines.get(at).toString();
	}

	public int caretLine() {
		return line;
	}

	public int caretColumn() {
		return column;
	}

	public boolean hasSelection() {
		return anchorLine >= 0 && (anchorLine != line || anchorColumn != column);
	}

	/** The selection as {from line, from column, to line, to column}, or null. */
	public int[] selection() {
		if (!hasSelection()) {
			return null;
		}
		boolean forward = anchorLine < line || (anchorLine == line && anchorColumn < column);
		return forward ? new int[] {anchorLine, anchorColumn, line, column} : new int[] {line, column, anchorLine, anchorColumn};
	}

	public String selectedText() {
		int[] s = selection();
		if (s == null) {
			return "";
		}
		StringBuilder out = new StringBuilder();
		for (int i = s[0]; i <= s[2]; i++) {
			String row = line(i);
			out.append(row, i == s[0] ? s[1] : 0, i == s[2] ? s[3] : row.length());
			if (i < s[2]) {
				out.append('\n');
			}
		}
		return out.toString();
	}

	private void snapshot() {
		undo.push(new Snapshot(text(), line, column));
		redo.clear();
		while (undo.size() > MOST_UNDO) {
			undo.removeLast();
		}
	}

	public void undo() {
		restore(undo, redo);
	}

	public void redo() {
		restore(redo, undo);
	}

	private void restore(Deque<Snapshot> from, Deque<Snapshot> to) {
		if (from.isEmpty()) {
			return;
		}
		to.push(new Snapshot(text(), line, column));
		Snapshot was = from.pop();
		setText(was.text());
		line = was.line();
		column = was.column();
	}

	private void deleteSelection() {
		int[] s = selection();
		if (s == null) {
			anchorLine = -1;
			return;
		}
		String tail = line(s[2]).substring(s[3]);
		lines.get(s[0]).setLength(s[1]);
		lines.get(s[0]).append(tail);
		for (int i = s[2]; i > s[0]; i--) {
			lines.remove(i);
		}
		line = s[0];
		column = s[1];
		anchorLine = -1;
	}

	/** Types or pastes {@code text} over the selection. */
	public void insert(String text) {
		snapshot();
		deleteSelection();
		String[] parts = text.replace("\r", "").split("\n", -1);
		StringBuilder current = lines.get(line);
		String tail = current.substring(column);
		current.setLength(column);
		current.append(parts[0]);
		for (int i = 1; i < parts.length; i++) {
			line++;
			lines.add(line, new StringBuilder(parts[i]));
		}
		column = lines.get(line).length();
		lines.get(line).append(tail);
	}

	/** Enter: a new line, at the indent of this one. */
	public void newline() {
		String row = line(line);
		int indent = 0;
		while (indent < row.length() && row.charAt(indent) == '\t') {
			indent++;
		}
		insert("\n" + INDENT.repeat(indent));
	}

	public void backspace() {
		if (hasSelection()) {
			cutSelection();
			return;
		}
		if (column == 0 && line == 0) {
			return;
		}
		snapshot();
		if (column > 0) {
			lines.get(line).deleteCharAt(--column);
		} else {
			column = lines.get(line - 1).length();
			lines.get(line - 1).append(lines.get(line));
			lines.remove(line--);
		}
	}

	public void delete() {
		if (hasSelection()) {
			cutSelection();
			return;
		}
		if (column == lines.get(line).length() && line == lines.size() - 1) {
			return;
		}
		snapshot();
		if (column < lines.get(line).length()) {
			lines.get(line).deleteCharAt(column);
		} else {
			lines.get(line).append(lines.get(line + 1));
			lines.remove(line + 1);
		}
	}

	/** Deletes the selection, as a step of its own. */
	public void cutSelection() {
		snapshot();
		deleteSelection();
	}

	public void selectAll() {
		anchorLine = 0;
		anchorColumn = 0;
		line = lines.size() - 1;
		column = lines.get(line).length();
	}

	/** Moves the caret, keeping the selection's other end when {@code extend}. */
	public void moveTo(int toLine, int toColumn, boolean extend) {
		if (extend && anchorLine < 0) {
			anchorLine = line;
			anchorColumn = column;
		} else if (!extend) {
			anchorLine = -1;
		}
		line = Math.max(0, Math.min(lines.size() - 1, toLine));
		column = Math.max(0, Math.min(lines.get(line).length(), toColumn));
	}

	public void left(boolean extend) {
		if (column > 0) {
			moveTo(line, column - 1, extend);
		} else if (line > 0) {
			moveTo(line - 1, lines.get(line - 1).length(), extend);
		}
	}

	public void right(boolean extend) {
		if (column < lines.get(line).length()) {
			moveTo(line, column + 1, extend);
		} else if (line < lines.size() - 1) {
			moveTo(line + 1, 0, extend);
		}
	}
}
