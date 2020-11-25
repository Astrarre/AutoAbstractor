package io.github.astrarre.abstracter.util.asm;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import com.google.common.reflect.TypeToken;
import io.github.astrarre.Impl;
import io.github.astrarre.abstracter.func.map.TypeMappingFunction;
import io.github.astrarre.abstracter.util.AnnotationReader;
import io.github.astrarre.abstracter.util.reflect.TypeUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

@SuppressWarnings ("UnstableApiUsage")
public class FieldUtil implements Opcodes {
	// todo annotations
	public static MethodNode createGetter(Class<?> cls, Field field, boolean impl, boolean iface) {
		int access = field.getModifiers();
		access &= ~ACC_ENUM;
		String owner = org.objectweb.asm.Type.getInternalName(field.getDeclaringClass());
		TypeToken<?> token = TypeToken.of(cls).resolveType(field.getGenericType());
		String descriptor = TypeUtil.getInterfaceDesc(token.getRawType());
		String name = field.getName();
		String signature = TypeUtil.toSignature(token.getType());
		MethodNode node = new MethodNode(access,
				getEtterName("get", name),
				"()" + descriptor,
				signature.equals(descriptor) ? null : "()" + signature,
				null);
		for (Annotation annotation : field.getAnnotations()) {
			if(node.visibleAnnotations == null) node.visibleAnnotations = new ArrayList<>();
			node.visibleAnnotations.add(AnnotationReader.accept(annotation));
		}
		if (impl) {
			if (Modifier.isStatic(access)) {
				node.visitFieldInsn(GETSTATIC, owner, name, descriptor);
			} else {
				node.visitVarInsn(ALOAD, 0);
				if(iface) {
					node.visitTypeInsn(CHECKCAST, owner);
				}
				node.visitFieldInsn(GETFIELD, owner, name, descriptor);
			}
			node.visitInsn(org.objectweb.asm.Type.getType(descriptor).getOpcode(IRETURN));
		} else {
			InvokeUtil.visitStub(node);
		}

		return node;
	}

	public static String getEtterName(String prefix, String name) {
		return prefix + Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	public static MethodNode createSetter(Class<?> cls, Field field, boolean impl, boolean iface) {
		int access = field.getModifiers();
		access &= ~ACC_ENUM;
		String owner = Type.getInternalName(field.getDeclaringClass());
		TypeToken<?> token = TypeToken.of(cls).resolveType(field.getGenericType());
		String descriptor = TypeUtil.getInterfaceDesc(token.getRawType());
		String name = field.getName();
		String signature = TypeUtil.toSignature(token.getType());
		MethodNode node = new MethodNode(access,
				getEtterName("set", name),
				"(" + descriptor + ")V",
				signature.equals(descriptor) ? null : "(" + signature + ")V",
				null);
		for (Annotation annotation : field.getAnnotations()) {
			if(node.visibleAnnotations == null) node.visibleAnnotations = new ArrayList<>();
			node.visibleAnnotations.add(AnnotationReader.accept(annotation));
		}
		Type type = Type.getType(descriptor);
		if (impl) {
			if (Modifier.isStatic(access)) {
				node.visitVarInsn(type.getOpcode(ILOAD), 0);
				node.visitFieldInsn(PUTSTATIC, owner, name, descriptor);
			} else {
				node.visitVarInsn(ALOAD, 0);
				if(iface) {
					node.visitTypeInsn(CHECKCAST, owner);
				}
				node.visitVarInsn(type.getOpcode(ILOAD), 1);
				if (!Type.getDescriptor(field.getType()).equals(descriptor)) {
					node.visitTypeInsn(CHECKCAST, Type.getInternalName(field.getDeclaringClass()));
				}
				node.visitFieldInsn(PUTFIELD, owner, name, descriptor);
			}
		} else {
			InvokeUtil.visitStub(node);
		}
		node.visitInsn(RETURN);
		node.visitParameter(name, 0);
		return node;
	}

	public static void createConstant(ClassNode header, Class<?> cls, Field field, boolean impl) {
		java.lang.reflect.Type reified = TypeMappingFunction.reify(cls, field.getGenericType());
		FieldNode node = new FieldNode(field.getModifiers() & ~ACC_ENUM,
				field.getName(),
				TypeUtil.getInterfaceDesc(TypeMappingFunction.raw(cls, field.getGenericType())),
				TypeUtil.toSignature(reified),
				null);

		if(Modifier.isStatic(node.access)) {
			MethodNode init = MethodUtil.findOrCreateMethod(ACC_STATIC | ACC_PUBLIC, header, "<clinit>", "()V");
			InsnList list = init.instructions;
			if (list.getLast() == null) {
				list.insert(new InsnNode(RETURN));
			}

			InsnList insn = new InsnList();
			if (impl) {
				insn.add(new FieldInsnNode(GETSTATIC,
						Type.getInternalName(field.getDeclaringClass()),
						field.getName(),
						Type.getDescriptor(field.getType())));
			} else {
				insn.add(new MethodInsnNode(INVOKESTATIC,
						Type.getInternalName(Impl.class),
						Impl.FIELD_INIT,
						"()Ljava/lang/Object;"));
			}
			insn.add(new FieldInsnNode(PUTSTATIC, header.name, node.name, node.desc));
			list.insert(insn);
		}
		header.fields.add(node);
	}
}
