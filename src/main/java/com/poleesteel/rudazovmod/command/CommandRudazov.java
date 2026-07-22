package com.poleesteel.rudazovmod.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import com.poleesteel.rudazovmod.capabilities.ActiveSpiritProvider;
import com.poleesteel.rudazovmod.capabilities.IActiveSpirit;
import com.poleesteel.rudazovmod.magic.AbstractSpell;
import com.poleesteel.rudazovmod.magic.SpellRegistry;

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
        if (!(sender instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) sender;
        IActiveSpirit spirit = player.getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
        if (spirit == null) return;

        if (args.length == 0) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "Использование: " + getUsage(sender)));
            return;
        }

        // КОМАНДА: /rudazovmod unlock <all | spell_id>
        if (args[0].equalsIgnoreCase("unlock") && args.length >= 2) {
            if (args[1].equalsIgnoreCase("all")) {
                for (AbstractSpell spell : SpellRegistry.getAllSpells()) {
                    spirit.unlockSpell(spell.getId().toString());
                }
                player.sendMessage(new TextComponentString(TextFormatting.GREEN + "Вы изучили ВСЕ заклинания!"));
            } else {
                String spellId = args[1];
                if (SpellRegistry.getSpell(spellId) != null) {
                    spirit.unlockSpell(spellId);
                    player.sendMessage(new TextComponentString(TextFormatting.GREEN + "Заклинание изучено: " + spellId));
                } else {
                    player.sendMessage(new TextComponentString(TextFormatting.RED + "Заклинание не найдено в реестре: " + spellId));
                }
            }
        }
        // КОМАНДА: /rudazovmod bind <слот 1-4> <spell_id>
        else if (args[0].equalsIgnoreCase("bind") && args.length >= 3) {
            try {
                int slot = Integer.parseInt(args[1]) - 1; // Переводим 1-4 в массив 0-3
                String spellId = args[2];

                if (!spirit.isSpellUnlocked(spellId)) {
                    player.sendMessage(new TextComponentString(TextFormatting.RED + "Сначала изучите это заклинание!"));
                    return;
                }

                spirit.bindSpell(slot, spellId);
                player.sendMessage(new TextComponentString(TextFormatting.GOLD + "Слот " + (slot + 1) + " привязан к: " + spellId));
            } catch (NumberFormatException e) {
                player.sendMessage(new TextComponentString(TextFormatting.RED + "Номер слота должен быть числом от 1 до 4!"));
            }
        }
    }
}