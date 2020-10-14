package io.github.f2bb.abstracter.func.inheritance;

public interface InnerClassVisit<T> {


	/**
	 * visit an inner class
	 * @param header the header of the abstracted inner class
	 */
	void visitInnerClass(T header, Class<?> cls);
}
