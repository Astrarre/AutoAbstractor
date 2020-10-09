package io.github.f2bb.abs;

import io.github.f2bb.Abstracter;
import io.github.f2bb.utils.view.FieldView;
import io.github.f2bb.utils.view.MethodView;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;

public class ImplAsmBaseAbstracter extends AbstractBaseAbstracter {
	protected ImplAsmBaseAbstracter(ClassNode node, Abstracter abstracter) {
		super(node, abstracter);
	}

	@Override
	protected MethodVisitor writeMethod(MethodView view) {
		return null;
	}

	@Override
	protected FieldVisitor writeField(FieldView view) {
		return null;
	}
}
