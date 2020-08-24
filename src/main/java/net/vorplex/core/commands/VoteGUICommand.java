package net.vorplex.core.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.vorplex.core.Main;
import net.vorplex.core.util.BookUtils;
import net.vorplex.core.util.NMSUtils;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.lang.reflect.Method;
import java.util.List;

public class VoteGUICommand implements CommandExecutor {
    private static Main plugin = Main.getInstance();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (plugin.getConfig().getString("VoteBookGUI.votelinks.1") == null) {
            commandSender.sendMessage("vote link #1 is null!!");
            return false;
        }
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("sorry you must be a player to use this command!");
            return false;
        }
        if (s.equalsIgnoreCase("vote")) {
            Player player = (Player) commandSender;
            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
            BookMeta bookMeta = (BookMeta) book.getItemMeta();
            ComponentBuilder text = new ComponentBuilder("        Vote").strikethrough(false).bold(true).color(ChatColor.DARK_GREEN)
                    .append("\n------------------").strikethrough(true).bold(false).color(ChatColor.BLACK)
                    .append("\nVote for the server to gain rewards!\n").strikethrough(false).color(ChatColor.BLACK);
            for (String id : plugin.getConfig().getConfigurationSection("VoteBookGUI.votelinks").getKeys(false)){
                text.append("\n>> ").strikethrough(false).bold(true).color(ChatColor.BLACK)
                        .append("Link " + id).strikethrough(false).bold(false).color(ChatColor.DARK_GREEN)
                        .event(new ClickEvent(ClickEvent.Action.OPEN_URL, plugin.getConfig().getString("VoteBookGUI.votelinks." + id)))
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Go to vote link " + id + "!").create()));
            }
            List<Object> pages;
            try {
                pages = (List<Object>) NMSUtils.getBukkitClass("inventory.CraftMetaBook").getDeclaredField("pages").get(bookMeta);
            } catch (ReflectiveOperationException ex) {
                ex.printStackTrace();
                return false;
            }
            for (Class<?> clazz : NMSUtils.getNMSClass("IChatBaseComponent").getDeclaredClasses()) {
                try {
                    Method method = clazz.getDeclaredMethod("a", String.class);
                    if (method != null) {
                        pages.add(method.invoke(clazz, ComponentSerializer.toString(text.create())));
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            bookMeta.setTitle("/vote GUI");
            bookMeta.setAuthor("Vorplex");
            book.setItemMeta(bookMeta);

            BookUtils.openBook(book, player);
            return true;
        }
        return false;
    }
}

