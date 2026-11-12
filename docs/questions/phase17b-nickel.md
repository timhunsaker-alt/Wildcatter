# Phase 17b, the nickel smelter, refinery and alloy melt shop: decisions taken while Tim was away (2026-11-12)

Spec: `docs/multiblocks/nickel-smelter.md` (DRAFT, decided on recommendation; its "As built" section lists what differs). The spec's own
choices are `docs/questions/specs-wildcatter.md`, items 47-51. Each item below was decided on the best real-world recommendation; for Tim.
Format as in `docs/questions/README.md`.

## 17b1: the smelter and the refinery

### 1. A block of nickel ore is 4 t of feed, as the spec says, not the ore table's 2.9 t
- **Decided on recommendation; for Tim.** The roaster counts an ore item as 4,000 kg of feed at 12 % Ni (480 kg of Ni), the spec's
  number. `Ore.PENTLANDITE` keeps its 2.9 t/m3 rock density for the other readouts.
- **Real counterpart:** a massive Sudbury sulfide ore at 60 % sulfides weighs about 4.0-4.5 t/m3 (pyrrhotite 4.6, pentlandite 4.8,
  chalcopyrite 4.2; Sudbury host norite 2.9). Unverified.

### 2. Tailings' concentrate counts kg for kg as feed
- **Decided on recommendation; for Tim.** The pentlandite in a thickened concentrate's assay is counted as feed at the concentrate grade
  (12 % Ni), kg for kg.
- **Real counterpart:** Sudbury's bulk Ni-Cu concentrate is about 10-13 % Ni (Vale and Glencore reports); pure pentlandite is 34 % Ni. The
  assay carries the mineral, not a grade, so the feed grade is used.

### 3. Calcine is counted at 1 t a tonne of feed
- **Decided on recommendation; for Tim.** Roasting 2 t of feed gives 2 t of calcine, so the spec's 500 kWh a tonne of calcine is its 1 MW.
- **Real counterpart:** FeS + 1.5 O2 -> FeO + SO2 loses 18 % of the sulfide's mass; partial roasting with oxygen picked up by the iron and
  dust return keeps calcine at 85-100 % of feed. The spec sized the furnace on 2 t/h.

### 4. The nickel furnace holds its own 1 MW on the 17a transformer
- **Decided on recommendation; for Tim.** The furnace transformer's lowest tap gives 3.9 MW to the 17a SAF; the nickel furnace asks for
  `min(tap power, 1 MW)` (its electrode regulator), at the transformer's PF 0.80. A transformer finds either furnace by touching
  (`TransformerFurnace`); the SAF's behaviour is unchanged.
- **Real counterpart:** six-in-line matte furnaces run at PF 0.9 or better on resistance heating (Falconbridge Sudbury's furnace about
  0.95, unverified); the PF stays the transformer's 0.80 because the transformer's load is built once with its PF (a later change).

### 5. Furnace matte grade 25 % Ni, slag 0.7 t a tonne, paste 2.5 kg a tonne
- **Decided on recommendation; for Tim.** Furnace matte carries 25 % Ni (its Cu at the feed's 1:3 makes 33 % Ni+Cu); 0.7 t of slag a tonne
  of calcine (counted); Soderberg paste 2.5 kg a tonne of calcine (green anodes, as the SAF); the hearth reads FULL at 10 t of matte.
- **Real counterpart:** Sudbury furnace matte 30-35 % Ni+Cu (Xstrata modelling paper, the spec's source); matte furnaces burn 2-3 kg of
  electrode a tonne (textbook, unverified).

### 6. A blow charges up to 4 t of furnace matte and waits for 0.5 t
- **Decided on recommendation; for Tim.** The converter charges what the furnace holds, up to 4 t (two hours of the furnace's matte),
  once there is at least 0.5 t, and blows for 2 hours (the spec's), divided by the world's time scale (`TimeScale.scaled`). Its slag back
  to the furnace is counted.
- **Real counterpart:** Sudbury's Peirce-Smith converters take 40-100 t charges over several hours; this smelter is small.

### 7. The converter's off-gas: 0.35 kg of sulfur a kg of Ni
- **Decided on recommendation; for Tim.** What the roaster left in the calcine (30 % of the feed's sulfur) less the Bessemer matte's 20 %:
  about 84 kg/h of sulfur to the acid plant at the line's rate, on the same pipe as the roaster's 420 kg/h.
- **Real counterpart:** mass balance on the spec's numbers (derived).

### 8. The leach's chlorine: the anodes' back first, make-up off the pipe; on its north side
- **Decided on recommendation; for Tim.** The leach takes 1.62 kg of Cl2 a kg of Ni (nickel, copper and cobalt to their chlorides); the
  tankhouse returns 1.21 kg a kg of Ni won (about 70 %), the rest is make-up from the pipe, which also gives the first fill. The make-up
  pipe goes on the **north** face of its bottom cells: the spec's layout puts the tankhouse against its east face, where the spec's text
  put the chlorine. The leach stops (NO CHLORINE) when the tankhouse has liquor but no DC for five seconds, or is gone.
- **Real counterpart:** Nikkelverk's chlorine circuit returns the anode gas to the leach; make-up chlorine is bought (Glencore Nikkelverk,
  textbook).

### 9. Sizes not in the spec
- **Decided on recommendation; for Tim.** The leach takes 0.5 t of Bessemer matte an hour (250 kg of Ni, ahead of the cells' 230);
  purification takes up to 1.2 times that; liquor buffers hold 500 kg of Ni; the tankhouse holds 3 t of cathode for bags; the roaster
  2 t of concentrate and 8 t of calcine; Bessemer matte 10 t.
- **Real counterpart:** buffers sized for a few hours of the line, as the mod's other process lines.

### 10. Nickel recovery and the by-products
- **Decided on recommendation; for Tim.** 95 % of the leached nickel reaches the cathode (the spec's recovery), the rest counted with the
  iron and cobalt residues; copper cast into vanilla copper ingots of 100 kg (as the iron ingot) for a hopper under the purification
  circuit; cobalt counted as cobalt carbonate (CoCO3, 2.02 kg a kg of Co).
- **Real counterpart:** Nikkelverk recovers above 98 % Ni overall; copper there is electrowon (left out, the spec).

### 11. The cells' back-EMF: 1.6 V
- **Decided on recommendation; for Tim.** Each chloride cell is 1.6 V of back-EMF behind a resistance that gives 3.5 V at 12.3 kA.
- **Real counterpart:** NiCl2 decomposition: E(Cl2/Cl-) 1.36 V less E(Ni2+/Ni) -0.26 V = 1.62 V (standard potentials, textbook).

### 12. The tankhouse's terminals and its rectifier at its south end; the purification's box north
- **Decided on recommendation; for Tim.** The tankhouse's + and - are on its south end (in its own frame) at x 1; a rectifier set
  against it takes its 12.3 kA rating, as the magnesium cell's does. The purification circuit's motor box is on its north face: its
  west side stands against the leach reactor in the scene's layout.

### 13. Placeholders
- The two bulk bag icons (`bessemer_matte_bag_placeholder`, `nickel_cathode_bag_placeholder`) are the Ludo bulk bag with a swatch drawn
  by script (as 17a's). Listed in PROGRESS.md, Known gaps.
