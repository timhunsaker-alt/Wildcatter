package dev.wildcatter.control;

import dev.wildcatter.Wildcatter;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** The one packet between a PLC cabinet and its screen, either way: a compound of what changed. Each loader registers it. */
public final class PlcNet {
	public record Payload(CompoundTag tag) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(Wildcatter.id("plc"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = ByteBufCodecs.COMPOUND_TAG.map(Payload::new, Payload::tag).cast();

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** The client's side, which the client classes set, so a server never loads them. */
	public static Consumer<CompoundTag> client = tag -> {
	};

	private PlcNet() {
	}

	/** A screen asks. The player must stand near the cabinet, and the rest of the checks are the cabinet's. */
	public static void fromClient(ServerPlayer player, CompoundTag tag) {
		BlockPos pos = new BlockPos(tag.getIntOr("x", 0), tag.getIntOr("y", 0), tag.getIntOr("z", 0));
		if (player.blockPosition().distSqr(pos) <= 64 && player.level().getBlockEntity(pos) instanceof PlcRequests screen) {
			screen.request(player, tag);
		}
	}
}
