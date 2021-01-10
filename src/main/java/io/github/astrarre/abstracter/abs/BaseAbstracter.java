package io.github.astrarre.abstracter.abs;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.function.Consumer;

import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.abs.method.BaseMethodAbstracter;
import io.github.astrarre.abstracter.abs.method.MethodAbstracter;
import io.github.astrarre.abstracter.func.elements.ConstructorSupplier;
import io.github.astrarre.abstracter.func.elements.FieldSupplier;
import io.github.astrarre.abstracter.func.elements.MethodSupplier;
import io.github.astrarre.abstracter.func.inheritance.InterfaceFunction;
import io.github.astrarre.abstracter.func.inheritance.SuperFunction;
import io.github.astrarre.abstracter.func.map.TypeMappingFunction;
import io.github.astrarre.abstracter.util.reflect.ReflectUtil;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
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
	public int getAccess(int modifiers) {
		return this.cls.getModifiers();
	}

	@Override
	public void abstractConstructor(ClassNode node, Constructor<?> constructor, boolean impl) {
		String desc = Type.getConstructorDescriptor(constructor);
		MethodNode method = new MethodNode(constructor.getModifiers(),
				"<init>",
				AbstractAbstracter.REMAPPER.mapSignature(desc, false),
				impl ? null : AbstractAbstracter.REMAPPER.mapSignature(ReflectUtil.getSignature(constructor), false),
				null);
		if (impl) {
			// fixme: this.invokeConstructor(method, constructor, false);
		} else {
			AbstractAbstracter.visitStub(method);
		}
		node.methods.add(method);
	}


	@Override
	public MethodAbstracter abstractMethod(Method method, boolean impl) {
		return new BaseMethodAbstracter(this, method, impl);
	}

	@Override
	public void abstractField(ClassNode node, Field field, boolean impl) {
		if (AbstracterConfig.isMinecraft(TypeMappingFunction.raw(this.cls, field.getGenericType()))) {
			if (!Modifier.isFinal(field.getModifiers())) {
				MethodNode setter = this.createSetter(this.name, this.cls, field, impl, false);
				if (!AbstractAbstracter.conflicts(setter.name, setter.desc, node)) {
					node.methods.add(setter);
				}
			}
			MethodNode getter = this.createGetter(this.name, this.cls, field, impl, false);
			if (!AbstractAbstracter.conflicts(getter.name, getter.desc, node)) {
				node.methods.add(getter);
			}
		} else if (!impl || Modifier.isStatic(field.getModifiers())) {
			this.createConstant(node, this.cls, field, impl);
		}
	}



	@Override
	public void postProcess(ClassNode node, boolean impl) {
		for (MethodNode method : node.methods) {
			if (method.name.equals("<clinit>")) {
				method.visitMethodInsn(INVOKESTATIC, node.name, "astrarre_artificial_clinit", "()V");
				method.visitInsn(RETURN);
			}
		}

		node.interfaces.add(AbstracterConfig.getInterfaceName(this.cls));
		if (node.signature != null) {
			// todo inner instance classes, god damnit
			String name = AbstracterConfig.getInterfaceName(this.cls);
			TypeVariable<?>[] variable = this.cls.getTypeParameters();
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
		super.postProcess(node, impl);
	}


}
