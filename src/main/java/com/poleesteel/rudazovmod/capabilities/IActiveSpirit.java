package com.poleesteel.rudazovmod.capabilities;

import java.util.Set;
import java.util.Map;

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

    void unlockSpell(String spellId);
    boolean isSpellUnlocked(String spellId);
    Set<String> getUnlockedSpells();

    void bindSpell(int slot, String spellId); // slot от 0 до 3
    String getBoundSpell(int slot);
    Map<Integer, String> getBoundSpells();
}