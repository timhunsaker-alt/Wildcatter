package dev.wildcatter.control;

import dev.wildcatter.plc.CpuSpec;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.item.Item;

/** A module for a PLC cabinet's rail (docs/multiblocks/plc.md). */
public class PlcModuleItem extends Item {
	public enum Type {
		CPU_SMALL(CpuSpec.SMALL), CPU_LARGE(CpuSpec.LARGE), DI16(null), DO16(null), AI8(null), AO4(null), RTD4(null), HSC2(null);

		public final @Nullable CpuSpec cpu;

		Type(@Nullable CpuSpec cpu) {
			this.cpu = cpu;
		}
	}

	private final Type type;

	public PlcModuleItem(Properties properties, Type type) {
		super(properties);
		this.type = type;
	}

	public Type type() {
		return type;
	}
}
