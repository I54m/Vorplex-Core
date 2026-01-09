package net.vorplex.core.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import net.vorplex.core.VorplexCore;
import net.vorplex.core.autorestart.AutoRestartConfig;
import net.vorplex.core.autorestart.AutoRestartScheduler;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class AutoRestartCommand {
    private static final VorplexCore plugin = VorplexCore.getInstance();
    private static final AutoRestartConfig autoRestartConfig = new AutoRestartConfig();


    public static final LiteralCommandNode<CommandSourceStack> COMMAND_NODE = Commands.literal("autorestart")
            .requires(source -> source.getSender().hasPermission("vorplexcore.autorestart.admin"))
            .then(Commands.literal("now").executes(AutoRestartCommand::restartNow))
            .then(Commands.literal("queue")
                    .executes(AutoRestartCommand::queueRestart)
                    .then(Commands.argument("minutes|hours", StringArgumentType.word())
                            .then(Commands.argument("amount of time", IntegerArgumentType.integer(0, 59))
                                    .executes(AutoRestartCommand::queueRestart)
                            ))
            )
            .then(Commands.literal("start").executes(AutoRestartCommand::startScheduler))
            .then(Commands.literal("stop").executes(AutoRestartCommand::stopScheduler))
            .then(Commands.literal("info").executes(AutoRestartCommand::restartInfo))
            .then(Commands.literal("help").executes(AutoRestartCommand::help))
            .executes(AutoRestartCommand::help)
            .build();


    private static int restartNow(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();
        AutoRestartScheduler.stop();
        AutoRestartScheduler.scheduleNotify(autoRestartConfig, 600);
        sender.sendRichMessage(plugin.getPrefix() + "<green>Rebooting in 30 seconds!");
        Audience audience = Audience.audience(Bukkit.getServer().getOnlinePlayers());
        if (autoRestartConfig.notifyChatEnabled)
            audience.sendMessage(plugin.getPrefix().append(Component.text("Server will reboot in 30 seconds!").color(NamedTextColor.GREEN)));
        if (autoRestartConfig.notifyTitleEnabled)
            audience.showTitle(Title.title(Component.text("Server will reboot in 30 seconds!").color(NamedTextColor.GREEN), Component.empty(),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))));
        audience.playSound(autoRestartConfig.notifySound);

        AutoRestartScheduler.tasks.add(plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Audience audienceLater = Audience.audience(Bukkit.getServer().getOnlinePlayers());
            if (autoRestartConfig.notifyChatEnabled)
                audienceLater.sendMessage(plugin.getPrefix().append(Component.text("Server is now rebooting!").color(NamedTextColor.GREEN)));
            if (autoRestartConfig.notifyTitleEnabled)
                audienceLater.showTitle(Title.title(Component.text("Server is now rebooting").color(NamedTextColor.GREEN), Component.empty(),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))));
            audienceLater.playSound(autoRestartConfig.notifySound);
            plugin.getServer().restart();
        }, 600));
        return Command.SINGLE_SUCCESS;
    }

    private static int queueRestart(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();
        final String minuteHours;
        final int amountOfTime;
        try {
            minuteHours = ctx.getArgument("minutes|hours", String.class);
            amountOfTime = ctx.getArgument("amount of time", Integer.class);
        } catch (IllegalArgumentException iae) {
            sender.sendRichMessage(plugin.getPrefix() + "<red>Queue an auto reboot. Usage:");
            sender.sendRichMessage(plugin.getPrefix() + "<red>/autorestart queue <minutes|hours> <amount of time> ");
            return Command.SINGLE_SUCCESS;
        }

        AutoRestartConfig config = new AutoRestartConfig();
        config.schedule = new ArrayList<>();
        if (minuteHours.equalsIgnoreCase("minutes") || minuteHours.equalsIgnoreCase("minute")) {
            if (amountOfTime > 59) {
                sender.sendRichMessage(plugin.getPrefix() + "<red>Minutes must be between 0 and 59");
                return Command.SINGLE_SUCCESS;
            }
            config.schedule.add("0 0/" + amountOfTime + " * 1/1 * ? *");
        } else if (minuteHours.equalsIgnoreCase("hours") || minuteHours.equalsIgnoreCase("hour")) {
            if (amountOfTime > 23) {
                sender.sendRichMessage(plugin.getPrefix() + "<red>Hours must be between 0 and 23");
                return Command.SINGLE_SUCCESS;
            }
            config.schedule.add("0 0 0/" + amountOfTime + " 1/1 * ? *");
        } else {
            sender.sendRichMessage(plugin.getPrefix() + "<red>You can only queue a reboot between 1 minute and 23 hours!");
            return Command.SINGLE_SUCCESS;
        }
        String time = amountOfTime + " " + minuteHours;
        sender.sendRichMessage(plugin.getPrefix() + "<green>Queued auto reboot for <time> from now!", Placeholder.parsed("time", time));
        AutoRestartScheduler.stop();
        AutoRestartScheduler.start(config);
        Audience audience = Audience.audience(Bukkit.getServer().getOnlinePlayers());
        if (autoRestartConfig.notifyChatEnabled)
            audience.sendMessage(plugin.getPrefix().append(Component.text("Server will now auto reboot " + time + " from now!")).color(NamedTextColor.GREEN));
        if (autoRestartConfig.notifyTitleEnabled)
            audience.showTitle(Title.title(Component.text("Server will now auto reboot ").color(NamedTextColor.GREEN), Component.text(time + " from now!").color(NamedTextColor.GREEN),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))));
        audience.playSound(autoRestartConfig.notifySound);
        return Command.SINGLE_SUCCESS;
    }

    private static int startScheduler(final CommandContext<CommandSourceStack> ctx) {
        if (AutoRestartScheduler.tasks.isEmpty()) {
            AutoRestartScheduler.start(new AutoRestartConfig());
            ctx.getSource().getSender().sendRichMessage(plugin.getPrefix() + "<green>Started all auto reboot tasks!");
        } else
            ctx.getSource().getSender().sendRichMessage(plugin.getPrefix() + "<red>There are auto reboot tasks already running!");
        return Command.SINGLE_SUCCESS;
    }

    private static int stopScheduler(final CommandContext<CommandSourceStack> ctx) {
        if (!AutoRestartScheduler.tasks.isEmpty()) {
            AutoRestartScheduler.stop();
            Audience audience = Audience.audience(Bukkit.getServer().getOnlinePlayers());
            if (autoRestartConfig.notifyChatEnabled)
                audience.sendMessage(plugin.getPrefix().append(Component.text("Reboot was aborted!").color(NamedTextColor.GREEN)));
            if (autoRestartConfig.notifyTitleEnabled)
                audience.showTitle(Title.title(Component.text("Reboot was aborted!").color(NamedTextColor.GREEN), Component.empty(),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))));
            audience.playSound(autoRestartConfig.notifySound);
            ctx.getSource().getSender().sendRichMessage(plugin.getPrefix() + "<green>Stopped all auto reboot tasks!");
        } else
            ctx.getSource().getSender().sendMessage(plugin.getPrefix() + "<red>There are no auto reboot tasks running!");
        return Command.SINGLE_SUCCESS;
    }

    private static int restartInfo(final CommandContext<CommandSourceStack> ctx) {
        ZonedDateTime nextTime = AutoRestartScheduler.nextTime;
        String date = "<red>No auto reboot tasks are currently scheduled!";
        if (nextTime != null) date = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss (O)").format(nextTime);
        ctx.getSource().getSender().sendMessage(plugin.getPrefix() + "<green>Next restart: " + date);
        return Command.SINGLE_SUCCESS;
    }

    private static int help(final CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().getSender().sendRichMessage("<st><white>|----------</st>" + plugin.getPrefix() + "<st><white>----------|</st><br>"
                + "<white>  /autorestart now <gray>- Reboot the server in 30s.<br>"
                + "<white>  /autorestart queue <gray>- Queue an auto reboot.<br>"
                + "<white>  /autorestart start <gray>- Add all auto reboots to the queue.<br>"
                + "<white>  /autorestart stop <gray>- Stop and remove all reboots from the queue.<br>"
                + "<white>  /autorestart info <gray>- Get the time to the next auto reboot.<br>"
                + "<white>  /autorestart help <gray>- View this help menu."
        );
        return Command.SINGLE_SUCCESS;
    }
}