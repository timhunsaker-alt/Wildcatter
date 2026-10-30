package dev.wildcatter.client;

import dev.wildcatter.energy.ElectricalStandard;
import dev.wildcatter.energy.StandardData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

/**
 * Adds the electrical standard selector to the Create World screen. The choice is remembered until the new world loads,
 * where it is stored with the world and can never change.
 */
public class WildcatterClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof SelectWorldScreen) {
				ElectricalStandard.setPending(null);   // a cancelled creation must not leak into the next world
			} else if (screen instanceof CreateWorldScreen) {
				ElectricalStandard.setPending(StandardData.configDefault());
				// It sits on the Game tab, under the Allow Commands button, lined up with the world name field.
				EditBox nameField = null;
				for (AbstractWidget widget : Screens.getWidgets(screen)) {
					if (widget instanceof EditBox box) {
						nameField = box;
					}
				}
				if (nameField == null) {
					return;
				}
				EditBox anchor = nameField;
				CycleButton<ElectricalStandard> button = CycleButton.<ElectricalStandard>builder(WildcatterClient::label, ElectricalStandard::pending)
						.withValues(ElectricalStandard.values())
						.withTooltip(value -> Tooltip.create(Component.literal("The electrical standard of this world: its voltages, frequency and equipment. It cannot be changed after the world is created.")))
						.create(anchor.getX(), anchor.getY() + 112, anchor.getWidth(), 20, Component.literal("Grid"), (b, value) -> ElectricalStandard.setPending(value));
				Screens.getWidgets(screen).add(button);
				ScreenEvents.afterTick(screen).register(s -> {
					button.setX(anchor.getX());
					button.setY(anchor.getY() + 112);
					button.visible = anchor.visible;   // only on the Game tab
				});
			}
		});
	}

	private static Component label(ElectricalStandard standard) {
		return Component.literal(standard.displayName() + " (" + (int) standard.hertz() + " Hz)");
	}
}
