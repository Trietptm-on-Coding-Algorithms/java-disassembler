package the.bytecode.club.jda.plugin.preinstalled;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import the.bytecode.club.jda.BytecodeViewer;
import the.bytecode.club.jda.api.Plugin;
import the.bytecode.club.jda.api.PluginConsole;

import java.util.ArrayList;

/**
 * The idea/core was based off of J-RET's Malicious Code Searcher I improved it,
 * and added more stuff to search for.
 *
 * @author Konloch
 * @author Adrianherrera
 * @author WaterWolf
 */

public class MaliciousCodeScanner extends Plugin
{

    public boolean ORE, ONE, ORU, OIO, LWW, LHT, LHS, LIP, NSM, ROB;

    public MaliciousCodeScanner(boolean reflect, boolean runtime, boolean net, boolean io, boolean www, boolean http, boolean https, boolean ip, boolean nullSecMan, boolean robot)
    {
        ORE = reflect;
        ONE = net;
        ORU = runtime;
        OIO = io;
        LWW = www;
        LHT = http;
        LHS = https;
        LIP = ip;
        NSM = nullSecMan;
        ROB = robot;
    }

    @Override
    public void execute(ArrayList<ClassNode> classNodeList)
    {
        PluginConsole frame = new PluginConsole("Malicious Code Scanner");
        StringBuilder sb = new StringBuilder();
        for (ClassNode classNode : classNodeList)
        {
            for (Object o : classNode.fields.toArray())
            {
                FieldNode f = (FieldNode) o;
                Object v = f.value;
                if (v instanceof String)
                {
                    String s = (String) v;
                    if ((LWW && s.contains("www.")) || (LHT && s.contains("http://")) || (LHS && s.contains("https://")) || (ORE && s.contains("java/lang/Runtime")) || (ORE && s.contains("java.lang.Runtime")) || (ROB && s.contains("java.awt.Robot")) || (ROB && s.contains("java/awt/Robot")) || (LIP && s.matches("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b")))
                        sb.append("Found LDC \"").append(s).append("\" at field ").append(classNode.name).append(".").append(f.name).append("(").append(f.desc).append(")").append(BytecodeViewer.nl);
                }
                if (v instanceof String[])
                {
                    for (int i = 0; i < ((String[]) v).length; i++)
                    {
                        String s = ((String[]) v)[i];
                        if ((LWW && s.contains("www.")) || (LHT && s.contains("http://")) || (LHS && s.contains("https://")) || (ORE && s.contains("java/lang/Runtime")) || (ORE && s.contains("java.lang.Runtime")) || (ROB && s.contains("java.awt.Robot")) || (ROB && s.contains("java/awt/Robot")) || (LIP && s.matches("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b")))
                            sb.append("Found LDC \"").append(s).append("\" at field ").append(classNode.name).append(".").append(f.name).append("(").append(f.desc).append(")").append(BytecodeViewer.nl);
                    }
                }
            }

            boolean prevInsn_aconst_null = false;

            for (Object o : classNode.methods.toArray())
            {
                MethodNode m = (MethodNode) o;

                InsnList iList = m.instructions;
                for (AbstractInsnNode a : iList.toArray())
                {
                    if (a instanceof MethodInsnNode)
                    {
                        final MethodInsnNode min = (MethodInsnNode) a;
                        if ((ORE && min.owner.startsWith("java/lang/reflect")) || (ONE && min.owner.startsWith("java/net")) || (ORU && min.owner.equals("java/lang/Runtime")) || (ROB && min.owner.equals("java/awt/Robot")) || (OIO && min.owner.startsWith("java/io")))
                        {
                            sb.append("Found Method call to ").append(min.owner).append(".").append(min.name).append("(").append(min.desc).append(") at ").append(classNode.name).append(".").append(m.name).append("(").append(m.desc).append(")").append(BytecodeViewer.nl);
                        }
                    }
                    if (a instanceof LdcInsnNode)
                    {
                        if (((LdcInsnNode) a).cst instanceof String)
                        {
                            final String s = (String) ((LdcInsnNode) a).cst;
                            if ((LWW && s.contains("www.")) || (LHT && s.contains("http://")) || (LHS && s.contains("https://")) || (ORE && s.contains("java/lang/Runtime")) || (ORE && s.contains("java.lang.Runtime")) || (ROB && s.contains("java.awt.Robot")) || (ROB && s.contains("java/awt/Robot")) || (LIP && s.matches("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b")))
                            {
                                sb.append("Found LDC \"").append(s).append("\" at method ").append(classNode.name).append(".").append(m.name).append("(").append(m.desc).append(")").append(BytecodeViewer.nl);
                            }
                        }
                    }

                    // Check if the security manager is getting set to null
                    if ((a instanceof InsnNode) && (a.opcode() == Opcodes.ACONST_NULL))
                    {
                        prevInsn_aconst_null = true;
                    }
                    else if ((a instanceof MethodInsnNode) && (a.opcode() == Opcodes.INVOKESTATIC))
                    {
                        final String owner = ((MethodInsnNode) a).owner;
                        final String name = ((MethodInsnNode) a).name;
                        if ((NSM && prevInsn_aconst_null && owner.equals("java/lang/System") && name.equals("setSecurityManager")))
                        {
                            sb.append("Found Security Manager set to null at method ").append(classNode.name).append(".").append(m.name).append("(").append(m.desc).append(")").append(BytecodeViewer.nl);
                            prevInsn_aconst_null = false;
                        }
                    }
                    else
                    {
                        prevInsn_aconst_null = false;
                    }
                }
            }
        }

        frame.appendText(sb.toString());
        frame.setVisible(true);
    }

}