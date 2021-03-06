package cd4017be.lib.tileentity;

import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import cd4017be.lib.templates.MultiblockComp;
import cd4017be.lib.templates.SharedNetwork;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/**
 * 
 * @author CD4017BE
 * @deprecated use {@link PassiveMultiblockTile}
 */
@Deprecated
public class MultiblockTile<C extends MultiblockComp<C, N>, N extends SharedNetwork<C, N>> extends BaseTileEntity implements ITickable, INeighborAwareTile {

	protected C comp;

	@Override
	public void update() {
		if (comp.network != null) comp.network.updateTick(comp);
		if (comp instanceof ITickable) ((ITickable)comp).update();
	}

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing s) {
		if (comp instanceof ICapabilityProvider) return ((ICapabilityProvider)comp).hasCapability(cap, s);
		if (cap == comp.getCap()) return true;
		return super.hasCapability(cap, s);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing s) {
		if (comp instanceof ICapabilityProvider) return ((ICapabilityProvider)comp).getCapability(cap, s);
		if (cap == comp.getCap()) return (T)comp;
		return super.getCapability(cap, s);
	}

	@Override
	public void neighborTileChange(TileEntity te, EnumFacing side) {
		comp.updateCon = true;
	}

	@Override
	public void neighborBlockChange(Block b, BlockPos src) {
		comp.updateCon = true;
	}

	@Override
	protected void setupData() {
		comp.setUID(SharedNetwork.ExtPosUID(pos, world.provider.getDimension()));
	}

	@Override
	protected void clearData() {
		if (comp.network != null) comp.network.remove(comp);
	}

}
