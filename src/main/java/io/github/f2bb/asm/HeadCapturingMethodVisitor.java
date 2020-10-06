package io.github.f2bb.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;

public class HeadCapturingMethodVisitor extends MethodVisitor {
	private final MethodVisitor visitor;

	public HeadCapturingMethodVisitor(MethodVisitor visitor) {
		super(Opcodes.ASM9);
		this.visitor = visitor;
	}

	@Override
	public void visitParameter(String name, int access) {
		this.visitor.visitParameter(name, access);
	}

	@Override
	public AnnotationVisitor visitAnnotationDefault() {
		return this.visitor.visitAnnotationDefault();
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		return this.visitor.visitAnnotation(descriptor, visible);
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		return this.visitor.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
	}

	@Override
	public void visitAnnotableParameterCount(int parameterCount, boolean visible) {
		this.visitor.visitAnnotableParameterCount(parameterCount, visible);
	}

	@Override
	public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
		return this.visitor.visitParameterAnnotation(parameter, descriptor, visible);
	}

	@Override
	public void visitAttribute(Attribute attribute) {
		this.visitor.visitAttribute(attribute);
	}

	@Override
	public void visitCode() {
		this.visitor.visitCode();
	}

	@Override
	public void visitEnd() {
		this.visitor.visitEnd();
	}
}
