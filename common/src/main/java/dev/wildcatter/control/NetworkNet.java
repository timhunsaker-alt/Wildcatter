package dev.wildcatter.control;

import dev.wildcatter.block.PipeBlock;
import dev.wildcatter.block.Plumbing;
import dev.wildcatter.multiblock.BigParts;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** What a run of network cable joins: cabinets with a COMM module, HMIs, remote drops. No traffic or timing is modelled (docs/multiblocks/plc.md). */
public final class NetworkNet {
	private NetworkNet() {
	}

	private static <T> List<T> on(Level level, Collection<BlockPos> cells, Class<T> kind) {
		Set<T> out = new LinkedHashSet<>();
		for (BlockPos cable : Plumbing.network(level, cells, PipeBlock.Kind.NETWORK)) {
			for (Direction side : Direction.values()) {
				BlockEntity entity = BigParts.entity(level, cable.relative(side));
				if (kind.isInstance(entity)) {
					out.add(kind.cast(entity));
				}
			}
		}
		return new ArrayList<>(out);
	}

	/** The PLCs on the network touching {@code cells}, each with a COMM module seated. */
	public static List<PlcCabinetBlockEntity> plcs(Level level, Collection<BlockPos> cells) {
		List<PlcCabinetBlockEntity> out = new ArrayList<>();
		for (PlcCabinetBlockEntity cabinet : on(level, cells, PlcCabinetBlockEntity.class)) {
			if (cabinet.hasComm()) {
				out.add(cabinet);
			}
		}
		return out;
	}

	/** The remote I/O drops on the network touching {@code cells}. */
	public static <T> List<T> all(Level level, Collection<BlockPos> cells, Class<T> kind) {
		return on(level, cells, kind);
	}

	/** A tag's value from whichever PLC on the network has it; a name with a station, {@code PLC-2/MAIN.X}, asks that one. Null if none does. */
	public static Object read(List<PlcCabinetBlockEntity> plcs, String tag) {
		int slash = tag.indexOf('/');
		for (PlcCabinetBlockEntity plc : plcs) {
			if (plc.cpu() != null && (slash < 0 || plc.station().equalsIgnoreCase(tag.substring(0, slash)))) {
				Object value = plc.cpu().read(tag.substring(slash + 1).toUpperCase(java.util.Locale.ROOT));
				if (value != null) {
					return value;
				}
			}
		}
		return null;
	}

	/** Writes a tag on whichever PLC has it; true if one took it. */
	public static boolean write(List<PlcCabinetBlockEntity> plcs, String tag, Object value) {
		int slash = tag.indexOf('/');
		String name = tag.substring(slash + 1).toUpperCase(java.util.Locale.ROOT);
		for (PlcCabinetBlockEntity plc : plcs) {
			if (plc.cpu() != null && (slash < 0 || plc.station().equalsIgnoreCase(tag.substring(0, slash))) && plc.cpu().read(name) != null) {
				return plc.cpu().write(name, value);
			}
		}
		return false;
	}
}
