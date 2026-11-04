package dev.wildcatter.client;

import dev.wildcatter.Tuning;
import dev.wildcatter.blueprint.Blueprint;
import dev.wildcatter.blueprint.BlueprintLibrary;
import dev.wildcatter.blueprint.Surveys;
import dev.wildcatter.block.SiteMarkerBlockEntity;
import dev.wildcatter.item.BlueprintItem;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A blueprint's ghost (docs/blueprints.md): held, it is drawn where it would stand, its origin on the block you look at and turned the
 * way you face; a site marker draws its own until it is built. Parts are outline boxes, green where they fit and red where something is
 * in the way; ordinary blocks not yet standing are translucent cubes in their map colour, near you only.
 */
public final class BlueprintGhost {
	private static final int FITS = 0xFF44DD55;
	private static final int BLOCKED = 0xFFEE4433;
	private static final int MARKER = 0xFF55AAEE;
	/** A site's ghost is drawn from this far from its box, m. */
	private static final int SITE_RANGE = 128;
	/** Turned blueprints and their states, by data and turns: turning a big one each frame would be slow. */
	private static final Map<String, Object[]> CACHE = new HashMap<>();

	private BlueprintGhost() {
	}

	public static void draw(Minecraft client) {
		if (client.player == null || client.level == null) {
			return;
		}
		for (SiteMarkerBlockEntity site : SiteMarkerBlockEntity.CLIENT) {
			if (site.isRemoved() || site.getLevel() != client.level) {
				SiteMarkerBlockEntity.CLIENT.remove(site);
				continue;
			}
			if (!site.complete()) {
				draw(client, site.data(), site.turns(), site.getBlockPos(), MARKER);
			}
		}
		if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
			return;
		}
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = client.player.getItemInHand(hand);
			if (stack.getItem() instanceof BlueprintItem && !client.player.isShiftKeyDown()) {
				String data = BlueprintItem.data(stack);
				if (!data.isEmpty()) {
					draw(client, data, BlueprintItem.turns(client.player), hit.getBlockPos().relative(hit.getDirection()), FITS);
				}
				return;
			}
		}
	}

	private static void draw(Minecraft client, String data, int turns, BlockPos origin, int fits) {
		Object[] cached = CACHE.computeIfAbsent(data.hashCode() + ":" + data.length() + ":" + turns, k -> {
			Blueprint b = BlueprintLibrary.of(data);
			if (b == null) {
				return null;
			}
			Blueprint t = b.turned(turns);
			BlockState[] raw = Surveys.states(t);
			BlockState[] states = new BlockState[raw.length];
			for (int i = 0; i < raw.length; i++) {
				states[i] = raw[i] == null ? null : Surveys.turned(raw[i], turns);
			}
			return new Object[] {t, states};
		});
		if (cached == null) {
			return;
		}
		if (CACHE.size() > 16) {
			CACHE.clear();
		}
		Blueprint b = (Blueprint) cached[0];
		BlockState[] states = (BlockState[]) cached[1];
		Level level = client.level;
		AABB site = new AABB(origin).minmax(new AABB(origin.offset(b.sizeX() - 1, b.sizeY() - 1, b.sizeZ() - 1)));
		if (site.distanceToSqr(client.player.position()) > SITE_RANGE * SITE_RANGE) {
			return;   // measured to the site's nearest edge, not its marker: a big plant is seen from beside its far end
		}
		Gizmos.cuboid(site.inflate(0.02), GizmoStyle.stroke(0x88FFFFFF, 1.0F));
		for (Blueprint.Part p : b.parts()) {
			if (Surveys.partStands(level, origin, p)) {
				continue;
			}
			boolean clear = true;
			for (BlockPos cell : BlockPos.betweenClosed(origin.offset(p.x0(), p.y0(), p.z0()), origin.offset(p.x1(), p.y1(), p.z1()))) {
				if (!level.getBlockState(cell).canBeReplaced()) {
					clear = false;
					break;
				}
			}
			AABB box = new AABB(origin.offset(p.x0(), p.y0(), p.z0())).minmax(new AABB(origin.offset(p.x1(), p.y1(), p.z1())));
			Gizmos.cuboid(box.inflate(0.01), GizmoStyle.stroke(clear ? fits : BLOCKED, 2.0F));
		}
		BlockPos me = client.player.blockPosition();
		double range = Tuning.GHOST_RANGE * Tuning.GHOST_RANGE;
		int drawn = 0;
		for (Blueprint.Block k : b.blocks()) {
			BlockPos at = origin.offset(k.x(), k.y(), k.z());
			if (at.distSqr(me) > range) {
				continue;
			}
			BlockState want = states[k.state()];
			if (want == null || Surveys.standsAs(level.getBlockState(at), want)) {
				continue;
			}
			int colour = want.getMapColor(level, at).col;
			Gizmos.cuboid(new AABB(at).deflate(0.15), GizmoStyle.fill(0x55000000 | colour));
			if (++drawn >= Tuning.GHOST_BLOCKS) {
				break;
			}
		}
	}
}
