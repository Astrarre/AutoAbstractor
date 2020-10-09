package io.github.f2bb.utils;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class Abstracter {

	@Nullable
	public ClassNode getClass(String clsName) {
		// todo impl
		return null;
	}

	// super class -> sub class order
	// interface -> super class -> sub class order
	// sub class -> super class -> interface order and then reverse
	public List<ClassNode> walkInheritance(String clsName) {
		List<ClassNode> nodes = new ArrayList<>();
		this.walkInheritance(nodes, clsName);
		return Lists.reverse(nodes);
	}

	private void walkInheritance(List<ClassNode> nodes, String name) {
		ClassNode n = this.getClass(name);
		if (n != null) {
			nodes.add(n);
			this.walkInheritance(nodes, n.superName);

			for (String anInterface : n.interfaces) {
				this.walkInheritance(nodes, anInterface);
			}
		}

	}

	public List<MethodNode> getMethods(String clsName) {
		Map<String, MethodNode> descToNode = new HashMap<>();
		for (ClassNode node : this.walkInheritance(clsName)) {

		}
		return null;
	}

	public boolean shouldAbstractDesc(String desc) {
		return true;
	}

	public boolean shouldAbstract(String internalName) {
		return true;
	}
}
