package dev.wildcatter.neoforge;

import dev.wildcatter.platform.Platform;
import java.nio.file.Path;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

public final class NeoForgePlatform implements Platform {
	@Override
	public String loader() {
		return "NeoForge";
	}

	@Override
	public Path configDir() {
		return FMLPaths.CONFIGDIR.get();
	}

	@Override
	public boolean isModLoaded(String modId) {
		return ModList.get().isLoaded(modId);
	}
}
