package io.github.astrarre.abstracter.abs.method;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import io.github.astrarre.abstracter.util.AbstracterLoader;
import io.github.astrarre.abstracter.util.reflect.TypeUtil;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class MethodAbstracter implements Opcodes {
	/**
	 * if `this` is a super class of the target class (like in interface ctors)
	 */
	protected boolean castThis;
	protected String thisName;
	protected int targetClassAccess, targetMethodAccess;
	protected boolean isSpecial;
	protected String owner, name, desc;

	public MethodAbstracter(ClassNode from, Method target, boolean isSpecial) {
		Class<?> declaring = target.getDeclaringClass();
		// todo reify
		this.castThis = !declaring.isAssignableFrom(AbstracterLoader.getClass(from.superName.replace('.', '/')));
		this.isSpecial = isSpecial;
		this.targetClassAccess = declaring.getModifiers();
		this.targetMethodAccess = target.getModifiers();
		this.owner = Type.getInternalName(declaring);
		this.desc = Type.getMethodDescriptor(target);
	}

	protected MethodAbstracter(boolean castThis,
			String thisName,
			int targetClassAccess,
			int targetMethodAccess,
			boolean isSpecial,
			String owner,
			String name,
			String desc) {
		this.castThis = castThis;
		this.thisName = thisName;
		this.targetClassAccess = targetClassAccess;
		this.targetMethodAccess = targetMethodAccess;
		this.isSpecial = isSpecial;
		this.owner = owner;
		this.name = name;
		this.desc = desc;
	}

	public MethodNode create() {

	}

	protected int getAccess() {
		// in interface you need to remove final
		return this.targetMethodAccess;
	}

	public void accept(MethodNode visitor) {
		int index = 0;
		int opcode = this.getOpcode();
		if (!Modifier.isStatic(this.targetMethodAccess)) {
			this.acceptInstance(visitor);
		}

		Type targetType = Type.getMethodType(this.desc);
		Type originType = Type.getMethodType(visitor.desc);
		// cast parameters
		Type[] targetArgs = targetType.getArgumentTypes();
		Type[] originArgs = originType.getArgumentTypes();

		for (int i = 0; i < targetArgs.length; i++) {
			Type targetArg = targetArgs[i];
			Type originArg = originArgs[i];
			visitor.visitVarInsn(originArg.getOpcode(ILOAD), index);
			index += originArg.getSize();
			// if type changed, generics or abstraction
			if (!targetArg.equals(originArg)) {
				TypeUtil.cast(originArg.getInternalName(), targetArg.getInternalName(), visitor);
			}
		}

		visitor.visitMethodInsn(opcode, this.owner, this.name, this.desc);
		Type targetReturn = targetType.getReturnType();
		Type originReturn = originType.getReturnType();
		this.acceptReturn(visitor, targetReturn, originReturn);
	}

	protected void acceptReturn(MethodNode node, Type targetReturn, Type originReturn) {
		// cast if non-minecraft or generics
		if (targetReturn.getSort() == originReturn.getSort() && !targetReturn.equals(originReturn)) {
			TypeUtil.cast(targetReturn.getInternalName(), originReturn.getInternalName(), node);
		}
		node.visitInsn(targetReturn.getOpcode(IRETURN));
	}

	protected void acceptInstance(MethodNode node) {
		// load `this`
		node.visitVarInsn(ALOAD, 0);
		// if interface, then cast
		if (this.castThis) {
			// todo nuke this with the force of a thousand suns
			TypeUtil.cast(this.thisName, this.owner, node);
		}
	}

	protected int getOpcode() {
		int opcode;
		if (Modifier.isStatic(this.targetMethodAccess)) {
			opcode = INVOKESTATIC;
		} else {
			// load `this`
			if(this.isSpecial) {
				opcode = INVOKESPECIAL;
			} else if(Modifier.isInterface(this.targetClassAccess)) {
				opcode = INVOKEINTERFACE;
			} else {
				opcode = INVOKEVIRTUAL;
			}
		}
		return opcode;
	}
}
