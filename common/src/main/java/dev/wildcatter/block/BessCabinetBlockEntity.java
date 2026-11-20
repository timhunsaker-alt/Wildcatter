package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.BessCells;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The commercial battery cabinet (docs/multiblocks/grid-batteries.md, 22b): five LFP modules (261 kWh), a 125 kW PCS on the world's
 * industrial LV tier, its BMS, a 3 kW chiller and aerosol suppression in one outdoor enclosure, with its own controller. Its AC terminal is
 * the west face of its main cell (the bottom front corner, facing north), where a 480 V cable comes in as to a panelboard's feeder.
 */
public class BessCabinetBlockEntity extends BessInverterBlockEntity implements BessModules {
	private final BessCells cells = new BessCells(0, Tuning.CABINET_HEAT_CAPACITY, Tuning.CABINET_CHILLER_WATTS);
	private int modules;

	public BessCabinetBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.BESS_CABINET_ENTITY, pos, state);
	}

	@Override
	public BessCells cells() {
		return cells;
	}

	@Override
	public int modules() {
		return modules;
	}

	@Override
	public int maxModules() {
		return Tuning.CABINET_MODULES;
	}

	@Override
	public void setModules(int n) {
		modules = n;
		cells.setRated(n * Tuning.LFP_MODULE_KWH);
		setChanged();
	}

	@Override
	public double ratedWatts() {
		return Tuning.CABINET_WATTS;
	}

	@Override
	protected double ratedVa() {
		return Tuning.CABINET_VA;
	}

	@Override
	protected String partName() {
		return "Battery cabinet";
	}

	@Override
	protected List<BessCells> cells(Level level) {
		return modules > 0 ? List.of(cells) : List.of();
	}

	@Override
	protected String unformed(Level level) {
		return modules == 0 ? "NO MODULES: load five LFP modules by right-clicking them in" : null;
	}

	@Override
	protected List<Lines.Face> acPorts(Level level) {
		return LeasePartBlock.faces(worldPosition, getBlockState(), at -> at[0] == 0 && at[1] == 0 && at[2] == 0, Direction.WEST);
	}


	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("modules", modules);
		output.putDouble("soc", cells.soc());
		output.putDouble("soh", cells.soh());
		output.putDouble("cycles", cells.cycles());
		output.putDouble("temp", cells.tempC());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		modules = input.getIntOr("modules", 0);
		cells.setRated(modules * Tuning.LFP_MODULE_KWH);
		cells.set(input.getDoubleOr("soc", 0), input.getDoubleOr("soh", 1), input.getDoubleOr("cycles", 0), input.getDoubleOr("temp", Tuning.CELL_AMBIENT_C));
	}
}
