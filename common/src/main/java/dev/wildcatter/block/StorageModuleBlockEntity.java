package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SpentFuel;
import dev.wildcatter.multiblock.BigParts;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A horizontal concrete storage module on the pad (docs/multiblocks/reactor-extensions.md, 19b): 1 m walls round one welded, helium
 * filled canister of up to 12 assemblies, cooled by air. The assemblies stay records (never items) and go on decaying here; it radiates
 * through its walls (one solid block's shielding, a thousandth). It holds one canister and cannot be broken while it does.
 */
public class StorageModuleBlockEntity extends PlantUnitBlockEntity {
	private static final Map<ResourceKey<Level>, Set<BlockPos>> MODULES = new HashMap<>();

	public static void clearAll() {
		MODULES.clear();
	}

	public static Set<BlockPos> modules(Level level) {
		return MODULES.getOrDefault(level.dimension(), Set.of());
	}

	private final List<SpentFuel.Assembly> records = new ArrayList<>();

	public StorageModuleBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.STORAGE_MODULE_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	public List<SpentFuel.Assembly> records() {
		return records;
	}

	/** The transfer cask's delivery: a sealed canister. */
	public void receive(Level level, List<SpentFuel.Assembly> canister) {
		records.addAll(canister);
		setChanged();
		level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
	}

	public double watts() {
		double w = 0;
		for (SpentFuel.Assembly a : records) {
			w += a.watts();
		}
		return w;
	}

	/** The middle of the module, where its canister lies. */
	public Vec3 centre() {
		List<BlockPos> cells = level == null ? List.of(worldPosition) : BigParts.cellsAt(level, worldPosition);
		double x = 0, y = 0, z = 0;
		for (BlockPos c : cells) {
			x += c.getX() + 0.5;
			y += c.getY() + 0.5;
			z += c.getZ() + 0.5;
		}
		return new Vec3(x / cells.size(), y / cells.size(), z / cells.size());
	}

	public static void tick(Level level, BlockPos pos, BlockState state, StorageModuleBlockEntity module) {
		MODULES.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos.immutable());
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL == 0 && !module.records.isEmpty()) {
			double dt = seconds();
			for (SpentFuel.Assembly a : module.records) {
				a.decay(dt);
			}
			module.setChanged();
		}
	}

	@Override
	public Component inspect() {
		return Component.literal(records.isEmpty() ? "Horizontal storage module (1 m concrete walls): EMPTY; link a cask loading station to it (sneak-click the station with the Power Meter)"
				: String.format("Horizontal storage module (1 m concrete walls): a welded canister of %d assemblies, decay heat %.2f kW, cooled by air; it cannot be broken while loaded",
						records.size(), watts() / 1000));
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (level != null && !level.isClientSide()) {
			java.util.Optional.ofNullable(MODULES.get(level.dimension())).ifPresent(set -> set.remove(worldPosition));
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		SpentFuelPoolBlockEntity.saveRecords(output, "m", records);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		SpentFuelPoolBlockEntity.loadRecords(input, "m", records);
	}
}
