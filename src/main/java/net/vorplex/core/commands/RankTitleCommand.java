package net.vorplex.core.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.vorplex.core.VorplexCore;
import net.vorplex.core.objects.ScrollerInventory;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class RankTitleCommand {

    private static final VorplexCore plugin = VorplexCore.getInstance();
    private static final Component name = Component.text("Ranktitle GUI - ").color(NamedTextColor.RED);

    public static final LiteralCommandNode<CommandSourceStack> COMMAND_NODE = Commands.literal("ranktitle")
            .requires(source -> source.getSender() instanceof Player)
            .requires(source -> source.getSender().hasPermission("vorplexcore.ranktitle"))
            .then(Commands.literal("one").executes(RankTitleCommand::openOne))
            .then(Commands.literal("two").executes(RankTitleCommand::openTwo))
            .then(Commands.literal("three").executes(RankTitleCommand::openThree))
            .then(Commands.literal("four").executes(RankTitleCommand::openFour))
            .then(Commands.literal("five").executes(RankTitleCommand::openFive))
            .then(Commands.literal("six").executes(RankTitleCommand::openSix))
            .executes(RankTitleCommand::openOne)
            .build();

    private static CompletableFuture<Suggestions> getTimeUnitSuggestions(final CommandContext<CommandSourceStack> ctx, final SuggestionsBuilder builder) {
        for (TimeUnit timeunit : TimeUnit.values()) {
            builder.suggest(timeunit.toString());
        }
        return builder.buildFuture();
    }

    private static int openOne(final CommandContext<CommandSourceStack> ctx) {
        final Player player = (Player) ctx.getSource().getSender();
        ArrayList<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (i % 2 == 0)
                items.add(ItemStack.of(Material.RED_STAINED_GLASS_PANE, 1));
            else
                items.add(ItemStack.of(Material.LIME_STAINED_GLASS_PANE, 1));
        }

        ScrollerInventory scrollerInventory = new ScrollerInventory(name.append(Component.text("9 Items")), items, (clicker, clickType, item, inventory) -> {
            clicker.sendRichMessage("<green>Click action triggered!");
            return true;
        }, (closer, inventory) -> {
            closer.sendRichMessage("<red>Close action triggered!");
        });
        scrollerInventory.open(player);

        return Command.SINGLE_SUCCESS;
    }

    private static int openTwo(final CommandContext<CommandSourceStack> ctx) {
        final Player player = (Player) ctx.getSource().getSender();
        ArrayList<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 18; i++) {
            if (i % 2 == 0)
                items.add(ItemStack.of(Material.RED_STAINED_GLASS_PANE, 1));
            else
                items.add(ItemStack.of(Material.LIME_STAINED_GLASS_PANE, 1));
        }

        ScrollerInventory scrollerInventory = new ScrollerInventory(name.append(Component.text("18 Items")), items, (clicker, clickType, item, inventory) -> {
            clicker.sendRichMessage("<green>Click action triggered!");
            return true;
        }, (closer, inventory) -> {
            closer.sendRichMessage("<red>Close action triggered!");
        });
        scrollerInventory.open(player);

        return Command.SINGLE_SUCCESS;
    }

    private static int openThree(final CommandContext<CommandSourceStack> ctx) {
        final Player player = (Player) ctx.getSource().getSender();
        ArrayList<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 27; i++) {
            if (i % 2 == 0)
                items.add(ItemStack.of(Material.RED_STAINED_GLASS_PANE, 1));
            else
                items.add(ItemStack.of(Material.LIME_STAINED_GLASS_PANE, 1));
        }

        ScrollerInventory scrollerInventory = new ScrollerInventory(name.append(Component.text("27 Items")), items, (clicker, clickType, item, inventory) -> {
            clicker.sendRichMessage("<green>Click action triggered!");
            return true;
        }, (closer, inventory) -> {
            closer.sendRichMessage("<red>Close action triggered!");
        });
        scrollerInventory.open(player);

        return Command.SINGLE_SUCCESS;
    }

    private static int openFour(final CommandContext<CommandSourceStack> ctx) {
        final Player player = (Player) ctx.getSource().getSender();
        ArrayList<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            if (i % 2 == 0)
                items.add(ItemStack.of(Material.RED_STAINED_GLASS_PANE, 1));
            else
                items.add(ItemStack.of(Material.LIME_STAINED_GLASS_PANE, 1));
        }

        ScrollerInventory scrollerInventory = new ScrollerInventory(name.append(Component.text("36 Items")), items, (clicker, clickType, item, inventory) -> {
            clicker.sendRichMessage("<green>Click action triggered!");
            return true;
        }, (closer, inventory) -> {
            closer.sendRichMessage("<red>Close action triggered!");
        });
        scrollerInventory.open(player);

        return Command.SINGLE_SUCCESS;
    }

    private static int openFive(final CommandContext<CommandSourceStack> ctx) {
        final Player player = (Player) ctx.getSource().getSender();
        ArrayList<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            if (i % 2 == 0)
                items.add(ItemStack.of(Material.RED_STAINED_GLASS_PANE, 1));
            else
                items.add(ItemStack.of(Material.LIME_STAINED_GLASS_PANE, 1));
        }

        ScrollerInventory scrollerInventory = new ScrollerInventory(name.append(Component.text("45 Items")), items, (clicker, clickType, item, inventory) -> {
            clicker.sendRichMessage("<green>Click action triggered!");
            return true;
        }, (closer, inventory) -> {
            closer.sendRichMessage("<red>Close action triggered!");
        });
        scrollerInventory.open(player);

        return Command.SINGLE_SUCCESS;
    }

    private static int openSix(final CommandContext<CommandSourceStack> ctx) {
        final Player player = (Player) ctx.getSource().getSender();
        ArrayList<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 54; i++) {
            if (i % 2 == 0)
                items.add(ItemStack.of(Material.RED_STAINED_GLASS_PANE, 1));
            else
                items.add(ItemStack.of(Material.LIME_STAINED_GLASS_PANE, 1));
        }

        ScrollerInventory scrollerInventory = new ScrollerInventory(name.append(Component.text("54 Items")), items, (clicker, clickType, item, inventory) -> {
            clicker.sendRichMessage("<green>Click action triggered!");
            return true;
        }, (closer, inventory) -> {
            closer.sendRichMessage("<red>Close action triggered!");
        });
        scrollerInventory.open(player);

        return Command.SINGLE_SUCCESS;
    }


