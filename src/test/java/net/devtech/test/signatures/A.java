package net.devtech.test.signatures;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.f2bb.util.Util;

public class A<O extends Runnable, T> extends AbstractSet<O> implements Consumer<T> {
	@Override
	public Iterator<O> iterator() {
		return null;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public void accept(T t) {

	}

	public class B<C, D extends Function<O, T> & Runnable> extends AbstractList<C> implements Supplier<D>, Runnable {
		@Override
		public C get(int index) {
			return null;
		}

		@Override
		public int size() {
			return 0;
		}

		@Override
		public D get() {
			return null;
		}

		@Override
		public void run() {

		}
	}

	public class CL<L extends Array & Runnable & Function<Runnable, ? extends L>> extends A<Runnable, ? extends L>.B<Integer, L> {
	}

	public static class Test {

	}
}
