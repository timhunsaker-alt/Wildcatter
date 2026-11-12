package dev.wildcatter.block;

import dev.wildcatter.TimeScale;
import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Nickel;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Peirce-Smith converter (docs/multiblocks/nickel-smelter.md): a horizontal drum on its tuyere line, its air blower 250 kW at MV (box
 * west). It charges the furnace matte of the electric furnace it touches and blows it for 2 hours (at the world's time scale), the air
 * oxidising the iron to slag (back to the furnace, counted) and the sulfur to SO2 out its south side as burner gas for an acid plant's
 * converter (NO ACID PLANT otherwise). Bessemer matte (50 % Ni, 25 % Cu, 20 % S) waits for the chlorine leach reactor it touches or for
 * the bagging station set against it.
 */
public class PeirceSmithConverterBlockEntity extends UnitMotorBlockEntity implements Bagged {
	private boolean blowing;
	private double chargeNi;      // kg of Ni in the blow
	private double blowLeft;      // s
	private double blowTotal;     // s, at the time scale it began at
	private double bessemerNi;    // kg of Ni in the Bessemer matte waiting
	private int blows;
	private double gasRate;       // kg/s of burner gas, the last interval
	private String unit = "starting";

	public PeirceSmithConverterBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.PEIRCE_SMITH_CONVERTER_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[0] == 2 && at[1] == 0 && at[2] == 9 && side == Direction.SOUTH ? Product.BURNER_GAS : null;
	}

	/** A few seconds of its gas at the fastest a blow goes (a full charge at the world's time scale). */
	@Override
	protected double hold(Product product) {
		if (product != Product.BURNER_GAS) {
			return 0;
		}
		double perSecond = Tuning.PS_CHARGE_MAX_KG / Nickel.furnaceMattePerNi() * Nickel.converterGasPerNi() / TimeScale.scaled(Tuning.PS_BLOW_SECONDS);
		return Math.max(perSecond * Tuning.PLANT_HOLD_SECONDS, perSecond * seconds() * 3);
	}

	public boolean blowing() {
		return blowing;
	}

	/** kg of Bessemer matte waiting. */
	public double bessemer() {
		return bessemerNi * Nickel.bessemerPerNi();
	}

	public double bessemerNi() {
		return bessemerNi;
	}

	/** The leach reactor takes up to {@code kg} of Ni as Bessemer matte granules; returns what it gave. */
	public double takeBessemerNi(double kg) {
		double got = Math.max(0, Math.min(kg, bessemerNi));
		bessemerNi -= got;
		setChanged();
		return got;
	}

	public double gasRate() {
		return gasRate;
	}

	/** For the game test: the blow's time skipped; the next interval finishes it. */
	public void testFinishBlow() {
		if (blowing) {
			blowLeft = Math.min(blowLeft, 0.001);
		}
	}

	@Override
	public Item bagItem() {
		return WildcatterItems.BESSEMER_MATTE_BAG;
	}

	@Override
	public double bagReady() {
		return bessemer();
	}

	@Override
	public double takeForBag(double kg) {
		return takeBessemerNi(kg / Nickel.bessemerPerNi()) * Nickel.bessemerPerNi();
	}

	@Override
	protected void work(Level level) {
		gasRate = 0;
		double dt = seconds();
		NickelFurnaceBlockEntity furnace = Touching.find(level, worldPosition, NickelFurnaceBlockEntity.class, f -> true);
		if (!blowing) {
			if (furnace == null) {
				unit = "NOT BUILT: set it against the nickel electric furnace";
				return;
			}
			if (bessemer() >= Tuning.PS_MATTE_HOLD_KG) {
				unit = "FULL: its Bessemer matte is not being taken (the leach reactor, or a bagging station against it)";
				return;
			}
			if (furnace.matte() < Tuning.PS_CHARGE_MIN_KG) {
				unit = String.format("waiting for furnace matte: %.1f t in the furnace, a blow takes at least %.1f t", furnace.matte() / 1000, Tuning.PS_CHARGE_MIN_KG / 1000);
				return;
			}
			if (plcHold(level)) {
				unit = status;
				return;
			}
			chargeNi = furnace.takeMatteNi(Tuning.PS_CHARGE_MAX_KG / Nickel.furnaceMattePerNi());
			blowTotal = TimeScale.scaled(Tuning.PS_BLOW_SECONDS);
			blowLeft = blowTotal;
			blowing = true;
		}
		double share = Math.min(dt, blowLeft) / blowTotal;
		double gas = chargeNi * Nickel.converterGasPerNi() * share;
		if (room(Product.BURNER_GAS) < gas) {
			unit = "NO ACID PLANT: pipe its off-gas (south side) to an acid plant's converter; it must not vent";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting the air blower: " + describe();
			return;
		}
		add(Product.BURNER_GAS, gas * strength);
		gasRate = gas * strength / dt;
		blowLeft -= dt * strength;
		if (blowLeft <= 0) {
			bessemerNi += chargeNi;
			if (furnace != null) {   // its slag back to the furnace: the iron of the furnace matte, oxidised (counted)
				furnace.returnSlag(chargeNi * (Nickel.furnaceMattePerNi() - Nickel.bessemerPerNi()) / 1000);
			}
			blows++;
			blowing = false;
			chargeNi = 0;
			unit = String.format("blow finished: %.1f t of Bessemer matte (50 %% Ni) waiting", bessemer() / 1000);
			return;
		}
		unit = String.format("BLOWING %.1f t of furnace matte: %.0f min left; %.0f kg/h of SO2 to the acid plant", chargeNi * Nickel.furnaceMattePerNi() / 1000,
				blowLeft / 60, gasRate * 3600 / dev.wildcatter.fluid.AcidPlant.gasPerSulfur() * 2);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Peirce-Smith converter (%.0f kW air blower, %s): ", Tuning.PS_BLOWER_WATTS / 1000, Voltage.MEDIUM.label()) + unit
				+ String.format("; Bessemer matte %.1f t waiting (%.0f kg of Ni); %d blows; ", bessemer() / 1000, bessemerNi, blows) + describe());
	}

	@Override
	public List<Point> points() {
		return RatePoints.POINTS;
	}

	@Override
	public boolean readPoint(int point) {
		return RatePoints.read(point, blowing, unitRemote.auto());
	}

	@Override
	public double readMa(int point) {
		return RatePoints.ma(blowing ? 1 : 0, 1);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("blowing", blowing);
		output.putDouble("chargeNi", chargeNi);
		output.putDouble("blowLeft", blowLeft);
		output.putDouble("blowTotal", blowTotal);
		output.putDouble("bessemerNi", bessemerNi);
		output.putInt("blows", blows);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		blowing = input.getBooleanOr("blowing", false);
		chargeNi = input.getDoubleOr("chargeNi", 0);
		blowLeft = input.getDoubleOr("blowLeft", 0);
		blowTotal = input.getDoubleOr("blowTotal", Tuning.PS_BLOW_SECONDS);
		bessemerNi = input.getDoubleOr("bessemerNi", 0);
		blows = input.getIntOr("blows", 0);
	}
}
