package dev.wildcatter.control;

import com.mojang.serialization.Codec;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.multiblock.BigParts;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A rack of modules on the network, with its own 24 V and its own control cables. The PLC with this rack on its network scans it as more slots: rack 1's slot 0 is
 * slot 12, so {@code %IX192.0}, and each rack takes 12 slots more (docs/multiblocks/plc.md).
 */
public class RemoteIoDropBlockEntity extends BlockEntity implements Inspectable, BigPartEntity {
	public static final int RACKS = 4;
	private static final int WALK_TICKS = 10;
	private final PlcModuleItem.Type[] modules = new PlcModuleItem.Type[PlcCabinetBlockEntity.MODULE_SLOTS];
	private int rack = 1;
	private boolean grown;
	private ControlNet.Found net = new ControlNet.Found(List.of(), false, null);

	public RemoteIoDropBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.REMOTE_IO_DROP_ENTITY, pos, state);
	}

	public int rack() {
		return rack;
	}

	public void setRack(int value) {
		rack = Math.max(1, Math.min(RACKS, value));
		setChanged();
	}

	public void nextRack() {
		rack = rack % RACKS + 1;
		setChanged();
	}

	public PlcModuleItem.Type module(int slot) {
		return modules[slot];
	}

	public boolean live() {
		return net.supplyLive();
	}

	public List<BlockPos> devices() {
		return net.devices();
	}

	/** Why not, or null when it went in. A CPU and a COMM module have no place here: the adapter is built in. */
	public String seat(PlcModuleItem.Type type) {
		if (type.cpu != null || type == PlcModuleItem.Type.COMM) {
			return "A drop has its adapter built in, and takes I/O modules only";
		}
		for (int i = 0; i < modules.length; i++) {
			if (modules[i] == null) {
				modules[i] = type;
				setChanged();
				return null;
			}
		}
		return "The rail is full";
	}

	public ItemStack unseatLast() {
		for (int i = modules.length - 1; i >= 0; i--) {
			if (modules[i] != null) {
				for (var item : BuiltInRegistries.ITEM) {
					if (item instanceof PlcModuleItem module && module.type() == modules[i]) {
						modules[i] = null;
						setChanged();
						return new ItemStack(item);
					}
				}
			}
		}
		return ItemStack.EMPTY;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, RemoteIoDropBlockEntity drop) {
		if (level.getGameTime() % WALK_TICKS == 0) {
			drop.net = ControlNet.walk(level, BigParts.cells(pos, state));
		}
	}

	@Override
	public Component inspect() {
		StringBuilder out = new StringBuilder("Remote I/O drop, rack " + rack + ": ");
		out.append(live() ? "24 V OK" : "NO 24 V").append(", ").append(NetworkNet.plcs(level, BigParts.cells(worldPosition, getBlockState())).size()).append(" PLC(s) on its network, ").append(net.devices().size()).append(" devices reached; ");
		for (int i = 0; i < modules.length; i++) {
			if (modules[i] != null) {
				out.append(i).append(": ").append(modules[i]).append(" ");
			}
		}
		return Component.literal(out.toString());
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
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level != null) {
			ItemStack out;
			while (!(out = unseatLast()).isEmpty()) {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), out);
			}
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("grown", grown);
		output.putInt("rack", rack);
		output.store("modules", Codec.STRING.listOf(), java.util.Arrays.stream(modules).map(m -> m == null ? "" : m.name()).toList());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		rack = input.getIntOr("rack", 1);
		List<String> rail = input.read("modules", Codec.STRING.listOf()).orElse(List.of());
		for (int i = 0; i < modules.length && i < rail.size(); i++) {
			modules[i] = rail.get(i).isEmpty() ? null : PlcModuleItem.Type.valueOf(rail.get(i));
		}
	}
}
