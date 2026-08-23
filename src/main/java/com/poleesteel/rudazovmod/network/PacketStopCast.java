package com.poleesteel.rudazovmod.network;

import com.poleesteel.rudazovmod.spell.engine.SpellEngine;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Отпускание кнопки слота. Для INSTANT — no-op, для CHANNEL — endCast.
 * Слот не передаём: на игроке один активный каст.
 */
public class PacketStopCast implements IMessage {

    public PacketStopCast() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<PacketStopCast, IMessage> {
        @Override
        public IMessage onMessage(PacketStopCast message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> SpellEngine.endCast(player));
            return null;
        }
    }
}
