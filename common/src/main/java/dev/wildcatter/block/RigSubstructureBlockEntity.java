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
			case 5 -> (phase == Phase.KICK && kickShown) || phase == Phase.BLOWOUT || phase == Phase.UNDERGROUND;
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
	public java.util.Map<BlockPos, String> hoaPositions() {
		return java.util.Map.of(worldPosition, remote.hoa().name());
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
		KICK, SHUT_IN, BLOWOUT, KILLED,
		/** 23a (docs/multiblocks/oilfield-follow-ons.md): running and cementing intermediate casing; a kick flowing into a broken-down shoe. */
		CASING, UNDERGROUND
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

	/** The mud is under the field's pore pressure and the trip margin: the well kicks. In working oil mud the gas dissolves and the alarm comes late. */
	private void kick(ServerLevel level) {
		phase = Phase.KICK;
		kickAt = level.getGameTime();
		pitGain = 0;
		influxUp = 0;
		kickShown = false;
		if (!oilMudWorking()) {
			alarm(level);
		}
		setChanged();
		sync();
	}

	/** The pit volume alarm: the 30 s to shut in start now. */
	private void alarm(ServerLevel level) {
		kickShown = true;
		kickAt = level.getGameTime();
		level.playSound(null, hole(), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0F, 0.6F);
		Drilling.announce(level, hole(), Component.literal("KICK! The pits are gaining: shut the well in (sneak-click the driller's console) within "
				+ Tuning.KICK_WINDOW_TICKS / 20 + " s"));
	}

	/** m3/s the kick flows: with {@code chokeKill}, with the underbalance (84 psi gives the Phase 15 rate); else the Phase 15 rate. */
	private double kickRate(@Nullable Reservoir field) {
		if (!dev.wildcatter.Rules.chokeKill() || field == null) {
			return Tuning.KICK_GAIN_M3S;
		}
		double under = dev.wildcatter.well.WellControl.psi(porePpg(field) - mudPpg(), tvdFeet());
		return Math.max(0.002, Math.min(Tuning.KICK_GAIN_M3S, Tuning.KICK_INFLUX_M3S_PER_PSI * under));
	}

	/** The pit gain the pit volume totalizer shows: in oil mud a third of it until the influx is within 20 blocks of the surface (question 8). */
	public double shownGain() {
		if (oilMudWorking() && depth - influxUp > Tuning.OIL_MUD_BREAKOUT_M) {
			return pitGain * Tuning.OIL_MUD_KICK_SHOWN;
		}
		return pitGain;
	}

	// ---- 23a: casing, the leak-off test, the fracture gradient and losses (docs/multiblocks/oilfield-follow-ons.md) -------------------
	/** Metres down of the intermediate casing shoe; 0 with only the surface casing. */
	private int shoeDepth;
	private double shoeFgPpg = Tuning.FG_OPEN_HOLE_PPG;
	private boolean lotDone;
	/** ppg a cured loss has added to the weakest point (wellbore strengthening). */
	private double lcmBonus;
	private double lcmCure;
	private double lcmPumped;
	private double casingLeft;
	private dev.wildcatter.well.WellControl.Losses losses = dev.wildcatter.well.WellControl.Losses.NONE;
	private double lostMud;
	private int columnDrop;
	private double fallTicks;
	private boolean kickShown = true;
	private double influxUp;
	/** The racks: casing joints (slot 0) and cement bags (slot 1), by click or a hopper on the floor. */
	private final net.minecraft.core.NonNullList<ItemStack> rack = net.minecraft.core.NonNullList.withSize(2, ItemStack.EMPTY);
	/** 23b: the well in a choke kill. */
	private dev.wildcatter.well.@Nullable KillModel well;

	public int shoeDepth() {
		return shoeDepth;
	}

	public int depthNow() {
		return depth;
	}

	public boolean lotDone() {
		return lotDone;
	}

	public dev.wildcatter.well.WellControl.Losses losses() {
		return losses;
	}

	public double lostMud() {
		return lostMud;
	}

	/** The depth of the weakest point: the intermediate shoe, or the surface casing's. */
	public int weakDepth() {
		return shoeDepth > 0 ? shoeDepth : Math.min(depth, Tuning.SURFACE_CASING_M);
	}

	/** The weakest point's fracture gradient, ppg: the intermediate shoe's, or the open hole's under surface casing. */
	public double weakFg() {
		return (shoeDepth > 0 ? shoeFgPpg : Tuning.FG_OPEN_HOLE_PPG) + lcmBonus;
	}

	/** True with open hole below the weakest point (drilled past the surface casing or the intermediate shoe). */
	private boolean openHole() {
		return depth > weakDepth();
	}

	/** MAASP for the mud in the hole, psi. */
	public double maasp() {
		return dev.wildcatter.well.WellControl.maasp(weakFg(), mudPpg(), weakDepth() * Tuning.FEET_PER_M);
	}

	public int jointsNeeded() {
		return (depth + Tuning.CASING_JOINT_M - 1) / Tuning.CASING_JOINT_M;
	}

	public int joints() {
		return rack.get(0).getCount();
	}

	public int cementBags() {
		return rack.get(1).getCount();
	}

	/** Racks a stack of casing joints or cement bags held or dropped in by hopper; returns how many it took. */
	public int rackUp(ItemStack stack) {
		int slot = stack.is(WildcatterItems.CASING_JOINT) ? 0 : stack.is(WildcatterItems.OILWELL_CEMENT) ? 1 : -1;
		if (slot < 0) {
			return 0;
		}
		ItemStack held = rack.get(slot);
		int room = held.isEmpty() ? 64 : 64 - held.getCount();
		int n = Math.min(room, stack.getCount());
		if (n <= 0) {
			return 0;
		}
		if (held.isEmpty()) {
			rack.set(slot, stack.copyWithCount(n));
		} else {
			held.grow(n);
		}
		setChanged();
		return n;
	}

	/** The racks, for a hopper on the rig floor. */
	public net.minecraft.world.WorldlyContainer rackContainer() {
		return new net.minecraft.world.WorldlyContainer() {
			@Override
			public int[] getSlotsForFace(Direction side) {
				return new int[] {0, 1};
			}

			@Override
			public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
				return canPlaceItem(slot, stack);
			}

			@Override
			public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
				return false;
			}

			@Override
			public boolean canPlaceItem(int slot, ItemStack stack) {
				return slot == 0 ? stack.is(WildcatterItems.CASING_JOINT) : slot == 1 && stack.is(WildcatterItems.OILWELL_CEMENT);
			}

			@Override
			public int getContainerSize() {
				return 2;
			}

			@Override
			public boolean isEmpty() {
				return rack.stream().allMatch(ItemStack::isEmpty);
			}

			@Override
			public ItemStack getItem(int slot) {
				return rack.get(slot);
			}

			@Override
			public ItemStack removeItem(int slot, int count) {
				return ItemStack.EMPTY;
			}

			@Override
			public ItemStack removeItemNoUpdate(int slot) {
				return ItemStack.EMPTY;
			}

			@Override
			public void setItem(int slot, ItemStack stack) {
				rack.set(slot, stack);
				RigSubstructureBlockEntity.this.setChanged();
			}

			@Override
			public void setChanged() {
				RigSubstructureBlockEntity.this.setChanged();
			}

			@Override
			public boolean stillValid(net.minecraft.world.entity.player.Player player) {
				return false;
			}

			@Override
			public void clearContent() {
			}
		};
	}

	/**
	 * The console's second button (sneak-click) when no kick is on: SET CASING with no intermediate string yet, then the LOT after 3 m of
	 * new hole. Returns what happened.
	 */
	public Component consoleCasing(ServerLevel level) {
		if (phase != Phase.IDLE) {
			return Component.literal(phase == Phase.CASING ? status : "Stop drilling first (right-click the console): " + status);
		}
		if (shoeDepth == 0) {
			return setCasing(level);
		}
		if (!lotDone) {
			return leakOffTest(level);
		}
		return Component.literal(String.format("Intermediate casing at %d m, shoe FG %.1f ppg, MAASP %,.0f psi with %.1f ppg mud: one string is all this rig runs",
				shoeDepth, weakFg(), maasp(), mudPpg()));
	}

	/** SET CASING: runs casing from the surface to the bit and cements it; the bit's depth becomes the shoe. */
	public Component setCasing(ServerLevel level) {
		if (!formed || depth <= Tuning.SURFACE_CASING_M) {
			return Component.literal("SET CASING: drill below the surface casing (" + Tuning.SURFACE_CASING_M + " m) first");
		}
		int need = jointsNeeded();
		if (joints() < need || cementBags() < Tuning.CASING_CEMENT_BAGS) {
			return Component.literal(String.format("SET CASING needs %d casing joints and %d bags of oilwell cement on the rack (has %d and %d): click them into the substructure or hopper them onto the floor",
					need, Tuning.CASING_CEMENT_BAGS, joints(), cementBags()));
		}
		rack.get(0).shrink(need);
		rack.get(1).shrink(Tuning.CASING_CEMENT_BAGS);
		casingLeft = need * (double) Tuning.CASING_TICKS_PER_JOINT + Tuning.CASING_WOC_TICKS;
		phase = Phase.CASING;
		status = "RUNNING CASING";
		setChanged();
		sync();
		return Component.literal(String.format("SET CASING: running %d joints of 9-5/8 in casing to %d m, then %d minutes waiting on cement", need, depth, Tuning.CASING_WOC_TICKS / 1200));
	}

	/** The casing run and the wait on cement, a tick of the drawworks' power at a time. */
	private void runCasing(ServerLevel level, double power) {
		casingLeft -= power;
		int joints = jointsNeeded();
		double running = casingLeft - Tuning.CASING_WOC_TICKS;
		status = running > 0 ? String.format("RUNNING CASING: joint %d of %d", joints - (int) Math.ceil(running / Tuning.CASING_TICKS_PER_JOINT) + 1, joints)
				: String.format("WAITING ON CEMENT: %d s left", (int) Math.ceil(Math.max(0, casingLeft) / 20));
		if (casingLeft <= 0) {
			casingLeft = 0;
			shoeDepth = depth;
			Reservoir field = Drilling.field(level, hole());
			shoeFgPpg = dev.wildcatter.well.WellControl.shoeFg(hole().getY() - depth, field != null && field.deep() ? field.topY() : Integer.MIN_VALUE);
			lotDone = false;
			lcmBonus = 0;
			phase = Phase.IDLE;
			dev.wildcatter.quest.QuestEvents.milestone(level, hole(), dev.wildcatter.quest.Milestones.CASING_SET);
			Drilling.announce(level, hole(), Component.literal("CASING SET at " + depth + " m and cemented: drill " + Tuning.LOT_NEW_HOLE_M
					+ " m of new hole, then run a leak-off test (sneak-click the console). FG NOT TESTED until then."));
			sync();
		}
		setChanged();
	}

	/** The LOT: pumps slowly against the closed annular and reads where the shoe takes mud, so its FG and the MAASP show. */
	public Component leakOffTest(ServerLevel level) {
		if (depth < shoeDepth + Tuning.LOT_NEW_HOLE_M) {
			return Component.literal("LOT: drill " + (shoeDepth + Tuning.LOT_NEW_HOLE_M - depth) + " m more of new hole below the shoe first");
		}
		lotDone = true;
		double feet = shoeDepth * Tuning.FEET_PER_M;
		double leakOff = dev.wildcatter.well.WellControl.maasp(weakFg(), mudPpg(), feet);
		dev.wildcatter.quest.QuestEvents.milestone(level, hole(), dev.wildcatter.quest.Milestones.LOT_DONE);
		setChanged();
		return Component.literal(String.format("LOT: leak-off at %,.0f psi on the annular with %.2f ppg mud: the shoe at %d m holds %.1f ppg; MAASP %,.0f psi",
				leakOff, mudPpg(), shoeDepth, weakFg(), leakOff));
	}

	/** The equivalent mud weight at the weakest point: the mud, plus the annular friction while pumping. */
	private double shoeEmw(boolean pumpingNow) {
		return mudPpg() + (pumpingNow ? Tuning.ECD_DRILLING_PPG : 0);
	}

	/**
	 * Each tick it pumps (by the rule {@code chokeKill}): the weakest point against its FG. Partial losses take mud and drilling goes on;
	 * total losses stop the rig until the weight comes down or the hole is cased. Returns true while total.
	 */
	private boolean lossCheck(ServerLevel level) {
		if (!dev.wildcatter.Rules.chokeKill() || !openHole()) {
			losses = dev.wildcatter.well.WellControl.Losses.NONE;
			return false;
		}
		double fg = weakFg();
		dev.wildcatter.well.WellControl.Losses now = dev.wildcatter.well.WellControl.losses(shoeEmw(true), fg);
		if (losses == dev.wildcatter.well.WellControl.Losses.TOTAL && now == dev.wildcatter.well.WellControl.Losses.TOTAL) {
			fallTicks += 1;
			if (fallTicks >= Tuning.LOSS_TOTAL_FALL_TICKS) {
				fallTicks = 0;
				columnDrop = Math.min(depth, columnDrop + 1);
			}
			return true;
		}
		if (losses == dev.wildcatter.well.WellControl.Losses.TOTAL) {
			useMud(columnDrop * Tuning.ANNULUS_L_PER_M);   // the hole filled again from the tanks
			lostMud += columnDrop * Tuning.ANNULUS_L_PER_M / 1000;
			columnDrop = 0;
		}
		if (now == dev.wildcatter.well.WellControl.Losses.TOTAL && losses != now) {
			Drilling.announce(level, hole(), Component.literal(String.format("LOST CIRCULATION: TOTAL at the shoe (%.1f ppg against %.1f): no returns. Bring the weight down or case the hole.", shoeEmw(true), fg)));
		}
		losses = now;
		if (now == dev.wildcatter.well.WellControl.Losses.PARTIAL) {
			double litres = dev.wildcatter.well.WellControl.partialLossRate(shoeEmw(true), fg) * 1000 / 20;
			useMud(litres);
			lostMud += litres / 1000;
			if (lcmCure > 0) {
				lcmCure -= 1;
				if (lcmCure <= 0) {
					lcmBonus = Tuning.LCM_STRENGTHENING_PPG;   // the pill seals the fractures: the shoe holds 0.5 ppg more
					losses = dev.wildcatter.well.WellControl.Losses.NONE;
					Drilling.announce(level, hole(), Component.literal(String.format("LCM: the losses have stopped; the weakest point now holds %.1f ppg", weakFg())));
				}
			}
		}
		return now == dev.wildcatter.well.WellControl.Losses.TOTAL;
	}

	/** The mud hopper has mixed {@code kg} of LCM in: during partial losses a tonne cures them within a minute. Returns what it took. */
	public double pumpLcm(double kg) {
		lcmPumped += kg;
		if (lcmPumped >= Tuning.BULK_BAG_KG - 1e-6) {
			lcmPumped -= Tuning.BULK_BAG_KG;
			if (losses == dev.wildcatter.well.WellControl.Losses.PARTIAL && lcmCure <= 0) {
				lcmCure = Tuning.LCM_CURE_TICKS;
			}
		}
		setChanged();
		return kg;   // what is not in a loss zone is screened out at the shakers
	}

	/** The losses for a readout: "", or "LOST CIRCULATION: PARTIAL (...)". */
	private String lossText() {
		return switch (losses) {
			case NONE -> "";
			case PARTIAL -> String.format("LOST CIRCULATION: PARTIAL (%.1f ppg at the shoe against %.1f; pits falling%s): ", shoeEmw(true), weakFg(), lcmCure > 0 ? ", LCM curing" : "; an LCM bag through the hopper cures it");
			case TOTAL -> String.format("LOST CIRCULATION: TOTAL (%.1f ppg at the shoe against %.1f): no returns, the fluid level %d m down; bring the weight down (water or base oil) or case the hole",
					shoeEmw(true), weakFg(), columnDrop);
		};
	}

	/** Losses and a kick at once: the field flows into the broken-down shoe, nothing at the surface. A relief well kills it. */
	private void underground(ServerLevel level) {
		phase = Phase.UNDERGROUND;
		BLOWING.add(this);
		Drilling.announce(level, hole(), Component.literal("UNDERGROUND BLOWOUT: the kick is flowing into the broken-down shoe. Nothing shows at the surface; drill a relief well."));
		setChanged();
		sync();
	}

	// ---- 23b: the choke manifold and the hand kill (docs/multiblocks/oilfield-follow-ons.md) ------------------------------------------
	private @Nullable BlockPos manifoldAt, separatorAt, panelAt;
	private boolean flare;
	private long systemAt = -1000, panelSeen = -1000;
	/** The driller's method: 1 in its first circulation (old mud, ICP held), 2 in the second; 0 for wait and weight. */
	private int drillers;
	private double secondFrom;
	private double influxFrom;
	/** The mud weight the kill sheet was worked out for (the string's, after a second kick). */
	private double sheetOmw;
	private double gasVented;
	private String killNote = "";
	/** Game test hook: model steps a tick (a faster kill). */
	private int killSteps = 1;

	/** The remote choke panel on the floor tells the rig it is there, every interval. */
	void panelHere(BlockPos at, long now) {
		panelAt = at;
		panelSeen = now;
	}

	public @Nullable ChokePanelBlockEntity panel() {
		return level != null && panelAt != null && level.getGameTime() - panelSeen < 40
				&& dev.wildcatter.multiblock.BigParts.entity(level, panelAt) instanceof ChokePanelBlockEntity p ? p : null;
	}

	/** Finds the choke manifold piped from the BOP stack's choke line within 12 blocks, the separator against it and a flare on its vent. */
	private void findChokeSystem(ServerLevel level) {
		if (level.getGameTime() - systemAt < 20) {
			return;
		}
		systemAt = level.getGameTime();
		manifoldAt = separatorAt = null;
		flare = false;
		BopStackBlockEntity stack = bop();
		if (stack == null) {
			return;
		}
		java.util.List<Lines.Face> faces = Lines.faces(dev.wildcatter.multiblock.BigParts.cells(stack.getBlockPos(), stack.getBlockState()));
		for (Lines.Line line : Lines.lines(level, faces)) {
			for (BlockPos pipe : line.pipes()) {
				for (Direction side : Direction.values()) {
					if (manifoldAt == null && dev.wildcatter.multiblock.BigParts.entity(level, pipe.relative(side)) instanceof ChokeManifoldBlockEntity m
							&& m.getBlockPos().closerThan(worldPosition, Tuning.CHOKE_REACH + 9)) {
						manifoldAt = m.getBlockPos();
					}
				}
			}
		}
		if (manifoldAt == null || !(level.getBlockEntity(manifoldAt) instanceof ChokeManifoldBlockEntity manifold)) {
			return;
		}
		MudGasSeparatorBlockEntity sep = Touching.find(level, manifoldAt, MudGasSeparatorBlockEntity.class, s -> true);
		if (sep != null) {
			separatorAt = sep.getBlockPos();
			flare = sep.ventsToFlare(level);
		}
		manifold.rigHere(worldPosition, level.getGameTime());
	}

	public boolean hasChoke() {
		return manifoldAt != null;
	}

	public boolean hasSeparator() {
		return separatorAt != null;
	}

	/** With {@code chokeKill}, the next block reaches a deep field and there is no choke manifold on the BOP's choke line. */
	private boolean needsChoke(ServerLevel level) {
		if (!dev.wildcatter.Rules.chokeKill()) {
			return false;
		}
		Reservoir field = Drilling.field(level, hole());
		if (field == null || !field.deep() || hole().getY() - (depth + 1) > field.topY()) {
			return false;
		}
		findChokeSystem(level);
		return manifoldAt == null;
	}

	public dev.wildcatter.well.@Nullable KillModel well() {
		return well;
	}

	/** The kill sheet worked out at shut-in, or null with no kill under way. */
	public dev.wildcatter.well.WellControl.@Nullable KillSheet killSheet() {
		if (well == null) {
			return null;
		}
		dev.wildcatter.well.WellControl.KillSheet sheet = dev.wildcatter.well.WellControl.killSheet(sidpp, sicp, sheetOmw > 0 ? sheetOmw : well.omw, tvdFeet(), well.shoeM * Tuning.FEET_PER_M, well.fgPpg);
		if (sheetOmw > 0) {
			// after a second kick the kill weight is the first sheet's unless the new SIDPP asks for more (no second trip margin)
			double kmw = Math.max(killPpg, sheet.kmwExact());
			sheet = new dev.wildcatter.well.WellControl.KillSheet(sheet.sidpp(), sheet.sicp(), sheet.omw(), sheet.kmwExact(), kmw, sheet.scrPsi(), sheet.icp(),
					sheet.scrPsi() * kmw / sheet.omw(), sheet.strokesToBit(), sheet.strokesBitToShoe(), sheet.strokesShoeToSurface(), sheet.maasp());
		}
		return sheet;
	}

	/** The drill pipe pressure the schedule asks for now: the kill sheet's, or ICP held through the driller's first circulation. */
	public double schedulePsi() {
		dev.wildcatter.well.WellControl.KillSheet sheet = killSheet();
		if (sheet == null) {
			return 0;
		}
		if (drillers == 1) {
			return sheet.icp();
		}
		return sheet.schedule(well.strokes() - (drillers == 2 ? secondFrom : 0));
	}

	/** The kill weight the tanks must reach, ppg. */
	public double killWeight() {
		return killPpg;
	}

	public int drillersMethod() {
		return drillers;
	}

	/** The panel's method switch: wait and weight, or the driller's method (before the pump starts). */
	public String toggleMethod() {
		if (well == null || well.strokes() > 1) {
			return "The method is set before the kill starts";
		}
		drillers = drillers == 0 ? 1 : 0;
		setChanged();
		return drillers == 0 ? "WAIT AND WEIGHT: weight the tanks to the kill weight, then bring the pump up" : "DRILLER'S METHOD: first circulation on the old mud, holding ICP";
	}

	public double gasVented() {
		return gasVented;
	}

	public String killNote() {
		return killNote;
	}

	public void testKillSteps(int steps) {
		killSteps = Math.max(1, steps);
	}

	/** Each tick of a shut-in well under {@code chokeKill}: the panel's pump and choke worked against the well model. */
	private void chokeKill(ServerLevel level, double power) {
		findChokeSystem(level);
		ChokePanelBlockEntity panel = panel();
		if (manifoldAt == null) {
			status = status() + ": NO CHOKE: pipe a choke manifold to the BOP stack's choke line within " + Tuning.CHOKE_REACH + " blocks";
			return;
		}
		if (panel == null) {
			status = status() + ": NO CHOKE PANEL: set a remote choke panel on the rig floor";
			return;
		}
		dev.wildcatter.well.WellControl.KillSheet sheet = killSheet();
		for (int k = 0; k < killSteps && phase == Phase.SHUT_IN; k++) {
			double target = panel.pumpOn() && power > 0.01 && mudVolume() > 1000 ? Tuning.SCR_SPM : 0;
			double spm = well.spm();
			spm = target > spm ? Math.min(target, spm + Tuning.PUMP_SPM_PER_S / 20) : Math.max(target, spm - Tuning.PUMP_SPM_PER_S * 2 / 20);
			double rate = (panel.powered() ? Tuning.HYDRAULIC_CHOKE_PER_S : Tuning.MANUAL_CHOKE_PER_S) / 20;
			double before = well.choke();
			double want = panel.chokeTarget(level.getGameTime(), before);
			double choke = Math.max(before - rate, Math.min(before + rate, want));
			well.step(0.05, mudPpg(), spm, choke);
			double opening = (choke - before) * 20;
			// the mud lost at the shoe
			dev.wildcatter.well.WellControl.Losses now = dev.wildcatter.well.WellControl.losses(well.shoeEmw(), weakFg());
			if (well.lossLps() > 0) {
				useMud(well.lossLps() / 20);
				lostMud += well.lossLps() / 20000;
			}
			if (now != losses && now != dev.wildcatter.well.WellControl.Losses.NONE) {
				Drilling.announce(level, hole(), Component.literal(String.format("LOST CIRCULATION at the shoe in the kill: %.1f ppg against %.1f. Open the choke.", well.shoeEmw(), weakFg())));
			}
			losses = now;
			if (now == dev.wildcatter.well.WellControl.Losses.TOTAL && well.bhpPsi() < well.porePsi()) {   // losses and a kick at once
				underground(level);
				return;
			}
			// gas at the surface: through the separator to the flare, or on the floor
			if (well.ventedL() > 0) {
				gasVented += well.ventedL() / 1000;
				boolean blown = separatorAt == null || (well.casingPsi() > Tuning.SEPARATOR_SEAL_PSI && opening > Tuning.SEPARATOR_FAST_OPEN_PER_S);
				if (blown) {
					gasOnFloor(level);
					if (phase != Phase.SHUT_IN) {
						return;
					}
				} else if (separatorAt != null && level.getBlockEntity(separatorAt) instanceof MudGasSeparatorBlockEntity sep) {
					sep.gasThrough(level, well.ventedL() / 1000);
				}
			}
			// too little back pressure: a second kick
			if (well.influxL() - influxFrom > Tuning.SECOND_KICK_M3 * 1000) {
				secondKick(level, panel);
				return;
			}
			// the driller's method: the first circulation ends once the gas is out and a circulation pumped
			if (drillers == 1 && sheet != null && !well.gasInHole() && well.strokes() >= sheet.strokesCirculation() && mudPpg() + 1e-6 >= killPpg) {
				drillers = 2;
				secondFrom = well.strokes();
			}
			// killed: kill mud all round, the choke open, the pump stopped and both gauges at zero, then a minute's flow check
			boolean killMudRound = well.lightestPpg() + 0.05 >= killPpg && !well.gasInHole();
			if (killMudRound && well.spm() < 1e-6 && choke >= 0.95 && well.casingPsi() < 1.5 && well.dppNow() < 1.5) {
				flowCheck++;
				if (well.influxL() > influxFrom + 1) {
					flowCheck = 0;
					influxFrom = well.influxL();
				}
				if (flowCheck >= Tuning.FLOW_CHECK_TICKS) {
					wellDead(level);
					return;
				}
			} else {
				flowCheck = 0;
			}
		}
		pitGain = well.gasL() / 1000;
		killNote = flowCheck > 0 ? String.format("FLOW CHECK: %d s", (Tuning.FLOW_CHECK_TICKS - flowCheck) / 20)
				: well.spm() < 1e-6 && well.strokes() < 1 ? "bring the pump up to SCR on the panel, opening the choke to hold casing pressure at SICP"
				: killMudRound() ? "kill mud all round: stop the pump, open the choke full, flow check" : "hold drill pipe pressure on the schedule with the choke";
		status = String.format("KILL%s: pump %.0f spm, choke %.0f %%, DPP %,.0f psi (schedule %,.0f), casing %,.0f psi, %,.0f strokes; %s%s",
				drillers == 1 ? " (driller's 1st circulation)" : drillers == 2 ? " (driller's 2nd)" : "", well.spm(), well.choke() * 100, well.dppGauge(), schedulePsi(),
				well.casingPsi(), well.strokes(), killNote, losses != dev.wildcatter.well.WellControl.Losses.NONE ? "; LOST CIRCULATION at the shoe" : "");
	}

	private boolean killMudRound() {
		return well != null && well.lightestPpg() + 0.05 >= killPpg && !well.gasInHole();
	}

	/** Too little back pressure let more influx in: the schedule no longer holds; shut in and start again. */
	private void secondKick(ServerLevel level, ChokePanelBlockEntity panel) {
		panel.stopForSecondKick();
		pitGain = well.gasL() / 1000;
		well.shutInAgain();   // the choke closed and the pump stopped: the new shut-in pressures, the stroke counter from zero
		double[] shut = well.shutInPressures();
		sidpp = shut[0];
		sicp = shut[1];
		sheetOmw = well.stringPpg();
		influxFrom = well.influxL();
		drillers = drillers == 0 ? 0 : 1;
		flowCheck = 0;
		dev.wildcatter.well.WellControl.KillSheet sheet = killSheet();
		if (sheet != null) {
			killPpg = sheet.kmw();
		}
		Drilling.announce(level, hole(), Component.literal(String.format("SECOND KICK: the choke was held too far open and the well took more influx. Shut in: SIDPP %,.0f psi, SICP %,.0f psi. Start the kill again.", sidpp, sicp)));
		setChanged();
	}

	/** Gas on the rig floor: no separator, or its seal blown by a fast opening at high casing pressure. A fire within 16 blocks lights it. */
	private void gasOnFloor(ServerLevel level) {
		killNote = "GAS ON THE FLOOR";
		BlockPos h = hole();
		if (level.getGameTime() % 10 == 0) {
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, h.getX() + 0.5, h.getY() + 7, h.getZ() + 0.5, 8, 0.8, 0.5, 0.8, 0.02);
			Drilling.announce(level, h, Component.literal(separatorAt == null ? "GAS ON THE FLOOR: no mud-gas separator against the choke manifold"
					: "GAS ON THE FLOOR: the separator's liquid seal blew on a fast choke opening"));
		}
		if (fireNear(level, h)) {
			ignite(level, h);
			phase = Phase.BLOWOUT;
			BLOWING.add(this);
			sync();
		}
	}

	/** The kill is done: the BOP opens and the rig drills on into the field, as a strike. */
	private void wellDead(ServerLevel level) {
		BopStackBlockEntity stack = bop();
		if (stack != null) {
			stack.setClosed(false);
		}
		ChokePanelBlockEntity panel = panel();
		if (panel != null) {
			panel.stopForSecondKick();
		}
		well = null;
		losses = dev.wildcatter.well.WellControl.Losses.NONE;
		Drilling.announce(level, hole(), Component.literal("WELL DEAD: the flow check holds. The BOP opens."));
		dev.wildcatter.quest.QuestEvents.milestone(level, hole(), dev.wildcatter.quest.Milestones.WELL_KILLED_CHOKE);
		phase = Phase.DRILLING;
		Reservoir field = Drilling.field(level, hole());
		if (field != null) {
			strike(level, field);
		}
		setChanged();
		sync();
	}

	/** The casing for a readout. */
	private String casingText() {
		if (shoeDepth == 0) {
			return depth > Tuning.SURFACE_CASING_M ? String.format(", surface casing at %d m (open hole FG %.1f ppg)", Tuning.SURFACE_CASING_M, weakFg()) : "";
		}
		return lotDone ? String.format(", shoe at %d m: FG %.1f ppg, MAASP %,.0f psi", shoeDepth, weakFg(), maasp()) : String.format(", shoe at %d m: FG NOT TESTED", shoeDepth);
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
		if (dev.wildcatter.Rules.chokeKill()) {
			// 23a-23b: the well as a column of mud with the influx at the bottom; the shoe sees the shut-in pressure
			well = new dev.wildcatter.well.KillModel(Math.max(1, depth), Math.max(1, Math.min(depth, weakDepth())), weakFg(), pore, mudPpg(), Math.min(pitGain, 3.0));
			double[] shut = well.shutInPressures();
			sidpp = shut[0];
			sicp = shut[1];
			flowCheck = 0;
			influxFrom = 0;
			sheetOmw = 0;
			drillers = 0;
			secondFrom = 0;
			gasVented = 0;
			if (dev.wildcatter.well.WellControl.losses(well.shoeEmw(), weakFg()) == dev.wildcatter.well.WellControl.Losses.TOTAL) {
				stack.setClosed(true);
				underground(level);
				return Component.literal(String.format("SHUT IN, but the shoe breaks down: %.1f ppg at %d m against %.1f. UNDERGROUND BLOWOUT.", well.shoeEmw(), weakDepth(), weakFg()));
			}
		}
		setChanged();
		sync();
		return Component.literal("SHUT IN: annular and rams closed. " + status());
	}

	private double sicp;
	private int flowCheck;

	private String status() {
		if (well != null) {
			return String.format("SHUT IN: SIDPP %,.0f psi, SICP %,.0f psi, kill weight %.1f ppg (mud %.2f ppg, %.2f m3 pit gain), MAASP %,.0f psi", sidpp, sicp, killPpg, mudPpg(), shownGain(), maasp());
		}
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
				pitGain += kickRate(field) / 20;
				influxUp = Math.min(depth, influxUp + Tuning.KICK_RISE_M_PER_S / 20);
				if (!kickShown) {
					// in oil mud the gas is in solution: the totalizer shows a third, until it breaks out near the surface
					status = String.format("drilling (pit volume %+.2f m3)", shownGain());
					if (shownGain() >= Tuning.KICK_ALARM_M3) {
						alarm(level);
					}
					break;
				}
				long left = Tuning.KICK_WINDOW_TICKS - (now - kickAt);
				status = String.format("KICK: PIT GAIN %.1f m3: shut in (sneak-click the driller's console) within %d s", shownGain(), Math.max(0, left / 20));
				if (now % 40 == 0) {
					level.playSound(null, hole(), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0F, 0.6F);
				}
				if (left <= 0) {
					blowout(level);
				}
			}
			case SHUT_IN -> {
				if (well != null && dev.wildcatter.Rules.chokeKill()) {
					chokeKill(level, power);   // 23b: the kill through the choke, by hand or by a PLC
					break;
				}
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
			case UNDERGROUND -> {
				status = "UNDERGROUND BLOWOUT: the field is flowing into the broken-down shoe; kill it with a relief well";
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
						status = "BLOWN DOWN underground: the field is empty";
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
			if (wild != this && wild.level == level && !wild.isRemoved() && (wild.phase == Phase.BLOWOUT || wild.phase == Phase.UNDERGROUND)
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

	/** Game test hook: the casing run and the wait on cement as if done. */
	public void testCasingDone() {
		casingLeft = Math.min(casingLeft, 1);
	}

	/** Game test hook: circulate off bottom: the pumps run (losses, ECD) but the bit does not drill. */
	public void testHoldBit(boolean hold) {
		holdBit = hold;
	}

	private boolean holdBit;

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
		BARITE("barite", Tuning.BARITE_SG), BENTONITE("bentonite", Tuning.BENTONITE_SG), SALT("rock salt", Tuning.SALT_APPARENT_SG),
		/** 23a: calcium chloride flake (77 %) for oil mud's water phase, the oil mud package, and LCM for losses. */
		CACL2("CaCl2 flake", Tuning.CACL2_APPARENT_SG), OBM("OBM package", Tuning.OBM_PACKAGE_SG), LCM("LCM", 2.7);

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

	/** Litres of mud in the tanks: the water (and 23a's diesel) and what the solids add to it. */
	public double mudVolume() {
		return water + diesel + barite / Tuning.BARITE_SG + bentonite / Tuning.BENTONITE_SG + dissolvedSalt() / Tuning.SALT_APPARENT_SG
				+ cacl2 / Tuning.CACL2_APPARENT_SG + obm / Tuning.OBM_PACKAGE_SG;
	}

	/** The mud's density, kg/L (water alone 1.0). */
	public double mudDensity() {
		double v = mudVolume();
		return v <= 0 ? (oilMud ? Tuning.DIESEL_KG_PER_L : Tuning.WATER_KG_PER_L)
				: (water * Tuning.WATER_KG_PER_L + diesel * Tuning.DIESEL_KG_PER_L + barite + bentonite + dissolvedSalt() + cacl2 + obm) / v;
	}

	// ---- 23a: oil-based (invert emulsion) mud ----------------------------------------------------------------------------------------
	/** The tanks switched to OIL MUD (sneak-click on them, empty). */
	private boolean oilMud;
	private double diesel;     // L
	private double cacl2;      // kg dissolved in the water phase
	private double obm;        // kg of emulsifier, organoclay and lime
	private double dieselLost; // L gone down the hole and to the pit
	private double cuttings;   // m3 of oily cuttings
	private long dumpAsked = -1000;

	public boolean oilMud() {
		return oilMud;
	}

	public double diesel() {
		return diesel;
	}

	public double dieselLost() {
		return dieselLost;
	}

	public double cuttings() {
		return cuttings;
	}

	/** Oil-water ratio's oil share. */
	public double owr() {
		return diesel + water <= 0 ? 0 : diesel / (diesel + water);
	}

	/** The water phase's CaCl2 by weight. */
	public double cacl2Strength() {
		return cacl2 + water <= 0 ? 0 : cacl2 / (cacl2 + water * Tuning.WATER_KG_PER_L);
	}

	/** The emulsion holds: enough of the package (15 lb/bbl). Without it the oil mud gives none of its benefits (ES LOW). */
	public boolean esOk() {
		double v = mudVolume();
		return v > 0 && obm / v >= Tuning.OBM_ES_KG_PER_L;
	}

	/** Oil mud whose emulsion holds: no washout in salt, never LOW GEL, 25 % faster, and a kick shows late. */
	public boolean oilMudWorking() {
		return oilMud && diesel > 0 && esOk();
	}

	/** The tanks' sneak-click: an empty system switches WATER / OIL MUD; a full one asks, then DUMPs to the reserve pit. */
	public Component tanksSwitch(long now) {
		if (mudVolume() < 1) {
			oilMud = !oilMud;
			water = 0;
			diesel = cacl2 = obm = barite = bentonite = salt = 0;
			setChanged();
			return Component.literal(oilMud ? "Mud tanks on OIL MUD: pipe diesel to the tanks and water from a pump (to 80/20), then CaCl2 and the OBM package through the hopper"
					: "Mud tanks on WATER MUD");
		}
		if (now - dumpAsked > 100) {
			dumpAsked = now;
			return Component.literal(String.format("DUMP %.0f m3 of mud to the reserve pit? Sneak-click again within 5 s", mudVolume() / 1000));
		}
		dumpAsked = -1000;
		double gone = mudVolume();
		dieselLost += diesel;
		water = 0;
		diesel = cacl2 = obm = barite = bentonite = salt = 0;
		setChanged();
		return Component.literal(String.format("DUMPED %.0f m3 of mud to the reserve pit (lost). Sneak-click the empty tanks to switch WATER / OIL MUD", gone / 1000));
	}

	/** Litres of water the oil mud wants for 80/20 against the diesel in the tanks. */
	private double waterTarget() {
		return diesel * (1 - Tuning.OIL_MUD_OWR) / Tuning.OIL_MUD_OWR;
	}

	/** Diesel off the pipe at the mud tanks; returns what fitted. */
	public double addDiesel(double litres) {
		double took = Math.max(0, Math.min(litres, Tuning.LAND_RIG_MUD_TANK - mudVolume()));
		diesel += took;
		if (took > 0) {
			setChanged();
		}
		return took;
	}

	/** Each second on oil mud: the tanks' transfer pump takes diesel off a steel pipe at the tanks. */
	private void takeDiesel(ServerLevel level) {
		if (!oilMud) {
			return;
		}
		BlockPos tanks = LandRig.mudTanks(worldPosition, facing());
		BlockState state = level.getBlockState(tanks);
		if (!(state.getBlock() instanceof dev.wildcatter.multiblock.BigPart)) {
			return;
		}
		java.util.List<Lines.Face> faces = Lines.faces(dev.wildcatter.multiblock.BigParts.cells(tanks, state));
		double want = Math.min(Tuning.DIESEL_FILL_LPS, Tuning.LAND_RIG_MUD_TANK - mudVolume());
		if (want > 1) {
			addDiesel(Lines.pull(level, faces, dev.wildcatter.fluid.Product.DIESEL, want, FluidNode.Pull.MACHINE, this));
		}
	}

	/** Game test hook: diesel straight into the tanks. */
	public void testDiesel(double litres) {
		addDiesel(litres);
	}

	/** "OIL MUD 80/20, 28 % CaCl2, 13.9 ppg, ES ok": oil mud for a readout. */
	private String oilMudText() {
		return String.format("OIL MUD %.0f/%.0f, %.0f %% CaCl2, %.1f ppg, %s; %.1f m3 of diesel lost, %.1f m3 of oily cuttings", owr() * 100, (1 - owr()) * 100,
				cacl2Strength() * 100, mudPpg(), esOk() ? "ES ok" : "ES LOW (mix the OBM package)", dieselLost / 1000, cuttings);
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
		if (oilMudWorking()) {
			return false;   // 23a: the organoclay carries the cuttings
		}
		return lowGel(bentonite, mudVolume());
	}

	/** LOW GEL for this much bentonite (kg) in this much mud (L), by the world's rule {@code mudGel}. */
	public static boolean lowGel(double bentoniteKg, double mudL) {
		if (!dev.wildcatter.Rules.mudGel()) {
			return false;   // the world rule is off (a world from before Phase 15): any mud drills at full rate, as before
		}
		return mudL <= 0 || bentoniteKg / mudL < Tuning.LOW_GEL_KG_PER_L;
	}

	/** Salt-saturated: as much salt as the water holds (360 kg a m3), so rock salt does not dissolve into the hole. */
	public boolean saltSaturated() {
		return water > 0 && salt >= water * Tuning.SALT_SATURATION_KG_PER_L * Tuning.SALT_SATURATED_AT;
	}

	/** "8.34 ppg (1,000 kg/m3), gel 0 lb/bbl LOW GEL, fresh": the mud for a readout. */
	public String mudText() {
		if (oilMud) {
			return oilMudText();
		}
		return String.format("%.2f ppg (%,.0f kg/m3), gel %.0f lb/bbl%s, %s", mudPpg(), mudDensity() * 1000, gelLbBbl(), lowGel() ? " LOW GEL" : "",
				saltSaturated() ? "salt-saturated" : salt > 1 ? "salty (not saturated)" : "fresh");
	}

	/** kg of this solid the tanks have room for. */
	public double roomFor(Solid solid) {
		double room = Tuning.LAND_RIG_MUD_TANK - mudVolume();
		if (solid == Solid.LCM) {
			return Tuning.BULK_BAG_KG;   // pumped as a pill: what the loss zone does not take is screened out at the shakers
		}
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
			case CACL2 -> {
				cacl2 += took * Tuning.CACL2_FLAKE;
				water += (int) Math.round(took * (1 - Tuning.CACL2_FLAKE));
			}
			case OBM -> obm += took;
			case LCM -> pumpLcm(took);
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
		dieselLost += diesel * (1 - keep);
		diesel *= keep;
		cacl2 *= keep;
		obm *= keep;
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
		if (phase == Phase.SHUT_IN && well != null && dev.wildcatter.Rules.chokeKill()) {
			ChokePanelBlockEntity panel = panel();
			return panel != null && panel.pumpOn() || well.spm() > 0 ? Tuning.LAND_RIG_DRILL_WATTS : Tuning.LAND_RIG_IDLE_WATTS;
		}
		if (phase == Phase.SHUT_IN && mudPpg() + 1e-6 >= killPpg) {
			return Tuning.LAND_RIG_DRILL_WATTS;   // the mud pumps circulating kill mud through the choke
		}
		if (phase == Phase.CASING) {
			return Tuning.LAND_RIG_CONNECTION_WATTS;   // the drawworks running casing, then the cementing unit and the wait
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
		if (oilMud) {
			took = Math.max(0, Math.min(took, (int) Math.floor(waterTarget() - water)));   // 23a: only to 80/20 against the diesel
		}
		water += took;
		if (took > 0) {
			setChanged();
		}
		return took;
	}

	public double waterLevel() {
		if (oilMud) {
			return diesel <= 0 ? 1.0 : Math.min(1.0, water / Math.max(1, waterTarget()));
		}
		return water / (double) Tuning.LAND_RIG_MUD_TANK;
	}

	/** Game test hook: empties or fills the mud tanks with water (what was mixed in stays, and is thinned or thickened). */
	public void testWater(int litres) {
		water = litres;
		if (litres <= 0) {
			barite = bentonite = salt = 0;
			diesel = cacl2 = obm = 0;
		}
		setChanged();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, RigSubstructureBlockEntity rig) {
		rig.followPlc(level.getGameTime());
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		if ((level.getGameTime() + pos.hashCode()) % 20 == 0) {
			rig.takeDiesel(server);
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
		if (rig.formed && (rig.phase == Phase.KICK || rig.phase == Phase.SHUT_IN || rig.phase == Phase.BLOWOUT || rig.phase == Phase.UNDERGROUND)) {
			rig.wellControl(server, power);
		} else if (rig.formed && rig.phase == Phase.CASING) {
			rig.runCasing(server, power);
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
		} else if (rig.needsChoke(server)) {
			rig.status = "NO CHOKE: the rule chokeKill wants a CHOKE MANIFOLD piped from the BOP stack's choke line within " + Tuning.CHOKE_REACH + " blocks before a deep field";
		} else if (rig.lossCheck(server)) {
			rig.status = rig.lossText();
		} else if (rig.washout(server)) {
			rig.status = "WASHOUT: fresh mud dissolves the rock salt and the hole washes out; mix rock salt into the mud until it is salt-saturated ("
					+ Math.round(Tuning.SALT_SATURATION_KG_PER_L * 1000) + " kg a m3 of water)";
		} else {
			rpm = (float) Tuning.TOP_DRIVE_RPM;
			double rate = power * (rig.lowGel() ? Tuning.LOW_GEL_RATE : 1) * (rig.oilMudWorking() ? Tuning.OIL_MUD_RATE : 1);
			rig.progress += rig.holdBit ? 0 : rate;
			if (rig.oilMudWorking() && server.getGameTime() % 200 == 0) {
				dev.wildcatter.quest.QuestEvents.milestone(server, rig.hole(), dev.wildcatter.quest.Milestones.OIL_MUD_DRILLING);
			}
			rig.status = rig.lossText() + (rig.oilMudWorking() ? "on oil mud, " : "") + "drilling at " + Math.round(rate * 100) + "% of full rate" + (rig.lowGel() ? " (LOW GEL: the cuttings settle; mix bentonite to " + Math.round(Tuning.LOW_GEL_KG_PER_L * 350.5) + " lb/bbl)" : "");
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
		if (!dev.wildcatter.Rules.wellControl() || saltSaturated() || oilMudWorking() || !isSalt(level.getBlockState(hole().below(depth + 1)))) {
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

	/** A strike into this field with mud this heavy kicks: a deep (geopressured) field under its kill weight, by the rule {@code wellControl}. */
	public static boolean kicks(Reservoir field, double mudPpg) {
		return dev.wildcatter.Rules.wellControl() && field.deep() && mudPpg < porePpg(field) + Tuning.TRIP_MARGIN_PPG - 1e-6;
	}

	/** The next block reaches a deep (geopressured) field and there is no BOP. */
	private boolean needsBop(ServerLevel level) {
		Reservoir field = Drilling.field(level, hole());
		return dev.wildcatter.Rules.wellControl() && field != null && field.deep() && hole().getY() - (depth + 1) <= field.topY() && !hasBop();
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
		if (oilMud) {
			cuttings += Tuning.CUTTINGS_M3_PER_M;   // counted on the readout; they go nowhere (a cuttings dryer is left out)
		}
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
			if (kicks(field, mudPpg())) {
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
		dev.wildcatter.quest.QuestEvents.milestone(level, hole, dev.wildcatter.quest.Milestones.WELL_STRUCK);   // the quest book: a strike
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
				+ (topDrive ? "in" : "MISSING") + bopText() + casingText() + (joints() + cementBags() > 0 ? String.format(", rack %d joints and %d cement bags", joints(), cementBags()) : "")
				+ (lostMud > 0.05 ? String.format(", %.1f m3 lost to the formation", lostMud) : "") + (house != null && formed ? "; drive house " + house.load().describe() : ""));
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
		// 23a-23b
		output.putInt("shoeDepth", shoeDepth);
		output.putDouble("shoeFg", shoeFgPpg);
		output.putBoolean("lotDone", lotDone);
		output.putDouble("lcmBonus", lcmBonus);
		output.putDouble("lcmCure", lcmCure);
		output.putDouble("casingLeft", casingLeft);
		output.putInt("losses", losses.ordinal());
		output.putDouble("lostMud", lostMud);
		output.putInt("columnDrop", columnDrop);
		output.putBoolean("kickShown", kickShown);
		output.putDouble("influxUp", influxUp);
		output.putBoolean("oilMud", oilMud);
		output.putDouble("diesel", diesel);
		output.putDouble("cacl2", cacl2);
		output.putDouble("obm", obm);
		output.putDouble("dieselLost", dieselLost);
		output.putDouble("cuttings", cuttings);
		output.putDouble("sicp", sicp);
		output.putInt("flowCheck", flowCheck);
		output.putString("well", well == null ? "" : well.save());
		output.putInt("drillers", drillers);
		output.putDouble("secondFrom", secondFrom);
		output.putDouble("influxFrom", influxFrom);
		output.putDouble("sheetOmw", sheetOmw);
		output.putDouble("gasVented", gasVented);
		net.minecraft.world.ContainerHelper.saveAllItems(output, rack);
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
		shoeDepth = input.getIntOr("shoeDepth", 0);
		shoeFgPpg = input.getDoubleOr("shoeFg", Tuning.FG_OPEN_HOLE_PPG);
		lotDone = input.getBooleanOr("lotDone", false);
		lcmBonus = input.getDoubleOr("lcmBonus", 0);
		lcmCure = input.getDoubleOr("lcmCure", 0);
		casingLeft = input.getDoubleOr("casingLeft", 0);
		losses = dev.wildcatter.well.WellControl.Losses.values()[Math.max(0, Math.min(2, input.getIntOr("losses", 0)))];
		lostMud = input.getDoubleOr("lostMud", 0);
		columnDrop = input.getIntOr("columnDrop", 0);
		kickShown = input.getBooleanOr("kickShown", true);
		influxUp = input.getDoubleOr("influxUp", 0);
		oilMud = input.getBooleanOr("oilMud", false);
		diesel = input.getDoubleOr("diesel", 0);
		cacl2 = input.getDoubleOr("cacl2", 0);
		obm = input.getDoubleOr("obm", 0);
		dieselLost = input.getDoubleOr("dieselLost", 0);
		cuttings = input.getDoubleOr("cuttings", 0);
		sicp = input.getDoubleOr("sicp", 0);
		flowCheck = input.getIntOr("flowCheck", 0);
		String w = input.getStringOr("well", "");
		well = w.isEmpty() ? null : dev.wildcatter.well.KillModel.load(w);
		drillers = input.getIntOr("drillers", 0);
		secondFrom = input.getDoubleOr("secondFrom", 0);
		influxFrom = input.getDoubleOr("influxFrom", 0);
		sheetOmw = input.getDoubleOr("sheetOmw", 0);
		gasVented = input.getDoubleOr("gasVented", 0);
		rack.clear();
		net.minecraft.world.ContainerHelper.loadAllItems(input, rack);
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
