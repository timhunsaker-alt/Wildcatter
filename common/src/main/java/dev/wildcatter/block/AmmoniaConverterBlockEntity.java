package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.AmmoniaPlant;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The ammonia converter and its loop (docs/multiblocks/ammonia-plant.md): an iron-catalyst converter at 400-450 C, its recycle
 * circulator, heat exchangers and the ammonia chiller that condenses the product. It takes syngas from the syngas compressor
 * touching it; about 15 % converts a pass and the rest goes round again. Liquid ammonia out of the south face of its bottom cells.
 * The circulator and chiller are its load (10 kW at the industrial voltage); terminal box on the west face of its main cell.
 */
public class AmmoniaConverterBlockEntity extends UnitMotorBlockEntity {
	private double making;   // kg/h of ammonia, the last interval
	private String unit = "starting";

	public AmmoniaConverterBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.AMMONIA_CONVERTER_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 2 && at[1] == 0 && side == Direction.SOUTH ? Product.AMMONIA : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.AMMONIA ? AmmoniaPlant.ammoniaKgh() / Tuning.AMMONIA_KG_PER_L / 3600 * Tuning.PLANT_HOLD_SECONDS * 4 : 0;
	}

	/** kg/h of liquid ammonia it made over the last interval. */
	public double making() {
		return making;
	}

	@Override
	protected void work(Level level) {
		making = 0;
		SyngasCompressorBlockEntity compressor = Touching.find(level, worldPosition, SyngasCompressorBlockEntity.class, c -> true);
		if (compressor == null) {
			unit = "NO SYNGAS: no syngas compressor touching it";
			return;
		}
		double roomKg = room(Product.AMMONIA) * Tuning.AMMONIA_KG_PER_L;
		if (roomKg <= 1e-7) {
			unit = "idle: FULL, its ammonia has nowhere to go (pipe its south side to a bullet)";
			return;
		}
		if (plcHold(level)) {
			unit = status;
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "COLD: its circulator and chiller are not running (" + describe() + ")";
			return;
		}
		double maxNm3 = Math.min(AmmoniaPlant.ammoniaNm3h() / 3600 * seconds() * strength, roomKg / AmmoniaPlant.ammoniaKgPerNm3());
		double syngas = compressor.takeSyngas(maxNm3 * 2);   // 4 Nm3 of syngas make 2 of ammonia
		if (syngas <= 1e-9) {
			unit = "NO SYNGAS: its compressor is not delivering (" + compressorState(compressor) + ")";
			return;
		}
		double kg = syngas / 2 * AmmoniaPlant.ammoniaKgPerNm3();
		add(Product.AMMONIA, kg / Tuning.AMMONIA_KG_PER_L);
		making = kg / seconds() * 3600;
		unit = String.format("converting at 400-450 C and %.0f bar, %.0f %% a pass: %.1f kg/h of liquid ammonia (%.1f L/h)", Tuning.SYNGAS_LOOP_BAR,
				Tuning.AMMONIA_PER_PASS * 100, making, making / Tuning.AMMONIA_KG_PER_L);
	}

	private static String compressorState(SyngasCompressorBlockEntity c) {
		String s = c.inspect().getString();
		int colon = s.indexOf("): ");
		int semi = s.indexOf(';');
		return colon >= 0 && semi > colon ? s.substring(colon + 3, semi) : s;
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Ammonia converter (circulator and chiller %.0f kW, %s): ", Tuning.AMMONIA_CONVERTER_WATTS / 1000, Voltage.INDUSTRIAL.label())
				+ unit + String.format("; ammonia held %.2f L; ", held(Product.AMMONIA)) + describe());
	}

	// ---- PLC points: RUN, RUNNING, a 4-20 mA rate ---------------------------------------------------------------------------------
	@Override
	public List<Point> points() {
		return RatePoints.POINTS;
	}

	@Override
	public boolean readPoint(int point) {
		return RatePoints.read(point, making > 0, unitRemote.auto());
	}

	@Override
	public double readMa(int point) {
		return RatePoints.ma(making, AmmoniaPlant.ammoniaKgh());
	}
}
