package net.vorplex.core.commands;

import net.vorplex.core.VorplexCore;
import net.vorplex.core.autorestart.Config;
import net.vorplex.core.autorestart.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class AutoRe implements CommandExecutor {
    private VorplexCore plugin = VorplexCore.getInstance();


    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (s.equalsIgnoreCase("autorestart") || s.equalsIgnoreCase("autore") || s.equalsIgnoreCase("re")){
            if (!sender.hasPermission("core.autorestart.admin")){
                sender.sendMessage(ChatColor.RED + "You do not have permission to use restart commands!");
                return false;
            }
            if (!(strings.length >= 1)){
                sender.sendMessage(ChatColor.WHITE + "" + ChatColor.STRIKETHROUGH + "|----------" + VorplexCore.PREFIX.replace(" ", "") + ChatColor.WHITE + "" + ChatColor.STRIKETHROUGH + "----------|\n"
                        + ChatColor.WHITE + "  /autorestart reload " + ChatColor.GRAY + "- Reload configuration.\n"
                        + ChatColor.WHITE + "  /autorestart now " + ChatColor.GRAY + "- Reboot the server in 1 minute.\n"
                        + ChatColor.WHITE + "  /autorestart queue " + ChatColor.GRAY + "- Queue an auto reboot.\n"
                        + ChatColor.WHITE + "  /autorestart stop " + ChatColor.GRAY + "- Stop and remove all reboots from the queue.\n"
                        + ChatColor.WHITE + "  /autorestart start " + ChatColor.GRAY + "- Add all auto reboots to the queue.\n"
                        + ChatColor.WHITE + "  /autorestart time " + ChatColor.GRAY + "- Get the time to the next auto reboot.\n"
                        + ChatColor.WHITE + "  /autorestart help " + ChatColor.GRAY + "- View this help menu.\n"
                );
                return false;
            }
            if (strings[0].equalsIgnoreCase("now")){
                Scheduler.stop();
                Config config = new Config();
                Scheduler.scheduleNotify(config, 600);
                sender.sendMessage(VorplexCore.PREFIX + ChatColor.GREEN + "Auto Rebooting in 30 seconds!");
                for (Player players : Bukkit.getOnlinePlayers()) {
                    if (config.notifyChatEnabled)
                        players.sendMessage(VorplexCore.PREFIX + ChatColor.GREEN + "Server will now auto reboot in 30 seconds!");
                    if (config.notifyTitleEnabled) {
                        if (plugin.old) players.sendTitle(ChatColor.GREEN + "Server will now auto reboot in", ChatColor.GREEN + "30 seconds!");
                        else players.sendTitle(ChatColor.GREEN + "Server will now auto reboot in", ChatColor.GREEN + "30 seconds!", 40, 40, 40);
                    }
                    players.playSound(players.getLocation(), config.notifySound, 1.0f, 1.0f);
                }
                Scheduler.tasks.add(Bukkit.getScheduler().runTaskLater(VorplexCore.getInstance(), () -> {
                    for (Player players : Bukkit.getOnlinePlayers()) {
                        if (config.notifyChatEnabled)
                            players.sendMessage(VorplexCore.PREFIX + ChatColor.GREEN + "Server is now auto rebooting!");
                        if (config.notifyTitleEnabled) {
                            if (plugin.old) players.sendTitle(ChatColor.GREEN + "Server is now auto rebooting!", "");
                            else players.sendTitle(ChatColor.GREEN + "Server is now auto rebooting!", "", 40, 40, 40);
                        }
                        players.playSound(players.getLocation(), config.notifySound, 1.0f, 1.0f);
                    }
                    Bukkit.spigot().restart();
                }, 600));
                return true;
            }else if (strings[0].equalsIgnoreCase("queue")){
                if (!(strings.length >= 3)){
                    sender.sendMessage(VorplexCore.PREFIX + ChatColor.RED + "Queue an auto reboot. Usage:");
                    sender.sendMessage(VorplexCore.PREFIX + ChatColor.RED + "/autorestart queue <amount of time> <minutes|hours>");
                    return false;
                }
                int amountOfTime;
                try{
                    amountOfTime = Integer.parseInt(strings[1]);
                }catch (NumberFormatException nfe){
                    sender.sendMessage(VorplexCore.PREFIX + ChatColor.RED + strings[1] + " is not a valid amount of time!");
                    return false;
                }
                Config config = new Config();
                config.schedule = new ArrayList<>();
                if (strings[2].equalsIgnoreCase("minutes") || strings[2].equalsIgnoreCase("minute")){
                    if (amountOfTime > 59){
                        sender.sendMessage(VorplexCore.PREFIX + ChatColor.RED + "Minutes must be between 0 and 59");
                        return false;
                    }
                    config.schedule.add("0 0/" + amountOfTime + " * 1/1 * ? *");
                }else if (strings[2].equalsIgnoreCase("hours") || strings[2].equalsIgnoreCase("hour")){
                    if (amountOfTime > 23){
                        sender.sendMessage(VorplexCore.PREFIX + ChatColor.RED + "Hours must be between 0 and 23");
                        return false;
                    }
                    config.schedule.add("0 0 0/" + amountOfTime + " 1/1 * ? *");
                }else{
                    sender.sendMessage(VorplexCore.PREFIX + ChatColor.RED + "You can only queue a reboot between 1 minute and 23 hours!");
                    return false;
                }
                sender.sendMessage(VorplexCore.PREFIX + ChatColor.GREEN + "Queued auto reboot for " + strings[1] + " " + strings[2] + " from now!");
                Scheduler.stop();
                Scheduler.start(config);
                for (Player players : Bukkit.getOnlinePlayers()) {
                    if (config.notifyChatEnabled)
                        players.sendMessage(VorplexCore.PREFIX + ChatColor.GREEN + "Server will now auto reboot: " + strings[1] + " " + strings[2] + " from now!");
                    if (config.notifyTitleEnabled) {
                        if (plugin.old) players.sendTitle(ChatColor.GREEN + "Server will now auto reboot: ", strings[1] + " " + strings[2] + " from now!");
                        else players.sendTitle(ChatColor.GREEN + "Server will now auto reboot: ", strings[1] + " " + strings[2] + " from now!", 40, 40, 40);
                    }
                    players.playSound(players.getLocation(), config.notifySound, 1.0f, 1.0f);
                }
                return true;
            }else if (strings[0].equalsIgnoreCase("stop")){
                if (!Scheduler.tasks.isEmpty()) {
                    for (Player players : Bukkit.getOnlinePlayers()) {
                        if (VorplexCore.getInstance().config.notifyChatEnabled)
                            players.sendMessage(VorplexCore.PREFIX + ChatColor.GREEN + "Reboot was aborted!");
                        if (VorplexCore.getInstance().config.notifyTitleEnabled) {
                            if (plugin.old)
                                players.sendTitle(ChatColor.GREEN + "Reboot was aborted!", "");
                            else players.sendTitle(ChatColor.GREEN + "Reboot was aborted!", "", 40, 40, 40);
                        }
                        players.playSound(players.getLocation(), VorplexCore.getInstance().config.notifySound, 1.0f, 1.0f);
                    }
                    sender.sendMessage(VorplexCore.PREFIX + ChatColor.GREEN + "Stopped all auto reboot tasks!");
                    Scheduler.stop();
                    return true;
                }else {
                    sender.sendMessage(VorplexCore.PREFIX + ChatColor.RED + "There are no auto reboot tasks running!");
                    return false;
                }
            }else if (strings[0].equalsIgnoreCase("start")){
                if (Scheduler.tasks.isEmpty()) {
                    sender.sendMessage(VorplexCore.PREFIX + ChatColor.GREEN + "Started all auto reboot tasks!");
                    Scheduler.start(new Config());
                    return true;
                }else {
                    sender.sendMessage(VorplexCore.PREFIX + ChatColor.RED + "There are auto reboot tasks already running!");
                    return false;
                }
            }else if (strings[0].equalsIgnoreCase("time")){
                ZonedDateTime nextTime = Scheduler.nextTime;
                String date = ChatColor.RED + "No auto reboot tasks are currently scheduled!";
                if (nextTime != null) date = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss (O)").format(nextTime);
                sender.sendMessage(VorplexCore.PREFIX + ChatColor.GREEN + "Next restart: " + date);
                return true;
            }else if (strings[0].equalsIgnoreCase("reload")) {
                Scheduler.stop();
                Scheduler.start(new Config());
                sender.sendMessage(VorplexCore.PREFIX + ChatColor.GREEN + "Reloaded the config!");
                return true;
            }else {
                sender.sendMessage(ChatColor.WHITE + "" + ChatColor.STRIKETHROUGH + "|----------" + VorplexCore.PREFIX.replace(" ", "") + ChatColor.WHITE + "" + ChatColor.STRIKETHROUGH + "----------|\n"
                        + ChatColor.WHITE + "  /autorestart reload " + ChatColor.GRAY + "- Reload configuration.\n"
                        + ChatColor.WHITE + "  /autorestart now " + ChatColor.GRAY + "- Reboot the server now.\n"
                        + ChatColor.WHITE + "  /autorestart queue " + ChatColor.GRAY + "- Queue an auto reboot.\n"
                        + ChatColor.WHITE + "  /autorestart stop " + ChatColor.GRAY + "- Stop and remove all reboots from the queue.\n"
                        + ChatColor.WHITE + "  /autorestart start " + ChatColor.GRAY + "- Add all auto reboots to the queue.\n"
                        + ChatColor.WHITE + "  /autorestart time " + ChatColor.GRAY + "- Get the time to the next auto reboot.\n"
                        + ChatColor.WHITE + "  /autorestart help " + ChatColor.GRAY + "- View this help menu.\n"


                );
                return false;
            }
        }
        return false;
    }
}