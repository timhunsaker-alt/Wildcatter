package dev.wildcatter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.wildcatter.Wildcatter;
import dev.wildcatter.block.CraneBlockEntity;
import dev.wildcatter.block.SiteMarkerBlockEntity;
import dev.wildcatter.blueprint.Blueprint;
import dev.wildcatter.blueprint.Surveys;
import dev.wildcatter.crane.CraneKind;
import dev.wildcatter.crane.LoadChart;
import dev.wildcatter.multiblock.BigPart;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws what stands above a crane's base (docs/multiblocks/tower-crane.md, crawler-crane.md): the tower crane's lattice mast, its
 * slewing platform and cab, its 75 m jib and counter-jib, the trolley, the ropes and the hook; or the crawler's upper works, its
 * lattice boom at its length and angle, the pendants, the ropes and the hook. The load on the hook is drawn from its own block models.
 * Everything turns with the slewing angle and moves through the step the crane is in.
 */
public class CraneRenderer implements BlockEntityRenderer<CraneBlockEntity, CraneRenderer.State> {
	private static final BlockDisplayContext CONTEXT = BlockDisplayContext.create();

	public static class State extends BlockEntityRenderState {
		CraneKind kind = CraneKind.TOWER;
		int boom;
		double cx, cz;              // the slewing centre, from the block
		double deg, radius, height; // the hook
		final List<BlockModelRenderState> load = new ArrayList<>();
		final List<double[]> at = new ArrayList<>();   // each load model's corner, from the block
		int loadModels;
	}

	private final BlockModelResolver resolver;

	public CraneRenderer(BlockEntityRendererProvider.Context context) {
		this.resolver = context.blockModelResolver();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(CraneBlockEntity crane, State state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(crane, state, crumbling);
		BlockPos pos = crane.getBlockPos();
		state.kind = crane.kind();
		state.boom = crane.boom();
		double[] c = crane.centre();
		state.cx = c[0] - pos.getX();
		state.cz = c[1] - pos.getZ();
		float time = crane.getLevel() == null ? 0 : crane.getLevel().getGameTime() + partialTick;
		double[] hook = crane.hookAt(time);
		state.deg = hook[0];
		state.radius = hook[1];
		state.height = hook[2];
		state.loadModels = 0;
		if (crane.carrying() && crane.site() != null && crane.getLevel() != null
				&& crane.getLevel().getBlockEntity(crane.site()) instanceof SiteMarkerBlockEntity marker && marker.lifts() != null
				&& crane.lift() >= 0 && crane.lift() < marker.lifts().size()) {
			extractLoad(state, marker, marker.lifts().get(crane.lift()), pos);
		}
	}

	/** The load's block models, hung with the middle of its bottom at the hook. */
	private void extractLoad(State state, SiteMarkerBlockEntity marker, Blueprint.Lift lift, BlockPos pos) {
		Blueprint b = marker.blueprint();
		BlockState[] states = marker.states();
		List<Object[]> cells = new ArrayList<>();   // {state, x, y, z} in the site
		if (lift.isPart()) {
			Blueprint.Part p = b.parts().get(lift.part());
			BlockState main = Surveys.partState(p);
			if (main != null && main.getBlock() instanceof BigPart part) {
				int turns = p.turns();
				for (int i = 0; i < part.footprint().pieces(); i++) {
					int[] off = part.footprint().offset(i, turns);
					cells.add(new Object[] {main.setValue(part.piece(), i), p.x() + off[0], p.y() + off[1], p.z() + off[2]});
				}
			}
		} else {
			for (int i : lift.blocks()) {
				Blueprint.Block k = b.blocks().get(i);
				if (states[k.state()] != null) {
					cells.add(new Object[] {states[k.state()], k.x(), k.y(), k.z()});
				}
			}
		}
		if (cells.isEmpty()) {
			return;
		}
		int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
		for (Object[] cell : cells) {
			x0 = Math.min(x0, (int) cell[1]);
			y0 = Math.min(y0, (int) cell[2]);
			z0 = Math.min(z0, (int) cell[3]);
			x1 = Math.max(x1, (int) cell[1]);
			z1 = Math.max(z1, (int) cell[3]);
		}
		double midX = (x0 + x1 + 1) / 2.0, midZ = (z0 + z1 + 1) / 2.0;
		double rad = Math.toRadians(state.deg);
		double hx = state.cx + Math.sin(rad) * state.radius;
		double hz = state.cz - Math.cos(rad) * state.radius;
		for (Object[] cell : cells) {
			if (state.loadModels >= state.load.size()) {
				state.load.add(new BlockModelRenderState());
				state.at.add(new double[3]);
			}
			resolver.update(state.load.get(state.loadModels), (BlockState) cell[0], CONTEXT);
			double[] at = state.at.get(state.loadModels);
			at[0] = hx + (int) cell[1] - midX;
			at[1] = state.height + (int) cell[2] - y0;
			at[2] = hz + (int) cell[3] - midZ;
			state.loadModels++;
		}
	}

	private static TextureAtlasSprite sprite(String name) {
		return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(Wildcatter.id("block/mat/" + name));
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		TextureAtlasSprite yellow = sprite("mat_paint_crane_yellow_placeholder");
		TextureAtlasSprite concrete = sprite("mat_concrete");
		TextureAtlasSprite dark = sprite("mat_oil_dark");
		TextureAtlasSprite rope = sprite("mat_galvanized");
		TextureAtlasSprite grey = sprite("mat_paint_ansi61");
		int light = 0xF00000;   // full sky light: it stands in the open
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (pose, v) -> {
			Frame f = new Frame(pose, v, light, state.cx, state.cz, state.deg);
			if (state.kind == CraneKind.TOWER) {
				tower(f, state, yellow, concrete, dark, rope, grey);
			} else {
				crawler(f, state, yellow, concrete, dark, rope, grey);
			}
		});
		for (int i = 0; i < state.loadModels; i++) {
			double[] at = state.at.get(i);
			poseStack.pushPose();
			poseStack.translate(at[0], at[1], at[2]);
			state.load.get(i).submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			poseStack.popPose();
		}
	}

