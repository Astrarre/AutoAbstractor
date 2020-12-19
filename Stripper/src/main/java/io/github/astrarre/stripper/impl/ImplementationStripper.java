package io.github.astrarre.stripper.impl;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import io.github.astrarre.stripper.AbstractProcessor;
import org.objectweb.asm.tree.ClassNode;

public class ImplementationStripper extends VoidVisitorAdapter<Void> implements AbstractProcessor {
	private static final BlockStmt EMPTY = StaticJavaParser.parseBlock("{throw new RuntimeException();}");

	@Override
	public boolean apply(CompilationUnit unit, TypeDeclaration<?> primary) {
		unit.accept(this, null);
		return false;
	}

	// code attributes aren't even parsed, so nothing to do here
	@Override public boolean apply(ClassNode node) {return false;}

	@Override
	public void visit(ConstructorDeclaration n, Void arg) {
		n.setBody(EMPTY);
		super.visit(n, arg);
	}

	@Override
	public void visit(FieldDeclaration n, Void arg) {
		for (VariableDeclarator declarator : n.findAll(VariableDeclarator.class)) {
			if(n.hasModifier(Modifier.Keyword.FINAL)) {
				declarator.setInitializer("Impl.init()");
			} else {
				declarator.removeInitializer();
			}
		}
		super.visit(n, arg);
	}

	@Override
	public void visit(MethodDeclaration n, Void arg) {
		n.setBody(EMPTY);
		super.visit(n, arg);
	}
}
