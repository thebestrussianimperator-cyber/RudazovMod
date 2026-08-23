package com.poleesteel.rudazovmod.client.render.hud;

import com.poleesteel.rudazovmod.capabilities.ActiveSpiritProvider;
import com.poleesteel.rudazovmod.capabilities.IActiveSpirit;
import com.poleesteel.rudazovmod.client.gui.GuiGrimoire;
import com.poleesteel.rudazovmod.client.input.KeyBindHandler;
import com.poleesteel.rudazovmod.spell.api.SpellDefinition;
import com.poleesteel.rudazovmod.spell.engine.SpellEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Optional;

@Mod.EventBusSubscriber(Side.CLIENT)
public class SpellSlotHud {

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onRenderGui(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.TEXT) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null || mc.gameSettings.hideGUI) {
            return;
        }
        IActiveSpirit spirit = player.getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
        if (spirit == null) {
            return;
        }

        ScaledResolution res = event.getResolution();
        int x = 4;
        int y = res.getScaledHeight() - 78;
        for (int i = 0; i < 4; i++) {
            String bound = spirit.getBoundSpell(i);
            String key = KeyBindHandler.SLOTS[i].getDisplayName();
            String name = I18n.format("gui.rudazovmod.grimoire.empty");
            int color = 0x888888;
            if (bound != null && !bound.isEmpty()) {
                Optional<SpellDefinition> spell = SpellEngine.findDefinition(player, bound);
                if (spell.isPresent()) {
                    name = GuiGrimoire.shortName(spell.get());
                    color = 0xFFFFFF;
                }
            }
            Gui.drawRect(x, y + i * 12, x + 92, y + i * 12 + 11, 0x88000000);
            mc.fontRenderer.drawStringWithShadow(key + " " + name, x + 2, y + i * 12 + 2, color);
        }
    }
}
