package io.github.astrarre.abstracter.abs;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;

import io.github.astrarre.abstracter.func.elements.ConstructorSupplier;
import io.github.astrarre.abstracter.func.elements.FieldSupplier;
import io.github.astrarre.abstracter.func.elements.MethodSupplier;
import io.github.astrarre.abstracter.func.inheritance.InterfaceFunction;
import io.github.astrarre.abstracter.func.inheritance.SuperFunction;
import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.func.map.TypeMappingFunction;
import io.github.astrarre.abstracter.util.AbstracterLoader;
import io.github.astrarre.abstracter.util.asm.FieldUtil;
import io.github.astrarre.abstracter.util.asm.InvokeUtil;
import io.github.astrarre.abstracter.util.asm.MethodUtil;
import io.github.astrarre.abstracter.util.reflect.ReflectUtil;
import io.github.astrarre.abstracter.util.reflect.TypeUtil;
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
	public int getAccess(int modifiers) {
		return this.cls.getModifiers();
	}

	@Override
	public void abstractConstructor(ClassNode node, Constructor<?> constructor, boolean impl) {
		String desc = Type.getConstructorDescriptor(constructor);
		MethodNode method = new MethodNode(constructor.getModifiers(),
				"<init>",
				TypeUtil.REMAPPER.mapSignature(desc, false),
				impl ? null : TypeUtil.REMAPPER.mapSignature(ReflectUtil.getSignature(constructor), false),
				null);
		if (impl) {
			InvokeUtil.invokeConstructor(method, constructor, false);
		} else {
			InvokeUtil.visitStub(method);
		}
		node.methods.add(method);
	}

	@Override
	public void abstractMethod(ClassNode node, Method method, boolean impl) {
		MethodUtil.abstractMethod(node, this.cls, method, impl, false);
	}

	@Override
	public void abstractField(ClassNode node, Field field, boolean impl) {
		if (AbstracterLoader.isMinecraft(TypeMappingFunction.raw(this.cls, field.getGenericType()))) {
			if (!Modifier.isFinal(field.getModifiers())) {
				MethodNode setter = FieldUtil.createSetter(this.cls, field, impl, false);
				if (!MethodUtil.conflicts(setter.name, setter.desc, node)) {
					this.addFieldRefAnnotation(setter, field);
					node.methods.add(setter);
				}
			}
			MethodNode getter = FieldUtil.createGetter(this.cls, field, impl, false);
			if (!MethodUtil.conflicts(getter.name, getter.desc, node)) {
				this.addFieldRefAnnotation(getter, field);
				node.methods.add(getter);
			}
		} else if (!impl || Modifier.isStatic(field.getModifiers())) {
			FieldUtil.createConstant(node, this.cls, field, impl);
		}
	}

	@Override
	public void postProcess(ClassNode node, boolean impl) {
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
