package net.vorplex.core.commands;

import net.vorplex.core.Main;
import net.vorplex.core.util.NameFetcher;
import net.vorplex.core.util.UUIDFetcher;
import net.vorplex.core.util.UserFetcher;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
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

public class LeaveMessageCommand implements CommandExecutor {

    private Main plugin = Main.getInstance();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player)){
            commandSender.sendMessage("You must be a player to use this commmand!");
            return false;
        }
        Player player = (Player) commandSender;
        if (!player.hasPermission("vorplexcore.customleavemessages")){
            player.sendMessage(plugin.prefix + ChatColor.RED + "You do not have permission to use this command!");
            return false;
        }
        if (strings.length < 1){
            player.sendMessage(ChatColor.LIGHT_PURPLE + "|-----" + ChatColor.DARK_PURPLE + "[" + ChatColor.LIGHT_PURPLE + "Custom Leave Messages Tips" + ChatColor.DARK_PURPLE + "]" + ChatColor.LIGHT_PURPLE + "-----|");
            player.sendMessage(ChatColor.WHITE + "- To set your custom leave message type /leavemessage set <message>");
            player.sendMessage(ChatColor.WHITE + "- You can use color codes with '&'");
            player.sendMessage(ChatColor.WHITE + "- Use %me% to insert your name, rank and title");
            return false;
        }
        if (strings[0].equalsIgnoreCase("set")) {
            if (strings.length < 2){
                player.sendMessage(plugin.prefix + ChatColor.RED + "Please provide a leave message!");
                return false;
            }
            StringBuilder leavemessage = new StringBuilder();
            for (int i = 1; i < strings.length; i++) {
                leavemessage.append(strings[i]).append(" ");
            }
            if (ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', leavemessage.toString())).replace(" ", "").length() > plugin.getConfig().getInt("LeaveMessages.customleavemessages.maxlength")) {
                player.sendMessage(plugin.prefix + ChatColor.RED + "That Leave message is too long, the maximum length for leave messages is " + plugin.getConfig().getInt("LeaveMessages.customleavemessages.maxlength") + " (excludes color codes)");
                return false;
            } else {
                try {
                    String sql = "SELECT * FROM `vorplexcore_leavemessages` WHERE UUID='" + player.getUniqueId().toString() + "';";
                    PreparedStatement stmt = plugin.connection.prepareStatement(sql);
                    ResultSet results = stmt.executeQuery();
                    String leaveMessageRaw = leavemessage.toString();
                    if (leaveMessageRaw.contains("'"))
                        leaveMessageRaw = leaveMessageRaw.replace("'", "%sinquo%");
                    if (leaveMessageRaw.contains("\""))
                        leaveMessageRaw = leaveMessageRaw.replace("\"", "%dubquo%");
                    if (leaveMessageRaw.contains("`"))
                        leaveMessageRaw = leaveMessageRaw.replace("`", "%bcktck%");
                    if (results.next()) {
                        String sql1 = "UPDATE `vorplexcore_leavemessages` SET `RawMessage`='" + leaveMessageRaw + "' WHERE UUID='" + player.getUniqueId().toString() + "';";
                        PreparedStatement stmt1 = plugin.connection.prepareStatement(sql1);
                        stmt1.executeUpdate();
                        stmt1.close();
                    } else {
                        String sql1 = "INSERT INTO `vorplexcore_leavemessages` (`UUID`, `RawMessage`)" +
                                " VALUES ('" + player.getUniqueId().toString() + "','" + leaveMessageRaw + "');";
                        PreparedStatement stmt1 = plugin.connection.prepareStatement(sql1);
                        stmt1.executeUpdate();
                        stmt1.close();
                    }
                    plugin.customLeaveMessages.put(player.getUniqueId(), leavemessage.toString());
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
                        placeholder = plugin.equippedTitles.get(player.getUniqueId()) + " " + prefix + " " + player.getName();
                    } else {
                        placeholder = prefix + " " + player.getName();
                    }
                    player.sendMessage(plugin.prefix + ChatColor.GREEN + "Set your leave message to: " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', leavemessage.toString().replace("%me%", placeholder).replace("\n", "")));
                    return true;
                } catch (SQLException sqle) {
                    sqle.printStackTrace();
                    player.sendMessage(plugin.prefix + ChatColor.RED + "An Error occurred in our database and we were unable to save your leave message, please try again later!!");
                    return false;
                }

            }
        }else if (strings[0].equalsIgnoreCase("clear")){
            if (!player.hasPermission("vorplexcore.customleavemessages.admin")){
                player.sendMessage(plugin.prefix + ChatColor.RED + "You do not have permission to use that command!");
                return false;
            }
            if (strings.length < 2){
                player.sendMessage(plugin.prefix + ChatColor.RED + "Usage: /leavemessage clear <player> - clear a player's leave message.");
                return false;
            }
            String targetuuid = null;
            Player findTarget = Bukkit.getPlayerExact(strings[0]);
            Future<String> future = null;
            ExecutorService executorService = null;
            if (findTarget != null) {
                targetuuid = findTarget.getUniqueId().toString().replace("-", "");
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
            UUID targetUUID = UUIDFetcher.formatUUID(targetuuid);
            String targetName = NameFetcher.getName(targetuuid.replace("-", ""));
            if (targetName == null) {
                targetName = strings[0];
            }
            try{
                String sql1 = "DELETE FROM `vorplexcore_leavemessages` WHERE UUID='" + targetUUID.toString() + "'";
                PreparedStatement stmt1 = plugin.connection.prepareStatement(sql1);
                stmt1.executeUpdate();
                stmt1.close();
                plugin.customLeaveMessages.remove(targetUUID);
                player.sendMessage(plugin.prefix + ChatColor.GREEN + "Cleared leave message for player: " + targetName + "!");
            }catch (SQLException sqle){
                sqle.printStackTrace();
                player.sendMessage(plugin.prefix + ChatColor.RED + "An Error occurred in our database and we were unable to clear that player's leave message!");
                return false;
            }
        }else{
            player.sendMessage(ChatColor.LIGHT_PURPLE + "|-----" + ChatColor.DARK_PURPLE + "[" + ChatColor.LIGHT_PURPLE + "Custom Leave Messages Tips" + ChatColor.DARK_PURPLE + "]" + ChatColor.LIGHT_PURPLE + "-----|");
            player.sendMessage(ChatColor.WHITE + "- To set your custom leave message type /leavemessage set <message>");
            player.sendMessage(ChatColor.WHITE + "- You can use color codes with '&'");
            player.sendMessage(ChatColor.WHITE + "- Use %me% to insert your name, rank and title");
            return false;
        }
        return false;
    }
}
