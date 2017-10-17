package cd4017be.lib.item;

import java.util.Arrays;
import java.util.HashMap;
import cd4017be.lib.BlockItemRegistry;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

/**
 * 
 * @author CD4017BE
 */
public class ItemMaterial extends BaseItem {

	public HashMap<Integer, String> variants = new HashMap<Integer, String>();

	public ItemMaterial(String id) {
		super(id);
		this.setHasSubtypes(true);
	}

	@Override
	protected void init() {}

	@Override
	public String getUnlocalizedName(ItemStack item) {
		String name = variants.get(item.getItemDamage());
		return this.getUnlocalizedName() + (name == null ? "" : ":" + name);
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> subItems) {
		if (!isInCreativeTab(tab)) return;
		Item item = this;
		subItems.add(new ItemStack(this, 1, 0));
		int[] ids = new int[variants.size()];
		int n = 0;
		for (int i : variants.keySet()) ids[n++] = i;
		Arrays.sort(ids);
		for (int i : ids) subItems.add(new ItemStack(item, 1, i));
	}

	public void addMaterial(int id, String name) {
		if (id <= 0 || id >= 32768 || variants.containsKey(id)) throw new IllegalArgumentException("Id already occupied or out of range!");
		variants.put(id, name);
		BlockItemRegistry.registerItemStack(new ItemStack(this, 1, id), this.getRegistryName().getResourcePath() + "." + name);
	}

}