	// ---- the tower crane ---------------------------------------------------------------------------------------------------------------
	private static final double MAST_HALF = 1.15, MAST_FROM = 3.0, SECTION = 3.0;

	private static void tower(Frame f, State s, TextureAtlasSprite yellow, TextureAtlasSprite concrete, TextureAtlasSprite dark,
			TextureAtlasSprite rope, TextureAtlasSprite grey) {
		double top = CraneKind.TOWER_HOOK_HEIGHT + 1.5;   // the jib's bottom chords
		// the mast does not turn: drawn in the base's frame (u east, v south)
		f.fixed = true;
		double h = MAST_HALF;
		for (double u : new double[] {-h, h}) {
			for (double w : new double[] {-h, h}) {
				f.beam(yellow, u, MAST_FROM, w, u, top - 1.0, w, 0.18);
			}
		}
		for (double y = MAST_FROM; y < top - 1.0; y += SECTION) {
			double y2 = Math.min(y + SECTION, top - 1.0);
			f.beam(yellow, -h, y, -h, h, y, -h, 0.1);
			f.beam(yellow, -h, y, h, h, y, h, 0.1);
			f.beam(yellow, -h, y, -h, -h, y, h, 0.1);
			f.beam(yellow, h, y, -h, h, y, h, 0.1);
			f.beam(yellow, -h, y, -h, h, y2, -h, 0.08);   // one diagonal on each face
			f.beam(yellow, h, y, h, -h, y2, h, 0.08);
			f.beam(yellow, -h, y, h, -h, y2, -h, 0.08);
			f.beam(yellow, h, y, -h, h, y2, h, 0.08);
		}
		// what slews: u along the jib (out), v across
		f.fixed = false;
		f.box(yellow, -1.6, top - 1.0, -1.6, 1.6, top, 1.6);                // the slewing platform
		f.box(grey, 1.2, top - 3.2, -2.6, 3.0, top - 0.8, -1.2);             // the cab, hung off the platform
		double jib = 76.8, counter = 22.0;
		double w = 0.9, chordUp = 1.9;
		f.beam(yellow, 0, top, -w, jib, top, -w, 0.16);                       // the jib's bottom chords
		f.beam(yellow, 0, top, w, jib, top, w, 0.16);
		f.beam(yellow, 0, top + chordUp, 0, jib - 1.5, top + 0.6, 0, 0.16);   // its top chord
		for (double u = 0; u < jib - 2; u += SECTION) {
			double t1 = top + chordUp - (chordUp - 0.6) * u / jib;
			f.beam(yellow, u, top, -w, u, top, w, 0.08);
			f.beam(yellow, u, top, -w, u + SECTION / 2, t1, 0, 0.07);
			f.beam(yellow, u, top, w, u + SECTION / 2, t1, 0, 0.07);
			f.beam(yellow, u + SECTION, top, -w, u + SECTION / 2, t1, 0, 0.07);
			f.beam(yellow, u + SECTION, top, w, u + SECTION / 2, t1, 0, 0.07);
		}
		f.beam(yellow, 0, top, -w, -counter, top, -w, 0.16);                  // the counter-jib
		f.beam(yellow, 0, top, w, -counter, top, w, 0.16);
		for (double u = 0; u > -counter; u -= SECTION) {
			f.beam(yellow, u, top, -w, u, top, w, 0.08);
			f.beam(yellow, u, top, -w, u - SECTION, top, w, 0.06);
		}
		f.box(concrete, -counter + 0.3, top - 2.4, -1.2, -counter + 4.3, top, 1.2);   // its ballast blocks
		f.box(grey, -8.0, top, -0.8, -5.0, top + 1.6, 0.8);                   // the hoist gear's house
		// the trolley, the ropes and the hook
		double r = s.radius;
		f.box(dark, r - 1.0, top - 0.6, -w - 0.2, r + 1.0, top, w + 0.2);
		double hookTop = s.height + 1.6;
		f.beam(rope, r - 0.15, top - 0.6, -0.15, r - 0.15, hookTop, -0.15, 0.04);
		f.beam(rope, r + 0.15, top - 0.6, 0.15, r + 0.15, hookTop, 0.15, 0.04);
		f.box(yellow, r - 0.4, hookTop - 0.6, -0.3, r + 0.4, hookTop, 0.3);   // the hook block
		f.box(dark, r - 0.08, s.height + 0.9, -0.08, r + 0.08, hookTop - 0.6, 0.08);   // the hook
		f.beam(rope, r, s.height + 0.9, 0, r - 0.6, s.height + 0.02, 0, 0.025);         // the slings
		f.beam(rope, r, s.height + 0.9, 0, r + 0.6, s.height + 0.02, 0, 0.025);
	}

