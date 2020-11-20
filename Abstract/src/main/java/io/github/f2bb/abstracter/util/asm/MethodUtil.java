package io.github.f2bb.abstracter.util.asm;

import static io.github.f2bb.abstracter.util.ArrayUtil.map;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.function.Function;

import com.google.common.reflect.TypeToken;
import io.github.f2bb.abstracter.func.map.TypeMappingFunction;
import io.github.f2bb.abstracter.util.AnnotationReader;
import io.github.f2bb.abstracter.util.reflect.TypeUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class MethodUtil {
	public static MethodNode findOrCreateMethod(int access, ClassNode node, String name, String desc) {
		for (MethodNode method : node.methods) {
			if (name.equals(method.name) && desc.equals(method.desc)) {
				return method;
			}
		}
		MethodNode method = new MethodNode(access, name, desc, null, null);
		node.methods.add(method);
		return method;
	}

	public static boolean conflicts(String name, String desc, ClassNode node) {
		for (MethodNode method : node.methods) {
			if (name.equals(method.name) && desc.equals(method.desc)) {
				return true;
			}
		}
		return false;
	}

	public static void abstractMethod(ClassNode header,
			Class<?> abstracting,
			Method method,
			boolean impl,
			boolean iface) {
		Function<Type, TypeToken<?>> resolve = TypeMappingFunction.resolve(abstracting);
		TypeToken<?>[] params = map(method.getGenericParameterTypes(), resolve, TypeToken[]::new);
		TypeToken<?> returnType = resolve.apply(method.getGenericReturnType());
		String desc = TypeUtil.methodDescriptor(params, returnType);
		String sign = impl ? null : TypeUtil.methodSignature(method.getTypeParameters(), params, returnType);
		int access = method.getModifiers();
		if (Modifier.isInterface(header.access)) {
			access &= ~Opcodes.ACC_FINAL;
		}

		MethodNode node = new MethodNode(access, method.getName(), desc, sign, null);
		for (Annotation annotation : method.getAnnotations()) {
			if(node.visibleAnnotations == null) node.visibleAnnotations = new ArrayList<>();
			node.visibleAnnotations.add(AnnotationReader.accept(annotation));
		}
		boolean identical = desc.equals(org.objectweb.asm.Type
				                                .getMethodDescriptor(method)) && !Modifier.isStatic(access);
		if (identical && impl && !iface) {
			// if instance, identical and impl, then virtual lookups go brr
		} else {
			if (!Modifier.isAbstract(access)) {
				if (impl) {
					// triangular method
					InvokeUtil
							.invokeTarget(node, header.superName, method, iface ? Opcodes.INVOKEVIRTUAL : Opcodes.INVOKESPECIAL,
									iface);
					if (!iface && !Modifier.isFinal(access)) {
						visitBridge(header, method, desc);
					}
				} else {
					if(iface && !Modifier.isStatic(node.access)) {
						node.access |= Opcodes.ACC_ABSTRACT;
					} else {
						InvokeUtil.visitStub(node);
					}
				}
			}

			for (Parameter parameter : method.getParameters()) {
				node.visitParameter(parameter.getName(), 0);
			}
			header.methods.add(node);
		}
	}

	private static void visitBridge(ClassNode header, Method method, String targetDesc) {
		int access = method.getModifiers();
		MethodNode node = new MethodNode((access & ~Opcodes.ACC_ABSTRACT) | Opcodes.ACC_FINAL,
				method.getName(),
				org.objectweb.asm.Type.getMethodDescriptor(method),
				null /*sign*/,
				null);

		// triangular method
		InvokeUtil.invokeBridged(node, header, targetDesc);
		header.methods.add(node);
	}
}
