package dev.wildcatter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildcatter.kinetic.Kinetic;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * Draws the turning parts of a {@link Kinetic} machine (docs/kinetics-and-sound.md): each rotor's block model, turned about its
 * pivot and axis by the shaft angle, and turned again with the machine's facing. The machine's own block model stays still.
 */
public class KineticRenderer<T extends BlockEntity & Kinetic> implements BlockEntityRenderer<T, KineticRenderer.State> {
	private static final BlockDisplayContext CONTEXT = BlockDisplayContext.create();

	public static class State extends BlockEntityRenderState {
		final List<BlockModelRenderState> models = new ArrayList<>();
		final List<Kinetic.Rotor> rotors = new ArrayList<>();
		float angle;
		Direction facing = Direction.NORTH;
	}

	private final BlockModelResolver resolver;

	public KineticRenderer(BlockEntityRendererProvider.Context context) {
		this.resolver = context.blockModelResolver();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(T machine, State state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(machine, state, crumbling);
		state.facing = machine.facing();
		state.angle = machine.spin().angle(System.nanoTime());
		state.rotors.clear();
		state.rotors.addAll(machine.rotors());
		while (state.models.size() < state.rotors.size()) {
			state.models.add(new BlockModelRenderState());
		}
		for (int i = 0; i < state.rotors.size(); i++) {
			resolver.update(state.models.get(i), state.rotors.get(i).model(), CONTEXT);
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		for (int i = 0; i < state.rotors.size(); i++) {
			Kinetic.Rotor rotor = state.rotors.get(i);
			pose.pushPose();
			// turn the whole machine frame to its facing about the block centre
			pose.translate(0.5F, 0.5F, 0.5F);
			pose.rotateDegrees(Axis.YP, -state.facing.toYRot() + 180.0F);
			pose.translate(-0.5F, -0.5F, -0.5F);
			// then the rotor about its own pivot and axis
			Vector3f p = rotor.pivot();
			pose.translate(p.x, p.y, p.z);
			float degrees = state.angle * rotor.ratio();
			pose.rotateDegrees(switch (rotor.axis()) {
				case X -> Axis.XP;
				case Y -> Axis.YP;
				case Z -> Axis.ZP;
			}, degrees);
			pose.translate(-0.5F, -0.5F, -0.5F);   // rotor models are drawn centred on the block
			state.models.get(i).submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			pose.popPose();
		}
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}
}
