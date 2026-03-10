package net.vorplex.core.objects;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.vorplex.core.VorplexCore;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class ScrollerInventory implements Listener, InventoryHolder {

    @Getter
    private final UUID id;
    @Getter
    private int currentPage = 0;
    @Getter
    private final HashMap<UUID, ScrollerInventory> viewers = new HashMap<>();
    @Getter
    private final Component name;
    @Nullable
    private final onClick click;
    @Nullable
    private final onClose close;
    private final ArrayList<Inventory> pages = new ArrayList<>();

    private final Component nextPageName = Component.text("Next Page").color(NamedTextColor.LIGHT_PURPLE);
    private final Component previousPageName = Component.text("Previous Page").color(NamedTextColor.LIGHT_PURPLE);

    private final ItemStack nextPage = ItemStack.of(Material.ARROW, 1);
    private final ItemStack previousPage = ItemStack.of(Material.ARROW, 1);

    private final VorplexCore plugin = VorplexCore.getInstance();

    /**
     * Create a Scroller Inventory without any click or close actions
     *
     * @param items An ArrayList of ItemStacks to be added to the Inventory - will be paginated if required
     * @param name  The name of the ScrollerInventory
     */
    public ScrollerInventory(ArrayList<ItemStack> items, Component name) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.click = null;
        this.close = null;

        // Setup control items
        setupControlItems();
        // register events for this ScrollerInventory
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        //run the first population of the menu
        repopulate(items);
    }

    /**
     * Create a Scroller Inventory with a click action and no close action
     *
     * @param items An ArrayList of ItemStacks to be added to the Inventory - will be paginated if required
     * @param name  The name of the ScrollerInventory
     * @param click A lambda function to execute upon the user clicking an item (does not work for nextPage or previousPage)
     */
    public ScrollerInventory(ArrayList<ItemStack> items, Component name, @Nullable onClick click) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.click = click;
        this.close = null;

        // Setup control items
        setupControlItems();
        // register events for this ScrollerInventory
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        //run the first population of the menu
        repopulate(items);
    }

    /**
     * Create a Scroller Inventory with a click and a close action
     *
     * @param items An ArrayList of ItemStacks to be added to the Inventory - will be paginated if required
     * @param name  The name of the ScrollerInventory
     * @param click A lambda function to execute upon the user clicking an item (does not work for nextPage or previousPage)
     * @param close A lambda function to execute upon the user closing the ScrollerInventory
     */
    public ScrollerInventory(ArrayList<ItemStack> items, Component name, @Nullable onClick click, @Nullable onClose close) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.click = click;
        this.close = close;

        // Setup control items
        setupControlItems();
        // register events for this ScrollerInventory
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        //run the first population of the menu
        repopulate(items);
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (pages.size() > 1)
            return pages.getFirst();
        else return getBlankPage(this.name);
    }

    /**
     * Set up the next and previous buttons
     */
    private void setupControlItems() {
        ItemMeta meta = previousPage.getItemMeta();
        meta.customName(Component.text("Previous Page").color(NamedTextColor.LIGHT_PURPLE));
        previousPage.setItemMeta(meta);

        meta = nextPage.getItemMeta();
        meta.customName(Component.text("Next Page").color(NamedTextColor.LIGHT_PURPLE));
        nextPage.setItemMeta(meta);
    }

    /**
     * Creates a blank page with the next and prev buttons
     *
     * @param name The Name of the page/Inventory
     * @return The Inventory object of the page
     */
    private @NotNull Inventory getBlankPage(@NotNull Component name) {
        Inventory page = plugin.getServer().createInventory(this, 54, name);

        page.setItem(53, this.nextPage);
        page.setItem(45, this.previousPage);

        return page;
    }

    /**
     * Used to repopulate the ScrollerInventory with items
     *
     * @param items An ArrayList of ItemStacks to use in the ScrollerInventory
     */
    public void repopulate(@NotNull ArrayList<ItemStack> items) {
        pages.clear();
        currentPage = 0;

        int itemsPerPagedInventory = 45;

        // If we don't need pagination
        if (items.size() <= itemsPerPagedInventory) {

            int requiredRows = (int) Math.ceil(items.size() / 9.0);
            if (requiredRows == 0) requiredRows = 1;

            int inventorySize = requiredRows * 9;

            Inventory page = plugin.getServer().createInventory(this, inventorySize, name);

            for (ItemStack item : items) {
                page.addItem(item);
            }

            pages.add(page);
            return;
        }

        // Pagination required
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

    /**
     * Close the ScrollerInventory for a Player
     *
     * @param player the player to close the ScrollerInventory for
     */
    public void close(@NotNull Player player) {
        if (!viewers.containsKey(player.getUniqueId())) return;
        if (player.getOpenInventory().getTopInventory().getHolder(false) instanceof ScrollerInventory) {
            viewers.remove(player.getUniqueId());
            player.closeInventory();
        }
    }

    /**
     * Open the ScrollerInventory for a Player
     *
     * @param player the player to open the ScrollerInventory for
     */
    public void open(Player player) {
        player.openInventory(pages.getFirst());
        viewers.put(player.getUniqueId(), this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(@NotNull InventoryClickEvent event) {
        Inventory inventory = event.getClickedInventory();
        if (inventory == null || !(inventory.getHolder(false) instanceof ScrollerInventory scrollerInventory)) return;
        if (scrollerInventory.getId() != this.id) return;
        if (!(event.getWhoClicked() instanceof Player p)) return;

        //Get the current scroller inventory the player is looking at, if the player is looking at one.
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        if (item.getItemMeta() == null) return;
        if (item.getItemMeta().customName() == null) return;
        //ALWAYS cancel the click event
        event.setCancelled(true);
        //If the pressed item was a nextPage button
        if (Objects.equals(item.getItemMeta().customName(), this.nextPageName)){
            //If there is no next page, don't do anything
            if (scrollerInventory.currentPage < scrollerInventory.pages.size() - 1) {
                //Next page exists, flip the page
                scrollerInventory.currentPage += 1;
                p.openInventory(scrollerInventory.pages.get(scrollerInventory.currentPage));
            }
            return;
            //if the pressed item was a previous page button
        } else if (Objects.equals(item.getItemMeta().customName(), this.previousPageName)){
            //If the page number is more than 0 (So a previous page exists)
            if (scrollerInventory.currentPage > 0) {
                //Flip to previous page
                scrollerInventory.currentPage -= 1;
                p.openInventory(scrollerInventory.pages.get(scrollerInventory.currentPage));
            }
            return;
        }
        if (event.getCurrentItem() != null) {
            if (this.click != null)
                if (click.click(p, event.getCurrentItem(), this))
                    close(p);
        }
    }

    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player p)) return;

        if (p.getOpenInventory().getTopInventory().getHolder(false) instanceof ScrollerInventory scrollerInventory) {
            if (scrollerInventory.getId() != this.id) return;
            //TODO this may also trigger if the page changes
            if (this.close != null)
                this.close.close(p, this);
            viewers.remove(p.getUniqueId());
        }
    }

    /**
     * Interface used to create a click action
     */
    public interface onClick {
        /**
         * Lambda function used to create a click action
         * @param clicker the Player who clicked the item
         * @param item the ItemStack that was clicked
         * @param scrollerInventory the ScrollerInventory that was clicked
         * @return true to close the inventory, false to leave open
         */
        boolean click(Player clicker, ItemStack item, ScrollerInventory scrollerInventory);
    }

    /**
     * Interface used to create a close action
     */
    public interface onClose {
        /**
         * Lambda function used to create a close action
         *
         * @param closer            the Player who closed the inventory
         * @param scrollerInventory the ScrollerInventory that was closed
         */
        void close(Player closer, ScrollerInventory scrollerInventory);
    }
}
