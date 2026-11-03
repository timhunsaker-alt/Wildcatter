package dev.wildcatter;

import static dev.wildcatter.BlockFactory.*;

import dev.wildcatter.block.BreakerBlock;
import dev.wildcatter.block.FuseBlock;
import dev.wildcatter.block.InsulatorBlock;
import dev.wildcatter.energy.PlugType;
import dev.wildcatter.block.CrossarmBlock;
import dev.wildcatter.block.PoleBlock;
import dev.wildcatter.block.LoadBlock;
import dev.wildcatter.circuit.Conductor;
import net.minecraft.world.level.material.MapColor;
import dev.wildcatter.block.GroundRodBlock;
import dev.wildcatter.block.GroundWireBlock;
import dev.wildcatter.block.TransformerBlock;
import dev.wildcatter.block.ThreePhaseHeaterBlock;
import dev.wildcatter.block.MotorStarterBlock;
import dev.wildcatter.block.MccIncomingBlock;
import dev.wildcatter.energy.Voltage;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/** Poles, wire, cable, protection, plugs, transformers and the substation yard. Reached through {@link WildcatterBlocks}, which registers them. */
public interface GridBlocks {
	/** Each is a small 110 V set; eight of one kind in a 2x2x2 cube make a 4.16 kV generator. */

