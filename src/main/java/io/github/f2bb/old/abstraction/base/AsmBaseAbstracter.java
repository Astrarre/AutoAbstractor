package io.github.f2bb.old.abstraction.base;

import static io.github.f2bb.old.util.AbstracterUtil.add;
import static io.github.f2bb.old.util.AbstracterUtil.map;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.common.reflect.TypeToken;
import io.github.f2bb.old.loader.AbstracterLoader;
import io.github.f2bb.old.util.AsmUtil;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

@SuppressWarnings ("UnstableApiUsage")
public class AsmBaseAbstracter extends AbstractBaseAbstracter {
	private final boolean impl;

	public AsmBaseAbstracter(Class<?> toAbstract, boolean impl) {
		super(toAbstract);
		this.impl = impl;
	}

	protected ClassNode node;

	@Override
	public void write(ZipOutputStream out) throws IOException {
		ClassNode node = new ClassNode();
		TypeToken<?> sup = this.abstractionType.getSuperClass();
		List<TypeToken<?>> interfaces = this.abstractionType.getInterfaces();
		node.visit(V1_8,
				// todo instance inner classes
				this.cls.getModifiers(),
				this.abstractionType.getBaseAbstractedName(this.cls),
				this.impl ? null : this.classSignature(this.cls.getTypeParameters(),
						this.resolve(sup),
						map(interfaces, this::resolve, java.lang.reflect.Type[]::new)) + this.getInterfaceSign(),
				Type.getInternalName(sup),
				add(map(interfaces, this.abstractionType::getAbstractedName, String[]::new),
						this.abstractionType.getAbstractedName(this.cls)));
		this.node = node;
		super.write(out);
		out.putNextEntry(new ZipEntry(node.name + ".class"));
		ClassWriter writer = new ClassWriter(ASM9);
		node.accept(writer);
		out.write(writer.toByteArray());
		out.closeEntry();
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
		String name = "T" + this.abstractionType.getAbstractedName(this.cls);
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
