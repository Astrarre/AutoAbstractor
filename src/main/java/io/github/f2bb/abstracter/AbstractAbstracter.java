package io.github.f2bb.abstracter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.function.IntUnaryOperator;

import io.github.f2bb.abstracter.func.abstracting.FieldAbstracter;
import io.github.f2bb.abstracter.func.elements.ConstructorSupplier;
import io.github.f2bb.abstracter.func.elements.FieldSupplier;
import io.github.f2bb.abstracter.func.elements.MethodSupplier;
import io.github.f2bb.abstracter.func.inheritance.InterfaceFunction;
import io.github.f2bb.abstracter.func.inheritance.SuperFunction;
import io.github.f2bb.abstracter.func.string.ToStringFunction;

public abstract class AbstractAbstracter<T> {
	protected final ConstructorSupplier constructorSupplier;
	protected final FieldSupplier fieldSupplier;
	protected final MethodSupplier methodSupplier;
	protected final InterfaceFunction interfaceFunction;
	protected final SuperFunction superFunction;
	protected final ToStringFunction<Class<?>> nameFunction;
	protected final IntUnaryOperator accessOperator;

	protected final FieldAbstracter<T> fieldAbstracter;

	protected AbstractAbstracter(ConstructorSupplier supplier,
			FieldSupplier fieldSupplier,
			MethodSupplier methodSupplier,
			InterfaceFunction function,
			SuperFunction superFunction,
			ToStringFunction<Class<?>> nameFunction,
			IntUnaryOperator operator, FieldAbstracter<T> abstracter) {
		this.constructorSupplier = supplier;
		this.fieldSupplier = fieldSupplier;
		this.methodSupplier = methodSupplier;
		this.interfaceFunction = function;
		this.superFunction = superFunction;
		this.nameFunction = nameFunction;
		this.accessOperator = operator;
		this.fieldAbstracter = abstracter;
	}

	public T apply(Class<?> cls) {
		T header = this.createHeader(this.accessOperator.applyAsInt(cls.getModifiers()),
				this.nameFunction.toString(cls),
				cls.getTypeParameters(),
				this.superFunction.findValidSuper(cls),
				this.interfaceFunction.getInterfaces(cls));
		for (Field field : this.fieldSupplier.getFields(cls)) {
			this.fieldAbstracter.abstractField(header, cls, field);
		}

		for (Constructor<?> constructor : this.constructorSupplier.getConstructors(cls)) {
			this.abstractConstructor(header, cls, constructor);
		}

		for (Method method : this.methodSupplier.getMethods(cls)) {
			this.abstractMethod(header, cls, method);
		}

		return header;
	}

	protected abstract T createHeader(int access,
			String name,
			TypeVariable<?>[] variables,
			Type sup,
			Collection<Type> interfaces);

	protected abstract void abstractMethod(T header, Class<?> cls, Method field);

	protected abstract void abstractConstructor(T header, Class<?> cls, Constructor<?> field);
}
