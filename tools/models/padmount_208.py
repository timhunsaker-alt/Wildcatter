"""75 kVA three-phase pad-mount, 12.47 kV to 208Y/120 V, 3 %Z (docs/multiblocks/padmount.md): about 1.37 W x 1.27 H x 1.25 D m."""
import padlib

M = padlib.padmount("padmount_208", 1.37, 1.27, 1.25, 14)
