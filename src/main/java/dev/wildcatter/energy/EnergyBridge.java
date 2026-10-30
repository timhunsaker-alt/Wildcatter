package dev.wildcatter.energy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The one place the grid meets another mod's energy. Providers turn a block next to a plug adapter into a {@link HostEnergy}.
 * Team Reborn Energy support registers a provider here when it is available for this Minecraft version; until then nothing
 * is registered by default.
 */
public final class EnergyBridge {
	/** A provider looks at the block at a position and returns its energy handle, or null if it is not an energy block. */
	public interface Provider extends Function<Target, HostEnergy> {
	}

	public record Target(Level level, BlockPos pos) {
	}

	private static final List<Provider> PROVIDERS = new ArrayList<>();
	/** Watts of the grid that one FE per tick is worth. The default, 1 W = 1 FE/t, is set by the config. */
	private static volatile double wattsPerFe = 1.0;

	private EnergyBridge() {
	}

	public static void register(Provider provider) {
		PROVIDERS.add(provider);
	}

	public static void clear() {
		PROVIDERS.clear();
	}

	public static @Nullable HostEnergy find(Level level, BlockPos pos) {
		for (Provider provider : PROVIDERS) {
			HostEnergy found = provider.apply(new Target(level, pos));
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	public static double wattsPerFe() {
		return wattsPerFe;
	}

	public static void setWattsPerFe(double value) {
		wattsPerFe = value > 0 ? value : 1.0;
	}
}
