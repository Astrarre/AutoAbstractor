package io.github.f2bb.utils.view;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.f2bb.Abstracter;
import io.github.f2bb.utils.AbstracterImpl;
import io.github.f2bb.utils.types.Arguments;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.ClassNode;

public class ClassView {
	public final Optional<ClassView> viewer;
	public final ClassNode node;
	/**
	 * the type arguments for all of the classes this class extends/implements
	 */
	public final List<Arguments> args;
	/**
	 * a mapping from type variable -> type parameter from this class's viewer's view
	 */
	public final Map<String, String> typeParameterMap;

	public ClassView(Abstracter abstracter, @Nullable ClassView viewer, ClassNode node, int index) {
		this.viewer = Optional.ofNullable(viewer);
		this.node = node;
		this.args = Arguments.get(node.signature);
		List<List<String>> typeParameters = Arguments.getTypeDeclarations(abstracter, node.name);
		if (viewer != null) {
			Arguments viewedArguments = viewer.args.get(index);
			this.typeParameterMap = Arguments.createArgumentMapping(typeParameters, viewedArguments);
		} else {
			this.typeParameterMap = Collections.emptyMap();
		}
	}

	public int getTrueAccess() {
		return this.viewer.map(c -> c.node)
				       .map(c -> c.innerClasses)
				       .map(List::stream)
				       .flatMap(s -> s.filter(i -> i.name.equals(this.node.name)).findFirst())
				       .map(i -> i.access)
				       .orElse(this.node.access);
	}

	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.node.visit(version, access, name, signature, superName, interfaces);
	}

	public void visitSource(String file, String debug) {
		this.node.visitSource(file, debug);
	}

	public ModuleVisitor visitModule(String name, int access, String version) {
		return this.node.visitModule(name, access, version);
	}

	public void visitNestHost(String nestHost) {
		this.node.visitNestHost(nestHost);
	}

	public void visitOuterClass(String owner, String name, String descriptor) {
		this.node.visitOuterClass(owner, name, descriptor);
	}

	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		return this.node.visitAnnotation(descriptor, visible);
	}

	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		return this.node.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
	}

	public void visitAttribute(Attribute attribute) {
		this.node.visitAttribute(attribute);
	}

	public void visitNestMember(String nestMember) {
		this.node.visitNestMember(nestMember);
	}

	public void visitPermittedSubclass(String permittedSubclass) {
		this.node.visitPermittedSubclass(permittedSubclass);
	}

	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		this.node.visitInnerClass(name, outerName, innerName, access);
	}

	public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
		return this.node.visitRecordComponent(name, descriptor, signature);
	}

	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		return this.node.visitField(access, name, descriptor, signature, value);
	}

	public MethodVisitor visitMethod(int access,
			String name,
			String descriptor,
			String signature,
			String[] exceptions) {
		return this.node.visitMethod(access, name, descriptor, signature, exceptions);
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
}
