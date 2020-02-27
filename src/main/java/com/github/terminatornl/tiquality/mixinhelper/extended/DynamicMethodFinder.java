package com.github.terminatornl.tiquality.mixinhelper.extended;

import com.github.terminatornl.tiquality.mixinhelper.MixinConfigPlugin;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.objectweb.asm.tree.*;

import javax.annotation.Nullable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.terminatornl.tiquality.mixinhelper.MixinConfigPlugin.LOGGER;

/**
 * Remaps method calls to the target method within this class,
 * Much like @Shadow, but can use regex.
 */
public class DynamicMethodFinder implements Transformer {

    private final ClassNode classNode;

    public DynamicMethodFinder(ClassNode classNode) {
        this.classNode = classNode;
    }

    @Override
    public void transform() {
        LinkedList<ScheduledAction> scheduledActions = new LinkedList<>();
        for (MethodNode method : classNode.methods) {
            if (method.visibleAnnotations == null) {
                continue;
            }
            for (AnnotationNode annotation : method.visibleAnnotations) {
                if (annotation.desc.equals("Lcom/github/terminatornl/tiquality/mixinhelper/extended/DynamicMethodFinder$FindMethod;")) {
                    @Nullable String currentKey = null;
                    String deobfRegex = null;
                    String obfRegex = null;
                    for (Object key_value : annotation.values) {
                        if (currentKey == null) {
                            currentKey = (String) key_value;
                        } else {
                            if (currentKey.equals("deobfRegexName")) {
                                deobfRegex = (String) key_value;
                            } else if (currentKey.equals("obfRegexName")) {
                                obfRegex = (String) key_value;
                            }
                            currentKey = null;
                        }
                    }
                    String regexUsed = MixinConfigPlugin.isProductionEnvironment() ? obfRegex : deobfRegex;
                    if (regexUsed == null) {
                        LOGGER.fatal("Invalid annotation found. (@DynamicMethodFinder.FindMethod)");
                        FMLCommonHandler.instance().exitJava(-1, true);
                    } else {
                        findTarget(scheduledActions, method, regexUsed);
                    }
                }
            }
        }
        for (ScheduledAction action : scheduledActions) {
            action.apply();
        }
    }

    private void findTarget(LinkedList<ScheduledAction> scheduledActions, MethodNode instructor, String nameRegex) {
        final AtomicBoolean found = new AtomicBoolean(false);

        MethodHelper.findMethods(nameRegex, null, classNode, new MethodHelper.Handler() {
            @Override
            public void onFoundMethod(MethodNode node) {
                if (found.get()) {
                    LOGGER.fatal("@DynamicMethodFinder.FindMethod matched multiple targets. This is not allowed.");
                    FMLCommonHandler.instance().exitJava(-1, true);
                }
                found.set(true);
                if (instructor.equals(node) == false) {
                    scheduledActions.add(new ScheduledAction(instructor, node));
                }
            }
        });

        if (found.get() == false) {
            throw new IllegalStateException("Transformer did not find matches!");
        }
    }

    private void redirectMethodInsnNode(MethodNode target, MethodInsnNode node, MethodNode method) {
        LOGGER.info("Found target inside method: " + method.name);
        LOGGER.debug(Debugging.getInstructionText(node));
        node.name = target.name;
        node.desc = target.desc;
        LOGGER.debug("becomes...");
        LOGGER.debug(Debugging.getInstructionText(node));
    }

    /**
     * Redirects the method specified here to the found method
     */
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.METHOD)
    public @interface FindMethod {
        String deobfRegexName();

        String obfRegexName();
    }

    public class ScheduledAction {
        private final MethodNode instructor;
        private final MethodNode target;

        public ScheduledAction(MethodNode instructor, MethodNode target) {
            this.instructor = instructor;
            this.target = target;
        }

        public void apply() {
            LOGGER.info("Linking " + instructor.name + " to: " + target.name + "...");
            for (MethodNode method : classNode.methods) {
                for (ListIterator<AbstractInsnNode> it = method.instructions.iterator(); it.hasNext(); ) {
                    AbstractInsnNode node = it.next();
                    if (node instanceof MethodInsnNode) {
                        MethodInsnNode methodNode = (MethodInsnNode) node;
                        if (methodNode.name.equals(instructor.name)) {
                            if (Objects.equals(methodNode.desc, instructor.desc)) {
                                redirectMethodInsnNode(target, methodNode, method);
                            }
                        }
                    }
                }
            }
        }
    }
}
