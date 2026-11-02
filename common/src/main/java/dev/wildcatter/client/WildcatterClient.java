package dev.wildcatter.client;

import dev.wildcatter.energy.ElectricalStandard;
import dev.wildcatter.energy.StandardData;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.Nullable;

/**
 * The client side, the same on every loader: the block entity renderers, the electrical standard selector on the Create World
 * screen (the choice is remembered until the new world loads, where it is stored with the world and can never change), and the
 * ghost outline. Each loader's client entry point calls these from its own events.
 */
public final class WildcatterClient {
	private WildcatterClient() {
	}

	/** A loader's way to register a block entity renderer (vanilla's {@code BlockEntityRenderers.register} has this shape). */
	public interface RendererSink {
		<T extends BlockEntity, S extends BlockEntityRenderState> void accept(BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> provider);
	}

	public static void init() {
		dev.wildcatter.kinetic.Sounding.Hook.start = MachineSound::start;
	}

	public static void renderers(RendererSink sink) {
		sink.accept(dev.wildcatter.WildcatterBlocks.INSULATOR_ENTITY, context -> new SpanRenderer());
		sink.accept(dev.wildcatter.WildcatterBlocks.NATURAL_DRAFT_TOWER_ENTITY, context -> new CoolingTowerRenderer());
		sink.accept(dev.wildcatter.WildcatterBlocks.SPENT_FUEL_POOL_ENTITY, context -> new SpentFuelPoolRenderer());
		sink.accept(dev.wildcatter.WildcatterBlocks.GENSET_PART_ENTITY, context -> new KineticRenderer<>(context));
		sink.accept(dev.wildcatter.WildcatterBlocks.COOLING_TOWER_ENTITY, context -> new KineticRenderer<>(context));
		sink.accept(dev.wildcatter.WildcatterBlocks.STEAM_PLANT_PART_ENTITY, context -> new KineticRenderer<>(context));
		sink.accept(dev.wildcatter.WildcatterBlocks.DISCONNECT_ENTITY, context -> new KineticRenderer<>(context));
		sink.accept(dev.wildcatter.WildcatterBlocks.PUMPJACK_ENTITY, context -> new KineticRenderer<>(context));
		sink.accept(dev.wildcatter.WildcatterBlocks.DRILLING_RIG_ENTITY, context -> new KineticRenderer<>(context));
		sink.accept(dev.wildcatter.WildcatterBlocks.STEAM_TURBINE_ENTITY, context -> new KineticRenderer<>(context));
		sink.accept(dev.wildcatter.WildcatterBlocks.PUMP_ENTITY, context -> new KineticRenderer<>(context));
		sink.accept(dev.wildcatter.WildcatterBlocks.RIG_SUBSTRUCTURE_ENTITY, context -> new KineticRenderer<>(context));
		sink.accept(dev.wildcatter.WildcatterBlocks.RIG_PART_ENTITY, context -> new KineticRenderer<>(context));
		sink.accept(dev.wildcatter.WildcatterBlocks.INJECTION_PUMP_ENTITY, context -> new KineticRenderer<>(context));
		sink.accept(dev.wildcatter.WildcatterBlocks.GAS_COMPRESSOR_ENTITY, context -> new KineticRenderer<>(context));
		sink.accept(dev.wildcatter.WildcatterBlocks.UNIT_MOTOR_ENTITY, context -> new KineticRenderer<>(context));
		sink.accept(dev.wildcatter.WildcatterBlocks.PE_REACTOR_ENTITY, context -> new KineticRenderer<>(context));
		sink.accept(dev.wildcatter.WildcatterBlocks.PELLETIZER_ENTITY, context -> new KineticRenderer<>(context));
	}

	/** Leaving a world: forget its machine loops. */
	public static void disconnected() {
		MachineSound.clearAll();
	}

	/** Draws the ghost outline; each loader calls it just before this frame's gizmos are collected. */
	public static void drawGizmos() {
		net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
		try (var gizmos = client.levelRenderer.collectPerFrameRenderThreadGizmos()) {
			GhostOutline.draw(client);
		}
	}

	/**
	 * A screen has opened with these widgets: on Create World, adds the standard selector with {@code add}. Returns what to run
	 * each tick while that screen is open, or null.
	 */
	public static @Nullable Runnable screenOpened(Screen screen, List<?> widgets, Consumer<AbstractWidget> add) {
		if (screen instanceof SelectWorldScreen) {
			ElectricalStandard.setPending(null);   // a cancelled creation must not leak into the next world
		} else if (screen instanceof CreateWorldScreen) {
			ElectricalStandard.setPending(StandardData.configDefault());
			// It sits on the Game tab, under the Allow Commands button, lined up with the world name field.
			EditBox nameField = null;
			for (Object widget : widgets) {
				if (widget instanceof EditBox box) {
					nameField = box;
				}
			}
			if (nameField == null) {
				return null;
			}
			EditBox anchor = nameField;
			CycleButton<ElectricalStandard> button = CycleButton.<ElectricalStandard>builder(WildcatterClient::label, ElectricalStandard::pending)
					.withValues(ElectricalStandard.values())
					.withTooltip(value -> Tooltip.create(Component.literal("The electrical standard of this world: its voltages, frequency and equipment. It cannot be changed after the world is created.")))
					.create(anchor.getX(), anchor.getY() + 112, anchor.getWidth(), 20, Component.literal("Grid"), (b, value) -> ElectricalStandard.setPending(value));
			add.accept(button);
			return () -> {
				button.setX(anchor.getX());
				button.setY(anchor.getY() + 112);
				button.visible = anchor.visible;   // only on the Game tab
			};
		}
		return null;
	}

	private static Component label(ElectricalStandard standard) {
		return Component.literal(standard.displayName() + " (" + (int) standard.hertz() + " Hz)");
	}
}
