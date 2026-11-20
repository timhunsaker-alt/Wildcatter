package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Frasch wellhead's controller (docs/multiblocks/oilfield-follow-ons.md, 23d). It finds the Frasch water heater and the lift air
 * compressor by steel pipe on its other sides; with water at 150 C or hotter and air, it melts the native sulfur within 6 blocks of the
 * hole's bottom in the caprock, nearest first, each block giving 1.7 t (80 % of its sulfur) and turning to barren calcite, and lifts it as
 * molten sulfur at up to 2 t/h, offered on the pipe at the side it faces (for the acid plant's sulfur furnace, or a sulfur vat). The water
 * put down must come out: a disposal wellhead on another hole into the same caprock within 16 blocks is its bleed water well; without one
 * the rate falls to a third over 10 minutes (NO BLEED). NO HOT WATER, NO AIR, CAPROCK SPENT.
 */
public class FraschWellheadBlockEntity extends BlockEntity implements FluidNode, Inspectable {
	private double molten;      // litres held for the pipe
	private double melted;      // kg of the block now melting
	private double lifted;      // kg lifted in all
	private double bleed = 1.0; // the rate's share left by the bleed water
	private double rate;        // kg/h the last interval
	private @Nullable BlockPos melting;
	private String status = "idle";

	public FraschWellheadBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.FRASCH_WELLHEAD_ENTITY, pos, state);
	}

	private Direction out() {
		return getBlockState().getValue(FraschWellheadBlock.FACING);
	}

	private static boolean caprock(BlockState state) {
		return state.is(Blocks.CALCITE) || state.is(OreBlocks.block(Ore.SULFUR));
	}

	private List<Lines.Face> inlets() {
		List<Lines.Face> faces = new ArrayList<>();
		for (Direction d : Direction.Plane.HORIZONTAL) {
			if (d != out()) {
				faces.add(new Lines.Face(worldPosition, d));
			}
		}
		return faces;
	}

	private <T> @Nullable T onPipe(Level level, Class<T> kind) {
		for (Lines.Line line : Lines.lines(level, inlets())) {
			for (BlockPos pipe : line.pipes()) {
				for (Direction side : Direction.values()) {
					if (dev.wildcatter.multiblock.BigParts.entity(level, pipe.relative(side)) instanceof BlockEntity e && kind.isInstance(e)) {
						return kind.cast(e);
					}
				}
			}
		}
		return null;
	}

	/** A disposal wellhead on another hole into the same caprock within 16 blocks: the bleed water well. */
	boolean bleedWell(Level level) {
		int r = Tuning.BLEED_REACH;
		for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-r, -4, -r), worldPosition.offset(r, 4, r))) {
			if (level.getBlockState(p).getBlock() instanceof DisposalWellheadBlock) {
				BlockPos bottom = Caverns.bottom(level, p.immutable());
				if (bottom != null && caprock(level.getBlockState(bottom))) {
					return true;
				}
			}
		}
		return false;
	}

	/** The nearest native sulfur block in reach of the hole's bottom, or null. */
	private @Nullable BlockPos nearestSulfur(Level level, BlockPos bottom) {
		BlockPos best = null;
		double bestD = Double.MAX_VALUE;
		int r = Tuning.FRASCH_REACH;
		for (BlockPos p : BlockPos.betweenClosed(bottom.offset(-r, -r, -r), bottom.offset(r, r, r))) {
			double d = p.distSqr(bottom);
			if (d <= r * r && d < bestD && level.getBlockState(p).is(OreBlocks.block(Ore.SULFUR))) {
				best = p.immutable();
				bestD = d;
			}
		}
		return best;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, FraschWellheadBlockEntity head) {
		if (!(level instanceof ServerLevel server) || (level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		head.work(server);
		head.setChanged();
	}

	private void work(ServerLevel level) {
		double dt = Tuning.WELL_INTERVAL / 20.0;
		rate = 0;
		BlockPos bottom = Caverns.bottom(level, worldPosition);
		if (bottom == null || !caprock(level.getBlockState(bottom))) {
			status = "NO HOLE: it must stand on a cased hole into a salt dome's caprock";
			return;
		}
		if (melting == null || !level.getBlockState(melting).is(OreBlocks.block(Ore.SULFUR))) {
			melting = nearestSulfur(level, bottom);
			melted = 0;
		}
		if (melting == null) {
			status = "CAPROCK SPENT: no native sulfur within " + Tuning.FRASCH_REACH + " blocks of the hole";
			return;
		}
		// the bleed water well: without one the formation fills and the rate falls to a third in 10 minutes
		boolean bleeds = bleedWell(level);
		double step = (1 - Tuning.BLEED_FLOOR) * Tuning.WELL_INTERVAL / Tuning.BLEED_FALL_TICKS;
		bleed = bleeds ? Math.min(1, bleed + step) : Math.max(Tuning.BLEED_FLOOR, bleed - step);
		double room = (Tuning.FRASCH_SULFUR_KGS * Tuning.PLANT_HOLD_SECONDS * 4) / Tuning.MOLTEN_SULFUR_KG_PER_L - molten;
		if (room <= 1e-6) {
			status = "FULL: its molten sulfur has nowhere to go (pipe the side it faces to a sulfur vat or the acid plant's furnace)";
			return;
		}
		LiftAirCompressorBlockEntity air = onPipe(level, LiftAirCompressorBlockEntity.class);
		FraschWaterHeaterBlockEntity heater = onPipe(level, FraschWaterHeaterBlockEntity.class);
		if (heater == null) {
			status = "NO HOT WATER: pipe a Frasch water heater to its side";
			return;
		}
		double kgS = Math.min(Tuning.FRASCH_SULFUR_KGS * dt * bleed, room * Tuning.MOLTEN_SULFUR_KG_PER_L);
		double water = heater.supply(level, kgS * Tuning.FRASCH_WATER_PER_SULFUR);
		if (water <= 1e-9) {
			status = String.format("NO HOT WATER: the heater's water is at %.0f C (it must be over %.0f C)", heater.temp(), Tuning.FRASCH_MIN_C);
			return;
		}
		double airShare = air == null ? 0 : air.air(level);
		if (airShare <= 0) {
			status = air == null ? "NO AIR: pipe a lift air compressor to its side" : "NO AIR: its compressor is not running";
			return;
		}
		kgS = Math.min(kgS, water / Tuning.FRASCH_WATER_PER_SULFUR) * Math.min(1, airShare);
		melt(level, kgS);
		rate = kgS / dt * 3600;
		if (level.getGameTime() % 200 < Tuning.WELL_INTERVAL) {
			dev.wildcatter.quest.QuestEvents.milestone(level, worldPosition, dev.wildcatter.quest.Milestones.FRASCH_FLOWING);
		}
		status = String.format("lifting %.2f t/h of molten sulfur%s", rate / 1000, bleeds ? " (bleed water to its disposal well)"
				: String.format(" (NO BLEED: the rate at %.0f %%; a disposal wellhead into the same caprock within %d blocks)", bleed * 100, Tuning.BLEED_REACH));
	}

	/** {@code kg} of sulfur melted out of the block in hand and lifted; a block given up turns to barren calcite. */
	private void melt(Level level, double kg) {
		double perBlock = Ore.SULFUR.kgPerBlock() * Tuning.FRASCH_YIELD;
		melted += kg;
		lifted += kg;
		molten += kg / Tuning.MOLTEN_SULFUR_KG_PER_L;
		if (melted >= perBlock && melting != null) {
			level.setBlock(melting, Blocks.CALCITE.defaultBlockState(), 3);
			melting = null;
			melted = 0;
		}
	}

	/** Game test hook: as if {@code kg} more had melted out of the block in hand (it turns to calcite at 1.7 t). */
	public void testMelt(double kg) {
		if (level != null) {
			double perBlock = Ore.SULFUR.kgPerBlock() * Tuning.FRASCH_YIELD;
			melted += Math.min(kg, perBlock);
			if (melted >= perBlock && melting != null) {
				level.setBlock(melting, Blocks.CALCITE.defaultBlockState(), 3);
				melting = null;
				melted = 0;
			}
		}
	}

	/** Game test hook: as if 10 minutes had passed without a bleed well. */
	public void testBleedSpent() {
		bleed = Tuning.BLEED_FLOOR;
	}

	public double rate() {
		return rate;
	}

	public double bleed() {
		return bleed;
	}

	public double lifted() {
		return lifted;
	}

	@Override
	public @Nullable Product offers(BlockPos cell, Direction side) {
		return side == out() && molten > 1e-6 ? Product.MOLTEN_SULFUR : null;
	}

	@Override
	public double available(BlockPos cell, Direction side, Product product, Pull pull) {
		return side == out() && product == Product.MOLTEN_SULFUR ? molten : 0;
	}

	@Override
	public double take(BlockPos cell, Direction side, Product product, double max, Pull pull) {
		if (side != out() || product != Product.MOLTEN_SULFUR) {
			return 0;
		}
		double got = Math.min(max, molten);
		molten -= got;
		setChanged();
		return got;
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Frasch wellhead (three strings): %s; %.0f L of molten sulfur held, %.1f t lifted", status, molten, lifted / 1000));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("molten", molten);
		output.putDouble("melted", melted);
		output.putDouble("lifted", lifted);
		output.putDouble("bleed", bleed);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		molten = input.getDoubleOr("molten", 0);
		melted = input.getDoubleOr("melted", 0);
		lifted = input.getDoubleOr("lifted", 0);
		bleed = input.getDoubleOr("bleed", 1.0);
	}
}
