package com.poleesteel.rudazovmod.capabilities;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.minecraftforge.common.util.Constants;

import java.util.Map;

public class ActiveSpiritStorage implements IStorage<IActiveSpirit> {

    @Override
    public NBTBase writeNBT(Capability<IActiveSpirit> capability, IActiveSpirit instance, EnumFacing side) {
        NBTTagCompound tag = new NBTTagCompound();

        // 1. Базовые параметры маны и чакр
        tag.setFloat("CurrentMana", instance.getMana());
        tag.setFloat("MaxMana", instance.getMaxMana());
        tag.setInteger("ChakraLevel", instance.getChakraLevel());

        // 2. Сохраняем изученные заклинания (NBTTagList из строк)
        NBTTagList unlockedList = new NBTTagList();
        for (String spellId : instance.getUnlockedSpells()) {
            unlockedList.appendTag(new NBTTagString(spellId));
        }
        tag.setTag("UnlockedSpells", unlockedList);

        // 3. Сохраняем бинды слотов (NBTTagList из NBTTagCompound: Слот + ID спелла)
        NBTTagList boundList = new NBTTagList();
        for (Map.Entry<Integer, String> entry : instance.getBoundSpells().entrySet()) {
            NBTTagCompound slotTag = new NBTTagCompound();
            slotTag.setInteger("Slot", entry.getKey());
            slotTag.setString("SpellId", entry.getValue());
            boundList.appendTag(slotTag);
        }
        tag.setTag("BoundSpells", boundList);

        return tag;
    }

    @Override
    public void readNBT(Capability<IActiveSpirit> capability, IActiveSpirit instance, EnumFacing side, NBTBase nbt) {
        if (nbt instanceof NBTTagCompound) {
            NBTTagCompound tag = (NBTTagCompound) nbt;

            // 1. Читаем базовые параметры
            instance.setMana(tag.getFloat("CurrentMana"));
            instance.setMaxMana(tag.getFloat("MaxMana"));
            instance.setChakraLevel(tag.getInteger("ChakraLevel"));

            // 2. Читаем изученные заклинания (используем константу TAG_STRING = 8)
            if (tag.hasKey("UnlockedSpells", Constants.NBT.TAG_LIST)) {
                NBTTagList unlockedList = tag.getTagList("UnlockedSpells", Constants.NBT.TAG_STRING);
                for (int i = 0; i < unlockedList.tagCount(); i++) {
                    instance.unlockSpell(unlockedList.getStringTagAt(i));
                }
            }

            // 3. Читаем бинды слотов (используем константу TAG_COMPOUND = 10)
            if (tag.hasKey("BoundSpells", Constants.NBT.TAG_LIST)) {
                NBTTagList boundList = tag.getTagList("BoundSpells", Constants.NBT.TAG_COMPOUND);
                for (int i = 0; i < boundList.tagCount(); i++) {
                    NBTTagCompound slotTag = boundList.getCompoundTagAt(i);
                    int slot = slotTag.getInteger("Slot");
                    String spellId = slotTag.getString("SpellId");
                    instance.bindSpell(slot, spellId);
                }
            }
        }
    }
}