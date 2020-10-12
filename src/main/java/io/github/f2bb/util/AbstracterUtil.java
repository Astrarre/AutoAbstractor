package io.github.f2bb.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;

import com.google.common.collect.Collections2;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class AbstracterUtil implements Opcodes {

	private static final Constructor<WildcardTypeName> WILDCARD_TYPE_NAME_CONSTRUCTOR;

	static {
		try {
			WILDCARD_TYPE_NAME_CONSTRUCTOR = WildcardTypeName.class.getDeclaredConstructor(List.class,
					List.class,
					List.class);
			WILDCARD_TYPE_NAME_CONSTRUCTOR.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public static WildcardTypeName get(List<TypeName> upper, List<TypeName> lower) {
		try {
			return WILDCARD_TYPE_NAME_CONSTRUCTOR.newInstance(upper, lower, Collections.emptyList());
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
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
}
