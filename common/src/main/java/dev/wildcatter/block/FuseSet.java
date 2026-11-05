package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.item.CartridgeFuseItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The three cartridge fuses of a fused switch, one per phase (docs/multiblocks/safety-switch.md, load-interrupter.md). Each clip is
 * empty, holds a good fuse, or holds a blown one. A good fuse heats on I²t over its rating, as the one-block fuse does, on its kind's
 * curve, and melts at once above its instant multiple; a blown fuse opens only its own phase.
 */
final class FuseSet {
	private static final String[] PHASES = {"A", "B", "C"};

	private final CartridgeFuseItem.Kind kind;
	/** Each clip's fuse rating; 0 for an empty clip. */
	private final int[] amps = new int[3];
	private final boolean[] blown = new boolean[3];
	private final double[] heat = new double[3];
	private final int[] links = {-1, -1, -1};
	private final double[] carrying = new double[3];

	/** The game test shrinks every rating by this factor so a small fault can blow a big fuse. Null in play. */
	static volatile Double testRatingScale;

	FuseSet(CartridgeFuseItem.Kind kind) {
		this.kind = kind;
	}

	CartridgeFuseItem.Kind kind() {
		return kind;
	}

	/** Fits a fuse into the first empty or blown clip; false if every clip holds a good fuse. A blown fuse comes out as nothing. */
	boolean fit(int rating) {
		for (int k = 0; k < 3; k++) {
			if (amps[k] == 0 || blown[k]) {
				amps[k] = rating;
				blown[k] = false;
				heat[k] = 0;
				return true;
			}
		}
		return false;
	}

	/** Pulls every fuse: the good ones come out as items, the blown ones as nothing. */
	List<ItemStack> pull() {
		List<ItemStack> out = new ArrayList<>();
		for (int k = 0; k < 3; k++) {
			if (amps[k] > 0 && !blown[k]) {
				CartridgeFuseItem item = WildcatterItems.cartridgeFuse(kind, amps[k]);
				if (item != null) {
					out.add(new ItemStack(item));
				}
			}
			amps[k] = 0;
			blown[k] = false;
			heat[k] = 0;
		}
		return out;
	}

	boolean empty() {
		return amps[0] == 0 && amps[1] == 0 && amps[2] == 0;
	}

	/** Phase {@code k}'s fuse is fitted and good. */
	boolean good(int k) {
		return amps[k] > 0 && !blown[k];
	}

	/** The fuse in phase {@code k}'s clip carries current from {@code a} to {@code b} if it is good; nothing if not. */
	void build(Circuit circuit, int k, int a, int b) {
		links[k] = good(k) ? circuit.resistor(a, b, Tuning.BREAKER_CLOSED_OHMS) : -1;
	}

	/** Clears the links: the switch is open, so no fuse carries anything. */
	void open() {
		links[0] = links[1] = links[2] = -1;
	}

	private double rating(int k) {
		return amps[k] * (testRatingScale != null ? testRatingScale : 1.0);
	}

	private double instantMultiple() {
		return kind == CartridgeFuseItem.Kind.CLASS_J ? Tuning.CLASS_J_INSTANT_MULTIPLE : Tuning.E_FUSE_INSTANT_MULTIPLE;
	}

	private double secondsAt2x(int k) {
		return kind == CartridgeFuseItem.Kind.CLASS_J ? Tuning.CLASS_J_SECONDS_AT_2X
				: amps[k] > 100 ? Tuning.E_FUSE_SECONDS_AT_2X_LARGE : Tuning.E_FUSE_SECONDS_AT_2X;
	}

	/** The instant melt: true if a fuse blew in this solve. */
	boolean protect(Circuit circuit) {
		boolean any = false;
		for (int k = 0; k < 3; k++) {
			if (links[k] >= 0 && circuit.current(links[k]) > rating(k) * instantMultiple()) {
				blow(k);
				any = true;
			}
		}
		return any;
	}

	/** The I²t heating: true if a fuse blew. */
	boolean apply(Circuit circuit, int ticks) {
		boolean any = false;
		for (int k = 0; k < 3; k++) {
			carrying[k] = links[k] >= 0 ? circuit.current(links[k]) : 0;
			if (amps[k] == 0 || blown[k]) {
				continue;
			}
			double ratio = carrying[k] / rating(k);
			if (ratio > 1.0) {
				heat[k] += (ratio * ratio - 1.0) / 3.0 * ticks / (20.0 * secondsAt2x(k));
			} else {
				heat[k] = Math.max(0, heat[k] - ticks / (20.0 * Tuning.BREAKER_COOL_SECONDS));
			}
			if (heat[k] >= 1.0) {
				blow(k);
				any = true;
			}
		}
		return any;
	}

	private void blow(int k) {
		blown[k] = true;
		heat[k] = 0;
		links[k] = -1;
		carrying[k] = 0;
	}

	/** The largest current a fuse carries. */
	double mostAmps() {
		return Math.max(carrying[0], Math.max(carrying[1], carrying[2]));
	}

	/** The largest rating fitted; 0 if none. */
	int largest() {
		return Math.max(amps[0], Math.max(amps[1], amps[2]));
	}

	/** The phases whose fuses are blown, as "A, C"; empty if none. */
	String blownPhases() {
		List<String> out = new ArrayList<>();
		for (int k = 0; k < 3; k++) {
			if (amps[k] > 0 && blown[k]) {
				out.add(PHASES[k]);
			}
		}
		return String.join(", ", out);
	}

	/** "fuses 60 A class J: A good, B BLOWN, C empty". */
	String describe() {
		StringBuilder text = new StringBuilder("fuses");
		int rating = largest();
		if (rating > 0) {
			text.append(' ').append(CartridgeFuseItem.label(kind, rating));
		}
		text.append(':');
		for (int k = 0; k < 3; k++) {
			text.append(k == 0 ? " " : ", ").append(PHASES[k]).append(' ')
					.append(amps[k] == 0 ? "empty" : blown[k] ? "BLOWN" : heat[k] > 0.05 ? "hot " + Math.round(heat[k] * 100) + "%" : "good");
			if (amps[k] > 0 && amps[k] != rating) {
				text.append(" (").append(CartridgeFuseItem.label(kind, amps[k])).append(')');
			}
		}
		return text.toString();
	}

	void save(ValueOutput output) {
		for (int k = 0; k < 3; k++) {
			output.putInt("fuse" + k, amps[k]);
			output.putBoolean("blown" + k, blown[k]);
			output.putDouble("fuseHeat" + k, heat[k]);
		}
	}

	void load(ValueInput input) {
		for (int k = 0; k < 3; k++) {
			amps[k] = Math.max(0, input.getIntOr("fuse" + k, 0));
			blown[k] = input.getBooleanOr("blown" + k, false);
			heat[k] = input.getDoubleOr("fuseHeat" + k, 0);
		}
	}
}
