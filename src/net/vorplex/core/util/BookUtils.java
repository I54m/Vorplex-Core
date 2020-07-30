package net.vorplex.core.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;

public class BookUtils {
    private static Constructor<?> serializerConstructor;
    private static Object bookKey;
    private static Constructor<?> titleConstructor;
    private static boolean old;


    public static void init() {
        try {
            if (Bukkit.getServer().getVersion().contains("1.7") ||
                    Bukkit.getServer().getVersion().contains("1.8") ||
                    Bukkit.getServer().getVersion().contains("1.9") ||
                    Bukkit.getServer().getVersion().contains("1.10") ||
                    Bukkit.getServer().getVersion().contains("1.11") ||
                    Bukkit.getServer().getVersion().contains("1.12")) old = true;
            serializerConstructor = NMSUtils.getNMSClass("PacketDataSerializer").getConstructor(ByteBuf.class);
            if (old) {
                bookKey = "MC|BOpen";
                titleConstructor = NMSUtils.getNMSClass("PacketPlayOutCustomPayload").getConstructor(String.class, NMSUtils.getNMSClass("PacketDataSerializer"));
            } else {
                Constructor<?> keyConstructor = NMSUtils.getNMSClass("MinecraftKey").getConstructor(String.class);
                bookKey = keyConstructor.newInstance("minecraft:book_open");
                titleConstructor = NMSUtils.getNMSClass("PacketPlayOutCustomPayload").getConstructor(NMSUtils.getNMSClass("MinecraftKey"), NMSUtils.getNMSClass("PacketDataSerializer"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void openBook(ItemStack book, Player p) {
        int slot = p.getInventory().getHeldItemSlot();
        ItemStack oldItem = p.getInventory().getItem(slot) != null ? p.getInventory().getItem(slot).clone() : new ItemStack(Material.AIR);
        p.getInventory().setItem(slot, book);

        ByteBuf buf = Unpooled.buffer(256);
        buf.setByte(0, (byte) 0);
        buf.writerIndex(1);

        try {
            Object packetDataSerializer = serializerConstructor.newInstance(buf);

            Object payload = titleConstructor.newInstance(old ? (String) bookKey : bookKey, packetDataSerializer);

            NMSUtils.sendPacket(p, payload);
        } catch (Exception e) {
            e.printStackTrace();
        }

        p.getInventory().setItem(slot, oldItem);
    }
}
