package com.poleesteel.rudazovmod.capabilities;

import com.poleesteel.rudazovmod.spell.api.Form;
import com.poleesteel.rudazovmod.spell.api.SpellDefinition;
import com.poleesteel.rudazovmod.spell.api.SpellElement;
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
        writeDevelopment(instance, tag);
        writeMastery(instance, tag);
        writeBook(instance, tag);
        return tag;
    }

    @Override
    public void readNBT(Capability<IActiveSpirit> capability, IActiveSpirit instance, EnumFacing side, NBTBase nbt) {
        if (nbt instanceof NBTTagCompound) {
            NBTTagCompound tag = (NBTTagCompound) nbt;
            instance.setMaxMana(tag.getFloat("MaxMana"));
            instance.setMana(tag.getFloat("CurrentMana"));
            readDevelopment(instance, tag);
            readMastery(instance, tag);
            readBook(instance, tag);
        }
    }

    /** Unlock / bind / гримуар / мастерство / развитие — без маны. Для PacketSyncSpirit. */
    public static NBTTagCompound writeBookNBT(IActiveSpirit instance) {
        NBTTagCompound tag = new NBTTagCompound();
        writeBook(instance, tag);
        writeMastery(instance, tag);
        writeDevelopment(instance, tag);
        return tag;
    }

    public static void replaceBook(IActiveSpirit instance, NBTTagCompound tag) {
        instance.clearUnlockedSpells();
        instance.clearBoundSpells();
        instance.clearGrimoire();
        if (tag != null) {
            readBook(instance, tag);
            readMastery(instance, tag);
            readDevelopment(instance, tag);
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

    private static void writeDevelopment(IActiveSpirit instance, NBTTagCompound tag) {
        tag.setFloat("SpiritDevelopment", instance.getSpiritDevelopment());
        tag.setInteger("ChakraLevel", instance.getChakraLevel());
    }

    private static void readDevelopment(IActiveSpirit instance, NBTTagCompound tag) {
        if (tag.hasKey("SpiritDevelopment", Constants.NBT.TAG_ANY_NUMERIC)) {
            instance.setSpiritDevelopment(tag.getFloat("SpiritDevelopment"));
        } else if (tag.hasKey("ChakraLevel", Constants.NBT.TAG_ANY_NUMERIC)) {
            instance.setChakraLevel(tag.getInteger("ChakraLevel"));
        }
    }

    private static void writeMastery(IActiveSpirit instance, NBTTagCompound tag) {
        NBTTagCompound forms = new NBTTagCompound();
        for (Form form : Form.values()) {
            forms.setFloat(form.name(), instance.getFormMastery(form));
        }
        tag.setTag("FormMastery", forms);

        NBTTagCompound elements = new NBTTagCompound();
        for (SpellElement element : SpellElement.values()) {
            elements.setFloat(element.name(), instance.getElementMastery(element));
        }
        tag.setTag("ElementMastery", elements);
    }

    private static void readMastery(IActiveSpirit instance, NBTTagCompound tag) {
        if (tag.hasKey("FormMastery", Constants.NBT.TAG_COMPOUND)) {
            NBTTagCompound forms = tag.getCompoundTag("FormMastery");
            for (Form form : Form.values()) {
                if (forms.hasKey(form.name(), Constants.NBT.TAG_ANY_NUMERIC)) {
                    instance.setFormMastery(form, forms.getFloat(form.name()));
                }
            }
        }
        if (tag.hasKey("ElementMastery", Constants.NBT.TAG_COMPOUND)) {
            NBTTagCompound elements = tag.getCompoundTag("ElementMastery");
            for (SpellElement element : SpellElement.values()) {
                if (elements.hasKey(element.name(), Constants.NBT.TAG_ANY_NUMERIC)) {
                    instance.setElementMastery(element, elements.getFloat(element.name()));
                }
            }
        }
    }
}