	/** An older world's one-block pole section and crossarm: they still hold their insulators, but are no longer made (4.5e). */
	Block POLE = block("pole", props -> new PoleBlock(props.mapColor(MapColor.WOOD)
			.strength(2.0F, 3.0F).sound(SoundType.WOOD).noOcclusion()));
	Block CROSSARM = block("crossarm", props -> new CrossarmBlock(props.mapColor(MapColor.WOOD)
			.strength(1.5F, 3.0F).sound(SoundType.WOOD).noOcclusion()));
	/** True-size wood poles and their hardware (docs/multiblocks/poles.md): 30, 40 and 75 ft, standing 8, 10 and 20 m. */
	Block WOOD_POLE_30 = block("wood_pole_30", props -> new dev.wildcatter.block.WoodPoleBlock(props.mapColor(MapColor.WOOD)
			.strength(2.0F, 3.0F).sound(SoundType.WOOD), 8, "wood_pole_30"));
	Block WOOD_POLE_40 = block("wood_pole_40", props -> new dev.wildcatter.block.WoodPoleBlock(props.mapColor(MapColor.WOOD)
			.strength(2.0F, 3.0F).sound(SoundType.WOOD), 10, "wood_pole_40"));
	Block WOOD_POLE_75 = block("wood_pole_75", props -> new dev.wildcatter.block.WoodPoleBlock(props.mapColor(MapColor.WOOD)
			.strength(2.5F, 4.0F).sound(SoundType.WOOD), 20, "wood_pole_75"));
	Block CROSSARM_DIST = block("crossarm_dist", props -> new dev.wildcatter.block.CrossarmPartBlock(props.mapColor(MapColor.WOOD)
			.strength(1.5F, 3.0F).sound(SoundType.WOOD)));
	Block CROSSBEAM_138 = block("crossbeam_138", props -> new dev.wildcatter.block.CrossbeamBlock(props.mapColor(MapColor.WOOD)
			.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
	Block X_BRACE_138 = block("x_brace_138", props -> new dev.wildcatter.block.XBraceBlock(props.mapColor(MapColor.WOOD)
			.strength(1.5F, 3.0F).sound(SoundType.WOOD)));

	// Wire. Every circuit needs two: a hot (red) one out to the load and a neutral (black) one back.
	// Bare wire shocks you while it is live; wool-wrapped wire is safe until the wool wears out.
	// Higher voltages are only bare for now: they get proper insulation once there is plastic.
	Block WIRE_HOT = cable("wire_hot", Voltage.LOW, Conductor.A, false);
	Block WIRE_NEUTRAL = cable("wire_neutral", Voltage.LOW, Conductor.N, false);
	Block WOOL_WIRE_HOT = cable("wool_wire_hot", Voltage.LOW, Conductor.A, true);
	Block WOOL_WIRE_NEUTRAL = cable("wool_wire_neutral", Voltage.LOW, Conductor.N, true);
	Block MV_WIRE_HOT = cable("mv_wire_hot", Voltage.MEDIUM, Conductor.A, false);
	Block MV_WIRE_NEUTRAL = cable("mv_wire_neutral", Voltage.MEDIUM, Conductor.N, false);
	Block DIST_WIRE_HOT = cable("dist_wire_hot", Voltage.DISTRIBUTION, Conductor.A, false);
	Block DIST_WIRE_NEUTRAL = cable("dist_wire_neutral", Voltage.DISTRIBUTION, Conductor.N, false);
	Block HV_WIRE_HOT = cable("hv_wire_hot", Voltage.HIGH, Conductor.A, false);
	Block HV_WIRE_NEUTRAL = cable("hv_wire_neutral", Voltage.HIGH, Conductor.N, false);

	// Plastic insulation never wears out, at any voltage: safe to touch, with no bare-line arc reach.
	Block PLASTIC_WIRE_HOT = plasticCable("plastic_wire_hot", Voltage.LOW, Conductor.A);
	Block PLASTIC_WIRE_NEUTRAL = plasticCable("plastic_wire_neutral", Voltage.LOW, Conductor.N);
	Block MV_PLASTIC_WIRE_HOT = plasticCable("mv_plastic_wire_hot", Voltage.MEDIUM, Conductor.A);
	Block MV_PLASTIC_WIRE_NEUTRAL = plasticCable("mv_plastic_wire_neutral", Voltage.MEDIUM, Conductor.N);
	Block DIST_PLASTIC_WIRE_HOT = plasticCable("dist_plastic_wire_hot", Voltage.DISTRIBUTION, Conductor.A);
	Block DIST_PLASTIC_WIRE_NEUTRAL = plasticCable("dist_plastic_wire_neutral", Voltage.DISTRIBUTION, Conductor.N);
	Block HV_PLASTIC_WIRE_HOT = plasticCable("hv_plastic_wire_hot", Voltage.HIGH, Conductor.A);
	Block HV_PLASTIC_WIRE_NEUTRAL = plasticCable("hv_plastic_wire_neutral", Voltage.HIGH, Conductor.N);
	Block INDUSTRIAL_WIRE_HOT = cable("industrial_wire_hot", Voltage.INDUSTRIAL, Conductor.A, false);
	Block INDUSTRIAL_WIRE_NEUTRAL = cable("industrial_wire_neutral", Voltage.INDUSTRIAL, Conductor.N, false);
	Block INDUSTRIAL_PLASTIC_WIRE_HOT = plasticCable("industrial_plastic_wire_hot", Voltage.INDUSTRIAL, Conductor.A);
	Block INDUSTRIAL_PLASTIC_WIRE_NEUTRAL = plasticCable("industrial_plastic_wire_neutral", Voltage.INDUSTRIAL, Conductor.N);

	/** Three-phase cable: A, B, C, N and G in one insulated block. */
	Block CABLE_LV = multiCable("cable_lv", Voltage.LOW);
	Block CABLE_INDUSTRIAL = multiCable("cable_industrial", Voltage.INDUSTRIAL);
	/** 4.16 kV (IEC 3.3 kV) cable: 5 kV class shielded MV cable, for a land rig's drive house or a plant's 4.16 kV feeders. */
	Block CABLE_MV = multiCable("cable_mv", Voltage.MEDIUM);
	Block CABLE_DIST = multiCable("cable_dist", Voltage.DISTRIBUTION);
	/** 138 kV (IEC 132 kV) cable: the getaway from a substation transformer's bushings to the line's three insulators. */
	Block CABLE_HV = multiCable("cable_hv", Voltage.HIGH);
	/** 800 A feeder busway (208Y/120 V and 480Y/277 V): A, B, C, N and G bars in one steel housing, for genset and switchgear runs. */
	Block BUSWAY_LV = busway("busway_lv", Voltage.LOW);
	Block BUSWAY_INDUSTRIAL = busway("busway_industrial", Voltage.INDUSTRIAL);

	/** A copper rod in the earth, for grounding machines. */
	Block GROUND_ROD = block("ground_rod", props -> new GroundRodBlock(props.mapColor(MapColor.COLOR_ORANGE)
			.strength(1.0F).sound(SoundType.COPPER).noOcclusion()));
	Block GROUND_WIRE = block("ground_wire", props -> new GroundWireBlock(props.mapColor(MapColor.COLOR_GREEN)
			.strength(0.3F).sound(SoundType.COPPER).noOcclusion()));

	Block BREAKER = block("breaker", props -> new BreakerBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(1.5F).sound(SoundType.METAL), Voltage.LOW));
	/** Opens and closes the hot wire by hand and protects nothing. */
	Block SWITCH = block("switch", props -> new BreakerBlock(props.mapColor(MapColor.COLOR_YELLOW)
			.strength(1.5F).sound(SoundType.METAL), Voltage.LOW, true));
	/** Opens its feeder when the island's frequency falls too far: load shedding. */
	Block UFLS_RELAY = block("ufls_relay", props -> new BreakerBlock(props.mapColor(MapColor.COLOR_PURPLE)
			.strength(1.5F).sound(SoundType.METAL), Voltage.LOW, false, true));
	/** A fused cutout: the 12.47 kV fuse on a pole. Its link drops open when it blows and a new link (a copper ingot) renews it. */
	Block FUSED_CUTOUT = block("fused_cutout", props -> new FuseBlock(props.mapColor(MapColor.TERRACOTTA_ORANGE)
			.strength(1.5F).sound(SoundType.GLASS), Voltage.DISTRIBUTION));
	/** Insulators on top of a pole or crossarm: one conductor each, rated for a tier of line. */
	Block INSULATOR_LV = block("insulator_lv", props -> new InsulatorBlock(props.mapColor(MapColor.TERRACOTTA_WHITE)
			.strength(1.0F).sound(SoundType.GLASS).noOcclusion(), Voltage.LOW));
	Block INSULATOR_DIST = block("insulator_dist", props -> new InsulatorBlock(props.mapColor(MapColor.TERRACOTTA_BROWN)
			.strength(1.0F).sound(SoundType.GLASS).noOcclusion(), Voltage.DISTRIBUTION));
	Block INSULATOR_HV = block("insulator_hv", props -> new InsulatorBlock(props.mapColor(MapColor.COLOR_LIGHT_GRAY)
			.strength(1.5F).sound(SoundType.GLASS).noOcclusion(), Voltage.HIGH));
	/** Receptacles and the plug adapters that fit them, one pair per type. */
	Block RECEPTACLE_NEMA_5_15 = receptacle("receptacle_nema_5_15", PlugType.NEMA_5_15);
	Block RECEPTACLE_NEMA_5_20 = receptacle("receptacle_nema_5_20", PlugType.NEMA_5_20);
	Block RECEPTACLE_NEMA_14_50 = receptacle("receptacle_nema_14_50", PlugType.NEMA_14_50);
	Block RECEPTACLE_CEE_7 = receptacle("receptacle_cee_7", PlugType.CEE_7);
	Block RECEPTACLE_IEC_60309_BLUE = receptacle("receptacle_iec_60309_blue", PlugType.IEC_60309_BLUE);
	Block PLUG_NEMA_5_15 = plug("plug_nema_5_15", PlugType.NEMA_5_15);
	Block PLUG_NEMA_5_20 = plug("plug_nema_5_20", PlugType.NEMA_5_20);
	Block PLUG_NEMA_14_50 = plug("plug_nema_14_50", PlugType.NEMA_14_50);
	Block PLUG_CEE_7 = plug("plug_cee_7", PlugType.CEE_7);
	Block PLUG_IEC_60309_BLUE = plug("plug_iec_60309_blue", PlugType.IEC_60309_BLUE);
	Block RECEPTACLE_NEMA_L21_30 = receptacle("receptacle_nema_l21_30", PlugType.NEMA_L21_30);
	Block RECEPTACLE_NEMA_L22_30 = receptacle("receptacle_nema_l22_30", PlugType.NEMA_L22_30);
	Block RECEPTACLE_IEC_60309_RED_32 = receptacle("receptacle_iec_60309_red_32", PlugType.IEC_60309_RED_32);
	Block RECEPTACLE_IEC_60309_RED_63 = receptacle("receptacle_iec_60309_red_63", PlugType.IEC_60309_RED_63);
	Block PLUG_NEMA_L21_30 = plug("plug_nema_l21_30", PlugType.NEMA_L21_30);
	Block PLUG_NEMA_L22_30 = plug("plug_nema_l22_30", PlugType.NEMA_L22_30);
	Block PLUG_IEC_60309_RED_32 = plug("plug_iec_60309_red_32", PlugType.IEC_60309_RED_32);
	Block PLUG_IEC_60309_RED_63 = plug("plug_iec_60309_red_63", PlugType.IEC_60309_RED_63);
	/** The distribution-tier under-frequency relay for feeders. */
	Block UFLS_RELAY_DIST = block("ufls_relay_dist", props -> new BreakerBlock(props.mapColor(MapColor.COLOR_PURPLE)
			.strength(3.5F, 7.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.DISTRIBUTION, false, true));
	/** One-shot protection: cheap, fast, and it has to be replaced after it blows. */
	Block FUSE = block("fuse", props -> new FuseBlock(props.mapColor(MapColor.COLOR_ORANGE).strength(1.0F).sound(SoundType.GLASS)));
	/** For 4.16 kV and 138 kV hot lines. Same rules as the 110 V breaker, with ratings to suit the current. */
	Block MV_BREAKER = block("mv_breaker", props -> new BreakerBlock(props.mapColor(MapColor.COLOR_BLUE)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.MEDIUM));
	/** For 12.47 kV feeders. */
	Block DIST_BREAKER = block("dist_breaker", props -> new BreakerBlock(props.mapColor(MapColor.COLOR_GREEN)
			.strength(3.5F, 7.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.DISTRIBUTION));
	Block HV_BREAKER = block("hv_breaker", props -> new BreakerBlock(props.mapColor(MapColor.COLOR_RED)
			.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.HIGH));
	/** Ganged three-pole breakers for three-phase runs. */
	Block BREAKER_3P_LV = block("breaker_3p_lv", props -> BreakerBlock.threePole(props.mapColor(MapColor.COLOR_GRAY)
			.strength(1.5F).sound(SoundType.METAL), Voltage.LOW));
	Block BREAKER_3P_INDUSTRIAL = block("breaker_3p_industrial", props -> BreakerBlock.threePole(props.mapColor(MapColor.COLOR_ORANGE)
			.strength(2.5F, 5.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.INDUSTRIAL));
	/** A combination motor starter, and the incoming section and buckets of a motor control center. */
	Block MOTOR_STARTER = block("motor_starter", props -> new MotorStarterBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(2.5F, 5.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), false));
	Block MCC_INCOMING = block("mcc_incoming", props -> new MccIncomingBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	/** An older world's one-block MCC bucket: it drops as a bucket item, which now clicks into a starter section. No item of its own. */
	Block MCC_BUCKET = block("mcc_bucket", props -> new MotorStarterBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), true));
	/** A motor control center's starter section at true size: three bucket spaces (docs/multiblocks/motor-control-center.md). */
	Block MCC_SECTION = block("mcc_section", props -> new dev.wildcatter.block.MccSectionBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	/** Soft starters and drives for big motors (docs/multiblocks/soft-starter.md, vfd.md). */
	Block SOFT_STARTER = block("soft_starter", props -> new dev.wildcatter.block.StarterCabinetBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), dev.wildcatter.multiblock.Footprint.box(1, 2, 1, 0, 0), "soft_starter",
			dev.wildcatter.block.Starter.Kind.SOFT, Voltage.INDUSTRIAL, Tuning.CABINET_SETTINGS_LV, false));
	Block DRIVE_CABINET = block("drive_cabinet", props -> new dev.wildcatter.block.StarterCabinetBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), dev.wildcatter.multiblock.Footprint.box(1, 2, 1, 0, 0), "drive_cabinet",
			dev.wildcatter.block.Starter.Kind.DRIVE, Voltage.INDUSTRIAL, Tuning.CABINET_SETTINGS_LV, false));
	Block SOFT_STARTER_MV = block("soft_starter_mv", props -> new dev.wildcatter.block.StarterCabinetBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(3.5F, 7.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), dev.wildcatter.multiblock.Footprint.box(1, 3, 1, 0, 0), "soft_starter_mv",
			dev.wildcatter.block.Starter.Kind.SOFT, Voltage.MEDIUM, Tuning.CABINET_SETTINGS_MV, false));
	Block DRIVE_MV_CONTROL = block("drive_mv_control", props -> new dev.wildcatter.block.StarterCabinetBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(3.5F, 7.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), dev.wildcatter.multiblock.Footprint.box(1, 3, 2, 0, 0), "drive_mv_control",
			dev.wildcatter.block.Starter.Kind.DRIVE, Voltage.MEDIUM, Tuning.CABINET_SETTINGS_MV, true));
	/** Bare copper DC bus bars on standoff insulators (docs/multiblocks/dc-bus.md). */
	Block DC_BUS_4K = block("dc_bus_4k", props -> new dev.wildcatter.block.DcBusBarBlock(props.mapColor(MapColor.COLOR_ORANGE)
			.strength(3.0F, 6.0F).sound(SoundType.COPPER).requiresCorrectToolForDrops().noOcclusion(), Tuning.DC_BUS_4K_AMPS, Tuning.DC_BUS_4K_OHMS_PER_M, "4 kA"));
	Block DC_BUS_16K = block("dc_bus_16k", props -> new dev.wildcatter.block.DcBusBarBlock(props.mapColor(MapColor.COLOR_ORANGE)
			.strength(3.0F, 6.0F).sound(SoundType.COPPER).requiresCorrectToolForDrops().noOcclusion(), Tuning.DC_BUS_16K_AMPS, Tuning.DC_BUS_16K_OHMS_PER_M, "16 kA"));
	Block DRIVE_MV_TRANSFORMER = block("drive_mv_transformer", props -> new dev.wildcatter.block.DriveSectionBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(3.5F, 7.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), dev.wildcatter.multiblock.Footprint.box(2, 3, 2, 0, 0), "drive_mv_transformer"));
	Block DRIVE_MV_CELLS = block("drive_mv_cells", props -> new dev.wildcatter.block.DriveSectionBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(3.5F, 7.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), dev.wildcatter.multiblock.Footprint.box(2, 3, 2, 0, 0), "drive_mv_cells"));
	Block THREE_PHASE_HEATER = block("three_phase_heater", props -> new ThreePhaseHeaterBlock(props.mapColor(MapColor.COLOR_RED)
			.strength(2.0F).sound(SoundType.METAL).lightLevel(state -> state.getValue(LoadBlock.LIT) ? 7 : 0), Voltage.INDUSTRIAL, Tuning.THREE_PHASE_HEATER_WATTS));
	Block ELECTRIC_LAMP = block("electric_lamp", props -> new LoadBlock(props.mapColor(MapColor.COLOR_YELLOW)
			.strength(0.5F).sound(SoundType.GLASS).lightLevel(state -> state.getValue(LoadBlock.LIT) ? 15 : 0), Tuning.LAMP_WATTS));
	Block ELECTRIC_HEATER = block("electric_heater", props -> new LoadBlock(props.mapColor(MapColor.COLOR_RED)
			.strength(1.0F).sound(SoundType.METAL).lightLevel(state -> state.getValue(LoadBlock.LIT) ? 7 : 0), Tuning.HEATER_WATTS));

	/** Steps 110 V up to 4.16 kV and back. */
	Block TRANSFORMER = block("transformer", props -> new TransformerBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.LOW, Voltage.MEDIUM));
	/** Steps 4.16 kV up to 138 kV and back. */
	/** 12.47 kV (7.2 kV to neutral) down to 120/240 V split-phase, 25 kVA; it hangs on a pole. */
	Block POLE_TRANSFORMER = block("pole_transformer", props -> new TransformerBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.LOW, Voltage.DISTRIBUTION, Tuning.POLE_TRANSFORMER_VA));
	// ---- The distribution substation (docs/multiblocks/substation.md): it replaces the old single-block 138/12.47 kV transformer ----
	Block POWER_TRANSFORMER = block("power_transformer", props -> new dev.wildcatter.block.PowerTransformerBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Tuning.SUBSTATION_VA, Tuning.SUBSTATION_PERCENT_Z));
	// the yard's parts at true size (4.5d): length along the line, height, width across it
	Block HV_BREAKER_3P = block("hv_breaker_3p", props -> new dev.wildcatter.block.BigBreakerBlock(props.mapColor(MapColor.COLOR_RED)
			.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.HIGH, 2, 5, 9, "hv_breaker_3p"));
	Block DIST_BREAKER_3P = block("dist_breaker_3p", props -> new dev.wildcatter.block.BigBreakerBlock(props.mapColor(MapColor.COLOR_GREEN)
			.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.DISTRIBUTION, 2, 4, 3, "dist_breaker_3p"));
	Block DISCONNECT_HV = block("disconnect_hv", props -> new dev.wildcatter.block.BigDisconnectBlock(props.mapColor(MapColor.METAL)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.HIGH, 2, 6, 9, "disconnect_hv", dev.wildcatter.block.BigDisconnectBlock::hvRotors));
	Block DISCONNECT_DIST = block("disconnect_dist", props -> new dev.wildcatter.block.BigDisconnectBlock(props.mapColor(MapColor.METAL)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.DISTRIBUTION, 1, 5, 3, "disconnect_dist", dev.wildcatter.block.BigDisconnectBlock::distRotors));
	Block BUS_HV = block("bus_hv", props -> new dev.wildcatter.block.StationBusBlock(props.mapColor(MapColor.METAL)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.HIGH, 2, 6, 9, "bus_hv"));
	Block BUS_DIST = block("bus_dist", props -> new dev.wildcatter.block.StationBusBlock(props.mapColor(MapColor.METAL)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.DISTRIBUTION, 1, 4, 3, "bus_dist"));
	/** Decoration: a substation's chain-link fence and its barbed-wire top, each a block; they join like iron bars. */
	Block CHAIN_LINK_FENCE = block("chain_link_fence", props -> new dev.wildcatter.block.ChainLinkFenceBlock(props.mapColor(MapColor.METAL)
			.strength(3.0F, 6.0F).sound(SoundType.CHAIN).requiresCorrectToolForDrops().noOcclusion()));
	Block BARBED_WIRE_TOP = block("barbed_wire_top", props -> new dev.wildcatter.block.ChainLinkFenceBlock(props.mapColor(MapColor.METAL)
			.strength(3.0F, 6.0F).sound(SoundType.CHAIN).requiresCorrectToolForDrops().noOcclusion()));
	Block RELAY_PANEL = block("relay_panel", props -> new dev.wildcatter.block.RelayPanelBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	/** Three-phase pad-mount transformers, on the ground and fed by underground cable. */
	Block PADMOUNT_208 = block("padmount_208", props -> dev.wildcatter.block.PadmountBlock.padmount(props.mapColor(MapColor.COLOR_GREEN)
			.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), "padmount_208", Voltage.LOW, Voltage.DISTRIBUTION, Tuning.PADMOUNT_208_VA, Tuning.PADMOUNT_208_PERCENT_Z));
	Block PADMOUNT_480 = block("padmount_480", props -> dev.wildcatter.block.PadmountBlock.padmount(props.mapColor(MapColor.COLOR_GREEN)
			.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), "padmount_480", Voltage.INDUSTRIAL, Voltage.DISTRIBUTION, Tuning.PADMOUNT_480_VA, Tuning.PADMOUNT_480_PERCENT_Z));
	/** A plant's unit substation: 4.16 kV down to 480 V. */
	/** The steam plant's generator step-up: 4.16 kV delta to 12.47 kV grounded wye (IEC 3.3 kV / 11 kV), 1,500 kVA. */
	Block GSU_TRANSFORMER = block("gsu_transformer", props -> new dev.wildcatter.block.GsuTransformerBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.MEDIUM, Voltage.DISTRIBUTION, Tuning.GSU_VA, Tuning.GSU_PERCENT_Z));
	Block UNIT_SUBSTATION = block("unit_substation", props -> dev.wildcatter.block.PadmountBlock.unitSubstation(props.mapColor(MapColor.COLOR_GRAY)
			.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.INDUSTRIAL, Voltage.MEDIUM, Tuning.UNIT_SUBSTATION_VA, Tuning.UNIT_SUBSTATION_PERCENT_Z));
	Block SUBSTATION_TRANSFORMER = block("substation_transformer", props -> new TransformerBlock(props.mapColor(MapColor.COLOR_BLUE)
			.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.MEDIUM, Voltage.HIGH));
	Block UNIT_AUX_TRANSFORMER = block("unit_auxiliary_transformer", props -> new dev.wildcatter.block.UatTransformerBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.MEDIUM, Voltage.GENERATOR, Tuning.UAT_VA, Tuning.UAT_PERCENT_Z));
	Block GSU_60 = block("gsu_60", props -> new dev.wildcatter.block.Gsu60TransformerBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.GENERATOR, Voltage.HIGH, Tuning.GSU60_VA, Tuning.GSU60_PERCENT_Z, false, "gsu_60"));
	Block SUBSTATION_TRANSFORMER_60 = block("substation_transformer_60", props -> new dev.wildcatter.block.Gsu60TransformerBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.DISTRIBUTION, Voltage.HIGH, Tuning.SUBSTATION60_VA, Tuning.SUBSTATION60_PERCENT_Z, true, "substation_transformer_60"));
	Block GSU_25 = block("gsu_25", props -> new dev.wildcatter.block.Gsu25TransformerBlock(props.mapColor(MapColor.COLOR_GRAY)
			.strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), Voltage.GENERATOR, Voltage.HIGH, Tuning.GSU25_VA, Tuning.GSU25_PERCENT_Z));
	Block BUS_DUCT_GEN = busway("bus_duct_gen", Voltage.GENERATOR);
}
