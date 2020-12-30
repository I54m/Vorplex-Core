package net.vorplex.core.listeners;

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
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        if (item.getType() == Material.GLASS && event.getSlotType() == InventoryType.SlotType.ARMOR && event.getSlot() == 39) {
            Player player = (Player) event.getWhoClicked();
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 3, 5));
            player.playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING, 100, 1);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 3, 5));
            player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 5, 5));
            player.damage(1);
            player.setFoodLevel(0);
            player.sendMessage(ChatColor.RED + "Best to keep your helmet on! You don't want to suffocate, do you?");
            event.setCancelled(true);
            player.stopSound(Sound.ITEM_ELYTRA_FLYING);
            player.setFoodLevel(20);
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 5, 10));
        }
    }
}
