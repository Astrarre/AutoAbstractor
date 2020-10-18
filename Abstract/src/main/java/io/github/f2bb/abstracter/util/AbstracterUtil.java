package io.github.f2bb.abstracter.util;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;

import io.github.f2bb.abstracter.AbstracterConfig;
import io.github.f2bb.abstracter.Abstracter;
import org.objectweb.asm.commons.SignatureRemapper;
import org.objectweb.asm.signature.SignatureReader;

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

	public static String getRawName(Type type) {
		if (type instanceof RawClassType) {
			return ((RawClassType) type).getInternalName();
		}
		return org.objectweb.asm.Type.getInternalName(raw(type));
	}

	public static <A> A[] add(A[] as, A a) {
		A[] copy = Arrays.copyOf(as, as.length + 1);
		copy[as.length] = a;
		return copy;
	}

	public static <A, B> B[] map(A[] arr, Function<A, B> func, IntFunction<B[]> array) {
		B[] bs = array.apply(arr.length);
		for (int i = 0; i < arr.length; i++) {
			bs[i] = func.apply(arr[i]);
		}
		return bs;
	}

	public static <A, B> List<B> map(A[] arr, Function<A, B> func) {
		ArrayList<B> array = new ArrayList<>(arr.length);
		for (A a : arr) {
			array.add(func.apply(a));
		}
		return array;
	}

	public static String getInterfaceDesc(Class<?> cls) {
		if(cls.isPrimitive()) {
			return org.objectweb.asm.Type.getDescriptor(cls);
		} else {
			return "L" + AbstracterConfig.getInterfaceName(cls) + ";";
		}
	}

	/**
	 * @return true if the class is a minecraft class, but isn't supposed to be abstracted
	 */
	public static boolean isUnabstractedClass(Class<?> cls) {
		return Abstracter.isMinecraft(cls) && !AbstracterConfig.isInterfaceAbstracted(cls);
	}
}
