package com.poleesteel.rudazovmod.magic;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import com.poleesteel.rudazovmod.capabilities.ActiveSpiritProvider;
import com.poleesteel.rudazovmod.capabilities.IActiveSpirit;

public abstract class AbstractSpell {
    private final ResourceLocation id;
    private final String name;

    public AbstractSpell(String idName, String localizedName) {
        this.id = new ResourceLocation("rudazovmod", idName);
        this.name = localizedName;
    }

    public ResourceLocation getId() { return id; }
    public String getName() { return name; }

    /**
     * Сколько маны стоит заклинание (можно переопределять для динамического расчета)
     */
    public abstract float getManaCost(EntityPlayer player);

    /**
     * Можно ли применять заклинание прямо сейчас (кулдауны, условия)
     */
    public boolean canCast(EntityPlayer player) {
        IActiveSpirit spirit = player.getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
        return spirit != null && spirit.getMana() >= getManaCost(player);
    }

    /**
     * Главный метод: что именно делает магия в мире (вызывается СТРОГО на сервере!)
     */
    public abstract void onCast(EntityPlayer player);

    /**
     * Вспомогательный метод: списать ману и запустить каст
     */
    public boolean execute(EntityPlayer player) {
        if (canCast(player)) {
            IActiveSpirit spirit = player.getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
            if (spirit != null && spirit.consumeMana(getManaCost(player))) {
                onCast(player);
                return true;
            }
        }
        return false;
    }
}