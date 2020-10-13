package io.github.f2bb.abstracter.util;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

public class AbstracterUtil {
	public static Class<?> raw(Type type) {
		if(type instanceof Class<?>)
			return (Class<?>) type;
		else if(type instanceof GenericArrayType) {
			return Array.newInstance(raw(((GenericArrayType) type).getGenericComponentType()), 0).getClass();
		} else if(type instanceof ParameterizedType) {
			return (Class<?>) ((ParameterizedType) type).getRawType();
		} else if(type instanceof TypeVariable<?>) {
			// todo
		} else if(type instanceof WildcardType) {
			// todo
		}
		throw new UnsupportedOperationException("Raw type " + type + " not found!");
	}

	public static String getRawName(Type type) {
		if(type instanceof RawClassType) {
			// todo
		}
		return org.objectweb.asm.Type.getInternalName(raw(type));
	}
}
