package com.poleesteel.rudazovmod.spell.api;

import com.github.bsideup.jabel.Desugar;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import java.util.Optional;

/**
 * Размеченная цель каста. Закрытый набор: Entity / Item / Block / None.
 * {@code sealed} на Jabel 1.0.1 (source 8) не компилируется — те же типы, без permits.
 */
public interface SpellTarget {

    TargetType type();

    static NoneTarget none() {
        return NoneTarget.INSTANCE;
    }

    static EntityTarget entity(Entity entity) {
        return new EntityTarget(entity.getEntityId());
    }

    static ItemTarget item(EntityItem item) {
        return new ItemTarget(item.getEntityId());
    }

    static BlockTarget block(BlockPos pos, EnumFacing face) {
        return new BlockTarget(pos, face);
    }

    static Optional<BlockTarget> fromBlockHit(RayTraceResult hit) {
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            return Optional.empty();
        }
        return Optional.of(new BlockTarget(hit.getBlockPos(), hit.sideHit));
    }

    @Desugar
    record EntityTarget(int entityId) implements SpellTarget {
        @Override
        public TargetType type() {
            return TargetType.ENTITY;
        }

        public Entity find(World world) {
            return world.getEntityByID(entityId);
        }
    }

    @Desugar
    record ItemTarget(int entityId) implements SpellTarget {
        @Override
        public TargetType type() {
            return TargetType.ITEM;
        }

        public EntityItem find(World world) {
            Entity entity = world.getEntityByID(entityId);
            return entity instanceof EntityItem ? (EntityItem) entity : null;
        }
    }

    @Desugar
    record BlockTarget(BlockPos pos, EnumFacing face) implements SpellTarget {
        @Override
        public TargetType type() {
            return TargetType.BLOCK;
        }
    }

    enum NoneTarget implements SpellTarget {
        INSTANCE;

        @Override
        public TargetType type() {
            return TargetType.NONE;
        }
    }
}
