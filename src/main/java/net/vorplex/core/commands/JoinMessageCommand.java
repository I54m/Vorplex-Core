package net.vorplex.core.commands;

import net.luckperms.api.context.ContextManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.vorplex.core.Main;
import net.vorplex.core.util.NameFetcher;
import net.vorplex.core.util.UUIDFetcher;
import net.vorplex.core.util.UserFetcher;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class JoinMessageCommand implements CommandExecutor {

    private Main plugin = Main.getInstance();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player)){
            commandSender.sendMessage("You must be a player to use this commmand!");
            return false;
        }
        Player player = (Player) commandSender;
        if (!player.hasPermission("vorplexcore.customjoinmessages")){
            player.sendMessage(plugin.prefix + ChatColor.RED + "You do not have permission to use this command!");
            return false;
        }
        if (strings.length < 1){
            player.sendMessage(ChatColor.LIGHT_PURPLE + "|-----" + ChatColor.DARK_PURPLE + "[" + ChatColor.LIGHT_PURPLE + "Custom Join Messages Tips" + ChatColor.DARK_PURPLE + "]" + ChatColor.LIGHT_PURPLE + "-----|");
            player.sendMessage(ChatColor.WHITE + "- To set your custom join message type /joinmessage set <message>");
            player.sendMessage(ChatColor.WHITE + "- You can use color codes with '&'");
            player.sendMessage(ChatColor.WHITE + "- Use %me% to insert your name, rank and title");
            return false;
        }
        if (strings[0].equalsIgnoreCase("set")) {
            if (strings.length < 2){
                player.sendMessage(plugin.prefix + ChatColor.RED + "Please provide a join message!");
                return false;
            }
            StringBuilder joinmessage = new StringBuilder();
            for (int i = 1; i < strings.length; i++) {
                joinmessage.append(strings[i]).append(" ");
            }
            if (ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', joinmessage.toString())).replace(" ", "").length() > plugin.getConfig().getInt("JoinMessages.customjoinmessages.maxlength")) {
                player.sendMessage(plugin.prefix + ChatColor.RED + "That join message is too long, the maximum length for join messages is " + plugin.getConfig().getInt("JoinMessages.customjoinmessages.maxlength") + " (excludes color codes)");
                return false;
            } else {
                try {
                    String sql = "SELECT * FROM `vorplexcore_joinmessages` WHERE UUID='" + player.getUniqueId().toString() + "';";
                    PreparedStatement stmt = plugin.connection.prepareStatement(sql);
                    ResultSet results = stmt.executeQuery();
                    String joinMessageRaw = joinmessage.toString();
                    if (joinMessageRaw.contains("'"))
                        joinMessageRaw = joinMessageRaw.replace("'", "%sinquo%");
                    if (joinMessageRaw.contains("\""))
                        joinMessageRaw = joinMessageRaw.replace("\"", "%dubquo%");
                    if (joinMessageRaw.contains("`"))
                        joinMessageRaw = joinMessageRaw.replace("`", "%bcktck%");
                    if (results.next()) {
                        String sql1 = "UPDATE `vorplexcore_joinmessages` SET `RawMessage`='" + joinMessageRaw + "' WHERE UUID='" + player.getUniqueId().toString() + "';";
                        PreparedStatement stmt1 = plugin.connection.prepareStatement(sql1);
                        stmt1.executeUpdate();
                        stmt1.close();
                    } else {
                        String sql1 = "INSERT INTO `vorplexcore_joinmessages` (`UUID`, `RawMessage`)" +
                                " VALUES ('" + player.getUniqueId().toString() + "','" + joinMessageRaw + "');";
                        PreparedStatement stmt1 = plugin.connection.prepareStatement(sql1);
                        stmt1.executeUpdate();
                        stmt1.close();
                    }
                    plugin.customJoinMessages.put(player.getUniqueId(), joinmessage.toString());
                    User user = plugin.luckPermsAPI.getUserManager().getUser(player.getName());
                    if (user == null) {
                        UserFetcher userFetcher = new UserFetcher();
                        userFetcher.setUuid(player.getUniqueId());
                        ExecutorService executorService = Executors.newSingleThreadExecutor();
                        Future<User> userFuture = executorService.submit(userFetcher);
                        try {
                            user = userFuture.get(5, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            executorService.shutdown();
                            player.sendMessage(ChatColor.RED + "We were unable to fetch your permission information please try again later!");
                            return false;
                        }
                        executorService.shutdown();
                        if (user == null) {
                            throw new IllegalStateException();
                        }
                    }
                    ContextManager cm = plugin.luckPermsAPI.getContextManager();
                    QueryOptions queryOptions = cm.getQueryOptions(user).orElse(cm.getStaticQueryOptions());
                    String prefix = user.getCachedData().getMetaData(queryOptions).getPrefix();
                    if (prefix == null) prefix = "";
                    String placeholder;
                    if (plugin.equippedTitles.containsKey(player.getUniqueId())) {
                        placeholder = ChatColor.DARK_GRAY + "[" + ChatColor.translateAlternateColorCodes('&', plugin.equippedTitles.get(player.getUniqueId())) + ChatColor.DARK_GRAY + "]" + ChatColor.RESET + " " + prefix + ChatColor.RESET + " " + player.getName();
                    } else {
                        placeholder = prefix + ChatColor.RESET + " " + player.getName();
                    }
                    player.sendMessage(plugin.prefix + ChatColor.GREEN + "Set your join message to: " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', joinmessage.toString().replace("%me%", placeholder).replace("\n", "")));
                    return true;
                } catch (SQLException sqle) {
                    sqle.printStackTrace();
                    player.sendMessage(plugin.prefix + ChatColor.RED + "An Error occurred in our database and we were unable to save your join message, please try again later!!");
                    return false;
                }

            }
        }else if (strings[0].equalsIgnoreCase("clear")){
            if (!player.hasPermission("vorplexcore.customjoinmessages.admin")){
                player.sendMessage(plugin.prefix + ChatColor.RED + "You do not have permission to use that command!");
                return false;
            }
            if (strings.length < 2){
                player.sendMessage(plugin.prefix + ChatColor.RED + "Usage: /joinmessage clear <player> - clear a player's join message.");
                return false;
            }
            UUID targetuuid = null;
            Player findTarget = Bukkit.getPlayerExact(strings[0]);
            Future<UUID> future = null;
            ExecutorService executorService = null;
            if (findTarget != null) {
                targetuuid = findTarget.getUniqueId();
            } else {
                UUIDFetcher uuidFetcher = new UUIDFetcher();
                uuidFetcher.fetch(strings[1]);
                executorService = Executors.newSingleThreadExecutor();
                future = executorService.submit(uuidFetcher);
            }
            if (future != null) {
                try {
                    targetuuid = future.get(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    e.printStackTrace();
                    player.sendMessage(plugin.prefix + ChatColor.RED + "Unable to fetch player's uuid!");
                    executorService.shutdown();
                    return false;
                }
                executorService.shutdown();
            }
            if (targetuuid == null) {
                player.sendMessage(plugin.prefix + ChatColor.RED + "That is not a player's name!");
                return false;
            }
            String targetName = NameFetcher.getName(targetuuid);
            if (targetName == null) {
                targetName = strings[0];
            }
            try{
                String sql1 = "DELETE FROM `vorplexcore_joinmessages` WHERE UUID='" + targetuuid.toString() + "'";
                PreparedStatement stmt1 = plugin.connection.prepareStatement(sql1);
                stmt1.executeUpdate();
                stmt1.close();
                plugin.customJoinMessages.remove(targetuuid);
                player.sendMessage(plugin.prefix + ChatColor.GREEN + "Cleared join message for player: " + targetName + "!");
            }catch (SQLException sqle){
                sqle.printStackTrace();
                player.sendMessage(plugin.prefix + ChatColor.RED + "An Error occurred in our database and we were unable to clear that player's join message!");
                return false;
            }
        }else{
            player.sendMessage(ChatColor.LIGHT_PURPLE + "|-----" + ChatColor.DARK_PURPLE + "[" + ChatColor.LIGHT_PURPLE + "Custom Join Messages Tips" + ChatColor.DARK_PURPLE + "]" + ChatColor.LIGHT_PURPLE + "-----|");
            player.sendMessage(ChatColor.WHITE + "- To set your custom join message type /joinmessage set <message>");
            player.sendMessage(ChatColor.WHITE + "- You can use color codes with '&'");
            player.sendMessage(ChatColor.WHITE + "- Use %me% to insert your name, rank and title");
            return false;
        }
        return false;
    }
}
