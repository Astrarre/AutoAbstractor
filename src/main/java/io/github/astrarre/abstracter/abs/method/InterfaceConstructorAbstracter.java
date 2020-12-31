package io.github.astrarre.abstracter.abs.method;

import java.lang.reflect.Constructor;

import io.github.astrarre.abstracter.util.reflect.TypeUtil;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

public class InterfaceConstructorAbstracter extends MethodAbstracter {
	protected InterfaceConstructorAbstracter(Constructor<?> constructor) {
		super(false, null, 0, 0, true, Type.getInternalName(constructor.getDeclaringClass()), "<init>", Type.getConstructorDescriptor(constructor));
	}

	@Override
	protected void acceptInstance(MethodNode node) {
		node.visitTypeInsn(NEW, this.owner);
		node.visitInsn(DUP);
	}

	@Override
	protected void acceptReturn(MethodNode node, Type targetReturn, Type originReturn) {
		TypeUtil.cast(this.owner, originReturn.getInternalName(), node);
		node.visitInsn(ARETURN);
	}
}
