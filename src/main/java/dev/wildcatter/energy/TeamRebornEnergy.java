package dev.wildcatter.energy;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import team.reborn.energy.api.EnergyStorage;

/**
 * Plugs into Team Reborn Energy, the energy API of Tech Reborn and other Fabric mods: a storage beside a plug adapter, found on the
 * face toward the plug, takes the plug's power at 1 W = 1 E/t by default (the same setting as FE).
 */
public final class TeamRebornEnergy {
	private TeamRebornEnergy() {
	}

	public static void register() {
		EnergyBridge.register(target -> {
			EnergyStorage storage = EnergyStorage.SIDED.find(target.level(), target.pos(), target.side());
			return storage != null && storage.supportsInsertion() ? new Sink(storage) : null;
		});
	}

	private record Sink(EnergyStorage storage) implements HostEnergy {
		@Override
		public long demand() {
			if (Transaction.isOpen()) {
				return 0;
			}
			try (Transaction simulation = Transaction.openOuter()) {
				return storage.insert(Long.MAX_VALUE, simulation);   // never committed: only asks what it would take this tick
			}
		}

		@Override
		public long insert(long amount) {
			if (amount <= 0 || Transaction.isOpen()) {
				return 0;
			}
			try (Transaction transaction = Transaction.openOuter()) {
				long taken = storage.insert(amount, transaction);
				transaction.commit();
				return taken;
			}
		}

		@Override
		public String unit() {
			return "E";
		}
	}
}
