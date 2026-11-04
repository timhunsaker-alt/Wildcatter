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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A crane at work (docs/multiblocks/tower-crane.md, crawler-crane.md): it finds the nearest site marker in its reach, takes the
 * marker's next lift (or, in DISMANTLE, its last standing one), checks it against its chart, its height under the hook, its chest
 * and its power, and then runs the lift's steps ({@link LiftCycle}) at its real speeds. The tower crane is one frequency-converter
 * load on the industrial voltage at the west face of its switch cabinet; the crawler burns diesel from its own tank, piped to any
 * cell of its crawlers. The client draws it from the step it is in.
 */
public class CraneBlockEntity extends BlockEntity implements CircuitPart, Inspectable, BigPartEntity {
	private final DriveLoad load = new DriveLoad(Voltage.INDUSTRIAL, Tuning.LAND_RIG_POWER_FACTOR, 0);
	private boolean grown;
	private int boom = 3;   // the crawler's 32 m boom to start
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
	/** The lift's radius at the site: what its chart is read at. */
	private double liftRadius;
	private final List<ItemStack> held = new ArrayList<>();
	private String status = "starting";
	private String doing = "";

	public CraneBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CRANE_ENTITY, pos, state);
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

	/** A lift's box in the site, in blueprint cells: x0, y0, z0, x1, y1, z1. */
	private static int[] box(Blueprint b, Blueprint.Lift lift) {
		if (lift.isPart()) {
			Blueprint.Part p = b.parts().get(lift.part());
			return new int[] {p.x0(), p.y0(), p.z0(), p.x1(), p.y1(), p.z1()};
		}
		int[] out = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
		for (int i : lift.blocks()) {
			Blueprint.Block k = b.blocks().get(i);
			out[0] = Math.min(out[0], k.x());
			out[1] = Math.min(out[1], k.y());
			out[2] = Math.min(out[2], k.z());
			out[3] = Math.max(out[3], k.x());
			out[4] = Math.max(out[4], k.y());
			out[5] = Math.max(out[5], k.z());
		}
		return out;
	}

	// ---- the chest ----------------------------------------------------------------------------------------------------------------
	/** A chest, barrel or hopper against any cell of the base, or null. */
	public @Nullable BlockPos chestPos() {
		if (level == null) {
			return null;
		}
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
		if (kind() == CraneKind.CRAWLER) {
			return litres > 0.01;
		}
		return !load.undervoltage() && load.volts() >= Voltage.INDUSTRIAL.lineToLine() * Tuning.LAND_RIG_UNDERVOLTAGE;
	}

	private String noPower() {
		return kind() == CraneKind.CRAWLER ? "NO DIESEL: its tank is dry, pipe diesel to its crawlers"
				: String.format("NO POWER: %.0f V at its switch cabinet", load.volts());
	}

	/** Looks for its next lift and starts it, or says what it waits for. */
	private void findWork() {
		SiteMarkerBlockEntity m = findSite();
		site = m == null ? null : m.getBlockPos();
		if (m == null) {
			status = String.format("NO SITE within %.0f m", chart().maxRadius());
			return;
		}
		Blueprint b = m.blueprint();
		List<Blueprint.Lift> lifts = m.lifts();
		if (b == null || lifts == null) {
			status = "THE SITE'S BLUEPRINT IS UNREADABLE";
			return;
		}
		boolean down = m.dismantle();
		int index = -1;
		if (down) {
			for (int i = lifts.size() - 1; i >= 0 && index < 0; i--) {
				if (m.liftAnyStands(lifts.get(i)) && !m.claimedByOther(i, worldPosition)) {
					index = i;
				}
			}
		} else {
			for (int i = 0; i < lifts.size() && index < 0; i++) {
				if (!m.liftStands(lifts.get(i)) && !m.claimedByOther(i, worldPosition)) {
					index = i;
				}
			}
		}
		if (index < 0) {
			status = String.format("IDLE: %s is %s", siteName(m), down ? "cleared" : "built");
			return;
		}
		Blueprint.Lift l = lifts.get(index);
		String what = m.describe(l);
		int[] box = box(b, l);
		BlockPos origin = m.getBlockPos();
		double sx = origin.getX() + (box[0] + box[3] + 1) / 2.0;
		double sz = origin.getZ() + (box[2] + box[5] + 1) / 2.0;
		double setHeight = origin.getY() + box[1] - worldPosition.getY();
		double radius = radiusTo(sx, sz);
		double tonnes = Masses.onHook(l.tonnes());
		LoadChart chart = chart();
		String boomText = kind() == CraneKind.CRAWLER ? String.format(" on the %d m boom", LoadChart.CRAWLER_BOOMS[boom]) : "";
		if (!chart.reaches(radius)) {
			status = String.format("OUT OF REACH: %s at %.0f m (%.1f to %.1f m%s)", what, radius, chart.minRadius(), chart.maxRadius(), boomText);
			return;
		}
		double capacity = chart.capacity(radius);
		if (tonnes > capacity) {
			status = String.format("TOO HEAVY: %s, %.1f t on the hook, chart %.1f t at %.0f m%s%s", what, tonnes, capacity, chart.readAt(radius), boomText,
					kind() == CraneKind.CRAWLER ? ": a shorter boom lifts more" : "");
			return;
		}
		BlockPos chestAt = chestPos();
		if (chestAt == null) {
			status = "NO CHEST: set a chest, barrel or hopper against its base";
			return;
		}
		double pickDeg = degreesTo(chestAt.getX() + 0.5, chestAt.getZ() + 0.5);
		double pickRadius = chart.minRadius();
		double pickHeight = chestAt.getY() + 1 - worldPosition.getY();
		double siteTop = origin.getY() + b.sizeY() - worldPosition.getY();
		double loadHeight = box[4] - box[1] + 1;
		double clearance = Math.max(siteTop, Math.max(pickHeight, setHeight)) + 2;
		double under = Math.min(kind().hookHeight(boom, radius), kind().hookHeight(boom, pickRadius));
		if (clearance + loadHeight > under) {
			status = String.format("TOO HIGH: the site needs %.0f m under the hook (%.1f m%s)", clearance + loadHeight, under, boomText);
			return;
		}
		if (!down) {
			BlockPos blocked = m.inTheWay(l);
			if (blocked != null) {
				status = String.format("IN THE WAY: %s at %d, %d, %d", BuiltInRegistries.BLOCK.getKey(level.getBlockState(blocked).getBlock()),
						blocked.getX(), blocked.getY(), blocked.getZ());
				return;
			}
			Container c = chest();
			StringBuilder missing = new StringBuilder();
			for (Map.Entry<Item, Integer> e : needs(m, l).entrySet()) {
				int short_ = e.getValue() - count(c, e.getKey());
				if (short_ > 0) {
					missing.append(missing.isEmpty() ? "" : ", ").append(short_).append(" x ").append(new ItemStack(e.getKey()).getHoverName().getString());
				}
			}
			if (!missing.isEmpty()) {
				status = "WAITING: " + missing + " in its chest";
				return;
			}
		}
		if (!powered()) {
			status = noPower();
			return;
		}
		if (!m.claim(index, worldPosition)) {
			return;
		}
		LiftCycle.Hook at = new LiftCycle.Hook(hookDeg, hookRadius, hookHeight);
		LiftCycle.Hook chestHook = new LiftCycle.Hook(pickDeg, pickRadius, pickHeight);
		LiftCycle.Hook siteHook = new LiftCycle.Hook(degreesTo(sx, sz), radius, setHeight);
		steps = LiftCycle.plan(kind(), boom, at, down ? siteHook : chestHook, down ? chestHook : siteHook, clearance, tonnes);
		lift = index;
		dismantling = down;
		step = 0;
		elapsed = 0;
		onHook = tonnes;
		liftRadius = radius;
		carried = false;
		stepStart = level.getGameTime();
		doing = String.format("%s %s, lift %d of %d", down ? "DISMANTLING" : "BUILDING", siteName(m), index + 1, lifts.size());
		status = describeStep();
		sync();
	}

	private String describeStep() {
		if (steps == null) {
			return status;
		}
		SiteMarkerBlockEntity m = marker();
		LiftCycle.Step st = steps.get(step);
		String what = m != null && m.lifts() != null && lift < m.lifts().size() ? m.describe(m.lifts().get(lift)) : "its load";
		return String.format("%s: %s %s, %.1f t on the hook, chart %.1f t at %.0f m", doing, st.phase().label, what, onHook,
				chart().capacity(liftRadius), chart().readAt(liftRadius));
	}

	// ---- each tick --------------------------------------------------------------------------------------------------------------------
	public static void tick(Level level, BlockPos pos, BlockState state, CraneBlockEntity crane) {
		crane.step(level);
	}

	private void step(Level level) {
		if (!parked) {
			hookDeg = (getBlockState().getValue(BigPartBlock.FACING).toYRot() + 180) % 360;
			hookRadius = chart().minRadius();
			hookHeight = Math.max(2, kind().hookHeight(boom, hookRadius) - 6);
			parked = true;
			sync();
		}
		if (kind() == CraneKind.CRAWLER && level.getGameTime() % Tuning.WELL_INTERVAL == 0) {
			double room = CraneKind.CRAWLER_TANK_LITRES - litres;
			if (room >= 1) {
				litres += Lines.pull(level, Lines.faces(BigParts.cells(worldPosition, getBlockState())), Product.DIESEL, room, FluidNode.Pull.MACHINE, null);
			}
		}
		if (!held.isEmpty() && steps == null) {
			returnHeld();   // a load left on the hook (the world was saved mid-lift) goes back to the chest
		}
		if (steps == null) {
			load.setDemand(site != null ? CraneKind.TOWER_STANDBY_WATTS : 0);
			if ((level.getGameTime() + worldPosition.hashCode()) % 20 == 0) {
				findWork();
				setChanged();
			}
			return;
		}
		if (!powered()) {
			status = noPower() + "; holding " + (carried ? "its load on its brakes" : "where it is");
			load.setDemand(CraneKind.TOWER_STANDBY_WATTS);
			return;
		}
		LiftCycle.Step st = steps.get(step);
		load.setDemand(CraneKind.TOWER_STANDBY_WATTS + st.watts());
		if (kind() == CraneKind.CRAWLER) {
			litres = Math.max(0, litres - CraneKind.litresPerSecond(st.watts()) / 20.0);
		}
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
					status = "NO CHEST: set a chest, barrel or hopper against its base; holding its load";
					return false;
				}
				held.replaceAll(stack -> put(c, stack));
				held.removeIf(ItemStack::isEmpty);
				if (!held.isEmpty()) {
					status = "CHEST FULL: holding its load over the chest";
					return false;
				}
			} else if (m != null && m.blueprint() != null) {
				Surveys.setLift(level, m.getBlockPos(), m.blueprint(), m.states(), m.lifts().get(lift));
				held.clear();
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

	/** Sneak-click on the crawler: the next boom length, while it has no lift on. */
	public void nextBoom() {
		if (kind() != CraneKind.CRAWLER) {
			return;
		}
		if (steps != null) {
			status = "the boom changes only between lifts; " + status;
			return;
		}
		boom = (boom + 1) % LoadChart.CRAWLER_BOOMS.length;
		hookRadius = Math.max(hookRadius, chart().minRadius());
		hookRadius = Math.min(hookRadius, chart().maxRadius());
		hookHeight = Math.min(hookHeight, kind().hookHeight(boom, hookRadius) - 1);
		status = String.format("boom set to %d m: %.1f to %.1f m, %.1f t at %.1f m", LoadChart.CRAWLER_BOOMS[boom], chart().minRadius(), chart().maxRadius(),
				chart().capacity(chart().minRadius()), chart().minRadius());
		setChanged();
		sync();
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
		String title = kind().title() + (kind() == CraneKind.CRAWLER ? String.format(", %d m boom", LoadChart.CRAWLER_BOOMS[boom]) : "");
		String supply = kind() == CraneKind.CRAWLER ? String.format("; diesel %.0f of %.0f L", litres, CraneKind.CRAWLER_TANK_LITRES)
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

	public boolean carrying() {
		return carried;
	}

	public @Nullable BlockPos site() {
		return site;
	}

	public int lift() {
		return lift;
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
		output.putBoolean("grown", grown);
		output.putInt("boom", boom);
		output.putDouble("litres", litres);
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
		grown = input.getBooleanOr("grown", false);
		boom = Math.clamp(input.getIntOr("boom", 3), 0, LoadChart.CRAWLER_BOOMS.length - 1);
		litres = input.getDoubleOr("litres", 0);
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
		} else if (level != null && level.isClientSide()) {
			steps = null;
			carried = false;
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
		}
		return tag;
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		SiteMarkerBlockEntity m = marker();
		if (m != null && lift >= 0) {
			m.release(lift, worldPosition);
		}
		returnHeld();
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
