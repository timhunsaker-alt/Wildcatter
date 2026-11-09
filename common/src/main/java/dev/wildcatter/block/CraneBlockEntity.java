package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.blueprint.Blueprint;
import dev.wildcatter.blueprint.BlueprintLibrary;
import dev.wildcatter.blueprint.Masses;
import dev.wildcatter.blueprint.Surveys;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.DriveLoad;
import dev.wildcatter.crane.CraneKind;
import dev.wildcatter.crane.LiftCycle;
import dev.wildcatter.crane.LoadChart;
import dev.wildcatter.crane.Siting;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.multiblock.BigParts;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A crane at work (docs/multiblocks/tower-crane.md, crawler-crane.md, ring-crane.md): it finds the nearest site marker in its reach
 * and takes the earliest lift there it can make (in DISMANTLE, the last standing one), passing over lifts it cannot make but never
 * onto one resting on a lift not yet set (docs/blueprints.md, "Sharing and tandem lifts"). It checks each against its chart, its height
 * under the hook, its chest and its power, then runs the lift's steps ({@link LiftCycle}) at its real speeds. A part too heavy for any
 * one crane on the site goes up in tandem with a second crane, each carrying half. The tower crane is one frequency-converter load on
 * the industrial voltage at the west face of its switch cabinet; the crawler and ring cranes burn diesel from their own tanks, piped
 * to any cell of their base. The client draws it from the step it is in.
 */
public class CraneBlockEntity extends BlockEntity implements CircuitPart, Inspectable, BigPartEntity, dev.wildcatter.control.ControlDevice {
	private final dev.wildcatter.control.Remote remote = new dev.wildcatter.control.Remote(1);

	@Override
	public String deviceName() {
		return "Crane";
	}

	@Override
	public java.util.List<Point> points() {
		return java.util.List.of(new Point("RUN ENABLE", true), new Point("CARRYING", false), new Point("AUTO", false));
	}

