package org.union4dev.deobfuscator.util;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.tinylog.Logger;
import org.union4dev.deobfuscator.asm.SuperClassWriter;

public class ClassNodeUtil {

    public static byte[] parseNode(ClassNode classNode) {
        ClassWriter writer = new SuperClassWriter(ClassWriter.COMPUTE_FRAMES);
        try {
            classNode.accept(writer);
        } catch (Throwable e) {
            if (e instanceof NegativeArraySizeException || e instanceof ArrayIndexOutOfBoundsException) {
                Logger.warn("Failed to compute frames while writing " + classNode.name + " COMPUTE_MAX instead.");
                writer = new SuperClassWriter(ClassWriter.COMPUTE_MAXS);
                classNode.accept(writer);
            } else if (e.getMessage() != null) {
                if (e.getMessage().equals("JSR/RET are not supported with computeFrames option")) {
                    Logger.warn(classNode.name + " contained JSR/RET COMPUTE_MAXS instead.");
                    writer = new SuperClassWriter(ClassWriter.COMPUTE_MAXS);
                    classNode.accept(writer);
                } else {
                    Logger.warn("Error while writing " + classNode.name + " due to " + e.getMessage());
                }
            } else {
                Logger.warn("Error while writing " + classNode.name + " stacktrace here.");
                Logger.error(e);
            }
        }
        return writer.toByteArray();
    }

    public static byte[] quickParseNode(ClassNode classNode) {
        final ClassWriter classWriter = new ClassWriter(0);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    public static ClassNode quickParseBytes(byte[] bytes) {
        final ClassReader classReader = new ClassReader(bytes);
        final ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        return classNode;
    }

    public static AbstractInsnNode getPrevious(AbstractInsnNode instruction, int step) {
        int currentStep = 0;
        do {
            instruction = instruction.getPrevious();
            if (instruction.getOpcode() != -1) currentStep++;
        } while (currentStep != step);
        return instruction;
    }

    public static AbstractInsnNode getNext(AbstractInsnNode instruction, int step) {
        int currentStep = 0;
        do {
            instruction = instruction.getNext();
            if (instruction.getOpcode() != -1) currentStep++;
        } while (currentStep != step);
        return instruction;
    }

    public static boolean isNumber(AbstractInsnNode node) {
        if (node.getOpcode() >= Opcodes.ICONST_M1 && node.getOpcode() <= Opcodes.SIPUSH)
            return true;
        if (node instanceof LdcInsnNode) {
            final LdcInsnNode ldc = (LdcInsnNode)node;
            return ldc.cst instanceof Number;
        }
        return false;
    }

    public static boolean isInteger(AbstractInsnNode node) {
        if (node.getOpcode() >= Opcodes.ICONST_M1 && node.getOpcode() <= Opcodes.ICONST_5)
            return true;
        if (node.getOpcode() == Opcodes.SIPUSH || node.getOpcode() == Opcodes.BIPUSH)
            return true;
        if (node instanceof LdcInsnNode) {
            final LdcInsnNode ldc = (LdcInsnNode)node;
            return ldc.cst instanceof Integer;
        }
        return false;
    }

    public static int getInteger(AbstractInsnNode node) {
        if (node.getOpcode() >= Opcodes.ICONST_M1 && node.getOpcode() <= Opcodes.ICONST_5)
            return node.getOpcode() - 3;
        if (node.getOpcode() == Opcodes.SIPUSH || node.getOpcode() == Opcodes.BIPUSH)
            return ((IntInsnNode)node).operand;
        if (node instanceof LdcInsnNode) {
            final LdcInsnNode ldc = (LdcInsnNode)node;
            if (ldc.cst instanceof Integer) {
                return (int) ldc.cst;
            }
        }
        return 0;
    }

    public static AbstractInsnNode getIntInsn(int number) {
        if (number >= -1 && number <= 5)
            return new InsnNode(number + 3);
        else if (number >= -128 && number <= 127)
            return new IntInsnNode(Opcodes.BIPUSH, number);
        else if (number >= -32768 && number <= 32767)
            return new IntInsnNode(Opcodes.SIPUSH, number);
        else
            return new LdcInsnNode(number);
    }

    public static Number getNumber(AbstractInsnNode node) {
        if (node.getOpcode() >= Opcodes.ICONST_M1 && node.getOpcode() <= Opcodes.ICONST_5)
            return node.getOpcode() - 3;
        if (node.getOpcode() == Opcodes.SIPUSH || node.getOpcode() == Opcodes.BIPUSH)
            return ((IntInsnNode)node).operand;
        if (node instanceof LdcInsnNode) {
            final LdcInsnNode ldc = (LdcInsnNode)node;
            if (ldc.cst instanceof Number) {
                return (Number) ldc.cst;
            }
        }
        return null;
    }

    public static boolean isLong(AbstractInsnNode node) {
        if (node instanceof LdcInsnNode) {
            final LdcInsnNode ldc = (LdcInsnNode)node;
            return ldc.cst instanceof Long;
        }
        return false;
    }

    public static long getLong(AbstractInsnNode node) {
        if (node instanceof LdcInsnNode) {
            final LdcInsnNode ldc = (LdcInsnNode)node;
            if (ldc.cst instanceof Long) {
                return (long) ldc.cst;
            }
        }
        return 0L;
    }

    public static boolean isString(AbstractInsnNode node) {
        if (node instanceof LdcInsnNode) {
            final LdcInsnNode ldc = (LdcInsnNode) node;
            return ldc.cst instanceof String;
        }
        return false;
    }

    public static String getString(AbstractInsnNode node) {
        if (node instanceof LdcInsnNode) {
            final LdcInsnNode ldc = (LdcInsnNode) node;
            if (ldc.cst instanceof String) return (String) ldc.cst;
        }
        return null;
    }

    public static MethodNode getMethod(ClassNode classNode, String name, String desc) {
        for (MethodNode method : classNode.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) {
                return method;
            }
        }
        return null;
    }

    public static FieldNode getField(ClassNode classNode, String name, String desc) {
        for (FieldNode fieldNode : classNode.fields) {
            if (fieldNode.name.equals(name) && fieldNode.desc.equals(desc)) {
                return fieldNode;
            }
        }
        return null;
    }
}
