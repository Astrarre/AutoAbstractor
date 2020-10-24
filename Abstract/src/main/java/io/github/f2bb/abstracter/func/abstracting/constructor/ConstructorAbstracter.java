package io.github.f2bb.abstracter.func.abstracting.constructor;

import java.lang.reflect.Constructor;

import com.squareup.javapoet.TypeSpec;
import io.github.f2bb.abstracter.func.abstracting.constructor.asm.AsmBaseConstructorAbstracter;
import io.github.f2bb.abstracter.func.abstracting.constructor.asm.AsmInterfaceConstructorAbstracter;
import io.github.f2bb.abstracter.func.abstracting.constructor.java.JavaBaseConstructorAbstracter;
import io.github.f2bb.abstracter.func.abstracting.constructor.java.JavaInterfaceConstructorAbstracter;
import io.github.f2bb.abstracter.func.filter.MemberFilter;
import org.objectweb.asm.tree.ClassNode;

public interface ConstructorAbstracter<T> {
	// asm
	ConstructorAbstracter<ClassNode> INTERFACE_ASM = new AsmInterfaceConstructorAbstracter();
	ConstructorAbstracter<ClassNode> BASE_ASM = new AsmBaseConstructorAbstracter();
	// java
	ConstructorAbstracter<TypeSpec.Builder> INTERFACE_JAVA = new JavaInterfaceConstructorAbstracter();
	ConstructorAbstracter<TypeSpec.Builder> BASE_JAVA = new JavaBaseConstructorAbstracter();

	// manual
	void abstractConstructor(T header, Class<?> abstracting, Constructor<?> constructor, boolean impl);

	default ConstructorAbstracter<T> filtered(MemberFilter<Constructor<?>> ctor) {
		return (h, a, c, i) -> {
			if(ctor.test(a, c)) {
				this.abstractConstructor(h, a, c, i);
			}
		};
	}

	static <T> ConstructorAbstracter<T> nothing() {
		return (h, a, c, i) -> {};
	}
}
