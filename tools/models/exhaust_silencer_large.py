"""Exhaust silencer for the 1 MW gas set: about 2.5 m long, 0.7 m across, 3 x 1 x 1 (docs/multiblocks/genset.md)."""
from silencer import silencer

M = silencer("exhaust_silencer_large", 3, 0.35, 2.5, (0.3, 1.4, 2.6), 8)
