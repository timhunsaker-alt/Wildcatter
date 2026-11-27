package dev.wildcatter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.wildcatter.block.AttachedGear;
import dev.wildcatter.block.CableBlock;
import dev.wildcatter.block.MultiCableBlock;
import dev.wildcatter.block.PlugAdapterBlock;
import dev.wildcatter.block.ReceptacleBlock;
import dev.wildcatter.block.WireShape;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Where a wire or cable meets a machine (WireShape#terminals), what it lands on:
 * <ul>
 * <li>at a machine with terminals on its faces, a wire on a terminal block, its conductor in a brass lug with the clamp screw; a cable
 * through a cable gland, its locknut against the machine and its sealing nut round the sheath;</li>
 * <li>at gear mounted on a wall, floor or ceiling of its cell (a receptacle, a lamp, a heater: AttachedGear), the wire carried on
 * across that cell to the gear's box, entering it through a connector, so it plainly reaches the box rather than stopping in the air.</li>
 * </ul>
 * All of it grows with the conductor's gauge. A run with no machine beside it draws nothing here ({@link #shouldRender} is false), so
 * long runs cost only that check.
 */
public class TerminalRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, TerminalRenderer.State> {
	public static class State extends BlockEntityRenderState {
		int terminals;
		double width;
		boolean cable;
		int colour;
		/** The conductor's own texture, for the lead (the wire's colour band, the cable's sheath). */
		net.minecraft.resources.@Nullable Identifier texture;
		/** For each side, the face of the next cell its gear is mounted on (a lead runs to it), or null for a terminal on the face. */
		final Direction[] gear = new Direction[6];
		final double[] gearHeight = new double[6];
		/** Half the height of that gear's box, in pixels: a lead comes up into its bottom. */
		final double[] gearHalf = new double[6];
		/** For each side, how far the machine's body stands back from the cell face, in pixels: the conductor is carried on to it. */
		final double[] gap = new double[6];
	}

	private static final double[] WIRE_WIDTH = {2, 3, 4, 5};
	private static final double[] CABLE_WIDTH = {4, 5, 6, 7};
	private static final double[] SINGLE_WIDTH = {3, 4, 5, 6};

	@Override
	public State createRenderState() {
		return new State();
	}

	private static int terminals(BlockEntity entity) {
		return entity.getLevel() == null ? 0 : WireShape.terminals(entity.getLevel(), entity.getBlockPos(), entity.getBlockState());
	}

	@Override
	public boolean shouldRender(T entity, Vec3 cameraPos) {
		return Vec3.atCenterOf(entity.getBlockPos()).closerThan(cameraPos, getViewDistance()) && terminals(entity) != 0;
	}

	@Override
	public void extractRenderState(T entity, State state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(entity, state, crumbling);
		BlockState block = entity.getBlockState();
		state.terminals = terminals(entity);
		state.texture = texture(block);
		int gauge = WireShape.gauge(block).ordinal();
		if (block.getBlock() instanceof MultiCableBlock cable) {
			state.cable = true;
			state.width = cable.busway() ? 8 : (cable.singlePhase() ? SINGLE_WIDTH : CABLE_WIDTH)[gauge];
			state.colour = cable.singlePhase() ? 0xFFE4DCC4 : 0xFF34343A;
		} else {
			state.cable = false;
			state.width = WIRE_WIDTH[gauge];
			state.colour = block.hasProperty(CableBlock.CONDUCTOR) ? switch (block.getValue(CableBlock.CONDUCTOR)) {
				case A -> 0xFFB8382C;
				case B -> 0xFFD9822B;
				case C -> 0xFF3A5FB8;
				case N -> 0xFF2C2C30;
				case G -> 0xFF3C9A48;
			} : 0xFF2C2C30;
		}
		for (Direction side : Direction.values()) {
			state.gear[side.ordinal()] = null;
			state.gap[side.ordinal()] = 0;
			if ((state.terminals & 1 << side.ordinal()) == 0 || entity.getLevel() == null) {
				continue;
			}
			net.minecraft.core.BlockPos next = entity.getBlockPos().relative(side);
			BlockState there = entity.getLevel().getBlockState(next);
			// a machine drawn at true size may not fill its cell: find its surface on the conductor's line
			net.minecraft.world.phys.shapes.VoxelShape body = there.getShape(entity.getLevel(), next);
			if (!body.isEmpty()) {
				Vec3 from = Vec3.atCenterOf(entity.getBlockPos()).add(side.getStepX() * 0.5, side.getStepY() * 0.5, side.getStepZ() * 0.5);
				net.minecraft.world.phys.BlockHitResult hit = body.clip(from, from.add(side.getStepX(), side.getStepY(), side.getStepZ()), next);
				if (hit != null) {
					state.gap[side.ordinal()] = Math.min(14, hit.getLocation().distanceTo(from) * 16);
				}
			}
			if (there.hasProperty(AttachedGear.ATTACH) && !(there.getBlock() instanceof PlugAdapterBlock)) {
				Direction mount = there.getValue(AttachedGear.ATTACH);
				state.gear[side.ordinal()] = mount;
				// the box's middle on a wall: a receptacle low (0.4 m in its cell), a lamp high (0.6 m), other gear midway
				state.gearHeight[side.ordinal()] = mount == Direction.DOWN ? 0.03 : mount == Direction.UP ? 0.97
						: there.getBlock() instanceof ReceptacleBlock ? 0.4 : there.getBlock() instanceof dev.wildcatter.block.LoadBlock ? 0.6 : 0.5;
				state.gearHalf[side.ordinal()] = there.getBlock() instanceof ReceptacleBlock ? 1.7 : there.getBlock() instanceof dev.wildcatter.block.LoadBlock ? 1.0 : 1.5;
			}
		}
	}

	/** The texture a wire or cable is drawn in (tools/wire_look.py): {base}_{conductor}[_{wear}] for wire, the sheath for cable. */
	private static net.minecraft.resources.@Nullable Identifier texture(BlockState block) {
		String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block.getBlock()).getPath();
		if (block.getBlock() instanceof MultiCableBlock cable) {
			return cable.singlePhase() ? dev.wildcatter.Wildcatter.id("block/single_phase_cable_placeholder") : cable.busway() ? null : dev.wildcatter.Wildcatter.id("block/" + path);
		}
		if (block.hasProperty(CableBlock.CONDUCTOR) && (path.endsWith("_hot") || path.endsWith("_neutral"))) {
			String base = path.substring(0, path.lastIndexOf('_'));
			return dev.wildcatter.Wildcatter.id("block/" + base + "_" + block.getValue(CableBlock.CONDUCTOR).getSerializedName()
					+ (block.hasProperty(CableBlock.WEAR) ? "_" + block.getValue(CableBlock.WEAR) : ""));
		}
		return null;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.terminals == 0) {
			return;
		}
		TextureAtlasSprite brass = Boxes.mat("mat_brass");
		TextureAtlasSprite body = Boxes.mat("mat_polymer_grey");
		TextureAtlasSprite steel = Boxes.mat("mat_galvanized");
		TextureAtlasSprite rubber = Boxes.mat("mat_rubber");
		int light = state.lightCoords;
		double w = state.width;
		TextureAtlasSprite own = body;
		int ownColour = state.colour;
		if (state.texture != null) {
			own = net.minecraft.client.Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS).getSprite(state.texture);
			ownColour = 0xFFFFFFFF;
		}
		TextureAtlasSprite sheath = own;
		int sheathColour = ownColour;
		for (Direction side : Direction.values()) {
			if ((state.terminals & 1 << side.ordinal()) == 0) {
				continue;
			}
			Direction mount = state.gear[side.ordinal()];
			if (mount != null) {
				double height = state.gearHeight[side.ordinal()];
				double half = state.gearHalf[side.ordinal()];
				collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS),
						(p, v) -> lead(p, v, sheath, steel, side, mount, height, half, w, sheathColour, light));
				continue;
			}
			pose.pushPose();
			pose.translate(0.5F, 0.5F, 0.5F);
			switch (side) {
				case SOUTH -> pose.rotateDegrees(Axis.YP, 180);
				case WEST -> pose.rotateDegrees(Axis.YP, 90);
				case EAST -> pose.rotateDegrees(Axis.YP, -90);
				case UP -> pose.rotateDegrees(Axis.XP, 90);
				case DOWN -> pose.rotateDegrees(Axis.XP, -90);
				default -> { }
			}
			pose.translate(-0.5F, -0.5F, -0.5F);
			// drawn facing north: the cell's face is z = 0, the conductor comes in along z; the machine's body is gap pixels further on
			double gap = state.gap[side.ordinal()];
			TextureAtlasSprite carried = sheath;
			int carriedColour = sheathColour;
			collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (p, v) -> {
				if (gap > 0.05) {
					square(p, v, carried, w, -gap, 0.01, carriedColour, light);   // carried on to the body
				}
			});
			pose.translate(0, 0, -gap / 16);
			collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (p, v) -> {
				if (state.cable) {
					double nut = w + 2;
					double gland = w + 1;
					square(p, v, steel, nut, 0, 0.7, 0xFFD0D0D0, light);                  // locknut on the machine
					square(p, v, brass, gland, 0.7, 2.0, 0xFFFFFFFF, light);              // the gland's body
					square(p, v, steel, gland + 0.4, 2.0, 2.8, 0xFFE0E0E0, light);        // its sealing nut
					square(p, v, rubber, w + 0.4, 2.8, 3.2, 0xFFFFFFFF, light);           // the seal round the sheath
				} else {
					double block = w + 2.4;
					square(p, v, body, block, 0, 1.2, 0xFF505258, light);                  // the terminal block
					square(p, v, brass, w + 0.8, 1.2, 2.4, 0xFFFFFFFF, light);              // the lug crimped on the conductor
					Boxes.px(p, v, brass, 7.4, 8 + block / 2, 0.3, 8.6, 8 + block / 2 + 0.5, 0.9, 0xFFFFFFFF, light);   // its clamp screw
				}
			});
			pose.popPose();
		}
	}

	private static void square(PoseStack.Pose p, VertexConsumer v, TextureAtlasSprite s, double size, double z0, double z1, int colour, int light) {
		Boxes.px(p, v, s, 8 - size / 2, 8 - size / 2, z0, 8 + size / 2, 8 + size / 2, z1, colour, light);
	}

	/**
	 * The conductor carried on from this cell's face across the next cell to the box of the gear mounted there: along the side it leaves
	 * by, then up or down to the box's height, then across, ending in a connector at the box. In block pixels from this cell.
	 */
	private static void lead(PoseStack.Pose p, VertexConsumer v, TextureAtlasSprite sheath, TextureAtlasSprite steel, Direction side, Direction mount,
			double height, double box, double w, int colour, int light) {
		double[] from = {8 + side.getStepX() * 8, 8 + side.getStepY() * 8, 8 + side.getStepZ() * 8};
		double inset = w / 2 + 0.1;   // the lead runs along the wall, just off it
		double[] to = {8 + side.getStepX() * 16 + mount.getStepX() * (8 - inset), 8 + side.getStepY() * 16 + mount.getStepY() * (8 - inset),
				8 + side.getStepZ() * 16 + mount.getStepZ() * (8 - inset)};
		boolean fromAbove = side == Direction.DOWN;   // a run coming down to it enters the box's top, any other its bottom
		if (mount.getAxis().isHorizontal()) {
			to[1] = side.getStepY() * 16 + height * 16 + (fromAbove ? box + h(w) : -box - h(w));   // just over or under the box, on the wall
		} else if (side.getAxis().isHorizontal()) {
			// gear on a floor or ceiling, the run beside it: along that floor to the box's side
			to[side.getAxis().ordinal()] -= side.getAxisDirection().getStep() * (box + h(w));
		} else {
			to[1] -= mount.getStepY() * (box * 2 + h(w));   // coming straight down onto (or up to) it: to its top
		}
		int[] order = side.getAxis() == Direction.Axis.Y ? new int[] {1, 0, 2} : new int[] {side.getAxis().ordinal(), 1, 3 - side.getAxis().ordinal() - 1};
		double[] at = from.clone();
		double h = h(w);
		for (int axis : order) {
			double[] next = at.clone();
			next[axis] = to[axis];
			if (Math.abs(next[axis] - at[axis]) > 1e-6) {
				double[] lo = new double[3];
				double[] hi = new double[3];
				for (int k = 0; k < 3; k++) {
					lo[k] = Math.min(at[k], next[k]) - (k == axis ? 0 : h);
					hi[k] = Math.max(at[k], next[k]) + (k == axis ? 0 : h);
				}
				// overlap the corners so a bend has no gap
				lo[axis] -= h;
				hi[axis] += h;
				Boxes.px(p, v, sheath, lo[0], lo[1], lo[2], hi[0], hi[1], hi[2], colour, light);
			}
			at = next;
		}
		// up into the box's bottom, through a connector (on a wall); straight into it on a floor or ceiling
		double c = h + 0.35;
		if (mount.getAxis().isHorizontal()) {
			double s = fromAbove ? -1 : 1;   // toward the box
			double y0 = to[1] + s * h;
			Boxes.px(p, v, steel, to[0] - c, Math.min(y0, y0 + s * 0.6), to[2] - c, to[0] + c, Math.max(y0, y0 + s * 0.6), to[2] + c, 0xFFD8D8D8, light);
		} else if (side.getAxis().isHorizontal()) {
			int a = side.getAxis().ordinal();
			double[] lo = {to[0] - c, to[1] - c, to[2] - c};
			double[] hi = {to[0] + c, to[1] + c, to[2] + c};
			double edge = to[a] + side.getAxisDirection().getStep() * h;
			lo[a] = Math.min(edge, edge + side.getAxisDirection().getStep() * 0.6);
			hi[a] = Math.max(edge, edge + side.getAxisDirection().getStep() * 0.6);
			Boxes.px(p, v, steel, lo[0], lo[1], lo[2], hi[0], hi[1], hi[2], 0xFFD8D8D8, light);
		}
	}

	private static double h(double w) {
		return w / 2;
	}
}
