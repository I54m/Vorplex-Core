package net.vorplex.core.listeners;

//import com.earth2me.essentials.spawn.EssentialsSpawn;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.vorplex.core.VorplexCore;
import net.vorplex.core.util.NameFetcher;
import net.vorplex.core.util.UUIDFetcher;
import net.vorplex.core.util.UserFetcher;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class PlayerJoin implements Listener {

    private final VorplexCore plugin = VorplexCore.getInstance();
    public static ItemStack oxygenHelmet = new ItemStack(Material.GLASS, 1);

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoinHighest(PlayerJoinEvent event) {
        if (plugin.getConfig().getBoolean("Hub.oxygen-helmet-enabled")) {
            event.getPlayer().getInventory().setHelmet(oxygenHelmet);
            event.getPlayer().updateInventory();
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage("");
        Player player = event.getPlayer();
        UUIDFetcher.updateStoredUUID(player.getName(), player.getUniqueId());
        NameFetcher.updateStoredName(player.getUniqueId(), player.getName());
        if (plugin.getConfig().getBoolean("Announcer.enabled")) {
            if (!VorplexCore.announce) {
                VorplexCore.announce = true;
                Bukkit.getLogger().info("Detected players online! enabling announcements!");
            }
        }
        if (plugin.getConfig().getBoolean("Hub.enabled")) {
            Location location;
//            if (plugin.essentials) {
//                location = EssentialsSpawn.getPlugin(EssentialsSpawn.class).getSpawn("default");
//            } else {
                location = player.getWorld().getSpawnLocation().clone();
                location.setPitch(10.5f);
                location.setYaw(180f);
//            }
            player.teleport(location);
            String titlestring = plugin.getConfig().getString("Hub.join-title-message");
            String[] args = titlestring.split(":");
            if (args.length != 5) {
                player.sendMessage(ChatColor.RED + "Error: Title message has too many or too little arguments it must have 5 arguments!!");
                return;
            }
            args[0] = args[0].replace("%player%", player.getName());
            args[1] = args[1].replace("%player%", player.getName());
            try {
                if (plugin.old)
                    player.sendTitle(ChatColor.translateAlternateColorCodes('&', args[0]), ChatColor.translateAlternateColorCodes('&', args[1]));
                else
                    player.sendTitle(ChatColor.translateAlternateColorCodes('&', args[0]), ChatColor.translateAlternateColorCodes('&', args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
            } catch (NumberFormatException nfe) {
                player.sendMessage(ChatColor.RED + "Error: Last three arguments in the title must be numbers!!");
            }
        }
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
                if (user == null)
                    throw new IllegalStateException();
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
                            placeholder = ChatColor.DARK_GRAY + "[" + ChatColor.translateAlternateColorCodes('&', plugin.equippedTitles.get(player.getUniqueId())) + ChatColor.DARK_GRAY + "]" + ChatColor.RESET + " " +
                                    prefix + ChatColor.RESET + " " + player.getName();
                        } else {
                            placeholder = prefix + ChatColor.RESET + " " + player.getName();
                        }
                        String joinmessage = plugin.customJoinMessages.get(player.getUniqueId()).replace("%me%", placeholder).replace("\n", "");
                        event.setJoinMessage(ChatColor.translateAlternateColorCodes('&', joinmessage));
                        return;
                    }
                }
            }
            if (plugin.getConfig().getBoolean("JoinMessages.permissionbasedjoinmessages.enabled")) {
                for (String permission : plugin.permissionJoinMessages.keySet()) {
                    if (player.hasPermission("vorplexcore.joinmessages." + permission)) {
                        String joinmessage = plugin.permissionJoinMessages.get(permission).replace("%name%", ChatColor.RESET + " " + player.getName()).replace("%prefix%", prefix);
                        if (plugin.equippedTitles.containsKey(player.getUniqueId()) && plugin.equippedTitles.get(player.getUniqueId()) != null) {
                            joinmessage = joinmessage.replace("%title%", ChatColor.DARK_GRAY + "[" + ChatColor.translateAlternateColorCodes('&', plugin.equippedTitles.get(player.getUniqueId())) + ChatColor.DARK_GRAY + "]" + ChatColor.RESET + " ");
                        } else {
                            joinmessage = joinmessage.replace("%title%", "");
                        }
                        event.setJoinMessage(ChatColor.translateAlternateColorCodes('&', joinmessage));
                        break;
                    }
                }
            }
        }
//        if (plugin.getConfig().getBoolean("ViaVersion.enable-legacy-warning-on-join")) {
//            if (plugin.viaVersionApi != null)
//                if (plugin.viaVersionApi.getPlayerVersion(player.getUniqueId()) < 393)
//                    Bukkit.getScheduler().runTaskLater(plugin, () -> player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("ViaVersion.legacy-warning", "&c&lDetected legacy client version! It is recommended that you update to a 1.13+ client for the best experience!"))), 5 * 20);
//        }
    }
}
