package io.github.astrarre.abstracter.util.asm;

import static io.github.astrarre.abstracter.util.ArrayUtil.map;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.function.Function;

import com.google.common.reflect.TypeToken;
import io.github.astrarre.abstracter.ConflictingDefault;
import io.github.astrarre.abstracter.func.map.TypeMappingFunction;
import io.github.astrarre.abstracter.util.AnnotationReader;
import io.github.astrarre.abstracter.util.reflect.TypeUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class MethodUtil implements Opcodes {
	private static final String CONFLICTING_DEFAULT = org.objectweb.asm.Type.getDescriptor(ConflictingDefault.class);

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

	public static void abstractMethod(ClassNode header, Class<?> abstracting, Method method, boolean impl, boolean iface) {
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
		for (Annotation annotation : method.getAnnotations()) {
			if (node.visibleAnnotations == null) {
				node.visibleAnnotations = new ArrayList<>();
			}
			node.visibleAnnotations.add(AnnotationReader.accept(annotation));
		}


		if (impl) {
			boolean equalDesc = desc.equals(org.objectweb.asm.Type.getMethodDescriptor(method));
			if (desc.equals(org.objectweb.asm.Type.getMethodDescriptor(method)) && method.isDefault()) {
				node.visitAnnotation(CONFLICTING_DEFAULT, false);
			}

			// if abstract, and base then we don't invoke target, otherwise we're fine
			if (!Modifier.isAbstract(access) || iface) {
				node.access &= ~ACC_ABSTRACT;
				// invoke target
				InvokeUtil.invokeTarget(header.name, node, header.superName, method, iface ? INVOKEVIRTUAL : INVOKESPECIAL, iface);
			}

			if (equalDesc && Modifier.isFinal(access)) {
				node.visitAnnotation(CONFLICTING_DEFAULT, false);
			}

			// if base, non-final and non-static we need a bridge method to complete the triangle method
			if (!iface && !Modifier.isFinal(access) && !Modifier.isStatic(access)) {
				MethodNode n = visitBridge(header, method, desc);
				if (equalDesc) {
					n.visitAnnotation(CONFLICTING_DEFAULT, false);
				}
			}
		} else {
			// if no implementation is needed, no implementation is needed
			if (iface && !Modifier.isStatic(node.access)) {
				node.access |= ACC_ABSTRACT;
			} else {
				InvokeUtil.visitStub(node);
			}
		}

		for (Parameter parameter : method.getParameters()) {
			node.visitParameter(parameter.getName(), 0);
		}
		header.methods.add(node);
	}

	private static MethodNode visitBridge(ClassNode header, Method method, String targetDesc) {
		int access = method.getModifiers();
		MethodNode node = new MethodNode((access & ~ACC_ABSTRACT) | ACC_FINAL | ACC_BRIDGE | ACC_SYNTHETIC,
				method.getName(),
				org.objectweb.asm.Type.getMethodDescriptor(method),
				null /*sign*/,
				null);

		// triangular method
		InvokeUtil.invokeBridged(node, header, targetDesc);
		header.methods.add(node);
		return node;
	}
}
