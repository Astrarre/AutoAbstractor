package io.github.f2bb.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.function.Function;
import java.util.logging.Logger;

import com.google.common.reflect.TypeToken;
import io.github.f2bb.ex.ImplementationHiddenException;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class AsmUtil implements Opcodes {
	public static final String OBJECT_NAME = Type.getInternalName(Object.class);
	public static final String OBJECT_DESC = Type.getDescriptor(Object.class);
	private static final Logger LOGGER = Logger.getLogger("AsmUtil");

	public static Iterable<String> splitName(String name) {
		return () -> new Iterator<String>() {
			private String next = name;

			@Override
			public boolean hasNext() {
				return this.next != null;
			}

			@Override
			public String next() {
				String orig = this.next;
				int i = orig.lastIndexOf('$');
				if (i < 0) {
					this.next = null;
				} else {
					this.next = orig.substring(0, i);
				}
				return orig;
			}
		};
	}

	public static String prefixName(String prefix, String name) {
		int i = name.lastIndexOf('/') + 1;
		return name.substring(0, i) + prefix + name.substring(i);
	}

	public interface VisitMethod {
		MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions);
	}

	public static MethodVisitor visitStub(MethodVisitor visitor) {
		visitor.visitMethodInsn(INVOKESTATIC, ImplementationHiddenException.INTERNAL, "create", "()L" + ImplementationHiddenException.INTERNAL + ';', false);
		visitor.visitInsn(ATHROW);
		return visitor;
	}

	public static MethodVisitor generateGetter(VisitMethod supply, Function<java.lang.reflect.Type, String> sign, TypeToken<?> token, Field field, boolean impl) {
		int access = field.getModifiers();
		String owner = Type.getInternalName(field.getType());
		String descriptor = Type.getDescriptor(token.getRawType());
		String name = field.getName();
		String signature = sign.apply(token.getType());

		MethodVisitor visitor = supply.visitMethod(access | ACC_FINAL, getEtterName("get", descriptor, name), "()" + descriptor, signature == null ? null : "()" + signature, null);
		if(impl) {
			if (Modifier.isStatic(access)) {
				visitor.visitFieldInsn(GETSTATIC, owner, name, descriptor);
			} else {
				visitor.visitVarInsn(ALOAD, 0);
				visitor.visitFieldInsn(GETFIELD, owner, name, descriptor);
			}
			visitor.visitInsn(Type.getType(descriptor).getOpcode(IRETURN));
		} else {
			visitStub(visitor);
		}

		return visitor;
	}

	public static String getEtterName(String prefix, String desc, String name) {
		return prefix + name(desc) + Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	public static MethodVisitor generateSetter(VisitMethod supply, Function<java.lang.reflect.Type, String> sign, TypeToken<?> token, Field field, boolean impl) {
		int access = field.getModifiers();
		String owner = Type.getInternalName(field.getType());
		String descriptor = Type.getDescriptor(token.getRawType());
		String name = field.getName();
		String signature = sign.apply(token.getType());

		MethodVisitor visitor = supply.visitMethod(access | ACC_FINAL, getEtterName("set", descriptor, name), "(" + descriptor + ")V", signature == null ? null : "(" + signature + ")V", null);
		Type type = Type.getType(descriptor);
		if(impl) {
			if (Modifier.isStatic(access)) {
				visitor.visitVarInsn(type.getOpcode(ILOAD), 0);
				visitor.visitFieldInsn(PUTSTATIC, owner, name, descriptor);
			} else {
				visitor.visitVarInsn(ALOAD, 0);
				visitor.visitVarInsn(type.getOpcode(ILOAD), 1);
				visitor.visitFieldInsn(PUTFIELD, owner, name, descriptor);
			}
		} else {
			visitStub(visitor);
		}
		visitor.visitInsn(RETURN);
		visitor.visitParameter(name, ACC_FINAL);
		return visitor;
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