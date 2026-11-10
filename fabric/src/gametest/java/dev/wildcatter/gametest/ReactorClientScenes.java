package dev.wildcatter.gametest;

import dev.wildcatter.Tuning;
import dev.wildcatter.Wildcatter;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.BoilerBlockEntity;
import dev.wildcatter.block.ContainmentMonitorBlockEntity;
import dev.wildcatter.block.ReactorVesselBlockEntity;
import dev.wildcatter.block.ReliefTankBlockEntity;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.fluid.Reactor;
import dev.wildcatter.multiblock.BigParts;
import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Phase 19's client scenes, for filming (WILDCATTER_ONLY=reactorContainment): the NeoForge {@code containment} and relief tank builds
 * seen from inside the closed building, lit by night vision. The PORVs lift into the relief tank until its disc bursts in a cloud;
 * the damaged core's hydrogen is burned by the powered igniter; with its power cut and the recombiner gone, the lower compartment blasts.
 */
final class ReactorClientScenes {
	private final ClientGameTestContext context;
	private final TestServerContext server;
	private final int y;
	private final int z0;

	ReactorClientScenes(ClientGameTestContext context, TestServerContext server, int y, int z0) {
		this.context = context;
		this.server = server;
		this.y = y;
		this.z0 = z0;
	}

