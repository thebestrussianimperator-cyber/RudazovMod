package com.poleesteel.rudazovmod.capabilities;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;

public class ActiveSpiritStorage implements IStorage<IActiveSpirit> {
    @Override
    public NBTBase writeNBT(Capability<IActiveSpirit> capability, IActiveSpirit instance, EnumFacing side) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setFloat("CurrentMana", instance.getMana());
        tag.setFloat("MaxMana", instance.getMaxMana());
        tag.setInteger("ChakraLevel", instance.getChakraLevel());
        return tag;
    }

    @Override
    public void readNBT(Capability<IActiveSpirit> capability, IActiveSpirit instance, EnumFacing side, NBTBase nbt) {
        if (nbt instanceof NBTTagCompound) {
            NBTTagCompound tag = (NBTTagCompound) nbt;
            instance.setMana(tag.getFloat("CurrentMana"));
            instance.setMaxMana(tag.getFloat("MaxMana"));
            instance.setChakraLevel(tag.getInteger("ChakraLevel"));
        }
    }
}