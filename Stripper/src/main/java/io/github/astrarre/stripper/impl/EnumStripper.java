package io.github.astrarre.stripper.impl;

import java.lang.reflect.Modifier;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import io.github.astrarre.stripper.AbstractProcessor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class EnumStripper implements AbstractProcessor {
	@Override
	public boolean apply(CompilationUnit unit, TypeDeclaration<?> primary) {
		for (EnumDeclaration declaration : unit.findAll(EnumDeclaration.class)) {
			declaration.getConstructors().clear();
			for (EnumConstantDeclaration dec : declaration.findAll(EnumConstantDeclaration.class)) {
				dec.setArguments(new NodeList<>());
			}
		}
		return false;
	}

	@Override
	public boolean apply(ClassNode node) {
		if((node.access & ACC_ENUM) != 0) {
			// delete constructors
			node.methods.removeIf(method -> method.name.equals("<init>"));
			// add no-arg ctor
			MethodNode replace = new MethodNode(0, "<init>", "()V", null, null);
			node.methods.add(replace);
		}
		// todo find out if u need a static initializer
		return false;
	}
}
