package dev.wildcatter.plc;

/** One scalar a screen can read or write by name: a memory slot, or a direct address when {@code area} is I, Q or M. */
public record Tag(String path, Elementary type, int slot, char area, int byteAddress, int bit) {
}
