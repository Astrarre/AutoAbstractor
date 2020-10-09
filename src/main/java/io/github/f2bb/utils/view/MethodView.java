package io.github.f2bb.utils.view;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.MethodNode;

public class MethodView {
	public final ClassView viewer;
	public final MethodNode node;

	public MethodView(ClassView viewer, MethodNode node) {
		this.viewer = viewer;
		this.node = node;
	}

	@Override
	public String toString() {
		return this.viewer.node.name + ";" + this.node.name + ";" + this.node.desc;
	}

	public void visitParameter(String name, int access) {
		this.node.visitParameter(name, access);
	}

	public AnnotationVisitor visitAnnotationDefault() {
		return this.node.visitAnnotationDefault();
	}

	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		return this.node.visitAnnotation(descriptor, visible);
	}

	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		return this.node.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
	}

	public void visitAnnotableParameterCount(int parameterCount, boolean visible) {
		this.node.visitAnnotableParameterCount(parameterCount, visible);
	}

	public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
		return this.node.visitParameterAnnotation(parameter, descriptor, visible);
	}

	public void visitAttribute(Attribute attribute) {
		this.node.visitAttribute(attribute);
	}

	public void visitCode() {
		this.node.visitCode();
	}

	public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
		this.node.visitFrame(type, numLocal, local, numStack, stack);
	}

	public void visitInsn(int opcode) {
		this.node.visitInsn(opcode);
	}

	public void visitIntInsn(int opcode, int operand) {
		this.node.visitIntInsn(opcode, operand);
	}

	public void visitVarInsn(int opcode, int var) {
		this.node.visitVarInsn(opcode, var);
	}

	public void visitTypeInsn(int opcode, String type) {
		this.node.visitTypeInsn(opcode, type);
	}

	public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
		this.node.visitFieldInsn(opcode, owner, name, descriptor);
	}

	public void visitMethodInsn(int opcodeAndSource,
			String owner,
			String name,
			String descriptor,
			boolean isInterface) {
		this.node.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface);
	}

	public void visitInvokeDynamicInsn(String name,
			String descriptor,
			Handle bootstrapMethodHandle,
			Object... bootstrapMethodArguments) {
		this.node.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
	}

	public void visitJumpInsn(int opcode, Label label) {
		this.node.visitJumpInsn(opcode, label);
	}

	public void visitLabel(Label label) {
		this.node.visitLabel(label);
	}

	public void visitLdcInsn(Object value) {
		this.node.visitLdcInsn(value);
	}

	public void visitIincInsn(int var, int increment) {
		this.node.visitIincInsn(var, increment);
	}

	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
		this.node.visitTableSwitchInsn(min, max, dflt, labels);
	}

	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		this.node.visitLookupSwitchInsn(dflt, keys, labels);
	}

	public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
		this.node.visitMultiANewArrayInsn(descriptor, numDimensions);
	}

	public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		return this.node.visitInsnAnnotation(typeRef, typePath, descriptor, visible);
	}

	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		this.node.visitTryCatchBlock(start, end, handler, type);
	}

	public AnnotationVisitor visitTryCatchAnnotation(int typeRef,
			TypePath typePath,
			String descriptor,
			boolean visible) {
		return this.node.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible);
	}

	public void visitLocalVariable(String name,
			String descriptor,
			String signature,
			Label start,
			Label end,
			int index) {
		this.node.visitLocalVariable(name, descriptor, signature, start, end, index);
	}

	public AnnotationVisitor visitLocalVariableAnnotation(int typeRef,
			TypePath typePath,
			Label[] start,
			Label[] end,
			int[] index,
			String descriptor,
			boolean visible) {
		return this.node.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible);
	}

	public void visitLineNumber(int line, Label start) {
		this.node.visitLineNumber(line, start);
	}

	public void visitMaxs(int maxStack, int maxLocals) {
		this.node.visitMaxs(maxStack, maxLocals);
	}

	public void visitEnd() {
		this.node.visitEnd();
	}

	public void check(int api) {
		this.node.check(api);
	}

	public void accept(ClassVisitor classVisitor) {
		this.node.accept(classVisitor);
	}

	public void accept(MethodVisitor methodVisitor) {
		this.node.accept(methodVisitor);
	}

	@Deprecated
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor) {
		this.node.visitMethodInsn(opcode, owner, name, descriptor);
	}
}
