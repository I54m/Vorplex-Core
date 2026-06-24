package net.vorplex.core.listeners;

import de.myzelyam.api.vanish.PlayerShowEvent;
import de.myzelyam.api.vanish.VanishAPI;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.vorplex.core.VorplexCore;
import net.vorplex.core.util.Debug;
import net.vorplex.core.util.LuckpermsUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;

public class JoinMessageListeners implements Listener {

    private final VorplexCore plugin = VorplexCore.getInstance();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final String prefix = LuckpermsUtil.getPrefix(player);
        if (plugin.isPremiumVanish())
            if (VanishAPI.isInvisible(player)) {
                Debug.log("NOT sending join message for vanished player: " + player.getName());
                return;
            }
//        if (plugin.getConfig().getBoolean("JoinMessages.customjoinmessages.enabled")) {
//            if (player.hasPermission("vorplexcore.customjoinmessages")) {
//                if (plugin.customJoinMessages.containsKey(player.getUniqueId())) {
//                    String placeholder = prefix + ChatColor.RESET + " " + player.getName();
//                    String joinmessage = plugin.customJoinMessages.get(player.getUniqueId()).replace("%me%", placeholder).replace("\n", "");
//                    event.setJoinMessage(ChatColor.translateAlternateColorCodes('&', joinmessage));
//                    return;
//                }
//            }
//        }
        if (plugin.getConfig().getBoolean("JoinMessages.PermissionBasedJoinMessages.enabled", true)) {
            Debug.log("Sending join message for player: " + player.getName());
            Component joinMessage = getPermissionJoinMessage(player, prefix);
            event.joinMessage(joinMessage == null ? Component.text("") : joinMessage);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerShow(PlayerShowEvent event) {
        if (event.isSilent()) return;
        if (!plugin.getConfig().getBoolean("JoinMessages.SendOnUnVanish", true)) return;
        final Player player = event.getPlayer();
        final String prefix = LuckpermsUtil.getPrefix(player);
//        if (plugin.getConfig().getBoolean("JoinMessages.customjoinmessages.enabled")) {
//            if (player.hasPermission("vorplexcore.customjoinmessages")) {
//                if (plugin.customJoinMessages.containsKey(player.getUniqueId())) {
//                    String placeholder = prefix + ChatColor.RESET + " " + player.getName();
//                    String joinmessage = plugin.customJoinMessages.get(player.getUniqueId()).replace("%me%", placeholder).replace("\n", "");
//                    for (Player all : Bukkit.getOnlinePlayers()) {
//                        all.sendMessage(ChatColor.translateAlternateColorCodes('&', joinmessage));
//                    }
//                    return;
//                }
//            }
//        }
        if (plugin.getConfig().getBoolean("JoinMessages.PermissionBasedJoinMessages.enabled", true)) {
            Debug.log("Sending fake join message for player: " + player.getName());
            Component joinMessage = getPermissionJoinMessage(player, prefix);
            if (joinMessage != null)
                Audience.audience(Bukkit.getServer().getOnlinePlayers()).sendMessage(joinMessage);
        }
    }

    /**
     * Get a player's permission based join message with placeholders replaced
     *
     * @param player the player to get the permission based join message for
     * @param prefix the player's prefix (rank)
     * @return the join message in a formatted component or null if the player does not have any permission based join messages
     */
    @Nullable
    public Component getPermissionJoinMessage(Player player, String prefix) {
        Debug.log("checking permission based join messages for " + player.getName());
        for (String permission : plugin.getConfig().getConfigurationSection("JoinMessages.PermissionJoinMessages.messages").getKeys(false)) {
            if (player.hasPermission("vorplexcore.joinmessages." + permission)) {
                Debug.log("Found permission vorplexcore.joinmessages." + permission + " for player: " + player.getName());
                String rawJoinMessage = plugin.getConfig().getString("JoinMessages.PermissionJoinMessages.messages." + permission);
                if (rawJoinMessage == null) return null;
                String parsedJoinMessage = rawJoinMessage;
                Debug.log("Raw Leave Message: " + rawJoinMessage);
                Debug.log("Parsed Leave Message: " + parsedJoinMessage);
                if (plugin.isPlaceholderAPI())
                    parsedJoinMessage = PlaceholderAPI.setPlaceholders(player, parsedJoinMessage);
                else
                    Debug.log("PlaceholderAPI not active, unable to replace placeholders in join message for: " + player.getName());

                return MiniMessage.miniMessage().deserialize(parsedJoinMessage,
                        Placeholder.parsed("prefix", prefix),
                        Placeholder.component("name", Component.text(player.getName()))
                );
            }
        }
        return null;
    }
}
