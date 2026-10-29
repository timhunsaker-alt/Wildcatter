package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.EnergyBuffer;
import dev.wildcatter.energy.EnergyNet;
import dev.wildcatter.energy.EnergyNode;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.safety.Bondable;
import dev.wildcatter.safety.Grounding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Takes steam from any boiler touching it and turns it into RF, pushed out into the cables around it. */
public class SteamTurbineBlockEntity extends BlockEntity implements EnergyNode, Inspectable, Bondable {
	private final EnergyBuffer energy = new EnergyBuffer(Tuning.TURBINE_BUFFER, 0, Tuning.TURBINE_OUTPUT);
	private int lastMade;
	/** Ticks since it last made power; the rotor keeps spinning for a second so the block doesn't flicker. */
	private int sinceMade = 100;

	public SteamTurbineBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.STEAM_TURBINE_ENTITY, pos, state);
	}

	@Override
	public EnergyBuffer energy() {
		return energy;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, SteamTurbineBlockEntity turbine) {
		int made = 0;
		int steamWanted = (turbine.energy.capacity() - turbine.energy.stored()) / Tuning.TURBINE_RF_PER_STEAM;
		if (steamWanted > 0) {
			for (Direction side : Direction.values()) {
				if (level.getBlockEntity(pos.relative(side)) instanceof BoilerBlockEntity boiler) {
					int steam = boiler.takeSteam(Math.min(steamWanted, Tuning.BOILER_STEAM_PER_TICK * 2));
					made += turbine.energy.generate(steam * Tuning.TURBINE_RF_PER_STEAM);
					steamWanted -= steam;
				}
			}
		}
		turbine.lastMade = made;
		EnergyNet.push(level, pos, turbine.energy, Voltage.LOW);

		turbine.sinceMade = made > 0 ? 0 : Math.min(turbine.sinceMade + 1, 100);
		boolean running = turbine.sinceMade < 20;
		if (state.getValue(SteamTurbineBlock.RUNNING) != running) {
			level.setBlock(pos, state.setValue(SteamTurbineBlock.RUNNING, running), 3);
		}
		if (made > 0) {
			turbine.setChanged();
		}
	}

	public boolean isLive() {
		return energy.stored() > 0;
	}

	@Override
	public Component inspect() {
		return Component.literal("Steam turbine (110 V): " + energy.stored() + "/" + energy.capacity() + " RF, making " + lastMade + " RF/t; "
				+ (level != null && Grounding.isGrounded(level, worldPosition) ? "grounded" : "UNGROUNDED, touching it can shock you"));
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
