package dev.wildcatter.energy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The one place the grid meets another mod's energy. Providers turn a block next to a plug adapter into a {@link HostEnergy}.
 * Team Reborn Energy ({@link TeamRebornEnergy}) registers one at startup; the game test adds its own stand-ins apart from it.
 */
public final class EnergyBridge {
	/** A provider looks at the block at a position and returns its energy handle, or null if it is not an energy block. */
	public interface Provider extends Function<Target, HostEnergy> {
	}

	/** The block at {@code pos}, reached from the plug through its {@code side} face. */
	public record Target(Level level, BlockPos pos, Direction side) {
	}

	private static final List<Provider> PROVIDERS = new ArrayList<>();
	private static final List<Provider> TEST_PROVIDERS = new ArrayList<>();
	/** Watts of the grid that one FE per tick is worth. The default, 1 W = 1 FE/t, is set by the config. */
	private static volatile double wattsPerFe = 1.0;

	private EnergyBridge() {
	}

	public static void register(Provider provider) {
		PROVIDERS.add(provider);
	}

	/** For the game test: a stand-in machine, kept apart from the real providers. */
	public static void registerForTest(Provider provider) {
		TEST_PROVIDERS.add(provider);
	}

	public static void clearTests() {
		TEST_PROVIDERS.clear();
	}

	public static @Nullable HostEnergy find(Level level, BlockPos pos, Direction side) {
		for (Provider provider : TEST_PROVIDERS) {
			HostEnergy found = provider.apply(new Target(level, pos, side));
			if (found != null) {
				return found;
			}
		}
		for (Provider provider : PROVIDERS) {
			HostEnergy found = provider.apply(new Target(level, pos, side));
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
