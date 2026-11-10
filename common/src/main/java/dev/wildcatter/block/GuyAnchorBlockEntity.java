package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.multiblock.BigParts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** A guy anchor's one guy: the pole cell it is clamped to and that pole's foot. Saved here; the pole keeps the anchor's place. */
public class GuyAnchorBlockEntity extends BlockEntity implements Inspectable {
	private @Nullable BlockPos attach;
	private @Nullable BlockPos pole;

	public GuyAnchorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.GUY_ANCHOR_ENTITY, pos, state);
	}

	/** The pole cell the guy is clamped to, or null with no guy. */
	public @Nullable BlockPos attach() {
		return attach;
	}

	/** The foot (main cell) of the guyed pole, or null with no guy. */
	public @Nullable BlockPos pole() {
		return pole;
	}

	public void guy(BlockPos attach, BlockPos pole) {
		this.attach = attach.immutable();
		this.pole = pole.immutable();
		changed();
	}

	private void changed() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	/** A guy whose pole has gone is let go. */
	public static void tick(Level level, BlockPos pos, BlockState state, GuyAnchorBlockEntity anchor) {
		if (anchor.attach == null || (level.getGameTime() + pos.hashCode()) % PoleLoad.CHECK_TICKS != 0 || !level.isLoaded(anchor.attach)) {
			return;
		}
		BlockState there = level.getBlockState(anchor.attach);
		if (!(there.getBlock() instanceof WoodPoleBlock) || !BigParts.main(anchor.attach, there).equals(anchor.pole)) {
			anchor.attach = null;
			anchor.pole = null;
			anchor.changed();
		}
	}

	@Override
	public Component inspect() {
		if (attach == null || level == null) {
			return Component.literal("Guy anchor: no guy (string one from a pole with a guy strand)");
		}
		BlockState there = level.getBlockState(attach);
		double strength = there.getBlock() instanceof WoodPoleBlock pole && this.pole != null ? PoleLoad.guyLbf(this.pole, pole.height(), attach, worldPosition) : 0;
		return Component.literal(String.format("Guy anchor: a 3/8 in EHS guy to the pole at %s, %.0f m up; it holds up to %,.0f lbf of line pull at the pole top",
				attach.toShortString(), (double) (attach.getY() - worldPosition.getY()), strength));
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (attach != null && pole != null) {
			output.putLong("attach", attach.asLong());
			output.putLong("pole", pole.asLong());
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		long a = input.getLongOr("attach", Long.MIN_VALUE);
		long p = input.getLongOr("pole", Long.MIN_VALUE);
		attach = a == Long.MIN_VALUE ? null : BlockPos.of(a);
		pole = p == Long.MIN_VALUE ? null : BlockPos.of(p);
	}
}
