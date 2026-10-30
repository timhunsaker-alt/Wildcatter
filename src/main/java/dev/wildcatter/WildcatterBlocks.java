package dev.wildcatter;

import dev.wildcatter.block.BoilerBlock;
import dev.wildcatter.block.BoilerBlockEntity;
import dev.wildcatter.block.BreakerBlock;
import dev.wildcatter.block.FuseBlock;
import dev.wildcatter.block.InsulatorBlock;
import dev.wildcatter.block.PlugAdapterBlock;
import dev.wildcatter.block.PlugAdapterBlockEntity;
import dev.wildcatter.block.ReceptacleBlock;
import dev.wildcatter.block.ReceptacleBlockEntity;
import dev.wildcatter.energy.PlugType;
import dev.wildcatter.block.InsulatorBlockEntity;
import dev.wildcatter.block.FuseBlockEntity;
import dev.wildcatter.block.BreakerBlockEntity;
import dev.wildcatter.block.CableBlock;
import dev.wildcatter.block.CasingBlock;
import dev.wildcatter.block.CrossarmBlock;
import dev.wildcatter.block.PoleBlock;
import dev.wildcatter.block.ColumnBlock;
import dev.wildcatter.block.EngineBlock;
import dev.wildcatter.block.EngineBlockEntity;
import dev.wildcatter.block.CoolingFillBlock;
import dev.wildcatter.block.CoolingTowerBlock;
import dev.wildcatter.block.CoolingTowerBlockEntity;
import dev.wildcatter.block.RefineryBlock;
import dev.wildcatter.block.RefineryBlockEntity;
import dev.wildcatter.block.DrillingRigBlock;
import dev.wildcatter.block.DrillingRigBlockEntity;
import dev.wildcatter.block.GasWellheadBlock;
import dev.wildcatter.block.GasWellheadBlockEntity;
import dev.wildcatter.block.PumpjackBlock;
import dev.wildcatter.block.PumpjackBlockEntity;
import dev.wildcatter.block.LoadBlock;
import dev.wildcatter.block.LoadBlockEntity;
import dev.wildcatter.block.WireBlockEntity;
import dev.wildcatter.circuit.Conductor;
import net.minecraft.world.level.material.MapColor;
import dev.wildcatter.block.FireboxBlock;
import dev.wildcatter.block.FireboxBlockEntity;
import dev.wildcatter.block.PipeBlock;
import dev.wildcatter.block.PumpBlock;
import dev.wildcatter.block.PumpBlockEntity;
import dev.wildcatter.block.GroundRodBlock;
import dev.wildcatter.block.GroundWireBlock;
import dev.wildcatter.block.SteamTurbineBlock;
import dev.wildcatter.block.TransformerBlock;
import dev.wildcatter.block.TransformerBlockEntity;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.block.SteamTurbineBlockEntity;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Every block, its item, and its block entity. */
public final class WildcatterBlocks {
	public static final Block BOILER = block("boiler", props -> new BoilerBlock(props.mapColor(MapColor.METAL)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	public static final Block STEAM_TURBINE = block("steam_turbine", props -> new SteamTurbineBlock(props.mapColor(MapColor.METAL)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

	/** Holds the fuel and heats the boiler above it. Nine in a 3x3 floor become one big firebox. */
	public static final Block FIREBOX = block("firebox", props -> new FireboxBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(3.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()
			.lightLevel(state -> state.getValue(FireboxBlock.LIT) ? 13 : 0)));
	/** Water pipe: carries the pump's water to the boilers, and the turbines' condensate back. */
	public static final Block PIPE = block("pipe", props -> new PipeBlock(props.mapColor(MapColor.METAL)
			.strength(1.5F, 4.0F).sound(SoundType.METAL).noOcclusion(), PipeBlock.Kind.WATER));
	/** A 110 V electric pump that lifts water from the water touching it. */
	public static final Block PUMP = block("pump", props -> new PumpBlock(props.mapColor(MapColor.COLOR_BLUE)
			.strength(2.0F, 5.0F).sound(SoundType.METAL)));

	/** Carries gas (and pumped crude) from a well to the fireboxes. Never joins a water pipe. */
	public static final Block GAS_LINE = block("gas_line", props -> new PipeBlock(props.mapColor(MapColor.COLOR_ORANGE)
			.strength(1.5F, 4.0F).sound(SoundType.METAL).noOcclusion(), PipeBlock.Kind.GAS));
	/** Sinks a cased hole straight down. A strike turns it into a pumpjack or a gas wellhead. */
	public static final Block DRILLING_RIG = block("drilling_rig", props -> new DrillingRigBlock(props.mapColor(MapColor.COLOR_YELLOW)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	public static final Block CASING = block("casing", props -> new CasingBlock(props.mapColor(MapColor.METAL)
			.strength(4.0F, 8.0F).sound(SoundType.NETHERITE_BLOCK)));
	public static final Block PUMPJACK = block("pumpjack", props -> new PumpjackBlock(props.mapColor(MapColor.COLOR_BLACK)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	public static final Block GAS_WELLHEAD = block("gas_wellhead", props -> new GasWellheadBlock(props.mapColor(MapColor.COLOR_ORANGE)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

	/** Crude in, diesel out. The base is a pot still; two columns on top make it a full tower that also makes plastic. */
	public static final Block REFINERY = block("refinery", props -> new RefineryBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	public static final Block REFINERY_COLUMN = block("refinery_column", props -> new ColumnBlock(props.mapColor(MapColor.METAL)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	/** A fan base plus two fill sections make a cooling tower: it cools the condensate of turbines on the same water pipes. */
	public static final Block COOLING_TOWER = block("cooling_tower", props -> new CoolingTowerBlock(props.mapColor(MapColor.STONE)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	public static final Block COOLING_FILL = block("cooling_fill", props -> new CoolingFillBlock(props.mapColor(MapColor.STONE)
			.strength(2.0F, 4.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
	/** Each is a small 110 V set; eight of one kind in a 2x2x2 cube make a 4.16 kV generator. */
	public static final Block DIESEL_ENGINE = block("diesel_engine", props -> new EngineBlock(props.mapColor(MapColor.COLOR_GREEN)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), EngineBlock.Fuel.DIESEL));
	public static final Block GAS_TURBINE = block("gas_turbine", props -> new EngineBlock(props.mapColor(MapColor.COLOR_LIGHT_BLUE)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), EngineBlock.Fuel.GAS));

	/** A utility pole section and the crossarm that goes on top. Bare high-voltage lines belong up here. */
	public static final Block POLE = block("pole", props -> new PoleBlock(props.mapColor(MapColor.WOOD)
			.strength(2.0F, 3.0F).sound(SoundType.WOOD).noOcclusion()));
	public static final Block CROSSARM = block("crossarm", props -> new CrossarmBlock(props.mapColor(MapColor.WOOD)
			.strength(1.5F, 3.0F).sound(SoundType.WOOD).noOcclusion()));

	// Wire. Every circuit needs two: a hot (red) one out to the load and a neutral (black) one back.
	// Bare wire shocks you while it is live; wool-wrapped wire is safe until the wool wears out.
	// Higher voltages are only bare for now: they get proper insulation once there is plastic.
	public static final Block WIRE_HOT = cable("wire_hot", Voltage.LOW, Conductor.A, false);
	public static final Block WIRE_NEUTRAL = cable("wire_neutral", Voltage.LOW, Conductor.N, false);
	public static final Block WOOL_WIRE_HOT = cable("wool_wire_hot", Voltage.LOW, Conductor.A, true);
	public static final Block WOOL_WIRE_NEUTRAL = cable("wool_wire_neutral", Voltage.LOW, Conductor.N, true);
	public static final Block MV_WIRE_HOT = cable("mv_wire_hot", Voltage.MEDIUM, Conductor.A, false);
	public static final Block MV_WIRE_NEUTRAL = cable("mv_wire_neutral", Voltage.MEDIUM, Conductor.N, false);
	public static final Block DIST_WIRE_HOT = cable("dist_wire_hot", Voltage.DISTRIBUTION, Conductor.A, false);
	public static final Block DIST_WIRE_NEUTRAL = cable("dist_wire_neutral", Voltage.DISTRIBUTION, Conductor.N, false);
	public static final Block HV_WIRE_HOT = cable("hv_wire_hot", Voltage.HIGH, Conductor.A, false);
	public static final Block HV_WIRE_NEUTRAL = cable("hv_wire_neutral", Voltage.HIGH, Conductor.N, false);

	// Plastic insulation never wears out, at any voltage: safe to touch, with no bare-line arc reach.
	public static final Block PLASTIC_WIRE_HOT = plasticCable("plastic_wire_hot", Voltage.LOW, Conductor.A);
	public static final Block PLASTIC_WIRE_NEUTRAL = plasticCable("plastic_wire_neutral", Voltage.LOW, Conductor.N);
	public static final Block MV_PLASTIC_WIRE_HOT = plasticCable("mv_plastic_wire_hot", Voltage.MEDIUM, Conductor.A);
	public static final Block MV_PLASTIC_WIRE_NEUTRAL = plasticCable("mv_plastic_wire_neutral", Voltage.MEDIUM, Conductor.N);
	public static final Block DIST_PLASTIC_WIRE_HOT = plasticCable("dist_plastic_wire_hot", Voltage.DISTRIBUTION, Conductor.A);
	public static final Block DIST_PLASTIC_WIRE_NEUTRAL = plasticCable("dist_plastic_wire_neutral", Voltage.DISTRIBUTION, Conductor.N);
	public static final Block HV_PLASTIC_WIRE_HOT = plasticCable("hv_plastic_wire_hot", Voltage.HIGH, Conductor.A);
	public static final Block HV_PLASTIC_WIRE_NEUTRAL = plasticCable("hv_plastic_wire_neutral", Voltage.HIGH, Conductor.N);

	/** A copper rod in the earth, for grounding machines. */
	public static final Block GROUND_ROD = block("ground_rod", props -> new GroundRodBlock(props.mapColor(MapColor.COLOR_ORANGE)
			.strength(1.0F).sound(SoundType.COPPER).noOcclusion()));
	public static final Block GROUND_WIRE = block("ground_wire", props -> new GroundWireBlock(props.mapColor(MapColor.COLOR_GREEN)
			.strength(0.3F).sound(SoundType.COPPER).noOcclusion()));

	public static final Block BREAKER = block("breaker", props -> new BreakerBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(1.5F).sound(SoundType.METAL), Voltage.LOW));
	/** Opens and closes the hot wire by hand and protects nothing. */
	public static final Block SWITCH = block("switch", props -> new BreakerBlock(props.mapColor(MapColor.COLOR_YELLOW)
			.strength(1.5F).sound(SoundType.METAL), Voltage.LOW, true));
	/** Opens its feeder when the island's frequency falls too far: load shedding. */
	public static final Block UFLS_RELAY = block("ufls_relay", props -> new BreakerBlock(props.mapColor(MapColor.COLOR_PURPLE)
			.strength(1.5F).sound(SoundType.METAL), Voltage.LOW, false, true));
	/** A fused cutout: the 12.47 kV fuse on a pole. Its link drops open when it blows and a new link (a copper ingot) renews it. */
	public static final Block FUSED_CUTOUT = block("fused_cutout", props -> new FuseBlock(props.mapColor(MapColor.TERRACOTTA_ORANGE)
			.strength(1.5F).sound(SoundType.GLASS), Voltage.DISTRIBUTION));
	/** Insulators on top of a pole or crossarm: one conductor each, rated for a tier of line. */
	public static final Block INSULATOR_LV = block("insulator_lv", props -> new InsulatorBlock(props.mapColor(MapColor.TERRACOTTA_WHITE)
			.strength(1.0F).sound(SoundType.GLASS).noOcclusion(), Voltage.LOW));
	public static final Block INSULATOR_DIST = block("insulator_dist", props -> new InsulatorBlock(props.mapColor(MapColor.TERRACOTTA_BROWN)
			.strength(1.0F).sound(SoundType.GLASS).noOcclusion(), Voltage.DISTRIBUTION));
	public static final Block INSULATOR_HV = block("insulator_hv", props -> new InsulatorBlock(props.mapColor(MapColor.COLOR_LIGHT_GRAY)
			.strength(1.5F).sound(SoundType.GLASS).noOcclusion(), Voltage.HIGH));
	/** Receptacles and the plug adapters that fit them, one pair per type. */
	public static final Block RECEPTACLE_NEMA_5_15 = receptacle("receptacle_nema_5_15", PlugType.NEMA_5_15);
	public static final Block RECEPTACLE_NEMA_5_20 = receptacle("receptacle_nema_5_20", PlugType.NEMA_5_20);
	public static final Block RECEPTACLE_NEMA_14_50 = receptacle("receptacle_nema_14_50", PlugType.NEMA_14_50);
	public static final Block RECEPTACLE_CEE_7 = receptacle("receptacle_cee_7", PlugType.CEE_7);
	public static final Block RECEPTACLE_IEC_60309_BLUE = receptacle("receptacle_iec_60309_blue", PlugType.IEC_60309_BLUE);
	public static final Block PLUG_NEMA_5_15 = plug("plug_nema_5_15", PlugType.NEMA_5_15);
	public static final Block PLUG_NEMA_5_20 = plug("plug_nema_5_20", PlugType.NEMA_5_20);
	public static final Block PLUG_NEMA_14_50 = plug("plug_nema_14_50", PlugType.NEMA_14_50);
	public static final Block PLUG_CEE_7 = plug("plug_cee_7", PlugType.CEE_7);
	public static final Block PLUG_IEC_60309_BLUE = plug("plug_iec_60309_blue", PlugType.IEC_60309_BLUE);
	/** One-shot protection: cheap, fast, and it has to be replaced after it blows. */
	public static final Block FUSE = block("fuse", props -> new FuseBlock(props.mapColor(MapColor.COLOR_ORANGE).strength(1.0F).sound(SoundType.GLASS)));
	/** For 4.16 kV and 138 kV hot lines. Same rules as the 110 V breaker, with ratings to suit the current. */
	public static final Block MV_BREAKER = block("mv_breaker", props -> new BreakerBlock(props.mapColor(MapColor.COLOR_BLUE)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.MEDIUM));
	/** For 12.47 kV feeders. */
	public static final Block DIST_BREAKER = block("dist_breaker", props -> new BreakerBlock(props.mapColor(MapColor.COLOR_GREEN)
			.strength(3.5F, 7.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.DISTRIBUTION));
	public static final Block HV_BREAKER = block("hv_breaker", props -> new BreakerBlock(props.mapColor(MapColor.COLOR_RED)
			.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.HIGH));
	public static final Block ELECTRIC_LAMP = block("electric_lamp", props -> new LoadBlock(props.mapColor(MapColor.COLOR_YELLOW)
			.strength(0.5F).sound(SoundType.GLASS).lightLevel(state -> state.getValue(LoadBlock.LIT) ? 15 : 0), Tuning.LAMP_WATTS));
	public static final Block ELECTRIC_HEATER = block("electric_heater", props -> new LoadBlock(props.mapColor(MapColor.COLOR_RED)
			.strength(1.0F).sound(SoundType.METAL).lightLevel(state -> state.getValue(LoadBlock.LIT) ? 7 : 0), Tuning.HEATER_WATTS));

	/** Steps 110 V up to 4.16 kV and back. */
	public static final Block TRANSFORMER = block("transformer", props -> new TransformerBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.LOW, Voltage.MEDIUM));
	/** Steps 4.16 kV up to 138 kV and back. */
	/** 12.47 kV (7.2 kV to neutral) down to 120/240 V split-phase, 25 kVA; it hangs on a pole. */
	public static final Block POLE_TRANSFORMER = block("pole_transformer", props -> new TransformerBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.LOW, Voltage.DISTRIBUTION, Tuning.POLE_TRANSFORMER_VA));
	/** 138 kV to 12.47 kV: the stand-in until the Phase 4 substation multiblock. */
	public static final Block DISTRIBUTION_TRANSFORMER = block("distribution_transformer", props -> new TransformerBlock(props.mapColor(MapColor.COLOR_LIGHT_GREEN)
			.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.DISTRIBUTION, Voltage.HIGH));
	public static final Block SUBSTATION_TRANSFORMER = block("substation_transformer", props -> new TransformerBlock(props.mapColor(MapColor.COLOR_BLUE)
			.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.MEDIUM, Voltage.HIGH));

	public static final BlockEntityType<BoilerBlockEntity> BOILER_ENTITY = entity(BoilerBlockEntity::new, BOILER);
	public static final BlockEntityType<SteamTurbineBlockEntity> STEAM_TURBINE_ENTITY = entity(SteamTurbineBlockEntity::new, STEAM_TURBINE);
	public static final BlockEntityType<WireBlockEntity> WIRE_ENTITY = entity(WireBlockEntity::new, DIST_WIRE_HOT, DIST_WIRE_NEUTRAL, DIST_PLASTIC_WIRE_HOT, DIST_PLASTIC_WIRE_NEUTRAL, WIRE_HOT, WIRE_NEUTRAL, WOOL_WIRE_HOT,
			WOOL_WIRE_NEUTRAL, MV_WIRE_HOT, MV_WIRE_NEUTRAL, HV_WIRE_HOT, HV_WIRE_NEUTRAL,
			PLASTIC_WIRE_HOT, PLASTIC_WIRE_NEUTRAL, MV_PLASTIC_WIRE_HOT, MV_PLASTIC_WIRE_NEUTRAL, HV_PLASTIC_WIRE_HOT, HV_PLASTIC_WIRE_NEUTRAL);
	public static final BlockEntityType<LoadBlockEntity> LOAD_ENTITY = entity(LoadBlockEntity::new, ELECTRIC_LAMP, ELECTRIC_HEATER);
	public static final BlockEntityType<BreakerBlockEntity> BREAKER_ENTITY = entity(BreakerBlockEntity::new, BREAKER, MV_BREAKER, DIST_BREAKER, HV_BREAKER, SWITCH, UFLS_RELAY);
	public static final BlockEntityType<FuseBlockEntity> FUSE_ENTITY = entity(FuseBlockEntity::new, FUSE, FUSED_CUTOUT);
	public static final BlockEntityType<ReceptacleBlockEntity> RECEPTACLE_ENTITY = entity(ReceptacleBlockEntity::new, RECEPTACLE_NEMA_5_15, RECEPTACLE_NEMA_5_20, RECEPTACLE_NEMA_14_50, RECEPTACLE_CEE_7, RECEPTACLE_IEC_60309_BLUE);
	public static final BlockEntityType<PlugAdapterBlockEntity> PLUG_ADAPTER_ENTITY = entity(PlugAdapterBlockEntity::new, PLUG_NEMA_5_15, PLUG_NEMA_5_20, PLUG_NEMA_14_50, PLUG_CEE_7, PLUG_IEC_60309_BLUE);
	public static final BlockEntityType<InsulatorBlockEntity> INSULATOR_ENTITY = entity(InsulatorBlockEntity::new, INSULATOR_LV, INSULATOR_DIST, INSULATOR_HV);
	public static final BlockEntityType<TransformerBlockEntity> TRANSFORMER_ENTITY = entity(TransformerBlockEntity::new, TRANSFORMER, SUBSTATION_TRANSFORMER, DISTRIBUTION_TRANSFORMER, POLE_TRANSFORMER);

	public static final BlockEntityType<FireboxBlockEntity> FIREBOX_ENTITY = entity(FireboxBlockEntity::new, FIREBOX);
	public static final BlockEntityType<PumpBlockEntity> PUMP_ENTITY = entity(PumpBlockEntity::new, PUMP);

	public static final BlockEntityType<DrillingRigBlockEntity> DRILLING_RIG_ENTITY = entity(DrillingRigBlockEntity::new, DRILLING_RIG);
	public static final BlockEntityType<PumpjackBlockEntity> PUMPJACK_ENTITY = entity(PumpjackBlockEntity::new, PUMPJACK);
	public static final BlockEntityType<GasWellheadBlockEntity> GAS_WELLHEAD_ENTITY = entity(GasWellheadBlockEntity::new, GAS_WELLHEAD);

	public static final BlockEntityType<RefineryBlockEntity> REFINERY_ENTITY = entity(RefineryBlockEntity::new, REFINERY);
	public static final BlockEntityType<CoolingTowerBlockEntity> COOLING_TOWER_ENTITY = entity(CoolingTowerBlockEntity::new, COOLING_TOWER);
	public static final BlockEntityType<EngineBlockEntity> ENGINE_ENTITY = entity(EngineBlockEntity::new, DIESEL_ENGINE, GAS_TURBINE);

	private WildcatterBlocks() {
	}

	public static Block receptacleFor(PlugType type) {
		return switch (type) {
			case NEMA_5_15 -> RECEPTACLE_NEMA_5_15;
			case NEMA_5_20 -> RECEPTACLE_NEMA_5_20;
			case NEMA_14_50 -> RECEPTACLE_NEMA_14_50;
			case CEE_7 -> RECEPTACLE_CEE_7;
			case IEC_60309_BLUE -> RECEPTACLE_IEC_60309_BLUE;
		};
	}

	public static Block plugFor(PlugType type) {
		return switch (type) {
			case NEMA_5_15 -> PLUG_NEMA_5_15;
			case NEMA_5_20 -> PLUG_NEMA_5_20;
			case NEMA_14_50 -> PLUG_NEMA_14_50;
			case CEE_7 -> PLUG_CEE_7;
			case IEC_60309_BLUE -> PLUG_IEC_60309_BLUE;
		};
	}

	private static Block receptacle(String name, PlugType type) {
		return block(name, props -> new ReceptacleBlock(props.mapColor(MapColor.COLOR_LIGHT_GRAY).strength(1.0F).sound(SoundType.METAL).noOcclusion(), type));
	}

	private static Block plug(String name, PlugType type) {
		return block(name, props -> new PlugAdapterBlock(props.mapColor(MapColor.COLOR_BLACK).strength(1.0F).sound(SoundType.METAL).noOcclusion(), type));
	}

	private static Block cable(String name, Voltage voltage, Conductor role, boolean wool) {
		return block(name, props -> new CableBlock(props.mapColor(role == Conductor.A ? MapColor.COLOR_RED : MapColor.COLOR_BLACK)
				.strength(0.5F).sound(wool ? SoundType.WOOL : SoundType.COPPER).noOcclusion(), voltage, role, wool));
	}

	private static Block plasticCable(String name, Voltage voltage, Conductor role) {
		return block(name, props -> new CableBlock(props.mapColor(role == Conductor.A ? MapColor.COLOR_RED : MapColor.COLOR_BLACK)
				.strength(0.5F).sound(SoundType.COPPER).noOcclusion(), voltage, role, false, true));
	}

	private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Wildcatter.id(name));
		return factory.apply(BlockBehaviour.Properties.of().setId(key));
	}

	private static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> entity(
			BlockEntityType.BlockEntitySupplier<T> factory, Block... blocks) {
		return new BlockEntityType<>(factory, Set.of(blocks));
	}

	public static void register() {
		add("boiler", BOILER);
		add("steam_turbine", STEAM_TURBINE);
		add("firebox", FIREBOX);
		add("pipe", PIPE);
		add("pump", PUMP);
		add("gas_line", GAS_LINE);
		add("drilling_rig", DRILLING_RIG);
		add("casing", CASING);
		add("pumpjack", PUMPJACK);
		add("gas_wellhead", GAS_WELLHEAD);
		add("refinery", REFINERY);
		add("refinery_column", REFINERY_COLUMN);
		add("cooling_tower", COOLING_TOWER);
		add("cooling_fill", COOLING_FILL);
		add("diesel_engine", DIESEL_ENGINE);
		add("gas_turbine", GAS_TURBINE);
		add("plastic_wire_hot", PLASTIC_WIRE_HOT);
		add("plastic_wire_neutral", PLASTIC_WIRE_NEUTRAL);
		add("mv_plastic_wire_hot", MV_PLASTIC_WIRE_HOT);
		add("mv_plastic_wire_neutral", MV_PLASTIC_WIRE_NEUTRAL);
		add("hv_plastic_wire_hot", HV_PLASTIC_WIRE_HOT);
		add("hv_plastic_wire_neutral", HV_PLASTIC_WIRE_NEUTRAL);
		add("wire_hot", WIRE_HOT);
		add("wire_neutral", WIRE_NEUTRAL);
		add("wool_wire_hot", WOOL_WIRE_HOT);
		add("wool_wire_neutral", WOOL_WIRE_NEUTRAL);
		add("mv_wire_hot", MV_WIRE_HOT);
		add("mv_wire_neutral", MV_WIRE_NEUTRAL);
		add("dist_wire_hot", DIST_WIRE_HOT);
		add("dist_wire_neutral", DIST_WIRE_NEUTRAL);
		add("dist_plastic_wire_hot", DIST_PLASTIC_WIRE_HOT);
		add("dist_plastic_wire_neutral", DIST_PLASTIC_WIRE_NEUTRAL);
		add("hv_wire_hot", HV_WIRE_HOT);
		add("hv_wire_neutral", HV_WIRE_NEUTRAL);
		add("ground_rod", GROUND_ROD);
		add("ground_wire", GROUND_WIRE);
		add("pole", POLE);
		add("crossarm", CROSSARM);
		add("breaker", BREAKER);
		for (PlugType type : PlugType.values()) {
			add("receptacle_" + type.id(), receptacleFor(type));
			add("plug_" + type.id(), plugFor(type));
		}
		add("fuse", FUSE);
		add("fused_cutout", FUSED_CUTOUT);
		add("insulator_lv", INSULATOR_LV);
		add("insulator_dist", INSULATOR_DIST);
		add("insulator_hv", INSULATOR_HV);
		add("switch", SWITCH);
		add("ufls_relay", UFLS_RELAY);
		add("mv_breaker", MV_BREAKER);
		add("dist_breaker", DIST_BREAKER);
		add("hv_breaker", HV_BREAKER);
		add("transformer", TRANSFORMER);
		add("substation_transformer", SUBSTATION_TRANSFORMER);
		add("distribution_transformer", DISTRIBUTION_TRANSFORMER);
		add("pole_transformer", POLE_TRANSFORMER);
		add("electric_lamp", ELECTRIC_LAMP);
		add("electric_heater", ELECTRIC_HEATER);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("boiler"), BOILER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("steam_turbine"), STEAM_TURBINE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("firebox"), FIREBOX_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("pump"), PUMP_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("drilling_rig"), DRILLING_RIG_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("pumpjack"), PUMPJACK_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("gas_wellhead"), GAS_WELLHEAD_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("refinery"), REFINERY_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("cooling_tower"), COOLING_TOWER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("engine"), ENGINE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("wire"), WIRE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("load"), LOAD_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("breaker"), BREAKER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("fuse"), FUSE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("receptacle"), RECEPTACLE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("plug_adapter"), PLUG_ADAPTER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("insulator"), INSULATOR_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("transformer"), TRANSFORMER_ENTITY);
	}

	/** Registers the block and its item. */
	private static void add(String name, Block block) {
		Registry.register(BuiltInRegistries.BLOCK, Wildcatter.id(name), block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Wildcatter.id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
	}
}
