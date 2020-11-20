package io.github.f2bb.stripper.impl;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import io.github.f2bb.Impl;
import io.github.f2bb.stripper.StripSource;

public class Replacer extends ModifierVisitor<Void> {
	BlockStmt stmt = StaticJavaParser.parseBlock("{throw Impl.call();}");

	@Override
	public Visitable visit(ConstructorDeclaration n, Void arg) {
		if (StripSource.expose(n) || !n.getParentNode().map(a -> (TypeDeclaration) a).filter(TypeDeclaration::isEnumDeclaration)
		                               .isPresent()) {
			n.tryAddImportToParentCompilationUnit(Impl.class);
			n.setBody(this.stmt);
		} else {
			n.remove();
		}
		return super.visit(n, arg);
	}

	@Override
	public Visitable visit(EnumConstantDeclaration n, Void arg) {
		n.setArguments(new NodeList<>());
		return super.visit(n, arg);
	}

	@Override
	public Visitable visit(FieldDeclaration n, Void arg) {
		if (StripSource.expose(n)) {
			if (n.isStatic()) {
				boolean visit = false;
				for (VariableDeclarator variable : n.getVariables()) {
					variable.setInitializer("Impl.init()");
					visit = true;
				}
				if (visit) {
					n.tryAddImportToParentCompilationUnit(Impl.class);
				}
			} else {
				for (VariableDeclarator variable : n.getVariables()) {
					variable.setInitializer((Expression) null);
				}
			}
		} else {
			n.remove();
		}
		return super.visit(n, arg);
	}

	@Override
	public Visitable visit(InitializerDeclaration n, Void arg) {
		n.tryAddImportToParentCompilationUnit(Impl.class);
		n.setBody(this.stmt);
		return super.visit(n, arg);
	}

	@Override
	public Visitable visit(MethodDeclaration n, Void arg) {
		if (StripSource.expose(n)) {
			if (n.getBody().isPresent()) {
				n.tryAddImportToParentCompilationUnit(Impl.class);
				n.setBody(this.stmt);
			}
		} else {
			n.remove();
		}

		return super.visit(n, arg);
	}
}
