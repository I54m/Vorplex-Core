package net.vorplex.core.listeners;

import de.myzelyam.api.vanish.PlayerShowEvent;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.vorplex.core.VorplexCore;
import net.vorplex.core.util.UserFetcher;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class PlayerShow implements Listener {

    private VorplexCore plugin = VorplexCore.getInstance();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerUnvanish(PlayerShowEvent event) {
        Player player = event.getPlayer();
        if (plugin.getConfig().getBoolean("JoinMessages.enabled")) {
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
                    return;
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
            if (plugin.getConfig().getBoolean("JoinMessages.customjoinmessages.enabled")) {
                if (player.hasPermission("vorplexcore.customjoinmessages")) {
                    if (plugin.customJoinMessages.containsKey(player.getUniqueId())) {
                        String placeholder;
                        if (plugin.equippedTitles.containsKey(player.getUniqueId()) && plugin.equippedTitles.get(player.getUniqueId()) != null) {
                            placeholder = ChatColor.DARK_GRAY + "[" + ChatColor.translateAlternateColorCodes('&', plugin.equippedTitles.get(player.getUniqueId())) + ChatColor.DARK_GRAY + "]"+ ChatColor.RESET + " " +
                                    prefix + ChatColor.RESET + " " + player.getName();
                        } else {
                            placeholder = prefix + ChatColor.RESET + " " + player.getName();
                        }
                        String joinmessage = plugin.customJoinMessages.get(player.getUniqueId()).replace("%me%", placeholder).replace("\n", "");
                        for (Player all : Bukkit.getOnlinePlayers()) {
                            all.sendMessage(ChatColor.translateAlternateColorCodes('&', joinmessage));
                        }
                        return;
                    }
                }
            }
            if (plugin.getConfig().getBoolean("JoinMessages.permissionbasedjoinmessages.enabled")) {
                for (String permission : plugin.permissionJoinMessages.keySet()) {
                    if (player.hasPermission("vorplexcore.joinmessages." + permission)) {
                        String joinmessage = plugin.permissionJoinMessages.get(permission).replace("%name%", ChatColor.RESET + " " + player.getName()).replace("%prefix%", prefix);
                        if (plugin.equippedTitles.containsKey(player.getUniqueId()) && plugin.equippedTitles.get(player.getUniqueId()) != null) {
                            joinmessage = joinmessage.replace("%title%", ChatColor.DARK_GRAY + "[" + ChatColor.translateAlternateColorCodes('&', plugin.equippedTitles.get(player.getUniqueId())) + ChatColor.DARK_GRAY + "]"+ ChatColor.RESET + " ");
                        } else {
                            joinmessage = joinmessage.replace("%title%", "");
                        }
                        for (Player all : Bukkit.getOnlinePlayers()) {
                            all.sendMessage(ChatColor.translateAlternateColorCodes('&', joinmessage));
                        }
                        break;
                    }
                }
            }
        }
    }
}
