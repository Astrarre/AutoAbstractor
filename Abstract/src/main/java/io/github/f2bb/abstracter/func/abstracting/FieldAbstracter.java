package io.github.f2bb.abstracter.func.abstracting;

import java.lang.reflect.Field;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import io.github.f2bb.ImplementationHiddenException;
import io.github.f2bb.abstracter.func.filter.Filters;
import io.github.f2bb.abstracter.func.filter.MemberFilter;
import io.github.f2bb.abstracter.func.map.TypeMappingFunction;
import io.github.f2bb.abstracter.impl.JavaUtil;
import io.github.f2bb.abstracter.util.asm.MethodUtil;
import io.github.f2bb.abstracter.util.asm.SignUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

public interface FieldAbstracter<T> extends Opcodes {
	FieldAbstracter<ClassNode> IMPL_EMPTY = (h, c, f) -> {};
	FieldAbstracter<ClassNode> ASM_API_EMPTY = (h, c, f) -> {
		java.lang.reflect.Type reified = TypeMappingFunction.reify(c, f.getGenericType());
		FieldNode node = new FieldNode(f.getModifiers(),
				f.getName(),
				Type.getDescriptor(f.getType()),
				SignUtil.toSignature(reified),
				null);
		h.fields.add(node);
	};

	static FieldAbstracter<ClassNode> constant(boolean impl) {
		return (h, c, f) -> {
			java.lang.reflect.Type reified = TypeMappingFunction.reify(c, f.getGenericType());
			FieldNode node = new FieldNode(f.getModifiers(),
					f.getName(),
					SignUtil.getInterfaceDesc(TypeMappingFunction.raw(c, f.getGenericType())),
					SignUtil.toSignature(reified),
					null);
			h.fields.add(node);
			if (impl) {
				MethodNode init = MethodUtil.findOrCreateMethod(ACC_STATIC | ACC_PUBLIC, h, "<clinit>", "()V");
				InsnList list = init.instructions;
				if(list.getLast() == null) {
					list.insert(new InsnNode(RETURN));
				}

				InsnList insn = new InsnList();
				insn.add(new FieldInsnNode(GETSTATIC,
						Type.getInternalName(f.getDeclaringClass()),
						f.getName(),
						Type.getDescriptor(f.getType())));
				insn.add(new FieldInsnNode(PUTSTATIC, h.name, node.name, node.desc));
				list.insert(insn);
			}
		};
	}

	FieldAbstracter<TypeSpec.Builder> JAVA_EMPTY_API = (h, c, f) -> {
		FieldSpec.Builder builder = FieldSpec.builder(JavaUtil.toTypeName(TypeMappingFunction
				                                                                  .reify(c, f.getGenericType())),
				f.getName(),
				JavaUtil.getModifiers(f.getModifiers()).toArray(new Modifier[0]));
		builder.initializer("$T.instance()", ImplementationHiddenException.class);
		h.addField(builder.build());
	};

	FieldAbstracter<ClassNode> ASM_GETTER_IMPL_BASE = (h, c, f) -> h.methods.add(FieldAbstraction.generateGetter(c,
			f,
			true,
			false));
	FieldAbstracter<ClassNode> ASM_GETTER_API_BASE = (h, c, f) -> h.methods.add(FieldAbstraction.generateGetter(c,
			f,
			false,
			false));
	FieldAbstracter<ClassNode> ASM_SETTER_IMPL_BASE = (h, c, f) -> h.methods.add(FieldAbstraction.generateSetter(c,
			f,
			true,
			false));
	FieldAbstracter<ClassNode> ASM_SETTER_API_BASE = (h, c, f) -> h.methods.add(FieldAbstraction.generateGetter(c,
			f,
			false,
			false));

	FieldAbstracter<ClassNode> ASM_GETTER_IMPL_INTER = (h, c, f) -> h.methods.add(FieldAbstraction.generateGetter(c,
			f,
			true,
			true));
	FieldAbstracter<ClassNode> ASM_GETTER_API_INTER = (h, c, f) -> h.methods.add(FieldAbstraction.generateGetter(c,
			f,
			false,
			true));
	FieldAbstracter<ClassNode> ASM_SETTER_IMPL_INTER = (h, c, f) -> h.methods.add(FieldAbstraction.generateSetter(c,
			f,
			true,
			true));
	FieldAbstracter<ClassNode> ASM_SETTER_API_INTER = (h, c, f) -> h.methods.add(FieldAbstraction.generateGetter(c,
			f,
			false,
			true));

	FieldAbstracter<TypeSpec.Builder> JAVA_GETTER_API_INTER = FieldAbstraction.getJavaGetter(true);
	FieldAbstracter<TypeSpec.Builder> JAVA_GETTER_API_BASE = FieldAbstraction.getJavaGetter(false);

	FieldAbstracter<TypeSpec.Builder> JAVA_SETTER_API_INTER = FieldAbstraction.getJavaSetter(true);
	FieldAbstracter<TypeSpec.Builder> JAVA_SETTER_API_BASE = FieldAbstraction.getJavaSetter(false);

	// asm
	FieldAbstracter<ClassNode> BASE_IMPL_ASM = defaultBase(IMPL_EMPTY, ASM_GETTER_IMPL_BASE, ASM_SETTER_IMPL_BASE);
	FieldAbstracter<ClassNode> BASE_API_ASM = defaultBase(ASM_API_EMPTY, ASM_GETTER_API_BASE, ASM_SETTER_API_BASE);

	FieldAbstracter<ClassNode> INTERFACE_IMPL_ASM = defaultInterface(constant(true),
			ASM_GETTER_IMPL_INTER,
			ASM_SETTER_IMPL_INTER);
	FieldAbstracter<ClassNode> INTERFACE_API_ASM = defaultInterface(constant(false),
			ASM_GETTER_API_INTER,
			ASM_SETTER_API_INTER);

	// java
	FieldAbstracter<TypeSpec.Builder> BASE_API_JAVA = defaultBase(JAVA_EMPTY_API,
			JAVA_GETTER_API_BASE,
			JAVA_SETTER_API_BASE);
	FieldAbstracter<TypeSpec.Builder> INTERFACE_API_JAVA = defaultInterface(JAVA_EMPTY_API,
			JAVA_GETTER_API_INTER,
			JAVA_SETTER_API_INTER);


	static <T> FieldAbstracter<T> defaultBase(FieldAbstracter<T> empty,
			FieldAbstracter<T> getter,
			FieldAbstracter<T> setter) {
		return getter.and(setter.onlyIf(MemberFilter.<Field>withAccess(Filters.FINAL).negate()))
		             .ifElse(MemberFilter.MINECRAFT_TYPE.or(MemberFilter.withAccess(Filters.STATIC))
		                                                .and(MemberFilter.withAccess(Filters.PUBLIC)), empty);
	}

	static <T> FieldAbstracter<T> defaultInterface(FieldAbstracter<T> empty,
			FieldAbstracter<T> getter,
			FieldAbstracter<T> setter) {
		return empty.ifElse(MemberFilter.<Field>withAccess(Filters.STATIC).and(MemberFilter.withAccess(Filters.FINAL)),
				getter.and(setter.onlyIf(MemberFilter.<Field>withAccess(Filters.FINAL).negate())));
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

	static <T> FieldAbstracter<T> nothing() {
		return (h, a, c) -> {};
	}
}
