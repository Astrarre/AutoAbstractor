package io.github.f2bb.abstraction.base;

import static io.github.f2bb.util.AbstracterUtil.add;
import static io.github.f2bb.util.AbstracterUtil.map;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.common.reflect.TypeToken;
import io.github.f2bb.loader.AbstracterLoader;
import io.github.f2bb.util.AsmUtil;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

@SuppressWarnings ("UnstableApiUsage")
public class AsmBaseAbstracter extends AbstractBaseAbstracter {
	private final boolean impl;

	public AsmBaseAbstracter(AbstracterLoader loader, Class<?> toAbstract, boolean impl) {
		super(loader, toAbstract);
		this.impl = impl;
	}

	protected ClassNode node;

	@Override
	public void write(ZipOutputStream out) throws IOException {
		ClassNode node = new ClassNode();
		Class<?> sup = this.findSuper();
		Class<?>[] interfaces = this.getInterfaces();
		node.visit(V1_8,
				// todo instance inner classes
				this.cls.getModifiers(),
				this.loader.getBaseAbstractedName(this.cls),
				this.impl ? null : this.classSignature(this.cls.getTypeParameters(),
						this.resolve(sup),
						map(interfaces, this::resolve, java.lang.reflect.Type[]::new)) + this.getInterfaceSign(),
				Type.getInternalName(sup),
				add(map(interfaces, this.loader::getAbstractedName, String[]::new),
						this.loader.getAbstractedName(this.cls)));
		this.node = node;
		super.write(out);
		out.putNextEntry(new ZipEntry(node.name + ".class"));
		ClassWriter writer = new ClassWriter(ASM9);
		node.accept(writer);
		out.write(writer.toByteArray());
		out.closeEntry();
	}

	@Override
	public void visitBridge(Method method, String target) {
		if (this.impl) {
			int access = method.getModifiers();
			MethodNode node = new MethodNode(access | ACC_FINAL,
					method.getName(),
					Type.getMethodDescriptor(method),
					null /*sign*/,
					null);
			if (!Modifier.isAbstract(access)) {
				// triangular method
				this.invoke(node, method, false);
			}
			this.node.methods.add(node);
		}
	}

	@Override
	public String visitBridged(Method method) {
		TypeToken<?>[] params = map(method.getGenericParameterTypes(), this::resolved, TypeToken[]::new);
		TypeToken<?> returnType = this.resolved(method.getGenericReturnType());
		String desc = this.methodDescriptor(params, returnType);
		String sign = this.impl ? null : this.methodSignature(method.getTypeParameters(), params, returnType, true);
		int access = method.getModifiers();
		MethodNode node = new MethodNode(access, method.getName(), desc, sign, null);
		if (!Modifier.isAbstract(access)) {
			if(this.impl) {
				// triangular method
				this.invoke(node, method, true);
			} else {
				AsmUtil.visitStub(node);
			}
		}

		this.node.methods.add(node);
		return desc;
	}

	@Override
	public void visitFieldGetter(TypeToken<?> token, Field field) {
		AsmUtil.generateGetter(this.node::visitMethod, this::toSignature, token, field, this.impl);
	}

	@Override
	public void visitFieldSetter(TypeToken<?> token, Field field) {
		AsmUtil.generateSetter(this.node::visitMethod, this::toSignature, token, field, this.impl);
	}

	@Override
	public void visitEmptyField(TypeToken<?> token, Field field) {
		if(!this.impl) {
			FieldNode node = new FieldNode(field.getModifiers(), field.getName(), Type.getDescriptor(field.getType()),
					this.toSignature(token.getType()), null);
			this.node.fields.add(node);
		}
	}

	public String getInterfaceSign() {
		String name = "T" + this.loader.getAbstractedName(this.cls);
		TypeVariable<?>[] variables = this.cls.getTypeParameters();
		if (variables.length > 0) {
			StringBuilder builder = new StringBuilder(name);
			builder.append('<');
			for (TypeVariable<?> variable : variables) {
				builder.append('T').append(variable.getName()).append(';');
			}
			builder.append('>');
			builder.append(';');
			return builder.toString();
		}
		return name + ";";
	}
}
