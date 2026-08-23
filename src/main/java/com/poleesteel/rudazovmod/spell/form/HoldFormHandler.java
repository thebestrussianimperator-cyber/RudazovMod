package com.poleesteel.rudazovmod.spell.form;

import com.github.bsideup.jabel.Desugar;
import com.poleesteel.rudazovmod.spell.api.CastContext;
import com.poleesteel.rudazovmod.spell.api.Form;
import com.poleesteel.rudazovmod.spell.api.FormHandler;
import com.poleesteel.rudazovmod.spell.api.SpellElement;
import com.poleesteel.rudazovmod.spell.api.SpellTarget;
import com.poleesteel.rudazovmod.spell.resolve.LookTrace;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Удержание цели перед кастером. Стихия окрашивает тягу, не подменяет форму.
 * ENTITY/ITEM — motion сущности (у дропа ещё запрет pickup).
 * BLOCK — мутация мира: вынуть, нести, поставить. Не motionX.
 */
public final class HoldFormHandler implements FormHandler {

    private static final double HOLD_DISTANCE = 4.0D;
    private static final double PULL = 0.4D;
    private static final Map<UUID, CarriedBlock> CARRIED = new HashMap<>();

    @Override
    public Form form() {
        return Form.HOLD;
    }

    @Override
    public boolean canStart(CastContext context) {
        if (context.target() instanceof SpellTarget.BlockTarget blockTarget) {
            return canLift(context.world(), context.caster(), blockTarget.pos());
        }
        return true;
    }

    @Override
    public boolean isTargetStillHeld(CastContext context) {
        return CARRIED.containsKey(context.caster().getUniqueID());
    }

    @Override
    public void onStart(CastContext context) {
        if (context.world().isRemote) {
            return;
        }
        SpellTarget target = context.target();
        if (target instanceof SpellTarget.EntityTarget entityTarget) {
            pullEntity(entityTarget.find(context.world()), context);
        } else if (target instanceof SpellTarget.ItemTarget itemTarget) {
            holdItem(itemTarget.find(context.world()), context);
        } else if (target instanceof SpellTarget.BlockTarget blockTarget) {
            pickupBlock(context, blockTarget.pos());
        }
    }

    @Override
    public void onTick(CastContext context) {
        if (context.world().isRemote) {
            return;
        }
        SpellTarget target = context.target();
        if (target instanceof SpellTarget.EntityTarget entityTarget) {
            pullEntity(entityTarget.find(context.world()), context);
        } else if (target instanceof SpellTarget.ItemTarget itemTarget) {
            holdItem(itemTarget.find(context.world()), context);
        } else if (target instanceof SpellTarget.BlockTarget blockTarget) {
            if (!CARRIED.containsKey(context.caster().getUniqueID())) {
                pickupBlock(context, blockTarget.pos());
            }
            hoverCarried(context);
        }
    }

    @Override
    public void onEnd(CastContext context) {
        if (context.world().isRemote) {
            return;
        }
        SpellTarget target = context.target();
        if (target instanceof SpellTarget.ItemTarget itemTarget) {
            EntityItem item = itemTarget.find(context.world());
            if (item != null) {
                item.setNoPickupDelay();
                context.spell().element().onHoldRelease(item, context.spell().power(), context.caster());
            }
        } else if (target instanceof SpellTarget.EntityTarget entityTarget) {
            Entity entity = entityTarget.find(context.world());
            context.spell().element().onHoldRelease(entity, context.spell().power(), context.caster());
        }
        releaseBlock(context);
    }

    private static void holdItem(EntityItem item, CastContext context) {
        if (item == null || !item.isEntityAlive()) {
            return;
        }
        item.setInfinitePickupDelay();
        item.setNoDespawn();
        pullEntity(item, context);
    }

    private static void pullEntity(Entity entity, CastContext context) {
        if (entity == null || !entity.isEntityAlive()) {
            return;
        }
        float pull = (float) PULL * context.spell().element().holdPullMultiplier();
        Vec3d holdPos = holdPos(context);
        entity.motionX = (holdPos.x - entity.posX) * pull;
        entity.motionY = (holdPos.y - entity.posY) * pull;
        entity.motionZ = (holdPos.z - entity.posZ) * pull;
        entity.isAirBorne = true;
        entity.fallDistance = 0.0F;
        entity.velocityChanged = true;
        flavor(entity, context);
    }

    private static void flavor(Entity entity, CastContext context) {
        context.spell().element().onHoldTick(
                entity, context.spell().power(), context.caster(), context.ticksHeld());
        spawnElementParticles(context, entity.posX, entity.posY + entity.height * 0.5D, entity.posZ);
    }

    private static void pickupBlock(CastContext context, BlockPos pos) {
        World world = context.world();
        EntityPlayer player = context.caster();
        if (!canLift(world, player, pos)) {
            return;
        }
        IBlockState state = world.getBlockState(pos);
        NBTTagCompound tileNbt = null;
        TileEntity tile = world.getTileEntity(pos);
        if (tile != null) {
            tileNbt = tile.writeToNBT(new NBTTagCompound());
        }
        world.playEvent(2001, pos, Block.getStateId(state));
        world.setBlockToAir(pos);
        CARRIED.put(player.getUniqueID(), new CarriedBlock(state, tileNbt, pos.toImmutable()));
    }

