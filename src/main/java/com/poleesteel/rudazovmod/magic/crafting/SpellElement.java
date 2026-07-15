package com.poleesteel.rudazovmod.magic.crafting;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;

public enum SpellElement {
    FIRE("Огонь", 0xFF4500, 1.2F) {
        @Override
        public void onHit(EntityLivingBase target, float power) {
            target.setFire((int) (3 * power)); // Поджигаем на N секунд
            target.attackEntityFrom(DamageSource.IN_FIRE, 4.0F * power);
        }
    },
    ICE("Лёд", 0x00FFFF, 1.0F) {
        @Override
        public void onHit(EntityLivingBase target, float power) {
            // Замедление (аналог заморозки в 1.12.2)
            target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, (int)(60 * power), 2));
            target.attackEntityFrom(DamageSource.MAGIC, 2.0F * power);
        }
    },
    EARTH("Земля", 0x8B4513, 1.5F) {
        @Override
        public void onHit(EntityLivingBase target, float power) {
            target.attackEntityFrom(DamageSource.GENERIC, 7.0F * power); // Сильный физический урон
            target.knockBack(target, power * 0.5F, 0, 0); // Отбрасывание
        }
    };

    private final String name;
    private final int color; // Цвет для частиц и полоски в интерфейсе
    private final float manaMultiplier;

    SpellElement(String name, int color, float manaMultiplier) {
        this.name = name;
        this.color = color;
        this.manaMultiplier = manaMultiplier;
    }

    public int getColor() { return color; }
    public float getManaMultiplier() { return manaMultiplier; }

    // Абстрактный метод, который каждая стихия реализует по-своему!
    public abstract void onHit(EntityLivingBase target, float power);
}