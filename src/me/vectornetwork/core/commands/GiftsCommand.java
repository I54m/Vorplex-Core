package me.vectornetwork.core.commands;

import me.vectornetwork.core.Main;
import me.vectornetwork.core.util.NameFetcher;
import me.vectornetwork.core.objects.Gift;
import me.vectornetwork.core.objects.IconMenu;
import me.vectornetwork.core.objects.ScrollerInventory;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GiftsCommand implements CommandExecutor {

    private Main plugin = Main.getInstance();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player)){
            commandSender.sendMessage("You must be a player ot use this commmand!");
            return false;
        }
        Player player = (Player) commandSender;
        if (!player.hasPermission("vectorcore.gifts.claim")){
            player.sendMessage(plugin.prefix + ChatColor.RED + "You do not have permission to use this command!");
            return false;
        }
        if (!plugin.gifts.containsKey(player.getUniqueId())){
            player.sendMessage(plugin.prefix + ChatColor.RED + "You do not have any gifts to claim!");
            return false;
        }
        return openGiftMenu(player);
    }

    private boolean openGiftMenu(Player player){
        ArrayList<Gift> gifts = new ArrayList<>(plugin.gifts.get(player.getUniqueId()));
        if (gifts.size() <= 56) {
            IconMenu menu = new IconMenu(ChatColor.LIGHT_PURPLE + player.getName() + "'s Gifts", 1, (clicker, menu1, slot, item) -> {
                if (clicker == player) {
                    if (item == null || !item.hasItemMeta()) return false;
                    if (item.getItemMeta().hasDisplayName()) {
                        Gift gift = getGift(item, clicker);
                        if (gift == null){
                            clicker.sendMessage(plugin.prefix + ChatColor.RED + "An error occurred, please try again!");
                            return true;
                        }
                        if (clicker.getInventory().firstEmpty() == -1){
                            clicker.sendMessage(plugin.prefix + ChatColor.RED + "Your inventory is full, please remove some items before redeeming a gift!");
                            return true;
                        }
                        clicker.getInventory().addItem(gift.getItem());
                        clicker.sendMessage(plugin.prefix + ChatColor.GREEN + item.getItemMeta().getDisplayName() + ChatColor.LIGHT_PURPLE + " Redeemed!");
                        ArrayList<Gift> giftsLeft = new ArrayList<>(plugin.gifts.get(player.getUniqueId()));
                        giftsLeft.remove(gift);
                        if (giftsLeft.isEmpty()) {
                            plugin.gifts.remove(player.getUniqueId());
                            return true;
                        }else {
                            plugin.gifts.put(player.getUniqueId(), giftsLeft);
                            menu1.close(clicker);
                            menu1.removeButton(slot);
                            menu1.open(clicker);
                        }
                    }
                }
                return false;
            });
            int position = 0;
            for (Gift gift : gifts) {
                ItemStack giftclone = gift.getItem().clone();
                ItemMeta im = giftclone.getItemMeta();
                im.setDisplayName(ChatColor.LIGHT_PURPLE + "Gift from: " + ChatColor.WHITE + NameFetcher.getName(gift.getSender().toString()));
                if (giftclone.getItemMeta().hasLore()) {
                    List<String> lore = new ArrayList<>(gift.getItem().getItemMeta().getLore());
                    if (giftclone.getItemMeta().hasDisplayName()) {
                        lore.add(0, ChatColor.LIGHT_PURPLE + "Item Name: " + ChatColor.WHITE + gift.getItem().getItemMeta().getDisplayName());
                        lore.add(1, ChatColor.LIGHT_PURPLE + "Item Lore: ");
                    }else{
                        lore.add(0, ChatColor.LIGHT_PURPLE + "Item Lore: ");
                    }
                    im.setLore(lore);
                }
                giftclone.setItemMeta(im);
                menu.addButton(position, giftclone);
                position++;
                if (position >= 54) {
                    break;
                } else if (position == 45) {
                    menu.setSize(6);
                } else if (position == 36) {
                    menu.setSize(5);
                } else if (position == 27) {
                    menu.setSize(4);
                } else if (position == 18) {
                    menu.setSize(3);
                } else if (position == 9) {
                    menu.setSize(2);
                }

            }
            menu.open(player);
            return true;
        } else {
            ArrayList<ItemStack> items = new ArrayList<>();
            for (Gift gift : gifts) {
                ItemStack giftclone = gift.getItem().clone();
                ItemMeta im = giftclone.getItemMeta();
                im.setDisplayName(ChatColor.LIGHT_PURPLE + "Gift from: " + ChatColor.WHITE + NameFetcher.getName(gift.getSender().toString()));
                if (giftclone.getItemMeta().hasLore()) {
                    List<String> lore = new ArrayList<>(gift.getItem().getItemMeta().getLore());
                    if (giftclone.getItemMeta().hasDisplayName()) {
                        lore.add(0, ChatColor.LIGHT_PURPLE + "Item Name: " + ChatColor.WHITE + gift.getItem().getItemMeta().getDisplayName());
                        lore.add(1, ChatColor.LIGHT_PURPLE + "Item Lore: ");
                    }else{
                        lore.add(0, ChatColor.LIGHT_PURPLE + "Item Lore: ");
                    }
                    im.setLore(lore);
                }
                giftclone.setItemMeta(im);
                items.add(giftclone);
            }
            ScrollerInventory.onClick onClick = (clicker, item, scrollerInventory) -> {
                if (clicker == player) {
                    if (item == null || !item.hasItemMeta()) return false;
                    if (item.getItemMeta().hasDisplayName()) {
                        Gift gift = getGift(item, clicker);
                        if (gift == null){
                            clicker.sendMessage(plugin.prefix + ChatColor.RED + "An error occurred, please try again!");
                            return true;
                        }
                        if (clicker.getInventory().firstEmpty() == -1){
                            clicker.sendMessage(plugin.prefix + ChatColor.RED + "Your inventory is full, please remove some items before redeeming a gift!");
                            return true;
                        }
                        clicker.getInventory().addItem(gift.getItem());
                        clicker.sendMessage(plugin.prefix + ChatColor.GREEN + item.getItemMeta().getDisplayName() + ChatColor.LIGHT_PURPLE + " Redeemed!");
                        ArrayList<Gift> giftsLeft = new ArrayList<>(plugin.gifts.get(clicker.getUniqueId()));
                        giftsLeft.remove(gift);
                        if (giftsLeft.isEmpty()) {
                            plugin.gifts.remove(clicker.getUniqueId());
                            return true;
                        }else {
                            plugin.gifts.put(clicker.getUniqueId(), giftsLeft);
                            clicker.closeInventory();
                            scrollerInventory.recalculate(items);
                            scrollerInventory.open(clicker);
                        }
                    }
                }
                return false;
            };
            new ScrollerInventory(items, ChatColor.LIGHT_PURPLE + player.getName() + "'s Gifts", player, onClick).open(player);
            return false;
        }
    }

    private Gift getGift(ItemStack item, Player receiver){
        String ItemName = "";
        List<String> ItemLore = null;
        String senderName = null;
        if (item.getItemMeta().hasDisplayName()){
            senderName = item.getItemMeta().getDisplayName().replace(ChatColor.LIGHT_PURPLE + "Gift from: " + ChatColor.WHITE, "");
        }
        if (item.getItemMeta().hasLore()){
            List<String> lore = item.getItemMeta().getLore();
            if (lore.get(0).contains("Item Name: ")){
                ItemName = lore.get(0).replace("Item Name: ", "");
                ItemLore = lore.subList(2, lore.size()-1);
            }else if (lore.get(0).contains("Item Lore: ")){
                ItemLore = lore.subList(1, lore.size()-1);
            }
        }
        ArrayList<Gift> gifts = new ArrayList<>(plugin.gifts.get(receiver.getUniqueId()));
        for (Gift gift : gifts) {
            if (gift.getItem().getType() == item.getType() && gift.getItem().getEnchantments().equals(item.getEnchantments()) && NameFetcher.getName(gift.getSender().toString()).equals(senderName)) {
                if (gift.getItem().getItemMeta().hasDisplayName()) {
                    if (ChatColor.stripColor(ItemName).equals(ChatColor.stripColor(gift.getItem().getItemMeta().getDisplayName()))) {
                        if (ItemLore != null && gift.getItem().getItemMeta().hasLore()) {
                            if (ItemLore.equals(gift.getItem().getItemMeta().getLore())) {
                                return gift;
                            }
                        }
                    }
                }
                return gift;
            }
        }
        return null;
    }
}
