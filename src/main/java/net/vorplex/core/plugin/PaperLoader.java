package net.vorplex.core.plugin;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;

public class PaperLoader implements PluginLoader {

    @Override
    public void classloader(PluginClasspathBuilder pluginClasspathBuilder) {
//            classpathBuilder.addLibrary(new JarLibrary(Path.of("dependency.jar")));
//
//            MavenLibraryResolver resolver = new MavenLibraryResolver();
//            resolver.addDependency(new Dependency(new DefaultArtifact("com.example:example:version"), null));
//            resolver.addRepository(new RemoteRepository.Builder("paper", "default", "https://repo.papermc.io/repository/maven-public/").build());
//
//            classpathBuilder.addLibrary(resolver);
    }
}