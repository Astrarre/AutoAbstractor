package io.github.f2bb.abstracter.func.abstracting;

import java.lang.reflect.Constructor;

import com.squareup.javapoet.TypeSpec;
import io.github.f2bb.abstracter.func.filter.MemberFilter;
import org.objectweb.asm.tree.ClassNode;

public interface ConstructorAbstracter<T> {
	// asm
	ConstructorAbstracter<ClassNode> IMPL_INTERFACE_ASM = (h, a, c) -> ConstructorAbstraction.abstractInterfaceCtorAsm(h, a, c, true);
	ConstructorAbstracter<ClassNode> API_INTERFACE_ASM = (h, a, c) -> ConstructorAbstraction.abstractInterfaceCtorAsm(h, a, c, false);
	ConstructorAbstracter<ClassNode> IMPL_BASE_ASM = (h, a, c) -> ConstructorAbstraction.abstractBaseCtorAsm(h, a, c, true);
	ConstructorAbstracter<ClassNode> API_BASE_ASM = (h, a, c) -> ConstructorAbstraction.abstractBaseCtorAsm(h, a, c, false);
	// java
	ConstructorAbstracter<TypeSpec.Builder> API_INTERFACE_JAVA = ConstructorAbstraction::abstractInterfaceCtorJava;
	ConstructorAbstracter<TypeSpec.Builder> IMPL_INTERFACE_JAVA = ConstructorAbstraction::abstractInterfaceCtorJava;

	void abstractConstructor(T header, Class<?> abstracting, Constructor<?> constructor);

	default ConstructorAbstracter<T> filtered(MemberFilter<Constructor<?>> ctor) {
		return (h, a, c) -> {
			if(ctor.test(a, c)) {
				this.abstractConstructor(h, a, c);
			}
		};
	}
}
