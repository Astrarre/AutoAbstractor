package io.github.f2bb.abstracter.func.string;

import io.github.f2bb.abstracter.Abstracter;

public interface ToStringFunction<T> {
	ToStringFunction<Class<?>> BASE_DEFAULT = Abstracter::getBaseName;
	ToStringFunction<Class<?>> INTERFACE_DEFAULT = Abstracter::getInterfaceName;

	String toString(T instance);

	default ToStringFunction<T> concat(ToStringFunction<T> func) {
		return i -> this.toString(i) + func.toString(i);
	}
}
