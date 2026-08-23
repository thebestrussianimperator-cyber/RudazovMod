package com.poleesteel.rudazovmod.client.input;

import com.poleesteel.rudazovmod.client.gui.GuiGrimoire;
import com.poleesteel.rudazovmod.network.PacketCastSpell;
import com.poleesteel.rudazovmod.network.PacketHandler;
import com.poleesteel.rudazovmod.network.PacketStopCast;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@Mod.EventBusSubscriber(Side.CLIENT)
public class KeyBindHandler {

    public static final KeyBinding SPELL_SLOT_1 = new KeyBinding("key.rudazovmod.slot1", Keyboard.KEY_Z, "key.categories.rudazovmod");
    public static final KeyBinding SPELL_SLOT_2 = new KeyBinding("key.rudazovmod.slot2", Keyboard.KEY_X, "key.categories.rudazovmod");
    public static final KeyBinding SPELL_SLOT_3 = new KeyBinding("key.rudazovmod.slot3", Keyboard.KEY_C, "key.categories.rudazovmod");
    public static final KeyBinding SPELL_SLOT_4 = new KeyBinding("key.rudazovmod.slot4", Keyboard.KEY_V, "key.categories.rudazovmod");
    public static final KeyBinding OPEN_GRIMOIRE = new KeyBinding("key.rudazovmod.grimoire", Keyboard.KEY_G, "key.categories.rudazovmod");

    public static final KeyBinding[] SLOTS = {SPELL_SLOT_1, SPELL_SLOT_2, SPELL_SLOT_3, SPELL_SLOT_4};

    /** -1 = ничего не зажато. Один слот за раз. */
    private static int heldSlot = -1;

    public static void init() {
        for (KeyBinding key : SLOTS) {
            ClientRegistry.registerKeyBinding(key);
        }
        ClientRegistry.registerKeyBinding(OPEN_GRIMOIRE);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null && mc.world != null && mc.currentScreen == null && OPEN_GRIMOIRE.isPressed()) {
            mc.displayGuiScreen(new GuiGrimoire());
        }

        if (mc.player == null || mc.world == null || mc.isGamePaused() || mc.currentScreen != null) {
            if (heldSlot >= 0) {
                PacketHandler.INSTANCE.sendToServer(new PacketStopCast());
                heldSlot = -1;
            }
            return;
        }

        int down = -1;
        for (int i = 0; i < SLOTS.length; i++) {
            if (SLOTS[i].isKeyDown()) {
                down = i;
                break;
            }
        }

        if (down == heldSlot) {
            return;
        }
        if (heldSlot >= 0) {
            PacketHandler.INSTANCE.sendToServer(new PacketStopCast());
        }
        if (down >= 0) {
            PacketHandler.INSTANCE.sendToServer(new PacketCastSpell(down));
        }
        heldSlot = down;
    }
}
