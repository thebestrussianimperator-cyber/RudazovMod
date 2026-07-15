package com.poleesteel.rudazovmod.magic.crafting;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import com.poleesteel.rudazovmod.capabilities.ActiveSpiritProvider;
import com.poleesteel.rudazovmod.capabilities.IActiveSpirit;

public class CustomSpell {
    private String customName;       // Например: "Копьё Ледяного Гнева"
    private SpellElement element;    // Лёд
    private SpellForm form;          // Снаряд
    private float powerLevel;        // Мощность (например, 2.0)

    public CustomSpell(String name, SpellElement element, SpellForm form, float powerLevel) {
        this.customName = name;
        this.element = element;
        this.form = form;
        this.powerLevel = powerLevel;
    }

    // Автоматический расчет стоимости по лору Рудазова (Форма * Стихия * Мощность)
    public float calculateManaCost() {
        return form.getBaseManaCost() * element.getManaMultiplier() * (powerLevel * 1.5F);
    }

    public boolean cast(EntityPlayer caster) {
        IActiveSpirit spirit = caster.getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
        float cost = calculateManaCost();

        if (spirit != null && spirit.consumeMana(cost)) {
            // Делегируем выполнение Форме с учетом Стихии!
            form.cast(caster, element, powerLevel);
            return true;
        }
        return false;
    }

    // Сохранение созданного спелла в NBT предмет или в духовную память игрока
    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Name", customName);
        tag.setString("Element", element.name());
        tag.setString("Form", form.name());
        tag.setFloat("Power", powerLevel);
        return tag;
    }

    public static CustomSpell readFromNBT(NBTTagCompound tag) {
        return new CustomSpell(
                tag.getString("Name"),
                SpellElement.valueOf(tag.getString("Element")),
                SpellForm.valueOf(tag.getString("Form")),
                tag.getFloat("Power")
        );
    }
}