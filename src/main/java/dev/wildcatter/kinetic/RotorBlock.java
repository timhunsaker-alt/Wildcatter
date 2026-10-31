package dev.wildcatter.kinetic;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Never placed and has no item: its block states only carry the models of the parts that turn (a fan, a pulley, a coupling),
 * which {@code client/KineticRenderer} draws turning inside the machines. Each model is drawn centred on the block, with its axis along X.
 */
public class RotorBlock extends Block {
	public enum Part implements StringRepresentable {
		RADIATOR_FAN("radiator_fan"), CRANK_PULLEY("crank_pulley"), COUPLING("coupling"), TOWER_FAN("tower_fan"), DISCONNECT_BLADE("disconnect_blade"), ALTERNATOR_FAN("alternator_fan"), TURBINE_COUPLING("turbine_coupling"), PUMP_COUPLING("pump_coupling"),
		DISCONNECT_ARM_HV("disconnect_arm_hv"), DISCONNECT_ARM_HV_BACK("disconnect_arm_hv_back"), DISCONNECT_BLADE_DIST("disconnect_blade_dist"),
		PUMPJACK_CRANK("pumpjack_crank"), PUMPJACK_PITMAN_LOW("pumpjack_pitman_low"), PUMPJACK_PITMAN_HIGH("pumpjack_pitman_high"),
		PUMPJACK_BEAM_FRONT("pumpjack_beam_front"), PUMPJACK_BEAM_MID("pumpjack_beam_mid"), PUMPJACK_BEAM_BACK("pumpjack_beam_back"),
		PUMPJACK_ROD("pumpjack_rod"), PUMPJACK_ROD_LOW("pumpjack_rod_low"), PUMPJACK_BRIDLE("pumpjack_bridle"),
		RIG_CARRIAGE("rig_carriage"), RIG_SPINDLE("rig_spindle"), RIG_PIPE("rig_pipe"),
		STARTER_COUPLING("starter_coupling"), FLYBALL_GOVERNOR("flyball_governor"), STARTER_PUMP_COUPLING("starter_pump_coupling");

		private final String name;

		Part(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

	public RotorBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(PART, Part.RADIATOR_FAN));
	}

	public BlockState part(Part part) {
		return defaultBlockState().setValue(PART, part);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PART);
	}
}
