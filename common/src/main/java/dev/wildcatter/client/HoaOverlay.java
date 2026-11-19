package dev.wildcatter.client;

import dev.wildcatter.control.HoaView;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.phys.Vec3;

/**
 * The control key's overlay (Tim's answer C18): while the key is in a hand, each HAND / OFF / AUTO selector the server reports nearby is
 * drawn as a label over its device, coloured as a keyswitch's legend: HAND amber, OFF red, AUTO green (LOCAL and REMOTE on switchgear
 * likewise). The server sends them twice a second ({@link HoaView}); a list older than a second and a half is dropped.
 */
public final class HoaOverlay {
	private static final int HAND = 0xFFFFB020;
	private static final int OFF = 0xFFFF4040;
	private static final int AUTO = 0xFF40E060;
	private static final long STALE_MS = 1500;

	private record Label(Vec3 at, String text) {
	}

	private static List<Label> labels = List.of();
	private static long received;

	private HoaOverlay() {
	}

	public static void fromServer(CompoundTag tag) {
		ListTag list = tag.getListOrEmpty("hoaView");
		List<Label> next = new ArrayList<>(list.size());
		for (int i = 0; i < list.size(); i++) {
			CompoundTag one = list.getCompoundOrEmpty(i);
			next.add(new Label(new Vec3(one.getDoubleOr("x", 0), one.getDoubleOr("y", 0), one.getDoubleOr("z", 0)), one.getStringOr("h", "")));
		}
		labels = next;
		received = System.currentTimeMillis();
	}

	static int colour(String position) {
		return switch (position) {
			case "AUTO", "REMOTE" -> AUTO;
			case "OFF" -> OFF;
			default -> HAND;
		};
	}

	/** What is drawn now, for the client test. */
	public static List<String> shown() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || !HoaView.holdsKey(client.player) || System.currentTimeMillis() - received > STALE_MS) {
			return List.of();
		}
		return labels.stream().map(Label::text).toList();
	}

	public static void draw(Minecraft client) {
		if (client.player == null || !HoaView.holdsKey(client.player) || System.currentTimeMillis() - received > STALE_MS) {
			return;
		}
		for (Label label : labels) {
			Gizmos.billboardText(label.text(), label.at(), TextGizmo.Style.forColorAndCentered(colour(label.text())).withScale(0.6F));
		}
	}
}
