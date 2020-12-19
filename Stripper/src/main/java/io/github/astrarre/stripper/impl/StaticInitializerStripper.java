package io.github.astrarre.stripper.impl;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import io.github.astrarre.stripper.AbstractProcessor;
import org.objectweb.asm.tree.ClassNode;

public class StaticInitializerStripper implements AbstractProcessor {
	@Override
	public boolean apply(CompilationUnit unit, TypeDeclaration<?> primary) {
		for (InitializerDeclaration declaration : unit.findAll(InitializerDeclaration.class)) {
			declaration.remove();
		}
		return false;
	}

	@Override
	public boolean apply(ClassNode node) {
		node.methods.removeIf(method -> method.name.equals("<clinit>"));
		return false;
	}
}
