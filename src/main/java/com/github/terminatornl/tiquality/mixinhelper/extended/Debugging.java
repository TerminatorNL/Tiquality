package com.github.terminatornl.tiquality.mixinhelper.extended;

import com.github.terminatornl.tiquality.mixinhelper.MixinConfigPlugin;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Field;
import java.util.ListIterator;

/**
 * The most basic and inefficient debugging tool I could come up with.
 */
public class Debugging {

    public static void dumpClassToFatalLog(ClassNode classNode) {
        MixinConfigPlugin.LOGGER.fatal("Dumping class methods: " + classNode.name);
        if (classNode.methods == null) {
            MixinConfigPlugin.LOGGER.fatal("No methods inside class");
            return;
        }
        for (MethodNode method : classNode.methods) {
            MixinConfigPlugin.LOGGER.fatal("Method name: " + method.name);
            MixinConfigPlugin.LOGGER.fatal("Method desc: " + method.desc);
            MixinConfigPlugin.LOGGER.fatal("Nodes: " + getInstructions(method));
        }
    }

    public static String getInstructions(MethodNode method) {
        StringBuilder builder = new StringBuilder();
        builder.append("\n");
        for (ListIterator<AbstractInsnNode> it = method.instructions.iterator(); it.hasNext(); ) {
            builder.append(getInstructionText(it.next()));
            builder.append("\n");
        }
        return builder.toString();
    }

    public static String translateOpcode(int opcode) {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        try {
            for (Field field : Opcodes.class.getFields()) {
                if (opcode == (Integer) field.get(null)) {
                    if (builder.length() > 1) {
                        builder.append("/");
                    }
                    builder.append(field.getName());
                }
            }
            builder.append(") (");
            builder.append(opcode);
            builder.append(")");
            return builder.toString();
        } catch (Exception e) {
            return "Unknown, error: " + e.getLocalizedMessage();
        }
    }

    public static String getInstructionText(AbstractInsnNode node) {
        String translation = translateOpcode(node.getOpcode());
        if (node instanceof LabelNode) {
            return "label: " + ((LabelNode) node).getLabel() + " " + translation;
        } else if (node instanceof FrameNode) {
            return "\tframe: L:" + ((FrameNode) node).local + " S:" + ((FrameNode) node).stack + " " + translation;
        } else if (node instanceof LineNumberNode) {
            return "\tline number: " + ((LineNumberNode) node).line + " " + translation;
        } else if (node instanceof InsnNode) {
            return "\tinsn: " + translation;
        } else if (node instanceof JumpInsnNode) {
            return "\tjump: " + ((JumpInsnNode) node).label.getLabel() + " " + translation;
        } else if (node instanceof FieldInsnNode) {
            return "\tfield: " + ((FieldInsnNode) node).name + " " + translation;
        } else if (node instanceof VarInsnNode) {
            return "\tvar: " + ((VarInsnNode) node).var + " " + translation;
        } else if (node instanceof MethodInsnNode) {
            return "\tmethod: " + ((MethodInsnNode) node).owner + "." + ((MethodInsnNode) node).name + ", " + ((MethodInsnNode) node).desc + " " + translation;
        } else if (node instanceof IincInsnNode) {
            return "\tinc: " + ((IincInsnNode) node).incr + " " + ((IincInsnNode) node).var;
        } else if (node instanceof TypeInsnNode) {
            return "\ttype: " + ((TypeInsnNode) node).desc;
        } else {
            return "\tUnknown: " + node.toString() + node.getClass().toGenericString();
        }
    }
}
