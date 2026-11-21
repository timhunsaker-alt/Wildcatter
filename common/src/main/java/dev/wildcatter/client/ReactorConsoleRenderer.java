package dev.wildcatter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildcatter.Tuning;
import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.block.ReactorConsoleBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The reactor console's rod position indicators (docs/multiblocks/reactor.md): a vertical board on top of the console, as the rod control
 * section of a real main control board, with one column of lamps per RCCA, the four shutdown bank rods and then bank D. Each column lights
 * green up to its rod's position (12 lamps over the 228 steps); its red ROD BOTTOM lamp lights when the rod is fully in. Across the top
 * the REACTOR TRIP annunciator flashes red while the reactor is tripped. Driven by the positions the server sends (the console, from the
 * vessel's step), eased between updates so a rod drop is seen falling.
 */
public class ReactorConsoleRenderer implements BlockEntityRenderer<ReactorConsoleBlockEntity, ReactorConsoleRenderer.State> {
	private static final int LAMPS = 12;

	public static class State extends BlockEntityRenderState {
		double shutdown, bankD;
		boolean tripped;
		float turn;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(ReactorConsoleBlockEntity console, State state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(console, state, crumbling);
		long now = System.nanoTime();
		console.easedShutdown = Boxes.ease(console.easedShutdown, console.shownShutdown(), console.easedAt, now);
		console.easedBankD = Boxes.ease(console.easedBankD, console.shownBankD(), console.easedAt, now);
		console.easedAt = now;
		state.shutdown = console.easedShutdown / Tuning.ROD_STEPS;
		state.bankD = console.easedBankD / Tuning.ROD_STEPS;
		state.tripped = console.shownTripped();
		Direction facing = console.getBlockState().hasProperty(BigPartBlock.FACING) ? console.getBlockState().getValue(BigPartBlock.FACING) : Direction.NORTH;
		state.turn = -facing.toYRot() + 180;   // drawn facing north (yRot 180)
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		TextureAtlasSprite paint = Boxes.mat("mat_polymer_grey");
		TextureAtlasSprite lamp = Boxes.mat("mat_bulb_lit");
		boolean flashOn = state.tripped && (System.currentTimeMillis() / 300) % 2 == 0;
		pose.pushPose();
		pose.translate(0.5F, 0.5F, 0.5F);
		pose.rotateDegrees(Axis.YP, state.turn);
		pose.translate(-0.5F, -0.5F, -0.5F);
		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (p, v) -> {
			int lit = 0xF000F0;
			// the board: on top of the console's upper panel (its front at z 9.6 px, its top at 30.4 px), the three cells wide
			Boxes.px(p, v, paint, 4, 30.4, 10.4, 44, 50, 14.4, 0xFF3A3E42, state.lightCoords);
			// the REACTOR TRIP annunciator across its top
			Boxes.px(p, v, lamp, 14, 45, 10.0, 34, 48.5, 10.4, flashOn ? 0xFFFF3020 : 0xFF401010, flashOn ? lit : state.lightCoords);
			// five columns: the shutdown bank's four, a gap, bank D
			double[] xs = {8, 13, 18, 23, 34};
			for (int rod = 0; rod < 5; rod++) {
				double at = rod < 4 ? state.shutdown : state.bankD;
				double x0 = xs[rod], x1 = x0 + 3;
				int shown = (int) Math.round(at * LAMPS);
				for (int k = 0; k < LAMPS; k++) {
					boolean on = k < shown;
					double y0 = 35.5 + k * 0.75;
					Boxes.px(p, v, lamp, x0, y0, 10.0, x1, y0 + 0.5, 10.4, on ? 0xFF40FF60 : 0xFF103018, on ? lit : state.lightCoords);
				}
				boolean bottom = at < 0.5 / Tuning.ROD_STEPS;
				Boxes.px(p, v, lamp, x0, 32.5, 10.0, x1, 34.5, 10.4, bottom ? 0xFFFF4030 : 0xFF401010, bottom ? lit : state.lightCoords);
			}
		});
		pose.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public boolean shouldRender(ReactorConsoleBlockEntity entity, Vec3 cameraPos) {
		return true;
	}
}
