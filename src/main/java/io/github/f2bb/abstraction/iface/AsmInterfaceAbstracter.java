package io.github.f2bb.abstraction.iface;

import static io.github.f2bb.util.AbstracterUtil.map;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.common.reflect.TypeToken;
import io.github.f2bb.loader.AbstracterLoader;
import io.github.f2bb.util.AsmUtil;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

@SuppressWarnings ("UnstableApiUsage")
public class AsmInterfaceAbstracter extends AbstractInterfaceAbstracter {
	private final boolean impl;

	public AsmInterfaceAbstracter(AbstracterLoader loader, Class<?> cls, boolean impl) {
		super(loader, cls);
		this.impl = impl;
	}

	private ClassNode node;
	@Override
	public void write(ZipOutputStream out) throws IOException {
		ClassNode node = new ClassNode();
		this.node = node;
		// todo interface
		super.write(out);
		out.putNextEntry(new ZipEntry(node.name + ".class"));
		ClassWriter writer = new ClassWriter(ASM9);
		node.accept(writer);
		out.write(writer.toByteArray());
		out.closeEntry();
	}

	@Override
	public void visitGetter(Field field, TypeToken<?> type) {
		AsmUtil.generateGetter(this.node::visitMethod, this::toSignature, type, field, this.impl);
	}

	@Override
	public void visitSetter(Field field, TypeToken<?> type) {
		AsmUtil.generateSetter(this.node::visitMethod, this::toSignature, type, field, this.impl);
	}

	@Override
	public void visitDelegate(Method method) {
		TypeToken<?>[] params = map(method.getGenericParameterTypes(), this::resolved, TypeToken[]::new);
		TypeToken<?> returnType = this.resolved(method.getGenericReturnType());
		String desc = this.methodDescriptor(params, returnType);
		String sign = this.impl ? null : this.methodSignature(method.getTypeParameters(), params, returnType, true);
		MethodNode node = new MethodNode(method.getModifiers(), method.getName(), desc, sign, null);
		this.invoke(node, method, false);
	}
}
