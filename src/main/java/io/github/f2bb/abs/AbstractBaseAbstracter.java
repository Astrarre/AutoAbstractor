package io.github.f2bb.abs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import io.github.f2bb.Abstracter;
import io.github.f2bb.utils.Identifier;
import io.github.f2bb.utils.view.ClassView;
import io.github.f2bb.utils.view.FieldView;
import io.github.f2bb.utils.view.MethodView;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public abstract class AbstractBaseAbstracter implements Opcodes {
	public static final int FILTER = ACC_BRIDGE | ACC_PRIVATE | ACC_SYNTHETIC;
	protected final ClassNode node;
	protected final Abstracter abstracter;

	protected AbstractBaseAbstracter(ClassNode node, Abstracter abstracter) {
		this.node = node;
		this.abstracter = abstracter;
		node.innerClasses.forEach(i -> this.visitInnerClass(i.name, i.outerName, i.innerName, i.access));
		this.visitOuterClass(node.outerClass);

		for (MethodView method : this.getAllInheritedMethods(node.name)) {
			if (abstracter.shouldAbstractMethod(method.viewer.node.name, method.node.name, method.node.desc)) {
				MethodVisitor visitor = this.writeMethod(method);
				method.node.accept(visitor);
			}

		}
		for (FieldView field : this.getAllInheritedFields(node.name)) {
			if (abstracter.shouldAbstractField(field.viewer.node.name, field.node.name, field.node.desc)) {
				FieldVisitor visitor = this.writeField(field);
				field.node.accept(new ClassVisitor(ASM9) {
					@Override
					public FieldVisitor visitField(int access,
							String name,
							String descriptor,
							String signature,
							Object value) {
						return visitor;
					}
				});
			}
		}
	}

	/**
	 * @return visitor for annotations and parameters
	 */
	protected abstract MethodVisitor writeMethod(MethodView view);

	protected abstract FieldVisitor writeField(FieldView view);

	protected abstract void visitInnerClass(String name, String outerName, String innerName, int access);

	protected abstract void visitOuterClass(@Nullable String owner);

	public Collection<MethodView> getAllInheritedMethods(String clsName) {
		Map<Identifier, MethodView> views = new HashMap<>();
		for (ClassView view : this.walkInheritance(clsName)) {
			for (MethodNode method : view.node.methods) {
				views.put(new Identifier(method.name, method.desc), new MethodView(this.abstracter, view, method));
			}
		}
		Collection<MethodView> methodViews = views.values();
		methodViews.removeIf(m -> ((FILTER & m.node.access) != 0));
		return methodViews;
	}

	public Collection<FieldView> getAllInheritedFields(String internalName) {
		List<FieldView> views = new ArrayList<>();
		for (ClassView view : this.walkInheritance(internalName)) {
			for (FieldNode field : view.node.fields) {
				views.add(new FieldView(this.abstracter, view, field));
			}
		}
		views.removeIf(m -> ((FILTER & m.node.access) != 0));
		return views;
	}

	// super class -> sub class order
	// interface -> super class -> sub class order
	// sub class -> super class -> interface order and then reverse
	// interfaces, super, super, super, class
	// class of clsName will be the last in the list
	// (hint: method override order)
	public List<ClassView> walkInheritance(String clsName) {
		List<ClassView> nodes = new ArrayList<>();
		this.walkInheritance(new HashSet<>(), nodes, null, clsName, 0);
		return Lists.reverse(nodes);
	}

	private void walkInheritance(Set<String> interfaceDuplicates,
			List<ClassView> nodes,
			ClassView viewer,
			String clsName,
			int index) {
		if (interfaceDuplicates.add(clsName)) {
			ClassNode node = this.abstracter.getClass(clsName);
			if (node != null) {
				ClassView view = new ClassView(this.abstracter, viewer, node, index);
				// add this
				nodes.add(view);
				// visit super
				this.walkInheritance(interfaceDuplicates, nodes, view, node.superName, 0);
				// visit interfaces
				List<String> interfaces = node.interfaces;
				for (int i = 0; i < interfaces.size(); i++) {
					String itface = interfaces.get(i);
					this.walkInheritance(interfaceDuplicates, nodes, view, itface, i + 1);
				}
			}
		}
	}
}
