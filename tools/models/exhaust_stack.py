"""Exhaust silencer for the 500 kW diesel set: about 1.8 m long, 0.5 m across, 2 x 1 x 1 (docs/multiblocks/genset.md)."""
from silencer import silencer

M = silencer("exhaust_stack", 2, 0.25, 1.6, (0.3, 1.7), 6)
