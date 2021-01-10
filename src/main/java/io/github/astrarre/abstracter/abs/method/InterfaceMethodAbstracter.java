package io.github.astrarre.abstracter.abs.method;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import io.github.astrarre.abstracter.abs.AbstractAbstracter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

public class InterfaceMethodAbstracter extends MethodAbstracter<Method> {
	public InterfaceMethodAbstracter(AbstractAbstracter abstracter, Method method, boolean impl) {
		super(abstracter, method, impl);
	}

	@Override
	public Header getHeader() {
		Header header = super.getHeader();
		header.access &= ~ACC_FINAL;
		if (this.impl) {
			header.access &= ~ACC_ABSTRACT;
		} else if (!Modifier.isStatic(header.access)) {
			header.access |= ACC_ABSTRACT;
		}

		return header;
	}

	@Override
	protected void invokeTarget(MethodNode node) {
		this.invoke(node,
				Type.getInternalName(this.method.getDeclaringClass()),
				this.method.getName(),
				Type.getMethodDescriptor(this.method),
				this.getOpcode(this.method, INVOKEVIRTUAL));
	}
}
