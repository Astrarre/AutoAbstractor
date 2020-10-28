package io.github.f2bb.abstracter.func.string;

import org.objectweb.asm.Type;

public interface ToStringFunction<T> {
	ToStringFunction<Class<?>> BASE_DEFAULT = c -> getName(c, "Base");
	ToStringFunction<Class<?>> INTERFACE_DEFAULT = c -> getName(c, "I");
	ToStringFunction<Class<?>> INTERNAL_NAME = Type::getInternalName;

	String toString(T instance);

	default ToStringFunction<T> concat(ToStringFunction<T> func) {
		return i -> this.toString(i) + func.toString(i);
	}

	static String getName(Class<?> cls, String prefix) {
		String str = Type.getInternalName(cls);
		str = str.replace("net/minecraft/", "io/github/f2bb/");
		int last = str.lastIndexOf('/') + 1;
		return str.substring(0, last) + prefix + str.substring(last);
	}

	static <T> ToStringFunction<T> constant(String constant) {
		return c -> constant;
	}
}
