package net.vorplex.core.listeners;

import net.vorplex.core.VorplexCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class InventoryClick implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(final InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        if (item.getType() == Material.GLASS && event.getSlotType() == InventoryType.SlotType.ARMOR && event.getSlot() == 39) {
            final Player player = (Player) event.getWhoClicked();
            player.playSound(player.getLocation(), Sound.BLOCK_METAL_PRESSURE_PLATE_CLICK_OFF, 100, 0.5f);
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 5));
            player.playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING, 100, 1);
//            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 5));
//            player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 100, 5));
            player.damage(5);
            player.setFoodLevel(1);
            event.setCancelled(true);
            player.getInventory().setHelmet(new ItemStack(Material.AIR));
            Bukkit.getScheduler().runTaskLater(VorplexCore.getInstance(), () -> player.stopSound(Sound.ITEM_ELYTRA_FLYING), 20);
            Bukkit.getScheduler().runTaskLater(VorplexCore.getInstance(), () -> {
                player.getInventory().setHelmet(PlayerJoin.oxygenHelmet);
                player.playSound(player.getLocation(), Sound.BLOCK_METAL_PRESSURE_PLATE_CLICK_ON, 100, 1.5f);
                player.sendMessage(ChatColor.RED + "Best to keep your helmet on! You don't want to suffocate, do you?");
                player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 150, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 150, 10));
            }, 60);
        }
    }
}
