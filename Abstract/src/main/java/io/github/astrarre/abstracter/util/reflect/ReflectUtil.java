package io.github.astrarre.abstracter.util.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.java.decompiler.modules.decompiler.exps.AnnotationExprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.Exprent;
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
	private static final Field PAR_NAMES = findField(AnnotationExprent.class, "parNames");
	private static final Field PAR_VALUES = findField(AnnotationExprent.class, "parValues");

	public static Map<String, Exprent> getValues(AnnotationExprent exprent) {
		try {
			List<Exprent> values =  (List<Exprent>) PAR_VALUES.get(exprent);
			List<String> keys = (List<String>) PAR_NAMES.get(exprent);
			Map<String, Exprent> annotation = new HashMap<>();
			for (int i = 0; i < values.size(); i++) {
				annotation.put(keys.get(i), values.get(i));
			}
			return annotation;
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
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

	public static Field findField(Class<?> obj, String name) {
		try {
			Field method = obj.getDeclaredField(name);
			if (!method.isAccessible()) {
				method.setAccessible(true);
			}
			return method;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}