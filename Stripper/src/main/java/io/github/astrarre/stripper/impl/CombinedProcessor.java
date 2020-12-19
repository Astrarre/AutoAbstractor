package io.github.astrarre.stripper.impl;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import io.github.astrarre.stripper.AbstractProcessor;
import org.objectweb.asm.tree.ClassNode;

public class CombinedProcessor implements AbstractProcessor {
	private final List<AbstractProcessor> processors = new ArrayList<>();

	public CombinedProcessor add(AbstractProcessor processor) {
		this.processors.add(processor);
		return this;
	}

	@Override
	public boolean apply(CompilationUnit unit, TypeDeclaration<?> primary) {
		for (AbstractProcessor processor : processors) {
			if (processor.apply(unit, primary)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean apply(ClassNode node) {
		for (AbstractProcessor processor : this.processors) {
			if (processor.apply(node)) {
				return true;
			}
		}
		return false;
	}
}
