package io.github.f2bb.abstracter.func.abstracting;

import static io.github.f2bb.abstracter.Abstracter.map;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.TypeVariable;
import java.util.function.Function;

import com.google.common.reflect.TypeToken;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import io.github.f2bb.abstracter.ex.ImplementationHiddenException;
import io.github.f2bb.abstracter.func.map.TypeMappingFunction;
import io.github.f2bb.abstracter.impl.AsmAbstracter;
import io.github.f2bb.abstracter.impl.JavaAbstracter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

@SuppressWarnings ("UnstableApiUsage")
public class MethodAbstraction implements Opcodes {
	public static void visitJava(TypeSpec.Builder header, Class<?> declaring, Method method) {
		TypeMappingFunction reifier = TypeMappingFunction.reify(declaring);
		java.lang.reflect.Type returnType = reifier.map(method.getGenericReturnType());

		MethodSpec.Builder builder = MethodSpec.methodBuilder(method.getName());
		for (Parameter parameter : method.getParameters()) {
			builder.addParameter(JavaAbstracter.toTypeName(reifier.map(parameter.getParameterizedType())), parameter.getName());
		}
		builder.returns(JavaAbstracter.toTypeName(returnType));
		for (TypeVariable<Method> parameter : method.getTypeParameters()) {
			builder.addTypeVariable((TypeVariableName) JavaAbstracter.toTypeName(reifier.map(parameter)));
		}
		builder.addStatement("throw $T.create()", ImplementationHiddenException.class);
		header.addMethod(builder.build());
	}

	public static void visitBridged(ClassNode header, Class<?> declaring, Method method, boolean impl,
			boolean base) {
		Function<java.lang.reflect.Type, TypeToken<?>> resolve = TypeMappingFunction.resolve(declaring);
		TypeToken<?>[] params = map(method.getGenericParameterTypes(), resolve, TypeToken[]::new);
		TypeToken<?> returnType = resolve.apply(method.getGenericReturnType());
		String desc = AsmAbstracter.methodDescriptor(params, returnType);
		String sign = impl ? null : AsmAbstracter.methodSignature(method.getTypeParameters(), params, returnType);
		int access = method.getModifiers();
		MethodNode node = new MethodNode(access, method.getName(), desc, sign, null);
		boolean identical = desc.equals(Type.getMethodDescriptor(method));
		if (!Modifier.isAbstract(access)) {
			if (!impl) {
				AsmAbstracter.visitStub(node);
				header.methods.add(node);
			} else if (!identical) {
				// triangular method
				AsmAbstracter.invoke(node, method, base);
				if (base) {
					visitBridge(header, method, desc);
				}
				header.methods.add(node);
			}
		}
	}

	private static void visitBridge(ClassNode header, Method method, String target) {
		int access = method.getModifiers();
		MethodNode node = new MethodNode(access | ACC_FINAL,
				method.getName(),
				Type.getMethodDescriptor(method),
				null /*sign*/,
				null);
		if (!Modifier.isAbstract(access)) {
			// triangular method
			AsmAbstracter.invoke(node,
					method.getModifiers(),
					header.name,
					method.getName(),
					target,
					INVOKEVIRTUAL,
					false);
		}
		header.methods.add(node);
	}
}
