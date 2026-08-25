package com.poleesteel.rudazovmod.spell.api;

import com.github.bsideup.jabel.Desugar;
import com.poleesteel.rudazovmod.Tags;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Данные заклинания. Поведение задаётся осями, не Java-классом спелла.
 * {@link #cost()} — база из осей, в NBT не пишется. Скидка мастерства — {@link SpellCost#of(SpellDefinition, float, float)}.
 */
@Desugar
public record SpellDefinition(
        ResourceLocation id,
        CastMode castMode,
        TargetType targetType,
        Form form,
        SpellElement element,
        float power,
        ProjectileShape projectileShape,
        Homing homing
) {
    public SpellDefinition(
            ResourceLocation id,
            CastMode castMode,
            TargetType targetType,
            Form form,
            SpellElement element,
            float power) {
        this(id, castMode, targetType, form, element, power, ProjectileShape.ORB, Homing.NONE);
    }

    public SpellDefinition(
            ResourceLocation id,
            CastMode castMode,
            TargetType targetType,
            Form form,
            SpellElement element,
            float power,
            ProjectileShape projectileShape) {
        this(id, castMode, targetType, form, element, power, projectileShape, Homing.NONE);
    }

    public SpellDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(castMode, "castMode");
        Objects.requireNonNull(targetType, "targetType");
        Objects.requireNonNull(form, "form");
        Objects.requireNonNull(element, "element");
        if (projectileShape == null) {
            projectileShape = ProjectileShape.ORB;
        }
        if (homing == null) {
            homing = Homing.NONE;
        }
        if (power <= 0.0F || Float.isNaN(power) || Float.isInfinite(power)) {
            throw new IllegalArgumentException("power");
        }
        if (!SpellCombination.isLegal(form, targetType, castMode)) {
            throw new IllegalArgumentException(
                    "illegal combination: " + form + "/" + targetType + "/" + castMode);
        }
        if (!SpellCombination.usesProjectileShape(form, targetType, castMode)) {
            projectileShape = ProjectileShape.ORB;
            homing = Homing.NONE;
        }
    }

    public float cost() {
        return SpellCost.of(this);
    }

    public static ResourceLocation newCustomId() {
        return new ResourceLocation(Tags.MODID, "custom/" + UUID.randomUUID().toString());
    }

    public static SpellDefinition createCustom(
            CastMode castMode, TargetType targetType, Form form, SpellElement element, float power) {
        return createCustom(castMode, targetType, form, element, power, ProjectileShape.ORB, Homing.NONE);
    }

    public static SpellDefinition createCustom(
            CastMode castMode, TargetType targetType, Form form, SpellElement element, float power,
            ProjectileShape projectileShape) {
        return createCustom(castMode, targetType, form, element, power, projectileShape, Homing.NONE);
    }

    public static SpellDefinition createCustom(
            CastMode castMode, TargetType targetType, Form form, SpellElement element, float power,
            ProjectileShape projectileShape, Homing homing) {
        return new SpellDefinition(
                newCustomId(), castMode, targetType, form, element, power,
                projectileShape == null ? ProjectileShape.ORB : projectileShape,
                homing == null ? Homing.NONE : homing);
    }

    public static <T extends Enum<T>> Optional<T> parseEnum(Class<T> type, String raw) {
        if (raw == null || raw.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    /**
     * Строка без {@code :} получает namespace мода, не {@code minecraft:}.
     */
    public static ResourceLocation parseId(String raw) {
        if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException("id");
        }
        return raw.indexOf(':') >= 0
                ? new ResourceLocation(raw)
                : new ResourceLocation(Tags.MODID, raw);
    }

    public NBTTagCompound writeNBT() {
        return writeNBT(new NBTTagCompound());
    }

    public NBTTagCompound writeNBT(NBTTagCompound tag) {
        tag.setString("Id", id.toString());
        tag.setString("CastMode", castMode.name());
        tag.setString("TargetType", targetType.name());
        tag.setString("Form", form.name());
        tag.setString("Element", element.name());
        tag.setFloat("Power", power);
        tag.setString("ProjectileShape", projectileShape.name());
        tag.setString("Homing", homing.name());
        return tag;
    }

    public static Optional<SpellDefinition> readNBT(NBTTagCompound nbt) {
        if (nbt == null) {
            return Optional.empty();
        }
        try {
            if (!nbt.hasKey("Id", Constants.NBT.TAG_STRING)
                    || !nbt.hasKey("CastMode", Constants.NBT.TAG_STRING)
                    || !nbt.hasKey("TargetType", Constants.NBT.TAG_STRING)
                    || !nbt.hasKey("Form", Constants.NBT.TAG_STRING)
                    || !nbt.hasKey("Element", Constants.NBT.TAG_STRING)
                    || !nbt.hasKey("Power", Constants.NBT.TAG_ANY_NUMERIC)) {
                return Optional.empty();
            }
            ProjectileShape shape = ProjectileShape.ORB;
            if (nbt.hasKey("ProjectileShape", Constants.NBT.TAG_STRING)) {
                shape = parseEnum(ProjectileShape.class, nbt.getString("ProjectileShape"))
                        .orElse(ProjectileShape.ORB);
            }
            Homing homing = Homing.NONE;
            if (nbt.hasKey("Homing", Constants.NBT.TAG_STRING)) {
                homing = parseEnum(Homing.class, nbt.getString("Homing")).orElse(Homing.NONE);
            }
            SpellDefinition spell = new SpellDefinition(
                    parseId(nbt.getString("Id")),
                    parseEnum(CastMode.class, nbt.getString("CastMode")).orElseThrow(IllegalArgumentException::new),
                    parseEnum(TargetType.class, nbt.getString("TargetType")).orElseThrow(IllegalArgumentException::new),
                    parseEnum(Form.class, nbt.getString("Form")).orElseThrow(IllegalArgumentException::new),
                    parseEnum(SpellElement.class, nbt.getString("Element")).orElseThrow(IllegalArgumentException::new),
                    nbt.getFloat("Power"),
                    shape,
                    homing);
            return Optional.of(spell);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return Optional.empty();
        }
    }
}
