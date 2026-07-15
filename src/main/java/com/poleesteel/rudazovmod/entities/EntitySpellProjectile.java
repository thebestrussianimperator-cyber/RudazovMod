package com.poleesteel.rudazovmod.entities;

import com.poleesteel.rudazovmod.magic.crafting.SpellElement;
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

        // Задаем скорость и точность полета (2.5F — летит быстрее стрелы!)
        this.shoot(throwerIn, throwerIn.rotationPitch, throwerIn.rotationYaw, 0.0F, 2.5F, 0.5F);
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
        return 0.01F; // Почти нулевая гравитация — магия летит ровно туда, куда смотрит прицел!
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        // На стороне клиента спавним красивый цветной шлейф из частиц
        if (this.world.isRemote) {
            int color = getElement().getColor();
            // Разбираем HEX-цвет на RGB (от 0.0 до 1.0)
            float r = ((color >> 16) & 0xFF) / 255.0F;
            float g = ((color >> 8) & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;

            for (int i = 0; i < 3; i++) {
                // В 1.12.2 частицы REDSTONE меняют свой цвет, если передать RGB в параметры скорости!
                this.world.spawnParticle(EnumParticleTypes.REDSTONE,
                        this.posX + (this.rand.nextDouble() - 0.5D) * 0.3D,
                        this.posY + (this.rand.nextDouble() - 0.5D) * 0.3D,
                        this.posZ + (this.rand.nextDouble() - 0.5D) * 0.3D,
                        r, g, b);
            }
        }
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (!this.world.isRemote) {
            // Если попали по живому существу — вызываем логику стихии (урон, поджог, заморозка)
            if (result.typeOfHit == RayTraceResult.Type.ENTITY && result.entityHit instanceof EntityLivingBase) {
                getElement().onHit((EntityLivingBase) result.entityHit, this.power);
            }
            // Исчезаем при ударе о моба или стену
            this.setDead();
        }
    }
}