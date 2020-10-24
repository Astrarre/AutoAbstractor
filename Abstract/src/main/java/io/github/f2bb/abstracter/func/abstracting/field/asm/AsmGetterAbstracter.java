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

public class AsmGetterAbstracter implements FieldAbstracter<ClassNode> {
	private final boolean iface;

	public AsmGetterAbstracter(boolean iface) {this.iface = iface;}

	@Override
	public void abstractField(ClassNode header, Class<?> abstracting, Field field, boolean impl) {
		int access = field.getModifiers();
		String owner = Type.getInternalName(field.getType());
		TypeToken<?> token = TypeToken.of(abstracting);
		String descriptor = Type.getDescriptor(token.getRawType());
		String name = field.getName();
		String signature = TypeUtil.toSignature(token.getType());
		if(this.iface) {
			access &= ~ACC_FINAL;
		} else {
			access |= ACC_FINAL;
		}
		MethodNode node = new MethodNode(access, FieldAbstraction.getEtterName("get", descriptor, name),
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
			node.visitInsn(Type.getType(descriptor).getOpcode(IRETURN));
		} else {
			InvokeUtil.visitStub(node);
		}
		header.methods.add(node);
	}
}
