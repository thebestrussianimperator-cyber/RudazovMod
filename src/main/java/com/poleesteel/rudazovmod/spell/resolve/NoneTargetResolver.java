package com.poleesteel.rudazovmod.spell.resolve;

import com.poleesteel.rudazovmod.spell.api.CastContext;
import com.poleesteel.rudazovmod.spell.api.SpellTarget;
import com.poleesteel.rudazovmod.spell.api.TargetResolver;
import com.poleesteel.rudazovmod.spell.api.TargetType;

import java.util.Optional;

public final class NoneTargetResolver implements TargetResolver {

    @Override
    public TargetType type() {
        return TargetType.NONE;
    }

    @Override
    public Optional<SpellTarget> resolve(CastContext context) {
        return Optional.of(SpellTarget.none());
    }

    @Override
    public boolean isStillValid(CastContext context, SpellTarget target) {
        return target instanceof SpellTarget.NoneTarget;
    }
}
