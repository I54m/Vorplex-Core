package net.vorplex.core.objects;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.vorplex.core.VorplexCore;
import net.vorplex.core.util.Debug;
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

    private final ItemStack nextPage = ItemStack.of(Material.ARROW, 1);
    private final ItemStack previousPage = ItemStack.of(Material.ARROW, 1);
    private final ItemStack fillerItem = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE, 1);

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
        Debug.log("Created new ScrollerInventory: " + this);
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
        Debug.log("Created new ScrollerInventory: " + this);
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
        Debug.log("Created new ScrollerInventory: " + this);
    }

    @Override
    public @NotNull Inventory getInventory() {
        //TODO may need to fetch current page so click actions work correctly
        if (!pages.isEmpty())
            return pages.getFirst();
        else return getBlankPage(this.name);
    }

    @Override
    public String toString() {
        StringBuilder pageSummary = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            Inventory page = pages.get(i);
            pageSummary.append("{index=")
                    .append(i)
                    .append(", size=")
                    .append(page.getSize())
                    .append("}");

            if (i < pages.size() - 1) {
                pageSummary.append(", ");
            }
        }

        return "ScrollerInventory{" +
                "id=" + id +
                ", name=" + PlainTextComponentSerializer.plainText().serialize(name) +
                ", currentPage=" + currentPage +
                ", totalPages=" + pages.size() +
                ", pages=[" + pageSummary + "]" +
                ", viewers=" + viewers.keySet() +
                ", clickHandler=" + (click != null) +
                ", closeHandler=" + (close != null) +
                '}';
    }

    /**
     * Set up the next and previous buttons
     */
    private void setupControlItems() {
        ItemMeta meta = previousPage.getItemMeta();
        meta.customName(Component.text("<- Previous Page").color(NamedTextColor.LIGHT_PURPLE));
        previousPage.setItemMeta(meta);

        meta = nextPage.getItemMeta();
        meta.customName(Component.text("Next Page ->").color(NamedTextColor.LIGHT_PURPLE));
        nextPage.setItemMeta(meta);

        meta = fillerItem.getItemMeta();
        meta.setHideTooltip(true);
        fillerItem.setItemMeta(meta);
    }

    /**
     * Creates a blank page with the next and prev buttons
     *
     * @param name The Name of the page/Inventory
     * @return The Inventory object of the page
     */
    private @NotNull Inventory getBlankPage(@NotNull Component name) {
        Inventory page = plugin.getServer().createInventory(this, 54, name);

        page.setItem(45, this.previousPage);
        page.setItem(46, this.fillerItem);
        page.setItem(47, this.fillerItem);
        page.setItem(48, this.fillerItem);
        page.setItem(49, this.fillerItem);
        page.setItem(50, this.fillerItem);
        page.setItem(51, this.fillerItem);
        page.setItem(52, this.fillerItem);
        page.setItem(53, this.nextPage);

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

        Debug.log("Repopulate triggered for scrollerInventory with id: " + this.id + " Items: " + items);
        int itemsPerPagedInventory = 45;

        // If we don't need pagination
        if (items.size() <= itemsPerPagedInventory) {

            int requiredRows = (int) Math.ceil(items.size() / 9.0);
            if (requiredRows == 0) requiredRows = 1;

            int inventorySize = requiredRows * 9;
            Debug.log("No Pagination required - Items will fit in a " + inventorySize + " Size inventory");
            Inventory page = plugin.getServer().createInventory(this, inventorySize, name);

            int index = 0;
            for (ItemStack item : items) {
                page.setItem(index, item);
                index++;
            }

            pages.add(page);
            return;
        }

        // Pagination required
        Debug.log("Pagination required - creating pages...");
        //create new blank page
        Inventory page = getBlankPage(name);
        int index = 0;
        //According to the items in the arraylist, add items to the ScrollerInventory
        for (ItemStack item : items) {
            //If the current page is full, add the page to the inventory's pages arraylist, and create a new page to add the items.
            if (index >= 45) {
                pages.add(page);
                page = getBlankPage(name);
                index = 0;
            }
            page.setItem(index, item);
            index++;
        }
        pages.add(page);
        Debug.log("Created " + pages.size() + " total pages.");

    }

    /**
     * Close the ScrollerInventory for a Player
     *
     * @param player the player to close the ScrollerInventory for
     */
    public void close(@NotNull Player player) {
        if (!viewers.containsKey(player.getUniqueId())) return;
        if (player.getOpenInventory().getTopInventory().getHolder(false) instanceof ScrollerInventory) {
            Debug.log("Closing scroller inventory for " + player.getName());
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
        Debug.log("Opening scroller inventory for " + player.getName());
        player.openInventory(pages.getFirst());
        viewers.put(player.getUniqueId(), this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(@NotNull InventoryClickEvent event) {
        Inventory inventory = event.getClickedInventory();
        if (inventory == null || !(inventory.getHolder(false) instanceof ScrollerInventory scrollerInventory)) return;
        if (scrollerInventory.getId() != this.id) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        //ALWAYS cancel the click event in a scroller inventory
        event.setCancelled(true);
        // check if there is an item clicked and if there is a click action associated with the scroller inventory, then execute it
        if (event.getCurrentItem() != null)
            if (this.click != null) {
                Debug.log("Running click action for player " + player.getName() + " with item: " + event.getCurrentItem());
                if (click.click(player, event.getCurrentItem(), this)) {
                    Debug.log("Click action returned true - closing scroller inventory");
                    close(player);
                }
            }

        //Get the current scroller inventory the player is looking at, if the player is looking at one.
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        if (item.getItemMeta() == null) return;
        if (item.getItemMeta().customName() == null) return;

        //If the pressed item was a nextPage button
        if (item.equals(this.nextPage)) {
            Debug.log("Next page button was clicked");
            //If there is no next page, don't do anything
            if (scrollerInventory.currentPage < scrollerInventory.pages.size() - 1) {
                Debug.log("Next page available, incrementing current page and opening new page");
                //Next page exists, flip the page
                scrollerInventory.currentPage += 1;
                player.openInventory(scrollerInventory.pages.get(scrollerInventory.currentPage));
                return;
            }
            Debug.log("No next page to go to");
            //if the pressed item was a previous page button
        } else if (item.equals(this.previousPage)) {
            Debug.log("Previous page button was clicked");
            //If the page number is more than 0 (So a previous page exists)
            if (scrollerInventory.currentPage > 0) {
                Debug.log("Previous page available, decrementing current page and opening new page");
                //Flip to previous page
                scrollerInventory.currentPage -= 1;
                player.openInventory(scrollerInventory.pages.get(scrollerInventory.currentPage));
                return;
            }
            Debug.log("No previous page to go to");
        }
    }

    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        if (event.getInventory().getHolder(false) instanceof ScrollerInventory scrollerInventory) {
            if (scrollerInventory.getId() != this.id) return;
            //TODO this may also trigger if the page changes test with a close action to decide if this needs to be changed
            if (this.close != null) {
                Debug.log("Running close action for player " + player.getName());
                this.close.close(player, this);
            }
            viewers.remove(player.getUniqueId());
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
