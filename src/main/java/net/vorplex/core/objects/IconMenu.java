package net.vorplex.core.objects;

import net.vorplex.core.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IconMenu implements Listener {

    private List<String> viewing = new ArrayList<>();
    private String name;
    private int size;
    private onClick click;
    private onClose close;
    private ItemStack[] items;

    public IconMenu(String name, int size, onClick click, onClose close) {
        this.name = name;
        this.size = size * 9;
        items = new ItemStack[this.size];
        this.click = click;
        this.close = close;
        Main plugin = Main.getInstance();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void setSize(int size) {
        this.size = size * 9;
        this.items = Arrays.copyOf(items, this.size);
    }

    public void setName(String name) {
        this.name = name;
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        for (Player p : this.getViewers())
            close(p);
    }

    public void open(Player p) {
        p.openInventory(getInventory(p));
        viewing.add(p.getName());
    }

    private Inventory getInventory(Player p) {
        Inventory inv = Bukkit.createInventory(p, size, name);
        for (int i = 0; i < items.length; i++)
            if (items[i] != null)
                inv.setItem(i, items[i]);
        return inv;
    }

    public void close(Player p) {
        if (p.getOpenInventory().getTitle().equals(name))
            p.closeInventory();
        viewing.remove(p.getName());
    }

    private List<Player> getViewers() {
        List<Player> viewers = new ArrayList<>();
        for (String s : viewing)
            viewers.add(Bukkit.getPlayer(s));
        return viewers;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (viewing.contains(event.getWhoClicked().getName())) {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();
            if (event.getCurrentItem() != null) {
                if (click.click(p, this, event.getSlot(), event.getCurrentItem()))
                    close(p);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (viewing.contains(event.getPlayer().getName())) {
            Player p = (Player) event.getPlayer();
            close.close(p, this);
            viewing.remove(p.getName());
        }
    }

    public void addButton(int position, ItemStack item, String name, String... lore) {
        items[position] = getItem(item, name, lore);
    }

    public void addButton(int position, ItemStack item) {
        items[position] = item;
    }

    private ItemStack getItem(ItemStack item, String name, String... lore) {
        ItemMeta im = item.getItemMeta();
        im.setDisplayName(name);
        im.setLore(Arrays.asList(lore));
        item.setItemMeta(im);
        return item;
    }

    public interface onClick {
        boolean click(Player clicker, IconMenu menu, int slot, ItemStack item);
    }

    public interface onClose {
        void close(Player closer, IconMenu menu);
    }
}
