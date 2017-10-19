package cd4017be.api.automation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.mojang.authlib.GameProfile;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;

/**
 *
 * @author CD4017BE
 */
public class AreaProtect implements ForgeChunkManager.LoadingCallback, IProtectionHandler{

	public static byte permissions = 1;
	public static byte chunkloadPerm = 1;
	public static byte maxChunksPBlock = 24;
	public static AreaProtect instance = new AreaProtect();
	private static boolean registered = false;
	private static Object mod;
	public static ArrayList<IProtectionHandler> handlers = new ArrayList<IProtectionHandler>();

	public static void register(Object mod) {
		if (!registered) {
			AreaProtect.mod = mod;
			if (permissions >= 0) {
				MinecraftForge.EVENT_BUS.register(instance);
				handlers.add(instance);
			}
			if (chunkloadPerm >= 0) ForgeChunkManager.setForcedChunkLoadingCallback(mod, instance);
		}
		registered = true;
	}

	@SubscribeEvent
	public void handlePlayerInteract(PlayerInteractEvent event) {
		if (permissions < 0 || event.getSide() == Side.CLIENT) return;
		ProtectLvl pl = this.getPlayerAccess(event.getEntityPlayer().getGameProfile(), event.getEntityPlayer().world, event.getPos().getX() >> 4, event.getPos().getZ() >> 4);
		if (pl == ProtectLvl.Free) return;
		if (event instanceof LeftClickBlock || event.getItemStack() != null || 
				pl == ProtectLvl.NoAcces || pl == ProtectLvl.NoInventory) event.setCanceled(true);
	}

	public static ProtectLvl playerAccess(GameProfile name, World world, int chunkX, int chunkZ) {
		ProtectLvl lvl = ProtectLvl.Free;
		for (IProtectionHandler handler : handlers) {
			ProtectLvl tmp = handler.getPlayerAccess(name, world, chunkX, chunkZ);
			if (tmp == ProtectLvl.NoInventory) return tmp;
			else if (tmp.ordinal() > lvl.ordinal()) lvl = tmp;
		}
		return lvl;
	}

	public static boolean operationAllowed(GameProfile player, World world, int cx, int cz) {
		for (IProtectionHandler handler : handlers)
			if (!handler.isOperationAllowed(player, world, cx, cz)) return false;
		return true;
	}

	public static boolean operationAllowed(GameProfile player, World world, int x0, int x1, int z0, int z1) {
		for (IProtectionHandler handler : handlers)
			if (!handler.isOperationAllowed(player, world, x0, x1, z0, z1)) return false;
		return true;
	}

	public static boolean interactingAllowed(GameProfile player, World world, int cx, int cz) {
		for (IProtectionHandler handler : handlers)
			if (!handler.isInteractingAllowed(player, world, cx, cz)) return false;
		return true;
	}

	public HashMap<Integer, ArrayList<IAreaConfig>> loadedSS = new HashMap<Integer, ArrayList<IAreaConfig>>();
	public HashMap<Integer, ArrayList<Ticket>> usedTickets = new HashMap<Integer, ArrayList<Ticket>>();

	/**
	 * @param name
	 * @param world
	 * @param chunkX
	 * @param chunkZ
	 * @return the restriction level for given username at given position.
	 */
	@Override
	public ProtectLvl getPlayerAccess(GameProfile player, World world, int chunkX, int chunkZ) {
		int ac = 0;
		ArrayList<IAreaConfig> list = loadedSS.get(world.provider.getDimension());
		if (list == null) return ProtectLvl.Free;
		for (IAreaConfig cfg : list) {
			ac = Math.max(ac, cfg.getProtectLvlFor(player.getName(), chunkX, chunkZ));
		}
		return ProtectLvl.getLvl(ac);
	}

	@Override
	public boolean isOperationAllowed(GameProfile player, World world, int cx, int cz) {
		ArrayList<IAreaConfig> list = loadedSS.get(world.provider.getDimension());
		if (list == null) return true;
		for (IAreaConfig cfg : list) {
			if (cfg.getProtectLvlFor(player.getName(), cx, cz) != 0) return false;
		}
		return true;
	}

	@Override
	public boolean isOperationAllowed(GameProfile player, World world, int x0, int x1, int z0, int z1) {
		x0 >>= 4; z0 >>= 4; x1 = (x1 + 15) >> 4; z1 = (z1 + 15) >> 4;
		for (int cx = x0; cx < x1; cx++)
			for (int cz = z0; cz < z1; cz++)
				if (!isOperationAllowed(player, world, cx, cz))
					return false;
		return true;
	}

	@Override
	public boolean isInteractingAllowed(GameProfile player, World world, int cx, int cz) {
		ArrayList<IAreaConfig> list = loadedSS.get(world.provider.getDimension());
		if (list == null) return true;
		for (IAreaConfig cfg : list) {
			if (cfg.getProtectLvlFor(player.getName(), cx, cz) > 1) return false;
		}
		return true;
	}

	public void loadSecuritySys(IAreaConfig config) {
		int[] pos = config.getPosition();
		ArrayList<IAreaConfig> list = loadedSS.get(pos[3]);
		if (list == null) {
			loadedSS.put(pos[3], list = new ArrayList<IAreaConfig>());
		}
		list.add(config);
		ArrayList<Ticket> tickets = usedTickets.get(pos[3]);
		if (tickets != null) {
			NBTTagCompound tag;
			for (Ticket t : tickets) {
				tag = t.getModData();
				if (tag.getInteger("bx") == pos[0] && tag.getInteger("by") == pos[1] && tag.getInteger("bz") == pos[2]) {
					config.setTicket(t);
					break;
				}
			}
		}
	}

	public void removeChunkLoader(IAreaConfig config) {
		int d = config.getPosition()[3];
		ArrayList<Ticket> list = usedTickets.get(d);
		Ticket t = config.getTicket();
		if (t != null) {
			config.setTicket(null);
			ForgeChunkManager.releaseTicket(t);
			if (list != null) {
				list.remove(t);
				if (list.isEmpty()) usedTickets.remove(d);
			}
		}
	}

	public void supplyTicket(IAreaConfig config, World world) {
		if (chunkloadPerm < 0) return;
		int[] p = config.getPosition();
		ArrayList<Ticket> list = usedTickets.get(p[3]);
		NBTTagCompound tag;
		if (list != null) for (Ticket t : list) {
			tag = t.getModData();
			if (tag.getInteger("bx") == p[0] && tag.getInteger("by") == p[1] && tag.getInteger("bz") == p[2]) {
				config.setTicket(t);
				return;
			}
		}
		Ticket t = ForgeChunkManager.requestTicket(mod, world, Type.NORMAL);
		if (t == null) return;
		tag = t.getModData();
		tag.setInteger("bx", p[0]);
		tag.setInteger("by", p[1]);
		tag.setInteger("bz", p[2]);
		t.setChunkListDepth(maxChunksPBlock);
		config.setTicket(t);
	}

	public void unloadSecuritySys(IAreaConfig config) {
		int d = config.getPosition()[3];
		ArrayList<IAreaConfig> list = loadedSS.get(d);
		if (list != null) {
			list.remove(config);
			if (list.isEmpty()) loadedSS.remove(d);
		}
	}

	@Override
	public void ticketsLoaded(List<Ticket> tickets, World world) {
		ArrayList<Ticket> list = usedTickets.get(world.provider.getDimension());
		if (list == null) {
			usedTickets.put(world.provider.getDimension(), list = new ArrayList<Ticket>());
		}
		list.addAll(tickets);
	}

}
