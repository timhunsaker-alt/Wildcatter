package dev.wildcatter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.wildcatter.Tuning;
import dev.wildcatter.Wildcatter;
import dev.wildcatter.block.NaturalDraftTowerBlockEntity;
import dev.wildcatter.fluid.CoolingTower;
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
 * Draws the hyperbolic natural-draft cooling tower (docs/multiblocks/natural-draft-tower.md) as one smooth model from its controller:
 * the basin's rim, the raker columns, the shell up to its last finished lift (inside and out), and the fill, in concrete. The tower is
 * 88 m tall and about 64 m across, so it renders off-screen and far away.
 */
public class CoolingTowerRenderer implements BlockEntityRenderer<NaturalDraftTowerBlockEntity, CoolingTowerRenderer.State> {
	private static final int SIDES = 96;

	public static class State extends BlockEntityRenderState {
		int stage;
		int lifts;
		double cx, cz;   // the centre, from the controller's block
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(NaturalDraftTowerBlockEntity entity, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay breakProgress) {
		BlockEntityRenderState.extractBase(entity, state, breakProgress);
		state.stage = entity.stage();
		state.lifts = entity.lifts();
		double[] c = entity.centre();
		state.cx = c[0] - entity.getBlockPos().getX();
		state.cz = c[1] - entity.getBlockPos().getZ();
	}

	private static TextureAtlasSprite concrete() {
		return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(Wildcatter.id("block/mat/mat_concrete"));
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.stage == NaturalDraftTowerBlockEntity.BASIN) {
			return;
		}
		TextureAtlasSprite sprite = concrete();
		int light = 0xF00000;   // full sky light: the shell stands in the open, day or night as the sky is
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (pose, v) -> {
			Quads q = new Quads(pose, v, sprite, light, state.cx, state.cz);
			// the rim: an outer wall and its top, 1 m high
			q.band(Tuning.TOWER_BASIN_R + 1, 0, Tuning.TOWER_BASIN_R + 1, 1, true);
			q.band(Tuning.TOWER_BASIN_R, 1, Tuning.TOWER_BASIN_R + 1, 1, true);
			// the raker columns: 40 pairs of slim legs leaning from the rim in to the shell's foot
			if (state.stage >= NaturalDraftTowerBlockEntity.SHELL) {
				double foot = CoolingTower.radius(Tuning.TOWER_SHELL_BASE_Y);
				for (int k = 0; k < 40; k++) {
					for (double off : new double[] {-0.04, 0.04}) {
						double a = 2 * Math.PI * k / 40 + off;
						q.leg(a, Tuning.TOWER_BASIN_R, 0, foot, Tuning.TOWER_SHELL_BASE_Y, 0.3);
					}
				}
			}
			// the shell, lift by lift, outside and in
			for (int i = 0; i < state.lifts; i++) {
				double y1 = CoolingTower.liftY(i);
				double y2 = y1 + CoolingTower.liftHeight(i);
				q.band(CoolingTower.radius(y1), y1, CoolingTower.radius(y2), y2, true);
				q.band(CoolingTower.radius(y1) - 0.3, y1, CoolingTower.radius(y2) - 0.3, y2, false);
			}
			// the shell's top lip
			if (state.lifts >= CoolingTower.lifts()) {
				double r = CoolingTower.radius(Tuning.TOWER_TOP_Y);
				q.band(r - 0.3, Tuning.TOWER_TOP_Y, r, Tuning.TOWER_TOP_Y, true);
			}
			// the fill pack over the basin
			if (state.stage >= NaturalDraftTowerBlockEntity.DONE) {
				double fy = Tuning.TOWER_SHELL_BASE_Y - 2;
				q.band(4, fy, CoolingTower.radius(Tuning.TOWER_SHELL_BASE_Y) - 0.5, fy, true);
			}
		});
	}

	/** Writes textured quads in the tower's frame: y up from the controller's floor, angles round the centre. */
	private static final class Quads {
		final PoseStack.Pose pose;
		final VertexConsumer v;
		final TextureAtlasSprite s;
		final int light;
		final double cx, cz;

		Quads(PoseStack.Pose pose, VertexConsumer v, TextureAtlasSprite s, int light, double cx, double cz) {
			this.pose = pose;
			this.v = v;
			this.s = s;
			this.light = light;
			this.cx = cx;
			this.cz = cz;
		}

		void vertex(double x, double y, double z, float u, float w, float nx, float ny, float nz) {
			// the entity shader dims level normals; tilting them up lets the shell read as daylit concrete
			float len = (float) Math.sqrt(nx * nx * 0.25F + (ny + 0.8F) * (ny + 0.8F) + nz * nz * 0.25F);
			v.addVertex(pose, (float) x, (float) y, (float) z).setColor(0xFFFFFFFF).setUv(s.getU(u), s.getV(w)).setOverlay(OverlayTexture.NO_OVERLAY)
					.setLight(light).setNormal(pose, nx * 0.5F / len, (ny + 0.8F) / len, nz * 0.5F / len);
		}

		/** A ring band from radius r1 at y1 to r2 at y2, facing out (or in). */
		void band(double r1, double y1, double r2, double y2, boolean out) {
			for (int k = 0; k < SIDES; k++) {
				double a0 = 2 * Math.PI * k / SIDES;
				double a1 = 2 * Math.PI * (k + 1) / SIDES;
				float nx = (float) Math.cos((a0 + a1) / 2) * (out ? 1 : -1);
				float nz = (float) Math.sin((a0 + a1) / 2) * (out ? 1 : -1);
				double[][] p = {{cx + r1 * Math.cos(a0), y1, cz + r1 * Math.sin(a0)}, {cx + r1 * Math.cos(a1), y1, cz + r1 * Math.sin(a1)},
						{cx + r2 * Math.cos(a1), y2, cz + r2 * Math.sin(a1)}, {cx + r2 * Math.cos(a0), y2, cz + r2 * Math.sin(a0)}};
				int[] order = out ? new int[] {0, 3, 2, 1} : new int[] {0, 1, 2, 3};
				float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
				for (int i : order) {
					vertex(p[i][0], p[i][1], p[i][2], uv[i][0], uv[i][1], nx, 0, nz);
				}
			}
		}

		/** A square leg of half-width {@code w} from radius r1 at y1 to r2 at y2, at angle {@code a}. */
		void leg(double a, double r1, double y1, double r2, double y2, double w) {
			double ca = Math.cos(a), sa = Math.sin(a);
			double tx = -sa, tz = ca;   // across the leg
			double[][] base = {{r1 - w, -w}, {r1 + w, -w}, {r1 + w, w}, {r1 - w, w}};
			double[][] top = {{r2 - w, -w}, {r2 + w, -w}, {r2 + w, w}, {r2 - w, w}};
			for (int f = 0; f < 4; f++) {
				int g = (f + 1) % 4;
				double[][] p = {{base[f][0], y1, base[f][1]}, {base[g][0], y1, base[g][1]}, {top[g][0], y2, top[g][1]}, {top[f][0], y2, top[f][1]}};
				float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
				for (int i : new int[] {0, 3, 2, 1}) {
					double r = p[i][0], t = p[i][2];
					vertex(cx + r * ca + t * tx, p[i][1], cz + r * sa + t * tz, uv[i][0], uv[i][1], (float) ca, 0, (float) sa);
				}
			}
		}
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 512;
	}

	@Override
	public boolean shouldRender(NaturalDraftTowerBlockEntity entity, Vec3 cameraPos) {
		return true;
	}
}
