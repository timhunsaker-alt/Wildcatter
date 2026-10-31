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
import dev.wildcatter.block.MultiCableBlock;
import dev.wildcatter.block.MultiCableBlockEntity;
import dev.wildcatter.block.ThreePhaseHeaterBlock;
import dev.wildcatter.block.MotorStarterBlock;
import dev.wildcatter.block.MotorStarterBlockEntity;
import dev.wildcatter.block.MccIncomingBlock;
import dev.wildcatter.block.MccIncomingBlockEntity;
import dev.wildcatter.block.ThreePhaseHeaterBlockEntity;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.block.SteamTurbineBlockEntity;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import dev.wildcatter.energy.ConductorSize;
import dev.wildcatter.item.ConductorItem;
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
	/** The foot and top of the cooling-tower column (docs/multiblocks/cooling-tower.md). */
	public static final Block TOWER_BASIN = block("tower_basin", props -> new dev.wildcatter.block.TowerBasinBlock(props.mapColor(MapColor.STONE)
			.strength(2.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
	public static final Block DRIFT_ELIMINATOR = block("drift_eliminator", props -> new Block(props.mapColor(MapColor.STONE)
			.strength(1.5F, 3.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	public static final Block COOLING_FILL = block("cooling_fill", props -> new CoolingFillBlock(props.mapColor(MapColor.STONE)
			.strength(2.0F, 4.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
	/** Each is a small 110 V set; eight of one kind in a 2x2x2 cube make a 4.16 kV generator. */

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
	public static final Block INDUSTRIAL_WIRE_HOT = cable("industrial_wire_hot", Voltage.INDUSTRIAL, Conductor.A, false);
	public static final Block INDUSTRIAL_WIRE_NEUTRAL = cable("industrial_wire_neutral", Voltage.INDUSTRIAL, Conductor.N, false);
	public static final Block INDUSTRIAL_PLASTIC_WIRE_HOT = plasticCable("industrial_plastic_wire_hot", Voltage.INDUSTRIAL, Conductor.A);
	public static final Block INDUSTRIAL_PLASTIC_WIRE_NEUTRAL = plasticCable("industrial_plastic_wire_neutral", Voltage.INDUSTRIAL, Conductor.N);

	/** Three-phase cable: A, B, C, N and G in one insulated block. */
	public static final Block CABLE_LV = multiCable("cable_lv", Voltage.LOW);
	public static final Block CABLE_INDUSTRIAL = multiCable("cable_industrial", Voltage.INDUSTRIAL);
	public static final Block CABLE_DIST = multiCable("cable_dist", Voltage.DISTRIBUTION);
	/** 138 kV (IEC 132 kV) cable: the getaway from a substation transformer's bushings to the line's three insulators. */
	public static final Block CABLE_HV = multiCable("cable_hv", Voltage.HIGH);
	/** 800 A feeder busway (208Y/120 V and 480Y/277 V): A, B, C, N and G bars in one steel housing, for genset and switchgear runs. */
	public static final Block BUSWAY_LV = busway("busway_lv", Voltage.LOW);
	public static final Block BUSWAY_INDUSTRIAL = busway("busway_industrial", Voltage.INDUSTRIAL);

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
	public static final Block RECEPTACLE_NEMA_L21_30 = receptacle("receptacle_nema_l21_30", PlugType.NEMA_L21_30);
	public static final Block RECEPTACLE_NEMA_L22_30 = receptacle("receptacle_nema_l22_30", PlugType.NEMA_L22_30);
	public static final Block RECEPTACLE_IEC_60309_RED_32 = receptacle("receptacle_iec_60309_red_32", PlugType.IEC_60309_RED_32);
	public static final Block RECEPTACLE_IEC_60309_RED_63 = receptacle("receptacle_iec_60309_red_63", PlugType.IEC_60309_RED_63);
	public static final Block PLUG_NEMA_L21_30 = plug("plug_nema_l21_30", PlugType.NEMA_L21_30);
	public static final Block PLUG_NEMA_L22_30 = plug("plug_nema_l22_30", PlugType.NEMA_L22_30);
	public static final Block PLUG_IEC_60309_RED_32 = plug("plug_iec_60309_red_32", PlugType.IEC_60309_RED_32);
	public static final Block PLUG_IEC_60309_RED_63 = plug("plug_iec_60309_red_63", PlugType.IEC_60309_RED_63);
	/** The distribution-tier under-frequency relay for feeders. */
	public static final Block UFLS_RELAY_DIST = block("ufls_relay_dist", props -> new BreakerBlock(props.mapColor(MapColor.COLOR_PURPLE)
			.strength(3.5F, 7.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.DISTRIBUTION, false, true));
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
	/** Ganged three-pole breakers for three-phase runs. */
	public static final Block BREAKER_3P_LV = block("breaker_3p_lv", props -> BreakerBlock.threePole(props.mapColor(MapColor.COLOR_GRAY)
			.strength(1.5F).sound(SoundType.METAL), Voltage.LOW));
	public static final Block BREAKER_3P_INDUSTRIAL = block("breaker_3p_industrial", props -> BreakerBlock.threePole(props.mapColor(MapColor.COLOR_ORANGE)
			.strength(2.5F, 5.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.INDUSTRIAL));
	/** A combination motor starter, and the incoming section and buckets of a motor control center. */
	public static final Block MOTOR_STARTER = block("motor_starter", props -> new MotorStarterBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(2.5F, 5.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), false));
	public static final Block MCC_INCOMING = block("mcc_incoming", props -> new MccIncomingBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	public static final Block MCC_BUCKET = block("mcc_bucket", props -> new MotorStarterBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), true));
	public static final Block THREE_PHASE_HEATER = block("three_phase_heater", props -> new ThreePhaseHeaterBlock(props.mapColor(MapColor.COLOR_RED)
			.strength(2.0F).sound(SoundType.METAL).lightLevel(state -> state.getValue(LoadBlock.LIT) ? 7 : 0), Voltage.INDUSTRIAL, Tuning.THREE_PHASE_HEATER_WATTS));
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
	// ---- The distribution substation (docs/multiblocks/substation.md): it replaces the old single-block 138/12.47 kV transformer ----
	public static final Block POWER_TRANSFORMER = block("power_transformer", props -> new dev.wildcatter.block.PowerTransformerBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Tuning.SUBSTATION_VA, Tuning.SUBSTATION_PERCENT_Z));
	public static final Block HV_BREAKER_3P = block("hv_breaker_3p", props -> BreakerBlock.relayOperated(props.mapColor(MapColor.COLOR_RED)
			.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.HIGH));
	public static final Block DIST_BREAKER_3P = block("dist_breaker_3p", props -> BreakerBlock.relayOperated(props.mapColor(MapColor.COLOR_GREEN)
			.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.DISTRIBUTION));
	public static final Block DISCONNECT_HV = block("disconnect_hv", props -> new dev.wildcatter.block.DisconnectSwitchBlock(props.mapColor(MapColor.METAL)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), Voltage.HIGH));
	public static final Block DISCONNECT_DIST = block("disconnect_dist", props -> new dev.wildcatter.block.DisconnectSwitchBlock(props.mapColor(MapColor.METAL)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), Voltage.DISTRIBUTION));
	public static final Block BUS_HV = busway("bus_hv", Voltage.HIGH);
	public static final Block BUS_DIST = busway("bus_dist", Voltage.DISTRIBUTION);
	public static final Block RELAY_PANEL = block("relay_panel", props -> new dev.wildcatter.block.RelayPanelBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	/** Three-phase pad-mount transformers, on the ground and fed by underground cable. */
	public static final Block PADMOUNT_208 = block("padmount_208", props -> TransformerBlock.threePhase(props.mapColor(MapColor.COLOR_GREEN)
			.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.LOW, Voltage.DISTRIBUTION, Tuning.PADMOUNT_208_VA, Tuning.PADMOUNT_208_PERCENT_Z));
	public static final Block PADMOUNT_480 = block("padmount_480", props -> TransformerBlock.threePhase(props.mapColor(MapColor.COLOR_GREEN)
			.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.INDUSTRIAL, Voltage.DISTRIBUTION, Tuning.PADMOUNT_480_VA, Tuning.PADMOUNT_480_PERCENT_Z));
	/** A plant's unit substation: 4.16 kV down to 480 V. */
	/** The steam plant's generator step-up: 4.16 kV delta to 12.47 kV grounded wye (IEC 3.3 kV / 11 kV), 1,500 kVA. */
	public static final Block GSU_TRANSFORMER = block("gsu_transformer", props -> new dev.wildcatter.block.GsuTransformerBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.MEDIUM, Voltage.DISTRIBUTION, Tuning.GSU_VA, Tuning.GSU_PERCENT_Z));
	public static final Block UNIT_SUBSTATION = block("unit_substation", props -> TransformerBlock.threePhase(props.mapColor(MapColor.COLOR_GRAY)
			.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.INDUSTRIAL, Voltage.MEDIUM, Tuning.UNIT_SUBSTATION_VA, Tuning.UNIT_SUBSTATION_PERCENT_Z));
	public static final Block SUBSTATION_TRANSFORMER = block("substation_transformer", props -> new TransformerBlock(props.mapColor(MapColor.COLOR_BLUE)
			.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.MEDIUM, Voltage.HIGH));

	// ---- Gensets (docs/multiblocks/genset.md) ----
	public static final Block GENSET_CONTROL_PANEL = block("genset_control_panel", props -> new dev.wildcatter.block.GensetControlPanelBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	public static final Block DIESEL_ENGINE_PART = gensetPart("genset_diesel_engine", dev.wildcatter.block.GensetPartBlock.Part.DIESEL_ENGINE);
	public static final Block GAS_ENGINE_PART = gensetPart("genset_gas_engine", dev.wildcatter.block.GensetPartBlock.Part.GAS_ENGINE);
	public static final Block ALTERNATOR = gensetPart("alternator", dev.wildcatter.block.GensetPartBlock.Part.ALTERNATOR);
	public static final Block RADIATOR = gensetPart("radiator", dev.wildcatter.block.GensetPartBlock.Part.RADIATOR);
	public static final Block FUEL_TANK = gensetPart("fuel_tank", dev.wildcatter.block.GensetPartBlock.Part.FUEL_TANK);
	public static final Block EXHAUST_STACK = gensetPart("exhaust_stack", dev.wildcatter.block.GensetPartBlock.Part.EXHAUST);
	// ---- The 1 MW steam plant (docs/multiblocks/steam-plant.md) ----
	public static final Block TURBINE_GENERATOR = block("turbine_generator", props -> new dev.wildcatter.block.TurbineGeneratorBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	public static final Block BOILER_BURNER = plantPart("boiler_burner", dev.wildcatter.block.SteamPlantPartBlock.Part.BURNER);
	public static final Block BOILER_DRUM = plantPart("boiler_drum", dev.wildcatter.block.SteamPlantPartBlock.Part.DRUM);
	public static final Block SUPERHEATER = plantPart("superheater", dev.wildcatter.block.SteamPlantPartBlock.Part.SUPERHEATER);
	public static final Block TURBINE_CASING = plantPart("turbine_casing", dev.wildcatter.block.SteamPlantPartBlock.Part.TURBINE);
	public static final Block CONDENSER = plantPart("condenser", dev.wildcatter.block.SteamPlantPartBlock.Part.CONDENSER);
	public static final Block FEED_PUMP = plantPart("feed_pump", dev.wildcatter.block.SteamPlantPartBlock.Part.FEED_PUMP);
	/** Never placed: its states carry the models of the turning parts (kinetic/RotorBlock). */
	public static final Block ROTOR = block("rotor", props -> new dev.wildcatter.kinetic.RotorBlock(props.noOcclusion().noLootTable()));

	public static final BlockEntityType<BoilerBlockEntity> BOILER_ENTITY = entity(BoilerBlockEntity::new, BOILER);
	public static final BlockEntityType<SteamTurbineBlockEntity> STEAM_TURBINE_ENTITY = entity(SteamTurbineBlockEntity::new, STEAM_TURBINE);
	public static final BlockEntityType<WireBlockEntity> WIRE_ENTITY = entity(WireBlockEntity::new, DIST_WIRE_HOT, DIST_WIRE_NEUTRAL, DIST_PLASTIC_WIRE_HOT, DIST_PLASTIC_WIRE_NEUTRAL, WIRE_HOT, WIRE_NEUTRAL, WOOL_WIRE_HOT,
			WOOL_WIRE_NEUTRAL, MV_WIRE_HOT, MV_WIRE_NEUTRAL, HV_WIRE_HOT, HV_WIRE_NEUTRAL,
			PLASTIC_WIRE_HOT, PLASTIC_WIRE_NEUTRAL, MV_PLASTIC_WIRE_HOT, MV_PLASTIC_WIRE_NEUTRAL, HV_PLASTIC_WIRE_HOT, HV_PLASTIC_WIRE_NEUTRAL,
			INDUSTRIAL_WIRE_HOT, INDUSTRIAL_WIRE_NEUTRAL, INDUSTRIAL_PLASTIC_WIRE_HOT, INDUSTRIAL_PLASTIC_WIRE_NEUTRAL);
	public static final BlockEntityType<MultiCableBlockEntity> MULTI_CABLE_ENTITY = entity(MultiCableBlockEntity::new, CABLE_LV, CABLE_INDUSTRIAL, CABLE_DIST, CABLE_HV, BUSWAY_LV, BUSWAY_INDUSTRIAL, BUS_HV, BUS_DIST);
	public static final BlockEntityType<MotorStarterBlockEntity> MOTOR_STARTER_ENTITY = entity(MotorStarterBlockEntity::new, MOTOR_STARTER, MCC_BUCKET);
	public static final BlockEntityType<MccIncomingBlockEntity> MCC_INCOMING_ENTITY = entity(MccIncomingBlockEntity::new, MCC_INCOMING);
	public static final BlockEntityType<dev.wildcatter.block.GensetPartBlockEntity> GENSET_PART_ENTITY = entity(dev.wildcatter.block.GensetPartBlockEntity::new,
			DIESEL_ENGINE_PART, GAS_ENGINE_PART, ALTERNATOR, RADIATOR, FUEL_TANK, EXHAUST_STACK);
	public static final BlockEntityType<dev.wildcatter.block.SteamPlantPartBlockEntity> STEAM_PLANT_PART_ENTITY = entity(dev.wildcatter.block.SteamPlantPartBlockEntity::new,
			BOILER_BURNER, BOILER_DRUM, SUPERHEATER, TURBINE_CASING, CONDENSER, FEED_PUMP);
	public static final BlockEntityType<dev.wildcatter.block.TurbineGeneratorBlockEntity> TURBINE_GENERATOR_ENTITY = entity(dev.wildcatter.block.TurbineGeneratorBlockEntity::new, TURBINE_GENERATOR);
	public static final BlockEntityType<dev.wildcatter.block.GensetControlPanelBlockEntity> GENSET_CONTROL_PANEL_ENTITY = entity(dev.wildcatter.block.GensetControlPanelBlockEntity::new, GENSET_CONTROL_PANEL);
	public static final BlockEntityType<ThreePhaseHeaterBlockEntity> THREE_PHASE_HEATER_ENTITY = entity(ThreePhaseHeaterBlockEntity::new, THREE_PHASE_HEATER);
	public static final BlockEntityType<LoadBlockEntity> LOAD_ENTITY = entity(LoadBlockEntity::new, ELECTRIC_LAMP, ELECTRIC_HEATER);
	public static final BlockEntityType<BreakerBlockEntity> BREAKER_ENTITY = entity(BreakerBlockEntity::new, BREAKER, MV_BREAKER, DIST_BREAKER, HV_BREAKER, SWITCH, UFLS_RELAY, UFLS_RELAY_DIST,
			BREAKER_3P_LV, BREAKER_3P_INDUSTRIAL, HV_BREAKER_3P, DIST_BREAKER_3P);
	public static final BlockEntityType<FuseBlockEntity> FUSE_ENTITY = entity(FuseBlockEntity::new, FUSE, FUSED_CUTOUT);
	public static final BlockEntityType<ReceptacleBlockEntity> RECEPTACLE_ENTITY = entity(ReceptacleBlockEntity::new, RECEPTACLE_NEMA_5_15, RECEPTACLE_NEMA_5_20, RECEPTACLE_NEMA_14_50, RECEPTACLE_CEE_7, RECEPTACLE_IEC_60309_BLUE,
			RECEPTACLE_NEMA_L21_30, RECEPTACLE_NEMA_L22_30, RECEPTACLE_IEC_60309_RED_32, RECEPTACLE_IEC_60309_RED_63);
	public static final BlockEntityType<PlugAdapterBlockEntity> PLUG_ADAPTER_ENTITY = entity(PlugAdapterBlockEntity::new, PLUG_NEMA_5_15, PLUG_NEMA_5_20, PLUG_NEMA_14_50, PLUG_CEE_7, PLUG_IEC_60309_BLUE,
			PLUG_NEMA_L21_30, PLUG_NEMA_L22_30, PLUG_IEC_60309_RED_32, PLUG_IEC_60309_RED_63);
	public static final BlockEntityType<InsulatorBlockEntity> INSULATOR_ENTITY = entity(InsulatorBlockEntity::new, INSULATOR_LV, INSULATOR_DIST, INSULATOR_HV);
	public static final BlockEntityType<dev.wildcatter.block.PowerTransformerBlockEntity> POWER_TRANSFORMER_ENTITY = entity(dev.wildcatter.block.PowerTransformerBlockEntity::new, POWER_TRANSFORMER);
	public static final BlockEntityType<dev.wildcatter.block.DisconnectSwitchBlockEntity> DISCONNECT_ENTITY = entity(dev.wildcatter.block.DisconnectSwitchBlockEntity::new, DISCONNECT_HV, DISCONNECT_DIST);
	public static final BlockEntityType<TransformerBlockEntity> TRANSFORMER_ENTITY = entity(TransformerBlockEntity::new, TRANSFORMER, SUBSTATION_TRANSFORMER, POLE_TRANSFORMER,
			PADMOUNT_208, PADMOUNT_480, UNIT_SUBSTATION, GSU_TRANSFORMER);

	public static final BlockEntityType<FireboxBlockEntity> FIREBOX_ENTITY = entity(FireboxBlockEntity::new, FIREBOX);
	public static final BlockEntityType<PumpBlockEntity> PUMP_ENTITY = entity(PumpBlockEntity::new, PUMP);

	public static final BlockEntityType<DrillingRigBlockEntity> DRILLING_RIG_ENTITY = entity(DrillingRigBlockEntity::new, DRILLING_RIG);
	public static final BlockEntityType<PumpjackBlockEntity> PUMPJACK_ENTITY = entity(PumpjackBlockEntity::new, PUMPJACK);
	public static final BlockEntityType<GasWellheadBlockEntity> GAS_WELLHEAD_ENTITY = entity(GasWellheadBlockEntity::new, GAS_WELLHEAD);

	public static final BlockEntityType<RefineryBlockEntity> REFINERY_ENTITY = entity(RefineryBlockEntity::new, REFINERY);
	public static final BlockEntityType<dev.wildcatter.block.TowerBasinBlockEntity> TOWER_BASIN_ENTITY = entity(dev.wildcatter.block.TowerBasinBlockEntity::new, TOWER_BASIN);
	public static final BlockEntityType<CoolingTowerBlockEntity> COOLING_TOWER_ENTITY = entity(CoolingTowerBlockEntity::new, COOLING_TOWER);

	private WildcatterBlocks() {
	}

	public static Block receptacleFor(PlugType type) {
		return switch (type) {
			case NEMA_5_15 -> RECEPTACLE_NEMA_5_15;
			case NEMA_5_20 -> RECEPTACLE_NEMA_5_20;
			case NEMA_14_50 -> RECEPTACLE_NEMA_14_50;
			case CEE_7 -> RECEPTACLE_CEE_7;
			case IEC_60309_BLUE -> RECEPTACLE_IEC_60309_BLUE;
			case NEMA_L21_30 -> RECEPTACLE_NEMA_L21_30;
			case NEMA_L22_30 -> RECEPTACLE_NEMA_L22_30;
			case IEC_60309_RED_32 -> RECEPTACLE_IEC_60309_RED_32;
			case IEC_60309_RED_63 -> RECEPTACLE_IEC_60309_RED_63;
		};
	}

	public static Block plugFor(PlugType type) {
		return switch (type) {
			case NEMA_5_15 -> PLUG_NEMA_5_15;
			case NEMA_5_20 -> PLUG_NEMA_5_20;
			case NEMA_14_50 -> PLUG_NEMA_14_50;
			case CEE_7 -> PLUG_CEE_7;
			case IEC_60309_BLUE -> PLUG_IEC_60309_BLUE;
			case NEMA_L21_30 -> PLUG_NEMA_L21_30;
			case NEMA_L22_30 -> PLUG_NEMA_L22_30;
			case IEC_60309_RED_32 -> PLUG_IEC_60309_RED_32;
			case IEC_60309_RED_63 -> PLUG_IEC_60309_RED_63;
		};
	}

	private static Block receptacle(String name, PlugType type) {
		return block(name, props -> new ReceptacleBlock(props.mapColor(MapColor.COLOR_LIGHT_GRAY).strength(1.0F).sound(SoundType.METAL).noOcclusion(), type));
	}

	private static Block plug(String name, PlugType type) {
		return block(name, props -> new PlugAdapterBlock(props.mapColor(MapColor.COLOR_BLACK).strength(1.0F).sound(SoundType.METAL).noOcclusion(), type));
	}

	private static Block multiCable(String name, Voltage voltage) {
		return block(name, props -> new MultiCableBlock(props.mapColor(MapColor.COLOR_BLACK).strength(0.8F).sound(SoundType.WOOL).noOcclusion(), voltage, false));
	}

	private static Block gensetPart(String name, dev.wildcatter.block.GensetPartBlock.Part part) {
		return block(name, props -> new dev.wildcatter.block.GensetPartBlock(props.mapColor(MapColor.COLOR_GRAY)
				.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	private static Block plantPart(String name, dev.wildcatter.block.SteamPlantPartBlock.Part part) {
		return block(name, props -> new dev.wildcatter.block.SteamPlantPartBlock(props.mapColor(MapColor.COLOR_GRAY)
				.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), part));
	}

	private static Block busway(String name, Voltage voltage) {
		return block(name, props -> new MultiCableBlock(props.mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops(), voltage, true));
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
		add("tower_basin", TOWER_BASIN);
		add("drift_eliminator", DRIFT_ELIMINATOR);
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
		add("ufls_relay_dist", UFLS_RELAY_DIST);
		add("mv_breaker", MV_BREAKER);
		add("dist_breaker", DIST_BREAKER);
		add("hv_breaker", HV_BREAKER);
		add("transformer", TRANSFORMER);
		add("substation_transformer", SUBSTATION_TRANSFORMER);
		add("power_transformer", POWER_TRANSFORMER);
		add("hv_breaker_3p", HV_BREAKER_3P);
		add("dist_breaker_3p", DIST_BREAKER_3P);
		add("disconnect_hv", DISCONNECT_HV);
		add("disconnect_dist", DISCONNECT_DIST);
		add("bus_hv", BUS_HV);
		add("bus_dist", BUS_DIST);
		add("relay_panel", RELAY_PANEL);
		add("pole_transformer", POLE_TRANSFORMER);
		add("electric_lamp", ELECTRIC_LAMP);
		add("electric_heater", ELECTRIC_HEATER);
		add("industrial_wire_hot", INDUSTRIAL_WIRE_HOT);
		add("industrial_wire_neutral", INDUSTRIAL_WIRE_NEUTRAL);
		add("industrial_plastic_wire_hot", INDUSTRIAL_PLASTIC_WIRE_HOT);
		add("industrial_plastic_wire_neutral", INDUSTRIAL_PLASTIC_WIRE_NEUTRAL);
		add("cable_lv", CABLE_LV);
		add("cable_industrial", CABLE_INDUSTRIAL);
		add("cable_dist", CABLE_DIST);
		add("cable_hv", CABLE_HV);
		add("busway_lv", BUSWAY_LV);
		add("busway_industrial", BUSWAY_INDUSTRIAL);
		add("breaker_3p_lv", BREAKER_3P_LV);
		add("breaker_3p_industrial", BREAKER_3P_INDUSTRIAL);
		add("three_phase_heater", THREE_PHASE_HEATER);
		add("motor_starter", MOTOR_STARTER);
		add("mcc_incoming", MCC_INCOMING);
		add("genset_control_panel", GENSET_CONTROL_PANEL);
		add("genset_diesel_engine", DIESEL_ENGINE_PART);
		add("genset_gas_engine", GAS_ENGINE_PART);
		add("alternator", ALTERNATOR);
		add("radiator", RADIATOR);
		add("fuel_tank", FUEL_TANK);
		add("exhaust_stack", EXHAUST_STACK);
		add("turbine_generator", TURBINE_GENERATOR);
		add("boiler_burner", BOILER_BURNER);
		add("boiler_drum", BOILER_DRUM);
		add("superheater", SUPERHEATER);
		add("turbine_casing", TURBINE_CASING);
		add("condenser", CONDENSER);
		add("feed_pump", FEED_PUMP);
		Registry.register(BuiltInRegistries.BLOCK, Wildcatter.id("rotor"), ROTOR);   // no item: it is never placed
		add("mcc_bucket", MCC_BUCKET);
		add("padmount_208", PADMOUNT_208);
		add("padmount_480", PADMOUNT_480);
		add("unit_substation", UNIT_SUBSTATION);
		add("gsu_transformer", GSU_TRANSFORMER);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("boiler"), BOILER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("steam_turbine"), STEAM_TURBINE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("firebox"), FIREBOX_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("pump"), PUMP_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("drilling_rig"), DRILLING_RIG_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("pumpjack"), PUMPJACK_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("gas_wellhead"), GAS_WELLHEAD_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("refinery"), REFINERY_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("cooling_tower"), COOLING_TOWER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("wire"), WIRE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("load"), LOAD_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("breaker"), BREAKER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("fuse"), FUSE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("receptacle"), RECEPTACLE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("plug_adapter"), PLUG_ADAPTER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("insulator"), INSULATOR_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("transformer"), TRANSFORMER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("multi_cable"), MULTI_CABLE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("three_phase_heater"), THREE_PHASE_HEATER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("motor_starter"), MOTOR_STARTER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("mcc_incoming"), MCC_INCOMING_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("genset_part"), GENSET_PART_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("tower_basin"), TOWER_BASIN_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("power_transformer"), POWER_TRANSFORMER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("disconnect_switch"), DISCONNECT_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("steam_plant_part"), STEAM_PLANT_PART_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("turbine_generator"), TURBINE_GENERATOR_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Wildcatter.id("genset_control_panel"), GENSET_CONTROL_PANEL_ENTITY);
	}

	/** Registers the block and its item. */
	private static void add(String name, Block block) {
		Registry.register(BuiltInRegistries.BLOCK, Wildcatter.id(name), block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Wildcatter.id(name));
		Item.Properties props = new Item.Properties().setId(itemKey).useBlockDescriptionPrefix();
		Item item = block instanceof CableBlock wire ? new ConductorItem(block, props, wire.voltage(), ConductorSize.Kind.WIRE)
				: block instanceof MultiCableBlock cable ? new ConductorItem(block, props, cable.voltage(), cable.busway() ? ConductorSize.Kind.BUSWAY : ConductorSize.Kind.CABLE)
				: new BlockItem(block, props);
		Registry.register(BuiltInRegistries.ITEM, itemKey, item);
	}
}
