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
 * standing in it, 214 mm square and 4 m tall. While an assembly's decay heat is high it glows Cherenkov blue: the light is made in the
 * water round the fuel, so a deep electric blue is drawn additively round each hot assembly and rising off its top, brightest at the
 * hottest. A pool's cleanup skid keeps its water clear, so the pool's surface is drawn as clear water over the racks (the game's own
 * water face, about 70 % opaque, hid them): a water mask hides that face and a thinner tinted one is drawn in its place.
 */
public class SpentFuelPoolRenderer implements BlockEntityRenderer<SpentFuelPoolBlockEntity, SpentFuelPoolRenderer.State> {
	public static class State extends BlockEntityRenderState {
		final List<double[]> assemblies = new ArrayList<>();   // {x, y, z, glow 0-1} from the block
		final List<double[]> racks = new ArrayList<>();
		/** The pool's water: {x0, z0, x1, z1, surface y} relative to the block, or null with no water. */
		double @org.jspecify.annotations.Nullable [] surface;
		int waterColor = 0x3F76E4;
	}

	/** The glow's shells round an assembly: {half width in m, red, green, blue, alpha at full glow 0-1}. */
	private static final double[][] HALO = {
			{0.12, 150, 205, 255, 0.60},
			{0.18, 60, 130, 255, 0.36},
			{0.25, 40, 100, 255, 0.22},
			{0.33, 30, 80, 255, 0.14},
			{0.42, 20, 60, 240, 0.09},
			{0.55, 15, 45, 220, 0.06}};

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
		state.surface = null;
		if (entity.layers() > 0) {
			double[] a = entity.rackPoint(0), b = entity.rackPoint(Tuning.POOL_RACKS - 1);
			double floor = a[1] - 2;
			state.surface = new double[] {Math.min(a[0], b[0]) - 0.5 - bx, Math.min(a[2], b[2]) - 0.5 - bz, Math.max(a[0], b[0]) + 0.5 - bx, Math.max(a[2], b[2]) + 0.5 - bz,
					floor + entity.layers() - 1 + 0.8888889 - by};
			if (Minecraft.getInstance().level != null) {
				state.waterColor = net.minecraft.client.renderer.BiomeColors.getAverageWaterColor(Minecraft.getInstance().level, entity.getBlockPos());
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
				// hot: the steel lit by its own glow, a pale electric blue; cooled: plain steel in the pool's light
				double g = a[3];
				int color = 0xFF000000 | (int) (255 - 135 * g) << 16 | (int) (255 - 55 * g) << 8 | 255;
				int light = g > 0.05 ? 0xF000F0 : state.lightCoords;
				box(pose, v, steel, a[0] - 0.107, a[1] + 0.1, a[2] - 0.107, a[0] + 0.107, a[1] + 4.16, a[2] + 0.107, color, light);
			}
		});
		// the Cherenkov light in the water round each hot assembly, additive, so neighbours add up
		collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, v) -> {
			for (double[] a : state.assemblies) {
				double g = a[3];
				if (g < 0.02) {
					continue;
				}
				for (double[] h : HALO) {
					int alpha = (int) (255 * g * h[4]);
					glowBox(pose, v, a[0] - h[0], a[1] + 0.1, a[2] - h[0], a[0] + h[0], a[1] + 4.3, a[2] + h[0], (int) h[1], (int) h[2], (int) h[3], alpha, alpha);
				}
				// the light rising off the rack through the water, fading
				int up = (int) (255 * g * 0.30);
				glowBox(pose, v, a[0] - 0.45, a[1] + 4.3, a[2] - 0.45, a[0] + 0.45, a[1] + 7.5, a[2] + 0.45, 30, 100, 255, up, 0);
			}
		});
		double[] w = state.surface;
		if (w != null && !Minecraft.getInstance().gameRenderer.useImprovedTransparency()) {
			// clear water: the mask (drawn after the glow, before the game's water) hides the water's own face over the pool...
			float y = (float) w[4];
			collector.order(1).submitCustomGeometry(poseStack, RenderTypes.waterMask(), (pose, v) -> {
				quad(pose, v, w, y + 0.002f);
			});
			// ...and a thinner face of the pool's water is drawn in its place
			TextureAtlasSprite water = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
					.getSprite(net.minecraft.resources.Identifier.withDefaultNamespace("block/water_still"));
			int tint = 0x70000000 | (state.waterColor & 0xFFFFFF);
			collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), (pose, v) -> {
				for (double x = w[0]; x < w[2] - 1e-6; x++) {
					for (double z = w[1]; z < w[3] - 1e-6; z++) {
						float x0 = (float) x, x1 = (float) Math.min(w[2], x + 1), z0 = (float) z, z1 = (float) Math.min(w[3], z + 1), yy = y + 0.004f;
						v.addVertex(pose, x0, yy, z0).setColor(tint).setUv(water.getU(0), water.getV(0)).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(pose, 0, 1, 0);
						v.addVertex(pose, x0, yy, z1).setColor(tint).setUv(water.getU(0), water.getV(1)).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(pose, 0, 1, 0);
						v.addVertex(pose, x1, yy, z1).setColor(tint).setUv(water.getU(1), water.getV(1)).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(pose, 0, 1, 0);
						v.addVertex(pose, x1, yy, z0).setColor(tint).setUv(water.getU(1), water.getV(0)).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords).setNormal(pose, 0, 1, 0);
					}
				}
			});
		}
	}

	/** A flat quad over the pool, both faces (the mask must hide the water's face seen from above and from under it). */
	private static void quad(PoseStack.Pose pose, VertexConsumer v, double[] w, float y) {
		float x0 = (float) w[0], z0 = (float) w[1], x1 = (float) w[2], z1 = (float) w[3];
		v.addVertex(pose, x0, y, z0);
		v.addVertex(pose, x0, y, z1);
		v.addVertex(pose, x1, y, z1);
		v.addVertex(pose, x1, y, z0);
		v.addVertex(pose, x0, y, z0);
		v.addVertex(pose, x1, y, z0);
		v.addVertex(pose, x1, y, z1);
		v.addVertex(pose, x0, y, z1);
	}

	/** A glowing box's four sides and its top, its colour's alpha {@code aBottom} at the foot and {@code aTop} at the head. */
	private static void glowBox(PoseStack.Pose pose, VertexConsumer v, double x0, double y0, double z0, double x1, double y1, double z1, int r, int g, int b, int aBottom, int aTop) {
		double[][] sides = {{x0, z0, x1, z0}, {x1, z0, x1, z1}, {x1, z1, x0, z1}, {x0, z1, x0, z0}};
		for (double[] s : sides) {
			v.addVertex(pose, (float) s[0], (float) y0, (float) s[1]).setColor(r, g, b, aBottom);
			v.addVertex(pose, (float) s[2], (float) y0, (float) s[3]).setColor(r, g, b, aBottom);
			v.addVertex(pose, (float) s[2], (float) y1, (float) s[3]).setColor(r, g, b, aTop);
			v.addVertex(pose, (float) s[0], (float) y1, (float) s[1]).setColor(r, g, b, aTop);
		}
		if (aTop > 0) {
			v.addVertex(pose, (float) x0, (float) y1, (float) z0).setColor(r, g, b, aTop);
			v.addVertex(pose, (float) x0, (float) y1, (float) z1).setColor(r, g, b, aTop);
			v.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(r, g, b, aTop);
			v.addVertex(pose, (float) x1, (float) y1, (float) z0).setColor(r, g, b, aTop);
		}
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
