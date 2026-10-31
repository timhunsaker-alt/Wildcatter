# Moving parts and sound (APPROVED 2026-10-30 with the Phase 4 plan)

Tim asked for machines that visibly move, like the Create mod, with proper sounds. These are the rules.

## Moving parts
- A machine part that turns (a flywheel, crank pulley, alternator shaft, radiator or tower fan, turbine rotor, pump coupling,
  disconnect blade) is drawn by `client/KineticRenderer` as a separate rotor model turned about its axis.
- The speed is the machine's real speed: the alternator from the island frequency (120·f / poles), and everything else from its own
  state. It speeds up and coasts down with the machine's inertia, stops when tripped, and slows with frequency.
- Shown at real rpm / 20 (1,800 rpm shows 1.5 turns per second) so it reads as turning and does not strobe. This is a display choice, not physics.
- Slow machines show at their real speed (Phase 4.5g, docs/multiblocks/well-site.md): the pumpjack's crank at 9 rpm and the small
  rig's top head at 80 rpm. A part can also move along a linkage instead of turning about one axis: the pumpjack's walking beam
  rocks, its pitman arms follow the crank pins and its polished rod goes up and down (`kinetic/BeamLinkage`).
- The server sends the speed to the client only when it changes by more than 2 %. The client turns the part between updates.

## Sound
- A looping machine sound (`client/MachineSound`) plays while a machine runs. Its pitch follows speed and its volume follows load.
  It fades out when the machine stops or the block is gone.
- One-shots: breaker and HV breaker clunks, fuse pop, insulator flashover crack, disconnect arc, out-of-sync bang, engine start crank.
- Sources come from Ludo (createSoundEffect), are kept in `art/sound_source/`, and are processed by `tools/process_sounds.py`.

| Sound | Kind | Machine |
|---|---|---|
| `diesel_idle`, `diesel_load` | loop | diesel genset |
| `gas_engine` | loop | gas genset |
| `alternator_whine` | loop | alternators, the steam plant generator |
| `fan` | loop | radiator, cooling tower, refinery |
| `turbine_whine` | loop | steam turbine |
| `burner_roar` | loop | boiler burner |
| `transformer_hum` | loop | transformers, by load |
| `motor_hum` | loop | pumps, pumpjack, rig, feed pump |
| `engine_crank` | one-shot | genset start |
| `hv_breaker` | one-shot | HV and MV breakers |
| `fuse_pop` | one-shot | fuses and cutouts |
| `flashover` | one-shot | insulators |
| `disconnect_arc` | one-shot | disconnect switch |
| `sync_bang` | one-shot | out-of-sync close |
