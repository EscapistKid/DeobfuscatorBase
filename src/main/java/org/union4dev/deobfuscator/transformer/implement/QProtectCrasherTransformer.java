package org.union4dev.deobfuscator.transformer.implement;

import org.objectweb.asm.tree.*;
import org.union4dev.deobfuscator.transformer.Transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QProtectCrasherTransformer extends Transformer {

    /*
    catch xxxx A B C
    catch xxxx D C C
    A:
    xxx
    B:
    xxx
    C:
    goto D
    D:
    dup
    ifnull C
    (dup
    putstatic xxx Ljava/lang/Throwable;)
    (invokestatic xxx (Ljava/lang/Throwable;)Ljava/lang/Throwable;)
    athrow
     */

    @Override
    public void transform(Map<String, ClassNode> nodeMap) {
        int count = 0;
        for (ClassNode classNode : nodeMap.values()) {
            for (MethodNode method : classNode.methods) {
                if (method.tryCatchBlocks == null || method.tryCatchBlocks.isEmpty()) continue;
                final List<TryCatchBlockNode> taskRemove = new ArrayList<>();
                for (TryCatchBlockNode tryCatchBlock : method.tryCatchBlocks) {
                    if (tryCatchBlock.handler.getNext() instanceof JumpInsnNode) {
                        final JumpInsnNode jumpInsnNode = (JumpInsnNode) tryCatchBlock.handler.getNext();
                        if (isCrasherHandler(jumpInsnNode.label)) taskRemove.add(tryCatchBlock);
                    } else if (isCrasherHandler(tryCatchBlock.handler)) taskRemove.add(tryCatchBlock);
                }
                for (TryCatchBlockNode tryCatchBlockNode : taskRemove) {
                    method.tryCatchBlocks.remove(tryCatchBlockNode);
                }
                count += taskRemove.size();
            }
        }
        System.out.println("Finish QProtectCrasherTransformer with " + count + " trash tcbs removed.");
    }

    private boolean isCrasherHandler(LabelNode labelNode) {
        try {
            if (labelNode.getNext().getOpcode() == DUP) {
                if (labelNode.getNext().getNext().getOpcode() == IFNULL) {
                    if (labelNode.getNext().getNext().getNext().getOpcode() == DUP) {
                        if (labelNode.getNext().getNext().getNext().getNext().getOpcode() == PUTSTATIC) {
                            final FieldInsnNode fieldInsnNode = (FieldInsnNode) labelNode.getNext().getNext().getNext().getNext();
                            return fieldInsnNode.desc.equals("Ljava/lang/Throwable;") && labelNode.getNext().getNext().getNext().getNext().getNext().getOpcode() == ATHROW;
                        } else return false;
                    } else if (labelNode.getNext().getNext().getNext().getOpcode() == INVOKESTATIC) {
                        final MethodInsnNode methodInsnNode = (MethodInsnNode) labelNode.getNext().getNext().getNext();
                        return methodInsnNode.desc.equals("(Ljava/lang/Throwable;)Ljava/lang/Throwable;") && labelNode.getNext().getNext().getNext().getNext().getOpcode() == ATHROW;
                    } else return false;
                } else if (labelNode.getNext().getNext().getOpcode() == PUTSTATIC) {
                    final FieldInsnNode fieldInsnNode = (FieldInsnNode) labelNode.getNext().getNext();
                    return fieldInsnNode.desc.equals("Ljava/lang/Throwable;") && labelNode.getNext().getNext().getNext().getOpcode() == ATHROW;
                }
            }
            return false;
        } catch (NullPointerException e) {
            return false;
        }
    }
}
