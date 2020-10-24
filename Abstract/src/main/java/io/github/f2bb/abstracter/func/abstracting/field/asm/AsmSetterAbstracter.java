package io.github.f2bb.abstracter.func.abstracting.field.asm;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.google.common.reflect.TypeToken;
import io.github.f2bb.abstracter.func.abstracting.field.FieldAbstracter;
import io.github.f2bb.abstracter.func.abstracting.field.FieldAbstraction;
import io.github.f2bb.abstracter.util.asm.InvokeUtil;
import io.github.f2bb.abstracter.util.asm.TypeUtil;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class AsmSetterAbstracter implements FieldAbstracter<ClassNode> {
	private final boolean iface;

	public AsmSetterAbstracter(boolean iface) {this.iface = iface;}

	@Override
	public void abstractField(ClassNode header, Class<?> cls, Field field, boolean impl) {
		int access = field.getModifiers();
		String owner = Type.getInternalName(field.getType());
		TypeToken<?> token = TypeToken.of(cls);
		String descriptor = Type.getDescriptor(token.getRawType());
		String name = field.getName();
		String signature = TypeUtil.toSignature(token.getType());
		if (iface) {
			access &= ~ACC_FINAL;
		} else {
			access |= ACC_FINAL;
		}
		MethodNode node = new MethodNode(access,
				FieldAbstraction.getEtterName("set", descriptor, name),
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
		header.methods.add(node);
	}
}