	// ---- the crawler crane --------------------------------------------------------------------------------------------------------------
	private static void crawler(Frame f, State s, TextureAtlasSprite yellow, TextureAtlasSprite concrete, TextureAtlasSprite dark,
			TextureAtlasSprite rope, TextureAtlasSprite grey) {
		f.fixed = false;
		f.box(yellow, -4.5, 2.0, -1.6, 3.0, 3.4, 1.6);           // the turntable and engine house
		f.box(dark, -6.8, 2.2, -2.6, -4.5, 4.8, 2.6);            // the 104 t counterweight
		f.box(grey, 1.0, 2.2, -2.9, 3.4, 4.6, -1.7);              // the cab
		double frameTop = 9.0;
		f.beam(yellow, -2.0, 3.4, -1.2, -1.0, frameTop, -0.8, 0.22);   // the A-frame
		f.beam(yellow, -2.0, 3.4, 1.2, -1.0, frameTop, 0.8, 0.22);
		f.beam(yellow, -1.0, frameTop, -0.8, -1.0, frameTop, 0.8, 0.2);
		// the boom: its foot 1.5 m out and 2.7 m up, at the angle that puts the hook at its radius
		double length = LoadChart.CRAWLER_BOOMS[s.boom];
		double angle = Math.toRadians(CraneKind.boomDegrees(s.boom, s.radius));
		double fu = LoadChart.BOOM_FOOT_OUT, fy = LoadChart.BOOM_FOOT_UP;
		double tu = fu + length * Math.cos(angle), ty = fy + length * Math.sin(angle);
		double half = 1.0;   // the boom's lattice, 2 m square out of its 3 m foot
		double nu = -Math.sin(angle), ny = Math.cos(angle);   // across the boom, in its plane
		for (double a : new double[] {-half, half}) {
			for (double b : new double[] {-half, half}) {
				f.beam(yellow, fu + nu * a, fy + ny * a, b, tu + nu * a, ty + ny * a, b, 0.14);
			}
		}
		int bays = (int) Math.ceil(length / SECTION);
		for (int i = 0; i < bays; i++) {
			double t0 = (double) i / bays, t1 = (double) (i + 1) / bays;
			double u0 = fu + (tu - fu) * t0, y0 = fy + (ty - fy) * t0, u1 = fu + (tu - fu) * t1, y1 = fy + (ty - fy) * t1;
			f.beam(yellow, u0 + nu * -half, y0 + ny * -half, -half, u1 + nu * half, y1 + ny * half, -half, 0.06);
			f.beam(yellow, u0 + nu * -half, y0 + ny * -half, half, u1 + nu * half, y1 + ny * half, half, 0.06);
			f.beam(yellow, u0 + nu * half, y0 + ny * half, -half, u1 + nu * half, y1 + ny * half, half, 0.06);
			f.beam(yellow, u0 + nu * -half, y0 + ny * -half, -half, u1 + nu * -half, y1 + ny * -half, half, 0.06);
		}
		f.box(yellow, tu - 0.8, ty - 0.8, -1.0, tu + 0.8, ty + 0.4, 1.0);     // the boom head and its sheaves
		f.beam(rope, -1.0, frameTop, -0.6, tu, ty + 0.4, -0.6, 0.05);         // the pendants
		f.beam(rope, -1.0, frameTop, 0.6, tu, ty + 0.4, 0.6, 0.05);
		double r = s.radius;
		double hookTop = s.height + 2.2;
		f.beam(rope, r - 0.2, ty - 0.8, -0.2, r - 0.2, hookTop, -0.2, 0.05);
		f.beam(rope, r + 0.2, ty - 0.8, 0.2, r + 0.2, hookTop, 0.2, 0.05);
		f.box(yellow, r - 0.7, hookTop - 1.2, -0.5, r + 0.7, hookTop, 0.5);    // the hook block
		f.box(dark, r - 0.12, s.height + 1.0, -0.12, r + 0.12, hookTop - 1.2, 0.12);
		f.beam(rope, r, s.height + 1.0, 0, r - 0.8, s.height + 0.02, 0, 0.03);
		f.beam(rope, r, s.height + 1.0, 0, r + 0.8, s.height + 0.02, 0, 0.03);
	}

