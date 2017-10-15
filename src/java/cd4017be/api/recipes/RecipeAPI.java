package cd4017be.api.recipes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import cd4017be.api.recipes.AutomationRecipes.*;
import cd4017be.lib.Lib;
import cd4017be.lib.script.Function.Iterator;
import cd4017be.lib.script.Function.ArrayIterator;
import cd4017be.lib.script.Function.ListIterator;
import cd4017be.lib.templates.NBTRecipe;
import cd4017be.lib.script.Function.FilteredIterator;
import cd4017be.lib.script.Parameters;
import cd4017be.lib.util.OreDictStack;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.IFuelHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

public class RecipeAPI {

	public static interface IRecipeHandler {
		public void addRecipe(Parameters param);
	}

	public static interface IRecipeList {
		public Iterator list(Parameters param);
	}

	public static final HashMap<String, IRecipeHandler> Handlers;
	public static final HashMap<String, IRecipeList> Lists;

	static {
		Handlers = new HashMap<String, IRecipeHandler>();
		Lists = new HashMap<String, IRecipeList>();
		
		Handlers.put("shaped", (p) -> {
			String[] pattern = p.getString(2).split("/");
			int n = p.param.length - 3;
			Object[] arr = new Object[n * 2 + pattern.length];
			for (int i = 0; i < pattern.length; i++) arr[i] = pattern[i];
			for (int i = 0; i < n; i++) {
				arr[pattern.length + i * 2] = Character.forDigit(i, 9);
				arr[pattern.length + i * 2 + 1] = p.param[i + 3];
			}
			GameRegistry.addRecipe(new ShapedOreRecipe(p.get(1, ItemStack.class), arr));
		});
		Handlers.put("shapedNBT", (p) -> {
			String[] pattern = p.getString(3).split("/");
			int n = p.param.length - 4;
			Object[] arr = new Object[n * 2 + pattern.length];
			for (int i = 0; i < pattern.length; i++) arr[i] = pattern[i];
			for (int i = 0; i < n; i++) {
				arr[pattern.length + i * 2] = Character.forDigit(i, 9);
				arr[pattern.length + i * 2 + 1] = p.param[i + 4];
			}
			GameRegistry.addRecipe(new NBTRecipe(p.get(2, ItemStack.class), p.getString(1), arr));
		});
		Handlers.put("ore", (p) -> {
			String name = p.getString(1);
			for (int i = 2; i < p.param.length; i++)
				OreDictionary.registerOre(name, p.get(i, ItemStack.class));
		});
		Lists.put("ore", (p) -> new FilteredIterator(new ArrayIterator(OreDictionary.getOreNames()), new RegexFilter(p.getString(1))));
		Lists.put("craftIng", (p) -> new CraftingRecipeIterator(getFilter(p.get(1)), false));
		Lists.put("craftRes", (p) -> new CraftingRecipeIterator(getFilter(p.get(1)), true));
		Handlers.put("shapeless", (p) -> GameRegistry.addRecipe(new ShapelessOreRecipe(p.get(1, ItemStack.class), Arrays.copyOfRange(p.param, 2, p.param.length))));
		Handlers.put("smelt", (p) -> GameRegistry.addSmelting(p.get(1, ItemStack.class), p.get(2, ItemStack.class), p.param.length > 3 ? (float)p.getNumber(3) : 0F));
		Handlers.put("fuel", new FuelHandler());
		Handlers.put("worldgen", new OreGenHandler());
		Handlers.put("item", (p) -> Lib.materials.addMaterial((int)p.getNumber(1), p.getString(2)));
		//TODO Handlers.put("fluidCont", (p) -> FluidContainerRegistry.registerFluidContainer(p.get(1, FluidStack.class), p.get(2, ItemStack.class), p.get(3, ItemStack.class)));
		if (Loader.isModLoaded("Automation")) {
			Handlers.put("advFurn", (p) -> {
				FluidStack Fin = null;
				FluidStack Fout = null;
				ArrayList<Object> Iin = new ArrayList<Object>();
				ArrayList<ItemStack> Iout = new ArrayList<ItemStack>();
				for (Object o : p.getArray(1)) {
					if (o instanceof FluidStack) Fin = (FluidStack)o;
					else Iin.add(o);
				}
				for (Object o : p.getArray(2)) {
					if (o instanceof FluidStack) Fout = (FluidStack)o;
					else if (o instanceof ItemStack) Iout.add((ItemStack)o);
					else throw new IllegalArgumentException("expected ItemStack or FluidStack as element of array @ 2");
				}
				AutomationRecipes.addRecipe(new LFRecipe(Fin, Iin.isEmpty() ? null : Iin.toArray(new Object[Iin.size()]), Fout, Iout.isEmpty() ? null : Iout.toArray(new ItemStack[Iout.size()]), (float)p.getNumber(3)));
			});
			Handlers.put("compAs", (p) -> AutomationRecipes.addCmpRecipe(p.get(1, ItemStack.class), Arrays.copyOfRange(p.param, 2, p.param.length)));
			Handlers.put("electr", (p) -> AutomationRecipes.addRecipe(new ElRecipe(p.get(1), p.get(2), p.get(3), (float)p.getNumber(4))));
			Handlers.put("cool", (p) -> AutomationRecipes.addRecipe(new CoolRecipe(p.get(1), p.get(2), p.get(3), p.get(4), (float)p.getNumber(5))));
			Handlers.put("trash", (p) -> AutomationRecipes.addRecipe(new GCRecipe(p.get(1, ItemStack.class), p.get(2, ItemStack.class), (int)p.getNumber(3))));
			Handlers.put("heatRad", (p) -> AutomationRecipes.addRadiatorRecipe(p.get(1, FluidStack.class), p.get(2, FluidStack.class)));
			Handlers.put("algae", (p) -> AutomationRecipes.bioList.add(new BioEntry(p.get(1), (int)p.getNumber(2), (int)p.getNumber(3))));
		}
	}

