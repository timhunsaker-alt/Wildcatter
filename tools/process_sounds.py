#!/usr/bin/env python3
"""Converts the Ludo sound effects (mp3 / wav) into the mono Ogg Vorbis files Minecraft wants.

Usage: python3 tools/process_sounds.py [folder]   (needs numpy and soundfile; the folder defaults to art/sound_source)
Keep the Ludo sources in art/sound_source/<name>.mp3|wav. Writes src/main/resources/assets/wildcatter/sounds/*.ogg and sounds.json.
One-shots are trimmed of silence and faded at both ends. Loops (docs/kinetics-and-sound.md) are left whole, because Ludo made
them to loop seamlessly and trimming would put a click in the seam.
"""
import json, os, sys
import numpy as np
import soundfile as sf

src = sys.argv[1] if len(sys.argv) > 1 else "art/sound_source"
out = "src/main/resources/assets/wildcatter/sounds"
os.makedirs(out, exist_ok=True)
ONE_SHOTS = ("turbine_hum", "boiler_hiss", "breaker_trip", "breaker_click", "zap", "structure_formed", "engine_crank", "sync_bang", "disconnect_arc", "hv_breaker", "fuse_pop", "flashover")
LOOPS = ("diesel_idle", "diesel_load", "gas_engine", "alternator_whine", "fan", "turbine_whine", "burner_roar", "motor_hum")
for name in ONE_SHOTS + LOOPS:
	found = [f for f in os.listdir(src) if f.startswith(name + ".")] if os.path.isdir(src) else []
	if not found:
		continue   # no source kept for it: the .ogg already in the mod stays
	data, rate = sf.read(os.path.join(src, found[0]), always_2d=True)
	mono = data.mean(axis=1)
	if name in ONE_SHOTS:
		loud = np.abs(mono) > 0.01
		if loud.any():  # trim the silence at both ends
			first, last = np.argmax(loud), len(loud) - np.argmax(loud[::-1])
			mono = mono[max(0, first - 200):last + 200]
	peak = np.abs(mono).max()
	if peak > 0:
		mono = mono / peak * 0.9
	if name in ONE_SHOTS:
		fade = min(len(mono) // 20, int(rate * 0.02))
		mono[:fade] *= np.linspace(0, 1, fade)
		mono[-fade:] *= np.linspace(1, 0, fade)
	sf.write(f"{out}/{name}.ogg", mono, rate, format="OGG", subtype="VORBIS")
	print(name, f"{len(mono) / rate:.2f}s")
present = [n for n in ONE_SHOTS + LOOPS if os.path.exists(f"{out}/{n}.ogg")]
with open("src/main/resources/assets/wildcatter/sounds.json", "w") as f:
	json.dump({n: {"sounds": [f"wildcatter:{n}"], "subtitle": f"subtitles.wildcatter.{n}"} for n in present}, f, indent=2)
	f.write("\n")
