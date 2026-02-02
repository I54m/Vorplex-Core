package net.vorplex.core.listeners;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.vorplex.core.VorplexCore;
import net.vorplex.core.autopickup.AutoPickupConfig;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Map;

public class BlockBreak implements Listener {

    private final VorplexCore plugin = VorplexCore.getInstance();
    private final AutoPickupConfig config = plugin.getAutoPickupConfig();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreakEvent(BlockBreakEvent event) {
        if (!config.isEnabled()) return;
        if (!config.getAllowedItems().contains(event.getBlock().getType())) return;
        if (config.getDisabledPlayers().contains(event.getPlayer().getUniqueId())) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();
        //disallow blocks with inventories to prevent dupes
        if (block.getState() instanceof InventoryHolder) return;
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return;
        if (!player.hasPermission("vorplexcore.autopickup")) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        Collection<ItemStack> drops = block.getDrops(tool, player);
        event.setDropItems(false);

        boolean inventoryFull = false;

        for (ItemStack drop : drops) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(drop);

            if (!leftover.isEmpty()) inventoryFull = true;

            leftover.values().forEach(item ->
                    block.getWorld().dropItemNaturally(block.getLocation(), item)
            );
        }

        if (inventoryFull) {
            if (config.isInventoryFullSound())
                player.playSound(Sound.sound(Key.key("entity.player.teleport"), Sound.Source.MASTER, 1f, 1f));
            player.sendActionBar(Component.text("Your Inventory is full!").color(NamedTextColor.RED));
        }
    }
}
