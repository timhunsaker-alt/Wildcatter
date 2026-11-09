package dev.wildcatter.block;

import dev.wildcatter.circuit.TimeCurrent;
import dev.wildcatter.circuit.TripCurve;
import dev.wildcatter.energy.ElectricalStandard;
import dev.wildcatter.multiblock.BigParts;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * The coordination check (protection gap wave): the device upstream of a breaker or fuse, found by following the conductors from its
 * line side to the first breaker, fuse or relay on them, and the study of the two ({@link TimeCurrent#study}). A gutter's breaker has
 * its panel's main upstream. The walk stops at anything else (a transformer, a source): the device upstream of a transformer is on
 * another voltage, and is not compared.
 */
public final class Coordination {
	private Coordination() {
	}

	private static final int MAX_CELLS = 512;

	/** The device upstream of {@code device}, or null. */
	public static @Nullable TripCurve upstream(Level level, Protective device) {
		if (device instanceof PanelGutterBlockEntity gutter) {
			PanelGutterBlockEntity.Place place = gutter.place();
			return place.panel();
		}
		BlockEntity self = (BlockEntity) device;
		ArrayDeque<BlockPos[]> queue = new ArrayDeque<>();   // {here, the cell it was reached from}
		Set<BlockPos> seen = new HashSet<>(BigParts.cellsAt(level, self.getBlockPos()));
		for (Protective.Terminal t : device.lineSide()) {
			BlockPos next = t.cell().relative(t.face());
			if (joined(level, t.cell(), t.face()) && seen.add(next)) {
				queue.add(new BlockPos[] {next, t.cell()});
			}
		}
		while (!queue.isEmpty() && seen.size() < MAX_CELLS) {
			BlockPos[] step = queue.poll();
			BlockPos here = step[0];
			if (!level.isLoaded(here)) {
				continue;
			}
			BlockEntity e = BigParts.entity(level, here);
			if (e == null || e == self) {
				continue;
			}
			if (e instanceof WireBlockEntity || e instanceof MultiCableBlockEntity) {
				for (Direction d : Direction.values()) {
					BlockPos next = here.relative(d);
					if (!seen.contains(next) && joined(level, here, d) && seen.add(next)) {
						queue.add(new BlockPos[] {next, here});
					}
				}
			} else if (e instanceof TripCurve curve && !(e instanceof PanelGutterBlockEntity g && g.breaker() == null) && !reachedByLine(e, here, step[1])) {
				return curve;
			}
		}
		return null;
	}

	/** True if the cell {@code at} and its neighbour across {@code face} meet on a phase conductor of some voltage. */
	private static boolean joined(Level level, BlockPos at, Direction face) {
		BlockPos next = at.relative(face);
		for (dev.wildcatter.energy.Voltage v : dev.wildcatter.energy.Voltage.values()) {
			for (dev.wildcatter.circuit.Conductor role : new dev.wildcatter.circuit.Conductor[] {dev.wildcatter.circuit.Conductor.A, dev.wildcatter.circuit.Conductor.B, dev.wildcatter.circuit.Conductor.C}) {
				if (dev.wildcatter.circuit.CircuitPart.portAt(level, at, face, role, v) && dev.wildcatter.circuit.CircuitPart.portAt(level, next, face.getOpposite(), role, v)) {
					return true;
				}
			}
		}
		return false;
	}

	/** True if the walk came to {@code e}'s cell {@code here} from {@code from} through one of its line terminals: a device beside this one on the same supply, not above it. */
	private static boolean reachedByLine(BlockEntity e, BlockPos here, BlockPos from) {
		if (!(e instanceof Protective p)) {
			return false;
		}
		for (Protective.Terminal t : p.lineSide()) {
			if (t.cell().equals(here) && t.cell().relative(t.face()).equals(from)) {
				return true;
			}
		}
		return false;
	}

	/** "; coordination: under the 200 A main, selective up to its 10 kA rating", or "" if nothing upstream is found. */
	public static String readout(@Nullable Level level, Protective device) {
		if (level == null || level.isClientSide()) {
			return "";
		}
		TripCurve up = upstream(level, device);
		if (up == null) {
			return "";
		}
		TimeCurrent.Study study = TimeCurrent.study(device.studyCurve(), up, device.ratingAmps(), device.interruptingAmps(), ElectricalStandard.active().hertz());
		return "; coordination: under the " + up.curveName() + ", " + TimeCurrent.describe(study, device.interruptingAmps());
	}
}
