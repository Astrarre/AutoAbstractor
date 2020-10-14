package io.github.f2bb.abstracter.func.abstracting;

import java.lang.reflect.Field;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import io.github.f2bb.abstracter.ex.ImplementationHiddenException;
import io.github.f2bb.abstracter.func.filter.Filters;
import io.github.f2bb.abstracter.func.filter.MemberFilter;
import io.github.f2bb.abstracter.func.map.TypeMappingFunction;
import io.github.f2bb.abstracter.impl.AsmAbstracter;
import io.github.f2bb.abstracter.impl.JavaAbstracter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

public interface FieldAbstracter<T> {
	FieldAbstracter<ClassNode> IMPL_EMPTY = (h, c, f) -> {};
	FieldAbstracter<ClassNode> ASM_API_EMPTY = (h, c, f) -> {
		java.lang.reflect.Type reified = TypeMappingFunction.reify(c, f.getGenericType());
		FieldNode node = new FieldNode(f.getModifiers(),
				f.getName(),
				Type.getDescriptor(f.getType()),
				AsmAbstracter.toSignature(reified),
				null);
		h.fields.add(node);
	};

	FieldAbstracter<TypeSpec.Builder> JAVA_EMPTY_API = (h, c, f) -> {
		FieldSpec.Builder builder = FieldSpec.builder(JavaAbstracter.toTypeName(TypeMappingFunction.reify(c,
				f.getGenericType())),
				f.getName(),
				JavaAbstracter.getModifiers(f.getModifiers()).toArray(new Modifier[0]));
		builder.initializer("$T.instance()", ImplementationHiddenException.class);
		h.addField(builder.build());
	};

	FieldAbstracter<ClassNode> ASM_GETTER_IMPL_BASE = (h, c, f) -> h.methods.add(FieldAbstraction.generateGetter(c, f, true, false));
	FieldAbstracter<ClassNode> ASM_GETTER_API_BASE = (h, c, f) -> h.methods.add(FieldAbstraction.generateGetter(c, f, false, false));
	FieldAbstracter<ClassNode> ASM_SETTER_IMPL_BASE = (h, c, f) -> h.methods.add(FieldAbstraction.generateSetter(c, f, true, false));
	FieldAbstracter<ClassNode> ASM_SETTER_API_BASE = (h, c, f) -> h.methods.add(FieldAbstraction.generateGetter(c, f, false, false));

	FieldAbstracter<ClassNode> ASM_GETTER_IMPL_INTER = (h, c, f) -> h.methods.add(FieldAbstraction.generateGetter(c, f, true, true));
	FieldAbstracter<ClassNode> ASM_GETTER_API_INTER = (h, c, f) -> h.methods.add(FieldAbstraction.generateGetter(c, f, false, true));
	FieldAbstracter<ClassNode> ASM_SETTER_IMPL_INTER = (h, c, f) -> h.methods.add(FieldAbstraction.generateSetter(c, f, true, true));
	FieldAbstracter<ClassNode> ASM_SETTER_API_INTER = (h, c, f) -> h.methods.add(FieldAbstraction.generateGetter(c, f, false, true));

	FieldAbstracter<TypeSpec.Builder> JAVA_GETTER_API_INTER = FieldAbstraction.getJavaGetter(true);
	FieldAbstracter<TypeSpec.Builder> JAVA_GETTER_API_BASE = FieldAbstraction.getJavaGetter(false);

	FieldAbstracter<TypeSpec.Builder> JAVA_SETTER_API_INTER = FieldAbstraction.getJavaSetter(true);
	FieldAbstracter<TypeSpec.Builder> JAVA_SETTER_API_BASE = FieldAbstraction.getJavaSetter(false);

	// asm
	FieldAbstracter<ClassNode> BASE_IMPL_ASM = defaultBase(IMPL_EMPTY, ASM_GETTER_IMPL_BASE, ASM_SETTER_IMPL_BASE);
	FieldAbstracter<ClassNode> BASE_API_ASM = defaultBase(ASM_API_EMPTY, ASM_GETTER_API_BASE, ASM_SETTER_API_BASE);
	FieldAbstracter<ClassNode> INTERFACE_IMPL_ASM = defaultInterface(ASM_GETTER_IMPL_INTER, ASM_SETTER_IMPL_INTER);
	FieldAbstracter<ClassNode> INTERFACE_API_ASM = defaultInterface(ASM_GETTER_API_INTER, ASM_SETTER_API_INTER);
	// java
	FieldAbstracter<TypeSpec.Builder> BASE_API_JAVA = defaultBase(JAVA_EMPTY_API, JAVA_GETTER_API_BASE, JAVA_SETTER_API_BASE);
	FieldAbstracter<TypeSpec.Builder> INTER_API_JAVA = defaultInterface(JAVA_GETTER_API_INTER, JAVA_SETTER_API_INTER);


	static <T> FieldAbstracter<T> defaultBase(FieldAbstracter<T> empty,
			FieldAbstracter<T> getter,
			FieldAbstracter<T> setter) {
		return getter.and(setter.onlyIf(MemberFilter.<Field>withAccess(Filters.FINAL).negate()))
				       .ifElse(MemberFilter.MINECRAFT_TYPE.or(MemberFilter.withAccess(Filters.STATIC))
						               .and(MemberFilter.withAccess(Filters.PUBLIC)), empty);
	}

	static <T> FieldAbstracter<T> defaultInterface(FieldAbstracter<T> getter, FieldAbstracter<T> setter) {
		return getter.and(setter.onlyIf(MemberFilter.<Field>withAccess(Filters.FINAL).negate()));
	}

	void abstractField(T header, Class<?> abstracting, Field field);

	default FieldAbstracter<T> ifElse(MemberFilter<Field> filter, FieldAbstracter<T> abstracter) {
		return (h, c, f) -> {
			if (filter.test(c, f)) {
				this.abstractField(h, c, f);
			} else {
				abstracter.abstractField(h, c, f);
			}
		};
	}

	default FieldAbstracter<T> and(FieldAbstracter<T> abstracter) {
		return (h, c, f) -> {
			this.abstractField(h, c, f);
			abstracter.abstractField(h, c, f);
		};
	}

	default FieldAbstracter<T> onlyIf(MemberFilter<Field> filter) {
		return (h, c, f) -> {
			if (filter.test(c, f)) {
				this.abstractField(h, c, f);
			}
		};
	}
}
