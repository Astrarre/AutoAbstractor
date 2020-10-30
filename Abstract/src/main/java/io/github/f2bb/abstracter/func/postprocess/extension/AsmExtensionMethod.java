package io.github.f2bb.abstracter.func.postprocess.extension;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import io.github.f2bb.abstracter.func.postprocess.PostProcessor;
import io.github.f2bb.abstracter.util.ReflectUtil;
import io.github.f2bb.abstracter.util.asm.InvokeUtil;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

// todo exceptions maybe
public class AsmExtensionMethod implements PostProcessor, Opcodes {
	private final int access;
	private final Method method;
	@Nullable
	private final String comment;

	public AsmExtensionMethod(int access, Method method, @Nullable String comment) {
		this.access = access;
		this.method = method;
		this.comment = comment;
	}

	@Override
	public void processAsm(ClassNode header, Class<?> cls, boolean impl) {
		MethodNode node = new MethodNode(this.access, this.method.getName(), Type.getMethodDescriptor(this.method),
				impl ? null : ReflectUtil.getSignature(this.method), null);
		if (!impl) {
			for (String parameter : this.parameters) {
				node.visitParameter(parameter, 0);
			}
		}

		if (impl) {
			Type type = Type.getMethodType(null);
			int i = 0;
			if (!Modifier.isStatic(this.access)) {
				node.visitVarInsn(ALOAD, i++);
			}

			Type[] types = type.getArgumentTypes();
			for (int length = types.length; i < length; i++) {
				node.visitVarInsn(types[i].getOpcode(ILOAD), i);
			}

			node.visitMethodInsn(this.targetOpcode,
					this.targetClass,
					this.targetName,
					this.desc,
					this.targetOpcode == INVOKEINTERFACE);

			node.visitInsn(type.getReturnType().getOpcode(IRETURN));
		} else {
			InvokeUtil.visitStub(node);
		}
	}

	@Override
	public void processJava(TypeSpec.Builder header, Class<?> cls, boolean impl) {
		MethodSpec.Builder builder = MethodSpec.methodBuilder(this.name);

	}
}
