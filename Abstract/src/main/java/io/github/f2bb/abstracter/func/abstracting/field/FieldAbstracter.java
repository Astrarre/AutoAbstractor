package io.github.f2bb.abstracter.func.abstracting.field;

import java.lang.reflect.Field;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import io.github.f2bb.ImplementationHiddenException;
import io.github.f2bb.abstracter.func.QuadFunction;
import io.github.f2bb.abstracter.func.abstracting.field.asm.AsmConstantFieldAbstracter;
import io.github.f2bb.abstracter.func.abstracting.field.asm.AsmGetterAbstracter;
import io.github.f2bb.abstracter.func.abstracting.field.asm.AsmSetterAbstracter;
import io.github.f2bb.abstracter.func.abstracting.field.java.JavaGetterFieldAbstracter;
import io.github.f2bb.abstracter.func.abstracting.field.java.JavaSetterFieldAbstracter;
import io.github.f2bb.abstracter.func.map.TypeMappingFunction;
import io.github.f2bb.abstracter.impl.JavaUtil;
import io.github.f2bb.abstracter.util.asm.TypeUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public interface FieldAbstracter<T> extends Opcodes {
	FieldAbstracter<ClassNode> ASM_VIRTUAL_FIELD = (h, c, f, i) -> {
		java.lang.reflect.Type reified = TypeMappingFunction.reify(c, f.getGenericType());
		FieldNode node = new FieldNode(f.getModifiers(),
				f.getName(),
				Type.getDescriptor(f.getType()),
				TypeUtil.toSignature(reified),
				null);
		h.fields.add(node);
	};

	FieldAbstracter<TypeSpec.Builder> JAVA_VIRTUAL_FIELD = (h, c, f, i) -> {
		FieldSpec.Builder builder = FieldSpec.builder(JavaUtil.toTypeName(TypeMappingFunction
				                                                                  .reify(c, f.getGenericType())),
				f.getName(),
				JavaUtil.getModifiers(f.getModifiers()).toArray(new Modifier[0]));
		builder.initializer("$T.instance()", ImplementationHiddenException.class);
		h.addField(builder.build());
	};

	FieldAbstracter<ClassNode> ASM_CONSTANT = new AsmConstantFieldAbstracter();
	FieldAbstracter<ClassNode> ASM_GETTER_BASE = new AsmGetterAbstracter(false);
	FieldAbstracter<ClassNode> ASM_SETTER_BASE = new AsmSetterAbstracter(false);
	FieldAbstracter<ClassNode> ASM_GETTER_INTER = new AsmGetterAbstracter(true);
	FieldAbstracter<ClassNode> ASM_SETTER_INTER = new AsmSetterAbstracter(true);
	FieldAbstracter<TypeSpec.Builder> JAVA_GETTER_API_INTER = new JavaGetterFieldAbstracter(true);
	FieldAbstracter<TypeSpec.Builder> JAVA_GETTER_API_BASE = new JavaGetterFieldAbstracter(false);
	FieldAbstracter<TypeSpec.Builder> JAVA_SETTER_API_INTER = new JavaSetterFieldAbstracter(true);
	FieldAbstracter<TypeSpec.Builder> JAVA_SETTER_API_BASE = new JavaSetterFieldAbstracter(false);
	// asm
	FieldAbstracter<ClassNode> BASE_ASM = new BaseFieldAbstracter<>(FieldAbstracter.ASM_VIRTUAL_FIELD,
			ASM_GETTER_BASE,
			ASM_SETTER_BASE);
	FieldAbstracter<ClassNode> INTERFACE_ASM = new InterfaceFieldAbstracter<>(ASM_CONSTANT,
			ASM_GETTER_INTER,
			ASM_SETTER_INTER);

	// java
	FieldAbstracter<TypeSpec.Builder> BASE_API_JAVA = new BaseFieldAbstracter<>(JAVA_VIRTUAL_FIELD,
			JAVA_GETTER_API_BASE,
			JAVA_SETTER_API_BASE);
	FieldAbstracter<TypeSpec.Builder> INTERFACE_API_JAVA = new InterfaceFieldAbstracter<>(JAVA_VIRTUAL_FIELD,
			JAVA_GETTER_API_INTER,
			JAVA_SETTER_API_INTER);


	void abstractField(T header, Class<?> abstracting, Field field, boolean impl);

	static <T> FieldAbstracter<T> nothing() {
		return (h, a, c, i) -> {};
	}
}
