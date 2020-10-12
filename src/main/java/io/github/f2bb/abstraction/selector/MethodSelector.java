package io.github.f2bb.abstraction.selector;

import java.lang.reflect.Method;

public interface MethodSelector {
	Iterable<Method> getMethods(Class<?> cls);
}
