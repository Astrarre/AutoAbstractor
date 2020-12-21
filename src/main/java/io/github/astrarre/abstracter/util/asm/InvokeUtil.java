package io.github.astrarre.abstracter.util.asm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import io.github.astrarre.abstracter.util.reflect.TypeUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class InvokeUtil implements Opcodes {
	private static final String RUNTIME_EXCEPTION = Type.getInternalName(RuntimeException.class);

	/**
	 * invoke the native method from a bridged method
	 */
	public static void invokeTarget(String abstractedName, MethodNode from, String sup, Method target, int instanceInsn, boolean iface) {
		Class<?> declaring = target.getDeclaringClass();
		int access = target.getModifiers();
		if (instanceInsn != INVOKESPECIAL || Modifier.isStatic(access)) {
			sup = Type.getInternalName(declaring);
		}

		invoke(abstractedName,
				from,
				declaring.getModifiers(),
				access,
				sup,
				target.getName(),
				Type.getMethodDescriptor(target),
				instanceInsn,
				false,
				iface);
	}

	/**
	 * invoke a method (api facing) invoke virtual from another method with the same parameter types
	 */
	public static void invoke(String abstractedName,
			MethodNode from,
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
					TypeUtil.cast(abstractedName, owner, from);
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
			index += originArg.getSize();
			// if type changed, generics or abstraction
			if (!targetArg.equals(originArg)) {
				TypeUtil.cast(originArg.getInternalName(), targetArg.getInternalName(), from);
			}
		}

		from.visitMethodInsn(opcode, owner, name, desc);
		Type targetReturn = targetType.getReturnType();
		Type originReturn = originType.getReturnType();

		// cast if non-minecraft or generics
		if (interfaceCtor) {
			TypeUtil.cast(owner, originReturn.getInternalName(), from);
			from.visitInsn(ARETURN);
		} else {
			if (targetReturn.getSort() == originReturn.getSort() && !targetReturn.equals(originReturn)) {
				TypeUtil.cast(targetReturn.getInternalName(), originReturn.getInternalName(), from);
			}
			from.visitInsn(targetReturn.getOpcode(IRETURN));
		}
	}

	public static void invokeBridged(MethodNode from, ClassNode node, String desc) {
		invoke(node.name, from, node.access, from.access, node.name, from.name, desc, INVOKEVIRTUAL, false, false);
	}

	public static void invokeConstructor(String abstractedName, MethodNode from, Constructor<?> constructor, boolean interfaceCtor) {
		invoke(abstractedName,
				from,
				0,
				0,
				Type.getInternalName(constructor.getDeclaringClass()),
				"<init>",
				Type.getConstructorDescriptor(constructor),
				INVOKESPECIAL,
				interfaceCtor,
				false);
	}

	public static void visitStub(MethodNode visitor) {
		if (!Modifier.isAbstract(visitor.access)) {
			visitor.visitTypeInsn(NEW, RUNTIME_EXCEPTION);
			visitor.visitInsn(DUP);
			visitor.visitMethodInsn(INVOKESPECIAL, RUNTIME_EXCEPTION, "<init>", "()V", false);
			visitor.visitInsn(ATHROW);
		}
	}
}
