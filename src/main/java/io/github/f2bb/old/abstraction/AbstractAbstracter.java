package io.github.f2bb.old.abstraction;

import static io.github.f2bb.old.util.AbstracterUtil.get;
import static io.github.f2bb.old.util.AbstracterUtil.map;

import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.logging.Logger;
import java.util.zip.ZipOutputStream;

import com.google.common.reflect.TypeToken;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import io.github.f2bb.abstracter.Abstracter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

@SuppressWarnings ("UnstableApiUsage")
public abstract class AbstractAbstracter implements Opcodes {

	protected final AbstractionType abstractionType;
	protected final Class<?> cls;
	protected final TypeToken<?> token;

	protected AbstractAbstracter(AbstractionType loader, Class<?> cls) {
		this.abstractionType = loader;
		this.cls = cls;
		this.token = TypeToken.of(cls);
	}

	/**
	 * write the files to the output stream
	 */
	public abstract void write(ZipOutputStream out) throws IOException;

	public Type resolve(Type type) {
		return this.token.resolveType(type).getType();
	}

	public Class<?> raw(Type type) {
		return this.token.resolveType(type).getRawType();
	}

	public TypeToken<?> resolved(Type type) {
		return this.token.resolveType(type);
	}

	public TypeName toTypeName(Type type) {
		if (type instanceof Class<?>) {
			Class<?> cls = (Class<?>) type;
			if (Abstracter.isMinecraft(cls)) {
				String name = Abstracter.getInterfaceName(cls);
				int pkgIndex = Math.max(name.lastIndexOf('/'), 0);
				String pkg = name.substring(0, pkgIndex);
				int innerIndex = name.indexOf('$', pkgIndex);
				if (innerIndex > 0) {
					return ClassName.get(pkg.replace('/', '.'),
							name.substring(pkgIndex + 1, innerIndex),
							name.substring(innerIndex + 1).split("\\$"));
				} else {
					return ClassName.get(pkg.replace('/', '.'), name.substring(pkgIndex + 1));
				}
			}
			if (!cls.isPrimitive()) {
				return ClassName.get(cls);
			} else {
				return TypeName.get(cls);
			}
		} else if (type instanceof GenericArrayType) {
			return ArrayTypeName.of(this.toTypeName(((GenericArrayType) type).getGenericComponentType()));
		} else if (type instanceof ParameterizedType) {
			ParameterizedType ptn = (ParameterizedType) type;
			return ParameterizedTypeName.get((ClassName) this.toTypeName(ptn.getRawType()),
					map(ptn.getActualTypeArguments(), this::toTypeName, TypeName[]::new));
		} else if (type instanceof TypeVariable<?>) {
			TypeVariable<?> tvn = (TypeVariable<?>) type;
			return TypeVariableName.get(tvn.getName(), map(tvn.getBounds(), this::toTypeName, TypeName[]::new));
		} else if (type instanceof WildcardType) {
			WildcardType wtn = (WildcardType) type;
			return get(map(wtn.getLowerBounds(), this::toTypeName), map(wtn.getUpperBounds(), this::toTypeName));
		}
		throw new IllegalArgumentException("What " + type);
	}

	public String toSignature(Type type) {
		return this.toSignature(type, true);
	}

