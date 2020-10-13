package io.github.f2bb.abstracter;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import io.github.f2bb.abstracter.util.RawClassType;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

public class AbstracterUtil {
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

	public static void visit(SignatureVisitor visitor, Type type) {
		if (type instanceof Class<?>) {
			Class<?> c = (Class<?>) type;
			if (c.isPrimitive()) {
				visitor.visitBaseType(org.objectweb.asm.Type.getDescriptor(c).charAt(0));
			} else {
				visitor.visitClassType(org.objectweb.asm.Type.getInternalName(c));
				visitor.visitEnd();
			}
			return;
		} else if (type instanceof GenericArrayType) {
			visit(visitor.visitArrayType(), ((GenericArrayType) type).getGenericComponentType());
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
				visit(visitor, arg);
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
				visit(visitor, l);
			}
			return;
		} else if (type instanceof RawClassType) {
			SignatureReader reader = new SignatureReader(type.getTypeName());
			reader.accept(visitor);
			return;
		}
		throw new IllegalArgumentException("Unrecognized type " + type + " " + type.getClass());
	}
}
