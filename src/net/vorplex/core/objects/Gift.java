package net.vorplex.core.objects;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Gift {

    private final ItemStack item;
    private final UUID sender;

    public Gift(ItemStack item, UUID sender) {
        this.item = item;
        this.sender = sender;
    }

    public ItemStack getItem() {
        return item;
    }

    public UUID getSender() {
        return sender;
    }


}
