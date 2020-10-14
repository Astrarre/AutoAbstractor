package io.github.f2bb.old.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;

import com.squareup.javapoet.WildcardTypeName;
import io.github.f2bb.abstracter.impl.JavaAbstracter;
import org.objectweb.asm.Opcodes;

public class AbstracterUtil implements Opcodes {

	public static String getSign(Method method) {
		try {
			return (String) JavaAbstracter.METHOD_GENERIC.invoke(method);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public static String getSign(Field field) {
		try {
			return (String) JavaAbstracter.FIELD_GENERIC.invoke(field);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public static String getSign(Constructor<?> field) {
		try {
			return (String) JavaAbstracter.CONSTRUCTOR_GENERIC.get(field);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
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
