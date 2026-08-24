package com.poleesteel.rudazovmod.capabilities;

import com.poleesteel.rudazovmod.spell.api.Form;
import com.poleesteel.rudazovmod.spell.api.SpellDefinition;
import com.poleesteel.rudazovmod.spell.api.SpellElement;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface IActiveSpirit {
    float getMana();
    float getMaxMana();
    int getChakraLevel();

    void setMana(float mana);
    void setMaxMana(float maxMana);
    void setChakraLevel(int level);

    boolean consumeMana(float amount);
    void regenerate();
    void upgradeChakras();

    float getFormMastery(Form form);
    float getElementMastery(SpellElement element);
    void setFormMastery(Form form, float value);
    void setElementMastery(SpellElement element, float value);

    void unlockSpell(String spellId);
    boolean isSpellUnlocked(String spellId);
    Set<String> getUnlockedSpells();

    void bindSpell(int slot, String spellId); // slot от 0 до 3
    String getBoundSpell(int slot);
    Map<Integer, String> getBoundSpells();

    /** Свои собранные определения. Пресеты реестра здесь не живут. */
    Optional<SpellDefinition> getSpell(String spellId);
    Collection<SpellDefinition> getGrimoire();
    void putSpell(SpellDefinition spell);
    void clearUnlockedSpells();
    void clearBoundSpells();
    void clearGrimoire();

    /** В гримуаре или в unlock (пресет). */
    boolean ownsSpell(String spellId);
}
