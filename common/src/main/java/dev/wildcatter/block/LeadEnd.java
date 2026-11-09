package dev.wildcatter.block;

import dev.wildcatter.energy.Voltage;
import java.util.Set;
import net.minecraft.core.BlockPos;

/**
 * Pole equipment a lead (a jumper) can come down to from a line insulator (docs/multiblocks/poles.md, "Leads"): a fused cutout's line
 * terminal or a pole transformer's H1 bushing. The insulator owns the lead and builds it into the circuit; this end publishes the node
 * the lead lands on and remembers which insulators feed it, so the two are solved as one circuit.
 */
public interface LeadEnd {
	/** The voltage of the terminal a lead lands on; null if this part takes no lead (not a cutout, or not a pole unit). */
	Voltage leadVoltage();

	/** The insulators whose leads land here. */
	Set<BlockPos> leadsIn();

	/** How high in its cell the lead lands (the bushing or the cutout's top contact), for the drawing. */
	double leadY();

	default boolean takesLead() {
		return leadVoltage() != null && leadsIn().isEmpty();
	}
}
