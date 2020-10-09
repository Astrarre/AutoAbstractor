package io.github.f2bb.utils;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import io.github.f2bb.utils.view.ClassView;
import io.github.f2bb.utils.view.MethodView;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class Abstracter implements Opcodes {
	public static final int FILTER = ACC_BRIDGE | ACC_PRIVATE | ACC_SYNTHETIC;
	@Nullable
	public ClassNode getClass(String clsName) {
		// todo impl
		return null;
	}

	public boolean shouldAbstractDesc(String desc) {
		return true;
	}

	public boolean shouldAbstract(String internalName) {
		return true;
	}

	// super class -> sub class order
	// interface -> super class -> sub class order
	// sub class -> super class -> interface order and then reverse

	// interfaces, super, super, super, class
	// class of clsName will be the last in the list
	// (hint: method override order)
	public List<ClassView> walkInheritance(String clsName) {
		List<ClassView> nodes = new ArrayList<>();
		this.walkInheritance(new HashSet<>(),nodes, null, clsName, 0);
		return Lists.reverse(nodes);
	}

	public Collection<MethodView> getMethods(String clsName) {
		Map<Identifier, MethodView> views = new HashMap<>();
		for (ClassView view : this.walkInheritance(clsName)) {
			for (MethodNode method : view.node.methods) {
				views.put(new Identifier(method.name, method.desc), new MethodView(view, method));
			}
		}
		Collection<MethodView> methodViews = views.values();
		methodViews.removeIf(m -> ((FILTER & m.node.access) != 0));
		return methodViews;
	}

	private void walkInheritance(Set<String> interfaceDuplicates, List<ClassView> nodes, ClassView viewer, String clsName, int index) {
		if(interfaceDuplicates.add(clsName)) {
			ClassNode node = this.getClass(clsName);
			if (node != null) {
				ClassView view = new ClassView(this, viewer, node, index);
				// add this
				nodes.add(view);
				// visit super
				this.walkInheritance(interfaceDuplicates, nodes, view, node.superName, 0);
				// visit interfaces
				List<String> interfaces = node.interfaces;
				for (int i = 0; i < interfaces.size(); i++) {
					String itface = interfaces.get(i);
					this.walkInheritance(interfaceDuplicates, nodes, view, itface, i+1);
				}
			}
		}
	}
}