	// ---- geometry ----------------------------------------------------------------------------------------------------------------------
	/**
	 * Writes textured boxes and beams in the crane's frame: u out along the jib or boom, y up from the base, v across, turned about
	 * the slewing centre by the slewing angle (degrees clockwise from north). With {@link #fixed} set, u is east and v south.
	 */
	private static final class Frame {
		final PoseStack.Pose pose;
		final VertexConsumer v;
		final int light;
		final double cx, cz;
		final double fx, fz, rx, rz;   // the out and across directions in the world
		boolean fixed;

		Frame(PoseStack.Pose pose, VertexConsumer v, int light, double cx, double cz, double deg) {
			this.pose = pose;
			this.v = v;
			this.light = light;
			this.cx = cx;
			this.cz = cz;
			double a = Math.toRadians(deg);
			fx = Math.sin(a);
			fz = -Math.cos(a);
			rx = Math.cos(a);
			rz = Math.sin(a);
		}

		double[] world(double u, double y, double w) {
			return fixed ? new double[] {cx + u, y, cz + w} : new double[] {cx + fx * u + rx * w, y, cz + fz * u + rz * w};
		}

		/** An upright box from (u0, y0, w0) to (u1, y1, w1) in the frame. */
		void box(TextureAtlasSprite s, double u0, double y0, double w0, double u1, double y1, double w1) {
			double[][] c = new double[8][];
			for (int i = 0; i < 8; i++) {
				c[i] = world((i & 1) == 0 ? u0 : u1, (i & 2) == 0 ? y0 : y1, (i & 4) == 0 ? w0 : w1);
			}
			cuboid(s, c);
		}

