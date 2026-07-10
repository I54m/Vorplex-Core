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
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.Nullable;

public class LeaveMessageListeners implements Listener {

    private static final VorplexCore plugin = VorplexCore.getInstance();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final String prefix = LuckpermsUtil.getPrefix(player);
        if (plugin.isPremiumVanish())
            if (isVanished(player) || VanishAPI.isInvisible(player)) {
                Debug.log("NOT sending leave message for vanished player: " + player.getName());
                plugin.getCustomLeaveMessagesCache().remove(player.getUniqueId());
                plugin.getCustomJoinMessagesCache().remove(player.getUniqueId());
                return;
            }
        if (plugin.getConfig().getBoolean("LeaveMessages.CustomLeaveMessages.enabled", true)) {
            if (player.hasPermission("vorplexcore.customleavemessages")) {
                if (plugin.getCustomLeaveMessagesCache().containsKey(player.getUniqueId())) {
                    Debug.log("Sending Custom leave message for player: " + player.getName());
                    event.quitMessage(getCustomLeaveMessage(player, prefix));
                    plugin.getCustomLeaveMessagesCache().remove(player.getUniqueId());
                    plugin.getCustomJoinMessagesCache().remove(player.getUniqueId());
                    return;
                }
            }
        }
        if (plugin.getConfig().getBoolean("LeaveMessages.PermissionBasedLeaveMessages.enabled", true)) {
            Debug.log("Sending leave message for player: " + player.getName());
            Component leaveMessage = getPermissionLeaveMessage(player, prefix);
            event.quitMessage(leaveMessage == null ? Component.text("") : leaveMessage);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerShow(PlayerHideEvent event) {
        if (event.isSilent()) return;
        if (!plugin.getConfig().getBoolean("LeaveMessages.SendOnVanish", true)) return;
        final Player player = event.getPlayer();
        final String prefix = LuckpermsUtil.getPrefix(player);

        if (plugin.getConfig().getBoolean("LeaveMessages.CustomLeaveMessages.enabled", true)) {
            if (player.hasPermission("vorplexcore.customleavemessages")) {
                if (plugin.getCustomLeaveMessagesCache().containsKey(player.getUniqueId())) {
                    Debug.log("Sending Custom leave message for player: " + player.getName());
                    Audience.audience(Bukkit.getServer().getOnlinePlayers()).sendMessage(getCustomLeaveMessage(player, prefix));
                    return;
                }
            }
        }
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

    /**
     * Get a player's custom leave message
     *
     * @param player the player to get the custom leave message for
     * @param prefix the player's prefix (rank)
     * @return the leave message in a formatted component or null if the player does not have a cached custom leave message
     * @throws IllegalStateException if there is no custom leave message cached for the player
     */
    public static Component getCustomLeaveMessage(Player player, String prefix) {
        if (!plugin.getCustomLeaveMessagesCache().containsKey(player.getUniqueId()))
            throw new IllegalStateException("No custom leave message cached for player: " + player.getName());

        return plugin.getBasicMM().deserialize(
                plugin.getCustomLeaveMessagesCache().get(player.getUniqueId()),
                Placeholder.parsed("prefix", prefix),
                Placeholder.component("name", Component.text(player.getName()))
        ).hoverEvent(
                MiniMessage.miniMessage().deserialize(
                        "<white>This is <prefix><name>'s Custom Leave Message!</white>",
                        Placeholder.parsed("prefix", prefix),
                        Placeholder.component("name", Component.text(player.getName()))
                )
        );
    }

    /**
     * private helper method to determine if a player is vanished based on their metadata
     * this is needed for leave events as the palyer can be considered offline during a leave event
     *
     * @param player the player to check
     * @return true if the player is vanished, else false
     */
    private boolean isVanished(Player player) {
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }
        return false;
    }
}
