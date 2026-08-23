package com.poleesteel.rudazovmod.network;

import com.poleesteel.rudazovmod.spell.engine.SpellBook;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * C→S: привязка уже существующего определения к слоту. Сервер проверяет ownsSpell.
 */
public class PacketBindSpell implements IMessage {

    private int slot;
    private String spellId = "";

    public PacketBindSpell() {}

    public PacketBindSpell(int slot, String spellId) {
        this.slot = slot;
        this.spellId = spellId == null ? "" : spellId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.slot = buf.readInt();
        this.spellId = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.slot);
        ByteBufUtils.writeUTF8String(buf, this.spellId);
    }

    public static class Handler implements IMessageHandler<PacketBindSpell, IMessage> {
        @Override
        public IMessage onMessage(PacketBindSpell message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (message.spellId == null || message.spellId.length() > 128) {
                    return;
                }
                SpellBook.bind(player, message.slot, message.spellId);
            });
            return null;
        }
    }
}
