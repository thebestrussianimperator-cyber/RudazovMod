package com.poleesteel.rudazovmod.entities;

import com.poleesteel.rudazovmod.spell.api.ProjectileShape;
import com.poleesteel.rudazovmod.spell.api.SpellDefinition;
import com.poleesteel.rudazovmod.spell.api.SpellElement;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Универсальный магический снаряд. Вид (шар / стрела / копьё / молот) — не отдельная сущность.
 */
public class EntitySpellProjectile extends EntityThrowable {

    private static final DataParameter<Integer> ELEMENT_ORDINAL =
            EntityDataManager.createKey(EntitySpellProjectile.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> SHAPE_ORDINAL =
            EntityDataManager.createKey(EntitySpellProjectile.class, DataSerializers.VARINT);
    private static final DataParameter<Float> POWER =
            EntityDataManager.createKey(EntitySpellProjectile.class, DataSerializers.FLOAT);

    private static final float ELEMENT_SPEED_NORM = 2.15F;

    private final Set<Integer> piercedIds = new HashSet<>();
    private int pierceLeft;

    public EntitySpellProjectile(World worldIn) {
        super(worldIn);
    }

    public EntitySpellProjectile(World worldIn, EntityLivingBase throwerIn, SpellDefinition spell) {
        this(worldIn, throwerIn, spell.element(), spell.power(), spell.projectileShape());
    }

    public EntitySpellProjectile(World worldIn, EntityLivingBase throwerIn, SpellElement element, float power) {
        this(worldIn, throwerIn, element, power, ProjectileShape.ORB);
    }

    public EntitySpellProjectile(
            World worldIn, EntityLivingBase throwerIn, SpellElement element, float power, ProjectileShape shape) {
        super(worldIn, throwerIn);
        ProjectileShape resolved = shape == null ? ProjectileShape.ORB : shape;
        this.setElement(element);
        this.setShape(resolved);
        this.setPower(power);
        this.pierceLeft = resolved.extraPierce(power);
        this.applyShapeSize();
        float speed = resolved.speed() * (element.projectileSpeed() / ELEMENT_SPEED_NORM);
        this.shoot(throwerIn, throwerIn.rotationPitch, throwerIn.rotationYaw, 0.0F,
                speed, resolved.inaccuracy());
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(ELEMENT_ORDINAL, SpellElement.FIRE.ordinal());
        this.dataManager.register(SHAPE_ORDINAL, ProjectileShape.ORB.ordinal());
        this.dataManager.register(POWER, 1.0F);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (SHAPE_ORDINAL.equals(key) || POWER.equals(key)) {
            this.applyShapeSize();
        }
    }

    public SpellElement getElement() {
        return fromOrdinal(SpellElement.values(), this.dataManager.get(ELEMENT_ORDINAL), SpellElement.FIRE);
    }

    public void setElement(SpellElement element) {
        this.dataManager.set(ELEMENT_ORDINAL, (element == null ? SpellElement.FIRE : element).ordinal());
    }

    public ProjectileShape getShape() {
        return fromOrdinal(ProjectileShape.values(), this.dataManager.get(SHAPE_ORDINAL), ProjectileShape.ORB);
    }

    public void setShape(ProjectileShape shape) {
        this.dataManager.set(SHAPE_ORDINAL, (shape == null ? ProjectileShape.ORB : shape).ordinal());
    }

    public float getPower() {
        float value = this.dataManager.get(POWER);
        if (value <= 0.0F || Float.isNaN(value) || Float.isInfinite(value)) {
            return 1.0F;
        }
        return value;
    }

    public void setPower(float power) {
        float value = power <= 0.0F || Float.isNaN(power) || Float.isInfinite(power) ? 1.0F : power;
        this.dataManager.set(POWER, value);
    }

    @Override
    protected float getGravityVelocity() {
        ProjectileShape shape = getShape();
        float gravity = shape.gravity();
        if (shape != ProjectileShape.ARROW) {
            gravity += getElement().projectileGravity() * 0.35F;
        }
        return gravity;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.isDead) {
            return;
        }

        if (!this.world.isRemote && this.ticksExisted > getShape().maxLife()) {
            if (getShape().slamsOnExpire()) {
                impactSplash(null);
                playImpactFx();
            }
            this.setDead();
            return;
        }

        if (this.world.isRemote) {
            spawnTrail();
        }
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (this.world.isRemote) {
            return;
        }
        SpellElement element = getElement();
        float power = getPower();
        ProjectileShape shape = getShape();

        if (result.typeOfHit == RayTraceResult.Type.ENTITY && result.entityHit instanceof EntityLivingBase) {
            EntityLivingBase target = (EntityLivingBase) result.entityHit;
            if (target == this.getThrower() || this.piercedIds.contains(target.getEntityId())) {
                return;
            }
            this.piercedIds.add(target.getEntityId());
            hitLiving(target, element, power, shape, 1.0F);
            impactSplash(target);
            playImpactFx();
            if (this.pierceLeft > 0) {
                this.pierceLeft--;
                this.motionX *= 0.82D;
                this.motionY *= 0.82D;
                this.motionZ *= 0.82D;
                this.posX += this.motionX;
                this.posY += this.motionY;
                this.posZ += this.motionZ;
                return;
            }
            this.setDead();
            return;
        }

        if (result.typeOfHit == RayTraceResult.Type.BLOCK) {
            element.onWorldHit(this.world, result.getBlockPos(), result.sideHit,
                    power * shape.hitPowerMultiplier(), this.getThrower(), true);
            impactSplash(null);
            playImpactFx();
        }
        this.setDead();
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("Element", getElement().ordinal());
        compound.setInteger("Shape", getShape().ordinal());
        compound.setFloat("Power", getPower());
        compound.setInteger("PierceLeft", this.pierceLeft);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        setElement(fromOrdinal(SpellElement.values(), compound.getInteger("Element"), SpellElement.FIRE));
        setShape(fromOrdinal(ProjectileShape.values(), compound.getInteger("Shape"), ProjectileShape.ORB));
        setPower(compound.getFloat("Power"));
        this.pierceLeft = compound.getInteger("PierceLeft");
        applyShapeSize();
    }

