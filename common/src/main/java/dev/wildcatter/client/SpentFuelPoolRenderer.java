package dev.wildcatter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.wildcatter.Tuning;
import dev.wildcatter.Wildcatter;
import dev.wildcatter.block.SpentFuelPoolBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the spent fuel pool's racks (docs/multiblocks/fuel-handling.md): a steel grid on the pool's floor and each irradiated assembly
 * standing in it, 214 mm square and 4 m tall. While an assembly's decay heat is high it glows Cherenkov blue.
 */
public class SpentFuelPoolRenderer implements BlockEntityRenderer<SpentFuelPoolBlockEntity, SpentFuelPoolRenderer.State> {
	public static class State extends BlockEntityRenderState {
		final List<double[]> assemblies = new ArrayList<>();   // {x, y, z, glow 0-1} from the block
		final List<double[]> racks = new ArrayList<>();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(SpentFuelPoolBlockEntity entity, State state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
		BlockEntityRenderState.extractBase(entity, state, breakProgress);
		state.assemblies.clear();
		state.racks.clear();
		double bx = entity.getBlockPos().getX(), by = entity.getBlockPos().getY(), bz = entity.getBlockPos().getZ();
		for (int i = 0; i < Tuning.POOL_RACKS; i++) {
			double[] c = entity.rackPoint(i);
			state.racks.add(new double[] {c[0] - bx, c[1] - 2 - by, c[2] - bz});
			if (i < entity.racks().size()) {
				double kw = entity.racks().get(i).watts() / 1000;
				state.assemblies.add(new double[] {c[0] - bx, c[1] - 2 - by, c[2] - bz, Math.min(1, kw / 20)});
			}
		}
	}

	private static TextureAtlasSprite sprite(String name) {
		return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(Wildcatter.id("block/mat/" + name));
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		TextureAtlasSprite steel = sprite("mat_stainless");
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (pose, v) -> {
			for (double[] r : state.racks) {
				box(pose, v, steel, r[0] - 0.45, r[1], r[2] - 0.45, r[0] + 0.45, r[1] + 0.15, r[2] + 0.45, 0xFFB0B0B0, state.lightCoords);   // the rack's cell
			}
			for (double[] a : state.assemblies) {
				int blue = (int) (255 * a[3]);
				int color = 0xFF000000 | (255 - blue) << 16 | (255 - blue / 3) << 8 | 255;
				int light = a[3] > 0.05 ? 0xF000F0 : state.lightCoords;   // Cherenkov light glows of itself
				box(pose, v, steel, a[0] - 0.107, a[1] + 0.1, a[2] - 0.107, a[0] + 0.107, a[1] + 4.16, a[2] + 0.107, color, light);
			}
		});
	}

	private static void box(PoseStack.Pose pose, VertexConsumer v, TextureAtlasSprite s, double x0, double y0, double z0, double x1, double y1, double z1, int color, int light) {
		double[][] faces = {
				{x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, 0, 0, -1},
				{x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, 0, 0, 1},
				{x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, -1, 0, 0},
				{x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1, 0, 0},
				{x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0}};
		float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
		for (double[] f : faces) {
			for (int k = 3; k >= 0; k--) {
				v.addVertex(pose, (float) f[k * 3], (float) f[k * 3 + 1], (float) f[k * 3 + 2]).setColor(color).setUv(s.getU(uv[k][0]), s.getV(uv[k][1]))
						.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, (float) f[12], (float) f[13], (float) f[14]);
			}
		}
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public boolean shouldRender(SpentFuelPoolBlockEntity entity, Vec3 cameraPos) {
		return true;
	}
}
