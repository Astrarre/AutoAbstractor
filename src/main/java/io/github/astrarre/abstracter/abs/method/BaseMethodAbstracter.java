package io.github.astrarre.abstracter.abs.method;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import io.github.astrarre.abstracter.abs.AbstractAbstracter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class BaseMethodAbstracter extends MethodAbstracter<Method> {

	public BaseMethodAbstracter(AbstractAbstracter abstracter, Method method, boolean impl) {
		super(abstracter, method, impl);
	}

	@Override
	public MethodNode abstractMethod(ClassNode header) {
		MethodNode node = super.abstractMethod(header);
		if (this.impl) {
			int access = this.method.getModifiers();
			if (!Modifier.isFinal(access) && !Modifier.isStatic(access)) {
				this.visitBridge(header, this.method, node.desc);
			}
		} else {
			AbstractAbstracter.visitStub(node);
		}
		return node;
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
				Type.getMethodDescriptor(this.method),
				this.getOpcode(this.method, INVOKESPECIAL));
	}

	private MethodNode visitBridge(ClassNode header, Method method, String targetDesc) {
		int access = method.getModifiers();
		MethodNode node = new MethodNode((access & ~ACC_ABSTRACT) | ACC_FINAL | ACC_BRIDGE | ACC_SYNTHETIC,
				method.getName(),
				org.objectweb.asm.Type.getMethodDescriptor(method),
				null /*sign*/,
				null);

		// triangular method
		this.invoke(node, header.name, node.name, targetDesc, this.getOpcode(this.method, INVOKEVIRTUAL));
		header.methods.add(node);
		return node;
	}

}
