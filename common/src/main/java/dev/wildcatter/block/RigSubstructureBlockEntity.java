package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.kinetic.Kinetic;
import dev.wildcatter.kinetic.RotorBlock;
import dev.wildcatter.kinetic.Spin;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.well.Reservoir;
import dev.wildcatter.well.ReservoirData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * The land rig's controller, in its substructure (docs/multiblocks/land-rig.md). It checks the rig every 20 ticks, keeps the top
 * drive, the mud water, the depth and the stand, tells the drive house how much power it wants, and drills as fast as the power
 * it gets allows: {@link Tuning#LAND_RIG_TICKS_PER_BLOCK} a block at full power, a 15 s connection after each 27 m stand. On a
 * strike it sets the wellhead in the hole cell under its floor and stops.
 */
public class RigSubstructureBlockEntity extends BlockEntity implements Inspectable, BigPartEntity, Kinetic, dev.wildcatter.control.ControlDevice, dev.wildcatter.kinetic.DrawnRuns {
	private final dev.wildcatter.control.Remote remote = new dev.wildcatter.control.Remote(1);

	@Override
	public String deviceName() {
		return "Drilling rig";
	}

	@Override
	public java.util.List<Point> points() {
		return java.util.List.of(new Point("DRILL ENABLE", true), new Point("DRILLING", false), new Point("MUD PUMPS", false), new Point("IN A CONNECTION", false), new Point("AUTO", false),
				new Point("KICK", false), new Point("SHUT IN", true), new Point("SHUT IN (state)", false), new Point("MUD WEIGHT 4-20 mA (8-20 ppg)", false, Signal.MA),
				new Point("PIT VOLUME 4-20 mA (0-60 m3)", false, Signal.MA));
	}

	@Override
	public boolean readPoint(int point) {
		return switch (point) {
			case 1 -> drilling();
			case 2 -> pumping();
			case 3 -> connecting();
			case 4 -> remote.auto();
			case 5 -> phase == Phase.KICK || phase == Phase.BLOWOUT;
			case 7 -> phase == Phase.SHUT_IN;
			default -> false;
		};
	}

	@Override
	public double readMa(int point) {
		return switch (point) {
			case 8 -> 4 + 16 * Math.max(0, Math.min(1, (mudPpg() - 8) / 12));
			case 9 -> 4 + 16 * Math.max(0, Math.min(1, mudVolume() / Tuning.LAND_RIG_MUD_TANK));
			default -> 0;
		};
	}

	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
		if (point == 0) {
			remote.command(0, on, gameTime);
		} else if (point == 6 && on && (phase == Phase.KICK || phase == Phase.BLOWOUT) && level instanceof ServerLevel server) {
			shutIn(server);   // the PLC's SHUT IN command
		}
	}

	@Override
	public String cycleHoa(BlockPos cell) {
		setChanged();
		return remote.cycle();
	}

	/** In AUTO it drills while enabled; a rig past idle or drilling, one finished, stays as it is. */
	private void followPlc(long now) {
		if (!remote.hand()) {
			boolean want = remote.on(0, now);
			if (want && phase == Phase.IDLE) {
				phase = Phase.DRILLING;
				setChanged();
			} else if (!want && phase == Phase.DRILLING) {
				phase = Phase.IDLE;
				setChanged();
			}
		}
	}

	/** The top drive's quill at the top of a stand, above the hole (the floor's top is 6 m up). */
	private static final float TOP_DRIVE_TOP = 6.0F + Tuning.LAND_RIG_STAND_BLOCKS + 3.0F;

	public enum Phase {
		IDLE, DRILLING, COMPLETE, DRY,
		/** 15b2 (docs/multiblocks/drilling-mud.md): the well flowing into the hole, the shut-in well, a wild well, one killed by a relief well. */
		KICK, SHUT_IN, BLOWOUT, KILLED
	}

	// ---- 15b2: well control -------------------------------------------------------------------------------------------------------
	/** The rigs blowing out now, for a relief well to find. Server side only. */
	private static final java.util.Set<RigSubstructureBlockEntity> BLOWING = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
	private @Nullable BlockPos accumulatorAt;
	private long accumulatorSeen = -1000;
	private long kickAt;
	private double pitGain;
	private double killPpg;
	private double sidpp;
	private double killing;
	private double blowCarry;
	private boolean onFire;

	/** The accumulator within reach tells the rig it is there, every 20 ticks. */
	void accumulatorHere(BlockPos at, long now) {
		accumulatorAt = at;
		accumulatorSeen = now;
	}

	public Phase phase() {
		return phase;
	}

	public boolean onFire() {
		return onFire;
	}

	/** The BOP stack on the wellhead cell under the rotary table, or null. */
	public @Nullable BopStackBlockEntity bop() {
		return level != null && dev.wildcatter.multiblock.BigParts.entity(level, hole().above()) instanceof BopStackBlockEntity stack ? stack : null;
	}

	public @Nullable BopAccumulatorBlockEntity accumulator() {
		return level != null && accumulatorAt != null && level.getGameTime() - accumulatorSeen < 80
				&& level.getBlockEntity(accumulatorAt) instanceof BopAccumulatorBlockEntity a ? a : null;
	}

	/** A BOP stack under the floor and its accumulator within reach: what a geopressured (deep) field needs. */
	public boolean hasBop() {
		return bop() != null && accumulator() != null;
	}

	/** The pore pressure of a field as an equivalent mud weight: deep fields are geopressured. */
	public static double porePpg(Reservoir field) {
		return field.deep() ? Tuning.DEEP_PORE_PPG : Tuning.NORMAL_PORE_PPG;
	}

	/** The hole's true vertical depth, feet. */
	private double tvdFeet() {
		return depth * Tuning.FEET_PER_M;
	}

	/** The mud is under the field's pore pressure and the trip margin: the well kicks. */
	private void kick(ServerLevel level) {
		phase = Phase.KICK;
		kickAt = level.getGameTime();
		pitGain = 0;
		level.playSound(null, hole(), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0F, 0.6F);
		Drilling.announce(level, hole(), Component.literal("KICK! The pits are gaining: shut the well in (sneak-click the driller's console) within "
				+ Tuning.KICK_WINDOW_TICKS / 20 + " s"));
		setChanged();
		sync();
	}

	/**
	 * Closes the annular and the rams from the accumulator: in a kick's window or on a wild well whose stack still stands. Returns
	 * what happened, for the console.
	 */
	public Component shutIn(ServerLevel level) {
		if (phase != Phase.KICK && phase != Phase.BLOWOUT) {
			return Component.literal("Nothing to shut in: " + status);
		}
		BopStackBlockEntity stack = bop();
		BopAccumulatorBlockEntity acc = accumulator();
		if (stack == null || acc == null) {
			return Component.literal("NO BOP: no stack or no accumulator to close it");
		}
		if (!acc.takeClose()) {
			return Component.literal("The accumulator's bottles are spent: no close left");
		}
		stack.setClosed(true);
		Reservoir field = Drilling.field(level, hole());
		double pore = field != null ? porePpg(field) : Tuning.DEEP_PORE_PPG;
		sidpp = Math.max(0, (pore - mudPpg()) * Tuning.PSI_PER_PPG_FT * tvdFeet());   // SIDPP = 0.052 x (pore - mud) x TVD
		killPpg = pore + Tuning.TRIP_MARGIN_PPG;
		killing = 0;
		phase = Phase.SHUT_IN;
		BLOWING.remove(this);
		setChanged();
		sync();
		return Component.literal("SHUT IN: annular and rams closed. " + status());
	}

	private String status() {
		return String.format("SHUT IN: SIDPP %,.0f psi, kill weight %.1f ppg (mud %.2f ppg, %.1f m3 pit gain)", sidpp, killPpg, mudPpg(), pitGain);
	}

	/** The wild well flows, may ignite, and can still be shut in or killed by a relief well. */
	private void blowout(ServerLevel level) {
		phase = Phase.BLOWOUT;
		BLOWING.add(this);
		Drilling.announce(level, hole(), Component.literal("BLOWOUT! The well is flowing wild round the rig floor. Keep fire away; close the BOP if it stands, or drill a relief well."));
		setChanged();
		sync();
	}

	/** Every tick of a kick, a shut-in well or a blowout. */
	private void wellControl(ServerLevel level, double power) {
		long now = level.getGameTime();
		Reservoir field = Drilling.field(level, hole());
		switch (phase) {
			case KICK -> {
				pitGain += Tuning.KICK_GAIN_M3S / 20;
				long left = Tuning.KICK_WINDOW_TICKS - (now - kickAt);
				status = String.format("KICK: PIT GAIN %.1f m3: shut in (sneak-click the driller's console) within %d s", pitGain, Math.max(0, left / 20));
				if (now % 40 == 0) {
					level.playSound(null, hole(), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0F, 0.6F);
				}
				if (left <= 0) {
					blowout(level);
				}
			}
			case SHUT_IN -> {
				if (mudPpg() + 1e-6 < killPpg) {
					status = status() + ": weight the mud to the kill weight";
				} else {
					killing += power;
					status = String.format("CIRCULATING KILL MUD through the choke: %.0f %%", Math.min(100, killing * 100 / Tuning.KILL_CIRCULATION_TICKS));
					if (killing >= Tuning.KILL_CIRCULATION_TICKS) {
						BopStackBlockEntity stack = bop();
						if (stack != null) {
							stack.setClosed(false);
						}
						phase = Phase.DRILLING;
						if (field != null) {
							strike(level, field);
						}
					}
				}
			}
			case BLOWOUT -> {
				status = onFire ? "BLOWOUT ON FIRE: the rig is burning" : "BLOWOUT: the well is flowing wild; close the BOP if it stands, or kill it with a relief well";
				if (field != null) {
					blowCarry += Tuning.BLOWOUT_FLOW * Tuning.GAS_UNITS_PER_TICK;
					int whole = (int) blowCarry;
					if (whole > 0) {
						blowCarry -= whole;
						ReservoirData.get(level).draw(field, whole);
					}
					if (ReservoirData.get(level).remaining(field) <= 0) {
						phase = Phase.DRY;
						BLOWING.remove(this);
						status = "BLOWN DOWN: the field is empty";
					}
				}
				BlockPos h = hole();
				if (now % 4 == 0) {
					level.sendParticles(onFire ? net.minecraft.core.particles.ParticleTypes.FLAME : net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
							h.getX() + 0.5, h.getY() + 7, h.getZ() + 0.5, 20, 0.6, 2.0, 0.6, 0.1);
				}
				if (!onFire && now % 20 == 0 && fireNear(level, h)) {
					ignite(level, h);
				}
			}
			default -> {
			}
		}
		setChanged();
	}

	/** A fire, lava or a lit campfire within {@link Tuning#IGNITION_RANGE} of the hole: the gas round the floor finds it. */
	private static boolean fireNear(ServerLevel level, BlockPos hole) {
		int r = Tuning.IGNITION_RANGE;
		for (BlockPos p : BlockPos.betweenClosed(hole.offset(-r, -2, -r), hole.offset(r, 10, r))) {
			BlockState state = level.getBlockState(p);
			if (state.is(net.minecraft.tags.BlockTags.FIRE) || state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)
					|| (state.is(net.minecraft.tags.BlockTags.CAMPFIRES) && state.getValue(net.minecraft.world.level.block.CampfireBlock.LIT))) {
				return true;
			}
		}
		return false;
	}

	/** The gas round the floor lights: fire on the ground under the floor round the hole. */
	private void ignite(ServerLevel level, BlockPos hole) {
		onFire = true;
		for (BlockPos p : BlockPos.betweenClosed(hole.offset(-2, 0, -2), hole.offset(2, 0, 2))) {
			if (level.getBlockState(p).isAir() && !p.equals(hole) && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) {
				level.setBlock(p, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState(), 3);
			}
		}
		level.playSound(null, hole, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 3.0F, 0.6F);
		Drilling.announce(level, hole, Component.literal("The blowout has caught fire: the rig is burning"));
	}

	/** A relief well that has reached this field with kill-weight mud kills every wild well on it at least 20 blocks off. */
	private void relieve(ServerLevel level, Reservoir field) {
		for (RigSubstructureBlockEntity wild : java.util.List.copyOf(BLOWING)) {
			if (wild != this && wild.level == level && !wild.isRemoved() && wild.phase == Phase.BLOWOUT
					&& wild.hole().distSqr(hole()) >= Tuning.RELIEF_MIN_DISTANCE * Tuning.RELIEF_MIN_DISTANCE) {
				Reservoir theirs = Drilling.field(level, wild.hole());
				if (theirs != null && theirs.key().equals(field.key())) {
					wild.phase = Phase.KILLED;
					wild.status = "KILLED by a relief well's kill-weight mud";
					BLOWING.remove(wild);
					wild.setChanged();
					wild.sync();
					Drilling.announce(level, wild.hole(), Component.literal("The relief well has killed the blowout"));
				}
			}
		}
	}

	/** Game test hook: as if it had already drilled (and cased) this far. */
	public void testDepth(int blocks) {
		depth = blocks;
		stand = 0;
		setChanged();
	}

	/** Game test hook: as if this many ticks of full-power circulation had gone by. */
	public void testCirculated(double ticks) {
		killing += ticks;
	}

	/** Game test hook: as if the kick's window had run out. */
	public void testKickTimeout() {
		kickAt -= Tuning.KICK_WINDOW_TICKS;
	}

	private Phase phase = Phase.IDLE;
	private boolean formed;
	private String missing = "";
	private boolean topDrive;
	private int water;
	/** kg of barite, bentonite and rock salt mixed into the mud in the tanks (docs/multiblocks/drilling-mud.md). */
	private double barite, bentonite, salt;
	/** Blocks of rock salt drilled at the bottom of the hole: a washout fills them back in. */
	private int saltMetres;
	private int depth;
	/** Blocks drilled in the current stand, and progress toward the next block in ticks of full power. */
	private int stand;
	private double progress;
	/** Ticks of full power left in the connection under way (0: drilling). */
	private double connection;
	private boolean grown;
	private String status = "";
	private final Spin spin = new Spin();
	/** Client side: the top drive's drawn height, eased toward where the stand puts it. */
	private float shownHead = Float.NaN;
	private long shownAt;

	public RigSubstructureBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.RIG_SUBSTRUCTURE_ENTITY, pos, state);
	}

	public Direction facing() {
		return getBlockState().getValue(RigPartBlock.FACING);
	}

	public BlockPos hole() {
		return LandRig.hole(worldPosition, facing());
	}

	public boolean formed() {
		return formed;
	}

	/**
	 * The motor cables from the drive house (docs/questions/misc-gaps.md, 4), in the substructure's frame (the hole is at x 4..5, z
	 * 4..5; LandRig's offsets): a ladder tray on the ground out of the drive house's east end, along its side and up the pumps' east
	 * side, with each pump's three cables rising to its motor's terminal box; and a tray out of the drive house's north side, up the
	 * back of the substructure to the drill floor, the drawworks motor's cables over to its terminal box. Drawn once the rig is formed.
	 */
	private static final List<dev.wildcatter.kinetic.DrawnRuns.Run> CABLES = List.of(
			dev.wildcatter.kinetic.DrawnRuns.Run.of(dev.wildcatter.kinetic.DrawnRuns.Kind.TRAY, "block/mat/mat_galvanized", 0.45,
					new double[] {10.0, 0.05, 13.0}, new double[] {19.6, 0.05, 13.0}, new double[] {19.6, 0.05, 1.25}),
			dev.wildcatter.kinetic.DrawnRuns.Run.of(dev.wildcatter.kinetic.DrawnRuns.Kind.CABLES, "block/mat/mat_rubber", 0.03,
					new double[] {19.6, 0.1, 5.25}, new double[] {19.6, 2.08, 5.25}, new double[] {19.0, 2.08, 5.25}),
			dev.wildcatter.kinetic.DrawnRuns.Run.of(dev.wildcatter.kinetic.DrawnRuns.Kind.CABLES, "block/mat/mat_rubber", 0.03,
					new double[] {19.6, 0.1, 1.25}, new double[] {19.6, 2.08, 1.25}, new double[] {19.0, 2.08, 1.25}),
			dev.wildcatter.kinetic.DrawnRuns.Run.of(dev.wildcatter.kinetic.DrawnRuns.Kind.TRAY, "block/mat/mat_galvanized", 0.45,
					new double[] {5.25, 0.05, 11.0}, new double[] {5.25, 0.05, 9.15}, new double[] {5.25, 8.3, 9.15}),
			dev.wildcatter.kinetic.DrawnRuns.Run.of(dev.wildcatter.kinetic.DrawnRuns.Kind.CABLES, "block/mat/mat_rubber", 0.03,
					new double[] {5.25, 8.3, 9.2}, new double[] {5.25, 8.3, 7.5}, new double[] {6.25, 8.3, 7.5}, new double[] {6.25, 8.05, 7.5}));

	@Override
	public List<dev.wildcatter.kinetic.DrawnRuns.Run> drawnRuns() {
		return formed ? CABLES : List.of();
	}

	public boolean drilling() {
		return phase == Phase.DRILLING && formed && topDrive;
	}

	/** In a connection: the drawworks hoisting, the pumps stopped. */
	public boolean connecting() {
		return drilling() && connection > 0;
	}

	/** The mud pumps run while it drills between connections, with mud in the tanks. */
	public boolean pumping() {
		return drilling() && connection <= 0 && mudVolume() >= Tuning.LAND_RIG_MUD_PER_BLOCK;
	}

	// ---- the mud (docs/multiblocks/drilling-mud.md): water, bentonite gel, barite weight, salt --------------------------------------
	/** What the mixing hopper puts into the tanks. */
	public enum Solid {
		BARITE("barite", Tuning.BARITE_SG), BENTONITE("bentonite", Tuning.BENTONITE_SG), SALT("rock salt", Tuning.SALT_APPARENT_SG);

		private final String label;
		final double sg;

		Solid(String label, double sg) {
			this.label = label;
			this.sg = sg;
		}

		public String label() {
			return label;
		}
	}

	/** kg of salt dissolved: up to saturation; the rest lies undissolved in the pits. */
	private double dissolvedSalt() {
		return Math.min(salt, water * Tuning.SALT_SATURATION_KG_PER_L);
	}

	/** Litres of mud in the tanks: the water and what the solids add to it. */
	public double mudVolume() {
		return water + barite / Tuning.BARITE_SG + bentonite / Tuning.BENTONITE_SG + dissolvedSalt() / Tuning.SALT_APPARENT_SG;
	}

	/** The mud's density, kg/L (water alone 1.0). */
	public double mudDensity() {
		double v = mudVolume();
		return v <= 0 ? Tuning.WATER_KG_PER_L : (water * Tuning.WATER_KG_PER_L + barite + bentonite + dissolvedSalt()) / v;
	}

	/** The mud weight in lb/gal (ppg), as a mud engineer reads it off a mud balance: water 8.34. */
	public double mudPpg() {
		return mudDensity() * Tuning.PPG_PER_KG_L;
	}

	/** Bentonite in the mud, lb/bbl (1 kg/m3 = 0.3505 lb/bbl). */
	public double gelLbBbl() {
		double v = mudVolume();
		return v <= 0 ? 0 : bentonite / v * 1000 * 0.3505;
	}

	/** Below 15 lb/bbl of bentonite the cuttings settle when the pumps stop: the rate of penetration is halved. */
	public boolean lowGel() {
		double v = mudVolume();
		return v <= 0 || bentonite / v < Tuning.LOW_GEL_KG_PER_L;
	}

	/** Salt-saturated: as much salt as the water holds (360 kg a m3), so rock salt does not dissolve into the hole. */
	public boolean saltSaturated() {
		return water > 0 && salt >= water * Tuning.SALT_SATURATION_KG_PER_L * Tuning.SALT_SATURATED_AT;
	}

	/** "8.34 ppg (1,000 kg/m3), gel 0 lb/bbl LOW GEL, fresh": the mud for a readout. */
	public String mudText() {
		return String.format("%.2f ppg (%,.0f kg/m3), gel %.0f lb/bbl%s, %s", mudPpg(), mudDensity() * 1000, gelLbBbl(), lowGel() ? " LOW GEL" : "",
				saltSaturated() ? "salt-saturated" : salt > 1 ? "salty (not saturated)" : "fresh");
	}

	/** kg of this solid the tanks have room for. */
	public double roomFor(Solid solid) {
		double room = Tuning.LAND_RIG_MUD_TANK - mudVolume();
		if (solid == Solid.SALT) {
			double toSaturate = Math.max(0, water * Tuning.SALT_SATURATION_KG_PER_L - salt);
			return Math.max(0, Math.min(toSaturate + 50, room * solid.sg));   // salt past saturation is not wanted: the hopper stops there
		}
		return Math.max(0, room * solid.sg);
	}

	/** The mixing hopper puts {@code kg} of a solid into the tanks; returns how much fitted. */
	public double addSolid(Solid solid, double kg) {
		double took = Math.max(0, Math.min(kg, roomFor(solid)));
		switch (solid) {
			case BARITE -> barite += took;
			case BENTONITE -> bentonite += took;
			case SALT -> salt += took;
		}
		if (took > 0) {
			setChanged();
		}
		return took;
	}

	/** {@code litres} of mud go down the hole: water and solids alike. */
	private void useMud(double litres) {
		double v = mudVolume();
		if (v <= 0) {
			return;
		}
		double keep = Math.max(0, 1 - litres / v);
		water = (int) Math.round(water * keep);
		barite *= keep;
		bentonite *= keep;
		salt *= keep;
	}

	/** Game test hook: mixes this much straight into the tanks, as the hopper would. */
	public void testMix(Solid solid, double kg) {
		addSolid(solid, kg);
	}

	private @Nullable RigDriveHouseBlockEntity driveHouse() {
		return level != null && level.getBlockEntity(LandRig.driveHouse(worldPosition, facing())) instanceof RigDriveHouseBlockEntity house ? house : null;
	}

	/** The power the drive house should draw for what the rig is doing now. */
	public double demand() {
		if (!formed) {
			return 0;
		}
		if (connecting()) {
			return Tuning.LAND_RIG_CONNECTION_WATTS;
		}
		if (phase == Phase.SHUT_IN && mudPpg() + 1e-6 >= killPpg) {
			return Tuning.LAND_RIG_DRILL_WATTS;   // the mud pumps circulating kill mud through the choke
		}
		return pumping() ? Tuning.LAND_RIG_DRILL_WATTS : Tuning.LAND_RIG_IDLE_WATTS;
	}

	public boolean installTopDrive() {
		if (topDrive || !formed) {
			return false;
		}
		topDrive = true;
		setChanged();
		sync();
		return true;
	}

	/** The driller's console: start or stop drilling. */
	public Component toggle() {
		if (!remote.hand()) {
			return Component.literal("The selector is not in HAND: the driller's console does nothing");
		}
		if (phase == Phase.IDLE) {
			phase = Phase.DRILLING;
		} else if (phase == Phase.DRILLING) {
			phase = Phase.IDLE;
		}
		setChanged();
		return inspect();
	}

	/** Water from a pump's pipe into the mud tanks; returns how much fitted. It dilutes the mud's weight. */
	public int addWater(int amount) {
		int took = Math.max(0, Math.min(amount, (int) Math.floor(Tuning.LAND_RIG_MUD_TANK - mudVolume())));
		water += took;
		if (took > 0) {
			setChanged();
		}
		return took;
	}

	public double waterLevel() {
		return water / (double) Tuning.LAND_RIG_MUD_TANK;
	}

	/** Game test hook: empties or fills the mud tanks with water (what was mixed in stays, and is thinned or thickened). */
	public void testWater(int litres) {
		water = litres;
		if (litres <= 0) {
			barite = bentonite = salt = 0;
		}
		setChanged();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, RigSubstructureBlockEntity rig) {
		rig.followPlc(level.getGameTime());
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		if ((level.getGameTime() + pos.hashCode()) % 20 == 0) {
			PartLayout.Found found = LandRig.layout().check(level, pos, rig.facing());
			boolean was = rig.formed;
			rig.formed = found.parts() != null;
			rig.missing = found.missing();
			if (was != rig.formed) {
				rig.setChanged();
				level.sendBlockUpdated(pos, state, state, 3);   // the client draws the cables of a formed rig
				dev.wildcatter.circuit.CircuitManager.markDirty(level);
			}
		}
		RigDriveHouseBlockEntity house = rig.driveHouse();
		double power = house != null && rig.formed ? house.delivered() : 0;
		float rpm = 0;
		if (rig.formed && (rig.phase == Phase.KICK || rig.phase == Phase.SHUT_IN || rig.phase == Phase.BLOWOUT)) {
			rig.wellControl(server, power);
		} else if (!rig.drilling()) {
			rig.status = !rig.formed ? "NOT BUILT: needs the " + rig.missing : !rig.topDrive ? "NO TOP DRIVE: click one into the mast"
					: rig.phase == Phase.COMPLETE ? "WELL COMPLETE: break the rig down to move it" : rig.phase == Phase.DRY ? (rig.status.startsWith("BLOWN") ? rig.status : "DRY HOLE: nothing under it down to bedrock")
					: rig.phase == Phase.KILLED ? "KILLED by a relief well's kill-weight mud"
					: rig.remote.hand() ? "stopped: right-click the driller's console to drill" : rig.remote.auto() ? "stopped: its PLC is not enabling it" : "stopped: selector in OFF";
		} else if (power <= 0.01) {
			rig.status = house != null && house.undervoltage() ? "drives stopped on UNDERVOLTAGE" : "NO POWER at the drive house";
		} else if (rig.connection > 0) {
			rig.connection -= power;
			rig.status = "making a connection: the drawworks hoisting the next stand";
			if (rig.connection <= 0) {
				rig.connection = 0;
				rig.sync();
			}
		} else if (rig.mudVolume() < Tuning.LAND_RIG_MUD_PER_BLOCK) {
			rig.status = "NO MUD: pipe water from a pump to the mud tanks";
		} else if (rig.needsBop(server)) {
			rig.status = "NO BOP: the deep field ahead is geopressured: set a BOP STACK under the rotary table and its ACCUMULATOR within "
					+ Tuning.ACCUMULATOR_REACH + " blocks";
		} else if (rig.washout(server)) {
			rig.status = "WASHOUT: fresh mud dissolves the rock salt and the hole washes out; mix rock salt into the mud until it is salt-saturated ("
					+ Math.round(Tuning.SALT_SATURATION_KG_PER_L * 1000) + " kg a m3 of water)";
		} else {
			rpm = (float) Tuning.TOP_DRIVE_RPM;
			double rate = power * (rig.lowGel() ? Tuning.LOW_GEL_RATE : 1);
			rig.progress += rate;
			rig.status = "drilling at " + Math.round(rate * 100) + "% of full rate" + (rig.lowGel() ? " (LOW GEL: the cuttings settle; mix bentonite to " + Math.round(Tuning.LOW_GEL_KG_PER_L * 350.5) + " lb/bbl)" : "");
			while (rig.progress >= Tuning.LAND_RIG_TICKS_PER_BLOCK && rig.drilling() && rig.connection <= 0) {
				rig.progress -= Tuning.LAND_RIG_TICKS_PER_BLOCK;
				rig.advance(server);
			}
		}
		rig.spin.setRpm(rpm);
		if (rig.spin.needsSync()) {
			rig.spin.markSynced();
			rig.sync();
		}
	}

	/**
	 * True if the next block down is rock salt and the mud is not salt-saturated: the hole washes out. The first time, the metres
	 * already drilled in salt at the bottom of the hole fill back in (docs/multiblocks/drilling-mud.md).
	 */
	private boolean washout(ServerLevel level) {
		if (saltSaturated() || !isSalt(level.getBlockState(hole().below(depth + 1)))) {
			return false;
		}
		if (saltMetres > 0) {
			for (int k = 0; k < saltMetres && depth > 0; k++) {
				level.setBlock(hole().below(depth), dev.wildcatter.ore.OreBlocks.block(dev.wildcatter.ore.Ore.HALITE).defaultBlockState(), 3);
				depth--;
				stand = Math.max(0, stand - 1);
			}
			saltMetres = 0;
			setChanged();
			sync();
		}
		return true;
	}

	/** The next block reaches a deep (geopressured) field and there is no BOP. */
	private boolean needsBop(ServerLevel level) {
		Reservoir field = Drilling.field(level, hole());
		return field != null && field.deep() && hole().getY() - (depth + 1) <= field.topY() && !hasBop();
	}

	private static boolean isSalt(BlockState state) {
		return state.is(dev.wildcatter.ore.OreBlocks.block(dev.wildcatter.ore.Ore.HALITE));
	}

	private void advance(ServerLevel level) {
		BlockPos hole = hole();
		Reservoir field = Drilling.field(level, hole);
		boolean salty = isSalt(level.getBlockState(hole.below(depth + 1)));
		if (!Drilling.caseNext(level, hole, depth)) {
			phase = field == null ? Phase.DRY : Phase.COMPLETE;   // bedrock: with a field above it, the hole has already struck
			if (field == null) {
				level.playSound(null, hole, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.8F, 0.5F);
			}
			setChanged();
			sync();
			return;
		}
		depth++;
		saltMetres = salty ? saltMetres + 1 : 0;
		useMud(Tuning.LAND_RIG_MUD_PER_BLOCK);
		stand++;
		dev.wildcatter.well.RollFront front = Drilling.isrFirst(level, hole) ? Drilling.rollFront(level, hole) : null;
		if (front != null && hole.getY() - depth <= front.bottomY()) {
			phase = Phase.COMPLETE;   // through a roll-front: an ISR well, its gamma log in the wellhead
			String log = Drilling.gammaLog(level, hole, hole.getY() - depth);
			if (level.getBlockState(hole).canBeReplaced()) {
				Drilling.setIsrWell(level, hole, log);
			}
			Drilling.announce(level, hole, Component.literal("ISR WELL completed through a roll-front, " + depth + " m down. " + log));
		} else if (field != null && hole.getY() - depth <= field.topY()) {
			if (field.deep() && mudPpg() < porePpg(field) + Tuning.TRIP_MARGIN_PPG - 1e-6) {
				kick(level);   // underbalanced into a geopressured field
			} else {
				strike(level, field);
			}
		} else if (stand >= Tuning.LAND_RIG_STAND_BLOCKS) {
			stand = 0;
			connection = Tuning.LAND_RIG_CONNECTION_TICKS;
		}
		setChanged();
		sync();
	}

	private void strike(ServerLevel level, Reservoir field) {
		phase = Phase.COMPLETE;
		ReservoirData data = ReservoirData.get(level);
		BlockPos hole = hole();
		if (data.remaining(field) <= 0) {
			phase = Phase.DRY;
			return;
		}
		if (level.getBlockState(hole).canBeReplaced()) {
			Drilling.setWellhead(level, hole, field.kind(), facing());
		}
		if (field.deep() && mudPpg() >= porePpg(field) + Tuning.TRIP_MARGIN_PPG - 1e-6) {
			relieve(level, field);   // a relief well: kill-weight mud into the field kills a wild well on it
		}
		Drilling.announce(level, hole, Component.literal("STRIKE! " + (field.deep() ? "Deep " : "") + (field.kind() == Reservoir.Kind.OIL ? "oil" : "gas") + " at "
				+ depth + " m. The wellhead is set in the cellar under the rig floor: break the rig down to move it."));
	}

	private void sync() {
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	@Override
	public Component inspect() {
		RigDriveHouseBlockEntity house = driveHouse();
		return Component.literal("Land rig (1,000 hp, " + Voltage.MEDIUM.label() + " three-phase): " + status + "; " + depth + " m down, stand "
				+ stand + "/" + Tuning.LAND_RIG_STAND_BLOCKS + ", mud water " + (int) (mudVolume() / 1000) + " of " + Tuning.LAND_RIG_MUD_TANK / 1000 + " m3 at " + mudText() + ", top drive "
				+ (topDrive ? "in" : "MISSING") + bopText() + (house != null && formed ? "; drive house " + house.load().describe() : ""));
	}

	private String bopText() {
		BopStackBlockEntity stack = bop();
		BopAccumulatorBlockEntity acc = accumulator();
		if (stack == null && acc == null) {
			return ", NO BOP";
		}
		return stack == null ? ", NO BOP (accumulator but no stack)" : acc == null ? ", BOP " + (stack.closed() ? "CLOSED" : "open") + " but NO ACCUMULATOR"
				: ", BOP " + (stack.closed() ? "CLOSED" : "open") + " (" + acc.closes() + " closes)";
	}

	// ---- moving parts: the top drive rides down the stand, its quill and the pipe below turning --------------------------------
	@Override
	public List<Rotor> rotors() {
		List<Rotor> out = new ArrayList<>();
		if (!topDrive) {
			return out;
		}
		RotorBlock rotor = (RotorBlock) WildcatterBlocks.ROTOR;
		float head = head();
		// the hole is 4 cells east and south of the main cell, facing north
		Vector3f hole = new Vector3f(4.5F, 0, 4.5F);
		out.add(new Rotor(rotor.part(RotorBlock.Part.TOP_DRIVE), hole, Direction.Axis.Y, 1.0F, (m, deg) -> m.translate(0, head, 0)));
		out.add(new Rotor(rotor.part(RotorBlock.Part.TOP_DRIVE_QUILL), hole, Direction.Axis.Y, 1.0F,
				(m, deg) -> m.translate(0, head, 0).rotateY((float) Math.toRadians(deg))));
		for (int k = 0; k < 16; k++) {   // 2 m pieces of the stand down through the rotary table into the hole
			float down = -2.5F - 2.0F * k;
			out.add(new Rotor(rotor.part(RotorBlock.Part.RIG_PIPE), hole, Direction.Axis.Y, 1.0F,
					(m, deg) -> m.translate(0, head + down, 0).rotateY((float) Math.toRadians(deg))));
		}
		return out;
	}

	/** Client side: the top drive's height, easing toward where the stand puts it (back to the top in a connection). */
	private float head() {
		float target = connection > 0 || phase != Phase.DRILLING ? TOP_DRIVE_TOP : TOP_DRIVE_TOP - stand;
		long now = System.nanoTime();
		if (Float.isNaN(shownHead)) {
			shownHead = target;
		} else {
			float step = Math.min(0.25F, (now - shownAt) / 1e9F) * (target > shownHead ? 2.0F : 8.0F);
			shownHead = Math.abs(target - shownHead) <= step ? target : shownHead + Math.signum(target - shownHead) * step;
		}
		shownAt = now;
		return shownHead;
	}

	@Override
	public Spin spin() {
		return spin;
	}

	/** 120 rpm: shown at its real speed. */
	@Override
	public float displayDivisor() {
		return 1.0F;
	}

	// ---- saving, and the top drive dropping with the substructure ------------------------------------------------------------
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level != null && topDrive) {
			Containers.dropItemStack(level, pos.getX(), pos.getY() + 6, pos.getZ(), new ItemStack(WildcatterItems.TOP_DRIVE));
		}
	}

	@Override
	public boolean grown() {
		return grown;
	}

	@Override
	public void markGrown() {
		grown = true;
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		remote.save(output);
		output.putInt("phase", phase.ordinal());
		output.putBoolean("topDrive", topDrive);
		output.putInt("water", water);
		output.putDouble("barite", barite);
		output.putDouble("bentonite", bentonite);
		output.putDouble("salt", salt);
		output.putInt("saltMetres", saltMetres);
		output.putLong("kickAt", kickAt);
		output.putDouble("pitGain", pitGain);
		output.putDouble("killPpg", killPpg);
		output.putDouble("sidpp", sidpp);
		output.putDouble("killing", killing);
		output.putBoolean("onFire", onFire);
		output.putString("status", status);
		output.putInt("depth", depth);
		output.putInt("stand", stand);
		output.putDouble("progress", progress);
		output.putDouble("connection", connection);
		output.putBoolean("grown", grown);
		output.putFloat("rpm", spin.rpm());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		remote.load(input);
		phase = Phase.values()[Math.max(0, Math.min(Phase.values().length - 1, input.getIntOr("phase", 0)))];
		topDrive = input.getBooleanOr("topDrive", false);
		water = input.getIntOr("water", 0);
		barite = input.getDoubleOr("barite", 0);
		bentonite = input.getDoubleOr("bentonite", 0);
		salt = input.getDoubleOr("salt", 0);
		saltMetres = input.getIntOr("saltMetres", 0);
		kickAt = input.getLongOr("kickAt", 0);
		pitGain = input.getDoubleOr("pitGain", 0);
		killPpg = input.getDoubleOr("killPpg", 0);
		sidpp = input.getDoubleOr("sidpp", 0);
		killing = input.getDoubleOr("killing", 0);
		onFire = input.getBooleanOr("onFire", false);
		status = input.getStringOr("status", "");
		depth = input.getIntOr("depth", 0);
		stand = input.getIntOr("stand", 0);
		progress = input.getDoubleOr("progress", 0);
		connection = input.getDoubleOr("connection", 0);
		grown = input.getBooleanOr("grown", grown);
		formed = input.getBooleanOr("formed", formed);   // sent to the client for the cables; the server finds it again on its next check
		spin.setRpm(input.getFloatOr("rpm", 0));
	}

	@Override
	public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
		net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
		tag.putInt("phase", phase.ordinal());
		tag.putBoolean("topDrive", topDrive);
		tag.putInt("stand", stand);
		tag.putDouble("connection", connection);
		tag.putFloat("rpm", spin.rpm());
		tag.putBoolean("formed", formed);
		return tag;
	}
}
