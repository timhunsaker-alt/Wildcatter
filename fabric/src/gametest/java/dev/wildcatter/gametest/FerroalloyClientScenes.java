package dev.wildcatter.gametest;

import dev.wildcatter.Tuning;
import dev.wildcatter.Wildcatter;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.block.BaggingStationBlockEntity;
import dev.wildcatter.block.CapacitorBankBlockEntity;
import dev.wildcatter.block.FurnaceTransformerBlockEntity;
import dev.wildcatter.block.GasTurbineBlockEntity;
import dev.wildcatter.block.GensetControlPanelBlockEntity;
import dev.wildcatter.block.GensetPartBlockEntity;
import dev.wildcatter.block.GensetSkid;
import dev.wildcatter.block.StorageTankBlockEntity;
import dev.wildcatter.block.SubmergedArcFurnaceBlockEntity;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Phase 17a's client scene, for filming (run alone with WILDCATTER_ONLY=ferroalloys): the NeoForge scene {@code ferroalloys}'s build, laid
 * out round an origin (0, up, z0). A 45 MW aeroderivative through its GSU and a 60 MVA substation transformer feeds the furnace
 * transformer and the submerged arc furnace on 12.47 kV; the furnace runs up to 8 MW at PF 0.80; the capacitor bank closes and the feeder's
 * power factor rises past 0.93; the hearth is brought to full by hook and the furnace taps into its bagging station, its fume and sparks at
 * the taphole. Screenshots of each readout and of the whole plant.
 */
final class FerroalloyClientScenes {
	private final ClientGameTestContext context;
	private final TestServerContext server;
	private final int y;
	private final int up;
	private final int z0;

	FerroalloyClientScenes(ClientGameTestContext context, TestServerContext server, int y, int up, int z0) {
		this.context = context;
		this.server = server;
		this.y = y;
		this.up = up;
		this.z0 = z0;
	}

	// ---- helpers (x, z relative to the origin; dy above up) ---------------------------------------------------------------------------
	private BlockPos at(int x, int dy, int z) {
		return new BlockPos(x, up + dy, z0 + z);
	}

	private void on(Consumer<ServerLevel> work) {
		server.computeOnServer(srv -> {
			work.accept(srv.overworld());
			return null;
		});
	}

	private <T> T get(Function<ServerLevel, T> work) {
		return server.computeOnServer(srv -> work.apply(srv.overworld()));
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}

	private void clear(int x1, int z1, int x2, int z2, int height) {
		for (int zz = z1; zz <= z2; zz += 8) {   // /fill takes at most 32,768 blocks
			int top = Math.min(zz + 7, z2);
			server.runCommand("fill " + x1 + " " + (y - 1) + " " + (z0 + zz) + " " + x2 + " " + (y + height) + " " + (z0 + top) + " minecraft:air");
			server.runCommand("fill " + x1 + " " + (y - 1) + " " + (z0 + zz) + " " + x2 + " " + (y - 1) + " " + (z0 + top) + " minecraft:stone");
		}
	}

	private void place(Block block, int x, int dy, int z, Direction facing) {
		on(level -> {
			BlockState state = block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
			level.setBlock(at(x, dy, z), state, 3);
			BigParts.grow(level, at(x, dy, z), state);
		});
	}

	private void place(Block block, int x, int z, Direction facing) {
		place(block, x, 0, z, facing);
	}

	private void put(String id, int x, int dy, int z) {
		on(level -> level.setBlock(at(x, dy, z), BuiltInRegistries.BLOCK.getValue(Wildcatter.id(id)).defaultBlockState(), 3));
	}

	private BlockEntity entity(ServerLevel level, int x, int dy, int z) {
		return BigParts.entity(level, at(x, dy, z));
	}

	private String read(int x, int dy, int z) {
		return get(level -> entity(level, x, dy, z) instanceof Inspectable i ? i.inspect().getString() : "(no readout)");
	}

	private void genset(int x, int z) {
		on(level -> GensetSkid.build(level, at(x, 0, z), Direction.NORTH, false));
	}

