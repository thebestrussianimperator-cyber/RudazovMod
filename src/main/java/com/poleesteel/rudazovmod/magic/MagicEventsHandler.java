package com.poleesteel.rudazovmod.magic;

import com.poleesteel.rudazovmod.capabilities.ActiveSpiritProvider;
import com.poleesteel.rudazovmod.capabilities.IActiveSpirit;
import com.poleesteel.rudazovmod.network.PacketSyncMana;
import com.poleesteel.rudazovmod.network.PacketSyncSpirit;
import com.poleesteel.rudazovmod.spell.engine.SpellEngine;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
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
            SpellEngine.tick(event.player);

            IActiveSpirit spirit = event.player.getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
            if (spirit != null) {
                spirit.regenerate();

                // Синхронизируем с клиентом раз в 5 тиков (0.25 сек)
                if (event.player.ticksExisted % 5 == 0 && event.player instanceof EntityPlayerMP) {
                    PacketSyncMana.sendTo(event.player);
                }
            }
        }
    }

    // 3. КРИТИЧЕСКИ ВАЖНО ДЛЯ 1.12.2: Сохраняем прокачку чакр при смерти игрока!
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        SpellEngine.endCast(event.getOriginal());

        IActiveSpirit oldSpirit = event.getOriginal().getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
        IActiveSpirit newSpirit = event.getEntityPlayer().getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
        if (oldSpirit == null || newSpirit == null) {
            return;
        }
        Capability.IStorage<IActiveSpirit> storage = ActiveSpiritProvider.ACTIVE_SPIRIT_CAP.getStorage();
        NBTBase nbt = storage.writeNBT(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, oldSpirit, null);
        if (nbt != null) {
            storage.readNBT(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, newSpirit, null, nbt);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerLoggedOutEvent event) {
        SpellEngine.endCast(event.player);
    }

    @SubscribeEvent
    public void onLogin(PlayerLoggedInEvent event) {
        PacketSyncSpirit.sendTo(event.player);
        PacketSyncMana.sendTo(event.player);
    }

    @SubscribeEvent
    public void onRespawn(PlayerRespawnEvent event) {
        PacketSyncSpirit.sendTo(event.player);
        PacketSyncMana.sendTo(event.player);
    }

    @SubscribeEvent
    public void onChangedDimension(PlayerChangedDimensionEvent event) {
        PacketSyncSpirit.sendTo(event.player);
        PacketSyncMana.sendTo(event.player);
    }
}