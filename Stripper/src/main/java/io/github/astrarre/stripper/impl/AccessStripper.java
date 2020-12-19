package io.github.astrarre.stripper.impl;

import java.util.Iterator;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import io.github.astrarre.stripper.AbstractProcessor;
import io.github.astrarre.stripper.Hide;
import io.github.astrarre.stripper.asm.AsmUtil;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class AccessStripper implements AbstractProcessor {
	@Override
	public boolean apply(CompilationUnit unit, TypeDeclaration<?> primary) {
		if (!canSee(primary)) {
			// private/package-private class
			return true;
		}

		for (Modifier modifier : unit.findAll(Modifier.class)) {
			modifier.getParentNode()
			        .map(NodeWithModifiers.class::cast)
			        .filter(AccessStripper::canSee)
			        .map(Node.class::cast)
			        .ifPresent(Node::remove);
		}
		return false;
	}

	public static boolean canSee(NodeWithModifiers<?> node) {
		return node.hasModifier(Modifier.Keyword.PROTECTED) || node.hasModifier(Modifier.Keyword.PUBLIC);
	}

	public static boolean canSee(int access) {
		return ((ACC_PUBLIC | ACC_PROTECTED) & access) != 0;
	}

	@Override
	public boolean apply(ClassNode node) {
		if(!canSee(node.access)) {
			return true;
		}

		Iterator<MethodNode> iterator = node.methods.iterator();
		while (iterator.hasNext()) {
			MethodNode method = iterator.next();
			if (!canSee(node.access)) {
				iterator.remove();
			} else {
				method.instructions.clear();
				AsmUtil.visitStub(method);
			}
		}

		node.fields.removeIf(field -> !canSee(field.access));
		return false;
	}
}
