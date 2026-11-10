package dev.wildcatter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.wildcatter.Wildcatter;
import dev.wildcatter.kinetic.DrawnRuns;
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
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a formed machine's {@link DrawnRuns}: the pipes and cable trays between its parts (docs/questions/misc-gaps.md, 1 and 4), in
 * the mod's material textures, a metre of texture to a metre of run as the part models are. {@link KineticRenderer} draws them too for
 * a machine that also turns.
 */
public class RunsRenderer<T extends BlockEntity & DrawnRuns> implements BlockEntityRenderer<T, RunsRenderer.State> {
	public static class State extends BlockEntityRenderState {
		final List<DrawnRuns.Run> runs = new ArrayList<>();
		Direction facing = Direction.NORTH;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(T machine, State state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(machine, state, crumbling);
		state.facing = machine.facing();
		state.runs.clear();
		state.runs.addAll(machine.drawnRuns());
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		draw(state.runs, state.facing, state.lightCoords, pose, collector);
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	/** Draws the runs in the machine's frame, turned to its facing about the controller block's centre (as KineticRenderer turns). */
	static void draw(List<DrawnRuns.Run> runs, Direction facing, int light, PoseStack pose, SubmitNodeCollector collector) {
		if (runs.isEmpty()) {
			return;
		}
		pose.pushPose();
		pose.translate(0.5F, 0.5F, 0.5F);
		pose.rotateDegrees(Axis.YP, -facing.toYRot() + 180.0F);
		pose.translate(-0.5F, -0.5F, -0.5F);
		var atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
		for (DrawnRuns.Run run : runs) {
			TextureAtlasSprite sprite = atlas.getSprite(Wildcatter.id(run.texture()));
			TextureAtlasSprite cable = atlas.getSprite(Wildcatter.id("block/mat/mat_rubber"));
			collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (p, v) -> {
				Quads q = new Quads(p, v, sprite, light);
				List<double[]> pts = run.points();
				switch (run.kind()) {
					case PIPE -> {
						for (int i = 0; i + 1 < pts.size(); i++) {
							double a = i == 0 ? 0 : run.size(), b = i + 2 == pts.size() ? 0 : run.size();   // run on past each elbow
							q.tube(pts.get(i), pts.get(i + 1), run.size(), 8, a, b);
						}
						double flange = run.size() * 1.8 + 0.03;
						q.tube(pts.get(0), toward(pts.get(0), pts.get(1), 0.03), flange, 12, 0, 0);
						int n = pts.size();
						q.tube(toward(pts.get(n - 1), pts.get(n - 2), 0.03), pts.get(n - 1), flange, 12, 0, 0);
					}
					case TRAY -> {
						for (int i = 0; i + 1 < pts.size(); i++) {
							q.tray(pts.get(i), pts.get(i + 1), run.size(), i == 0 ? 0 : run.size() / 2, i + 2 == pts.size() ? 0 : run.size() / 2);
						}
						Quads c = new Quads(p, v, cable, light);
						for (int i = 0; i + 1 < pts.size(); i++) {
							c.cables(pts.get(i), pts.get(i + 1), 0.035, true, i == 0 ? 0 : run.size() / 2, i + 2 == pts.size() ? 0 : run.size() / 2);
						}
					}
					case CABLES -> {
						Quads c = new Quads(p, v, sprite, light);
						for (int i = 0; i + 1 < pts.size(); i++) {
							c.cables(pts.get(i), pts.get(i + 1), run.size(), false, i == 0 ? 0 : run.size() * 3, i + 2 == pts.size() ? 0 : run.size() * 3);
						}
					}
					case POSTS -> {
						for (double[] at : pts) {
							q.box(new double[] {at[0], 0, at[2]}, new double[] {at[0], at[1], at[2]}, new double[] {1, 0, 0}, run.size(), run.size(), 0, 0);
						}
					}
				}
			});
		}
		pose.popPose();
	}

	private static double[] toward(double[] from, double[] to, double distance) {
		double[] d = sub(to, from);
		double len = len(d);
		return len < 1e-9 ? from : new double[] {from[0] + d[0] / len * distance, from[1] + d[1] / len * distance, from[2] + d[2] / len * distance};
	}

	static double[] sub(double[] a, double[] b) {
		return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
	}

	static double len(double[] a) {
		return Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
	}

	static double[] cross(double[] a, double[] b) {
		return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
	}

