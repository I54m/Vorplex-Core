package me.vectornetwork.core.objects;

import me.vectornetwork.core.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class ScrollerInventory implements Listener {

    private final ArrayList<Inventory> pages = new ArrayList<>();
    private final UUID id;
    private int currpage = 0;
    private static final HashMap<UUID, ScrollerInventory> users = new HashMap<>();
    private final onClick click;
    private final String name;
    //Running this will open a paged inventory for the specified player, with the items in the arraylist specified.
    public ScrollerInventory(ArrayList<ItemStack> items, String name, onClick click){
        this.id = UUID.randomUUID();
        this.name = name;
        this.click = click;
        Main plugin = Main.getInstance();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        //create new blank page
        Inventory page = getBlankPage(name);
        //According to the items in the arraylist, add items to the ScrollerInventory
        for (ItemStack item : items) {
            //If the current page is full, add the page to the inventory's pages arraylist, and create a new page to add the items.
            if (page.firstEmpty() == 46) {
                pages.add(page);
                page = getBlankPage(name);
            }
            page.addItem(item);
        }
        pages.add(page);
    }

    public void open(Player p){
        //open page 0 for the specified player
        p.openInventory(pages.get(0));
        users.put(p.getUniqueId(), this);
    }

    public void recalculate(ArrayList<ItemStack> items){
        pages.clear();
        //create new blank page
        Inventory page = getBlankPage(name);
        //According to the items in the arraylist, add items to the ScrollerInventory
        for (ItemStack item : items) {
            //If the current page is full, add the page to the inventory's pages arraylist, and create a new page to add the items.
            if (page.firstEmpty() == 46) {
                pages.add(page);
                page = getBlankPage(name);
            }
            page.addItem(item);
        }
        pages.add(page);

    }
    private final Main plugin = Main.getInstance();
    private static final String nextPageName = ChatColor.LIGHT_PURPLE + "Next Page";
    private static final String previousPageName = ChatColor.LIGHT_PURPLE + "Previous Page";
    //This creates a blank page with the next and prev buttons
    private Inventory getBlankPage(String name){
        Inventory page = Bukkit.createInventory(null, 54, name);

        ItemStack nextpage = new ItemStack(Material.ARROW, 1);
        ItemMeta meta = nextpage.getItemMeta();
        meta.setDisplayName(nextPageName);
        nextpage.setItemMeta(meta);

        ItemStack prevpage = new ItemStack(Material.ARROW, 1);
        meta = prevpage.getItemMeta();
        meta.setDisplayName(previousPageName);
        prevpage.setItemMeta(meta);


        page.setItem(53, nextpage);
        page.setItem(45, prevpage);
        return page;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event){
        if(!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        //Get the current scroller inventory the player is looking at, if the player is looking at one.
        if(!users.containsKey(p.getUniqueId())) return;
        ScrollerInventory inv = users.get(p.getUniqueId());
        if(event.getCurrentItem() == null) return;
        if(event.getCurrentItem().getItemMeta() == null) return;
        if(event.getCurrentItem().getItemMeta().getDisplayName() == null) return;
        //If the pressed item was a nextpage button
        if(event.getCurrentItem().getItemMeta().getDisplayName().equals(ScrollerInventory.nextPageName)){
            event.setCancelled(true);
            //If there is no next page, don't do anything
            if (inv.currpage < inv.pages.size() - 1) {
                //Next page exists, flip the page
                inv.currpage += 1;
                p.openInventory(inv.pages.get(inv.currpage));
            }
            return;
            //if the pressed item was a previous page button
        }else if(event.getCurrentItem().getItemMeta().getDisplayName().equals(ScrollerInventory.previousPageName)){
            event.setCancelled(true);
            //If the page number is more than 0 (So a previous page exists)
            if(inv.currpage > 0){
                //Flip to previous page
                inv.currpage -= 1;
                p.openInventory(inv.pages.get(inv.currpage));
            }
            return;
        }
        event.setCancelled(true);
        if (event.getCurrentItem() != null) {
            if (click.click(p, event.getCurrentItem(), this))
                if (p.getOpenInventory().getTitle().equals(name))
                    p.closeInventory();
        }
    }

    public interface onClick {
        boolean click(Player clicker, ItemStack item, ScrollerInventory scrollerInventory);
    }
}
