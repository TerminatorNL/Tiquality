package com.github.terminatornl.tiquality.mixinhelper.extended;

import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.MethodNode;

import javax.annotation.Nullable;
import java.util.regex.Pattern;

public class MethodHelper {

    public static boolean isExcluded(MethodNode node) {
        if (node.visibleAnnotations == null) {
            return false;
        }
        for (AnnotationNode a : node.visibleAnnotations) {
            if ("Lcom/github/terminatornl/tiquality/mixinhelper/extended/DynamicExclusion;".equals(a.desc)) {
                return true;
            }
        }
        return false;
    }

    public static void findMethods(@Nullable String nameRegex, @Nullable String signatureRegex, ClassNode classNode, Handler handler) {
        if (nameRegex != null && nameRegex.equals("")) {
            nameRegex = null;
        }
        if (signatureRegex != null && signatureRegex.equals("")) {
            signatureRegex = null;
        }

        Pattern namePattern = nameRegex == null ? null : Pattern.compile(nameRegex);
        Pattern signaturePattern = signatureRegex == null ? null : Pattern.compile(signatureRegex);

        if (namePattern != null) {
            for (MethodNode node : classNode.methods) {
                if (namePattern.matcher(node.name).find()) {
                    if (signaturePattern == null) {
                        handler.onFoundMethod(node);
                    } else {
                        if (node.signature != null && signaturePattern.matcher(node.signature).find()) {
                            handler.onFoundMethod(node);
                        }
                    }
                }
            }
        } else if (signaturePattern != null) {
            for (MethodNode node : classNode.methods) {
                if (signaturePattern.matcher(node.signature).find()) {
                    handler.onFoundMethod(node);
                }
            }
        } else {
            throw new IllegalArgumentException("Namepattern and SignaturePattern cannot both be null!");
        }
    }

    public interface Handler {
        void onFoundMethod(MethodNode node);
    }
}
