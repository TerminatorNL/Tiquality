package com.github.terminatornl.tiquality.mixinhelper.extended;

import com.github.terminatornl.tiquality.mixinhelper.MixinConfigPlugin;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static com.github.terminatornl.tiquality.mixinhelper.MixinConfigPlugin.LOGGER;

/**
 * Remaps method calls to the target method within this class,
 * Much like @Shadow, but can use regex.
 */
public class DynamicMethodRedirector implements Transformer {

    private final ClassNode classNode;

    public DynamicMethodRedirector(ClassNode classNode) {
        this.classNode = classNode;
    }

    @Override
    public void transform() {
        LinkedList<Task> scheduledActions = new LinkedList<>();
        for (MethodNode method : classNode.methods) {
            if (method.visibleAnnotations == null) {
                continue;
            }
            for (AnnotationNode annotation : method.visibleAnnotations) {
                if (annotation.desc.equals("Lcom/github/terminatornl/tiquality/mixinhelper/extended/DynamicMethodRedirector$RedirectMethod;")) {
                    @Nullable String currentKey = null;
                    String deobfRegexName = null;
                    String obfRegexName = null;
                    String deobfRegexOwner = null;
                    String obfRegexOwner = null;
                    for (Object key_value : annotation.values) {
                        if (currentKey == null) {
                            currentKey = (String) key_value;
                        } else {
                            if (currentKey.equals("deobfRegexName")) {
                                deobfRegexName = (String) key_value;
                            } else if (currentKey.equals("obfRegexName")) {
                                obfRegexName = (String) key_value;
                            } else if (currentKey.equals("deobfRegexOwner")) {
                                deobfRegexOwner = (String) key_value;
                            } else if (currentKey.equals("obfRegexOwner")) {
                                obfRegexOwner = (String) key_value;
                            }
                            currentKey = null;
                        }
                    }
                    String nameRegexUsed = MixinConfigPlugin.isProductionEnvironment() ? obfRegexName : deobfRegexName;
                    String ownerRegexUsed = MixinConfigPlugin.isProductionEnvironment() ? obfRegexOwner : deobfRegexOwner;
                    if (nameRegexUsed == null || ownerRegexUsed == null) {
                        LOGGER.fatal("Invalid annotation found. (@DynamicMethodRedirector.RedirectMethod)");
                        FMLCommonHandler.instance().exitJava(-1, true);
                    } else {
                        findTargets(scheduledActions, method, nameRegexUsed, ownerRegexUsed);
                    }
                }
            }
        }
        for (Task action : scheduledActions) {
            action.apply();
        }
    }

    private void findTargets(@Nonnull LinkedList<Task> scheduledActions, @Nonnull MethodNode instructor, @Nonnull String nameRegex, @Nonnull String ownerRegex) {
        final AtomicBoolean found = new AtomicBoolean(false);
        Pattern namePattern = Pattern.compile(nameRegex);
        Pattern ownerPattern = Pattern.compile(ownerRegex);

        for (MethodNode method : classNode.methods) {
            if (MethodHelper.isExcluded(method)) {
                continue;
            }
            ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();
            while (iterator.hasNext()) {
                AbstractInsnNode node = iterator.next();
                if (node instanceof MethodInsnNode) {
                    MethodInsnNode methodNode = (MethodInsnNode) node;
                    if (namePattern.matcher(methodNode.name).matches() && ownerPattern.matcher(methodNode.owner).matches()) {
                        found.set(true);
                        if (methodNode.getOpcode() == Opcodes.INVOKESTATIC) {
                            scheduledActions.add(new RedirectMethodNode(instructor, methodNode));
                        } else {
                            scheduledActions.add(new RedirectAndConvertToStatic(instructor, methodNode));
                        }
                    }
                }
            }
        }

        if (found.get() == false) {
            throw new IllegalStateException("Transformer did not find matches! " + nameRegex + " - " + ownerRegex);
        }
    }

    /**
     * Redirects the method specified here to the found method
     */
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.METHOD)
    public @interface RedirectMethod {
        String deobfRegexName();

        String obfRegexName();

        String deobfRegexOwner();

        String obfRegexOwner();
    }

    interface Task {
        void apply();
    }

    public class RedirectAndConvertToStatic implements Task {
        private final MethodNode instructor;
        private final MethodInsnNode node;

        public RedirectAndConvertToStatic(MethodNode instructor, MethodInsnNode node) {
            this.instructor = instructor;
            this.node = node;
        }

        @Override
        public void apply() {
            node.setOpcode(Opcodes.INVOKESTATIC);
            node.owner = classNode.name;
            node.name = instructor.name;
            node.desc = instructor.desc;
        }
    }

    public class RedirectMethodNode implements Task {
        private final MethodNode instructor;
        private final MethodInsnNode node;

        public RedirectMethodNode(MethodNode instructor, MethodInsnNode node) {
            this.instructor = instructor;
            this.node = node;
        }

        @Override
        public void apply() {
            node.owner = classNode.name;
            node.name = instructor.name;
            node.desc = instructor.desc;
        }
    }
}
