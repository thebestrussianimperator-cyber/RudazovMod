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
    private EntityLivingBase ownerEntity; // Хозяин цепи (игрок)

    private static final DataParameter<Integer> TARGET_ID = EntityDataManager.createKey(EntityBloodChain.class, DataSerializers.VARINT);
    // Добавляем ID хозяина для видеокарты (Клиента)
    private static final DataParameter<Integer> OWNER_ID = EntityDataManager.createKey(EntityBloodChain.class, DataSerializers.VARINT);

    public EntityBloodChain(World worldIn) {
        super(worldIn);
        this.setSize(0.5F, 0.5F);
        this.noClip = true;
    }

    // Новый конструктор, куда мы передаем и жертву, и игрока
    public EntityBloodChain(World worldIn, EntityLivingBase target, EntityLivingBase owner) {
        this(worldIn);
        this.targetEntity = target;
        this.ownerEntity = owner;

        if (target != null) {
            this.dataManager.set(TARGET_ID, target.getEntityId());
            this.setLocationAndAngles(target.posX, target.posY + (target.height / 2.0F), target.posZ, target.rotationYaw, target.rotationPitch);
        }
        if (owner != null) {
            this.dataManager.set(OWNER_ID, owner.getEntityId());
        }
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(TARGET_ID, -1);
        this.dataManager.register(OWNER_ID, -1);
    }

    public Entity getTarget() {
        int id = this.dataManager.get(TARGET_ID);
        return id != -1 ? this.world.getEntityByID(id) : null;
    }

    // Метод для получения игрока-хозяина
    public Entity getOwner() {
        int id = this.dataManager.get(OWNER_ID);
        return id != -1 ? this.world.getEntityByID(id) : null;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        Entity currentTarget = this.world.isRemote ? getTarget() : this.targetEntity;
        Entity currentOwner = this.world.isRemote ? getOwner() : this.ownerEntity;

        // Если хозяин умер, вышел из игры или убежал дальше 20 блоков — цепь рвется!
        if (currentOwner != null && (currentOwner.isDead || this.getDistance(currentOwner) > 20.0F)) {
            if (!this.world.isRemote) this.setDead();
            return;
        }

        if (currentTarget instanceof EntityLivingBase) {
            EntityLivingBase livingTarget = (EntityLivingBase) currentTarget;

            if (livingTarget.isEntityAlive()) {
                this.setPosition(livingTarget.posX, livingTarget.posY + (livingTarget.height / 2.0F), livingTarget.posZ);

                if (!this.world.isRemote) {
                    livingTarget.motionX = 0.0D;
                    livingTarget.motionZ = 0.0D;
                    if (livingTarget.motionY > 0.0D) {
                        livingTarget.motionY = -0.1D;
                    }

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