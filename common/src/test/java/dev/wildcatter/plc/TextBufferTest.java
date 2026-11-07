package dev.wildcatter.plc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TextBufferTest {
	@Test
	void typingAndNewlinesKeepTheIndent() {
		TextBuffer text = new TextBuffer("");
		text.insert("\tIF a THEN");
		text.newline();
		text.insert("b;");
		assertEquals("\tIF a THEN\n\tb;", text.text());
		assertEquals(1, text.caretLine());
		assertEquals(3, text.caretColumn());
	}

	@Test
	void selectionReplacesAndCopies() {
		TextBuffer text = new TextBuffer("one\ntwo\nthree");
		text.moveTo(0, 1, false);
		text.moveTo(2, 2, true);
		assertEquals("ne\ntwo\nth", text.selectedText());
		text.insert("X");
		assertEquals("oXree", text.text());
		assertFalse(text.hasSelection());
	}

	@Test
	void backspaceAndDeleteJoinLines() {
		TextBuffer text = new TextBuffer("ab\ncd");
		text.moveTo(1, 0, false);
		text.backspace();
		assertEquals("abcd", text.text());
		assertEquals(2, text.caretColumn());
		text.delete();
		assertEquals("abd", text.text());
		text.moveTo(0, 0, false);
		text.backspace();
		assertEquals("abd", text.text(), "nothing before the start");
	}

	@Test
	void undoAndRedo() {
		TextBuffer text = new TextBuffer("a");
		text.moveTo(0, 1, false);
		text.insert("b");
		text.insert("c");
		text.undo();
		assertEquals("ab", text.text());
		text.undo();
		assertEquals("a", text.text());
		text.redo();
		assertEquals("ab", text.text());
		assertEquals(2, text.caretColumn());
	}

	@Test
	void pastingManyLines() {
		TextBuffer text = new TextBuffer("xy");
		text.moveTo(0, 1, false);
		text.insert("1\r\n2\n3");
		assertEquals("x1\n2\n3y", text.text());
		assertEquals(2, text.caretLine());
		assertEquals(1, text.caretColumn());
	}

	@Test
	void theCaretStaysInBoundsAndSelectAllWorks() {
		TextBuffer text = new TextBuffer("abc\nd");
		text.moveTo(99, 99, false);
		assertEquals(1, text.caretLine());
		assertEquals(1, text.caretColumn());
		text.left(false);
		text.left(false);
		assertEquals(0, text.caretLine());
		assertEquals(3, text.caretColumn());
		text.right(false);
		assertEquals(1, text.caretLine());
		text.selectAll();
		assertTrue(text.hasSelection());
		assertEquals("abc\nd", text.selectedText());
	}
}
