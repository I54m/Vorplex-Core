package net.vorplex.core.plugin;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import net.vorplex.core.VorplexCore;
import org.bukkit.plugin.java.JavaPlugin;

public class PaperBootstrap implements PluginBootstrap {
    @Override
    public void bootstrap(BootstrapContext context) {
//        context.getLogger().info(Component.text("Registering Commands..."));
//        context.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
//            commands.registrar().register();
//        });
//        context.getLogger().info(Component.text("Registered Commands!"));
    }

    @Override
    public JavaPlugin createPlugin(PluginProviderContext context) {
        return new VorplexCore();
    }
}