		/** A square beam {@code t} m thick from one point to another in the frame. */
		void beam(TextureAtlasSprite s, double u0, double y0, double w0, double u1, double y1, double w1, double t) {
			double[] a = world(u0, y0, w0), b = world(u1, y1, w1);
			double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
			double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (len < 1e-6) {
				return;
			}
			dx /= len;
			dy /= len;
			dz /= len;
			// two directions across the beam
			double[] up = Math.abs(dy) > 0.9 ? new double[] {1, 0, 0} : new double[] {0, 1, 0};
			double[] e1 = norm(cross(new double[] {dx, dy, dz}, up));
			double[] e2 = cross(e1, new double[] {dx, dy, dz});
			double h = t / 2;
			double[][] c = new double[8][];
			for (int i = 0; i < 8; i++) {
				double[] base = (i & 1) == 0 ? a : b;
				double s1 = (i & 2) == 0 ? -h : h, s2 = (i & 4) == 0 ? -h : h;
				c[i] = new double[] {base[0] + e1[0] * s1 + e2[0] * s2, base[1] + e1[1] * s1 + e2[1] * s2, base[2] + e1[2] * s1 + e2[2] * s2};
			}
			cuboid(s, c);
		}

		/** Six quads of a box given by its eight corners: bit 0 the first axis, bit 1 the second, bit 2 the third. */
		void cuboid(TextureAtlasSprite s, double[][] c) {
			int[][] faces = {{0, 2, 6, 4}, {1, 5, 7, 3}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 1, 3, 2}, {4, 6, 7, 5}};
			for (int[] q : faces) {
				double[] n = norm(cross(sub(c[q[1]], c[q[0]]), sub(c[q[3]], c[q[0]])));
				// face the quad outward, whichever way the corners were turned
				double[] mid = new double[3];
				for (double[] p : c) {
					for (int k = 0; k < 3; k++) {
						mid[k] += p[k] / 8;
					}
				}
				boolean out = dot(n, sub(c[q[0]], mid)) >= 0;
				int[] order = out ? q : new int[] {q[3], q[2], q[1], q[0]};
				double[] nn = out ? n : new double[] {-n[0], -n[1], -n[2]};
				float[][] uv = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
				for (int k = 0; k < 4; k++) {
					double[] p = c[order[k]];
					float ny = (float) nn[1];
					float len = (float) Math.sqrt(nn[0] * nn[0] * 0.25 + (ny + 0.8) * (ny + 0.8) + nn[2] * nn[2] * 0.25);
					v.addVertex(pose, (float) p[0], (float) p[1], (float) p[2]).setColor(0xFFFFFFFF).setUv(s.getU(uv[k][0]), s.getV(uv[k][1]))
							.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
							.setNormal(pose, (float) nn[0] * 0.5F / len, (ny + 0.8F) / len, (float) nn[2] * 0.5F / len);
				}
			}
		}

		static double[] sub(double[] a, double[] b) {
			return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
		}

		static double dot(double[] a, double[] b) {
			return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
		}

		static double[] cross(double[] a, double[] b) {
			return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
		}

		static double[] norm(double[] a) {
			double l = Math.sqrt(dot(a, a));
			return l < 1e-12 ? new double[] {0, 1, 0} : new double[] {a[0] / l, a[1] / l, a[2] / l};
		}
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 256;
	}
}
