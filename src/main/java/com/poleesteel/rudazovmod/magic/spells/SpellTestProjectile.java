package com.poleesteel.rudazovmod.magic.spells;

import com.poleesteel.rudazovmod.magic.AbstractSpell;
import com.poleesteel.rudazovmod.magic.crafting.SpellElement;
import com.poleesteel.rudazovmod.magic.crafting.SpellForm;
import net.minecraft.entity.player.EntityPlayer;

public class SpellTestProjectile extends AbstractSpell {
    private final SpellElement element;

    public SpellTestProjectile(String id, String name, SpellElement element) {
        super(id, name);
        this.element = element;
    }

    @Override
    public float getManaCost(EntityPlayer player) {
        return 15.0F; // Фиксированная цена для проверки шкалы маны
    }

    @Override
    public void onCast(EntityPlayer player) {
        // Вызываем нашу Форму (Снаряд), передаём в неё игрока, Стихию и Мощность (2.0)
        SpellForm.PROJECTILE.cast(player, this.element, 2.0F);
    }
}