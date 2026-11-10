package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The uranium calciner (docs/multiblocks/adu-yellowcake.md, 16a): a six-hearth furnace with a rabble arm that drives the ammonia and
 * water out of the precipitation tank's ammonium diuranate at 750-800 C and hands U3O8 at 96 % to the drum filler. It stands where
 * the yellowcake dryer stands, touching the tank and the filler. Its burners fire pipeline gas or LPG on the east face of its bottom
 * cells (0.6 MW); its rabble drive and off-gas fan are a motor on the industrial supply, its box on the north face of its main cell. From cold it takes five
 * minutes to come to temperature. Its off-gas (ammonia and water) goes to a scrubber, counted.
 */
public class UraniumCalcinerBlockEntity extends UnitMotorBlockEntity {
	private double warm;      // 0 cold, 1 at 750 C
	private double offGas;    // kg of ammonia and water driven off, to its scrubber
	private double made;      // kg of U3O8 calcined
	private String unit = "starting";

	public UraniumCalcinerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.URANIUM_CALCINER_ENTITY, pos, state);
	}

	private List<Lines.Face> gas() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	/** Its box is on the north face of its main cell: the west stands against the precipitation tank. */
	@Override
	protected List<Direction> boxFaces() {
		return List.of(LeasePartBlock.world(getBlockState(), Direction.NORTH));
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	public double warm() {
		return warm;
	}

	/** For the game test: the hearths already at temperature. */
	public void setWarmForTest() {
		warm = 1;
		setChanged();
	}

	public double made() {
		return made;
	}

	@Override
	protected void work(Level level) {
		PrecipitationTankBlockEntity tank = Touching.find(level, worldPosition, PrecipitationTankBlockEntity.class, t -> true);
		DrumFillerBlockEntity filler = Touching.find(level, worldPosition, DrumFillerBlockEntity.class, f -> true);
		if (tank == null || filler == null) {
			unit = "NOT BUILT: set it against a PRECIPITATION TANK and a DRUM FILLER";
			cool();
			return;
		}
		if (tank.adu() < 1e-6 && warm <= 0) {
			unit = "idle: no ADU in the tank (pipe liquid ammonia to the tank's east side)";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting the rabble drive: " + describe();
			cool();
			return;
		}
		if (warm < 1) {
			// firing up: the burners at full rate until the hearths reach 750 C
			double need = Tuning.CALCINER_FIRING_WATTS * seconds();
			double got = GasFiring.fire(level, gas(), need, this);
			if (got <= 0) {
				unit = "NO GAS: pipe pipeline gas or LPG to its east side";
				cool();
				return;
			}
			warm = Math.min(1, warm + seconds() / Tuning.CALCINER_WARMUP_SECONDS * got / need);
			unit = String.format("firing up: hearths at %.0f C of 750", 25 + 725 * warm);
			return;
		}
		double max = Math.min(Tuning.CALCINER_U3O8_KGS * seconds() * strength, filler.roomU3o8() * Tuning.ADU_U3O8);
		double u = max > 1e-6 ? tank.takeAdu(max) : 0;
		if (u <= 0) {
			unit = max <= 1e-6 ? "idle at temperature: the drum filler is full" : "idle at temperature: no ADU in the tank";
			GasFiring.fire(level, gas(), Tuning.CALCINER_FIRING_WATTS * seconds() * 0.2, this);   // holding its hearths hot
			return;
		}
		double need = Tuning.CALCINER_FIRING_WATTS * seconds() * u / (Tuning.CALCINER_U3O8_KGS * seconds());
		double got = GasFiring.fire(level, gas(), need, this);
		double done = u * Math.min(1, need > 0 ? got / need : 0);
		tank.takeAdu(-(u - done));   // what it could not calcine goes back
		if (done <= 0) {
			unit = "NO GAS: pipe pipeline gas or LPG to its east side";
			cool();
			return;
		}
		filler.addU3o8(done / Tuning.ADU_U3O8);
		made += done;
		offGas += done * (Tuning.ADU_NH3_PER_U3O8 * 0.36 + 0.3);   // the ADU's own ammonia and its water of hydration (proposed)
		unit = String.format("calcining %.3f kg/s of U3O8 at 750 C on %.0f kW of gas", done / seconds(), got / seconds() / 1000);
	}

	private void cool() {
		warm = Math.max(0, warm - seconds() / (Tuning.CALCINER_WARMUP_SECONDS * 3));
	}

	@Override
	public Component inspect() {
		return Component.literal("Uranium calciner (six hearths, " + Math.round(Tuning.CALCINER_FIRING_WATTS / 1000) + " kW of burners; drive " + Math.round(Tuning.CALCINER_WATTS / 1000)
				+ " kW, " + Voltage.INDUSTRIAL.label() + "): " + unit + String.format("; %,.0f kg of U3O8 calcined; %,.0f kg of off-gas to its scrubber; ", made, offGas) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("warm", warm);
		output.putDouble("offGas", offGas);
		output.putDouble("made", made);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		warm = input.getDoubleOr("warm", 0);
		offGas = input.getDoubleOr("offGas", 0);
		made = input.getDoubleOr("made", 0);
	}
}
