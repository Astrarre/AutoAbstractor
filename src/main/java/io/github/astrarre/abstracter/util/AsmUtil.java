package io.github.astrarre.abstracter.util;

import static org.objectweb.asm.Type.getInternalName;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Iterator;

import io.github.astrarre.abstracter.AbstracterConfig;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;

public class AsmUtil {
	public static void visit(AbstracterConfig config, SignatureVisitor visitor, TypeVariable<?>[] variables) {
		for (TypeVariable<?> variable : variables) {
			visitor.visitFormalTypeParameter(variable.getName());
			boolean first = true;
			for (Type bound : variable.getBounds()) {
				if (first) {
					visitor.visitClassBound();
					first = false;
				} else {
					visitor.visitInterfaceBound();
				}
				visit(config, visitor, bound);
			}
		}
	}

	public static void visit(AbstracterConfig config, SignatureVisitor visitor, Type type, boolean remap) {
		visit(config, visitor, type, remap, true);
	}

	public static void visit(AbstracterConfig config, SignatureVisitor visitor, Type type, boolean remap, boolean visitEnd) {
		if (type instanceof Class<?>) {
			Class<?> c = (Class<?>) type;
			if (c.isArray()) {
				visit(config, visitor.visitArrayType(), c.getComponentType(), remap);
			} else if (c.isPrimitive()) {
				visitor.visitBaseType(org.objectweb.asm.Type.getDescriptor(c).charAt(0));
			} else {
				if (remap) {
					visitor.visitClassType(config.getInterfaceName(c));
				} else {
					visitor.visitClassType(getInternalName(c));
				}
				if (visitEnd) {
					visitor.visitEnd();
				}
			}
			return;
		} else if (type instanceof GenericArrayType) {
			visit(config, visitor.visitArrayType(), ((GenericArrayType) type).getGenericComponentType(), remap);
			return;
		} else if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			Type owner = pt.getOwnerType();
			Class<?> raw = (Class<?>) pt.getRawType();
			if (owner != null) {
				visit(config, visitor, owner, remap, false);
				visitor.visitInnerClassType(raw.getSimpleName());
			} else {
				// visit the type
				if (raw.isPrimitive()) {
					visitor.visitBaseType(org.objectweb.asm.Type.getDescriptor(raw).charAt(0));
				} else {
					visitor.visitClassType(getInternalName(raw));
				}
			}

			Type[] args = pt.getActualTypeArguments();
			for (Type arg : args) {
				if (!(arg instanceof WildcardType)) {
					visitor.visitTypeArgument('=');
				}
				visit(config, visitor, arg, remap);
			}

			if (visitEnd) {
				visitor.visitEnd();
			}
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
					visitor.visitTypeArgument();
				} else {
					visitor.visitTypeArgument('+');
				}
			}

			for (Type l : array) {
				visit(config, visitor, l, remap);
			}
			return;
		} else if (type == null) {
			return;
		}
		throw new IllegalArgumentException("Unrecognized type " + type + " " + type.getClass());
	}

	public static void visit(AbstracterConfig config, SignatureVisitor visitor, Type type) {
		visit(config, visitor, type, true);
	}

	public static Class<?> raw(Type type) {
		if (type instanceof Class<?>) {
			return (Class<?>) type;
		} else if (type instanceof GenericArrayType) {
			return Array.newInstance(raw(((GenericArrayType) type).getGenericComponentType()), 0).getClass();
		} else if (type instanceof ParameterizedType) {
			return (Class<?>) ((ParameterizedType) type).getRawType();
		} else if (type instanceof TypeVariable<?>) {
			Iterator<Type> iterator = Arrays.asList(((TypeVariable<?>) type).getBounds()).iterator();
			while (iterator.hasNext()) {
				Type bound = iterator.next();
				if (bound != Object.class) {
					return raw(bound);
				} else if (!iterator.hasNext()) {
					return Object.class;
				}
			}
		} else if (type instanceof WildcardType) {
			// todo
		} else if (type == null) {
			return Object.class;
		}
		throw new UnsupportedOperationException("Raw type " + type + " not found!");
	}

	// todo these need special casing for Consumer stuff, tbh I could just add a global function for this kind of stuff
	public static String toSignature(AbstracterConfig config, Type reified) {
		SignatureWriter writer = new SignatureWriter();
		visit(config, writer, reified);
		return writer.toString();
	}
}
