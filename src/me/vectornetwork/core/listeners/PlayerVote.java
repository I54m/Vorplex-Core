package me.vectornetwork.core.listeners;

import com.vexsoftware.votifier.model.VotifierEvent;
import me.vectornetwork.core.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

public class PlayerVote implements Listener {

    private Main plugin = Main.getInstance();

    @EventHandler
    public void onPlayerVote(VotifierEvent event){
        List<String> commands = plugin.getConfig().getStringList("VoteRewards.commands");
        for (String command : commands){
            command = command.replace("%player%", event.getVote().getUsername());
            Bukkit.dispatchCommand(plugin.getServer().getConsoleSender(), command);
        }
        Player player = Bukkit.getPlayer(event.getVote().getUsername());
        String message = plugin.getConfig().getString("VoteRewards.message");
        message = message.replace("%player%", player.getName()).replace("%votesite%", event.getVote().getServiceName());
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        if (plugin.getConfig().getBoolean("VoteRewards.broadcastvote")){
            String broadcast = plugin.getConfig().getString("VoteRewards.broadcast");
            broadcast = broadcast.replace("%player%", player.getName()).replace("%votesite%", event.getVote().getServiceName());
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', broadcast));
        }
    }
}
