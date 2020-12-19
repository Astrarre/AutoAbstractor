package io.github.astrarre.stripper.asm;

import java.lang.reflect.Modifier;
import java.util.List;

import io.github.astrarre.stripper.Hide;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;

public class AsmUtil implements Opcodes {

	public static final String HIDE = Type.getDescriptor(Hide.class);
	public static boolean isVisible(int access) {
		return Modifier.isPublic(access) || Modifier.isProtected(access);
	}

	public static boolean isHidden(List<AnnotationNode> nodes) {
		if(nodes == null) return false;
		for (AnnotationNode node : nodes) {
			if(node.desc.equals(HIDE)) {
				return true;
			}
		}
		return false;
	}

	public static boolean is(int access, int flag) {
		return (access & flag) != 0;
	}

	private static final String RUNTIME_EXCEPTION = Type.getInternalName(RuntimeException.class);
	public static void visitStub(MethodNode visitor) {
		if(!Modifier.isAbstract(visitor.access)) {
			int opcode = Type.getMethodType(visitor.desc).getReturnType().getOpcode(IRETURN);
			if(opcode != RETURN) {
				visitor.visitTypeInsn(NEW, RUNTIME_EXCEPTION);
				visitor.visitInsn(DUP);
				visitor.visitMethodInsn(INVOKESPECIAL, RUNTIME_EXCEPTION, "<init>", "()V", false);
				visitor.visitInsn(ATHROW);
			}
			visitor.visitInsn(opcode);
		}
	}
}
