package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.blueprint.Blueprint;
import dev.wildcatter.blueprint.BlueprintLibrary;
import dev.wildcatter.blueprint.Surveys;
import dev.wildcatter.energy.Inspectable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

/**
 * A blueprint's site (docs/blueprints.md, "Site marker"): the blueprint's origin is here, turned the way it was placed. Every few
 * seconds it counts what of it stands and names the next lift; the client draws its ghost until it is built.
 */
public class SiteMarkerBlockEntity extends BlockEntity implements Inspectable {
	/** The markers the client has loaded, for the ghost. */
	public static final Set<SiteMarkerBlockEntity> CLIENT = ConcurrentHashMap.newKeySet();
	/** The markers the server has loaded, for the cranes to find. */
	public static final Set<SiteMarkerBlockEntity> SERVER = ConcurrentHashMap.newKeySet();

	private String data = "";
	private int turns;
	private @Nullable Blueprint turned;
	private @Nullable BlockState[] states;
	private @Nullable List<Blueprint.Lift> lifts;
	private int done;
	private boolean complete;
	private boolean dismantle;
	private String next = "";
	/** Lifts a crane has taken on, by index, and the crane: two cranes never work the same lift (10c3). Not saved. */
	private final java.util.Map<Integer, BlockPos> claims = new java.util.HashMap<>();

