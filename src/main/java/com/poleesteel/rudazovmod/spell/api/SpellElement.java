package com.poleesteel.rudazovmod.spell.api;

import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Стихия окрашивает форму: удар, удержание, снаряд, блок.
 * Не отдельный Java-класс заклинания.
 */
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
    },
    LIFE("Жизнь", 0x55FF88, 1.3F) {
        @Override
        public void onHit(EntityLivingBase target, float power, EntityLivingBase source) {
            applyLife(target, 3.0F * power, source);
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

    public EnumParticleTypes trailParticle() {
        switch (this) {
            case ICE:
                return EnumParticleTypes.SNOWBALL;
            case EARTH:
                return EnumParticleTypes.CRIT;
            case LIFE:
                return EnumParticleTypes.HEART;
            case FIRE:
            default:
                return EnumParticleTypes.FLAME;
        }
    }

    /** Множитель тяги HOLD. Лёд вязкий, земля — сырая сила. */
    public float holdPullMultiplier() {
        switch (this) {
            case ICE:
                return 0.55F;
            case EARTH:
                return 1.45F;
            case LIFE:
                return 0.85F;
            case FIRE:
            default:
                return 1.0F;
        }
    }

    public float projectileSpeed() {
        switch (this) {
            case ICE:
                return 1.6F;
            case EARTH:
                return 2.2F;
            case LIFE:
                return 2.0F;
            case FIRE:
            default:
                return 2.8F;
        }
    }

    public float projectileGravity() {
        switch (this) {
            case ICE:
                return 0.02F;
            case EARTH:
                return 0.08F;
            case LIFE:
                return 0.015F;
            case FIRE:
            default:
                return 0.01F;
        }
    }

    /**
     * HOLD каждый тик. Не полный onHit каждый тик — иначе земля убивает за полсекунды.
     */
    public void onHoldTick(Entity target, float power, EntityLivingBase caster, int ticksHeld) {
        if (this == LIFE && (target == null || target instanceof EntityItem)
                && ticksHeld > 0 && ticksHeld % 20 == 0 && caster != null) {
            caster.heal(0.5F * power);
            return;
        }
        if (target == null || !target.isEntityAlive()) {
            return;
        }
        switch (this) {
            case FIRE:
                if (target instanceof EntityLivingBase) {
                    target.setFire((int) Math.max(2, 2 * power));
                    if (ticksHeld > 0 && ticksHeld % 20 == 0) {
                        onHit((EntityLivingBase) target, power * 0.35F, caster);
                    }
                } else if (target instanceof EntityItem && ticksHeld == 20) {
                    smeltItem((EntityItem) target);
                }
                break;
            case ICE:
                if (target instanceof EntityLivingBase living) {
                    living.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 40, 3, false, true));
                    if (ticksHeld > 0 && ticksHeld % 20 == 0) {
                        onHit(living, power * 0.4F, caster);
                    }
                }
                break;
            case LIFE:
                if (target instanceof EntityLivingBase living && living != caster
                        && ticksHeld > 0 && ticksHeld % 10 == 0) {
                    drainToCaster(living, 1.5F * power, caster);
                }
                break;
            case EARTH:
            default:
                break;
        }
    }

    /** HOLD при отпускании: земля швыряет, лёд оставляет столбцом, огонь дожигает. */
    public void onHoldRelease(Entity target, float power, EntityLivingBase caster) {
        if (target == null || !target.isEntityAlive()) {
            return;
        }
        switch (this) {
            case FIRE:
                target.setFire((int) (4 * power));
                break;
            case ICE:
                target.motionX = 0.0D;
                target.motionY = 0.0D;
                target.motionZ = 0.0D;
                target.velocityChanged = true;
                if (target instanceof EntityLivingBase living) {
                    living.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, (int) (80 * power), 4, false, true));
                }
                break;
            case EARTH:
                if (target instanceof EntityLivingBase living) {
                    onHit(living, power, caster);
                }
                target.motionY -= 0.55D * power;
                target.velocityChanged = true;
                break;
            case LIFE:
                if (target instanceof EntityLivingBase living && living != caster) {
                    drainToCaster(living, 2.0F * power, caster);
                }
                break;
            default:
                break;
        }
    }

    /**
     * Удар RAY по блоку. {@code concentrated} — снаряд INSTANT; канал жжёт/морозит слабее и не копает землю.
     */
    public void onWorldHit(World world, BlockPos pos, EnumFacing face, float power,
            EntityLivingBase caster, boolean concentrated) {
        if (world == null || world.isRemote || pos == null) {
            return;
        }
        if (caster instanceof EntityPlayer && !world.isBlockModifiable((EntityPlayer) caster, pos)) {
            return;
        }
        switch (this) {
            case FIRE:
                igniteNear(world, pos, face);
                break;
            case ICE:
                freezeBlock(world, pos);
                break;
            case EARTH:
                punchBlock(world, pos, power, concentrated);
                break;
            case LIFE:
                if (concentrated || world.rand.nextFloat() < 0.25F) {
                    tryGrow(world, pos);
                }
                break;
            default:
                break;
        }
    }

    private static void applyLife(EntityLivingBase target, float amount, EntityLivingBase source) {
        if (target.isEntityUndead()) {
            target.attackEntityFrom(DamageSource.MAGIC, amount);
        } else {
            target.heal(amount);
        }
    }

    private static void drainToCaster(EntityLivingBase target, float amount, EntityLivingBase caster) {
        float before = target.getHealth();
        if (target.isEntityUndead()) {
            target.attackEntityFrom(DamageSource.MAGIC, amount * 1.25F);
        } else {
            target.attackEntityFrom(DamageSource.MAGIC, amount);
        }
        float taken = Math.max(0.0F, before - target.getHealth());
        if (taken > 0.0F && caster != null && caster.isEntityAlive()) {
            caster.heal(taken);
        }
    }

    private static void tryGrow(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof IGrowable)) {
            return;
        }
        IGrowable growable = (IGrowable) state.getBlock();
        if (growable.canGrow(world, pos, state, false)
                && growable.canUseBonemeal(world, world.rand, pos, state)) {
            growable.grow(world, world.rand, pos, state);
        }
    }

    private static void smeltItem(EntityItem drop) {
        ItemStack input = drop.getItem();
        if (input.isEmpty()) {
            return;
        }
        ItemStack recipe = FurnaceRecipes.instance().getSmeltingResult(input);
        if (recipe.isEmpty()) {
            return;
        }
        ItemStack cooked = recipe.copy();
        cooked.setCount(input.getCount());
        drop.setItem(cooked);
    }

    private static void igniteNear(World world, BlockPos pos, EnumFacing face) {
        BlockPos air = face == null ? pos.up() : pos.offset(face);
        if (world.isAirBlock(air) && Blocks.FIRE.canPlaceBlockAt(world, air)) {
            world.setBlockState(air, Blocks.FIRE.getDefaultState());
        }
    }

    private static void freezeBlock(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Material material = state.getMaterial();
        if (material == Material.WATER) {
            world.setBlockState(pos, Blocks.ICE.getDefaultState());
        } else if (material == Material.LAVA) {
            world.setBlockState(pos, Blocks.OBSIDIAN.getDefaultState());
        }
    }

    private static void punchBlock(World world, BlockPos pos, float power, boolean concentrated) {
        IBlockState state = world.getBlockState(pos);
        float hardness = state.getBlockHardness(world, pos);
        if (hardness < 0.0F) {
            return;
        }
        if (!concentrated && hardness > 0.0F) {
            return;
        }
        float limit = concentrated ? 1.5F * Math.max(1.0F, power) : 0.0F;
        if (hardness <= limit) {
            world.destroyBlock(pos, true);
        }
    }
}
