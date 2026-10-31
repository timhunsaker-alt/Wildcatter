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
	// Phase 4 machine loops and one-shots (docs/kinetics-and-sound.md)
	public static final SoundEvent DIESEL_IDLE = make("diesel_idle");
	public static final SoundEvent DIESEL_LOAD = make("diesel_load");
	public static final SoundEvent GAS_ENGINE = make("gas_engine");
	public static final SoundEvent ALTERNATOR_WHINE = make("alternator_whine");
	public static final SoundEvent FAN = make("fan");
	public static final SoundEvent ENGINE_CRANK = make("engine_crank");
	public static final SoundEvent SYNC_BANG = make("sync_bang");
	public static final SoundEvent TURBINE_WHINE = make("turbine_whine");
	public static final SoundEvent BURNER_ROAR = make("burner_roar");
	public static final SoundEvent MOTOR_HUM = make("motor_hum");
	public static final SoundEvent DISCONNECT_ARC = make("disconnect_arc");
	public static final SoundEvent HV_BREAKER = make("hv_breaker");
	public static final SoundEvent FUSE_POP = make("fuse_pop");
	public static final SoundEvent FLASHOVER = make("flashover");

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
