package io.github.f2bb.abstracter.func.header;

import static org.objectweb.asm.Opcodes.ACC_ENUM;
import static org.objectweb.asm.Opcodes.ASM9;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;

import com.squareup.javapoet.TypeSpec;
import io.github.f2bb.abstracter.impl.AsmAbstracter;
import io.github.f2bb.abstracter.impl.JavaAbstracter;
import io.github.f2bb.abstracter.util.AbstracterUtil;
import org.objectweb.asm.tree.ClassNode;

public interface HeaderFunction<T> {
	HeaderFunction<TypeSpec.Builder> JAVA = (a, n, v, s, i) -> {
		TypeSpec.Builder builder;
		if(Modifier.isInterface(a)) {
			builder = TypeSpec.interfaceBuilder(n);
		} else if((ACC_ENUM & a) != 0) {
			builder = TypeSpec.enumBuilder(n);
		} else {
			builder = TypeSpec.classBuilder(n);
		}
		JavaAbstracter.getModifiers(a).forEach(builder::addModifiers);
		builder.superclass(s);
		i.forEach(builder::addSuperinterface);
		return builder;
	};

	HeaderFunction<ClassNode> ASM = (a, n, v, s, i) -> {
		ClassNode node = new ClassNode();
		node.visit(ASM9,
				a,
				n,
				AsmAbstracter.classSignature(v, s, i),
				AbstracterUtil.getRawName(s),
				i.stream().map(AbstracterUtil::getRawName).toArray(String[]::new));
		return node;
	};

	T createHeader(int access, String name, TypeVariable<?>[] variables, Type sup, Collection<Type> interfaces);

}
