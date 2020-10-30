package io.github.f2bb.abstracter.func.abstracting.constructor.asm;

import static io.github.f2bb.abstracter.util.ArrayUtil.map;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import java.lang.reflect.Constructor;
import java.util.function.Function;

import com.google.common.reflect.TypeToken;
import io.github.f2bb.abstracter.func.abstracting.constructor.ConstructorAbstracter;
import io.github.f2bb.abstracter.func.map.TypeMappingFunction;
import io.github.f2bb.abstracter.util.asm.InvokeUtil;
import io.github.f2bb.abstracter.util.asm.TypeUtil;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

@SuppressWarnings ("UnstableApiUsage")
public class AsmInterfaceConstructorAbstracter implements ConstructorAbstracter<ClassNode> {
	@Override
	public void abstractConstructor(ClassNode header, Class<?> abstracting, Constructor<?> constructor, boolean impl) {
		Function<java.lang.reflect.Type, TypeToken<?>> resolve = TypeMappingFunction.resolve(abstracting);
		TypeToken<?>[] params = map(constructor.getGenericParameterTypes(), resolve::apply, TypeToken[]::new);
		TypeToken<?> returnType = resolve.apply(abstracting);
		MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC,
				"newInstance",
				TypeUtil.methodDescriptor(params, returnType),
				TypeUtil.methodSignature(constructor.getTypeParameters(), params, returnType),
				null);
		if (impl) {
			InvokeUtil.invokeConstructor(method, constructor, true);
		} else {
			InvokeUtil.visitStub(method);
		}
		header.methods.add(method);
	}
}
