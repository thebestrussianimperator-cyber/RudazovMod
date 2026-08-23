package com.poleesteel.rudazovmod.spell.resolve;

import com.poleesteel.rudazovmod.spell.api.CastContext;
import com.poleesteel.rudazovmod.spell.api.SpellTarget;
import com.poleesteel.rudazovmod.spell.api.TargetResolver;
import com.poleesteel.rudazovmod.spell.api.TargetType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;

import java.util.Optional;

/**
 * Живые и коллизируемые сущности. {@link EntityItem} сюда не входит.
 */
public final class EntityTargetResolver implements TargetResolver {

    @Override
    public TargetType type() {
        return TargetType.ENTITY;
    }

    @Override
    public Optional<SpellTarget> resolve(CastContext context) {
        return LookTrace.findEntity(context.caster(), EntityTargetResolver::isEntityCandidate)
                .map(SpellTarget::entity);
    }

    @Override
    public boolean isStillValid(CastContext context, SpellTarget target) {
        if (!(target instanceof SpellTarget.EntityTarget entityTarget)) {
            return false;
        }
        Entity entity = entityTarget.find(context.world());
        return entity != null && isEntityCandidate(entity);
    }

    private static boolean isEntityCandidate(Entity entity) {
        if (entity == null || !entity.isEntityAlive() || entity instanceof EntityItem) {
            return false;
        }
        return entity.canBeCollidedWith() || entity instanceof EntityLivingBase;
    }
}
