package io.github.f2bb.abstracter.util;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Iterator;

import io.github.f2bb.abstracter.AbstracterConfig;

public class AbstracterUtil {
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

}
