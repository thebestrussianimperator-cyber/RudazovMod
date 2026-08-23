package com.poleesteel.rudazovmod.network;

import com.poleesteel.rudazovmod.capabilities.ActiveSpiritProvider;
import com.poleesteel.rudazovmod.capabilities.IActiveSpirit;
import com.poleesteel.rudazovmod.spell.api.SpellDefinition;
import com.poleesteel.rudazovmod.spell.engine.SpellEngine;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.Optional;

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
                if (message.slotIndex < 0 || message.slotIndex > 3) {
                    return;
                }
                IActiveSpirit spirit = player.getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
                if (spirit == null) {
                    return;
                }
                String spellId = spirit.getBoundSpell(message.slotIndex);
                if (spellId == null || spellId.isEmpty()) {
                    return;
                }
                Optional<SpellDefinition> spell = SpellEngine.findDefinition(player, spellId);
                if (!spell.isPresent()) {
                    return;
                }
                if (!spirit.ownsSpell(spellId) && !spirit.ownsSpell(spell.get().id().toString())) {
                    return;
                }
                SpellEngine.startCast(player, spell.get());
            });
            return null;
        }
    }
}
