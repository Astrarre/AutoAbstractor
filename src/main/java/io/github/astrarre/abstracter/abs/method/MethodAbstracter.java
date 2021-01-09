package io.github.astrarre.abstracter.abs.method;

import static io.github.astrarre.abstracter.util.ArrayUtil.map;
import static org.objectweb.asm.Type.getInternalName;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.function.Function;

import com.google.common.reflect.TypeToken;
import io.github.astrarre.abstracter.abs.AbstractAbstracter;
import io.github.astrarre.abstracter.func.map.TypeMappingFunction;
import io.github.astrarre.abstracter.util.AnnotationReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public abstract class MethodAbstracter implements Opcodes {
	protected final AbstractAbstracter abstracter;
	protected final Method method;

	public MethodAbstracter(AbstractAbstracter abstracter, Method method) {
		this.abstracter = abstracter;
		this.method = method;
	}

	public MethodNode abstractMethod(ClassNode header, boolean impl) {
		Header methodHeader = this.getHeader(impl);
		String desc = methodHeader.desc;
		String sign = methodHeader.sign;
		int access = this.method.getModifiers();
		MethodNode node = new MethodNode(access, this.method.getName(), desc, sign, null);
		for (Annotation annotation : this.method.getAnnotations()) {
			if (node.visibleAnnotations == null) {
				node.visibleAnnotations = new ArrayList<>();
			}
			node.visibleAnnotations.add(AnnotationReader.accept(annotation));
		}

		if (impl) {
			// if abstract, and base then we don't invoke target, otherwise we're fine
			if (!Modifier.isAbstract(access)) {
				// invoke target
				this.invokeTarget(node);
			}
		} else if (!Modifier.isAbstract(node.access)) {
			AbstractAbstracter.visitStub(node);
		}

		for (Parameter parameter : this.method.getParameters()) {
			node.visitParameter(parameter.getName(), 0);
		}
		header.methods.add(node);
		return node;
	}

	// todo add Abstract for interface methods
	public Header getHeader(boolean impl) {
		Function<Type, TypeToken<?>> resolve = TypeMappingFunction.resolve(this.abstracter.cls);
		TypeToken<?>[] params = map(this.method.getGenericParameterTypes(), resolve, TypeToken[]::new);
		TypeToken<?> returnType = resolve.apply(this.method.getGenericReturnType());
		String desc = AbstractAbstracter.methodDescriptor(params, returnType);
		String sign = impl ? null : AbstractAbstracter.methodSignature(this.method.getTypeParameters(), params, returnType);
		int access = this.method.getModifiers();
		return new Header(access, this.method.getName(), desc, sign);
	}

	protected abstract void invokeTarget(MethodNode node);

	/**
	 * invoke a method (api facing) invoke virtual from another method with the same parameter types
	 */
	public void invoke(MethodNode from,
			int classAccess,
			int methodAccess,
			String owner,
			String name,
			String desc,
			int opcode,
			boolean interfaceCtor,
			boolean iface) {
		int index = 0;
		if (Modifier.isStatic(methodAccess)) {
			opcode = INVOKESTATIC;
		} else {
			// load `this`
			if (interfaceCtor) {
				from.visitTypeInsn(NEW, owner);
				from.visitInsn(DUP);
			} else {
				from.visitVarInsn(ALOAD, 0);
				// if interface, then cast
				if (iface) {
					cast(this.name, owner, from);
				}
				index++;
			}

			// auto adjust for interface
			if (opcode == INVOKEVIRTUAL && Modifier.isInterface(classAccess)) {
				opcode = INVOKEINTERFACE;
			}
		}

		org.objectweb.asm.Type targetType = org.objectweb.asm.Type.getMethodType(desc);
		org.objectweb.asm.Type originType = org.objectweb.asm.Type.getMethodType(from.desc);
		// cast parameters
		org.objectweb.asm.Type[] targetArgs = targetType.getArgumentTypes();
		org.objectweb.asm.Type[] originArgs = originType.getArgumentTypes();

		for (int i = 0; i < targetArgs.length; i++) {
			org.objectweb.asm.Type targetArg = targetArgs[i];
			org.objectweb.asm.Type originArg = originArgs[i];
			from.visitVarInsn(originArg.getOpcode(ILOAD), index);
			index += originArg.getSize();
			// if type changed, generics or abstraction
			if (!targetArg.equals(originArg)) {
				// todo only cast when necessary
				cast(originArg.getInternalName(), targetArg.getInternalName(), from);
			}
		}

		from.visitMethodInsn(opcode, owner, name, desc);
		org.objectweb.asm.Type originReturn = originType.getReturnType();

		// cast if non-minecraft or generics
		if (interfaceCtor) {
			cast(owner, originReturn.getInternalName(), from);
			from.visitInsn(ARETURN);
		} else {
			org.objectweb.asm.Type targetReturn = targetType.getReturnType();
			if (targetReturn.getSort() == originReturn.getSort() && !targetReturn.equals(originReturn)) {
				cast(targetReturn.getInternalName(), originReturn.getInternalName(), from);
			}
			from.visitInsn(targetReturn.getOpcode(IRETURN));
		}
	}

	/**
	 * invoke the native method from a bridged method
	 */
	public void invokeTarget(MethodNode from, String targetClass, Method targetMethod, int instanceInsn, boolean iface) {
		Class<?> declaring = targetMethod.getDeclaringClass();
		int access = targetMethod.getModifiers();
		if (instanceInsn != INVOKESPECIAL || Modifier.isStatic(access)) {
			targetClass = getInternalName(declaring);
		}

		this.invoke(from,
				declaring.getModifiers(),
				access,
				targetClass,
				targetMethod.getName(),
				org.objectweb.asm.Type.getMethodDescriptor(targetMethod),
				instanceInsn,
				false,
				iface);
	}

	public static final class Header {
		public int access;
		public String name, desc, sign;

		public Header(int access, String name, String desc, String sign) {
			this.access = access;
			this.name = name;
			this.desc = desc;
			this.sign = sign;
		}
	}
}