	private void run(int x, int z) {
		on(level -> {
			if (level.getBlockEntity(GensetSkid.base(at(x, 0, z), Direction.NORTH, false)) instanceof GensetPartBlockEntity tank) {
				tank.setFuel(2000);
			}
			((GensetControlPanelBlockEntity) level.getBlockEntity(at(x, 0, z))).setWantRun(true);
		});
	}

	private void show(int x, int dy, int z, String tp, String shot) {
		server.runCommand(tp);
		context.waitTicks(3);
		on(level -> {
			if (entity(level, x, dy, z) instanceof Inspectable i) {
				for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
					player.sendOverlayMessage(i.inspect());
				}
			}
		});
		context.waitTicks(10);
		context.takeScreenshot(shot);
		context.waitTicks(40);   // a moment to read it on film
	}

	private String tp(int x, int dy, int z, int yaw, int pitch) {
		return "tp @p " + x + " " + (up + dy) + " " + (z0 + z) + " " + yaw + " " + pitch;
	}

	private void log(String text) {
		System.out.println("[wildcatter-test] " + text);
	}

	// ---- ferroalloys --------------------------------------------------------------------------------------------------------------
	void ferroalloys() {
		Direction north = Direction.NORTH;
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand(tp(64, 16, 6, 140, 30));
		context.waitTicks(20);
		clear(-2, -24, 68, 22, 18);
		Filming film = null;
		try {
			film = context.computeOnClient(client -> Filming.start(client, "ferroalloys"));
			// the peaker and its substation
			place(WildcatterBlocks.AERO_TURBINE, 20, 0, north);
			place(WildcatterBlocks.AERO_INLET_HOUSE, 20, -5, north);
			place(WildcatterBlocks.AERO_EXHAUST_STACK, 20, 14, north);
			place(WildcatterBlocks.AERO_GENERATOR, 25, 1, north);
			place(WildcatterBlocks.GEN_BREAKER_3000, 26, 8, north);
			place(WildcatterBlocks.GSU_60, 36, 8, north);
			place(WildcatterBlocks.SUBSTATION_TRANSFORMER_60, 36, -6, north);
			place(WildcatterBlocks.STOCK_TANK, 14, 4, north);
			put("gas_line", 18, 0, 6);
			put("gas_line", 19, 0, 6);
			for (int x = 26; x <= 37; x++) {
				put("bus_duct_gen", x, 0, 10);
			}
			put("bus_duct_gen", 36, 0, 9);
			for (int z = -10; z <= 4; z++) {
				put("cable_hv", 36, 6, z);
			}
			genset(10, 2);
			place(WildcatterBlocks.UNIT_SUBSTATION, 14, 2, Direction.SOUTH);
			for (int x = 15; x <= 19; x++) {
				put("cable_mv", x, 0, 2);
			}
			for (int x = 36; x <= 43; x++) {
				put("cable_dist", x, 0, -5);
			}
			for (int z = -20; z <= -6; z++) {
				put("cable_dist", 43, 0, z);
			}
			// the furnace, its transformer and the capacitor bank; the bagging station at the taphole on its own diesel set
			place(WildcatterBlocks.FURNACE_TRANSFORMER, 44, -20, north);
			place(WildcatterBlocks.SUBMERGED_ARC_FURNACE, 48, -20, north);
			place(WildcatterBlocks.CAPACITOR_BANK, 44, -15, north);
			place(WildcatterBlocks.BAGGING_STATION, 56, -16, Direction.SOUTH);
			genset(64, -12);
			for (int z = -16; z <= -12; z++) {
				put("cable_industrial", 65, 0, z);
			}
			for (int x = 57; x <= 64; x++) {
				put("cable_industrial", x, 0, -16);
			}
			on(level -> {
				level.setBlock(at(51, 8, -17), Blocks.HOPPER.defaultBlockState(), 3);
				HopperBlockEntity hopper = (HopperBlockEntity) level.getBlockEntity(at(51, 8, -17));
				hopper.setItem(0, new ItemStack(OreBlocks.MINERALS.get(Ore.CHROMITE), 8));
				hopper.setItem(1, new ItemStack(WildcatterItems.MET_COKE, 64));
				hopper.setItem(2, new ItemStack(Items.QUARTZ, 64));
				hopper.setItem(3, new ItemStack(WildcatterItems.GREEN_ANODE, 4));
				((StorageTankBlockEntity) entity(level, 14, 0, 4)).testFill(Product.KEROSENE, 40_000);
				((BaggingStationBlockEntity) entity(level, 56, 0, -16)).slots().set(0, new ItemStack(WildcatterItems.EMPTY_BULK_BAG, 16));
				((CapacitorBankBlockEntity) entity(level, 44, 0, -15)).setClosed(false);
			});
			context.waitTicks(30);
			run(10, 2);
			run(64, -12);
			context.waitTicks(260);
			on(level -> ((GasTurbineBlockEntity) entity(level, 20, 0, 0)).setWantRun(true));
			context.waitTicks(200);
			on(level -> ((GasTurbineBlockEntity) entity(level, 20, 0, 0)).skipStartForTest(false));
			context.waitTicks(100);
			on(level -> ((GasTurbineBlockEntity) entity(level, 20, 0, 0)).skipRampForTest());
			// the electrodes go down: 10 % a second
			server.runCommand(tp(60, 12, -2, 145, 35));
			context.waitTicks(300);
			double mw = get(level -> ((FurnaceTransformerBlockEntity) entity(level, 44, 0, -20)).load().watts()) / 1e6;
			double pfOpen = get(level -> ((FurnaceTransformerBlockEntity) entity(level, 44, 0, -20)).feederPowerFactor());
			check(mw > 7.5 && mw < 8.5, "the furnace should draw 7.5-8.5 MW: " + mw + " | " + read(48, 0, -20));
			show(48, 0, -20, tp(58, 10, -6, 145, 30), "wildcatter-ferroalloys-furnace");
			show(44, 0, -20, tp(40, 3, -23, -60, 10), "wildcatter-ferroalloys-transformer");
			// the capacitor bank closed
			on(level -> ((CapacitorBankBlockEntity) entity(level, 44, 0, -15)).setClosed(true));
			context.waitTicks(40);
			double pf = get(level -> ((FurnaceTransformerBlockEntity) entity(level, 44, 0, -20)).feederPowerFactor());
			check(pf > 0.93 && pfOpen < 0.85, "the bank should lift the feeder's PF past 0.93: " + pfOpen + " to " + pf);
			show(44, 0, -15, tp(40, 3, -9, -140, 15), "wildcatter-ferroalloys-capacitor-bank");
			show(44, 0, -20, tp(40, 3, -23, -60, 10), "wildcatter-ferroalloys-feeder-pf");
			// the tap: the hearth brought to full
			on(level -> ((SubmergedArcFurnaceBlockEntity) entity(level, 48, 0, -20)).testFillHearth(Tuning.SAF_HEARTH_KG - 0.2));
			server.runCommand(tp(61, 3, -21, 55, 15));
			context.waitTicks(60);
			context.takeScreenshot("wildcatter-ferroalloys-tap");
			context.waitTicks(200);
			int bags = get(level -> {
				int n = 0;
				for (ItemStack s : ((BaggingStationBlockEntity) entity(level, 56, 0, -16)).slots()) {
					if (s.is(WildcatterItems.FERROCHROME_BAG)) {
						n += s.getCount();
					}
				}
				return n;
			});
			check(bags >= 1, "the tap should fill bags of HC ferrochrome: " + bags + " | " + read(56, 0, -16));
			show(56, 0, -16, tp(61, 3, -21, 55, 15), "wildcatter-ferroalloys-bags");
			server.runCommand(tp(64, 14, 0, 145, 30));
			context.waitTicks(20);
			context.takeScreenshot("wildcatter-ferroalloys");
			context.waitTicks(40);
			log("ferroalloys ok: " + String.format("%.2f MW, feeder PF %.2f open, %.2f closed, %d bags", mw, pfOpen, pf, bags) + " | " + read(48, 0, -20));
		} finally {
			if (film != null) {
				film.stop();
			}
		}
	}
}
