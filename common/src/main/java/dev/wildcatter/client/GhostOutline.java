package dev.wildcatter.client;

import dev.wildcatter.multiblock.BigPart;
import dev.wildcatter.multiblock.BigParts;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Holding a true-size part (docs/multiblocks/README.md, "Big parts"), you see where it would go before you click: a green box
 * round all its cells where it fits, red where something is in the way.
 */
public final class GhostOutline {
	private static final int FITS = 0xFF44DD55;
	private static final int BLOCKED = 0xFFEE4433;

	private GhostOutline() {
	}

	public static void draw(Minecraft client) {
		if (client.player == null || client.level == null || !(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
			return;
		}
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = client.player.getItemInHand(hand);
			if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof BigPart part) {
				BlockPlaceContext context = part.snap(new BlockPlaceContext(client.player, hand, stack, hit));
				BlockPos main = context.getClickedPos();
				BlockState state = item.getBlock().getStateForPlacement(context);
				boolean fits = state != null;
				if (state == null) {
					state = part.placementState(context);
				}
				AABB box = new AABB(main);
				for (BlockPos cell : BigParts.cells(main, state)) {
					box = box.minmax(new AABB(cell));
				}
				Gizmos.cuboid(box.inflate(0.01), GizmoStyle.stroke(fits ? FITS : BLOCKED, 2.5F));
				return;
			}
		}
	}
}
