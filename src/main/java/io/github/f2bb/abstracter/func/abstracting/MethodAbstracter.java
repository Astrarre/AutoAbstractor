package io.github.f2bb.abstracter.func.abstracting;

import java.lang.reflect.Method;

import com.squareup.javapoet.TypeSpec;
import io.github.f2bb.abstracter.func.filter.MemberFilter;
import org.objectweb.asm.tree.ClassNode;

public interface MethodAbstracter<T> {
	// asm
	MethodAbstracter<ClassNode> BASE_IMPL_ASM = (h, a, m) -> MethodAbstraction.visitBridged(h, a, m, true, true);
	MethodAbstracter<ClassNode> BASE_API_ASM = (h, a, m) -> MethodAbstraction.visitBridged(h, a, m, false, true);
	MethodAbstracter<ClassNode> INTER_IMPL_ASM = (h, a, m) -> MethodAbstraction.visitBridged(h, a, m, true, false);
	MethodAbstracter<ClassNode> INTER_API_ASM = (h, a, m) -> MethodAbstraction.visitBridged(h, a, m, false, false);
	// java
	MethodAbstracter<TypeSpec.Builder> API_JAVA = MethodAbstraction::visitJava;

	void abstractMethod(T header, Class<?> abstracting, Method method);

	default MethodAbstracter<T> ifElse(MemberFilter<Method> filter, MethodAbstracter<T> abstracter) {
		return (h, c, m) -> {
			if (filter.test(c, m)) {
				this.abstractMethod(h, c, m);
			} else {
				abstracter.abstractMethod(h, c, m);
			}
		};
	}

	default MethodAbstracter<T> and(MethodAbstracter<T> abstracter) {
		return (h, c, m) -> {
			this.abstractMethod(h, c, m);
			abstracter.abstractMethod(h, c, m);
		};
	}

	default MethodAbstracter<T> onlyIf(MemberFilter<Method> filter) {
		return (h, c, m) -> {
			if (filter.test(c, m)) {
				this.abstractMethod(h, c, m);
			}
		};
	}
}
