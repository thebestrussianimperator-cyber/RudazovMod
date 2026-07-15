package com.poleesteel.rudazovmod.capabilities;

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
}