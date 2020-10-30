package io.github.f2bb.abstracter.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;
import org.objectweb.asm.Opcodes;

/**
 * Common utilty methods that are useful when working with reflection.
 */
public class ReflectUtil implements Opcodes {
	/** Static helper class, shouldn't be constructed. */
	private ReflectUtil() {}

	private static final Method CLASS_SIGNATURE = find(Class.class, "getGenericSignature0");
	private static final Method FIELD_SIGNATURE = find(Field.class, "getGenericSignature");
	private static final Method METHOD_SIGNATURE = find(Method.class, "getGenericSignature");
	private static final Method CONSTRUCTOR_SIGNATURE = find(Constructor.class, "getSignature");

	private static final Constructor<WildcardTypeName> WILDCARD_TYPE_NAME_CONSTRUCTOR;

	static {
		try {
			WILDCARD_TYPE_NAME_CONSTRUCTOR = WildcardTypeName.class.getDeclaredConstructor(List.class, List.class, List.class);
			CONSTRUCTOR_SIGNATURE.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public static WildcardTypeName get(List<TypeName> upper, List<TypeName> lower, List<AnnotationSpec> annotations) {
		try {
			return WILDCARD_TYPE_NAME_CONSTRUCTOR.newInstance(upper, lower, annotations);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
	}

	public static String getSignature(Class<?> cls) {
		try {
			return (String) CLASS_SIGNATURE.invoke(cls);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
	}

	public static String getSignature(Constructor<?> constructor) {
		try {
			return (String) CONSTRUCTOR_SIGNATURE.invoke(constructor);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
	}

	public static String getSignature(Method cls) {
		try {
			return (String) METHOD_SIGNATURE.invoke(cls);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
	}

	public static String getSignature(Field cls) {
		try {
			return (String) FIELD_SIGNATURE.invoke(cls);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
	}

	public static Method find(Class<?> obj, String name, Class<?>... params) {
		try {
			Method method = obj.getDeclaredMethod(name, params);
			if (!method.isAccessible()) {
				method.setAccessible(true);
			}
			return method;
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}
}