package com.poleesteel.rudazovmod.command;

import com.poleesteel.rudazovmod.capabilities.ActiveSpiritProvider;
import com.poleesteel.rudazovmod.capabilities.IActiveSpirit;
import com.poleesteel.rudazovmod.spell.api.SpellDefinition;
import com.poleesteel.rudazovmod.spell.engine.SpellRegistry;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.Optional;

public class CommandRudazov extends CommandBase {

    @Override
    public String getName() {
        return "rudazovmod";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/rudazovmod <unlock|bind> <args>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) sender;
        IActiveSpirit spirit = player.getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
        if (spirit == null) {
            return;
        }

        if (args.length == 0) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "Использование: " + getUsage(sender)));
            return;
        }

        if (args[0].equalsIgnoreCase("unlock") && args.length >= 2) {
            if (args[1].equalsIgnoreCase("all")) {
                for (SpellDefinition spell : SpellRegistry.all()) {
                    spirit.unlockSpell(spell.id().toString());
                }
                player.sendMessage(new TextComponentString(TextFormatting.GREEN + "Вы изучили ВСЕ заклинания!"));
            } else {
                Optional<SpellDefinition> spell = SpellRegistry.get(args[1]);
                if (spell.isPresent()) {
                    String id = spell.get().id().toString();
                    spirit.unlockSpell(id);
                    player.sendMessage(new TextComponentString(TextFormatting.GREEN + "Заклинание изучено: " + id));
                } else {
                    player.sendMessage(new TextComponentString(TextFormatting.RED + "Заклинание не найдено в реестре: " + args[1]));
                }
            }
        } else if (args[0].equalsIgnoreCase("bind") && args.length >= 3) {
            try {
                int slot = Integer.parseInt(args[1]) - 1;
                Optional<SpellDefinition> spell = SpellRegistry.get(args[2]);
                if (!spell.isPresent()) {
                    player.sendMessage(new TextComponentString(TextFormatting.RED + "Заклинание не найдено в реестре: " + args[2]));
                    return;
                }
                String id = spell.get().id().toString();
                if (!spirit.isSpellUnlocked(id) && !spirit.isSpellUnlocked(args[2])) {
                    player.sendMessage(new TextComponentString(TextFormatting.RED + "Сначала изучите это заклинание!"));
                    return;
                }
                spirit.bindSpell(slot, id);
                player.sendMessage(new TextComponentString(TextFormatting.GOLD + "Слот " + (slot + 1) + " привязан к: " + id));
            } catch (NumberFormatException e) {
                player.sendMessage(new TextComponentString(TextFormatting.RED + "Номер слота должен быть числом от 1 до 4!"));
            }
        }
    }
}
