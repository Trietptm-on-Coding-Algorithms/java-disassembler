package club.bytecode.the.jda.decompilers.bytecode;

import club.bytecode.the.jda.api.ExceptionUI;
import org.apache.commons.lang3.StringEscapeUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author Konloch
 * @author Bibl
 */
public class InstructionPrinter {
    protected final MethodNodeDecompiler parent;

    /**
     * The MethodNode to print
     **/
    protected final MethodNode mNode;
    protected final TypeAndName[] args;

    protected int[] pattern;
    protected boolean match;

    protected List<AbstractInsnNode> matchedInsns;
    protected List<Integer>[] handlers;
    protected Map<LabelNode, Integer> labels;

    public InstructionPrinter(MethodNodeDecompiler parent, MethodNode m, TypeAndName[] args) {
        this.parent = parent;
        this.args = args;
        mNode = m;
        labels = new HashMap<>();
        // matchedInsns = new ArrayList<AbstractInsnNode>(); // ingnored because
        // match = false
        match = false;

        buildHandlerLabelCache();
    }

    private void buildHandlerLabelCache() {
        // enumerate labels
        for(Iterator<AbstractInsnNode> it = mNode.instructions.iterator(); it.hasNext(); ) {
            AbstractInsnNode ain = it.next();
            if (ain instanceof LabelNode)
                resolveLabel((LabelNode) ain);
        }

        // init handlers array
        handlers = new List[labels.size()];
        for (int i = 0 ; i < handlers.length; i++)
            handlers[i] = new ArrayList<>();

        // ok, now for the main attraction
        for (TryCatchBlockNode tc : mNode.tryCatchBlocks) {
            int startIdx = mNode.instructions.indexOf(tc.start);
            int endIdx = mNode.instructions.indexOf(tc.end);

            for (int i = startIdx; i < endIdx; i++) {
                AbstractInsnNode ain = mNode.instructions.get(i);
                if (ain instanceof LabelNode) {
                    int label = labels.get(ain); // label number
                    handlers[label - 1].add(labels.get(tc.handler));
                }
            }
        }
    }

    /**
     * Creates the print
     *
     * @return The print as an ArrayList
     */
    public ArrayList<String> createPrint() {
        ArrayList<String> info = new ArrayList<>();
        ListIterator<?> it = mNode.instructions.iterator();
        boolean firstLabel = false;
        while (it.hasNext()) {
            AbstractInsnNode ain = (AbstractInsnNode) it.next();
            String line;
            if (ain instanceof VarInsnNode) {
                line = printVarInsnNode((VarInsnNode) ain, it);
            } else if (ain instanceof IntInsnNode) {
                line = printIntInsnNode((IntInsnNode) ain, it);
            } else if (ain instanceof FieldInsnNode) {
                line = printFieldInsnNode((FieldInsnNode) ain, it);
            } else if (ain instanceof MethodInsnNode) {
                line = printMethodInsnNode((MethodInsnNode) ain, it);
            } else if (ain instanceof LdcInsnNode) {
                line = printLdcInsnNode((LdcInsnNode) ain, it);
            } else if (ain instanceof InsnNode) {
                line = printInsnNode((InsnNode) ain, it);
            } else if (ain instanceof JumpInsnNode) {
                line = printJumpInsnNode((JumpInsnNode) ain, it);
            } else if (ain instanceof LineNumberNode) {
                line = printLineNumberNode((LineNumberNode) ain, it);
            } else if (ain instanceof LabelNode) {
                if (firstLabel && parent.createLabelBrackets())
                    info.add("}");

                line = printLabelnode((LabelNode) ain) + ":";
                int labelId = resolveLabel((LabelNode) ain);

                if (parent.createLabelBrackets()) {
                    if (!firstLabel)
                        firstLabel = true;
                    line += " {";
                }

                StringBuilder comment = new StringBuilder();
                List<Integer> handlerLabels = handlers[labels.get(ain) - 1];
                if (parent.appendHandlerComments() && handlerLabels.size() > 0) {
                    comment.append("Handlers: ");
                }
                for (int handler : handlerLabels) {
                    if (parent.appendHandlerComments())
                        comment.append("L").append(handler).append(" ");
                    if (handler == labelId && parent.createComments())
                        comment.insert(0, "Finally block" + (parent.appendHandlerComments() ? ", " : ""));
                }
                if (comment.length() > 0) {
                    line += " // " + comment;
                }
            } else if (ain instanceof TypeInsnNode) {
                line = printTypeInsnNode((TypeInsnNode) ain);
            } else if (ain instanceof FrameNode) {
                line = "";
            } else if (ain instanceof IincInsnNode) {
                line = printIincInsnNode((IincInsnNode) ain);
            } else if (ain instanceof TableSwitchInsnNode) {
                line = printTableSwitchInsnNode((TableSwitchInsnNode) ain);
            } else if (ain instanceof LookupSwitchInsnNode) {
                line = printLookupSwitchInsnNode((LookupSwitchInsnNode) ain);
            } else if (ain instanceof InvokeDynamicInsnNode) {
                line = printInvokeDynamicInsNode((InvokeDynamicInsnNode) ain);
            } else if (ain instanceof MultiANewArrayInsnNode) {
                line = printMultiANewArrayInsnNode((MultiANewArrayInsnNode) ain);
            } else {
                line = "// UNADDED OPCODE: " + nameOpcode(ain.getOpcode()) + " " + ain.toString();
            }
            if (!line.equals("")) {
                if (match)
                    if (matchedInsns.contains(ain))
                        line = "   -> " + line;

                info.add(line);
            }
        }
        if (firstLabel && parent.createLabelBrackets())
            info.add("}");
        return info;
    }

