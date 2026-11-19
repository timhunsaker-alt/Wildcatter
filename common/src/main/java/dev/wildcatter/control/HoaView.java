package dev.wildcatter.control;

import dev.wildcatter.multiblock.BigParts;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

/**
 * The control key's overlay (Tim's answer C18, 2026-11-19): while a player holds the key, the server sends twice a second where each
 * HAND / OFF / AUTO selector within {@link #RADIUS} blocks stands, from {@link ControlDevice#hoaPositions} (any mod's devices), and the client
 * draws it over the device. No block state carries it: the selector lives in each device's own data.
 */
public final class HoaView {
	/** Blocks round the player whose selectors are shown. */
	public static final int RADIUS = 16;
	private static final int EVERY_TICKS = 10;

	private HoaView() {
	}

	public static boolean holdsKey(net.minecraft.world.entity.player.Player player) {
		for (InteractionHand hand : InteractionHand.values()) {
			if (player.getItemInHand(hand).getItem() instanceof ControlKeyItem) {
				return true;
			}
		}
		return false;
	}

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % EVERY_TICKS != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (holdsKey(player)) {
				dev.wildcatter.platform.Platform.INSTANCE.sendToPlayer(player, new PlcNet.Payload(collect((ServerLevel) player.level(), player.blockPosition())));
			}
		}
	}

	/** Every selector near {@code centre}: its label's anchor and position, as the packet carries them. */
	public static CompoundTag collect(ServerLevel level, BlockPos centre) {
		ListTag list = new ListTag();
		int r = RADIUS;
		for (int cx = (centre.getX() - r) >> 4; cx <= (centre.getX() + r) >> 4; cx++) {
			for (int cz = (centre.getZ() - r) >> 4; cz <= (centre.getZ() + r) >> 4; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (!(be instanceof ControlDevice device) || be.isRemoved() || be.getBlockPos().distSqr(centre) > (double) r * r) {
						continue;
					}
					Map<BlockPos, String> positions = device.hoaPositions();
					for (Map.Entry<BlockPos, String> entry : positions.entrySet()) {
						Vec3 at = anchor(level, be, entry.getKey());
						CompoundTag one = new CompoundTag();
						one.putDouble("x", at.x);
						one.putDouble("y", at.y);
						one.putDouble("z", at.z);
						one.putString("h", entry.getValue());
						list.add(one);
					}
				}
			}
		}
		CompoundTag tag = new CompoundTag();
		tag.put("hoaView", list);
		return tag;
	}

	/**
	 * Where a selector's label goes: over the top of the device for its main cell (the whole of a true-size part), or out from the front of
	 * its own cell for one of several (an MCC's buckets).
	 */
	static Vec3 anchor(ServerLevel level, BlockEntity be, BlockPos cell) {
		BlockState state = be.getBlockState();
		if (cell.equals(be.getBlockPos())) {
			List<BlockPos> cells = BigParts.cellsAt(level, be.getBlockPos());
			double x = 0;
			double z = 0;
			int top = cell.getY();
			for (BlockPos c : cells) {
				x += c.getX() + 0.5;
				z += c.getZ() + 0.5;
				top = Math.max(top, c.getY());
			}
			return new Vec3(x / cells.size(), top + 1.35, z / cells.size());
		}
		Vec3 centre = Vec3.atCenterOf(cell);
		if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			Direction front = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
			return centre.add(front.getStepX() * 0.62, 0, front.getStepZ() * 0.62);
		}
		return centre.add(0, 0.7, 0);
	}
}
