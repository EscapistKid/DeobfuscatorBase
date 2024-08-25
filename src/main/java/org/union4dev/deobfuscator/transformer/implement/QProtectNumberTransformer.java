package org.union4dev.deobfuscator.transformer.implement;

import org.objectweb.asm.tree.*;
import org.tinylog.Logger;
import org.union4dev.deobfuscator.transformer.Transformer;
import org.union4dev.deobfuscator.util.ClassNodeUtil;
import org.union4dev.deobfuscator.util.InstructionModifier;
import org.union4dev.deobfuscator.util.TransformerHelper;

import java.util.Map;
import java.util.Objects;

public class QProtectNumberTransformer extends Transformer {
    @Override
    public void transform(Map<String, ClassNode> nodeMap) {
        Logger.info("Start QProtectNumberTransformer.");
        int count = 0;
        for (ClassNode classNode : nodeMap.values()) {
            for (MethodNode method : classNode.methods) {
                boolean modified;
                do {
                    modified = false;
                    final InstructionModifier modifier = new InstructionModifier();
                    for (AbstractInsnNode instruction : method.instructions) {
                        if (TransformerHelper.isInvokeStatic(instruction, "java/lang/Integer", "parseInt", "(Ljava/lang/String;I)I") && ClassNodeUtil.isInteger(instruction.getPrevious()) && ClassNodeUtil.isString(instruction.getPrevious().getPrevious())) {
                            final int result = Integer.parseInt(ClassNodeUtil.getString(instruction.getPrevious().getPrevious()), ClassNodeUtil.getInteger(instruction.getPrevious()));
                            modifier.remove(instruction.getPrevious().getPrevious());
                            modifier.remove(instruction.getPrevious());
                            modifier.replace(instruction, ClassNodeUtil.getIntInsn(result));
                            modified = true;
                            count++;
                            break;
                        } else if (TransformerHelper.isInvokeStatic(instruction, "java/lang/Long", "parseLong", "(Ljava/lang/String;I)J") && ClassNodeUtil.isInteger(instruction.getPrevious()) && ClassNodeUtil.isString(instruction.getPrevious().getPrevious())) {
                            final long result = Long.parseLong(ClassNodeUtil.getString(instruction.getPrevious().getPrevious()), ClassNodeUtil.getInteger(instruction.getPrevious()));
                            modifier.remove(instruction.getPrevious().getPrevious());
                            modifier.remove(instruction.getPrevious());
                            modifier.replace(instruction, new LdcInsnNode(result));
                            modified = true;
                            count++;
                            break;
                        } else if (TransformerHelper.isInvokeVirtual(instruction, "java/lang/String", "hashCode", "()I") && ClassNodeUtil.isString(instruction.getPrevious())) {
                            final int result = ClassNodeUtil.getString(instruction.getPrevious()).hashCode();
                            modifier.remove(instruction.getPrevious());
                            modifier.replace(instruction, ClassNodeUtil.getIntInsn(result));
                            modified = true;
                            count++;
                            break;
                        } else if (TransformerHelper.isInvokeStatic(instruction, "java/lang/Float", "intBitsToFloat", "(I)F") && ClassNodeUtil.isInteger(instruction.getPrevious())) {
                            final float result = Float.intBitsToFloat(ClassNodeUtil.getInteger(instruction.getPrevious()));
                            modifier.remove(instruction.getPrevious());
                            modifier.replace(instruction, new LdcInsnNode(result));
                            modified = true;
                            count++;
                            break;
                        } else if (TransformerHelper.isInvokeStatic(instruction, "java/lang/Float", "floatToIntBits", "(F)I") && ClassNodeUtil.isNumber(instruction.getPrevious())) {
                            final Number number = ClassNodeUtil.getNumber(instruction.getPrevious());
                            if (number instanceof Float) {
                                final int result = Float.floatToIntBits(number.floatValue());
                                modifier.remove(instruction.getPrevious());
                                modifier.replace(instruction, ClassNodeUtil.getIntInsn(result));
                                modified = true;
                                count++;
                                break;
                            }
                        } else if (TransformerHelper.isInvokeStatic(instruction, "java/lang/Double", "longBitsToDouble", "(J)D") && ClassNodeUtil.isLong(instruction.getPrevious())) {
                            final double result = Double.longBitsToDouble(ClassNodeUtil.getLong(instruction.getPrevious()));
                            modifier.remove(instruction.getPrevious());
                            modifier.replace(instruction, new LdcInsnNode(result));
                            modified = true;
                            count++;
                            break;
                        } else if (TransformerHelper.isInvokeStatic(instruction, "java/lang/Double", "doubleToLongBits", "(D)J") && ClassNodeUtil.isNumber(instruction.getPrevious())) {
                            final Number number = ClassNodeUtil.getNumber(instruction.getPrevious());
                            if (number instanceof Double) {
                                final long result = Double.doubleToLongBits(number.doubleValue());
                                modifier.remove(instruction.getPrevious());
                                modifier.replace(instruction, new LdcInsnNode(result));
                                modified = true;
                                count++;
                                break;
                            }
                        } else if (instruction.getOpcode() == I2B && ClassNodeUtil.isInteger(instruction.getPrevious())) {
                            final byte result = (byte) ClassNodeUtil.getInteger(instruction.getPrevious());
                            modifier.remove(instruction.getPrevious());
                            modifier.replace(instruction, new IntInsnNode(BIPUSH, result));
                            modified = true;
                            count++;
                            break;
                        } else if (instruction.getOpcode() == I2S && ClassNodeUtil.isInteger(instruction.getPrevious())) {
                            final short result = (short) ClassNodeUtil.getInteger(instruction.getPrevious());
                            modifier.remove(instruction.getPrevious());
                            modifier.replace(instruction, new IntInsnNode(SIPUSH, result));
                            modified = true;
                            count++;
                            break;
                        } else if (instruction.getOpcode() == I2L && ClassNodeUtil.isInteger(instruction.getPrevious())) {
                            final long result = ClassNodeUtil.getInteger(instruction.getPrevious());
                            modifier.remove(instruction.getPrevious());
                            modifier.replace(instruction, new LdcInsnNode(result));
                            modified = true;
                            count++;
                            break;
                        } else if (instruction.getOpcode() == IADD || instruction.getOpcode() == ISUB || instruction.getOpcode() == IMUL || instruction.getOpcode() == IDIV
                                || instruction.getOpcode() == IREM || instruction.getOpcode() == ISHL || instruction.getOpcode() == ISHR || instruction.getOpcode() == IUSHR
                                || instruction.getOpcode() == IAND || instruction.getOpcode() == IOR || instruction.getOpcode() == IXOR) {
                            if (ClassNodeUtil.isInteger(ClassNodeUtil.getPrevious(instruction, 1)) && ClassNodeUtil.isInteger(ClassNodeUtil.getPrevious(instruction, 2))) {
                                final Integer result = doIntegerMath(ClassNodeUtil.getInteger(ClassNodeUtil.getPrevious(instruction, 2)), ClassNodeUtil.getInteger(ClassNodeUtil.getPrevious(instruction, 1)), instruction.getOpcode());
                                if (result != null) {
                                    modifier.remove(ClassNodeUtil.getPrevious(instruction, 2));
                                    modifier.remove(ClassNodeUtil.getPrevious(instruction, 1));
                                    modifier.replace(instruction, ClassNodeUtil.getIntInsn(result));
                                    modified = true;
                                    count++;
                                    break;
                                }
                            }
                        } else if (instruction.getOpcode() == LADD || instruction.getOpcode() == LSUB || instruction.getOpcode() == LMUL || instruction.getOpcode() == LDIV
                                || instruction.getOpcode() == LREM || instruction.getOpcode() == LSHL || instruction.getOpcode() == LSHR || instruction.getOpcode() == LUSHR
                                || instruction.getOpcode() == LAND || instruction.getOpcode() == LOR || instruction.getOpcode() == LXOR) {
                            if (ClassNodeUtil.isNumber(ClassNodeUtil.getPrevious(instruction, 1)) && ClassNodeUtil.isNumber(ClassNodeUtil.getPrevious(instruction, 2))) {
                                final long value1 = Objects.requireNonNull(ClassNodeUtil.getNumber(ClassNodeUtil.getPrevious(instruction, 2))).longValue();
                                final long value2 = Objects.requireNonNull(ClassNodeUtil.getNumber(ClassNodeUtil.getPrevious(instruction, 1))).longValue();
                                final Long result = doLongMath(value1, value2, instruction.getOpcode());
                                if (result != null) {
                                    modifier.remove(ClassNodeUtil.getPrevious(instruction, 2));
                                    modifier.remove(ClassNodeUtil.getPrevious(instruction, 1));
                                    modifier.replace(instruction, new LdcInsnNode(result));
                                    modified = true;
                                    count++;
                                    break;
                                }
                            }
                        }
                    }
                    modifier.apply(method);
                } while (modified);
            }
        }
        Logger.info("Finish QProtectNumberTransformer with " + count + " arithmetics removed.");
    }

    private Integer doIntegerMath(int value1, int value2, int opcode) {
        switch (opcode) {
            case IADD:
                return value1 + value2;
            case ISUB:
                return value1 - value2;
            case IMUL:
                return value1 * value2;
            case IDIV:
                return value1 / value2;
            case IREM:
                return value1 % value2;
            case ISHL:
                return value1 << value2;
            case ISHR:
                return value1 >> value2;
            case IUSHR:
                return value1 >>> value2;
            case IAND:
                return value1 & value2;
            case IOR:
                return value1 | value2;
            case IXOR:
                return value1 ^ value2;
        }
        return null;
    }

    private Long doLongMath(long value1, long value2, int opcode) {
        switch (opcode) {
            case LADD:
                return value1 + value2;
            case LSUB:
                return value1 - value2;
            case LMUL:
                return value1 * value2;
            case LDIV:
                return value1 / value2;
            case LREM:
                return value1 % value2;
            case LSHL:
                return value1 << value2;
            case LSHR:
                return value1 >> value2;
            case LUSHR:
                return value1 >>> value2;
            case LAND:
                return value1 & value2;
            case LOR:
                return value1 | value2;
            case LXOR:
                return value1 ^ value2;
        }
        return null;
    }
}
