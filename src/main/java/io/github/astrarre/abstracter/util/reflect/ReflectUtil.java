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
	private static final Method CONSTRUCTOR_SIGNATURE = find(Constructor.class, "getSignature");

	public static String getSignature(Constructor<?> constructor) {
		try {
			return (String) CONSTRUCTOR_SIGNATURE.invoke(constructor);
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