    protected String printVarInsnNode(VarInsnNode vin, ListIterator<?> it) {
        StringBuilder sb = new StringBuilder();
        sb.append(nameOpcode(vin.getOpcode()));
        sb.append(" ");
        sb.append(vin.var);
        if (parent.createComments()) {
            if (vin.var == 0 && !Modifier.isStatic(mNode.access)) {
                sb.append(" // Reference to self");
            } else {
                final int refIndex = vin.var - (Modifier.isStatic(mNode.access) ? 0 : 1);
                if (refIndex >= 0 && refIndex < args.length - 1) {
                    sb.append(" // Reference to ").append(args[refIndex].name);
                }
            }
        }

        return sb.toString();
    }

    protected String printIntInsnNode(IntInsnNode iin, ListIterator<?> it) {
        return nameOpcode(iin.getOpcode()) + " " + iin.operand;
    }

    protected String printFieldInsnNode(FieldInsnNode fin, ListIterator<?> it) {
        String desc = fin.desc;
        return nameOpcode(fin.getOpcode()) + " " + fin.owner + "." + fin.name + ":" + desc;
    }

    protected String printMethodInsnNode(MethodInsnNode min, ListIterator<?> it) {
        StringBuilder sb = new StringBuilder();
        sb.append(nameOpcode(min.getOpcode())).append(" ").append(min.owner).append(" ").append(min.name).append("(");

        String desc = min.desc;
        sb.append(desc);

        sb.append(");");

        return sb.toString();
    }

    protected String printLdcInsnNode(LdcInsnNode ldc, ListIterator<?> it) {
        if (ldc.cst instanceof String)
            return nameOpcode(ldc.getOpcode()) + " \"" + StringEscapeUtils.escapeJava(ldc.cst.toString()) + "\" (" + ldc.cst.getClass().getCanonicalName() + ")";

        return nameOpcode(ldc.getOpcode()) + " " + StringEscapeUtils.escapeJava(ldc.cst.toString()) + " (" + ldc.cst.getClass().getCanonicalName() + ")";
    }

    protected String printInsnNode(InsnNode in, ListIterator<?> it) {
        return nameOpcode(in.getOpcode());
    }

    protected String printJumpInsnNode(JumpInsnNode jin, ListIterator<?> it) {
        String line = nameOpcode(jin.getOpcode()) + " L" + resolveLabel(jin.label);
        return line;
    }

    protected String printLineNumberNode(LineNumberNode lin, ListIterator<?> it) {
        return "";
    }

    protected String printLabelnode(LabelNode label) {
        return "L" + resolveLabel(label);
    }

    protected String printTypeInsnNode(TypeInsnNode tin) {
        try {
            String desc = tin.desc;
            return nameOpcode(tin.getOpcode()) + " " + desc;
        } catch (Exception e) {
            new ExceptionUI(e, "printing instruction");
        }
        return "// error";
    }

    protected String printIincInsnNode(IincInsnNode iin) {
        return nameOpcode(iin.getOpcode()) + " " + iin.var + " " + iin.incr;
    }

    protected String printTableSwitchInsnNode(TableSwitchInsnNode tin) {
        String line = nameOpcode(tin.getOpcode()) + " \n";
        List<?> labels = tin.labels;
        int count = 0;
        for (int i = tin.min; i < tin.max + 1; i++) {
            line += "                val: " + i + " -> " + "L" + resolveLabel((LabelNode) labels.get(count++)) + "\n";
        }
        line += "                default" + " -> L" + resolveLabel(tin.dflt) + "";
        return line;
    }

    protected String printLookupSwitchInsnNode(LookupSwitchInsnNode lin) {
        String line = nameOpcode(lin.getOpcode()) + ": \n";
        List<?> keys = lin.keys;
        List<?> labels = lin.labels;

        for (int i = 0; i < keys.size(); i++) {
            int key = (Integer) keys.get(i);
            LabelNode label = (LabelNode) labels.get(i);
            line += "                val: " + key + " -> " + "L" + resolveLabel(label) + "\n";
        }
        line += "                default" + " -> L" + resolveLabel(lin.dflt) + "";
        return line;
    }

    protected String printInvokeDynamicInsNode(InvokeDynamicInsnNode idin) {
        StringBuilder sb = new StringBuilder();
        final String bsmName = idin.bsm.getName();
        sb.append(nameOpcode(idin.getOpcode())).append(" ").append(bsmName).append("<");

        String desc = idin.desc;
        sb.append(desc);
        sb.append(">(\n");
        Object[] bsmArgs = idin.bsmArgs;
        for (int i = 0; i < bsmArgs.length; i++) {
            Object arg = bsmArgs[i];
            sb.append("                ");
            sb.append(arg);
            if (i < bsmArgs.length - 1) {
                sb.append(", ");
            }
            switch(i) {
                case 0:
                    sb.append(" // caller");
                    break;
                case 1:
                    sb.append(" // invokedName");
                    break;
                case 2:
                    sb.append(" // invokedType");
                    break;                        
            }
            sb.append("\n");
        }

        sb.append("             );");

        return sb.toString();
    }

    protected String printMultiANewArrayInsnNode(MultiANewArrayInsnNode manain) {
        return nameOpcode(manain.getOpcode()) + " " + manain.desc;
    }

    protected String nameOpcode(int opcode) {
        return "    " + OpcodeInfo.OPCODES.getOrDefault(opcode, String.format("unknown#%02x", opcode)).toLowerCase();
    }

    protected int resolveLabel(LabelNode label) {
        if (labels.containsKey(label)) {
            return labels.get(label);
        } else {
            int newLabelIndex = labels.size() + 1;
            labels.put(label, newLabelIndex);
            return newLabelIndex;
        }
    }
}
