package io.github.astrarre.abstracter.func.post;

import java.io.Serializable;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

import org.objectweb.asm.Type;

@SuppressWarnings ("UnstableApiUsage")
public class ReflectionHacks {
	// warning: cursed code from stack overflow : this is for convenience
	// https://stackoverflow.com/questions/31178103/how-can-i-find-the-target-of-a-java8-method-reference
	public static Method reverseReference(Serializable lambda) {
		SerializedLambda ser = getLambda(lambda);
		if(ser.getImplMethodKind() != MethodHandleInfo.REF_invokeStatic) {
			throw new IllegalArgumentException("Extension methods must be static!");
		}
		String owner = ser.getImplClass();
		String name = ser.getImplMethodName();
		String desc = ser.getImplMethodSignature();
		try {
			Class<?> cls = lambda.getClass().getClassLoader().loadClass(owner.replace('/', '.'));
			for (Method method : cls.getDeclaredMethods()) {
				if(name.equals(method.getName()) && desc.equals(Type.getMethodDescriptor(method))) {
					return method;
				}
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		throw new IllegalArgumentException("Unable to find method " + owner + ";" + name + ";" + desc);
	}

	private static SerializedLambda getLambda(Serializable lambda) {
		for (Class<?> cl = lambda.getClass(); cl != null; cl = cl.getSuperclass()) {
			try {
				Method m = cl.getDeclaredMethod("writeReplace");
				m.setAccessible(true);
				Object replacement = m.invoke(lambda);
				if (!(replacement instanceof SerializedLambda)) {
					break; // custom interface implementation
				}
				return (SerializedLambda) replacement;
			} catch (ReflectiveOperationException e) {
				// do nothing
			}
		}

		throw new IllegalArgumentException("lambda was not a method reference!");
	}
}