	public SiteMarkerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.SITE_MARKER_ENTITY, pos, state);
	}

	public void set(String data, int turns) {
		this.data = data;
		this.turns = Math.floorMod(turns, 4);
		turned = null;
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	public String data() {
		return data;
	}

	public int turns() {
		return turns;
	}

	/** The blueprint as it stands here, turned; null if it cannot be read. */
	public @Nullable Blueprint blueprint() {
		if (turned == null) {
			Blueprint b = BlueprintLibrary.of(data);
			if (b == null) {
				return null;
			}
			turned = b.turned(turns);
			BlockState[] raw = Surveys.states(turned);
			states = new BlockState[raw.length];
			for (int i = 0; i < raw.length; i++) {
				states[i] = raw[i] == null ? null : Surveys.turned(raw[i], turns);
			}
			lifts = Surveys.lifts(turned);
		}
		return turned;
	}

	/** Each palette entry turned as the site is; for the ghost and the cranes. */
	public @Nullable BlockState[] states() {
		blueprint();
		return states;
	}

	public @Nullable List<Blueprint.Lift> lifts() {
		blueprint();
		return lifts;
	}

	public boolean complete() {
		return complete;
	}

	public int done() {
		return done;
	}

	/** True while the site is to be taken down, last lift first (docs/blueprints.md, "Site marker"). */
	public boolean dismantle() {
		return dismantle;
	}

	public void toggleMode() {
		dismantle = !dismantle;
		claims.clear();
		recount();
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	/** Takes lift {@code index} for the crane at {@code crane}; false if another crane has it. */
	public boolean claim(int index, BlockPos crane) {
		BlockPos holder = claims.get(index);
		if (holder != null && !holder.equals(crane)) {
			return false;
		}
		claims.put(index, crane.immutable());
		return true;
	}

	public void release(int index, BlockPos crane) {
		claims.remove(index, crane);
	}

	/** True if another crane than {@code crane} has lift {@code index}. */
	public boolean claimedByOther(int index, BlockPos crane) {
		BlockPos holder = claims.get(index);
		return holder != null && !holder.equals(crane);
	}

	/** True if any block or the part of lift {@code lift} stands: something of it to take down. */
	public boolean liftAnyStands(Blueprint.Lift lift) {
		Blueprint b = blueprint();
		if (b == null || level == null) {
			return false;
		}
		if (lift.isPart()) {
			return Surveys.partStands(level, worldPosition, b.parts().get(lift.part()));
		}
		for (int i : lift.blocks()) {
			Blueprint.Block k = b.blocks().get(i);
			if (Surveys.blockStands(level, worldPosition, k, states[k.state()])) {
				return true;
			}
		}
		return false;
	}

	/** True if every block and part of lift {@code lift} stands. */
	public boolean liftStands(Blueprint.Lift lift) {
		Blueprint b = blueprint();
		if (b == null || level == null) {
			return false;
		}
		if (lift.isPart()) {
			return Surveys.partStands(level, worldPosition, b.parts().get(lift.part()));
		}
		for (int i : lift.blocks()) {
			Blueprint.Block k = b.blocks().get(i);
			if (!Surveys.blockStands(level, worldPosition, k, states[k.state()])) {
				return false;
			}
		}
		return true;
	}

	/** Recounts the site now. */
	public void recount() {
		Blueprint b = blueprint();
		if (b == null || level == null) {
			next = "UNREADABLE BLUEPRINT";
			return;
		}
		int count = 0;
		String first = "";
		if (dismantle) {
			for (int i = lifts.size() - 1; i >= 0; i--) {
				if (liftAnyStands(lifts.get(i))) {
					count++;
					if (first.isEmpty()) {
						first = describe(lifts.get(i));
					}
				}
			}
			done = count;
			complete = false;
			next = first;
			setChanged();
			return;
		}
		for (Blueprint.Lift lift : lifts) {
			if (liftStands(lift)) {
				count++;
			} else if (first.isEmpty()) {
				first = describe(lift);
				BlockPos blocked = inTheWay(lift);
				if (blocked != null) {
					first += String.format("; IN THE WAY: %s at %d, %d, %d", net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(blocked).getBlock()),
							blocked.getX(), blocked.getY(), blocked.getZ());
				}
			}
		}
		done = count;
		boolean was = complete;
		complete = count == lifts.size();
		next = first;
		if (was != complete) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
		setChanged();
	}

	/** A cell of the lift where something else stands, or null: a crane or a hand cannot set it until it is cleared. */
	public @Nullable BlockPos inTheWay(Blueprint.Lift lift) {
		Blueprint b = blueprint();
		if (b == null || level == null) {
			return null;
		}
		if (lift.isPart()) {
			Blueprint.Part p = b.parts().get(lift.part());
			for (BlockPos cell : BlockPos.betweenClosed(worldPosition.offset(p.x0(), p.y0(), p.z0()), worldPosition.offset(p.x1(), p.y1(), p.z1()))) {
				if (!level.getBlockState(cell).canBeReplaced()) {
					return cell.immutable();
				}
			}
			return null;
		}
		for (int i : lift.blocks()) {
			Blueprint.Block k = b.blocks().get(i);
			BlockPos at = worldPosition.offset(k.x(), k.y(), k.z());
			BlockState there = level.getBlockState(at);
			if (!there.canBeReplaced() && !Surveys.standsAs(there, states[k.state()])) {
				return at;
			}
		}
		return null;
	}

	/** "Pot (60 t) at 66, 64, -6313", or "a pallet of 12 blocks (14.4 t) at ...". */
	public String describe(Blueprint.Lift lift) {
		Blueprint b = blueprint();
		if (lift.isPart()) {
			Blueprint.Part p = b.parts().get(lift.part());
			BlockPos at = worldPosition.offset(p.x(), p.y(), p.z());
			var block = Surveys.part(p);
			String what = block == null ? p.id() : block.getName().getString();
			return String.format("%s (%.0f t) at %d, %d, %d", what, lift.tonnes(), at.getX(), at.getY(), at.getZ());
		}
		Blueprint.Block k = b.blocks().get(lift.blocks().get(0));
		BlockPos at = worldPosition.offset(k.x(), k.y(), k.z());
		return String.format("a pallet of %d blocks (%.1f t) at %d, %d, %d", lift.blocks().size(), lift.tonnes(), at.getX(), at.getY(), at.getZ());
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, SiteMarkerBlockEntity site) {
		SERVER.add(site);
		if ((level.getGameTime() + pos.hashCode()) % Tuning.SITE_CHECK_TICKS == 0) {
			site.recount();
		}
	}

	public static void clientTick(Level level, BlockPos pos, BlockState state, SiteMarkerBlockEntity site) {
		CLIENT.add(site);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		CLIENT.remove(this);
		SERVER.remove(this);
	}

	@Override
	public Component inspect() {
		Blueprint b = blueprint();
		if (b == null) {
			return Component.literal("Site marker: UNREADABLE BLUEPRINT (a library plant missing from this version, or a damaged survey)");
		}
		if (level != null && !level.isClientSide()) {
			recount();
		}
		String name = data.startsWith("lib:") ? BlueprintLibrary.title(data.substring(4)) : b.name();
		if (dismantle) {
			return Component.literal(done == 0 ? String.format("Site: %s, DISMANTLE: cleared", name)
					: String.format("Site: %s, DISMANTLE, %d lifts, %d still stand; next: %s", name, lifts.size(), done, next));
		}
		return Component.literal(complete ? String.format("Site: %s, all %d lifts stand: built", name, lifts.size())
				: String.format("Site: %s, %d lifts, %d done; next: %s", name, lifts.size(), done, next));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putString("blueprint", data);
		output.putInt("turns", turns);
		output.putBoolean("complete", complete);
		output.putBoolean("dismantle", dismantle);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		String was = data;
		int wasTurns = turns;
		data = input.getStringOr("blueprint", "");
		turns = input.getIntOr("turns", 0);
		complete = input.getBooleanOr("complete", false);
		dismantle = input.getBooleanOr("dismantle", false);
		if (!was.equals(data) || wasTurns != turns) {
			turned = null;
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}
}
