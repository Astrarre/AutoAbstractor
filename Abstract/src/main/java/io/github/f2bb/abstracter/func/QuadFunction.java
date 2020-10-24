package io.github.f2bb.abstracter.func;

public interface QuadFunction<A, B, C, D, E> {
	E accept(A a, B b, C c, D d);
}
