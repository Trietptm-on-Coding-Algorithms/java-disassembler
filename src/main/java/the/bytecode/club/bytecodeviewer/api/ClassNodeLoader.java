package the.bytecode.club.bytecodeviewer.api;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * @author Demmonic
 */

public final class ClassNodeLoader extends ClassLoader
{

    public ClassNodeLoader()
    {
        super(ClassLoader.getSystemClassLoader());
    }

    private HashMap<String, ClassNode> classes = new HashMap<>();

    /**
     * Adds the provided class node to the class loader
     */
    public void addClass(ClassNode cn)
    {
        classes.put(cn.name.replace("/", "."), cn);
    }

    /**
     * @param name The name of the class
     * @return If this class loader contains the provided class node
     */
    public boolean contains(String name)
    {
        return (classes.get(name) != null);
    }

    /**
     * @return All class nodes in this loader
     */
    public Collection<ClassNode> getAll()
    {
        return classes.values();
    }

    /**
     * Clears out all class nodes
     */
    public void clear()
    {
        classes.clear();
    }

    /**
     * @return All classes in this loader
     */
    public Collection<Class<?>> getAllClasses()
    {
        ArrayList<Class<?>> classes = new ArrayList<>();
        for (String s : this.classes.keySet())
        {
            try
            {
                classes.add(loadClass(s));
            }
            catch (ClassNotFoundException e)
            {
                e.printStackTrace();
            }
        }

        return classes;
    }

    /**
     * @param name The name of the class
     * @return The class node with the provided name
     */
    public ClassNode get(String name)
    {
        return classes.get(name);
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException
    {
        return findClass(className);
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException
    {
        if (classes.containsKey(name))
        {
            return nodeToClass(classes.get(name));
        }
        else
        {
            return super.findClass(name);
        }
    }

    /**
     * Converts a class node to a class
     *
     * @param node The node to convert
     * @return The converted class
     */
    public Class<?> nodeToClass(ClassNode node)
    {
        if (super.findLoadedClass(node.name.replace("/", ".")) != null)
            return findLoadedClass(node.name.replace("/", "."));
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        try
        {
            node.accept(cw);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        byte[] b = cw.toByteArray();
        return defineClass(node.name.replaceAll("/", "."), b, 0, b.length, getDomain());
    }

    /**
     * @return This class loader's protection domain
     */
    private ProtectionDomain getDomain()
    {
        CodeSource code = new CodeSource(null, (Certificate[]) null);
        return new ProtectionDomain(code, getPermissions());
    }

    /**
     * @return This class loader's permissions
     */
    private Permissions getPermissions()
    {
        Permissions permissions = new Permissions();
        permissions.add(new AllPermission());
        return permissions;
    }

}
