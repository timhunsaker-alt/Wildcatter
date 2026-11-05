package dev.wildcatter.item;

import net.minecraft.world.item.Item;

/**
 * A cartridge fuse that clips into a fused switch: a class J time-delay fuse for the 480 V safety switch
 * (docs/multiblocks/safety-switch.md), or an E-rated current-limiting power fuse for the fused 5 kV load-interrupter
 * (docs/multiblocks/load-interrupter.md).
 */
public class CartridgeFuseItem extends Item {
	public enum Kind { CLASS_J, E_RATED }

	private final Kind kind;
	private final int amps;

	public CartridgeFuseItem(Properties properties, Kind kind, int amps) {
		super(properties);
		this.kind = kind;
		this.amps = amps;
	}

	public Kind kind() {
		return kind;
	}

	public int amps() {
		return amps;
	}

	/** How it reads: "60 A class J" or "50E". */
	public static String label(Kind kind, int amps) {
		return kind == Kind.CLASS_J ? amps + " A class J" : amps + "E";
	}
}
