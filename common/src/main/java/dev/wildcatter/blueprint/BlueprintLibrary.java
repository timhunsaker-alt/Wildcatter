package dev.wildcatter.blueprint;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The library of the mod's plants (docs/blueprints.md, "The library"): JSON in the jar's {@code assets/wildcatter/blueprints}, made
 * from the game test scenes, read the same way on the server and the client. No Minecraft imports.
 */
public final class BlueprintLibrary {
	private BlueprintLibrary() {
	}

	/** Each plant's file name and its title, in the order a blueprint steps through them. */
	public static final Map<String, String> PLANTS = plants();

	private static Map<String, String> plants() {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("steam_plant", "Steam plant");
		m.put("genset", "Diesel genset");
		m.put("combined_cycle", "Gas turbine and combined cycle");
		m.put("aeroderivative", "Aeroderivative peaker");
		m.put("substation", "Distribution substation");
		m.put("land_rig", "Land rig");
		m.put("lease", "Oil lease");
		m.put("gas_plant", "Gas plant");
		m.put("crude_unit", "Crude unit");
		m.put("cracker", "Cracker and polyethylene");
		m.put("acid_plant", "Sulfuric acid plant");
		m.put("uranium_mill", "Uranium mill");
		m.put("conversion", "Conversion to UF6");
		m.put("enrichment", "Centrifuge cascade");
		m.put("zirconium", "Zirconium plant");
		m.put("fuel_plant", "Fuel plant");
		m.put("boron", "Boron plant");
		m.put("pwr", "PWR nuclear island");
		m.put("turbine_island", "Turbine island");
		m.put("cooling_tower", "Natural draft cooling tower");
		m.put("bayer_refinery", "Bayer refinery");
		m.put("alumina_plant", "Lime, AlF3 and chlorine");
		m.put("coker", "Delayed coker and calciner");
		m.put("coke_ovens", "Coke ovens and tar still");
		m.put("anode_plant", "Anode plant");
		m.put("potline", "Potline");
		return java.util.Collections.unmodifiableMap(m);
	}

	public static List<String> names() {
		return List.copyOf(PLANTS.keySet());
	}

	public static String title(String name) {
		return PLANTS.getOrDefault(name, name);
	}

	private static final Map<String, Blueprint> CACHE = new ConcurrentHashMap<>();

	/** The library's plant {@code name}, or null if it is not in the jar. */
	public static Blueprint load(String name) {
		Blueprint cached = CACHE.get(name);
		if (cached != null) {
			return cached;
		}
		try (InputStream in = BlueprintLibrary.class.getResourceAsStream("/assets/wildcatter/blueprints/" + name + ".json")) {
			if (in == null) {
				return null;
			}
			Blueprint b = Blueprint.fromJson(new String(in.readAllBytes(), StandardCharsets.UTF_8));
			CACHE.put(name, b);
			return b;
		} catch (IOException e) {
			return null;
		}
	}

	/** A blueprint item's data: "lib:<name>" for the library, else a surveyed blueprint's compressed form. */
	public static Blueprint of(String data) {
		if (data == null || data.isEmpty()) {
			return null;
		}
		if (data.startsWith("lib:")) {
			return load(data.substring(4));
		}
		Blueprint cached = CACHE.get(data);
		if (cached != null) {
			return cached;
		}
		try {
			Blueprint b = Blueprint.decompress(data);
			if (CACHE.size() > 64) {
				CACHE.keySet().removeIf(k -> !PLANTS.containsKey(k));
			}
			CACHE.put(data, b);
			return b;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
