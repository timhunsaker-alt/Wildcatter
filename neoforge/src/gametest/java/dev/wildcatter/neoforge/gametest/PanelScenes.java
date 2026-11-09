package dev.wildcatter.neoforge.gametest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;

/** NeoForge server scenes of the protection gap wave, group C: panels, GFCI on a shock, ATS, fuse let-through, coordination. Helpers in {@link GridSceneKit}. */
final class PanelScenes {
	private PanelScenes() {
	}

	/** Scene name to scene; each is registered as {@code wildcatter_gametest:<name>} with 3,000 ticks. */
	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> scenes = new LinkedHashMap<>();
		return scenes;
	}
}
