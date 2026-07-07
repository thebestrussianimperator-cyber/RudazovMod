package com.poleesteel.rudazovmod.entities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.world.World;

public class EntityBloodChain extends Entity {

    private EntityLivingBase targetEntity;
    private static final DataParameter<Integer> TARGET_ID = EntityDataManager.createKey(EntityBloodChain.class, DataSerializers.VARINT);

    public EntityBloodChain(World worldIn) {
        super(worldIn);
        this.setSize(0.5F, 0.5F);
        this.noClip = true; // Отключаем физическую коллизию цепи, чтобы не толкала мобов
    }

    public EntityBloodChain(World worldIn, EntityLivingBase target) {
        this(worldIn); // Вызываем базовый конструктор
        this.targetEntity = target;

        if (target != null) {
            this.dataManager.set(TARGET_ID, target.getEntityId());

            // ВОТ ОНО! Спавним цепь сразу на мобе, а не на (0,0,0)
            this.setLocationAndAngles(target.posX, target.posY + (target.height / 2.0F), target.posZ, target.rotationYaw, target.rotationPitch);
        }
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(TARGET_ID, -1);
    }

    public Entity getTarget() {
        int id = this.dataManager.get(TARGET_ID);
        return id != -1 ? this.world.getEntityByID(id) : null;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        Entity currentTarget = this.world.isRemote ? getTarget() : this.targetEntity;

        if (currentTarget instanceof EntityLivingBase) {
            EntityLivingBase livingTarget = (EntityLivingBase) currentTarget;

            if (livingTarget.isEntityAlive()) {
                // Визуально держим цепь на мобе
                this.setPosition(livingTarget.posX, livingTarget.posY + (livingTarget.height / 2.0F), livingTarget.posZ);

                // Безопасный паралич (только на сервере)
                if (!this.world.isRemote) {
                    // Глушим инерцию
                    livingTarget.motionX = 0.0D;
                    livingTarget.motionZ = 0.0D;
                    if (livingTarget.motionY > 0.0D) {
                        livingTarget.motionY = -0.1D;
                    }

                    // Накладываем замедление без сетевого спама
                    net.minecraft.potion.PotionEffect slowness = livingTarget.getActivePotionEffect(net.minecraft.init.MobEffects.SLOWNESS);
                    if (slowness == null || slowness.getDuration() <= 20) {
                        livingTarget.addPotionEffect(new net.minecraft.potion.PotionEffect(net.minecraft.init.MobEffects.SLOWNESS, 60, 10, false, false));
                    }
                }
            } else if (!this.world.isRemote) {
                this.setDead();
            }
        } else if (!this.world.isRemote) {
            this.setDead();
        }
    }

    @Override protected void readEntityFromNBT(NBTTagCompound compound) {}
    @Override protected void writeEntityToNBT(NBTTagCompound compound) {}
}