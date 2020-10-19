package io.github.f2bb.abstracter.util.asm;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;

import com.google.common.reflect.TypeToken;
import io.github.f2bb.abstracter.AbstracterConfig;
import io.github.f2bb.abstracter.util.RawClassType;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;

public class SignatureUtil {
	public static String toSignature(Type reified) {
		SignatureWriter writer = new SignatureWriter();
		visit(writer, reified);
		return writer.toString();
	}

	public static String classSignature(TypeVariable<?>[] variables, Type superClass, Collection<Type> interfaces) {
		SignatureWriter writer = new SignatureWriter();
		visit(writer, variables);
		visit(writer.visitSuperclass(), superClass);
		for (Type iface : interfaces) {
			visit(writer.visitInterface(), iface);
		}
		return writer.toString();
	}

	public static void visit(SignatureVisitor visitor, TypeVariable<?>[] variables) {
		for (TypeVariable<?> variable : variables) {
			visit(visitor, variable);
		}
	}

	public static void visit(SignatureVisitor visitor, TypeVariable<?> variable) {
		visitor.visitFormalTypeParameter(variable.getName());
		boolean first = true;
		for (Type bound : variable.getBounds()) {
			if (first) {
				visitor.visitClassBound();
				first = false;
			} else {
				visitor.visitInterfaceBound();
			}
			visit(visitor, bound);
		}
	}

	public static void visit(SignatureVisitor visitor, Type type, boolean remap) {
		if (type instanceof Class<?>) {
			Class<?> c = (Class<?>) type;
			if (c.isPrimitive()) {
				visitor.visitBaseType(org.objectweb.asm.Type.getDescriptor(c).charAt(0));
			} else {
				if (remap) {
					visitor.visitClassType(AbstracterConfig.getInterfaceName(c));
				} else {
					visitor.visitClassType(org.objectweb.asm.Type.getInternalName(c));
				}
				visitor.visitEnd();
			}
			return;
		} else if (type instanceof GenericArrayType) {
			visit(visitor.visitArrayType(), ((GenericArrayType) type).getGenericComponentType(), remap);
			return;
		} else if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			// visit the type
			Class<?> raw = (Class<?>) pt.getRawType();
			if (raw.isPrimitive()) {
				visitor.visitBaseType(org.objectweb.asm.Type.getDescriptor(raw).charAt(0));
			} else {
				visitor.visitClassType(org.objectweb.asm.Type.getInternalName(raw));
			}

			Type[] args = pt.getActualTypeArguments();
			for (Type arg : args) {
				if (arg instanceof Class<?>) {
					visitor.visitTypeArgument('=');
				}
				visit(visitor, arg, remap);
			}
			visitor.visitEnd();
			return;
		} else if (type instanceof TypeVariable<?>) {
			visitor.visitTypeVariable(((TypeVariable<?>) type).getName());
			return;
		} else if (type instanceof WildcardType) {
			WildcardType wt = (WildcardType) type;
			Type[] array = wt.getLowerBounds();
			if (array.length > 0) {
				visitor.visitTypeArgument('-');
			} else {
				array = wt.getUpperBounds();
				if (array.length == 1 && array[0] == Object.class) {
					visitor.visitTypeArgument('*');
				} else {
					visitor.visitTypeArgument('+');
				}
			}

			for (Type l : array) {
				visit(visitor, l, remap);
			}
			return;
		} else if (type instanceof RawClassType) {
			SignatureReader reader = new SignatureReader(type.getTypeName());
			reader.accept(visitor);
			return;
		} else if (type == null) {
			return;
		}
		throw new IllegalArgumentException("Unrecognized type " + type + " " + type.getClass());
	}

	public static void visit(SignatureVisitor visitor, Type type) {
		visit(visitor, type, true);
	}

	public static StringBuilder typeVarsAsString(TypeVariable<?>[] variables) {
		if (variables.length > 0) {
			StringBuilder builder = new StringBuilder();
			builder.append('<');
			for (TypeVariable<?> variable : variables) {
				builder.append(variable.getName());
				for (Type bound : variable.getBounds()) {
					builder.append(':').append(toSignature(bound));
				}
			}
			builder.append('>');
			return builder;
		}
		return new StringBuilder();
	}

	public static String methodSignature(TypeVariable<?>[] variables,
			TypeToken<?>[] parameters,
			TypeToken<?> returnType) {
		StringBuilder builder = typeVarsAsString(variables);
		builder.append('(');
		for (TypeToken<?> parameter : parameters) {
			builder.append(toSignature(parameter.getType()));
		}
		builder.append(')');
		builder.append(toSignature(returnType.getType()));
		return builder.toString();
	}

	public static String methodDescriptor(TypeToken<?>[] parameters, TypeToken<?> returnType) {
		StringBuilder builder = new StringBuilder();
		builder.append('(');
		for (TypeToken<?> parameter : parameters) {
			builder.append(toSignature(parameter.getRawType()));
		}
		builder.append(')');
		builder.append(toSignature(returnType.getRawType()));
		return builder.toString();
	}
}
