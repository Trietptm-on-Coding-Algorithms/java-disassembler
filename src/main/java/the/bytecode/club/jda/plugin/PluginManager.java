package the.bytecode.club.jda.plugin;

import the.bytecode.club.jda.JDA;
import the.bytecode.club.jda.MiscUtils;
import the.bytecode.club.jda.api.Plugin;
import the.bytecode.club.jda.plugin.strategies.CompiledJavaPluginLaunchStrategy;
import the.bytecode.club.jda.plugin.strategies.GroovyPluginLaunchStrategy;
import the.bytecode.club.jda.plugin.strategies.PythonPluginLaunchStrategy;
import the.bytecode.club.jda.plugin.strategies.RubyPluginLaunchStrategy;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Supports loading of groovy, python or ruby scripts.
 * <p>
 * Only allows one plugin to be running at once.
 *
 * @author Konloch
 * @author Bibl
 * @since 01/16/16, 14:36, Adaptable PluginLaunchStrategy system.
 */
public final class PluginManager
{

    private static final Map<String, PluginLaunchStrategy> launchStrategies = new HashMap<>();
    private static final PluginFileFilter filter = new PluginFileFilter();
    private static Plugin pluginInstance;

    static
    {
        launchStrategies.put("jar", new CompiledJavaPluginLaunchStrategy());

        GroovyPluginLaunchStrategy groovy = new GroovyPluginLaunchStrategy();
        launchStrategies.put("gy", groovy);
        launchStrategies.put("groovy", groovy);

        PythonPluginLaunchStrategy python = new PythonPluginLaunchStrategy();
        launchStrategies.put("py", python);
        launchStrategies.put("python", python);

        RubyPluginLaunchStrategy ruby = new RubyPluginLaunchStrategy();
        launchStrategies.put("rb", ruby);
        launchStrategies.put("ruby", ruby);
    }

    /**
     * Runs a new plugin instance
     *
     * @param newPluginInstance the new plugin instance
     */
    public static void runPlugin(Plugin newPluginInstance)
    {
        if (pluginInstance == null || pluginInstance.isFinished())
        {
            pluginInstance = newPluginInstance;
            pluginInstance.start(); // start the thread
        }
        else if (!pluginInstance.isFinished())
        {
            JDA.showMessage("There is currently another plugin running right now, please wait for that to finish executing.");
        }
    }

    /**
     * Starts and runs a plugin from file
     *
     * @param f the file of the plugin
     * @throws Exception
     */
    public static void runPlugin(File f) throws Throwable
    {
        String ext = f.getName().substring(f.getName().lastIndexOf('.') + 1);
        PluginLaunchStrategy strategy = launchStrategies.get(ext);

        if (strategy == null)
        {
            throw new RuntimeException(String.format("No launch strategy for extension %s (%s)", ext, f.getAbsolutePath()));
        }

        Plugin p = strategy.run(f);

        if (p != null)
        {
            runPlugin(p);
        }
    }

    public static void register(String name, PluginLaunchStrategy strat)
    {
        launchStrategies.put(name, strat);
    }

    public static Set<String> pluginExtensions()
    {
        return launchStrategies.keySet();
    }

    public static FileFilter fileFilter()
    {
        return filter;
    }

    public static class PluginFileFilter extends FileFilter
    {

        @Override
        public boolean accept(File f)
        {
            if (f.isDirectory())
                return true;

            return PluginManager.pluginExtensions().contains(MiscUtils.extension(f.getAbsolutePath()));
        }

        @Override
        public String getDescription()
        {
            return "JDA Plugins";
        }
    }
}