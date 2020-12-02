package io.github.astrarre.stripper.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * fixes enum constructors, runs after method stripper
 */
public class EnumStripper implements Opcodes {
	/**
	 * since this runs after method stripper, the constructor and static initializers were deleted
	 */
	public static void strip(ClassNode node) {
		if(AsmUtil.is(node.access, ACC_ENUM)) {
			MethodNode init = new MethodNode(0, "<init>", "()V", null, null);
			AsmUtil.visitStub(init);

			MethodNode clinit = new MethodNode(ACC_PUBLIC | ACC_STATIC, "<clinit>", "()V", null, null);
			node.methods.add(clinit);

			for (FieldNode field : node.fields) {
				if(AsmUtil.is(field.access, ACC_ENUM)) {
					clinit.visitTypeInsn(NEW, node.name);
					clinit.visitInsn(DUP);
					clinit.visitMethodInsn(INVOKESPECIAL, node.name, "<init>", "()V", false);
					clinit.visitFieldInsn(PUTSTATIC, node.name, field.name, field.desc);
				}
			}
		}
	}
}
