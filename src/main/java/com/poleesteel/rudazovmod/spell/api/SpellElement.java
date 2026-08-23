package com.poleesteel.rudazovmod.spell.api;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;

public enum SpellElement {
    FIRE("Огонь", 0xFF4500, 1.2F) {
        @Override
        public void onHit(EntityLivingBase target, float power, EntityLivingBase source) {
            target.setFire((int) (3 * power));
            target.attackEntityFrom(DamageSource.IN_FIRE, 4.0F * power);
        }
    },
    ICE("Лёд", 0x00FFFF, 1.0F) {
        @Override
        public void onHit(EntityLivingBase target, float power, EntityLivingBase source) {
            target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, (int) (60 * power), 2));
            target.attackEntityFrom(DamageSource.MAGIC, 2.0F * power);
        }
    },
    EARTH("Земля", 0x8B4513, 1.5F) {
        @Override
        public void onHit(EntityLivingBase target, float power, EntityLivingBase source) {
            target.attackEntityFrom(DamageSource.GENERIC, 7.0F * power);
            if (source != null) {
                target.knockBack(source, power * 0.5F,
                        source.posX - target.posX,
                        source.posZ - target.posZ);
            }
        }
    };

    private final String displayName;
    private final int color;
    private final float manaMultiplier;

    SpellElement(String displayName, int color, float manaMultiplier) {
        this.displayName = displayName;
        this.color = color;
        this.manaMultiplier = manaMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }

    public float getManaMultiplier() {
        return manaMultiplier;
    }

    public abstract void onHit(EntityLivingBase target, float power, EntityLivingBase source);
}
