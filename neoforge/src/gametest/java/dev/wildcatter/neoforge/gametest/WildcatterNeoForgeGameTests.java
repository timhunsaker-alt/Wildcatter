package dev.wildcatter.neoforge.gametest;

import dev.wildcatter.Wildcatter;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.block.BoilerBlockEntity;
import dev.wildcatter.block.LoadBlock;
import dev.wildcatter.energy.Inspectable;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * The NeoForge smoke scenes (Phase 9b): the mod registers, a starter plant lights a lamp through the circuit manager's block entity
 * hook, a plug fills a real FE storage, and the ores reach their biomes. The physics is covered by the Fabric client game
 * test. Run with {@code ./gradlew :neoforge:runGameTestServer}.
 */
@Mod("wildcatter_gametest")
public class WildcatterNeoForgeGameTests {
	private static final String[] SCENES = {"registers", "lamp", "plug", "ores"};
	/** The ammonia plant's scenes (AmmoniaPlantScenes): big, so each gets room round it, and long, for the gensets to start. */
	private static final String[] PLANT_SCENES = {"ammonia_loop", "nitric_acid", "an_emulsion", "crane_siting", "quench_sump", "stabilizer_fuel", "cog_engine", "pot_aux", "burner_hopper"};
	/** Phase 14b-14d's scenes (GasPowerScenes): longer still, for the acid plant to raise steam and the turbine to take the plant. */
	private static final String[] GAS_POWER_SCENES = {"sales_gas", "acid_steam", "biogas"};

	/** The protection gap wave's grid scenes (docs/questions/protection-gaps.md), each group in its own class, all run as plant scenes. */
	private static final java.util.Map<String, Consumer<GameTestHelper>> GRID_SCENES = new java.util.LinkedHashMap<>();

	static {
		GRID_SCENES.putAll(RelayScenes.scenes());
		GRID_SCENES.putAll(DifferentialScenes.scenes());
		GRID_SCENES.putAll(PanelScenes.scenes());
		GRID_SCENES.putAll(DistributionScenes.scenes());
	}

