package dev.wildcatter.plc;

/** A CPU module's size (docs/multiblocks/plc.md, proposed figures). */
public record CpuSpec(String name, int instructionsPerScan, int memorySlots, int inputBytes, int outputBytes, int markerBytes) {
	public static final CpuSpec SMALL = new CpuSpec("small CPU", 20_000, 16_384, 512, 512, 4_096);
	public static final CpuSpec LARGE = new CpuSpec("large CPU", 100_000, 262_144, 4_096, 4_096, 65_536);
}
