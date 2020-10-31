package io.github.f2bb.abstracter.func.postprocess.extension;

import java.lang.reflect.Modifier;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import io.github.f2bb.ImplementationHiddenException;
import io.github.f2bb.abstracter.func.postprocess.PostProcessor;
import io.github.f2bb.abstracter.util.asm.InvokeUtil;
import io.github.f2bb.abstracter.util.java.JavaUtil;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

// todo exceptions maybe
// todo signatures

/**
 * creates a stub method, use a mixin to actually implement the method
 */
public class ExtensionMethod implements PostProcessor, Opcodes {
	private final int access;
	private final String name, desc;
	private final String[] parameters;
	@Nullable private final String comment;

	public ExtensionMethod(int access, String name, String desc, String[] parameters, @Nullable String comment) {
		this.access = access;
		this.name = name;
		this.desc = desc;
		this.parameters = parameters;
		this.comment = comment;
	}

	@Override
	public void processAsm(ClassNode header, Class<?> cls, boolean impl) {
		MethodNode node = new MethodNode(this.access, this.name, this.desc, null, null);
		if (!Modifier.isAbstract(this.access)) {
			InvokeUtil.visitStub(node);
		}
		for (String parameter : this.parameters) {
			node.visitParameter(parameter, 0);
		}
	}

	@Override
	public void processJava(TypeSpec.Builder header, Class<?> cls, boolean impl) {
		MethodSpec.Builder builder = MethodSpec.methodBuilder(this.name);
		Type method = Type.getMethodType(this.desc);
		Type[] types = method.getArgumentTypes();
		for (int i = 0; i < types.length; i++) {
			Type type = types[i];
			builder.addParameter(JavaUtil.toTypeName(type), this.parameters[i]);
		}
		if (!Modifier.isAbstract(this.access)) {
			builder.addCode("throw $T.create();", ImplementationHiddenException.class);
		}
		builder.returns(JavaUtil.toTypeName(method.getReturnType()));
		if (this.comment != null) {
			builder.addJavadoc(this.comment);
		}
	}
}
