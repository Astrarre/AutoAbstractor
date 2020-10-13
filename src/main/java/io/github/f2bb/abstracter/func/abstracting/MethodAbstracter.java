package io.github.f2bb.abstracter.func.abstracting;

import java.lang.reflect.Method;

public interface MethodAbstracter<T> {
	void abstractField(T header, Class<?> abstracting, Method field);
}
