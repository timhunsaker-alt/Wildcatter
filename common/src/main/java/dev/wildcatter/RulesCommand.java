package dev.wildcatter;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.wildcatter.Rules.Rule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/**
 * {@code /wildcatter rule} lists the world's rules; {@code /wildcatter rule <name>} reads one; {@code /wildcatter rule <name> true|false}
 * sets it for this world (saved with it). Operators only (game masters, as {@code /gamerule}).
 */
public final class RulesCommand {
	private RulesCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(Wildcatter.MOD_ID).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("rule").executes(c -> list(c.getSource()))
						.then(Commands.argument("name", StringArgumentType.word())
								.suggests((c, b) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(Rule.values()).map(r -> r.key), b))
								.executes(c -> read(c.getSource(), StringArgumentType.getString(c, "name")))
								.then(Commands.argument("value", BoolArgumentType.bool())
										.executes(c -> set(c.getSource(), StringArgumentType.getString(c, "name"), BoolArgumentType.getBool(c, "value")))))));
	}

	private static int list(CommandSourceStack source) {
		String origin = RulesData.get(source.getLevel()).origin();
		source.sendSuccess(() -> Component.literal("Wildcatter rules for this world (" + origin + " world):"), false);
		for (Rule r : Rule.values()) {
			source.sendSuccess(() -> line(r), false);
		}
		return Rule.values().length;
	}

	private static Component line(Rule r) {
		return Component.literal("  " + r.key + " = " + Rules.on(r) + " (" + r.what + "; new worlds: " + (r.newWorld ? "on" : "off") + ")");
	}

	private static int read(CommandSourceStack source, String name) {
		Rule r = Rule.byKey(name);
		if (r == null) {
			source.sendFailure(Component.literal("No rule " + name + ". /wildcatter rule lists them."));
			return 0;
		}
		source.sendSuccess(() -> line(r), false);
		return Rules.on(r) ? 1 : 0;
	}

	private static int set(CommandSourceStack source, String name, boolean value) {
		Rule r = Rule.byKey(name);
		if (r == null) {
			source.sendFailure(Component.literal("No rule " + name + ". /wildcatter rule lists them."));
			return 0;
		}
		RulesData.get(source.getLevel());   // make sure the world's copy is loaded before it changes
		Rules.set(r, value);
		Wildcatter.LOGGER.info("World rule {} set to {} by {}", r.key, value, source.getTextName());
		source.sendSuccess(() -> Component.literal("Rule " + r.key + " is now " + value + " in this world."), true);
		return 1;
	}
}
