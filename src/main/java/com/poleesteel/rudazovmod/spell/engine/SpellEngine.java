package com.poleesteel.rudazovmod.spell.engine;

import com.poleesteel.rudazovmod.RudazovMod;
import com.poleesteel.rudazovmod.capabilities.ActiveSpiritProvider;
import com.poleesteel.rudazovmod.capabilities.IActiveSpirit;
import com.poleesteel.rudazovmod.spell.api.CastContext;
import com.poleesteel.rudazovmod.spell.api.CastMode;
import com.poleesteel.rudazovmod.spell.api.Form;
import com.poleesteel.rudazovmod.spell.api.FormHandler;
import com.poleesteel.rudazovmod.spell.api.SpellCombination;
import com.poleesteel.rudazovmod.spell.api.SpellDefinition;
import com.poleesteel.rudazovmod.spell.api.SpellTarget;
import com.poleesteel.rudazovmod.spell.api.TargetResolver;
import com.poleesteel.rudazovmod.spell.api.TargetType;
import com.poleesteel.rudazovmod.spell.form.HoldFormHandler;
import com.poleesteel.rudazovmod.spell.form.RayFormHandler;
import com.poleesteel.rudazovmod.spell.resolve.BlockTargetResolver;
import com.poleesteel.rudazovmod.spell.resolve.EntityTargetResolver;
import com.poleesteel.rudazovmod.spell.resolve.ItemTargetResolver;
import com.poleesteel.rudazovmod.spell.resolve.NoneTargetResolver;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Единственный вход каста: resolve → мана → form.onStart/onTick/onEnd.
 */
public final class SpellEngine {

    private static final Map<TargetType, TargetResolver> RESOLVERS = new EnumMap<>(TargetType.class);
    private static final Map<Form, FormHandler> FORMS = new EnumMap<>(Form.class);
    private static final ActiveCastTracker TRACKER = new ActiveCastTracker();

    private SpellEngine() {}

    public static void init() {
        registerResolver(new NoneTargetResolver());
        registerResolver(new EntityTargetResolver());
        registerResolver(new ItemTargetResolver());
        registerResolver(new BlockTargetResolver());
        registerForm(new RayFormHandler());
        registerForm(new HoldFormHandler());
        SpellRegistry.registerDefaults();
    }

    /**
     * Гримуар игрока, иначе пресет {@link SpellRegistry}. В статический реестр кастомы не кладём.
     */
    public static Optional<SpellDefinition> findDefinition(EntityPlayer caster, String spellId) {
        if (spellId == null || spellId.isEmpty()) {
            return Optional.empty();
        }
        IActiveSpirit spirit = caster.getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
        if (spirit != null) {
            Optional<SpellDefinition> custom = spirit.getSpell(spellId);
            if (custom.isPresent()) {
                return custom;
            }
        }
        return SpellRegistry.get(spellId);
    }

    public static boolean startCast(EntityPlayer caster, String spellId) {
        Optional<SpellDefinition> spell = findDefinition(caster, spellId);
        return spell.isPresent() && startCast(caster, spell.get());
    }

    public static boolean startCast(EntityPlayer caster, ResourceLocation spellId) {
        return startCast(caster, spellId == null ? null : spellId.toString());
    }

    public static boolean startCast(EntityPlayer caster, SpellDefinition spell) {
        if (caster.world.isRemote) {
            return false;
        }
        if (!SpellCombination.canCast(spell)) {
            RudazovMod.LOGGER.warn("[spell] Некастуемая комбинация {}", spell.id());
            return false;
        }

        TargetResolver resolver = RESOLVERS.get(spell.targetType());
        FormHandler handler = FORMS.get(spell.form());
        if (resolver == null || handler == null) {
            RudazovMod.LOGGER.warn("[spell] Нет resolver/form для {}", spell.id());
            return false;
        }

        CastContext context = CastContext.start(caster, spell);
        Optional<SpellTarget> resolved = resolver.resolve(context);
        if (!resolved.isPresent()) {
            return false;
        }
        SpellTarget target = resolved.get();
        if (target.type() != spell.targetType()) {
            RudazovMod.LOGGER.warn("[spell] TargetType mismatch: spell={} expected={} got={}",
                    spell.id(), spell.targetType(), target.type());
            return false;
        }
        context = context.withTarget(target);

        if (!canAfford(caster, spell.cost())) {
            return false;
        }

        if (spell.castMode() == CastMode.CHANNEL && TRACKER.isActive(caster)) {
            endCast(caster);
        }

        if (spell.castMode() == CastMode.INSTANT) {
            if (!tryConsume(caster, spell.cost())) {
                return false;
            }
            handler.onStart(context);
            handler.onEnd(context);
            return true;
        }

        handler.onStart(context);
        TRACKER.begin(caster, spell, target);
        return true;
    }

    public static void tick(EntityPlayer caster) {
        if (caster.world.isRemote) {
            return;
        }

        Optional<ActiveCastTracker.ActiveCast> opt = TRACKER.get(caster);
        if (!opt.isPresent()) {
            return;
        }
        ActiveCastTracker.ActiveCast cast = opt.get();

        if (!caster.isEntityAlive()) {
            endCast(caster);
            return;
        }

        SpellDefinition spell = cast.spell();
        TargetResolver resolver = RESOLVERS.get(spell.targetType());
        FormHandler handler = FORMS.get(spell.form());
        if (resolver == null || handler == null) {
            TRACKER.remove(caster);
            return;
        }

        CastContext context = CastContext.start(caster, spell)
                .withTarget(cast.target())
                .withTicksHeld(Math.max(0, caster.ticksExisted - cast.startTick()));

        if (!resolver.isStillValid(context, cast.target()) || !tryConsume(caster, spell.cost())) {
            endCast(caster);
            return;
        }

        handler.onTick(context);
    }

    public static void endCast(EntityPlayer caster) {
        if (caster.world.isRemote) {
            return;
        }

        Optional<ActiveCastTracker.ActiveCast> opt = TRACKER.remove(caster);
        if (!opt.isPresent()) {
            return;
        }
        ActiveCastTracker.ActiveCast cast = opt.get();
        SpellDefinition spell = cast.spell();
        FormHandler handler = FORMS.get(spell.form());
        if (handler == null) {
            return;
        }

        CastContext context = CastContext.start(caster, spell)
                .withTarget(cast.target())
                .withTicksHeld(Math.max(0, caster.ticksExisted - cast.startTick()));
        handler.onEnd(context);
    }

    private static boolean canAfford(EntityPlayer caster, float cost) {
        if (cost <= 0.0F) {
            return true;
        }
        IActiveSpirit spirit = caster.getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
        return spirit != null && spirit.getMana() >= cost;
    }

    private static boolean tryConsume(EntityPlayer caster, float cost) {
        if (cost <= 0.0F) {
            return true;
        }
        IActiveSpirit spirit = caster.getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
        return spirit != null && spirit.consumeMana(cost);
    }

    private static void registerResolver(TargetResolver resolver) {
        RESOLVERS.put(resolver.type(), resolver);
    }

    private static void registerForm(FormHandler handler) {
        FORMS.put(handler.form(), handler);
    }
}