	static double[] unit(double[] a) {
		double l = len(a);
		return new double[] {a[0] / l, a[1] / l, a[2] / l};
	}

	/** Writes textured quads, a metre of texture to a metre of geometry. */
	private static final class Quads {
		final PoseStack.Pose pose;
		final VertexConsumer v;
		final TextureAtlasSprite s;
		final int light;

		Quads(PoseStack.Pose pose, VertexConsumer v, TextureAtlasSprite s, int light) {
			this.pose = pose;
			this.v = v;
			this.s = s;
			this.light = light;
		}

		/** The run's axis, and two unit vectors across it: "side" level where it can be, "up" the other. */
		private static double[][] frame(double[] a, double[] b) {
			double[] d = unit(sub(b, a));
			double[] side = Math.abs(d[1]) > 0.99 ? new double[] {1, 0, 0} : unit(cross(d, new double[] {0, 1, 0}));
			double[] up = unit(cross(side, d));
			return new double[][] {d, side, up};
		}

		/** A quad from four corners in order round it, facing {@code n}; its texture covers {@code du} x {@code dv} metres. */
		private void quad(double[][] p, double[] n, double du, double dv) {
			double[] normal = cross(sub(p[1], p[0]), sub(p[2], p[0]));
			boolean flip = normal[0] * n[0] + normal[1] * n[1] + normal[2] * n[2] < 0;
			float[][] uv = {{0, 0}, {(float) Math.min(1, du), 0}, {(float) Math.min(1, du), (float) Math.min(1, dv)}, {0, (float) Math.min(1, dv)}};
			for (int k = 0; k < 4; k++) {
				int i = flip ? 3 - k : k;
				v.addVertex(pose, (float) p[i][0], (float) p[i][1], (float) p[i][2]).setColor(0xFFFFFFFF).setUv(s.getU(uv[i][0]), s.getV(uv[i][1]))
						.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, (float) n[0], (float) n[1], (float) n[2]);
			}
		}

		private static double[] at(double[] o, double[] d, double t, double[] u, double su, double[] w, double sw) {
			return new double[] {o[0] + d[0] * t + u[0] * su + w[0] * sw, o[1] + d[1] * t + u[1] * su + w[1] * sw, o[2] + d[2] * t + u[2] * su + w[2] * sw};
		}

		/** A round tube of radius r and the given sides from a to b, run on {@code before} past a and {@code after} past b; in pieces of a metre. */
		void tube(double[] a, double[] b, double r, int sides, double before, double after) {
			double[][] f = frame(a, b);
			double[] d = f[0], u = f[1], w = f[2];
			double length = len(sub(b, a));
			double t0 = -before, t1 = length + after;
			int pieces = Math.max(1, (int) Math.ceil(t1 - t0));
			double arc = 2 * Math.PI * r / sides;
			for (int k = 0; k < pieces; k++) {
				double s0 = t0 + (t1 - t0) * k / pieces, s1 = t0 + (t1 - t0) * (k + 1) / pieces;
				for (int j = 0; j < sides; j++) {
					double a0 = 2 * Math.PI * j / sides, a1 = 2 * Math.PI * (j + 1) / sides;
					double[][] p = {at(a, d, s0, u, r * Math.cos(a0), w, r * Math.sin(a0)), at(a, d, s1, u, r * Math.cos(a0), w, r * Math.sin(a0)),
							at(a, d, s1, u, r * Math.cos(a1), w, r * Math.sin(a1)), at(a, d, s0, u, r * Math.cos(a1), w, r * Math.sin(a1))};
					double am = (a0 + a1) / 2;
					quad(p, new double[] {u[0] * Math.cos(am) + w[0] * Math.sin(am), u[1] * Math.cos(am) + w[1] * Math.sin(am), u[2] * Math.cos(am) + w[2] * Math.sin(am)}, s1 - s0, arc);
				}
			}
			for (double t : new double[] {t0, t1}) {   // the end caps, as a fan of quads
				double[] n = t == t0 ? new double[] {-d[0], -d[1], -d[2]} : d;
				double[] c = at(a, d, t, u, 0, w, 0);
				for (int j = 0; j < sides; j += 2) {
					double a0 = 2 * Math.PI * j / sides, a1 = 2 * Math.PI * (j + 1) / sides, a2 = 2 * Math.PI * (j + 2) / sides;
					quad(new double[][] {c, at(a, d, t, u, r * Math.cos(a0), w, r * Math.sin(a0)), at(a, d, t, u, r * Math.cos(a1), w, r * Math.sin(a1)),
							at(a, d, t, u, r * Math.cos(a2), w, r * Math.sin(a2))}, n, r, r);
				}
			}
		}

