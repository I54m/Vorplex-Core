package net.vorplex.core.chat;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminChatCommand {

    public static final LiteralCommandNode<CommandSourceStack> COMMAND_NODE = Commands.literal("adminchat")
            .requires(ctx -> ctx.getSender() instanceof Player)
            .requires(ctx -> ctx.getSender().hasPermission("vorplexcore.adminchat"))
            .executes(AdminChatCommand::toggleCommand)
            .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes((ctx -> {
                        ChatUtils.sendAdminChat((Player) ctx.getSource().getSender(), StringArgumentType.getString(ctx, "message"));
                        return Command.SINGLE_SUCCESS;
                    }))
            ).build();

    private static int toggleCommand(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();
        final Player player = (Player) sender;
        if (ChatUtils.isAdminChatOn(player)) {
            ChatUtils.adminChat.remove(player);
            player.sendRichMessage("<red>Toggled Admin Chat off!");
        } else {
            if (ChatUtils.isStaffChatOn(player)) {
                ChatUtils.staffChat.remove(player);
                player.sendRichMessage("<red>Toggled Staff Chat off!");
            }
            ChatUtils.adminChat.add(player);
            player.sendRichMessage("<green>Toggled Admin Chat on!");
        }
        return Command.SINGLE_SUCCESS;
    }
}
