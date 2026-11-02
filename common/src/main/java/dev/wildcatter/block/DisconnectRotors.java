package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.kinetic.Kinetic.Rotor;
import dev.wildcatter.kinetic.RotorBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

/**
 * Where the true-size disconnects' moving parts turn (tools/models/disconnect_*.py and rotors.py), in their main cell's frame
 * for a yard facing north: x along the line, z across it.
 */
final class DisconnectRotors {
	private DisconnectRotors() {
	}

	/** 138 kV centre-break: phases 3 m apart; per phase two rotating stacks 1.4 m apart, their arms meeting in the middle. */
	static List<Rotor> hv() {
		RotorBlock rotor = (RotorBlock) WildcatterBlocks.ROTOR;
		List<Rotor> out = new ArrayList<>();
		for (float z : new float[] {1.5F, 4.5F, 7.5F}) {
			out.add(new Rotor(rotor.part(RotorBlock.Part.DISCONNECT_ARM_HV), new Vector3f(0.3F, 5.55F, z), Direction.Axis.Y, 1.0F));
			out.add(new Rotor(rotor.part(RotorBlock.Part.DISCONNECT_ARM_HV_BACK), new Vector3f(1.7F, 5.55F, z), Direction.Axis.Y, -1.0F));
		}
		return out;
	}

	/** 12.47 kV vertical-break: one blade per phase, 1 m apart, hinged at its west end; open, it stands up. */
	static List<Rotor> dist() {
		RotorBlock rotor = (RotorBlock) WildcatterBlocks.ROTOR;
		List<Rotor> out = new ArrayList<>();
		for (float z : new float[] {0.5F, 1.5F, 2.5F}) {
			out.add(new Rotor(rotor.part(RotorBlock.Part.DISCONNECT_BLADE_DIST), new Vector3f(0.15F, 4.3F, z), Direction.Axis.Z, -1.0F));
		}
		return out;
	}
}
