package net.vorplex.core.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.vorplex.core.VorplexCore;
import net.vorplex.core.autopickup.AutoPickupConfig;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ToggleAutoPickupCommand {

    private static final VorplexCore plugin = VorplexCore.getInstance();
    private static AutoPickupConfig autoPickupConfig = plugin.getAutoPickupConfig();

    public static final LiteralCommandNode<CommandSourceStack> COMMAND_NODE = Commands.literal("toggleautopickup")
            .requires(ctx -> ctx.getSender() instanceof Player && ctx.getSender().hasPermission("vorplexcore.autoitempickup"))
            .executes(ToggleAutoPickupCommand::toggleCommand).build();

    private static int toggleCommand(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();
        final Player player = (Player) sender;
        if (autoPickupConfig.getDisabledPlayers().contains(player.getUniqueId())) {
            autoPickupConfig.getDisabledPlayers().remove(player.getUniqueId());
            player.sendRichMessage("<green>Toggled Auto Pickup On!");
        } else {
            autoPickupConfig.getDisabledPlayers().add(player.getUniqueId());
            player.sendRichMessage("<red>Toggled Auto Pickup off!");
        }
        return Command.SINGLE_SUCCESS;
    }
}