	public String toSignature(Type type, boolean interfaceDesc) {
		if (type instanceof Class<?>) {
			Class<?> c = (Class<?>) type;
			return interfaceDesc ? Abstracter.getInterfaceDesc(c) : org.objectweb.asm.Type.getDescriptor(c);
		} else if (type instanceof GenericArrayType) {
			return '[' + this.toSignature(((GenericArrayType) type).getGenericComponentType(), interfaceDesc);
		} else if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			String raw = this.toSignature(pt.getRawType(), interfaceDesc);
			Type[] args = pt.getActualTypeArguments();
			if (args.length > 0) {
				StringBuilder builder = new StringBuilder(raw);
				// cut of ';'
				builder.setLength(builder.length() - 1);

				builder.append('<');
				for (Type arg : args) {
					builder.append(this.toSignature(arg, interfaceDesc));
				}
				builder.append('>');
				builder.append(';');
				raw = builder.toString();
			}
			return raw;
		} else if (type instanceof TypeVariable<?>) {
			return "T" + ((TypeVariable<?>) type).getName() + ";";
		} else if (type instanceof WildcardType) {
			WildcardType wt = (WildcardType) type;
			Type[] array = wt.getLowerBounds();
			StringBuilder builder;
			if (array.length > 0) {
				builder = new StringBuilder("-");
			} else {
				builder = new StringBuilder("+");
				array = wt.getUpperBounds();
			}

			for (Type l : array) {
				builder.append(this.toSignature(l, interfaceDesc));
			}
			return builder.toString();
		}
		throw new IllegalArgumentException(String.valueOf(type));
	}

	public StringBuilder typeVarsAsString(TypeVariable<?>[] variables) {
		if (variables.length > 0) {
			StringBuilder builder = new StringBuilder();
			builder.append('<');
			for (TypeVariable<?> variable : variables) {
				builder.append(variable.getName());
				for (Type bound : variable.getBounds()) {
					builder.append(':').append(this.toSignature(bound));
				}
			}
			builder.append('>');
			return builder;
		}
		return new StringBuilder();
	}

	public String methodSignature(TypeVariable<?>[] variables,
			TypeToken<?>[] parameters,
			TypeToken<?> returnType,
			boolean map) {
		StringBuilder builder = this.typeVarsAsString(variables);
		builder.append('(');
		for (TypeToken parameter : parameters) {
			builder.append(this.toSignature(parameter.getType(), map));
		}
		builder.append(')');
		builder.append(this.toSignature(returnType.getType(), map));
		return builder.toString();
	}

	public String methodDescriptor(TypeToken<?>[] parameters, TypeToken<?> returnType) {
		StringBuilder builder = new StringBuilder();
		builder.append('(');
		for (TypeToken parameter : parameters) {
			builder.append(this.toSignature(parameter.getRawType()));
		}
		builder.append(')');
		builder.append(this.toSignature(returnType.getRawType()));
		return builder.toString();
	}

	public String classSignature(TypeVariable<?>[] variables, Type superClass, Type[] interfaces) {
		StringBuilder builder = this.typeVarsAsString(variables);
		builder.append(this.toSignature(superClass));
		for (Type anInterface : interfaces) {
			builder.append(this.toSignature(anInterface));
		}
		return builder.toString();
	}

	public void invoke(MethodVisitor visitor, Method method, boolean special) {
		Class<?> dec = method.getDeclaringClass();
		this.invoke(visitor,
				method.getModifiers(),
				org.objectweb.asm.Type.getInternalName(dec),
				method.getName(),
				org.objectweb.asm.Type.getMethodDescriptor(method),
				special ? INVOKESPECIAL : INVOKEVIRTUAL,
				dec.isInterface());
	}

	public void invoke(MethodVisitor visitor,
			int access,
			String owner,
			String name,
			String desc,
			int instanceOpcode,
			boolean isInterface) {
		if (isInterface && instanceOpcode == INVOKEVIRTUAL) {
			instanceOpcode = INVOKEINTERFACE;
		}

		org.objectweb.asm.Type methodDesc = org.objectweb.asm.Type.getMethodType(desc);
		int index = 0;
		int opcode;
		// todo casts
		if (!Modifier.isStatic(access)) {
			visitor.visitVarInsn(ALOAD, index++);
			opcode = instanceOpcode;
		} else {
			opcode = INVOKESTATIC;
		}

		for (org.objectweb.asm.Type type : methodDesc.getArgumentTypes()) {
			visitor.visitVarInsn(type.getOpcode(ILOAD), index++);
		}
		visitor.visitMethodInsn(opcode, owner, name, desc, isInterface);
		visitor.visitInsn(methodDesc.getReturnType().getOpcode(IRETURN));
	}
}
