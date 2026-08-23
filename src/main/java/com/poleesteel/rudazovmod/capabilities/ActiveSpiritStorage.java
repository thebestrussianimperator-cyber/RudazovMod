package com.poleesteel.rudazovmod.capabilities;

import com.poleesteel.rudazovmod.spell.api.SpellDefinition;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.minecraftforge.common.util.Constants;

import java.util.Map;
import java.util.Optional;

public class ActiveSpiritStorage implements IStorage<IActiveSpirit> {

    @Override
    public NBTBase writeNBT(Capability<IActiveSpirit> capability, IActiveSpirit instance, EnumFacing side) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setFloat("CurrentMana", instance.getMana());
        tag.setFloat("MaxMana", instance.getMaxMana());
        tag.setInteger("ChakraLevel", instance.getChakraLevel());
        writeBook(instance, tag);
        return tag;
    }

    @Override
    public void readNBT(Capability<IActiveSpirit> capability, IActiveSpirit instance, EnumFacing side, NBTBase nbt) {
        if (nbt instanceof NBTTagCompound) {
            NBTTagCompound tag = (NBTTagCompound) nbt;
            instance.setMana(tag.getFloat("CurrentMana"));
            instance.setMaxMana(tag.getFloat("MaxMana"));
            instance.setChakraLevel(tag.getInteger("ChakraLevel"));
            readBook(instance, tag);
        }
    }

    /** Unlock / bind / гримуар — без маны. Для PacketSyncSpirit. */
    public static NBTTagCompound writeBookNBT(IActiveSpirit instance) {
        NBTTagCompound tag = new NBTTagCompound();
        writeBook(instance, tag);
        return tag;
    }

    public static void replaceBook(IActiveSpirit instance, NBTTagCompound tag) {
        instance.clearUnlockedSpells();
        instance.clearBoundSpells();
        instance.clearGrimoire();
        if (tag != null) {
            readBook(instance, tag);
        }
    }

    private static void writeBook(IActiveSpirit instance, NBTTagCompound tag) {
        NBTTagList unlockedList = new NBTTagList();
        for (String spellId : instance.getUnlockedSpells()) {
            unlockedList.appendTag(new NBTTagString(spellId));
        }
        tag.setTag("UnlockedSpells", unlockedList);

        NBTTagList boundList = new NBTTagList();
        for (Map.Entry<Integer, String> entry : instance.getBoundSpells().entrySet()) {
            NBTTagCompound slotTag = new NBTTagCompound();
            slotTag.setInteger("Slot", entry.getKey());
            slotTag.setString("SpellId", entry.getValue());
            boundList.appendTag(slotTag);
        }
        tag.setTag("BoundSpells", boundList);

        NBTTagList grimoireList = new NBTTagList();
        for (SpellDefinition spell : instance.getGrimoire()) {
            grimoireList.appendTag(spell.writeNBT());
        }
        tag.setTag("Grimoire", grimoireList);
    }

    private static void readBook(IActiveSpirit instance, NBTTagCompound tag) {
        if (tag.hasKey("UnlockedSpells", Constants.NBT.TAG_LIST)) {
            NBTTagList unlockedList = tag.getTagList("UnlockedSpells", Constants.NBT.TAG_STRING);
            for (int i = 0; i < unlockedList.tagCount(); i++) {
                instance.unlockSpell(unlockedList.getStringTagAt(i));
            }
        }

        if (tag.hasKey("BoundSpells", Constants.NBT.TAG_LIST)) {
            NBTTagList boundList = tag.getTagList("BoundSpells", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < boundList.tagCount(); i++) {
                NBTTagCompound slotTag = boundList.getCompoundTagAt(i);
                instance.bindSpell(slotTag.getInteger("Slot"), slotTag.getString("SpellId"));
            }
        }

        if (tag.hasKey("Grimoire", Constants.NBT.TAG_LIST)) {
            NBTTagList grimoireList = tag.getTagList("Grimoire", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < grimoireList.tagCount(); i++) {
                Optional<SpellDefinition> spell = SpellDefinition.readNBT(grimoireList.getCompoundTagAt(i));
                if (spell.isPresent()) {
                    instance.putSpell(spell.get());
                }
            }
        }
    }
}
