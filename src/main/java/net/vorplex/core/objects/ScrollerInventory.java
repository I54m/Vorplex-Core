package net.vorplex.core.objects;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.vorplex.core.VorplexCore;
import net.vorplex.core.util.Debug;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class ScrollerInventory implements Listener, InventoryHolder {

    // Constants
    private final VorplexCore plugin = VorplexCore.getInstance();
    private final int ITEMS_PER_PAGE = 45;
    private final ItemStack nextPage = ItemStack.of(Material.ARROW, 1);
    private final ItemStack previousPage = ItemStack.of(Material.ARROW, 1);
    private final ItemStack fillerItem = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE, 1);
    private final NamespacedKey nextPageKey = new NamespacedKey(plugin, "next_page");
    private final NamespacedKey previousPageKey = new NamespacedKey(plugin, "previous_page");
    private final NamespacedKey fillerItemKey = new NamespacedKey(plugin, "filler_item");

    // Constructor variables
    @Getter
    private final UUID id;
    @Getter
    private final Component name;
    private final ArrayList<ItemStack> items;
    @Nullable
    private final ClickAction clickAction;
    @Nullable
    private final CloseAction closeAction;

    // Storage
    @Getter
    private final HashMap<UUID, Page> viewers = new HashMap<>();

    /**
     * Create a Scroller Inventory without any click or close actions
     *
     * @param name  The name of the ScrollerInventory
     * @param items An ArrayList of ItemStacks to be added to the Inventory - will be paginated if required
     */
    public ScrollerInventory(Component name, ArrayList<ItemStack> items) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.items = items;
        this.clickAction = null;
        this.closeAction = null;

        // register events for this ScrollerInventory
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        //setup control Items
        setupControlItems();

        Debug.log("Created new ScrollerInventory: " + this);
    }

    /**
     * Create a Scroller Inventory with a click action and no close action
     *
     * @param name        The name of the ScrollerInventory
     * @param items       An ArrayList of ItemStacks to be added to the Inventory - will be paginated if required
     * @param clickAction A lambda function to execute upon the user clicking an item (does not work for nextPage or previousPage)
     */
    public ScrollerInventory(Component name, ArrayList<ItemStack> items, @Nullable ClickAction clickAction) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.items = items;
        this.clickAction = clickAction;
        this.closeAction = null;

        // register events for this ScrollerInventory
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        //setup control Items
        setupControlItems();

        Debug.log("Created new ScrollerInventory: " + this);
    }

    /**
     * Create a Scroller Inventory with a click and a close action
     *
     * @param name        The name of the ScrollerInventory
     * @param items       An ArrayList of ItemStacks to be added to the Inventory - will be paginated if required
     * @param clickAction A lambda function to execute upon the user clicking an item (does not work for nextPage or previousPage)
     * @param closeAction A lambda function to execute upon the user closing the ScrollerInventory
     */
    public ScrollerInventory(Component name, ArrayList<ItemStack> items, @Nullable ClickAction clickAction, @Nullable CloseAction closeAction) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.items = items;
        this.clickAction = clickAction;
        this.closeAction = closeAction;

        // register events for this ScrollerInventory
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        //setup control Items
        setupControlItems();

        Debug.log("Created new ScrollerInventory: " + this);
    }

    @Override
    public @NotNull Inventory getInventory() {
        int inventorySize;
        if (this.items.size() > ITEMS_PER_PAGE) {
            inventorySize = 54;
        } else {
            int rows = (int) Math.ceil(this.items.size() / 9.0);
            if (rows == 0) rows = 1;
            inventorySize = rows * 9;
        }
        return renderPage(plugin.getServer().createInventory(this, inventorySize, name), 0);
    }

    @Override
    public String toString() {
        return "ScrollerInventory{" +
                "id=" + this.id +
                ", name=" + PlainTextComponentSerializer.plainText().serialize(this.name) +
                ", items=" + this.items +
                ", clickAction=" + (this.clickAction != null) +
                ", closeAction=" + (this.closeAction != null) +
                ", viewers=" + this.viewers +
                '}';
    }

    /**
     * Set up the next and previous buttons and the filler item
     */
    private void setupControlItems() {
        Debug.log("Setting up control items used for pagination");
        ItemMeta meta = this.previousPage.getItemMeta();
        meta.customName(Component.text("<- Previous Page").color(NamedTextColor.LIGHT_PURPLE));
        meta.getPersistentDataContainer().set(this.previousPageKey, PersistentDataType.BYTE, (byte) 1);
        this.previousPage.setItemMeta(meta);

        meta = this.nextPage.getItemMeta();
        meta.customName(Component.text("Next Page ->").color(NamedTextColor.LIGHT_PURPLE));
        meta.getPersistentDataContainer().set(this.nextPageKey, PersistentDataType.BYTE, (byte) 1);
        this.nextPage.setItemMeta(meta);

        meta = this.fillerItem.getItemMeta();
        meta.setHideTooltip(true);
        meta.getPersistentDataContainer().set(this.fillerItemKey, PersistentDataType.BYTE, (byte) 1);
        this.fillerItem.setItemMeta(meta);
    }

    /**
     * Place the control buttons and filler item into the inventory
     * @param page the inventory to place the items
     */
    private void setupControls(Inventory page) {
        Debug.log("Adding pagination control items to inventory");
        page.setItem(45, this.previousPage);
        page.setItem(46, this.fillerItem);
        page.setItem(47, this.fillerItem);
        page.setItem(48, this.fillerItem);
        page.setItem(49, this.fillerItem);
        page.setItem(50, this.fillerItem);
        page.setItem(51, this.fillerItem);
        page.setItem(52, this.fillerItem);
        page.setItem(53, this.nextPage);
    }

    /**
     * Render the page for a specific player
     * @param player the player to render the page for
     */
    private void renderPage(Player player) {
        UUID uuid = player.getUniqueId();
        Inventory inv = this.viewers.get(uuid).getInventory();
        int page = this.viewers.get(uuid).getPageNumber();

        if (inv == null) return;

        renderPage(inv, page);
    }

    /**
     * Render a specific page without requiring a player
     *
     * @param inv  the inventory to render the page to
     * @param page the page the render
     * @return     the fully rendered inventory page
     */
    private Inventory renderPage(@NotNull Inventory inv, int page) {
        // Clear inventory
        inv.clear();

        if (this.items.size() <= ITEMS_PER_PAGE) {
            int slot = 0;
            for (ItemStack item : this.items) {
                inv.setItem(slot++, item);
            }
            return inv;
        }

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, this.items.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            inv.setItem(slot++, this.items.get(i));
        }

        setupControls(inv);
        return inv;
    }

    /**
     * Close the ScrollerInventory for a Player
     *
     * @param player the player to close the ScrollerInventory for
     */
    public void close(@NotNull Player player) {
        if (!this.viewers.containsKey(player.getUniqueId())) return;
        if (player.getOpenInventory().getTopInventory().getHolder(false) instanceof ScrollerInventory) {
            Debug.log("Closing scroller inventory for " + player.getName());
            this.viewers.remove(player.getUniqueId());
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
        final int inventorySize;

        if (this.items.size() > ITEMS_PER_PAGE) {
            inventorySize = 54;
            Debug.log("Pagination required - pages will be rendered as needed");
        } else {
            int rows = (int) Math.ceil(this.items.size() / 9.0);
            if (rows == 0) rows = 1;
            inventorySize = rows * 9;
            Debug.log("No Pagination required - Items will fit in a " + inventorySize + " Size inventory");
        }

        Inventory inv = plugin.getServer().createInventory(this, inventorySize, this.name);
        this.viewers.put(player.getUniqueId(), new Page(inv, 0));
        player.openInventory(inv);

        renderPage(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(@NotNull InventoryClickEvent event) {
        Inventory inventory = event.getView().getTopInventory();
        if (!(inventory.getHolder(false) instanceof ScrollerInventory scrollerInventory)) return;
        if (scrollerInventory.getId() != this.id) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Page page = this.viewers.get(player.getUniqueId());
        int currentPage = page.getPageNumber();
        //ALWAYS cancel the click event in a scroller inventory
        event.setCancelled(true);

        // player clicked their own inventory or outside the inventory, event is already canceled, no need to continue
        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(inventory)) return;

        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        //If the pressed item was a nextPage button
        if (meta != null && meta.getPersistentDataContainer().has(this.nextPageKey, PersistentDataType.BYTE)) {
            Debug.log("Next page button was clicked");
            //If there is no next page, don't do anything
            if (currentPage < (this.items.size() - 1) / 45) {
                Debug.log("Next page available, incrementing current page and opening new page");
                //Next page exists, flip the page and add player to switching pages list
                page.setPageNumber(currentPage + 1);
                this.viewers.put(player.getUniqueId(), page);
                renderPage(player);
                return;
            }
            Debug.log("No next page to go to");
            return;
            //if the pressed item was a previous page button
        } else if (meta != null && meta.getPersistentDataContainer().has(this.previousPageKey, PersistentDataType.BYTE)) {
            Debug.log("Previous page button was clicked");
            //If the page number is more than 0 (So a previous page exists)
            if (currentPage > 0) {
                Debug.log("Previous page available, decrementing current page and opening new page");
                //Flip to previous page and add player to switching pages list
                page.setPageNumber(currentPage - 1);
                this.viewers.put(player.getUniqueId(), page);
                renderPage(player);
                return;
            }
            Debug.log("No previous page to go to");
            return;
        }

        // prevent click action triggering on filler item
        if (meta != null && meta.getPersistentDataContainer().has(this.fillerItemKey, PersistentDataType.BYTE)) return;

        // check if there is a click action associated with the scroller inventory, then execute it
        if (this.clickAction != null) {
            Debug.log("Running click action for player " + player.getName() + " with click type: " + event.getClick() + " with item: " + event.getCurrentItem());
            if (this.clickAction.click(player, event.getClick(), event.getCurrentItem(), this)) {
                Debug.log("Click action returned true - closing scroller inventory");
                close(player);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        if (event.getInventory().getHolder(false) instanceof ScrollerInventory scrollerInventory) {
            if (scrollerInventory.getId() != this.id) return;
            if (this.closeAction != null) {
                Debug.log("Running close action for player " + player.getName());
                this.closeAction.close(player, event.getReason(), this);
            }
            this.viewers.remove(player.getUniqueId());
        }
    }

    /**
     * A simple object used to store both an inventory and the page number that is currently rendered
     */
    public static class Page {

        @Getter
        @Setter
        private int pageNumber;
        @Getter
        private final Inventory inventory;

        /**
         * A simple object used to store both an inventory and the page number that is currently rendered
         *
         * @param inventory  the inventory currently open
         * @param pageNumber the current page number that is rendered
         */
        public Page(Inventory inventory, int pageNumber) {
            this.pageNumber = pageNumber;
            this.inventory = inventory;
        }
    }

    /**
     * Interface used to create a click action
     */
    public interface ClickAction {
        /**
         * Lambda function used to process a click action - will not trigger on filler item or page buttons
         * @param clicker           the Player who clicked the item
         * @param clickType         the type of click the player made on the item
         * @param item              the ItemStack that was clicked
         * @param scrollerInventory the ScrollerInventory that was clicked
         * @return                  true to close the inventory, false to leave open
         */
        boolean click(Player clicker, ClickType clickType, ItemStack item, ScrollerInventory scrollerInventory);
    }

    /**
     * Interface used to create a close action
     */
    public interface CloseAction {
        /**
         * Lambda function used to process a close action - will not trigger on page change
         *
         * @param closer            the Player who closed the inventory
         * @param reason            the reason the inventory was closed
         * @param scrollerInventory the ScrollerInventory that was closed
         */
        void close(Player closer, InventoryCloseEvent.Reason reason, ScrollerInventory scrollerInventory);
    }
}
