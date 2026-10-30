package dev.wildcatter.energy;

import com.mojang.serialization.Codec;
import dev.wildcatter.Wildcatter;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The world's electrical standard, stored with the overworld. It is chosen the first time the world loads (from the
 * Create World screen if one set it, otherwise from {@code config/wildcatter.properties}) and never changes after that.
 */
public class StandardData extends SavedData {
	public static final Codec<StandardData> CODEC = Codec.STRING.xmap(StandardData::fromName, data -> data.standard.name());
	public static final SavedDataType<StandardData> TYPE = new SavedDataType<>(Wildcatter.id("electrical_standard"), StandardData::choose, CODEC, DataFixTypes.LEVEL);

	private final ElectricalStandard standard;

	private StandardData(ElectricalStandard standard) {
		this.standard = standard;
	}

	private static StandardData fromName(String name) {
		try {
			return new StandardData(ElectricalStandard.valueOf(name));
		} catch (IllegalArgumentException e) {
			return choose();
		}
	}

	/** A world that has no standard yet: use the Create World choice, else the config default. */
	private static StandardData choose() {
		ElectricalStandard picked = ElectricalStandard.pending();
		ElectricalStandard standard = picked != null ? picked : configDefault();
		ElectricalStandard.setPending(null);
		return new StandardData(standard);
	}

	private static ElectricalStandard configDefault() {
		java.nio.file.Path file = FabricLoader.getInstance().getConfigDir().resolve("wildcatter.properties");
		java.util.Properties props = new java.util.Properties();
		try {
			if (java.nio.file.Files.exists(file)) {
				try (java.io.InputStream in = java.nio.file.Files.newInputStream(file)) {
					props.load(in);
				}
			} else {
				java.nio.file.Files.writeString(file, "# Electrical standard for NEW worlds: NORTH_AMERICAN (60 Hz) or IEC (50 Hz). A world keeps the one it was created with.\nstandard=NORTH_AMERICAN\n");
			}
			return ElectricalStandard.valueOf(props.getProperty("standard", "NORTH_AMERICAN").trim());
		} catch (java.io.IOException | IllegalArgumentException e) {
			return ElectricalStandard.NORTH_AMERICAN;
		}
	}

	/** Reads (or fixes) the world's standard and makes it the active one. Call when the server has started. */
	public static ElectricalStandard apply(MinecraftServer server) {
		StandardData data = server.overworld().getDataStorage().computeIfAbsent(TYPE);
		data.setDirty();
		ElectricalStandard.setActive(data.standard);
		Wildcatter.LOGGER.info("Electrical standard: {} ({} Hz)", data.standard.displayName(), data.standard.hertz());
		return data.standard;
	}
}
