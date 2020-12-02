package io.github.astrarre.abstracter.util.asm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import io.github.astrarre.Impl;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class InvokeUtil implements Opcodes {

	/**
	 * invoke the native method from a bridged method
	 */
	public static void invokeTarget(MethodNode from, String sup, Method target, int instanceInsn, boolean iface) {
		Class<?> declaring = target.getDeclaringClass();
		int access = target.getModifiers();
		if(instanceInsn != INVOKESPECIAL || Modifier.isStatic(access)) {
			sup = Type.getInternalName(declaring);
		}

		invoke(from,
				declaring.getModifiers(),
				access,
				sup,
				target.getName(),
				Type.getMethodDescriptor(target),
				instanceInsn,
				false,
				iface);
	}

	public static void invokeBridged(MethodNode from, ClassNode node, String desc) {
		invoke(from, node.access, from.access, node.name, from.name, desc, INVOKEVIRTUAL, false, false);
	}

	public static void invokeConstructor(MethodNode from, Constructor<?> constructor, boolean interfaceCtor) {
		invoke(from,
				0,
				0,
				Type.getInternalName(constructor.getDeclaringClass()),
				"<init>",
				Type.getConstructorDescriptor(constructor),
				INVOKESPECIAL,
				interfaceCtor,
				false);
	}

	/**
	 * invoke a method (api facing) invoke virtual from another method with the same parameter types
	 */
	public static void invoke(MethodNode from,
			int classAccess,
			int methodAccess,
			String owner,
			String name,
			String desc,
			int opcode,
			boolean interfaceCtor,
			boolean iface) {
		// if nativeSide is true, then cast return type
		// else cast parameters

		int index = 0;
		if (Modifier.isStatic(methodAccess)) {
			opcode = INVOKESTATIC;
		} else {
			// load `this`
			if (interfaceCtor) {
				from.visitTypeInsn(NEW, owner);
				from.visitInsn(DUP);
			} else {
				from.visitVarInsn(ALOAD, 0);
				// if interface, then cast
				if (iface) {
					from.visitTypeInsn(CHECKCAST, owner);
				}
				index++;
			}

			// auto adjust for interface
			if (opcode == INVOKEVIRTUAL && Modifier.isInterface(classAccess)) {
				opcode = INVOKEINTERFACE;
			}
		}

		Type targetType = Type.getMethodType(desc);
		Type originType = Type.getMethodType(from.desc);
		// cast parameters
		Type[] targetArgs = targetType.getArgumentTypes();
		Type[] originArgs = originType.getArgumentTypes();

		for (int i = 0; i < targetArgs.length; i++) {
			Type targetArg = targetArgs[i];
			Type originArg = originArgs[i];
			from.visitVarInsn(originArg.getOpcode(ILOAD), index);
			index+=originArg.getSize();
			// if type changed, generics or abstraction
			if (!targetArg.equals(originArg)) {
				from.visitTypeInsn(CHECKCAST, targetArg.getInternalName());
			}
		}

		from.visitMethodInsn(opcode, owner, name, desc);
		Type targetReturn = targetType.getReturnType();
		Type originReturn = originType.getReturnType();

		// cast if non-minecraft or generics
		if (interfaceCtor) {
			from.visitTypeInsn(CHECKCAST, originReturn.getInternalName());
			from.visitInsn(ARETURN);
		} else {
			if (targetReturn.getSort() == originReturn.getSort() && !targetReturn.equals(originReturn)) {
				from.visitTypeInsn(CHECKCAST, originReturn.getInternalName());
			}
			from.visitInsn(targetReturn.getOpcode(IRETURN));
		}
	}

	public static final String INTERNAL = org.objectweb.asm.Type.getInternalName(Impl.class);
	private static final String DESC = "()" + Type.getDescriptor(Object.class);
	public static void visitStub(MethodNode visitor) {
		if(!Modifier.isAbstract(visitor.access)) {
			int opcode = Type.getMethodType(visitor.desc).getReturnType().getOpcode(IRETURN);
			if(opcode != RETURN) {
				visitor.visitMethodInsn(INVOKESTATIC, INTERNAL, Impl.INIT, DESC, false);
				visitor.visitInsn(opcode);
			}

		}
	}
}
