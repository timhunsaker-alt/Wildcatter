package dev.wildcatter.neoforge.gametest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;

/** NeoForge server scenes of the protection gap wave, group B: 87T, 87G, the GSU's back-feed onto a bus fault, grounding clamps burning. Helpers in {@link GridSceneKit}. */
final class DifferentialScenes {
	private DifferentialScenes() {
	}

	/** Scene name to scene; each is registered as {@code wildcatter_gametest:<name>} with 3,000 ticks. */
	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> scenes = new LinkedHashMap<>();
		return scenes;
	}
}
