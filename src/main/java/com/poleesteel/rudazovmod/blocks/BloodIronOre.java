package com.poleesteel.rudazovmod.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;

public class BloodIronOre extends Block {
    public BloodIronOre(String name) {
        super(Material.ROCK); // Материал — камень
        setTranslationKey(name);
        setRegistryName(name);
        setHardness(3.0F); // Прочность руды
        setResistance(5.0F);
        setHarvestLevel("pickaxe", 2); // Нужна как минимум железная кирка
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }

    // Метод дропа переопределять не нужно: по умолчанию из класса Block
    // выпадает именно сам блок руды (в виде предмета ItemBlock), что нам и требуется.
}
