package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The choke manifold (docs/multiblocks/oilfield-follow-ons.md, 23b): a 5,000 psi manifold of gate valves, a hydraulic adjustable
 * drilling choke worked from the remote panel, a manual adjustable choke, gauges and a buffer chamber, joined to the BOP stack's choke
 * line by steel pipe within 12 blocks of the substructure. The rig finds it by that pipe; the mud-gas separator stands against it.
 */
public class ChokeManifoldBlockEntity extends ProcessPartBlockEntity implements dev.wildcatter.kinetic.Sounding {
	private @Nullable BlockPos rigAt;
	private long rigSeen = -1000;
	/** When kill flow last went through its chokes (the rig tells it each tick of a kill): its running loop plays while it does. */
	private long flowSeen = -1000;
	private final dev.wildcatter.kinetic.RunningSound running = new dev.wildcatter.kinetic.RunningSound();

	public ChokeManifoldBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CHOKE_MANIFOLD_ENTITY, pos, state);
	}

	void rigHere(BlockPos rig, long now) {
		rigAt = rig;
		rigSeen = now;
	}

	/** The rig in a kill, each tick that mud or gas comes up the choke line and through an open choke. */
	void flowing(Level level) {
		flowSeen = level.getGameTime();
		running.update(this, true);
	}

	public boolean flowingNow() {
		return running.on();
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	@Override
	protected void work(Level level) {
		running.update(this, level.getGameTime() - flowSeen < 20);
		status = rigAt != null && level.getGameTime() - rigSeen < 60 ? "on the rig's choke line" : "NOT ON A RIG: pipe it to a BOP stack's choke line within 12 blocks of the substructure";
	}

	@Override
	public Component inspect() {
		String rig = "";
		if (level != null && rigAt != null && level.getGameTime() - rigSeen < 60 && level.getBlockEntity(rigAt) instanceof RigSubstructureBlockEntity r && r.well() != null) {
			rig = String.format("; choke %.0f %% open, casing %,.0f psi, %.1f m3 of gas through it", r.well().choke() * 100, r.well().casingPsi(), r.gasVented());
		}
		return Component.literal("Choke manifold (5,000 psi, hydraulic and manual adjustable chokes): " + status + rig);
	}

	@Override
	public net.minecraft.sounds.SoundEvent loop() {
		return running.on() ? dev.wildcatter.WildcatterSounds.CHOKE_FLOW : null;
	}

	@Override
	public float soundPitch() {
		return 1.0F;
	}

	@Override
	public float soundVolume() {
		return running.on() ? 0.8F : 0;
	}

	@Override
	protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
		super.loadAdditional(input);
		running.load(input);
	}

	@Override
	public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
		return running.tag();
	}
}
