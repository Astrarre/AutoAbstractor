package io.github.f2bb.abstracter.func.abstracting.method;

import java.lang.reflect.Method;

import com.squareup.javapoet.TypeSpec;
import org.objectweb.asm.tree.ClassNode;

public interface MethodAbstracter<T> {
	// asm
	MethodAbstracter<ClassNode> BASE_ASM = new AsmMethodAbstracter(false);
	MethodAbstracter<ClassNode> INTERFACE_ASM = new AsmMethodAbstracter(true);
	// java
	MethodAbstracter<TypeSpec.Builder> API_JAVA_BASE = new JavaMethodAbstracter(false);
	MethodAbstracter<TypeSpec.Builder> API_JAVA_INTERFACE = new JavaMethodAbstracter(true);

	void abstractMethod(T header, Class<?> abstracting, Method method, boolean impl);

	static <T> MethodAbstracter<T> nothing() {
		return (h, a, c, i) -> {};
	}
}
