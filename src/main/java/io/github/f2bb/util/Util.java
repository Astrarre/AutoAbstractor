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

	public interface Func<T> {
		void accept(T a) throws Throwable;
	}

	public static int lessThanEqual(int a, int b) {
		if (a <= b) {
			return a;
		}
		throw new IllegalArgumentException("a > b!");
	}

	public static <T> Consumer<T> attempt(Func<T> func) {
		return a -> {
			try {
				func.accept(a);
			} catch (Throwable t) {
				throw new RuntimeException(t);
			}
		};
	}

	public static String repeat(char chr, int times) {
		if (times == 0) {
			return "";
		}
		char[] arr = new char[times];
		Arrays.fill(arr, chr);
		return new String(arr);
	}

	public static String capitalizeFirstLetter(String string) {
		return Character.toUpperCase(string.charAt(0)) + string.substring(1);
	}

	public static <A> A[] add(List<A> list, A element, IntFunction<A[]> create) {
		int size = list.size();
		A[] arr = list.toArray(create.apply(size + 1));
		arr[size] = element;
		return arr;
	}

	public static <A> A[] add(A[] array, A element) {
		int len = array.length;
		A[] arr = Arrays.copyOf(array, len + 1);
		arr[len] = element;
		return arr;
	}

	public static <A, B> B[] map(A[] arr, Function<A, B> function, IntFunction<B[]> create) {
		B[] b = create.apply(arr.length);
		for (int i = 0; i < arr.length; i++) {
			b[i] = function.apply(arr[i]);
		}
		return b;
	}

	public static <A, B> B[] map(List<A> arr, Function<A, B> function, IntFunction<B[]> create) {
		int size = arr.size();
		B[] b = create.apply(size);
		for (int i = 0; i < size; i++) {
			b[i] = function.apply(arr.get(i));
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


	public static <T> T invoke(Method method, Object instance, Object... params) {
		try {
			return (T) method.invoke(instance, params);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public static long pack(int a, int b) {
		return (long) a << 32 | (b & 0xffffffffL);
	}

	public static int count(String str, char c) {
		int count = 0;
		for (int i = 0; i < str.length(); i++) {
			if(str.charAt(i) == c)
				count++;
		}
		return count;
	}
}
