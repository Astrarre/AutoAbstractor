package io.github.astrarre.abstracter.abs.method;

import static io.github.astrarre.abstracter.util.ArrayUtil.map;
import static org.objectweb.asm.Type.ARRAY;
import static org.objectweb.asm.Type.OBJECT;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.reflect.TypeToken;
import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.abs.AbstractAbstracter;
import io.github.astrarre.abstracter.abs.member.MemberAbstracter;
import io.github.astrarre.abstracter.func.map.TypeMappingFunction;
import io.github.astrarre.abstracter.util.AnnotationReader;
import org.intellij.lang.annotations.MagicConstant;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public abstract class MethodAbstracter<T extends Executable> extends MemberAbstracter<T> {
	public MethodAbstracter(AbstractAbstracter abstracter, T method, boolean impl) {
		super(abstracter, method, impl);
	}

	public String methodSignature(TypeVariable<?>[] variables, TypeToken<?>[] parameters, TypeToken<?> returnType) {
		StringBuilder builder = MemberAbstracter.typeVarsAsString(variables);
		builder.append('(');
		for (TypeToken<?> parameter : parameters) {
			builder.append(MemberAbstracter.toSignature(parameter.getType()));
		}
		builder.append(')');
		builder.append(MemberAbstracter.toSignature(returnType.getType()));
		return builder.toString();
	}

	public String methodDescriptor(TypeToken<?>[] parameters, TypeToken<?> returnType) {
		StringBuilder builder = new StringBuilder();
		builder.append('(');
		for (TypeToken<?> parameter : parameters) {
			builder.append(MemberAbstracter.toSignature(parameter.getRawType()));
		}
		builder.append(')');
		builder.append(MemberAbstracter.toSignature(returnType.getRawType()));
		return builder.toString();
	}


	public MethodNode abstractMethod(ClassNode header) {
		Header methodHeader = this.getHeader();
		String desc = methodHeader.desc;
		String sign = methodHeader.sign;
		MethodNode node = new MethodNode(methodHeader.access, methodHeader.name, desc, sign, null);
		for (Annotation annotation : this.member.getAnnotations()) {
			if (node.visibleAnnotations == null) {
				node.visibleAnnotations = new ArrayList<>();
			}
			node.visibleAnnotations.add(AnnotationReader.accept(annotation));
		}

		if (this.impl) {
			// if abstract, and base then we don't invoke target, otherwise we're fine
			if (!Modifier.isAbstract(node.access)) {
				// invoke target
				this.invokeTarget(node);
			}
		} else if (!Modifier.isAbstract(node.access)) {
			AbstractAbstracter.visitStub(node);
		}

		for (Parameter parameter : this.member.getParameters()) {
			node.visitParameter(parameter.getName(), 0);
		}
		header.methods.add(node);
		return node;
	}

	// todo add Abstract for interface methods
	public Header getHeader() {
		Function<Type, TypeToken<?>> resolve = TypeMappingFunction.resolve(this.abstracter.cls);
		TypeToken<?>[] params = map(this.member.getGenericParameterTypes(), resolve, TypeToken[]::new);
		TypeToken<?> returnType = resolve.apply(this.member instanceof Constructor ? void.class : ((Method)this.member).getGenericReturnType());
		String desc = this.methodDescriptor(params, returnType);
		String sign = this.impl ? null : this.methodSignature(this.member.getTypeParameters(), params, returnType);
		if(desc.equals(sign)) sign = null;
		int access = this.member.getModifiers();
		return new Header(access, this.member instanceof Constructor ? "<init>" : this.member.getName(), desc, sign);
	}

	protected abstract void invokeTarget(MethodNode node);

	@MagicConstant(flagsFromClass = Opcodes.class)
	public int getOpcode(Member target, @MagicConstant(flagsFromClass = Opcodes.class) int insn) {
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
	public void invoke(MethodNode from, String owner, String name, String desc, @MagicConstant(flagsFromClass = Opcodes.class) int opcode) {
		int index = 0;
		if (opcode != INVOKESTATIC) {
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

		org.objectweb.asm.Type targetReturn = targetType.getReturnType();

		this.cast(AbstractAbstracter.Location.RETURN,
				targetReturn,
				originReturn,
				from,
				visitor -> visitor.visitInsn(originReturn.getOpcode(IRETURN)));
	}

	protected int loadThis(MethodNode node) {
		this.abstracter.castToMinecraft(node, v -> {
			v.visitVarInsn(ALOAD, 0);
			// from.visitTypeInsn(NEW, owner);
			//	from.visitInsn(DUP);
		}, AbstractAbstracter.Location.THIS);
		return 1;
	}

}
