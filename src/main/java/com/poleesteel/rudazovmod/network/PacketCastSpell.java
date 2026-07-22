package com.poleesteel.rudazovmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import com.poleesteel.rudazovmod.capabilities.ActiveSpiritProvider;
import com.poleesteel.rudazovmod.capabilities.IActiveSpirit;
import com.poleesteel.rudazovmod.magic.AbstractSpell;
import com.poleesteel.rudazovmod.magic.SpellRegistry;

public class PacketCastSpell implements IMessage {
    private int slotIndex;

    public PacketCastSpell() {}

    public PacketCastSpell(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.slotIndex = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.slotIndex);
    }

    public static class Handler implements IMessageHandler<PacketCastSpell, IMessage> {
        @Override
        public IMessage onMessage(PacketCastSpell message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {

                IActiveSpirit spirit = player.getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
                if (spirit != null) {
                    // СЕРВЕР сам проверяет, что лежит в этом слоте и изучено ли оно!
                    String spellId = spirit.getBoundSpell(message.slotIndex);

                    if (spellId != null && !spellId.isEmpty() && spirit.isSpellUnlocked(spellId)) {
                        AbstractSpell spell = SpellRegistry.getSpell(spellId);
                        if (spell != null) {
                            spell.execute(player); // Запускаем магию!
                        }
                    }
                }
            });
            return null;
        }
    }
}