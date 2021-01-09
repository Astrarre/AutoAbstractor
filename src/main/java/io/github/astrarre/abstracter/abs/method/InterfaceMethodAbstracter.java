package io.github.astrarre.abstracter.abs.method;

import java.lang.reflect.Method;

import io.github.astrarre.abstracter.abs.AbstractAbstracter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

public class InterfaceMethodAbstracter extends MethodAbstracter {

	public InterfaceMethodAbstracter(AbstractAbstracter abstracter, Method method) {
		super(abstracter, method);
	}

	@Override
	public Header getHeader(boolean impl) {
		Header header = super.getHeader(impl);
		if (impl) {
			header.access &= ~ACC_ABSTRACT;
		} else {
			header.access |= ACC_ABSTRACT;
		}

		return header;
	}

	@Override
	protected void invokeTarget(MethodNode node) {
		this.abstracter.invokeTarget(node, Type.getInternalName(this.method.getDeclaringClass()), this.method, Opcodes.INVOKEVIRTUAL, true);
	}
}
