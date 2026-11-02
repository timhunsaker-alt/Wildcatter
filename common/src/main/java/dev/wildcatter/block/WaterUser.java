package dev.wildcatter.block;

/** A machine a water pump on its pipes fills: a cracking furnace, an acid plant's boiler and absorber, a mill's SAG mill and thickener. */
public interface WaterUser {
	/** 0 to 1, for the pump's level switch. */
	double waterLevel();

	/** Takes up to {@code litres}; returns how much fitted. */
	int addWater(int litres);
}
