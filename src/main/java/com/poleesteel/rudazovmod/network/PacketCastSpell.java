package com.poleesteel.rudazovmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import com.poleesteel.rudazovmod.magic.AbstractSpell;
import com.poleesteel.rudazovmod.magic.SpellRegistry;

public class PacketCastSpell implements IMessage {
    private String spellId = "";

    public PacketCastSpell() {}

    public PacketCastSpell(String spellId) {
        this.spellId = spellId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.spellId = ByteBufUtils.readUTF8String(buf); // Читаем ID заклинания из сети
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // Защита от NullPointerException при отправке по сети:
        ByteBufUtils.writeUTF8String(buf, this.spellId == null ? "" : this.spellId); // Отправляем ID заклинания
    }

    public static class Handler implements IMessageHandler<PacketCastSpell, IMessage> {
        @Override
        public IMessage onMessage(PacketCastSpell message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {

                // Ищем заклинание в реестре по ID, который прислал клиент
                AbstractSpell spell = SpellRegistry.getSpell(message.spellId);
                if (spell != null) {
                    spell.execute(player); // Запускаем магию!
                }
            });
            return null;
        }
    }
}