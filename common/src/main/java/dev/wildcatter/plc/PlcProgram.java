package dev.wildcatter.plc;

import java.util.List;
import java.util.Map;

/** A compiled project, ready to download to a CPU. Immutable and plain data. */
public record PlcProgram(int[] code, int[] lines, long[] constants, String[] strings, long[] initMem, String[] initStr, boolean[] retain, int[] entries,
		String[] pouNames, List<Task> tasks, Map<String, Tag> tags) {

	/** A task and what it runs, in priority order. An interval of 0 runs every scan. */
	public record Task(String name, long intervalMs, int priority, List<Run> runs) {
	}

	/** One program instance: its POU's entry and the memory it lives at. */
	public record Run(String instance, int pou, int slot) {
	}
}
