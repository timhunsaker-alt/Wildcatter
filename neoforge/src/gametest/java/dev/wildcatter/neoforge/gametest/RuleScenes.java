package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.abs;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.check;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;

import com.mojang.serialization.MapCodec;
import dev.wildcatter.Rules;
import dev.wildcatter.Rules.Rule;
import dev.wildcatter.RulesData;
import dev.wildcatter.Wildcatter;
import dev.wildcatter.block.RigSubstructureBlockEntity;
import dev.wildcatter.ore.SaltDomePlacement;
import dev.wildcatter.well.Reservoir;
import dev.wildcatter.well.ReservoirData;
import dev.wildcatter.well.SaltDome;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

/**
 * The world's rules (Tim's answer 23) on the server. The game test worlds start as an existing world would, every rule off
 * ({@link RulesData#testWorldsAsExisting}): the scenes were written for that, and those that test a rule switch it on, by the
 * {@code /wildcatter rule} command ({@link #rule}) or for a whole batch by a {@link RulesOn} environment.
 * <ul>
 * <li>{@code world_rules} (a batch of its own): the test world is found NEW (not yet initialized when it loaded) and its rules saved
 * with it; every rule is off as the harness asks; the command sets a rule, saves it with the world and refuses a name it does not
 * know; with {@code saltDomeGrid192} off the dome's placement takes exactly the random numbers and positions the four modifiers it had
 * before Phase 15 take, and a grid dome's flank field is not there; on, it is; {@code mudGel} off, water is not LOW GEL; on, it is;
 * {@code wellControl} off, an underbalanced strike into a deep field does not kick.</li>
 * </ul>
 */
