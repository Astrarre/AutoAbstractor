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
	protected void invokeTarget(MethodNode node) {
		Class<?> target;
		if (Modifier.isStatic(this.method.getModifiers())) {
			target = this.method.getDeclaringClass();
		} else {
			target = this.abstracter.cls;
		}

		this.invoke(node,
				Type.getInternalName(target),
				this.method.getName(),
				"<init>",
				this.getOpcode(this.method, INVOKESPECIAL));
	}
}
