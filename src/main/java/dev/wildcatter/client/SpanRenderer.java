package dev.wildcatter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcatter.Tuning;
import dev.wildcatter.block.InsulatorBlock;
import dev.wildcatter.block.InsulatorBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/** Draws the overhead spans between insulators as sagging lines. The lower end of each span draws it. */
public class SpanRenderer implements BlockEntityRenderer<InsulatorBlockEntity, SpanRenderer.State> {
	public static class State extends BlockEntityRenderState {
		/** Per span: offset from this block to the other end, whether it is covered, its length, and each end's conductor height. */
		final List<double[]> spans = new ArrayList<>();
		int color = 0xFFC8322D;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(InsulatorBlockEntity entity, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay breakProgress) {
		BlockEntityRenderState.extractBase(entity, state, breakProgress);
		state.spans.clear();
		state.color = switch (entity.conductor()) {
			case A -> 0xFFC8322D;
			case B -> 0xFFE68C28;
			case C -> 0xFF325AC8;
			case N -> 0xFF2A2A2E;
			case G -> 0xFF28963F;
		};
		double here = InsulatorBlock.conductorY(entity.getBlockState());
		for (InsulatorBlockEntity.Span span : entity.spans()) {
			if (entity.getBlockPos().asLong() < span.other().asLong()) {
				double there = entity.getLevel() != null ? InsulatorBlock.conductorY(entity.getLevel().getBlockState(span.other())) : here;
				state.spans.add(new double[] {span.other().getX() - entity.getBlockPos().getX(), span.other().getY() - entity.getBlockPos().getY(),
						span.other().getZ() - entity.getBlockPos().getZ(), span.covered() ? 1 : 0, span.length(), here, there});
			}
		}
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.spans.isEmpty()) {
			return;
		}
		int color = state.color;
		collector.submitCustomGeometry(poseStack, RenderTypes.LINES, (pose, vertices) -> {
			for (double[] span : state.spans) {
				double dx = span[0];
				double dy = span[1];
				double dz = span[2];
				double sag = Tuning.SPAN_SAG * span[4];
				float width = span[3] > 0 ? 4.0F : 2.5F;
				int segments = Math.max(14, (int) (span[4] / 3));
				double px = 0.5;
				double py = span[5];
				double pz = 0.5;
				double rise = dy + span[6] - span[5];
				for (int i = 1; i <= segments; i++) {
					double t = i / (double) segments;
					double nx = 0.5 + dx * t;
					double ny = span[5] + rise * t - sag * 4.0 * t * (1 - t);
					double nz = 0.5 + dz * t;
					float lx = (float) (nx - px);
					float ly = (float) (ny - py);
					float lz = (float) (nz - pz);
					float len = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
					float ux = lx / len;
					float uy = ly / len;
					float uz = lz / len;
					vertices.addVertex(pose, (float) px, (float) py, (float) pz).setColor(color).setNormal(pose, ux, uy, uz).setLineWidth(width);
					vertices.addVertex(pose, (float) nx, (float) ny, (float) nz).setColor(color).setNormal(pose, ux, uy, uz).setLineWidth(width);
					px = nx;
					py = ny;
					pz = nz;
				}
			}
		});
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 320;
	}

	@Override
	public boolean shouldRender(InsulatorBlockEntity entity, Vec3 cameraPos) {
		return true;
	}
}
