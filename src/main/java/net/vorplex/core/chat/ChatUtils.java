package net.vorplex.core.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.vorplex.core.VorplexCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ChatUtils {

    private static final String STAFF_CHAT_FORMAT = "<gray>[<red><bold>SC</red>]</gray> <rank><sender><white>: <red><message></red>";
    private static final String ADMIN_CHAT_FORMAT = "<gray>[<blue><bold>AC</blue>]</gray> <rank><sender><white>: <blue><message></blue>";
    private static final VorplexCore plugin = VorplexCore.getInstance();

    public static List<Player> staffChat = new ArrayList<>();
    public static List<Player> adminChat = new ArrayList<>();

    public static boolean isStaffChatOn(Player player) {
        return staffChat.contains(player);
    }

    public static boolean isAdminChatOn(Player player) {
        return adminChat.contains(player);
    }

    public static List<Player> getOnlineStaff() {
        return (List<Player>) Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission("vorplexcore.staffchat"))
                .toList();
    }

    public static List<Player> getOnlineAdmins() {
        return (List<Player>) Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission("vorplexcore.adminchat"))
                .toList();
    }

    public static String getPrefix(Player player) {
        User user = plugin.getLuckPermsAPI().getUserManager().getUser(player.getUniqueId());
        if (user == null) return "";

        CachedMetaData metaData = user.getCachedData().getMetaData();
        return metaData.getPrefix() != null ? metaData.getPrefix() : "";
    }

    public static void sendStaffChat(Player player, String message) {
        getOnlineStaff().forEach(staff ->
                staff.sendRichMessage(STAFF_CHAT_FORMAT,
                        Placeholder.component("rank", MiniMessage.miniMessage().deserialize(getPrefix(player))),
                        Placeholder.component("sender", Component.text(player.getName())),
                        Placeholder.component("message", Component.text(message))
                )
        );
    }


    public static void sendAdminChat(Player player, String message) {
        getOnlineAdmins().forEach(admin ->
                admin.sendRichMessage(ADMIN_CHAT_FORMAT,
                        Placeholder.component("rank", MiniMessage.miniMessage().deserialize(getPrefix(player))),
                        Placeholder.component("sender", Component.text(player.getName())),
                        Placeholder.component("message", Component.text(message))
                )
        );
    }
}
