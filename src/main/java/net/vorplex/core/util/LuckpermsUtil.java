package net.vorplex.core.util;

import net.luckperms.api.context.ContextManager;
import net.luckperms.api.messaging.MessagingService;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.query.QueryOptions;
import net.vorplex.core.VorplexCore;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class LuckpermsUtil {

    private static final VorplexCore plugin = VorplexCore.getInstance();

    /**
     * Get a User's highest group
     *
     * @param user the user to get the group for
     * @return     the group with the highest weight
     */
    @Nullable
    public static Group getGroup(@NotNull User user) {
        final Collection<Group> inheritedGroups = user.getInheritedGroups(user.getQueryOptions());
        final Map<Integer, Group> groups = new HashMap<>();
        int highestWeight = Integer.MIN_VALUE;
        for (Group group : inheritedGroups) {
            if (group.getWeight().isPresent()) {
                groups.put(group.getWeight().getAsInt(), group);
                if (group.getWeight().getAsInt() > highestWeight) highestWeight = group.getWeight().getAsInt();
            }
        }
        return groups.getOrDefault(highestWeight, null);
    }

    /**
     * Get an online player's highest group
     *
     * @param player the player to get the group for
     * @return       the group with the highest weight
     */
    @Nullable
    public static Group getGroup(@NotNull Player player) {
        return getGroup(getUser(player));
    }

    /**
     * Get a User instance from an online player
     *
     * @param player the online player
     * @return       a user instance of the player
     */
    public static User getUser(@NotNull Player player) {
        return plugin.luckPermsAPI.getPlayerAdapter(Player.class).getUser(player);
    }

    /**
     * Get all the prefixes that apply to a user will remove any prefix added by rank title
     *
     * @param user      the user to fetch prefixes for
     * @param ranktitle true to include the ranktitle prefix
     * @return          a SortedMap of Integers to Strings where the Integer is the priority of the prefix and the string is the prefix
     */
    public static SortedMap<Integer, String> getPrefixes(@NotNull User user, boolean ranktitle) {
        final SortedMap<Integer, String> prefixes = new TreeMap<>();
        prefixes.putAll(user.getCachedData().getMetaData().getPrefixes());
        if (!ranktitle)
            prefixes.remove(plugin.getConfig().getInt("RankTitle.priority-to-add-prefixes"));
        return prefixes;
    }

    /**
     * Get the player's prefix with the highest weight
     *
     * @param player the user to fetch the prefix for
     * @return the user's prefix or an empty string if no prefix is found
     */
    public static String getPrefix(@NotNull Player player) {
        return getPrefix(getUser(player));
    }

    /**
     * Get the user's prefix with the highest weight
     *
     * @param user the user to fetch the prefix for
     * @return the user's prefix or an empty string if no prefix is found
     */
    public static String getPrefix(@NotNull User user) {
        ContextManager cm = plugin.luckPermsAPI.getContextManager();
        QueryOptions queryOptions = cm.getQueryOptions(user).orElse(cm.getStaticQueryOptions());
        String prefix = user.getCachedData().getMetaData(queryOptions).getPrefix();
        return prefix == null ? "" : prefix;
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
