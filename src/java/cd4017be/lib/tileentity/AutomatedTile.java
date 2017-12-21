package cd4017be.lib.tileentity;

import cd4017be.api.Capabilities;
import cd4017be.api.automation.IOperatingArea;
import cd4017be.api.automation.PipeEnergy;
import cd4017be.api.protect.PermissionUtil;
import cd4017be.lib.BlockGuiHandler.ClientPacketReceiver;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.capability.Inventory;
import cd4017be.lib.capability.TankContainer;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * The ultimate TileEntity template for lazy people
 * @author CD4017BE
 */
@Deprecated
public class AutomatedTile extends BaseTileEntity implements ITickable, ClientPacketReceiver {

	public Inventory inventory;
	public TankContainer tanks;
	public PipeEnergy energy;

	@Override
	public void update() {
		if (world.isRemote) return;
		if (inventory != null) inventory.update(this);
		if (tanks != null) tanks.update(this, inventory);
		if (energy != null) energy.update(this);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		if (inventory != null) inventory.readFromNBT(nbt, "Items"); 
		if (tanks != null) tanks.readFromNBT(nbt, "tank");
		if (energy != null) energy.readFromNBT(nbt, "wire");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		if (inventory != null) inventory.writeToNBT(nbt, "Items");
		if (tanks != null) tanks.writeToNBT(nbt, "tank");
		if (energy != null) energy.writeToNBT(nbt, "wire");
		return super.writeToNBT(nbt);
	}

	public static int CmdOffset = 16;

	@Override
	public void onPacketFromClient(PacketBuffer dis, EntityPlayer player) throws IOException {
		if (!PermissionUtil.handler.canEdit(world, pos, player.getGameProfile())) return;
		byte cmd = dis.readByte();
		if (cmd == 0 && inventory != null) {
			inventory.sideCfg = dis.readLong();
			this.markUpdate();
		} else if (cmd == 1 && tanks != null) {
			long dcfg = dis.readLong();
			if ((tanks.sideCfg & 0xf000000000000L & ~dcfg) != 0)
				for (int i = 0; i < tanks.tanks.length; i++)
					if (!tanks.canUnlock(i)) dcfg |= 1L << (i + 48);
			tanks.sideCfg = dcfg;
			this.markUpdate();
		} else if (cmd == 2 && tanks != null){
			int id = dis.readByte();
			if (id >= 0 && id < tanks.tanks.length) tanks.setFluid(id, null);
		} else if (cmd == 3 && energy != null) {
			energy.sideCfg = dis.readByte();
			this.markUpdate();
		} else if (cmd >= CmdOffset)
			this.customPlayerCommand((byte)(cmd - CmdOffset), dis, player);
	}

	protected void customPlayerCommand(byte cmd, PacketBuffer dis, EntityPlayer player) throws IOException {}

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing s) {
		if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
			return tanks != null && (s == null || (tanks.sideCfg >> (s.ordinal() * 8) & 0xff) != 0);
		else if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
			return inventory != null && (s == null || (inventory.sideCfg >> (s.ordinal() * 10) & 0x3ff) != 0);
		else if (cap == Capabilities.ELECTRIC_CAPABILITY)
			return energy != null && (s == null || energy.isConnected(s.ordinal()));
		else return super.hasCapability(cap, s);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing s) {
		if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) 
			return tanks == null ? null : (T)tanks.new Access(s);
		else if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) 
			return inventory == null ? null : (T)inventory.new Access(s);
		else if (cap == Capabilities.ELECTRIC_CAPABILITY)
			return energy == null || !(s == null || energy.isConnected(s.ordinal())) ? null : (T)energy;
		else return super.getCapability(cap, s);
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		if (this instanceof IOperatingArea && IOperatingArea.Handler.renderArea((IOperatingArea)this)) {
				int[] area = ((IOperatingArea)this).getOperatingArea();
				return new AxisAlignedBB(area[0], area[1], area[2], area[3], area[4], area[5]);
		} else return super.getRenderBoundingBox();
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		NBTTagCompound nbt = pkt.getNbtCompound();
		if (this instanceof IOperatingArea) {
			int[] a = nbt.getIntArray("area");
			int[] a1 = ((IOperatingArea)this).getOperatingArea();
			for (int i = 0; i < a.length && i < a1.length; i++) a1[i] = a[i];
		}
		if (energy != null) energy.sideCfg = nbt.getByte("Ecfg");
		if (tanks != null) tanks.sideCfg = nbt.getLong("Tcfg");
		if (inventory != null) inventory.sideCfg = nbt.getLong("Icfg");
		this.markUpdate();
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		if (this instanceof IOperatingArea) nbt.setIntArray("area", ((IOperatingArea)this).getOperatingArea());
		if (energy != null) nbt.setByte("Ecfg", energy.sideCfg);
		if (tanks != null) nbt.setLong("Tcfg", tanks.sideCfg);
		if (inventory != null) nbt.setLong("Icfg", inventory.sideCfg);
		return new SPacketUpdateTileEntity(pos, -1, nbt);
	}

	public boolean transferStack(ItemStack item, int s, TileContainer container) {return false;}

	public boolean slotClick(ItemStack item, Slot s, int b, ClickType m, TileContainer container) {return false;}

	public int insertAm(int g, int s, ItemStack item, ItemStack insert) {
		int m = Math.min(insert.getMaxStackSize() - (item == null ? 0 : item.getCount()), insert.getCount()); 
		return item == null || ItemHandlerHelper.canItemStacksStack(item, insert) ? m : 0;
	}

	public int extractAm(int g, int s, ItemStack item, int extract) {
		return item == null ? 0 : item.getCount() < extract ? item.getCount() : extract;
	}

}
