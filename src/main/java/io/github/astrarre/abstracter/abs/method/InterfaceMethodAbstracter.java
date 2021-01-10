package io.github.astrarre.abstracter.abs.method;

import java.lang.reflect.Method;

import io.github.astrarre.abstracter.abs.AbstractAbstracter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

public class InterfaceMethodAbstracter extends MethodAbstracter {


	public InterfaceMethodAbstracter(AbstractAbstracter abstracter, Method method, boolean impl) {
		super(abstracter, method, impl);
	}

	@Override
	public Header getHeader() {
		Header header = super.getHeader();
		if (impl) {
			header.access &= ~ACC_ABSTRACT;
		} else {
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
