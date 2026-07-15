package com.poleesteel.rudazovmod.magic.crafting;

import net.minecraft.entity.player.EntityPlayer;
import com.poleesteel.rudazovmod.entities.EntitySpellProjectile;

public enum SpellForm {
    PROJECTILE("Снаряд", 10.0F) {
        @Override
        public void cast(EntityPlayer caster, SpellElement element, float power) {
            // Вместо 100 разных энтити мы спавним ОДИН универсальный снаряд,
            // передавая ему стихию и мощность!
            if (!caster.world.isRemote) {
                EntitySpellProjectile projectile = new EntitySpellProjectile(caster.world, caster, element, power);
                caster.world.spawnEntity(projectile);
            }
        }
    },
    AOE_WAVE("Волна", 25.0F) {
        @Override
        public void cast(EntityPlayer caster, SpellElement element, float power) {
            // Поражаем всех мобов вокруг игрока
            double radius = 3.0D * power;
            caster.world.getEntitiesWithinAABB(net.minecraft.entity.EntityLivingBase.class,
                            caster.getEntityBoundingBox().grow(radius, 1.0D, radius))
                    .stream()
                    .filter(e -> e != caster)
                    .forEach(e -> element.onHit(e, power));
        }
    };

    private final String name;
    private final float baseManaCost;

    SpellForm(String name, float baseManaCost) {
        this.name = name;
        this.baseManaCost = baseManaCost;
    }

    public float getBaseManaCost() { return baseManaCost; }
    public abstract void cast(EntityPlayer caster, SpellElement element, float power);
}