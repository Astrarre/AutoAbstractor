package io.github.f2bb.util;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

public class Util {
	// indy -> generated class -> inlined method
	// somehow deal with inheritance, assuming indy doesn't already handle it
	//   if indy doesnt give us inheritance, add a method invocation to the static init block of every inline type
	//   then, when it's loaded, generate a class and use the VolatileCallSite to point to that new class that accounts for inheritance

	//   if a method is invoked that has a signature of anything other than the Struct, then they are trying to pass `this` into something that
	//     assumes objects are a thing

	public static <A, B> B[] map(A[] arr, Function<A, B> function, IntFunction<B[]> create) {
		B[] b = create.apply(arr.length);
		for (int i = 0; i < arr.length; i++) {
			b[i] = function.apply(arr[i]);
		}
		return b;
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
