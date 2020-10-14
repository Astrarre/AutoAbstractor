package io.github.f2bb.old.abstraction;

import static io.github.f2bb.old.util.AbstracterUtil.map;

import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.zip.ZipOutputStream;

import com.google.common.reflect.TypeToken;
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


}
