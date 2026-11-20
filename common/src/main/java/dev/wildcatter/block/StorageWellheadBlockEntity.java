package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.well.Cavern;
import dev.wildcatter.well.CavernData;
import java.util.List;
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
 * The gas storage wellhead (docs/multiblocks/oilfield-follow-ons.md, 23c) on a leached cavern. Debrining: the storage compressor's gas
 * pushes the brine up the inner string and out of the side it faces to a disposal wellhead, the same volume, until DRY; the first gas
 * is the cushion (to the minimum pressure, 0.3 psi/ft as if at 1,000 m, not withdrawable). Then it stores working gas to the maximum
 * (0.85 psi/ft: HIGH PRESSURE) and gives it up to the glycol dehydrator (WET GAS without one), up to 5 Sm3/s, falling as the pressure
 * nears the minimum. PLC points: INJECT, WITHDRAW, SHUT IN; PRESSURE and WORKING GAS (4-20 mA); HIGH PRESSURE, LOW PRESSURE, WET GAS.
 */
public class StorageWellheadBlockEntity extends BlockEntity implements Inspectable, dev.wildcatter.control.ControlDevice {
	public enum Mode {
		SHUT_IN, INJECT, WITHDRAW
	}

	private Mode mode = Mode.SHUT_IN;
	private long plcAt = -1000;
	private Mode plcMode = Mode.SHUT_IN;
	private long dehydratorSeen = -1000;
	private double lastIn, lastOut;   // Sm3/s the last interval
	private int surveyLeft;
	private java.util.@Nullable UUID surveyor;
	private String status = "SHUT IN";

