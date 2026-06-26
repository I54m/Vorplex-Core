package net.vorplex.core.listeners;

//import com.earth2me.essentials.spawn.EssentialsSpawn;

import net.vorplex.core.VorplexCore;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerJoin implements Listener {

    private final VorplexCore plugin = VorplexCore.getInstance();
    public static ItemStack oxygenHelmet = new ItemStack(Material.GLASS, 1);

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoinHigh(PlayerJoinEvent event) {
        if (plugin.getConfig().getBoolean("Hub.oxygen-helmet-enabled")) {
            event.getPlayer().getInventory().setHelmet(oxygenHelmet);
            event.getPlayer().updateInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoinHighest(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        //TODO fetcher update give fetchers their own listener
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
//                if (plugin.old)
//                    player.sendTitle(ChatColor.translateAlternateColorCodes('&', args[0]), ChatColor.translateAlternateColorCodes('&', args[1]));
//                else
                player.sendTitle(ChatColor.translateAlternateColorCodes('&', args[0]), ChatColor.translateAlternateColorCodes('&', args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
            } catch (NumberFormatException nfe) {
                player.sendMessage(ChatColor.RED + "Error: Last three arguments in the title must be numbers!!");
            }
        }
    }
}
