package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.Steam;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The pressurizer relief tank (docs/multiblocks/reactor-extensions.md, 19a): a horizontal tank of 30 m3 of water against the
 * pressurizer or the vessel. What the PORVs let go is sparged into its water and condenses, warming it; at 0.7 MPa its rupture disc
 * bursts and what follows goes on into the building. A sneak-click turns its spray off the CVCS's makeup water on or off; on a burst
 * tank under 100 C, it fits a new disc. Without one the PORVs discharge as before.
 */
public class ReliefTankBlockEntity extends PlantUnitBlockEntity {
	private double waterC = 30;
	private double kg = Tuning.RELIEF_TANK_KG;
	private boolean burst;
	private boolean spray;
	private double received;
	private boolean justBurst;

	public ReliefTankBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.RELIEF_TANK_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	public double waterC() {
		return waterC;
	}

	public boolean burst() {
		return burst;
	}

	/** Its pressure: the water's saturation pressure, no lower than the atmosphere's. */
	public double pressure() {
		return Math.max(0.1013, Steam.psat(waterC));
	}

	/** True once, the step its disc bursts (19b: its contamination). */
	public boolean takeBurst() {
		boolean was = justBurst;
		justBurst = false;
		return was;
	}

	/**
	 * The vessel's step: the PORVs' discharge ({@code kgIn} of steam carrying {@code joules}) into its water, and its spray off the
	 * CVCS. Returns the kg that goes on into the building (all of it once the disc has burst).
	 */
	public double relieve(Level level, double kgIn, double joules, @Nullable CvcsBlockEntity cvcs, double dt) {
		if (spray && cvcs != null && !burst) {
			double s = Math.min(Tuning.RELIEF_SPRAY_KGS * dt, cvcs.water());
			if (s > 0) {
				cvcs.use(0, s);
				waterC = (kg * waterC + s * 30) / (kg + s);   // the warm water drains to the drain tank as the spray comes in
			}
		}
		if (kgIn <= 0) {
			return 0;
		}
		if (burst) {
			return kgIn;
		}
		received += kgIn;
		// the steam condenses in the water: its enthalpy over the water's
		double cp = 4186;
		waterC = (kg * cp * waterC + joules) / ((kg + kgIn) * cp);
		kg = Math.min(kg + kgIn, Tuning.RELIEF_TANK_KG * 1.2);
		if (pressure() >= Tuning.RELIEF_DISC_MPA) {
			burst = true;
			justBurst = true;
			if (level instanceof ServerLevel sl) {
				BlockPos c = worldPosition.above(2);
				sl.sendParticles(ParticleTypes.CLOUD, c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 80, 1.5, 0.8, 2.5, 0.08);
			}
		}
		setChanged();
		return 0;
	}

	/** For the game test: the water at {@code c} C. */
	public void setWaterForTest(double c) {
		waterC = c;
		setChanged();
	}

	public String sneakClick() {
		if (burst && waterC < 100) {
			burst = false;
			kg = Tuning.RELIEF_TANK_KG;
			setChanged();
			return "Pressurizer relief tank: a new rupture disc fitted";
		}
		if (burst) {
			return String.format("Pressurizer relief tank: the disc has burst; cool the water under 100 C (%.0f C) to fit a new one", waterC);
		}
		spray = !spray;
		setChanged();
		return "Pressurizer relief tank: spray " + (spray ? "ON (the CVCS's makeup water)" : "OFF");
	}

	@Override
	public List<Point> points() {
		return List.of(new Point("DISC BURST", false), new Point("TEMPERATURE 4-20 mA, 200 C", false, Signal.MA), new Point("PRESSURE 4-20 mA, 1 MPa", false, Signal.MA));
	}

	@Override
	public boolean readPoint(int point) {
		return point == 0 && burst;
	}

	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
	}

	@Override
	public double readMa(int point) {
		double share = point == 1 ? waterC / 200 : point == 2 ? pressure() : 0;
		return 4 + 16 * Math.max(0, Math.min(1.03, share));
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Pressurizer relief tank (%.0f m3): water %.1f C, %.2f MPa (rupture disc %.1f MPa%s); spray %s; %.0f kg taken from the PORVs",
				kg / 1000, waterC, pressure(), Tuning.RELIEF_DISC_MPA, burst ? ", BURST: the discharge goes into the building" : "", spray ? "ON" : "OFF", received));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("waterC", waterC);
		output.putDouble("kg", kg);
		output.putBoolean("burst", burst);
		output.putBoolean("spray", spray);
		output.putDouble("received", received);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		waterC = input.getDoubleOr("waterC", 30);
		kg = input.getDoubleOr("kg", Tuning.RELIEF_TANK_KG);
		burst = input.getBooleanOr("burst", false);
		spray = input.getBooleanOr("spray", false);
		received = input.getDoubleOr("received", 0);
	}
}