	private static class FuelHandler implements IRecipeHandler, IFuelHandler {
		HashMap<Integer, Integer> fuelList;
		public FuelHandler() {
			fuelList = new HashMap<Integer, Integer>();
			GameRegistry.registerFuelHandler(this);
		}
		int key(ItemStack item) {
			return Item.getIdFromItem(item.getItem()) & 0xffff | (item.getItemDamage() & 0xffff) << 16;
		}
		@Override
		public void addRecipe(Parameters p) {
			fuelList.put(key(p.get(1, ItemStack.class)), (int)p.getNumber(2));
		}
		@Override
		public int getBurnTime(ItemStack fuel) {
			Integer val = fuelList.get(key(fuel));
			return val == null ? 0 : val;
		}
	}

	public static Predicate<Object> getFilter(Object o) {
		if (o instanceof String) return new RegexFilter((String)o);
		else if (o instanceof ItemStack) {
			final ItemStack item = (ItemStack)o;
			return (p) -> p instanceof ItemStack && item.isItemEqual((ItemStack)p);
		} else if (o instanceof FluidStack) {
			final FluidStack fluid = (FluidStack)o;
			return (p) -> p instanceof FluidStack && fluid.isFluidEqual((FluidStack)p);
		} else if (o instanceof OreDictStack) {
			final OreDictStack ore = (OreDictStack)o;
			return (p) ->
				p instanceof OreDictStack ? ore.ID == ((OreDictStack)p).ID :
				p instanceof String ? ore.id.equals((String)p) :
				p instanceof ItemStack && ore.isEqual((ItemStack)p);
		} else if (o == null) return (p) -> p == null;
		else return (p) -> o.equals(p);
	}

	public static class RegexFilter implements Predicate<Object> {
		public RegexFilter(String expr) {
			pattern = Pattern.compile(expr);
		}
		private final Pattern pattern;
		@Override
		public boolean test(Object o) {
			return o != null && pattern.matcher(o.toString()).matches();
		}
	}

	private static class CraftingRecipeIterator implements Iterator {
		private final List<IRecipe> list;
		private final Predicate<Object> key;
		private final boolean in;
		private int idx;
		private Object[] curElement;
		private IRecipe curRecipe;

		public CraftingRecipeIterator(Predicate<Object> key, boolean res) {
			this.key = key;
			this.list = CraftingManager.getInstance().getRecipeList();
			this.in = !res;
			this.idx = -1;
		}

		@Override
		public Object get() {
			return curElement;
		}

		@Override
		public void set(Object o) {
			if (list.get(idx) != curRecipe) throw new ConcurrentModificationException();
			if (o == null) list.remove(idx--);
			else if (o == curElement && curElement[0] != curRecipe.getRecipeOutput()) {
				if (!(curElement[0] instanceof ItemStack)) throw new IllegalArgumentException("ItemStack expected");
				ItemStack item = (ItemStack)curElement[0];
				ItemStack res = curRecipe.getRecipeOutput();
				if (res != null) {
					//TODO res.setItem(item.getItem());
					res.setItemDamage(item.getItemDamage());
					res.setTagCompound(item.getTagCompound());
					res.setCount(item.getCount());
				}
			}
		}

