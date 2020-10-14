package io.github.f2bb.abstracter.func.inheritance;

import com.squareup.javapoet.TypeSpec;
import org.objectweb.asm.tree.ClassNode;

public interface InnerClassVisit<T> {
	InnerClassVisit<ClassNode> ASM = (h, c, ih, ic) -> {
		String name = ih.name;
		int last = name.lastIndexOf('$');
		h.visitInnerClass(ih.name, name.substring(0, last), name.substring(last+1), ih.access);
	};

	InnerClassVisit<TypeSpec.Builder> JAVA = (h, c, ih, ic) -> h.addType(ih.build());

	void visitInnerClass(T currentHeader, Class<?> currentClass, T innerHeader, Class<?> innerClass);
}
