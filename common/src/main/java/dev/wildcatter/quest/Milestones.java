package dev.wildcatter.quest;

import dev.wildcatter.Wildcatter;
import net.minecraft.resources.Identifier;

/**
 * The milestone keys Wildcatter's machines fire through {@link QuestEvents#milestone} (docs/quests.md, "Milestones Wildcatter fires").
 * Add-ons may use these keys in their own chapters' tasks. A formed multiblock fires {@code wildcatter:formed/<controller block>} through
 * {@code Multiblock.formed}.
 */
public final class Milestones {
	/** A steam turbine on a boiler is making power. */
	public static final Identifier STEAM_TURBINE_GENERATING = Wildcatter.id("steam_turbine_generating");
	/** An electric lamp is lit. */
	public static final Identifier LAMP_LIT = Wildcatter.id("lamp_lit");
	/** A genset, either kind, is running. */
	public static final Identifier GENSET_RUNNING = Wildcatter.id("genset_running");
	public static final Identifier DIESEL_GENSET_RUNNING = Wildcatter.id("diesel_genset_running");
	public static final Identifier GAS_GENSET_RUNNING = Wildcatter.id("gas_genset_running");
	/** A gas genset is running on biogas (another mod's digester gas on Wildcatter's steel pipe). */
	public static final Identifier GENSET_ON_BIOGAS = Wildcatter.id("genset_on_biogas");
	/** A three-phase motor has come up to speed and runs. */
	public static final Identifier MOTOR_RUNNING = Wildcatter.id("motor_running");
	/** A three-phase motor started from a motor starter (standalone, an MCC bucket or a starter cabinet) is up to speed. */
	public static final Identifier MOTOR_STARTED = Wildcatter.id("motor_started");
	/** A transformer's high side is live at its voltage. */
	public static final Identifier TRANSFORMER_ENERGISED = Wildcatter.id("transformer_energised");
	/** A pole-mount transformer's high side is live at its voltage (not a dry-type, pad-mount or substation unit). */
	public static final Identifier POLE_TRANSFORMER_ENERGISED = Wildcatter.id("pole_transformer_energised");
	/** A pumpjack's motor is running and it is lifting well fluid. */
	public static final Identifier PUMPJACK_RUNNING = Wildcatter.id("pumpjack_running");
	/** A drilling rig has reached an oil or gas field (a strike, not a dry hole). */
	public static final Identifier WELL_STRUCK = Wildcatter.id("well_struck");
	/** 23a: a land rig has set and cemented intermediate casing. */
	public static final Identifier CASING_SET = Wildcatter.id("casing_set");
	/** 23a: a land rig has run a leak-off test on its shoe. */
	public static final Identifier LOT_DONE = Wildcatter.id("lot_done");
	/** 23a: a land rig is drilling on oil mud whose emulsion holds. */
	public static final Identifier OIL_MUD_DRILLING = Wildcatter.id("oil_mud_drilling");
	/** 23b: a kick killed through the choke manifold: WELL DEAD. */
	public static final Identifier WELL_KILLED_CHOKE = Wildcatter.id("well_killed_choke");
	/** 23c: a storage cavern debrined dry. */
	public static final Identifier CAVERN_DRY = Wildcatter.id("cavern_dry");
	/** 23d: a Frasch wellhead lifting molten sulfur. */
	public static final Identifier FRASCH_FLOWING = Wildcatter.id("frasch_flowing");
	/** A crude unit (the topping unit or the atmospheric column) is refining. */
	public static final Identifier CRUDE_UNIT_RUNNING = Wildcatter.id("crude_unit_running");
	/** The gas plant's NGL chiller is chilling dried gas. */
	public static final Identifier GAS_PLANT_RUNNING = Wildcatter.id("gas_plant_running");
	/** The pelletizer is making plastic sheets. */
	public static final Identifier PELLETIZER_RUNNING = Wildcatter.id("pelletizer_running");
	/** The reactor is built and critical. */
	public static final Identifier REACTOR_CRITICAL = Wildcatter.id("reactor_critical");
	/** The nuclear turbine-generator's breaker is closed and it delivers power. */
	public static final Identifier TURBINE_ON_LINE = Wildcatter.id("turbine_on_line");

	/** 22b: a battery block (or cabinet) delivers 1 MW or more to the grid. */
	public static final Identifier BESS_DISCHARGING = Wildcatter.id("bess_discharging");
	/** 22c: a grid-forming battery closes onto a dead bus (a black start). */
	public static final Identifier BESS_BLACK_START = Wildcatter.id("bess_black_start");

	private Milestones() {
	}

	/** The key a formed multiblock fires: {@code <namespace>:formed/<controller block path>}. */
	public static Identifier formed(Identifier controller) {
		return Identifier.fromNamespaceAndPath(controller.getNamespace(), "formed/" + controller.getPath());
	}
}
