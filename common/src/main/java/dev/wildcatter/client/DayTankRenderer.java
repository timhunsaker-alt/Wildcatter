package dev.wildcatter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildcatter.block.DayTankBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The day tank's sight glass, on each of its four sides: a steel-framed glass strip up the side, the water standing in it at the tank's
 * level, as a level gauge on a real tank shows it. Driven by the level the server sends (DayTankBlockEntity).
 */
public class DayTankRenderer implements BlockEntityRenderer<DayTankBlockEntity, DayTankRenderer.State> {
	/** The glass runs from just above the tank's base to just under its top, in block pixels. */
	private static final double BOTTOM = 3, TOP = 14;

	public static class State extends BlockEntityRenderState {
		double level;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DayTankBlockEntity tank, State state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(tank, state, crumbling);
		long now = System.nanoTime();
		tank.shownLevel = Boxes.ease(tank.shownLevel, Math.max(0, Math.min(1, tank.waterLevel())), tank.shownAt, now);
		tank.shownAt = now;
		state.level = tank.shownLevel;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		TextureAtlasSprite steel = Boxes.mat("mat_stainless");
		TextureAtlasSprite glass = Boxes.mat("mat_stainless");
		TextureAtlasSprite water = net.minecraft.client.Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS)
				.getSprite(net.minecraft.resources.Identifier.withDefaultNamespace("block/water_still"));
		double surface = BOTTOM + (TOP - BOTTOM) * state.level;
		for (int side = 0; side < 4; side++) {
			pose.pushPose();
			pose.translate(0.5F, 0.5F, 0.5F);
			pose.rotateDegrees(Axis.YP, side * 90.0F);
			pose.translate(-0.5F, -0.5F, -0.5F);
			collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (p, v) -> {
				// on the south face (the body's side is at z 15), to the right of its middle
				Boxes.px(p, v, steel, 10, BOTTOM - 1, 15, 15, BOTTOM, 15.7, 0xFFFFFFFF, state.lightCoords);         // the lower fitting
				Boxes.px(p, v, steel, 10, TOP, 15, 15, TOP + 1, 15.7, 0xFFFFFFFF, state.lightCoords);               // the upper fitting
				Boxes.px(p, v, steel, 10, BOTTOM, 15, 10.75, TOP, 15.7, 0xFFFFFFFF, state.lightCoords);             // the frame's sides
				Boxes.px(p, v, steel, 14.25, BOTTOM, 15, 15, TOP, 15.7, 0xFFFFFFFF, state.lightCoords);
				Boxes.px(p, v, glass, 10.75, BOTTOM, 15, 14.25, TOP, 15.45, 0xFF8C969E, state.lightCoords);         // the empty glass, pale
				if (surface > BOTTOM + 0.05) {
					Boxes.px(p, v, water, 10.75, BOTTOM, 15.45, 14.25, surface, 15.55, 0xFF2F7BEA, state.lightCoords);   // the water in it
					Boxes.px(p, v, glass, 10.75, surface - 0.3, 15.55, 14.25, surface, 15.6, 0xFFD8F0FF, state.lightCoords);   // its meniscus
				}
			});
			pose.popPose();
		}
	}
}
