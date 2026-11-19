package dev.wildcatter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.wildcatter.Wildcatter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;

/** Small drawn parts for block entity renderers: a box in block pixels (0-16), on one of the mod's materials, tinted. */
final class Boxes {
	private Boxes() {
	}

	static TextureAtlasSprite mat(String name) {
		return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(Wildcatter.id("block/mat/" + name));
	}

	/** Eases a drawn value toward the last one the server sent, so a part moves smoothly between updates. */
	static double ease(double shown, double target, long lastNanos, long now) {
		if (shown < 0) {
			return target;
		}
		double seconds = Math.min(1, (now - lastNanos) / 1e9);
		return shown + (target - shown) * Math.min(1, seconds * 8);
	}

	/** A box from (x0, y0, z0) to (x1, y1, z1) in block pixels. */
	static void px(PoseStack.Pose pose, VertexConsumer v, TextureAtlasSprite s, double x0, double y0, double z0, double x1, double y1, double z1, int color, int light) {
		box(pose, v, s, (float) (x0 / 16), (float) (y0 / 16), (float) (z0 / 16), (float) (x1 / 16), (float) (y1 / 16), (float) (z1 / 16), color, light);
	}

	static void box(PoseStack.Pose pose, VertexConsumer v, TextureAtlasSprite s, float x0, float y0, float z0, float x1, float y1, float z1, int color, int light) {
		float[][] faces = {
				{x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, 0, 0, -1},
				{x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, 0, 0, 1},
				{x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, -1, 0, 0},
				{x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1, 0, 0},
				{x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0},
				{x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0, -1, 0}};
		float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
		for (float[] f : faces) {
			for (int k = 3; k >= 0; k--) {
				v.addVertex(pose, f[k * 3], f[k * 3 + 1], f[k * 3 + 2]).setColor(color).setUv(s.getU(uv[k][0]), s.getV(uv[k][1]))
						.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, f[12], f[13], f[14]);
			}
		}
	}
}
