package io.github.astrarre.stripper.asm;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;

public class AsmUtil implements Opcodes {

	private static final String RUNTIME_EXCEPTION = Type.getInternalName(RuntimeException.class);

	public static boolean hasAnnotation(List<AnnotationNode> nodes, Class<? extends Annotation> anno) {
		String desc = Type.getDescriptor(anno);
		if (nodes == null) {
			return false;
		}
		for (AnnotationNode node : nodes) {
			if (desc.equals(node.desc)) {
				return true;
			}
		}
		return false;
	}

	public static void visitStub(MethodNode visitor) {
		if (!Modifier.isAbstract(visitor.access)) {
			int opcode = Type.getMethodType(visitor.desc).getReturnType().getOpcode(IRETURN);
			if (opcode != RETURN) {
				visitor.visitTypeInsn(NEW, RUNTIME_EXCEPTION);
				visitor.visitInsn(DUP);
				visitor.visitMethodInsn(INVOKESPECIAL, RUNTIME_EXCEPTION, "<init>", "()V", false);
				visitor.visitInsn(ATHROW);
			} else {
				visitor.visitInsn(opcode);
			}
		}
	}
}
