package dev.wildcatter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.wildcatter.Wildcatter;
import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.control.PlcCabinetBlockEntity;
import dev.wildcatter.control.PlcModuleItem;
import dev.wildcatter.control.RemoteIoDropBlockEntity;
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

/**
 * The modules actually seated, drawn in a PLC cabinet's or a remote drop's door window (docs/multiblocks/plc.md, "drawn with the modules
 * actually seated"): two DIN rails, the CPU (or the drop's adapter) first with its RUN lamp and memory card, then each module in its slot,
 * its label strip coloured by kind; an empty slot shows the bare rail. Drawn with the mod's existing materials, no new texture.
 */
public class PlcRackRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, PlcRackRenderer.State> {
	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		boolean drop;
		int cpu = -1;
		final int[] modules = new int[PlcCabinetBlockEntity.MODULE_SLOTS];
		int lamp;
		boolean card;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(T entity, State state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
		BlockEntityRenderState.extractBase(entity, state, breakProgress);
		state.facing = entity.getBlockState().hasProperty(BigPartBlock.FACING) ? entity.getBlockState().getValue(BigPartBlock.FACING) : Direction.NORTH;
		if (entity instanceof PlcCabinetBlockEntity cabinet) {
			state.drop = false;
			state.cpu = cabinet.cpuModule() == null ? -1 : cabinet.cpuModule().ordinal();
			for (int i = 0; i < state.modules.length; i++) {
				state.modules[i] = cabinet.module(i) == null ? -1 : cabinet.module(i).ordinal();
			}
			state.lamp = cabinet.lamp();
			state.card = cabinet.hasCard();
		} else if (entity instanceof RemoteIoDropBlockEntity rack) {
			state.drop = true;
			state.cpu = -1;
			for (int i = 0; i < state.modules.length; i++) {
				state.modules[i] = rack.module(i) == null ? -1 : rack.module(i).ordinal();
			}
			state.lamp = 0;
			state.card = false;
		}
	}

	private static TextureAtlasSprite sprite(String name) {
		return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(Wildcatter.id("block/mat/" + name));
	}

	/** A module's label colour: the kind of signal it takes. */
	private static int labelColour(PlcModuleItem.Type type) {
		return switch (type) {
			case DI16 -> 0xFF3C9A48;
			case DO16 -> 0xFFD0612A;
			case AI8 -> 0xFF3A6FC4;
			case AO4 -> 0xFF8A55C0;
			case RTD4 -> 0xFFD8B83A;
			case HSC2 -> 0xFF3AB8C0;
			case COMM -> 0xFFE8E8E8;
			default -> 0xFF9AA0A8;
		};
	}

	// the window in the door, in the north-facing model's pixels: x 4.48-11.52, y 13.6-23.2 (over both cells), its face at z 3.152
	private static final float X0 = 4.48F / 16, X1 = 11.52F / 16, Y0 = 13.6F / 16, Y1 = 23.2F / 16, FACE = 3.152F / 16;

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		TextureAtlasSprite polymer = sprite("mat_paint_ansi61");
		TextureAtlasSprite galv = sprite("mat_galvanized");
		TextureAtlasSprite dark = sprite("mat_oil_dark");
		TextureAtlasSprite bulb = sprite("mat_bulb_lit");
		TextureAtlasSprite brass = sprite("mat_brass");
		pose.pushPose();
		pose.translate(0.5F, 0.5F, 0.5F);
		pose.rotateDegrees(Axis.YP, -state.facing.toYRot() + 180.0F);
		pose.translate(-0.5F, -0.5F, -0.5F);
		int light = state.lightCoords;
		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (p, v) -> {
			float px = 1F / 16;
			// the back of the enclosure seen through the window, then the two rails
			box(p, v, dark, X0, Y0, FACE - 0.01F * px, X1, Y1, FACE, 0xFF2A2C30, light);
			float rowH = (Y1 - Y0 - 1.2F * px) / 2;
			float[] rowY = {Y0 + rowH + 0.9F * px, Y0 + 0.3F * px};   // the top rail first
			for (float ry : rowY) {
				box(p, v, galv, X0 + 0.2F * px, ry + rowH / 2 - 0.35F * px, FACE - 0.12F * px, X1 - 0.2F * px, ry + rowH / 2 + 0.35F * px, FACE - 0.01F * px, 0xFFFFFFFF, light);
			}
			float headW = 1.5F * px;
			float slotW = (X1 - X0 - 0.4F * px - headW) / 6;
			// the CPU or the drop's adapter, on the top rail's left
			float hx = X0 + 0.2F * px;
			float top = rowY[0];
			if (state.drop || state.cpu >= 0) {
				box(p, v, polymer, hx, top, FACE - 0.9F * px, hx + headW - 0.06F * px, top + rowH, FACE - 0.12F * px, state.drop ? 0xFF9AA2AC : 0xFF5A6A80, light);
				int lampColour = switch (state.lamp) {
					case 1 -> 0xFF40FF50;
					case 2 -> 0xFFFFB020;
					case 3 -> 0xFFFF3030;
					default -> 0xFF303030;
				};
				int lampLight = state.lamp > 0 ? 0xF000F0 : light;
				box(p, v, bulb, hx + 0.25F * px, top + rowH - 0.75F * px, FACE - 1.0F * px, hx + 0.6F * px, top + rowH - 0.4F * px, FACE - 0.9F * px, lampColour, lampLight);
				if (state.cpu == PlcModuleItem.Type.CPU_LARGE.ordinal()) {
					box(p, v, polymer, hx + 0.8F * px, top + rowH - 0.75F * px, FACE - 1.0F * px, hx + 1.2F * px, top + rowH - 0.4F * px, FACE - 0.9F * px, 0xFFC8C8C8, light);   // the large CPU's second port
				}
				if (state.card) {
					box(p, v, brass, hx + 0.3F * px, top + 0.5F * px, FACE - 1.05F * px, hx + 1.1F * px, top + 0.8F * px, FACE - 0.9F * px, 0xFFFFFFFF, light);
				}
			}
			for (int slot = 0; slot < state.modules.length; slot++) {
				if (state.modules[slot] < 0) {
					continue;
				}
				PlcModuleItem.Type type = PlcModuleItem.Type.values()[state.modules[slot]];
				int row = slot / 6;
				float x = X0 + 0.2F * px + headW + (slot % 6) * slotW;
				float y = rowY[row];
				box(p, v, polymer, x, y, FACE - 0.8F * px, x + slotW - 0.06F * px, y + rowH, FACE - 0.12F * px, 0xFFB4B8BE, light);   // a light grey body against the dark enclosure
				box(p, v, polymer, x + 0.05F * px, y + rowH - 1.4F * px, FACE - 0.85F * px, x + slotW - 0.11F * px, y + rowH - 0.2F * px, FACE - 0.8F * px, labelColour(type), light);
			}
		});
		pose.popPose();
	}

	/** Laid out left to right as a person at the door sees it, which is the model's x from high to low. */
	private static void box(PoseStack.Pose pose, VertexConsumer v, TextureAtlasSprite s, float left, float y0, float z0, float right, float y1, float z1, int color, int light) {
		float x0 = X0 + X1 - right;
		float x1 = X0 + X1 - left;
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
