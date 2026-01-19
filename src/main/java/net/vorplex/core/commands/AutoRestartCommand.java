package net.vorplex.core.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class AutoRestartCommand {
    private static final VorplexCore plugin = VorplexCore.getInstance();
    private static final AutoRestartConfig autoRestartConfig = new AutoRestartConfig();


    public static final LiteralCommandNode<CommandSourceStack> COMMAND_NODE = Commands.literal("autorestart")
            .requires(source -> source.getSender().hasPermission("vorplexcore.autorestart.admin"))
            .then(Commands.literal("now").executes(AutoRestartCommand::restartNow))
            .then(Commands.literal("queue")
                    .executes(AutoRestartCommand::queueRestart)
                    .then(Commands.argument("timeunit", StringArgumentType.word())
                            .suggests(AutoRestartCommand::getTimeUnitSuggestions)
                            .then(Commands.argument("amount of time", LongArgumentType.longArg(1))
                                    .executes(AutoRestartCommand::queueRestart)
                            ))
            )
            .then(Commands.literal("start").executes(AutoRestartCommand::startScheduler))
            .then(Commands.literal("stop").executes(AutoRestartCommand::stopScheduler))
            .then(Commands.literal("info").executes(AutoRestartCommand::restartInfo))
            .then(Commands.literal("help").executes(AutoRestartCommand::help))
            .executes(AutoRestartCommand::help)
            .build();

    private static CompletableFuture<Suggestions> getTimeUnitSuggestions(final CommandContext<CommandSourceStack> ctx, final SuggestionsBuilder builder) {
        for (TimeUnit timeunit : TimeUnit.values()) {
            builder.suggest(timeunit.toString());
        }
        return builder.buildFuture();
    }


    private static int restartNow(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();
        AutoRestartScheduler.scheduleReboot(620);
        sender.sendRichMessage(plugin.getPrefix() + "<green>Rebooting in 30 seconds!");
        return Command.SINGLE_SUCCESS;
    }

    private static int queueRestart(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();
        final String timeunitString;
        final long amountOfTime;
        try {
            timeunitString = ctx.getArgument("timeunit", String.class);
            amountOfTime = ctx.getArgument("amount of time", Long.class);
        } catch (IllegalArgumentException iae) {
            sender.sendRichMessage(plugin.getPrefix() + "<red>Queue an auto reboot. Usage:");
            sender.sendRichMessage(plugin.getPrefix() + "<red>/autorestart queue <timeunit> <amount of time> ");
            return Command.SINGLE_SUCCESS;
        }
        TimeUnit timeUnit;
        try {
            timeUnit = TimeUnit.valueOf(timeunitString);
        } catch (IllegalArgumentException iae) {
            sender.sendRichMessage(plugin.getPrefix() + "<red>" + timeunitString + " is not a valid timeunit!");
            return Command.SINGLE_SUCCESS;
        }
        AutoRestartScheduler.scheduleReboot(timeUnit, amountOfTime);

        String time = amountOfTime + " " + timeunitString.toLowerCase();
        sender.sendRichMessage(plugin.getPrefix() + "<green>Queued auto reboot for <time> from now!", Placeholder.parsed("time", time));

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
                audience.sendMessage(Component.text("Reboot was aborted!").color(NamedTextColor.GREEN));
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
        ctx.getSource().getSender().sendRichMessage("<st><white>|                     </st> " + plugin.getPrefix() + "<st><white>                    |</st><br>"
                + "<light_purple> /autorestart <white>now <gray>- Reboot the server in 30s.<br>"
                + "<light_purple> /autorestart <white>queue <gray>- Queue an auto reboot.<br>"
                + "<light_purple> /autorestart <white>start <gray>- Add all auto reboots to the queue.<br>"
                + "<light_purple> /autorestart <white>stop <gray>- Stop and remove all reboots from the queue.<br>"
                + "<light_purple> /autorestart <white>info <gray>- Get the time to the next auto reboot.<br>"
                + "<light_purple> /autorestart <white>help <gray>- View this help menu."
        );
        return Command.SINGLE_SUCCESS;
    }
}