	@Override
	public boolean readPoint(int point) {
		return point == 1 ? carrying() : point == 2 && remote.auto();
	}

	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
		if (point == 0) {
			remote.command(0, on, gameTime);
		}
	}

	@Override
	public String cycleHoa(BlockPos cell) {
		setChanged();
		return remote.cycle();
	}

	/** The cranes the server has loaded, for a tandem partner. */
	public static final Set<CraneBlockEntity> SERVER = ConcurrentHashMap.newKeySet();
	/** How many open lifts a crane looks at for one it can make, each time it looks. */
	private static final int LOOK_AHEAD = 64;

	private final DriveLoad load = new DriveLoad(Voltage.INDUSTRIAL, Tuning.LAND_RIG_POWER_FACTOR, 0);
	private boolean grown;
	private int boom;
	private double litres;
	// where the hook is (at the start of the step it is in)
	private double hookDeg, hookRadius, hookHeight;
	private boolean parked;
	// the lift it is working
	private @Nullable BlockPos site;
	private int lift = -1;
	private boolean dismantling;
	private @Nullable List<LiftCycle.Step> steps;
	private int step;
	private double elapsed;
	private long stepStart;
	private boolean carried;
	private double onHook;
	/** A tandem lift's whole load, t; 0 alone. */
	private double tandemTonnes;
	/** The lift's radius: the farther of its pick and set, where its chart is read. */
	private double liftRadius;
	/** The other crane of a tandem lift, and whether this one leads it (the part is in its chest, and it sets it). */
	private @Nullable BlockPos partner;
	private boolean leading;
	private final List<ItemStack> held = new ArrayList<>();
	private String status = "starting";
	private String doing = "";
	/** The ring crane's laydown chest, and when to look for one again. */
	private @Nullable BlockPos laydown;
	private long nextScan;
	/** After a lift is found blocked by its jib or boom, it looks again from this game time. */
	private long blockedUntil;
	/** The ring crane's travel on its rails: metres still to go (signed along x or z), and how far into the next metre it is. */
	private int travelLeft;
	private boolean travelAlongX;
	private double travelled;
	private String travelTo = "";
	/** For the game test: its travel sped up this many times. */
	private double speedup = 1;
	/** Set while it moves itself a metre along its rails, so its old cells going leave the site and the chest alone. */
	private boolean moving;
	/** For the game test: the wind 10 m up, m/s, in place of the world's weather; negative to read the weather. */
	private double testWind = -1;
	/** True while it holds a lift for the wind. */
	private boolean windHeld;

	public CraneBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CRANE_ENTITY, pos, state);
		boom = state.getBlock() instanceof CranePartBlock part ? part.part().kind.firstBoom() : 0;
	}

	public CraneKind kind() {
		return getBlockState().getBlock() instanceof CranePartBlock part ? part.part().kind : CraneKind.TOWER;
	}

	public int boom() {
		return boom;
	}

	public LoadChart chart() {
		return kind().chart(boom);
	}

	private String boomText() {
		return kind().hasBoom() ? " on the " + kind().boomName(boom) + " boom" : "";
	}

	// ---- geometry -------------------------------------------------------------------------------------------------------------------
	/** The slewing centre, world x and z: the middle of the base. */
	public double[] centre() {
		int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
		for (BlockPos cell : BigParts.cells(worldPosition, getBlockState())) {
			minX = Math.min(minX, cell.getX());
			maxX = Math.max(maxX, cell.getX());
			minZ = Math.min(minZ, cell.getZ());
			maxZ = Math.max(maxZ, cell.getZ());
		}
		return new double[] {(minX + maxX + 1) / 2.0, (minZ + maxZ + 1) / 2.0};
	}

	/** Degrees clockwise from north, seen from above, from the slewing centre to world x, z. */
	private double degreesTo(double x, double z) {
		double[] c = centre();
		double d = Math.toDegrees(Math.atan2(x - c[0], -(z - c[1])));
		return d < 0 ? d + 360 : d;
	}

	private double radiusTo(double x, double z) {
		double[] c = centre();
		return Math.hypot(x - c[0], z - c[1]);
	}

	// ---- the chest ----------------------------------------------------------------------------------------------------------------
	/**
	 * Its chest: for the tower and crawler cranes a chest, barrel or hopper against any cell of the base; for the ring crane, whose
	 * chart starts 40 m out, the one nearest its centre within its chart's radius, its laydown. Null if none.
	 */
	public @Nullable BlockPos chestPos() {
		if (level == null) {
			return null;
		}
		return kind() == CraneKind.RING ? laydownPos() : againstBase();
	}

	private @Nullable BlockPos againstBase() {
		List<BlockPos> cells = BigParts.cells(worldPosition, getBlockState());
		Set<BlockPos> mine = new HashSet<>(cells);
		for (BlockPos cell : cells) {
			for (Direction d : Direction.values()) {
				BlockPos n = cell.relative(d);
				if (d != Direction.DOWN && !mine.contains(n) && level.getBlockEntity(n) instanceof Container) {
					return n;
				}
			}
		}
		return null;
	}

	private boolean laydownFits(BlockPos at) {
		return level.getBlockEntity(at) instanceof Container && chart().reaches(radiusTo(at.getX() + 0.5, at.getZ() + 0.5));
	}

	private @Nullable BlockPos laydownPos() {
		if (laydown != null && laydownFits(laydown)) {
			return laydown;
		}
		laydown = null;
		if (level.isClientSide() || level.getGameTime() < nextScan) {
			return null;
		}
		nextScan = level.getGameTime() + 100;   // look again in 5 s
		double[] c = centre();
		double reach = chart().maxRadius();
		double best = Double.MAX_VALUE;
		for (int cx = SectionPos.blockToSectionCoord(c[0] - reach); cx <= SectionPos.blockToSectionCoord(c[0] + reach); cx++) {
			for (int cz = SectionPos.blockToSectionCoord(c[1] - reach); cz <= SectionPos.blockToSectionCoord(c[1] + reach); cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (be instanceof Container) {
						BlockPos at = be.getBlockPos();
						double r = radiusTo(at.getX() + 0.5, at.getZ() + 0.5);
						if (r < best && chart().reaches(r)) {
							best = r;
							laydown = at.immutable();
						}
					}
				}
			}
		}
		return laydown;
	}

	private String noChest() {
		return kind() == CraneKind.RING
				? String.format("NO CHEST: set a chest within its reach, %.0f to %.0f m out", chart().minRadius(), chart().maxRadius())
				: "NO CHEST: set a chest, barrel or hopper against its base";
	}

	private @Nullable Container chest() {
		BlockPos at = chestPos();
		return at != null && level.getBlockEntity(at) instanceof Container c ? c : null;
	}

	private static int count(Container c, Item item) {
		int n = 0;
		for (int i = 0; i < c.getContainerSize(); i++) {
			if (c.getItem(i).is(item)) {
				n += c.getItem(i).getCount();
			}
		}
		return n;
	}

	private static void take(Container c, Item item, int n) {
		for (int i = 0; i < c.getContainerSize() && n > 0; i++) {
			if (c.getItem(i).is(item)) {
				n -= c.removeItem(i, n).getCount();
			}
		}
		c.setChanged();
	}

	/** Puts {@code stack} into {@code c} as far as it fits; returns what did not. */
	private static ItemStack put(Container c, ItemStack stack) {
		for (int i = 0; i < c.getContainerSize() && !stack.isEmpty(); i++) {
			ItemStack there = c.getItem(i);
			if (!there.isEmpty() && ItemStack.isSameItemSameComponents(there, stack) && there.getCount() < there.getMaxStackSize()) {
				int move = Math.min(stack.getCount(), there.getMaxStackSize() - there.getCount());
				there.grow(move);
				stack.shrink(move);
			}
		}
		for (int i = 0; i < c.getContainerSize() && !stack.isEmpty(); i++) {
			if (c.getItem(i).isEmpty() && c.canPlaceItem(i, stack)) {
				c.setItem(i, stack.split(Math.min(stack.getCount(), stack.getMaxStackSize())));
			}
		}
		c.setChanged();
		return stack;
	}

	/** What lift {@code lift} needs from the chest: each item and how many, leaving out what stands already. */
	private Map<Item, Integer> needs(SiteMarkerBlockEntity marker, Blueprint.Lift lift) {
		Map<Item, Integer> out = new LinkedHashMap<>();
		Blueprint b = marker.blueprint();
		if (lift.isPart()) {
			Block part = Surveys.part(b.parts().get(lift.part()));
			if (part != null) {
				out.put(part.asItem(), 1);
			}
			return out;
		}
		BlockState[] states = marker.states();
		for (int i : lift.blocks()) {
			Blueprint.Block k = b.blocks().get(i);
			BlockState want = states[k.state()];
			if (want != null && !Surveys.blockStands(level, marker.getBlockPos(), k, want)) {
				out.merge(want.getBlock().asItem(), 1, Integer::sum);
			}
		}
		return out;
	}

	/** What the chest lacks for lift {@code l}, as "2 x Pot, 12 x Stone Bricks", or empty. */
	private String missing(SiteMarkerBlockEntity m, Blueprint.Lift l) {
		Container c = chest();
		StringBuilder missing = new StringBuilder();
		for (Map.Entry<Item, Integer> e : needs(m, l).entrySet()) {
			int short_ = e.getValue() - (c == null ? 0 : count(c, e.getKey()));
			if (short_ > 0) {
				missing.append(missing.isEmpty() ? "" : ", ").append(short_).append(" x ").append(new ItemStack(e.getKey()).getHoverName().getString());
			}
		}
		return missing.toString();
	}

	// ---- finding work -----------------------------------------------------------------------------------------------------------
	private @Nullable SiteMarkerBlockEntity findSite() {
		double[] c = centre();
		SiteMarkerBlockEntity best = null;
		double bestDistance = chart().maxRadius();
		for (SiteMarkerBlockEntity m : SiteMarkerBlockEntity.SERVER) {
			if (m.isRemoved() || m.getLevel() != level) {
				continue;
			}
			double d = Math.hypot(m.getBlockPos().getX() + 0.5 - c[0], m.getBlockPos().getZ() + 0.5 - c[1]);
			if (d <= bestDistance) {
				best = m;
				bestDistance = d;
			}
		}
		return best;
	}

	private @Nullable SiteMarkerBlockEntity marker() {
		return site != null && level != null && level.getBlockEntity(site) instanceof SiteMarkerBlockEntity m ? m : null;
	}

	private String siteName(SiteMarkerBlockEntity m) {
		return m.data().startsWith("lib:") ? BlueprintLibrary.title(m.data().substring(4)) : m.blueprint().name();
	}

	private boolean powered() {
		if (kind().diesel()) {
			return litres > 0.01;
		}
		return !load.undervoltage() && load.volts() >= Voltage.INDUSTRIAL.lineToLine() * Tuning.LAND_RIG_UNDERVOLTAGE;
	}

	private String noPower() {
		return kind() == CraneKind.CRAWLER ? "NO DIESEL: its tank is dry, pipe diesel to its crawlers"
				: kind() == CraneKind.RING ? "NO DIESEL: its tank is dry, pipe diesel to its ring"
				: String.format("NO POWER: %.0f V at its switch cabinet", load.volts());
	}

	/**
	 * Looks for its next lift and starts it, or says what it waits for. It walks the site's lifts in order (backward in DISMANTLE),
	 * skipping what is done, what another crane has, and what rests on a lift not yet set (or carries one still standing); it takes
	 * the first it can make. With none, it reads the first one it could not make and why.
	 */
	private void findWork() {
		if (level.getGameTime() < blockedUntil) {
			return;   // its jib or boom was found blocked: it looks again shortly
		}
		String stop = windStop();
		if (stop == null) {
			stop = softGround();
		}
		if (stop != null) {
			status = stop;
			return;
		}
		SiteMarkerBlockEntity m = findSite();
		site = m == null ? null : m.getBlockPos();
		if (m == null) {
			status = String.format("NO SITE within %.0f m", chart().maxRadius());
			planTravel();
			return;
		}
		Blueprint b = m.blueprint();
		List<Blueprint.Lift> lifts = m.lifts();
		if (b == null || lifts == null) {
			status = "THE SITE'S BLUEPRINT IS UNREADABLE";
			return;
		}
		boolean down = m.dismantle();
		String first = null;
		boolean left = false;
		int looked = 0;
		for (int k = 0; k < lifts.size() && looked < LOOK_AHEAD; k++) {
			int i = down ? lifts.size() - 1 - k : k;
			if (down ? !m.anyStands(i) : m.stands(i)) {
				continue;
			}
			left = true;
			if (m.claimedByOther(i, worldPosition) || !(down ? m.openForDismantle(i) : m.openForBuild(i))) {
				continue;
			}
			looked++;
			String why = tryLift(m, i, down);
			if (why == null) {
				return;
			}
			if (why.startsWith("BLOCKED")) {
				status = why;   // its slew is blocked for every lift alike: it looks again in 5 s
				blockedUntil = level.getGameTime() + 100;
				return;
			}
			if (first == null) {
				first = why;
			}
		}
		if (first != null) {
			status = first;
			if (first.startsWith("OUT OF REACH")) {
				planTravel();
			}
		} else if (left) {
			status = down ? "WAITING: the lifts left to take down carry lifts still on other cranes' hooks"
					: "WAITING: the lifts left rest on lifts on other cranes' hooks";
		} else {
			status = String.format("IDLE: %s is %s", siteName(m), down ? "cleared" : "built");
		}
	}

	/** Where a crane works one lift: its hooks at the pick and the set, its clearance over the site, the load's height. */
	private record Work(LiftCycle.Hook pick, LiftCycle.Hook set, double clearance, double loadHeight) {
		double radius() {
			return Math.max(pick.radius(), set.radius());
		}
	}

	/**
	 * This crane's hooks for a load picked at world x, z with its bottom at {@code pickY}, and set at x, z with its bottom at
	 * {@code setY}. A chest against its own base is read at its chart's least radius, as the load swings clear of it.
	 */
	private Work work(double px, double pickY, double pz, boolean ownChest, double sx, double setY, double sz, double siteTop, double loadHeight) {
		double pickRadius = ownChest && kind() != CraneKind.RING ? chart().minRadius() : radiusTo(px, pz);
		LiftCycle.Hook pick = new LiftCycle.Hook(degreesTo(px, pz), pickRadius, pickY - worldPosition.getY());
		LiftCycle.Hook set = new LiftCycle.Hook(degreesTo(sx, sz), radiusTo(sx, sz), setY - worldPosition.getY());
		double clearance = Math.max(siteTop - worldPosition.getY(), Math.max(pick.height(), set.height())) + 2;
		return new Work(pick, set, clearance, loadHeight);
	}

	/** Why this crane cannot reach or clear {@code w}, or null if it can. */
	private @Nullable String reachOrHeight(Work w, String what) {
		LoadChart chart = chart();
		if (!chart.reaches(w.set().radius()) || !chart.reaches(w.pick().radius())) {
			double r = chart.reaches(w.set().radius()) ? w.pick().radius() : w.set().radius();
			String where = chart.reaches(w.set().radius()) ? "its pick" : what;
			return String.format("OUT OF REACH: %s at %.0f m (%.1f to %.1f m%s)", where, r, chart.minRadius(), chart.maxRadius(), boomText());
		}
		double under = Math.min(kind().hookHeight(boom, w.set().radius()), kind().hookHeight(boom, w.pick().radius()));
		if (w.clearance() + w.loadHeight() > under) {
			return String.format("TOO HIGH: the site needs %.0f m under the hook (%.1f m%s)", w.clearance() + w.loadHeight(), under, boomText());
		}
		return null;
	}

	/** Starts lift {@code index} if this crane (or it and a tandem partner) can make it now; else why not. */
	private @Nullable String tryLift(SiteMarkerBlockEntity m, int index, boolean down) {
		Blueprint b = m.blueprint();
		Blueprint.Lift l = m.lifts().get(index);
		String what = m.describe(l);
		int[] box = b.box(l);
		BlockPos origin = m.getBlockPos();
		double sx = origin.getX() + (box[0] + box[3] + 1) / 2.0;
		double sz = origin.getZ() + (box[2] + box[5] + 1) / 2.0;
		double setY = origin.getY() + box[1];
		double siteTop = origin.getY() + b.sizeY();
		double loadHeight = box[4] - box[1] + 1;
		double tonnes = Masses.onHook(l.tonnes());
		BlockPos chestAt = chestPos();
		if (chestAt == null) {
			return noChest();
		}
		Work w = work(chestAt.getX() + 0.5, chestAt.getY() + 1, chestAt.getZ() + 0.5, true, sx, setY, sz, siteTop, loadHeight);
		String cannot = reachOrHeight(w, what);
		if (cannot != null) {
			return cannot;
		}
		LoadChart chart = chart();
		double hooked = tonnes + kind().hookBlockTonnes();
		double capacity = chart.capacity(w.radius());
		boolean tandem = hooked > capacity;
		String heavy = String.format("TOO HEAVY: %s, %.1f t on the hook, chart %.1f t at %.0f m%s", what, hooked, capacity, chart.readAt(w.radius()), boomText());
		if (tandem && !l.isPart()) {
			return heavy;   // a pallet is split to suit the lightest chart; it never goes up in tandem
		}
		if (!down) {
			BlockPos blocked = m.inTheWay(l);
			if (blocked != null) {
				return String.format("IN THE WAY: %s at %d, %d, %d", BuiltInRegistries.BLOCK.getKey(level.getBlockState(blocked).getBlock()),
						blocked.getX(), blocked.getY(), blocked.getZ());
			}
			String lacks = missing(m, l);
			if (!lacks.isEmpty()) {
				return tandem ? heavy : "WAITING: " + lacks + " in its chest";
			}
		}
		if (!powered()) {
			return noPower();
		}
		if (tandem) {
			String why = tryTandem(m, index, l, chestAt, sx, setY, sz, siteTop, loadHeight, tonnes, down, box);
			return why == null ? null : heavy + (kind() == CraneKind.CRAWLER ? ": a shorter boom lifts more" : "") + "; " + why;
		}
		List<LiftCycle.Step> plan = LiftCycle.plan(kind(), boom, here(), down ? w.set() : w.pick(), down ? w.pick() : w.set(), w.clearance(), hooked);
		String struck = struck(plan);
		if (struck != null) {
			return struck;
		}
		if (!m.claim(index, worldPosition)) {
			return "WAITING: another crane took it";
		}
		start(m, index, down, plan, hooked, 0, w.radius(), null, false);
		return null;
	}

	private LiftCycle.Hook here() {
		return new LiftCycle.Hook(hookDeg, hookRadius, hookHeight);
	}

	/** True if this crane could lift {@code l} of site {@code m} by itself, from its own chest, at its chart. */
	private boolean holdsAlone(SiteMarkerBlockEntity m, Blueprint.Lift l) {
		BlockPos chestAt = chestPos();
		if (chestAt == null || !worldPositionSite(m)) {
			return false;
		}
		Blueprint b = m.blueprint();
		int[] box = b.box(l);
		BlockPos o = m.getBlockPos();
		Work w = work(chestAt.getX() + 0.5, chestAt.getY() + 1, chestAt.getZ() + 0.5, true, o.getX() + (box[0] + box[3] + 1) / 2.0, o.getY() + box[1],
				o.getZ() + (box[2] + box[5] + 1) / 2.0, o.getY() + b.sizeY(), box[4] - box[1] + 1);
		return reachOrHeight(w, "") == null && Masses.onHook(l.tonnes()) + kind().hookBlockTonnes() <= chart().capacity(w.radius());
	}

	private boolean worldPositionSite(SiteMarkerBlockEntity m) {
		return m.getBlockPos().equals(site);
	}

	/**
	 * A part too heavy for this crane alone: if no crane on the site can take it alone, looks for an idle, powered partner on the
	 * same site, so that each carries half (plus its own hook block) within 80 % of its chart at the farther of its pick and set,
	 * both reaching the pick at this crane's chest and the set. Starts the lift and returns null, or says why it cannot.
	 */
	private @Nullable String tryTandem(SiteMarkerBlockEntity m, int index, Blueprint.Lift l, BlockPos chestAt, double sx, double setY, double sz,
			double siteTop, double loadHeight, double tonnes, boolean down, int[] box) {
		List<CraneBlockEntity> others = new ArrayList<>();
		for (CraneBlockEntity c : SERVER) {
			if (c != this && !c.isRemoved() && c.level == level && c.worldPositionSite(m)) {
				others.add(c);
			}
		}
		for (CraneBlockEntity c : others) {
			if (c.holdsAlone(m, l)) {
				return "the " + c.kind().title() + " at " + c.worldPosition.toShortString() + " takes it alone";
			}
		}
		double px = chestAt.getX() + 0.5, pickY = chestAt.getY() + 1, pz = chestAt.getZ() + 0.5;
		double half = Math.max(0, (Math.max(box[3] - box[0], box[5] - box[2])) / 2.0);   // the hooks at the part's two ends
		double[] mine = centre();
		for (CraneBlockEntity p : others) {
			if (p.steps != null || !p.powered()) {
				continue;
			}
			double[] theirs = p.centre();
			double ux = theirs[0] - mine[0], uz = theirs[1] - mine[1];
			double len = Math.hypot(ux, uz);
			if (len < 1e-6) {
				continue;
			}
			ux = ux / len * half;
			uz = uz / len * half;
			Work a = work(px - ux, pickY, pz - uz, true, sx - ux, setY, sz - uz, siteTop, loadHeight);
			Work bw = p.work(px + ux, pickY, pz + uz, false, sx + ux, setY, sz + uz, siteTop, loadHeight);
			double shareA = LiftCycle.tandemShare(kind(), tonnes), shareB = LiftCycle.tandemShare(p.kind(), tonnes);
			if (reachOrHeight(a, "") != null || p.reachOrHeight(bw, "") != null
					|| shareA > LiftCycle.TANDEM_USE * chart().capacity(a.radius()) || shareB > LiftCycle.TANDEM_USE * p.chart().capacity(bw.radius())) {
				continue;
			}
			if (!m.claim(index, worldPosition)) {
				return "another crane took it";
			}
			// one height over the site for both hooks, each measured from its own crane's base
			double over = Math.max(a.clearance() + worldPosition.getY(), bw.clearance() + p.worldPosition.getY());
			List<LiftCycle.Step> mineSteps = LiftCycle.plan(kind(), boom, here(), down ? a.set() : a.pick(), down ? a.pick() : a.set(), over - worldPosition.getY(), shareA);
			List<LiftCycle.Step> theirSteps = LiftCycle.plan(p.kind(), p.boom, p.here(), down ? bw.set() : bw.pick(), down ? bw.pick() : bw.set(),
					over - p.worldPosition.getY(), shareB);
			String struck = struck(mineSteps);
			if (struck == null) {
				struck = p.struck(theirSteps);
			}
			if (struck != null) {
				m.release(index, worldPosition);
				return struck;
			}
			LiftCycle.Tandem t = LiftCycle.together(mineSteps, theirSteps);
			start(m, index, down, t.lead(), shareA, tonnes, a.radius(), p.worldPosition, true);
			p.start(m, index, down, t.follower(), shareB, tonnes, bw.radius(), worldPosition, false);
			return null;
		}
		return "no second crane in reach can take half with 25 % to spare";
	}

	/** Begins a lift's steps: alone, or as the lead or the follower of a tandem lift with the crane at {@code with}. */
	private void start(SiteMarkerBlockEntity m, int index, boolean down, List<LiftCycle.Step> plan, double share, double whole, double radius,
			@Nullable BlockPos with, boolean lead) {
		site = m.getBlockPos();
		steps = plan;
		lift = index;
		dismantling = down;
		step = 0;
		elapsed = 0;
		onHook = share;
		tandemTonnes = whole;
		liftRadius = radius;
		carried = false;
		partner = with == null ? null : with.immutable();
		leading = with == null || lead;
		stepStart = level.getGameTime();
		doing = String.format("%s %s, lift %d of %d", down ? "DISMANTLING" : "BUILDING", siteName(m), index + 1, m.lifts().size());
		status = describeStep();
		sync();
	}

	private @Nullable CraneBlockEntity partnerCrane() {
		return partner != null && level != null && level.getBlockEntity(partner) instanceof CraneBlockEntity c ? c : null;
	}

	private String describeStep() {
		if (steps == null) {
			return status;
		}
		SiteMarkerBlockEntity m = marker();
		LiftCycle.Step st = steps.get(Math.min(step, steps.size() - 1));
		String what = m != null && m.lifts() != null && lift < m.lifts().size() ? m.describe(m.lifts().get(lift)) : "its load";
		double capacity = chart().capacity(liftRadius);
		if (partner != null) {
			CraneBlockEntity p = partnerCrane();
			String with = String.format("TANDEM with the %s at %d, %d, %d", p == null ? "crane" : p.kind().title(), partner.getX(), partner.getY(), partner.getZ());
			return String.format("%s: %s: %s %s, %.1f of %.1f t, chart %.1f t at %.0f m, 80 %% %.1f t", with, doing, st.phase().label, what, onHook,
					tandemTonnes, capacity, chart().readAt(liftRadius), LiftCycle.TANDEM_USE * capacity);
		}
		String cap = chart().capped(liftRadius) ? String.format(" (chart %.0f t, its hook %.0f t)", chart().chartFigure(liftRadius), capacity) : "";
		return String.format("%s: %s %s, %.1f t on the hook, chart %.1f t at %.0f m%s", doing, st.phase().label, what, onHook,
				capacity, chart().readAt(liftRadius), cap);
	}

	// ---- each tick --------------------------------------------------------------------------------------------------------------------
	public static void tick(Level level, BlockPos pos, BlockState state, CraneBlockEntity crane) {
		if (!crane.remote.hand() && !crane.remote.on(0, level.getGameTime())) {
			return;   // held where it is: a load stays on the hook
		}
		SERVER.add(crane);
		crane.step(level);
	}

	private void step(Level level) {
		if (!parked) {
			hookDeg = (getBlockState().getValue(BigPartBlock.FACING).toYRot() + 180) % 360;
			hookRadius = chart().minRadius();
			hookHeight = Math.max(2, kind().hookHeight(boom, hookRadius) - 6);
			if (kind() == CraneKind.RING) {
				hookHeight = Math.min(hookHeight, 20);   // its hook block lowered near the ground
			}
			parked = true;
			sync();
		}
		if (kind().diesel() && level.getGameTime() % Tuning.WELL_INTERVAL == 0) {
			double room = kind().tankLitres() - litres;
			if (room >= 1) {
				litres += Lines.pull(level, Lines.faces(BigParts.cells(worldPosition, getBlockState())), Product.DIESEL, room, FluidNode.Pull.MACHINE, null);
			}
		}
		if (partner != null && !leading) {
			follow(level);
			return;
		}
		if (!held.isEmpty() && steps == null) {
			returnHeld();   // a load left on the hook (the world was saved mid-lift) goes back to the chest
		}
		if (steps == null && travelLeft != 0) {
			travel(level);
			return;
		}
		if (steps == null) {
			load.setDemand(site != null ? CraneKind.TOWER_STANDBY_WATTS : 0);
			if ((level.getGameTime() + worldPosition.hashCode()) % 20 == 0) {
				findWork();
				setChanged();
			}
			return;
		}
		CraneBlockEntity other = null;
		if (partner != null) {
			other = partnerCrane();
			if (other == null || !worldPosition.equals(other.partner)) {
				abort("THE TANDEM PARTNER IS GONE: its load went back to the chest");
				return;
			}
		}
		if (!powered() || (other != null && !other.powered())) {
			String who = !powered() ? noPower() : "its tandem partner: " + other.noPower();
			status = who + "; holding " + (carried ? "its load on its brakes" : "where it is");
			load.setDemand(CraneKind.TOWER_STANDBY_WATTS);
			return;
		}
		String gale = level.getGameTime() % 20 == 0 ? windStop() : null;
		if (gale == null && other != null && level.getGameTime() % 20 == 0) {
			gale = other.windStop();
		}
		if (gale != null || (windHeld && level.getGameTime() % 20 != 0)) {
			if (gale != null) {
				status = gale + "; holding " + (carried ? "its load on its brakes" : "where it is");
				windHeld = true;
			}
			load.setDemand(CraneKind.TOWER_STANDBY_WATTS);
			stepStart++;   // the step it is in waits, for the client's drawing too
			return;
		}
		if (windHeld) {
			windHeld = false;
			sync();
		}
		LiftCycle.Step st = steps.get(step);
		burn(st);
		elapsed += 1 / 20.0;
		if (elapsed + 1e-9 >= st.seconds()) {
			if (finish(level, st)) {
				next(level, st);
			}
		}
		if (level.getGameTime() % 20 == 0) {
			setChanged();
		}
	}

	private void burn(LiftCycle.Step st) {
		load.setDemand(CraneKind.TOWER_STANDBY_WATTS + st.watts());
		if (kind().diesel()) {
			litres = Math.max(0, litres - kind().dieselPerSecond(st.watts()) / 20.0);
		}
	}

	/** A tandem follower keeps step with its lead, which picks, sets and holds for both. */
	private void follow(Level level) {
		CraneBlockEntity lead = partnerCrane();
		if (lead == null || lead.steps == null || !worldPosition.equals(lead.partner) || steps == null) {
			LiftCycle.Step last = steps == null ? null : steps.get(Math.min(step, steps.size() - 1));
			if (last != null) {
				hookDeg = ((last.to().degrees() % 360) + 360) % 360;
				hookRadius = last.to().radius();
				hookHeight = last.to().height();
			}
			steps = null;
			partner = null;
			leading = false;
			lift = -1;
			onHook = 0;
			carried = false;
			status = "between lifts";
			sync();
			return;
		}
		int was = step;
		step = Math.min(lead.step, steps.size() - 1);
		elapsed = lead.elapsed;
		stepStart = lead.stepStart;
		carried = lead.carried;
		if (powered()) {
			burn(steps.get(step));
		}
		if (step != was) {
			LiftCycle.Step done = steps.get(was);
			hookDeg = ((done.to().degrees() % 360) + 360) % 360;
			hookRadius = done.to().radius();
			hookHeight = done.to().height();
			status = describeStep();
			sync();
		}
		if (level.getGameTime() % 20 == 0) {
			status = lead.status.startsWith("NO ") || lead.status.contains("holding") ? "TANDEM: " + lead.status : describeStep();
			setChanged();
		}
	}

	/** What happens at the end of a step; false to stay in it (the chest is full). */
	private boolean finish(Level level, LiftCycle.Step st) {
		SiteMarkerBlockEntity m = marker();
		if (st.phase() == LiftCycle.Phase.RIG) {
			if (m == null || m.blueprint() == null) {
				abort("THE SITE IS GONE");
				return false;
			}
			Blueprint.Lift l = m.lifts().get(lift);
			if (dismantling) {
				takeDown(level, m, l);
				if (held.isEmpty()) {
					abort("");
					return false;
				}
			} else {
				Container c = chest();
				Map<Item, Integer> needs = needs(m, l);
				for (Map.Entry<Item, Integer> e : needs.entrySet()) {
					if (c == null || count(c, e.getKey()) < e.getValue()) {
						abort("WAITING: its chest was emptied");
						return false;
					}
				}
				for (Map.Entry<Item, Integer> e : needs.entrySet()) {
					take(c, e.getKey(), e.getValue());
					held.add(new ItemStack(e.getKey(), e.getValue()));
				}
			}
			carried = true;
			return true;
		}
		if (st.phase() == LiftCycle.Phase.SET) {
			if (dismantling) {
				Container c = chest();
				if (c == null) {
					status = noChest() + "; holding its load";
					return false;
				}
				held.replaceAll(stack -> put(c, stack));
				held.removeIf(ItemStack::isEmpty);
				if (!held.isEmpty()) {
					status = "CHEST FULL: holding its load over the chest";
					return false;
				}
				if (m != null) {
					m.recordWorker(worldPosition);
					if (partner != null) {
						m.recordWorker(partner);
					}
				}
			} else if (m != null && m.blueprint() != null) {
				Surveys.setLift(level, m.getBlockPos(), m.blueprint(), m.states(), m.lifts().get(lift));
				held.clear();
				m.recordSet(lift, worldPosition, partner);
				m.recount();
			}
			carried = false;
			return true;
		}
		return true;
	}

	private void next(Level level, LiftCycle.Step st) {
		hookDeg = ((st.to().degrees() % 360) + 360) % 360;
		hookRadius = st.to().radius();
		hookHeight = st.to().height();
		step++;
		elapsed = 0;
		stepStart = level.getGameTime();
		if (step >= steps.size()) {
			SiteMarkerBlockEntity m = marker();
			if (m != null) {
				m.release(lift, worldPosition);
				m.recount();
			}
			steps = null;
			lift = -1;
			onHook = 0;
			tandemTonnes = 0;
			partner = null;
			status = "between lifts";
		} else {
			status = describeStep();
		}
		sync();
	}

	private void abort(String why) {
		SiteMarkerBlockEntity m = marker();
		if (m != null) {
			m.release(lift, worldPosition);
		}
		returnHeld();
		steps = null;
		lift = -1;
		carried = false;
		onHook = 0;
		tandemTonnes = 0;
		partner = null;
		if (!why.isEmpty()) {
			status = why;
		}
		sync();
	}

	/** Takes what stands of lift {@code l} off the site onto the hook: a part as its item, each block as its item. */
	private void takeDown(Level level, SiteMarkerBlockEntity m, Blueprint.Lift l) {
		Blueprint b = m.blueprint();
		BlockPos origin = m.getBlockPos();
		if (l.isPart()) {
			Blueprint.Part p = b.parts().get(l.part());
			if (Surveys.partStands(level, origin, p)) {
				BlockPos main = origin.offset(p.x(), p.y(), p.z());
				BlockState state = level.getBlockState(main);
				held.add(new ItemStack(state.getBlock().asItem()));
				for (BlockPos cell : BigParts.cells(main, state)) {
					level.setBlock(cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
				}
			}
			return;
		}
		BlockState[] states = m.states();
		Map<Item, Integer> items = new LinkedHashMap<>();
		for (int i : l.blocks()) {
			Blueprint.Block k = b.blocks().get(i);
			if (Surveys.blockStands(level, origin, k, states[k.state()])) {
				BlockPos at = origin.offset(k.x(), k.y(), k.z());
				items.merge(level.getBlockState(at).getBlock().asItem(), 1, Integer::sum);
				level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
			}
		}
		items.forEach((item, n) -> held.add(new ItemStack(item, n)));
	}

	/** Puts what is on the hook back in the chest, or drops it there. */
	private void returnHeld() {
		if (level == null) {
			return;
		}
		Container c = chest();
		BlockPos at = chestPos() != null ? chestPos() : worldPosition;
		for (ItemStack stack : held) {
			ItemStack left = c == null ? stack : put(c, stack);
			if (!left.isEmpty()) {
				Containers.dropItemStack(level, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, left);
			}
		}
		held.clear();
		carried = false;
	}

	/** Sneak-click on the crawler or the ring crane: the next boom length, while it has no lift on. */
	public void nextBoom() {
		if (!kind().hasBoom()) {
			return;
		}
		if (steps != null) {
			status = "the boom changes only between lifts; " + status;
			return;
		}
		boom = (boom + 1) % kind().booms();
		hookRadius = Math.max(hookRadius, chart().minRadius());
		hookRadius = Math.min(hookRadius, chart().maxRadius());
		hookHeight = Math.min(hookHeight, kind().hookHeight(boom, hookRadius) - 1);
		laydown = null;
		nextScan = 0;
		status = String.format("boom set to %s: %.1f to %.1f m, %.1f t at %.1f m", kind().boomName(boom), chart().minRadius(), chart().maxRadius(),
				chart().capacity(chart().minRadius()), chart().minRadius());
		setChanged();
		sync();
	}

	// ---- wind, ground and collision (docs/questions/plant-crane-gaps.md, 1 to 3) -----------------------------------------------------
	/** The wind at its jib or boom head now, m/s: the world's weather 10 m up, rising with height. */
	public double wind() {
		if (level == null) {
			return 0;
		}
		double at10 = testWind >= 0 ? testWind : Siting.windAt10(level.getRainLevel(1.0F), level.getThunderLevel(1.0F));
		return Siting.windAt(at10, kind().headHeight(boom, Math.max(hookRadius, chart().minRadius())));
	}

	/** Why the wind stops it, or null. */
	private @Nullable String windStop() {
		double w = wind();
		if (w <= kind().windLimit() + 1e-9) {
			return null;
		}
		return String.format("WIND: %.1f m/s at its %s, over its %.0f m/s limit", w, kind() == CraneKind.TOWER ? "jib" : "boom head", kind().windLimit());
	}

	/** Why the ground under its crawlers or ring will not bear it, or null. */
	public @Nullable String softGround() {
		double need = kind().groundTonnesPerM2();
		if (need <= 0 || level == null) {
			return null;
		}
		List<BlockPos> cells = BigParts.cells(worldPosition, getBlockState());
		int bottom = Integer.MAX_VALUE;
		for (BlockPos cell : cells) {
			bottom = Math.min(bottom, cell.getY());
		}
		int soft = 0, total = 0;
		Siting.Ground worst = Siting.Ground.ROCK;
		for (BlockPos cell : cells) {
			if (cell.getY() != bottom) {
				continue;
			}
			total++;
			Siting.Ground g = CraneGround.at(level, cell.below());
			if (!g.bears(need)) {
				soft++;
				if (g.tonnesPerM2 < worst.tonnesPerM2) {
					worst = g;
				}
			}
		}
		if (soft == 0) {
			return null;
		}
		return String.format("SOFT GROUND: %d of %d m2 under its %s bear less than its %.1f t/m2 (%s, %.1f t/m2): set it on %s", soft, total,
				kind() == CraneKind.RING ? "ring" : "crawlers", need, worst.label, worst.tonnesPerM2,
				kind() == CraneKind.RING ? "rock, concrete or crane rail" : "gravel, rock or concrete");
	}

	/** What its jib or boom would strike on {@code plan}'s slews, as a BLOCKED readout, or null if its way is clear. */
	private @Nullable String struck(List<LiftCycle.Step> plan) {
		double reach = kind() == CraneKind.TOWER ? Siting.TOWER_JIB : kind().boomFoot()[0] + kind().boomLength(boom);
		Set<BlockPos> mine = new HashSet<>(BigParts.cells(worldPosition, getBlockState()));
		List<double[]> masts = new ArrayList<>();
		double[] c = centre();
		for (CraneBlockEntity other : SERVER) {
			if (other != this && !other.isRemoved() && other.level == level && other.kind() == CraneKind.TOWER) {
				double[] oc = other.centre();
				if (Math.hypot(oc[0] - c[0], oc[1] - c[1]) <= reach + Siting.TOWER_COUNTER_JIB + 5) {
					masts.add(new double[] {oc[0], oc[1], other.worldPosition.getY() + CraneKind.TOWER_HOOK_HEIGHT + Siting.TOWER_JIB_OVER_HOOK + Siting.TOWER_JIB_DEPTH});
				}
			}
		}
		for (LiftCycle.Step st : plan) {
			if (st.phase() != LiftCycle.Phase.TO_PICK && st.phase() != LiftCycle.Phase.SLEW) {
				continue;
			}
			for (double[] at : Siting.sweep(st.from(), st.to(), reach)) {
				String hit = strikes(at[0], at[1], mine, masts, c);
				if (hit != null) {
					return String.format("BLOCKED: its %s would strike %s %s", kind() == CraneKind.TOWER ? "jib" : "boom", hit,
							st.phase() == LiftCycle.Phase.TO_PICK ? "on its way to the pick" : "as it slews the load");
				}
			}
		}
		return null;
	}

	/** What the jib or boom strikes with the hook at {@code degrees}, {@code radius}: a block or another tower crane's mast, or null. */
	private @Nullable String strikes(double degrees, double radius, Set<BlockPos> mine, List<double[]> masts, double[] c) {
		double a = Math.toRadians(degrees);
		double sx = Math.sin(a), sz = -Math.cos(a);
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		int base = worldPosition.getY();
		if (kind() == CraneKind.TOWER) {
			double y0 = base + CraneKind.TOWER_HOOK_HEIGHT + Siting.TOWER_JIB_OVER_HOOK, y1 = y0 + Siting.TOWER_JIB_DEPTH;
			for (double d = -Siting.TOWER_COUNTER_JIB; d <= Siting.TOWER_JIB; d += 0.5) {
				if (Math.abs(d) < Siting.TOWER_MAST_HALF + Siting.CLEARANCE) {
					continue;   // its own slewing platform
				}
				double x = c[0] + sx * d, z = c[1] + sz * d;
				for (int y = (int) Math.floor(y0); y <= (int) Math.floor(y1); y++) {
					String hit = hit(at.set(Math.floor(x), y, Math.floor(z)), mine, masts, x, y, z);
					if (hit != null) {
						return hit;
					}
				}
			}
			return null;
		}
		double[] foot = kind().boomFoot();
		double angle = Math.toRadians(kind().boomAngle(boom, Math.max(radius, chart().minRadius())));
		for (double s = 2; s <= kind().boomLength(boom); s += 0.5) {
			double out = foot[0] + s * Math.cos(angle);
			double y = base + foot[1] + s * Math.sin(angle);
			double x = c[0] + sx * out, z = c[1] + sz * out;
			String hit = hit(at.set(Math.floor(x), Math.floor(y), Math.floor(z)), mine, masts, x, y, z);
			if (hit != null) {
				return hit;
			}
		}
		return null;
	}

	private @Nullable String hit(BlockPos at, Set<BlockPos> mine, List<double[]> masts, double x, double y, double z) {
		for (double[] mast : masts) {
			if (y <= mast[2] && Math.abs(x - mast[0]) <= Siting.TOWER_MAST_HALF + Siting.CLEARANCE && Math.abs(z - mast[1]) <= Siting.TOWER_MAST_HALF + Siting.CLEARANCE) {
				return String.format("the tower crane's mast at %.0f, %.0f", mast[0], mast[1]);
			}
		}
		if (mine.contains(at) || !level.isLoaded(at)) {
			return null;
		}
		BlockState state = level.getBlockState(at);
		if (state.isAir() || state.getCollisionShape(level, at).isEmpty()) {
			return null;
		}
		return String.format("%s at %d, %d, %d", BuiltInRegistries.BLOCK.getKey(state.getBlock()), at.getX(), at.getY(), at.getZ());
	}

	// ---- the ring crane's travel on its rails (docs/questions/plant-crane-gaps.md, 4) -------------------------------------------------
	/** The rail cell under its slewing centre, a metre below its ring. */
	private BlockPos centreRail() {
		double[] c = centre();
		return BlockPos.containing(c[0], worldPosition.getY() - 1, c[1]);
	}

	/**
	 * With nothing it can make (no site in reach, or the lifts out of reach), a ring crane standing on crane rail looks along them for
	 * the stop that brings the nearest site with work left closest in, and travels there.
	 */
	private void planTravel() {
		if (kind() != CraneKind.RING || level == null) {
			return;
		}
		BlockPos rail = centreRail();
		BlockState under = level.getBlockState(rail);
		if (!under.is(WildcatterBlocks.CRANE_RAIL)) {
			return;
		}
		boolean alongX = under.getValue(CraneRailBlock.AXIS) == Direction.Axis.X;
		Direction ahead = alongX ? Direction.EAST : Direction.SOUTH;
		int fwd = run(rail, ahead), back = run(rail, ahead.getOpposite());
		if (fwd == 0 && back == 0) {
			return;
		}
		double[] c = centre();
		int[] dir = {ahead.getStepX(), ahead.getStepZ()};
		double aim = chart().minRadius() + Siting.RING_TRAVEL_MARGIN;
		SiteMarkerBlockEntity target = null;
		int bestK = 0;
		double bestScore = Double.MAX_VALUE;
		for (SiteMarkerBlockEntity m : SiteMarkerBlockEntity.SERVER) {
			if (m.isRemoved() || m.getLevel() != level || m.blueprint() == null || (m.dismantle() ? m.done() == 0 : m.complete())) {
				continue;
			}
			double[] at = {m.getBlockPos().getX() + 0.5, m.getBlockPos().getZ() + 0.5};
			int k = Siting.bestStop(c, dir, back, fwd, at, aim, chart().maxRadius());
			if (k == 0) {
				continue;
			}
			double score = Math.abs(Math.hypot(at[0] - c[0] - dir[0] * k, at[1] - c[1] - dir[1] * k) - aim);
			if (score < bestScore) {
				bestScore = score;
				bestK = k;
				target = m;
			}
		}
		if (target == null) {
			return;
		}
		travelLeft = bestK;
		travelAlongX = alongX;
		travelled = 0;
		travelTo = siteName(target);
		status = travelStatus();
		setChanged();
	}

	/** How many metres of rail run on from {@code from} toward {@code d}, up to 512. */
	private int run(BlockPos from, Direction d) {
		int n = 0;
		BlockPos at = from;
		while (n < 512) {
			at = at.relative(d);
			if (!level.isLoaded(at) || !level.getBlockState(at).is(WildcatterBlocks.CRANE_RAIL)) {
				break;
			}
			n++;
		}
		return n;
	}

	private String travelStatus() {
		return String.format("TRAVELLING on its rails to bring %s within reach: %d m to go at %.0f m/h", travelTo, Math.abs(travelLeft), Siting.RING_TRAVEL_M_H);
	}

	private void travel(Level level) {
		if (!powered()) {
			status = noPower() + "; stopped on its rails";
			return;
		}
		double watts = Siting.RING_TRAVEL_WATTS;
		litres = Math.max(0, litres - kind().dieselPerSecond(watts) / 20.0);
		travelled += speedup * Siting.RING_TRAVEL_M_H / 3600 / 20;
		if (travelled < 1) {
			if (level.getGameTime() % 20 == 0) {
				status = travelStatus();
				setChanged();
			}
			return;
		}
		String why = moveOne();
		if (why != null) {
			status = why;
			travelled = 1;   // it tries the metre again
			if (why.startsWith("END")) {
				travelLeft = 0;
			}
			setChanged();
		}
	}

	/** Moves the whole crane a metre along its rails; null once moved (this block entity is then gone), or why it cannot. */
	private @Nullable String moveOne() {
		Direction d = travelAlongX ? (travelLeft > 0 ? Direction.EAST : Direction.WEST) : (travelLeft > 0 ? Direction.SOUTH : Direction.NORTH);
		if (!level.getBlockState(centreRail().relative(d)).is(WildcatterBlocks.CRANE_RAIL)) {
			return "END OF ITS RAILS: it stopped short of " + travelTo;
		}
		BlockState state = getBlockState();
		BlockPos to = worldPosition.relative(d);
		List<BlockPos> from = BigParts.cells(worldPosition, state);
		Set<BlockPos> old = new HashSet<>(from);
		for (BlockPos cell : BigParts.cells(to, state)) {
			if (old.contains(cell)) {
				continue;
			}
			if (!level.isLoaded(cell)) {
				return "TRAVEL HELD: the ground ahead is not loaded";
			}
			BlockState there = level.getBlockState(cell);
			if (!there.canBeReplaced()) {
				return String.format("TRAVEL BLOCKED: %s at %d, %d, %d in its path", BuiltInRegistries.BLOCK.getKey(there.getBlock()), cell.getX(), cell.getY(), cell.getZ());
			}
		}
		travelLeft -= Integer.signum(travelLeft);
		travelled -= 1;
		if (travelLeft == 0) {
			laydown = null;
			nextScan = 0;
		}
		status = travelLeft == 0 ? "arrived: looking for its next lift" : travelStatus();
		CompoundTag data = saveCustomOnly(level.registryAccess());
		Level here = level;
		moving = true;
		int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
		for (BlockPos cell : from) {
			if (!cell.equals(worldPosition)) {
				here.setBlock(cell, Blocks.AIR.defaultBlockState(), quiet);
			}
		}
		here.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), quiet);
		here.setBlock(to, state, Block.UPDATE_ALL);
		if (here.getBlockEntity(to) instanceof CraneBlockEntity moved) {
			moved.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, here.registryAccess(), data));
			moved.setChanged();
		}
		BigParts.grow(here, to, state);
		return null;
	}

	/** True while it travels on its rails. */
	public boolean travelling() {
		return travelLeft != 0;
	}

	/** For the game test: diesel in its tank, as if piped. */
	public void testFuel(double add) {
		litres = Math.min(kind().tankLitres(), litres + add);
		setChanged();
	}

	/** For the game test: the wind 10 m up, m/s, whatever the world's weather; negative to read the weather again. */
	public void testWind(double metresPerSecond) {
		testWind = metresPerSecond;
	}

	/** For the game test: its travel this many times faster than real. */
	public void testSpeedup(double times) {
		speedup = times;
		setChanged();
	}

	// ---- power: the tower crane's switch cabinet ------------------------------------------------------------------------------------
	private Direction terminalFace() {
		return getBlockState().getValue(BigPartBlock.FACING).getCounterClockWise();   // west when facing north
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return kind() == CraneKind.TOWER && face == terminalFace() && voltage == Voltage.INDUSTRIAL && role != Conductor.N;
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return cell.equals(worldPosition) && hasPort(face, role, voltage);
	}

	@Override
	public void build(Circuit circuit) {
		if (kind() == CraneKind.TOWER) {
			load.build(circuit, worldPosition, List.of(terminalFace()));
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		if (kind() == CraneKind.TOWER) {
			load.apply(circuit, ticks);
		}
	}

	// ---- the readout ------------------------------------------------------------------------------------------------------------------
	public String status() {
		return status;
	}

	@Override
	public Component inspect() {
		String title = kind().title() + (kind().hasBoom() ? ", " + kind().boomName(boom) + " boom" : "");
		String supply = kind().diesel() ? String.format("; diesel %.0f of %.0f L", litres, kind().tankLitres())
				: String.format("; %.0f V, %.0f kW", load.volts(), load.watts() / 1000);
		return Component.literal(title + ": " + status + supply);
	}

	// ---- for the renderer ---------------------------------------------------------------------------------------------------------------
	/** The hook where the step it is in began and where it ends, and how far through it is at {@code time} (game ticks). */
	public double[] hookAt(float time) {
		if (steps == null || step >= steps.size()) {
			return new double[] {hookDeg, hookRadius, hookHeight};
		}
		LiftCycle.Step st = steps.get(step);
		double f = st.seconds() <= 0 ? 1 : Math.clamp((time - stepStart) / 20.0 / st.seconds(), 0, 1);
		double deg = st.from().degrees() + LiftCycle.turn(st.from().degrees(), st.to().degrees()) * f;
		return new double[] {deg, st.from().radius() + (st.to().radius() - st.from().radius()) * f, st.from().height() + (st.to().height() - st.from().height()) * f};
	}

	/** The hook's world position at {@code time}: x, y (its highest point), z. */
	public double[] hookWorld(float time) {
		double[] h = hookAt(time);
		double[] c = centre();
		double a = Math.toRadians(h[0]);
		return new double[] {c[0] + Math.sin(a) * h[1], worldPosition.getY() + h[2], c[1] - Math.cos(a) * h[1]};
	}

	public boolean carrying() {
		return carried;
	}

	public @Nullable BlockPos site() {
		return site;
	}

	public int lift() {
		return lift;
	}

	/** The other crane of its tandem lift, or null. */
	public @Nullable BlockPos partner() {
		return partner;
	}

	/** True unless it follows another crane in a tandem lift. */
	public boolean leading() {
		return partner == null || leading;
	}

	// ---- saving and the client ----------------------------------------------------------------------------------------------------------
	private void sync() {
		setChanged();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		remote.save(output);
		output.putBoolean("grown", grown);
		output.putInt("boom", boom);
		output.putDouble("litres", litres);
		output.putInt("travelLeft", travelLeft);
		output.putBoolean("travelAlongX", travelAlongX);
		output.putDouble("travelled", travelled);
		output.putString("travelTo", travelTo);
		output.putDouble("speedup", speedup);
		output.putDouble("hookDeg", hookDeg);
		output.putDouble("hookRadius", hookRadius);
		output.putDouble("hookHeight", hookHeight);
		output.putBoolean("parked", parked);
		output.putString("status", status);
		output.putInt("heldCount", held.size());
		for (int i = 0; i < held.size(); i++) {
			output.store("held" + i, ItemStack.CODEC, held.get(i));
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		remote.load(input);
		grown = input.getBooleanOr("grown", false);
		boom = Math.clamp(input.getIntOr("boom", kind().firstBoom()), 0, kind().booms() - 1);
		litres = input.getDoubleOr("litres", 0);
		travelLeft = input.getIntOr("travelLeft", 0);
		travelAlongX = input.getBooleanOr("travelAlongX", false);
		travelled = input.getDoubleOr("travelled", 0);
		travelTo = input.getStringOr("travelTo", "");
		speedup = input.getDoubleOr("speedup", 1);
		hookDeg = input.getDoubleOr("hookDeg", 0);
		hookRadius = input.getDoubleOr("hookRadius", 0);
		hookHeight = input.getDoubleOr("hookHeight", 0);
		parked = input.getBooleanOr("parked", false);
		status = input.getStringOr("status", "");
		held.clear();
		for (int i = 0; i < input.getIntOr("heldCount", 0); i++) {
			input.read("held" + i, ItemStack.CODEC).ifPresent(held::add);
		}
		// the client's copy of the step it is in
		if (input.getIntOr("stepPhase", -1) >= 0 && level != null && level.isClientSide()) {
			LiftCycle.Hook from = new LiftCycle.Hook(input.getDoubleOr("fromDeg", 0), input.getDoubleOr("fromR", 0), input.getDoubleOr("fromH", 0));
			LiftCycle.Hook to = new LiftCycle.Hook(input.getDoubleOr("toDeg", 0), input.getDoubleOr("toR", 0), input.getDoubleOr("toH", 0));
			steps = List.of(new LiftCycle.Step(LiftCycle.Phase.values()[input.getIntOr("stepPhase", 0)], input.getDoubleOr("stepSeconds", 0), from, to,
					input.getBooleanOr("carried", false), 0));
			step = 0;
			stepStart = input.getLongOr("stepStart", 0);
			carried = input.getBooleanOr("carried", false);
			lift = input.getIntOr("lift", -1);
			site = input.getIntOr("siteY", Integer.MIN_VALUE) == Integer.MIN_VALUE ? null
					: new BlockPos(input.getIntOr("siteX", 0), input.getIntOr("siteY", 0), input.getIntOr("siteZ", 0));
			partner = input.getIntOr("partnerY", Integer.MIN_VALUE) == Integer.MIN_VALUE ? null
					: new BlockPos(input.getIntOr("partnerX", 0), input.getIntOr("partnerY", 0), input.getIntOr("partnerZ", 0));
			leading = input.getBooleanOr("leading", true);
		} else if (level != null && level.isClientSide()) {
			steps = null;
			carried = false;
			partner = null;
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("boom", boom);
		tag.putDouble("hookDeg", hookDeg);
		tag.putDouble("hookRadius", hookRadius);
		tag.putDouble("hookHeight", hookHeight);
		tag.putBoolean("parked", parked);
		if (steps != null && step < steps.size()) {
			LiftCycle.Step st = steps.get(step);
			tag.putInt("stepPhase", st.phase().ordinal());
			tag.putDouble("stepSeconds", st.seconds());
			tag.putDouble("fromDeg", st.from().degrees());
			tag.putDouble("fromR", st.from().radius());
			tag.putDouble("fromH", st.from().height());
			tag.putDouble("toDeg", st.to().degrees());
			tag.putDouble("toR", st.to().radius());
			tag.putDouble("toH", st.to().height());
			tag.putLong("stepStart", stepStart);
			tag.putBoolean("carried", carried);
			tag.putInt("lift", lift);
			if (site != null) {
				tag.putInt("siteX", site.getX());
				tag.putInt("siteY", site.getY());
				tag.putInt("siteZ", site.getZ());
			}
			if (partner != null) {
				tag.putInt("partnerX", partner.getX());
				tag.putInt("partnerY", partner.getY());
				tag.putInt("partnerZ", partner.getZ());
				tag.putBoolean("leading", leading);
			}
		}
		return tag;
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (moving) {
			return;   // it travels on: it has no lift on and nothing on its hook
		}
		SiteMarkerBlockEntity m = marker();
		if (m != null && lift >= 0 && leading()) {
			m.release(lift, worldPosition);
		}
		returnHeld();
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		SERVER.remove(this);
	}

	@Override
	public boolean grown() {
		return grown;
	}

	@Override
	public void markGrown() {
		grown = true;
	}
}
