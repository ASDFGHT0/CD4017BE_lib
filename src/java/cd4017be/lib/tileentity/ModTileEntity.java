package cd4017be.lib.tileentity;

import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.TileBlockRegistry;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.api.IAbstractTile;
import cd4017be.lib.TileBlockRegistry.TileBlockEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

/**
 *
 * @author CD4017BE
 * @deprecated replaced by BaseTileEntity
 */
@Deprecated
public class ModTileEntity extends TileEntity implements IAbstractTile {

	public int dimensionId;

	@Override
	public NBTTagCompound getUpdateTag() {
		return this.writeToNBT(new NBTTagCompound());
	}

	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing s, float X, float Y, float Z) {
		TileBlockEntry entry = TileBlockRegistry.getBlockEntry(this.getBlockType());
		if (entry != null && entry.container != null && !player.isSneaking()) {
			BlockGuiHandler.openGui(player, this.world, pos.getX(), pos.getY(), pos.getZ());
			return true;
		} else return false;
	}

	public EnumFacing getClickedSide(float X, float Y, float Z) {
		X -= 0.5F;
		Y -= 0.5F;
		Z -= 0.5F;
		float dx = Math.abs(X);
		float dy = Math.abs(Y);
		float dz = Math.abs(Z);
		return dy > dz && dy > dx ? Y < 0 ? EnumFacing.DOWN : EnumFacing.UP : dz > dx ? Z < 0 ? EnumFacing.NORTH : EnumFacing.SOUTH : X < 0 ? EnumFacing.WEST : EnumFacing.EAST;
	}

	public void onClicked(EntityPlayer player) {}

	public void onNeighborBlockChange(Block b, BlockPos src) {}

	public void onNeighborTileChange(BlockPos pos) {}

	public void breakBlock() {
		if (this.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
			IItemHandler access = this.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
			for (int i = access.getSlots() - 1; i >= 0; i--) {
				ItemStack item = access.extractItem(i, 65536, false);
				if (item != null) this.dropStack(item);
			}
		}
	}

	public int redstoneLevel(int s, boolean str) {return 0;}

	public void onPlaced(EntityLivingBase entity, ItemStack item) {}

	public void onEntityCollided(Entity entity) {}

	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		return new ArrayList<ItemStack>();
	}

	public TileEntity getLoadedTile(BlockPos pos) {
		if (world.isBlockLoaded(pos)) return world.getTileEntity(pos);
		else return null;
	}

	public <C> C getNeighborCap(Capability<C> cap, EnumFacing side) {
		BlockPos pos = this.pos.offset(side);
		if (world.isBlockLoaded(pos)) {
			TileEntity te = world.getTileEntity(pos);
			if (te != null)
				return te.getCapability(cap, side.getOpposite());
		}
		return null;
	}

	public void markUpdate() {
		IBlockState state = world.getBlockState(pos);
		this.world.notifyBlockUpdate(pos, state, state, 3);
	}

	@Override
	public void setWorld(World world) {
		super.setWorld(world);
		this.dimensionId = world.provider.getDimension();
	}

	public void onPlayerCommand(PacketBuffer data, EntityPlayerMP player) throws IOException {}

	public PacketBuffer getPacketTargetData() {
		return BlockGuiHandler.getPacketTargetData(pos);
	}

	public boolean canPlayerAccessUI(EntityPlayer player) {
		return !player.isDead && !this.tileEntityInvalid;
	}

	public void initContainer(DataContainer container) {}

	public boolean detectAndSendChanges(DataContainer container, PacketBuffer dos) {return false;}

	public void updateClientChanges(DataContainer container, PacketBuffer dis) {}

	public int[] getSyncVariables() {return null;}

	public void setSyncVariable(int i, int v) {}

	public void dropStack(ItemStack stack) {
		if (stack == null) return;
		EntityItem ei = new EntityItem(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
		world.spawnEntity(ei);
	}

	/**
	 * @return BTNSWE orientation of TileBlock in range 0-5
	 */
	public byte getOrientation() {
		return (byte)((this.getBlockMetadata() - 2) % 6);
	}

	public String getName() {
		return I18n.translateToLocal(this.getBlockType().getUnlocalizedName().replace("tile.", "gui.").concat(".name"));
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
	}

	@Override
	public ICapabilityProvider getTileOnSide(EnumFacing s) {
		return this.getLoadedTile(pos.offset(s));
	}

	public BlockPos pos() {return pos;}

	@Override
	public boolean invalid() {
		return tileEntityInvalid;
	}

}
