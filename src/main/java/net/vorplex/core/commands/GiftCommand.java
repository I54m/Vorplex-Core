package net.vorplex.core.commands;

import net.vorplex.core.Main;
import net.vorplex.core.objects.Gift;
import net.vorplex.core.objects.IconMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GiftCommand implements CommandExecutor {

    public static Map<UUID, ItemStack> inProgress = new HashMap<>();
    private final Main plugin = Main.getInstance();
    private final boolean old = plugin.old;

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("You must be a player to use this command!");
            return false;
        }
        Player player = (Player) commandSender;
        if (!player.hasPermission("vorplexcore.gifts.send")) {
            player.sendMessage(plugin.prefix + ChatColor.RED + "you do not have permission to use this command!");
            return false;
        }
        if (strings.length < 1) {
            player.sendMessage(plugin.prefix + ChatColor.RED + "Send a player the item in your main hand as a gift, Usage: /gift <player>");
            return false;
        }
        ItemStack itemgift = old ? player.getItemInHand().clone() : player.getInventory().getItemInMainHand().clone();
        if (itemgift == null || itemgift.getType() == Material.AIR) {
            player.sendMessage(plugin.prefix + ChatColor.RED + "You must be holding an item in your main hand to gift a player");
            return false;
        }
        Player receiver = Bukkit.getPlayer(strings[0]);
        if (receiver == null) {
            player.sendMessage(plugin.prefix + ChatColor.RED + "That is not an online player's name!");
            return false;
        }
        if (receiver.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(plugin.prefix + ChatColor.RED + "You cannot send yourself a gift!");
            return false;
        }
        if (plugin.gifts.containsKey(receiver.getUniqueId())) {
            ArrayList<Gift> gifts = new ArrayList<>(plugin.gifts.get(receiver.getUniqueId()));
            for (Gift gift : gifts) {
                if (gift.getSender().equals(player.getUniqueId())) {
                    player.sendMessage(plugin.prefix + ChatColor.RED + receiver.getName() + " already has an unopened gift from you!");
                    return false;
                }
            }
        }
        inProgress.put(player.getUniqueId(), itemgift);
        player.getInventory().remove(itemgift);
        player.updateInventory();
        final ItemStack deny = old ? new ItemStack(Material.valueOf("WOOL"), 1, (short) 14) : new ItemStack(Material.RED_STAINED_GLASS_PANE);
        final ItemStack confirm = old ? new ItemStack(Material.valueOf("WOOL"), 1, (short) 5) : new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        IconMenu menu = new IconMenu(ChatColor.RED + "/Gift confirmation", 3, (clicker, menu1, slot, item) -> {
            if (clicker == player) {
                if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
                if (item.getItemMeta().getDisplayName().contains(ChatColor.GREEN + "Confirm") && (old ? item.getType() == confirm.getType() && item.getDurability() == confirm.getDurability() : item.getType() == confirm.getType())) {
                    clicker.sendMessage(plugin.prefix + ChatColor.GREEN + "Gifting item to " + receiver.getName());
                    Gift gift = new Gift(itemgift, player.getUniqueId());
                    if (plugin.gifts.containsKey(receiver.getUniqueId())) {
                        ArrayList<Gift> gifts = new ArrayList<>(plugin.gifts.get(receiver.getUniqueId()));
                        gifts.add(gift);
                        plugin.gifts.put(receiver.getUniqueId(), gifts);
                    } else {
                        ArrayList<Gift> gifts = new ArrayList<>();
                        gifts.add(gift);
                        plugin.gifts.put(receiver.getUniqueId(), gifts);
                    }
                    if (receiver.isOnline())
                        receiver.sendMessage(plugin.prefix + ChatColor.GREEN + clicker.getName() + " has sent you a gift! Do /gifts to claim it!");
                    inProgress.remove(clicker.getUniqueId());
                    return true;
                } else if (item.getItemMeta().getDisplayName().contains(ChatColor.RED + "Deny") && (old ? item.getType() == deny.getType() && item.getDurability() == deny.getDurability() : item.getType() == deny.getType())) {
                    clicker.sendMessage(plugin.prefix + ChatColor.RED + "Cancelled gifting item to " + receiver.getName());
                    if (old) player.getInventory().setItemInHand(inProgress.get(clicker.getUniqueId()));
                    else player.getInventory().setItemInMainHand(inProgress.get(clicker.getUniqueId()));
                    inProgress.remove(clicker.getUniqueId());
                    player.updateInventory();
                    return true;
                }
            }
            return false;
        }, (closer, menu1) -> {
            closer.sendMessage(plugin.prefix + ChatColor.RED + "Cancelled gifting item to " + receiver.getName());
            if (old) player.getInventory().setItemInHand(inProgress.get(closer.getUniqueId()));
            else player.getInventory().setItemInMainHand(inProgress.get(closer.getUniqueId()));
            inProgress.remove(closer.getUniqueId());
            player.updateInventory();
            return true;
        });


        menu.addButton(0, deny, ChatColor.RED + "Deny");
        menu.addButton(1, deny, ChatColor.RED + "Deny");
        menu.addButton(2, deny, ChatColor.RED + "Deny");

        menu.addButton(4, new ItemStack(Material.PAPER), ChatColor.RED + "WARNING!!!",
                ChatColor.WHITE + "Confirming this action will", ChatColor.WHITE + "send " + receiver.getName() + " the item in",
                ChatColor.WHITE + "your main hand (below).", ChatColor.RED + "This cannot be undone!!");

        menu.addButton(6, confirm, ChatColor.GREEN + "Confirm");
        menu.addButton(7, confirm, ChatColor.GREEN + "Confirm");
        menu.addButton(8, confirm, ChatColor.GREEN + "Confirm");
        menu.addButton(9, deny, ChatColor.RED + "Deny");
        menu.addButton(10, deny, ChatColor.RED + "Deny");
        menu.addButton(11, deny, ChatColor.RED + "Deny");

        menu.addButton(13, itemgift);

        menu.addButton(15, confirm, ChatColor.GREEN + "Confirm");
        menu.addButton(16, confirm, ChatColor.GREEN + "Confirm");
        menu.addButton(17, confirm, ChatColor.GREEN + "Confirm");
        menu.addButton(18, deny, ChatColor.RED + "Deny");
        menu.addButton(19, deny, ChatColor.RED + "Deny");
        menu.addButton(20, deny, ChatColor.RED + "Deny");


        menu.addButton(24, confirm, ChatColor.GREEN + "Confirm");
        menu.addButton(25, confirm, ChatColor.GREEN + "Confirm");
        menu.addButton(26, confirm, ChatColor.GREEN + "Confirm");
        menu.open(player);
        return true;
    }
}
