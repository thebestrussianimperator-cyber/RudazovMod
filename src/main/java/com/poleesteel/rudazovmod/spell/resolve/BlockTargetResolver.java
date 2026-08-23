package com.poleesteel.rudazovmod.spell.resolve;

import com.poleesteel.rudazovmod.spell.api.CastContext;
import com.poleesteel.rudazovmod.spell.api.SpellTarget;
import com.poleesteel.rudazovmod.spell.api.TargetResolver;
import com.poleesteel.rudazovmod.spell.api.TargetType;

import java.util.Optional;

/**
 * Луч блока. В 1.12.2 это {@link net.minecraft.util.math.RayTraceResult}, не BlockHitResult.
 */
public final class BlockTargetResolver implements TargetResolver {

    @Override
    public TargetType type() {
        return TargetType.BLOCK;
    }

    @Override
    public Optional<SpellTarget> resolve(CastContext context) {
        Optional<SpellTarget.BlockTarget> hit = LookTrace.findBlock(context.caster())
                .flatMap(SpellTarget::fromBlockHit);
        if (!hit.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(hit.get());
    }

    @Override
    public boolean isStillValid(CastContext context, SpellTarget target) {
        if (!(target instanceof SpellTarget.BlockTarget blockTarget)) {
            return false;
        }
        return !context.world().isAirBlock(blockTarget.pos());
    }
}
