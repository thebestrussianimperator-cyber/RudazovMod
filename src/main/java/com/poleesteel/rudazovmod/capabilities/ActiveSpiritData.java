package com.poleesteel.rudazovmod.capabilities;

import com.poleesteel.rudazovmod.spell.api.Form;
import com.poleesteel.rudazovmod.spell.api.SpellDefinition;
import com.poleesteel.rudazovmod.spell.api.SpellElement;
import com.poleesteel.rudazovmod.spell.api.SpellProgression;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ActiveSpiritData implements IActiveSpirit {
    private float currentMana = 50.0F;
    private float maxMana = 100.0F;
    private int chakraLevel = 1;

    private final EnumMap<Form, Float> formMastery = new EnumMap<>(Form.class);
    private final EnumMap<SpellElement, Float> elementMastery = new EnumMap<>(SpellElement.class);

    private final Set<String> unlockedSpells = new HashSet<>();
    private final Map<Integer, String> boundSpells = new HashMap<>();
    private final Map<String, SpellDefinition> grimoire = new LinkedHashMap<>();

    @Override public float getMana() { return this.currentMana; }
    @Override public float getMaxMana() { return this.maxMana; }
    @Override public int getChakraLevel() { return this.chakraLevel; }

    @Override
    public void setMana(float mana) {
        this.currentMana = Math.max(0.0F, Math.min(mana, this.maxMana));
    }

    @Override
    public void setMaxMana(float maxMana) {
        if (Float.isNaN(maxMana) || Float.isInfinite(maxMana)) {
            return;
        }
        this.maxMana = Math.max(1.0F, maxMana);
        if (this.currentMana > this.maxMana) {
            this.currentMana = this.maxMana;
        }
    }

    @Override
    public void setChakraLevel(int level) {
        this.chakraLevel = Math.max(SpellProgression.MIN_CHAKRA, Math.min(SpellProgression.MAX_CHAKRA, level));
    }

    @Override
    public boolean consumeMana(float amount) {
        if (this.currentMana >= amount) {
            this.currentMana -= amount;
            return true;
        }
        return false;
    }

    @Override
    public void regenerate() {
        if (this.currentMana < this.maxMana) {
            this.currentMana += 0.05F * this.chakraLevel;
            if (this.currentMana > this.maxMana) {
                this.currentMana = this.maxMana;
            }
        }
    }

    @Override
    public void upgradeChakras() {
        if (this.chakraLevel >= SpellProgression.MAX_CHAKRA) {
            return;
        }
        this.chakraLevel++;
        this.maxMana += SpellProgression.CHAKRA_MANA_STEP;
    }

    @Override
    public float getFormMastery(Form form) {
        if (form == null) {
            return 0.0F;
        }
        Float value = this.formMastery.get(form);
        return value == null ? 0.0F : value;
    }

    @Override
    public float getElementMastery(SpellElement element) {
        if (element == null) {
            return 0.0F;
        }
        Float value = this.elementMastery.get(element);
        return value == null ? 0.0F : value;
    }

    @Override
    public void setFormMastery(Form form, float value) {
        if (form != null) {
            this.formMastery.put(form, SpellProgression.clampMastery(value));
        }
    }

    @Override
    public void setElementMastery(SpellElement element, float value) {
        if (element != null) {
            this.elementMastery.put(element, SpellProgression.clampMastery(value));
        }
    }

    @Override
    public void unlockSpell(String spellId) {
        String canonical = canonicalize(spellId);
        if (canonical != null) {
            this.unlockedSpells.add(canonical);
        }
    }

    @Override
    public boolean isSpellUnlocked(String spellId) {
        if (spellId == null || spellId.isEmpty()) {
            return false;
        }
        if (this.unlockedSpells.contains(spellId)) {
            return true;
        }
        String canonical = canonicalize(spellId);
        return canonical != null && this.unlockedSpells.contains(canonical);
    }

    @Override
    public Set<String> getUnlockedSpells() {
        return this.unlockedSpells;
    }

    @Override
    public void bindSpell(int slot, String spellId) {
        if (slot < 0 || slot >= 4) {
            return;
        }
        String canonical = canonicalize(spellId);
        if (canonical != null) {
            this.boundSpells.put(slot, canonical);
        }
    }

    @Override
    public String getBoundSpell(int slot) {
        return this.boundSpells.getOrDefault(slot, "");
    }

    @Override
    public Map<Integer, String> getBoundSpells() {
        return this.boundSpells;
    }

    @Override
    public Optional<SpellDefinition> getSpell(String spellId) {
        if (spellId == null || spellId.isEmpty()) {
            return Optional.empty();
        }
        SpellDefinition direct = this.grimoire.get(spellId);
        if (direct != null) {
            return Optional.of(direct);
        }
        String canonical = canonicalize(spellId);
        if (canonical == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.grimoire.get(canonical));
    }

    @Override
    public Collection<SpellDefinition> getGrimoire() {
        return Collections.unmodifiableCollection(this.grimoire.values());
    }

    @Override
    public void putSpell(SpellDefinition spell) {
        if (spell != null) {
            this.grimoire.put(spell.id().toString(), spell);
        }
    }

    @Override
    public void clearUnlockedSpells() {
        this.unlockedSpells.clear();
    }

    @Override
    public void clearBoundSpells() {
        this.boundSpells.clear();
    }

    @Override
    public void clearGrimoire() {
        this.grimoire.clear();
    }

    @Override
    public boolean ownsSpell(String spellId) {
        return getSpell(spellId).isPresent() || isSpellUnlocked(spellId);
    }

    private static String canonicalize(String spellId) {
        if (spellId == null || spellId.isEmpty()) {
            return null;
        }
        try {
            return SpellDefinition.parseId(spellId).toString();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
