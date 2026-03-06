package net.vorplex.core.listeners;

//import com.earth2me.essentials.spawn.EssentialsSpawn;

import lombok.NonNull;
import net.vorplex.core.VorplexCore;
import net.vorplex.core.util.Debug;
import net.vorplex.core.util.NameFetcher;
import net.vorplex.core.util.UUIDFetcher;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;

public class PlayerJoin implements Listener {

    private final VorplexCore plugin = VorplexCore.getInstance();
    public static ItemStack oxygenHelmet = new ItemStack(Material.GLASS, 1);

//    @EventHandler(priority = EventPriority.HIGHEST)
//    public void onPlayerJoinHighest(PlayerJoinEvent event) {
//        if (plugin.getConfig().getBoolean("Hub.oxygen-helmet-enabled")) {
//            event.getPlayer().getInventory().setHelmet(oxygenHelmet);
//            event.getPlayer().updateInventory();
//        }
//    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
//        event.setJoinMessage("");
        final Player player = event.getPlayer();
        UUIDFetcher.updateStoredUUID(player.getName(), player.getUniqueId());
        NameFetcher.updateStoredName(player.getUniqueId(), player.getName());
        if (plugin.getConfig().getBoolean("SafeLogin.enabled", true)) {
            if (!plugin.getConfig().getList("SafeLogin.AllowedWorlds", new ArrayList<>(Collections.singleton("world"))).contains(player.getWorld().getName())) {
                Debug.log(player.getName() + " is not in an allowed world for safe-login");
                return;
            }
            if (isLocationUnSafe(player, player.getLocation())) {
                Debug.log(player.getName() + "'s login location was deemed unsafe applying buffs and scheduling tp...");
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 10));
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 10));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100, 10));
                final Location safeLocation = player.getWorld().getHighestBlockAt(player.getLocation()).getLocation();
                if (safeLocation.getBlock().getType() == Material.LAVA) {
                    Debug.log("Safe location had lava placing stone");
                    safeLocation.getBlock().setType(Material.STONE);
                }
                safeLocation.add(0.5, 1, 0.5);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    Debug.log("Teleporting " + player.getName() + " and setting velocity to 0");
                    player.setVelocity(new Vector(0, 0, 0));
                    player.teleport(safeLocation);
                    player.sendRichMessage(plugin.getConfig().getString("SafeLogin.TeleportMessage", "<red>You joined in an unsafe location, You have been teleported to the highest safe block!"));
                    Debug.log(player.getName() + " was teleported successfully");
                }, 60);
            }
        }

