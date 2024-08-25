package org.union4dev.deobfuscator.transformer.implement;

import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.tinylog.Logger;
import org.union4dev.deobfuscator.transformer.Transformer;
import org.union4dev.deobfuscator.util.ClassNodeUtil;
import org.union4dev.deobfuscator.util.InstructionModifier;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class QProtectFlowTransformer extends Transformer {

    private static final List<Integer> INSN_JUMPS = Arrays.asList(IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE);

    @Override
    public void transform(Map<String, ClassNode> nodeMap) {
        Logger.info("Start QProtectFlowTransformer.");
        final AtomicInteger counter1 = new AtomicInteger();
        final AtomicInteger counter2 = new AtomicInteger();
        for (ClassNode classNode : nodeMap.values()) {
            for (MethodNode method : classNode.methods) {
                part1(method, counter1);
                part2(classNode, method, counter2);
            }
        }
        Logger.info("Finish QProtectFlowTransformer with " + counter1.get() + " junk code removed & " + counter2.get() + " fake jump removed.");
    }

    /*
    A:
    getstatic xxxx.flag Z
    dup
    (swap)
    ifne B
    ifeq C
    [num]   # balance.
    B:
    pop
    xxxx
    C:
    xxxx
     */
    private void part1(MethodNode methodNode, AtomicInteger counter) {
        final InstructionModifier modifier = new InstructionModifier();
        for (AbstractInsnNode instruction : methodNode.instructions) {
            if (instruction.getOpcode() == DUP) {
                final List<AbstractInsnNode> cleanList = getCleanList(instruction);
                if (!cleanList.isEmpty()) {
                    for (AbstractInsnNode abstractInsnNode : cleanList) {
                        modifier.remove(abstractInsnNode);
                    }
                    counter.incrementAndGet();
                }
            }
        }
        modifier.apply(methodNode);
    }

    /*
    aload this
    astore v
    xxxx
    aload v
    ifnull X
     */
    private void part2(ClassNode classNode, MethodNode methodNode, AtomicInteger counter) {
        final List<Integer> targetLocal = getTargetLocal(methodNode);
        if (targetLocal.isEmpty()) return;

        try {
            final Frame<SourceValue>[] frames = new Analyzer<>(new SourceInterpreter()).analyze(classNode.name, methodNode);
            final InstructionModifier modifier = new InstructionModifier();
            for (AbstractInsnNode instruction : methodNode.instructions) {
                if (instruction.getOpcode() == IFNULL || instruction.getOpcode() == IFNONNULL) {
                    final Frame<SourceValue> frame = frames[methodNode.instructions.indexOf(instruction)];
                    if (frame != null) {
                        final SourceValue stack = frame.getStack(frame.getStackSize() - 1);
                        final AbstractInsnNode insnNode = stack.insns.iterator().next();
                        if (insnNode.getOpcode() == ALOAD) {
                            final VarInsnNode varInsnNode = (VarInsnNode) insnNode;
                            if (targetLocal.contains(varInsnNode.var)) {
                                modifier.replace(instruction, new InsnNode(POP));
                                counter.incrementAndGet();
                            }
                        }
                    }
                }
            }
            modifier.apply(methodNode);
        } catch (AnalyzerException e) {
            e.printStackTrace(System.err);
        }
    }

    private List<Integer> getTargetLocal(MethodNode methodNode) {
        final List<Integer> targets = new ArrayList<>();
        for (AbstractInsnNode instruction : methodNode.instructions) {
            if (instruction.getOpcode() == ALOAD) {
                final VarInsnNode varInsnNode = (VarInsnNode) instruction;
                if (varInsnNode.var == 0 && !Modifier.isStatic(methodNode.access)) {
                    if (varInsnNode.getNext().getOpcode() == ASTORE) {
                        final VarInsnNode varInsnNode1 = (VarInsnNode) instruction.getNext();
                        targets.add(varInsnNode1.var);
                    }
                }
            } else if (instruction.getOpcode() == ACONST_NULL && Modifier.isStatic(methodNode.access)) {
                if (instruction.getNext().getOpcode() == ASTORE) {
                    final VarInsnNode varInsnNode1 = (VarInsnNode) instruction.getNext();
                    targets.add(varInsnNode1.var);
                }
            }
        }
        return targets;
    }

    private List<AbstractInsnNode> getCleanList(AbstractInsnNode start) {
        final List<AbstractInsnNode> result = new ArrayList<>();
        try {
            if (start.getNext().getOpcode() == SWAP) {
                if (INSN_JUMPS.contains(start.getNext().getNext().getOpcode())) {
                    if (start.getNext().getNext().getNext().getOpcode() == getOpposedOpcode(start.getNext().getNext().getOpcode()) && ClassNodeUtil.isInteger(start.getNext().getNext().getNext().getNext())) {
                        final JumpInsnNode jumpInsnNode = (JumpInsnNode) start.getNext().getNext();
                        if (jumpInsnNode.label.getNext().getOpcode() == POP) {
                            result.add(jumpInsnNode.label.getNext());
                            result.add(start);
                            result.add(start.getNext());
                            result.add(start.getNext().getNext());
                            result.add(start.getNext().getNext().getNext().getNext());
                        }
                    }
                }
            } else if (INSN_JUMPS.contains(start.getNext().getOpcode())) {
                if (start.getNext().getNext().getOpcode() == getOpposedOpcode(start.getNext().getOpcode()) && ClassNodeUtil.isInteger(start.getNext().getNext().getNext())) {
                    final JumpInsnNode jumpInsnNode = (JumpInsnNode) start.getNext();
                    if (jumpInsnNode.label.getNext().getOpcode() == POP) {
                        result.add(jumpInsnNode.label.getNext());
                        result.add(start);
                        result.add(start.getNext());
                        result.add(start.getNext().getNext().getNext());
                    }
                }
            }
        } catch (NullPointerException e) {
            result.clear();
        }
        return result;
    }

    private int getOpposedOpcode(int opcode) {
        switch (opcode) {
            case IFEQ: {
                return IFNE;
            }
            case IFNE: {
                return IFEQ;
            }
            case IFLT: {
                return IFGE;
            }
            case IFGE: {
                return IFLT;
            }
            case IFGT: {
                return IFLE;
            }
            case IFLE: {
                return IFGT;
            }
        }
        return -1;
    }
}
