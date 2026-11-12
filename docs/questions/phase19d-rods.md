# Phase 19d, rods and the sodium sulfate: decisions taken while Tim was away (2026-11-12)

Spec: `docs/multiblocks/reactor-extensions.md` items 9-11 (DRAFT, decided on recommendation; "As built (19d)" there). The spec's own
choices are in `docs/questions/specs-wildcatter.md`, items 85-96. Format as in `docs/questions/README.md`. Each entry was decided on
recommendation; for Tim. Entries marked **(needs you)** change something outside this phase or pick between real options.

### 1. B-10 burnup and SDM LOW are behind a world rule, `reactorRodWear`
- **Chosen:** a world rule `reactorRodWear` (on in new worlds, off in worlds saved before): with it a B4C RCCA burns its B-10, its worth
  falls past 40 %, the vessel reads each RCCA and the shutdown margin, and the console alarms SDM LOW. Off, a reactor runs and reads as
  before. A hafnium RCCA is a new part: with one loaded the vessel reads the RCCAs and the margin (and SDM LOW) without the rule.
- **Why:** the brief: anything that changes an existing reactor goes behind a world rule.

### 2. How fast the boron burns (needs you)
- **Chosen:** a B4C RCCA fully inserted at full power reaches 30 % B-10 burnup at its tip in **5 effective full-power years**
  *(proposed, unverified)*, at the reactor's 100x burnup: about **438 hours** of world time. The rate is the power times the share of the
  RCCA inserted (the S-curve of 8a), not a computed share of the core's absorption. Bank D (the centre RCCA) is in at power; the
  shutdown bank is out, so it hardly wears.
- **Real counterpart:** PWR and CE B4C rods are replaced for tip depletion and cladding strain after roughly 10-15 years of service in
  the rodded region (textbook; the 5 EFPY is the time *fully inserted at power*, which a real lead bank never is).
- **Alternatives:** a share of the core's absorption from the rods' worth (a second number to tune, the same shape).

### 3. The shutdown margin is the rods', as the design check
- **Chosen:** the vessel reads the margin of the RCCAs loaded: every RCCA but the one of highest worth, less the power defect from full
  power (Doppler and moderator), as `ReactorDesign`'s design check. It does not move with boron, xenon or temperature. A fresh B4C set
  reads **2,212 pcm**; a hafnium set **1,552 pcm** (over the limit); a hafnium centre with four B4C RCCAs at 60 % reads 892 pcm and
  SDM LOW.
- **Real counterpart:** NUREG-1431 LCO 3.1.1: in Modes 1 and 2 the SDM is assured by the rod insertion limits (a design calculation with
  the most reactive rod stuck out); in Modes 3-5 by boron. The boron side is not modelled.
- **Alternatives:** the margin from the core's present reactivity (boron, xenon, temperature): it would read SDM LOW in every cold
  unborated core at loading.

### 4. Ag-In-Cd waits for a zinc refinery
- **Chosen:** not built. Its worth, 90 % of B4C's *(proposed, unverified)*, is recorded in Tuning (`AIC_RCCA_WORTH`) for when indium
  and cadmium exist.

### 5. RCCAs come out by hand, carrying their B-10 (needs you)
- **Chosen:** a sneak-click on the cold open vessel takes the last RCCA out as before; a B4C one carries its B-10 burnup on the item
  (custom data `b10`) and keeps it when loaded again. The refuelling machine does not swap RCCAs and a replaced RCCA is not a pool
  record.
- **Why:** the spec's swap at the refuelling machine and a pool record for RCCAs need a second record kind in the pool and its decay
  heat; the item route needed nothing new. Real irradiated RCCAs are highly activated and go to the pool.
- **Alternatives:** the swap and the record as the spec proposes (a later wave).

### 6. Bank D is the first RCCA loaded
- **Chosen:** as 8a: the first RCCA loaded is the centre one (bank D), the next four the shutdown bank; out last-in first-out.

### 7. The hafnium RCCA at the bench
- **Chosen:** 24 hafnium rodlets in the bench's tray hopper (in place of 9 trays of B4C), 20 kg of steel for the spider and hardware, an
  hour, as the B4C RCCA. 120 rodlets (5 RCCAs) are about 2,100 hours of the zirconium line's hafnium (17c1).

### 8. Sodium sulfate bags come from the borax dissolver only
- **Chosen:** the borax dissolver (where the boron plant counts its Na2SO4, 0.57 kg a kg of boric acid) fills 1 t bags at a bagging
  station set against it (`sodium_sulfate_bag`, tagged `c:sodium_sulfate`). An existing dissolver's counted sulfate becomes bags once a
  station is set against it. The lithium plant (20c, merged later on `w-next`) bags its sulfate as the same `sodium_sulfate_bag` (one item kept on the merge); the chromate kiln (17c2) keeps its sulfate counted.

### 9. The bags are dumped through the decant tower
- **Chosen:** a lined cell's decant tower takes sodium sulfate bags from a hopper on top, or a right-click with one: each adds 1 t to
  its settled tails, read as "t sodium sulfate". It refuses a bag when the cell is full or breached.
- **Alternatives:** a glass batch recipe (the spec's alternative; Wildcatter has no glass works).