//    @Override
//    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
//        if (!(commandSender instanceof Player)) {
//            commandSender.sendMessage("You must be a player to use this command!");
//            return false;
//        }
//        Player player = (Player) commandSender;
//        if (!player.hasPermission("vorplexcore.ranktitles")) {
//            player.sendMessage(plugin.LEGACY_PREFIX + ChatColor.RED + "You do not have permission to use this command!");
//            return false;
//        }
//        User user = plugin.luckPermsAPI.getUserManager().getUser(player.getName());
//        if (user == null) {
//            UserFetcher userFetcher = new UserFetcher();
//            userFetcher.setUuid(player.getUniqueId());
//            ExecutorService executorService = Executors.newSingleThreadExecutor();
//            Future<User> userFuture = executorService.submit(userFetcher);
//            try {
//                user = userFuture.get(5, TimeUnit.SECONDS);
//            } catch (Exception e) {
//                executorService.shutdown();
//                player.sendMessage(ChatColor.RED + "We were unable to fetch your permission information please try again later!");
//                return false;
//            }
//            executorService.shutdown();
//            if (user == null) {
//                throw new IllegalStateException();
//            }
//        }
//        ContextManager cm = plugin.luckPermsAPI.getContextManager();
//        QueryOptions queryOptions = cm.getQueryOptions(user).orElse(cm.getStaticQueryOptions());
//        Set<Group> groups = plugin.luckPermsAPI.getGroupManager().getLoadedGroups();
//        TreeMap<Integer, String> prefixes = new TreeMap<>();
//        for (Group possiblegroup : groups) {
//            if (player.hasPermission("group." + possiblegroup.getName())) {
//                Map<Integer, String> GroupPrefixes = possiblegroup.getCachedData().getMetaData(queryOptions).getPrefixes();
//                for (int priority : GroupPrefixes.keySet()) {
//                    if (priority < plugin.getConfig().getInt("RankTitle.priority-to-add-prefixes")) {
//                        if (prefixes.containsKey(priority)) continue;
//                        if (!prefixes.containsValue(GroupPrefixes.get(priority)))
//                            prefixes.put(priority, GroupPrefixes.get(priority));
//                    }
//                }
//            }
//        }
//        if (prefixes.size() <= 1) {
//            player.sendMessage(plugin.LEGACY_PREFIX + ChatColor.RED + "You do not have any prefixes to change!");
//            return false;
//        }
//        if (prefixes.size() <= 56) {
//            IconMenu menu = new IconMenu(ChatColor.LIGHT_PURPLE + "Rank Titles", 1, (clicker, menu1, slot, item) -> {
//                if (clicker.equals(player)) {
//                    if (item.getItemMeta().hasDisplayName()) {
//                        menu1.close(clicker);
//                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " meta removeprefix " + plugin.getConfig().getInt("RankTitle.priority-to-add-prefixes"));
//                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " meta addprefix " + plugin.getConfig().getInt("RankTitle.priority-to-add-prefixes") + " " + ChatColor.translateAlternateColorCodes('&', item.getItemMeta().getDisplayName()));
//                        clicker.sendMessage(plugin.LEGACY_PREFIX + ChatColor.LIGHT_PURPLE + "Set your prefix to: " + ChatColor.translateAlternateColorCodes('&', item.getItemMeta().getDisplayName()));
//                        return true;
//                    }
//                }
//                return false;
//            }, (closer, menu1) -> {
//            });
//            int position = 0;
//            for (int id : (prefixes).navigableKeySet()) {
//                menu.addButton(position, new ItemStack(Material.PAPER, 1), ChatColor.translateAlternateColorCodes('&', prefixes.get(id)), ChatColor.LIGHT_PURPLE + "Click this to change your selected prefix to: ", ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', prefixes.get(id)));
//                position++;
//                if (position >= 54) {
//                    break;
//                } else if (position == 45) {
//                    menu.setSize(6);
//                } else if (position == 36) {
//                    menu.setSize(5);
//                } else if (position == 27) {
//                    menu.setSize(4);
//                } else if (position == 18) {
//                    menu.setSize(3);
//                } else if (position == 9) {
//                    menu.setSize(2);
//                }
//            }
//            menu.open(player);
//            return true;
//        } else {
//            ArrayList<ItemStack> items = new ArrayList<>();
//            for (int id : (prefixes).navigableKeySet()) {
//                ItemStack item = new ItemStack(Material.PAPER, 1);
//                ItemMeta im = item.getItemMeta();
//                im.setDisplayName(ChatColor.translateAlternateColorCodes('&', prefixes.get(id)));
//                im.setLore(Arrays.asList(ChatColor.LIGHT_PURPLE + "Click this to change your selected prefix to: ", ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', prefixes.get(id))));
//                item.setItemMeta(im);
//                items.add(item);
//            }
//            ScrollerInventory.onClick onClick = (clicker, item, scrollerInventory) -> {
//                if (clicker.equals(player)) {
//                    if (item.getItemMeta().hasDisplayName()) {
//                        clicker.closeInventory();
//                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " meta removeprefix " + plugin.getConfig().getInt("RankTitle.priority-to-add-prefixes"));
//                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " meta addprefix " + plugin.getConfig().getInt("RankTitle.priority-to-add-prefixes") + " " + ChatColor.translateAlternateColorCodes('&', item.getItemMeta().getDisplayName()));
//                        clicker.sendMessage(plugin.LEGACY_PREFIX + ChatColor.LIGHT_PURPLE + "Set your prefix to: " + ChatColor.translateAlternateColorCodes('&', item.getItemMeta().getDisplayName()));
//                        return true;
//                    }
//                }
//                return false;
//            };
//            new ScrollerInventory(items, Component.text("Rank Titles").color(NamedTextColor.LIGHT_PURPLE), onClick).open(player);
//            return false;
//        }
//    }
}
