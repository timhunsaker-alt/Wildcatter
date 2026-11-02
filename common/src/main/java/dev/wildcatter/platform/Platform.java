package dev.wildcatter.platform;

import java.nio.file.Path;
import java.util.ServiceLoader;

/** What common code needs from the mod loader it runs on. Each loader's jar provides one, found by {@link ServiceLoader}. */
public interface Platform {
	Platform INSTANCE = ServiceLoader.load(Platform.class, Platform.class.getClassLoader()).findFirst()
			.orElseThrow(() -> new IllegalStateException("no Wildcatter platform: run the Fabric or NeoForge jar"));

	/** The loader's name, for logs. */
	String loader();

	/** Where config files go. */
	Path configDir();

	boolean isModLoaded(String modId);
}