    private static void hoverCarried(CastContext context) {
        CarriedBlock carried = CARRIED.get(context.caster().getUniqueID());
        if (carried == null) {
            return;
        }
        context.spell().element().onHoldTick(
                null, context.spell().power(), context.caster(), context.ticksHeld());
        if (!(context.world() instanceof WorldServer)) {
            return;
        }
        Vec3d holdPos = holdPos(context);
        WorldServer world = (WorldServer) context.world();
        world.spawnParticle(
                EnumParticleTypes.BLOCK_CRACK,
                holdPos.x, holdPos.y, holdPos.z,
                6, 0.15D, 0.15D, 0.15D, 0.0D,
                Block.getStateId(carried.state()));
        spawnElementParticles(context, holdPos.x, holdPos.y, holdPos.z);
    }

    private static void spawnElementParticles(CastContext context, double x, double y, double z) {
        if (!(context.world() instanceof WorldServer)) {
            return;
        }
        SpellElement element = context.spell().element();
        ((WorldServer) context.world()).spawnParticle(
                element.trailParticle(), x, y, z, 4, 0.12D, 0.12D, 0.12D, 0.0D);
    }

    private static void releaseBlock(CastContext context) {
        CarriedBlock carried = CARRIED.remove(context.caster().getUniqueID());
        if (carried == null) {
            return;
        }
        if (tryPlace(context, lookPlacePos(context), lookPlaceFace(context), carried)) {
            return;
        }
        if (tryPlace(context, carried.origin(), EnumFacing.UP, carried)) {
            return;
        }
        dropCarried(context, carried);
    }

    private static boolean tryPlace(CastContext context, BlockPos pos, EnumFacing face, CarriedBlock carried) {
        if (pos == null) {
            return false;
        }
        World world = context.world();
        EntityPlayer player = context.caster();
        if (!world.isBlockLoaded(pos) || pos.getY() < 0 || pos.getY() >= world.getHeight()) {
            return false;
        }
        if (!world.isBlockModifiable(player, pos)) {
            return false;
        }
        Block block = carried.state().getBlock();
        if (!world.mayPlace(block, pos, false, face, player)) {
            return false;
        }
        if (!world.setBlockState(pos, carried.state(), 3)) {
            return false;
        }
        restoreTile(world, pos, carried);
        return true;
    }

    private static void restoreTile(World world, BlockPos pos, CarriedBlock carried) {
        if (carried.tileNbt() == null) {
            return;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (tile == null) {
            return;
        }
        NBTTagCompound copy = carried.tileNbt().copy();
        copy.setInteger("x", pos.getX());
        copy.setInteger("y", pos.getY());
        copy.setInteger("z", pos.getZ());
        tile.readFromNBT(copy);
        tile.markDirty();
    }

    private static void dropCarried(CastContext context, CarriedBlock carried) {
        Vec3d holdPos = holdPos(context);
        Item item = Item.getItemFromBlock(carried.state().getBlock());
        if (item == Items.AIR) {
            return;
        }
        int meta = carried.state().getBlock().damageDropped(carried.state());
        EntityItem drop = new EntityItem(
                context.world(), holdPos.x, holdPos.y, holdPos.z,
                new ItemStack(item, 1, meta));
        drop.setNoPickupDelay();
        context.world().spawnEntity(drop);
    }

    private static boolean canLift(World world, EntityPlayer player, BlockPos pos) {
        if (!world.isBlockLoaded(pos) || world.isAirBlock(pos)) {
            return false;
        }
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (state.getMaterial() == Material.AIR || state.getBlockHardness(world, pos) < 0.0F) {
            return false;
        }
        if (block == Blocks.BARRIER
                || block == Blocks.BEDROCK
                || block == Blocks.COMMAND_BLOCK
                || block == Blocks.CHAIN_COMMAND_BLOCK
                || block == Blocks.REPEATING_COMMAND_BLOCK
                || block == Blocks.STRUCTURE_BLOCK
                || block == Blocks.STRUCTURE_VOID
                || block == Blocks.END_PORTAL
                || block == Blocks.END_PORTAL_FRAME
                || block == Blocks.END_GATEWAY
                || block == Blocks.PORTAL
                || block == Blocks.PISTON_HEAD
                || block == Blocks.PISTON_EXTENSION) {
            return false;
        }
        if (block instanceof BlockDoor || block instanceof BlockBed || block instanceof BlockDoublePlant) {
            return false;
        }
        if (!world.isBlockModifiable(player, pos)) {
            return false;
        }
        return !MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(world, pos, state, player));
    }

    private static Vec3d holdPos(CastContext context) {
        Vec3d eye = context.eyePos();
        Vec3d look = context.lookVec();
        return eye.add(look.x * HOLD_DISTANCE, look.y * HOLD_DISTANCE, look.z * HOLD_DISTANCE);
    }

    private static BlockPos lookPlacePos(CastContext context) {
        RayTraceResult hit = context.world().rayTraceBlocks(
                context.eyePos(),
                context.eyePos().add(
                        context.lookVec().x * LookTrace.RANGE,
                        context.lookVec().y * LookTrace.RANGE,
                        context.lookVec().z * LookTrace.RANGE),
                false, false, false);
        if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK) {
            return hit.getBlockPos().offset(hit.sideHit);
        }
        return new BlockPos(holdPos(context));
    }

    private static EnumFacing lookPlaceFace(CastContext context) {
        RayTraceResult hit = context.world().rayTraceBlocks(
                context.eyePos(),
                context.eyePos().add(
                        context.lookVec().x * LookTrace.RANGE,
                        context.lookVec().y * LookTrace.RANGE,
                        context.lookVec().z * LookTrace.RANGE),
                false, false, false);
        if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK) {
            return hit.sideHit;
        }
        return EnumFacing.UP;
    }

    @Desugar
    private record CarriedBlock(IBlockState state, NBTTagCompound tileNbt, BlockPos origin) {}
}
