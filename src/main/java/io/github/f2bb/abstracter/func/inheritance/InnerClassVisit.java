package io.github.f2bb.abstracter.func.inheritance;

public interface InnerClassVisit<T> {
	void visitInnerClass(T currentHeader, Class<?> currentClass, T innerHeader, Class<?> innerClass);
}
