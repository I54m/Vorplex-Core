package net.vorplex.core.autopickup;

import lombok.Getter;
import net.vorplex.core.VorplexCore;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AutoPickupConfig {

    private final VorplexCore plugin = VorplexCore.getInstance();
    @Getter
    private ArrayList<Material> allowedItems = new ArrayList<>();
    @Getter
    private ArrayList<UUID> disabledPlayers = new ArrayList<>();
    @Getter
    private boolean inventoryFullSound;
    @Getter
    private boolean enabled;

    public AutoPickupConfig() {
        enabled = plugin.getConfig().getBoolean("AutoPickup.enabled", true);
        inventoryFullSound = plugin.getConfig().getBoolean("AutoPickup.InventoryFullSound");
        List<String> configList = (List<String>) plugin.getConfig().getList("AutoPickup.AllowedItems");
        if (configList == null || configList.isEmpty()) {
            plugin.getComponentLogger().error("AllowedItems list is empty!");
            plugin.getComponentLogger().error("AutoPickup Module will not be enabled!");
            return;
        }
        for (String item : configList) {
            Material material = Material.matchMaterial(item);
            if (material == null) {
                plugin.getComponentLogger().error("Could not find matching material for {}", item);
                continue;
            }
            allowedItems.add(material);
        }
    }
}
