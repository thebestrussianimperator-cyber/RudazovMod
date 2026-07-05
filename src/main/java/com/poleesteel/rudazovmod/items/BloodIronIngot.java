package com.poleesteel.rudazovmod.items;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public class BloodIronIngot extends Item {
    public BloodIronIngot(String name) {
        setTranslationKey(name);
        setRegistryName(name);
        setCreativeTab(CreativeTabs.MATERIALS); // Появится во вкладке "Материалы" в креативе
    }
}