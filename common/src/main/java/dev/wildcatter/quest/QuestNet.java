package dev.wildcatter.quest;

import dev.wildcatter.Wildcatter;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * The quest book's one packet, either way, a compound tag (as {@code PlcNet}): the server sends {@code chapters} (each chapter file's
 * text) on join and after a reload, and {@code progress} when it changes; the client sends {@code claim}. Each loader registers it.
 */
public final class QuestNet {
	public record Payload(CompoundTag tag) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(Wildcatter.id("quest"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = ByteBufCodecs.COMPOUND_TAG.map(Payload::new, Payload::tag).cast();

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** The client's side: {@link QuestClient#fromServer} unless a client never set it (a dedicated server never calls it). */
	public static Consumer<CompoundTag> client = QuestClient::fromServer;

	private QuestNet() {
	}

	/** A screen asks to claim a quest's rewards; the tracker checks it is done and not yet claimed. */
	public static void fromClient(ServerPlayer player, CompoundTag tag) {
		String claim = tag.getStringOr("claim", "");
		Identifier id = Identifier.tryParse(claim);
		if (id != null && !claim.isEmpty()) {
			QuestTracker.claim(player, id);
		}
	}

	/** Asks the server to claim a quest's rewards (from the screen). */
	public static void claim(Identifier quest) {
		CompoundTag tag = new CompoundTag();
		tag.putString("claim", quest.toString());
		dev.wildcatter.platform.Platform.INSTANCE.sendToServer(new Payload(tag));
	}
}
