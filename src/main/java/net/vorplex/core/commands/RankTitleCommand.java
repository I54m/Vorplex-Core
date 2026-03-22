package net.vorplex.core.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.model.user.User;
import net.vorplex.core.VorplexCore;
import net.vorplex.core.objects.ScrollerInventory;
import net.vorplex.core.util.LuckpermsUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class RankTitleCommand {

    private static final VorplexCore plugin = VorplexCore.getInstance();
    private static final NamespacedKey prefixKey = new NamespacedKey(plugin, "rank_title_prefix");

    public static final LiteralCommandNode<CommandSourceStack> COMMAND_NODE = Commands.literal("ranktitle")
            .requires(source -> source.getSender() instanceof Player || source.getSender().hasPermission("vorplexcore.ranktitle"))
            .executes(RankTitleCommand::openSelf)
            .then(Commands.argument("target", ArgumentTypes.player())
                    .requires(source -> source.getSender().hasPermission("vorplexcore.ranktitle.others"))
                    .executes(RankTitleCommand::openOther)
            )
            .build();

    private static int openSelf(final CommandContext<CommandSourceStack> ctx) {
        final Player player = (Player) ctx.getSource().getSender();
        final User user = LuckpermsUtil.getUser(player);
        final Map<Integer, String> prefixes = LuckpermsUtil.getPrefixes(user, false);

        if (prefixes.size() <= 1) {
            player.sendRichMessage("<red>You do not have any prefixes to change!");
            return Command.SINGLE_SUCCESS;
        }
        buildGUI(player, user, Component.text("Ranktitle GUI").color(NamedTextColor.LIGHT_PURPLE), prefixes).open(player);
        return Command.SINGLE_SUCCESS;
    }

    private static int openOther(final CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final Player player = (Player) ctx.getSource().getSender();
        final PlayerSelectorArgumentResolver targetResolver = ctx.getArgument("target", PlayerSelectorArgumentResolver.class);
        final Player target = targetResolver.resolve(ctx.getSource()).getFirst();
        final User user = LuckpermsUtil.getUser(target);
        final Map<Integer, String> prefixes = LuckpermsUtil.getPrefixes(user, false);

        if (prefixes.size() <= 1) {
            player.sendRichMessage("<red>" + target.getName() + " does not have any prefixes to change!");
            return Command.SINGLE_SUCCESS;
        }
        buildGUI(target, user, Component.text("Changing " + target.getName() + "'s Ranktitle").color(NamedTextColor.RED), prefixes).open(player);
        return Command.SINGLE_SUCCESS;
    }

    private static ScrollerInventory buildGUI(Player player, User user, Component name, Map<Integer, String> prefixes) throws IllegalArgumentException {
        if (prefixes.size() <= 1)
            throw new IllegalArgumentException("player " + player.getName() + " does not have any prefixes to be changed!");

        ArrayList<ItemStack> items = new ArrayList<>();
        for (Map.Entry<Integer, String> prefixEntry : prefixes.entrySet()) {
            String prefix = plugin.isPlaceholderAPI() ? PlaceholderAPI.setPlaceholders(player, prefixEntry.getValue()) : prefixEntry.getValue();
            ItemStack item = new ItemStack(Material.PAPER, 1);
            ItemMeta meta = item.getItemMeta();
            meta.customName(MiniMessage.miniMessage().deserialize(prefix).decoration(TextDecoration.ITALIC, false).append(player.displayName()));
            meta.getPersistentDataContainer().set(prefixKey, PersistentDataType.STRING, prefix);
            meta.lore(Arrays.asList(Component.text("Click this to change your selected prefix to: ").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false), MiniMessage.miniMessage().deserialize(prefix).decoration(TextDecoration.ITALIC, false)));
            item.setItemMeta(meta);
            items.add(item);
        }
        ScrollerInventory.ClickAction onClick = (clicker, clickType, item, scrollerInventory) -> {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.getPersistentDataContainer().has(prefixKey, PersistentDataType.STRING)) {
                String prefix = meta.getPersistentDataContainer().get(prefixKey, PersistentDataType.STRING);
                if (prefix == null) {
                    clicker.sendRichMessage("<red>An Error occurred when trying to set your prefix!");
                    plugin.getComponentLogger().error(Component.text(clicker.getName() + " Tried to change their rank title, but there was no prefix data set on the gui item they clicked. Do you have an empty prefix?"));
                    return true;
                }
                prefix = plugin.isPlaceholderAPI() ? PlaceholderAPI.setPlaceholders(clicker, prefix) : prefix;
                LuckpermsUtil.setPrefix(user, prefix);
                clicker.sendRichMessage(plugin.getPrefix() + "<light_purple>Set your prefix to: " + prefix);
                return true;
            } else return false;
        };
        return new ScrollerInventory(name, items, onClick);
    }
}
