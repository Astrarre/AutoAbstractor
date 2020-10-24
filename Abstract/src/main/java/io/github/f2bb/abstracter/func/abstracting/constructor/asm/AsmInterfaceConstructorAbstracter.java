package io.github.f2bb.abstracter.func.abstracting.constructor.asm;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import java.lang.reflect.Constructor;

import io.github.f2bb.abstracter.func.abstracting.constructor.ConstructorAbstracter;
import io.github.f2bb.abstracter.util.asm.InvokeUtil;
import io.github.f2bb.abstracter.util.asm.TypeUtil;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class AsmInterfaceConstructorAbstracter implements ConstructorAbstracter<ClassNode> {
	@Override
	public void abstractConstructor(ClassNode header, Class<?> abstracting, Constructor<?> constructor, boolean impl) {
		String desc = TypeUtil.REMAPPER.mapSignature(Type.getConstructorDescriptor(constructor), false);
		desc = desc.substring(0, desc.length() - 1);
		MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC,
				"newInstance",
				desc + TypeUtil.getInterfaceDesc(abstracting),
				null,
				null);
		if (impl) {
			InvokeUtil.invokeConstructor(method, constructor, true);
		} else {
			InvokeUtil.visitStub(method);
		}
		header.methods.add(method);
	}
}
