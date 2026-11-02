package dev.wildcatter;

import dev.wildcatter.energy.ConductorSize;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

/** Item and block-entity data the mod adds. */
public final class WildcatterComponents {
	/** The conductor size of a wire, cable or spool (docs/phase4-conductors.md). */
	public static final DataComponentType<ConductorSize> CONDUCTOR_SIZE = DataComponentType.<ConductorSize>builder()
			.persistent(ConductorSize.CODEC)
			.networkSynchronized(ByteBufCodecs.idMapper(i -> ConductorSize.values()[i], ConductorSize::ordinal))
			.build();

	private WildcatterComponents() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Wildcatter.id("conductor_size"), CONDUCTOR_SIZE);
	}
}