	private BlockPos at(int x, int dy, int z) {
		return new BlockPos(x, y + dy, z0 + z);
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

	private void place(Block block, int x, int dy, int z) {
		on(level -> {
			BlockState state = block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
			level.setBlock(at(x, dy, z), state, 3);
			BigParts.grow(level, at(x, dy, z), state);
		});
	}

	private void put(String id, int x, int dy, int z) {
		on(level -> level.setBlock(at(x, dy, z), BuiltInRegistries.BLOCK.getValue(Wildcatter.id(id)).defaultBlockState(), 3));
	}

	private String read(int x, int dy, int z) {
		return get(level -> BigParts.entity(level, at(x, dy, z)) instanceof Inspectable i ? i.inspect().getString() : "(no readout)");
	}

	/** Shows {@code text} on the action bar, for the film. */
	private void say(String text) {
		on(level -> level.getServer().getPlayerList().getPlayers().forEach(p -> p.sendOverlayMessage(Component.literal(text))));
	}

	private void fill(int x1, int dy1, int z1, int x2, int dy2, int z2, String block) {
		server.runCommand("fill " + x1 + " " + (y + dy1) + " " + (z0 + z1) + " " + x2 + " " + (y + dy2) + " " + (z0 + z2) + " " + block);
	}

	private int frame;

	/** Waits {@code ticks}, a frame every 4 ticks (film-reactor-NNNN in the screenshots). */
	private void shoot(int ticks) {
		for (int t = 0; t < ticks; t += 4) {
			context.waitTicks(Math.min(4, ticks - t));
			context.takeScreenshot(String.format("film-reactor-%04d", frame++));
		}
	}

	void containment() {
		server.runCommand("effect give @p minecraft:night_vision 900 0 true");
		server.runCommand("effect give @p minecraft:resistance 900 4 true");
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand("tp @p 9 " + (y + 30) + " " + (z0 + 9) + " 0 90");
		context.waitTicks(20);
		fill(-4, -1, -4, 22, 16, 22, "minecraft:air");
		fill(-4, -1, -4, 22, -1, 22, "minecraft:stone");
		// the building: a hollow box of containment concrete, x 0-18, z 0-18, its floor at y and its roof 13 up; the hatch's and airlock's holes
		fill(0, 0, 0, 18, 13, 18, "wildcatter:containment_concrete hollow");
		fill(6, 1, 18, 9, 4, 18, "minecraft:air");
		fill(12, 1, 0, 13, 3, 0, "minecraft:air");
		place(WildcatterBlocks.EQUIPMENT_HATCH, 6, 1, 18);
		place(WildcatterBlocks.PERSONNEL_AIRLOCK, 12, 1, 0);
		place(WildcatterBlocks.REACTOR_VESSEL, 4, 1, 4);                  // x 4-6, z 4-6
		place(WildcatterBlocks.PRESSURIZER, 4, 1, 7);                     // x 4-5, z 7-8
		place(WildcatterBlocks.PRESSURIZER_RELIEF_TANK, 6, 1, 7);         // x 6-7, z 7-12
		place(WildcatterBlocks.HYDROGEN_RECOMBINER, 10, 1, 4);
		// the igniter's 120 V: a starter plant inside, hot wire to the igniter, neutral back
		server.runCommand("setblock 8 " + (y + 1) + " " + (z0 + 14) + " minecraft:campfire[lit=true]");
		put("boiler", 8, 2, 14);
		put("steam_turbine", 9, 2, 14);
		on(level -> ((BoilerBlockEntity) level.getBlockEntity(at(8, 2, 14))).addWater(4000));
		put("wool_wire_hot", 10, 2, 14);
		put("wool_wire_hot", 11, 2, 14);
		put("hydrogen_igniter", 12, 2, 14);
		for (int x = 9; x <= 12; x++) {
			put("wool_wire_neutral", x, 2, 15);
		}
		place(WildcatterBlocks.CONTAINMENT_MONITOR, 16, 1, 2);            // reads the air from (16, 1, 1)
		// inside, in the south-east corner, looking north-west at the vessel, the relief tank and the igniter's plant
		server.runCommand("tp @p 16.7 " + (y + 6) + " " + (z0 + 16.7) + " 124 22");
		context.waitTicks(100);
		// the screen recorder can miss the game's window, so the film is also kept as a run of frames (film-reactor-NNNN)
		Filming film = context.computeOnClient(client -> Filming.start(client, "reactorContainment"));
		try {
			String closed = get(level -> ((ContainmentMonitorBlockEntity) level.getBlockEntity(at(16, 1, 2))).refill(level));
			check(closed.contains("CLOSED"), "the building should read CLOSED: " + closed);
			double volume = get(level -> ((ContainmentMonitorBlockEntity) level.getBlockEntity(at(16, 1, 2))).volume());
			say(String.format("Containment monitor: CLOSED, %,.0f m3 free", volume));
			shoot(80);
			context.takeScreenshot("wildcatter-containment-closed");
			// the PORVs lift into the relief tank until its disc bursts
			Reactor r = get(level -> ((ReactorVesselBlockEntity) level.getBlockEntity(at(4, 1, 4))).reactor());
			on(level -> {
				((ReactorVesselBlockEntity) level.getBlockEntity(at(4, 1, 4))).setHotStandbyForTest(1500);
				((ReliefTankBlockEntity) level.getBlockEntity(at(6, 1, 7))).setWaterForTest(150);
			});
			boolean burst = false;
			for (int i = 0; i < 40 && !burst; i++) {
				on(level -> r.setPressureForTest(17.5));
				shoot(10);
				burst = get(level -> ((ReliefTankBlockEntity) level.getBlockEntity(at(6, 1, 7))).burst());
				say(get(level -> {
					ReliefTankBlockEntity t = (ReliefTankBlockEntity) level.getBlockEntity(at(6, 1, 7));
					return String.format("Relief tank: PORVs lifting; water %.1f C, %.2f MPa (disc %.1f)%s", t.waterC(), t.pressure(), Tuning.RELIEF_DISC_MPA, t.burst() ? ": DISC BURST" : "");
				}));
			}
			check(burst, "the relief tank's disc should burst: " + read(6, 1, 7));
			shoot(4);
			context.takeScreenshot("wildcatter-relief-tank-burst");
			shoot(60);
			// the core damaged by hook: its hydrogen, burned by the powered igniter
			on(level -> {
				r.setHistoryForTest(1.0);
				r.setSetpoint(-1);
				r.setInventoryForTest(0.25 * Tuning.RCS_WATER_KG);
				r.setFuelForTest(1150);
			});
			boolean burned = false;
			for (int i = 0; i < 160 && !burned; i++) {
				shoot(10);
				burned = get(level -> ((ContainmentMonitorBlockEntity) level.getBlockEntity(at(16, 1, 2))).burns() > 0);
				say(get(level -> {
					ContainmentMonitorBlockEntity m = (ContainmentMonitorBlockEntity) level.getBlockEntity(at(16, 1, 2));
					return String.format("Core damaged: hydrogen in the lower compartment %.1f %%; igniter burns %d", m.lowerH2() * 100, m.burns());
				}));
			}
			String burn = read(16, 1, 2);
			check(burned && !r.exploded(), "the igniter should burn the hydrogen with no blast: " + burn);
			say("The powered igniter burned the hydrogen at 4 %: no blast");
			shoot(2);
			context.takeScreenshot("wildcatter-igniter-burn");
			shoot(60);
			// the igniter's power cut, the recombiner gone: the lower compartment blasts (looking up at the vessel's head)
			server.runCommand("tp @p 16.7 " + (y + 6) + " " + (z0 + 16.7) + " 132 -8");
			on(level -> {
				level.setBlock(at(10, 2, 14), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
				level.setBlock(at(10, 1, 4), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
				level.setBlock(at(10, 2, 4), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
			});
			shoot(40);
			on(level -> {
				ContainmentMonitorBlockEntity m = (ContainmentMonitorBlockEntity) level.getBlockEntity(at(16, 1, 2));
				m.refill(level);
				m.addHydrogenForTest(Tuning.H2_KG_PER_M3 * Tuning.CONTAINMENT_LOWER_M3 * 0.05);
			});
			boolean blasted = false;
			for (int i = 0; i < 20 && !blasted; i++) {
				shoot(5);
				blasted = get(level -> ((ContainmentMonitorBlockEntity) level.getBlockEntity(at(16, 1, 2))).blasted());
			}
			context.takeScreenshot("wildcatter-containment-blast");
			String blast = read(16, 1, 2);
			say("Igniter unpowered, recombiner gone: HYDROGEN BLAST in the lower compartment");
			check(blasted && r.exploded(), "unpowered, the lower compartment should blast: " + blast);
			shoot(100);
			System.out.println("[wildcatter-test] reactorContainment ok: " + closed + " | " + burn + " | " + blast);
		} finally {
			film.stop();
		}
	}
}
