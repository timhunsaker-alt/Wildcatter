package dev.wildcatter.item;

import dev.wildcatter.Tuning;
import dev.wildcatter.block.InsulatorBlockEntity;
import dev.wildcatter.energy.Voltage;
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

/**
 * A conductor spool. Right-click an insulator, then a second one, to string a span of this tier between them. The spool pays out
 * one unit of its length for every block of span. A covered spool makes a plastic-covered span that is safe to touch.
 */
public class SpoolItem extends Item {
	private record Pending(ResourceKey<Level> dimension, BlockPos pos) {
	}

	/** The first insulator each player has clicked. Not saved: it is only ever a moment between two clicks. */
	private static final Map<UUID, Pending> PENDING = new HashMap<>();

	private final Voltage tier;
	private final boolean covered;

	public SpoolItem(Properties properties, Voltage tier, boolean covered) {
		super(properties);
		this.tier = tier;
		this.covered = covered;
	}

	public Voltage tier() {
		return tier;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!(context.getLevel() instanceof ServerLevel level) || !(context.getPlayer() instanceof ServerPlayer player)
				|| !(level.getBlockEntity(context.getClickedPos()) instanceof InsulatorBlockEntity here)) {
			return InteractionResult.PASS;
		}
		BlockPos pos = context.getClickedPos();
		Pending first = PENDING.get(player.getUUID());
		if (first == null || first.dimension != level.dimension() || !(level.getBlockEntity(first.pos) instanceof InsulatorBlockEntity there)) {
			PENDING.put(player.getUUID(), new Pending(level.dimension(), pos.immutable()));
			player.sendOverlayMessage(Component.literal("First insulator chosen (" + here.conductor().label() + "). Right-click the second one."));
			return InteractionResult.SUCCESS;
		}
		if (first.pos.equals(pos)) {
			PENDING.remove(player.getUUID());
			player.sendOverlayMessage(Component.literal("Cancelled."));
			return InteractionResult.SUCCESS;
		}
		int left = context.getItemInHand().getMaxDamage() - context.getItemInHand().getDamageValue();
		Result result = connect(level, first.pos, pos, tier, covered, left);
		player.sendOverlayMessage(Component.literal(result.message()));
		if (result.ok()) {
			context.getItemInHand().hurtAndBreak(result.length(), player, context.getHand());
			PENDING.remove(player.getUUID());
		}
		return InteractionResult.SUCCESS;
	}

	/** The outcome of trying to string a span: whether it was strung, what to tell the player, and how many blocks it was. */
	public record Result(boolean ok, String message, int length) {
	}

	/** Strings a span between two insulators if the rules allow it. The spool holds {@code spoolLeft} blocks of span. */
	public static Result connect(ServerLevel level, BlockPos a, BlockPos b, Voltage tier, boolean covered, int spoolLeft) {
		if (!(level.getBlockEntity(a) instanceof InsulatorBlockEntity there) || !(level.getBlockEntity(b) instanceof InsulatorBlockEntity here)) {
			return new Result(false, "Both ends must be insulators.", 0);
		}
		String problem = problem(here, there, b, a, tier, covered);
		if (problem != null) {
			return new Result(false, problem, 0);
		}
		int length = length(b, a);
		if (spoolLeft < length) {
			return new Result(false, "This spool has " + spoolLeft + " blocks of span left; this one needs " + length + ".", length);
		}
		here.addSpan(new InsulatorBlockEntity.Span(a.immutable(), tier.ordinal(), covered, length));
		there.addSpan(new InsulatorBlockEntity.Span(b.immutable(), tier.ordinal(), covered, length));
		return new Result(true, "Span of " + length + " blocks (" + Math.round(length * Tuning.SPAN_METERS_PER_BLOCK) + " m), " + tier.label() + (covered ? ", covered" : ", bare") + ".", length);
	}

	private static int length(BlockPos a, BlockPos b) {
		return Math.max(1, (int) Math.ceil(Math.sqrt(a.distSqr(b))));
	}

	/** Why this span cannot be strung, or null if it can. */
	private static String problem(InsulatorBlockEntity here, InsulatorBlockEntity there, BlockPos pos, BlockPos other, Voltage tier, boolean covered) {
		if (here.conductor() != there.conductor()) {
			return "These insulators carry different conductors (" + there.conductor().label() + " and " + here.conductor().label() + "). Set both the same first.";
		}
		int length = length(pos, other);
		if (length > Tuning.maxSpan(tier)) {
			return "Too far: " + length + " blocks (" + Math.round(length * Tuning.SPAN_METERS_PER_BLOCK) + " m). A " + tier.label() + " span reaches at most "
					+ Tuning.maxSpan(tier) + " blocks (" + Math.round(Tuning.maxSpan(tier) * Tuning.SPAN_METERS_PER_BLOCK) + " m): put another pole between.";
		}
		for (InsulatorBlockEntity end : new InsulatorBlockEntity[] {here, there}) {
			if (!end.spans().isEmpty() && (end.lineTier() != tier || end.spans().get(0).covered() != covered)) {
				return "That insulator already holds a " + end.lineTier().label() + (end.spans().get(0).covered() ? " covered" : " bare") + " line: one kind of line per insulator.";
			}
			if (end.spanCount() >= 4) {
				return "An insulator holds at most four spans.";
			}
		}
		if (here.hasSpanTo(other)) {
			return "These two are already joined.";
		}
		return null;
	}
}
