package dev.wildcatter.energy;

import net.minecraft.network.chat.Component;

/** Something the Power Meter (and right-clicking) can read out. */
public interface Inspectable {
	Component inspect();
}
