package io.github.astrarre.abstracter.abs;

import static io.github.astrarre.abstracter.util.ArrayUtil.map;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.reflect.TypeToken;
import io.github.astrarre.abstracter.abs.method.InterfaceMethodAbstracter;
import io.github.astrarre.abstracter.abs.method.MethodAbstracter;
import io.github.astrarre.abstracter.func.elements.ConstructorSupplier;
import io.github.astrarre.abstracter.func.elements.FieldSupplier;
import io.github.astrarre.abstracter.func.elements.MethodSupplier;
import io.github.astrarre.abstracter.func.inheritance.InterfaceFunction;
import io.github.astrarre.abstracter.func.inheritance.SuperFunction;
import io.github.astrarre.abstracter.func.map.TypeMappingFunction;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

@SuppressWarnings ("UnstableApiUsage")
public class InterfaceAbstracter extends AbstractAbstracter {
	private static final int REMOVE_FLAGS = ACC_ENUM | ACC_FINAL;
	private static final int ADD_FLAGS = ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT;

	public InterfaceAbstracter(Class<?> cls) {
		this(cls,
				getName(cls, "", 0),
				InterfaceFunction.INTERFACE_DEFAULT,
				ConstructorSupplier.INTERFACE_DEFAULT,
				FieldSupplier.INTERFACE_DEFAULT,
				MethodSupplier.INTERFACE_DEFAULT);
	}

	public InterfaceAbstracter(Class<?> cls,
			String name,
			InterfaceFunction interfaces,
			ConstructorSupplier supplier,
			FieldSupplier fieldSupplier,
			MethodSupplier methodSupplier) {
		super(cls, name, interfaces, SuperFunction.EMPTY, supplier, fieldSupplier, methodSupplier);
	}

	public InterfaceAbstracter(Class<?> cls, int version) {
		this(cls, getName(cls, "", version));
	}

	public InterfaceAbstracter(Class<?> cls, String name) {
		this(cls,
				name,
				InterfaceFunction.INTERFACE_DEFAULT,
				ConstructorSupplier.INTERFACE_DEFAULT,
				FieldSupplier.INTERFACE_DEFAULT,
				MethodSupplier.INTERFACE_DEFAULT);
	}

	@Override
	public void castToMinecraft(MethodVisitor visitor, Consumer<MethodVisitor> apply, Location parameter) {
		apply.accept(visitor);
		visitor.visitTypeInsn(CHECKCAST, this.name);
	}

	@Override
	public void castToCurrent(MethodVisitor visitor, Consumer<MethodVisitor> apply, Location parameter) {
		apply.accept(visitor);
	}

	@Override
	public int getAccess(int modifiers) {
		return (modifiers & ~REMOVE_FLAGS) | ADD_FLAGS;
	}

	@Override
	public MethodAbstracter<Constructor<?>> abstractConstructor(Constructor<?> constructor, boolean impl) {
		Function<Type, TypeToken<?>> resolve = TypeMappingFunction.resolve(this.cls);
		TypeToken<?>[] params = map(constructor.getGenericParameterTypes(), resolve::apply, TypeToken[]::new);
		TypeToken<?> returnType = resolve.apply(this.cls);
		MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC,
				"newInstance",
				AbstractAbstracter.methodDescriptor(params, returnType),
				AbstractAbstracter.methodSignature(constructor.getTypeParameters(), params, returnType),
				null);
		if (impl) {
			// fixme: AbstractAbstracter.invokeConstructor(this.name, method, constructor, true);
		} else {
			AbstractAbstracter.visitStub(method);
		}
	}

	@Override
	public MethodAbstracter<Method> abstractMethod(Method method, boolean impl) {
		return new InterfaceMethodAbstracter(this, method, impl);
	}

	@Override
	public void abstractField(ClassNode node, Field field, boolean impl) {
		int access = field.getModifiers();
		if ((access & ACC_ENUM) != 0) {
			this.createConstant(node, this.cls, field, impl);
		} else {
			if (!Modifier.isFinal(access)) {
				MethodNode setter = this.createSetter(this.name, this.cls, field, impl, true);
				if (!AbstractAbstracter.conflicts(setter.name, setter.desc, node)) {
					setter.access &= ~ACC_FINAL;
					node.methods.add(setter);
				}
			}

			MethodNode getter = this.createGetter(this.name, this.cls, field, impl, true);
			if (!AbstractAbstracter.conflicts(getter.name, getter.desc, node)) {
				getter.access &= ~ACC_FINAL;
				node.methods.add(getter);
			}
		}
	}

}
