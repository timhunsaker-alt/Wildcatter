package dev.wildcatter.block;

import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;

/** A pipe source whose slurry carries an assay (the concentrator slurries). A source that is not one gives an empty assay. */
public interface Assayed {
	SlurryAssay assayOf(Product product);
}
