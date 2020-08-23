package net.vorplex.core.listeners;

import net.luckperms.api.context.ContextManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.vorplex.core.Main;
import net.vorplex.core.util.UserFetcher;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class PlayerQuit implements Listener {

    private Main plugin = Main.getInstance();

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        event.setQuitMessage("");
        if(Bukkit.getOnlinePlayers().toArray().length <= 0){
            Main.announce = false;
            Bukkit.getLogger().info("Detected no players online! disabling announcements to save resources!");
        }
        Player player = event.getPlayer();
        if (plugin.getConfig().getBoolean("LeaveMessages.enabled")){
            User user = plugin.luckPermsAPI.getUserManager().getUser(player.getName());
            if (user == null) {
                UserFetcher userFetcher = new UserFetcher();
                userFetcher.setUuid(player.getUniqueId());
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                Future<User> userFuture = executorService.submit(userFetcher);
                try {
                    user = userFuture.get(5, TimeUnit.SECONDS);
                }catch (Exception e){
                    executorService.shutdown();
                    player.sendMessage(ChatColor.RED + "We were unable to fetch your permission information please try again later!");
                    return;
                }
                executorService.shutdown();
                if(user == null){
                    throw new IllegalStateException();
                }
            }
            ContextManager cm = plugin.luckPermsAPI.getContextManager();
            QueryOptions queryOptions = cm.getQueryOptions(user).orElse(cm.getStaticQueryOptions());
            String prefix = user.getCachedData().getMetaData(queryOptions).getPrefix();
            if (prefix == null) prefix = "";
            if (plugin.getConfig().getBoolean("LeaveMessages.customleavemessages.enabled")) {
                if (player.hasPermission("vorplexcore.customleavemessages")) {
                    if (plugin.customLeaveMessages.containsKey(player.getUniqueId())) {
                        String placeholder;
                        if (plugin.equippedTitles.containsKey(player.getUniqueId()) && plugin.equippedTitles.get(player.getUniqueId()) != null) {
                            placeholder = ChatColor.DARK_GRAY + "[" + ChatColor.translateAlternateColorCodes('&', plugin.equippedTitles.get(player.getUniqueId())) + ChatColor.DARK_GRAY + "]"+ ChatColor.RESET + " " +
                                    prefix + player.getName();
                        } else {
                            placeholder = prefix + player.getName();
                        }
                        String leavemessage = plugin.customLeaveMessages.get(player.getUniqueId()).replace("%me%", placeholder).replace("\n", "");
                        event.setQuitMessage(ChatColor.translateAlternateColorCodes('&', leavemessage));
                        return;
                    }
                }
            }
            if (plugin.getConfig().getBoolean("LeaveMessages.permissionbasedleavemessages.enabled")) {
                for (String permission : plugin.permissionLeaveMessages.keySet()) {
                    if (player.hasPermission("vorplexcore.leavemessages." + permission)) {
                        String leavemessage = plugin.permissionLeaveMessages.get(permission).replace("%name%", ChatColor.RESET + " " + player.getName()).replace("%prefix%", prefix);
                        if (plugin.equippedTitles.containsKey(player.getUniqueId()) && plugin.equippedTitles.get(player.getUniqueId()) != null) {
                            leavemessage = leavemessage.replace("%title%", ChatColor.DARK_GRAY + "[" + ChatColor.translateAlternateColorCodes('&', plugin.equippedTitles.get(player.getUniqueId())) + ChatColor.DARK_GRAY + "]"+ ChatColor.RESET + " ");
                        } else {
                            leavemessage = leavemessage.replace("%title%", "");
                        }
                        event.setQuitMessage(ChatColor.translateAlternateColorCodes('&', leavemessage));
                        break;
                    }
                }
            }
        }
    }
}