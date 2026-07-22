package com.poleesteel.rudazovmod.capabilities;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;

public class ActiveSpiritData implements IActiveSpirit {
    private float currentMana = 50.0F;
    private float maxMana = 100.0F;
    private int chakraLevel = 1;

    @Override public float getMana() { return this.currentMana; }
    @Override public float getMaxMana() { return this.maxMana; }
    @Override public int getChakraLevel() { return this.chakraLevel; }

    @Override public void setMana(float mana) { this.currentMana = Math.min(mana, this.maxMana); }
    @Override public void setMaxMana(float maxMana) { this.maxMana = maxMana; }
    @Override public void setChakraLevel(int level) { this.chakraLevel = level; }

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
        this.chakraLevel++;
        this.maxMana += 50.0F;
    }

    private final Set<String> unlockedSpells = new HashSet<>();
    private final Map<Integer, String> boundSpells = new HashMap<>();

    @Override
    public void unlockSpell(String spellId) {
        this.unlockedSpells.add(spellId);
    }

    @Override
    public boolean isSpellUnlocked(String spellId) {
        return this.unlockedSpells.contains(spellId);
    }

    @Override
    public Set<String> getUnlockedSpells() {
        return this.unlockedSpells;
    }

    @Override
    public void bindSpell(int slot, String spellId) {
        if (slot >= 0 && slot < 4) {
            this.boundSpells.put(slot, spellId);
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
}