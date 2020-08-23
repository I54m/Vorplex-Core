package net.vorplex.core.commands;

import net.luckperms.api.context.ContextManager;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.vorplex.core.Main;
import net.vorplex.core.objects.IconMenu;
import net.vorplex.core.util.UserFetcher;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class RankTitleCommand implements CommandExecutor {

    private Main plugin = Main.getInstance();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player)){
            commandSender.sendMessage("You must be a player to use this command!");
            return false;
        }
        Player player = (Player) commandSender;
        if (!player.hasPermission("vorplexcore.ranktitles")){
            player.sendMessage(plugin.prefix + ChatColor.RED + "You do not have permission to use this command!");
            return false;
        }
        User user = plugin.luckPermsAPI.getUserManager().getUser(player.getName());
        if (user == null) {
            UserFetcher userFetcher = new UserFetcher();
            userFetcher.setUuid(player.getUniqueId());
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Future<User> userFuture = executorService.submit(userFetcher);
            try {
                user = userFuture.get(5, TimeUnit.SECONDS);
            }catch (Exception e){
                executorService.shutdown();
                player.sendMessage(ChatColor.RED + "We were unable to fetch your permission information please try again later!");
                return false;
            }
            executorService.shutdown();
            if(user == null){
                throw new IllegalStateException();
            }
        }
        ContextManager cm = plugin.luckPermsAPI.getContextManager();
        QueryOptions queryOptions = cm.getQueryOptions(user).orElse(cm.getStaticQueryOptions());
        Set<Group> groups = plugin.luckPermsAPI.getGroupManager().getLoadedGroups();
        TreeMap<Integer, String> prefixes = new TreeMap<>();
        for (Group possiblegroup : groups){
            if (player.hasPermission("group." + possiblegroup.getName())){
                SortedMap<Integer, String> GroupPrefixes = possiblegroup.getCachedData().getMetaData(queryOptions).getPrefixes();
                for (int priority : GroupPrefixes.keySet()){
                    if (priority < plugin.getConfig().getInt("RankTitle.priority-to-add-prefixes")){
                        prefixes.put(priority, GroupPrefixes.get(priority));
                    }
                }
            }
        }
        if (prefixes.size() <= 1){
            player.sendMessage(plugin.prefix + ChatColor.RED + "You do not have any prefixes to change!");
            return false;
        }
        IconMenu menu = new IconMenu(ChatColor.LIGHT_PURPLE + "Rank Titles", 1, (clicker, menu1, slot, item) -> {
            if (clicker == player){
                if (item.getItemMeta().hasDisplayName()){
                    menu1.close(clicker);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " meta removeprefix " + plugin.getConfig().getInt("RankTitle.priority-to-add-prefixes"));
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " meta addprefix " + plugin.getConfig().getInt("RankTitle.priority-to-add-prefixes") + " " + ChatColor.translateAlternateColorCodes('&', item.getItemMeta().getDisplayName()));
                    clicker.sendMessage(plugin.prefix + ChatColor.LIGHT_PURPLE + "Set your prefix to: " + ChatColor.translateAlternateColorCodes('&', item.getItemMeta().getDisplayName()));
                    return true;
                }
            }
            return false;
        });
        int position = 0;
        for (int priority : (prefixes).navigableKeySet()){
            menu.addButton(position, new ItemStack(Material.PAPER, 1), ChatColor.translateAlternateColorCodes('&', prefixes.get(priority)), ChatColor.LIGHT_PURPLE + "Click this to change your selected prefix to: ", ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', prefixes.get(priority)));
            position++;
            if (position >= 9){
                menu.setSize(2);
            }
            if (position >= 18){
                menu.setSize(3);
            }
            if (position >= 27){
                menu.setSize(4);
            }
            if (position >= 36){
                menu.setSize(5);
            }
            if (position >= 45){
                menu.setSize(6);
            }
            if (position >= 54){
                break;
            }
        }
        menu.open(player);
        return true;
    }
}