    private void hitLiving(
            EntityLivingBase target, SpellElement element, float power, ProjectileShape shape, float knockbackFactor) {
        double mx = target.motionX;
        double my = target.motionY;
        double mz = target.motionZ;
        float hitPower = power * shape.hitPowerMultiplier();
        element.onHit(target, hitPower, this.getThrower());
        float retain = shape.elementKnockbackRetain();
        if (retain < 0.999F) {
            target.motionX = mx + (target.motionX - mx) * retain;
            target.motionY = my + (target.motionY - my) * retain;
            target.motionZ = mz + (target.motionZ - mz) * retain;
            target.velocityChanged = true;
        }
        applyShapeKnockback(target, knockbackFactor);
    }

    private void impactSplash(EntityLivingBase primary) {
        ProjectileShape shape = getShape();
        float radius = shape.splashRadius(getPower());
        if (radius <= 0.01F) {
            return;
        }
        SpellElement element = getElement();
        float splashPower = getPower() * shape.splashPowerFactor();
        List<EntityLivingBase> nearby = this.world.getEntitiesWithinAABB(
                EntityLivingBase.class, this.getEntityBoundingBox().grow(radius));
        for (EntityLivingBase living : nearby) {
            if (!living.isEntityAlive() || living == this.getThrower() || living == primary) {
                continue;
            }
            if (this.getDistance(living) > radius) {
                continue;
            }
            if (this.piercedIds.contains(living.getEntityId())) {
                continue;
            }
            this.piercedIds.add(living.getEntityId());
            hitLiving(living, element, splashPower / Math.max(0.01F, shape.hitPowerMultiplier()), shape, 0.45F);
        }
    }

    private void applyShapeKnockback(EntityLivingBase target, float factor) {
        ProjectileShape shape = getShape();
        float strength = shape.knockbackStrength(getPower()) * factor;
        if (strength <= 0.01F) {
            return;
        }
        EntityLivingBase thrower = this.getThrower();
        double dx;
        double dz;
        if (thrower != null) {
            dx = thrower.posX - target.posX;
            dz = thrower.posZ - target.posZ;
        } else {
            dx = -this.motionX;
            dz = -this.motionZ;
        }
        target.knockBack(this, strength, dx, dz);
        if (shape == ProjectileShape.HAMMER) {
            target.motionY += 0.32D * getPower() * factor;
            target.velocityChanged = true;
        }
    }

