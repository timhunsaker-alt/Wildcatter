package dev.wildcatter.energy;

/**
 * A tank of energy in RF. No Minecraft types in here on purpose: this and {@link EnergyNode} are the whole
 * "energy API", so bridging to another mod's energy (Mekanism, NeoForge's FE) only means translating these calls.
 */
public final class EnergyBuffer {
	private final int capacity;
	private final int maxInsert;
	private final int maxExtract;
	private int stored;

	public EnergyBuffer(int capacity, int maxInsert, int maxExtract) {
		this.capacity = capacity;
		this.maxInsert = maxInsert;
		this.maxExtract = maxExtract;
	}

	public int stored() {
		return stored;
	}

	public int capacity() {
		return capacity;
	}

	public double fraction() {
		return capacity == 0 ? 0 : (double) stored / capacity;
	}

	/** True if this buffer only ever receives (a machine), so it should be filled no matter how full it is. */
	public boolean isSink() {
		return maxExtract == 0;
	}

	/** How much {@link #insert} would accept right now. */
	public int room() {
		return Math.min(maxInsert, capacity - stored);
	}

	/** How much {@link #extract} would give right now. */
	public int available() {
		return Math.min(maxExtract, stored);
	}

	/** Puts energy in (limited by the input rate and the room left). Returns how much went in. */
	public int insert(int amount) {
		int moved = Math.max(0, Math.min(amount, room()));
		stored += moved;
		return moved;
	}

	/** Takes energy out (limited by the output rate). Returns how much came out. */
	public int extract(int amount) {
		int moved = Math.max(0, Math.min(amount, available()));
		stored -= moved;
		return moved;
	}

	/** For the owner of this buffer to draw from it directly, ignoring the output rate (a machine using power). */
	public int consume(int amount) {
		int moved = Math.max(0, Math.min(amount, stored));
		stored -= moved;
		return moved;
	}

	/** For the owner of this buffer to add to it directly, ignoring the input rate (a generator making power). */
	public int generate(int amount) {
		int moved = Math.max(0, Math.min(amount, capacity - stored));
		stored += moved;
		return moved;
	}

	public void set(int value) {
		stored = Math.max(0, Math.min(value, capacity));
	}
}
