package dev.wildcatter.item;

import dev.wildcatter.Tuning;
import dev.wildcatter.block.BigPartBlockEntity;
import dev.wildcatter.block.GuyAnchorBlockEntity;
import dev.wildcatter.block.PoleLoad;
import dev.wildcatter.block.WoodPoleBlock;
import dev.wildcatter.multiblock.BigParts;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A coil of 3/8 in EHS guy strand (protection-gaps 20): right-click a wood pole where the guy is to be clamped (near its top, where the
 * line pulls), then a guy anchor (either order). The coil pays out a metre for each metre of guy.
 */
public class GuyStrandItem extends Item {
	private record Pending(ResourceKey<Level> dimension, BlockPos pos) {
	}

	/** The first end each player has clicked: only ever a moment between two clicks. */
	private static final Map<UUID, Pending> PENDING = new HashMap<>();

	/** A pole holds at most this many guys. */
	public static final int MAX_GUYS = 4;

	public static void clearAll() {
		PENDING.clear();
	}

	public GuyStrandItem(Properties properties) {
		super(properties);
	}

	private static boolean pole(Level level, BlockPos pos) {
		return level.getBlockState(pos).getBlock() instanceof WoodPoleBlock;
	}

	private static boolean anchor(Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof GuyAnchorBlockEntity;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!(context.getLevel() instanceof ServerLevel level) || !(context.getPlayer() instanceof ServerPlayer player)) {
			return InteractionResult.PASS;
		}
		BlockPos pos = context.getClickedPos();
		if (!pole(level, pos) && !anchor(level, pos)) {
			if (level.getBlockState(pos).is(dev.wildcatter.WildcatterBlocks.POLE)) {
				player.sendOverlayMessage(Component.literal("Guy a true-size wood pole; the old one-block pole sections take no guys."));
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		}
		Pending first = PENDING.get(player.getUUID());
		boolean firstOk = first != null && first.dimension == level.dimension() && (pole(level, first.pos) || anchor(level, first.pos));
		if (!firstOk || first.pos.equals(pos) || pole(level, first.pos) == pole(level, pos)) {
			if (firstOk && first.pos.equals(pos)) {
				PENDING.remove(player.getUUID());
				player.sendOverlayMessage(Component.literal("Cancelled."));
				return InteractionResult.SUCCESS;
			}
			PENDING.put(player.getUUID(), new Pending(level.dimension(), pos.immutable()));
			player.sendOverlayMessage(Component.literal(pole(level, pos) ? "Guy clamp point chosen on the pole. Right-click the guy anchor."
					: "Guy anchor chosen. Right-click the pole where the guy is to be clamped, near its top."));
			return InteractionResult.SUCCESS;
		}
		int left = context.getItemInHand().getMaxDamage() - context.getItemInHand().getDamageValue();
		Result result = pole(level, pos) ? connect(level, pos, first.pos, left) : connect(level, first.pos, pos, left);
		player.sendOverlayMessage(Component.literal(result.message()));
		if (result.ok()) {
			context.getItemInHand().hurtAndBreak(result.length(), player, context.getHand());
			PENDING.remove(player.getUUID());
		}
		return InteractionResult.SUCCESS;
	}

	/** The outcome: whether the guy was strung, what to tell the player, and its length in metres. */
	public record Result(boolean ok, String message, int length) {
	}

	/** Strings a guy from the pole cell {@code attach} down to the anchor at {@code anchorPos}, from a coil with {@code left} metres. */
	public static Result connect(ServerLevel level, BlockPos attach, BlockPos anchorPos, int left) {
		BlockState there = level.getBlockState(attach);
		if (!(there.getBlock() instanceof WoodPoleBlock pole)) {
			return new Result(false, "A guy is clamped to a true-size wood pole.", 0);
		}
		if (!(level.getBlockEntity(anchorPos) instanceof GuyAnchorBlockEntity anchor)) {
			return new Result(false, "A guy goes down to a guy anchor.", 0);
		}
		BlockPos foot = BigParts.main(attach, there);
		if (!(level.getBlockEntity(foot) instanceof BigPartBlockEntity entity)) {
			return new Result(false, "That pole has not stood up yet.", 0);
		}
		if (anchor.attach() != null) {
			return new Result(false, "That anchor already holds a guy: one guy to an anchor.", 0);
		}
		if (entity.guyAnchors().size() >= MAX_GUYS) {
			return new Result(false, "That pole already has " + MAX_GUYS + " guys.", 0);
		}
		double lead = Math.hypot(anchorPos.getX() - attach.getX(), anchorPos.getZ() - attach.getZ());
		int rise = attach.getY() - anchorPos.getY();
		if (rise < 2) {
			return new Result(false, "The anchor must be in the ground below the clamp: clamp the guy higher up the pole.", 0);
		}
		if (lead < 2) {
			return new Result(false, "The anchor is too close to the pole: set it at least 2 m out (a lead about the clamp's height is best).", 0);
		}
		int length = (int) Math.ceil(Math.sqrt(attach.distSqr(anchorPos)));
		if (length > left) {
			return new Result(false, "This coil has " + left + " m left; this guy needs " + length + ".", length);
		}
		anchor.guy(attach, foot);
		entity.addGuyAnchor(anchorPos);
		double holds = PoleLoad.guyLbf(foot, pole.height(), attach, anchorPos);
		return new Result(true, String.format("Guy of %d m, lead %.0f m, clamped %d m up: holds up to %,.0f lbf of line pull at the pole top.", length, lead,
				attach.getY() - foot.getY() + 1, holds), length);
	}

	/** The coil's length, metres. */
	public static int coilMeters() {
		return Tuning.GUY_STRAND_METERS;
	}
}
