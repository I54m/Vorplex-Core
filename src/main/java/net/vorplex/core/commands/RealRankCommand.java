package net.vorplex.core.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.vorplex.core.util.LuckpermsUtil;
import org.bukkit.entity.Player;

public class RealRankCommand {

    public static final LiteralCommandNode<CommandSourceStack> COMMAND_NODE = Commands.literal("realrank")
            .requires(source -> source.getSender() instanceof Player || source.getSender().hasPermission("vorplexcore.realrank"))
            .then(Commands.argument("target", ArgumentTypes.player())
                    .executes(RealRankCommand::realRank)
            )
            .build();

    private static int realRank(final CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final Player player = (Player) ctx.getSource().getSender();
        final PlayerSelectorArgumentResolver targetResolver = ctx.getArgument("target", PlayerSelectorArgumentResolver.class);
        final Player target = targetResolver.resolve(ctx.getSource()).getFirst();
        final String prefix = LuckpermsUtil.getPrefixes(LuckpermsUtil.getUser(target), false).sequencedValues().getLast();

        if (prefix == null || prefix.isBlank()) {
            player.sendRichMessage("<red>" + target.getName() + "'s real rank could not be found!");
            return Command.SINGLE_SUCCESS;
        }

        player.sendRichMessage(target.getName() + "'s real rank is: " + prefix);
        return Command.SINGLE_SUCCESS;
    }
}
