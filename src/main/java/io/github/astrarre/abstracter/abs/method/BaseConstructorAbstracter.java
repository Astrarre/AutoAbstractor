package io.github.astrarre.abstracter.abs.method;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import io.github.astrarre.abstracter.abs.AbstractAbstracter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

// todo write bridges as intermediary
// todo then remove super class
// todo then add the super class at runtime
// todo this way TR does not accidentally remap the bridge and crash with duplicates
public class BaseConstructorAbstracter extends MethodAbstracter<Constructor<?>> {
	public BaseConstructorAbstracter(AbstractAbstracter abstracter, Constructor<?> method, boolean impl) {
		super(abstracter, method, impl);
	}


	@Override
	public Header getHeader() {
		Header header = super.getHeader();
		header.name = "<init>";
		return header;
	}
	@Override
	protected void invokeTarget(MethodNode node) {
		Class<?> target;
		if (Modifier.isStatic(this.member.getModifiers())) {
			target = this.member.getDeclaringClass();
		} else {
			target = this.abstracter.cls;
		}

		this.invoke(node,
				Type.getInternalName(target),
				"<init>",
				Type.getConstructorDescriptor(this.member),
				this.getOpcode(this.member, INVOKESPECIAL));
	}
}
