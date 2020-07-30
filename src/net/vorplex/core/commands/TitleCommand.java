package net.vorplex.core.commands;

import net.vorplex.core.Main;
import net.vorplex.core.objects.IconMenu;
import net.vorplex.core.objects.ScrollerInventory;
import net.vorplex.core.util.NameFetcher;
import net.vorplex.core.util.UUIDFetcher;
import net.vorplex.core.util.UserFetcher;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TitleCommand implements CommandExecutor {
    private final Main plugin = Main.getInstance();


    @SuppressWarnings("NullableProblems")
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("You must be a player to use this command!");
            return false;
        }
        Player player = (Player) commandSender;
        if (!player.hasPermission("vorplexcore.titles.command")) {
            player.sendMessage(ChatColor.RED + "You do not have permission for this command!");
            return false;
        }
        try {
            if (strings.length <= 0) {
                TreeMap<Integer, String> titles = new TreeMap<>();
                for (int titleid : plugin.titles.keySet()) {
                    if (player.hasPermission("vorplexcore.titles." + titleid)) {
                        titles.put(titleid, plugin.titles.get(titleid));
                    }
                }
                if (titles.isEmpty()) {
                    player.sendMessage(plugin.prefix + ChatColor.RED + "You do not have any titles to change!");
                    return false;
                }
                if (titles.size() <= 56) {
                    IconMenu menu = new IconMenu(ChatColor.LIGHT_PURPLE + player.getName() + "'s Unlocked Titles", 1, (clicker, menu1, slot, item) -> {
                        if (clicker == player) {
                            if (item == null || !item.hasItemMeta()) return false;
                            if (item.getItemMeta().hasDisplayName()) {
                                List<String> lore = item.getItemMeta().getLore();
                                int titleid;
                                try {
                                    titleid = Integer.parseInt(lore.get(2).substring(13));
                                } catch (NumberFormatException nfe) {
                                    nfe.printStackTrace();
                                    menu1.close(clicker);
                                    clicker.sendMessage(plugin.prefix + ChatColor.RED + "An Error occurred in our database and we were unable to equip that title!");
                                    return false;
                                }
                                try {
                                    String sql = "SELECT * FROM `vorplexcore_equippedtitles` WHERE UUID='" + clicker.getUniqueId().toString() + "';";
                                    PreparedStatement stmt = plugin.connection.prepareStatement(sql);
                                    ResultSet results = stmt.executeQuery();
                                    if (results.next()) {
                                        String sql1 = "UPDATE `vorplexcore_equippedtitles` SET `UUID`='" + clicker.getUniqueId().toString() + "', `TitleID`='" + titleid + "', `RawTitle`='" + plugin.titles.get(titleid) + "' WHERE `UUID`='" + player.getUniqueId().toString() + "';";
                                        PreparedStatement stmt1 = plugin.connection.prepareStatement(sql1);
                                        stmt1.executeUpdate();
                                        stmt1.close();
                                    } else {
                                        String sql1 = "INSERT INTO `vorplexcore_equippedtitles` (`UUID`, `TitleID`, `RawTitle`)" +
                                                " VALUES ('" + player.getUniqueId().toString() + "','" + titleid + "','" + plugin.titles.get(titleid) + "');";
                                        PreparedStatement stmt1 = plugin.connection.prepareStatement(sql1);
                                        stmt1.executeUpdate();
                                        stmt1.close();
                                    }

                                } catch (SQLException sqle) {
                                    sqle.printStackTrace();
                                    menu1.close(clicker);
                                    clicker.sendMessage(plugin.prefix + ChatColor.RED + "An Error occurred in our database and we were unable to equip that title!");
                                    return true;
                                }
                                menu1.close(clicker);
                                plugin.equippedTitles.put(clicker.getUniqueId(), plugin.titles.get(titleid));
                                clicker.sendMessage(plugin.prefix + ChatColor.LIGHT_PURPLE + "Set your equipped title to: " + ChatColor.translateAlternateColorCodes('&', item.getItemMeta().getDisplayName()));
                                return true;
                            }
                        }
                        return false;
                    });
                    int position = 1;
                    menu.addButton(0, new ItemStack(Material.BARRIER, 1), ChatColor.GRAY + "" + ChatColor.ITALIC + "*no title*",
                            ChatColor.LIGHT_PURPLE + "Click this to unequip your title.",
                            ChatColor.WHITE + "",
                            ChatColor.WHITE + "Title id: #0");
                    for (int id : titles.navigableKeySet()) {
                        if (id > 0) {
                            menu.addButton(position, new ItemStack(Material.PAPER, 1), ChatColor.translateAlternateColorCodes('&', titles.get(id)),
                                    ChatColor.LIGHT_PURPLE + "Click this to change your",
                                    ChatColor.LIGHT_PURPLE + "equipped title to: " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', titles.get(id)),
                                    ChatColor.WHITE + "Title id: #" + id);
                            position++;
                        }
                        if (position >= 54) {
                            break;
                        } else if (position == 45) {
                            menu.setSize(6);
                        } else if (position == 36) {
                            menu.setSize(5);
                        } else if (position == 27) {
                            menu.setSize(4);
                        } else if (position == 18) {
                            menu.setSize(3);
                        } else if (position == 9) {
                            menu.setSize(2);
                        }

                    }
                    menu.open(player);
                } else {
                    ArrayList<ItemStack> items = new ArrayList<>();
                    ItemStack notitle = new ItemStack(Material.BARRIER, 1);
                    ItemMeta ntim = notitle.getItemMeta();
                    ntim.setDisplayName(ChatColor.GRAY + "" + ChatColor.ITALIC + "*no title*");
                    List<String> ntlore = new ArrayList<>();
                    ntlore.add(ChatColor.LIGHT_PURPLE + "Click this to unequip your title.");
                    ntlore.add(ChatColor.WHITE + "");
                    ntlore.add(ChatColor.WHITE + "Title id: #0");
                    ntim.setLore(ntlore);
                    notitle.setItemMeta(ntim);
                    items.add(notitle);
                    for (int id : titles.navigableKeySet()) {
                        if (id > 0) {
                            ItemStack item = new ItemStack(Material.PAPER, 1);
                            ItemMeta im = item.getItemMeta();
                            im.setDisplayName(ChatColor.translateAlternateColorCodes('&', titles.get(id)));
                            List<String> lore = new ArrayList<>();
                            lore.add(ChatColor.LIGHT_PURPLE + "Click this to change your");
                            lore.add(ChatColor.LIGHT_PURPLE + "equipped title to: " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', titles.get(id)));
                            lore.add(ChatColor.WHITE + "Title id: #" + id);
                            im.setLore(lore);
                            item.setItemMeta(im);
                            items.add(item);
                        }
                    }
                    ScrollerInventory inventory = new ScrollerInventory(items, ChatColor.LIGHT_PURPLE + player.getName() + "'s Unlocked Titles", (clicker, item, scrollerInventory) -> {
                        if (clicker == player) {
                            if (item == null || !item.hasItemMeta()) return false;
                            if (item.getItemMeta().hasDisplayName()) {
                                List<String> lore = item.getItemMeta().getLore();
                                int titleid;
                                try {
                                    titleid = Integer.parseInt(lore.get(2).substring(13));
                                } catch (NumberFormatException nfe) {
                                    nfe.printStackTrace();
                                    clicker.sendMessage(plugin.prefix + ChatColor.RED + "An Error occurred in our database and we were unable to equip that title!");
                                    return true;
                                }
                                try {
                                    String sql = "SELECT * FROM `vorplexcore_equippedtitles` WHERE UUID='" + clicker.getUniqueId().toString() + "';";
                                    PreparedStatement stmt = plugin.connection.prepareStatement(sql);
                                    ResultSet results = stmt.executeQuery();
                                    String rawTitle = plugin.titles.get(titleid);
                                    if (rawTitle.contains("'"))
                                        rawTitle = rawTitle.replace("'", "%sinquo%");
                                    if (rawTitle.contains("\""))
                                        rawTitle = rawTitle.replace("\"", "%dubquo%");
                                    if (rawTitle.contains("`"))
                                        rawTitle = rawTitle.replace("`", "%bcktck%");
                                    if (results.next()) {
                                        String sql1 = "UPDATE `vorplexcore_equippedtitles` SET `UUID`='" + clicker.getUniqueId().toString() + "', `TitleID`='" + titleid
                                                + "', `RawTitle`='" + rawTitle + "' WHERE `UUID`='" + player.getUniqueId().toString() + "';";
                                        PreparedStatement stmt1 = plugin.connection.prepareStatement(sql1);
                                        stmt1.executeUpdate();
                                        stmt1.close();
                                    } else {
                                        String sql1 = "INSERT INTO `vorplexcore_equippedtitles` (`UUID`, `TitleID`, `RawTitle`)" +
                                                " VALUES ('" + player.getUniqueId().toString() + "','" + titleid + "','" + rawTitle + "');";
                                        PreparedStatement stmt1 = plugin.connection.prepareStatement(sql1);
                                        stmt1.executeUpdate();
                                        stmt1.close();
                                    }
                                } catch (SQLException sqle) {
                                    sqle.printStackTrace();
                                    clicker.sendMessage(plugin.prefix + ChatColor.RED + "An Error occurred in our database and we were unable to equip that title!");
                                    return true;
                                }
                                plugin.equippedTitles.put(clicker.getUniqueId(), plugin.titles.get(titleid));
                                clicker.sendMessage(plugin.prefix + ChatColor.LIGHT_PURPLE + "Set your equipped title to: " + ChatColor.translateAlternateColorCodes('&', item.getItemMeta().getDisplayName()));
                                return true;
                            }
                        }
                        return false;
                    });
                    inventory.open(player);
                }
                return true;
            }
            switch (strings[0]) {
                case "create": {
                    if (!player.hasPermission("vorplexcore.titles.create")) {
                        player.sendMessage(ChatColor.RED + "You do not have permission for this command!");
                        return false;
                    }
                    if (strings.length == 1) {
                        player.sendMessage(ChatColor.RED + "Usage: /title create <title with color codes>");
                        return false;
                    } else {
                        StringBuilder titlesb = new StringBuilder();
                        for (int i = 1; i < strings.length; i++) {
                            titlesb.append(strings[i]);
                            if (!(i == strings.length-1))
                                titlesb.append(" ");
                        }
                        String title = titlesb.toString();
                        if (title.contains("'"))
                            title = title.replace("'", "%sinquo%");
                        if (title.contains("\""))
                            title = title.replace("\"", "%dubquo%");
                        if (title.contains("`"))
                            title = title.replace("`", "%bcktck%");
                        String sql1 = "INSERT INTO `vorplexcore_titles` (`RawTitle`)" +
                                " VALUES ('" + title + "');";
                        PreparedStatement stmt1 = plugin.connection.prepareStatement(sql1);
                        stmt1.executeUpdate();
                        stmt1.close();
                        plugin.titles.put(plugin.titles.size(), titlesb.toString());
                        player.sendMessage(plugin.prefix + ChatColor.GREEN + "Created Title: \"" + ChatColor.translateAlternateColorCodes('&', titlesb.toString()) + ChatColor.GREEN + "\" with id: " + (plugin.titles.size()-1) + "!");
                        return true;
                    }
                }
                case "help": {
                    if (!player.hasPermission("vorplexcore.titles.help")) {
                        player.sendMessage(plugin.prefix + ChatColor.RED + "You do not have permission for this command!");
                        return false;
                    }
                    if (player.hasPermission("vorplexcore.titles.create"))
                        player.sendMessage(ChatColor.LIGHT_PURPLE + "Create a new title for players to use. \n" + ChatColor.WHITE + "Usage: /title create <title with color codes>");
                    if (player.hasPermission("vorplexcore.titles.delete"))
                        player.sendMessage(ChatColor.LIGHT_PURPLE + "Delete a title so that no one can use it. \n" + ChatColor.WHITE + "Usage: /title delete <id>");
                    if (player.hasPermission("vorplexcore.titles.list"))
                        player.sendMessage(ChatColor.LIGHT_PURPLE + "List all the titles and their id in plain text. \n" + ChatColor.WHITE + "Usage: /title list [from id]");
                    if (player.hasPermission("vorplexcore.titles.clear"))
                        player.sendMessage(ChatColor.LIGHT_PURPLE + "Clear a player's title. \n" + ChatColor.WHITE + "Usage: /title clear <player>");
                    if (player.hasPermission("vorplexcore.titles.set"))
                        player.sendMessage(ChatColor.LIGHT_PURPLE + "Change a player's title. \n" + ChatColor.WHITE + "Usage: /title set <player>");
                    return true;
                }
                case "delete": {
                    if (!player.hasPermission("vorplexcore.titles.delete")) {
                        player.sendMessage(ChatColor.RED + "You do not have permission for this command!");
                        return false;
                    }
                    if (strings.length == 1) {
                        player.sendMessage(ChatColor.RED + "Usage: /title delete <id>");
                        return false;
                    } else {
                        int id;
                        try {
                            id = Integer.parseInt(strings[1]);
                        } catch (NumberFormatException nfe) {
                            player.sendMessage(plugin.prefix + ChatColor.RED + strings[1] + " is not a number!");
                            return false;
                        }
                        if (id < 1) {
                            player.sendMessage(plugin.prefix + ChatColor.RED + "You cannot delete a title with an id less than 1!");
                            return false;
                        }
                        if (!plugin.titles.containsKey(id)) {
                            player.sendMessage(plugin.prefix + ChatColor.RED + "There is no title with that id!");
                            return false;
                        }
                        player.sendMessage(plugin.prefix + ChatColor.GREEN + "Deleted Title: \"" + ChatColor.translateAlternateColorCodes('&', plugin.titles.get(id)) + ChatColor.GREEN + "\" with id: " + plugin.titles.size() + "!");
                        String sql1 = "DELETE FROM `vorplexcore_titles` WHERE ID='" + id + "'";
                        PreparedStatement stmt1 = plugin.connection.prepareStatement(sql1);
                        stmt1.executeUpdate();
                        stmt1.close();
                        plugin.titles.remove(id);
                        return true;
                    }
                }
                case "list": {
                    if (!player.hasPermission("vorplexcore.titles.list")) {
                        player.sendMessage(ChatColor.RED + "You do not have permission for this command!");
                        return false;
                    }
                    int start = 1;
                    if (strings.length == 2) {
                        try {
                            start = Integer.parseInt(strings[1]);
                        } catch (NumberFormatException nfe) {
                            player.sendMessage(ChatColor.RED + strings[1] + " is not a number!");
                            return false;
                        }
                        if (start >= plugin.titles.keySet().size()) {
                            player.sendMessage(ChatColor.RED + strings[1] + " is higher than the amount of titles!");
                            return false;
                        }
                    }
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "|---------" + ChatColor.DARK_PURPLE + "[" + ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Titles" + ChatColor.DARK_PURPLE + "]" + ChatColor.LIGHT_PURPLE + "---------|");
                    TreeMap<Integer, String> titles2 = new TreeMap<>(plugin.titles);
                    int i = start;
                    for (int id : titles2.navigableKeySet()) {
                        if (id > 0) {
                            if (id >= start) {
                                if (i <= (start + 10)) {
                                    i++;
                                    player.sendMessage(ChatColor.GREEN + "#" + id + ": " + ChatColor.translateAlternateColorCodes('&', plugin.titles.get(id)));
                                }
                            }
                        }
                    }
                    return true;
                }
                case "clear": {
                    if (!player.hasPermission("vorplexcore.titles.clear")) {
                        player.sendMessage(ChatColor.RED + "You do not have permission for this command!");
                        return false;
                    }
                    if (strings.length == 1) {
                        player.sendMessage(ChatColor.RED + "Usage: /title clear <player>");
                        return false;
                    } else {
                        String targetuuid = null;
                        Player findTarget = Bukkit.getPlayerExact(strings[0]);
                        Future<String> future = null;
                        ExecutorService executorService = null;
                        if (findTarget != null) {
                            targetuuid = findTarget.getUniqueId().toString().replace("-", "");
                        } else {
                            UUIDFetcher uuidFetcher = new UUIDFetcher();
                            uuidFetcher.fetch(strings[1]);
                            executorService = Executors.newSingleThreadExecutor();
                            future = executorService.submit(uuidFetcher);
                        }
                        if (future != null) {
                            try {
                                targetuuid = future.get(10, TimeUnit.SECONDS);
                            } catch (Exception e) {
                                e.printStackTrace();
                                player.sendMessage(plugin.prefix + ChatColor.RED + "Unable to fetch player's uuid!");
                                executorService.shutdown();
                                return false;
                            }
                            executorService.shutdown();
                        }
                        UUID targetUUID = UUIDFetcher.formatUUID(targetuuid);
                        if (targetuuid == null) {
                            player.sendMessage(plugin.prefix + ChatColor.RED + "That is not a player's name!");
                            return false;
                        }
                        String targetName = NameFetcher.getName(targetuuid.replace("-", ""));
                        if (targetName == null) {
                            targetName = strings[0];
                        }
                        player.sendMessage(plugin.prefix + ChatColor.GREEN + "Cleared Title for player: " + targetName + "!");
                        String sql1 = "DELETE FROM `vorplexcore_equippedtitles` WHERE UUID='" + targetUUID.toString() + "'";
                        PreparedStatement stmt1 = plugin.connection.prepareStatement(sql1);
                        stmt1.executeUpdate();
                        stmt1.close();
                        plugin.equippedTitles.remove(targetUUID);
                        return true;
                    }
                }
                case "set": {
                    if (!player.hasPermission("vorplexcore.titles.set")) {
                        player.sendMessage(ChatColor.RED + "You do not have permission for this command!");
                        return false;
                    }
                    if (strings.length == 1) {
                        player.sendMessage(ChatColor.RED + "Usage: /title set <player>");
                        return false;
                    }
                    String targetuuid = null;
                    Player findTarget = Bukkit.getPlayerExact(strings[0]);
                    Future<String> future = null;
                    ExecutorService executorService = null;
                    if (findTarget != null) {
                        targetuuid = findTarget.getUniqueId().toString().replace("-", "");
                    } else {
                        UUIDFetcher uuidFetcher = new UUIDFetcher();
                        uuidFetcher.fetch(strings[1]);
                        executorService = Executors.newSingleThreadExecutor();
                        future = executorService.submit(uuidFetcher);
                    }
                    if (future != null) {
                        try {
                            targetuuid = future.get(10, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            e.printStackTrace();
                            player.sendMessage(plugin.prefix + ChatColor.RED + "Unable to fetch player's uuid!");
                            executorService.shutdown();
                            return false;
                        }
                        executorService.shutdown();
                    }
                    if (targetuuid == null) {
                        player.sendMessage(plugin.prefix + ChatColor.RED + "That is not a player's name!");
                        return false;
                    }
                    UUID targetUUID = UUIDFetcher.formatUUID(targetuuid);
                    String targetName = NameFetcher.getName(targetuuid.replace("-", ""));
                    if (targetName == null) {
                        targetName = strings[0];
                    }
                    User user = plugin.luckPermsAPI.getUserManager().getUser(targetUUID);
                    if (user == null) {
                        UserFetcher userFetcher = new UserFetcher();
                        userFetcher.setUuid(targetUUID);
                        ExecutorService executorService1 = Executors.newSingleThreadExecutor();
                        Future<User> userFuture = executorService1.submit(userFetcher);
                        try {
                            user = userFuture.get(5, TimeUnit.SECONDS);
                        }catch (Exception e){
                            executorService1.shutdown();
                            player.sendMessage(ChatColor.RED + "We were unable to fetch your permission information please try again later!");
                            return false;
                        }
                        executorService1.shutdown();
                        if(user == null){
                            throw new IllegalStateException();
                        }
                    }
                    ContextManager cm =  plugin.luckPermsAPI.getContextManager();
                    QueryOptions queryOptions = cm.getQueryOptions(user).orElse(cm.getStaticQueryOptions());
                    TreeMap<Integer, String> titles = new TreeMap<>();
                    for (int titleid : plugin.titles.keySet()) {
                        if (user.getCachedData().getPermissionData(queryOptions).checkPermission("vorplexcore.titles." + titleid).asBoolean()) {
                            titles.put(titleid, plugin.titles.get(titleid));
                        }
                    }
                    if (titles.isEmpty()) {
                        player.sendMessage(plugin.prefix + ChatColor.RED + "You do not have any titles to change!");
                        return false;
                    }
                    final String TARGETNAME = targetName;
                    if (titles.size() <= 56) {
                        IconMenu menu = new IconMenu(ChatColor.LIGHT_PURPLE + targetName + "'s Unlocked Titles", 1, (clicker, menu1, slot, item) -> {
                            if (clicker == player) {
                                if (item == null || !item.hasItemMeta() || item.getItemMeta() == null) return false;
                                if (item.getItemMeta().hasDisplayName()) {
                                    List<String> lore = item.getItemMeta().getLore();
                                    int titleid;
                                    try {
                                        titleid = Integer.parseInt(lore.get(2).substring(13));
                                    } catch (NumberFormatException nfe) {
                                        nfe.printStackTrace();
                                        menu1.close(clicker);
                                        clicker.sendMessage(plugin.prefix + ChatColor.RED + "An Error occurred in our database and we were unable to equip that title!");
                                        return false;
                                    }
                                    try {
                                        String sql = "SELECT * FROM `vorplexcore_equippedtitles` WHERE UUID='" + targetUUID.toString() + "';";
                                        PreparedStatement stmt = plugin.connection.prepareStatement(sql);
                                        ResultSet results = stmt.executeQuery();
                                        String rawTitle = plugin.titles.get(titleid);
                                        if (rawTitle.contains("'"))
                                            rawTitle = rawTitle.replace("'", "%sinquo%");
                                        if (rawTitle.contains("\""))
                                            rawTitle = rawTitle.replace("\"", "%dubquo%");
                                        if (rawTitle.contains("`"))
                                            rawTitle = rawTitle.replace("`", "%bcktck%");
                                        if (results.next()) {
                                            String sql1 = "UPDATE `vorplexcore_equippedtitles` SET `UUID`='" + targetUUID.toString() + "', `TitleID`='" + titleid
                                                    + "', `RawTitle`='" + rawTitle + "' WHERE `UUID`='" + targetUUID.toString() + "';";
                                            PreparedStatement stmt1 = plugin.connection.prepareStatement(sql1);
                                            stmt1.executeUpdate();
                                            stmt1.close();
                                        } else {
                                            String sql1 = "INSERT INTO `vorplexcore_equippedtitles` (`UUID`, `TitleID`, `RawTitle`)" +
                                                    " VALUES ('" + targetUUID.toString() + "','" + titleid + "','" + rawTitle + "');";
                                            PreparedStatement stmt1 = plugin.connection.prepareStatement(sql1);
                                            stmt1.executeUpdate();
                                            stmt1.close();
                                        }
                                    } catch (SQLException sqle) {
                                        sqle.printStackTrace();
                                        clicker.sendMessage(plugin.prefix + ChatColor.RED + "An Error occurred in our database and we were unable to equip that title!");
                                        return true;
                                    }
                                    menu1.close(clicker);
                                    plugin.equippedTitles.put(targetUUID, plugin.titles.get(titleid));
                                    clicker.sendMessage(plugin.prefix + ChatColor.LIGHT_PURPLE + "Set " + TARGETNAME + "'s equipped title to: " + ChatColor.translateAlternateColorCodes('&', item.getItemMeta().getDisplayName()));
                                    return true;
                                }
                            }
                            return false;
                        });
                        int position = 1;
                        menu.addButton(0, new ItemStack(Material.BARRIER, 1), ChatColor.GRAY + "" + ChatColor.ITALIC + "*no title*",
                                ChatColor.LIGHT_PURPLE + "Click this to unequip " + targetName + "'s title.",
                                ChatColor.WHITE + "",
                                ChatColor.WHITE + "Title id: #0");
                        for (int id : titles.navigableKeySet()) {
                            if (id > 0) {
                                menu.addButton(position, new ItemStack(Material.PAPER, 1), ChatColor.translateAlternateColorCodes('&', titles.get(id)),
                                        ChatColor.LIGHT_PURPLE + "Click this to change " + targetName + "'s",
                                        ChatColor.LIGHT_PURPLE + "equipped title to: " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', titles.get(id)),
                                        ChatColor.WHITE + "Title id: " + "#" + id);
                                position++;
                            }
                            if (position >= 54) {
                                break;
                            } else if (position == 45) {
                                menu.setSize(6);
                            } else if (position == 36) {
                                menu.setSize(5);
                            } else if (position == 27) {
                                menu.setSize(4);
                            } else if (position == 18) {
                                menu.setSize(3);
                            } else if (position == 9) {
                                menu.setSize(2);
                            }

                        }
                        menu.open(player);
                    } else {
                        ArrayList<ItemStack> items = new ArrayList<>();
                        ItemStack notitle = new ItemStack(Material.BARRIER, 1);
                        ItemMeta ntim = notitle.getItemMeta();
                        ntim.setDisplayName(ChatColor.GRAY + "" + ChatColor.ITALIC + "*no title*");
                        List<String> ntlore = new ArrayList<>();
                        ntlore.add(ChatColor.LIGHT_PURPLE + "Click this to unequip" + targetName + "'s title.");
                        ntlore.add(ChatColor.WHITE + "");
                        ntlore.add(ChatColor.WHITE + "Title id: " + "#0");
                        ntim.setLore(ntlore);
                        notitle.setItemMeta(ntim);
                        items.add(notitle);
                        for (int id : titles.navigableKeySet()) {
                            if (id > 0) {
                                ItemStack item = new ItemStack(Material.PAPER, 1);
                                ItemMeta im = item.getItemMeta();
                                im.setDisplayName(ChatColor.translateAlternateColorCodes('&', titles.get(id)));
                                List<String> lore = new ArrayList<>();
                                lore.add(ChatColor.LIGHT_PURPLE + "Click this to change " + targetName + "'s");
                                lore.add(ChatColor.LIGHT_PURPLE + "equipped title to: " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', titles.get(id)));
                                lore.add(ChatColor.WHITE + "Title id: " + "#" + id);
                                im.setLore(lore);
                                item.setItemMeta(im);
                                items.add(item);
                            }
                        }
                        ScrollerInventory inventory = new ScrollerInventory(items, ChatColor.LIGHT_PURPLE + player.getName() + "'s Unlocked Titles", (clicker, item, scrollerInventory) -> {
                            if (clicker == player) {
                                if (item == null || !item.hasItemMeta()) return false;
                                if (item.getItemMeta().hasDisplayName()) {
                                    List<String> lore = item.getItemMeta().getLore();
                                    int titleid;
                                    try {
                                        titleid = Integer.parseInt(lore.get(2).substring(13));
                                    } catch (NumberFormatException nfe) {
                                        nfe.printStackTrace();
                                        clicker.sendMessage(plugin.prefix + ChatColor.RED + "An Error occurred in our database and we were unable to equip that title!");
                                        return true;
                                    }
                                    try {
                                        String sql = "SELECT * FROM `vorplexcore_equippedtitles` WHERE UUID='" + clicker.getUniqueId().toString() + "';";
                                        PreparedStatement stmt = plugin.connection.prepareStatement(sql);
                                        ResultSet results = stmt.executeQuery();
                                        String rawTitle = plugin.titles.get(titleid);
                                        if (rawTitle.contains("'"))
                                            rawTitle = rawTitle.replace("'", "%sinquo%");
                                        if (rawTitle.contains("\""))
                                            rawTitle = rawTitle.replace("\"", "%dubquo%");
                                        if (rawTitle.contains("`"))
                                            rawTitle = rawTitle.replace("`", "%bcktck%");
                                        if (results.next()) {
                                            String sql1 = "UPDATE `vectorcore_equippedtitles` SET `UUID`='" + clicker.getUniqueId().toString() + "', `TitleID`='" + titleid
                                                    + "', `RawTitle`='" + rawTitle + "' WHERE `UUID`='" + player.getUniqueId().toString() + "';";
                                            PreparedStatement stmt1 = plugin.connection.prepareStatement(sql1);
                                            stmt1.executeUpdate();
                                            stmt1.close();
                                        } else {
                                            String sql1 = "INSERT INTO `vectorcore_equippedtitles` (`UUID`, `TitleID`, `RawTitle`)" +
                                                    " VALUES ('" + player.getUniqueId().toString() + "','" + titleid + "','" + rawTitle + "');";
                                            PreparedStatement stmt1 = plugin.connection.prepareStatement(sql1);
                                            stmt1.executeUpdate();
                                            stmt1.close();
                                        }
                                    } catch (SQLException sqle) {
                                        sqle.printStackTrace();
                                        clicker.sendMessage(plugin.prefix + ChatColor.RED + "An Error occurred in our database and we were unable to equip that title!");
                                        return true;
                                    }
                                    plugin.equippedTitles.put(clicker.getUniqueId(), plugin.titles.get(titleid));
                                    clicker.sendMessage(plugin.prefix + ChatColor.LIGHT_PURPLE + "Set your equipped title to: " + ChatColor.translateAlternateColorCodes('&', item.getItemMeta().getDisplayName()));
                                    return true;
                                }
                            }
                            return false;
                        });
                        inventory.open(player);
                    }
                    return true;
                }
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        return false;
    }
}