//        if (plugin.getConfig().getBoolean("Hub.enabled")) {
//            Location location;
////            if (plugin.essentials) {
////                location = EssentialsSpawn.getPlugin(EssentialsSpawn.class).getSpawn("default");
////            } else {
//                location = player.getWorld().getSpawnLocation().clone();
//                location.setPitch(10.5f);
//                location.setYaw(180f);
////            }
//            player.teleport(location);
//            String titlestring = plugin.getConfig().getString("Hub.join-title-message");
//            String[] args = titlestring.split(":");
//            if (args.length != 5) {
//                player.sendMessage(ChatColor.RED + "Error: Title message has too many or too little arguments it must have 5 arguments!!");
//                return;
//            }
//            args[0] = args[0].replace("%player%", player.getName());
//            args[1] = args[1].replace("%player%", player.getName());
//            try {
//                if (plugin.old)
//                    player.sendTitle(ChatColor.translateAlternateColorCodes('&', args[0]), ChatColor.translateAlternateColorCodes('&', args[1]));
//                else
//                    player.sendTitle(ChatColor.translateAlternateColorCodes('&', args[0]), ChatColor.translateAlternateColorCodes('&', args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
//            } catch (NumberFormatException nfe) {
//                player.sendMessage(ChatColor.RED + "Error: Last three arguments in the title must be numbers!!");
//            }
//        }
//        if (plugin.getConfig().getBoolean("JoinMessages.enabled")) {
//            User user = plugin.luckPermsAPI.getUserManager().getUser(player.getName());
//            if (user == null) {
//                UserFetcher userFetcher = new UserFetcher();
//                userFetcher.setUuid(player.getUniqueId());
//                ExecutorService executorService = Executors.newSingleThreadExecutor();
//                Future<User> userFuture = executorService.submit(userFetcher);
//                try {
//                    user = userFuture.get(5, TimeUnit.SECONDS);
//                } catch (Exception e) {
//                    executorService.shutdown();
//                    player.sendMessage(ChatColor.RED + "We were unable to fetch your permission information please try again later!");
//                    return;
//                }
//                executorService.shutdown();
//                if (user == null)
//                    throw new IllegalStateException();
//            }
//            ContextManager cm = plugin.luckPermsAPI.getContextManager();
//            QueryOptions queryOptions = cm.getQueryOptions(user).orElse(cm.getStaticQueryOptions());
//            String prefix = user.getCachedData().getMetaData(queryOptions).getPrefix();
//            if (prefix == null) prefix = "";
//            if (plugin.getConfig().getBoolean("JoinMessages.customjoinmessages.enabled")) {
//                if (player.hasPermission("vorplexcore.customjoinmessages")) {
//                    if (plugin.customJoinMessages.containsKey(player.getUniqueId())) {
//                        String placeholder;
//                        if (plugin.equippedTitles.containsKey(player.getUniqueId()) && plugin.equippedTitles.get(player.getUniqueId()) != null) {
//                            placeholder = ChatColor.DARK_GRAY + "[" + ChatColor.translateAlternateColorCodes('&', plugin.equippedTitles.get(player.getUniqueId())) + ChatColor.DARK_GRAY + "]" + ChatColor.RESET + " " +
//                                    prefix + ChatColor.RESET + " " + player.getName();
//                        } else {
//                            placeholder = prefix + ChatColor.RESET + " " + player.getName();
//                        }
//                        String joinmessage = plugin.customJoinMessages.get(player.getUniqueId()).replace("%me%", placeholder).replace("\n", "");
//                        event.setJoinMessage(ChatColor.translateAlternateColorCodes('&', joinmessage));
//                        return;
//                    }
//                }
//            }
//            if (plugin.getConfig().getBoolean("JoinMessages.permissionbasedjoinmessages.enabled")) {
//                for (String permission : plugin.permissionJoinMessages.keySet()) {
//                    if (player.hasPermission("vorplexcore.joinmessages." + permission)) {
//                        String joinmessage = plugin.permissionJoinMessages.get(permission).replace("%name%", ChatColor.RESET + " " + player.getName()).replace("%prefix%", prefix);
//                        if (plugin.equippedTitles.containsKey(player.getUniqueId()) && plugin.equippedTitles.get(player.getUniqueId()) != null) {
//                            joinmessage = joinmessage.replace("%title%", ChatColor.DARK_GRAY + "[" + ChatColor.translateAlternateColorCodes('&', plugin.equippedTitles.get(player.getUniqueId())) + ChatColor.DARK_GRAY + "]" + ChatColor.RESET + " ");
//                        } else {
//                            joinmessage = joinmessage.replace("%title%", "");
//                        }
//                        event.setJoinMessage(ChatColor.translateAlternateColorCodes('&', joinmessage));
//                        break;
//                    }
//                }
//            }
//        }
    }


    /**
     * Check a player's location to see if it is safe
     *
     * @param player       the player to check the safe location for
     * @param feetLocation the foot location of the player
     * @return true if the location is considered unsafe, else false
     */
    public boolean isLocationUnSafe(final @NonNull Player player, final @NonNull Location feetLocation) {
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            Debug.log(player.getName() + " is in creative or spectator, location is considered safe for them");
            return false;
        }
        final Location headLocation = feetLocation.add(0, 1, 0);
        final Block standingBlock = feetLocation.subtract(0, 1, 0).getBlock();
        if (feetLocation.getBlock().isSuffocating() && headLocation.getBlock().isSuffocating()) {
            Debug.log(player.getName() + "'s head AND feet location was in a block that causes suffocation - NOT SAFE");
            return true;
        } else if (feetLocation.getBlock().getType() == Material.LAVA || headLocation.getBlock().getType() == Material.LAVA) {
            Debug.log(player.getName() + "'s head OR feet location was in lava - NOT SAFE");
            return true;
        } else if (!player.isFlying() && !standingBlock.isSolid() && !standingBlock.isLiquid()) {
            Debug.log(player.getName() + " was not flying and was in the air - NOT SAFE");
            return true;
        } else {
            Debug.log("All checks passed for " + player.getName() + " - SAFE!");
            return false;
        }
    }
}
