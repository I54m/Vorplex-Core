package net.vorplex.core.listeners;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.vorplex.core.VorplexCore;
import net.vorplex.core.autopickup.AutoPickupConfig;
import net.vorplex.core.util.Debug;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class BlockBreak implements Listener {

    private final VorplexCore plugin = VorplexCore.getInstance();
    private final AutoPickupConfig config = plugin.getAutoPickupConfig();
    private final Map<UUID, Long> lastInventoryFull = new HashMap<>();

    private static final EnumSet<Material> UPWARDS_PLANTS = EnumSet.of(
            Material.SUGAR_CANE,
            Material.KELP,
            Material.BAMBOO,
            Material.TWISTING_VINES,
            Material.KELP_PLANT,
            Material.TWISTING_VINES_PLANT
    );
    private static final EnumSet<Material> DOWNWARDS_PLANTS = EnumSet.of(
            Material.CAVE_VINES,
            Material.PALE_HANGING_MOSS,
            Material.WEEPING_VINES,
            Material.CAVE_VINES_PLANT,
            Material.WEEPING_VINES_PLANT
    );

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
        if (!player.hasPermission("vorplexcore.autoitempickup")) return;

        Debug.log("handling block break for: " + block.getType() + " At x:" + block.getX() + " y:" + block.getY() + " z:" + block.getZ());
        giveOrDrop(player, block);
        handleVerticalPlant(player, block);
        event.setDropItems(false);
    }

    private void handleVerticalPlant(Player player, Block block) {
        Block current = block;
        Material origType = block.getType();
        Material secondaryType = origType.toString().contains("_PLANT") ?
                Material.matchMaterial(origType.toString().replace("_PLANT", "")) :
                Material.matchMaterial(origType + "_PLANT");
        BlockFace direction;
        if (UPWARDS_PLANTS.contains(block.getType())) {
            Debug.log("direction=up");
            direction = BlockFace.UP;
        } else if (DOWNWARDS_PLANTS.contains(block.getType())) {
            Debug.log("direction=down");
            direction = BlockFace.DOWN;
        } else {
            Debug.log("not in either list");
            return;
        }

        while (current.getType() == origType || current.getType() == secondaryType) {
            current.setType(Material.AIR);
            Block next = current.getRelative(direction);
            Debug.log("Next block y = " + next.getY());
            if (next.getType() != origType && next.getType() != secondaryType) {
                Debug.log("next block is not same type. next type = " + next.getType());
                break;
            }

            giveOrDrop(player, next);
            current = next;
        }
    }

    private void giveOrDrop(Player player, Block block) {
        Debug.log("giving drops for " + block.getType());
        ItemStack tool = player.getInventory().getItemInMainHand();
        Collection<ItemStack> drops = block.getDrops(tool, player);

        boolean inventoryFull = false;

        for (ItemStack drop : drops) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(drop);

            if (!leftover.isEmpty()) inventoryFull = true;

            leftover.values().forEach(item ->
                    block.getWorld().dropItemNaturally(block.getLocation(), item)
            );
        }

        if (inventoryFull) notifyInventoryFull(player);
    }

    private void notifyInventoryFull(Player player) {
        long now = System.currentTimeMillis();
        long last = lastInventoryFull.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < 2000) return;

        lastInventoryFull.put(player.getUniqueId(), now);
        player.sendActionBar(Component.text("Your Inventory is full!").color(NamedTextColor.RED));
        if (config.isInventoryFullSound())
            player.playSound(Sound.sound(Key.key("entity.player.teleport"), Sound.Source.MASTER, 1f, 1f));
    }
}
