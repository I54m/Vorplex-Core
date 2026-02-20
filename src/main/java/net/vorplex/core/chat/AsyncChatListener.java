package net.vorplex.core.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class AsyncChatListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (ChatUtils.isStaffChatOn(player)) {
            event.viewers().clear();
            event.setCancelled(true);
            ChatUtils.sendStaffChat(player, PlainTextComponentSerializer.plainText().serialize(event.originalMessage()));
        } else if (ChatUtils.isAdminChatOn(player)) {
            event.viewers().clear();
            event.setCancelled(true);
            ChatUtils.sendAdminChat(player, PlainTextComponentSerializer.plainText().serialize(event.originalMessage()));
        }
    }
}
