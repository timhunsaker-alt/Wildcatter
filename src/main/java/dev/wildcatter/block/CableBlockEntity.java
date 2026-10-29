package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.EnergyBuffer;
import dev.wildcatter.energy.EnergyNet;
import dev.wildcatter.energy.EnergyNode;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.safety.Shock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** One piece of cable. Which voltage it carries (and how much) comes from the block it belongs to. */
public class CableBlockEntity extends BlockEntity implements EnergyNode, Inspectable {
	private final Voltage voltage;
	private final EnergyBuffer energy;
	private int lastSent;

	public CableBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CABLE_ENTITY, pos, state);
		this.voltage = ((CableBlock) state.getBlock()).voltage();
		this.energy = switch (voltage) {
			case LOW -> new EnergyBuffer(Tuning.CABLE_BUFFER, Tuning.CABLE_THROUGHPUT, Tuning.CABLE_THROUGHPUT);
			case MEDIUM -> new EnergyBuffer(Tuning.MEDIUM_CABLE_BUFFER, Tuning.MEDIUM_CABLE_THROUGHPUT, Tuning.MEDIUM_CABLE_THROUGHPUT);
			case HIGH -> new EnergyBuffer(Tuning.HIGH_CABLE_BUFFER, Tuning.HIGH_CABLE_THROUGHPUT, Tuning.HIGH_CABLE_THROUGHPUT);
		};
	}

	private boolean insulated() {
		return ((CableBlock) getBlockState().getBlock()).insulated();
	}

	@Override
	public EnergyBuffer energy() {
		return energy;
	}

	@Override
	public Voltage voltageAt(Direction side) {
		return voltage;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, CableBlockEntity cable) {
		cable.lastSent = cable.energy.stored() > 0 ? EnergyNet.push(level, pos, cable.energy, cable.voltage) : 0;
		if (cable.lastSent > 0) {
			cable.setChanged();
		}
		// A bare wire with power in it shocks whoever is close enough: touching it at 110 V, metres away at 138 kV.
		if (!cable.insulated() && cable.energy.stored() > 0 && level instanceof ServerLevel server
				&& level.getGameTime() % Tuning.SHOCK_INTERVAL == 0) {
			// Measured from the wire itself (its thin core and arms), not from the whole block.
			double reach = Shock.reach(cable.voltage);
			List<AABB> wire = WireShape.of(state).toAabbs().stream().map(box -> box.move(pos).inflate(reach)).toList();
			AABB search = new AABB(pos).inflate(reach);
			for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, search, e -> e.isAlive() && !e.isSpectator())) {
				if (wire.stream().anyMatch(zone -> zone.intersects(victim.getBoundingBox()))) {
					Shock.zap(server, victim, cable.voltage, pos);
				}
			}
		}
	}

	@Override
	public Component inspect() {
		return Component.literal((insulated() ? "Insulated cable" : "Bare wire, shocks when live") + " (" + voltage.label() + "): "
				+ energy.stored() + "/" + energy.capacity() + " RF, passing on " + lastSent + " RF/t");
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("energy", energy.stored());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		energy.set(input.getIntOr("energy", 0));
	}
}
