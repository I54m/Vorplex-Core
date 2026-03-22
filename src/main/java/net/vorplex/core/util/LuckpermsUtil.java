package net.vorplex.core.util;

import net.luckperms.api.messaging.MessagingService;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.PrefixNode;
import net.vorplex.core.VorplexCore;
import org.apache.commons.lang3.NotImplementedException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class LuckpermsUtil {

    private static final VorplexCore plugin = VorplexCore.getInstance();

    @Nullable
    public static Group getGroup(@NotNull User user) {
//        final Collection<Group> inheritedGroups = user.getInheritedGroups(user.getQueryOptions());
//        for (Group group : inheritedGroups) {
//        }
        throw new NotImplementedException();
    }

    /**
     * Get an online player's highest group
     *
     * @param player the player to get the group for
     * @return the group with the highest weight
     */
    @Nullable
    public static Group getPlayerGroup(@NotNull Player player) {
        final Set<Group> possibleGroups = plugin.luckPermsAPI.getGroupManager().getLoadedGroups();
        for (Group group : possibleGroups) {
            if (player.hasPermission("group." + group.getName())) {
                return group;
            }
        }
        return null;
    }

    /**
     * Get a User instance from an online player
     *
     * @param player the online player
     * @return a user instance of the player
     */
    public static User getUser(@NotNull Player player) {
        return plugin.luckPermsAPI.getPlayerAdapter(Player.class).getUser(player);
    }

    /**
     * Get all the prefixes that apply to a user will remove any prefix added by rank title
     *
     * @param user      the user to fetch prefixes for
     * @param ranktitle true to include the ranktitle prefix
     * @return a SortedMap of Integers to Strings where the Integer is the priority of the prefix and the string is the prefix
     */
    public static SortedMap<Integer, String> getPrefixes(@NotNull User user, boolean ranktitle) {
        final SortedMap<Integer, String> prefixes = new TreeMap<>();
        prefixes.putAll(user.getCachedData().getMetaData().getPrefixes());
        if (!ranktitle)
            prefixes.remove(plugin.getConfig().getInt("RankTitle.priority-to-add-prefixes"));
        return prefixes;
    }

    /**
     * Set the rank title prefix for a user - will remove previous rank title prefixes if there is one at the same priority
     *
     * @param user   the user to set the prefix for
     * @param prefix the prefix to set
     */
    public static void setPrefix(@NotNull User user, @NotNull String prefix) {
        final SortedMap<Integer, String> prefixes = getPrefixes(user, true);
        if (prefixes.containsKey(plugin.getConfig().getInt("RankTitle.priority-to-add-prefixes"))) {
            String oldPrefix = prefixes.get(plugin.getConfig().getInt("RankTitle.priority-to-add-prefixes"));
            user.data().remove(PrefixNode.builder(oldPrefix, plugin.getConfig().getInt("RankTitle.priority-to-add-prefixes")).build());
        }
        user.data().add(PrefixNode.builder(prefix, plugin.getConfig().getInt("RankTitle.priority-to-add-prefixes")).build());
        plugin.luckPermsAPI.getUserManager().saveUser(user);
        plugin.luckPermsAPI.getMessagingService().ifPresent(MessagingService::pushUpdate);
    }
}
