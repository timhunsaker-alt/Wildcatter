package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.abs;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.air;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.check;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.entity;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.floor;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.genset;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.gensetPanel;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.near;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.place;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.put;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.read;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.start;

import dev.wildcatter.Rules;
import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterComponents;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.block.BaggingStationBlockEntity;
import dev.wildcatter.block.BessCabinetBlockEntity;
import dev.wildcatter.block.BessContainerBlockEntity;
import dev.wildcatter.block.BessInverterBlockEntity.Preset;
import dev.wildcatter.block.BessPcsBlockEntity;
import dev.wildcatter.block.BessPlantControllerBlockEntity;
import dev.wildcatter.block.CellLineBlockEntity;
import dev.wildcatter.block.GasTurbineBlockEntity;
import dev.wildcatter.block.GraphiteFurnaceBlockEntity;
import dev.wildcatter.block.GraphiteLineBlockEntity;
import dev.wildcatter.block.LfpKilnBlockEntity;
import dev.wildcatter.block.MultiCableBlockEntity;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Phase 22's scenes (docs/multiblocks/grid-batteries.md, "Game tests"). Every part faces north at y 2, its main cell at its north-west
 * bottom corner; test sources stand in for nitrogen, phosphoric acid, HF and ethylene.
 * <ul>
 * <li>{@code cell_plant} (22a): the graphite line, furnace (on the peaker's distribution feeder), LFP kiln and cell line each make their
 * product; bags drop at the stations; a module (brought near done by hook) comes out; the kiln's nitrogen cut, NO NITROGEN.</li>
 * <li>{@code bess_block} (22b): a utility block (PCS, two containers of 96 modules, MV skid) on the peaker's 12.47 kV (11 kV IEC) feeder,
 * run by a plant controller over control cable: charging and discharging at 2.5 MW, the cells' energy following the losses; the feeder
 * cut, LOSS OF GRID.</li>
 * <li>{@code bess_peak_shave} (22b): a cabinet on a 480 V genset island holds the set under its limit while heaters load it.</li>
 * <li>{@code bess_forming} (22c): a cabinet set to GRID FORMING black-starts a dead 480 V bus and carries its heaters; a genset synchronises
 * and closes onto it; a bolted fault holds the cabinet at its current limit and trips it on UNDERVOLTAGE.</li>
 * <li>{@code bess_runaway} (22c): with the rule on, fire at a container sets it off, the container touching it catches, the one a block apart
 * does not, and the burned one is gone; with the rule off, fire does nothing. A container broken keeps its modules and health in its item.</li>
 * </ul>
 */
final class BessScenes {
	private BessScenes() {
	}

	private static final int U = 2;
	private static final Direction NORTH = Direction.NORTH;

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> scenes = new LinkedHashMap<>();
		scenes.put("cell_plant", BessScenes::cellPlant);
		scenes.put("bess_block", BessScenes::bessBlock);
		scenes.put("bess_peak_shave", BessScenes::peakShave);
		scenes.put("bess_forming", BessScenes::forming);
		scenes.put("bess_runaway", BessScenes::runaway);
		return scenes;
	}

	static void await(GameTestHelper helper, int max, BooleanSupplier done, Runnable then) {
		if (max <= 0 || done.getAsBoolean()) {
			then.run();
			return;
		}
		helper.runAfterDelay(20, () -> await(helper, max - 20, done, then));
	}

	private static int count(BaggingStationBlockEntity station, net.minecraft.world.item.Item item) {
		int n = 0;
		for (ItemStack st : station.slots()) {
			if (st.is(item)) {
				n += st.getCount();
			}
		}
		return n;
	}

	// ---- cell_plant (22a) ---------------------------------------------------------------------------------------------------------------
	static void cellPlant(GameTestHelper helper) {
		String scene = "cell_plant";
		floor(helper, -44, -32, 44, 24);
		GasTurbineBlockEntity gt = ChemicalScenes.peaker(helper, -40, -20);
		place(helper, WildcatterBlocks.GRAPHITE_LINE, 0, U, 0, NORTH);          // x 0-2, z 0-5, box west (-1, 0)
		place(helper, WildcatterBlocks.BAGGING_STATION, 4, U, 1, Direction.SOUTH);   // x 3-4, z 0-1, against its east face; box (5, 1)
		place(helper, WildcatterBlocks.GRAPHITE_FURNACE, 10, U, 0, NORTH);      // x 10-13, z 0-7, box west (9, 0)
		place(helper, WildcatterBlocks.BAGGING_STATION, 15, U, 3, Direction.SOUTH);  // x 14-15, z 2-3; box (16, 3)
		place(helper, WildcatterBlocks.LFP_KILN, 20, U, 0, NORTH);              // x 20-22, z 0-11, box west (19, 0)
		place(helper, WildcatterBlocks.BAGGING_STATION, 24, U, 3, Direction.SOUTH);  // x 23-24, z 2-3; box (25, 3)
		place(helper, WildcatterBlocks.CELL_LINE, 30, U, 0, NORTH);             // x 30-33, z 0-15, box west (29, 0)
		// pipe
		ChemicalScenes.run(helper, "gas_line", new int[] {14, 0});
		ChemicalScenes.source(helper, 15, 0, Product.NITROGEN, 1.0);                 // the furnace's nitrogen
		ChemicalScenes.run(helper, "gas_line", new int[] {23, 0});
		TestGasSource.SourceEntity acid = ChemicalScenes.source(helper, 24, 0, Product.MERCHANT_ACID, 5.0);   // merchant phosphoric acid
		ChemicalScenes.run(helper, "gas_line", new int[] {23, 11});
		ChemicalScenes.source(helper, 24, 11, Product.NITROGEN, 1.0);                // the kiln's nitrogen
		ChemicalScenes.run(helper, "gas_line", new int[] {34, 0});
		ChemicalScenes.source(helper, 35, 0, Product.ANHYDROUS_HF, 1.0);
		ChemicalScenes.run(helper, "gas_line", new int[] {34, 2});
		ChemicalScenes.source(helper, 35, 2, Product.ETHYLENE, 1.0);
		// power: the peaker's 12.47 kV east to the furnace's box; 480 V sets for the rest
		ChemicalScenes.run(helper, "cable_dist", new int[] {-4, -25}, new int[] {9, -25}, new int[] {9, 0});
		// one 480 V bus on three sets in parallel (they synchronise as they start)
		genset(helper, -6, U, 8);
		ChemicalScenes.run(helper, "cable_industrial", new int[] {-5, 8}, new int[] {-5, 0}, new int[] {-1, 0});                       // the line's box
		ChemicalScenes.run(helper, "cable_industrial", new int[] {-5, 7}, new int[] {5, 7}, new int[] {5, 1});                         // station 1
		ChemicalScenes.run(helper, "cable_industrial", new int[] {5, 7}, new int[] {9, 7}, new int[] {9, 10}, new int[] {17, 10}, new int[] {17, 3}, new int[] {16, 3});   // station 2
		ChemicalScenes.run(helper, "cable_industrial", new int[] {17, 10}, new int[] {19, 10});
		genset(helper, 18, U, 16);
		ChemicalScenes.run(helper, "busway_industrial", new int[] {19, 16}, new int[] {19, 0});                                        // the kiln's box: 481 A, busway
		ChemicalScenes.run(helper, "cable_industrial", new int[] {19, 12}, new int[] {26, 12}, new int[] {26, 3}, new int[] {25, 3});  // station 3
		genset(helper, 28, U, 18);
		ChemicalScenes.run(helper, "busway_industrial", new int[] {29, 18}, new int[] {29, 0});                                        // the cell line's box: 361 A, busway
		ChemicalScenes.run(helper, "cable_industrial", new int[] {26, 12}, new int[] {28, 12});
		GraphiteLineBlockEntity line = entity(helper, 0, U, 0, GraphiteLineBlockEntity.class);
		GraphiteFurnaceBlockEntity furnace = entity(helper, 10, U, 0, GraphiteFurnaceBlockEntity.class);
		LfpKilnBlockEntity kiln = entity(helper, 20, U, 0, LfpKilnBlockEntity.class);
		CellLineBlockEntity cells = entity(helper, 30, U, 0, CellLineBlockEntity.class);
		// the feeds go in once all three sets are up, so no set takes a 400 kW start alone
		Runnable feed = () -> {
			line.slots().set(0, new ItemStack(OreBlocks.MINERALS.get(Ore.GRAPHITE), 4));
			furnace.slots().set(0, new ItemStack(WildcatterItems.SPHERICAL_GRAPHITE_BAG, 2));
			kiln.slots().set(0, new ItemStack(Items.IRON_INGOT, 8));
			kiln.slots().set(1, new ItemStack(WildcatterItems.LI2CO3_BAG, 2));
			cells.slots().set(0, new ItemStack(WildcatterItems.LFP_CATHODE_BAG, 1));
			cells.slots().set(1, new ItemStack(WildcatterItems.PURIFIED_GRAPHITE_BAG, 1));
			cells.slots().set(2, new ItemStack(WildcatterItems.LI2CO3_BAG, 1));
			cells.slots().set(3, new ItemStack(Items.COPPER_INGOT, 2));
			cells.slots().set(4, new ItemStack(WildcatterItems.ALUMINIUM_INGOT, 2));
		};
		BaggingStationBlockEntity[] stations = {entity(helper, 4, U, 1, BaggingStationBlockEntity.class), entity(helper, 15, U, 3, BaggingStationBlockEntity.class),
				entity(helper, 24, U, 3, BaggingStationBlockEntity.class)};
		for (BaggingStationBlockEntity st : stations) {
			st.slots().set(0, new ItemStack(WildcatterItems.EMPTY_BULK_BAG, 4));
		}
		start(helper, -6, U, 8, () -> start(helper, 18, U, 16, () -> start(helper, 28, U, 18, () -> ChemicalScenes.startPeaker(helper, -40, -20, gt, () -> {
			feed.run();
			await(helper, 1200,
				() -> line.making() > 0 && furnace.making() > 0 && kiln.making() > 0 && cells.progress() > 0, () -> {
			String chain = " | sets " + read(helper, -6, U, 8) + " / " + read(helper, 18, U, 16) + " / " + read(helper, 28, U, 18) + " | line " + read(helper, 0, U, 0) + " | furnace " + read(helper, 10, U, 0) + " | kiln " + read(helper, 20, U, 0) + " | cell line " + read(helper, 30, U, 0);
			log(scene, "running:" + chain);
			check(helper, near(line.making(), Tuning.GRAPHITE_LINE_KGH, 0.15), "the graphite line should make about 400 kg/h: " + line.making() + chain);
			check(helper, near(furnace.making(), Tuning.GRAPHITE_FURNACE_KGH, 0.15), "the furnace should purify about 360 kg/h: " + furnace.making() + chain);
			check(helper, near(kiln.making(), Tuning.LFP_KILN_KGH, 0.15), "the kiln should make about 700 kg/h of cathode: " + kiln.making() + chain);
			check(helper, read(helper, 10, U, 0).contains(Voltage.DISTRIBUTION.label()), "the furnace should be on the distribution tier: " + chain);
			line.testAdd(Tuning.BULK_BAG_KG);
			furnace.testAdd(Tuning.BULK_BAG_KG);
			kiln.testAdd(Tuning.BULK_BAG_KG);
			check(helper, cells.progress() >= 0, "the cell line should have taken its HF and ethylene and started a module: " + chain);
			cells.testProgress(0.98);
			await(helper, 600, () -> count(stations[0], WildcatterItems.SPHERICAL_GRAPHITE_BAG) >= 1 && count(stations[1], WildcatterItems.PURIFIED_GRAPHITE_BAG) >= 1
					&& count(stations[2], WildcatterItems.LFP_CATHODE_BAG) >= 1 && cells.modules() >= 1, () -> {
				String bags = " | stations " + read(helper, 4, U, 1) + " / " + read(helper, 15, U, 3) + " / " + read(helper, 24, U, 3) + " | " + read(helper, 30, U, 0);
				check(helper, count(stations[0], WildcatterItems.SPHERICAL_GRAPHITE_BAG) >= 1, "a spherical graphite bag should drop" + bags);
				check(helper, count(stations[1], WildcatterItems.PURIFIED_GRAPHITE_BAG) >= 1, "a purified graphite bag should drop" + bags);
				check(helper, count(stations[2], WildcatterItems.LFP_CATHODE_BAG) >= 1, "a cathode bag should drop" + bags);
				check(helper, cells.modules() >= 1, "the cell line should make a module" + bags);
				log(scene, "bags and a module:" + bags + String.format(" | acid taken %.0f L", acid.taken));
				air(helper, 23, U, 11);
				air(helper, 24, U, 11);
				await(helper, 200, () -> read(helper, 20, U, 0).contains("NO NITROGEN"), () -> {
					String cut = read(helper, 20, U, 0);
					check(helper, cut.contains("NO NITROGEN"), "the kiln with its nitrogen cut should read NO NITROGEN: " + cut);
					log(scene, "ok: " + cut);
					helper.succeed();
				});
			});
		});
		}))));
	}

	// ---- bess_block (22b) --------------------------------------------------------------------------------------------------------------
	static void bessBlock(GameTestHelper helper) {
		String scene = "bess_block";
		floor(helper, -44, -32, 30, 12);
		GasTurbineBlockEntity gt = ChemicalScenes.peaker(helper, -40, -20);
		place(helper, WildcatterBlocks.BESS_MV_SKID, 10, U, 0, NORTH);         // x 10-12, z 0-2: LV west of (10, 0), HV east of (12, 0)
		place(helper, WildcatterBlocks.BESS_PCS, 10, U, -3, NORTH);            // x 10-12, z -3..-1, touching the skid
		place(helper, WildcatterBlocks.BESS_CONTAINER, 10, U, -9, NORTH);      // x 10-12, z -9..-4, touching the PCS's north
		place(helper, WildcatterBlocks.BESS_CONTAINER, 13, U, -6, NORTH);      // x 13-15, z -6..-1, touching its east
		place(helper, WildcatterBlocks.BESS_PLANT_CONTROLLER, 2, U, -6, NORTH);   // x 2-4, z -6..-4
		for (int x = 5; x <= 9; x++) {
			put(helper, "control_cable", x, U, -3);                               // the controller to the PCS
		}
		put(helper, "control_cable", 5, U, -4);
		ChemicalScenes.run(helper, "cable_dist", new int[] {-4, -25}, new int[] {20, -25}, new int[] {20, 0}, new int[] {13, 0});
		BessContainerBlockEntity a = entity(helper, 10, U, -9, BessContainerBlockEntity.class);
		BessContainerBlockEntity b = entity(helper, 13, U, -6, BessContainerBlockEntity.class);
		for (BessContainerBlockEntity box : new BessContainerBlockEntity[] {a, b}) {
			box.setModules(Tuning.CONTAINER_MODULES);
			box.cells().setSoc(0.5);
		}
		BessPcsBlockEntity pcs = entity(helper, 10, U, -3, BessPcsBlockEntity.class);
		BessPlantControllerBlockEntity plant = entity(helper, 2, U, -6, BessPlantControllerBlockEntity.class);
		ChemicalScenes.startPeaker(helper, -40, -20, gt, () -> {
			plant.setFrequencyResponse(false);
			double eStart = pcs.energyKwh(helper.getLevel());
			plant.setPreset(Preset.CHARGE);
			helper.runAfterDelay(200, () -> {
				String charging = read(helper, 10, U, -3) + " | " + read(helper, 2, U, -6) + " | skid " + read(helper, 10, U, 0);
				log(scene, "charging: " + charging);
				check(helper, plant.pcs(helper.getLevel()).size() == 1, "the plant controller should find its PCS by control cable: " + charging);
				check(helper, near(pcs.gridWatts(), -Tuning.PCS_WATTS, 0.06), "the block should charge at about 2.5 MW: " + pcs.gridWatts() + " | " + charging);
				double e0 = pcs.energyKwh(helper.getLevel());
				long t0 = helper.getLevel().getGameTime();
				plant.setPreset(Preset.DISCHARGE);
				helper.runAfterDelay(40, () -> {
					double e1 = pcs.energyKwh(helper.getLevel());
					long t1 = helper.getLevel().getGameTime();
					helper.runAfterDelay(400, () -> {
						String discharging = read(helper, 10, U, -3) + " | " + read(helper, 2, U, -6);
						double e2 = pcs.energyKwh(helper.getLevel());
						double seconds = (helper.getLevel().getGameTime() - t1) / 20.0;
						double delivered = pcs.gridWatts() * seconds / 3.6e6;
						double drawn = e1 - e2;
						log(scene, String.format("discharging: %s | %.2f kWh out of the cells for about %.2f kWh delivered", discharging, drawn, delivered));
						check(helper, near(pcs.gridWatts(), Tuning.PCS_WATTS, 0.06), "the block should discharge at about 2.5 MW: " + discharging);
						check(helper, drawn > delivered * 1.03 && drawn < delivered * 1.12, String.format("the cells should give up about 4.6 %% more than is delivered, "
								+ "plus the chillers: %.2f against %.2f kWh", drawn, delivered));
						check(helper, e0 > eStart + 3, "charging should have filled the cells: " + eStart + " to " + e0 + " kWh (" + (t1 - t0) + " ticks)");
						// frequency response on: the island runs fast with 2.5 MW more on it, so the block gives back some at 4 % droop
						plant.setFrequencyResponse(true);
						helper.runAfterDelay(100, () -> {
							String fr = read(helper, 10, U, -3);
							double hz = pcs.measure(dev.wildcatter.control.Measurable.Quantity.HERTZ);
							double nominal = dev.wildcatter.energy.ElectricalStandard.active().hertz();
							double band = dev.wildcatter.energy.ElectricalStandard.active().frequencyResponseDeadbandHz();
							double expect = Tuning.PCS_WATTS - Math.max(0, hz - nominal - band) / nominal / Tuning.BESS_DROOP * Tuning.PCS_WATTS;
							log(scene, String.format("frequency response at %.3f Hz: %s (expected about %.0f kW)", hz, fr, expect / 1000));
							check(helper, hz <= nominal + band || pcs.gridWatts() < Tuning.PCS_WATTS - 20_000, "over the deadband it should give back power: " + fr);
							check(helper, near(pcs.gridWatts(), expect, 0.05), "its response should follow 4 % droop: " + pcs.gridWatts() + " against " + expect);
							air(helper, 20, U, -10);                                        // the feeder cut
							await(helper, 300, () -> pcs.tripped().startsWith("LOSS OF GRID"), () -> {
								String lost = read(helper, 10, U, -3);
								check(helper, pcs.tripped().startsWith("LOSS OF GRID"), "with its feeder cut the block should trip LOSS OF GRID: " + lost);
								log(scene, "ok: " + lost);
								helper.succeed();
							});
						});
					});
				});
			});
		});
	}

	// ---- a cabinet on a 480 V bus ---------------------------------------------------------------------------------------------------------
	/** A diesel set at (0, 0) (terminal (1, 0)), a 480 V cable east to a cabinet at (8, 0) (its terminal the west face of (8, 0)), heaters beside it. */
	private static BessCabinetBlockEntity cabinetBus(GameTestHelper helper, int heaters) {
		floor(helper, -8, -6, 16, 8);
		genset(helper, 0, U, 0);
		for (int x = 1; x <= 7; x++) {
			put(helper, "cable_industrial", x, U, 0);
			if (helper.getLevel().getBlockEntity(abs(helper, x, U, 0)) instanceof MultiCableBlockEntity cable) {
				cable.setSize(dev.wildcatter.energy.ConductorSize.KCMIL_500);
			}
		}
		for (int i = 0; i < heaters; i++) {
			put(helper, "three_phase_heater", 2 + i, U, 1);
		}
		place(helper, WildcatterBlocks.BESS_CABINET, 8, U, 0, NORTH);          // x 8-9, z 0-1
		BessCabinetBlockEntity cab = entity(helper, 8, U, 0, BessCabinetBlockEntity.class);
		cab.setModules(Tuning.CABINET_MODULES);
		cab.cells().setSoc(0.8);
		return cab;
	}

	static void peakShave(GameTestHelper helper) {
		String scene = "bess_peak_shave";
		BessCabinetBlockEntity cab = cabinetBus(helper, 5);                    // 75 kW of heaters
		start(helper, 0, U, 0, () -> {
			cab.setPeakLimit(40_000);
			cab.setPreset(Preset.PEAK_SHAVE);
			helper.runAfterDelay(300, () -> {
				double set = gensetPanel(helper, 0, U, 0).deliveredWatts();
				String state = read(helper, 8, U, 0) + " | set " + read(helper, 0, U, 0);
				log(scene, "peak shaving: " + state);
				check(helper, near(set, 40_000, 0.15), "the set should be held near its 40 kW limit: " + set + " | " + state);
				check(helper, cab.gridWatts() > 20_000, "the cabinet should carry the rest of the heaters: " + state);
				helper.succeed();
			});
		});
	}

	// ---- bess_forming (22c) ----------------------------------------------------------------------------------------------------------------
	static void forming(GameTestHelper helper) {
		String scene = "bess_forming";
		BessCabinetBlockEntity cab = cabinetBus(helper, 4);                    // 60 kW of heaters
		cab.setPreset(Preset.FORMING);
		await(helper, 400, () -> cab.breakerClosed() && cab.gridWatts() > 40_000 && cab.volts() > Voltage.INDUSTRIAL.lineToLine() * 0.92, () -> {
			String black = read(helper, 8, U, 0);
			log(scene, "black start: " + black);
			check(helper, cab.breakerClosed() && cab.online(), "the cabinet should close onto the dead bus: " + black);
			check(helper, near(cab.volts(), Voltage.INDUSTRIAL.lineToLine(), 0.08), "it should hold the bus near its voltage: " + black);
			check(helper, near(cab.gridWatts(), 60_000, 0.15), "it should carry the heaters: " + black);
			start(helper, 0, U, 0, () -> {
				String both = read(helper, 0, U, 0) + " | " + read(helper, 8, U, 0);
				log(scene, "the set synchronised onto it: " + both);
				check(helper, read(helper, 0, U, 0).contains("breaker CLOSED"), "the set should synchronise and close onto the cabinet's bus: " + both);
				air(helper, 1, U, 0);                                              // the set cut off: a bolted fault on the cabinet alone
				gensetPanel(helper, 0, U, 0).setWantRun(false);
				helper.runAfterDelay(60, () -> {
					((MultiCableBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 5, U, 0))).setGrounded(true);
					double rated = Tuning.CABINET_VA / (3 * Voltage.INDUSTRIAL.nominal());
					double[] most = {0};
					sample(helper, 30, cab, most);
					await(helper, 200, () -> !cab.tripped().isEmpty(), () -> {
						String fault = read(helper, 8, U, 0);
						log(scene, String.format("fault: at most %.0f A against %.0f A rated | %s", most[0], rated, fault));
						check(helper, cab.tripped().startsWith("UNDERVOLTAGE"), "a bolted fault should trip it on UNDERVOLTAGE: " + fault);
						check(helper, most[0] < rated * (Tuning.BESS_CURRENT_LIMIT + 0.15), "it should hold its current near 1.2 per unit: " + most[0] + " A of " + rated);
						helper.succeed();
					});
				});
			});
		});
	}

	private static void sample(GameTestHelper helper, int n, BessCabinetBlockEntity cab, double[] most) {
		if (n <= 0) {
			return;
		}
		helper.runAfterDelay(4, () -> {
			if (cab.tripped().isEmpty() && cab.volts() < Voltage.INDUSTRIAL.lineToLine() * 0.5) {
				most[0] = Math.max(most[0], cab.amps());
			}
			sample(helper, n - 1, cab, most);
		});
	}

	// ---- bess_runaway (22c) ------------------------------------------------------------------------------------------------------------------
	static void runaway(GameTestHelper helper) {
		String scene = "bess_runaway";
		floor(helper, -6, -4, 16, 18);
		boolean[] rules = Rules.snapshot();
		place(helper, WildcatterBlocks.BESS_CONTAINER, 0, U, 0, NORTH);        // A: x 0-2, z 0-5
		place(helper, WildcatterBlocks.BESS_CONTAINER, 3, U, 0, NORTH);        // B: x 3-5, touching A
		place(helper, WildcatterBlocks.BESS_CONTAINER, 7, U, 0, NORTH);        // C: x 7-9, a block from B
		place(helper, WildcatterBlocks.BESS_CONTAINER, 0, U, 10, NORTH);       // D: rule off, later
		BessContainerBlockEntity a = entity(helper, 0, U, 0, BessContainerBlockEntity.class);
		BessContainerBlockEntity b = entity(helper, 3, U, 0, BessContainerBlockEntity.class);
		BessContainerBlockEntity c = entity(helper, 7, U, 0, BessContainerBlockEntity.class);
		for (BessContainerBlockEntity box : new BessContainerBlockEntity[] {a, b, c}) {
			box.setModules(Tuning.CONTAINER_MODULES);
		}
		// a container broken keeps its modules and health in its item
		BessContainerBlockEntity d = entity(helper, 0, U, 10, BessContainerBlockEntity.class);
		d.setModules(Tuning.CONTAINER_MODULES);
		d.cells().testCycles(100, 25);
		double soh = d.cells().soh();
		helper.getLevel().destroyBlock(abs(helper, 0, U, 10), true);
		helper.runAfterDelay(5, () -> {
			ItemStack dropped = ItemStack.EMPTY;
			for (ItemEntity e : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(abs(helper, 0, U, 10)).inflate(8))) {
				if (e.getItem().is(WildcatterBlocks.BESS_CONTAINER.asItem())) {
					dropped = e.getItem();
				}
			}
			check(helper, !dropped.isEmpty(), "a broken container should drop itself");
			check(helper, dropped.getOrDefault(WildcatterComponents.BESS_MODULES, 0) == Tuning.CONTAINER_MODULES
					&& Math.abs(dropped.getOrDefault(WildcatterComponents.BESS_SOH, 1.0) - soh) < 1e-6, "its item should keep 96 modules and SOH " + soh + ": " + dropped.getComponents());
			Rules.set(Rules.Rule.BESS_THERMAL_RUNAWAY, true);
			helper.getLevel().setBlock(abs(helper, -1, U, 2), Blocks.NETHERRACK.defaultBlockState(), 3);
			helper.getLevel().setBlock(abs(helper, -1, U + 1, 2), Blocks.FIRE.defaultBlockState(), 3);   // fire against A's west wall
			a.testFire(Tuning.RUNAWAY_FIRE_SECONDS - 2);
			await(helper, 200, a::burning, () -> {
				check(helper, a.burning(), "fire touching A for 30 s should set it off: " + read(helper, 0, U, 0));
				log(scene, "A: " + read(helper, 0, U, 0));
				b.testSpread(Tuning.RUNAWAY_SPREAD_SECONDS - 2);
				c.testSpread(Tuning.RUNAWAY_SPREAD_SECONDS - 2);
				helper.runAfterDelay(100, () -> {
					check(helper, b.burning(), "B, touching A, should catch: " + read(helper, 3, U, 0));
					check(helper, !c.burning(), "C, a block apart, should not: " + read(helper, 7, U, 0));
					a.testBurn(Tuning.RUNAWAY_BURN_SECONDS - 1);
					helper.runAfterDelay(60, () -> {
						check(helper, helper.getLevel().getBlockState(abs(helper, 0, U, 0)).isAir(), "A should be burned out");
						// the rule off: fire against C does nothing
						Rules.set(Rules.Rule.BESS_THERMAL_RUNAWAY, false);
						helper.getLevel().setBlock(abs(helper, 10, U, 2), Blocks.NETHERRACK.defaultBlockState(), 3);
						helper.getLevel().setBlock(abs(helper, 10, U + 1, 2), Blocks.FIRE.defaultBlockState(), 3);
						c.testFire(Tuning.RUNAWAY_FIRE_SECONDS + 5);
						helper.runAfterDelay(60, () -> {
							check(helper, !c.burning(), "with the rule off fire should not set C off: " + read(helper, 7, U, 0));
							log(scene, "ok: B " + read(helper, 3, U, 0) + " | C " + read(helper, 7, U, 0));
							Rules.restore(rules);
							helper.succeed();
						});
					});
				});
			});
		});
	}
}
