package com.poleesteel.rudazovmod.spell.resolve;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Общий луч взгляда. Стена ближе сущности — сущность не берём.
 */
public final class LookTrace {

    public static final double RANGE = 16.0D;

    private LookTrace() {}

    public static Optional<Entity> findEntity(EntityPlayer player, Predicate<Entity> filter) {
        Vec3d eye = player.getPositionEyes(1.0F);
        Vec3d look = player.getLook(1.0F);
        Vec3d end = eye.add(look.x * RANGE, look.y * RANGE, look.z * RANGE);

        double maxDistSq = RANGE * RANGE;
        RayTraceResult blockHit = player.world.rayTraceBlocks(eye, end, false, false, false);
        if (blockHit != null && blockHit.typeOfHit == RayTraceResult.Type.BLOCK) {
            maxDistSq = eye.squareDistanceTo(blockHit.hitVec);
        }

        AxisAlignedBB searchBox = player.getEntityBoundingBox()
                .expand(look.x * RANGE, look.y * RANGE, look.z * RANGE)
                .grow(1.0D);

        List<Entity> list = player.world.getEntitiesWithinAABBExcludingEntity(player, searchBox);
        Entity closest = null;
        double closestDistSq = maxDistSq;

        for (Entity entity : list) {
            if (!filter.test(entity)) {
                continue;
            }
            AxisAlignedBB aabb = entity.getEntityBoundingBox().grow(0.3D);
            RayTraceResult intercept = aabb.calculateIntercept(eye, end);
            if (intercept == null) {
                continue;
            }
            double distSq = eye.squareDistanceTo(intercept.hitVec);
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closest = entity;
            }
        }

        return Optional.ofNullable(closest);
    }

    public static Optional<RayTraceResult> findBlock(EntityPlayer player) {
        Vec3d eye = player.getPositionEyes(1.0F);
        Vec3d look = player.getLook(1.0F);
        Vec3d end = eye.add(look.x * RANGE, look.y * RANGE, look.z * RANGE);
        RayTraceResult hit = player.world.rayTraceBlocks(eye, end, false, false, false);
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            return Optional.empty();
        }
        return Optional.of(hit);
    }
}
