package com.poleesteel.rudazovmod.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;

public class BloodIronBlock extends Block {
    public BloodIronBlock(String name) {
        super(Material.IRON); // Материал железо (дает правильные звуки шагов и ломания)
        setTranslationKey(name);
        setRegistryName(name);
        setHardness(5.0F); // Прочность блока
        setResistance(10.0F); // Сопротивление взрыву
        setHarvestLevel("pickaxe", 2); // 2 = Железная кирка и выше
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }
}