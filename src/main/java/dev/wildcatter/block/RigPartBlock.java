package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.Footprint;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * One part of the 1,000 hp land rig at true size (docs/multiblocks/land-rig.md). The substructure goes down first over the hole
 * and runs the rig; every other part snaps into its place when clicked on its floor or on the ground near it (see {@link LandRig}).
 * The top drive is an item clicked into the mast.
 */
public class RigPartBlock extends BigPartBlock {
	public enum Part {
		SUBSTRUCTURE("rig_substructure", substructure()),
		MAST("rig_mast", mast()),
		DRAWWORKS("rig_drawworks", Footprint.box(6, 3, 3, 0, 0)),
		MUD_PUMP("mud_pump", Footprint.box(5, 3, 3, 0, 0)),
		MUD_TANKS("mud_tanks", Footprint.box(3, 3, 12, 0, 0)),
		DRIVE_HOUSE("rig_drive_house", Footprint.box(12, 4, 4, 0, 0));

		private final String id;
		private final Footprint footprint;

		Part(String id, Footprint footprint) {
			this.id = id;
			this.footprint = footprint;
		}

		public String id() {
			return id;
		}
	}

	/** Box-on-box, 9 x 6 x 9 from its main cell at the north-west corner: two side boxes 2 wide from the ground up, the floor on top. */
	private static Footprint substructure() {
		List<Footprint.Cell> cells = new ArrayList<>();
		cells.add(new Footprint.Cell(0, 0, 0));
		for (int y = 0; y < 6; y++) {
			for (int z = 0; z < 9; z++) {
				for (int x = 0; x < 9; x++) {
					boolean box = x <= 1 || x >= 7;
					if ((y == 5 || box) && (x != 0 || y != 0 || z != 0)) {
						cells.add(new Footprint.Cell(x, y, z));
					}
				}
			}
		}
		return Footprint.of(cells.toArray(Footprint.Cell[]::new));
	}

	/** The mast from its north-west leg: four legs 42 m tall round the top drive's path, the crown across the top. */
	private static Footprint mast() {
		List<Footprint.Cell> cells = new ArrayList<>();
		cells.add(new Footprint.Cell(0, 0, 0));
		for (int y = 0; y < 42; y++) {
			for (int[] leg : new int[][] {{0, 0}, {2, 0}, {0, 2}, {2, 2}}) {
				if (y != 0 || leg[0] != 0 || leg[1] != 0) {
					cells.add(new Footprint.Cell(leg[0], y, leg[1]));
				}
			}
		}
		for (int z = 0; z < 3; z++) {
			for (int x = 0; x < 3; x++) {
				cells.add(new Footprint.Cell(x, 42, z));
			}
		}
		return Footprint.of(cells.toArray(Footprint.Cell[]::new));
	}

	private final Part part;

	public RigPartBlock(Properties properties, Part part) {
		super(properties, part.footprint, part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	public BlockPlaceContext snap(BlockPlaceContext context) {
		return part == Part.SUBSTRUCTURE ? context : LandRig.snap(context, this);
	}

	/** Facing the player, or the rig's way when it goes into its place. */
	@Override
	public BlockState placementState(BlockPlaceContext context) {
		PartLayout.Slot slot = part == Part.SUBSTRUCTURE ? null : LandRig.slotFor(context.getLevel(), context.getClickedPos(), this);
		return defaultBlockState().setValue(FACING, slot != null ? slot.facing() : context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case SUBSTRUCTURE -> new RigSubstructureBlockEntity(pos, state);
			case DRIVE_HOUSE -> new RigDriveHouseBlockEntity(pos, state);
			default -> new RigPartBlockEntity(pos, state);
		};
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return switch (part) {
			case SUBSTRUCTURE -> level.isClientSide() ? null : createTickerHelper(type, WildcatterBlocks.RIG_SUBSTRUCTURE_ENTITY, RigSubstructureBlockEntity::tick);
			case DRIVE_HOUSE -> null;
			default -> level.isClientSide() ? null : createTickerHelper(type, WildcatterBlocks.RIG_PART_ENTITY, RigPartBlockEntity::tick);
		};
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		BlockPos main = main(pos, state);
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, main, player);
		if (read != null) {
			return read;
		}
		if (stack.is(WildcatterItems.TOP_DRIVE)) {   // into the mast (or straight to the substructure)
			if (!level.isClientSide()) {
				RigSubstructureBlockEntity rig = controller(level, main, state);
				if (rig == null) {
					player.sendOverlayMessage(Component.literal("The top drive goes into a land rig's mast: build the rig first"));
				} else if (rig.installTopDrive()) {
					if (!player.getAbilities().instabuild) {
						stack.shrink(1);
					}
					player.sendOverlayMessage(Component.literal("Top drive hung in the mast"));
				} else {
					player.sendOverlayMessage(rig.inspect());
				}
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			RigSubstructureBlockEntity rig = controller(level, main(pos, state), state);
			if (part == Part.SUBSTRUCTURE && rig != null) {
				player.sendOverlayMessage(rig.toggle());   // the driller's console
			} else if (level.getBlockEntity(main(pos, state)) instanceof dev.wildcatter.energy.Inspectable i) {
				player.sendOverlayMessage(i.inspect());
			} else if (rig != null) {
				player.sendOverlayMessage(rig.inspect());
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** The substructure running the rig this part (main cell {@code main}) belongs to, or null. */
	static RigSubstructureBlockEntity controller(Level level, BlockPos main, BlockState state) {
		BlockPos origin = state.getBlock() == WildcatterBlocks.RIG_SUBSTRUCTURE ? main
				: LandRig.controllerOf(level, state.getBlock(), main, state.getValue(FACING));
		return origin != null && level.getBlockEntity(origin) instanceof RigSubstructureBlockEntity rig ? rig : null;
	}
}
