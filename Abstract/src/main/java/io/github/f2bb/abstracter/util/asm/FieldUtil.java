package io.github.f2bb.abstracter.util.asm;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.google.common.reflect.TypeToken;
import io.github.f2bb.abstracter.func.map.TypeMappingFunction;
import io.github.f2bb.abstracter.util.reflect.TypeUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

@SuppressWarnings ("UnstableApiUsage")
public class FieldUtil implements Opcodes {
	// todo annotations
	public static MethodNode createGetter(Class<?> cls, Field field, boolean impl) {
		int access = field.getModifiers();
		String owner = org.objectweb.asm.Type.getInternalName(field.getType());
		TypeToken<?> token = TypeToken.of(cls).resolveType(field.getGenericType());
		String descriptor = org.objectweb.asm.Type.getDescriptor(token.getRawType());
		String name = field.getName();
		String signature = TypeUtil.toSignature(token.getType());
		MethodNode node = new MethodNode(access, getEtterName("get", name),
				"()" + descriptor,
				signature.equals(descriptor) ? null : "()" + signature,
				null);
		if (impl) {
			if (Modifier.isStatic(access)) {
				node.visitFieldInsn(GETSTATIC, owner, name, descriptor);
			} else {
				node.visitVarInsn(ALOAD, 0);
				node.visitFieldInsn(GETFIELD, owner, name, descriptor);
			}
			node.visitInsn(org.objectweb.asm.Type.getType(descriptor).getOpcode(IRETURN));
		} else {
			InvokeUtil.visitStub(node);
		}

		return node;
	}

	public static MethodNode createSetter(Class<?> cls, Field field, boolean impl) {
		int access = field.getModifiers();
		String owner = Type.getInternalName(field.getType());
		TypeToken<?> token = TypeToken.of(cls);
		String descriptor = Type.getDescriptor(token.getRawType());
		String name = field.getName();
		String signature = TypeUtil.toSignature(token.getType());
		MethodNode node = new MethodNode(access,
				getEtterName("set", name),
				"(" + descriptor + ")V",
				signature.equals(descriptor) ? null : "(" + signature + ")V",
				null);
		Type type = Type.getType(descriptor);
		if (impl) {
			if (Modifier.isStatic(access)) {
				node.visitVarInsn(type.getOpcode(ILOAD), 0);
				node.visitFieldInsn(PUTSTATIC, owner, name, descriptor);
			} else {
				node.visitVarInsn(ALOAD, 0);
				node.visitVarInsn(type.getOpcode(ILOAD), 1);
				if (!Type.getDescriptor(field.getType()).equals(descriptor)) {
					node.visitTypeInsn(CHECKCAST, Type.getInternalName(field.getType()));
				}
				node.visitFieldInsn(PUTFIELD, owner, name, descriptor);
			}
		} else {
			InvokeUtil.visitStub(node);
		}
		node.visitInsn(RETURN);
		node.visitParameter(name, ACC_FINAL);
		return node;
	}

	public static FieldNode createConstant(ClassNode header, Class<?> cls, Field field, boolean impl) {
		java.lang.reflect.Type reified = TypeMappingFunction.reify(cls, field.getGenericType());
		FieldNode node = new FieldNode(field.getModifiers(),
				field.getName(),
				TypeUtil.getInterfaceDesc(TypeMappingFunction.raw(cls, field.getGenericType())),
				TypeUtil.toSignature(reified),
				null);
		if (impl) {
			MethodNode init = MethodUtil.findOrCreateMethod(ACC_STATIC | ACC_PUBLIC, header, "<clinit>", "()V");
			InsnList list = init.instructions;
			if(list.getLast() == null) {
				list.insert(new InsnNode(RETURN));
			}

			InsnList insn = new InsnList();
			insn.add(new FieldInsnNode(GETSTATIC,
					Type.getInternalName(field.getDeclaringClass()),
					field.getName(),
					Type.getDescriptor(field.getType())));
			insn.add(new FieldInsnNode(PUTSTATIC, header.name, node.name, node.desc));
			list.insert(insn);
		}

		return node;
	}

	public static String getEtterName(String prefix, String name) {
		return prefix + Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}
}
