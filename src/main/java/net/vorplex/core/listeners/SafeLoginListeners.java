package net.vorplex.core.listeners;

import lombok.NonNull;
import net.vorplex.core.VorplexCore;
import net.vorplex.core.util.Debug;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;

public class SafeLoginListeners implements Listener {

    private final VorplexCore plugin = VorplexCore.getInstance();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (plugin.getConfig().getBoolean("SafeLogin.enabled", true)) {
            if (!plugin.getConfig().getList("SafeLogin.AllowedWorlds", new ArrayList<>(Collections.singleton("world"))).contains(player.getWorld().getName())) {
                Debug.log(player.getName() + " is not in an allowed world for safe-login");
                return;
            }
            if (isLocationUnSafe(player)) {
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
                    player.setFireTicks(0);
                    player.teleport(safeLocation);
                    player.sendRichMessage(plugin.getConfig().getString("SafeLogin.TeleportMessage", "<red>You joined in an unsafe location, You have been teleported to the highest safe block!"));
                    Debug.log(player.getName() + " was teleported successfully");
                }, 60);
            }
        }
    }

    /**
     * Check a player's location to see if it is unsafe
     *
     * @param player the player to check the safe location for
     * @return true if the location is considered unsafe, else false
     */
    public boolean isLocationUnSafe(final @NonNull Player player) {
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            Debug.log(player.getName() + " is in creative or spectator, location is considered safe for them");
            return false;
        }
        final Location headLocation = player.getEyeLocation().getBlock().getLocation();
        final Location feetLocation = player.getLocation().getBlock().getLocation();
        Debug.log("isLocationUnSafe - headLocation = " + headLocation + " feetLocation = " + feetLocation);
        final Block standingBlock = feetLocation.getBlock().getRelative(BlockFace.DOWN);
        if (feetLocation.getBlock().isSuffocating() && headLocation.getBlock().isSuffocating()) {
            Debug.log(player.getName() + "'s head AND feet location was in a block that causes suffocation - NOT SAFE");
            return true;
        } else if (feetLocation.getBlock().getType() == Material.LAVA || headLocation.getBlock().getType() == Material.LAVA || player.getFireTicks() > 0) {
            Debug.log(player.getName() + "'s head OR feet location was in lava OR they were on fire - NOT SAFE");
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
