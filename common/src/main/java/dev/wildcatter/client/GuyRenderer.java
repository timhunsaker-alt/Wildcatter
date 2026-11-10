package dev.wildcatter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcatter.block.GuyAnchorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/** Draws a guy as a taut grey strand from its anchor's rod eye up to its clamp on the pole. */
public class GuyRenderer implements BlockEntityRenderer<GuyAnchorBlockEntity, GuyRenderer.State> {
	public static class State extends BlockEntityRenderState {
		/** Offset from the anchor to the clamp, or null with no guy. */
		double[] to;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(GuyAnchorBlockEntity entity, State state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
		BlockEntityRenderState.extractBase(entity, state, breakProgress);
		BlockPos attach = entity.attach();
		BlockPos here = entity.getBlockPos();
		state.to = attach == null ? null : new double[] {attach.getX() - here.getX(), attach.getY() - here.getY(), attach.getZ() - here.getZ()};
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.to == null) {
			return;
		}
		double[] to = state.to;
		// from the rod eye to the pole's side facing the anchor, 0.1 m off its centre line
		double lead = Math.hypot(to[0], to[2]);
		float ex = 0.5F, ey = 0.32F, ez = 0.5F;
		float px = (float) (0.5 + to[0] - (lead > 0 ? 0.1 * to[0] / lead : 0));
		float py = (float) (to[1] + 0.8);
		float pz = (float) (0.5 + to[2] - (lead > 0 ? 0.1 * to[2] / lead : 0));
		float lx = px - ex, ly = py - ey, lz = pz - ez;
		float len = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
		collector.submitCustomGeometry(poseStack, RenderTypes.LINES, (pose, vertices) -> {
			vertices.addVertex(pose, ex, ey, ez).setColor(0xFF9AA0A6).setNormal(pose, lx / len, ly / len, lz / len).setLineWidth(2.5F);
			vertices.addVertex(pose, px, py, pz).setColor(0xFF9AA0A6).setNormal(pose, lx / len, ly / len, lz / len).setLineWidth(2.5F);
		});
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 256;
	}

	@Override
	public boolean shouldRender(GuyAnchorBlockEntity entity, Vec3 cameraPos) {
		return true;
	}
}
