package net.vorplex.core.commands;

import net.vorplex.core.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class BuyCommand implements CommandExecutor {

    private final Main plugin = Main.getInstance();

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        String rawMessage = plugin.getConfig().getString("buycommand.message");
        if (rawMessage == null)
            commandSender.sendMessage("discordcommand.message is null! or config was not loaded correctly!");
        else commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', rawMessage));
        return true;
    }
}
