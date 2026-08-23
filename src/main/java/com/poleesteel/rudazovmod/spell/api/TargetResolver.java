package com.poleesteel.rudazovmod.spell.api;

import java.util.Optional;

public interface TargetResolver {

    TargetType type();

    Optional<SpellTarget> resolve(CastContext context);

    boolean isStillValid(CastContext context, SpellTarget target);
}
