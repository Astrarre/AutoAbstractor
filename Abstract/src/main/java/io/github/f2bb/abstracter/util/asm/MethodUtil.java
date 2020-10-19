package io.github.f2bb.abstracter.util.asm;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class MethodUtil {
	public static MethodNode findOrCreateMethod(int access, ClassNode node, String name, String desc) {
		for (MethodNode method : node.methods) {
			if (name.equals(method.name) && desc.equals(method.desc)) {
				return method;
			}
		}
		MethodNode method = new MethodNode(access, name, desc, null, null);
		node.methods.add(method);
		return method;
	}
}