	public StorageWellheadBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.STORAGE_WELLHEAD_ENTITY, pos, state);
	}

	public CavernData.@Nullable Hole hole() {
		return level instanceof ServerLevel server ? CavernData.get(server).at(worldPosition) : null;
	}

	public Mode mode() {
		return level != null && level.getGameTime() - plcAt < 40 ? plcMode : mode;
	}

	public String cycleMode() {
		mode = Mode.values()[(mode.ordinal() + 1) % Mode.values().length];
		setChanged();
		return "Storage wellhead: " + mode.name().replace('_', ' ') + "; " + inspect().getString();
	}

	public void setMode(Mode m) {
		mode = m;
		setChanged();
	}

	public double volume() {
		CavernData.Hole hole = hole();
		return hole == null ? 0 : hole.volume();
	}

	public double gas() {
		CavernData.Hole hole = hole();
		return hole == null ? 0 : hole.gas;
	}

	public boolean dry() {
		CavernData.Hole hole = hole();
		return hole != null && hole.dry;
	}

	/** The cavern's pressure, bar: the brine's (about the minimum) while it debrines. */
	public double bar() {
		CavernData.Hole hole = hole();
		if (hole == null || hole.volume() == 0) {
			return 0;
		}
		return hole.dry ? Cavern.bar(hole.gas, hole.volume()) : Cavern.minBar();
	}

	public double working() {
		CavernData.Hole hole = hole();
		return hole == null || !hole.dry ? 0 : Cavern.working(hole.gas, hole.volume());
	}

	private boolean disposalOnBrineSide(Level level) {
		Direction out = getBlockState().getValue(StorageWellheadBlock.FACING);
		for (Lines.Line line : Lines.lines(level, List.of(new Lines.Face(worldPosition, out)))) {
			if (line.touches(level, DisposalWellheadBlock.class)) {
				return true;
			}
		}
		return false;
	}

	/** The storage compressor offers {@code sm3} of gas; returns what went in. */
	public double inject(Level level, double sm3) {
		CavernData.Hole hole = hole();
		if (hole == null || mode() != Mode.INJECT || sm3 <= 0) {
			return 0;
		}
		if (hole.breached) {
			status = "BREACHED: the cavern is lost";
			return 0;
		}
		if (hole.volume() == 0) {
			status = "NO CAVERN: leach one with a brine wellhead first";
			return 0;
		}
		double took;
		if (!hole.dry) {
			if (!disposalOnBrineSide(level)) {
				status = "DEBRINING: NO DISPOSAL: pipe the side it faces to a disposal wellhead for the brine";
				return 0;
			}
			double perM3 = Cavern.sm3PerM3(Cavern.minBar());   // gas at the cavern's pressure fills what the brine leaves
			took = Math.min(sm3, (hole.volume() - hole.debrined) * perM3);
			hole.gas += took;
			hole.debrined += took / perM3;
			if (hole.debrined >= hole.volume() - 1e-6) {
				hole.dry = true;
				hole.debrined = hole.volume();
				if (level instanceof ServerLevel server) {
					dev.wildcatter.quest.QuestEvents.milestone(server, worldPosition, dev.wildcatter.quest.Milestones.CAVERN_DRY);
					Drilling.announce(server, worldPosition, Component.literal("CAVERN DRY: debrined, its cushion gas in. It stores working gas now."));
				}
			}
			status = String.format("DEBRINING: %.0f of %d m3 of brine out to disposal", hole.debrined, hole.volume());
		} else {
			took = Math.min(sm3, Math.max(0, Cavern.capacity(hole.volume()) - hole.gas));
			hole.gas += took;
			status = took < sm3 * 0.5 ? "HIGH PRESSURE: full at " + String.format("%.0f bar", bar()) : "INJECTING";
		}
		lastIn += took;
		setChanged();
		return took;
	}

	/** The glycol dehydrator draws up to {@code sm3} of wet gas; returns what came out. */
	public double withdraw(Level level, double sm3) {
		dehydratorSeen = level.getGameTime();
		CavernData.Hole hole = hole();
		if (hole == null || mode() != Mode.WITHDRAW || !hole.dry || hole.breached) {
			return 0;
		}
		double share = Cavern.withdrawShare(hole.gas, hole.volume());
		double took = Math.min(sm3 * share, Cavern.working(hole.gas, hole.volume()));
		hole.gas -= took;
		lastOut += took;
		status = share < 1e-3 ? "LOW PRESSURE: down to its cushion" : String.format("WITHDRAWING at %.0f %% of full rate", share * 100);
		setChanged();
		return took;
	}

	public boolean wetGas() {
		return level != null && mode() == Mode.WITHDRAW && level.getGameTime() - dehydratorSeen > 60;
	}

	public boolean highPressure() {
		return dry() && bar() >= Cavern.maxBar() - 0.5;
	}

	public boolean lowPressure() {
		return dry() && bar() <= Cavern.minBar() + 0.5;
	}

	public void survey(net.minecraft.world.entity.player.Player player) {
		surveyLeft = Tuning.SONAR_TICKS;
		surveyor = player.getUUID();
	}

	/** Game test hook: gas as if injected (debrining it dry first). */
	public void testGas(double sm3) {
		CavernData.Hole hole = hole();
		if (hole != null) {
			hole.dry = true;
			hole.debrined = hole.volume();
			hole.gas += sm3;
		}
	}

	public static void tick(Level level, BlockPos pos, BlockState state, StorageWellheadBlockEntity head) {
		if (!(level instanceof ServerLevel server) || level.getGameTime() % 20 != 0) {
			return;
		}
		CavernData.Hole hole = head.hole();
		if (hole == null) {
			return;
		}
		if (head.mode() == Mode.WITHDRAW && head.wetGas()) {
			head.status = "WET GAS: withdrawal goes only through a glycol dehydrator";
		} else if (head.mode() == Mode.SHUT_IN) {
			head.status = "SHUT IN: the subsurface safety valve closed";
		}
		head.lastIn = head.lastOut = 0;
		Caverns.tickHeave(server, hole, pos);
		head.surveyLeft = Sonar.tick(server, pos, hole, head.surveyLeft, head.surveyor, "Storage wellhead");
		// cavernStability: held under its minimum pressure for a game week, the roof works loose
		if (dev.wildcatter.Rules.cavernStability() && hole.dry && !hole.breached && head.bar() < Cavern.minBar() * 0.99) {
			if (hole.lowSince < 0) {
				hole.lowSince = level.getGameTime();
			} else if (level.getGameTime() - hole.lowSince > Tuning.CAVERN_LOW_TICKS) {
				Caverns.heave(server, hole, pos);
			}
		} else {
			hole.lowSince = -1;
		}
	}

	// ---- PLC points --------------------------------------------------------------------------------------------------------------
	@Override
	public String deviceName() {
		return "Gas storage wellhead";
	}

	@Override
	public List<Point> points() {
		return List.of(new Point("INJECT", true), new Point("WITHDRAW", true), new Point("SHUT IN", true),
				new Point("PRESSURE 4-20 mA (0-200 bar)", false, Signal.MA), new Point("WORKING GAS 4-20 mA (0-full)", false, Signal.MA),
				new Point("HIGH PRESSURE", false), new Point("LOW PRESSURE", false), new Point("WET GAS", false));
	}

	@Override
	public boolean readPoint(int point) {
		return switch (point) {
			case 5 -> highPressure();
			case 6 -> lowPressure();
			case 7 -> wetGas();
			default -> false;
		};
	}

	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
		if (on && point <= 2) {
			plcMode = point == 0 ? Mode.INJECT : point == 1 ? Mode.WITHDRAW : Mode.SHUT_IN;
			plcAt = gameTime;
		}
	}

	@Override
	public double readMa(int point) {
		double v = volume();
		return switch (point) {
			case 3 -> 4 + 16 * Math.min(1.03, bar() / 200);
			case 4 -> 4 + 16 * (v <= 0 ? 0 : Math.min(1.03, working() / Math.max(1, Cavern.capacity(v) - Cavern.cushion(v))));
			default -> 0;
		};
	}

	@Override
	public Component inspect() {
		double v = volume();
		CavernData.Hole hole = hole();
		String rec = level == null || hole == null ? "" : Caverns.recordText(level, hole);
		return Component.literal(String.format("Gas storage wellhead (%s): %s; %.0f bar (%,.0f psi); working gas %,.0f Sm3 (%,.0f GJ), cushion %,.0f Sm3; %s",
				mode().name().replace('_', ' '), status, bar(), bar() / Tuning.BAR_PER_PSI, working(), Cavern.gj(working()), dry() ? Cavern.cushion(v) : gas(), rec));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("mode", mode.ordinal());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		mode = Mode.values()[Math.max(0, Math.min(2, input.getIntOr("mode", 0)))];
	}
}
