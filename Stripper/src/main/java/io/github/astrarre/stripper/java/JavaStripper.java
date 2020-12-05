package io.github.astrarre.stripper.java;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import io.github.astrarre.Hide;
import io.github.astrarre.Impl;

public class JavaStripper extends VoidVisitorAdapter<Void> {
	private static final JavaStripper INSTANCE = new JavaStripper();
	private static final BlockStmt EMPTY = StaticJavaParser.parseBlock("{throw Impl.call();}");

	public static void stripAnnotations(CompilationUnit unit) {
		for (MarkerAnnotationExpr expr : unit.findAll(MarkerAnnotationExpr.class)) {
			if(expr.getNameAsString().equals(Hide.class.getSimpleName())) {
				expr.getParentNode().ifPresent(Node::remove);
			}
		}
	}

	public static void stripInaccessibles(CompilationUnit unit) {
		for (Modifier modifier : unit.findAll(Modifier.class)) {
			modifier.getParentNode().map(n -> (NodeWithModifiers<?>)n).filter(n -> !n.hasModifier(Modifier.Keyword.PUBLIC) && !n.hasModifier(
					Modifier.Keyword.PROTECTED)).map(n -> (Node)n).ifPresent(Node::remove);
		}
	}

	public static void nukeImplementation(CompilationUnit unit) {
		unit.accept(INSTANCE, null);
	}

	@Override
	public void visit(ConstructorDeclaration n, Void arg) {
		n.setBody(EMPTY);
		n.tryAddImportToParentCompilationUnit(Impl.class);
		super.visit(n, arg);
	}

	@Override
	public void visit(FieldDeclaration n, Void arg) {
		for (VariableDeclarator declarator : n.findAll(VariableDeclarator.class)) {
			if(n.hasModifier(Modifier.Keyword.FINAL)) {
				declarator.setInitializer("Impl.init()");
				n.tryAddImportToParentCompilationUnit(Impl.class);
			} else {
				declarator.removeInitializer();
			}
		}
		super.visit(n, arg);
	}

	@Override
	public void visit(MethodDeclaration n, Void arg) {
		n.setBody(EMPTY);
		n.tryAddImportToParentCompilationUnit(Impl.class);
		super.visit(n, arg);
	}

	@Override
	public void visit(EnumDeclaration n, Void arg) {
		for (ConstructorDeclaration declaration : n.findAll(ConstructorDeclaration.class)) {
			declaration.remove();
		}
		super.visit(n, arg);
	}

	@Override
	public void visit(EnumConstantDeclaration n, Void arg) {
		n.getArguments().clear();
	}
}
