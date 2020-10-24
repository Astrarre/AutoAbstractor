package io.github.f2bb.abstracter.func.abstracting.constructor.asm;

import java.lang.reflect.Constructor;

import io.github.f2bb.abstracter.func.abstracting.constructor.ConstructorAbstracter;
import io.github.f2bb.abstracter.util.asm.InvokeUtil;
import io.github.f2bb.abstracter.util.asm.TypeUtil;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class AsmBaseConstructorAbstracter implements ConstructorAbstracter<ClassNode> {
	@Override
	public void abstractConstructor(ClassNode header, Class<?> abstracting, Constructor<?> constructor, boolean impl) {
		String desc = Type.getConstructorDescriptor(constructor);
		MethodNode method = new MethodNode(constructor.getModifiers(),
				"<init>",
				TypeUtil.REMAPPER.mapSignature(desc, false),
				null,
				null);
		if (impl) {
			InvokeUtil.invokeConstructor(method, constructor, false);
		} else {
			InvokeUtil.visitStub(method);
		}
		header.methods.add(method);
	}
}
