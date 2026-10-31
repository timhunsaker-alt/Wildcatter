"""300 kVA three-phase pad-mount, 12.47 kV to 480Y/277 V, 5 %Z (docs/multiblocks/padmount.md): about 1.75 W x 1.40 H x 1.60 D m."""
import padlib

M = padlib.padmount("padmount_480", 1.75, 1.40, 1.60, 18)
