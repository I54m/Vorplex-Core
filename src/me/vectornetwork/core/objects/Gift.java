package me.vectornetwork.core.objects;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Gift {

    private ItemStack item;
    private UUID sender;

    public Gift(ItemStack item, UUID sender){
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
