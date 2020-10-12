package io.github.f2bb.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;

import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;
import org.objectweb.asm.Opcodes;

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

	private static final Method METHOD_GENERIC;
	private static final Method FIELD_GENERIC;
	private static final Field CONSTRUCTOR_GENERIC;
	static {
		try {
			METHOD_GENERIC = Method.class.getDeclaredMethod("getGenericSignature");
			METHOD_GENERIC.setAccessible(true);
			FIELD_GENERIC = Field.class.getDeclaredMethod("getGenericSignature");
			FIELD_GENERIC.setAccessible(true);
			CONSTRUCTOR_GENERIC = Constructor.class.getDeclaredField("signature");
			CONSTRUCTOR_GENERIC.setAccessible(true);
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public static String getSign(Method method) {
		try {
			return (String) METHOD_GENERIC.invoke(method);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public static String getSign(Field field) {
		try {
			return (String) FIELD_GENERIC.invoke(field);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public static String getSign(Constructor<?> field) {
		try {
			return (String) CONSTRUCTOR_GENERIC.get(field);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * default not included
	 */
	public static List<javax.lang.model.element.Modifier> getModifiers(int modifier) {
		List<javax.lang.model.element.Modifier> modifiers = new ArrayList<>();
		if (java.lang.reflect.Modifier.isAbstract(modifier)) {
			modifiers.add(javax.lang.model.element.Modifier.ABSTRACT);
		}
		if (java.lang.reflect.Modifier.isTransient(modifier)) {
			modifiers.add(javax.lang.model.element.Modifier.TRANSIENT);
		}
		if (java.lang.reflect.Modifier.isFinal(modifier)) {
			modifiers.add(javax.lang.model.element.Modifier.FINAL);
		}
		if (java.lang.reflect.Modifier.isNative(modifier)) {
			modifiers.add(javax.lang.model.element.Modifier.NATIVE);
		}
		if (java.lang.reflect.Modifier.isPrivate(modifier)) {
			modifiers.add(javax.lang.model.element.Modifier.PRIVATE);
		}
		if (java.lang.reflect.Modifier.isProtected(modifier)) {
			modifiers.add(javax.lang.model.element.Modifier.PROTECTED);
		}
		if (java.lang.reflect.Modifier.isPublic(modifier)) {
			modifiers.add(javax.lang.model.element.Modifier.PUBLIC);
		}
		if (java.lang.reflect.Modifier.isStatic(modifier)) {
			modifiers.add(javax.lang.model.element.Modifier.STATIC);
		}
		if (java.lang.reflect.Modifier.isStrict(modifier)) {
			modifiers.add(javax.lang.model.element.Modifier.STRICTFP);
		}
		if (java.lang.reflect.Modifier.isSynchronized(modifier)) {
			modifiers.add(javax.lang.model.element.Modifier.SYNCHRONIZED);
		}
		if (java.lang.reflect.Modifier.isVolatile(modifier)) {
			modifiers.add(javax.lang.model.element.Modifier.VOLATILE);
		}
		return modifiers;
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
