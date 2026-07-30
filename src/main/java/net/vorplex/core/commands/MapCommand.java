package net.vorplex.core.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.vorplex.core.VorplexCore;

public class MapCommand {

    public static final LiteralCommandNode<CommandSourceStack> COMMAND_NODE = Commands.literal("map")
            .executes((ctx) -> {
                String rawMessage = VorplexCore.getInstance().getConfig().getString("MapCommand.message");
                if (rawMessage == null)
                    ctx.getSource().getSender().sendMessage("MapCommand.message is null! or config was not loaded correctly!");
                else ctx.getSource().getSender().sendRichMessage(rawMessage);
                return Command.SINGLE_SUCCESS;
            }).build();
}
