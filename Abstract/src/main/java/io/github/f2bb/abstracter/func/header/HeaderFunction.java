package io.github.f2bb.abstracter.func.header;

import static org.objectweb.asm.Opcodes.ACC_ENUM;
import static org.objectweb.asm.Opcodes.V1_8;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import io.github.f2bb.abstracter.AbstracterConfig;
import io.github.f2bb.abstracter.util.ArrayUtil;
import io.github.f2bb.abstracter.util.asm.TypeUtil;
import io.github.f2bb.abstracter.util.java.JavaUtil;
import org.objectweb.asm.tree.ClassNode;

public interface HeaderFunction<T> {
	static HeaderFunction<TypeSpec.Builder> getJava(boolean isBase) {
		return (c, a, n, v, s, i) -> {
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

			for (Type type : i) {
				builder.addSuperinterface(JavaUtil.toTypeName(type));
			}

			if (isBase) {
				builder.addSuperinterface(JavaUtil.getName(AbstracterConfig.getInterfaceName(c)));
			}
			return builder;
		};
	}

	static HeaderFunction<ClassNode> getAsm(boolean isBase) {
		return (c, a, n, v, s, i) -> {
			ClassNode node = new ClassNode();
			String[] interfaces = i.stream().map(TypeUtil::getRawName).toArray(String[]::new);
			if (isBase) {
				interfaces = ArrayUtil.add(interfaces, AbstracterConfig.getInterfaceName(c));
			}
			node.visit(V1_8, a, n, null, TypeUtil.getRawName(s), interfaces);
			return node;
		};
	}

	static <T> HeaderFunction<T> nothing() {
		return (c, a, n, v, s, i) -> null;
	}

	T createHeader(Class<?> cls,
			int access,
			String name,
			TypeVariable<?>[] variables,
			Type sup,
			Collection<Type> interfaces);
}
