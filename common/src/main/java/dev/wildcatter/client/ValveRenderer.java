package dev.wildcatter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildcatter.control.ValveBlock;
import dev.wildcatter.control.ValveBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A valve's moving part, at the opening the server sends (ValveBlockEntity). A control valve is a globe valve on a diaphragm actuator: its
 * stem rises in the open yoke as it opens, a yellow travel indicator on it against the yoke. An on-off valve and a motor-operated valve
 * are quarter-turn valves: the position indicator on top of the actuator turns through 90 degrees, across the pipe shut and along it
 * open, red shut and green open as their indicators are painted.
 */
public class ValveRenderer implements BlockEntityRenderer<ValveBlockEntity, ValveRenderer.State> {
	public static class State extends BlockEntityRenderState {
		double position;
		ValveBlock.Type type = ValveBlock.Type.CONTROL;
		/** The pipe runs east-west through it. */
		boolean alongX;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(ValveBlockEntity valve, State state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(valve, state, crumbling);
		long now = System.nanoTime();
		valve.shownPosition = Boxes.ease(valve.shownPosition, valve.position(), valve.shownAt, now);
		valve.shownAt = now;
		state.position = valve.shownPosition;
		BlockState block = valve.getBlockState();
		state.type = block.getBlock() instanceof ValveBlock v ? v.type() : ValveBlock.Type.CONTROL;
		boolean x = dev.wildcatter.block.PipeBlock.joined(block, net.minecraft.core.Direction.EAST) || dev.wildcatter.block.PipeBlock.joined(block, net.minecraft.core.Direction.WEST);
		boolean z = dev.wildcatter.block.PipeBlock.joined(block, net.minecraft.core.Direction.NORTH) || dev.wildcatter.block.PipeBlock.joined(block, net.minecraft.core.Direction.SOUTH);
		state.alongX = x || !z;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		TextureAtlasSprite steel = Boxes.mat("mat_stainless");
		TextureAtlasSprite paint = Boxes.mat("mat_polymer_grey");
		int light = state.lightCoords;
		double open = Math.max(0, Math.min(1, state.position));
		if (state.type == ValveBlock.Type.CONTROL) {
			// the stem through the yoke (11 to 14 px), and the travel indicator clamped on it: down shut, up full open
			double y = 11.1 + 2.2 * open;
			collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (p, v) -> {
				Boxes.px(p, v, steel, 7.5, 11, 7.5, 8.5, 14, 8.5, 0xFFFFFFFF, light);
				Boxes.px(p, v, paint, 6.2, y, 6.2, 9.8, y + 0.7, 9.8, 0xFFF2C21B, light);
			});
			return;
		}
		// a quarter-turn indicator on the actuator's top: an arrow across the pipe when shut, along it when open
		double top = state.type == ValveBlock.Type.MOTOR_OPERATED ? 16 : 15;
		float turn = (float) (90 * open) + (state.alongX ? 0 : 90);
		int r = (int) (0xD0 * (1 - open) + 0x2A * open), g = (int) (0x30 * (1 - open) + 0xB0 * open), b = (int) (0x2A * (1 - open) + 0x48 * open);
		int colour = 0xFF000000 | r << 16 | g << 8 | b;
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, turn + 90);   // at 0 open the arrow lies across the pipe
		pose.translate(-0.5F, 0.0F, -0.5F);
		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (p, v) -> {
			Boxes.px(p, v, steel, 7, top, 7, 9, top + 0.4, 9, 0xFFFFFFFF, light);                   // its boss
			Boxes.px(p, v, paint, 3.5, top + 0.4, 7, 12.5, top + 0.9, 9, colour, light);            // the arrow's shaft, along x before turning
			Boxes.px(p, v, paint, 10.5, top + 0.4, 6, 12.5, top + 0.9, 10, colour, light);          // and its head
		});
		pose.popPose();
	}
}
