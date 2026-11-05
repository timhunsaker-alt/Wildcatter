package dev.wildcatter.item;

import dev.wildcatter.energy.Voltage;
import net.minecraft.world.item.Item;

/**
 * A branch breaker that clips into a panel's gutter (docs/multiblocks/panelboards.md): one item per rating, as real ones are.
 * Its tier says which panel it fits: {@link Voltage#LOW} the load centre, {@link Voltage#INDUSTRIAL} the panelboard.
 */
public class BranchBreakerItem extends Item {
	private final Voltage tier;
	private final int amps;
	private final int poles;
	private final boolean gfci;

	public BranchBreakerItem(Properties properties, Voltage tier, int amps, int poles, boolean gfci) {
		super(properties);
		this.tier = tier;
		this.amps = amps;
		this.poles = poles;
		this.gfci = gfci;
	}

	public Voltage tier() {
		return tier;
	}

	public int amps() {
		return amps;
	}

	public int poles() {
		return poles;
	}

	public boolean gfci() {
		return gfci;
	}
}
