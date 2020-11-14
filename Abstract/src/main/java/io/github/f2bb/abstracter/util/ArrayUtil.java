package io.github.f2bb.abstracter.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;

public class ArrayUtil {
	public static <A, B> B[] map(A[] arr, Function<A, B> func, IntFunction<B[]> array) {
		B[] bs = array.apply(arr.length);
		for (int i = 0; i < arr.length; i++) {
			bs[i] = func.apply(arr[i]);
		}
		return bs;
	}

	public static <A, B> List<B> map(A[] arr, Function<A, B> func) {
		ArrayList<B> array = new ArrayList<>(arr.length);
		for (A a : arr) {
			array.add(func.apply(a));
		}
		return array;
	}

	public static <A> A[] add(A[] as, A a) {
		A[] bs = Arrays.copyOf(as, as.length + 1);
		bs[as.length] = a;
		return bs;
	}
}
