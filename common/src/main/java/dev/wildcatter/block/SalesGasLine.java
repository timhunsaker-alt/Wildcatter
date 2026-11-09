package dev.wildcatter.block;

import com.mojang.serialization.Codec;
import dev.wildcatter.Wildcatter;
import dev.wildcatter.fluid.SalesGas;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The transmission line off the map (docs/multiblocks/sales-gas.md): one per dimension, kept with that dimension's saved data. Receipt
 * stations put energy on its account, less the 2 % it keeps as compressor fuel; delivery stations take it out. The account is in GJ.
 */
public class SalesGasLine extends SavedData {
	public static final Codec<SalesGasLine> CODEC = Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).xmap(SalesGasLine::new, line -> line.totals);
	public static final SavedDataType<SalesGasLine> TYPE = new SavedDataType<>(Wildcatter.id("sales_gas_line"), SalesGasLine::new, CODEC, DataFixTypes.LEVEL);

	private final Map<String, Double> totals;

	public SalesGasLine() {
		this.totals = new HashMap<>();
	}

	private SalesGasLine(Map<String, Double> totals) {
		this.totals = new HashMap<>(totals);
	}

	/** The line of this dimension: every station in it is on the same system. */
	public static SalesGasLine get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	private double total(String key) {
		return totals.getOrDefault(key, 0.0);
	}

	private void add(String key, double amount) {
		totals.merge(key, amount, Double::sum);
		setDirty();
	}

	/** A receipt of {@code m3} of pipeline gas: books it at its heating value, the line keeping its fuel. */
	public void receive(double m3) {
		double gj = SalesGas.gj(m3);
		add("receivedM3", m3);
		add("receivedGj", gj);
		add("retainedGj", SalesGas.retained(gj));
	}

	/** A delivery of {@code m3}: the gas and its line heater's share come off the account. */
	public void deliver(double m3) {
		add("deliveredM3", m3);
		add("debitedGj", SalesGas.debit(m3));
	}

	/** GJ on the account: received, less what the line kept and what was delivered. */
	public double balance() {
		return Math.max(0, total("receivedGj") - total("retainedGj") - total("debitedGj"));
	}

	public double receivedGj() {
		return total("receivedGj");
	}

	public double retainedGj() {
		return total("retainedGj");
	}

	public double deliveredM3() {
		return total("deliveredM3");
	}

	/** Game test hook: the account drained, as if other stations had taken it all. */
	public void drainForTest() {
		add("debitedGj", balance());
	}
}
