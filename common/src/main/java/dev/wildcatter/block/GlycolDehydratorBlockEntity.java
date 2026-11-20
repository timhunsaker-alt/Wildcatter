package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The glycol dehydrator (docs/multiblocks/oilfield-follow-ons.md, 23c): a TEG contactor and its gas-fired reboiler (0.1 MW of gas on the
 * east face of its bottom cells; 15 kW at industrial LV for its pumps and cooler) drying gas withdrawn from a storage cavern to pipeline
 * spec, up to 5 Sm3/s. The wet gas comes from a storage wellhead piped to the north face of its front bottom cells (on WITHDRAW); the dry
 * pipeline gas leaves the south face of its back bottom cells, for a receipt meter station, a gas plant or a set.
 */
public class GlycolDehydratorBlockEntity extends UnitMotorBlockEntity {
	private double dried;
	private String unit = "starting";

	public GlycolDehydratorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.GLYCOL_DEHYDRATOR_ENTITY, pos, state);
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	private List<Lines.Face> burner() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return side == Direction.SOUTH && at[2] == 2 && at[1] == 0 ? Product.PIPELINE_GAS : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.PIPELINE_GAS ? Tuning.STORAGE_RATE_SM3S * Tuning.PLANT_HOLD_SECONDS : 0;
	}

	public double dried() {
		return dried;
	}

	@Override
	protected void work(Level level) {
		StorageWellheadBlockEntity head = StorageCompressorBlockEntity.wellheadOn(level, inlet());
		if (head == null) {
			unit = "NO WELLHEAD: pipe a gas storage wellhead to the north side of its front";
			return;
		}
		if (head.mode() != StorageWellheadBlockEntity.Mode.WITHDRAW) {
			head.withdraw(level, 0);   // it is there: the wellhead does not read WET GAS
			unit = "idle: the wellhead is not on WITHDRAW";
			return;
		}
		double room = room(Product.PIPELINE_GAS);
		if (room < 1e-3) {
			unit = "FULL: its dry gas has nowhere to go (pipe the south side of its back)";
			head.withdraw(level, 0);
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			head.withdraw(level, 0);
			unit = "starting: " + describe();
			return;
		}
		double need = Tuning.DEHYDRATOR_REBOILER_W * seconds();
		double fired = GasFiring.fire(level, burner(), need, this);
		if (fired < need * 0.5) {
			head.withdraw(level, 0);
			unit = "NO FUEL: pipe fuel gas to the east side of its bottom for the reboiler";
			return;
		}
		double took = head.withdraw(level, Math.min(room, Tuning.STORAGE_RATE_SM3S * seconds() * strength));
		add(Product.PIPELINE_GAS, took);
		dried += took;
		unit = String.format("drying %.1f Sm3/s of withdrawn gas to pipeline spec", took / seconds());
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Glycol dehydrator (TEG contactor and reboiler, %.1f MW of gas): %s; %,.0f Sm3 dried; ", Tuning.DEHYDRATOR_REBOILER_W / 1e6, unit, dried) + describe());
	}
}
