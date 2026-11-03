package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The coke pit and its hydraulic decoking (docs/multiblocks/delayed-coker.md): a 400 kW high-pressure jet pump at 4.16 kV, about
 * 200 bar, cuts the coke out of the drum standing on it that is not filling, up to 10 t an hour, on cutting water off a pump (0.3 L
 * a kg made up). Green petroleum coke goes out in 1,000 kg pieces to a hopper under it, or to an empty hand.
 */
public class CokePitBlockEntity extends FillerBlockEntity implements WaterUser {
	private double coke;    // kg cut, toward a piece
	private double water;   // litres
	private int pieces;
	private String unit = "starting";

	public CokePitBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.COKE_PIT_ENTITY, pos, state);
	}

	@Override
	public boolean takes(ItemStack stack) {
		return false;
	}

	@Override
	public double waterLevel() {
		return water / Tuning.CARBON_WATER_HOLD;
	}

	@Override
	public int addWater(int litres) {
		int fits = (int) Math.max(0, Math.min(litres, Tuning.CARBON_WATER_HOLD - water));
		water += fits;
		setChanged();
		return fits;
	}

	@Override
	protected void work(Level level) {
		while (coke >= Tuning.GREEN_COKE_KG && putOut(new ItemStack(WildcatterItems.GREEN_COKE))) {
			coke -= Tuning.GREEN_COKE_KG;
			pieces++;
		}
		if (coke >= Tuning.GREEN_COKE_KG) {
			unit = "FULL: take the coke (empty hand, or a hopper under it)";
			return;
		}
		CokeDrumBlockEntity drum = null;
		boolean any = false;
		for (BlockEntity e : Touching.parts(level, worldPosition)) {
			if (e instanceof CokeDrumBlockEntity d) {
				any = true;
				if (!d.filling() && d.coke() > 0.01) {
					drum = d;
				}
			}
		}
		if (!any) {
			unit = "NOT BUILT: stand the coke drums on it";
			return;
		}
		if (drum == null) {
			unit = String.format("idle: no drum to cut; %.0f/%.0f kg toward a piece", coke, Tuning.GREEN_COKE_KG);
			return;
		}
		if (water < 1) {
			unit = "NO WATER: pump cutting water to it";
			return;
		}
		double strength = Math.min(drive(level), drum.drive(level));
		if (strength <= 0) {
			unit = "starting: " + describe() + "; drum " + drum.motor().describe();
			return;
		}
		double kg = Math.min(Math.min(Tuning.COKE_CUT_KGS * seconds(), water / Tuning.COKE_CUT_WATER_PER_KG), Tuning.GREEN_COKE_KG * 2 - coke) * strength;
		kg = drum.takeCoke(kg);
		water -= kg * Tuning.COKE_CUT_WATER_PER_KG;
		coke += kg;
		unit = String.format("cutting %.0f kg/h of coke at 200 bar, %,.0f kg left in the drum; %.0f/%.0f kg toward a piece", kg / seconds() * 3600, drum.coke(), coke,
				Tuning.GREEN_COKE_KG);
	}

	/** For the game test: coke as if cut. */
	public void addCoke(double kg) {
		coke += kg;
		setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Coke pit: %s; %d pieces of green coke cut, %d waiting; water %.0f/%.0f L; ", unit, pieces, outCount(), water,
				Tuning.CARBON_WATER_HOLD) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("coke", coke);
		output.putDouble("water", water);
		output.putInt("pieces", pieces);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		coke = input.getDoubleOr("coke", 0);
		water = input.getDoubleOr("water", 0);
		pieces = input.getIntOr("pieces", 0);
	}
}