final class RuleScenes {
	private RuleScenes() {
	}

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> out = new LinkedHashMap<>();
		out.put("world_rules", RuleScenes::worldRules);
		return out;
	}

	/** Sets a world rule as an operator would: {@code /wildcatter rule <key> <on>}. */
	static void rule(GameTestHelper helper, String key, boolean on) {
		MinecraftServer server = helper.getLevel().getServer();
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "wildcatter rule " + key + " " + on);
	}

	/** A test environment that turns these rules on for its batch and hands back what they were after it. */
	record RulesOn(List<String> keys) implements TestEnvironmentDefinition<boolean[]> {
		@Override
		public boolean[] setup(ServerLevel level) {
			boolean[] before = Rules.snapshot();
			for (String key : keys) {
				Rules.set(Rule.byKey(key), true);
			}
			Wildcatter.LOGGER.info("Game test environment: rules {} on", keys);
			return before;
		}

		@Override
		public void teardown(ServerLevel level, boolean[] before) {
			Rules.restore(before);
		}

		@Override
		public MapCodec<RulesOn> codec() {
			return MapCodec.unit(this);
		}
	}

	static void worldRules(GameTestHelper helper) {
		String scene = "world_rules";
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		boolean[] before = Rules.snapshot();
		try {
			RulesData data = RulesData.get(level);
			log(scene, "the test world's rules: origin " + data.origin() + ", on: " + Rules.describe());
			check(helper, data.origin().equals("new"), "the game test world should be found NEW (not initialized as it loaded), not " + data.origin());
			check(helper, Rules.describe().equals("none"), "the harness starts the test world with every rule off, not " + Rules.describe());

			// the command
			int ok;
			try {
				ok = server.getCommands().getDispatcher().execute("wildcatter rule mudGel true", server.createCommandSourceStack().withSuppressedOutput());
			} catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
				ok = -1;
			}
			check(helper, ok == 1 && Rules.mudGel() && data.saved(Rule.MUD_GEL), "the command should turn mudGel on and save it with the world");
			int list;
			int bad;
			try {
				list = server.getCommands().getDispatcher().execute("wildcatter rule", server.createCommandSourceStack().withSuppressedOutput());
				bad = server.getCommands().getDispatcher().execute("wildcatter rule noSuchRule true", server.createCommandSourceStack().withSuppressedOutput());
			} catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
				list = -1;
				bad = -1;
			}
			check(helper, list == Rule.values().length && bad == 0, "the command should list " + Rule.values().length + " rules (" + list + ") and refuse an unknown one (" + bad + ")");
			rule(helper, "mudGel", false);
			check(helper, !Rules.mudGel() && !data.saved(Rule.MUD_GEL), "the command should turn mudGel off again");

			// mudGel: plain water (no bentonite in 60 m3) LOW GEL only with the rule
			check(helper, !RigSubstructureBlockEntity.lowGel(0, 60_000), "with mudGel off plain water should drill at full rate, as before Phase 15");
			Rules.set(Rule.MUD_GEL, true);
			check(helper, RigSubstructureBlockEntity.lowGel(0, 60_000), "with mudGel on plain water should be LOW GEL");
			Rules.set(Rule.MUD_GEL, false);

			// wellControl: an underbalanced strike into a deep field kicks only with the rule
			Reservoir deep = new Reservoir(0, 0, 0, 0, 10, -40, Reservoir.Kind.OIL, 100_000, true);
			check(helper, !RigSubstructureBlockEntity.kicks(deep, 8.6), "with wellControl off a deep field should not kick, as before Phase 15");
			Rules.set(Rule.WELL_CONTROL, true);
			check(helper, RigSubstructureBlockEntity.kicks(deep, 8.6) && !RigSubstructureBlockEntity.kicks(deep, 14.0), "with wellControl on, 8.6 ppg into a deep field should kick and 14 ppg not");
			Rules.set(Rule.WELL_CONTROL, false);

			// saltDomeGrid192 off: the dome's placement is the old one, random number for random number
			PlacedFeature placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE)
					.getValueOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, Wildcatter.id("salt_dome")));
			PlacementContext context = new PlacementContext(level, level.getChunkSource().getGenerator(), Optional.of(placed));
			List<PlacementModifier> old = List.of(RarityFilter.onAverageOnceEvery(150), InSquarePlacement.spread(),
					HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(0)), BiomeFilter.biome());
			BlockPos origin = abs(helper, 0, 0, 0);
			int passed = 0, rare = 0;
			for (long s = 0; s < 3000; s++) {
				long rs = s * 0x9E3779B97F4A7C15L;   // spread: the first float of neighbouring small seeds is nearly the same
				RandomSource a = RandomSource.create(rs), b = RandomSource.create(rs);
				List<BlockPos> now = new ArrayList<>();
				SaltDomePlacement.INSTANCE.modify(context, a, origin, now::add);
				List<BlockPos> then = new ArrayList<>(List.of(origin));
				for (PlacementModifier m : old) {
					List<BlockPos> next = new ArrayList<>();
					for (BlockPos p : then) {
						m.modify(context, b, p, next::add);
					}
					then = next;
				}
				passed += now.size();
				rare += RandomSource.create(rs).nextFloat() < 1.0F / 150 ? 1 : 0;
				if (!now.equals(then) || a.nextLong() != b.nextLong()) {
					check(helper, false, "seed " + s + ": the dome's placement with saltDomeGrid192 off differs from its old placement: " + now + " against " + then);
				}
			}
			log(scene, "saltDomeGrid192 off: 3,000 chunks placed as before (" + rare + " through the 1-in-150 rarity, " + passed + " through the biome check too)");
			Rules.set(Rule.SALT_DOME_GRID_192, true);
			List<BlockPos> grid = new ArrayList<>();
			SaltDomePlacement.INSTANCE.modify(context, RandomSource.create(1), origin, grid::add);
			check(helper, grid.equals(List.of(origin)), "with saltDomeGrid192 on the feature should run once at the chunk: " + grid);
			Rules.set(Rule.SALT_DOME_GRID_192, false);

			// saltDomeGrid192 and the fields: a grid dome's flank field is there only with the rule
			int cx = Math.floorDiv(origin.getX(), dev.wildcatter.Tuning.DOME_CELL), cz = Math.floorDiv(origin.getZ(), dev.wildcatter.Tuning.DOME_CELL);
			long seed = -1;
			SaltDome dome = null;
			for (long s = 1; s < 100_000 && dome == null; s++) {
				SaltDome d = SaltDome.inCell(s, cx, cz);
				if (d != null && !d.flanks().isEmpty()) {
					seed = s;
					dome = d;
				}
			}
			check(helper, dome != null, "no seed puts a dome with a flank field in the scene's cell");
			int[] mid = dome.flanks().getFirst().middle();
			ReservoirData.TestRegion region = new ReservoirData.TestRegion(cx * dev.wildcatter.Tuning.DOME_CELL, cz * dev.wildcatter.Tuning.DOME_CELL,
					(cx + 1) * dev.wildcatter.Tuning.DOME_CELL - 1, (cz + 1) * dev.wildcatter.Tuning.DOME_CELL - 1, seed);
			ReservoirData.TEST_REGIONS.add(region);
			ReservoirData.testDomeBiome = true;
			try {
				Reservoir off = ReservoirData.field(level, mid[0], mid[1]);
				Rules.set(Rule.SALT_DOME_GRID_192, true);
				Reservoir on = ReservoirData.field(level, mid[0], mid[1]);
				Rules.set(Rule.SALT_DOME_GRID_192, false);
				log(scene, "seed " + seed + ", the flank's middle at " + mid[0] + ", " + mid[1] + ": off " + (off == null ? "nothing" : off.key()) + ", on " + (on == null ? "nothing" : on.key()));
				check(helper, off == null || !off.onDome(), "with saltDomeGrid192 off there should be no flank field: " + (off == null ? "" : off.key()));
				check(helper, on != null && on.source() == Reservoir.Source.FLANK, "with saltDomeGrid192 on the flank field should be there");
			} finally {
				ReservoirData.TEST_REGIONS.remove(region);
				ReservoirData.testDomeBiome = null;
			}
		} finally {
			Rules.restore(before);
		}
		log(scene, "ok");
		helper.succeed();
	}
}
