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

	/** The kg of molten aluminium in a tapping crucible (docs/multiblocks/potline.md). */
	public static final DataComponentType<Double> METAL_KG = DataComponentType.<Double>builder()
			.persistent(com.mojang.serialization.Codec.DOUBLE)
			.networkSynchronized(ByteBufCodecs.DOUBLE)
			.build();

	/** A blueprint's data: "lib:<name>" or a surveyed blueprint's compressed form (docs/blueprints.md). */
	public static final DataComponentType<String> BLUEPRINT = DataComponentType.<String>builder()
			.persistent(com.mojang.serialization.Codec.STRING)
			.networkSynchronized(ByteBufCodecs.STRING_UTF8)
			.build();
	/** The survey tool's two corners. */
	public static final DataComponentType<net.minecraft.core.BlockPos> SURVEY_A = DataComponentType.<net.minecraft.core.BlockPos>builder()
			.persistent(net.minecraft.core.BlockPos.CODEC)
			.networkSynchronized(net.minecraft.core.BlockPos.STREAM_CODEC)
			.build();
	public static final DataComponentType<net.minecraft.core.BlockPos> SURVEY_B = DataComponentType.<net.minecraft.core.BlockPos>builder()
			.persistent(net.minecraft.core.BlockPos.CODEC)
			.networkSynchronized(net.minecraft.core.BlockPos.STREAM_CODEC)
			.build();

	/** 16c1: the U-235 assay (a fraction) of a 30B of LEU or a fuel assembly made from it; none reads as the standard 4.95 %. */
	public static final DataComponentType<Double> ASSAY = DataComponentType.<Double>builder()
			.persistent(com.mojang.serialization.Codec.DOUBLE)
			.networkSynchronized(ByteBufCodecs.DOUBLE)
			.build();

	/** The assay a stack carries: its component, or the standard product assay. */
	public static double assay(net.minecraft.world.item.ItemStack stack) {
		Double a = stack.get(ASSAY);
		return a == null ? Tuning.ASSAY_PRODUCT : a;
	}

	/** Stamps a stack with an assay; the standard one is left unstamped, so it stacks and reads as every 30B before. */
	public static net.minecraft.world.item.ItemStack withAssay(net.minecraft.world.item.ItemStack stack, double assay) {
		if (Math.abs(assay - Tuning.ASSAY_PRODUCT) > 1e-5) {
			stack.set(ASSAY, Math.round(assay * 1e5) / 1e5);
		}
		return stack;
	}

	private WildcatterComponents() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Wildcatter.id("conductor_size"), CONDUCTOR_SIZE);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Wildcatter.id("metal_kg"), METAL_KG);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Wildcatter.id("blueprint"), BLUEPRINT);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Wildcatter.id("survey_a"), SURVEY_A);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Wildcatter.id("survey_b"), SURVEY_B);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Wildcatter.id("assay"), ASSAY);
	}
}
