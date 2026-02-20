package net.vorplex.core.chat;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StaffChatCommand {

    public static final LiteralCommandNode<CommandSourceStack> COMMAND_NODE = Commands.literal("staffchat")
            .requires(ctx -> ctx.getSender() instanceof Player)
            .requires(ctx -> ctx.getSender().hasPermission("vorplexcore.staffchat"))
            .executes(StaffChatCommand::toggleCommand)
            .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes((ctx) -> {
                        ChatUtils.sendStaffChat((Player) ctx.getSource().getSender(), StringArgumentType.getString(ctx, "message"));
                        return Command.SINGLE_SUCCESS;
                    })
            ).build();

    private static int toggleCommand(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();
        final Player player = (Player) sender;
        if (ChatUtils.isStaffChatOn(player)) {
            ChatUtils.staffChat.remove(player);
            player.sendRichMessage("<red>Toggled Staff Chat off!");
        } else {
            if (ChatUtils.isAdminChatOn(player)) {
                ChatUtils.adminChat.remove(player);
                player.sendRichMessage("<red>Toggled Admin Chat off!");
            }
            ChatUtils.staffChat.add(player);
            player.sendRichMessage("<green>Toggled Staff Chat on!");
        }
        return Command.SINGLE_SUCCESS;
    }

}
