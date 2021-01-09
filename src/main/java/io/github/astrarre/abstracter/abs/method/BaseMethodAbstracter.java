package io.github.astrarre.abstracter.abs.method;

import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import io.github.astrarre.abstracter.abs.AbstractAbstracter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class BaseMethodAbstracter extends MethodAbstracter {
	public BaseMethodAbstracter(AbstractAbstracter abstracter, Method method) {
		super(abstracter, method);
	}

	@Override
	protected void invokeTarget(MethodNode node) {
		this.invokeTarget(node, Type.getInternalName(this.abstracter.cls), this.method, INVOKEVIRTUAL, false);
	}

	@Override
	public MethodNode abstractMethod(ClassNode header, boolean impl) {
		MethodNode node = super.abstractMethod(header, impl);
		if(impl) {
			if (!Modifier.isFinal(node.access) && !Modifier.isStatic(node.access)) {
				this.visitBridge(header, this.method, node.desc);
			}
		} else {
			AbstractAbstracter.visitStub(node);
		}
		return node;
	}

	private MethodNode visitBridge(ClassNode header, Method method, String targetDesc) {
		int access = method.getModifiers();
		MethodNode node = new MethodNode((access & ~ACC_ABSTRACT) | ACC_FINAL | ACC_BRIDGE | ACC_SYNTHETIC,
				method.getName(),
				org.objectweb.asm.Type.getMethodDescriptor(method),
				null /*sign*/,
				null);

		// triangular method
		this.invokeBridged(node, header, targetDesc);
		header.methods.add(node);
		return node;
	}

	public void invokeBridged(MethodNode from, ClassNode node, String desc) {
		this.invoke(from, node.access, from.access, node.name, from.name, desc, INVOKEVIRTUAL, false, false);
	}
}