    private void playImpactFx() {
        ProjectileShape shape = getShape();
        SoundEvent sound;
        float volume;
        float pitch;
        EnumParticleTypes burst;
        int count;
        switch (shape) {
            case ARROW:
                sound = SoundEvents.ENTITY_ARROW_HIT;
                volume = 0.9F;
                pitch = 1.25F;
                burst = EnumParticleTypes.CRIT;
                count = 6;
                break;
            case SPEAR:
                sound = SoundEvents.ENTITY_PLAYER_ATTACK_STRONG;
                volume = 0.85F;
                pitch = 0.9F;
                burst = EnumParticleTypes.CRIT_MAGIC;
                count = 10;
                break;
            case HAMMER:
                sound = SoundEvents.BLOCK_ANVIL_LAND;
                volume = 0.55F;
                pitch = 0.7F;
                burst = EnumParticleTypes.SMOKE_LARGE;
                count = 14;
                break;
            case ORB:
            default:
                sound = SoundEvents.ENTITY_GENERIC_EXPLODE;
                volume = 0.35F;
                pitch = 1.45F;
                burst = EnumParticleTypes.EXPLOSION_NORMAL;
                count = 8;
                break;
        }
        this.world.playSound(null, this.posX, this.posY, this.posZ, sound, SoundCategory.PLAYERS, volume, pitch);
        if (this.world instanceof WorldServer) {
            WorldServer server = (WorldServer) this.world;
            double spread = shape == ProjectileShape.HAMMER || shape == ProjectileShape.ORB ? 0.35D : 0.12D;
            server.spawnParticle(burst, this.posX, this.posY, this.posZ, count, spread, spread, spread, 0.04D);
            int color = getElement().getColor();
            double r = ((color >> 16) & 0xFF) / 255.0D;
            double g = ((color >> 8) & 0xFF) / 255.0D;
            double b = (color & 0xFF) / 255.0D;
            server.spawnParticle(EnumParticleTypes.REDSTONE, this.posX, this.posY, this.posZ,
                    10, spread, spread, spread, 0.0D);
            // REDSTONE использует скорость как цвет на клиенте; дублируем стихийный след
            server.spawnParticle(getElement().trailParticle(), this.posX, this.posY, this.posZ,
                    8, spread, spread * 0.6D, spread, 0.02D);
            if (shape == ProjectileShape.ORB) {
                server.spawnParticle(EnumParticleTypes.SPELL_INSTANT, this.posX, this.posY, this.posZ,
                        6, r * 0.01D, g * 0.01D, b * 0.01D, 0.0D);
            }
        }
    }

    private void spawnTrail() {
        SpellElement element = getElement();
        ProjectileShape shape = getShape();
        int color = element.getColor();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        EnumParticleTypes trail = element.trailParticle();
        int count;
        double spread;
        switch (shape) {
            case ARROW:
                count = 1;
                spread = 0.04D;
                break;
            case SPEAR:
                count = 2;
                spread = 0.07D;
                break;
            case HAMMER:
                count = 2;
                spread = 0.32D;
                break;
            case ORB:
            default:
                count = 3;
                spread = 0.20D;
                break;
        }
        for (int i = 0; i < count; i++) {
            this.world.spawnParticle(EnumParticleTypes.REDSTONE,
                    this.posX + (this.rand.nextDouble() - 0.5D) * spread,
                    this.posY + (this.rand.nextDouble() - 0.5D) * spread,
                    this.posZ + (this.rand.nextDouble() - 0.5D) * spread,
                    r, g, b);
            this.world.spawnParticle(trail, this.posX, this.posY, this.posZ, 0.0D, 0.0D, 0.0D);
        }
    }

    private void applyShapeSize() {
        ProjectileShape shape = getShape();
        float scale = shape.sizeScale(getPower());
        this.setSize(shape.width() * scale, shape.height() * scale);
    }

    private static <T extends Enum<T>> T fromOrdinal(T[] values, int ordinal, T fallback) {
        if (ordinal < 0 || ordinal >= values.length) {
            return fallback;
        }
        return values[ordinal];
    }
}
