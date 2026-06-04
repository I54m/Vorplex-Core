package net.vorplex.core.listeners;

import de.myzelyam.api.vanish.PlayerHideEvent;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

public class LeaveMessageListeners implements Listener {

    private final VorplexCore plugin = VorplexCore.getInstance();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final String prefix = LuckpermsUtil.getPrefix(player);
        if (plugin.isPremiumVanish())
            if (VanishAPI.isInvisible(player)) return;
//        if (plugin.getConfig().getBoolean("LeaveMessages.customleavemessages.enabled")) {
//            if (player.hasPermission("vorplexcore.customleavemessages")) {
//                if (plugin.customLeaveMessages.containsKey(player.getUniqueId())) {
//                    String placeholder = prefix + ChatColor.RESET + " " + player.getName();
//                    String leavemessage = plugin.customLeaveMessages.get(player.getUniqueId()).replace("%me%", placeholder).replace("\n", "");
//                    event.setQuitMessage(ChatColor.translateAlternateColorCodes('&', leavemessage));
//                    return;
//                }
//            }
//        }
        if (plugin.getConfig().getBoolean("LeaveMessages.PermissionBasedLeaveMessages.enabled", true)) {
            Component leaveMessage = getPermissionLeaveMessage(player, prefix);
            event.quitMessage(leaveMessage == null ? Component.text("") : leaveMessage);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerShow(PlayerHideEvent event) {
        if (event.isSilent()) return;
        if (!plugin.getConfig().getBoolean("LeaveMessages.SendOnVanish", true)) return;
        final Player player = event.getPlayer();
        final String prefix = LuckpermsUtil.getPrefix(player);
//        if (plugin.getConfig().getBoolean("LeaveMessages.customleavemessages.enabled")) {
//            if (player.hasPermission("vorplexcore.customleavemessages")) {
//                if (plugin.customLoinMessages.containsKey(player.getUniqueId())) {
//                    String placeholder = prefix + ChatColor.RESET + " " + player.getName();
//                    String leavemessage = plugin.customleaveMessages.get(player.getUniqueId()).replace("%me%", placeholder).replace("\n", "");
//                    for (Player all : Bukkit.getOnlinePlayers()) {
//                        all.sendMessage(ChatColor.translateAlternateColorCodes('&', leavemessage));
//                    }
//                    return;
//                }
//            }
//        }
        if (plugin.getConfig().getBoolean("LeaveMessages.PermissionBasedLeaveMessages.enabled", true)) {
            Debug.log("Sending fake leave message for player: " + player.getName());
            Component leaveMessage = getPermissionLeaveMessage(player, prefix);
            if (leaveMessage != null)
                Audience.audience(Bukkit.getServer().getOnlinePlayers()).sendMessage(leaveMessage);
        }
    }

    /**
     * Get a player's permission based leave message with placeholders replaced
     *
     * @param player the player to get the permission based leave message for
     * @param prefix the player's prefix (rank)
     * @return the leave message in a formatted component or null if the player does not have any permission based leave messages
     */
    @Nullable
    public Component getPermissionLeaveMessage(Player player, String prefix) {
        Debug.log("checking permission based leave messages for " + player.getName());
        for (String permission : plugin.getConfig().getConfigurationSection("LeaveMessages.PermissionLeaveMessages.messages").getKeys(false)) {
            if (player.hasPermission("vorplexcore.leavemessages." + permission)) {
                Debug.log("Found permission vorplexcore.leavemessages." + permission + " for player: " + player.getName());
                String rawLeaveMessage = plugin.getConfig().getString("LeaveMessages.PermissionLeaveMessages.messages." + permission);
                if (rawLeaveMessage == null) return null;
                String parsedLeaveMessage = rawLeaveMessage;
                Debug.log("Raw Leave Message: " + rawLeaveMessage);
                Debug.log("Parsed Leave Message: " + parsedLeaveMessage);
                if (plugin.isPlaceholderAPI())
                    parsedLeaveMessage = PlaceholderAPI.setPlaceholders(player, parsedLeaveMessage);
                else
                    Debug.log("PlaceholderAPI not active, unable to replace placeholders in leave message for: " + player.getName());

                return MiniMessage.miniMessage().deserialize(parsedLeaveMessage,
                        Placeholder.parsed("prefix", prefix),
                        Placeholder.component("name", Component.text(player.getName()))
                );
            }
        }
        return null;
    }
}
