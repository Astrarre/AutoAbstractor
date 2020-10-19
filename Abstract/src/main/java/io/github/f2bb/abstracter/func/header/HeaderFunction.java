package io.github.f2bb.abstracter.func.header;

import static org.objectweb.asm.Opcodes.ACC_ENUM;
import static org.objectweb.asm.Opcodes.V1_8;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import io.github.f2bb.abstracter.impl.JavaUtil;
import io.github.f2bb.abstracter.util.AbstracterUtil;
import io.github.f2bb.abstracter.util.asm.SignatureUtil;
import org.objectweb.asm.tree.ClassNode;

public interface HeaderFunction<T> {
	HeaderFunction<TypeSpec.Builder> JAVA = (a, n, v, s, i) -> {
		TypeSpec.Builder builder;
		ClassName name = JavaUtil.getName(n);
		if (Modifier.isInterface(a)) {
			builder = TypeSpec.interfaceBuilder(name);
		} else if ((ACC_ENUM & a) != 0) {
			builder = TypeSpec.enumBuilder(name);
		} else {
			builder = TypeSpec.classBuilder(name);
		}

		JavaUtil.getModifiers(a).forEach(builder::addModifiers);
		if (s != null) {
			builder.superclass(JavaUtil.toTypeName(s));
		}

		i.forEach(builder::addSuperinterface);
		return builder;
	};

	HeaderFunction<ClassNode> ASM = (a, n, v, s, i) -> {
		ClassNode node = new ClassNode();
		node.visit(V1_8,
				a,
				n,
				null,
				AbstracterUtil.getRawName(s),
				i.stream().map(AbstracterUtil::getRawName).toArray(String[]::new));
		return node;
	};

	T createHeader(int access, String name, TypeVariable<?>[] variables, Type sup, Collection<Type> interfaces);

	static <T> HeaderFunction<T> nothing() {
		return (a, n, v, s, i) -> null;
	}
}
