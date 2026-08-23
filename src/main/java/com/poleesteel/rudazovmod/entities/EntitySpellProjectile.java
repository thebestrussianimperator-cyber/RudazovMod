package com.poleesteel.rudazovmod.entities;

import com.poleesteel.rudazovmod.spell.api.SpellElement;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class EntitySpellProjectile extends EntityThrowable {
    // Синхронизируем ID стихии с клиентом, чтобы все видели правильный цвет магии
    private static final DataParameter<Integer> ELEMENT_ORDINAL = EntityDataManager.createKey(EntitySpellProjectile.class, DataSerializers.VARINT);
    private float power = 1.0F;

    // Обязательный конструктор для Forge (вызывается при загрузке мира)
    public EntitySpellProjectile(World worldIn) {
        super(worldIn);
    }

    // Наш конструктор: вызывается магом при касте
    public EntitySpellProjectile(World worldIn, EntityLivingBase throwerIn, SpellElement element, float power) {
        super(worldIn, throwerIn);
        this.power = power;
        this.setElement(element);
        this.shoot(throwerIn, throwerIn.rotationPitch, throwerIn.rotationYaw, 0.0F,
                element.projectileSpeed(), 0.0F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(ELEMENT_ORDINAL, 0); // По умолчанию FIRE (0)
    }

    public SpellElement getElement() {
        int ord = this.dataManager.get(ELEMENT_ORDINAL);
        SpellElement[] vals = SpellElement.values();
        return (ord >= 0 && ord < vals.length) ? vals[ord] : SpellElement.FIRE;
    }

    public void setElement(SpellElement element) {
        this.dataManager.set(ELEMENT_ORDINAL, element.ordinal());
    }

    @Override
    protected float getGravityVelocity() {
        return getElement().projectileGravity();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        // На стороне клиента спавним красивый цветной шлейф из частиц
        if (this.world.isRemote) {
            SpellElement element = getElement();
            int color = element.getColor();
            float r = ((color >> 16) & 0xFF) / 255.0F;
            float g = ((color >> 8) & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            EnumParticleTypes trail = element.trailParticle();

            for (int i = 0; i < 3; i++) {
                this.world.spawnParticle(EnumParticleTypes.REDSTONE,
                        this.posX + (this.rand.nextDouble() - 0.5D) * 0.3D,
                        this.posY + (this.rand.nextDouble() - 0.5D) * 0.3D,
                        this.posZ + (this.rand.nextDouble() - 0.5D) * 0.3D,
                        r, g, b);
                this.world.spawnParticle(trail,
                        this.posX, this.posY, this.posZ,
                        0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (!this.world.isRemote) {
            SpellElement element = getElement();
            if (result.typeOfHit == RayTraceResult.Type.ENTITY && result.entityHit instanceof EntityLivingBase) {
                element.onHit((EntityLivingBase) result.entityHit, this.power, this.getThrower());
            } else if (result.typeOfHit == RayTraceResult.Type.BLOCK) {
                element.onWorldHit(this.world, result.getBlockPos(), result.sideHit,
                        this.power, this.getThrower(), true);
            }
            this.setDead();
        }
    }
}