		@Override
		public boolean next() {
			int l = list.size();
			while (++idx < l) {
				curRecipe = list.get(idx);
				ItemStack result = curRecipe.getRecipeOutput();
				if (!(in || key.test(result))) continue;
				Iterator ingred;
				if (curRecipe instanceof ShapedOreRecipe) ingred = new ShapedIngredients((ShapedOreRecipe)curRecipe, in ? key : null);
				else if (curRecipe instanceof ShapelessOreRecipe) ingred = new ShapelessIngredients((ShapelessOreRecipe)curRecipe, in ? key : null);
				else if (curRecipe instanceof ShapedRecipes) {
					ingred = new ArrayIterator(((ShapedRecipes)curRecipe).recipeItems);
					if (in) ingred = new FilteredIterator(ingred, key);
				} else if (curRecipe instanceof ShapelessRecipes) {
					ingred = new ListIterator<ItemStack>(((ShapelessRecipes)curRecipe).recipeItems);
					if (in) ingred = new FilteredIterator(ingred, key);
				}
				else continue;
				if (in) {
					if (ingred.next()) ingred.reset();
					else continue;
				}
				curElement = new Object[]{result, ingred};
				return true;
			}
			return false;
		}

		@Override
		public void reset() {
			idx = -1;
		}

	}

	private static class ShapelessIngredients extends ListIterator<Object> {
		private final Predicate<Object> key;
		private Object curElement;

		public ShapelessIngredients(ShapelessOreRecipe rcp, Predicate<Object> key) {
			super(rcp.getInput());
			this.key = key;
		}

		@Override
		public Object get() {
			return curElement;
		}

		@Override
		public void set(Object o) {
			if (o == curElement) return;
			if (o instanceof ItemStack) arr.set(idx, o);
			else if (o instanceof OreDictStack) arr.set(idx, OreDictionary.getOres(((OreDictStack)o).id));
			else if (o == null) arr.remove(idx--);
			else throw new IllegalArgumentException("exp. ItemStack or OreDictStack");
		}

		@Override
		public boolean next() {
			while (++idx < arr.size()) {
				curElement = arr.get(idx);
				if (curElement instanceof List) {
					List<?> list = (List<?>)curElement;
					if (list.isEmpty()) continue;
					if (key == null) {
						curElement = list.get(0);
						return true;
					}
					for (Object o : list)
						if (key.test(o)) {
							curElement = o;
							return true;
						}
				} else if (key == null || key.test(curElement)) return true;
			}
			return false;
		}
	}

	private static class ShapedIngredients extends ArrayIterator {
		private final Predicate<Object> key;
		private Object curElement;

		public ShapedIngredients(ShapedOreRecipe rcp, Predicate<Object> key) {
			super(rcp.getInput());
			this.key = key;
		}

		@Override
		public Object get() {
			return curElement;
		}

		@Override
		public void set(Object o) {
			if (o == curElement) return;
			if (o == null || o instanceof ItemStack) arr[idx] = o;
			else if (o instanceof OreDictStack) arr[idx] = OreDictionary.getOres(((OreDictStack)o).id);
			else throw new IllegalArgumentException("exp. ItemStack or OreDictStack");
		}

		@Override
		public boolean next() {
			while (++idx < arr.length) {
				curElement = arr[idx];
				if (curElement instanceof List) {
					List<?> list = (List<?>)curElement;
					if (list.isEmpty()) continue;
					if (key == null) {
						curElement = list.get(0);
						return true;
					}
					for (Object o : list)
						if (key.test(o)) {
							curElement = o;
							return true;
						}
				} else if (key == null || key.test(curElement)) return true;
			}
			return false;
		}
	}

	public static void createOreDictEntries(Class<?> c, String name) {
		if (Block.class.isAssignableFrom(c)) {
			Item item;
			for (Block block : Block.REGISTRY)
				if (c.isInstance(block) && (item = Item.getItemFromBlock(block)) != null)
					OreDictionary.registerOre(name, item);
		} else if (Item.class.isAssignableFrom(c)) {
			for (Item item : Item.REGISTRY)
				if (c.isInstance(item))
					OreDictionary.registerOre(name, item);
		} 
	}

}
