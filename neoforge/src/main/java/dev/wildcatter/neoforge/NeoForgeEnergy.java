package dev.wildcatter.neoforge;

import dev.wildcatter.energy.EnergyBridge;
import dev.wildcatter.energy.HostEnergy;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Plugs into NeoForge's energy (FE), which Mekanism and most NeoForge tech mods use: an energy handler beside a plug adapter, found on
 * the face toward the plug, takes the plug's power at 1 W = 1 FE/t by default.
 */
public final class NeoForgeEnergy {
	private NeoForgeEnergy() {
	}

	public static void register() {
		EnergyBridge.register(target -> {
			EnergyHandler handler = target.level().getCapability(Capabilities.Energy.BLOCK, target.pos(), target.side());
			return handler != null ? new Sink(handler) : null;
		});
	}

	private record Sink(EnergyHandler handler) implements HostEnergy {
		private static boolean busy() {
			return Transaction.getLifecycle() != Transaction.Lifecycle.NONE;
		}

		@Override
		public long demand() {
			if (busy()) {
				return 0;
			}
			try (Transaction simulation = Transaction.openRoot()) {
				return handler.insert(Integer.MAX_VALUE, simulation);   // never committed: only asks what it would take this tick
			}
		}

		@Override
		public long insert(long amount) {
			if (amount <= 0 || busy()) {
				return 0;
			}
			try (Transaction transaction = Transaction.openRoot()) {
				int taken = handler.insert((int) Math.min(amount, Integer.MAX_VALUE), transaction);
				transaction.commit();
				return taken;
			}
		}
	}
}
