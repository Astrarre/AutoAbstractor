package io.github.astrarre.stripper;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public interface AbstractProcessor extends Opcodes {
	/**
	 * strip implementation from java source code
	 * @return true to skip the class entirely
	 * @param primary the root class of this file
	 */
	boolean apply(CompilationUnit unit, TypeDeclaration<?> primary);

	/**
	 * strip implementation from java bytecode
	 * @return true to skip the class entirely
	 */
	boolean apply(ClassNode node);
}
