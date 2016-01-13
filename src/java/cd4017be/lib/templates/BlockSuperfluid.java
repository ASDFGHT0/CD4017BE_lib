/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.lib.templates;

import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.ModFluid;

import java.util.HashMap;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialTransparent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemBlock;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

/**
 *
 * @author CD4017BE
 */
public class BlockSuperfluid extends BlockFluidClassic
{
    public static final Material materialGas = new MaterialGas();
    public static class MaterialGas extends MaterialTransparent {
    	public MaterialGas() {
    		super(MapColor.airColor);
    		this.setNoPushMobility();
    	}
    }
    
    private Random random = new Random();
    
    public BlockSuperfluid(String id, ModFluid fluid)
    {
        super(fluid, fluid.isGaseous() ? fluid.getTemperature() > 350 ? Material.fire : materialGas : Material.water);
        this.setBlockName(id);
        BlockItemRegistry.registerBlock(this, id, ItemBlock.class);
        BlockItemRegistry.registerName(this, fluid.getLocalizedName());
        this.setBlockTextureName(BlockItemRegistry.currentMod.concat(":liquids/").concat(fluid.getTexName()));
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random rand) 
    {
        int m = world.getBlockMetadata(x, y, z);
        if (m == 0) {
            //Move source vertically if possible
            if (tryReplace(world, x, y + densityDir, z)) {
                this.moveSourceTo(world, x, y, z, x, y + densityDir, z);
                return;
            }
            //Move source horizontally if possible
            if (!isFlowingVertically(world, x, y, z)) {
                boolean[] b = this.getOptimalFlowDirections(world, x, y, z);
                int x1 = x, z1 = z;
                if (b[0]) x1--;
                if (b[1]) x1++;
                if (b[2]) z1--;
                if (b[3]) z1++;
                if (x1 != x || z1 != z && this.tryReplace(world, x1, y, z1)) {
                    this.moveSourceTo(world, x, y, z, x1, y, z1);
                }
            }
        } else if (m <= 4 && this.canDisplace(world, x, y + densityDir, z)) {
            int[] p = this.findSource(world, x, y, z);
            if (p != null) world.scheduleBlockUpdate(p[0], p[1], p[2], this, tickRate);
        }
        super.updateTick(world, x, y, z, rand);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block blockId) 
    {
        if (!this.checkFluidReaction(world, x, y, z))
        super.onNeighborBlockChange(world, x, y, z, blockId);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) 
    {
        if (!this.checkFluidReaction(world, x, y, z))
        super.onBlockAdded(world, x, y, z);
    }
    
    private boolean checkFluidReaction(World world, int x, int y, int z)
    {
        if (this.densityDir > 0) return false;
        Fluid fluid = reactConversions.get(this.getFluid());
        if (fluid == null) return false;
        ForgeDirection dir;
        int x1, y1, z1;
        boolean react = false;
        for (int s = 0; s < 6; s++) {
            dir = ForgeDirection.getOrientation(s);
            x1 = x + dir.offsetX; y1 = y + dir.offsetY; z1 = z + dir.offsetZ;
            Block id = world.getBlock(x1, y1, z1);
            if ((id == Block.getBlockFromName("waterMoving") || id == Block.getBlockFromName("waterStill")) && fluid.getTemperature() < FluidRegistry.WATER.getTemperature()) {
                world.setBlock(x1, y1, z1, world.getBlockMetadata(x1, y1, z1) == 0 ? Block.getBlockFromName("ice") : Block.getBlockFromName("snow"), 0, 2);
                react = true;
            } else if ((id == Block.getBlockFromName("lavaMoving") || id == Block.getBlockFromName("lavaStill")) && fluid.getTemperature() < FluidRegistry.LAVA.getTemperature()) {
                world.setBlock(x1, y1, z1, world.getBlockMetadata(x1, y1, z1) == 0 ? Block.getBlockFromName("obsidian") : Block.getBlockFromName("cobblestone"), 0, 2);
                react = true;
            } else {
                int t = BlockFluidBase.getTemperature(world, x1, y1, z1);
                if (t != Integer.MAX_VALUE && t > fluid.getTemperature()) react = true;
            }
        }
        if (react) {
            if (random.nextFloat() < 0.05F) {
                int[] p = this.findSource(world, x, y, z);
                if (p != null && !(p[0] == x && p[1] == y && p[2] == z)) world.setBlockMetadataWithNotify(p[0], p[1], p[2], 1, 3);
            }
            world.setBlock(x, y, z, fluid.getBlock(), 0, 3);
            world.notifyBlocksOfNeighborChange(x, y, z, fluid.getBlock());
            return true;
        } else return false;
    }
    
    public static HashMap<Fluid, Fluid> reactConversions = new HashMap();
    public static HashMap<Fluid, PotionEffect[]> effects = new HashMap();
    
    public boolean tryReplace(World world, int x, int y, int z)
    {
        if (world.isAirBlock(x, y, z)) return true;

        Block bId = world.getBlock(x, y, z);

        if (bId == this)
        {
            return world.getBlockMetadata(x, y, z) != 0;
        }

        Material material = bId.getMaterial();
        if (material.blocksMovement() || material == Material.portal)
        {
            return false;
        }

        int density = getDensity(world, x, y, z);
        if (density == Integer.MAX_VALUE) 
        {
            bId.dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            return true;
        }
        
        return this.density > density;
    }
    
    public void moveSourceTo(World world, int x, int y, int z, int nx, int ny, int nz)
    {
        world.setBlockMetadataWithNotify(x, y, z, 1, 3);
        world.setBlock(nx, ny, nz, this, 0, 3);
        world.scheduleBlockUpdate(x, y, z, this, tickRate);
        world.notifyBlocksOfNeighborChange(x, y, z, this);
    }
    
    public int[] findSource(World world, int x, int y, int z)
    {
        int m = world.getBlockMetadata(x, y, z);
        int m1;
        while (m > 0) {
            if (world.getBlock(x - 1, y, z) == this && (m1 = world.getBlockMetadata(x - 1, y, z)) < m) x--;
            else if (world.getBlock(x + 1, y, z) == this && (m1 = world.getBlockMetadata(x + 1, y, z)) < m) x++;
            else if (world.getBlock(x, y, z - 1) == this && (m1 = world.getBlockMetadata(x, y, z - 1)) < m) z--;
            else if (world.getBlock(x, y, z + 1) == this && (m1 = world.getBlockMetadata(x, y, z + 1)) < m) z++;
            else return null;
            m = m1;
        }
        return new int[] {x, y, z};
    }

    @Override
    public void velocityToAddToEntity(World world, int x, int y, int z, Entity entity, Vec3 vec) {
        super.velocityToAddToEntity(world, x, y, z, entity, vec);
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) 
    {
        PotionEffect[] eff = effects.get(this.getFluid());
        if (eff != null && entity instanceof EntityLivingBase)
        for (PotionEffect e : eff) {
            ((EntityLivingBase)entity).addPotionEffect(new PotionEffect(e.getPotionID(), e.getDuration(), e.getAmplifier()));
        }
        int t = BlockFluidBase.getTemperature(world, x, y, z);
        if (t > 350) {
            entity.setFire((t - 300) / 50);
        }
    }
    
}