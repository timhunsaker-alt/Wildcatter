# Specs for the unscheduled Ideas: decisions for Tim

Tim (2026-11-09): "make decisions best recommendation and complete the rest of these two mods leaving questions for when I get back ...
lets get all ideas done or planed out as much as possible without my input".

This file covers the specs written for every item in ROADMAP.md's "Ideas (not scheduled)" except the ammonia plant (approved, being
built). The overview and proposed phase order are in `docs/ideas-plan.md`. Each entry is a decision taken on the best recommendation,
in the format of `docs/questions/README.md`. **Nothing here is approved; no code was written.** Item numbers match the "Decisions are in
... (items N-M)" line at the top of each spec.

---

## Sales gas pipeline and meter station (`docs/multiblocks/sales-gas.md`)

### 1. What "a buyer" is, in a mod with no money
- **Chosen:** an off-map transmission line, one per dimension, with a world-saved account: receipt stations put pipeline gas in, delivery
  stations anywhere in the dimension take it out, less 2 % kept as compressor fuel.
- **Why:** it is what an interstate pipeline really is (transport between a receipt point and a delivery point), it gives residue gas a use
  besides engines and the flare, and it lets a gas field feed a distant power plant.
- **Alternatives:** a pure sink that books the energy sold (honest, but gives nothing back); a player-built trunk line with compressor
  stations and line pack (bigger, and the mod's pipes carry no pressure); a villager trade (money by the back door).

### 2. One account per world or per player
- **Chosen:** per dimension (the whole world shares one line).
- **Why:** simplest; Wildcatter has no teams elsewhere.
- **Alternatives:** per player or per scoreboard team, so multiplayer players cannot draw each other's gas.

### 3. Which gas the line accepts
- **Chosen:** pipeline gas only; separator, compressed and dried gas are refused as OFF SPEC: RICH, wet gas as OFF SPEC: WATER;
  biogas (biogas.md) as OFF SPEC: CO2.
- **Why:** real tariffs set heating-value, dew-point and water limits, and the 5b gas plant is exactly what meets them.
- **Alternatives:** accept any dry gas at its heating value.

### 4. Station rating, retention and battery time
- **Chosen:** 5 m3/s a station; 2 % retention; 8 game hours on the RTU battery before METER FAILED.
- **Why:** five gas plants a station is plenty; 2 % is mid-range of real 1-3 % *(unverified)*; a real RTU rides through a short outage.
- **Alternatives:** no retention; a station that keeps working unpowered.

## The acid plant's steam to a turbine (`docs/multiblocks/acid-plant-steam.md`)

### 5. Reuse the 5 MW turbine, or a new small one
- **Chosen:** reuse `steam_turbine_5`, `st_generator`, the air-cooled condenser and feed pump at a third of their flow (about 1.6 MW), with
  the turbine island's 13.8/4.16 kV unit auxiliary transformer onto the acid plant's 4.16 kV bus.
- **Why:** no new turbine parts; Stingray's planned plant gives about the same MW per kg/s of steam.
- **Alternatives:** a new 2 MW, 4.16 kV turbine-generator (more parts, no transformer needed).

### 6. Superheat
- **Chosen:** a new `acid_superheater` part against the converter (real DCDA plants superheat between beds); the turbine needs it and trips
  on LOW STEAM TEMPERATURE without heat.
- **Why:** condensing turbines need superheat; it is a real distinct part.
- **Alternatives:** let the turbine run on saturated steam at a derate.

### 7. The feed pump returns condensate to the waste heat boiler
- **Chosen:** yes; the boiler's make-up falls to 10 %.
- **Why:** a closed loop, as the steam plant's.
- **Alternatives:** keep the water pipe feed and dump the condensate.

## Biogas (`docs/biogas.md`)

### 8. Derate and heat rate
- **Chosen:** 850 kW on biogas (the CG260-12's 84 %), 11.0 MJ/kWh, so 23 m3 of biogas makes Farmings' 48 kWh.
- **Why:** a published biogas rating, and it agrees with Farmings' OMAFA figure.
- **Alternatives:** a pure heating-value derate (none: the engine just burns more gas); a steeper derate.

### 9. Which machines burn biogas
- **Chosen:** the gas set and fireboxes; not the steam plant burner, turbines, reformer, dryers or the sales line.
- **Why:** they already require pipeline gas; real ones need fuel-gas specs biogas does not meet.
- **Alternatives:** let every gas burner take it at its heating value.

### 10. The interface for Farmings
- **Chosen:** Farmings' block entity implements `dev.wildcatter.block.FluidNode` offering `Product.BIOGAS`; added in v0.6.0.
- **Why:** it is how every Wildcatter source works already, and Wildcatter never calls Farmings.
- **Alternatives:** a Wildcatter-side "gas inlet" block Farmings fills by its own API.

### 11. A small biogas set
- **Chosen:** not in this spec; proposed as a new Idea (a 100-250 kW gas set, as real farm CHP units).
- **Why:** a 1 MW set idles on 61 m3/h, more than a hundred-cow digester makes; Farmings runs it in bursts from a gas holder.
- **Alternatives:** add the small set now.

## Mekanism (`docs/mekanism-check.md`)

### 12. Is it out?
- **Chosen:** no (checked 2026-11-09: latest release v1.21.1-10.7.19.85; the main branch is named 26.3 with no release). Keep the Idea; re-check
  each phase.
- **Why:** the releases page lists no 26.x build.
- **Alternatives:** test against a dev build of the 26.3 branch (its API can move); test against the unofficial 26.1.2 fork (wrong version).

### 13. How to check
- **Chosen:** nine NeoForge smoke scenes with Mekanism as a test-only runtime dependency.
- **Why:** 9c's rule (no code needs Mekanism) and Phase 9's testing split.
- **Alternatives:** a manual play session only.

## Salt domes (`docs/salt-dome-oil.md`)

### 14. Move the domes to a seed grid
- **Chosen:** yes, a 192-block grid, so a flank field can find its dome from the seed (reservoirs depend only on the seed).
- **Why:** the alternative (storing dome positions when chunks generate) breaks "nothing is stored until a well starts to empty one".
- **Alternatives:** keep the vanilla feature and store each placed dome; or give flank fields their own grid unrelated to domes (no link).

### 15. Old worlds
- **Chosen:** old domes keep no oil; a grid dome in already generated chunks has its oil but no salt drawn.
- **Why:** reservoirs stay a pure function of the seed; worldgen only reaches new chunks anyway.
- **Alternatives:** skip grid domes whose chunks exist (needs a chunk check in a seed function).

### 16. Field sizes and kinds
- **Chosen:** 1-3 flank fields a dome, 80 % oil, up to twice a shallow field (150,000-400,000 units); a caprock pool in 30 % of domes,
  20,000-60,000 units, a gusher that falls off fast.
- **Why:** Spindletop's story: a short caprock boom, then richer flanks.
- **Alternatives:** flank fields at ordinary sizes; no caprock pool.

### 17. A surface mound and the seismograph
- **Chosen:** a 1-2 block mound over each grid dome; the seismograph reads SALT DOME and its crest.
- **Why:** real domes were found by low hills and geophysics.
- **Alternatives:** no surface sign (find them by drilling and the seismograph only).

## Drilling mud (`docs/multiblocks/drilling-mud.md`)

### 18. Shallow fields stay drillable on water
- **Chosen:** shallow and flank fields at 8.34 ppg pore pressure (a fresh-water gradient); only deep fields geopressured (13.5 ppg).
- **Why:** nothing that works today stops working; deep fields are the land rig's, the late game.
- **Alternatives:** a real 8.9 ppg normal gradient everywhere (water would kick on every field).

### 19. Kicks and blowouts
- **Chosen:** in a second wave (15b2): a 30 s window to shut in after the alarm, else a blowout that can ignite; killed by a BOP or a
  relief well.
- **Why:** "safety as consequence" (VISION pillar 4); it also closes the known gap of no BOP stack.
- **Alternatives:** kicks shut in automatically (no blowouts); no well control at all, mud weight just a requirement.

### 20. Salt and the small rig
- **Chosen:** WASHOUT applies to the land rig only.
- **Why:** the small rig has no mud system to saturate.
- **Alternatives:** give the small rig a brine option too.

### 21. Barite grade
- **Chosen:** API 13A 4.1, from the game's 90 % ore.
- **Why:** the real gangue lowers SG; 4.1 is a real grade since 2010.
- **Alternatives:** 4.2.

### 22. Where barite and bentonite come from
- **Chosen:** a mud plant of two mills and the existing bagging station; the rig mixes bags through a new mixing hopper.
- **Why:** distinct real parts; the bag format is already in the mod.
- **Alternatives:** craft barite powder in a crafting grid (not real).

## ADU yellowcake (`docs/multiblocks/adu-yellowcake.md`)

### 23. New tank or the existing one
- **Chosen:** the existing precipitation tank takes ammonia as a second reagent; a new calciner takes the dryer's place.
- **Why:** a real tank precipitates with whichever reagent it is fed.
- **Alternatives:** a separate ADU tank.

### 24. The ammonia shortfall
- **Chosen:** the tank precipitates as fast as ammonia allows; at 15.2 kg/h that is all of an ISR wellfield but 5 % of the mill.
- **Why:** the approved ammonia plant is one reformer's worth; inflating it would reopen an approved spec.
- **Alternatives:** a larger ammonia plant (a new Idea: one 300 Nm3/h reformer train); a lower ammonia use than 0.2 kg/kg.

### 25. Grade and drum
- **Chosen:** 96 % U3O8, 400 kg net, 326 kg of U; conversion's hydrogen, HF and ash follow the grade.
- **Why:** mid of the real 94-98 %; the same drum keeps the logistics.
- **Alternatives:** heavier drums.

### 26. Hydrogen peroxide (uranyl peroxide)
- **Chosen:** left out.
- **Why:** the Idea named ADU; peroxide needs a peroxide plant.
- **Alternatives:** peroxide for ISR eluate (what real Wyoming plants use).

## Conversion extensions (`docs/multiblocks/conversion-extensions.md`)

### 27. Cathode hydrogen
- **Chosen:** a cathode gas scrubber (NaF beds) puts the cells' 28.6 Nm3/h on a hydrogen pipe; it covers the reducer's 25.4.
- **Why:** derived from conversion.md's own numbers; real cells make one H2 per F2.
- **Alternatives:** burn it as boiler fuel; keep venting.

### 28. The ash bed
- **Chosen:** 2 t of ash then BED FULL; a 10-minute retire into ash drums (3 % U) that the mill's leach tank recovers; 500 kg of fluorspar for a
  new bed.
- **Why:** the real practice (ash to a mill); it closes a counted stream.
- **Alternatives:** ash to the lined cell as waste.

### 29. KOH scrubber
- **Chosen:** a new KOH scrubber regenerated with lime, its CaF2 back to the HF kiln; the first KOH from the chlor-alkali cell run on potash;
  old plants without a scrubber keep running.
- **Why:** real chemistry; uses potash, lime and the cell; no plant stops on upgrade.
- **Alternatives:** make the scrubber mandatory; skip KOH and use caustic soda (NaF scale fouls real scrubbers).

### 30. Full size
- **Chosen:** four cell houses in series on a new 12.47 kV rectifier, and a larger size of the five main-line parts, in a later wave (16b2).
- **Why:** cells in series are the potline's precedent; capacity by a bigger part, not copies.
- **Alternatives:** leave the plant quarter size.

## ISR operations (`docs/multiblocks/isr-operations.md`)

### 31. A CO2 source
- **Chosen:** a CO2 liquefier on the lime kiln's off-gas, a 30 t storage tank, a 0.5 g/L dose giving a 1.3x head-grade factor.
- **Why:** the lime kiln already counts 327 kg/h of CO2; real kilns supply CO2.
- **Alternatives:** CO2 from the steam reformer's PSA tail gas; bottled CO2 cylinders by hopper (about five a hour, a chore).

### 32. The bleed to disposal
- **Chosen:** a new "wellfield bleed" product the lease's injection pump also takes.
- **Why:** reuses the disposal well; real ISR uses Class I disposal wells.
- **Alternatives:** call it produced water; evaporation ponds.

### 33. Monitor ring
- **Chosen:** at least 6 monitor wells, one per 40 blocks of ring 30-60 blocks out, or the house will not inject; a lixiviant front that grows
  when bleed is under 0.5 %; EXCURSION when it reaches a monitor; injection stops after 3 game days.
- **Why:** the licence condition is real and gives the bleed a purpose.
- **Alternatives:** monitor wells optional (they only warn).

### 34. Restoration
- **Chosen:** SWEEP, RO, STABILITY, RESTORED modes, lowest priority; an unrestored front makes a new house start in EXCURSION.
- **Why:** real restoration, with a small consequence.
- **Alternatives:** leave restoration out.

### 35. Satellite IX
- **Chosen:** a SATELLITE mode on the existing columns and a placeable resin trailer (10 m3, 800 kg of U3O8) carried as one item.
- **Why:** the mod has no vehicles; the fuel assembly is the precedent for a big carried item.
- **Alternatives:** long pregnant-lixiviant pipelines only.

## Enrichment operations (`docs/multiblocks/enrichment-operations.md`)

### 36. Assay choices
- **Chosen:** product 3.0-4.95 %, tails 0.20-0.30 %; an assembly's starting reactivity falls 4,000 pcm a percent below 4.95 %.
- **Why:** the real ordering range; the reactivity slope is a proposal to make low-enriched fuel shorter-lived.
- **Alternatives:** product only (tails fixed).

### 37. Separative work against speed
- **Chosen:** keep the approved linear "speed share" (enrichment.md).
- **Why:** not reopening an approved rule.
- **Alternatives:** SWU proportional to the fourth power of speed (the Dirac formula), which punishes an under-powered converter hard.

### 38. Spin-up and coast-down times
- **Chosen:** 10 minutes up, 60 minutes down; a cascade rides through short outages; lingering at 30-60 % speed for 2 minutes can crash.
- **Why:** real machines ride through; the resonance band is real; times compressed.
- **Alternatives:** instant spin-up and stop, as now.

### 39. Crashes
- **Chosen:** only for a visible cause (critical speed, hot casing, overspeed, out-of-phase close), 20 % to a neighbour, 0.1 kg of UF6 out.
- **Why:** random crashes punish nothing the player did.
- **Alternatives:** a small random failure rate as well.

### 40. Sampling and heels
- **Chosen:** a sampling autoclave whose SAMPLED mark the fuel plant requires; heels counted only (20 kg 48Y, 10 kg 30B), no washing.
- **Why:** sampling is a real gate with a part; washing is a chore.
- **Alternatives:** five-yearly recertification of cylinders.

## Ferroalloys (`docs/multiblocks/ferroalloys.md`)

### 41. One furnace for three alloys
- **Chosen:** one 10 MVA SAF with a recipe setting (FeCr, FeMn, FeSi, and later Ti slag), changing after a 1-hour hearth empty.
- **Why:** real furnaces do switch campaigns; one part keeps it small.
- **Alternatives:** a furnace per alloy.

### 42. Furnace transformer and capacitor bank
- **Chosen:** a 10 MVA furnace transformer on the distribution tier (150 V, 38 kA secondary) and a 3 Mvar capacitor bank as a general grid
  part.
- **Why:** the furnace's PF 0.80 makes reactive power a real problem for a gas-turbine island; capacitors are the real fix.
- **Alternatives:** no capacitor bank; a furnace at PF 0.95 (unreal).

### 43. Quartz
- **Chosen:** vanilla nether quartz stands in for quartzite.
- **Why:** no quartzite rock in the mod.
- **Alternatives:** add a quartzite ore; use sand (real FeSi needs lump quartz).

### 44. Molybdenum and tungsten concentrates come from Tailings
- **Chosen:** the roaster and vessel take flotation concentrate (assayed slurry) from Tailings' concentrator.
- **Why:** 3 % and 6.5 % ores need concentrating, which Tailings does; no duplicate flotation in Wildcatter.
- **Alternatives:** a small Wildcatter flotation cell; let the roaster take ore directly (unreal).

### 45. Electrode paste
- **Chosen:** Søderberg paste from the anode plant's mixer, 20 kg a tonne; ELECTRODES SHORT without it.
- **Why:** the anode plant already has calcined coke and pitch.
- **Alternatives:** no electrode consumption.

### 46. Ferrotungsten has no user
- **Chosen:** make it anyway, tagged for other mods.
- **Why:** the Idea lists tungsten; real FeW is a tool-steel input.
- **Alternatives:** drop tungsten from the plan; pure tungsten for the rod welder's TIG electrodes (powder metallurgy, a bigger plant).

## Nickel smelter (`docs/multiblocks/nickel-smelter.md`)

### 47. The flowsheet
- **Chosen:** Sudbury-style roaster, electric furnace and Peirce-Smith converter; Nikkelverk chlorine leach and electrowinning.
- **Why:** uses the acid plant, the furnace transformer, chlorine and the DC tier.
- **Alternatives:** flash smelting; the Mond carbonyl process (uses the CO the chlorinators count).

### 48. Feed grade
- **Chosen:** the 60 % sulfide ore treated as concentrate grade, 12 % Ni, 4 % Cu, 0.4 % Co; Tailings' concentrate also accepted.
- **Why:** the game's 10x grade already makes it concentrate-like.
- **Alternatives:** require Tailings' concentrate.

### 49. Size
- **Chosen:** 2 t of feed an hour, 230 kg of nickel; 18 cells at 12.3 kA.
- **Why:** far more than the fuel plant needs (20 kg an assembly) at a sane electrical size.
- **Alternatives:** a larger smelter that makes the SAF transformer work hard.

### 50. Alloys
- **Chosen:** 304, 316 and an Alloy 600-type nickel-chromium alloy in an induction furnace and an AOD with oxygen and nitrogen (no argon).
- **Why:** HC ferrochrome's carbon must come out; the mod has oxygen and nitrogen generators.
- **Alternatives:** use chromium metal and skip the AOD; add argon from an air separation unit.

### 51. Alloy 718 and niobium
- **Chosen:** not made; Alloy 600-type stands in for the grids.
- **Why:** no niobium ore.
- **Alternatives:** add pyrochlore (Araxá) to the ores as a new Idea.

## Fertilizer (`docs/multiblocks/fertilizer.md`)

### 52. Size
- **Chosen:** 20 t of P2O5 a day (a third of the acid plant), 5 t/h of MOP.
- **Why:** leaves acid for the mill.
- **Alternatives:** a plant sized to the whole acid plant.

### 53. TSP first, DAP as ammonia allows
- **Chosen:** the granulator makes TSP (no ammonia) or DAP.
- **Why:** the ammonia plant gives only 69 kg of DAP an hour.
- **Alternatives:** DAP only (the plant would idle).

### 54. Phosphogypsum
- **Chosen:** out as assayed tails slurry to a lined tailings cell; NO GYPSUM STACK stops the filter.
- **Why:** reuses Phase 11 and 12; a real plant cannot run without its stack.
- **Alternatives:** count it.

### 55. Fertilizer's use
- **Chosen:** bags tagged `c:fertilizers` for Farmings; no use on crops inside Wildcatter.
- **Why:** VISION: not a consumer mod; Farmings owns crops.
- **Alternatives:** a bag works as bone meal on a 3x3.

### 56. Potash route
- **Chosen:** hot leach and vacuum crystallization.
- **Why:** fewer parts than flotation and real (Saskatchewan uses both).
- **Alternatives:** flotation via Tailings.

## Uranium from phosphoric acid (`docs/multiblocks/uranium-from-phosphate.md`)

### 57. Process
- **Chosen:** DEPA-TOPO, two cycles, ammonium carbonate strip, AUC to the ADU calciner.
- **Why:** the US commercial process; reuses 16a's calciner.
- **Alternatives:** OPAP; ion exchange.

### 58. Extractant
- **Chosen:** an "extractant drum" crafted from merchant acid, LPG and iron as a stand-in for DEPA and TOPO.
- **Why:** the mod has no 2-ethylhexanol chemistry.
- **Alternatives:** buy-in style crafting from vanilla items; a proper oxo-alcohol chain (large).

### 59. Recovery
- **Chosen:** 80 % of the rock's uranium to the acid, 90 % recovered: 2.7 kg of U an hour.
- **Why:** literature ranges *(unverified)*.
- **Alternatives:** higher grades to make it worth more.

## Chlor-alkali plant (`docs/multiblocks/chlor-alkali-plant.md`)

### 60. The Idea is half done
- **Chosen:** treat it as the full-size plant (1 t/h of chlorine); keep 7f's small cell for the zirconium plant.
- **Why:** 7f built a 21 kg/h cell; the titanium plant and others need fifty times that.
- **Alternatives:** strike the Idea as done.

### 61. Rectifier
- **Chosen:** a new 12.47 kV `rectifier_mv`, 3.2 MVA, up to 16 kA and 500 V, shared with full-size conversion.
- **Why:** 2.4 MW DC is beyond the 4.16 kV unit.
- **Alternatives:** a potline rectiformer (60 kA, far too big).

### 62. Brine wells
- **Chosen:** optional solution mining of a salt dome, removing rock salt blocks from the bottom of the hole.
- **Why:** real Gulf Coast practice; ties to the salt domes.
- **Alternatives:** rock salt by hopper only.

### 63. HCl and calcium in brine
- **Chosen:** an HCl burner for spare hydrogen; brine calcium removed with carbonate made from caustic and CO2, voltage creeping without it.
- **Why:** no soda ash in the mod; HCl regenerates the brine IX.
- **Alternatives:** skip brine purity entirely.

## Titanium pigment (`docs/multiblocks/titanium-pigment.md`)

### 64. Chloride or sulfate process
- **Chosen:** chloride, with ilmenite smelted to slag in the SAF.
- **Why:** its chlorine loop uses the chlor-alkali plant; slag smelting gives the furnace another job.
- **Alternatives:** the sulfate process on the acid plant (simpler parts, a lot of acid and copperas waste).

### 65. Oxygen
- **Chosen:** a new 100 Nm3/h VPSA oxygen part.
- **Why:** the ISR's 50 Nm3/h PSA would need two copies.
- **Alternatives:** two ISR oxygen generators.

### 66. Pigment's use
- **Chosen:** a bag crafts into 64 white dye; tagged for other mods.
- **Why:** the only vanilla use.
- **Alternatives:** no use.

### 67. Titanium metal
- **Chosen:** left out; proposed as a later Idea (Kroll on the same TiCl4, for condenser tubes).
- **Why:** scope.
- **Alternatives:** add it here.

## Lithium (`docs/multiblocks/lithium.md`)

### 68. Hydroxide or carbonate
- **Chosen:** LiOH·H2O by caustic; a small carbonator for Li2CO3.
- **Why:** the mod has caustic and no soda ash.
- **Alternatives:** make soda ash (Solvay, on the ammonia plant and limestone).

### 69. A use for lithium
- **Chosen:** Li2CO3 in the potline bath, 0.1 V lower a pot.
- **Why:** real practice and a grid-visible effect.
- **Alternatives:** none in-mod.

### 70. Grid batteries
- **Chosen:** proposed as a new Idea, not specced.
- **Why:** lithium, nickel, cobalt, manganese and graphite are all in the ores; a battery energy storage system with an inverter is grid
  gameplay. It is too big to add without Tim.
- **Alternatives:** spec it now.

### 71. Sodium sulfate
- **Chosen:** bagged, no buyer (as item 96).
- **Why:** see item 96.
- **Alternatives:** see item 96.

## Zirconium extensions (`docs/multiblocks/zirconium-extensions.md`)

### 72. Fumed silica
- **Chosen:** a flame-hydrolysis burner on the chlorinator's SiCl4, using the small cell's hydrogen; no in-mod use.
- **Why:** real; closes a counted stream; makes HCl.
- **Alternatives:** leave SiCl4 counted.

### 73. Caustic uses
- **Chosen:** already done by 10b2 and this plan; strike the Idea.
- **Why:** the Bayer refinery takes it; lithium, brine and nickel will too.
- **Alternatives:** none.

### 74. Hafnium
- **Chosen:** HfCl4 drums and a Kroll campaign to hafnium rodlets; about 2,100 hours of the line's hafnium for a set.
- **Why:** a real, very slow late goal; B4C stays the norm.
- **Alternatives:** leave hafnium counted.

### 75. Pidgeon magnesium
- **Chosen:** dolime from the lime kiln, FeSi, a Pidgeon furnace, magnesium ingots to the Kroll loop.
- **Why:** gives dolomite and FeSi a use.
- **Alternatives:** none.

### 76. The chromium step
- **Chosen:** a chromate kiln with a hexavalent-chromium spill hazard, then the aluminothermic vessel; lowest priority.
- **Why:** pure fidelity (10 g of Cr an hour).
- **Alternatives:** keep the chromite stand-in.

### 77. Scrap and yields
- **Chosen:** 8/6/2 % losses to Zircaloy scrap, up to 30 % back in the VAR.
- **Why:** real practice; a small loop.
- **Alternatives:** none.

### 78. Batches
- **Chosen:** two alternating retorts inside the one Kroll part (50 kg, 5 h, then 4 h distillation); VAR 100 kg ingots melted twice.
- **Why:** lumpy output as real, same average; no tiling.
- **Alternatives:** keep steady flow.

## Fuel fabrication extensions (`docs/multiblocks/fuel-fabrication-extensions.md`)

### 79. HF recovery
- **Chosen:** 70 % aqueous HF to the conversion HF column, 0.5 kg of acid a kg.
- **Why:** 13 % of the conversion plant's HF.
- **Alternatives:** none.

### 80. Zinc stearate
- **Chosen:** zinc ingot plus an animal fat (`c:animal_fats`; rotten flesh stands in); without it 10 % cracked pellets.
- **Why:** stearic acid comes from tallow; the mod has none.
- **Alternatives:** zinc ingot alone.

### 81. Steel retired for hardware
- **Chosen:** once 17b2 lands the fixture takes 304 and nickel alloy only.
- **Why:** fuel.md's stand-in was explicitly temporary.
- **Alternatives:** accept either.

### 82. Inspection
- **Chosen:** pellet and rod inspection parts; without them 1 % of assemblies carry a leaking rod.
- **Why:** consequence feeds the reactor's contamination.
- **Alternatives:** no defects modelled.

### 83. Burnable absorbers
- **Chosen:** IFBA only, 0-104 rods, 3,000 pcm at 104; gadolinia waits for a rare-earth plant (new Idea).
- **Why:** boron is in the mod; gadolinium is not.
- **Alternatives:** both.

### 84. Scrap
- **Chosen:** a scrap oxidizer, U3O8 back to the granulator at up to 10 % (doubling as the pore former).
- **Why:** real practice.
- **Alternatives:** count scrap.

## Reactor extensions (`docs/multiblocks/reactor-extensions.md`)

### 85. Containment volume and hydrogen
- **Chosen:** a containment building of new concrete blocks, flood-filled (up to 60,000 m3); 8d's 5,000 m3 becomes the lower compartment,
  mixing into the whole over an hour.
- **Why:** a big volume alone would make the 8d blast impossible; the compartment keeps the hazard and makes PARs and igniters matter.
- **Alternatives:** a fixed containment volume part; no compartment.

### 86. Containment failure
- **Chosen:** alarm above 0.4 MPa gauge; the hatch fails at 2.5x design.
- **Why:** real large-dry design pressure; failure margins are large in reality.
- **Alternatives:** no failure.

### 87. LTOP and the P-T curve
- **Chosen:** a simplified curve (3.4 MPa to 100 C, rising to 15.5 MPa at 290 C), LTOP armed at 3.2 MPa below 150 C, vessel failure at twice
  the curve.
- **Why:** real protection with a severe but rare consequence.
- **Alternatives:** alarm only.

### 88. The 55 C/h limit
- **Chosen:** alarm and fatigue count in plant time; a `realHeatup` world setting for the real 8-12 hours.
- **Why:** keeps the approved 80x compression.
- **Alternatives:** remove the compression (reopens an approved decision).

### 89. Relief tank
- **Chosen:** 30 m3, disc at 0.7 MPa, spray from the CVCS.
- **Why:** real arrangement *(values unverified)*.
- **Alternatives:** none.

### 90. OTΔT and OPΔT
- **Chosen:** the Westinghouse forms with H. B. Robinson 2's constants (K1 1.1265, K2 0.01228/F, K3 0.00089/psig); OPΔT constants to be
  read from the same note; no axial-flux term.
- **Why:** the real trip in place of the 343 C simplification.
- **Alternatives:** keep the simplification.

### 91. Casks
- **Chosen:** a cask loading station at the pool, 12-assembly canisters, horizontal concrete modules within 64 blocks reached by an abstract
  10-minute transfer; fuel stays records; modules unbreakable while loaded.
- **Why:** keeps Tim's "irradiated fuel is never an item"; no vehicles in the mod.
- **Alternatives:** a crane lift of a loaded cask as a big part (would make loaded fuel an item, against the 8e decision).

### 92. Contamination
- **Chosen:** sparse per-block contamination with a 30-day game half-life, up to 1 mSv/h at 1 m; players carry it; a personnel monitor and a
  plug-in washer clean it.
- **Why:** real hazard; the washer exercises plugs.
- **Alternatives:** contamination as dose-rate zones only.

### 93. Fresh-fuel inspection
- **Chosen:** 1 % handling damage on hopper or crane moves; inspection finds it; uninspected damage becomes a leaking rod.
- **Why:** gives inspection a reason.
- **Alternatives:** inspection as decoration.

### 94. Rod materials
- **Chosen:** hafnium RCCAs at 85 % of B4C's worth; AIC at 90 % gated on a zinc refinery (new Idea); the console reads SDM.
- **Why:** indium and cadmium need a zinc refinery.
- **Alternatives:** AIC from silver alone (unreal).

### 95. Helium in B4C
- **Chosen:** B-10 burnup per RCCA, REPLACE at 30 %, worth falling past 40 %; RCCAs swapped by the refuelling machine.
- **Why:** real rod life limit.
- **Alternatives:** none.

### 96. Sodium sulfate
- **Chosen:** 1 t bags tagged `c:sodium_sulfate`; disposable into a lined tailings cell; no in-mod use.
- **Why:** no real buyer in the mod's scope.
- **Alternatives:** a glass batch recipe (salt cake as a fining agent: sand plus a bag gives more glass in a furnace); a kraft pulp mill
  (out of scope).

---

## The plan (`docs/ideas-plan.md`)

### 97. Order
- **Chosen:** 14 gas and power quick wins; 15 oilfield (salt domes, drilling mud); 16 uranium routes and plant operations; 17 metals; 18
  phosphate; 19 reactor and fuel extensions; 20 chemicals (chlor-alkali, titanium, lithium).
- **Why:** value to the fuel-generation-grid chain (VISION pillar 1) against size; dependencies (17a before 17b; 18a before 18b; 20a
  before 20b and 20c).
- **Alternatives:** nuclear extensions first (end-game depth), or ores first (they are "unused").

### 98. Grouping
- **Chosen:** one spec per plant for the extensions (conversion, ISR, enrichment, zirconium, fuel, reactor); one spec per ore plant;
  system specs (biogas, salt domes, Mekanism) under `docs/`.
- **Why:** as asked, and as the repo already keeps worldgen in `docs/ores.md`.
- **Alternatives:** one spec per item.

### 99. The multiblock index
- **Chosen:** rows added to `docs/multiblocks/README.md` marked DRAFT with a proposed phase.
- **Why:** the index is where specs are found; the README's "APPROVED" banner covers only the older rows.
- **Alternatives:** keep them out of the index until approved.

## Proposed changes to files this wave did not edit

### 100. ROADMAP.md
- **Proposed:** replace "Ideas (not scheduled)" items with a pointer to `docs/ideas-plan.md` and the spec of each; strike "uses for the
  chlor-alkali caustic soda" (done in 10b2); keep the Mekanism line until it is released; add the new Ideas below.
- **New Ideas proposed:** a battery energy storage system (lithium, nickel, cobalt, manganese, graphite); a zinc refinery (indium,
  cadmium, so AIC rods); a rare-earth plant on the separator's monazite (gadolinia); pyrochlore for niobium (Alloy 718); a larger ammonia
  train; a small 100-250 kW biogas set; titanium metal by Kroll; containment spray and fan coolers; gas storage in salt caverns.

### 101. PROGRESS.md
- **Proposed:** nothing until a wave is built. When 14a lands, strike the known gap "Gas has no sales pipeline yet"; when 15b2 lands,
  "There is no blowout preventer stack in the cellar yet"; when 14d lands, the 9a and 9c Mekanism gaps; when 19b lands, 8d's "no
  containment building" and 8e's "no casks" and "no contamination".

### 102. ELECTRICAL_MODEL.md
- **Proposed:** no change to the settled table. Add, as built, the shunt capacitor bank (ferroalloys.md, a general grid part) and the
  furnace transformer to the equipment notes, and `rectifier_mv` to the DC tier's sources (dc-bus.md).

### 103. ores.md
- **Proposed:** when 15a lands, the salt dome's placement moves to the seed grid (its table row and "Real Gulf Coast domes trap oil on their
  flanks; that link waits for a later wave" line); each ore's "No use yet" changes as its plant lands (OreUsesTest keeps them honest).

---

## Figures not verified
Every *(unverified)* in the specs. The ones that matter most before approval:
- tariff gas quality limits and fuel retention (sales-gas.md);
- the SAF's FeMn and FeSi MWh/t, power factor, and Søderberg paste use (ferroalloys.md);
- nickel electric-furnace kWh/t and chloride electrowinning voltage (nickel-smelter.md);
- phosphoric acid's sulfuric acid use per tonne of P2O5, francolite's P2O5 (fertilizer.md);
- uranium reaching phosphoric acid and DEPA-TOPO recovery (uranium-from-phosphate.md);
- the chloride process's chlorine make-up, ilmenite smelting MWh/t (titanium-pigment.md);
- decrepitation energy and the potline's lithium effect (lithium.md);
- ISR monitor-ring spacing, CO2 dose and effect, RO energy (isr-operations.md);
- cylinder heel limits, real centrifuge spin-up hours (enrichment-operations.md);
- PAR capacity, relief tank disc pressure, P-T curve, OPΔT constants, cask heat limits (reactor-extensions.md);
- Mekanism's FE to Joule ratio and machine draws for 26.3 (mekanism-check.md).
