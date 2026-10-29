#!/usr/bin/env python3
"""Converts the Ludo sound effects (mp3 / wav) into the mono Ogg Vorbis files Minecraft wants.

Usage: python3 tools/process_sounds.py <folder with the downloaded files>   (needs numpy and soundfile)
Writes src/main/resources/assets/wildcatter/sounds/*.ogg and sounds.json.
"""
import json, os, sys
import numpy as np
import soundfile as sf

src = sys.argv[1]
out = "src/main/resources/assets/wildcatter/sounds"
os.makedirs(out, exist_ok=True)
names = ("turbine_hum", "boiler_hiss", "breaker_trip", "breaker_click", "zap", "structure_formed")
for name in names:
	path = next(os.path.join(src, f) for f in os.listdir(src) if f.startswith(name + "."))
	data, rate = sf.read(path, always_2d=True)
	mono = data.mean(axis=1)
	loud = np.abs(mono) > 0.01
	if loud.any():  # trim the silence at both ends
		first, last = np.argmax(loud), len(loud) - np.argmax(loud[::-1])
		mono = mono[max(0, first - 200):last + 200]
	peak = np.abs(mono).max()
	if peak > 0:
		mono = mono / peak * 0.9
	fade = min(len(mono) // 20, int(rate * 0.02))
	mono[:fade] *= np.linspace(0, 1, fade)
	mono[-fade:] *= np.linspace(1, 0, fade)
	sf.write(f"{out}/{name}.ogg", mono, rate, format="OGG", subtype="VORBIS")
	print(name, f"{len(mono) / rate:.2f}s")
with open("src/main/resources/assets/wildcatter/sounds.json", "w") as f:
	json.dump({n: {"sounds": [f"wildcatter:{n}"], "subtitle": f"subtitles.wildcatter.{n}"} for n in names}, f, indent=2)
	f.write("\n")
