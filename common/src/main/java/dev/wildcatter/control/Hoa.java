package dev.wildcatter.control;

/** A device's selector switch: HAND, its own buttons and logic; OFF, stopped; AUTO, the PLC commands it (docs/multiblocks/plc.md). */
public enum Hoa {
	HAND, OFF, AUTO;

	public Hoa next() {
		return values()[(ordinal() + 1) % values().length];
	}
}