		/** A box along a to b, half-widths hu along {@code side} and hw across both, run on past its ends. */
		void box(double[] a, double[] b, double[] side, double hu, double hw, double before, double after) {
			double[] d = unit(sub(b, a));
			double[] u = Math.abs(d[0] * side[0] + d[1] * side[1] + d[2] * side[2]) > 0.99 ? frame(a, b)[1] : unit(side);
			double[] w = unit(cross(u, d));
			double length = len(sub(b, a));
			double t0 = -before, t1 = length + after;
			int pieces = Math.max(1, (int) Math.ceil(t1 - t0));
			for (int k = 0; k < pieces; k++) {
				double s0 = t0 + (t1 - t0) * k / pieces, s1 = t0 + (t1 - t0) * (k + 1) / pieces;
				for (int face = 0; face < 4; face++) {
					double[] n = switch (face) {
						case 0 -> u;
						case 1 -> new double[] {-u[0], -u[1], -u[2]};
						case 2 -> w;
						default -> new double[] {-w[0], -w[1], -w[2]};
					};
					double su = face == 0 ? hu : face == 1 ? -hu : 0, sw = face == 2 ? hw : face == 3 ? -hw : 0;
					double[] across = face < 2 ? w : u;
					double half = face < 2 ? hw : hu;
					double[] c0 = at(a, d, s0, u, su, w, sw), c1 = at(a, d, s1, u, su, w, sw);
					quad(new double[][] {plus(c0, across, -half), plus(c1, across, -half), plus(c1, across, half), plus(c0, across, half)}, n, s1 - s0, 2 * half);
				}
			}
			for (double t : new double[] {t0, t1}) {
				double[] n = t == t0 ? new double[] {-d[0], -d[1], -d[2]} : d;
				quad(new double[][] {at(a, d, t, u, -hu, w, -hw), at(a, d, t, u, hu, w, -hw), at(a, d, t, u, hu, w, hw), at(a, d, t, u, -hu, w, hw)}, n, 2 * hu, 2 * hw);
			}
		}

		private static double[] plus(double[] p, double[] d, double k) {
			return new double[] {p[0] + d[0] * k, p[1] + d[1] * k, p[2] + d[2] * k};
		}

		/**
		 * A ladder tray {@code width} wide from a to b: its floor and two side rails 0.1 m high. A level run lies flat; a riser stands
		 * with its floor against the face of a structure north of it, its cables on its south (+z) side.
		 */
		void tray(double[] a, double[] b, double width, double before, double after) {
			double[][] f = frame(a, b);
			double[] d = f[0], side = f[1];
			boolean riser = Math.abs(d[1]) > 0.99;
			double[] floorNormal = riser ? new double[] {0, 0, 1} : new double[] {0, 1, 0};   // the side the cables lie on
			double[] across = riser ? new double[] {1, 0, 0} : side;
			box(a, b, across, width / 2, 0.01, before, after);
			for (double k : new double[] {-1, 1}) {
				double[] off = plus(new double[] {0, 0, 0}, across, k * (width / 2 - 0.01));
				double[] lift = plus(off, floorNormal, 0.05);
				box(plus(a, lift, 1), plus(b, lift, 1), across, 0.01, 0.05, before, after);
			}
		}

		/** Three cables side by side from a to b: on a tray's floor (lifted onto it) or hanging free. */
		void cables(double[] a, double[] b, double r, boolean onTray, double before, double after) {
			double[][] f = frame(a, b);
			double[] d = f[0];
			boolean riser = Math.abs(d[1]) > 0.99;
			double[] across = riser ? new double[] {1, 0, 0} : f[1];
			double[] lift = onTray ? (riser ? new double[] {0, 0, 0.01 + r} : new double[] {0, 0.01 + r, 0}) : new double[] {0, 0, 0};
			for (int k = -1; k <= 1; k++) {
				double[] off = plus(lift, across, k * r * 2.2);
				tube(plus(a, off, 1), plus(b, off, 1), r, 6, before, after);
			}
		}
	}
}
