package dev.wildcatter;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

/** The mod's own sounds (the files are in assets/wildcatter/sounds). */
public final class WildcatterSounds {
	public static final SoundEvent TURBINE_HUM = make("turbine_hum");
	public static final SoundEvent BOILER_HISS = make("boiler_hiss");
	public static final SoundEvent BREAKER_TRIP = make("breaker_trip");
	public static final SoundEvent BREAKER_CLICK = make("breaker_click");
	public static final SoundEvent ZAP = make("zap");
	public static final SoundEvent STRUCTURE_FORMED = make("structure_formed");

	private WildcatterSounds() {
	}

	private static SoundEvent make(String name) {
		var id = Wildcatter.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	/** Touch this class early so the sounds are registered. */
	public static void register() {
	}
}
