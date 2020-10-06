package io.github.f2bb.util;

import java.lang.reflect.Modifier;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class AsmUtil implements Opcodes {
	public static String prefixName(String prefix, String name) {
		int i = name.lastIndexOf('/') + 1;
		return name.substring(0, i) + prefix + name.substring(i);
	}

	public interface VisitMethod {
		MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions);
	}

	public static FieldVisitor generateGetter(VisitMethod supply, String owner, int access, String name, String descriptor, String signature) {
		MethodVisitor visitor = supply.visitMethod(access, "get" + name(descriptor) + Character.toUpperCase(name.charAt(0)) + name.substring(1), "()" + descriptor, "()" + signature, null);
		if (Modifier.isStatic(access)) {
			visitor.visitFieldInsn(GETSTATIC, owner, name, descriptor);
		} else {
			visitor.visitVarInsn(ALOAD, 0);
			visitor.visitFieldInsn(GETFIELD, owner, name, descriptor);
		}
		visitor.visitInsn(Type.getType(descriptor).getOpcode(IRETURN));
		return new FieldVisitor(ASM9) {
			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				return visitor.visitAnnotation(descriptor, visible);
			}
		};
	}

	private static String name(String desc) {
		Type type = Type.getType(desc);
		switch (type.getSort()) {
		case Type.ARRAY:
			return "Arr";
		case Type.OBJECT:
			return "Obj";
		case Type.BYTE:
			return "Byte";
		case Type.BOOLEAN:
			return "Bool";
		case Type.SHORT:
			return "Short";
		case Type.CHAR:
			return "Char";
		case Type.INT:
			return "Int";
		case Type.FLOAT:
			return "Float";
		case Type.LONG:
			return "Long";
		case Type.DOUBLE:
			return "Double";
		}
		throw new IllegalArgumentException(desc);
	}
}
