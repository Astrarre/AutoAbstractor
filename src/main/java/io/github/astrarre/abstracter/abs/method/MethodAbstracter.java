package io.github.astrarre.abstracter.abs.method;

import static io.github.astrarre.abstracter.util.ArrayUtil.map;
import static org.objectweb.asm.Type.ARRAY;
import static org.objectweb.asm.Type.OBJECT;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.reflect.TypeToken;
import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.abs.AbstractAbstracter;
import io.github.astrarre.abstracter.func.map.TypeMappingFunction;
import io.github.astrarre.abstracter.util.AnnotationReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public abstract class MethodAbstracter implements Opcodes {
	protected final AbstractAbstracter abstracter;
	protected final Method method;
	protected final boolean impl;

	public MethodAbstracter(AbstractAbstracter abstracter, Method method, boolean impl) {
		this.abstracter = abstracter;
		this.method = method;
		this.impl = impl;
	}

	public MethodNode abstractMethod(ClassNode header) {
		Header methodHeader = this.getHeader();
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

		if (this.impl) {
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
	public Header getHeader() {
		Function<Type, TypeToken<?>> resolve = TypeMappingFunction.resolve(this.abstracter.cls);
		TypeToken<?>[] params = map(this.method.getGenericParameterTypes(), resolve, TypeToken[]::new);
		TypeToken<?> returnType = resolve.apply(this.method.getGenericReturnType());
		String desc = AbstractAbstracter.methodDescriptor(params, returnType);
		String sign = this.impl ? null : AbstractAbstracter.methodSignature(this.method.getTypeParameters(), params, returnType);
		if(desc.equals(sign)) sign = null;
		int access = this.method.getModifiers();
		return new Header(access, this.method.getName(), desc, sign);
	}

	protected abstract void invokeTarget(MethodNode node);

	public int getOpcode(Method target, int insn) {
		int methodAccess = target.getModifiers();
		if (Modifier.isStatic(methodAccess)) {
			return INVOKESTATIC;
		} else if (insn == INVOKEVIRTUAL) {
			Class<?> cls = target.getDeclaringClass();
			if (cls.isInterface()) {
				return INVOKEINTERFACE;
			} else {
				return insn;
			}
		} else {
			return insn;
		}
	}

	/**
	 * invoke a method (api facing) invoke virtual from another method with the same parameter types
	 */
	public void invoke(MethodNode from, String owner, String name, String desc, int opcode) {
		int index = 0;
		if (!Modifier.isStatic(from.access)) {
			index = this.loadThis(from);
		}

		org.objectweb.asm.Type targetType = org.objectweb.asm.Type.getMethodType(desc);
		org.objectweb.asm.Type originType = org.objectweb.asm.Type.getMethodType(from.desc);
		// cast parameters
		org.objectweb.asm.Type[] targetArgs = targetType.getArgumentTypes();
		org.objectweb.asm.Type[] originArgs = originType.getArgumentTypes();
		for (int i = 0; i < targetArgs.length; i++) {
			org.objectweb.asm.Type targetArg = targetArgs[i];
			org.objectweb.asm.Type originArg = originArgs[i];
			int finalIndex = index;
			this.cast(AbstractAbstracter.Location.PARAMETER, originArg, targetArg, from, v -> v.visitVarInsn(originArg.getOpcode(ILOAD),
					finalIndex));
			index += originArg.getSize();
		}

		from.visitMethodInsn(opcode, owner, name, desc);
		org.objectweb.asm.Type originReturn = originType.getReturnType();

		// cast if non-minecraft or generics
		org.objectweb.asm.Type targetReturn = targetType.getReturnType();

		this.cast(AbstractAbstracter.Location.RETURN,
				targetReturn,
				originReturn,
				from,
				visitor -> visitor.visitInsn(targetReturn.getOpcode(IRETURN)));
		from.visitInsn(targetReturn.getOpcode(IRETURN));
	}

	protected int loadThis(MethodNode node) {
		this.abstracter.castToMinecraft(node, v -> {
			v.visitVarInsn(ALOAD, 0);
			// from.visitTypeInsn(NEW, owner);
			//	from.visitInsn(DUP);
		}, AbstractAbstracter.Location.THIS);
		return 1;
	}

	public void cast(AbstractAbstracter.Location location,
			org.objectweb.asm.Type fromType,
			org.objectweb.asm.Type toType,
			MethodNode visitor,
			Consumer<MethodVisitor> apply) {
		if (fromType.equals(toType)) {
			apply.accept(visitor);
			return;
		}

		if (fromType.getSort() == OBJECT) {
			String internalName = fromType.getInternalName();
			AbstractAbstracter abstracter = AbstracterConfig.getInterfaceAbstraction(internalName);
			if (abstracter != null) {
				if (toType.getDescriptor().equals(abstracter.getDesc(location))) {
					abstracter.castToMinecraft(visitor, apply, location);
					return;
				} else {
					throw new IllegalStateException(toType + " --/--> " + abstracter.getDesc(location));
				}
			}
		}

		if (toType.getSort() == OBJECT) {
			AbstractAbstracter abstracter = AbstracterConfig.getInterfaceAbstraction(toType.getInternalName());
			if (abstracter != null) {
				if (fromType.getDescriptor().equals(abstracter.getDesc(location))) {
					abstracter.castToCurrent(visitor, apply, location);
					return;
				} else {
					throw new IllegalStateException(toType + " --/--> " + abstracter.getDesc(location));
				}
			}
		}

		if ((toType.getSort() & fromType.getSort()) == OBJECT) {
			Class<?> from = AbstracterConfig.getClass(fromType.getInternalName()), to = AbstracterConfig.getClass(toType.getInternalName());
			if (!to.isAssignableFrom(from)) {
				apply.accept(visitor);
				visitor.visitTypeInsn(CHECKCAST, org.objectweb.asm.Type.getInternalName(to));
			}
		}

		if(fromType.getSort() == ARRAY && toType.getSort() == ARRAY) {
			try {
				String name = fromType.getInternalName();
				AbstracterConfig.getClass(name);
				visitor.visitTypeInsn(CHECKCAST, name);
				return;
			} catch (IllegalArgumentException e) {
			}
		}

		throw new IllegalStateException(this.method + " " + toType + " --/--> " + fromType);
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
