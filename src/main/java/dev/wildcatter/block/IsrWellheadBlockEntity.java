package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.well.RollFront;
import dev.wildcatter.well.RollFrontData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * An ISR well (docs/multiblocks/isr-wellfield.md). An injector takes its share of barren lixiviant from the header house's injection
 * manifold on its pipe and sends it down into the front. A producer's submersible pump, run by the header house its pipe leads to, lifts 2.5 L/s of pregnant lixiviant: its
 * uranium follows the 5-spot, the injectors injecting within {@link Tuning#ISR_PATTERN_RANGE} blocks of it (up to four), the
 * oxygen they carried, the grade of the ore at the well, and what is left of the front.
 */
public class IsrWellheadBlockEntity extends BlockEntity implements FluidNode, Mixed, Inspectable {
	/** The wells loaded in each level, so a producer finds its injectors without searching the ground. */
	private static final Map<Level, Set<BlockPos>> WELLS = Collections.synchronizedMap(new java.util.WeakHashMap<>());

	private String log = "";
	private double pregnant;      // litres held at the wellhead
	private double conc;          // kg of U3O8 a litre in them
	private double oxygen;        // the share of the lixiviant it injected that carried oxygen, smoothed
	private long injected = -1000;
	private long powered = -1000;
	private double strength;
	private double headGrade;     // mg/L, the last interval
	private int injectors;
	private String status = "starting";

	public IsrWellheadBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.ISR_WELLHEAD_ENTITY, pos, state);
	}

	@Override
	public void setLevel(Level level) {
		super.setLevel(level);
		WELLS.computeIfAbsent(level, l -> Collections.synchronizedSet(new java.util.HashSet<>())).add(worldPosition);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (level != null && WELLS.containsKey(level)) {
			WELLS.get(level).remove(worldPosition);
		}
	}

	public boolean producer() {
		return getBlockState().getValue(IsrWellheadBlock.PUMP);
	}

	/** The rig's gamma log of this hole. */
	public void setLog(String log) {
		this.log = log;
		setChanged();
	}

	public boolean injecting(Level level) {
		return !producer() && level.getGameTime() - injected < 40;
	}

	public double headGrade() {
		return headGrade;
	}

	public int injectors() {
		return injectors;
	}

	private double injecting;     // L/s, the last share

	/** The header house's injection manifold sends this injector {@code litres} this interval, {@code oxygen} of it oxygenated. */
	public void inject(Level level, double litres, double oxygen, double seconds) {
		if (litres > 0.0001) {
			injected = level.getGameTime();
			injecting = litres / seconds;
			this.oxygen = this.oxygen * 0.8 + oxygen * 0.2;
			setChanged();
		}
	}

	/** The header house runs this producer's pump at {@code strength} (its drives' share, 0 to 1) this interval. */
	public void power(Level level, double strength) {
		this.powered = level.getGameTime();
		this.strength = strength;
	}

	private @Nullable RollFront front(ServerLevel level) {
		return Drilling.rollFront(level, worldPosition);
	}

	private boolean cased(Level level) {
		return level.getBlockState(worldPosition.below()).is(WildcatterBlocks.CASING);
	}

	private List<Lines.Face> faces() {
		List<Lines.Face> out = new ArrayList<>();
		for (Direction side : Direction.Plane.HORIZONTAL) {
			out.add(new Lines.Face(worldPosition, side));
		}
		return out;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, IsrWellheadBlockEntity w) {
		if (!(level instanceof ServerLevel server) || (level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		double seconds = Tuning.WELL_INTERVAL / 20.0;
		RollFront front = w.front(server);
		if (!w.cased(level)) {
			w.status = "NOT A WELL: a rig drills and cases a hole before a wellhead goes on it";
		} else if (front == null) {
			w.status = "NOT ON A ROLL-FRONT: no ore under this well";
		} else if (!w.producer()) {
			w.status = level.getGameTime() - w.injected < 40 ? String.format("INJECTOR: injecting %.2f L/s of lixiviant, %.0f %% of it with oxygen", w.injecting, w.oxygen * 100)
					: "INJECTOR: idle, no lixiviant from a header house on its pipe";
		} else if (level.getGameTime() - w.powered > 40) {
			w.status = "PRODUCER: its pump is idle; pipe the well to a header house, which runs its pump";
		} else {
			double oxygen = 0;
			int n = 0;
			Set<BlockPos> wells = WELLS.getOrDefault(level, Set.of());
			synchronized (wells) {
				for (BlockPos other : wells) {
					double dx = other.getX() - pos.getX(), dz = other.getZ() - pos.getZ();
					if (!other.equals(pos) && dx * dx + dz * dz <= Tuning.ISR_PATTERN_RANGE * Tuning.ISR_PATTERN_RANGE
							&& level.getBlockEntity(other) instanceof IsrWellheadBlockEntity injector && injector.injecting(level)
							&& front.equals(injector.front(server))) {
						n++;
						oxygen += injector.oxygen;
					}
				}
			}
			w.injectors = n;
			RollFrontData data = RollFrontData.get(server);
			w.headGrade = dev.wildcatter.fluid.Isr.headGrade(n, n > 0 ? oxygen / n : 0, front.grade(pos.getX(), pos.getZ()), data.left(front));
			double litres = Math.min(Tuning.ISR_PRODUCER_LPS * seconds * w.strength, Math.max(0, Tuning.ISR_PRODUCER_LPS * Tuning.PLANT_HOLD_SECONDS - w.pregnant));
			double u = data.draw(front, litres * w.headGrade * 1e-6);
			w.conc = w.pregnant + litres > 0 ? (w.conc * w.pregnant + u) / (w.pregnant + litres) : 0;
			w.pregnant += litres;
			w.status = String.format("PRODUCER: pumping %.2f L/s at %.0f mg/L U3O8, %d of 4 injectors in its pattern%s; the front has %.0f %% of its uranium left",
					litres / seconds, w.headGrade, n, n > 0 && oxygen / n < 0.5 ? " (NO OXYGEN in their lixiviant)" : "", data.left(front) * 100);
		}
		w.setChanged();
	}

	// ---- its pipe: a producer offers pregnant lixiviant on every side ----------------------------------------------------------
	@Override
	public @Nullable Product offers(BlockPos cell, Direction side) {
		return producer() && side.getAxis().isHorizontal() ? Product.PREGNANT_LIXIVIANT : null;
	}

	@Override
	public double available(BlockPos cell, Direction side, Product product, Pull pull) {
		return offers(cell, side) == product && pull == Pull.MACHINE ? pregnant : 0;
	}

	@Override
	public double take(BlockPos cell, Direction side, Product product, double max, Pull pull) {
		double got = Math.min(max, available(cell, side, product, pull));
		if (got > 0) {
			pregnant -= got;
			setChanged();
		}
		return got;
	}

	@Override
	public double[] mixOf(Product product) {
		return new double[] {conc, 0, 0, 0, 0};
	}

	@Override
	public Component inspect() {
		return Component.literal("ISR well: " + status + (log.isEmpty() ? "" : "; " + log));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putString("log", log);
		output.putDouble("pregnant", pregnant);
		output.putDouble("conc", conc);
		output.putDouble("oxygen", oxygen);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		log = input.getStringOr("log", "");
		pregnant = input.getDoubleOr("pregnant", 0);
		conc = input.getDoubleOr("conc", 0);
		oxygen = input.getDoubleOr("oxygen", 0);
	}
}
