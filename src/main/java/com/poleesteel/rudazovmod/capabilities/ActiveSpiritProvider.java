package com.poleesteel.rudazovmod.capabilities;

import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

public class ActiveSpiritProvider implements ICapabilitySerializable<NBTBase> {
    @CapabilityInject(IActiveSpirit.class)
    public static final Capability<IActiveSpirit> ACTIVE_SPIRIT_CAP = null;

    private final IActiveSpirit instance = ACTIVE_SPIRIT_CAP.getDefaultInstance();

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == ACTIVE_SPIRIT_CAP;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        return capability == ACTIVE_SPIRIT_CAP ? ACTIVE_SPIRIT_CAP.<T>cast(this.instance) : null;
    }

    @Override
    public NBTBase serializeNBT() {
        return ACTIVE_SPIRIT_CAP.getStorage().writeNBT(ACTIVE_SPIRIT_CAP, this.instance, null);
    }

    @Override
    public void deserializeNBT(NBTBase nbt) {
        ACTIVE_SPIRIT_CAP.getStorage().readNBT(ACTIVE_SPIRIT_CAP, this.instance, null, nbt);
    }
}