package net.vorplex.core.listeners;

import net.vorplex.core.Main;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class CommandPreProcess implements Listener {
    private static Main plugin = Main.getPlugin(Main.class);
    public static List<String> servers = plugin.getConfig().getStringList("VorplexServer.serverslist");

    private static void sendPluginMessage(@NotNull Player player, String channel, @NotNull String... messages) {
        try {
            ByteArrayOutputStream outbytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outbytes);
            for (String msg : messages) {
                out.writeUTF(msg);
            }
            player.sendPluginMessage(plugin, channel, outbytes.toByteArray());
            out.close();
            outbytes.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandPreProcess(PlayerCommandPreprocessEvent event){
        String message = event.getMessage();
        String[] args = message.split(" ");
        Player player = event.getPlayer();
        if (args.length >= 2){
            if (!args[0].equalsIgnoreCase("/server")) return;
            if (!player.hasPermission("vorplexcore.server.switch")) return;
            for (String server : servers){
                if (server.equalsIgnoreCase(args[1]) || server.contains(args[1])){
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("VorplexServer.sendingtoservermessage").replace("%server%", server)));
                    sendPluginMessage(player, "BungeeCord", "Connect", server);
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
}