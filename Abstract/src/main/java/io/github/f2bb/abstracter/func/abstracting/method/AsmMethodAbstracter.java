package io.github.f2bb.abstracter.func.abstracting.method;

import static io.github.f2bb.abstracter.util.ArrayUtil.map;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.function.Function;

import com.google.common.reflect.TypeToken;
import io.github.f2bb.abstracter.func.map.TypeMappingFunction;
import io.github.f2bb.abstracter.util.asm.InvokeUtil;
import io.github.f2bb.abstracter.util.asm.TypeUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

@SuppressWarnings ("UnstableApiUsage")
public class AsmMethodAbstracter implements MethodAbstracter<ClassNode>, Opcodes {
	private final boolean iface;

	public AsmMethodAbstracter(boolean iface) {this.iface = iface;}

	@Override
	public void abstractMethod(ClassNode header, Class<?> abstracting, Method method, boolean impl) {
		Function<Type, TypeToken<?>> resolve = TypeMappingFunction.resolve(abstracting);
		TypeToken<?>[] params = map(method.getGenericParameterTypes(), resolve, TypeToken[]::new);
		TypeToken<?> returnType = resolve.apply(method.getGenericReturnType());
		String desc = TypeUtil.methodDescriptor(params, returnType);
		String sign = impl ? null : TypeUtil.methodSignature(method.getTypeParameters(), params, returnType);
		int access = method.getModifiers();
		if (Modifier.isInterface(header.access)) {
			access &= ~ACC_FINAL;
		}

		MethodNode node = new MethodNode(access, method.getName(), desc, sign, null);
		boolean identical = desc.equals(org.objectweb.asm.Type.getMethodDescriptor(method)) && !Modifier.isStatic(access);
		if (identical && impl) {
			// if instance, identical and impl, then virtual lookups go brr
		} else {
			if (!Modifier.isAbstract(access)) {
				if (impl) {
					// triangular method
					InvokeUtil.invokeTarget(node, header.superName, method, this.iface ? INVOKEVIRTUAL : INVOKESPECIAL,
							this.iface);
					if (!this.iface && !Modifier.isFinal(access)) {
						visitBridge(header, method, desc);
					}
				} else {
					InvokeUtil.visitStub(node);
				}
			}

			header.methods.add(node);
		}
	}

	private static void visitBridge(ClassNode header, Method method, String targetDesc) {
		int access = method.getModifiers();
		MethodNode node = new MethodNode((access & ~ACC_ABSTRACT) | ACC_FINAL,
				method.getName(),
				org.objectweb.asm.Type.getMethodDescriptor(method),
				null /*sign*/,
				null);

		// triangular method
		InvokeUtil.invokeBridged(node, header, targetDesc);
		header.methods.add(node);
	}
}
