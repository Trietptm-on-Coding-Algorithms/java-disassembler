package the.bytecode.club.jda.plugin.strategies;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import the.bytecode.club.jda.JarUtils;
import the.bytecode.club.jda.api.ExceptionUI;
import the.bytecode.club.jda.api.Plugin;
import the.bytecode.club.jda.plugin.PluginLaunchStrategy;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Konloch
 * @author Bibl (don't ban me pls)
 * @created 1 Jun 2015
 */
public class CompiledJavaPluginLaunchStrategy implements PluginLaunchStrategy
{

    private static final String PLUGIN_CLASS_NAME = Plugin.class.getCanonicalName().replace(".", "/");

    private final Set<LoadedPluginData> loaded = new HashSet<>();

    @Override
    public Plugin run(File file) throws Throwable
    {
        Set<LoadedNodeData> set = loadData(file);

        LoadedNodeData pdata = null;
        for (LoadedNodeData d : set)
        {
            ClassNode cn = d.node;
            if (cn.superName.equals(PLUGIN_CLASS_NAME))
            {
                if (pdata == null)
                {
                    pdata = d;
                }
                else
                {
                    throw new RuntimeException("Multiple plugin subclasses.");
                }
            }
        }

        LoadingClassLoader cl = new LoadingClassLoader(pdata, set);
        Plugin p = cl.pluginKlass.newInstance();
        LoadedPluginData npdata = new LoadedPluginData(pdata, cl, p);
        loaded.add(npdata);

        return p;
    }

    public Set<LoadedPluginData> getLoaded()
    {
        return loaded;
    }

    private static Set<LoadedNodeData> loadData(File jarFile) throws Throwable
    {
        ZipInputStream jis = new ZipInputStream(new FileInputStream(jarFile));
        ZipEntry entry;

        Set<LoadedNodeData> set = new HashSet<>();

        while ((entry = jis.getNextEntry()) != null)
        {
            try
            {
                String name = entry.getName();
                if (name.endsWith(".class"))
                {
                    byte[] bytes = JarUtils.getBytes(jis);
                    String magic = String.format("%02X", bytes[0]) + String.format("%02X", bytes[1]) + String.format("%02X", bytes[2]) + String.format("%02X", bytes[3]);
                    if (magic.toLowerCase().equals("cafebabe"))
                    {
                        try
                        {
                            ClassReader cr = new ClassReader(bytes);
                            ClassNode cn = new ClassNode();
                            cr.accept(cn, 0);
                            LoadedNodeData data = new LoadedNodeData(bytes, cn);
                            set.add(data);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                    else
                    {
                        System.out.println(jarFile + ">" + name + ": Header does not start with CAFEBABE, ignoring.");
                    }
                }
            }
            catch (Exception e)
            {
                new ExceptionUI(e);
            }
            finally
            {
                jis.closeEntry();
            }
        }
        jis.close();

        return set;
    }

    public static class LoadedNodeData
    {
        private final byte[] bytes;
        private final ClassNode node;

        public LoadedNodeData(byte[] bytes, ClassNode node)
        {
            this.bytes = bytes;
            this.node = node;
        }
    }

    public static class LoadedPluginData
    {
        private final LoadedNodeData data;
        private final LoadingClassLoader classLoader;
        private final Plugin plugin;

        public LoadedPluginData(LoadedNodeData data, LoadingClassLoader classLoader, Plugin plugin)
        {
            this.data = data;
            this.classLoader = classLoader;
            this.plugin = plugin;
        }

        public LoadedNodeData getData()
        {
            return data;
        }

        public LoadingClassLoader getClassLoader()
        {
            return classLoader;
        }

        public Plugin getPlugin()
        {
            return plugin;
        }
    }

    public static class LoadingClassLoader extends ClassLoader
    {
        private final LoadedNodeData data;
        private Map<String, LoadedNodeData> cache;
        private Map<String, Class<?>> ccache;
        private final Class<? extends Plugin> pluginKlass;

        public LoadingClassLoader(LoadedNodeData data, Set<LoadedNodeData> set) throws Throwable
        {
            this.data = data;

            cache = new HashMap<>();
            ccache = new HashMap<>();

            for (LoadedNodeData d : set)
            {
                cache.put(d.node.name, d);
            }

            @SuppressWarnings("unchecked")
            Class<? extends Plugin> pluginKlass = (Class<? extends Plugin>) loadClass(data.node.name.replace("/", "."));

            if (pluginKlass == null)
                throw new RuntimeException();

            this.pluginKlass = pluginKlass;
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException
        {
            name = name.replace(".", "/");

            System.out.println("finding " + name);

            if (ccache.containsKey(name))
                return ccache.get(name);

            LoadedNodeData data = cache.get(name);
            if (data != null)
            {
                byte[] bytes = data.bytes;
                Class<?> klass = defineClass(data.node.name.replace("/", "."), bytes, 0, bytes.length);
                ccache.put(name, klass);
                return klass;
            }

            return super.findClass(name);
        }

        public LoadedNodeData getPluginNode()
        {
            return data;
        }

        public Class<? extends Plugin> getPluginKlass()
        {
            return pluginKlass;
        }
    }
}