package com.poleesteel.rudazovmod.magic;

import com.poleesteel.rudazovmod.capabilities.ActiveSpiritProvider;
import com.poleesteel.rudazovmod.capabilities.IActiveSpirit;
import com.poleesteel.rudazovmod.network.PacketHandler;
import com.poleesteel.rudazovmod.network.PacketSyncMana;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class MagicEventsHandler {

    // 1. Прикрепляем чакры к каждому игроку при спавне
    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            event.addCapability(new ResourceLocation("rudazovmod", "active_spirit"), new ActiveSpiritProvider());
        }
    }

    // 2. Сердцебиение магии: регенерация маны каждый тик на сервере
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!event.player.world.isRemote && event.phase == TickEvent.Phase.END) {
            IActiveSpirit spirit = event.player.getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
            if (spirit != null) {
                spirit.regenerate();

                // Синхронизируем с клиентом раз в 5 тиков (0.25 сек)
                if (event.player.ticksExisted % 5 == 0 && event.player instanceof EntityPlayerMP) {
                    PacketHandler.INSTANCE.sendTo(
                            new PacketSyncMana(spirit.getMana(), spirit.getMaxMana(), spirit.getChakraLevel()),
                            (EntityPlayerMP) event.player
                    );
                }
            }
        }
    }

    // 3. КРИТИЧЕСКИ ВАЖНО ДЛЯ 1.12.2: Сохраняем прокачку чакр при смерти игрока!
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            IActiveSpirit oldSpirit = event.getOriginal().getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
            IActiveSpirit newSpirit = event.getEntityPlayer().getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);

            if (oldSpirit != null && newSpirit != null) {
                newSpirit.setMana(oldSpirit.getMana());
                newSpirit.setMaxMana(oldSpirit.getMaxMana());
                newSpirit.setChakraLevel(oldSpirit.getChakraLevel());
            }
        }
    }
}