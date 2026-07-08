package net.vorplex.core.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.vorplex.core.VorplexCore;
import net.vorplex.core.database.StorageException;
import net.vorplex.core.util.LuckpermsUtil;
import net.vorplex.core.util.UUIDFetcher;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LeaveMessageCommand {

    private static final VorplexCore plugin = VorplexCore.getInstance();

    public static final LiteralCommandNode<CommandSourceStack> COMMAND_NODE = Commands.literal("leavemessage")
            .requires(source -> source.getSender() instanceof Player && source.getSender().hasPermission("vorplexcore.customleavemessages"))
            .then(Commands.literal("set")
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                            .executes(LeaveMessageCommand::setSelf)
                    )
                    .executes(LeaveMessageCommand::help)
            )
            .then(Commands.literal("clear")
                    .executes(LeaveMessageCommand::clearSelf)
                    .then(Commands.argument("target", StringArgumentType.string())
                            .requires(source -> source.getSender().hasPermission("vorplexcore.customleavemessages.admin"))
                            .executes(LeaveMessageCommand::clearOther)
                    )
            )
            .executes(LeaveMessageCommand::help)
            .build();

    private static int help(final CommandContext<CommandSourceStack> ctx) {
        final Player player = (Player) ctx.getSource().getSender();
        player.sendRichMessage("<light_purple>|<st>      </st><dark_purple>[</dark_purple><white>Custom Leave Messages Tips</white><dark_purple>]</dark_purple><st>     </st>|</light_purple>");
        player.sendRichMessage("<white>- To set your custom leave message type /leavemessage set <message></white>");
        player.sendRichMessage("<white>- To clear your custom leave message type /leavemessage clear</white>");
        player.sendRichMessage("<white>- Use mini message codes to color your message</white>");
        player.sendRichMessage("<white>- Use <name> to insert your name</white>");
        player.sendRichMessage("<white>- Use <prefix> to insert your rank prefix</white>");
        return Command.SINGLE_SUCCESS;
    }

    private static int setSelf(final CommandContext<CommandSourceStack> ctx) {
        final Player player = (Player) ctx.getSource().getSender();
        final String messageArg = ctx.getArgument("message", String.class);
        final String filteredMessage = messageArg.replace("<br>", "").replace("<newline>", "");
        final String strippedMessage = plugin.getBasicMM().stripTags(filteredMessage);
        if (strippedMessage.length() > plugin.getConfig().getInt("LeaveMessages.CustomLeaveMessages.maxlength", 128)) {
            player.sendRichMessage(plugin.getPrefix() + "<red>That Leave message is too long, the maximum length for leave messages is " + plugin.getConfig().getInt("LeaveMessages.CustomLeaveMessages.maxlength", 128) + " (excludes MiniMessage Tags)");
            return Command.SINGLE_SUCCESS;
        } else {
            try {
                plugin.getStorageProvider().setLeaveMessage(player.getUniqueId(), filteredMessage);
                plugin.getCustomLeaveMessagesCache().put(player.getUniqueId(), filteredMessage);

                String prefix = LuckpermsUtil.getPrefix(player);
                player.sendRichMessage(plugin.getPrefix() + "<green>Set your leave message to: ");
                player.sendMessage(plugin.getBasicMM().deserialize(plugin.getCustomLeaveMessagesCache().get(player.getUniqueId()), Placeholder.parsed("prefix", prefix), Placeholder.component("name", Component.text(player.getName()))));
                return Command.SINGLE_SUCCESS;
            } catch (StorageException se) {
                player.sendRichMessage(plugin.getPrefix() + "<red>An Error occurred and we were unable to save your leave message, please try again later!");
                logException(se, player.getName(), player.getUniqueId());
                return Command.SINGLE_SUCCESS;
            }
        }
    }

    private static int clearSelf(final CommandContext<CommandSourceStack> ctx) {
        final Player player = (Player) ctx.getSource().getSender();
        try {
            plugin.getStorageProvider().deleteLeaveMessage(player.getUniqueId());
            plugin.getCustomLeaveMessagesCache().remove(player.getUniqueId());

            player.sendRichMessage(plugin.getPrefix() + "<green>Cleared your custom leave message!");
            return Command.SINGLE_SUCCESS;
        } catch (StorageException se) {
            player.sendRichMessage(plugin.getPrefix() + "<red>An Error occurred and we were unable to clear your leave message, please try again later!");
            logException(se, player.getName(), player.getUniqueId());
            return Command.SINGLE_SUCCESS;
        }
    }

    private static int clearOther(final CommandContext<CommandSourceStack> ctx) {
        final Player player = (Player) ctx.getSource().getSender();
        final String targetName = ctx.getArgument("target", String.class);
        if (targetName.contains("\"") || targetName.contains(" ")) {
            player.sendRichMessage(plugin.getPrefix() + "<red>That is an invalid player's name!");
            return Command.SINGLE_SUCCESS;
        }
        UUID targetUUID = null;
        try {
            targetUUID = UUIDFetcher.fetchUUID(targetName);
            if (targetUUID == null) {
                player.sendRichMessage(plugin.getPrefix() + "<red>Could not find player: " + targetName + "!");
                return Command.SINGLE_SUCCESS;
            }
            plugin.getStorageProvider().deleteLeaveMessage(targetUUID);
            plugin.getCustomLeaveMessagesCache().remove(targetUUID);

            player.sendRichMessage(plugin.getPrefix() + "<green>Cleared " + targetName + "'s custom leave message!");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            player.sendRichMessage(plugin.getPrefix() + "<red>An Error occurred and we were unable to clear " + targetName + "'s leave message, please try again later!");
            logException(e, targetName, targetUUID);
            return Command.SINGLE_SUCCESS;
        }
    }

    private static void logException(Exception e, String playerName, UUID playerUUID) {
        plugin.getComponentLogger().error("An Error was encountered while trying to delete a custom leave message for: {} (UUID: {})", playerName, playerUUID == null ? "null" : playerUUID);
        plugin.getComponentLogger().error("Error message: {}", e.getMessage());
        if (e.getCause() != null)
            plugin.getComponentLogger().error("Cause message: {}", e.getCause().getMessage());
    }
}
