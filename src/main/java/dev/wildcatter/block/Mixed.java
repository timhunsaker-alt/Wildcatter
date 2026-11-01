package dev.wildcatter.block;

import dev.wildcatter.fluid.Product;

/** A pipe source whose product carries what it is made of (cracked gas, the mill's liquors, lixiviant), five shares. */
public interface Mixed {
	double[] mixOf(Product product);
}
