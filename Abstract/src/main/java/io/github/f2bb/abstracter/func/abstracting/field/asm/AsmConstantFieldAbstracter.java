package io.github.f2bb.abstracter.func.abstracting.field.asm;

import java.lang.reflect.Field;

import io.github.f2bb.abstracter.func.abstracting.field.FieldAbstracter;
import io.github.f2bb.abstracter.func.map.TypeMappingFunction;
import io.github.f2bb.abstracter.util.asm.MethodUtil;
import io.github.f2bb.abstracter.util.asm.TypeUtil;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

public class AsmConstantFieldAbstracter implements FieldAbstracter<ClassNode> {
	@Override
	public void abstractField(ClassNode header, Class<?> cls, Field field, boolean impl) {
		java.lang.reflect.Type reified = TypeMappingFunction.reify(cls, field.getGenericType());
		FieldNode node = new FieldNode(field.getModifiers(),
				field.getName(),
				TypeUtil.getInterfaceDesc(TypeMappingFunction.raw(cls, field.getGenericType())),
				TypeUtil.toSignature(reified),
				null);
		header.fields.add(node);
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
	}
}