	public WildcatterNeoForgeGameTests(IEventBus modBus) {
		modBus.addListener(RegisterEvent.class, event -> {
			// the stand-in gas source: a block of this mod offering a product on Wildcatter's steel pipe, as another mod would (docs/biogas.md)
			event.register(Registries.BLOCK, helper -> helper.register(id("gas_source"), new TestGasSource.SourceBlock(
					net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id("gas_source"))))));
			event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
				TestGasSource.TYPE = new net.minecraft.world.level.block.entity.BlockEntityType<>(TestGasSource.SourceEntity::new,
						java.util.Set.of(BuiltInRegistries.BLOCK.getValue(id("gas_source"))));
				helper.register(id("gas_source"), TestGasSource.TYPE);
			});
		});
		modBus.addListener(RegisterEvent.class, event -> event.register(Registries.TEST_FUNCTION, helper -> {
			helper.register(id("registers"), (Consumer<GameTestHelper>) WildcatterNeoForgeGameTests::registers);
			helper.register(id("lamp"), (Consumer<GameTestHelper>) WildcatterNeoForgeGameTests::lamp);
			helper.register(id("plug"), (Consumer<GameTestHelper>) WildcatterNeoForgeGameTests::plug);
			helper.register(id("ores"), (Consumer<GameTestHelper>) WildcatterNeoForgeGameTests::ores);
			helper.register(id("ammonia_loop"), (Consumer<GameTestHelper>) AmmoniaPlantScenes::ammoniaLoop);
			helper.register(id("nitric_acid"), (Consumer<GameTestHelper>) AmmoniaPlantScenes::nitricAcid);
			helper.register(id("an_emulsion"), (Consumer<GameTestHelper>) AmmoniaPlantScenes::anEmulsion);
			helper.register(id("crane_siting"), (Consumer<GameTestHelper>) CraneScenes::craneSiting);
			helper.register(id("ring_travel"), (Consumer<GameTestHelper>) CraneScenes::ringTravel);
			helper.register(id("quench_sump"), (Consumer<GameTestHelper>) PlantGapScenes::quenchSump);
			helper.register(id("stabilizer_fuel"), (Consumer<GameTestHelper>) PlantGapScenes::stabilizerFuel);
			helper.register(id("cog_engine"), (Consumer<GameTestHelper>) PlantGapScenes::cogEngine);
			helper.register(id("pot_aux"), (Consumer<GameTestHelper>) PlantGapScenes::potAux);
			helper.register(id("burner_hopper"), (Consumer<GameTestHelper>) PlantGapScenes::burnerHopper);
			helper.register(id("sales_gas"), (Consumer<GameTestHelper>) GasPowerScenes::salesGas);
			helper.register(id("acid_steam"), (Consumer<GameTestHelper>) GasPowerScenes::acidSteam);
			helper.register(id("biogas"), (Consumer<GameTestHelper>) GasPowerScenes::biogas);
			GRID_SCENES.forEach((name, scene) -> helper.register(id(name), scene));
		}));
		modBus.addListener(RegisterCapabilitiesEvent.class, event -> event.registerBlock(Capabilities.Energy.BLOCK, (level, pos, state, entity, side) -> STORAGE, Blocks.GOLD_BLOCK));
		modBus.addListener(RegisterGameTestsEvent.class, event -> {
			Holder<TestEnvironmentDefinition<?>> plain = event.registerEnvironment(id("plain"));
			for (String scene : SCENES) {
				event.registerTest(id(scene), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(scene)),
						new TestData<>(plain, Identifier.withDefaultNamespace("empty"), 400, 1, true)));
			}
			for (String scene : java.util.stream.Stream.concat(java.util.Arrays.stream(PLANT_SCENES), GRID_SCENES.keySet().stream()).toList()) {
				event.registerTest(id(scene), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(scene)),
						new TestData<>(plain, net.minecraft.world.level.Level.OVERWORLD, Identifier.withDefaultNamespace("empty"), 3000, 1, true,
								net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, 40)));
			}
			// the ring crane's travel runs 200 m down its rails: room round it, as the sales scene has
			event.registerTest(id("ring_travel"), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id("ring_travel")),
					new TestData<>(plain, net.minecraft.world.level.Level.OVERWORLD, Identifier.withDefaultNamespace("empty"), 3000, 1, true,
							net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, 240)));
			for (String scene : GAS_POWER_SCENES) {
				event.registerTest(id(scene), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(scene)),
						new TestData<>(plain, net.minecraft.world.level.Level.OVERWORLD, Identifier.withDefaultNamespace("empty"), 12000, 1, true,
								net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, 240)));   // padding: the sales scene's delivery station is 200 blocks off
			}
		});
	}

	static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("wildcatter_gametest", path);
	}

	private static void check(GameTestHelper helper, boolean ok, String message) {
		helper.assertTrue(ok, Component.literal(message));
	}

	private static String read(GameTestHelper helper, BlockPos pos) {
		return helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof Inspectable i ? i.inspect().getString() : "(no readout)";
	}

	/** Every block, item and block entity type is registered, and the Field Manual has its pages. */
	private static void registers(GameTestHelper helper) {
		long blocks = BuiltInRegistries.BLOCK.keySet().stream().filter(k -> k.getNamespace().equals(Wildcatter.MOD_ID)).count();
		long items = BuiltInRegistries.ITEM.keySet().stream().filter(k -> k.getNamespace().equals(Wildcatter.MOD_ID)).count();
		long entities = BuiltInRegistries.BLOCK_ENTITY_TYPE.keySet().stream().filter(k -> k.getNamespace().equals(Wildcatter.MOD_ID)).count();
		check(helper, blocks > 250 && items > 300 && entities > 100, "registered " + blocks + " blocks, " + items + " items, " + entities + " block entity types");
		check(helper, BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(Wildcatter.id("wildcatter")), "the creative tab is registered");
		check(helper, new ItemStack(WildcatterItems.FIELD_MANUAL).has(DataComponents.WRITTEN_BOOK_CONTENT), "the Field Manual carries its pages");
		System.out.println("[wildcatter-neoforge-test] registers ok: " + blocks + " blocks, " + items + " items, " + entities + " block entity types");
		helper.succeed();
	}

	/** The 120 V starter plant: a boiler on a campfire and its turbine, a hot and a neutral wire, and a lamp. */
	private static void plant(GameTestHelper helper) {
		for (int x = -1; x <= 6; x++) {
			for (int z = -1; z <= 2; z++) {
				helper.setBlock(x, 0, z, Blocks.STONE);
			}
		}
		helper.setBlock(0, 1, 0, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
		helper.setBlock(0, 2, 0, WildcatterBlocks.BOILER);
		helper.setBlock(1, 2, 0, WildcatterBlocks.STEAM_TURBINE);
		helper.getBlockEntity(new BlockPos(0, 2, 0), BoilerBlockEntity.class).addWater(4000);
	}

	private static void wire(GameTestHelper helper, int x, int z, String id) {
		helper.setBlock(x, 2, z, BuiltInRegistries.BLOCK.getValue(Wildcatter.id(id)));
	}

	/** The plant lights a lamp: the block entity hook, the solver and the ticking all work on NeoForge. */
	private static void lamp(GameTestHelper helper) {
		plant(helper);
		wire(helper, 2, 0, "wool_wire_hot");
		wire(helper, 3, 0, "wool_wire_hot");
		wire(helper, 4, 0, "wool_wire_hot");
		helper.setBlock(5, 2, 0, WildcatterBlocks.ELECTRIC_LAMP);
		for (int x = 1; x <= 5; x++) {
			wire(helper, x, 1, "wool_wire_neutral");
		}
		BlockPos lamp = new BlockPos(5, 2, 0);
		helper.succeedWhen(() -> {
			check(helper, helper.getLevel().getBlockState(helper.absolutePos(lamp)).getValue(LoadBlock.LIT), "the lamp should light: " + read(helper, lamp));
			String reading = read(helper, lamp);
			check(helper, reading.matches(".*\\b(11[0-9]|120) V, 0\\.[45]\\d A.*"), "lamp volts and amps look wrong: " + reading);
			System.out.println("[wildcatter-neoforge-test] lamp ok: " + reading);
		});
	}

	/** A real NeoForge energy handler on a gold block: 1,000,000 FE, taking 1,000 FE a tick (a stand-in for a Mekanism cube). */
	private static final SimpleEnergyHandler STORAGE = new SimpleEnergyHandler(1_000_000, 1000, 0);

	/** A NEMA 5-15 plug fills the FE storage beside it, and draws nothing once it is full. */
	private static void plug(GameTestHelper helper) {
		plant(helper);
		wire(helper, 2, 0, "wool_wire_hot");
		for (int x = 1; x <= 3; x++) {
			wire(helper, x, 1, "wool_wire_neutral");
		}
		wire(helper, 3, 0, "receptacle_nema_5_15");
		wire(helper, 4, 0, "plug_nema_5_15");
		helper.setBlock(5, 2, 0, Blocks.GOLD_BLOCK);
		helper.runAfterDelay(160, () -> {
			long before = STORAGE.getAmountAsLong();
			helper.runAfterDelay(100, () -> {
				long gained = STORAGE.getAmountAsLong() - before;
				String plug = read(helper, new BlockPos(4, 2, 0));
				check(helper, plug.contains("FE/t") && gained > 70_000 && gained < 120_000, "the plug should fill the FE storage at about 930 FE a tick (" + gained + " in 100 ticks): " + plug);
				fill();
				helper.runAfterDelay(80, () -> {
					String full = read(helper, new BlockPos(4, 2, 0));
					check(helper, full.contains("delivering 0 W") && full.contains(", 0.0 A"), "a full storage should draw nothing: " + full);
					System.out.println("[wildcatter-neoforge-test] plug ok: " + plug + " | full: " + full + " | FE gained " + gained);
					helper.succeed();
				});
			});
		});
	}

	private static void fill() {
		try (Transaction t = Transaction.openRoot()) {
			while (STORAGE.insert(Integer.MAX_VALUE, t) > 0) {
				// the handler takes at most 1,000 a call: fill it the rest of the way
			}
			t.commit();
		}
	}

	/** The biome modifiers load, one per placed ore feature, and put the salt domes into the plains. */
	private static void ores(GameTestHelper helper) {
		var access = helper.getLevel().registryAccess();
		long modifiers = access.lookupOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS).listElementIds()
				.filter(k -> k.identifier().getNamespace().equals(Wildcatter.MOD_ID)).count();
		check(helper, modifiers == dev.wildcatter.ore.OreGen.placements().size(), modifiers + " biome modifiers, expected " + dev.wildcatter.ore.OreGen.placements().size());
		ResourceKey<PlacedFeature> saltDome = ResourceKey.create(Registries.PLACED_FEATURE, Wildcatter.id("salt_dome"));
		boolean inPlains = access.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS).value().getGenerationSettings().features().stream()
				.anyMatch(step -> step.stream().anyMatch(f -> f.is(saltDome)));
		check(helper, inPlains, "the salt domes should generate in the plains");
		System.out.println("[wildcatter-neoforge-test] ores ok: " + modifiers + " biome modifiers; salt domes in the plains");
		helper.succeed();
	}
}
