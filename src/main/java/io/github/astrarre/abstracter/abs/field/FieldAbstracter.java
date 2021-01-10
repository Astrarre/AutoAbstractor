package io.github.astrarre.abstracter.abs.field;

import static org.objectweb.asm.Type.getInternalName;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;

import io.github.astrarre.abstracter.abs.AbstractAbstracter;
import io.github.astrarre.abstracter.abs.member.MemberAbstracter;
import io.github.astrarre.abstracter.func.map.TypeMappingFunction;
import io.github.astrarre.abstracter.util.AnnotationReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

public abstract class FieldAbstracter extends MemberAbstracter<Field> {
	public FieldAbstracter(AbstractAbstracter abstracter, Field member, boolean impl) {
		super(abstracter, member, impl);
	}

	public void abstractField(ClassNode node) {
		Header constantHeader = this.getHeader(Abstraction.CONSTANT);
		if (this.isConstant(constantHeader)) {
			this.createConstant(constantHeader, node);
		} else {
			if (this.getter()) {
				Header header = this.getHeader(Abstraction.GETTER);
				MethodNode getter = this.createGetter(header);
				if (!AbstractAbstracter.conflicts(getter.name, getter.desc, node)) {
					node.methods.add(getter);
				}
			}

			if (this.setter()) {
				Header header = this.getHeader(Abstraction.SETTER);
				MethodNode setter = this.createSetter(header);
				if (!AbstractAbstracter.conflicts(setter.name, setter.desc, node)) {
					node.methods.add(setter);
				}
			}
		}
	}

	public enum Abstraction {
		GETTER,
		SETTER,
		CONSTANT
	}

	protected Header getHeader(Abstraction abstraction) {
		// todo change the descriptor to instead use return and get
		Type reified = TypeMappingFunction.reify(this.abstracter.cls, this.member.getGenericType());
		Header header = new Header(this.member.getModifiers() & ~ACC_ENUM,
				this.member.getName(),
				this.abstracter.getInterfaceDesc(TypeMappingFunction.raw(this.abstracter.cls, this.member.getGenericType())),
				toSignature(reified));
		if (header.desc.equals(header.sign)) {
			header.sign = null;
		}
		return header;
	}

	protected abstract boolean isConstant(Header header);

	public FieldNode createConstant(Header header, ClassNode node) {
		FieldNode field = new FieldNode(header.access, header.name, header.desc, header.sign, null);

		if (Modifier.isStatic(field.access) && this.impl) {
			MethodNode init = AbstractAbstracter.findMethod(node, "astrarre_artificial_clinit", "()V");
			InsnList list = init.instructions;
			InsnList insn = new InsnList();
			insn.add(new FieldInsnNode(GETSTATIC,
					getInternalName(this.member.getDeclaringClass()),
					this.member.getName(),
					org.objectweb.asm.Type.getDescriptor(this.member.getType())));
			insn.add(new FieldInsnNode(PUTSTATIC, node.name, field.name, field.desc));
			list.insert(insn);
		}

		node.fields.add(field);
		return field;
	}

	protected boolean getter() {
		return true;
	}

	public MethodNode createGetter(Header header) {
		int access = header.access;
		access &= ~ACC_ENUM;
		String owner = getInternalName(this.member.getDeclaringClass());
		MethodNode node = new MethodNode(access, this.getEtterName("get", header.name),
				"()" + header.desc,
				header.sign == null ? null : "()" + header.sign,
				null);
		for (Annotation annotation : this.member.getAnnotations()) {
			if (node.visibleAnnotations == null) {
				node.visibleAnnotations = new ArrayList<>();
			}
			node.visibleAnnotations.add(AnnotationReader.accept(annotation));
		}

		if (this.impl) {
			org.objectweb.asm.Type ret = org.objectweb.asm.Type.getType(this.member.getType());
			if (Modifier.isStatic(access)) {
				node.visitFieldInsn(GETSTATIC, owner, this.member.getName(), ret.getDescriptor());
			} else {
				this.abstracter.castToMinecraft(node, visitor -> visitor.visitVarInsn(ALOAD, 0), AbstractAbstracter.Location.THIS);
				node.visitFieldInsn(GETFIELD, owner, this.member.getName(), ret.getDescriptor());
			}

			org.objectweb.asm.Type actual = org.objectweb.asm.Type.getType(header.desc);
			this.cast(AbstractAbstracter.Location.RETURN, ret, actual, node, v -> v.visitInsn(actual.getOpcode(IRETURN)));
		} else {
			AbstractAbstracter.visitStub(node);
		}

		return node;
	}

	protected boolean setter() {
		return !Modifier.isFinal(this.member.getModifiers());
	}

	public MethodNode createSetter(Header header) {
		String owner = getInternalName(this.member.getDeclaringClass());
		MethodNode node = new MethodNode(header.access,
				this.getEtterName("set", header.name),
				"(" + header.desc + ")V",
				header.sign == null ? null : "(" + header.sign + ")V",
				null);

		for (Annotation annotation : this.member.getAnnotations()) {
			if (node.visibleAnnotations == null) {
				node.visibleAnnotations = new ArrayList<>();
			}
			node.visibleAnnotations.add(AnnotationReader.accept(annotation));
		}
		org.objectweb.asm.Type createdType = org.objectweb.asm.Type.getType(header.desc);
		org.objectweb.asm.Type originalType = org.objectweb.asm.Type.getType(this.member.getType());

		if (this.impl) {
			if (Modifier.isStatic(node.access)) {
				node.visitVarInsn(createdType.getOpcode(ILOAD), 0);
				node.visitFieldInsn(PUTSTATIC, owner, this.member.getName(), originalType.getDescriptor());
			} else {
				this.abstracter.castToMinecraft(node, visitor -> visitor.visitVarInsn(ALOAD, 0), AbstractAbstracter.Location.THIS);
				this.cast(AbstractAbstracter.Location.PARAMETER, createdType, originalType, node, v -> v.visitVarInsn(createdType.getOpcode(ILOAD), 1));
				node.visitFieldInsn(PUTFIELD, owner, this.member.getName(), originalType.getDescriptor());
			}
		} else {
			AbstractAbstracter.visitStub(node);
		}
		node.visitInsn(RETURN);
		node.visitParameter(header.name, 0);
		return node;
	}

	public String getEtterName(String prefix, String name) {
		return prefix + Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}
}
