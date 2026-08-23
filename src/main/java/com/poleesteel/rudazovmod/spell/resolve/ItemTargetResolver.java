package com.poleesteel.rudazovmod.spell.resolve;

import com.poleesteel.rudazovmod.spell.api.CastContext;
import com.poleesteel.rudazovmod.spell.api.SpellTarget;
import com.poleesteel.rudazovmod.spell.api.TargetResolver;
import com.poleesteel.rudazovmod.spell.api.TargetType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;

import java.util.Optional;

/**
 * Только дроп ({@link EntityItem} в 1.12.2, ItemEntity в современном маппинге).
 */
public final class ItemTargetResolver implements TargetResolver {

    @Override
    public TargetType type() {
        return TargetType.ITEM;
    }

    @Override
    public Optional<SpellTarget> resolve(CastContext context) {
        return LookTrace.findEntity(context.caster(), ItemTargetResolver::isItemDrop)
                .filter(entity -> entity instanceof EntityItem)
                .map(entity -> SpellTarget.item((EntityItem) entity));
    }

    @Override
    public boolean isStillValid(CastContext context, SpellTarget target) {
        if (!(target instanceof SpellTarget.ItemTarget itemTarget)) {
            return false;
        }
        EntityItem item = itemTarget.find(context.world());
        return isItemDrop(item);
    }

    private static boolean isItemDrop(Entity entity) {
        return entity instanceof EntityItem && entity.isEntityAlive();
    }
}
