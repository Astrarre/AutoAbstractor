package io.github.astrarre.stripper.impl;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import io.github.astrarre.stripper.AbstractProcessor;
import org.objectweb.asm.tree.ClassNode;

public class OuterMethodStripper implements AbstractProcessor {
	@Override public boolean apply(CompilationUnit unit, TypeDeclaration<?> primary) {return false;}

	@Override
	public boolean apply(ClassNode node) {
		return node.outerMethodDesc != null;
	}
}
