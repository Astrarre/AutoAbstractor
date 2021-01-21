package io.github.astrarre.abstracter.abs;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.function.Consumer;

import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.abs.field.FieldAbstracter;
import io.github.astrarre.abstracter.abs.method.BaseConstructorAbstracter;
import io.github.astrarre.abstracter.abs.method.BaseMethodAbstracter;
import io.github.astrarre.abstracter.abs.method.MethodAbstracter;
import io.github.astrarre.abstracter.func.elements.ConstructorSupplier;
import io.github.astrarre.abstracter.func.elements.FieldSupplier;
import io.github.astrarre.abstracter.func.elements.MethodSupplier;
import io.github.astrarre.abstracter.func.inheritance.InterfaceFunction;
import io.github.astrarre.abstracter.func.inheritance.SuperFunction;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class BaseAbstracter extends AbstractAbstracter {
	public BaseAbstracter(Class<?> cls) {
		this(cls, getName(cls, "Base", 0));
	}

	public BaseAbstracter(Class<?> c, String s) {
		this(c,
				s,
				InterfaceFunction.EMPTY,
				SuperFunction.BASE_DEFAULT,
				ConstructorSupplier.BASE_DEFAULT,
				FieldSupplier.BASE_DEFAULT,
				MethodSupplier.BASE_DEFAULT);
	}

	public BaseAbstracter(Class<?> cls,
			String name,
			InterfaceFunction interfaces,
			SuperFunction function,
			ConstructorSupplier supplier,
			FieldSupplier fieldSupplier,
			MethodSupplier methodSupplier) {
		super(cls, name, interfaces, function, supplier, fieldSupplier, methodSupplier);
	}

	@Override
	public void castToMinecraft(MethodVisitor visitor, Consumer<MethodVisitor> apply, Location parameter) {apply.accept(visitor);}

	@Override
	public void castToCurrent(MethodVisitor visitor, Consumer<MethodVisitor> apply, Location parameter) {
		throw new IllegalStateException("what");
	}

	@Override
	public int getAccess(AbstracterConfig config, int modifiers) {
		return this.getCls(config).getModifiers();
	}

	@Override
	public MethodAbstracter<Constructor<?>> abstractConstructor(AbstracterConfig config, Constructor<?> constructor, boolean impl) {
		return new BaseConstructorAbstracter(config, this, constructor, impl);
	}

	@Override
	public MethodAbstracter<Method> abstractMethod(AbstracterConfig config, Method method, boolean impl) {
		return new BaseMethodAbstracter(config, this, method, impl);
	}

	@Override
	public FieldAbstracter abstractField(AbstracterConfig config, Field field, boolean impl) {
		return new FieldAbstracter(config, this, field, impl);
	}

	@Override
	public void postProcess(AbstracterConfig config, ClassNode node, boolean impl) {
		for (MethodNode method : node.methods) {
			if (method.name.equals("<clinit>")) {
				method.visitMethodInsn(INVOKESTATIC, node.name, "astrarre_artificial_clinit", "()V");
				method.visitInsn(RETURN);
			}
		}

		node.interfaces.add(config.getInterfaceName(this.getCls(config)));
		if (node.signature != null) {
			// todo inner instance classes, god damnit
			String name = config.getInterfaceName(this.getCls(config));
			TypeVariable<?>[] variable = this.getCls(config).getTypeParameters();
			StringBuilder signature = new StringBuilder(name.length() + 2 + variable.length * 3);
			signature.append('L').append(name);
			if (variable.length != 0) {
				signature.append('<');
				for (TypeVariable<?> var : variable) {
					signature.append('T').append(var.getName()).append(';');
				}
				signature.append('>');
			}
			signature.append(';');
			node.signature += signature;
		}

		// run post processors last
		super.postProcess(config, node, impl);
	}


}
