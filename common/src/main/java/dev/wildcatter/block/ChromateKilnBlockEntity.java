package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.Zirconium;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The chromate kiln (docs/multiblocks/zirconium-extensions.md item 5, 17c2): a rotary kiln at about 1,100 C roasting chromite with caustic
 * soda in air to sodium chromate, then a leach and a sulfur reduction to chromic oxide, 3 x 3 x 8. Chromite and sulfur by a hopper on top;
 * 32 % caustic soda off the pipe at the north face of its front cells; gas at the east face of its bottom cells, 0.3 MW; 25 kg sacks of
 * chromic oxide out to a hopper under it, a sack an hour. Its drive and fans: 15 kW on the industrial supply, box west. Iron oxide and
 * sodium sulfate are counted. Hexavalent chromium: broken while it holds chromate, it leaves a toxic spill that poisons whoever stands in
 * it.
 */
public class ChromateKilnBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private static final int CHROMITE = 0, SULFUR = 1, OUT = 2;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(3, ItemStack.EMPTY);
	private double chromite, sulfur;   // kg on hand
	private double chromate;  // kg of Cr2O3 it will make, held as sodium chromate in the kiln and leach
	private double sulfate;   // kg of Na2SO4 counted
	private String unit = "starting";

	public ChromateKilnBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CHROMATE_KILN_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	private boolean takes(int slot, ItemStack stack) {
		return slot == CHROMITE && stack.is(OreBlocks.MINERALS.get(Ore.CHROMITE)) || slot == SULFUR && stack.is(OreBlocks.MINERALS.get(Ore.SULFUR));
	}

	@Override
	public boolean takes(ItemStack stack) {
		return takes(CHROMITE, stack) || takes(SULFUR, stack);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {CHROMITE, SULFUR} : side == Direction.DOWN ? new int[] {OUT} : new int[0];
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return takes(slot, stack);
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side == Direction.UP && takes(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == OUT && side == Direction.DOWN;
	}

	private List<Lines.Face> caustic() {
		return faces(at -> at[1] == 0 && at[2] == 0, Direction.NORTH);
	}

	private List<Lines.Face> gas() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	public double chromate() {
		return chromate;
	}

	public int sacks() {
		return slots.get(OUT).getCount();
	}

	/** For the game test: chromate as if roasted, a sack all but made. */
	public void addChromateForTest(double kg) {
		chromate += kg;
		setChanged();
	}

	/** Broken while it holds chromate: a spill of hexavalent chromium round it, poison to whoever stands in it. */
	public void spill(ServerLevel level) {
		if (chromate < 1e-3) {
			return;
		}
		BlockPos centre = LeasePartBlock.at(worldPosition, getBlockState(), 1, 0, 4);
		AreaEffectCloud cloud = new AreaEffectCloud(level, centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5);
		cloud.setRadius(4.0F);
		cloud.setRadiusPerTick(0);
		cloud.setDuration((int) (Tuning.CHROMATE_SPILL_SECONDS * 20));
		cloud.setWaitTime(0);
		cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
		level.addFreshEntity(cloud);
		chromate = 0;
	}

	@Override
	protected void work(Level level) {
		if (chromate >= Tuning.CR2O3_SACK_KG - 1e-6) {
			// the leach and the sulfur reduction take a sack's worth of chromate at a time
			if (sulfur < Tuning.CR2O3_SACK_KG * Zirconium.sulfurPerCr2o3() && !slots.get(SULFUR).isEmpty()) {
				slots.get(SULFUR).shrink(1);
				sulfur += Ore.SULFUR.kgPerBlock();
			}
			if (sulfur < Tuning.CR2O3_SACK_KG * Zirconium.sulfurPerCr2o3()) {
				unit = "NO SULFUR: put sulfur in its hopper (it reduces the chromate)";
				return;
			}
			ItemStack out = slots.get(OUT);
			if (!out.isEmpty() && out.getCount() >= out.getMaxStackSize()) {
				unit = "FULL: take the sacks of chromic oxide (a hopper under it)";
				return;
			}
			if (out.isEmpty()) {
				slots.set(OUT, new ItemStack(WildcatterItems.CHROMIC_OXIDE));
			} else {
				out.grow(1);
			}
			chromate -= Tuning.CR2O3_SACK_KG;
			sulfur -= Tuning.CR2O3_SACK_KG * Zirconium.sulfurPerCr2o3();
			sulfate += Tuning.CR2O3_SACK_KG * Zirconium.sulfatePerCr2o3();
			setChanged();
		}
		double want = Tuning.CR2O3_KGS * seconds();
		if (chromite < want * Zirconium.chromitePerCr2o3() && !slots.get(CHROMITE).isEmpty()) {
			slots.get(CHROMITE).shrink(1);
			chromite += Ore.CHROMITE.kgPerBlock();
		}
		if (chromite < want * Zirconium.chromitePerCr2o3()) {
			unit = "NO CHROMITE: put chromite in its hopper";
			return;
		}
		double naoh = Lines.available(level, caustic(), Product.CAUSTIC_SODA, Pull.MACHINE, this) * Tuning.CAUSTIC_STRENGTH;
		if (naoh <= 1e-9) {
			unit = "NO CAUSTIC: pipe 32 % caustic soda to the north face of its front cells";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting the kiln drive: " + describe();
			return;
		}
		double kg = Math.min(want * strength, naoh / Zirconium.naohPerCr2o3());
		double need = kg / Tuning.CR2O3_KGS * Tuning.CHROMATE_KILN_GAS_WATTS;   // joules: the kiln's 0.3 MW at its rate
		double got = GasFiring.fire(level, gas(), need, this);
		kg *= Math.min(1, got / Math.max(need, 1e-9));
		if (kg <= 1e-12) {
			unit = "NO GAS: pipe gas or LPG to the east face of its bottom cells";
			return;
		}
		Lines.pull(level, caustic(), Product.CAUSTIC_SODA, kg * Zirconium.naohPerCr2o3() / Tuning.CAUSTIC_STRENGTH, Pull.MACHINE, this);
		chromite -= kg * Zirconium.chromitePerCr2o3();
		chromate += kg;
		unit = String.format("roasting chromite with caustic soda at 1,100 C: %.1f kg/h of chromic oxide, %.0f kW of gas; chromate %.1f of a %.0f kg sack", kg / seconds() * 3600,
				got / seconds() / 1000, chromate, Tuning.CR2O3_SACK_KG);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Chromate kiln (%.0f kW, %s): %s; chromite %,.0f kg, sulfur %,.0f kg on hand; %d sacks for a hopper; Na2SO4 %,.0f kg; %s",
				Tuning.CHROMATE_KILN_WATTS / 1000, Voltage.INDUSTRIAL.label(), unit, chromite, sulfur, sacks(), sulfate,
				chromate > 1e-3 ? "HOLDS HEXAVALENT CHROMIUM; " : "") + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("chromite", chromite);
		output.putDouble("sulfur", sulfur);
		output.putDouble("chromate", chromate);
		output.putDouble("sulfate", sulfate);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		chromite = input.getDoubleOr("chromite", 0);
		sulfur = input.getDoubleOr("sulfur", 0);
		chromate = input.getDoubleOr("chromate", 0);
		sulfate = input.getDoubleOr("sulfate", 0);
	}
}
