package io.github.astrarre.stripper.asm;

import java.util.Iterator;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * converts method implementations to stubs, hides private methods and fields, and strips explicitly stripped methods, deletes static initializers
 */
public class ElementStripper {
	public static void strip(ClassNode node) {
		Iterator<MethodNode> iterator = node.methods.iterator();
		while (iterator.hasNext()) {
			MethodNode method = iterator.next();
			if (!(AsmUtil.isVisible(method.access) || AsmUtil.isHidden(method.invisibleAnnotations)) || method.name.equals("<clinit>")) {
				iterator.remove();
			} else {
				method.instructions.clear();
				AsmUtil.visitStub(method);
			}
		}

		node.fields.removeIf(field -> !(AsmUtil.isVisible(field.access) || AsmUtil.isHidden(field.invisibleAnnotations)));
	}
}
